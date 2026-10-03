package br.rpgatributos.classe;

import br.rpgatributos.Skill;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static br.rpgatributos.classe.Tarefa.Tipo.*;
import static br.rpgatributos.classe.Tarefa.t;

/**
 * A árvore de classes. As básicas pedem nível total e o atributo da classe; as
 * avançadas pedem classes DOMINADAS antes (às vezes duas). Cada classe tem passivo,
 * duas habilidades, tarefas do caminho e uma prova.
 */
public enum Classe {
    // ---------------- básicas ----------------
    GUERREIRO("Guerreiro", "⚔", 0xE53935, Material.IRON_SWORD, Tier.BASICA, List.of(), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE),
            Passivo.p().vida(4).corpo(0.10).texto("+2 ❤ e +10% de dano corpo a corpo"),
            List.of(Habilidade.INVESTIDA, Habilidade.GRITO_DE_GUERRA),
            List.of(t(ABATES_CORPO, 100)),
            Prova.ONDAS, null, List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER),
            "Linha de frente: aguenta pancada e bate forte."),
    ARQUEIRO("Arqueiro", "➹", 0x7CB342, Material.BOW, Tier.BASICA, List.of(), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE, Skill.CORRIDA),
            Passivo.p().flecha(0.15).velocidade(0.05).texto("+15% de dano com flechas e +5% de velocidade"),
            List.of(Habilidade.CHUVA_DE_FLECHAS, Habilidade.SALTO_EVASIVO),
            List.of(t(ABATES_FLECHA, 100)),
            Prova.ALVOS, null, List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.SPIDER),
            "Mata de longe e nunca fica parado."),
    MAGO("Mago", "✦", 0x8E24AA, Material.ENCHANTED_BOOK, Tier.BASICA, List.of(), Skill.ARCANO,
            EnumSet.of(Skill.ARCANO),
            Passivo.p().mana(40).regen(1).texto("+40 de mana e +1 de mana por segundo"),
            List.of(Habilidade.MISSIL_ARCANO, Habilidade.BARREIRA_ARCANA),
            List.of(t(MAGIAS, 150)),
            Prova.DUELO, EntityType.EVOKER, List.of(EntityType.VEX),
            "Domina o Arcano: mais mana e magias de classe."),
    LADINO("Ladino", "✧", 0x546E7A, Material.LEATHER_BOOTS, Tier.BASICA, List.of(), Skill.CORRIDA,
            EnumSet.of(Skill.CORRIDA, Skill.PULO, Skill.COMBATE),
            Passivo.p().velocidade(0.12).esquiva(0.08).texto("+12% de velocidade e 8% de chance de esquivar"),
            List.of(Habilidade.BOMBA_DE_FUMACA, Habilidade.ROLAMENTO),
            List.of(t(CORRER, 3000), t(ABATES_CORPO, 50)),
            Prova.DUELO, EntityType.VINDICATOR, List.of(),
            "Rápido e escorregadio: ninguém acerta."),
    FERREIRO("Ferreiro", "⚒", 0xE8823A, Material.ANVIL, Tier.BASICA, List.of(), Skill.FERRARIA,
            EnumSet.of(Skill.FERRARIA, Skill.MINERACAO),
            Passivo.p().raridade(0.05).refino(0.03).texto("5% de chance de forjar uma raridade acima e +3% no refino"),
            List.of(Habilidade.REPARO_RAPIDO, Habilidade.ARMADURA_REFORCADA),
            List.of(t(FORJAR, 20)),
            Prova.DUELO, EntityType.IRON_GOLEM, List.of(),
            "O caminho do Overgeared: forja melhor que ninguém."),
    DOMADOR("Domador", "♞", 0xC9955C, Material.LEAD, Tier.BASICA, List.of(), Skill.DOMA,
            EnumSet.of(Skill.DOMA),
            Passivo.p().companheiros(1).danoCompanheiro(0.15).texto("+1 companheiro e +15% de dano deles"),
            List.of(Habilidade.CHAMADO, Habilidade.FURIA_DA_MATILHA),
            List.of(t(COMPANHEIROS, 2), t(ABATES_COMPANHEIRO, 30)),
            Prova.ONDAS, null, List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER),
            "Luta junto com as suas feras."),

    // ---------------- avançadas ----------------
    BERSERKER("Berserker", "☠", 0xB71C1C, Material.NETHERITE_AXE, Tier.AVANCADA, List.of(GUERREIRO), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE),
            Passivo.p().vida(4).corpo(0.15).texto("+15% de dano corpo a corpo; até +40% com pouca vida"),
            List.of(Habilidade.FURIA, Habilidade.REDEMOINHO),
            List.of(t(ABATES_CORPO, 500), t(CHEFES, 3)),
            Prova.ONDAS, null, List.of(EntityType.VINDICATOR, EntityType.ZOMBIE, EntityType.HUSK),
            "Quanto mais apanha, mais forte bate."),
    CAVALEIRO("Cavaleiro", "♜", 0x90A4AE, Material.IRON_HORSE_ARMOR, Tier.AVANCADA, List.of(GUERREIRO, DOMADOR), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE, Skill.DOMA),
            Passivo.p().vida(6).armadura(4).corpo(0.10).texto("+3 ❤, +4 de armadura e +25% de dano montado"),
            List.of(Habilidade.CARGA, Habilidade.BALUARTE),
            List.of(t(ABATES_CORPO, 300), t(MASMORRAS, 1)),
            Prova.DUELO, EntityType.RAVAGER, List.of(),
            "Guerreiro montado: tanque da party."),
    PALADINO("Paladino", "✚", 0xFDD835, Material.GOLDEN_SWORD, Tier.AVANCADA, List.of(GUERREIRO, MAGO), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE, Skill.ARCANO),
            Passivo.p().vida(6).armadura(3).mana(30).corpo(0.05).texto("+3 ❤, +3 de armadura e +30 de mana"),
            List.of(Habilidade.LUZ_SAGRADA, Habilidade.ESCUDO_DIVINO),
            List.of(t(ABATES_CORPO, 300), t(MAGIAS, 300)),
            Prova.ONDAS, null, List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER_SKELETON),
            "Cura e protege os aliados; terror dos mortos-vivos."),
    ATIRADOR("Atirador de Elite", "➶", 0x33691E, Material.CROSSBOW, Tier.AVANCADA, List.of(ARQUEIRO), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE, Skill.CORRIDA),
            Passivo.p().flecha(0.30).texto("+30% de dano com flechas e 15% de chance de crítico (x2)"),
            List.of(Habilidade.TIRO_PERFURANTE, Habilidade.OLHO_DE_AGUIA),
            List.of(t(ABATES_FLECHA, 500), t(CHEFES, 3)),
            Prova.ALVOS, null, List.of(EntityType.BLAZE, EntityType.SKELETON, EntityType.HUSK),
            "Um tiro, um abate."),
    CACADOR("Caçador", "☤", 0x6D4C41, Material.SPYGLASS, Tier.AVANCADA, List.of(ARQUEIRO, DOMADOR), Skill.COMBATE,
            EnumSet.of(Skill.COMBATE, Skill.DOMA),
            Passivo.p().flecha(0.20).danoCompanheiro(0.20).velocidade(0.05).texto("+20% de dano com flechas e dos companheiros"),
            List.of(Habilidade.MARCA_DO_CACADOR, Habilidade.ARMADILHA),
            List.of(t(ABATES_FLECHA, 300), t(ABATES_COMPANHEIRO, 50)),
            Prova.ALVOS, null, List.of(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.HUSK),
            "Marca a presa e caça junto com as feras."),
    ELEMENTALISTA("Elementalista", "❄", 0x29B6F6, Material.BLAZE_ROD, Tier.AVANCADA, List.of(MAGO), Skill.ARCANO,
            EnumSet.of(Skill.ARCANO),
            Passivo.p().mana(80).regen(2).custoMagia(0.20).texto("+80 de mana, +2 de mana/s e magias 20% mais baratas"),
            List.of(Habilidade.TEMPESTADE_ELEMENTAL, Habilidade.ESCUDO_ELEMENTAL),
            List.of(t(MAGIAS, 800), t(CHEFES, 3)),
            Prova.DUELO, EntityType.BLAZE, List.of(EntityType.BLAZE),
            "Fogo, gelo e raio na ponta dos dedos."),
    FEITICEIRO("Feiticeiro de Batalha", "✴", 0x7B1FA2, Material.AMETHYST_SHARD, Tier.AVANCADA, List.of(MAGO, GUERREIRO), Skill.ARCANO,
            EnumSet.of(Skill.ARCANO, Skill.COMBATE),
            Passivo.p().mana(40).regen(1).corpo(0.10).texto("+40 de mana, +10% de dano corpo a corpo; cada golpe devolve mana"),
            List.of(Habilidade.LAMINA_ARCANA, Habilidade.PASSO_ARCANO),
            List.of(t(MAGIAS, 300), t(ABATES_CORPO, 300)),
            Prova.ONDAS, null, List.of(EntityType.VINDICATOR, EntityType.WITCH, EntityType.ZOMBIE),
            "Espada numa mão, magia na outra."),
    ASSASSINO("Assassino", "✝", 0x263238, Material.NETHERITE_SWORD, Tier.AVANCADA, List.of(LADINO), Skill.CORRIDA,
            EnumSet.of(Skill.CORRIDA, Skill.PULO, Skill.COMBATE),
            Passivo.p().velocidade(0.15).esquiva(0.12).corpo(0.10).texto("+15% de velocidade, 12% de esquiva e +50% de dano pelas costas"),
            List.of(Habilidade.PASSOS_DAS_SOMBRAS, Habilidade.GOLPE_LETAL),
            List.of(t(ABATES_CORPO, 400), t(MASMORRAS, 2)),
            Prova.DUELO, EntityType.WITHER_SKELETON, List.of(),
            "Some nas sombras e acaba com tudo num golpe."),
    MESTRE_FORJADOR("Mestre Forjador", "⚒", 0xFF6F00, Material.SMITHING_TABLE, Tier.AVANCADA, List.of(FERREIRO), Skill.FERRARIA,
            EnumSet.of(Skill.FERRARIA, Skill.MINERACAO),
            Passivo.p().raridade(0.12).refino(0.08).armadura(2).texto("12% de chance de forjar uma raridade acima, +8% no refino"),
            List.of(Habilidade.AFIAR, Habilidade.MARTELO_DO_TROVAO),
            List.of(t(FORJAR, 80), t(MASMORRAS, 1)),
            Prova.DUELO, EntityType.IRON_GOLEM, List.of(),
            "O martelo dele faz lendas."),
    MESTRE_DAS_FERAS("Mestre das Feras", "♞", 0x8B5A2B, Material.SADDLE, Tier.AVANCADA, List.of(DOMADOR), Skill.DOMA,
            EnumSet.of(Skill.DOMA),
            Passivo.p().companheiros(2).componentes(1).danoCompanheiro(0.40).texto("+2 companheiros, +1 componente e +40% de dano deles"),
            List.of(Habilidade.RUGIDO, Habilidade.VINCULO_VITAL),
            List.of(t(ABATES_COMPANHEIRO, 150), t(CHEFES, 2)),
            Prova.DUELO, EntityType.RAVAGER, List.of(),
            "Comanda um bando de feras."),

    // ---------------- lendárias (um dono por vez; o selo de um Local Oculto concede) ----------------
    HERDEIRO_DO_FERREIRO("Herdeiro do Ferreiro Lendário", "✪", 0xFF8F00, Material.NETHERITE_INGOT, Tier.LENDARIA,
            List.of(MESTRE_FORJADOR), Skill.FERRARIA, EnumSet.of(Skill.FERRARIA, Skill.MINERACAO, Skill.COMBATE),
            Passivo.p().raridade(0.25).refino(0.15).armadura(4).vida(4).texto("25% de forjar uma raridade acima, +15% no refino, +2 ❤ e +4 de armadura"),
            List.of(Habilidade.MARTELO_CELESTIAL, Habilidade.BENCAO_DO_FERREIRO),
            List.of(), Prova.DUELO, null, List.of(),
            "O legado do maior ferreiro que já existiu."),
    ARQUIMAGO_PRIMORDIAL("Arquimago Primordial", "✺", 0x7E57C2, Material.NETHER_STAR, Tier.LENDARIA,
            List.of(ELEMENTALISTA), Skill.ARCANO, EnumSet.of(Skill.ARCANO),
            Passivo.p().mana(150).regen(4).custoMagia(0.35).texto("+150 de mana, +4 de mana/s e magias 35% mais baratas"),
            List.of(Habilidade.METEORO_ARCANO, Habilidade.TEMPO_SUSPENSO),
            List.of(), Prova.DUELO, null, List.of(),
            "Conhece a magia de antes de o mundo ter nome."),
    SENHOR_DAS_FERAS_ANCESTRAIS("Senhor das Feras Ancestrais", "❂", 0x6D4C41, Material.DRAGON_HEAD, Tier.LENDARIA,
            List.of(MESTRE_DAS_FERAS), Skill.DOMA, EnumSet.of(Skill.DOMA, Skill.COMBATE),
            Passivo.p().companheiros(3).componentes(2).danoCompanheiro(0.80).vida(4).texto("+3 companheiros, +2 componentes, +80% de dano deles e +2 ❤"),
            List.of(Habilidade.CHAMADO_ANCESTRAL, Habilidade.FORMA_BESTIAL),
            List.of(), Prova.DUELO, null, List.of(),
            "As feras mais antigas atendem ao seu chamado."),
    SENHOR_DA_GUERRA("Senhor da Guerra", "♛", 0xB0BEC5, Material.NETHERITE_SWORD, Tier.LENDARIA,
            List.of(BERSERKER), Skill.COMBATE, EnumSet.of(Skill.COMBATE),
            Passivo.p().vida(10).armadura(4).corpo(0.30).texto("+5 ❤, +4 de armadura, +30% de dano; ainda mais forte com pouca vida"),
            List.of(Habilidade.TERREMOTO, Habilidade.ESTANDARTE_DE_GUERRA),
            List.of(), Prova.DUELO, null, List.of(),
            "O herdeiro do rei que nunca perdeu uma guerra."),
    LAMINA_FANTASMA("Lâmina Fantasma", "☽", 0x37474F, Material.ECHO_SHARD, Tier.LENDARIA,
            List.of(ASSASSINO), Skill.CORRIDA, EnumSet.of(Skill.CORRIDA, Skill.PULO, Skill.COMBATE),
            Passivo.p().velocidade(0.25).esquiva(0.25).corpo(0.20).texto("+25% de velocidade, 25% de esquiva e dano x2 pelas costas"),
            List.of(Habilidade.DANCA_DAS_LAMINAS, Habilidade.VEU_SOMBRIO),
            List.of(), Prova.DUELO, null, List.of(),
            "Ninguém a vê chegar. Ninguém a vê partir."),
    ARQUEIRO_CELESTIAL("Arqueiro Celestial", "✵", 0x80DEEA, Material.SPECTRAL_ARROW, Tier.LENDARIA,
            List.of(ATIRADOR), Skill.COMBATE, EnumSet.of(Skill.COMBATE, Skill.CORRIDA),
            Passivo.p().flecha(0.60).velocidade(0.10).texto("+60% de dano com flechas, +10% de velocidade e 25% de crítico (x2)"),
            List.of(Habilidade.FLECHA_ESTELAR, Habilidade.CHUVA_DE_ESTRELAS),
            List.of(), Prova.DUELO, null, List.of(),
            "Mira nas estrelas e acerta."),
    SANTO_PALADINO("Santo Paladino", "☀", 0xFFF176, Material.TOTEM_OF_UNDYING, Tier.LENDARIA,
            List.of(PALADINO), Skill.COMBATE, EnumSet.of(Skill.COMBATE, Skill.ARCANO),
            Passivo.p().vida(10).armadura(6).mana(60).corpo(0.10).texto("+5 ❤, +6 de armadura, +60 de mana e +10% de dano"),
            List.of(Habilidade.JULGAMENTO_DIVINO, Habilidade.AURA_SAGRADA),
            List.of(), Prova.DUELO, null, List.of(),
            "A luz que voltou a uma capela esquecida."),
    SOBERANO_DAS_SOMBRAS("Soberano das Sombras", "☾", 0x5E35B1, Material.WITHER_SKELETON_SKULL, Tier.LENDARIA,
            List.of(), Skill.COMBATE, EnumSet.of(Skill.COMBATE, Skill.ARCANO),
            Passivo.p().vida(6).mana(60).regen(1).corpo(0.15).texto("+3 ❤, +60 de mana, +15% de dano; quem você derrota pode virar sombra"),
            List.of(Habilidade.LEVANTE_SE, Habilidade.EXERCITO_DAS_SOMBRAS),
            List.of(), Prova.DUELO, null, List.of(),
            "Desperta do outro lado de um Portal Pesadelo. Os mortos se levantam para servi-lo.");

    public enum Tier {
        BASICA("Básica"), AVANCADA("Avançada"), OCULTA("Oculta"), LENDARIA("Lendária");

        private final String nome;

        Tier(String nome) { this.nome = nome; }

        public String nome() { return nome; }
    }

    /** Como é a prova de mudança de classe. */
    public enum Prova {
        ONDAS("Sobreviva a 3 ondas de monstros"),
        ALVOS("Derrote os alvos só com flechas"),
        DUELO("Derrote o Guardião da classe");

        private final String descricao;

        Prova(String descricao) { this.descricao = descricao; }

        public String descricao() { return descricao; }
    }

    private final String nome, simbolo, descricao;
    private final TextColor cor;
    private final Material icone;
    private final Tier tier;
    private final List<Classe> requisitos;
    private final Skill atributo;
    private final Set<Skill> afinidades;
    private final Passivo passivo;
    private final List<Habilidade> habilidades;
    private final List<Tarefa> tarefas;
    private final Prova prova;
    private final EntityType guardiao;
    private final List<EntityType> monstros;

    Classe(String nome, String simbolo, int cor, Material icone, Tier tier, List<Classe> requisitos, Skill atributo,
           Set<Skill> afinidades, Passivo passivo, List<Habilidade> habilidades, List<Tarefa> tarefas, Prova prova,
           EntityType guardiao, List<EntityType> monstros, String descricao) {
        this.nome = nome;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.icone = icone;
        this.tier = tier;
        this.requisitos = requisitos;
        this.atributo = atributo;
        this.afinidades = afinidades;
        this.passivo = passivo;
        this.habilidades = habilidades;
        this.tarefas = tarefas;
        this.prova = prova;
        this.guardiao = guardiao;
        this.monstros = monstros;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String simbolo() { return simbolo; }
    public TextColor cor() { return cor; }
    public Material icone() { return icone; }
    public Tier tier() { return tier; }
    /** Classes que precisam estar dominadas antes. */
    public List<Classe> requisitos() { return requisitos; }
    /** Atributo pedido (e o nível dele cresce com o tier). */
    public Skill atributo() { return atributo; }
    /** Atributos cujo XP também sobe o nível da classe. */
    public Set<Skill> afinidades() { return afinidades; }
    public Passivo passivo() { return passivo; }
    public List<Habilidade> habilidades() { return habilidades; }
    public List<Tarefa> tarefas() { return tarefas; }
    public Prova prova() { return prova; }
    public EntityType guardiao() { return guardiao; }
    public List<EntityType> monstros() { return monstros; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public boolean avancada() { return tier != Tier.BASICA; }

    public static Classe porId(String id) {
        for (Classe c : values()) if (c.id().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) return c;
        return null;
    }
}
