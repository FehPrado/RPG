package br.rpgatributos.apoio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * O HUD do mod (0.2.0), com os dados que o plugin manda a cada meio segundo:
 * embaixo à direita, a classe com as habilidades (recarga) e a mana; no alto à esquerda, o portal
 * (rank, tempo e objetivo); no meio, o mandamento e os olhos da Câmara das Estátuas.
 */
final class Hud {

    private Hud() { }

    static void desenhar(GuiGraphicsExtractor g, DeltaTracker tempo) {
        Minecraft mc = Minecraft.getInstance();
        if (!Estado.temHud() || mc.gui.screen() != null || mc.player == null) return;
        Font f = mc.font;
        JsonObject h = Estado.hud;
        int w = g.guiWidth(), alt = g.guiHeight();
        classe(g, f, h, w, alt);
        portal(g, f, Estado.objeto(h, "portal"), w);
    }

    private static void classe(GuiGraphicsExtractor g, Font f, JsonObject h, int w, int alt) {
        JsonObject c = Estado.objeto(h, "classe");
        JsonArray hab = c != null && c.has("hab") && c.get("hab").isJsonArray() ? c.getAsJsonArray("hab") : new JsonArray();
        int linhas = 2 + hab.size();
        int largura = 128, x = w - largura - 4, y = alt - 14 - linhas * 11;
        g.fill(x - 3, y - 3, x + largura + 1, alt - 10, 0x90000000);
        int cy = y;
        if (c != null) {
            int cor = Desenho.rgb(Estado.inteiro(c, "cor", 0xFFFFFF));
            g.text(f, Estado.texto(c, "simbolo", "") + " " + Estado.texto(c, "nome", "") + " " + Estado.inteiro(c, "nivel", 1), x, cy, cor, true);
            cy += 11;
            int sel = Estado.inteiro(c, "sel", 0), k = 0;
            for (JsonElement el : hab) {
                JsonObject ho = el.getAsJsonObject();
                double falta = Estado.numero(ho, "falta", 0), total = Math.max(1, Estado.numero(ho, "total", 1));
                boolean escolhida = k == sel;
                String nome = (escolhida ? "▶ " : "  ") + Estado.texto(ho, "nome", "?");
                g.text(f, nome, x, cy, escolhida ? 0xFFFFFFFF : 0xFFB0BEC5, false);
                if (falta > 0) {
                    String s = (int) Math.ceil(falta) + "s";
                    g.text(f, s, x + largura - f.width(s) - 2, cy, 0xFFE57373, false);
                    g.fill(x + 10, cy + 9, x + 10 + (int) ((largura - 14) * (1 - falta / total)), cy + 10, 0xFF90A4AE);
                } else {
                    g.text(f, "✔", x + largura - 8, cy, 0xFF81C784, false);
                }
                cy += 11;
                k++;
            }
        } else {
            g.text(f, "Sem classe (/classe)", x, cy, 0xFF90A4AE, false);
            cy += 11;
        }
        double mana = Estado.numero(h, "mana", 0), max = Math.max(1, Estado.numero(h, "manaMax", 1));
        Desenho.barra(g, x, cy + 2, largura - 40, 5, mana / max, 0xFF7E57C2);
        g.text(f, (int) mana + "", x + largura - 36, cy, 0xFFCE93D8, false);
        g.text(f, "◆" + Estado.inteiro(h, "cristais", 0), x + largura - 16, cy, 0xFF26C6DA, false);
    }

    private static void portal(GuiGraphicsExtractor g, Font f, JsonObject p, int w) {
        if (p == null) return;
        int cor = Desenho.rgb(Estado.inteiro(p, "cor", 0xB388FF));
        long falta = (long) Estado.numero(p, "falta", 0);
        String tempo = String.format("⏱ %d:%02d", falta / 60, falta % 60);
        String titulo = "۞ " + Estado.texto(p, "titulo", "Portal");
        String objetivo = Estado.texto(p, "objetivo", "");
        int largura = Math.max(f.width(titulo + "  " + tempo), f.width(objetivo)) + 8;
        g.fill(2, 2, 2 + largura, 26, 0x90000000);
        g.text(f, titulo, 6, 5, cor, true);
        g.text(f, tempo, 6 + f.width(titulo) + 8, 5, falta < 300 ? 0xFFE57373 : 0xFFB0BEC5, false);
        g.text(f, objetivo, 6, 16, 0xFFFFFFFF, false);
        JsonObject c = Estado.objeto(p, "camara");
        if (c == null) return;
        // A Câmara das Estátuas: o mandamento e, no 3º, os olhos do deus.
        int meio = w / 2, y = 40;
        g.centeredText(f, Estado.texto(c, "mandamento", ""), meio, y, 0xFFBCAAA4);
        String olhos = Estado.texto(c, "olhos", "");
        boolean pisca = (System.currentTimeMillis() / 250) % 2 == 0;
        switch (olhos) {
            case "abertos" -> g.centeredText(f, "👁 OS OLHOS ESTÃO ABERTOS: NÃO SE MEXA 👁", meio, y + 12, pisca ? 0xFFFF1744 : 0xFFB71C1C);
            case "aviso" -> g.centeredText(f, "⚠ Os olhos vão abrir!", meio, y + 12, 0xFFFFD54F);
            default -> g.centeredText(f, "Os olhos estão fechados: pode andar.", meio, y + 12, 0xFF81C784);
        }
    }
}
