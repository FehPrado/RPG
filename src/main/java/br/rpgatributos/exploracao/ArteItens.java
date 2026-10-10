package br.rpgatributos.exploracao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pixel art (16×16) dos peixes raros, pratos da Cozinha, variedades raras, componentes
 * alquímicos e elixires. Cada letra do desenho é uma cor da paleta e '.' é transparente.
 * {@code vanilla} é o item do jogo em que o visual entra (por custom_model_data), e
 * {@code id} é o nome da textura e da marca no item.
 */
final class ArteItens {

    record Arte(String vanilla, String id, String[] desenho, Map<Character, Integer> cores, boolean naMao) {
        Arte(String vanilla, String id, String[] desenho, Map<Character, Integer> cores) {
            this(vanilla, id, desenho, cores, false);
        }
    }

    private ArteItens() { }

    private static Map<Character, Integer> c(Object... kv) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((Character) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static Map<Character, Integer> com(Map<Character, Integer> base, Object... kv) {
        Map<Character, Integer> m = new HashMap<>(base);
        m.putAll(c(kv));
        return m;
    }

    // =====================================================================
    //  Peixes
    // =====================================================================

    private static final String[] PEIXE = {
            "................",
            "................",
            "................",
            "................",
            ".....ooooo......",
            "...oohhhhmmo..oo",
            "..ohhmmmmmmdoodo",
            ".ohwkmmmmmmmddmo",
            ".ommmmmmmmmmddmo",
            "..odmmmmmmmdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_PINTADO = {
            "................",
            "................",
            "................",
            "................",
            ".....ooooo......",
            "...oohhahmmo..oo",
            "..ohhmmammadoodo",
            ".ohwkmammmamddmo",
            ".ommammmammmddmo",
            "..odmmamammdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_LISTRADO = {
            "................",
            "................",
            "................",
            "................",
            ".....ooooo......",
            "...oohhhhmmo..oo",
            "..ohhmmmmmmdoodo",
            ".ohwkmmmmmmmddmo",
            ".oaaaaaaaaaaddmo",
            "..odmmmmmmmdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_CRISTAIS = {
            "................",
            "................",
            "................",
            ".....a.a.a......",
            ".....ooooo......",
            "...oohhhhmmo..oo",
            "..ohhmmammmdoodo",
            ".ohwkmmmmammddmo",
            ".ommmammmmmmddmo",
            "..odmmmmmamdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_COROA = {
            "................",
            "................",
            "...c.c.c........",
            "...ccccc........",
            ".....ooooo......",
            "...oohhhhmmo..oo",
            "..ohhmmmmmmdoodo",
            ".ohwkmmmmmmmddmo",
            ".ommmmmmmmmmddmo",
            "..odmmmmmmmdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_ABISSAL = {
            "................",
            "...aa...........",
            "...aa...........",
            ".....d..........",
            ".....ooooo......",
            "...oohhhhmmo..oo",
            "..ohhmmmmmmdoodo",
            ".ohwkmmmmmmmddmo",
            ".ommmmmmmmmmddmo",
            "..owwmmmmmmdoodo",
            "...oodddddddo.oo",
            ".....ooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_DRAGAO = {
            "................",
            "................",
            "................",
            "......a.a.a.a...",
            ".....oaoaoaoo...",
            "...oohhhhmmo..oo",
            "..ohhmmmmmmdoaao",
            ".ohwkmmmmmmmddmo",
            ".ommmmmmmmmmddmo",
            "..odmmmmmmmdoaao",
            "...oodddddddo.oo",
            ".....ooaaooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PEIXE_REDONDO = {
            "................",
            "................",
            ".......oo.......",
            "......ohho......",
            "....oohmmmoo....",
            "...ohmmmmmmdo...",
            "..ohwkmmmmmmdo..",
            "..ommmmmmammdoo.",
            "..ommmmmmmamddoo",
            "..ommmmmmammdoo.",
            "...odmmmmmmdo...",
            "....oodmmdoo....",
            "......oddo......",
            ".......oo.......",
            "................",
            "................"};

    private static final String[] ENGUIA = {
            "................",
            "................",
            "....a......ooo..",
            "..........ohwko.",
            ".........ohmmmo.",
            "........ohmmdo..",
            "..a....ohmmdo...",
            "......ohmmdo....",
            ".....ohmmdo.....",
            "....ohmmdo...a..",
            "...ohmmdo.......",
            "..ohmmdo........",
            "..ommdo.....a...",
            "...ooo..........",
            "................",
            "................"};

    private static final String[] BAIACU = {
            "................",
            ".....c.c.c......",
            ".....ccccc......",
            "...a..oooo..a...",
            "....oohhhhoo....",
            "..aohmmmmmmdoa..",
            "...ohwkmmmmmdo..",
            ".aohmmmmmmmmmdoo",
            "..ommmmmmmmmddoo",
            ".aodmmmmmmmmdoa.",
            "...odmmmmmmdo...",
            "..a.oodddddo.a..",
            "......oooo......",
            "................",
            "................",
            "................"};

    private static final int BRANCO = 0xFFFFFFFF, PRETO = 0xFF1A1A1A;

    private static void peixes(List<Arte> l) {
        l.add(new Arte("cod", "peixe_carpa_dourada", PEIXE,
                c('o', 0xFF6B4A00, 'd', 0xFFB8860B, 'm', 0xFFFFC93C, 'h', 0xFFFFF0A0, 'w', BRANCO, 'k', PRETO)));
        l.add(new Arte("salmon", "peixe_truta_arco_iris", PEIXE_LISTRADO,
                c('o', 0xFF2F4A3A, 'd', 0xFF5E7D6A, 'm', 0xFF9CC2A8, 'h', 0xFFE0F0E6, 'w', BRANCO, 'k', PRETO, 'a', 0xFFFF7FAF)));
        l.add(new Arte("tropical_fish", "peixe_peixe_lua", PEIXE_REDONDO,
                c('o', 0xFF3F4A73, 'd', 0xFF8A97C9, 'm', 0xFFC9D6FF, 'h', 0xFFF0F4FF, 'w', BRANCO, 'k', PRETO, 'a', 0xFFFFF5B0)));
        l.add(new Arte("cod", "peixe_peixe_pedra", PEIXE_PINTADO,
                c('o', 0xFF2E2E2E, 'd', 0xFF5A5A5A, 'm', 0xFF8C8C8C, 'h', 0xFFBDBDBD, 'w', BRANCO, 'k', PRETO, 'a', 0xFF4A4A4A)));
        l.add(new Arte("cod", "peixe_enguia_eletrica", ENGUIA,
                c('o', 0xFF3A3000, 'd', 0xFF9A8A10, 'm', 0xFFFFE04D, 'h', 0xFFFFF7B0, 'w', BRANCO, 'k', PRETO, 'a', 0xFF9FF0FF)));
        l.add(new Arte("cod", "peixe_peixe_gelo", PEIXE_CRISTAIS,
                c('o', 0xFF2A5A78, 'd', 0xFF5FA8D0, 'm', 0xFFA8E8FF, 'h', 0xFFE8FBFF, 'w', BRANCO, 'k', PRETO, 'a', BRANCO)));
        l.add(new Arte("tropical_fish", "peixe_koi_celeste", PEIXE_PINTADO,
                c('o', 0xFF6A3A4A, 'd', 0xFFD0B0B8, 'm', 0xFFFFF4F7, 'h', BRANCO, 'w', 0xFF7FB8FF, 'k', PRETO, 'a', 0xFFFF6F91)));
        l.add(new Arte("pufferfish", "peixe_baiacu_rei", BAIACU,
                c('o', 0xFF5A4200, 'd', 0xFFC9A020, 'm', 0xFFFFD23F, 'h', 0xFFFFF0A0, 'w', BRANCO, 'k', PRETO, 'a', 0xFF8A6A10, 'c', 0xFFFFB300)));
        l.add(new Arte("salmon", "peixe_salmao_rei", PEIXE_COROA,
                c('o', 0xFF6A2410, 'd', 0xFFC0452A, 'm', 0xFFFF6F3C, 'h', 0xFFFFB08A, 'w', BRANCO, 'k', PRETO, 'c', 0xFFFFD23F)));
        l.add(new Arte("salmon", "peixe_peixe_fantasma", PEIXE,
                c('o', 0xFF7A8A8C, 'd', 0xFFA8B8BA, 'm', 0xFFD0DCDE, 'h', 0xFFF2F8F8, 'w', BRANCO, 'k', 0xFF5A6A6C)));
        l.add(new Arte("cod", "peixe_peixe_abissal", PEIXE_ABISSAL,
                c('o', 0xFF140F2E, 'd', 0xFF2A2060, 'm', 0xFF3B2E7E, 'h', 0xFF6A5AB0, 'w', BRANCO, 'k', 0xFF7FFFE0, 'a', 0xFF7FFFE0)));
        l.add(new Arte("tropical_fish", "peixe_peixe_dragao", PEIXE_DRAGAO,
                c('o', 0xFF5A0A14, 'd', 0xFFB01830, 'm', 0xFFFF4058, 'h', 0xFFFF9AA8, 'w', BRANCO, 'k', PRETO, 'a', 0xFFFFC93C)));
        // 2.29: os peixes do gelo.
        l.add(new Arte("salmon", "peixe_truta_do_gelo", PEIXE_LISTRADO,
                c('o', 0xFF2F5A78, 'd', 0xFF6FA8C8, 'm', 0xFFBFE9FF, 'h', 0xFFF0FBFF, 'w', BRANCO, 'k', PRETO, 'a', 0xFFFF9AB0)));
        l.add(new Arte("cod", "peixe_lucio_polar", PEIXE_PINTADO,
                c('o', 0xFF4A5A66, 'd', 0xFF9AAAB6, 'm', 0xFFE3F2FD, 'h', BRANCO, 'w', BRANCO, 'k', PRETO, 'a', 0xFF78909C)));
        l.add(new Arte("tropical_fish", "peixe_peixe_lanterna", PEIXE_ABISSAL,
                c('o', 0xFF0E2A40, 'd', 0xFF1E5A80, 'm', 0xFF3F9FD0, 'h', 0xFF7FDBFF, 'w', BRANCO, 'k', 0xFFFFF59D, 'a', 0xFFFFF59D)));
        l.add(new Arte("cod", "peixe_enguia_de_cristal", ENGUIA,
                c('o', 0xFF3A2A60, 'd', 0xFF7E6AB0, 'm', 0xFFB39DDB, 'h', 0xFFEDE7F6, 'w', BRANCO, 'k', PRETO, 'a', BRANCO)));
        l.add(new Arte("cod", "peixe_esturjao_ancestral", PEIXE_COROA,
                c('o', 0xFF1C262B, 'd', 0xFF37474F, 'm', 0xFF546E7A, 'h', 0xFF90A4AE, 'w', BRANCO, 'k', PRETO, 'c', 0xFFB0BEC5)));
    }

    // =====================================================================
    //  Pratos
    // =====================================================================

    private static final String[] TIGELA = {
            "................",
            "................",
            "................",
            "......s....s....",
            ".....s....s.....",
            "......s....s....",
            ".oooooooooooooo.",
            ".ocacccbcccacco.",
            ".owwwwwwwwwwwwo.",
            "..owmmmmmmmmwo..",
            "..odmmmmmmmmdo..",
            "...odmmmmmmdo...",
            "....oddddddo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] TIGELA_FRIA = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "......b..a......",
            ".oooocccccccooo.",
            ".ocacccbcccacco.",
            ".owwwwwwwwwwwwo.",
            "..owmmmmmmmmwo..",
            "..odmmmmmmmmdo..",
            "...odmmmmmmdo...",
            "....oddddddo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] TORTA = {
            "................",
            "................",
            "................",
            "................",
            ".....oooooo.....",
            "...oowwwwwwoo...",
            "..owwccccccwwo..",
            ".owcccacccaccwo.",
            ".owcacccaccccwo.",
            ".owwccccccccwwo.",
            ".odwwwwwwwwwwdo.",
            "..oddddddddddo..",
            "...oooooooooo...",
            "................",
            "................",
            "................"};

    private static final String[] PAO = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...ohhhmhhhmo...",
            "..ohmmhmmmhmmo..",
            ".ohmmmmmmmmmmdo.",
            ".ommmmmmmmmmmdo.",
            ".odmmmmmmmmmddo.",
            "..oddddddddddo..",
            "...oooooooooo...",
            "................",
            "................",
            "................"};

    private static final String[] PAO_DOURADO = {
            "................",
            "................",
            "................",
            "....a.....a.....",
            "......a.......a.",
            "....oooooooo....",
            "...ohhhmhhhmo...",
            "..ohmmhmmmhmmo..",
            ".ohmmmmmmmmmmdo.",
            ".ommmmmmmmmmmdo.",
            ".odmmmmmmmmmddo.",
            "..oddddddddddo..",
            "...oooooooooo...",
            "................",
            "................",
            "................"};

    private static final String[] BISCOITO = {
            "................",
            "................",
            "................",
            "................",
            ".....oooooo.....",
            "...oohmmmmmmoo..",
            "..ohmmammmmammo.",
            "..ommmmmhmmmmmo.",
            "..omammmmmmamdo.",
            "..ommmmammmmmdo.",
            "...odmmmmmmddo..",
            "....oodddddoo...",
            "......ooooo.....",
            "................",
            "................",
            "................"};

    private static final String[] FILE = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...omhmmhmmhmo..",
            "..omammhmmamhmo.",
            "..ommhmmahmmmdo.",
            "...odmmmmmmmdo..",
            "....oooooooooo..",
            ".owwwwwwwwwwwwo.",
            "..oooooooooooo..",
            "................",
            "................",
            "................"};

    private static final String[] SUSHI = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "....a.......a...",
            "..oooooo..oooooo",
            "..ommhmo..ommhmo",
            "..ohmmmo..ohmmmo",
            "..okkkko..okkkko",
            "..owwwwo..owwwwo",
            "..owwwwo..owwwwo",
            "..oooooo..oooooo",
            "................",
            "................",
            "................"};

    private static final String[] BATATA_RECHEADA = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".....oaaao......",
            "....oabaabo.....",
            "...ohaaaaamo....",
            "..ohmmaaammdo...",
            "..ommmmmmmmdo...",
            "..odmmmmmmddo...",
            "...oddddddoo....",
            "....ooooooo.....",
            "................",
            "................",
            "................"};

    private static final String[] CARNE = {
            "................",
            "................",
            "................",
            "...........oo...",
            "..........owwo..",
            ".........owwo...",
            "....ooooowwo....",
            "...ohhmmmoo.....",
            "..ohmmmmmmdo....",
            ".ohmmamammmdo...",
            ".ommmmmmmmmdo...",
            ".odmmmmmmmddo...",
            "..oddddddddo....",
            "...oooooooo.....",
            "................",
            "................"};

    private static final Map<Character, Integer> TIGELA_MADEIRA = c('o', 0xFF3B2412, 'd', 0xFF6B4423, 'm', 0xFF9C6B3A,
            'w', 0xFFC8955A, 's', 0xFFDDDDDD);
    private static final Map<Character, Integer> MASSA = c('o', 0xFF4A2A10, 'd', 0xFF8A5A2A, 'm', 0xFFC8883A, 'h', 0xFFE8B868);
    private static final Map<Character, Integer> CROSTA = c('o', 0xFF4A2A10, 'd', 0xFFA0703A, 'w', 0xFFE0B070);

    private static void pratos(List<Arte> l) {
        l.add(new Arte("bread", "prato_pao_caseiro", PAO, MASSA));
        l.add(new Arte("mushroom_stew", "prato_ensopado_legumes", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFFB5651D, 'a', 0xFFFF8C1A, 'b', 0xFF5DBB3A)));
        l.add(new Arte("pumpkin_pie", "prato_torta_abobora", TORTA, com(CROSTA, 'c', 0xFFE88A1A, 'a', 0xFFFFF0D0)));
        l.add(new Arte("cooked_salmon", "prato_peixe_ervas", FILE,
                c('o', 0xFF5A2410, 'm', 0xFFFF8C5A, 'h', 0xFFFFC0A0, 'd', 0xFFC05A30, 'a', 0xFF3E9E3E, 'w', 0xFFF0F0F0)));
        l.add(new Arte("baked_potato", "prato_batata_recheada", BATATA_RECHEADA, com(MASSA, 'a', 0xFFFFD23F, 'b', 0xFFC0392B)));
        l.add(new Arte("cookie", "prato_biscoito_mel", BISCOITO, com(MASSA, 'a', 0xFFFFB300)));
        l.add(new Arte("rabbit_stew", "prato_ensopado_cacador", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFF7A4A2A, 'a', 0xFFB03A2E, 'b', 0xFFE8C15A)));
        l.add(new Arte("beetroot_soup", "prato_salada_arcana", TIGELA_FRIA,
                com(TIGELA_MADEIRA, 'c', 0xFF6BBF4A, 'a', 0xFFC77DFF, 'b', 0xFFE0115F)));
        l.add(new Arte("cooked_beef", "prato_banquete_guerreiro", CARNE,
                c('o', 0xFF3A1A0A, 'd', 0xFF7A3A1A, 'm', 0xFFB0603A, 'h', 0xFFE09060, 'a', 0xFF5A2A10, 'w', 0xFFF0EAD8)));
        l.add(new Arte("bread", "prato_pao_dourado", PAO_DOURADO,
                c('o', 0xFF5A3A00, 'd', 0xFFC08A10, 'm', 0xFFFFC93C, 'h', 0xFFFFF0A0, 'a', BRANCO)));
        l.add(new Arte("mushroom_stew", "prato_sopa_cristalina", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFF7FE3D4, 'a', BRANCO, 'b', 0xFF2FB0A8)));
        l.add(new Arte("pumpkin_pie", "prato_torta_rubi", TORTA, com(CROSTA, 'c', 0xFFC0103A, 'a', 0xFFFF8FA8)));
        l.add(new Arte("beetroot_soup", "prato_sopa_pescador", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFFF0E0C0, 'a', 0xFFFF8C5A, 'b', 0xFF5DBB3A)));
        l.add(new Arte("cooked_salmon", "prato_sushi_real", SUSHI,
                c('o', 0xFF3A2A2A, 'm', 0xFFFF8C5A, 'h', 0xFFFFD0B0, 'k', 0xFF1E3A1E, 'w', 0xFFF8F8F0, 'a', 0xFFFFD23F)));
        l.add(new Arte("rabbit_stew", "prato_caldeirada_abissal", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFF3B2E7E, 'a', 0xFF7FFFE0, 'b', 0xFFFF8C1A)));
        l.add(new Arte("suspicious_stew", "prato_banquete_lendario", TIGELA,
                c('o', 0xFF4A3000, 'd', 0xFF9A6A00, 'm', 0xFFE0A800, 'w', 0xFFFFE07A, 'c', 0xFFC0392B, 'a', 0xFFFFD23F,
                        'b', 0xFFFF8C1A, 's', 0xFFFFF59D)));
        // 2.29: pratos da pesca no gelo.
        l.add(new Arte("mushroom_stew", "prato_caldo_quente", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFFE8C9A0, 'a', 0xFFBFE9FF, 'b', 0xFFFF8C5A)));
        l.add(new Arte("beetroot_soup", "prato_sorvete_cristal", TIGELA_FRIA,
                com(TIGELA_MADEIRA, 'c', 0xFFE8E0FF, 'a', 0xFFB39DDB, 'b', BRANCO)));
        l.add(new Arte("suspicious_stew", "prato_caviar_ancestral", TIGELA,
                com(TIGELA_MADEIRA, 'c', 0xFF263238, 'a', 0xFF546E7A, 'b', 0xFFFFD23F)));
    }

    // =====================================================================
    //  Variedades raras e sementes
    // =====================================================================

    private static final String[] TRIGO = {
            "................",
            ".....h...h...h..",
            "....hmh.hmh.hmh.",
            "....mdm.mdm.mdm.",
            "....hmh.hmh.hmh.",
            "....mdm.mdm.mdm.",
            ".....d...d...d..",
            ".....s...s...s..",
            "......s..s..s...",
            ".......s.s.s....",
            "........sss.....",
            "........sss.....",
            ".......oaao.....",
            "........sss.....",
            ".......s.s.s....",
            "................"};

    private static final String[] CENOURA = {
            "................",
            "..........g.g...",
            "..........ggg...",
            ".........ogg....",
            "........ohmo....",
            ".......ohmmo....",
            "......ohmmdo....",
            ".....ohmmdo.....",
            "....ohmmdo......",
            "...ohmmdo.......",
            "..ohmdo.........",
            "..omdo..........",
            "...oo...........",
            "................",
            "................",
            "................"};

    private static final String[] BATATA = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".....oooooo.....",
            "...oohmmmmmoo...",
            "..ohmmamdmmmdo..",
            "..ommmmmmammdo..",
            "..omamdmmmmmdo..",
            "..odmmmmmamdo...",
            "...oddmmmddo....",
            ".....ooooo......",
            "................",
            "................",
            "................"};

    private static final String[] BETERRABA = {
            "................",
            "......g..g......",
            ".....ggg.gg.....",
            "......gggg......",
            ".......oo.......",
            ".....oohhoo.....",
            "....ohhmmmdo....",
            "...ohmmmammdo...",
            "...ohmmamammdo..",
            "...ommmmmmmdo...",
            "....odmmmmdo....",
            ".....oddddo.....",
            "......oddo......",
            ".......dd.......",
            "........d.......",
            "................"};

    private static final String[] SEMENTES = {
            "................",
            "................",
            "................",
            "................",
            ".....oo.........",
            "....ohmo...oo...",
            "....omdo..ohmo..",
            ".....oo...omdo..",
            "...oo......oo...",
            "..ohmo..........",
            "..omdo...oo.....",
            "...oo...ohmo....",
            "........omdo....",
            ".........oo.....",
            "................",
            "................"};

    private static void plantacoes(List<Arte> l) {
        l.add(new Arte("wheat", "variedade_trigo_dourado", TRIGO,
                c('h', 0xFFFFF0A0, 'm', 0xFFFFC93C, 'd', 0xFFB8860B, 's', 0xFFC8A030, 'o', 0xFF5A3A00, 'a', 0xFFC0392B)));
        l.add(new Arte("carrot", "variedade_cenoura_cristalina", CENOURA,
                c('o', 0xFF1F5A54, 'd', 0xFF2FB0A8, 'm', 0xFF7FE3D4, 'h', 0xFFE8FFFB, 'g', 0xFF4FD1A5)));
        l.add(new Arte("potato", "variedade_batata_ancestral", BATATA,
                c('o', 0xFF3A2410, 'd', 0xFF6B4423, 'm', 0xFF9C7048, 'h', 0xFFC8A070, 'a', 0xFFFFB300)));
        l.add(new Arte("beetroot", "variedade_beterraba_rubi", BETERRABA,
                c('o', 0xFF4A0020, 'd', 0xFF900030, 'm', 0xFFE0115F, 'h', 0xFFFF8FB1, 'a', BRANCO, 'g', 0xFF3E9E3E)));
        l.add(new Arte("wheat_seeds", "semente_trigo_dourado", SEMENTES,
                c('o', 0xFF5A3A00, 'd', 0xFFB8860B, 'm', 0xFFFFC93C, 'h', 0xFFFFF0A0)));
        l.add(new Arte("beetroot_seeds", "semente_beterraba_rubi", SEMENTES,
                c('o', 0xFF4A0020, 'd', 0xFF900030, 'm', 0xFFE0115F, 'h', 0xFFFF8FB1)));
    }

    // =====================================================================
    //  Componentes alquímicos
    // =====================================================================

    private static final String[] PILHA_PO = {
            "................",
            "................",
            "................",
            "....a......a....",
            "................",
            ".......a........",
            "..a..........a..",
            "................",
            "......oooo......",
            "....oohmhmoo....",
            "..oohmmmhmmmoo..",
            ".ohmmmmmmmmmmmo.",
            ".oddmmmmmmmmddo.",
            "..oooooooooooo..",
            "................",
            "................"};

    private static final String[] FRASCO_PEQUENO = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "......oooo......",
            "......occo......",
            "......oooo......",
            ".....ogllwo.....",
            "....ogllllwo....",
            "....ollalllo....",
            "....ollllllo....",
            "....odlllldo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] GOTA = {
            "................",
            "................",
            "................",
            ".......oo.......",
            "......ohlo......",
            ".....ohlllo.....",
            "....ohlllllo....",
            "...ohlllllldo...",
            "...ollllllldo...",
            "...ollllllldo...",
            "....odlllldo....",
            ".....oddddo.....",
            "......oooo......",
            "................",
            "................",
            "................"};

    private static final String[] CRISTAIS_SAL = {
            "................",
            "................",
            "................",
            "................",
            "...........a....",
            ".....oooo.......",
            ".....ohho...a...",
            ".....omdo..oooo.",
            "..oooodoo..ohho.",
            "..ohho....omdo..",
            "..omdo....oooo..",
            "..oooo....a.....",
            "........a.......",
            "................",
            "................",
            "................"};

    private static final String[] METAL_LIQUIDO = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".....oooo.......",
            "...oohhmmoo.....",
            "..ohhmmmmmdo....",
            "..ommmmmmmdo.oo.",
            "...odddddoo.ohdo",
            "....ooooo...ooo.",
            "......oo........",
            ".....ohdo.......",
            "......oo........",
            "................",
            "................"};

    private static final String[] CRISTAL = {
            "................",
            ".......oo.......",
            "......ohwo......",
            "..a..ohhwmo..a..",
            ".....ohwmmo.....",
            "....ohhwmmdo....",
            "....ohwmmmdo....",
            "....ohwmmmdo....",
            "....ohwmmmdo....",
            "....ohwmmddo....",
            ".....ohmmdo.....",
            ".....ohmddo.....",
            "......omdo......",
            ".......oo.......",
            "................",
            "................"};

    private static final String[] PEROLA = {
            "................",
            "................",
            "................",
            "................",
            "......oooo......",
            "....oohhmmoo....",
            "...ohwhmmmmdo...",
            "..ohwhmmmmmmdo..",
            "..ohhmmmmmmmdo..",
            "..ommmmmmmmddo..",
            "..ommmmmmmdddo..",
            "...odmmmmdddo...",
            "....oodddddoo...",
            "......oooo......",
            "................",
            "................"};

    private static final String[] ESCAMA = {
            "................",
            "................",
            "................",
            "......oooo......",
            "....oohhmmoo....",
            "...ohhmmmmmdo...",
            "..ohmmammammdo..",
            "..ommmmmmmmmdo..",
            "..omammammamdo..",
            "...ommmmmmmdo...",
            "...odmammamdo...",
            "....odmmmmdo....",
            ".....oddddo.....",
            "......oddo......",
            ".......oo.......",
            "................"};

    private static final Map<Character, Integer> VIDRO = c('o', 0xFF2A2A2A, 'c', 0xFF8A5A2A, 'g', 0xFFB0C8D0, 'w', BRANCO);

    private static void componentes(List<Arte> l) {
        l.add(new Arte("glowstone_dust", "componente_po_arcano", PILHA_PO,
                c('o', 0xFF3A1A5A, 'd', 0xFF7A3AB0, 'm', 0xFFC77DFF, 'h', 0xFFE8C8FF, 'a', BRANCO)));
        l.add(new Arte("honey_bottle", "componente_oleo_de_peixe", FRASCO_PEQUENO,
                com(VIDRO, 'l', 0xFFE8C15A, 'd', 0xFFA88420, 'a', 0xFFFFF0A0)));
        l.add(new Arte("red_dye", "componente_tintura_vital", GOTA,
                c('o', 0xFF5A0020, 'h', 0xFFFF8FA8, 'l', 0xFFE0115F, 'd', 0xFF900030)));
        l.add(new Arte("sugar", "componente_sal_lunar", CRISTAIS_SAL,
                c('o', 0xFF4A5070, 'h', BRANCO, 'm', 0xFFDDE4FF, 'd', 0xFFA8B0D8, 'a', 0xFFFFF5B0)));
        l.add(new Arte("iron_nugget", "componente_mercurio_vivo", METAL_LIQUIDO,
                c('o', 0xFF3A3A48, 'h', BRANCO, 'm', 0xFFC8C8D8, 'd', 0xFF8A8A9A)));
        l.add(new Arte("prismarine_crystals", "componente_cristal_de_mana", CRISTAL,
                c('o', 0xFF1A4A6A, 'w', BRANCO, 'h', 0xFFB8ECFF, 'm', 0xFF7FD7FF, 'd', 0xFF3A8AC0, 'a', 0xFFE0F8FF)));
        l.add(new Arte("experience_bottle", "componente_solvente", FRASCO_PEQUENO,
                com(VIDRO, 'l', 0xFF9ACD32, 'd', 0xFF5A8A10, 'a', 0xFFE0FFA0)));
        l.add(new Arte("ender_pearl", "componente_perola_negra", PEROLA,
                c('o', 0xFF0A0614, 'd', 0xFF1E1430, 'm', 0xFF3B2E5A, 'h', 0xFF6A5A90, 'w', BRANCO)));
        l.add(new Arte("turtle_scute", "componente_escama_do_abismo", ESCAMA,
                c('o', 0xFF0E3A32, 'd', 0xFF1E6A5A, 'm', 0xFF2E8B7A, 'h', 0xFF7FD0C0, 'a', 0xFF1E6A5A)));
        // Óleos de lâmina: frasco fino pingando.
        l.add(new Arte("honey_bottle", "componente_oleo_de_fogo", FRASCO_OLEO,
                com(VIDRO, 'l', 0xFFFF7043, 'd', 0xFFB0301A, 'a', 0xFFFFD180)));
        l.add(new Arte("experience_bottle", "componente_oleo_gelido", FRASCO_OLEO,
                com(VIDRO, 'l', 0xFF81D4FA, 'd', 0xFF2A7AB0, 'a', BRANCO)));
        l.add(new Arte("experience_bottle", "componente_oleo_trovejante", FRASCO_OLEO,
                com(VIDRO, 'l', 0xFFFFF176, 'd', 0xFFC0A020, 'a', BRANCO)));
        l.add(new Arte("dragon_breath", "componente_oleo_venenoso", FRASCO_OLEO,
                com(VIDRO, 'l', 0xFF8BC34A, 'd', 0xFF3A6A1A, 'a', 0xFFD8FFA0)));
        l.add(new Arte("ominous_bottle", "componente_oleo_de_prata", FRASCO_OLEO,
                com(VIDRO, 'l', 0xFFD8D8E0, 'd', 0xFF8A8A9A, 'a', BRANCO)));
    }

    private static final String[] FRASCO_OLEO = {
            "................",
            "......oooo......",
            "......occo......",
            "......oooo......",
            "......ogwo......",
            "......ogwo......",
            ".....ogllwo.....",
            ".....oalllo.....",
            ".....ollllo.....",
            ".....ollllo.....",
            ".....odlldo.....",
            "......oooo......",
            ".........l......",
            ".........d......",
            "................",
            "................"};

    // =====================================================================
    //  Elixires
    // =====================================================================

    private static final String[] GARRAFA = {
            "................",
            "......oooo......",
            "......occo......",
            "......oooo......",
            "......ogwo......",
            ".....ogllwo.....",
            "....ogllllwo....",
            "...ogllllllwo...",
            "...olllllllwo...",
            "...ollalllllo...",
            "...olllllallo...",
            "...odlllllldo...",
            "....odddddddo...",
            ".....ooooooo....",
            "................",
            "................"};

    private static final String[] FRASCO_ALTO = {
            ".....oooooo.....",
            ".....occcco.....",
            ".....oooooo.....",
            ".....oglgwo.....",
            ".....ogllwo.....",
            ".....ollllo.....",
            ".....ollalo.....",
            ".....ollllo.....",
            ".....olalwo.....",
            ".....ollllo.....",
            ".....ollllo.....",
            ".....odlllo.....",
            ".....oddddo.....",
            "......oooo......",
            "................",
            "................"};

    private static final String[] FRASCO_CONICO = {
            "................",
            "......oooo......",
            "......occo......",
            "......oooo......",
            "......ogwo......",
            "......ogwo......",
            ".....ogllwo.....",
            "....ogllllwo....",
            "....ollalllo....",
            "...ogllllllwo...",
            "...olllllallo...",
            "..ollllllllllo..",
            "..oddddddddddo..",
            "..oooooooooooo..",
            "................",
            "................"};

    private static Arte elixir(String id, String[] forma, int liquido, int escuro, int bolha) {
        return new Arte("potion", "elixir_" + id, forma, com(VIDRO, 'l', liquido, 'd', escuro, 'a', bolha));
    }

    private static void elixires(List<Arte> l) {
        l.add(elixir("cura", GARRAFA, 0xFFFF4D6D, 0xFFB0203A, 0xFFFFB0C0));
        l.add(elixir("rapidez", FRASCO_ALTO, 0xFF7FD7FF, 0xFF2A8AC0, 0xFFE0F8FF));
        l.add(elixir("minerador", FRASCO_CONICO, 0xFFFFC93C, 0xFFB8860B, 0xFFFFF0A0));
        l.add(elixir("mergulhador", GARRAFA, 0xFF2E86DE, 0xFF1A4A8A, 0xFFA0D0FF));
        l.add(elixir("sorte", FRASCO_CONICO, 0xFF5BD15B, 0xFF2A8A2A, 0xFFC0FFC0));
        l.add(elixir("pedra", GARRAFA, 0xFF8C8C8C, 0xFF4A4A4A, 0xFFC8C8C8));
        l.add(elixir("sombras", FRASCO_ALTO, 0xFF4B3B6B, 0xFF2A1A40, 0xFF9A8AC0));
        l.add(elixir("mana", FRASCO_CONICO, 0xFF7F8CFF, 0xFF3A48C0, 0xFFD0D8FF));
        l.add(elixir("gigante", GARRAFA, 0xFFD35400, 0xFF8A3000, 0xFFFFB070));
        l.add(elixir("fenix", FRASCO_ALTO, 0xFFFF7A1A, 0xFFC03A00, 0xFFFFF04D));
        l.add(elixir("tita", FRASCO_CONICO, 0xFFFFD23F, 0xFFC09000, BRANCO));
        l.add(elixir("purificacao", FRASCO_ALTO, 0xFFFFFFE0, 0xFFD8D8A0, BRANCO));
    }

    // =====================================================================
    //  Gemas: bruta (pedra com cristais), lapidada e perfeita (com brilhos)
    // =====================================================================

    private static final String[] GEMA_BRUTA = {
            "................",
            "................",
            "................",
            ".....ooooo......",
            "...oopphmoo.....",
            "..oppphmmdpo....",
            ".oppqpmmdppqo...",
            ".opphmpqppppqo..",
            ".ophmmdpphmpqo..",
            ".oqpmdppphmmdo..",
            "..oqpppqpmddo...",
            "...oqqpppqqo....",
            ".....ooooo......",
            "................",
            "................",
            "................"};

    private static final String[] GEMA_LAPIDADA = {
            "................",
            "................",
            "................",
            "......oooo......",
            "....oohhmmoo....",
            "...ohhwhmmmdo...",
            "..ohhhhmmmmddo..",
            "..ommmmmmmmmdo..",
            "..odmmmmmmmddo..",
            "...odmmmmmddo...",
            "....oddmmddo....",
            ".....oddddo.....",
            "......oddo......",
            ".......oo.......",
            "................",
            "................"};

    private static final String[] GEMA_PERFEITA = {
            "................",
            "..a..........a..",
            "................",
            ".....oooooo.....",
            "....ohwhhmmo....",
            "...ohwhhhmmmo...",
            "..ohhhhhmmmmdo..",
            ".oommmmmmmmmmoo.",
            "..odmmhmmmmddo..",
            "...odmmmmmddo...",
            "....odmmmddo....",
            ".....odmddo.....",
            "......oddo......",
            "..a....oo....a..",
            "................",
            "................"};

    private static final Map<Character, Integer> PEDRA = c('p', 0xFF8A8A8A, 'q', 0xFF5A5A5A, 'w', BRANCO);

    private static void gema(List<Arte> l, String id, String vanilla, int contorno, int escuro, int meio, int claro, int brilho) {
        Map<Character, Integer> cores = com(PEDRA, 'o', contorno, 'd', escuro, 'm', meio, 'h', claro, 'a', brilho);
        l.add(new Arte(vanilla, "gema_" + id + "_1", GEMA_BRUTA, cores));
        l.add(new Arte(vanilla, "gema_" + id + "_2", GEMA_LAPIDADA, cores));
        l.add(new Arte(vanilla, "gema_" + id + "_3", GEMA_PERFEITA, cores));
    }

    private static void gemas(List<Arte> l) {
        gema(l, "rubi", "redstone", 0xFF4A0010, 0xFF9A0A28, 0xFFE0115F, 0xFFFF7A9A, 0xFFFFD0DC);
        gema(l, "safira", "lapis_lazuli", 0xFF0A1A4A, 0xFF1A3A9A, 0xFF2E6FE0, 0xFF8AB8FF, 0xFFD0E4FF);
        gema(l, "esmeralda", "emerald", 0xFF043A1A, 0xFF0A7A3A, 0xFF1FC46A, 0xFF8AF0B8, 0xFFD0FFE4);
        gema(l, "topazio", "gold_nugget", 0xFF4A2A00, 0xFFB06A00, 0xFFFFB300, 0xFFFFE08A, 0xFFFFF4D0);
        gema(l, "ametista", "amethyst_shard", 0xFF2A0A4A, 0xFF5A2A9A, 0xFF9A5AE0, 0xFFD0A8FF, 0xFFF0E0FF);
        gema(l, "onix", "coal", 0xFF000000, 0xFF141414, 0xFF2E2E36, 0xFF6A6A78, 0xFFB0B0C0);
    }

    // =====================================================================
    //  Materiais raros e núcleos dos chefes
    // =====================================================================

    private static final String[] FRAGMENTO = {
            "................",
            "................",
            "................",
            ".......oo.......",
            "......ohmo......",
            ".....ohmamo.....",
            "....ohmaamdo....",
            "...ohmmammddo...",
            "..ohmmmaammdo...",
            "..ommamaamddo...",
            "...odmamddo.....",
            "....oddmdo......",
            ".....oddo.......",
            "......oo........",
            "................",
            "................"};

    private static final String[] PEDRA_RUNA = {
            "................",
            "................",
            "................",
            ".....oooooo.....",
            "....ohhmmmmo....",
            "...ohmmmmmmdo...",
            "..ohmmmammmmdo..",
            "..ommamamammdo..",
            "..ommmaaammmdo..",
            "..ommmmammmmdo..",
            "..odmmmammmddo..",
            "...oddmmmmddo...",
            "....oddddddo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] ORBE = {
            "................",
            "...s.......s....",
            "......oooo......",
            "....oowhabbo....",
            "...owhaabbcco...",
            "..owhaabbcccbo..",
            "..ohaabbcccbbo..",
            "..oaabbcccbbao..",
            "..oabbcccbbaao..",
            "..obbcccbbaaho..",
            "...obcccbbaao...",
            "....occbbaao....",
            ".....oooooo.....",
            "....s.......s...",
            "................",
            "................"};

    private static final String[] MAPA = {
            "................",
            "................",
            "................",
            "..oooooooooooo..",
            "..ohhhhhhhhhmo..",
            "..ohmmmmmmmmmo..",
            "..ohmkkmmxmxmo..",
            "..ohmmmkmmxmmo..",
            "..ohmmmmkxmxmo..",
            "..ohmkmmmmmmo...",
            "..ohmmkkmmmo....",
            "..odmmmmmo.o....",
            "..oddddoo.......",
            "..ooo...........",
            "................",
            "................"};

    private static final String[] PAGINA = {
            "................",
            "...oooooooooo...",
            "...obbbbbbbbo...",
            "...obwwwwwwbo...",
            "...obwkkkkwbo...",
            "...obwwwwwwbo...",
            "...obwkkkwwbo...",
            "...obwwaawwbo...",
            "...obwaccawbo...",
            "...obwwaawwbo...",
            "...obwkkkkwbo...",
            "...obwwwwwwbo...",
            "...obwkkwwwbo...",
            "...obbbbbbbbo...",
            "...oooooooooo...",
            "................"};

    private static final String[] NUCLEO_FERRO = {
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...ohhhhhhhmo...",
            "...ohrmmmmrmo...",
            "...ohmmaammdo...",
            "...ohmaccamdo...",
            "...ohmaccamdo...",
            "...ohmmaammdo...",
            "...ohrmmmmrdo...",
            "...oddddddddo...",
            "....oooooooo....",
            "................",
            "................",
            "................"};

    private static final String[] PRESA = {
            "................",
            "................",
            "......oooo......",
            ".....ohhhmo.....",
            ".....ohhmmo.....",
            ".....ohmmmo.....",
            "......ohmmo.....",
            "......ohmmo.....",
            ".......ohmo.....",
            ".......ohmo.....",
            "........omo.....",
            "........oao.....",
            ".........a......",
            "........aaa.....",
            ".........a......",
            "................"};

    private static final String[] BRASA = {
            "................",
            "........f.......",
            ".......fyf......",
            "......fyyyf.....",
            ".....oooooo.....",
            "...oodmmmmdoo...",
            "..odmmammmmmdo..",
            "..omaaammaammo..",
            "..ommaccaaamdo..",
            "..odmaccaammdo..",
            "..oddmaammmddo..",
            "...oddmmmmddo...",
            ".....oooooo.....",
            "................",
            "................",
            "................"};

    private static final String[] FILACTERIO = {
            "................",
            "................",
            "......oooo......",
            ".....owwwwo.....",
            "....owoaaowo....",
            "...owoaccaowo...",
            "...owaccbcawo...",
            "...owacbccawo...",
            "...owoaccaowo...",
            "....owoaaowo....",
            ".....owwwwo.....",
            "......oooo......",
            ".......ww.......",
            "......w..w......",
            "................",
            "................"};

    private static final String[] OLHO = {
            "................",
            "..........y.....",
            ".........y......",
            "........yy......",
            "......oooo......",
            "....oowwwwoo....",
            "...owwwccwwwo...",
            "..owwwcaacwwwo..",
            "..owwwcakcwwwo..",
            "...owwwccwwwo...",
            "....oowwwwoo....",
            "......oooo......",
            "......yy........",
            ".....y..........",
            "....y...........",
            "................"};

    private static final String[] COROA = {
            "................",
            "................",
            "................",
            "................",
            "..a....a....a...",
            "..o...ooo...o...",
            "..oo..ogo..oo...",
            "..ogo.ogo.ogo...",
            "..oggoggoggo....",
            "..ogggggggggo...",
            "..ogpgbgpgbgo...",
            "..ogggggggggo...",
            "..ooooooooooo...",
            "................",
            "................",
            "................"};

    private static void raros(List<Arte> l) {
        l.add(new Arte("netherite_scrap", "raro_fragmento_de_forja", FRAGMENTO,
                c('o', 0xFF2A1A10, 'h', 0xFF8D6E63, 'm', 0xFF4E342E, 'd', 0xFF2E1B12, 'a', 0xFFFF9800)));
        l.add(new Arte("prismarine_crystals", "raro_pedra_de_protecao", PEDRA_RUNA,
                c('o', 0xFF1A2A3A, 'h', 0xFF9FB8CF, 'm', 0xFF5C7C99, 'd', 0xFF34495E, 'a', 0xFF64FFDA)));
        l.add(new Arte("heart_of_the_sea", "raro_essencia_primordial", ORBE,
                c('o', 0xFF1A1A2E, 'w', BRANCO, 'h', 0xFFE0F7FA, 'a', 0xFF4DD0E1, 'b', 0xFFE040FB, 'c', 0xFFFFD740, 's', BRANCO)));
        l.add(new Arte("paper", "raro_mapa_rasgado", MAPA,
                c('o', 0xFF4E342E, 'h', 0xFFF5E6C8, 'm', 0xFFE0C9A6, 'd', 0xFFB8A07A, 'k', 0xFF8D6E63, 'x', 0xFFD32F2F)));
        l.add(new Arte("paper", "raro_pagina_de_lenda", PAGINA,
                c('o', 0xFF3A1020, 'b', 0xFFFF5FD7, 'w', 0xFFFFF8E1, 'k', 0xFFBCAAA4, 'a', 0xFFFF5FD7, 'c', 0xFFFFD54F)));
        l.add(new Arte("iron_nugget", "raro_nucleo_golem", NUCLEO_FERRO,
                c('o', 0xFF2E2E36, 'h', 0xFFE0E0E0, 'm', 0xFFA8A8B4, 'd', 0xFF6E6E7A, 'r', 0xFF4A4A54, 'a', 0xFFFF8F00, 'c', 0xFFFFE082)));
        l.add(new Arte("disc_fragment_5", "raro_nucleo_rainha", PRESA,
                c('o', 0xFF3A2A4A, 'h', BRANCO, 'm', 0xFFE8E0D0, 'a', 0xFF76FF03)));
        l.add(new Arte("nether_brick", "raro_nucleo_chamas", BRASA,
                c('o', 0xFF1A0A00, 'd', 0xFF3E2723, 'm', 0xFF5D4037, 'a', 0xFFFF6D00, 'c', 0xFFFFEA00, 'f', 0xFFFF3D00, 'y', 0xFFFFC400)));
        l.add(new Arte("nautilus_shell", "raro_nucleo_lich", FILACTERIO,
                c('o', 0xFF2A2030, 'w', 0xFFEDE7D9, 'a', 0xFF7E57C2, 'c', 0xFFB39DDB, 'b', BRANCO)));
        l.add(new Arte("prismarine_shard", "raro_nucleo_tempestade", OLHO,
                c('o', 0xFF263238, 'w', 0xFFECEFF1, 'c', 0xFF26C6DA, 'a', 0xFF80DEEA, 'k', 0xFF102027, 'y', 0xFFFFEB3B)));
        l.add(new Arte("echo_shard", "raro_nucleo_arauto", COROA,
                c('o', 0xFF2A1A3A, 'g', 0xFFFFC107, 'a', 0xFFC77DFF, 'p', 0xFF7C4DFF, 'b', 0xFF40C4FF)));
    }

    // =====================================================================
    //  Acessórios
    // =====================================================================

    private static final String[] ANEL = {
            "................",
            "................",
            "................",
            "......oaao......",
            ".....oahaao.....",
            "....oogaagoo....",
            "...ogo....ogo...",
            "..ogo......ogo..",
            "..og........go..",
            "..og........go..",
            "..ogo......ogo..",
            "...ogo....ogo...",
            "....ooggggoo....",
            "......oooo......",
            "................",
            "................"};

    private static final String[] AMULETO = {
            "................",
            "..c..........c..",
            "...c........c...",
            "....c......c....",
            ".....c....c.....",
            "......c..c......",
            ".......oo.......",
            ".....oggggo.....",
            "....ogaaaago....",
            "...ogaahaaago...",
            "...ogaaaaaago...",
            "....ogaaaago....",
            ".....oggggo.....",
            "......oooo......",
            "................",
            "................"};

    private static final String[] CINTO = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "ooooooggggoooooo",
            "lllllogbbgolllll",
            "lhlhlogbbgolhlhl",
            "lllllogbbgolllll",
            "ooooooggggoooooo",
            "................",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] LANTERNA = {
            "................",
            "................",
            ".......oo.......",
            "......o..o......",
            "....oooooooo....",
            "....ommmmmmo....",
            "....ogyyyyho....",
            "....ogyffyho....",
            "....ogyffyho....",
            "....ogyyyyho....",
            "....ommmmmmo....",
            "....oooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] IMA = {
            "................",
            "..a..........a..",
            "...oooo..oooo...",
            "...owwo..owwo...",
            "...owwo..owwo...",
            "...orro..orro...",
            "...orro..orro...",
            "...orro..orro...",
            "...orroooorro...",
            "...orrrrrrrro...",
            "....orrrrrro....",
            ".....oooooo.....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] RELOGIO = {
            "................",
            "................",
            ".......oo.......",
            "......oggo......",
            ".....oooooo.....",
            "....oggggggo....",
            "...ogwwwwwwgo...",
            "...ogwwkwwwgo...",
            "...ogwwkkkwgo...",
            "...ogwwwwwwgo...",
            "...ogwwwwwwgo...",
            "....oggggggo....",
            ".....oooooo.....",
            "................",
            "................",
            "................"};

    private static final int OURO = 0xFFFFC107, FERRO = 0xFFB0BEC5;

    private static void anel(List<Arte> l, String id, String vanilla, int metal, int gema, int brilho) {
        l.add(new Arte(vanilla, id, ANEL, c('o', metal == OURO ? 0xFF3E2723 : 0xFF37474F, 'g', metal, 'a', gema, 'h', brilho)));
    }

    private static void amuleto(List<Arte> l, String id, String vanilla, int gema, int brilho) {
        l.add(new Arte(vanilla, id, AMULETO, c('o', 0xFF3E2723, 'c', 0xFFFFD54F, 'g', OURO, 'a', gema, 'h', brilho)));
    }

    private static void cinto(List<Arte> l, String id, String vanilla, int couro, int costura) {
        l.add(new Arte(vanilla, id, CINTO, c('o', 0xFF212121, 'l', couro, 'h', costura, 'g', OURO, 'b', 0xFF3E2723)));
    }

    private static void acessorios(List<Arte> l) {
        anel(l, "anel_vigor", "gold_nugget", OURO, 0xFFE0115F, 0xFFFF8FB1);
        anel(l, "anel_forca", "iron_nugget", FERRO, 0xFF5A5A6E, 0xFF9E9E9E);
        anel(l, "anel_vento", "gold_nugget", OURO, 0xFFB2EBF2, BRANCO);
        anel(l, "anel_mares", "nautilus_shell", OURO, 0xFF2E86DE, 0xFFA0D0FF);
        anel(l, "anel_brasas", "blaze_powder", OURO, 0xFFFF6A2B, 0xFFFFCC80);
        anel(l, "anel_minerador", "amethyst_shard", FERRO, 0xFFB66BFF, 0xFFE1BEE7);
        anel(l, "anel_fortuna", "emerald", OURO, 0xFF2ECC71, 0xFFB9F6CA);
        anel(l, "anel_sabio", "prismarine_crystals", OURO, 0xFF7FD7FF, 0xFFE1F5FE);
        amuleto(l, "amuleto_mana", "heart_of_the_sea", 0xFF7F8CFF, 0xFFD0D8FF);
        amuleto(l, "amuleto_guardiao", "prismarine_shard", 0xFF3D6BFF, 0xFFA0C4FF);
        amuleto(l, "amuleto_vida", "golden_apple", 0xFFFF4D6D, 0xFFFFB0C0);
        amuleto(l, "amuleto_fenix", "totem_of_undying", 0xFFFF7A1A, 0xFFFFF04D);
        cinto(l, "cinto_atleta", "lead", 0xFF43A047, 0xFFA5D6A7);
        cinto(l, "cinto_andarilho", "leather", 0xFF8D6E63, 0xFFD7CCC8);
        cinto(l, "cinto_tita", "iron_ingot", 0xFF616161, 0xFFBDBDBD);
        l.add(new Arte("lantern", "lanterna_bolso", LANTERNA,
                c('o', 0xFF212121, 'm', 0xFF455A64, 'g', 0xFF607D8B, 'y', 0xFFFFE082, 'f', 0xFFFF9800, 'h', 0xFFFFF8E1)));
        l.add(new Arte("lodestone", "ima_bolso", IMA, c('o', 0xFF212121, 'w', 0xFFE0E0E0, 'r', 0xFFE53935, 'a', 0xFF90CAF9)));
        l.add(new Arte("gold_ingot", "relogio_bolso", RELOGIO, c('o', 0xFF3E2723, 'g', OURO, 'w', 0xFFFFF8E1, 'k', 0xFF212121)));
    }

    // =====================================================================
    //  Lendas (armas e ferramentas; arco, escudo e armaduras ficam com o visual do jogo)
    // =====================================================================

    private static final String[] ESPADA = {
            "..............oo",
            ".............oho",
            "..........a.ohmo",
            "...........ohmo.",
            "..........ohmo..",
            ".........ohmo.a.",
            "........ohmo....",
            "....a..ohmo.....",
            "......ohmo......",
            "...o.ohmo.......",
            "...ogohmo.......",
            "....oggo........",
            "...owoggo.......",
            "..owo..oo.......",
            ".opo............",
            "..o............."};

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

    private static final String[] MARTELO = {
            "................",
            ".........oooooo.",
            ".........ohhhmo.",
            "........oohmmmdo",
            "........ohmamddo",
            "........ommmmddo",
            "........oodddoo.",
            ".......owo.oo...",
            "......owo.......",
            ".....owo........",
            "....owo.........",
            "...owo..........",
            "..owo...........",
            ".opo............",
            "..o.............",
            "................"};

    /** Itens do jogo terminados em {@code sufixo} (ex.: "_SWORD"), como nomes de item. */
    private static List<String> itens(String... sufixos) {
        List<String> l = new ArrayList<>();
        for (org.bukkit.Material m : org.bukkit.Material.values()) {
            String n = m.name();
            if (n.startsWith("LEGACY_")) continue;
            for (String s : sufixos) {
                if (n.endsWith(s) && !(s.equals("_AXE") && n.endsWith("_PICKAXE"))) l.add(n.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return l;
    }

    private static void lenda(List<Arte> l, String id, String[] desenho, Map<Character, Integer> cores, List<String> vanillas) {
        for (String v : vanillas) l.add(new Arte(v, "lenda_" + id, desenho, cores, true));
    }

    private static void lendas(List<Arte> l) {
        List<String> espadas = itens("_SWORD"), machados = itens("_AXE"), picaretas = itens("_PICKAXE");
        List<String> espadasEMachados = new ArrayList<>(espadas);
        espadasEMachados.addAll(machados);
        List<String> martelo = new ArrayList<>(machados);
        martelo.add("mace");
        lenda(l, "lamina_do_exorcista", ESPADA, c('o', 0xFF3A3A48, 'h', BRANCO, 'm', 0xFFE0E6F0, 'g', 0xFFFFD54F,
                'w', 0xFF5D4037, 'p', 0xFFFFD54F, 'a', 0xFFFFF59D), espadasEMachados);
        lenda(l, "presa_da_tecela", ESPADA, c('o', 0xFF1A0A2A, 'h', 0xFFB39DDB, 'm', 0xFF5E35B1, 'g', 0xFF4A148C,
                'w', 0xFF212121, 'p', 0xFF7CB342, 'a', 0xFF9CCC65), espadas);
        lenda(l, "machado_do_lenhador", MACHADO, c('o', 0xFF1B2A10, 'h', 0xFFC5E1A5, 'm', 0xFF7CB342, 'd', 0xFF33691E,
                'w', 0xFF5D4037, 'a', 0xFF2E7D32), machados);
        lenda(l, "picareta_do_abismo", PICARETA, c('o', 0xFF0A0614, 'h', 0xFF6A5AB0, 'm', 0xFF2E1F5A, 'd', 0xFF140F2E,
                'w', 0xFF3E2723, 'a', 0xFF00E5FF), picaretas);
        lenda(l, "lamina_do_carrasco", ESPADA, c('o', 0xFF1A0000, 'h', 0xFFB71C1C, 'm', 0xFF5A0A0A, 'g', 0xFF212121,
                'w', 0xFF3E2723, 'p', 0xFF8A0A0A, 'a', 0xFFD50000), espadasEMachados);
        lenda(l, "martelo_do_forjador", MARTELO, c('o', 0xFF3E2000, 'h', 0xFFFFF59D, 'm', 0xFFFFC107, 'd', 0xFFB06A00,
                'w', 0xFF5D4037, 'p', 0xFFFF6F00, 'a', 0xFFFF3D00), martelo);
        lenda(l, "lamina_do_arquimago", ESPADA, c('o', 0xFF0D1A4A, 'h', 0xFFB3E5FC, 'm', 0xFF5C6BC0, 'g', 0xFFCE93D8,
                'w', 0xFF311B92, 'p', 0xFF7C4DFF, 'a', 0xFF80D8FF), espadas);
    }

    // =====================================================================
    //  Vida no mundo: bebidas, cartas, frascos, flechas, méis e ervas
    // =====================================================================

    private static final String[] JARRA = {
            "................",
            "................",
            ".....oooooo.....",
            ".....occcco.....",
            "....oooooooo....",
            "....ogllllwo....",
            "...ogllllllwo...",
            "...ollllllllo...",
            "...ollallllwo...",
            "...ollllllalo...",
            "...odlllllldo...",
            "....oddddddo....",
            ".....oooooo.....",
            "................",
            "................",
            "................"};

    private static final String[] GARRAFA_VINHO = {
            ".......oo.......",
            "......occo......",
            "......oooo......",
            "......ogwo......",
            "......ogwo......",
            "......ogwo......",
            ".....ogllwo.....",
            "....ogllllwo....",
            "....ollllllo....",
            "....ollaallo....",
            "....oaaaaaao....",
            "....ollllllo....",
            "....odlllldo....",
            "....oddddddo....",
            ".....oooooo.....",
            "................"};

    private static final String[] CANECA = {
            "................",
            "................",
            "................",
            "...whwwhww......",
            "..wwwwwwwwww....",
            "..oooooooooo....",
            "..ollllllllo.oo.",
            "..olallllllloo.o",
            "..ollllllllo..o.",
            "..olllllallo..o.",
            "..ollllllllooo..",
            "..oddddddddo....",
            "..oooooooooo....",
            "................",
            "................",
            "................"};

    private static final String[] CARTA = {
            "................",
            "...oooooooooo...",
            "...obbbbbbbbo...",
            "...obffffffbo...",
            "...obfaaaafbo...",
            "...obfaccafbo...",
            "...obfaccafbo...",
            "...obfaaaafbo...",
            "...obffffffbo...",
            "...obwwwwwwbo...",
            "...obwkkkkwbo...",
            "...obwwwwwwbo...",
            "...obwkkkwwbo...",
            "...obbbbbbbbo...",
            "...oooooooooo...",
            "................"};

    private static final String[] FLECHA = {
            "................",
            "..........a..a..",
            "...........hhh..",
            "..........hhhh..",
            ".........shhh...",
            "........s.hh....",
            ".......s........",
            "......s.........",
            ".....s..........",
            "....s...........",
            ".ffs............",
            "ffsf............",
            ".sff............",
            "s.f.............",
            "................",
            "................"};

    private static final String[] RAMO = {
            "................",
            "................",
            "..........ll....",
            ".........lhl....",
            "....ll..lhll....",
            "...lhll.lll.ll..",
            "...llll.s..lhl..",
            "....ll.s..llll..",
            "......s....ll...",
            ".....s.ll.......",
            "....s.lhll......",
            "...s..lll.......",
            "..s.............",
            ".s..............",
            "................",
            "................"};

    private static final String[] FLOR = {
            "................",
            "................",
            "......pppp......",
            "....pphpphpp....",
            "...ppppccpppp...",
            "...pphpcapphp...",
            "....pppppppp....",
            "......pppp......",
            ".......ss.......",
            ".......s..ll....",
            "....ll.s.lkl....",
            "...lkl.s.ll.....",
            "....ll.s........",
            ".......s........",
            ".......s........",
            "................"};

    private static final String[] TUFO = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....a....a......",
            "......a.......a.",
            "...mmm..mm......",
            "..mhmmmmhmm.mm..",
            ".mmmdmmmmmmdmhm.",
            ".mdmmmdmmhmmmmd.",
            "..dddddddddddd..",
            "................",
            "................"};

    private static final String[] FOLHA = {
            "................",
            "................",
            "...........oo...",
            ".........oohho..",
            ".......oohhmmo..",
            "......ohhmmmmo..",
            ".....ohmmmvmmo..",
            "....ohmmmvmmdo..",
            "....ommmvmmmdo..",
            "...ohmmvmmmdo...",
            "...ommvmmddo....",
            "...omvmddoo.....",
            "..ovvddoo.......",
            ".vo.oo..........",
            "v...............",
            "................"};

    private static final String[] RAIZ = {
            "................",
            "................",
            ".......oo.......",
            "......omdo......",
            "......odmo......",
            ".......omdo.....",
            "......omdo......",
            ".....omdmdo.....",
            "....omdo.odo....",
            "...omao...omo...",
            "..omdo.....odo..",
            "..odo.......oao.",
            "...a.........o..",
            "................",
            "................",
            "................"};

    private static final String[] FRUTA_REDONDA = {
            "................",
            "................",
            "................",
            ".........g......",
            ".......sgg......",
            ".....oooooo.....",
            "....ohhmmmmo....",
            "...ohhmmmmmdo...",
            "...ohmmmmmmdo...",
            "...ommmmmmmdo...",
            "...ommmmmmddo...",
            "...odmmmmmddo...",
            "....oddmmddo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] CEREJAS = {
            "................",
            "................",
            "..........gg....",
            ".........sg.....",
            "........s.s.....",
            ".......s...s....",
            "......s....s....",
            ".....s.....s....",
            "...ooo....ooo...",
            "..ohmmo..ohmmo..",
            "..ommmo..ommmo..",
            "..odmdo..odmdo..",
            "...ooo....ooo...",
            "................",
            "................",
            "................"};

    private static void frutas(List<Arte> l) {
        Map<Character, Integer> galho = c('s', 0xFF5D4037, 'g', 0xFF4CAF50);
        l.add(new Arte("apple", "fruta_laranja", FRUTA_REDONDA, com(galho, 'o', 0xFF8A3B00, 'd', 0xFFE65100, 'm', 0xFFFF9800, 'h', 0xFFFFCC80)));
        l.add(new Arte("apple", "fruta_pessego", FRUTA_REDONDA, com(galho, 'o', 0xFF8A3B2E, 'd', 0xFFE57373, 'm', 0xFFFFAB91, 'h', 0xFFFFE0D0)));
        l.add(new Arte("apple", "fruta_cereja", CEREJAS, com(galho, 'o', 0xFF4A0010, 'd', 0xFF880E4F, 'm', 0xFFD81B60, 'h', 0xFFF48FB1)));
        l.add(new Arte("apple", "fruta_limao", FRUTA_REDONDA, com(galho, 'o', 0xFF5A6A00, 'd', 0xFFAFB42B, 'm', 0xFFCDDC39, 'h', 0xFFF0F4C3)));
        l.add(new Arte("apple", "fruta_maca_dourada", FRUTA_REDONDA, com(galho, 'o', 0xFF7A5A00, 'd', 0xFFFFA000, 'm', 0xFFFFD54F, 'h', 0xFFFFF59D)));
        l.add(new Arte("potion", "bebida_licor_de_frutas", GARRAFA_VINHO, com(VIDRO, 'l', 0xFFFF8A65, 'd', 0xFFBF360C, 'a', 0xFFFFF3E0)));
    }

    private static void vida(List<Arte> l) {
        Map<Character, Integer> madeira = c('o', 0xFF3E2723, 'c', 0xFFC62828, 'g', 0xFFB0C8D0, 'w', BRANCO);
        l.add(new Arte("potion", "bebida_hidromel", JARRA, com(madeira, 'l', 0xFFE8A33D, 'd', 0xFFB06A00, 'a', 0xFFFFE082)));
        l.add(new Arte("potion", "bebida_cidra", GARRAFA, com(VIDRO, 'l', 0xFFE57373, 'd', 0xFFB71C1C, 'a', 0xFFFFCDD2)));
        l.add(new Arte("potion", "bebida_vinho", GARRAFA_VINHO, com(VIDRO, 'l', 0xFF8E244D, 'd', 0xFF4A0E28, 'a', 0xFFF5E6C8)));
        l.add(new Arte("potion", "bebida_cerveja", CANECA,
                c('o', 0xFF4E342E, 'w', BRANCO, 'h', 0xFFE0E0E0, 'l', 0xFFD4A017, 'd', 0xFF8D6E00, 'a', 0xFFFFF59D)));
        l.add(new Arte("potion", "bebida_licor_arcano", FRASCO_CONICO, com(VIDRO, 'l', 0xFF9575CD, 'd', 0xFF512DA8, 'a', 0xFFE1BEE7)));
        l.add(new Arte("potion", "bebida_licor_de_ervas", FRASCO_ALTO, com(VIDRO, 'l', 0xFF7CB342, 'd', 0xFF33691E, 'a', 0xFFDCEDC8)));
        Map<Character, Integer> papel = c('o', 0xFF212121, 'w', 0xFFF5F5F5, 'k', 0xFF9E9E9E);
        l.add(new Arte("paper", "carta_comum", CARTA, com(papel, 'b', 0xFF9E9E9E, 'f', 0xFFE0E0E0, 'a', 0xFF78909C, 'c', 0xFFECEFF1)));
        l.add(new Arte("paper", "carta_rara", CARTA, com(papel, 'b', 0xFF1E88E5, 'f', 0xFFBBDEFB, 'a', 0xFF42A5F5, 'c', BRANCO)));
        l.add(new Arte("paper", "carta_epica", CARTA, com(papel, 'b', 0xFF8E24AA, 'f', 0xFFE1BEE7, 'a', 0xFFAB47BC, 'c', 0xFFFFD54F)));
        l.add(new Arte("paper", "carta_brilhante", CARTA, com(papel, 'b', 0xFFFFB300, 'f', 0xFFFFF59D, 'a', 0xFFFFCA28, 'c', BRANCO)));
        l.add(new Arte("snowball", "frasco_fumaca", GARRAFA, com(VIDRO, 'c', 0xFFEEEEEE, 'l', 0xFF9E9E9E, 'd', 0xFF616161, 'a', 0xFFE0E0E0)));
        l.add(new Arte("snowball", "frasco_cola", GARRAFA, com(VIDRO, 'c', 0xFF8D6E63, 'l', 0xFF9CCC65, 'd', 0xFF558B2F, 'a', 0xFFDCEDC8)));
        l.add(new Arte("snowball", "frasco_fogo_grego", GARRAFA, com(VIDRO, 'c', 0xFFE0E0E0, 'l', 0xFFFF7043, 'd', 0xFFBF360C, 'a', 0xFFFFEB3B)));
        l.add(new Arte("snowball", "frasco_cura", GARRAFA, com(VIDRO, 'c', BRANCO, 'l', 0xFFF06292, 'd', 0xFFAD1457, 'a', 0xFFFCE4EC)));
        Map<Character, Integer> haste = c('s', 0xFF8D6E63);
        l.add(new Arte("arrow", "flecha_fogo", FLECHA, com(haste, 'h', 0xFFFF7043, 'f', 0xFFFFCC80, 'a', 0xFFFFC107)));
        l.add(new Arte("arrow", "flecha_gelo", FLECHA, com(haste, 'h', 0xFF81D4FA, 'f', 0xFFE1F5FE, 'a', BRANCO)));
        l.add(new Arte("arrow", "flecha_rastreadora", FLECHA, com(haste, 'h', 0xFFCE93D8, 'f', 0xFF7B1FA2, 'a', 0xFFE1BEE7)));
        l.add(new Arte("arrow", "flecha_corda", FLECHA, com(haste, 'h', 0xFF9E9E9E, 'f', 0xFFD7CCC8, 'a', 0xFFA1887F)));
        l.add(new Arte("arrow", "flecha_explosiva", FLECHA, com(haste, 'h', 0xFFE53935, 'f', BRANCO, 'a', 0xFFFFEB3B)));
        Map<Character, Integer> pote = c('o', 0xFF3E2723, 'c', 0xFF8D6E63, 'g', 0xFFB0C8D0, 'w', BRANCO);
        l.add(new Arte("honey_bottle", "mel_silvestre", JARRA, com(pote, 'l', 0xFFE8A33D, 'd', 0xFFB06A00, 'a', 0xFFFFE082)));
        l.add(new Arte("honey_bottle", "mel_dourado", JARRA, com(pote, 'l', 0xFFFFD54F, 'd', 0xFFC79100, 'a', BRANCO)));
        l.add(new Arte("honey_bottle", "mel_gelado", JARRA, com(pote, 'l', 0xFFB3E5FC, 'd', 0xFF4FA3D1, 'a', BRANCO)));
        l.add(new Arte("honey_bottle", "mel_noturno", JARRA, com(pote, 'l', 0xFF5C6BC0, 'd', 0xFF283593, 'a', 0xFFC5CAE9)));
        l.add(new Arte("honey_bottle", "mel_floral", JARRA, com(pote, 'l', 0xFFF48FB1, 'd', 0xFFC2185B, 'a', 0xFFFCE4EC)));
        l.add(new Arte("oak_sapling", "erva_salvia", RAMO, c('l', 0xFF7CB342, 'h', 0xFFAED581, 's', 0xFF558B2F)));
        l.add(new Arte("dandelion", "erva_erva_de_sol", FLOR,
                c('p', 0xFFFFC107, 'h', 0xFFFFE082, 'c', 0xFFFF6F00, 'a', 0xFFFFAB40, 's', 0xFF558B2F, 'l', 0xFF7CB342, 'k', 0xFFAED581)));
        l.add(new Arte("glow_lichen", "erva_musgo_lunar", TUFO, c('m', 0xFF7986CB, 'h', 0xFFC5CAE9, 'd', 0xFF3949AB, 'a', 0xFFB3E5FC)));
        l.add(new Arte("blue_orchid", "erva_folha_gelida", FOLHA,
                c('o', 0xFF1E5A78, 'm', 0xFF81D4FA, 'h', 0xFFE1F5FE, 'd', 0xFF4FA3D1, 'v', BRANCO)));
        l.add(new Arte("hanging_roots", "erva_raiz_abissal", RAIZ, c('o', 0xFF2A1A10, 'm', 0xFF6D4C41, 'd', 0xFF4E342E, 'a', 0xFFB388FF)));
        l.add(new Arte("azure_bluet", "erva_flor_de_cristal", FLOR,
                c('p', 0xFFF8BBD0, 'h', BRANCO, 'c', 0xFFEC407A, 'a', 0xFFF48FB1, 's', 0xFF26A69A, 'l', 0xFF4DB6AC, 'k', 0xFFB2DFDB)));
    }
    static List<Arte> todas() {
        List<Arte> l = new ArrayList<>();
        peixes(l);
        pratos(l);
        plantacoes(l);
        componentes(l);
        elixires(l);
        gemas(l);
        raros(l);
        acessorios(l);
        lendas(l);
        vida(l);
        frutas(l);
        oficios(l);
        estacoesVivas(l);
        return l;
    }

    // =====================================================================
    //  Curtume e tecelagem (2.30)
    // =====================================================================

    private static final String[] PELE_ESTICADA = {
            "................",
            "...oo......oo...",
            "..ommo....ommo..",
            "..ommmooooommo..",
            "...ommmmmmmmo...",
            "...ommhmmmmmo...",
            "..ommhhmmmmmmo..",
            "..ommmmmmmmdmo..",
            "..ommmmmmmmdmo..",
            "..ommmmmmmddmo..",
            "...ommmmmmdmo...",
            "...ommmmmmmmo...",
            "..ommo....ommo..",
            "..ommo....ommo..",
            "...oo......oo...",
            "................"};

    private static final String[] COURO_COSTURADO = {
            "................",
            "................",
            "..oooooooooooo..",
            "..ohhhhhhhhhho..",
            "..ohwmwmwmwmdo..",
            "..ohmmmmmmmmdo..",
            "..ohmmmmmmmmdo..",
            "..ohmmmmmmmmdo..",
            "..ohmmmmmmmmdo..",
            "..ohmmmmmmmmdo..",
            "..ohwmwmwmwmdo..",
            "..oddddddddddo..",
            "..oooooooooooo..",
            "................",
            "................",
            "................"};

    private static final String[] TECIDO_DOBRADO = {
            "................",
            "................",
            "................",
            "...oooooooooo...",
            "..ohhhhhhhhhho..",
            "..ommmmmmmmmmo..",
            "..oddddddddddo..",
            "..ohhhhhhhhhho..",
            "..ommmmmmmmmmo..",
            "..oddddddddddo..",
            "..ohhhhhhhhhho..",
            "..ommmmmmmmmmo..",
            "..ommmmmmmmmmo..",
            "...oooooooooo...",
            "................",
            "................"};

    private static Map<Character, Integer> pele(int o, int m, int h, int d) {
        return c('o', o, 'm', m, 'h', h, 'd', d);
    }

    private static Map<Character, Integer> couro(int o, int m, int h, int d, int w) {
        return c('o', o, 'm', m, 'h', h, 'd', d, 'w', w);
    }

    private static void oficios(List<Arte> l) {
        l.add(new Arte("rabbit_hide", "oficio_pele_de_lobo", PELE_ESTICADA, pele(0xFF3A3A3A, 0xFF8A8A8A, 0xFFBDBDBD, 0xFF5E5E5E)));
        l.add(new Arte("rabbit_hide", "oficio_pele_de_urso", PELE_ESTICADA, pele(0xFF7A7A70, 0xFFF2F2F2, BRANCO, 0xFFCFCFC4)));
        l.add(new Arte("rabbit_hide", "oficio_pele_de_raposa", PELE_ESTICADA, pele(0xFF5A2A0A, 0xFFE07B2E, 0xFFFFC58A, 0xFFB0541A)));
        l.add(new Arte("rabbit_hide", "oficio_couro_de_hoglin", PELE_ESTICADA, pele(0xFF3A1A10, 0xFFA0523A, 0xFFD08A6A, 0xFF6A3020)));
        l.add(new Arte("prismarine_shard", "oficio_escama_de_guardiao", ESCAMA,
                c('o', 0xFF1E4A40, 'd', 0xFF3E7A6A, 'm', 0xFF5FA89A, 'h', 0xFFA8E0D0, 'a', 0xFFE07B2E)));
        l.add(new Arte("leather", "oficio_couro_lobo", COURO_COSTURADO, couro(0xFF2A2420, 0xFF6D6460, 0xFF9A908A, 0xFF4A4240, 0xFFD8D0C0)));
        l.add(new Arte("leather", "oficio_couro_urso", COURO_COSTURADO, couro(0xFF6A665E, 0xFFE6E1D6, BRANCO, 0xFFB8B2A6, 0xFF8A7A60)));
        l.add(new Arte("leather", "oficio_couro_raposa", COURO_COSTURADO, couro(0xFF4A2008, 0xFFC9682A, 0xFFE8955A, 0xFF8A4418, 0xFFFFE0B0)));
        l.add(new Arte("leather", "oficio_couro_escamas", COURO_COSTURADO, couro(0xFF123A36, 0xFF3E8C82, 0xFF6FC0B4, 0xFF26605A, 0xFFE07B2E)));
        l.add(new Arte("leather", "oficio_couro_brasa", COURO_COSTURADO, couro(0xFF2A0A06, 0xFF8B2E22, 0xFFC0503A, 0xFF5A1A12, 0xFFFFB300)));
        l.add(new Arte("leather", "oficio_couro_noturno", COURO_COSTURADO, couro(0xFF100A1C, 0xFF3B2E5A, 0xFF6A5A90, 0xFF241C3A, 0xFFB39DDB)));
        l.add(new Arte("paper", "oficio_tecido", TECIDO_DOBRADO, pele(0xFF7A7060, 0xFFEDE6D6, BRANCO, 0xFFC8BEA8)));
        l.add(new Arte("paper", "oficio_feltro", TECIDO_DOBRADO, pele(0xFF4A3A2A, 0xFFB7A089, 0xFFD8C4AC, 0xFF8A7460)));
        l.add(new Arte("paper", "oficio_tecido_de_alga", TECIDO_DOBRADO, pele(0xFF1A2A10, 0xFF4E7A3A, 0xFF7AA860, 0xFF2E5020)));
    }

    // =====================================================================
    //  Estações vivas (2.31): coletas, colheitas, sementes e as fases das plantas
    // =====================================================================

    private static final String[] COGUMELO = {
            "................",
            "................",
            "................",
            "......oooo......",
            ".....occhco.....",
            "....ochcccco....",
            "...occcchccco...",
            "...ochcccccho...",
            "...occccchcco...",
            "....oooooooo....",
            "......osdo......",
            "......osdo......",
            "......osdo......",
            ".....ossddo.....",
            ".....oooooo.....",
            "................"};

    private static final String[] BULBO = {
            "................",
            ".......g.g......",
            "......gkg.g.....",
            ".......gkgk.....",
            "........gk......",
            ".......oko......",
            "......ommmo.....",
            ".....ohmmmdo....",
            "....ohmmmmmdo...",
            "....ohmmmmmdo...",
            "....ommmmmmdo...",
            ".....ommmmdo....",
            "......oddoo.....",
            ".......oo.......",
            "................",
            "................"};

    private static final String[] BROTO = {
            "................",
            "................",
            "......kkkk......",
            ".....kgggghk....",
            "....kgkkkkghk...",
            "....kgk..kgk....",
            "....kgk.kgk.....",
            ".....kgggk......",
            "......kgk.......",
            "......kgk.......",
            ".......kgk......",
            ".......kgk......",
            "......kgk.......",
            "......kgk.......",
            ".......k........",
            "................"};

    private static final String[] BAGAS = {
            "................",
            "................",
            "........k.......",
            "........kg......",
            ".......k.gg.....",
            ".....oo..oo.....",
            "....ohmoohmo....",
            "....omdoomdo....",
            ".....oooooo.....",
            "......ohmo......",
            "......omdo......",
            ".......oo.......",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] ALGA = {
            "................",
            ".......k........",
            "......kgk.......",
            "......kgk.......",
            ".......kgk......",
            ".......kgk......",
            "......kgk.......",
            "......kghk......",
            ".......kgk......",
            "........kgk.....",
            "........kgk.....",
            ".......kgk......",
            "......kgk.......",
            "......kgk.......",
            ".......k........",
            "................"};

    private static final String[] NOZ = {
            "................",
            "................",
            "................",
            "......oooo......",
            ".....occcco.....",
            "....occcccco....",
            "....oooooooo....",
            "....ohmmmmdo....",
            "....ohmmmmdo....",
            "....ommmmmdo....",
            ".....ommmdo.....",
            "......oddo......",
            ".......oo.......",
            "................",
            "................",
            "................"};

    private static final String[] RAIZ_INVERNO = {
            "................",
            "......g.g.......",
            ".....gkgk.......",
            "......kgk.......",
            ".....oooo.......",
            "....ohmmdo......",
            "....ommmdo......",
            ".....ommdo......",
            ".....ommdo......",
            "......omdo......",
            "......omdo......",
            ".......odo......",
            ".......od.......",
            "........o.......",
            "................",
            "................"};

    private static final String[] MORANGUINHO = {
            "................",
            "................",
            ".....g.kg.g.....",
            "......gkkg......",
            "....oooggooo....",
            "...ohmmmmmmmo...",
            "...ohammammdo...",
            "...ommmmammdo...",
            "....omammmdo....",
            "....ommmamdo....",
            ".....ommmdo.....",
            ".....omamdo.....",
            "......omdo......",
            ".......oo.......",
            "................",
            "................"};

    private static final String[] PIMENTINHA = {
            "................",
            "..........kg....",
            ".........kg.....",
            "........oko.....",
            ".......ohmo.....",
            "......ohmmo.....",
            "......ommdo.....",
            ".....ohmmdo.....",
            ".....ommdo......",
            "....ohmmdo......",
            "....ommdo.......",
            "...ohmdo........",
            "...omdo.........",
            "..oodo..........",
            "..oo............",
            "................"};

    private static final String[] GOMOS = {
            "................",
            "................",
            ".......kg.......",
            "........k.......",
            "....oooooooo....",
            "...ohmdmmdmmo...",
            "..ohmmdmmdmmdo..",
            "..ohmmdmmdmmdo..",
            "..ommmdmmdmmdo..",
            "..ommmdmmdmmdo..",
            "...ommdmmdmdo...",
            "....oooooooo....",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] FOLHOSA = {
            "................",
            "................",
            "................",
            ".....kk..kk.....",
            "....kggkkggk....",
            "...kgghggghgk...",
            "..kgggwwwwgggk..",
            "..kggwwhwwwggk..",
            "..kgggwwwwgggk..",
            "...kggggggggk...",
            "....kkggggkk....",
            "......kkkk......",
            "................",
            "................",
            "................",
            "................"};

    private static final String[] PACOTE_SEMENTE = {
            "................",
            "...oooooooooo...",
            "...owwwwwwwwo...",
            "...oaaaaaaaao...",
            "...owwwwwwwwo...",
            "...owwwmmwwwo...",
            "...owwmmmmwwo...",
            "...owwmhmmwwo...",
            "...owwwmmwwwo...",
            "...owwwwwwwwo...",
            "...oaaaaaaaao...",
            "...owwwwwwwwo...",
            "...owgwgwgwwo...",
            "...owwwwwwwwo...",
            "...oooooooooo...",
            "................"};

    /** As 4 fases de uma planta (o chão fica na última linha). */
    /** Fruto redondo com cabinho (tomate, cebola, rabanete...). */
    private static final String[] REDONDO = {
            "................",
            "................",
            "......kgkg......",
            ".......gk.......",
            ".....oogkoo.....",
            "....ohhmmmmo....",
            "...ohhmmmmmdo...",
            "...ohmmmmmmdo...",
            "...ommmmmmmdo...",
            "...ommmmmmmdo...",
            "...ommmmmmddo...",
            "....ommmmddo....",
            ".....oooooo.....",
            "................",
            "................",
            "................"};

    /** Espiga de milho na palha. */
    private static final String[] ESPIGA = {
            "................",
            "........o.......",
            ".......ohm......",
            "......ohmmo.....",
            "......ohmdo.....",
            ".....gohmmog....",
            ".....gomdmog....",
            "....kgohmmogk...",
            "....kgomdmogk...",
            "....kgohmmogk...",
            ".....kgommogk...",
            "......kgoogk....",
            ".......kggk.....",
            "........kk......",
            "................",
            "................"};

    /** Vagem (feijão-verde, berinjela comprida). */
    private static final String[] VAGEM = {
            "................",
            "............kg..",
            "...........ok...",
            "..........ohmo..",
            ".........ohmdo..",
            "........ohmdo...",
            ".......ohmmo....",
            "......ohmdo.....",
            ".....ohmmo......",
            "....ohmdo.......",
            "...ohmmo........",
            "...omdo.........",
            "....oo..........",
            "................",
            "................",
            "................"};

    private static final String[][] FASES = {
            {"................", "................", "................", "................", "................", "................",
                    "................", "................", "................", "................", "................", "................",
                    "......h..h......", ".......gg.......", ".......kg.......", ".......kk......."},
            {"................", "................", "................", "................", "................", "................",
                    "................", "................", "................", ".....h....h.....", "....hgg..ggh....", ".....gkggkg.....",
                    "......gkkg......", "...h...kg...h...", "...ggk.kg.kgg...", "......kkkk......"},
            {"................", "................", "................", "................", "......h..h......", ".....hgb.gbh....",
                    "....hggkgkggh...", "...b.gkggkg.b...", "..hgg.gkkg.ggh..", "..gkgg.kg.ggkg..", "...gkg.kg.gkg...", "..b.gkgkgkg.b...",
                    "...hggkkkkggh...", "....gkg.kgkg....", ".....gk.kgk.....", "......kkkk......"},
            {"................", "................", ".....h....h.....", "....hgm..mgh....", "...hgmdgkgmdh...", "..am.gkggkg.ma..",
                    "..md.gkggkg.dm..", "...hgg.kg.ggh...", "..gkgma.kamgkg..", "..gkgdd.kddgkg..", "...gkg.kg.gkg...", "..am.gkgkgk.ma..",
                    "..mdhggkkkgghdm.", "....gkg.kgkg....", ".....gk.kgk.....", "......kkkk......"}};

    private static int escurecer(int argb, double f) {
        int r = (int) (((argb >> 16) & 255) * f), g = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clarear(int argb, double f) {
        int r = ((argb >> 16) & 255), g = ((argb >> 8) & 255), b = (argb & 255);
        r += (int) ((255 - r) * f);
        g += (int) ((255 - g) * f);
        b += (int) ((255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Paleta de fruto/legume a partir de uma cor: contorno, meio, brilho, sombra e destaque. */
    private static Map<Character, Integer> fruto(int cor, int destaque) {
        int c = 0xFF000000 | cor;
        return c('o', escurecer(c, 0.35), 'm', c, 'h', clarear(c, 0.45), 'd', escurecer(c, 0.7), 'a', destaque,
                'g', 0xFF4E8A2A, 'k', 0xFF2E5A1E, 'c', escurecer(c, 0.6), 's', 0xFFEDE6D6, 'w', BRANCO);
    }

    private static Map<Character, Integer> folha(int g, int k, int h) {
        return c('g', g, 'k', k, 'h', h);
    }

    private static void estacoesVivas(List<Arte> l) {
        // Coletas (todas em cima da maçã, porque se comem).
        l.add(new Arte("apple", "coleta_morel", COGUMELO, c('o', 0xFF3A2A18, 'c', 0xFFB08850, 'h', 0xFF6A4A28, 's', 0xFFEDE0C0, 'd', 0xFFC8B890)));
        l.add(new Arte("apple", "coleta_alho_selvagem", BULBO, c('o', 0xFF6A6458, 'm', 0xFFF2EEDD, 'h', BRANCO, 'd', 0xFFC8C0A8, 'g', 0xFF7CB342, 'k', 0xFF3E6A20)));
        l.add(new Arte("apple", "coleta_broto_de_samambaia", BROTO, folha(0xFF7CB342, 0xFF2E5A1E, 0xFFB5E07A)));
        l.add(new Arte("apple", "coleta_amora_silvestre", BAGAS, fruto(0x4A148C, 0xFFFFFFFF)));
        l.add(new Arte("apple", "coleta_alga_doce", ALGA, folha(0xFF2E7D32, 0xFF143A18, 0xFF8BC34A)));
        l.add(new Arte("apple", "coleta_groselha", BAGAS, fruto(0xE53935, 0xFFFFFFFF)));
        l.add(new Arte("apple", "coleta_castanha", NOZ, fruto(0x6D4C41, 0xFFFFFFFF)));
        l.add(new Arte("apple", "coleta_cogumelo_do_bosque", COGUMELO, c('o', 0xFF4A2410, 'c', 0xFFC0702A, 'h', 0xFFF2D6A0, 's', 0xFFEDE6D6, 'd', 0xFFC8BEA8)));
        l.add(new Arte("apple", "coleta_avela", NOZ, fruto(0xA1887F, 0xFFFFFFFF)));
        l.add(new Arte("apple", "coleta_raiz_de_inverno", RAIZ_INVERNO, fruto(0x8D6E63, 0xFFFFFFFF)));
        l.add(new Arte("apple", "coleta_cogumelo_de_neve", COGUMELO, c('o', 0xFF5A6A7A, 'c', 0xFFE3F2FD, 'h', 0xFF9FC8E8, 's', BRANCO, 'd', 0xFFC8D8E0)));
        l.add(new Arte("apple", "coleta_fruto_de_zimbro", BAGAS, fruto(0x3949AB, 0xFFFFFFFF)));

        // Colheitas, sementes (pacotinho com a cor da planta) e as 4 fases de cada planta.
        Object[][] cultivos = {
                {"morango", MORANGUINHO, 0xE53935, 0xFFFFE082, 0xFFFFFFFF},
                {"couve_flor", FOLHOSA, 0xF5F0E0, 0xFFFFFFFF, 0xFFFFFFFF},
                {"mirtilo", BAGAS, 0x3F51B5, 0xFFFFFFFF, 0xFFE8EAF6},
                {"pimenta", PIMENTINHA, 0xD32F2F, 0xFFFFFFFF, 0xFFFFFFFF},
                {"uva", BAGAS, 0x7B1FA2, 0xFFFFFFFF, 0xFFF3E5F5},
                {"abobora_moranga", GOMOS, 0xEF6C00, 0xFFFFFFFF, 0xFFFFE0B2},
                {"couve_gelada", FOLHOSA, 0x5E9C7A, 0xFFFFFFFF, 0xFFE0F2F1},
                {"nabo_de_neve", BULBO, 0xEDE7F6, 0xFFFFFFFF, 0xFFFFFFFF},
                // 2.33: mais 12 (5 por estação)
                {"pastinaca", RAIZ_INVERNO, 0xF3E5AB, 0xFFFFFFFF, 0xFFFFFFFF},
                {"alho", BULBO, 0xF8F4E8, 0xFFFFFFFF, 0xFFFFFFFF},
                {"feijao_verde", VAGEM, 0x7CB342, 0xFFFFFFFF, 0xFFDCEDC8},
                {"tomate", REDONDO, 0xE53935, 0xFFFFFFFF, 0xFFFFCDD2},
                {"milho", ESPIGA, 0xFBC02D, 0xFFFFFFFF, 0xFFFFF59D},
                {"melao", GOMOS, 0x8BC34A, 0xFFFFFFFF, 0xFFF1F8E9},
                {"berinjela", VAGEM, 0x6A1B9A, 0xFFFFFFFF, 0xFFE1BEE7},
                {"inhame", RAIZ_INVERNO, 0x8D6E63, 0xFFFFFFFF, 0xFFFFFFFF},
                {"oxicoco", BAGAS, 0xB71C1C, 0xFFFFFFFF, 0xFFFFCDD2},
                {"cebola", REDONDO, 0xAD5A8C, 0xFFFFFFFF, 0xFFF8BBD0},
                {"rabanete_gelado", REDONDO, 0xEF5350, 0xFFFFFFFF, 0xFFFFFFFF},
                {"alho_poro", BROTO, 0xC5E1A5, 0xFFFFFFFF, 0xFFFFFFFF}};
        for (Object[] cv : cultivos) {
            String id = (String) cv[0];
            int cor = (int) cv[2], destaque = (int) cv[3], botao = (int) cv[4];
            Map<Character, Integer> pal = new java.util.HashMap<>(fruto(cor, destaque));
            if (id.equals("couve_flor")) pal.putAll(c('w', 0xFFF5F0E0, 'h', BRANCO));
            if (id.equals("couve_gelada")) pal.putAll(c('g', 0xFF5E9C7A, 'k', 0xFF2E5A44, 'h', 0xFFB2DFDB, 'w', 0xFF9CCFB8));
            if (id.equals("nabo_de_neve")) pal.putAll(c('o', 0xFF6A5A80, 'd', 0xFFB39DDB));
            l.add(new Arte("apple", "colheita_" + id, (String[]) cv[1], pal));
            l.add(new Arte("wheat_seeds", "semente_" + id, PACOTE_SEMENTE, c('o', 0xFF5A4A30, 'w', 0xFFF2E6C8, 'a', 0xFF000000 | cor,
                    'm', 0xFF000000 | cor, 'h', clarear(0xFF000000 | cor, 0.5), 'g', 0xFF8D6E63)));
            int mCor = 0xFF000000 | cor;
            Map<Character, Integer> fase = c('k', 0xFF2E5A1E, 'g', 0xFF4E8A2A, 'h', 0xFF8BC34A, 'b', botao,
                    'm', mCor, 'd', escurecer(mCor, 0.7), 'a', clarear(mCor, 0.45));
            for (int f = 0; f < 4; f++) l.add(new Arte("short_grass", "planta_" + id + "_" + f, FASES[f], fase));
        }
        // 2.33: produtos dos animais.
        l.add(new Arte("clay_ball", "produto_trufa", NOZ, c('o', 0xFF2A1A10, 'm', 0xFF5D4037, 'h', 0xFF8D6E63, 'd', 0xFF3E2723, 'a', 0xFFBCAAA4,
                'g', 0xFF4E8A2A, 'k', 0xFF2E5A1E, 'c', 0xFF4E342E, 's', 0xFFD7CCC8, 'w', 0xFFEFEBE9)));
        // A planta murcha (fora de época): a fase 2 em tons de palha seca.
        l.add(new Arte("dead_bush", "planta_murcha", FASES[2], c('k', 0xFF5A4022, 'g', 0xFF8A6A3A, 'h', 0xFFB89A5E, 'b', 0xFF6A5030,
                'm', 0xFF7A5A30, 'd', 0xFF4A3218, 'a', 0xFFA08050)));
    }
}
