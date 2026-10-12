package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combate montado. Montado num companheiro, a tecla F (trocar de mão, sem agachar) solta o golpe da
 * montaria: cada lendária tem o seu, e as outras montarias dão uma Investida. Correndo montado, a
 * montaria atropela os monstros que encontra. O dano cresce com a Doma de quem monta e só acerta
 * monstros (nunca outros jogadores, companheiros ou moradores).
 */
public final class CombateMontado implements Listener {

    /** Um golpe da montaria: nome, recarga e o que ele faz. */
    private enum GolpeMontado {
        INVESTIDA("Investida", 8),
        CHAMA_DE_JADE("Chama de Jade", 15),
        MERGULHO_DE_FOGO("Mergulho de Fogo", 15),
        MAREMOTO("Maremoto", 12),
        BOTE_DO_TIGRE("Bote do Tigre", 10),
        CASCO_INABALAVEL("Casco Inabalável", 18),
        OLHAR_DA_VERDADE("Olhar da Verdade", 12),
        RAJADA_DO_CEU("Rajada do Céu", 12);

        final String nome;
        final int recarga;

        GolpeMontado(String nome, int recarga) {
            this.nome = nome;
            this.recarga = recarga;
        }

        static GolpeMontado de(LivingEntity montaria) {
            Lendaria l = Lendaria.de(montaria);
            if (l == null) return INVESTIDA;
            return switch (l) {
                case QILIN -> CHAMA_DE_JADE;
                case FENGHUANG -> MERGULHO_DE_FOGO;
                case LONGMA -> MAREMOTO;
                case BAIHU -> BOTE_DO_TIGRE;
                case XUANWU -> CASCO_INABALAVEL;
                case BAIZE -> OLHAR_DA_VERDADE;
                case KUNPENG -> RAJADA_DO_CEU;
                case PIXIU -> INVESTIDA;
            };
        }
    }

    /** Um golpe que continua nos próximos ticks (investida, bote, mergulho). */
    private static final class EmCurso {
        final Player p;
        final LivingEntity montaria;
        final GolpeMontado g;
        final double dano;
        final List<UUID> acertados = new ArrayList<>();
        int ticks;

        EmCurso(Player p, LivingEntity montaria, GolpeMontado g, double dano) {
            this.p = p;
            this.montaria = montaria;
            this.g = g;
            this.dano = dano;
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Long> recargas = new HashMap<>();
    private final Map<UUID, Long> atropelados = new HashMap<>();
    private final List<EmCurso> emCurso = new ArrayList<>();

    public CombateMontado(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** A montaria do jogador, se ele estiver num companheiro (dele ou da party). */
    private static LivingEntity montaria(Player p) {
        return p.getVehicle() instanceof LivingEntity m && Companheiros.eh(m) ? m : null;
    }

    /** Dano base do golpe, que cresce com a Doma de quem monta (até o dobro). */
    private double dano(Player p, double base) {
        double nivel = plugin.stats().getNivel(p, Skill.DOMA), max = Math.max(1, plugin.settings().nivelMaximo);
        return base * (1 + nivel / max);
    }

    /** Só monstros: nada de jogadores, companheiros, moradores ou animais. */
    private static boolean inimigo(Entity e) {
        return e instanceof LivingEntity le && le instanceof Enemy && le.isValid() && !le.isDead() && !Companheiros.eh(le);
    }

    // =====================================================================
    //  F montado: o golpe da montaria
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoApertarF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (p.isSneaking()) return; // agachar + F continua sendo a habilidade de classe
        LivingEntity m = montaria(p);
        if (m == null) return;
        e.setCancelled(true);
        GolpeMontado g = GolpeMontado.de(m);
        long falta = recargas.getOrDefault(m.getUniqueId(), 0L) - System.currentTimeMillis();
        TextColor cor = Lendaria.de(m) != null ? TextColor.color(Lendaria.de(m).cor()) : Companheiros.COR;
        if (falta > 0) {
            p.sendActionBar(Component.text("♞ " + g.nome + " recarregando (" + (falta / 1000 + 1) + " s)", NamedTextColor.RED));
            return;
        }
        recargas.put(m.getUniqueId(), System.currentTimeMillis() + g.recarga * 1000L);
        p.sendActionBar(Component.text("♞ " + g.nome + "!", cor, TextDecoration.BOLD));
        usar(p, m, g);
        if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
            plugin.stats().darXp(p, Skill.DOMA, 6);
        }
    }

    private void usar(Player p, LivingEntity m, GolpeMontado g) {
        World w = m.getWorld();
        Location pe = m.getLocation();
        Vector frente = p.getLocation().getDirection().setY(0);
        if (frente.lengthSquared() < 0.01) frente = new Vector(0, 0, 1);
        frente.normalize();
        switch (g) {
            case INVESTIDA -> {
                m.setVelocity(frente.clone().multiply(1.4).setY(0.2));
                w.playSound(pe, Sound.ENTITY_HORSE_GALLOP, 1.5f, 0.8f);
                emCurso.add(new EmCurso(p, m, g, dano(p, 6)));
            }
            case CHAMA_DE_JADE -> {
                // Cone de fogo verde à frente; a party em volta se cura.
                for (LivingEntity alvo : w.getNearbyLivingEntities(pe, 8)) {
                    if (!inimigo(alvo) || !naFrente(pe, frente, alvo, 0.5)) continue;
                    alvo.damage(dano(p, 6), p);
                    alvo.setFireTicks(100);
                }
                for (int i = 1; i <= 8; i++) {
                    Location c = pe.clone().add(frente.clone().multiply(i)).add(0, 1, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, c, 10, 0.3 + i * 0.12, 0.3, 0.3 + i * 0.12, 0.01);
                    w.spawnParticle(Particle.HAPPY_VILLAGER, c, 2, 0.4, 0.3, 0.4, 0);
                }
                for (Player aliado : aliados(p, pe, 8)) aliado.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 1, true, true, true));
                w.playSound(pe, Sound.ITEM_FIRECHARGE_USE, 1.2f, 1.3f);
            }
            case MERGULHO_DE_FOGO -> {
                if (!m.isOnGround()) {
                    // No ar: mergulha e explode em fogo onde tocar o chão.
                    m.setGravity(true);
                    m.setVelocity(frente.clone().multiply(0.6).setY(-1.6));
                    w.playSound(pe, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
                    emCurso.add(new EmCurso(p, m, g, dano(p, 10)));
                } else {
                    explosaoDeFogo(p, m, dano(p, 6), 4);
                }
            }
            case MAREMOTO -> {
                // Uma onda em linha reta que arrasta os monstros.
                for (LivingEntity alvo : w.getNearbyLivingEntities(pe.clone().add(frente.clone().multiply(5)), 6)) {
                    if (!inimigo(alvo) || !naFrente(pe, frente, alvo, 0.2)) continue;
                    alvo.damage(dano(p, 7), p);
                    alvo.setVelocity(frente.clone().multiply(1.3).setY(0.5));
                }
                for (int i = 1; i <= 10; i++) {
                    w.spawnParticle(Particle.SPLASH, pe.clone().add(frente.clone().multiply(i)).add(0, 0.6, 0), 30, 1.2, 0.4, 1.2, 0.2);
                }
                for (Player aliado : aliados(p, pe, 8)) aliado.setFireTicks(0);
                w.playSound(pe, Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.6f);
            }
            case BOTE_DO_TIGRE -> {
                // Salta sobre o monstro que o jogador mira (ou para a frente).
                Entity mirado = p.getTargetEntity(16);
                Vector salto = mirado != null && inimigo(mirado)
                        ? mirado.getLocation().toVector().subtract(pe.toVector()).multiply(0.18).setY(0.6)
                        : frente.clone().multiply(1.6).setY(0.6);
                if (salto.length() > 2.2) salto.normalize().multiply(2.2);
                m.setVelocity(salto);
                w.playSound(pe, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.5f);
                emCurso.add(new EmCurso(p, m, g, dano(p, 12)));
            }
            case CASCO_INABALAVEL -> {
                for (LivingEntity alvo : w.getNearbyLivingEntities(pe, 6)) {
                    if (!inimigo(alvo)) continue;
                    alvo.damage(dano(p, 6), p);
                    Vector fora = alvo.getLocation().toVector().subtract(pe.toVector()).setY(0);
                    if (fora.lengthSquared() < 0.01) fora = frente.clone();
                    alvo.setVelocity(fora.normalize().multiply(1.6).setY(0.5));
                }
                for (Player aliado : aliados(p, pe, 7)) aliado.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1, true, true, true));
                w.spawnParticle(Particle.BLOCK, pe.clone().add(0, 0.2, 0), 80, 2.5, 0.2, 2.5, 0, org.bukkit.Material.MOSSY_STONE_BRICKS.createBlockData());
                w.playSound(pe, Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.5f);
                w.playSound(pe, Sound.ENTITY_TURTLE_HURT, 1.5f, 0.5f);
            }
            case OLHAR_DA_VERDADE -> {
                for (LivingEntity alvo : w.getNearbyLivingEntities(pe, 16)) {
                    if (!inimigo(alvo) || !naFrente(pe, frente, alvo, 0.3)) continue;
                    alvo.damage(dano(p, 4), p);
                    alvo.removePotionEffect(PotionEffectType.INVISIBILITY);
                    alvo.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
                    alvo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                }
                w.spawnParticle(Particle.ENCHANT, pe.clone().add(0, 1.5, 0), 120, 3, 1, 3, 1);
                w.playSound(pe, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
            }
            case RAJADA_DO_CEU -> {
                for (LivingEntity alvo : w.getNearbyLivingEntities(pe, 10)) {
                    if (!inimigo(alvo)) continue;
                    alvo.damage(dano(p, 5), p);
                    Vector fora = alvo.getLocation().toVector().subtract(pe.toVector());
                    if (fora.lengthSquared() < 0.01) fora = frente.clone();
                    alvo.setVelocity(fora.normalize().multiply(1.8).setY(0.3));
                }
                w.spawnParticle(Particle.GUST_EMITTER_LARGE, pe.clone().add(0, 1, 0), 1);
                w.spawnParticle(Particle.CLOUD, pe.clone().add(0, 1, 0), 80, 4, 1.5, 4, 0.1);
                w.playSound(pe, Sound.ENTITY_BREEZE_WIND_BURST, 1.5f, 0.7f);
            }
        }
    }

    /** Montaria no meio de um golpe (investida, bote, mergulho): quem guia espera ele acabar. */
    public boolean ocupada(Entity montaria) {
        for (EmCurso c : emCurso) if (c.montaria.equals(montaria)) return true;
        return false;
    }

    /** O alvo está na frente (produto escalar com a direção maior que o mínimo)? */
    private static boolean naFrente(Location de, Vector frente, Entity alvo, double minimo) {
        Vector ate = alvo.getLocation().toVector().subtract(de.toVector()).setY(0);
        return ate.lengthSquared() < 1 || ate.normalize().dot(frente) >= minimo;
    }

    /** O próprio jogador e quem é da party dele, perto. */
    private List<Player> aliados(Player p, Location c, double raio) {
        Party pt = plugin.parties().party(p);
        List<Player> l = new ArrayList<>();
        for (Player x : c.getWorld().getNearbyPlayers(c, raio)) {
            if (x.equals(p) || (pt != null && pt.tem(x.getUniqueId()))) l.add(x);
        }
        return l;
    }

    private void explosaoDeFogo(Player p, LivingEntity m, double dano, double raio) {
        World w = m.getWorld();
        Location c = m.getLocation();
        for (LivingEntity alvo : w.getNearbyLivingEntities(c, raio)) {
            if (!inimigo(alvo)) continue;
            alvo.damage(dano, p);
            alvo.setFireTicks(120);
            Vector fora = alvo.getLocation().toVector().subtract(c.toVector()).setY(0);
            if (fora.lengthSquared() > 0.01) alvo.setVelocity(fora.normalize().multiply(0.9).setY(0.5));
        }
        w.spawnParticle(Particle.FLAME, c.clone().add(0, 0.5, 0), 150, raio / 2, 0.5, raio / 2, 0.12);
        w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 3, 1, 0.3, 1, 0);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
    }

    // =====================================================================
    //  A cada tick: golpes em curso e atropelar
    // =====================================================================

    public void tick() {
        for (Iterator<EmCurso> it = emCurso.iterator(); it.hasNext(); ) {
            EmCurso c = it.next();
            c.ticks++;
            if (!c.montaria.isValid() || !c.p.isOnline()) {
                it.remove();
                continue;
            }
            if (c.g == GolpeMontado.MERGULHO_DE_FOGO) {
                c.montaria.getWorld().spawnParticle(Particle.FLAME, c.montaria.getLocation(), 6, 0.5, 0.3, 0.5, 0.02);
                if (c.montaria.isOnGround() || c.montaria.isInWater() || c.ticks > 80) {
                    explosaoDeFogo(c.p, c.montaria, c.dano, 5);
                    it.remove();
                }
                continue;
            }
            // Investida e bote: acerta (uma vez cada) quem a montaria encostar.
            for (Entity x : c.montaria.getNearbyEntities(1.5, 1.2, 1.5)) {
                if (!inimigo(x) || c.acertados.contains(x.getUniqueId())) continue;
                c.acertados.add(x.getUniqueId());
                LivingEntity alvo = (LivingEntity) x;
                alvo.damage(c.dano, c.p);
                Vector empurra = c.montaria.getVelocity().clone().setY(0);
                if (empurra.lengthSquared() > 0.01) alvo.setVelocity(empurra.normalize().multiply(1.1).setY(0.45));
                if (c.g == GolpeMontado.BOTE_DO_TIGRE) alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                alvo.getWorld().spawnParticle(Particle.CRIT, alvo.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.2);
            }
            if (c.ticks > 20) it.remove();
        }
        if (Bukkit.getCurrentTick() % 4 == 0) atropelar();
    }

    /** Correndo montado: a montaria derruba os monstros no caminho (cada um no máximo 1 vez por segundo). */
    private void atropelar() {
        long agora = System.currentTimeMillis();
        atropelados.values().removeIf(t -> agora - t > 5000);
        for (Player p : Bukkit.getOnlinePlayers()) {
            LivingEntity m = montaria(p);
            if (m == null || !p.getCurrentInput().isSprint()) continue;
            Vector v = m.getVelocity().clone().setY(0);
            if (v.lengthSquared() < 0.09) continue; // devagar não atropela
            for (Entity x : m.getNearbyEntities(m.getWidth() / 2 + 0.6, 1, m.getWidth() / 2 + 0.6)) {
                if (!inimigo(x) || agora - atropelados.getOrDefault(x.getUniqueId(), 0L) < 1000) continue;
                atropelados.put(x.getUniqueId(), agora);
                LivingEntity alvo = (LivingEntity) x;
                alvo.damage(dano(p, 3), p);
                alvo.setVelocity(v.clone().normalize().multiply(0.9).setY(0.4));
                alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
            }
        }
    }
}
