package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static br.rpgatributos.arcano.Essencia.*;

/**
 * Infusões lendárias: núcleos de chefe infundidos no corpo. Além de essências
 * fortes, cada núcleo dá um poder único que nenhum item comum dá.
 */
public final class Lendarias implements Listener {

    public static final int CARGA = 4;
    public static final int PODER = 6;
    public static final int CUSTO_XP = 20;

    private final RPGAtributos plugin;
    private final NamespacedKey kArmadura, kRepulsao, kVida, kDano;
    /** Quem pode usar o pulo duplo agora (já tocou o chão desde o último). */
    private final Set<UUID> puloPronto = new HashSet<>();

    public Lendarias(RPGAtributos plugin) {
        this.plugin = plugin;
        kArmadura = new NamespacedKey(plugin, "lendaria_armadura");
        kRepulsao = new NamespacedKey(plugin, "lendaria_repulsao");
        kVida = new NamespacedKey(plugin, "lendaria_vida");
        kDano = new NamespacedKey(plugin, "lendaria_dano");
    }

    // =====================================================================
    //  Dados de cada núcleo
    // =====================================================================

    public static Map<Essencia, Integer> essencias(Raro r) {
        Map<Essencia, Integer> m = new EnumMap<>(Essencia.class);
        switch (r) {
            case NUCLEO_GOLEM -> { m.put(TERRA, 5); m.put(VIDA, 2); }
            case NUCLEO_RAINHA -> { m.put(NATUREZA, 4); m.put(SOMBRA, 3); }
            case NUCLEO_CHAMAS -> { m.put(FOGO, 6); m.put(ENERGIA, 1); }
            case NUCLEO_LICH -> { m.put(SOMBRA, 5); m.put(VAZIO, 2); }
            case NUCLEO_TEMPESTADE -> { m.put(VENTO, 5); m.put(ENERGIA, 3); }
            case NUCLEO_ARAUTO -> { m.put(VAZIO, 5); m.put(SOMBRA, 3); m.put(ENERGIA, 2); }
            default -> { }
        }
        return m;
    }

    public static String poder(Raro r) {
        return switch (r) {
            case NUCLEO_GOLEM -> "Pele de Ferro: +4 de armadura e resistência a empurrões";
            case NUCLEO_RAINHA -> "Presas: imune a veneno e envenena quem te ataca";
            case NUCLEO_CHAMAS -> "Brasa Eterna: imune a fogo e incendeia quem te ataca";
            case NUCLEO_LICH -> "Não-Morto: imune a Definhamento e rouba 8% do dano que causa";
            case NUCLEO_TEMPESTADE -> "Pulo Duplo: pule de novo no ar (aperte pular duas vezes)";
            case NUCLEO_ARAUTO -> "Coroa do Fim: +15% de dano e +4 corações";
            default -> "";
        };
    }

    private boolean tem(Player p, Raro r) {
        return plugin.arcano().perfil(p).temNucleo(r);
    }

    // =====================================================================
    //  Atributos (Pele de Ferro e Coroa do Fim)
    // =====================================================================

    public void aplicar(Player p) {
        Perfil pf = plugin.arcano().perfil(p);
        boolean golem = pf.temNucleo(Raro.NUCLEO_GOLEM);
        boolean arauto = pf.temNucleo(Raro.NUCLEO_ARAUTO);
        definir(p, Attribute.ARMOR, kArmadura, golem ? 4 : 0, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.KNOCKBACK_RESISTANCE, kRepulsao, golem ? 0.5 : 0, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MAX_HEALTH, kVida, arauto ? 8 : 0, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.ATTACK_DAMAGE, kDano, arauto ? 0.15 : 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (!pf.temNucleo(Raro.NUCLEO_TEMPESTADE) && p.getGameMode() == GameMode.SURVIVAL && p.getAllowFlight()
                && puloPronto.remove(p.getUniqueId())) {
            p.setAllowFlight(false);
        }
    }

    private static void definir(Player p, Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op) {
        AttributeInstance i = p.getAttribute(a);
        if (i == null) return;
        i.removeModifier(k);
        if (v != 0) i.addModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
        if (a == Attribute.MAX_HEALTH && p.getHealth() > i.getValue()) p.setHealth(i.getValue());
    }

    /** Efeitos passivos (usado pelo sistema de passivos): Brasa Eterna = resistência ao fogo. */
    public Map<PotionEffectType, Integer> efeitos(Player p) {
        return tem(p, Raro.NUCLEO_CHAMAS) ? Map.of(PotionEffectType.FIRE_RESISTANCE, 0) : Map.of();
    }

    // =====================================================================
    //  Pulo Duplo (Olho da Tempestade)
    // =====================================================================

    /** Roda a cada 4 ticks: libera o pulo duplo para quem está no chão. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (!tem(p, Raro.NUCLEO_TEMPESTADE)) continue;
            boolean noChao = p.getLocation().subtract(0, 0.1, 0).getBlock().isSolid();
            if (noChao && !p.isFlying()) {
                puloPronto.add(p.getUniqueId());
                if (!p.getAllowFlight()) p.setAllowFlight(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTentarVoar(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!e.isFlying() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!tem(p, Raro.NUCLEO_TEMPESTADE)) return;
        e.setCancelled(true);
        p.setAllowFlight(false);
        if (!puloPronto.remove(p.getUniqueId())) return;
        Vector v = p.getLocation().getDirection().setY(0);
        if (v.lengthSquared() > 1e-4) v.normalize().multiply(0.45);
        p.setVelocity(v.setY(0.85));
        p.setFallDistance(0);
        p.getWorld().spawnParticle(Particle.GUST, p.getLocation(), 1);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.6f, 1.4f);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        puloPronto.remove(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Combate
    // =====================================================================

    /** Imunidades: veneno (Rainha) e Definhamento (Lich). */
    @EventHandler(ignoreCancelled = true)
    public void aoReceberEfeito(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getNewEffect() == null) return;
        PotionEffectType t = e.getNewEffect().getType();
        if ((t == PotionEffectType.POISON && tem(p, Raro.NUCLEO_RAINHA))
                || (t == PotionEffectType.WITHER && tem(p, Raro.NUCLEO_LICH))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDano(EntityDamageByEntityEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.THORNS) return;
        // Quem apanha: Presas e Brasa Eterna revidam em quem atacou corpo a corpo.
        if (e.getEntity() instanceof Player defensor && e.getDamager() instanceof LivingEntity atacante && atacante != defensor) {
            if (tem(defensor, Raro.NUCLEO_RAINHA) && ThreadLocalRandom.current().nextDouble() < 0.25) {
                atacante.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                atacante.getWorld().spawnParticle(Particle.ITEM_SLIME, atacante.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3);
            }
            if (tem(defensor, Raro.NUCLEO_CHAMAS)) atacante.setFireTicks(Math.max(atacante.getFireTicks(), 60));
        }
        // Quem ataca: Não-Morto rouba vida.
        if (e.getDamager() instanceof Player atacante && e.getEntity() instanceof LivingEntity && tem(atacante, Raro.NUCLEO_LICH)) {
            AttributeInstance max = atacante.getAttribute(Attribute.MAX_HEALTH);
            if (max != null && !atacante.isDead()) {
                atacante.setHealth(Math.min(max.getValue(), atacante.getHealth() + e.getFinalDamage() * 0.08));
            }
        }
    }

    /** Mensagem bonita ao infundir um núcleo. */
    public static Component anuncio(Raro r) {
        return Component.text("✦ Poder lendário: ", NamedTextColor.GOLD).append(Component.text(poder(r), r.cor()));
    }
}
