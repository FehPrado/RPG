package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;

/** Uso do grimório, entrada/saída, morte (instabilidade) e os efeitos que reagem a dano. */
public final class ArcanoListener implements Listener {

    private static final double REFLEXO_BASTIAO = 0.3;

    private final RPGAtributos plugin;
    private boolean refletindo;

    public ArcanoListener(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Arcano arcano() { return plugin.arcano(); }

    // =====================================================================
    //  Entrar, sair, morrer
    // =====================================================================

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        arcano().perfil(e.getPlayer());
        arcano().aplicarBonus(e.getPlayer());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        arcano().descarregar(e.getPlayer());
    }

    @EventHandler
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        Perfil pf = arcano().perfil(p);
        if (!pf.temInfusao()) return;
        pf.instavelAte = System.currentTimeMillis() + plugin.settings().arcInstabilidadeSeg * 1000L;
        pf.avisouInstavel = true;
        pf.fenixAte = 0;
        arcano().salvar(p);
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Perfil pf = arcano().perfil(p);
        if (!pf.instavel()) return;
        Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage(
                Component.text("☠ Suas infusões estão instáveis por " + pf.segundosInstavel()
                        + "s: magias mais fracas e mais caras.", NamedTextColor.RED)));
    }

    // =====================================================================
    //  Grimório
    // =====================================================================

    @EventHandler(priority = EventPriority.NORMAL)
    public void aoUsarGrimorio(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            if (arcano().ehGrimorio(e.getItem())) e.setUseItemInHand(Event.Result.DENY);
            return;
        }
        if (!arcano().ehGrimorio(e.getItem()) || e.getAction() == Action.PHYSICAL) return;
        Block clicado = e.getClickedBlock();
        // O clique na estação (infusor, forja, altar, cozinha, reciclagem) é tratado por ela.
        if (arcano().infusores().eh(clicado) || plugin.forjas().eh(clicado) || plugin.altares().eh(clicado)
                || plugin.cozinha().eh(clicado) || plugin.reciclagem().eh(clicado)) return;

        e.setUseItemInHand(Event.Result.DENY); // o livro nunca é "usado" (não some)
        boolean direito = e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (direito) {
            if (p.isSneaking()) {
                e.setCancelled(true);
                arcano().menus().abrirGrimorio(p);
                return;
            }
            // Deixa abrir porta, baú etc. normalmente com o grimório na mão.
            if (interativo(clicado)) return;
            e.setCancelled(true);
            arcano().conjuracao().lancar(p);
        } else {
            e.setCancelled(true);
            trocarMagia(p);
        }
    }

    /** Bloco que reage a clique direito (porta, baú, alavanca...). */
    @SuppressWarnings("deprecation") // ainda é o jeito oficial de saber isso no Paper
    private static boolean interativo(Block b) {
        return b != null && b.getType().asBlockType() != null && b.getType().asBlockType().isInteractable();
    }

    private void trocarMagia(Player p) {
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        int espacos = arc.espacosGrimorio(p);
        for (int passo = 1; passo <= espacos; passo++) {
            int i = (pf.selecionada + passo) % espacos;
            Magia m = pf.magia(i);
            if (m == null) continue;
            pf.selecionada = i;
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.4f);
            p.sendActionBar(Component.text("✦ " + m.nome(), Arcano.COR)
                    .append(Component.text("  ·  " + (int) arc.custoMana(m, pf) + " mana", NamedTextColor.AQUA)));
            return;
        }
        p.sendActionBar(Component.text("Nenhuma magia no grimório. Agache + clique direito para criar.", NamedTextColor.RED));
    }

    // =====================================================================
    //  Bastião, Fênix e Fantasma
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGolpear(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && e.getEntity() instanceof LivingEntity alvo
                && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            arcano().conjuracao().golpeDoAvatar(p, alvo);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageByEntityEvent e) {
        if (refletindo || !(e.getEntity() instanceof Player p) || !(e.getDamager() instanceof LivingEntity agressor)) return;
        if (agressor == p || e.getCause() == EntityDamageEvent.DamageCause.THORNS) return;
        Perfil pf = arcano().perfil(p);
        if (Bukkit.getCurrentTick() >= pf.bastiaoAte) return;
        double refletido = e.getFinalDamage() * REFLEXO_BASTIAO;
        if (refletido < 0.5) return;
        refletindo = true;
        try {
            agressor.damage(refletido, DamageSource.builder(DamageType.THORNS).withCausingEntity(p).withDirectEntity(p).build());
        } finally {
            refletindo = false;
        }
        agressor.getWorld().spawnParticle(Particle.WAX_ON, agressor.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoQuaseMorrer(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        // /kill e o vazio não são impedidos (no vazio ele só cairia de novo).
        if (e.getCause() == EntityDamageEvent.DamageCause.KILL || e.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        Perfil pf = arcano().perfil(p);
        if (Bukkit.getCurrentTick() >= pf.fenixAte || e.getFinalDamage() < p.getHealth()) return;

        // A Fênix: em vez de morrer, renasce em chamas.
        e.setCancelled(true);
        pf.fenixAte = 0;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(Math.max(1, (max == null ? 20 : max.getValue()) * 0.5));
        p.setFireTicks(0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 0));
        World w = p.getWorld();
        w.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 150, 1, 1, 1, 0.2);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.5);
        w.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.2f);
        for (Entity ent : p.getNearbyEntities(5, 3, 5)) {
            if (!(ent instanceof LivingEntity le) || (ent instanceof Player alvo && !plugin.pvpPermitido(p, alvo))) continue;
            le.setFireTicks(Math.max(le.getFireTicks(), 100));
            Vector v = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (v.lengthSquared() > 1e-4) le.setVelocity(v.normalize().multiply(1.2).setY(0.5));
        }
        p.showTitle(Title.title(Component.text("♨ Fênix ♨", Essencia.FOGO.cor(), TextDecoration.BOLD),
                Component.text("Você renasceu das cinzas!", NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1800), Duration.ofMillis(500))));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        if (Bukkit.getCurrentTick() < arcano().perfil(p).fantasmaAte) e.setCancelled(true);
    }
}
