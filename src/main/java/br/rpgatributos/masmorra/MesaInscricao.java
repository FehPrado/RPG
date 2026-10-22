package br.rpgatributos.masmorra;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Mesa de Inscrição (2.72): uma mesa de flecheiro com 1 livro e 1 bolsa de tinta brilhante jogados
 * em cima. Escreve Inscrições (1 papel + Cristais de Mana) e, com um Cristal de Portal na mão,
 * aplica as que você tem no inventário (até 4 por cristal).
 */
public final class MesaInscricao extends Estacao {

    static final TextColor COR = TextColor.color(0x80DEEA);
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int S_CRISTAL = 4, S_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        Location mesa;
        @Override public Inventory getInventory() { return inv; }
    }

    public MesaInscricao(RPGAtributos plugin) {
        super(plugin, "mesas_inscricao", "mesa_inscricao_display");
    }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.FLETCHING_TABLE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.BOOK || m == Material.GLOW_INK_SAC;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.FLETCHING_TABLE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.BOOK) < 1 || contar(itens, Material.GLOW_INK_SAC) < 1) return false;
        tirar(itens, Material.BOOK, 1);
        tirar(itens, Material.GLOW_INK_SAC, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.WRITABLE_BOOK);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("✎ Mesa de Inscrição", COR)
                .append(Component.newline())
                .append(Component.text("clique com um Cristal de Portal", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.GLOW, c, 30, 0.4, 0.3, 0.4, 0.05);
        c.getWorld().playSound(c, Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
        if (quem != null) quem.sendMessage(Component.text("✎ Mesa de Inscrição pronta. Escreva Inscrições e aplique num Cristal de Portal.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 10 == 0) centro.getWorld().spawnParticle(Particle.GLOW, centro, 2, 0.2, 0.2, 0.2, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.BOOK));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GLOW_INK_SAC));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.mesa = b.getLocation();
        t.inv = Bukkit.createInventory(t, 54, Component.text("✎ Mesa de Inscrição"));
        desenhar(p, t);
        p.openInventory(t.inv);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    /** Quantas inscrições desse tipo o jogador tem no inventário. */
    private static int quantas(Player p, Inscricao i) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) if (Inscricao.de(s) == i) n += s.getAmount();
        return n;
    }

    private static boolean tirarUma(Player p, Inscricao i) {
        ItemStack[] cont = p.getInventory().getStorageContents();
        for (int k = 0; k < cont.length; k++) {
            if (Inscricao.de(cont[k]) != i) continue;
            cont[k].setAmount(cont[k].getAmount() - 1);
            p.getInventory().setStorageContents(cont);
            return true;
        }
        return false;
    }

    private void desenhar(Player p, Tela t) {
        t.inv.clear();
        ItemStack mao = p.getInventory().getItemInMainHand();
        CristalPortal.Dados d = CristalPortal.ler(mao);
        int mana = ManaCristal.valor(p);
        if (d != null) {
            ItemStack c = mao.clone();
            c.setAmount(1);
            t.inv.setItem(S_CRISTAL, c);
        } else {
            ItemStack info = new ItemStack(Material.AMETHYST_SHARD);
            info.editMeta(m -> {
                m.itemName(Component.text("Sem Cristal de Portal na mão", NamedTextColor.GRAY));
                m.lore(List.of(Component.text("Segure um cristal e clique na mesa", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("para aplicar as suas Inscrições.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            });
            t.inv.setItem(S_CRISTAL, info);
        }
        Inscricao[] todas = Inscricao.values();
        for (int k = 0; k < todas.length && k < SLOTS.length; k++) {
            Inscricao i = todas[k];
            int tem = quantas(p, i);
            ItemStack icone = i.criar(1);
            String motivo = d == null ? null : CristalPortal.impedimento(d, i);
            icone.editMeta(m -> {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(i.descricao(), NamedTextColor.GRAY));
                lore.add(Component.text("Até " + i.maximo() + " por cristal.", NamedTextColor.DARK_GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("Você tem: " + tem, tem > 0 ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY));
                if (d != null) {
                    if (tem <= 0) lore.add(Component.text("Esquerdo: aplicar (escreva uma antes)", NamedTextColor.DARK_GRAY));
                    else if (motivo != null) lore.add(Component.text("✖ " + motivo, NamedTextColor.RED));
                    else lore.add(Component.text("» Esquerdo: aplicar no cristal da mão", NamedTextColor.YELLOW));
                }
                boolean pode = mana >= i.preco();
                lore.add(Component.text("» Direito: escrever (1 papel + " + i.preco() + " de mana)", pode ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                m.addItemFlags(ItemFlag.values());
                m.setEnchantmentGlintOverride(tem > 0);
            });
            icone.setAmount(Math.max(1, Math.min(64, tem)));
            t.inv.setItem(SLOTS[k], icone);
        }
        ItemStack manaInfo = new ItemStack(Material.AMETHYST_CLUSTER);
        manaInfo.editMeta(m -> m.itemName(Component.text("Seus Cristais de Mana: " + mana, ManaCristal.COR, TextDecoration.BOLD)));
        t.inv.setItem(40, manaInfo);
        ItemStack fechar = new ItemStack(Material.BARRIER);
        fechar.editMeta(m -> m.itemName(Component.text("Fechar", NamedTextColor.RED)));
        t.inv.setItem(S_FECHAR, fechar);
        ItemStack vidro = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < t.inv.getSize(); i++) if (t.inv.getItem(i) == null) t.inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != topo) return;
        if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
        if (!eh(t.mesa.getBlock())) { p.closeInventory(); return; }
        Inscricao[] todas = Inscricao.values();
        for (int k = 0; k < todas.length && k < SLOTS.length; k++) {
            if (SLOTS[k] != e.getSlot()) continue;
            Inscricao i = todas[k];
            if (e.isRightClick()) escrever(p, i);
            else aplicar(p, i);
            desenhar(p, t);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private void escrever(Player p, Inscricao i) {
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && !p.getInventory().containsAtLeast(new ItemStack(Material.PAPER), 1)) {
            erro(p, "Falta 1 papel comum.");
            return;
        }
        if (!criativo && ManaCristal.valor(p) < i.preco()) {
            erro(p, "Faltam Cristais de Mana (" + i.preco() + ").");
            return;
        }
        if (!criativo) {
            // Papel comum (sem dados do plugin): nada de gastar um Pergaminho ou outra Inscrição.
            if (!tirarPapelComum(p)) { erro(p, "Falta 1 papel comum."); return; }
            ManaCristal.gastar(p, i.preco());
        }
        for (ItemStack s : p.getInventory().addItem(i.criar(1)).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
        p.playSound(p.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1.2f);
    }

    private static boolean tirarPapelComum(Player p) {
        ItemStack[] cont = p.getInventory().getStorageContents();
        for (int k = 0; k < cont.length; k++) {
            ItemStack s = cont[k];
            if (s == null || s.getType() != Material.PAPER || (s.hasItemMeta() && !s.getItemMeta().getPersistentDataContainer().isEmpty())) continue;
            s.setAmount(s.getAmount() - 1);
            p.getInventory().setStorageContents(cont);
            return true;
        }
        return false;
    }

    private void aplicar(Player p, Inscricao i) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        CristalPortal.Dados d = CristalPortal.ler(mao);
        if (d == null) { erro(p, "Segure um Cristal de Portal na mão."); return; }
        if (quantas(p, i) <= 0) { erro(p, "Você não tem essa Inscrição. Clique direito para escrever."); return; }
        String motivo = CristalPortal.impedimento(d, i);
        if (motivo != null) { erro(p, motivo); return; }
        if (!tirarUma(p, i)) return;
        // Um cristal por vez: se a mão tinha vários, só um recebe a inscrição.
        ItemStack novo = CristalPortal.criar(CristalPortal.inscrever(d, i));
        ItemStack atual = p.getInventory().getItemInMainHand();
        if (atual.getAmount() > 1) {
            atual.setAmount(atual.getAmount() - 1);
            for (ItemStack s : p.getInventory().addItem(novo).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
        } else {
            p.getInventory().setItemInMainHand(novo);
        }
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.3f);
        p.sendActionBar(Component.text("✎ Inscrição aplicada: " + i.nome(), COR));
    }

    private static void erro(Player p, String msg) {
        p.sendActionBar(Component.text("✎ " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
