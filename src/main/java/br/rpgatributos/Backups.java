package br.rpgatributos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Backup automático dos dados do plugin: ao ligar o servidor, os .yml da pasta do plugin (colônias,
 * territórios, criação, profundezas, jogadores...) são copiados para {@code backups/AAAA-MM-DD},
 * no máximo uma vez por dia. Ficam só os últimos dias da config; os mais velhos são apagados.
 */
public final class Backups {

    private Backups() { }

    /**
     * Faz o backup do dia (se ainda não foi feito) e apaga os antigos.
     *
     * @return quantos arquivos foram copiados (0 se já havia backup hoje ou {@code dias} é 0)
     */
    public static int fazer(Path pasta, int dias, LocalDate hoje) throws IOException {
        if (dias <= 0 || !Files.isDirectory(pasta)) return 0;
        Path raiz = pasta.resolve("backups");
        Path destino = raiz.resolve(hoje.toString());
        int copiados = 0;
        if (!Files.exists(destino)) {
            List<Path> arquivos;
            try (Stream<Path> s = Files.walk(pasta)) {
                arquivos = s.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".yml"))
                        .filter(p -> !p.startsWith(raiz))
                        .toList();
            }
            for (Path a : arquivos) {
                Path alvo = destino.resolve(pasta.relativize(a).toString());
                Files.createDirectories(alvo.getParent());
                Files.copy(a, alvo, StandardCopyOption.REPLACE_EXISTING);
                copiados++;
            }
        }
        limpar(raiz, dias);
        return copiados;
    }

    /** Deixa só os {@code dias} backups mais novos (as pastas têm a data no nome, então a ordem é a do nome). */
    private static void limpar(Path raiz, int dias) throws IOException {
        if (!Files.isDirectory(raiz)) return;
        List<Path> pastas = new ArrayList<>();
        try (Stream<Path> s = Files.list(raiz)) {
            s.filter(Files::isDirectory).filter(p -> p.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}")).forEach(pastas::add);
        }
        pastas.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (int i = 0; i < pastas.size() - dias; i++) apagar(pastas.get(i));
    }

    private static void apagar(Path p) throws IOException {
        try (Stream<Path> s = Files.walk(p)) {
            for (Path x : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(x);
        }
    }
}
