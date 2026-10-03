package br.rpgatributos.party;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /party (menu e subcomandos) e /pc (chat da party). */
public final class ComandoParty implements TabExecutor {

    private static final List<String> SUBS = List.of("convidar", "aceitar", "recusar", "sair", "expulsar",
            "lider", "desfazer", "chat", "fogoamigo", "criar");

    private final RPGAtributos plugin;

    public ComandoParty(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Parties parties() { return plugin.parties(); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Só jogadores.", NamedTextColor.RED));
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("pc")) {
            if (args.length == 0) {
                boolean ligado = parties().alternarChat(p);
                p.sendMessage(Component.text("✉ Chat da party " + (ligado ? "ligado." : "desligado."), Parties.COR));
            } else {
                erro(p, parties().enviarChat(p, Component.text(String.join(" ", args))));
            }
            return true;
        }
        if (args.length == 0) {
            plugin.menuParty().abrir(p);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "criar" -> erro(p, parties().criar(p));
            case "convidar", "convite", "invite" -> {
                if (args.length < 2) { erro(p, "Use: /party convidar <nick>"); return true; }
                Player alvo = Bukkit.getPlayerExact(args[1]);
                if (alvo == null) { erro(p, "Jogador não encontrado (precisa estar online)."); return true; }
                erro(p, parties().convidar(p, alvo));
            }
            case "aceitar" -> erro(p, parties().aceitar(p));
            case "recusar" -> erro(p, parties().recusar(p));
            case "sair" -> erro(p, parties().sair(p));
            case "expulsar", "lider" -> {
                if (args.length < 2) { erro(p, "Use: /party " + sub + " <nick>"); return true; }
                UUID alvo = membro(p, args[1]);
                if (alvo == null) { erro(p, "Esse jogador não é da sua party."); return true; }
                erro(p, sub.equals("lider") ? parties().passarLideranca(p, alvo) : parties().expulsar(p, alvo));
            }
            case "desfazer" -> erro(p, parties().desfazer(p));
            case "fogoamigo" -> erro(p, parties().alternarFogoAmigo(p));
            case "chat" -> {
                boolean ligado = parties().alternarChat(p);
                p.sendMessage(Component.text("✉ Chat da party " + (ligado ? "ligado." : "desligado."), Parties.COR));
            }
            default -> p.sendMessage(Component.text("""
                    /party  (abre o menu)
                    /party convidar <nick>
                    /party aceitar | recusar
                    /party sair
                    /party expulsar <nick>  (líder)
                    /party lider <nick>  (passa a liderança)
                    /party desfazer  (líder)
                    /party fogoamigo  (líder)
                    /party chat  ou  /pc <mensagem>""", NamedTextColor.YELLOW));
        }
        return true;
    }

    /** Membro da party do jogador pelo nome (funciona com quem está offline). */
    private UUID membro(Player p, String nome) {
        Party pt = parties().party(p);
        if (pt == null) return null;
        for (Map.Entry<UUID, String> m : pt.membros().entrySet()) {
            if (m.getValue().equalsIgnoreCase(nome)) return m.getKey();
        }
        return null;
    }

    private static void erro(Player p, String msg) {
        if (msg != null) p.sendMessage(Component.text(msg, NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pc") || !(sender instanceof Player p)) return List.of();
        List<String> op = new ArrayList<>();
        if (args.length == 1) {
            op.addAll(SUBS);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("convidar")) return null; // nomes de jogadores online
            if (sub.equals("expulsar") || sub.equals("lider")) {
                Party pt = parties().party(p);
                if (pt != null) for (Map.Entry<UUID, String> m : pt.membros().entrySet()) {
                    if (!m.getKey().equals(p.getUniqueId())) op.add(m.getValue());
                }
            }
        }
        String ultimo = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        op.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(ultimo));
        return op;
    }
}
