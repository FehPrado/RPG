package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.colonia.Colonia;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sons do mundo (2.97), só som, de longe:
 * <ul>
 *   <li>Na lua cheia, em florestas, taigas, neve e montanhas, lobos uivam ao longe (e os lobos de verdade por perto também).</li>
 *   <li>De noite, nos pântanos e mangues, um coro de sapos.</li>
 *   <li>Ao amanhecer, o sino da prefeitura de cada colônia toca para quem está a até 64 blocos.</li>
 * </ul>
 */
public final class SonsDoMundo {

    private final RPGAtributos plugin;
    /** Colônias que já tocaram o sino hoje (dono + dia). */
    private final Set<String> sinos = new HashSet<>();

    public SonsDoMundo(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** A cada 10 s. */
    public void tick() {
        if (!plugin.settings().mundoSons) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL || p.getLocation().getBlock().getLightFromSky() < 10) continue;
            long t = w.getTime();
            boolean noite = t >= 13000 && t < 23000;
            String bioma = p.getLocation().getBlock().getBiome().getKey().getKey();
            if (noite && Ceu.fase(w) == 0 && rnd().nextDouble() < 0.25
                    && (bioma.contains("taiga") || bioma.contains("forest") || bioma.contains("snowy") || bioma.contains("grove")
                    || bioma.contains("slopes") || bioma.contains("peaks") || bioma.contains("windswept"))) {
                uivar(p);
            }
            if (noite && (bioma.contains("swamp") || bioma.contains("mangrove")) && rnd().nextDouble() < 0.4) {
                for (int i = 0; i < 3; i++) {
                    Location l = longe(p.getLocation(), 8, 20);
                    p.playSound(l, Sound.ENTITY_FROG_AMBIENT, SoundCategory.AMBIENT, 0.7f, (float) rnd().nextDouble(0.8, 1.2));
                }
            }
        }
        sinoDaManha();
    }

    private void uivar(Player p) {
        for (Entity e : p.getNearbyEntities(48, 16, 48)) {
            if (e instanceof Wolf lobo && !lobo.isTamed()) {
                lobo.getWorld().playSound(lobo.getLocation(), Sound.ENTITY_WOLF_WHINE, SoundCategory.NEUTRAL, 1.5f, 0.55f);
                return;
            }
        }
        Location l = longe(p.getLocation(), 40, 60);
        p.playSound(l, Sound.ENTITY_WOLF_WHINE, SoundCategory.AMBIENT, 0.8f, (float) rnd().nextDouble(0.5, 0.6));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> p.playSound(l, Sound.ENTITY_WOLF_WHINE, SoundCategory.AMBIENT, 0.6f, 0.5f), 12L);
    }

    private void sinoDaManha() {
        for (Colonia c : plugin.colonias().todas()) {
            Location pref = c.prefeitura();
            if (pref == null || !pref.isChunkLoaded()) continue;
            long t = pref.getWorld().getTime();
            if (t < 23400 || t > 23800) continue;
            if (!sinos.add(c.dono() + ":" + pref.getWorld().getFullTime() / 24000)) continue;
            pref.getWorld().playSound(pref.clone().add(0.5, 1, 0.5), Sound.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2f, 1f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    pref.getWorld().playSound(pref.clone().add(0.5, 1, 0.5), Sound.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2f, 1f), 30L);
        }
        if (sinos.size() > 200) sinos.clear();
    }

    private static Location longe(Location l, double min, double max) {
        double ang = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(min, max);
        return l.clone().add(Math.cos(ang) * d, rnd().nextDouble(-2, 6), Math.sin(ang) * d);
    }
}
