package br.rpgatributos.reino;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/** Cargos do reino, do mais alto ao mais baixo. */
public enum Cargo {
    REI("Rei", "♛", NamedTextColor.GOLD, "Manda em tudo: guerra, paz, cargos e a coroa."),
    NOBRE("Nobre", "♜", NamedTextColor.LIGHT_PURPLE, "Convida, expulsa cidadãos e cavaleiros e mexe no tesouro."),
    CAVALEIRO("Cavaleiro", "⚔", NamedTextColor.RED, "Luta pelo reino. Pode convidar."),
    CIDADAO("Cidadão", "•", NamedTextColor.GRAY, "Membro do reino.");

    private final String nome;
    private final String icone;
    private final TextColor cor;
    private final String descricao;

    Cargo(String nome, String icone, TextColor cor, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.cor = cor;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String icone() { return icone; }
    public TextColor cor() { return cor; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Pode administrar (tesouro, expulsar quem está abaixo)? */
    public boolean administra() { return this == REI || this == NOBRE; }

    public boolean podeConvidar() { return this != CIDADAO; }

    /** Está acima do outro cargo? */
    public boolean acimaDe(Cargo outro) { return ordinal() < outro.ordinal(); }

    public static Cargo porId(String id) {
        for (Cargo c : values()) if (c.id().equalsIgnoreCase(id) || c.nome.equalsIgnoreCase(id)) return c;
        return null;
    }
}
