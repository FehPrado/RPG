import br.rpgatributos.exploracao.PacoteRecursos;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gera o mesmo pacote do servidor, para quem usa o ZIP local no cliente. */
public class ExportarPacoteRecursos {
    public static void main(String[] args) throws Exception {
        Path apoio = Path.of("target/pacote-exportado");
        Files.createDirectories(apoio);
        Path destino = Path.of(args.length > 0 ? args[0] : "RPGAtributos-recursos-2.81.0.zip");
        byte[] zip = PacoteRecursos.exportar(apoio.toFile());
        Files.write(destino, zip);
        System.out.println(destino.toAbsolutePath() + " (" + zip.length + " bytes)");
    }
}
