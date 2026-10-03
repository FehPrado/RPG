package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temperatura leve, só nos extremos: frio (nevasca, biomas gelados à noite, inverno à noite)
 * e calor (deserto/savana/terras áridas de dia no verão, e o Nether). Roupa de couro, fogueira
 * perto ou resistência ao fogo protegem do frio; resistência ao fogo ou água protegem do calor.
 */
public final class Temperatura {

    private final RPGAtributos plugin;
    private final Map<UUID, Integer> frio = new HashMap<>();

    public Temperatura(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Roda a cada 2 segundos. */
    public void tick() {
        if (!plugin.settings().temperaturaAtiva) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL || p.isDead()) continue;
            if (comFrio(p)) {
                int s = frio.merge(p.getUniqueId(), 2, Integer::sum);
                if (s >= 20) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false, true));
                    p.setFreezeTicks(Math.min(p.getMaxFreezeTicks() - 5, p.getFreezeTicks() + 30));
                    if (s % 10 == 0) p.sendActionBar(Component.text("❄ Você está com frio! (couro, fogueira ou resistência ao fogo)", NamedTextColor.AQUA));
                }
            } else {
                frio.remove(p.getUniqueId());
                if (comCalor(p)) {
                    p.setExhaustion(p.getExhaustion() + 0.4f);
                    if (p.getTicksLived() % 200 < 40) p.sendActionBar(Component.text("☀ Calor! Você cansa e fica com fome mais rápido.", NamedTextColor.GOLD));
                }
            }
        }
    }

    private boolean comFrio(Player p) {
        World w = p.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return false;
        Block b = p.getLocation().getBlock();
        if (b.getLightFromSky() < 14) return false; // dentro de casa ou em caverna
        boolean noite = Ceu.noite(w);
        boolean nevasca = plugin.ceu().clima() == Ceu.Clima.NEVASCA;
        boolean gelado = b.getTemperature() < 0.15;
        boolean inverno = plugin.estacoes().atual() == Estacoes.Estacao.INVERNO;
        if (!nevasca && !(noite && (gelado || inverno))) return false;
        return pecasDeCouro(p) < 2 && !fogoPerto(p.getLocation());
    }

    private boolean comCalor(Player p) {
        World w = p.getWorld();
        if (p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) || p.isInWater()) return false;
        if (w.getEnvironment() == World.Environment.NETHER) return true;
        if (w.getEnvironment() != World.Environment.NORMAL || Ceu.noite(w) || w.hasStorm()) return false;
        Block b = p.getLocation().getBlock();
        if (b.getLightFromSky() < 14 || plugin.estacoes().atual() != Estacoes.Estacao.VERAO) return false;
        String bioma = w.getBiome(b.getX(), b.getY(), b.getZ()).getKey().getKey();
        return bioma.contains("desert") || bioma.contains("badlands") || bioma.contains("savanna");
    }

    private static int pecasDeCouro(Player p) {
        int n = 0;
        for (ItemStack i : p.getInventory().getArmorContents()) if (i != null && i.getType().name().startsWith("LEATHER_")) n++;
        return n;
    }

    private static boolean fogoPerto(Location l) {
        for (int x = -3; x <= 3; x++) for (int y = -1; y <= 2; y++) for (int z = -3; z <= 3; z++) {
            Material m = l.getBlock().getRelative(x, y, z).getType();
            if (m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE || m == Material.FIRE || m == Material.LAVA
                    || m == Material.MAGMA_BLOCK || m == Material.FURNACE || m == Material.BLAST_FURNACE || m == Material.SMOKER) return true;
        }
        return false;
    }
}
