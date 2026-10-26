package br.rpgatributos.exploracao;

import br.rpgatributos.arcano.Essencia;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.Color;

/** Uma aparência de invocação por essência, preservando as animações do bicho de origem. */
public final class ModelosInvocacoes {
    private ModelosInvocacoes() { }
    private static final String CEM = "assets/minecraft/optifine/cem/";
    public static String nome(Essencia e) { return e == Essencia.VIDA ? "Espírito da Vida" : "Elemental de " + e.nome(); }
    public static String cem(Essencia e) { return e == Essencia.VIDA ? "allay" : e == Essencia.TERRA ? "iron_golem" : "vex"; }
    private static byte[] ler(String nome) throws IOException {
        try (var in = ModelosInvocacoes.class.getResourceAsStream("/modelos/visual/" + nome)) { if (in == null) throw new IOException(nome); return in.readAllBytes(); }
    }
    public static void somar(Map<String, byte[]> f) throws IOException {
        for (Essencia e : Essencia.values()) {
            String base = e == Essencia.VIDA ? "espirito" : e == Essencia.TERRA ? "golem_de_pedra" : "elemental";
            String id = "rpg_invocacao_" + e.name().toLowerCase(java.util.Locale.ROOT);
            JsonObject modelo = JsonParser.parseString(new String(ler(base + ".jem"), StandardCharsets.UTF_8)).getAsJsonObject();
            modelo.addProperty("texture", id + ".png");
            var partes = modelo.getAsJsonArray("models");
            for (var parte : partes) {
                var p = parte.getAsJsonObject();
                if (!p.get("part").getAsString().equals("body")) continue;
                JsonArray filhos = p.getAsJsonArray("submodels"); if (filhos == null) { filhos = new JsonArray(); p.add("submodels", filhos); }
                int quantidade = e == Essencia.VENTO || e == Essencia.VAZIO ? 8 : 3 + e.ordinal() % 4;
                for (int n = 0; n < quantidade; n++) {
                    double a = Math.PI * 2 * n / quantidade, r = e == Essencia.TERRA ? 4 : 6;
                    JsonObject detalhe = new JsonObject(); detalhe.addProperty("id", "essencia_" + e.name().toLowerCase(java.util.Locale.ROOT) + n);
                    JsonObject caixa = new JsonObject();
                    double altura = e == Essencia.FOGO || e == Essencia.GELO ? 4 : 1.5;
                    caixa.add("coordinates", JsonParser.parseString("[" + Math.cos(a) * r + "," + (-3 + Math.sin(a) * 3) + ",-3,1.5," + altura + ",1.5]"));
                    caixa.add("textureOffset", JsonParser.parseString("[0,0]")); JsonArray caixas = new JsonArray(); caixas.add(caixa); detalhe.add("boxes", caixas); filhos.add(detalhe);
                }
                break;
            }
            var imagem = ImageIO.read(new ByteArrayInputStream(ler(base + ".png")));
            float[] matiz = Color.RGBtoHSB((e.cor().value() >> 16) & 255, (e.cor().value() >> 8) & 255, e.cor().value() & 255, null);
            for (int y = 0; y < imagem.getHeight(); y++) for (int x = 0; x < imagem.getWidth(); x++) {
                int c = imagem.getRGB(x, y); if ((c >>> 24) == 0) continue;
                float[] cor = Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
                int novo = Color.HSBtoRGB(matiz[0], Math.max(.25f, matiz[1] * .7f), cor[2]); imagem.setRGB(x, y, (c & 0xFF000000) | novo & 0xFFFFFF);
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream(); ImageIO.write(imagem, "png", png); f.put(CEM + id + ".png", png.toByteArray());
            var regras = ModelosVisuais.Regras.ler(f.get(CEM + cem(e) + ".properties")); int numero = regras.proximoArquivo();
            f.put(CEM + cem(e) + numero + ".jem", modelo.toString().getBytes(StandardCharsets.UTF_8));
            String nomes = java.util.regex.Pattern.quote(nome(e));
            if (e == Essencia.VIDA) nomes += "|" + java.util.regex.Pattern.quote("Espírito da Floresta");
            if (e == Essencia.TERRA) nomes += "|" + java.util.regex.Pattern.quote("Golem de Pedra");
            if (e == Essencia.SOMBRA) nomes += "|" + java.util.regex.Pattern.quote("Sombra Gêmea");
            regras.regras.addFirst(new String[]{"" + numero, "✦ (" + nomes + ") de .*"});
            f.put(CEM + cem(e) + ".properties", regras.gravar("RPGAtributos - invocações por essência"));
        }
    }
}
