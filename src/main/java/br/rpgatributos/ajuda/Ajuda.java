package br.rpgatributos.ajuda;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A interface do plugin para os jogadores: o /rpg (com os próximos passos e as categorias), o
 * Guia interativo (um assunto por tela, com o ritual desenhado em itens, e a busca no chat) e
 * o menu da Jornada do Aventureiro. Quem está com o pacote de recursos vê os menus com fundo
 * desenhado; quem não está, vê os mesmos menus com vidros.
 */
public final class Ajuda implements Listener {

    public static final TextColor COR = TextColor.color(0xF2C14E);
    private static final Key FONTE = Key.key("rpgatributos", "menus");

    private static final int S_PERFIL = 4, S_VOLTAR = 45, S_MEIO = 49, S_FECHAR = 53;
    private static final int[] S_PROXIMOS = {11, 13, 15};
    private static final int[] S_CATEGORIAS = {28, 29, 30, 31, 32, 33, 34};
    private static final int[] S_ATALHOS = {37, 38, 39, 40, 41, 42, 43};
    private static final int[] S_LISTA = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] S_LIGADOS = {37, 38, 39, 40, 41, 42, 43};

    /** Atalhos do /rpg: {ícone, nome, comando}. */
    private static final Object[][] ATALHOS = {
            {Material.PLAYER_HEAD, "Atributos", "atributos"}, {Material.IRON_SWORD, "Combos", "combos"},
            {Material.LECTERN, "Classe", "classe"}, {Material.CHEST, "Mochila", "mochila"},
            {Material.GOLD_NUGGET, "Acessórios", "acessorios"}, {Material.ENCHANTED_BOOK, "Grimório", "grimorio"},
            {Material.LODESTONE, "Território", "territorio"}};

    /** Um menu aberto: o que cada espaço faz quando clicado (o booleano é o shift). */
    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        final Map<Integer, Consumer<Boolean>> acoes = new HashMap<>();
        Runnable voltar;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final Jornada jornada;
    private final Set<UUID> comPacote = new HashSet<>();

    public Ajuda(RPGAtributos plugin) {
        this.plugin = plugin;
        this.jornada = new Jornada(plugin);
    }

    public Jornada jornada() { return jornada; }

    // =====================================================================
    //  Pacote de recursos e título com fundo
    // =====================================================================

    @EventHandler
    public void aoStatusDoPacote(PlayerResourcePackStatusEvent e) {
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> comPacote.add(e.getPlayer().getUniqueId());
            case DECLINED, FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED -> comPacote.remove(e.getPlayer().getUniqueId());
            default -> { }
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        comPacote.remove(e.getPlayer().getUniqueId());
    }

    public boolean temPacote(Player p) {
        return comPacote.contains(p.getUniqueId());
    }

    /** Título do menu: com o pacote, desenha o fundo; sem ele, só o texto. */
    public Component titulo(Player p, Fundo f, String texto) {
        Component t = Component.text(texto);
        if (!temPacote(p)) return t;
        Component fundo = Component.text("" + Fundo.VOLTA_ANTES + f.letra() + Fundo.VOLTA_DEPOIS).font(FONTE).color(NamedTextColor.WHITE);
        return Component.text().append(fundo).append(t).build();
    }

    private Tela nova(Player p, Fundo f, String texto) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, titulo(p, f, texto));
        return t;
    }

    private void mostrar(Player p, Tela t) {
        if (!temPacote(p)) {
            ItemStack vidro = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
            vidro.editMeta(m -> m.setHideTooltip(true));
            for (int i = 0; i < t.inventario.getSize(); i++) if (t.inventario.getItem(i) == null) t.inventario.setItem(i, vidro);
        }
        p.openInventory(t.inventario);
    }

    private void botao(Tela t, int slot, ItemStack item, Consumer<Boolean> acao) {
        t.inventario.setItem(slot, item);
        if (acao != null) t.acoes.put(slot, acao);
    }

    private void comando(Player p, String cmd) {
        p.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> p.performCommand(cmd));
    }

    // =====================================================================
    //  /rpg
    // =====================================================================

    public void abrirRpg(Player p) {
        Tela t = nova(p, Fundo.RPG, "⚔ RPGAtributos");
        // Perfil.
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(p));
        List<Component> perfil = new ArrayList<>();
        perfil.add(linha("Nível total: ", String.valueOf(plugin.stats().nivelTotal(p)), NamedTextColor.YELLOW));
        var classe = plugin.classes().ativa(p);
        perfil.add(linha("Classe: ", classe == null ? "nenhuma" : classe.nome(), NamedTextColor.WHITE));
        var deus = plugin.deuses().deus(p);
        perfil.add(linha("Deus: ", deus == null ? "nenhum" : deus.nome(), NamedTextColor.WHITE));
        var reino = plugin.reinos().de(p);
        perfil.add(linha("Reino: ", reino == null ? "nenhum" : reino.nome(), NamedTextColor.WHITE));
        perfil.add(linha("Talentos livres: ", String.valueOf(plugin.talentos().livres(p)), NamedTextColor.AQUA));
        perfil.add(Component.empty());
        perfil.add(Component.text("» Clique: seus atributos", NamedTextColor.YELLOW));
        botao(t, S_PERFIL, enfeitar(cabeca, Component.text("⚔ " + p.getName(), COR, TextDecoration.BOLD), perfil), s -> comando(p, "atributos"));
        // Próximos passos.
        List<Sugestao> sug = sugestoes(p);
        for (int i = 0; i < S_PROXIMOS.length && i < sug.size(); i++) {
            Sugestao sg = sug.get(i);
            List<Component> lore = new ArrayList<>();
            for (String l : quebrar(sg.motivo(), 34)) lore.add(Component.text(l, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique: ver como fazer", NamedTextColor.YELLOW));
            ItemStack it = item(sg.icone(), Component.text("➜ " + sg.titulo(), i == 0 ? COR : NamedTextColor.WHITE, TextDecoration.BOLD), lore);
            if (i == 0) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            Topico tp = Topicos.porId(sg.topico());
            botao(t, S_PROXIMOS[i], it, s -> { if (tp != null) abrirTopico(p, tp, () -> abrirRpg(p)); });
        }
        // Categorias.
        Categoria[] cats = Categoria.values();
        for (int i = 0; i < cats.length; i++) {
            Categoria c = cats[i];
            List<Component> lore = new ArrayList<>();
            for (String l : quebrar(c.descricao(), 34)) lore.add(Component.text(l, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique: ver tudo", NamedTextColor.YELLOW));
            botao(t, S_CATEGORIAS[i], item(c.icone(), Component.text(c.nome(), c.cor(), TextDecoration.BOLD), lore), s -> abrirCategoria(p, c));
        }
        // Atalhos.
        for (int i = 0; i < ATALHOS.length; i++) {
            Object[] a = ATALHOS[i];
            String cmd = (String) a[2];
            botao(t, S_ATALHOS[i], item((Material) a[0], Component.text((String) a[1], NamedTextColor.WHITE, TextDecoration.BOLD),
                    List.of(Component.text("Atalho: /" + cmd, NamedTextColor.DARK_GRAY))), s -> comando(p, cmd));
        }
        // Barra de baixo.
        int feitos = jornada.feitos(p);
        botao(t, 45, item(Material.WRITABLE_BOOK, Component.text("✦ Jornada do Aventureiro", Jornada.COR, TextDecoration.BOLD), List.of(
                Component.text(feitos + " de " + Jornada.PASSOS.size() + " passos", NamedTextColor.WHITE),
                barra(feitos / (double) Jornada.PASSOS.size()),
                Component.empty(), Component.text("» Clique: ver os passos", NamedTextColor.YELLOW))), s -> abrirJornada(p));
        botao(t, 47, item(Material.OAK_SIGN, Component.text("Placar lateral", NamedTextColor.WHITE, TextDecoration.BOLD),
                List.of(Component.text("Liga ou desliga o placar do lado da tela.", NamedTextColor.GRAY))), s -> comando(p, "placar"));
        botao(t, S_MEIO, item(Material.KNOWLEDGE_BOOK, Component.text("Guia do Aventureiro", TextColor.color(0x6A1B9A), TextDecoration.BOLD), List.of(
                Component.text("Procure qualquer coisa no chat:", NamedTextColor.GRAY),
                Component.text("/guia <palavra>", NamedTextColor.WHITE),
                Component.text("Ex.: /guia alquimia, /guia reciclar", NamedTextColor.DARK_GRAY),
                Component.empty(), Component.text("» Clique: pegar o livro", NamedTextColor.YELLOW))), s -> comando(p, "guia livro"));
        botao(t, 51, item(Material.BOOK, Component.text("Diário", TextColor.color(0x8D6E63), TextDecoration.BOLD),
                List.of(Component.text("Os marcos da sua jornada.", NamedTextColor.GRAY))), s -> comando(p, "diario"));
        botao(t, S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()), s -> p.closeInventory());
        mostrar(p, t);
    }

    // =====================================================================
    //  Categoria e assunto
    // =====================================================================

    public void abrirCategoria(Player p, Categoria c) {
        Tela t = nova(p, Fundo.CATEGORIA, c.nome());
        t.voltar = () -> abrirRpg(p);
        List<Component> topo = new ArrayList<>();
        for (String l : quebrar(c.descricao(), 34)) topo.add(Component.text(l, NamedTextColor.GRAY));
        topo.add(Component.empty());
        topo.add(Component.text("Clique num assunto para ver como funciona.", NamedTextColor.DARK_GRAY));
        topo.add(Component.text("Shift + clique: abre direto (se tiver menu).", NamedTextColor.DARK_GRAY));
        botao(t, S_PERFIL, item(c.icone(), Component.text(c.nome(), c.cor(), TextDecoration.BOLD), topo), null);
        List<Topico> lista = Topicos.de(c);
        for (int i = 0; i < lista.size() && i < S_LISTA.length; i++) {
            Topico tp = lista.get(i);
            List<Component> lore = new ArrayList<>();
            for (String l : quebrar(tp.texto().getFirst(), 34)) lore.add(Component.text(l, NamedTextColor.GRAY));
            if (tp.temRitual()) lore.add(Component.text("✦ Estação: " + tp.nomeBloco(), NamedTextColor.GOLD));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique: como funciona", NamedTextColor.YELLOW));
            if (tp.comando() != null) lore.add(Component.text("» Shift + clique: abrir /" + tp.comando(), NamedTextColor.YELLOW));
            botao(t, S_LISTA[i], item(tp.icone(), Component.text(tp.nome(), c.cor(), TextDecoration.BOLD), lore), shift -> {
                if (shift && tp.comando() != null) comando(p, tp.comando());
                else abrirTopico(p, tp, () -> abrirCategoria(p, c));
            });
        }
        botoesBase(p, t);
        mostrar(p, t);
    }

    public void abrirTopico(Player p, Topico tp, Runnable voltar) {
        Tela t = nova(p, Fundo.TOPICO, tp.nome());
        t.voltar = voltar != null ? voltar : () -> abrirCategoria(p, tp.categoria());
        List<Component> lore = new ArrayList<>();
        for (String par : tp.texto()) {
            for (String l : quebrar(par, 36)) lore.add(Component.text(l, NamedTextColor.WHITE));
            lore.add(Component.empty());
        }
        lore.add(Component.text(tp.categoria().nome(), tp.categoria().cor()));
        ItemStack principal = item(tp.icone(), Component.text(tp.nome(), tp.categoria().cor(), TextDecoration.BOLD), lore);
        principal.editMeta(m -> m.setEnchantmentGlintOverride(true));
        botao(t, S_PERFIL, principal, null);
        // O ritual desenhado em itens.
        if (tp.temRitual()) {
            botao(t, 19, item(tp.bloco(), Component.text("1. Coloque: " + tp.nomeBloco(), NamedTextColor.GOLD, TextDecoration.BOLD),
                    List.of(Component.text("Este é o bloco da estação.", NamedTextColor.GRAY))), null);
            botao(t, 20, item(Material.HOPPER, Component.text("2. Jogue em cima (tecla Q):", NamedTextColor.GOLD, TextDecoration.BOLD),
                    List.of(Component.text("Fique perto e solte os itens", NamedTextColor.GRAY),
                            Component.text("em cima do bloco. Você tem 30 s.", NamedTextColor.GRAY))), null);
            int slot = 21;
            for (Object[] par : tp.itens()) {
                if (slot > 23) break;
                int qtd = (Integer) par[1];
                ItemStack it = par[0] instanceof ItemStack is ? is.clone() : new ItemStack((Material) par[0]);
                it.setAmount(Math.max(1, Math.min(64, qtd)));
                it.editMeta(m -> m.lore(List.of(Component.text(qtd + "×", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))));
                botao(t, slot++, it, null);
            }
            botao(t, 24, item(Material.SPECTRAL_ARROW, Component.text("➜", NamedTextColor.GOLD, TextDecoration.BOLD), List.of()), null);
            ItemStack vira = item(tp.icone(), Component.text("3. Vira: " + tp.nome(), NamedTextColor.GREEN, TextDecoration.BOLD),
                    List.of(Component.text("Clique com o botão direito nela para usar.", NamedTextColor.GRAY)));
            vira.editMeta(m -> m.setEnchantmentGlintOverride(true));
            botao(t, 25, vira, null);
        } else {
            botao(t, 22, item(Material.PAPER, Component.text("Não precisa montar nada", NamedTextColor.GRAY),
                    List.of(Component.text("Leia o texto acima e use o menu.", NamedTextColor.DARK_GRAY))), null);
        }
        if (tp.dica() != null) {
            List<Component> d = new ArrayList<>();
            for (String l : quebrar(tp.dica(), 34)) d.add(Component.text(l, NamedTextColor.WHITE));
            botao(t, 31, item(Material.GLOWSTONE_DUST, Component.text("Dica", NamedTextColor.YELLOW, TextDecoration.BOLD), d), null);
        }
        // Assuntos ligados.
        int i = 0;
        for (String id : tp.ligados()) {
            Topico l = Topicos.porId(id);
            if (l == null || i >= S_LIGADOS.length) continue;
            botao(t, S_LIGADOS[i++], item(l.icone(), Component.text("Veja também: " + l.nome(), l.categoria().cor()),
                    List.of(Component.text("» Clique para abrir", NamedTextColor.YELLOW))), s -> abrirTopico(p, l, () -> abrirTopico(p, tp, voltar)));
        }
        if (tp.comando() != null) {
            botao(t, S_MEIO, item(Material.LIME_DYE, Component.text("Abrir /" + tp.comando(), NamedTextColor.GREEN, TextDecoration.BOLD),
                    List.of(Component.text("Vai direto para o menu do sistema.", NamedTextColor.GRAY))), s -> comando(p, tp.comando()));
        }
        botoesBase(p, t);
        mostrar(p, t);
    }

    private void botoesBase(Player p, Tela t) {
        if (t.voltar != null) {
            Runnable v = t.voltar;
            botao(t, S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()), s -> v.run());
        }
        if (t.inventario.getItem(S_MEIO) == null) {
            botao(t, S_MEIO, item(Material.NETHER_STAR, Component.text("Menu /rpg", COR, TextDecoration.BOLD), List.of()), s -> abrirRpg(p));
        }
        botao(t, S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()), s -> p.closeInventory());
    }

    // =====================================================================
    //  Jornada
    // =====================================================================

    public void abrirJornada(Player p) {
        Tela t = nova(p, Fundo.JORNADA, "Jornada do Aventureiro");
        t.voltar = () -> abrirRpg(p);
        int feitos = jornada.feitos(p);
        botao(t, S_PERFIL, item(Material.WRITABLE_BOOK, Component.text("✦ Jornada do Aventureiro", Jornada.COR, TextDecoration.BOLD), List.of(
                Component.text(feitos + " de " + Jornada.PASSOS.size() + " passos", NamedTextColor.WHITE),
                barra(feitos / (double) Jornada.PASSOS.size()), Component.empty(),
                Component.text("Doze passos que ensinam o básico.", NamedTextColor.GRAY),
                Component.text("Cada um dá uma recompensa e se completa", NamedTextColor.GRAY),
                Component.text("sozinho quando você faz a coisa.", NamedTextColor.GRAY))), null);
        for (int i = 0; i < Jornada.PASSOS.size(); i++) {
            Jornada.Passo ps = Jornada.PASSOS.get(i);
            boolean feito = i < feitos, agora = i == feitos;
            List<Component> lore = new ArrayList<>();
            for (String l : quebrar(ps.objetivo(), 34)) lore.add(Component.text(l, feito ? NamedTextColor.DARK_GRAY : NamedTextColor.GRAY));
            lore.add(Component.text("Recompensa: " + ps.recompensa(), feito ? NamedTextColor.DARK_GRAY : NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(feito ? Component.text("✔ Feito", NamedTextColor.GREEN) : agora ? Component.text("◆ É agora!", Jornada.COR)
                    : Component.text("Depois do passo " + i, NamedTextColor.DARK_GRAY));
            lore.add(Component.text("» Clique: ver como fazer", NamedTextColor.YELLOW));
            ItemStack it = item(feito ? Material.LIME_DYE : agora ? ps.icone() : Material.GRAY_DYE,
                    Component.text((i + 1) + ". " + ps.nome(), feito ? NamedTextColor.GREEN : agora ? Jornada.COR : NamedTextColor.GRAY, TextDecoration.BOLD), lore);
            if (agora) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            Topico tp = Topicos.porId(ps.topico());
            botao(t, Fundo.CAMINHO[i], it, s -> { if (tp != null) abrirTopico(p, tp, () -> abrirJornada(p)); });
        }
        Jornada.Passo atual = jornada.atual(p);
        botao(t, 40, atual == null
                ? item(Material.NETHER_STAR, Component.text("Jornada completa!", Jornada.COR, TextDecoration.BOLD),
                        List.of(Component.text("Agora o mundo é seu. Veja o /rpg", NamedTextColor.GRAY), Component.text("para os próximos passos.", NamedTextColor.GRAY)))
                : item(Material.CHEST, Component.text("Próxima recompensa", Jornada.COR, TextDecoration.BOLD),
                        List.of(Component.text(atual.recompensa(), NamedTextColor.WHITE))), null);
        botoesBase(p, t);
        mostrar(p, t);
    }

    /** A cada 3 s: a Jornada confere os passos de quem está online. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) jornada.verificar(p);
    }

    // =====================================================================
    //  Próximos passos
    // =====================================================================

    record Sugestao(Material icone, String titulo, String motivo, String topico) { }

    /** Até 3 sugestões: o passo da Jornada e o que faz sentido agora para esse jogador. */
    List<Sugestao> sugestoes(Player p) {
        List<Sugestao> l = new ArrayList<>();
        Jornada.Passo ps = jornada.atual(p);
        if (ps != null) l.add(new Sugestao(ps.icone(), "Jornada: " + ps.nome(), ps.objetivo(), ps.topico()));
        int livres = plugin.talentos().livres(p);
        if (livres > 0) l.add(new Sugestao(Material.EXPERIENCE_BOTTLE, livres + " ponto(s) de talento",
                "Você tem pontos de talento para gastar. Abra /talentos.", "talentos"));
        boolean renasce = true;
        for (Skill s : Skill.values()) if (plugin.stats().getNivel(p, s) < 100) renasce = false;
        if (renasce) l.add(new Sugestao(Material.TOTEM_OF_UNDYING, "Você pode renascer", "Todos os atributos no 100: renasça para ganhar bônus para sempre.", "renascer"));
        var terr = plugin.territorios().de(p.getUniqueId());
        if (terr != null && terr.expansoes().size() < plugin.territorios().expansoesPermitidas(p)) {
            l.add(new Sugestao(Material.LODESTONE, "Expanda o território", "Você já pode criar um Marco de Expansão em outro lugar.", "territorio"));
        }
        var col = plugin.colonias().de(p.getUniqueId());
        if (terr != null && col == null) l.add(new Sugestao(Material.BELL, "Funde uma colônia", "Você tem território: um sino vira a Prefeitura e chegam moradores.", "colonia"));
        if (col != null && col.nivel() >= 2 && plugin.reinos().de(p) == null) {
            l.add(new Sugestao(Material.GOLDEN_HELMET, "Funde um reino", "Sua colônia já tem nível para um reino: /reino fundar <nome>.", "reino"));
        }
        if (plugin.classes().ativa(p) == null && ps != null && !"classes".equals(ps.topico())) {
            l.add(new Sugestao(Material.LECTERN, "Escolha uma classe", "Classes dão passivos e habilidades (agache + F).", "classes"));
        }
        if (plugin.deuses().deus(p) == null && (ps == null || !"deuses".equals(ps.topico()))) {
            l.add(new Sugestao(Material.CHISELED_QUARTZ_BLOCK, "Siga um deus", "A fé dá bônus passivos e um milagre.", "deuses"));
        }
        if (plugin.titulos().contador(p, "estruturas") < 3) {
            l.add(new Sugestao(Material.MOSSY_STONE_BRICKS, "Explore terras novas", "Estruturas com tesouros, guardas e moradores surgem onde ninguém foi.", "estruturas"));
        }
        l.add(new Sugestao(Material.IRON_SWORD, "Treine os combos", "Cada arma tem 7 golpes que liberam pela proficiência.", "combos"));
        l.add(new Sugestao(Material.ANVIL, "Forje algo melhor", "Ferraria alta deixa os itens forjados mais raros.", "forja"));
        // Sem repetir o mesmo assunto.
        List<Sugestao> unicas = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Sugestao s : l) if (vistos.add(s.topico())) unicas.add(s);
        return unicas;
    }

    // =====================================================================
    //  /guia
    // =====================================================================

    /** /guia (menu), /guia livro (o livro), /guia <palavra> (busca), /guia <assunto> (abre). */
    public void comandoGuia(Player p, String[] args) {
        if (args.length == 0) {
            abrirRpg(p);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("livro")) {
            plugin.guia().dar(p);
            return;
        }
        String busca = String.join(" ", args);
        Topico exato = Topicos.porId(Topicos.normalizar(busca));
        if (exato != null) {
            abrirTopico(p, exato, null);
            return;
        }
        List<Topico> achados = Topicos.buscar(busca);
        if (achados.isEmpty()) {
            p.sendMessage(Component.text("✎ Nada encontrado para \"" + busca + "\". Tente outra palavra ou abra o ", NamedTextColor.GRAY)
                    .append(Component.text("/rpg", COR).clickEvent(ClickEvent.runCommand("/rpg"))).append(Component.text(".", NamedTextColor.GRAY)));
            return;
        }
        if (achados.size() == 1) {
            abrirTopico(p, achados.getFirst(), null);
            return;
        }
        p.sendMessage(Component.text("✎ Guia — \"" + busca + "\" (clique para abrir):", COR, TextDecoration.BOLD));
        for (int i = 0; i < achados.size() && i < 8; i++) {
            Topico t = achados.get(i);
            p.sendMessage(Component.text("  ➜ ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(t.nome(), t.categoria().cor()).clickEvent(ClickEvent.runCommand("/guia " + t.id()))
                            .hoverEvent(HoverEvent.showText(Component.text(t.texto().getFirst(), NamedTextColor.GRAY))))
                    .append(Component.text("  (" + t.categoria().nome() + ")", NamedTextColor.DARK_GRAY)));
        }
    }

    public List<String> completarGuia(String parcial) {
        List<String> l = new ArrayList<>();
        l.add("livro");
        for (Topico t : Topicos.todos()) l.add(t.id());
        String p = parcial.toLowerCase();
        l.removeIf(s -> !s.startsWith(p));
        return l;
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= t.inventario.getSize()) return;
        Consumer<Boolean> acao = t.acoes.get(e.getRawSlot());
        if (acao == null) return;
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        acao.accept(e.isShiftClick());
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    private static Component linha(String rotulo, String valor, TextColor cor) {
        return Component.text(rotulo, NamedTextColor.GRAY).append(Component.text(valor, cor));
    }

    private static Component barra(double prog) {
        int cheios = (int) Math.round(Math.max(0, Math.min(1, prog)) * 20);
        return Component.text("■".repeat(cheios), COR).append(Component.text("■".repeat(20 - cheios), NamedTextColor.DARK_GRAY));
    }

    /** Quebra um texto em linhas de até {@code largura} letras (para caber no tooltip). */
    static List<String> quebrar(String texto, int largura) {
        List<String> l = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (atual.length() > 0 && atual.length() + 1 + palavra.length() > largura) {
                l.add(atual.toString());
                atual.setLength(0);
            }
            if (atual.length() > 0) atual.append(' ');
            atual.append(palavra);
        }
        if (atual.length() > 0) l.add(atual.toString());
        return l;
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return enfeitar(new ItemStack(m), nome, lore);
    }

    private static ItemStack enfeitar(ItemStack i, Component nome, List<Component> lore) {
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
