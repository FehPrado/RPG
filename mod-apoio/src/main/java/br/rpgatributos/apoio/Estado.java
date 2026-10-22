package br.rpgatributos.apoio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * O que o mod sabe do plugin: o último HUD, a próxima tela que vai abrir e os números do Sistema.
 * Tudo chega pelo {@link DadosPayload} na thread principal do jogo.
 */
final class Estado {

    private Estado() { }

    /** O último HUD (classe, mana, portal), ou null se o servidor não mandou nenhum. */
    static JsonObject hud;
    /** Quando chegou o último HUD (sem HUD há 3 s: some da tela). */
    static long hudEm;
    /** A tela anunciada pelo plugin para o próximo menu que abrir ("pele", "sistema", "classes", "loja", "mural"). */
    static String tela;
    static JsonObject telaDados = new JsonObject();
    static long telaEm;
    /** Os números da janela do Sistema (barra de XP, pontos). */
    static JsonObject sistema = new JsonObject();

    static void receber(String texto) {
        try {
            int esp = texto.indexOf(' ');
            String tipo = esp < 0 ? texto : texto.substring(0, esp);
            String resto = esp < 0 ? "" : texto.substring(esp + 1);
            switch (tipo) {
                case "hud" -> {
                    hud = JsonParser.parseString(resto).getAsJsonObject();
                    hudEm = System.currentTimeMillis();
                }
                case "tela" -> {
                    int e2 = resto.indexOf(' ');
                    tela = e2 < 0 ? resto : resto.substring(0, e2);
                    telaDados = e2 < 0 ? new JsonObject() : JsonParser.parseString(resto.substring(e2 + 1)).getAsJsonObject();
                    telaEm = System.currentTimeMillis();
                    if ("sistema".equals(tela)) sistema = telaDados;
                }
                case "dados" -> {
                    int e2 = resto.indexOf(' ');
                    if (e2 > 0 && resto.startsWith("sistema")) sistema = JsonParser.parseString(resto.substring(e2 + 1)).getAsJsonObject();
                }
                default -> { } // mensagem de uma versão mais nova do plugin
            }
        } catch (RuntimeException ignorado) {
            // JSON estranho: ignora (o jogo não pode cair por causa disso)
        }
    }

    /** A tela anunciada há pouco (e já "gasta"), ou null. */
    static String pegarTela() {
        String t = tela;
        tela = null;
        return t != null && System.currentTimeMillis() - telaEm < 1500 ? t : null;
    }

    static boolean temHud() {
        return hud != null && System.currentTimeMillis() - hudEm < 3000;
    }

    static void limpar() {
        hud = null;
        tela = null;
        sistema = new JsonObject();
    }

    // ---- ajudantes de leitura ----

    static int inteiro(JsonObject o, String k, int padrao) {
        return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : padrao;
    }

    static double numero(JsonObject o, String k, double padrao) {
        return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : padrao;
    }

    static String texto(JsonObject o, String k, String padrao) {
        return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : padrao;
    }

    static JsonObject objeto(JsonObject o, String k) {
        return o != null && o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : null;
    }
}
