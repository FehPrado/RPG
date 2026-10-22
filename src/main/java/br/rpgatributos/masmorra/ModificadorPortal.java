package br.rpgatributos.masmorra;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Modificadores do Cristal de Portal (2.68): mais risco, mais tesouro. Os ruins aumentam o tesouro;
 * os bons ajudam, mas tiram um pouco dele.
 */
public enum ModificadorPortal {
    FURIA("Fúria", "Monstros causam +30% de dano.", false, 0.25),
    COURACA("Couraça", "Monstros com +40% de vida.", false, 0.25),
    RELOGIO_CURTO("Relógio Curto", "4 minutos a menos.", false, 0.35),
    SEM_CURA("Sem Cura", "Ninguém se cura com a fome cheia.", false, 0.30),
    ONDA_EXTRA("Onda Extra", "Uma onda a mais em cada sala.", false, 0.25),
    TESOURO_FARTO("Tesouro Farto", "Baús mais cheios.", true, 0.50),
    BENCAO("Bênção", "Regeneração I lá dentro.", true, -0.10),
    PRESSA("Pressa", "Velocidade I lá dentro.", true, -0.05);

    private final String nome, descricao;
    private final boolean bom;
    private final double tesouro;

    ModificadorPortal(String nome, String descricao, boolean bom, double tesouro) {
        this.nome = nome;
        this.descricao = descricao;
        this.bom = bom;
        this.tesouro = tesouro;
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public boolean bom() { return bom; }
    /** Quanto muda o tesouro (0,25 = +25%). */
    public double tesouro() { return tesouro; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Multiplicador dos sorteios nos baús com estes modificadores. */
    public static double multTesouro(Set<ModificadorPortal> mods) {
        double m = 1;
        for (ModificadorPortal x : mods) m += x.tesouro;
        return Math.max(0.5, m);
    }

    public static String texto(Set<ModificadorPortal> mods) {
        return String.join(",", mods.stream().map(Enum::name).toList());
    }

    public static EnumSet<ModificadorPortal> ler(String s) {
        EnumSet<ModificadorPortal> r = EnumSet.noneOf(ModificadorPortal.class);
        if (s == null || s.isBlank()) return r;
        for (String x : s.split(",")) {
            try { r.add(valueOf(x.trim())); } catch (IllegalArgumentException ignorado) { }
        }
        return r;
    }
}
