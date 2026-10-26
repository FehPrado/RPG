package br.rpgatributos.masmorra;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConteudoSalasTest {
    @Test void sequenciasSaoPermutacoesResetamEConcluemUmaVez() {
        Set<List<Integer>> variantes = new HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            var s = new ConteudoSalas.Sequencia(new Random(seed)); variantes.add(s.ordem());
            assertEquals(Set.of(0,1,2),new HashSet<>(s.ordem()));
            assertTrue(s.tocar(s.ordem().get(0))); assertEquals(1,s.progresso());
            assertFalse(s.tocar(s.ordem().get(0))); assertEquals(0,s.progresso());
            assertFalse(s.tocar(-1)); assertFalse(s.tocar(3)); assertEquals(0,s.progresso());
            for (int runa : s.ordem()) assertTrue(s.tocar(runa));
            assertTrue(s.concluida()); assertEquals(3,s.progresso());
            assertFalse(s.tocar(s.ordem().get(0))); assertEquals(3,s.progresso());
        }
        assertEquals(6,variantes.size());
    }
    @Test void encontrosRespeitamLimitesEDiferenciamOsGrupos() {
        for (var e : ConteudoSalas.Encontro.values()) for (int base=1;base<=30;base++) {
            assertTrue(e.quantidade(base)>=1 && e.quantidade(base)<=14);
            assertTrue(e.vida(0)>0 && e.dano(0)>0);
        }
        assertEquals(9,ConteudoSalas.Encontro.HORDA.quantidade(6));
        assertEquals(3,ConteudoSalas.Encontro.SENTINELA.quantidade(6));
        assertTrue(ConteudoSalas.Encontro.SENTINELA.vida(0)>ConteudoSalas.Encontro.SENTINELA.vida(1));
    }
    @Test void todasAsSalasETemasTemIdentidade() {
        for (TipoSala tipo : TipoSala.values()) for (Tema tema : Tema.values()) {
            Sala s = new Sala(0,0,tipo,0);
            for (Gerador.Estilo estilo : Gerador.Estilo.values()) {
                s.estilo=estilo; assertFalse(ConteudoSalas.nome(tema,s).isBlank());
            }
        }
    }
}
