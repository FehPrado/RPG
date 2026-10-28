package br.rpgatributos.mar;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.estruturas.MarAberto;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O Navio Fantasma (3.0): em noite de lua nova (ou de neblina), surge no mar perto de quem
 * navega, com marujos fantasmas e o Capitão Fantasma. Ao amanhecer ele some sem deixar rastro: o
 * plugin guarda o que havia em cada bloco (em mar.yml, para sumir mesmo se o servidor reiniciar).
 */
public final class NavioFantasma {

    static final NamespacedKey K = new NamespacedKey("rpgatributos", "navio_fantasma");
    static final TextColor COR = TextColor.color(0xB0BEC5);
    private static final int[][] MARUJOS = {{1, 3, 3}, {-1, 3, 3}, {1, 3, -1}, {-1, 3, -1}, {0, 3, 5}};
    private static final int[] CAPITAO = {0, 3, -2};

    private final RPGAtributos plugin;
    private final File arquivo;
    private String mundo;
    private int x, y, z, rot;
    private long fim;
    private List<String> antes = new ArrayList<>();
    private final List<UUID> tripulacao = new ArrayList<>();
    /** Quando o mar pode trazer o próximo navio sozinho. */
    long proximoNatural;

    NavioFantasma(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "mar.yml");
    }

    boolean ativo() {
        return mundo != null;
    }

    // =====================================================================
    //  Arquivo (o navio que ficou no mundo quando o servidor parou)
    // =====================================================================

    void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        if (!y.contains("fantasma.mundo")) return;
        mundo = y.getString("fantasma.mundo");
        x = y.getInt("fantasma.x");
        this.y = y.getInt("fantasma.y");
        z = y.getInt("fantasma.z");
        rot = y.getInt("fantasma.rot");
        fim = y.getLong("fantasma.fim");
        antes = new ArrayList<>(y.getStringList("fantasma.antes"));
        // O navio não sobrevive a um reinício: some assim que os mundos carregam.
        Bukkit.getScheduler().runTaskLater(plugin, () -> sumir(false), 60L);
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        if (ativo()) {
            y.set("fantasma.mundo", mundo);
            y.set("fantasma.x", x);
            y.set("fantasma.y", this.y);
            y.set("fantasma.z", z);
            y.set("fantasma.rot", rot);
            y.set("fantasma.fim", fim);
            y.set("fantasma.antes", antes);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar mar.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Surgir e sumir
    // =====================================================================

    /** O navio surge a uns 40 blocos à frente do jogador. @return erro ou null. */
    String surgir(Player p) {
        if (ativo()) return "Já tem um navio fantasma no mar.";
        World w = p.getWorld();
        Location l = p.getLocation();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double base = Math.toRadians(l.getYaw() + 90);
        for (int tent = 0; tent < 8; tent++) {
            double a = base + (tent == 0 ? 0 : r.nextDouble(-1.2, 1.2));
            int cx = (int) Math.floor(l.getX() + Math.cos(a) * 40), cz = (int) Math.floor(l.getZ() + Math.sin(a) * 40);
            if (!livre(w, cx, cz)) continue;
            Double sup = Oceano.superficie(w, cx, cz, 5);
            if (sup == null) continue;
            mundo = w.getName();
            x = cx;
            y = (int) Math.floor(sup) - 1;
            z = cz;
            rot = r.nextInt(4);
            fim = System.currentTimeMillis() + 20 * 60_000L;
            antes = plugin.estruturas().marAberto().erguerNavioFantasma(w, x, y, z, rot, r.nextLong());
            salvar();
            tripular(w);
            Location centro = new Location(w, x + 0.5, y + 4, z + 0.5);
            w.playSound(centro, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 3f, 0.6f);
            w.playSound(centro, Sound.BLOCK_BELL_RESONATE, 3f, 0.4f);
            w.spawnParticle(Particle.SOUL, centro, 200, 5, 4, 8, 0.03, null, true);
            for (Player o : w.getPlayers()) {
                if (o.getLocation().distanceSquared(centro) > 90 * 90) continue;
                o.showTitle(Title.title(Component.text("☠ O Navio Fantasma", COR, TextDecoration.BOLD),
                        Component.text("Surgiu da névoa. Some quando o sol nascer.", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3000), Duration.ofMillis(900))));
            }
            return null;
        }
        return "Não achei mar livre e fundo (5 blocos) a uns 40 blocos à sua frente.";
    }

    /** Não ergue o navio em cima de obra (território) nem de estrutura. */
    private boolean livre(World w, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (plugin.territorios().em(w.getName(), (cx >> 4) + dx, (cz >> 4) + dz) != null) return false;
            }
        }
        // Toda a área do navio tem de ser mar aberto: nada de estrutura, ilha, píer ou barco no caminho.
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                if (Oceano.superficie(w, cx + dx, cz + dz, 3) == null) return false;
            }
        }
        Location c = new Location(w, cx + 0.5, 64, cz + 0.5);
        // No mar de teste as estruturas ficam juntas: lá basta não encostar.
        if (plugin.estruturas().temEstruturaPerto(c, br.rpgatributos.ModoTeste.ehTeste(w) ? 16 : 24)) return false;
        c.setY(Oceano.superficie(w, cx, cz, 3));
        return w.getNearbyEntitiesByType(org.bukkit.entity.Boat.class, c, 11, 8, 11).isEmpty();
    }

    private void tripular(World w) {
        tripulacao.clear();
        for (int[] m : MARUJOS) {
            Location l = MarAberto.pontoNavio(w, x, y, z, rot, m[0], m[1], m[2]);
            Stray s = w.spawn(l, Stray.class, e -> {
                e.customName(Component.text("Marujo Fantasma", COR));
                fantasma(e, "marujo");
            });
            tripulacao.add(s.getUniqueId());
        }
        Location l = MarAberto.pontoNavio(w, x, y, z, rot, CAPITAO[0], CAPITAO[1], CAPITAO[2]);
        WitherSkeleton c = w.spawn(l, WitherSkeleton.class, e -> {
            e.customName(Component.text("Capitão Fantasma", COR, TextDecoration.BOLD));
            e.setCustomNameVisible(true);
            e.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            e.getEquipment().setItemInMainHandDropChance(0f);
            fantasma(e, "capitao");
            var v = e.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (v != null) {
                v.setBaseValue(200);
                e.setHealth(200);
            }
        });
        tripulacao.add(c.getUniqueId());
    }

    private static void fantasma(Mob e, String papel) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(false);
        e.setGlowing(true);
        e.setCanPickupItems(false);
        e.getPersistentDataContainer().set(K, PersistentDataType.STRING, papel);
    }

    /** A cada segundo: névoa de almas e, ao amanhecer (ou no fim do tempo), o navio some. */
    void segundo(int ciclo) {
        if (!ativo()) return;
        World w = Bukkit.getWorld(mundo);
        if (w == null) {
            mundo = null;
            salvar();
            return;
        }
        long t = w.getTime();
        boolean dia = t < 12500 || t > 23500;
        if (dia || System.currentTimeMillis() > fim) {
            sumir(true);
            return;
        }
        if (ciclo % 2 == 0) {
            Location c = new Location(w, x + 0.5, y + 5, z + 0.5);
            boolean alguem = false;
            for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(c) <= 100 * 100) alguem = true;
            if (alguem) {
                w.spawnParticle(Particle.SOUL, c, 30, 4, 3, 7, 0.02, null, true);
                w.spawnParticle(Particle.WHITE_ASH, c, 60, 6, 4, 9, 0, null, true);
            }
        }
    }

    /** O navio afunda na névoa: tripulação some e cada bloco volta a ser o que era. */
    void sumir(boolean avisar) {
        if (!ativo()) return;
        World w = Bukkit.getWorld(mundo);
        for (UUID u : tripulacao) {
            Entity e = Bukkit.getEntity(u);
            if (e != null) e.remove();
        }
        tripulacao.clear();
        if (w != null) {
            for (int cx = (x - 9) >> 4; cx <= (x + 9) >> 4; cx++) for (int cz = (z - 9) >> 4; cz <= (z + 9) >> 4; cz++) w.getChunkAt(cx, cz);
            Location c = new Location(w, x + 0.5, y + 4, z + 0.5);
            for (Entity e : w.getNearbyEntities(c, 12, 12, 12)) if (e.getPersistentDataContainer().has(K)) e.remove();
            MarAberto.desfazer(w, antes);
            if (avisar) {
                w.spawnParticle(Particle.SOUL, c, 150, 5, 4, 8, 0.05, null, true);
                w.playSound(c, Sound.AMBIENT_CAVE, 3f, 0.5f);
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(c) <= 100 * 100) p.sendMessage(Component.text("☠ O Navio Fantasma se desfez na névoa.", COR));
                }
            }
        }
        mundo = null;
        antes = new ArrayList<>();
        salvar();
    }

    void aoMorrer(EntityDeathEvent e) {
        String papel = e.getEntity().getPersistentDataContainer().get(K, PersistentDataType.STRING);
        if (papel == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (!papel.equals("capitao")) {
            if (r.nextDouble() < 0.3) e.getDrops().add(new ItemStack(Material.GOLD_NUGGET, 2 + r.nextInt(4)));
            return;
        }
        e.getDrops().add(new ItemStack(Material.GOLD_INGOT, 5 + r.nextInt(6)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 6 + r.nextInt(8)));
        e.getDrops().add(new ItemStack(Material.ECHO_SHARD, 1 + r.nextInt(2)));
        if (r.nextDouble() < 0.5) e.getDrops().add(Raro.MAPA_RASGADO.criar(1));
        if (r.nextDouble() < 0.25) e.getDrops().add(Raro.PAGINA_DE_LENDA.criar(1));
        e.setDroppedExp(120);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "capitao_fantasma", "Derrotou o Capitão Fantasma");
            plugin.titulos().registrar(quem, "capitao_fantasma", 1);
            Bukkit.broadcast(Component.text("☠ ", COR).append(Component.text(quem.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" derrotou o Capitão Fantasma!", NamedTextColor.GRAY)));
        }
    }

    static boolean sobra(Entity e) {
        return e.getPersistentDataContainer().has(K);
    }

    boolean conhecido(Entity e) {
        return tripulacao.contains(e.getUniqueId());
    }
}
