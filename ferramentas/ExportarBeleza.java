import br.rpgatributos.visual.ArtesVisuais;
import java.nio.file.*;

/** Exporta os modelos nativos para prévias fora do jogo. */
public class ExportarBeleza {
    public static void main(String[] args) throws Exception {
        for (var e : ArtesVisuais.arquivosDoPacote().entrySet()) {
            Path p = Path.of("target/beleza").resolve(e.getKey());
            Files.createDirectories(p.getParent()); Files.write(p, e.getValue());
        }
    }
}
