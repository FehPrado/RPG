package br.rpgatributos.mar;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.detalhes.BarcoGrande;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tempestade no mar (3.0): na trovoada, o mar aberto fica bravo para quem está de barco. Ondas
 * empurram o barco, raios caem por perto (de vez em quando, no próprio barco: o Barco Grande
 * perde casco) e destroços de outros navios boiam com o que sobrou da carga.
 */
public final class Tempestade {

    static final TextColor COR = TextColor.color(0x90A4AE);

    private final RPGAtributos plugin;

    Tempestade(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    void segundo(int ciclo) {
        if (!plugin.settings().marTempestade) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Set<UUID> vistos = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (!w.isThundering() || p.getGameMode() == GameMode.SPECTATOR) continue;
            Boat b = Oceano.barco(p);
            if (b == null || !vistos.add(b.getUniqueId()) || !Oceano.noMar(b.getLocation())) continue;
            Location l = b.getLocation();
            // Ondas: de 3 em 3 s, o mar joga o barco de lado.
            if (ciclo % 3 == 0) {
                double a = r.nextDouble() * Math.PI * 2;
                b.setVelocity(b.getVelocity().add(new Vector(Math.cos(a), 0.12, Math.sin(a)).multiply(0.32)));
                w.spawnParticle(Particle.SPLASH, l.clone().add(0, 0.5, 0), 40, 1.6, 0.3, 1.6, 0.4);
                w.playSound(l, Sound.WEATHER_RAIN_ABOVE, 1.2f, 0.6f);
                w.playSound(l, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 0.6f);
            }
            // Raios perto; de vez em quando, no barco.
            if (r.nextDouble() < 0.07) {
                double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 18;
                w.strikeLightningEffect(l.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            } else if (r.nextDouble() < 0.012) {
                w.strikeLightningEffect(l);
                for (Player o : BarcoGrande.eBarcoGrande(b) ? plugin.barcoGrande().aBordo(b) : java.util.List.of(p)) {
                    if (o.getGameMode() != GameMode.CREATIVE) o.damage(3);
                }
                if (BarcoGrande.eBarcoGrande(b)) plugin.barcoGrande().melhorias().danificar(b, 8, "Um raio atingiu o barco");
            }
            // Destroços boiando, com o que sobrou da carga de alguém.
            if (r.nextDouble() < 0.025) destrocos(w, l, p);
        }
    }

    private static void destrocos(World w, Location perto, Player p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble() * Math.PI * 2, d = 6 + r.nextDouble() * 6;
        Location l = perto.clone().add(Math.cos(a) * d, 0.6, Math.sin(a) * d);
        Double sup = Oceano.superficie(w, l.getBlockX(), l.getBlockZ(), 2);
        if (sup == null) return;
        l.setY(sup + 0.2);
        int n = 1 + r.nextInt(3);
        for (int i = 0; i < n; i++) {
            Item it = w.dropItem(l, carga(r));
            it.setVelocity(new Vector(r.nextDouble(-0.05, 0.05), 0.1, r.nextDouble(-0.05, 0.05)));
        }
        w.spawnParticle(Particle.BLOCK, l, 20, 0.6, 0.1, 0.6, Material.OAK_PLANKS.createBlockData());
        p.sendActionBar(Component.text("⛈ Destroços boiando por perto: sobrou alguma carga.", COR));
    }

    private static ItemStack carga(ThreadLocalRandom r) {
        int x = r.nextInt(100);
        if (x < 22) return new ItemStack(Material.OAK_PLANKS, 3 + r.nextInt(6));
        if (x < 38) return new ItemStack(Material.STRING, 2 + r.nextInt(4));
        if (x < 50) return new ItemStack(Material.PAPER, 2 + r.nextInt(4));
        if (x < 62) return new ItemStack(Material.WHEAT, 3 + r.nextInt(6));
        if (x < 72) return new ItemStack(Material.IRON_INGOT, 1 + r.nextInt(2));
        if (x < 80) return new ItemStack(Material.GOLD_NUGGET, 3 + r.nextInt(6));
        if (x < 88) return new ItemStack(Material.EMERALD, 1 + r.nextInt(2));
        if (x < 94) return new ItemStack(Material.EXPERIENCE_BOTTLE, 1 + r.nextInt(2));
        if (x < 98) return br.rpgatributos.estruturas.MarAberto.cartaNautica(1);
        return new ItemStack(Material.NAME_TAG);
    }
}
