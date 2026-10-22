package br.rpgatributos.apoio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Mod de apoio do RPGAtributos: roda só no jogo do jogador (o servidor continua só com o plugin).
 * Registra teclas próprias em Opções > Controles e avisa o plugin quando elas são apertadas.
 * Desde a 0.2.0 também recebe dados do plugin: o HUD, a roda de habilidades e as telas dos menus.
 */
@Mod(value = RpgApoio.MODID, dist = Dist.CLIENT)
public final class RpgApoio {
    public static final String MODID = "rpgapoio";
    public static final String VERSAO = "0.2.0";

    public RpgApoio(IEventBus modBus) {
        modBus.addListener(Teclas::registrar);
        modBus.addListener(RpgApoio::rede);
        modBus.addListener(RpgApoio::camadas);
        NeoForge.EVENT_BUS.addListener(Teclas::aCadaTick);
        NeoForge.EVENT_BUS.addListener(Teclas::aoSair);
        NeoForge.EVENT_BUS.addListener(RpgApoio::aoAbrirTela);
    }

    /** Os canais são opcionais: o jogo entra em qualquer servidor, com ou sem o plugin. */
    private static void rede(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1").optional();
        r.playToServer(TeclaPayload.TYPE, TeclaPayload.CODEC, (payload, ctx) -> { });
        r.playToClient(DadosPayload.TYPE, DadosPayload.CODEC, (payload, ctx) -> Estado.receber(payload.texto()));
    }

    private static void camadas(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.HOTBAR, Identifier.fromNamespaceAndPath(MODID, "hud"), Hud::desenhar);
    }

    /**
     * Um menu do plugin está abrindo (o plugin avisou logo antes): no lugar do baú comum entra a
     * tela do mod. Os 4 principais têm tela própria; os outros ganham a "pele".
     */
    private static void aoAbrirTela(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof ContainerScreen cs) || e.getNewScreen() instanceof TelaPele) return;
        String tela = Estado.pegarTela();
        Minecraft mc = Minecraft.getInstance();
        if (tela == null || mc.player == null) return;
        ChestMenu menu = cs.getMenu();
        Inventory inv = mc.player.getInventory();
        Component titulo = cs.getTitle();
        int linhas = menu.getRowCount();
        Screen nova = switch (tela) {
            case "sistema" -> linhas == 6 ? new TelaSistema(menu, inv, titulo) : null;
            case "classes" -> linhas == 6 ? new TelaClasses(menu, inv, titulo) : null;
            case "loja" -> linhas == 3 ? new TelaLoja(menu, inv, titulo) : null;
            case "mural" -> linhas == 6 ? new TelaMural(menu, inv, titulo) : null;
            default -> null;
        };
        if (nova == null) nova = new TelaPele(menu, inv, titulo, Estado.texto(Estado.telaDados, "tema", "rpg"));
        e.setNewScreen(nova);
    }
}
