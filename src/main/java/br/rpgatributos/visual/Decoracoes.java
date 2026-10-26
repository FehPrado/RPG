package br.rpgatributos.visual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.masmorra.Tema;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joml.Matrix4f;
import java.util.*;

/** Fabricação, colocação e recuperação de ornamentos persistentes, sem substituir blocos. */
public final class Decoracoes implements Listener {
    private final RPGAtributos plugin;
    private static final NamespacedKey K_ITEM = new NamespacedKey("rpgatributos", "decoracao");
    private static final NamespacedKey K_DONO = new NamespacedKey("rpgatributos", "decoracao_dono");
    private static final NamespacedKey K_LIGADO = new NamespacedKey("rpgatributos", "decoracao_ligado");
    public static final NamespacedKey K_RARIDADE = new NamespacedKey("rpgatributos", "bau_raridade_visual");
    private static final NamespacedKey K_ABERTO = new NamespacedKey("rpgatributos", "bau_visual_aberto");
    private static final int[] SLOTS = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37};
    private static final class Tela implements InventoryHolder {
        Inventory inv; Tema tema = Tema.CRIPTA;
        public Inventory getInventory() { return inv; }
    }
    public Decoracoes(RPGAtributos plugin) { this.plugin = plugin; }
    public static String codigo(ObjetoDecorativo o, Tema t) { return o.id() + ":" + t.name(); }
    public static ObjetoDecorativo objeto(String codigo) { return codigo == null ? null : ObjetoDecorativo.porId(codigo.split(":")[0]); }
    public static Tema tema(String codigo) {
        try { return Tema.valueOf(codigo.split(":")[1]); } catch (RuntimeException e) { return Tema.CRIPTA; }
    }
    public static ItemStack item(ObjetoDecorativo o, Tema t) {
        ItemStack i = AnimacoesMagicas.item(ArtesVisuais.id(o, t));
        i.editMeta(m -> {
            m.itemName(Component.text(o.nome, net.kyori.adventure.text.format.TextColor.color(ArtesVisuais.cor(t))));
            m.lore(List.of(Component.text(t.nome(), NamedTextColor.GRAY), Component.text("Clique num bloco para colocar.", NamedTextColor.YELLOW),
                    Component.text("Agache e clique para recolher; clique para girar.", NamedTextColor.GRAY),
                    Component.text("Ornamento: não guarda itens nem causa dano.", NamedTextColor.DARK_GRAY)));
            m.getPersistentDataContainer().set(K_ITEM, PersistentDataType.STRING, codigo(o, t));
        }); return i;
    }
    public void abrir(Player p) {
        Tela t = new Tela(); t.inv = Bukkit.createInventory(t, 54, Embelezamento.titulo(plugin, p, br.rpgatributos.ajuda.Fundo.DECORACOES, "✦ Decoração e cenários")); desenhar(p, t); p.openInventory(t.inv);
    }
    private void desenhar(Player p, Tela t) {
        t.inv.clear();
        for (Tema tema : Tema.values()) {
            ItemStack icone = new ItemStack(tema == t.tema ? Material.ENCHANTED_BOOK : Material.BOOK);
            icone.editMeta(m -> m.itemName(Component.text(tema.nome(), net.kyori.adventure.text.format.TextColor.color(ArtesVisuais.cor(tema)))));
            t.inv.setItem(1 + tema.ordinal() * 2, icone);
        }
        for (int n = 0; n < ObjetoDecorativo.values().length; n++) {
            ObjetoDecorativo o = ObjetoDecorativo.values()[n]; ItemStack i = item(o, t.tema);
            i.editMeta(m -> {
                var lore = new ArrayList<>(m.lore()); lore.add(Component.empty());
                lore.add(p.getGameMode() == GameMode.CREATIVE ? Component.text("Clique: pegar (Criativo)", NamedTextColor.AQUA)
                        : Component.text("Clique: fabricar por " + o.quantidade + " × ", NamedTextColor.AQUA).append(Component.translatable(o.custo.translationKey()))); m.lore(lore);
            }); t.inv.setItem(SLOTS[n], i);
        }
        ItemStack ajuda = new ItemStack(Material.PAPER); ajuda.editMeta(m -> {
            m.itemName(Component.text("Decore seu mundo", NamedTextColor.GOLD));
            m.lore(List.of(Component.text("Troque o tema nas abas acima.", NamedTextColor.GRAY), Component.text("Renomeie placas e estandartes na bigorna", NamedTextColor.GRAY), Component.text("para mostrar nomes de lojas e reinos.", NamedTextColor.GRAY)));
        }); t.inv.setItem(49, ajuda);
    }
    @EventHandler(ignoreCancelled = true)
    public void clicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true); if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= 54 || e.getRawSlot() < 0) return;
        int slot = e.getRawSlot();
        for (Tema tema : Tema.values()) if (slot == 1 + tema.ordinal() * 2) { t.tema = tema; desenhar(p, t); return; }
        for (int n = 0; n < SLOTS.length; n++) if (slot == SLOTS[n]) {
            ObjetoDecorativo o = ObjetoDecorativo.values()[n];
            if (p.getInventory().firstEmpty() < 0) { p.sendMessage(Component.text("Abra espaço no inventário.", NamedTextColor.RED)); return; }
            if (p.getGameMode() != GameMode.CREATIVE) {
                ItemStack custo = new ItemStack(o.custo, o.quantidade);
                if (!p.getInventory().containsAtLeast(custo, o.quantidade)) { p.sendMessage(Component.text("Faltam os materiais indicados.", NamedTextColor.RED)); return; }
                p.getInventory().removeItem(custo);
            }
            p.getInventory().addItem(item(o, t.tema)); p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, .6f, 1.3f); return;
        }
    }
    @EventHandler public void arrastar(InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true)
    public void tesouro(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p) || !(e.getInventory().getHolder(false) instanceof org.bukkit.block.Chest bau)) return;
        if (bau.getPersistentDataContainer().getOrDefault(K_RARIDADE, PersistentDataType.INTEGER, 0) < 2 || bau.getPersistentDataContainer().has(K_ABERTO)) return;
        bau.getPersistentDataContainer().set(K_ABERTO, PersistentDataType.BYTE, (byte) 1); bau.update();
        plugin.animacoesMagicas().invocacao(p, bau.getLocation().add(.5, .8, .5), java.util.EnumSet.of(br.rpgatributos.arcano.Essencia.ENERGIA));
    }
    public static void ambiente(Player p) {
        int n = 0;
        for (Entity e : p.getNearbyEntities(12, 6, 12)) if (e instanceof ItemDisplay d && d.getPersistentDataContainer().has(K_ITEM)) {
            ObjetoDecorativo o = objeto(d.getPersistentDataContainer().get(K_ITEM, PersistentDataType.STRING));
            if (o == ObjetoDecorativo.BRASEIRO) p.spawnParticle(Particle.FLAME, d.getLocation().add(0, 1, 0), 2, .15, .1, .15, .005);
            if (o == ObjetoDecorativo.CRISTAL) p.spawnParticle(Particle.END_ROD, d.getLocation().add(0, .8, 0), 1, .2, .3, .2, .005);
            if (++n >= 6) break;
        }
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void colocar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null) return;
        String codigo = e.getItem().getPersistentDataContainer().get(K_ITEM, PersistentDataType.STRING);
        ObjetoDecorativo o = objeto(codigo); if (o == null) return; e.setCancelled(true);
        Player p = e.getPlayer(); var bloco = e.getClickedBlock().getRelative(e.getBlockFace()); Location l = bloco.getLocation().add(.5, 0, .5);
        if (plugin.masmorras().ehMundo(p.getWorld()) || !bloco.isPassable()) return;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!plugin.territorios().podeConstruir(p, bloco.getRelative(dx, 0, dz))) return;
        long quantidade = Arrays.stream(bloco.getChunk().getEntities()).filter(ent -> ent instanceof ItemDisplay && ent.getPersistentDataContainer().has(K_ITEM)).count();
        if (quantidade >= 48) { p.sendMessage(Component.text("Já há muitos ornamentos neste trecho.", NamedTextColor.RED)); return; }
        l.setYaw(Math.round(p.getLocation().getYaw() / 45) * 45f);
        ItemStack colocado = e.getItem().asOne();
        ItemDisplay d = criar(l, o, tema(codigo), true);
        d.getPersistentDataContainer().set(K_DONO, PersistentDataType.STRING, p.getUniqueId().toString()); d.setItemStack(colocado);
        Interaction interacao = l.getWorld().spawn(l, Interaction.class, i -> { i.setInteractionWidth(1); i.setInteractionHeight(o == ObjetoDecorativo.PORTICO ? 2 : 1.3f); i.setPersistent(true); });
        d.getPersistentDataContainer().set(K_LIGADO, PersistentDataType.STRING, interacao.getUniqueId().toString());
        interacao.getPersistentDataContainer().set(K_LIGADO, PersistentDataType.STRING, d.getUniqueId().toString());
        if (o == ObjetoDecorativo.PLACA || o == ObjetoDecorativo.BANDEIRA) {
            Component nome = colocado.getItemMeta().hasDisplayName() ? colocado.getItemMeta().displayName() : Component.text(o.nome + " de " + p.getName());
            TextDisplay texto = l.getWorld().spawn(l.clone().add(0, o == ObjetoDecorativo.BANDEIRA ? 1.8 : 1.3, 0), TextDisplay.class, t -> { t.text(nome); t.setBillboard(Display.Billboard.CENTER); t.setPersistent(true); t.setLineWidth(120); t.setSeeThrough(false); });
            texto.getPersistentDataContainer().set(K_LIGADO, PersistentDataType.STRING, d.getUniqueId().toString());
        }
        if (p.getGameMode() != GameMode.CREATIVE) { ItemStack resto = e.getItem().clone(); resto.setAmount(resto.getAmount() - 1); p.getInventory().setItemInMainHand(resto.isEmpty() ? null : resto); }
        p.playSound(l, Sound.BLOCK_WOOD_PLACE, 1, 1);
    }
    public static ItemDisplay criar(Location l, ObjetoDecorativo o, Tema t, boolean persistente) {
        return l.getWorld().spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(item(o, t)); d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE); d.setPersistent(persistente); d.setInvulnerable(true); d.setGravity(false);
            d.setTransformationMatrix(new Matrix4f().translate(-.5f, 0, -.5f));
            d.getPersistentDataContainer().set(K_ITEM, PersistentDataType.STRING, codigo(o, t));
            if (o == ObjetoDecorativo.CRISTAL || o == ObjetoDecorativo.BRASEIRO || o == ObjetoDecorativo.LANTERNA) d.setBrightness(new Display.Brightness(15, 15));
        });
    }
    @EventHandler(ignoreCancelled = true)
    public void interagir(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        String id = e.getRightClicked().getPersistentDataContainer().get(K_LIGADO, PersistentDataType.STRING); if (id == null) return;
        Entity ent; try { ent = Bukkit.getEntity(UUID.fromString(id)); } catch (IllegalArgumentException ex) { return; }
        if (!(ent instanceof ItemDisplay d)) return;
        String dono = d.getPersistentDataContainer().get(K_DONO, PersistentDataType.STRING); if (dono == null) return;
        e.setCancelled(true); Player p = e.getPlayer();
        if ((!dono.equals(p.getUniqueId().toString()) && !p.hasPermission("rpg.admin")) || !plugin.territorios().podeConstruir(p, d.getLocation())) return;
        if (p.isSneaking()) {
            ItemStack item = d.getItemStack().asOne();
            for (Entity ligado : d.getNearbyEntities(2, 3, 2)) if (d.getUniqueId().toString().equals(ligado.getPersistentDataContainer().get(K_LIGADO, PersistentDataType.STRING))) ligado.remove();
            d.remove(); p.getInventory().addItem(item).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        } else d.setRotation(d.getLocation().getYaw() + 45, 0);
    }
}
