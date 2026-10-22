package br.rpgatributos.apoio;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A janela do Sistema (/masmorra arvore) desenhada pelo mod: rank e barra de XP no topo, os 3 ramos
 * da Árvore de Caçador ligados por linhas, as características ao lado e os botões embaixo.
 * Os espaços são os mesmos do menu do plugin (SistemaCacador).
 */
final class TelaSistema extends TelaRpg {

    private static final int[] RAMO = {9, 18, 27};
    private static final int[] CARACTERISTICA = {15, 24, 33};
    private static final String[] NOME_RAMO = {"Lâmina", "Escudo", "Fortuna"};
    private static final int Y0 = 64, PASSO_Y = 34;

    TelaSistema(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "sistema", 300, 214);
    }

    @Override
    protected void montar() {
        botao(4, 8, 22);
        for (int r = 0; r < 3; r++) {
            int y = Y0 + r * PASSO_Y;
            botao(RAMO[r], 10, y);
            for (int k = 1; k <= 4; k++) botao(RAMO[r] + k, 10 + k * 34, y);
            botao(CARACTERISTICA[r], 196, y);
        }
        int yb = 184;
        botao(39, 16, yb);
        botao(41, 56, yb);
        botao(47, 150, yb);
        botao(51, 190, yb);
        botao(49, 270, yb);
    }

    @Override
    protected void fundo(GuiGraphicsExtractor g, int mx, int my) {
        JsonObject s = Estado.sistema;
        int cor = Desenho.rgb(Estado.inteiro(s, "cor", 0x4FC3F7));
        // Topo: rank, barra de XP e pontos.
        g.text(this.font, Estado.texto(s, "rank", "Caçador"), 30, 22, cor, true);
        double prog = Estado.numero(s, "progresso", 0);
        Desenho.barra(g, 30, 33, 160, 6, prog, cor);
        int falta = Estado.inteiro(s, "falta", 0);
        g.text(this.font, Estado.inteiro(s, "xp", 0) + " XP" + (falta > 0 ? " · faltam " + falta : " · rank máximo"), 30, 42, 0xFFB0BEC5, false);
        int livres = Estado.inteiro(s, "livres", 0), pontos = Estado.inteiro(s, "pontos", 0);
        g.text(this.font, "Pontos: " + livres + "/" + pontos, 200, 22, livres > 0 ? 0xFF81C784 : 0xFF90A4AE, true);
        g.text(this.font, "◆ " + Estado.inteiro(s, "mana", 0) + " de mana", 200, 34, 0xFF26C6DA, false);
        // Os ramos: o nome em cima e as linhas entre os nós (acesas até onde há pontos).
        for (int r = 0; r < 3; r++) {
            int y = Y0 + r * PASSO_Y;
            g.text(this.font, NOME_RAMO[r], 10, y - 11, 0xFFCFD8DC, false);
            for (int k = 0; k < 4; k++) {
                int de = 10 + k * 34 + 17, ate = 10 + (k + 1) * 34 - 1;
                ItemStack prox = item(RAMO[r] + k + 1);
                boolean acesa = !prox.isEmpty() && prox.hasFoil();
                g.fill(de, y + 7, ate, y + 9, acesa ? tema.destaque() : 0x50FFFFFF);
            }
        }
        // As características: o nome ao lado do ícone.
        g.text(this.font, "Características", 196, Y0 - 11, 0xFFCFD8DC, false);
        for (int r = 0; r < 3; r++) {
            int slot = CARACTERISTICA[r];
            if (!tem(slot)) continue;
            g.text(this.font, caber(nome(slot), 78), 216, Y0 + r * PASSO_Y + 4, 0xFFFFFFFF, false);
        }
        // Rótulos dos botões.
        String[] rotulos = {"Classe", "Missão", "Refazer", "Re-desp.", "Fechar"};
        int[] xs = {16, 56, 150, 190, 270};
        for (int k = 0; k < rotulos.length; k++) g.centeredText(this.font, rotulos[k], xs[k] + 8, 203, 0xFF90A4AE);
    }
}
