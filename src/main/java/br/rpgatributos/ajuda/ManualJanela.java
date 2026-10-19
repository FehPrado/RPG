package br.rpgatributos.ajuda;

import br.rpgatributos.RPGAtributos;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * O Manual em janela (2.59): as telas nativas do jogo ("diálogos", 1.21.6+), sem mod. Mostra os mesmos
 * assuntos do /guia em páginas com texto, ícone, botões e busca, e um botão que abre o Manual completo
 * no navegador. O link também vai para o menu de pausa do jogo (Esc → Links do servidor) e para o chat
 * ({@code /manual link}).
 */
public final class ManualJanela implements CommandExecutor, TabCompleter {

    private static final TextColor COR = TextColor.color(0xC8A26B);
    private static final int LARGURA = 320;
    private static final ClickCallback.Options OPCOES = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(30)).build();

    private final RPGAtributos plugin;

    public ManualJanela(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private String url() {
        return plugin.settings().manualUrl;
    }

    /** Põe o Manual nos "Links do servidor" do menu de pausa. */
    public void registrarLink() {
        String u = url();
        if (u == null || u.isBlank()) return;
        try {
            Bukkit.getServerLinks().addLink(Component.text("📖 Manual do RPGAtributos"), URI.create(u));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Link do Manual inválido (manual.url): " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Comando
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Manual: " + url()));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("link")) {
            link(p);
            return true;
        }
        if (args.length > 0) {
            buscar(p, String.join(" ", args));
            return true;
        }
        inicio(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> l = new ArrayList<>();
        String a = args[0].toLowerCase(Locale.ROOT);
        if ("link".startsWith(a)) l.add("link");
        for (Topico t : Topicos.todos()) if (t.id().startsWith(a)) l.add(t.id());
        return l;
    }

    /** O link clicável no chat (o jogo pergunta antes de abrir o navegador). */
    public void link(Player p) {
        p.sendMessage(Component.text("📖 ", COR).append(Component.text("Abrir o Manual do RPGAtributos", COR, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url()))
                .hoverEvent(HoverEvent.showText(Component.text("Abre no navegador: " + url(), NamedTextColor.GRAY))))
                .append(Component.text("  (ou /manual para ler aqui dentro)", NamedTextColor.GRAY)));
    }

    // =====================================================================
    //  As janelas
    // =====================================================================

    private static ActionButton botao(String texto, TextColor cor, String dica, int largura, Consumer<Player> aoClicar) {
        return ActionButton.create(Component.text(texto, cor), dica == null ? null : Component.text(dica), largura,
                DialogAction.customClick((view, quem) -> {
                    if (quem instanceof Player pl) aoClicar.accept(pl);
                }, OPCOES));
    }

    private ActionButton site() {
        return ActionButton.create(Component.text("🌐 Abrir no navegador", NamedTextColor.AQUA),
                Component.text("O Manual completo, com imagens e as receitas"), 150, DialogAction.staticAction(ClickEvent.openUrl(url())));
    }

    private static ActionButton fechar() {
        return ActionButton.create(Component.text("Fechar"), null, 100, null);
    }

    private static Dialog janela(Component titulo, List<DialogBody> corpo, List<DialogInput> campos, List<ActionButton> botoes, int colunas) {
        DialogBase base = DialogBase.builder(titulo).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(corpo).inputs(campos).build();
        return Dialog.create(f -> f.empty().base(base).type(DialogType.multiAction(botoes).columns(colunas).exitAction(fechar()).build()));
    }

    private static DialogBody texto(Component c) {
        return DialogBody.plainMessage(c, LARGURA);
    }

    /** Tela inicial: as áreas, a busca, as novidades e o site. */
    public void inicio(Player p) {
        List<DialogBody> corpo = List.of(
                DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).showTooltip(false).description(DialogBody.plainMessage(
                        Component.text("Tudo o que o RPGAtributos acrescenta ao Minecraft, organizado por área. Escolha uma, ou digite o que procura.",
                                NamedTextColor.GRAY), 260)).build(),
                texto(Component.text("Versão " + plugin.getPluginMeta().getVersion() + " · " + Topicos.todos().size() + " assuntos", NamedTextColor.DARK_GRAY)));
        List<DialogInput> campos = List.of(DialogInput.text("busca", Component.text("Buscar um assunto (ex.: forja, peixe, elevador)"))
                .width(LARGURA).maxLength(40).build());
        List<ActionButton> botoes = new ArrayList<>();
        for (Categoria c : Categoria.values()) {
            botoes.add(botao(c.nome(), c.cor(), c.descricao(), 150, pl -> categoria(pl, c)));
        }
        botoes.add(ActionButton.create(Component.text("🔍 Buscar", NamedTextColor.YELLOW), Component.text("Busca o que você digitou acima"), 150,
                DialogAction.customClick((view, quem) -> {
                    if (quem instanceof Player pl) buscar(pl, view.getText("busca"));
                }, OPCOES)));
        botoes.add(botao("✦ Novidades da versão", NamedTextColor.GOLD, "O que entrou agora", 150, pl -> {
            pl.closeDialog();
            plugin.ajuda().abrirNovidades(pl);
        }));
        botoes.add(site());
        p.showDialog(janela(Component.text("📖 Manual do RPGAtributos", COR, TextDecoration.BOLD), corpo, campos, botoes, 2));
    }

    /** Uma área: os assuntos dela. */
    public void categoria(Player p, Categoria c) {
        List<Topico> l = Topicos.de(c);
        List<ActionButton> botoes = new ArrayList<>();
        for (Topico t : l) {
            String dica = t.texto().isEmpty() ? null : t.texto().getFirst();
            if (dica != null && dica.length() > 120) dica = dica.substring(0, 117) + "...";
            botoes.add(botao(t.nome(), NamedTextColor.WHITE, dica, 150, pl -> topico(pl, t)));
        }
        botoes.add(botao("◀ Início", NamedTextColor.GRAY, null, 150, this::inicio));
        List<DialogBody> corpo = List.of(
                DialogBody.item(new ItemStack(c.icone())).showTooltip(false)
                        .description(DialogBody.plainMessage(Component.text(c.descricao(), NamedTextColor.GRAY), 260)).build(),
                texto(Component.text(l.size() + " assuntos. Passe o mouse num botão para ver do que se trata.", NamedTextColor.DARK_GRAY)));
        p.showDialog(janela(Component.text(c.nome(), c.cor(), TextDecoration.BOLD), corpo, List.of(), botoes, 2));
    }

    /** Um assunto: o texto, o ritual, a dica, o comando e os assuntos ligados. */
    public void topico(Player p, Topico t) {
        List<DialogBody> corpo = new ArrayList<>();
        corpo.add(DialogBody.item(new ItemStack(t.icone())).showTooltip(false)
                .description(DialogBody.plainMessage(Component.text(t.categoria().nome(), t.categoria().cor()), 200)).build());
        for (String linha : t.texto()) corpo.add(texto(Component.text(linha)));
        if (t.temRitual()) {
            Component r = Component.text("⚒ Como fazer: jogue (tecla Q) ", NamedTextColor.GOLD);
            List<Object[]> itens = t.itens();
            for (int i = 0; i < itens.size(); i++) {
                Object[] it = itens.get(i);
                Component nome = it[0] instanceof ItemStack s ? s.effectiveName() : Component.translatable(((Material) it[0]).translationKey());
                r = r.append(Component.text(it[1] + " ", NamedTextColor.WHITE)).append(nome.colorIfAbsent(NamedTextColor.WHITE))
                        .append(Component.text(i < itens.size() - 2 ? ", " : i == itens.size() - 2 ? " e " : "", NamedTextColor.GOLD));
            }
            r = r.append(Component.text(" em cima de " + (t.nomeBloco() != null ? t.nomeBloco() : "um bloco") + ".", NamedTextColor.GOLD));
            corpo.add(texto(r));
        }
        if (t.dica() != null) corpo.add(texto(Component.text("💡 " + t.dica(), NamedTextColor.YELLOW)));
        if (t.comando() != null) corpo.add(texto(Component.text("Comando: /" + t.comando(), NamedTextColor.AQUA)));
        List<ActionButton> botoes = new ArrayList<>();
        if (t.comando() != null) {
            botoes.add(botao("▶ Abrir /" + t.comando(), NamedTextColor.GREEN, "Fecha o Manual e abre o sistema", 150, pl -> {
                pl.closeDialog();
                pl.performCommand(t.comando());
            }));
        }
        for (String id : t.ligados()) {
            Topico l = Topicos.porId(id);
            if (l != null && botoes.size() < 7) botoes.add(botao("→ " + l.nome(), NamedTextColor.WHITE, "Assunto ligado", 150, pl -> topico(pl, l)));
        }
        botoes.add(botao("◀ " + t.categoria().nome(), NamedTextColor.GRAY, null, 150, pl -> categoria(pl, t.categoria())));
        botoes.add(botao("🏠 Início", NamedTextColor.GRAY, null, 150, this::inicio));
        botoes.add(site());
        p.showDialog(janela(Component.text(t.nome(), t.categoria().cor(), TextDecoration.BOLD), corpo, List.of(), botoes, 2));
    }

    /** Resultado da busca (um só: abre direto). */
    public void buscar(Player p, String termo) {
        if (termo == null || termo.isBlank()) {
            inicio(p);
            return;
        }
        Topico exato = Topicos.porId(termo.toLowerCase(Locale.ROOT).trim());
        List<Topico> achou = exato != null ? List.of(exato) : Topicos.buscar(termo);
        if (achou.size() == 1) {
            topico(p, achou.getFirst());
            return;
        }
        List<ActionButton> botoes = new ArrayList<>();
        for (Topico t : achou) {
            if (botoes.size() >= 16) break;
            botoes.add(botao(t.nome(), t.categoria().cor(), t.categoria().nome(), 150, pl -> topico(pl, t)));
        }
        botoes.add(botao("◀ Início", NamedTextColor.GRAY, null, 150, this::inicio));
        List<DialogBody> corpo = List.of(texto(achou.isEmpty()
                ? Component.text("Nada encontrado para \"" + termo + "\". Tente outra palavra (ex.: forja, peixe, raça).", NamedTextColor.GRAY)
                : Component.text(achou.size() + " assunto(s) com \"" + termo + "\"" + (achou.size() > 16 ? " (mostrando 16)" : "") + ".", NamedTextColor.GRAY)));
        p.showDialog(janela(Component.text("🔍 Busca", COR, TextDecoration.BOLD), corpo, List.of(), botoes, 2));
    }
}
