package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.combo.TipoArma;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Combate montado, parte 2 (2.54). Junto com os golpes da tecla F e o atropelo do
 * {@link CombateMontado}:
 * <ul>
 *   <li><b>Carga:</b> golpe corpo a corpo montado cresce com a velocidade da montaria (até +50%;
 *       lança, arpão, tridente e montante ganham +20% a mais correndo).</li>
 *   <li><b>Arco montado:</b> flechas atiradas de cima da montaria fazem +15%.</li>
 *   <li><b>Escudo na montaria:</b> com o escudo levantado, a montaria leva metade do dano.</li>
 *   <li>Lutar montado dá um pouco de XP de Doma.</li>
 * </ul>
 */
public final class CargaMontada implements Listener {

    private static final Set<TipoArma> DE_CARGA = Set.of(TipoArma.LANCA, TipoArma.ARPAO, TipoArma.TRIDENTE, TipoArma.MONTANTE);

    private final RPGAtributos plugin;
    private final Map<UUID, Long> avisado = new HashMap<>(), xpEm = new HashMap<>();

    public CargaMontada(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static double velocidade(Entity montaria) {
        Vector v = montaria.getVelocity();
        return Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAtacarMontado(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity alvo)) return;
        Player p;
        boolean flecha = false;
        if (e.getDamager() instanceof Player pl && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) p = pl;
        else if (e.getDamager() instanceof AbstractArrow a && a.getShooter() instanceof Player pl) {
            p = pl;
            flecha = true;
        } else return;
        if (!(p.getVehicle() instanceof LivingEntity montaria) || montaria.equals(alvo)) return;
        double mult;
        String texto;
        if (flecha) {
            mult = 1.15;
            texto = "🏹 Arco montado +15%";
        } else {
            double v = velocidade(montaria);
            double bonus = Math.min(0.5, v * 1.6);
            TipoArma t = TipoArma.de(p);
            if (t != null && DE_CARGA.contains(t) && v > 0.15) bonus += 0.2;
            if (bonus < 0.05) return;
            mult = 1 + bonus;
            texto = "⚡ Carga! +" + Math.round(bonus * 100) + "%";
            if (bonus >= 0.3) {
                alvo.getWorld().spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
                Vector frente = montaria.getLocation().getDirection().setY(0);
                if (frente.lengthSquared() > 1e-4) alvo.setVelocity(alvo.getVelocity().add(frente.normalize().multiply(0.6 * bonus * 2).setY(0.25)));
            }
        }
        e.setDamage(e.getDamage() * mult);
        long agora = System.currentTimeMillis();
        if (agora - avisado.getOrDefault(p.getUniqueId(), 0L) > 2500) {
            avisado.put(p.getUniqueId(), agora);
            p.sendActionBar(Component.text(texto, NamedTextColor.GOLD));
        }
        if (agora - xpEm.getOrDefault(p.getUniqueId(), 0L) > 500 && alvo instanceof org.bukkit.entity.Enemy) {
            xpEm.put(p.getUniqueId(), agora);
            plugin.stats().darXp(p, Skill.DOMA, 0.6);
        }
    }

    /** Com o escudo levantado, a montaria leva metade do dano. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerirMontaria(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity montaria)) return;
        for (Entity passageiro : montaria.getPassengers()) {
            if (!(passageiro instanceof Player p) || !p.isBlocking()) continue;
            ItemStack ativo = p.getActiveItem();
            if (ativo == null || ativo.getType() != Material.SHIELD) continue;
            e.setDamage(e.getDamage() * 0.5);
            montaria.getWorld().playSound(montaria.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 1.2f);
            return;
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        avisado.remove(e.getPlayer().getUniqueId());
        xpEm.remove(e.getPlayer().getUniqueId());
    }
}
