package br.rpgatributos.conforto;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * /rpg: o menu central. O menu em si (próximos passos, categorias, atalhos e a Jornada) fica
 * em {@link br.rpgatributos.ajuda.Ajuda}; aqui ficam o comando e a dica da primeira entrada.
 */
public final class MenuRpg implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFD54F);

    private final RPGAtributos plugin;

    public MenuRpg(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        plugin.ajuda().abrirRpg(p);
    }

    /** Uma vez por jogador: apresenta o /rpg e a Jornada. */
    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var k = new NamespacedKey(plugin, "dica_rpg");
        if (p.getPersistentDataContainer().has(k)) return;
        p.getPersistentDataContainer().set(k, PersistentDataType.BYTE, (byte) 1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            p.sendMessage(Component.text("⚔ Bem-vindo! ", COR, TextDecoration.BOLD).append(Component.text("Abra o ", NamedTextColor.GRAY))
                    .append(Component.text("/rpg", COR).clickEvent(ClickEvent.runCommand("/rpg")))
                    .append(Component.text(": ele mostra o seu próximo passo e tudo o que dá para fazer. A ", NamedTextColor.GRAY))
                    .append(Component.text("Jornada do Aventureiro", COR).clickEvent(ClickEvent.runCommand("/jornada")))
                    .append(Component.text(" ensina o básico com recompensas. Dúvida? ", NamedTextColor.GRAY))
                    .append(Component.text("/guia <palavra>", NamedTextColor.WHITE).clickEvent(ClickEvent.suggestCommand("/guia ")))
                    .append(Component.text(".", NamedTextColor.GRAY)));
        }, 100L);
    }
}
