package br.rpgatributos.territorio;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menus do território: principal, mapa de chunks, membros, adicionar membro e regras. */
public final class MenuTerritorio implements Listener {

    private enum Tipo { PRINCIPAL, MAPA, MEMBROS, ADICIONAR, REGRAS }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        /** Dono do território mostrado (null = jogador sem território). */
        final UUID dono;
        Inventory inventario;
        /** Mapa: chunk do centro. */
        String mundo;
        int cx, cz;
        final Map<Integer, UUID> jogadores = new HashMap<>();

        Tela(Tipo tipo, UUID dono) {
            this.tipo = tipo;
            this.dono = dono;
        }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    // principal
    private static final int S_INFO = 4, S_MAPA = 19, S_MEMBROS = 21, S_REGRAS = 23, S_BORDAS = 25;
    private static final int S_REIVINDICAR = 30, S_LIBERAR = 32, S_EXPANSOES = 34, S_FECHAR = 49, S_ABANDONAR = 53;
    private static final int S_COMO = 13, S_AQUI = 22;
    // mapa (5 linhas x 9 colunas de chunks)
    private static final int S_NORTE = 45, S_LEGENDA = 47, S_VOLTAR = 49, S_CONTA = 51;
    // membros e regras
    private static final int S_ADICIONAR = 48;
    private static final int[] S_FLAGS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    private final RPGAtributos plugin;

    public MenuTerritorio(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Territorios ter() { return plugin.territorios(); }
    private Settings cfg() { return plugin.settings(); }

    private Territorio territorio(Tela t) {
        return t.dono == null ? null : ter().de(t.dono);
    }

    // =====================================================================
    //  Principal
    // =====================================================================

    /** Abre o menu de um território (null = mostra como criar um e onde o jogador está). */
    public void abrirPrincipal(Player p, Territorio terr) {
        Tela t = new Tela(Tipo.PRINCIPAL, terr == null ? null : terr.dono());
        t.inventario = Bukkit.createInventory(t, 54, Component.text(terr == null ? "⚑ Território"
                : terr.dono().equals(p.getUniqueId()) ? "⚑ Seu território" : "⚑ Território de " + terr.nomeDono()));
        desenharPrincipal(p, t);
        p.openInventory(t.inventario);
    }

    private void desenharPrincipal(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Territorio terr = territorio(t);
        if (terr == null) {
            desenharSemTerritorio(p, inv);
        } else {
            boolean gerencia = ter().gerencia(p, terr);
            inv.setItem(S_INFO, info(p, terr, gerencia));
            inv.setItem(S_MAPA, item(Material.FILLED_MAP, Component.text("◈ Mapa", NamedTextColor.AQUA, TextDecoration.BOLD), List.of(
                    Component.text("Os chunks em volta de você.", NamedTextColor.GRAY),
                    Component.text(gerencia ? "Clique num chunk livre para reivindicar." : "Veja de quem é cada lugar.", NamedTextColor.GRAY))));
            inv.setItem(S_MEMBROS, item(Material.PLAYER_HEAD, Component.text("☺ Membros (" + terr.membros().size() + "/" + cfg().terMembrosMax + ")",
                    NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                    Component.text("Membros constroem e mexem em tudo.", NamedTextColor.GRAY))));
            List<Component> regras = new ArrayList<>();
            for (Flag f : Flag.values()) {
                regras.add(Component.text((terr.flag(f) ? "✔ " : "✖ ") + f.nome(), terr.flag(f) ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            inv.setItem(S_REGRAS, item(Material.COMPARATOR, Component.text("⚙ Regras", NamedTextColor.GOLD, TextDecoration.BOLD), regras));
            inv.setItem(S_BORDAS, bordas());
            if (gerencia) {
                Location aqui = p.getLocation();
                String erro = ter().impedimentoReivindicar(p, aqui.getWorld().getName(), aqui.getBlockX() >> 4, aqui.getBlockZ() >> 4);
                inv.setItem(S_REIVINDICAR, item(Material.GRASS_BLOCK, Component.text("+ Reivindicar este chunk", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                        Component.text("O chunk onde você está agora.", NamedTextColor.GRAY),
                        erro == null ? Component.text("» Clique para reivindicar", NamedTextColor.YELLOW) : Component.text(erro, NamedTextColor.RED))));
                inv.setItem(S_LIBERAR, item(Material.DEAD_BUSH, Component.text("- Liberar este chunk", NamedTextColor.RED, TextDecoration.BOLD), List.of(
                        Component.text("Tira o chunk onde você está do território.", NamedTextColor.GRAY),
                        Component.text("Shift + clique para confirmar.", NamedTextColor.GRAY))));
                if (terr.dono().equals(p.getUniqueId())) inv.setItem(S_EXPANSOES, expansoes(p, terr));
                inv.setItem(S_ABANDONAR, item(Material.TNT, Component.text("Abandonar território", NamedTextColor.DARK_RED, TextDecoration.BOLD), List.of(
                        Component.text("Apaga o território inteiro.", NamedTextColor.GRAY),
                        Component.text("O Marco vira magnetita e a bandeira cai.", NamedTextColor.GRAY),
                        Component.text("As 4 esmeraldas não voltam.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Shift + clique para confirmar.", NamedTextColor.RED))));
            }
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(inv);
    }

    private ItemStack info(Player p, Territorio terr, boolean gerencia) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Dono: " + terr.nomeDono(), NamedTextColor.WHITE));
        if (terr.dono().equals(p.getUniqueId())) {
            lore.add(Component.text("Chunks: " + terr.chunks().size() + " / " + ter().limite(p), NamedTextColor.WHITE));
            lore.add(Component.text("(+1 chunk a cada " + cfg().terNiveisPorChunk + " níveis somados, até " + cfg().terChunksMax + ")", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(Component.text("Chunks: " + terr.chunks().size(), NamedTextColor.WHITE));
        }
        lore.add(Component.text("Membros: " + terr.membros().size(), NamedTextColor.WHITE));
        var reino = ter().reinoDe(terr);
        if (reino != null) lore.add(Component.text("♛ Província do reino " + reino.nome(), reino.cor()));
        if (!terr.expansoes().isEmpty()) lore.add(Component.text("Expansões: " + terr.expansoes().size(), NamedTextColor.WHITE));
        lore.add(Component.text("Marco: " + terr.marcoX() + " " + terr.marcoY() + " " + terr.marcoZ() + " (" + terr.mundo() + ")", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(switch (ter().relacao(p, terr)) {
            case PROPRIO -> Component.text("Este território é seu.", NamedTextColor.GREEN);
            case AMIGO -> Component.text("Você pode construir aqui.", NamedTextColor.AQUA);
            default -> Component.text("Você é visitante aqui.", NamedTextColor.GOLD);
        });
        if (gerencia) {
            lore.add(Component.empty());
            lore.add(Component.text("Segure outro estandarte e clique", NamedTextColor.YELLOW));
            lore.add(Component.text("aqui para trocar a bandeira.", NamedTextColor.YELLOW));
        }
        return enfeitar(terr.bandeira(), Component.text("⚑ Território de " + terr.nomeDono(), Territorios.COR, TextDecoration.BOLD), lore);
    }

    private ItemStack expansoes(Player p, Territorio terr) {
        int tem = terr.expansoes().size(), pode = ter().expansoesPermitidas(p);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Quer mais terra? Crie uma Expansão: outra", NamedTextColor.GRAY));
        lore.add(Component.text("área do seu território, em outro lugar.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Magnetita + jogue em cima (Q):", NamedTextColor.GRAY));
        lore.add(Component.text("  • 1 estandarte, 4 esmeraldas", NamedTextColor.WHITE));
        lore.add(Component.text("  • 1 bloco de esmeralda", NamedTextColor.WHITE));
        lore.add(Component.text("Cada uma protege +" + cfg().terChunksPorExpansao + " chunks (3x3) em volta.", NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Expansões: " + tem + " / " + pode + " (máximo " + cfg().terExpansoesMax + ")", NamedTextColor.WHITE));
        if (pode < cfg().terExpansoesMax) {
            lore.add(Component.text("(+1 a cada " + cfg().terNiveisPorExpansao + " níveis somados)", NamedTextColor.DARK_GRAY));
        }
        for (int[] e : terr.expansoes()) lore.add(Component.text("  ⚑ " + e[0] + " " + e[1] + " " + e[2], Territorios.COR));
        if (tem > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("Shift + clique dentro de uma expansão:", NamedTextColor.RED));
            lore.add(Component.text("desfaz ela (chunks soltos são liberados).", NamedTextColor.RED));
        }
        return item(Material.LODESTONE, Component.text("⚑ Expansões", Territorios.COR, TextDecoration.BOLD), lore);
    }

    private void desenharSemTerritorio(Player p, Inventory inv) {
        inv.setItem(S_COMO, item(Material.LODESTONE, Component.text("⚑ Como criar seu território", Territorios.COR, TextDecoration.BOLD), List.of(
                Component.text("1. Coloque uma magnetita.", NamedTextColor.GRAY),
                Component.text("2. Jogue em cima dela (tecla Q):", NamedTextColor.GRAY),
                Component.text("   • 1 estandarte (vira sua bandeira)", NamedTextColor.WHITE),
                Component.text("   • 4 esmeraldas", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("A magnetita vira o Marco do Território", NamedTextColor.GRAY),
                Component.text("e protege " + cfg().terChunksIniciais + " chunks em volta dela.", NamedTextColor.GRAY),
                Component.text("Seu limite agora: " + ter().limite(p) + " chunks.", NamedTextColor.YELLOW),
                Component.text("(+1 a cada " + cfg().terNiveisPorChunk + " níveis somados)", NamedTextColor.DARK_GRAY))));
        Territorio aqui = ter().em(p.getLocation());
        inv.setItem(S_AQUI, item(aqui == null ? Material.GRASS_BLOCK : Material.RED_BANNER,
                Component.text(aqui == null ? "☘ Aqui: terras livres" : "⚑ Aqui: território de " + aqui.nomeDono(),
                        aqui == null ? NamedTextColor.GREEN : NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(Component.text(aqui == null ? "Dá para criar um território aqui." : "Escolha outro lugar para o seu.", NamedTextColor.GRAY))));
        inv.setItem(S_MAPA, item(Material.FILLED_MAP, Component.text("◈ Mapa", NamedTextColor.AQUA, TextDecoration.BOLD), List.of(
                Component.text("Veja os territórios em volta.", NamedTextColor.GRAY))));
        inv.setItem(S_BORDAS, bordas());
    }

    private static ItemStack bordas() {
        return item(Material.ENDER_EYE, Component.text("◎ Mostrar bordas", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), List.of(
                Component.text("Partículas nas bordas dos territórios", NamedTextColor.GRAY),
                Component.text("perto de você, por 20 segundos.", NamedTextColor.GRAY),
                Component.text("Verde: seu • Azul: amigo • Vermelho: de outro", NamedTextColor.DARK_GRAY)));
    }

    // =====================================================================
    //  Mapa
    // =====================================================================

    public void abrirMapa(Player p, UUID dono) {
        Tela t = new Tela(Tipo.MAPA, dono);
        Location l = p.getLocation();
        t.mundo = l.getWorld().getName();
        t.cx = l.getBlockX() >> 4;
        t.cz = l.getBlockZ() >> 4;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("◈ Mapa de territórios"));
        desenharMapa(p, t);
        p.openInventory(t.inventario);
    }

    private void desenharMapa(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Territorio meu = ter().de(p.getUniqueId());
        for (int linha = 0; linha < 5; linha++) {
            for (int col = 0; col < 9; col++) {
                int cx = t.cx + col - 4, cz = t.cz + linha - 2;
                inv.setItem(linha * 9 + col, celula(p, meu, t.mundo, cx, cz, col == 4 && linha == 2));
            }
        }
        inv.setItem(S_NORTE, item(Material.COMPASS, Component.text("↑ Norte", NamedTextColor.WHITE, TextDecoration.BOLD), List.of(
                Component.text("Cada quadrado é um chunk (16x16 blocos).", NamedTextColor.GRAY),
                Component.text("Sua cabeça é onde você está.", NamedTextColor.GRAY))));
        inv.setItem(S_LEGENDA, item(Material.PAPER, Component.text("Legenda", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("■ Verde: seu território", NamedTextColor.GREEN),
                Component.text("■ Azul: você constrói lá", NamedTextColor.AQUA),
                Component.text("■ Vermelho: de outro jogador", NamedTextColor.RED),
                Component.text("■ Branco: livre", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Clique num livre: reivindicar", NamedTextColor.YELLOW),
                Component.text("Shift + clique no seu: liberar", NamedTextColor.YELLOW))));
        inv.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        if (meu != null) {
            inv.setItem(S_CONTA, item(Material.LODESTONE, Component.text("Chunks: " + meu.chunks().size() + " / " + ter().limite(p),
                    Territorios.COR, TextDecoration.BOLD), List.of()));
        }
        preencher(inv);
    }

    private ItemStack celula(Player p, Territorio meu, String mundo, int cx, int cz, boolean centro) {
        Territorio t = ter().em(mundo, cx, cz);
        Territorios.Relacao r = ter().relacao(p, t);
        List<Component> lore = new ArrayList<>();
        Component nome;
        switch (r) {
            case PROPRIO -> {
                nome = Component.text("Seu território", NamedTextColor.GREEN, TextDecoration.BOLD);
                if (t.chunkComMarco(Territorio.chave(cx, cz))) lore.add(Component.text("⚑ Um Marco fica aqui.", NamedTextColor.GOLD));
                else lore.add(Component.text("Shift + clique: liberar", NamedTextColor.YELLOW));
            }
            case AMIGO -> nome = Component.text("Território de " + t.nomeDono(), NamedTextColor.AQUA, TextDecoration.BOLD);
            case OUTRO -> nome = Component.text("Território de " + t.nomeDono(), NamedTextColor.RED, TextDecoration.BOLD);
            default -> {
                nome = Component.text("Livre", NamedTextColor.WHITE, TextDecoration.BOLD);
                if (meu != null) {
                    String erro = ter().impedimentoReivindicar(p, mundo, cx, cz);
                    lore.add(erro == null ? Component.text("» Clique para reivindicar", NamedTextColor.YELLOW)
                            : Component.text(erro, NamedTextColor.DARK_GRAY));
                }
            }
        }
        var reino = ter().reinoDe(t);
        if (reino != null) lore.addFirst(Component.text("♛ Província do reino " + reino.nome(), reino.cor()));
        lore.add(Component.text("Chunk " + cx + ", " + cz + " (blocos " + (cx << 4) + ", " + (cz << 4) + ")", NamedTextColor.DARK_GRAY));
        ItemStack base;
        if (centro) {
            base = new ItemStack(Material.PLAYER_HEAD);
            base.editMeta(SkullMeta.class, m -> m.setOwningPlayer(p));
            lore.addFirst(Component.text("● Você está aqui", NamedTextColor.YELLOW));
        } else if (t != null && t.chunkComMarco(Territorio.chave(cx, cz))) {
            base = t.bandeira();
        } else {
            base = new ItemStack(Territorios.vidro(r));
        }
        return enfeitar(base, nome, lore);
    }

    // =====================================================================
    //  Membros
    // =====================================================================

    private void abrirMembros(Player p, Territorio terr) {
        Tela t = new Tela(Tipo.MEMBROS, terr.dono());
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☺ Membros do território"));
        boolean gerencia = ter().gerencia(p, terr);
        int slot = 0;
        for (Map.Entry<UUID, String> m : terr.membros().entrySet()) {
            if (slot >= 45) break;
            ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
            cabeca.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(Bukkit.getOfflinePlayer(m.getKey())));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(Bukkit.getPlayer(m.getKey()) != null ? "Online" : "Offline", NamedTextColor.GRAY));
            if (gerencia) lore.add(Component.text("Shift + clique: tirar do território", NamedTextColor.RED));
            t.inventario.setItem(slot, enfeitar(cabeca, Component.text(m.getValue(), NamedTextColor.WHITE, TextDecoration.BOLD), lore));
            t.jogadores.put(slot, m.getKey());
            slot++;
        }
        if (slot == 0) {
            t.inventario.setItem(22, item(Material.OAK_SIGN, Component.text("Nenhum membro ainda", NamedTextColor.GRAY), List.of(
                    Component.text(terr.flag(Flag.PARTY) ? "Sua party já pode construir aqui." : "Só o dono constrói aqui.", NamedTextColor.DARK_GRAY))));
        }
        if (gerencia) {
            t.inventario.setItem(S_ADICIONAR, item(Material.EMERALD, Component.text("+ Adicionar membro", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                    Component.text("Escolha um jogador online.", NamedTextColor.GRAY),
                    Component.text("Offline: /territorio adicionar <nick>", NamedTextColor.DARK_GRAY))));
        }
        t.inventario.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(t.inventario);
        p.openInventory(t.inventario);
    }

    private void abrirAdicionar(Player p, Territorio terr) {
        Tela t = new Tela(Tipo.ADICIONAR, terr.dono());
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☺ Adicionar membro"));
        int slot = 0;
        for (Player outro : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            if (outro.getUniqueId().equals(terr.dono()) || terr.membro(outro.getUniqueId())) continue;
            ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
            cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(outro));
            t.inventario.setItem(slot, enfeitar(cabeca, Component.text(outro.getName(), NamedTextColor.WHITE, TextDecoration.BOLD),
                    List.of(Component.text("» Clique para adicionar", NamedTextColor.YELLOW))));
            t.jogadores.put(slot, outro.getUniqueId());
            slot++;
        }
        if (slot == 0) {
            t.inventario.setItem(22, item(Material.OAK_SIGN, Component.text("Ninguém para adicionar", NamedTextColor.GRAY), List.of()));
        }
        t.inventario.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(t.inventario);
        p.openInventory(t.inventario);
    }

    // =====================================================================
    //  Regras
    // =====================================================================

    private void abrirRegras(Player p, Territorio terr) {
        Tela t = new Tela(Tipo.REGRAS, terr.dono());
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⚙ Regras do território"));
        desenharRegras(p, t, terr);
        p.openInventory(t.inventario);
    }

    private void desenharRegras(Player p, Tela t, Territorio terr) {
        Inventory inv = t.inventario;
        inv.clear();
        boolean gerencia = ter().gerencia(p, terr);
        Flag[] flags = Flag.values();
        for (int i = 0; i < flags.length && i < S_FLAGS.length; i++) {
            Flag f = flags[i];
            boolean ligada = terr.flag(f);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(f.descricao(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text(ligada ? "✔ Ligada" : "✖ Desligada", ligada ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD));
            var reino = ter().reinoDe(terr);
            if (reino != null && reino.proibe(f)) {
                lore.add(Component.text("⚖ Proibida pela lei de " + reino.nome() + " (vale mesmo ligada)", NamedTextColor.GOLD));
            }
            if (gerencia) lore.add(Component.text("» Clique para mudar", NamedTextColor.YELLOW));
            Component nome = Component.text(f.nome(), ligada ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD);
            inv.setItem(S_FLAGS[i], item(f.icone(), nome, lore));
            inv.setItem(S_FLAGS[i] + 9, item(ligada ? Material.LIME_DYE : Material.GRAY_DYE, nome, lore));
        }
        inv.setItem(31, item(Material.BOOK, Component.text("Bom saber", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("Dono e membros sempre podem tudo.", NamedTextColor.GRAY),
                Component.text("Mobs nunca pisoteiam a plantação nem", NamedTextColor.GRAY),
                Component.text("pegam blocos (enderman) aqui dentro.", NamedTextColor.GRAY),
                Component.text("Líquidos, pistões e fogo de fora", NamedTextColor.GRAY),
                Component.text("não atravessam a borda.", NamedTextColor.GRAY))));
        inv.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(inv);
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
        boolean shift = e.isShiftClick();
        Territorio terr = territorio(t);
        switch (t.tipo) {
            case PRINCIPAL -> clicarPrincipal(p, t, terr, slot, shift);
            case MAPA -> clicarMapa(p, t, slot, shift);
            case MEMBROS -> {
                if (terr == null) return;
                if (slot == S_VOLTAR) { clique(p); abrirPrincipal(p, terr); return; }
                if (!ter().gerencia(p, terr)) return;
                if (slot == S_ADICIONAR) { clique(p); abrirAdicionar(p, terr); return; }
                UUID alvo = t.jogadores.get(slot);
                if (alvo == null || !shift) return;
                ter().removerMembro(terr, alvo);
                clique(p);
                abrirMembros(p, terr);
            }
            case ADICIONAR -> {
                if (terr == null) return;
                if (slot == S_VOLTAR) { clique(p); abrirMembros(p, terr); return; }
                UUID alvo = t.jogadores.get(slot);
                if (alvo == null || !ter().gerencia(p, terr)) return;
                if (resultado(p, ter().adicionarMembro(terr, Bukkit.getOfflinePlayer(alvo)))) abrirMembros(p, terr);
            }
            case REGRAS -> {
                if (terr == null) return;
                if (slot == S_VOLTAR) { clique(p); abrirPrincipal(p, terr); return; }
                if (!ter().gerencia(p, terr)) return;
                Flag[] flags = Flag.values();
                for (int i = 0; i < flags.length && i < S_FLAGS.length; i++) {
                    if (slot == S_FLAGS[i] || slot == S_FLAGS[i] + 9) {
                        ter().alternarFlag(terr, flags[i]);
                        clique(p);
                        desenharRegras(p, t, terr);
                        return;
                    }
                }
            }
        }
    }

    private void clicarPrincipal(Player p, Tela t, Territorio terr, int slot, boolean shift) {
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (slot == S_BORDAS) {
            p.closeInventory();
            ter().mostrarBordas(p, 20);
            p.sendActionBar(Component.text("◎ Mostrando as bordas por 20 segundos.", NamedTextColor.LIGHT_PURPLE));
            return;
        }
        if (slot == S_MAPA) { clique(p); abrirMapa(p, t.dono); return; }
        if (terr == null) return;
        switch (slot) {
            case S_MEMBROS -> { clique(p); abrirMembros(p, terr); return; }
            case S_REGRAS -> { clique(p); abrirRegras(p, terr); return; }
            default -> { }
        }
        if (!ter().gerencia(p, terr)) return;
        Location aqui = p.getLocation();
        switch (slot) {
            case S_INFO -> trocarBandeira(p, terr);
            case S_REIVINDICAR -> {
                if (!terr.dono().equals(p.getUniqueId())) { resultado(p, "Só o dono reivindica chunks."); return; }
                if (resultado(p, ter().reivindicar(p, aqui.getWorld().getName(), aqui.getBlockX() >> 4, aqui.getBlockZ() >> 4))) {
                    p.sendMessage(Component.text("⚑ Chunk reivindicado! (" + terr.chunks().size() + "/" + ter().limite(p) + ")", Territorios.COR));
                    ter().mostrarBordas(p, 8);
                }
            }
            case S_LIBERAR -> {
                if (!shift) { resultado(p, "Use shift + clique para liberar o chunk."); return; }
                if (resultado(p, ter().liberar(terr, aqui.getWorld().getName(), aqui.getBlockX() >> 4, aqui.getBlockZ() >> 4))) {
                    p.sendMessage(Component.text("Chunk liberado.", NamedTextColor.GRAY));
                }
            }
            case S_EXPANSOES -> {
                if (!shift) { resultado(p, "Para criar: Marco novo em outro lugar. Para desfazer: shift + clique dentro da expansão."); return; }
                int i = ter().expansaoAqui(terr, aqui);
                if (i < 0) { resultado(p, "Fique dentro da expansão que quer desfazer."); return; }
                int liberados = ter().abandonarExpansao(terr, i);
                p.sendMessage(Component.text("⚑ Expansão desfeita (" + liberados + " chunk(s) liberado(s)).", NamedTextColor.GRAY));
            }
            case S_ABANDONAR -> {
                if (!shift) { resultado(p, "Use shift + clique para confirmar."); return; }
                ter().abandonar(terr);
                p.closeInventory();
                p.sendMessage(Component.text("⚑ Território abandonado.", NamedTextColor.GRAY));
                return;
            }
            default -> { return; }
        }
        desenharPrincipal(p, t);
    }

    private void trocarBandeira(Player p, Territorio terr) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (!Tag.ITEMS_BANNERS.isTagged(mao.getType())) {
            resultado(p, "Segure um estandarte na mão para trocar a bandeira.");
            return;
        }
        ItemStack nova = mao.clone();
        nova.setAmount(1);
        mao.setAmount(mao.getAmount() - 1);
        ItemStack antiga = terr.bandeira();
        p.getInventory().addItem(antiga).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        ter().trocarBandeira(terr, nova);
        p.sendMessage(Component.text("⚑ Bandeira trocada!", Territorios.COR));
        clique(p);
    }

    private void clicarMapa(Player p, Tela t, int slot, boolean shift) {
        if (slot == S_VOLTAR) {
            clique(p);
            abrirPrincipal(p, t.dono == null ? ter().de(p.getUniqueId()) : ter().de(t.dono));
            return;
        }
        if (slot >= 45) return;
        int cx = t.cx + slot % 9 - 4, cz = t.cz + slot / 9 - 2;
        Territorio dono = ter().em(t.mundo, cx, cz);
        Territorio meu = ter().de(p.getUniqueId());
        if (meu == null) return;
        if (dono == null) {
            if (!resultado(p, ter().reivindicar(p, t.mundo, cx, cz))) return;
        } else if (dono == meu && shift) {
            if (!resultado(p, ter().liberar(meu, t.mundo, cx, cz))) return;
        } else {
            return;
        }
        desenharMapa(p, t);
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

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

    private static void preencher(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }
}
