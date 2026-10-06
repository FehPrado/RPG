package br.rpgatributos.combo;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Posturas de combate. Valem enquanto o jogador segura uma arma de combo. A Mestra é a da arma
 * na mão e só existe com a proficiência dela no máximo; sem isso, conta como Neutra.
 *
 * {@code dano}: multiplicador do dano causado; {@code recebido}: do dano recebido;
 * {@code velocidade}: velocidade de andar a mais (fração); {@code equilibrio}: quanto os golpes
 * desequilibram o inimigo; {@code vigor}: custo de vigor dos golpes; {@code recarga}: recarga dos golpes.
 */
public enum Postura {
    NEUTRA("Neutra", "◇", 0xBDBDBD, Material.STICK, 1.0, 1.0, 0, 1.0, 1.0, 1.0,
            "Sem bônus e sem perdas."),
    OFENSIVA("Ofensiva", "▲", 0xE53935, Material.REDSTONE, 1.15, 1.10, 0, 1.25, 1.10, 1.0,
            "+15% de dano e desequilibra mais, mas você leva +10% de dano e os golpes custam +10% de vigor."),
    DEFENSIVA("Defensiva", "■", 0x42A5F5, Material.SHIELD, 0.90, 0.80, -0.05, 0.8, 1.0, 1.0,
            "-20% de dano recebido, resiste a empurrões e cada golpe de combo cura ½ ❤. -10% de dano e um pouco mais lento."),
    AGIL("Ágil", "≈", 0x66BB6A, Material.FEATHER, 0.92, 1.0, 0.10, 1.0, 1.0, 0.80,
            "+10% de velocidade, golpes recarregam 20% mais rápido e a esquiva custa metade do vigor. -8% de dano."),
    MESTRA("Mestra", "✦", 0xFFD54F, Material.NETHER_STAR, 1.0, 1.0, 0, 1.0, 1.0, 1.0,
            "A postura própria da arma na mão. Libera na proficiência 20 dela.");

    private final String nome, simbolo, descricao;
    private final TextColor cor;
    private final Material icone;
    private final double dano, recebido, velocidade, equilibrio, vigor, recarga;

    Postura(String nome, String simbolo, int cor, Material icone, double dano, double recebido, double velocidade,
            double equilibrio, double vigor, double recarga, String descricao) {
        this.nome = nome;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.icone = icone;
        this.dano = dano;
        this.recebido = recebido;
        this.velocidade = velocidade;
        this.equilibrio = equilibrio;
        this.vigor = vigor;
        this.recarga = recarga;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String simbolo() { return simbolo; }
    public String descricao() { return descricao; }
    public TextColor cor() { return cor; }
    public Color corParticula() { return Color.fromRGB(cor.value()); }
    public Material icone() { return icone; }
    public double dano() { return dano; }
    public double recebido() { return recebido; }
    public double velocidade() { return velocidade; }
    public double equilibrio() { return equilibrio; }
    public double vigor() { return vigor; }
    public double recarga() { return recarga; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Postura porId(String s) {
        for (Postura p : values()) if (p.id().equalsIgnoreCase(s) || p.nome.equalsIgnoreCase(s)) return p;
        return null;
    }

    // =====================================================================
    //  As posturas mestras de cada arma
    // =====================================================================

    /** Nome da postura mestra da arma. */
    public static String nomeMestra(TipoArma t) {
        return switch (t) {
            case ESPADA -> "Duelista";
            case MACHADO -> "Carrasco";
            case LANCA -> "Falange";
            case TRIDENTE -> "Maré";
            case MACA -> "Colosso";
            case ARCO -> "Atirador Paciente";
        };
    }

    public static String descricaoMestra(TipoArma t) {
        return switch (t) {
            case ESPADA -> "+15% de dano e o golpe no tempo certo fica mais fácil (janela maior).";
            case MACHADO -> "+40% de dano contra quem tem menos de 35% da vida.";
            case LANCA -> "+10% de dano e -25% de dano recebido de quem está na sua frente.";
            case TRIDENTE -> "Na água ou na chuva: +30% de dano e +20% de velocidade.";
            case MACA -> "+10% de dano, desequilibra o dobro e resiste a empurrões. Um pouco mais lento.";
            case ARCO -> "Parado há 1 segundo: +35% de dano das flechas e dos golpes de arco.";
        };
    }

    /** Id da pegada da Mestra no pacote (só espada, machado e maça mudam a pegada). */
    public static String pegadaMestra(TipoArma t) {
        return switch (t) {
            case ESPADA -> "duelista";
            case MACHADO -> "carrasco";
            case MACA -> "colosso";
            default -> null;
        };
    }
}
