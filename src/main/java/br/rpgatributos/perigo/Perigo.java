package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.arcano.ItensMagicos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O mundo fica perigoso: monstros mais fortes a cada semana do servidor (com teto) e
 * Monstros de Elite com modificadores, barra de vida e loot melhor.
 */
public final class Perigo implements Listener {

    public static final NamespacedKey CHAVE_ELITE = new NamespacedKey("rpgatributos", "elite");
    public static final NamespacedKey CHAVE_LACAIO = new NamespacedKey("rpgatributos", "elite_lacaio");
    private static final String[] GRAUS = {"", "Elite", "Campeão", "Senhor"};

    private final RPGAtributos plugin;
    private final File arquivo;
    private long inicio;
    private final Set<UUID> elites = new HashSet<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private final Map<UUID, Integer> ultimoTruque = new HashMap<>();
    private final Set<UUID> enfurecidos = new HashSet<>();
    private int ciclo;

    public Perigo(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "mundo.yml");
        YamlConfiguration y = arquivo.exists() ? YamlConfiguration.loadConfiguration(arquivo) : new YamlConfiguration();
        inicio = y.getLong("inicio", 0);
        if (inicio <= 0) {
            inicio = System.currentTimeMillis();
            salvar();
        }
    }

    private Settings cfg() { return plugin.settings(); }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private void salvar() {
        YamlConfiguration y = arquivo.exists() ? YamlConfiguration.loadConfiguration(arquivo) : new YamlConfiguration();
        y.set("inicio", inicio);
        try { y.save(arquivo); } catch (IOException ex) { plugin.getLogger().warning("Não foi possível salvar mundo.yml"); }
    }

    // =====================================================================
    //  Idade do mundo
    // =====================================================================

    public long inicio() { return inicio; }

    public int semanas() {
        return (int) ((System.currentTimeMillis() - inicio) / (7L * 24 * 3_600_000));
    }

    /** Quanto os monstros estão mais fortes (0,1 = +10%). */
    public double forca() {
        return Math.min(cfg().perAumentoMax, semanas() * cfg().perAumentoSemana);
    }

    public double chanceElite() {
        return Math.min(0.15, cfg().perEliteChance + semanas() * cfg().perEliteChanceSemana);
    }

    /** Para o /rpgadmin: define a idade do mundo em semanas. */
    public void definirSemanas(int s) {
        inicio = System.currentTimeMillis() - Math.max(0, s) * 7L * 24 * 3_600_000;
        salvar();
    }

    private static void multiplicar(LivingEntity e, Attribute a, double fator) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * fator);
    }

    /** Aplica a idade do mundo num monstro que acabou de nascer. */
    public void envelhecer(LivingEntity e) {
        double f = forca();
        if (f <= 0) return;
        multiplicar(e, Attribute.MAX_HEALTH, 1 + f);
        multiplicar(e, Attribute.ATTACK_DAMAGE, 1 + f);
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) e.setHealth(max.getValue());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r != CreatureSpawnEvent.SpawnReason.NATURAL && r != CreatureSpawnEvent.SpawnReason.PATROL
                && r != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) return;
        if (!(e.getEntity() instanceof Monster m)) return;
        envelhecer(m);
        if (rnd().nextDouble() < chanceElite() * plugin.ceu().multiplicadorElite(m.getWorld())) {
            double x = rnd().nextDouble();
            tornarElite(m, x < 0.75 ? 1 : x < 0.95 ? 2 : 3);
        }
    }

    // =====================================================================
    //  Elites
    // =====================================================================

    public static boolean ehElite(Entity e) {
        return e != null && e.getPersistentDataContainer().has(CHAVE_ELITE);
    }

    public static Set<Afixo> afixos(Entity e) {
        Set<Afixo> s = EnumSet.noneOf(Afixo.class);
        String t = e.getPersistentDataContainer().get(CHAVE_ELITE, PersistentDataType.STRING);
        if (t == null || t.isEmpty()) return s;
        for (String p : t.split(",")) {
            Afixo a = Afixo.porNome(p);
            if (a != null) s.add(a);
        }
        return s;
    }

    /** Transforma o monstro num Elite com {@code n} modificadores aleatórios (1 a 3). */
    public void tornarElite(LivingEntity e, int n) {
        List<Afixo> todos = new ArrayList<>(List.of(Afixo.values()));
        if (e instanceof Creeper) todos.remove(Afixo.EXPLOSIVO);
        Collections.shuffle(todos);
        // A estação puxa um modificador (inverno: Gélido, verão: Incendiário...).
        Afixo daEstacao = plugin.estacoes().afixoDaEstacao();
        if (daEstacao != null && todos.remove(daEstacao) && rnd().nextDouble() < 0.4) todos.addFirst(daEstacao);
        else if (daEstacao != null) todos.add(daEstacao);
        tornarElite(e, EnumSet.copyOf(todos.subList(0, Math.max(1, Math.min(3, n)))));
    }

    public void tornarElite(LivingEntity e, Set<Afixo> afs) {
        int n = afs.size();
        StringBuilder id = new StringBuilder();
        for (Afixo a : afs) id.append(id.isEmpty() ? "" : ",").append(a.name());
        e.getPersistentDataContainer().set(CHAVE_ELITE, PersistentDataType.STRING, id.toString());
        multiplicar(e, Attribute.MAX_HEALTH, 2 + 0.75 * n);
        multiplicar(e, Attribute.ATTACK_DAMAGE, 1.2 + 0.15 * n);
        if (afs.contains(Afixo.VELOZ)) multiplicar(e, Attribute.MOVEMENT_SPEED, 1.4);
        if (afs.contains(Afixo.BLINDADO)) {
            AttributeInstance a = e.getAttribute(Attribute.ARMOR);
            if (a != null) a.setBaseValue(a.getBaseValue() + 12);
            AttributeInstance t = e.getAttribute(Attribute.ARMOR_TOUGHNESS);
            if (t != null) t.setBaseValue(t.getBaseValue() + 4);
            AttributeInstance k = e.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (k != null) k.setBaseValue(0.8);
        }
        if (afs.contains(Afixo.GIGANTE)) {
            AttributeInstance s = e.getAttribute(Attribute.SCALE);
            if (s != null) s.setBaseValue(1.6);
            multiplicar(e, Attribute.MAX_HEALTH, 1.5);
            multiplicar(e, Attribute.ATTACK_DAMAGE, 1.4);
            multiplicar(e, Attribute.MOVEMENT_SPEED, 0.85);
        }
        if (afs.contains(Afixo.INCENDIARIO)) {
            e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        }
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) e.setHealth(max.getValue());
        Component nome = Component.text("★".repeat(n) + " " + GRAUS[n] + " ", corGrau(n), TextDecoration.BOLD);
        boolean primeiro = true;
        for (Afixo a : afs) {
            nome = nome.append(Component.text((primeiro ? "" : " ") + a.nome(), a.cor()));
            primeiro = false;
        }
        e.customName(nome);
        e.setCustomNameVisible(true);
        e.setRemoveWhenFarAway(true);
        elites.add(e.getUniqueId());
        e.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, e.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
    }

    private static NamedTextColor corGrau(int n) {
        return n >= 3 ? NamedTextColor.DARK_RED : n == 2 ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
    }

    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) if (ehElite(x)) elites.add(x.getUniqueId());
    }

    /** A cada meio segundo: poderes dos Elites e as barras de vida. */
    public void tick() {
        ciclo++;
        int agora = Bukkit.getCurrentTick();
        Iterator<UUID> it = elites.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity ent = Bukkit.getEntity(id);
            if (!(ent instanceof LivingEntity e) || !e.isValid() || e.isDead()) {
                it.remove();
                BossBar b = barras.remove(id);
                if (b != null) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(b);
                enfurecidos.remove(id);
                ultimoTruque.remove(id);
                continue;
            }
            Set<Afixo> afs = afixos(e);
            AttributeInstance maxI = e.getAttribute(Attribute.MAX_HEALTH);
            double max = maxI == null ? 20 : maxI.getValue();
            if (afs.contains(Afixo.REGENERANTE) && e.getHealth() < max) e.setHealth(Math.min(max, e.getHealth() + max * 0.0075));
            if (afs.contains(Afixo.ENFURECIDO) && e.getHealth() < max * 0.3 && enfurecidos.add(id)) {
                e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
                e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
                e.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 8, 0.4, 0.3, 0.4);
                e.getWorld().playSound(e.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
            }
            LivingEntity alvo = e instanceof Mob mob ? mob.getTarget() : null;
            int ultimo = ultimoTruque.getOrDefault(id, 0);
            if (alvo != null && alvo.isValid() && alvo.getWorld().equals(e.getWorld()) && agora - ultimo > 200) {
                if (afs.contains(Afixo.FANTASMA) && alvo.getLocation().distanceSquared(e.getLocation()) < 24 * 24) {
                    ultimoTruque.put(id, agora);
                    e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
                    Location atras = alvo.getLocation().clone().add(alvo.getLocation().getDirection().setY(0).normalize().multiply(-2));
                    if (atras.getBlock().isPassable() && atras.clone().add(0, 1, 0).getBlock().isPassable()) {
                        e.getWorld().spawnParticle(Particle.PORTAL, e.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.3);
                        e.teleport(atras);
                    }
                } else if (afs.contains(Afixo.INVOCADOR) && agora - ultimo > 300) {
                    ultimoTruque.put(id, agora);
                    for (int i = 0; i < 2; i++) {
                        Location l = e.getLocation().add(rnd().nextDouble(-2, 2), 0, rnd().nextDouble(-2, 2));
                        Entity lacaio = e.getWorld().spawnEntity(l, e.getType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
                        lacaio.getPersistentDataContainer().set(CHAVE_LACAIO, PersistentDataType.BYTE, (byte) 1);
                        if (lacaio instanceof Mob m) m.setTarget(alvo);
                    }
                    e.getWorld().spawnParticle(Particle.WITCH, e.getLocation().add(0, 1, 0), 30, 1, 0.5, 1, 0.05);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 1f);
                }
            }
            if (ciclo % 4 == 0) e.getWorld().spawnParticle(Particle.DUST, e.getLocation().add(0, e.getHeight() + 0.3, 0), 2, 0.2, 0.1, 0.2, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(afs.isEmpty() ? 0xFFD23F : afs.iterator().next().cor().value()), 1.2f));
            atualizarBarra(e, afs.size(), max);
        }
    }

    private void atualizarBarra(LivingEntity e, int n, double max) {
        BossBar b = barras.computeIfAbsent(e.getUniqueId(), k -> BossBar.bossBar(Component.empty(), 1, n >= 3 ? BossBar.Color.RED
                : n == 2 ? BossBar.Color.YELLOW : BossBar.Color.WHITE, BossBar.Overlay.PROGRESS));
        Component nome = e.customName();
        b.name((nome == null ? Component.text("Elite") : nome).append(Component.text("   ♥ " + (int) Math.ceil(e.getHealth()) + " / " + (int) max,
                NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)));
        b.progress((float) Math.max(0, Math.min(1, e.getHealth() / max)));
        for (Player p : e.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(e.getLocation()) < 20 * 20) p.showBossBar(b);
            else p.hideBossBar(b);
        }
    }

    /** Tira todas as barras (ao desligar). */
    public void parar() {
        for (BossBar b : barras.values()) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(b);
        barras.clear();
    }

    // ---------- poderes nos golpes ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGolpear(EntityDamageByEntityEvent e) {
        Entity atacante = e.getDamager();
        if (atacante instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) atacante = atirador;
        if (ehElite(atacante) && e.getEntity() instanceof LivingEntity alvo) {
            Set<Afixo> afs = afixos(atacante);
            if (afs.contains(Afixo.VAMPIRO) && atacante instanceof LivingEntity le) {
                AttributeInstance max = le.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) le.setHealth(Math.min(max.getValue(), le.getHealth() + e.getFinalDamage() * 0.5));
                le.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, le.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0.1);
            }
            if (afs.contains(Afixo.VENENOSO)) {
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            }
            if (afs.contains(Afixo.GELIDO)) {
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), alvo.getMaxFreezeTicks() + 40));
            }
            if (afs.contains(Afixo.INCENDIARIO)) alvo.setFireTicks(Math.max(alvo.getFireTicks(), 80));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.MAGIC && ehElite(e.getEntity()) && afixos(e.getEntity()).contains(Afixo.ANTIMAGIA)) {
            e.setDamage(e.getDamage() * 0.3);
            e.getEntity().getWorld().spawnParticle(Particle.ENCHANTED_HIT, e.getEntity().getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.1);
        }
    }

    // ---------- morte e loot ----------

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (!ehElite(m)) return;
        Set<Afixo> afs = afixos(m);
        int n = Math.max(1, afs.size());
        if (afs.contains(Afixo.EXPLOSIVO)) {
            Location l = m.getLocation();
            l.getWorld().spawnParticle(Particle.FLAME, l.clone().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.05);
            l.getWorld().playSound(l, Sound.ENTITY_CREEPER_PRIMED, 1f, 0.8f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> l.getWorld().createExplosion(l, 3f, false, false), 30L);
        }
        Player p = m.getKiller();
        if (p == null) return;
        e.setDroppedExp(e.getDroppedExp() * (3 + 2 * n) + 10);
        List<ItemStack> loot = e.getDrops();
        loot.add(new ItemStack(Material.EMERALD, 2 + rnd().nextInt(3 * n)));
        if (rnd().nextDouble() < 0.5) loot.add(Reagente.PO_ARCANO.criar(1 + rnd().nextInt(3)));
        if (rnd().nextDouble() < 0.1 * n) {
            Gema[] gs = Gema.values();
            loot.add(gs[rnd().nextInt(gs.length)].criar(1, 1));
        }
        if (rnd().nextDouble() < 0.06 * n) loot.add(Raro.FRAGMENTO_DE_FORJA.criar(1));
        if (n >= 2 && rnd().nextDouble() < 0.04 * n) loot.add(Raro.PEDRA_DE_PROTECAO.criar(1));
        if (n >= 3 && rnd().nextDouble() < 0.02) loot.add(ItensMagicos.tomoAleatorio());
        plugin.titulos().registrar(p, "elites", 1);
        if (n >= 3) {
            Bukkit.broadcast(Component.text("★ ", NamedTextColor.DARK_RED).append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" derrotou um ", NamedTextColor.GRAY)).append(m.customName() == null ? Component.text("Senhor") : m.customName())
                    .append(Component.text("!", NamedTextColor.GRAY)));
        }
    }
}
