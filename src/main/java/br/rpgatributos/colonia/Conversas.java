package br.rpgatributos.colonia;

import br.rpgatributos.RPGAtributos;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Conversa em janela com os moradores (2.61): as janelas nativas do jogo, com perguntas que se abrem
 * conforme o laço cresce (de onde veio, do que tem medo, o sonho, o segredo) e a conversa do dia, que
 * soma laço como o botão de antes. As respostas vêm da história do próprio morador.
 */
public final class Conversas {

    private static final TextColor COR = TextColor.color(0xFF8FA8);
    private static final ClickCallback.Options OPCOES = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(15)).build();

    private final RPGAtributos plugin;

    public Conversas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private record Pergunta(String texto, int capitulo, java.util.function.Function<Lacos.Historia, String> resposta) { }

    private static final List<Pergunta> PERGUNTAS = List.of(
            new Pergunta("De onde você vem?", 1, h -> "Eu vim de " + h.origem() + "."),
            new Pergunta("Do que você tem medo?", 2, h -> "Vou te contar... eu tenho medo " + h.medo() + "."),
            new Pergunta("Qual é o seu sonho?", 3, h -> "Um dia eu quero " + h.sonho() + "."),
            new Pergunta("Tem algum segredo?", 4, h -> "Só para você: " + h.segredo() + "."));

    private static ActionButton botao(Component texto, String dica, Consumer<Player> aoClicar) {
        return ActionButton.create(texto, dica == null ? null : Component.text(dica), 200,
                DialogAction.customClick((view, quem) -> {
                    if (quem instanceof Player p) aoClicar.accept(p);
                }, OPCOES));
    }

    private static Dialog janela(Component titulo, List<DialogBody> corpo, List<ActionButton> botoes) {
        DialogBase base = DialogBase.builder(titulo).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(corpo).build();
        return Dialog.create(f -> f.empty().base(base).type(DialogType.multiAction(botoes).columns(1)
                .exitAction(ActionButton.create(Component.text("Até mais"), null, 200, null)).build()));
    }

    /**
     * Abre a conversa. {@code falas}: o que o morador diz no dia a dia; {@code voltar}: reabre o menu
     * dele (null = sem botão de voltar).
     */
    public void abrir(Player p, Lacos.Alvo a, Material icone, String[] falas, Runnable voltar) {
        int cap = plugin.lacos().capitulo(p, a.id());
        String fala = falas[ThreadLocalRandom.current().nextInt(falas.length)];
        List<DialogBody> corpo = new ArrayList<>();
        corpo.add(DialogBody.item(new ItemStack(icone)).showTooltip(false)
                .description(DialogBody.plainMessage(Component.text("♥".repeat(cap) + "♡".repeat(5 - cap) + "  laço: capítulo " + cap + " de 5", COR), 220))
                .build());
        corpo.add(DialogBody.plainMessage(Component.text("\"" + fala + "\"", NamedTextColor.WHITE), 300));
        List<ActionButton> botoes = new ArrayList<>();
        botoes.add(botao(Component.text("💬 Conversar um pouco", NamedTextColor.YELLOW), "Uma vez por dia: aumenta o laço", pl -> {
            pl.closeDialog();
            plugin.lacos().interagir(pl, a, falas);
        }));
        for (Pergunta q : PERGUNTAS) {
            boolean aberta = cap >= q.capitulo();
            botoes.add(botao(Component.text((aberta ? "» " : "🔒 ") + q.texto(), aberta ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY),
                    aberta ? null : "Vocês ainda não são tão próximos (capítulo " + q.capitulo() + " do laço)",
                    pl -> responder(pl, a, icone, falas, voltar, Component.text(aberta ? q.resposta().apply(a.historia())
                            : "Hum... ainda não te conheço o bastante para isso."))));
        }
        botoes.add(botao(Component.text("» Do que você gosta de ganhar?", NamedTextColor.WHITE), null,
                pl -> responder(pl, a, icone, falas, voltar, gostos(a.historia()))));
        if (voltar != null) botoes.add(botao(Component.text("◀ Voltar", NamedTextColor.GRAY), null, pl -> {
            pl.closeDialog();
            voltar.run();
        }));
        p.showDialog(janela(Component.text(a.nome(), COR, TextDecoration.BOLD), corpo, botoes));
    }

    private static Component gostos(Lacos.Historia h) {
        if (h.presentes().length == 0) return Component.text("Qualquer coisa feita com carinho.");
        Component c = Component.text("Ah, eu adoro ");
        for (int i = 0; i < h.presentes().length; i++) {
            if (i > 0) c = c.append(Component.text(" e "));
            c = c.append(Component.translatable(h.presentes()[i].translationKey()));
        }
        return c.append(Component.text(". Mas só um por dia, hein!"));
    }

    private void responder(Player p, Lacos.Alvo a, Material icone, String[] falas, Runnable voltar, Component resposta) {
        List<DialogBody> corpo = List.of(DialogBody.plainMessage(Component.text("\"").append(resposta).append(Component.text("\"")).color(NamedTextColor.WHITE), 300));
        List<ActionButton> botoes = List.of(botao(Component.text("◀ Mais perguntas", NamedTextColor.YELLOW), null,
                pl -> abrir(pl, a, icone, falas, voltar)));
        p.showDialog(janela(Component.text(a.nome(), COR, TextDecoration.BOLD), corpo, botoes));
    }
}
