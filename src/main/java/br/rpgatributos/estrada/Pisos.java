package br.rpgatributos.estrada;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

/**
 * O piso das estradas (2.94), em 4 níveis:
 * <ol>
 *   <li>caminho de terra (a trilha);</li>
 *   <li>cascalho e barro compactado;</li>
 *   <li>pedregulho, tijolos de todo tipo, ladrilhos e laje/escada de pedra;</li>
 *   <li>pedra polida, lisa ou cortada (a "Estrada Real").</li>
 * </ol>
 * Lajes e escadas contam como o bloco delas. Pedra comum inteira não conta (é o chão das montanhas e cavernas).
 */
public final class Pisos {

    public static final int NENHUM = 0, TERRA = 1, CASCALHO = 2, PEDRA = 3, REAL = 4;

    private static final String[] NOMES = {"", "Trilha", "Cascalho", "Calçada", "Estrada Real"};
    private static final int[] NIVEL = new int[Material.values().length];

    static {
        for (Material m : Material.values()) NIVEL[m.ordinal()] = m.isBlock() ? calcular(m) : NENHUM;
    }

    private Pisos() { }

    public static int nivel(Material m) {
        return NIVEL[m.ordinal()];
    }

    public static int nivel(Block b) {
        return nivel(b.getType());
    }

    public static String nome(int nivel) {
        return nivel > 0 && nivel < NOMES.length ? NOMES[nivel] : "";
    }

    /** O bloco em que a entidade está pisando (laje e caminho de terra são mais baixos; por isso o 0,1). Camada de neve: o de baixo dela. */
    public static Block debaixo(Entity e) {
        Block b = e.getLocation().subtract(0, 0.1, 0).getBlock();
        return b.getType() == Material.SNOW ? b.getRelative(BlockFace.DOWN) : b;
    }

    /**
     * O nível do piso neste bloco, contando o que depende do lugar (2.95): camada de neve por cima não
     * apaga a estrada, e tábua, laje ou escada de madeira com água ou vão embaixo é ponte (nível do cascalho).
     */
    public static int nivelEm(Block b) {
        if (b.getType() == Material.SNOW) b = b.getRelative(BlockFace.DOWN);
        int n = nivel(b.getType());
        if (n == NENHUM && ponteDeMadeira(b.getType())) {
            Block d = b.getRelative(BlockFace.DOWN);
            if (d.isEmpty() || d.isLiquid()) return CASCALHO;
        }
        return n;
    }

    private static boolean ponteDeMadeira(Material m) {
        return Tag.PLANKS.isTagged(m) || Tag.WOODEN_SLABS.isTagged(m) || Tag.WOODEN_STAIRS.isTagged(m);
    }

    private static int calcular(Material m) {
        if (m == Material.DIRT_PATH) return TERRA;
        if (m == Material.GRAVEL || m == Material.PACKED_MUD) return CASCALHO;
        String n = m.name();
        if (n.endsWith("_WALL") || n.contains("FENCE") || n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE")
                || n.contains("INFESTED") || n.endsWith("_ORE")) return NENHUM;
        boolean parte = n.endsWith("_SLAB") || n.endsWith("_STAIRS");
        String base = n.endsWith("_SLAB") ? n.substring(0, n.length() - 5) : n.endsWith("_STAIRS") ? n.substring(0, n.length() - 7) : n;
        if (base.startsWith("POLISHED_") || base.startsWith("SMOOTH_") || base.startsWith("CUT_")) return REAL;
        if (parte && base.equals("STONE")) return PEDRA;
        if (base.contains("COBBLESTONE") || base.startsWith("COBBLED_") || base.contains("BRICK") || base.endsWith("_TILES")
                || base.endsWith("_TILE")) return PEDRA;
        return NENHUM;
    }
}
