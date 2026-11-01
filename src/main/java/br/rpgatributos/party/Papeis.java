package br.rpgatributos.party;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.classe.Classe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Papéis na party (3.18): a classe dá um papel quando há outro membro da party por perto (30 blocos).
 * <ul>
 *   <li><b>Protetor</b> (Tanque, Cavaleiro, Paladino, Senhor da Guerra): monstros que miram um aliado
 *       perto dele passam a mirar nele, e ele leva 10% menos dano.</li>
 *   <li><b>Curandeiro</b> (Curandeiro, Santo Paladino): cada golpe em monstro (a cada 3 s) cura um coração
 *       no aliado mais ferido por perto.</li>
 *   <li><b>Marcador</b> (Arqueiro, Atirador de Elite, Caçador, Arqueiro Celestial): a flecha marca o alvo
 *       por 8 s (brilha) e a party inteira faz +15% de dano nele.</li>
 * </ul>
 */
public final class Papeis implements Listener {

    public enum Papel {
        PROTETOR("🛡 Protetor", 0x8D6E63), CURANDEIRO("✙ Curandeiro", 0x66BB6A), MARCADOR("➹ Marcador", 0x7CB342);

        final String nome;
        final TextColor cor;

        Papel(String nome, int cor) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
        }

        public String nome() { return nome; }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Long> curouEm = new HashMap<>();
    /** Alvo marcado -> até quando, e de qual party. */
    private final Map<UUID, Object[]> marcados = new HashMap<>();
    private int ciclo;

    public Papeis(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static Papel papelDa(Classe c) {
        if (c == null) return null;
        return switch (c.name()) {
            case "TANQUE", "CAVALEIRO", "PALADINO", "SENHOR_DA_GUERRA" -> Papel.PROTETOR;
            case "CURANDEIRO", "SANTO_PALADINO" -> Papel.CURANDEIRO;
            case "ARQUEIRO", "ATIRADOR", "CACADOR", "ARQUEIRO_CELESTIAL" -> Papel.MARCADOR;
            default -> null;
        };
    }

    /** O papel que vale agora (precisa de outro membro da party a até 30 blocos). */
    public Papel papel(Player p) {
        if (!plugin.getConfig().getBoolean("party.papeis", true)) return null;
        Party pt = plugin.parties().party(p);
        if (pt == null) return null;
        Papel papel = papelDa(plugin.classes().ativa(p));
        if (papel == null) return null;
        for (Player o : pt.online()) {
            if (o != p && o.getWorld().equals(p.getWorld()) && o.getLocation().distanceSquared(p.getLocation()) < 30 * 30) return papel;
        }
        return null;
    }

    /** A cada segundo: o protetor puxa os monstros dos aliados. */
    public void segundo() {
        ciclo++;
        long agora = System.currentTimeMillis();
        marcados.values().removeIf(v -> (long) v[0] < agora);
        if (ciclo % 2 != 0) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE || papel(p) != Papel.PROTETOR) continue;
            Party pt = plugin.parties().party(p);
            for (Entity e : p.getNearbyEntities(8, 4, 8)) {
                if (!(e instanceof Mob m) || !(e instanceof Enemy) || !(m.getTarget() instanceof Player alvo) || alvo == p) continue;
                if (!pt.tem(alvo.getUniqueId()) || r.nextDouble() > 0.6) continue;
                m.setTarget(p);
                e.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, e.getLocation().add(0, e.getHeight() + 0.3, 0), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e instanceof EntityDamageByEntityEvent && papel(p) == Papel.PROTETOR) e.setDamage(e.getDamage() * 0.9);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity alvo) || !(alvo instanceof Enemy)) return;
        Player p = e.getDamager() instanceof Player pl ? pl
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null) return;
        long agora = System.currentTimeMillis();
        Party pt = plugin.parties().party(p);
        // Alvo marcado pela party: +15%.
        Object[] marca = marcados.get(alvo.getUniqueId());
        if (marca != null && pt != null && pt.id().equals(marca[1]) && (long) marca[0] > agora) e.setDamage(e.getDamage() * 1.15);
        Papel papel = papel(p);
        if (papel == Papel.MARCADOR && e.getDamager() instanceof Projectile) {
            marcados.put(alvo.getUniqueId(), new Object[]{agora + 8000, pt.id()});
            alvo.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0, false, false, false));
            p.sendActionBar(Component.text("➹ Alvo marcado: a party faz +15% de dano nele", Papel.MARCADOR.cor));
        }
        if (papel == Papel.CURANDEIRO && agora - curouEm.getOrDefault(p.getUniqueId(), 0L) > 3000) {
            Player ferido = null;
            double pior = 1.01;
            for (Player o : pt.online()) {
                if (!o.getWorld().equals(p.getWorld()) || o.getLocation().distanceSquared(p.getLocation()) > 12 * 12 || o.isDead()) continue;
                var max = o.getAttribute(Attribute.MAX_HEALTH);
                if (max == null) continue;
                double frac = o.getHealth() / max.getValue();
                if (frac < pior) { pior = frac; ferido = o; }
            }
            if (ferido != null && pior < 1) {
                curouEm.put(p.getUniqueId(), agora);
                var max = ferido.getAttribute(Attribute.MAX_HEALTH);
                ferido.setHealth(Math.min(max.getValue(), ferido.getHealth() + 2));
                ferido.getWorld().spawnParticle(Particle.HEART, ferido.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3);
                plugin.titulos().registrar(p, "curas_party", 1);
            }
        }
    }
}
