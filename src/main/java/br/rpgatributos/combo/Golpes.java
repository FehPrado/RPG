package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.party.Party;
import br.rpgatributos.sombra.Sombras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (!(e instanceof LivingEntity le) || e == p || !le.isValid() || le.isDead() || e instanceof ArmorStand) return false;
        if (Sombras.eh(e)) return plugin.sombras().inimigoDoJogador(p, e);
        if (Companheiros.eh(e)) {
            UUID d = Companheiros.dono(e);
            return d != null && !d.equals(p.getUniqueId()) && pvp(p, d);
        }
        if (e instanceof Tameable t && t.isTamed()) return false;
        String inv = e.getPersistentDataContainer().get(K_INVOCACAO, PersistentDataType.STRING);
        if (inv != null) {
            try {
                UUID u = UUID.fromString(inv);
                return !u.equals(p.getUniqueId()) && pvp(p, u);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        UUID colono = plugin.colonias().colonoDe(e);
        if (colono != null) return plugin.reinos().inimigosEmGuerra(p.getUniqueId(), colono);
        if (e instanceof Player o) {
            return (o.getGameMode() == GameMode.SURVIVAL || o.getGameMode() == GameMode.ADVENTURE) && plugin.pvpPermitido(p, o);
        }
        if (e instanceof Enemy || Chefes.ehChefe(e) || Chefes.ehLacaio(e)) return true;
        return e instanceof Mob m && m.getTarget() == p;
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

    private void ferir(Player p, LivingEntity e, double valor) {
        if (!e.isValid() || e.isDead()) return;
        e.setNoDamageTicks(0);
        e.damage(valor, p);
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
                    atacante.damage(e.getDamage() * 0.5, v);
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
