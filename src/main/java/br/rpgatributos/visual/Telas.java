package br.rpgatributos.visual;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 3.2: telas de diálogo do jogo (a janela com texto e botões). Os NPCs conversam nelas com
 * escolhas, e as ações sem volta pedem confirmação nelas. Cada botão chama o plugin uma vez,
 * por até 10 minutos; depois disso, ou se a tela fechar, nada acontece.
 */
public final class Telas {

    /** Um botão: o texto, a dica (ao passar o mouse; pode ser null) e o que acontece. */
    public record Opcao(Component rotulo, Component dica, Consumer<Player> acao) {
        public static Opcao de(String rotulo, String dica, Consumer<Player> acao) {
            return new Opcao(Component.text(rotulo), dica == null ? null : Component.text(dica, NamedTextColor.GRAY), acao);
        }
    }

    private static final ClickCallback.Options UMA_VEZ = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(10)).build();

    private Telas() {
    }

    private static Plugin plugin() {
        return Bukkit.getPluginManager().getPlugin("RPGAtributos");
    }

    private static ActionButton botao(Opcao o, int largura) {
        DialogAction acao = DialogAction.customClick((resposta, quem) -> {
            if (!(quem instanceof Player p) || o.acao() == null) return;
            Bukkit.getScheduler().runTask(plugin(), () -> {
                if (p.isOnline()) o.acao().accept(p);
            });
        }, UMA_VEZ);
        ActionButton.Builder b = ActionButton.builder(o.rotulo()).width(largura).action(acao);
        if (o.dica() != null) b.tooltip(o.dica());
        return b.build();
    }

    private static List<DialogBody> corpo(ItemStack icone, List<Component> texto) {
        List<DialogBody> c = new ArrayList<>();
        if (icone != null && !icone.isEmpty()) c.add(DialogBody.item(icone).showTooltip(false).showDecorations(false).build());
        for (Component t : texto) c.add(DialogBody.plainMessage(t, 300));
        return c;
    }

    /**
     * Uma conversa: título (o nome de quem fala), um ícone opcional, as falas e os botões
     * (um embaixo do outro). "Até mais" fecha sem fazer nada.
     */
    public static void conversa(Player p, Component titulo, ItemStack icone, List<Component> falas, List<Opcao> opcoes) {
        List<ActionButton> botoes = new ArrayList<>();
        for (Opcao o : opcoes) botoes.add(botao(o, 260));
        ActionButton sair = ActionButton.builder(Component.text("Até mais", NamedTextColor.GRAY)).width(140).build();
        // Sem escolhas, a tela é só um aviso com o botão de sair (a de várias ações pede pelo menos uma).
        DialogType tipo = botoes.isEmpty() ? DialogType.notice(sair) : DialogType.multiAction(botoes).exitAction(sair).columns(1).build();
        Dialog d = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(titulo).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE).body(corpo(icone, falas)).build())
                .type(tipo));
        p.showDialog(d);
    }

    /** Uma confirmação para o que não tem volta: o botão vermelho faz, o outro desiste. */
    public static void confirmar(Player p, Component titulo, List<Component> texto, String sim, String nao, Consumer<Player> aoConfirmar) {
        ActionButton ok = botao(new Opcao(Component.text(sim, NamedTextColor.RED), Component.text("Não dá para desfazer.", NamedTextColor.RED), aoConfirmar), 160);
        ActionButton volta = ActionButton.builder(Component.text(nao)).width(160).build();
        Dialog d = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(titulo).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE).body(corpo(null, texto)).build())
                .type(DialogType.confirmation(ok, volta)));
        p.showDialog(d);
    }
}
