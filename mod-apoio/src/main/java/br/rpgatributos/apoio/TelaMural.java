package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/**
 * O Mural das Relíquias desenhado pelo mod: uma linha por Monarca, com as 5 relíquias, a barra
 * de quantas já estão no mural e o botão do portal. Os espaços são os do menu do plugin
 * (MuralReliquias).
 */
final class TelaMural extends TelaRpg {

    private static final int Y0 = 28, PASSO_Y = 30;

    TelaMural(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "portal", 260, 212);
    }

    @Override
    protected void montar() {
        for (int r = 0; r < 5; r++) {
            int y = Y0 + r * PASSO_Y;
            botao(r * 9, 8, y);
            for (int i = 1; i <= 5; i++) botao(r * 9 + i, 36 + (i - 1) * 22, y);
            botao(r * 9 + 7, 232, y);
        }
        botao(45, 8, 186);
        botao(47, 120, 186);
        botao(49, 232, 186);
    }

    /** A relíquia está no mural? (o plugin escreve "✔ No mural" nela) */
    private boolean posta(int slot) {
        return !linha(slot, "✔ No mural").isEmpty();
    }

    @Override
    protected void fundo(GuiGraphicsExtractor g, int mx, int my) {
        for (int r = 0; r < 5; r++) {
            int y = Y0 + r * PASSO_Y;
            int n = 0;
            for (int i = 1; i <= 5; i++) if (posta(r * 9 + i)) n++;
            // A linha que liga as relíquias acende conforme elas vão sendo postas.
            g.fill(36 + 8, y + 22, 36 + 4 * 22 + 8, y + 23, 0x40FFFFFF);
            if (n > 0) g.fill(36 + 8, y + 22, 36 + 8 + (int) Math.round(4 * 22 * n / 5.0), y + 23, tema.destaque());
            g.text(this.font, n + "/5", 152, y + 4, n == 5 ? 0xFF81C784 : 0xFFB0BEC5, false);
            if (tem(r * 9 + 7) && item(r * 9 + 7).hasFoil()) g.text(this.font, "Pronto!", 180, y + 4, 0xFFFFD54F, true);
        }
        g.text(this.font, "Colocar todas", 140, 190, 0xFF90A4AE, false);
    }
}
