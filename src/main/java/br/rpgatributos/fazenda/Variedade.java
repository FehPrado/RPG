package br.rpgatributos.fazenda;

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
 * Sementes mutantes: nascem quando duas plantações diferentes amadurecem lado a lado.
 * Plantadas, crescem como a planta comum, mas a colheita é a variedade especial,
 * usada nas receitas mais fortes da Cozinha.
 */
public enum Variedade {
    TRIGO_DOURADO("Trigo Dourado", 0xFFD23F, Material.WHEAT, Material.WHEAT, Material.WHEAT_SEEDS,
            Material.WHEAT, Material.CARROTS),
    CENOURA_CRISTALINA("Cenoura Cristalina", 0x7FE3D4, Material.CARROTS, Material.CARROT, Material.CARROT,
            Material.CARROTS, Material.POTATOES),
    BATATA_ANCESTRAL("Batata Ancestral", 0xB58A55, Material.POTATOES, Material.POTATO, Material.POTATO,
            Material.POTATOES, Material.BEETROOTS),
    BETERRABA_RUBI("Beterraba Rubi", 0xE0115F, Material.BEETROOTS, Material.BEETROOT, Material.BEETROOT_SEEDS,
            Material.BEETROOTS, Material.WHEAT);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "variedade");
    public static final NamespacedKey CHAVE_SEMENTE = new NamespacedKey("rpgatributos", "variedade_semente");

    private final String nome;
    private final TextColor cor;
    private final Material planta;
    private final Material produto;
    private final Material semente;
    private final Material paiA;
    private final Material paiB;

    Variedade(String nome, int cor, Material planta, Material produto, Material semente, Material paiA, Material paiB) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.planta = planta;
        this.produto = produto;
        this.semente = semente;
        this.paiA = paiA;
        this.paiB = paiB;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    /** Bloco da plantação (ex.: WHEAT, CARROTS). */
    public Material planta() { return planta; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Plantações que, maduras lado a lado, podem gerar essa variedade. */
    public boolean cruzamento(Material a, Material b) {
        return (a == paiA && b == paiB) || (a == paiB && b == paiA);
    }

    public ItemStack produto(int qtd) {
        ItemStack i = new ItemStack(produto, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text("Variedade rara. Ingrediente de", NamedTextColor.GRAY),
                            Component.text("pratos especiais da Cozinha.", NamedTextColor.GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
        });
        return i;
    }

    public ItemStack semente(int qtd) {
        ItemStack i = new ItemStack(semente, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Semente de " + nome, cor));
            m.lore(List.of(Component.text("Plante em terra arada: nasce " + nome + ".", NamedTextColor.GRAY),
                            Component.text("Nasceu de um cruzamento de plantações.", NamedTextColor.DARK_GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            m.getPersistentDataContainer().set(CHAVE_SEMENTE, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    /** Variedade do item (produto ou semente), ou null. */
    public static Variedade de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean ehSemente(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(CHAVE_SEMENTE);
    }

    public static Variedade porId(String id) {
        for (Variedade v : values()) if (v.id().equalsIgnoreCase(id)) return v;
        return null;
    }
}
