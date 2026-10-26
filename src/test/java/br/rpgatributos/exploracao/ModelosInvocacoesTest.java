package br.rpgatributos.exploracao;

import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.domador.ModelosLendarios;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ModelosInvocacoesTest {
    @Test void cadaEssenciaEscolheModeloProprioAntesDaRegraGenerica() throws Exception {
        var f = ModelosLendarios.arquivosDoPacote(); ModelosVisuais.somar(f, null); ModelosMonstros.somar(f); ModelosInvocacoes.somar(f);
        String raiz = "assets/minecraft/optifine/cem/";
        for (Essencia e : Essencia.values()) {
            Properties p = new Properties(); p.load(new ByteArrayInputStream(f.get(raiz + ModelosInvocacoes.cem(e) + ".properties")));
            String nome = "✦ " + ModelosInvocacoes.nome(e) + " de Felipe", numero = null;
            for (int i = 1; i < 100; i++) {
                String regra = p.getProperty("name." + i);
                if (regra != null && nome.matches(regra.substring(6))) { numero = p.getProperty("models." + i); break; }
            }
            assertNotNull(numero, nome);
            var modelo = JsonParser.parseString(new String(f.get(raiz + ModelosInvocacoes.cem(e) + numero + ".jem"), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("rpg_invocacao_" + e.name().toLowerCase(java.util.Locale.ROOT) + ".png", modelo.get("texture").getAsString());
            assertTrue(f.containsKey(raiz + modelo.get("texture").getAsString()));
        }
    }
}
