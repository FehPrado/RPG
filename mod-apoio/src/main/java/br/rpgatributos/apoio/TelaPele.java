package br.rpgatributos.apoio;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A "pele" de todos os menus do plugin: o mesmo baú do servidor (os cliques não mudam), mas com
 * fundo do tema, sem os vidros de enfeite e com um quadrinho atrás de cada item.
 */
final class TelaPele extends ContainerScreen {

    private final Desenho.Tema tema;

    TelaPele(ChestMenu menu, Inventory inventory, Component title, String tema) {
        super(menu, inventory, title);
        this.tema = Desenho.tema(tema);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // Escurece o mundo como sempre, mas no lugar da textura de baú vem o painel do tema.
        this.extractTransparentBackground(g);
        int linhas = this.menu.getRowCount();
        Desenho.painel(g, this.leftPos, this.topPos, this.imageWidth, linhas * 18 + 17, tema);
        // O inventário do jogador, mais discreto, embaixo.
        int yInv = this.topPos + linhas * 18 + 17;
        g.fill(this.leftPos, yInv + 2, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xD0101010);
        g.outline(this.leftPos - 1, yInv + 1, this.imageWidth + 2, this.topPos + this.imageHeight - yInv, Desenho.alfa(tema.destaque(), 0x60));
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        ItemStack i = slot.getItem();
        if (Desenho.enfeite(i)) return; // o vidro de enfeite some
        boolean doMenu = slot.index < this.menu.getRowCount() * 9;
        if (doMenu && !i.isEmpty()) Desenho.slot(g, slot.x, slot.y, tema, i.hasFoil());
        else g.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, 0x40000000);
        super.extractSlot(g, slot, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY + 1, tema.destaque(), true);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFA0A0A0, false);
    }
}
