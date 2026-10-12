package br.rpgatributos.apoio;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Mod de apoio do RPGAtributos: roda só no jogo do jogador (o servidor continua só com o plugin).
 * Registra teclas próprias em Opções > Controles e avisa o plugin quando elas são apertadas.
 */
@Mod(value = RpgApoio.MODID, dist = Dist.CLIENT)
public final class RpgApoio {
    public static final String MODID = "rpgapoio";
    public static final String VERSAO = "0.1.0";

    public RpgApoio(IEventBus modBus) {
        modBus.addListener(Teclas::registrar);
        modBus.addListener(RpgApoio::rede);
        NeoForge.EVENT_BUS.addListener(Teclas::aCadaTick);
        NeoForge.EVENT_BUS.addListener(Teclas::aoSair);
    }

    /** O canal é opcional: o jogo entra em qualquer servidor, com ou sem o plugin. */
    private static void rede(RegisterPayloadHandlersEvent e) {
        e.registrar("1").optional().playToServer(TeclaPayload.TYPE, TeclaPayload.CODEC, (payload, ctx) -> { });
    }
}
