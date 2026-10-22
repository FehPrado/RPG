package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;

/**
 * Rank de Caçador (2.68): sobe vencendo portais (XP de caçador pelo rank do portal). Diz até que
 * rank de Cristal de Portal o jogador consegue fazer no altar (o dele e um acima).
 */
public final class Cacador {

    private static final NamespacedKey K_XP = new NamespacedKey("rpgatributos", "cacador_xp");
    /** XP para chegar em cada rank (E, D, C, B, A, S). */
    private static final int[] LIMIARES = {0, 100, 300, 700, 1500, 3000};

    private Cacador() { }

    public static int xp(Player p) {
        return p.getPersistentDataContainer().getOrDefault(K_XP, PersistentDataType.INTEGER, 0);
    }

    public static Rank rank(Player p) {
        int x = xp(p);
        Rank r = Rank.E;
        for (int i = 0; i < LIMIARES.length; i++) if (x >= LIMIARES[i]) r = Rank.values()[i];
        return r;
    }

    /** O rank mais alto de cristal que o altar faz para ele (o dele e um acima). */
    public static Rank maximoCristal(Player p) {
        return rank(p).acima();
    }

    /** Quanto falta para o próximo rank (0 se já é S). */
    public static int falta(Player p) {
        Rank r = rank(p);
        return r == Rank.S ? 0 : LIMIARES[r.ordinal() + 1] - xp(p);
    }

    /** 2.77 (tela do mod): quanto já andou do rank atual até o próximo (0 a 1; 1 no S). */
    public static double progresso(Player p) {
        Rank r = rank(p);
        if (r == Rank.S) return 1;
        int de = LIMIARES[r.ordinal()], ate = LIMIARES[r.ordinal() + 1];
        return Math.max(0, Math.min(1, (xp(p) - de) / (double) (ate - de)));
    }

    public static void darXp(Player p, int quanto) {
        if (quanto <= 0) return;
        quanto = (int) Math.round(quanto * PoderCacador.multXp(p)); // Caçador Nato (Despertar)
        Rank antes = rank(p);
        p.getPersistentDataContainer().set(K_XP, PersistentDataType.INTEGER, xp(p) + quanto);
        Rank depois = rank(p);
        p.sendActionBar(Component.text("[Sistema] +" + quanto + " XP de caçador", NamedTextColor.AQUA));
        if (depois != antes) {
            p.showTitle(Title.title(Component.text("[Sistema]", NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text("Rank de Caçador: " + depois.name(), depois.cor(), TextDecoration.BOLD),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.4f);
            // 2.72/2.73: pontos novos na Árvore de Caçador e, nos ranks D, B e S, uma característica.
            JanelaSistema.mostrar(p, "Rank novo",
                    Component.text("Você agora é um caçador de " + depois.nome() + ".", depois.cor()),
                    Component.text("O altar já faz cristais até " + depois.acima().nome() + ".", NamedTextColor.WHITE),
                    Component.text("Árvore de Caçador: " + ArvoreCacador.livres(p) + " ponto(s) livre(s). /masmorra arvore", NamedTextColor.GRAY));
            // Depois do título do rank, para um não apagar o outro.
            org.bukkit.Bukkit.getScheduler().runTaskLater(org.bukkit.plugin.java.JavaPlugin.getPlugin(br.rpgatributos.RPGAtributos.class), () -> {
                if (p.isOnline()) Despertar.verificar(p);
            }, 80L);
        }
    }

    public static void definirXp(Player p, int xp) {
        p.getPersistentDataContainer().set(K_XP, PersistentDataType.INTEGER, Math.max(0, xp));
    }

    /** As linhas do /masmorra rank. */
    public static void mostrar(Player p) {
        Rank r = rank(p);
        p.sendMessage(Component.text("[Sistema] Caçador ", NamedTextColor.AQUA)
                .append(Component.text(r.nome(), r.cor(), TextDecoration.BOLD))
                .append(Component.text("  (" + xp(p) + " XP" + (r == Rank.S ? ")" : ", faltam " + falta(p) + " para " + r.acima().nome() + ")"), NamedTextColor.GRAY)));
        p.sendMessage(Component.text("   O Altar do Portal faz cristais até " + maximoCristal(p).nome() + ". Suba vencendo portais.", NamedTextColor.GRAY));
        p.sendMessage(Component.text("   Árvore de Caçador: " + ArvoreCacador.livres(p) + " de " + ArvoreCacador.pontos(p) + " ponto(s) livre(s).", NamedTextColor.GRAY));
        var cs = Despertar.de(p);
        if (cs.isEmpty()) {
            p.sendMessage(Component.text("   Ainda não despertou (desperta no Rank D).", NamedTextColor.DARK_GRAY));
        } else {
            Component linha = Component.text("   Características: ", NamedTextColor.GRAY);
            for (int i = 0; i < cs.size(); i++) {
                Despertar.Caracteristica c = cs.get(i);
                linha = linha.append(Component.text((i > 0 ? ", " : "") + c.nome, c.raridade.cor));
            }
            p.sendMessage(linha);
        }
    }
}
