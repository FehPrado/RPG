package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.masmorra.ArvoreCacador.No;
import br.rpgatributos.masmorra.Despertar.Caracteristica;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

/**
 * Os poderes do caçador nos portais (2.72): os nós da Árvore de Caçador e as características do
 * Despertar. Fora dos portais, nada disto vale.
 */
public final class PoderCacador implements Listener {

    private static final NamespacedKey K_MOB = new NamespacedKey("rpgatributos", "masmorra_mob");
    private static final NamespacedKey K_VIGOR = new NamespacedKey("rpgatributos", "vigor_portal");
    private static final Set<EntityPotionEffectEvent.Cause> DE_MONSTRO = Set.of(EntityPotionEffectEvent.Cause.ATTACK,
            EntityPotionEffectEvent.Cause.ARROW, EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD, EntityPotionEffectEvent.Cause.POTION_SPLASH);
    private static final Set<PotionEffectType> RUINS = Set.of(PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.POISON);

    private final RPGAtributos plugin;
    private int ciclo;

    public PoderCacador(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Um monstro de portal (de masmorra) ou o chefe dela. */
    static boolean monstroDoPortal(Masmorras mas, Entity e) {
        if (e == null || !mas.ehMundo(e.getWorld())) return false;
        return e.getPersistentDataContainer().has(K_MOB) || Chefes.ehChefe(e);
    }

    // =====================================================================
    //  Ganchos usados pela masmorra
    // =====================================================================

    /** Multiplicador da chance de Cristal de Mana dos monstros que ele abate. */
    static double multMana(Player p) {
        if (p == null) return 1;
        return 1 + 0.15 * ArvoreCacador.grau(p, No.OLHO_MANA) + (Despertar.tem(p, Caracteristica.MANA_FARTA) ? 0.2 : 0);
    }

    /** Sorteios a mais no tesouro final: o melhor da party (Saqueador + Rei dos Baús). */
    static int sorteiosExtra(Collection<Player> party) {
        int melhor = 0;
        for (Player p : party) {
            melhor = Math.max(melhor, ArvoreCacador.grau(p, No.SAQUEADOR) + (Despertar.tem(p, Caracteristica.REI_DOS_BAUS) ? 1 : 0));
        }
        return melhor;
    }

    static boolean passoLeve(Player p) {
        return ArvoreCacador.grau(p, No.PASSO_LEVE) > 0;
    }

    /** XP de caçador a mais (Caçador Nato). */
    static double multXp(Player p) {
        return Despertar.tem(p, Caracteristica.CACADOR_NATO) ? 1.15 : 1;
    }

    /** Chance a mais de subir a raridade ao revelar um item (Sorte do Avaliador). */
    static double sorteRevelar(Player p) {
        return p == null ? 0 : 0.05 * ArvoreCacador.grau(p, No.SORTE);
    }

    // =====================================================================
    //  Combate
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Masmorras mas = plugin.masmorras();
        if (mas == null || !mas.ehMundo(e.getEntity().getWorld())) return;
        Entity d = e.getDamager();
        Player atacante = d instanceof Player p ? p : d instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null;
        if (atacante != null && e.getEntity() instanceof LivingEntity alvo && monstroDoPortal(mas, alvo)) {
            double mult = 1 + 0.04 * ArvoreCacador.grau(atacante, No.GOLPE);
            var maxAlvo = alvo.getAttribute(Attribute.MAX_HEALTH);
            double vidaMax = maxAlvo != null ? maxAlvo.getValue() : alvo.getHealth();
            if (alvo.getHealth() < vidaMax * 0.35) mult += 0.10 * ArvoreCacador.grau(atacante, No.EXECUCAO);
            if (Chefes.ehChefe(alvo)) mult += 0.06 * ArvoreCacador.grau(atacante, No.CACA_CHEFE);
            var maxAt = atacante.getAttribute(Attribute.MAX_HEALTH);
            if (Despertar.tem(atacante, Caracteristica.SANGUE_FRIO) && maxAt != null && atacante.getHealth() < maxAt.getValue() / 2) mult += 0.12;
            if (Despertar.tem(atacante, Caracteristica.ASSASSINO) && alvo.getHealth() >= vidaMax - 0.01) mult += 0.5;
            if (mult != 1) e.setDamage(e.getDamage() * mult);
            if (Despertar.tem(atacante, Caracteristica.VAMPIRICO) && maxAt != null && !atacante.isDead()) {
                atacante.setHealth(Math.min(maxAt.getValue(), atacante.getHealth() + e.getDamage() * 0.04));
            }
        }
        if (e.getEntity() instanceof Player vitima) {
            Entity origem = d instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : d;
            if (monstroDoPortal(mas, origem)) {
                double red = 0.04 * ArvoreCacador.grau(vitima, No.PELE) + (Despertar.tem(vitima, Caracteristica.PELE_DE_ACO) ? 0.08 : 0);
                if (red > 0) e.setDamage(e.getDamage() * Math.max(0.5, 1 - red));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || !monstroDoPortal(plugin.masmorras(), e.getEntity())) return;
        int rec = ArvoreCacador.grau(p, No.RECUPERACAO);
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        if (rec > 0 && max != null && !p.isDead()) p.setHealth(Math.min(max.getValue(), p.getHealth() + rec));
        if (ArvoreCacador.grau(p, No.FRENESI) > 0) p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 0, true, true, true));
    }

    /** Segunda Chance: uma vez por portal, o golpe fatal deixa com 3 corações. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoDano(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.VOID || c == EntityDamageEvent.DamageCause.KILL || c == EntityDamageEvent.DamageCause.SUICIDE) return;
        if (p.getHealth() - e.getFinalDamage() > 0 || !Despertar.tem(p, Caracteristica.SEGUNDA_CHANCE)) return;
        if (p.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || p.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return; // o totem vem primeiro
        Masmorra m = plugin.masmorras().de(p);
        if (m == null || !m.segundaChance.add(p.getUniqueId())) return;
        e.setCancelled(true);
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(Math.min(max != null ? max.getValue() : 20, 6));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 1));
        p.showTitle(Title.title(Component.text("SEGUNDA CHANCE", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("[Sistema] Só uma vez neste portal.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.3f);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.3);
    }

    /** Inabalável: lentidão, fraqueza e veneno dos monstros não pegam. */
    @EventHandler(ignoreCancelled = true)
    public void aoEfeito(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.getNewEffect() == null || !DE_MONSTRO.contains(e.getCause())) return;
        if (!RUINS.contains(e.getNewEffect().getType()) || !plugin.masmorras().ehMundo(p.getWorld())) return;
        if (Despertar.tem(p, Caracteristica.INABALAVEL)) e.setCancelled(true);
    }

    // =====================================================================
    //  A cada segundo: vida, visão, velocidade e o que brilha
    // =====================================================================

    public void tick() {
        ciclo++;
        Masmorras mas = plugin.masmorras();
        if (mas == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Masmorra m = mas.ehMundo(p.getWorld()) ? mas.de(p) : null;
            vigor(p, m != null ? 2 * ArvoreCacador.grau(p, No.VIGOR) : 0);
            efeito(p, PotionEffectType.NIGHT_VISION, m != null && Despertar.tem(p, Caracteristica.OLHOS_DE_GATO));
            efeito(p, PotionEffectType.SPEED, m != null && Despertar.tem(p, Caracteristica.PES_LIGEIROS));
            if (m == null) continue;
            if (ArvoreCacador.grau(p, No.FARO) > 0) {
                brilhar(p, m.segredos.keySet(), Color.fromRGB(0xFFD54F), 14, true);
                brilhar(p, m.mimicos, Color.fromRGB(0xAB47BC), 6, false);
            }
            if (Despertar.tem(p, Caracteristica.INSTINTO)) brilhar(p, m.armadilhas.keySet(), Color.fromRGB(0xE53935), 10, false);
        }
    }

    private static void vigor(Player p, double quanto) {
        AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(K_VIGOR);
        if (atual != null && atual.getAmount() == quanto) return;
        if (atual != null) inst.removeModifier(K_VIGOR);
        if (quanto > 0) inst.addTransientModifier(new AttributeModifier(K_VIGOR, quanto, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        else if (p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    /** Mantém (ou tira) um efeito "de fundo" nosso, sem mexer numa poção que o jogador bebeu. */
    private static void efeito(Player p, PotionEffectType tipo, boolean quer) {
        PotionEffect atual = p.getPotionEffect(tipo);
        boolean nosso = atual != null && atual.isAmbient() && !atual.hasParticles() && atual.getAmplifier() == 0 && atual.getDuration() <= 400;
        if (quer) {
            if (atual == null || (nosso && atual.getDuration() < 300)) p.addPotionEffect(new PotionEffect(tipo, 400, 0, true, false, true));
        } else if (nosso) {
            p.removePotionEffect(tipo);
        }
    }

    /** Partículas só para ele nos pontos perto (até 10 blocos), no máximo {@code limite}. */
    private static void brilhar(Player p, Collection<Location> pontos, Color cor, int limite, boolean dentroDoBloco) {
        Location eu = p.getLocation();
        Particle.DustOptions po = new Particle.DustOptions(cor, 1.3f);
        double dy = dentroDoBloco ? 0.5 : 1.1, abre = dentroDoBloco ? 0.55 : 0.25;
        int n = 0;
        for (Location l : pontos) {
            if (l.getWorld() != eu.getWorld() || l.distanceSquared(eu) > 100) continue;
            // Numa parede, as partículas saem pelas faces do bloco.
            p.spawnParticle(Particle.DUST, l.clone().add(0.5, dy, 0.5), 3, abre, abre * 0.6, abre, 0, po);
            if (++n >= limite) break;
        }
    }

    // =====================================================================
    //  Entrar e sair do servidor
    // =====================================================================

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            ArvoreCacador.conferir(p);
            Despertar.verificar(p);
        }, 60L);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ArvoreCacador.esquecer(e.getPlayer().getUniqueId());
        Despertar.esquecer(e.getPlayer().getUniqueId());
    }
}
