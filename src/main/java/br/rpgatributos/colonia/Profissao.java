package br.rpgatributos.colonia;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.util.Locale;

/**
 * Profissões dos cidadãos da colônia. Cada uma precisa de um posto no território da colônia
 * (um bloco de profissão do jogo ou uma estação do plugin) e libera num nível da colônia.
 * A roupa do aldeão é a da profissão do jogo mais parecida.
 */
public enum Profissao {
    DESEMPREGADO("Desempregado", Material.WHEAT_SEEDS, 0xAAAAAA, Villager.Profession.NONE, 1, null, Posto.NENHUM,
            "Esperando um trabalho."),
    FAZENDEIRO("Fazendeiro", Material.WHEAT, 0x7BC043, Villager.Profession.FARMER, 1, Material.COMPOSTER, Posto.BLOCO,
            "Colhe as plantações perto da composteira (trigo, cenoura, batata, beterraba)."),
    LENHADOR("Lenhador", Material.OAK_LOG, 0xC98A3C, Villager.Profession.FLETCHER, 1, Material.FLETCHING_TABLE, Posto.BLOCO,
            "Traz troncos, mudas e maçãs."),
    PESCADOR("Pescador", Material.COD, 0x2FA4C9, Villager.Profession.FISHERMAN, 1, Material.BARREL, Posto.BLOCO,
            "Pesca perto do barril (precisa de água por perto)."),
    MINERADOR("Minerador", Material.IRON_PICKAXE, 0x8C8C8C, Villager.Profession.MASON, 2, Material.STONECUTTER, Posto.BLOCO,
            "Traz pedra, carvão e minérios brutos (às vezes diamante)."),
    COZINHEIRO("Cozinheiro", Material.BREAD, 0xE8A33D, Villager.Profession.BUTCHER, 2, Material.SMOKER, Posto.COZINHA,
            "Cozinha pratos com os ingredientes do depósito (alimentam a colônia)."),
    FERREIRO("Ferreiro", Material.ANVIL, 0xE8823A, Villager.Profession.TOOLSMITH, 3, Material.ANVIL, Posto.FORJA,
            "Conserta equipamentos do depósito e funde minérios brutos."),
    TRATADOR("Tratador", Material.LEAD, 0xC9955C, Villager.Profession.SHEPHERD, 3, Material.HAY_BLOCK, Posto.ALTAR_DOMADOR,
            "Cuida dos animais: couro, lã, ovos, penas e carne."),
    ALQUIMISTA("Alquimista", Material.BREWING_STAND, 0x4FD1A5, Villager.Profession.CLERIC, 4, Material.BREWING_STAND, Posto.BANCADA_ALQUIMICA,
            "Faz Pó Arcano e Óleo de Peixe com o que tiver no depósito."),
    MERCADOR("Mercador", Material.EMERALD, 0x3DDC84, Villager.Profession.CARTOGRAPHER, 2, Material.CARTOGRAPHY_TABLE, Posto.BLOCO,
            "Vende a sobra do depósito na estrada e traz esmeraldas."),
    BIBLIOTECARIO("Bibliotecário", Material.BOOK, 0xB39DDB, Villager.Profession.LIBRARIAN, 3, Material.LECTERN, Posto.BLOCO,
            "Faz papel, livros e, com lápis-lazúli, livros encantados."),
    CONSTRUTOR("Construtor", Material.BRICKS, 0xD7A86E, Villager.Profession.LEATHERWORKER, 1, Material.CRAFTING_TABLE, Posto.BLOCO,
            "Ergue casas prontas (2 camas, porta e luz) com madeira, pedra e vidro do depósito."),
    SOLDADO("Soldado", Material.IRON_SWORD, 0xFF5555, Villager.Profession.NONE, 2, Material.TARGET, Posto.QUARTEL,
            "Veste a armadura e luta de espada: defende a colônia e vai à guerra."),
    ARQUEIRO("Arqueiro", Material.BOW, 0xFFAA00, Villager.Profession.NONE, 3, Material.TARGET, Posto.QUARTEL,
            "Luta de arco, de longe."),
    CAVALEIRO("Cavaleiro", Material.SADDLE, 0xFFD54F, Villager.Profession.NONE, 4, Material.TARGET, Posto.QUARTEL,
            "Luta montado a cavalo: mais vida, mais rápido e bate forte.");

    /** Cada Quartel (bloco de alvo) abriga 4 soldados. */
    public static final int SOLDADOS_POR_QUARTEL = 4;

    public boolean soldado() { return this == SOLDADO || this == ARQUEIRO || this == CAVALEIRO; }

    /** Que tipo de posto a profissão precisa. */
    public enum Posto { NENHUM, BLOCO, COZINHA, FORJA, ALTAR_DOMADOR, BANCADA_ALQUIMICA, QUARTEL }

    private final String nome;
    private final Material icone;
    private final TextColor cor;
    private final Villager.Profession roupa;
    private final int nivelColonia;
    private final Material bloco;
    private final Posto posto;
    private final String descricao;

    Profissao(String nome, Material icone, int cor, Villager.Profession roupa, int nivelColonia, Material bloco, Posto posto,
              String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.cor = TextColor.color(cor);
        this.roupa = roupa;
        this.nivelColonia = nivelColonia;
        this.bloco = bloco;
        this.posto = posto;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    public TextColor cor() { return cor; }
    public Villager.Profession roupa() { return roupa; }
    public int nivelColonia() { return nivelColonia; }
    /** Bloco do posto (para as estações do plugin, o bloco-base delas). */
    public Material bloco() { return bloco; }
    public Posto posto() { return posto; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Nome do posto para mostrar no menu. */
    public String nomePosto() {
        return switch (posto) {
            case NENHUM -> "nenhum";
            case BLOCO -> switch (this) {
                case FAZENDEIRO -> "Composteira numa Fazenda";
                case LENHADOR -> "Bancada de flechas";
                case PESCADOR -> "Barril";
                case MINERADOR -> "Cortador de pedras";
                case MERCADOR -> "Mesa de cartografia";
                case BIBLIOTECARIO -> "Atril numa Biblioteca";
                case CONSTRUTOR -> "Bancada de trabalho";
                default -> "?";
            };
            case COZINHA -> "Cozinha";
            case FORJA -> "Forja do Ferreiro";
            case ALTAR_DOMADOR -> "Altar do Domador";
            case BANCADA_ALQUIMICA -> "Bancada Alquímica";
            case QUARTEL -> "Alvo num Quartel";
        };
    }

    /**
     * A construção onde essa profissão trabalha (como as cabanas do MineColonies): o posto só conta
     * se estiver dentro de uma construção desse tipo, pronta (nível 1 ou mais). null = posto em qualquer lugar.
     */
    public Planta.Efeito construcao() {
        return switch (this) {
            case FAZENDEIRO -> Planta.Efeito.FAZENDA;
            case BIBLIOTECARIO -> Planta.Efeito.BIBLIOTECA;
            case SOLDADO, ARQUEIRO, CAVALEIRO -> Planta.Efeito.QUARTEL;
            default -> null;
        };
    }

    public static Profissao porId(String id) {
        for (Profissao p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
