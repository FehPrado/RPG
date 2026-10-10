package br.rpgatributos.exploracao;

import br.rpgatributos.forja.Traje;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * O desenho dos {@link Traje Trajes}: o ícone de cada peça no inventário (moldes de 16x16
 * pintados com a paleta do traje) e a textura no corpo de quem veste, pintada por código no
 * mapa de 64x32 das armaduras do jogo (camada de cima: elmo, peitoral e botas; camada das pernas:
 * cinto e grevas). Nada é copiado do jogo: só as posições das faces do modelo.
 */
public final class ArteTrajes {

    private ArteTrajes() { }

    // =====================================================================
    //  Ícones (o = contorno, b = cor, h = luz, d = sombra, t = detalhe, g = gema)
    // =====================================================================

    private static final String[] CAPACETE = {
            "................",
            ".....oooooo.....",
            "....ohhhhhbo....",
            "...ohhbbbbbdo...",
            "..ohbbbbbbbbdo..",
            "..ohbbbbbbbbdo..",
            "..otttttgtttto..",
            "..obbbbbbbbbdo..",
            "..obdoooooodbo..",
            "..obbbbtbbbbdo..",
            "..obbbbtbbbbdo..",
            "..obbbbtbbbbdo..",
            "..obbbdddbbbdo..",
            "..otbbo..obbto..",
            "..otto....otto..",
            "...oo......oo..."};

    private static final String[] PEITORAL = {
            "................",
            "..ooo......ooo..",
            ".ohhbo....obhdo.",
            "ohhbbboooobbbddo",
            "ohbbbbbttbbbbbdo",
            "obbbbbbttbbbbbdo",
            "oobbbbbggbbbbboo",
            ".obbbbbttbbbbdo.",
            ".obhbbbttbbbbdo.",
            ".obhbbbttbbbbdo.",
            ".obbbbbttbbbbdo.",
            ".otttttttttttto.",
            ".obbbbbbbbbbbdo.",
            ".obbbbbbbbbbddo.",
            "..odddddddddoo..",
            "................"};

    private static final String[] CALCA = {
            "................",
            "..oooooooooooo..",
            "..ottttggtttto..",
            "..obhbbbbbbbdo..",
            "..obhbbbbbbbdo..",
            "..obhbdoobhbdo..",
            "..obhbdoobhbdo..",
            "..otttdootttdo..",
            "..obhbdoobhbdo..",
            "..obhbdoobhbdo..",
            "..obhbdoobhbdo..",
            "..obhbdoobhbdo..",
            "..obbbdoobbbdo..",
            "..oddddooddddo..",
            "..oooooooooooo..",
            "................"};

    private static final String[] BOTAS = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "..oooo....oooo..",
            "..ohbo....ohbo..",
            "..ohbo....ohbo..",
            "..otto....otto..",
            "..ohbo....ohbo..",
            "..ohbbo...ohbbo.",
            "..ohbbbo..ohbbbo",
            ".ohbbbbo.ohbbbbo",
            ".ottttto.ottttto",
            ".oooooo..oooooo.",
            "................"};

    static String[] molde(Traje.Peca p) {
        return switch (p) {
            case CAPACETE -> CAPACETE;
            case PEITORAL -> PEITORAL;
            case CALCA -> CALCA;
            case BOTAS -> BOTAS;
        };
    }

    static Map<Character, Integer> paleta(Traje t) {
        Map<Character, Integer> m = new HashMap<>();
        m.put('o', escurecer(t.sombra(), 0.55));
        m.put('b', t.base());
        m.put('h', t.luz());
        m.put('d', t.sombra());
        m.put('t', t.detalhe());
        m.put('g', t.gema());
        return m;
    }

    /** Os ícones entram nas peças de armadura do jogo (couro, malha, ferro, ouro, diamante, netherita...). */
    static void registrar(List<ArteItens.Arte> l) {
        for (Traje t : Traje.values()) {
            Map<Character, Integer> cores = paleta(t);
            for (Traje.Peca p : Traje.Peca.values()) {
                for (String v : hospedeiros(p)) l.add(new ArteItens.Arte(v, t.visual(p), molde(p), cores));
            }
        }
    }

    /** Os itens do jogo de cada peça (o casco de tartaruga fica de fora). */
    public static List<String> hospedeiros(Traje.Peca p) {
        List<String> l = new ArrayList<>();
        for (org.bukkit.Material m : org.bukkit.Material.values()) {
            String n = m.name();
            if (m.isLegacy() || !m.isItem() || !n.endsWith(p.sufixo()) || m == org.bukkit.Material.TURTLE_HELMET) continue;
            l.add(n.toLowerCase(Locale.ROOT));
        }
        return l;
    }

    // =====================================================================
    //  No corpo (64x32, o mapa das armaduras do jogo)
    // =====================================================================

    /** O que vai em cada pixel de uma face. */
    private enum Parte { ELMO_TOPO, ELMO_LADO, ELMO_FRENTE, TRONCO_TOPO, TRONCO, TRONCO_FRENTE, BRACO_TOPO, BRACO,
        BOTA, BOTA_SOLA, CINTO, CINTO_FRENTE, PERNA, PERNA_FRENTE }

    /** Camada de cima: elmo (cabeça), peitoral (tronco e braços) e botas (fim das pernas). */
    public static BufferedImage corpo(Traje t) {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        // Cabeça (8x8x8 em 0,0): topo, direita, frente, esquerda e trás.
        face(im, t, 8, 0, 8, 8, Parte.ELMO_TOPO);
        face(im, t, 0, 8, 8, 8, Parte.ELMO_LADO);
        face(im, t, 8, 8, 8, 8, Parte.ELMO_FRENTE);
        face(im, t, 16, 8, 8, 8, Parte.ELMO_LADO);
        face(im, t, 24, 8, 8, 8, Parte.ELMO_LADO);
        // Tronco (8x12x4 em 16,16).
        face(im, t, 20, 16, 8, 4, Parte.TRONCO_TOPO);
        face(im, t, 16, 20, 4, 12, Parte.TRONCO);
        face(im, t, 20, 20, 8, 12, Parte.TRONCO_FRENTE);
        face(im, t, 28, 20, 4, 12, Parte.TRONCO);
        face(im, t, 32, 20, 8, 12, Parte.TRONCO);
        // Braço (4x12x4 em 40,16): o jogo usa o mesmo para os dois.
        face(im, t, 44, 16, 4, 4, Parte.BRACO_TOPO);
        for (int x = 40; x < 56; x += 4) face(im, t, x, 20, 4, 12, Parte.BRACO);
        // Pernas (4x12x4 em 0,16): só as botas, embaixo.
        for (int x = 0; x < 16; x += 4) face(im, t, x, 20, 4, 12, Parte.BOTA);
        face(im, t, 8, 16, 4, 4, Parte.BOTA_SOLA);
        return im;
    }

    /** Camada das pernas: cinto (fim do tronco) e grevas. */
    public static BufferedImage pernas(Traje t) {
        BufferedImage im = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        face(im, t, 16, 20, 4, 12, Parte.CINTO);
        face(im, t, 20, 20, 8, 12, Parte.CINTO_FRENTE);
        face(im, t, 28, 20, 4, 12, Parte.CINTO);
        face(im, t, 32, 20, 8, 12, Parte.CINTO);
        face(im, t, 0, 20, 4, 12, Parte.PERNA);
        face(im, t, 4, 20, 4, 12, Parte.PERNA_FRENTE);
        face(im, t, 8, 20, 4, 12, Parte.PERNA);
        face(im, t, 12, 20, 4, 12, Parte.PERNA);
        face(im, t, 4, 16, 4, 4, Parte.PERNA); // topo da perna (aparece pouco)
        return im;
    }

    private static void face(BufferedImage im, Traje t, int x0, int y0, int w, int h, Parte parte) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = pixel(t, parte, x, y, w, h, x0 * 31 + y0 * 17);
                if (c != 0) im.setRGB(x0 + x, y0 + y, c);
            }
        }
    }

    /** A cor de um pixel de uma face (0 = vazio). */
    private static int pixel(Traje t, Parte parte, int x, int y, int w, int h, int semente) {
        boolean borda = x == 0 || x == w - 1;
        switch (parte) {
            case ELMO_TOPO -> {
                if (t.estilo() != Traje.Estilo.TECIDO && (x == 3 || x == 4)) return y == 0 ? t.luz() : t.detalhe(); // a crista
                return padrao(t, x, y, semente, borda || y == 0 || y == h - 1);
            }
            case ELMO_LADO -> {
                if (y == 5) return t.detalhe();
                if (y == 7) return t.sombra();
                return padrao(t, x, y, semente, false);
            }
            case ELMO_FRENTE -> {
                // A testa e a faixa; embaixo, só as laterais e o protetor do nariz (o rosto aparece).
                if (y <= 1) return y == 1 && (x == 3 || x == 4) ? t.gema() : padrao(t, x, y, semente, false);
                if (y == 2) return t.detalhe();
                if (t.estilo() == Traje.Estilo.TECIDO) return (x == 0 || x == 7) ? t.sombra() : 0;
                if (x <= 1 || x >= 6) return y == 7 ? t.detalhe() : padrao(t, x, y, semente, x == 1 || x == 6);
                if ((x == 3 || x == 4) && y <= 5) return t.sombra();
                return 0;
            }
            case TRONCO_TOPO -> {
                return y == 0 || y == h - 1 ? t.detalhe() : t.luz();
            }
            case TRONCO -> {
                if (y == 0 || y == h - 1) return t.detalhe();
                return padrao(t, x, y, semente, borda);
            }
            case TRONCO_FRENTE -> {
                if (y == 0 || y == h - 1) return t.detalhe();
                if ((x == 3 || x == 4) && (y == 3 || y == 4)) return t.gema();
                if ((x == 3 || x == 4) && (y == 2 || y == 5)) return t.detalhe();
                if (t.estilo() == Traje.Estilo.TECIDO && (x == 3 || x == 4) && y > 5) return t.detalhe(); // os laços do gibão
                return padrao(t, x, y, semente, borda);
            }
            case BRACO_TOPO -> {
                return t.luz();
            }
            case BRACO -> {
                // Ombreira em cima, braçadeira embaixo.
                if (y <= 2) return t.estilo() == Traje.Estilo.TECIDO ? padrao(t, x, y, semente, false) : (y == 0 ? t.luz() : t.base());
                if (y == 3) return t.detalhe();
                if (y >= 9) return y == 9 ? t.detalhe() : t.sombra();
                return padrao(t, x, y, semente, false);
            }
            case BOTA -> {
                if (y < 7) return 0;
                if (y == 7) return t.detalhe();
                if (y == h - 1) return escurecer(t.sombra(), 0.7);
                return padrao(t, x, y, semente, false);
            }
            case BOTA_SOLA -> {
                return escurecer(t.sombra(), 0.7);
            }
            case CINTO -> {
                if (y < 8) return 0;
                if (y == 9 || y == 10) return t.detalhe();
                return t.sombra();
            }
            case CINTO_FRENTE -> {
                if (y < 8) return 0;
                if ((y == 9 || y == 10) && (x == 3 || x == 4)) return t.gema(); // a fivela
                if (y == 9 || y == 10) return t.detalhe();
                return t.sombra();
            }
            case PERNA -> {
                if (y >= 10) return 0; // as botas cobrem o fim
                return padrao(t, x, y, semente, false);
            }
            case PERNA_FRENTE -> {
                if (y >= 10) return 0;
                if (t.estilo() != Traje.Estilo.TECIDO && (y == 4 || y == 5)) return y == 4 ? t.luz() : t.detalhe(); // joelheira
                return padrao(t, x, y, semente, false);
            }
        }
        return 0;
    }

    /** A textura do material: placas com emendas, escamas ou tecido trançado (com um pouco de ruído). */
    private static int padrao(Traje t, int x, int y, int semente, boolean sombraNaBorda) {
        int c = switch (t.estilo()) {
            case PLACAS -> y % 4 == 3 ? t.sombra() : y % 4 == 0 ? t.luz() : t.base();
            case ESCAMAS -> {
                int deslocado = (y / 2) % 2;
                if (y % 2 == 0) yield (x + deslocado) % 2 == 0 ? t.luz() : t.base();
                yield (x + deslocado) % 2 == 0 ? t.base() : t.sombra();
            }
            case TECIDO -> (x + y) % 4 == 0 ? t.sombra() : (x * 3 + y) % 7 == 0 ? t.luz() : t.base();
        };
        if (sombraNaBorda) c = misturar(c, t.sombra(), 0.5);
        // Ruído leve e sempre igual (a mesma textura toda vez que o pacote é gerado).
        int r = ((x * 73856093) ^ (y * 19349663) ^ semente) & 7;
        return r == 0 ? clarear(c, 1.08) : r == 1 ? escurecer(c, 0.93) : c;
    }

    // =====================================================================
    //  Pacote de recursos
    // =====================================================================

    /** Arquivos do equipamento de cada traje (o ícone de cada peça entra pelo {@link ArteItens}). */
    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new HashMap<>();
        for (Traje t : Traje.values()) {
            String id = t.equipamento();
            f.put("assets/rpgatributos/equipment/" + id + ".json", ("""
                    { "layers": {
                        "humanoid": [ { "texture": "rpgatributos:%1$s" } ],
                        "humanoid_leggings": [ { "texture": "rpgatributos:%1$s" } ]
                    } }
                    """.formatted(id)).getBytes(StandardCharsets.UTF_8));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid/" + id + ".png", png(corpo(t)));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid_leggings/" + id + ".png", png(pernas(t)));
        }
        return f;
    }

    // =====================================================================
    //  Cores
    // =====================================================================

    static int escurecer(int argb, double f) {
        int r = (int) (((argb >> 16) & 0xFF) * f), g = (int) (((argb >> 8) & 0xFF) * f), b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int clarear(int argb, double f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f)),
                b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int misturar(int a, int b, double f) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = (int) (((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = (int) ((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static byte[] png(BufferedImage im) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(im, "png", b);
        return b.toByteArray();
    }
}
