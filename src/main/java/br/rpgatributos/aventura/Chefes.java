package br.rpgatributos.aventura;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefe.Habilidade;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Os chefes vivos: nascimento, barra de vida, habilidades, fúria, proteções
 * contra trapaça (coleira no altar, imunidade a queda/lava/sufocar) e recompensas.
 */
public final class Chefes implements Listener {

    public static final NamespacedKey CHAVE_CHEFE = new NamespacedKey("rpgatributos", "chefe");
    public static final NamespacedKey CHAVE_LACAIO = new NamespacedKey("rpgatributos", "lacaio");

    /** Jogadores até essa distância contam como "na luta". */
    private static final double RAIO_LUTA = 40;
    /** Jogadores até essa distância veem a barra de vida. */
    private static final double RAIO_BARRA = 50;
    /** Se o chefe for puxado para mais longe que isso do altar, volta. */
    private static final double COLEIRA = 40;
    /** Sem nenhum jogador perto por esse tempo, o chefe vai embora. */
    private static final int TICKS_ABANDONADO = 1200;
    private static final int MAX_LACAIOS = 8;
    private static final double VIDA_MAXIMA = 1000;

    private static final Set<DamageCause> IMUNE = EnumSet.of(
            DamageCause.SUFFOCATION, DamageCause.FALL, DamageCause.DROWNING, DamageCause.CRAMMING,
            DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA,
            DamageCause.CONTACT, DamageCause.FLY_INTO_WALL, DamageCause.FREEZE);

    private static final class Ativo {
        final Chefe chefe;
        final LivingEntity entidade;
        final Location altar;
        final BossBar barra;
        final Set<UUID> veem = new HashSet<>();
        final Map<UUID, Double> dano = new LinkedHashMap<>();
        final Map<Habilidade, Integer> prontaEm = new EnumMap<>(Habilidade.class);
        final List<UUID> lacaios = new ArrayList<>();
        int abandonado;
        boolean furia;

        Ativo(Chefe chefe, LivingEntity entidade, Location altar, BossBar barra) {
            this.chefe = chefe;
            this.entidade = entidade;
            this.altar = altar;
            this.barra = barra;
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Ativo> ativos = new HashMap<>();
    private int ciclo;

    public Chefes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    public boolean ocupado(Location altar) {
        for (Ativo a : ativos.values()) if (a.altar.equals(altar)) return true;
        return false;
    }

    public static boolean ehChefe(Entity e) {
        return e != null && e.getPersistentDataContainer().has(CHAVE_CHEFE);
    }

    public static boolean ehLacaio(Entity e) {
        return e != null && e.getPersistentDataContainer().has(CHAVE_LACAIO);
    }

    private static boolean valido(Player p) {
        return p.isOnline() && !p.isDead()
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE);
    }

    private static List<Player> perto(Location l, double raio) {
        List<Player> lista = new ArrayList<>();
        for (Player p : l.getWorld().getPlayers()) {
            if (valido(p) && p.getLocation().distanceSquared(l) <= raio * raio) lista.add(p);
        }
        return lista;
    }

    private static Player maisProximo(List<Player> lista, Location l) {
        Player melhor = null;
        double d = Double.MAX_VALUE;
        for (Player p : lista) {
            double x = p.getLocation().distanceSquared(l);
            if (x < d) { d = x; melhor = p; }
        }
        return melhor;
    }

    // =====================================================================
    //  Invocar
    // =====================================================================

    public void invocar(Chefe c, Location altar, Player quem) {
        invocar(c, altar, quem, true);
    }

    /**
     * Invoca o chefe em cima do bloco {@code altar} (ele fica preso perto dali).
     * {@code anunciar}: avisa o servidor inteiro (o Altar Ritualístico avisa; a masmorra não).
     */
    public LivingEntity invocar(Chefe c, Location altar, Player quem, boolean anunciar) {
        World w = altar.getWorld();
        Location onde = altar.clone().add(0.5, 1.05, 0.5);
        int jogadores = Math.max(1, perto(onde, RAIO_LUTA).size());
        // Cada jogador a mais: +60% de vida.
        double vida = Math.min(VIDA_MAXIMA, c.vida() * (1 + 0.6 * (jogadores - 1)));

        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) c.tipo().getEntityClass();
        LivingEntity boss = w.spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> configurar(e, c, vida));

        BossBar barra = BossBar.bossBar(Component.text("☠ " + c.nome(), c.cor()), 1f, c.corBarra(), BossBar.Overlay.NOTCHED_20);
        ativos.put(boss.getUniqueId(), new Ativo(c, boss, altar.clone(), barra));

        w.strikeLightningEffect(onde);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, onde, 1);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, onde, 80, 1, 1.5, 1, 0.05);
        w.playSound(onde, Sound.ENTITY_WITHER_SPAWN, 1.2f, c.nivel() >= 3 ? 0.7f : 1f);
        for (Player p : perto(onde, RAIO_BARRA)) {
            p.showTitle(Title.title(Component.text("☠ " + c.nome() + " ☠", c.cor(), TextDecoration.BOLD),
                    Component.text("Dificuldade " + c.estrelas(), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
        if (anunciar) {
            plugin.getServer().broadcast(Component.text("☠ ", NamedTextColor.DARK_RED)
                    .append(Component.text(quem == null ? "Alguém" : quem.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" invocou ", NamedTextColor.GRAY))
                    .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                    .append(Component.text(" no Altar Ritualístico!", NamedTextColor.GRAY)));
        }
        return boss;
    }

    private static void base(LivingEntity e, Attribute a, double valor) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(valor);
    }

    private void configurar(LivingEntity e, Chefe c, double vida) {
        e.customName(Component.text("☠ " + c.nome() + " ☠", c.cor(), TextDecoration.BOLD));
        e.setCustomNameVisible(true);
        base(e, Attribute.MAX_HEALTH, vida);
        e.setHealth(vida);
        base(e, Attribute.SCALE, c.escala());
        base(e, Attribute.ATTACK_DAMAGE, c.dano());
        base(e, Attribute.ARMOR, c.armadura());
        base(e, Attribute.KNOCKBACK_RESISTANCE, 1);
        base(e, Attribute.FOLLOW_RANGE, 48);
        e.setRemoveWhenFarAway(false);
        e.setPersistent(false); // se o chunk descarregar, o chefe some (sem chefe perdido no mundo)
        e.getPersistentDataContainer().set(CHAVE_CHEFE, PersistentDataType.STRING, c.id());

        EntityEquipment eq = e.getEquipment();
        if (eq != null) {
            if (c == Chefe.LICH) {
                eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET)); // coroa (e não queima no sol)
                ItemStack arco = new ItemStack(Material.BOW);
                arco.addUnsafeEnchantment(Enchantment.POWER, 3);
                eq.setItemInMainHand(arco);
            } else if (c == Chefe.ARAUTO) {
                eq.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                eq.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            }
            eq.setHelmetDropChance(0);
            eq.setItemInMainHandDropChance(0);
        }
        if (c == Chefe.LICH) e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
    }

    // =====================================================================
    //  A cada meio segundo
    // =====================================================================

    public void tick() {
        ciclo++;
        int agora = Bukkit.getCurrentTick();
        Iterator<Ativo> it = ativos.values().iterator();
        while (it.hasNext()) {
            Ativo a = it.next();
            LivingEntity e = a.entidade;
            if (!e.isValid() || e.isDead()) {
                encerrar(a); // some sem o evento de morte (chunk descarregou etc.): tira barra e lacaios
                it.remove();
                continue;
            }
            List<Player> naLuta = perto(e.getLocation(), RAIO_LUTA);
            atualizarBarra(a);

            if (e.getLocation().distanceSquared(a.altar) > COLEIRA * COLEIRA) {
                e.teleport(a.altar.clone().add(0.5, 1.05, 0.5));
                e.getWorld().spawnParticle(Particle.REVERSE_PORTAL, e.getLocation(), 40, 0.5, 1, 0.5, 0.1);
            }

            if (naLuta.isEmpty()) {
                a.abandonado += 10;
                if (a.abandonado >= TICKS_ABANDONADO) {
                    e.getWorld().spawnParticle(Particle.LARGE_SMOKE, e.getLocation(), 60, 1, 1.5, 1, 0.02);
                    for (Player p : perto(e.getLocation(), 100)) {
                        p.sendMessage(Component.text("☠ " + a.chefe.nome() + " voltou para as sombras.", NamedTextColor.GRAY));
                    }
                    encerrar(a);
                    e.remove();
                    it.remove();
                }
                continue;
            }
            a.abandonado = 0;

            if (e instanceof Mob mob) {
                LivingEntity alvo = mob.getTarget();
                if (!(alvo instanceof Player p) || !valido(p) || p.getLocation().distanceSquared(e.getLocation()) > 32 * 32) {
                    mob.setTarget(maisProximo(naLuta, e.getLocation()));
                }
            }

            double vidaRel = e.getHealth() / maxVida(e);
            if (!a.furia && vidaRel < 0.3) furia(a);
            if (ciclo % 2 == 0) tentarHabilidade(a, naLuta, agora, vidaRel);
            a.lacaios.removeIf(id -> Bukkit.getEntity(id) == null);
        }
    }

    private static double maxVida(LivingEntity e) {
        AttributeInstance i = e.getAttribute(Attribute.MAX_HEALTH);
        return i == null ? 20 : i.getValue();
    }

    private void atualizarBarra(Ativo a) {
        LivingEntity e = a.entidade;
        double max = maxVida(e);
        a.barra.progress((float) Math.max(0, Math.min(1, e.getHealth() / max)));
        a.barra.name(Component.text("☠ " + a.chefe.nome() + (a.furia ? " (FÚRIA)" : ""), a.chefe.cor(), TextDecoration.BOLD)
                .append(Component.text("   ♥ " + (int) Math.ceil(e.getHealth()) + " / " + (int) max, NamedTextColor.WHITE)));
        Set<UUID> agora = new HashSet<>();
        for (Player p : e.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(e.getLocation()) <= RAIO_BARRA * RAIO_BARRA) {
                agora.add(p.getUniqueId());
                if (a.veem.add(p.getUniqueId())) p.showBossBar(a.barra);
            }
        }
        for (Iterator<UUID> it = a.veem.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            if (agora.contains(id)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.hideBossBar(a.barra);
            it.remove();
        }
    }

    private void furia(Ativo a) {
        a.furia = true;
        LivingEntity e = a.entidade;
        e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 0, false, true));
        e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, true));
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.6f);
        e.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 20, 1, 0.5, 1);
        for (Player p : perto(e.getLocation(), RAIO_BARRA)) {
            p.sendActionBar(Component.text("☠ " + a.chefe.nome() + " entrou em FÚRIA!", NamedTextColor.RED, TextDecoration.BOLD));
        }
    }

    private void tentarHabilidade(Ativo a, List<Player> naLuta, int agora, double vidaRel) {
        if (plugin.combos().equilibrio().quebrado(a.entidade)) return; // desequilibrado: não solta habilidade
        if (rnd().nextDouble() > (a.furia ? 0.6 : 0.35)) return;
        List<Habilidade> prontas = new ArrayList<>();
        for (Habilidade h : a.chefe.habilidades()) {
            if (agora < a.prontaEm.getOrDefault(h, 0)) continue;
            if (h == Habilidade.ESCUDO_DE_FERRO && vidaRel > 0.6) continue;
            prontas.add(h);
        }
        if (prontas.isEmpty()) return;
        Habilidade h = prontas.get(rnd().nextInt(prontas.size()));
        a.prontaEm.put(h, agora + (int) (h.recarga() * 20 * (a.furia ? 0.7 : 1)));
        executar(a, h, naLuta);
    }

    // =====================================================================
    //  Habilidades
    // =====================================================================

    private void executar(Ativo a, Habilidade h, List<Player> naLuta) {
        LivingEntity boss = a.entidade;
        World w = boss.getWorld();
        Location l = boss.getLocation();
        Player alvo = boss instanceof Mob m && m.getTarget() instanceof Player p ? p : maisProximo(naLuta, l);
        if (alvo == null) return;

        switch (h) {
            case TREMOR -> {
                w.spawnParticle(Particle.EXPLOSION, l, 6, 2, 0.2, 2);
                w.spawnParticle(Particle.CLOUD, l, 40, 3, 0.2, 3, 0.1);
                w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                for (Player p : perto(l, 6)) {
                    ferir(boss, p, 6);
                    empurrar(p, l, 0.6, 0.8);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                }
            }
            case ARREMESSO -> {
                Player p = maisProximo(perto(l, 5), l);
                if (p == null) return;
                w.playSound(l, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.6f);
                ferir(boss, p, 4);
                empurrar(p, l, 0.8, 1.6);
            }
            case ESCUDO_DE_FERRO -> {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                w.spawnParticle(Particle.WAX_ON, l.clone().add(0, boss.getHeight() / 2, 0), 60, 1, 1.5, 1, 0);
                w.playSound(l, Sound.ITEM_ARMOR_EQUIP_IRON, 2f, 0.5f);
            }
            case NINHADA -> {
                for (int i = 0; i < 3; i++) lacaio(a, EntityType.CAVE_SPIDER, ao(l, 2));
                w.playSound(l, Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.6f);
            }
            case TEIA -> {
                for (Player p : sortear(naLuta, 2)) {
                    plugin.arcano().temporarios().colocarSemJogador(p.getLocation().getBlock(), Material.COBWEB, 80);
                    w.spawnParticle(Particle.ITEM_COBWEB, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3);
                }
            }
            case NUVEM_VENENOSA -> {
                w.spawn(alvo.getLocation(), AreaEffectCloud.class, c -> {
                    c.setRadius(3.5f);
                    c.setDuration(140);
                    c.setColor(Color.fromRGB(0x4E9A06));
                    c.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), true);
                    if (boss instanceof ProjectileSource ps) c.setSource(ps);
                });
                w.playSound(alvo.getLocation(), Sound.ENTITY_WITCH_THROW, 1f, 0.6f);
            }
            case SALTO -> {
                Vector v = alvo.getLocation().toVector().subtract(l.toVector());
                if (v.lengthSquared() < 1) return;
                boss.setVelocity(v.normalize().multiply(1.3).setY(0.7));
                w.playSound(l, Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 1.4f);
            }
            case BOLAS_DE_FOGO -> {
                for (Player p : sortear(naLuta, 3)) {
                    for (int i = 0; i < 2; i++) {
                        Vector dir = p.getEyeLocation().toVector().subtract(boss.getEyeLocation().toVector()).normalize();
                        SmallFireball f = boss.launchProjectile(SmallFireball.class, dir.multiply(1.2));
                        f.setIsIncendiary(false); // não bota fogo no terreno
                    }
                }
                w.playSound(l, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.7f);
            }
            case ANEL_DE_FOGO -> {
                for (int i = 0; i < 48; i++) {
                    double ang = 2 * Math.PI * i / 48;
                    w.spawnParticle(Particle.FLAME, l.clone().add(Math.cos(ang) * 6, 0.3, Math.sin(ang) * 6), 2, 0.1, 0.2, 0.1, 0.01);
                }
                w.playSound(l, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.6f);
                for (Player p : perto(l, 6)) {
                    ferir(boss, p, 4);
                    p.setFireTicks(Math.max(p.getFireTicks(), 80));
                }
            }
            case METEORO -> impactoAtrasado(boss, alvo.getLocation(), 30, 3, 8, Particle.LAVA, true);
            case INVOCAR_MORTOS -> {
                for (int i = 0; i < 2; i++) lacaio(a, EntityType.ZOMBIE, ao(l, 3));
                for (int i = 0; i < 2; i++) lacaio(a, EntityType.SKELETON, ao(l, 3));
                w.spawnParticle(Particle.SCULK_SOUL, l, 40, 2, 0.5, 2, 0.02);
                w.playSound(l, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
            }
            case RAIO_SOMBRIO -> {
                feixe(boss.getEyeLocation(), alvo.getEyeLocation(), Particle.SCULK_SOUL);
                w.playSound(l, Sound.ENTITY_WITHER_SHOOT, 1f, 1.2f);
                if (boss.hasLineOfSight(alvo)) {
                    ferirMagico(boss, alvo, 5);
                    alvo.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                }
            }
            case TELEPORTE -> {
                Location destino = alvo.getLocation().add(rnd().nextDouble(-6, 6), 0, rnd().nextDouble(-6, 6));
                if (!destino.getBlock().isPassable() || !destino.clone().add(0, 1, 0).getBlock().isPassable()) return;
                w.spawnParticle(Particle.PORTAL, l.add(0, 1, 0), 50, 0.5, 1, 0.5, 0.3);
                boss.teleport(destino);
                w.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            }
            case DRENO -> {
                double curou = 0;
                for (Player p : perto(l, 10)) {
                    ferirMagico(boss, p, 3);
                    feixe(p.getLocation().add(0, 1, 0), boss.getLocation().add(0, boss.getHeight() / 2, 0), Particle.SCULK_SOUL);
                    curou += 3;
                }
                boss.setHealth(Math.min(maxVida(boss), boss.getHealth() + curou));
                w.playSound(l, Sound.ENTITY_WITHER_AMBIENT, 1f, 1.4f);
            }
            case RAJADA -> {
                w.spawnParticle(Particle.GUST_EMITTER_LARGE, l.clone().add(0, 1, 0), 1);
                w.playSound(l, Sound.ENTITY_BREEZE_WIND_BURST, 2f, 0.6f);
                for (Player p : perto(l, 8)) {
                    ferir(boss, p, 4);
                    empurrar(p, l, 1.6, 0.6);
                }
            }
            case RAIOS -> {
                for (Player p : sortear(naLuta, 3)) impactoAtrasado(boss, p.getLocation(), 20, 2.5, 7, Particle.ELECTRIC_SPARK, false);
            }
            case CICLONE -> {
                w.playSound(l, Sound.ENTITY_BREEZE_WIND_BURST, 2f, 0.4f);
                plugin.getServer().getScheduler().runTaskTimer(plugin, tarefa -> {
                    if (!boss.isValid()) { tarefa.cancel(); return; }
                    Location c = boss.getLocation();
                    w.spawnParticle(Particle.CLOUD, c, 30, 4, 1, 4, 0.1);
                    for (Player p : perto(c, 12)) {
                        Vector v = c.toVector().subtract(p.getLocation().toVector());
                        if (v.lengthSquared() > 4) p.setVelocity(v.normalize().multiply(0.45).setY(0.15));
                    }
                    if (rnd().nextInt(12) == 0) tarefa.cancel();
                }, 0L, 5L);
            }
            case GOLPE_DO_VAZIO -> {
                Vector atras = alvo.getLocation().getDirection().setY(0);
                if (atras.lengthSquared() < 1e-4) atras = new Vector(1, 0, 0);
                Location destino = alvo.getLocation().subtract(atras.normalize().multiply(1.5));
                if (!destino.getBlock().isPassable()) destino = alvo.getLocation();
                w.spawnParticle(Particle.REVERSE_PORTAL, l.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.1);
                boss.teleport(destino.setDirection(alvo.getLocation().toVector().subtract(destino.toVector())));
                w.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                ferir(boss, alvo, a.chefe.dano() * 1.2);
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0));
            }
            case VEXES -> {
                for (int i = 0; i < 3; i++) {
                    LivingEntity v = lacaio(a, EntityType.VEX, ao(l, 2).add(0, 2, 0));
                    if (v instanceof Vex vex) vex.setLimitedLifetimeTicks(600);
                }
                w.playSound(l, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 0.6f);
            }
            case ONDA_SONICA -> {
                w.playSound(l, Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1f);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!boss.isValid() || !alvo.isOnline()) return;
                    feixe(boss.getEyeLocation(), alvo.getEyeLocation(), Particle.SONIC_BOOM);
                    w.playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
                    if (boss.hasLineOfSight(alvo) && alvo.getLocation().distanceSquared(boss.getLocation()) < 20 * 20) {
                        alvo.damage(10, DamageSource.builder(DamageType.SONIC_BOOM).withCausingEntity(boss).withDirectEntity(boss).build());
                        empurrar(alvo, boss.getLocation(), 1.4, 0.4);
                    }
                }, 25L);
            }
            case ESCURIDAO -> {
                w.playSound(l, Sound.ENTITY_WARDEN_ROAR, 2f, 0.8f);
                for (Player p : perto(l, 30)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0));
                }
            }
        }
    }

    /** Aviso no chão e, depois de um tempo, impacto (meteoro ou raio). */
    private void impactoAtrasado(LivingEntity boss, Location onde, int atraso, double raio, double dano, Particle aviso, boolean fogo) {
        World w = onde.getWorld();
        Location chao = onde.clone();
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Consumer<BukkitTask>() {
            int t = 0;
            @Override
            public void accept(BukkitTask tarefa) {
                if (t < atraso) {
                    for (int i = 0; i < 16; i++) {
                        double ang = 2 * Math.PI * i / 16;
                        w.spawnParticle(Particle.DUST, chao.clone().add(Math.cos(ang) * raio, 0.1, Math.sin(ang) * raio), 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.RED, 1.2f));
                    }
                    w.spawnParticle(aviso, chao.clone().add(0, 0.2, 0), 2, 0.3, 0.1, 0.3, 0);
                    t += 5;
                    return;
                }
                tarefa.cancel();
                if (fogo) {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, chao, 1);
                    w.spawnParticle(Particle.LAVA, chao, 20, 1, 0.3, 1);
                    w.playSound(chao, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                } else {
                    w.strikeLightningEffect(chao);
                }
                for (Player p : perto(chao, raio)) {
                    ferirMagico(boss, p, dano);
                    if (fogo) p.setFireTicks(Math.max(p.getFireTicks(), 80));
                }
            }
        }, 0L, 5L);
    }

    private LivingEntity lacaio(Ativo a, EntityType tipo, Location onde) {
        if (a.lacaios.size() >= MAX_LACAIOS) return null;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        LivingEntity e = onde.getWorld().spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, l -> {
            l.getPersistentDataContainer().set(CHAVE_LACAIO, PersistentDataType.STRING, a.chefe.id());
            l.setPersistent(false);
            l.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        });
        a.lacaios.add(e.getUniqueId());
        if (e instanceof Mob m) m.setTarget(maisProximo(perto(onde, RAIO_LUTA), onde));
        onde.getWorld().spawnParticle(Particle.LARGE_SMOKE, onde, 10, 0.3, 0.5, 0.3, 0.02);
        return e;
    }

    private static Location ao(Location l, double raio) {
        Location x = l.clone().add(rnd().nextDouble(-raio, raio), 0, rnd().nextDouble(-raio, raio));
        if (!x.getBlock().isPassable()) return l.clone();
        return x;
    }

    private static List<Player> sortear(List<Player> lista, int qtd) {
        List<Player> copia = new ArrayList<>(lista);
        java.util.Collections.shuffle(copia);
        return copia.subList(0, Math.min(qtd, copia.size()));
    }

    private static void ferir(LivingEntity boss, Player p, double dano) {
        p.damage(dano, boss);
    }

    private static void ferirMagico(LivingEntity boss, Player p, double dano) {
        p.damage(dano, DamageSource.builder(DamageType.MAGIC).withCausingEntity(boss).withDirectEntity(boss).build());
    }

    private static void empurrar(LivingEntity e, Location de, double forca, double y) {
        Vector v = e.getLocation().toVector().subtract(de.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1));
        e.setVelocity(v.normalize().multiply(forca).setY(y));
    }

    private static void feixe(Location de, Location ate, Particle p) {
        Vector passo = ate.toVector().subtract(de.toVector());
        double dist = passo.length();
        if (dist < 0.1) return;
        passo.normalize().multiply(0.5);
        Location l = de.clone();
        for (double d = 0; d < dist; d += 0.5) {
            l.getWorld().spawnParticle(p, l, 1, 0, 0, 0, 0);
            l.add(passo);
        }
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    /** Quem bateu (direto, com flecha ou com magia) entra na divisão das recompensas. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerirChefe(EntityDamageByEntityEvent e) {
        Ativo a = ativos.get(e.getEntity().getUniqueId());
        if (a == null) return;
        Player p = null;
        if (e.getDamager() instanceof Player x) p = x;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player x) p = x;
        else if (e.getDamageSource().getCausingEntity() instanceof Player x) p = x;
        if (p != null) a.dano.merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        Entity ent = e.getEntity();
        if (!ehChefe(ent)) return;
        if (e.getCause() == DamageCause.VOID) {
            e.setCancelled(true);
            Ativo a = ativos.get(ent.getUniqueId());
            if (a != null) ent.teleport(a.altar.clone().add(0.5, 1.05, 0.5));
            return;
        }
        if (IMUNE.contains(e.getCause())) {
            e.setCancelled(true);
            return;
        }
        // Não apanha dos próprios lacaios nem das próprias bolas de fogo.
        if (e instanceof EntityDamageByEntityEvent ed) {
            Entity fonte = ed.getDamager();
            if (fonte instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) fonte = atirador;
            if (fonte == ent || ehLacaio(fonte)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        if (ehLacaio(morto)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            return;
        }
        Ativo a = ativos.remove(morto.getUniqueId());
        if (a == null) return;
        recompensar(a, e);
        encerrar(a);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPegarFogo(EntityCombustEvent e) {
        if (e instanceof EntityCombustByEntityEvent || e instanceof EntityCombustByBlockEvent) return;
        if (ehChefe(e.getEntity()) || ehLacaio(e.getEntity())) e.setCancelled(true); // sol não queima
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetEvent e) {
        if ((ehChefe(e.getEntity()) || ehLacaio(e.getEntity())) && e.getTarget() != null && !(e.getTarget() instanceof Player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTransformar(EntityTransformEvent e) {
        if (ehChefe(e.getEntity())) e.setCancelled(true);
    }

    // =====================================================================
    //  Fim da luta
    // =====================================================================

    private void recompensar(Ativo a, EntityDeathEvent e) {
        Chefe c = a.chefe;
        int n = c.nivel();
        e.getDrops().clear();
        e.setDroppedExp(80 * n);

        List<Player> participantes = new ArrayList<>();
        for (Map.Entry<UUID, Double> en : a.dano.entrySet()) {
            Player p = Bukkit.getPlayer(en.getKey());
            if (p != null && p.getWorld().equals(e.getEntity().getWorld())
                    && p.getLocation().distanceSquared(e.getEntity().getLocation()) < 100 * 100) {
                participantes.add(p);
            }
        }
        if (participantes.isEmpty() && e.getEntity().getKiller() != null) participantes.add(e.getEntity().getKiller());

        // Saque (um só, para todos dividirem) com fragmentos extras por jogador a mais.
        List<ItemStack> saque = e.getDrops();
        saque.add(c.nucleo().criar());
        int[][] fragmentos = {{1, 2}, {2, 3}, {3, 5}, {5, 8}};
        double[] pedra = {0.25, 0.35, 0.5, 0.75};
        double[] essencia = {0.05, 0.10, 0.2, 0.5};
        int frag = rnd().nextInt(fragmentos[n - 1][0], fragmentos[n - 1][1] + 1) + Math.max(0, participantes.size() - 1);
        saque.add(Raro.FRAGMENTO_DE_FORJA.criar(frag));
        if (rnd().nextDouble() < pedra[n - 1]) saque.add(Raro.PEDRA_DE_PROTECAO.criar());
        if (rnd().nextDouble() < essencia[n - 1]) saque.add(Raro.ESSENCIA_PRIMORDIAL.criar());
        if (rnd().nextDouble() < 0.05 + 0.03 * n) saque.add(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio());
        for (Chefe.Saque s : c.saque()) saque.add(new ItemStack(s.material(), rnd().nextInt(s.min(), s.max() + 1)));

        Location l = e.getEntity().getLocation();
        l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 1, 0), 150, 1, 1.5, 1, 0.5);
        l.getWorld().playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);

        List<String> nomes = new ArrayList<>();
        for (Player p : participantes) {
            nomes.add(p.getName());
            int dano = (int) Math.round(a.dano.getOrDefault(p.getUniqueId(), 0.0));
            p.sendMessage(Component.text("☠ Você ajudou a derrotar ", NamedTextColor.GOLD)
                    .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                    .append(Component.text(" (" + dano + " de dano). O saque caiu onde ele morreu.", NamedTextColor.GRAY)));
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                plugin.stats().darXp(p, Skill.COMBATE, 150.0 * n);
            }
            plugin.titulos().registrar(p, "chefes", 1);
            plugin.titulos().registrar(p, "chefe_" + c.id(), 1);
            plugin.mercadores().contratoCumprido(p, c);
        }
        plugin.getServer().broadcast(Component.text("☠ ", NamedTextColor.DARK_RED)
                .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                .append(Component.text(" foi derrotado por ", NamedTextColor.GRAY))
                .append(Component.text(nomes.isEmpty() ? "ninguém" : String.join(", ", nomes), NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GRAY)));
    }

    /** Esconde a barra e tira os lacaios. */
    private void encerrar(Ativo a) {
        for (UUID id : a.veem) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.hideBossBar(a.barra);
        }
        a.veem.clear();
        for (UUID id : a.lacaios) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        a.lacaios.clear();
    }

    public void removerTodos() {
        for (Ativo a : ativos.values()) {
            encerrar(a);
            a.entidade.remove();
        }
        ativos.clear();
    }

    public String resumo() {
        StringBuilder sb = new StringBuilder();
        for (Ativo a : ativos.values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(a.chefe.nome()).append(" (♥ ").append((int) a.entidade.getHealth()).append(")");
        }
        return sb.isEmpty() ? "nenhum" : sb.toString();
    }
}
