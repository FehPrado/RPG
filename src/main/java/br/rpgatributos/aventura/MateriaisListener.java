package br.rpgatributos.aventura;

import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/** Material raro não pode ser gasto como se fosse o item comum que serve de base. */
public final class MateriaisListener implements Listener {

    private static boolean algumRaro(ItemStack... itens) {
        for (ItemStack i : itens) if (Raro.de(i) != null) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararReceita(PrepareItemCraftEvent e) {
        if (algumRaro(e.getInventory().getMatrix())) e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararFerraria(PrepareSmithingEvent e) {
        if (algumRaro(e.getInventory().getContents())) e.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararBigorna(PrepareAnvilEvent e) {
        if (algumRaro(e.getInventory().getContents())) e.setResult(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFundir(FurnaceSmeltEvent e) {
        if (algumRaro(e.getSource())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQueimar(FurnaceBurnEvent e) {
        if (algumRaro(e.getFuel())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFazerPocao(BrewEvent e) {
        if (algumRaro(e.getContents().getIngredient())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFabricarNoCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getState() instanceof Crafter c && algumRaro(c.getInventory().getContents())) e.setCancelled(true);
    }
}
