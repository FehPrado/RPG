package br.rpgatributos.domador;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelos 3D dos cinco Guardiões para quem joga com os mods EMF + ETF (ou OptiFine). Quem não tem os
 * mods continua vendo a pele normal (o jogo ignora a pasta optifine/cem). Cada modelo vale só para a
 * criatura com o nome da lendária (regras por nome nos .properties) e esconde as camadas de pele,
 * sela e coleira para elas não cobrirem o modelo. Os arquivos ficam em resources/modelos/guardioes
 * (feitos no Blockbench; os .bbmodel de origem ficam fora do jar).
 *
 * <ul>
 *   <li>Qilin, Fenghuang e Longma: cavalo (horse2, horse3 e horse4);</li>
 *   <li>Baihu: lobo (wolf2);</li>
 *   <li>Xuanwu: tartaruga (turtle2).</li>
 * </ul>
 */
public final class ModelosLendarios {

    private ModelosLendarios() { }

    private static final String PASTA = "/modelos/guardioes/";

    private static final List<String> ARQUIVOS = List.of(
            "horse.properties", "horse2.jem", "horse3.jem", "horse4.jem",
            "horse_armor.properties", "horse_armor2.jem", "horse_armor3.jem", "horse_armor4.jem",
            "horse_saddle.properties", "horse_saddle2.jem", "horse_saddle3.jem", "horse_saddle4.jem",
            "wolf.properties", "wolf2.jem", "wolf_armor.properties", "wolf_armor2.jem",
            "wolf_collar.properties", "wolf_collar2.jem",
            "turtle.properties", "turtle2.jem",
            "qilin.png", "fenghuang.png", "longma.png", "baihu.png", "xuanwu.png");

    /**
     * O jogo senta o jogador numa altura fixa da criatura (lobo 13,1 px, tartaruga 8,9 px, vezes o
     * tamanho dela). Os modelos do Baihu e do Xuanwu têm as costas mais altas (19 e 17 px), então o
     * jogador ficava dentro deles: o modelo inteiro encolhe a partir do chão até as costas chegarem
     * no assento. Fica também mais perto da caixa de colisão da criatura.
     */
    private static final Map<String, Double> ENCOLHER = Map.of(
            "wolf2.jem", 13.1 / 19.0,
            "turtle2.jem", 8.9 / 17.0);

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        for (String nome : ARQUIVOS) {
            try (InputStream in = ModelosLendarios.class.getResourceAsStream(PASTA + nome)) {
                if (in == null) throw new IOException("Modelo dos Guardiões não encontrado no jar: " + nome);
                byte[] dados = in.readAllBytes();
                Double k = ENCOLHER.get(nome);
                if (k != null) dados = encolher(dados, k);
                f.put("assets/minecraft/optifine/cem/" + nome, dados);
            }
        }
        return f;
    }

    /** Multiplica por k as posições e caixas de tudo o que está dentro de "g_root" (o ponto no chão). */
    static byte[] encolher(byte[] jem, double k) throws IOException {
        JsonObject raiz = JsonParser.parseString(new String(jem, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject g = acharRaiz(raiz.getAsJsonArray("models"));
        if (g == null) throw new IOException("Modelo sem a parte g_root para encolher");
        for (JsonElement s : filhos(g)) escalar(s.getAsJsonObject(), k);
        for (JsonElement b : caixas(g)) escalarCaixa(b.getAsJsonObject(), k);
        return new GsonBuilder().setPrettyPrinting().create().toJson(raiz).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject acharRaiz(JsonArray partes) {
        if (partes == null) return null;
        for (JsonElement e : partes) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("id") && "g_root".equals(o.get("id").getAsString())) return o;
            JsonObject r = acharRaiz(o.getAsJsonArray("submodels"));
            if (r != null) return r;
        }
        return null;
    }

    private static void escalar(JsonObject parte, double k) {
        multiplicar(parte.getAsJsonArray("translate"), k, 3);
        for (JsonElement b : caixas(parte)) escalarCaixa(b.getAsJsonObject(), k);
        for (JsonElement s : filhos(parte)) escalar(s.getAsJsonObject(), k);
    }

    private static void escalarCaixa(JsonObject caixa, double k) {
        multiplicar(caixa.getAsJsonArray("coordinates"), k, 6);
        if (caixa.has("sizeAdd")) caixa.addProperty("sizeAdd", caixa.get("sizeAdd").getAsDouble() * k);
    }

    private static void multiplicar(JsonArray a, double k, int n) {
        if (a == null) return;
        for (int i = 0; i < Math.min(n, a.size()); i++) a.set(i, new JsonPrimitive(Math.round(a.get(i).getAsDouble() * k * 1000) / 1000.0));
    }

    private static JsonArray filhos(JsonObject o) {
        return o.has("submodels") ? o.getAsJsonArray("submodels") : new JsonArray();
    }

    private static JsonArray caixas(JsonObject o) {
        return o.has("boxes") ? o.getAsJsonArray("boxes") : new JsonArray();
    }
}
