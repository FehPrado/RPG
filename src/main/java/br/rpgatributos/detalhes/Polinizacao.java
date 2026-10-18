package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Bee;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abelhas polinizam (Vanilla+, 2.56): uma colmeia ou ninho com abelhas a até 8 blocos da plantação
 * faz ela crescer mais rápido (às vezes cresce um estágio a mais), com um pouco de pólen no ar.
 * As colmeias de cada chunk ficam guardadas por um minuto para não procurar a cada crescimento.
 */
public final class Polinizacao implements Listener {

    private static final double RAIO = 8, CHANCE = 0.4;
    private static final long VALIDADE = 60_000L;

    private record Colmeias(long quando, List<Location> lugares) { }

    private final RPGAtributos plugin;
    private final Map<Long, Colmeias> cache = new HashMap<>();

    public Polinizacao(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static boolean plantacao(Material m) {
        return switch (m) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, MELON_STEM, PUMPKIN_STEM, SWEET_BERRY_BUSH, TORCHFLOWER_CROP, PITCHER_CROP -> true;
            default -> false;
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCrescer(BlockGrowEvent e) {
        if (!plugin.settings().vanillaAbelhas) return;
        Block b = e.getBlock();
        if (!plantacao(b.getType()) || !(e.getNewState().getBlockData() instanceof Ageable)) return;
        if (ThreadLocalRandom.current().nextDouble() >= CHANCE || !temAbelhas(b)) return;
        // Um estágio a mais, no tick seguinte (depois que o crescimento normal entrou).
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(b.getBlockData() instanceof Ageable a) || a.getAge() >= a.getMaximumAge()) return;
            a.setAge(a.getAge() + 1);
            b.setBlockData(a, true);
            b.getWorld().spawnParticle(Particle.FALLING_NECTAR, b.getLocation().add(0.5, 0.8, 0.5), 4, 0.25, 0.2, 0.25, 0);
            b.getWorld().spawnParticle(Particle.WAX_ON, b.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0);
        });
    }

    /** Há colmeia/ninho com abelhas (dentro ou voando perto) a até 8 blocos? */
    private boolean temAbelhas(Block b) {
        World w = b.getWorld();
        Location c = b.getLocation();
        for (int cx = (b.getX() - 8) >> 4; cx <= (b.getX() + 8) >> 4; cx++) {
            for (int cz = (b.getZ() - 8) >> 4; cz <= (b.getZ() + 8) >> 4; cz++) {
                if (!w.isChunkLoaded(cx, cz)) continue;
                for (Location l : colmeias(w.getChunkAt(cx, cz))) {
                    if (l.distanceSquared(c) > RAIO * RAIO) continue;
                    if (l.getBlock().getState(false) instanceof Beehive h && h.getEntityCount() > 0) return true;
                    for (var x : w.getNearbyEntities(l, 6, 4, 6, en -> en instanceof Bee)) return true;
                }
            }
        }
        return false;
    }

    private List<Location> colmeias(Chunk ch) {
        long chave = ((long) ch.getX() << 32) ^ (ch.getZ() & 0xffffffffL) ^ ((long) ch.getWorld().getName().hashCode() << 48);
        long agora = System.currentTimeMillis();
        Colmeias c = cache.get(chave);
        if (c != null && agora - c.quando() < VALIDADE) return c.lugares();
        List<Location> l = new ArrayList<>();
        for (BlockState st : ch.getTileEntities(bl -> bl.getType() == Material.BEEHIVE || bl.getType() == Material.BEE_NEST, false)) {
            l.add(st.getLocation());
        }
        if (cache.size() > 4096) cache.clear();
        cache.put(chave, new Colmeias(agora, l));
        return l;
    }
}
