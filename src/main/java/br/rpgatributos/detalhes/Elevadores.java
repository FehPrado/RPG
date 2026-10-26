package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Elevadores (2.57): um bloco feito na bancada. Em cima de um, pular leva ao próximo elevador acima
 * na mesma coluna (até 16 blocos) e agachar leva ao de baixo. Precisa de 2 blocos livres em cima do
 * destino. As posições ficam guardadas no próprio chunk; quebrado, devolve o Elevador. Explosões não
 * o destroem e pistões não o empurram.
 */
public final class Elevadores implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "elevador");
    private static final TextColor COR = TextColor.color(0xB0BEC5);
    private static final int ALCANCE = 16;

    private final RPGAtributos plugin;
    private final NamespacedKey kPos;
    /** Chave do chunk → posições empacotadas dos elevadores. */
    private final Map<Long, Set<Integer>> marcados = new HashMap<>();

    public Elevadores(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kPos = new NamespacedKey(plugin, "elevadores_pos");
    }

    public static ItemStack item(int qtd) {
        ItemStack i = new ItemStack(Material.LODESTONE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Elevador", COR));
            m.lore(List.of(
                    Component.text("Ponha um acima do outro, na mesma coluna.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Pule para subir, agache para descer (até 16 blocos).", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    private static boolean eItem(ItemStack i) {
        return i != null && i.getType() == Material.LODESTONE && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "elevador"), item(2));
        r.shape(" I ", "IPI", " I ");
        r.setIngredient('I', Material.IRON_INGOT);
        r.setIngredient('P', Material.ENDER_PEARL);
        Bukkit.addRecipe(r);
    }

    /** Os chunks que já estavam carregados quando o plugin ligou. */
    public void iniciar() {
        for (World w : Bukkit.getWorlds()) for (Chunk c : w.getLoadedChunks()) carregar(c);
    }

    // =====================================================================
    //  Marcas no chunk
    // =====================================================================

    private static long chave(World w, int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL) ^ ((long) w.getName().hashCode() << 48);
    }

    private static int empacotar(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y + 2048) << 8);
    }

    private boolean eh(Block b) {
        if (b.getType() != Material.LODESTONE) return false;
        Set<Integer> s = marcados.get(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4));
        return s != null && s.contains(empacotar(b.getX(), b.getY(), b.getZ()));
    }

    private void salvar(Chunk c) {
        Set<Integer> s = marcados.get(chave(c.getWorld(), c.getX(), c.getZ()));
        if (s == null || s.isEmpty()) c.getPersistentDataContainer().remove(kPos);
        else c.getPersistentDataContainer().set(kPos, PersistentDataType.INTEGER_ARRAY, s.stream().mapToInt(Integer::intValue).toArray());
    }

    private void carregar(Chunk c) {
        int[] v = c.getPersistentDataContainer().get(kPos, PersistentDataType.INTEGER_ARRAY);
        if (v == null || v.length == 0) return;
        Set<Integer> s = new HashSet<>();
        for (int x : v) s.add(x);
        marcados.put(chave(c.getWorld(), c.getX(), c.getZ()), s);
    }

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        carregar(e.getChunk());
    }

    @EventHandler
    public void aoDescarregarChunk(ChunkUnloadEvent e) {
        marcados.remove(chave(e.getWorld(), e.getChunk().getX(), e.getChunk().getZ()));
    }

    // =====================================================================
    //  Pôr, quebrar, explosões e pistões
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (!eItem(e.getItemInHand())) return;
        Block b = e.getBlockPlaced();
        marcados.computeIfAbsent(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4), k -> new HashSet<>()).add(empacotar(b.getX(), b.getY(), b.getZ()));
        salvar(b.getChunk());
        e.getPlayer().sendActionBar(Component.text("⇅ Elevador: pule em cima para subir, agache para descer.", COR));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!eh(b)) return;
        tirar(b);
        e.setDropItems(false);
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), item(1));
    }

    private void tirar(Block b) {
        Set<Integer> s = marcados.get(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4));
        if (s != null) s.remove(empacotar(b.getX(), b.getY(), b.getZ()));
        salvar(b.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(this::eh);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(this::eh);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(this::eh)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(this::eh)) e.setCancelled(true);
    }

    // =====================================================================
    //  Subir e descer
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoPular(PlayerJumpEvent e) {
        if (!plugin.settings().confElevadores) return;
        Player p = e.getPlayer();
        Block pe = apoioDoPulo(e);
        if (pe == null || !eh(pe)) return;
        Block destino = procurar(pe, 1);
        if (destino == null) return;
        e.setCancelled(true);
        // O Paper ainda reposiciona o jogador ao cancelar o salto. Transporte no tick seguinte.
        Location origem = e.getFrom().clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || p.isDead() || p.isInsideVehicle() || p.isFlying()
                    || p.getWorld() != origem.getWorld() || p.getLocation().distanceSquared(origem) > 2.25) return;
            if (!eh(pe)) return;
            Block atual = procurar(pe, 1);
            if (atual != null) levar(p, atual);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void aoAgachar(PlayerToggleSneakEvent e) {
        if (!plugin.settings().confElevadores || !e.isSneaking()) return;
        Player p = e.getPlayer();
        Block pe = embaixo(p);
        if (pe == null) return;
        Block destino = procurar(pe, -1);
        if (destino != null) levar(p, destino);
    }

    /** O elevador em que o jogador está em pé (ou null). */
    private Block embaixo(Player p) {
        Block b = blocoSob(p, p.getLocation());
        return b != null && eh(b) ? b : null;
    }

    static Block apoioDoPulo(PlayerJumpEvent e) { return blocoSob(e.getPlayer(), e.getFrom()); }

    private static Block blocoSob(Player p, Location origem) {
        if (p.isInsideVehicle() || p.isFlying()) return null;
        return origem.clone().subtract(0, 0.2, 0).getBlock();
    }

    /** O próximo elevador na direção (1 = acima, -1 = abaixo) com 2 blocos livres em cima. */
    private Block procurar(Block de, int dir) {
        World w = de.getWorld();
        for (int d = 1; d <= ALCANCE; d++) {
            int y = de.getY() + d * dir;
            if (y < w.getMinHeight() || y >= w.getMaxHeight() - 2) return null;
            Block b = w.getBlockAt(de.getX(), y, de.getZ());
            if (!eh(b)) continue;
            if (b.getRelative(0, 1, 0).isPassable() && b.getRelative(0, 2, 0).isPassable()) return b;
        }
        return null;
    }

    private static void levar(Player p, Block destino) {
        Location l = p.getLocation();
        Location para = destino.getLocation().add(0.5, 1, 0.5);
        para.setYaw(l.getYaw());
        para.setPitch(l.getPitch());
        if (!p.teleport(para)) return;
        p.setVelocity(new org.bukkit.util.Vector());
        p.setFallDistance(0);
        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, para.clone().add(0, 0.5, 0), 12, 0.3, 0.4, 0.3, 0.02);
        p.getWorld().playSound(para, Sound.BLOCK_PISTON_EXTEND, 0.5f, 1.6f);
    }
}
