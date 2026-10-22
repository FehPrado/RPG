package br.rpgatributos.apoio;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * O que o plugin manda para o mod (0.2.0), no canal "rpgatributos:dados": uma linha de texto que
 * começa com o tipo ("hud", "tela", "dados") seguida de JSON.
 */
public record DadosPayload(String texto) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DadosPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("rpgatributos", "dados"));
    public static final StreamCodec<ByteBuf, DadosPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(DadosPayload::new, DadosPayload::texto);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
