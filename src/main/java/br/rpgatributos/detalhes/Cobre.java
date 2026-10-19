package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cobre com propósito (2.62): o para-raios guarda a carga do raio que caiu nele por 15 minutos (soltando
 * faíscas). Clicando nele, o jogador pega a carga: fica ligeiro por 3 minutos (Pressa II e Velocidade) e
 * a próxima peça que forjar na Forja do Ferreiro tem mais chance de sair mais rara (vale 30 minutos).
 */
public final class Cobre implements Listener {

    private static final TextColor COR = TextColor.color(0xE8823A);
    private static final NamespacedKey K_CARGA = new NamespacedKey("rpgatributos", "carga_do_raio");
    private static final long DURA = 15 * 60_000L, CARGA_NA_FORJA = 30 * 60_000L;
    /** Chance a mais de subir a raridade na forja com a carga. */
    public static final double BONUS_FORJA = 0.08;

    private final RPGAtributos plugin;
    /** Para-raios carregados → até quando. */
    private final Map<Location, Long> carregados = new HashMap<>();
    private int ticks;

    public Cobre(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static boolean paraRaios(Material m) {
        return m.name().endsWith("LIGHTNING_ROD");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoRaio(LightningStrikeEvent e) {
        if (!plugin.settings().cobre) return;
        Block b = e.getLightning().getLocation().getBlock();
        for (int dy = -2; dy <= 1; dy++) {
            Block r = b.getRelative(0, dy, 0);
            if (!paraRaios(r.getType())) continue;
            carregados.put(r.getLocation(), System.currentTimeMillis() + DURA);
            return;
        }
    }

    /** A cada tick: faíscas nos para-raios carregados (e some quando a carga acaba). */
    public void tick() {
        if (carregados.isEmpty() || ++ticks % 10 != 0) return;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<Location, Long>> it = carregados.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, Long> en = it.next();
            Location l = en.getKey();
            if (agora > en.getValue() || !l.isChunkLoaded() || !paraRaios(l.getBlock().getType())) {
                it.remove();
                continue;
            }
            l.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, l.clone().add(0.5, 0.8, 0.5), 4, 0.15, 0.4, 0.15, 0.05);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        Long ate = carregados.get(b.getLocation());
        if (ate == null || System.currentTimeMillis() > ate) return;
        e.setCancelled(true);
        carregados.remove(b.getLocation());
        Player p = e.getPlayer();
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 180, 1, true, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 0, true, true, true));
        p.getPersistentDataContainer().set(K_CARGA, PersistentDataType.LONG, System.currentTimeMillis() + CARGA_NA_FORJA);
        Location c = b.getLocation().add(0.5, 0.8, 0.5);
        b.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 40, 0.3, 0.6, 0.3, 0.3);
        b.getWorld().playSound(c, Sound.ITEM_TRIDENT_THUNDER, 0.5f, 1.6f);
        p.sendMessage(Component.text("⚡ Você pegou a carga do raio: ligeiro por 3 min, e a próxima peça forjada (em 30 min) tem mais chance de sair mais rara.", COR));
    }

    /** Chamado pela Forja: gasta a carga do jogador (se tiver) e devolve a chance a mais. */
    public double gastarCarga(Player ferreiro) {
        if (ferreiro == null) return 0;
        Long ate = ferreiro.getPersistentDataContainer().get(K_CARGA, PersistentDataType.LONG);
        if (ate == null) return 0;
        ferreiro.getPersistentDataContainer().remove(K_CARGA);
        if (System.currentTimeMillis() > ate) return 0;
        ferreiro.sendActionBar(Component.text("⚡ A carga do raio passou para a peça.", NamedTextColor.GOLD));
        return BONUS_FORJA;
    }
}
