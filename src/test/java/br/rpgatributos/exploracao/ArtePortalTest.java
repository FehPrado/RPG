package br.rpgatributos.exploracao;

import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.masmorra.EquipamentoPortal;
import br.rpgatributos.masmorra.Rank;
import br.rpgatributos.masmorra.EstiloPortal;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArtePortalTest {
    @Test
    void pacoteCompletoSemReferenciasQuebradas() throws Exception {
        Map<String, byte[]> arquivos = ArtePortal.arquivosDoPacote();
        assertEquals(285, arquivos.keySet().stream().filter(s -> s.contains("/items/")).count());
        for (var entrada : arquivos.entrySet()) {
            if (entrada.getKey().endsWith(".json")) {
                validar(JsonParser.parseString(new String(entrada.getValue(), StandardCharsets.UTF_8)), arquivos);
            } else if (entrada.getKey().endsWith(".png")) {
                var imagem = ImageIO.read(new ByteArrayInputStream(entrada.getValue()));
                assertNotNull(imagem, entrada.getKey());
                assertEquals(entrada.getKey().contains("/entity/") ? 64 : 16, imagem.getWidth(), entrada.getKey());
                assertEquals(entrada.getKey().contains("/entity/") ? 32 : 16, imagem.getHeight(), entrada.getKey());
                boolean visivel = false;
                for (int y = 0; y < imagem.getHeight(); y++) for (int x = 0; x < imagem.getWidth(); x++) visivel |= (imagem.getRGB(x, y) >>> 24) > 0;
                assertTrue(visivel, entrada.getKey());
            }
        }
        Set<String> ids = new HashSet<>();
        for (Rank r : Rank.values()) for (EstiloPortal e : EstiloPortal.values()) for (EquipamentoPortal.Peca p : EquipamentoPortal.Peca.values()) {
            String id = ArtePortal.visual(r, p, e);
            assertTrue(ids.add(id));
            assertTrue(arquivos.containsKey("assets/rpgatributos/items/" + id + ".json"));
            assertNotNull(EquipamentoPortal.material(r, p));
        }
        for (Chefe c : Chefe.values()) for (EstiloPortal e : EstiloPortal.values()) {
            String id = ArtePortal.visual(c, e);
            assertTrue(ids.add(id));
            assertTrue(arquivos.containsKey("assets/rpgatributos/items/" + id + ".json"));
        }
        assertEquals(216, ids.size());
    }

    private static void validar(JsonElement json, Map<String, byte[]> arquivos) {
        if (json.isJsonArray()) { json.getAsJsonArray().forEach(e -> validar(e, arquivos)); return; }
        if (!json.isJsonObject()) return;
        for (var campo : json.getAsJsonObject().entrySet()) {
            JsonElement valor = campo.getValue();
            if (valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isString()) {
                String s = valor.getAsString();
                if (s.startsWith("rpgatributos:")) {
                    String caminho = s.substring("rpgatributos:".length());
                    if (campo.getKey().equals("model")) assertTrue(arquivos.containsKey("assets/rpgatributos/models/" + caminho + ".json"), s);
                    if (campo.getKey().equals("layer0") || campo.getKey().equals("0") || campo.getKey().equals("particle"))
                        assertTrue(arquivos.containsKey("assets/rpgatributos/textures/" + caminho + ".png"), s);
                    if (campo.getKey().equals("texture")) {
                        assertTrue(arquivos.containsKey("assets/rpgatributos/textures/entity/equipment/humanoid/" + caminho + ".png"), s);
                        assertTrue(arquivos.containsKey("assets/rpgatributos/textures/entity/equipment/humanoid_leggings/" + caminho + ".png"), s);
                    }
                }
            }
            validar(valor, arquivos);
        }
    }

    @Test
    void arcoTemTodasAsFasesECadaRankTemArmaduraVestida() throws Exception {
        Map<String, byte[]> f = ArtePortal.arquivosDoPacote();
        String arco = ArtePortal.visual(Chefe.LICH);
        String definicao = new String(f.get("assets/rpgatributos/items/" + arco + ".json"), StandardCharsets.UTF_8);
        for (int fase = 0; fase < 3; fase++) assertTrue(definicao.contains(arco + "_pulling_" + fase));
        Set<Integer> paletas = new HashSet<>();
        for (Rank r : Rank.values()) {
            assertTrue(paletas.add(ArtePortal.paleta(r).base()));
            var corpo = ArteTrajes.corpo(ArtePortal.paleta(r));
            var pernas = ArteTrajes.pernas(ArtePortal.paleta(r));
            assertNotEquals(0, corpo.getRGB(8, 8) >>> 24, r + " elmo");
            assertNotEquals(0, corpo.getRGB(22, 24) >>> 24, r + " peito");
            assertNotEquals(0, pernas.getRGB(5, 22) >>> 24, r + " grevas");
        }
    }
}
