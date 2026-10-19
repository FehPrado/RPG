package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lanterna de mão (2.65): segurando uma tocha, lanterna, pedra luminosa (e afins), o lugar onde você
 * está fica iluminado enquanto anda. A luz é só desenhada para quem está perto (bloco de luz falso):
 * o mundo não muda, nada fica para trás.
 */
final class LanternaMao implements Listener {

    private static final double ALCANCE = 48;

    /** Onde a luz de cada jogador está e quem a vê. */
    private record Luz(Location onde, int nivel, List<UUID> veem) {}

    private final RPGAtributos plugin;
    private final Map<UUID, Luz> luzes = new HashMap<>();

    LanternaMao(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static int nivel(ItemStack i) {
        if (i == null) return 0;
        return switch (i.getType()) {
            case TORCH, COPPER_TORCH -> 13;
            case LANTERN, GLOWSTONE, SEA_LANTERN, SHROOMLIGHT, JACK_O_LANTERN, BEACON, OCHRE_FROGLIGHT,
                 VERDANT_FROGLIGHT, PEARLESCENT_FROGLIGHT, COPPER_LANTERN -> 15;
            case SOUL_TORCH, SOUL_LANTERN -> 10;
            case REDSTONE_TORCH, GLOW_BERRIES, GLOW_INK_SAC -> 7;
            default -> 0;
        };
    }

    void tick() {
        if (!plugin.settings().utilLanterna) {
            if (!luzes.isEmpty()) apagarTodas();
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            int n = Math.max(nivel(p.getInventory().getItemInMainHand()), nivel(p.getInventory().getItemInOffHand()));
            Luz atual = luzes.get(p.getUniqueId());
            if (n == 0 || p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                if (atual != null) apagar(p.getUniqueId());
                continue;
            }
            Block b = p.getEyeLocation().getBlock();
            boolean naAgua = b.getType() == Material.WATER;
            if (!b.getType().isAir() && !naAgua) b = p.getLocation().getBlock();
            if (!b.getType().isAir() && b.getType() != Material.WATER) {
                if (atual != null) apagar(p.getUniqueId());
                continue;
            }
            Location onde = b.getLocation();
            if (atual != null && atual.onde().equals(onde) && atual.nivel() == n && Bukkit.getCurrentTick() % 20 != 0) continue;
            if (atual != null && !atual.onde().equals(onde)) apagar(p.getUniqueId());
            Light luz = (Light) Material.LIGHT.createBlockData();
            luz.setLevel(n);
            luz.setWaterlogged(b.getType() == Material.WATER);
            List<UUID> veem = new ArrayList<>();
            for (Player v : p.getWorld().getPlayers()) {
                if (v.getLocation().distanceSquared(onde) > ALCANCE * ALCANCE) continue;
                v.sendBlockChange(onde, luz);
                veem.add(v.getUniqueId());
            }
            luzes.put(p.getUniqueId(), new Luz(onde, n, veem));
        }
    }

    private void apagar(UUID id) {
        Luz l = luzes.remove(id);
        if (l == null || l.onde().getWorld() == null) return;
        BlockData real = l.onde().getBlock().getBlockData();
        for (UUID v : l.veem()) {
            Player p = Bukkit.getPlayer(v);
            if (p != null && p.getWorld().equals(l.onde().getWorld())) p.sendBlockChange(l.onde(), real);
        }
    }

    void apagarTodas() {
        for (UUID id : List.copyOf(luzes.keySet())) apagar(id);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        apagar(e.getPlayer().getUniqueId());
    }
}
