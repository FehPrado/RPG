package br.rpgatributos.perigo;

import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/** Modificadores dos Monstros de Elite. Um Elite tem 1, um Campeão 2 e um Senhor 3. */
public enum Afixo {
    VELOZ("Veloz", 0x55FFFF, "40% mais rápido"),
    BLINDADO("Blindado", 0xAAAAAA, "muita armadura e não é empurrado"),
    VAMPIRO("Vampiro", 0xAA0000, "se cura com o dano que causa"),
    EXPLOSIVO("Explosivo", 0xFF5500, "explode ao morrer (sem quebrar blocos)"),
    INVOCADOR("Invocador", 0x9A86B8, "chama ajudantes durante a luta"),
    GIGANTE("Gigante", 0x8B5A2B, "enorme: muita vida e dano, mas mais lento"),
    FANTASMA("Fantasma", 0xDDDDFF, "fica invisível e teleporta perto do alvo"),
    VENENOSO("Venenoso", 0x5BD15B, "golpes envenenam e definham"),
    GELIDO("Gélido", 0x9FE7FF, "golpes congelam e deixam lento"),
    INCENDIARIO("Incendiário", 0xFF6A2B, "golpes incendeiam; não pega fogo"),
    ANTIMAGIA("Antimagia", 0xC77DFF, "magias causam 70% menos dano nele"),
    REGENERANTE("Regenerante", 0xFF5C8A, "recupera vida sem parar"),
    ENFURECIDO("Enfurecido", 0xFF2222, "com pouca vida fica muito mais forte");

    private final String nome;
    private final TextColor cor;
    private final String descricao;

    Afixo(String nome, int cor, String descricao) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Afixo porNome(String n) {
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
