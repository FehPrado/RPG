package br.rpgatributos.combate;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.combo.TipoArma;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fraquezas (2.54): cada tipo de criatura apanha mais (ou menos) de certas armas, e o clima mexe
 * no combate. A barra avisa quando você acerta um ponto fraco; o Bestiário mostra as fraquezas.
 */
public final class Fraquezas implements Listener {

    /** Uma regra: a família de criaturas, as armas que fazem mais e as que fazem menos. */
    public record Regra(String familia, Set<TipoArma> fortes, double bonus, Set<TipoArma> fracas, double penalidade) { }

    private final RPGAtributos plugin;
    private final Map<UUID, Long> avisado = new HashMap<>();

    public Fraquezas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** As regras de cada família (null = sem fraqueza conhecida). */
    public static Regra regra(Entity e) {
        return regra(e.getClass());
    }

    public static Regra regra(Class<?> c) {
        if (AbstractSkeleton.class.isAssignableFrom(c)) return new Regra("Esqueletos", Set.of(TipoArma.MACA, TipoArma.CLAVA, TipoArma.MANGUAL, TipoArma.BASTAO), 0.30,
                Set.of(TipoArma.ARCO, TipoArma.ARREMESSO), 0.25);
        if (Spider.class.isAssignableFrom(c)) return new Regra("Aranhas", Set.of(TipoArma.LANCA, TipoArma.ARPAO, TipoArma.TRIDENTE, TipoArma.FOICE), 0.25, Set.of(), 0);
        if (MagmaCube.class.isAssignableFrom(c) || Slime.class.isAssignableFrom(c)) return new Regra("Slimes", Set.of(TipoArma.MACA, TipoArma.CLAVA, TipoArma.MONTANTE), 0.25,
                Set.of(TipoArma.ADAGA, TipoArma.DUPLAS, TipoArma.GARRAS), 0.30);
        if (Creeper.class.isAssignableFrom(c)) return new Regra("Creepers", Set.of(TipoArma.ARCO, TipoArma.ARREMESSO, TipoArma.BUMERANGUE, TipoArma.CHICOTE), 0.25, Set.of(), 0);
        if (Enderman.class.isAssignableFrom(c)) return new Regra("Endermen", Set.of(TipoArma.LEQUES, TipoArma.CHICOTE, TipoArma.ESPADA), 0.20, Set.of(), 0);
        if (Blaze.class.isAssignableFrom(c) || Ghast.class.isAssignableFrom(c)) return new Regra("Criaturas de fogo", Set.of(TipoArma.TRIDENTE, TipoArma.ARPAO, TipoArma.MANGUAL), 0.40,
                Set.of(TipoArma.CLAVA), 0.30);
        if (Phantom.class.isAssignableFrom(c) || Vex.class.isAssignableFrom(c)) return new Regra("Voadores", Set.of(TipoArma.ARCO, TipoArma.BUMERANGUE, TipoArma.CHICOTE, TipoArma.LEQUES), 0.25, Set.of(), 0);
        if (Guardian.class.isAssignableFrom(c)) return new Regra("Guardiões", Set.of(TipoArma.TRIDENTE, TipoArma.ARPAO), 0.30, Set.of(), 0);
        if (IronGolem.class.isAssignableFrom(c) || Ravager.class.isAssignableFrom(c)) return new Regra("Brutamontes", Set.of(TipoArma.MACHADO, TipoArma.MONTANTE, TipoArma.MACA), 0.20,
                Set.of(TipoArma.ADAGA, TipoArma.CHICOTE), 0.25);
        if (Witch.class.isAssignableFrom(c) || Spellcaster.class.isAssignableFrom(c)) return new Regra("Feiticeiros", Set.of(TipoArma.ADAGA, TipoArma.DUPLAS, TipoArma.ARREMESSO, TipoArma.GARRAS), 0.25, Set.of(), 0);
        if (Zombie.class.isAssignableFrom(c)) return new Regra("Mortos-vivos", Set.of(TipoArma.ESPADA, TipoArma.FOICE, TipoArma.MONTANTE), 0.15, Set.of(), 0);
        return null;
    }

    /** Linhas para o Bestiário ("Fraco contra: maça, clava..."). */
    public static List<Component> linhas(org.bukkit.entity.EntityType tipo) {
        List<Component> l = new ArrayList<>();
        if (tipo.getEntityClass() == null) return l;
        Regra r = regra(tipo.getEntityClass());
        if (r == null) return l;
        l.add(Component.text("Fraco contra: " + nomes(r.fortes()) + " (+" + Math.round(r.bonus() * 100) + "%)", NamedTextColor.GREEN));
        if (!r.fracas().isEmpty()) l.add(Component.text("Resiste a: " + nomes(r.fracas()) + " (-" + Math.round(r.penalidade() * 100) + "%)", NamedTextColor.RED));
        return l;
    }

    private static String nomes(Set<TipoArma> s) {
        List<String> l = new ArrayList<>();
        for (TipoArma t : TipoArma.values()) if (s.contains(t)) l.add(t.nome().toLowerCase(java.util.Locale.ROOT));
        return String.join(", ", l);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity alvo) || alvo instanceof Player) return;
        Player p;
        TipoArma t;
        if (e.getDamager() instanceof Player pl) {
            p = pl;
            t = TipoArma.de(pl);
        } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl) {
            p = pl;
            t = pr instanceof AbstractArrow && !(pr instanceof org.bukkit.entity.Trident) ? TipoArma.ARCO : TipoArma.de(pl);
        } else {
            return;
        }
        double mult = 1;
        String aviso = null;
        Regra r = regra(alvo);
        if (r != null && t != null) {
            if (r.fortes().contains(t)) {
                mult *= 1 + r.bonus();
                aviso = "⚑ Ponto fraco: " + t.nome().toLowerCase(java.util.Locale.ROOT) + " contra " + r.familia().toLowerCase(java.util.Locale.ROOT)
                        + " (+" + Math.round(r.bonus() * 100) + "%)";
            } else if (r.fracas().contains(t)) {
                mult *= 1 - r.penalidade();
                aviso = "⛨ " + r.familia() + " resistem a " + t.nome().toLowerCase(java.util.Locale.ROOT) + " (-" + Math.round(r.penalidade() * 100) + "%)";
            }
        }
        // O clima.
        World w = p.getWorld();
        boolean chuva = w.hasStorm() && p.isInRain();
        if (t != null && chuva) {
            if (t == TipoArma.TRIDENTE || t == TipoArma.ARPAO) mult *= w.isThundering() ? 1.25 : 1.15;
            if (t == TipoArma.CLAVA) mult *= 0.8; // a chuva apaga as brasas
            if (t == TipoArma.ARCO) mult *= 0.9; // corda molhada
        }
        if (alvo.getFireTicks() > 0 && chuva) alvo.setFireTicks(Math.min(alvo.getFireTicks(), 20));
        if (t == TipoArma.MANGUAL && alvo.getLocation().getBlock().getTemperature() < 0.15) mult *= 1.15; // gelo no frio
        // De noite, os mortos-vivos aguentam um pouco mais.
        if (alvo instanceof Zombie || alvo instanceof AbstractSkeleton) {
            long hora = w.getTime();
            if (hora >= 13000 && hora <= 23000) mult *= 0.9;
        }
        if (mult == 1) return;
        e.setDamage(e.getDamage() * mult);
        if (aviso != null) {
            long agora = System.currentTimeMillis();
            if (agora - avisado.getOrDefault(p.getUniqueId(), 0L) > 4000) {
                avisado.put(p.getUniqueId(), agora);
                p.sendActionBar(Component.text(aviso, mult > 1 ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
        }
    }
}
