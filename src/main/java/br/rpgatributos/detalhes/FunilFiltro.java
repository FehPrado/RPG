package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Funil com filtro (2.65): uma moldura presa no funil, com um item dentro, faz o funil só aceitar
 * aquele item (vale para várias molduras: aceita qualquer um dos itens delas). Moldura vazia ou
 * sem moldura: o funil de sempre.
 */
final class FunilFiltro implements Listener {

    private record Filtro(Set<Material> itens, long ate) {}

    private final RPGAtributos plugin;
    /** O filtro de cada funil fica guardado 2 segundos (o funil pergunta muitas vezes). */
    private final Map<Location, Filtro> cache = new HashMap<>();

    FunilFiltro(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Set<Material> filtro(Block funil) {
        Location l = funil.getLocation();
        long agora = System.currentTimeMillis();
        Filtro f = cache.get(l);
        if (f != null && f.ate() > agora) return f.itens();
        if (cache.size() > 4000) cache.clear();
        Set<Material> itens = EnumSet.noneOf(Material.class);
        for (ItemFrame m : funil.getWorld().getNearbyEntitiesByType(ItemFrame.class, l.clone().add(0.5, 0.5, 0.5), 1.0)) {
            if (!m.getLocation().getBlock().getRelative(m.getAttachedFace()).equals(funil)) continue;
            ItemStack i = m.getItem();
            if (!i.isEmpty()) itens.add(i.getType());
        }
        cache.put(l, new Filtro(itens, agora + 2000));
        return itens;
    }

    private Block funil(Inventory inv) {
        return plugin.settings().utilFunil && inv.getHolder(false) instanceof Hopper h ? h.getBlock() : null;
    }

    private boolean recusa(Inventory destino, ItemStack item) {
        Block b = funil(destino);
        if (b == null) return false;
        Set<Material> f = filtro(b);
        return !f.isEmpty() && !f.contains(item.getType());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMover(InventoryMoveItemEvent e) {
        if (recusa(e.getDestination(), e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPegarDoChao(InventoryPickupItemEvent e) {
        if (recusa(e.getInventory(), e.getItem().getItemStack())) e.setCancelled(true);
    }
}
