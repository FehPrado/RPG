package br.rpgatributos.lenda;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.forja.Categoria;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Livro das Lendas: cada lenda, de quem ela é, o que pede e o botão de forjar. */
public final class MenuLendas implements Listener {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int S_INFO = 4, S_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;

    public MenuLendas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Lendas lendas() { return plugin.lendas(); }

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Livro das Lendas"));
        desenhar(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        inv.setItem(S_INFO, item(Material.KNOWLEDGE_BOOK, Component.text("✦ Livro das Lendas", Lendas.COR, TextDecoration.BOLD), List.of(
                Component.text("Itens Especiais que nascem dos seus feitos.", NamedTextColor.GRAY),
                Component.text("Cada lenda existe uma vez só no mundo.", NamedTextColor.GRAY),
                Component.text("Os feitos ficam escondidos: decifre o enigma,", NamedTextColor.GRAY),
                Component.text("ou ache Páginas do Livro das Lendas.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Para forjar: cumpra os feitos, fique perto", NamedTextColor.GRAY),
                Component.text("de uma Forja do Ferreiro, segure o item", NamedTextColor.GRAY),
                Component.text("certo e clique na lenda.", NamedTextColor.GRAY),
                Component.text("Custo: " + Lendas.custo() + ".", NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("A lenda desperta (fica mais forte) com", NamedTextColor.GRAY),
                Component.text("os abates feitos com ela: I a V.", NamedTextColor.GRAY))));
        Lenda[] todas = Lenda.values();
        for (int i = 0; i < todas.length && i < SLOTS.length; i++) inv.setItem(SLOTS[i], icone(p, todas[i]));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private ItemStack icone(Player p, Lenda l) {
        Lendas.Registro r = lendas().dono(l);
        List<Component> lore = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        for (Categoria c : l.categorias()) cats.add(c.nome());
        lore.add(Component.text("✦ ESPECIAL • " + String.join(" ou ", cats), Lendas.COR));
        lore.add(Component.empty());
        lore.add(Component.text("Poder:", NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text("  " + l.poder(), NamedTextColor.WHITE));
        lore.add(Component.empty());
        boolean minha = r != null && r.dono().equals(p.getUniqueId());
        if (r != null) {
            lore.add(Component.text((minha ? "É sua" : "Pertence a " + r.nome()) + " (desde "
                    + new SimpleDateFormat("dd/MM/yyyy").format(new Date(r.data())) + ").", minha ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (minha) {
                lore.add(Component.text("Perdeu o item? Clique para forjar de novo", NamedTextColor.YELLOW));
                lore.add(Component.text("(a cópia antiga se apaga).", NamedTextColor.YELLOW));
                lore.add(Component.text("Shift + clique: abrir mão da lenda.", NamedTextColor.DARK_GRAY));
            }
        } else {
            lore.add(Component.text("Livre! Ninguém a forjou ainda.", NamedTextColor.GREEN));
        }
        lore.add(Component.empty());
        lore.add(Component.text("\"" + l.dica() + "\"", NamedTextColor.GOLD, TextDecoration.ITALIC));
        lore.add(Component.empty());
        lore.add(Component.text("Feitos pedidos:", NamedTextColor.GRAY));
        boolean tudo = true;
        for (Lenda.Requisito req : l.requisitos()) {
            long tem = Math.min(req.qtd(), lendas().valor(p, req.contador()));
            boolean ok = tem >= req.qtd();
            tudo &= ok;
            if (lendas().visivel(p, l, req)) {
                lore.add(Component.text((ok ? " ✔ " : " ✖ ") + req.texto() + " (" + tem + "/" + req.qtd() + ")", ok ? NamedTextColor.GREEN : NamedTextColor.RED));
            } else {
                lore.add(Component.text(" ✖ ??? (um feito escondido)", NamedTextColor.DARK_GRAY));
            }
        }
        if (tudo) lore.add(Component.text("✦ Seus feitos bastam para esta lenda!", Lendas.COR));
        else if (!lendas().revelada(p, l)) lore.add(Component.text("Uma Página do Livro das Lendas revela os feitos.", NamedTextColor.DARK_GRAY));
        boolean ferraria = plugin.stats().getNivel(p, br.rpgatributos.Skill.FERRARIA) >= 50;
        lore.add(Component.text((ferraria ? " ✔ " : " ✖ ") + "Ferraria nível 50", ferraria ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (tudo && ferraria && (r == null || minha)) lore.add(Component.text("» Segure o item e clique para forjar", NamedTextColor.YELLOW));
        ItemStack i = item(r == null || minha ? l.icone() : Material.GRAY_DYE,
                Component.text("✦ " + l.nome(), r == null || minha ? Lendas.COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore);
        if (r == null && tudo && ferraria) i.editMeta(m -> m.setEnchantmentGlintOverride(true));
        return i;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
        Lenda[] todas = Lenda.values();
        for (int i = 0; i < todas.length && i < SLOTS.length; i++) {
            if (SLOTS[i] != e.getSlot()) continue;
            Lenda l = todas[i];
            String erro;
            if (e.isShiftClick()) {
                erro = lendas().renunciar(p, l);
            } else {
                erro = lendas().forjar(p, l);
                if (erro == null) { p.closeInventory(); return; }
            }
            if (erro != null) {
                p.sendMessage(Component.text(erro, NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            }
            desenhar(p, t);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
