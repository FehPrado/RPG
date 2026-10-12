package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O Nian (年), a fera do fim do inverno. Nas noites de inverno ele pode descer das montanhas perto de
 * alguém. Arma nenhuma o derruba: ele só foge com medo, e o medo vem dos fogos de artifício
 * estourando nele, do fogo e do som dos chifres de cabra. Quem ajudar a espantá-lo ganha a
 * recompensa. Se a noite acabar antes, ele volta sozinho para as montanhas.
 */
public final class Nian implements Listener {

    private static final NamespacedKey K_NIAN = new NamespacedKey("rpgatributos", "nian");
    private static final TextColor COR = TextColor.color(0xD83A2E);
    private static final double CORAGEM = 100;
    /** O tick roda a cada 2 s; a chance da noite é dividida pelas tentativas (uma por minuto, 10 por noite). */
    private static final int CICLOS_POR_TENTATIVA = 30, TENTATIVAS_POR_NOITE = 10;

    private final RPGAtributos plugin;
    private int ciclo;
    private UUID nian;
    private double coragem;
    private BossBar barra;
    private final Set<UUID> veem = new HashSet<>(), espantaram = new HashSet<>();
    /** O dia (do mundo) em que ele já veio: uma vez por noite, no máximo. */
    private long diaQueVeio = -1;

    public Nian(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static boolean eh(Entity e) {
        return e != null && e.getPersistentDataContainer().has(K_NIAN);
    }

    // =====================================================================
    //  A cada 2 s
    // =====================================================================

    public void tick() {
        ciclo++;
        LivingEntity e = atual();
        if (e != null) {
            if (e.getWorld().isDayTime()) {
                partir(e, "☀ O dia chegou e o Nian voltou para as montanhas. Ninguém o espantou desta vez.");
                return;
            }
            atualizarBarra(e);
            return;
        }
        if (nian != null) fim(); // sumiu (chunk descarregado, comando)
        if (ciclo % CICLOS_POR_TENTATIVA != 0 || plugin.estacoes().atual() != Estacoes.Estacao.INVERNO) return;
        double chance = plugin.settings().perNianChance / TENTATIVAS_POR_NOITE;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        List<Player> jogadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(jogadores);
        for (Player p : jogadores) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL || w.isDayTime() || w.getFullTime() / 24000 == diaQueVeio) continue;
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.getLocation().getBlock().getLightFromSky() < 12) continue; // a céu aberto
            if (aparecer(p.getLocation()) != null) return;
        }
    }

    private LivingEntity atual() {
        return nian != null && Bukkit.getEntity(nian) instanceof LivingEntity e && e.isValid() ? e : null;
    }

    /** O Nian desce perto do lugar (sorteio ou admin). */
    public LivingEntity aparecer(Location perto) {
        LivingEntity velho = atual();
        if (velho != null) velho.remove();
        fim();
        Location onde = lugar(perto);
        if (onde == null) return null;
        World w = onde.getWorld();
        Ravager e = w.spawn(onde, Ravager.class, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, x -> {
            x.getPersistentDataContainer().set(K_NIAN, PersistentDataType.BYTE, (byte) 1);
            x.customName(Component.text("Nian 年", COR, TextDecoration.BOLD));
            x.setCustomNameVisible(true);
            x.setPersistent(false);
            x.setRemoveWhenFarAway(false);
            x.setCanJoinRaid(false);
            base(x, Attribute.MAX_HEALTH, 300);
            base(x, Attribute.SCALE, 1.4);
            base(x, Attribute.ATTACK_DAMAGE, 14);
            base(x, Attribute.KNOCKBACK_RESISTANCE, 1);
            x.setHealth(300);
        });
        nian = e.getUniqueId();
        coragem = CORAGEM;
        diaQueVeio = w.getFullTime() / 24000;
        barra = BossBar.bossBar(Component.text("Nian 年 — coragem", COR, TextDecoration.BOLD), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        w.playSound(onde, Sound.ENTITY_RAVAGER_ROAR, 3f, 0.6f);
        w.spawnParticle(Particle.LARGE_SMOKE, onde.clone().add(0, 1, 0), 60, 1.5, 1, 1.5, 0.05);
        for (Player p : w.getNearbyPlayers(onde, 96)) {
            p.sendMessage(Component.text("☾ O Nian desceu das montanhas! ", COR, TextDecoration.BOLD)
                    .append(Component.text("Arma nenhuma o derruba: espante-o com fogos de artifício, fogo e chifres de cabra.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.BOLD, false)));
        }
        return e;
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        var inst = e.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    private static Location lugar(Location perto) {
        World w = perto.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 16; i++) {
            double ang = r.nextDouble(Math.PI * 2), dist = 28 + r.nextDouble(12);
            int x = (int) Math.floor(perto.getX() + Math.cos(ang) * dist), z = (int) Math.floor(perto.getZ() + Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block chao = w.getBlockAt(x, y, z);
            if (!chao.getType().isSolid() || chao.isLiquid()) continue;
            boolean livre = true;
            for (int dy = 1; dy <= 4 && livre; dy++) livre = w.getBlockAt(x, y + dy, z).isPassable();
            if (livre) return new Location(w, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private void atualizarBarra(LivingEntity e) {
        barra.progress((float) Math.max(0, Math.min(1, coragem / CORAGEM)));
        Set<UUID> agora = new HashSet<>();
        for (Player p : e.getWorld().getNearbyPlayers(e.getLocation(), 64)) {
            agora.add(p.getUniqueId());
            if (veem.add(p.getUniqueId())) p.showBossBar(barra);
        }
        for (UUID id : new ArrayList<>(veem)) {
            if (agora.contains(id)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.hideBossBar(barra);
            veem.remove(id);
        }
        if (ciclo % 5 == 0) e.getWorld().playSound(e.getLocation(), Sound.ENTITY_RAVAGER_AMBIENT, 2f, 0.6f);
    }

    // =====================================================================
    //  O medo
    // =====================================================================

    private void assustar(LivingEntity e, double quanto, Player quem, String como) {
        if (quem != null) espantaram.add(quem.getUniqueId());
        coragem -= quanto;
        e.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 3, 0.5, 0.3, 0.5, 0);
        if (quem != null && como != null) quem.sendActionBar(Component.text(como + " (coragem do Nian: " + Math.max(0, Math.round(coragem)) + ")", COR));
        if (coragem <= 0) espantado(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageEvent e) {
        if (!eh(e.getEntity()) || !(e.getEntity() instanceof LivingEntity n)) return;
        Player quem = null;
        if (e instanceof EntityDamageByEntityEvent d) {
            if (d.getDamager() instanceof Firework) {
                e.setDamage(0); // o susto vem do estouro (aoEstourar)
                return;
            }
            if (d.getDamager() instanceof Player p) quem = p;
            else if (d.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player p) quem = p;
        }
        switch (e.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> {
                e.setDamage(0);
                assustar(n, 1.5, null, null);
                return;
            }
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> {
                e.setDamage(0);
                assustar(n, 8, quem, "💥 A explosão assusta o Nian!");
                return;
            }
            default -> { }
        }
        // Armas quase não ferem: ele não morre, só foge.
        e.setDamage(e.getDamage() * 0.2);
        if (quem != null) espantaram.add(quem.getUniqueId());
        if (n.getHealth() - e.getFinalDamage() <= 1) {
            e.setCancelled(true);
            assustar(n, 25, quem, "O Nian recua, ferido e com medo!");
        } else if (quem != null && ciclo % 3 == 0) {
            quem.sendActionBar(Component.text("A arma mal arranha o Nian. Use fogos, fogo e chifres de cabra!", NamedTextColor.GRAY));
        }
    }

    /** Fogos de artifício estourando perto dele (até 6 blocos), com ou sem estrela. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoEstourar(org.bukkit.event.entity.FireworkExplodeEvent e) {
        LivingEntity n = atual();
        Firework fw = e.getEntity();
        if (n == null || !fw.getWorld().equals(n.getWorld())) return;
        Location meio = n.getLocation().add(0, n.getHeight() / 2, 0);
        if (fw.getLocation().distanceSquared(meio) > 6 * 6) return;
        assustar(n, 15, fw.getShooter() instanceof Player p ? p : null, "💥 O estouro assusta o Nian!");
    }

    /** O som do chifre de cabra espanta (o próprio jogo cuida da espera entre um toque e outro). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoTocar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack i = e.getItem();
        LivingEntity n = atual();
        if (n == null || i == null || i.getType() != Material.GOAT_HORN || e.getPlayer().hasCooldown(i)) return;
        Player p = e.getPlayer();
        if (!p.getWorld().equals(n.getWorld()) || p.getLocation().distanceSquared(n.getLocation()) > 24 * 24) return;
        assustar(n, 8, p, "📯 O chifre ecoa e o Nian se encolhe!");
    }

    /** Se mesmo assim ele cair (comando), conta como espantado. */
    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        if (!eh(e.getEntity())) return;
        e.getDrops().clear();
        if (e.getEntity().getUniqueId().equals(nian)) fim();
    }

    private void espantado(LivingEntity e) {
        Location c = e.getLocation();
        World w = c.getWorld();
        w.spawnParticle(Particle.FIREWORK, c.clone().add(0, 1.5, 0), 120, 1.5, 1.5, 1.5, 0.15);
        w.playSound(c, Sound.ENTITY_RAVAGER_STUNNED, 2f, 0.6f);
        w.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2f, 1f);
        // O que ele deixou para trás na fuga.
        w.dropItemNaturally(c, Raro.ESSENCIA_PRIMORDIAL.criar(1));
        w.dropItemNaturally(c, new ItemStack(Material.GOLD_BLOCK, 2));
        w.dropItemNaturally(c, new ItemStack(Material.EMERALD, 12));
        w.dropItemNaturally(c, new ItemStack(Material.FIREWORK_ROCKET, 16));
        List<String> nomes = new ArrayList<>();
        for (UUID id : espantaram) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            nomes.add(p.getName());
            plugin.titulos().registrar(p, "nian_espantado", 1);
            plugin.diario().marco(p, "nian", "Espantou o Nian numa noite de inverno");
        }
        plugin.getServer().broadcast(Component.text("🎆 O Nian fugiu de volta para as montanhas! ", COR, TextDecoration.BOLD)
                .append(Component.text(nomes.isEmpty() ? "O inverno segue em paz." : "Espantado por " + String.join(", ", nomes) + ".", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, false)));
        e.remove();
        fim();
    }

    private void partir(LivingEntity e, String msg) {
        for (Player p : e.getWorld().getNearbyPlayers(e.getLocation(), 96)) p.sendMessage(Component.text(msg, COR));
        e.getWorld().spawnParticle(Particle.LARGE_SMOKE, e.getLocation().add(0, 1, 0), 60, 1.5, 1, 1.5, 0.05);
        e.remove();
        fim();
    }

    private void fim() {
        if (barra != null) {
            for (UUID id : veem) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.hideBossBar(barra);
            }
        }
        veem.clear();
        espantaram.clear();
        barra = null;
        nian = null;
    }

    /** Ao desligar o servidor: tira o Nian do mundo (ele não é salvo). */
    public void parar() {
        LivingEntity e = atual();
        if (e != null) e.remove();
        fim();
    }

    /** Admin: onde ele está. */
    public String detalhes() {
        LivingEntity e = atual();
        return e == null ? "O Nian não está no mundo agora." : String.format(java.util.Locale.ROOT, "Nian em %s %d %d %d, coragem %.0f",
                e.getWorld().getName(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ(), coragem);
    }
}
