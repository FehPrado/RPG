package br.rpgatributos;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tag personalizada em cima da cabeça.
 *
 * Como funciona: o nome normal do jogador é escondido (time do scoreboard) e um
 * TextDisplay acompanha o jogador a cada tick mostrando o texto escolhido.
 * O TextDisplay NÃO é montado no jogador de propósito: no Paper, jogador com
 * passageiro não consegue ser teleportado (/home, /spawn, /tpa falhariam).
 */
public final class TagManager implements Listener {

    private static final String NOME_TIME = "rpgatributos_tag";
    private static final LegacyComponentSerializer CORES = LegacyComponentSerializer.legacyAmpersand();

    private final RPGAtributos plugin;
    private final NamespacedKey chaveTag;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();

    public TagManager(RPGAtributos plugin) {
        this.plugin = plugin;
        this.chaveTag = new NamespacedKey(plugin, "tag");
    }

    private Settings cfg() { return plugin.settings(); }

    // ---------- dados ----------

    public String getTag(Player p) {
        return p.getPersistentDataContainer().get(chaveTag, PersistentDataType.STRING);
    }

    public void setTag(Player p, String tag) {
        if (tag == null) p.getPersistentDataContainer().remove(chaveTag);
        else p.getPersistentDataContainer().set(chaveTag, PersistentDataType.STRING, tag);
        atualizarTexto(p);
        garantir(p);
    }

    /** Remove códigos de cor e devolve só o texto visível (para limitar o tamanho). */
    public static String textoVisivel(String tag) {
        return PlainTextComponentSerializer.plainText().serialize(CORES.deserialize(tag));
    }

    /**
     * Valida e aplica uma tag digitada pelo jogador (comando ou menu).
     * @return null se deu certo, ou a mensagem de erro.
     */
    public String aplicarTagDigitada(Player p, String texto) {
        texto = texto.trim();
        if (!p.hasPermission("rpg.tag.cores")) texto = textoVisivel(texto);
        String visivel = textoVisivel(texto);
        int max = cfg().tagTamanhoMax;
        if (visivel.isBlank()) return "A tag não pode ficar vazia.";
        if (visivel.length() > max) return "Tag muito grande (máx. " + max + " letras).";
        setTag(p, texto);
        return null;
    }

    private boolean usaTag(Player p) {
        return getTag(p) != null || cfg().tagMostrarNivel || plugin.titulos().ativo(p) != null || plugin.classes().ativa(p) != null
                || plugin.reinos().de(p) != null;
    }

    private Component montarTexto(Player p) {
        String tag = getTag(p);
        Component nome = tag != null ? CORES.deserialize(tag) : Component.text(p.getName());
        Component titulo = plugin.titulos().linhaDaTag(p);
        if (titulo != null) nome = titulo.append(Component.newline()).append(nome);
        Component reino = plugin.reinos().linhaDaTag(p);
        if (reino != null) nome = reino.append(Component.newline()).append(nome);
        Component classe = plugin.classes().linhaDaTag(p);
        if (!cfg().tagMostrarNivel) return classe == null ? nome : nome.append(Component.newline()).append(classe);
        Component nivel = Component.text("Nível total " + plugin.stats().nivelTotal(p), NamedTextColor.GRAY);
        return nome.append(Component.newline())
                .append(classe == null ? nivel : classe.append(Component.text(" • ", NamedTextColor.DARK_GRAY)).append(nivel));
    }

    // ---------- time que esconde o nome normal ----------

    private Team time() {
        Scoreboard sb = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team t = sb.getTeam(NOME_TIME);
        if (t == null) {
            t = sb.registerNewTeam(NOME_TIME);
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return t;
    }

    // ---------- entidade ----------

    private boolean deveMostrar(Player p) {
        return p.isOnline() && !p.isDead()
                && p.getGameMode() != GameMode.SPECTATOR
                && !p.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    private Location posicao(Player p) {
        return p.getLocation().add(0, p.getHeight() + cfg().tagAltura, 0);
    }

    /** Cria/remove o display conforme necessário. */
    public void garantir(Player p) {
        if (!usaTag(p)) {
            remover(p, true);
            return;
        }
        time().addEntry(p.getName());

        TextDisplay d = displays.get(p.getUniqueId());
        if (!deveMostrar(p)) {
            if (d != null) { d.remove(); displays.remove(p.getUniqueId()); }
            return;
        }
        if (d != null && d.isValid() && d.getWorld().equals(p.getWorld())) return;
        if (d != null) d.remove();

        TextDisplay novo = p.getWorld().spawn(posicao(p), TextDisplay.class, td -> {
            td.text(montarTexto(p));
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setShadowed(true);
            td.setBackgroundColor(Color.fromARGB(64, 0, 0, 0));
            td.setTeleportDuration(1); // movimento suave ao seguir o jogador
        });
        p.hideEntity(plugin, novo); // o próprio jogador não vê a tag (não atrapalha a visão)
        displays.put(p.getUniqueId(), novo);
    }

    public void atualizarTexto(Player p) {
        TextDisplay d = displays.get(p.getUniqueId());
        if (d != null && d.isValid()) d.text(montarTexto(p));
    }

    public void remover(Player p, boolean devolverNome) {
        TextDisplay d = displays.remove(p.getUniqueId());
        if (d != null) d.remove();
        if (devolverNome) {
            Team t = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam(NOME_TIME);
            if (t != null) t.removeEntry(p.getName());
        }
    }

    public void removerTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) remover(p, true);
        displays.values().forEach(TextDisplay::remove);
        displays.clear();
    }

    /** Roda todo tick: faz a tag seguir o jogador. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!usaTag(p)) continue;
            TextDisplay d = displays.get(p.getUniqueId());
            if (d == null || !d.isValid() || !d.getWorld().equals(p.getWorld()) || !deveMostrar(p)) {
                garantir(p);
                continue;
            }
            d.teleport(posicao(p));
        }
    }

    // ---------- eventos ----------

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        garantir(e.getPlayer());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        remover(e.getPlayer(), false);
    }
}
