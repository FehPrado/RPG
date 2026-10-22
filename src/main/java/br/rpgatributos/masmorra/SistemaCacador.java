package br.rpgatributos.masmorra;

import br.rpgatributos.classe.Classe;
import br.rpgatributos.classe.Habilidade;
import br.rpgatributos.masmorra.ArvoreCacador.No;
import br.rpgatributos.masmorra.ArvoreCacador.Ramo;
import br.rpgatributos.masmorra.Despertar.Caracteristica;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A janela do Sistema (2.72): o Rank de Caçador, a Árvore de Caçador (clique num nó para subir) e as
 * características do Despertar. Abre com /masmorra arvore ou no Altar do Portal.
 */
public final class SistemaCacador implements Listener {

    /** Cada ramo numa linha: o ícone do ramo e os 4 nós. */
    private static final int[] INICIO_RAMO = {9, 18, 27};
    private static final int[] S_CARACTERISTICAS = {15, 24, 33};
    private static final int S_INFO = 4, S_REFAZER = 47, S_REDESPERTAR = 51, S_FECHAR = 49, S_FUNCAO = 39, S_MISSAO = 41;
    /** Refazer a árvore custa mana (de graça se nada foi gasto). */
    public static final int PRECO_REFAZER = 16;

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private final br.rpgatributos.RPGAtributos plugin;

    public SistemaCacador(br.rpgatributos.RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public void abrir(Player p) {
        ArvoreCacador.conferir(p);
        Despertar.verificar(p);
        Tela t = new Tela();
        t.inv = Bukkit.createInventory(t, 54, Component.text("[Sistema] Caçador", LojaSistema.COR));
        desenhar(p, t);
        p.openInventory(t.inv);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.1f);
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

    private void desenhar(Player p, Tela t) {
        t.inv.clear();
        Rank r = Cacador.rank(p);
        int livres = ArvoreCacador.livres(p), pontos = ArvoreCacador.pontos(p);
        t.inv.setItem(S_INFO, item(Material.NAME_TAG, Component.text("[Sistema] Caçador " + r.nome(), r.cor(), TextDecoration.BOLD), List.of(
                Component.text(Cacador.xp(p) + " XP de caçador" + (r == Rank.S ? "" : " (faltam " + Cacador.falta(p) + " para " + r.acima().nome() + ")"), NamedTextColor.GRAY),
                Component.text("Pontos da árvore: " + livres + " livre(s) de " + pontos, livres > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                Component.text("1 no E e +3 a cada rank (no S, +1 a cada 500 XP).", NamedTextColor.DARK_GRAY),
                Component.text("Os bônus valem só dentro dos portais.", NamedTextColor.DARK_GRAY))));

        for (Ramo ramo : Ramo.values()) {
            int base = INICIO_RAMO[ramo.ordinal()];
            int gasto = 0;
            for (No n : No.values()) if (n.ramo == ramo) gasto += ArvoreCacador.grau(p, n);
            t.inv.setItem(base, item(ramo.icone, Component.text(ramo.nome, ramo.cor, TextDecoration.BOLD),
                    List.of(Component.text(gasto + " ponto(s) neste ramo", NamedTextColor.GRAY))));
            int k = 1;
            for (No n : No.values()) {
                if (n.ramo != ramo) continue;
                int grau = ArvoreCacador.grau(p, n);
                String motivo = ArvoreCacador.impedimento(p, n);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(n.descricao, NamedTextColor.GRAY));
                lore.add(Component.text("Grau " + grau + "/" + n.maximo + "  •  a partir do " + n.rank.nome(), NamedTextColor.WHITE));
                lore.add(Component.empty());
                if (grau >= n.maximo) lore.add(Component.text("✔ No máximo", NamedTextColor.GREEN));
                else if (motivo == null) lore.add(Component.text("» Clique para subir (1 ponto)", NamedTextColor.YELLOW));
                else lore.add(Component.text("✖ " + motivo, NamedTextColor.RED));
                Material icone = r.ordinal() < n.rank.ordinal() ? Material.GRAY_DYE : n.icone;
                ItemStack it = item(icone, Component.text(n.nome, grau > 0 ? ramo.cor : NamedTextColor.WHITE, TextDecoration.BOLD), lore);
                it.setAmount(Math.max(1, grau));
                if (grau > 0) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
                t.inv.setItem(base + k++, it);
            }
        }

        List<Caracteristica> cs = Despertar.de(p);
        int vagas = Despertar.vagas(r);
        String[] quando = {"Rank D", "Rank B", "Rank S"};
        for (int i = 0; i < S_CARACTERISTICAS.length; i++) {
            if (i < cs.size()) {
                Caracteristica c = cs.get(i);
                t.inv.setItem(S_CARACTERISTICAS[i], item(c.icone, Component.text(c.nome, c.raridade.cor, TextDecoration.BOLD), List.of(
                        Component.text(c.descricao, NamedTextColor.GRAY),
                        Component.text("Característica " + c.raridade.nome, c.raridade.cor))));
            } else {
                t.inv.setItem(S_CARACTERISTICAS[i], item(Material.LIGHT_GRAY_STAINED_GLASS, Component.text("Característica " + (i + 1), NamedTextColor.DARK_GRAY),
                        List.of(Component.text(i < vagas ? "Abra a janela de novo para despertar." : "Desperta no " + quando[i] + ".", NamedTextColor.GRAY))));
            }
        }

        int mana = ManaCristal.valor(p);
        int usados = ArvoreCacador.usados(p);
        t.inv.setItem(S_REFAZER, item(Material.WRITABLE_BOOK, Component.text("Refazer a árvore", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Devolve todos os pontos.", NamedTextColor.GRAY),
                Component.text(usados == 0 ? "Nada para refazer." : "Custa " + PRECO_REFAZER + " de mana em cristais.", mana >= PRECO_REFAZER ? NamedTextColor.YELLOW : NamedTextColor.RED))));
        t.inv.setItem(S_REDESPERTAR, item(Material.ECHO_SHARD, Component.text("Re-despertar", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), List.of(
                Component.text("Sorteia todas as características de novo.", NamedTextColor.GRAY),
                Component.text(cs.isEmpty() ? "Você ainda não despertou." : "Custa " + Despertar.PRECO_REDESPERTAR + " de mana em cristais.",
                        !cs.isEmpty() && mana >= Despertar.PRECO_REDESPERTAR ? NamedTextColor.YELLOW : NamedTextColor.RED),
                Component.text("Clique com o Shift para confirmar.", NamedTextColor.DARK_GRAY))));
        t.inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));

        // 2.76: a classe é a da árvore de classes (as habilidades ficam 20% mais fortes nos portais).
        Classe c = plugin.classes().ativa(p);
        List<Component> lc = new ArrayList<>();
        if (c == null) {
            lc.add(Component.text("Você ainda não tem classe.", NamedTextColor.GRAY));
        } else {
            lc.add(Component.text("Passivo: " + c.passivo().texto(), NamedTextColor.GRAY));
            for (Habilidade h : c.habilidades()) lc.add(Component.text(" ✦ " + h.nome() + " (" + h.recarga() + " s)", NamedTextColor.WHITE));
        }
        lc.add(Component.text("Nos portais, as habilidades ficam 20% mais fortes.", NamedTextColor.AQUA));
        lc.add(Component.empty());
        lc.add(Component.text("» Clique para abrir a árvore de classes", NamedTextColor.YELLOW));
        t.inv.setItem(S_FUNCAO, item(c == null ? Material.IRON_SWORD : c.icone(),
                Component.text(c == null ? "Classe" : "Classe: " + c.nome(), c == null ? NamedTextColor.WHITE : c.cor(), TextDecoration.BOLD), lc));
        List<Component> missao = new ArrayList<>(MissaoDiaria.linhas(p));
        missao.add(Component.text("/masmorra missao", NamedTextColor.DARK_GRAY));
        t.inv.setItem(S_MISSAO, item(MissaoDiaria.feitaHoje(p) ? Material.WRITTEN_BOOK : Material.WRITABLE_BOOK,
                Component.text("Missão Diária", JanelaSistema.AZUL, TextDecoration.BOLD), missao));

        ItemStack vidro = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < t.inv.getSize(); i++) if (t.inv.getItem(i) == null) t.inv.setItem(i, vidro);
        // 2.77: a tela do mod de apoio mostra os números (barra de XP, pontos) a partir daqui.
        if (plugin.canalApoio() != null) plugin.canalApoio().sistemaMudou(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != topo) return;
        int slot = e.getSlot();
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (slot == S_FUNCAO) {
            p.closeInventory();
            p.performCommand("classe");
            return;
        }
        if (slot == S_REFAZER) {
            if (ArvoreCacador.usados(p) == 0) return;
            if (!criativo && !ManaCristal.gastar(p, PRECO_REFAZER)) { erro(p, "Faltam Cristais de Mana (" + PRECO_REFAZER + ")."); return; }
            ArvoreCacador.refazer(p);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
            desenhar(p, t);
            return;
        }
        if (slot == S_REDESPERTAR) {
            if (!Despertar.despertou(p)) return;
            if (!e.isShiftClick()) { erro(p, "Clique com o Shift para confirmar o Re-despertar."); return; }
            if (!criativo && !ManaCristal.gastar(p, Despertar.PRECO_REDESPERTAR)) { erro(p, "Faltam Cristais de Mana (" + Despertar.PRECO_REDESPERTAR + ")."); return; }
            Despertar.redespertar(p);
            desenhar(p, t);
            return;
        }
        for (Ramo ramo : Ramo.values()) {
            int base = INICIO_RAMO[ramo.ordinal()], k = 1;
            for (No n : No.values()) {
                if (n.ramo != ramo) continue;
                if (base + k++ != slot) continue;
                String motivo = ArvoreCacador.impedimento(p, n);
                if (motivo != null) { erro(p, motivo); return; }
                ArvoreCacador.subir(p, n);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
                desenhar(p, t);
                return;
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static void erro(Player p, String msg) {
        p.sendActionBar(Component.text("[Sistema] " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
