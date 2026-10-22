package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Items;

/**
 * A árvore de classes (/classe e o Santuário) desenhada pelo mod: as básicas numa linha, as
 * avançadas embaixo da coluna delas, ligadas por linhas, e a página das lendárias com o Soberano
 * no meio dos Monarcas. Os espaços são os do menu do plugin (Santuarios).
 */
final class TelaClasses extends TelaRpg {

    private static final int X0 = 10, PASSO_X = 28, Y0 = 40, PASSO_Y = 34;

    TelaClasses(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "classes", 270, 222);
    }

    /** Página das lendárias? (o plugin põe "Voltar" no espaço 45 e o cabeçalho dos Monarcas no 22) */
    private boolean lendarias() {
        return item(45).is(Items.ARROW);
    }

    private static int x(int slot) { return X0 + (slot % 9) * PASSO_X; }
    private static int y(int slot) { return Y0 + (slot / 9 - 1) * PASSO_Y; }

    @Override
    protected void montar() {
        // Todos os espaços das linhas 1 a 4; os vazios ou de enfeite não aparecem.
        for (int s = 9; s < 45; s++) botao(s, x(s), y(s));
        botao(4, 226, 4);
        botao(45, 10, 196);
        botao(49, 120, 196);
        botao(53, 236, 196);
    }

    @Override
    protected void fundo(GuiGraphicsExtractor g, int mx, int my) {
        int linha = Desenho.alfa(tema.destaque(), 0x70);
        if (!lendarias()) {
            g.text(this.font, "Básicas", 6, Y0 - 11, 0xFFCFD8DC, false);
            g.text(this.font, "Avançadas", 6, Y0 + PASSO_Y - 11, 0xFFCFD8DC, false);
            // Cada coluna: a básica em cima e as avançadas dela embaixo, ligadas.
            for (int col = 0; col < 9; col++) {
                int anterior = -1;
                for (int row = 1; row <= 4; row++) {
                    int s = row * 9 + col;
                    if (s == 26 || !tem(s)) continue;
                    if (anterior >= 0) Desenho.ligar(g, x(s) + 8, y(anterior) + 17, x(s) + 8, y(s) - 1, linha);
                    anterior = s;
                }
            }
        } else {
            g.text(this.font, "Lendárias (um dono só)", 6, Y0 - 11, 0xFFCFD8DC, false);
            g.centeredText(this.font, "♛ Monarcas e o Soberano", x(31) + 8, Y0 + 2 * PASSO_Y - 11, 0xFFB388FF);
            // O Soberano no meio, ligado aos quatro Monarcas.
            if (tem(31)) {
                for (int s : new int[]{29, 30, 32, 33}) if (tem(s)) Desenho.ligar(g, x(31) + 8, y(31) + 8, x(s) + 8, y(s) + 8, linha);
            }
        }
    }
}
