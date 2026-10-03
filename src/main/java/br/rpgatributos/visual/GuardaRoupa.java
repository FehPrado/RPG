package br.rpgatributos.visual;

import br.rpgatributos.RPGAtributos;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda-roupa: muda só a APARÊNCIA da armadura vestida, sem tirar a armadura de verdade.
 * Usa o componente de equipamento do próprio jogo (o visual é igual para todos, sem mod).
 * A aparência fica só enquanto a peça está vestida: ao tirar, o item volta ao normal.
 */
public final class GuardaRoupa implements Listener {

    private static final TextColor COR = TextColor.color(0xFF8FD7);
    /** Ordem dos espaços: cabeça, peito, pernas e pés. */
    private static final EquipmentSlot[] PARTES = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NOMES = {"Cabeça", "Peito", "Pernas", "Pés"};
    private static final Material[] ICONES = {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS};
    /** Componentes que mudam a aparência (e voltam ao normal ao tirar). */
    private static final List<DataComponentType> VISUAIS = List.of(DataComponentTypes.EQUIPPABLE, DataComponentTypes.DYED_COLOR,
            DataComponentTypes.TRIM, DataComponentTypes.ITEM_MODEL, DataComponentTypes.PROFILE);
    private static final int S_INFO = 4;
    private static final int S_LIGADO = 48;
    private static final int S_VOLTAR = 49;
    private static final int S_FECHAR = 50;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kRoupas, kOcultos, kDesligado;
    private static NamespacedKey kVisual, kBackup;
    private final Set<UUID> pendentes = new HashSet<>();

    public GuardaRoupa(RPGAtributos plugin) {
        this.plugin = plugin;
        kRoupas = new NamespacedKey(plugin, "guarda_roupa");
        kOcultos = new NamespacedKey(plugin, "guarda_roupa_oculto");
        kDesligado = new NamespacedKey(plugin, "guarda_roupa_desligado");
        kVisual = new NamespacedKey(plugin, "visual");
        kBackup = new NamespacedKey(plugin, "visual_original");
    }

    // =====================================================================
    //  Dados do jogador
    // =====================================================================

    private ItemStack[] roupas(Player p) {
        ItemStack[] r = new ItemStack[PARTES.length];
        byte[] bytes = p.getPersistentDataContainer().get(kRoupas, PersistentDataType.BYTE_ARRAY);
        if (bytes != null) {
            ItemStack[] lidos = ItemStack.deserializeItemsFromBytes(bytes);
            for (int i = 0; i < lidos.length && i < r.length; i++) r[i] = lidos[i] == null || lidos[i].isEmpty() ? null : lidos[i];
        }
        return r;
    }

    private void salvarRoupas(Player p, ItemStack[] r) {
        ItemStack[] copia = new ItemStack[r.length];
        for (int i = 0; i < r.length; i++) copia[i] = r[i] == null ? ItemStack.empty() : r[i];
        p.getPersistentDataContainer().set(kRoupas, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(copia));
    }

    private int ocultos(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kOcultos, PersistentDataType.INTEGER, 0);
    }

    private boolean ligado(Player p) {
        return !p.getPersistentDataContainer().has(kDesligado);
    }

    /** Qual aparência a peça desse espaço deve ter: null (a dela), "oculto" ou "item:<código>". */
    private String desejado(Player p, int i, ItemStack[] roupas, int ocultos) {
        if (!ligado(p)) return null;
        if ((ocultos & (1 << i)) != 0) return "oculto";
        if (roupas[i] == null) return null;
        return "item:" + Integer.toHexString(Arrays.hashCode(roupas[i].serializeAsBytes()));
    }

    // =====================================================================
    //  Aplicar e desfazer a aparência
    // =====================================================================

    /** Confere as 4 peças vestidas e aplica (ou tira) a aparência escolhida. */
    public void atualizar(Player p) {
        EntityEquipment eq = p.getEquipment();
        ItemStack[] roupas = roupas(p);
        int ocultos = ocultos(p);
        for (int i = 0; i < PARTES.length; i++) {
            ItemStack vestido = eq.getItem(PARTES[i]);
            if (vestido.isEmpty()) continue;
            String quero = desejado(p, i, roupas, ocultos);
            String tem = vestido.getPersistentDataContainer().get(kVisual, PersistentDataType.STRING);
            if (Objects.equals(quero, tem)) continue;
            ItemStack novo = vestido.clone();
            reverter(novo);
            if (quero != null) aplicar(novo, PARTES[i], quero.equals("oculto") ? null : roupas[i], quero);
            if (!novo.equals(vestido)) eq.setItem(PARTES[i], novo);
        }
    }

    private static void aplicar(ItemStack vestido, EquipmentSlot parte, ItemStack roupa, String assinatura) {
        // Só armaduras de verdade (chapéus do /chapeu continuam aparecendo como são).
        Equippable base = vestido.getData(DataComponentTypes.EQUIPPABLE);
        if (base == null) return;
        byte[] original = vestido.serializeAsBytes();
        if (roupa == null) {
            vestido.setData(DataComponentTypes.EQUIPPABLE, base.toBuilder().assetId(null).build());
        } else {
            Equippable eqRoupa = roupa.getData(DataComponentTypes.EQUIPPABLE);
            Key aparencia = eqRoupa == null ? null : eqRoupa.assetId();
            if (aparencia != null) {
                vestido.setData(DataComponentTypes.EQUIPPABLE, base.toBuilder().assetId(aparencia).build());
                copiar(roupa, vestido, DataComponentTypes.DYED_COLOR);
                copiar(roupa, vestido, DataComponentTypes.TRIM);
            } else if (parte == EquipmentSlot.HEAD) {
                // Qualquer item na cabeça: a peça some e o modelo do item aparece no lugar.
                vestido.setData(DataComponentTypes.EQUIPPABLE, base.toBuilder().assetId(null).build());
                Key modelo = roupa.getData(DataComponentTypes.ITEM_MODEL);
                if (modelo != null) vestido.setData(DataComponentTypes.ITEM_MODEL, modelo);
                copiar(roupa, vestido, DataComponentTypes.PROFILE);
            } else {
                return;
            }
        }
        vestido.editPersistentDataContainer(pdc -> {
            pdc.set(kVisual, PersistentDataType.STRING, assinatura);
            pdc.set(kBackup, PersistentDataType.BYTE_ARRAY, original);
        });
    }

    private static void copiar(ItemStack de, ItemStack para, DataComponentType tipo) {
        if (de.hasData(tipo)) para.copyDataFrom(de, t -> t.equals(tipo));
        else para.unsetData(tipo);
    }

    /** O item tem uma aparência do guarda-roupa aplicada? */
    public static boolean temVisual(ItemStack item) {
        return item != null && !item.isEmpty() && kBackup != null && item.getPersistentDataContainer().has(kBackup);
    }

    /** Desfaz a aparência: o item volta a ser exatamente como era (só a parte visual muda). */
    public static void reverter(ItemStack item) {
        if (!temVisual(item)) return;
        byte[] bytes = item.getPersistentDataContainer().get(kBackup, PersistentDataType.BYTE_ARRAY);
        ItemStack original;
        try {
            original = ItemStack.deserializeBytes(bytes);
        } catch (RuntimeException e) {
            original = null;
        }
        for (DataComponentType t : VISUAIS) {
            if (original != null && original.isDataOverridden(t)) {
                if (original.hasData(t)) item.copyDataFrom(original, x -> x.equals(t));
                else item.unsetData(t);
            } else {
                item.resetData(t);
            }
        }
        item.editPersistentDataContainer(pdc -> {
            pdc.remove(kVisual);
            pdc.remove(kBackup);
        });
    }

    /** Peças com aparência que saíram do corpo (inventário, cursor, mão) voltam ao normal. */
    public void limpar(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (i >= 36 && i <= 39) continue; // armadura vestida
            ItemStack s = inv.getItem(i);
            if (!temVisual(s)) continue;
            ItemStack c = s.clone();
            reverter(c);
            inv.setItem(i, c);
        }
        ItemStack cursor = p.getItemOnCursor();
        if (temVisual(cursor)) {
            ItemStack c = cursor.clone();
            reverter(c);
            p.setItemOnCursor(c);
        }
    }

    private static void limpar(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!temVisual(s)) continue;
            ItemStack c = s.clone();
            reverter(c);
            inv.setItem(i, c);
        }
    }

    private void agendar(Player p) {
        if (!pendentes.add(p.getUniqueId())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendentes.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            atualizar(p);
            limpar(p);
        });
    }

    /** A cada 2 segundos, por garantia. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            atualizar(p);
            limpar(p);
        }
    }

    /** Ao desligar o plugin: tudo volta ao normal (nada fica "preso" com outra aparência). */
    public void desfazerTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            EntityEquipment eq = p.getEquipment();
            for (EquipmentSlot s : PARTES) {
                ItemStack v = eq.getItem(s);
                if (!temVisual(v)) continue;
                ItemStack c = v.clone();
                reverter(c);
                eq.setItem(s, c);
            }
            limpar(p);
        }
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler
    public void aoMudarEquipamento(EntityEquipmentChangedEvent e) {
        if (e.getEntity() instanceof Player p) agendar(p);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        agendar(e.getPlayer());
    }

    @EventHandler
    public void aoFechar(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Inventory topo = e.getView().getTopInventory();
        if (topo.getHolder(false) instanceof InventoryHolder h && !(h instanceof Player)) limpar(topo);
        agendar(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoJogar(PlayerDropItemEvent e) {
        ItemStack s = e.getItemDrop().getItemStack();
        if (!temVisual(s)) return;
        reverter(s);
        e.getItemDrop().setItemStack(s);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoMorrer(PlayerDeathEvent e) {
        for (ItemStack s : e.getDrops()) reverter(s);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Guarda-roupa"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        ItemStack[] roupas = roupas(p);
        int ocultos = ocultos(p);
        boolean ligado = ligado(p);
        inv.setItem(S_INFO, item(Material.ARMOR_STAND, Component.text("✦ Guarda-roupa", COR, TextDecoration.BOLD), List.of(
                Component.text("Muda a aparência da sua armadura", NamedTextColor.GRAY),
                Component.text("sem tirar a armadura de verdade.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Clique numa peça do seu inventário", NamedTextColor.WHITE),
                Component.text("para usar como aparência.", NamedTextColor.WHITE),
                Component.text("Na cabeça vale qualquer item!", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Só aparece por cima de uma peça vestida.", NamedTextColor.DARK_GRAY)), false));
        EntityEquipment eq = p.getEquipment();
        for (int i = 0; i < PARTES.length; i++) {
            int base = 10 + i * 9;
            inv.setItem(base, item(ICONES[i], Component.text(NOMES[i], COR, TextDecoration.BOLD), List.of(), false));
            if (roupas[i] == null) {
                inv.setItem(base + 2, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Sem aparência", NamedTextColor.GRAY), List.of(
                        Component.text("Clique numa peça do seu inventário.", NamedTextColor.DARK_GRAY)), false));
            } else {
                ItemStack mostra = roupas[i].clone();
                String parte = NOMES[i].toLowerCase(java.util.Locale.ROOT);
                mostra.editMeta(m -> {
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Aparência de " + parte, COR).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text("» Clique para devolver", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    m.lore(lore);
                    m.addItemFlags(ItemFlag.values());
                });
                inv.setItem(base + 2, mostra);
            }
            boolean oculto = (ocultos & (1 << i)) != 0;
            inv.setItem(base + 4, item(oculto ? Material.ENDER_PEARL : Material.ENDER_EYE,
                    Component.text(oculto ? "Peça escondida" : "Peça visível", oculto ? NamedTextColor.RED : NamedTextColor.GREEN, TextDecoration.BOLD),
                    List.of(Component.text(oculto ? "A peça vestida fica invisível." : "Mostra a peça (ou a aparência escolhida).", NamedTextColor.GRAY),
                            Component.text("» Clique para " + (oculto ? "mostrar" : "esconder"), NamedTextColor.YELLOW)), false));
            ItemStack vestido = eq.getItem(PARTES[i]);
            if (vestido.isEmpty()) {
                inv.setItem(base + 6, item(Material.GLASS_PANE, Component.text("Nada vestido", NamedTextColor.DARK_GRAY), List.of(), false));
            } else {
                ItemStack mostra = vestido.clone();
                reverter(mostra);
                mostra.editMeta(m -> m.lore(List.of(Component.text("Vestido agora (armadura de verdade)", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))));
                inv.setItem(base + 6, mostra);
            }
        }
        inv.setItem(S_LIGADO, item(ligado ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text(ligado ? "Guarda-roupa ligado" : "Guarda-roupa desligado", ligado ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD),
                List.of(Component.text("» Clique para " + (ligado ? "desligar" : "ligar"), NamedTextColor.YELLOW)), false));
        inv.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        boolean noMenu = e.getRawSlot() < topo.getSize();
        int slot = e.getSlot();
        if (!noMenu) {
            if (slot >= 36 && slot <= 39) {
                erro(p, "Escolha uma peça guardada no inventário (não a que está vestida).");
                return;
            }
            ItemStack clicado = e.getCurrentItem();
            if (clicado == null || clicado.isEmpty()) return;
            guardar(p, t, slot, clicado);
            return;
        }
        switch (slot) {
            case S_FECHAR -> p.closeInventory();
            case S_VOLTAR -> plugin.menus().abrirCosmeticos(p);
            case S_LIGADO -> {
                if (ligado(p)) p.getPersistentDataContainer().set(kDesligado, PersistentDataType.BYTE, (byte) 1);
                else p.getPersistentDataContainer().remove(kDesligado);
                mudou(p, t);
            }
            default -> {
                int linha = (slot - 10) / 9, coluna = (slot - 10) % 9;
                if (slot < 10 || linha >= PARTES.length) return;
                if (coluna == 2) devolver(p, t, linha);
                else if (coluna == 4) {
                    p.getPersistentDataContainer().set(kOcultos, PersistentDataType.INTEGER, ocultos(p) ^ (1 << linha));
                    mudou(p, t);
                }
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    /** Em qual espaço o item entra como aparência (-1 = nenhum). */
    private static int parteDe(ItemStack item) {
        Equippable eq = item.getData(DataComponentTypes.EQUIPPABLE);
        if (eq != null && eq.assetId() != null) {
            for (int i = 0; i < PARTES.length; i++) if (PARTES[i] == eq.slot()) return i;
        }
        return 0; // qualquer outro item vai para a cabeça
    }

    private void guardar(Player p, Tela t, int slotInv, ItemStack clicado) {
        if (temVisual(clicado)) {
            ItemStack c = clicado.clone();
            reverter(c);
            clicado = c;
        }
        int parte = parteDe(clicado);
        ItemStack[] roupas = roupas(p);
        ItemStack antiga = roupas[parte];
        roupas[parte] = clicado.asOne();
        ItemStack resto = clicado.clone();
        resto.setAmount(clicado.getAmount() - 1);
        p.getInventory().setItem(slotInv, resto.getAmount() > 0 ? resto : null);
        if (antiga != null) p.getInventory().addItem(antiga).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        salvarRoupas(p, roupas);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
        mudou(p, t);
    }

    private void devolver(Player p, Tela t, int parte) {
        ItemStack[] roupas = roupas(p);
        if (roupas[parte] == null) return;
        if (p.getInventory().firstEmpty() < 0) {
            erro(p, "Seu inventário está cheio.");
            return;
        }
        p.getInventory().addItem(roupas[parte]);
        roupas[parte] = null;
        salvarRoupas(p, roupas);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 0.9f);
        mudou(p, t);
    }

    private void mudou(Player p, Tela t) {
        atualizar(p);
        desenhar(p, t);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilhar) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }
}
