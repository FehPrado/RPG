package br.rpgatributos.mochila;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Mochila: um baú pessoal (/mochila) que cresce com o nível total: 1 fileira no começo e
 * mais uma a cada tantos níveis somados, até 6. Fica salva no próprio jogador e não cai
 * quando ele morre.
 */
public final class Mochila implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xC9955C);
    public static final int FILEIRAS_MAX = 6;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        UUID dono;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey chave;

    public Mochila(RPGAtributos plugin) {
        this.plugin = plugin;
        this.chave = new NamespacedKey(plugin, "mochila");
    }

    /** Fileiras liberadas pelo nível total. */
    public int fileiras(Player p) {
        int por = Math.max(1, plugin.settings().mochilaNiveisPorFileira);
        return Math.max(1, Math.min(FILEIRAS_MAX, 1 + plugin.stats().nivelTotal(p) / por));
    }

    private ItemStack[] carregar(Player p) {
        byte[] b = p.getPersistentDataContainer().get(chave, PersistentDataType.BYTE_ARRAY);
        if (b == null || b.length == 0) return new ItemStack[0];
        try {
            return ItemStack.deserializeItemsFromBytes(b);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Mochila de " + p.getName() + " não pôde ser lida: " + ex.getMessage());
            return new ItemStack[0];
        }
    }

    private void salvar(Player p, ItemStack[] itens) {
        ItemStack[] copia = new ItemStack[itens.length];
        boolean vazia = true;
        for (int i = 0; i < itens.length; i++) {
            copia[i] = itens[i] == null || itens[i].isEmpty() ? ItemStack.empty() : itens[i];
            if (!copia[i].isEmpty()) vazia = false;
        }
        if (vazia) p.getPersistentDataContainer().remove(chave);
        else p.getPersistentDataContainer().set(chave, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(copia));
    }

    public void abrir(Player p) {
        if (!plugin.settings().mochilaAtiva) {
            p.sendMessage(Component.text("A mochila está desligada neste servidor.", NamedTextColor.RED));
            return;
        }
        ItemStack[] salvos = carregar(p);
        int fileiras = fileiras(p);
        // Se já houver itens além do tamanho atual (ex.: nível zerado por um admin), mostra tudo.
        int usadas = 0;
        for (int i = 0; i < salvos.length; i++) if (salvos[i] != null && !salvos[i].isEmpty()) usadas = i + 1;
        fileiras = Math.min(FILEIRAS_MAX, Math.max(fileiras, (usadas + 8) / 9));
        Tela t = new Tela();
        t.dono = p.getUniqueId();
        t.inventario = Bukkit.createInventory(t, fileiras * 9, Component.text("Mochila (" + fileiras + "/" + FILEIRAS_MAX + " fileiras)"));
        for (int i = 0; i < salvos.length && i < t.inventario.getSize(); i++) {
            if (salvos[i] != null && !salvos[i].isEmpty()) t.inventario.setItem(i, salvos[i]);
        }
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.8f, 0.9f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoFechar(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Tela t) || !(e.getPlayer() instanceof Player p)) return;
        if (!p.getUniqueId().equals(t.dono)) return;
        salvar(p, t.inventario.getContents());
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 0.5f, 1.2f);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        int por = Math.max(1, plugin.settings().mochilaNiveisPorFileira);
        int f = fileiras(p);
        if (f < FILEIRAS_MAX) {
            int falta = f * por - plugin.stats().nivelTotal(p);
            p.sendActionBar(Component.text("Próxima fileira com mais " + Math.max(1, falta) + " níveis somados", COR));
        }
        return true;
    }
}
