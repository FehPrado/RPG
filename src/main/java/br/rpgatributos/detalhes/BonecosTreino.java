package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.combo.TipoArma;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boneco de Treino: 1 abóbora esculpida e 1 suporte de armadura jogados num fardo de feno.
 * Um boneco aparece em cima: bata nele (com arma, arco, combo ou magia) para ver o dano de cada
 * golpe e o dano por segundo. Ele nunca quebra. Dá um pouco de XP de Combate e de proficiência
 * (até a proficiência 5), com limite por hora para não virar fazenda.
 */
public final class BonecosTreino extends Estacao {

    public static final TextColor COR = TextColor.color(0xD9A441);
    private static final int PROFICIENCIA_MAXIMA = 5;

    private record Golpe(long quando, double dano) { }

    private final NamespacedKey kBoneco;
    private final Map<UUID, Deque<Golpe>> golpes = new HashMap<>();
    private final Map<UUID, Double> melhor = new HashMap<>();
    private final Map<UUID, double[]> xpHora = new HashMap<>(); // [início da hora, XP ganho]
    private final Map<UUID, Long> ultimoXp = new HashMap<>();
    private int passos;

    public BonecosTreino(RPGAtributos plugin) {
        super(plugin, "bonecos_treino", "boneco_display");
        this.kBoneco = new NamespacedKey(plugin, "boneco_treino");
    }

    private static String chave(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.HAY_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.CARVED_PUMPKIN || m == Material.ARMOR_STAND;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.HAY_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.CARVED_PUMPKIN) < 1 || contar(itens, Material.ARMOR_STAND) < 1) return false;
        tirar(itens, Material.CARVED_PUMPKIN, 1);
        tirar(itens, Material.ARMOR_STAND, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.AIR);
    }

    @Override
    protected float escalaItem() {
        return 0.1f;
    }

    @Override
    protected double alturaItem() {
        return 3.1;
    }

    @Override
    protected Component nome() {
        return Component.text("🎯 Boneco de Treino", COR)
                .append(Component.newline())
                .append(Component.text("bata para medir o seu dano", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        garantirBoneco(b.getLocation());
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_ARMOR_STAND_PLACE, 1f, 0.9f);
        if (quem != null) quem.sendMessage(Component.text("🎯 Boneco de Treino pronto. Bata nele para ver o dano de cada golpe.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 20 == 0) garantirBoneco(centro.getBlock().getLocation().subtract(0, Math.floor(alturaItem()), 0));
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.CARVED_PUMPKIN));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.ARMOR_STAND));
    }

    @Override
    protected void aoRemover(Location bloco) {
        String k = chave(bloco);
        for (Entity e : bloco.getWorld().getNearbyEntities(bloco.clone().add(0.5, 1.5, 0.5), 1, 1.5, 1)) {
            if (k.equals(e.getPersistentDataContainer().get(kBoneco, PersistentDataType.STRING))) e.remove();
        }
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Deque<Golpe> g = golpes.get(p.getUniqueId());
        if (g == null || g.isEmpty()) {
            p.sendMessage(Component.text("🎯 Bata no boneco para medir o seu dano.", COR));
            return;
        }
        p.sendMessage(Component.text("🎯 Treino: maior golpe " + StatsManager.fmt(melhor.getOrDefault(p.getUniqueId(), 0.0))
                + ", dano por segundo " + StatsManager.fmt(dps(g)) + " (últimos 5 s).", COR));
    }

    /** O boneco (um suporte de armadura) está em cima do fardo? Se não, põe. */
    private void garantirBoneco(Location bloco) {
        if (!bloco.isChunkLoaded() || !blocoValido(bloco.getBlock().getType())) return;
        String k = chave(bloco);
        for (Entity e : bloco.getWorld().getNearbyEntities(bloco.clone().add(0.5, 1.5, 0.5), 1, 1.5, 1)) {
            if (k.equals(e.getPersistentDataContainer().get(kBoneco, PersistentDataType.STRING))) return;
        }
        Location onde = bloco.clone().add(0.5, 1, 0.5);
        bloco.getWorld().spawn(onde, ArmorStand.class, a -> {
            a.setArms(true);
            a.setBasePlate(false);
            a.setGravity(false);
            a.setPersistent(true);
            a.setCanPickupItems(false);
            a.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
            ItemStack peito = new ItemStack(Material.LEATHER_CHESTPLATE);
            peito.editMeta(LeatherArmorMeta.class, m -> m.setColor(Color.fromRGB(0xC8A060)));
            a.getEquipment().setChestplate(peito);
            for (EquipmentSlot s : EquipmentSlot.values()) {
                a.addEquipmentLock(s, ArmorStand.LockType.REMOVING_OR_CHANGING);
                a.addEquipmentLock(s, ArmorStand.LockType.ADDING);
            }
            a.getPersistentDataContainer().set(kBoneco, PersistentDataType.STRING, k);
        });
    }

    private boolean ehBoneco(Entity e) {
        return e instanceof ArmorStand && e.getPersistentDataContainer().has(kBoneco);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMexer(PlayerArmorStandManipulateEvent e) {
        if (ehBoneco(e.getRightClicked())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoClicarBoneco(PlayerInteractAtEntityEvent e) {
        if (ehBoneco(e.getRightClicked())) e.setCancelled(true);
    }

    // =====================================================================
    //  Golpes
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (!ehBoneco(e.getEntity())) return;
        Player p = e.getDamager() instanceof Player pl ? pl
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        double dano = e.getFinalDamage();
        e.setCancelled(true);
        if (p == null || dano <= 0) return;
        long agora = System.currentTimeMillis();
        Deque<Golpe> g = golpes.computeIfAbsent(p.getUniqueId(), u -> new ArrayDeque<>());
        g.addLast(new Golpe(agora, dano));
        while (!g.isEmpty() && agora - g.peekFirst().quando() > 5000) g.removeFirst();
        double recorde = Math.max(melhor.getOrDefault(p.getUniqueId(), 0.0), dano);
        melhor.put(p.getUniqueId(), recorde);
        p.sendActionBar(Component.text("🎯 " + StatsManager.fmt(dano) + " de dano", COR)
                .append(Component.text("  ·  " + StatsManager.fmt(dps(g)) + "/s", NamedTextColor.YELLOW))
                .append(Component.text("  ·  maior: " + StatsManager.fmt(recorde), NamedTextColor.GRAY)));
        Entity boneco = e.getEntity();
        boneco.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, boneco.getLocation().add(0, 1.4, 0),
                (int) Math.min(10, Math.max(1, dano / 2)), 0.25, 0.3, 0.25, 0.1);
        boneco.getWorld().playSound(boneco.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 0.8f, 1f);
        plugin.recordes().golpe(p, dano);
        darXp(p, agora);
    }

    private static double dps(Deque<Golpe> g) {
        if (g.isEmpty()) return 0;
        double soma = 0;
        for (Golpe x : g) soma += x.dano();
        double segundos = Math.max(1, (g.peekLast().quando() - g.peekFirst().quando()) / 1000.0);
        return soma / segundos;
    }

    /** XP de Combate e de proficiência, no máximo um por meio segundo e até o limite da hora. */
    private void darXp(Player p, long agora) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        Long ultimo = ultimoXp.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < 500) return;
        ultimoXp.put(p.getUniqueId(), agora);
        double[] h = xpHora.computeIfAbsent(p.getUniqueId(), u -> new double[]{agora, 0});
        if (agora - h[0] > 3_600_000) { h[0] = agora; h[1] = 0; }
        double limite = plugin.settings().detBonecoXpHora;
        if (h[1] >= limite) {
            if (h[1] == limite) p.sendMessage(Component.text("🎯 Você já treinou bastante por agora: o boneco não dá mais XP nesta hora.", NamedTextColor.GRAY));
            h[1] += 0.001;
            return;
        }
        double xp = 0.4;
        h[1] = Math.min(limite, h[1] + xp);
        plugin.stats().darXp(p, Skill.COMBATE, xp);
        TipoArma t = TipoArma.de(p.getInventory().getItemInMainHand());
        if (t != null && plugin.combos().nivel(p, t) < PROFICIENCIA_MAXIMA) plugin.combos().darXp(p, t, 0.5);
    }

    @Override
    protected void aoTick() {
        if (++passos % 1200 != 0) return; // a cada 5 min limpa quem não treina mais
        long agora = System.currentTimeMillis();
        golpes.values().removeIf(g -> g.isEmpty() || agora - g.peekLast().quando() > 300_000);
        ultimoXp.values().removeIf(t -> agora - t > 300_000);
    }
}
