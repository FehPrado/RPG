package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocos criados por magia: somem sozinhos depois de um tempo, não dropam nada
 * (nem dão XP) e só aparecem onde o jogador poderia construir.
 */
public final class BlocosTemporarios implements Listener {

    private record Temp(Block bloco, BlockData original, Material colocado) {}

    private final RPGAtributos plugin;
    private final Map<Location, Temp> ativos = new HashMap<>();

    public BlocosTemporarios(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /**
     * Pergunta aos plugins de proteção (WorldGuard, GriefPrevention...) se o jogador
     * pode construir ali, simulando a colocação de um bloco.
     */
    public boolean podeConstruir(Player p, Block b) {
        BlockPlaceEvent teste = new BlockPlaceEvent(b, b.getState(), b.getRelative(BlockFace.DOWN),
                new ItemStack(Material.STONE), p, true, EquipmentSlot.HAND);
        simulando = true;
        try {
            plugin.getServer().getPluginManager().callEvent(teste);
        } finally {
            simulando = false;
        }
        return !teste.isCancelled() && teste.canBuild();
    }

    private static boolean simulando;

    /**
     * O BlockPlaceEvent atual é só um teste de proteção (nada foi colocado de verdade)?
     * Quem marca blocos ao colocar deve ignorar esse evento.
     */
    public static boolean simulando() {
        return simulando;
    }

    /** Dá para colocar um bloco temporário aqui sem destruir nada importante? */
    public static boolean livre(Block b) {
        if (b.isEmpty()) return true;
        if (b.getType() == Material.WATER) return true;
        return b.isReplaceable() && !(b.getState() instanceof TileState);
    }

    /** @return true se colocou. */
    public boolean colocar(Player p, Block b, BlockData nova, int ticks) {
        if (ativos.containsKey(b.getLocation()) || !livre(b) || !podeConstruir(p, b)) return false;
        Temp t = new Temp(b, b.getBlockData(), nova.getMaterial());
        b.setBlockData(nova, false);
        ativos.put(b.getLocation(), t);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> reverter(b.getLocation()), ticks);
        return true;
    }

    public boolean colocar(Player p, Block b, Material m, int ticks) {
        return colocar(p, b, m.createBlockData(), ticks);
    }

    /** Para habilidades de chefes: não há jogador para checar proteção (o bloco some sozinho). */
    public boolean colocarSemJogador(Block b, Material m, int ticks) {
        if (ativos.containsKey(b.getLocation()) || !livre(b)) return false;
        Temp t = new Temp(b, b.getBlockData(), m);
        b.setType(m, false);
        ativos.put(b.getLocation(), t);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> reverter(b.getLocation()), ticks);
        return true;
    }

    public boolean temporario(Block b) {
        return ativos.containsKey(b.getLocation());
    }

    private void reverter(Location l) {
        Temp t = ativos.remove(l);
        if (t == null) return;
        if (t.bloco().getType() == t.colocado()) t.bloco().setBlockData(t.original(), true);
    }

    public void reverterTodos() {
        for (Location l : new ArrayList<>(ativos.keySet())) reverter(l);
    }

    // Quebrar um bloco de magia: ele só some (sem drop, sem XP de Mineração).
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (!temporario(e.getBlock())) return;
        e.setCancelled(true);
        reverter(e.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        filtrar(e.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        filtrar(e.blockList());
    }

    private void filtrar(List<Block> blocos) {
        blocos.removeIf(b -> {
            if (!temporario(b)) return false;
            reverter(b.getLocation());
            return true;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (temporario(b)) { e.setCancelled(true); return; }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (temporario(b)) { e.setCancelled(true); return; }
    }
}
