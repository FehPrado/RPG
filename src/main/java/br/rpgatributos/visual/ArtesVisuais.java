package br.rpgatributos.visual;

import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.masmorra.Tema;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Modelos nativos do pacote: decoração, acessórios de corpo e efeitos mágicos. */
public final class ArtesVisuais {
    private ArtesVisuais() { }
    private static final String ROOT = "assets/rpgatributos/";

    public static final class Modelo {
        final JsonArray elementos = new JsonArray();
        public Modelo caixa(double x, double y, double z, double w, double h, double d, int cor) {
            JsonObject e = new JsonObject();
            e.add("from", vetor(x, y, z)); e.add("to", vetor(x + w, y + h, z + d));
            JsonObject faces = new JsonObject();
            for (String lado : new String[]{"north", "south", "east", "west", "up", "down"}) {
                JsonObject face = new JsonObject(); face.addProperty("texture", "#t");
                face.addProperty("tintindex", 0);
                face.add("uv", vetor((cor % 4) * 4, (cor / 4) * 4, (cor % 4 + 1) * 4, (cor / 4 + 1) * 4));
                faces.add(lado, face);
            }
            e.add("faces", faces); elementos.add(e); return this;
        }
        public Modelo anel(double y, double raio, int cor) {
            for (int n = 0; n < 24; n++) {
                double a = Math.PI * 2 * n / 24;
                caixa(7.4 + Math.cos(a) * raio, y, 7.4 + Math.sin(a) * raio, 1.2, .45, 1.2, cor);
            }
            return this;
        }
        String json(String textura) {
            JsonObject obj = new JsonObject(), tex = new JsonObject();
            tex.addProperty("t", "rpgatributos:item/beleza_" + textura); tex.addProperty("particle", "#t");
            obj.add("textures", tex); obj.add("elements", elementos);
            JsonObject display = new JsonObject();
            for (String tipo : new String[]{"gui", "ground", "fixed", "thirdperson_righthand", "firstperson_righthand"}) {
                JsonObject tr = new JsonObject(); tr.add("rotation", vetor(25, 215, 0)); tr.add("scale", vetor(.48, .48, .48)); display.add(tipo, tr);
            }
            obj.add("display", display); return obj.toString();
        }
    }
    static JsonArray vetor(double... valores) { JsonArray a = new JsonArray(); for (double v : valores) a.add(v); return a; }
    private static byte[] texto(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static byte[] png(BufferedImage im) throws IOException { var out = new ByteArrayOutputStream(); ImageIO.write(im, "png", out); return out.toByteArray(); }

    public static String id(ObjetoDecorativo o, Tema t) { return "decor_" + o.id() + "_" + t.name().toLowerCase(java.util.Locale.ROOT); }
    public static int cor(Tema t) { return switch (t) { case CRIPTA -> 0x9D8AB8; case CAVERNA -> 0x49BFA1; case INFERNO -> 0xFF8B35; case GELO -> 0x87DFFF; }; }

    public static Modelo decoracao(ObjetoDecorativo o) {
        Modelo m = new Modelo();
        switch (o) {
            case BANDEIRA -> { m.caixa(7, 0, 7, 2, 28, 2, 1).caixa(7, 23, 7, 11, 1, 1, 2).caixa(8, 7, 7.5, 9, 16, .5, 0).caixa(11, 12, 7, 3, 6, .6, 3); }
            case ESTANTE -> {
                m.caixa(0, 0, 10, 16, 2, 6, 1).caixa(0, 15, 10, 16, 1, 6, 1).caixa(0, 0, 15, 16, 16, 1, 1);
                for (int y : new int[]{2, 9}) { m.caixa(0, y + 6, 10, 16, 1, 6, 2); for (int x = 1; x < 15; x += 3) m.caixa(x, y, 11, 2, 6, 4, x % 4); }
            }
            case MESA -> { m.caixa(-4, 12, -1, 24, 2, 18, 1); for (int x : new int[]{-2, 16}) for (int z : new int[]{1, 13}) m.caixa(x, 0, z, 2, 12, 2, 1); m.caixa(6, 14, 5, 4, 1, 5, 2).caixa(7, 15, 7, 2, 3, 2, 3); }
            case CADEIRA -> { m.caixa(2, 8, 2, 12, 2, 12, 1).caixa(2, 10, 12, 12, 13, 2, 1); for (int x : new int[]{3, 11}) for (int z : new int[]{3, 11}) m.caixa(x, 0, z, 2, 8, 2, 1); }
            case ARMAS -> { m.caixa(1, 0, 6, 14, 2, 4, 1).caixa(2, 2, 7, 2, 20, 2, 1).caixa(12, 2, 7, 2, 20, 2, 1).caixa(2, 17, 7, 12, 2, 2, 1); for (int x : new int[]{5, 9}) m.caixa(x, 5, 6, 1.5, 15, 1, 2).caixa(x - 2, 17, 5.5, 5, 1, 2, 3); }
            case ESCUDO -> m.caixa(2, 4, 12, 12, 14, 2, 1).caixa(4, 2, 12, 8, 2, 2, 1).caixa(7, 4, 11, 2, 14, 1, 3).caixa(2, 11, 11, 12, 2, 1, 2);
            case BRASEIRO -> m.caixa(2, 0, 2, 12, 2, 12, 1).caixa(6, 2, 6, 4, 7, 4, 1).caixa(1, 9, 1, 14, 3, 14, 2).caixa(4, 12, 4, 8, 4, 8, 3).caixa(6, 16, 6, 4, 4, 4, 0);
            case LANTERNA -> m.caixa(3, 0, 3, 10, 2, 10, 1).caixa(4, 2, 4, 8, 10, 8, 3).caixa(3, 12, 3, 10, 2, 10, 2).anel(15, 3, 1);
            case CRISTAL -> { m.caixa(1, 0, 1, 14, 2, 14, 1); for (int n = 0; n < 4; n++) m.caixa(4 + n, 2 + n * 3, 4 + n, 8 - n * 2, 3, 8 - n * 2, n % 2 == 0 ? 0 : 3); m.caixa(1, 2, 10, 3, 7, 3, 0); }
            case ALTAR -> m.caixa(0, 0, 0, 16, 3, 16, 1).caixa(3, 3, 3, 10, 7, 10, 1).caixa(-1, 10, -1, 18, 3, 18, 2).anel(13, 6, 3).caixa(7, 14, 7, 2, 4, 2, 0);
            case CAIXOTES -> m.caixa(0, 0, 1, 10, 10, 10, 1).caixa(7, 0, 8, 9, 8, 8, 0).caixa(2, 10, 2, 8, 7, 8, 1).caixa(-.1, 3, .8, 10.2, 1, .5, 2).caixa(-.1, 7, .8, 10.2, 1, .5, 2);
            case VASO -> m.caixa(4, 0, 4, 8, 2, 8, 1).caixa(2, 2, 2, 12, 7, 12, 0).caixa(4, 9, 4, 8, 2, 8, 2).caixa(7, 11, 7, 2, 8, 2, 1).caixa(2, 15, 5, 6, 2, 4, 3).caixa(8, 18, 6, 6, 2, 4, 3);
            case PLACA -> m.caixa(7, 0, 7, 2, 22, 2, 1).caixa(1, 13, 5, 14, 7, 2, 0).caixa(3, 15, 4.8, 9, .8, .3, 2).caixa(3, 17, 4.8, 6, .8, .3, 2);
            case BAU -> { for (double x : new double[]{-.4, 14.4}) for (double z : new double[]{-.4, 14.4}) m.caixa(x, 0, z, 2, 13, 2, 2); m.caixa(6, 5, -.6, 4, 5, .8, 3); }
            case PORTICO -> m.caixa(-8, 0, 6, 4, 32, 4, 1).caixa(20, 0, 6, 4, 32, 4, 1).caixa(-8, 28, 6, 32, 4, 4, 2).caixa(6, 27, 5, 4, 4, 1, 3);
        }
        return m;
    }

    public static Modelo acessorio(String id) {
        Modelo m = new Modelo();
        switch (id) {
            case "mochila" -> m.caixa(3, 0, 10, 10, 12, 5, 1).caixa(4, 9, 9, 8, 4, 6, 0).caixa(6, 3, 15, 4, 5, 1, 2);
            case "aljava" -> { m.caixa(10, 1, 10, 4, 13, 4, 1).caixa(9, 12, 9, 6, 2, 6, 2); for (int i = 0; i < 3; i++) m.caixa(10.5 + i, 12, 11, .5, 7 + i, .5, 3); }
            case "capa" -> m.caixa(1, -10, 10, 14, 24, 1, 0).caixa(2, -10, 9.8, 1, 24, 1.3, 2).caixa(13, -10, 9.8, 1, 24, 1.3, 2);
            case "colar" -> m.caixa(3, 9, -.8, 10, .7, .7, 2).caixa(7, 6, -1, 2, 3, 1, 3);
            default -> throw new IllegalArgumentException(id);
        }
        return m;
    }

    public static Modelo magia(String tipo) {
        Modelo m = new Modelo();
        switch (tipo) {
            case "buraco_negro" -> {
                for (int y = 2; y <= 12; y += 2) { double largura = 12 - Math.abs(7 - y); m.caixa(8 - largura / 2, y, 8 - largura / 2, largura, 2, largura, 1); }
                m.anel(6.5, 11, 3).anel(7, 14, 0).anel(7.5, 16, 2);
            }
            case "pilar" -> { for (int i = 0; i < 5; i++) { double w = 10 - i * 1.6; m.caixa(8 - w / 2, i * 3, 8 - w / 2, w, 3, w, i % 2 == 0 ? 0 : 3); } }
            case "runa", "invocacao" -> { m.anel(0, 7, 0).anel(.2, 5, 3); for (int i = 0; i < 6; i++) { double a = Math.PI * i / 3; m.caixa(7 + Math.cos(a) * 6, .5, 7 + Math.sin(a) * 6, 2, .6, 2, 2); } }
            default -> m.caixa(4, 4, 4, 8, 8, 8, 0).caixa(6, 2, 6, 4, 12, 4, 3).caixa(2, 6, 6, 12, 4, 4, 2);
        }
        return m;
    }

    private static void registrar(Map<String, byte[]> f, String id, Modelo m, int cor, boolean animado) throws IOException {
        f.put(ROOT + "models/item/beleza_" + id + ".json", texto(m.json(id)));
        f.put(ROOT + "items/beleza_" + id + ".json", texto("{\"model\":{\"type\":\"minecraft:model\",\"model\":\"rpgatributos:item/beleza_" + id + "\",\"tints\":[{\"type\":\"minecraft:custom_model_data\",\"index\":0,\"default\":16777215}]}}"));
        int[] cores = {0xFF000000 | cor, 0xFF272335, 0xFFC9A460, 0xFFDAF7FF};
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, cores[(y / 4) * 4 + x / 4 < 4 ? x / 4 : 1]);
        String caminho = ROOT + "textures/item/beleza_" + id + ".png";
        f.put(caminho, png(im)); if (animado) animar(f, caminho);
    }

    private static void animar(Map<String, byte[]> f, String caminho) throws IOException {
        BufferedImage base = ImageIO.read(new ByteArrayInputStream(f.get(caminho)));
        int w = base.getWidth(), h = base.getHeight();
        BufferedImage frames = new BufferedImage(w, h * 4, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < 4; frame++) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = base.getRGB(x, y); double luz = 0.82 + .18 * Math.sin(frame * Math.PI / 2 + (x + y) * .4);
            int r = (int) (((c >> 16) & 255) * luz), g = (int) (((c >> 8) & 255) * luz), b = (int) ((c & 255) * luz);
            frames.setRGB(x, y + frame * h, (c & 0xFF000000) | r << 16 | g << 8 | b);
        }
        f.put(caminho, png(frames)); f.put(caminho + ".mcmeta", texto("{\"animation\":{\"frametime\":4,\"interpolate\":true,\"width\":" + w + ",\"height\":" + h + "}}"));
    }

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        for (Tema t : Tema.values()) for (ObjetoDecorativo o : ObjetoDecorativo.values()) registrar(f, id(o, t), decoracao(o), cor(t), o == ObjetoDecorativo.CRISTAL || o == ObjetoDecorativo.BRASEIRO || o == ObjetoDecorativo.LANTERNA);
        for (String a : new String[]{"mochila", "aljava", "capa", "colar"}) registrar(f, "corpo_" + a, acessorio(a), 0x785D89, false);
        registrar(f, "buraco_negro", magia("buraco_negro"), 0x9B58DA, true);
        for (Essencia e : Essencia.values()) for (String tipo : new String[]{"orbe", "runa", "pilar", "invocacao"}) registrar(f, tipo + "_" + e.name().toLowerCase(java.util.Locale.ROOT), magia(tipo), e.cor().value(), true);
        return f;
    }

    public static void animarItens(Map<String, byte[]> f) throws IOException {
        for (String c : java.util.List.copyOf(f.keySet())) if (c.endsWith(".png") && c.contains("/textures/item/")
                && ((c.endsWith("_cores.png") && (c.contains("portal_chefe_") || c.contains("portal_cacador_s_espada"))) || c.contains("/elixir_"))) animar(f, c);
    }
}
