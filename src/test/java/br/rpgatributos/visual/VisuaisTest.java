package br.rpgatributos.visual;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import br.rpgatributos.masmorra.Tema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class VisuaisTest {
    @Test void salvarECarregarLookNaoCopiaEquipamentosENemAceitaIdsDesconhecidos() {
        var look = new Embelezamento.Look(List.of("barba_ana", "chifres_dragao", "cabelo_longo", "inexistente"), 0x123456, 2, 1, 3, 2);
        assertEquals(3, look.enfeites().size());
        assertEquals(look, Embelezamento.Look.ler(look.salvar()).orElseThrow());
        assertTrue(Embelezamento.Look.ler(null).isEmpty());
        assertTrue(Embelezamento.Look.ler("dados antigos quebrados").isEmpty());
        var vazio = new Embelezamento.Look(List.of(), -1, -1, 8, 999, -20);
        assertEquals(vazio, Embelezamento.Look.ler(vazio.salvar()).orElseThrow());
        assertTrue(vazio.corpo() >= 0 && vazio.corpo() < 4);
        assertEquals(0xFFFFFF, vazio.tom());
    }

    @Test void todasAsAnimacoesSaoFinitasECrescemSemSaltarParaEscalasInvalidas() {
        for (String tipo : List.of("buraco_negro", "pilar", "orbe", "runa", "invocacao", "impacto")) {
            for (int i = -2; i <= 110; i++) for (float v : AnimacoesMagicas.escala(tipo, i, 100)) assertTrue(Float.isFinite(v) && v > 0 && v < 6, tipo + " " + i);
        }
        assertTrue(AnimacoesMagicas.escala("buraco_negro", 99, 100)[0] < AnimacoesMagicas.escala("buraco_negro", 50, 100)[0]);
    }

    @Test void decoracoesEModelosTemTexturasECoordenadasValidas() throws Exception {
        var f = ArtesVisuais.arquivosDoPacote();
        assertEquals(105, f.keySet().stream().filter(s -> s.contains("/items/")).count());
        for (Tema t : Tema.values()) for (ObjetoDecorativo o : ObjetoDecorativo.values()) {
            assertSame(o, Decoracoes.objeto(Decoracoes.codigo(o, t)));
            assertSame(t, Decoracoes.tema(Decoracoes.codigo(o, t)));
            assertTrue(o.quantidade > 0);
            assertTrue(f.containsKey("assets/rpgatributos/items/beleza_" + ArtesVisuais.id(o, t) + ".json"));
        }
        for (var entry : f.entrySet()) {
            if (entry.getKey().contains("/models/") && entry.getKey().endsWith(".json")) {
                JsonObject modelo = JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
                String tex = modelo.getAsJsonObject("textures").get("t").getAsString().replace("rpgatributos:", "assets/rpgatributos/textures/") + ".png";
                assertTrue(f.containsKey(tex), tex);
                for (var el : modelo.getAsJsonArray("elements")) {
                    JsonObject cubo = el.getAsJsonObject();
                    for (int i = 0; i < 3; i++) {
                        double de = cubo.getAsJsonArray("from").get(i).getAsDouble(), ate = cubo.getAsJsonArray("to").get(i).getAsDouble();
                        assertTrue(Double.isFinite(de) && Double.isFinite(ate) && de >= -16 && ate <= 32 && de < ate, entry.getKey());
                    }
                }
            }
            if (entry.getKey().endsWith(".png.mcmeta")) {
                JsonObject animation = JsonParser.parseString(new String(entry.getValue(), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animation");
                String png = entry.getKey().substring(0, entry.getKey().length() - ".mcmeta".length());
                var im = ImageIO.read(new ByteArrayInputStream(f.get(png)));
                assertEquals(animation.get("height").getAsInt() * 4, im.getHeight());
                assertEquals(animation.get("width").getAsInt(), im.getWidth());
                boolean mudou = false;
                int altura = animation.get("height").getAsInt();
                for (int y = 0; y < altura; y++) for (int x = 0; x < im.getWidth(); x++)
                    mudou |= im.getRGB(x, y) != im.getRGB(x, y + altura);
                assertTrue(mudou, png);
            }
        }
    }
}
