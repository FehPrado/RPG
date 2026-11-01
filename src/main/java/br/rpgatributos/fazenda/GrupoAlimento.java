package br.rpgatributos.fazenda;

import br.rpgatributos.alquimia.Ingrediente;
import br.rpgatributos.mundo.NaturezaViva;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.vida.Artesanato;
import br.rpgatributos.vida.Coleta;
import br.rpgatributos.vida.Criacao;
import br.rpgatributos.vida.Cultivo;
import br.rpgatributos.vida.Erva;
import br.rpgatributos.vida.Fruta;
import br.rpgatributos.vida.Mel;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Os grupos de ingrediente (3.15): tudo o que se colhe, cria, pesca ou caça cai num grupo, e as
 * receitas podem pedir "qualquer fruta", "qualquer erva"... além do item exato. Assim o que o
 * jogador colhe sempre tem uma panela para onde ir.
 */
public enum GrupoAlimento {
    GRAO("grão ou castanha", 0xE8C15A),
    LEGUME("legume", 0x7CB342),
    FRUTA("fruta", 0xE53935),
    ERVA("erva", 0x2E7D32),
    COGUMELO("cogumelo", 0xA1887F),
    PEIXE("peixe", 0x4FA3D1),
    CARNE("carne", 0xB0603A),
    LATICINIO("ovo ou laticínio", 0xFFF3C4),
    DOCE("doce (mel, açúcar, geleia)", 0xFFB300),
    MONSTRO("parte de monstro", 0x6D7F3A);

    private final String nome;
    private final TextColor cor;

    GrupoAlimento(String nome, int cor) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }

    /** O grupo do item, ou null (prato pronto, ferramenta, item do plugin que não é comida...). */
    public static GrupoAlimento de(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        if (Fruta.de(s) != null) return FRUTA;
        Coleta co = Coleta.de(s);
        if (co != null) return switch (co) {
            case MOREL, COGUMELO_DO_BOSQUE, COGUMELO_DE_NEVE -> COGUMELO;
            case AMORA_SILVESTRE, GROSELHA, FRUTO_DE_ZIMBRO -> FRUTA;
            case CASTANHA, AVELA -> GRAO;
            default -> LEGUME;
        };
        if (Erva.de(s) != null) return ERVA;
        if (Mel.de(s) != null || NaturezaViva.ehSeiva(s)) return DOCE;
        Cultivo cv = Cultivo.daColheita(s);
        if (cv != null) return switch (cv) {
            case MORANGO, MIRTILO, TOMATE, MELAO, UVA, OXICOCO -> FRUTA;
            case MILHO -> GRAO;
            default -> LEGUME;
        };
        Variedade v = Variedade.de(s);
        if (v != null) return Variedade.ehSemente(s) ? null : v == Variedade.TRIGO_DOURADO ? GRAO : LEGUME;
        if (PeixeRaro.de(s) != null) return PEIXE;
        Artesanato a = Artesanato.de(s);
        if (a != null) return switch (a) {
            case QUEIJO, QUEIJO_DE_CABRA, MAIONESE -> LATICINIO;
            case GELEIA -> DOCE;
            case CONSERVA -> LEGUME;
            case OLEO_DE_TRUFA -> null;
        };
        if (br.rpgatributos.mundo.Fauna.carne(s) != null) return CARNE;
        if ("trufa".equals(Criacao.produto(s))) return COGUMELO;
        if ("leite_cabra".equals(Criacao.produto(s))) return LATICINIO;
        // Do jogo: só o item comum (com ou sem estrela de qualidade).
        if (!Ingrediente.simples(s)) return null;
        return switch (s.getType()) {
            case WHEAT -> GRAO;
            case CARROT, POTATO, BEETROOT, PUMPKIN, KELP, DRIED_KELP -> LEGUME;
            case APPLE, SWEET_BERRIES, GLOW_BERRIES, MELON_SLICE, CHORUS_FRUIT -> FRUTA;
            case BROWN_MUSHROOM, RED_MUSHROOM, CRIMSON_FUNGUS, WARPED_FUNGUS -> COGUMELO;
            case COD, SALMON, TROPICAL_FISH, COOKED_COD, COOKED_SALMON -> PEIXE;
            case BEEF, PORKCHOP, CHICKEN, MUTTON, RABBIT,
                 COOKED_BEEF, COOKED_PORKCHOP, COOKED_CHICKEN, COOKED_MUTTON, COOKED_RABBIT -> CARNE;
            case EGG, BROWN_EGG, BLUE_EGG, MILK_BUCKET -> LATICINIO;
            case SUGAR, HONEY_BOTTLE, COCOA_BEANS -> DOCE;
            case ROTTEN_FLESH, SPIDER_EYE, SLIME_BALL, PHANTOM_MEMBRANE, MAGMA_CREAM, GHAST_TEAR -> MONSTRO;
            default -> null;
        };
    }
}
