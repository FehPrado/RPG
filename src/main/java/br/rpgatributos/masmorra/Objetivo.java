package br.rpgatributos.masmorra;

import java.util.Locale;

/** O que a party precisa fazer na masmorra de um Cristal de Portal (2.68). */
public enum Objetivo {
    CACADA("Caçada", "Derrote o chefe."),
    SAQUE("Saque", "Ache as relíquias (vasos dourados) espalhadas pelas salas."),
    MONOLITOS("Monólitos", "Desperte os monólitos (magnetitas) e vença o que sair de cada um.");

    private final String nome, descricao;

    Objetivo(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Objetivo porId(String s) {
        if (s == null) return null;
        for (Objetivo o : values()) if (o.name().equalsIgnoreCase(s)) return o;
        return null;
    }
}
