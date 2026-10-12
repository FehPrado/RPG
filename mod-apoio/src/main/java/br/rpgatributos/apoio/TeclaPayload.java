package br.rpgatributos.apoio;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** O nome da tecla apertada, no canal "rpgatributos:tecla" que o plugin escuta. */
public record TeclaPayload(String acao) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeclaPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("rpgatributos", "tecla"));
    public static final StreamCodec<ByteBuf, TeclaPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(TeclaPayload::new, TeclaPayload::acao);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
