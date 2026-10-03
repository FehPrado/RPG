package br.rpgatributos.territorio;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /territorio: menu, mapa, bordas, reivindicar, liberar, membros, abandonar. */
public final class ComandoTerritorio implements TabExecutor {

    private static final List<String> SUBS = List.of("mapa", "bordas", "info", "reivindicar", "liberar",
            "adicionar", "remover", "abandonar");

    private final RPGAtributos plugin;

    public ComandoTerritorio(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Territorios ter() { return plugin.territorios(); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Só jogadores.", NamedTextColor.RED));
            return true;
        }
        Territorio meu = ter().de(p.getUniqueId());
        if (args.length == 0) {
            plugin.menusTerritorio().abrirPrincipal(p, meu);
            return true;
        }
        Location aqui = p.getLocation();
        String mundo = aqui.getWorld().getName();
        int cx = aqui.getBlockX() >> 4, cz = aqui.getBlockZ() >> 4;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "mapa" -> plugin.menusTerritorio().abrirMapa(p, meu == null ? null : meu.dono());
            case "bordas", "ver" -> {
                ter().mostrarBordas(p, 20);
                p.sendMessage(Component.text("◎ Mostrando as bordas por 20 segundos.", NamedTextColor.LIGHT_PURPLE));
            }
            case "info" -> {
                Territorio t = ter().em(aqui);
                if (t == null) {
                    p.sendMessage(Component.text("☘ Aqui são terras livres (chunk " + cx + ", " + cz + ").", NamedTextColor.GREEN));
                } else {
                    p.sendMessage(Component.text("⚑ Território de " + t.nomeDono() + " (" + t.chunks().size() + " chunks). "
                            + (ter().podeConstruir(p, aqui) ? "Você pode construir aqui." : "Você é visitante."), Territorios.COR));
                }
            }
            case "reivindicar", "claim" -> {
                String erro = ter().reivindicar(p, mundo, cx, cz);
                if (erro != null) erro(p, erro);
                else {
                    p.sendMessage(Component.text("⚑ Chunk reivindicado! (" + meu.chunks().size() + "/" + ter().limite(p) + ")", Territorios.COR));
                    ter().mostrarBordas(p, 8);
                }
            }
            case "liberar", "unclaim" -> {
                String erro = ter().liberar(meu, mundo, cx, cz);
                if (erro != null) erro(p, erro);
                else p.sendMessage(Component.text("Chunk liberado.", NamedTextColor.GRAY));
            }
            case "adicionar", "remover" -> {
                if (meu == null) { erro(p, "Você não tem território."); return true; }
                if (args.length < 2) { erro(p, "Use: /territorio " + args[0] + " <nick>"); return true; }
                if (args[0].equalsIgnoreCase("adicionar")) {
                    OfflinePlayer alvo = Bukkit.getPlayerExact(args[1]);
                    if (alvo == null) alvo = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (alvo == null) { erro(p, "Jogador não encontrado (precisa já ter entrado no servidor)."); return true; }
                    String erro = ter().adicionarMembro(meu, alvo);
                    if (erro != null) erro(p, erro);
                    else p.sendMessage(Component.text("⚑ " + alvo.getName() + " agora é membro do seu território.", Territorios.COR));
                } else {
                    UUID alvo = null;
                    for (Map.Entry<UUID, String> m : meu.membros().entrySet()) {
                        if (m.getValue().equalsIgnoreCase(args[1])) alvo = m.getKey();
                    }
                    if (alvo == null) { erro(p, "Esse jogador não é membro."); return true; }
                    ter().removerMembro(meu, alvo);
                    p.sendMessage(Component.text("⚑ " + args[1] + " não é mais membro.", NamedTextColor.GRAY));
                }
            }
            case "abandonar" -> {
                if (meu == null) { erro(p, "Você não tem território."); return true; }
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirmar")) {
                    erro(p, "Isso apaga o território inteiro. Para confirmar: /territorio abandonar confirmar");
                    return true;
                }
                ter().abandonar(meu);
                p.sendMessage(Component.text("⚑ Território abandonado.", NamedTextColor.GRAY));
            }
            default -> p.sendMessage(Component.text("""
                    /territorio  (abre o menu)
                    /territorio bordas  (mostra as bordas)
                    /territorio info  (de quem é este chunk)
                    /territorio reivindicar | liberar  (o chunk onde você está)
                    /territorio adicionar | remover <nick>
                    /territorio abandonar confirmar""", NamedTextColor.YELLOW));
        }
        return true;
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        List<String> op = new ArrayList<>();
        if (args.length == 1) {
            op.addAll(SUBS);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("adicionar")) return null;
            if (sub.equals("abandonar")) op.add("confirmar");
            if (sub.equals("remover")) {
                Territorio meu = ter().de(p.getUniqueId());
                if (meu != null) op.addAll(meu.membros().values());
            }
        }
        String ultimo = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        op.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(ultimo));
        return op;
    }
}
