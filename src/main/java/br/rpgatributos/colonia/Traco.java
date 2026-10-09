package br.rpgatributos.colonia;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O jeito de cada morador: 1 ou 2 traços sorteados quando ele chega. Mexem na produção,
 * na fome, no humor da colônia, no XP e na força como soldado. Os nomes são substantivos
 * para valer para qualquer morador ("Coragem", não "Corajoso/Corajosa").
 */
public enum Traco {
    DEDICACAO("Dedicação", true, "Produz 15% a mais."),
    PREGUICA("Preguiça", false, "Produz 15% a menos."),
    APETITE("Apetite grande", false, "Come quase o dobro, mas pratos da Cozinha o deixam radiante."),
    FRUGALIDADE("Come pouco", true, "Come pouco mais da metade."),
    BOM_HUMOR("Bom humor", true, "Deixa a colônia mais feliz."),
    MAU_HUMOR("Mau humor", false, "Deixa a colônia menos feliz."),
    CORAGEM("Coragem", true, "Como soldado: +20% de vida e dano."),
    MEDO("Medo", false, "Como soldado: -15% de vida e dano."),
    MADRUGADOR("Acorda cedo", true, "Trabalha também no começo e no fim da noite."),
    CURIOSIDADE("Curiosidade", true, "Ganha 50% a mais de XP."),
    SORTE("Sorte", true, "Às vezes rende um item a mais.");

    private final String nome;
    private final boolean bom;
    private final String descricao;

    Traco(String nome, boolean bom, String descricao) {
        this.nome = nome;
        this.bom = bom;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public boolean bom() { return bom; }
    public String descricao() { return descricao; }
    public TextColor cor() { return bom ? NamedTextColor.AQUA : NamedTextColor.LIGHT_PURPLE; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** O traço que não combina com este (não aparecem juntos). */
    private Traco oposto() {
        return switch (this) {
            case DEDICACAO -> PREGUICA;
            case PREGUICA -> DEDICACAO;
            case APETITE -> FRUGALIDADE;
            case FRUGALIDADE -> APETITE;
            case BOM_HUMOR -> MAU_HUMOR;
            case MAU_HUMOR -> BOM_HUMOR;
            case CORAGEM -> MEDO;
            case MEDO -> CORAGEM;
            default -> null;
        };
    }

    /** 1 traço, ou 2 (metade das vezes) que não se contradizem. */
    public static List<Traco> sortear() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Traco[] todos = values();
        List<Traco> l = new ArrayList<>();
        l.add(todos[r.nextInt(todos.length)]);
        if (r.nextBoolean()) {
            for (int i = 0; i < 10; i++) {
                Traco t = todos[r.nextInt(todos.length)];
                if (t != l.get(0) && t != l.get(0).oposto()) { l.add(t); break; }
            }
        }
        return l;
    }

    public static Traco porId(String id) {
        for (Traco t : values()) if (t.id().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) return t;
        return null;
    }
}
