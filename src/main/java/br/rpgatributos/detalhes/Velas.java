package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Barco a vela (2.62): a Vela (lã e gravetos na bancada) vai num barco comum com um clique. A vela
 * fica de pé no barco (ela ocupa o segundo lugar: o barco a vela leva uma pessoa) e, remando para a
 * frente na água, o barco corre bem mais; na Ventania, mais ainda. Agachado + clique tira a vela.
 */
public final class Velas implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "vela_barco");
    static final NamespacedKey K_DISPLAY = new NamespacedKey("rpgatributos", "vela_display");
    private static final TextColor COR = TextColor.color(0xE3F2FD);
    private static final double VELOCIDADE_MAXIMA = 0.75;

    private final RPGAtributos plugin;

    public Velas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack item() {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            m.itemName(Component.text("Vela de Barco", COR));
            m.lore(List.of(
                    Component.text("Clique num barco para pôr a vela.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Remando para a frente na água, ele corre bem mais.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "vela_barco");
        });
        return i;
    }

    static boolean eVela(ItemStack i) {
        return i != null && i.getType() == Material.PAPER && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "vela_barco"), item());
        r.shape(" W ", "WWS", " S ");
        r.setIngredient('W', new org.bukkit.inventory.RecipeChoice.MaterialChoice(org.bukkit.Tag.WOOL));
        r.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(r);
    }

    static boolean temVela(Boat b) {
        return b.getPersistentDataContainer().has(CHAVE);
    }

    // =====================================================================
    //  Pôr e tirar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoClicar(PlayerInteractEntityEvent e) {
        if (!plugin.settings().velas || e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof Boat b) || b instanceof ChestBoat || BarcoGrande.eBarcoGrande(b)) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (eVela(mao) && !temVela(b)) {
            e.setCancelled(true);
            if (b.getPassengers().size() >= 2) {
                p.sendActionBar(Component.text("⛵ O barco está cheio: tire alguém para pôr a vela.", NamedTextColor.RED));
                return;
            }
            mao.setAmount(mao.getAmount() - 1);
            b.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
            montarVela(b);
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
            p.sendActionBar(Component.text("⛵ Vela posta: reme para a frente na água. Agachado + clique tira.", COR));
        } else if (p.isSneaking() && mao.getType().isAir() && temVela(b)) {
            e.setCancelled(true);
            tirarVela(b);
            for (ItemStack s : p.getInventory().addItem(item()).values()) p.getWorld().dropItem(p.getLocation(), s);
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
        }
    }

    /** A vela vai como passageira do barco (anda junto, sem ser teleportada). */
    private static void montarVela(Boat b) {
        ItemDisplay d = b.getWorld().spawn(b.getLocation(), ItemDisplay.class, x -> {
            ItemStack bandeira = new ItemStack(Material.WHITE_BANNER);
            x.setItemStack(bandeira);
            x.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            x.setTransformation(new Transformation(new Vector3f(0, 1.1f, -0.2f), new AxisAngle4f(), new Vector3f(1.6f, 1.6f, 1.6f), new AxisAngle4f()));
            x.setPersistent(true);
            x.getPersistentDataContainer().set(K_DISPLAY, PersistentDataType.BYTE, (byte) 1);
        });
        b.addPassenger(d);
    }

    private static void tirarVela(Boat b) {
        b.getPersistentDataContainer().remove(CHAVE);
        for (Entity x : b.getPassengers()) {
            if (x instanceof ItemDisplay d && d.getPersistentDataContainer().has(K_DISPLAY)) {
                b.removePassenger(d);
                d.remove();
            }
        }
    }

    /** Quem rema é o primeiro passageiro: quando alguém sobe, a vela volta para o segundo lugar. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoSubir(org.bukkit.event.entity.EntityMountEvent e) {
        if (!(e.getMount() instanceof Boat b) || !(e.getEntity() instanceof Player) || !temVela(b) || BarcoGrande.eBarcoGrande(b)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!b.isValid() || b.getPassengers().isEmpty() || b.getPassengers().getFirst() instanceof Player) return;
            for (Entity x : List.copyOf(b.getPassengers())) {
                if (x instanceof ItemDisplay d && d.getPersistentDataContainer().has(K_DISPLAY)) {
                    b.removePassenger(d);
                    b.addPassenger(d);
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(VehicleDestroyEvent e) {
        if (!(e.getVehicle() instanceof Boat b) || !temVela(b) || BarcoGrande.eBarcoGrande(b)) return;
        tirarVela(b);
        b.getWorld().dropItemNaturally(b.getLocation(), item());
    }

    // =====================================================================
    //  O vento na vela
    // =====================================================================

    /** A cada tick: o barco a vela com alguém remando para a frente ganha impulso. */
    public void tick() {
        if (!plugin.settings().velas) return;
        boolean ventania = plugin.ceu().clima() == br.rpgatributos.mundo.Ceu.Clima.VENTANIA;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getVehicle() instanceof Boat b) || !temVela(b) || b.getPassengers().getFirst() != p) continue;
            if (b.getStatus() != Boat.Status.IN_WATER || !p.getCurrentInput().isForward()) continue;
            Vector frente = b.getLocation().getDirection().setY(0);
            if (frente.lengthSquared() < 1e-4) continue;
            frente.normalize();
            Vector v = b.getVelocity();
            double impulso = ventania ? 0.035 : 0.025;
            Vector nova = v.clone().add(frente.multiply(impulso));
            double h = Math.hypot(nova.getX(), nova.getZ()), max = ventania ? VELOCIDADE_MAXIMA * 1.25 : VELOCIDADE_MAXIMA;
            if (h > max) {
                nova.setX(nova.getX() / h * max);
                nova.setZ(nova.getZ() / h * max);
            }
            b.setVelocity(nova);
            if (Bukkit.getCurrentTick() % 10 == 0) b.getWorld().spawnParticle(Particle.SPLASH, b.getLocation(), 6, 0.5, 0.1, 0.5, 0.1);
        }
    }
}
