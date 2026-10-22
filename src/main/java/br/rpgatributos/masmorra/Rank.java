package br.rpgatributos.masmorra;

import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/**
 * O rank de um portal (2.68), de E a S, no estilo dos caçadores. Cada rank usa uma das 4
 * dificuldades de sempre (salas, monstros, chefe e o nível total pedido) e ajusta a força e o
 * tesouro em cima dela: E é um Fácil mais leve, S é um Pesadelo mais forte.
 */
public enum Rank {
    E(0x9E9E9E, Dificuldade.FACIL, 0.8, 0, 10, 0.10),
    D(0x4CAF50, Dificuldade.FACIL, 1.15, 1, 20, 0.14),
    C(0x2196F3, Dificuldade.NORMAL, 1.0, 1, 40, 0.18),
    B(0x9C27B0, Dificuldade.DIFICIL, 1.0, 2, 80, 0.22),
    A(0xE53935, Dificuldade.PESADELO, 1.0, 3, 160, 0.26),
    S(0xFFC107, Dificuldade.PESADELO, 1.45, 5, 320, 0.32);

    private final TextColor cor;
    private final Dificuldade dificuldade;
    private final double forca;
    private final int tesouroExtra, xpCacador;
    private final double chanceMana;

    Rank(int cor, Dificuldade dificuldade, double forca, int tesouroExtra, int xpCacador, double chanceMana) {
        this.cor = TextColor.color(cor);
        this.dificuldade = dificuldade;
        this.forca = forca;
        this.tesouroExtra = tesouroExtra;
        this.xpCacador = xpCacador;
        this.chanceMana = chanceMana;
    }

    public TextColor cor() { return cor; }
    public Dificuldade dificuldade() { return dificuldade; }
    /** Multiplicador da vida e do dano dos monstros (em cima da dificuldade). */
    public double forca() { return forca; }
    /** Sorteios a mais nos baús. */
    public int tesouroExtra() { return tesouroExtra; }
    /** XP de caçador por portal vencido. */
    public int xpCacador() { return xpCacador; }
    /** Chance de um monstro deixar um Cristal de Mana. */
    public double chanceMana() { return chanceMana; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** "Rank C". */
    public String nome() { return "Rank " + name(); }

    /** "Rank C · Normal": o rank e o nome antigo, para quem já conhece. */
    public String nomeCompleto() { return nome() + " · " + dificuldade.nome(); }

    /** O rank acima (S fica S). */
    public Rank acima() { return this == S ? S : values()[ordinal() + 1]; }

    /** O rank de uma dificuldade antiga (o meio de cada uma). */
    public static Rank de(Dificuldade d) {
        return switch (d) {
            case FACIL -> D;
            case NORMAL -> C;
            case DIFICIL -> B;
            case PESADELO -> A;
        };
    }

    public static Rank porId(String s) {
        if (s == null) return null;
        for (Rank r : values()) if (r.name().equalsIgnoreCase(s)) return r;
        return null;
    }
}
