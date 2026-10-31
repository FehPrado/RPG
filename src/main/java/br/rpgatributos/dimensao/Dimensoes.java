package br.rpgatributos.dimensao;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.3: as dimensões do RPG. Cada uma é um mundo do datapack (terreno, céu e camadas); aqui ficam
 * as regras de entrada e saída:
 * <ul>
 *   <li>Ao entrar, o jogo guarda de onde você veio (no próprio jogador).</li>
 *   <li>Na chegada fica o <b>Rasgo de Volta</b> (um bloco de ardósia reforçada com obsidiana chorona
 *       em volta): agachar em cima dele por 2 s devolve você ao lugar de onde veio. Cada jogador tem
 *       o seu; voltar pela entrada traz de novo ao mesmo rasgo. As Pedras de Viagem também funcionam.</li>
 *   <li>Morrer lá dentro: renasce em casa e os itens ficam no túmulo, lá dentro (os Túmulos de sempre).</li>
 *   <li>Cada dimensão tem borda de 5 mil blocos e um aviso a cada camada nova.</li>
 * </ul>
 * Uma dimensão posta pelo datapack não sai mais do mundo: para desligar, o config bloqueia a entrada.
 */
public final class Dimensoes implements Listener {

    public enum Dimensao {
        ABISMO("abismo", "O Abismo", 0x9575CD, "Lá embaixo, os Antigos guardaram o que não queriam que ninguém achasse.", false),
        // 3.5: as Luas (só se chega de foguete; veja Foguetes)
        LUA_PRATEADA("lua_prateada", "A Lua Prateada", 0xD8DCE8, "Silêncio, poeira e a Terra que você não consegue ver.", true),
        // 3.6: as outras luas (o Núcleo escolhe pelo céu da hora do lançamento)
        LUA_ESCURA("lua_escura", "A Lua Escura", 0x6A6A8A, "Nem as estrelas brilham aqui. Algo enxerga você.", true),
        LUA_VERMELHA("lua_vermelha", "A Lua Vermelha", 0xE53935, "A poeira tem cor de sangue, e o céu também.", true),
        LUA_CRISTAL("lua_cristal", "A Lua de Cristal", 0x64B5F6, "O chão canta quando você pisa.", true),
        LUA_SOMBRIA("lua_sombria", "A Lua Sombria", 0x8E24AA, "Ela só abre no eclipse. E fecha logo.", true),
        // 3.7: o Mundo das Almas (só se chega pelo ritual; veja Almas)
        ALMAS("almas", "O Mundo das Almas", 0x7FD8E8, "Tudo é cinza e silencioso. Os Antigos chegaram aqui antes de você.", false);

        final String id, nome, frase;
        final TextColor cor;
        final boolean lua;

        Dimensao(String id, String nome, int cor, String frase, boolean lua) {
            this.id = id;
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.frase = frase;
            this.lua = lua;
        }

        public boolean lua() { return lua; }
        public String frase() { return frase; }
        public TextColor cor() { return cor; }

        public String id() { return id; }
        public String nome() { return nome; }

        public NamespacedKey chave() { return new NamespacedKey("rpgatributos", id); }

        public static Dimensao porId(String s) {
            for (Dimensao d : values()) if (d.id.equalsIgnoreCase(s)) return d;
            return null;
        }
    }

    /** As camadas do Abismo, pelo bioma (o datapack escolhe o bioma pela profundidade). */
    private static final Map<String, String> CAMADAS = Map.of(
            "abismo_borda", "1. Borda dos Ventos",
            "abismo_floresta", "2. Floresta Pendurada",
            "abismo_cristal", "3. Campos de Cristal",
            "abismo_fungos", "4. Mar de Fungos",
            "abismo_fundo", "5. O Fundo",
            "almas_margem", "A Margem",
            "almas_campos", "Campos de Lembranças",
            "almas_ruinas", "Ruínas dos Antigos");

    private static final int BORDA = 5000;
    private final RPGAtributos plugin;
    private final NamespacedKey kVolta, kRasgo;
    private final Map<UUID, String> camadaAtual = new HashMap<>();
    private final Map<UUID, Integer> agachado = new HashMap<>();

    public Dimensoes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kVolta = new NamespacedKey(plugin, "dimensao_volta");
        this.kRasgo = new NamespacedKey(plugin, "dimensao_rasgo");
    }

    /** O mundo da dimensão (null se o datapack ainda não carregou: falta reiniciar). */
    public World mundo(Dimensao d) {
        return Bukkit.getWorld(d.chave());
    }

    /** A dimensão deste mundo, ou null se é um mundo comum. */
    public static Dimensao de(World w) {
        if (w == null || !w.getKey().getNamespace().equals("rpgatributos")) return null;
        return Dimensao.porId(w.getKey().getKey());
    }

    public boolean ativa(Dimensao d) {
        return plugin.getConfig().getBoolean("dimensoes." + d.id + ".ativa", true);
    }

    /** Ao ligar: borda de 5 mil blocos e regras de cada dimensão. */
    public void preparar() {
        for (Dimensao d : Dimensao.values()) {
            World w = mundo(d);
            if (w == null) continue;
            WorldBorder b = w.getWorldBorder();
            if (b.getSize() > BORDA + 1) {
                b.setCenter(0, 0);
                b.setSize(BORDA);
            }
            w.setGameRule(GameRule.DO_INSOMNIA, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            plugin.getLogger().info("Dimensão " + d.nome + " pronta (" + w.getName() + ", y " + w.getMinHeight() + " a " + w.getMaxHeight() + ").");
        }
    }

    // =====================================================================
    //  Entrar e sair
    // =====================================================================

    /** @return mensagem de erro ou null. */
    public String entrar(Player p, Dimensao d) {
        if (!ativa(d)) return d.nome + " está fechado neste servidor.";
        World w = mundo(d);
        if (w == null) return d.nome + " ainda não existe: o servidor precisa reiniciar uma vez (datapack novo).";
        if (d == Dimensao.ALMAS) {
            // Admin: o ritual direto (sem punhal); a lanterna vem junto.
            String erro = plugin.almas().podeSacrificar(p);
            if (erro != null) return erro;
            plugin.almas().sacrificar(p);
            return null;
        }
        if (de(p.getWorld()) == null) guardarVolta(p);
        if (d.lua()) {
            // Admin (de verdade, só se chega de foguete): direto num chão firme da lua.
            for (int i = 0; i < 30; i++) {
                int x = ThreadLocalRandom.current().nextInt(-600, 601), z = ThreadLocalRandom.current().nextInt(-600, 601);
                Block b = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING);
                if (b.getY() < 40 || !b.getType().isSolid()) continue;
                p.setFallDistance(0);
                p.teleport(b.getLocation().add(0.5, 1, 0.5));
                return null;
            }
            return "Não achei chão firme na lua.";
        }
        Location rasgo = rasgoDe(p, w);
        if (rasgo == null) {
            rasgo = acharChegada(w);
            if (rasgo == null) return "Não achei um lugar firme para chegar. Tente de novo.";
            construirRasgo(rasgo.getBlock());
            p.getPersistentDataContainer().set(kRasgo, PersistentDataType.STRING, w.getName() + "," + rasgo.getBlockX() + "," + rasgo.getBlockY() + "," + rasgo.getBlockZ());
        }
        Location chegada = rasgo.clone().add(2.5, 1, 0.5);
        chegada.setYaw(p.getLocation().getYaw());
        p.setFallDistance(0);
        p.teleport(chegada);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false, false));
        p.playSound(chegada, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.6f);
        p.showTitle(Title.title(Component.text(d.nome, d.cor, TextDecoration.BOLD), Component.text(d.frase, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))));
        p.sendMessage(Component.text("✦ Para voltar: agache em cima do Rasgo de Volta (o bloco escuro com obsidiana chorona em volta) por 2 segundos.", d.cor));
        plugin.diario().marco(p, "dimensao_" + d.id, "Entrou n" + (d == Dimensao.ABISMO ? "o " : "a ") + d.nome.replaceFirst("^(O|A) ", ""));
        plugin.titulos().registrar(p, "dimensoes", 1);
        return null;
    }

    /** Volta para onde estava antes de entrar (ou para o começo do mundo, se não houver). */
    public void sair(Player p) {
        Location l = volta(p);
        if (l == null) l = Bukkit.getWorlds().getFirst().getSpawnLocation();
        p.setFallDistance(0);
        p.teleport(l);
        p.playSound(l, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.4f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false, false));
    }

    public void guardarVolta(Player p) {
        Location l = p.getLocation();
        p.getPersistentDataContainer().set(kVolta, PersistentDataType.STRING,
                l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw());
    }

    public Location volta(Player p) {
        String v = p.getPersistentDataContainer().get(kVolta, PersistentDataType.STRING);
        if (v == null) return null;
        String[] s = v.split(",");
        World w = Bukkit.getWorld(s[0]);
        if (w == null) return null;
        Location l = new Location(w, Double.parseDouble(s[1]), Double.parseDouble(s[2]), Double.parseDouble(s[3]), Float.parseFloat(s[4]), 0);
        // Voltar caindo na fenda de novo seria um laço: sai ao lado, em chão firme.
        Block b = w.getHighestBlockAt(l.getBlockX() + 4, l.getBlockZ() + 4, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return b.getLocation().add(0.5, 1, 0.5);
    }

    private Location rasgoDe(Player p, World w) {
        String v = p.getPersistentDataContainer().get(kRasgo, PersistentDataType.STRING);
        if (v == null) return null;
        String[] s = v.split(",");
        if (!s[0].equals(w.getName())) return null;
        Location l = new Location(w, Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
        if (l.getBlock().getType() != Material.REINFORCED_DEEPSLATE) construirRasgo(l.getBlock());
        return l;
    }

    /** Um chão firme no planalto (acima da Floresta Pendurada), ao acaso, longe da borda. */
    private Location acharChegada(World w) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(-1500, 1501), z = r.nextInt(-1500, 1501);
            Block b = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (b.getY() < 150 || !b.getType().isSolid() || b.isLiquid()) continue;
            boolean firme = true;
            for (int dx = -2; dx <= 2 && firme; dx++) for (int dz = -2; dz <= 2 && firme; dz++)
                if (Math.abs(w.getHighestBlockYAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES) - b.getY()) > 1) firme = false;
            if (firme) return b.getLocation();
        }
        return null;
    }

    /** O Rasgo: ardósia reforçada no chão, obsidiana chorona nas diagonais e ar em cima. */
    private static void construirRasgo(Block centro) {
        centro.setType(Material.REINFORCED_DEEPSLATE);
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{-1, 1}) centro.getRelative(dx, 0, dz).setType(Material.CRYING_OBSIDIAN);
        for (int dy = 1; dy <= 2; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Block b = centro.getRelative(dx, dy, dz);
            if (!b.getType().isAir()) b.setType(Material.AIR);
        }
    }

    /** Quem cai dentro da Fenda do Abismo não se machuca: vai direto para o Abismo. */
    @EventHandler(ignoreCancelled = true)
    public void aoCairNaFenda(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        if (plugin.estruturas() == null || !plugin.estruturas().naFenda(p.getLocation())) return;
        e.setCancelled(true);
        String erro = entrar(p, Dimensao.ABISMO);
        if (erro != null) p.sendActionBar(Component.text(erro, NamedTextColor.RED));
    }

    /** O Rasgo de Volta não quebra (a ardósia reforçada já não sai; a obsidiana em volta também não). */
    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (de(e.getBlock().getWorld()) == null) return;
        Material m = e.getBlock().getType();
        if (m != Material.CRYING_OBSIDIAN && m != Material.REINFORCED_DEEPSLATE) return;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (e.getBlock().getRelative(dx, 0, dz).getType() == Material.REINFORCED_DEEPSLATE) {
                e.setCancelled(true);
                return;
            }
    }

    // =====================================================================
    //  A cada segundo
    // =====================================================================

    public void segundo() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Dimensao d = de(p.getWorld());
            if (d == null) {
                camadaAtual.remove(p.getUniqueId());
                agachado.remove(p.getUniqueId());
                continue;
            }
            // A camada nova.
            String bioma = p.getWorld().getBiome(p.getLocation()).getKey().getKey();
            String camada = CAMADAS.get(bioma);
            if (camada != null && !camada.equals(camadaAtual.put(p.getUniqueId(), camada))) {
                p.sendActionBar(Component.text((d == Dimensao.ABISMO ? "⬇ " : "✦ ") + camada, d.cor, TextDecoration.BOLD));
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.6f);
            }
            // O Rasgo: partículas e a volta (agachado em cima por 2 s).
            Block pe = p.getLocation().getBlock().getRelative(0, -1, 0);
            if (pe.getType() == Material.REINFORCED_DEEPSLATE && p.isSneaking() && p.getGameMode() != GameMode.SPECTATOR) {
                int n = agachado.merge(p.getUniqueId(), 1, Integer::sum);
                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, pe.getLocation().add(0.5, 1.2, 0.5), 40, 0.3, 0.6, 0.3, 0.05);
                if (n >= 2) {
                    agachado.remove(p.getUniqueId());
                    sair(p);
                }
            } else {
                agachado.remove(p.getUniqueId());
            }
        }
        for (Dimensao d : Dimensao.values()) {
            World w = mundo(d);
            if (w == null) continue;
            for (Player p : w.getPlayers()) {
                Location r = rasgoDe(p, w);
                if (r != null && r.distanceSquared(p.getLocation()) < 32 * 32)
                    w.spawnParticle(Particle.PORTAL, r.clone().add(0.5, 1.2, 0.5), 12, 0.3, 0.5, 0.3, 0.3);
            }
        }
    }

    /** Admin: o resumo. */
    public String info() {
        StringBuilder sb = new StringBuilder();
        for (Dimensao d : Dimensao.values()) {
            World w = mundo(d);
            sb.append(d.nome).append(": ").append(w == null ? "não carregou (reinicie)" : w.getName() + ", " + w.getPlayers().size() + " jogador(es), borda " + (int) w.getWorldBorder().getSize())
                    .append(ativa(d) ? "" : " (entrada fechada)").append(". ");
        }
        return sb.toString().trim();
    }

    /** Usado pelo mapa e pelas mensagens: o nome da camada onde o jogador está, ou null. */
    public String camada(Player p) {
        return camadaAtual.get(p.getUniqueId());
    }
}
