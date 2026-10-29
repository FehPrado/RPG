import java.io.File;
import java.util.*;

/**
 * Gera os moldes (.nbt) das estruturas grandes da 3.1 para o datapack do RPGAtributos.
 * Uso: java GerarEstruturas <pasta datapack/data/rpgatributos/structure> <pasta prévias>
 * O ponto importante de cada uma (câmara do chefe, coração, sala do trono, arena) fica no centro do
 * molde: o jogo gira a estrutura ao acaso, e o plugin só sabe a caixa dela.
 */
public class GerarEstruturas {

    static final String L = "rpgatributos:chests/";

    public static void main(String[] a) throws Exception {
        File dir = new File(a[0]), prev = new File(a[1]);
        Map<String, Molde> todos = new LinkedHashMap<>();
        todos.put("arvore_mundo", arvore());
        todos.put("colosso_caido", colosso());
        todos.put("cidade_ana", cidadeAna());
        todos.put("ossada_dragao", ossada());
        todos.put("coliseu", coliseu());
        todos.put("castelo_rei_caido", castelo());
        for (var e : todos.entrySet()) {
            e.getValue().gravar(new File(dir, e.getKey() + ".nbt"));
            Previa.salvar(e.getValue(), new File(prev, e.getKey() + ".png"));
            System.out.println(e.getKey() + ": " + e.getValue().sx + "x" + e.getValue().sy + "x" + e.getValue().sz + ", " + e.getValue().contar() + " blocos");
        }
    }

    static String slabB(String m) { return m + "_slab[type=bottom,waterlogged=false]"; }

    static String escada(String m, String facing) { return m + "_stairs[facing=" + facing + ",half=bottom,shape=straight,waterlogged=false]"; }

    /** Escada em caracol em volta de (cx,cz): meio bloco por degrau, {@code porVolta} degraus por volta. */
    static void caracol(Molde m, double cx, double cz, double r0, double r1, int y0, int y1, int porVolta, String laje, String cheio, double fase) {
        int passos = (y1 - y0) * 2;
        for (int i = 0; i <= passos; i++) {
            double a = fase + i * 2 * Math.PI / porVolta;
            int nivel = y0 + i / 2;
            for (double rr = r0; rr <= r1; rr += 0.35) {
                for (double da = -0.06; da <= 0.06; da += 0.06) {
                    int x = (int) Math.round(cx + Math.cos(a + da) * rr), z = (int) Math.round(cz + Math.sin(a + da) * rr);
                    m.set(x, nivel, z, i % 2 == 0 ? laje : cheio);
                    for (int h = 1; h <= 3; h++) if (m.get(x, nivel + h, z) != null && !m.get(x, nivel + h, z).contains("slab") && !m.get(x, nivel + h, z).contains("planks") && !m.get(x, nivel + h, z).contains("tiles")) m.set(x, nivel + h, z, "air");
                }
            }
        }
    }

    // =====================================================================
    //  Árvore-Mundo: raízes com a câmara do Guardião, tronco oco com escada, copa com vila
    // =====================================================================

    static Molde arvore() {
        Molde m = new Molde(61, 121, 61, 101);
        int c = 30;
        Molde.Pal casca = (x, y, z) -> m.mix(0.7, "oak_wood[axis=y]", "dark_oak_wood[axis=y]", "spruce_wood[axis=y]");
        Molde.Pal terra = (x, y, z) -> m.mix(0.5, "rooted_dirt", "dirt", "coarse_dirt", "moss_block");
        // Câmara das raízes (lá embaixo, uns 18 blocos abaixo do chão).
        m.elipsoide(c, 7, c, 13, 9.5, 13, 0.2, terra);
        m.elipsoide(c, 7, c, 10.5, 7, 10.5, 0.1, (x, y, z) -> "air");
        for (int x = 0; x < m.sx; x++)
            for (int z = 0; z < m.sz; z++)
                if (Math.hypot(x - c, z - c) <= 10.5) {
                    m.set(x, 0, z, "moss_block");
                    m.set(x, 1, z, Math.hypot(x - c, z - c) > 9 ? "moss_block" : (m.r.nextDouble() < 0.25 ? "moss_carpet" : "air"));
                }
        // Raízes que descem pela câmara.
        for (int i = 0; i < 7; i++) {
            double ang = i * 2 * Math.PI / 7 + 0.3;
            double rx = c + Math.cos(ang) * 8, rz = c + Math.sin(ang) * 8;
            m.linha(c + Math.cos(ang) * 5, 15, c + Math.sin(ang) * 5, rx, 7, rz, 1.3, casca);
            m.linha(rx, 7, rz, c + Math.cos(ang) * 10, 1, c + Math.sin(ang) * 10, 1.1, casca);
        }
        // Luz: cogumelos-luz e raízes penduradas.
        for (int i = 0; i < 14; i++) {
            double ang = m.r.nextDouble() * Math.PI * 2;
            int x = (int) Math.round(c + Math.cos(ang) * 10.6), z = (int) Math.round(c + Math.sin(ang) * 10.6);
            m.set(x, 2 + m.r.nextInt(7), z, "shroomlight");
        }
        for (int x = c - 10; x <= c + 10; x++)
            for (int z = c - 10; z <= c + 10; z++)
                for (int y = 3; y < 17; y++)
                    if ("minecraft:air".equals(m.get(x, y, z)) && m.get(x, y + 1, z) != null && !"minecraft:air".equals(m.get(x, y + 1, z))
                            && !m.get(x, y + 1, z).contains("roots") && m.r.nextDouble() < 0.18) m.set(x, y, z, "hanging_roots[waterlogged=false]");
        // Tronco: de dentro da câmara até a copa; oco, com uma coluna no meio e escada em caracol.
        m.cilindro(c, c, 8, 10, 96, casca);
        m.cilindro(c, c, 5.6, 2, 95, (x, y, z) -> "air");
        m.cilindro(c, c, 1.5, 1, 100, (x, y, z) -> "oak_log[axis=y]");
        caracol(m, c, c, 2.2, 5.3, 2, 88, 30, slabB("oak"), "oak_planks", 0);
        // Janelas no tronco e luz na parede.
        for (int y = 20; y < 86; y += 11) {
            double ang = y * 0.37;
            int x = (int) Math.round(c + Math.cos(ang) * 7.5), z = (int) Math.round(c + Math.sin(ang) * 7.5);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy < 3; dy++) {
                if (Math.hypot(x + dx - c, z + dz - c) > 5.6) m.set(x + dx, y + dy, z + dz, "air");
            }
            m.set((int) Math.round(c + Math.cos(ang + 1.6) * 6.2), y + 1, (int) Math.round(c + Math.sin(ang + 1.6) * 6.2), "shroomlight");
        }
        // Raízes de fora: arcos saindo da base e mergulhando na terra.
        for (int i = 0; i < 9; i++) {
            double ang = i * 2 * Math.PI / 9 + 0.15;
            double mx = c + Math.cos(ang) * 14, mz = c + Math.sin(ang) * 14;
            m.linha(c + Math.cos(ang) * 7, 22, c + Math.sin(ang) * 7, mx, 21, mz, 2.0, casca);
            m.linha(mx, 21, mz, c + Math.cos(ang) * 25, 11, c + Math.sin(ang) * 25, 1.5, casca);
        }
        // Galhos e copa.
        for (int i = 0; i < 8; i++) {
            double ang = i * 2 * Math.PI / 8 + 0.4;
            int y0 = 78 + (i % 3) * 5;
            double ex = c + Math.cos(ang) * 24, ez = c + Math.sin(ang) * 24;
            m.linha(c + Math.cos(ang) * 6, y0, c + Math.sin(ang) * 6, c + Math.cos(ang) * 15, y0 + 6, c + Math.sin(ang) * 15, 1.8, casca);
            m.linha(c + Math.cos(ang) * 15, y0 + 6, c + Math.sin(ang) * 15, ex, y0 + 13, ez, 1.1, casca);
        }
        m.cilindro(c, c, 5.5, 96, 104, casca);
        Molde.Pal folhas = (x, y, z) -> m.mix(0.6, "oak_leaves[distance=7,persistent=true,waterlogged=false]",
                "azalea_leaves[distance=7,persistent=true,waterlogged=false]", "flowering_azalea_leaves[distance=7,persistent=true,waterlogged=false]");
        Molde.Pal folhasSeVazio = (x, y, z) -> { String a = m.get(x, y, z); return a == null || a.equals("minecraft:air") ? folhas.at(x, y, z) : a; };
        m.elipsoide(c, 106, c, 21, 10, 21, 0.35, folhasSeVazio);
        for (int i = 0; i < 8; i++) {
            double ang = i * 2 * Math.PI / 8 + 0.4;
            int y0 = 78 + (i % 3) * 5;
            m.elipsoide(c + Math.cos(ang) * 23, y0 + 14, c + Math.sin(ang) * 23, 6.5, 4, 6.5, 0.4, folhasSeVazio);
        }
        // Plataforma em volta do tronco (anel) e 3 casinhas da vila da copa.
        m.anel(c, c, 7.9, 14, 88, 88, (x, y, z) -> "spruce_planks");
        m.anel(c, c, 7.9, 14, 89, 92, (x, y, z) -> "air");
        m.anel(c, c, 13.2, 14, 89, 89, (x, y, z) -> "spruce_fence[east=false,north=false,south=false,west=false,waterlogged=false]");
        // Portas do tronco para a plataforma (quatro lados).
        for (int k = 0; k < 4; k++) {
            double ang = k * Math.PI / 2;
            for (double rr = 5; rr <= 8.5; rr += 0.5) {
                int x = (int) Math.round(c + Math.cos(ang) * rr), z = (int) Math.round(c + Math.sin(ang) * rr);
                for (int dy = 89; dy <= 91; dy++) { m.set(x, dy, z, "air"); m.set(x + (k % 2 == 0 ? 0 : 1), dy, z + (k % 2 == 0 ? 1 : 0), "air"); }
            }
        }
        for (int k = 0; k < 3; k++) {
            double ang = k * 2 * Math.PI / 3 + 0.9;
            int hx = (int) Math.round(c + Math.cos(ang) * 18), hz = (int) Math.round(c + Math.sin(ang) * 18);
            m.cilindro(hx, hz, 4.2, 88, 88, (x, y, z) -> "spruce_planks");
            m.fill(hx - 3, 89, hz - 3, hx + 3, 94, hz + 3, "air");
            m.fill(hx - 2, 89, hz - 2, hx + 2, 92, hz + 2, (x, y, z) -> (Math.abs(x - hx) == 2 || Math.abs(z - hz) == 2) ? (y == 90 && (x == hx || z == hz) ? "glass_pane[east=false,north=false,south=false,west=false,waterlogged=false]" : "stripped_spruce_log[axis=y]") : "air");
            m.fill(hx - 3, 93, hz - 3, hx + 3, 93, hz + 3, (x, y, z) -> slabB("spruce"));
            m.fill(hx - 2, 94, hz - 2, hx + 2, 94, hz + 2, (x, y, z) -> slabB("spruce"));
            // porta virada para o tronco
            int px = hx + (int) Math.signum(c - hx) * 2, pz = hz;
            if (Math.abs(c - hx) < Math.abs(c - hz)) { px = hx; pz = hz + (int) Math.signum(c - hz) * 2; }
            m.set(px, 89, pz, "air");
            m.set(px, 90, pz, "air");
            m.set(hx, 91, hz, "lantern[hanging=false,waterlogged=false]");
            m.set(hx, 89, hz, "crafting_table");
            m.bau(hx + 1, 89, hz + 1, "north", L + "arvore_copa");
            // passarela do anel até a casa
            m.linha(c + Math.cos(ang) * 13, 88, c + Math.sin(ang) * 13, hx, 88, hz, 1.2, (x, y, z) -> "spruce_planks");
        }
        // Flores de esporo debaixo da copa (soltam partículas).
        for (int x = 0; x < m.sx; x++)
            for (int z = 0; z < m.sz; z++)
                for (int y = 90; y < 110; y++)
                    if (m.get(x, y, z) == null && m.get(x, y + 1, z) != null && m.get(x, y + 1, z).contains("leaves") && m.r.nextDouble() < 0.025) m.set(x, y, z, "spore_blossom");
        // Câmara: o tesouro das raízes (o chefe nasce no meio, pelo plugin).
        m.bau(c, 2, c - 7, "south", L + "arvore_raizes");
        m.bau(c, 101, c + 2, "north", L + "arvore_copa");
        m.set(c, 101, c, "lantern[hanging=false,waterlogged=false]");
        return m;
    }

    // =====================================================================
    //  Colosso Caído: estátua deitada, oca, com o Coração no peito e tesouro na cabeça
    // =====================================================================

    static Molde colosso() {
        Molde m = new Molde(71, 26, 41, 202);
        Molde.Pal pedra = (x, y, z) -> m.mix(0.5, "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "andesite", "tuff_bricks", "mossy_cobblestone");
        int cz = 20;
        // Cabeça (olhando para o céu)
        m.elipsoide(10, 9, cz, 7, 7, 6.5, 0.05, pedra);
        // Corpo
        m.elipsoide(35, 9, cz, 13.5, 7.5, 10, 0.05, pedra);
        // Pescoço
        m.linha(15, 8, cz, 24, 9, cz, 3.6, pedra);
        // Braços
        m.linha(26, 10, 11, 36, 7, 4, 3.0, pedra);
        m.linha(36, 7, 4, 47, 6, 5, 2.7, pedra);
        m.elipsoide(49, 6, 5, 3, 2.5, 3, 0.1, pedra);
        m.linha(26, 10, 29, 20, 7, 36, 3.0, pedra);
        m.linha(20, 7, 36, 12, 5, 37, 2.7, pedra);
        m.elipsoide(9, 5, 37, 3, 2.5, 3, 0.1, pedra);
        // Pernas e pés
        m.linha(46, 8, 15, 58, 7, 14, 4.2, pedra);
        m.linha(58, 7, 14, 66, 6, 14, 3.8, pedra);
        m.linha(46, 8, 25, 58, 7, 26, 4.2, pedra);
        m.linha(58, 7, 26, 66, 6, 26, 3.8, pedra);
        m.fill(66, 4, 11, 69, 13, 17, pedra);
        m.fill(66, 4, 23, 69, 13, 29, pedra);
        // Rosto: olhos e boca
        for (int z : new int[]{cz - 3, cz + 3}) m.fill(8, 15, z - 1, 9, 16, z, "crying_obsidian");
        m.fill(12, 15, cz - 2, 13, 15, cz + 2, "polished_andesite");
        // Por dentro: cabeça e peito ocos, ligados pelo pescoço; entrada pela rachadura do peito e por um buraco do lado.
        m.elipsoide(10, 9, cz, 4.5, 4.5, 4, 0, (x, y, z) -> "air");
        m.elipsoide(35, 9, cz, 10.5, 5, 7.3, 0, (x, y, z) -> "air");
        m.linha(13, 7, cz, 26, 7, cz, 1.7, (x, y, z) -> "air");
        for (int x = 0; x < m.sx; x++) for (int z = 0; z < m.sz; z++) {
            if (Molde.sq((x - 10) / 4.5) + Molde.sq((z - cz) / 4.0) <= 1) m.fill(x, 4, z, x, 5, z, pedra);
            if (Molde.sq((x - 35) / 10.5) + Molde.sq((z - cz) / 7.3) <= 1) m.fill(x, 4, z, x, 5, z, pedra);
        }
        for (int x = 30; x <= 40; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            if (m.r.nextDouble() < 0.8) for (int y = 13; y <= 18; y++) m.set(x, y, z, "air");
        }
        m.linha(35, 7, 33, 35, 7, 25, 1.8, (x, y, z) -> "air");
        // O Coração do Colosso, no meio do peito.
        m.fill(35, 6, cz, 35, 8, cz, "crying_obsidian");
        m.set(35, 9, cz, "beacon");
        m.set(34, 6, cz, "chiseled_stone_bricks");
        m.set(36, 6, cz, "chiseled_stone_bricks");
        m.bau(35, 6, cz + 3, "north", L + "colosso_coracao");
        m.bau(10, 6, cz, "east", L + "colosso_cabeca");
        // Lanternas lá dentro.
        m.set(28, 6, cz - 4, "lantern[hanging=false,waterlogged=false]");
        m.set(42, 6, cz + 4, "lantern[hanging=false,waterlogged=false]");
        // A espada quebrada, cravada ao lado.
        m.linha(47, 0, 36, 60, 13, 36, 0.4, (x, y, z) -> "iron_block");
        m.linha(47, 0, 37, 60, 13, 37, 0.4, (x, y, z) -> "smooth_stone");
        m.linha(59, 12, 33, 62, 15, 40, 0.4, (x, y, z) -> "gold_block");
        m.linha(61, 14, 36, 64, 17, 37, 0.4, (x, y, z) -> "dark_oak_log[axis=y]");
        m.set(65, 18, 37, "gold_block");
        // Musgo por cima.
        for (int x = 0; x < m.sx; x++) for (int z = 0; z < m.sz; z++) for (int y = m.sy - 2; y >= 0; y--) {
            String s = m.get(x, y, z);
            if (s == null || s.equals("minecraft:air")) continue;
            if (s.contains("stone") || s.contains("andesite") || s.contains("tuff") || s.contains("cobble")) {
                if (m.get(x, y + 1, z) == null && m.r.nextDouble() < 0.12) m.set(x, y + 1, z, "moss_carpet");
            }
            break;
        }
        return m;
    }

    // =====================================================================
    //  Cidade Anã: casa do portão em cima, escada em caracol e a cidade lá embaixo
    // =====================================================================

    static Molde cidadeAna() {
        Molde m = new Molde(49, 50, 49, 303);
        int c = 24, sup = 36;
        boolean[] dentro = new boolean[m.b.length];
        Molde.Pal ar = (x, y, z) -> "air";
        // Salas (ar), marcadas como "dentro".
        int[][] salas = {
                {12, 2, 12, 36, 14, 36},   // grande salão
                {38, 2, 16, 47, 9, 32},    // forja (leste)
                {1, 2, 15, 10, 8, 33},     // mina (oeste)
                {16, 2, 1, 32, 8, 10},     // quartel (norte)
                {18, 2, 38, 30, 8, 47},    // tesouro (sul)
        };
        for (int[] s : salas) m.fill(s[0], s[1], s[2], s[3], s[4], s[5], ar);
        // Teto do salão em abóbada.
        for (int y = 15; y <= 18; y++) { int k = y - 14; m.fill(12 + k * 2, y, 12 + k * 2, 36 - k * 2, y, 36 - k * 2, ar); }
        // Corredores.
        m.fill(37, 2, 23, 38, 5, 25, ar);
        m.fill(10, 2, 23, 12, 5, 25, ar);
        m.fill(23, 2, 10, 25, 5, 12, ar);
        m.fill(23, 2, 36, 25, 5, 38, ar);
        // Poço central até a superfície.
        m.cilindro(c, c, 4.6, 15, sup, ar);
        for (int i = 0; i < m.b.length; i++) if ("minecraft:air".equals(m.b[i])) dentro[i] = true;
        // Casca de pedra em volta de tudo que é sala (2 blocos), só debaixo da terra.
        for (int y = 0; y < sup; y++) for (int z = 0; z < m.sz; z++) for (int x = 0; x < m.sx; x++) {
            if (dentro[m.idx(x, y, z)]) continue;
            boolean perto = false;
            for (int d = -2; d <= 2 && !perto; d++) for (int e = -2; e <= 2 && !perto; e++) for (int f = -2; f <= 2 && !perto; f++)
                if (m.dentro(x + d, y + e, z + f) && dentro[m.idx(x + d, y + e, z + f)]) perto = true;
            if (perto) m.set(x, y, z, y < 18 ? m.mix(0.7, "deepslate", "cobbled_deepslate", "tuff") : m.mix(0.7, "stone", "andesite", "cobblestone"));
        }
        // Pisos.
        m.fill(12, 1, 12, 36, 1, 36, (x, y, z) -> (x + z) % 2 == 0 ? "polished_deepslate" : "deepslate_tiles");
        m.fill(38, 1, 16, 47, 1, 32, "deepslate_bricks");
        m.fill(1, 1, 15, 10, 1, 33, "cobbled_deepslate");
        m.fill(16, 1, 1, 32, 1, 10, "deepslate_bricks");
        m.fill(18, 1, 38, 30, 1, 47, "polished_deepslate");
        // Pilares do salão com lanternas.
        for (int px : new int[]{16, 32}) for (int pz : new int[]{16, 32}) {
            m.cilindro(px, pz, 1.2, 2, 16, (x, y, z) -> "polished_deepslate");
            m.set(px, 10, pz + 2, "lantern[hanging=false,waterlogged=false]");
        }
        for (int px : new int[]{16, 24, 32}) for (int pz : new int[]{14, 34}) m.set(px, 14, pz, "lantern[hanging=true,waterlogged=false]");
        for (int x = 14; x <= 34; x += 4) for (int z = 14; z <= 34; z += 4) if (m.get(x, 19, z) != null) m.set(x, 19, z, "ochre_froglight[axis=y]");
        // Escada em caracol do salão até a casa do portão, em volta de uma coluna.
        m.cilindro(c, c, 1.4, 1, sup + 1, (x, y, z) -> "polished_deepslate");
        caracol(m, c, c, 2.0, 4.3, 2, sup, 26, slabB("deepslate_tile"), "deepslate_tiles", 0);
        // Trono do Rei da Montanha (lado norte do salão).
        m.fill(20, 2, 13, 28, 2, 15, "polished_blackstone");
        m.set(24, 3, 13, escada("polished_blackstone", "south"));
        m.set(23, 3, 13, "gold_block");
        m.set(25, 3, 13, "gold_block");
        m.fill(24, 2, 16, 24, 2, 22, "red_carpet");
        // Forja: canal de lava, bigornas, altos-fornos, barris.
        m.fill(42, 1, 17, 43, 1, 31, "lava[level=0]");
        for (int z = 18; z <= 30; z += 3) { m.set(39, 2, z, "anvil[facing=east]"); m.set(46, 2, z, "blast_furnace[facing=west,lit=true]"); m.set(46, 3, z, "iron_chain[axis=y,waterlogged=false]"); }
        m.barril(39, 2, 31, L + "cidade_ana_forja");
        m.set(40, 2, 17, "smithing_table");
        // Mina: trilhos e minério nas paredes.
        for (int z = 16; z <= 32; z++) m.set(5, 2, z, "rail[shape=north_south,waterlogged=false]");
        for (int i = 0; i < 40; i++) {
            int x = m.r.nextBoolean() ? 0 : 11, y = 2 + m.r.nextInt(6), z = 15 + m.r.nextInt(19);
            if (x == 11 && z >= 22 && z <= 26) continue;
            m.set(x, y, z, m.pick("deepslate_iron_ore", "deepslate_iron_ore", "deepslate_gold_ore", "deepslate_coal_ore", "deepslate_copper_ore", "deepslate_diamond_ore"));
        }
        m.barril(2, 2, 32, L + "cidade_ana_mina");
        m.set(9, 6, 24, "lantern[hanging=true,waterlogged=false]");
        // Quartel: camas e um baú.
        for (int x = 17; x <= 31; x += 2) { m.set(x, 2, 2, "red_bed[facing=north,occupied=false,part=head]"); m.set(x, 2, 3, "red_bed[facing=north,occupied=false,part=foot]"); }
        m.bau(31, 2, 9, "west", L + "cidade_ana_quartel");
        m.set(24, 6, 6, "lantern[hanging=true,waterlogged=false]");
        // Tesouro: ouro e dois baús.
        m.fill(20, 2, 44, 28, 2, 46, (x, y, z) -> m.r.nextDouble() < 0.35 ? "gold_block" : "raw_gold_block");
        m.bau(19, 2, 40, "east", L + "cidade_ana_tesouro");
        m.bau(29, 2, 40, "west", L + "cidade_ana_tesouro");
        m.set(24, 7, 42, "lantern[hanging=true,waterlogged=false]");
        // Casa do portão, na superfície (com base de pedra para não ficar no ar em ladeira).
        int g0 = c - 6, g1 = c + 6;
        m.fill(g0 - 1, sup - 6, g0 - 1, g1 + 1, sup, g1 + 1, (x, y, z) -> m.get(x, y, z) == null || !dentro[m.idx(x, y, z)] ? m.mix(0.6, "stone_bricks", "cracked_stone_bricks", "andesite") : "air");
        m.cilindro(c, c, 4.6, sup - 6, sup, ar);
        m.cilindro(c, c, 1.4, sup - 6, sup + 1, (x, y, z) -> "polished_deepslate");
        caracol(m, c, c, 2.0, 4.3, sup - 1, sup, 26, slabB("deepslate_tile"), "deepslate_tiles", 0);
        m.fill(g0, sup + 1, g0, g1, sup + 9, g1, (x, y, z) -> (x == g0 || x == g1 || z == g0 || z == g1) ? m.mix(0.6, "deepslate_bricks", "cracked_deepslate_bricks", "polished_deepslate") : "air");
        m.fill(g0, sup + 10, g0, g1, sup + 10, g1, "deepslate_tiles");
        for (int x = g0; x <= g1; x += 2) { m.set(x, sup + 11, g0, "deepslate_bricks"); m.set(x, sup + 11, g1, "deepslate_bricks"); }
        for (int z = g0; z <= g1; z += 2) { m.set(g0, sup + 11, z, "deepslate_bricks"); m.set(g1, sup + 11, z, "deepslate_bricks"); }
        m.anel(c, c, 4.6, 5.6, sup + 1, sup + 1, (x, y, z) -> "deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        // Portão grande (sul) e tochas.
        m.fill(c - 1, sup + 1, g1, c + 1, sup + 5, g1, ar);
        m.set(c - 2, sup + 4, g1 + 1, "lantern[hanging=false,waterlogged=false]");
        m.set(c + 2, sup + 4, g1 + 1, "lantern[hanging=false,waterlogged=false]");
        m.set(c - 2, sup + 3, g1 + 1, "deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        m.set(c + 2, sup + 3, g1 + 1, "deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        m.set(c, sup + 8, c, "lantern[hanging=true,waterlogged=false]");
        // De volta: tirar a coluna/escada que caiu fora do poço não é preciso; o salão fica livre em volta dela.
        return m;
    }

    // =====================================================================
    //  Ossada do Dragão: esqueleto gigante num sítio de escavação
    // =====================================================================

    static Molde ossada() {
        Molde m = new Molde(65, 26, 41, 404);
        int cz = 20;
        Molde.Pal osso = (x, y, z) -> m.mix(0.85, "bone_block[axis=y]", "bone_block[axis=x]", "bone_block[axis=z]", "calcite");
        // Chão do sítio (areia e cascalho, com areia suspeita para escavar).
        for (int x = 0; x < m.sx; x++) for (int z = 0; z < m.sz; z++) {
            if (Molde.sq((x - 32) / 32.0) + Molde.sq((z - cz) / 20.0) > 1) continue;
            m.set(x, 0, z, "sandstone");
            m.set(x, 1, z, m.mix(0.7, "sand", "gravel", "coarse_dirt"));
            if (m.r.nextDouble() < 0.05) m.setNbt(x, 1, z, "suspicious_sand[dusted=0]", "{id:\"minecraft:brushable_block\",LootTable:\"rpgatributos:archaeology/ossada\"}");
            else if (m.r.nextDouble() < 0.025) m.setNbt(x, 1, z, "suspicious_gravel[dusted=0]", "{id:\"minecraft:brushable_block\",LootTable:\"rpgatributos:archaeology/ossada\"}");
        }
        // Coluna: da cauda (x=3) ao pescoço (x=50), corcunda no meio.
        double[] yc = new double[m.sx];
        for (int x = 3; x <= 50; x++) {
            double t = (x - 3) / 47.0;
            yc[x] = 3 + 9 * Math.pow(Math.sin(Math.PI * Math.min(1, t * 1.15)), 0.7);
        }
        for (int x = 3; x < 50; x++) m.linha(x, yc[x], cz + Math.sin(x * 0.15) * 1.5, x + 1, yc[x + 1], cz + Math.sin((x + 1) * 0.15) * 1.5, x < 15 ? 0.7 : 1.3, osso);
        // Pescoço subindo até o crânio.
        m.linha(50, yc[50], cz, 56, 10, cz, 1.2, osso);
        // Crânio.
        m.elipsoide(59, 9, cz, 5, 3.5, 3.6, 0.1, osso);
        m.elipsoide(59, 9, cz, 3.3, 2.2, 2.2, 0, (x, y, z) -> "air");
        for (int z : new int[]{cz - 3, cz + 3}) m.fill(58, 10, z, 59, 11, z, "air");
        m.linha(56, 6, cz, 64, 5, cz, 0.9, osso);
        m.linha(56, 12, cz - 2, 50, 16, cz - 4, 0.6, osso);
        m.linha(56, 12, cz + 2, 50, 16, cz + 4, 0.6, osso);
        // Costelas.
        for (int x = 17; x <= 41; x += 3) {
            for (int lado : new int[]{-1, 1}) {
                double py = yc[x], pz = cz;
                for (int k = 1; k <= 10; k++) {
                    double t = k / 10.0;
                    double ny = yc[x] - (yc[x] - 1.5) * t * t, nz = cz + lado * 9 * Math.sin(t * Math.PI / 2 + 0.2);
                    m.linha(x, py, pz, x, ny, nz, 0.5, osso);
                    py = ny;
                    pz = nz;
                }
            }
        }
        // Asas abertas no chão, com os dedos.
        for (int lado : new int[]{-1, 1}) {
            double ox = 38, oy = yc[38], oz = cz + lado * 3;
            double tx = 30, ty = 4, tz = cz + lado * 19;
            m.linha(ox, oy, oz, tx, ty, tz, 0.9, osso);
            for (int d = 0; d < 3; d++) m.linha(tx, ty, tz, 22 - d * 6, 2, cz + lado * (18 - d * 2), 0.5, osso);
        }
        // Patas.
        for (int[] p : new int[][]{{22, -1}, {22, 1}, {40, -1}, {40, 1}}) m.linha(p[0], yc[p[0]] - 1, cz + p[1] * 2, p[0] + 2, 2, cz + p[1] * 7, 0.7, osso);
        // Acampamento dos escavadores.
        m.fill(3, 2, 31, 8, 4, 36, (x, y, z) -> (y == 4 || x == 3 || x == 8) && !(y < 4 && z == 36) ? "white_wool" : "air");
        m.set(5, 2, 33, "crafting_table");
        m.barril(6, 2, 33, L + "ossada_acampamento");
        m.set(11, 2, 34, "campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]");
        m.fill(13, 2, 30, 13, 6, 30, "scaffolding[bottom=false,distance=0,waterlogged=false]");
        m.set(12, 2, 37, "barrel[facing=up,open=false]");
        return m;
    }

    // =====================================================================
    //  Coliseu: arena oval com arquibancada; spawners de desafio na arena e o cofre no camarote
    // =====================================================================

    static Molde coliseu() {
        Molde m = new Molde(61, 22, 49, 505);
        int cx = 30, cz = 24;
        double ox = 30, oz = 24, ax = 15, az = 10;
        Molde.Pal muro = (x, y, z) -> m.mix(0.6, "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "polished_andesite");
        for (int x = 0; x < m.sx; x++) for (int z = 0; z < m.sz; z++) {
            double qo = Math.sqrt(Molde.sq((x - cx) / ox) + Molde.sq((z - cz) / oz));
            double qa = Math.sqrt(Molde.sq((x - cx) / ax) + Molde.sq((z - cz) / az));
            if (qo > 1) continue;
            m.set(x, 0, z, "stone_bricks");
            if (qa <= 1) {
                m.set(x, 1, z, m.mix(0.75, "sand", "coarse_dirt", "gravel"));
                for (int y = 2; y < m.sy; y++) m.set(x, y, z, "air");
            } else if (qa <= 1.12) {
                m.fill(x, 1, z, x, 4, z, "polished_andesite");
                for (int y = 5; y < m.sy; y++) m.set(x, y, z, "air");
            } else if (qo <= 0.88) {
                // Arquibancada: sobe de 4 até 13 conforme se afasta da arena.
                double f = (qa - 1.12) / ((0.88 * ox / ax) - 1.12);
                int h = 4 + (int) Math.round(Math.max(0, Math.min(1, f)) * 9);
                m.fill(x, 1, z, x, h - 1, z, muro);
                m.set(x, h, z, "smooth_stone_slab[type=bottom,waterlogged=false]");
                for (int y = h + 1; y < m.sy; y++) m.set(x, y, z, "air");
            } else {
                // Muro de fora, com arcos.
                double ang = Math.atan2((z - cz) / oz, (x - cx) / ox);
                int setor = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * 36);
                double meioSetor = ((setor + 0.5) / 36.0) * 2 * Math.PI - Math.PI;
                boolean arco = Math.abs(ang - meioSetor) < 0.035 * 36 / 12;
                for (int y = 1; y <= 18; y++) {
                    boolean buraco = arco && (y >= 1 && y <= 5 || y >= 9 && y <= 12) && qo > 0.92;
                    m.set(x, y, z, buraco ? "air" : muro.at(x, y, z));
                }
            }
        }
        // Portões nas pontas do eixo maior.
        for (int lado : new int[]{-1, 1}) {
            int x0 = lado < 0 ? 0 : cx + 15, x1 = lado < 0 ? cx - 15 : m.sx - 1;
            m.fill(x0, 1, cz - 2, x1, 5, cz + 2, "air");
            m.fill(x0, 1, cz - 2, x1, 1, cz + 2, "coarse_dirt");
            int xp = lado < 0 ? cx - 16 : cx + 16;
            m.fill(xp, 4, cz - 2, xp, 5, cz + 2, "iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]");
        }
        // Camarote do imperador (lado norte), com o cofre e um baú.
        m.fill(cx - 4, 1, 1, cx + 4, 13, 7, muro);
        m.fill(cx - 3, 14, 2, cx + 3, 17, 7, "air");
        m.fill(cx - 4, 14, 1, cx + 4, 14, 1, "red_wool");
        m.setNbt(cx, 14, 3, "vault[facing=south,ominous=false,vault_state=inactive]",
                "{id:\"minecraft:vault\",config:{key_item:{count:1,id:\"minecraft:trial_key\"},loot_table:\"rpgatributos:coliseu/recompensa\"}}");
        m.bau(cx - 2, 14, 3, "south", L + "coliseu");
        m.fill(cx - 4, 18, 1, cx + 4, 18, 7, "smooth_stone_slab[type=bottom,waterlogged=false]");
        m.set(cx + 2, 14, 3, "lantern[hanging=false,waterlogged=false]");
        // Spawners de desafio na arena (quatro tipos de lutadores).
        String[][] sp = {{"gladiadores", "-8", "0"}, {"arqueiros", "8", "0"}, {"feras", "0", "-5"}, {"campeoes", "0", "5"}};
        for (String[] s : sp) {
            int x = cx + Integer.parseInt(s[1]), z = cz + Integer.parseInt(s[2]);
            m.setNbt(x, 1, z, "trial_spawner[ominous=false,trial_spawner_state=inactive]",
                    "{id:\"minecraft:trial_spawner\",normal_config:\"rpgatributos:coliseu/" + s[0] + "/normal\",ominous_config:\"rpgatributos:coliseu/" + s[0] + "/ominous\"}");
        }
        // Estátuas nos portões.
        for (int lado : new int[]{-1, 1}) for (int zz : new int[]{cz - 4, cz + 4}) {
            int x = cx + lado * 26;
            m.fill(x, 1, zz, x, 6, zz, "chiseled_stone_bricks");
            m.set(x, 7, zz, "carved_pumpkin[facing=" + (lado < 0 ? "west" : "east") + "]");
        }
        m.ruir(12, 0.35, "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "polished_andesite");
        return m;
    }

    // =====================================================================
    //  Castelo do Rei Caído: muralha, torres, pátio e o torreão com a sala do trono no meio
    // =====================================================================

    static Molde castelo() {
        Molde m = new Molde(45, 32, 45, 606);
        int c = 22;
        Molde.Pal pedra = (x, y, z) -> m.mix(0.55, "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "cobblestone", "andesite");
        // Chão.
        m.fill(1, 0, 1, 43, 0, 43, (x, y, z) -> m.mix(0.5, "cobblestone", "gravel", "mossy_cobblestone", "dirt_path"));
        m.fill(1, 1, 1, 43, 20, 43, "air");
        // Muralha (2 de grossura) com ameias.
        for (int x = 3; x <= 41; x++) for (int z = 3; z <= 41; z++) {
            boolean borda = x <= 4 || x >= 40 || z <= 4 || z >= 40;
            if (!borda) continue;
            m.fill(x, 1, z, x, 10, z, pedra);
            boolean fora = x == 3 || x == 41 || z == 3 || z == 41;
            if (fora && (x + z) % 2 == 0) m.set(x, 11, z, pedra.at(x, 11, z));
        }
        // Torres nos cantos.
        int[][] torres = {{4, 4}, {40, 4}, {4, 40}, {40, 40}};
        for (int i = 0; i < 4; i++) {
            int tx = torres[i][0], tz = torres[i][1];
            int topo = i == 3 ? 7 : 15;
            m.cilindro(tx, tz, 4.4, 1, topo, pedra);
            m.cilindro(tx, tz, 2.4, 1, topo - 1, (x, y, z) -> "air");
            if (i != 3) {
                m.anel(tx, tz, 3.4, 4.4, topo + 1, topo + 1, (x, y, z) -> (x + z) % 2 == 0 ? pedra.at(x, y, z) : "air");
                m.cilindro(tx, tz, 3.4, topo, topo, (x, y, z) -> "spruce_planks");
                m.set(tx, topo - 3, tz, "lantern[hanging=true,waterlogged=false]");
            } else {
                // A torre que caiu: entulho no pátio.
                for (int k = 0; k < 60; k++) m.set(30 + m.r.nextInt(9), 1, 30 + m.r.nextInt(9), pedra.at(0, 0, 0));
            }
        }
        // Portão (sul).
        m.fill(20, 1, 39, 24, 7, 43, pedra);
        m.fill(21, 1, 39, 23, 5, 43, "air");
        m.fill(21, 5, 41, 23, 5, 41, "iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]");
        for (int x = 20; x <= 24; x += 2) m.set(x, 8, 43, pedra.at(x, 8, 43));
        // Torreão no meio (17 x 17), dois andares.
        int k0 = c - 8, k1 = c + 8;
        m.fill(k0, 1, k0, k1, 19, k1, (x, y, z) -> (x == k0 || x == k1 || z == k0 || z == k1) ? pedra.at(x, y, z) : "air");
        m.fill(k0 + 1, 10, k0 + 1, k1 - 1, 10, k1 - 1, "dark_oak_planks");
        m.fill(k0, 20, k0, k1, 20, k1, "stone_bricks");
        for (int x = k0; x <= k1; x += 2) { m.set(x, 21, k0, pedra.at(x, 21, k0)); m.set(x, 21, k1, pedra.at(x, 21, k1)); }
        for (int z = k0; z <= k1; z += 2) { m.set(k0, 21, z, pedra.at(k0, 21, z)); m.set(k1, 21, z, pedra.at(k1, 21, z)); }
        // Janelas.
        for (int y : new int[]{4, 13}) for (int t = k0 + 3; t <= k1 - 3; t += 5) {
            m.set(t, y, k0, "air"); m.set(t, y + 1, k0, "air");
            m.set(k0, y, t, "air"); m.set(k0, y + 1, t, "air");
            m.set(k1, y, t, "air"); m.set(k1, y + 1, t, "air");
        }
        // Porta do torreão (sul) e tapete até o trono.
        m.fill(c - 1, 1, k1, c + 1, 4, k1, "air");
        m.fill(c, 0, k0 + 3, c, 0, k1, "polished_andesite");
        m.fill(c, 1, k0 + 4, c, 1, k1 - 1, "red_carpet");
        // Trono (lado norte), no meio do salão.
        m.fill(c - 2, 1, k0 + 1, c + 2, 1, k0 + 3, "polished_blackstone");
        m.set(c, 2, k0 + 2, escada("polished_blackstone", "south"));
        m.set(c - 1, 2, k0 + 2, "gold_block");
        m.set(c + 1, 2, k0 + 2, "gold_block");
        m.set(c, 3, k0 + 1, "gold_block");
        m.bau(c, 2, k0 + 1, "south", L + "castelo_trono");
        // Pilares e luz do salão.
        for (int x : new int[]{c - 5, c + 5}) for (int z : new int[]{c - 4, c + 2}) {
            m.fill(x, 1, z, x, 9, z, "polished_andesite");
            m.set(x, 8, z + 1, "lantern[hanging=false,waterlogged=false]");
        }
        // Escada de mão para o andar de cima, e o andar de cima.
        for (int y = 1; y <= 10; y++) m.set(k0 + 1, y, k1 - 1, "ladder[facing=east,waterlogged=false]");
        m.set(k0 + 1, 10, k1 - 1, "air");
        for (int x = k0 + 1; x <= k1 - 1; x++) m.set(x, 11, k0 + 1, "bookshelf");
        m.bau(k1 - 1, 11, k0 + 1, "west", L + "castelo_biblioteca");
        m.set(c, 11, c, "lectern[facing=north,has_book=false,powered=false]");
        m.set(c, 18, c, "lantern[hanging=true,waterlogged=false]");
        // Pátio: poço e feno.
        m.cilindro(10, 30, 2, 1, 1, (x, y, z) -> "cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]");
        m.set(10, 1, 30, "water[level=0]");
        m.set(10, 0, 30, "water[level=0]");
        m.fill(32, 1, 8, 34, 1, 9, "hay_block[axis=x]");
        // Ruína: muros e telhado desabando um pouco, musgo.
        m.ruir(6, 0.25, "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "cobblestone", "andesite");
        return m;
    }
}
