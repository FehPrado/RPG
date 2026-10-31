package br.rpgatributos.dimensao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.visual.Telas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.5: o Foguete dos Antigos. Junta as cinco peças do Abismo (Planta, Casco, Núcleo, Tanque e Motor)
 * na bancada e sai o foguete; clicado no chão, ele fica de pé (blocos de cobre, 3 x 3 e 10 de altura).
 * <ul>
 *   <li>Clicando nele: <b>Lançar</b> (precisa de céu aberto em cima), com contagem, fumaça e a subida
 *       (o foguete vira blocos voando e some no céu), e você pousa na lua do céu daquela hora.</li>
 *   <li>Na lua, o foguete fica pousado ao seu lado; clicando nele: <b>Voltar para casa</b> (ele volta
 *       para a plataforma de onde saiu).</li>
 *   <li>Ninguém quebra; o dono recolhe pelo menu.</li>
 * </ul>
 * 3.6.1: cada jogador pode ter quantos foguetes quiser (quem morre na lua e deixa o foguete lá faz
 * outro e vai buscar). Quem está a até 6 blocos vai junto na viagem (tripulação). Se a plataforma de
 * casa sumiu (o mundo de testes apagado, por exemplo) ou foi ocupada, o foguete pousa no lugar livre
 * mais perto, ou no mundo principal; nunca fica parado sem avisar e nunca pousa por cima de construção.
 * O foguete também pode ser recolhido na lua.
 */
public final class Foguetes implements Listener {

    public static final TextColor COR = TextColor.color(0xF2A36A);
    private static final NamespacedKey K_FOGUETE = new NamespacedKey("rpgatributos", "foguete_dos_antigos");
    /** Até onde a tripulação precisa estar do foguete para ir junto. */
    private static final double TRIPULACAO = 6;

    /** Um foguete de pé: de quem é, onde está (o bloco do meio da base) e onde fica a plataforma de casa. */
    private static final class Foguete {
        UUID id;
        UUID dono;
        String mundo;
        int x, y, z;
        /** Null quando a plataforma de casa não existe mais (o mundo foi apagado). */
        String casaMundo;
        int cx, cy, cz;
        boolean voando;

        boolean contem(Block b) {
            return b.getWorld().getName().equals(mundo) && Math.abs(b.getX() - x) <= 1 && Math.abs(b.getZ() - z) <= 1 && b.getY() >= y && b.getY() <= y + 9;
        }

        Location base() {
            World w = Bukkit.getWorld(mundo);
            return w == null ? null : new Location(w, x, y, z);
        }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Foguete> foguetes = new LinkedHashMap<>();

    public Foguetes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "foguetes.yml");
    }

    public static ItemStack item() {
        ItemStack i = new ItemStack(Material.FIREWORK_ROCKET);
        i.editMeta(m -> {
            m.itemName(Component.text("Foguete dos Antigos", COR, TextDecoration.BOLD));
            m.lore(List.of(Component.text("Clique no chão, a céu aberto, para pôr o foguete de pé.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Clique nele para lançar: ele leva à lua do céu daquela hora.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_FOGUETE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "foguete_dos_antigos");
        });
        return i;
    }

    public void registrarReceita() {
        ShapelessRecipe r = new ShapelessRecipe(new NamespacedKey(plugin, "foguete_dos_antigos"), item());
        for (Antigos.Peca pc : Antigos.Peca.values()) r.addIngredient(new RecipeChoice.ExactChoice(pc.item()));
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        foguetes.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection sec = y.getConfigurationSection("foguetes");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(k);
            if (c == null) continue;
            Foguete f = new Foguete();
            try {
                f.id = UUID.fromString(k);
                // Até a 3.6 a chave era o dono (um foguete por jogador).
                f.dono = UUID.fromString(c.getString("dono", k));
            } catch (IllegalArgumentException e) {
                continue;
            }
            f.mundo = c.getString("mundo");
            if (f.mundo == null) continue;
            f.x = c.getInt("x"); f.y = c.getInt("y"); f.z = c.getInt("z");
            f.casaMundo = c.getString("casa-mundo");
            f.cx = c.getInt("casa-x", f.x); f.cy = c.getInt("casa-y", f.y); f.cz = c.getInt("casa-z", f.z);
            foguetes.put(f.id, f);
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Foguete f : foguetes.values()) {
            String b = "foguetes." + f.id + ".";
            y.set(b + "dono", f.dono.toString());
            y.set(b + "mundo", f.mundo); y.set(b + "x", f.x); y.set(b + "y", f.y); y.set(b + "z", f.z);
            y.set(b + "casa-mundo", f.casaMundo); y.set(b + "casa-x", f.cx); y.set(b + "casa-y", f.cy); y.set(b + "casa-z", f.cz);
        }
        try {
            y.save(arquivo);
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui salvar foguetes.yml: " + e.getMessage());
        }
    }

    /**
     * O mundo foi apagado (a área de testes): os foguetes de pé nele somem e os que saíram de lá
     * ficam sem plataforma (voltam para o mundo principal).
     */
    public int esquecerMundo(String nome) {
        int n = 0;
        for (var it = foguetes.values().iterator(); it.hasNext(); ) {
            Foguete f = it.next();
            if (nome.equals(f.mundo)) {
                it.remove();
                n++;
            } else if (nome.equals(f.casaMundo)) f.casaMundo = null;
        }
        salvar();
        return n;
    }

    // =====================================================================
    //  Os blocos do foguete (3 x 3 de base, 10 de altura)
    // =====================================================================

    /** O desenho: deslocamento (dx, dy, dz) → bloco. A base é y 0, o bico y 9. */
    private static Map<int[], BlockData> desenho() {
        Map<int[], BlockData> d = new LinkedHashMap<>();
        BlockData corpo = Bukkit.createBlockData(Material.WAXED_CUT_COPPER), escuro = Bukkit.createBlockData(Material.WAXED_EXPOSED_CUT_COPPER),
                vidro = Bukkit.createBlockData(Material.LIGHT_BLUE_STAINED_GLASS), motor = Bukkit.createBlockData(Material.WAXED_COPPER_GRATE),
                aleta = Bukkit.createBlockData("minecraft:lightning_rod[facing=up]"), bico = Bukkit.createBlockData(Material.WAXED_COPPER_BLOCK);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) d.put(new int[]{dx, 0, dz}, motor);
        for (int[] q : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) d.put(new int[]{q[0], 1, q[1]}, aleta);
        for (int y = 1; y <= 7; y++) {
            d.put(new int[]{0, y, 0}, y == 4 || y == 5 ? vidro : y % 3 == 0 ? escuro : corpo);
            if (y >= 2) {
                d.put(new int[]{-1, y, 0}, y % 3 == 0 ? escuro : corpo);
                d.put(new int[]{1, y, 0}, y % 3 == 0 ? escuro : corpo);
                d.put(new int[]{0, y, -1}, y == 5 ? vidro : corpo);
                d.put(new int[]{0, y, 1}, y == 5 ? vidro : corpo);
            }
        }
        d.put(new int[]{0, 8, 0}, bico);
        d.put(new int[]{0, 9, 0}, aleta);
        return d;
    }

    private static void erguer(Location base) {
        for (Map.Entry<int[], BlockData> e : desenho().entrySet())
            base.clone().add(e.getKey()[0], e.getKey()[1], e.getKey()[2]).getBlock().setBlockData(e.getValue(), false);
    }

    private static void desmontar(Location base) {
        for (int[] k : desenho().keySet()) base.clone().add(k[0], k[1], k[2]).getBlock().setType(Material.AIR, false);
    }

    /** Cabe um foguete aqui sem apagar nada? (todo o desenho em ar ou mato, e chão firme embaixo) */
    private boolean cabe(Location base) {
        if (base.getBlockY() < base.getWorld().getMinHeight() + 1 || base.getBlockY() + 10 > base.getWorld().getMaxHeight()) return false;
        Block chao = base.clone().add(0, -1, 0).getBlock();
        if (!chao.getType().isSolid() || de(chao) != null) return false;
        for (int[] k : desenho().keySet()) {
            Block b = base.clone().add(k[0], k[1], k[2]).getBlock();
            if (!b.getType().isAir() && !b.isReplaceable()) return false;
        }
        return true;
    }

    /**
     * O lugar livre mais perto de (x, z), no chão, até {@code raio} blocos: primeiro o ponto exato
     * (se y for dado), depois em volta, do mais perto para o mais longe.
     */
    private Location lugarPerto(World w, int x, Integer y, int z, int raio) {
        if (y != null) {
            Location exato = new Location(w, x, y, z);
            if (cabe(exato)) return exato;
        }
        List<int[]> pontos = new ArrayList<>();
        for (int dx = -raio; dx <= raio; dx += 2) for (int dz = -raio; dz <= raio; dz += 2) pontos.add(new int[]{dx, dz});
        pontos.sort(Comparator.comparingInt(q -> q[0] * q[0] + q[1] * q[1]));
        for (int[] q : pontos) {
            int px = x + q[0], pz = z + q[1];
            Location l = new Location(w, px, w.getHighestBlockYAt(px, pz, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1, pz);
            if (cabe(l)) return l;
        }
        return null;
    }

    private Foguete de(Block b) {
        for (Foguete f : foguetes.values()) if (!f.voando && f.contem(b)) return f;
        return null;
    }

    /** Quem vai junto: o dono e quem estiver a até 6 blocos do foguete, no mesmo mundo. */
    private static List<Player> tripulacao(Player dono, Location base) {
        List<Player> t = new ArrayList<>();
        t.add(dono);
        Location centro = base.clone().add(0.5, 0, 0.5);
        for (Player o : base.getWorld().getPlayers())
            if (o != dono && !o.isDead() && o.getLocation().distanceSquared(centro) <= TRIPULACAO * TRIPULACAO) t.add(o);
        return t;
    }

    // =====================================================================
    //  Pôr de pé, clicar, proteger
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        Foguete f = de(e.getClickedBlock());
        if (f != null) {
            e.setCancelled(true);
            menu(p, f);
            return;
        }
        ItemStack mao = e.getItem();
        if (mao == null || !mao.getPersistentDataContainer().has(K_FOGUETE)) return;
        e.setCancelled(true);
        Block chao = e.getClickedBlock();
        Location base = chao.getLocation().add(0, 1, 0);
        String erro = lugarLivre(p, base);
        if (erro != null) {
            p.sendActionBar(Component.text(erro, NamedTextColor.RED));
            return;
        }
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        erguer(base);
        Foguete f2 = new Foguete();
        f2.id = UUID.randomUUID();
        f2.dono = p.getUniqueId();
        f2.mundo = base.getWorld().getName();
        f2.x = base.getBlockX(); f2.y = base.getBlockY(); f2.z = base.getBlockZ();
        f2.casaMundo = f2.mundo; f2.cx = f2.x; f2.cy = f2.y; f2.cz = f2.z;
        foguetes.put(f2.id, f2);
        salvar();
        p.playSound(base, Sound.BLOCK_COPPER_PLACE, 1f, 0.7f);
        p.sendMessage(Component.text("🚀 O Foguete dos Antigos está de pé. Clique nele para lançar.", COR));
    }

    private String lugarLivre(Player p, Location base) {
        World w = base.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || Dimensoes.de(w) != null || plugin.masmorras().ehMundo(w)) return "O foguete só sai do mundo normal.";
        if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, base.getBlock())) return "Aqui você não pode construir.";
        for (int[] k : desenho().keySet()) {
            Block b = base.clone().add(k[0], k[1], k[2]).getBlock();
            if (!b.getType().isAir() && !b.isReplaceable()) return "Precisa de um espaço livre de 3 x 3 e 10 de altura.";
        }
        return null;
    }

    private void menu(Player p, Foguete f) {
        Component titulo = Component.text("🚀 Foguete dos Antigos", COR, TextDecoration.BOLD);
        World w = Bukkit.getWorld(f.mundo);
        Dimensoes.Dimensao aqui = Dimensoes.de(w);
        List<Telas.Opcao> op = new ArrayList<>();
        if (!f.dono.equals(p.getUniqueId())) {
            String nome = Bukkit.getOfflinePlayer(f.dono).getName();
            Telas.conversa(p, titulo, item(), List.of(Component.text("Este foguete é de " + (nome != null ? nome : "outra pessoa") + ".", NamedTextColor.GRAY),
                    Component.text("Fique perto dele (até 6 blocos) quando o dono lançar, e você vai junto.", NamedTextColor.GRAY)), List.of());
            return;
        }
        if (aqui != null && aqui.lua()) {
            op.add(Telas.Opcao.de("🏠 Voltar para casa", casaExiste(f) ? "O foguete pousa de novo na plataforma de onde saiu"
                    : "A plataforma de casa não existe mais: ele pousa no mundo principal", q -> voltar(q, f)));
            op.add(Telas.Opcao.de("Recolher o foguete", "Ele vai para a mochila e você fica aqui (volte com a Pedra de Viagem ou outro foguete)", q -> recolher(q, f)));
            Telas.conversa(p, titulo, item(), List.of(Component.text("O motor ainda está quente. A plataforma de casa espera por você.", NamedTextColor.WHITE),
                    Component.text("Quem estiver a até 6 blocos vai junto.", NamedTextColor.GRAY)), op);
            return;
        }
        Dimensoes.Dimensao destino = luaDoCeu(Bukkit.getWorlds().getFirst());
        op.add(Telas.Opcao.de("🚀 Lançar para " + destino.nome(), "Precisa de céu aberto em cima do foguete", q -> lancar(q, f, destino)));
        op.add(Telas.Opcao.de("Recolher o foguete", "Ele volta para a sua mochila", q -> recolher(q, f)));
        Telas.conversa(p, titulo, item(), List.of(
                Component.text("O Núcleo de Navegação aponta para o céu: " + destino.nome() + ".", NamedTextColor.WHITE),
                Component.text("Leve o Capacete Lunar: lá em cima não tem ar. Quem estiver a até 6 blocos vai junto.", NamedTextColor.GRAY)), op);
    }

    private static boolean casaExiste(Foguete f) {
        return f.casaMundo != null && Bukkit.getWorld(f.casaMundo) != null;
    }

    /** O clique veio de um menu antigo? (o foguete já saiu, foi recolhido ou você não está mais perto dele) */
    private boolean valido(Player p, Foguete f) {
        if (f.voando || foguetes.get(f.id) != f || !p.getWorld().getName().equals(f.mundo)) return false;
        Location b = f.base();
        return b != null && p.getLocation().distanceSquared(b.clone().add(0.5, 0, 0.5)) <= 24 * 24;
    }

    /**
     * Qual lua o Núcleo aponta agora (pelo céu do mundo principal): eclipse → Sombria, Lua de Sangue →
     * Vermelha, Lua Azul → de Cristal, noite de lua nova → Escura; no resto, a Prateada.
     */
    public Dimensoes.Dimensao luaDoCeu(World principal) {
        var ceu = plugin.ceu();
        boolean noite = br.rpgatributos.mundo.Ceu.noite(principal);
        if (ceu != null && ceu.eclipse()) return Dimensoes.Dimensao.LUA_SOMBRIA;
        if (ceu != null && ceu.luaDeSangue()) return Dimensoes.Dimensao.LUA_VERMELHA;
        if (plugin.rituaisMundo() != null && plugin.rituaisMundo().luaAzul(principal)) return Dimensoes.Dimensao.LUA_CRISTAL;
        if (noite && br.rpgatributos.mundo.Ceu.fase(principal) == 4) return Dimensoes.Dimensao.LUA_ESCURA;
        return Dimensoes.Dimensao.LUA_PRATEADA;
    }

    private void recolher(Player p, Foguete f) {
        if (!valido(p, f)) return;
        Location b = f.base();
        if (b != null) desmontar(b);
        foguetes.remove(f.id);
        salvar();
        p.getInventory().addItem(item()).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.sendActionBar(Component.text("Foguete recolhido.", COR));
    }

    /** Admin: desmonta todos os foguetes de alguém (onde estiverem) e entrega os itens. */
    public int recolherTodos(UUID dono, Player recebe) {
        int n = 0;
        for (var it = foguetes.values().iterator(); it.hasNext(); ) {
            Foguete f = it.next();
            if (!f.dono.equals(dono)) continue;
            Location b = f.base();
            if (b != null) desmontar(b);
            it.remove();
            n++;
            if (recebe != null) recebe.getInventory().addItem(item()).values().forEach(s -> recebe.getWorld().dropItem(recebe.getLocation(), s));
        }
        if (n > 0) salvar();
        return n;
    }

    /** Admin: onde estão os foguetes de alguém. */
    public List<String> onde(UUID dono) {
        List<String> l = new ArrayList<>();
        for (Foguete f : foguetes.values()) {
            if (!f.dono.equals(dono)) continue;
            Dimensoes.Dimensao d = Dimensoes.de(Bukkit.getWorld(f.mundo));
            l.add((d != null ? d.nome() : f.mundo) + " " + f.x + " " + f.y + " " + f.z + (f.voando ? " (voando)" : ""));
        }
        return l;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (de(e.getBlock()) != null) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("O foguete não quebra. O dono recolhe pelo menu (clique nele).", NamedTextColor.GRAY));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> de(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> de(b) != null);
    }

    /** Morreu numa lua com o foguete lá: avisa onde ele ficou (ele espera; dá para buscar com outro foguete). */
    @EventHandler
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        Dimensoes.Dimensao d = Dimensoes.de(p.getWorld());
        if (d == null || !d.lua()) return;
        for (Foguete f : foguetes.values()) {
            if (!f.dono.equals(p.getUniqueId()) || !f.mundo.equals(p.getWorld().getName())) continue;
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.sendMessage(Component.text("🚀 Seu foguete ficou pousado n" + d.nome().replaceFirst("^A ", "a ")
                    + " (" + f.x + ", " + f.y + ", " + f.z + "). Ele espera por você: faça outro foguete e vá buscá-lo.", COR)), 40L);
            return;
        }
    }

    // =====================================================================
    //  Lançar e voltar
    // =====================================================================

    private void lancar(Player p, Foguete f, Dimensoes.Dimensao destino) {
        if (!valido(p, f)) return;
        World w = Bukkit.getWorld(f.mundo);
        World lua = plugin.dimensoes().mundo(destino);
        if (lua == null || !plugin.dimensoes().ativa(destino)) {
            p.sendMessage(Component.text(destino.nome() + " ainda não está ao alcance (o servidor precisa reiniciar, ou a viagem está fechada).", NamedTextColor.RED));
            return;
        }
        Location base = new Location(w, f.x, f.y, f.z);
        if (w.getHighestBlockYAt(f.x, f.z, HeightMap.MOTION_BLOCKING) > f.y + 9) {
            p.sendMessage(Component.text("Tem alguma coisa em cima do foguete. Ele precisa de céu aberto.", NamedTextColor.RED));
            return;
        }
        f.voando = true;
        // Contagem regressiva.
        for (int i = 0; i < 5; i++) {
            final int n = 5 - i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player t : tripulacao(p, base))
                    t.showTitle(Title.title(Component.text(String.valueOf(n), COR, TextDecoration.BOLD), Component.text("Prepare-se para o lançamento", NamedTextColor.GRAY),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, base.clone().add(0.5, 0.2, 0.5), 20, 1.2, 0.2, 1.2, 0.02, null, true);
                w.playSound(base, Sound.BLOCK_NOTE_BLOCK_BIT, 1.5f, 0.5f + n * 0.15f);
            }, i * 20L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> decolar(p, f, base, lua, destino), 100L);
    }

    private void decolar(Player p, Foguete f, Location base, World lua, Dimensoes.Dimensao destino) {
        World w = base.getWorld();
        List<Player> tripulacao = p.isOnline() ? tripulacao(p, base) : List.of();
        // O foguete vira blocos voando (só durante a subida) e sobe 70 blocos em 4 segundos.
        List<BlockDisplay> pecas = new ArrayList<>();
        for (Map.Entry<int[], BlockData> e : desenho().entrySet()) {
            Location l = base.clone().add(e.getKey()[0], e.getKey()[1], e.getKey()[2]);
            BlockDisplay d = w.spawn(l, BlockDisplay.class, x -> {
                x.setBlock(e.getValue());
                x.setPersistent(false);
            });
            pecas.add(d);
        }
        desmontar(base);
        w.playSound(base, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        w.playSound(base, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (BlockDisplay d : pecas) {
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(80);
                d.setTransformation(new Transformation(new Vector3f(0, 70, 0), new AxisAngle4f(), new Vector3f(1, 1, 1), new AxisAngle4f()));
            }
        }, 2L);
        for (Player t : tripulacao)
            if (t.getWorld() == w && t.getLocation().distanceSquared(base) < 12 * 12) t.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 70, 14, false, false, false));
        for (int t = 0; t < 80; t += 2) {
            final int tt = t;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location fogo = base.clone().add(0.5, tt * 70.0 / 80, 0.5);
                w.spawnParticle(Particle.FLAME, fogo, 25, 0.5, 0.3, 0.5, 0.05, null, true);
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, fogo, 8, 0.6, 0.3, 0.6, 0.01, null, true);
            }, t);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pecas.forEach(org.bukkit.entity.Entity::remove);
            pousar(p, tripulacao, f, lua, destino);
        }, 82L);
    }

    /** Pousa na lua: o foguete fica de pé num lugar firme e a tripulação ao lado dele. */
    private void pousar(Player p, List<Player> tripulacao, Foguete f, World lua, Dimensoes.Dimensao destino) {
        Location l = p.isOnline() ? chaoNaLua(lua) : null;
        if (l == null) {
            // Sem lugar (ou o dono saiu do jogo no meio): o foguete volta para a plataforma sem sair.
            Location pad = new Location(Bukkit.getWorld(f.mundo), f.x, f.y, f.z);
            Location livre = lugarPerto(pad.getWorld(), f.x, f.y, f.z, 16);
            if (livre != null) {
                erguer(livre);
                f.x = livre.getBlockX(); f.y = livre.getBlockY(); f.z = livre.getBlockZ();
            }
            f.voando = false;
            if (livre == null) foguetes.remove(f.id);
            salvar();
            if (p.isOnline()) {
                if (livre == null) p.getInventory().addItem(item()).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
                p.sendMessage(Component.text("O Núcleo não achou onde pousar. O foguete voltou " + (livre != null ? "para a plataforma." : "para a sua mochila."), NamedTextColor.RED));
            }
            return;
        }
        erguer(l);
        f.mundo = lua.getName();
        f.x = l.getBlockX(); f.y = l.getBlockY(); f.z = l.getBlockZ();
        f.voando = false;
        salvar();
        int i = 0;
        for (Player t : tripulacao) {
            if (!t.isOnline() || t.isDead()) continue;
            plugin.dimensoes().guardarVolta(t);
            Location chegada = l.clone().add(2.5 + (i % 2) * 1.0, 0, 0.5 + (i / 2) * 1.0 - (i > 0 ? 1 : 0));
            i++;
            t.setFallDistance(0);
            t.teleport(chegada);
            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false, false));
            t.showTitle(Title.title(Component.text(destino.nome(), destino.cor(), TextDecoration.BOLD), Component.text(destino.frase(), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))));
            t.playSound(chegada, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
            plugin.diario().marco(t, "pousou_" + destino.id(), "Pousou n" + destino.nome().replaceFirst("^A ", "a "));
            plugin.titulos().registrar(t, "pousos_lua", 1);
            // 3.6: cada lua nova conta para o Viajante das Luas.
            NamespacedKey kVisitas = new NamespacedKey(plugin, "luas_visitadas");
            String vistas = t.getPersistentDataContainer().getOrDefault(kVisitas, PersistentDataType.STRING, "");
            if (!List.of(vistas.split(",")).contains(destino.id())) {
                t.getPersistentDataContainer().set(kVisitas, PersistentDataType.STRING, vistas.isEmpty() ? destino.id() : vistas + "," + destino.id());
                plugin.titulos().registrar(t, "luas_visitadas", 1);
            }
            if (destino == Dimensoes.Dimensao.LUA_SOMBRIA) t.sendMessage(Component.text("✦ A Lua Sombria fecha em 10 minutos. Quem ficar volta à força.", destino.cor()));
        }
    }

    private Location chaoNaLua(World lua) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(-1200, 1201), z = r.nextInt(-1200, 1201);
            int y = lua.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING);
            if (y < 40) continue;
            boolean plano = true;
            for (int dx = -2; dx <= 2 && plano; dx++) for (int dz = -2; dz <= 2 && plano; dz++)
                if (Math.abs(lua.getHighestBlockYAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING) - y) > 1) plano = false;
            Location l = new Location(lua, x, y + 1, z);
            if (plano && cabe(l)) return l;
        }
        return null;
    }

    /**
     * Onde o foguete pousa ao voltar: a plataforma de casa se ela continua livre; senão o lugar livre
     * mais perto dela; sem o mundo de casa, perto de onde você estava antes de subir (ou do começo do
     * mundo principal).
     */
    private Location destinoDeCasa(Player p, Foguete f) {
        World casa = f.casaMundo == null ? null : Bukkit.getWorld(f.casaMundo);
        if (casa != null) {
            Location l = lugarPerto(casa, f.cx, f.cy, f.cz, 24);
            if (l != null) return l;
        }
        Location v = plugin.dimensoes().volta(p);
        if (v == null || Dimensoes.de(v.getWorld()) != null) v = Bukkit.getWorlds().getFirst().getSpawnLocation();
        return lugarPerto(v.getWorld(), v.getBlockX(), null, v.getBlockZ(), 32);
    }

    private void voltar(Player p, Foguete f) {
        if (!valido(p, f)) return;
        World aqui = Bukkit.getWorld(f.mundo);
        Location l = new Location(aqui, f.x, f.y, f.z);
        Location pad = destinoDeCasa(p, f);
        if (pad == null) {
            p.sendMessage(Component.text("O Núcleo não achou um lugar livre em casa para pousar. Tente de novo, ou recolha o foguete e use a Pedra de Viagem.", NamedTextColor.RED));
            return;
        }
        List<Player> tripulacao = tripulacao(p, l);
        desmontar(l);
        aqui.spawnParticle(Particle.FLAME, l.clone().add(0.5, 0.5, 0.5), 80, 1, 1, 1, 0.1);
        aqui.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        erguer(pad);
        f.mundo = pad.getWorld().getName(); f.x = pad.getBlockX(); f.y = pad.getBlockY(); f.z = pad.getBlockZ();
        // A plataforma passa a ser onde ele pousou (se a antiga sumiu ou estava ocupada).
        f.casaMundo = f.mundo; f.cx = f.x; f.cy = f.y; f.cz = f.z;
        salvar();
        int i = 0;
        for (Player t : tripulacao) {
            Location chegada = pad.clone().add(2.5 + (i % 2) * 1.0, 0, 0.5 + (i / 2) * 1.0 - (i > 0 ? 1 : 0));
            i++;
            t.setFallDistance(0);
            t.teleport(chegada);
            t.playSound(chegada, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
            t.showTitle(Title.title(Component.text("🏠 De volta", COR, TextDecoration.BOLD), Component.text("O foguete pousou na plataforma.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2000), Duration.ofMillis(500))));
        }
    }

    /** 3.6: a Lua Sombria fechou: o foguete leva você de volta (ou, sem foguete lá, você volta para onde estava). */
    public void forcarVolta(Player p) {
        for (Foguete f : List.copyOf(foguetes.values())) {
            if (!f.dono.equals(p.getUniqueId()) || f.voando || !f.mundo.equals(p.getWorld().getName())) continue;
            Location pad = destinoDeCasa(p, f);
            if (pad == null) break;
            // Arranca sozinho: não importa onde você está na lua.
            Location l = f.base();
            desmontar(l);
            erguer(pad);
            f.mundo = pad.getWorld().getName(); f.x = pad.getBlockX(); f.y = pad.getBlockY(); f.z = pad.getBlockZ();
            f.casaMundo = f.mundo; f.cx = f.x; f.cy = f.y; f.cz = f.z;
            salvar();
            p.setFallDistance(0);
            p.teleport(pad.clone().add(2.5, 0, 0.5));
            return;
        }
        Location v = plugin.dimensoes().volta(p);
        if (v == null || Dimensoes.de(v.getWorld()) != null) v = Bukkit.getWorlds().getFirst().getSpawnLocation();
        p.setFallDistance(0);
        p.teleport(v);
    }

    /** Admin: um foguete pronto na mão. */
    public static ItemStack itemAdmin() {
        return item();
    }
}
