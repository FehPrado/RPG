import br.rpgatributos.raca.Enfeites;
import br.rpgatributos.ajuda.Fundo;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class ExportarEnfeites {
    public static void main(String[] args) throws Exception {
        Path pasta = Path.of("target/enfeites"); Files.createDirectories(pasta);
        for (var e : Enfeites.arquivosDoPacote().entrySet()) {
            Path p = pasta.resolve(e.getKey()); Files.createDirectories(p.getParent()); Files.write(p, e.getValue());
        }
        for (var e : Enfeites.texturas().entrySet()) {
            Path p = pasta.resolve("assets/rpgatributos/textures/" + e.getKey() + ".png"); Files.createDirectories(p.getParent());
            ImageIO.write(e.getValue(), "png", p.toFile());
        }
        var linhas = new java.util.ArrayList<String>();
        for (var e : Enfeites.Enfeite.values()) linhas.add(e.id + "|" + e.nome);
        Files.write(pasta.resolve("catalogo.txt"), linhas, StandardCharsets.UTF_8);
        ImageIO.write(Fundo.GUARDAROUPA.desenhar(), "png", pasta.resolve("guarda-roupa.png").toFile());
    }
}
