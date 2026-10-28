package br.rpgatributos.mar;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.detalhes.BarcoGrande;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Squid;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O Kraken (3.0): ataca barcos no mar fundo, à noite ou na tempestade. Primeiro só os tentáculos
 * aparecem, em volta do barco: apertam o casco (o Barco Grande perde vida; do barco pequeno ele
 * arranca quem rema) e agarram quem está na água. Cortados os quatro, o corpo sobe e fica
 * vulnerável; ele cospe tinta e um tentáculo volta a crescer de vez em quando.
 * <p>Corpo e tentáculos são lulas grandes sem cérebro (o plugin as move): nada fica salvo no mundo.
 */
public final class Kraken {

    static final NamespacedKey K = new NamespacedKey("rpgatributos", "kraken");
    static final TextColor COR = TextColor.color(0x7E57C2);
    private static final int TENTACULOS = 4;
    private static final double VIDA_CORPO = 320, VIDA_TENTACULO = 36;

    private final RPGAtributos plugin;
    private Squid corpo;
    private final List<Squid> tentaculos = new ArrayList<>();
    private UUID barco;
    private Location centro;
    private BossBar barra;
    private long fim, proximaTinta, proximoBroto;
    private boolean exposto;
    private double angulo;
    private int ciclo;
    /** Quando o mar pode trazer o próximo Kraken sozinho. */
    long proximoNatural;

    Kraken(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    boolean ativo() {
        return corpo != null;
    }

    // =====================================================================
    //  Subir do fundo
    // =====================================================================

    /** Chama o Kraken para o barco do jogador (ou para onde ele nada). @return erro ou null. */
    String invocar(Player p) {
        if (ativo()) return "Já tem um Kraken no mar.";
        Boat b = Oceano.barco(p);
        Location ref = b != null ? b.getLocation() : p.getLocation();
        World w = ref.getWorld();
        Double sup = Oceano.superficie(w, ref.getBlockX(), ref.getBlockZ(), 8);
        if (sup == null) return "O Kraken precisa de mar fundo (8 blocos de água).";
        barco = b == null ? null : b.getUniqueId();
        centro = new Location(w, ref.getX(), sup, ref.getZ());
        exposto = false;
        angulo = 0;
        fim = System.currentTimeMillis() + 7 * 60_000L;
        proximaTinta = 0;
        proximoBroto = 0;
        corpo = w.spawn(centro.clone().add(0, -7, 0), Squid.class, s -> preparar(s, "corpo", 6.5, VIDA_CORPO));
        corpo.customName(Component.text("Kraken", COR, TextDecoration.BOLD));
        corpo.setCustomNameVisible(true);
        tentaculos.clear();
        for (int i = 0; i < TENTACULOS; i++) brotar(w);
        barra = BossBar.bossBar(Component.text("Kraken: corte os tentáculos", COR), 1f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);
        w.playSound(centro, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2f, 0.5f);
        w.playSound(centro, Sound.AMBIENT_UNDERWATER_ENTER, 2f, 0.5f);
        w.spawnParticle(Particle.SQUID_INK, centro, 120, 4, 0.5, 4, 0.05);
        for (Player o : w.getPlayers()) {
            if (o.getLocation().distanceSquared(centro) > 60 * 60) continue;
            o.showTitle(Title.title(Component.text("🐙 O Kraken!", COR, TextDecoration.BOLD),
                    Component.text("Tentáculos em volta do barco. Corte os quatro!", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
        return null;
    }

    private void preparar(Squid s, String parte, double escala, double vida) {
        s.setAI(false);
        s.setPersistent(false);
        s.setRemoveWhenFarAway(false);
        s.getPersistentDataContainer().set(K, PersistentDataType.STRING, parte);
        AttributeInstance e = s.getAttribute(Attribute.SCALE);
        if (e != null) e.setBaseValue(escala);
        AttributeInstance v = s.getAttribute(Attribute.MAX_HEALTH);
        if (v != null) {
            v.setBaseValue(vida);
            s.setHealth(vida);
        }
    }

    private void brotar(World w) {
        Squid t = w.spawn(centro.clone().add(0, -2, 0), Squid.class, s -> preparar(s, "tentaculo", 2.4, VIDA_TENTACULO));
        tentaculos.add(t);
        w.spawnParticle(Particle.BUBBLE_COLUMN_UP, t.getLocation(), 30, 0.5, 1, 0.5, 0.1);
    }

    // =====================================================================
    //  Movimento (a cada 2 ticks)
    // =====================================================================

    void mover() {
        if (!ativo()) return;
        if (!corpo.isValid()) {
            retirar(null);
            return;
        }
        Entity b = barco == null ? null : Bukkit.getEntity(barco);
        if (b != null && b.isValid() && b.getWorld().equals(centro.getWorld())) {
            centro.setX(b.getLocation().getX());
            centro.setZ(b.getLocation().getZ());
        }
        angulo += 0.045;
        double t = Bukkit.getCurrentTick() * 0.15;
        for (int i = 0; i < tentaculos.size(); i++) {
            Squid s = tentaculos.get(i);
            if (!s.isValid()) continue;
            double a = angulo + i * Math.PI * 2 / Math.max(1, tentaculos.size());
            Location l = centro.clone().add(Math.cos(a) * 2.6, -1.1 + Math.sin(t + i) * 0.45, Math.sin(a) * 2.6);
            l.setDirection(centro.toVector().subtract(l.toVector()).setY(0.6));
            s.teleport(l);
        }
        Location c = exposto
                ? centro.clone().add(Math.cos(-angulo * 0.6) * 6, -1.2, Math.sin(-angulo * 0.6) * 6)
                : centro.clone().add(0, -7, 0);
        c.setDirection(centro.toVector().subtract(c.toVector()));
        corpo.teleport(c);
    }

    // =====================================================================
    //  Ataques (a cada segundo)
    // =====================================================================

    void segundo() {
        if (!ativo()) return;
        ciclo++;
        long agora = System.currentTimeMillis();
        World w = centro.getWorld();
        List<Player> perto = new ArrayList<>();
        for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(centro) <= 64 * 64) perto.add(p);
        if (perto.isEmpty() || agora > fim) {
            retirar("🐙 O Kraken afundou de volta para o escuro.");
            return;
        }
        AttributeInstance max = corpo.getAttribute(Attribute.MAX_HEALTH);
        int vivos = (int) tentaculos.stream().filter(Entity::isValid).count();
        barra.name(Component.text(exposto ? "Kraken" : "Kraken: corte os tentáculos (" + vivos + ")", COR));
        barra.progress((float) Math.max(0, Math.min(1, exposto ? corpo.getHealth() / (max == null ? VIDA_CORPO : max.getValue()) : vivos / (double) TENTACULOS)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (perto.contains(p)) p.showBossBar(barra);
            else p.hideBossBar(barra);
        }
        if (ciclo % 2 != 0) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Entity eb = barco == null ? null : Bukkit.getEntity(barco);
        Boat alvo = eb instanceof Boat b && b.isValid() ? b : null;
        for (Squid t : tentaculos) {
            if (!t.isValid()) continue;
            Location lt = t.getLocation();
            w.spawnParticle(Particle.SPLASH, lt.clone().add(0, 1, 0), 10, 0.4, 0.2, 0.4, 0.2);
            if (alvo != null && alvo.getLocation().distanceSquared(lt) <= 4.5 * 4.5) {
                if (BarcoGrande.eBarcoGrande(alvo)) {
                    if (r.nextInt(2) == 0) plugin.barcoGrande().melhorias().danificar(alvo, 3, "O Kraken aperta o casco");
                } else if (r.nextInt(5) == 0 && !alvo.getPassengers().isEmpty() && alvo.getPassengers().getFirst() instanceof Player remador) {
                    alvo.removePassenger(remador);
                    remador.damage(4);
                    remador.setVelocity(new Vector(0, -0.5, 0));
                    remador.sendActionBar(Component.text("🐙 O Kraken te arrancou do barco!", COR));
                }
            }
            for (Player p : perto) {
                if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
                if (p.getLocation().distanceSquared(lt) > 3.2 * 3.2) continue;
                p.damage(3, t);
                if (p.isInWater()) {
                    p.setVelocity(new Vector(0, -0.45, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                }
            }
        }
        if (!exposto) return;
        if (agora >= proximaTinta) {
            proximaTinta = agora + 7000;
            Location c = corpo.getLocation();
            w.spawnParticle(Particle.SQUID_INK, c, 150, 3, 1, 3, 0.1);
            w.playSound(c, Sound.ENTITY_SQUID_SQUIRT, 2f, 0.5f);
            for (Player p : perto) {
                if (p.getLocation().distanceSquared(c) <= 11 * 11) p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            }
        }
        if (agora >= proximoBroto && vivos < 2) {
            proximoBroto = agora + 20000;
            brotar(w);
        }
    }

    // =====================================================================
    //  Dano e morte
    // =====================================================================

    /** O corpo só apanha depois que sobe (com os tentáculos cortados). */
    void aoFerir(EntityDamageEvent e) {
        String parte = e.getEntity().getPersistentDataContainer().get(K, PersistentDataType.STRING);
        if (parte == null) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.DROWNING
                || e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            e.setCancelled(true);
            return;
        }
        if (parte.equals("corpo") && !exposto) {
            e.setCancelled(true);
            if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent d && d.getDamager() instanceof Player p) {
                p.sendActionBar(Component.text("🐙 O corpo está fundo demais: corte os tentáculos primeiro!", COR));
            }
        }
    }

    void aoMorrer(EntityDeathEvent e) {
        String parte = e.getEntity().getPersistentDataContainer().get(K, PersistentDataType.STRING);
        if (parte == null) return;
        e.getDrops().clear();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (parte.equals("tentaculo")) {
            e.getDrops().add(new ItemStack(Material.INK_SAC, 2 + r.nextInt(3)));
            tentaculos.remove((Squid) e.getEntity());
            if (!exposto && tentaculos.stream().noneMatch(Entity::isValid)) expor();
            return;
        }
        if (!ativo() || e.getEntity() != corpo) return;
        e.getDrops().add(new ItemStack(Material.INK_SAC, 16 + r.nextInt(8)));
        e.getDrops().add(new ItemStack(Material.GLOW_INK_SAC, 6 + r.nextInt(6)));
        e.getDrops().add(new ItemStack(Material.NAUTILUS_SHELL, 2 + r.nextInt(3)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 10 + r.nextInt(12)));
        e.getDrops().add(new ItemStack(Material.PRISMARINE_CRYSTALS, 6 + r.nextInt(8)));
        e.getDrops().add(Reagente.PEROLA_NEGRA.criar(2));
        if (r.nextDouble() < 0.4) e.getDrops().add(new ItemStack(Material.HEART_OF_THE_SEA));
        e.setDroppedExp(200);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "kraken", "Venceu o Kraken");
            plugin.titulos().registrar(quem, "kraken", 1);
            Bukkit.broadcast(Component.text("🐙 ", COR).append(Component.text(quem.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" venceu o Kraken em alto-mar!", NamedTextColor.GRAY)));
        }
        corpo = null;
        retirar(null);
    }

    private void expor() {
        exposto = true;
        World w = centro.getWorld();
        corpo.setGlowing(true);
        proximoBroto = System.currentTimeMillis() + 15000;
        w.playSound(centro, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 2f, 0.4f);
        w.spawnParticle(Particle.BUBBLE_COLUMN_UP, centro, 80, 3, 1, 3, 0.2);
        for (Player o : w.getPlayers()) {
            if (o.getLocation().distanceSquared(centro) > 64 * 64) continue;
            o.showTitle(Title.title(Component.text("🐙 O Kraken subiu!", COR, TextDecoration.BOLD),
                    Component.text("Agora ele está vulnerável.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2000), Duration.ofMillis(500))));
        }
    }

    /** Some com tudo (avisa quem estava perto). */
    void retirar(String aviso) {
        if (corpo != null && corpo.isValid()) corpo.remove();
        for (Squid t : tentaculos) if (t.isValid()) t.remove();
        tentaculos.clear();
        if (barra != null) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
        if (aviso != null && centro != null) {
            for (Player p : centro.getWorld().getPlayers()) if (p.getLocation().distanceSquared(centro) <= 80 * 80) p.sendMessage(Component.text(aviso, COR));
        }
        corpo = null;
        barra = null;
        barco = null;
    }

    /** Uma parte do Kraken que ficou para trás (o servidor caiu no meio da luta). */
    static boolean sobra(Entity e) {
        return e.getPersistentDataContainer().has(K);
    }

    boolean conhecido(Entity e) {
        return e == corpo || tentaculos.contains(e);
    }
}
