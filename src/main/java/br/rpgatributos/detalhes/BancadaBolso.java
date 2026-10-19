package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Bancada de Bolso (2.65): clique direito abre a bancada de trabalho; agachado, escolhe outra
 * estação (pedra de amolar, tear, cortador de pedras, mesa de cartografia ou de ferraria). Não
 * precisa pôr o bloco no chão. Clicando num bloco que abre algo (baú, porta...), ele abre normal.
 */
final class BancadaBolso implements Listener {

    private static final class Escolha implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final Material[] ESTACOES = {Material.CRAFTING_TABLE, Material.GRINDSTONE, Material.LOOM,
            Material.STONECUTTER, Material.CARTOGRAPHY_TABLE, Material.SMITHING_TABLE};
    private static final String[] NOMES = {"Bancada de trabalho", "Pedra de amolar", "Tear", "Cortador de pedras",
            "Mesa de cartografia", "Mesa de ferraria"};
    private static final int[] SLOTS = {1, 2, 3, 5, 6, 7};

    private final RPGAtributos plugin;

    BancadaBolso(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !Utilidades.Item.BANCADA.e(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        // Bloco que abre algo (baú, porta...): fica com o bloco, a não ser agachado.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !p.isSneaking() && e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable()) return;
        e.setCancelled(true);
        if (!plugin.settings().utilBancada) return;
        if (p.isSneaking()) escolher(p);
        else abrir(p, 0);
    }

    private void escolher(Player p) {
        Escolha h = new Escolha();
        Inventory inv = Bukkit.createInventory(h, 9, Component.text("Bancada de Bolso", Utilidades.COR));
        h.inv = inv;
        for (int i = 0; i < ESTACOES.length; i++) {
            ItemStack s = new ItemStack(ESTACOES[i]);
            int k = i;
            s.editMeta(m -> {
                m.itemName(Component.text(NOMES[k], Utilidades.COR));
                m.lore(List.of(Component.text("Clique para abrir", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            });
            inv.setItem(SLOTS[i], s);
        }
        p.openInventory(inv);
    }

    private void abrir(Player p, int i) {
        switch (i) {
            case 0 -> p.openWorkbench(null, true);
            case 1 -> p.openGrindstone(null, true);
            case 2 -> p.openLoom(null, true);
            case 3 -> p.openStonecutter(null, true);
            case 4 -> p.openCartographyTable(null, true);
            case 5 -> p.openSmithingTable(null, true);
            default -> { return; }
        }
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 0.6f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoEscolher(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Escolha)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != e.getRawSlot()) continue;
            int k = i;
            Bukkit.getScheduler().runTask(plugin, () -> abrir(p, k));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder(false) instanceof Escolha) e.setCancelled(true);
    }
}
