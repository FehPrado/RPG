package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Regador (2.65): enche na água (20 regadas) e, clicando numa plantação, molha um quadrado de 3x3:
 * cada planta que ainda cresce tem 40% de chance de subir um estágio. Uma regada a cada meio segundo.
 */
final class Regador implements Listener {

    static final NamespacedKey K_AGUA = new NamespacedKey("rpgatributos", "regador_agua");
    static final int MAXIMO = 20;

    private final RPGAtributos plugin;
    private final Map<UUID, Long> espera = new HashMap<>();

    Regador(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Põe a água no regador e mostra no nome. */
    static ItemStack encher(ItemStack i, int agua) {
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(K_AGUA, PersistentDataType.INTEGER, agua);
            m.itemName(Component.text(Utilidades.Item.REGADOR.nome + " (" + agua + "/" + MAXIMO + ")", Utilidades.COR));
        });
        return i;
    }

    private static int agua(ItemStack i) {
        return i.getPersistentDataContainer().getOrDefault(K_AGUA, PersistentDataType.INTEGER, 0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !Utilidades.Item.REGADOR.e(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!plugin.settings().utilRegador) return;
        long agora = System.currentTimeMillis();
        if (espera.getOrDefault(p.getUniqueId(), 0L) > agora) return;
        espera.put(p.getUniqueId(), agora + 500);
        ItemStack mao = p.getInventory().getItemInMainHand();

        // Encher: mirando na água.
        RayTraceResult r = p.rayTraceBlocks(5, FluidCollisionMode.ALWAYS);
        Block alvo = r == null ? null : r.getHitBlock();
        if (alvo != null && (alvo.getType() == Material.WATER || alvo.getBlockData() instanceof Levelled && alvo.getType() == Material.WATER_CAULDRON)) {
            encher(mao, MAXIMO);
            p.playSound(p.getLocation(), Sound.ITEM_BUCKET_FILL, 0.8f, 1.3f);
            p.sendActionBar(Component.text("🚿 Regador cheio (" + MAXIMO + " regadas).", Utilidades.COR));
            return;
        }
        Block b = e.getClickedBlock();
        if (b == null) return;
        int agua = agua(mao);
        if (agua <= 0) {
            p.sendActionBar(Component.text("🚿 O regador está vazio: clique na água para encher.", NamedTextColor.GRAY));
            return;
        }
        if (!plugin.territorios().podeConstruir(p, b)) {
            p.sendActionBar(Component.text("🚿 Esta plantação é de um território que não é seu.", NamedTextColor.RED));
            return;
        }
        // A plantação clicada (ou a terra arada embaixo dela).
        Block centro = b.getType() == Material.FARMLAND ? b.getRelative(0, 1, 0) : b;
        int molhou = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block x = centro.getRelative(dx, dy, dz);
                    if (!(x.getBlockData() instanceof Ageable a) || a.getAge() >= a.getMaximumAge()) continue;
                    if (x.getType() == Material.FROSTED_ICE || x.getType() == Material.FIRE) continue;
                    molhou++;
                    if (ThreadLocalRandom.current().nextDouble() < 0.40) {
                        a.setAge(a.getAge() + 1);
                        x.setBlockData(a, true);
                        x.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, x.getLocation().add(0.5, 0.5, 0.5), 3, 0.25, 0.25, 0.25, 0);
                    }
                    x.getWorld().spawnParticle(Particle.SPLASH, x.getLocation().add(0.5, 0.8, 0.5), 6, 0.3, 0.1, 0.3, 0);
                }
            }
        }
        if (molhou == 0) {
            p.sendActionBar(Component.text("🚿 Não tem planta crescendo aqui.", NamedTextColor.GRAY));
            return;
        }
        encher(mao, agua - 1);
        p.playSound(b.getLocation(), Sound.WEATHER_RAIN, 0.4f, 1.8f);
    }
}
