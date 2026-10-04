package br.rpgatributos.progressao;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Os talentos. Cada árvore tem 4 faixas: a faixa 2 pede 5 pontos gastos na árvore, a 3 pede 10
 * e o talento final pede 15. Cada ponto num talento é um "grau" (até o máximo dele).
 */
public enum Talento {
    // ---------- Guerreiro ----------
    FORCA_BRUTA(Arvore.GUERREIRO, 1, 5, "Força Bruta", Material.IRON_SWORD, "+2% de dano corpo a corpo por grau"),
    PELE_GROSSA(Arvore.GUERREIRO, 1, 5, "Pele Grossa", Material.LEATHER_CHESTPLATE, "+1 de armadura por grau"),
    VIGOR_DE_BATALHA(Arvore.GUERREIRO, 2, 3, "Vigor de Batalha", Material.GOLDEN_APPLE, "+15 de vigor máximo por grau"),
    SEDE_DE_SANGUE(Arvore.GUERREIRO, 2, 3, "Sede de Sangue", Material.REDSTONE, "Derrotar um inimigo cura ½ ❤ por grau"),
    GOLPE_CRITICO(Arvore.GUERREIRO, 3, 3, "Golpe Crítico", Material.FLINT, "4% de chance por grau de um golpe causar 50% a mais"),
    INABALAVEL(Arvore.GUERREIRO, 3, 2, "Inabalável", Material.SHIELD, "-4% de dano recebido e +10% de resistência a empurrões por grau"),
    FURIA_IMORTAL(Arvore.GUERREIRO, 4, 1, "Fúria Imortal", Material.BLAZE_POWDER,
            "Com menos de 25% de vida: +25% de dano e Regeneração II por 6 s (a cada 60 s)"),

    // ---------- Arcano ----------
    MENTE_AMPLA(Arvore.ARCANO, 1, 5, "Mente Ampla", Material.LAPIS_LAZULI, "+10 de mana máxima por grau"),
    FLUXO(Arvore.ARCANO, 1, 5, "Fluxo", Material.PRISMARINE_CRYSTALS, "+0,2 de mana por segundo por grau"),
    ECONOMIA_ARCANA(Arvore.ARCANO, 2, 3, "Economia Arcana", Material.GLOWSTONE_DUST, "Magias custam 4% menos mana por grau"),
    ECO_DE_MANA(Arvore.ARCANO, 2, 3, "Eco de Mana", Material.ECHO_SHARD, "6% de chance por grau de uma magia não gastar mana"),
    ESCUDO_DE_MANA(Arvore.ARCANO, 3, 2, "Escudo de Mana", Material.AMETHYST_CLUSTER,
            "10% do dano recebido por grau sai da mana em vez da vida"),
    ERUDITO(Arvore.ARCANO, 3, 3, "Erudito", Material.BOOK, "+2% de XP em todos os atributos por grau"),
    SOBRECARGA(Arvore.ARCANO, 4, 1, "Sobrecarga", Material.NETHER_STAR, "A cada 30 s, a próxima magia não gasta mana"),

    // ---------- Artesão ----------
    MAOS_DE_FERREIRO(Arvore.ARTESAO, 1, 5, "Mãos de Ferreiro", Material.SMITHING_TABLE,
            "+1% de chance por grau de forjar uma raridade acima"),
    COLHEITA_FARTA(Arvore.ARTESAO, 1, 5, "Colheita Farta", Material.WHEAT,
            "+4% de chance de minério, tronco e colheita em dobro por grau"),
    REFINADOR(Arvore.ARTESAO, 2, 3, "Refinador", Material.GRINDSTONE, "+2% de chance de refino por grau"),
    ALQUIMISTA_NATO(Arvore.ARTESAO, 2, 3, "Alquimista Nato", Material.BREWING_STAND,
            "+5% de chance por grau de a alquimia render o dobro"),
    MESTRE_CUCA(Arvore.ARTESAO, 3, 2, "Mestre-Cuca", Material.CAKE, "Pratos da Cozinha duram +15% por grau"),
    ENGENHOSO(Arvore.ARTESAO, 3, 3, "Engenhoso", Material.IRON_PICKAXE,
            "10% de chance por grau de um item não gastar durabilidade"),
    OBRA_PRIMA(Arvore.ARTESAO, 4, 1, "Obra-Prima", Material.NETHERITE_INGOT,
            "+5% de forjar uma raridade acima; quando sobe, 25% de chance de subir mais uma"),

    // ---------- Explorador ----------
    PASSOS_LEVES(Arvore.EXPLORADOR, 1, 5, "Passos Leves", Material.LEATHER_BOOTS, "+2% de velocidade por grau"),
    SORTE_DO_VIAJANTE(Arvore.EXPLORADOR, 1, 5, "Sorte do Viajante", Material.RABBIT_FOOT, "+0,4 de sorte por grau (baús, pesca)"),
    QUEDA_SUAVE(Arvore.EXPLORADOR, 2, 3, "Queda Suave", Material.FEATHER, "+2 blocos de queda sem dano por grau"),
    ESQUIVA_AGIL(Arvore.EXPLORADOR, 2, 3, "Esquiva Ágil", Material.PHANTOM_MEMBRANE,
            "Esquiva com 15% menos recarga e 3 de vigor a menos por grau"),
    CACADOR_DE_TESOUROS(Arvore.EXPLORADOR, 3, 2, "Caçador de Tesouros", Material.FILLED_MAP,
            "Mapas do tesouro caem 50% mais por grau"),
    GARIMPEIRO(Arvore.EXPLORADOR, 3, 3, "Garimpeiro", Material.DIAMOND_PICKAXE, "+15% de chance de Mitrilo Bruto em dobro por grau"),
    ANDARILHO_ETERNO(Arvore.EXPLORADOR, 4, 1, "Andarilho Eterno", Material.ELYTRA,
            "Fora de combate: Velocidade I e Pressa I o tempo todo");

    /** Pontos gastos na árvore para liberar cada faixa (1 a 4). */
    public static final int[] PONTOS_FAIXA = {0, 0, 5, 10, 15};

    private final Arvore arvore;
    private final int faixa, maximo;
    private final String nome, descricao;
    private final Material item;

    Talento(Arvore arvore, int faixa, int maximo, String nome, Material item, String descricao) {
        this.arvore = arvore;
        this.faixa = faixa;
        this.maximo = maximo;
        this.nome = nome;
        this.item = item;
        this.descricao = descricao;
    }

    public Arvore arvore() { return arvore; }
    public int faixa() { return faixa; }
    public int maximo() { return maximo; }
    public String nome() { return nome; }
    public Material item() { return item; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Talento porId(String id) {
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Quantos pontos cabem na árvore inteira. */
    public static int totalDaArvore(Arvore a) {
        int t = 0;
        for (Talento x : values()) if (x.arvore == a) t += x.maximo;
        return t;
    }
}
