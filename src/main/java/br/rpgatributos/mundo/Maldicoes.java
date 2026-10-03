package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.perigo.Afixo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maldições jogáveis: licantropia (lobisomem) e vampirismo. Pega ao ser mordido por um
 * lobisomem (noites de lua cheia) ou vampiro (noites de lua nova). Dão poderes e fraquezas;
 * quem não quiser cura com o Elixir da Purificação.
 */
public final class Maldicoes implements Listener {

    public enum Maldicao { LICANTROPIA, VAMPIRISMO }

    private static final NamespacedKey CHAVE_MOB = new NamespacedKey("rpgatributos", "criatura_maldita");

    private final RPGAtributos plugin;
    private final NamespacedKey kMaldicao, kArmadura, kResistencia, kTamanho;
    private final Set<UUID> transformados = new HashSet<>();
    /** Quem pediu para se transformar (lobisomem fora da lua cheia). */
    private final Set<UUID> querem = new HashSet<>();
    private final Set<UUID> criaturas = new HashSet<>();

    public Maldicoes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kMaldicao = new NamespacedKey(plugin, "maldicao");
        this.kArmadura = new NamespacedKey(plugin, "lobisomem_armadura");
        this.kResistencia = new NamespacedKey(plugin, "lobisomem_resistencia");
        this.kTamanho = new NamespacedKey(plugin, "lobisomem_tamanho");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    public Maldicao de(Player p) {
        String s = p.getPersistentDataContainer().get(kMaldicao, PersistentDataType.STRING);
        if (s == null) return null;
        try { return Maldicao.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    public boolean transformado(Player p) {
        return transformados.contains(p.getUniqueId());
    }

    public void amaldicoar(Player p, Maldicao m) {
        if (m == null) {
            curar(p);
            return;
        }
        p.getPersistentDataContainer().set(kMaldicao, PersistentDataType.STRING, m.name());
        boolean lobo = m == Maldicao.LICANTROPIA;
        p.showTitle(Title.title(Component.text(lobo ? "🐺 Licantropia" : "🦇 Vampirismo", lobo ? NamedTextColor.GOLD : NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("Você foi amaldiçoado... /maldicao", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), lobo ? Sound.ENTITY_WOLF_GROWL : Sound.ENTITY_BAT_AMBIENT, 1f, 0.6f);
        p.sendMessage(Component.text(lobo
                ? "🐺 Na lua cheia você vira lobisomem (força, velocidade, cura; sem armadura e sem magia). Ouro te machuca mais. Cura: Elixir da Purificação."
                : "🦇 Você rouba vida ao golpear e enxerga no escuro, mas o sol te queima sem capacete. Cura: Elixir da Purificação.",
                lobo ? NamedTextColor.GOLD : NamedTextColor.RED));
    }

    public void curar(Player p) {
        if (de(p) == null) return;
        destransformar(p);
        querem.remove(p.getUniqueId());
        p.getPersistentDataContainer().remove(kMaldicao);
        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.sendMessage(Component.text("✦ A maldição foi purificada.", NamedTextColor.AQUA));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
    }

    /** /maldicao transformar: o lobisomem se transforma à noite por vontade própria. */
    public String alternarTransformacao(Player p) {
        if (de(p) != Maldicao.LICANTROPIA) return "Só lobisomens se transformam.";
        if (!Ceu.noite(p.getWorld())) return "Só dá para se transformar à noite.";
        if (plugin.ceu().luaCheia()) return "Na lua cheia a fera manda: não dá para voltar.";
        if (querem.remove(p.getUniqueId())) {
            destransformar(p);
            return null;
        }
        querem.add(p.getUniqueId());
        return null;
    }

    // =====================================================================
    //  Criaturas malditas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(e.getEntity() instanceof Zombie z)) return;
        if (!z.getWorld().equals(Bukkit.getWorlds().getFirst())) return;
        if (plugin.ceu().luaCheia() && rnd().nextDouble() < 0.06) {
            e.setCancelled(true);
            lobisomem(z.getLocation());
        } else if (plugin.ceu().luaNova() && rnd().nextDouble() < 0.06) {
            vampiro(z);
        }
    }

    public LivingEntity lobisomem(org.bukkit.Location l) {
        Wolf w = l.getWorld().spawn(l, Wolf.class, CreatureSpawnEvent.SpawnReason.CUSTOM, x -> {
            AttributeInstance escala = x.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(1.6);
            AttributeInstance vida = x.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) vida.setBaseValue(40);
            x.setHealth(40);
            AttributeInstance dano = x.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue(8);
            AttributeInstance vel = x.getAttribute(Attribute.MOVEMENT_SPEED);
            if (vel != null) vel.setBaseValue(0.38);
            x.customName(Component.text("🐺 Lobisomem", NamedTextColor.GOLD, TextDecoration.BOLD));
            x.setCustomNameVisible(true);
            x.getPersistentDataContainer().set(CHAVE_MOB, PersistentDataType.STRING, Maldicao.LICANTROPIA.name());
        });
        criaturas.add(w.getUniqueId());
        l.getWorld().playSound(l, Sound.ENTITY_WOLF_GROWL, 1.5f, 0.5f);
        return w;
    }

    public void vampiro(Zombie z) {
        plugin.perigo().tornarElite(z, EnumSet.of(Afixo.VAMPIRO, Afixo.FANTASMA));
        z.customName(Component.text("🦇 Vampiro", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        z.getPersistentDataContainer().set(CHAVE_MOB, PersistentDataType.STRING, Maldicao.VAMPIRISMO.name());
        ItemStack capa = new ItemStack(Material.LEATHER_CHESTPLATE);
        capa.editMeta(LeatherArmorMeta.class, m -> m.setColor(Color.fromRGB(0x2B0A0A)));
        z.getEquipment().setChestplate(capa);
        z.getEquipment().setChestplateDropChance(0);
        criaturas.add(z.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMorder(EntityDamageByEntityEvent e) {
        // Mordida que passa a maldição.
        if (e.getEntity() instanceof Player p && p.getGameMode() == GameMode.SURVIVAL && de(p) == null) {
            String tipo = e.getDamager().getPersistentDataContainer().get(CHAVE_MOB, PersistentDataType.STRING);
            if (tipo != null && rnd().nextDouble() < plugin.settings().malChanceMordida) {
                try { amaldicoar(p, Maldicao.valueOf(tipo)); } catch (IllegalArgumentException ignored) { }
            }
        }
        // Poderes e fraquezas de quem é amaldiçoado.
        Entity atacante = e.getDamager();
        if (atacante instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) atacante = atirador;
        if (atacante instanceof Player p && de(p) == Maldicao.VAMPIRISMO && e.getDamager() == p) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + e.getFinalDamage() * 0.15));
        }
        if (e.getEntity() instanceof Player v && de(v) == Maldicao.LICANTROPIA && atacante instanceof LivingEntity le) {
            Material arma = le.getEquipment() == null ? Material.AIR : le.getEquipment().getItemInMainHand().getType();
            if (arma.name().startsWith("GOLDEN_")) e.setDamage(e.getDamage() * 1.5);
        }
    }

    // =====================================================================
    //  A cada 2 segundos
    // =====================================================================

    public void tick() {
        // Lobisomens e vampiros somem com o dia.
        for (Iterator<UUID> it = criaturas.iterator(); it.hasNext(); ) {
            Entity e = Bukkit.getEntity(it.next());
            if (e == null || !e.isValid()) { it.remove(); continue; }
            if (!Ceu.noite(e.getWorld())) {
                e.getWorld().spawnParticle(Particle.LARGE_SMOKE, e.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
                e.remove();
                it.remove();
                continue;
            }
            if (e instanceof Wolf w && (w.getTarget() == null || !w.getTarget().isValid())) {
                Player perto = null;
                double d = 32 * 32;
                for (Player p : w.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double x = p.getLocation().distanceSquared(w.getLocation());
                    if (x < d) { d = x; perto = p; }
                }
                if (perto != null) {
                    w.setAngry(true);
                    w.setTarget(perto);
                }
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            Maldicao m = de(p);
            if (m == null) continue;
            World w = p.getWorld();
            boolean noite = Ceu.noite(w);
            if (m == Maldicao.LICANTROPIA) {
                boolean deve = noite && (plugin.ceu().luaCheia() || querem.contains(p.getUniqueId()));
                if (deve && !transformado(p)) transformar(p);
                else if (!deve && transformado(p)) { destransformar(p); querem.remove(p.getUniqueId()); }
                if (transformado(p)) {
                    efeito(p, PotionEffectType.STRENGTH, 1);
                    efeito(p, PotionEffectType.SPEED, 1);
                    efeito(p, PotionEffectType.JUMP_BOOST, 1);
                    efeito(p, PotionEffectType.REGENERATION, 0);
                    efeito(p, PotionEffectType.NIGHT_VISION, 0);
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 0,
                            new Particle.DustOptions(Color.fromRGB(0x6B4A2B), 1.4f));
                }
            } else {
                if (noite) {
                    efeito(p, PotionEffectType.NIGHT_VISION, 0);
                    efeito(p, PotionEffectType.SPEED, 0);
                } else if (p.getGameMode() == GameMode.SURVIVAL && w.getEnvironment() == World.Environment.NORMAL
                        && p.getLocation().getBlock().getLightFromSky() >= 15 && !w.hasStorm() && !p.isInWater()
                        && (p.getInventory().getHelmet() == null || p.getInventory().getHelmet().isEmpty())) {
                    p.setFireTicks(Math.max(p.getFireTicks(), 60));
                    p.sendActionBar(Component.text("🦇 O sol queima! Use um capacete ou fique na sombra.", NamedTextColor.RED));
                }
            }
        }
    }

    private static void efeito(Player p, PotionEffectType t, int nivel) {
        p.addPotionEffect(new PotionEffect(t, t == PotionEffectType.NIGHT_VISION ? 400 : 60, nivel, true, false, true));
    }

    private void transformar(Player p) {
        transformados.add(p.getUniqueId());
        modificar(p, Attribute.ARMOR, kArmadura, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        modificar(p, Attribute.ARMOR_TOUGHNESS, kResistencia, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        modificar(p, Attribute.SCALE, kTamanho, 0.15, AttributeModifier.Operation.ADD_SCALAR);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.5f, 0.5f);
        p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.03);
        p.sendActionBar(Component.text("🐺 A fera desperta! (sem armadura e sem magia)", NamedTextColor.GOLD));
    }

    private void destransformar(Player p) {
        if (!transformados.remove(p.getUniqueId())) return;
        for (Attribute a : new Attribute[]{Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS, Attribute.SCALE}) {
            AttributeInstance i = p.getAttribute(a);
            if (i == null) continue;
            i.removeModifier(kArmadura);
            i.removeModifier(kResistencia);
            i.removeModifier(kTamanho);
        }
        for (PotionEffectType t : new PotionEffectType[]{PotionEffectType.STRENGTH, PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST,
                PotionEffectType.REGENERATION}) {
            PotionEffect ef = p.getPotionEffect(t);
            if (ef != null && ef.isAmbient()) p.removePotionEffect(t);
        }
        p.sendActionBar(Component.text("Você voltou à forma humana.", NamedTextColor.GRAY));
    }

    private static void modificar(Player p, Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op) {
        AttributeInstance i = p.getAttribute(a);
        if (i == null) return;
        i.removeModifier(k);
        i.addModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
    }

    public void parar() {
        for (UUID id : Set.copyOf(transformados)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) destransformar(p);
        }
        for (UUID id : criaturas) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
    }

    /** Texto do /maldicao. */
    public void info(Player p) {
        Maldicao m = de(p);
        if (m == null) {
            p.sendMessage(Component.text("Você não tem maldição. Cuidado com lobisomens (lua cheia) e vampiros (lua nova)...", NamedTextColor.GRAY));
            return;
        }
        if (m == Maldicao.LICANTROPIA) {
            p.sendMessage(Component.text("🐺 Licantropia", NamedTextColor.GOLD, TextDecoration.BOLD));
            p.sendMessage(Component.text(" Na lua cheia (ou à noite com /maldicao transformar): força II, velocidade II,", NamedTextColor.WHITE));
            p.sendMessage(Component.text(" pulo, regeneração e visão noturna. Sem armadura e sem magias enquanto transformado.", NamedTextColor.WHITE));
            p.sendMessage(Component.text(" Fraqueza: armas de ouro causam +50% de dano em você.", NamedTextColor.RED));
        } else {
            p.sendMessage(Component.text("🦇 Vampirismo", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            p.sendMessage(Component.text(" Rouba 15% do dano corpo a corpo como vida; à noite, visão noturna e velocidade.", NamedTextColor.WHITE));
            p.sendMessage(Component.text(" Fraqueza: o sol queima se você estiver ao ar livre sem capacete.", NamedTextColor.RED));
        }
        p.sendMessage(Component.text(" Cura: Elixir da Purificação (Bancada Alquímica).", NamedTextColor.AQUA));
    }
}
