package br.rpgatributos.masmorra;

import org.bukkit.Material;

import java.util.concurrent.ThreadLocalRandom;

/** Aparência das salas geradas pelo código. Cada masmorra sorteia um tema. */
public enum Tema {
    CRIPTA("Cripta",
            new Material[]{Material.STONE_BRICKS, Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.ANDESITE},
            new Material[]{Material.STONE_BRICKS, Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CHISELED_STONE_BRICKS},
            Material.POLISHED_ANDESITE, Material.CHISELED_STONE_BRICKS, Material.GLOWSTONE, Material.SOUL_LANTERN, Material.WATER,
            new Material[]{Material.COBWEB, Material.SKELETON_SKULL, Material.BONE_BLOCK, Material.CANDLE}, Material.COBWEB),
    CAVERNA("Caverna Profunda",
            new Material[]{Material.DEEPSLATE_TILES, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.DEEPSLATE_BRICKS},
            new Material[]{Material.DEEPSLATE_BRICKS, Material.CRACKED_DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES, Material.COBBLED_DEEPSLATE},
            Material.POLISHED_DEEPSLATE, Material.CHISELED_DEEPSLATE, Material.PEARLESCENT_FROGLIGHT, Material.LANTERN, Material.WATER,
            new Material[]{Material.MOSS_CARPET, Material.AMETHYST_CLUSTER, Material.COBWEB, Material.MOSS_BLOCK}, Material.IRON_CHAIN),
    INFERNO("Fornalha Infernal",
            new Material[]{Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, Material.NETHER_BRICKS},
            new Material[]{Material.POLISHED_BLACKSTONE_BRICKS, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS, Material.NETHER_BRICKS, Material.GILDED_BLACKSTONE},
            Material.POLISHED_BASALT, Material.CHISELED_POLISHED_BLACKSTONE, Material.SHROOMLIGHT, Material.LANTERN, Material.LAVA,
            new Material[]{Material.WITHER_SKELETON_SKULL, Material.MAGMA_BLOCK, Material.COBWEB, Material.CANDLE}, Material.IRON_CHAIN),
    GELO("Palácio Gelado",
            new Material[]{Material.PACKED_ICE, Material.SNOW_BLOCK, Material.POLISHED_DIORITE, Material.PACKED_ICE},
            new Material[]{Material.PACKED_ICE, Material.SNOW_BLOCK, Material.POLISHED_DIORITE, Material.BLUE_ICE},
            Material.QUARTZ_PILLAR, Material.CHISELED_QUARTZ_BLOCK, Material.SEA_LANTERN, Material.SOUL_LANTERN, Material.POWDER_SNOW,
            new Material[]{Material.SNOW, Material.ICE, Material.COBWEB, Material.SNOW}, Material.COBWEB);

    private final String nome;
    private final Material[] piso;
    private final Material[] parede;
    private final Material pilar;
    private final Material destaque;
    private final Material luz;
    private final Material lanterna;
    private final Material liquido;
    private final Material[] decorChao;
    private final Material decorTeto;

    Tema(String nome, Material[] piso, Material[] parede, Material pilar, Material destaque, Material luz,
         Material lanterna, Material liquido, Material[] decorChao, Material decorTeto) {
        this.nome = nome;
        this.piso = piso;
        this.parede = parede;
        this.pilar = pilar;
        this.destaque = destaque;
        this.luz = luz;
        this.lanterna = lanterna;
        this.liquido = liquido;
        this.decorChao = decorChao;
        this.decorTeto = decorTeto;
    }

    private static Material um(Material[] lista) {
        return lista[ThreadLocalRandom.current().nextInt(lista.length)];
    }

    public String nome() { return nome; }
    public Material piso() { return um(piso); }
    public Material parede() { return um(parede); }
    public Material pilar() { return pilar; }
    public Material destaque() { return destaque; }
    /** Bloco de luz embutido no teto e no topo dos pilares. */
    public Material luz() { return luz; }
    public Material lanterna() { return lanterna; }
    /** Líquido do fosso (água, lava ou neve fofa). */
    public Material liquido() { return liquido; }
    public Material decorChao() { return um(decorChao); }
    public Material decorTeto() { return decorTeto; }

    public static Tema sortear() {
        Tema[] t = values();
        return t[ThreadLocalRandom.current().nextInt(t.length)];
    }
}
