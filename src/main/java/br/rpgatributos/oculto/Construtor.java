package br.rpgatributos.oculto;

import net.kyori.adventure.text.Component;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.Switch;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.Random;

/**
 * Constrói um Local Oculto: uma ruína pequena na superfície com um poço, a antessala do
 * enigma lá embaixo e o salão do guardião com o selo.
 */
final class Construtor {

    private Construtor() { }

    private static void por(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    /** Uma caixa: chão, paredes e teto do material; dentro vazio. */
    private static void sala(World w, int x0, int x1, int y0, int y1, int z0, int z1, Material parede, Material piso) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    boolean borda = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1;
                    por(w, x, y, z, borda ? (y == y0 ? piso : parede) : Material.AIR);
                }
            }
        }
    }

    /** Decide a altura da superfície e constrói tudo. */
    static void construir(Local l) {
        World w = l.world();
        if (w == null) return;
        Random r = new Random(l.id.hashCode());
        TipoLocal t = l.tipo;
        int ys = w.getHighestBlockYAt(l.x, l.z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        l.y = Math.max(ys, w.getMinHeight() + 22);
        int ya = l.yAntessala();
        int x = l.x, z = l.z;

        // ---------- antessala (enigma) ----------
        sala(w, x - 6, x + 6, ya - 1, ya + 5, z - 6, z + 6, t.parede(), t.detalhe());
        // pilar central com a escada do poço
        for (int y = ya; y <= ya + 4; y++) por(w, x, y, z + 1, t.detalhe());
        // luzes nos cantos
        for (int[] c : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) por(w, x + c[0], ya, z + c[1], t.luz());
        por(w, x - 5, ya + 4, z, t.luz());
        por(w, x + 5, ya + 4, z, t.luz());
        // alavancas na parede norte, com a cor de cada uma em cima
        for (int i = 0; i < 4; i++) {
            Block alavanca = l.alavanca(i).getBlock();
            Switch s = (Switch) Material.LEVER.createBlockData();
            s.setAttachedFace(FaceAttachable.AttachedFace.WALL);
            s.setFacing(BlockFace.SOUTH);
            s.setPowered(false);
            alavanca.setBlockData(s, false);
            por(w, alavanca.getX(), ya + 3, z - 6, Local.CORES[l.cores[i]]);
        }
        // púlpito com o livro do enigma
        Block pulpito = w.getBlockAt(x + 4, ya, z + 3);
        pulpito.setType(Material.LECTERN, false);
        if (pulpito.getState(false) instanceof Lectern lec) lec.getInventory().setItem(0, livro(l));
        // portão (grades) para o salão, na parede oeste
        fecharPortao(l);

        // ---------- salão do guardião ----------
        sala(w, x - 22, x - 7, ya - 1, ya + 7, z - 8, z + 8, t.parede(), t.detalhe());
        for (int lado = -1; lado <= 1; lado++) for (int dy = 0; dy <= 2; dy++) por(w, x - 7, ya + dy, z + lado, Material.AIR);
        for (int[] c : new int[][]{{-11, -4}, {-11, 4}, {-18, -4}, {-18, 4}}) {
            for (int y = ya; y <= ya + 6; y++) por(w, x + c[0], y, z + c[1], t.detalhe());
            por(w, x + c[0], ya + 6, z + c[1], t.luz());
        }
        for (int xx = x - 20; xx <= x - 9; xx += 4) for (int zz = z - 6; zz <= z + 6; zz += 6) por(w, xx, ya + 7, zz, t.luz());
        // altar do selo
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) por(w, x - 19 + dx, ya - 1, z + dz, t.detalhe());
        por(w, x - 19, ya, z, Material.BEACON);
        por(w, x - 19, ya, z - 2, t.enfeite());
        por(w, x - 19, ya, z + 2, t.enfeite());
        // enfeites espalhados
        for (int i = 0; i < 6; i++) {
            int ex = x - 20 + r.nextInt(12), ez = z - 7 + r.nextInt(15);
            if (w.getBlockAt(ex, ya, ez).isEmpty() && Math.abs(ez - z) > 1) por(w, ex, ya, ez, t.enfeite());
        }

        // ---------- poço até a superfície ----------
        // Paredes em volta do poço (para nenhuma caverna vazar para dentro) e ar em cima da boca.
        for (int y = ya + 5; y < l.y - 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) por(w, x + dx, y, z + dz, t.parede());
            }
        }
        for (int y = l.y; y <= l.y + 2; y++) por(w, x, y, z, Material.AIR);
        // Escada do chão da antessala até a boca do poço, presa no bloco ao sul (pilar / parede do poço).
        Ladder escada = (Ladder) Material.LADDER.createBlockData();
        escada.setFacing(BlockFace.NORTH);
        for (int y = ya; y <= l.y - 1; y++) w.getBlockAt(x, y, z).setBlockData(escada, false);

        // ---------- ruína na superfície ----------
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (r.nextDouble() < 0.7) por(w, x + dx, l.y - 1, z + dz, r.nextBoolean() ? t.parede() : t.detalhe());
                boolean anel = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                if (!anel) continue;
                int altura = r.nextInt(4);
                for (int h = 0; h < altura; h++) por(w, x + dx, l.y + h, z + dz, t.parede());
            }
        }
        for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            int altura = 2 + r.nextInt(3);
            for (int h = 0; h < altura; h++) por(w, x + c[0], l.y + h, z + c[1], t.detalhe());
        }
        l.construido = true;
    }

    static void fecharPortao(Local l) {
        World w = l.world();
        int ya = l.yAntessala();
        for (int lado = -1; lado <= 1; lado++) {
            for (int dy = 0; dy <= 2; dy++) w.getBlockAt(l.x - 6, ya + dy, l.z + lado).setType(Material.IRON_BARS, true);
        }
    }

    static void abrirPortao(Local l) {
        World w = l.world();
        int ya = l.yAntessala();
        for (int lado = -1; lado <= 1; lado++) {
            for (int dy = 0; dy <= 2; dy++) w.getBlockAt(l.x - 6, ya + dy, l.z + lado).setType(Material.AIR, true);
        }
    }

    /** Desliga as alavancas. */
    static void desligarAlavancas(Local l) {
        for (int i = 0; i < 4; i++) {
            Block b = l.alavanca(i).getBlock();
            if (b.getBlockData() instanceof Switch s) {
                s.setPowered(false);
                b.setBlockData(s, true);
            }
        }
    }

    static ItemStack livro(Local l) {
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text(l.tipo.nome()));
            m.author(Component.text("???"));
            m.pages(Component.text(l.tipo.abertura() + "\n\n" + l.enigma()),
                    Component.text("Errar acorda os guardiões.\n\nAlém do portão, um guardião protege o selo. "
                            + "O selo só reconhece quem já dominou o caminho certo..."));
        });
        return livro;
    }
}
