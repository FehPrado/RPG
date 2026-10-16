package br.rpgatributos.exploracao;

import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Nomeada;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * O desenho das armas com nome ({@link Nomeada}): moldes de 16x16 pintados com a paleta de
 * cada uma. Arcos e bestas são desenhados por código, um quadro por fase da corda.
 */
public final class ArteNomeadas {

    /** Um desenho pronto: o id da textura, o desenho e as cores. */
    public record Desenho(String id, String[] linhas, Map<Character, Integer> cores) { }

    private ArteNomeadas() { }

    // =====================================================================
    //  Moldes
    // =====================================================================

    private static final String[] ESPADA = {
            "..............oo",
            ".............oho",
            "............ohmo",
            "...........ohmo.",
            "..........ohmo..",
            ".........ohmo...",
            "........ohmo....",
            ".......ohmo.....",
            "......ohmo......",
            "...o.ohmo.......",
            "...ogohmo.......",
            "....oggo........",
            "...owoggo.......",
            "..owo..oo.......",
            ".opo............",
            "..o............."};

    private static final String[] SABRE = {
            "................",
            "...........ooo..",
            "..........ohhdo.",
            ".........ohmdo..",
            "........ohmdo...",
            ".......ohmdo....",
            "......ohmdo.....",
            ".....ohmdo......",
            "....ohmdo.......",
            "..o.ohdo........",
            "..oggoo.........",
            "...ogo..........",
            "..owoo..........",
            ".owo............",
            "opo.............",
            ".o.............."};

    private static final String[] LARGA = {
            ".............ooo",
            "............ohho",
            "...........ohhmo",
            "..........ohhmdo",
            ".........ohhmdo.",
            "........ohhmdo..",
            ".......ohhmdo...",
            "......ohhmdo....",
            ".....ohhmdo.....",
            "..o.ohhmdo......",
            "..ogghmdo.......",
            "...oggdo........",
            "..owoggo........",
            ".owo.oo.........",
            "opo.............",
            ".o.............."};

    private static final String[] MACHADO = {
            "................",
            "..........ooo...",
            ".........ohhmo..",
            "........ohmamdo.",
            ".......ohmmammdo",
            "......owoommddo.",
            ".....owo.oddoo..",
            "....owo...oo....",
            "...owo..........",
            "..owo...........",
            ".owo............",
            "owo.............",
            "oo..............",
            "................",
            "................",
            "................"};

    private static final String[] MACHADO2 = {
            "................",
            "........oooo....",
            ".......ohhmmo...",
            "......ohmammdo..",
            "......ohmmamdo..",
            ".....owohmmddo..",
            "....owo.ohmddo..",
            "...owo...oddo...",
            "..owo.....oo....",
            ".owo............",
            "owo.............",
            "oo..............",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] MACA = {
            "................",
            "..........o.o...",
            ".........ohoho..",
            "........ohhmmdo.",
            ".......oohmamdoo",
            "........ommmddo.",
            ".........oddoo..",
            "........owoo.o..",
            ".......owo......",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..opo...........",
            "...o............",
            "................"};

    private static final String[] MARTELO = {
            "................",
            ".........oooooo.",
            ".........ohhhmo.",
            "........oohmmmdo",
            "........ohmamddo",
            "........ommmmddo",
            "........oodddoo.",
            ".......owo.oo...",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o.............",
            "................"};

    private static final String[] PICARETA = {
            "................",
            ".....oooooo.....",
            "...oohhmmmdoo...",
            "..ohmao..oamdo..",
            ".ohmo.owo..odmo.",
            ".omo.owo....omo.",
            ".oo.owo......oo.",
            "...owo..........",
            "..owo...........",
            ".owo............",
            "owo.............",
            "oo..............",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PA = {
            "................",
            "...........ooo..",
            "..........ohhmo.",
            ".........ohhmmdo",
            ".........ohmmddo",
            "..........oddoo.",
            ".........owoo...",
            "........owo.....",
            ".......owo......",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o............."};

    private static final String[] ENXADA = {
            "................",
            ".........ooooo..",
            "........ohhmmdo.",
            "........oommddo.",
            "..........owoo..",
            ".........owo....",
            "........owo.....",
            ".......owo......",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o.............",
            "................"};

    private static final String[] VARA = {
            "............ooo.",
            "...........owwso",
            "..........owwo.s",
            ".........owo....",
            "........owo....s",
            ".......owo......",
            "......owo......s",
            ".....owo........",
            "....owo........s",
            "...owo........aa",
            "..ogo.........a.",
            ".ogo............",
            "opo.............",
            "oo..............",
            "................",
            "................"};

    // Armas dos minérios (com nome): o mesmo desenho das armas da bancada, nas letras da paleta.
    private static final String[] N_MONTANTE = {
            ".............ooh",
            "............ohho",
            "...........ohhmo",
            "..........ohhmo.",
            ".........ohhmo..",
            "........ohhmo...",
            ".......ohhmo....",
            "......ohhmo.....",
            ".....ohhmo......",
            ".o..ohhmo.......",
            "ogoohhmo........",
            ".ogohmo.........",
            "..oaoo..........",
            ".owwgo..........",
            "owwoogo.........",
            "awo..o.........."};

    private static final String[] N_BUMERANGUE = {
            "................",
            "................",
            "...oo.........o.",
            "..opmo.......opo",
            "...oamo.....oamo",
            "....owmo...owmo.",
            ".....owmo.owmo..",
            "......owmowmo...",
            ".......omamo....",
            ".......owwo.....",
            "........oo......",
            "................",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] N_LEQUES = {
            "..oooo..........",
            ".oddddoo........",
            ".owmmdddoo......",
            ".owmmmwmddo.....",
            ".owmmmwmmddo....",
            ".owmmwmmmmwdo...",
            ".owmmwmmmwmddo..",
            ".owmmmmmwmmmdo..",
            ".owmwmmwmmmmmdo.",
            ".owmmmwmmmmwwdo.",
            ".owmmwmmmwwmmddo",
            "..oowmmwmmmmmmdo",
            ".owoommmmmmmmmdo",
            ".ogwowwwwwwwwwdo",
            "ogoo.oooooooooo.",
            "go.............."};

    private static final String[] N_ARPAO = {
            "...........ooohh",
            "..........ohhhmo",
            "...........ohmo.",
            "..........ohmoho",
            ".........oaaooho",
            ".........owao.o.",
            "........owgo....",
            ".......owgo.....",
            "......owgo......",
            ".....owgo.......",
            "....owgo........",
            "...owgo.........",
            "..opgopo........",
            ".owgppo.........",
            "owgooo..........",
            "wgo............."};

    private static final String[] N_MANGUAL = {
            "..........oho...",
            "..........odmo..",
            "........oodhmdoo",
            ".......ohdmmdmmh",
            "........ommdmmdo",
            ".........odmmdo.",
            ".........opmdo..",
            ".......oopooho..",
            "......oppo..o...",
            ".....opoo.......",
            "....opo.........",
            "...owo..........",
            "..owgo..........",
            ".owgo...........",
            "owgo............",
            "poo............."};

    private static final String[] N_GARRAS = {
            "................",
            "................",
            "............o...",
            "...........oho..",
            "..........oho...",
            "..........omo.o.",
            ".........omo.oho",
            "........ohmooho.",
            "..ooooooomohhmo.",
            ".ogggggghmhomooo",
            ".owwwwwwmhmmoohh",
            ".owwaawwhmoohhoo",
            ".owwaawwmohhommo",
            ".owwwwwwhhmmmoo.",
            "..oooooommooo...",
            "........oo......"};

    private static final String[] N_CLAVA = {
            "................",
            ".........ooooo..",
            "........ommwmao.",
            ".......omammmmo.",
            ".......ommmmwmo.",
            ".......ommwmamo.",
            ".......owmmmmwo.",
            ".......oppawmo..",
            "......oppoooo...",
            ".....oppo.......",
            "....owmo........",
            "...owmo.........",
            "..owmo..........",
            ".owmo...........",
            "owmo............",
            "ppo............."};

    private static String[] molde(String nome) {
        return switch (nome) {
            case "sabre" -> SABRE;
            case "larga" -> LARGA;
            case "machado" -> machado(false);
            case "machado2" -> machado(true);
            case "maca" -> MACA;
            case "martelo" -> MARTELO;
            case "picareta" -> PICARETA;
            case "pa" -> PA;
            case "enxada" -> ENXADA;
            case "vara" -> VARA;
            case "lanca" -> lanca();
            case "crescente" -> crescente();
            case "tridente" -> tridente();
            case "adaga" -> encurtar(ESPADA, 4);
            case "faca" -> encurtar(ESPADA, 7);
            case "machadinha" -> machadinha();
            case "foice" -> foice();
            case "bastao" -> bastao();
            case "chicote" -> chicote();
            case "montante" -> N_MONTANTE;
            case "bumerangue" -> N_BUMERANGUE;
            case "leques" -> N_LEQUES;
            case "arpao" -> N_ARPAO;
            case "mangual" -> N_MANGUAL;
            case "garras" -> N_GARRAS;
            case "clava" -> N_CLAVA;
            default -> ESPADA;
        };
    }

    // =====================================================================
    //  Arco e besta (por código)
    // =====================================================================

    private static char[][] vazio() {
        char[][] g = new char[16][16];
        for (char[] l : g) java.util.Arrays.fill(l, '.');
        return g;
    }

    private static void pix(char[][] g, int x, int y, char c) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) g[y][x] = c;
    }

    private static void linha(char[][] g, int x0, int y0, int x1, int y1, char c) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, e = dx + dy;
        while (true) {
            pix(g, x0, y0, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * e;
            if (e2 >= dy) { e += dy; x0 += sx; }
            if (e2 <= dx) { e += dx; y0 += sy; }
        }
    }

    /** Curva de Bézier quadrática de (x0,y0) a (x2,y2) puxada para (cx,cy). */
    private static void curva(char[][] g, double x0, double y0, double cx, double cy, double x2, double y2, char c, char luz) {
        for (double t = 0; t <= 1.0001; t += 0.01) {
            double x = (1 - t) * (1 - t) * x0 + 2 * (1 - t) * t * cx + t * t * x2;
            double y = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * cy + t * t * y2;
            pix(g, (int) Math.round(x), (int) Math.round(y), c);
            if (luz != 0) pix(g, (int) Math.round(x) + 1, (int) Math.round(y) + 1, luz);
        }
    }

    /** Contorno escuro em volta das letras de madeira/metal. */
    private static void contornar(char[][] g, String letras) {
        char[][] c = new char[16][16];
        for (int y = 0; y < 16; y++) c[y] = g[y].clone();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (g[y][x] != '.') continue;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && letras.indexOf(g[ny][nx]) >= 0) { c[y][x] = 'o'; break; }
                }
            }
        }
        for (int y = 0; y < 16; y++) g[y] = c[y];
    }

    private static String[] texto(char[][] g) {
        String[] l = new String[16];
        for (int y = 0; y < 16; y++) l[y] = new String(g[y]);
        return l;
    }

    // =====================================================================
    //  Arsenal (2.51): adaga, faca, machadinha, foice, bastão e chicote
    // =====================================================================

    /** A espada com a lâmina cortada em {@code corte} linhas (a ponta desce junto): adaga e faca. */
    private static String[] encurtar(String[] base, int corte) {
        String[] l = new String[16];
        String vazia = ".".repeat(16);
        for (int y = 0; y < corte; y++) l[y] = vazia;
        l[corte] = base[0].substring(corte) + ".".repeat(corte);
        l[corte + 1] = base[1].substring(corte) + ".".repeat(corte);
        for (int y = corte + 2; y < 16; y++) l[y] = base[y];
        return l;
    }

    /** Machadinha: um machado pequeno, de cabo curto. */
    private static String[] machadinha() {
        char[][] g = vazio();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = (x - 9.5) / 3.6, dy = (y - 5) / 2.8;
                if (dx * dx + dy * dy > 1 || x + y > 15) continue;
                int dist = 15 - (x + y);
                pix(g, x, y, dist >= 6 ? 'h' : dist >= 3 ? 'm' : 'd');
            }
        }
        linha(g, 2, 13, 12, 3, 'w');
        pix(g, 1, 14, 'p');
        pix(g, 10, 5, 'a');
        contornar(g, "whmdap");
        return texto(g);
    }

    /** Foice: cabo comprido na diagonal e a lâmina curva lá em cima, voltada para trás. */
    private static String[] foice() {
        char[][] g = vazio();
        linha(g, 1, 14, 11, 4, 'w');
        linha(g, 2, 14, 12, 4, 'd');
        pix(g, 1, 15, 'p');
        pix(g, 0, 15, 'p');
        pix(g, 11, 3, 'a');
        pix(g, 12, 3, 'a');
        curva(g, 12, 2, 7, -1, 1, 4, 'm', 'h');
        curva(g, 12, 3, 7, 1, 2, 5, 'm', (char) 0);
        pix(g, 1, 5, 'h');
        pix(g, 0, 6, 'h');
        contornar(g, "whmdpa");
        return texto(g);
    }

    /** Bastão: uma haste de ponta a ponta, com ponteiras de metal e a faixa da pegada no meio. */
    private static String[] bastao() {
        char[][] g = vazio();
        linha(g, 1, 14, 14, 1, 'w');
        linha(g, 2, 14, 14, 2, 'h');
        linha(g, 1, 13, 13, 1, 'd');
        for (int[] p : new int[][]{{1, 14}, {2, 14}, {1, 13}, {14, 1}, {14, 2}, {13, 1}, {0, 15}, {15, 0}}) pix(g, p[0], p[1], 'a');
        for (int[] p : new int[][]{{7, 8}, {8, 7}, {7, 7}, {8, 8}, {6, 9}, {9, 6}}) pix(g, p[0], p[1], 'g');
        contornar(g, "whdag");
        return texto(g);
    }

    /** Chicote: cabo curto e a tira de couro fazendo uma volta no ar. */
    private static String[] chicote() {
        char[][] g = vazio();
        linha(g, 1, 14, 5, 10, 'w');
        linha(g, 2, 14, 5, 11, 'd');
        pix(g, 0, 15, 'p');
        pix(g, 1, 15, 'p');
        pix(g, 0, 14, 'p');
        pix(g, 5, 10, 'g');
        pix(g, 6, 10, 'g');
        pix(g, 5, 9, 'g');
        curva(g, 6, 9, 15, 12, 11, 2, 'm', (char) 0);
        curva(g, 11, 2, 9, -1, 5, 2, 'm', (char) 0);
        pix(g, 4, 3, 'a');
        pix(g, 5, 3, 'a');
        contornar(g, "wdpgma");
        return texto(g);
    }

    /** Arco: madeira curvada para cima e à esquerda, corda na diagonal. {@code fase}: -1 parado, 0 a 2 puxando. */
    private static String[] arco(int fase) {
        char[][] g = vazio();
        curva(g, 2, 14, 1, 1, 14, 2, 'w', 'h');
        pix(g, 2, 14, 'a');
        pix(g, 14, 2, 'a');
        pix(g, 1, 15, 'a');
        pix(g, 15, 1, 'a');
        if (fase < 0) {
            linha(g, 3, 13, 13, 3, 's');
        } else {
            int n = 9 + fase; // o ponto da corda vai para baixo e para a direita
            linha(g, 3, 13, n, n, 's');
            linha(g, 13, 3, n, n, 's');
            // A flecha: do ponto da corda até lá na frente (em cima, à esquerda).
            int comp = 7 + fase;
            linha(g, n - 1, n - 1, n - comp, n - comp, 'f');
            pix(g, n - comp, n - comp, 'x');
            pix(g, n - comp - 1, n - comp, 'x');
            pix(g, n - comp, n - comp - 1, 'x');
            pix(g, n, n - 1, 'e');
            pix(g, n - 1, n, 'e');
        }
        contornar(g, "wha");
        return texto(g);
    }

    /** Besta: coronha grossa descendo para baixo e à direita, arco pequeno atravessado na frente. */
    private static String[] besta(String estado, int fase) {
        char[][] g = vazio();
        int n = switch (estado) {
            case "puxando" -> 6 + fase;
            case "flecha", "foguete" -> 9;
            default -> 5;
        };
        // Coronha (3 linhas: luz, madeira e sombra) e a soleira de metal.
        linha(g, 4, 4, 14, 14, 'w');
        linha(g, 5, 4, 14, 13, 'h');
        linha(g, 4, 5, 13, 14, 'd');
        linha(g, 6, 4, 14, 12, 'w');
        pix(g, 15, 15, 'a');
        pix(g, 14, 15, 'a');
        pix(g, 15, 14, 'a');
        // O arco da besta, na frente da coronha, com as pontas de metal.
        curva(g, 0, 9, 1, 1, 9, 0, 'w', (char) 0);
        pix(g, 0, 9, 'a');
        pix(g, 9, 0, 'a');
        linha(g, 1, 8, n, n, 's');
        linha(g, 8, 1, n, n, 's');
        if (estado.equals("flecha")) {
            linha(g, n - 1, n - 1, 2, 2, 'f');
            pix(g, 1, 1, 'x');
            pix(g, 2, 1, 'x');
            pix(g, 1, 2, 'x');
        } else if (estado.equals("foguete")) {
            linha(g, n - 1, n - 1, 3, 3, 'r');
            pix(g, 2, 2, 'x');
        }
        contornar(g, "whad");
        return texto(g);
    }

    /** Machado: lâmina em meia-lua do lado de cima do cabo ({@code barbado}: desce um pouco pelo cabo). */
    private static String[] machado(boolean barbado) {
        char[][] g = vazio();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = (x - 8.5) / 4.6, dy = (y - 4.5) / 3.4;
                boolean lamina = dx * dx + dy * dy <= 1 && x + y <= 14;
                if (barbado) {
                    double bx = (x - 10.5) / 3.0, by = (y - 7.5) / 2.6;
                    lamina |= bx * bx + by * by <= 1 && x + y <= 18 && x + y >= 15 && y > 5;
                }
                if (!lamina) continue;
                int dist = 15 - (x + y); // longe do cabo = fio da lâmina
                pix(g, x, y, dist >= 7 ? 'h' : dist >= 4 ? 'm' : 'd');
            }
        }
        linha(g, 1, 14, 13, 2, 'w');
        pix(g, 10, 5, 'a');
        contornar(g, "whmda");
        return texto(g);
    }

    // =====================================================================
    //  Lanças e tridentes (por código, na diagonal: cabo embaixo à esquerda, ponta em cima à direita)
    // =====================================================================

    /** Quanto o ponto anda ao longo da diagonal (0 no canto de baixo, 15 no de cima). */
    private static double ao(int x, int y) { return (x - y + 15) / 2.0; }

    /** Quanto o ponto sai da diagonal (negativo: para cima e à esquerda). */
    private static double lado(int x, int y) { return (x + y - 15) / 2.0; }

    /** Cabo, ponteira e amarração até {@code ate} na diagonal. */
    private static char[][] haste(int ate) {
        char[][] g = vazio();
        linha(g, 1, 14, ate, 15 - ate, 'w');
        pix(g, 0, 15, 'p');
        pix(g, 1, 14, 'p');
        pix(g, ate, 15 - ate, 'g');
        pix(g, ate - 1, 16 - ate, 'g');
        return g;
    }

    /** Lança: ponta em folha e uma borla pendurada na amarração. */
    private static String[] lanca() {
        char[][] g = haste(10);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double a = ao(x, y), l = lado(x, y);
                if (a < 11 || a > 15.6) continue;
                double larg = a < 12.6 ? 0.5 + (a - 11) * 0.65 : 1.55 * (15.6 - a) / 3.0;
                if (Math.abs(l) > larg) continue;
                pix(g, x, y, l < -0.3 ? 'h' : l > 0.3 ? 'd' : 'm');
            }
        }
        pix(g, 11, 6, 'a');
        pix(g, 11, 7, 'a');
        pix(g, 12, 8, 'a');
        contornar(g, "whmdgpa");
        return texto(g);
    }

    /** Lâmina em meia-lua (como a guandao): a lâmina curva sai para um lado, um esporão para o outro. */
    private static String[] crescente() {
        char[][] g = haste(9);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double a = ao(x, y), l = lado(x, y);
                if (a < 9.5 || a > 15.6) continue;
                double t = (a - 9.5) / 6.1;
                double fora = -0.4 - 2.6 * Math.sin(Math.PI * Math.min(1, t * 1.15)) * (1 - 0.35 * t); // o fio, curvo
                boolean lamina = l <= 0.6 && l >= fora;
                boolean esporao = a >= 10 && a <= 11.5 && l > 0.6 && l <= 1.6;
                if (lamina) pix(g, x, y, l < fora + 0.8 ? 'h' : l > 0 ? 'd' : 'm');
                else if (esporao) pix(g, x, y, 'd');
            }
        }
        pix(g, 10, 6, 'a');
        pix(g, 10, 7, 'a');
        contornar(g, "whmdgpa");
        return texto(g);
    }

    /** Tridente: travessa e três dentes paralelos à diagonal. */
    private static String[] tridente() {
        char[][] g = haste(9);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double a = ao(x, y), l = lado(x, y);
                // Cada dente é uma linha de 2 pixels na diagonal (x + y fixo), com 2 de folga entre eles.
                int s = x + y;
                boolean travessa = a >= 9.6 && a <= 10.6 && Math.abs(l) <= 3;
                boolean meio = a > 10.6 && a <= 15.6 && (s == 15 || s == 16);
                boolean dentes = a > 10.6 && a <= 14 && (s == 11 || s == 12 || s == 19 || s == 20);
                if (travessa) pix(g, x, y, 'd');
                else if (meio) pix(g, x, y, a > 14.4 ? 'h' : s == 15 ? 'h' : 'm');
                else if (dentes) pix(g, x, y, a > 13 ? 'h' : (s == 11 || s == 19) ? 'm' : 'd');
            }
        }
        pix(g, 9, 6, 'a');
        contornar(g, "whmdgpa");
        return texto(g);
    }

    // =====================================================================
    //  Tudo
    // =====================================================================

    static Map<Character, Integer> paleta(Nomeada n) {
        Map<Character, Integer> m = new HashMap<>();
        // Cores que valem para todos (corda, flecha...); a paleta da arma vem por cima.
        m.put('s', 0xFFE0E0E0);
        m.put('f', 0xFF8D6E63);
        m.put('x', 0xFFCFD8DC);
        m.put('e', 0xFFF5F5F5);
        m.put('r', 0xFFC62828);
        m.put('o', 0xFF1A1410);
        Object[] c = n.cores();
        for (int i = 0; i + 1 < c.length; i += 2) m.put((Character) c[i], (Integer) c[i + 1]);
        // Sombra da madeira: a cor dela mais escura, se a paleta não trouxer.
        if (!m.containsKey('d') && m.containsKey('w')) {
            int w = m.get('w');
            int r = (int) (((w >> 16) & 0xFF) * 0.6), g = (int) (((w >> 8) & 0xFF) * 0.6), b = (int) ((w & 0xFF) * 0.6);
            m.put('d', 0xFF000000 | (r << 16) | (g << 8) | b);
        }
        return m;
    }

    /** A paleta das armas dos minérios sem nome (para o 3D na mão, nas mesmas letras das armas com nome). */
    public static Map<Character, Integer> paletaArsenal(String molde) {
        return switch (molde) {
            case "montante" -> cores('o', 0xFF212121, 'h', 0xFFE0F7FA, 'm', 0xFF7FCFE0, 'd', 0xFF7FCFE0, 'g', 0xFFC9A227, 'w', 0xFF4E342E, 'p', 0xFF4E342E, 'a', 0xFF4DD0E1);
            case "bumerangue" -> cores('o', 0xFF3E2723, 'h', 0xFFBC9664, 'm', 0xFFBC9664, 'd', 0xFFBC9664, 'g', 0xFF8D6E4A, 'w', 0xFF8D6E4A, 'p', 0xFFFFD54F, 'a', 0xFFF2A33A);
            case "leques" -> cores('o', 0xFF1B2A10, 'h', 0xFF4CAF7D, 'm', 0xFF4CAF7D, 'd', 0xFF2E7D52, 'g', 0xFFC9A227, 'w', 0xFF4E342E, 'p', 0xFF4E342E, 'a', 0xFF4CAF7D);
            case "arpao" -> cores('o', 0xFF1E1E28, 'h', 0xFFE0E0E0, 'm', 0xFF8C8C96, 'd', 0xFF8C8C96, 'g', 0xFF8D6E63, 'w', 0xFF5D4037, 'p', 0xFFC9A26B, 'a', 0xFFF3E9F0);
            case "mangual" -> cores('o', 0xFF1E2832, 'h', 0xFFF0FAFF, 'm', 0xFFA8E4FF, 'd', 0xFF64B4E6, 'g', 0xFF8D6E63, 'w', 0xFF5D4037, 'p', 0xFF8C8C96, 'a', 0xFFF0FAFF);
            case "garras" -> cores('o', 0xFF1E0A0A, 'h', 0xFFECEFF1, 'm', 0xFF9696A0, 'd', 0xFF9696A0, 'g', 0xFF8D5A2B, 'w', 0xFF4E342E, 'p', 0xFF4E342E, 'a', 0xFFB0102A);
            case "clava" -> cores('o', 0xFF28190A, 'h', 0xFF7A5A36, 'm', 0xFF7A5A36, 'd', 0xFF7A5A36, 'g', 0xFF4E342E, 'w', 0xFF4E342E, 'p', 0xFF3C2819, 'a', 0xFFFF6F00);
            default -> cores('o', 0xFF212121, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF616161, 'g', 0xFF8D6E63, 'w', 0xFF5D4037, 'p', 0xFF8D6E63, 'a', 0xFFFFD54F);
        };
    }

    private static Map<Character, Integer> cores(Object... kv) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((Character) kv[i], (Integer) kv[i + 1]);
        m.put('s', 0xFFE0E0E0);
        return m;
    }

    /** Todos os desenhos (ids = textura). Arcos e bestas têm um por fase. */
    public static List<Desenho> desenhos() {
        List<Desenho> l = new ArrayList<>();
        for (Nomeada n : Nomeada.values()) {
            Map<Character, Integer> cores = paleta(n);
            String id = n.visual();
            switch (n.categoria()) {
                case ARCO -> {
                    l.add(new Desenho(id, arco(-1), cores));
                    for (int f = 0; f < 3; f++) l.add(new Desenho(id + "_pulling_" + f, arco(f), cores));
                }
                case BESTA -> {
                    l.add(new Desenho(id, besta("parada", 0), cores));
                    for (int f = 0; f < 3; f++) l.add(new Desenho(id + "_pulling_" + f, besta("puxando", f), cores));
                    l.add(new Desenho(id + "_arrow", besta("flecha", 0), cores));
                    l.add(new Desenho(id + "_firework", besta("foguete", 0), cores));
                }
                case VARA -> {
                    l.add(new Desenho(id, VARA, cores));
                    String[] lancada = new String[16];
                    for (int y = 0; y < 16; y++) lancada[y] = VARA[y].replace('s', '.').replace('a', '.');
                    l.add(new Desenho(id + "_cast", lancada, cores));
                }
                default -> l.add(new Desenho(id, molde(n.molde()), cores));
            }
        }
        return l;
    }

    /**
     * Arcos, bestas e varas têm definição própria no pacote (fases da corda, vara lançada); lanças
     * e tridentes também (o jogo usa outro modelo na mão).
     */
    public static boolean especial(Categoria c) {
        return c == Categoria.ARCO || c == Categoria.BESTA || c == Categoria.VARA || c == Categoria.LANCA;
    }

    /** Espadas, machados, maça e ferramentas entram como os outros visuais (com as posturas, nas armas). */
    static void registrar(List<ArteItens.Arte> l) {
        for (Nomeada n : Nomeada.values()) {
            if (especial(n.categoria())) continue;
            Map<Character, Integer> cores = paleta(n);
            String[] desenho = switch (n.molde()) {
                case "machado" -> machado(false);
                case "machado2" -> machado(true);
                default -> molde(n.molde());
            };
            for (String v : hospedeiros(n.categoria())) l.add(new ArteItens.Arte(v, n.visual(), desenho, cores, true));
        }
    }

    /** Os itens do jogo em que cada categoria entra. */
    public static List<String> hospedeiros(Categoria c) {
        List<String> l = new ArrayList<>();
        String fim = switch (c) {
            case ESPADA -> "_SWORD";
            case MACHADO -> "_AXE";
            case PICARETA -> "_PICKAXE";
            case PA -> "_SHOVEL";
            case ENXADA -> "_HOE";
            default -> null;
        };
        if (fim == null) {
            switch (c) {
                case MACA -> l.add("mace");
                case ARCO -> l.add("bow");
                case BESTA -> l.add("crossbow");
                case VARA -> l.add("fishing_rod");
                default -> { }
            }
            return l;
        }
        for (org.bukkit.Material m : org.bukkit.Material.values()) {
            String n = m.name();
            if (m.isLegacy() || !m.isItem() || !n.endsWith(fim) || (fim.equals("_AXE") && n.endsWith("_PICKAXE"))) continue;
            l.add(n.toLowerCase(Locale.ROOT));
        }
        return l;
    }
}
