package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As vilas do próprio jogo com mais vida (2.62):
 * <ul>
 *   <li><b>Nomes:</b> cada aldeão de vila ganha um nome (aparece quando você olha para ele).</li>
 *   <li><b>O sino reúne:</b> tocar o sino da vila chama os aldeões em volta para a praça.</li>
 *   <li><b>Dia de feira:</b> a cada 3 dias, de dia, as trocas ficam 20% mais baratas na vila, há festa
 *       em volta do sino e às vezes um mercador ambulante chega.</li>
 * </ul>
 * Não mexe nos moradores das vilas do plugin, da colônia nem em aldeão com nome.
 */
public final class VilasVanilla implements Listener {

    private static final TextColor COR = TextColor.color(0x8BC34A);
    private static final String[] NOMES = {"Ana", "Bento", "Cida", "Dona Rosa", "Eliseu", "Fátima", "Gerson", "Helena", "Ivo", "Joana", "Lauro",
            "Marta", "Nestor", "Olga", "Pedro", "Quitéria", "Raul", "Sônia", "Tadeu", "Úrsula", "Valter", "Zélia", "Benedita", "Chico", "Damião",
            "Iracema", "Josefa", "Mané", "Nina", "Severino", "Tonico", "Zé"};

    private final RPGAtributos plugin;
    private final NamespacedKey kNomeado;
    /** Sinos de vila vistos (praças) → último dia de feira anunciado por jogador. */
    private final Map<Location, Long> sinos = new HashMap<>();
    private final Map<String, Long> avisado = new HashMap<>();
    private final Set<Location> mercadorVeio = new HashSet<>();
    private int segundos;

    public VilasVanilla(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kNomeado = new NamespacedKey(plugin, "aldeao_nomeado");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** Aldeão do jogo (não é morador de vila do plugin nem da colônia, nem tem nome dado por alguém). */
    private boolean daVila(Villager v) {
        var pdc = v.getPersistentDataContainer();
        for (NamespacedKey k : pdc.getKeys()) {
            if (k.getNamespace().equals("rpgatributos") && !k.equals(kNomeado)) return false;
        }
        return v.customName() == null || pdc.has(kNomeado);
    }

    // =====================================================================
    //  Nomes
    // =====================================================================

    @EventHandler
    public void aoCarregar(EntitiesLoadEvent e) {
        if (!plugin.settings().vilasVanilla) return;
        for (Entity x : e.getEntities()) if (x instanceof Villager v) nomear(v);
    }

    private void nomear(Villager v) {
        if (v.isAdult() == false || !daVila(v) || v.getPersistentDataContainer().has(kNomeado)) return;
        String nome = NOMES[Math.floorMod(v.getUniqueId().hashCode(), NOMES.length)];
        v.customName(Component.text(nome));
        v.setCustomNameVisible(false); // aparece só quando alguém olha para ele
        v.getPersistentDataContainer().set(kNomeado, PersistentDataType.BYTE, (byte) 1);
    }

    // =====================================================================
    //  O sino da vila
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoTocar(BellRingEvent e) {
        if (!plugin.settings().vilasVanilla || !(e.getEntity() instanceof Player p)) return;
        Block sino = e.getBlock();
        Location praca = sino.getLocation().add(0.5, 0, 0.5);
        int vieram = 0;
        for (Villager v : sino.getWorld().getNearbyEntitiesByType(Villager.class, praca, 48, 16, 48)) {
            if (!daVila(v) || v.isSleeping()) continue;
            Location ponto = v.getMemory(MemoryKey.MEETING_POINT);
            if (ponto != null && ponto.getBlock().equals(sino)) sinos.putIfAbsent(sino.getLocation(), 0L);
            v.getPathfinder().moveTo(praca.clone().add(rnd().nextDouble(-3, 3), 0, rnd().nextDouble(-3, 3)), 0.7);
            vieram++;
        }
        if (vieram > 0) p.sendActionBar(Component.text("🔔 " + vieram + (vieram == 1 ? " aldeão vem" : " aldeões vêm") + " para a praça.", COR));
    }

    // =====================================================================
    //  Dia de feira
    // =====================================================================

    /** Dia de feira: a cada 3 dias do mundo, entre a manhã e o fim da tarde. */
    public static boolean diaDeFeira(World w) {
        long hora = w.getTime();
        return (w.getFullTime() / 24000) % 3 == 0 && hora >= 1000 && hora <= 11000;
    }

    /** Chamado a cada segundo: acha as praças perto dos jogadores e faz a festa. */
    public void tick() {
        if (!plugin.settings().vilasVanilla || ++segundos % 5 != 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL) continue;
            if (segundos % 30 == 0) {
                for (Villager v : w.getNearbyEntitiesByType(Villager.class, p.getLocation(), 64, 24, 64)) {
                    nomear(v);
                    Location ponto = daVila(v) ? v.getMemory(MemoryKey.MEETING_POINT) : null;
                    if (ponto != null && ponto.getWorld() != null && ponto.getBlock().getType() == Material.BELL) sinos.putIfAbsent(ponto.getBlock().getLocation(), 0L);
                }
            }
            if (!diaDeFeira(w)) continue;
            long dia = w.getFullTime() / 24000;
            for (Location sino : sinos.keySet()) {
                if (!sino.getWorld().equals(w) || sino.distanceSquared(p.getLocation()) > 48 * 48 || !sino.isChunkLoaded()) continue;
                Location c = sino.clone().add(0.5, 1, 0.5);
                w.spawnParticle(Particle.HAPPY_VILLAGER, c, 6, 4, 1, 4, 0);
                if (rnd().nextDouble() < 0.3) w.spawnParticle(Particle.NOTE, c.clone().add(0, 1, 0), 2, 2, 0.5, 2, 1);
                String chave = p.getUniqueId() + ":" + sino.getBlockX() + ":" + sino.getBlockZ();
                if (avisado.getOrDefault(chave, -1L) != dia) {
                    avisado.put(chave, dia);
                    p.sendMessage(Component.text("🛒 Dia de feira na vila! As trocas com os aldeões estão 20% mais baratas hoje.", COR));
                    p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.6f, 1.2f);
                }
                // Às vezes um mercador ambulante chega para a feira (um por praça por feira).
                Location chaveMercador = sino.clone();
                chaveMercador.setYaw(dia);
                if (!mercadorVeio.contains(chaveMercador) && rnd().nextDouble() < 0.02) {
                    mercadorVeio.add(chaveMercador);
                    Location onde = c.clone().add(rnd().nextDouble(-4, 4), 0, rnd().nextDouble(-4, 4));
                    onde.setY(w.getHighestBlockYAt(onde) + 1);
                    w.spawn(onde, WanderingTrader.class, t -> t.setDespawnDelay(20 * 60 * 10));
                }
            }
        }
    }

    /** Na feira, a loja do aldeão abre com 20% de desconto. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAbrirLoja(InventoryOpenEvent e) {
        if (!plugin.settings().vilasVanilla || !(e.getInventory() instanceof MerchantInventory mi)) return;
        if (!(mi.getMerchant() instanceof Villager v) || !daVila(v) || !diaDeFeira(v.getWorld())) return;
        boolean pertoDeSino = false;
        for (Location s : sinos.keySet()) if (s.getWorld().equals(v.getWorld()) && s.distanceSquared(v.getLocation()) < 64 * 64) pertoDeSino = true;
        if (!pertoDeSino) return;
        for (MerchantRecipe r : v.getRecipes()) {
            if (r.getIngredients().isEmpty()) continue;
            int base = r.getIngredients().getFirst().getAmount();
            r.setSpecialPrice(r.getSpecialPrice() - Math.max(1, (int) Math.round(base * 0.2)));
        }
        if (e.getPlayer() instanceof Player p) p.sendActionBar(Component.text("🛒 Preço de feira: 20% mais barato hoje.", COR));
    }
}
