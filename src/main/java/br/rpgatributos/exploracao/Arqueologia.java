package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Arqueologia: sítios com pedras antigas e blocos suspeitos aparecem pelo mundo (areia,
 * cascalho, terra). Com o pincel saem relíquias, mapas e outros achados. Cada relíquia
 * nova entra na sua coleção (Enciclopédia), e a coleção completa vale um título.
 */
public final class Arqueologia implements Listener {

    public static final TextColor COR = TextColor.color(0xBCAAA4);
    private static final NamespacedKey K_RELIQUIA = new NamespacedKey("rpgatributos", "reliquia");

    private final RPGAtributos plugin;
    private final NamespacedKey kChunk, kColecao;

    public Arqueologia(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kChunk = new NamespacedKey(plugin, "arqueologia_v1");
        this.kColecao = new NamespacedKey(plugin, "reliquias");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Relíquias
    // =====================================================================

    public static ItemStack criar(Reliquia r) {
        ItemStack i = new ItemStack(Material.QUARTZ);
        i.editMeta(m -> {
            m.itemName(Component.text(r.nome(), COR));
            m.setItemModel(NamespacedKey.minecraft(r.modelo()));
            m.setMaxStackSize(1);
            m.lore(List.of(
                    Component.text(r.linha1(), NamedTextColor.GRAY),
                    Component.text(r.linha2(), NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("✦ Relíquia do mundo (entra na sua coleção)", COR)).stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_RELIQUIA, PersistentDataType.STRING, r.id());
        });
        return i;
    }

    public static Reliquia de(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        return Reliquia.porId(i.getPersistentDataContainer().getOrDefault(K_RELIQUIA, PersistentDataType.STRING, ""));
    }

    public ItemStack reliquiaAleatoria() {
        int total = 0;
        for (Reliquia r : Reliquia.values()) total += r.peso();
        int x = rnd().nextInt(total);
        for (Reliquia r : Reliquia.values()) {
            x -= r.peso();
            if (x < 0) return criar(r);
        }
        return criar(Reliquia.MOEDA_DO_REI_CAIDO);
    }

    public Set<Reliquia> colecao(Player p) {
        Set<Reliquia> s = EnumSet.noneOf(Reliquia.class);
        for (String id : p.getPersistentDataContainer().getOrDefault(kColecao, PersistentDataType.STRING, "").split(",")) {
            Reliquia r = Reliquia.porId(id);
            if (r != null) s.add(r);
        }
        return s;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPegar(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Reliquia r = de(e.getItem().getItemStack());
        if (r == null) return;
        Set<Reliquia> s = colecao(p);
        if (!s.add(r)) return;
        StringBuilder sb = new StringBuilder();
        for (Reliquia x : s) sb.append(sb.isEmpty() ? "" : ",").append(x.id());
        p.getPersistentDataContainer().set(kColecao, PersistentDataType.STRING, sb.toString());
        plugin.titulos().definirMinimo(p, "reliquias", s.size());
        p.showTitle(Title.title(Component.text("✦ " + r.nome(), COR, TextDecoration.BOLD),
                Component.text("Nova relíquia na coleção (" + s.size() + "/" + Reliquia.values().length + ")", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        p.playSound(p.getLocation(), Sound.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE, 1f, 0.8f);
        p.sendMessage(Component.text("✦ " + r.nome() + ": " + r.linha1() + " " + r.linha2(), COR));
    }

    // =====================================================================
    //  Sítios
    // =====================================================================

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        Chunk c = e.getChunk();
        World w = c.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || plugin.masmorras().ehMundo(w)) return;
        var pdc = c.getPersistentDataContainer();
        if (pdc.has(kChunk)) return;
        pdc.set(kChunk, PersistentDataType.BYTE, (byte) 1);
        Random r = new Random(w.getSeed() ^ (c.getX() * 49632L) ^ (c.getZ() * 325176L) ^ 0xA2C3L);
        if (r.nextDouble() > 0.02) return;
        int x = c.getX() * 16 + 4 + r.nextInt(8), z = c.getZ() * 16 + 4 + r.nextInt(8);
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (c.isLoaded()) construir(new Location(w, x, 0, z), r); });
    }

    /** Monta um sítio: pedras antigas em volta e blocos suspeitos no chão. @return false se o chão não serve. */
    public boolean construir(Location onde, Random r) {
        World w = onde.getWorld();
        int x = onde.getBlockX(), z = onde.getBlockZ();
        Block topo = w.getHighestBlockAt(x, z);
        Material chao = topo.getType();
        Material suspeito;
        if (chao == Material.SAND || chao == Material.RED_SAND || chao == Material.SANDSTONE) suspeito = Material.SUSPICIOUS_SAND;
        else if (chao == Material.GRAVEL || chao == Material.DIRT || chao == Material.GRASS_BLOCK || chao == Material.COARSE_DIRT
                || chao == Material.PODZOL || chao == Material.MUD) suspeito = Material.SUSPICIOUS_GRAVEL;
        else return false;
        int y = topo.getY();
        // Pedras antigas em círculo.
        Material[] pedras = {Material.CRACKED_STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CHISELED_STONE_BRICKS, Material.MOSSY_COBBLESTONE};
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 + r.nextDouble(0.4);
            int px = x + (int) Math.round(Math.cos(a) * 3), pz = z + (int) Math.round(Math.sin(a) * 3);
            int py = w.getHighestBlockYAt(px, pz);
            int altura = 1 + r.nextInt(2);
            for (int k = 1; k <= altura; k++) w.getBlockAt(px, py + k, pz).setType(pedras[r.nextInt(pedras.length)], false);
        }
        // Blocos suspeitos (enterrados um bloco abaixo, alguns na superfície).
        int quantos = 3 + r.nextInt(3);
        boolean reliquia = false;
        for (int i = 0; i < quantos; i++) {
            int bx = x + r.nextInt(-2, 3), bz = z + r.nextInt(-2, 3);
            int by = w.getHighestBlockYAt(bx, bz) - (r.nextBoolean() ? 1 : 0);
            Block b = w.getBlockAt(bx, by, bz);
            if (!b.getType().isSolid() || b.getType() == Material.CHEST) continue;
            b.setType(suspeito, false);
            if (b.getState() instanceof BrushableBlock bb) {
                ItemStack achado;
                if (!reliquia || r.nextDouble() < 0.25) {
                    achado = reliquiaAleatoria();
                    reliquia = true;
                } else achado = achadoComum(r);
                bb.setItem(achado);
                bb.update(true, false);
            }
        }
        return true;
    }

    private ItemStack achadoComum(Random r) {
        double x = r.nextDouble();
        if (x < 0.12) return plugin.mapas().criar(r.nextDouble() < 0.2 ? MapasDoTesouro.Tipo.RARO : MapasDoTesouro.Tipo.COMUM, null);
        if (x < 0.22) return Raro.FRAGMENTO_DE_FORJA.criar(1);
        if (x < 0.40) return new ItemStack(Material.EMERALD, 2 + r.nextInt(4));
        if (x < 0.55) return new ItemStack(Material.GOLD_NUGGET, 4 + r.nextInt(8));
        if (x < 0.70) return new ItemStack(Material.BONE, 2 + r.nextInt(3));
        if (x < 0.85) return new ItemStack(Material.COAL, 3 + r.nextInt(4));
        return new ItemStack(Material.ARCHER_POTTERY_SHERD);
    }

    /** Admin: um sítio a alguns blocos à frente. */
    public boolean construirAdmin(Player p) {
        Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(6));
        return construir(l, new Random());
    }
}
