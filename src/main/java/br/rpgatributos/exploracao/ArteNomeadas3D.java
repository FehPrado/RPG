package br.rpgatributos.exploracao;

import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Nomeada;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modelos 3D das armas com nome (só na mão, no chão, no suporte e na moldura; no inventário
 * fica o desenho 2D). Cada arma é montada com peças (pomo, cabo, guarda, lâmina...) na mesma
 * diagonal do desenho 2D, com espessura: assim as posições na mão e as pegadas das posturas
 * continuam valendo. As cores vêm de uma textura pequena com um quadrinho por cor da paleta.
 */
public final class ArteNomeadas3D {

    private ArteNomeadas3D() { }

    /** Uma peça: ao longo da arma (t), para o lado (v) e na espessura (w), com a letra da cor. */
    record Peca(double t0, double t1, double v0, double v1, double w0, double w1, char cor) { }

    /** Um elemento pronto do modelo (já na diagonal). */
    public record Elemento(double[] de, double[] ate, double[] origem, char cor) { }

    /** Que tipos ganham modelo 3D. */
    public static boolean tem3D(Categoria c) {
        return switch (c) {
            case ESPADA, MACHADO, MACA, PICARETA, PA, ENXADA, VARA -> true;
            default -> false;
        };
    }

    /** As letras que entram na textura de cores (cada uma num quadrinho de 4x4). */
    private static final String LETRAS = "ohmdgwpas";

    // =====================================================================
    //  As peças de cada tipo
    // =====================================================================

    private static List<Peca> pecas(Nomeada n, boolean lancada) {
        List<Peca> l = new ArrayList<>();
        switch (n.molde()) {
            case "espada", "sabre", "larga" -> {
                boolean larga = n.molde().equals("larga"), sabre = n.molde().equals("sabre");
                double lam = larga ? 3.2 : sabre ? 2.0 : 2.3;
                l.add(new Peca(0, 1.8, -1, 1, -1, 1, 'p'));           // pomo
                l.add(new Peca(1.8, 5.6, -0.6, 0.6, -0.6, 0.6, 'w')); // cabo
                l.add(new Peca(5.6, 6.8, larga ? -3.6 : -3.0, larga ? 3.6 : 3.0, -0.9, 0.9, 'g')); // guarda
                l.add(new Peca(5.9, 6.5, -0.5, 0.5, -1.1, 1.1, 'a'));  // gema da guarda
                if (sabre) {
                    // Lâmina curva: segmentos que vão se afastando para um lado.
                    l.add(new Peca(6.8, 11, -lam / 2, lam / 2, -0.35, 0.35, 'm'));
                    l.add(new Peca(11, 15, -lam / 2 + 0.3, lam / 2 + 0.3, -0.35, 0.35, 'm'));
                    l.add(new Peca(15, 18.5, -lam / 2 + 0.7, lam / 2 + 0.5, -0.3, 0.3, 'h'));
                    l.add(new Peca(18.5, 20.5, 0.4, 1.3, -0.25, 0.25, 'h'));
                } else {
                    l.add(new Peca(6.8, larga ? 17.5 : 18.5, -lam / 2, lam / 2, -0.35, 0.35, 'm'));
                    l.add(new Peca(6.8, larga ? 17.5 : 18.5, lam / 2 - 0.5, lam / 2, -0.4, 0.4, 'h')); // fio claro
                    l.add(new Peca(7.3, larga ? 16.5 : 17.5, -0.25, 0.25, -0.45, 0.45, 'd'));         // canaleta
                    l.add(new Peca(larga ? 17.5 : 18.5, larga ? 19.5 : 20.5, -lam / 3, lam / 3, -0.3, 0.3, 'h')); // ponta
                    l.add(new Peca(larga ? 19.5 : 20.5, larga ? 20.5 : 21.3, -0.4, 0.4, -0.25, 0.25, 'h'));
                }
            }
            case "machado", "machado2" -> {
                l.add(new Peca(0, 19, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(0, 1.2, -0.8, 0.8, -0.8, 0.8, 'd'));
                l.add(new Peca(13, 19, 0.5, 4.5, -0.5, 0.5, 'm'));     // lâmina
                l.add(new Peca(12.3, 19.7, 4.5, 5.6, -0.35, 0.35, 'h')); // fio
                l.add(new Peca(14.5, 17.5, -1.8, 0.5, -0.7, 0.7, 'd'));  // contrapeso
                l.add(new Peca(15.5, 16.5, -0.2, 0.2, -0.8, 0.8, 'a'));  // rebite
                if (n.molde().equals("machado2")) {
                    l.add(new Peca(10.5, 13, 1.5, 4.5, -0.45, 0.45, 'm')); // barba
                    l.add(new Peca(10, 12.3, 4.5, 5.4, -0.3, 0.3, 'h'));
                }
            }
            case "maca" -> {
                l.add(new Peca(0, 15, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(0, 1.4, -0.9, 0.9, -0.9, 0.9, 'p'));
                l.add(new Peca(15, 20, -2, 2, -2, 2, 'm'));
                l.add(new Peca(15.5, 19.5, -2.5, 2.5, -1.5, 1.5, 'd'));
                l.add(new Peca(15.5, 19.5, -1.5, 1.5, -2.5, 2.5, 'h'));
                l.add(new Peca(20, 21.5, -0.6, 0.6, -0.6, 0.6, 'a'));   // ponta
                l.add(new Peca(17, 18, -3.4, 3.4, -0.4, 0.4, 'a'));     // espinhos dos lados
                l.add(new Peca(17, 18, -0.4, 0.4, -3.4, 3.4, 'a'));
            }
            case "martelo" -> {
                l.add(new Peca(0, 15, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(0, 1.4, -0.9, 0.9, -0.9, 0.9, 'p'));
                l.add(new Peca(14.5, 19.5, -4.5, 4.5, -2, 2, 'm'));
                l.add(new Peca(14.8, 19.2, 4.5, 5.2, -1.7, 1.7, 'h'));   // faces
                l.add(new Peca(14.8, 19.2, -5.2, -4.5, -1.7, 1.7, 'd'));
                l.add(new Peca(16.5, 17.5, -1, 1, -2.2, 2.2, 'a'));     // faixa
            }
            case "picareta" -> {
                l.add(new Peca(0, 18, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(16, 18, -6.5, 6.5, -0.7, 0.7, 'm'));
                l.add(new Peca(14, 16.5, 5.5, 7, -0.5, 0.5, 'h'));       // pontas curvadas
                l.add(new Peca(14, 16.5, -7, -5.5, -0.5, 0.5, 'd'));
                l.add(new Peca(18, 18.6, -5, 5, -0.5, 0.5, 'h'));
                l.add(new Peca(16.3, 17.7, -0.9, 0.9, -0.9, 0.9, 'a'));
            }
            case "pa" -> {
                l.add(new Peca(0, 14, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(0, 1.4, -0.9, 0.9, -0.9, 0.9, 'p'));
                l.add(new Peca(12.5, 14.5, -0.9, 0.9, -0.8, 0.8, 'd'));  // encaixe
                l.add(new Peca(14.5, 16, -1.4, 1.4, -0.35, 0.35, 'm'));   // ombro estreito
                l.add(new Peca(16, 20.5, -2, 2, -0.3, 0.3, 'm'));         // lâmina comprida
                l.add(new Peca(16, 20.5, -2, -1.5, -0.4, 0.4, 'h'));
                l.add(new Peca(20.5, 21.6, -1.3, 1.3, -0.25, 0.25, 'h')); // ponta arredondada
            }
            case "enxada" -> {
                l.add(new Peca(0, 19, -0.6, 0.6, -0.6, 0.6, 'w'));
                l.add(new Peca(0, 1.4, -0.9, 0.9, -0.9, 0.9, 'p'));
                l.add(new Peca(17, 19.2, -0.8, 4.8, -0.35, 0.35, 'm'));   // lâmina para o lado
                l.add(new Peca(15.5, 17, 3.8, 4.8, -0.35, 0.35, 'h'));    // fio dobrado
                l.add(new Peca(17.5, 18.7, -1.2, 0.2, -0.8, 0.8, 'd'));
            }
            case "vara" -> {
                l.add(new Peca(0, 4, -0.8, 0.8, -0.8, 0.8, 'g'));        // cabo
                l.add(new Peca(0, 0.8, -1, 1, -1, 1, 'p'));
                l.add(new Peca(4, 13, -0.45, 0.45, -0.45, 0.45, 'w'));
                l.add(new Peca(13, 21, -0.3, 0.3, -0.3, 0.3, 'w'));
                l.add(new Peca(3.5, 5, -1.9, -0.6, -0.7, 0.7, 'a'));     // molinete
                l.add(new Peca(20.5, 21.5, -0.5, 0.5, -0.5, 0.5, 'a'));  // ponteira
            }
            default -> { }
        }
        return l;
    }

    // =====================================================================
    //  Na diagonal
    // =====================================================================

    private static final double R2 = Math.sqrt(0.5);

    /** As peças viram elementos girados 45° em volta do eixo z, ao longo da diagonal do desenho. */
    public static List<Elemento> elementos(Nomeada n, boolean lancada) {
        List<Elemento> l = new ArrayList<>();
        double bx = 1.2, by = 1.2;
        for (Peca p : pecas(n, lancada)) {
            double tc = (p.t0() + p.t1()) / 2, vc = (p.v0() + p.v1()) / 2, wc = 8 + (p.w0() + p.w1()) / 2;
            double cx = bx + tc * R2 - vc * R2, cy = by + tc * R2 + vc * R2;
            double lt = (p.t1() - p.t0()) / 2, lv = (p.v1() - p.v0()) / 2, lw = (p.w1() - p.w0()) / 2;
            l.add(new Elemento(new double[]{cx - lt, cy - lv, wc - lw}, new double[]{cx + lt, cy + lv, wc + lw}, new double[]{cx, cy, wc}, p.cor()));
        }
        // A linha da vara pendurada da ponta (não gira: cai reta), só quando não está lançada.
        if (n.categoria() == Categoria.VARA && !lancada) {
            double px = bx + 21 * R2, py = by + 21 * R2;
            l.add(new Elemento(new double[]{px - 0.15, py - 11, 7.85}, new double[]{px + 0.15, py, 8.15}, null, 's'));
            l.add(new Elemento(new double[]{px - 0.6, py - 12, 7.6}, new double[]{px + 0.6, py - 11, 8.4}, null, 'a'));
        }
        return l;
    }

    // =====================================================================
    //  JSON e textura
    // =====================================================================

    private static String num(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    private static String uv(char c) {
        int i = Math.max(0, LETRAS.indexOf(c));
        int x = (i % 4) * 4, y = (i / 4) * 4;
        return "[" + (x + 1) + ", " + (y + 1) + ", " + (x + 3) + ", " + (y + 3) + "]";
    }

    private static final String DISPLAY = """
            "display": {
              "thirdperson_righthand": { "rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85] },
              "thirdperson_lefthand": { "rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85] },
              "firstperson_righthand": { "rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68] },
              "firstperson_lefthand": { "rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68] },
              "ground": { "rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },
              "head": { "rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1] },
              "fixed": { "rotation": [0, 180, 0], "scale": [1, 1, 1] }
            }""";

    private static final String DISPLAY_VARA = """
            "display": {
              "thirdperson_righthand": { "rotation": [0, 90, 55], "translation": [0, 4.0, 2.5], "scale": [0.85, 0.85, 0.85] },
              "thirdperson_lefthand": { "rotation": [0, -90, -55], "translation": [0, 4.0, 2.5], "scale": [0.85, 0.85, 0.85] },
              "firstperson_righthand": { "rotation": [0, 90, 25], "translation": [0, 1.6, 0.8], "scale": [0.68, 0.68, 0.68] },
              "firstperson_lefthand": { "rotation": [0, -90, -25], "translation": [0, 1.6, 0.8], "scale": [0.68, 0.68, 0.68] },
              "ground": { "rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },
              "head": { "rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1] },
              "fixed": { "rotation": [0, 180, 0], "scale": [1, 1, 1] }
            }""";

    /** O modelo 3D (textura: rpgatributos:item/{visual}_cores). */
    public static String modelo(Nomeada n, boolean lancada) {
        String tex = "rpgatributos:item/" + n.visual() + "_cores";
        StringBuilder sb = new StringBuilder("{ \"textures\": { \"0\": \"" + tex + "\", \"particle\": \"" + tex + "\" },\n  \"elements\": [\n");
        List<Elemento> els = elementos(n, lancada);
        for (int i = 0; i < els.size(); i++) {
            Elemento e = els.get(i);
            String uv = uv(e.cor());
            sb.append("    { \"from\": [").append(num(e.de()[0])).append(", ").append(num(e.de()[1])).append(", ").append(num(e.de()[2])).append("], ")
                    .append("\"to\": [").append(num(e.ate()[0])).append(", ").append(num(e.ate()[1])).append(", ").append(num(e.ate()[2])).append("], ");
            if (e.origem() != null) {
                sb.append("\"rotation\": { \"angle\": 45, \"axis\": \"z\", \"origin\": [").append(num(e.origem()[0])).append(", ")
                        .append(num(e.origem()[1])).append(", ").append(num(e.origem()[2])).append("] }, ");
            }
            sb.append("\"faces\": { ");
            String[] lados = {"north", "east", "south", "west", "up", "down"};
            for (int k = 0; k < lados.length; k++) {
                sb.append(k == 0 ? "" : ", ").append('"').append(lados[k]).append("\": { \"uv\": ").append(uv).append(", \"texture\": \"#0\" }");
            }
            sb.append(" } }").append(i < els.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ],\n  ").append(n.categoria() == Categoria.VARA ? DISPLAY_VARA : DISPLAY).append("\n}\n");
        return sb.toString();
    }

    /** A textura de cores: um quadrinho 4x4 de cada cor da paleta (com uma borda um pouco mais escura). */
    public static BufferedImage cores(Map<Character, Integer> paleta) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < LETRAS.length(); i++) {
            Integer c = paleta.get(LETRAS.charAt(i));
            if (c == null) c = 0xFFFF00FF;
            int x0 = (i % 4) * 4, y0 = (i / 4) * 4;
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    boolean borda = x == 0 || y == 0 || x == 3 || y == 3;
                    int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                    double f = borda ? 0.82 : 1.0;
                    im.setRGB(x0 + x, y0 + y, 0xFF000000 | ((int) (r * f) << 16) | ((int) (g * f) << 8) | (int) (b * f));
                }
            }
        }
        return im;
    }
}
