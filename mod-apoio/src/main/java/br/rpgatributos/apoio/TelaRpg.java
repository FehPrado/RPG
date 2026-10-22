package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Base das telas próprias do mod (Sistema, Classes, Loja, Mural). Por baixo continua o baú do
 * servidor: cada "botão" daqui é um espaço do baú, desenhado onde a tela quiser. Clicar num botão é
 * clicar naquele espaço (esquerdo, direito ou com Shift), então o plugin age como sempre.
 */
abstract class TelaRpg extends AbstractContainerScreen<ChestMenu> {

    /** Um espaço do baú desenhado em (x, y) dentro da tela. */
    record Botao(int slot, int x, int y) { }

    protected final Desenho.Tema tema;
    protected final List<Botao> botoes = new ArrayList<>();
    private Botao emCima;

    TelaRpg(ChestMenu menu, Inventory inventory, Component title, String tema, int largura, int altura) {
        super(menu, inventory, title, largura, altura);
        this.tema = Desenho.tema(tema);
    }

    /** Os botões desta tela (chamado ao abrir e quando o tamanho da janela muda). */
    protected abstract void montar();

    /** Linhas, barras e textos atrás dos botões (coordenadas dentro da tela). */
    protected abstract void fundo(GuiGraphicsExtractor g, int mx, int my);

    @Override
    protected void init() {
        super.init();
        botoes.clear();
        montar();
    }

    protected ItemStack item(int slot) {
        return slot >= 0 && slot < this.menu.slots.size() ? this.menu.getSlot(slot).getItem() : ItemStack.EMPTY;
    }

    /** O espaço tem algo de verdade (não é vazio nem vidro de enfeite)? */
    protected boolean tem(int slot) {
        ItemStack i = item(slot);
        return !i.isEmpty() && !Desenho.enfeite(i);
    }

    protected void botao(int slot, int x, int y) {
        botoes.add(new Botao(slot, x, y));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.extractTransparentBackground(g);
        Desenho.painel(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, tema);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        g.pose().pushMatrix();
        g.pose().translate(this.leftPos, this.topPos);
        g.text(this.font, this.title, 8, 5, tema.destaque(), true);
        fundo(g, mx, my);
        emCima = null;
        for (Botao b : botoes) {
            ItemStack i = item(b.slot());
            if (i.isEmpty() || Desenho.enfeite(i)) continue;
            boolean sobre = mx >= b.x() - 1 && mx < b.x() + 17 && my >= b.y() - 1 && my < b.y() + 17;
            if (sobre) emCima = b;
            Desenho.slot(g, b.x(), b.y(), tema, sobre || i.hasFoil());
            g.item(i, b.x(), b.y());
            g.itemDecorations(this.font, i, b.x(), b.y());
        }
        g.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (emCima == null) return;
        ItemStack i = item(emCima.slot());
        if (i.isEmpty()) return;
        g.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(i), i.getTooltipImage(), i, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (emCima == null) return false;
        Slot s = this.menu.getSlot(emCima.slot());
        int botao = event.button() == 1 ? 1 : 0;
        ContainerInput tipo = event.hasShiftDown() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP;
        this.slotClicked(s, s.index, botao, tipo);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true; // nada de arrastar itens nas telas do mod
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return true;
    }

    // ---- ajudantes de texto ----

    /** O nome do item (sem a cor do servidor, na cor dada). */
    protected String nome(int slot) {
        return item(slot).getHoverName().getString();
    }

    /** As linhas da descrição do item (texto puro). */
    protected List<String> descricao(int slot) {
        List<String> l = new ArrayList<>();
        var lore = item(slot).get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) for (Component c : lore.lines()) l.add(c.getString());
        return l;
    }

    /** A primeira linha da descrição que começa com {@code prefixo} (ou ""). */
    protected String linha(int slot, String prefixo) {
        for (String s : descricao(slot)) if (s.startsWith(prefixo)) return s;
        return "";
    }

    /** Texto cortado para caber em {@code largura} pixels. */
    protected String caber(String s, int largura) {
        if (this.font.width(s) <= largura) return s;
        while (!s.isEmpty() && this.font.width(s + "…") > largura) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
