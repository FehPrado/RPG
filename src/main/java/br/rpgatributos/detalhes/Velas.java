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

    /** 3.0: o pano da vela: o estandarte que o dono pôs nela, ou o branco. */
    static ItemStack bandeira(Boat b) {
        byte[] est = b.getPersistentDataContainer().get(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
        return est != null ? ItemStack.deserializeBytes(est) : new ItemStack(Material.WHITE_BANNER);
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
            p.sendActionBar(Component.text("⛵ Vela posta: reme para a frente na água. Um estandarte clicado vira o pano. Agachado + clique tira.", COR));
        } else if (org.bukkit.Tag.ITEMS_BANNERS.isTagged(mao.getType()) && temVela(b)) {
            // 3.0: o estandarte vira o pano da vela (o anterior volta para a mão).
            e.setCancelled(true);
            byte[] antes = b.getPersistentDataContainer().get(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
            b.getPersistentDataContainer().set(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY, mao.asOne().serializeAsBytes());
            if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
            if (antes != null) for (ItemStack s : p.getInventory().addItem(ItemStack.deserializeBytes(antes)).values()) p.getWorld().dropItem(p.getLocation(), s);
            for (Entity x : b.getPassengers()) if (x instanceof ItemDisplay d && d.getPersistentDataContainer().has(K_DISPLAY)) d.setItemStack(bandeira(b));
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.3f);
        } else if (p.isSneaking() && mao.getType().isAir() && temVela(b)) {
            e.setCancelled(true);
            byte[] est = b.getPersistentDataContainer().get(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
            tirarVela(b);
            for (ItemStack s : p.getInventory().addItem(item()).values()) p.getWorld().dropItem(p.getLocation(), s);
            if (est != null) for (ItemStack s : p.getInventory().addItem(ItemStack.deserializeBytes(est)).values()) p.getWorld().dropItem(p.getLocation(), s);
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
        }
    }

    /** A vela vai como passageira do barco (anda junto, sem ser teleportada). */
    private static void montarVela(Boat b) {
        ItemDisplay d = b.getWorld().spawn(b.getLocation(), ItemDisplay.class, x -> {
            ItemStack bandeira = bandeira(b);
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
        b.getPersistentDataContainer().remove(MelhoriasBarco.K_ESTANDARTE);
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
        byte[] est = b.getPersistentDataContainer().get(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
        tirarVela(b);
        b.getWorld().dropItemNaturally(b.getLocation(), item());
        if (est != null) b.getWorld().dropItemNaturally(b.getLocation(), ItemStack.deserializeBytes(est));
    }

    // =====================================================================
    //  O vento na vela
    // =====================================================================

    /** Onde cada barco a vela estava no tick anterior (para medir a velocidade de verdade). */
    private final java.util.Map<java.util.UUID, org.bukkit.Location> antes = new java.util.HashMap<>();

    /** O ângulo em que a vela de cada barco está agora (para não mandar o mesmo de novo). */
    private final java.util.Map<java.util.UUID, Float> anguloVela = new java.util.HashMap<>();

    /**
     * A cada tick: o barco a vela com alguém remando para a frente ganha o impulso do vento.
     * <ul>
     *   <li>Vento por trás: o máximo; de lado: a metade; de frente: quase nada (sobra o remo).</li>
     *   <li>A força vem do tempo (fraco, bom na chuva, forte na Ventania; na Ventania ele ainda
     *       empurra o barco um pouco para o lado do vento).</li>
     *   <li>A vela gira para pegar o vento, e a barra diz se ele está a favor, de lado ou contra.</li>
     * </ul>
     * Quem move o barco é o jogo de quem pilota: no servidor, a velocidade do barco fica quase
     * zero. Por isso a velocidade é medida pelo quanto o barco andou desde o tick anterior (usar a
     * do servidor freava o barco a cada tick).
     */
    public void tick() {
        if (!plugin.settings().velas) return;
        double forca = br.rpgatributos.mundo.Vento.forca(plugin);
        boolean ventania = forca >= 1.5;
        double rumo = br.rpgatributos.mundo.Vento.rumo();
        Vector vento = br.rpgatributos.mundo.Vento.direcao();
        java.util.Set<java.util.UUID> vistos = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getVehicle() instanceof Boat b) || !temVela(b) || b.getPassengers().getFirst() != p) continue;
            vistos.add(b.getUniqueId());
            orientarVela(b, rumo, ventania);
            org.bukkit.Location agora = b.getLocation(), ultimo = antes.put(b.getUniqueId(), agora);
            if (ultimo == null || !ultimo.getWorld().equals(agora.getWorld())) continue;
            if (b.getStatus() != Boat.Status.IN_WATER || !p.getCurrentInput().isForward()) continue;
            Vector frente = agora.getDirection().setY(0);
            if (frente.lengthSquared() < 1e-4) continue;
            frente.normalize();
            Vector v = agora.toVector().subtract(ultimo.toVector());
            if (v.lengthSquared() > 4) continue; // teleporte ou volta ao mundo: não é velocidade
            v.setY(Math.max(-0.1, Math.min(0.1, v.getY())));
            double cos = frente.dot(vento);
            double aproveita = 0.15 + 0.85 * (1 + cos) / 2;
            Vector nova = v.clone().add(frente.clone().multiply(0.03 * forca * aproveita));
            if (ventania) nova.add(vento.clone().multiply(0.006));
            double h = Math.hypot(nova.getX(), nova.getZ());
            double max = VELOCIDADE_MAXIMA * (0.55 + 0.45 * aproveita) * (ventania ? 1.25 : 1);
            if (h > max) {
                nova.setX(nova.getX() / h * max);
                nova.setZ(nova.getZ() / h * max);
            }
            b.setVelocity(nova);
            if (Bukkit.getCurrentTick() % 10 == 0) b.getWorld().spawnParticle(Particle.SPLASH, b.getLocation(), 6, 0.5, 0.1, 0.5, 0.1);
            if (Bukkit.getCurrentTick() % 30 == 0) {
                String lado = cos > 0.5 ? "a favor" : cos < -0.5 ? "contra" : "de lado";
                NamedTextColor c = cos > 0.5 ? NamedTextColor.GREEN : cos < -0.5 ? NamedTextColor.RED : NamedTextColor.YELLOW;
                p.sendActionBar(Component.text("⛵ Vento " + lado, c).append(Component.text(" · "
                        + br.rpgatributos.mundo.Vento.intensidade(forca) + " (" + br.rpgatributos.mundo.Vento.de() + ")", COR)));
            }
        }
        antes.keySet().retainAll(vistos);
        anguloVela.keySet().retainAll(vistos);
    }

    /**
     * A vela (o estandarte) gira em cima do barco para ficar de frente para o vento, até 70° para
     * cada lado (a posição dela no barco não muda: só o giro em volta do mastro).
     */
    private void orientarVela(Boat b, double rumo, boolean ventania) {
        for (Entity x : b.getPassengers()) {
            if (!(x instanceof ItemDisplay d) || !d.getPersistentDataContainer().has(K_DISPLAY)) continue;
            double phi = ((d.getLocation().getYaw() - rumo) % 360 + 540) % 360 - 180;
            if (phi > 90) phi -= 180; // a vela é igual dos dois lados
            if (phi < -90) phi += 180;
            phi = Math.max(-70, Math.min(70, phi));
            if (ventania) phi += java.util.concurrent.ThreadLocalRandom.current().nextDouble(-6, 6);
            Float atual = anguloVela.get(b.getUniqueId());
            if (atual != null && Math.abs(atual - phi) < 3) continue;
            anguloVela.put(b.getUniqueId(), (float) phi);
            Transformation t = d.getTransformation();
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(8);
            d.setTransformation(new Transformation(t.getTranslation(), new org.joml.Quaternionf().rotationY((float) Math.toRadians(phi)),
                    t.getScale(), t.getRightRotation()));
        }
    }
}
