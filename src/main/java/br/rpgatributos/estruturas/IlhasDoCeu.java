package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import br.rpgatributos.exploracao.ItemCeu;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
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
 * As Ilhas do Céu: arquipélagos raros (no máximo um por região) bem acima do chão, cada um
 * com uma ilha principal, 3 ilhotas e uma construção (jardim, observatório, forja ou ninho).
 * No chão, à frente, fica a Plataforma de Vento: pisou, o jogador é lançado até a ilha.
 * Não há volta segura: quem subiu sem Capa, Balão ou élitro só desce caindo.
 */
final class IlhasDoCeu {

    private static final String[] NOMES = {"Aerília", "Nubéria", "Celéstia", "Ventória", "Zéfira", "Altamira", "Brumália", "Estratos"};
    /** Quanto acima do gramado da ilha o lançamento leva o jogador antes de empurrá-lo para dentro. */
    private static final int FOLGA = 12;

    private final RPGAtributos plugin;
    private final Estruturas estruturas;
    private final Projetos projetos;
    private final Map<Long, Integer> tentativas = new HashMap<>();
    private final Set<String> obrando = new HashSet<>();
    /** Quem está sendo lançado agora. */
    private final Set<UUID> voando = new HashSet<>();
    /** Quem acabou de ser lançado: o pouso na ilha não machuca (até quando, em ms). */
    private final Map<UUID, Long> pouso = new HashMap<>();
    private final Map<UUID, Sitio> pousoEm = new HashMap<>();
    private boolean avaliando;

    IlhasDoCeu(RPGAtributos plugin, Estruturas estruturas, Projetos projetos) {
        this.plugin = plugin;
        this.estruturas = estruturas;
        this.projetos = projetos;
    }

    private Settings cfg() {
        return plugin.settings();
    }

    static boolean pronta(Sitio s) {
        return s.fase >= Ilha.ETAPAS;
    }

    /** "Aerília, o Jardim Suspenso". */
    static String titulo(Sitio s) {
        Ilha.Tipo t = Ilha.tipo(s.semente);
        return (s.nome != null ? s.nome : "Ilha do Céu") + (t == Ilha.Tipo.FORJA ? ", a " : ", o ") + t.nome;
    }

    private long regiao(int x, int z) {
        int r = cfg().ilhaRegiao;
        return ((long) Math.floorDiv(x, r) << 32) | (Math.floorDiv(z, r) & 0xffffffffL);
    }

    private boolean temIlha(World w, long regiao) {
        for (Sitio s : estruturas.sitios) {
            if (s.tipo == Estrutura.ILHA_DO_CEU && s.mundo.equals(w.getName()) && regiao(s.x, s.z) == regiao) return true;
        }
        return false;
    }

    // =====================================================================
    //  Onde nascem
    // =====================================================================

    /** Um chunk novo carregou: em terra firme, talvez uma ilha nasça lá em cima. */
    void talvez(Chunk c) {
        if (!cfg().ilhaAtivada || avaliando) return;
        World w = c.getWorld();
        int x = c.getX() * 16 + 8, z = c.getZ() * 16 + 8;
        Location spawn = w.getSpawnLocation();
        double dx = x - spawn.getX(), dz = z - spawn.getZ();
        if (dx * dx + dz * dz < (double) cfg().ilhaDistanciaSpawn * cfg().ilhaDistanciaSpawn) return;
        String bioma = w.getBiome(x, w.getHighestBlockYAt(x, z), z).getKey().getKey();
        if (!Estrutura.ILHA_DO_CEU.aceita(bioma)) return;
        long reg = regiao(x, z);
        if (tentativas.getOrDefault(reg, 0) >= 6 || temIlha(w, reg)) return;
        Random r = new Random(w.getSeed() ^ (c.getX() * 0x9E3779B97F4A7C15L) ^ (c.getZ() * 0xC2B2AE3D27D4EB4FL) ^ 0x1A4AL);
        if (r.nextDouble() >= cfg().ilhaChance) return;
        tentativas.merge(reg, 1, Integer::sum);
        avaliando = true;
        long semente = r.nextLong();
        int rot = r.nextInt(4);
        carregar(w, x, z).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            avaliando = false;
            if (ex == null) avaliar(w, x, z, rot, semente);
        }));
    }

    private CompletableFuture<Void> carregar(World w, int x, int z) {
        List<CompletableFuture<Chunk>> l = new ArrayList<>();
        int raio = Math.max(Ilha.ALCANCE, Ilha.PLATAFORMA + 4);
        for (int cx = (x - raio) >> 4; cx <= (x + raio) >> 4; cx++) {
            for (int cz = (z - raio) >> 4; cz <= (z + raio) >> 4; cz++) l.add(w.getChunkAtAsync(cx, cz, true));
        }
        return CompletableFuture.allOf(l.toArray(new CompletableFuture[0]));
    }

    /** A altura da ilha: bem acima do chão mais alto em volta (null se não cabe no céu). */
    private Integer altura(World w, int x, int z) {
        int chao = w.getMinHeight();
        for (int dx = -Ilha.ALCANCE; dx <= Ilha.ALCANCE; dx += 6) {
            for (int dz = -Ilha.ALCANCE; dz <= Ilha.ALCANCE; dz += 6) {
                chao = Math.max(chao, w.getHighestBlockYAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING));
            }
        }
        int teto = w.getMaxHeight() - 26;
        int y0 = Math.max(chao + 75, 200);
        if (y0 <= teto) return y0;
        return chao + 45 <= teto ? teto : null;
    }

    /** O giro em que a plataforma cai em chão firme e aberto (sem copa de árvore em cima); -1 se nenhum. */
    private int giroComChao(World w, int x, int y0, int z, int preferido) {
        for (int k = 0; k < 4; k++) {
            int rot = (preferido + k) % 4;
            Block b = new Obra(w, x, y0, z, rot, null).bloco(0, 0, Ilha.PLATAFORMA);
            int comFolhas = w.getHighestBlockYAt(b.getX(), b.getZ(), HeightMap.MOTION_BLOCKING);
            int semFolhas = w.getHighestBlockYAt(b.getX(), b.getZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block chao = w.getBlockAt(b.getX(), semFolhas, b.getZ());
            if (comFolhas != semFolhas || chao.isLiquid() || !chao.getType().isSolid()) continue;
            // Chão quase plano em volta (a plataforma não fica afundada numa encosta).
            boolean plano = true;
            for (int[] d : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}, {0, -3}, {0, 3}, {-3, 0}, {3, 0}}) {
                int h = w.getHighestBlockYAt(b.getX() + d[0], b.getZ() + d[1], HeightMap.MOTION_BLOCKING_NO_LEAVES);
                if (Math.abs(h - semFolhas) > 2) { plano = false; break; }
            }
            if (plano) return rot;
        }
        return -1;
    }

    private void avaliar(World w, int x, int z, int rot, long semente) {
        Integer y0 = altura(w, x, z);
        if (y0 == null) return;
        int raio = Math.max(Ilha.ALCANCE, Ilha.PLATAFORMA + 4);
        for (int cx = (x - raio) >> 4; cx <= (x + raio) >> 4; cx++) {
            for (int cz = (z - raio) >> 4; cz <= (z + raio) >> 4; cz++) {
                if (plugin.territorios().em(w.getName(), cx, cz) != null) return;
            }
        }
        if (estruturas.perto(w, x, z, 120) != null) return;
        int giro = giroComChao(w, x, y0, z, rot);
        if (giro < 0) return;
        fundar(w, x, y0, z, giro, semente);
    }

    private Sitio fundar(World w, int x, int y0, int z, int rot, long semente) {
        Sitio s = new Sitio(UUID.randomUUID().toString().substring(0, 8), Estrutura.ILHA_DO_CEU, w.getName(), x, y0, z, rot);
        s.nome = NOMES[new Random(semente).nextInt(NOMES.length)];
        s.semente = semente;
        Block b = new Obra(w, x, y0, z, rot, null).bloco(0, 0, Ilha.PLATAFORMA);
        s.fundo = w.getHighestBlockYAt(b.getX(), b.getZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
        estruturas.adicionar(s);
        plugin.getLogger().info("Ilha do Céu nova: " + titulo(s) + " em " + w.getName() + " " + x + " " + y0 + " " + z
                + " (plataforma no chão em y " + s.fundo + ")");
        obra(s);
        return s;
    }

    /** Pelo administrador: em cima de (x, z), sem conferir território nem distância. */
    String fundarAqui(World w, int x, int z) {
        Integer y0 = altura(w, x, z);
        if (y0 == null) return "Não cabe no céu aqui (montanha alta demais).";
        int giro = giroComChao(w, x, y0, z, ThreadLocalRandom.current().nextInt(4));
        fundar(w, x, y0, z, Math.max(0, giro), ThreadLocalRandom.current().nextLong());
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
            int raio = Math.max(Ilha.ALCANCE, Ilha.PLATAFORMA + 4);
            List<Chunk> presos = new ArrayList<>();
            for (int cx = (s.x - raio) >> 4; cx <= (s.x + raio) >> 4; cx++) {
                for (int cz = (s.z - raio) >> 4; cz <= (s.z + raio) >> 4; cz++) {
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
                        plugin.getLogger().info("Ilha do Céu " + titulo(s) + " pronta.");
                        return;
                    }
                    Obra o = new Obra(w, s.x, s.y, s.z, s.rot, new Random(s.semente + s.fase * 7919L));
                    try {
                        Ilha.etapa(s.fase, o, s.semente);
                        if (s.fase == 4) encherBau(o, Ilha.tipo(s.semente));
                        if (s.fase == Ilha.ETAPAS - 1) Ilha.plataforma(o, s.fundo - s.y);
                    } catch (RuntimeException e) {
                        plugin.getLogger().warning("Ilha do Céu " + s.nome + ", etapa " + s.fase + ": " + e);
                    }
                    s.fase++;
                    estruturas.sujo = true;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }));
    }

    /** O baú da construção, com o que combina com ela (e lã e linha para quem quiser um Balão). */
    private void encherBau(Obra o, Ilha.Tipo t) {
        int[] onde = switch (t) {
            case JARDIM -> new int[]{-2, 1, -2};
            case OBSERVATORIO, NINHO -> new int[]{2, 1, -2};
            case FORJA -> new int[]{1, 1, 2};
        };
        List<ItemStack> extras = new ArrayList<>();
        extras.add(new ItemStack(Material.STRING, 2 + o.r.nextInt(4)));
        extras.add(new ItemStack(Material.WHITE_WOOL, 1 + o.r.nextInt(3)));
        switch (t) {
            case JARDIM -> {
                extras.add(new ItemStack(Material.CHERRY_SAPLING, 1 + o.r.nextInt(2)));
                extras.add(ItemCeu.FRUTA_CELESTE.criar(2 + o.r.nextInt(2)));
                extras.add(new ItemStack(Material.HONEY_BOTTLE, 1 + o.r.nextInt(2)));
                extras.add(Reagente.TINTURA_VITAL.criar(1 + o.r.nextInt(2)));
            }
            case OBSERVATORIO -> {
                extras.add(new ItemStack(Material.SPYGLASS));
                extras.add(Reagente.SAL_LUNAR.criar(1 + o.r.nextInt(2)));
                extras.add(Reagente.PO_ARCANO.criar(2 + o.r.nextInt(3)));
            }
            case FORJA -> {
                extras.add(Raro.FRAGMENTO_DE_FORJA.criar(1 + o.r.nextInt(2)));
                extras.add(new ItemStack(Material.IRON_INGOT, 3 + o.r.nextInt(5)));
                extras.add(new ItemStack(Material.GOLD_INGOT, 1 + o.r.nextInt(3)));
            }
            case NINHO -> {
                extras.add(new ItemStack(Material.PHANTOM_MEMBRANE, 2 + o.r.nextInt(3)));
                extras.add(new ItemStack(Material.FEATHER, 4 + o.r.nextInt(6)));
                extras.add(new ItemStack(Material.WIND_CHARGE, 4 + o.r.nextInt(5)));
            }
        }
        projetos.bau(o.bloco(onde[0], onde[1], onde[2]), LootTables.SIMPLE_DUNGEON, o, extras);
    }

    /** Ao ligar: termina as obras que o servidor desligou no meio. */
    void retomar() {
        for (Sitio s : new ArrayList<>(estruturas.sitios)) {
            if (s.tipo == Estrutura.ILHA_DO_CEU && !pronta(s)) obra(s);
        }
    }

    // =====================================================================
    //  Plataforma de Vento e o lançamento
    // =====================================================================

    /** A placa de pressão da plataforma. */
    static Block placa(Sitio s, World w) {
        return s.bloco(w, 0, s.fundo + 1 - s.y, Ilha.PLATAFORMA);
    }

    /** Alguém pisou numa placa de pressão: se for de uma plataforma, lança. */
    boolean pisou(Player p, Block b) {
        if (b.getType() != Material.POLISHED_BLACKSTONE_PRESSURE_PLATE || voando.contains(p.getUniqueId())) return false;
        for (Sitio s : estruturas.sitios) {
            if (s.tipo != Estrutura.ILHA_DO_CEU || !pronta(s) || !s.mundo.equals(b.getWorld().getName())) continue;
            if (placa(s, b.getWorld()).equals(b)) {
                lancar(p, s);
                return true;
            }
        }
        return false;
    }

    private boolean podeDescer(Player p) {
        ItemStack peito = p.getInventory().getChestplate();
        return plugin.mobilidade().temPlanador(p) || (peito != null && peito.getType() == Material.ELYTRA);
    }

    private void lancar(Player p, Sitio s) {
        if (!podeDescer(p)) {
            p.showTitle(Title.title(Component.text("⚠ Sem planador!", NamedTextColor.RED),
                    Component.text("Lá em cima, só a queda te traz de volta.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2500), Duration.ofMillis(500))));
        }
        UUID id = p.getUniqueId();
        voando.add(id);
        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 0.7f);
        w.spawnParticle(Particle.GUST_EMITTER_SMALL, p.getLocation(), 1);
        double alvoY = s.y + FOLGA;
        new BukkitRunnable() {
            int t;
            boolean subindo = true;

            @Override
            public void run() {
                t++;
                if (!p.isOnline() || p.isDead() || !p.getWorld().getName().equals(s.mundo) || t > 220) {
                    fim();
                    return;
                }
                Location l = p.getLocation();
                if (subindo) {
                    if (l.getY() < alvoY) {
                        p.setVelocity(new Vector(0, 1.6, 0));
                        p.setFallDistance(0);
                        if (t % 2 == 0) w.spawnParticle(Particle.CLOUD, l, 3, 0.2, 0.1, 0.2, 0.02);
                        return;
                    }
                    subindo = false;
                    w.playSound(l, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.4f);
                }
                Vector h = new Vector(s.x + 0.5 - l.getX(), 0, s.z + 0.5 - l.getZ());
                double d = h.length();
                if (d < 5) {
                    fim();
                    return;
                }
                h.normalize().multiply(Math.min(1.0, 0.3 + d * 0.04));
                h.setY(t % 3 == 0 ? 0.06 : 0.02);
                p.setVelocity(h);
                p.setFallDistance(0);
            }

            private void fim() {
                cancel();
                voando.remove(id);
                pouso.put(id, System.currentTimeMillis() + 10_000);
                pousoEm.put(id, s);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** O pouso de quem acabou de ser lançado, em cima da ilha, não machuca. */
    void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        if (voando.contains(id)) {
            e.setCancelled(true);
            return;
        }
        Long ate = pouso.remove(id);
        Sitio s = pousoEm.remove(id);
        if (ate == null || s == null || ate < System.currentTimeMillis()) return;
        if (p.getLocation().getY() >= s.y - 4 && s.distancia2(p.getLocation()) <= (Ilha.RAIO + 6) * (Ilha.RAIO + 6)) e.setCancelled(true);
    }

    // =====================================================================
    //  O Guardião dos Ventos (no Ninho)
    // =====================================================================

    private final Map<String, UUID> guardioes = new HashMap<>();
    private final Map<String, net.kyori.adventure.bossbar.BossBar> barras = new HashMap<>();

    private Breeze guardiao(Sitio s) {
        UUID id = guardioes.get(s.id);
        return id != null && Bukkit.getEntity(id) instanceof Breeze b && b.isValid() && !b.isDead() ? b : null;
    }

    private void invocar(Sitio s, World w) {
        Location l = s.ponto(w, 0, 2, 0);
        Breeze b = estruturas.guarda(s, l, Breeze.class, "Guardião dos Ventos", NamedTextColor.AQUA, m -> {
            m.getPersistentDataContainer().set(Estruturas.K_GUARDA, PersistentDataType.STRING, s.id + ":ventos");
            m.setRemoveWhenFarAway(false);
            m.setCustomNameVisible(true);
            Estruturas.vida(m, 320);
            AttributeInstance escala = m.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(2.4);
            AttributeInstance dano = m.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue(8);
        });
        guardioes.put(s.id, b.getUniqueId());
        w.playSound(l, Sound.ENTITY_BREEZE_IDLE_AIR, 2f, 0.5f);
        w.spawnParticle(Particle.GUST_EMITTER_LARGE, l, 2);
        for (Player o : w.getPlayers()) {
            if (s.distancia2(o.getLocation()) > 48 * 48) continue;
            o.showTitle(Title.title(Component.text("≋ Guardião dos Ventos", NamedTextColor.AQUA),
                    Component.text("O ninho não é lugar para visitas.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
    }

    /** A cada 2 s: barra de chefe e, com menos da metade da vida, rajadas que jogam todo mundo para o alto. */
    void poderes(int ciclo) {
        for (var it = barras.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Sitio s = null;
            for (Sitio x : estruturas.sitios) if (x.id.equals(en.getKey())) s = x;
            if (s != null && guardiao(s) != null) continue;
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(en.getValue());
            it.remove();
        }
        for (var en : guardioes.entrySet()) {
            if (!(Bukkit.getEntity(en.getValue()) instanceof Breeze b) || !b.isValid()) continue;
            var max = b.getAttribute(Attribute.MAX_HEALTH);
            float prog = max == null ? 1f : (float) Math.max(0, Math.min(1, b.getHealth() / max.getValue()));
            var barra = barras.computeIfAbsent(en.getKey(), k -> net.kyori.adventure.bossbar.BossBar.bossBar(
                    Component.text("≋ Guardião dos Ventos", NamedTextColor.AQUA), 1f,
                    net.kyori.adventure.bossbar.BossBar.Color.BLUE, net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10));
            barra.progress(prog);
            for (Player p : b.getWorld().getPlayers()) {
                double d2 = p.getLocation().distanceSquared(b.getLocation());
                if (d2 <= 40 * 40) p.showBossBar(barra);
                else p.hideBossBar(barra);
                if (prog < 0.5 && ciclo % 3 == 0 && d2 <= 7 * 7 && p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                    p.setVelocity(p.getVelocity().setY(1.1));
                    p.getWorld().spawnParticle(Particle.GUST, p.getLocation(), 2);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1f, 0.8f);
                }
            }
        }
    }

    void guardiaoMorreu(EntityDeathEvent e, String id) {
        Sitio s = null;
        for (Sitio x : estruturas.sitios) if (x.id.equals(id)) s = x;
        if (s == null) return;
        s.chefeEm = System.currentTimeMillis();
        guardioes.remove(s.id);
        estruturas.sujo = true;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        e.getDrops().add(Raro.FRAGMENTO_CELESTE.criar(2 + r.nextInt(2)));
        e.getDrops().add(ItemCeu.FRUTA_CELESTE.criar(2));
        e.getDrops().add(new ItemStack(Material.PHANTOM_MEMBRANE, 2 + r.nextInt(3)));
        e.getDrops().add(new ItemStack(Material.BREEZE_ROD, 2 + r.nextInt(3)));
        e.getDrops().add(new ItemStack(Material.EMERALD, 6 + r.nextInt(7)));
        if (r.nextDouble() < 0.15) e.getDrops().add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
        e.setDroppedExp(e.getDroppedExp() + 120);
        ItemStack nomeada = plugin.nomeadas().talvezDeChefe(0.25);
        if (nomeada != null) e.getDrops().add(nomeada);
        Player quem = e.getEntity().getKiller();
        if (quem != null) plugin.diario().marco(quem, "guardiao_ventos", "Derrotou o Guardião dos Ventos de " + s.nome);
    }

    // =====================================================================
    //  Vida das ilhas (a cada 2 s, para cada jogador perto)
    // =====================================================================

    void tick(Sitio s, World w, Player p, boolean jogando, double d2, double dy) {
        if (!pronta(s)) return;
        long agora = System.currentTimeMillis();
        Ilha.Tipo tipo = Ilha.tipo(s.semente);
        // A Árvore Celeste solta uma fruta de vez em quando para quem está embaixo dela.
        if (tipo == Ilha.Tipo.JARDIM && d2 <= 12 * 12 && dy >= -1 && dy <= 14 && agora - s.ronda > 5 * 60_000L) {
            s.ronda = agora;
            estruturas.sujo = true;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            Location l = s.ponto(w, r.nextInt(-3, 4), 8, r.nextInt(-3, 4));
            w.dropItemNaturally(l, ItemCeu.FRUTA_CELESTE.criar(1));
            w.spawnParticle(Particle.CHERRY_LEAVES, l, 12, 1, 0.5, 1, 0);
        }
        // Quem pisa no ninho acorda o Guardião dos Ventos (volta depois de uns dias).
        if (tipo == Ilha.Tipo.NINHO && jogando && d2 <= 9 * 9 && dy >= -2 && dy <= 10
                && (s.chefeEm == 0 || agora - s.chefeEm > cfg().cidadeDiasParaVoltar * 86_400_000L) && guardiao(s) == null) {
            invocar(s, w);
        }
        // Vento lá em cima.
        if (d2 <= 40 * 40 && dy >= -25 && dy <= 30) {
            Location l = p.getLocation();
            w.spawnParticle(Particle.CLOUD, l.clone().add(0, 1, 0), 4, 6, 2, 6, 0.04);
            if (ThreadLocalRandom.current().nextInt(4) == 0) p.playSound(l, Sound.ITEM_ELYTRA_FLYING, 0.08f, 0.6f);
        }
        // A plataforma rodopia quando alguém chega perto dela.
        Block placa = placa(s, w);
        Location c = placa.getLocation().add(0.5, 0.2, 0.5);
        if (c.distanceSquared(p.getLocation()) <= 20 * 20) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + (System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2;
                w.spawnParticle(Particle.CLOUD, c.clone().add(Math.cos(a) * 1.2, 0.3 + i * 0.25, Math.sin(a) * 1.2), 1, 0, 0, 0, 0);
            }
        }
    }
}
