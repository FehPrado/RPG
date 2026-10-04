package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enciclopédia do Mundo (/enciclopedia): biomas visitados, estruturas encontradas, itens
 * conhecidos, relíquias, constelações e monstros. Cada bioma novo dá +0,2% de XP em tudo
 * (até +15%), e os marcos valem títulos.
 */
public final class Enciclopedia implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x8D6E63);
    private static final int S_BIOMAS = 10, S_ESTRUTURAS = 12, S_ITENS = 14, S_RELIQUIAS = 16, S_ESTRELAS = 20, S_MONSTROS = 24, S_FECHAR = 31;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kBiomas, kEstruturas, kItens;
    private int totalBiomas = -1, totalEstruturas = -1, totalItens = -1;

    public Enciclopedia(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kBiomas = new NamespacedKey(plugin, "enc_biomas");
        this.kEstruturas = new NamespacedKey(plugin, "enc_estruturas");
        this.kItens = new NamespacedKey(plugin, "enc_itens");
    }

    // =====================================================================
    //  Registro
    // =====================================================================

    private static Set<String> lista(PersistentDataContainer pdc, NamespacedKey k) {
        String s = pdc.getOrDefault(k, PersistentDataType.STRING, "");
        Set<String> r = new LinkedHashSet<>();
        if (!s.isEmpty()) r.addAll(Arrays.asList(s.split(",")));
        return r;
    }

    private static boolean juntar(PersistentDataContainer pdc, NamespacedKey k, String valor) {
        Set<String> s = lista(pdc, k);
        if (!s.add(valor)) return false;
        pdc.set(k, PersistentDataType.STRING, String.join(",", s));
        return true;
    }

    public int biomas(Player p) { return lista(p.getPersistentDataContainer(), kBiomas).size(); }
    public int estruturas(Player p) { return lista(p.getPersistentDataContainer(), kEstruturas).size(); }

    private BitSet itens(Player p) {
        long[] l = p.getPersistentDataContainer().get(kItens, PersistentDataType.LONG_ARRAY);
        return l == null ? new BitSet() : BitSet.valueOf(l);
    }

    public int quantosItens(Player p) { return itens(p).cardinality(); }

    public double multiplicadorXp(Player p) {
        return 1 + Math.min(0.15, 0.002 * biomas(p));
    }

    private static String bonito(String chave) {
        String s = chave.contains(":") ? chave.substring(chave.indexOf(':') + 1) : chave;
        s = s.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** A cada 2 segundos: bioma e estrutura onde cada jogador está. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.masmorras().ehMundo(p.getWorld())) continue;
            Location l = p.getLocation();
            String bioma = l.getBlock().getBiome().getKey().toString();
            if (juntar(p.getPersistentDataContainer(), kBiomas, bioma)) {
                int n = biomas(p);
                plugin.titulos().definirMinimo(p, "biomas", n);
                p.sendActionBar(Component.text("✎ Novo bioma na Enciclopédia: " + bonito(bioma) + " (" + n + ")", COR));
                p.playSound(l, Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
            }
            for (GeneratedStructure g : p.getWorld().getStructures(l.getBlockX() >> 4, l.getBlockZ() >> 4)) {
                if (!g.getBoundingBox().contains(l.toVector())) continue;
                var chave = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).getKey(g.getStructure());
                if (chave == null) continue;
                String id = chave.toString();
                if (juntar(p.getPersistentDataContainer(), kEstruturas, id)) {
                    int n = estruturas(p);
                    plugin.titulos().definirMinimo(p, "estruturas", n);
                    p.sendMessage(Component.text("✎ Estrutura descoberta: " + bonito(id) + " (" + n + " na Enciclopédia)", COR));
                    p.playSound(l, Sound.UI_TOAST_IN, 1f, 1f);
                }
            }
        }
    }

    private void conhecer(Player p, Material m) {
        if (m == null || m.isAir()) return;
        BitSet b = itens(p);
        if (b.get(m.ordinal())) return;
        b.set(m.ordinal());
        p.getPersistentDataContainer().set(kItens, PersistentDataType.LONG_ARRAY, b.toLongArray());
        int n = b.cardinality();
        if (n % 50 == 0) {
            plugin.titulos().definirMinimo(p, "itens_conhecidos", n);
            p.sendActionBar(Component.text("✎ " + n + " itens conhecidos na Enciclopédia!", COR));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPegar(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) conhecer(p, e.getItem().getItemStack().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCriar(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) conhecer(p, e.getRecipe().getResult().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoTirarDaFornalha(FurnaceExtractEvent e) {
        conhecer(e.getPlayer(), e.getItemType());
    }

    // =====================================================================
    //  /enciclopedia
    // =====================================================================

    private void contarTotais() {
        if (totalBiomas >= 0) return;
        try {
            totalBiomas = (int) RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream().count();
            totalEstruturas = (int) RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).stream().count();
        } catch (RuntimeException ex) {
            totalBiomas = 0;
            totalEstruturas = 0;
        }
        int n = 0;
        for (Material m : Material.values()) if (m.isItem() && !m.isAir()) n++;
        totalItens = n;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        contarTotais();
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 36, Component.text("✎ Enciclopédia do Mundo"));
        Inventory inv = t.inventario;
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Set<String> bs = lista(pdc, kBiomas), es = lista(pdc, kEstruturas);
        inv.setItem(S_BIOMAS, item(Material.GRASS_BLOCK, "Biomas", bs.size(), totalBiomas, nomes(bs),
                "+" + String.format("%.1f", (multiplicadorXp(p) - 1) * 100) + "% de XP em tudo (0,2% por bioma)"));
        inv.setItem(S_ESTRUTURAS, item(Material.MOSSY_STONE_BRICKS, "Estruturas", es.size(), totalEstruturas, nomes(es), "Entre nelas para registrar"));
        inv.setItem(S_ITENS, item(Material.CHEST, "Itens conhecidos", quantosItens(p), totalItens, List.of(), "Pegue, crie ou funda itens novos"));
        Set<Reliquia> rel = plugin.arqueologia().colecao(p);
        List<String> nomesRel = new ArrayList<>();
        for (Reliquia r : Reliquia.values()) nomesRel.add((rel.contains(r) ? "✔ " : "✖ ") + (rel.contains(r) ? r.nome() : "???"));
        inv.setItem(S_RELIQUIAS, item(Material.BRUSH, "Relíquias", rel.size(), Reliquia.values().length, nomesRel, "Pincel nos sítios de arqueologia"));
        inv.setItem(S_ESTRELAS, item(Material.SPYGLASS, "Constelações", plugin.astronomia().vistas(p).size(), 12, List.of(), "Veja em /estrelas"));
        inv.setItem(S_MONSTROS, item(Material.ZOMBIE_HEAD, "Monstros", -1, -1, List.of(), "Veja em /bestiario"));
        inv.setItem(S_FECHAR, item(Material.BARRIER, "Fechar", -1, -1, List.of(), null));
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
    }

    private static List<String> nomes(Set<String> s) {
        List<String> l = new ArrayList<>();
        List<String> todos = new ArrayList<>(s);
        int inicio = Math.max(0, todos.size() - 12);
        for (int i = todos.size() - 1; i >= inicio; i--) l.add("• " + bonito(todos.get(i)));
        if (todos.size() > 12) l.add("... e mais " + (todos.size() - 12));
        return l;
    }

    private static ItemStack item(Material m, String titulo, int tem, int total, List<String> linhas, String dica) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(Component.text(titulo + (tem >= 0 ? " (" + tem + (total > 0 ? "/" + total : "") + ")" : ""), COR, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String s : linhas) lore.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            if (dica != null) lore.add(Component.text(dica, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        switch (e.getSlot()) {
            case S_FECHAR -> p.closeInventory();
            case S_ESTRELAS -> { p.closeInventory(); p.performCommand("estrelas"); }
            case S_MONSTROS -> { p.closeInventory(); p.performCommand("bestiario"); }
            default -> { }
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }
}
