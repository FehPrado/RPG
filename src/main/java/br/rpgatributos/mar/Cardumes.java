package br.rpgatributos.mar;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Cod;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.TropicalFish;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cardumes (3.0): manchas de peixe no mar aberto, perto de quem navega. A água ferve de peixe e
 * espirra; pescar dentro dela rende mais peixe raro e peixe em dobro, e a rede do Barco Grande
 * pega muito mais. Cada cardume dura alguns minutos e some (aparece outro em outro lugar).
 * Os cardumes grandes, no mar fundo, trazem uma baleia: o jato de água dela se vê de longe.
 */
public final class Cardumes {

    static final NamespacedKey K_PEIXE = new NamespacedKey("rpgatributos", "cardume_peixe");
    static final NamespacedKey K_BALEIA = new NamespacedKey("rpgatributos", "baleia");

    static final class Cardume {
        final World w;
        final double x, y, z;
        final long fim;
        final boolean grande;
        final List<UUID> peixes = new ArrayList<>();
        UUID baleia;

        Cardume(World w, double x, double y, double z, long fim, boolean grande) {
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
            this.fim = fim;
            this.grande = grande;
        }

        double raio() {
            return grande ? 9 : 6;
        }

        Location centro() {
            return new Location(w, x, y, z);
        }
    }

    private final RPGAtributos plugin;
    private final List<Cardume> ativos = new ArrayList<>();

    Cardumes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** O lugar está dentro de um cardume (para a vara e a rede). */
    public boolean em(Location l) {
        return dentro(l) != null;
    }

    private Cardume dentro(Location l) {
        for (Cardume c : ativos) {
            if (!c.w.equals(l.getWorld())) continue;
            double dx = l.getX() - c.x, dz = l.getZ() - c.z;
            if (dx * dx + dz * dz <= c.raio() * c.raio() && Math.abs(l.getY() - c.y) <= 8) return c;
        }
        return null;
    }

    int quantos() {
        return ativos.size();
    }

    // =====================================================================
    //  Nascer e sumir
    // =====================================================================

    /** A cada segundo: cardumes vencidos somem; de 10 em 10 s, quem navega no mar ganha cardume por perto; a baleia fica no dela. */
    void segundo(int ciclo) {
        long agora = System.currentTimeMillis();
        for (Iterator<Cardume> it = ativos.iterator(); it.hasNext(); ) {
            Cardume c = it.next();
            if (agora < c.fim) continue;
            it.remove();
            desfazer(c);
        }
        if (plugin.settings().marCardumes && ciclo % 10 == 0) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location l = p.getLocation();
                if (!Oceano.noMar(l) || !Oceano.naSuperficie(p)) continue;
                int perto = 0;
                for (Cardume c : ativos) if (c.w.equals(l.getWorld()) && c.centro().distanceSquared(l) <= 140 * 140) perto++;
                if (perto >= 2 || r.nextDouble() > 0.35) continue;
                double a = r.nextDouble() * Math.PI * 2, d = 40 + r.nextDouble() * 60;
                Location alvo = l.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                if (!alvo.getChunk().isLoaded()) continue;
                criar(alvo, Oceano.marFundo(alvo) && r.nextDouble() < 0.22);
            }
        }
        if (ciclo % 2 == 0) for (Cardume c : ativos) baleia(c, ciclo);
    }

    /** Cria um cardume aqui (na superfície da água). Null se não há água funda. */
    Cardume criar(Location onde, boolean grande) {
        World w = onde.getWorld();
        Double sup = Oceano.superficie(w, onde.getBlockX(), onde.getBlockZ(), 4);
        if (sup == null) return null;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long dura = (grande ? 8 : 5) * 60_000L + r.nextLong(3 * 60_000L);
        Cardume c = new Cardume(w, onde.getBlockX() + 0.5, sup, onde.getBlockZ() + 0.5, System.currentTimeMillis() + dura, grande);
        String bioma = w.getBiome(onde.getBlockX(), (int) Math.floor(sup) - 1, onde.getBlockZ()).getKey().getKey();
        boolean quente = bioma.contains("warm");
        int n = grande ? 7 : 4;
        for (int i = 0; i < n; i++) {
            Location l = new Location(w, c.x + r.nextDouble(-3, 3), sup - 1.5 - r.nextDouble(2), c.z + r.nextDouble(-3, 3));
            Class<? extends Fish> tipo = quente ? TropicalFish.class : r.nextBoolean() ? Cod.class : Salmon.class;
            Fish f = w.spawn(l, tipo, x -> {
                x.setPersistent(false);
                x.setRemoveWhenFarAway(true);
                x.getPersistentDataContainer().set(K_PEIXE, PersistentDataType.BYTE, (byte) 1);
            });
            c.peixes.add(f.getUniqueId());
        }
        if (grande && plugin.settings().marBaleias) {
            Location l = new Location(w, c.x, sup - 3, c.z);
            Dolphin b = w.spawn(l, Dolphin.class, x -> {
                x.customName(Component.text("Baleia", NamedTextColor.AQUA));
                x.setPersistent(false);
                x.setRemoveWhenFarAway(true);
                x.setInvulnerable(true);
                x.getPersistentDataContainer().set(K_BALEIA, PersistentDataType.BYTE, (byte) 1);
                AttributeInstance escala = x.getAttribute(Attribute.SCALE);
                if (escala != null) escala.setBaseValue(3.4);
                AttributeInstance vel = x.getAttribute(Attribute.MOVEMENT_SPEED);
                if (vel != null) vel.setBaseValue(0.6);
            });
            c.baleia = b.getUniqueId();
        }
        ativos.add(c);
        return c;
    }

    private static void desfazer(Cardume c) {
        for (UUID u : c.peixes) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) e.remove();
        }
        if (c.baleia != null) {
            Entity e = Bukkit.getEntity(c.baleia);
            if (e != null) {
                e.getWorld().spawnParticle(Particle.SPLASH, e.getLocation(), 40, 1.5, 0.5, 1.5, 0.3);
                e.remove();
            }
        }
    }

    /** A baleia nada em volta do cardume e solta o jato de vez em quando (dá para ver de longe). */
    private static void baleia(Cardume c, int ciclo) {
        if (c.baleia == null) return;
        Entity e = Bukkit.getEntity(c.baleia);
        if (!(e instanceof Dolphin d) || !d.isValid()) return;
        Location l = d.getLocation();
        double dx = c.x - l.getX(), dz = c.z - l.getZ();
        if (dx * dx + dz * dz > 14 * 14) d.setVelocity(new Vector(dx, 0, dz).normalize().multiply(0.6));
        if (ciclo % 4 == 0) {
            Location jato = new Location(c.w, l.getX(), c.y, l.getZ());
            for (int i = 0; i < 6; i++) {
                c.w.spawnParticle(Particle.SPLASH, jato.clone().add(0, i * 0.7, 0), 18, 0.25, 0.2, 0.25, 0.05, null, true);
                c.w.spawnParticle(Particle.CLOUD, jato.clone().add(0, i * 0.7, 0), 2, 0.15, 0.1, 0.15, 0.01, null, true);
            }
            c.w.playSound(jato, Sound.ENTITY_DOLPHIN_SPLASH, 2f, 0.4f);
        }
    }

    /** A cada meio segundo: a água do cardume ferve (dá para ver de longe). */
    void mostrar() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Cardume c : ativos) {
            boolean alguem = false;
            for (Player p : c.w.getPlayers()) {
                if (p.getLocation().distanceSquared(c.centro()) <= 128 * 128) {
                    alguem = true;
                    break;
                }
            }
            if (!alguem) continue;
            double rr = c.raio() * 0.7;
            for (int i = 0; i < (c.grande ? 6 : 4); i++) {
                Location l = new Location(c.w, c.x + r.nextDouble(-rr, rr), c.y + 0.1, c.z + r.nextDouble(-rr, rr));
                c.w.spawnParticle(Particle.SPLASH, l, 10, 0.4, 0.05, 0.4, 0.15, null, true);
                c.w.spawnParticle(Particle.BUBBLE_POP, l, 4, 0.4, 0.05, 0.4, 0.02);
            }
            if (r.nextInt(6) == 0) c.w.playSound(c.centro(), Sound.ENTITY_FISH_SWIM, 1f, 1.2f);
        }
    }

    /** Admin: cardume aqui. */
    String criarAqui(Player p, boolean grande) {
        Cardume c = criar(p.getLocation(), grande);
        return c == null ? "Aqui não tem água funda (4 blocos) para um cardume." : null;
    }

    void limpar() {
        for (Cardume c : ativos) desfazer(c);
        ativos.clear();
    }

    /** Peixe ou baleia de um cardume que ficou para trás (o servidor caiu, o chunk descarregou). */
    static boolean sobra(Entity e) {
        return e.getPersistentDataContainer().has(K_PEIXE) || e.getPersistentDataContainer().has(K_BALEIA);
    }

    boolean conhecido(Entity e) {
        for (Cardume c : ativos) if (c.peixes.contains(e.getUniqueId()) || e.getUniqueId().equals(c.baleia)) return true;
        return false;
    }
}
