package br.rpgatributos.estruturas;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Sign;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * O desenho de uma Ilha do Céu: a ilha principal (rocha com grama por cima e afinando para
 * baixo, com raízes penduradas), 3 ilhotas em volta, a construção do tipo dela e a
 * Plataforma de Vento no chão, que lança o jogador lá para cima. Tudo sai da semente, então
 * as etapas que faltam saem iguais se o servidor parar no meio da obra.
 *
 * Coordenadas locais: (0, 0, 0) é o meio da ilha principal, na altura do gramado.
 */
final class Ilha {

    /** Raio da ilha principal e até onde vão as ilhotas (para carregar os chunks). */
    static final int RAIO = 16, ALCANCE = 36;
    /** A plataforma fica à frente da ilha (+z local), longe da sombra dela. */
    static final int PLATAFORMA = RAIO + 12;
    static final int ETAPAS = 6;

    enum Tipo {
        JARDIM("Jardim Suspenso", "Uma árvore que nunca viu o chão floresce aqui em cima."),
        OBSERVATORIO("Observatório Antigo", "Daqui, os antigos liam as estrelas de perto."),
        FORJA("Forja dos Ventos", "O vento ainda sopra o fole de uma forja sem ferreiro."),
        NINHO("Ninho do Guardião", "Algo enorme fez ninho no alto. E não gosta de visitas.");

        final String nome, frase;

        Tipo(String nome, String frase) {
            this.nome = nome;
            this.frase = frase;
        }
    }

    static Tipo tipo(long semente) {
        return Tipo.values()[(int) Math.floorMod(semente >>> 7, Tipo.values().length)];
    }

    /** Uma ilha (a principal ou uma ilhota): centro local, raio e espessura. */
    record Corpo(int dx, int dy, int dz, int r, int espessura, long semente) { }

    static Corpo principal(long semente) {
        return new Corpo(0, 0, 0, RAIO, 24, semente);
    }

    static List<Corpo> ilhotas(long semente) {
        Random r = new Random(semente ^ 0x15107AL);
        List<Corpo> l = new ArrayList<>();
        // Nos lados e atrás (nunca à frente, +z, onde ficam a plataforma e o caminho do lançamento).
        for (int i = 0; i < 3; i++) {
            double ang = Math.toRadians(100 + i * 80 + (r.nextDouble() - 0.5) * 30);
            int dist = 24 + r.nextInt(6);
            int raio = 4 + r.nextInt(3);
            l.add(new Corpo((int) Math.round(Math.sin(ang) * dist), -9 + r.nextInt(14), (int) Math.round(Math.cos(ang) * dist),
                    raio, 9 + raio * 2, semente + i * 7919L));
        }
        return l;
    }

    // =====================================================================
    //  Ruído (para as bordas e o fundo não ficarem redondos demais)
    // =====================================================================

    private static double hash(long semente, int x, int z) {
        long h = semente * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) / (double) (1L << 53);
    }

    /** Ruído suave de 0 a 1, com "células" do tamanho {@code escala}. */
    static double ruido(long semente, double x, double z, double escala) {
        double fx = x / escala, fz = z / escala;
        int ix = (int) Math.floor(fx), iz = (int) Math.floor(fz);
        double tx = fx - ix, tz = fz - iz;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = hash(semente, ix, iz), b = hash(semente, ix + 1, iz), c = hash(semente, ix, iz + 1), d = hash(semente, ix + 1, iz + 1);
        return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz;
    }

    /** O topo e o fundo da coluna (dx, dz) de um corpo, em y local; null fora dele. */
    static int[] coluna(Corpo c, int dx, int dz) {
        int x = dx - c.dx(), z = dz - c.dz();
        double d = Math.sqrt(x * x + z * z);
        double borda = c.r() * (0.82 + 0.32 * ruido(c.semente(), x, z, 6));
        if (d > borda) return null;
        double f = d / borda;
        // Gramado quase plano no meio (onde vai a construção) e ondulado nas beiradas.
        int topo = d < 7 ? 0 : (int) Math.round(1.8 * ruido(c.semente() + 1, x, z, 5) - 0.9 * f);
        // Afina para baixo como um cone (a "raiz" de pedra que pende da ilha), com bico irregular.
        double fundo = c.espessura() * Math.pow(1 - f, 1.35) * (0.6 + 0.6 * ruido(c.semente() + 2, x, z, 3.5)) + 2 * (1 - f);
        return new int[]{c.dy() + topo, c.dy() + topo - Math.max(1, (int) Math.round(fundo))};
    }

    // =====================================================================
    //  Etapas da obra
    // =====================================================================

    static void etapa(int fase, Obra o, long semente) {
        switch (fase) {
            case 0 -> corpo(o, principal(semente), true);
            case 1 -> corpo(o, principal(semente), false);
            case 2 -> { for (Corpo c : ilhotas(semente)) { corpo(o, c, true); corpo(o, c, false); } }
            case 3 -> {
                enfeitar(o, principal(semente));
                for (Corpo c : ilhotas(semente)) enfeitar(o, c);
            }
            case 4 -> construcao(o, tipo(semente));
            case 5 -> ponte(o, semente);
            default -> { }
        }
    }

    /** A rocha da ilha, metade de cada vez ({@code norte}: dz ≤ 0). */
    private static void corpo(Obra o, Corpo c, boolean norte) {
        for (int x = c.dx() - c.r() - 4; x <= c.dx() + c.r() + 4; x++) {
            for (int z = c.dz() - c.r() - 4; z <= c.dz() + c.r() + 4; z++) {
                if (norte != (z - c.dz() <= 0)) continue;
                int[] col = coluna(c, x, z);
                if (col == null) continue;
                for (int y = col[1]; y <= col[0]; y++) {
                    Material m;
                    if (y == col[0]) m = Material.GRASS_BLOCK;
                    else if (y > col[0] - 3) m = o.um(Material.DIRT, 8, Material.COARSE_DIRT, 1, Material.ROOTED_DIRT, 1);
                    else m = o.um(Material.STONE, 14, Material.ANDESITE, 3, Material.TUFF, 2, Material.CALCITE, 1, Material.COBBLESTONE, 1);
                    o.por(x, y, z, m);
                }
            }
        }
    }

    /** Grama, flores, raízes e pingos de pedra por baixo. */
    private static void enfeitar(Obra o, Corpo c) {
        for (int x = c.dx() - c.r() - 4; x <= c.dx() + c.r() + 4; x++) {
            for (int z = c.dz() - c.r() - 4; z <= c.dz() + c.r() + 4; z++) {
                int[] col = coluna(c, x, z);
                if (col == null) continue;
                int dx = x - c.dx(), dz = z - c.dz();
                boolean meio = c.dx() == 0 && c.dz() == 0 && dx * dx + dz * dz < 64; // a construção fica no meio
                if (!meio) {
                    double v = o.r.nextDouble();
                    if (v < 0.30) o.porSeVazio(x, col[0] + 1, z, Material.SHORT_GRASS);
                    else if (v < 0.36) o.porSeVazio(x, col[0] + 1, z, o.um(Material.CORNFLOWER, 3, Material.OXEYE_DAISY, 3, Material.ALLIUM, 2,
                            Material.AZURE_BLUET, 3, Material.LILY_OF_THE_VALLEY, 2));
                    else if (v < 0.38) o.porSeVazio(x, col[0] + 1, z, Material.TALL_GRASS);
                }
                double w = o.r.nextDouble();
                if (w < 0.14) o.porSeVazio(x, col[1] - 1, z, Material.HANGING_ROOTS);
                else if (w < 0.19 && col[0] - col[1] >= 4) {
                    int n = 1 + o.r.nextInt(3);
                    for (int i = 1; i <= n; i++) {
                        o.por(x, col[1] - i, z, "pointed_dripstone[vertical_direction=down,thickness="
                                + (i == n ? "tip" : i == n - 1 ? "frustum" : "middle") + "]");
                    }
                }
            }
        }
    }

    private static void construcao(Obra o, Tipo t) {
        switch (t) {
            case JARDIM -> jardim(o);
            case OBSERVATORIO -> observatorio(o);
            case FORJA -> forja(o);
            case NINHO -> ninho(o);
        }
    }

    /** Árvore Celeste: tronco de cerejeira e copa de azaleia florida, com um laguinho. */
    private static void jardim(Obra o) {
        // Tronco grosso na base, 4 galhos e uma copa larga e baixa, como as árvores que crescem ao vento.
        for (int y = 1; y <= 9; y++) o.por(0, y, 0, "cherry_log[axis=y]");
        for (int[] r : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            o.por(r[0], 1, r[1], Material.CHERRY_WOOD);
            o.por(r[0] * 2, 1, r[1] * 2, "cherry_log[axis=" + (r[0] != 0 ? "x" : "z") + "]");
        }
        int[][] galhos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < galhos.length; i++) {
            int gx = galhos[i][0], gz = galhos[i][1], base = 5 + i % 2;
            for (int k = 1; k <= 3; k++) o.por(gx * k, base + k / 2, gz * k, "cherry_log[axis=" + (gx != 0 ? "x" : "z") + "]");
        }
        for (int x = -7; x <= 7; x++) {
            for (int y = 7; y <= 13; y++) {
                for (int z = -7; z <= 7; z++) {
                    double d = Math.sqrt(x * x + (y - 10) * (y - 10) * 2.2 + z * z);
                    if (d > 6.6 || (x == 0 && z == 0 && y <= 9)) continue;
                    if (d > 5.6 && o.chance(0.45)) continue;
                    o.por(x, y, z, (o.chance(0.45) ? "flowering_azalea_leaves" : "azalea_leaves") + "[persistent=true]");
                }
            }
        }
        // Pétalas caídas em volta do tronco.
        for (int i = 0; i < 10; i++) {
            int x = o.r.nextInt(11) - 5, z = o.r.nextInt(11) - 5;
            if (Math.abs(x) + Math.abs(z) > 2) o.porSeVazio(x, 1, z, Material.PINK_PETALS);
        }
        // Laguinho
        for (int x = 2; x <= 4; x++) for (int z = -4; z <= -2; z++) o.por(x, 0, z, Material.WATER);
        o.por(3, 1, -5, Material.PINK_PETALS);
        for (int[] f : new int[][]{{-3, 2}, {-2, 4}, {2, 3}, {-4, -2}, {3, 1}, {-1, -4}}) o.porSeVazio(f[0], 1, f[1], Material.PEONY);
        o.por(-2, 1, -2, Material.CHEST);
    }

    /** Anel de pilares de quartzo (alguns quebrados), piso de calcita e o púlpito do astrônomo. */
    private static void observatorio(Obra o) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 5.2) o.por(x, 0, z, d < 1.5 ? Material.CHISELED_QUARTZ_BLOCK : (x + z) % 2 == 0 ? Material.CALCITE : Material.SMOOTH_QUARTZ);
            }
        }
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            int x = (int) Math.round(Math.sin(ang) * 5), z = (int) Math.round(Math.cos(ang) * 5);
            int alt = i % 3 == 1 ? 2 : 5; // uns quebrados
            for (int y = 1; y <= alt; y++) o.por(x, y, z, "quartz_pillar[axis=y]");
            if (alt == 5) o.por(x, 6, z, Material.CHISELED_QUARTZ_BLOCK);
        }
        // Arco que sobrou da cúpula
        for (int x = -4; x <= 4; x++) o.por(x, 6 + (Math.abs(x) <= 2 ? 1 : 0), 4, Math.abs(x) == 3 ? "quartz_stairs[facing=east,half=bottom]" : "smooth_quartz_slab[type=bottom]");
        o.por(0, 1, 0, "lectern[facing=north]");
        o.por(0, 1, -2, Material.SMOOTH_QUARTZ);
        o.por(0, 2, -2, Material.LIGHTNING_ROD);
        o.por(2, 1, -2, Material.CHEST);
    }

    /** Ruína de forja: piso de ardósia, alto-forno, bigorna, fole de lã e lanternas penduradas. */
    private static void forja(Obra o) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) > 8) continue;
                o.por(x, 0, z, o.um(Material.DEEPSLATE_TILES, 5, Material.CRACKED_DEEPSLATE_TILES, 2, Material.POLISHED_DEEPSLATE, 2));
            }
        }
        o.encher(-3, -1, 1, 3, -4, -4, Material.DEEPSLATE_BRICKS);
        o.por(-2, 2, -4, Material.CAULDRON);
        o.por(-2, 1, -3, "blast_furnace[facing=south]");
        o.por(0, 1, -3, Material.ANVIL);
        o.por(2, 1, -3, Material.SMITHING_TABLE);
        o.por(-2, 4, -4, Material.CAMPFIRE);
        // Moinho de vento: mastro e pás de lã que o vento empurra
        for (int y = 1; y <= 6; y++) o.por(4, y, 2, "stripped_spruce_log[axis=y]");
        // As pás ficam num plano à frente do mastro (z = 1), em cruz.
        for (int i = 1; i <= 3; i++) {
            o.por(4, 6 + i, 1, Material.WHITE_WOOL);
            o.por(4, 6 - i, 1, Material.WHITE_WOOL);
            o.por(4 + i, 6, 1, Material.WHITE_WOOL);
            o.por(4 - i, 6, 1, Material.WHITE_WOOL);
        }
        o.por(4, 6, 1, Material.STRIPPED_SPRUCE_WOOD);
        for (int x : new int[]{-4, 3}) {
            o.por(x, 1, 3, Material.DEEPSLATE_BRICK_WALL);
            o.por(x, 2, 3, Material.DEEPSLATE_BRICK_WALL);
            o.por(x, 3, 3, "lantern[hanging=false]");
        }
        o.por(1, 1, 2, Material.CHEST);
    }

    /** Ninho enorme de feno, galhos e ossos, com ovos no meio. */
    private static void ninho(Obra o) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 6.2) continue;
                if (d > 4.2) {
                    int alt = d > 5.4 ? 1 : 2;
                    for (int y = 1; y <= alt; y++) o.por(x, y, z, o.um(Material.HAY_BLOCK, 4, Material.STRIPPED_OAK_WOOD, 2, Material.MUD_BRICKS, 1, Material.BONE_BLOCK, 1));
                    if (o.chance(0.3)) o.porSeVazio(x, alt + 1, z, Material.DEAD_BUSH);
                } else {
                    o.por(x, 0, z, Material.HAY_BLOCK);
                }
            }
        }
        o.por(-1, 1, 0, Material.SNIFFER_EGG);
        o.por(1, 1, 1, Material.SNIFFER_EGG);
        o.por(0, 1, -2, Material.TURTLE_EGG);
        o.por(2, 1, -2, Material.CHEST);
    }

    /** Ponte de cordas até a ilhota mais perto e a Plataforma de Vento é feita à parte (no chão). */
    private static void ponte(Obra o, long semente) {
        Corpo alvo = null;
        double melhor = Double.MAX_VALUE;
        for (Corpo c : ilhotas(semente)) {
            double d = c.dx() * c.dx() + c.dz() * c.dz();
            if (d < melhor) {
                melhor = d;
                alvo = c;
            }
        }
        if (alvo == null) return;
        double dist = Math.sqrt(melhor);
        double ux = alvo.dx() / dist, uz = alvo.dz() / dist;
        int de = RAIO - 4, ate = (int) dist - alvo.r() + 2;
        for (int i = de; i <= ate; i++) {
            int x = (int) Math.round(ux * i), z = (int) Math.round(uz * i);
            double t = (i - de) / (double) Math.max(1, ate - de);
            int y = (int) Math.round(alvo.dy() * t - Math.sin(t * Math.PI) * 2); // a ponte faz barriga no meio
            Block b = o.bloco(x, y, z);
            if (!b.getType().isAir() && b.getType() != Material.SHORT_GRASS) continue;
            o.por(x, y, z, "spruce_slab[type=top]");
            if (i % 3 == 0) {
                int px = (int) Math.round(-uz), pz = (int) Math.round(ux);
                o.porSeVazio(x + px, y + 1, z + pz, Material.SPRUCE_FENCE);
                o.porSeVazio(x - px, y + 1, z - pz, Material.SPRUCE_FENCE);
            }
        }
        o.conectar();
    }

    // =====================================================================
    //  Plataforma de Vento (no chão)
    // =====================================================================

    /** Constrói a plataforma com o topo do chão em {@code yChao} (em y local) e devolve a placa de pressão. */
    static Block plataforma(Obra o, int yChao) {
        int z0 = PLATAFORMA;
        // Um patamar de 7x7: alicerce até o chão firme embaixo e tudo limpo em cima (o terreno pode ser torto).
        for (int x = -3; x <= 3; x++) {
            for (int z = z0 - 3; z <= z0 + 3; z++) {
                boolean borda = Math.abs(x) == 3 || Math.abs(z - z0) == 3;
                boolean centro = x == 0 && z == z0;
                o.por(x, yChao, z, centro ? Material.CHISELED_TUFF_BRICKS : borda ? Material.GRASS_BLOCK
                        : Math.abs(x) == 2 || Math.abs(z - z0) == 2
                        ? o.um(Material.MOSSY_STONE_BRICKS, 2, Material.CRACKED_STONE_BRICKS, 1, Material.STONE_BRICKS, 3) : Material.TUFF_BRICKS);
                for (int y = yChao - 1; y >= yChao - 8; y--) {
                    Block b = o.bloco(x, y, z);
                    if (b.getType().isSolid() && !b.getType().name().endsWith("_LEAVES")) break;
                    o.por(x, y, z, borda ? Material.DIRT : Material.STONE_BRICKS);
                }
                for (int y = 1; y <= 5; y++) {
                    if (!o.bloco(x, yChao + y, z).getType().isAir()) o.por(x, yChao + y, z, Material.AIR);
                }
            }
        }
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            o.por(c[0], yChao + 1, z0 + c[1], Material.TUFF_BRICK_WALL);
            o.por(c[0], yChao + 2, z0 + c[1], Material.TUFF_BRICK_WALL);
            o.por(c[0], yChao + 3, z0 + c[1], Material.SOUL_LANTERN);
        }
        Block placa = o.por(0, yChao + 1, z0, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE);
        // O aviso, virado para quem chega pela frente.
        Block aviso = o.por(0, yChao + 1, z0 + 3, "oak_sign[rotation=0]");
        if (aviso.getState() instanceof org.bukkit.block.Sign s && aviso.getBlockData() instanceof Sign) {
            var lado = s.getSide(org.bukkit.block.sign.Side.FRONT);
            lado.line(0, net.kyori.adventure.text.Component.text("⚠ Plataforma"));
            lado.line(1, net.kyori.adventure.text.Component.text("de Vento"));
            lado.line(2, net.kyori.adventure.text.Component.text("Sem planador, a"));
            lado.line(3, net.kyori.adventure.text.Component.text("queda é fatal!"));
            s.setWaxed(true);
            s.update(true, false);
        }
        o.conectar();
        return placa;
    }
}
