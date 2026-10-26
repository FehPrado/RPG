package br.rpgatributos.exploracao;

import br.rpgatributos.raca.Enfeites;
import br.rpgatributos.raca.Enfeites.Enfeite;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class EnfeitesTest {
    @Test
    void barbaEChifreSaoIndependentesECombinamComOutrasCategorias() {
        var lista = List.of(Enfeite.BARBA_ANA, Enfeite.CHIFRES_DRAGAO, Enfeite.ORELHAS_ELFICAS,
                Enfeite.OCULOS_INVENTOR, Enfeite.AUREOLA_ESPECTRAL, Enfeite.BARBATANAS);
        assertEquals(6, lista.stream().map(Enfeite::categoria).distinct().count());
        var flags = Enfeites.flags(lista);
        assertEquals(Enfeite.values().length, flags.size());
        assertEquals(6, flags.stream().filter(Boolean::booleanValue).count());
        assertTrue(flags.get(Enfeite.BARBA_ANA.ordinal()));
        assertTrue(flags.get(Enfeite.CHIFRES_DRAGAO.ordinal()));
        assertFalse(flags.get(Enfeite.BARBA_TRANCADA.ordinal()));
        assertTrue(Enfeites.flags(List.of()).stream().noneMatch(Boolean::booleanValue));
        assertEquals(1, Enfeite.BARBA_ANA.ordinal()); // marcas de itens existentes continuam estáveis
        assertEquals(2, Enfeite.CHIFRES_DRAGAO.ordinal());
    }

    @Test
    void compostoReferenciaTodosOsModelosETexturasSemDuplicarFlags() {
        var arquivos = Enfeites.arquivosDoPacote();
        var texturas = Enfeites.texturas();
        var json = JsonParser.parseString(new String(arquivos.get("assets/rpgatributos/items/enfeites_combinados.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        var modelos = json.getAsJsonObject("model").getAsJsonArray("models");
        assertEquals(Enfeite.values().length, modelos.size());
        var indices = new HashSet<Integer>();
        for (var elemento : modelos) {
            var condicao = elemento.getAsJsonObject();
            assertTrue(indices.add(condicao.get("index").getAsInt()));
            String modelo = condicao.getAsJsonObject("on_true").get("model").getAsString().replace("rpgatributos:", "assets/rpgatributos/models/") + ".json";
            assertTrue(arquivos.containsKey(modelo), modelo);
            var obj = JsonParser.parseString(new String(arquivos.get(modelo), StandardCharsets.UTF_8)).getAsJsonObject();
            assertFalse(obj.getAsJsonArray("elements").isEmpty());
            String textura = obj.getAsJsonObject("textures").get("t").getAsString().replace("rpgatributos:", "");
            assertTrue(texturas.containsKey(textura), textura);
        }
        for (Enfeite e : Enfeite.values()) assertSame(e, Enfeites.porId(e.id));
    }
}
