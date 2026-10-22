package br.rpgatributos.apoio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A roda de habilidades: segure a tecla de habilidade (V), aponte o mouse para uma e solte (ou
 * clique). Sem mexer o mouse, soltar usa a que já estava escolhida.
 */
final class RodaHabilidades extends Screen {

    private final JsonArray hab;
    private final int selecionada;
    private int apontada = -1;
    private boolean feito;

    RodaHabilidades(JsonObject classe) {
        super(Component.literal("Habilidades"));
        this.hab = classe.getAsJsonArray("hab");
        this.selecionada = Estado.inteiro(classe, "sel", 0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // Sem escurecer o mundo: a roda fica por cima da luta.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int cx = this.width / 2, cy = this.height / 2, n = hab.size(), raio = 62;
        double dx = mouseX - cx, dy = mouseY - cy;
        apontada = -1;
        if (n > 0 && dx * dx + dy * dy > 18 * 18) {
            double ang = Math.atan2(dy, dx) + Math.PI / 2; // 0 = em cima
            if (ang < 0) ang += Math.PI * 2;
            apontada = (int) Math.round(ang / (Math.PI * 2 / n)) % n;
        }
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xC0FFFFFF);
        g.centeredText(this.font, "Solte para usar", cx, cy + 10, 0xFFB0BEC5);
        for (int i = 0; i < n; i++) {
            JsonObject h = hab.get(i).getAsJsonObject();
            double ang = Math.PI * 2 * i / n - Math.PI / 2;
            int x = cx + (int) Math.round(Math.cos(ang) * raio), y = cy + (int) Math.round(Math.sin(ang) * raio);
            String nome = Estado.texto(h, "nome", "?");
            double falta = Estado.numero(h, "falta", 0);
            String linha2 = falta > 0 ? (int) Math.ceil(falta) + "s" : "pronta";
            int largura = Math.max(this.font.width(nome), this.font.width(linha2)) + 10;
            boolean marcada = i == apontada || (apontada < 0 && i == selecionada);
            g.fill(x - largura / 2, y - 12, x + largura / 2, y + 12, marcada ? 0xD02A3F55 : 0xB0101418);
            g.outline(x - largura / 2, y - 12, largura, 24, marcada ? 0xFF4FC3F7 : 0x80FFFFFF);
            g.centeredText(this.font, nome, x, y - 8, marcada ? 0xFFFFFFFF : 0xFFCFD8DC);
            g.centeredText(this.font, linha2, x, y + 2, falta > 0 ? 0xFFE57373 : 0xFF81C784);
        }
    }

    private void escolher() {
        if (feito) return;
        feito = true;
        ClientPacketDistributor.sendToServer(new TeclaPayload(apontada >= 0 ? "habilidade:" + apontada : "habilidade"));
        this.onClose();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (Teclas.HABILIDADE.matches(event)) {
            escolher();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        escolher();
        return true;
    }
}
