package br.rpgatributos.forja;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Grau do item forjado, do mais comum ao mais raro (inspirado em Overgeared).
 * Pesos e multiplicadores daqui são só o padrão: o config.yml pode mudar.
 */
public enum Raridade {
    //        id          nome        cor       ícone no menu               status efeitos peso0 pesoMax mult  fatorChance
    COMUM   ("comum",    "Comum",    0xD8D8D8, Material.IRON_INGOT,        1, 0, 70, 25,  1.0, 1.0),
    RARO    ("raro",     "Raro",     0x55C8FF, Material.DIAMOND,           2, 0, 24, 35,  1.3, 1.0),
    EPICO   ("epico",    "Épico",    0xB66BFF, Material.AMETHYST_SHARD,    3, 1,  5, 22,  1.7, 1.0),
    UNICO   ("unico",    "Único",    0xFF9A3C, Material.BLAZE_POWDER,      3, 1,  1, 11,  2.1, 1.2),
    LENDARIO("lendario", "Lendário", 0xFFD23F, Material.TOTEM_OF_UNDYING,  4, 2,  0,  5,  2.6, 1.45),
    MITICO  ("mitico",   "Mítico",   0xFF4058, Material.NETHER_STAR,       5, 3,  0,  1.5, 3.3, 1.8);

    private final String id;
    private final String nome;
    private final TextColor cor;
    private final Material icone;
    private final int qtdStatus;
    private final int qtdEfeitos;
    private final double pesoInicial;
    private final double pesoFinal;
    private final double multiplicador;
    private final double fatorChance;

    Raridade(String id, String nome, int cor, Material icone, int qtdStatus, int qtdEfeitos,
             double pesoInicial, double pesoFinal, double multiplicador, double fatorChance) {
        this.id = id;
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.icone = icone;
        this.qtdStatus = qtdStatus;
        this.qtdEfeitos = qtdEfeitos;
        this.pesoInicial = pesoInicial;
        this.pesoFinal = pesoFinal;
        this.multiplicador = multiplicador;
        this.fatorChance = fatorChance;
    }

    public String id() { return id; }
    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public Material icone() { return icone; }
    /** Quantos bônus de forja o item ganha. */
    public int qtdStatus() { return qtdStatus; }
    /** Quantos efeitos especiais o item ganha. */
    public int qtdEfeitos() { return qtdEfeitos; }
    public double pesoInicialPadrao() { return pesoInicial; }
    public double pesoFinalPadrao() { return pesoFinal; }
    public double multiplicadorPadrao() { return multiplicador; }
    /** Multiplica a chance dos efeitos "ao acertar", "ao ser atingido" etc. */
    public double fatorChance() { return fatorChance; }

    public boolean peloMenos(Raridade outra) {
        return ordinal() >= outra.ordinal();
    }

    /** ★★★☆☆☆ */
    public String estrelas() {
        return "★".repeat(ordinal() + 1) + "☆".repeat(values().length - 1 - ordinal());
    }

    /** Aceita "epico", "épico", "EPICO"... */
    public static Raridade porId(String texto) {
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        for (Raridade r : values()) {
            if (r.id.equals(t)) return r;
        }
        return null;
    }
}
