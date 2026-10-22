package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Estatísticas dos portais (2.70): por jogador (no próprio jogador) e os recordes do servidor
 * (os 5 melhores tempos de cada rank, em recordes_portais.yml).
 */
public final class Estatisticas {

    /** As notas, da pior para a melhor. */
    public enum Nota {
        D(NamedTextColor.GRAY, 0), C(NamedTextColor.WHITE, 0), B(NamedTextColor.GREEN, 1), A(NamedTextColor.AQUA, 2), S(NamedTextColor.GOLD, 4);

        public final NamedTextColor cor;
        /** Sorteios a mais no baú final. */
        public final int tesouro;

        Nota(NamedTextColor cor, int tesouro) {
            this.cor = cor;
            this.tesouro = tesouro;
        }
    }

    public record Recorde(String nome, int segundos, String nota) { }

    private final RPGAtributos plugin;
    private final File arquivo;

    public Estatisticas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "recordes_portais.yml");
    }

    private static NamespacedKey k(String s) {
        return new NamespacedKey("rpgatributos", "mstat_" + s);
    }

    private static int ler(Player p, String s) {
        return p.getPersistentDataContainer().getOrDefault(k(s), PersistentDataType.INTEGER, 0);
    }

    private static void somar(Player p, String s, int n) {
        p.getPersistentDataContainer().set(k(s), PersistentDataType.INTEGER, ler(p, s) + n);
    }

    public static void morreu(Player p) { somar(p, "mortes", 1); }
    public static void segredo(Player p) { somar(p, "segredos", 1); }
    public static void reliquia(Player p) { somar(p, "reliquias", 1); }
    public static void mimico(Player p) { somar(p, "mimicos", 1); }

    /** A nota da corrida (com a pontuação, para mostrar). */
    public static Nota nota(int pontos) {
        return pontos >= 125 ? Nota.S : pontos >= 105 ? Nota.A : pontos >= 85 ? Nota.B : pontos >= 65 ? Nota.C : Nota.D;
    }

    /** Registra um portal vencido para o jogador e o recorde do servidor. */
    public void venceu(Player p, Rank r, int segundos, Nota n) {
        somar(p, "vencidos_" + r.id(), 1);
        if (n.ordinal() > ler(p, "melhor_nota") || !p.getPersistentDataContainer().has(k("melhor_nota"))) {
            p.getPersistentDataContainer().set(k("melhor_nota"), PersistentDataType.INTEGER, Math.max(n.ordinal(), ler(p, "melhor_nota")));
        }
        int antes = ler(p, "tempo_" + r.id());
        if (antes == 0 || segundos < antes) p.getPersistentDataContainer().set(k("tempo_" + r.id()), PersistentDataType.INTEGER, segundos);
        recorde(p.getName(), r, segundos, n);
    }

    private void recorde(String nome, Rank r, int segundos, Nota n) {
        YamlConfiguration y = arquivo.exists() ? YamlConfiguration.loadConfiguration(arquivo) : new YamlConfiguration();
        List<Recorde> l = recordes(y, r);
        l.add(new Recorde(nome, segundos, n.name()));
        l.sort(Comparator.comparingInt(Recorde::segundos));
        while (l.size() > 5) l.removeLast();
        List<String> linhas = new ArrayList<>();
        for (Recorde x : l) linhas.add(x.nome() + ";" + x.segundos() + ";" + x.nota());
        y.set("recordes." + r.id(), linhas);
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar recordes_portais.yml: " + ex.getMessage());
        }
    }

    private static List<Recorde> recordes(YamlConfiguration y, Rank r) {
        List<Recorde> l = new ArrayList<>();
        for (String s : y.getStringList("recordes." + r.id())) {
            String[] p = s.split(";");
            if (p.length != 3) continue;
            try { l.add(new Recorde(p[0], Integer.parseInt(p[1]), p[2])); } catch (NumberFormatException ignorado) { }
        }
        return l;
    }

    public List<Recorde> recordes(Rank r) {
        return arquivo.exists() ? recordes(YamlConfiguration.loadConfiguration(arquivo), r) : new ArrayList<>();
    }

    static String tempo(int s) {
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** O /masmorra stats. */
    public void mostrar(Player p) {
        Cacador.mostrar(p);
        StringBuilder b = new StringBuilder();
        int total = 0;
        for (Rank r : Rank.values()) {
            int n = ler(p, "vencidos_" + r.id());
            total += n;
            if (!b.isEmpty()) b.append("  ");
            b.append(r.name()).append(": ").append(n);
        }
        p.sendMessage(Component.text("   Portais vencidos: " + total + "  (" + b + ")", NamedTextColor.WHITE));
        boolean temNota = p.getPersistentDataContainer().has(k("melhor_nota"));
        Nota melhor = Nota.values()[Math.min(Nota.values().length - 1, ler(p, "melhor_nota"))];
        p.sendMessage(Component.text("   Melhor nota: ", NamedTextColor.GRAY).append(temNota ? Component.text(melhor.name(), melhor.cor, TextDecoration.BOLD) : Component.text("-", NamedTextColor.DARK_GRAY)));
        StringBuilder t = new StringBuilder();
        for (Rank r : Rank.values()) {
            int s = ler(p, "tempo_" + r.id());
            if (s <= 0) continue;
            if (!t.isEmpty()) t.append("  ");
            t.append(r.name()).append(" ").append(tempo(s));
        }
        p.sendMessage(Component.text("   Melhores tempos: " + (t.isEmpty() ? "-" : t), NamedTextColor.GRAY));
        p.sendMessage(Component.text("   Segredos achados: " + ler(p, "segredos") + "  •  Relíquias: " + ler(p, "reliquias")
                + "  •  Mímicos vencidos: " + ler(p, "mimicos") + "  •  Mortes em portais: " + ler(p, "mortes"), NamedTextColor.GRAY));
    }

    /** O /masmorra recordes [rank]. */
    public void mostrarRecordes(Player p, Rank so) {
        p.sendMessage(Component.text("⏱ Recordes dos portais (os mais rápidos)", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Rank r : Rank.values()) {
            if (so != null && r != so) continue;
            List<Recorde> l = recordes(r);
            if (l.isEmpty()) {
                if (so != null) p.sendMessage(Component.text("   " + r.nome() + ": ninguém ainda.", NamedTextColor.DARK_GRAY));
                continue;
            }
            p.sendMessage(Component.text("   " + r.nome(), r.cor(), TextDecoration.BOLD));
            int i = 1;
            for (Recorde x : l) {
                p.sendMessage(Component.text("     " + i++ + ". " + x.nome() + " — " + tempo(x.segundos()) + " (nota " + x.nota() + ")", NamedTextColor.GRAY));
            }
        }
    }
}
