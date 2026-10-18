package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.fazenda.Variedade;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Conforto no dia a dia (2.57), cada um desligável em {@code conforto}:
 * <ul>
 *   <li><b>Colher com clique:</b> clique direito numa plantação madura colhe (como se tivesse quebrado,
 *       com qualidade, variedades e XP) e replanta com uma semente do chão ou do inventário.</li>
 *   <li><b>Ordenar com clique:</b> clique direito com a mão vazia num espaço vazio de um baú, barril,
 *       caixa de shulker ou baú do ender organiza tudo (junta as pilhas e agrupa por tipo). No
 *       próprio inventário, organiza a mochila (sem mexer na barra de atalhos nem na armadura).</li>
 *   <li><b>Repor a mão:</b> acabou a pilha na mão (bloco ou comida) ou a ferramenta quebrou: puxa
 *       outra igual do inventário.</li>
 *   <li><b>Portas duplas:</b> abrir uma porta de madeira abre a do lado (e portões lado a lado).
 *       Agachado, abre só uma.</li>
 * </ul>
 */
public final class Conforto implements Listener {

    private static final Map<Material, Material> SEMENTE = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART);

    private final RPGAtributos plugin;

    public Conforto(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Colher e replantar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoColher(PlayerInteractEvent e) {
        if (!plugin.settings().confColher || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.useInteractedBlock() == Event.Result.DENY || e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        Material tipo = b.getType();
        Material semente = SEMENTE.get(tipo);
        if (semente == null || p.isSneaking() || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!(b.getBlockData() instanceof Ageable a) || a.getAge() < a.getMaximumAge()) return;
        e.setCancelled(true);
        // Quebra como o jogador (proteção, qualidade, variedade e XP passam pelos mesmos eventos).
        if (!p.breakBlock(b)) return;
        p.swingMainHand();
        plugin.getServer().getScheduler().runTask(plugin, () -> replantar(p, b, tipo, semente));
    }

    private void replantar(Player p, Block b, Material tipo, Material semente) {
        if (!b.getType().isAir()) return;
        Material solo = b.getRelative(BlockFace.DOWN).getType();
        if (tipo == Material.NETHER_WART ? solo != Material.SOUL_SAND : solo != Material.FARMLAND) return;
        if (!tirarSemente(p, b, semente)) return;
        b.setType(tipo);
        b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5),
                tipo == Material.NETHER_WART ? Sound.ITEM_NETHER_WART_PLANT : Sound.ITEM_CROP_PLANT, 0.8f, 1f);
    }

    /** Uma semente comum (não de variedade): primeiro das que caíram ali, depois do inventário. */
    private static boolean tirarSemente(Player p, Block b, Material semente) {
        for (Item it : b.getWorld().getNearbyEntitiesByType(Item.class, b.getLocation().add(0.5, 0.5, 0.5), 1.5)) {
            ItemStack s = it.getItemStack();
            if (s.getType() != semente || Variedade.de(s) != null) continue;
            if (s.getAmount() <= 1) it.remove();
            else {
                s.setAmount(s.getAmount() - 1);
                it.setItemStack(s);
            }
            return true;
        }
        if (p.getGameMode() == GameMode.CREATIVE) return true;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType() != semente || Variedade.de(s) != null) continue;
            s.setAmount(s.getAmount() - 1);
            inv.setItem(i, s.getAmount() > 0 ? s : null);
            return true;
        }
        return false;
    }

    // =====================================================================
    //  Ordenar baú e mochila
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoOrdenar(InventoryClickEvent e) {
        if (!plugin.settings().confOrdenar || e.getClick() != ClickType.RIGHT || !(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCursor() != null && !e.getCursor().getType().isAir()) return;
        if (e.getCurrentItem() != null && !e.getCurrentItem().getType().isAir()) return;
        Inventory clicado = e.getClickedInventory();
        if (clicado == null) return;
        if (clicado instanceof PlayerInventory pi) {
            if (e.getSlot() < 9 || e.getSlot() > 35) return; // só a mochila (não a barra, a armadura nem a outra mão)
            e.setCancelled(true);
            ordenar(pi, 9, 36);
        } else if (clicado.equals(e.getView().getTopInventory()) && guardaItens(clicado)) {
            e.setCancelled(true);
            ordenar(clicado, 0, clicado.getSize());
        } else return;
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.6f, 1.1f);
        p.updateInventory();
    }

    /** Baú, barril, caixa de shulker (de bloco) ou baú do ender: não mexe em menus do plugin. */
    private static boolean guardaItens(Inventory inv) {
        if (inv.getType() == InventoryType.ENDER_CHEST) return true;
        if (inv.getType() != InventoryType.CHEST && inv.getType() != InventoryType.BARREL && inv.getType() != InventoryType.SHULKER_BOX) return false;
        var dono = inv.getHolder(false);
        return dono instanceof Container || dono instanceof org.bukkit.block.DoubleChest;
    }

    private static void ordenar(Inventory inv, int de, int ate) {
        List<ItemStack> itens = new ArrayList<>();
        for (int i = de; i < ate; i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir()) continue;
            juntar(itens, s.clone());
        }
        itens.sort(Comparator.comparing((ItemStack s) -> s.getType().ordinal())
                .thenComparing(Conforto::nome)
                .thenComparing(s -> -s.getAmount()));
        for (int i = de; i < ate; i++) inv.setItem(i, null);
        int slot = de;
        for (ItemStack s : itens) {
            if (slot >= ate) break; // não acontece: juntar nunca aumenta o número de pilhas
            inv.setItem(slot++, s);
        }
    }

    /** Junta nas pilhas iguais que ainda têm espaço; o que sobra vira pilha nova. */
    private static void juntar(List<ItemStack> itens, ItemStack s) {
        for (ItemStack x : itens) {
            if (s.getAmount() <= 0) return;
            if (!x.isSimilar(s) || x.getAmount() >= x.getMaxStackSize()) continue;
            int cabe = Math.min(x.getMaxStackSize() - x.getAmount(), s.getAmount());
            x.setAmount(x.getAmount() + cabe);
            s.setAmount(s.getAmount() - cabe);
        }
        if (s.getAmount() > 0) itens.add(s);
    }

    private static String nome(ItemStack s) {
        var m = s.getItemMeta();
        if (m == null) return "";
        if (m.hasItemName()) return PlainTextComponentSerializer.plainText().serialize(m.itemName());
        if (m.hasDisplayName()) return PlainTextComponentSerializer.plainText().serialize(m.displayName());
        return "";
    }

    // =====================================================================
    //  Repor a mão
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (e.getItemInHand().getAmount() == 1) depois(e.getPlayer(), e.getHand(), e.getItemInHand().clone(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        if (e.getItem().getAmount() == 1) depois(e.getPlayer(), e.getHand(), e.getItem().clone(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoQuebrarFerramenta(PlayerItemBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack quebrou = e.getBrokenItem();
        EquipmentSlot mao = quebrou.equals(p.getInventory().getItemInOffHand()) ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        depois(p, mao, quebrou.clone(), true);
    }

    private void depois(Player p, EquipmentSlot mao, ItemStack modelo, boolean ferramenta) {
        if (!plugin.settings().confRepor || p.getGameMode() == GameMode.CREATIVE) return;
        if (mao != EquipmentSlot.HAND && mao != EquipmentSlot.OFF_HAND) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> repor(p, mao, modelo, ferramenta));
    }

    private static void repor(Player p, EquipmentSlot mao, ItemStack modelo, boolean ferramenta) {
        if (!p.isOnline()) return;
        PlayerInventory inv = p.getInventory();
        ItemStack agora = inv.getItem(mao);
        if (agora != null && !agora.getType().isAir()) return;
        int mesmo = -1, parecido = -1;
        String tipo = tipoDeFerramenta(modelo.getType());
        for (int i = 0; i < 36; i++) {
            if (mao == EquipmentSlot.HAND && i == inv.getHeldItemSlot()) continue;
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir()) continue;
            if (ferramenta) {
                if (s.getType() == modelo.getType() && mesmo < 0) mesmo = i;
                else if (tipo != null && tipo.equals(tipoDeFerramenta(s.getType())) && parecido < 0) parecido = i;
            } else {
                modelo.setAmount(s.getAmount());
                if (s.isSimilar(modelo)) { mesmo = i; break; }
            }
        }
        int achou = mesmo >= 0 ? mesmo : parecido;
        if (achou < 0) return;
        inv.setItem(mao, inv.getItem(achou));
        inv.setItem(achou, null);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.4f, 1.4f);
    }

    /** "_PICKAXE", "_AXE", "_SHOVEL", "_HOE", "_SWORD", "SHEARS"... (null se não for ferramenta). */
    private static String tipoDeFerramenta(Material m) {
        String n = m.name();
        for (String suf : new String[]{"_PICKAXE", "_SHOVEL", "_HOE", "_SWORD", "_SPEAR", "_AXE"}) if (n.endsWith(suf)) return suf;
        return switch (m) {
            case SHEARS, FISHING_ROD, FLINT_AND_STEEL, BOW, CROSSBOW, TRIDENT, MACE, BRUSH -> n;
            default -> null;
        };
    }

    // =====================================================================
    //  Portas duplas
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoAbrir(PlayerInteractEvent e) {
        if (!plugin.settings().confPortas || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.useInteractedBlock() == Event.Result.DENY || e.getClickedBlock() == null || e.getPlayer().isSneaking()) return;
        Block b = e.getClickedBlock();
        if (b.getBlockData() instanceof Door d && Tag.WOODEN_DOORS.isTagged(b.getType())) {
            Block baixo = d.getHalf() == Bisected.Half.TOP ? b.getRelative(BlockFace.DOWN) : b;
            if (!(baixo.getBlockData() instanceof Door db)) return;
            boolean antes = db.isOpen();
            Block par = parDaPorta(baixo, db);
            if (par == null) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!(baixo.getBlockData() instanceof Door agora) || agora.isOpen() == antes) return; // a porta clicada não mudou
                abrirPorta(par, agora.isOpen());
            });
        } else if (b.getBlockData() instanceof Gate g) {
            boolean antes = g.isOpen();
            List<Block> vizinhos = new ArrayList<>();
            for (BlockFace lado : lados(g.getFacing())) {
                Block v = b.getRelative(lado);
                if (v.getBlockData() instanceof Gate gv && gv.getFacing().getModZ() * g.getFacing().getModZ() + gv.getFacing().getModX() * g.getFacing().getModX() != 0
                        && gv.isOpen() == antes) vizinhos.add(v);
            }
            if (vizinhos.isEmpty()) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!(b.getBlockData() instanceof Gate agora) || agora.isOpen() == antes) return;
                for (Block v : vizinhos) {
                    if (!(v.getBlockData() instanceof Gate gv)) continue;
                    gv.setOpen(agora.isOpen());
                    gv.setFacing(agora.getFacing());
                    v.setBlockData(gv, true);
                }
            });
        }
    }

    /** A outra folha: do lado, mesma direção, dobradiça do lado oposto e no mesmo estado. */
    private static Block parDaPorta(Block baixo, Door d) {
        for (BlockFace lado : lados(d.getFacing())) {
            Block v = baixo.getRelative(lado);
            if (!(v.getBlockData() instanceof Door dv) || dv.getHalf() != Bisected.Half.BOTTOM) continue;
            if (!Tag.WOODEN_DOORS.isTagged(v.getType())) continue;
            if (dv.getFacing() == d.getFacing() && dv.getHinge() != d.getHinge() && dv.isOpen() == d.isOpen()) return v;
        }
        return null;
    }

    private static BlockFace[] lados(BlockFace frente) {
        return frente == BlockFace.NORTH || frente == BlockFace.SOUTH
                ? new BlockFace[]{BlockFace.EAST, BlockFace.WEST} : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH};
    }

    private static void abrirPorta(Block baixo, boolean aberta) {
        for (Block parte : new Block[]{baixo, baixo.getRelative(BlockFace.UP)}) {
            if (!(parte.getBlockData() instanceof Door d)) continue;
            d.setOpen(aberta);
            parte.setBlockData(d, false);
        }
        baixo.getWorld().playSound(baixo.getLocation().add(0.5, 0.5, 0.5),
                aberta ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE, 0.8f, 1f);
    }
}
