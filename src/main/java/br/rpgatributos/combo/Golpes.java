package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.party.Party;
import br.rpgatributos.sombra.Sombras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * O que cada golpe de combo faz. O dano parte do dano de ataque do jogador (arma,
 * forja, força...) vezes o multiplicador do golpe e a {@code forca} da proficiência; o
 * dano sai como golpe do jogador, então passivos de classe, efeitos da forja e
 * reações elementais também valem.
 */
public final class Golpes implements Listener {

    private static final NamespacedKey K_INVOCACAO = new NamespacedKey("rpgatributos", "invocacao_dono");
    private static final NamespacedKey K_FLECHA = new NamespacedKey("rpgatributos", "combo_flecha");
    private static final NamespacedKey K_FLECHA_DANO = new NamespacedKey("rpgatributos", "combo_flecha_dano");

    private record Marca(long ate, double bonus) {}

    private final RPGAtributos plugin;
    private final Map<UUID, Marca> marcas = new HashMap<>();
    private final Map<UUID, Long> semQueda = new HashMap<>();
    private final Map<UUID, Long> furia = new HashMap<>();
    /** Égide (lenda): até quando devolve metade do dano. */
    private final Map<UUID, Long> egide = new HashMap<>();
    private boolean refletindo;
    private final Set<BukkitTask> tarefas = new HashSet<>();
    private final Set<UUID> displays = new HashSet<>();

    Golpes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
    private static long agora() { return System.currentTimeMillis(); }

    // =====================================================================
    //  Alvos
    // =====================================================================

    /** O golpe desse jogador pode acertar essa criatura? */
    private boolean inimigo(Player p, Entity e) {
        return plugin.alvos().inimigo(p, e);
    }

    private boolean pvp(Player p, UUID outro) {
        Player o = plugin.getServer().getPlayer(outro);
        return o != null && plugin.pvpPermitido(p, o);
    }

    private List<LivingEntity> perto(Player p, Location c, double raio) {
        List<LivingEntity> l = new ArrayList<>();
        for (Entity e : c.getWorld().getNearbyEntities(c, raio, raio, raio)) {
            if (inimigo(p, e) && e.getLocation().distanceSquared(c) <= raio * raio) l.add((LivingEntity) e);
        }
        return l;
    }

    /** Inimigos num cone na frente do jogador. */
    private List<LivingEntity> cone(Player p, double alcance, double graus) {
        Vector frente = horizontal(p);
        double cos = Math.cos(Math.toRadians(graus / 2));
        List<LivingEntity> l = new ArrayList<>();
        for (LivingEntity e : perto(p, p.getLocation(), alcance)) {
            Vector para = e.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (para.lengthSquared() < 0.25 || para.normalize().dot(frente) >= cos) l.add(e);
        }
        return l;
    }

    /** Inimigos ao longo de uma linha (para estocadas e feixes). */
    private List<LivingEntity> linha(Player p, Location de, Vector dir, double comprimento, double raio) {
        Set<UUID> vistos = new HashSet<>();
        List<LivingEntity> l = new ArrayList<>();
        Vector d = dir.clone().normalize();
        for (double s = 0; s <= comprimento; s += 0.5) {
            Location ponto = de.clone().add(d.clone().multiply(s));
            if (!ponto.getBlock().isPassable()) break;
            for (LivingEntity e : perto(p, ponto, raio)) if (vistos.add(e.getUniqueId())) l.add(e);
        }
        return l;
    }

    /** A criatura que o jogador está olhando (sem parede no meio), ou null. */
    private LivingEntity alvo(Player p, double alcance) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), alcance, 0.7,
                e -> inimigo(p, e));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity le)) return null;
        RayTraceResult parede = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(),
                p.getEyeLocation().distance(le.getEyeLocation()));
        return parede != null && parede.getHitBlock() != null ? null : le;
    }

    /** O ponto do chão que o jogador está olhando. */
    private static Location mira(Player p, double alcance) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), alcance);
        Location l = r != null && r.getHitBlock() != null ? r.getHitPosition().toLocation(p.getWorld())
                : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(alcance));
        // Desce até o chão.
        for (int i = 0; i < 8 && l.clone().subtract(0, 0.1, 0).getBlock().isPassable() && l.getY() > p.getWorld().getMinHeight(); i++) l.subtract(0, 1, 0);
        return l;
    }

    private static Vector horizontal(Player p) {
        Vector v = p.getLocation().getDirection().setY(0);
        return v.lengthSquared() < 1e-4 ? new Vector(0, 0, 1) : v.normalize();
    }

    // =====================================================================
    //  Dano e efeitos
    // =====================================================================

    /** Dano base do jogador com a arma da mão. */
    private static double base(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.ATTACK_DAMAGE);
        return Math.max(2, a == null ? 2 : a.getValue());
    }

    /** Dano base de uma flecha do arco/besta da mão. */
    private static double baseArco(Player p) {
        ItemStack arma = p.getInventory().getItemInMainHand();
        int poder = arma.getEnchantmentLevel(Enchantment.POWER);
        return 6 + 1.5 * poder;
    }

    /** Ligado pelo Combos (postura, estilo e equilíbrio). */
    private Combos combos;
    /** Um golpe de combo está ferindo agora (para o número de dano subir). */
    boolean ferindo;
    /** O golpe sendo usado agora saiu no tempo certo. */
    boolean perfeitoAgora;
    /** Quem já contou como acerto no ar neste golpe (o estilo só sobe uma vez por golpe). */
    private final Set<UUID> aereosDoGolpe = new HashSet<>();

    void ligar(Combos c) {
        this.combos = c;
    }

    /** Começa um golpe novo (zera as marcas do anterior). */
    void comecar(boolean perfeito) {
        perfeitoAgora = perfeito;
        aereosDoGolpe.clear();
    }

    private static boolean voa(Entity e) {
        return e instanceof org.bukkit.entity.Flying || e instanceof org.bukkit.entity.Bat || e instanceof org.bukkit.entity.Bee
                || e instanceof org.bukkit.entity.Allay || e instanceof org.bukkit.entity.Vex || e instanceof org.bukkit.entity.Blaze
                || e instanceof org.bukkit.entity.Parrot || e instanceof org.bukkit.entity.EnderDragon || e instanceof org.bukkit.entity.Wither;
    }

    private void ferir(Player p, LivingEntity e, double valor) {
        if (!e.isValid() || e.isDead()) return;
        double v = valor;
        // No ar (lançado ou pulando): +30%, fica suspenso um instante e conta para o estilo.
        boolean noAr = !e.isOnGround() && !e.isInWater() && !voa(e) && !(e instanceof org.bukkit.entity.ArmorStand);
        if (noAr) {
            v *= 1.3;
            if (combos != null && aereosDoGolpe.add(e.getUniqueId())) {
                combos.estilo().bonus(p, 8);
                p.sendActionBar(Component.text("☁ No ar! +30%", NamedTextColor.AQUA, TextDecoration.BOLD));
            }
            e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation(), 6, 0.3, 0.1, 0.3, 0.02);
            if (!(e instanceof Player) && !Chefes.ehChefe(e)) {
                Vector vel = e.getVelocity();
                e.setVelocity(new Vector(vel.getX() * 0.4, Math.max(vel.getY(), 0.28), vel.getZ() * 0.4));
            }
        }
        e.setNoDamageTicks(0);
        ferindo = true;
        try {
            plugin.alvos().ferir(p, e, v);
        } finally {
            ferindo = false;
        }
        if (combos == null) return;
        combos.ligacoes().aoGolpe(p, e, v);
        // Equilíbrio do inimigo, cura da postura Defensiva e o "tranco" do acerto.
        double eq = v * combos.posturas().multEquilibrio(p) * (perfeitoAgora ? 1.3 : 1) * (noAr ? 1.2 : 1);
        combos.equilibrio().golpe(p, e, eq, combos.estilo());
        if (combos.posturas().curaNoGolpe(p)) {
            var max = p.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + 1));
        }
        if (!(e instanceof Player) && e.isValid()) e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4, 2, false, false, false));
    }

    private static void empurrar(LivingEntity e, Location de, double forca, double cima) {
        Vector v = e.getLocation().toVector().subtract(de.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1));
        e.setVelocity(e.getVelocity().add(v.normalize().multiply(forca).setY(cima)));
    }

    private static void efeito(LivingEntity e, PotionEffectType t, int ticks, int nivel) {
        e.addPotionEffect(new PotionEffect(t, ticks, nivel, false, true, true));
    }

    /** O alvo leva mais dano por um tempo (Rachar Escudo, Quebra-Armadura, Flecha Sombria). */
    private void marcar(LivingEntity e, int segundos, double bonus) {
        Marca atual = marcas.get(e.getUniqueId());
        if (atual != null && atual.ate > agora() && atual.bonus > bonus) bonus = atual.bonus;
        marcas.put(e.getUniqueId(), new Marca(agora() + segundos * 1000L, bonus));
        e.getWorld().spawnParticle(Particle.ENCHANTED_HIT, e.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
    }

    private void sangrar(Player p, LivingEntity e, double porSegundo, int segundos) {
        Particle.DustOptions sangue = new Particle.DustOptions(Color.fromRGB(0x9B0000), 1.2f);
        repetir(20, segundos, i -> {
            if (!e.isValid() || e.isDead()) return;
            e.getWorld().spawnParticle(Particle.DUST, e.getLocation().add(0, 1, 0), 8, 0.25, 0.4, 0.25, 0, sangue);
            ferir(p, e, porSegundo);
        });
    }

    private BukkitTask repetir(long periodo, int vezes, IntConsumer passo) {
        int[] i = {0};
        BukkitTask[] ref = new BukkitTask[1];
        ref[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (i[0] >= vezes) {
                ref[0].cancel();
                tarefas.remove(ref[0]);
                return;
            }
            passo.accept(i[0]++);
        }, periodo, periodo);
        tarefas.add(ref[0]);
        return ref[0];
    }

    private void depois(long ticks, Runnable r) {
        BukkitTask[] ref = new BukkitTask[1];
        ref[0] = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tarefas.remove(ref[0]);
            r.run();
        }, ticks);
        tarefas.add(ref[0]);
    }

    /** Salta e chama {@code aoCair} quando tocar o chão. {@code depoisDe}: ticks antes de começar a checar. */
    private void saltar(Player p, Vector impulso, int depoisDe, Consumer<Location> aoCair) {
        p.setVelocity(impulso);
        semQueda.put(p.getUniqueId(), agora() + 8000);
        int[] t = {0};
        BukkitTask[] ref = new BukkitTask[1];
        ref[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            t[0]++;
            if (!p.isOnline() || p.isDead()) {
                ref[0].cancel();
                tarefas.remove(ref[0]);
                return;
            }
            if (t[0] > depoisDe && (p.isOnGround() || p.isInWater() || t[0] > 80)) {
                ref[0].cancel();
                tarefas.remove(ref[0]);
                aoCair.accept(p.getLocation());
            }
        }, 1L, 1L);
        tarefas.add(ref[0]);
    }

    private ItemDisplay display(Location l, ItemStack item, float escala) {
        ItemStack copia = item.clone();
        copia.setAmount(1);
        ItemDisplay d = l.getWorld().spawn(l, ItemDisplay.class, x -> {
            x.setItemStack(copia);
            x.setPersistent(false);
            x.setTeleportDuration(1);
            x.setBillboard(Display.Billboard.FIXED);
            x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(escala), new AxisAngle4f()));
        });
        displays.add(d.getUniqueId());
        return d;
    }

    private void tirar(ItemDisplay d) {
        displays.remove(d.getUniqueId());
        d.remove();
    }

    private static BlockData chao(Location l) {
        Block b = l.clone().subtract(0, 1, 0).getBlock();
        return (b.isPassable() ? Material.DIRT : b.getType()).createBlockData();
    }

    private static void anel(Location c, double raio, Particle part, int pontos) {
        for (int i = 0; i < pontos; i++) {
            double a = Math.PI * 2 * i / pontos;
            c.getWorld().spawnParticle(part, c.getX() + Math.cos(a) * raio, c.getY() + 0.2, c.getZ() + Math.sin(a) * raio, 1, 0, 0, 0, 0);
        }
    }

    private static boolean falhar(Player p, String msg) {
        p.sendActionBar(Component.text("✖ " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.6f);
        return false;
    }

    /**
     * 2.51: uma lâmina voando (faca, machadinha ou a arma da mão). Acerta o primeiro inimigo (ou
     * todos, se {@code atravessa}) e chama {@code aoAcertar}; {@code aoParar} quando para (bateu,
     * acertou ou chegou longe).
     */
    private void lamina(Player p, Location de, Vector dir, double velocidade, double alcance, boolean atravessa, ItemStack visual,
                        java.util.function.BiConsumer<LivingEntity, Location> aoAcertar, Consumer<Location> aoParar) {
        ItemDisplay d = display(de, visual, 0.7f);
        Vector v = dir.clone().normalize().multiply(velocidade);
        Location pos = de.clone();
        Set<UUID> acertados = new HashSet<>();
        int passos = Math.max(1, (int) Math.ceil(alcance / velocidade));
        boolean[] fim = {false};
        Runnable parar = () -> {
            fim[0] = true;
            if (d.isValid()) tirar(d);
            if (aoParar != null) aoParar.accept(pos.clone());
        };
        repetir(1, passos, i -> {
            if (fim[0] || !d.isValid()) return;
            Location prox = pos.clone().add(v);
            if (!prox.getBlock().isPassable()) {
                parar.run();
                return;
            }
            pos.setX(prox.getX());
            pos.setY(prox.getY());
            pos.setZ(prox.getZ());
            d.teleport(pos);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) (i * 1.1), 1, 0, 0), new Vector3f(0.7f), new AxisAngle4f()));
            for (LivingEntity e : perto(p, pos, 1.1)) {
                if (!acertados.add(e.getUniqueId())) continue;
                aoAcertar.accept(e, pos.clone());
                if (!atravessa) {
                    parar.run();
                    return;
                }
            }
            if (i == passos - 1) parar.run();
        });
    }

    /** Bumerangue: vai em linha atravessando todos e volta até você, acertando de novo na volta. */
    private void vaiEVolta(Player p, Vector dir, double alcance, double danoIda, double danoVolta, Consumer<LivingEntity> extra) {
        Location inicio = p.getEyeLocation().subtract(0, 0.3, 0);
        lamina(p, inicio, dir, 1.0, alcance, true, visualLamina(p), (e, o) -> {
            ferir(p, e, danoIda);
            if (extra != null) extra.accept(e);
        }, onde -> lamina(p, onde, p.getEyeLocation().toVector().subtract(onde.toVector()), 1.0,
                Math.max(2, onde.distance(p.getEyeLocation())), true, visualLamina(p), (e, o2) -> {
                    ferir(p, e, danoVolta);
                    if (extra != null) extra.accept(e);
                }, null));
    }

    /** O que voa nos golpes de arremesso: a arma da mão (ou uma faca de ferro). */
    private static ItemStack visualLamina(Player p) {
        ItemStack m = p.getInventory().getItemInMainHand();
        return m.getType().isAir() ? new ItemStack(Material.IRON_SWORD) : m;
    }

    /** Puxa a criatura até perto do jogador. */
    private static void puxar(Player p, LivingEntity e, double forca) {
        Vector v = p.getLocation().toVector().subtract(e.getLocation().toVector());
        if (v.lengthSquared() < 1) return;
        e.setVelocity(v.normalize().multiply(forca).setY(0.35));
    }

    /** O alvo está de costas para o jogador? */
    private static boolean pelasCostas(Player p, LivingEntity e) {
        Vector olhar = e.getLocation().getDirection().setY(0), deMim = p.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);
        return olhar.lengthSquared() > 1e-4 && deMim.lengthSquared() > 1e-4 && olhar.normalize().dot(deMim.normalize()) < -0.4;
    }

    private Arrow flecha(Player p, Location de, Vector dir, float velocidade, double dano) {
        Arrow a = p.getWorld().spawnArrow(de, dir, velocidade, 0f);
        a.setShooter(p);
        a.setDamage(dano / velocidade);
        a.setCritical(true);
        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        return a;
    }

    // =====================================================================
    //  Usar
    // =====================================================================

    /** @return false se não deu para usar (sem alvo etc.): aí não gasta vigor nem recarga. */
    boolean usar(Player p, Golpe g, double forca) {
        World w = p.getWorld();
        Location l = p.getLocation();
        double d = base(p) * forca;
        double b = baseArco(p) * forca;
        switch (g) {
            // ---------------- Espada ----------------
            case ESTOCADA -> {
                Vector dir = horizontal(p);
                p.setVelocity(dir.clone().multiply(1.8).setY(0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 10, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.05);
                    for (LivingEntity e : perto(p, c, 2.0)) {
                        if (!acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 1.4 * d);
                        empurrar(e, c, 0.6, 0.2);
                    }
                });
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
            }
            case CORTE_GIRATORIO -> {
                for (LivingEntity e : perto(p, l, 3.5)) {
                    ferir(p, e, 1.2 * d);
                    empurrar(e, l, 0.8, 0.3);
                }
                anel(l.clone().add(0, 1, 0), 2.2, Particle.SWEEP_ATTACK, 8);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
            }
            case LAMINA_CRUZADA -> {
                for (LivingEntity e : cone(p, 5, 100)) {
                    ferir(p, e, 1.3 * d);
                    sangrar(p, e, 0.15 * d, 4);
                }
                Vector f = horizontal(p), lado = new Vector(-f.getZ(), 0, f.getX());
                Location c = p.getEyeLocation().add(f.clone().multiply(2));
                Particle.DustOptions corte = new Particle.DustOptions(Color.fromRGB(0xE53935), 1f);
                for (double s = -1.5; s <= 1.5; s += 0.15) {
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, -s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.1f);
            }
            case EXECUCAO -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                boolean fraco = max != null && a.getHealth() < max.getValue() * 0.3;
                ferir(p, a, (fraco ? 4 : 1.5) * d);
                w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1, 0), fraco ? 25 : 10, 0.3, 0.5, 0.3, 0.2);
                w.playSound(l, fraco ? Sound.BLOCK_ANVIL_LAND : Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, fraco ? 0.6f : 1f);
            }
            case TEMPESTADE_DE_LAMINAS -> repetir(8, 5, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 3.5)) ferir(p, e, 0.6 * d);
                anel(c.clone().add(0, 1, 0), 2.2, Particle.SWEEP_ATTACK, 6);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1f + i * 0.15f);
            });
            case PASSO_FANTASMA -> {
                LivingEntity a = alvo(p, 10);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Vector costas = a.getLocation().getDirection().setY(0);
                if (costas.lengthSquared() < 1e-4) costas = horizontal(p);
                Location atras = a.getLocation().subtract(costas.normalize().multiply(1.3));
                if (!atras.getBlock().isPassable() || !atras.clone().add(0, 1, 0).getBlock().isPassable()) atras = a.getLocation();
                atras.setDirection(a.getLocation().toVector().subtract(atras.toVector()));
                w.spawnParticle(Particle.SQUID_INK, l.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                p.teleport(atras);
                ferir(p, a, 2 * d);
                w.spawnParticle(Particle.REVERSE_PORTAL, atras.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                w.playSound(atras, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1.2f);
            }
            case JULGAMENTO_DA_LAMINA -> {
                p.setVelocity(new Vector(0, 0.9, 0));
                semQueda.put(p.getUniqueId(), agora() + 5000);
                depois(12, () -> {
                    if (!p.isOnline()) return;
                    Vector dir = horizontal(p);
                    Location de = p.getLocation().add(0, 0.5, 0);
                    for (LivingEntity e : linha(p, de, dir, 9, 1.6)) {
                        ferir(p, e, 3 * d);
                        e.setVelocity(e.getVelocity().setY(0.6));
                    }
                    for (double s = 0; s <= 9; s += 0.4) {
                        Location pt = de.clone().add(dir.clone().multiply(s));
                        w.spawnParticle(Particle.END_ROD, pt, 2, 0.1, 0.6, 0.1, 0.01);
                    }
                    w.strikeLightningEffect(de.clone().add(dir.clone().multiply(9)));
                    w.playSound(de, Sound.ITEM_TRIDENT_THUNDER, 1f, 1.3f);
                });
            }

            // ---------------- Machado ----------------
            case SALTO_ESMAGADOR -> saltar(p, horizontal(p).multiply(1.1).setY(0.75), 4, onde -> {
                for (LivingEntity e : perto(p, onde, 3.5)) {
                    ferir(p, e, 1.4 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 1);
                    empurrar(e, onde, 0.6, 0.35);
                }
                w.spawnParticle(Particle.BLOCK, onde, 80, 2, 0.2, 2, chao(onde));
                w.spawnParticle(Particle.EXPLOSION, onde, 2, 0.5, 0.2, 0.5, 0);
                w.playSound(onde, Sound.BLOCK_ANVIL_LAND, 1f, 0.6f);
            });
            case ARREMESSO_DO_MACHADO -> {
                Location inicio = p.getEyeLocation().subtract(0, 0.3, 0);
                Vector dir = p.getEyeLocation().getDirection().normalize();
                ItemDisplay ax = display(inicio, p.getInventory().getItemInMainHand(), 1f);
                Set<UUID> acertados = new HashSet<>();
                boolean[] voltando = {false};
                double[] dist = {0};
                Location pos = inicio.clone();
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.7f);
                repetir(1, 60, i -> {
                    if (!ax.isValid()) return;
                    if (!voltando[0]) {
                        Location prox = pos.clone().add(dir.clone().multiply(0.9));
                        dist[0] += 0.9;
                        if (!prox.getBlock().isPassable() || dist[0] >= 15) {
                            voltando[0] = true; // bateu ou chegou longe: volta para o dono
                            acertados.clear();
                        } else {
                            pos.setX(prox.getX());
                            pos.setY(prox.getY());
                            pos.setZ(prox.getZ());
                        }
                    } else {
                        Vector volta = p.getEyeLocation().toVector().subtract(pos.toVector());
                        if (volta.lengthSquared() < 2.25 || !p.isOnline() || !p.getWorld().equals(pos.getWorld())) {
                            tirar(ax);
                            return;
                        }
                        pos.add(volta.normalize().multiply(1.1));
                    }
                    ax.teleport(pos);
                    ax.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) (i * 0.9), 1, 0, 0),
                            new Vector3f(1f), new AxisAngle4f()));
                    for (LivingEntity e : perto(p, pos, 1.4)) {
                        if (!acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 1.0 * d);
                        w.spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.1);
                    }
                    if (i == 59) tirar(ax);
                });
            }
            case RACHAR_ESCUDO -> {
                LivingEntity a = alvo(p, 4);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.2 * d);
                marcar(a, 6, 0.25);
                if (a instanceof Player alvoP) alvoP.setCooldown(Material.SHIELD, 100);
                w.playSound(a.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 0.8f);
            }
            case FURIA_DO_LENHADOR -> {
                efeito(p, PotionEffectType.STRENGTH, 160, 0);
                efeito(p, PotionEffectType.HASTE, 160, 1);
                furia.put(p.getUniqueId(), agora() + 8000);
                w.spawnParticle(Particle.ANGRY_VILLAGER, l.clone().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.2f);
            }
            case REDEMOINHO_DE_ACO -> {
                efeito(p, PotionEffectType.SPEED, 60, 0);
                repetir(5, 12, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 3)) ferir(p, e, 0.5 * d);
                    anel(c.clone().add(0, 1, 0), 2, Particle.SWEEP_ATTACK, 5);
                    if (i % 2 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.3f);
                });
            }
            case DECAPITAR -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 2.2 * d);
                w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1.5, 0), 15, 0.3, 0.3, 0.3, 0.2);
                w.playSound(a.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
                depois(1, () -> {
                    if (a.isDead() || !a.isValid()) {
                        plugin.combos().darVigor(p, 20);
                        plugin.combos().zerarRecarga(p, Golpe.SALTO_ESMAGADOR);
                        p.sendActionBar(Component.text("✠ Decapitado! +20 vigor e Salto Esmagador pronto", NamedTextColor.GOLD));
                    }
                });
            }
            case CISAO_DA_TERRA -> {
                Vector dir = horizontal(p);
                Location de = p.getLocation();
                Set<UUID> acertados = new HashSet<>();
                w.playSound(de, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                repetir(1, 10, i -> {
                    Location pt = de.clone().add(dir.clone().multiply(i + 1));
                    w.spawnParticle(Particle.BLOCK, pt.clone().add(0, 0.2, 0), 30, 0.4, 0.3, 0.4, chao(pt));
                    w.spawnParticle(Particle.EXPLOSION, pt, 1, 0, 0, 0, 0);
                    for (LivingEntity e : perto(p, pt, 1.8)) {
                        if (!acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 2.5 * d);
                        e.setVelocity(e.getVelocity().setY(1.1));
                    }
                });
            }

            // ---------------- Lança ----------------
            case ESTOCADA_LONGA -> {
                Vector dir = p.getEyeLocation().getDirection();
                for (LivingEntity e : linha(p, p.getEyeLocation(), dir, 6, 1.1)) {
                    ferir(p, e, 1.4 * d);
                    empurrar(e, l, 0.5, 0.15);
                }
                for (double s = 0.5; s <= 6; s += 0.3) w.spawnParticle(Particle.CRIT, p.getEyeLocation().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1.3f);
            }
            case VARREDURA -> {
                for (LivingEntity e : cone(p, 4.5, 180)) {
                    ferir(p, e, 1.1 * d);
                    empurrar(e, l, 1.4, 0.4);
                }
                Vector f = horizontal(p);
                for (int i = -4; i <= 4; i++) {
                    Vector v = f.clone().rotateAroundY(Math.toRadians(i * 22.5)).multiply(2.5);
                    w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(v).add(0, 1, 0), 1, 0, 0, 0, 0);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
            }
            case ARREMESSO_DA_LANCA -> {
                Location pos = p.getEyeLocation().subtract(0, 0.2, 0);
                Vector dir = p.getEyeLocation().getDirection().normalize();
                ItemDisplay lanca = display(pos, p.getInventory().getItemInMainHand(), 1.2f);
                Set<UUID> acertados = new HashSet<>();
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
                repetir(1, 17, i -> {
                    if (!lanca.isValid()) return;
                    Location prox = pos.clone().add(dir.clone().multiply(1.5));
                    if (!prox.getBlock().isPassable() || i == 16) {
                        w.spawnParticle(Particle.CRIT, pos, 15, 0.2, 0.2, 0.2, 0.1);
                        tirar(lanca);
                        return;
                    }
                    pos.setX(prox.getX());
                    pos.setY(prox.getY());
                    pos.setZ(prox.getZ());
                    lanca.teleport(pos);
                    w.spawnParticle(Particle.END_ROD, pos, 1, 0, 0, 0, 0);
                    for (LivingEntity e : perto(p, pos, 1.3)) {
                        if (acertados.add(e.getUniqueId())) ferir(p, e, 1.6 * d);
                    }
                });
            }
            case SALTO_DO_DRAGAO -> {
                Location destino = mira(p, 15);
                p.setVelocity(new Vector(0, 1.4, 0));
                semQueda.put(p.getUniqueId(), agora() + 8000);
                w.playSound(l, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
                depois(12, () -> {
                    if (!p.isOnline()) return;
                    Vector mergulho = destino.toVector().subtract(p.getLocation().toVector());
                    if (mergulho.lengthSquared() > 1e-4) p.setVelocity(mergulho.normalize().multiply(2.2));
                    saltar(p, p.getVelocity(), 2, onde -> {
                        for (LivingEntity e : perto(p, onde, 3)) {
                            ferir(p, e, 2 * d);
                            empurrar(e, onde, 0.7, 0.4);
                        }
                        w.spawnParticle(Particle.REVERSE_PORTAL, onde, 80, 1.5, 0.3, 1.5, 0.05);
                        w.spawnParticle(Particle.EXPLOSION, onde, 1, 0, 0, 0, 0);
                        w.playSound(onde, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                    });
                });
            }
            case MURALHA_DE_LANCAS -> {
                Map<UUID, Long> ultimo = new HashMap<>();
                repetir(5, 20, i -> {
                    Location c = p.getLocation();
                    anel(c, 3, Particle.CRIT, 16);
                    for (LivingEntity e : perto(p, c, 3)) {
                        Long u = ultimo.get(e.getUniqueId());
                        if (u != null && agora() - u < 1000) continue;
                        ultimo.put(e.getUniqueId(), agora());
                        ferir(p, e, 0.8 * d);
                        empurrar(e, c, 1.0, 0.3);
                    }
                });
                w.playSound(l, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
            }
            case PERFURACAO -> repetir(5, 3, i -> {
                Vector dir = p.getEyeLocation().getDirection();
                for (LivingEntity e : linha(p, p.getEyeLocation(), dir, 6, 1.1)) {
                    ferir(p, e, 0.8 * d);
                    marcar(e, 4, 0.25);
                }
                for (double s = 0.5; s <= 6; s += 0.4) w.spawnParticle(Particle.CRIT, p.getEyeLocation().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.9f, 1.4f + i * 0.2f);
            });
            case LANCA_CELESTE -> {
                Location alvo = mira(p, 30);
                repetir(1, 20, i -> {
                    for (int k = 0; k < 4; k++) w.spawnParticle(Particle.END_ROD, alvo.clone().add(0, 14 - (i * 0.7) + k * 0.4, 0), 1, 0.05, 0, 0.05, 0);
                    anel(alvo, 4, Particle.END_ROD, 12);
                });
                depois(20, () -> {
                    w.strikeLightningEffect(alvo);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 1);
                    for (LivingEntity e : perto(p, alvo, 4)) {
                        ferir(p, e, 3 * d);
                        e.setVelocity(e.getVelocity().setY(0.8));
                    }
                });
                w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.5f);
            }

            // ---------------- Tridente ----------------
            case ONDA -> {
                for (LivingEntity e : cone(p, 6, 70)) {
                    ferir(p, e, 1.1 * d);
                    empurrar(e, l, 1.3, 0.3);
                }
                Vector f = horizontal(p);
                for (double s = 1; s <= 6; s += 0.5) {
                    Location pt = l.clone().add(f.clone().multiply(s)).add(0, 0.5, 0);
                    w.spawnParticle(Particle.SPLASH, pt, 15, s * 0.15, 0.3, s * 0.15, 0.1);
                    w.spawnParticle(Particle.BUBBLE_POP, pt, 5, s * 0.15, 0.3, s * 0.15, 0.05);
                }
                w.playSound(l, Sound.ENTITY_GENERIC_SPLASH, 1f, 0.8f);
            }
            case PUXAO -> {
                LivingEntity a = alvo(p, 15);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Vector v = p.getLocation().toVector().subtract(a.getLocation().toVector());
                double dist = v.length();
                a.setVelocity(v.normalize().multiply(Math.min(2.5, 0.35 * dist)).setY(0.4));
                ferir(p, a, 0.8 * d);
                efeito(a, PotionEffectType.SLOWNESS, 40, 0);
                Vector passo = v.clone().normalize().multiply(-0.5);
                Location pt = p.getEyeLocation();
                for (double s = 0; s < dist; s += 0.5) w.spawnParticle(Particle.FISHING, pt.add(passo), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
            }
            case REDEMOINHO -> {
                Location c = mira(p, 20);
                boolean forte = w.hasStorm() || c.getBlock().isLiquid() || p.isInWater();
                double mult = forte ? 1.6 : 1;
                repetir(5, 20, i -> {
                    for (int k = 0; k < 10; k++) {
                        double a = i * 0.8 + k * 0.63, r = 0.5 + (k % 5) * 0.9;
                        w.spawnParticle(Particle.SPLASH, c.getX() + Math.cos(a) * r, c.getY() + 0.3 + k * 0.15, c.getZ() + Math.sin(a) * r, 2, 0, 0, 0, 0);
                    }
                    w.spawnParticle(Particle.CLOUD, c.clone().add(0, 0.5, 0), 4, 1.5, 0.5, 1.5, 0.02);
                    for (LivingEntity e : perto(p, c, 5)) {
                        Vector v = c.toVector().subtract(e.getLocation().toVector()).setY(0);
                        if (v.lengthSquared() > 0.5) e.setVelocity(e.getVelocity().add(v.normalize().multiply(0.25)));
                        if (i % 4 == 0) ferir(p, e, 0.35 * d * mult);
                    }
                    if (i % 4 == 0) w.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1f, 1f);
                });
            }
            case TRIDENTE_TROVAO -> {
                LivingEntity a = alvo(p, 20);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                w.strikeLightningEffect(a.getLocation());
                ferir(p, a, 2 * d);
                Set<UUID> acertados = new HashSet<>();
                acertados.add(a.getUniqueId());
                LivingEntity atual = a;
                for (int salto = 0; salto < 2; salto++) {
                    LivingEntity prox = null;
                    double melhor = 36;
                    for (LivingEntity e : perto(p, atual.getLocation(), 6)) {
                        if (acertados.contains(e.getUniqueId())) continue;
                        double x = e.getLocation().distanceSquared(atual.getLocation());
                        if (x < melhor) { melhor = x; prox = e; }
                    }
                    if (prox == null) break;
                    Vector passo = prox.getLocation().toVector().subtract(atual.getLocation().toVector());
                    double dist = passo.length();
                    passo.normalize().multiply(0.4);
                    Location pt = atual.getLocation().add(0, 1, 0);
                    for (double s = 0; s < dist; s += 0.4) w.spawnParticle(Particle.ELECTRIC_SPARK, pt.add(passo), 2, 0.05, 0.05, 0.05, 0);
                    w.strikeLightningEffect(prox.getLocation());
                    ferir(p, prox, 0.7 * d);
                    acertados.add(prox.getUniqueId());
                    atual = prox;
                }
                w.playSound(l, Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
            }
            case MARE_ALTA -> {
                List<Player> aliados = new ArrayList<>();
                aliados.add(p);
                Party pt = plugin.parties().party(p);
                if (pt != null) for (Player o : pt.online()) {
                    if (o != p && o.getWorld().equals(w) && o.getLocation().distanceSquared(l) < 12 * 12) aliados.add(o);
                }
                for (Player a : aliados) {
                    efeito(a, PotionEffectType.DOLPHINS_GRACE, 160, 0);
                    efeito(a, PotionEffectType.CONDUIT_POWER, 160, 0);
                    efeito(a, PotionEffectType.REGENERATION, 160, 0);
                    w.spawnParticle(Particle.NAUTILUS, a.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.5);
                }
                for (LivingEntity e : perto(p, l, 8)) efeito(e, PotionEffectType.SLOWNESS, 80, 0);
                anel(l, 4, Particle.SPLASH, 24);
                w.playSound(l, Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1.2f);
            }
            case MERGULHO -> {
                Vector dir = p.getEyeLocation().getDirection().normalize();
                p.setVelocity(dir.multiply(p.isInWater() ? 3.0 : 1.6));
                semQueda.put(p.getUniqueId(), agora() + 4000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 12, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(p.isInWater() ? Particle.BUBBLE_POP : Particle.SPLASH, c.clone().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
                    for (LivingEntity e : perto(p, c, 2)) {
                        if (acertados.add(e.getUniqueId())) ferir(p, e, 1.5 * d);
                    }
                });
                w.playSound(l, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
            }
            case FURIA_DE_POSEIDON -> repetir(10, 6, i -> {
                Location c = p.getLocation();
                Location raio = c.clone().add(rnd().nextDouble(-6, 6), 0, rnd().nextDouble(-6, 6));
                raio.setY(w.getHighestBlockYAt(raio) + 1);
                if (Math.abs(raio.getY() - c.getY()) > 8) raio.setY(c.getY());
                w.strikeLightningEffect(raio);
                for (LivingEntity e : perto(p, raio, 2.5)) ferir(p, e, 1.2 * d);
                if (i % 2 == 0) {
                    anel(c, 5, Particle.SPLASH, 30);
                    for (LivingEntity e : perto(p, c, 6)) empurrar(e, c, 0.6, 0.2);
                }
            });

            // ---------------- Maça ----------------
            case ABALO_SISMICO -> {
                for (LivingEntity e : perto(p, l, 4)) {
                    ferir(p, e, 1.2 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 1);
                }
                w.spawnParticle(Particle.BLOCK, l, 100, 2.5, 0.2, 2.5, chao(l));
                w.playSound(l, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
            }
            case MARTELO_ASCENDENTE -> {
                LivingEntity a = alvo(p, 4);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.3 * d);
                a.setVelocity(a.getVelocity().setY(1.3));
                w.spawnParticle(Particle.CLOUD, a.getLocation(), 20, 0.4, 0.1, 0.4, 0.05);
                w.playSound(l, Sound.ITEM_MACE_SMASH_AIR, 1f, 1f);
            }
            case ATORDOAR -> {
                for (LivingEntity e : cone(p, 4, 90)) {
                    ferir(p, e, 1.0 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 40, 4);
                    efeito(e, PotionEffectType.WEAKNESS, 40, 1);
                    efeito(e, PotionEffectType.MINING_FATIGUE, 40, 1);
                    if (e instanceof Player) efeito(e, PotionEffectType.NAUSEA, 60, 0);
                    w.spawnParticle(Particle.ENCHANTED_HIT, e.getEyeLocation().add(0, 0.4, 0), 12, 0.3, 0.1, 0.3, 0.05);
                }
                w.playSound(l, Sound.BLOCK_BELL_USE, 1f, 0.6f);
            }
            case QUEBRA_ARMADURA -> {
                LivingEntity a = alvo(p, 4);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.5 * d);
                marcar(a, 8, 0.25);
                w.playSound(a.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 0.6f);
            }
            case SALTO_METEORO -> {
                double[] pico = {l.getY()};
                p.setVelocity(horizontal(p).multiply(0.4).setY(1.5));
                semQueda.put(p.getUniqueId(), agora() + 8000);
                w.playSound(l, Sound.ITEM_MACE_SMASH_AIR, 1f, 0.6f);
                repetir(1, 12, i -> pico[0] = Math.max(pico[0], p.getLocation().getY()));
                depois(12, () -> {
                    if (!p.isOnline()) return;
                    p.setVelocity(p.getVelocity().setY(-1.6));
                    saltar(p, p.getVelocity(), 1, onde -> {
                        double altura = Math.max(0, Math.min(20, pico[0] - onde.getY()));
                        for (LivingEntity e : perto(p, onde, 4)) {
                            ferir(p, e, (1.5 + altura * 0.2) * d);
                            empurrar(e, onde, 0.8, 0.5);
                        }
                        w.spawnParticle(Particle.EXPLOSION, onde, 3, 1, 0.2, 1, 0);
                        w.spawnParticle(Particle.FLAME, onde, 60, 2, 0.3, 2, 0.05);
                        w.spawnParticle(Particle.BLOCK, onde, 80, 2, 0.2, 2, chao(onde));
                        w.playSound(onde, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.2f, 0.6f);
                    });
                });
            }
            case ECO_DO_IMPACTO -> {
                Location c = l.clone();
                w.spawnParticle(Particle.SONIC_BOOM, c.clone().add(0, 0.5, 0), 1);
                w.playSound(c, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1f, 1f);
                double[] raios = {3, 5, 7};
                for (int k = 0; k < 3; k++) {
                    double r = raios[k];
                    depois(1 + k * 15L, () -> {
                        anel(c, r, Particle.CLOUD, (int) (r * 8));
                        for (LivingEntity e : perto(p, c, r)) {
                            if (e.getLocation().distance(c) < r - 2.2) continue;
                            ferir(p, e, 0.8 * d);
                            empurrar(e, c, 0.7, 0.3);
                        }
                        w.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.6f);
                    });
                }
            }
            case MARTELO_DOS_DEUSES -> {
                Location alvo = mira(p, 25);
                Location topo = alvo.clone().add(0, 12, 0);
                ItemDisplay martelo = display(topo, new ItemStack(Material.MACE), 5f);
                w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f);
                repetir(1, 20, i -> {
                    if (!martelo.isValid()) return;
                    martelo.teleport(topo.clone().subtract(0, i * 0.6, 0));
                    anel(alvo, 5, Particle.CRIT, 16);
                });
                depois(21, () -> {
                    tirar(martelo);
                    w.strikeLightningEffect(alvo);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 1);
                    w.spawnParticle(Particle.BLOCK, alvo, 150, 3, 0.3, 3, chao(alvo));
                    for (LivingEntity e : perto(p, alvo, 5)) {
                        ferir(p, e, 3 * d);
                        e.setVelocity(e.getVelocity().setY(1.0));
                    }
                    w.playSound(alvo, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 2f, 0.5f);
                });
            }

            // ---------------- Arco e besta ----------------
            case TIRO_TRIPLO -> {
                Vector dir = p.getEyeLocation().getDirection();
                for (int k = -1; k <= 1; k++) flecha(p, p.getEyeLocation(), dir.clone().rotateAroundY(Math.toRadians(k * 10)), 3f, b);
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
            }
            case FLECHA_PERFURANTE -> {
                Arrow a = flecha(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 4.5f, 1.5 * b);
                a.setPierceLevel(5);
                a.setGlowing(true);
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 1.6f);
            }
            case SALTO_TATICO -> {
                Vector dir = horizontal(p);
                p.setVelocity(dir.clone().multiply(-1.2).setY(0.5));
                semQueda.put(p.getUniqueId(), agora() + 4000);
                flecha(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 3f, 1.2 * b);
                w.spawnParticle(Particle.CLOUD, l, 10, 0.3, 0.1, 0.3, 0.05);
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
            }
            case FLECHA_EXPLOSIVA -> {
                Arrow a = flecha(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 3f, b);
                a.getPersistentDataContainer().set(K_FLECHA, PersistentDataType.STRING, "explosiva");
                a.getPersistentDataContainer().set(K_FLECHA_DANO, PersistentDataType.DOUBLE, 1.6 * b);
                a.setFireTicks(200);
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 0.7f);
            }
            case SARAIVADA -> {
                Location alvo = mira(p, 30);
                repetir(1, 25, i -> {
                    Location de = alvo.clone().add(rnd().nextDouble(-4, 4), 12, rnd().nextDouble(-4, 4));
                    flecha(p, de, new Vector(0, -1, 0), 1.5f, 0.5 * b);
                });
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 0.6f);
            }
            case FLECHA_SOMBRIA -> {
                Arrow a = flecha(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 2.5f, 1.8 * b);
                a.setGlowing(true);
                a.setGravity(false);
                a.getPersistentDataContainer().set(K_FLECHA, PersistentDataType.STRING, "sombria");
                repetir(1, 60, i -> {
                    if (!a.isValid() || a.isInBlock() || a.isOnGround()) return;
                    w.spawnParticle(Particle.SQUID_INK, a.getLocation(), 1, 0, 0, 0, 0);
                    LivingEntity melhor = null;
                    double dist = 14 * 14;
                    for (LivingEntity e : perto(p, a.getLocation(), 14)) {
                        double x = e.getLocation().distanceSquared(a.getLocation());
                        if (x < dist) { dist = x; melhor = e; }
                    }
                    if (melhor != null) {
                        Vector para = melhor.getLocation().add(0, melhor.getHeight() / 2, 0).toVector().subtract(a.getLocation().toVector());
                        a.setVelocity(para.normalize().multiply(a.getVelocity().length()));
                    }
                    if (i == 59) a.setGravity(true);
                });
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);
            }
            case TIRO_CELESTIAL -> {
                efeito(p, PotionEffectType.SLOWNESS, 20, 3);
                repetir(2, 10, i -> w.spawnParticle(Particle.END_ROD, p.getEyeLocation(), 6, 0.5, 0.5, 0.5, 0.05));
                w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.8f);
                depois(20, () -> {
                    if (!p.isOnline()) return;
                    Location de = p.getEyeLocation();
                    Vector dir = de.getDirection().normalize();
                    RayTraceResult parede = w.rayTraceBlocks(de, dir, 40);
                    double alcance = parede == null ? 40 : de.toVector().distance(parede.getHitPosition());
                    for (LivingEntity e : linha(p, de, dir, alcance, 1.2)) ferir(p, e, 3 * b);
                    for (double s = 0; s <= alcance; s += 0.5) {
                        Location pt = de.clone().add(dir.clone().multiply(s));
                        w.spawnParticle(Particle.END_ROD, pt, 1, 0.02, 0.02, 0.02, 0);
                        if (((int) (s * 2)) % 4 == 0) w.spawnParticle(Particle.FIREWORK, pt, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                    w.playSound(de, Sound.ENTITY_GUARDIAN_ATTACK, 1f, 1.8f);
                });
            }

            // ---------------- Adaga (2.51) ----------------
            case PUNHALADA -> {
                LivingEntity a = alvo(p, 3.8);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                boolean costas = pelasCostas(p, a);
                ferir(p, a, (costas ? 2.6 : 1.3) * d);
                w.spawnParticle(costas ? Particle.DAMAGE_INDICATOR : Particle.CRIT, a.getLocation().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.1);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, costas ? 0.8f : 1.4f);
            }
            case CORTE_VENENOSO -> {
                LivingEntity a = alvo(p, 3.8);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.1 * d);
                efeito(a, PotionEffectType.POISON, 100, 1);
                sangrar(p, a, 0.12 * d, 4);
                w.spawnParticle(Particle.ITEM_SLIME, a.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.05);
                w.playSound(l, Sound.ENTITY_SPIDER_HURT, 0.8f, 1.4f);
            }
            case ROLAMENTO -> {
                p.setVelocity(horizontal(p).multiply(-1.3).setY(0.35));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                List<LivingEntity> l2 = perto(p, l, 10);
                l2.sort((x, y) -> Double.compare(x.getLocation().distanceSquared(l), y.getLocation().distanceSquared(l)));
                if (!l2.isEmpty()) {
                    LivingEntity a = l2.getFirst();
                    Vector dir = a.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
                    lamina(p, p.getEyeLocation(), dir, 1.2, 12, false, visualLamina(p), (e, onde) -> ferir(p, e, 1.0 * d), null);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
            }
            case NUVEM_DE_FUMACA -> {
                for (LivingEntity e : perto(p, l, 5)) {
                    efeito(e, PotionEffectType.BLINDNESS, 60, 0);
                    if (e instanceof Mob m && p.equals(m.getTarget())) m.setTarget(null);
                }
                efeito(p, PotionEffectType.INVISIBILITY, 60, 0);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l.clone().add(0, 1, 0), 40, 1.5, 0.8, 1.5, 0.02);
                w.playSound(l, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.8f);
            }
            case DANCA_DAS_LAMINAS -> repetir(3, 6, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 3)) ferir(p, e, 0.5 * d);
                anel(c.clone().add(0, 1, 0), 1.6, Particle.SWEEP_ATTACK, 4);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f + i * 0.08f);
            });
            case SOMBRA_ASSASSINA -> {
                LivingEntity a = alvo(p, 12);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Vector costas = a.getLocation().getDirection().setY(0);
                if (costas.lengthSquared() < 1e-4) costas = horizontal(p);
                Location atras = a.getLocation().subtract(costas.normalize().multiply(1.2));
                if (!atras.getBlock().isPassable() || !atras.clone().add(0, 1, 0).getBlock().isPassable()) atras = a.getLocation();
                atras.setDirection(a.getLocation().toVector().subtract(atras.toVector()));
                w.spawnParticle(Particle.SMOKE, l.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.02);
                p.teleport(atras);
                ferir(p, a, 2.5 * d);
                w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1, 0), 12, 0.2, 0.4, 0.2, 0.1);
                w.playSound(atras, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);
            }
            case MARCA_DA_MORTE -> {
                LivingEntity a = alvo(p, 8);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Particle.DustOptions roxo = new Particle.DustOptions(Color.fromRGB(0x4A148C), 1.4f);
                repetir(10, 6, i -> {
                    if (a.isValid()) w.spawnParticle(Particle.DUST, a.getLocation().add(0, a.getHeight() + 0.4, 0), 6, 0.2, 0.1, 0.2, 0, roxo);
                });
                w.playSound(a.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.6f);
                depois(60, () -> {
                    if (!a.isValid() || a.isDead()) return;
                    AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                    double falta = max == null ? 0.5 : 1 - a.getHealth() / max.getValue();
                    ferir(p, a, (3 + 3 * falta) * d);
                    w.spawnParticle(Particle.SOUL, a.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.05);
                    w.playSound(a.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
                });
            }

            // ---------------- Lâminas Duplas ----------------
            case CORTE_DUPLO -> {
                LivingEntity a = alvo(p, 3.8);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.0 * d);
                depois(4, () -> {
                    if (!a.isValid() || !p.isOnline()) return;
                    p.swingOffHand();
                    ferir(p, a, 1.0 * d);
                    w.playSound(a.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.6f);
                });
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
            }
            case REDEMOINHO_GEMEO -> repetir(5, 2, i -> {
                Location c = p.getLocation();
                if (i == 1) p.swingOffHand();
                for (LivingEntity e : perto(p, c, 3.2)) {
                    ferir(p, e, 0.9 * d);
                    empurrar(e, c, 0.4, 0.2);
                }
                anel(c.clone().add(0, 1, 0), 2, Particle.SWEEP_ATTACK, 8);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.1f + i * 0.2f);
            });
            case X_CRUZADO -> {
                for (LivingEntity e : cone(p, 5, 110)) {
                    ferir(p, e, 1.2 * d);
                    sangrar(p, e, 0.2 * d, 5);
                }
                p.swingOffHand();
                Vector f = horizontal(p), lado = new Vector(-f.getZ(), 0, f.getX());
                Location c = p.getEyeLocation().add(f.clone().multiply(2));
                Particle.DustOptions corte = new Particle.DustOptions(Color.fromRGB(0xEF5350), 1f);
                for (double s = -1.5; s <= 1.5; s += 0.15) {
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, -s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
            }
            case INVESTIDA_GEMEA -> {
                p.setVelocity(horizontal(p).multiply(2.0).setY(0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 10, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                    for (LivingEntity e : perto(p, c, 2.0)) if (acertados.add(e.getUniqueId())) ferir(p, e, 1.2 * d);
                });
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
            }
            case CHUVA_DE_ACO -> repetir(5, 8, i -> {
                Location c = p.getLocation();
                if (i % 2 == 1) p.swingOffHand();
                for (LivingEntity e : perto(p, c, 3.5)) ferir(p, e, 0.45 * d);
                w.spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 12, 1.5, 0.5, 1.5, 0.1);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.2f + i * 0.08f);
            });
            case DANCA_DO_VENTO -> saltar(p, horizontal(p).multiply(0.6).setY(0.9), 6, onde -> {
                for (LivingEntity e : perto(p, onde, 4)) {
                    ferir(p, e, 1.6 * d);
                    e.setVelocity(e.getVelocity().setY(0.8));
                }
                w.spawnParticle(Particle.GUST, onde.clone().add(0, 0.5, 0), 4, 1, 0.2, 1, 0);
                anel(onde.clone().add(0, 0.5, 0), 3, Particle.SWEEP_ATTACK, 10);
                w.playSound(onde, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1f);
            });
            case CICLONE_CARMESIM -> repetir(5, 12, i -> {
                Location c = p.getLocation();
                if (i % 2 == 1) p.swingOffHand();
                double total = 0;
                for (LivingEntity e : perto(p, c, 3.5)) {
                    ferir(p, e, 0.6 * d);
                    total += 0.6 * d;
                }
                curar(p, total * 0.15);
                Particle.DustOptions verm = new Particle.DustOptions(Color.fromRGB(0xB71C1C), 1.3f);
                for (int k = 0; k < 10; k++) {
                    double ang = i * 0.8 + k * Math.PI / 5;
                    w.spawnParticle(Particle.DUST, c.getX() + Math.cos(ang) * 2.4, c.getY() + 1, c.getZ() + Math.sin(ang) * 2.4, 1, 0, 0, 0, 0, verm);
                }
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 0.9f + i * 0.05f);
            });

            // ---------------- Arremesso ----------------
            case LEQUE_DE_FACAS -> {
                Vector base = p.getEyeLocation().getDirection();
                for (int k = -1; k <= 1; k++) {
                    Vector dir = base.clone().rotateAroundY(Math.toRadians(15 * k));
                    lamina(p, p.getEyeLocation(), dir, 1.3, 18, false, visualLamina(p), (e, onde) -> ferir(p, e, 0.9 * d), null);
                }
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);
            }
            case RICOCHETE -> {
                LivingEntity a = alvo(p, 16);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Set<UUID> ja = new HashSet<>();
                LivingEntity[] atual = {a};
                Location[] de = {p.getEyeLocation()};
                repetir(5, 4, i -> {
                    LivingEntity e = atual[0];
                    if (e == null || !e.isValid()) return;
                    ja.add(e.getUniqueId());
                    Location para = e.getLocation().add(0, e.getHeight() / 2, 0);
                    Vector v = para.toVector().subtract(de[0].toVector());
                    for (double s = 0; s < v.length(); s += 0.5) w.spawnParticle(Particle.CRIT, de[0].clone().add(v.clone().normalize().multiply(s)), 1, 0, 0, 0, 0);
                    ferir(p, e, 1.1 * d);
                    w.playSound(para, Sound.ITEM_TRIDENT_HIT, 0.8f, 1.4f + i * 0.1f);
                    de[0] = para;
                    LivingEntity prox = null;
                    double melhor = 64;
                    for (LivingEntity o : perto(p, para, 8)) {
                        double dd = o.getLocation().distanceSquared(para);
                        if (!ja.contains(o.getUniqueId()) && dd < melhor) {
                            melhor = dd;
                            prox = o;
                        }
                    }
                    atual[0] = prox;
                });
            }
            case LAMINA_QUE_PRENDE -> {
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.5, 20, false, visualLamina(p), (e, onde) -> {
                    ferir(p, e, 1.0 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 4);
                    e.setVelocity(new Vector(0, -0.2, 0));
                    w.spawnParticle(Particle.BLOCK, e.getLocation(), 20, 0.3, 0.1, 0.3, Material.COBWEB.createBlockData());
                }, null);
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
            }
            case MACHADINHA_GIRATORIA -> {
                Location inicio = p.getEyeLocation().subtract(0, 0.3, 0);
                Vector dir = p.getEyeLocation().getDirection().normalize();
                lamina(p, inicio, dir, 1.0, 14, true, visualLamina(p), (e, onde) -> ferir(p, e, 1.3 * d), onde ->
                        lamina(p, onde, p.getEyeLocation().toVector().subtract(onde.toVector()), 1.0,
                                Math.max(2, onde.distance(p.getEyeLocation())), true, visualLamina(p), (e, o2) -> ferir(p, e, 0.9 * d), null));
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.7f);
            }
            case CHUVA_DE_LAMINAS -> {
                Location alvoChao = mira(p, 25);
                repetir(3, 12, i -> {
                    Location c = alvoChao.clone().add(rnd().nextDouble(-2.5, 2.5), 0, rnd().nextDouble(-2.5, 2.5));
                    for (double y = 6; y > 0; y -= 0.6) w.spawnParticle(Particle.CRIT, c.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                    for (LivingEntity e : perto(p, c, 1.6)) ferir(p, e, 0.7 * d);
                    w.playSound(c, Sound.ITEM_TRIDENT_HIT_GROUND, 0.6f, 1.4f);
                });
            }
            case LAMINA_EXPLOSIVA -> {
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.4, 25, false, visualLamina(p), (e, onde) -> {
                }, onde -> {
                    for (LivingEntity e : perto(p, onde, 3.5)) {
                        ferir(p, e, 1.8 * d);
                        empurrar(e, onde, 0.7, 0.4);
                    }
                    w.spawnParticle(Particle.EXPLOSION, onde, 3, 0.5, 0.5, 0.5, 0);
                    w.playSound(onde, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
                });
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1f);
            }
            case TEMPESTADE_DE_ACO -> repetir(2, 20, i -> {
                double ang = i * (Math.PI * 2 / 10) + (i >= 10 ? Math.PI / 10 : 0);
                Vector dir = new Vector(Math.cos(ang), 0.05, Math.sin(ang));
                lamina(p, p.getEyeLocation().subtract(0, 0.4, 0), dir, 1.2, 12, true, visualLamina(p), (e, onde) -> ferir(p, e, 0.8 * d), null);
                if (i % 4 == 0) w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.7f, 1.3f + i * 0.02f);
            });

            // ---------------- Foice ----------------
            case CEIFAR -> {
                for (LivingEntity e : cone(p, 4.5, 200)) {
                    ferir(p, e, 1.2 * d);
                    empurrar(e, l, 0.3, 0.1);
                }
                Vector f = horizontal(p);
                for (int k = -10; k <= 10; k++) {
                    Vector v = f.clone().rotateAroundY(Math.toRadians(k * 10)).multiply(3);
                    w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(v).add(0, 1, 0), 1, 0, 0, 0, 0);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
            }
            case GANCHO_DA_FOICE -> {
                LivingEntity a = alvo(p, 7);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                ferir(p, a, 0.8 * d);
                puxar(p, a, 1.2);
                w.playSound(l, Sound.ITEM_CROSSBOW_HIT, 1f, 0.8f);
            }
            case COLHEITA -> {
                int n = 0;
                for (LivingEntity e : perto(p, l, 4)) {
                    ferir(p, e, 1.2 * d);
                    n++;
                }
                curar(p, Math.min(8, n * 1.5));
                anel(l.clone().add(0, 1, 0), 3, Particle.HAPPY_VILLAGER, 16);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f);
            }
            case FOICE_GIRATORIA -> {
                ItemDisplay foice = display(l.clone().add(0, 1, 0), visualLamina(p), 1.2f);
                repetir(2, 20, i -> {
                    if (!foice.isValid() || !p.isOnline()) return;
                    double ang = i * 0.7;
                    Location c = p.getLocation().add(Math.cos(ang) * 2.8, 1, Math.sin(ang) * 2.8);
                    foice.teleport(c);
                    foice.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) ang, 0, 1, 0), new Vector3f(1.2f), new AxisAngle4f()));
                    for (LivingEntity e : perto(p, c, 1.5)) ferir(p, e, 0.5 * d);
                    if (i % 4 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 0.8f);
                    if (i == 19) tirar(foice);
                });
            }
            case CEIFA_DAS_ALMAS -> {
                for (LivingEntity e : perto(p, l, 5)) {
                    ferir(p, e, 1.5 * d);
                    efeito(e, PotionEffectType.WITHER, 80, 1);
                }
                anel(l.clone().add(0, 0.5, 0), 4, Particle.SOUL, 24);
                w.playSound(l, Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.6f);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
            }
            case PASSO_DO_CEIFADOR -> {
                Vector dir = horizontal(p);
                Location de = l.clone().add(0, 0.5, 0);
                for (LivingEntity e : linha(p, de, dir, 8, 1.5)) {
                    ferir(p, e, 1.6 * d);
                    efeito(e, PotionEffectType.WITHER, 60, 1);
                }
                Location fim = l.clone();
                for (double s = 0.5; s <= 8; s += 0.5) {
                    Location pt = l.clone().add(dir.clone().multiply(s));
                    if (!pt.getBlock().isPassable() || !pt.clone().add(0, 1, 0).getBlock().isPassable()) break;
                    fim = pt;
                    w.spawnParticle(Particle.SOUL, pt.clone().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
                }
                p.teleport(fim.setDirection(dir));
                w.playSound(fim, Sound.ENTITY_PHANTOM_FLAP, 1f, 0.6f);
            }
            case COLHEITA_FINAL -> {
                w.playSound(l, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);
                depois(10, () -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 7)) {
                        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
                        boolean fraco = max != null && e.getHealth() < max.getValue() * 0.2 && !Chefes.ehChefe(e) && !(e instanceof Player);
                        ferir(p, e, fraco ? 1000 : 3 * d);
                    }
                    for (int k = 0; k < 36; k++) {
                        double ang = Math.PI * 2 * k / 36;
                        for (double r = 2; r <= 7; r += 1.25) w.spawnParticle(Particle.SWEEP_ATTACK, c.getX() + Math.cos(ang) * r, c.getY() + 1, c.getZ() + Math.sin(ang) * r, 1, 0, 0, 0, 0);
                    }
                    w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.4f);
                });
            }

            // ---------------- Bastão ----------------
            case VARREDURA_BAIXA -> {
                for (LivingEntity e : perto(p, l, 3.5)) {
                    ferir(p, e, 1.0 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 2);
                }
                anel(l.clone().add(0, 0.2, 0), 2.5, Particle.SWEEP_ATTACK, 10);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.9f);
            }
            case ESTOCADA_DO_BASTAO -> {
                LivingEntity a = alvo(p, 5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.1 * d);
                empurrar(a, l, 2.2, 0.4);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
            }
            case SALTO_COM_VARA -> {
                p.setVelocity(horizontal(p).multiply(1.4).setY(1.0));
                semQueda.put(p.getUniqueId(), agora() + 6000);
                w.spawnParticle(Particle.CLOUD, l, 15, 0.3, 0.1, 0.3, 0.05);
                w.playSound(l, Sound.ENTITY_GOAT_LONG_JUMP, 1f, 1f);
            }
            case MOINHO -> repetir(4, 10, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 3.5)) {
                    ferir(p, e, 0.4 * d);
                    empurrar(e, c, 0.9, 0.25);
                }
                anel(c.clone().add(0, 1, 0), 2.5, Particle.SWEEP_ATTACK, 6);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.6f, 1f + i * 0.05f);
            });
            case TERREMOTO_DO_BASTAO -> {
                for (LivingEntity e : perto(p, l, 5)) {
                    ferir(p, e, 1.3 * d);
                    e.setVelocity(new Vector(0, 1.0, 0));
                }
                w.spawnParticle(Particle.BLOCK, l, 80, 2.5, 0.2, 2.5, chao(l));
                w.playSound(l, Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
            }
            case DEFESA_DO_MONGE -> {
                efeito(p, PotionEffectType.RESISTANCE, 100, 1);
                repetir(5, 20, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 2.2)) empurrar(e, c, 0.7, 0.2);
                    anel(c.clone().add(0, 0.1, 0), 1.2, Particle.END_ROD, 8);
                });
                w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
            }
            case PILAR_CELESTE -> {
                Vector dir = horizontal(p);
                repetir(2, 10, i -> {
                    Location pt = l.clone().add(dir.clone().multiply(i + 1));
                    for (LivingEntity e : perto(p, pt, 1.8)) {
                        ferir(p, e, 2.2 * d);
                        e.setVelocity(new Vector(0, 1.3, 0));
                    }
                    for (double y = 0; y < 4; y += 0.5) w.spawnParticle(Particle.END_ROD, pt.clone().add(0, y, 0), 1, 0.1, 0, 0.1, 0);
                    w.playSound(pt, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 0.6f + i * 0.08f);
                });
            }

            // ---------------- Chicote ----------------
            case CHICOTADA -> {
                LivingEntity a = alvo(p, 8);
                if (a == null) return falhar(p, "Nenhum inimigo ao alcance do chicote");
                ferir(p, a, 1.2 * d);
                Vector v = a.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
                for (double s = 0; s < v.length(); s += 0.4) w.spawnParticle(Particle.CRIT, p.getEyeLocation().add(v.clone().normalize().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(a.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1.8f);
            }
            case LACO -> {
                LivingEntity a = alvo(p, 10);
                if (a == null) return falhar(p, "Nenhum inimigo ao alcance do chicote");
                ferir(p, a, 0.6 * d);
                puxar(p, a, 1.4);
                w.playSound(l, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1f);
            }
            case DESARMAR -> {
                LivingEntity a = alvo(p, 8);
                if (a == null) return falhar(p, "Nenhum inimigo ao alcance do chicote");
                ferir(p, a, 0.8 * d);
                efeito(a, PotionEffectType.WEAKNESS, 120, 1);
                if (a instanceof Mob m && m.getEquipment() != null && !Chefes.ehChefe(m)) {
                    ItemStack arma = m.getEquipment().getItemInMainHand();
                    if (!arma.getType().isAir()) {
                        m.getEquipment().setItemInMainHand(null);
                        w.dropItemNaturally(m.getLocation(), arma);
                    }
                }
                w.playSound(a.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1.2f);
            }
            case ESTALO -> {
                for (LivingEntity e : cone(p, 7, 70)) {
                    ferir(p, e, 0.9 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 20, 5);
                    efeito(e, PotionEffectType.NAUSEA, 40, 0);
                }
                w.spawnParticle(Particle.SONIC_BOOM, p.getEyeLocation().add(horizontal(p).multiply(2)), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.6f);
            }
            case BALANCO -> {
                RayTraceResult r = w.rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15);
                if (r == null || r.getHitBlock() == null) return falhar(p, "Mire num bloco até 15 blocos de distância");
                Location ponto = r.getHitPosition().toLocation(w);
                Vector v = ponto.toVector().subtract(p.getLocation().toVector());
                p.setVelocity(v.multiply(0.18).setY(Math.max(0.5, v.getY() * 0.2 + 0.5)));
                semQueda.put(p.getUniqueId(), agora() + 5000);
                Vector dl = ponto.toVector().subtract(p.getEyeLocation().toVector());
                for (double s = 0; s < dl.length(); s += 0.5) w.spawnParticle(Particle.CRIT, p.getEyeLocation().add(dl.clone().normalize().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.8f);
            }
            case CHICOTE_DE_FOGO -> {
                Vector dir = horizontal(p);
                Location de = l.clone().add(0, 1, 0);
                for (LivingEntity e : linha(p, de, dir, 9, 1.4)) {
                    ferir(p, e, 1.4 * d);
                    e.setFireTicks(100);
                }
                for (double s = 0; s <= 9; s += 0.35) w.spawnParticle(Particle.FLAME, de.clone().add(dir.clone().multiply(s)), 2, 0.05, 0.1, 0.05, 0.01);
                w.playSound(l, Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
            }
            case SERPENTE -> repetir(8, 3, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 7)) {
                    ferir(p, e, 1.2 * d);
                    puxar(p, e, 0.7);
                }
                for (int k = 0; k < 40; k++) {
                    double ang = Math.PI * 2 * k / 40 + i;
                    w.spawnParticle(Particle.CRIT, c.getX() + Math.cos(ang) * 6, c.getY() + 1, c.getZ() + Math.sin(ang) * 6, 1, 0, 0, 0, 0);
                }
                w.playSound(c, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1.4f + i * 0.2f);
            });

            // ---------------- Montante (minério) ----------------
            case GOLPE_PESADO -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 2.0 * d);
                empurrar(a, l, 1.2, 0.35);
                w.spawnParticle(Particle.CRIT, a.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.6f);
            }
            case VARREDURA_DO_TITA -> {
                for (LivingEntity e : cone(p, 4.5, 200)) {
                    ferir(p, e, 1.4 * d);
                    empurrar(e, l, 0.6, 0.2);
                }
                Vector f = horizontal(p);
                for (int k = -10; k <= 10; k++) {
                    Vector v = f.clone().rotateAroundY(Math.toRadians(k * 10)).multiply(3.5);
                    w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(v).add(0, 1, 0), 1, 0, 0, 0, 0);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
            }
            case INVESTIDA_DO_MONTANTE -> {
                p.setVelocity(horizontal(p).multiply(1.6).setY(0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 12, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                    for (LivingEntity e : perto(p, c, 2.2)) {
                        if (!acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 1.3 * d);
                        empurrar(e, c, 0.8, 0.3);
                    }
                });
                w.playSound(l, Sound.ENTITY_RAVAGER_STEP, 1f, 1f);
            }
            case LAMINA_ERGUIDA -> {
                for (LivingEntity e : perto(p, l, 3.5)) {
                    ferir(p, e, 1.2 * d);
                    e.setVelocity(new Vector(0, 1.0, 0));
                }
                anel(l.clone().add(0, 0.3, 0), 2.5, Particle.SWEEP_ATTACK, 10);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
            }
            case CORTE_DO_HORIZONTE -> {
                Vector dir = horizontal(p);
                Location de = l.clone().add(0, 1, 0);
                for (LivingEntity e : linha(p, de, dir, 10, 1.5)) ferir(p, e, 1.8 * d);
                for (double s = 0; s <= 10; s += 0.7) w.spawnParticle(Particle.SWEEP_ATTACK, de.clone().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.4f);
            }
            case FURIA_DO_GIGANTE -> {
                efeito(p, PotionEffectType.STRENGTH, 120, 1);
                efeito(p, PotionEffectType.RESISTANCE, 120, 0);
                for (LivingEntity e : perto(p, l, 3.5)) {
                    ferir(p, e, 1.2 * d);
                    empurrar(e, l, 1.0, 0.3);
                }
                anel(l.clone().add(0, 1, 0), 3, Particle.SWEEP_ATTACK, 12);
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1f);
            }
            case QUEDA_DA_MONTANHA -> saltar(p, horizontal(p).multiply(0.5).setY(1.1), 6, onde -> {
                for (LivingEntity e : perto(p, onde, 6)) {
                    ferir(p, e, 3.5 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 2);
                    empurrar(e, onde, 0.8, 0.4);
                }
                w.spawnParticle(Particle.BLOCK, onde, 120, 3, 0.2, 3, chao(onde));
                w.spawnParticle(Particle.EXPLOSION, onde, 4, 1.5, 0.3, 1.5, 0);
                w.playSound(onde, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
            });

            // ---------------- Bumerangue (minério) ----------------
            case ARCO_DE_AMBAR -> {
                vaiEVolta(p, p.getEyeLocation().getDirection(), 12, 1.0 * d, 0.8 * d, null);
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.6f);
            }
            case DUPLO_RETORNO -> {
                Vector base = p.getEyeLocation().getDirection();
                vaiEVolta(p, base.clone().rotateAroundY(Math.toRadians(20)), 11, 0.8 * d, 0.7 * d, null);
                vaiEVolta(p, base.clone().rotateAroundY(Math.toRadians(-20)), 11, 0.8 * d, 0.7 * d, null);
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.4f);
            }
            case PANCADA_ATORDOANTE -> {
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.2, 14, false, visualLamina(p), (e, onde) -> {
                    ferir(p, e, 1.2 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 40, 4);
                    efeito(e, PotionEffectType.BLINDNESS, 40, 0);
                    w.playSound(onde, Sound.BLOCK_BELL_USE, 0.8f, 1.6f);
                }, onde -> lamina(p, onde, p.getEyeLocation().toVector().subtract(onde.toVector()), 1.2,
                        Math.max(2, onde.distance(p.getEyeLocation())), true, visualLamina(p), (e, o2) -> { }, null));
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 1.2f);
            }
            case ORBITA -> {
                ItemDisplay bum = display(l.clone().add(0, 1, 0), visualLamina(p), 1.0f);
                repetir(1, 40, i -> {
                    if (!bum.isValid() || !p.isOnline()) return;
                    double ang = i * Math.PI * 2 / 20;
                    Location c = p.getLocation().add(Math.cos(ang) * 2.6, 1, Math.sin(ang) * 2.6);
                    bum.teleport(c);
                    bum.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) (i * 0.9), 0, 1, 0), new Vector3f(1f), new AxisAngle4f()));
                    for (LivingEntity e : perto(p, c, 1.4)) ferir(p, e, 0.5 * d);
                    if (i % 5 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.7f);
                    if (i == 39) tirar(bum);
                });
            }
            case SOL_DO_DESERTO -> {
                Location alvoChao = mira(p, 20);
                for (double y = 0; y < 5; y += 0.5) w.spawnParticle(Particle.FLAME, l.clone().add(0, 2 + y, 0), 1, 0.05, 0, 0.05, 0);
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.8f);
                depois(15, () -> {
                    for (LivingEntity e : perto(p, alvoChao, 4)) {
                        ferir(p, e, 1.6 * d);
                        e.setFireTicks(Math.max(e.getFireTicks(), 80));
                    }
                    w.spawnParticle(Particle.FLAME, alvoChao.clone().add(0, 0.5, 0), 60, 2, 0.3, 2, 0.05);
                    w.spawnParticle(Particle.LAVA, alvoChao, 10, 1.5, 0.2, 1.5, 0);
                    w.playSound(alvoChao, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                });
            }
            case RICOCHETE_DE_AMBAR -> {
                LivingEntity a = alvo(p, 16);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Set<UUID> ja = new HashSet<>();
                LivingEntity[] atual = {a};
                Location[] de = {p.getEyeLocation()};
                repetir(5, 5, i -> {
                    LivingEntity e = atual[0];
                    if (e == null || !e.isValid()) return;
                    ja.add(e.getUniqueId());
                    Location para = e.getLocation().add(0, e.getHeight() / 2, 0);
                    Vector v = para.toVector().subtract(de[0].toVector());
                    for (double s = 0; s < v.length(); s += 0.5) w.spawnParticle(Particle.WAX_ON, de[0].clone().add(v.clone().normalize().multiply(s)), 1, 0, 0, 0, 0);
                    ferir(p, e, 1.2 * d);
                    w.playSound(para, Sound.ITEM_TRIDENT_HIT, 0.8f, 1.5f + i * 0.1f);
                    de[0] = para;
                    LivingEntity prox = null;
                    double melhor = 64;
                    for (LivingEntity o : perto(p, para, 8)) {
                        double dd = o.getLocation().distanceSquared(para);
                        if (!ja.contains(o.getUniqueId()) && dd < melhor) {
                            melhor = dd;
                            prox = o;
                        }
                    }
                    atual[0] = prox;
                });
            }
            case TEMPESTADE_DE_AREIA -> {
                for (int k = 0; k < 8; k++) {
                    double ang = Math.PI * 2 * k / 8;
                    vaiEVolta(p, new Vector(Math.cos(ang), 0, Math.sin(ang)), 9, 0.9 * d, 0.6 * d,
                            e -> efeito(e, PotionEffectType.BLINDNESS, 40, 0));
                }
                w.spawnParticle(Particle.BLOCK, l.clone().add(0, 1, 0), 80, 3, 0.8, 3, Material.SAND.createBlockData());
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
            }

            // ---------------- Leques de Guerra (minério) ----------------
            case CORTE_DO_LEQUE -> {
                for (LivingEntity e : cone(p, 4.5, 100)) ferir(p, e, 1.1 * d);
                w.spawnParticle(Particle.CHERRY_LEAVES, p.getEyeLocation().add(horizontal(p).multiply(2)), 15, 1, 0.4, 1, 0);
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.7f);
            }
            case RAJADA_DE_VENTO -> {
                for (LivingEntity e : cone(p, 7, 60)) {
                    ferir(p, e, 1.0 * d);
                    empurrar(e, l, 1.6, 0.4);
                }
                w.spawnParticle(Particle.GUST, p.getEyeLocation().add(horizontal(p).multiply(2.5)), 3, 0.8, 0.3, 0.8, 0);
                w.playSound(l, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.1f);
            }
            case DANCA_DA_GARCA -> {
                p.setVelocity(horizontal(p).multiply(-1.0).setY(0.5));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.4, 14, true, visualLamina(p), (e, onde) -> ferir(p, e, 1.2 * d), null);
                w.playSound(l, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.4f);
            }
            case PETALAS_DE_JADE -> {
                repetir(5, 4, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 3)) ferir(p, e, 0.5 * d);
                    anel(c.clone().add(0, 1, 0), 2.2, Particle.CHERRY_LEAVES, 12);
                    w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f + i * 0.1f);
                });
                curar(p, 4);
            }
            case VENDAVAL -> {
                for (LivingEntity e : perto(p, l, 5)) {
                    ferir(p, e, 1.3 * d);
                    e.setVelocity(new Vector(0, 1.2, 0));
                }
                w.spawnParticle(Particle.GUST, l.clone().add(0, 0.5, 0), 8, 2.5, 0.3, 2.5, 0);
                w.playSound(l, Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 0.7f);
            }
            case LEQUE_ESPELHO -> {
                efeito(p, PotionEffectType.ABSORPTION, 120, 1);
                repetir(5, 24, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 2.4)) empurrar(e, c, 0.8, 0.2);
                    if (i % 3 == 0) anel(c.clone().add(0, 1, 0), 1.4, Particle.CHERRY_LEAVES, 8);
                });
                w.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
            case FURACAO_DE_JADE -> repetir(4, 10, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 5)) {
                    ferir(p, e, 0.6 * d);
                    puxar(p, e, 0.4);
                }
                for (int k = 0; k < 16; k++) {
                    double ang = Math.PI * 2 * k / 16 + i * 0.6;
                    double r = 1.5 + (k % 4);
                    w.spawnParticle(Particle.CHERRY_LEAVES, c.getX() + Math.cos(ang) * r, c.getY() + 0.5 + (k % 3), c.getZ() + Math.sin(ang) * r, 1, 0, 0, 0, 0);
                }
                if (i % 2 == 0) w.playSound(c, Sound.ENTITY_BREEZE_IDLE_GROUND, 1f, 1.2f);
            });

            // ---------------- Arpão (minério) ----------------
            case ARPOAR -> {
                double agua = p.isInWater() ? 1.5 : 1;
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.6, 18, false, visualLamina(p), (e, onde) -> {
                    ferir(p, e, 1.2 * d * agua);
                    puxar(p, e, 1.4);
                }, null);
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
            }
            case ESTOCADA_DO_ARPAO -> {
                LivingEntity a = alvo(p, 5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.4 * d);
                sangrar(p, a, 0.15 * d, 4);
                w.playSound(l, Sound.ITEM_TRIDENT_HIT, 1f, 0.8f);
            }
            case CORRENTE_MARINHA -> {
                double agua = p.isInWater() ? 1.5 : 1;
                Vector dir = horizontal(p);
                Location de = l.clone().add(0, 1, 0);
                for (LivingEntity e : linha(p, de, dir, 9, 1.4)) {
                    ferir(p, e, 1.0 * d * agua);
                    puxar(p, e, 1.2);
                }
                for (double s = 0; s <= 9; s += 0.4) w.spawnParticle(Particle.SPLASH, de.clone().add(dir.clone().multiply(s)), 3, 0.1, 0.1, 0.1, 0);
                w.playSound(l, Sound.ENTITY_GENERIC_SPLASH, 1f, 1f);
            }
            case MERGULHO_DO_TUBARAO -> {
                boolean agua = p.isInWater();
                Vector dir = agua ? p.getEyeLocation().getDirection().normalize() : horizontal(p);
                p.setVelocity(dir.multiply(agua ? 2.6 : 1.6).setY(agua ? dir.getY() : 0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 10, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.05);
                    for (LivingEntity e : perto(p, c, 2.0)) if (acertados.add(e.getUniqueId())) ferir(p, e, 1.3 * d * (agua ? 1.5 : 1));
                });
                w.playSound(l, Sound.ENTITY_DOLPHIN_JUMP, 1f, 1f);
            }
            case ARPAO_TRIPLO -> {
                double agua = p.isInWater() ? 1.5 : 1;
                Vector base = p.getEyeLocation().getDirection();
                for (int k = -1; k <= 1; k++) {
                    Vector dir = base.clone().rotateAroundY(Math.toRadians(15 * k));
                    lamina(p, p.getEyeLocation(), dir, 1.5, 16, false, visualLamina(p), (e, onde) -> {
                        ferir(p, e, 1.0 * d * agua);
                        puxar(p, e, 1.0);
                    }, null);
                }
                w.playSound(l, Sound.ITEM_TRIDENT_THROW, 1f, 0.8f);
            }
            case ONDA_DO_ARPAO -> {
                double agua = p.isInWater() ? 1.5 : 1;
                for (LivingEntity e : perto(p, l, 5)) {
                    ferir(p, e, 1.4 * d * agua);
                    empurrar(e, l, 1.2, 0.4);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 1);
                }
                for (double r = 1; r <= 5; r += 1) anel(l.clone().add(0, 0.3, 0), r, Particle.SPLASH, (int) (r * 10));
                w.playSound(l, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
            }
            case FURIA_DO_LEVIATA -> {
                double agua = p.isInWater() ? 1.5 : 1;
                lamina(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 1.8, 25, true, visualLamina(p), (e, onde) -> {
                    ferir(p, e, 3.0 * d * agua);
                    puxar(p, e, 1.0);
                    w.spawnParticle(Particle.SPLASH, onde, 30, 0.5, 0.5, 0.5, 0.2);
                }, null);
                w.playSound(l, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.4f);
            }

            // ---------------- Mangual (minério) ----------------
            case GOLPE_GELADO -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.4 * d);
                efeito(a, PotionEffectType.SLOWNESS, 60, 1);
                w.spawnParticle(Particle.SNOWFLAKE, a.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                w.playSound(l, Sound.BLOCK_GLASS_HIT, 1f, 0.8f);
            }
            case GIRO_DA_CORRENTE -> repetir(4, 4, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 3.5)) {
                    ferir(p, e, 0.6 * d);
                    empurrar(e, c, 0.5, 0.2);
                }
                anel(c.clone().add(0, 1, 0), 2.8, Particle.SNOWFLAKE, 12);
                w.playSound(c, Sound.BLOCK_METAL_HIT, 1f, 0.8f + i * 0.1f);
            });
            case QUEBRA_GELO -> {
                for (LivingEntity e : perto(p, l, 3.5)) {
                    ferir(p, e, 1.5 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 2);
                }
                w.spawnParticle(Particle.BLOCK, l, 80, 2, 0.2, 2, Material.PACKED_ICE.createBlockData());
                w.playSound(l, Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
            }
            case CORRENTE_QUE_PRENDE -> {
                LivingEntity a = alvo(p, 7);
                if (a == null) return falhar(p, "Nenhum inimigo ao alcance da corrente");
                ferir(p, a, 0.8 * d);
                puxar(p, a, 1.2);
                efeito(a, PotionEffectType.SLOWNESS, 40, 4);
                w.playSound(l, Sound.BLOCK_METAL_PLACE, 1f, 0.8f);
            }
            case NEVASCA -> repetir(6, 10, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 5)) {
                    ferir(p, e, 0.5 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 40, 1);
                    e.setFreezeTicks(Math.min(300, e.getFreezeTicks() + 30));
                }
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 50, 4, 1.5, 4, 0.02);
                if (i % 3 == 0) w.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.3f, 1.6f);
            });
            case PRISAO_DE_GELO -> {
                LivingEntity a = alvo(p, 8);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                ferir(p, a, 1.6 * d);
                efeito(a, PotionEffectType.SLOWNESS, 60, 6);
                a.setFreezeTicks(Math.max(a.getFreezeTicks(), 200));
                w.spawnParticle(Particle.BLOCK, a.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, Material.BLUE_ICE.createBlockData());
                w.playSound(a.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.6f);
            }
            case AVALANCHE -> {
                Vector dir = horizontal(p);
                repetir(5, 3, i -> {
                    Location pt = l.clone().add(dir.clone().multiply(3 * (i + 1)));
                    for (LivingEntity e : perto(p, pt, 2.6)) {
                        ferir(p, e, 2.2 * d);
                        efeito(e, PotionEffectType.SLOWNESS, 60, 2);
                        empurrar(e, l, 1.0, 0.4);
                    }
                    w.spawnParticle(Particle.BLOCK, pt.clone().add(0, 0.5, 0), 60, 1.2, 0.5, 1.2, Material.SNOW_BLOCK.createBlockData());
                    w.playSound(pt, Sound.BLOCK_SNOW_BREAK, 1.2f, 0.5f);
                });
            }

            // ---------------- Garras (minério) ----------------
            case RASGAR -> {
                LivingEntity a = alvo(p, 3.8);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                repetir(3, 4, i -> {
                    if (!a.isValid() || a.isDead()) return;
                    ferir(p, a, 0.6 * d);
                    curar(p, 0.6 * d * 0.15);
                    if (i % 2 == 1) p.swingOffHand();
                    w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0.05);
                    w.playSound(a.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.6f + i * 0.1f);
                });
            }
            case SALTO_DA_FERA -> {
                LivingEntity a = alvo(p, 8);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                Vector v = a.getLocation().toVector().subtract(l.toVector());
                p.setVelocity(v.multiply(0.22).setY(0.45));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                depois(8, () -> {
                    if (!a.isValid() || a.isDead() || !p.isOnline()) return;
                    if (a.getLocation().distanceSquared(p.getLocation()) > 16) return;
                    ferir(p, a, 1.3 * d);
                    w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.1);
                    w.playSound(a.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.8f, 1.4f);
                });
            }
            case SANGRIA -> {
                LivingEntity a = alvo(p, 4);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 0.6 * d);
                sangrar(p, a, 0.25 * d, 6);
                w.spawnParticle(Particle.BLOCK, a.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, Material.REDSTONE_BLOCK.createBlockData());
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
            }
            case FRENESI -> {
                efeito(p, PotionEffectType.HASTE, 160, 1);
                efeito(p, PotionEffectType.SPEED, 160, 0);
                efeito(p, PotionEffectType.STRENGTH, 160, 0);
                w.spawnParticle(Particle.ANGRY_VILLAGER, l.clone().add(0, 2, 0), 6, 0.4, 0.3, 0.4, 0);
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.6f);
            }
            case GARRAS_CRUZADAS -> {
                for (LivingEntity e : cone(p, 4, 110)) {
                    ferir(p, e, 1.3 * d);
                    sangrar(p, e, 0.15 * d, 4);
                }
                p.swingOffHand();
                Vector f = horizontal(p), lado = new Vector(-f.getZ(), 0, f.getX());
                Location c = p.getEyeLocation().add(f.clone().multiply(2));
                Particle.DustOptions corte = new Particle.DustOptions(Color.fromRGB(0xB0102A), 1f);
                for (double s = -1.5; s <= 1.5; s += 0.15) {
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                    w.spawnParticle(Particle.DUST, c.clone().add(lado.clone().multiply(s)).add(0, -s * 0.8, 0), 1, 0, 0, 0, 0, corte);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.3f);
            }
            case PREDADOR -> {
                LivingEntity a = alvo(p, 12);
                if (a == null) return falhar(p, "Nenhum inimigo à vista");
                marcar(a, 8, 0.3);
                efeito(p, PotionEffectType.INVISIBILITY, 60, 0);
                for (LivingEntity e : perto(p, l, 16)) if (e instanceof Mob m && p.equals(m.getTarget())) m.setTarget(null);
                w.spawnParticle(Particle.SMOKE, l.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.02);
                w.playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.6f);
            }
            case BANQUETE_DE_SANGUE -> repetir(5, 4, i -> {
                Location c = p.getLocation();
                double total = 0;
                for (LivingEntity e : perto(p, c, 4)) {
                    ferir(p, e, 0.6 * d);
                    total += 0.6 * d;
                }
                curar(p, total * 0.25);
                Particle.DustOptions verm = new Particle.DustOptions(Color.fromRGB(0xB0102A), 1.3f);
                for (int k = 0; k < 10; k++) {
                    double ang = i * 0.8 + k * Math.PI / 5;
                    w.spawnParticle(Particle.DUST, c.getX() + Math.cos(ang) * 2.6, c.getY() + 1, c.getZ() + Math.sin(ang) * 2.6, 1, 0, 0, 0, 0, verm);
                }
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 0.9f + i * 0.08f);
            });

            // ---------------- Clava Fumegante (minério) ----------------
            case PANCADA_EM_BRASA -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 1.5 * d);
                a.setFireTicks(Math.max(a.getFireTicks(), 80));
                w.spawnParticle(Particle.FLAME, a.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.03);
                w.playSound(l, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.2f);
            }
            case FUMACA_SUFOCANTE -> {
                for (LivingEntity e : perto(p, l, 4)) {
                    ferir(p, e, 0.4 * d);
                    efeito(e, PotionEffectType.BLINDNESS, 60, 0);
                    efeito(e, PotionEffectType.WEAKNESS, 100, 0);
                }
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l.clone().add(0, 1, 0), 40, 2, 0.8, 2, 0.02);
                w.playSound(l, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.6f);
            }
            case BRASAS_ESPALHADAS -> {
                for (LivingEntity e : cone(p, 5, 90)) {
                    ferir(p, e, 1.0 * d);
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                }
                w.spawnParticle(Particle.FLAME, p.getEyeLocation().add(horizontal(p).multiply(2.5)), 40, 1.2, 0.5, 1.2, 0.05);
                w.playSound(l, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
            }
            case GOLPE_SISMICO -> {
                for (LivingEntity e : perto(p, l, 4.5)) {
                    ferir(p, e, 1.4 * d);
                    e.setVelocity(new Vector(0, 0.8, 0));
                }
                w.spawnParticle(Particle.BLOCK, l, 80, 2.5, 0.2, 2.5, chao(l));
                w.playSound(l, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
            }
            case CHAMA_DO_PANTANO -> {
                Vector dir = horizontal(p);
                Location de = l.clone().add(0, 0.5, 0);
                for (LivingEntity e : linha(p, de, dir, 9, 1.4)) {
                    ferir(p, e, 1.5 * d);
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                }
                for (double s = 0; s <= 9; s += 0.35) {
                    w.spawnParticle(Particle.FLAME, de.clone().add(dir.clone().multiply(s)), 2, 0.1, 0.2, 0.1, 0.01);
                    if (((int) (s * 3)) % 3 == 0) w.spawnParticle(Particle.SMOKE, de.clone().add(dir.clone().multiply(s)), 1, 0.1, 0.3, 0.1, 0.01);
                }
                w.playSound(l, Sound.ITEM_FIRECHARGE_USE, 1f, 0.6f);
            }
            case FORNALHA_VIVA -> {
                efeito(p, PotionEffectType.FIRE_RESISTANCE, 120, 0);
                repetir(10, 10, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 3)) {
                        ferir(p, e, 0.4 * d);
                        e.setFireTicks(Math.max(e.getFireTicks(), 60));
                    }
                    anel(c.clone().add(0, 0.2, 0), 2.5, Particle.FLAME, 14);
                });
                w.playSound(l, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1f, 0.8f);
            }
            case ERUPCAO -> saltar(p, horizontal(p).multiply(0.3).setY(1.0), 6, onde -> {
                for (LivingEntity e : perto(p, onde, 5)) {
                    ferir(p, e, 3.0 * d);
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                    empurrar(e, onde, 1.0, 0.5);
                }
                w.spawnParticle(Particle.LAVA, onde, 30, 2, 0.5, 2, 0);
                w.spawnParticle(Particle.FLAME, onde.clone().add(0, 0.5, 0), 80, 2.5, 0.6, 2.5, 0.05);
                w.spawnParticle(Particle.EXPLOSION, onde, 3, 1, 0.3, 1, 0);
                w.playSound(onde, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
            });

            // ---------------- 2.54: técnicas secretas ----------------
            case VENTO_CORTANTE -> {
                Vector dir = p.getEyeLocation().getDirection().normalize();
                Location de = p.getEyeLocation().subtract(0, 0.3, 0);
                for (LivingEntity e : linha(p, de, dir, 12, 1.4)) {
                    ferir(p, e, 2.2 * d);
                    empurrar(e, l, 0.6, 0.2);
                }
                for (double s = 0.5; s <= 12; s += 0.5) {
                    Location pt = de.clone().add(dir.clone().multiply(s));
                    w.spawnParticle(Particle.SWEEP_ATTACK, pt, 1, 0, 0, 0, 0);
                    if (s % 2 == 0) w.spawnParticle(Particle.CLOUD, pt, 2, 0.2, 0.2, 0.2, 0.01);
                }
                w.playSound(l, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
            }
            case MACHADOS_ANCESTRAIS -> {
                ItemStack ax = new ItemStack(Material.GOLDEN_AXE);
                List<ItemDisplay> trio = new ArrayList<>();
                for (int k = 0; k < 3; k++) trio.add(display(l.clone().add(0, 1, 0), ax, 0.9f));
                repetir(2, 30, i -> {
                    if (!p.isOnline()) return;
                    Location c = p.getLocation().add(0, 1, 0);
                    for (int k = 0; k < trio.size(); k++) {
                        ItemDisplay x = trio.get(k);
                        if (!x.isValid()) continue;
                        double ang = i * 0.5 + k * Math.PI * 2 / 3;
                        Location pt = c.clone().add(Math.cos(ang) * 2.6, 0, Math.sin(ang) * 2.6);
                        x.teleport(pt);
                        x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) (i * 1.2), 1, 0, 0), new Vector3f(0.9f), new AxisAngle4f()));
                        for (LivingEntity e : perto(p, pt, 1.4)) ferir(p, e, 0.45 * d);
                        if (i == 29) tirar(x);
                    }
                    if (i % 5 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 0.7f);
                });
            }
            case MIL_ESTOCADAS -> repetir(2, 8, i -> {
                Vector dir = horizontal(p);
                Location de = p.getLocation().add(0, 1, 0);
                for (LivingEntity e : linha(p, de, dir, 5, 1.2)) ferir(p, e, 0.55 * d);
                for (double s = 0.5; s <= 5; s += 0.5) w.spawnParticle(Particle.CRIT, de.clone().add(dir.clone().multiply(s)), 1, 0.05, 0.05, 0.05, 0);
                w.playSound(de, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 1.4f + i * 0.05f);
            });
            case MAREMOTO -> repetir(2, 12, i -> {
                Vector dir = horizontal(p), lado = new Vector(-dir.getZ(), 0, dir.getX());
                Location frente = l.clone().add(dir.clone().multiply(i + 1));
                for (double s = -2.5; s <= 2.5; s += 0.5) {
                    Location pt = frente.clone().add(lado.clone().multiply(s));
                    w.spawnParticle(Particle.SPLASH, pt.clone().add(0, 0.6, 0), 4, 0.2, 0.4, 0.2, 0.1);
                    w.spawnParticle(Particle.BUBBLE_POP, pt.clone().add(0, 1.2, 0), 1, 0.1, 0.3, 0.1, 0.02);
                }
                for (LivingEntity e : perto(p, frente, 3)) {
                    ferir(p, e, 0.5 * d);
                    e.setVelocity(dir.clone().multiply(1.1).setY(0.35));
                }
                if (i % 3 == 0) w.playSound(frente, Sound.ENTITY_GENERIC_SPLASH, 1f, 0.6f);
            });
            case SINO_DO_JUIZO -> {
                for (LivingEntity e : perto(p, l, 6)) {
                    ferir(p, e, 2 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 50, 5);
                    efeito(e, PotionEffectType.WEAKNESS, 80, 1);
                }
                anel(l.clone().add(0, 0.3, 0), 3, Particle.END_ROD, 24);
                anel(l.clone().add(0, 0.3, 0), 6, Particle.END_ROD, 40);
                w.playSound(l, Sound.BLOCK_BELL_USE, 1.5f, 0.5f);
                w.playSound(l, Sound.BLOCK_BELL_RESONATE, 1f, 0.7f);
            }
            case FLECHA_COMETA -> {
                Location alvoChao = mira(p, 40);
                repetir(8, 3, i -> {
                    Location c = alvoChao.clone().add(rnd().nextDouble(-1.5, 1.5), 0, rnd().nextDouble(-1.5, 1.5));
                    for (double y = 8; y > 0; y -= 0.5) w.spawnParticle(Particle.FLAME, c.clone().add(0, y, 0), 1, 0.05, 0, 0.05, 0);
                    for (LivingEntity e : perto(p, c, 3)) {
                        ferir(p, e, 1.4 * b);
                        e.setFireTicks(Math.max(e.getFireTicks(), 80));
                    }
                    w.spawnParticle(Particle.EXPLOSION, c, 2, 0.4, 0.2, 0.4, 0);
                    w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
                });
            }
            case DANCA_DAS_SOMBRAS -> {
                List<LivingEntity> alvos = perto(p, l, 10);
                if (alvos.isEmpty()) return falhar(p, "Nenhum inimigo por perto");
                alvos.sort((x, y) -> Double.compare(x.getLocation().distanceSquared(l), y.getLocation().distanceSquared(l)));
                List<LivingEntity> tres = alvos.subList(0, Math.min(3, alvos.size()));
                repetir(6, tres.size(), i -> {
                    LivingEntity a = tres.get(i);
                    if (!a.isValid() || !p.isOnline()) return;
                    Vector costas = a.getLocation().getDirection().setY(0);
                    if (costas.lengthSquared() < 1e-4) costas = horizontal(p);
                    Location atras = a.getLocation().subtract(costas.normalize().multiply(1.2));
                    if (!atras.getBlock().isPassable() || !atras.clone().add(0, 1, 0).getBlock().isPassable()) atras = a.getLocation();
                    atras.setDirection(a.getLocation().toVector().subtract(atras.toVector()));
                    w.spawnParticle(Particle.SMOKE, p.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
                    p.teleport(atras);
                    ferir(p, a, 1.8 * d);
                    w.playSound(atras, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f + i * 0.15f);
                });
            }
            case TEMPESTADE_ESCARLATE -> repetir(3, 12, i -> {
                Location c = p.getLocation();
                if (i % 2 == 1) p.swingOffHand();
                for (LivingEntity e : perto(p, c, 4)) {
                    ferir(p, e, 0.4 * d);
                    if (i == 11) sangrar(p, e, 0.2 * d, 4);
                }
                Particle.DustOptions verm = new Particle.DustOptions(Color.fromRGB(0xC62828), 1.2f);
                for (int k = 0; k < 8; k++) {
                    double ang = i * 0.7 + k * Math.PI / 4;
                    w.spawnParticle(Particle.DUST, c.getX() + Math.cos(ang) * 2.5, c.getY() + 1, c.getZ() + Math.sin(ang) * 2.5, 1, 0, 0, 0, 0, verm);
                }
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.3f + i * 0.04f);
            });
            case MIL_LAMINAS -> repetir(1, 30, i -> {
                Vector dir = p.getEyeLocation().getDirection().rotateAroundY(Math.toRadians(rnd().nextDouble(-35, 35)));
                dir.setY(dir.getY() + rnd().nextDouble(-0.1, 0.1));
                lamina(p, p.getEyeLocation().subtract(0, 0.3, 0), dir, 1.4, 16, false, visualLamina(p), (e, onde) -> ferir(p, e, 0.5 * d), null);
                if (i % 5 == 0) w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.6f, 1.5f);
            });
            case LUA_DE_SANGUE -> {
                double total = 0;
                for (LivingEntity e : perto(p, l, 6)) {
                    ferir(p, e, 2.2 * d);
                    total += 2.2 * d;
                }
                curar(p, Math.min(10, total * 0.2));
                Particle.DustOptions verm = new Particle.DustOptions(Color.fromRGB(0x8B0000), 1.4f);
                for (int k = 0; k < 48; k++) {
                    double ang = Math.PI * 2 * k / 48;
                    w.spawnParticle(Particle.DUST, l.getX() + Math.cos(ang) * 5, l.getY() + 1, l.getZ() + Math.sin(ang) * 5, 1, 0, 0, 0, 0, verm);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
            }
            case MONTANHA_INABALAVEL -> {
                efeito(p, PotionEffectType.RESISTANCE, 120, 2);
                repetir(5, 24, i -> {
                    Location c = p.getLocation();
                    for (LivingEntity e : perto(p, c, 2.5)) empurrar(e, c, 1.2, 0.35);
                    anel(c.clone().add(0, 0.1, 0), 1.5, Particle.CRIT, 10);
                });
                w.playSound(l, Sound.BLOCK_STONE_PLACE, 1.5f, 0.5f);
            }
            case SERPENTE_RELAMPAGO -> {
                LivingEntity a = alvo(p, 12);
                if (a == null) return falhar(p, "Nenhum inimigo ao alcance do chicote");
                Set<UUID> ja = new HashSet<>();
                LivingEntity[] atual = {a};
                Location[] de = {p.getEyeLocation()};
                repetir(4, 5, i -> {
                    LivingEntity e = atual[0];
                    if (e == null || !e.isValid()) return;
                    ja.add(e.getUniqueId());
                    Location para = e.getLocation().add(0, e.getHeight() / 2, 0);
                    Vector v = para.toVector().subtract(de[0].toVector());
                    for (double s = 0; s < v.length(); s += 0.4) w.spawnParticle(Particle.ELECTRIC_SPARK, de[0].clone().add(v.clone().normalize().multiply(s)), 2, 0.05, 0.05, 0.05, 0);
                    ferir(p, e, 1.3 * d);
                    w.playSound(para, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
                    de[0] = para;
                    LivingEntity prox = null;
                    double melhor = 64;
                    for (LivingEntity o : perto(p, para, 8)) {
                        double dd = o.getLocation().distanceSquared(para);
                        if (!ja.contains(o.getUniqueId()) && dd < melhor) {
                            melhor = dd;
                            prox = o;
                        }
                    }
                    atual[0] = prox;
                });
            }
            case CEU_PARTIDO -> {
                p.setVelocity(new Vector(0, 0.6, 0));
                semQueda.put(p.getUniqueId(), agora() + 4000);
                depois(10, () -> {
                    if (!p.isOnline()) return;
                    Vector dir = horizontal(p);
                    Location de = p.getLocation().add(0, 0.5, 0);
                    for (LivingEntity e : linha(p, de, dir, 12, 1.8)) {
                        ferir(p, e, 4 * d);
                        e.setVelocity(e.getVelocity().setY(0.7));
                    }
                    for (double s = 0; s <= 12; s += 0.5) {
                        Location pt = de.clone().add(dir.clone().multiply(s));
                        w.spawnParticle(Particle.BLOCK, pt, 6, 0.3, 0.1, 0.3, chao(pt));
                        w.spawnParticle(Particle.END_ROD, pt.clone().add(0, 0.5, 0), 1, 0.05, 0.4, 0.05, 0);
                    }
                    w.playSound(de, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.4f);
                });
            }
            case ORBITA_ETERNA -> {
                ItemDisplay bum = display(l.clone().add(0, 1, 0), visualLamina(p), 1f);
                repetir(2, 30, i -> {
                    if (!bum.isValid() || !p.isOnline()) return;
                    double ang = i * 0.55;
                    Location c = p.getLocation().add(Math.cos(ang) * 3, 1, Math.sin(ang) * 3);
                    bum.teleport(c);
                    bum.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) (i * 1.4), 0, 1, 0), new Vector3f(1f), new AxisAngle4f()));
                    for (LivingEntity e : perto(p, c, 1.5)) ferir(p, e, 0.5 * d);
                    if (i % 6 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.6f);
                    if (i == 29) tirar(bum);
                });
            }
            case VENDAVAL_DE_PETALAS -> {
                for (LivingEntity e : cone(p, 8, 80)) {
                    ferir(p, e, 1.5 * d);
                    empurrar(e, l, 1.3, 0.4);
                }
                Vector f = horizontal(p);
                for (int k = 0; k < 60; k++) {
                    Vector v = f.clone().rotateAroundY(Math.toRadians(rnd().nextDouble(-40, 40))).multiply(rnd().nextDouble(1, 8));
                    w.spawnParticle(Particle.CHERRY_LEAVES, l.clone().add(v).add(0, rnd().nextDouble(0.3, 2), 0), 1, 0, 0, 0, 0);
                }
                w.playSound(l, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.4f);
            }
            case FISGADA_DO_LEVIATA -> {
                Vector dir = p.getEyeLocation().getDirection().normalize();
                List<LivingEntity> fisgados = linha(p, p.getEyeLocation(), dir, 15, 1.5);
                if (fisgados.isEmpty()) return falhar(p, "Ninguém na linha do arpão");
                for (LivingEntity e : fisgados) {
                    ferir(p, e, 1.2 * d);
                    puxar(p, e, 1.6);
                }
                for (double s = 0; s <= 15; s += 0.5) w.spawnParticle(Particle.BUBBLE_POP, p.getEyeLocation().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0);
                w.playSound(l, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.2f, 0.6f);
            }
            case NEVASCA_DE_FERRO -> repetir(3, 20, i -> {
                Location c = p.getLocation();
                for (LivingEntity e : perto(p, c, 4)) {
                    ferir(p, e, 0.35 * d);
                    e.setFreezeTicks(Math.min(e.getMaxFreezeTicks() + 60, e.getFreezeTicks() + 30));
                    efeito(e, PotionEffectType.SLOWNESS, 40, 2);
                }
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 20, 2, 0.6, 2, 0.02);
                if (i % 4 == 0) w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 0.6f);
            });
            case FRENESI_DA_FERA -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                repetir(2, 10, i -> {
                    if (!a.isValid() || a.isDead()) return;
                    ferir(p, a, 0.45 * d);
                    curar(p, 0.45 * d * 0.25);
                    if (i % 2 == 1) p.swingOffHand();
                    w.spawnParticle(Particle.DAMAGE_INDICATOR, a.getLocation().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.05);
                    w.playSound(a.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.4f, 1.6f);
                });
            }
            case ERUPCAO_VULCANICA -> {
                for (LivingEntity e : perto(p, l, 5)) {
                    ferir(p, e, 1.8 * d);
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                    e.setVelocity(e.getVelocity().setY(0.6));
                }
                for (int k = 0; k < 8; k++) {
                    double ang = Math.PI * 2 * k / 8;
                    Location pilar = l.clone().add(Math.cos(ang) * 3.5, 0, Math.sin(ang) * 3.5);
                    for (double y = 0; y < 3; y += 0.3) w.spawnParticle(Particle.FLAME, pilar.clone().add(0, y, 0), 3, 0.15, 0.05, 0.15, 0.01);
                    w.spawnParticle(Particle.LAVA, pilar, 3, 0.2, 0.1, 0.2, 0);
                }
                w.playSound(l, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
                w.playSound(l, Sound.BLOCK_LAVA_POP, 1f, 0.6f);
            }

            // ---------------- Lendas ----------------
            case PURIFICACAO_SAGRADA -> {
                for (LivingEntity e : perto(p, l, 5)) ferir(p, e, (mortoVivo(e) ? 3 : 1.5) * d);
                for (Player a : aliados(p, 8)) curar(a, 6);
                w.spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 120, 2.5, 1, 2.5, 0.05);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 1, 0), 40, 1, 1, 1, 0.3);
                w.playSound(l, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.6f);
            }
            case TEIA_DA_RAINHA -> {
                for (LivingEntity e : cone(p, 6, 80)) {
                    ferir(p, e, 1.2 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 4);
                    efeito(e, PotionEffectType.POISON, 100, 1);
                    w.spawnParticle(Particle.BLOCK, e.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, Material.COBWEB.createBlockData());
                }
                w.playSound(l, Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.6f);
            }
            case QUEDA_DA_FLORESTA -> {
                Vector dir = horizontal(p);
                Location de = p.getLocation();
                Set<UUID> acertados = new HashSet<>();
                w.playSound(de, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 0.5f);
                repetir(1, 9, i -> {
                    Location pt = de.clone().add(dir.clone().multiply(i + 1));
                    w.spawnParticle(Particle.BLOCK, pt.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, Material.OAK_LOG.createBlockData());
                    for (LivingEntity e : perto(p, pt, 1.8)) {
                        if (!acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 2.5 * d);
                        efeito(e, PotionEffectType.SLOWNESS, 40, 2);
                    }
                });
            }
            case COLAPSO_DO_ABISMO -> {
                for (LivingEntity e : perto(p, l, 6)) {
                    Vector v = l.toVector().subtract(e.getLocation().toVector()).setY(0);
                    if (v.lengthSquared() > 0.25) v.normalize().multiply(0.6);
                    e.setVelocity(v.setY(-0.8));
                    ferir(p, e, 2 * d);
                    efeito(e, PotionEffectType.SLOWNESS, 60, 5);
                }
                anel(l, 3, Particle.SQUID_INK, 20);
                w.spawnParticle(Particle.BLOCK, l, 120, 3, 0.2, 3, Material.DEEPSLATE.createBlockData());
                w.playSound(l, Sound.ENTITY_WARDEN_DIG, 1f, 0.8f);
            }
            case TEMPESTADE_DE_RAIOS -> {
                Location alvo = mira(p, 30);
                double poder = Math.max(d, b);
                repetir(5, 6, i -> {
                    Location pt = alvo.clone().add(rnd().nextDouble(-4, 4), 0, rnd().nextDouble(-4, 4));
                    w.strikeLightningEffect(pt);
                    for (LivingEntity e : perto(p, pt, 2.5)) ferir(p, e, 1.5 * poder);
                });
            }
            case VAZIO_DO_FIM -> {
                Location centro = l.clone().add(horizontal(p).multiply(3)).add(0, 0.5, 0);
                for (LivingEntity e : perto(p, l, 8)) {
                    Vector v = centro.toVector().subtract(e.getLocation().toVector());
                    if (v.lengthSquared() > 0.25) e.setVelocity(v.normalize().multiply(0.9).setY(0.3));
                }
                repetir(2, 10, i -> w.spawnParticle(Particle.REVERSE_PORTAL, centro, 30, 1.5, 1, 1.5, 0.1));
                depois(20, () -> {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, centro, 1);
                    for (LivingEntity e : perto(p, centro, 3.5)) {
                        ferir(p, e, 2 * d);
                        empurrar(e, centro, 1.0, 0.5);
                    }
                    w.playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                });
                w.playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
            }
            case EGIDE -> {
                efeito(p, PotionEffectType.ABSORPTION, 120, 3);
                egide.put(p.getUniqueId(), agora() + 6000);
                anel(l.clone().add(0, 1, 0), 1.2, Particle.END_ROD, 16);
                w.playSound(l, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.3f);
            }
            case PASSO_DO_ANDARILHO -> {
                Vector dir = horizontal(p);
                p.setVelocity(dir.clone().multiply(2.6).setY(0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                efeito(p, PotionEffectType.SPEED, 80, 2);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 14, i -> {
                    Location c = p.getLocation();
                    w.spawnParticle(Particle.CLOUD, c, 3, 0.2, 0.1, 0.2, 0.01);
                    for (LivingEntity e : perto(p, c, 2)) if (acertados.add(e.getUniqueId())) ferir(p, e, 1.5 * d);
                });
                w.playSound(l, Sound.ENTITY_BREEZE_JUMP, 1f, 1f);
            }
            case SENTENCA -> {
                LivingEntity a = alvo(p, 5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                boolean fraco = max != null && a.getHealth() < max.getValue() * 0.25;
                if (Chefes.ehChefe(a)) ferir(p, a, 5 * d);
                else if (fraco) ferir(p, a, a.getHealth() + 1000);
                else ferir(p, a, 1.5 * d);
                w.spawnParticle(Particle.SOUL, a.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.05);
                w.playSound(a.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
            }
            case BIGORNA_CELESTE -> {
                LivingEntity a = alvo(p, 20);
                Location alvo = a != null ? a.getLocation() : mira(p, 20);
                Location topo = alvo.clone().add(0, 10, 0);
                ItemDisplay bigorna = display(topo, new ItemStack(Material.ANVIL), 2f);
                repetir(1, 12, i -> { if (bigorna.isValid()) bigorna.teleport(topo.clone().subtract(0, i * 0.85, 0)); });
                depois(13, () -> {
                    tirar(bigorna);
                    w.spawnParticle(Particle.END_ROD, alvo, 60, 1.5, 0.5, 1.5, 0.1);
                    for (LivingEntity e : perto(p, alvo, 3)) {
                        ferir(p, e, 3 * d);
                        efeito(e, PotionEffectType.SLOWNESS, 40, 4);
                        efeito(e, PotionEffectType.WEAKNESS, 40, 1);
                    }
                    w.playSound(alvo, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.6f);
                });
            }
            case LAMINA_ARCANA_SUPREMA -> {
                plugin.arcano().darMana(p, 30);
                for (int k = 0; k < 5; k++) {
                    Location pos = p.getEyeLocation().add(rnd().nextDouble(-1, 1), rnd().nextDouble(0, 1), rnd().nextDouble(-1, 1));
                    boolean[] fim = {false};
                    repetir(1, 50, i -> {
                        if (fim[0]) return;
                        LivingEntity melhor = null;
                        double dist = 16 * 16;
                        for (LivingEntity e : perto(p, pos, 16)) {
                            double x = e.getLocation().distanceSquared(pos);
                            if (x < dist) { dist = x; melhor = e; }
                        }
                        if (melhor == null) { fim[0] = true; return; }
                        Vector para = melhor.getLocation().add(0, melhor.getHeight() / 2, 0).toVector().subtract(pos.toVector());
                        if (para.lengthSquared() < 1.4) {
                            ferir(p, melhor, 1.0 * d);
                            w.spawnParticle(Particle.WITCH, pos, 10, 0.2, 0.2, 0.2, 0.05);
                            fim[0] = true;
                            return;
                        }
                        pos.add(para.normalize().multiply(0.8));
                        w.spawnParticle(Particle.WITCH, pos, 2, 0.05, 0.05, 0.05, 0);
                    });
                }
                w.playSound(l, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.4f);
            }
            case CHAMADO_DA_ALCATEIA -> {
                for (int k = 0; k < 3; k++) {
                    Location onde = l.clone().add(Math.cos(k * 2.1) * 2, 0, Math.sin(k * 2.1) * 2);
                    org.bukkit.entity.Wolf lobo = w.spawn(onde, org.bukkit.entity.Wolf.class, o -> {
                        o.setTamed(true);
                        o.setOwner(p);
                        o.setPersistent(false);
                        o.setGlowing(true);
                        o.customName(Component.text("Lobo Espectral", NamedTextColor.AQUA));
                        o.getPersistentDataContainer().set(K_INVOCACAO, PersistentDataType.STRING, p.getUniqueId().toString());
                        AttributeInstance vida = o.getAttribute(Attribute.MAX_HEALTH);
                        if (vida != null) { vida.setBaseValue(40); o.setHealth(40); }
                        AttributeInstance dano = o.getAttribute(Attribute.ATTACK_DAMAGE);
                        if (dano != null) dano.setBaseValue(6 * forca);
                    });
                    w.spawnParticle(Particle.SOUL, onde.clone().add(0, 0.5, 0), 20, 0.3, 0.4, 0.3, 0.02);
                    depois(400, () -> {
                        if (lobo.isValid()) {
                            lobo.getWorld().spawnParticle(Particle.SOUL, lobo.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
                            lobo.remove();
                        }
                    });
                }
                w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.6f);
            }

            // ---------------- Classes ----------------
            case FURIA_SANGRENTA -> {
                if (p.getHealth() <= 6) return falhar(p, "Vida baixa demais para a Fúria Sangrenta");
                p.setHealth(p.getHealth() - 4);
                int n = 0;
                for (LivingEntity e : perto(p, l, 4)) {
                    ferir(p, e, 2.5 * d);
                    n++;
                }
                curar(p, Math.min(10, n * 0.75 * d));
                anel(l.clone().add(0, 1, 0), 2.5, Particle.SWEEP_ATTACK, 8);
                w.spawnParticle(Particle.DUST, l.clone().add(0, 1, 0), 60, 2, 0.6, 2, 0, new Particle.DustOptions(Color.fromRGB(0xB71C1C), 1.5f));
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
            }
            case CARGA_DO_CAVALEIRO -> {
                Entity quem = p.getVehicle() != null ? p.getVehicle() : p;
                double mult = quem != p ? 2 : 1;
                quem.setVelocity(horizontal(p).multiply(2.2).setY(0.15));
                semQueda.put(p.getUniqueId(), agora() + 3000);
                Set<UUID> acertados = new HashSet<>();
                repetir(1, 12, i -> {
                    Location c = quem.getLocation();
                    w.spawnParticle(Particle.CLOUD, c, 4, 0.3, 0.1, 0.3, 0.02);
                    for (LivingEntity e : perto(p, c, 2.2)) {
                        if (e == quem || !acertados.add(e.getUniqueId())) continue;
                        ferir(p, e, 2 * d * mult);
                        empurrar(e, c, 1.6, 0.5);
                    }
                });
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
            }
            case MARTELO_SAGRADO -> {
                for (LivingEntity e : perto(p, l, 5)) ferir(p, e, (mortoVivo(e) ? 3 : 2) * d);
                for (Player a : aliados(p, 8)) {
                    curar(a, 4);
                    efeito(a, PotionEffectType.REGENERATION, 100, 0);
                }
                w.spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 80, 2.5, 0.5, 2.5, 0.05);
                w.playSound(l, Sound.BLOCK_BELL_RESONATE, 1f, 1.2f);
            }
            case TIRO_NA_CABECA -> {
                Arrow a = flecha(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 5f, 3.5 * b);
                a.setGlowing(true);
                a.setPierceLevel(1);
                w.playSound(l, Sound.ENTITY_ARROW_SHOOT, 1f, 2f);
            }
            case FLECHAS_DA_MATILHA -> {
                LivingEntity alvo = alvo(p, 40);
                if (alvo == null) return falhar(p, "Nenhum alvo à vista");
                Vector dir = p.getEyeLocation().getDirection();
                for (int k = -1; k <= 1; k++) flecha(p, p.getEyeLocation(), dir.clone().rotateAroundY(Math.toRadians(k * 4)), 3f, b);
                marcar(alvo, 8, 0.2);
                for (var r : plugin.companheiros().de(p.getUniqueId())) {
                    if (plugin.getServer().getEntity(r.id()) instanceof Mob m && m.getWorld().equals(w)
                            && m.getLocation().distanceSquared(l) < 30 * 30) m.setTarget(alvo);
                }
                w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1f, 1.2f);
            }
            case PRISMA_ELEMENTAL -> {
                List<LivingEntity> alvos = cone(p, 6, 90);
                for (LivingEntity e : alvos) {
                    ferir(p, e, 1.5 * d);
                    e.setFireTicks(Math.max(e.getFireTicks(), 80));
                    efeito(e, PotionEffectType.SLOWNESS, 60, 1);
                }
                if (!alvos.isEmpty()) w.strikeLightningEffect(alvos.getFirst().getLocation());
                Vector f = horizontal(p);
                for (double s = 1; s <= 6; s += 0.5) {
                    Location pt = l.clone().add(f.clone().multiply(s)).add(0, 1, 0);
                    w.spawnParticle(Particle.FLAME, pt, 3, s * 0.12, 0.2, s * 0.12, 0.01);
                    w.spawnParticle(Particle.SNOWFLAKE, pt, 3, s * 0.12, 0.2, s * 0.12, 0.01);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 3, s * 0.12, 0.2, s * 0.12, 0.01);
                }
                w.playSound(l, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.8f);
            }
            case LAMINA_RUNICA -> repetir(6, 3, i -> {
                for (LivingEntity e : cone(p, 4.5, 120)) ferir(p, e, 1.0 * d);
                plugin.arcano().darMana(p, 7);
                Location c = p.getEyeLocation().add(horizontal(p).multiply(1.5));
                w.spawnParticle(Particle.WITCH, c, 25, 0.8, 0.4, 0.8, 0.05);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.6f + i * 0.1f);
            });
            case MIL_CORTES -> {
                List<LivingEntity> alvos = new ArrayList<>(perto(p, l, 10));
                if (alvos.isEmpty()) return falhar(p, "Nenhum inimigo perto");
                alvos.sort((x, y) -> Double.compare(x.getLocation().distanceSquared(l), y.getLocation().distanceSquared(l)));
                List<LivingEntity> fila = alvos.subList(0, Math.min(5, alvos.size()));
                for (int k = 0; k < fila.size(); k++) {
                    LivingEntity a = fila.get(k);
                    depois(1 + k * 4L, () -> {
                        if (!p.isOnline() || !a.isValid() || a.isDead()) return;
                        Vector costas = a.getLocation().getDirection().setY(0);
                        if (costas.lengthSquared() < 1e-4) costas = new Vector(1, 0, 0);
                        Location atras = a.getLocation().subtract(costas.normalize().multiply(1.2));
                        if (!atras.getBlock().isPassable()) atras = a.getLocation();
                        atras.setDirection(a.getLocation().toVector().subtract(atras.toVector()));
                        p.teleport(atras);
                        ferir(p, a, 1.2 * d);
                        w.spawnParticle(Particle.SWEEP_ATTACK, a.getLocation().add(0, 1, 0), 1);
                        w.playSound(atras, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
                    });
                }
            }
            case MARTELO_INCANDESCENTE -> {
                LivingEntity a = alvo(p, 4.5);
                if (a == null) return falhar(p, "Nenhum inimigo na sua frente");
                ferir(p, a, 2.5 * d);
                a.setFireTicks(Math.max(a.getFireTicks(), 120));
                marcar(a, 6, 0.25);
                w.spawnParticle(Particle.LAVA, a.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0);
                w.playSound(a.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
            }
            case INVESTIDA_DA_FERA -> {
                LivingEntity alvo = alvo(p, 15);
                if (alvo == null) return falhar(p, "Nenhum inimigo à vista");
                for (var r : plugin.companheiros().de(p.getUniqueId())) {
                    if (!(plugin.getServer().getEntity(r.id()) instanceof Mob m) || !m.getWorld().equals(w)
                            || m.getLocation().distanceSquared(l) > 20 * 20) continue;
                    m.setTarget(alvo);
                    Vector salto = alvo.getLocation().toVector().subtract(m.getLocation().toVector());
                    if (salto.lengthSquared() > 1) m.setVelocity(salto.normalize().multiply(1.2).setY(0.5));
                    efeito(m, PotionEffectType.STRENGTH, 200, 0);
                }
                Vector dir = alvo.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                for (LivingEntity e : linha(p, p.getEyeLocation(), dir.lengthSquared() < 1e-4 ? horizontal(p) : dir, 6, 1.2)) ferir(p, e, 1.5 * d);
                w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.8f);
            }
        }
        return true;
    }

    /** Explosão do finalizador em cadeia (depois do golpe que o gastou). */
    void finalizar(Player p, double forca) {
        Location l = p.getLocation();
        World w = p.getWorld();
        for (LivingEntity e : perto(p, l, 4)) {
            ferir(p, e, base(p) * forca);
            empurrar(e, l, 0.9, 0.4);
        }
        w.spawnParticle(Particle.EXPLOSION_EMITTER, l, 1);
        anel(l.clone().add(0, 0.5, 0), 3.5, Particle.END_ROD, 24);
        w.playSound(l, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.4f);
    }

    private static boolean mortoVivo(Entity e) {
        return e instanceof org.bukkit.entity.Zombie || e instanceof org.bukkit.entity.AbstractSkeleton || e instanceof org.bukkit.entity.Phantom
                || e instanceof org.bukkit.entity.Wither || e instanceof org.bukkit.entity.Zoglin;
    }

    private List<Player> aliados(Player p, double raio) {
        List<Player> l = new ArrayList<>();
        l.add(p);
        Party pt = plugin.parties().party(p);
        if (pt == null) return l;
        for (Player o : pt.online()) {
            if (o != p && o.getWorld().equals(p.getWorld()) && o.getLocation().distanceSquared(p.getLocation()) <= raio * raio) l.add(o);
        }
        return l;
    }

    private static void curar(LivingEntity e, double qtd) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max == null || e.isDead()) return;
        e.setHealth(Math.min(max.getValue(), e.getHealth() + qtd));
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        Long ate = semQueda.remove(e.getEntity().getUniqueId());
        if (ate != null && ate > agora()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        if (!refletindo && e.getEntity() instanceof Player v) {
            Long ate = egide.get(v.getUniqueId());
            Entity fonte = e.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Entity s ? s : e.getDamager();
            if (ate != null && ate > agora() && fonte instanceof LivingEntity atacante && atacante != v) {
                refletindo = true;
                try {
                    plugin.alvos().ferir(v, atacante, e.getDamage() * 0.5);
                } finally {
                    refletindo = false;
                }
            }
        }
        Marca m = marcas.get(e.getEntity().getUniqueId());
        if (m != null) {
            if (m.ate > agora()) e.setDamage(e.getDamage() * (1 + m.bonus));
            else marcas.remove(e.getEntity().getUniqueId());
        }
        if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity) {
            Long ate = furia.get(p.getUniqueId());
            if (ate != null && ate > agora()) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + 1));
            }
        }
        if (e.getDamager() instanceof Arrow a && "sombria".equals(a.getPersistentDataContainer().get(K_FLECHA, PersistentDataType.STRING))
                && e.getEntity() instanceof LivingEntity alvo) {
            marcar(alvo, 8, 0.2);
            alvo.setGlowing(true);
            depois(160, () -> { if (alvo.isValid()) alvo.setGlowing(false); });
        }
    }

    @EventHandler
    public void aoAcertarFlecha(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Arrow a) || !(a.getShooter() instanceof Player p)) return;
        if (!"explosiva".equals(a.getPersistentDataContainer().get(K_FLECHA, PersistentDataType.STRING))) return;
        Double dano = a.getPersistentDataContainer().get(K_FLECHA_DANO, PersistentDataType.DOUBLE);
        Location c = a.getLocation();
        c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
        c.getWorld().spawnParticle(Particle.FLAME, c, 40, 1, 1, 1, 0.1);
        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
        for (LivingEntity v : perto(p, c, 3)) {
            ferir(p, v, dano == null ? 6 : dano);
            empurrar(v, c, 0.8, 0.4);
        }
        a.remove();
    }

    public void parar() {
        for (BukkitTask t : tarefas) t.cancel();
        tarefas.clear();
        for (UUID id : displays) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) e.remove();
        }
        displays.clear();
    }
}
