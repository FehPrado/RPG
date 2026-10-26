package br.rpgatributos.exploracao;

import br.rpgatributos.masmorra.Tema;
import br.rpgatributos.masmorra.Monarca;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.EntityType;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Cobertura dos monstros comuns e dos nomes próprios dos portais no pacote EMF. */
public final class ModelosMonstros {
    private ModelosMonstros() { }
    private static final String CEM = "assets/minecraft/optifine/cem/";
    public record Especie(EntityType tipo, String cem, String fonte, String nome) { }
    public static final List<Especie> ESPECIES = List.of(
            new Especie(EntityType.ZOMBIE, "zombie", "elite_zombie_blindado", "Carniçal"),
            new Especie(EntityType.HUSK, "husk", "elite_zombie_incendiario", "Dessecado"),
            new Especie(EntityType.DROWNED, "drowned", "sumo_sacerdote", "Afogado"),
            new Especie(EntityType.SKELETON, "skeleton", "elite_skeleton_blindado", "Sentinela Óssea"),
            new Especie(EntityType.STRAY, "stray", "arqueiro_espectral", "Arqueiro Glacial"),
            new Especie(EntityType.WITHER_SKELETON, "wither_skeleton", "rei_caido", "Cavaleiro Caído"),
            new Especie(EntityType.SPIDER, "spider", "elite_spider_venenoso", "Tecelã"),
            new Especie(EntityType.CAVE_SPIDER, "cave_spider", "rainha_aracnidea", "Aranha das Profundezas"),
            new Especie(EntityType.CREEPER, "creeper", "elite_creeper_blindado", "Explosivo"),
            new Especie(EntityType.WITCH, "witch", "cronista", "Bruxa"),
            new Especie(EntityType.BLAZE, "blaze", "senhor_das_chamas", "Espírito Ígneo"),
            new Especie(EntityType.BREEZE, "breeze", "tempestade_viva", "Espírito dos Ventos"),
            new Especie(EntityType.VINDICATOR, "vindicator", "sombra_sem_nome", "Carrasco"),
            new Especie(EntityType.PILLAGER, "pillager", "bibliotecario_amaldicoado", "Atirador"),
            new Especie(EntityType.EVOKER, "evoker", "bibliotecario_amaldicoado", "Conjurador"),
            new Especie(EntityType.PIGLIN_BRUTE, "piglin_brute", "paladino_corrompido", "Brutamontes"),
            new Especie(EntityType.RAVAGER, "ravager", "fera_ancestral", "Devastador")
    );

    public static String nome(EntityType tipo, Tema tema) {
        return ESPECIES.stream().filter(e -> e.tipo() == tipo).findFirst()
                .map(e -> "۞ " + e.nome() + " · " + tema.nome()).orElse(null);
    }

    private static byte[] fonte(String id, String extensao) throws IOException {
        try (var in = ModelosMonstros.class.getResourceAsStream("/modelos/visual/" + id + extensao)) {
            if (in == null) throw new IOException("Modelo ausente: " + id + extensao);
            return in.readAllBytes();
        }
    }

    private static int modelo(Map<String, byte[]> f, ModelosVisuais.Regras regras, Especie especie, Tema tema) throws IOException {
        JsonObject modelo = JsonParser.parseString(new String(fonte(especie.fonte(), ".jem"), StandardCharsets.UTF_8)).getAsJsonObject();
        String id = "rpg_mob_" + especie.cem() + "_" + tema.name().toLowerCase(java.util.Locale.ROOT);
        modelo.addProperty("texture", id + ".png");
        // Witch não tem as camadas de roupa do aldeão; piglins não têm o headwear do zumbi.
        JsonArray partes = modelo.getAsJsonArray("models");
        if (especie.tipo() == EntityType.WITCH || especie.tipo() == EntityType.PIGLIN_BRUTE) {
            partes.asList().removeIf(e -> {
                String parte = e.getAsJsonObject().get("part").getAsString();
                return parte.equals("headwear") || parte.equals("headwear2") || parte.equals("bodywear");
            });
        }
        // Cada tema leva uma assinatura geométrica no crânio (crista, cristais, chifres ou gelo).
        JsonObject cabeca = null;
        for (var p : partes) if (p.getAsJsonObject().get("part").getAsString().equals("head")) cabeca = p.getAsJsonObject();
        if (cabeca != null) {
            JsonArray filhos = cabeca.getAsJsonArray("submodels");
            if (filhos == null) { filhos = new JsonArray(); cabeca.add("submodels", filhos); }
            for (int n = 0; n <= tema.ordinal(); n++) {
                JsonObject enfeite = new JsonObject();
                enfeite.addProperty("id", "portal_crista_" + n);
                JsonArray caixas = new JsonArray();
                JsonObject caixa = new JsonObject();
                caixa.add("coordinates", JsonParser.parseString("[" + (-3 + n * 2) + ",-6,-1,1," + (2 + tema.ordinal()) + ",2]"));
                caixa.add("textureOffset", JsonParser.parseString("[0,0]"));
                caixas.add(caixa); enfeite.add("boxes", caixas); filhos.add(enfeite);
            }
        }
        int numero = regras.proximoArquivo();
        f.put(CEM + especie.cem() + numero + ".jem", modelo.toString().getBytes(StandardCharsets.UTF_8));
        var textura = ImageIO.read(new ByteArrayInputStream(fonte(especie.fonte(), ".png")));
        float hue = switch (tema) { case CRIPTA -> 0.74f; case CAVERNA -> 0.43f; case INFERNO -> 0.035f; case GELO -> 0.55f; };
        for (int y = 0; y < textura.getHeight(); y++) for (int x = 0; x < textura.getWidth(); x++) {
            int rgb = textura.getRGB(x, y), alpha = rgb >>> 24;
            if (alpha == 0) continue;
            float[] hsb = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
            int cor = Color.HSBtoRGB(hue, Math.max(0.2f, hsb[1] * 0.75f), hsb[2]);
            textura.setRGB(x, y, (alpha << 24) | (cor & 0xFFFFFF));
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream(); ImageIO.write(textura, "png", png);
        f.put(CEM + id + ".png", png.toByteArray());
        regras.regras.add(new String[]{"" + numero, java.util.regex.Pattern.quote(nome(especie.tipo(), tema))});
        return numero;
    }

    public static void somar(Map<String, byte[]> f) throws IOException {
        for (Especie e : ESPECIES) {
            var regras = ModelosVisuais.Regras.ler(f.get(CEM + e.cem() + ".properties"));
            java.util.ArrayList<String> comuns = new java.util.ArrayList<>();
            for (Tema tema : Tema.values()) comuns.add("" + modelo(f, regras, e, tema));
            // As regras especiais continuam primeiro; criaturas comuns também ganham modelos fora dos portais.
            regras.padrao = String.join(" ", comuns);
            f.put(CEM + e.cem() + ".properties", regras.gravar("RPGAtributos - especiais e monstros comuns"));
        }
        // Os chefes renomeados nos portais precisam de regras com os nomes novos.
        for (Monarca m : Monarca.values()) {
            String cem = switch (m.base) { case CHAMAS -> "blaze"; case LICH -> "skeleton"; case GOLEM -> "iron_golem"; case RAINHA -> "spider"; case ARAUTO -> "wither_skeleton"; default -> throw new IllegalStateException(); };
            String original = switch (m.base) { case CHAMAS -> "senhor_das_chamas"; case LICH -> "lich"; case GOLEM -> "golem_ancestral"; case RAINHA -> "rainha_aracnidea"; case ARAUTO -> "arauto_do_fim"; default -> throw new IllegalStateException(); };
            var regras = ModelosVisuais.Regras.ler(f.get(CEM + cem + ".properties"));
            int n = regras.proximoArquivo();
            JsonObject modelo = JsonParser.parseString(new String(fonte(original, ".jem"), StandardCharsets.UTF_8)).getAsJsonObject();
            modelo.addProperty("texture", "rpg_" + original + ".png");
            f.put(CEM + cem + n + ".jem", modelo.toString().getBytes(StandardCharsets.UTF_8));
            f.putIfAbsent(CEM + "rpg_" + original + ".png", fonte(original, ".png"));
            regras.regras.addFirst(new String[]{"" + n, ".*" + java.util.regex.Pattern.quote(m.chefe) + ".*"});
            f.put(CEM + cem + ".properties", regras.gravar("RPGAtributos - Monarcas e monstros"));
        }
    }
}
