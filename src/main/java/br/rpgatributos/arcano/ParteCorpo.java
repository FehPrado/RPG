package br.rpgatributos.arcano;

import br.rpgatributos.StatsManager;

/**
 * Onde o item é infundido. A parte decide o bônus fixo que o item dá;
 * as essências do item decidem as magias que o jogador consegue criar.
 */
public enum ParteCorpo {
    //        nome       libera em (fração do nível máximo de Arcano)
    MENTE("Mente", 0),
    CORACAO("Coração", 0),
    BRACOS("Braços", 0.2),
    PERNAS("Pernas", 0.4),
    SANGUE("Sangue", 0.7);

    private final String nome;
    private final double fracao;

    ParteCorpo(String nome, double fracao) {
        this.nome = nome;
        this.fracao = fracao;
    }

    public String nome() { return nome; }

    /** Nível de Arcano para liberar essa parte (com nível máximo 100: 0, 0, 20, 40, 70). */
    public int nivelNecessario(int nivelMaximo) {
        return (int) Math.ceil(fracao * nivelMaximo);
    }

    public boolean liberada(int nivelArcano, int nivelMaximo) {
        return nivelArcano >= nivelNecessario(nivelMaximo);
    }

    // Bônus por ponto de poder do item infundido
    public static final double MANA_POR_PODER = 8;
    public static final double VIDA_POR_PODER = 1;
    public static final double DANO_POR_PODER = 0.25;
    public static final double VELOCIDADE_POR_PODER = 0.015;
    public static final double REGEN_POR_PODER = 0.4;

    /** Texto do bônus que um item com esse poder dá nessa parte. */
    public String bonus(int poder) {
        return switch (this) {
            case MENTE -> "+" + StatsManager.fmt(poder * MANA_POR_PODER) + " de mana máxima";
            case CORACAO -> "+" + StatsManager.fmt(poder * VIDA_POR_PODER / 2) + " ❤ de vida";
            case BRACOS -> "+" + StatsManager.fmt(poder * DANO_POR_PODER) + " de dano";
            case PERNAS -> "+" + StatsManager.fmt(poder * VELOCIDADE_POR_PODER * 100) + "% de velocidade";
            case SANGUE -> "+" + StatsManager.fmt(poder * REGEN_POR_PODER) + " de mana por segundo";
        };
    }

    /** O que essa parte do corpo melhora (sem número). */
    public String funcao() {
        return switch (this) {
            case MENTE -> "Aumenta a mana máxima";
            case CORACAO -> "Aumenta a vida máxima";
            case BRACOS -> "Aumenta o dano";
            case PERNAS -> "Aumenta a velocidade";
            case SANGUE -> "Acelera a recuperação de mana";
        };
    }

    public static ParteCorpo porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
