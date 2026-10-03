package br.rpgatributos.fazenda;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Qualidade de colheitas e pratos. Comida de qualidade alta dá buffs mais longos;
 * ingredientes de qualidade alta melhoram o prato.
 */
public enum Qualidade {
    //        nome        estrelas  cor                         peso no nível 0 / máximo   fator do buff
    NORMAL("Normal", "", NamedTextColor.WHITE, 80, 25, 1.0),
    BOA("Boa", "★", NamedTextColor.GREEN, 15, 35, 1.15),
    OTIMA("Ótima", "★★", NamedTextColor.AQUA, 4, 25, 1.3),
    PERFEITA("Perfeita", "★★★", NamedTextColor.GOLD, 1, 15, 1.5);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "qualidade");

    private final String nome;
    private final String estrelas;
    private final TextColor cor;
    private final double pesoInicial;
    private final double pesoFinal;
    private final double fator;

    Qualidade(String nome, String estrelas, TextColor cor, double pesoInicial, double pesoFinal, double fator) {
        this.nome = nome;
        this.estrelas = estrelas;
        this.cor = cor;
        this.pesoInicial = pesoInicial;
        this.pesoFinal = pesoFinal;
        this.fator = fator;
    }

    public String nome() { return nome; }
    public String estrelas() { return estrelas; }
    public TextColor cor() { return cor; }
    /** Multiplica a duração dos buffs dos pratos. */
    public double fator() { return fator; }

    public Qualidade acima() {
        return values()[Math.min(ordinal() + 1, values().length - 1)];
    }

    /** Sorteia a qualidade conforme o nível (fração 0 a 1 do nível máximo). */
    public static Qualidade sortear(double fracaoNivel) {
        double t = Math.max(0, Math.min(1, fracaoNivel));
        double[] pesos = new double[values().length];
        double total = 0;
        for (Qualidade q : values()) {
            pesos[q.ordinal()] = q.pesoInicial + (q.pesoFinal - q.pesoInicial) * t;
            total += pesos[q.ordinal()];
        }
        double x = ThreadLocalRandom.current().nextDouble() * total;
        for (Qualidade q : values()) {
            x -= pesos[q.ordinal()];
            if (x < 0) return q;
        }
        return NORMAL;
    }

    public static Qualidade de(ItemStack item) {
        if (item == null || item.isEmpty()) return NORMAL;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return NORMAL;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }

    /** Marca a qualidade no item (normal não muda nada, para continuar empilhando com o comum). */
    public void aplicar(ItemStack item) {
        if (this == NORMAL || item == null || item.isEmpty()) return;
        item.editMeta(m -> {
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            List<Component> lore = m.hasLore() && m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
            lore.addFirst(Component.text(estrelas + " Qualidade " + nome, cor).decoration(TextDecoration.ITALIC, false));
            m.lore(lore);
        });
    }
}
