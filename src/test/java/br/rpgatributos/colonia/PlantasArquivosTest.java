package br.rpgatributos.colonia;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os projetos de construção (plantas/*.yml) são lidos sem servidor: cada camada com o mesmo
 * tamanho, toda letra com legenda, efeito que existe e níveis com o mínimo de blocos crescendo.
 */
class PlantasArquivosTest {

    private static final Path PASTA = Path.of("src/main/resources/plantas");
    private static final Pattern BLOCO = Pattern.compile("[a-z0-9_]+(\\[[a-z0-9_=,]+])?");

    /** Os efeitos que existem, lidos do código (carregar o enum pediria um servidor). */
    private static Set<String> efeitos() throws IOException {
        String fonte = Files.readString(Path.of("src/main/java/br/rpgatributos/colonia/Planta.java"));
        int i = fonte.indexOf("enum Efeito {"), j = fonte.indexOf(';', i);
        Set<String> s = new HashSet<>();
        Matcher m = Pattern.compile("\\b([A-Z_]+)\\(\"").matcher(fonte.substring(i, j));
        while (m.find()) s.add(m.group(1));
        return s;
    }

    @TestFactory
    Stream<DynamicTest> cadaPlanta() throws IOException {
        Set<String> efeitos = efeitos();
        assertTrue(efeitos.contains("CASA") && efeitos.contains("MINA"), "não achei os efeitos em Planta.java");
        List<Path> arquivos;
        try (Stream<Path> l = Files.list(PASTA)) {
            arquivos = l.filter(p -> p.toString().endsWith(".yml")).sorted().toList();
        }
        assertFalse(arquivos.isEmpty());
        return arquivos.stream().map(a -> DynamicTest.dynamicTest(a.getFileName().toString(), () -> conferir(a, efeitos)));
    }

    private static void conferir(Path arquivo, Set<String> efeitos) throws IOException {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(Files.newBufferedReader(arquivo));
        String nome = arquivo.getFileName().toString();
        assertNotNull(y.getString("nome"), nome + ": sem nome");
        assertTrue(efeitos.contains(y.getString("efeito", "?")), nome + ": efeito desconhecido " + y.getString("efeito"));
        ConfigurationSection legenda = y.getConfigurationSection("legenda");
        assertNotNull(legenda, nome + ": sem legenda");
        Set<Character> letras = new HashSet<>();
        for (String k : legenda.getKeys(false)) {
            assertEquals(1, k.length(), nome + ": legenda com chave de mais de uma letra: " + k);
            String bloco = legenda.getString(k, "");
            assertTrue(BLOCO.matcher(bloco).matches(), nome + ": bloco estranho na legenda '" + k + "': " + bloco);
            letras.add(k.charAt(0));
        }
        List<?> camadas = y.getList("camadas");
        assertNotNull(camadas, nome + ": sem camadas");
        assertFalse(camadas.isEmpty(), nome + ": sem camadas");
        int fundo = -1, largura = -1;
        for (int c = 0; c < camadas.size(); c++) {
            List<?> linhas = (List<?>) camadas.get(c);
            if (fundo < 0) fundo = linhas.size();
            assertEquals(fundo, linhas.size(), nome + ": a camada " + c + " tem outro número de linhas");
            for (Object o : linhas) {
                String linha = String.valueOf(o);
                if (largura < 0) largura = linha.length();
                assertEquals(largura, linha.length(), nome + ": linha com largura diferente na camada " + c + ": " + linha);
                for (char ch : linha.toCharArray()) {
                    assertTrue(ch == '.' || ch == '~' || letras.contains(ch), nome + ": a letra '" + ch + "' não está na legenda");
                }
            }
        }
        List<?> niveis = y.getList("niveis");
        assertNotNull(niveis, nome + ": sem níveis");
        assertFalse(niveis.isEmpty(), nome + ": sem níveis");
        List<Integer> blocos = new ArrayList<>();
        for (Object o : niveis) {
            assertTrue(o instanceof java.util.Map<?, ?>, nome + ": nível sem formato");
            Object b = ((java.util.Map<?, ?>) o).get("blocos");
            blocos.add(b instanceof Number n ? n.intValue() : 0);
        }
        for (int i = 1; i < blocos.size(); i++) {
            assertTrue(blocos.get(i) >= blocos.get(i - 1), nome + ": o nível " + (i + 1) + " pede menos blocos que o anterior");
        }
    }
}
