package br.rpgatributos.combate;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.combo.Equilibrio;
import br.rpgatributos.dimensao.MundoDasAlmas;
import br.rpgatributos.perigo.Assombros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Luta pesada (3.17): o inimigo forte também pede as ferramentas do jogador.
 * <ul>
 *   <li><b>Golpe anunciado:</b> quem tem 40 de vida ou mais (elites, chefes, assombros) às vezes para,
 *       brilha e avisa: <b>amarelo</b> é um golpe pesado que dá para aparar (aparar quebra o equilíbrio dele),
 *       <b>vermelho</b> esmaga em volta e só a esquiva salva (o escudo não segura), <b>roxo</b> é um agarrão:
 *       saia de perto antes.</li>
 *   <li><b>Execução:</b> com o equilíbrio quebrado, agachado + golpe: um quarto da vida dele de uma vez (um
 *       oitavo nos chefes), câmera lenta curta, e se ele morrer logo depois os drops vêm em dobro.</li>
 *   <li><b>Fases:</b> chefes (150 de vida ou mais) mudam aos 66% e aos 33%: rugem, empurram quem está perto e
 *       ficam mais fortes e rápidos. O Juiz ergue o escudo da balança: só a Lâmina Espectral corta inteiro.</li>
 * </ul>
 */
public final class LutaPesada implements Listener {

    public enum Golpe {
        PESADO("⚠ GOLPE PESADO: apare!", 0xFFD54F), ESMAGAR("⚠ ESMAGAR: esquive!", 0xE53935), AGARRAR("⚠ AGARRÃO: se afaste!", 0xAB47BC);

        final String aviso;
        final TextColor cor;
        final Color tinta;

        Golpe(String aviso, int cor) {
            this.aviso = aviso;
            this.cor = TextColor.color(cor);
            this.tinta = Color.fromRGB(cor);
        }
    }

    private static final class Preparo {
        final Golpe golpe;
        final UUID alvo;
        final long ate;

        Preparo(Golpe golpe, UUID alvo, long ate) {
            this.golpe = golpe;
            this.alvo = alvo;
            this.ate = ate;
        }
    }

    private static final NamespacedKey K_EXECUTADO = new NamespacedKey("rpgatributos", "executado_ate");
    private static final long PREPARO_MS = 1000;
    private static final double VIDA_CHEFE = 150;

    private final RPGAtributos plugin;
    private final Map<UUID, Preparo> preparando = new HashMap<>();
    private final Map<UUID, Long> proximo = new HashMap<>();
    private final Map<UUID, Integer> fases = new HashMap<>();
    /** Quem está levando um golpe pesado agora (para saber se aparou). */
    private final Set<UUID> pesadoEmCurso = new HashSet<>();
    private final Map<UUID, Long> avisoJuiz = new HashMap<>();
    private int ciclo;

    public LutaPesada(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private boolean ligado(String chave) {
        return plugin.getConfig().getBoolean("combate." + chave, true);
    }

    private static double vidaMax(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? 0 : a.getValue();
    }

    private static double ataque(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.ATTACK_DAMAGE);
        return Math.max(4, a == null ? 4 : a.getValue());
    }

    /** Inimigo forte que pode anunciar golpes. */
    private static boolean forte(Entity e) {
        if (!(e instanceof Mob m) || !m.isValid() || m.isDead() || !m.hasAI() || !m.isAware()) return false;
        if (!(e instanceof Enemy) && Assombros.tipo(e) == null) return false;
        return Equilibrio.tem(m);
    }

    public static boolean chefe(LivingEntity e) {
        return e instanceof Mob && (e instanceof Enemy || Assombros.tipo(e) != null) && vidaMax(e) >= VIDA_CHEFE;
    }

    private int fase(Entity e) {
        return fases.getOrDefault(e.getUniqueId(), 1);
    }

    // =====================================================================
    //  A cada 2 ticks
    // =====================================================================

    public void tick() {
        ciclo++;
        long agora = System.currentTimeMillis();
        if (ciclo % 5 == 0) {
            if (ligado("golpe-anunciado")) comecarGolpes(agora);
            if (ligado("fases-de-chefe")) verFases();
        }
        for (Iterator<Map.Entry<UUID, Preparo>> it = preparando.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            Preparo pr = en.getValue();
            if (!(ent instanceof Mob m) || !m.isValid() || m.isDead()) { it.remove(); continue; }
            Location c = m.getLocation().add(0, m.getHeight() * 0.6, 0);
            double raio = Math.max(0.8, m.getWidth());
            for (int i = 0; i < 8; i++) {
                double a = (ciclo * 0.6) + i * Math.PI / 4;
                m.getWorld().spawnParticle(Particle.DUST, c.clone().add(Math.cos(a) * raio, 0, Math.sin(a) * raio), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(pr.golpe.tinta, 1.6f));
            }
            if (agora < pr.ate) continue;
            it.remove();
            m.setGlowing(false);
            soltar(m, pr);
        }
    }

    private void comecarGolpes(long agora) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            for (Entity e : p.getNearbyEntities(5, 3, 5)) {
                if (!forte(e) || preparando.containsKey(e.getUniqueId())) continue;
                Mob m = (Mob) e;
                if (m.getTarget() != p || agora < proximo.getOrDefault(e.getUniqueId(), 0L)) continue;
                if (!proximo.containsKey(e.getUniqueId())) {
                    // A primeira vez espera um pouco (ninguém leva um golpe anunciado no primeiro segundo).
                    proximo.put(e.getUniqueId(), agora + 2500 + r.nextInt(2500));
                    continue;
                }
                double sorte = r.nextDouble();
                Golpe g = sorte < 0.5 ? Golpe.PESADO : sorte < 0.8 ? Golpe.ESMAGAR : Golpe.AGARRAR;
                anunciar(m, p, g, agora);
            }
        }
    }

    /** Começa a anunciar um golpe (também pelo /rpgadmin). */
    public void anunciar(Mob m, Player alvo, Golpe g, long agora) {
        int f = fase(m);
        long preparo = f >= 3 ? 750 : PREPARO_MS;
        preparando.put(m.getUniqueId(), new Preparo(g, alvo.getUniqueId(), agora + preparo));
        long espera = (long) ((7000 + ThreadLocalRandom.current().nextInt(4000)) / (f >= 3 ? 1.6 : f == 2 ? 1.3 : 1.0));
        proximo.put(m.getUniqueId(), agora + preparo + espera);
        m.setGlowing(true);
        m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int) (preparo / 50), 3, false, false, false));
        World w = m.getWorld();
        Location l = m.getLocation();
        switch (g) {
            case PESADO -> w.playSound(l, Sound.BLOCK_ANVIL_PLACE, 1f, 1.7f);
            case ESMAGAR -> w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
            case AGARRAR -> w.playSound(l, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1f, 0.8f);
        }
        for (Player o : w.getPlayers()) {
            if (o.getLocation().distanceSquared(l) > 10 * 10) continue;
            o.sendActionBar(Component.text(g.aviso, g.cor, TextDecoration.BOLD));
        }
    }

    private void soltar(Mob m, Preparo pr) {
        Player alvo = Bukkit.getPlayer(pr.alvo);
        World w = m.getWorld();
        Location c = m.getLocation();
        double dano = ataque(m) * (fase(m) >= 2 ? 1.25 : 1);
        switch (pr.golpe) {
            case PESADO -> {
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
                if (alvo == null || !alvo.getWorld().equals(w) || alvo.getLocation().distanceSquared(c) > 4.5 * 4.5) return;
                pesadoEmCurso.add(alvo.getUniqueId());
                try {
                    alvo.damage(dano * 2, m);
                } finally {
                    pesadoEmCurso.remove(alvo.getUniqueId());
                }
            }
            case ESMAGAR -> {
                w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.7f);
                for (int i = 0; i < 24; i++) {
                    double a = i * Math.PI / 12;
                    w.spawnParticle(Particle.EXPLOSION, c.clone().add(Math.cos(a) * 3.5, 0.2, Math.sin(a) * 3.5), 1, 0, 0, 0, 0);
                }
                DamageSource fonte = DamageSource.builder(DamageType.FALLING_ANVIL).withCausingEntity(m).withDirectEntity(m).build();
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(c) > 4 * 4 || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
                    p.damage(dano * 2.5, fonte);
                    if (!p.isDead()) p.setVelocity(p.getVelocity().setY(0.5));
                }
            }
            case AGARRAR -> {
                w.playSound(c, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1f, 0.6f);
                if (alvo == null || !alvo.getWorld().equals(w) || alvo.getLocation().distanceSquared(c) > 3 * 3) {
                    if (alvo != null) alvo.sendActionBar(Component.text("✧ Escapou do agarrão!", NamedTextColor.AQUA));
                    return;
                }
                Vector frente = c.getDirection().setY(0);
                if (frente.lengthSquared() < 0.01) frente = new Vector(1, 0, 0);
                Location presa = c.clone().add(frente.normalize().multiply(Math.max(1.0, m.getWidth())));
                presa.setYaw(alvo.getLocation().getYaw());
                presa.setPitch(alvo.getLocation().getPitch());
                alvo.teleport(presa);
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 6));
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0));
                alvo.damage(dano * 1.5, m);
                alvo.sendActionBar(Component.text("✖ Agarrado!", Golpe.AGARRAR.cor, TextDecoration.BOLD));
            }
        }
    }

    /** Golpe pesado aparado (o Aparar cancela o dano): o equilíbrio dele quebra na hora. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoBaterPesado(EntityDamageByEntityEvent e) {
        if (!e.isCancelled() || !pesadoEmCurso.contains(e.getEntity().getUniqueId()) || !(e.getDamager() instanceof LivingEntity atacante)) return;
        // Só vale com o escudo (a esquiva também cancela o dano, mas não é aparar).
        if (!(e.getEntity() instanceof Player p) || !p.isHandRaised() || p.getActiveItem().getType() != org.bukkit.Material.SHIELD) return;
        boolean quebrou = plugin.combos().equilibrio().quebrarAgora(atacante);
        p.sendActionBar(Component.text(quebrou ? "✦ Golpe pesado aparado! Agache e golpeie: EXECUÇÃO" : "✦ Golpe pesado aparado!", Aparar.COR, TextDecoration.BOLD));
        plugin.titulos().registrar(p, "pesados_aparados", 1);
    }

    // =====================================================================
    //  Execução
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExecutar(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !p.isSneaking() || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getEntity() instanceof LivingEntity alvo) || !ligado("execucao")) return;
        Equilibrio eq = plugin.combos().equilibrio();
        if (!eq.quebrado(alvo)) return;
        double max = vidaMax(alvo);
        e.setDamage(Math.max(e.getDamage(), max * (chefe(alvo) ? 0.125 : 0.25)));
        eq.encerrar(alvo);
        alvo.getPersistentDataContainer().set(K_EXECUTADO, PersistentDataType.LONG, System.currentTimeMillis() + 3000);
        // Câmera lenta curta.
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 12, 3, false, false, false));
        alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 5, false, false, false));
        Location c = alvo.getLocation().add(0, alvo.getHeight() * 0.6, 0);
        World w = alvo.getWorld();
        w.spawnParticle(Particle.SWEEP_ATTACK, c, 3, 0.3, 0.3, 0.3, 0);
        w.spawnParticle(Particle.CRIT, c, 50, 0.4, 0.6, 0.4, 0.5);
        w.spawnParticle(Particle.FLASH, c, 1, 0, 0, 0, 0, org.bukkit.Color.WHITE);
        w.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);
        w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.5f);
        p.showTitle(Title.title(Component.empty(), Component.text("✦ EXECUÇÃO", NamedTextColor.GOLD, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(900), Duration.ofMillis(300))));
        plugin.combos().darVigor(p, 15);
        plugin.titulos().registrar(p, "execucoes", 1);
    }

    /** Executado e morto logo depois: os drops comuns vêm em dobro (materiais raros e núcleos não). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoMorrer(EntityDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        preparando.remove(id);
        proximo.remove(id);
        fases.remove(id);
        avisoJuiz.remove(id);
        Long ate = e.getEntity().getPersistentDataContainer().get(K_EXECUTADO, PersistentDataType.LONG);
        if (ate == null || ate < System.currentTimeMillis() || e.getEntity().getKiller() == null) return;
        List<ItemStack> extra = new ArrayList<>();
        for (ItemStack s : e.getDrops()) if (s != null && !s.isEmpty() && Raro.de(s) == null && s.getMaxStackSize() > 1) extra.add(s.clone());
        e.getDrops().addAll(extra);
        e.setDroppedExp(e.getDroppedExp() * 2);
        e.getEntity().getKiller().sendActionBar(Component.text("✦ Execução: espólio em dobro!", NamedTextColor.GOLD));
    }

    // =====================================================================
    //  Fases dos chefes
    // =====================================================================

    private void verFases() {
        Set<UUID> vistos = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Entity e : p.getNearbyEntities(32, 16, 32)) {
                if (!(e instanceof LivingEntity le) || !vistos.add(e.getUniqueId()) || !chefe(le) || le.isDead()) continue;
                double frac = le.getHealth() / vidaMax(le);
                int nova = frac <= 1.0 / 3 ? 3 : frac <= 2.0 / 3 ? 2 : 1;
                if (nova > fase(le)) mudarFase(le, nova);
            }
        }
    }

    private void mudarFase(LivingEntity c, int fase) {
        fases.put(c.getUniqueId(), fase);
        World w = c.getWorld();
        Location l = c.getLocation();
        c.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (c.isValid()) c.setInvulnerable(false); }, 30);
        c.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, fase - 2, false, false, false));
        if (fase >= 3) c.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        w.playSound(l, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, fase >= 3 ? 0.8f : 1.2f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, l.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        for (Player p : w.getPlayers()) {
            double d2 = p.getLocation().distanceSquared(l);
            if (d2 > 40 * 40) continue;
            if (d2 < 6 * 6 && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                Vector longe = p.getLocation().toVector().subtract(l.toVector()).setY(0);
                if (longe.lengthSquared() < 0.01) longe = new Vector(1, 0, 0);
                p.setVelocity(longe.normalize().multiply(1.2).setY(0.5));
            }
            Component nome = c.customName() != null ? c.customName() : Component.translatable(c.getType().translationKey());
            p.showTitle(Title.title(Component.text("☠ FASE " + fase, NamedTextColor.RED, TextDecoration.BOLD), nome,
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1600), Duration.ofMillis(400))));
            if (MundoDasAlmas.ehJuiz(c) && fase == 2) {
                p.sendMessage(Component.text("A balança se ergue em volta do Juiz. Só uma lâmina das almas corta esse escudo.", TextColor.color(0x7FD8E8)));
            }
        }
        // Na fase nova o próximo golpe anunciado vem logo.
        proximo.put(c.getUniqueId(), System.currentTimeMillis() + 1500);
    }

    /** O escudo da balança: da fase 2 em diante o Juiz só leva dano inteiro da Lâmina Espectral. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerirJuiz(EntityDamageByEntityEvent e) {
        if (!MundoDasAlmas.ehJuiz(e.getEntity()) || fase(e.getEntity()) < 2) return;
        Player p = e.getDamager() instanceof Player pl ? pl
                : e.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p != null && MundoDasAlmas.ehEspectral(p.getInventory().getItemInMainHand())) return;
        e.setDamage(e.getDamage() * 0.35);
        long agora = System.currentTimeMillis();
        if (p != null && agora > avisoJuiz.getOrDefault(e.getEntity().getUniqueId(), 0L)) {
            avisoJuiz.put(e.getEntity().getUniqueId(), agora + 6000);
            p.sendActionBar(Component.text("A balança segura o golpe. (Lâmina Espectral)", TextColor.color(0x7FD8E8)));
            e.getEntity().getWorld().spawnParticle(Particle.SOUL, e.getEntity().getLocation().add(0, 1.5, 0), 10, 0.5, 0.6, 0.5, 0.02);
        }
    }

    public void desligar() {
        for (UUID id : preparando.keySet()) if (Bukkit.getEntity(id) instanceof Mob m) m.setGlowing(false);
        preparando.clear();
    }
}
