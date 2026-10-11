package br.rpgatributos.domador;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modelos 3D das lendárias para quem joga com os mods EMF + ETF (ou OptiFine). Quem não tem os
 * mods continua vendo a pele normal (o jogo ignora a pasta optifine/cem). O modelo vale só para a
 * criatura com o nome da lendária (regras por nome em horse.properties) e esconde a camada da pele
 * (horse_armor) para ela não cobrir o modelo. As partes têm os nomes do jogo, então pernas, cabeça e
 * cauda continuam animadas. Coordenadas no padrão dos modelos de criatura: chão em y 0, frente em -z.
 */
public final class ModelosLendarios {

    private ModelosLendarios() { }

    /** Uma caixa: posição, tamanho e o canto da textura (box UV). */
    record Caixa(double x, double y, double z, double w, double h, double d, int u, int v) { }

    /** Uma parte do modelo do jogo, com o pivô dela e as caixas. */
    record Parte(String nome, double[] pivo, List<Caixa> caixas) { }

    // Amostras de cor na metade direita da textura (cada uma 16 x 32).
    static final int OURO = 0, OURO_CLARO = 1, CHAMA = 2, CHAMA_CLARA = 3, BRANCO = 4, ESCURO = 5, JADE = 6, VERMELHO = 7;

    private static int[] amostra(int i) { return new int[]{64 + (i % 4) * 16, (i / 4) * 32}; }

    private static Caixa c(double x, double y, double z, double w, double h, double d, int amostra) {
        int[] uv = amostra(amostra);
        return new Caixa(x, y, z, w, h, d, uv[0], uv[1]);
    }

    private static Caixa pele(double x, double y, double z, double w, double h, double d, int u, int v) {
        return new Caixa(x, y, z, w, h, d, u, v);
    }

    // =====================================================================
    //  Qilin
    // =====================================================================

    static List<Parte> qilin() {
        List<Parte> l = new ArrayList<>();
        double[] cabeca = {0, 22, -9};
        l.add(new Parte("head", cabeca, List.of(
                pele(-3, 28, -11, 6, 5, 7, 0, 13),
                // Chifres dourados ramificados, voltados para trás
                c(-2.5, 33, -7, 1, 4, 1, OURO), c(1.5, 33, -7, 1, 4, 1, OURO),
                c(-2.5, 36, -6, 1, 1, 3, OURO_CLARO), c(1.5, 36, -6, 1, 1, 3, OURO_CLARO),
                c(-3.5, 35, -7, 1, 1, 1, OURO_CLARO), c(2.5, 35, -7, 1, 1, 1, OURO_CLARO))));
        l.add(new Parte("mouth", cabeca, List.of(
                pele(-2, 28, -16, 4, 5, 5, 0, 25),
                // Bigodes longos e a barbicha de chama
                c(-6, 30, -15.5, 4, 0.5, 0.5, OURO_CLARO), c(2, 30, -15.5, 4, 0.5, 0.5, OURO_CLARO),
                c(-1, 26, -15, 2, 2, 2, CHAMA))));
        l.add(new Parte("left_ear", new double[]{0, 23, -9.01}, List.of(pele(-2.55, 33, -5.01, 2, 3, 1, 19, 16))));
        l.add(new Parte("right_ear", new double[]{0, 23, -9.01}, List.of(pele(0.55, 33, -5.01, 2, 3, 1, 19, 16))));
        l.add(new Parte("neck", cabeca, List.of(pele(-1.95, 16, -11, 4, 12, 7, 0, 35))));
        l.add(new Parte("mane", new double[]{0, 22, -9.01}, List.of(
                pele(-1, 17, -4, 2, 16, 2, 56, 36),
                // Crista de chama verde ao longo do pescoço
                c(-1.5, 30, -3, 3, 5, 2, CHAMA), c(-1.5, 25, -2, 3, 5, 2, CHAMA), c(-1.5, 20, -1, 3, 5, 2, CHAMA),
                c(-0.5, 34, -3, 1, 2, 1, CHAMA_CLARA), c(-0.5, 29, -2, 1, 2, 1, CHAMA_CLARA))));
        List<Caixa> corpo = new ArrayList<>(List.of(pele(-5, 11, -11, 10, 10, 22, 0, 32)));
        // Escamas douradas na espinha
        for (double z = -9; z <= 8; z += 4) corpo.add(c(-0.75, 21, z, 1.5, 1.5, 2, OURO));
        l.add(new Parte("body", new double[]{0, 13, 6}, corpo));
        l.add(new Parte("tail", new double[]{0, 21, 11}, List.of(
                pele(-1.5, 7, 11, 3, 14, 4, 42, 36),
                // Tufo de chama na ponta
                c(-2.5, 3, 10.5, 5, 5, 5, CHAMA), c(-1.5, 1, 11.5, 3, 2, 3, CHAMA_CLARA))));
        String[][] pernas = {{"front_left_leg", "-4", "-9", "-5", "-10.9"}, {"front_right_leg", "4", "-9", "1", "-10.9"},
                {"back_left_leg", "-4", "8", "-5", "7"}, {"back_right_leg", "4", "8", "1", "7"}};
        for (String[] p : pernas) {
            double px = Double.parseDouble(p[1]), pz = Double.parseDouble(p[2]), x = Double.parseDouble(p[3]), z = Double.parseDouble(p[4]);
            l.add(new Parte(p[0], new double[]{px, 10, pz}, List.of(
                    pele(x, 0, z, 4, 11, 4, 48, 21),
                    // Chama verde no tornozelo
                    c(x - 0.5, 3, z - 0.5, 5, 2, 5, CHAMA))));
        }
        return l;
    }

    /** Nomes das partes do cavalo (para a camada da pele ficar vazia). */
    private static final String[] PARTES_CAVALO = {"head", "mouth", "left_ear", "right_ear", "neck", "mane", "body", "tail",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg"};

    // =====================================================================
    //  JSON
    // =====================================================================

    private static String n(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.2f", d);
    }

    static String jem(List<Parte> partes, String textura) {
        StringBuilder sb = new StringBuilder("{\n  \"texture\": \"" + textura + "\",\n  \"textureSize\": [128, 64],\n  \"models\": [\n");
        for (int i = 0; i < partes.size(); i++) {
            Parte p = partes.get(i);
            sb.append("    { \"part\": \"").append(p.nome()).append("\", \"id\": \"").append(p.nome()).append("\", \"invertAxis\": \"xy\", ")
                    .append("\"translate\": [").append(n(-p.pivo()[0])).append(", ").append(n(-p.pivo()[1])).append(", ").append(n(-p.pivo()[2])).append("],\n")
                    .append("      \"boxes\": [\n");
            for (int k = 0; k < p.caixas().size(); k++) {
                Caixa c = p.caixas().get(k);
                sb.append("        { \"coordinates\": [").append(n(c.x())).append(", ").append(n(c.y())).append(", ").append(n(c.z())).append(", ")
                        .append(n(c.w())).append(", ").append(n(c.h())).append(", ").append(n(c.d())).append("], \"textureOffset\": [")
                        .append(c.u()).append(", ").append(c.v()).append("] }").append(k < p.caixas().size() - 1 ? ",\n" : "\n");
            }
            sb.append("      ] }").append(i < partes.size() - 1 ? ",\n" : "\n");
        }
        return sb.append("  ]\n}\n").toString();
    }

    /** A camada da pele (armadura de cavalo) sem nenhuma caixa: some para quem vê o modelo. */
    private static String jemVazio() {
        StringBuilder sb = new StringBuilder("{\n  \"textureSize\": [64, 64],\n  \"models\": [\n");
        for (int i = 0; i < PARTES_CAVALO.length; i++) {
            sb.append("    { \"part\": \"").append(PARTES_CAVALO[i]).append("\", \"id\": \"").append(PARTES_CAVALO[i])
                    .append("\", \"invertAxis\": \"xy\", \"translate\": [0, 0, 0], \"boxes\": [] }").append(i < PARTES_CAVALO.length - 1 ? ",\n" : "\n");
        }
        return sb.append("  ]\n}\n").toString();
    }

    // =====================================================================
    //  Textura: a pele do cavalo à esquerda, as amostras de cor à direita
    // =====================================================================

    private static final int[][] CORES_QILIN = {
            {0xFFE2B53C, 0xFFB8892A}, {0xFFFFE082, 0xFFE2B53C}, {0xFF4FB286, 0xFF2E7D32}, {0xFFE8F5C8, 0xFF9CF0C0},
            {0xFFFFFFFF, 0xFFE0E0E0}, {0xFF10301F, 0xFF1F4B36}, {0xFF6CCB9C, 0xFF4FB286}, {0xFFD32F2F, 0xFF8E1B1B}};

    static BufferedImage textura(VisualDomador.Armadura pele, int[][] cores) throws IOException {
        BufferedImage im = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage base = VisualDomador.pintarPele(pele, VisualDomador.mapa("/mascaras/cavalo_pele.txt"), false);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) im.setRGB(x, y, base.getRGB(x, y));
        for (int i = 0; i < cores.length; i++) {
            int[] o = amostra(i);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 16; x++) {
                    // Do claro (em cima) para o escuro (embaixo), com um pontilhado leve.
                    boolean escuro = y > 20 || ((x + y) % 7 == 0 && y > 8);
                    im.setRGB(o[0] + x, o[1] + y, escuro ? cores[i][1] : cores[i][0]);
                }
            }
        }
        return im;
    }

    // =====================================================================
    //  Arquivos do pacote
    // =====================================================================

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        String cem = "assets/minecraft/optifine/cem/";
        f.put(cem + "horse2.jem", texto(jem(qilin(), "textures/entity/rpgatributos/qilin.png")));
        f.put(cem + "horse_armor2.jem", texto(jemVazio()));
        String regra = "# Modelos das lendárias do RPGAtributos (só com os mods EMF + ETF ou OptiFine).\n"
                + "models.2=2\nname.2=regex:Qilin.*\n";
        f.put(cem + "horse.properties", texto(regra));
        f.put(cem + "horse_armor.properties", texto(regra));
        f.put("assets/minecraft/textures/entity/rpgatributos/qilin.png", VisualDomador.png(textura(VisualDomador.Armadura.QILIN, CORES_QILIN)));
        return f;
    }

    private static byte[] texto(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
