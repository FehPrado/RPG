package br.rpgatributos.datapack;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.2: o datapack só sabe soltar itens do jogo. Nas escavações (areia e cascalho suspeitos das
 * estruturas do datapack) ele solta um papel marcado com {@code rpgatributos:troca}; quando o papel
 * cai no mundo, o plugin troca pelo item do RPG de verdade (relíquia, tomo, página, carta ou mapa).
 */
public final class TrocaDeSaque implements Listener {

    private static final NamespacedKey K_TROCA = new NamespacedKey("rpgatributos", "troca");

    private final RPGAtributos plugin;

    public TrocaDeSaque(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void aoCair(ItemSpawnEvent e) {
        Item item = e.getEntity();
        String tipo = item.getItemStack().getPersistentDataContainer().get(K_TROCA, PersistentDataType.STRING);
        if (tipo == null) return;
        ItemStack novo = criar(tipo);
        if (novo == null || novo.isEmpty()) {
            e.setCancelled(true);
            return;
        }
        item.setItemStack(novo);
    }

    /** O item do RPG para cada marca (null se a marca não for conhecida). */
    ItemStack criar(String tipo) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (tipo) {
            case "reliquia" -> plugin.arqueologia().reliquiaAleatoria();
            case "tomo" -> br.rpgatributos.arcano.ItensMagicos.tomoAleatorio();
            case "pagina" -> Raro.PAGINA_DE_LENDA.criar(1);
            case "mapa" -> Raro.MAPA_RASGADO.criar(1);
            case "carta" -> br.rpgatributos.vida.Album.carta(br.rpgatributos.vida.Album.sortear(r, false), false);
            default -> null;
        };
    }
}
