package br.rpgatributos.exploracao;

import br.rpgatributos.domador.ModelosLendarios;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ModelosVisuaisTest {
    private static final String CEM = "assets/minecraft/optifine/cem/";

    @Test
    void todosOsAlvosTemModeloTexturaETamanhoCoerentes() throws Exception {
        assertEquals(92, ModelosVisuais.alvos().size());
        assertEquals(92, ModelosVisuais.prontos(null).size());
        for (var alvo : ModelosVisuais.alvos()) {
            try (var jem = getClass().getResourceAsStream("/modelos/visual/" + alvo.id() + ".jem");
                 var png = getClass().getResourceAsStream("/modelos/visual/" + alvo.id() + ".png")) {
                assertNotNull(jem, alvo.id());
                assertNotNull(png, alvo.id());
                var modelo = JsonParser.parseString(new String(jem.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                var textura = ImageIO.read(png);
                assertNotNull(textura, alvo.id());
                assertEquals(modelo.getAsJsonArray("textureSize").get(0).getAsInt(), textura.getWidth());
                assertEquals(modelo.getAsJsonArray("textureSize").get(1).getAsInt(), textura.getHeight());
                assertEquals(alvo.id() + ".png", modelo.get("texture").getAsString());
                assertFalse(modelo.getAsJsonArray("models").isEmpty());
            }
        }
    }

    @Test
    void pacoteMantemGuardioesEAdicionaRegrasSemColisoes() throws Exception {
        var pacote = ModelosLendarios.arquivosDoPacote();
        var guardiao = pacote.get(CEM + "horse2.jem").clone();
        ModelosVisuais.somar(pacote, null);
        assertArrayEquals(guardiao, pacote.get(CEM + "horse2.jem"));
        for (var alvo : ModelosVisuais.alvos()) {
            Properties regras = new Properties();
            regras.load(new ByteArrayInputStream(pacote.get(CEM + alvo.cem() + ".properties")));
            var numeros = new HashSet<String>();
            String numero = null;
            for (int i = 1; regras.containsKey("models." + i); i++) {
                String n = regras.getProperty("models." + i);
                assertTrue(numeros.add(n), "Número de variante repetido: " + alvo.cem());
                if (("regex:" + alvo.nome()).equals(regras.getProperty("name." + i))) numero = n;
            }
            assertNotNull(numero, "Regra ausente: " + alvo.id());
            var bytes = pacote.get(CEM + alvo.cem() + numero + ".jem");
            assertNotNull(bytes, alvo.id());
            var modelo = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("rpg_" + alvo.id() + ".png", modelo.get("texture").getAsString());
            assertNotNull(pacote.get(CEM + modelo.get("texture").getAsString()));
        }
        // A roupa desenhada substitui as camadas ETF do aldeão.
        assertNotNull(pacote.get("assets/minecraft/optifine/random/entity/villager/type/plains.properties"));
    }

    @Test
    void novosNomesEscolhemUmaUnicaVariante() {
        String[][] casos = {
            {"ravager", "Nian 年", "nian"}, {"wolf", "🐺 Lobisomem", "lobisomem"},
            {"zombie", "🦇 Vampiro", "vampiro"},
            {"wither_skeleton", "☾ Coração da Penumbra", "coracao_da_penumbra"},
            {"breeze", "Guardião dos Ventos", "guardiao_dos_ventos"},
            {"drowned", "Sumo-Sacerdote Thalassor", "sumo_sacerdote"},
            {"iron_golem", "✦ Guardião da Forja Antiga ✦", "guardiao_da_forja_antiga"},
            {"evoker", "✦ Bibliotecário Amaldiçoado ✦", "bibliotecario_amaldicoado"},
            {"ravager", "✦ Fera Ancestral ✦", "fera_ancestral"},
            {"wither_skeleton", "✦ Rei Caído ✦", "rei_caido"},
            {"vindicator", "✦ Sombra Sem Nome ✦", "sombra_sem_nome"},
            {"stray", "✦ Arqueiro Espectral ✦", "arqueiro_espectral"},
            {"zombie", "✦ Paladino Corrompido ✦", "paladino_corrompido"},
            // Os mestres do mosteiro e das outras vilas não podem cair na mesma regra.
            {"villager", "Mestre Mestre Wen", "mestre"}, {"villager", "Mestre Mestra Mei", "mestre"},
            {"villager", "Mestre do Bastão Tião", "mestre_do_bastao"},
            {"villager", "Mestre Mineiro Thorin", "mestre_mineiro"},
            {"villager", "Mestre de Guerra Brom", "mestre_de_guerra"},
            // A Menina do Cais é aldeã bebê: modelo do villager_baby.
            {"villager_baby", "Menina do Cais Lia", "menina_do_cais"}
        };
        for (String[] caso : casos) {
            var encontrados = ModelosVisuais.alvos().stream()
                    .filter(a -> a.cem().equals(caso[0]) && caso[1].matches(a.nome())).toList();
            assertEquals(1, encontrados.size(), caso[1]);
            assertEquals(caso[2], encontrados.getFirst().id());
        }
        var vampiro = ModelosVisuais.alvos().stream().filter(a -> a.id().equals("vampiro")).findFirst().orElseThrow();
        assertFalse("★ Elite Vampiro".matches(vampiro.nome()));
    }

    @Test
    void camadasVanillaNaoCobremOsNovosTrajes() throws Exception {
        var pacote = ModelosLendarios.arquivosDoPacote();
        ModelosVisuais.somar(pacote, null);
        for (String base : new String[]{"drowned_outer", "stray_outer"}) {
            assertNotNull(pacote.get(CEM + base + "2.jem"));
            Properties regras = new Properties();
            regras.load(new ByteArrayInputStream(pacote.get(CEM + base + ".properties")));
            assertEquals("2", regras.getProperty("models.1"));
            assertNotNull(regras.getProperty("name.1"));
            assertEquals("1", regras.getProperty("models.2"));
            var modelo = JsonParser.parseString(new String(pacote.get(CEM + base + "2.jem"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (var p : modelo.getAsJsonArray("models")) assertTrue(p.getAsJsonObject().getAsJsonArray("boxes").isEmpty());
        }
    }

    @Test
    void edicaoDoServidorContinuaTendoPrioridade(@TempDir Path pasta) throws Exception {
        var modelo = "{\"textureSize\":[16,16],\"models\":[],\"credit\":\"edicao local\"}";
        var sub = Files.createDirectories(pasta.resolve("chefes"));
        Files.writeString(sub.resolve("lich.jem"), modelo);
        var pacote = ModelosLendarios.arquivosDoPacote();
        ModelosVisuais.somar(pacote, pasta.toFile());
        assertTrue(pacote.entrySet().stream().anyMatch(e -> e.getKey().endsWith(".jem")
                && new String(e.getValue(), StandardCharsets.UTF_8).contains("edicao local")));
    }
}
