package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.fe.Deus;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Carta;
import br.rpgatributos.vida.Erva;
import br.rpgatributos.vida.Flechas;
import br.rpgatributos.vida.Fruta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * As plantas de cada estrutura, bloco a bloco, em coordenadas locais (a frente olha para +z).
 * O chão (y = 0) já vem nivelado e o espaço acima já vem limpo.
 */
final class Projetos {

    /** Covas do cemitério (x, z da lápide); a terra fica em z+1 e z+2. */
    static final int[][] COVAS = {{-5, -1}, {-3, -1}, {3, -1}, {5, -1}, {-5, 3}, {-3, 3}, {3, 3}, {5, 3}};
    /** Onde os guardas aparecem (x, y, z locais). Na mina, y é relativo ao fundo do túnel. */
    static final int[][] GUARDAS_TORRE = {{0, 13, 1}, {1, 6, 0}, {0, 1, 4}};
    static final int[][] GUARDAS_ACAMPAMENTO = {{-1, 1, -2}, {-3, 1, 1}, {3, 1, 2}, {0, 1, 4}};
    static final int[] CHEFE_ACAMPAMENTO = {0, 1, -4};
    static final int[][] GUARDAS_MINA = {{-3, 0, 0}, {3, 0, 0}, {0, 0, 3}, {-4, 0, 1}, {4, 0, -1}};
    static final int[] EREMITA = {0, 1, -1};

    private final RPGAtributos plugin;
    private final Ruinas ruinas;
    private final Mar mar;

    Projetos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.ruinas = new Ruinas(plugin, this);
        this.mar = new Mar(plugin, this);
    }

    void construir(Sitio s, Obra o) {
        switch (s.tipo) {
            case TORRE_DE_VIGIA -> torre(o);
            case ACAMPAMENTO -> acampamento(o);
            case CEMITERIO -> cemiterio(o);
            case MINA -> mina(o, s.fundo);
            case POCO_DOS_DESEJOS -> poco(o);
            case CABANA_DO_EREMITA -> cabana(o);
            case SANTUARIO_ESQUECIDO -> santuario(o, s.deus);
            case OASIS -> ruinas.oasis(o);
            case CABANA_DA_BRUXA -> ruinas.bruxa(o);
            case FAROL -> ruinas.farol(o);
            case EXPEDICAO -> ruinas.expedicao(o);
            case VILA_SAQUEADA -> ruinas.vila(o, s.pista);
            case TORRE_DO_MAGO -> ruinas.torreMago(o);
            case CIRCULO_DE_PEDRAS -> ruinas.circulo(o);
            case FORJA_DOS_ANOES -> ruinas.forjaAnoes(o);
            case FORTIM -> ruinas.fortim(o);
            case NAUFRAGIO -> mar.naufragio(o, Mar.Navio.values()[Math.floorMod(s.fundo, Mar.Navio.values().length)], s.nome, s.pista);
            case CIDADE_SUBMERSA -> { } // feita por etapas na CidadesSubmersas
        }
        o.conectar();
    }

    // =====================================================================
    //  Torre de Vigia Abandonada
    // =====================================================================

    Material pedra(Obra o) {
        return o.um(Material.STONE_BRICKS, 6, Material.MOSSY_STONE_BRICKS, 3, Material.CRACKED_STONE_BRICKS, 2);
    }

    private void torre(Obra o) {
        // Base: anel de pedregulho, um degrau em volta e o alicerce da torre.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int anel = Math.max(Math.abs(dx), Math.abs(dz));
                if (anel == 3) {
                    o.por(dx, 0, dz, o.um(Material.COBBLESTONE, 3, Material.MOSSY_COBBLESTONE, 2));
                    if (Math.abs(dx) == 3 && Math.abs(dz) == 3) {
                        if (o.chance(0.5)) o.por(dx, 1, dz, "mossy_cobblestone_wall");
                    } else if (o.chance(0.8)) {
                        String lado = dz == 3 ? "north" : dz == -3 ? "south" : dx == 3 ? "west" : "east";
                        o.por(dx, 1, dz, (o.chance(0.7) ? "stone_brick_stairs" : "mossy_stone_brick_stairs") + "[facing=" + lado + "]");
                    }
                } else {
                    o.por(dx, -1, dz, Material.COBBLESTONE);
                    o.por(dx, 0, dz, anel == 2 ? pedra(o) : o.um(Material.COBBLESTONE, 2, Material.MOSSY_COBBLESTONE, 1));
                }
            }
        }
        // Paredes (y 1 a 11), com fendas de flecha e a porta ao sul.
        for (int dy = 1; dy <= 11; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
                    boolean canto = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    o.por(dx, dy, dz, canto && dy % 4 == 0 ? Material.CHISELED_STONE_BRICKS : pedra(o));
                }
            }
        }
        o.por(0, 1, 2, Material.AIR);
        o.por(0, 2, 2, Material.AIR);
        for (int dy : new int[]{3, 7, 10}) {
            o.por(2, dy, 0, Material.AIR);
            o.por(-2, dy, 0, Material.AIR);
            if (dy != 3) o.por(0, dy, 2, Material.AIR);
        }
        // Pisos de madeira (alguns tábuas caíram) e a escada encostada na parede norte.
        for (int piso : new int[]{5, 9}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == -1) continue;
                    o.por(dx, piso, dz, o.chance(0.12) && !(dx == 0 && dz == 0) ? Material.AIR : Material.SPRUCE_PLANKS);
                }
            }
        }
        for (int dy = 1; dy <= 12; dy++) o.por(0, dy, -1, "ladder[facing=south]");
        // Térreo: barril de flechas, teias nos cantos.
        o.por(-1, 1, -1, "barrel[facing=up]");
        bau(o.bloco(-1, 1, -1), null, o, flechas(o));
        if (o.chance(0.6)) o.por(1, 4, -1, Material.COBWEB);
        if (o.chance(0.6)) o.por(-1, 4, 1, Material.COBWEB);
        // 1º andar: o canto de dormir do vigia.
        o.por(1, 6, 1, Material.BROWN_CARPET);
        o.por(1, 6, 0, Material.BROWN_CARPET);
        o.por(-1, 6, 1, Material.CAULDRON);
        if (o.chance(0.5)) o.por(-1, 8, -1, Material.COBWEB);
        if (o.chance(0.5)) o.por(1, 8, 1, Material.COBWEB);
        // 2º andar: vazio, só teias.
        if (o.chance(0.7)) o.por(1, 10, -1, Material.COBWEB);
        // Plataforma no alto (com mãos-francesas por baixo), parapeito quebrado, sino e o baú.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int anel = Math.max(Math.abs(dx), Math.abs(dz));
                if (!(dx == 0 && dz == -1)) o.por(dx, 12, dz, Material.SPRUCE_PLANKS);
                if (anel == 3 && Math.abs(dx) != Math.abs(dz)) {
                    String lado = dz == 3 ? "north" : dz == -3 ? "south" : dx == 3 ? "west" : "east";
                    o.por(dx, 11, dz, "spruce_stairs[facing=" + lado + ",half=top]");
                }
                if (anel == 3) {
                    boolean canto = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                    if (canto) {
                        for (int dy = 13; dy <= 15; dy++) o.por(dx, dy, dz, "spruce_log[axis=y]");
                    } else if (o.chance(0.75)) {
                        o.por(dx, 13, dz, Material.SPRUCE_FENCE);
                    }
                }
            }
        }
        o.por(3, 13, 0, Material.SPRUCE_FENCE);
        o.por(3, 14, 0, "lantern[hanging=false]");
        o.por(-3, 13, 0, Material.SPRUCE_FENCE);
        o.por(-3, 14, 0, "lantern[hanging=false]");
        o.por(-2, 13, -2, "bell[attachment=floor,facing=south]");
        o.por(2, 13, -2, "chest[facing=west]");
        bau(o.bloco(2, 13, -2), LootTables.SIMPLE_DUNGEON, o, extrasTorre(o));
        o.por(2, 13, -1, "barrel[facing=up]");
        // Telhado de quatro águas, com buracos.
        for (int nivel = 0; nivel <= 3; nivel++) {
            int a = 3 - nivel, dy = 16 + nivel;
            for (int dx = -a; dx <= a; dx++) {
                for (int dz = -a; dz <= a; dz++) {
                    int anel = Math.max(Math.abs(dx), Math.abs(dz));
                    if (anel != a) continue;
                    if (a == 0) { o.por(0, dy, 0, "spruce_slab[type=bottom]"); continue; }
                    boolean canto = Math.abs(dx) == a && Math.abs(dz) == a;
                    if (!canto && o.chance(0.22)) continue;
                    if (canto) { o.por(dx, dy, dz, Material.SPRUCE_PLANKS); continue; }
                    String lado = dz == a ? "north" : dz == -a ? "south" : dx == a ? "west" : "east";
                    o.por(dx, dy, dz, "spruce_stairs[facing=" + lado + "]");
                }
            }
        }
        // Heras subindo pelas paredes de fora.
        for (int lado = 0; lado < 4; lado++) {
            for (int p = -1; p <= 1; p++) {
                if (!o.chance(0.35)) continue;
                int dx = lado == 0 ? 3 : lado == 1 ? -3 : p, dz = lado == 2 ? 3 : lado == 3 ? -3 : p;
                String face = lado == 0 ? "west" : lado == 1 ? "east" : lado == 2 ? "north" : "south";
                int topo = 4 + o.r.nextInt(7);
                for (int dy = topo; dy >= Math.max(2, topo - 5); dy--) {
                    if (o.bloco(dx, dy, dz).isEmpty()) o.por(dx, dy, dz, "vine[" + face + "=true]");
                }
            }
        }
    }

    private List<ItemStack> flechas(Obra o) {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.ARROW, 8 + o.r.nextInt(17)));
        if (o.chance(0.5)) l.add(Flechas.criar(Flechas.Tipo.values()[o.r.nextInt(Flechas.Tipo.values().length)], 3 + o.r.nextInt(4)));
        l.add(new ItemStack(Material.BREAD, 1 + o.r.nextInt(3)));
        return l;
    }

    private List<ItemStack> extrasTorre(Obra o) {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.EMERALD, 2 + o.r.nextInt(5)));
        if (o.chance(0.3)) l.add(Raro.MAPA_RASGADO.criar(1));
        if (o.chance(0.15)) l.add(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, o.bloco(0, 0, 0).getLocation()));
        if (o.chance(0.5)) l.add(new ItemStack(Material.SPYGLASS));
        return l;
    }

    // =====================================================================
    //  Acampamento de Bandidos
    // =====================================================================

    private void acampamento(Obra o) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dz * dz <= 34 && o.chance(0.45)) {
                    o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 3, Material.DIRT_PATH, 2, Material.ROOTED_DIRT, 1));
                }
            }
        }
        // Fogueira com pedras em volta e os troncos de sentar.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.por(dx, 0, dz, o.um(Material.COBBLESTONE, 2, Material.STONE, 1));
        o.por(0, 1, 0, "campfire[lit=true]");
        o.por(-2, 1, -1, "stripped_spruce_log[axis=z]");
        o.por(-2, 1, 0, "stripped_spruce_log[axis=z]");
        o.por(2, 1, 0, "stripped_spruce_log[axis=z]");
        o.por(2, 1, 1, "stripped_spruce_log[axis=z]");
        o.por(-1, 1, 2, "spruce_log[axis=y]");
        // Três tendas viradas para o fogo.
        Material[] las = {Material.BROWN_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL, Material.RED_WOOL, Material.LIGHT_GRAY_WOOL};
        tenda(o.parte(0, -5, 0), las[o.r.nextInt(las.length)], true);
        tenda(o.parte(-5, 1, 3), las[o.r.nextInt(las.length)], false);
        tenda(o.parte(5, 1, 1), las[o.r.nextInt(las.length)], false);
        // Postes com lanterna.
        for (int lado : new int[]{-3, 3}) {
            o.por(lado, 1, -3, Material.SPRUCE_FENCE);
            o.por(lado, 2, -3, Material.SPRUCE_FENCE);
            o.por(lado, 3, -3, "lantern[hanging=false]");
        }
        // Mantimentos (barris, feno e o baú da comida) a sudeste.
        o.por(5, 1, 6, "barrel[facing=up]");
        o.por(6, 1, 6, "barrel[facing=up]");
        o.por(6, 2, 6, "barrel[facing=up]");
        o.por(6, 1, 5, "barrel[facing=up]");
        o.por(4, 1, 6, "hay_block[axis=y]");
        o.por(3, 1, 6, "chest[facing=north]");
        List<ItemStack> comida = new ArrayList<>(List.of(new ItemStack(Material.BREAD, 3 + o.r.nextInt(5)),
                new ItemStack(Material.COOKED_BEEF, 2 + o.r.nextInt(4)), new ItemStack(Material.APPLE, 1 + o.r.nextInt(4))));
        if (o.chance(0.35)) comida.add(Fruta.sortear(o.r).fruta(2 + o.r.nextInt(3)));
        bau(o.bloco(3, 1, 6), null, o, comida);
        // O canto de treino: alvo sobre um fardo de feno e uma bigorna lascada.
        o.por(-6, 1, 5, "hay_block[axis=y]");
        o.por(-6, 2, 5, Material.TARGET);
        o.por(-4, 1, 5, "chipped_anvil[facing=east]");
    }

    /** Tenda em A: fundo fechado em z=-2, abertura em z=+1. */
    void tenda(Obra t, Material la, boolean comBau) {
        for (int tz = -2; tz <= 1; tz++) {
            t.por(-2, 1, tz, la);
            t.por(2, 1, tz, la);
            t.por(-1, 2, tz, la);
            t.por(1, 2, tz, la);
            t.por(0, 3, tz, la);
            boolean fundo = tz == -2;
            for (int dx = -1; dx <= 1; dx++) t.por(dx, 1, tz, fundo ? la : Material.AIR);
            t.por(0, 2, tz, fundo ? la : Material.AIR);
        }
        t.por(-1, 1, -1, Material.BROWN_CARPET);
        t.por(-1, 1, 0, Material.BROWN_CARPET);
        t.por(1, 1, -1, "barrel[facing=up]");
        if (comBau) {
            t.por(0, 1, -1, "chest[facing=south]");
            List<ItemStack> extras = new ArrayList<>();
            extras.add(new ItemStack(Material.EMERALD, 3 + t.r.nextInt(8)));
            if (t.chance(0.3)) extras.add(Raro.MAPA_RASGADO.criar(1));
            if (t.chance(0.35)) extras.add(Album.carta(Carta.values()[t.r.nextInt(Carta.values().length)], false));
            if (t.chance(0.2)) extras.add(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, t.bloco(0, 0, 0).getLocation()));
            bau(t.bloco(0, 1, -1), LootTables.PILLAGER_OUTPOST, t, extras);
        }
    }

    // =====================================================================
    //  Cemitério Antigo
    // =====================================================================

    private void cemiterio(Obra o) {
        // Chão de terra remexida, o muro baixo e o portão com lanternas.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (o.chance(0.45)) o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 2, Material.PODZOL, 2, Material.GRASS_BLOCK, 3));
            }
        }
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 7) continue;
                if (dz == 7 && Math.abs(dx) <= 1) continue;
                if (dz != -7 && o.chance(0.12)) continue;
                o.por(dx, 1, dz, o.chance(0.6) ? "mossy_cobblestone_wall" : "cobblestone_wall");
            }
        }
        for (int lado : new int[]{-2, 2}) {
            o.por(lado, 1, 7, Material.MOSSY_STONE_BRICKS);
            o.por(lado, 2, 7, Material.CHISELED_STONE_BRICKS);
            o.por(lado, 3, 7, "soul_lantern[hanging=false]");
        }
        // Caminho de pedras do portão até a cripta.
        for (int dz = -2; dz <= 7; dz++) {
            for (int dx = -1; dx <= 1; dx++) o.por(dx, 0, dz, o.um(Material.COBBLESTONE, 3, Material.MOSSY_COBBLESTONE, 2, Material.COARSE_DIRT, 2));
        }
        // As covas.
        String[] lapides = {"cobblestone_wall", "mossy_cobblestone_wall", "stone_brick_wall", "mossy_stone_brick_wall"};
        for (int[] c : COVAS) {
            if (o.chance(0.15)) continue;
            int gx = c[0], hz = c[1];
            if (o.chance(0.2)) o.por(gx, 1, hz, Material.CHISELED_STONE_BRICKS);
            else o.por(gx, 1, hz, lapides[o.r.nextInt(lapides.length)]);
            if (o.chance(0.3)) o.por(gx, 2, hz, "candle[candles=" + (1 + o.r.nextInt(3)) + ",lit=false]");
            o.por(gx, 0, hz + 1, o.um(Material.COARSE_DIRT, 2, Material.PODZOL, 2, Material.ROOTED_DIRT, 1));
            o.por(gx, 0, hz + 2, o.um(Material.COARSE_DIRT, 2, Material.PODZOL, 2, Material.ROOTED_DIRT, 1));
            if (o.chance(0.25)) o.por(gx, 1, hz + 1 + o.r.nextInt(2), o.um(Material.POPPY, 2, Material.DEAD_BUSH, 2, Material.LILY_OF_THE_VALLEY, 1));
        }
        // Árvore morta e um monte de ossos.
        for (int dy = 1; dy <= 4; dy++) o.por(5, dy, -5, "dark_oak_log[axis=y]");
        o.por(4, 4, -5, "dark_oak_log[axis=x]");
        o.por(6, 3, -5, "dark_oak_log[axis=x]");
        o.por(5, 5, -4, "dark_oak_log[axis=z]");
        o.por(-5, 1, -5, "bone_block[axis=y]");
        o.por(-4, 1, -5, "bone_block[axis=x]");
        o.por(-5, 2, -5, "skeleton_skull[rotation=0]");
        cripta(o);
    }

    private void cripta(Obra o) {
        // Câmara subterrânea (x -3..3, z -7..-1, y -5..-1): casca de pedra, oco por dentro.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -7; dz <= -1; dz++) {
                for (int dy = -5; dy <= -1; dy++) {
                    boolean casca = Math.abs(dx) == 3 || dz == -7 || dz == -1 || dy == -5 || dy == -1;
                    o.por(dx, dy, dz, casca ? pedra(o) : Material.AIR);
                }
            }
        }
        for (int dy = -4; dy <= -2; dy++) o.por(0, dy, -6, Material.STONE_BRICKS);
        o.por(2, -4, -6, "chest[facing=west]");
        List<ItemStack> extras = new ArrayList<>();
        extras.add(new ItemStack(Material.BONE, 3 + o.r.nextInt(6)));
        if (o.chance(0.35)) extras.add(Raro.PAGINA_DE_LENDA.criar(1));
        if (o.chance(0.2)) extras.add(plugin.arqueologia().reliquiaAleatoria());
        if (o.chance(0.25)) extras.add(Raro.MAPA_RASGADO.criar(1));
        bau(o.bloco(2, -4, -6), LootTables.SIMPLE_DUNGEON, o, extras);
        o.por(-2, -4, -4, Material.POLISHED_ANDESITE);
        o.por(-2, -4, -3, Material.POLISHED_ANDESITE);
        o.por(-2, -3, -4, "skeleton_skull[rotation=4]");
        o.por(2, -4, -2, "soul_lantern[hanging=false]");
        o.por(-2, -4, -6, "soul_lantern[hanging=false]");
        o.por(-1, -4, -6, "candle[candles=3,lit=true]");
        o.por(1, -4, -2, "bone_block[axis=y]");
        if (o.chance(0.6)) o.por(-2, -2, -2, Material.COBWEB);
        if (o.chance(0.6)) o.por(2, -2, -5, Material.COBWEB);

        // Mausoléu na superfície (x -2..2, z -7..-3).
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -7; dz <= -3; dz++) {
                boolean parede = Math.abs(dx) == 2 || dz == -7 || dz == -3;
                o.por(dx, 0, dz, parede ? Material.STONE_BRICKS : o.um(Material.POLISHED_ANDESITE, 3, Material.ANDESITE, 1));
                for (int dy = 1; dy <= 3; dy++) {
                    if (!parede) { o.por(dx, dy, dz, Material.AIR); continue; }
                    boolean frente = dz == -3 && Math.abs(dx) == 2;
                    o.por(dx, dy, dz, frente ? Material.CHISELED_STONE_BRICKS : pedra(o));
                }
            }
        }
        o.por(0, 1, -3, Material.AIR);
        o.por(0, 2, -3, Material.AIR);
        o.por(0, 3, -3, Material.CHISELED_STONE_BRICKS);
        o.por(0, 3, -2, "skeleton_wall_skull[facing=south]");
        // Telhado de duas águas, com um beiral na frente.
        for (int dz = -7; dz <= -2; dz++) {
            o.por(-3, 4, dz, "stone_brick_stairs[facing=east]");
            o.por(3, 4, dz, "stone_brick_stairs[facing=west]");
            o.por(-2, 5, dz, "stone_brick_stairs[facing=east]");
            o.por(2, 5, dz, "stone_brick_stairs[facing=west]");
            o.por(-1, 6, dz, "stone_brick_stairs[facing=east]");
            o.por(1, 6, dz, "stone_brick_stairs[facing=west]");
            o.por(0, 6, dz, "stone_brick_slab[type=bottom]");
            for (int dx = -2; dx <= 2; dx++) o.por(dx, 4, dz, dz == -2 ? "stone_brick_slab[type=top]" : "stone_bricks");
            for (int dx = -1; dx <= 1; dx++) o.por(dx, 5, dz, dz == -2 ? "stone_brick_slab[type=top]" : "stone_bricks");
        }
        // Dentro: o altar com velas e o alçapão (escada) para a câmara.
        o.por(0, 1, -6, Material.CHISELED_STONE_BRICKS);
        o.por(0, 2, -6, "candle[candles=4,lit=true]");
        o.por(-1, 1, -6, "candle[candles=2,lit=true]");
        o.por(1, 1, -6, "candle[candles=1,lit=true]");
        if (o.chance(0.6)) o.por(1, 3, -4, Material.COBWEB);
        for (int dy = -4; dy <= 0; dy++) o.por(0, dy, -5, "ladder[facing=south]");
    }

    // =====================================================================
    //  Mina Abandonada
    // =====================================================================

    private boolean firme(Block b) {
        Material m = b.getType();
        return m.isSolid() && m.isOccluding() && m != Material.GRAVEL && m != Material.SAND && m != Material.RED_SAND
                && !m.name().endsWith("CONCRETE_POWDER");
    }

    private Material rocha(Obra o, int yMundo) {
        return yMundo < 0 ? o.um(Material.DEEPSLATE, 3, Material.COBBLED_DEEPSLATE, 2, Material.TUFF, 1)
                : o.um(Material.STONE, 3, Material.COBBLESTONE, 2, Material.ANDESITE, 1);
    }

    private Material minerio(Obra o, int yMundo) {
        boolean fundo = yMundo < 0;
        Material m = o.um(Material.COAL_ORE, 4, Material.IRON_ORE, 4, Material.COPPER_ORE, 3, Material.GOLD_ORE, 1,
                Material.REDSTONE_ORE, yMundo < 16 ? 2 : 0, Material.LAPIS_ORE, yMundo < 32 ? 1 : 0);
        return fundo ? Material.valueOf("DEEPSLATE_" + m.name()) : m;
    }

    private void mina(Obra o, int d) {
        // ---- superfície: terreiro, cavalete sobre o poço, placa de aviso ----
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (o.chance(0.6)) o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 3, Material.GRAVEL, 1, Material.COBBLESTONE, 1));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                o.por(dx, 0, dz, Math.abs(dx) == 1 && Math.abs(dz) == 1 ? "spruce_planks" : "cobblestone");
                if (!(dx == 0 && dz == 1)) o.por(dx, 1, dz, Material.SPRUCE_FENCE);
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) for (int dy = 1; dy <= 3; dy++) o.por(dx, dy, dz, "spruce_log[axis=y]");
        }
        for (int dx = -2; dx <= 2; dx++) {
            o.por(dx, 4, -2, "spruce_log[axis=x]");
            o.por(dx, 4, 2, "spruce_log[axis=x]");
            o.por(dx, 4, 0, "spruce_log[axis=x]");
        }
        for (int dz : new int[]{-1, 1}) {
            o.por(-2, 4, dz, "spruce_log[axis=z]");
            o.por(2, 4, dz, "spruce_log[axis=z]");
        }
        o.por(0, 3, 0, "lantern[hanging=true]");
        o.por(-1, 1, 3, "spruce_sign[rotation=0]");
        placa(o.bloco(-1, 1, 3), "⚠ MINA FECHADA ⚠", "desabamento", "não desça", "— Guilda dos Mineiros");
        o.por(3, 1, -3, Material.COBBLESTONE);
        o.por(3, 1, -2, Material.GRAVEL);
        o.por(2, 1, -3, Material.ANDESITE);
        o.por(3, 2, -3, Material.GRAVEL);
        o.por(-3, 1, -2, "barrel[facing=up]");
        o.por(-3, 1, -1, "barrel[facing=up]");

        // ---- poço: revestido, com escada até o fundo ----
        int chao = -d; // altura dos pés no túnel
        for (int dy = chao + 3; dy <= -1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    boolean canto = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                    o.por(dx, dy, dz, canto ? Material.STRIPPED_SPRUCE_LOG : rocha(o, o.y + dy));
                }
            }
        }
        // ---- túneis: um em x (z -1..1) e um em z (x -1..1), 3 de altura ----
        List<int[]> ocos = new ArrayList<>();
        for (int a = -6; a <= 6; a++) {
            for (int b = -1; b <= 1; b++) {
                ocos.add(new int[]{a, b});
                ocos.add(new int[]{b, a});
            }
        }
        // Casca: só onde não é rocha firme (fecha cavernas, água e lava).
        for (int[] c : ocos) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    for (int dy = chao - 1; dy <= chao + 3; dy++) {
                        int px = c[0] + ox, pz = c[1] + oz;
                        if (Math.abs(px) > 7 || Math.abs(pz) > 7) continue;
                        Block b = o.bloco(px, dy, pz);
                        if (!firme(b)) o.por(px, dy, pz, rocha(o, b.getY()));
                    }
                }
            }
        }
        for (int[] c : ocos) for (int dy = chao; dy <= chao + 2; dy++) o.por(c[0], dy, c[1], Material.AIR);
        // Minérios nas paredes.
        for (int i = 0; i < 14; i++) {
            boolean emX = o.r.nextBoolean();
            int ao = -5 + o.r.nextInt(11), lado = o.r.nextBoolean() ? 2 : -2, dy = chao + o.r.nextInt(3);
            if (Math.abs(ao) <= 1) continue;
            int px = emX ? ao : lado, pz = emX ? lado : ao;
            Block b = o.bloco(px, dy, pz);
            if (b.getType().isOccluding() && !b.getType().name().endsWith("_ORE")) o.por(px, dy, pz, minerio(o, b.getY()));
        }
        // Escoras de madeira a cada 3 blocos, com lanternas entre elas.
        for (int a : new int[]{-5, -2, 2, 5}) {
            for (int s : new int[]{-1, 1}) {
                o.por(a, chao, s, Material.SPRUCE_FENCE);
                o.por(a, chao + 1, s, Material.SPRUCE_FENCE);
                o.por(s, chao, a, Material.SPRUCE_FENCE);
                o.por(s, chao + 1, a, Material.SPRUCE_FENCE);
            }
            for (int b = -1; b <= 1; b++) {
                o.por(a, chao + 2, b, Material.SPRUCE_PLANKS);
                o.por(b, chao + 2, a, Material.SPRUCE_PLANKS);
            }
        }
        for (int a : new int[]{-4, 4}) {
            if (o.chance(0.7)) o.por(a, chao + 2, 0, "lantern[hanging=true]");
            if (o.chance(0.7)) o.por(0, chao + 2, a, "lantern[hanging=true]");
        }
        // Teias nos cantos de cima.
        for (int[] c : ocos) {
            if ((Math.abs(c[0]) == 1 || Math.abs(c[1]) == 1) && o.chance(0.08)) {
                Block b = o.bloco(c[0], chao + 2, c[1]);
                if (b.isEmpty()) o.por(c[0], chao + 2, c[1], Material.COBWEB);
            }
        }
        // Escada do poço presa num pilar de madeira no túnel.
        for (int dy = chao; dy <= chao + 2; dy++) o.por(0, dy, -1, Material.STRIPPED_SPRUCE_LOG);
        for (int dy = chao; dy <= 0; dy++) o.por(0, dy, 0, "ladder[facing=south]");
        // Trilhos no túnel leste-oeste e no ramal sul, com o carrinho de carga no fim.
        for (int a = -5; a <= 5; a++) if (a != 0) o.por(a, chao, 0,"rail[shape=east_west]");
        for (int a = 3; a <= 4; a++) o.por(0, chao, a, "rail[shape=north_south]");
        // Desabamentos nas pontas.
        for (int[] ponta : new int[][]{{6, 0}, {-6, 0}, {0, -6}, {0, 6}}) {
            boolean emX = ponta[1] == 0;
            for (int b = -1; b <= 1; b++) {
                int px = emX ? ponta[0] : b, pz = emX ? b : ponta[1];
                o.por(px, chao, pz, o.um(Material.GRAVEL, 2, Material.COBBLESTONE, 1));
                if (b == 0 || o.chance(0.5)) o.por(px, chao + 1, pz, o.um(Material.GRAVEL, 2, Material.COBBLESTONE, 1));
            }
        }
        // Baú das ferramentas no ramal norte.
        o.por(1, chao, -4, "chest[facing=west]");
        bau(o.bloco(1, chao, -4), LootTables.VILLAGE_TOOLSMITH, o, List.of(new ItemStack(Material.TORCH, 6 + o.r.nextInt(10))));
        // Carrinho de carga (entidade) no ramal sul.
        Block trilho = o.bloco(0, chao, 4);
        List<ItemStack> carga = new ArrayList<>();
        carga.add(new ItemStack(Material.EMERALD, 2 + o.r.nextInt(5)));
        if (o.chance(0.35)) carga.add(Mitrilo.bruto(1 + o.r.nextInt(2)));
        if (o.chance(0.2)) carga.add(Raro.MAPA_RASGADO.criar(1));
        Inventory temp = org.bukkit.Bukkit.createInventory(null, 27);
        encher(temp, LootTables.ABANDONED_MINESHAFT, o, trilho, carga);
        trilho.getWorld().spawn(trilho.getLocation().add(0.5, 0.1, 0.5), StorageMinecart.class, c -> {
            c.customName(Component.text("Carrinho de minério", NamedTextColor.GRAY));
            c.getInventory().setContents(temp.getContents());
        });
    }

    static void placa(Block b, String... linhas) {
        if (!(b.getState() instanceof Sign s)) return;
        for (int i = 0; i < linhas.length && i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(linhas[i]));
        s.setWaxed(true);
        s.update(true, false);
    }

    // =====================================================================
    //  Poço dos Desejos
    // =====================================================================

    private void poco(Obra o) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 3 && o.chance(0.7)) {
                    o.por(dx, 0, dz, o.um(Material.COBBLESTONE, 2, Material.MOSSY_COBBLESTONE, 2, Material.DIRT_PATH, 3));
                }
            }
        }
        // Bacia (3 de fundo) e a borda.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean borda = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                for (int dy = -3; dy <= 0; dy++) {
                    if (borda || dy == -3) o.por(dx, dy, dz, dy == -3 && !borda ? Material.MOSSY_COBBLESTONE : pedra(o));
                    else o.por(dx, dy, dz, Material.WATER);
                }
                if (borda) o.por(dx, 1, dz, o.um(Material.STONE_BRICKS, 3, Material.MOSSY_STONE_BRICKS, 2));
            }
        }
        // Postes, telhadinho e a corrente com a lanterna.
        for (int lado : new int[]{-2, 2}) {
            o.por(lado, 2, 0, Material.SPRUCE_FENCE);
            o.por(lado, 3, 0, Material.SPRUCE_FENCE);
        }
        for (int dz = -2; dz <= 2; dz++) {
            o.por(-2, 4, dz, "spruce_stairs[facing=east]");
            o.por(2, 4, dz, "spruce_stairs[facing=west]");
            o.por(-1, 5, dz, "spruce_stairs[facing=east]");
            o.por(1, 5, dz, "spruce_stairs[facing=west]");
            o.por(0, 6, dz, "spruce_slab[type=bottom]");
        }
        o.por(0, 5, 0, "iron_chain[axis=y]");
        o.por(0, 4, 0, "iron_chain[axis=y]");
        o.por(0, 3, 0, "lantern[hanging=true]");
        // Flores em volta, um banco e um poste com lanterna.
        Material[] flores = {Material.POPPY, Material.DANDELION, Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.ALLIUM};
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 4 || !o.chance(0.3)) continue;
                Material chao = o.bloco(dx, 0, dz).getType();
                if (chao == Material.GRASS_BLOCK || chao == Material.DIRT || chao == Material.PODZOL || chao == Material.COARSE_DIRT) {
                    o.por(dx, 1, dz, flores[o.r.nextInt(flores.length)]);
                }
            }
        }
        o.por(-1, 1, 4, "spruce_stairs[facing=south]");
        o.por(0, 1, 4, "spruce_stairs[facing=south]");
        o.por(3, 1, 3, Material.SPRUCE_FENCE);
        o.por(3, 2, 3, Material.SPRUCE_FENCE);
        o.por(3, 3, 3, "lantern[hanging=false]");
    }

    // =====================================================================
    //  Cabana do Eremita
    // =====================================================================

    private void cabana(Obra o) {
        // Alicerce, paredes de tábuas com toras nos cantos, janelas e porta.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean parede = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                boolean canto = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                o.por(dx, 0, dz, parede ? o.um(Material.COBBLESTONE, 2, Material.MOSSY_COBBLESTONE, 2) : Material.SPRUCE_PLANKS);
                for (int dy = 1; dy <= 4; dy++) {
                    if (canto) o.por(dx, dy, dz, "spruce_log[axis=y]");
                    else if (parede) o.por(dx, dy, dz, Material.SPRUCE_PLANKS);
                    else o.por(dx, dy, dz, Material.AIR);
                }
            }
        }
        o.por(2, 2, 0, Material.GLASS_PANE);
        o.por(-2, 2, 0, Material.GLASS_PANE);
        o.por(0, 2, -2, Material.GLASS_PANE);
        o.por(0, 1, 2, "spruce_door[facing=north,half=lower,hinge=left]");
        o.por(0, 2, 2, "spruce_door[facing=north,half=upper,hinge=left]");
        // Telhado de carvalho escuro (com beiral) e as empenas.
        for (int dz = -3; dz <= 3; dz++) {
            o.por(-3, 4, dz, "dark_oak_stairs[facing=east]");
            o.por(3, 4, dz, "dark_oak_stairs[facing=west]");
            o.por(-2, 5, dz, "dark_oak_stairs[facing=east]");
            o.por(2, 5, dz, "dark_oak_stairs[facing=west]");
            o.por(-1, 6, dz, "dark_oak_stairs[facing=east]");
            o.por(1, 6, dz, "dark_oak_stairs[facing=west]");
            o.por(0, 7, dz, "dark_oak_slab[type=bottom]");
        }
        for (int dz : new int[]{-2, 2}) {
            for (int dx = -1; dx <= 1; dx++) o.por(dx, 5, dz, Material.SPRUCE_PLANKS);
            o.por(0, 6, dz, Material.SPRUCE_PLANKS);
        }
        for (int dx = -1; dx <= 1; dx++) o.por(dx, 5, 0, "stripped_spruce_log[axis=x]");
        o.por(0, 4, 0, "lantern[hanging=true]");
        // Dentro: cama, o alambique, o baú de ervas e um vaso.
        o.por(-1, 1, -1, "red_bed[facing=north,part=head]");
        o.por(-1, 1, 0, "red_bed[facing=north,part=foot]");
        o.por(1, 1, -1, Material.BREWING_STAND);
        o.por(1, 1, 0, "barrel[facing=up]");
        List<ItemStack> ervas = new ArrayList<>();
        ervas.add(Erva.values()[o.r.nextInt(Erva.values().length)].criar(2 + o.r.nextInt(3)));
        ervas.add(new ItemStack(Material.GLASS_BOTTLE, 2 + o.r.nextInt(3)));
        if (o.chance(0.4)) ervas.add(Fruta.sortear(o.r).fruta(2));
        bau(o.bloco(1, 1, 0), null, o, ervas);
        o.por(1, 1, 1, Material.POTTED_RED_MUSHROOM);
        o.por(0, 1, 0, Material.GREEN_CARPET);
        o.por(0, 1, 1, Material.GREEN_CARPET);
        // Fora: horta cercada a sudoeste, lenha empilhada a leste, lanterna na porta e a trilha.
        for (int dx = -6; dx <= -3; dx++) {
            for (int dz = 2; dz <= 6; dz++) {
                boolean cerca = dx == -6 || dx == -3 || dz == 2 || dz == 6;
                if (cerca) {
                    if (dx == -3 && dz == 4) o.por(dx, 1, dz, "spruce_fence_gate[facing=east]");
                    else o.por(dx, 1, dz, Material.SPRUCE_FENCE);
                } else {
                    o.por(dx, 0, dz, o.um(Material.MOSS_BLOCK, 2, Material.ROOTED_DIRT, 1));
                    if (o.chance(0.7)) o.por(dx, 1, dz, o.um(Material.FERN, 2, Material.ALLIUM, 1, Material.CORNFLOWER, 1,
                            Material.AZURE_BLUET, 1, Material.LILY_OF_THE_VALLEY, 1));
                }
            }
        }
        for (int dz = -2; dz <= 0; dz++) o.por(4, 1, dz, "spruce_log[axis=z]");
        for (int dz = -1; dz <= 0; dz++) o.por(4, 2, dz, "spruce_log[axis=z]");
        o.por(4, 1, 2, "spruce_log[axis=y]");
        o.por(3, 1, -3, Material.COMPOSTER);
        o.por(2, 1, 3, Material.SPRUCE_FENCE);
        o.por(2, 2, 3, Material.SPRUCE_FENCE);
        o.por(2, 3, 3, "lantern[hanging=false]");
        for (int dz = 3; dz <= 6; dz++) o.por(0, 0, dz, o.um(Material.DIRT_PATH, 3, Material.COARSE_DIRT, 1));
    }

    // =====================================================================
    //  Santuário Esquecido
    // =====================================================================

    /** Os blocos de cada deus: pisos, laje, pilar, base e topo do altar, luz e vela. */
    private record Paleta(Material p1, Material p2, Material p3, String laje, String pilar, String base, String topo, String luz, String vela) { }

    private static Paleta paleta(Deus d) {
        return switch (d) {
            case FERRUM -> new Paleta(Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE, Material.CRACKED_DEEPSLATE_TILES,
                    "deepslate_tile_slab[type=bottom]", "deepslate_bricks", "chiseled_deepslate", "chipped_anvil[facing=east]",
                    "lantern[hanging=false]", "orange_candle");
            case SYLVA -> new Paleta(Material.MOSSY_STONE_BRICKS, Material.MOSS_BLOCK, Material.STONE_BRICKS,
                    "mossy_stone_brick_slab[type=bottom]", "stripped_oak_log[axis=y]", "moss_block", "flowering_azalea",
                    "shroomlight", "lime_candle");
            case MARIS -> new Paleta(Material.PRISMARINE_BRICKS, Material.PRISMARINE, Material.DARK_PRISMARINE,
                    "prismarine_brick_slab[type=bottom]", "prismarine_bricks", "dark_prismarine", "sea_lantern",
                    "sea_lantern", "light_blue_candle");
            case BELLUM -> new Paleta(Material.POLISHED_GRANITE, Material.GRANITE, Material.STONE_BRICKS,
                    "polished_granite_slab[type=bottom]", "red_nether_bricks", "polished_blackstone", "red_banner[rotation=0]",
                    "lantern[hanging=false]", "red_candle");
            case ARCANUS -> new Paleta(Material.PURPUR_BLOCK, Material.QUARTZ_BRICKS, Material.SMOOTH_QUARTZ,
                    "purpur_slab[type=bottom]", "purpur_pillar[axis=y]", "quartz_pillar[axis=y]", "amethyst_cluster[facing=up]",
                    "end_rod[facing=up]", "purple_candle");
            case MORTIS -> new Paleta(Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                    "polished_blackstone_brick_slab[type=bottom]", "polished_blackstone", "soul_soil", "skeleton_skull[rotation=0]",
                    "soul_lantern[hanging=false]", "gray_candle");
        };
    }

    private void santuario(Obra o, Deus d) {
        Paleta p = paleta(d);
        // Piso redondo, gasto pelo tempo.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int r2 = dx * dx + dz * dz;
                if (r2 > 17) continue;
                if (r2 >= 13 && o.chance(0.25)) continue;
                o.por(dx, 0, dz, o.um(p.p1(), 5, p.p2(), 3, p.p3(), 2));
            }
        }
        // Estrado e altar.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) o.por(dx, 1, dz, p.laje());
        o.por(0, 1, 0, p.base());
        o.por(0, 2, 0, p.topo());
        for (int dx : new int[]{-2, 2}) for (int dz : new int[]{-2, 2}) o.por(dx, 1, dz, p.vela() + "[candles=3,lit=true]");
        // Pilares: alguns inteiros (com luz no alto), outros partidos.
        int[][] pilares = {{0, -4}, {-3, -2}, {3, -2}, {-3, 2}, {3, 2}};
        for (int i = 0; i < pilares.length; i++) {
            int[] c = pilares[i];
            int h = i == 0 ? 4 : 2 + o.r.nextInt(3);
            for (int dy = 1; dy <= h; dy++) o.por(c[0], dy, c[1], p.pilar());
            if (h == 4) o.por(c[0], 5, c[1], p.luz());
        }
        // Pedaços de pilar caídos no chão em volta.
        for (int i = 0; i < 4; i++) {
            double ang = o.r.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(ang) * 4.6), dz = (int) Math.round(Math.sin(ang) * 4.6);
            if (Math.abs(dx) > 5 || Math.abs(dz) > 5) continue;
            if (o.bloco(dx, 1, dz).isEmpty() && o.bloco(dx, 0, dz).getType().isSolid()) o.por(dx, 1, dz, p.pilar());
        }
        if (d == Deus.SYLVA) {
            for (int i = 0; i < 6; i++) {
                int dx = -4 + o.r.nextInt(9), dz = -4 + o.r.nextInt(9);
                if (o.bloco(dx, 1, dz).isEmpty() && o.bloco(dx, 0, dz).getType() == Material.MOSS_BLOCK) {
                    o.por(dx, 1, dz, o.um(Material.AZALEA, 1, Material.FERN, 2, Material.POPPY, 1));
                }
            }
        }
        // A inscrição: um púlpito com o livro do santuário.
        o.por(0, 1, 3, "lectern[facing=south]");
        if (o.bloco(0, 1, 3).getState(false) instanceof Lectern lec) lec.getInventory().setItem(0, inscricao(d));
    }

    static ItemStack inscricao(Deus d) {
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text("Inscrição de " + d.nome()));
            m.author(Component.text("os antigos"));
            m.pages(Component.text("Santuário Esquecido de " + d.nome() + ", " + d.titulo() + ".\n\n"
                            + "Antes dos reinos, os antigos subiam até aqui para rezar. As pedras caíram, mas "
                            + d.nome() + " ainda escuta."),
                    Component.text("Quem segue " + d.nome() + " e reza neste altar (clique nele) recebe o dobro da devoção "
                            + "de uma prece comum, e uma bênção de " + d.nome() + ".\n\nUma vez por dia."));
        });
        return livro;
    }

    // =====================================================================
    //  Baús
    // =====================================================================

    /** Enche um baú ou barril com o saque da tabela vanilla (se houver) e os extras do plugin. */
    void bau(Block b, LootTables tabela, Obra o, List<ItemStack> extras) {
        BlockState st = b.getState(false);
        if (!(st instanceof Container c)) return;
        Inventory inv = st instanceof org.bukkit.block.Chest ch ? ch.getBlockInventory() : c.getInventory();
        encher(inv, tabela, o, b, extras);
    }

    void encher(Inventory inv, LootTables tabela, Obra o, Block onde, List<ItemStack> extras) {
        List<ItemStack> itens = new ArrayList<>();
        if (tabela != null) {
            try {
                itens.addAll(tabela.getLootTable().populateLoot(o.r, new LootContext.Builder(onde.getLocation()).build()));
            } catch (RuntimeException ex) {
                plugin.getLogger().fine("Saque " + tabela + " falhou: " + ex.getMessage());
            }
        }
        itens.addAll(extras);
        ItemStack nomeada = plugin.nomeadas() == null ? null : plugin.nomeadas().talvezParaBau();
        if (nomeada != null) itens.add(nomeada);
        List<Integer> vagas = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) vagas.add(i);
        Collections.shuffle(vagas, o.r);
        int i = 0;
        for (ItemStack it : itens) {
            if (it == null || it.isEmpty()) continue;
            if (i < vagas.size()) inv.setItem(vagas.get(i++), it);
            else inv.addItem(it);
        }
    }
}
