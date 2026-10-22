package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

/** Cores e peças de desenho que todas as telas do mod usam. */
final class Desenho {

    private Desenho() { }

    /** Um tema: a cor de destaque e a do fundo. */
    record Tema(int destaque, int fundo, int fundoClaro) { }

    static Tema tema(String id) {
        return switch (id == null ? "" : id) {
            case "sistema" -> new Tema(0xFF4FC3F7, 0xF00A1622, 0xF0112436);
            case "portal" -> new Tema(0xFFB388FF, 0xF0140C22, 0xF0201435);
            case "classes" -> new Tema(0xFFFFD54F, 0xF01A140A, 0xF0261E10);
            case "arcano" -> new Tema(0xFFCE93D8, 0xF0160C1A, 0xF0221328);
            case "forja" -> new Tema(0xFFFFA726, 0xF01C1008, 0xF02A190C);
            case "reino" -> new Tema(0xFF81C784, 0xF00C160E, 0xF0122016);
            case "natureza" -> new Tema(0xFFAED581, 0xF00F160A, 0xF0172210);
            default -> new Tema(0xFFE0C080, 0xF0141210, 0xF01F1B17);
        };
    }

    /** Uma cor do plugin (RGB) com o alfa cheio. */
    static int rgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Escurece/clareia (fator &lt; 1 escurece). */
    static int vezes(int argb, double f) {
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        r = (int) Math.max(0, Math.min(255, r * f));
        g = (int) Math.max(0, Math.min(255, g * f));
        b = (int) Math.max(0, Math.min(255, b * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int alfa(int argb, int alfa) {
        return (alfa << 24) | (argb & 0xFFFFFF);
    }

    /** O painel de fundo: moldura com a cor do tema, faixa do título e fundo escuro. */
    static void painel(GuiGraphicsExtractor g, int x, int y, int w, int h, Tema t) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, alfa(t.destaque(), 0x60));
        g.fill(x, y, x + w, y + h, t.fundo());
        g.fillGradient(x, y, x + w, y + 16, alfa(t.destaque(), 0x50), alfa(t.destaque(), 0x08));
        g.outline(x - 1, y - 1, w + 2, h + 2, t.destaque());
        g.horizontalLine(x, x + w - 1, y + 16, alfa(t.destaque(), 0x90));
    }

    /** O quadradinho atrás de um item. */
    static void slot(GuiGraphicsExtractor g, int x, int y, Tema t, boolean destaque) {
        g.fill(x - 1, y - 1, x + 17, y + 17, destaque ? alfa(t.destaque(), 0x70) : 0x60000000);
        g.outline(x - 1, y - 1, 18, 18, destaque ? t.destaque() : alfa(t.destaque(), 0x50));
    }

    /** Uma barra de progresso (0 a 1). */
    static void barra(GuiGraphicsExtractor g, int x, int y, int w, int h, double v, int cor) {
        g.fill(x, y, x + w, y + h, 0xA0000000);
        int cheio = (int) Math.round(w * Math.max(0, Math.min(1, v)));
        if (cheio > 0) g.fillGradient(x, y, x + cheio, y + h, vezes(cor, 1.15), vezes(cor, 0.7));
        g.outline(x - 1, y - 1, w + 2, h + 2, 0x50FFFFFF);
    }

    /** Uma linha reta entre dois pontos (horizontal + vertical, como numa árvore). */
    static void ligar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int cor) {
        if (y0 == y1) {
            g.horizontalLine(Math.min(x0, x1), Math.max(x0, x1), y0, cor);
        } else if (x0 == x1) {
            g.verticalLine(x0, Math.min(y0, y1), Math.max(y0, y1), cor);
        } else {
            int meio = (y0 + y1) / 2;
            g.verticalLine(x0, Math.min(y0, meio), Math.max(y0, meio), cor);
            g.horizontalLine(Math.min(x0, x1), Math.max(x0, x1), meio, cor);
            g.verticalLine(x1, Math.min(meio, y1), Math.max(meio, y1), cor);
        }
    }

    /** Vidro de enfeite do plugin (painel colorido com a dica escondida): o mod não desenha. */
    static boolean enfeite(ItemStack i) {
        if (i.isEmpty() || !i.getItem().toString().endsWith("stained_glass_pane")) return false;
        TooltipDisplay d = i.get(DataComponents.TOOLTIP_DISPLAY);
        return d != null && d.hideTooltip();
    }
}
