package br.rpgatributos.combo;

import br.rpgatributos.classe.Classe;
import br.rpgatributos.lenda.Lenda;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Os golpes de combo. Cada arma tem 7: libera no nível de proficiência indicado, e o
 * jogador escolhe quais 4 ficam nas sequências. {@code vigor} é o custo e
 * {@code recarga} é em segundos.
 */
public enum Golpe {
    // ---------------- Espada ----------------
    ESTOCADA(TipoArma.ESPADA, 1, "Estocada", 15, 3, Material.IRON_SWORD,
            "Avança 6 blocos atravessando os inimigos no caminho."),
    CORTE_GIRATORIO(TipoArma.ESPADA, 3, "Corte Giratório", 20, 5, Material.GOLDEN_SWORD,
            "Gira a lâmina e acerta todos em volta, empurrando."),
    LAMINA_CRUZADA(TipoArma.ESPADA, 5, "Lâmina Cruzada", 20, 6, Material.SHEARS,
            "Corte em X na sua frente que faz os inimigos sangrarem."),
    EXECUCAO(TipoArma.ESPADA, 8, "Execução", 30, 10, Material.NETHERITE_SWORD,
            "Golpe pesado no alvo; quem estiver com menos de 30% de vida leva dano x4."),
    TEMPESTADE_DE_LAMINAS(TipoArma.ESPADA, 12, "Tempestade de Lâminas", 35, 12, Material.DIAMOND_SWORD,
            "Cinco giros seguidos em 2 segundos."),
    PASSO_FANTASMA(TipoArma.ESPADA, 16, "Passo Fantasma", 25, 8, Material.ECHO_SHARD,
            "Aparece nas costas do alvo e corta com dano dobrado."),
    JULGAMENTO_DA_LAMINA(TipoArma.ESPADA, 20, "Julgamento da Lâmina", 50, 25, Material.END_ROD,
            "Salta e crava uma lâmina de luz que corta uma linha de 9 blocos."),

    // ---------------- Machado ----------------
    SALTO_ESMAGADOR(TipoArma.MACHADO, 1, "Salto Esmagador", 20, 5, Material.IRON_AXE,
            "Salta para a frente e esmaga o chão onde cair, deixando os inimigos lentos."),
    ARREMESSO_DO_MACHADO(TipoArma.MACHADO, 3, "Arremesso do Machado", 15, 4, Material.GOLDEN_AXE,
            "Arremessa o machado girando; ele corta na ida e na volta."),
    RACHAR_ESCUDO(TipoArma.MACHADO, 5, "Rachar Escudo", 15, 6, Material.SHIELD,
            "Golpe que quebra a defesa: o alvo leva +25% de dano por 6 s e o escudo dele trava."),
    FURIA_DO_LENHADOR(TipoArma.MACHADO, 8, "Fúria do Lenhador", 30, 20, Material.BLAZE_POWDER,
            "Força e Pressa II por 8 s; cada golpe recupera vida."),
    REDEMOINHO_DE_ACO(TipoArma.MACHADO, 12, "Redemoinho de Aço", 35, 12, Material.DIAMOND_AXE,
            "Gira andando por 3 s, cortando tudo em volta."),
    DECAPITAR(TipoArma.MACHADO, 16, "Decapitar", 30, 10, Material.WITHER_SKELETON_SKULL,
            "Golpe enorme num alvo; se matar, devolve vigor e zera o Salto Esmagador."),
    CISAO_DA_TERRA(TipoArma.MACHADO, 20, "Cisão da Terra", 50, 25, Material.NETHERITE_AXE,
            "Racha o chão numa linha de 10 blocos e joga os inimigos para o alto."),

    // ---------------- Lança ----------------
    ESTOCADA_LONGA(TipoArma.LANCA, 1, "Estocada Longa", 15, 3, Material.IRON_SPEAR,
            "Perfura todos numa linha de 6 blocos."),
    VARREDURA(TipoArma.LANCA, 3, "Varredura", 20, 5, Material.STICK,
            "Arco de 180° na sua frente que empurra forte."),
    ARREMESSO_DA_LANCA(TipoArma.LANCA, 5, "Arremesso da Lança", 20, 6, Material.GOLDEN_SPEAR,
            "Arremessa uma lança espectral que atravessa tudo por 25 blocos."),
    SALTO_DO_DRAGAO(TipoArma.LANCA, 8, "Salto do Dragão", 30, 10, Material.DRAGON_BREATH,
            "Salta bem alto e mergulha onde você olha."),
    MURALHA_DE_LANCAS(TipoArma.LANCA, 12, "Muralha de Lanças", 30, 15, Material.POINTED_DRIPSTONE,
            "Por 5 s, quem chegar perto de você é ferido e empurrado."),
    PERFURACAO(TipoArma.LANCA, 16, "Perfuração", 25, 8, Material.DIAMOND_SPEAR,
            "Três estocadas rápidas em linha que quebram a defesa."),
    LANCA_CELESTE(TipoArma.LANCA, 20, "Lança Celeste", 50, 25, Material.NETHERITE_SPEAR,
            "Uma lança de luz cai do céu onde você olha."),

    // ---------------- Tridente ----------------
    ONDA(TipoArma.TRIDENTE, 1, "Onda", 15, 4, Material.WATER_BUCKET,
            "Uma onda na sua frente que fere e empurra."),
    PUXAO(TipoArma.TRIDENTE, 3, "Puxão", 15, 6, Material.FISHING_ROD,
            "Puxa o alvo que você olha até você."),
    REDEMOINHO(TipoArma.TRIDENTE, 5, "Redemoinho", 25, 10, Material.HEART_OF_THE_SEA,
            "Um redemoinho onde você olha que puxa e fere por 5 s (mais forte na chuva ou na água)."),
    TRIDENTE_TROVAO(TipoArma.TRIDENTE, 8, "Tridente do Trovão", 30, 12, Material.LIGHTNING_ROD,
            "Raio no alvo que salta para mais 2 inimigos."),
    MARE_ALTA(TipoArma.TRIDENTE, 12, "Maré Alta", 30, 15, Material.CONDUIT,
            "Você e a party nadam rápido, respiram e regeneram; inimigos perto ficam lentos."),
    MERGULHO(TipoArma.TRIDENTE, 16, "Mergulho", 25, 8, Material.PRISMARINE_SHARD,
            "Dispara para onde você olha (3x mais longe na água), ferindo quem atravessar."),
    FURIA_DE_POSEIDON(TipoArma.TRIDENTE, 20, "Fúria de Poseidon", 50, 25, Material.TRIDENT,
            "Tempestade de raios e ondas em volta de você por 3 s."),

    // ---------------- Maça ----------------
    ABALO_SISMICO(TipoArma.MACA, 1, "Abalo Sísmico", 20, 5, Material.MACE,
            "Bate no chão: fere e deixa lentos todos em volta."),
    MARTELO_ASCENDENTE(TipoArma.MACA, 3, "Martelo Ascendente", 20, 6, Material.PISTON,
            "Joga o alvo à sua frente para o alto."),
    ATORDOAR(TipoArma.MACA, 5, "Atordoar", 20, 8, Material.BELL,
            "Golpe em cone que atordoa por 2 s."),
    QUEBRA_ARMADURA(TipoArma.MACA, 8, "Quebra-Armadura", 25, 10, Material.CHAINMAIL_CHESTPLATE,
            "Golpe forte; o alvo leva +25% de dano por 8 s."),
    SALTO_METEORO(TipoArma.MACA, 12, "Salto Meteoro", 35, 12, Material.FIRE_CHARGE,
            "Salta alto e cai como um meteoro: quanto maior a queda, maior o dano."),
    ECO_DO_IMPACTO(TipoArma.MACA, 16, "Eco do Impacto", 30, 10, Material.SCULK_SHRIEKER,
            "Bate no chão e solta três ondas de choque cada vez maiores."),
    MARTELO_DOS_DEUSES(TipoArma.MACA, 20, "Martelo dos Deuses", 50, 25, Material.HEAVY_CORE,
            "Um martelo gigante cai do céu onde você olha."),

    // ---------------- Arco e besta ----------------
    TIRO_TRIPLO(TipoArma.ARCO, 1, "Tiro Triplo", 15, 3, Material.ARROW,
            "Três flechas em leque."),
    FLECHA_PERFURANTE(TipoArma.ARCO, 3, "Flecha Perfurante", 15, 5, Material.SPECTRAL_ARROW,
            "Flecha rapidíssima que atravessa vários inimigos."),
    SALTO_TATICO(TipoArma.ARCO, 5, "Salto Tático", 15, 6, Material.FEATHER,
            "Salta para trás e atira uma flecha para a frente."),
    FLECHA_EXPLOSIVA(TipoArma.ARCO, 8, "Flecha Explosiva", 25, 8, Material.TNT,
            "Flecha que explode ao acertar (sem quebrar blocos)."),
    SARAIVADA(TipoArma.ARCO, 12, "Saraivada", 30, 12, Material.TIPPED_ARROW,
            "Uma chuva de flechas cai onde você olha."),
    FLECHA_SOMBRIA(TipoArma.ARCO, 16, "Flecha Sombria", 25, 10, Material.ENDER_EYE,
            "Flecha que persegue o inimigo mais perto e o marca (+20% de dano)."),
    TIRO_CELESTIAL(TipoArma.ARCO, 20, "Tiro Celestial", 50, 25, Material.BEACON,
            "Carrega por 1 s e dispara um feixe de luz de 40 blocos que atravessa tudo."),

    // ---------------- Lendas (com a lenda na mão; armaduras e a picareta: carregando) ----------------
    PURIFICACAO_SAGRADA(Lenda.LAMINA_DO_EXORCISTA, "Purificação Sagrada", 40, 18, Material.TOTEM_OF_UNDYING,
            "Explosão de luz em volta: dano enorme em mortos-vivos e cura os aliados."),
    TEIA_DA_RAINHA(Lenda.PRESA_DA_TECELA, "Teia da Rainha", 35, 16, Material.COBWEB,
            "Cone de teias que prende e envenena os inimigos."),
    QUEDA_DA_FLORESTA(Lenda.MACHADO_DO_LENHADOR, "Queda da Floresta", 40, 18, Material.OAK_LOG,
            "Um tronco gigante cai numa linha de 9 blocos à sua frente."),
    COLAPSO_DO_ABISMO(Lenda.PICARETA_DO_ABISMO, "Colapso do Abismo", 40, 18, Material.DEEPSLATE,
            "O chão em volta afunda: puxa os inimigos para baixo e os prende."),
    TEMPESTADE_DE_RAIOS(Lenda.ARCO_DA_TEMPESTADE, "Tempestade de Raios", 45, 20, Material.LIGHTNING_ROD,
            "Seis raios caem onde você olha."),
    VAZIO_DO_FIM(Lenda.COROA_DO_FIM, "Vazio do Fim", 45, 20, Material.END_CRYSTAL,
            "Puxa todos os inimigos perto para um ponto e explode."),
    EGIDE(Lenda.EGIDE_DO_GUARDIAO, "Égide", 40, 25, Material.SHIELD,
            "Por 6 s: Absorção IV e quem te bater leva metade do dano de volta."),
    PASSO_DO_ANDARILHO(Lenda.BOTAS_DO_ANDARILHO, "Passo do Andarilho", 30, 12, Material.NETHERITE_BOOTS,
            "Avança 12 blocos atravessando inimigos e ganha Velocidade III."),
    SENTENCA(Lenda.LAMINA_DO_CARRASCO, "Sentença", 40, 20, Material.WITHER_ROSE,
            "Executa quem estiver com menos de 25% de vida (chefes levam dano x5)."),
    BIGORNA_CELESTE(Lenda.MARTELO_DO_FORJADOR, "Bigorna Celeste", 40, 18, Material.ANVIL,
            "Uma bigorna de luz cai no alvo e atordoa quem estiver perto."),
    LAMINA_ARCANA_SUPREMA(Lenda.LAMINA_DO_ARQUIMAGO, "Lâmina Arcana Suprema", 35, 16, Material.AMETHYST_CLUSTER,
            "Cinco projéteis arcanos perseguem os inimigos; devolve mana."),
    CHAMADO_DA_ALCATEIA(Lenda.ELMO_DAS_FERAS, "Chamado da Alcateia", 45, 30, Material.BONE,
            "Três lobos espectrais lutam ao seu lado por 20 s."),

    // ---------------- Classes (com a classe ativa, ou uma lendária que nasce dela) ----------------
    FURIA_SANGRENTA(Classe.BERSERKER, TipoArma.MACHADO, "Fúria Sangrenta", 30, 12, Material.REDSTONE,
            "Paga 4 de vida: giro devastador em volta que rouba vida."),
    CARGA_DO_CAVALEIRO(Classe.CAVALEIRO, TipoArma.LANCA, "Carga do Cavaleiro", 30, 10, Material.IRON_HORSE_ARMOR,
            "Investida de 10 blocos que arremessa os inimigos (o dobro montado)."),
    MARTELO_SAGRADO(Classe.PALADINO, TipoArma.MACA, "Martelo Sagrado", 35, 14, Material.GOLDEN_AXE,
            "Golpe de luz em área: mais forte em mortos-vivos e cura os aliados."),
    TIRO_NA_CABECA(Classe.ATIRADOR, TipoArma.ARCO, "Tiro na Cabeça", 30, 10, Material.TARGET,
            "Uma flecha rapidíssima com dano enorme."),
    FLECHAS_DA_MATILHA(Classe.CACADOR, TipoArma.ARCO, "Flechas da Matilha", 30, 12, Material.LEAD,
            "Três flechas que marcam o alvo; seus companheiros atacam ele."),
    PRISMA_ELEMENTAL(Classe.ELEMENTALISTA, TipoArma.TRIDENTE, "Prisma Elemental", 35, 12, Material.PRISMARINE_CRYSTALS,
            "Cone de fogo, gelo e raio ao mesmo tempo."),
    LAMINA_RUNICA(Classe.FEITICEIRO, TipoArma.ESPADA, "Lâmina Rúnica", 30, 10, Material.ENCHANTED_BOOK,
            "Três cortes arcanos em sequência que devolvem mana."),
    MIL_CORTES(Classe.ASSASSINO, TipoArma.ESPADA, "Mil Cortes", 35, 14, Material.IRON_SWORD,
            "Salta entre até 5 inimigos perto, cortando cada um."),
    MARTELO_INCANDESCENTE(Classe.MESTRE_FORJADOR, TipoArma.MACA, "Martelo Incandescente", 30, 10, Material.MAGMA_BLOCK,
            "Golpe em brasa: incendeia e quebra a defesa do alvo."),
    INVESTIDA_DA_FERA(Classe.MESTRE_DAS_FERAS, TipoArma.LANCA, "Investida da Fera", 30, 12, Material.SADDLE,
            "Seus companheiros saltam no alvo junto com a sua estocada.");

    private final TipoArma tipo;
    private final int nivel;
    private final String nome;
    private final double vigor;
    private final int recarga;
    private final Material icone;
    private final String descricao;
    private final Lenda lenda;
    private final Classe classe;

    Golpe(TipoArma tipo, int nivel, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(tipo, nivel, nome, vigor, recarga, icone, descricao, null, null);
    }

    /** Golpe de lenda: vale com qualquer arma de combo, se você tiver a lenda (veja {@link Combos#disponivel}). */
    Golpe(Lenda lenda, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(null, 1, nome, vigor, recarga, icone, descricao, lenda, null);
    }

    /** Golpe de classe: vale com a arma da classe enquanto ela (ou uma lendária que nasce dela) estiver ativa. */
    Golpe(Classe classe, TipoArma tipo, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(tipo, 1, nome, vigor, recarga, icone, descricao, null, classe);
    }

    Golpe(TipoArma tipo, int nivel, String nome, double vigor, int recarga, Material icone, String descricao, Lenda lenda, Classe classe) {
        this.tipo = tipo;
        this.nivel = nivel;
        this.nome = nome;
        this.vigor = vigor;
        this.recarga = recarga;
        this.icone = icone;
        this.descricao = descricao;
        this.lenda = lenda;
        this.classe = classe;
    }

    /** A arma do golpe (null nos golpes de lenda: valem com qualquer arma). */
    public TipoArma tipo() { return tipo; }
    public Lenda lenda() { return lenda; }
    public Classe classe() { return classe; }
    /** Golpe de lenda ou de classe (não sobe com a proficiência). */
    public boolean extra() { return lenda != null || classe != null; }
    /** Nível de proficiência da arma que libera o golpe. */
    public int nivel() { return nivel; }
    public String nome() { return nome; }
    public double vigor() { return vigor; }
    /** Recarga em segundos. */
    public int recarga() { return recarga; }
    public Material icone() { return icone; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Os 7 golpes normais da arma (os que sobem com a proficiência). */
    public static List<Golpe> da(TipoArma t) {
        List<Golpe> l = new ArrayList<>();
        for (Golpe g : values()) if (g.tipo == t && !g.extra()) l.add(g);
        return l;
    }

    /** Golpes de lenda e de classe que podem ir nas sequências dessa arma. */
    public static List<Golpe> extras(TipoArma t) {
        List<Golpe> l = new ArrayList<>();
        for (Golpe g : values()) if (g.extra() && (g.tipo == null || g.tipo == t)) l.add(g);
        return l;
    }

    public static Golpe porId(String id) {
        for (Golpe g : values()) if (g.id().equalsIgnoreCase(id)) return g;
        return null;
    }
}
