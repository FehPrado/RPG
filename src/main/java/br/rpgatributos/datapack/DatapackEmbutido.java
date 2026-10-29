package br.rpgatributos.datapack;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * O datapack do plugin (encantamentos, tipos de dano e, depois, as dimensões) mora dentro do jar
 * (pasta {@code datapack/}). Ao carregar, o plugin
 * copia ele para {@code <mundo>/datapacks/rpgatributos}; se a cópia era diferente (ou não existia),
 * o servidor precisa reiniciar uma vez, porque o jogo lê os datapacks antes de os plugins ligarem.
 * Quem instala só coloca o jar.
 */
public final class DatapackEmbutido {

    private static final String PASTA_JAR = "datapack/";
    private static final String NOME = "rpgatributos";
    private static final String MARCA = ".versao";

    private static boolean precisaReiniciar;

    private DatapackEmbutido() {
    }

    /** O datapack foi colocado ou trocado agora: o conteúdo novo só vale depois de reiniciar. */
    public static boolean precisaReiniciar() {
        return precisaReiniciar;
    }

    /** Chamado no onLoad: copia o datapack do jar se o que está no mundo for de outra versão. */
    public static void instalar(File jar, Logger log) {
        try (ZipFile zip = new ZipFile(jar)) {
            List<ZipEntry> arquivos = new ArrayList<>();
            zip.stream().filter(e -> !e.isDirectory() && e.getName().startsWith(PASTA_JAR)).forEach(arquivos::add);
            if (arquivos.isEmpty()) return;
            arquivos.sort(Comparator.comparing(ZipEntry::getName));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (ZipEntry e : arquivos) {
                md.update(e.getName().getBytes(StandardCharsets.UTF_8));
                try (InputStream in = zip.getInputStream(e)) {
                    md.update(in.readAllBytes());
                }
            }
            String versao = HexFormat.of().formatHex(md.digest());

            Path destino = pastaDoMundo().toPath().resolve("datapacks").resolve(NOME).toAbsolutePath().normalize();
            Path marca = destino.resolve(MARCA);
            if (Files.isRegularFile(marca) && Files.readString(marca).trim().equals(versao)) return;

            apagar(destino);
            for (ZipEntry e : arquivos) {
                Path alvo = destino.resolve(e.getName().substring(PASTA_JAR.length())).normalize();
                if (!alvo.startsWith(destino)) continue;
                Files.createDirectories(alvo.getParent());
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, alvo);
                }
            }
            Files.writeString(marca, versao);
            precisaReiniciar = true;
            log.warning("Datapack do RPGAtributos colocado em " + destino + ". Reinicie o servidor uma vez para ele valer (encantamentos e mensagens de morte).");
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.warning("Não consegui colocar o datapack do RPGAtributos: " + ex.getMessage());
        }
    }

    /** A pasta do mundo principal (level-name do server.properties; "world" se não achar). */
    private static File pastaDoMundo() {
        String nome = "world";
        File props = new File("server.properties");
        if (props.isFile()) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(props.toPath(), StandardCharsets.UTF_8)) {
                p.load(r);
                nome = p.getProperty("level-name", nome).trim();
            } catch (IOException ignored) {
            }
        }
        return new File(Bukkit.getWorldContainer(), nome);
    }

    private static void apagar(Path pasta) throws IOException {
        if (!Files.exists(pasta)) return;
        try (Stream<Path> s = Files.walk(pasta)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
