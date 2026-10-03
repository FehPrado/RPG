package br.rpgatributos.forja;

import org.bukkit.Material;

import java.util.Locale;

/** Material do item. Itens de material melhor ganham bônus maiores. */
public enum Tier {
    MADEIRA("WOODEN_", "Madeira", 0.6),
    PEDRA("STONE_", "Pedra", 0.75),
    OURO("GOLDEN_", "Ouro", 0.7),
    COBRE("COPPER_", "Cobre", 0.85),
    COURO("LEATHER_", "Couro", 0.6),
    MALHA("CHAINMAIL_", "Malha", 0.9),
    FERRO("IRON_", "Ferro", 1.0),
    DIAMANTE("DIAMOND_", "Diamante", 1.3),
    NETHERITE("NETHERITE_", "Netherite", 1.6),
    /** Arco, besta, escudo, maça, casco de tartaruga. */
    ESPECIAL("", "Especial", 1.15);

    private final String prefixo;
    private final String nome;
    private final double multiplicador;

    Tier(String prefixo, String nome, double multiplicador) {
        this.prefixo = prefixo;
        this.nome = nome;
        this.multiplicador = multiplicador;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String nome() { return nome; }
    public double multiplicador() { return multiplicador; }

    public static Tier de(Material m) {
        String n = m.name();
        for (Tier t : values()) {
            if (!t.prefixo.isEmpty() && n.startsWith(t.prefixo)) return t;
        }
        return ESPECIAL;
    }
}
