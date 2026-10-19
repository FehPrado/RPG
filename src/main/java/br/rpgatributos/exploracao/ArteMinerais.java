package br.rpgatributos.exploracao;

import br.rpgatributos.forja.ConjuntoMineral;
import br.rpgatributos.forja.ConjuntoMineral.Peca;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * O desenho dos {@link ConjuntoMineral conjuntos dos minerais} (2.64): as ferramentas usam os moldes
 * das armas com nome do plugin pintados com a paleta do mineral; as armaduras usam o desenho dos
 * Trajes (ícone e textura no corpo). Nada é copiado do jogo.
 * <p>Letras: o = contorno, h = luz, m = cor, d = sombra, a = detalhe, g = guarda, w = cabo, p = pomo.
 */
public final class ArteMinerais {

    private ArteMinerais() { }

    private static final String[] ESPADA = {
            "..............oo",
            ".............oho",
            "............ohmo",
            "...........ohmo.",
            "..........ohmo..",
            ".........ohmo...",
            "........ohmo....",
            ".......ohmo.....",
            "......ohmo......",
            "...o.ohmo.......",
            "...ogohmo.......",
            "....oggo........",
            "...owoggo.......",
            "..owo..oo.......",
            ".opo............",
            "..o............."};

    private static final String[] PICARETA = {
            "................",
            ".....oooooo.....",
            "...oohhmmmdoo...",
            "..ohmao..oamdo..",
            ".ohmo.owo..odmo.",
            ".omo.owo....omo.",
            ".oo.owo......oo.",
            "...owo..........",
            "..owo...........",
            ".owo............",
            "owo.............",
            "oo..............",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] MACHADO = {
            "................",
            "..........ooo...",
            ".........ohhmo..",
            "........ohmamdo.",
            ".......ohmmammdo",
            "......owoommddo.",
            ".....owo.oddoo..",
            "....owo...oo....",
            "...owo..........",
            "..owo...........",
            ".owo............",
            "owo.............",
            "oo..............",
            "................",
            "................",
            "................"};

    private static final String[] PA = {
            "................",
            "...........ooo..",
            "..........ohhmo.",
            ".........ohhmmdo",
            ".........ohmmddo",
            "..........oddoo.",
            ".........owoo...",
            "........owo.....",
            ".......owo......",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o............."};

    private static final String[] ENXADA = {
            "................",
            ".........ooooo..",
            "........ohhmmdo.",
            "........oommddo.",
            "..........owoo..",
            ".........owo....",
            "........owo.....",
            ".......owo......",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o.............",
            "................"};

    private static final int CABO = 0xFF7A5230, POMO = 0xFF4A2E18;

    private static String[] molde(Peca p) {
        return switch (p) {
            case ESPADA -> ESPADA;
            case PICARETA -> PICARETA;
            case MACHADO -> MACHADO;
            case PA -> PA;
            case ENXADA -> ENXADA;
            default -> null;
        };
    }

    /** O ícone de uma ferramenta ou arma (16x16). */
    public static BufferedImage icone(ConjuntoMineral c, Peca p) {
        if (p.armadura) return ArteTrajes.icone(c, p.traje());
        Map<Character, Integer> cores = new HashMap<>();
        cores.put('o', ArteTrajes.escurecer(c.sombra(), 0.5));
        cores.put('h', c.luz());
        cores.put('m', c.base());
        cores.put('d', c.sombra());
        cores.put('a', c.gema());
        cores.put('g', c.detalhe());
        cores.put('w', CABO);
        cores.put('p', POMO);
        String[] m = molde(p);
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) im.setRGB(x, y, cores.getOrDefault(m[y].charAt(x), 0));
        }
        return im;
    }

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new HashMap<>();
        for (ConjuntoMineral c : ConjuntoMineral.values()) {
            for (Peca p : Peca.values()) {
                String v = c.visual(p);
                String pai = p.armadura ? "minecraft:item/generated" : "minecraft:item/handheld";
                f.put("assets/rpgatributos/items/" + v + ".json",
                        ("{ \"model\": { \"type\": \"minecraft:model\", \"model\": \"rpgatributos:item/" + v + "\" } }").getBytes(StandardCharsets.UTF_8));
                f.put("assets/rpgatributos/models/item/" + v + ".json",
                        ("{ \"parent\": \"" + pai + "\", \"textures\": { \"layer0\": \"rpgatributos:item/" + v + "\" } }").getBytes(StandardCharsets.UTF_8));
                f.put("assets/rpgatributos/textures/item/" + v + ".png", ArteTrajes.png(icone(c, p)));
            }
            String id = c.equipamento();
            f.put("assets/rpgatributos/equipment/" + id + ".json", ("""
                    { "layers": {
                        "humanoid": [ { "texture": "rpgatributos:%1$s" } ],
                        "humanoid_leggings": [ { "texture": "rpgatributos:%1$s" } ]
                    } }
                    """.formatted(id)).getBytes(StandardCharsets.UTF_8));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid/" + id + ".png", ArteTrajes.png(ArteTrajes.corpo(c)));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid_leggings/" + id + ".png", ArteTrajes.png(ArteTrajes.pernas(c)));
        }
        return f;
    }
}
