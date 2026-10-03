package br.rpgatributos.oculto;

import br.rpgatributos.classe.Classe;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

/**
 * Tipos de Local Oculto. Cada um tem a sua aparência, um guardião e guarda uma classe lendária.
 */
public enum TipoLocal {
    TUMULO_DO_FERREIRO("Túmulo do Ferreiro Antigo", 0xFF8F00, Classe.HERDEIRO_DO_FERREIRO,
            Material.DEEPSLATE_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS, Material.ANVIL, Material.LANTERN,
            EntityType.IRON_GOLEM, "Guardião da Forja Antiga",
            "Aqui jaz o ferreiro cujo martelo fazia o metal cantar. Quem quiser o seu legado deve acordar as brasas na ordem certa:"),
    BIBLIOTECA_PROIBIDA("Biblioteca Proibida", 0x7E57C2, Classe.ARQUIMAGO_PRIMORDIAL,
            Material.DARK_OAK_PLANKS, Material.BOOKSHELF, Material.ENCHANTING_TABLE, Material.SOUL_LANTERN,
            EntityType.EVOKER, "Bibliotecário Amaldiçoado",
            "Os livros daqui guardam a primeira magia. Para abrir a sala do saber, leia os quatro selos na ordem:"),
    COVIL_DAS_FERAS("Covil das Feras Ancestrais", 0x6D4C41, Classe.SENHOR_DAS_FERAS_ANCESTRAIS,
            Material.MOSSY_COBBLESTONE, Material.MUD_BRICKS, Material.BONE_BLOCK, Material.LANTERN,
            EntityType.RAVAGER, "Fera Ancestral",
            "As feras mais antigas dormem aqui. Só quem chama na ordem do bando atravessa o portão:"),
    CRIPTA_DO_REI_CAIDO("Cripta do Rei Caído", 0x90A4AE, Classe.SENHOR_DA_GUERRA,
            Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.SKELETON_SKULL, Material.SOUL_LANTERN,
            EntityType.WITHER_SKELETON, "Rei Caído",
            "O rei que nunca perdeu uma guerra espera um herdeiro. Toque os estandartes na ordem da batalha:"),
    SANTUARIO_DAS_SOMBRAS("Santuário das Sombras", 0x37474F, Classe.LAMINA_FANTASMA,
            Material.POLISHED_BLACKSTONE, Material.OBSIDIAN, Material.WITHER_SKELETON_SKULL, Material.SOUL_LANTERN,
            EntityType.VINDICATOR, "Sombra Sem Nome",
            "Nada aqui faz barulho. Quem anda sem ser visto sabe a ordem das sombras:"),
    OBSERVATORIO_CELESTE("Observatório Celeste", 0x80DEEA, Classe.ARQUEIRO_CELESTIAL,
            Material.CALCITE, Material.AMETHYST_BLOCK, Material.SPYGLASS, Material.SEA_LANTERN,
            EntityType.STRAY, "Arqueiro Espectral",
            "Daqui se mirava nas estrelas. Siga o caminho do céu para abrir o portão:"),
    CAPELA_PROFANADA("Capela Profanada", 0xFFF176, Classe.SANTO_PALADINO,
            Material.QUARTZ_BRICKS, Material.CHISELED_QUARTZ_BLOCK, Material.GOLD_BLOCK, Material.GLOWSTONE,
            EntityType.ZOMBIE, "Paladino Corrompido",
            "A luz abandonou esta capela. Para devolvê-la, acenda as velas na ordem da oração:");

    private final String nome;
    private final TextColor cor;
    private final Classe classe;
    private final Material parede, detalhe, enfeite, luz;
    private final EntityType guardiao;
    private final String nomeGuardiao;
    private final String abertura;

    TipoLocal(String nome, int cor, Classe classe, Material parede, Material detalhe, Material enfeite, Material luz,
              EntityType guardiao, String nomeGuardiao, String abertura) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.classe = classe;
        this.parede = parede;
        this.detalhe = detalhe;
        this.enfeite = enfeite;
        this.luz = luz;
        this.guardiao = guardiao;
        this.nomeGuardiao = nomeGuardiao;
        this.abertura = abertura;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    /** A classe lendária que o selo deste local concede. */
    public Classe classe() { return classe; }
    public Material parede() { return parede; }
    public Material detalhe() { return detalhe; }
    public Material enfeite() { return enfeite; }
    public Material luz() { return luz; }
    public EntityType guardiao() { return guardiao; }
    public String nomeGuardiao() { return nomeGuardiao; }
    /** Início do enigma (o livro no púlpito da antessala). */
    public String abertura() { return abertura; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static TipoLocal porId(String id) {
        for (TipoLocal t : values()) if (t.id().equalsIgnoreCase(id)) return t;
        return null;
    }

    public static TipoLocal daClasse(Classe c) {
        for (TipoLocal t : values()) if (t.classe == c) return t;
        return null;
    }
}
