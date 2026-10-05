package br.rpgatributos.reino;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /reino [fundar|convidar|aceitar|recusar|sair|expulsar|cargo|coroa|desfazer|tesouro|guerra|paz|lista] */
public final class ComandoReino implements TabExecutor {

    private final RPGAtributos plugin;

    public ComandoReino(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Só jogadores.", NamedTextColor.RED));
            return true;
        }
        Reinos r = plugin.reinos();
        if (args.length == 0) {
            r.abrir(p);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String erro = switch (sub) {
            case "fundar" -> args.length < 2 ? "Use: /reino fundar <nome>" : r.fundar(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            case "convidar" -> {
                if (args.length < 2) yield "Use: /reino convidar <nick>";
                Player alvo = Bukkit.getPlayerExact(args[1]);
                yield alvo == null ? "Jogador não encontrado (precisa estar online)." : r.convidar(p, alvo);
            }
            case "aceitar" -> r.aceitar(p);
            case "recusar" -> {
                r.recusar(p);
                yield null;
            }
            case "sair" -> r.sair(p);
            case "expulsar" -> args.length < 2 ? "Use: /reino expulsar <nick>" : r.expulsar(p, args[1]);
            case "cargo" -> {
                if (args.length < 3) yield "Use: /reino cargo <nick> <nobre|cavaleiro|cidadao>";
                Cargo c = Cargo.porId(args[2]);
                yield c == null ? "Cargo inválido: nobre, cavaleiro ou cidadao." : r.definirCargo(p, args[1], c);
            }
            case "coroa" -> args.length < 2 ? "Use: /reino coroa <nick>" : r.passarCoroa(p, args[1]);
            case "desfazer" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirmar")) yield "Para desfazer o reino use: /reino desfazer confirmar";
                yield r.desfazer(p);
            }
            case "tesouro" -> {
                if (args.length < 3) yield "Use: /reino tesouro <depositar|sacar> <quantidade>";
                int qtd;
                try { qtd = Integer.parseInt(args[2]); } catch (NumberFormatException e) { yield "Quantidade inválida."; }
                yield args[1].equalsIgnoreCase("sacar") ? r.sacar(p, qtd) : r.depositar(p, qtd);
            }
            case "guerra" -> args.length < 2 ? "Use: /reino guerra <reino>" : r.declararGuerra(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            case "paz" -> args.length < 2 ? "Use: /reino paz <reino>" : r.pedirPaz(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            case "lista" -> {
                p.sendMessage(Component.text("♛ Reinos do servidor:", NamedTextColor.GOLD));
                for (Reino x : r.todos()) {
                    p.sendMessage(Component.text(" " + x.nome(), x.cor()).append(Component.text(" — Rei " + x.nomeRei() + ", "
                            + x.membros().size() + " membros, " + x.vitorias() + " vitórias", NamedTextColor.GRAY)));
                }
                if (r.todos().isEmpty()) p.sendMessage(Component.text(" Nenhum ainda.", NamedTextColor.GRAY));
                yield null;
            }
            case "lei" -> {
                var lei = args.length < 2 ? null : Reinos.leiPorNome(args[1]);
                yield lei == null ? "Use: /reino lei <paz|explosoes|incendios|muralhas> (decreta ou revoga)" : r.alternarLei(p, lei);
            }
            case "mapa" -> {
                plugin.menusTerritorio().abrirMapa(p, p.getUniqueId());
                yield null;
            }
            case "provincias", "leis" -> {
                r.abrir(p);
                yield null;
            }
            default -> "Subcomandos: fundar, convidar, aceitar, recusar, sair, expulsar, cargo, coroa, desfazer, tesouro, guerra, paz, lei, mapa, provincias, lista";
        };
        if (erro != null) p.sendMessage(Component.text(erro, NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> op = new ArrayList<>();
        if (args.length == 1) {
            op.addAll(List.of("fundar", "convidar", "aceitar", "recusar", "sair", "expulsar", "cargo", "coroa", "desfazer",
                    "tesouro", "guerra", "paz", "lei", "mapa", "provincias", "lista"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "convidar", "expulsar", "cargo", "coroa" -> { return null; }
                case "tesouro" -> op.addAll(List.of("depositar", "sacar"));
                case "guerra", "paz" -> { for (Reino x : plugin.reinos().todos()) op.add(x.nome()); }
                case "desfazer" -> op.add("confirmar");
                case "lei" -> op.addAll(List.of("paz", "explosoes", "incendios", "muralhas"));
                default -> { }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("cargo")) {
            op.addAll(List.of("nobre", "cavaleiro", "cidadao"));
        }
        String ultimo = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        op.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(ultimo));
        return op;
    }
}
