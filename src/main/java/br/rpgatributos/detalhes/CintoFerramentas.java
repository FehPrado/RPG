package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

/**
 * Cinto de Ferramentas (2.65): guarda até 9 ferramentas (picaretas, machados, pás, enxadas,
 * espadas e tesouras). Segurando o cinto e batendo num bloco, a melhor ferramenta para aquele bloco
 * vem para a mão; batendo num bloco de outro tipo, ela troca sozinha. Parou de usar (4 s), trocou
 * de espaço na barra, abriu um inventário, soltou ou morreu: a ferramenta volta para o cinto.
 * <p>Enquanto está na mão, a ferramenta carrega o cinto dentro dela (o resto das ferramentas), então
 * nada se perde nem se duplica: tudo o que a tira da mão a devolve para o cinto antes.
 */
final class CintoFerramentas implements Listener {

    private static final NamespacedKey K_CONTEUDO = new NamespacedKey("rpgatributos", "cinto_conteudo");
    private static final NamespacedKey K_CINTO = new NamespacedKey("rpgatributos", "cinto_dentro");
    private static final NamespacedKey K_ID = new NamespacedKey("rpgatributos", "cinto_id");
    private static final int LUGARES = 9;
    private static final long PARADA = 4000;

    private static final class Janela implements InventoryHolder {
        Inventory inv;
        int espaco;
        String id;
        @Override public Inventory getInventory() { return inv; }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Long> ultimoUso = new HashMap<>();

    CintoFerramentas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  O que vai dentro
    // =====================================================================

    static boolean ferramenta(ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        if (Utilidades.Item.CINTO.e(i) || emUso(i)) return false;
        String n = i.getType().name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || n.endsWith("_SWORD") || i.getType() == Material.SHEARS;
    }

    static boolean emUso(ItemStack i) {
        return i != null && !i.isEmpty() && i.hasItemMeta() && i.getPersistentDataContainer().has(K_CINTO);
    }

    private static List<ItemStack> conteudo(ItemStack cinto) {
        byte[] b = cinto.getPersistentDataContainer().get(K_CONTEUDO, PersistentDataType.BYTE_ARRAY);
        List<ItemStack> l = new ArrayList<>();
        if (b != null) for (ItemStack i : ItemStack.deserializeItemsFromBytes(b)) if (i != null && !i.isEmpty()) l.add(i);
        return l;
    }

    private static void guardar(ItemStack cinto, List<ItemStack> itens) {
        cinto.editMeta(m -> {
            m.getPersistentDataContainer().set(K_CONTEUDO, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(itens));
            if (!m.getPersistentDataContainer().has(K_ID)) m.getPersistentDataContainer().set(K_ID, PersistentDataType.STRING, UUID.randomUUID().toString());
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(Utilidades.Item.CINTO.linha1, NamedTextColor.GRAY));
            lore.add(Component.text(Utilidades.Item.CINTO.linha2, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Guardadas: " + itens.size() + "/" + LUGARES, Utilidades.COR));
            for (ItemStack i : itens) {
                Component nome = i.hasItemMeta() && i.getItemMeta().hasItemName() ? i.getItemMeta().itemName()
                        : i.hasItemMeta() && i.getItemMeta().hasDisplayName() ? i.getItemMeta().displayName() : Component.translatable(i.getType().translationKey());
                lore.add(Component.text(" • ", NamedTextColor.DARK_GRAY).append(nome.colorIfAbsent(NamedTextColor.GRAY)));
            }
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        });
    }

    /** Tira a ferramenta i do cinto: ela vem com o cinto (e o resto) dentro. */
    private static ItemStack sacar(ItemStack cinto, List<ItemStack> itens, int i) {
        ItemStack f = itens.remove(i);
        ItemStack c = cinto.clone();
        guardar(c, itens);
        ItemStack r = f.clone();
        r.editMeta(m -> m.getPersistentDataContainer().set(K_CINTO, PersistentDataType.BYTE_ARRAY, c.serializeAsBytes()));
        return r;
    }

    /** A ferramenta em uso volta para dentro do cinto (que é devolvido). */
    static ItemStack devolver(ItemStack ferramenta) {
        byte[] b = ferramenta.getPersistentDataContainer().get(K_CINTO, PersistentDataType.BYTE_ARRAY);
        ItemStack cinto = ItemStack.deserializeBytes(b);
        ItemStack f = ferramenta.clone();
        f.editMeta(m -> m.getPersistentDataContainer().remove(K_CINTO));
        List<ItemStack> itens = conteudo(cinto);
        itens.add(f);
        guardar(cinto, itens);
        return cinto;
    }

    /** O cinto que estava dentro, sem a ferramenta (quando ela quebra). */
    private static ItemStack cintoDe(ItemStack ferramenta) {
        return ItemStack.deserializeBytes(ferramenta.getPersistentDataContainer().get(K_CINTO, PersistentDataType.BYTE_ARRAY));
    }

    private static void devolverTudo(PlayerInventory inv) {
        ItemStack[] c = inv.getContents();
        for (int i = 0; i < c.length; i++) if (emUso(c[i])) inv.setItem(i, devolver(c[i]));
    }

    // =====================================================================
    //  A ferramenta certa vem para a mão
    // =====================================================================

    /** A melhor ferramenta para o bloco (a que faz o bloco soltar o item, e a mais rápida). */
    private static int melhor(Block b, List<ItemStack> itens) {
        int melhor = -1;
        double nota = 1.0001;
        for (int i = 0; i < itens.size(); i++) {
            ItemStack f = itens.get(i);
            double n = b.getDestroySpeed(f, true) + (b.isPreferredTool(f) && b.getType().getHardness() > 0 ? 1000 : 0);
            if (n > nota) {
                nota = n;
                melhor = i;
            }
        }
        return melhor;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(BlockDamageEvent e) {
        if (!plugin.settings().utilCinto) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        Block b = e.getBlock();
        if (Utilidades.Item.CINTO.e(mao)) {
            List<ItemStack> itens = conteudo(mao);
            int i = melhor(b, itens);
            if (i < 0) return;
            p.getInventory().setItemInMainHand(sacar(mao, itens, i));
            ultimoUso.put(p.getUniqueId(), System.currentTimeMillis());
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.5f, 1.4f);
        } else if (emUso(mao)) {
            ultimoUso.put(p.getUniqueId(), System.currentTimeMillis());
            ItemStack cinto = devolver(mao);
            List<ItemStack> itens = conteudo(cinto);
            int i = melhor(b, itens);
            if (i < 0 || itens.get(i).isSimilar(sem(mao))) return;
            p.getInventory().setItemInMainHand(sacar(cinto, itens, i));
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.5f, 1.4f);
        }
    }

    private static ItemStack sem(ItemStack emUso) {
        ItemStack f = emUso.clone();
        f.editMeta(m -> m.getPersistentDataContainer().remove(K_CINTO));
        return f;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (emUso(e.getPlayer().getInventory().getItemInMainHand())) ultimoUso.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    /** A cada segundo: a ferramenta parada há 4 s volta para o cinto. */
    void segundo() {
        long agora = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack mao = p.getInventory().getItemInMainHand();
            if (!emUso(mao)) continue;
            if (agora - ultimoUso.getOrDefault(p.getUniqueId(), 0L) < PARADA) continue;
            p.getInventory().setItemInMainHand(devolver(mao));
        }
    }

    void devolverTodos() {
        for (Player p : Bukkit.getOnlinePlayers()) devolverTudo(p.getInventory());
    }

    // =====================================================================
    //  Tudo o que tira a ferramenta da mão a devolve antes
    // =====================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoTrocarEspaco(PlayerItemHeldEvent e) {
        PlayerInventory inv = e.getPlayer().getInventory();
        ItemStack antes = inv.getItem(e.getPreviousSlot());
        if (emUso(antes)) inv.setItem(e.getPreviousSlot(), devolver(antes));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoTrocarMao(PlayerSwapHandItemsEvent e) {
        if (emUso(e.getMainHandItem()) || emUso(e.getOffHandItem())) {
            e.setCancelled(true);
            devolverTudo(e.getPlayer().getInventory());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoSoltar(PlayerDropItemEvent e) {
        ItemStack i = e.getItemDrop().getItemStack();
        if (emUso(i)) e.getItemDrop().setItemStack(devolver(i));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoMorrer(PlayerDeathEvent e) {
        for (ListIterator<ItemStack> it = e.getDrops().listIterator(); it.hasNext(); ) {
            ItemStack i = it.next();
            if (emUso(i)) it.set(devolver(i));
        }
        devolverTudo(e.getEntity().getInventory());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        devolverTudo(e.getPlayer().getInventory());
        ultimoUso.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoAbrir(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) devolverTudo(p.getInventory());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoMexer(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean algum = emUso(e.getCurrentItem()) || emUso(e.getCursor());
        if (!algum) for (ItemStack i : p.getInventory().getContents()) if (emUso(i)) { algum = true; break; }
        if (!algum) return;
        e.setCancelled(true);
        devolverTudo(p.getInventory());
        if (emUso(e.getCursor())) p.setItemOnCursor(devolver(e.getCursor()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoPorNaMoldura(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemFrame && emUso(e.getPlayer().getInventory().getItem(e.getHand()))) {
            e.setCancelled(true);
            devolverTudo(e.getPlayer().getInventory());
        }
    }

    /** A ferramenta quebrou: o cinto (com o resto) volta para a mão. */
    @EventHandler
    public void aoQuebrarFerramenta(PlayerItemBreakEvent e) {
        ItemStack f = e.getBrokenItem();
        if (!emUso(f)) return;
        ItemStack cinto = cintoDe(f);
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) {
                p.getWorld().dropItemNaturally(p.getLocation(), cinto);
                return;
            }
            if (p.getInventory().getItemInMainHand().isEmpty()) p.getInventory().setItemInMainHand(cinto);
            else for (ItemStack s : p.getInventory().addItem(cinto).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
        });
    }

    // =====================================================================
    //  A janela do cinto
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !Utilidades.Item.CINTO.e(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !p.isSneaking() && e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable()) return;
        e.setCancelled(true);
        if (!plugin.settings().utilCinto) return;
        ItemStack cinto = p.getInventory().getItemInMainHand();
        if (!cinto.getPersistentDataContainer().has(K_ID)) guardar(cinto, conteudo(cinto));
        Janela j = new Janela();
        j.inv = Bukkit.createInventory(j, LUGARES, Component.text(Utilidades.Item.CINTO.nome, Utilidades.COR));
        j.espaco = p.getInventory().getHeldItemSlot();
        j.id = cinto.getPersistentDataContainer().get(K_ID, PersistentDataType.STRING);
        List<ItemStack> itens = conteudo(cinto);
        for (int i = 0; i < itens.size() && i < LUGARES; i++) j.inv.setItem(i, itens.get(i));
        p.openInventory(j.inv);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.6f, 1f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMexerNaJanela(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Janela j)) return;
        Inventory topo = e.getView().getTopInventory();
        boolean noTopo = e.getClickedInventory() == topo;
        // O cinto não sai do lugar enquanto a janela está aberta.
        if (!noTopo && e.getSlot() == j.espaco && e.getClickedInventory() != null) { e.setCancelled(true); return; }
        if (e.getHotbarButton() == j.espaco || e.getClick() == ClickType.SWAP_OFFHAND || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) { e.setCancelled(true); return; }
        if (noTopo) {
            ItemStack entra = e.getHotbarButton() >= 0 ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : e.getCursor();
            if (entra != null && !entra.isEmpty() && !ferramenta(entra)) e.setCancelled(true);
        } else if (e.isShiftClick() && !ferramenta(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoArrastarNaJanela(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Janela)) return;
        boolean noTopo = e.getRawSlots().stream().anyMatch(s -> s < LUGARES);
        if (noTopo && !ferramenta(e.getOldCursor())) e.setCancelled(true);
    }

    @EventHandler
    public void aoFecharJanela(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Janela j) || !(e.getPlayer() instanceof Player p)) return;
        List<ItemStack> itens = new ArrayList<>();
        List<ItemStack> sobra = new ArrayList<>();
        for (ItemStack i : j.inv.getContents()) {
            if (i == null || i.isEmpty()) continue;
            if (ferramenta(i)) itens.add(i);
            else sobra.add(i);
        }
        ItemStack cinto = p.getInventory().getItem(j.espaco);
        if (Utilidades.Item.CINTO.e(cinto) && j.id != null && j.id.equals(cinto.getPersistentDataContainer().get(K_ID, PersistentDataType.STRING))) {
            guardar(cinto, itens);
        } else {
            sobra.addAll(itens); // o cinto sumiu do lugar: nada se perde, as ferramentas voltam para o jogador
        }
        for (ItemStack s : sobra) for (ItemStack r : p.getInventory().addItem(s).values()) p.getWorld().dropItemNaturally(p.getLocation(), r);
    }
}
