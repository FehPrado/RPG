package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Relíquias do Monarca (2.74): 25 (5 de cada Monarca), raras nos tesouros dos portais A e S. No
 * Mural das Relíquias, uma linha completa abre o Portal daquele Monarca; as 25 juntas abrem o
 * do Monarca da Destruição.
 */
public final class Reliquias {

    private static final NamespacedKey K = new NamespacedKey("rpgatributos", "reliquia_monarca");

    /** Uma relíquia: o Monarca e qual das 5 (0 a 4). */
    public record Reliquia(Monarca monarca, int indice) {
        public String nome() { return monarca.reliquias[indice]; }
        /** A posição dela no mural (0 a 24). */
        public int bit() { return monarca.ordinal() * 5 + indice; }
    }

    private Reliquias() { }

    public static ItemStack criar(Monarca m, int indice) {
        ItemStack i = new ItemStack(Material.ECHO_SHARD);
        i.editMeta(meta -> {
            meta.itemName(Component.text("♛ " + m.reliquias[indice], m.cor));
            meta.lore(List.of(
                    Component.text("Relíquia do " + m.nome + " (" + (indice + 1) + "/5)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Coloque no Mural das Relíquias.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(16);
            meta.getPersistentDataContainer().set(K, PersistentDataType.STRING, m.name() + ":" + indice);
        });
        return i;
    }

    public static ItemStack sortear() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Monarca[] ms = Monarca.values();
        return criar(ms[r.nextInt(ms.length)], r.nextInt(5));
    }

    public static Reliquia de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K, PersistentDataType.STRING);
        if (s == null) return null;
        String[] p = s.split(":");
        try {
            int n = Integer.parseInt(p[1]);
            return n >= 0 && n < 5 ? new Reliquia(Monarca.valueOf(p[0]), n) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static NamespacedKey chave() { return K; }

    /** Chance de uma relíquia no tesouro final: A 6%, S 12% (e mais no Ataque de Rank S, depois). */
    static double chance(Rank r) {
        return switch (r) {
            case A -> 0.06;
            case S -> 0.12;
            default -> 0;
        };
    }
}
