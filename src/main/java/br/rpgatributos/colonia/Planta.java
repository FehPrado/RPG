package br.rpgatributos.colonia;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um projeto de construção da colônia (a "planta" do MineColonies): o desenho em camadas, de baixo
 * para cima, lido de plantas/*.yml. Em cada camada, a primeira linha é o FUNDO e a última é a FRENTE
 * (a entrada); as colunas vão da esquerda para a direita de quem olha a frente de fora.
 * Na planta, "south" é o lado da entrada. '.' = ar (o Construtor limpa) e '~' = não mexe.
 */
public final class Planta {

    /** O que a construção faz quando fica pronta (além dos blocos que ela tem). */
    public enum Efeito {
        CASA("Casa: camas para os moradores", null),
        TORRE_DE_VIGIA("Aumenta o território em volta dela (mais a cada nível)", null),
        ARMAZEM("Os baús dela viram depósito da colônia", null),
        BIBLIOTECA("Bibliotecário produz +10% por nível", Profissao.BIBLIOTECARIO),
        QUARTEL("Alvos (quartel) e camas para os soldados", null),
        FAZENDA("Fazendeiro produz +10% por nível", Profissao.FAZENDEIRO),
        PRACA("Colônia +5% de felicidade por nível", null),
        OFICINA("Vagas de Construtor: +1 por nível", Profissao.CONSTRUTOR),
        CABANA_LENHADOR("Lenhador produz +10% por nível", Profissao.LENHADOR),
        CABANA_PESCADOR("Pescador produz +10% por nível (precisa de água perto)", Profissao.PESCADOR),
        MINA("Minerador produz +10% por nível", Profissao.MINERADOR),
        MERCADO("Mercador vende +10% por nível", Profissao.MERCADOR),
        FERRARIA("Ferreiro trabalha +10% por nível (faça a Forja na bigorna)", Profissao.FERREIRO),
        TAVERNA("Cozinheiro produz +10% por nível (faça a Cozinha no defumador)", Profissao.COZINHEIRO),
        ESTABULO("Tratador produz +10% por nível (faça o Altar no fardo de feno)", Profissao.TRATADOR),
        LABORATORIO("Alquimista produz +10% por nível (faça a Bancada no suporte)", Profissao.ALQUIMISTA),
        NENHUM("Decoração", null);

        private final String texto;
        private final Profissao trabalhador;

        Efeito(String texto, Profissao trabalhador) {
            this.texto = texto;
            this.trabalhador = trabalhador;
        }

        public String texto() { return texto; }
        /** Quem trabalha melhor com essa construção na colônia (null = ninguém). */
        public Profissao trabalhador() { return trabalhador; }
    }

    /** Um bloco do desenho, em coordenadas da planta. */
    record Bloco(int lx, int ly, int lz, BlockData dados, Material item, int qtd, boolean fragil) { }

    /** Giro da planta: na rotação r, a frente (south da planta) olha para GIRO[r] no mundo. */
    static final BlockFace[] GIRO = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
    private static final BlockFace[] HORIZONTAIS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final BlockData AR = Material.AIR.createBlockData();

    private final String id, nome;
    private final List<String> descricao;
    private final Material icone;
    private final int nivel;
    private final Efeito efeito;
    private final int largura, profundidade, altura;
    /** [ly][lz][lx]: null = não mexe; ar = limpar. */
    private final BlockData[][][] grade;
    /** Ordem em que o Construtor põe os blocos: camada por camada, e o que precisa de apoio por último. */
    private final List<Bloco> ordem;
    private final Map<Material, Integer> materiais;
    /** O mínimo de cada nível da construção (o 1º é o que quem constrói do próprio jeito precisa atender). */
    private final List<Vistoria.Requisito> niveis;

    private Planta(String id, String nome, List<String> descricao, Material icone, int nivel, Efeito efeito, BlockData[][][] grade,
                   List<Vistoria.Requisito> niveis) {
        this.niveis = niveis;
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.icone = icone;
        this.nivel = nivel;
        this.efeito = efeito;
        this.grade = grade;
        this.altura = grade.length;
        this.profundidade = grade[0].length;
        this.largura = grade[0][0].length;
        List<Bloco> firmes = new ArrayList<>(), frageis = new ArrayList<>();
        Map<Material, Integer> mats = new EnumMap<>(Material.class);
        for (int ly = 0; ly < altura; ly++) {
            for (int lz = 0; lz < profundidade; lz++) {
                for (int lx = 0; lx < largura; lx++) {
                    BlockData d = grade[ly][lz][lx];
                    if (d == null || d.getMaterial().isAir()) continue;
                    Material item = item(d);
                    int qtd = item == null ? 0 : (d instanceof Slab s && s.getType() == Slab.Type.DOUBLE ? 2 : 1);
                    boolean fragil = fragil(d.getMaterial());
                    (fragil ? frageis : firmes).add(new Bloco(lx, ly, lz, d, item, qtd, fragil));
                    if (item != null) mats.merge(item, qtd, Integer::sum);
                }
            }
        }
        List<Bloco> l = new ArrayList<>(firmes);
        l.addAll(frageis);
        this.ordem = Collections.unmodifiableList(l);
        this.materiais = Collections.unmodifiableMap(mats);
    }

    public String id() { return id; }
    public String nome() { return nome; }
    public List<String> descricao() { return descricao; }
    public Material icone() { return icone; }
    public int nivel() { return nivel; }
    public Efeito efeito() { return efeito; }
    public int largura() { return largura; }
    public int profundidade() { return profundidade; }
    public int altura() { return altura; }
    List<Bloco> ordem() { return ordem; }
    /** Tudo o que a obra inteira gasta. */
    public Map<Material, Integer> materiais() { return materiais; }

    public List<Vistoria.Requisito> niveis() { return niveis; }

    /** Quantos níveis a construção tem (pelo menos 1). */
    public int nivelMaximo() { return Math.max(1, niveis.size()); }

    /** A vistoria do próprio desenho (o que o Construtor deixa pronto). */
    Vistoria vistoriaDoDesenho() {
        Vistoria v = new Vistoria();
        for (int ly = 0; ly < altura; ly++) {
            for (int lz = 0; lz < profundidade; lz++) {
                for (int lx = 0; lx < largura; lx++) {
                    BlockData d = grade[ly][lz][lx];
                    if (d != null) v.contar(ly, d, true);
                }
            }
        }
        for (int lz = 1; lz < profundidade - 1; lz++) {
            for (int lx = 1; lx < largura - 1; lx++) {
                boolean coberta = false;
                for (int ly = 2; ly < altura && !coberta; ly++) {
                    BlockData d = grade[ly][lz][lx];
                    coberta = d != null && d.getMaterial().isSolid();
                }
                v.coluna(coberta);
            }
        }
        return v;
    }

    /** O que tem nesse lugar do desenho (null = não mexe). */
    BlockData em(int lx, int ly, int lz) { return grade[ly][lz][lx]; }

    // =====================================================================
    //  Ler do arquivo
    // =====================================================================

    /** Lê uma planta. @throws IllegalArgumentException com o motivo, se o desenho estiver errado. */
    static Planta ler(String id, ConfigurationSection s) {
        String nome = s.getString("nome", id);
        Material icone = Material.matchMaterial(s.getString("icone", "PAPER"));
        if (icone == null || !icone.isItem()) icone = Material.PAPER;
        Efeito efeito;
        try {
            efeito = Efeito.valueOf(s.getString("efeito", "NENHUM").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("efeito desconhecido: " + s.getString("efeito"));
        }
        Map<Character, BlockData> legenda = new HashMap<>();
        legenda.put('.', AR);
        legenda.put('~', null);
        ConfigurationSection leg = s.getConfigurationSection("legenda");
        if (leg == null) throw new IllegalArgumentException("falta a legenda");
        for (String k : leg.getKeys(false)) {
            if (k.length() != 1) throw new IllegalArgumentException("a legenda usa uma letra por bloco (\"" + k + "\")");
            if (k.equals(".") || k.equals("~")) throw new IllegalArgumentException("'.' e '~' já são ar e \"não mexe\"");
            String txt = leg.getString(k, "");
            try {
                legenda.put(k.charAt(0), Bukkit.createBlockData(txt.contains(":") ? txt : "minecraft:" + txt));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("bloco inválido na legenda (" + k + ": " + txt + ")");
            }
        }
        List<?> camadas = s.getList("camadas");
        if (camadas == null || camadas.isEmpty()) throw new IllegalArgumentException("falta o desenho (camadas)");
        int h = camadas.size(), d = -1, w = -1;
        BlockData[][][] grade = new BlockData[h][][];
        for (int ly = 0; ly < h; ly++) {
            if (!(camadas.get(ly) instanceof List<?> linhas) || linhas.isEmpty()) throw new IllegalArgumentException("camada " + ly + " vazia");
            if (d < 0) d = linhas.size();
            if (linhas.size() != d) throw new IllegalArgumentException("camada " + ly + " tem " + linhas.size() + " linhas (as outras têm " + d + ")");
            grade[ly] = new BlockData[d][];
            for (int lz = 0; lz < d; lz++) {
                String linha = String.valueOf(linhas.get(lz));
                if (w < 0) w = linha.length();
                if (linha.length() != w) throw new IllegalArgumentException("camada " + ly + ", linha " + (lz + 1) + ": largura " + linha.length() + " (esperado " + w + ")");
                grade[ly][lz] = new BlockData[w];
                for (int lx = 0; lx < w; lx++) {
                    char ch = linha.charAt(lx);
                    if (!legenda.containsKey(ch)) throw new IllegalArgumentException("letra '" + ch + "' fora da legenda (camada " + ly + ")");
                    grade[ly][lz][lx] = legenda.get(ch);
                }
            }
        }
        if (w < 3 || d < 3) throw new IllegalArgumentException("a planta precisa ter pelo menos 3x3");
        List<String> descricao = List.copyOf(s.getStringList("descricao"));
        List<Vistoria.Requisito> niveis = new ArrayList<>();
        List<?> reqs = s.getList("niveis");
        if (reqs != null) {
            for (Object o : reqs) {
                if (!(o instanceof Map<?, ?> m)) throw new IllegalArgumentException("niveis: cada nível é uma lista de requisitos");
                org.bukkit.configuration.MemoryConfiguration sec = new org.bukkit.configuration.MemoryConfiguration();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getValue() instanceof Map<?, ?> sub) sec.createSection(String.valueOf(e.getKey()), sub);
                    else sec.set(String.valueOf(e.getKey()), e.getValue());
                }
                niveis.add(Vistoria.Requisito.ler(sec));
            }
        }
        return new Planta(id, nome, descricao, icone, Math.max(1, s.getInt("nivel", 1)), efeito, grade, Collections.unmodifiableList(niveis));
    }

    // =====================================================================
    //  Material de cada bloco
    // =====================================================================

    private static final Map<Material, Material> ITEM_ESPECIAL = new LinkedHashMap<>();

    static {
        ITEM_ESPECIAL.put(Material.WHEAT, Material.WHEAT_SEEDS);
        ITEM_ESPECIAL.put(Material.CARROTS, Material.CARROT);
        ITEM_ESPECIAL.put(Material.POTATOES, Material.POTATO);
        ITEM_ESPECIAL.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        ITEM_ESPECIAL.put(Material.FARMLAND, Material.DIRT);
        ITEM_ESPECIAL.put(Material.DIRT_PATH, Material.DIRT);
        ITEM_ESPECIAL.put(Material.GRASS_BLOCK, Material.DIRT);
        ITEM_ESPECIAL.put(Material.TRIPWIRE, Material.STRING);
        ITEM_ESPECIAL.put(Material.REDSTONE_WIRE, Material.REDSTONE);
    }

    /** O item que o Construtor gasta para pôr esse bloco (null = de graça, como a metade de cima da porta). */
    static Material item(BlockData d) {
        Material m = d.getMaterial();
        if (m.isAir() || m == Material.WATER || m == Material.LAVA) return null;
        if (d instanceof Bed b && b.getPart() == Bed.Part.HEAD) return null;
        if (d instanceof Bisected b && !(d instanceof Stairs) && !(d instanceof TrapDoor) && b.getHalf() == Bisected.Half.TOP) return null;
        Material especial = ITEM_ESPECIAL.get(m);
        if (especial != null) return especial;
        if (m.isItem()) return m;
        String n = m.name();
        if (n.startsWith("POTTED_")) return Material.FLOWER_POT;
        if (n.contains("WALL_")) {
            Material sem = Material.matchMaterial(n.replace("WALL_", ""));
            if (sem != null && sem.isItem()) return sem;
        }
        return null;
    }

    /** Precisa de apoio (vai depois das paredes): tochas, portas, camas, tapetes, lanternas, plantas... */
    static boolean fragil(Material m) {
        if (!m.isSolid()) return true;
        String n = m.name();
        return n.endsWith("_DOOR") || n.endsWith("_BED") || n.endsWith("LANTERN") || n.endsWith("_CARPET") || n.endsWith("_BANNER")
                || n.endsWith("_SIGN") || n.startsWith("POTTED_") || m == Material.FLOWER_POT || n.endsWith("_PRESSURE_PLATE");
    }

    // =====================================================================
    //  Girar
    // =====================================================================

    /** Direção da planta → direção no mundo, na rotação {@code rot}. */
    static BlockFace face(BlockFace f, int rot) {
        for (int i = 0; i < 4; i++) {
            if (GIRO[i] == f) return GIRO[(i + rot) % 4];
        }
        return f;
    }

    /** Deslocamento (dx, dz) da planta → deslocamento no mundo, na rotação {@code rot}. */
    static int[] girar(int dx, int dz, int rot) {
        return switch (rot) {
            case 1 -> new int[]{-dz, dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }

    /** Uma cópia dos dados do bloco girada para o mundo. */
    static BlockData girar(BlockData original, int rot) {
        BlockData d = original.clone();
        if (rot == 0) return d;
        if (d instanceof Directional dir) {
            BlockFace f = face(dir.getFacing(), rot);
            if (dir.getFaces().contains(f)) dir.setFacing(f);
        }
        if (d instanceof Orientable o && rot % 2 == 1 && o.getAxis() != Axis.Y) {
            Axis novo = o.getAxis() == Axis.X ? Axis.Z : Axis.X;
            if (o.getAxes().contains(novo)) o.setAxis(novo);
        }
        if (d instanceof Rotatable ro) ro.setRotation(face(ro.getRotation(), rot));
        if (d instanceof MultipleFacing mf && !(d instanceof Fence) && !(d instanceof GlassPane)) {
            List<BlockFace> ligadas = new ArrayList<>();
            for (BlockFace f : HORIZONTAIS) if (mf.getAllowedFaces().contains(f) && mf.hasFace(f)) ligadas.add(f);
            for (BlockFace f : ligadas) mf.setFace(f, false);
            for (BlockFace f : ligadas) mf.setFace(face(f, rot), true);
        }
        return d;
    }
}
