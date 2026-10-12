package br.rpgatributos.apoio;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CanalApoioTest {

    /** Do jeito que o mod manda: tamanho em VarInt e o texto em UTF-8. */
    private static byte[] mensagem(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int n = b.length;
        while ((n & ~0x7F) != 0) {
            out.write((n & 0x7F) | 0x80);
            n >>>= 7;
        }
        out.write(n);
        out.writeBytes(b);
        return out.toByteArray();
    }

    @Test
    void leAsTeclasQueOModManda() {
        assertEquals("golpe_montaria", CanalApoio.ler(mensagem("golpe_montaria")));
        assertEquals("ola 0.1.0", CanalApoio.ler(mensagem("ola 0.1.0")));
        assertEquals("ação", CanalApoio.ler(mensagem("ação")));
        String longo = "x".repeat(200); // tamanho com dois bytes de VarInt
        assertEquals(longo, CanalApoio.ler(mensagem(longo)));
    }

    @Test
    void ignoraMensagemQuebrada() {
        assertNull(CanalApoio.ler(new byte[]{10, 'a', 'b'})); // diz 10 bytes, só tem 2
        assertNull(CanalApoio.ler(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 1}));
    }
}
