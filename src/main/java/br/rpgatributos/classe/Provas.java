package br.rpgatributos.classe;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.masmorra.Tema;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Prova de Mudança de Classe: uma arena só sua no mundo das masmorras. Ondas de monstros,
 * alvos só com flechas ou um duelo contra o Guardião da classe. Morrer na prova não
 * perde nada; falhar faz esperar alguns minutos para tentar de novo.
 */
public final class Provas implements Listener {

    private static final int TAMANHO = 27, ALTURA = 10;
    private static final long DURACAO_MS = 4 * 60_000;

    private static final class Instancia {
        final UUID jogador;
        final Classe classe;
        final Location centro;
        final long ate;
        final Set<UUID> mobs = new HashSet<>();
        final List<UUID> aliados = new ArrayList<>();
        int onda;
        int alvosFaltando;
        long proximaOnda;
        UUID guardiao;
        BossBar barra;
        boolean comecou;

        Instancia(UUID jogador, Classe classe, Location centro) {
            this.jogador = jogador;
            this.classe = classe;
            this.centro = centro;
            this.ate = System.currentTimeMillis() + DURACAO_MS + 3000;
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Instancia> ativas = new HashMap<>();
    private final Map<UUID, Long> esperar = new HashMap<>();
    private final Set<UUID> morreram = new HashSet<>();
    private int proximaVaga;

    public Provas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    public boolean emProva(Player p) {
        return ativas.containsKey(p.getUniqueId());
    }

    /** @return motivo de não ter começado, ou null. */
    public String iniciar(Player p, Classe c) {
        return iniciar(p, c, false);
    }

    /** {@code semTarefas}: admin testando (pula as tarefas do caminho). */
    public String iniciar(Player p, Classe c, boolean semTarefas) {
        if (!semTarefas && !plugin.classes().tarefasFeitas(p, c)) return "Termine as tarefas do caminho antes da prova.";
        if (emProva(p)) return "Você já está numa prova.";
        if (plugin.masmorras().de(p) != null) return "Saia da masmorra antes.";
        World w = plugin.masmorras().mundo();
        if (w == null) return "O mundo das provas não está disponível.";
        long espera = esperar.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        if (espera > 0) return "Descanse antes de tentar de novo (" + (espera / 60_000 + 1) + " min).";

        int vaga = proximaVaga++;
        Location centro = new Location(w, -100_000 - vaga * 200 + 0.5, 65, 100_000.5);
        construirArena(centro);
        Instancia inst = new Instancia(p.getUniqueId(), c, centro);
        ativas.put(p.getUniqueId(), inst);

        plugin.masmorras().guardarRetorno(p, p.getLocation());
        Location entrada = centro.clone().add(0, 0, -9);
        entrada.setDirection(new org.bukkit.util.Vector(0, 0, 1));
        p.teleport(entrada);
        p.playSound(entrada, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
        p.showTitle(Title.title(Component.text(c.simbolo() + " Prova: " + c.nome() + " " + c.simbolo(), c.cor(), TextDecoration.BOLD),
                Component.text(c.prova().descricao() + " (4 min)", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))));
        if (c.afinidades().contains(Skill.DOMA)) {
            // Domadores não estão sozinhos: dois lobos lutam com eles na prova.
            for (int i = 0; i < 2; i++) {
                Wolf lobo = w.spawn(entrada.clone().add(i * 2 - 1, 0, 1), Wolf.class, CreatureSpawnEvent.SpawnReason.CUSTOM, l -> {
                    l.setTamed(true);
                    l.setOwner(p);
                    l.setPersistent(false);
                });
                inst.aliados.add(lobo.getUniqueId());
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> comecar(inst), 60L);
        return null;
    }

    private void construirArena(Location centro) {
        World w = centro.getWorld();
        Tema t = Tema.sortear();
        int meio = (TAMANHO - 1) / 2;
        int cx = centro.getBlockX(), cz = centro.getBlockZ(), y0 = centro.getBlockY() - 1;
        for (int dx = -meio; dx <= meio; dx++) {
            for (int dz = -meio; dz <= meio; dz++) {
                w.getBlockAt(cx + dx, y0, cz + dz).setType(t.piso(), false);
                w.getBlockAt(cx + dx, y0 + ALTURA - 1, cz + dz).setType((dx % 6 == 0 && dz % 6 == 0) ? t.luz() : t.parede(), false);
                boolean borda = Math.abs(dx) == meio || Math.abs(dz) == meio;
                if (borda) for (int y = 1; y < ALTURA - 1; y++) w.getBlockAt(cx + dx, y0 + y, cz + dz).setType(t.parede(), false);
            }
        }
        for (int[] p : new int[][]{{6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) {
            for (int y = 1; y < ALTURA - 1; y++) w.getBlockAt(cx + p[0], y0 + y, cz + p[1]).setType(t.pilar(), false);
        }
        for (int i = -2; i <= 2; i++) {
            w.getBlockAt(cx + i, y0, cz).setType(t.destaque(), false);
            w.getBlockAt(cx, y0, cz + i).setType(t.destaque(), false);
        }
    }

    private void comecar(Instancia inst) {
        Player p = Bukkit.getPlayer(inst.jogador);
        if (p == null || ativas.get(inst.jogador) != inst) return;
        inst.comecou = true;
        switch (inst.classe.prova()) {
            case ONDAS -> onda(inst, p);
            case ALVOS -> {
                inst.alvosFaltando = inst.classe.avancada() ? 16 : 12;
                p.sendMessage(Component.text("➹ Derrote os alvos só com flechas: golpes de perto não contam!", NamedTextColor.YELLOW));
                alvos(inst);
            }
            case DUELO -> guardiao(inst, p);
        }
        p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 1f);
    }

    // ---------- ondas ----------

    private void onda(Instancia inst, Player p) {
        inst.onda++;
        int base = inst.classe.avancada() ? 6 : 4;
        int quantos = base + (inst.onda - 1) * 2;
        for (int i = 0; i < quantos; i++) inst.mobs.add(monstro(inst, sortear(inst), pontoNaBorda(inst)).getUniqueId());
        p.showTitle(Title.title(Component.empty(), Component.text("⚔ Onda " + inst.onda + "/3", NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(300))));
    }

    private EntityType sortear(Instancia inst) {
        List<EntityType> l = inst.classe.monstros();
        return l.isEmpty() ? EntityType.ZOMBIE : l.get(rnd().nextInt(l.size()));
    }

    /** Lugar perto das paredes, longe do jogador. */
    private Location pontoNaBorda(Instancia inst) {
        double ang = rnd().nextDouble() * Math.PI * 2, raio = rnd().nextDouble(8, 11);
        return inst.centro.clone().add(Math.cos(ang) * raio, 0, Math.sin(ang) * raio);
    }

    private LivingEntity monstro(Instancia inst, EntityType tipo, Location l) {
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        double vida = inst.classe.avancada() ? 2.0 : 1.3, dano = inst.classe.avancada() ? 1.5 : 1.2;
        LivingEntity e = l.getWorld().spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, m -> {
            multiplicar(m, Attribute.MAX_HEALTH, vida);
            multiplicar(m, Attribute.ATTACK_DAMAGE, dano);
            AttributeInstance v = m.getAttribute(Attribute.MAX_HEALTH);
            if (v != null) m.setHealth(v.getValue());
            m.setPersistent(false);
            m.setRemoveWhenFarAway(false);
        });
        l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        Player p = Bukkit.getPlayer(inst.jogador);
        if (p != null && e instanceof Mob m) m.setTarget(p);
        return e;
    }

    private static void multiplicar(LivingEntity e, Attribute a, double f) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * f);
    }

    // ---------- alvos ----------

    private void alvos(Instancia inst) {
        int vivos = inst.mobs.size();
        while (vivos < 4 && inst.alvosFaltando - vivos > 0) {
            inst.mobs.add(monstro(inst, sortear(inst), pontoNaBorda(inst)).getUniqueId());
            vivos++;
        }
    }

    // ---------- duelo ----------

    private void guardiao(Instancia inst, Player p) {
        Classe c = inst.classe;
        EntityType tipo = c.guardiao() == null ? EntityType.VINDICATOR : c.guardiao();
        double vida = c.avancada() ? 300 : 150;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        Location l = inst.centro.clone().add(0, 0, 8);
        LivingEntity g = l.getWorld().spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, m -> {
            AttributeInstance v = m.getAttribute(Attribute.MAX_HEALTH);
            if (v != null) v.setBaseValue(vida);
            m.setHealth(vida);
            multiplicar(m, Attribute.ATTACK_DAMAGE, c.avancada() ? 1.8 : 1.4);
            AttributeInstance kb = m.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.setBaseValue(0.6);
            m.customName(Component.text("⚔ Guardião do " + c.nome() + " ⚔", c.cor(), TextDecoration.BOLD));
            m.setCustomNameVisible(true);
            m.setPersistent(false);
            m.setRemoveWhenFarAway(false);
        });
        if (g instanceof IronGolem golem) golem.setPlayerCreated(false);
        if (g instanceof Mob m) m.setTarget(p);
        inst.guardiao = g.getUniqueId();
        inst.mobs.add(g.getUniqueId());
        inst.barra = BossBar.bossBar(Component.text("Guardião do " + c.nome(), c.cor()), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        p.showBossBar(inst.barra);
        l.getWorld().strikeLightningEffect(l);
    }

    // =====================================================================
    //  A cada 10 ticks
    // =====================================================================

    public void tick() {
        long agora = System.currentTimeMillis();
        for (Instancia inst : new ArrayList<>(ativas.values())) {
            Player p = Bukkit.getPlayer(inst.jogador);
            if (p == null || !p.isOnline() || !inst.centro.getWorld().equals(p.getWorld())) {
                falhar(inst, null);
                continue;
            }
            if (agora > inst.ate) {
                falhar(inst, "O tempo da prova acabou.");
                continue;
            }
            if (!inst.comecou) continue;
            // Ninguém sai da arena.
            if (p.getLocation().distanceSquared(inst.centro) > 16 * 16) p.teleport(inst.centro);
            int antes = inst.mobs.size();
            inst.mobs.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || e.isDead() || !e.isValid());
            int mortos = antes - inst.mobs.size();
            for (UUID id : inst.mobs) {
                if (Bukkit.getEntity(id) instanceof LivingEntity e && e.getLocation().distanceSquared(inst.centro) > 16 * 16) {
                    e.teleport(pontoNaBorda(inst));
                }
            }
            long seg = (inst.ate - agora) / 1000;
            switch (inst.classe.prova()) {
                case ONDAS -> {
                    p.sendActionBar(Component.text("⚔ Onda " + inst.onda + "/3 • monstros: " + inst.mobs.size()
                            + String.format(" • ⏱ %d:%02d", seg / 60, seg % 60), NamedTextColor.RED));
                    if (!inst.mobs.isEmpty()) break;
                    if (inst.onda >= 3) { vencer(inst, p); break; }
                    if (inst.proximaOnda == 0) inst.proximaOnda = agora + 2500;
                    else if (agora >= inst.proximaOnda) {
                        inst.proximaOnda = 0;
                        onda(inst, p);
                    }
                }
                case ALVOS -> {
                    inst.alvosFaltando -= mortos;
                    p.sendActionBar(Component.text("➹ Alvos restantes: " + Math.max(0, inst.alvosFaltando)
                            + String.format(" • ⏱ %d:%02d", seg / 60, seg % 60), NamedTextColor.YELLOW));
                    if (inst.alvosFaltando <= 0) vencer(inst, p);
                    else alvos(inst);
                }
                case DUELO -> {
                    Entity g = inst.guardiao == null ? null : Bukkit.getEntity(inst.guardiao);
                    if (!(g instanceof LivingEntity guarda) || guarda.isDead() || !guarda.isValid()) {
                        vencer(inst, p);
                        break;
                    }
                    AttributeInstance max = guarda.getAttribute(Attribute.MAX_HEALTH);
                    if (inst.barra != null && max != null) inst.barra.progress((float) Math.max(0, Math.min(1, guarda.getHealth() / max.getValue())));
                    if (guarda instanceof Mob m && m.getTarget() == null) m.setTarget(p);
                    p.sendActionBar(Component.text(String.format("⚔ Duelo • ⏱ %d:%02d", seg / 60, seg % 60), NamedTextColor.RED));
                }
            }
        }
    }

    private void vencer(Instancia inst, Player p) {
        encerrar(inst);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.showTitle(Title.title(Component.text("✔ PROVA CONCLUÍDA", NamedTextColor.GREEN, TextDecoration.BOLD), Component.empty(),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            plugin.masmorras().voltar(p);
            plugin.classes().conceder(p, inst.classe);
        }, 40L);
    }

    private void falhar(Instancia inst, String motivo) {
        encerrar(inst);
        esperar.put(inst.jogador, System.currentTimeMillis() + plugin.settings().claRecargaProvaMin * 60_000L);
        Player p = Bukkit.getPlayer(inst.jogador);
        if (p == null) return;
        if (!p.isDead()) plugin.masmorras().voltar(p);
        if (motivo != null) {
            p.sendMessage(Component.text("✖ " + motivo + " Você pode tentar de novo em " + plugin.settings().claRecargaProvaMin
                    + " min (as tarefas continuam feitas).", NamedTextColor.RED));
        }
    }

    private void encerrar(Instancia inst) {
        ativas.remove(inst.jogador);
        for (UUID id : inst.mobs) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        for (UUID id : inst.aliados) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        Player p = Bukkit.getPlayer(inst.jogador);
        if (p != null && inst.barra != null) p.hideBossBar(inst.barra);
        // Limpa o que sobrou na arena (lacaios, flechas, itens).
        for (Entity e : inst.centro.getWorld().getNearbyEntities(inst.centro, 16, 8, 16)) if (!(e instanceof Player)) e.remove();
    }

    public void encerrarTodas() {
        for (Instancia inst : new ArrayList<>(ativas.values())) falhar(inst, "As provas foram encerradas.");
        esperar.clear();
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    /** Morrer na prova não perde nada. */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Instancia inst = ativas.get(p.getUniqueId());
        if (inst == null) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        morreram.add(p.getUniqueId());
        falhar(inst, "Você caiu na prova.");
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!morreram.remove(p.getUniqueId())) return;
        e.setRespawnLocation(plugin.masmorras().ondeVolta(p));
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.masmorras().voltar(p));
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Instancia inst = ativas.get(e.getPlayer().getUniqueId());
        if (inst != null) falhar(inst, null);
    }

    /** Na prova de alvos, golpe de perto não conta. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        Instancia inst = ativas.get(p.getUniqueId());
        if (inst == null || inst.classe.prova() != Classe.Prova.ALVOS || !inst.mobs.contains(e.getEntity().getUniqueId())) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            e.setCancelled(true);
            p.sendActionBar(Component.text("➹ Só flechas contam nessa prova!", NamedTextColor.YELLOW));
        }
    }

    /** Ajuda: o material do ícone de uma prova. */
    public static Material icone(Classe.Prova p) {
        return switch (p) {
            case ONDAS -> Material.ZOMBIE_HEAD;
            case ALVOS -> Material.TARGET;
            case DUELO -> Material.WITHER_SKELETON_SKULL;
        };
    }
}
