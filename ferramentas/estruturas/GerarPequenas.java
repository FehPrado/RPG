import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * As estruturas pequenas do datapack (3.2): coisas que se acham pelo caminho, com arqueologia.
 * Uso: java GerarPequenas <datapack/data/rpgatributos/structure> <pasta prévias>
 * O y 0 de cada molde é o chão (menos onde a estrutura começa abaixo dele: veja Gigantes.superficie).
 */
public class GerarPequenas {

    static final String L = "rpgatributos:chests/";

    static String sus(String tabela) { return "{id:\"minecraft:brushable_block\",LootTable:\"rpgatributos:archaeology/" + tabela + "\"}"; }

    /** Areia ou cascalho suspeito (para o pincel) com a tabela de escavação. */
    static void suspeito(Molde m, int x, int y, int z, boolean areia, String tabela) {
        m.setNbt(x, y, z, (areia ? "suspicious_sand" : "suspicious_gravel") + "[dusted=0]", sus(tabela));
    }

    public static void main(String[] a) throws Exception {
        File dir = new File(a[0]), prev = new File(a[1]);
        Map<String, Molde> todos = new LinkedHashMap<>();
        todos.put("acampamento_cacadores", cacadores());
        todos.put("estatua_quebrada", estatua());
        todos.put("arco_antigo", arco());
        todos.put("santuario_rio", santuarioRio());
        todos.put("cadafalso", cadafalso());
        todos.put("cripta_poco", criptaPoco());
        todos.put("caravana_perdida", caravana());
        todos.put("sitio_escavacao", sitio());
        todos.put("fenda_abismo", fenda());
        for (var e : todos.entrySet()) {
            e.getValue().gravar(new File(dir, e.getKey() + ".nbt"));
            Previa.salvar(e.getValue(), new File(prev, e.getKey() + ".png"));
            System.out.println(e.getKey() + ": " + e.getValue().sx + "x" + e.getValue().sy + "x" + e.getValue().sz + ", " + e.getValue().contar() + " blocos");
        }
    }

    /** Uma barraca em A de lã (frente aberta no z0), de x0 a x0+4. */
    static void barraca(Molde m, int x0, int y0, int z0, int z1, String la) {
        for (int z = z0; z <= z1; z++) {
            m.set(x0, y0, z, la);
            m.set(x0 + 4, y0, z, la);
            m.set(x0 + 1, y0 + 1, z, la);
            m.set(x0 + 3, y0 + 1, z, la);
            m.set(x0 + 2, y0 + 2, z, la);
            m.set(x0 + 1, y0, z, "air");
            m.set(x0 + 2, y0, z, "air");
            m.set(x0 + 3, y0, z, "air");
            m.set(x0 + 2, y0 + 1, z, "air");
        }
        for (int x = x0 + 1; x <= x0 + 3; x++) m.set(x, y0, z1, la);
        m.set(x0 + 2, y0 + 1, z1, la);
    }

    // =====================================================================

    static Molde cacadores() {
        Molde m = new Molde(15, 8, 15, 701);
        int c = 7;
        for (int x = 0; x < 15; x++) for (int z = 0; z < 15; z++) {
            if (Math.hypot(x - c, z - c) > 7) continue;
            m.set(x, 0, z, m.mix(0.5, "podzol", "coarse_dirt", "dirt"));
            for (int y = 1; y < 6; y++) m.set(x, y, z, "air");
        }
        barraca(m, 1, 1, 3, 8, "brown_wool");
        barraca(m, 9, 1, 2, 6, "green_wool");
        m.barril(3, 1, 7, L + "cacadores");
        m.set(2, 1, 7, "red_bed[facing=south,occupied=false,part=foot]");
        m.set(2, 1, 8, "air");
        m.set(11, 1, 5, "crafting_table");
        // Fogueira com bancos.
        m.set(7, 1, 10, "campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]");
        m.set(5, 1, 10, "stripped_spruce_log[axis=z]");
        m.set(9, 1, 10, "stripped_spruce_log[axis=z]");
        m.set(7, 1, 12, "stripped_spruce_log[axis=x]");
        // Varal de secar couro (cerca e lajes) e a lanterna.
        m.fill(12, 1, 9, 12, 2, 9, "spruce_fence[east=false,north=false,south=false,west=false,waterlogged=false]");
        m.fill(12, 1, 12, 12, 2, 12, "spruce_fence[east=false,north=false,south=false,west=false,waterlogged=false]");
        m.fill(12, 3, 9, 12, 3, 12, "spruce_slab[type=bottom,waterlogged=false]");
        m.set(12, 2, 10, "white_carpet");
        m.set(12, 3, 9, "lantern[hanging=false,waterlogged=false]");
        m.set(4, 1, 12, "hay_block[axis=x]");
        m.set(3, 1, 12, "barrel[facing=up,open=false]");
        return m;
    }

    static Molde estatua() {
        Molde m = new Molde(13, 11, 13, 702);
        Molde.Pal pedra = (x, y, z) -> m.mix(0.5, "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks");
        for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) {
            if (Math.hypot(x - 6, z - 6) > 6.2) continue;
            m.set(x, 0, z, m.mix(0.6, "grass_block[snowy=false]", "coarse_dirt", "gravel"));
            if (m.r.nextDouble() < 0.12) GerarPequenas.suspeito(m, x, 0, z, false, "estatua");
            for (int y = 1; y < 11; y++) m.set(x, y, z, "air");
        }
        m.fill(4, 1, 4, 8, 2, 8, pedra);
        m.fill(5, 3, 5, 7, 3, 7, "chiseled_stone_bricks");
        // Só as pernas e o quadril ficaram em pé.
        m.fill(5, 4, 6, 5, 7, 6, "smooth_stone");
        m.fill(7, 4, 6, 7, 7, 6, "smooth_stone");
        m.fill(5, 8, 5, 7, 9, 7, "smooth_stone");
        m.set(5, 9, 5, "air");
        m.set(7, 9, 7, "air");
        // A cabeça caída, o braço e pedaços.
        m.elipsoide(10, 2, 10, 2.2, 2.2, 2.2, 0.15, (x, y, z) -> m.mix(0.6, "smooth_stone", "mossy_cobblestone", "andesite"));
        m.set(9, 2, 9, "air");
        m.linha(1, 1, 9, 4, 1, 11, 0.6, (x, y, z) -> "smooth_stone");
        m.set(2, 1, 3, "cracked_stone_bricks");
        m.set(10, 1, 2, "mossy_stone_bricks");
        for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) {
            String s = m.get(x, 1, z);
            if ("minecraft:air".equals(s) && m.r.nextDouble() < 0.1) m.set(x, 1, z, "moss_carpet");
        }
        return m;
    }

    static Molde arco() {
        Molde m = new Molde(15, 14, 7, 703);
        Molde.Pal pedra = (x, y, z) -> m.mix(0.55, "sandstone", "cut_sandstone", "smooth_sandstone", "chiseled_sandstone");
        for (int x = 0; x < 15; x++) for (int z = 0; z < 7; z++) {
            m.set(x, 0, z, m.mix(0.7, "sand", "gravel", "coarse_dirt"));
            if (m.r.nextDouble() < 0.1) GerarPequenas.suspeito(m, x, 0, z, true, "arco");
            for (int y = 1; y < 14; y++) m.set(x, y, z, "air");
        }
        m.fill(2, 1, 2, 4, 10, 4, pedra);
        m.fill(10, 1, 2, 12, 10, 4, pedra);
        m.fill(2, 11, 2, 12, 12, 4, pedra);
        m.fill(5, 10, 3, 9, 10, 3, pedra);
        m.set(7, 12, 3, "chiseled_tuff");
        m.set(3, 6, 2, "chiseled_tuff");
        m.set(11, 6, 2, "chiseled_tuff");
        // O lado direito do topo caiu.
        m.fill(10, 11, 2, 12, 12, 4, "air");
        m.fill(11, 8, 2, 12, 10, 4, "air");
        m.set(13, 1, 5, "cut_sandstone");
        m.set(12, 1, 6, "smooth_sandstone");
        m.set(14, 1, 3, "chiseled_sandstone");
        return m;
    }

    static Molde santuarioRio() {
        // Começa 6 blocos abaixo da superfície da água: as estacas descem até o leito.
        Molde m = new Molde(11, 14, 11, 704);
        String estaca = "stripped_dark_oak_log[axis=y]";
        for (int x : new int[]{1, 9}) for (int z : new int[]{1, 9}) m.fill(x, 0, z, x, 9, z, estaca);
        m.fill(0, 6, 0, 10, 6, 10, (x, y, z) -> (x + z) % 2 == 0 ? "spruce_planks" : "dark_oak_planks");
        m.fill(0, 7, 0, 10, 12, 10, "air");
        m.fill(1, 7, 1, 9, 9, 1, (x, y, z) -> x == 1 || x == 9 ? estaca : "air");
        for (int x = 0; x <= 10; x++) { m.set(x, 7, 0, "dark_oak_fence[east=false,north=false,south=false,west=false,waterlogged=false]"); m.set(x, 7, 10, "dark_oak_fence[east=false,north=false,south=false,west=false,waterlogged=false]"); }
        for (int z = 0; z <= 10; z++) { m.set(0, 7, z, "dark_oak_fence[east=false,north=false,south=false,west=false,waterlogged=false]"); m.set(10, 7, z, "dark_oak_fence[east=false,north=false,south=false,west=false,waterlogged=false]"); }
        m.fill(4, 7, 0, 6, 7, 0, "air");
        // Telhado de duas camadas.
        m.fill(0, 10, 0, 10, 10, 10, "dark_oak_slab[type=bottom,waterlogged=false]");
        m.fill(2, 11, 2, 8, 11, 8, "dark_oak_slab[type=bottom,waterlogged=false]");
        m.fill(4, 12, 4, 6, 12, 6, "dark_oak_slab[type=bottom,waterlogged=false]");
        m.set(5, 13, 5, "lightning_rod[facing=up,powered=false,waterlogged=false]");
        // Altar com velas e a oferenda.
        m.set(5, 7, 6, "chiseled_stone_bricks");
        m.set(5, 8, 6, "candle[candles=3,lit=true,waterlogged=false]");
        m.bau(5, 7, 8, "north", L + "santuario_rio");
        m.set(2, 9, 2, "lantern[hanging=true,waterlogged=false]");
        m.set(8, 9, 8, "lantern[hanging=true,waterlogged=false]");
        m.set(3, 7, 7, "flower_pot");
        return m;
    }

    static Molde cadafalso() {
        Molde m = new Molde(11, 10, 11, 705);
        String madeira = "dark_oak_planks";
        for (int x = 0; x < 11; x++) for (int z = 0; z < 11; z++) {
            m.set(x, 0, z, m.mix(0.6, "coarse_dirt", "gravel", "dirt"));
            for (int y = 1; y < 10; y++) m.set(x, y, z, "air");
        }
        for (int x : new int[]{2, 6}) for (int z : new int[]{2, 6}) m.fill(x, 1, z, x, 2, z, "dark_oak_log[axis=y]");
        m.fill(2, 3, 2, 6, 3, 6, madeira);
        for (int z = 7; z <= 9; z++) m.set(4, 3 - (z - 6), z, "dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        // A forca.
        m.fill(3, 4, 3, 3, 8, 3, "dark_oak_log[axis=y]");
        m.fill(3, 8, 3, 5, 8, 3, "dark_oak_log[axis=x]");
        m.fill(5, 6, 3, 5, 7, 3, "iron_chain[axis=y,waterlogged=false]");
        m.set(5, 3, 3, "dark_oak_trapdoor[facing=north,half=top,open=true,powered=false,waterlogged=false]");
        m.bau(4, 1, 4, "south", L + "cadafalso");
        // A gaiola ao lado.
        m.fill(8, 1, 2, 9, 3, 3, "iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]");
        m.set(8, 1, 2, "bone_block[axis=y]");
        m.set(9, 4, 2, "dark_oak_slab[type=bottom,waterlogged=false]");
        m.set(8, 4, 3, "dark_oak_slab[type=bottom,waterlogged=false]");
        return m;
    }

    static Molde criptaPoco() {
        // A boca do poço fica no chão (y 15); a cripta, 10 blocos abaixo.
        Molde m = new Molde(11, 21, 11, 706);
        int c = 5, chao = 15;
        Molde.Pal pedra = (x, y, z) -> m.mix(0.55, "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "cobblestone");
        // Cripta.
        m.fill(0, 0, 0, 10, 6, 10, pedra);
        m.fill(1, 1, 1, 9, 5, 9, "air");
        m.fill(1, 0, 1, 9, 0, 9, (x, y, z) -> m.mix(0.7, "polished_andesite", "gravel", "coarse_dirt"));
        for (int x = 1; x <= 9; x++) for (int z = 1; z <= 9; z++) if (m.r.nextDouble() < 0.12) suspeito(m, x, 0, z, false, "cripta");
        for (int x : new int[]{2, 8}) {
            m.fill(x, 1, 2, x, 1, 4, "stone_brick_slab[type=top,waterlogged=false]");
            m.fill(x, 1, 6, x, 1, 8, "stone_brick_slab[type=top,waterlogged=false]");
        }
        m.bau(c, 1, 9, "north", L + "cripta_poco");
        for (int[] p : new int[][]{{1, 1}, {9, 1}, {1, 9}, {9, 9}}) m.set(p[0], 1, p[1], "candle[candles=2,lit=true,waterlogged=false]");
        m.set(3, 5, 3, "cobweb");
        m.set(7, 5, 7, "cobweb");
        m.set(c, 5, c, "air");
        // O poço: um tubo de pedra com escada de mão do teto da cripta até a boca.
        for (int y = 6; y <= chao; y++) {
            m.fill(c - 2, y, c - 2, c + 2, y, c + 2, pedra);
            m.fill(c - 1, y, c - 1, c + 1, y, c + 1, "air");
            m.set(c, y, c - 1, "ladder[facing=south,waterlogged=false]");
        }
        for (int y = 1; y <= 5; y++) m.set(c, y, c - 1, y >= 2 ? "ladder[facing=south,waterlogged=false]" : "air");
        m.set(c, 1, c - 2, "stone_bricks");
        // A boca: mureta, dois postes e o telhadinho.
        m.fill(c - 2, chao + 1, c - 2, c + 2, chao + 1, c + 2, (x, y, z) -> Math.abs(x - c) == 2 || Math.abs(z - c) == 2 ? "cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]" : "air");
        m.fill(c - 2, chao + 2, c, c - 2, chao + 3, c, "spruce_fence[east=false,north=false,south=false,west=false,waterlogged=false]");
        m.fill(c + 2, chao + 2, c, c + 2, chao + 3, c, "spruce_fence[east=false,north=false,south=false,west=false,waterlogged=false]");
        m.fill(c - 3, chao + 4, c - 1, c + 3, chao + 4, c + 1, "spruce_slab[type=bottom,waterlogged=false]");
        m.set(c, chao + 3, c, "iron_chain[axis=y,waterlogged=false]");
        m.set(c, chao + 2, c, "lantern[hanging=true,waterlogged=false]");
        return m;
    }

    static Molde caravana() {
        Molde m = new Molde(15, 7, 11, 707);
        for (int x = 0; x < 15; x++) for (int z = 0; z < 11; z++) {
            m.set(x, 0, z, m.mix(0.6, "dirt_path", "coarse_dirt", "gravel"));
            for (int y = 1; y < 7; y++) m.set(x, y, z, "air");
        }
        // A carroça tombada de lado: o fundo virou parede.
        m.fill(3, 1, 3, 3, 3, 8, "spruce_planks");
        m.fill(4, 1, 3, 8, 1, 3, "spruce_planks");
        m.fill(4, 1, 8, 8, 1, 8, "spruce_planks");
        m.fill(4, 3, 3, 8, 3, 3, "spruce_slab[type=bottom,waterlogged=false]");
        for (int[] r : new int[][]{{2, 2}, {2, 9}}) {
            m.set(r[0], 1, r[1], "stripped_dark_oak_log[axis=x]");
            m.set(r[0], 2, r[1], "dark_oak_trapdoor[facing=west,half=bottom,open=true,powered=false,waterlogged=false]");
        }
        m.set(7, 1, 5, "spruce_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]");
        m.bau(5, 1, 5, "east", L + "caravana_perdida");
        m.barril(10, 1, 4, L + "caravana_perdida");
        m.set(11, 1, 6, "barrel[facing=east,open=false]");
        m.set(9, 1, 8, "hay_block[axis=z]");
        // O boi que puxava: só os ossos.
        m.fill(11, 1, 1, 13, 1, 1, "bone_block[axis=x]");
        m.set(13, 2, 1, "bone_block[axis=y]");
        m.set(12, 1, 2, "bone_block[axis=z]");
        m.fill(1, 1, 5, 1, 1, 6, "spruce_fence[east=false,north=true,south=true,waterlogged=false,west=false]");
        return m;
    }

    /**
     * A entrada do Abismo: uma rachadura no chão, com ametista e obsidiana chorona nas paredes. O
     * topo (y 12) fica na altura do chão; quem cai lá dentro vai para o Abismo (o plugin cuida).
     */
    static Molde fenda() {
        Molde m = new Molde(9, 14, 21, 709);
        int cx = 4, topo = 12;
        for (int z = 0; z < 21; z++) {
            double t = (z - 10) / 10.0;
            double meia = 2.6 * (1 - t * t) + 0.3;
            double desvio = Math.sin(z * 0.7) * 0.8;
            for (int x = 0; x < 9; x++) {
                double d = Math.abs(x - cx - desvio);
                for (int y = 0; y <= topo + 1; y++) {
                    double larg = meia * (0.35 + 0.65 * y / (double) topo);
                    if (d <= larg) m.set(x, y, z, y <= 1 ? "black_concrete" : "air");
                    else if (d <= larg + 1.2 && y <= topo) m.set(x, y, z, m.mix(0.5, "obsidian", "crying_obsidian", "amethyst_block", "cobbled_deepslate"));
                }
            }
        }
        // Borda rachada no chão e cristais nas paredes.
        for (int z = 0; z < 21; z++) for (int x = 0; x < 9; x++) {
            String s = m.get(x, topo, z);
            if (s != null && !s.equals("minecraft:air") && m.r.nextDouble() < 0.5) m.set(x, topo, z, m.pick("cracked_deepslate_bricks", "cobbled_deepslate", "tuff"));
            for (int y = 3; y < topo; y++) if ("minecraft:air".equals(m.get(x, y, z)) && m.get(x - 1, y, z) != null && m.get(x - 1, y, z).contains("amethyst_block") && m.r.nextDouble() < 0.5)
                m.set(x, y, z, "amethyst_cluster[facing=east,waterlogged=false]");
        }
        return m;
    }

    static Molde sitio() {
        // Começa 4 blocos abaixo do chão: a vala da escavação.
        Molde m = new Molde(17, 10, 17, 708);
        int topo = 4;
        for (int x = 0; x < 17; x++) for (int z = 0; z < 17; z++) {
            boolean vala = x >= 3 && x <= 13 && z >= 3 && z <= 13;
            if (vala) {
                m.set(x, 0, z, m.mix(0.6, "coarse_dirt", "gravel", "sand"));
                if (m.r.nextDouble() < 0.25) suspeito(m, x, 0, z, m.r.nextBoolean(), "sitio");
                for (int y = 1; y < 10; y++) m.set(x, y, z, "air");
                // Degraus nas paredes da vala.
                if (x == 3 || x == 13 || z == 3 || z == 13) {
                    int h = (x + z) % 3;
                    for (int y = 1; y <= h; y++) m.set(x, y, z, m.mix(0.6, "coarse_dirt", "gravel"));
                }
            } else {
                m.set(x, topo - 1, z, "coarse_dirt");
                m.set(x, topo, z, "air");
                for (int y = topo + 1; y < 10; y++) m.set(x, y, z, "air");
            }
        }
        // Andaimes e escadas para descer.
        m.fill(4, 1, 4, 4, 6, 4, "scaffolding[bottom=false,distance=0,waterlogged=false]");
        m.fill(12, 1, 12, 12, 6, 12, "scaffolding[bottom=false,distance=0,waterlogged=false]");
        for (int y = 1; y <= topo; y++) m.set(8, y, 3, "ladder[facing=south,waterlogged=false]");
        // Barracas e o acampamento em cima.
        barraca(m, 0, topo, 0, 2, "white_wool");
        m.set(14, topo, 1, "crafting_table");
        m.barril(15, topo, 1, L + "sitio_escavacao");
        m.set(15, topo, 15, "campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]");
        m.set(1, topo, 15, "decorated_pot[cracked=false,facing=north,waterlogged=false]");
        m.set(2, topo, 15, "decorated_pot[cracked=false,facing=north,waterlogged=false]");
        return m;
    }
}
