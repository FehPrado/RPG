package br.rpgatributos.ajuda;

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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Começo guiado (2.62): em vez de despejar tudo de uma vez, o jogo apresenta as coisas aos poucos, em
 * janelas nativas.
 * <ol>
 *   <li>Na primeira entrada: as boas-vindas, com 3 primeiros passos (raça, Jornada, Manual).</li>
 *   <li>Com 30 minutos de jogo: "ir mais longe" (Forja, classe, companheiro).</li>
 *   <li>Com 2 horas: "jogar junto" (party, território, colônia, reino).</li>
 * </ol>
 * Quem desligou as dicas (/guia dicas) não recebe as etapas 2 e 3. Quem já jogava antes da 2.62 com
 * bastante nível pula direto para o fim.
 */
public final class Boasvindas implements Listener {

    private static final NamespacedKey K_ETAPA = new NamespacedKey("rpgatributos", "guia_etapa");
    private static final TextColor COR = TextColor.color(0xC8A26B);
    private static final ClickCallback.Options OPCOES = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(20)).build();
    private static final int TRINTA_MIN = 20 * 60 * 30, DUAS_HORAS = 20 * 60 * 120;

    private final RPGAtributos plugin;
    private int segundos;

    public Boasvindas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static int etapa(Player p) {
        Integer e = p.getPersistentDataContainer().get(K_ETAPA, PersistentDataType.INTEGER);
        return e == null ? 0 : e;
    }

    private static void etapa(Player p, int e) {
        p.getPersistentDataContainer().set(K_ETAPA, PersistentDataType.INTEGER, e);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.settings().comecoGuiado || etapa(p) != 0) return;
        // Quem já jogava (com bastante nível) não precisa das boas-vindas.
        if (p.hasPlayedBefore() && plugin.stats().nivelTotal(p) >= 30) {
            etapa(p, 3);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || etapa(p) != 0) return;
            etapa(p, 1);
            boasVindas(p);
        }, 80L);
    }

    /** A cada segundo: confere o tempo de jogo de quem está nas etapas 1 e 2. */
    public void tick() {
        if (!plugin.settings().comecoGuiado || ++segundos % 60 != 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            int e = etapa(p);
            if (e < 1 || e > 2) continue;
            if (plugin.ajuda().dicas().desligadas(p)) {
                etapa(p, 3);
                continue;
            }
            int jogado = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            if (e == 1 && jogado >= TRINTA_MIN) {
                etapa(p, 2);
                maisLonge(p);
            } else if (e == 2 && jogado >= DUAS_HORAS) {
                etapa(p, 3);
                jogarJunto(p);
            }
        }
    }

    // =====================================================================
    //  As janelas
    // =====================================================================

    private static ActionButton botao(String texto, TextColor cor, String dica, Consumer<Player> aoClicar) {
        return ActionButton.create(Component.text(texto, cor), dica == null ? null : Component.text(dica), 200,
                DialogAction.customClick((view, quem) -> {
                    if (quem instanceof Player p) aoClicar.accept(p);
                }, OPCOES));
    }

    private static Dialog janela(Component titulo, List<DialogBody> corpo, List<ActionButton> botoes, String sair) {
        DialogBase base = DialogBase.builder(titulo).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(corpo).build();
        return Dialog.create(f -> f.empty().base(base).type(DialogType.multiAction(botoes).columns(1)
                .exitAction(ActionButton.create(Component.text(sair), null, 200, null)).build()));
    }

    private static DialogBody texto(String s, TextColor cor) {
        return DialogBody.plainMessage(Component.text(s, cor), 300);
    }

    private void comando(Player p, String cmd) {
        p.closeDialog();
        Bukkit.getScheduler().runTask(plugin, () -> p.performCommand(cmd));
    }

    private void assunto(Player p, String id) {
        Topico t = Topicos.porId(id);
        if (t != null) plugin.manualJanela().topico(p, t);
    }

    public void boasVindas(Player p) {
        List<DialogBody> corpo = new ArrayList<>();
        corpo.add(DialogBody.item(new ItemStack(Material.WRITTEN_BOOK)).showTooltip(false)
                .description(DialogBody.plainMessage(Component.text("É o Minecraft que você conhece, com mais história: vilas com moradores, ruínas, raças, "
                        + "uma forja com raridades, combos de arma e muito mais.", NamedTextColor.GRAY), 240)).build());
        corpo.add(texto("Você joga normal. As novidades aparecem aos poucos, quando fizerem sentido.", NamedTextColor.WHITE));
        corpo.add(texto("Três primeiros passos:", COR));
        corpo.add(texto("1. Escolha uma raça (ela muda o seu tamanho e cresce com você).", NamedTextColor.WHITE));
        corpo.add(texto("2. Siga a Jornada do Aventureiro: 12 passos com recompensa.", NamedTextColor.WHITE));
        corpo.add(texto("3. Na dúvida, abra o Manual (/manual) ou segure o item e use /guia mão.", NamedTextColor.WHITE));
        List<ActionButton> botoes = List.of(
                botao("🧝 Escolher a minha raça", NamedTextColor.GREEN, "Abre o /raca", pl -> comando(pl, "raca")),
                botao("✦ Ver a Jornada", NamedTextColor.GOLD, "Os primeiros passos, com recompensas", pl -> comando(pl, "jornada")),
                botao("📖 Abrir o Manual", NamedTextColor.AQUA, "Tudo o que o servidor tem", pl -> plugin.manualJanela().inicio(pl)));
        p.showDialog(janela(Component.text("Bem-vindo, " + p.getName() + "!", COR, TextDecoration.BOLD), corpo, botoes, "Só quero jogar"));
    }

    private void maisLonge(Player p) {
        List<DialogBody> corpo = List.of(
                texto("Meia hora de jogo! Quando quiser ir mais longe:", COR),
                texto("⚒ Forja do Ferreiro: armas e armaduras com raridade (de Comum a Mítica).", NamedTextColor.WHITE),
                texto("⚜ Classe: guerreiro, mago, arqueiro... cada uma com habilidades.", NamedTextColor.WHITE),
                texto("♞ Companheiro: um animal que luta, carrega e evolui com você.", NamedTextColor.WHITE));
        List<ActionButton> botoes = List.of(
                botao("⚒ Como fazer a Forja", NamedTextColor.GOLD, null, pl -> assunto(pl, "forja")),
                botao("⚜ As classes", NamedTextColor.LIGHT_PURPLE, null, pl -> assunto(pl, "classes")),
                botao("♞ Companheiros", NamedTextColor.GREEN, null, pl -> assunto(pl, "companheiros")));
        p.showDialog(janela(Component.text("Hora de ir mais longe", COR, TextDecoration.BOLD), corpo, botoes, "Depois"));
    }

    private void jogarJunto(Player p) {
        List<DialogBody> corpo = List.of(
                texto("Duas horas de jogo! Agora dá para jogar junto e construir:", COR),
                texto("👥 Party: dividir XP e não se ferir.", NamedTextColor.WHITE),
                texto("⚑ Território: um Marco protege a sua base.", NamedTextColor.WHITE),
                texto("🏘 Colônia: aldeões que trabalham para você.", NamedTextColor.WHITE),
                texto("♛ Reino: junte colônias, escolha um rei e vá à guerra.", NamedTextColor.WHITE));
        List<ActionButton> botoes = List.of(
                botao("👥 Party", NamedTextColor.AQUA, null, pl -> assunto(pl, "party")),
                botao("⚑ Território", NamedTextColor.GOLD, null, pl -> assunto(pl, "territorio")),
                botao("🏘 Colônia", NamedTextColor.GREEN, null, pl -> assunto(pl, "colonia")),
                botao("♛ Reino", NamedTextColor.LIGHT_PURPLE, null, pl -> assunto(pl, "reino")));
        p.showDialog(janela(Component.text("Jogar junto", COR, TextDecoration.BOLD), corpo, botoes, "Depois"));
    }
}
