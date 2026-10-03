package br.rpgatributos.fazenda;

import br.rpgatributos.pesca.PeixeRaro;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

/**
 * Receitas da Cozinha. Liberadas pelo nível de Culinária (em fração do nível máximo).
 * As variedades mutantes só entram nas receitas mais fortes.
 */
public enum Prato {
    PAO_CASEIRO("Pão Caseiro", Material.BREAD, 0, 10, 4,
            List.of(m(Material.WHEAT, 3), m(Material.EGG, 1)),
            List.of(e(PotionEffectType.HASTE, 120, 0)), null),
    ENSOPADO_LEGUMES("Ensopado de Legumes", Material.MUSHROOM_STEW, 0, 12, 6,
            List.of(m(Material.CARROT, 2), m(Material.POTATO, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 45, 0)), null),
    TORTA_ABOBORA("Torta de Abóbora da Vovó", Material.PUMPKIN_PIE, 0.1, 15, 4,
            List.of(m(Material.PUMPKIN, 1), m(Material.SUGAR, 1), m(Material.EGG, 1), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.LUCK, 300, 0), e(PotionEffectType.SPEED, 180, 0)), null),
    PEIXE_ERVAS("Salmão com Ervas", Material.COOKED_SALMON, 0.15, 15, 4,
            List.of(m(Material.SALMON, 1), m(Material.KELP, 2), m(Material.SWEET_BERRIES, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 300, 0), e(PotionEffectType.DOLPHINS_GRACE, 60, 0)), null),
    BATATA_RECHEADA("Batata Recheada", Material.BAKED_POTATO, 0.2, 20, 6,
            List.of(m(Material.POTATO, 2), m(Material.COOKED_PORKCHOP, 1), m(Material.BROWN_MUSHROOM, 1)),
            List.of(e(PotionEffectType.ABSORPTION, 120, 0), e(PotionEffectType.RESISTANCE, 60, 0)), null),
    BISCOITO_MEL("Biscoito de Mel", Material.COOKIE, 0.25, 20, 2,
            List.of(m(Material.WHEAT, 2), m(Material.HONEY_BOTTLE, 1), m(Material.COCOA_BEANS, 1)),
            List.of(e(PotionEffectType.HASTE, 180, 1)), null),
    ENSOPADO_CACADOR("Ensopado do Caçador", Material.RABBIT_STEW, 0.3, 25, 8,
            List.of(m(Material.COOKED_BEEF, 1), m(Material.CARROT, 1), m(Material.POTATO, 1),
                    m(Material.RED_MUSHROOM, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0)), null),
    SALADA_ARCANA("Salada Arcana", Material.BEETROOT_SOUP, 0.4, 30, 6,
            List.of(m(Material.BEETROOT, 2), m(Material.GLOW_BERRIES, 1), m(Material.CHORUS_FRUIT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 300, 0)), Especial.MANA),
    BANQUETE_GUERREIRO("Banquete do Guerreiro", Material.COOKED_BEEF, 0.5, 40, 10,
            List.of(m(Material.COOKED_BEEF, 2), m(Material.GOLDEN_CARROT, 1), v(Variedade.TRIGO_DOURADO, 1)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0), e(PotionEffectType.RESISTANCE, 180, 0),
                    e(PotionEffectType.REGENERATION, 180, 0)), null),
    PAO_DOURADO("Pão Dourado", Material.BREAD, 0.6, 45, 12,
            List.of(v(Variedade.TRIGO_DOURADO, 3)),
            List.of(e(PotionEffectType.ABSORPTION, 180, 1), e(PotionEffectType.SATURATION, 1, 4)), null),
    SOPA_CRISTALINA("Sopa Cristalina", Material.MUSHROOM_STEW, 0.7, 50, 8,
            List.of(v(Variedade.CENOURA_CRISTALINA, 2), v(Variedade.BATATA_ANCESTRAL, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 300, 0), e(PotionEffectType.SPEED, 180, 1)), null),
    TORTA_RUBI("Torta Rubi", Material.PUMPKIN_PIE, 0.8, 60, 8,
            List.of(v(Variedade.BETERRABA_RUBI, 2), m(Material.SUGAR, 1), m(Material.EGG, 1)),
            List.of(e(PotionEffectType.HEALTH_BOOST, 300, 1), e(PotionEffectType.REGENERATION, 30, 1)), null),
    SOPA_PESCADOR("Sopa do Pescador", Material.BEETROOT_SOUP, 0.1, 15, 6,
            List.of(m(Material.COD, 2), m(Material.POTATO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 300, 0), e(PotionEffectType.LUCK, 300, 0)), null),
    SUSHI_REAL("Sushi Real", Material.COOKED_SALMON, 0.45, 35, 8,
            List.of(p(PeixeRaro.SALMAO_REI, 1), m(Material.KELP, 2), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.DOLPHINS_GRACE, 120, 0), e(PotionEffectType.CONDUIT_POWER, 300, 0),
                    e(PotionEffectType.HASTE, 300, 0)), null),
    CALDEIRADA_ABISSAL("Caldeirada Abissal", Material.RABBIT_STEW, 0.75, 60, 10,
            List.of(p(PeixeRaro.PEIXE_ABISSAL, 1), m(Material.INK_SAC, 1), v(Variedade.BATATA_ANCESTRAL, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.RESISTANCE, 240, 0), e(PotionEffectType.NIGHT_VISION, 300, 0),
                    e(PotionEffectType.STRENGTH, 180, 0)), null),
    BANQUETE_LENDARIO("Banquete Lendário", Material.SUSPICIOUS_STEW, 0.9, 100, 12,
            List.of(v(Variedade.TRIGO_DOURADO, 1), v(Variedade.CENOURA_CRISTALINA, 1), v(Variedade.BATATA_ANCESTRAL, 1),
                    v(Variedade.BETERRABA_RUBI, 1), m(Material.GOLDEN_APPLE, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.REGENERATION, 300, 0), e(PotionEffectType.SPEED, 300, 0)), Especial.BANQUETE);

    /** Ingrediente: um material comum, uma variedade mutante OU um peixe raro. */
    public record Ingrediente(Material material, Variedade variedade, PeixeRaro peixe, int qtd) {}

    /** Efeito do prato: tipo, duração base em segundos e nível (0 = I). */
    public record Efeito(PotionEffectType tipo, int segundos, int nivel) {}

    /** Algo além dos efeitos de poção. */
    public enum Especial {
        /** Recupera 60 de mana. */
        MANA,
        /** Os efeitos valem para todos os jogadores a até 10 blocos. */
        BANQUETE
    }

    private final String nome;
    private final Material base;
    private final double nivel;
    private final double xp;
    private final float saturacao;
    private final List<Ingrediente> ingredientes;
    private final List<Efeito> efeitos;
    private final Especial especial;

    Prato(String nome, Material base, double nivel, double xp, float saturacao,
          List<Ingrediente> ingredientes, List<Efeito> efeitos, Especial especial) {
        this.nome = nome;
        this.base = base;
        this.nivel = nivel;
        this.xp = xp;
        this.saturacao = saturacao;
        this.ingredientes = ingredientes;
        this.efeitos = efeitos;
        this.especial = especial;
    }

    private static Ingrediente m(Material mat, int qtd) { return new Ingrediente(mat, null, null, qtd); }
    private static Ingrediente v(Variedade var, int qtd) { return new Ingrediente(null, var, null, qtd); }
    private static Ingrediente p(PeixeRaro peixe, int qtd) { return new Ingrediente(null, null, peixe, qtd); }
    private static Efeito e(PotionEffectType t, int seg, int nivel) { return new Efeito(t, seg, nivel); }

    public String nome() { return nome; }
    public Material base() { return base; }
    /** Nível de Culinária para liberar (ex.: 0,3 = 30% do nível máximo). */
    public int nivelNecessario(int nivelMaximo) { return (int) Math.ceil(nivel * nivelMaximo); }
    public double xp() { return xp; }
    public float saturacao() { return saturacao; }
    public List<Ingrediente> ingredientes() { return ingredientes; }
    public List<Efeito> efeitos() { return efeitos; }
    public Especial especial() { return especial; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Prato porId(String id) {
        for (Prato p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
