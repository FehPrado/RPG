package br.rpgatributos.combo;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Tipos de arma com combos. As armas corpo a corpo começam a sequência com o clique
 * direito (o esquerdo continua sendo o ataque normal); arco e besta começam com o
 * esquerdo, porque o direito puxa a flecha.
 */
public enum TipoArma {
    ESPADA("Espada", "⚔", 0xE0E0E0, Material.IRON_SWORD, 'D'),
    MACHADO("Machado", "✠", 0xC0874A, Material.IRON_AXE, 'D'),
    LANCA("Lança", "↟", 0x9FB8C8, Material.IRON_SPEAR, 'D'),
    TRIDENTE("Tridente", "♆", 0x4FC3F7, Material.TRIDENT, 'D'),
    MACA("Maça", "⚒", 0xB0A090, Material.MACE, 'D'),
    ARCO("Arco e Besta", "➹", 0x8BC34A, Material.BOW, 'E');

    private final String nome, simbolo;
    private final TextColor cor;
    private final Material icone;
    private final char inicio;

    TipoArma(String nome, String simbolo, int cor, Material icone, char inicio) {
        this.nome = nome;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.icone = icone;
        this.inicio = inicio;
    }

    public String nome() { return nome; }
    public String simbolo() { return simbolo; }
    public TextColor cor() { return cor; }
    public Material icone() { return icone; }
    /** Primeiro clique de toda sequência: 'D' (direito) ou 'E' (esquerdo). */
    public char inicio() { return inicio; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** As 4 sequências, na ordem dos espaços de combo. */
    public String[] sequencias() {
        return inicio == 'D' ? new String[]{"DDD", "DED", "DDE", "DEE"} : new String[]{"EEE", "EDE", "EED", "EDD"};
    }

    /** "DED" → "D · E · D" */
    public static String bonita(String seq) {
        return String.join(" · ", seq.split(""));
    }

    public static TipoArma de(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        Material m = item.getType();
        if (m == Material.TRIDENT) return TRIDENTE;
        if (m == Material.MACE) return MACA;
        if (m == Material.BOW || m == Material.CROSSBOW) return ARCO;
        String n = m.name();
        if (n.endsWith("_SWORD")) return ESPADA;
        if (n.endsWith("_SPEAR")) return LANCA;
        if (n.endsWith("_AXE") && !n.endsWith("_PICKAXE")) return MACHADO;
        return null;
    }

    public static TipoArma porId(String s) {
        for (TipoArma t : values()) if (t.id().equalsIgnoreCase(s) || t.name().equalsIgnoreCase(s)) return t;
        return null;
    }
}
