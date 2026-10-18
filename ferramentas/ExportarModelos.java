import br.rpgatributos.domador.ModelosLendarios;
import br.rpgatributos.exploracao.ModelosVisuais;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exporta os modelos usando as mesmas regras e ajustes do pacote do plugin. */
public class ExportarModelos {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Informe o ZIP de saída");
        int quantidade = ModelosVisuais.alvos().size();
        if (ModelosVisuais.prontos(null).size() != quantidade)
            throw new IllegalStateException("Compile o plugin com todos os modelos antes de exportar");
        var arquivos = ModelosLendarios.arquivosDoPacote();
        ModelosVisuais.somar(arquivos, null);
        // Os mesmos formatos usados em PacoteRecursos; execução no cliente ainda deve ser testada.
        arquivos.put("pack.mcmeta", """
            {"pack":{"description":"RPGAtributos: %d visuais + 5 Guardiões (EMF + ETF)",
            "pack_format":97,"min_format":88,"max_format":999}}
            """.formatted(quantidade).getBytes(StandardCharsets.UTF_8));
        Path saida = Path.of(args[0]);
        Files.createDirectories(saida.toAbsolutePath().getParent());
        try (var zip = new ZipOutputStream(Files.newOutputStream(saida))) {
            for (var arquivo : arquivos.entrySet()) {
                var entrada = new ZipEntry(arquivo.getKey());
                entrada.setTime(0);
                zip.putNextEntry(entrada);
                zip.write(arquivo.getValue());
                zip.closeEntry();
            }
        }
        System.out.println((quantidade + 5) + " visuais exportados: " + saida + " (" + arquivos.size() + " arquivos)");
    }
}
