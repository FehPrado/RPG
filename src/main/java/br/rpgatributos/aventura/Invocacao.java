package br.rpgatributos.aventura;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tudo que o Altar Ritualístico invoca e a oferenda de cada um.
 * A oferenda é jogada (tecla Q) em cima do altar, ou entregue pelo menu do altar.
 */
public enum Invocacao {
    // ---------- chefes ----------
    GOLEM(Chefe.GOLEM, Material.IRON_BLOCK, "Um gigante de ferro desperta.", false,
            oferenda(Material.IRON_BLOCK, 4, Material.CARVED_PUMPKIN, 1)),
    RAINHA(Chefe.RAINHA, Material.SPIDER_EYE, "A mãe de todas as aranhas sai da toca.", false,
            oferenda(Material.SPIDER_EYE, 8, Material.STRING, 16)),
    CHAMAS(Chefe.CHAMAS, Material.BLAZE_ROD, "O fogo do Nether ganha vontade própria.", false,
            oferenda(Material.BLAZE_ROD, 8, Material.MAGMA_CREAM, 4)),
    LICH(Chefe.LICH, Material.SKELETON_SKULL, "Um feiticeiro que enganou a morte.", false,
            oferenda(Material.SKELETON_SKULL, 1, Material.BONE, 32)),
    TEMPESTADE(Chefe.TEMPESTADE, Material.BREEZE_ROD, "Uma tempestade que anda e pensa.", false,
            oferenda(Material.BREEZE_ROD, 4, Material.WIND_CHARGE, 8)),
    ARAUTO(Chefe.ARAUTO, Material.ECHO_SHARD, "O mensageiro do fim de todas as coisas.", false,
            oferenda(Material.NETHER_STAR, 1, Material.ECHO_SHARD, 4, Material.DRAGON_BREATH, 1)),

    // ---------- eventos ----------
    LUA_DE_SANGUE(null, Material.REDSTONE_BLOCK, "Até o amanhecer: monstros mais fortes e em maior número, com o dobro de XP e drops. Ninguém consegue dormir.", true,
            oferenda(Material.WITHER_ROSE, 1, Material.ROTTEN_FLESH, 16)),
    METEOROS(null, Material.FIRE_CHARGE, "Por 90 segundos caem meteoros em volta do altar, espalhando minérios. Cuidado com o impacto!", false,
            oferenda(Material.FIRE_CHARGE, 8, Material.DIAMOND, 1)),
    ONDAS(null, Material.IRON_SWORD, "5 ondas de monstros cada vez mais fortes em volta do altar. Vença todas para ganhar o tesouro.", false,
            oferenda(Material.ROTTEN_FLESH, 8, Material.BONE, 8, Material.STRING, 8, Material.GUNPOWDER, 8)),
    MERCADOR(null, Material.EMERALD, "Um mercador com trocas raras (Pedras de Proteção, Fragmentos de Forja...) por 10 minutos.", false,
            oferenda(Material.EMERALD, 24, Material.ENDER_EYE, 1)),
    CACADOR(null, Material.CROSSBOW, "O Caçador de Recompensas aparece por 10 minutos e oferece contratos para matar chefes.", false,
            oferenda(Material.EMERALD, 16, Material.IRON_SWORD, 1));

    private final Chefe chefe;
    private final Material icone;
    private final String descricao;
    private final boolean soDeNoite;
    private final Map<Material, Integer> oferenda;

    Invocacao(Chefe chefe, Material icone, String descricao, boolean soDeNoite, Map<Material, Integer> oferenda) {
        this.chefe = chefe;
        this.icone = icone;
        this.descricao = descricao;
        this.soDeNoite = soDeNoite;
        this.oferenda = oferenda;
    }

    private static Map<Material, Integer> oferenda(Object... pares) {
        Map<Material, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put((Material) pares[i], (Integer) pares[i + 1]);
        return m;
    }

    /** O chefe invocado, ou null se é um evento. */
    public Chefe chefe() { return chefe; }
    public Material icone() { return icone; }
    public String descricao() { return descricao; }
    public boolean soDeNoite() { return soDeNoite; }
    public Map<Material, Integer> oferenda() { return oferenda; }

    public String nome() {
        if (chefe != null) return chefe.nome();
        return switch (this) {
            case LUA_DE_SANGUE -> "Lua de Sangue";
            case METEOROS -> "Chuva de Meteoros";
            case ONDAS -> "Ondas de Monstros";
            case MERCADOR -> "Mercador Arcano";
            case CACADOR -> "Caçador de Recompensas";
            default -> name();
        };
    }

    public TextColor cor() {
        if (chefe != null) return chefe.cor();
        return switch (this) {
            case LUA_DE_SANGUE -> NamedTextColor.DARK_RED;
            case METEOROS -> TextColor.color(0xFF8C00);
            case ONDAS -> NamedTextColor.RED;
            case MERCADOR -> NamedTextColor.GREEN;
            case CACADOR -> NamedTextColor.GOLD;
            default -> NamedTextColor.WHITE;
        };
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Invocacao porId(String id) {
        for (Invocacao i : values()) if (i.id().equalsIgnoreCase(id)) return i;
        return null;
    }
}
