package br.rpgatributos.masmorra;

import java.util.Locale;

/**
 * Tipos de sala. O tamanho é fixo (com as paredes): salas personalizadas precisam
 * ter exatamente esse tamanho, e o plugin abre as portas no meio de cada parede.
 */
public enum TipoSala {
    INICIO("Entrada", 19, 9),
    COMBATE("Combate", 19, 9),
    TESOURO("Tesouro", 19, 9),
    CHEFE("Chefe", 27, 12);

    private final String nome;
    private final int tamanho;
    private final int altura;

    TipoSala(String nome, int tamanho, int altura) {
        this.nome = nome;
        this.tamanho = tamanho;
        this.altura = altura;
    }

    public String nome() { return nome; }
    /** Largura e comprimento (x e z), com as paredes. Sempre ímpar, para ter um bloco central. */
    public int tamanho() { return tamanho; }
    /** Altura, com o piso e o teto. */
    public int altura() { return altura; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static TipoSala porId(String id) {
        for (TipoSala t : values()) if (t.id().equalsIgnoreCase(id)) return t;
        return null;
    }
}
