package br.rpgatributos.exploracao;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

/**
 * Itens do céu e das profundezas que não são materiais raros: a Fruta Celeste (da Árvore
 * Celeste, deixa leve como o vento) e a Semente Brilhante (das Raízes de Luz, ilumina onde cai).
 */
public enum ItemCeu {
    FRUTA_CELESTE("Fruta Celeste", Material.APPLE, 0xB39DDB,
            "Cresce numa árvore que nunca tocou o chão.",
            "Ao comer: Queda Lenta por 90 s e pulo mais alto por 60 s."),
    SEMENTE_BRILHANTE("Semente Brilhante", Material.SNOWBALL, 0xFFD54F,
            "Guarda um pouco da luz das raízes.",
            "Arremesse: ilumina onde cair por 2 minutos.");

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "item_ceu");

    private final String nome;
    private final Material base;
    private final TextColor cor;
    private final String frase, uso;

    ItemCeu(String nome, Material base, int cor, String frase, String uso) {
        this.nome = nome;
        this.base = base;
        this.cor = TextColor.color(cor);
        this.frase = frase;
        this.uso = uso;
    }

    public String nome() { return nome; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(base, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(
                    Component.text(frase, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true),
                    Component.empty(),
                    Component.text(uso, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
            if (this == SEMENTE_BRILHANTE) m.setMaxStackSize(16);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "ceu_" + id());
        });
        return i;
    }

    public static ItemCeu de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ItemCeu porId(String id) {
        for (ItemCeu x : values()) if (x.id().equalsIgnoreCase(id) || x.name().equalsIgnoreCase(id)) return x;
        return null;
    }
}
