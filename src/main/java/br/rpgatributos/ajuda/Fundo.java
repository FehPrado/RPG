package br.rpgatributos.ajuda;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Random;

/**
 * Os fundos desenhados dos menus (baú de 6 linhas, 176x222). Entram no pacote de recursos como
 * letras de uma fonte própria: o título do menu "escreve" a imagem por cima do fundo do baú.
 * Cada fundo diz quais espaços têm moldura e quais faixas são painéis.
 */
public enum Fundo {
    /** /rpg: perfil, próximos passos, categorias, atalhos e a barra de baixo. */
    RPG(0xE000, new int[]{4, 11, 13, 15, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 45, 47, 49, 51, 53},
            new int[][]{{1, 1, 2, 6, 1}, {3, 4, 1, 7, 0}}),
    /** Lista de sistemas ou assuntos de uma categoria (até 21). */
    CATEGORIA(0xE001, new int[]{4, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43, 45, 49, 53},
            new int[][]{{1, 4, 1, 7, 2}}),
    /** Um assunto do guia: o ícone, o ritual desenhado em itens, a dica e os assuntos ligados. */
    TOPICO(0xE002, new int[]{4, 19, 20, 21, 22, 23, 24, 25, 31, 37, 38, 39, 40, 41, 42, 43, 45, 49, 53},
            new int[][]{{2, 2, 1, 7, 1}, {4, 4, 1, 7, 2}}),
    /** A Jornada do Aventureiro: 12 passos num caminho. */
    JORNADA(0xE003, new int[]{4, 10, 12, 14, 16, 25, 23, 21, 19, 28, 30, 32, 34, 40, 45, 53},
            new int[][]{{1, 3, 1, 7, 1}});

    /** Os 12 passos da Jornada, em ordem (4 por linha, em zigue-zague). */
    public static final int[] CAMINHO = {10, 12, 14, 16, 25, 23, 21, 19, 28, 30, 32, 34};

    /** Avanços negativos da fonte: volta 8 px antes da imagem e 169 px depois (176 + 1 - 8). */
    public static final char VOLTA_ANTES = (char) 0xF801, VOLTA_DEPOIS = (char) 0xF802;
    public static final int LARGURA = 176, ALTURA = 222;

    private final char letra;
    private final int[] espacos;
    /** Painéis: {linha inicial, linha final, coluna inicial, coluna final, estilo (0 madeira, 1 destaque, 2 claro)}. */
    private final int[][] paineis;

    Fundo(int letra, int[] espacos, int[][] paineis) {
        this.letra = (char) letra;
        this.espacos = espacos;
        this.paineis = paineis;
    }

    public char letra() { return letra; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    // =====================================================================
    //  Desenho (só AWT: roda no servidor e numa prévia fora dele)
    // =====================================================================

    private static final Color MADEIRA = new Color(0x3B2A1A), MADEIRA_CLARA = new Color(0x5E432A), OURO = new Color(0xC8A045);
    private static final Color PAPEL = new Color(0xE9DCC0), PAPEL_ESCURO = new Color(0xD6C49E);
    private static final Color SLOT = new Color(0x9C8461), SLOT_SOMBRA = new Color(0x5B4A33), SLOT_LUZ = new Color(0xF6ECD3);

    public BufferedImage desenhar() {
        BufferedImage img = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Papel com textura leve.
        g.setColor(PAPEL);
        g.fillRect(3, 3, LARGURA - 6, ALTURA - 6);
        Random r = new Random(ordinal() * 7919L + 17);
        for (int i = 0; i < 900; i++) {
            int x = 4 + r.nextInt(LARGURA - 8), y = 4 + r.nextInt(ALTURA - 8);
            g.setColor(r.nextBoolean() ? PAPEL_ESCURO : new Color(0xF1E7D0));
            g.fillRect(x, y, 1, 1);
        }
        // Faixa do título.
        g.setColor(PAPEL_ESCURO);
        g.fillRect(4, 4, LARGURA - 8, 12);
        g.setColor(OURO);
        g.fillRect(4, 16, LARGURA - 8, 1);
        // Painéis.
        for (int[] p : paineis) painel(g, p[0], p[1], p[2], p[3], p[4]);
        if (this == JORNADA) caminho(g);
        // Separação do inventário do jogador.
        g.setColor(OURO);
        g.fillRect(6, 126, LARGURA - 12, 1);
        // Espaços do menu.
        for (int s : espacos) slot(g, 7 + 18 * (s % 9), 17 + 18 * (s / 9));
        // Inventário do jogador (3 linhas + barra).
        for (int lin = 0; lin < 3; lin++) for (int col = 0; col < 9; col++) slot(g, 7 + 18 * col, 139 + 18 * lin);
        for (int col = 0; col < 9; col++) slot(g, 7 + 18 * col, 197);
        // Moldura de madeira com filete dourado.
        g.setColor(MADEIRA);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(1, 1, LARGURA - 3, ALTURA - 3, 8, 8);
        g.setStroke(new BasicStroke(1));
        g.setColor(MADEIRA_CLARA);
        g.drawRoundRect(3, 3, LARGURA - 7, ALTURA - 7, 6, 6);
        g.setColor(OURO);
        for (int[] c : new int[][]{{2, 2}, {LARGURA - 5, 2}, {2, ALTURA - 5}, {LARGURA - 5, ALTURA - 5}}) g.fillRect(c[0], c[1], 3, 3);
        g.dispose();
        // Cantos de fora transparentes (a moldura é arredondada).
        for (int[] c : new int[][]{{0, 0}, {1, 0}, {0, 1}, {LARGURA - 1, 0}, {LARGURA - 2, 0}, {LARGURA - 1, 1},
                {0, ALTURA - 1}, {1, ALTURA - 1}, {0, ALTURA - 2}, {LARGURA - 1, ALTURA - 1}, {LARGURA - 2, ALTURA - 1}, {LARGURA - 1, ALTURA - 2}}) {
            img.setRGB(c[0], c[1], 0);
        }
        return img;
    }

    private static void painel(Graphics2D g, int linIni, int linFim, int colIni, int colFim, int estilo) {
        int x = 7 + 18 * colIni - 3, y = 17 + 18 * linIni - 3;
        int w = 18 * (colFim - colIni + 1) + 6, h = 18 * (linFim - linIni + 1) + 6;
        Color fundo = switch (estilo) {
            case 1 -> new Color(0xF3DE9E);
            case 2 -> new Color(0xF2E9D6);
            default -> new Color(0xCDB88E);
        };
        Color borda = estilo == 1 ? OURO : new Color(0xA88B5E);
        g.setColor(fundo);
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(borda);
        g.drawRoundRect(x, y, w - 1, h - 1, 6, 6);
    }

    /** A trilha pontilhada que liga os passos da Jornada (passa por trás dos espaços). */
    private static void caminho(Graphics2D g) {
        g.setColor(new Color(0x8B6B3A));
        for (int i = 1; i < CAMINHO.length; i++) {
            int a = CAMINHO[i - 1], b = CAMINHO[i];
            int x1 = 16 + 18 * (a % 9), y1 = 26 + 18 * (a / 9), x2 = 16 + 18 * (b % 9), y2 = 26 + 18 * (b / 9);
            int passos = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            for (int s = 0; s <= passos; s += 3) {
                int x = x1 + (x2 - x1) * s / Math.max(1, passos), y = y1 + (y2 - y1) * s / Math.max(1, passos);
                g.fillRect(x - 1, y - 1, 2, 2);
            }
        }
    }

    /** Espaço de item no estilo do jogo (18x18, sombra em cima/esquerda e luz embaixo/direita). */
    private static void slot(Graphics2D g, int x, int y) {
        g.setColor(SLOT_SOMBRA);
        g.fillRect(x, y, 17, 1);
        g.fillRect(x, y, 1, 17);
        g.setColor(SLOT_LUZ);
        g.fillRect(x + 1, y + 17, 17, 1);
        g.fillRect(x + 17, y + 1, 1, 17);
        g.setColor(SLOT);
        g.fillRect(x + 1, y + 1, 16, 16);
    }
}
