package br.rpgatributos.domador;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelos 3D dos cinco Guardiões para quem joga com os mods EMF + ETF (ou OptiFine). Quem não tem os
 * mods continua vendo a pele normal (o jogo ignora a pasta optifine/cem). Cada modelo vale só para a
 * criatura com o nome da lendária (regras por nome nos .properties) e esconde as camadas de pele,
 * sela e coleira para elas não cobrirem o modelo. Os arquivos ficam em resources/modelos/guardioes
 * (feitos no Blockbench; os .bbmodel de origem ficam fora do jar).
 *
 * <ul>
 *   <li>Qilin, Fenghuang e Longma: cavalo (horse2, horse3 e horse4);</li>
 *   <li>Baihu: lobo (wolf2);</li>
 *   <li>Xuanwu: tartaruga (turtle2).</li>
 * </ul>
 */
public final class ModelosLendarios {

    private ModelosLendarios() { }

    private static final String PASTA = "/modelos/guardioes/";

    private static final List<String> ARQUIVOS = List.of(
            "horse.properties", "horse2.jem", "horse3.jem", "horse4.jem",
            "horse_armor.properties", "horse_armor2.jem", "horse_armor3.jem", "horse_armor4.jem",
            "horse_saddle.properties", "horse_saddle2.jem", "horse_saddle3.jem", "horse_saddle4.jem",
            "wolf.properties", "wolf2.jem", "wolf_armor.properties", "wolf_armor2.jem",
            "wolf_collar.properties", "wolf_collar2.jem",
            "turtle.properties", "turtle2.jem",
            "qilin.png", "fenghuang.png", "longma.png", "baihu.png", "xuanwu.png");

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        for (String nome : ARQUIVOS) {
            try (InputStream in = ModelosLendarios.class.getResourceAsStream(PASTA + nome)) {
                if (in == null) throw new IOException("Modelo dos Guardiões não encontrado no jar: " + nome);
                f.put("assets/minecraft/optifine/cem/" + nome, in.readAllBytes());
            }
        }
        return f;
    }
}
