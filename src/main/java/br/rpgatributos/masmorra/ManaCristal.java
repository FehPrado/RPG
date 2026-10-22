package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Cristais de Mana (2.68): o que os monstros dos portais deixam. Valem como moeda no Altar do
 * Portal (no lugar da oferenda) e alimentam a magia oculta. Quatro tamanhos: 1, 4, 16 e 64 de valor.
 */
public final class ManaCristal {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "cristal_mana");
    static final TextColor COR = TextColor.color(0x26C6DA);

    public enum Tamanho {
        PEQUENO("pequeno", Material.SMALL_AMETHYST_BUD, 1),
        MEDIO("médio", Material.MEDIUM_AMETHYST_BUD, 4),
        GRANDE("grande", Material.LARGE_AMETHYST_BUD, 16),
        PERFEITO("perfeito", Material.AMETHYST_CLUSTER, 64);

        public final String nome;
        public final Material material;
        public final int valor;

        Tamanho(String nome, Material material, int valor) {
            this.nome = nome;
            this.material = material;
            this.valor = valor;
        }
    }

    private ManaCristal() { }

    public static ItemStack criar(Tamanho t, int qtd) {
        ItemStack i = new ItemStack(t.material, Math.max(1, qtd));
        i.editMeta(m -> {
            m.itemName(Component.text("Cristal de Mana " + t.nome, COR));
            m.lore(List.of(
                    Component.text("Vale " + t.valor + " de mana no Altar do Portal.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Sai dos monstros e baús dos portais.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, t.name());
        });
        return i;
    }

    public static Tamanho de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try { return Tamanho.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** Quanto de mana o jogador tem em cristais. */
    public static int valor(Player p) {
        int v = 0;
        for (ItemStack i : p.getInventory().getStorageContents()) {
            Tamanho t = de(i);
            if (t != null) v += t.valor * i.getAmount();
        }
        return v;
    }

    /**
     * Gasta {@code quanto} de mana em cristais, dos menores para os maiores; o troco volta em
     * cristais pequenos. @return false se não tem o bastante (nada é gasto).
     */
    public static boolean gastar(Player p, int quanto) {
        if (valor(p) < quanto) return false;
        PlayerInventory inv = p.getInventory();
        int falta = quanto;
        for (Tamanho t : Tamanho.values()) {
            ItemStack[] c = inv.getStorageContents();
            for (int s = 0; s < c.length && falta > 0; s++) {
                if (de(c[s]) != t) continue;
                while (c[s].getAmount() > 0 && falta > 0) {
                    c[s].setAmount(c[s].getAmount() - 1);
                    falta -= t.valor;
                }
            }
            inv.setStorageContents(c);
            if (falta <= 0) break;
        }
        if (falta < 0) for (ItemStack s : inv.addItem(criar(Tamanho.PEQUENO, -falta)).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
        return true;
    }

    /** Tem pelo menos um cristal desse tamanho (ou maior)? Gasta um. */
    public static boolean gastarUm(Player p, Tamanho minimo) {
        ItemStack[] c = p.getInventory().getStorageContents();
        for (Tamanho t : Tamanho.values()) {
            if (t.ordinal() < minimo.ordinal()) continue;
            for (int s = 0; s < c.length; s++) {
                if (de(c[s]) != t) continue;
                c[s].setAmount(c[s].getAmount() - 1);
                p.getInventory().setStorageContents(c);
                return true;
            }
        }
        return false;
    }
}
