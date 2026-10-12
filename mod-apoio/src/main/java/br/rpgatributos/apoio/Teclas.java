package br.rpgatributos.apoio;

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

    /** Tecla → nome da ação que o plugin entende. */
    private static final Map<KeyMapping, String> TECLAS = new LinkedHashMap<>();

    static {
        tecla("golpe_montaria", InputConstants.KEY_R);
        tecla("habilidade", InputConstants.KEY_V);
        tecla("trocar_habilidade", InputConstants.KEY_B);
        tecla("esquiva", InputConstants.KEY_LALT);
        tecla("postura", InputConstants.KEY_N);
        tecla("menu", InputConstants.KEY_J);
    }

    private static void tecla(String acao, int padrao) {
        TECLAS.put(new KeyMapping("key." + RpgApoio.MODID + "." + acao, InputConstants.Type.KEYBOARD, padrao, CATEGORIA), acao);
    }

    static void registrar(RegisterKeyMappingsEvent e) {
        e.registerCategory(CATEGORIA);
        TECLAS.keySet().forEach(e::register);
    }

    private static boolean saudou, avisouSemPlugin;

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
            while (en.getKey().consumeClick()) {
                if (temPlugin) {
                    ClientPacketDistributor.sendToServer(new TeclaPayload(en.getValue()));
                } else if (!avisouSemPlugin) {
                    avisouSemPlugin = true;
                    mc.player.sendOverlayMessage(Component.literal("Este servidor não tem o RPGAtributos: as teclas do mod de apoio não fazem nada aqui."));
                }
            }
        }
    }

    static void aoSair(ClientPlayerNetworkEvent.LoggingOut e) {
        saudou = false;
        avisouSemPlugin = false;
    }
}
