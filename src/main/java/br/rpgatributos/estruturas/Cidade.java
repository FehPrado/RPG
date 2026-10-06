package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Carta;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A planta da cidade submersa: 73 x 73 blocos no fundo do mar, construída em {@link #ETAPAS}
 * etapas (uma por tick) para não travar o servidor.
 *
 * Coordenadas locais com o centro no meio do templo, y = 0 no chão da praça. Muralha redonda
 * (raio 33) com 4 portões, avenidas em cruz, praça, templo de Maris com o altar do coração
 * (moldura de condutor), cofre embaixo do templo, 4 bairros (casas, arquivo, torre e jardim de
 * corais com a estátua de Maris).
 */
final class Cidade {

    static final int RAIO = 36;
    static final int ETAPAS = 21;
    /** Espaço livre acima do chão (o templo vai até y = 14). */
    static final int ALTURA = 24;
    /** O centro da moldura do coração (onde vai o condutor). */
    static final int[] CORACAO = {0, 7, 0};
    /** Onde o Sumo-Sacerdote aparece. */
    static final int[] SACERDOTE = {0, 4, -2};
    static final int[][] GUARDAS = {{1, 1, 30}, {-1, 1, -30}, {30, 1, 1}, {-30, 1, -1}, {11, 1, 11}, {-11, 1, 11}, {11, 1, -11}, {-11, 1, -11}};
    static final int[][] SENTINELAS = {{6, 5, 0}, {-6, 5, 0}, {0, 5, 6}};

    private static final String[] SACERDOTES = {"Nereu", "Taumas", "Proteu", "Íxion", "Glauco", "Forco"};

    private final RPGAtributos plugin;
    private final Projetos base;

    Cidade(RPGAtributos plugin, Projetos base) {
        this.plugin = plugin;
        this.base = base;
    }

    static String sacerdote(String cidade) {
        return "Sumo-Sacerdote " + SACERDOTES[Math.floorMod(cidade == null ? 0 : cidade.hashCode(), SACERDOTES.length)];
    }

    private static double dist(int x, int z) {
        return Math.sqrt(x * x + z * z);
    }

    private static boolean avenida(int x, int z) {
        return Math.abs(x) <= 2 || Math.abs(z) <= 2;
    }

    /** Faz a etapa {@code i} (0 a {@link #ETAPAS} - 1). */
    void etapa(int i, Obra o, String nome) {
        if (i < 8) {
            nivelar(o, -36 + i * 10, Math.min(36, -27 + i * 10));
            return;
        }
        switch (i) {
            case 8 -> chao(o);
            case 9 -> muralha(o);
            case 10 -> templo(o);
            case 11 -> santuario(o, nome);
            case 12 -> cofre(o);
            case 13 -> bairro(o, -1, -1, null, nome);
            case 14 -> bairro(o, 1, -1, "arquivo", nome);
            case 15 -> bairro(o, -1, 1, "torre", nome);
            case 16 -> bairro(o, 1, 1, "jardim", nome);
            case 17 -> postes(o);
            case 18 -> enfeitar(o, true);
            case 19 -> enfeitar(o, false);
            default -> { }
        }
        o.conectar();
    }

    // =====================================================================
    //  Terreno
    // =====================================================================

    /** Aplaina as colunas x0..x1: aterra o que está baixo e tira (vira água) o que está alto. */
    private static void nivelar(Obra o, int x0, int x1) {
        World w = o.w;
        int mar = w.getSeaLevel();
        for (int x = x0; x <= x1; x++) {
            for (int z = -RAIO; z <= RAIO; z++) {
                if (dist(x, z) > 35.5) continue;
                int gx = o.x + x, gz = o.z + z;
                int h = w.getHighestBlockYAt(gx, gz, HeightMap.OCEAN_FLOOR);
                for (int yy = h + 1; yy <= o.y; yy++) w.getBlockAt(gx, yy, gz).setType(yy < o.y - 2 ? Material.SANDSTONE : Material.SAND, false);
                for (int yy = o.y + 1; yy <= o.y + ALTURA; yy++) {
                    Block b = w.getBlockAt(gx, yy, gz);
                    Material m = b.getType();
                    if (yy < mar) {
                        if (m != Material.WATER) b.setType(Material.WATER, false);
                    } else if (!m.isAir()) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    /** O chão: praça em anéis, avenidas com faixa escura e luzes, bairros de areia e pedra gasta. */
    private static void chao(Obra o) {
        for (int x = -35; x <= 35; x++) {
            for (int z = -35; z <= 35; z++) {
                double d = dist(x, z);
                if (d > 34.4) continue;
                Material m;
                if (d <= 13.5) {
                    if (d >= 11 && d < 12 || d >= 6 && d < 7) m = Material.DARK_PRISMARINE;
                    else m = o.um(Material.PRISMARINE_BRICKS, 16, Material.PRISMARINE, 3, Material.SAND, 1);
                } else if (avenida(x, z)) {
                    boolean faixa = x == 0 || z == 0;
                    if (faixa && Math.round(d) % 7 == 0) m = Material.SEA_LANTERN;
                    else if (faixa) m = Material.DARK_PRISMARINE;
                    else m = o.um(Material.PRISMARINE_BRICKS, 17, Material.PRISMARINE, 2, Material.SAND, 1);
                } else if (d >= 32) {
                    m = Material.PRISMARINE_BRICKS;
                } else {
                    m = o.um(Material.SAND, 14, Material.GRAVEL, 2, Material.PRISMARINE, 2, Material.SANDSTONE, 1, Material.CLAY, 1);
                }
                o.por(x, 0, z, m);
            }
        }
    }

    // =====================================================================
    //  Muralha
    // =====================================================================

    private static void muralha(Obra o) {
        double[] rombos = new double[4];
        for (int i = 0; i < rombos.length; i++) rombos[i] = o.r.nextDouble() * Math.PI * 2;
        for (int x = -35; x <= 35; x++) {
            for (int z = -35; z <= 35; z++) {
                double d = dist(x, z);
                if (d < 30.5 || d >= 34.2) continue;
                double ang = Math.atan2(z, x);
                boolean rombo = false;
                for (double a : rombos) {
                    double dif = Math.abs(Math.atan2(Math.sin(ang - a), Math.cos(ang - a)));
                    if (dif < 0.11) rombo = true;
                }
                boolean portao = avenida(x, z);
                if (d < 32.5) {
                    // Pedras caídas do lado de dentro, onde a muralha rompeu.
                    if (rombo && !portao && o.chance(0.3)) o.por(x, 1, z, o.um(Material.PRISMARINE_BRICKS, 2, Material.PRISMARINE, 1));
                    continue;
                }
                if (portao) {
                    o.por(x, 5, z, Material.PRISMARINE_BRICKS);
                    o.por(x, 6, z, Material.DARK_PRISMARINE);
                    continue;
                }
                boolean pilar = Math.abs(x) <= 4 && Math.abs(z) > 28 || Math.abs(z) <= 4 && Math.abs(x) > 28;
                int h = pilar ? 8 : rombo ? o.r.nextInt(2) : 5;
                for (int y = 1; y <= h; y++) {
                    o.por(x, y, z, pilar && y == 8 ? Material.SEA_LANTERN : o.um(Material.PRISMARINE_BRICKS, 7, Material.PRISMARINE, 2, Material.DARK_PRISMARINE, 1));
                }
                if (h == 5 && ((x + z) & 1) == 0) o.por(x, 6, z, Material.DARK_PRISMARINE);
            }
        }
    }

    // =====================================================================
    //  Templo de Maris
    // =====================================================================

    private static void templo(Obra o) {
        // Base em três degraus.
        for (int y = 1; y <= 3; y++) {
            int lim = 11 - y;
            for (int x = -lim; x <= lim; x++) {
                for (int z = -lim; z <= lim; z++) {
                    int c = Math.max(Math.abs(x), Math.abs(z));
                    Material m = c == lim ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS;
                    if (y == 3 && c <= 3) m = Math.abs(x) == 3 && Math.abs(z) == 3 ? Material.SEA_LANTERN : Material.DARK_PRISMARINE;
                    o.por(x, y, z, m);
                }
            }
        }
        // Escadarias nos quatro lados.
        for (int k = 0; k <= 2; k++) {
            int y = 1 + k, a = 10 - k;
            for (int p = -1; p <= 1; p++) {
                o.por(p, y, a, "prismarine_brick_stairs[facing=north]");
                o.por(p, y, -a, "prismarine_brick_stairs[facing=south]");
                o.por(a, y, p, "prismarine_brick_stairs[facing=west]");
                o.por(-a, y, p, "prismarine_brick_stairs[facing=east]");
            }
        }
        // Colunata (algumas colunas quebradas, com os pedaços no chão).
        Set<Long> colunas = new LinkedHashSet<>();
        for (int p : new int[]{-7, -4, 4, 7}) {
            colunas.add(chave(p, 7));
            colunas.add(chave(p, -7));
            colunas.add(chave(7, p));
            colunas.add(chave(-7, p));
        }
        for (long k : colunas) {
            int x = (int) (k >> 32), z = (int) k;
            boolean quebrada = o.chance(0.25);
            int topo = quebrada ? 4 + o.r.nextInt(3) : 9;
            for (int y = 4; y <= topo; y++) o.por(x, y, z, y == 4 ? Material.PRISMARINE_BRICKS : y == 9 ? Material.DARK_PRISMARINE : Material.PRISMARINE);
            if (quebrada) {
                int fx = x + (x > 0 ? 1 : x < 0 ? -1 : 0), fz = z + (z > 0 ? 1 : z < 0 ? -1 : 0);
                if (Math.max(Math.abs(fx), Math.abs(fz)) <= 8) o.por(fx, 4, fz, Material.PRISMARINE);
            }
        }
        // Telhado em pirâmide, com um canto desabado.
        int[][] camadas = {{10, 8}, {11, 6}, {12, 4}, {13, 2}};
        for (int[] cm : camadas) {
            for (int x = -cm[1]; x <= cm[1]; x++) {
                for (int z = -cm[1]; z <= cm[1]; z++) {
                    boolean desabou = x > 1 && z < -1 && o.chance(0.55);
                    if (!desabou) o.por(x, cm[0], z, cm[0] == 10 ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS);
                }
            }
        }
        o.por(0, 14, 0, Material.SEA_LANTERN);
    }

    private static long chave(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** As paredes do santuário, a moldura do coração (condutor) e o púlpito com a história. */
    private static void santuario(Obra o, String nome) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean parede = Math.max(Math.abs(x), Math.abs(z)) == 4;
                if (!parede) continue;
                int p = Math.abs(x) == 4 ? z : x;
                for (int y = 4; y <= 9; y++) {
                    boolean porta = Math.abs(p) <= 1 && y <= 6;
                    boolean janela = Math.abs(p) == 2 && y == 7;
                    o.por(x, y, z, porta || janela ? Material.AIR : y == 9 ? Material.DARK_PRISMARINE : Material.PRISMARINE_BRICKS);
                }
            }
        }
        // A moldura: três anéis em volta do centro, como os condutores pedem.
        int cx = CORACAO[0], cy = CORACAO[1], cz = CORACAO[2];
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                boolean anel = Math.abs(a) == 2 && Math.abs(b) <= 1 || Math.abs(b) == 2 && Math.abs(a) <= 1;
                if (!anel) continue;
                o.por(cx + a, cy + b, cz, Material.PRISMARINE_BRICKS);
                o.por(cx, cy + b, cz + a, Material.PRISMARINE_BRICKS);
                o.por(cx + a, cy, cz + b, b == 0 || a == 0 ? Material.SEA_LANTERN : Material.DARK_PRISMARINE);
            }
        }
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) for (int c = -1; c <= 1; c++) o.por(cx + a, cy + b, cz + c, Material.AIR);
        o.por(-3, 4, 3, "lectern[facing=north]");
        Ruinas.porLivro(o.bloco(-3, 4, 3), Ruinas.livro("O Altar Vazio", "Os últimos de " + nome,
                "Aqui batia o coração de " + nome + ", presente de Maris.\n\nNa noite em que o mar subiu, " + sacerdote(nome) + " o arrancou do altar.",
                "Enquanto o coração não voltar a este altar, " + nome + " não descansa.\n\nPonha-o bem no meio da moldura de pedra-do-mar."));
    }

    /** O cofre embaixo do templo: desce pelo buraco no canto do santuário. */
    private void cofre(Obra o) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean borda = Math.max(Math.abs(x), Math.abs(z)) == 4;
                for (int y = -6; y <= 0; y++) {
                    if (y == -6) o.por(x, y, z, Material.PRISMARINE_BRICKS);
                    else if (borda || y == 0) o.por(x, y, z, Material.DARK_PRISMARINE);
                    else o.por(x, y, z, Material.AIR);
                }
            }
        }
        for (int y = 0; y <= 3; y++) o.por(2, y, -3, Material.AIR);
        o.por(3, -5, -3, Material.GOLD_BLOCK);
        o.por(-3, -5, -3, Material.GOLD_BLOCK);
        o.por(3, -5, 3, Material.SEA_LANTERN);
        o.por(-3, -5, 3, Material.SEA_LANTERN);
        for (int x = -2; x <= 2; x += 2) {
            o.por(x, -5, -3, "chest[facing=south]");
            List<ItemStack> extra = new ArrayList<>();
            extra.add(new ItemStack(Material.EMERALD, 4 + o.r.nextInt(8)));
            if (x == 0) {
                extra.add(br.rpgatributos.alquimia.Gema.values()[o.r.nextInt(br.rpgatributos.alquimia.Gema.values().length)].criar(2, 1));
                extra.add(Reagente.PEROLA_NEGRA.criar(1 + o.r.nextInt(2)));
            } else {
                extra.add(Reagente.ESCAMA_DO_ABISMO.criar(1 + o.r.nextInt(2)));
                if (o.chance(0.4)) extra.add(new ItemStack(Material.NAUTILUS_SHELL, 2 + o.r.nextInt(3)));
            }
            base.bau(o.bloco(x, -5, -3), x == 0 ? LootTables.BURIED_TREASURE : LootTables.SHIPWRECK_TREASURE, o, extra);
        }
    }

    // =====================================================================
    //  Bairros
    // =====================================================================

    private static final int[][] LOTES = {{16, 16}, {9, 20}, {20, 9}, {7, 27}, {27, 7}};

    /** Um quarto da cidade. {@code especial} troca o primeiro lote (arquivo, torre ou jardim). */
    private void bairro(Obra o, int sx, int sz, String especial, String nome) {
        for (int i = 0; i < LOTES.length; i++) {
            int px = LOTES[i][0] * sx, pz = LOTES[i][1] * sz;
            int giro;
            if (Math.abs(pz) >= Math.abs(px)) giro = pz > 0 ? 2 : 0;
            else giro = px > 0 ? 1 : 3;
            Obra lote = o.parte(px, pz, giro);
            if (i == 0 && especial != null) {
                switch (especial) {
                    case "arquivo" -> arquivo(lote, nome);
                    case "torre" -> torre(lote);
                    default -> jardim(lote);
                }
            } else {
                casa(lote);
            }
        }
    }

    private static Material paredeCasa(Obra o) {
        return o.um(Material.SMOOTH_SANDSTONE, 9, Material.CUT_SANDSTONE, 5, Material.PRISMARINE_BRICKS, 4, Material.PRISMARINE, 2);
    }

    /** Casa 7x7 de arenito e pedra-do-mar, com o telhado meio caído (ou só ruína). */
    private void casa(Obra h) {
        boolean ruina = h.chance(0.25), desabou = h.chance(0.4);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                h.por(x, 0, z, Material.PRISMARINE_BRICKS);
                boolean parede = Math.max(Math.abs(x), Math.abs(z)) == 3;
                boolean canto = Math.abs(x) == 3 && Math.abs(z) == 3;
                int alt = ruina ? 1 + h.r.nextInt(2) : 4;
                for (int y = 1; y <= 4; y++) {
                    boolean porta = z == 3 && x == 0 && y <= 2;
                    boolean janela = y == 2 && (Math.abs(x) == 3 && z == 0 || z == -3 && x == 0);
                    if (!parede || porta || janela || y > alt) h.por(x, y, z, Material.AIR);
                    else h.por(x, y, z, canto ? (y == 4 ? Material.DARK_PRISMARINE : Material.PRISMARINE) : paredeCasa(h));
                }
                if (!ruina && !(desabou && x > 0 && z < 0 && h.chance(0.7))) h.por(x, 5, z, "prismarine_brick_slab");
            }
        }
        if (desabou || ruina) {
            for (int i = 0; i < 3; i++) h.por(-1 + h.r.nextInt(3), 1, -1 + h.r.nextInt(3), paredeCasa(h));
        }
        if (h.chance(0.4)) {
            h.por(-2, 1, -2, "chest[facing=south]");
            List<ItemStack> extra = new ArrayList<>();
            if (h.chance(0.15)) extra.add(Album.carta(Carta.values()[h.r.nextInt(Carta.values().length)], false));
            if (h.chance(0.1)) extra.add(Reagente.PEROLA_NEGRA.criar(1));
            base.bau(h.bloco(-2, 1, -2), LootTables.UNDERWATER_RUIN_SMALL, h, extra);
        }
        if (h.chance(0.3)) h.por(2, 1, -2, Material.DECORATED_POT);
        if (h.chance(0.3)) h.por(1, 1, 1, "sea_pickle[pickles=" + (1 + h.r.nextInt(4)) + "]");
    }

    /** O arquivo da cidade: estantes, dois púlpitos e o baú com o último livro. */
    private void arquivo(Obra a, String nome) {
        String sacerdote = sacerdote(nome);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                a.por(x, 0, z, Material.DARK_PRISMARINE);
                boolean parede = Math.max(Math.abs(x), Math.abs(z)) == 4;
                for (int y = 1; y <= 6; y++) {
                    boolean porta = z == 4 && Math.abs(x) <= 1 && y <= 3;
                    boolean janela = y == 4 && Math.abs(x) == 4 && Math.abs(z) % 2 == 0 && Math.abs(z) <= 2;
                    if (!parede || porta || janela) a.por(x, y, z, Material.AIR);
                    else a.por(x, y, z, y == 6 ? Material.DARK_PRISMARINE : a.um(Material.PRISMARINE_BRICKS, 3, Material.CUT_SANDSTONE, 1));
                }
                if (!a.chance(0.3)) a.por(x, 7, z, "prismarine_brick_slab");
            }
        }
        for (int z = -3; z <= 2; z++) {
            for (int y = 1; y <= 3; y++) {
                if (a.chance(0.7)) a.por(-3, y, z, Material.BOOKSHELF);
                if (a.chance(0.7)) a.por(3, y, z, Material.BOOKSHELF);
            }
        }
        a.por(-1, 1, -3, "lectern[facing=south]");
        Ruinas.porLivro(a.bloco(-1, 1, -3), Ruinas.livro("A Fundação de " + nome, "Arquivo de " + nome,
                "Os primeiros de " + nome + " vieram do mar e prometeram voltar a ele.\n\nMaris lhes deu a ilha, a pedra que brilha e o peixe que nunca falta.",
                "Em troca, pediu só uma coisa: que ninguém pescasse na noite de lua nova.\n\nEssa noite era dela."));
        a.por(1, 1, -3, "lectern[facing=south]");
        Ruinas.porLivro(a.bloco(1, 1, -3), Ruinas.livro("O Pacto", "Arquivo de " + nome,
                "Por cem anos, " + nome + " guardou o pacto. O templo foi erguido com pedra-do-mar, e no altar batia o coração da cidade.",
                sacerdote + " quis o coração só para si. Dizia que, com ele, " + nome + " nunca mais precisaria de Maris."));
        a.por(0, 1, -3, "chest[facing=south]");
        List<ItemStack> extra = new ArrayList<>();
        extra.add(Ruinas.livro("A Noite em que o Mar Subiu", "O último arquivista",
                "Na lua nova, os barcos saíram. A cidade inteira pescou, e " + sacerdote + " riu no alto do templo.",
                "O mar subiu sem onda e sem barulho. Quem estava na rua virou guarda. Quem estava no templo nunca mais saiu.",
                "Dizem que " + sacerdote + " ainda guarda o coração lá dentro. E que, se ele voltar ao altar, " + nome + " enfim vai poder dormir."));
        base.bau(a.bloco(0, 1, -3), LootTables.UNDERWATER_RUIN_BIG, a, extra);
    }

    /** A torre de vigia do mar, com a luz no alto (quebrada de um lado). */
    private static void torre(Obra t) {
        int alt = Math.max(8, Math.min(20, t.w.getSeaLevel() - 4 - t.y));
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double d = dist(x, z);
                if (d > 3.4) continue;
                t.por(x, 0, z, Material.PRISMARINE_BRICKS);
                boolean parede = d > 2.3;
                for (int y = 1; y <= alt; y++) {
                    boolean porta = z == 3 && x == 0 && y <= 2;
                    boolean janela = y % 4 == 3 && (x == 0 || z == 0);
                    boolean quebrou = x > 0 && y > alt - 3 && t.chance(0.5);
                    if (!parede || porta || janela || quebrou) t.por(x, y, z, Material.AIR);
                    else t.por(x, y, z, y % 5 == 0 ? Material.DARK_PRISMARINE : t.um(Material.PRISMARINE_BRICKS, 3, Material.PRISMARINE, 1));
                }
                if (!parede) t.por(x, alt, z, Material.PRISMARINE_BRICKS);
                else if (((x + z) & 1) == 0 && !(x > 0 && t.chance(0.5))) t.por(x, alt + 1, z, Material.DARK_PRISMARINE);
            }
        }
        t.por(0, alt, 0, Material.SEA_LANTERN);
    }

    /** O jardim de corais em volta da estátua de Maris (com o tridente). */
    private static void jardim(Obra j) {
        Material[] blocos = {Material.TUBE_CORAL_BLOCK, Material.BRAIN_CORAL_BLOCK, Material.BUBBLE_CORAL_BLOCK, Material.FIRE_CORAL_BLOCK, Material.HORN_CORAL_BLOCK};
        String[] plantas = {"tube_coral", "brain_coral", "bubble_coral", "fire_coral", "horn_coral", "tube_coral_fan", "horn_coral_fan"};
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = dist(x, z);
                if (d > 5.5) continue;
                j.por(x, 0, z, d <= 1.5 ? Material.DARK_PRISMARINE : Material.SAND);
                if (d <= 2.5 || !j.chance(0.55)) continue;
                int alt = 1 + (j.chance(0.3) ? 1 : 0);
                for (int y = 1; y <= alt; y++) j.por(x, y, z, blocos[j.r.nextInt(blocos.length)]);
                if (j.chance(0.6)) j.por(x, alt + 1, z, plantas[j.r.nextInt(plantas.length)]);
                else if (j.chance(0.3)) j.por(x, alt + 1, z, "sea_pickle[pickles=" + (2 + j.r.nextInt(3)) + "]");
            }
        }
        // Estátua: pedestal, corpo, braços, cabeça de luz e o tridente.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) j.por(x, 1, z, Material.DARK_PRISMARINE);
        j.por(0, 2, 0, Material.PRISMARINE_BRICKS);
        j.por(0, 3, 0, Material.PRISMARINE_BRICKS);
        j.por(0, 4, 0, Material.PRISMARINE_BRICKS);
        j.por(-1, 4, 0, "prismarine_wall");
        j.por(1, 4, 0, "prismarine_wall");
        j.por(0, 5, 0, Material.SEA_LANTERN);
        j.por(1, 5, 0, "lightning_rod");
        j.por(1, 6, 0, "lightning_rod");
    }

    // =====================================================================
    //  Luzes e vida
    // =====================================================================

    /** Postes de luz dos dois lados das avenidas. */
    private static void postes(Obra o) {
        for (int p : new int[]{15, 22, 29}) {
            for (int s : new int[]{-1, 1}) {
                for (int lado : new int[]{-3, 3}) {
                    poste(o, lado, p * s);
                    poste(o, p * s, lado);
                }
            }
        }
    }

    private static void poste(Obra o, int x, int z) {
        o.por(x, 1, z, "prismarine_wall");
        o.por(x, 2, z, "prismarine_wall");
        o.por(x, 3, z, Material.SEA_LANTERN);
    }

    /** Algas, capim-marinho, corais e pepinos-do-mar onde o chão é de areia. */
    private static void enfeitar(Obra o, boolean oeste) {
        String[] corais = {"tube_coral", "brain_coral", "bubble_coral", "fire_coral", "horn_coral"};
        int mar = o.w.getSeaLevel();
        for (int x = oeste ? -32 : 0; x <= (oeste ? -1 : 32); x++) {
            for (int z = -32; z <= 32; z++) {
                double d = dist(x, z);
                if (d > 31.5 || d <= 13.5 || avenida(x, z)) continue;
                Block chao = o.bloco(x, 0, z);
                Material m = chao.getType();
                if (m != Material.SAND && m != Material.GRAVEL && m != Material.SANDSTONE && m != Material.CLAY) continue;
                if (o.bloco(x, 1, z).getType() != Material.WATER) continue;
                double v = o.r.nextDouble();
                if (v < 0.03) {
                    int alt = 3 + o.r.nextInt(8), topo = 1;
                    while (topo < alt && o.bloco(x, topo + 1, z).getType() == Material.WATER && o.y + topo + 1 < mar - 1) topo++;
                    for (int y = 1; y < topo; y++) o.por(x, y, z, Material.KELP_PLANT);
                    o.por(x, topo, z, Material.KELP);
                } else if (v < 0.18) {
                    o.por(x, 1, z, Material.SEAGRASS);
                } else if (v < 0.195) {
                    o.por(x, 1, z, corais[o.r.nextInt(corais.length)]);
                } else if (v < 0.205) {
                    o.por(x, 1, z, "sea_pickle[pickles=" + (1 + o.r.nextInt(4)) + "]");
                }
            }
        }
    }
}
