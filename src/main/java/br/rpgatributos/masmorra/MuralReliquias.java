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
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mural das Relíquias (2.74): pedra-negra polida entalhada com 1 obsidiana chorona e 1 bloco de ouro
 * jogados em cima. Guarda as Relíquias do Monarca (5 linhas de 5). Linha completa: abre o Portal
 * daquele Monarca; as 25: o do Monarca da Destruição. Fica salvo no próprio chunk.
 */
public final class MuralReliquias extends Estacao {

    static final TextColor COR = TextColor.color(0xB388FF);
    private static final int S_FECHAR = 49, S_COLOCAR = 47, S_INFO = 45;
    private static final int TODAS = (1 << 25) - 1;

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        Location mural;
        @Override public Inventory getInventory() { return inv; }
    }

    public MuralReliquias(RPGAtributos plugin) {
        super(plugin, "murais_reliquias", "mural_reliquias_display");
    }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHISELED_POLISHED_BLACKSTONE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.CRYING_OBSIDIAN || m == Material.GOLD_BLOCK;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHISELED_POLISHED_BLACKSTONE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.CRYING_OBSIDIAN) < 1 || contar(itens, Material.GOLD_BLOCK) < 1) return false;
        tirar(itens, Material.CRYING_OBSIDIAN, 1);
        tirar(itens, Material.GOLD_BLOCK, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ECHO_SHARD);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.6;
    }

    @Override
    protected Component nome() {
        return Component.text("♛ Mural das Relíquias ♛", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.SCULK_SOUL, c, 30, 0.4, 0.4, 0.4, 0.02);
        c.getWorld().playSound(c, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.6f, 0.8f);
        if (quem != null) quem.sendMessage(Component.text("♛ Mural das Relíquias pronto. Junte as Relíquias do Monarca (caem nos portais A e S).", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 8 == 0) centro.getWorld().spawnParticle(Particle.SCULK_SOUL, centro, 1, 0.2, 0.2, 0.2, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.CRYING_OBSIDIAN));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GOLD_BLOCK));
    }

    /** O mural deixou de existir: as relíquias que estavam nele caem no chão (nunca se perdem). */
    @Override
    protected void aoRemover(Location bloco) {
        int mask = mascara(bloco);
        if (mask == 0) return;
        Location c = bloco.clone().add(0.5, 0.5, 0.5);
        for (int bit = 0; bit < 25; bit++) {
            if ((mask & (1 << bit)) != 0) c.getWorld().dropItemNaturally(c, Reliquias.criar(Monarca.values()[bit / 5], bit % 5));
        }
        gravar(bloco, 0);
    }

    // =====================================================================
    //  O que está no mural (no PDC do chunk)
    // =====================================================================

    private static NamespacedKey chave(Location l) {
        return new NamespacedKey("rpgatributos", "mural_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    static int mascara(Location l) {
        return l.getChunk().getPersistentDataContainer().getOrDefault(chave(l), PersistentDataType.INTEGER, 0);
    }

    static void gravar(Location l, int mask) {
        if (mask == 0) l.getChunk().getPersistentDataContainer().remove(chave(l));
        else l.getChunk().getPersistentDataContainer().set(chave(l), PersistentDataType.INTEGER, mask);
    }

    private static int linha(Monarca m) {
        return 0b11111 << (m.ordinal() * 5);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.mural = b.getLocation();
        t.inv = Bukkit.createInventory(t, 54, Component.text("♛ Mural das Relíquias"));
        desenhar(p, t);
        p.openInventory(t.inv);
        p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 1f, 0.8f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    private static int tem(Player p, Reliquias.Reliquia r) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            Reliquias.Reliquia x = Reliquias.de(s);
            if (x != null && x.equals(r)) n += s.getAmount();
        }
        return n;
    }

    private static boolean tirar(Player p, Reliquias.Reliquia r) {
        ItemStack[] cont = p.getInventory().getStorageContents();
        for (int k = 0; k < cont.length; k++) {
            Reliquias.Reliquia x = Reliquias.de(cont[k]);
            if (x == null || !x.equals(r)) continue;
            cont[k].setAmount(cont[k].getAmount() - 1);
            p.getInventory().setStorageContents(cont);
            return true;
        }
        return false;
    }

    private void desenhar(Player p, Tela t) {
        t.inv.clear();
        int mask = mascara(t.mural);
        for (Monarca m : Monarca.values()) {
            int base = m.ordinal() * 9;
            String dono = donoDe(m);
            List<Component> info = new ArrayList<>();
            if (m.jogavel()) {
                info.add(Component.text("Classe lendária: " + (dono == null ? "livre" : "de " + dono), dono == null ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                info.add(Component.text("Passivo: " + m.classe().passivo().texto(), NamedTextColor.GRAY));
                for (br.rpgatributos.classe.Habilidade h : m.classe().habilidades()) info.add(Component.text(" ✦ " + h.nome(), NamedTextColor.WHITE));
            } else {
                info.add(Component.text("O chefe final: pede as 25 relíquias.", NamedTextColor.GRAY));
            }
            t.inv.setItem(base, item(m.icone, Component.text(m.nome, m.cor, TextDecoration.BOLD), info));
            for (int i = 0; i < 5; i++) {
                Reliquias.Reliquia r = new Reliquias.Reliquia(m, i);
                boolean posta = (mask & (1 << r.bit())) != 0;
                ItemStack icone;
                if (posta) {
                    icone = Reliquias.criar(m, i);
                    icone.editMeta(meta -> meta.lore(List.of(Component.text("✔ No mural", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))));
                } else {
                    int n = tem(p, r);
                    icone = item(n > 0 ? Material.ECHO_SHARD : Material.GRAY_DYE, Component.text(r.nome(), n > 0 ? m.cor : NamedTextColor.DARK_GRAY),
                            List.of(Component.text("Relíquia " + (i + 1) + "/5 do " + m.nome, NamedTextColor.GRAY),
                                    Component.text(n > 0 ? "» Clique para colocar (você tem " + n + ")" : "Ainda não está com você.", n > 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)));
                }
                t.inv.setItem(base + 1 + i, icone);
            }
            boolean pronta = m.jogavel() ? (mask & linha(m)) == linha(m) : mask == TODAS;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(m.jogavel() ? "Pede as 5 relíquias desta linha (elas se gastam)." : "Pede as 25 relíquias do mural (todas se gastam).", NamedTextColor.GRAY));
            lore.add(Component.text("Portal de rank S, 25 minutos. Caçadores A ou S.", NamedTextColor.GRAY));
            if (m.jogavel()) lore.add(Component.text(dono == null ? "Quem der o golpe final herda a classe " + m.nome + "." : "A classe já tem dono: vencer dá só o tesouro.", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(pronta ? Component.text("» Clique para abrir o portal aqui", NamedTextColor.YELLOW) : Component.text("Faltam relíquias.", NamedTextColor.RED));
            ItemStack botao = item(pronta ? Material.END_CRYSTAL : Material.BLACK_STAINED_GLASS, Component.text("Portal do " + m.nome, m.cor, TextDecoration.BOLD), lore);
            if (pronta) botao.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            t.inv.setItem(base + 7, botao);
        }
        t.inv.setItem(S_INFO, item(Material.BOOK, Component.text("Relíquias do Monarca", COR, TextDecoration.BOLD), List.of(
                Component.text("Caem nos tesouros dos portais A (6%) e S (12%).", NamedTextColor.GRAY),
                Component.text("No mural: " + Integer.bitCount(mask) + "/25.", NamedTextColor.WHITE),
                Component.text("Qualquer um pode colocar; quem abre o portal leva a party.", NamedTextColor.DARK_GRAY))));
        t.inv.setItem(S_COLOCAR, item(Material.HOPPER, Component.text("Colocar todas", NamedTextColor.YELLOW, TextDecoration.BOLD),
                List.of(Component.text("Põe no mural todas as relíquias que faltam e que você tem.", NamedTextColor.GRAY))));
        t.inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < t.inv.getSize(); i++) if (t.inv.getItem(i) == null) t.inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != topo) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (!eh(t.mural.getBlock())) { p.closeInventory(); return; }
        int mask = mascara(t.mural);
        if (slot == S_COLOCAR) {
            int postas = 0;
            for (int bit = 0; bit < 25; bit++) {
                if ((mask & (1 << bit)) != 0) continue;
                Reliquias.Reliquia r = new Reliquias.Reliquia(Monarca.values()[bit / 5], bit % 5);
                if (tirar(p, r)) { mask |= 1 << bit; postas++; }
            }
            gravar(t.mural, mask);
            if (postas > 0) p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
            else erro(p, "Você não tem relíquias que faltam no mural.");
            desenhar(p, t);
            return;
        }
        int linha = slot / 9, coluna = slot % 9;
        if (linha >= Monarca.values().length) return;
        Monarca m = Monarca.values()[linha];
        if (coluna >= 1 && coluna <= 5) {
            Reliquias.Reliquia r = new Reliquias.Reliquia(m, coluna - 1);
            if ((mask & (1 << r.bit())) != 0) return;
            if (!tirar(p, r)) { erro(p, "Você não tem essa relíquia."); return; }
            gravar(t.mural, mask | (1 << r.bit()));
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
            desenhar(p, t);
            return;
        }
        if (coluna == 7) abrirPortal(p, t, m, mask);
    }

    private void abrirPortal(Player p, Tela t, Monarca m, int mask) {
        int precisa = m.jogavel() ? linha(m) : TODAS;
        if ((mask & precisa) != precisa) { erro(p, "Faltam relíquias para este portal."); return; }
        if (p.getGameMode() != GameMode.CREATIVE && Cacador.rank(p).ordinal() < Rank.A.ordinal()) {
            erro(p, "Só caçadores de Rank A ou S abrem um Portal do Monarca.");
            return;
        }
        String falha = plugin.masmorras().abrirPortalMonarca(p, m, t.mural);
        if (falha != null) { erro(p, falha); return; }
        gravar(t.mural, mask & ~precisa);
        p.closeInventory();
        t.mural.getWorld().playSound(t.mural, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.6f);
        UUID dono = m.jogavel() ? plugin.classes().donoLendaria(m.classe()) : null;
        if (dono != null && !dono.equals(p.getUniqueId())) {
            p.sendMessage(Component.text("♛ A classe " + m.nome + " já tem dono: vencer dá o tesouro, não a classe.", NamedTextColor.GRAY));
        }
    }

    /** O nome de quem tem a classe deste Monarca, ou null. */
    private String donoDe(Monarca m) {
        if (!m.jogavel() || plugin.classes().donoLendaria(m.classe()) == null) return null;
        return plugin.classes().nomeDonoLendaria(m.classe());
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static void erro(Player p, String msg) {
        p.sendActionBar(Component.text("♛ " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
