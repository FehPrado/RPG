package br.rpgatributos.aventura;

import br.rpgatributos.alquimia.Acessorio;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.arcano.ItensMagicos;
import br.rpgatributos.pesca.PeixeRaro;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Material raro (e os itens da Alquimia: componentes, gemas, acessórios e elixires) não pode
 * ser gasto como se fosse o item comum que serve de base. Peixes raros também não vão para o forno.
 */
public final class MateriaisListener implements Listener {

    private static boolean especial(ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        return Raro.de(i) != null || Reagente.de(i) != null || Gema.de(i) != null || Acessorio.de(i) != null || Elixir.de(i) != null
                || ItensMagicos.ehPergaminho(i) || ItensMagicos.ehTomo(i) || i.getPersistentDataContainer().has(LEMBRANCA)
                || i.getPersistentDataContainer().has(br.rpgatributos.fe.Transmutacao.K_PEDRA)
                || (i.getType() == org.bukkit.Material.BREEZE_ROD && i.getPersistentDataContainer().has(FORJADO));
    }

    private static final org.bukkit.NamespacedKey LEMBRANCA = new org.bukkit.NamespacedKey("rpgatributos", "lembranca");
    private static final org.bukkit.NamespacedKey FORJADO = new org.bukkit.NamespacedKey("rpgatributos", "forja_raridade");

    private static boolean algumRaro(ItemStack... itens) {
        for (ItemStack i : itens) if (especial(i)) return true;
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
    public void aoPrepararEncantamento(org.bukkit.event.enchantment.PrepareItemEnchantEvent e) {
        if (especial(e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararBigorna(PrepareAnvilEvent e) {
        if (algumRaro(e.getInventory().getContents())) e.setResult(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoCozinharBloco(BlockCookEvent e) {
        if (algumRaro(e.getSource()) || PeixeRaro.de(e.getSource()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQueimar(FurnaceBurnEvent e) {
        if (algumRaro(e.getFuel())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFazerPocao(BrewEvent e) {
        if (algumRaro(e.getContents().getContents())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFabricarNoCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getState() instanceof Crafter c && algumRaro(c.getInventory().getContents())) e.setCancelled(true);
    }
}
