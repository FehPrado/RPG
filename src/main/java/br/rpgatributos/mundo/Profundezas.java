package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.ItemCeu;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * As Profundezas: abaixo do y 0 o mundo fica escuro de verdade. Raízes de Luz adormecidas
 * nascem nas cavernas fundas; um clique as acende e ilumina a área para sempre (e para todos).
 * No escuro aparecem criaturas da Penumbra, que roubam corações máximos: eles só voltam ao sol,
 * perto de uma raiz acesa ou com o Elixir da Purificação. Às vezes, acender uma raiz acorda o
 * Coração da Penumbra.
 */
public final class Profundezas implements Listener {

    private static final NamedTextColor COR = NamedTextColor.GOLD;
    private static final NamedTextColor ROXO = NamedTextColor.DARK_PURPLE;
    /** Até onde a luz de uma raiz acesa espanta a escuridão e a Penumbra. */
    private static final int RAIO_LUZ = 40;
    /** Tamanho das células do índice de raízes (a busca só olha as células em volta). */
    private static final int CELULA = 32;
    /** Quantos zumbis da Penumbra o Coração deixa vivos em volta dele, no máximo. */
    private static final int MAX_INVOCADOS = 6;
    private static final Material BULBO_ADORMECIDO = Material.BROWN_MUSHROOM_BLOCK, BULBO_ACESO = Material.SHROOMLIGHT;

    /** Uma Raiz de Luz (o bloco é o bulbo, no alto dela). */
    private static final class Raiz {
        final String mundo;
        final int x, y, z;
        boolean acesa;

        Raiz(String mundo, int x, int y, int z, boolean acesa) {
            this.mundo = mundo;
            this.x = x;
            this.y = y;
            this.z = z;
            this.acesa = acesa;
        }

        boolean perto(Location l, int raio) {
            if (!l.getWorld().getName().equals(mundo) || Math.abs(l.getY() - y) > 24) return false;
            double dx = l.getX() - x, dz = l.getZ() - z;
            return dx * dx + dz * dz <= (double) raio * raio;
        }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final NamespacedKey kPenumbra, kStacks, kModificador, kCoracao, kChunk;
    private final List<Raiz> raizes = new ArrayList<>();
    /** Índice das raízes: mundo → célula de 32x32 blocos → raízes ali. */
    private final Map<String, Map<Long, List<Raiz>>> grade = new HashMap<>();
    /** Luzes das Sementes Brilhantes: bloco → quando apagar. */
    private final Map<Block, Long> luzesTemporarias = new HashMap<>();
    /** Coração da Penumbra vivo → a barra de chefe dele. */
    private final Map<UUID, BossBar> coracoes = new HashMap<>();
    private final Map<UUID, Integer> curaSol = new HashMap<>();
    private boolean sujo;
    private int ciclo;

    public Profundezas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "profundezas.yml");
        this.kPenumbra = new NamespacedKey(plugin, "penumbra");
        this.kStacks = new NamespacedKey(plugin, "penumbra_coracoes");
        this.kModificador = new NamespacedKey(plugin, "penumbra_vida");
        this.kCoracao = new NamespacedKey(plugin, "coracao_penumbra");
        this.kChunk = new NamespacedKey(plugin, "raiz_v1");
    }

    private Settings cfg() {
        return plugin.settings();
    }

    private boolean mundoCerto(World w) {
        return w.getEnvironment() == World.Environment.NORMAL && !plugin.masmorras().ehMundo(w);
    }

    /** Quantos corações a Penumbra pode roubar (config). */
    private int maxPenumbra() {
        return cfg().profPenumbraMaxima;
    }

    private boolean fundo(Location l) {
        return cfg().profAtivadas && mundoCerto(l.getWorld()) && l.getY() < cfg().profAltura;
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        raizes.clear();
        grade.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String l : y.getStringList("raizes")) {
            String[] p = l.split(",");
            if (p.length < 5) continue;
            try {
                adicionar(new Raiz(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Boolean.parseBoolean(p[4])));
            } catch (NumberFormatException ignored) { }
        }
        // Luzes de Sementes Brilhantes que ficaram acesas (o servidor caiu antes de apagarem): voltam
        // para a lista e apagam na hora certa.
        for (String l : y.getStringList("luzes")) {
            String[] p = l.split(",");
            if (p.length < 5) continue;
            World w = Bukkit.getWorld(p[0]);
            if (w == null) continue;
            try {
                luzesTemporarias.put(w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])), Long.parseLong(p[4]));
            } catch (NumberFormatException ignored) { }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (Raiz r : raizes) l.add(r.mundo + "," + r.x + "," + r.y + "," + r.z + "," + r.acesa);
        y.set("raizes", l);
        List<String> luzes = new ArrayList<>();
        for (var en : luzesTemporarias.entrySet()) {
            Block b = en.getKey();
            luzes.add(b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ() + "," + en.getValue());
        }
        y.set("luzes", luzes);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar profundezas.yml: " + ex.getMessage());
        }
    }

    /** Ao desligar: apaga as luzes temporárias das Sementes Brilhantes e as barras de chefe. */
    public void parar() {
        for (Block b : luzesTemporarias.keySet()) if (b.getType() == Material.LIGHT) b.setType(Material.AIR, false);
        luzesTemporarias.clear();
        for (BossBar b : coracoes.values()) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(b);
        salvar();
    }

    public int acesas() {
        int n = 0;
        for (Raiz r : raizes) if (r.acesa) n++;
        return n;
    }

    public int total() {
        return raizes.size();
    }

    // =====================================================================
    //  Raízes de Luz: onde nascem
    // =====================================================================

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk() || !cfg().profAtivadas || !mundoCerto(e.getWorld())) return;
        Chunk c = e.getChunk();
        var pdc = c.getPersistentDataContainer();
        if (pdc.has(kChunk)) return;
        pdc.set(kChunk, PersistentDataType.BYTE, (byte) 1);
        World w = c.getWorld();
        Random r = new Random(w.getSeed() ^ (c.getX() * 0x9E3779B97F4A7C15L) ^ (c.getZ() * 0xC2B2AE3D27D4EB4FL) ^ 0x2A12L);
        if (r.nextDouble() >= cfg().profChanceRaiz) return;
        for (int t = 0; t < 8; t++) {
            int x = (c.getX() << 4) + 2 + r.nextInt(12), z = (c.getZ() << 4) + 2 + r.nextInt(12);
            Block chao = chaoDeCaverna(w, x, z, r);
            if (chao != null) {
                plantar(chao);
                return;
            }
        }
    }

    /** Um chão de caverna funda com 5 blocos de ar em cima (para a raiz e o bulbo). */
    private Block chaoDeCaverna(World w, int x, int z, Random r) {
        int topo = Math.min(cfg().profAltura - 6, -6), base = Math.max(w.getMinHeight() + 6, -56);
        for (int y = topo - r.nextInt(8); y > base; y--) {
            Block b = w.getBlockAt(x, y, z);
            if (!b.getType().isSolid() || b.isLiquid()) continue;
            boolean livre = true;
            for (int k = 1; k <= 5; k++) {
                Block a = b.getRelative(0, k, 0);
                if (!a.getType().isAir()) { livre = false; break; }
            }
            if (livre) return b;
        }
        return null;
    }

    /** A raiz: tronco de raízes de mangue, raízes penduradas e o bulbo adormecido em cima. */
    private Raiz plantar(Block chao) {
        chao.setType(Material.ROOTED_DIRT, false);
        for (int k = 1; k <= 3; k++) chao.getRelative(0, k, 0).setType(Material.MANGROVE_ROOTS, false);
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block lado = chao.getRelative(f);
            if (lado.getType().isSolid()) lado.setType(Material.ROOTED_DIRT, false);
            Block raiz = chao.getRelative(0, 1, 0).getRelative(f);
            if (raiz.getType().isAir()) raiz.setType(Material.MANGROVE_ROOTS, false);
        }
        Block bulbo = chao.getRelative(0, 4, 0);
        bulbo.setType(BULBO_ADORMECIDO, false);
        Raiz r = new Raiz(chao.getWorld().getName(), bulbo.getX(), bulbo.getY(), bulbo.getZ(), false);
        adicionar(r);
        sujo = true;
        return r;
    }

    // ---------- índice ----------

    private static long celula(int x, int z) {
        return ((long) Math.floorDiv(x, CELULA) << 32) ^ (Math.floorDiv(z, CELULA) & 0xFFFFFFFFL);
    }

    private void adicionar(Raiz r) {
        raizes.add(r);
        grade.computeIfAbsent(r.mundo, k -> new HashMap<>()).computeIfAbsent(celula(r.x, r.z), k -> new ArrayList<>()).add(r);
    }

    /** As raízes até {@code raio} blocos (no plano) de um ponto: só olha as células em volta. */
    private List<Raiz> raizesPerto(Location l, int raio) {
        Map<Long, List<Raiz>> m = grade.get(l.getWorld().getName());
        if (m == null) return List.of();
        List<Raiz> achadas = new ArrayList<>();
        int x = l.getBlockX(), z = l.getBlockZ();
        for (int cx = Math.floorDiv(x - raio, CELULA); cx <= Math.floorDiv(x + raio, CELULA); cx++) {
            for (int cz = Math.floorDiv(z - raio, CELULA); cz <= Math.floorDiv(z + raio, CELULA); cz++) {
                List<Raiz> l2 = m.get(((long) cx << 32) ^ (cz & 0xFFFFFFFFL));
                if (l2 == null) continue;
                for (Raiz r : l2) if (r.perto(l, raio)) achadas.add(r);
            }
        }
        return achadas;
    }

    private Raiz raizEm(Block b) {
        Map<Long, List<Raiz>> m = grade.get(b.getWorld().getName());
        List<Raiz> l = m == null ? null : m.get(celula(b.getX(), b.getZ()));
        if (l == null) return null;
        for (Raiz r : l) if (r.x == b.getX() && r.y == b.getY() && r.z == b.getZ()) return r;
        return null;
    }

    private boolean pertoDeLuz(Location l) {
        for (Raiz r : raizesPerto(l, RAIO_LUZ)) if (r.acesa) return true;
        return false;
    }

    /** Luz de tocha/lanterna forte ou dentro de um território amigo: a escuridão não entra. */
    private boolean protegido(Player p, Location l) {
        int luz = Math.max(l.getBlock().getLightFromBlocks(), p.getEyeLocation().getBlock().getLightFromBlocks());
        if (luz >= cfg().profLuzQueProtege) return true;
        var t = plugin.territorios().em(l);
        return t != null && plugin.territorios().amigo(t, p.getUniqueId());
    }

    // ---------- para a colônia ----------

    /** Há uma Raiz de Luz acesa num lugar que passa no filtro (ex.: debaixo do território de uma colônia)? */
    public boolean raizAcesa(String mundo, Predicate<Location> onde) {
        World w = Bukkit.getWorld(mundo);
        if (w == null) return false;
        for (Raiz r : raizes) if (r.acesa && r.mundo.equals(mundo) && onde.test(new Location(w, r.x, r.y, r.z))) return true;
        return false;
    }

    /** Algo sobe das profundezas: uma criatura da Penumbra aparece aqui (o poço da Mina da colônia). */
    public LivingEntity criaturaDaPenumbra(Location l) {
        boolean esqueleto = ThreadLocalRandom.current().nextDouble() < 0.35;
        LivingEntity m = esqueleto ? l.getWorld().spawn(l, org.bukkit.entity.Skeleton.class, this::marcarPenumbra)
                : l.getWorld().spawn(l, Zombie.class, this::marcarPenumbra);
        l.getWorld().spawnParticle(Particle.SQUID_INK, l.clone().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        l.getWorld().playSound(l, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.2f);
        return m;
    }

    // =====================================================================
    //  Acender uma raiz
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (b.getType() != BULBO_ADORMECIDO) return;
        Raiz r = raizEm(b);
        if (r == null || r.acesa) return;
        e.setCancelled(true);
        acender(e.getPlayer(), r, b);
    }

    private void acender(Player p, Raiz r, Block bulbo) {
        r.acesa = true;
        sujo = true;
        World w = bulbo.getWorld();
        bulbo.setType(BULBO_ACESO, false);
        // Luz espalhada pela caverna: blocos de luz invisíveis numa grade, só onde há ar.
        int postas = 0;
        for (int dx = -21; dx <= 21 && postas < 70; dx += 7) {
            for (int dz = -21; dz <= 21 && postas < 70; dz += 7) {
                if (dx * dx + dz * dz > 22 * 22) continue;
                for (int dy = -6; dy <= 8; dy += 2) {
                    Block a = bulbo.getRelative(dx, dy, dz);
                    if (!a.getType().isAir() || a.getLightFromSky() > 0) continue;
                    a.setType(Material.LIGHT, false);
                    if (a.getBlockData() instanceof Levelled lv) {
                        lv.setLevel(13);
                        a.setBlockData(lv, false);
                    }
                    postas++;
                    break;
                }
            }
        }
        Location c = bulbo.getLocation().add(0.5, 0.5, 0.5);
        w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
        w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.8f);
        w.spawnParticle(Particle.END_ROD, c, 80, 3, 2, 3, 0.08);
        w.spawnParticle(Particle.FLASH, c, 1);
        p.showTitle(Title.title(Component.text("✦ Raiz de Luz acesa", COR),
                Component.text("A escuridão recua por aqui.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2200), Duration.ofMillis(600))));
        w.dropItemNaturally(c, ItemCeu.SEMENTE_BRILHANTE.criar(2 + ThreadLocalRandom.current().nextInt(2)));
        p.giveExp(30);
        plugin.diario().marco(p, "raiz_de_luz", "Acendeu a primeira Raiz de Luz nas profundezas");
        plugin.titulos().registrar(p, "raizes", 1);
        // Toda a Penumbra de quem acendeu vai embora.
        curar(p);
        if (ThreadLocalRandom.current().nextDouble() < cfg().profChanceCoracao) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> despertarCoracao(bulbo.getLocation().add(0.5, -3, 0.5)), 60L);
        }
    }

    // =====================================================================
    //  Escuridão, cura e o Coração (a cada 2 s)
    // =====================================================================

    public void tick() {
        ciclo++;
        if (!cfg().profAtivadas) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location l = p.getLocation();
            boolean jogando = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
            int pen = penumbra(p);
            if (fundo(l)) {
                boolean luz = pertoDeLuz(l);
                if (cfg().profEscuridao && jogando && !luz && !p.hasPotionEffect(PotionEffectType.NIGHT_VISION) && !protegido(p, l)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, true, false, false));
                }
                if (luz && pen > 0) mudarPenumbra(p, -1);
                // Raízes adormecidas por perto brilham de leve, para serem achadas.
                for (Raiz r : raizesPerto(l, 28)) {
                    if (r.acesa) continue;
                    p.spawnParticle(Particle.END_ROD, r.x + 0.5, r.y + 0.6, r.z + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
                }
            } else if (pen > 0) {
                // Ao sol, a Penumbra vai embora aos poucos (1 coração a cada 10 s).
                boolean sol = p.getWorld().isDayTime() && l.getBlock().getLightFromSky() >= 14;
                if (sol && curaSol.merge(p.getUniqueId(), 1, Integer::sum) >= 5) {
                    curaSol.remove(p.getUniqueId());
                    mudarPenumbra(p, -1);
                }
            }
        }
        // Luzes das sementes que apagaram.
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<Block, Long>> it = luzesTemporarias.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (en.getValue() > agora) continue;
            if (en.getKey().getType() == Material.LIGHT) en.getKey().setType(Material.AIR, false);
            it.remove();
            sujo = true;
        }
        poderesDoCoracao();
        if (sujo && ciclo % 15 == 0) salvar();
    }

    // =====================================================================
    //  Penumbra
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Monster m) || e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Location l = m.getLocation();
        if (!fundo(l) || pertoDeLuz(l) || ThreadLocalRandom.current().nextDouble() >= cfg().profChancePenumbra) return;
        marcarPenumbra(m);
    }

    private void marcarPenumbra(LivingEntity m) {
        m.getPersistentDataContainer().set(kPenumbra, PersistentDataType.BYTE, (byte) 1);
        m.customName(Component.translatable(m.getType().translationKey(), ROXO).append(Component.text(" da Penumbra", ROXO)));
    }

    private boolean daPenumbra(Entity e) {
        if (e instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) e = atirador;
        return e.getPersistentDataContainer().has(kPenumbra);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoApanhar(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p) || !daPenumbra(e.getDamager())) return;
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        if (penumbra(p) >= maxPenumbra()) return;
        mudarPenumbra(p, 1);
        p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
    }

    public int penumbra(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kStacks, PersistentDataType.INTEGER, 0);
    }

    private void mudarPenumbra(Player p, int delta) {
        int antes = Math.min(penumbra(p), maxPenumbra()), depois = Math.max(0, Math.min(maxPenumbra(), antes + delta));
        if (antes == depois) return;
        p.getPersistentDataContainer().set(kStacks, PersistentDataType.INTEGER, depois);
        aplicarPenumbra(p);
        if (delta > 0) {
            p.sendActionBar(Component.text("☾ A Penumbra roubou 1 coração (" + depois + "/" + maxPenumbra() + ")", ROXO));
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.4f);
        } else if (depois == 0) {
            p.sendActionBar(Component.text("☀ A Penumbra foi embora", COR));
        } else {
            p.sendActionBar(Component.text("☀ Um coração voltou (" + depois + " ainda presos na Penumbra)", COR));
        }
    }

    private void aplicarPenumbra(Player p) {
        AttributeInstance vida = p.getAttribute(Attribute.MAX_HEALTH);
        if (vida == null) return;
        for (AttributeModifier m : List.copyOf(vida.getModifiers())) if (m.getKey().equals(kModificador)) vida.removeModifier(m);
        int n = Math.min(penumbra(p), maxPenumbra()); // se o limite da config baixou, vale o novo
        if (n > 0) vida.addModifier(new AttributeModifier(kModificador, -2.0 * n, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        if (p.getHealth() > vida.getValue()) p.setHealth(vida.getValue());
    }

    /** Elixir da Purificação, raiz acesa ou morte: tira toda a Penumbra. */
    public void curar(Player p) {
        if (penumbra(p) == 0) return;
        p.getPersistentDataContainer().set(kStacks, PersistentDataType.INTEGER, 0);
        aplicarPenumbra(p);
        p.sendActionBar(Component.text("☀ A Penumbra foi embora", COR));
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        aplicarPenumbra(e.getPlayer());
    }

    @EventHandler
    public void aoMorrerJogador(PlayerDeathEvent e) {
        curar(e.getPlayer());
    }

    // =====================================================================
    //  Semente Brilhante
    // =====================================================================

    @EventHandler
    public void aoAcertar(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball s) || ItemCeu.de(s.getItem()) != ItemCeu.SEMENTE_BRILHANTE) return;
        Block alvo = e.getHitBlock() != null && e.getHitBlockFace() != null ? e.getHitBlock().getRelative(e.getHitBlockFace())
                : s.getLocation().getBlock();
        if (!alvo.getType().isAir()) return;
        alvo.setType(Material.LIGHT, false);
        luzesTemporarias.put(alvo, System.currentTimeMillis() + 120_000);
        salvar(); // se o servidor cair, a luz não fica para sempre
        alvo.getWorld().spawnParticle(Particle.END_ROD, alvo.getLocation().add(0.5, 0.5, 0.5), 25, 0.4, 0.4, 0.4, 0.05);
        alvo.getWorld().playSound(alvo.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
    }

    // =====================================================================
    //  O Coração da Penumbra
    // =====================================================================

    private void despertarCoracao(Location l) {
        World w = l.getWorld();
        if (w == null) return;
        Block b = l.getBlock();
        for (int k = 0; k < 6 && !b.getType().isAir(); k++) b = b.getRelative(0, 1, 0);
        Location onde = b.getLocation().add(0.5, 0, 0.5);
        WitherSkeleton c = w.spawn(onde, WitherSkeleton.class, m -> {
            m.customName(Component.text("☾ Coração da Penumbra", ROXO));
            m.setCustomNameVisible(true);
            m.setRemoveWhenFarAway(false);
            m.getPersistentDataContainer().set(kCoracao, PersistentDataType.BYTE, (byte) 1);
            m.getPersistentDataContainer().set(kPenumbra, PersistentDataType.BYTE, (byte) 1);
            AttributeInstance vida = m.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) {
                vida.setBaseValue(350);
                m.setHealth(350);
            }
            AttributeInstance escala = m.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(1.5);
            AttributeInstance dano = m.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue(10);
            m.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            m.getEquipment().setItemInMainHandDropChance(0f);
        });
        registrarCoracao(c);
        w.playSound(onde, Sound.ENTITY_WARDEN_EMERGE, 1.5f, 0.8f);
        w.spawnParticle(Particle.SQUID_INK, onde.clone().add(0, 1, 0), 120, 1.5, 1.5, 1.5, 0.05);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(onde) > 40 * 40) continue;
            p.showTitle(Title.title(Component.text("☾ Coração da Penumbra", ROXO),
                    Component.text("A luz acordou o que dormia no escuro.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
    }

    private void registrarCoracao(LivingEntity c) {
        coracoes.computeIfAbsent(c.getUniqueId(),
                id -> BossBar.bossBar(Component.text("☾ Coração da Penumbra", ROXO), 1f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10));
    }

    /** O Coração continua sendo chefe depois de reiniciar o servidor (ou quando o chunk dele volta a carregar). */
    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) {
            if (x instanceof LivingEntity le && x.getPersistentDataContainer().has(kCoracao)) registrarCoracao(le);
        }
    }

    private void poderesDoCoracao() {
        for (Iterator<Map.Entry<UUID, BossBar>> it = coracoes.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = Bukkit.getEntity(en.getKey());
            if (!(e instanceof WitherSkeleton c) || !c.isValid() || c.isDead()) {
                for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(en.getValue());
                it.remove();
                continue;
            }
            AttributeInstance max = c.getAttribute(Attribute.MAX_HEALTH);
            en.getValue().progress((float) Math.max(0, Math.min(1, c.getHealth() / (max == null ? 350 : max.getValue()))));
            for (Player p : c.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(c.getLocation()) <= 40 * 40) p.showBossBar(en.getValue());
                else p.hideBossBar(en.getValue());
            }
            // A cada 10 s chama dois zumbis da Penumbra (até 6 vivos em volta dele).
            if (ciclo % 5 == 0 && c.getTarget() != null) {
                long vivos = c.getNearbyEntities(24, 12, 24).stream()
                        .filter(x -> x instanceof Zombie && x.getPersistentDataContainer().has(kPenumbra)).count();
                for (int i = 0; i < 2 && vivos + i < MAX_INVOCADOS; i++) {
                    Location l = c.getLocation().add(ThreadLocalRandom.current().nextInt(-3, 4), 0, ThreadLocalRandom.current().nextInt(-3, 4));
                    if (!l.getBlock().getType().isAir()) l = c.getLocation();
                    Zombie z = c.getWorld().spawn(l, Zombie.class, this::marcarPenumbra);
                    z.setTarget(c.getTarget());
                    c.getWorld().spawnParticle(Particle.SQUID_INK, l.clone().add(0, 1, 0), 15, 0.3, 0.6, 0.3, 0.02);
                }
            }
        }
    }

    @EventHandler
    public void aoMorrerCoracao(EntityDeathEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(kCoracao)) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        e.getDrops().clear();
        e.getDrops().add(ItemCeu.SEMENTE_BRILHANTE.criar(4 + r.nextInt(3)));
        e.getDrops().add(Raro.FRAGMENTO_DE_FORJA.criar(1 + r.nextInt(2)));
        e.getDrops().add(Raro.PEDRA_DE_PROTECAO.criar(1));
        if (r.nextDouble() < 0.4) e.getDrops().add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
        e.getDrops().add(new ItemStack(Material.ECHO_SHARD, 1 + r.nextInt(3)));
        e.setDroppedExp(150);
        ItemStack nomeada = plugin.nomeadas().talvezDeChefe(0.3);
        if (nomeada != null) e.getDrops().add(nomeada);
        Player quem = e.getEntity().getKiller();
        if (quem != null) {
            plugin.diario().marco(quem, "coracao_penumbra", "Derrotou o Coração da Penumbra");
            curar(quem);
        }
    }

    // =====================================================================
    //  Administrador
    // =====================================================================

    /** Planta uma raiz no chão de caverna mais perto embaixo de você. */
    public String plantarAqui(Player p) {
        Block b = p.getLocation().getBlock().getRelative(0, -1, 0);
        for (int k = 0; k < 10 && !b.getType().isSolid(); k++) b = b.getRelative(0, -1, 0);
        if (!b.getType().isSolid()) return "Fique em cima de um chão firme.";
        for (int k = 1; k <= 5; k++) if (!b.getRelative(0, k, 0).getType().isAir()) return "Precisa de 5 blocos de ar em cima do chão.";
        plantar(b);
        salvar();
        return null;
    }

    public void coracaoAqui(Player p) {
        despertarCoracao(p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(4)));
    }
}
