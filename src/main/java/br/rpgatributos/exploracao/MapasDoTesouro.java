package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.masmorra.Dificuldade;
import br.rpgatributos.masmorra.Tesouro;
import br.rpgatributos.perigo.Perigo;
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
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mapas do Tesouro: um mapa de verdade com um X no centro. Chegando perto do X, o baú
 * aparece enterrado embaixo dele, com guardiões em volta. Caem de Elites, da pesca, dos
 * baús das masmorras, dos sítios de arqueologia e da campanha do Cronista.
 */
public final class MapasDoTesouro implements Listener {

    public static final TextColor COR = TextColor.color(0xD4A017);
    private static final NamespacedKey K_MUNDO = new NamespacedKey("rpgatributos", "tesouro_mundo");
    private static final NamespacedKey K_X = new NamespacedKey("rpgatributos", "tesouro_x");
    private static final NamespacedKey K_Z = new NamespacedKey("rpgatributos", "tesouro_z");
    private static final NamespacedKey K_TIPO = new NamespacedKey("rpgatributos", "tesouro_tipo");
    private static final NamespacedKey K_GUARDIAO = new NamespacedKey("rpgatributos", "tesouro_guardiao");

    public enum Tipo {
        COMUM("Comum", 0xC8A165, 300, 800, Dificuldade.FACIL, 2),
        RARO("Raro", 0x55AAFF, 500, 1300, Dificuldade.NORMAL, 3),
        LENDARIO("Lendário", 0xFFAA00, 900, 2200, Dificuldade.DIFICIL, 4);

        private final String nome;
        private final TextColor cor;
        private final int min, max, guardioes;
        private final Dificuldade dificuldade;

        Tipo(String nome, int cor, int min, int max, Dificuldade d, int guardioes) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.min = min;
            this.max = max;
            this.dificuldade = d;
            this.guardioes = guardioes;
        }

        public String nome() { return nome; }
        public String id() { return name().toLowerCase(Locale.ROOT); }

        public static Tipo porId(String s) {
            for (Tipo t : values()) if (t.id().equalsIgnoreCase(s)) return t;
            return null;
        }
    }

    /** Desenha o X no centro do mapa. */
    private static final class Marca extends MapRenderer {
        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            var cursores = canvas.getCursors();
            for (int i = 0; i < cursores.size(); i++) if (cursores.getCursor(i).getType() == MapCursor.Type.RED_X) return;
            cursores.addCursor(new MapCursor((byte) 0, (byte) 0, (byte) 0, MapCursor.Type.RED_X, true, Component.text("Tesouro")));
        }
    }

    private final RPGAtributos plugin;
    private final Set<Integer> comMarca = new HashSet<>();

    public MapasDoTesouro(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** Cria um mapa com um tesouro longe de {@code perto} (no mundo principal, se for outro mundo). */
    public ItemStack criar(Tipo t, Location perto) {
        World w = perto != null && perto.getWorld().getEnvironment() == World.Environment.NORMAL && !plugin.masmorras().ehMundo(perto.getWorld())
                ? perto.getWorld() : Bukkit.getWorlds().getFirst();
        Location origem = perto != null && perto.getWorld().equals(w) ? perto : w.getSpawnLocation();
        int x = 0, z = 0;
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            double a = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(t.min, t.max);
            x = (int) (origem.getX() + Math.cos(a) * d);
            z = (int) (origem.getZ() + Math.sin(a) * d);
            if (plugin.territorios().em(new Location(w, x, 64, z)) == null) break;
        }
        MapView view = Bukkit.createMap(w);
        view.setCenterX(x);
        view.setCenterZ(z);
        view.setScale(MapView.Scale.NORMAL);
        view.setTrackingPosition(true);
        view.setUnlimitedTracking(true);
        view.addRenderer(new Marca());
        comMarca.add(view.getId());
        ItemStack mapa = new ItemStack(Material.FILLED_MAP);
        final int fx = x, fz = z;
        mapa.editMeta(MapMeta.class, m -> {
            m.setMapView(view);
            m.itemName(Component.text("Mapa do Tesouro (" + t.nome() + ")", t.cor));
            m.lore(List.of(
                    Component.text("Siga até o X no centro do mapa.", NamedTextColor.GRAY),
                    Component.text("Chegando perto, o baú aparece enterrado", NamedTextColor.GRAY),
                    Component.text("embaixo dele... e não estará sozinho.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("✕ Tesouro " + t.nome(), t.cor)).stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(K_MUNDO, PersistentDataType.STRING, w.getName());
            pdc.set(K_X, PersistentDataType.INTEGER, fx);
            pdc.set(K_Z, PersistentDataType.INTEGER, fz);
            pdc.set(K_TIPO, PersistentDataType.STRING, t.id());
        });
        return mapa;
    }

    private static boolean ehMapa(ItemStack i) {
        return i != null && i.getType() == Material.FILLED_MAP && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(K_X);
    }

    /** Tipo de tesouro sorteado para uma dificuldade de masmorra. */
    public static Tipo tipoPara(Dificuldade d) {
        return switch (d) {
            case FACIL -> Tipo.COMUM;
            case NORMAL -> rnd().nextDouble() < 0.3 ? Tipo.RARO : Tipo.COMUM;
            case DIFICIL -> rnd().nextDouble() < 0.3 ? Tipo.LENDARIO : Tipo.RARO;
            case PESADELO -> Tipo.LENDARIO;
        };
    }

    // =====================================================================
    //  Seguir o mapa
    // =====================================================================

    /** A cada segundo: o X volta ao mapa depois de reiniciar e o tesouro aparece para quem chega. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack[] itens = p.getInventory().getContents();
            for (int slot = 0; slot < itens.length; slot++) {
                ItemStack i = itens[slot];
                if (!ehMapa(i)) continue;
                MapMeta m = (MapMeta) i.getItemMeta();
                MapView view = m.getMapView();
                if (view != null && comMarca.add(view.getId())) view.addRenderer(new Marca());
                PersistentDataContainer pdc = m.getPersistentDataContainer();
                World w = Bukkit.getWorld(pdc.getOrDefault(K_MUNDO, PersistentDataType.STRING, ""));
                if (w == null || !w.equals(p.getWorld())) continue;
                int x = pdc.getOrDefault(K_X, PersistentDataType.INTEGER, 0), z = pdc.getOrDefault(K_Z, PersistentDataType.INTEGER, 0);
                double dx = p.getLocation().getX() - x, dz = p.getLocation().getZ() - z;
                if (dx * dx + dz * dz > 14 * 14) continue;
                Tipo t = Tipo.porId(pdc.getOrDefault(K_TIPO, PersistentDataType.STRING, "comum"));
                p.getInventory().setItem(slot, null);
                revelar(p, w, x, z, t == null ? Tipo.COMUM : t);
                break;
            }
        }
    }

    private void revelar(Player p, World w, int x, int z, Tipo t) {
        int topo = w.getHighestBlockYAt(x, z);
        Block b = w.getBlockAt(x, Math.max(w.getMinHeight() + 1, topo - 2), z);
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest bau) {
            Random r = new Random();
            Tesouro.encher(plugin, bau.getBlockInventory(), t.dificuldade, 5 + t.ordinal() * 3, true, r);
            bau.getBlockInventory().addItem(new ItemStack(Material.EMERALD, 8 + t.ordinal() * 8 + r.nextInt(8)));
            if (r.nextDouble() < 0.5) bau.getBlockInventory().addItem(plugin.arqueologia().reliquiaAleatoria());
            if (t == Tipo.LENDARIO) bau.getBlockInventory().addItem(Mitrilo.lingote(1));
        }
        Location marca = new Location(w, x + 0.5, topo + 1, z + 0.5);
        for (int i = 0; i < t.guardioes; i++) {
            double a = Math.PI * 2 * i / t.guardioes;
            Location l = marca.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5);
            l.setY(w.getHighestBlockYAt(l) + 1);
            EntityType tipo = switch (t) {
                case COMUM -> i % 2 == 0 ? EntityType.ZOMBIE : EntityType.SKELETON;
                case RARO -> i % 2 == 0 ? EntityType.VINDICATOR : EntityType.PILLAGER;
                case LENDARIO -> i == 0 ? EntityType.EVOKER : EntityType.WITHER_SKELETON;
            };
            LivingEntity g = (LivingEntity) w.spawnEntity(l, tipo, CreatureSpawnEvent.SpawnReason.CUSTOM);
            g.setPersistent(false);
            g.getPersistentDataContainer().set(K_GUARDIAO, PersistentDataType.BYTE, (byte) 1);
            g.customName(Component.text("Guardião do Tesouro", NamedTextColor.GOLD));
            plugin.perigo().tornarElite(g, t.ordinal() + 1);
            if (g instanceof org.bukkit.entity.Mob mob) mob.setTarget(p);
        }
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, marca, 60, 0.4, 1.5, 0.4, 0.2);
        w.strikeLightningEffect(marca);
        w.playSound(marca, Sound.EVENT_RAID_HORN, 1.5f, 1.2f);
        p.showTitle(Title.title(Component.text("✕ O tesouro está aqui! ✕", COR, TextDecoration.BOLD),
                Component.text("Cave embaixo do X... e cuidado com os guardiões", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(700))));
        plugin.titulos().registrar(p, "tesouros_enterrados", 1);
    }

    // =====================================================================
    //  De onde vêm os mapas
    // =====================================================================

    private void dar(Player p, Tipo t, String de) {
        p.getInventory().addItem(criar(t, p.getLocation())).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.sendMessage(Component.text("✕ Você achou um Mapa do Tesouro (" + t.nome() + ") " + de + "!", COR));
        p.playSound(p.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatarElite(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || !Perigo.ehElite(e.getEntity()) || e.getEntity().getPersistentDataContainer().has(K_GUARDIAO)) return;
        if (rnd().nextDouble() < 0.04) dar(p, Perigo.afixos(e.getEntity()).size() >= 2 ? Tipo.RARO : Tipo.COMUM, "com o Elite");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH && rnd().nextDouble() < 0.012) {
            dar(e.getPlayer(), rnd().nextDouble() < 0.2 ? Tipo.RARO : Tipo.COMUM, "numa garrafa no anzol");
        }
    }
}
