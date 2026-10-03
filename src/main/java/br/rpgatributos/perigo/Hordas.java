package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.colonia.Colonia;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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
 * Ataques à colônia: em noites aleatórias, com alguém da colônia por perto, os moradores
 * avisam e uma horda chega 1 minuto depois. Mais forte conforme o nível da colônia.
 * Vencer dá saque no depósito e alegria; se a horda durar até o amanhecer, ela recua
 * e a colônia fica triste.
 */
public final class Hordas {

    private static final int AVISO_SEGUNDOS = 60;

    private static final class Horda {
        final Colonia colonia;
        int chegaEm;
        boolean chegou;
        /** Começada pelo admin: não recua com o dia. */
        boolean forcada;
        int total;
        final Set<UUID> monstros = new HashSet<>();
        final BossBar barra = BossBar.bossBar(Component.text("⚠ Ataque à colônia!", NamedTextColor.RED), 1, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);

        Horda(Colonia colonia, int chegaEm) {
            this.colonia = colonia;
            this.chegaEm = chegaEm;
        }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kHorda;
    private final Map<UUID, Horda> hordas = new HashMap<>();
    private final Map<String, Long> ultimaHora = new HashMap<>();
    private int segundos;

    public Hordas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kHorda = new NamespacedKey(plugin, "horda");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** Jogadores da colônia a até 64 blocos da Prefeitura. */
    private List<Player> defensores(Colonia c) {
        List<Player> l = new ArrayList<>();
        Location pref = c.prefeitura();
        if (pref == null) return l;
        for (Player p : pref.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && p.getLocation().distanceSquared(pref) < 64 * 64 && plugin.colonias().daColonia(p, c)) l.add(p);
        }
        return l;
    }

    /** Roda 1x por segundo. */
    public void tick() {
        segundos++;
        // Começo da noite em cada mundo: sorteia os ataques.
        for (World w : Bukkit.getWorlds()) {
            long hora = w.getTime();
            Long antes = ultimaHora.put(w.getName(), hora);
            if (antes != null && antes < 13000 && hora >= 13000) sortear(w);
        }
        for (Horda h : List.copyOf(hordas.values())) atualizar(h);
    }

    private void sortear(World w) {
        for (Colonia c : plugin.colonias().todas()) {
            if (hordas.containsKey(c.dono()) || c.cidadaos().size() < 2) continue;
            Location pref = c.prefeitura();
            if (pref == null || !pref.getWorld().equals(w) || !pref.isChunkLoaded() || defensores(c).isEmpty()) continue;
            if (rnd().nextDouble() < plugin.settings().perHordaChance) avisar(c);
        }
    }

    /** Começa um ataque (o aviso agora, a horda daqui a 1 minuto). */
    public boolean avisar(Colonia c) {
        return avisar(c, false);
    }

    public boolean avisar(Colonia c, boolean forcada) {
        if (hordas.containsKey(c.dono())) return false;
        Horda h = new Horda(c, segundos + AVISO_SEGUNDOS);
        h.forcada = forcada;
        hordas.put(c.dono(), h);
        Location pref = c.prefeitura();
        if (pref != null) pref.getWorld().playSound(pref, Sound.BLOCK_BELL_USE, 2f, 0.7f);
        for (Player p : defensores(c)) {
            p.showTitle(Title.title(Component.text("⚠ Algo se aproxima...", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("A colônia vai ser atacada em 1 minuto!", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 0.9f);
        }
        return true;
    }

    private void atualizar(Horda h) {
        Colonia c = h.colonia;
        Location pref = c.prefeitura();
        if (pref == null || !plugin.colonias().todas().contains(c)) { encerrar(h, false, true); return; }
        if (!h.chegou) {
            int falta = h.chegaEm - segundos;
            h.barra.name(Component.text("⚠ A horda chega em " + falta + "s", NamedTextColor.RED));
            h.barra.progress(Math.max(0, Math.min(1, falta / (float) AVISO_SEGUNDOS)));
            mostrar(h);
            if (falta <= 0) chegar(h);
            return;
        }
        h.monstros.removeIf(id -> { Entity e = Bukkit.getEntity(id); return e == null || !e.isValid() || e.isDead(); });
        if (h.monstros.isEmpty()) { encerrar(h, true, false); return; }
        long hora = pref.getWorld().getTime();
        if (!h.forcada && (hora > 23000 || hora < 12000)) { encerrar(h, false, false); return; }
        h.barra.name(Component.text("⚠ Ataque à colônia: " + h.monstros.size() + " inimigos", NamedTextColor.RED, TextDecoration.BOLD));
        h.barra.progress(Math.max(0, Math.min(1, h.monstros.size() / (float) h.total)));
        mostrar(h);
        // Monstros sem alvo vão até a Prefeitura.
        if (segundos % 3 == 0) {
            for (UUID id : h.monstros) {
                if (!(Bukkit.getEntity(id) instanceof Mob m) || m.getTarget() != null) continue;
                if (m.getLocation().distanceSquared(pref) > 6 * 6) m.getPathfinder().moveTo(pref, 1.1);
            }
        }
    }

    private void mostrar(Horda h) {
        Location pref = h.colonia.prefeitura();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (pref != null && p.getWorld().equals(pref.getWorld()) && p.getLocation().distanceSquared(pref) < 80 * 80) p.showBossBar(h.barra);
            else p.hideBossBar(h.barra);
        }
    }

    private void chegar(Horda h) {
        h.chegou = true;
        Colonia c = h.colonia;
        Location pref = c.prefeitura();
        World w = pref.getWorld();
        int nivel = c.nivel();
        int total = Math.min(22, 4 + 3 * nivel + defensores(c).size());
        int grupos = 2 + rnd().nextInt(2);
        double base = rnd().nextDouble(Math.PI * 2);
        List<EntityType> tipos = new ArrayList<>(List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER));
        if (nivel >= 2) tipos.addAll(List.of(EntityType.HUSK, EntityType.STRAY));
        if (nivel >= 3) tipos.addAll(List.of(EntityType.VINDICATOR, EntityType.PILLAGER));
        if (nivel >= 4) tipos.add(EntityType.WITCH);
        for (int i = 0; i < total; i++) {
            double a = base + (i % grupos) * (2 * Math.PI / grupos) + rnd().nextDouble(-0.3, 0.3);
            Location l = pref.clone().add(Math.cos(a) * rnd().nextDouble(24, 32), 0, Math.sin(a) * rnd().nextDouble(24, 32));
            l.setY(w.getHighestBlockYAt(l) + 1);
            if (!l.isChunkLoaded()) continue;
            LivingEntity m = invocar(h, l, tipos.get(rnd().nextInt(tipos.size())));
            if (i == 0) plugin.perigo().tornarElite(m, nivel >= 4 ? 2 : 1); // o capitão da horda
        }
        if (nivel >= 5) {
            Location l = pref.clone().add(Math.cos(base) * 30, 0, Math.sin(base) * 30);
            l.setY(w.getHighestBlockYAt(l) + 1);
            LivingEntity ravager = invocar(h, l, EntityType.RAVAGER);
            plugin.perigo().tornarElite(ravager, 2);
            LivingEntity cavaleiro = invocar(h, l, EntityType.PILLAGER);
            ravager.addPassenger(cavaleiro);
        }
        h.total = Math.max(1, h.monstros.size());
        w.playSound(pref, Sound.EVENT_RAID_HORN, 3f, 0.6f);
        for (Player p : defensores(c)) {
            p.showTitle(Title.title(Component.text("⚔ A horda chegou!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                    Component.text(h.total + " inimigos atacam a colônia", NamedTextColor.RED),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2000), Duration.ofMillis(500))));
        }
    }

    private LivingEntity invocar(Horda h, Location l, EntityType tipo) {
        Entity e = l.getWorld().spawnEntity(l, tipo, CreatureSpawnEvent.SpawnReason.CUSTOM);
        LivingEntity le = (LivingEntity) e;
        le.getPersistentDataContainer().set(kHorda, PersistentDataType.STRING, h.colonia.dono().toString());
        le.setRemoveWhenFarAway(false);
        plugin.perigo().envelhecer(le);
        if (le instanceof Zombie z) {
            z.setCanBreakDoors(false);
            z.setShouldBurnInDay(false);
        }
        if (le instanceof org.bukkit.entity.AbstractSkeleton s) s.setShouldBurnInDay(false);
        if (le instanceof Raider r) r.setCanJoinRaid(false);
        // Mira primeiro num morador.
        if (le instanceof Mob m) {
            Villager perto = null;
            double d = Double.MAX_VALUE;
            for (Colonia.Cidadao ci : h.colonia.cidadaos()) {
                if (Bukkit.getEntity(ci.entidade()) instanceof Villager v && v.getWorld().equals(l.getWorld())) {
                    double x = v.getLocation().distanceSquared(l);
                    if (x < d) { d = x; perto = v; }
                }
            }
            if (perto != null) m.setTarget(perto);
        }
        h.monstros.add(le.getUniqueId());
        l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
        return le;
    }

    private void encerrar(Horda h, boolean vitoria, boolean silencioso) {
        hordas.remove(h.colonia.dono());
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(h.barra);
        if (silencioso) return;
        Colonia c = h.colonia;
        List<Player> defensores = defensores(c);
        if (vitoria) {
            int nivel = c.nivel();
            List<ItemStack> premio = new ArrayList<>();
            premio.add(new ItemStack(Material.EMERALD, 5 + 3 * nivel + rnd().nextInt(5)));
            premio.add(new ItemStack(Material.IRON_INGOT, 4 + 2 * nivel));
            if (rnd().nextDouble() < 0.3 + 0.1 * nivel) premio.add(Raro.FRAGMENTO_DE_FORJA.criar(1));
            if (nivel >= 4 && rnd().nextDouble() < 0.3) premio.add(new ItemStack(Material.DIAMOND, 1 + rnd().nextInt(2)));
            for (ItemStack i : premio) plugin.colonias().depositar(c, i);
            plugin.colonias().alterarFelicidade(c, 15);
            for (Player p : defensores) {
                p.showTitle(Title.title(Component.text("✦ Colônia defendida! ✦", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text("O saque foi para o depósito", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                plugin.titulos().registrar(p, "hordas", 1);
            }
        } else {
            for (UUID id : h.monstros) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) {
                    e.getWorld().spawnParticle(Particle.LARGE_SMOKE, e.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                    e.remove();
                }
            }
            plugin.colonias().alterarFelicidade(c, -15);
            for (Player p : defensores) p.sendMessage(Component.text("☠ A horda recuou com o amanhecer. Os moradores estão abalados.", NamedTextColor.GRAY));
        }
    }

    /** Ao desligar: os monstros das hordas somem. */
    public void parar() {
        for (Horda h : List.copyOf(hordas.values())) {
            for (UUID id : h.monstros) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            encerrar(h, false, true);
        }
    }
}
