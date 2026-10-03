package br.rpgatributos.masmorra;

import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.forja.Raridade;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Locale;

/** Dificuldades da masmorra: tamanho, monstros, chefe, custo e recompensas. */
public enum Dificuldade {
    FACIL("Fácil", 0x55FF55, 5, 6, 2, 1.0, 1.0, 0.8, 0, Material.EMERALD, 8, Material.IRON_INGOT,
            Raridade.RARO, Raridade.EPICO, List.of(Chefe.GOLEM, Chefe.RAINHA),
            List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER)),
    NORMAL("Normal", 0xFFFF55, 7, 8, 2, 1.6, 1.3, 1.0, 0.10, Material.DIAMOND, 2, Material.DIAMOND,
            Raridade.EPICO, Raridade.UNICO, List.of(Chefe.CHAMAS, Chefe.LICH),
            List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON, EntityType.STRAY, EntityType.CAVE_SPIDER, EntityType.WITCH)),
    DIFICIL("Difícil", 0xFF5555, 9, 10, 3, 2.4, 1.6, 1.3, 0.30, Material.DIAMOND, 6, Material.DIAMOND,
            Raridade.UNICO, Raridade.LENDARIO, List.of(Chefe.TEMPESTADE),
            List.of(EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.HUSK, EntityType.STRAY)),
    PESADELO("Pesadelo", 0xAA00AA, 11, 13, 3, 3.5, 2.0, 1.6, 0.50, Material.NETHERITE_INGOT, 1, Material.NETHERITE_INGOT,
            Raridade.LENDARIO, Raridade.MITICO, List.of(Chefe.ARAUTO),
            List.of(EntityType.WITHER_SKELETON, EntityType.BLAZE, EntityType.VINDICATOR, EntityType.EVOKER,
                    EntityType.PIGLIN_BRUTE, EntityType.RAVAGER));

    private final String nome;
    private final TextColor cor;
    private final int salasMin, salasMax, ondas;
    private final double vida, dano, vidaChefe, nivelTotal;
    private final Material custo;
    private final int qtdCusto;
    private final Material material;
    private final Raridade raridadeMin, raridadeMax;
    private final List<Chefe> chefes;
    private final List<EntityType> monstros;

    Dificuldade(String nome, int cor, int salasMin, int salasMax, int ondas, double vida, double dano, double vidaChefe,
                double nivelTotal, Material custo, int qtdCusto, Material material, Raridade raridadeMin, Raridade raridadeMax,
                List<Chefe> chefes, List<EntityType> monstros) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.salasMin = salasMin;
        this.salasMax = salasMax;
        this.ondas = ondas;
        this.vida = vida;
        this.dano = dano;
        this.vidaChefe = vidaChefe;
        this.nivelTotal = nivelTotal;
        this.custo = custo;
        this.qtdCusto = qtdCusto;
        this.material = material;
        this.raridadeMin = raridadeMin;
        this.raridadeMax = raridadeMax;
        this.chefes = chefes;
        this.monstros = monstros;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public int salasMin() { return salasMin; }
    public int salasMax() { return salasMax; }
    /** Ondas de monstros em cada sala de combate. */
    public int ondas() { return ondas; }
    /** Multiplicador da vida dos monstros. */
    public double vida() { return vida; }
    /** Multiplicador do dano dos monstros. */
    public double dano() { return dano; }
    /** Multiplicador da vida do chefe (além do bônus por jogador). */
    public double vidaChefe() { return vidaChefe; }
    public Material custo() { return custo; }
    public int qtdCusto() { return qtdCusto; }
    /** Material dos equipamentos forjados do tesouro (FERRO, DIAMANTE, NETHERITE). */
    public Material material() { return material; }
    public Raridade raridadeMin() { return raridadeMin; }
    public Raridade raridadeMax() { return raridadeMax; }
    public List<Chefe> chefes() { return chefes; }
    public List<EntityType> monstros() { return monstros; }
    public int nivel() { return ordinal(); }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Nível total (soma dos atributos) necessário para abrir. */
    public int nivelTotalNecessario(int nivelTotalMaximo) {
        return (int) Math.ceil(nivelTotal * nivelTotalMaximo);
    }

    public static Dificuldade porId(String id) {
        for (Dificuldade d : values()) if (d.id().equalsIgnoreCase(id)) return d;
        return null;
    }
}
