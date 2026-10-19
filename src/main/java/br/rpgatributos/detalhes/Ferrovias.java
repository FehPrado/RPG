package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Bell;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ferrovias melhores (2.62):
 * <ul>
 *   <li>Em trilho reto e plano, o carrinho com gente dentro acelera até o dobro da velocidade
 *       (e volta ao normal antes das curvas e subidas, para não descarrilar).</li>
 *   <li><b>Estação:</b> um sino ao lado do trilho toca quando um carrinho passa por ele.</li>
 * </ul>
 */
public final class Ferrovias implements Listener {

    private static final double NORMAL = 0.4, RAPIDO = 0.8;
    private static final int RETA = 8;
    private static final TextColor COR = TextColor.color(0xB0BEC5);

    private final RPGAtributos plugin;
    private final Map<UUID, Long> ultimoSino = new HashMap<>();

    public Ferrovias(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static boolean reto(Rail.Shape s) {
        return s == Rail.Shape.NORTH_SOUTH || s == Rail.Shape.EAST_WEST;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMover(VehicleMoveEvent e) {
        if (!plugin.settings().ferrovias || !(e.getVehicle() instanceof RideableMinecart m)) return;
        boolean comGente = false;
        for (Entity x : m.getPassengers()) if (x instanceof Player) comGente = true;
        if (!comGente) {
            if (m.getMaxSpeed() != NORMAL) m.setMaxSpeed(NORMAL);
            return;
        }
        Block trilho = m.getLocation().getBlock();
        if (!(trilho.getBlockData() instanceof Rail r)) return;
        Vector v = m.getVelocity().setY(0);
        double max = NORMAL;
        if (reto(r.getShape()) && v.lengthSquared() > 1e-4) {
            BlockFace dir = Math.abs(v.getX()) > Math.abs(v.getZ()) ? (v.getX() > 0 ? BlockFace.EAST : BlockFace.WEST)
                    : (v.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH);
            int retos = 0;
            Block b = trilho;
            for (int i = 0; i < RETA; i++) {
                b = b.getRelative(dir);
                if (!(b.getBlockData() instanceof Rail ra) || ra.getShape() != r.getShape()) break;
                retos++;
            }
            if (retos >= RETA) max = RAPIDO;
        }
        if (m.getMaxSpeed() != max) m.setMaxSpeed(max);
        estacao(m, trilho);
    }

    /** Sino colado no trilho (ou um bloco acima/abaixo dele): toca uma vez quando o carrinho passa. */
    private void estacao(Minecart m, Block trilho) {
        long agora = System.currentTimeMillis();
        if (agora - ultimoSino.getOrDefault(m.getUniqueId(), 0L) < 4000) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block s = trilho.getRelative(dx, dy, dz);
                    if (s.getType() != Material.BELL || !(s.getState(false) instanceof Bell sino)) continue;
                    ultimoSino.put(m.getUniqueId(), agora);
                    sino.ring();
                    for (Entity x : m.getPassengers()) if (x instanceof Player p) p.sendActionBar(Component.text("🔔 Estação", COR));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void aoSair(VehicleExitEvent e) {
        if (e.getVehicle() instanceof RideableMinecart m && m.getMaxSpeed() != NORMAL) m.setMaxSpeed(NORMAL);
        ultimoSino.remove(e.getVehicle().getUniqueId());
    }
}
