package br.rpgatributos.raca;

import br.rpgatributos.Skill;
import br.rpgatributos.classe.Classe;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Set;

/**
 * As raças (2.52). Cada uma tem um tamanho, uma fraqueza e cinco estágios de Despertar: começa
 * no I e sobe juntando Essência (o XP das atividades da raça) e fazendo a oferenda do estágio.
 * Os números de cada estágio ficam no {@link Racas}; aqui só o texto que o jogador lê.
 */
public enum Raca {
    HUMANO("Humano", "☺", 0xE0C9A6, Material.BOOK, 1.0,
            "Versáteis e teimosos: aprendem de tudo um pouco mais rápido.",
            "Nenhuma, mas também nenhum dom especial antes do fim.",
            Set.of(),
            new String[]{"Adaptável", "Persistente", "Engenhoso", "Resiliente", "Herança"},
            new String[]{"+10% de XP em tudo", "+10 de Vigor máximo", "+20% de XP em tudo", "+2 ❤ de vida máxima",
                    "Escolha um dom de outra raça"},
            new Material[]{null, Material.BOOK, Material.EMERALD, Material.GOLDEN_APPLE, Material.TOTEM_OF_UNDYING},
            new int[]{0, 8, 16, 2, 1}),
    ANAO("Anão", "⚒", 0xC1793A, Material.IRON_PICKAXE, 0.65,
            "Baixos, largos e teimosos como a rocha. Nasceram debaixo da montanha.",
            "8% mais lentos.",
            Set.of(Skill.MINERACAO, Skill.FERRARIA),
            new String[]{"Robusto", "Olhos da Pedra", "Mão de Ferro", "Sangue da Montanha", "Coração da Forja"},
            new String[]{"+2 ❤ e minera 10% mais rápido", "Enxerga no escuro nas cavernas", "Minera 25% mais rápido e quase não é empurrado",
                    "+2 ❤ e 8% de chance de minério dobrado", "Pressa nas cavernas e 20% de minério dobrado"},
            new Material[]{null, Material.COAL, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND},
            new int[]{0, 32, 16, 8, 4}),
    ELFO("Elfo", "❦", 0x8BC34A, Material.BOW, 1.1,
            "Altos e leves, de olhos atentos. Os bosques são a casa deles.",
            "-2 ❤ de vida máxima.",
            Set.of(Skill.MADEIRA, Skill.CORRIDA, Skill.ARCANO),
            new String[]{"Passo Leve", "Olho de Águia", "Sentidos Élficos", "Salto Gracioso", "Filho da Floresta"},
            new String[]{"6% mais rápido", "+15% de dano com flechas", "Agache 2 s parado: os monstros por perto brilham",
                    "Pula mais alto, cai melhor e fica 12% mais rápido", "Recupera vida nas florestas e +30% com flechas"},
            new Material[]{null, Material.ARROW, Material.GLOW_BERRIES, Material.AMETHYST_SHARD, Material.GOLDEN_APPLE},
            new int[]{0, 16, 16, 8, 2}),
    PEQUENINO("Pequenino", "✿", 0xFFB74D, Material.COOKIE, 0.5,
            "Metade do tamanho, o dobro da sorte. Gostam de comer bem e de não ser vistos.",
            "-4 ❤ de vida máxima.",
            Set.of(Skill.AGRICULTURA, Skill.CULINARIA, Skill.PESCA),
            new String[]{"Sortudo", "Barriga Feliz", "Despercebido", "Pés Ligeiros", "Sorte Grande"},
            new String[]{"+1 de sorte e anda agachado mais rápido", "Comer enche +2 de fome a mais",
                    "Agachado, os monstros longe não notam você", "10% mais rápido e +3 de sorte", "12% de chance de desviar de um golpe"},
            new Material[]{null, Material.BREAD, Material.HONEY_BOTTLE, Material.PUMPKIN_PIE, Material.GOLDEN_CARROT},
            new int[]{0, 16, 8, 8, 16}),
    PASSARO("Povo-Pássaro", "🪶", 0x90CAF9, Material.FEATHER, 0.9,
            "Ossos ocos e penas nos braços. No começo só caem devagar; com o tempo, voam.",
            "-2 ❤ e a armadura pesada no peito (ferro, ouro, diamante, netherite) prende as asas.",
            Set.of(Skill.PULO, Skill.CORRIDA),
            new String[]{"Ossos Leves", "Planar", "Asas", "Correntes de Ar", "Voo"},
            new String[]{"Sem dano de queda até 8 blocos e cai mais devagar", "Pule no ar para planar",
                    "As asas aparecem: pule planando para bater as asas (Vigor)", "Plana mais rápido e a batida custa menos",
                    "Segure o pulo planando para voar (gasta Vigor)"},
            new Material[]{null, Material.FEATHER, Material.PHANTOM_MEMBRANE, Material.BREEZE_ROD, Material.ELYTRA},
            new int[]{0, 16, 4, 4, 1}),
    MAR("Povo do Mar", "≈", 0x26C6DA, Material.PRISMARINE_SHARD, 1.0,
            "Guelras no pescoço e escamas nos braços. Na água, ninguém os alcança.",
            "Fica fraco no Nether e nos desertos (a não ser na chuva ou na água).",
            Set.of(Skill.NATACAO, Skill.PESCA),
            new String[]{"Guelras", "Olhos do Fundo", "Nadador", "Filho da Maré", "Fúria das Ondas"},
            new String[]{"Fôlego maior e nada mais rápido", "Enxerga no escuro debaixo d'água",
                    "Graça do Golfinho e minera normal debaixo d'água", "Respira debaixo d'água", "+25% de dano na água ou na chuva"},
            new Material[]{null, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS, Material.NAUTILUS_SHELL, Material.HEART_OF_THE_SEA},
            new int[]{0, 8, 8, 1, 1}),
    // ---------- 2.53: a segunda onda ----------
    DRACONATO("Draconato", "✹", 0xE65100, Material.BLAZE_POWDER, 1.05,
            "Sangue de dragão, escamas no rosto e chifres na testa. O fogo faz parte deles.",
            "O frio pesa: em neve, gelo e biomas gelados ficam lentos e fracos.",
            Set.of(Skill.COMBATE, Skill.FERRARIA, Skill.ARCANO),
            new String[]{"Escamas", "Sopro de Fogo", "Asas Curtas", "Coração Ígneo", "Fúria do Dragão"},
            new String[]{"Leva metade do dano de fogo e de lava", "Agache duas vezes rápido: sopra fogo na sua frente (gasta Vigor)",
                    "Asas pequenas: pule no ar para planar", "+2 ❤ e imune a fogo e lava", "O sopro vai mais longe e seus golpes incendeiam"},
            new Material[]{null, Material.BLAZE_POWDER, Material.PHANTOM_MEMBRANE, Material.MAGMA_CREAM, Material.DRAGON_BREATH},
            new int[]{0, 16, 4, 8, 1}),
    FERA("Homem-Fera", "☾", 0x8D6E63, Material.BONE, 1.05,
            "Meio gente, meio lobo. A lua chama e eles respondem. Primos da licantropia, mas no controle.",
            "Armas de ouro ferem 25% mais.",
            Set.of(Skill.COMBATE, Skill.CORRIDA, Skill.DOMA),
            new String[]{"Faro", "Garras", "Uivo", "Sangue Selvagem", "Senhor da Lua"},
            new String[]{"Enxerga no escuro à noite e é 4% mais rápido", "De mãos vazias, bate como uma espada (+5 de dano)",
                    "Agache duas vezes rápido: uivo que dá Força a você e à party e espanta os monstros perto",
                    "À noite regenera e fica mais rápido", "Imune à licantropia; à noite ganha Força (Força II na lua cheia)"},
            new Material[]{null, Material.BONE, Material.RABBIT_FOOT, Material.COOKED_BEEF, Material.GOLDEN_APPLE},
            new int[]{0, 32, 4, 32, 8}),
    ESPECTRAL("Espectral", "✧", 0x90A4AE, Material.PHANTOM_MEMBRANE, 1.0,
            "Metade aqui, metade do outro lado. Passos sem som e um frio no ar por onde passam.",
            "De dia, ao céu aberto, o sol enfraquece (Fraqueza).",
            Set.of(Skill.ARCANO, Skill.ALQUIMIA, Skill.PULO),
            new String[]{"Passo Etéreo", "Pacto dos Mortos", "Desvanecer", "Toque Gélido", "Além do Véu"},
            new String[]{"Cai devagar (sem dano até 6 blocos) e enxerga no escuro à noite",
                    "Zumbis, esqueletos e afins só notam você bem de perto",
                    "Agache duas vezes rápido: fica invisível por 8 s (a cada 45 s)",
                    "Seus golpes deixam lento e roubam um pouco de vida",
                    "Uma vez por hora, ao cair morto, volta com metade da vida"},
            new Material[]{null, Material.PHANTOM_MEMBRANE, Material.GHAST_TEAR, Material.ECHO_SHARD, Material.TOTEM_OF_UNDYING},
            new int[]{0, 8, 2, 4, 1}),
    GNOMO("Gnomo Inventor", "⚙", 0x26A69A, Material.CLOCK, 0.6,
            "Pequenos, curiosos e cheios de bugigangas. Se não funciona, consertam; se funciona, melhoram.",
            "-4 ❤ de vida máxima.",
            Set.of(Skill.FERRARIA, Skill.ALQUIMIA, Skill.MINERACAO),
            new String[]{"Remendos", "Olho Técnico", "Pólvora", "Autômato", "Grande Invento"},
            new String[]{"A ferramenta ou arma na mão se conserta um pouco a cada 30 s", "15% de chance de não gastar durabilidade",
                    "TNT, foguetes e frascos de arremesso fazem +30% de dano e não ferem você",
                    "Agache duas vezes rápido: um autômato de ferro luta ao seu lado por 40 s (a cada 2 min)",
                    "+10% de chance de a Forja subir uma raridade"},
            new Material[]{null, Material.REDSTONE, Material.GUNPOWDER, Material.IRON_BLOCK, Material.NETHERITE_INGOT},
            new int[]{0, 32, 16, 4, 1});

    /** Essência total para chegar em cada estágio (I, II, III, IV, V). */
    public static final double[] ESSENCIA = {0, 1000, 4000, 12000, 30000};
    public static final String[] ROMANO = {"I", "II", "III", "IV", "V"};

    private final String nome, icone, descricao, fraqueza;
    private final TextColor cor;
    private final Material item;
    private final double escala;
    private final Set<Skill> afinidades;
    private final String[] estagios, efeitos;
    private final Material[] oferenda;
    private final int[] quantidade;

    Raca(String nome, String icone, int cor, Material item, double escala, String descricao, String fraqueza, Set<Skill> afinidades,
         String[] estagios, String[] efeitos, Material[] oferenda, int[] quantidade) {
        this.nome = nome;
        this.icone = icone;
        this.cor = TextColor.color(cor);
        this.item = item;
        this.escala = escala;
        this.descricao = descricao;
        this.fraqueza = fraqueza;
        this.afinidades = afinidades;
        this.estagios = estagios;
        this.efeitos = efeitos;
        this.oferenda = oferenda;
        this.quantidade = quantidade;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String nome() { return nome; }
    public String icone() { return icone; }
    public TextColor cor() { return cor; }
    public Material item() { return item; }
    public double escala() { return escala; }
    public String descricao() { return descricao; }
    public String fraqueza() { return fraqueza; }
    public Set<Skill> afinidades() { return afinidades; }

    /** Nome do estágio (1 a 5). */
    public String estagio(int e) { return estagios[e - 1]; }
    /** O que o estágio dá (1 a 5). */
    public String efeito(int e) { return efeitos[e - 1]; }
    /** A oferenda para despertar o estágio (2 a 5). */
    public Material oferenda(int e) { return oferenda[e - 1]; }
    public int quantidade(int e) { return quantidade[e - 1]; }

    /** Quanto do XP de um atributo vira Essência: tudo nos da raça, um quinto nos outros (humanos: metade em tudo). */
    public double fatorEssencia(Skill s) {
        if (this == HUMANO) return 0.5;
        return afinidades.contains(s) ? 1.0 : 0.2;
    }

    /** 2.53: as classes que combinam com a raça (com uma delas ativa: +6% de dano e -5% de dano recebido). */
    public Set<Classe> sinergia() {
        return switch (this) {
            case HUMANO -> Set.of(Classe.GUERREIRO, Classe.CAVALEIRO, Classe.PALADINO, Classe.SANTO_PALADINO);
            case ANAO -> Set.of(Classe.FERREIRO, Classe.MESTRE_FORJADOR, Classe.HERDEIRO_DO_FERREIRO, Classe.BERSERKER);
            case ELFO -> Set.of(Classe.ARQUEIRO, Classe.CACADOR, Classe.ATIRADOR, Classe.ARQUEIRO_CELESTIAL);
            case PEQUENINO -> Set.of(Classe.LADINO, Classe.ASSASSINO, Classe.LAMINA_FANTASMA);
            case PASSARO -> Set.of(Classe.ARQUEIRO, Classe.ATIRADOR, Classe.ARQUEIRO_CELESTIAL);
            case MAR -> Set.of(Classe.ELEMENTALISTA, Classe.DOMADOR, Classe.MESTRE_DAS_FERAS);
            case DRACONATO -> Set.of(Classe.BERSERKER, Classe.ELEMENTALISTA, Classe.SENHOR_DA_GUERRA);
            case FERA -> Set.of(Classe.DOMADOR, Classe.MESTRE_DAS_FERAS, Classe.BERSERKER, Classe.SENHOR_DAS_FERAS_ANCESTRAIS);
            case ESPECTRAL -> Set.of(Classe.MAGO, Classe.FEITICEIRO, Classe.ASSASSINO, Classe.LAMINA_FANTASMA, Classe.SOBERANO_DAS_SOMBRAS);
            case GNOMO -> Set.of(Classe.FERREIRO, Classe.MESTRE_FORJADOR, Classe.MAGO, Classe.ARQUIMAGO_PRIMORDIAL);
        };
    }

    public static Raca porId(String s) {
        if (s == null) return null;
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace("-", "_").replace(" ", "_");
        for (Raca r : values()) {
            if (r.name().equalsIgnoreCase(n)) return r;
        }
        return switch (n.toLowerCase(Locale.ROOT)) {
            case "povo_passaro", "passaro", "ave" -> PASSARO;
            case "povo_do_mar", "mar", "tritao" -> MAR;
            case "anao" -> ANAO;
            case "homem_fera", "fera", "lobo" -> FERA;
            case "gnomo", "gnomo_inventor" -> GNOMO;
            case "dragao", "draconato" -> DRACONATO;
            default -> null;
        };
    }
}
