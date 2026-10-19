package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Chave de Construtor (2.65): gira o bloco clicado sem quebrar. Clique: a próxima direção (escadas,
 * troncos, alçapões, cabeças, placas, fornalhas...). Agachado: vira de cabeça para baixo (escada,
 * laje e alçapão). Portas, camas, baús duplos e pistões estendidos ficam como estão.
 */
final class ChaveConstrutor implements Listener {

    private static final List<BlockFace> ORDEM = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    private final RPGAtributos plugin;

    ChaveConstrutor(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || !Utilidades.Item.CHAVE.e(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (!plugin.settings().utilChave || b == null) return;
        if (!plugin.territorios().podeConstruir(p, b)) {
            p.sendActionBar(Component.text("🔧 Este bloco é de um território que não é seu.", NamedTextColor.RED));
            return;
        }
        BlockData d = b.getBlockData();
        if (d instanceof Door || d instanceof Bed || d instanceof PistonHead || (d instanceof Piston pi && pi.isExtended())
                || (d instanceof Chest c && c.getType() != Chest.Type.SINGLE)) {
            p.sendActionBar(Component.text("🔧 Esse bloco não gira com a chave.", NamedTextColor.GRAY));
            return;
        }
        boolean mudou = p.isSneaking() ? virar(d) : girar(d);
        if (!mudou) {
            p.sendActionBar(Component.text(p.isSneaking() ? "🔧 Esse bloco não vira." : "🔧 Esse bloco não gira.", NamedTextColor.GRAY));
            return;
        }
        b.setBlockData(d, true);
        b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 0.6f, 1.6f);
    }

    /** De cabeça para baixo: escada e alçapão (metade), laje (em cima/embaixo). */
    private static boolean virar(BlockData d) {
        if ((d instanceof Stairs || d instanceof TrapDoor) && d instanceof Bisected bi) {
            bi.setHalf(bi.getHalf() == Bisected.Half.TOP ? Bisected.Half.BOTTOM : Bisected.Half.TOP);
            return true;
        }
        if (d instanceof Slab s && s.getType() != Slab.Type.DOUBLE) {
            s.setType(s.getType() == Slab.Type.TOP ? Slab.Type.BOTTOM : Slab.Type.TOP);
            return true;
        }
        return false;
    }

    /** A próxima direção que o bloco aceita. */
    private static boolean girar(BlockData d) {
        if (d instanceof Orientable o) {
            List<Axis> eixos = new ArrayList<>(o.getAxes());
            if (eixos.size() < 2) return false;
            eixos.sort(null);
            o.setAxis(eixos.get((eixos.indexOf(o.getAxis()) + 1) % eixos.size()));
            return true;
        }
        if (d instanceof Directional di) {
            Set<BlockFace> aceitas = di.getFaces();
            List<BlockFace> l = new ArrayList<>();
            for (BlockFace f : ORDEM) if (aceitas.contains(f)) l.add(f);
            if (l.size() < 2) return false;
            di.setFacing(l.get((l.indexOf(di.getFacing()) + 1) % l.size()));
            return true;
        }
        if (d instanceof Rotatable r) {
            BlockFace[] voltas = {BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
                    BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST, BlockFace.SOUTH,
                    BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST, BlockFace.WEST,
                    BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST};
            int i = 0;
            for (int k = 0; k < voltas.length; k++) if (voltas[k] == r.getRotation()) i = k;
            r.setRotation(voltas[(i + 2) % voltas.length]);
            return true;
        }
        return false;
    }
}
