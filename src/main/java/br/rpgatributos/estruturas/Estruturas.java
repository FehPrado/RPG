package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.fe.Deus;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Bebida;
import br.rpgatributos.vida.Carta;
import br.rpgatributos.vida.Erva;
import br.rpgatributos.vida.Frascos;
import br.rpgatributos.vida.Fruta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Camel;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Estruturas prontas pelo mundo: torres de vigia, acampamentos de bandidos, cemitérios,
 * minas abandonadas, poços dos desejos, cabanas de eremita e santuários esquecidos.
 *
 * Só nascem em chunks novos (terra que ninguém pisou), fora de territórios, longe umas das
 * outras e das estruturas do próprio Minecraft, e só onde o chão é firme e quase plano.
 * Cada uma cabe dentro de um chunk, então nunca mexe no vizinho.
 */
public final class Estruturas implements Listener {

    public static final TextColor COR = TextColor.color(0xA1887F);
    public static final NamespacedKey K_GUARDA = new NamespacedKey("rpgatributos", "guarda_estrutura");
    public static final NamespacedKey K_EREMITA = new NamespacedKey("rpgatributos", "eremita");

    private static final String[] NOMES_EREMITA = {"Aldo", "Tobias", "Celeste", "Matias", "Odete", "Bento", "Irene", "Joaquim", "Lúcia", "Severino"};
    public static final String[] DIRECOES = {"a leste", "ao nordeste", "ao norte", "ao noroeste", "a oeste", "ao sudoeste", "ao sul", "ao sudeste"};
    private static final String[] FALAS = {
            "O Rei Caído não caiu sozinho. Junte as moedas dele e as perguntas certas aparecem.",
            "As masmorras respiram. Quanto mais fundo, mais elas lembram de quem entrou.",
            "Ferrum gosta de ferro, Sylva de colheita... e Mortis de quem não tem medo.",
            "Na lua cheia os bichos ficam valentes. Eu fico em casa.",
            "Os mapas rasgados não mentem. Só não contam tudo.",
            "Já vi um mercador que some no ar. Vende o que ninguém mais vende.",
            "Os vigias das torres sabiam de passagens antigas. Levaram o segredo com eles.",
            "Plante uma muda na estação certa e o pomar te paga por anos.",
            "Quem joga esmeralda no poço não compra sorte. Pede. É diferente.",
            "Os bandidos não são daqui. Vieram depois que o reino caiu."};

    private record Desejo(String nome, String descricao, PotionEffectType tipo, int segundos, int nivel) { }

    private static final List<Desejo> DESEJOS = List.of(
            new Desejo("Sorte", "Sorte II por 20 min", PotionEffectType.LUCK, 1200, 1),
            new Desejo("Vigor", "+2 ❤ por 20 min", PotionEffectType.HEALTH_BOOST, 1200, 0),
            new Desejo("Ligeireza", "Velocidade por 15 min", PotionEffectType.SPEED, 900, 0),
            new Desejo("Mãos Hábeis", "Pressa por 15 min", PotionEffectType.HASTE, 900, 0),
            new Desejo("Bom Nome", "Herói da Vila por 30 min (descontos com aldeões)", PotionEffectType.HERO_OF_THE_VILLAGE, 1800, 0),
            new Desejo("Fôlego", "Respiração aquática por 20 min", PotionEffectType.WATER_BREATHING, 1200, 0),
            new Desejo("Olhos de Coruja", "Visão noturna por 20 min", PotionEffectType.NIGHT_VISION, 1200, 0));

    private final RPGAtributos plugin;
    private final Projetos projetos;
    final CidadesSubmersas cidades;
    private final File arquivo;
    private final NamespacedKey kChunk, kPoco, kAltar;
    final List<Sitio> sitios = new ArrayList<>();
    private final Map<UUID, Long> ultimaFala = new HashMap<>();
    /** Trocas da bruxa (que não é aldeã), uma por cabana por dia. */
    private final Map<String, Merchant> mercadores = new HashMap<>();
    private final Map<String, Integer> recusas = new java.util.TreeMap<>();
    boolean sujo;
    private int ciclo;

    public Estruturas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.projetos = new Projetos(plugin);
        this.cidades = new CidadesSubmersas(plugin, this, projetos);
        this.arquivo = new File(plugin.getDataFolder(), "estruturas.yml");
        this.kChunk = new NamespacedKey(plugin, "estrutura_v1");
        this.kPoco = new NamespacedKey(plugin, "poco_desejo_dia");
        this.kAltar = new NamespacedKey(plugin, "santuario_esquecido_dia");
    }

    private static long hoje() {
        return LocalDate.now().toEpochDay();
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        sitios.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection sec = y.getConfigurationSection("sitios");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(id);
            Estrutura t = c == null ? null : Estrutura.porId(c.getString("tipo", ""));
            if (t == null) continue;
            Sitio s = new Sitio(id, t, c.getString("mundo", "world"), c.getInt("x"), c.getInt("y"), c.getInt("z"), c.getInt("rot"));
            s.fundo = c.getInt("fundo");
            s.deus = Deus.porId(c.getString("deus", ""));
            if (t == Estrutura.SANTUARIO_ESQUECIDO && s.deus == null) s.deus = Deus.SYLVA;
            s.guardas = c.getBoolean("guardas");
            s.ronda = c.getLong("ronda");
            String er = c.getString("eremita");
            if (er != null) {
                try { s.eremita = UUID.fromString(er); } catch (IllegalArgumentException ignored) { }
            }
            s.diaOfertas = c.getLong("dia-ofertas", -1);
            s.acesoAte = c.getLong("aceso-ate");
            s.nome = c.getString("nome");
            s.fase = c.getInt("fase");
            s.semente = c.getLong("semente");
            s.chefeEm = c.getLong("chefe-em");
            for (String v : c.getStringList("visitantes")) {
                try { s.visitantes.add(UUID.fromString(v)); } catch (IllegalArgumentException ignored) { }
            }
            sitios.add(s);
        }
        // Termina as cidades submersas que ficaram pela metade (depois que os mundos carregam).
        Bukkit.getScheduler().runTaskLater(plugin, cidades::retomar, 100L);
    }

    void adicionar(Sitio s) {
        sitios.add(s);
        salvar();
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Sitio s : sitios) {
            String b = "sitios." + s.id + ".";
            y.set(b + "tipo", s.tipo.id());
            y.set(b + "mundo", s.mundo);
            y.set(b + "x", s.x);
            y.set(b + "y", s.y);
            y.set(b + "z", s.z);
            y.set(b + "rot", s.rot);
            if (s.fundo != 0) y.set(b + "fundo", s.fundo);
            if (s.deus != null) y.set(b + "deus", s.deus.id());
            if (s.guardas) y.set(b + "guardas", true);
            if (s.ronda != 0) y.set(b + "ronda", s.ronda);
            if (s.eremita != null) y.set(b + "eremita", s.eremita.toString());
            if (s.diaOfertas >= 0) y.set(b + "dia-ofertas", s.diaOfertas);
            if (s.acesoAte > 0) y.set(b + "aceso-ate", s.acesoAte);
            if (s.nome != null) y.set(b + "nome", s.nome);
            if (s.fase != 0) y.set(b + "fase", s.fase);
            if (s.semente != 0) y.set(b + "semente", s.semente);
            if (s.chefeEm != 0) y.set(b + "chefe-em", s.chefeEm);
            if (!s.visitantes.isEmpty()) y.set(b + "visitantes", s.visitantes.stream().map(UUID::toString).toList());
        }
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar estruturas.yml: " + ex.getMessage());
        }
    }

    public int quantas() {
        return sitios.size();
    }

    // =====================================================================
    //  Onde nascem
    // =====================================================================

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        if (!plugin.settings().estrAtivadas) return;
        Chunk c = e.getChunk();
        World w = c.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || plugin.masmorras().ehMundo(w)) return;
        boolean novo = e.isNewChunk();
        if (!novo && plugin.settings().estrSoChunksNovos) return;
        var pdc = c.getPersistentDataContainer();
        if (pdc.has(kChunk)) return;
        pdc.set(kChunk, PersistentDataType.BYTE, (byte) 1);
        if (novo) cidades.talvez(c);
        Random r =new Random(w.getSeed() ^ (c.getX() * 0x9E3779B97F4A7C15L) ^ (c.getZ() * 0xC2B2AE3D27D4EB4FL) ^ 0x57A0L);
        if (r.nextDouble() >= plugin.settings().estrChance) return;
        // Até 3 tipos diferentes: se o chão não serve para um, talvez sirva para outro.
        List<Estrutura> tipos = new ArrayList<>();
        for (int i = 0; i < 12 && tipos.size() < 3; i++) {
            Estrutura t = Estrutura.sortear(r);
            if (!tipos.contains(t)) tipos.add(t);
        }
        int rot = r.nextInt(4);
        long semente = r.nextLong();
        int x = c.getX() * 16 + 8, z = c.getZ() * 16 + 8;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!c.isLoaded()) return;
            for (Estrutura t : tipos) if (construir(w, x, z, t, rot, new Random(semente), false, !novo) != null) return;
        });
    }

    /** O chão firme da coluna (pula troncos, folhas e plantas); null se for água ou lava. */
    private static Block chao(World w, int x, int z) {
        int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        for (int i = 0; i < 48 && y > w.getMinHeight(); i++, y--) {
            Block b = w.getBlockAt(x, y, z);
            if (b.isLiquid() || b.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged()) return null;
            if (ehChao(b.getType())) return b;
        }
        return null;
    }

    /**
     * O fundo do mar na coluna, com pelo menos {@code profundidade} blocos de água por cima
     * (algas e corais contam como água). Null se for raso, seco ou congelado até o fundo.
     */
    static Block fundoDoMar(World w, int x, int z, int profundidade) {
        int y = w.getHighestBlockYAt(x, z, HeightMap.OCEAN_FLOOR);
        Block chao = w.getBlockAt(x, y, z);
        if (!chao.getType().isSolid()) return null;
        for (int d = 1; d <= profundidade; d++) if (!molhado(w.getBlockAt(x, y + d, z))) return null;
        return chao;
    }

    /** Água, ou planta/coral que vive dentro dela. */
    static boolean molhado(Block b) {
        Material m = b.getType();
        if (m == Material.WATER || m == Material.BUBBLE_COLUMN || m == Material.KELP || m == Material.KELP_PLANT
                || m == Material.SEAGRASS || m == Material.TALL_SEAGRASS) return true;
        return b.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged();
    }

    /**
     * Para palafitas: a superfície da água (ou o chão, se estiver seco). Água funda demais
     * (mais de 4 blocos até o fundo) não serve.
     */
    private static Block superficie(World w, int x, int z) {
        int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        for (int i = 0; i < 48 && y > w.getMinHeight(); i++, y--) {
            Block b = w.getBlockAt(x, y, z);
            if (b.getType() == Material.LAVA) return null;
            if (b.getType() == Material.WATER) {
                for (int d = 1; d <= 4; d++) if (ehChao(w.getBlockAt(x, y - d, z).getType())) return b;
                return null;
            }
            if (ehChao(b.getType())) return b;
        }
        return null;
    }

    /** "ao norte daqui, a uns 300 blocos": de onde vieram os bandidos (o acampamento mais perto). */
    private String pista(World w, int x, int z) {
        Sitio melhor = null;
        double best = 1500.0 * 1500.0;
        for (Sitio s : sitios) {
            if (s.tipo != Estrutura.ACAMPAMENTO || !s.mundo.equals(w.getName())) continue;
            double d2 = (double) (s.x - x) * (s.x - x) + (double) (s.z - z) * (s.z - z);
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        if (melhor == null) return null;
        int setor = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-(melhor.z - z), melhor.x - x)) / 45.0), 8);
        return DIRECOES[setor] + ", a uns " + Math.max(50, Math.round(Math.sqrt(best) / 50.0) * 50) + " blocos daqui";
    }

    private static boolean ehChao(Material m) {
        return m.isSolid() && !Tag.LOGS.isTagged(m) && !Tag.LEAVES.isTagged(m) && m != Material.BAMBOO && m != Material.CACTUS
                && m != Material.MUSHROOM_STEM && !m.name().endsWith("MUSHROOM_BLOCK") && m != Material.PUMPKIN && m != Material.MELON;
    }

    /** Bloco que só alguém poria ali (para não construir em cima de obra de jogador em chunk antigo). */
    private static boolean artificial(Material m) {
        return Tag.PLANKS.isTagged(m) || Tag.WOOL.isTagged(m) || Tag.BEDS.isTagged(m) || Tag.DOORS.isTagged(m)
                || Tag.ALL_SIGNS.isTagged(m) || Tag.FENCES.isTagged(m) || Tag.RAILS.isTagged(m) || Tag.STAIRS.isTagged(m)
                || Tag.SLABS.isTagged(m) || Tag.TRAPDOORS.isTagged(m) || Tag.CANDLES.isTagged(m)
                || switch (m) {
            case CHEST, TRAPPED_CHEST, BARREL, CRAFTING_TABLE, FURNACE, SMOKER, BLAST_FURNACE, TORCH, WALL_TORCH, LANTERN,
                 SOUL_LANTERN, GLASS, GLASS_PANE, BRICKS, STONE_BRICKS, COBBLESTONE, FARMLAND, DIRT_PATH, HAY_BLOCK, BOOKSHELF,
                 LADDER, ANVIL, ENCHANTING_TABLE, BEACON, LODESTONE, RESPAWN_ANCHOR -> true;
            default -> false;
        };
    }

    /** Conta por que um lugar foi recusado (aparece em /rpgadmin estruturas). */
    private Sitio recusa(String motivo) {
        recusas.merge(motivo, 1, Integer::sum);
        return null;
    }

    public Map<String, Integer> recusas() {
        return recusas;
    }

    Sitio perto(World w, int x, int z, double raio) {
        for (Sitio s : sitios) {
            if (!s.mundo.equals(w.getName())) continue;
            double dx = s.x - x, dz = s.z - z;
            // A cidade submersa é grande: a distância conta a partir da muralha dela.
            double lim = raio + (s.tipo == Estrutura.CIDADE_SUBMERSA ? Cidade.RAIO + 8 : 0);
            if (dx * dx + dz * dz < lim * lim) return s;
        }
        return null;
    }

    /**
     * Tenta construir. {@code forcar}: ignora bioma, desnível, distância e territórios (comando de
     * administrador). {@code chunkAntigo}: confere se não há obra de jogador na área.
     * @return o sítio criado, ou null se o lugar não serviu.
     */
    Sitio construir(World w, int x, int z, Estrutura t, int rot, Random r, boolean forcar, boolean chunkAntigo) {
        int raio = t.raio();
        if (!forcar) {
            if (perto(w, x, z, plugin.settings().estrDistancia) != null) return recusa("perto de outra estrutura");
            for (int cx = (x - raio) >> 4; cx <= (x + raio) >> 4; cx++) {
                for (int cz = (z - raio) >> 4; cz <= (z + raio) >> 4; cz++) {
                    if (plugin.territorios().em(w.getName(), cx, cz) != null) return recusa("território");
                }
            }
        }
        // Mede o chão.
        int lado = raio * 2 + 1;
        int[][] alt = new int[lado][lado];
        Map<Material, Integer> tipos = new EnumMap<>(Material.class);
        int[] todas = new int[lado * lado];
        int n = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dz = -raio; dz <= raio; dz++) {
                Block g = t.submersa() ? fundoDoMar(w, x + dx, z + dz, 7) : t.palafita() ? superficie(w, x + dx, z + dz) : chao(w, x + dx, z + dz);
                if (g == null && !forcar) return recusa(t.submersa() ? "mar raso" : "água ou lava");
                int h = g == null ? w.getHighestBlockYAt(x + dx, z + dz) : g.getY();
                if (g != null) tipos.merge(g.getType(), 1, Integer::sum);
                alt[dx + raio][dz + raio] = h;
                todas[n++] = h;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (!forcar && max - min > t.desnivel()) return recusa("chão irregular");
        Arrays.sort(todas);
        int y0 = todas[todas.length / 2];
        if (!forcar) {
            String bioma = w.getBiome(x, y0 + 1, z).getKey().getKey();
            if (!t.aceita(bioma)) return recusa("bioma");
        }
        int fundo = 0;
        if (t == Estrutura.MINA) {
            fundo = 12 + r.nextInt(5);
            if (y0 - fundo - 2 <= w.getMinHeight() + 3) {
                fundo = y0 - w.getMinHeight() - 6;
                if (!forcar || fundo < 6) return recusa("fundo demais");
            }
        }
        if (!forcar) {
            BoundingBox area = new BoundingBox(x - raio, y0 - fundo - 4, z - raio, x + raio + 1, y0 + t.altura(), z + raio + 1);
            for (GeneratedStructure g : w.getChunkAt(x >> 4, z >> 4).getStructures()) {
                if (g.getBoundingBox().overlaps(area)) return recusa("estrutura do Minecraft");
            }
        }
        if (chunkAntigo && !forcar) {
            for (int dx = -raio; dx <= raio; dx++) {
                for (int dz = -raio; dz <= raio; dz++) {
                    for (int dy = -3; dy <= t.altura(); dy++) if (artificial(w.getBlockAt(x + dx, y0 + dy, z + dz).getType())) return recusa("obra de jogador");
                }
            }
        }

        // Nivela: aterra o que está baixo, corta o que está alto e limpa o espaço de cima.
        Material sup = tipos.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Material.GRASS_BLOCK);
        Material sub = switch (sup) {
            case GRASS_BLOCK, PODZOL, MYCELIUM, DIRT, COARSE_DIRT, ROOTED_DIRT, MUD, MOSS_BLOCK, SNOW_BLOCK, DIRT_PATH -> Material.DIRT;
            case SAND -> Material.SANDSTONE;
            case RED_SAND -> Material.RED_SANDSTONE;
            case GRAVEL -> Material.STONE;
            default -> sup;
        };
        if (sup == Material.DIRT_PATH || sup == Material.FARMLAND) sup = Material.GRASS_BLOCK;
        boolean natural = !forcar && !chunkAntigo;
        for (int dx = -raio; dx <= raio; dx++) {
            for (int dz = -raio; dz <= raio; dz++) {
                int gx = x + dx, gz = z + dz, h = alt[dx + raio][dz + raio];
                if (!t.palafita()) {
                    for (int yy = h + 1; yy < y0; yy++) w.getBlockAt(gx, yy, gz).setType(sub, false);
                    w.getBlockAt(gx, y0, gz).setType(sup, false);
                }
                for (int yy = y0 + 1; yy <= y0 + t.altura(); yy++) {
                    Block b = w.getBlockAt(gx, yy, gz);
                    if (b.getType().isAir()) continue;
                    if (t.submersa()) {
                        // No mar, o que sai (alga, coral, pedra) vira água; acima da superfície, ar.
                        if (b.getType() == Material.WATER) continue;
                        b.setType(yy < w.getSeaLevel() ? Material.WATER : Material.AIR, false);
                    } else if (natural && Tag.LOGS.isTagged(b.getType())) derrubar(b);
                    else b.setType(Material.AIR, false);
                }
            }
        }

        Sitio s = new Sitio(UUID.randomUUID().toString().substring(0, 8), t, w.getName(), x, y0, z, rot);
        s.fundo = fundo;
        if (t == Estrutura.SANTUARIO_ESQUECIDO) s.deus = Deus.values()[r.nextInt(Deus.values().length)];
        if (t == Estrutura.VILA_SAQUEADA) s.pista = pista(w, x, z);
        if (t == Estrutura.NAUFRAGIO) {
            s.fundo = r.nextInt(Mar.Navio.values().length);
            s.nome = Mar.Navio.values()[s.fundo].nome(r);
            s.pista = cidades.pista(w, x, z);
        }
        Obra obra = new Obra(w, x, y0, z, rot, r);
        obra.submersa = t.submersa();
        projetos.construir(s, obra);
        sitios.add(s);
        salvar();
        plugin.getLogger().info("Estrutura nova: " + t.nome() + " em " + w.getName() + " " + x + " " + y0 + " " + z);
        return s;
    }

    /** Derruba a árvore inteira (com física, para as folhas caírem sozinhas depois). */
    private static void derrubar(Block inicio) {
        World w = inicio.getWorld();
        Deque<Block> fila = new ArrayDeque<>();
        Set<Long> vistos = new HashSet<>();
        fila.add(inicio);
        int n = 0;
        while (!fila.isEmpty() && n < 400) {
            Block a = fila.poll();
            if (!Tag.LOGS.isTagged(a.getType())) continue;
            a.setType(Material.AIR, true);
            n++;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int vx = a.getX() + dx, vy = a.getY() + dy, vz = a.getZ() + dz;
                        if (!w.isChunkLoaded(vx >> 4, vz >> 4)) continue;
                        long k = ((long) vx & 0x3FFFFFF) | (((long) vz & 0x3FFFFFF) << 26) | (((long) vy & 0xFFF) << 52);
                        if (vistos.add(k)) {
                            Block v = w.getBlockAt(vx, vy, vz);
                            if (Tag.LOGS.isTagged(v.getType())) fila.add(v);
                        }
                    }
                }
            }
        }
    }

    // =====================================================================
    //  Vida das estruturas (a cada 2 s)
    // =====================================================================

    public void tick() {
        ciclo++;
        long agora = System.currentTimeMillis();
        if (!sitios.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                boolean jogando = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
                World w = p.getWorld();
                Location l = p.getLocation();
                for (Sitio s : sitios) {
                    if (!s.mundo.equals(w.getName())) continue;
                    if (s.tipo == Estrutura.CIDADE_SUBMERSA && !CidadesSubmersas.pronta(s)) continue;
                    double d2 = s.distancia2(l);
                    // O farol aceso dá sorte (pesca e saque) a quem navega perto.
                    if (s.tipo == Estrutura.FAROL && s.acesoAte > agora && d2 <= 96 * 96) {
                        PotionEffect sorte = p.getPotionEffect(PotionEffectType.LUCK);
                        if (sorte == null || sorte.getDuration() < 200) p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 300, 0, true, false, true));
                    }
                    double alcance = Math.max(48, s.tipo.raio() + 20), achar = Math.max(22, s.tipo.raio() + 8);
                    if (d2 > alcance * alcance) continue;
                    double dy = l.getY() - s.y;
                    if (d2 <= achar * achar && Math.abs(dy) <= 30 && s.visitantes.add(p.getUniqueId())) descobrir(p, s);
                    if (s.tipo.temMorador() && d2 <= 40 * 40) garantirMorador(s, w);
                    boolean perto20 = jogando && !s.guardas && d2 <= 20 * 20 && Math.abs(dy) <= 24;
                    switch (s.tipo) {
                        case TORRE_DE_VIGIA, ACAMPAMENTO, EXPEDICAO, VILA_SAQUEADA, FORTIM -> { if (perto20) soltarGuardas(s, w); }
                        case MINA -> {
                            if (jogando && !s.guardas && d2 <= 10 * 10 && l.getY() < s.y - s.fundo + 6) soltarGuardas(s, w);
                        }
                        case TORRE_DO_MAGO -> {
                            if (jogando && !s.guardas && d2 <= 6 * 6 && l.getY() > s.y + 5) soltarGuardas(s, w);
                        }
                        case FORJA_DOS_ANOES -> {
                            if (jogando && !s.guardas && d2 <= 12 * 12 && l.getY() < s.y + 0.5) soltarGuardas(s, w);
                        }
                        case CEMITERIO -> {
                            if (jogando && d2 <= 15 * 15 && Math.abs(dy) <= 8 && noite(w) && agora - s.ronda > 8 * 60_000L) levantarMortos(s, w, p);
                        }
                        case CABANA_DA_BRUXA -> {
                            if (jogando && d2 <= 16 * 16 && noite(w) && agora - s.ronda > 10 * 60_000L) aprendizesDaBruxa(s, w, p);
                        }
                        case CIRCULO_DE_PEDRAS -> {
                            if (noite(w)) circuloANoite(s, w, p, jogando, d2);
                        }
                        case FAROL -> {
                            if (s.acesoAte > 0 && agora > s.acesoAte) apagarFarol(s, w);
                        }
                        case CIDADE_SUBMERSA -> cidades.tick(s, w, p, jogando, d2, dy);
                        default -> { }
                    }
                }
            }
            cidades.poderes(ciclo);
        }
        if (sujo && ciclo % 15 == 0) salvar();
    }

    private static boolean noite(World w) {
        long t = w.getTime();
        return t >= 13000 && t <= 23000;
    }

    private void descobrir(Player p, Sitio s) {
        sujo = true;
        String titulo = switch (s.tipo) {
            case NAUFRAGIO -> s.nome != null ? "Naufrágio do " + s.nome : s.tipo.nome();
            case CIDADE_SUBMERSA -> s.nome != null ? s.nome + ", a Cidade Submersa" : s.tipo.nome();
            default -> s.tipo.nome();
        };
        p.showTitle(Title.title(Component.text("⌂ " + titulo, s.tipo.cor(), TextDecoration.BOLD),
                Component.text(s.tipo.frase(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.6f, 0.7f);
        plugin.diario().marco(p, "estrutura_" + s.tipo.id(), "Encontrou " + s.tipo.comArtigoIndefinido());
        plugin.titulos().registrar(p, "estruturas", 1);
        if (s.tipo == Estrutura.SANTUARIO_ESQUECIDO && s.deus != null) {
            p.sendMessage(Component.text(s.deus.simbolo() + " Um santuário antigo de " + s.deus.nome() + ", " + s.deus.titulo()
                    + ". Leia a inscrição no púlpito.", s.deus.cor()));
        }
    }

    // ---------------------------------------------------------------------
    //  Guardas
    // ---------------------------------------------------------------------

    <T extends Mob> T guarda(Sitio s, Location l, Class<T> c, String nome, NamedTextColor cor, Consumer<T> extra) {
        T m = l.getWorld().spawn(l, c, x -> {
            x.customName(Component.text(nome, cor));
            x.setRemoveWhenFarAway(true);
            x.setCanPickupItems(false);
            x.getPersistentDataContainer().set(K_GUARDA, PersistentDataType.STRING, s.id);
            extra.accept(x);
        });
        plugin.perigo().envelhecer(m);
        return m;
    }

    private void soltarGuardas(Sitio s, World w) {
        s.guardas = true;
        sujo = true;
        switch (s.tipo) {
            case TORRE_DE_VIGIA -> {
                for (int[] p : Projetos.GUARDAS_TORRE) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), Skeleton.class, "Sentinela Esquecida", NamedTextColor.GRAY, m -> {
                        m.getEquipment().setHelmet(new ItemStack(ThreadLocalRandom.current().nextBoolean() ? Material.CHAINMAIL_HELMET : Material.IRON_HELMET));
                        m.getEquipment().setHelmetDropChance(0.05f);
                    });
                }
            }
            case ACAMPAMENTO -> {
                for (int[] p : Projetos.GUARDAS_ACAMPAMENTO) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), Pillager.class, "Bandido", NamedTextColor.RED, m -> {
                        m.setPatrolLeader(false);
                        m.setCanJoinRaid(false);
                    });
                }
                int[] c = Projetos.CHEFE_ACAMPAMENTO;
                guarda(s, s.ponto(w, c[0], c[1], c[2]), Vindicator.class, "Chefe dos Bandidos", NamedTextColor.DARK_RED, m -> {
                    m.setPatrolLeader(false);
                    m.setCanJoinRaid(false);
                    m.getPersistentDataContainer().set(K_GUARDA, PersistentDataType.STRING, s.id + ":chefe");
                    m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
                    m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0x5D1A1A)));
                    m.getEquipment().setHelmetDropChance(0f);
                    AttributeInstance vida = m.getAttribute(Attribute.MAX_HEALTH);
                    if (vida != null) {
                        vida.setBaseValue(44);
                        m.setHealth(44);
                    }
                });
            }
            case MINA -> {
                for (int i = 0; i < Projetos.GUARDAS_MINA.length; i++) {
                    int[] p = Projetos.GUARDAS_MINA[i];
                    Location l = s.ponto(w, p[0], -s.fundo + p[1], p[2]);
                    if (i < 3) {
                        guarda(s, l, Zombie.class, "Mineiro Perdido", NamedTextColor.GOLD, m -> {
                            m.setAdult();
                            m.setShouldBurnInDay(false);
                            m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
                            m.getEquipment().setItemInMainHandDropChance(0.08f);
                            m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0xC9A227)));
                            m.getEquipment().setHelmetDropChance(0f);
                        });
                    } else {
                        guarda(s, l, CaveSpider.class, "Aranha da Mina", NamedTextColor.DARK_GREEN, m -> { });
                    }
                }
            }
            case EXPEDICAO -> {
                for (int[] p : Ruinas.GUARDAS_EXPEDICAO) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), Stray.class, "Explorador Congelado", NamedTextColor.AQUA, m -> {
                        m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, Color.fromRGB(0x8D6E63)));
                        m.getEquipment().setHelmetDropChance(0f);
                    });
                }
            }
            case VILA_SAQUEADA -> {
                for (int[] p : Ruinas.GUARDAS_VILA) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), ZombieVillager.class, "Aldeão Perdido", NamedTextColor.DARK_GREEN, m -> {
                        m.setAdult();
                        m.setShouldBurnInDay(false);
                        // Podem ser curados (fraqueza + maçã dourada): não somem com a distância.
                        m.setRemoveWhenFarAway(false);
                        m.getPersistentDataContainer().remove(K_GUARDA);
                    });
                }
            }
            case TORRE_DO_MAGO -> {
                int[] p = Ruinas.APRENDIZ;
                guarda(s, s.ponto(w, p[0], p[1], p[2]), Illusioner.class, "Aprendiz Corrompido", NamedTextColor.LIGHT_PURPLE, m -> {
                    m.setPatrolLeader(false);
                    m.setCanJoinRaid(false);
                    m.getPersistentDataContainer().set(K_GUARDA, PersistentDataType.STRING, s.id + ":aprendiz");
                    vida(m, 60);
                });
                w.playSound(s.ponto(w, p[0], p[1], p[2]), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 0.8f);
            }
            case FORJA_DOS_ANOES -> {
                for (int[] p : Ruinas.ANOES) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), Zombie.class, "Anão Espectral", NamedTextColor.GOLD, m -> {
                        m.setBaby();
                        m.setShouldBurnInDay(false);
                        m.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                        m.getEquipment().setHelmetDropChance(0.05f);
                        m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
                        m.getEquipment().setItemInMainHandDropChance(0.05f);
                        vida(m, 26);
                    });
                }
            }
            case FORTIM -> {
                for (int[] p : Ruinas.SOLDADOS) {
                    guarda(s, s.ponto(w, p[0], p[1], p[2]), Skeleton.class, "Soldado Esquecido", NamedTextColor.GRAY, m -> {
                        m.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                        m.getEquipment().setHelmetDropChance(0.05f);
                    });
                }
                int[] c = Ruinas.CAPITAO;
                guarda(s, s.ponto(w, c[0], c[1], c[2]), WitherSkeleton.class, "Capitão Esquecido", NamedTextColor.DARK_RED, m -> {
                    m.getPersistentDataContainer().set(K_GUARDA, PersistentDataType.STRING, s.id + ":capitao");
                    m.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    m.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                    m.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    m.getEquipment().setHelmetDropChance(0f);
                    m.getEquipment().setChestplateDropChance(0f);
                    vida(m, 70);
                });
            }
            default -> { }
        }
    }

    static void vida(Mob m, double max) {
        AttributeInstance a = m.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) return;
        a.setBaseValue(max);
        m.setHealth(max);
    }

    // ---------------------------------------------------------------------
    //  Bruxa, círculo de pedras e farol
    // ---------------------------------------------------------------------

    /** À noite a bruxa não atende, e as aprendizes dela saem do caldeirão. */
    private void aprendizesDaBruxa(Sitio s, World w, Player p) {
        s.ronda = System.currentTimeMillis();
        sujo = true;
        Location caldeirao = s.ponto(w, -1, 5, -2);
        w.spawnParticle(Particle.WITCH, caldeirao, 40, 0.4, 0.4, 0.4, 0.1);
        w.playSound(caldeirao, Sound.ENTITY_WITCH_CELEBRATE, 1f, 0.8f);
        for (int[] q : new int[][]{{-1, 4, 2}, {1, 4, 2}}) {
            guarda(s, s.ponto(w, q[0], q[1], q[2]), Witch.class, "Aprendiz da Bruxa", NamedTextColor.DARK_PURPLE, m -> { });
        }
        p.sendActionBar(Component.text("☾ A porta da cabana range... alguém saiu.", Estrutura.CABANA_DA_BRUXA.cor()));
    }

    private static long diaDoMundo(World w) {
        return w.getFullTime() / 24000L;
    }

    /** Lua cheia (fase 0) e o guardião ainda não veio nesta lua. */
    private void circuloANoite(Sitio s, World w, Player p, boolean jogando, double d2) {
        long dia = diaDoMundo(w);
        boolean cheia = dia % 8 == 0;
        if (d2 <= 16 * 16 && ThreadLocalRandom.current().nextInt(3) == 0) {
            for (int[] pd : Ruinas.PEDRAS) {
                w.spawnParticle(cheia ? Particle.SOUL_FIRE_FLAME : Particle.ENCHANT, s.ponto(w, pd[0], 2, pd[1]).add(0, 0.5, 0), cheia ? 4 : 6, 0.2, 0.6, 0.2, 0.02);
            }
        }
        if (!cheia || !jogando || s.ronda == dia || d2 > 4.5 * 4.5) return;
        s.ronda = dia;
        sujo = true;
        Location centro = s.ponto(w, 0, 2, 0);
        w.strikeLightningEffect(centro);
        w.playSound(centro, Sound.ENTITY_WARDEN_EMERGE, 1f, 0.7f);
        for (int[] pd : Ruinas.PEDRAS) w.spawnParticle(Particle.SOUL, s.ponto(w, pd[0], 1, pd[1]), 25, 0.3, 1, 0.3, 0.03);
        guarda(s, centro, WitherSkeleton.class, "Guardião Ancestral", NamedTextColor.GOLD, m -> {
            m.getPersistentDataContainer().set(K_GUARDA, PersistentDataType.STRING, s.id + ":guardiao");
            m.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
            m.getEquipment().setHelmet(new ItemStack(Material.CHISELED_STONE_BRICKS));
            m.getEquipment().setHelmetDropChance(0f);
            vida(m, 90);
        });
        for (int i = 0; i < 2; i++) {
            int[] pd = Ruinas.PEDRAS[ThreadLocalRandom.current().nextInt(Ruinas.PEDRAS.length)];
            guarda(s, s.ponto(w, pd[0] / 2, 1, pd[1] / 2), Breeze.class, "Espírito do Vento", NamedTextColor.WHITE, m -> { });
        }
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) <= 40 * 40) {
                o.showTitle(Title.title(Component.text("◯ O Guardião Ancestral desperta", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text("A lua cheia acordou as pedras.", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
            }
        }
    }

    private void apagarFarol(Sitio s, World w) {
        int[] l = Ruinas.LANTERNA;
        Block b = s.bloco(w, l[0], l[1], l[2]);
        if (!w.isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) return;
        if (b.getType() == Material.SEA_LANTERN) b.setBlockData(Material.REDSTONE_LAMP.createBlockData(), false);
        s.acesoAte = 0;
        sujo = true;
    }

    private void acenderFarol(Player p, Sitio s, Block lanterna) {
        long agora = System.currentTimeMillis();
        if (s.acesoAte > agora) {
            long h = (s.acesoAte - agora) / 3_600_000L;
            p.sendActionBar(Component.text("☀ O farol está aceso (mais " + h + " h). Quem navega perto tem sorte.", Estrutura.FAROL.cor()));
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.getType() != Material.GLOWSTONE) {
            p.sendActionBar(Component.text("☀ A lanterna está apagada. Um bloco de pedra luminosa acenderia de novo.", Estrutura.FAROL.cor()));
            return;
        }
        mao.setAmount(mao.getAmount() - 1);
        lanterna.setBlockData(Material.SEA_LANTERN.createBlockData(), false);
        s.acesoAte = agora + 7L * 24 * 3_600_000L;
        sujo = true;
        World w = lanterna.getWorld();
        Location c = lanterna.getLocation().add(0.5, 0.5, 0.5);
        w.spawnParticle(Particle.END_ROD, c, 60, 0.6, 0.6, 0.6, 0.08);
        w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) <= 160 * 160) {
                o.sendMessage(Component.text("☀ " + p.getName() + " acendeu o Farol Abandonado! Quem navega por perto tem sorte por 7 dias.", Estrutura.FAROL.cor()));
            }
        }
        plugin.diario().marco(p, "farol", "Acendeu um farol abandonado");
    }

    private static ItemStack couro(Material m, Color cor) {
        ItemStack i = new ItemStack(m);
        i.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(cor));
        return i;
    }

    private void levantarMortos(Sitio s, World w, Player p) {
        s.ronda = System.currentTimeMillis();
        sujo = true;
        List<int[]> covas = new ArrayList<>(Arrays.asList(Projetos.COVAS));
        Collections.shuffle(covas);
        int n = 2 + ThreadLocalRandom.current().nextInt(3);
        for (int i = 0; i < n && i < covas.size(); i++) {
            int[] c = covas.get(i);
            Location l = s.ponto(w, c[0], 1, c[1] + 1 + ThreadLocalRandom.current().nextInt(2));
            w.spawnParticle(Particle.SOUL, l.clone().add(0, 0.3, 0), 20, 0.3, 0.2, 0.3, 0.02);
            w.spawnParticle(Particle.BLOCK, l, 30, 0.3, 0.1, 0.3, 0, Material.PODZOL.createBlockData());
            w.playSound(l, Sound.BLOCK_ROOTED_DIRT_BREAK, 1f, 0.6f);
            if (ThreadLocalRandom.current().nextBoolean()) {
                guarda(s, l, Zombie.class, "Morto Inquieto", NamedTextColor.DARK_GRAY, Zombie::setAdult);
            } else {
                guarda(s, l, Skeleton.class, "Morto Inquieto", NamedTextColor.DARK_GRAY, m -> { });
            }
        }
        w.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.7f);
        p.sendActionBar(Component.text("☠ A terra do cemitério se mexe...", Estrutura.CEMITERIO.cor()));
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        String v = e.getEntity().getPersistentDataContainer().get(K_GUARDA, PersistentDataType.STRING);
        if (v == null || !v.contains(":")) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Player quem = e.getEntity().getKiller();
        switch (v.substring(v.indexOf(':') + 1)) {
            case "chefe" -> {
                e.getDrops().add(new ItemStack(Material.EMERALD, 5 + r.nextInt(8)));
                if (r.nextDouble() < 0.35) e.getDrops().add(Raro.MAPA_RASGADO.criar(1));
                if (r.nextDouble() < 0.15) e.getDrops().add(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, e.getEntity().getLocation()));
                if (quem != null) plugin.diario().marco(quem, "chefe_bandidos", "Derrotou o Chefe dos Bandidos");
            }
            case "aprendiz" -> {
                e.getDrops().add(Reagente.CRISTAL_DE_MANA.criar(1 + r.nextInt(2)));
                if (r.nextDouble() < 0.25) e.getDrops().add(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio());
                if (quem != null) plugin.diario().marco(quem, "aprendiz_mago", "Silenciou o Aprendiz Corrompido da torre do mago");
            }
            case "capitao" -> {
                e.getDrops().add(Raro.FRAGMENTO_DE_FORJA.criar(1));
                if (r.nextDouble() < 0.3) e.getDrops().add(Raro.PEDRA_DE_PROTECAO.criar(1));
                if (quem != null) plugin.diario().marco(quem, "capitao_fortim", "Derrotou o Capitão Esquecido de um fortim");
            }
            case "sacerdote" -> cidades.sacerdoteMorreu(e, v.substring(0, v.indexOf(':')));
            case "guardiao" -> {
                e.getDrops().add(plugin.arqueologia().reliquiaAleatoria());
                if (r.nextDouble() < 0.4) e.getDrops().add(Raro.PAGINA_DE_LENDA.criar(1));
                if (r.nextDouble() < 0.15) e.getDrops().add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
                e.setDroppedExp(e.getDroppedExp() + 80);
                if (quem != null) plugin.diario().marco(quem, "guardiao_pedras", "Derrotou o Guardião Ancestral na lua cheia");
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------------
    //  Moradores: o eremita, o mercador nômade e a bruxa
    // ---------------------------------------------------------------------

    private static final String[] NOMES_NOMADE = {"Kadir", "Zahra", "Omar", "Leila", "Rashid", "Samira"};
    private static final String[] NOMES_BRUXA = {"Morgana", "Zulmira", "Benedita", "Iracema", "Hécate", "Cotinha"};

    private static String nomeMorador(Sitio s) {
        int i = Math.floorMod(s.id.hashCode(), 6);
        return switch (s.tipo) {
            case OASIS -> "Mercador " + NOMES_NOMADE[i];
            case CABANA_DA_BRUXA -> "Bruxa " + NOMES_BRUXA[i];
            default -> "Eremita " + NOMES_EREMITA[Math.floorMod(s.id.hashCode(), NOMES_EREMITA.length)];
        };
    }

    private static int[] lugarMorador(Estrutura t) {
        return switch (t) {
            case OASIS -> Ruinas.NOMADE;
            case CABANA_DA_BRUXA -> Ruinas.BRUXA;
            default -> Projetos.EREMITA;
        };
    }

    /** Põe o morador no lugar (uma vez só; se ele sumir, volta) e renova as ofertas a cada dia. */
    private void garantirMorador(Sitio s, World w) {
        Entity e = s.eremita == null ? null : Bukkit.getEntity(s.eremita);
        if (e != null && e.isValid()) {
            if (s.diaOfertas != hoje()) {
                if (e instanceof Villager v) v.setRecipes(ofertas(s, v.getLocation()));
                mercadores.remove(s.id);
                s.diaOfertas = hoje();
                sujo = true;
            }
            return;
        }
        int[] p = lugarMorador(s.tipo);
        Location onde = s.ponto(w, p[0], p[1], p[2]);
        if (!onde.getChunk().isEntitiesLoaded()) return;
        for (Entity x : w.getNearbyEntities(onde, 8, 6, 8)) {
            if (s.id.equals(x.getPersistentDataContainer().get(K_EREMITA, PersistentDataType.STRING))) {
                s.eremita = x.getUniqueId();
                sujo = true;
                return;
            }
        }
        BlockFace frente = new Obra(w, s.x, s.y, s.z, s.rot, null).face(BlockFace.SOUTH);
        onde.setDirection(frente.getDirection());
        Component nome = Component.text(nomeMorador(s), s.tipo.cor());
        Entity novo;
        if (s.tipo == Estrutura.CABANA_DA_BRUXA) {
            novo = w.spawn(onde, Witch.class, m -> {
                m.customName(nome);
                m.setCustomNameVisible(true);
                m.setAI(false);
                m.setInvulnerable(true);
                m.setPersistent(true);
                m.setRemoveWhenFarAway(false);
                m.setSilent(true);
                m.getPersistentDataContainer().set(K_EREMITA, PersistentDataType.STRING, s.id);
            });
        } else {
            Villager v = w.spawn(onde, Villager.class, m -> {
                m.customName(nome);
                m.setCustomNameVisible(true);
                m.setAI(false);
                m.setInvulnerable(true);
                m.setPersistent(true);
                m.setRemoveWhenFarAway(false);
                if (s.tipo == Estrutura.OASIS) {
                    m.setVillagerType(Villager.Type.DESERT);
                    m.setProfession(Villager.Profession.LEATHERWORKER);
                } else {
                    m.setProfession(Villager.Profession.CLERIC);
                }
                m.setVillagerLevel(5);
                m.getPersistentDataContainer().set(K_EREMITA, PersistentDataType.STRING, s.id);
            });
            v.setRecipes(ofertas(s, onde));
            novo = v;
            if (s.tipo == Estrutura.OASIS && w.getNearbyEntitiesByType(Camel.class, onde, 16).isEmpty()) {
                w.spawn(s.ponto(w, 1, 1, 5), Camel.class, c -> {
                    c.setAdult();
                    c.setPersistent(true);
                    c.setRemoveWhenFarAway(false);
                });
            }
        }
        s.eremita = novo.getUniqueId();
        s.diaOfertas = hoje();
        sujo = true;
    }

    private record Oferta(ItemStack item, int esmeraldas, int usos) { }

    /** As ofertas do dia (mudam de um dia para o outro, sempre as mesmas no mesmo dia). */
    private List<MerchantRecipe> ofertas(Sitio s, Location onde) {
        Random r = new Random(s.id.hashCode() * 31L + hoje());
        List<Oferta> l = new ArrayList<>();
        Erva[] ervas = Erva.values();
        Elixir elixir = Elixir.values()[r.nextInt(Elixir.values().length)];
        switch (s.tipo) {
            case OASIS -> {
                l.add(new Oferta(Gema.values()[r.nextInt(Gema.values().length)].criar(1, 1), 10, 2));
                l.add(new Oferta(Gema.values()[r.nextInt(Gema.values().length)].criar(2, 1), 18, 1));
                l.add(new Oferta(Fruta.LIMAO.muda(1), 7, 2));
                l.add(new Oferta(Bebida.values()[r.nextInt(Bebida.values().length)].criar(1), 12, 2));
                l.add(new Oferta(elixir.criar(Qualidade.BOA, 1.1, nomeMorador(s)), 12, 2));
                l.add(new Oferta(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde), 16, 1));
                l.add(new Oferta(Album.carta(Carta.values()[r.nextInt(Carta.values().length)], false), 5, 2));
            }
            case CABANA_DA_BRUXA -> {
                l.add(new Oferta(elixir.criar(Qualidade.OTIMA, 1.2, nomeMorador(s)), 16, 2));
                l.add(new Oferta(Elixir.values()[r.nextInt(Elixir.values().length)].criar(Qualidade.BOA, 1.1, nomeMorador(s)), 11, 2));
                l.add(new Oferta(Reagente.values()[r.nextInt(Reagente.values().length)].criar(2), 8, 3));
                l.add(new Oferta(Reagente.values()[r.nextInt(Reagente.values().length)].criar(2), 8, 3));
                l.add(new Oferta(Frascos.criar(Frascos.Tipo.values()[r.nextInt(Frascos.Tipo.values().length)], 3), 9, 2));
                l.add(new Oferta(ervas[r.nextInt(ervas.length)].criar(3), 4, 4));
                l.add(new Oferta(new ItemStack(Material.GLOWSTONE), 6, 2));
            }
            default -> {
                l.add(new Oferta(ervas[r.nextInt(ervas.length)].criar(3), 4, 4));
                l.add(new Oferta(ervas[r.nextInt(ervas.length)].criar(3), 4, 4));
                l.add(new Oferta(Fruta.sortear(r).muda(1), 7, 2));
                l.add(new Oferta(elixir.criar(Qualidade.BOA, 1.1, nomeMorador(s)), 12, 2));
                l.add(new Oferta(Raro.MAPA_RASGADO.criar(1), 14, 1));
                l.add(new Oferta(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde), 18, 1));
                l.add(new Oferta(Album.carta(Carta.values()[r.nextInt(Carta.values().length)], false), 5, 2));
            }
        }
        Collections.shuffle(l, r);
        List<MerchantRecipe> receitas = new ArrayList<>();
        for (Oferta o : l.subList(0, 5)) {
            MerchantRecipe m = new MerchantRecipe(o.item(), 0, o.usos(), false, 0, 0f);
            m.addIngredient(new ItemStack(Material.EMERALD, o.esmeraldas()));
            receitas.add(m);
        }
        return receitas;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFalar(PlayerInteractEntityEvent e) {
        String id = e.getRightClicked().getPersistentDataContainer().get(K_EREMITA, PersistentDataType.STRING);
        if (id == null) return;
        Player p = e.getPlayer();
        Sitio daqui = null;
        for (Sitio s : sitios) if (s.id.equals(id)) daqui = s;
        // A bruxa não é aldeã: as trocas dela abrem por aqui (e só de dia).
        if (e.getRightClicked() instanceof Witch && daqui != null) {
            e.setCancelled(true);
            if (noite(p.getWorld())) {
                p.sendMessage(Component.text(nomeMorador(daqui) + ": ", Estrutura.CABANA_DA_BRUXA.cor(), TextDecoration.BOLD)
                        .append(Component.text("\"Volte quando o sol nascer... se ainda estiver vivo.\"", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
                return;
            }
            Sitio s = daqui;
            Merchant m = mercadores.computeIfAbsent(s.id, k -> {
                Merchant novo = Bukkit.createMerchant(Component.text(nomeMorador(s), s.tipo.cor()));
                novo.setRecipes(ofertas(s, e.getRightClicked().getLocation()));
                return novo;
            });
            p.openMerchant(m, true);
        }
        long agora = System.currentTimeMillis();
        if (agora - ultimaFala.getOrDefault(p.getUniqueId(), 0L) < 5 * 60_000L) return;
        ultimaFala.put(p.getUniqueId(), agora);
        String nome = daqui == null ? "Eremita" : nomeMorador(daqui);
        TextColor cor = daqui == null ? Estrutura.CABANA_DO_EREMITA.cor() : daqui.tipo.cor();
        p.sendMessage(Component.text(nome + ": ", cor, TextDecoration.BOLD)
                .append(Component.text("\"" + boato(p, e.getRightClicked().getLocation(), daqui) + "\"", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false)));
        plugin.diario().marco(p, "eremita", "Conversou com um morador das terras distantes");
    }

    /** Uma estrutura que pode aparecer num sonho. */
    public record PistaSonho(String id, Estrutura tipo, String nome, int x, int z) { }

    /** A estrutura mais perto (até 3000 blocos) que o jogador nunca visitou nem sonhou. */
    public PistaSonho paraSonhar(Player p, Location daqui, java.util.Set<String> sonhados) {
        Sitio melhor = null;
        double best = 3000.0 * 3000.0;
        for (Sitio s : sitios) {
            if (!s.mundo.equals(daqui.getWorld().getName()) || s.visitantes.contains(p.getUniqueId()) || sonhados.contains(s.id)) continue;
            if (s.tipo == Estrutura.CIDADE_SUBMERSA && !CidadesSubmersas.pronta(s)) continue;
            double d2 = s.distancia2(daqui);
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        return melhor == null ? null : new PistaSonho(melhor.id, melhor.tipo, melhor.nome, melhor.x, melhor.z);
    }

    /** Conta onde fica a estrutura mais perto que você ainda não viu; ou só uma história. */
    private String boato(Player p, Location daqui, Sitio cabana) {
        Sitio melhor = null;
        double best = 2500.0 * 2500.0;
        for (Sitio s : sitios) {
            if (s == cabana || !s.mundo.equals(daqui.getWorld().getName()) || s.visitantes.contains(p.getUniqueId())) continue;
            double d2 = s.distancia2(daqui);
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        if (melhor == null || ThreadLocalRandom.current().nextDouble() < 0.25) return FALAS[ThreadLocalRandom.current().nextInt(FALAS.length)];
        double dx = melhor.x - daqui.getX(), dz = melhor.z - daqui.getZ();
        int dist = (int) Math.max(50, Math.round(Math.sqrt(best) / 50.0) * 50);
        int setor = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dz, dx)) / 45.0), 8);
        return "Dizem que há " + melhor.tipo.comArtigoIndefinido() + " a uns " + dist + " blocos daqui, " + DIRECOES[setor] + ".";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(K_EREMITA)) e.setCancelled(true);
    }

    // ---------------------------------------------------------------------
    //  Poço dos Desejos
    // ---------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoJogar(PlayerDropItemEvent e) {
        Item item = e.getItemDrop();
        ItemStack st = item.getItemStack();
        if (st.getType() != Material.EMERALD || !st.getPersistentDataContainer().getKeys().isEmpty()) return;
        Player p = e.getPlayer();
        Sitio poco = null;
        for (Sitio s : sitios) {
            if (s.tipo == Estrutura.POCO_DOS_DESEJOS && s.mundo.equals(p.getWorld().getName()) && s.distancia2(p.getLocation()) <= 49) poco = s;
        }
        if (poco == null) return;
        Sitio alvo = poco;
        new BukkitRunnable() {
            int n;

            @Override
            public void run() {
                if (!item.isValid() || ++n > 30) { cancel(); return; }
                Location l = item.getLocation();
                if (Math.abs(l.getX() - alvo.x - 0.5) <= 1.6 && Math.abs(l.getZ() - alvo.z - 0.5) <= 1.6
                        && l.getY() >= alvo.y - 3 && l.getY() <= alvo.y + 1.1) {
                    cancel();
                    desejar(p, alvo, item);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private void desejar(Player p, Sitio s, Item item) {
        if (!p.isOnline()) return;
        Location agua = s.ponto(p.getWorld(), 0, 1, 0);
        if (p.getPersistentDataContainer().getOrDefault(kPoco, PersistentDataType.LONG, -1L) == hoje()) {
            p.sendActionBar(Component.text("≈ A água está calma. O poço só ouve um desejo por dia.", Estrutura.POCO_DOS_DESEJOS.cor()));
            return;
        }
        ItemStack st = item.getItemStack();
        if (st.getAmount() > 1) {
            st.setAmount(st.getAmount() - 1);
            item.setItemStack(st);
        } else {
            item.remove();
        }
        p.getPersistentDataContainer().set(kPoco, PersistentDataType.LONG, hoje());
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Desejo d = DESEJOS.get(r.nextInt(DESEJOS.size()));
        p.addPotionEffect(new PotionEffect(d.tipo(), d.segundos() * 20, d.nivel()));
        World w = agua.getWorld();
        w.spawnParticle(Particle.SPLASH, agua, 40, 0.6, 0.1, 0.6, 0.1);
        w.spawnParticle(Particle.END_ROD, agua, 25, 0.5, 0.6, 0.5, 0.03);
        w.playSound(agua, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f);
        w.playSound(agua, Sound.ENTITY_GENERIC_SPLASH, 0.6f, 1.2f);
        p.sendMessage(Component.text("≈ O poço aceita a esmeralda. Desejo de " + d.nome() + ": " + d.descricao() + ".", Estrutura.POCO_DOS_DESEJOS.cor()));
        if (r.nextDouble() < 0.12) {
            ItemStack presente = switch (r.nextInt(4)) {
                case 0 -> Fruta.sortear(r).muda(1);
                case 1 -> Erva.values()[r.nextInt(Erva.values().length)].criar(2);
                case 2 -> Album.carta(Carta.values()[r.nextInt(Carta.values().length)], false);
                default -> plugin.arqueologia().reliquiaAleatoria();
            };
            Item dado = w.dropItem(agua.clone().add(0, 0.5, 0), presente);
            dado.setVelocity(p.getLocation().toVector().subtract(agua.toVector()).normalize().multiply(0.35).setY(0.35));
            p.sendMessage(Component.text("≈ ...e a água devolve um presente!", Estrutura.POCO_DOS_DESEJOS.cor()));
        }
        plugin.titulos().registrar(p, "desejos", 1);
    }

    // ---------------------------------------------------------------------
    //  Santuário Esquecido
    // ---------------------------------------------------------------------

    private Sitio altar(Block b) {
        for (Sitio s : sitios) {
            if (s.tipo != Estrutura.SANTUARIO_ESQUECIDO || !s.mundo.equals(b.getWorld().getName())) continue;
            if (Math.abs(s.x - b.getX()) > 1 || Math.abs(s.z - b.getZ()) > 1) continue;
            if (b.getX() == s.x && b.getZ() == s.z && (b.getY() == s.y + 1 || b.getY() == s.y + 2)) return s;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        Sitio farol = lanternaDoFarol(b);
        if (farol != null) {
            e.setCancelled(true);
            acenderFarol(e.getPlayer(), farol, b);
            return;
        }
        Sitio s = altar(b);
        if (s == null || s.deus == null) return;
        e.setCancelled(true);
        rezar(e.getPlayer(), s);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(org.bukkit.event.block.BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() == Material.CONDUIT) cidades.aoColocarCondutor(e.getPlayer(), e.getBlockPlaced());
    }

    private Sitio lanternaDoFarol(Block b) {
        if (b.getType() != Material.REDSTONE_LAMP && b.getType() != Material.SEA_LANTERN) return null;
        for (Sitio s : sitios) {
            if (s.tipo != Estrutura.FAROL || !s.mundo.equals(b.getWorld().getName()) || Math.abs(s.x - b.getX()) > 2 || Math.abs(s.z - b.getZ()) > 2) continue;
            int[] l = Ruinas.LANTERNA;
            if (s.bloco(b.getWorld(), l[0], l[1], l[2]).equals(b)) return s;
        }
        return null;
    }

    private void rezar(Player p, Sitio s) {
        Deus d = s.deus, meu = plugin.deuses().deus(p);
        if (meu == null) {
            p.sendMessage(Component.text(d.simbolo() + " Um altar antigo de " + d.nome() + ", " + d.titulo()
                    + ". Escolha um deus num Santuário dos Deuses para rezar em altares como este.", d.cor()));
            return;
        }
        if (meu != d) {
            p.sendMessage(Component.text(d.simbolo() + " Este altar é de " + d.nome() + ". " + meu.nome() + " não é ouvido aqui.", d.cor()));
            return;
        }
        if (p.getPersistentDataContainer().getOrDefault(kAltar, PersistentDataType.LONG, -1L) == hoje()) {
            p.sendActionBar(Component.text(d.simbolo() + " Você já rezou num santuário esquecido hoje. Volte amanhã.", d.cor()));
            return;
        }
        p.getPersistentDataContainer().set(kAltar, PersistentDataType.LONG, hoje());
        plugin.deuses().darDevocao(p, 30);
        String bencao = switch (d) {
            case FERRUM -> { p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 300, 0)); yield "Resistência por 5 min"; }
            case SYLVA -> { p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60, 1)); yield "Regeneração II por 1 min"; }
            case MARIS -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 300, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 300, 0));
                yield "Graça do golfinho e fôlego por 5 min";
            }
            case BELLUM -> { p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 180, 0)); yield "Força por 3 min"; }
            case ARCANUS -> { p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 600, 0)); yield "Sorte por 10 min"; }
            case MORTIS -> { p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 180, 1)); yield "Absorção II por 3 min"; }
        };
        Location c = s.ponto(p.getWorld(), 0, 2, 0).add(0, 0.5, 0);
        p.getWorld().spawnParticle(Particle.END_ROD, c, 50, 0.4, 0.8, 0.4, 0.04);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 60, 0.5, 0.8, 0.5, 0.5);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        p.sendMessage(Component.text(d.simbolo() + " " + d.nome() + " ouviu você neste altar antigo. (+30 devoção, " + bencao + ")", d.cor()));
    }

    // =====================================================================
    //  Administrador
    // =====================================================================

    /** Constrói à frente do jogador, de frente para ele. @return mensagem de erro ou null. */
    public String construirAdmin(Player p, Estrutura t) {
        BlockFace f = p.getFacing();
        int rot = switch (f) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        Location l = p.getLocation().add(f.getDirection().multiply(t.raio() + 3));
        if (t == Estrutura.CIDADE_SUBMERSA) return cidades.fundarAqui(p.getWorld(), l.getBlockX(), l.getBlockZ());
        Sitio s = construir(p.getWorld(), l.getBlockX(), l.getBlockZ(), t, rot, new Random(), true, true);
        return s == null ? "Não deu para construir aqui (fundo demais para a mina?)." : null;
    }

    /** Pelo console: {@code /rpgadmin estrutura <tipo> <mundo> <x> <z>}. */
    public String construirEm(World w, int x, int z, Estrutura t) {
        if (t == Estrutura.CIDADE_SUBMERSA) return cidades.fundarAqui(w, x, z);
        Sitio s =construir(w, x, z, t, ThreadLocalRandom.current().nextInt(4), new Random(), true, true);
        return s == null ? "Não deu para construir aqui (fundo demais para a mina?)." : null;
    }

    public List<String> listar(Location l, int max) {
        List<Sitio> ordem = new ArrayList<>();
        for (Sitio s : sitios) if (s.mundo.equals(l.getWorld().getName())) ordem.add(s);
        ordem.sort((a, b) -> Double.compare(a.distancia2(l), b.distancia2(l)));
        List<String> linhas = new ArrayList<>();
        for (Sitio s : ordem.subList(0, Math.min(max, ordem.size()))) {
            linhas.add(s.tipo.nome() + (s.deus != null ? " (" + s.deus.nome() + ")" : "") + (s.nome != null ? " (" + s.nome + ")" : "") + " — " + s.x + " " + s.y + " " + s.z
                    + " (" + (int) Math.sqrt(s.distancia2(l)) + " blocos, " + s.visitantes.size() + " visitante(s))");
        }
        return linhas;
    }

    /** Tira do registro a estrutura mais perto (os blocos ficam). */
    public String esquecer(Location l) {
        Sitio s = perto(l.getWorld(), l.getBlockX(), l.getBlockZ(), 24);
        if (s == null) return null;
        sitios.remove(s);
        salvar();
        return s.tipo.nome();
    }
}
