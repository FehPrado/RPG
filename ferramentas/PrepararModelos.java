import br.rpgatributos.exploracao.ModelosVisuais;
import com.google.gson.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * Gera blockbench/preparados (2.55): um .jem com a forma vanilla e um .png com a textura vanilla de cada
 * alvo de ModelosVisuais. Uso (com o jar do plugin e o gson no classpath):
 *   java -cp RPGAtributos.jar;gson.jar ferramentas/PrepararModelos.java cem_template_models.json client.jar blockbench/preparados
 * cem_template_models.json: https://raw.githubusercontent.com/ewanhowell5195/wynem/main/src/assets/json/cem_template_models.json
 * client.jar: o jar do cliente do Minecraft (texturas em assets/minecraft/textures/entity).
 */
public class PrepararModelos {
    public static void main(String[] a) throws Exception {
        JsonObject raiz = JsonParser.parseString(Files.readString(Path.of(a[0]))).getAsJsonObject();
        JsonObject tm = raiz.getAsJsonObject("models");
        Map<String, String> apelido = new HashMap<>();
        for (JsonElement c : raiz.getAsJsonArray("categories")) {
            JsonObject co = c.getAsJsonObject();
            if (!co.has("entities")) continue;
            for (JsonElement e : co.getAsJsonArray("entities")) {
                JsonObject eo = e.getAsJsonObject();
                List<JsonObject> todos = new ArrayList<>(List.of(eo));
                if (eo.has("variants")) for (JsonElement v : eo.getAsJsonArray("variants")) todos.add(v.getAsJsonObject());
                for (JsonObject x : todos) if (x.has("id")) apelido.putIfAbsent(x.get("id").getAsString(), x.has("model") ? x.get("model").getAsString() : x.get("id").getAsString());
            }
        }
        ZipFile jar = new ZipFile(a[1]);
        Path saida = Path.of(a[2]);
        Gson g = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Map<String, List<ModelosVisuais.Alvo>> porGrupo = new TreeMap<>();
        int erros = 0;
        for (ModelosVisuais.Alvo al : ModelosVisuais.alvos()) {
            JsonObject t = tm.getAsJsonObject(apelido.getOrDefault(al.cem(), al.cem()));
            if (t == null) { System.out.println("SEM TEMPLATE " + al.cem()); erros++; continue; }
            JsonObject jem = JsonParser.parseString(t.get("model").getAsString()).getAsJsonObject();
            JsonObject novo = new JsonObject();
            novo.addProperty("texture", al.id() + ".png");
            for (var e : jem.entrySet()) novo.add(e.getKey(), e.getValue());
            BufferedImage img = null;
            for (String tx : al.texturas()) {
                var en = jar.getEntry("assets/minecraft/textures/entity/" + tx + ".png");
                if (en == null) { System.out.println("SEM TEXTURA " + tx + " (" + al.id() + ")"); erros++; continue; }
                BufferedImage camada = ImageIO.read(jar.getInputStream(en));
                if (img == null) {
                    img = new BufferedImage(camada.getWidth(), camada.getHeight(), BufferedImage.TYPE_INT_ARGB);
                }
                var gr = img.createGraphics();
                gr.drawImage(camada, 0, 0, img.getWidth(), img.getHeight(), null);
                gr.dispose();
            }
            JsonArray ts = jem.getAsJsonArray("textureSize");
            if (img != null && ts != null && (ts.get(0).getAsInt() != img.getWidth() || ts.get(1).getAsInt() != img.getHeight())) {
                System.out.println("AVISO tamanho " + al.id() + ": template " + ts + " textura " + img.getWidth() + "x" + img.getHeight());
            }
            Path pasta = saida.resolve(al.grupo());
            Files.createDirectories(pasta);
            Files.writeString(pasta.resolve(al.id() + ".jem"), g.toJson(novo));
            if (img != null) ImageIO.write(img, "png", pasta.resolve(al.id() + ".png").toFile());
            porGrupo.computeIfAbsent(al.grupo(), k -> new ArrayList<>()).add(al);
        }
        StringBuilder leia = new StringBuilder();
        leia.append("MODELOS PREPARADOS - RPGAtributos 2.55 (EMF + ETF)\r\n");
        leia.append("==================================================\r\n\r\n");
        leia.append("Cada .jem aqui é a forma vanilla exata da criatura (modelo de base do CEM Template Loader)\r\n");
        leia.append("e cada .png é a textura vanilla dela (para aldeões: pele + bioma + profissão já juntos).\r\n\r\n");
        leia.append("COMO EDITAR\r\n");
        leia.append("  1. Blockbench > File > Open Model > escolha o .jem (abre no formato OptiFine Entity).\r\n");
        leia.append("  2. Mude a forma e pinte a textura à vontade. NÃO renomeie as pastas do topo (head, body...):\r\n");
        leia.append("     é por esse nome que o EMF anima. Peças novas vão DENTRO delas.\r\n");
        leia.append("  3. File > Export > Export OptiFine JEM, por cima do mesmo arquivo; salve a textura também.\r\n\r\n");
        leia.append("COMO COLOCAR NO JOGO\r\n");
        leia.append("  Copie o <id>.jem e o <id>.png para plugins/RPGAtributos/pacote/modelos/ no servidor\r\n");
        leia.append("  (pode manter as subpastas) e use /rpgadmin modelos: o pacote é refeito com as regras por nome.\r\n");
        leia.append("  Se o pacote vem do Dropbox, suba o zip novo (o SHA-1 muda). Ou me mande os arquivos e eu\r\n");
        leia.append("  coloco dentro do jar (resources/modelos/visual).\r\n\r\n");
        leia.append("  Só entra no pacote o que tiver .jem; o resto continua vanilla. Sem os mods ninguém vê diferença.\r\n");
        leia.append("  Aldeão com modelo próprio: as camadas de bioma/profissão/nível somem (a roupa é a sua textura).\r\n");
        leia.append("  A criatura é escolhida pelo NOME que aparece em cima dela: companheiro renomeado pelo jogador\r\n");
        leia.append("  volta ao visual normal.\r\n\r\n");
        leia.append("O QUE TEM AQUI (id - criatura - regra de nome)\r\n");
        for (var en : porGrupo.entrySet()) {
            leia.append("\r\n[").append(en.getKey()).append("]\r\n");
            for (var al : en.getValue()) {
                leia.append("  ").append(al.id()).append(" - ").append(al.sobre()).append(" - ").append(al.cem())
                        .append(" com nome ").append(al.nome()).append("\r\n");
            }
        }
        Files.writeString(saida.resolve("LEIA-ME.txt"), leia.toString(), StandardCharsets.UTF_8);
        System.out.println("alvos=" + ModelosVisuais.alvos().size() + " grupos=" + porGrupo.size() + " erros=" + erros);
    }
}
