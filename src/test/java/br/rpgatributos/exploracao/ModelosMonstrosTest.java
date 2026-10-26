package br.rpgatributos.exploracao;

import br.rpgatributos.domador.ModelosLendarios;
import br.rpgatributos.masmorra.Dificuldade;
import br.rpgatributos.masmorra.Monarca;
import br.rpgatributos.masmorra.Tema;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModelosMonstrosTest {
    private static final String CEM = "assets/minecraft/optifine/cem/";

    @Test
    void cobreTodasAsDificuldadesENomesDosMonarcas() throws Exception {
        var pacote = ModelosLendarios.arquivosDoPacote();
        ModelosVisuais.somar(pacote, null);
        ModelosMonstros.somar(pacote);
        for (Dificuldade dificuldade : Dificuldade.values()) for (var tipo : dificuldade.monstros()) {
            var especie = ModelosMonstros.ESPECIES.stream().filter(e -> e.tipo() == tipo).findFirst().orElseThrow();
            for (Tema tema : Tema.values()) {
                String nome = ModelosMonstros.nome(tipo, tema);
                String numero = escolher(pacote, especie.cem(), nome);
                assertNotNull(numero, nome);
                var modelo = JsonParser.parseString(new String(pacote.get(CEM + especie.cem() + numero + ".jem"), StandardCharsets.UTF_8)).getAsJsonObject();
                assertNotNull(pacote.get(CEM + modelo.get("texture").getAsString()));
                assertTrue(modelo.get("texture").getAsString().contains(tema.name().toLowerCase(java.util.Locale.ROOT)));
            }
        }
        for (Monarca m : Monarca.values()) {
            String cem = switch (m.base) { case CHAMAS -> "blaze"; case LICH -> "skeleton"; case GOLEM -> "iron_golem"; case RAINHA -> "spider"; case ARAUTO -> "wither_skeleton"; default -> throw new AssertionError(); };
            assertNotNull(escolher(pacote, cem, "☠ " + m.chefe + " ☠"));
        }
        // Uma criatura especial continua escolhendo o modelo especial antes dos modelos comuns.
        String lich = escolher(pacote, "skeleton", "☠ Lich ☠");
        String json = new String(pacote.get(CEM + "skeleton" + lich + ".jem"), StandardCharsets.UTF_8);
        assertTrue(json.contains("rpg_lich.png"));
    }

    private static String escolher(Map<String, byte[]> f, String cem, String nome) throws Exception {
        Properties p = new Properties(); p.load(new ByteArrayInputStream(f.get(CEM + cem + ".properties")));
        for (int i = 1; i < 100; i++) {
            String regra = p.getProperty("name." + i);
            if (regra != null && nome.matches(regra.substring("regex:".length()))) return p.getProperty("models." + i);
        }
        return null;
    }

    @Test
    void pacoteCompletoExportaMonstrosEVariantes(@TempDir Path pasta) throws Exception {
        byte[] zip = PacoteRecursos.exportar(pasta.toFile());
        int monstros = 0, itensPortal = 0;
        boolean metadata = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (var e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                if (e.getName().startsWith(CEM + "rpg_mob_") && e.getName().endsWith(".png")) monstros++;
                if (e.getName().startsWith("assets/rpgatributos/items/portal_")) itensPortal++;
                if (e.getName().equals("pack.mcmeta")) metadata = true;
            }
        }
        assertEquals(68, monstros);
        assertEquals(285, itensPortal);
        assertTrue(metadata);
    }
}
