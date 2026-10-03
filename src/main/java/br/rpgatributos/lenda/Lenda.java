package br.rpgatributos.lenda;

import br.rpgatributos.forja.Categoria;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * As lendas: itens Especiais e únicos no servidor. Cada uma nasce dos feitos de quem a forja
 * (contadores de conquistas) e tem um poder próprio que desperta com o uso.
 */
public enum Lenda {
    LAMINA_DO_EXORCISTA("Lâmina do Exorcista", Material.NETHERITE_SWORD, Set.of(Categoria.ESPADA, Categoria.MACHADO),
            List.of(r("mortos_vivos", 1000, "Derrotar 1.000 mortos-vivos")),
            "+60% de dano em mortos-vivos, que pegam fogo; derrotar um cura 1 ❤.",
            "mortos_vivos", "derrotou %s mortos-vivos"),
    PRESA_DA_TECELA("Presa da Tecelã", Material.NETHERITE_SWORD, Set.of(Categoria.ESPADA, Categoria.LANCA),
            List.of(r("aranhas", 500, "Derrotar 500 aranhas"), r("chefe_rainha", 3, "Derrotar a Rainha Aracnídea 3 vezes")),
            "Golpes envenenam; 20% de chance de prender o alvo numa teia.",
            "aranhas", "derrotou %s aranhas"),
    MACHADO_DO_LENHADOR("Machado do Lenhador Eterno", Material.NETHERITE_AXE, Set.of(Categoria.MACHADO),
            List.of(r("troncos", 5000, "Cortar 5.000 troncos")),
            "Derruba a árvore inteira de uma vez (agachado corta só um tronco).",
            "troncos", "cortou %s troncos"),
    PICARETA_DO_ABISMO("Picareta do Abismo", Material.NETHERITE_PICKAXE, Set.of(Categoria.PICARETA),
            List.of(r("minerios", 3000, "Minerar 3.000 minérios")),
            "Agachado, minera o veio inteiro de minério (até 24 blocos).",
            "minerios", "minerou %s minérios"),
    ARCO_DA_TEMPESTADE("Arco da Tempestade", Material.BOW, Set.of(Categoria.ARCO, Categoria.BESTA),
            List.of(r("abates_flecha", 1500, "Derrotar 1.500 monstros com flechas"), r("chefe_tempestade", 1, "Derrotar a Tempestade Viva")),
            "25% das flechas chamam um raio no alvo.",
            "abates_flecha", "derrotou %s monstros com flechas"),
    COROA_DO_FIM("Coroa do Fim", Material.NETHERITE_HELMET, Set.of(Categoria.CAPACETE),
            List.of(r("chefe_arauto", 5, "Derrotar o Arauto do Fim 5 vezes")),
            "+25% de dano contra chefes; com pouca vida, ganha Resistência II (a cada 60 s).",
            "chefe_arauto", "derrotou o Arauto do Fim %s vezes"),
    EGIDE_DO_GUARDIAO("Égide do Guardião", Material.NETHERITE_CHESTPLATE, Set.of(Categoria.PEITORAL, Categoria.ESCUDO),
            List.of(r("masmorras", 10, "Vencer 10 masmorras"), r("ondas", 3, "Vencer as Ondas de Monstros 3 vezes")),
            "15% de chance de devolver metade do dano; ao apanhar, ganha Absorção (a cada 30 s).",
            "masmorras", "venceu %s masmorras"),
    BOTAS_DO_ANDARILHO("Botas do Andarilho", Material.NETHERITE_BOOTS, Set.of(Categoria.BOTAS),
            List.of(r(Lendas.CORRIDA, 100_000, "Correr 100.000 blocos")),
            "Velocidade permanente e nenhum dano de queda.",
            Lendas.CORRIDA, "correu %s blocos"),
    LAMINA_DO_CARRASCO("Lâmina do Carrasco", Material.NETHERITE_AXE, Set.of(Categoria.ESPADA, Categoria.MACHADO),
            List.of(r("abates_jogadores", 100, "Derrotar 100 jogadores")),
            "Inimigos abaixo de 20% de vida levam dano em dobro.",
            "abates_jogadores", "derrotou %s jogadores"),
    MARTELO_DO_FORJADOR("Martelo do Forjador Lendário", Material.MACE, Set.of(Categoria.MACA, Categoria.MACHADO),
            List.of(r("forjados", 500, "Forjar 500 itens"), r("forjado_mitico", 3, "Forjar 3 itens Míticos")),
            "10% de chance de trovão ao acertar; segurando, a forja tem +10% de sair uma raridade acima.",
            "forjados", "forjou %s itens"),
    LAMINA_DO_ARQUIMAGO("Lâmina do Arquimago", Material.NETHERITE_SWORD, Set.of(Categoria.ESPADA),
            List.of(r("magias", 5000, "Lançar 5.000 magias"), r("secretas", 30, "Descobrir as 30 magias secretas")),
            "Segurando, as magias custam 30% menos mana; golpes devolvem 3 de mana.",
            "magias", "lançou %s magias"),
    ELMO_DAS_FERAS("Elmo do Senhor das Feras", Material.NETHERITE_HELMET, Set.of(Categoria.CAPACETE),
            List.of(r("abates_companheiro", 1000, "Seus companheiros derrotam 1.000 monstros"), r("voadores", 1, "Dar Asas a um companheiro")),
            "Seus companheiros perto ganham Força e Regeneração.",
            "abates_companheiro", "venceu %s batalhas com suas feras");

    /** Um feito pedido: contador (de conquistas) e quantidade. */
    public record Requisito(String contador, int qtd, String texto) {}

    private final String nome;
    private final Material icone;
    private final Set<Categoria> categorias;
    private final List<Requisito> requisitos;
    private final String poder;
    private final String contadorHistoria;
    private final String historia;

    Lenda(String nome, Material icone, Set<Categoria> categorias, List<Requisito> requisitos, String poder,
          String contadorHistoria, String historia) {
        this.nome = nome;
        this.icone = icone;
        this.categorias = categorias;
        this.requisitos = requisitos;
        this.poder = poder;
        this.contadorHistoria = contadorHistoria;
        this.historia = historia;
    }

    private static Requisito r(String contador, int qtd, String texto) {
        return new Requisito(contador, qtd, texto);
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    /** Que tipo de item pode virar essa lenda. */
    public Set<Categoria> categorias() { return categorias; }
    public List<Requisito> requisitos() { return requisitos; }
    public String poder() { return poder; }
    /** Contador usado na frase da história (ex.: "derrotou 1.234 mortos-vivos"). */
    public String contadorHistoria() { return contadorHistoria; }
    public String historia() { return historia; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** O enigma que aparece no Livro das Lendas enquanto os feitos estão escondidos. */
    public String dica() {
        return switch (this) {
            case LAMINA_DO_EXORCISTA -> "Os mortos só descansam pelas mãos de quem já os fez descansar mil vezes.";
            case PRESA_DA_TECELA -> "Quem quebra a teia da rainha mais de uma vez herda o veneno dela.";
            case MACHADO_DO_LENHADOR -> "A floresta respeita quem já derrubou uma floresta inteira.";
            case PICARETA_DO_ABISMO -> "O abismo se abre para quem já arrancou milhares de tesouros da pedra.";
            case ARCO_DA_TEMPESTADE -> "A tempestade empresta o raio a quem a venceu e nunca erra de longe.";
            case COROA_DO_FIM -> "Só quem derrota o fim várias vezes pode se coroar com ele.";
            case EGIDE_DO_GUARDIAO -> "O escudo nasce de quem sobrevive onde ninguém deveria sobreviver.";
            case BOTAS_DO_ANDARILHO -> "O caminho reconhece quem já correu o mundo inteiro.";
            case LAMINA_DO_CARRASCO -> "A lâmina pesa com o sangue dos que caíram em duelo.";
            case MARTELO_DO_FORJADOR -> "O martelo escolhe quem já fez o metal cantar, e cantar os mitos.";
            case LAMINA_DO_ARQUIMAGO -> "O aço se curva para quem conhece todos os segredos da magia.";
            case ELMO_DAS_FERAS -> "As feras coroam quem lutou ao lado delas até o céu.";
        };
    }

    public static Lenda porId(String id) {
        for (Lenda l : values()) if (l.id().equalsIgnoreCase(id) || l.name().equalsIgnoreCase(id)) return l;
        return null;
    }
}
