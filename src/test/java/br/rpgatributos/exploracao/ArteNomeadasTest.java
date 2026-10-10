package br.rpgatributos.exploracao;

import br.rpgatributos.forja.Nomeada;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada arma com nome tem desenho de 16x16 e cor para cada letra do desenho. */
class ArteNomeadasTest {

    @Test
    void desenhosCompletos() {
        Set<String> ids = new HashSet<>();
        for (ArteNomeadas.Desenho d : ArteNomeadas.desenhos()) {
            assertTrue(ids.add(d.id()), "desenho repetido: " + d.id());
            assertEquals(16, d.linhas().length, d.id() + ": precisa de 16 linhas");
            for (String l : d.linhas()) {
                assertEquals(16, l.length(), d.id() + ": linha com largura errada: " + l);
                for (char c : l.toCharArray()) {
                    assertTrue(c == '.' || d.cores().containsKey(c), d.id() + ": a letra '" + c + "' não tem cor");
                }
            }
        }
    }

    @Test
    void nomesUnicosEIdsDeVolta() {
        Set<String> nomes = new HashSet<>();
        for (Nomeada n : Nomeada.values()) {
            assertTrue(nomes.add(n.nome()), "nome repetido: " + n.nome());
            assertSame(n, Nomeada.porId(n.id()));
            assertTrue(n.historia().length() > 10, n.nome() + ": sem história");
            Object[] c = n.cores();
            assertEquals(0, c.length % 2, n.nome() + ": paleta sem par");
            for (int i = 0; i < c.length; i += 2) {
                assertTrue(c[i] instanceof Character && c[i + 1] instanceof Integer, n.nome() + ": paleta com tipo errado");
            }
        }
    }
}
