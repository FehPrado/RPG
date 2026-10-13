package br.rpgatributos.fazenda;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Os efeitos estranhos dos pratos de monstro (Cozinha): subir paredes, cair sem dano, golpes de
 * fogo, sumir no vazio ao levar um golpe... Cada efeito dura um tempo e some sozinho.
 */
public final class IguariasMonstro implements Listener {

    private static final NamespacedKey K_FIRME = new NamespacedKey("rpgatributos", "prato_firme");

    private final RPGAtributos plugin;
    /** Jogador → efeito → até quando (ms). */
    private final Map<UUID, Map<Prato.Especial, Long>> ativos = new HashMap<>();
    private final Map<UUID, Long> esperaVazio = new HashMap<>();

    public IguariasMonstro(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private boolean tem(Player p, Prato.Especial e) {
        Map<Prato.Especial, Long> m = ativos.get(p.getUniqueId());
        Long ate = m == null ? null : m.get(e);
        return ate != null && ate > System.currentTimeMillis();
    }

    /** Chamado pela Cozinha quando o jogador come um prato de monstro. */
    public void aplicar(Player p, Prato.Especial e, double fator) {
        if (e == Prato.Especial.SEM_SONO) {
            p.setStatistic(Statistic.TIME_SINCE_REST, 0);
            p.sendActionBar(Component.text("☾ Os phantoms esquecem você por um tempo.", NamedTextColor.DARK_AQUA));
            return;
        }
        long ate = System.currentTimeMillis() + Math.round(e.segundos() * 1000 * fator);
        ativos.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Prato.Especial.class)).put(e, ate);
        if (e == Prato.Especial.ESTOMAGO_DE_FERRO) {
            p.removePotionEffect(PotionEffectType.HUNGER);
            p.removePotionEffect(PotionEffectType.POISON);
        }
        if (e == Prato.Especial.FIRME) {
            AttributeInstance a = p.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (a != null && a.getModifier(K_FIRME) == null) {
                a.addModifier(new AttributeModifier(K_FIRME, 0.7, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
        }
        p.sendActionBar(Component.text("✧ " + e.descricao(), NamedTextColor.DARK_GREEN));
    }

    // =====================================================================
    //  A cada tick: subir paredes e o fim dos efeitos
    // =====================================================================

    public void tick() {
        if (ativos.isEmpty()) return;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Map<Prato.Especial, Long>>> it = ativos.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<Prato.Especial, Long>> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            Map<Prato.Especial, Long> m = en.getValue();
            if (p == null) continue;
            for (Iterator<Map.Entry<Prato.Especial, Long>> i2 = m.entrySet().iterator(); i2.hasNext(); ) {
                Map.Entry<Prato.Especial, Long> ef = i2.next();
                if (ef.getValue() > agora) continue;
                i2.remove();
                acabou(p, ef.getKey());
            }
            if (m.isEmpty()) {
                it.remove();
                continue;
            }
            if (m.containsKey(Prato.Especial.ESCALAR)) escalar(p);
        }
    }

    private void acabou(Player p, Prato.Especial e) {
        if (e == Prato.Especial.FIRME) {
            AttributeInstance a = p.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (a != null) a.removeModifier(K_FIRME);
        }
        p.sendActionBar(Component.text("O efeito do prato passou.", NamedTextColor.GRAY));
    }

    /** Andando para a frente contra uma parede, sobe como aranha. */
    private static void escalar(Player p) {
        if (p.isFlying() || p.isGliding() || p.isInsideVehicle() || p.isSneaking() || !p.getCurrentInput().isForward()) return;
        Vector frente = p.getLocation().getDirection().setY(0);
        if (frente.lengthSquared() < 0.01) return;
        frente.normalize().multiply(0.6);
        Location pe = p.getLocation().add(frente);
        Block baixo = pe.getBlock(), alto = pe.clone().add(0, 1, 0).getBlock();
        if (!baixo.isSolid() && !alto.isSolid()) return;
        Vector v = p.getVelocity();
        p.setVelocity(new Vector(v.getX(), 0.22, v.getZ()));
        p.setFallDistance(0);
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p
                && (tem(p, Prato.Especial.AMORTECER) || tem(p, Prato.Especial.ESCALAR))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        // Golpes de fogo (só corpo a corpo).
        if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity alvo && tem(p, Prato.Especial.PUNHOS_DE_FOGO)) {
            alvo.setFireTicks(Math.max(alvo.getFireTicks(), 80));
        }
        // Passo do vazio: às vezes some e reaparece atrás de quem bateu.
        if (!(e.getEntity() instanceof Player p) || !tem(p, Prato.Especial.PASSO_DO_VAZIO)) return;
        Entity atacante = e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : e.getDamager();
        long agora = System.currentTimeMillis();
        if (agora < esperaVazio.getOrDefault(p.getUniqueId(), 0L) || ThreadLocalRandom.current().nextDouble() >= 0.3) return;
        if (!atacante.getWorld().equals(p.getWorld()) || atacante.getLocation().distanceSquared(p.getLocation()) > 16 * 16) return;
        Vector costas = atacante.getLocation().getDirection().setY(0);
        if (costas.lengthSquared() < 0.01) return;
        Location destino = atacante.getLocation().subtract(costas.normalize().multiply(1.6));
        if (!destino.getBlock().isPassable() || !destino.clone().add(0, 1, 0).getBlock().isPassable()) return;
        esperaVazio.put(p.getUniqueId(), agora + 3000);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
        destino.setDirection(atacante.getLocation().toVector().subtract(destino.toVector()));
        p.teleport(destino);
        p.getWorld().spawnParticle(Particle.PORTAL, destino.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
        p.getWorld().playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEfeito(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getNewEffect() == null || !tem(p, Prato.Especial.ESTOMAGO_DE_FERRO)) return;
        PotionEffectType t = e.getNewEffect().getType();
        if (t.equals(PotionEffectType.HUNGER) || t.equals(PotionEffectType.POISON)) e.setCancelled(true);
    }

    /** Se o servidor caiu no meio do efeito, o "Ossos de pedra" não fica para sempre. */
    @EventHandler
    public void aoEntrar(org.bukkit.event.player.PlayerJoinEvent e) {
        AttributeInstance a = e.getPlayer().getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (a != null) a.removeModifier(K_FIRME);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Map<Prato.Especial, Long> m = ativos.remove(e.getPlayer().getUniqueId());
        if (m != null && m.containsKey(Prato.Especial.FIRME)) acabou(e.getPlayer(), Prato.Especial.FIRME);
        esperaVazio.remove(e.getPlayer().getUniqueId());
    }
}
