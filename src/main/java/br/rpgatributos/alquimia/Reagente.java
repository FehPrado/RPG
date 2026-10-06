package br.rpgatributos.alquimia;

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
 * Componentes mágicos: feitos na Bancada Alquímica (ou achados pescando) e usados
 * para fazer elixires, gemas e acessórios.
 */
public enum Reagente {
    PO_ARCANO("Pó Arcano", "glowstone_dust", 0xC77DFF, "Base de quase toda receita alquímica."),
    OLEO_DE_PEIXE("Óleo de Peixe", "honey_bottle", 0xE8C15A, "Brilha fraco no escuro. Usado em lanternas e elixires da água."),
    TINTURA_VITAL("Tintura Vital", "red_dye", 0xE0115F, "Cheira a ferro e flores. Cura e vigor."),
    SAL_LUNAR("Sal Lunar", "sugar", 0xC9D6FF, "Cristais que só brilham à noite."),
    MERCURIO_VIVO("Mercúrio Vivo", "iron_nugget", 0xD8D8E8, "Metal que não para quieto."),
    CRISTAL_DE_MANA("Cristal de Mana", "prismarine_crystals", 0x7FD7FF, "Guarda mana como uma bateria."),
    SOLVENTE("Solvente Alquímico", "experience_bottle", 0x9ACD32, "Solta gemas engastadas sem quebrá-las."),
    PEROLA_NEGRA("Pérola Negra", "ender_pearl", 0x3B2E5A, "Vem do fundo do mar: Tesouros do Mar e criaturas marinhas."),
    ESCAMA_DO_ABISMO("Escama do Abismo", "turtle_scute", 0x2E8B7A, "Cai das criaturas marinhas que mordem a isca."),
    // ---------- óleos de lâmina (passados na arma) ----------
    OLEO_DE_FOGO("Óleo de Fogo", "honey_bottle", 0xFF7043, "Os golpes incendeiam. Elemento: Fogo."),
    OLEO_GELIDO("Óleo Gélido", "experience_bottle", 0x81D4FA, "Os golpes congelam e deixam lento. Elemento: Gelo."),
    OLEO_TROVEJANTE("Óleo Trovejante", "experience_bottle", 0xFFF176, "Às vezes um choque salta para outro inimigo. Elemento: Energia."),
    OLEO_VENENOSO("Óleo Venenoso", "dragon_breath", 0x8BC34A, "Os golpes envenenam. Elemento: Natureza."),
    OLEO_DE_PRATA("Óleo de Prata", "ominous_bottle", 0xE0E0E0, "+40% de dano contra mortos-vivos. Elemento: Vida.");

    /** É um óleo de lâmina (vai na arma, não é ingrediente). */
    public boolean oleo() {
        return name().startsWith("OLEO_") && this != OLEO_DE_PEIXE;
    }

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "reagente");

    private final String nome;
    private final NamespacedKey modelo;
    private final TextColor cor;
    private final String descricao;

    Reagente(String nome, String modelo, int cor, String descricao) {
        this.nome = nome;
        this.modelo = NamespacedKey.minecraft(modelo);
        this.cor = TextColor.color(cor);
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public NamespacedKey modelo() { return modelo; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.setItemModel(modelo);
            m.lore((oleo() ? List.of(
                    Component.text(descricao, NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Segure e clique com o botão direito: o óleo", NamedTextColor.WHITE),
                    Component.text("vai na arma da outra mão (ou na 1ª arma", NamedTextColor.WHITE),
                    Component.text("da sua barra) e dura 60 golpes.", NamedTextColor.WHITE),
                    Component.empty(),
                    Component.text("🜄 Óleo de lâmina", cor))
                    : List.of(
                    Component.text(descricao, NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("⚗ Componente alquímico", cor)))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "componente_" + id());
        });
        return i;
    }

    public static Reagente de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Reagente porId(String id) {
        for (Reagente r : values()) if (r.id().equalsIgnoreCase(id)) return r;
        return null;
    }
}
