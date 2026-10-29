package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.detalhes.BarcoGrande;
import br.rpgatributos.detalhes.ItensDetalhes;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Turtle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O mar aberto (3.0, "Mundo e Mar"): o que nasce na superfície do oceano, longe da costa.
 * <ul>
 *   <li>Ilhota: areia, palmeiras e uma de três histórias (o náufrago, o esconderijo pirata ou a ilha
 *       das aves);</li>
 *   <li>Navio Pirata ancorado: tripulação, canhões que atiram em quem chega perto e o Capitão, que
 *       leva um mapa do tesouro;</li>
 *   <li>Iceberg: gelo flutuando nos mares gelados, ursos e um explorador congelado com o diário;</li>
 *   <li>Barco Mercante: um mercador ancorado que vende o que serve no mar;</li>
 *   <li>Ilha-Tartaruga: uma ilha redonda demais. Cavar nela ou acender fogo acorda a Tartaruga
 *       Ancestral.</li>
 * </ul>
 * Mesmas regras de {@link Projetos}: coordenadas locais (proa para +z); aqui o y 0 é a água da
 * superfície (o que fica em y 1 já está fora da água).
 */
public final class MarAberto implements Listener {

    static final NamespacedKey K_BALA = new NamespacedKey("rpgatributos", "bala_de_canhao");
    static final NamespacedKey K_NAUFRAGO = new NamespacedKey("rpgatributos", "naufrago");
    private static final NamespacedKey K_CARTA = new NamespacedKey("rpgatributos", "carta_nautica");
    private static final NamespacedKey K_ATENDIDO = new NamespacedKey("rpgatributos", "naufrago_atendido");
    static final TextColor COR = TextColor.color(0x4FC3F7);

    /** O que conta como "lugar do mar" para a Carta Náutica. */
    private static final Set<Estrutura> DO_MAR = EnumSet.of(Estrutura.NAUFRAGIO, Estrutura.CIDADE_SUBMERSA, Estrutura.FAROL,
            Estrutura.VILA_PESCADORES, Estrutura.ILHOTA, Estrutura.NAVIO_PIRATA, Estrutura.ICEBERG, Estrutura.BARCO_MERCANTE,
            Estrutura.ILHA_TARTARUGA);

    // Pontos fixos das obras (locais).
    static final int[] NAUFRAGO = {1, 2, -1};
    static final int[][] PIRATAS_ILHA = {{-2, 2, 1}, {2, 2, 1}, {0, 2, -2}};
    static final int[][] TRIPULACAO = {{1, 3, 3}, {-1, 3, 3}, {1, 3, -1}, {-1, 3, -1}, {0, 3, 5}};
    static final int[] CAPITAO = {0, 3, -2};
    static final int[][] CANHOES = {{2, 3, 2}, {2, 3, -2}, {-2, 3, 2}, {-2, 3, -2}};
    static final int[] MERCADOR = {0, 3, 1};
    static final int[] TARTARUGA = {0, -2, 10};

    private static final String[] NAUFRAGOS = {"Joca", "Dona Filó", "Velho Tião", "Marisa", "Bento Sal", "Rosinha"};
    private static final String[] CAPITAES = {"Capitão Barba-Ruiva", "Capitã Ana Maré", "Capitão Olho-de-Vidro",
            "Capitã Rosa Negra", "Capitão Dente-de-Ouro", "Capitão Perna-de-Pau"};
    public static final String[] MERCADORES = {"Mercador do Mar Silas", "Mercadora do Mar Iara", "Mercador do Mar Nuno",
            "Mercadora do Mar Clara", "Mercador do Mar Ravi", "Mercadora do Mar Dora"};

    private final RPGAtributos plugin;
    private final Estruturas est;
    private final Projetos base;
    /** A barra de vida da Tartaruga Ancestral de cada ilha acordada. */
    private final Map<String, BossBar> barras = new HashMap<>();
    /** Quantas vezes a tartaruga já agiu (a onda vem de 4 em 4) e quando foi a última. */
    private final Map<String, Integer> golpes = new HashMap<>();
    private final Map<String, Long> ultimoGolpe = new HashMap<>();

    MarAberto(RPGAtributos plugin, Estruturas est, Projetos base) {
        this.plugin = plugin;
        this.est = est;
        this.base = base;
    }

    private static Material mat(String id) {
        return Material.valueOf(id.toUpperCase(java.util.Locale.ROOT));
    }

    // =====================================================================
    //  Onde nasce: a água da superfície, com fundo bastante
    // =====================================================================

    /**
     * A água da superfície na coluna (gelo também, nos mares gelados), com pelo menos
     * {@code profundidade} blocos de água por baixo. Null se for raso ou terra.
     */
    static Block superficie(World w, int x, int z, int profundidade) {
        int y = w.getHighestBlockYAt(x, z);
        Block topo = w.getBlockAt(x, y, z);
        Material m = topo.getType();
        boolean gelo = m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE;
        if (m != Material.WATER && !gelo) return null;
        for (int d = 1; d <= profundidade; d++) if (!Estruturas.molhado(w.getBlockAt(x, y - d, z))) return null;
        return topo;
    }

    /** Monta a obra no mar aberto (chamada por {@link Estruturas#construir}). */
    void construir(Sitio s, Obra o) {
        switch (s.tipo) {
            case ILHOTA -> ilhota(o, s.fundo);
            case NAVIO_PIRATA -> navio(o, Paleta.PIRATA, 7, s);
            case ICEBERG -> iceberg(o, s);
            case BARCO_MERCANTE -> navio(o, Paleta.MERCANTE, 5, s);
            case ILHA_TARTARUGA -> tartaruga(o);
            default -> { }
        }
        o.conectar();
    }

    /** Variação suave em volta do centro (ilhas e icebergs não ficam redondos demais). */
    private static double ruido(double[] fase, int x, int z) {
        double a = Math.atan2(z, x);
        return 0.7 * Math.sin(a * 3 + fase[0]) + 0.45 * Math.sin(a * 5 + fase[1]) + 0.25 * Math.sin(a * 7 + fase[2]);
    }

    private static double[] fases(Random r) {
        return new double[]{r.nextDouble() * 6.3, r.nextDouble() * 6.3, r.nextDouble() * 6.3};
    }

    /** Enche a coluna do fundo do mar até {@code topo} (y local). */
    private static void coluna(Obra o, int x, int z, int topo, Material cima, Material meio, Material baixo) {
        int fundo = 0;
        for (int dy = 0; dy > -48; dy--) {
            Block b = o.bloco(x, dy, z);
            if (!Estruturas.molhado(b) && !b.getType().isAir()) break;
            fundo = dy;
        }
        for (int dy = fundo; dy <= topo; dy++) {
            o.por(x, dy, z, dy == topo ? cima : dy >= topo - 2 ? meio : baixo);
        }
    }

    // =====================================================================
    //  Ilhota
    // =====================================================================

    void ilhota(Obra o, int variante) {
        double[] f = fases(o.r);
        // Ilha: platô a 1 bloco da água, praia rente à água e uma beirada rasa por baixo.
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double d = Math.hypot(x, z * 1.1) + ruido(f, x, z) * 0.6;
                if (d > 6.6) continue;
                int topo = d < 3.9 ? 1 : d < 5.0 ? 0 : d < 5.9 ? -1 : -2;
                Material cima = topo == 1 && d < 2.3 && variante != 1 ? Material.GRASS_BLOCK : Material.SAND;
                coluna(o, x, z, topo, cima, Material.SAND, Material.SANDSTONE);
            }
        }
        // Palmeiras.
        palmeira(o, -2, 1, -1, 1);
        if (o.chance(0.7)) palmeira(o, 1, -2, 1, -1);
        // Conchas e algas na beirada.
        for (int i = 0; i < 6; i++) {
            int x = -5 + o.r.nextInt(11), z = -5 + o.r.nextInt(11);
            Block b = o.bloco(x, 1, z);
            if (b.getType().isAir() && o.bloco(x, 0, z).getType() == Material.SAND) o.por(x, 1, z, o.chance(0.5) ? "dead_bush" : "short_dry_grass");
        }
        switch (variante) {
            case 0 -> naufragoAcampamento(o);
            case 1 -> esconderijoPirata(o);
            default -> ilhaDasAves(o);
        }
    }

    private static void palmeira(Obra o, int x, int z, int dx, int dz) {
        int cx = x, cz = z, alt = 5 + o.r.nextInt(2);
        for (int i = 1; i <= alt; i++) {
            if (i == 3 || i == 5) {
                cx += dx;
                cz += dz;
            }
            o.por(cx, 1 + i, cz, "jungle_log[axis=y]");
        }
        int ty = 2 + alt;
        String folha = "jungle_leaves[persistent=true]";
        o.por(cx, ty, cz, folha);
        int[][] bracos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] b : bracos) {
            o.por(cx + b[0], ty, cz + b[1], folha);
            o.por(cx + 2 * b[0], ty, cz + 2 * b[1], folha);
            o.por(cx + 3 * b[0], ty - 1, cz + 3 * b[1], folha);
        }
        o.por(cx + 1, ty - 1, cz, "cocoa[age=2,facing=west]");
    }

    private void naufragoAcampamento(Obra o) {
        // Abrigo: quatro varas, telhado de lona e a fogueira de sinal.
        for (int[] v : new int[][]{{-1, -2}, {2, -2}, {-1, 0}, {2, 0}}) {
            o.por(v[0], 2, v[1], "oak_fence");
            o.por(v[0], 3, v[1], "oak_fence");
        }
        for (int x = -1; x <= 2; x++) for (int z = -2; z <= 0; z++) o.por(x, 4, z, z == -1 ? Material.WHITE_WOOL : Material.LIGHT_GRAY_WOOL);
        o.por(-2, 2, 2, "campfire[lit=true]");
        o.por(0, 2, -2, "barrel[facing=up]");
        o.por(2, 2, 2, "oak_sign[rotation=0]");
        Block placa = o.bloco(2, 2, 2);
        if (placa.getState() instanceof org.bukkit.block.Sign sg) {
            var lado = sg.getSide(org.bukkit.block.sign.Side.FRONT);
            lado.line(0, Component.text("SOCORRO"));
            lado.line(1, Component.text("dia 41"));
            lado.line(2, Component.text("tenho fome"));
            sg.setWaxed(true);
            sg.update(false, false);
        }
        // Pedras escrevendo "SOS" na areia.
        for (int[] p : new int[][]{{-4, 1}, {-4, 0}, {-3, 0}, {-3, -1}, {-4, -1}}) o.por(p[0], 1, p[1] - 1, Material.COBBLESTONE);
    }

    private void esconderijoPirata(Obra o) {
        // O X na areia e o baú enterrado embaixo dele.
        for (int[] p : new int[][]{{-1, 0}, {1, 0}, {0, 1}, {-1, 2}, {1, 2}}) o.por(p[0], 1, p[1], Material.RED_TERRACOTTA);
        o.por(0, 0, 1, Material.SAND);
        Block bau = o.por(0, -1, 1, "chest[facing=south]");
        List<ItemStack> extra = new ArrayList<>();
        extra.add(new ItemStack(Material.GOLD_INGOT, 4 + o.r.nextInt(6)));
        extra.add(new ItemStack(Material.EMERALD, 3 + o.r.nextInt(5)));
        if (o.chance(0.35)) extra.add(Raro.MAPA_RASGADO.criar(1));
        base.bau(bau, LootTables.BURIED_TREASURE, o, extra);
        // Caveira no espeto, barris de pólvora e uma rede velha.
        o.por(-2, 2, -2, "oak_fence");
        o.por(-2, 3, -2, "skeleton_skull[rotation=8]");
        Block barril = o.por(2, 2, -2, "barrel[facing=up]");
        base.bau(barril, null, o, List.of(new ItemStack(Material.GUNPOWDER, 3 + o.r.nextInt(5)), new ItemStack(Material.ROTTEN_FLESH, 2)));
        o.por(2, 2, -1, "barrel[facing=north]");
        o.por(3, 2, 0, Material.COBWEB);
    }

    private void ilhaDasAves(Obra o) {
        for (int[] p : new int[][]{{-1, -1}, {1, 1}, {2, -2}, {-2, 1}}) {
            o.por(p[0], 2, p[1], Material.HAY_BLOCK);
            o.porSeVazio(p[0], 3, p[1], Material.OAK_LEAVES);
        }
        Block barril = o.por(0, 2, 0, "barrel[facing=up]");
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.FEATHER, 6 + o.r.nextInt(8)));
        l.add(new ItemStack(Material.EGG, 3 + o.r.nextInt(5)));
        l.add(ItensDetalhes.ninho(1 + o.r.nextInt(2)));
        if (o.chance(0.4)) l.add(new ItemStack(Material.TURTLE_SCUTE, 1 + o.r.nextInt(2)));
        base.bau(barril, null, o, l);
    }

    // =====================================================================
    //  Navios (pirata, mercante e o fantasma)
    // =====================================================================

    /** Madeiras, velas e o que vai no convés de cada navio. */
    enum Paleta {
        PIRATA("dark_oak", "dark_oak", Material.BLACK_WOOL, "black_banner", true),
        MERCANTE("spruce", "birch", Material.WHITE_WOOL, "white_banner", false),
        FANTASMA("dark_oak", "dark_oak", Material.LIGHT_GRAY_WOOL, "gray_banner", true);

        final String casco, conves, bandeira;
        final Material vela;
        final boolean canhoes;

        Paleta(String casco, String conves, Material vela, String bandeira, boolean canhoes) {
            this.casco = casco;
            this.conves = conves;
            this.vela = vela;
            this.bandeira = bandeira;
            this.canhoes = canhoes;
        }
    }

    /** Meia largura do casco em u (a proa afina, a popa é reta). */
    private static int largura(int u, int meio) {
        return u >= meio ? 0 : u >= meio - 2 ? 1 : 2;
    }

    /**
     * Um navio inteiro boiando, de proa para +z: casco de y -3 a 1 (a linha d'água é o 0),
     * convés em 2, amurada em 3, castelo de popa com a cabine, mastros e velas.
     */
    void navio(Obra o, Paleta pal, int meio, Sitio s) {
        Material tabua = mat(pal.casco + "_planks"), piso = mat(pal.conves + "_planks"), faixa = mat("stripped_" + pal.casco + "_log");
        String cerca = pal.casco + "_fence";
        boolean fantasma = pal == Paleta.FANTASMA;
        // Casco.
        for (int u = -meio; u <= meio; u++) {
            int w = largura(u, meio);
            for (int dy = -3; dy <= 1; dy++) {
                int lim = dy == -3 ? w - 1 : w;
                if (lim < 0) continue;
                for (int v = -lim; v <= lim; v++) {
                    boolean parede = Math.abs(v) == lim || dy == -3 || u == -meio || u == meio || largura(u + 1, meio) < Math.abs(v);
                    if (parede) o.por(v, dy, u, fantasma && o.chance(0.06) ? Material.AIR : dy == 1 ? faixa : tabua);
                    else o.por(v, dy, u, Material.AIR);
                }
            }
            // Convés e amurada.
            for (int v = -w; v <= w; v++) {
                boolean alcapao = v == 0 && u == 0;
                if (alcapao) o.por(v, 2, u, pal.casco + "_trapdoor[half=top,open=true,facing=south]");
                else o.por(v, 2, u, fantasma && Math.abs(v) < w && o.chance(0.08) ? Material.AIR : piso);
                if (u > -meio + 3 && (Math.abs(v) == w) && !(fantasma && o.chance(0.3))) o.por(v, 3, u, cerca);
            }
        }
        o.por(0, 3, meio, cerca);
        o.por(0, 4, meio, cerca);
        // O mastro desce até a quilha, e a escada do porão fica presa nele.
        for (int dy = -2; dy <= 1; dy++) {
            o.por(0, dy, -1, mat(pal.casco + "_log"));
            o.por(0, dy, 0, "ladder[facing=south]");
        }

        // Castelo de popa com a cabine do capitão.
        int c0 = -meio, c1 = -meio + 3;
        for (int u = c0; u <= c1; u++) {
            for (int v = -2; v <= 2; v++) {
                boolean parede = Math.abs(v) == 2 || u == c0 || u == c1;
                for (int dy = 3; dy <= 5; dy++) {
                    if (!parede) o.por(v, dy, u, Material.AIR);
                    else if (u == c1 && v == 0 && dy <= 4) o.por(v, dy, u, Material.AIR);
                    else if (dy == 4 && (Math.abs(v) == 2 && u == c0 + 1 || u == c0 && Math.abs(v) == 1)) o.por(v, dy, u, "glass_pane");
                    else o.por(v, dy, u, tabua);
                }
                o.por(v, 6, u, pal.casco + "_slab");
            }
        }
        o.por(1, 3, c0 + 1, fantasma ? "soul_lantern" : "lantern");
        Block bau = o.por(-1, 3, c0 + 1, "chest[facing=east]");
        List<ItemStack> extra = new ArrayList<>();
        if (pal == Paleta.PIRATA) {
            extra.add(new ItemStack(Material.GOLD_INGOT, 4 + o.r.nextInt(6)));
            if (o.chance(0.5)) extra.add(new ItemStack(Material.EMERALD, 3 + o.r.nextInt(5)));
        } else if (fantasma) {
            extra.add(new ItemStack(Material.GOLD_NUGGET, 10 + o.r.nextInt(14)));
            if (o.chance(0.3)) extra.add(Raro.MAPA_RASGADO.criar(1));
        } else {
            extra.add(new ItemStack(Material.EMERALD, 2 + o.r.nextInt(3)));
        }
        base.bau(bau, pal == Paleta.MERCANTE ? LootTables.SHIPWRECK_SUPPLY : LootTables.SHIPWRECK_TREASURE, o, extra);

        // Porão: carga.
        Block porao = o.por(-1, -2, -2, "chest[facing=east]");
        base.bau(porao, LootTables.SHIPWRECK_SUPPLY, o, new ArrayList<>());
        o.por(1, -2, -2, "barrel[facing=up]");
        o.por(1, -2, 2, "barrel[facing=up]");
        o.por(-1, -2, 2, "barrel[facing=west]");
        o.por(1, -1, -1, fantasma ? "soul_lantern" : "lantern");

        // Mastros e velas (de frente para a proa).
        int topo = meio >= 7 ? 12 : 10;
        for (int dy = 3; dy <= topo; dy++) o.por(0, dy, -1, mat(pal.casco + "_log"));
        int vela0 = 6, vela1 = topo - 1;
        for (int dy = vela0; dy <= vela1; dy++) {
            for (int v = -3; v <= 3; v++) {
                if (fantasma && o.chance(0.22)) continue; // vela rasgada
                o.por(v, dy, 0, pal.vela);
            }
        }
        for (int v = -3; v <= 3; v++) o.por(v, topo, 0, cerca);
        o.por(0, topo + 1, -1, pal.bandeira);
        if (meio >= 7) {
            for (int dy = 3; dy <= 9; dy++) o.por(0, dy, 4, mat(pal.casco + "_log"));
            for (int dy = 5; dy <= 8; dy++) for (int v = -2; v <= 2; v++) if (!(fantasma && o.chance(0.25))) o.por(v, dy, 5, pal.vela);
        }
        // Canhões nas amuradas.
        if (pal.canhoes) {
            for (int[] c : CANHOES) o.por(c[0], c[1], c[2], "dispenser[facing=" + (c[0] > 0 ? "east" : "west") + "]");
            o.por(0, 3, 6, "skeleton_skull[rotation=8]");
        }
        if (pal == Paleta.MERCANTE) {
            // Toldo listrado da loja no convés (abaixo da vela) e a mercadoria.
            for (int u = 0; u <= 2; u++) for (int v = -2; v <= 2; v++) o.por(v, 5, u, Math.floorMod(u + v, 2) == 0 ? Material.RED_WOOL : Material.WHITE_WOOL);
            for (int[] p : new int[][]{{-2, 0}, {2, 0}, {-2, 2}, {2, 2}}) {
                o.por(p[0], 3, p[1], "spruce_fence");
                o.por(p[0], 4, p[1], "spruce_fence");
            }
            o.por(-1, 3, 2, "barrel[facing=up]");
            o.por(1, 3, 2, "barrel[facing=up]");
            o.por(1, 3, 0, "barrel[facing=west]");
            o.por(-1, 3, 0, "lantern");
        }
        if (fantasma) {
            for (int i = 0; i < 10; i++) {
                int u = -meio + 4 + o.r.nextInt(2 * meio - 4), v = -1 + o.r.nextInt(3);
                Block b = o.bloco(v, 3, u);
                if (b.getType().isAir()) o.por(v, 3, u, Material.COBWEB);
            }
        }
    }

    // =====================================================================
    //  Iceberg
    // =====================================================================

    void iceberg(Obra o, Sitio s) {
        double[] f = fases(o.r);
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.hypot(x, z) + ruido(f, x, z) * 0.7;
                if (d > 5.6) {
                    if (d < 6.8 && o.chance(0.25)) o.por(x, 0, z, o.chance(0.5) ? Material.PACKED_ICE : Material.ICE);
                    continue;
                }
                int topo = 1 + (int) Math.round((5.6 - d) * 1.6), fundo = -4 - (int) Math.round(5.6 - d);
                for (int dy = fundo; dy <= topo; dy++) {
                    Material m = dy == topo ? Material.SNOW_BLOCK : o.chance(0.12) ? Material.BLUE_ICE : Material.PACKED_ICE;
                    o.por(x, dy, z, m);
                }
                if (o.chance(0.5)) o.por(x, topo + 1, z, "snow[layers=" + (1 + o.r.nextInt(3)) + "]");
            }
        }
        // Túnel até a câmara do explorador.
        for (int z = 1; z <= 6; z++) for (int x = -1; x <= 0; x++) for (int dy = 1; dy <= 2; dy++) o.por(x, dy, z, Material.AIR);
        for (int x = -2; x <= 1; x++) for (int z = -2; z <= 1; z++) for (int dy = 1; dy <= 3; dy++) o.por(x, dy, z, Material.AIR);
        // O explorador preso no gelo (atrás de um bloco de gelo transparente).
        o.por(-1, 1, -3, Material.AIR);
        o.por(-1, 2, -3, Material.AIR);
        o.por(-1, 1, -2, Material.ICE);
        o.por(-1, 2, -2, Material.ICE);
        Location onde = o.bloco(-1, 1, -3).getLocation().add(0.5, 0, 0.5);
        onde.setDirection(o.face(org.bukkit.block.BlockFace.SOUTH).getDirection());
        o.w.spawn(onde, ArmorStand.class, a -> {
            a.setPersistent(true);
            a.setArms(true);
            a.setBasePlate(false);
            a.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
            a.getEquipment().setChestplate(couro(Material.LEATHER_CHESTPLATE, Color.fromRGB(0x6D4C41)));
            a.getEquipment().setLeggings(couro(Material.LEATHER_LEGGINGS, Color.fromRGB(0x4E342E)));
            a.getEquipment().setBoots(couro(Material.LEATHER_BOOTS, Color.fromRGB(0x3E2723)));
            a.getEquipment().setItemInMainHand(new ItemStack(Material.SPYGLASS));
            a.setRightArmPose(new org.bukkit.util.EulerAngle(Math.toRadians(-100), 0, 0));
        });
        // O baú dele, congelado: quebre o gelo azul para abrir.
        o.por(1, 1, -3, "chest[facing=south]");
        o.por(1, 2, -3, Material.AIR);
        o.por(1, 1, -2, Material.BLUE_ICE);
        o.por(1, 2, -2, Material.BLUE_ICE);
        List<ItemStack> l = new ArrayList<>();
        l.add(diarioExplorador(s));
        l.add(new ItemStack(Material.COMPASS));
        l.add(cartaNautica(1));
        if (o.chance(0.5)) l.add(new ItemStack(Material.SPYGLASS));
        base.bau(o.bloco(1, 1, -3), LootTables.IGLOO_CHEST, o, l);
    }

    private ItemStack diarioExplorador(Sitio s) {
        World w = s.world();
        String rumo = w == null ? null : rumo(w, s.x, s.z, EnumSet.of(Estrutura.ILHA_TARTARUGA, Estrutura.NAVIO_PIRATA, Estrutura.NAUFRAGIO), 3000, s);
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text("Diário do Explorador"));
            m.author(Component.text("Expedição do Norte"));
            m.addPages(Component.text("Dia 3.\n\nO navio ficou preso no gelo. Subimos neste iceberg para ver o horizonte. Nada além de branco e azul."));
            m.addPages(Component.text(rumo != null
                    ? "Dia 9.\n\nPela luneta vi algo " + rumo + ". Talvez seja a salvação. Talvez seja outra coisa."
                    : "Dia 9.\n\nPela luneta, só o mar. Os ursos rondam a entrada à noite."));
            m.addPages(Component.text("Dia 12.\n\nO frio entrou nos ossos. Guardei tudo no baú e deixei a carta náutica. Que alguém termine a viagem por mim."));
        });
        return livro;
    }

    private static ItemStack couro(Material m, Color c) {
        ItemStack i = new ItemStack(m);
        i.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(c));
        return i;
    }

    // =====================================================================
    //  Ilha-Tartaruga
    // =====================================================================

    void tartaruga(Obra o) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -7; z <= 7; z++) {
                double e = (x / 5.2) * (x / 5.2) + (z / 6.2) * (z / 6.2);
                if (e > 1) continue;
                int topo = (int) Math.round(3.4 * Math.sqrt(1 - e));
                // Casco por baixo (a barriga clara) e por cima (placas verdes); o miolo é terra.
                for (int dy = -3; dy <= topo; dy++) {
                    Material m;
                    if (dy == -3) m = Material.YELLOW_TERRACOTTA;
                    else if (dy < 0) m = Material.GREEN_TERRACOTTA;
                    else if (dy == topo) m = e < 0.5 ? (o.chance(0.25) ? Material.MOSS_BLOCK : Material.GRASS_BLOCK) : placa(x, z);
                    else if (dy >= topo - 1 && e >= 0.5) m = placa(x, z);
                    else m = Material.DIRT;
                    o.por(x, dy, z, m);
                }
                if (e < 0.5 && o.chance(0.25)) o.porSeVazio(x, topo + 1, z, o.chance(0.6) ? Material.SHORT_GRASS : Material.FERN);
            }
        }
        // Cabeça, nadadeiras e rabo.
        for (int x = -1; x <= 1; x++) for (int dy = -1; dy <= 1; dy++) o.por(x, dy, 7, Material.GREEN_CONCRETE);
        o.por(-1, 1, 7, Material.BLACK_CONCRETE);
        o.por(1, 1, 7, Material.BLACK_CONCRETE);
        for (int[] p : new int[][]{{5, 3}, {6, 3}, {-5, 3}, {-6, 3}, {5, -3}, {6, -4}, {-5, -3}, {-6, -4}, {0, -7}}) {
            o.por(p[0], 0, p[1], Material.GREEN_TERRACOTTA);
        }
        // Moitas e o tesouro enterrado no alto do casco.
        o.porSeVazio(2, 4, 1, Material.OAK_LEAVES);
        o.porSeVazio(-2, 4, -1, Material.OAK_LEAVES);
        Block bau = o.por(0, 2, 0, "chest[facing=south]");
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.TURTLE_SCUTE, 2 + o.r.nextInt(3)));
        l.add(new ItemStack(Material.NAUTILUS_SHELL, 1 + o.r.nextInt(2)));
        if (o.chance(0.4)) l.add(new ItemStack(Material.HEART_OF_THE_SEA));
        base.bau(bau, LootTables.BURIED_TREASURE, o, l);
    }

    /** As placas do casco (um xadrez de verdes, como os hexágonos de uma tartaruga). */
    private static Material placa(int x, int z) {
        int a = Math.floorDiv(x + (Math.floorMod(Math.floorDiv(z, 2), 2) == 0 ? 0 : 1), 2) + Math.floorDiv(z, 2);
        return Math.floorMod(a, 2) == 0 ? Material.GREEN_TERRACOTTA : Material.LIME_TERRACOTTA;
    }

    // =====================================================================
    //  Navio fantasma (aparece e some: guarda o que havia antes)
    // =====================================================================

    /**
     * Ergue o navio fantasma com a proa para {@code rot} e devolve o que havia antes em cada bloco
     * da área ("x,y,z|dados"), para ele sumir sem deixar rastro.
     */
    public List<String> erguerNavioFantasma(World w, int x, int y, int z, int rot, long semente) {
        List<String> antes = new ArrayList<>();
        Obra o = new Obra(w, x, y, z, rot, new Random(semente));
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -4; dy <= 14; dy++) {
                    Block b = o.bloco(dx, dy, dz);
                    antes.add(b.getX() + "," + b.getY() + "," + b.getZ() + "|" + b.getBlockData().getAsString());
                }
            }
        }
        navio(o, Paleta.FANTASMA, 7, null);
        o.conectar();
        return antes;
    }

    /**
     * Desfaz o navio fantasma: cada bloco que ele mudou volta a ser o que era. O que não mudou
     * fica como está (um baú que já existia ali não perde nada).
     */
    public static void desfazer(World w, List<String> antes) {
        for (String s : antes) {
            int bar = s.indexOf('|');
            String[] c = s.substring(0, bar).split(",");
            Block b = w.getBlockAt(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
            String era = s.substring(bar + 1);
            if (b.getBlockData().getAsString().equals(era)) continue;
            if (b.getState() instanceof org.bukkit.inventory.InventoryHolder h) h.getInventory().clear();
            b.setBlockData(Bukkit.createBlockData(era), false);
        }
    }

    /** Onde ficam (no mundo) os pontos do navio fantasma, para a tripulação nascer. */
    public static Location pontoNavio(World w, int x, int y, int z, int rot, int dx, int dy, int dz) {
        return new Obra(w, x, y, z, rot, null).bloco(dx, dy, dz).getLocation().add(0.5, 0, 0.5);
    }

    // =====================================================================
    //  Vida das estruturas do mar (a cada 2 s, para cada jogador perto)
    // =====================================================================

    void tick(Sitio s, World w, Player p, boolean jogando, double d2) {
        switch (s.tipo) {
            case ILHOTA -> {
                if (s.fundo == 0 && d2 <= 40 * 40) garantirNaufrago(s, w);
                if (jogando && !s.guardas && d2 <= 14 * 14) {
                    s.guardas = true;
                    est.sujo = true;
                    if (s.fundo == 1) piratasDaIlha(s, w);
                    else if (s.fundo == 2) aves(s, w);
                }
            }
            case NAVIO_PIRATA -> {
                if (jogando && !s.guardas && d2 <= 34 * 34) tripulacao(s, w);
                if (jogando && s.fase == 0 && d2 <= 38 * 38 && d2 >= 5 * 5) canhao(s, w, p);
            }
            case ICEBERG -> {
                if (jogando && !s.guardas && d2 <= 16 * 16) ursos(s, w);
            }
            case ILHA_TARTARUGA -> {
                if (s.fase == 1) tartarugaAge(s, w, p, d2);
                else if (s.fase == 0 && d2 <= 30 * 30 && ThreadLocalRandom.current().nextInt(6) == 0) {
                    // A ilha respira: bolhas em volta da cabeça.
                    w.spawnParticle(Particle.BUBBLE_COLUMN_UP, s.ponto(w, 0, 0, 8), 12, 0.6, 0.3, 0.6, 0.05);
                    w.playSound(s.ponto(w, 0, 0, 7), Sound.ENTITY_TURTLE_AMBIENT_LAND, 0.8f, 0.5f);
                }
            }
            default -> { }
        }
    }

    // ---------- Ilhota ----------

    private void garantirNaufrago(Sitio s, World w) {
        Entity e = s.eremita == null ? null : Bukkit.getEntity(s.eremita);
        if (e != null && e.isValid()) return;
        Location onde = s.ponto(w, NAUFRAGO[0], NAUFRAGO[1], NAUFRAGO[2]);
        if (!onde.getChunk().isEntitiesLoaded()) return;
        for (Entity x : w.getNearbyEntities(onde, 8, 6, 8)) {
            if (s.id.equals(x.getPersistentDataContainer().get(K_NAUFRAGO, PersistentDataType.STRING))) {
                s.eremita = x.getUniqueId();
                est.sujo = true;
                return;
            }
        }
        Villager v = w.spawn(onde, Villager.class, m -> {
            m.customName(Component.text("Náufrago " + NAUFRAGOS[Math.floorMod(s.id.hashCode(), NAUFRAGOS.length)], Estrutura.ILHOTA.cor()));
            m.setCustomNameVisible(true);
            m.setAI(false);
            m.setInvulnerable(true);
            m.setPersistent(true);
            m.setRemoveWhenFarAway(false);
            m.setProfession(Villager.Profession.NITWIT);
            m.getPersistentDataContainer().set(K_NAUFRAGO, PersistentDataType.STRING, s.id);
        });
        s.eremita = v.getUniqueId();
        est.sujo = true;
    }

    private void piratasDaIlha(Sitio s, World w) {
        for (int[] p : PIRATAS_ILHA) {
            est.guarda(s, s.ponto(w, p[0], p[1], p[2]), Skeleton.class, "Pirata Esqueleto", NamedTextColor.DARK_GRAY, m -> {
                m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0x212121)));
                m.getEquipment().setHelmetDropChance(0f);
                m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                m.getEquipment().setItemInMainHandDropChance(0.06f);
            });
        }
    }

    private void aves(Sitio s, World w) {
        Parrot.Variant[] cores = Parrot.Variant.values();
        for (int i = 0; i < 4; i++) {
            Location l = s.ponto(w, -2 + i, 4, (i % 2) * 2 - 1);
            w.spawn(l, Parrot.class, pa -> {
                pa.setVariant(cores[ThreadLocalRandom.current().nextInt(cores.length)]);
                pa.setPersistent(true);
                pa.setRemoveWhenFarAway(false);
            });
        }
        w.playSound(s.ponto(w, 0, 3, 0), Sound.ENTITY_PARROT_AMBIENT, 1f, 1.2f);
    }

    // ---------- Navio Pirata ----------

    private static String capitao(Sitio s) {
        return CAPITAES[Math.floorMod(s.id.hashCode(), CAPITAES.length)];
    }

    private void tripulacao(Sitio s, World w) {
        s.guardas = true;
        est.sujo = true;
        for (int i = 0; i < TRIPULACAO.length; i++) {
            int[] p = TRIPULACAO[i];
            Location l = s.ponto(w, p[0], p[1], p[2]);
            if (i % 2 == 0) {
                est.guarda(s, l, Pillager.class, "Pirata", NamedTextColor.DARK_RED, m -> {
                    m.setPatrolLeader(false);
                    m.setCanJoinRaid(false);
                    m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0xB71C1C)));
                    m.getEquipment().setHelmetDropChance(0f);
                });
            } else {
                est.guarda(s, l, Vindicator.class, "Pirata", NamedTextColor.DARK_RED, m -> {
                    m.setPatrolLeader(false);
                    m.setCanJoinRaid(false);
                    m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0x212121)));
                    m.getEquipment().setHelmetDropChance(0f);
                });
            }
        }
        if (s.fase == 0) {
            est.guarda(s, s.ponto(w, CAPITAO[0], CAPITAO[1], CAPITAO[2]), Vindicator.class, capitao(s), NamedTextColor.GOLD, m -> {
                m.setPatrolLeader(false);
                m.setCanJoinRaid(false);
                m.getPersistentDataContainer().set(Estruturas.K_GUARDA, PersistentDataType.STRING, s.id + ":pirata");
                m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
                m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0x1A1A1A)));
                m.getEquipment().setChestplate(couro(Material.LEATHER_CHESTPLATE, Color.fromRGB(0x8B0000)));
                m.getEquipment().setHelmetDropChance(0f);
                m.getEquipment().setChestplateDropChance(0f);
                Estruturas.vida(m, 140);
                m.setCustomNameVisible(true);
            });
        }
        Location centro = s.ponto(w, 0, 4, 0);
        w.playSound(centro, Sound.EVENT_RAID_HORN, 1.5f, 1.2f);
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) <= 50 * 50) {
                o.showTitle(Title.title(Component.text("☠ " + Estrutura.NAVIO_PIRATA.nome() + (s.nome != null ? " " + s.nome : ""), NamedTextColor.DARK_RED, TextDecoration.BOLD),
                        Component.text(capitao(s) + " mandou carregar os canhões!", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
            }
        }
    }

    /** O canhão do lado do jogador atira (bola de fogo que não queima nem quebra bloco). */
    private void canhao(Sitio s, World w, Player p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextInt(3) != 0) return;
        Location alvo = p.getLocation().add(0, 0.8, 0);
        int[] melhor = CANHOES[0];
        double menor = Double.MAX_VALUE;
        for (int[] c : CANHOES) {
            double d = s.ponto(w, c[0], c[1], c[2]).distanceSquared(alvo);
            if (d < menor) {
                menor = d;
                melhor = c;
            }
        }
        Location boca = s.ponto(w, melhor[0], melhor[1], melhor[2]).add(0, 0.5, 0);
        if (boca.getBlock().getType() != Material.DISPENSER) return; // canhão quebrado
        Vector dir = alvo.toVector().subtract(boca.toVector());
        if (dir.lengthSquared() < 1) return;
        dir.normalize();
        Location saida = boca.clone().add(dir.clone().multiply(1.2));
        Vector mira = dir.clone().add(new Vector(r.nextDouble(-0.08, 0.08), r.nextDouble(-0.03, 0.06), r.nextDouble(-0.08, 0.08))).normalize();
        w.spawn(saida, Fireball.class, f -> {
            f.setYield(0f);
            f.setIsIncendiary(false);
            f.setDirection(mira);
            f.setVelocity(mira.clone().multiply(1.1));
            f.getPersistentDataContainer().set(K_BALA, PersistentDataType.BYTE, (byte) 1);
        });
        w.playSound(boca, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.6f);
        w.spawnParticle(Particle.LARGE_SMOKE, saida, 12, 0.3, 0.3, 0.3, 0.02);
    }

    @EventHandler
    public void aoAcertarBala(ProjectileHitEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(K_BALA)) return;
        Location l = e.getEntity().getLocation();
        World w = l.getWorld();
        w.spawnParticle(Particle.EXPLOSION, l, 2, 0.3, 0.3, 0.3, 0);
        w.spawnParticle(Particle.SPLASH, l, 30, 1, 0.3, 1, 0.2);
        w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);
        boolean barco = false;
        for (Entity x : w.getNearbyEntities(l, 2.6, 2.6, 2.6)) {
            if (x instanceof Boat b && BarcoGrande.eBarcoGrande(b) && !barco) {
                plugin.barcoGrande().melhorias().danificar(b, 7, "Uma bala de canhão acertou o casco");
                barco = true;
            } else if (x instanceof Player p && p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                p.damage(5, br.rpgatributos.datapack.Mortes.fonte("canhao", e.getEntity().getShooter() instanceof Entity atirador ? atirador : null));
                p.setVelocity(p.getLocation().toVector().subtract(l.toVector()).setY(0).normalize().multiply(0.6).setY(0.35));
            }
        }
        e.getEntity().remove();
        e.setCancelled(true);
    }

    /** O capitão caiu: os canhões calam e o mapa dele é seu. */
    void capitaoMorreu(EntityDeathEvent e, String id) {
        Sitio s = est.sitio(id);
        if (s != null) {
            s.fase = 1;
            est.sujo = true;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        e.getDrops().add(plugin.mapas().criar(MapasDoTesouro.Tipo.RARO, e.getEntity().getLocation()));
        e.getDrops().add(new ItemStack(Material.GOLD_INGOT, 6 + r.nextInt(7)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 4 + r.nextInt(6)));
        if (r.nextDouble() < 0.3) e.getDrops().add(Raro.MAPA_RASGADO.criar(1));
        if (r.nextDouble() < 0.25) e.getDrops().add(cartaNautica(1));
        e.setDroppedExp(e.getDroppedExp() + 60);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "capitao_pirata", "Derrotou o capitão de um navio pirata");
            plugin.titulos().registrar(quem, "capitaes_piratas", 1);
            quem.sendMessage(Component.text("☠ Os canhões calaram. O mapa do capitão leva a um tesouro enterrado.", COR));
        }
    }

    // ---------- Iceberg ----------

    private void ursos(Sitio s, World w) {
        s.guardas = true;
        est.sujo = true;
        for (int[] p : new int[][]{{-2, 3}, {2, 2}}) {
            Location l = topoLocal(s, w, p[0], p[1]);
            if (l == null) continue;
            est.guarda(s, l, PolarBear.class, "Urso do Gelo", NamedTextColor.WHITE, m -> Estruturas.vida(m, 40));
        }
    }

    /** O chão mais alto numa coluna local (para nascer bicho em cima do gelo). */
    private static Location topoLocal(Sitio s, World w, int x, int z) {
        for (int dy = 14; dy >= -1; dy--) {
            Block b = s.bloco(w, x, dy, z);
            if (b.getType().isSolid()) return b.getLocation().add(0.5, 1, 0.5);
        }
        return null;
    }

    // ---------- Ilha-Tartaruga ----------

    /** A ilha-tartaruga perto deste bloco que ainda dorme. */
    private Sitio ilhaDormindo(Block b) {
        for (Sitio s : est.sitios) {
            if (s.tipo != Estrutura.ILHA_TARTARUGA || s.fase != 0 || !s.mundo.equals(b.getWorld().getName())) continue;
            if (Math.abs(s.x - b.getX()) > 8 || Math.abs(s.z - b.getZ()) > 8 || b.getY() < s.y - 4 || b.getY() > s.y + 6) continue;
            return s;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCavar(BlockBreakEvent e) {
        Sitio s = ilhaDormindo(e.getBlock());
        if (s != null) acordar(s, e.getBlock().getWorld(), e.getPlayer(), "Você cavou fundo demais...");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAcender(BlockIgniteEvent e) {
        if (e.getPlayer() == null) return;
        Sitio s = ilhaDormindo(e.getBlock());
        if (s != null) acordar(s, e.getBlock().getWorld(), e.getPlayer(), "O fogo queimou as costas dela...");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPorFogo(BlockPlaceEvent e) {
        Material m = e.getBlockPlaced().getType();
        if (m != Material.CAMPFIRE && m != Material.SOUL_CAMPFIRE && m != Material.FIRE && m != Material.TORCH && m != Material.LANTERN) return;
        Sitio s = ilhaDormindo(e.getBlockPlaced());
        if (s != null) acordar(s, e.getBlockPlaced().getWorld(), e.getPlayer(), "O calor acordou a ilha...");
    }

    private void acordar(Sitio s, World w, Player p, String frase) {
        s.fase = 1;
        est.sujo = true;
        Location centro = s.ponto(w, 0, 2, 0);
        w.playSound(centro, Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
        w.playSound(centro, Sound.ENTITY_TURTLE_HURT, 2f, 0.4f);
        w.spawnParticle(Particle.BLOCK, centro, 120, 4, 1, 4, Material.GRASS_BLOCK.createBlockData());
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) > 48 * 48) continue;
            o.showTitle(Title.title(Component.text("🐢 A ilha se mexeu!", Estrutura.ILHA_TARTARUGA.cor(), TextDecoration.BOLD),
                    Component.text(frase + " a Tartaruga Ancestral acordou.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(700))));
            if (s.distancia2(o.getLocation()) <= 9 * 9) o.setVelocity(new Vector(0, 0.7, 0));
        }
        if (p != null) plugin.diario().marco(p, "ilha_tartaruga", "Acordou a Tartaruga Ancestral");
        chamarTartaruga(s, w);
    }

    private Turtle tartarugaDe(Sitio s, World w) {
        Location c = s.ponto(w, 0, 0, 0);
        for (Turtle t : w.getNearbyEntitiesByType(Turtle.class, c, 48)) {
            String v = t.getPersistentDataContainer().get(Estruturas.K_GUARDA, PersistentDataType.STRING);
            if ((s.id + ":tartaruga").equals(v)) return t;
        }
        return null;
    }

    private Turtle chamarTartaruga(Sitio s, World w) {
        Location l = s.ponto(w, TARTARUGA[0], TARTARUGA[1], TARTARUGA[2]);
        Turtle t = w.spawn(l, Turtle.class, m -> {
            m.customName(Component.text("Tartaruga Ancestral", Estrutura.ILHA_TARTARUGA.cor(), TextDecoration.BOLD));
            m.setCustomNameVisible(true);
            m.setAdult();
            m.setPersistent(false);
            m.setRemoveWhenFarAway(true);
            m.getPersistentDataContainer().set(Estruturas.K_GUARDA, PersistentDataType.STRING, s.id + ":tartaruga");
            AttributeInstance escala = m.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(5.5);
            AttributeInstance armadura = m.getAttribute(Attribute.ARMOR);
            if (armadura != null) armadura.setBaseValue(12);
            AttributeInstance vel = m.getAttribute(Attribute.MOVEMENT_SPEED);
            if (vel != null) vel.setBaseValue(0.35);
            Estruturas.vida(m, 400);
        });
        plugin.perigo().envelhecer(t);
        return t;
    }

    private void tartarugaAge(Sitio s, World w, Player p, double d2) {
        if (d2 > 48 * 48) return;
        Turtle t = tartarugaDe(s, w);
        if (t == null) {
            if (!s.ponto(w, 0, 0, 0).getChunk().isEntitiesLoaded()) return;
            t = chamarTartaruga(s, w);
        }
        BossBar barra = barras.computeIfAbsent(s.id, k -> BossBar.bossBar(Component.text("Tartaruga Ancestral", Estrutura.ILHA_TARTARUGA.cor()), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10));
        AttributeInstance max = t.getAttribute(Attribute.MAX_HEALTH);
        barra.progress((float) Math.max(0, Math.min(1, t.getHealth() / (max == null ? 400 : max.getValue()))));
        p.showBossBar(barra);
        // Longe demais da ilha: volta para perto da cabeça.
        Location casa = s.ponto(w, 0, -1, 9);
        if (t.getLocation().distanceSquared(casa) > 24 * 24) t.teleport(casa);
        // O tick passa uma vez por jogador perto: a tartaruga age no máximo a cada 2 s.
        long agora = System.currentTimeMillis();
        if (agora - ultimoGolpe.getOrDefault(s.id, 0L) < 1900) return;
        ultimoGolpe.put(s.id, agora);
        int n = golpes.merge(s.id, 1, Integer::sum);
        Location c = t.getLocation();
        for (Player o : w.getPlayers()) {
            if (o.getGameMode() == org.bukkit.GameMode.CREATIVE || o.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (o.getLocation().distanceSquared(c) <= 6 * 6) {
                o.damage(6, br.rpgatributos.datapack.Mortes.fonte("tartaruga", t));
                o.setVelocity(o.getLocation().toVector().subtract(c.toVector()).setY(0).normalize().multiply(1.1).setY(0.45));
            }
        }
        if (n % 4 == 0) onda(s, w, t);
    }

    /** A onda: empurra quem está perto e bate nos barcos. */
    private void onda(Sitio s, World w, Turtle t) {
        Location c = t.getLocation();
        w.playSound(c, Sound.ENTITY_GENERIC_SPLASH, 2f, 0.5f);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12;
            w.spawnParticle(Particle.SPLASH, c.clone().add(Math.cos(a) * 5, 0.5, Math.sin(a) * 5), 8, 0.3, 0.2, 0.3, 0.2);
        }
        for (Entity x : w.getNearbyEntities(c, 14, 6, 14)) {
            if (x instanceof Boat b) {
                if (BarcoGrande.eBarcoGrande(b)) plugin.barcoGrande().melhorias().danificar(b, 4, "A onda da Tartaruga bateu no barco");
                b.setVelocity(b.getLocation().toVector().subtract(c.toVector()).setY(0).normalize().multiply(0.9));
            } else if (x instanceof Player o && o.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                o.setVelocity(o.getLocation().toVector().subtract(c.toVector()).setY(0).normalize().multiply(1.2).setY(0.5));
                o.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
            }
        }
    }

    void tartarugaMorreu(EntityDeathEvent e, String id) {
        Sitio s = est.sitio(id);
        if (s != null) {
            s.fase = 2;
            est.sujo = true;
        }
        BossBar barra = barras.remove(id);
        if (barra != null) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
        golpes.remove(id);
        ultimoGolpe.remove(id);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        e.getDrops().clear();
        e.getDrops().add(new ItemStack(Material.TURTLE_SCUTE, 8 + r.nextInt(6)));
        e.getDrops().add(new ItemStack(Material.NAUTILUS_SHELL, 3 + r.nextInt(3)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 12 + r.nextInt(12)));
        if (r.nextDouble() < 0.5) e.getDrops().add(new ItemStack(Material.HEART_OF_THE_SEA));
        e.getDrops().add(Raro.PEDRA_DE_PROTECAO.criar(1));
        e.setDroppedExp(150);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "tartaruga_ancestral", "Venceu a Tartaruga Ancestral");
            plugin.titulos().registrar(quem, "tartaruga_ancestral", 1);
            Bukkit.broadcast(Component.text("🐢 ", Estrutura.ILHA_TARTARUGA.cor()).append(Component.text(quem.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" venceu a Tartaruga Ancestral! A ilha voltou a dormir.", NamedTextColor.GRAY)));
        }
    }

    // =====================================================================
    //  O náufrago e a Carta Náutica
    // =====================================================================

    /** Carta Náutica: aponta o lugar do mar mais perto que você ainda não viu. */
    public static ItemStack cartaNautica(int qtd) {
        ItemStack i = new ItemStack(Material.PAPER, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Carta Náutica", COR));
            m.lore(List.of(
                    Component.text("Clique com o botão direito: marca o lugar do mar", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("mais perto que você ainda não visitou.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setItemModel(NamespacedKey.minecraft("map"));
            m.getPersistentDataContainer().set(K_CARTA, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    public static boolean eCarta(ItemStack i) {
        return i != null && i.getType() == Material.PAPER && i.hasItemMeta() && i.getPersistentDataContainer().has(K_CARTA);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoLerCarta(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!eCarta(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Sitio melhor = null;
        double best = 4000.0 * 4000.0;
        for (Sitio s : est.sitios) {
            if (!DO_MAR.contains(s.tipo) || !s.mundo.equals(p.getWorld().getName()) || s.visitantes.contains(p.getUniqueId())) continue;
            if (s.tipo == Estrutura.CIDADE_SUBMERSA && !CidadesSubmersas.pronta(s)) continue;
            double d2 = s.distancia2(p.getLocation());
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        if (melhor == null) {
            p.sendActionBar(Component.text("⚓ A carta está em branco por estas águas. Tente em outro mar.", NamedTextColor.GRAY));
            return;
        }
        ItemStack mao = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        String nome = melhor.tipo == Estrutura.NAUFRAGIO && melhor.nome != null ? "o naufrágio do " + melhor.nome
                : melhor.tipo == Estrutura.NAVIO_PIRATA && melhor.nome != null ? "o navio pirata " + melhor.nome
                : melhor.tipo.comArtigoIndefinido();
        p.sendMessage(Component.text("⚓ A Carta Náutica marca " + nome + " " + direcao(p.getLocation().getX(), p.getLocation().getZ(), melhor)
                + ", a uns " + Math.max(10, Math.round(Math.sqrt(best) / 10.0) * 10) + " blocos.", COR));
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.9f);
    }

    private static String direcao(double x, double z, Sitio s) {
        int setor = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-(s.z - z), s.x - x)) / 45.0), 8);
        return Estruturas.DIRECOES[setor];
    }

    /** "ao norte, a uns 400 blocos" do lugar do tipo pedido mais perto (sem contar {@code fora}). */
    String rumo(World w, int x, int z, Set<Estrutura> tipos, double max, Sitio fora) {
        Sitio melhor = null;
        double best = max * max;
        for (Sitio s : est.sitios) {
            if (s == fora || !tipos.contains(s.tipo) || !s.mundo.equals(w.getName())) continue;
            double dx = s.x - x, dz = s.z - z, d2 = dx * dx + dz * dz;
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        if (melhor == null) return null;
        return direcao(x, z, melhor) + ", a uns " + Math.max(50, Math.round(Math.sqrt(best) / 50.0) * 50) + " blocos daqui";
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoFalarNaufrago(PlayerInteractEntityEvent e) {
        String id = e.getRightClicked().getPersistentDataContainer().get(K_NAUFRAGO, PersistentDataType.STRING);
        if (id == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Sitio s = est.sitio(id);
        String nome = "Náufrago " + NAUFRAGOS[Math.floorMod(id.hashCode(), NAUFRAGOS.length)];
        ItemStack mao = p.getInventory().getItemInMainHand();
        String ja = p.getPersistentDataContainer().getOrDefault(K_ATENDIDO, PersistentDataType.STRING, "");
        boolean atendido = List.of(ja.split(",")).contains(id);
        String fala;
        if (atendido) {
            fala = "Obrigado de novo pela comida. Quando passar um barco, eu grito!";
        } else if (mao.getType().isEdible()) {
            if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
            p.getPersistentDataContainer().set(K_ATENDIDO, PersistentDataType.STRING, ja.isEmpty() ? id : ja + "," + id);
            for (ItemStack sobra : p.getInventory().addItem(cartaNautica(2)).values()) p.getWorld().dropItem(p.getLocation(), sobra);
            String rumo = s == null ? null : rumo(e.getRightClicked().getWorld(), s.x, s.z, EnumSet.of(Estrutura.NAVIO_PIRATA, Estrutura.ILHA_TARTARUGA, Estrutura.NAUFRAGIO), 3000, s);
            fala = "Comida de verdade! Tome, as cartas do meu barco. " + (rumo != null ? "Da última vez que vi terra firme... não, era um navio, " + rumo + "." : "Não sei ler, mas parecem importantes.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            plugin.diario().marco(p, "naufrago", "Deu comida a um náufrago numa ilhota");
        } else {
            String[] linhas = {"Faz quarenta e um dias que só como coco. Você não teria... comida?",
                    "Meu barco virou numa tempestade. Os peixes aqui riem de mim.",
                    "Se me trouxer algo de comer, te dou as cartas do meu barco."};
            fala = linhas[ThreadLocalRandom.current().nextInt(linhas.length)];
        }
        p.sendMessage(Component.text(nome + ": ", Estrutura.ILHOTA.cor(), TextDecoration.BOLD)
                .append(Component.text("\"" + fala + "\"", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
    }

    /** Quem achou a estrutura do mar: avisos curtos de como ela funciona. */
    void descobriu(Player p, Sitio s) {
        String dica = switch (s.tipo) {
            case ILHOTA -> s.fundo == 0 ? "Alguém acena da praia. Leve comida para o náufrago."
                    : s.fundo == 1 ? "Um X vermelho na areia... o que será que tem embaixo?" : "Ninhos para todo lado. As aves não gostam de visita.";
            case NAVIO_PIRATA -> "Cuidado com os canhões. Derrube o capitão para calá-los.";
            case ICEBERG -> "Algo brilha dentro do gelo. Os ursos não vão gostar.";
            case BARCO_MERCANTE -> "O mercador vende o que serve no mar. Clique nele.";
            case ILHA_TARTARUGA -> "Uma ilha redonda demais. Dizem que tem um tesouro no alto... e que não se deve cavar.";
            default -> null;
        };
        if (dica != null) p.sendMessage(Component.text("⚓ " + dica, COR));
    }

    /** Na saída: tira as barras de vida. */
    public void desligar() {
        for (BossBar b : barras.values()) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(b);
        barras.clear();
    }

    /** Tira a barra da tartaruga de quem se afastou. */
    void esconderBarras() {
        for (Map.Entry<String, BossBar> en : barras.entrySet()) {
            Sitio s = est.sitio(en.getKey());
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (s == null || !p.getWorld().getName().equals(s.mundo) || s.distancia2(p.getLocation()) > 52 * 52) p.hideBossBar(en.getValue());
            }
        }
    }
}
