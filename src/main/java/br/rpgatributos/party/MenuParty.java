package br.rpgatributos.party;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menu da party: membros (vida, nível, distância), convidar, chat, fogo amigo, sair. */
public final class MenuParty implements Listener {

    private enum Tipo { PRINCIPAL, CONVIDAR }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        Inventory inventario;
        /** Jogador mostrado em cada slot (membros ou convidáveis). */
        final Map<Integer, UUID> jogadores = new HashMap<>();

        Tela(Tipo tipo) { this.tipo = tipo; }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private static final int S_INFO = 4;
    private static final int[] S_MEMBROS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int S_CONVIDAR = 45, S_CHAT = 46, S_FOGO = 47, S_FECHAR = 49, S_TERRITORIO = 51, S_SAIR = 52, S_DESFAZER = 53;
    private static final int S_CRIAR = 20, S_CONVITE = 22, S_COMO = 24;
    private static final int S_VOLTAR = 49;

    private final RPGAtributos plugin;

    public MenuParty(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Parties parties() { return plugin.parties(); }
    private Settings cfg() { return plugin.settings(); }

    // =====================================================================
    //  Tela principal
    // =====================================================================

    public void abrir(Player p) {
        Tela t = new Tela(Tipo.PRINCIPAL);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☺ Party"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        t.jogadores.clear();
        Party pt = parties().party(p);
        if (pt == null) desenharSemParty(p, inv);
        else desenharParty(p, t, pt);
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(inv);
    }

    private void desenharSemParty(Player p, Inventory inv) {
        inv.setItem(S_CRIAR, item(Material.CAKE, Component.text("☺ Criar party", Parties.COR, TextDecoration.BOLD), List.of(
                Component.text("Crie uma party e convide amigos.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique para criar", NamedTextColor.YELLOW))));
        String quem = parties().temConvite(p) ? parties().quemConvidou(p) : null;
        if (quem != null) {
            inv.setItem(S_CONVITE, brilhar(item(Material.WRITABLE_BOOK, Component.text("✉ Convite de " + quem, NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    Component.text("Clique esquerdo: aceitar", NamedTextColor.GREEN),
                    Component.text("Clique direito: recusar", NamedTextColor.RED)))));
        } else {
            inv.setItem(S_CONVITE, item(Material.PAPER, Component.text("Nenhum convite", NamedTextColor.GRAY), List.of(
                    Component.text("Quando alguém te convidar,", NamedTextColor.DARK_GRAY),
                    Component.text("o convite aparece aqui e no chat.", NamedTextColor.DARK_GRAY))));
        }
        inv.setItem(S_COMO, item(Material.BOOK, Component.text("Como funciona", NamedTextColor.YELLOW, TextDecoration.BOLD), comoFunciona()));
    }

    private List<Component> comoFunciona() {
        int raio = (int) cfg().partyRaioXp;
        return List.of(
                Component.text("• Ninguém da party fere o outro", NamedTextColor.GRAY),
                Component.text("  (o líder pode ligar o fogo amigo).", NamedTextColor.GRAY),
                Component.text("• Monstro que você mata dá " + Math.round(cfg().partyXpCompartilhado * 100) + "% do XP", NamedTextColor.GRAY),
                Component.text("  de Combate para quem está a até " + raio + " blocos,", NamedTextColor.GRAY),
                Component.text("  e você ganha +" + Math.round(cfg().partyBonusPorMembro * 100) + "% por membro perto", NamedTextColor.GRAY),
                Component.text("  (até +" + Math.round(cfg().partyBonusMax * 100) + "%).", NamedTextColor.GRAY),
                Component.text("• Chat só da party: /pc <mensagem>.", NamedTextColor.GRAY),
                Component.text("• Aviso quando alguém está com pouca vida.", NamedTextColor.GRAY),
                Component.text("• A party pode construir no seu território", NamedTextColor.GRAY),
                Component.text("  (regra \"Party pode construir\").", NamedTextColor.GRAY));
    }

    private void desenharParty(Player p, Tela t, Party pt) {
        Inventory inv = t.inventario;
        boolean lider = pt.lider().equals(p.getUniqueId());
        List<Component> info = new ArrayList<>();
        info.add(Component.text("Líder: " + pt.nomeLider(), NamedTextColor.GOLD));
        info.add(Component.text("Membros: " + pt.tamanho() + "/" + cfg().partyTamanhoMax, NamedTextColor.WHITE));
        info.add(Component.text("Fogo amigo: " + (pt.fogoAmigo() ? "ligado" : "desligado"),
                pt.fogoAmigo() ? NamedTextColor.RED : NamedTextColor.GREEN));
        info.add(Component.empty());
        info.addAll(comoFunciona());
        inv.setItem(S_INFO, item(Material.NETHER_STAR, Component.text("☺ Party de " + pt.nomeLider(), Parties.COR, TextDecoration.BOLD), info));

        int i = 0;
        for (Map.Entry<UUID, String> m : pt.membros().entrySet()) {
            if (i >= S_MEMBROS.length) break;
            inv.setItem(S_MEMBROS[i], cabecaMembro(p, pt, m.getKey(), m.getValue(), lider));
            t.jogadores.put(S_MEMBROS[i], m.getKey());
            i++;
        }

        inv.setItem(S_CONVIDAR, item(Material.EMERALD, Component.text("+ Convidar", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text(lider ? "Escolha um jogador online." : "Só o líder convida.", NamedTextColor.GRAY))));
        boolean chat = parties().chatAtivo(p);
        inv.setItem(S_CHAT, item(chat ? Material.WRITABLE_BOOK : Material.BOOK,
                Component.text("✉ Chat da party: " + (chat ? "LIGADO" : "desligado"), chat ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD),
                List.of(Component.text("Ligado: o que você escrever no chat", NamedTextColor.GRAY),
                        Component.text("vai só para a party.", NamedTextColor.GRAY),
                        Component.text("Ou use /pc <mensagem>.", NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text("» Clique para " + (chat ? "desligar" : "ligar"), NamedTextColor.YELLOW))));
        inv.setItem(S_FOGO, item(pt.fogoAmigo() ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                Component.text("⚔ Fogo amigo: " + (pt.fogoAmigo() ? "LIGADO" : "desligado"), pt.fogoAmigo() ? NamedTextColor.RED : NamedTextColor.GREEN, TextDecoration.BOLD),
                List.of(Component.text("Ligado: membros da party podem se ferir", NamedTextColor.GRAY),
                        Component.text("(para treinar ou duelar).", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text(lider ? "» Clique para mudar" : "Só o líder muda.", lider ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY))));
        inv.setItem(S_TERRITORIO, item(Material.LODESTONE, Component.text("⚑ Território", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text("Abre o menu do seu território.", NamedTextColor.GRAY))));
        inv.setItem(S_SAIR, item(Material.OAK_DOOR, Component.text("Sair da party", NamedTextColor.RED), List.of(
                Component.text("Shift + clique para sair.", NamedTextColor.GRAY))));
        if (lider) {
            inv.setItem(S_DESFAZER, item(Material.TNT, Component.text("Desfazer a party", NamedTextColor.DARK_RED, TextDecoration.BOLD), List.of(
                    Component.text("Todos saem da party.", NamedTextColor.GRAY),
                    Component.text("Shift + clique para confirmar.", NamedTextColor.GRAY))));
        }
    }

    private ItemStack cabecaMembro(Player quemVe, Party pt, UUID id, String nome, boolean veLider) {
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(Bukkit.getOfflinePlayer(id)));
        Player online = Bukkit.getPlayer(id);
        List<Component> lore = new ArrayList<>();
        if (pt.lider().equals(id)) lore.add(Component.text("★ Líder", NamedTextColor.GOLD));
        if (online == null) {
            lore.add(Component.text("Offline", NamedTextColor.DARK_GRAY));
        } else {
            AttributeInstance max = online.getAttribute(Attribute.MAX_HEALTH);
            lore.add(Component.text(String.format("❤ %.1f / %.0f", online.getHealth(), max == null ? 20 : max.getValue()), NamedTextColor.RED));
            lore.add(Component.text("Nível total: " + plugin.stats().nivelTotal(online), NamedTextColor.YELLOW));
            if (online.getWorld().equals(quemVe.getWorld())) {
                if (online != quemVe) lore.add(Component.text("A " + Math.round(online.getLocation().distance(quemVe.getLocation())) + " blocos de você", NamedTextColor.GRAY));
            } else {
                lore.add(Component.text("Em outro mundo", NamedTextColor.GRAY));
            }
        }
        if (veLider && !id.equals(quemVe.getUniqueId())) {
            lore.add(Component.empty());
            lore.add(Component.text("Shift + clique esquerdo: passar a liderança", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift + clique direito: tirar da party", NamedTextColor.RED));
        }
        return enfeitar(cabeca, Component.text(nome, online == null ? NamedTextColor.GRAY : NamedTextColor.WHITE, TextDecoration.BOLD), lore);
    }

    // =====================================================================
    //  Convidar
    // =====================================================================

    private void abrirConvidar(Player p) {
        Tela t = new Tela(Tipo.CONVIDAR);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☺ Convidar para a party"));
        int slot = 0;
        for (Player outro : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            if (outro.equals(p) || parties().mesmaParty(p.getUniqueId(), outro.getUniqueId())) continue;
            boolean livre = parties().party(outro) == null;
            ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
            cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(outro));
            t.inventario.setItem(slot, enfeitar(cabeca, Component.text(outro.getName(), livre ? NamedTextColor.WHITE : NamedTextColor.GRAY, TextDecoration.BOLD),
                    List.of(livre ? Component.text("» Clique para convidar", NamedTextColor.YELLOW)
                            : Component.text("Já está numa party.", NamedTextColor.DARK_GRAY))));
            t.jogadores.put(slot, outro.getUniqueId());
            slot++;
        }
        if (slot == 0) {
            t.inventario.setItem(22, item(Material.OAK_SIGN, Component.text("Ninguém para convidar", NamedTextColor.GRAY), List.of(
                    Component.text("Não há outros jogadores online.", NamedTextColor.DARK_GRAY))));
        }
        t.inventario.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(t.inventario);
        p.openInventory(t.inventario);
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (t.tipo == Tipo.CONVIDAR) {
            if (slot == S_VOLTAR) { clique(p); abrir(p); return; }
            UUID alvo = t.jogadores.get(slot);
            Player outro = alvo == null ? null : Bukkit.getPlayer(alvo);
            if (outro == null) return;
            if (resultado(p, parties().convidar(p, outro))) abrir(p);
            return;
        }
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        Party pt = parties().party(p);
        if (pt == null) {
            switch (slot) {
                case S_CRIAR -> resultado(p, parties().criar(p));
                case S_CONVITE -> {
                    if (!parties().temConvite(p)) return;
                    resultado(p, e.isRightClick() ? parties().recusar(p) : parties().aceitar(p));
                }
                default -> { return; }
            }
            desenhar(p, t);
            return;
        }
        boolean shift = e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT;
        switch (slot) {
            case S_CONVIDAR -> {
                if (!pt.lider().equals(p.getUniqueId())) { resultado(p, "Só o líder convida."); return; }
                clique(p);
                abrirConvidar(p);
                return;
            }
            case S_CHAT -> {
                boolean ligado = parties().alternarChat(p);
                p.sendMessage(Component.text("✉ Chat da party " + (ligado ? "ligado: suas mensagens vão só para a party." : "desligado."), Parties.COR));
                clique(p);
            }
            case S_FOGO -> resultado(p, parties().alternarFogoAmigo(p));
            case S_TERRITORIO -> {
                clique(p);
                plugin.menusTerritorio().abrirPrincipal(p, plugin.territorios().de(p.getUniqueId()));
                return;
            }
            case S_SAIR -> {
                if (!shift) { resultado(p, "Use shift + clique para sair."); return; }
                resultado(p, parties().sair(p));
            }
            case S_DESFAZER -> {
                if (!shift) { resultado(p, "Use shift + clique para confirmar."); return; }
                resultado(p, parties().desfazer(p));
            }
            default -> {
                UUID alvo = t.jogadores.get(slot);
                if (alvo == null || !shift) return;
                if (e.getClick() == ClickType.SHIFT_LEFT) resultado(p, parties().passarLideranca(p, alvo));
                else resultado(p, parties().expulsar(p, alvo));
            }
        }
        desenhar(p, t);
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    /** Mostra o erro (se houver). @return true se deu certo. */
    private static boolean resultado(Player p, String erro) {
        if (erro == null) {
            clique(p);
            return true;
        }
        p.sendMessage(Component.text(erro, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        return false;
    }

    private static void clique(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return enfeitar(new ItemStack(m), nome, lore);
    }

    private static ItemStack enfeitar(ItemStack i, Component nome, List<Component> lore) {
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    private static ItemStack brilhar(ItemStack i) {
        i.editMeta(m -> m.setEnchantmentGlintOverride(true));
        return i;
    }

    private static void preencher(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }
}
