package br.rpgatributos.alquimia;

import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.pesca.PeixeRaro;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static br.rpgatributos.alquimia.Ingrediente.g;
import static br.rpgatributos.alquimia.Ingrediente.m;
import static br.rpgatributos.alquimia.Ingrediente.p;
import static br.rpgatributos.alquimia.Ingrediente.peixeComum;
import static br.rpgatributos.alquimia.Ingrediente.r;
import static br.rpgatributos.alquimia.Ingrediente.raro;
import static br.rpgatributos.alquimia.Ingrediente.v;

/**
 * Uma receita da Bancada Alquímica. O resultado é um elixir, um componente (com quantidade),
 * uma gema (com grau) ou um acessório. O nível é uma fração do nível máximo de Alquimia.
 */
public record ReceitaAlquimica(Aba aba, Elixir elixir, Reagente reagente, int qtd, Gema gema, int grau, Acessorio acessorio,
                               List<Ingrediente> ingredientes, double nivel, double xp) {

    /** As abas do menu da Bancada. */
    public enum Aba { ELIXIRES, COMPONENTES, GEMAS, ACESSORIOS }

    public int nivelNecessario(int nivelMaximo) { return (int) Math.ceil(nivel * nivelMaximo); }

    /** Ícone do resultado (elixir em qualidade normal). */
    public ItemStack previa() {
        if (elixir != null) return elixir.criar(Qualidade.NORMAL, 1, "?");
        if (reagente != null) return reagente.criar(qtd);
        if (gema != null) return gema.criar(grau, 1);
        return acessorio.criar();
    }

    public String nome() {
        if (elixir != null) return elixir.nome();
        if (reagente != null) return (qtd > 1 ? qtd + "x " : "") + reagente.nome();
        if (gema != null) return gema.nome(grau);
        return acessorio.nome();
    }

    // =====================================================================
    //  Todas as receitas
    // =====================================================================

    private static ReceitaAlquimica elixir(Elixir e, double nivel, double xp, Ingrediente... ing) {
        List<Ingrediente> l = new ArrayList<>(List.of(ing));
        l.add(m(Material.GLASS_BOTTLE, 1));
        return new ReceitaAlquimica(Aba.ELIXIRES, e, null, 0, null, 0, null, l, nivel, xp);
    }

    private static ReceitaAlquimica comp(Reagente r, int qtd, double nivel, double xp, Ingrediente... ing) {
        return new ReceitaAlquimica(Aba.COMPONENTES, null, r, qtd, null, 0, null, List.of(ing), nivel, xp);
    }

    private static ReceitaAlquimica gema(Gema g, int grau, double nivel, double xp, Ingrediente... ing) {
        return new ReceitaAlquimica(Aba.GEMAS, null, null, 0, g, grau, null, List.of(ing), nivel, xp);
    }

    private static ReceitaAlquimica acess(Acessorio a, double nivel, double xp, Ingrediente... ing) {
        return new ReceitaAlquimica(Aba.ACESSORIOS, null, null, 0, null, 0, a, List.of(ing), nivel, xp);
    }

    public static final List<ReceitaAlquimica> TODAS = criarTodas();

    private static List<ReceitaAlquimica> criarTodas() {
        List<ReceitaAlquimica> l = new ArrayList<>();

        // ---------- componentes ----------
        l.add(comp(Reagente.PO_ARCANO, 2, 0, 6, m(Material.LAPIS_LAZULI, 2), m(Material.REDSTONE, 1), m(Material.GLOWSTONE_DUST, 1)));
        l.add(comp(Reagente.OLEO_DE_PEIXE, 2, 0, 6, peixeComum(3), m(Material.GLASS_BOTTLE, 1)));
        l.add(comp(Reagente.TINTURA_VITAL, 1, 0.1, 10, m(Material.GLISTERING_MELON_SLICE, 1), m(Material.BEETROOT, 2), r(Reagente.PO_ARCANO, 1)));
        l.add(comp(Reagente.TINTURA_VITAL, 3, 0.2, 18, v(Variedade.BETERRABA_RUBI, 1), m(Material.GLISTERING_MELON_SLICE, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(comp(Reagente.SOLVENTE, 2, 0.15, 10, r(Reagente.PO_ARCANO, 1), m(Material.SLIME_BALL, 1), m(Material.FERMENTED_SPIDER_EYE, 1)));
        l.add(comp(Reagente.SAL_LUNAR, 2, 0.2, 15, p(PeixeRaro.PEIXE_LUA, 1), m(Material.QUARTZ, 2), m(Material.BONE_MEAL, 1)));
        l.add(comp(Reagente.MERCURIO_VIVO, 2, 0.25, 18, p(PeixeRaro.ENGUIA_ELETRICA, 1), m(Material.IRON_INGOT, 2), m(Material.REDSTONE, 2)));
        l.add(comp(Reagente.CRISTAL_DE_MANA, 1, 0.3, 20, m(Material.AMETHYST_SHARD, 2), r(Reagente.PO_ARCANO, 2), m(Material.LAPIS_LAZULI, 2)));

        // ---------- óleos de lâmina ----------
        l.add(comp(Reagente.OLEO_DE_FOGO, 2, 0.05, 8, r(Reagente.OLEO_DE_PEIXE, 1), m(Material.BLAZE_POWDER, 1), m(Material.GLASS_BOTTLE, 1)));
        l.add(comp(Reagente.OLEO_GELIDO, 2, 0.1, 9, r(Reagente.OLEO_DE_PEIXE, 1), m(Material.PACKED_ICE, 1), m(Material.SNOWBALL, 2)));
        l.add(comp(Reagente.OLEO_GELIDO, 3, 0.15, 12, r(Reagente.OLEO_DE_PEIXE, 1), p(PeixeRaro.LUCIO_POLAR, 1)));
        l.add(comp(Reagente.CRISTAL_DE_MANA, 2, 0.3, 22, p(PeixeRaro.PEIXE_LANTERNA, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(comp(Reagente.OLEO_VENENOSO, 2, 0.1, 9, r(Reagente.OLEO_DE_PEIXE, 1), m(Material.SPIDER_EYE, 2), m(Material.POISONOUS_POTATO, 1)));
        l.add(comp(Reagente.OLEO_TROVEJANTE, 2, 0.25, 14, r(Reagente.OLEO_DE_PEIXE, 1), r(Reagente.MERCURIO_VIVO, 1), m(Material.COPPER_INGOT, 2)));
        l.add(comp(Reagente.OLEO_DE_PRATA, 2, 0.2, 12, r(Reagente.OLEO_DE_PEIXE, 1), m(Material.IRON_NUGGET, 6), m(Material.GLOWSTONE_DUST, 1)));

        // ---------- elixires ----------
        l.add(elixir(Elixir.CURA, 0, 10, r(Reagente.TINTURA_VITAL, 1)));
        l.add(elixir(Elixir.RAPIDEZ, 0.1, 15, p(PeixeRaro.TRUTA_ARCO_IRIS, 1), m(Material.SUGAR, 2), r(Reagente.PO_ARCANO, 1)));
        l.add(elixir(Elixir.MINERADOR, 0.15, 15, p(PeixeRaro.PEIXE_PEDRA, 1), m(Material.GLOWSTONE_DUST, 2), r(Reagente.PO_ARCANO, 1)));
        l.add(elixir(Elixir.MERGULHADOR, 0.15, 15, r(Reagente.OLEO_DE_PEIXE, 2), m(Material.PUFFERFISH, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(elixir(Elixir.SORTE, 0.25, 22, p(PeixeRaro.CARPA_DOURADA, 1), m(Material.GOLD_NUGGET, 4), r(Reagente.PO_ARCANO, 1)));
        l.add(elixir(Elixir.PEDRA, 0.3, 25, p(PeixeRaro.PEIXE_GELO, 1), m(Material.IRON_INGOT, 2), m(Material.MAGMA_CREAM, 1)));
        l.add(elixir(Elixir.SOMBRAS, 0.35, 28, p(PeixeRaro.PEIXE_FANTASMA, 1), m(Material.FERMENTED_SPIDER_EYE, 1), r(Reagente.SAL_LUNAR, 1)));
        l.add(elixir(Elixir.MANA, 0.4, 30, r(Reagente.CRISTAL_DE_MANA, 1), r(Reagente.SAL_LUNAR, 1)));
        l.add(elixir(Elixir.GIGANTE, 0.55, 45, p(PeixeRaro.BAIACU_REI, 1), m(Material.BLAZE_POWDER, 2), r(Reagente.TINTURA_VITAL, 1)));
        l.add(elixir(Elixir.FENIX, 0.7, 60, r(Reagente.PEROLA_NEGRA, 1), r(Reagente.TINTURA_VITAL, 2), m(Material.GHAST_TEAR, 1)));
        l.add(elixir(Elixir.PURIFICACAO, 0.3, 25, r(Reagente.TINTURA_VITAL, 2), r(Reagente.SAL_LUNAR, 1), m(Material.GOLDEN_CARROT, 1)));
        l.add(elixir(Elixir.TITA, 0.85, 100, p(PeixeRaro.PEIXE_ABISSAL, 1), r(Reagente.MERCURIO_VIVO, 1), r(Reagente.TINTURA_VITAL, 1),
                m(Material.GOLDEN_APPLE, 1)));

        // ---------- gemas (brutas, depois 3 de um grau = 1 do próximo) ----------
        l.add(gema(Gema.RUBI, 1, 0.1, 20, m(Material.REDSTONE, 8), r(Reagente.TINTURA_VITAL, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(gema(Gema.SAFIRA, 1, 0.15, 20, m(Material.LAPIS_LAZULI, 8), r(Reagente.SAL_LUNAR, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(gema(Gema.ESMERALDA, 1, 0.15, 20, m(Material.EMERALD, 2), r(Reagente.OLEO_DE_PEIXE, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(gema(Gema.TOPAZIO, 1, 0.2, 20, m(Material.GOLD_INGOT, 4), r(Reagente.MERCURIO_VIVO, 1)));
        l.add(gema(Gema.AMETISTA, 1, 0.2, 20, m(Material.AMETHYST_SHARD, 6), r(Reagente.CRISTAL_DE_MANA, 1)));
        l.add(gema(Gema.ONIX, 1, 0.25, 20, r(Reagente.PEROLA_NEGRA, 1), m(Material.OBSIDIAN, 4), r(Reagente.PO_ARCANO, 1)));
        for (Gema gm : Gema.values()) {
            l.add(gema(gm, 2, 0.4, 40, g(gm, 1, 3), r(Reagente.PO_ARCANO, 1), m(Material.DIAMOND, 1)));
        }
        for (Gema gm : Gema.values()) {
            l.add(gema(gm, 3, 0.7, 100, g(gm, 2, 3), raro(Raro.FRAGMENTO_DE_FORJA, 1), r(Reagente.CRISTAL_DE_MANA, 2)));
        }

        // ---------- acessórios ----------
        l.add(acess(Acessorio.RELOGIO_BOLSO, 0, 15, m(Material.CLOCK, 1), m(Material.COMPASS, 1), r(Reagente.PO_ARCANO, 1)));
        l.add(acess(Acessorio.LANTERNA_BOLSO, 0.05, 20, m(Material.LANTERN, 1), m(Material.GLOWSTONE_DUST, 2), r(Reagente.OLEO_DE_PEIXE, 1)));
        l.add(acess(Acessorio.CINTO_ATLETA, 0.15, 30, g(Gema.TOPAZIO, 1, 1), m(Material.LEATHER, 3), m(Material.RABBIT_FOOT, 1)));
        l.add(acess(Acessorio.ANEL_VIGOR, 0.2, 35, g(Gema.RUBI, 1, 1), m(Material.GOLD_INGOT, 2), r(Reagente.PO_ARCANO, 1)));
        l.add(acess(Acessorio.ANEL_VENTO, 0.2, 35, g(Gema.TOPAZIO, 1, 1), m(Material.GOLD_INGOT, 2), m(Material.FEATHER, 4)));
        l.add(acess(Acessorio.IMA_BOLSO, 0.25, 35, r(Reagente.MERCURIO_VIVO, 1), m(Material.IRON_INGOT, 4), m(Material.REDSTONE, 2)));
        l.add(acess(Acessorio.CAPA_PLANADORA, 0.2, 30, m(Material.PHANTOM_MEMBRANE, 4), m(Material.FEATHER, 6), m(Material.LEATHER, 2)));
        l.add(acess(Acessorio.ANEL_FORCA, 0.3, 40, g(Gema.ONIX, 1, 1), m(Material.IRON_INGOT, 2), r(Reagente.MERCURIO_VIVO, 1)));
        l.add(acess(Acessorio.ANEL_MARES, 0.3, 40, g(Gema.SAFIRA, 1, 1), r(Reagente.ESCAMA_DO_ABISMO, 1), m(Material.GOLD_INGOT, 2)));
        l.add(acess(Acessorio.ANEL_MINERADOR, 0.3, 40, g(Gema.AMETISTA, 1, 1), p(PeixeRaro.PEIXE_PEDRA, 1), m(Material.IRON_INGOT, 2)));
        l.add(acess(Acessorio.ANEL_BRASAS, 0.35, 45, g(Gema.RUBI, 1, 1), m(Material.MAGMA_CREAM, 2), m(Material.BLAZE_POWDER, 2)));
        l.add(acess(Acessorio.ANEL_FORTUNA, 0.4, 50, g(Gema.ESMERALDA, 1, 1), p(PeixeRaro.KOI_CELESTE, 1), m(Material.GOLD_INGOT, 2)));
        l.add(acess(Acessorio.CINTO_ANDARILHO, 0.4, 50, g(Gema.TOPAZIO, 2, 1), m(Material.LEATHER, 3), r(Reagente.MERCURIO_VIVO, 1)));
        l.add(acess(Acessorio.AMULETO_MANA, 0.45, 60, g(Gema.AMETISTA, 2, 1), r(Reagente.CRISTAL_DE_MANA, 2), m(Material.IRON_CHAIN, 1)));
        l.add(acess(Acessorio.AMULETO_GUARDIAO, 0.45, 60, g(Gema.SAFIRA, 2, 1), m(Material.IRON_BLOCK, 1), r(Reagente.MERCURIO_VIVO, 1)));
        l.add(acess(Acessorio.ANEL_SABIO, 0.5, 70, g(Gema.ESMERALDA, 2, 1), r(Reagente.CRISTAL_DE_MANA, 1), m(Material.GOLD_INGOT, 2)));
        l.add(acess(Acessorio.CINTO_TITA, 0.5, 70, g(Gema.ONIX, 2, 1), m(Material.LEATHER, 3), m(Material.IRON_BLOCK, 1)));
        l.add(acess(Acessorio.AMULETO_VIDA, 0.6, 90, g(Gema.RUBI, 2, 1), r(Reagente.TINTURA_VITAL, 2), m(Material.GOLDEN_APPLE, 1)));
        l.add(acess(Acessorio.AMULETO_FENIX, 0.85, 200, p(PeixeRaro.PEIXE_DRAGAO, 1), r(Reagente.PEROLA_NEGRA, 1), g(Gema.RUBI, 3, 1),
                m(Material.TOTEM_OF_UNDYING, 1)));
        return List.copyOf(l);
    }

    public static List<ReceitaAlquimica> daAba(Aba aba) {
        return TODAS.stream().filter(r -> r.aba() == aba).toList();
    }
}
