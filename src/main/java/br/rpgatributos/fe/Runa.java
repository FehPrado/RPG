package br.rpgatributos.fe;

import br.rpgatributos.forja.Categoria;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runas gravadas na Mesa Rúnica: uma por equipamento, com um efeito que funciona
 * enquanto ele está vestido ou na mão. Gravar outra troca a anterior.
 */
public enum Runa {
    SALTO("Runa do Salto", "ᛖ", 0xA0C4FF, EnumSet.of(Categoria.BOTAS), Map.of(Material.FEATHER, 4, Material.RABBIT_FOOT, 1), 10,
            "Pulo duplo: aperte pular de novo no ar"),
    PROTECAO("Runa da Proteção", "ᛉ", 0xFFD54F, EnumSet.of(Categoria.PEITORAL), Map.of(Material.IRON_INGOT, 4, Material.GOLDEN_APPLE, 1), 12,
            "Com menos de 30% de vida: Absorção II por 8 s (a cada 60 s)"),
    VISAO("Runa da Visão", "ᛟ", 0x80DEEA, EnumSet.of(Categoria.CAPACETE), Map.of(Material.GOLDEN_CARROT, 1, Material.GLOWSTONE_DUST, 4), 8,
            "Visão noturna em lugares escuros"),
    IMPETO("Runa do Ímpeto", "ᚱ", 0x9CCC65, EnumSet.of(Categoria.CALCA), Map.of(Material.SUGAR, 4, Material.RABBIT_HIDE, 2), 8,
            "+8% de velocidade"),
    EXPLOSAO("Runa da Explosão", "ᚦ", 0xFF7043, EnumSet.of(Categoria.ESPADA, Categoria.MACHADO, Categoria.LANCA, Categoria.MACA),
            Map.of(Material.GUNPOWDER, 4, Material.FIRE_CHARGE, 1), 12, "8% de chance de explodir no alvo (sem quebrar blocos)"),
    GELO("Runa do Gelo", "ᛁ", 0x81D4FA, EnumSet.of(Categoria.ESPADA, Categoria.MACHADO, Categoria.LANCA, Categoria.MACA),
            Map.of(Material.PACKED_ICE, 2, Material.SNOWBALL, 4), 10, "15% de chance de congelar o alvo por 2 s"),
    TEMPESTADE("Runa da Tempestade", "ᛋ", 0xFFF176, EnumSet.of(Categoria.ESPADA, Categoria.MACHADO, Categoria.LANCA, Categoria.MACA,
            Categoria.ARCO, Categoria.BESTA), Map.of(Material.LIGHTNING_ROD, 1, Material.COPPER_INGOT, 4), 15, "6% de chance de chamar um raio no alvo"),
    VAMPIRICA("Runa Vampírica", "ᚹ", 0xC62828, EnumSet.of(Categoria.ESPADA, Categoria.MACHADO, Categoria.LANCA, Categoria.MACA),
            Map.of(Material.GHAST_TEAR, 2, Material.REDSTONE, 4), 15, "Rouba 8% do dano como vida"),
    FUSAO("Runa da Fusão", "ᚲ", 0xFF8A65, EnumSet.of(Categoria.PICARETA, Categoria.MACHADO, Categoria.PA),
            Map.of(Material.COAL, 8, Material.BLAZE_POWDER, 1), 10, "Minérios, areia e pedra já saem fundidos"),
    ECO("Runa do Eco", "ᛇ", 0xB39DDB, EnumSet.of(Categoria.ARCO, Categoria.BESTA), Map.of(Material.AMETHYST_SHARD, 2, Material.STRING, 4), 12,
            "Cada flecha dispara uma segunda, com metade do dano");

    private final String nome, simbolo, descricao;
    private final TextColor cor;
    private final Set<Categoria> onde;
    private final Map<Material, Integer> custo;
    private final int niveis;

    Runa(String nome, String simbolo, int cor, Set<Categoria> onde, Map<Material, Integer> custo, int niveis, String descricao) {
        this.nome = nome;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.onde = onde;
        this.custo = custo;
        this.niveis = niveis;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String simbolo() { return simbolo; }
    public TextColor cor() { return cor; }
    public Set<Categoria> onde() { return onde; }
    public Map<Material, Integer> custo() { return custo; }
    /** Níveis de XP gastos para gravar. */
    public int niveis() { return niveis; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public boolean serve(Material m) {
        Categoria c = Categoria.de(m);
        return c != null && onde.contains(c);
    }

    public static Runa porId(String s) {
        for (Runa r : values()) if (r.id().equalsIgnoreCase(s)) return r;
        return null;
    }
}
