package br.rpgatributos;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Guarda e calcula XP/nível de cada atributo.
 * Os dados ficam salvos dentro do próprio jogador (PersistentDataContainer),
 * então não precisa de banco de dados nem arquivo extra.
 */
public final class StatsManager {

    private final RPGAtributos plugin;
    private final Map<Skill, NamespacedKey> chaves = new EnumMap<>(Skill.class);

    public StatsManager(RPGAtributos plugin) {
        this.plugin = plugin;
        for (Skill s : Skill.values()) {
            chaves.put(s, new NamespacedKey(plugin, "xp_" + s.id()));
        }
    }

    private Settings cfg() { return plugin.settings(); }

    // ---------- leitura / escrita ----------

    public double getXp(Player p, Skill s) {
        Double v = p.getPersistentDataContainer().get(chaves.get(s), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }

    public void setXp(Player p, Skill s, double xp) {
        double limite = xpTotalParaNivel(cfg().nivelMaximo);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(chaves.get(s), PersistentDataType.DOUBLE, Math.max(0, Math.min(xp, limite)));
    }

    public int getNivel(Player p, Skill s) {
        return nivelParaXp(getXp(p, s));
    }

    public int nivelTotal(Player p) {
        int total = 0;
        for (Skill s : Skill.values()) total += getNivel(p, s);
        return total;
    }

    // ---------- fórmulas ----------

    /** XP necessário para ir do nível {@code nivel} para o próximo. */
    public double xpParaProximo(int nivel) {
        return cfg().xpBase * Math.pow(nivel + 1, cfg().xpExpoente);
    }

    /** XP acumulado necessário para chegar no nível {@code nivel}. */
    public double xpTotalParaNivel(int nivel) {
        double total = 0;
        for (int i = 0; i < nivel; i++) total += xpParaProximo(i);
        return total;
    }

    public int nivelParaXp(double xp) {
        int nivel = 0;
        double acumulado = 0;
        while (nivel < cfg().nivelMaximo) {
            acumulado += xpParaProximo(nivel);
            if (xp < acumulado) break;
            nivel++;
        }
        return nivel;
    }

    /** Progresso (0 a 1) dentro do nível atual. */
    public double progresso(Player p, Skill s) {
        double xp = getXp(p, s);
        int nivel = nivelParaXp(xp);
        if (nivel >= cfg().nivelMaximo) return 1;
        return (xp - xpTotalParaNivel(nivel)) / xpParaProximo(nivel);
    }

    // ---------- ganhar XP ----------

    public void darXp(Player p, Skill s, double quantidade) {
        if (quantidade <= 0) return;
        // O XP dos atributos da classe também sobe o nível da classe (mesmo com o atributo no máximo).
        plugin.classes().aoGanharXp(p, s, quantidade);
        plugin.deuses().aoGanharXp(p, s, quantidade);
        int antes = getNivel(p, s);
        if (antes >= cfg().nivelMaximo) return;

        double ganho = quantidade * cfg().multiplicadorXp * plugin.titulos().multiplicadorXp(p, s) * plugin.acessorios().multiplicadorXp(p)
                * plugin.estacoes().multiplicadorXp(s) * plugin.astronomia().multiplicadorXp(p, s) * plugin.enciclopedia().multiplicadorXp(p)
                * plugin.talentos().multiplicadorXp(p) * plugin.fogueiras().multiplicadorXp(p) * plugin.trofeus().multiplicadorXp(p);
        setXp(p, s, getXp(p, s) + ganho);
        int depois = getNivel(p, s);

        if (cfg().actionBar) {
            double xp = getXp(p, s);
            double dentro = xp - xpTotalParaNivel(depois);
            Component barra = Component.text("+" + fmt(ganho) + " XP ", NamedTextColor.YELLOW)
                    .append(Component.text(s.icone() + " " + s.nome(), s.cor()))
                    .append(Component.text("  Nv " + depois, NamedTextColor.WHITE))
                    .append(depois >= cfg().nivelMaximo
                            ? Component.text("  (MÁX)", NamedTextColor.GOLD)
                            : Component.text("  " + fmt(dentro) + "/" + fmt(xpParaProximo(depois)), NamedTextColor.GRAY));
            p.sendActionBar(barra);
        }

        if (depois > antes) subiuDeNivel(p, s, depois);
    }

    private void subiuDeNivel(Player p, Skill s, int nivel) {
        p.showTitle(Title.title(
                Component.text("Nível " + nivel + "!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text(s.icone() + " " + s.nome(), s.cor()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        p.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(s.nome(), s.cor()))
                .append(Component.text(" subiu para o nível " + nivel + "! ", NamedTextColor.YELLOW))
                .append(Component.text(plugin.bonus().descricao(s, nivel), NamedTextColor.GRAY)));

        plugin.bonus().aplicar(p);
        plugin.tags().atualizarTexto(p);
        plugin.titulos().verificar(p); // títulos por nível
        plugin.talentos().aoSubirNivel(p);
        int total = nivelTotal(p);
        plugin.recordes().nivel(p, total + plugin.renascimento().vezes(p) * cfg().nivelMaximo * Skill.values().length);
        if (total % 100 == 0) plugin.diario().marco(p, "nivel_" + total, "Chegou ao nível total " + total);
        if (nivel >= cfg().nivelMaximo) plugin.diario().marco(p, s.id() + "_maximo", "Levou " + s.nome() + " ao nível máximo");
    }

    /** Número com até 2 casas e vírgula: 2 → "2", 2.5 → "2,5", 0.125 → "0,13". */
    public static String fmt(double v) {
        double r = Math.round(v * 100) / 100.0;
        if (r == Math.rint(r)) return String.valueOf((long) r);
        String s = String.format(Locale.ROOT, "%.2f", r);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.replace('.', ',');
    }
}
