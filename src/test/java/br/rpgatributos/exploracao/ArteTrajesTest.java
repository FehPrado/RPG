package br.rpgatributos.exploracao;

import br.rpgatributos.forja.Traje;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os Trajes: ícones completos, texturas no tamanho das armaduras do jogo e nomes certinhos. */
class ArteTrajesTest {

    @Test
    void iconesTemCorParaTudo() {
        for (Traje t : Traje.values()) {
            Map<Character, Integer> cores = ArteTrajes.paleta(t);
            for (Traje.Peca p : Traje.Peca.values()) {
                String[] m = ArteTrajes.molde(p);
                assertEquals(16, m.length, p + ": 16 linhas");
                for (String l : m) {
                    assertEquals(16, l.length(), p + ": linha com largura errada: " + l);
                    for (char c : l.toCharArray()) assertTrue(c == '.' || cores.containsKey(c), t + " " + p + ": a letra '" + c + "' não tem cor");
                }
            }
        }
    }

    @Test
    void texturasDoCorpo() {
        for (Traje t : Traje.values()) {
            BufferedImage corpo = ArteTrajes.corpo(t), pernas = ArteTrajes.pernas(t);
            assertEquals(64, corpo.getWidth());
            assertEquals(32, corpo.getHeight());
            assertEquals(64, pernas.getWidth());
            assertEquals(32, pernas.getHeight());
            // Elmo (cabeça), peitoral (tronco), braço e botas pintados; o resto do mapa fica vazio.
            assertTrue(cheio(corpo, 8, 8), t + ": elmo vazio");
            assertTrue(cheio(corpo, 22, 24), t + ": peitoral vazio");
            assertTrue(cheio(corpo, 45, 24), t + ": braço vazio");
            assertTrue(cheio(corpo, 5, 29), t + ": botas vazias");
            assertTrue(!cheio(corpo, 5, 21), t + ": as botas não podem subir pela perna toda");
            assertTrue(cheio(pernas, 5, 22), t + ": grevas vazias");
            assertTrue(cheio(pernas, 22, 30), t + ": cinto vazio");
            assertTrue(!cheio(pernas, 22, 22), t + ": o cinto não pode cobrir o peito");
            assertTrue(!cheio(corpo, 60, 4) && !cheio(pernas, 60, 4), t + ": pixel fora do mapa pintado");
        }
    }

    private static boolean cheio(BufferedImage im, int x, int y) {
        return (im.getRGB(x, y) >>> 24) != 0;
    }

    @Test
    void nomesEIds() {
        Set<String> nomes = new HashSet<>();
        for (Traje t : Traje.values()) {
            assertSame(t, Traje.porId(t.id()));
            for (Traje.Peca p : Traje.Peca.values()) {
                assertTrue(nomes.add(t.nome(p)), "nome repetido: " + t.nome(p));
                assertTrue(t.visual(p).matches("[a-z0-9_]+"), "id de textura inválido: " + t.visual(p));
            }
            assertTrue(t.equipamento().matches("[a-z0-9_]+"));
        }
        assertEquals("Elmo da Penumbra", Traje.PENUMBRA.nome(Traje.Peca.CAPACETE));
        assertEquals("Gibão da Colheita", Traje.COLHEITA.nome(Traje.Peca.PEITORAL));
    }
}
