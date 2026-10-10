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

    /** Arcos, bestas e varas têm definição própria no pacote (fases da corda, vara lançada). */
    public static boolean especial(Categoria c) {
        return c == Categoria.ARCO || c == Categoria.BESTA || c == Categoria.VARA;
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
