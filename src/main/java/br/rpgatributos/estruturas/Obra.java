package br.rpgatributos.estruturas;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ajuda a montar uma estrutura em coordenadas locais (dx, dy, dz) a partir de uma origem,
 * girada em passos de 90°. Na rotação 0 a frente da estrutura olha para o sul (+z); os
 * blocos com direção (escadas, portas, baús, toras...) giram junto. Cercas, painéis e muros
 * se ligam aos vizinhos no fim, com {@link #conectar()}.
 */
final class Obra {

    private static final BlockFace[] GIRO = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
    private static final BlockFace[] HORIZONTAIS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    final World w;
    final int x, y, z, rot;
    final Random r;
    private final List<Block> ligar;

    Obra(World w, int x, int y, int z, int rot, Random r) {
        this(w, x, y, z, rot, r, new ArrayList<>());
    }

    private Obra(World w, int x, int y, int z, int rot, Random r, List<Block> ligar) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rot = ((rot % 4) + 4) % 4;
        this.r = r;
        this.ligar = ligar;
    }

    /** Uma parte da obra com origem própria e giro extra (a tenda de um acampamento, por exemplo). */
    Obra parte(int dx, int dz, int giroExtra) {
        Block b = bloco(dx, 0, dz);
        return new Obra(w, b.getX(), y, b.getZ(), rot + giroExtra, r, ligar);
    }

    Block bloco(int dx, int dy, int dz) {
        int gx, gz;
        switch (rot) {
            case 1 -> { gx = -dz; gz = dx; }
            case 2 -> { gx = -dx; gz = -dz; }
            case 3 -> { gx = dz; gz = -dx; }
            default -> { gx = dx; gz = dz; }
        }
        return w.getBlockAt(x + gx, y + dy, z + gz);
    }

    /** Direção local → direção no mundo. */
    BlockFace face(BlockFace f) {
        for (int i = 0; i < 4; i++) {
            if (GIRO[i] == f) return GIRO[(i + rot) % 4];
        }
        return f;
    }

    static BlockData dados(String s) {
        return Bukkit.createBlockData(s.contains(":") ? s : "minecraft:" + s);
    }

    Block por(int dx, int dy, int dz, Material m) {
        return por(dx, dy, dz, m.createBlockData());
    }

    /** Ex.: {@code por(0, 1, 0, "spruce_stairs[facing=north,half=top]")} (direção local). */
    Block por(int dx, int dy, int dz, String dados) {
        return por(dx, dy, dz, dados(dados));
    }

    Block por(int dx, int dy, int dz, BlockData d) {
        Block b = bloco(dx, dy, dz);
        b.setBlockData(girar(d.clone()), false);
        if (d instanceof Fence || d instanceof GlassPane || d instanceof Wall) ligar.add(b);
        return b;
    }

    /** Põe só se o lugar estiver vazio (ar, planta, neve fina...). */
    void porSeVazio(int dx, int dy, int dz, Material m) {
        if (bloco(dx, dy, dz).isReplaceable()) por(dx, dy, dz, m);
    }

    /** Enche uma caixa (inclusive) com o material. */
    void encher(int x0, int x1, int y0, int y1, int z0, int z1, Material m) {
        for (int i = Math.min(x0, x1); i <= Math.max(x0, x1); i++) {
            for (int j = Math.min(y0, y1); j <= Math.max(y0, y1); j++) {
                for (int k = Math.min(z0, z1); k <= Math.max(z0, z1); k++) por(i, j, k, m);
            }
        }
    }

    /** Sorteia um dos materiais pelos pesos (pares material, peso). */
    Material um(Object... pares) {
        int total = 0;
        for (int i = 1; i < pares.length; i += 2) total += (Integer) pares[i];
        int v = r.nextInt(total);
        for (int i = 0; i < pares.length; i += 2) {
            v -= (Integer) pares[i + 1];
            if (v < 0) return (Material) pares[i];
        }
        return (Material) pares[0];
    }

    boolean chance(double p) {
        return r.nextDouble() < p;
    }

    private BlockData girar(BlockData d) {
        if (rot == 0) return d;
        if (d instanceof Directional dir) {
            BlockFace f = face(dir.getFacing());
            if (dir.getFaces().contains(f)) dir.setFacing(f);
        }
        if (d instanceof Orientable o && rot % 2 == 1 && o.getAxis() != Axis.Y) {
            Axis novo = o.getAxis() == Axis.X ? Axis.Z : Axis.X;
            if (o.getAxes().contains(novo)) o.setAxis(novo);
        }
        if (d instanceof Rotatable ro) ro.setRotation(face(ro.getRotation()));
        if (d instanceof Rail trilho && rot % 2 == 1) {
            if (trilho.getShape() == Rail.Shape.EAST_WEST) trilho.setShape(Rail.Shape.NORTH_SOUTH);
            else if (trilho.getShape() == Rail.Shape.NORTH_SOUTH) trilho.setShape(Rail.Shape.EAST_WEST);
        }
        if (d instanceof MultipleFacing mf && !(d instanceof Fence) && !(d instanceof GlassPane)) {
            List<BlockFace> ligadas = new ArrayList<>();
            for (BlockFace f : HORIZONTAIS) if (mf.getAllowedFaces().contains(f) && mf.hasFace(f)) ligadas.add(f);
            for (BlockFace f : ligadas) mf.setFace(f, false);
            for (BlockFace f : ligadas) mf.setFace(face(f), true);
        }
        return d;
    }

    /** Liga cercas, painéis/grades e muros aos vizinhos (sem física, eles ficariam soltos). */
    void conectar() {
        for (Block b : ligar) {
            BlockData d = b.getBlockData();
            if (d instanceof Wall muro) {
                int n = 0;
                boolean ns = false, lo = false;
                for (BlockFace f : HORIZONTAIS) {
                    boolean liga = ligaEm(b.getRelative(f), true);
                    muro.setHeight(f, liga ? Wall.Height.LOW : Wall.Height.NONE);
                    if (liga) {
                        n++;
                        if (f == BlockFace.NORTH || f == BlockFace.SOUTH) ns = true;
                        else lo = true;
                    }
                }
                boolean reto = n == 2 && (ns != lo);
                muro.setUp(!reto || !b.getRelative(BlockFace.UP).isEmpty());
                b.setBlockData(muro, false);
            } else if (d instanceof MultipleFacing mf) {
                for (BlockFace f : HORIZONTAIS) {
                    if (mf.getAllowedFaces().contains(f)) mf.setFace(f, ligaEm(b.getRelative(f), d instanceof Fence));
                }
                b.setBlockData(mf, false);
            }
        }
    }

    private static boolean ligaEm(Block v, boolean cercaOuMuro) {
        BlockData d = v.getBlockData();
        if (v.getType().isOccluding()) return true;
        if (cercaOuMuro) return d instanceof Fence || d instanceof Wall || v.getType().name().endsWith("_FENCE_GATE");
        return d instanceof GlassPane || v.getType() == Material.IRON_BARS;
    }
}
