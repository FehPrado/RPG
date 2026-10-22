package br.rpgatributos.apoio;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/** As teclas do RPGAtributos. O jogador pode trocar qualquer uma em Opções > Controles. */
final class Teclas {

    private Teclas() { }

    static final KeyMapping.Category CATEGORIA = new KeyMapping.Category(Identifier.fromNamespaceAndPath(RpgApoio.MODID, "rpgatributos"));

    /** Tecla → nome da ação que o plugin entende (as que só mandam a ação ao apertar). */
    private static final Map<KeyMapping, String> TECLAS = new LinkedHashMap<>();
    /** A habilidade de classe: tocar usa; segurar abre a roda de habilidades (0.2.0). */
    static final KeyMapping HABILIDADE = new KeyMapping("key." + RpgApoio.MODID + ".habilidade", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORIA);
    /** Segurar mais que isto (ms) abre a roda. */
    private static final long SEGURAR_MS = 250;

    static {
        tecla("golpe_montaria", InputConstants.KEY_R);
        tecla("trocar_habilidade", InputConstants.KEY_B);
        tecla("esquiva", InputConstants.KEY_LALT);
        tecla("postura", InputConstants.KEY_N);
        tecla("menu", InputConstants.KEY_J);
        // 0.2.0: os menus do Sistema e das classes
        tecla("sistema", InputConstants.KEY_K);
        tecla("missao", InputConstants.KEY_U);
        tecla("loja", InputConstants.KEY_Y);
        tecla("classes", InputConstants.KEY_O);
    }

    private static void tecla(String acao, int padrao) {
        TECLAS.put(new KeyMapping("key." + RpgApoio.MODID + "." + acao, InputConstants.Type.KEYBOARD, padrao, CATEGORIA), acao);
    }

    static void registrar(RegisterKeyMappingsEvent e) {
        e.registerCategory(CATEGORIA);
        e.register(HABILIDADE);
        TECLAS.keySet().forEach(e::register);
    }

    private static boolean saudou, avisouSemPlugin;
    private static boolean habilidadeApertada, rodaAberta;
    private static long habilidadeDesde;

    static void aCadaTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener rede = mc.getConnection();
        if (rede == null || mc.player == null) return;
        boolean temPlugin = NetworkRegistry.hasChannel(rede.getConnection(), ConnectionProtocol.PLAY, TeclaPayload.TYPE.id());
        if (temPlugin && !saudou) {
            saudou = true;
            ClientPacketDistributor.sendToServer(new TeclaPayload("ola " + RpgApoio.VERSAO));
        }
        for (Map.Entry<KeyMapping, String> en : TECLAS.entrySet()) {
            while (en.getKey().consumeClick()) enviar(mc, temPlugin, en.getValue());
        }
        habilidade(mc, temPlugin);
    }

    /** V: um toque usa a habilidade escolhida; segurado, abre a roda (se a classe tem mais de uma). */
    private static void habilidade(Minecraft mc, boolean temPlugin) {
        while (HABILIDADE.consumeClick()) { } // o que vale é estar apertada ou não
        boolean apertada = HABILIDADE.isDown();
        long agora = System.currentTimeMillis();
        if (apertada && !habilidadeApertada) {
            habilidadeApertada = true;
            habilidadeDesde = agora;
            rodaAberta = false;
        } else if (apertada && !rodaAberta && agora - habilidadeDesde >= SEGURAR_MS && mc.gui.screen() == null) {
            JsonObject classe = Estado.temHud() ? Estado.objeto(Estado.hud, "classe") : null;
            if (temPlugin && classe != null && classe.has("hab") && classe.getAsJsonArray("hab").size() > 1) {
                rodaAberta = true;
                mc.gui.setScreen(new RodaHabilidades(classe));
            }
        } else if (!apertada && habilidadeApertada) {
            habilidadeApertada = false;
            // Abrir a roda solta a tecla (o jogo faz isso ao abrir uma tela): aí quem decide é a roda.
            if (!rodaAberta) enviar(mc, temPlugin, "habilidade");
            rodaAberta = false;
        }
    }

    private static void enviar(Minecraft mc, boolean temPlugin, String acao) {
        if (temPlugin) {
            ClientPacketDistributor.sendToServer(new TeclaPayload(acao));
        } else if (!avisouSemPlugin) {
            avisouSemPlugin = true;
            mc.player.sendOverlayMessage(Component.literal("Este servidor não tem o RPGAtributos: as teclas do mod de apoio não fazem nada aqui."));
        }
    }

    static void aoSair(ClientPlayerNetworkEvent.LoggingOut e) {
        saudou = false;
        avisouSemPlugin = false;
        habilidadeApertada = false;
        rodaAberta = false;
        Estado.limpar();
    }
}
