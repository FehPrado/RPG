package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fe.Deus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As cidades submersas: raras (no máximo uma por região), grandes, no mar profundo. Nascem
 * quando um chunk novo de mar profundo é gerado: os 5x5 chunks em volta são carregados sem
 * travar, o fundo é medido e, se servir, a obra sai em {@link Cidade#ETAPAS} ticks.
 *
 * Quem chega perto acorda os guardas afogados; quem entra no santuário acorda o
 * Sumo-Sacerdote, que guarda o coração da cidade (um condutor). Devolvido à moldura do
 * altar, a cidade "desperta": os guardas somem para sempre e Maris agradece.
 */
final class CidadesSubmersas {

    private static final String[] NOMES = {"Thalassa", "Nerea", "Coralina", "Abissália", "Maremor", "Ondina", "Talássia", "Pelágia"};
    private static final NamedTextColor COR = NamedTextColor.DARK_AQUA;

    private final RPGAtributos plugin;
    private final Estruturas estruturas;
    private final Cidade planta;
    /** Região → quantas vezes já tentamos pôr uma cidade nela (desde que o servidor ligou). */
    private final Map<Long, Integer> tentativas = new HashMap<>();
    private final Set<String> obrando = new HashSet<>();
    private final Map<String, UUID> sacerdotes = new HashMap<>();
    private boolean avaliando;

    CidadesSubmersas(RPGAtributos plugin, Estruturas estruturas, Projetos projetos) {
        this.plugin = plugin;
        this.estruturas = estruturas;
        this.planta = new Cidade(plugin, projetos);
    }

    private Settings cfg() {
        return plugin.settings();
    }

    private long regiao(int x, int z) {
        int r = cfg().cidadeRegiao;
        return ((long) Math.floorDiv(x, r) << 32) | (Math.floorDiv(z, r) & 0xffffffffL);
    }

    private boolean temCidade(World w, long regiao) {
        for (Sitio s : estruturas.sitios) {
            if (s.tipo == Estrutura.CIDADE_SUBMERSA && s.mundo.equals(w.getName()) && regiao(s.x, s.z) == regiao) return true;
        }
        return false;
    }

    static boolean pronta(Sitio s) {
        return s.fase >= Cidade.ETAPAS;
    }

    // =====================================================================
    //  Onde nascem
    // =====================================================================

    /** Um chunk novo carregou: se for mar profundo, talvez uma cidade nasça aqui. */
    void talvez(Chunk c) {
        if (!cfg().cidadeAtivada || avaliando) return;
        World w = c.getWorld();
        int x = c.getX() * 16 + 8, z = c.getZ() * 16 + 8;
        Location spawn = w.getSpawnLocation();
        double dx = x - spawn.getX(), dz = z - spawn.getZ();
        if (dx * dx + dz * dz < (double) cfg().cidadeDistanciaSpawn * cfg().cidadeDistanciaSpawn) return;
        String bioma = w.getBiome(x, 40, z).getKey().getKey();
        if (!bioma.contains("deep") || !bioma.contains("ocean")) return;
        long reg = regiao(x, z);
        if (tentativas.getOrDefault(reg, 0) >= 6 || temCidade(w, reg)) return;
        Random r = new Random(w.getSeed() ^ (c.getX() * 0x9E3779B97F4A7C15L) ^ (c.getZ() * 0xC2B2AE3D27D4EB4FL) ^ 0xC1DAL);
        if (r.nextDouble() >= cfg().cidadeChance) return;
        tentativas.merge(reg, 1, Integer::sum);
        avaliando = true;
        long semente = r.nextLong();
        int rot = r.nextInt(4);
        carregar(w, x, z).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            avaliando = false;
            if (ex == null) avaliar(w, x, z, rot, semente);
        }));
    }

    /** Carrega (e gera, se preciso) os chunks da área sem travar o servidor. */
    private CompletableFuture<Void> carregar(World w, int x, int z) {
        List<CompletableFuture<Chunk>> l = new ArrayList<>();
        for (int cx = (x - Cidade.RAIO) >> 4; cx <= (x + Cidade.RAIO) >> 4; cx++) {
            for (int cz = (z - Cidade.RAIO) >> 4; cz <= (z + Cidade.RAIO) >> 4; cz++) l.add(w.getChunkAtAsync(cx, cz, true));
        }
        return CompletableFuture.allOf(l.toArray(new CompletableFuture[0]));
    }

    /** Mede o fundo: fundo o bastante, quase plano, longe de territórios e de monumentos. */
    private void avaliar(World w, int x, int z, int rot, long semente) {
        int[] alts = new int[81];
        int n = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -32; dx <= 32; dx += 8) {
            for (int dz = -32; dz <= 32; dz += 8) {
                Block f = Estruturas.fundoDoMar(w, x + dx, z + dz, 16);
                if (f == null) return;
                alts[n++] = f.getY();
                min = Math.min(min, f.getY());
                max = Math.max(max, f.getY());
            }
        }
        if (max - min > 12) return;
        Arrays.sort(alts);
        int y0 = alts[alts.length / 2];
        if (y0 + 16 >= w.getSeaLevel() - 1) return;
        for (int cx = (x - Cidade.RAIO) >> 4; cx <= (x + Cidade.RAIO) >> 4; cx++) {
            for (int cz = (z - Cidade.RAIO) >> 4; cz <= (z + Cidade.RAIO) >> 4; cz++) {
                if (plugin.territorios().em(w.getName(), cx, cz) != null) return;
            }
        }
        if (estruturas.perto(w, x, z, 120) != null) return;
        BoundingBox area = new BoundingBox(x - Cidade.RAIO, y0 - 8, z - Cidade.RAIO, x + Cidade.RAIO + 1, y0 + Cidade.ALTURA, z + Cidade.RAIO + 1);
        for (int cx = (x - Cidade.RAIO) >> 4; cx <= (x + Cidade.RAIO) >> 4; cx++) {
            for (int cz = (z - Cidade.RAIO) >> 4; cz <= (z + Cidade.RAIO) >> 4; cz++) {
                for (GeneratedStructure g : w.getChunkAt(cx, cz).getStructures()) {
                    if (g.getStructure().equals(Structure.MONUMENT) && g.getBoundingBox().overlaps(area)) return;
                }
            }
        }
        fundar(w, x, y0, z, rot, semente);
    }

    private Sitio fundar(World w, int x, int y0, int z, int rot, long semente) {
        Sitio s = new Sitio(UUID.randomUUID().toString().substring(0, 8), Estrutura.CIDADE_SUBMERSA, w.getName(), x, y0, z, rot);
        s.nome = NOMES[new Random(semente).nextInt(NOMES.length)];
        s.semente = semente;
        estruturas.adicionar(s);
        plugin.getLogger().info("Cidade submersa nova: " + s.nome + " em " + w.getName() + " " + x + " " + y0 + " " + z);
        obra(s);
        return s;
    }

    /** Pelo administrador: no fundo do mar debaixo de (x, z), sem conferir nada. */
    String fundarAqui(World w, int x, int z) {
        Block f = Estruturas.fundoDoMar(w, x, z, 1);
        int y0 = f != null ? f.getY() : w.getHighestBlockYAt(x, z);
        if (y0 + 16 >= w.getSeaLevel() - 1) return "Raso demais: a cidade precisa de uns 18 blocos de água.";
        fundar(w, x, y0, z, ThreadLocalRandom.current().nextInt(4), ThreadLocalRandom.current().nextLong());
        return null;
    }

    /** A obra, uma etapa por tick, com os chunks presos carregados até acabar. */
    private void obra(Sitio s) {
        World w = s.world();
        if (w == null || pronta(s) || !obrando.add(s.id)) return;
        carregar(w, s.x, s.z).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                obrando.remove(s.id);
                return;
            }
            List<Chunk> presos = new ArrayList<>();
            for (int cx = (s.x - Cidade.RAIO) >> 4; cx <= (s.x + Cidade.RAIO) >> 4; cx++) {
                for (int cz = (s.z - Cidade.RAIO) >> 4; cz <= (s.z + Cidade.RAIO) >> 4; cz++) {
                    Chunk c = w.getChunkAt(cx, cz);
                    c.addPluginChunkTicket(plugin);
                    presos.add(c);
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (pronta(s)) {
                        cancel();
                        for (Chunk c : presos) c.removePluginChunkTicket(plugin);
                        obrando.remove(s.id);
                        estruturas.salvar();
                        plugin.getLogger().info("Cidade submersa " + s.nome + " pronta.");
                        return;
                    }
                    Obra o = new Obra(w, s.x, s.y, s.z, s.rot, new Random(s.semente + s.fase * 7919L));
                    o.submersa = true;
                    try {
                        planta.etapa(s.fase, o, s.nome);
                    } catch (RuntimeException e) {
                        plugin.getLogger().warning("Cidade submersa " + s.nome + ", etapa " + s.fase + ": " + e);
                    }
                    s.fase++;
                    estruturas.sujo = true;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }));
    }

    /** Ao ligar: termina as obras que o servidor desligou no meio. */
    void retomar() {
        for (Sitio s : new ArrayList<>(estruturas.sitios)) {
            if (s.tipo == Estrutura.CIDADE_SUBMERSA && !pronta(s)) obra(s);
        }
    }

    /** "ao sudoeste, a uns 1200 blocos daqui": a cidade mais perto (para os diários dos naufrágios). */
    String pista(World w, int x, int z) {
        Sitio melhor = null;
        double best = 3000.0 * 3000.0;
        for (Sitio s : estruturas.sitios) {
            if (s.tipo != Estrutura.CIDADE_SUBMERSA || !s.mundo.equals(w.getName())) continue;
            double d2 = (double) (s.x - x) * (s.x - x) + (double) (s.z - z) * (s.z - z);
            if (d2 < best) {
                best = d2;
                melhor = s;
            }
        }
        if (melhor == null) return null;
        int setor = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-(melhor.z - z), melhor.x - x)) / 45.0), 8);
        return Estruturas.DIRECOES[setor] + ", a uns " + Math.max(50, Math.round(Math.sqrt(best) / 50.0) * 50) + " blocos daqui";
    }

    // =====================================================================
    //  Vida da cidade
    // =====================================================================

    private long volta() {
        return cfg().cidadeDiasParaVoltar * 86_400_000L;
    }

    /** A cada 2 s, para cada jogador perto da cidade. */
    void tick(Sitio s, World w, Player p, boolean jogando, double d2, double dy) {
        if (!pronta(s)) return;
        if (s.acesoAte > 0) {
            // Desperta: em paz. Quem nada nela anda leve como um golfinho.
            if (d2 <= 36 * 36 && p.isInWater()) {
                PotionEffect g = p.getPotionEffect(PotionEffectType.DOLPHINS_GRACE);
                if (g == null || g.getDuration() < 60) p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 120, 0, true, false, true));
            }
            return;
        }
        long agora = System.currentTimeMillis();
        if (s.guardas && agora - s.ronda > volta()) {
            s.guardas = false;
            estruturas.sujo = true;
        }
        if (jogando && !s.guardas && d2 <= 40 * 40 && Math.abs(dy) <= 30) soltarGuardas(s, w);
        boolean noSantuario = d2 <= 6 * 6 && dy >= 2 && dy <= 12;
        if (jogando && noSantuario && (s.chefeEm == 0 || agora - s.chefeEm > volta()) && sacerdote(s) == null) invocar(s, w);
    }

    private static ItemStack couro(Material m, int cor) {
        ItemStack i = new ItemStack(m);
        i.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(Color.fromRGB(cor)));
        return i;
    }

    private void soltarGuardas(Sitio s, World w) {
        s.guardas = true;
        s.ronda = System.currentTimeMillis();
        estruturas.sujo = true;
        for (int[] g : Cidade.GUARDAS) {
            estruturas.guarda(s, s.ponto(w, g[0], g[1], g[2]), Drowned.class, "Guarda Afogado de " + s.nome, COR, m -> {
                m.setAdult();
                m.setShouldBurnInDay(false);
                m.getEquipment().setHelmet(couro(Material.LEATHER_HELMET, 0x1F6F6A));
                m.getEquipment().setHelmetDropChance(0f);
                if (ThreadLocalRandom.current().nextBoolean()) {
                    m.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
                    m.getEquipment().setItemInMainHandDropChance(0.04f);
                }
                Estruturas.vida(m, 30);
            });
        }
        for (int[] g : Cidade.SENTINELAS) {
            estruturas.guarda(s, s.ponto(w, g[0], g[1], g[2]), Guardian.class, "Sentinela das Marés", NamedTextColor.AQUA, m -> { });
        }
    }

    /** O Sumo-Sacerdote desta cidade, se estiver vivo por perto. */
    private Mob sacerdote(Sitio s) {
        UUID id = sacerdotes.get(s.id);
        Entity e = id == null ? null : Bukkit.getEntity(id);
        if (e instanceof Mob m && m.isValid() && !m.isDead()) return m;
        World w = s.world();
        if (w == null) return null;
        String marca = s.id + ":sacerdote";
        for (Entity o : w.getNearbyEntities(s.ponto(w, 0, 6, 0), 24, 16, 24)) {
            if (o instanceof Mob m && marca.equals(m.getPersistentDataContainer().get(Estruturas.K_GUARDA, PersistentDataType.STRING))) {
                sacerdotes.put(s.id, m.getUniqueId());
                return m;
            }
        }
        sacerdotes.remove(s.id);
        return null;
    }

    private void invocar(Sitio s, World w) {
        int[] p = Cidade.SACERDOTE;
        Location l = s.ponto(w, p[0], p[1], p[2]);
        String nome = Cidade.sacerdote(s.nome);
        Drowned d = estruturas.guarda(s, l, Drowned.class, nome, NamedTextColor.GOLD, m -> {
            m.getPersistentDataContainer().set(Estruturas.K_GUARDA, PersistentDataType.STRING, s.id + ":sacerdote");
            m.setRemoveWhenFarAway(false);
            m.setAdult();
            m.setShouldBurnInDay(false);
            m.setCustomNameVisible(true);
            ItemStack tridente = new ItemStack(Material.TRIDENT);
            tridente.addUnsafeEnchantment(Enchantment.LOYALTY, 3);
            m.getEquipment().setItemInMainHand(tridente);
            m.getEquipment().setItemInMainHandDropChance(0f);
            m.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            m.getEquipment().setHelmetDropChance(0f);
            m.getEquipment().setChestplate(couro(Material.LEATHER_CHESTPLATE, 0x0E4D4A));
            m.getEquipment().setChestplateDropChance(0f);
            Estruturas.vida(m, 220);
            AttributeInstance escala = m.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(1.35);
        });
        sacerdotes.put(s.id, d.getUniqueId());
        w.playSound(l, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.7f);
        w.spawnParticle(Particle.NAUTILUS, l.clone().add(0, 1.5, 0), 120, 1.5, 1.5, 1.5, 0.6);
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) > 48 * 48) continue;
            o.showTitle(Title.title(Component.text("≈ " + nome, NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Ninguém leva o coração de " + s.nome + ".", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
    }

    /** Uma vez a cada 2 s: os poderes dos sacerdotes vivos. */
    void poderes(int ciclo) {
        for (Sitio s : estruturas.sitios) {
            if (s.tipo != Estrutura.CIDADE_SUBMERSA || !sacerdotes.containsKey(s.id)) continue;
            Mob m = sacerdote(s);
            if (m == null) continue;
            List<Player> perto = new ArrayList<>();
            for (Player p : m.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(m.getLocation()) <= 16 * 16 && !p.isDead()
                        && (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE)) perto.add(p);
            }
            if (perto.isEmpty()) continue;
            World w = m.getWorld();
            Location c = m.getLocation().add(0, 1, 0);
            if (ciclo % 4 == 0) {
                // Chamado das Marés: dois afogados saem do chão (até 4 de cada vez).
                int vivos = 0;
                for (Entity e : m.getNearbyEntities(20, 12, 20)) {
                    if (e instanceof Drowned x && s.id.equals(x.getPersistentDataContainer().get(Estruturas.K_GUARDA, PersistentDataType.STRING))) vivos++;
                }
                for (int i = 0; i < 2 && vivos < 4; i++, vivos++) {
                    Location l = c.clone().add(ThreadLocalRandom.current().nextDouble(-3, 3), 0, ThreadLocalRandom.current().nextDouble(-3, 3));
                    estruturas.guarda(s, l, Drowned.class, "Afogado de " + s.nome, COR, x -> {
                        x.setAdult();
                        x.setShouldBurnInDay(false);
                    });
                    w.spawnParticle(Particle.BUBBLE_COLUMN_UP, l, 30, 0.3, 0.6, 0.3, 0.1);
                }
                w.playSound(c, Sound.ENTITY_DROWNED_AMBIENT_WATER, 1.2f, 0.6f);
            } else if (ciclo % 6 == 3) {
                // Redemoinho: puxa quem está perto e deixa lento.
                w.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 0.7f);
                w.spawnParticle(Particle.NAUTILUS, c, 80, 3, 1.5, 3, 0.4);
                for (Player p : perto) {
                    Vector v = c.toVector().subtract(p.getLocation().toVector());
                    if (v.lengthSquared() < 1) continue;
                    p.setVelocity(p.getVelocity().add(v.normalize().multiply(0.9)));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
                    p.sendActionBar(Component.text("≈ O redemoinho do sacerdote te puxa!", COR));
                }
            }
        }
    }

    /** O Sumo-Sacerdote caiu: o coração da cidade e o tesouro dele. */
    void sacerdoteMorreu(EntityDeathEvent e, String id) {
        Sitio s = null;
        for (Sitio x : estruturas.sitios) if (x.id.equals(id)) s = x;
        if (s == null) return;
        s.chefeEm = System.currentTimeMillis();
        sacerdotes.remove(s.id);
        estruturas.sujo = true;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        ItemStack coracao = new ItemStack(Material.CONDUIT);
        String nome = s.nome;
        coracao.editMeta(m -> {
            m.itemName(Component.text("Coração de " + nome, NamedTextColor.AQUA));
            m.lore(List.of(
                    Component.text("Batia no altar do templo de " + nome + ".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Ponha bem no meio da moldura do altar", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                    Component.text("e a cidade enfim descansa.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
        });
        e.getDrops().add(coracao);
        e.getDrops().add(Reagente.PEROLA_NEGRA.criar(2 + r.nextInt(2)));
        e.getDrops().add(Reagente.ESCAMA_DO_ABISMO.criar(2 + r.nextInt(3)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 8 + r.nextInt(9)));
        e.getDrops().add(plugin.arqueologia().reliquiaAleatoria());
        if (r.nextDouble() < 0.4) e.getDrops().add(Raro.PAGINA_DE_LENDA.criar(1));
        if (r.nextDouble() < 0.35) {
            ItemStack t = new ItemStack(Material.TRIDENT);
            String dono = Cidade.sacerdote(nome);
            t.editMeta(m -> m.itemName(Component.text("Tridente do " + dono, NamedTextColor.GOLD)));
            t.addEnchantment(Enchantment.LOYALTY, 3);
            t.addEnchantment(Enchantment.IMPALING, 3);
            e.getDrops().add(t);
        }
        e.setDroppedExp(e.getDroppedExp() + 150);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "sacerdote_" + s.id, "Derrotou o " + Cidade.sacerdote(nome) + " em " + nome);
            plugin.titulos().registrar(quem, "sacerdotes", 1);
            quem.sendMessage(Component.text("≈ O coração de " + nome + " bate na sua mão. O altar do templo está esperando.", NamedTextColor.AQUA));
        }
    }

    /** Alguém pôs um condutor: se for no meio da moldura do altar, a cidade desperta. */
    void aoColocarCondutor(Player p, Block b) {
        for (Sitio s : estruturas.sitios) {
            if (s.tipo != Estrutura.CIDADE_SUBMERSA || !pronta(s) || s.acesoAte > 0 || !s.mundo.equals(b.getWorld().getName())) continue;
            if (s.distancia2(b.getLocation()) > 12 * 12) continue;
            int[] c = Cidade.CORACAO;
            if (!s.bloco(b.getWorld(), c[0], c[1], c[2]).equals(b)) {
                p.sendActionBar(Component.text("≈ Bem no meio da moldura do altar, onde a água está livre.", COR));
                continue;
            }
            despertar(s, p, b);
            return;
        }
    }

    private void despertar(Sitio s, Player p, Block b) {
        s.acesoAte = 1;
        estruturas.sujo = true;
        estruturas.salvar();
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        w.playSound(c, Sound.BLOCK_CONDUIT_ACTIVATE, 2f, 0.8f);
        w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.2f);
        w.spawnParticle(Particle.NAUTILUS, c, 300, 6, 4, 6, 1);
        w.spawnParticle(Particle.END_ROD, c, 80, 3, 3, 3, 0.05);
        // Os guardas descansam.
        for (Entity e : w.getNearbyEntities(s.ponto(w, 0, 6, 0), 48, 24, 48)) {
            String v = e.getPersistentDataContainer().get(Estruturas.K_GUARDA, PersistentDataType.STRING);
            if (v != null && v.startsWith(s.id) && e instanceof LivingEntity le) {
                w.spawnParticle(Particle.SOUL, le.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
                le.remove();
            }
        }
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) > 80 * 80) continue;
            o.showTitle(Title.title(Component.text("≈ " + s.nome + " desperta", NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text("O coração voltou. A cidade enfim descansa.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3500), Duration.ofMillis(1000))));
            plugin.diario().marco(o, "cidade_" + s.id, "Viu " + s.nome + " despertar");
            if (plugin.deuses().deus(o) == Deus.MARIS) {
                plugin.deuses().darDevocao(o, 80);
                o.sendMessage(Component.text(Deus.MARIS.simbolo() + " Maris agradece: a cidade dela descansa. (+80 devoção)", Deus.MARIS.cor()));
            }
        }
        plugin.titulos().registrar(p, "cidades_despertas", 1);
        plugin.getLogger().info(p.getName() + " despertou a cidade submersa " + s.nome + ".");
    }
}
