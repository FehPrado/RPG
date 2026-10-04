package br.rpgatributos.conforto;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.mundo.Ceu;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Placar lateral opcional (/placar): nível, vigor, mana, estação, lua, hora, Descansado e
 * talentos livres. Cada jogador que liga ganha um placar só dele; os times do placar principal
 * (como o que esconde o nome por causa da tag) são copiados para ele.
 */
public final class Placar implements Listener, CommandExecutor {

    private static final TextColor COR = TextColor.color(0xFFD54F);
    private static final int LINHAS = 10;

    private final RPGAtributos plugin;
    private final NamespacedKey kLigado;
    private final Map<UUID, Scoreboard> quadros = new HashMap<>();
    private final Map<UUID, Integer> usadas = new HashMap<>();
    private int passos;

    public Placar(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kLigado = new NamespacedKey(plugin, "placar");
    }

    public boolean ligado(Player p) {
        return p.getPersistentDataContainer().has(kLigado);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (ligado(p)) {
            p.getPersistentDataContainer().remove(kLigado);
            desligar(p);
            p.sendMessage(Component.text("Placar lateral desligado.", NamedTextColor.GRAY));
        } else {
            p.getPersistentDataContainer().set(kLigado, PersistentDataType.BYTE, (byte) 1);
            ligar(p);
            p.sendMessage(Component.text("Placar lateral ligado. Use /placar de novo para desligar.", COR));
        }
        return true;
    }

    private void ligar(Player p) {
        Scoreboard sb = plugin.getServer().getScoreboardManager().getNewScoreboard();
        Objective o = sb.registerNewObjective("rpg", Criteria.DUMMY, Component.text("⚔ RPGAtributos", COR, TextDecoration.BOLD));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        o.numberFormat(NumberFormat.blank());
        copiarTimes(sb);
        quadros.put(p.getUniqueId(), sb);
        p.setScoreboard(sb);
        atualizar(p, sb);
    }

    private void desligar(Player p) {
        quadros.remove(p.getUniqueId());
        usadas.remove(p.getUniqueId());
        p.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (ligado(p)) plugin.getServer().getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) ligar(p); }, 20L);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        quadros.remove(e.getPlayer().getUniqueId());
        usadas.remove(e.getPlayer().getUniqueId());
    }

    /** Todo segundo: as linhas; a cada 5 s, os times. */
    public void tick() {
        passos++;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Scoreboard sb = quadros.get(p.getUniqueId());
            if (sb == null) continue;
            if (passos % 5 == 0) copiarTimes(sb);
            atualizar(p, sb);
        }
    }

    private void atualizar(Player p, Scoreboard sb) {
        Objective o = sb.getObjective("rpg");
        if (o == null) return;
        List<Component> l = new ArrayList<>();
        int renasceu = plugin.renascimento().vezes(p);
        l.add(Component.text("Nível total ", NamedTextColor.GRAY).append(Component.text(plugin.stats().nivelTotal(p), NamedTextColor.YELLOW))
                .append(Component.text(renasceu > 0 ? " " + "★".repeat(renasceu) : "", NamedTextColor.GOLD)));
        l.add(Component.text("Vigor ", NamedTextColor.GRAY).append(Component.text((int) plugin.combos().vigor(p) + "/" + (int) plugin.combos().vigorMaximo(p), NamedTextColor.GOLD)));
        var pf = plugin.arcano().perfil(p);
        l.add(Component.text("Mana ", NamedTextColor.GRAY).append(Component.text((int) Math.max(0, pf.mana()) + "/" + (int) plugin.arcano().manaMax(p, pf), TextColor.color(0x7F8CFF))));
        l.add(Component.empty());
        var est = plugin.estacoes().atual();
        l.add(Component.text(est.icone() + " " + est.nome() + " · dia " + plugin.estacoes().dia(), TextColor.color(est.cor())));
        long t = (p.getWorld().getTime() + 6000) % 24000;
        l.add(Component.text("☾ " + Ceu.nomeFase(p.getWorld()) + " · " + String.format("%02d:%02d", t / 1000, t % 1000 * 60 / 1000), NamedTextColor.GRAY));
        long descanso = plugin.fogueiras().descansadoAte(p) - System.currentTimeMillis();
        if (descanso > 0) l.add(Component.text("♨ Descansado " + (descanso / 60000 + 1) + " min", TextColor.color(0xFF8A3D)));
        int livres = plugin.talentos().livres(p);
        if (livres > 0) l.add(Component.text("✦ " + livres + " ponto(s) de talento", TextColor.color(0xB39DDB)));
        int n = Math.min(LINHAS, l.size());
        for (int i = 0; i < n; i++) {
            var score = o.getScore("l" + i);
            score.setScore(LINHAS - i);
            score.customName(l.get(i));
        }
        int antes = usadas.getOrDefault(p.getUniqueId(), 0);
        for (int i = n; i < antes; i++) sb.resetScores("l" + i);
        usadas.put(p.getUniqueId(), n);
    }

    /** Os times do placar principal (cores, tags escondidas...) também valem no placar do jogador. */
    private void copiarTimes(Scoreboard sb) {
        Scoreboard principal = plugin.getServer().getScoreboardManager().getMainScoreboard();
        for (Team t : principal.getTeams()) {
            Team c = sb.getTeam(t.getName());
            if (c == null) c = sb.registerNewTeam(t.getName());
            c.displayName(t.displayName());
            c.prefix(t.prefix());
            c.suffix(t.suffix());
            if (t.hasColor()) c.color(NamedTextColor.nearestTo(t.color()));
            c.setAllowFriendlyFire(t.allowFriendlyFire());
            c.setCanSeeFriendlyInvisibles(t.canSeeFriendlyInvisibles());
            for (Team.Option op : Team.Option.values()) c.setOption(op, t.getOption(op));
            for (String e : t.getEntries()) if (!c.hasEntry(e)) c.addEntry(e);
            for (String e : new ArrayList<>(c.getEntries())) if (!t.hasEntry(e)) c.removeEntry(e);
        }
        for (Team c : new ArrayList<>(sb.getTeams())) if (principal.getTeam(c.getName()) == null) c.unregister();
    }

    public void desligarTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) if (quadros.containsKey(p.getUniqueId())) desligar(p);
    }
}
