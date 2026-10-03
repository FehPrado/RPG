package br.rpgatributos;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Locale;

/** Os atributos que o jogador evolui usando. */
public enum Skill {
    MINERACAO("mineracao", "Mineração", "⛏", NamedTextColor.GRAY, Material.IRON_PICKAXE,
            "Quebre pedras e minérios com picareta."),
    MADEIRA("madeira", "Corte de Madeira", "♣", NamedTextColor.GOLD, Material.IRON_AXE,
            "Corte troncos de árvore."),
    CORRIDA("corrida", "Corrida", "»", NamedTextColor.AQUA, Material.LEATHER_BOOTS,
            "Corra (sprint) pelo mundo."),
    PULO("pulo", "Pulo e Queda", "⬆", NamedTextColor.GREEN, Material.FEATHER,
            "Pule e sobreviva a quedas."),
    COMBATE("combate", "Combate", "⚔", NamedTextColor.RED, Material.IRON_SWORD,
            "Derrote monstros, animais e jogadores."),
    FERRARIA("ferraria", "Ferraria", "⚒", TextColor.color(0xE8823A), Material.ANVIL,
            "Fabrique armas, ferramentas e armaduras e funda lingotes."),
    ARCANO("arcano", "Arcano", "✦", TextColor.color(0xC77DFF), Material.ENCHANTING_TABLE,
            "Infunda itens no corpo e lance magias."),
    NATACAO("natacao", "Natação", "≈", TextColor.color(0x3D8BFF), Material.TROPICAL_FISH,
            "Nade e mergulhe (andar na correnteza parado não conta)."),
    AGRICULTURA("agricultura", "Agricultura", "☘", TextColor.color(0x7BC043), Material.GOLDEN_HOE,
            "Colha plantações maduras."),
    CULINARIA("culinaria", "Culinária", "♨", TextColor.color(0xE8A33D), Material.SMOKER,
            "Cozinhe pratos na Cozinha."),
    DOMA("doma", "Doma", "♞", TextColor.color(0xC9955C), Material.LEAD,
            "Dome e cruze animais, monte e cuide dos seus companheiros.");

    private final String id;
    private final String nome;
    private final String icone;
    private final TextColor cor;
    private final Material material;
    private final String comoEvoluir;

    Skill(String id, String nome, String icone, TextColor cor, Material material, String comoEvoluir) {
        this.id = id;
        this.nome = nome;
        this.icone = icone;
        this.cor = cor;
        this.material = material;
        this.comoEvoluir = comoEvoluir;
    }

    public String id() { return id; }
    public String nome() { return nome; }
    public String icone() { return icone; }
    public TextColor cor() { return cor; }
    public Material material() { return material; }
    public String comoEvoluir() { return comoEvoluir; }

    public static Skill porId(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);
        for (Skill s : values()) {
            if (s.id.equals(t) || s.name().equalsIgnoreCase(t)) return s;
        }
        return null;
    }
}
