package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
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

/** Ervas que crescem no Canteiro de Ervas, cada uma num tipo de lugar. */
public enum Erva {
    SALVIA("Sálvia", 0x8BC34A, "oak_sapling", "Cresce em qualquer lugar ameno. Usada no Frasco de Cura."),
    ERVA_DE_SOL("Erva-de-Sol", 0xFFC107, "dandelion", "Só nos lugares quentes (deserto, savana, terras áridas). Usada no Fogo-Grego."),
    MUSGO_LUNAR("Musgo Lunar", 0x9FA8DA, "glow_lichen", "Pântanos e manguezais. Usado no Frasco de Fumaça."),
    FOLHA_GELIDA("Folha Gélida", 0x81D4FA, "blue_orchid", "Lugares frios ou o inverno."),
    RAIZ_ABISSAL("Raiz Abissal", 0x6D4C41, "hanging_roots", "Embaixo da terra (abaixo da altura 40)."),
    FLOR_DE_CRISTAL("Flor-de-Cristal", 0xF48FB1, "azure_bluet", "Cerejeiras, campos de flores e prados.");

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "erva");

    private final String nome, modelo, descricao;
    private final TextColor cor;

    Erva(String nome, int cor, String modelo, String descricao) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.modelo = modelo;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.setItemModel(NamespacedKey.minecraft(modelo));
            m.lore(List.of(Component.text(descricao, NamedTextColor.GRAY), Component.empty(),
                            Component.text("☘ Erva do Canteiro", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "erva_" + id());
        });
        return i;
    }

    public static Erva de(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
