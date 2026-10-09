package br.rpgatributos.colonia;

import br.rpgatributos.fazenda.Cozinha;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.vida.Bebida;
import br.rpgatributos.vida.Coleta;
import br.rpgatributos.vida.Fruta;
import br.rpgatributos.vida.Mel;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Pedidos pessoais: de vez em quando um morador pede alguma coisa (uns pedidos servem para
 * qualquer um, outros são da profissão dele). Entregar deixa a colônia mais feliz e o morador
 * contente por um tempo; deixar passar o prazo deixa ele chateado.
 */
public enum Pedido {
    PRATO("um prato da Cozinha", 1, Material.RABBIT_STEW, "Cansei de comer pão seco.", Set.of(),
            s -> s.getPersistentDataContainer().has(Cozinha.CHAVE_PRATO)),
    FLORES("3 flores", 3, Material.POPPY, "Quero enfeitar a janela.", Set.of(),
            s -> Tag.SMALL_FLOWERS.isTagged(s.getType())),
    FRUTAS("2 frutas do pomar", 2, Material.APPLE, "Estou com saudade de fruta fresca.", Set.of(),
            s -> Fruta.de(s) != null),
    COLETA("1 coisa colhida pelo mundo (cogumelo, amora, castanha...)", 1, Material.SWEET_BERRIES,
            "Dizem que o bosque está cheio de coisas boas nesta estação.", Set.of(), s -> Coleta.de(s) != null),
    MEL("1 pote de mel", 1, Material.HONEY_BOTTLE, "Um docinho faria bem.", Set.of(),
            s -> s.getType() == Material.HONEY_BOTTLE || Mel.de(s) != null),
    BEBIDA("1 bebida do barril", 1, Material.POTION, "Quero comemorar com os vizinhos.", Set.of(),
            s -> Bebida.de(s) != null),
    LANTERNA("1 lanterna", 1, Material.LANTERN, "Minha casa fica escura demais à noite.", Set.of(),
            s -> s.getType() == Material.LANTERN || s.getType() == Material.SOUL_LANTERN),
    LIVRO("1 livro", 1, Material.BOOK, "Preciso de algo para ler antes de dormir.", Set.of(),
            s -> s.getType() == Material.BOOK || s.getType() == Material.WRITTEN_BOOK || s.getType() == Material.WRITABLE_BOOK),

    ENXADA("1 enxada de ferro (ou melhor)", 1, Material.IRON_HOE, "Minha enxada está em pedaços.", Set.of(Profissao.FAZENDEIRO),
            s -> melhorQueFerro(s, "_HOE")),
    MACHADO("1 machado de ferro (ou melhor)", 1, Material.IRON_AXE, "O cabo do meu machado rachou.", Set.of(Profissao.LENHADOR),
            s -> melhorQueFerro(s, "_AXE")),
    PICARETA("1 picareta de ferro (ou melhor)", 1, Material.IRON_PICKAXE, "Gastei a picareta numa veia de pedra dura.",
            Set.of(Profissao.MINERADOR), s -> melhorQueFerro(s, "_PICKAXE")),
    VARA("1 vara de pescar", 1, Material.FISHING_ROD, "Um bagre levou minha vara para o fundo do rio.", Set.of(Profissao.PESCADOR),
            s -> s.getType() == Material.FISHING_ROD),
    PEIXE_RARO("1 peixe raro", 1, Material.TROPICAL_FISH, "Quero testar uma receita nova.", Set.of(Profissao.COZINHEIRO),
            s -> PeixeRaro.de(s) != null),
    CARVAO("16 carvões", 16, Material.COAL, "A forja não acende sozinha.", Set.of(Profissao.FERREIRO),
            s -> s.getType() == Material.COAL || s.getType() == Material.CHARCOAL),
    LACO("1 laço", 1, Material.LEAD, "Um bezerro fugiu de novo.", Set.of(Profissao.TRATADOR),
            s -> s.getType() == Material.LEAD),
    GLOWSTONE("4 pós de pedra luminosa", 4, Material.GLOWSTONE_DUST, "Meus frascos não brilham mais.", Set.of(Profissao.ALQUIMISTA),
            s -> s.getType() == Material.GLOWSTONE_DUST),
    MAPA("1 mapa", 1, Material.MAP, "Quero conhecer as estradas do reino.", Set.of(Profissao.MERCADOR),
            s -> s.getType() == Material.MAP || s.getType() == Material.FILLED_MAP),
    TINTA("3 bolsas de tinta", 3, Material.INK_SAC, "Minha tinta acabou no meio de um capítulo.", Set.of(Profissao.BIBLIOTECARIO),
            s -> s.getType() == Material.INK_SAC || s.getType() == Material.GLOW_INK_SAC),
    TIJOLOS("8 blocos de tijolo", 8, Material.BRICKS, "Quero uma lareira de tijolos na minha casa.", Set.of(Profissao.CONSTRUTOR),
            s -> s.getType() == Material.BRICKS),
    ESCUDO("1 escudo", 1, Material.SHIELD, "Meu escudo velho já não segura nada.", Set.of(Profissao.SOLDADO, Profissao.CAVALEIRO),
            s -> s.getType() == Material.SHIELD),
    FLECHAS("16 flechas", 16, Material.ARROW, "A aljava está vazia.", Set.of(Profissao.ARQUEIRO),
            s -> s.getType() == Material.ARROW),
    MACAS("4 maçãs", 4, Material.APPLE, "São para o meu cavalo.", Set.of(Profissao.CAVALEIRO),
            s -> s.getType() == Material.APPLE);

    /** Turnos (minutos com a colônia carregada) para atender. */
    public static final int PRAZO = 30;
    /** Turnos de contente (entregue) ou chateado (esquecido). */
    public static final int CONTENTE = 20, CHATEADO = 10;

    private final String texto;
    private final int qtd;
    private final Material icone;
    private final String motivo;
    private final Set<Profissao> profissoes;
    private final Predicate<ItemStack> teste;

    Pedido(String texto, int qtd, Material icone, String motivo, Set<Profissao> profissoes, Predicate<ItemStack> teste) {
        this.texto = texto;
        this.qtd = qtd;
        this.icone = icone;
        this.motivo = motivo;
        this.profissoes = profissoes;
        this.teste = teste;
    }

    public String texto() { return texto; }
    public int qtd() { return qtd; }
    public Material icone() { return icone; }
    public String motivo() { return motivo; }
    public boolean serve(ItemStack s) { return s != null && !s.isEmpty() && teste.test(s); }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    private static boolean melhorQueFerro(ItemStack s, String fim) {
        String n = s.getType().name();
        return n.endsWith(fim) && (n.startsWith("IRON_") || n.startsWith("DIAMOND_") || n.startsWith("NETHERITE_"));
    }

    /** Um pedido para esse morador: metade das vezes um da profissão dele (se houver). */
    public static Pedido sortear(Profissao p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<Pedido> geral = new ArrayList<>(), dele = new ArrayList<>();
        for (Pedido pe : values()) {
            if (pe.profissoes.isEmpty()) geral.add(pe);
            else if (pe.profissoes.contains(p)) dele.add(pe);
        }
        List<Pedido> l = !dele.isEmpty() && r.nextBoolean() ? dele : geral;
        return l.get(r.nextInt(l.size()));
    }

    public static Pedido porId(String id) {
        for (Pedido p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
