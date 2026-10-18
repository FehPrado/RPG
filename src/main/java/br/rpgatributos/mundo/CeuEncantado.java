package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O céu encantado (2.58):
 * <ul>
 *   <li><b>Arco-íris:</b> quando a chuva para de dia, às vezes um arco-íris aparece na frente de quem
 *       está ao ar livre. Onde ele toca o chão fica o Pote do Arco-Íris por 3 minutos (quem pegar
 *       primeiro leva).</li>
 *   <li><b>Estrelas cadentes:</b> em noites limpas, de vez em quando uma estrela risca o céu. Quem
 *       agachar olhando para o céu em até 6 s faz um pedido e ganha uma bênção até o amanhecer (um
 *       pedido por noite).</li>
 * </ul>
 */
public final class CeuEncantado implements Listener {

    public static final NamespacedKey K_POTE = new NamespacedKey("rpgatributos", "pote_arco_iris");
    private static final TextColor COR = TextColor.color(0xFFD54F);
    private static final Color[] CORES = {Color.fromRGB(0xE53935), Color.fromRGB(0xFB8C00), Color.fromRGB(0xFDD835),
            Color.fromRGB(0x43A047), Color.fromRGB(0x1E88E5), Color.fromRGB(0x3949AB), Color.fromRGB(0x8E24AA)};

    private record Arco(Location centro, Vector lado, Vector frente, long ate) { }

    private record Bencao(PotionEffectType tipo, int nivel, String nome) { }

    private static final List<Bencao> BENCAOS = List.of(
            new Bencao(PotionEffectType.LUCK, 0, "Sorte"),
            new Bencao(PotionEffectType.SPEED, 0, "Velocidade"),
            new Bencao(PotionEffectType.REGENERATION, 0, "Regeneração"),
            new Bencao(PotionEffectType.NIGHT_VISION, 0, "Olhos de Coruja (visão noturna)"),
            new Bencao(PotionEffectType.RESISTANCE, 0, "Proteção das Estrelas (resistência)"),
            new Bencao(PotionEffectType.HASTE, 0, "Mãos Ligeiras (pressa)"));

    private final RPGAtributos plugin;
    private final List<Arco> arcos = new ArrayList<>();
    /** Jogador → até quando pode fazer o pedido (ms). */
    private final Map<UUID, Long> janela = new HashMap<>();
    /** Jogador → a noite (número do dia do mundo) em que já pediu. */
    private final Map<UUID, Long> pediu = new HashMap<>();
    private int ticks;

    public CeuEncantado(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static boolean aoArLivre(Player p) {
        Location l = p.getLocation();
        return l.getWorld().getHighestBlockYAt(l, HeightMap.MOTION_BLOCKING_NO_LEAVES) <= l.getBlockY() + 1;
    }

    // =====================================================================
    //  Arco-íris
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMudarTempo(WeatherChangeEvent e) {
        World w = e.getWorld();
        if (e.toWeatherState() || !plugin.settings().ceuArcoIris || w.getEnvironment() != World.Environment.NORMAL
                || plugin.masmorras().ehMundo(w)) return;
        long hora = w.getTime();
        if (hora > 11500) return; // só de dia
        if (rnd().nextDouble() >= plugin.settings().ceuArcoIrisChance) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : w.getPlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || !aoArLivre(p)) continue;
                boolean perto = false;
                for (Arco a : arcos) if (a.centro().getWorld().equals(w) && a.centro().distanceSquared(p.getLocation()) < 120 * 120) perto = true;
                if (!perto) arcoIris(p);
            }
        }, 40L);
    }

    /** Um arco-íris na frente do jogador (40 blocos), de lado a lado, com o pote na ponta direita. */
    public void arcoIris(Player p) {
        Vector frente = p.getLocation().getDirection().setY(0);
        if (frente.lengthSquared() < 1e-3) frente = new Vector(0, 0, 1);
        frente.normalize().rotateAroundY(Math.toRadians(rnd().nextDouble(-35, 35)));
        Vector lado = new Vector(-frente.getZ(), 0, frente.getX());
        World w = p.getWorld();
        Location centro = p.getLocation().add(frente.clone().multiply(40));
        centro.setY(w.getHighestBlockYAt(centro, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1);
        arcos.add(new Arco(centro, lado, frente, System.currentTimeMillis() + 180_000));
        // O pote: onde a ponta direita do arco encosta no chão.
        Location ponta = centro.clone().add(lado.clone().multiply(25));
        ponta.setY(w.getHighestBlockYAt(ponta, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1.2);
        ItemStack pote = new ItemStack(Material.DECORATED_POT);
        pote.editMeta(m -> {
            m.itemName(Component.text("Pote do Arco-Íris", COR));
            m.getPersistentDataContainer().set(K_POTE, PersistentDataType.BYTE, (byte) 1);
        });
        Item item = w.dropItem(ponta, pote, it -> {
            it.setVelocity(new Vector());
            it.setGlowing(true);
            it.setTicksLived(6000 - 3600); // some em 3 minutos
            it.customName(Component.text("✦ Pote do Arco-Íris", COR));
            it.setCustomNameVisible(true);
        });
        item.setPickupDelay(10);
        p.showTitle(Title.title(Component.text("🌈", COR), Component.text("Um arco-íris! Dizem que há um pote onde ele toca o chão.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
    }

    private void desenharArcos() {
        long agora = System.currentTimeMillis();
        arcos.removeIf(a -> agora > a.ate());
        for (Arco a : arcos) {
            World w = a.centro().getWorld();
            List<Player> veem = new ArrayList<>();
            for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(a.centro()) < 220 * 220) veem.add(p);
            if (veem.isEmpty()) continue;
            for (int faixa = 0; faixa < CORES.length; faixa++) {
                double r = 28 - faixa * 0.9;
                Particle.DustOptions tinta = new Particle.DustOptions(CORES[faixa], 4f);
                for (int i = 0; i <= 44; i++) {
                    double ang = Math.PI * i / 44;
                    Location pt = a.centro().clone().add(a.lado().clone().multiply(Math.cos(ang) * r)).add(0, Math.sin(ang) * r, 0);
                    new ParticleBuilder(Particle.DUST).location(pt).data(tinta).count(1).receivers(veem).force(true).spawn();
                }
            }
            // Brilho em cima do pote.
            Location ponta = a.centro().clone().add(a.lado().clone().multiply(25));
            ponta.setY(w.getHighestBlockYAt(ponta, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1.5);
            new ParticleBuilder(Particle.END_ROD).location(ponta).offset(0.2, 1.5, 0.2).count(4).extra(0.01).receivers(veem).force(true).spawn();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPegarPote(EntityPickupItemEvent e) {
        Item it = e.getItem();
        if (!it.getItemStack().getPersistentDataContainer().has(K_POTE)) return;
        e.setCancelled(true);
        if (!(e.getEntity() instanceof Player p)) return;
        it.remove();
        ThreadLocalRandom r = rnd();
        List<ItemStack> premio = new ArrayList<>();
        premio.add(new ItemStack(Material.EMERALD, 4 + r.nextInt(7)));
        premio.add(new ItemStack(Material.GOLD_INGOT, 3 + r.nextInt(4)));
        if (r.nextDouble() < 0.35) premio.add(br.rpgatributos.vida.Album.pacote(1));
        if (r.nextDouble() < 0.25) premio.add(new Raro[]{Raro.AMBAR, Raro.JADE, Raro.PEROLA}[r.nextInt(3)].criar(1));
        if (r.nextDouble() < 0.12) premio.add(new ItemStack(Material.DIAMOND));
        for (ItemStack i : premio) for (ItemStack sobra : p.getInventory().addItem(i).values()) p.getWorld().dropItem(p.getLocation(), sobra);
        p.showTitle(Title.title(Component.text("🌈 O Pote do Arco-Íris!", COR, TextDecoration.BOLD), Component.text("Era verdade.", NamedTextColor.GRAY)));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, it.getLocation(), 30, 0.3, 0.5, 0.3, 0.3);
        plugin.titulos().registrar(p, "arco_iris", 1);
        plugin.diario().marco(p, "arco_iris", "Achou o Pote do Arco-Íris");
    }

    // =====================================================================
    //  Estrelas cadentes
    // =====================================================================

    /** A cada tick do servidor. */
    public void tick() {
        ticks++;
        if (ticks % 10 == 0 && !arcos.isEmpty()) desenharArcos();
        if (ticks % 20 != 0 || !plugin.settings().ceuEstrelas) return;
        World w = Bukkit.getWorlds().getFirst();
        long hora = w.getTime();
        if (hora < 13500 || hora > 22500 || w.hasStorm()) return;
        if (rnd().nextDouble() >= plugin.settings().ceuEstrelasPorMinuto / 60.0) return;
        estrelaCadente(w);
    }

    public void estrelaCadente(World w) {
        List<Player> ceu = new ArrayList<>();
        for (Player p : w.getPlayers()) if (p.getGameMode() != GameMode.SPECTATOR && aoArLivre(p)) ceu.add(p);
        if (ceu.isEmpty()) return;
        long ate = System.currentTimeMillis() + 6000;
        for (Player p : ceu) {
            // Cada um vê a sua: alta, uns 60 blocos à frente, riscando de lado.
            Vector frente = p.getLocation().getDirection().setY(0);
            if (frente.lengthSquared() < 1e-3) frente = new Vector(1, 0, 0);
            frente.normalize().rotateAroundY(Math.toRadians(rnd().nextDouble(-50, 50)));
            Vector lado = new Vector(-frente.getZ(), 0, frente.getX()).multiply(rnd().nextBoolean() ? 1 : -1);
            Location inicio = p.getLocation().add(frente.clone().multiply(60)).add(lado.clone().multiply(-25)).add(0, 55, 0);
            Vector passo = lado.clone().multiply(1.6).add(new Vector(0, -0.55, 0));
            int[] n = {0};
            Bukkit.getScheduler().runTaskTimer(plugin, t -> {
                if (n[0]++ >= 28 || !p.isOnline()) { t.cancel(); return; }
                Location pt = inicio.clone().add(passo.clone().multiply(n[0]));
                new ParticleBuilder(Particle.END_ROD).location(pt).count(3).offset(0.1, 0.1, 0.1).extra(0).receivers(p).force(true).spawn();
                new ParticleBuilder(Particle.FIREWORK).location(pt.clone().subtract(passo.clone().multiply(0.5))).count(2).offset(0.2, 0.2, 0.2)
                        .extra(0.01).receivers(p).force(true).spawn();
            }, 0L, 1L);
            if (pediu.getOrDefault(p.getUniqueId(), -1L) == noite(w)) continue;
            janela.put(p.getUniqueId(), ate);
            p.sendActionBar(Component.text("✦ Uma estrela cadente! Agache olhando para o céu e faça um pedido.", TextColor.color(0xE1F5FE)));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.8f);
        }
    }

    private static long noite(World w) {
        return (w.getFullTime() + 12000) / 24000; // muda ao meio-dia: a noite inteira tem um número só
    }

    @EventHandler(ignoreCancelled = true)
    public void aoAgachar(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        Long ate = janela.get(p.getUniqueId());
        if (ate == null || System.currentTimeMillis() > ate) return;
        if (p.getLocation().getPitch() > -25) {
            p.sendActionBar(Component.text("✦ Olhe para o céu enquanto agacha.", NamedTextColor.GRAY));
            return;
        }
        janela.remove(p.getUniqueId());
        World w = p.getWorld();
        pediu.put(p.getUniqueId(), noite(w));
        Bencao b = BENCAOS.get(rnd().nextInt(BENCAOS.size()));
        long hora = w.getTime();
        int ateAmanhecer = (int) Math.max(1200, (hora <= 23000 ? 23000 - hora : 24000 - hora + 23000));
        p.addPotionEffect(new PotionEffect(b.tipo(), ateAmanhecer, b.nivel(), true, false, true));
        p.showTitle(Title.title(Component.text("✦", TextColor.color(0xE1F5FE)),
                Component.text("Seu pedido foi ouvido: " + b.nome() + " até o amanhecer.", TextColor.color(0xE1F5FE)),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.6f);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 2, 0), 20, 0.4, 0.6, 0.4, 0.02);
        plugin.titulos().registrar(p, "pedidos_estrela", 1);
    }
}
