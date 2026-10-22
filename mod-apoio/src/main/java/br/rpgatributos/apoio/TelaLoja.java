package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Loja do Sistema desenhada pelo mod: as ofertas em cartões (ícone, nome e preço, verde se dá
 * para comprar) e a sua mana no topo. Os espaços são os do menu do plugin (LojaSistema).
 */
final class TelaLoja extends TelaRpg {

    private static final int[] OFERTAS = {10, 11, 12, 13, 14, 15, 16};
    private static final Pattern NUMERO = Pattern.compile("(\\d+)");

    TelaLoja(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "sistema", 260, 170);
    }

    private static int cx(int k) { return 8 + (k % 2) * 124; }
    private static int cy(int k) { return 40 + (k / 2) * 30; }

    @Override
    protected void montar() {
        botao(22, 236, 4);
        for (int k = 0; k < OFERTAS.length; k++) botao(OFERTAS[k], cx(k) + 4, cy(k) + 4);
    }

    private static int numero(String s) {
        Matcher m = NUMERO.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    @Override
    protected void fundo(GuiGraphicsExtractor g, int mx, int my) {
        int mana = numero(nome(22));
        g.text(this.font, "◆ " + Math.max(0, mana) + " de mana", 150, 9, 0xFF26C6DA, true);
        g.text(this.font, "Clique para comprar. Paga com Cristais de Mana.", 8, 24, 0xFF90A4AE, false);
        for (int k = 0; k < OFERTAS.length; k++) {
            int s = OFERTAS[k];
            if (!tem(s)) continue;
            int x = cx(k), y = cy(k);
            g.fill(x, y, x + 118, y + 25, 0x50000000);
            g.outline(x, y, 118, 25, Desenho.alfa(tema.destaque(), 0x60));
            g.text(this.font, caber(nome(s), 90), x + 26, y + 4, 0xFFFFFFFF, false);
            int preco = numero(linha(s, "Preço"));
            boolean pode = preco >= 0 && mana >= preco;
            g.text(this.font, preco < 0 ? "" : preco + " ◆", x + 26, y + 15, pode ? 0xFF81C784 : 0xFFE57373, false);
        }
    }
}
