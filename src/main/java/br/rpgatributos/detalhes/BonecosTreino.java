package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.combo.TipoArma;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.util.Vector;
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
 * golpe e o dano por segundo. Ele nunca quebra. Só mede: não dá XP de Combate nem de proficiência
 * (os combos funcionam nele sem interromper a sequência).
 */
public final class BonecosTreino extends Estacao {

    public static final TextColor COR = TextColor.color(0xD9A441);

    private record Golpe(long quando, double dano) { }

    /** O treino de um jogador com uma arma: total, golpes, maior, tempo batendo e o dano de cada golpe de combo. */
    private static final class Treino {
        double soma, maior;
        int golpes;
        long ativoMs, ultimo;
        final Map<String, double[]> combos = new java.util.LinkedHashMap<>(); // nome → [soma, vezes]

        void somar(long agora, double dano, String combo) {
            // Só conta o tempo em que está batendo: uma pausa de mais de 2 s não derruba o dano por segundo.
            if (golpes > 0) ativoMs += Math.min(2000, agora - ultimo);
            ultimo = agora;
            soma += dano;
            golpes++;
            maior = Math.max(maior, dano);
            if (combo != null) {
                double[] c = combos.computeIfAbsent(combo, k -> new double[2]);
                c[0] += dano;
                c[1]++;
            }
        }

        double dps() {
            return soma / Math.max(1, ativoMs / 1000.0);
        }
    }

    private final NamespacedKey kBoneco;
    private final Map<UUID, Deque<Golpe>> golpes = new HashMap<>();
    private final Map<UUID, Double> melhor = new HashMap<>();
    /** Treino de cada jogador por arma (para comparar adaga, foice, espada...). */
    private final Map<UUID, Map<String, Treino>> treinos = new HashMap<>();
    /** Bonecos que levaram golpe há pouco (combos e habilidades empurram): voltam para o lugar. */
    private final Map<UUID, Long> ancorar = new HashMap<>();
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
        Map<String, Treino> porArma = treinos.get(p.getUniqueId());
        if (agachado) {
            treinos.remove(p.getUniqueId());
            golpes.remove(p.getUniqueId());
            melhor.remove(p.getUniqueId());
            p.sendMessage(Component.text("🎯 Treino zerado. Bata no boneco para começar de novo.", COR));
            return;
        }
        if (porArma == null || porArma.isEmpty()) {
            p.sendMessage(Component.text("🎯 Bata no boneco para medir o seu dano (troque de arma para comparar).", COR));
            return;
        }
        p.sendMessage(Component.text("🎯 Treino por arma", COR, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text("  (agachado + clique zera)", NamedTextColor.DARK_GRAY)));
        List<Map.Entry<String, Treino>> lista = new java.util.ArrayList<>(porArma.entrySet());
        lista.sort((x, y) -> Double.compare(y.getValue().dps(), x.getValue().dps()));
        for (Map.Entry<String, Treino> en : lista) {
            Treino t = en.getValue();
            p.sendMessage(Component.text(" ⚔ " + en.getKey(), NamedTextColor.WHITE)
                    .append(Component.text(": " + StatsManager.fmt(t.dps()) + "/s", NamedTextColor.YELLOW))
                    .append(Component.text(" · " + t.golpes + " golpes · média " + StatsManager.fmt(t.soma / t.golpes)
                            + " · maior " + StatsManager.fmt(t.maior), NamedTextColor.GRAY)));
            List<Map.Entry<String, double[]>> combos = new java.util.ArrayList<>(t.combos.entrySet());
            combos.sort((x, y) -> Double.compare(y.getValue()[0] / y.getValue()[1], x.getValue()[0] / x.getValue()[1]));
            for (int i = 0; i < Math.min(3, combos.size()); i++) {
                double[] c = combos.get(i).getValue();
                p.sendMessage(Component.text("    ✦ " + combos.get(i).getKey() + ": " + StatsManager.fmt(c[0] / c[1]) + " por uso (" + (int) c[1] + "x)",
                        NamedTextColor.GOLD));
            }
        }
    }

    /** Com o que o jogador está batendo: a arma do arsenal, o tipo de arma dos combos ou o item da mão. */
    private static String armaDe(Player p) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        br.rpgatributos.combo.Arsenal.Arma a = br.rpgatributos.combo.Arsenal.de(mao);
        if (a != null) {
            return TipoArma.de(p) == TipoArma.DUPLAS ? "Lâminas Duplas" : a.nome;
        }
        TipoArma t = TipoArma.de(p);
        if (t != null) return t.nome();
        return mao.isEmpty() ? "Mãos" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(mao.effectiveName());
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

    public boolean ehBoneco(Entity e) {
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

    /** Fogo, gelo, explosão e afins nunca estragam o boneco (os golpes passam por aoBater). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSofrer(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent || !ehBoneco(e.getEntity())) return;
        e.setCancelled(true);
        e.getEntity().setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPegarFogo(org.bukkit.event.entity.EntityCombustEvent e) {
        if (ehBoneco(e.getEntity())) e.setCancelled(true);
    }

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
        String arma = e.getDamager() instanceof Projectile && !(e.getDamager() instanceof org.bukkit.entity.Snowball) ? "Arco e besta" : armaDe(p);
        br.rpgatributos.combo.Golpe combo = plugin.combos().golpeRecente(p, 1500);
        treinos.computeIfAbsent(p.getUniqueId(), u -> new java.util.LinkedHashMap<>())
                .computeIfAbsent(arma, k -> new Treino()).somar(agora, dano, combo == null ? null : combo.nome());
        Component barra = Component.text("🎯 " + StatsManager.fmt(dano) + " de dano", COR)
                .append(Component.text(combo != null ? " (" + combo.nome() + ")" : "", NamedTextColor.GOLD))
                .append(Component.text("  ·  " + StatsManager.fmt(dps(g)) + "/s", NamedTextColor.YELLOW))
                .append(Component.text("  ·  maior: " + StatsManager.fmt(recorde), NamedTextColor.GRAY))
                .append(Component.text("  ·  " + arma, NamedTextColor.DARK_GRAY));
        // No meio de uma sequência de combo, a barra é do combo (D · E · ?): a do boneco espera.
        if (!plugin.combos().emSequencia(p)) p.sendActionBar(barra);
        // O combo escreve o nome dele na barra logo depois do golpe: a do boneco volta 1 tick depois.
        if (combo != null) plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!plugin.combos().emSequencia(p)) p.sendActionBar(barra);
        });
        Entity boneco = e.getEntity();
        ancorar.put(boneco.getUniqueId(), agora + 4000);
        plugin.getServer().getScheduler().runTask(plugin, () -> voltarAoLugar(boneco));
        boneco.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, boneco.getLocation().add(0, 1.4, 0),
                (int) Math.min(10, Math.max(1, dano / 2)), 0.25, 0.3, 0.25, 0.1);
        boneco.getWorld().playSound(boneco.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 0.8f, 1f);
        plugin.recordes().golpe(p, dano);
    }

    /** Põe o boneco de volta em cima do fardo de feno (os golpes empurram e levantam). */
    private void voltarAoLugar(Entity boneco) {
        if (!boneco.isValid()) return;
        String k = boneco.getPersistentDataContainer().get(kBoneco, PersistentDataType.STRING);
        if (k == null) return;
        String[] p = k.split(",");
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return;
        Location casa = new Location(w, Integer.parseInt(p[1]) + 0.5, Integer.parseInt(p[2]) + 1, Integer.parseInt(p[3]) + 0.5,
                boneco.getLocation().getYaw(), 0);
        boneco.setVelocity(new Vector());
        if (boneco.getLocation().distanceSquared(casa) > 0.0025) boneco.teleport(casa);
    }

    private static double dps(Deque<Golpe> g) {
        if (g.isEmpty()) return 0;
        double soma = 0;
        for (Golpe x : g) soma += x.dano();
        double segundos = Math.max(1, (g.peekLast().quando() - g.peekFirst().quando()) / 1000.0);
        return soma / segundos;
    }

    @Override
    protected void aoTick() {
        if (!ancorar.isEmpty()) {
            long t = System.currentTimeMillis();
            ancorar.entrySet().removeIf(en -> {
                Entity b = Bukkit.getEntity(en.getKey());
                if (b != null) voltarAoLugar(b);
                return b == null || t > en.getValue();
            });
        }
        if (++passos % 1200 != 0) return; // a cada 5 min limpa quem não treina mais
        long agora = System.currentTimeMillis();
        golpes.values().removeIf(g -> g.isEmpty() || agora - g.peekLast().quando() > 300_000);
        treinos.values().removeIf(m -> m.values().stream().allMatch(t -> agora - t.ultimo > 1_800_000));
    }
}
