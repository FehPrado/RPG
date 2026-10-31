package br.rpgatributos.dimensao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.visual.Telas;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Lectern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vex;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.8: o Mundo das Almas por dentro (parte 2). As regras de entrar e sair ficam em {@link Almas}.
 * <ul>
 *   <li><b>Cais do Barqueiro:</b> toda alma chega num cais (o mais perto, ou um novo). O Barqueiro
 *       atravessa o Rio do Esquecimento até a Cidade dos Antigos por 3 Almas Penadas.</li>
 *   <li><b>Almas Penadas:</b> vagam pelas ilhas; clique nelas com a Lanterna das Almas para recolher.</li>
 *   <li><b>Criaturas:</b> Sombras Errantes (voam e atravessam paredes) e Penitentes. Deixam Essência Etérea.</li>
 *   <li><b>A Cidade dos Antigos:</b> uma ilha só (construída pelo plugin na primeira travessia), com quatro
 *       casas da memória (Diário XI a XIV e um baú cada), o Tribunal com o sino e a Ilen.</li>
 *   <li><b>O Juiz:</b> tocar o sino do Tribunal acorda o chefe. Vencido, a Ilen entrega o último capítulo (XV).</li>
 * </ul>
 * A alma chega sem nada: os baús da cidade têm a Lâmina e o Manto Espectral, feitos para lutar aqui.
 */
public final class MundoDasAlmas implements Listener {

    private static final TextColor COR = Almas.COR;
    private static final TextColor JUIZ = TextColor.color(0xC9B458);
    static final NamespacedKey K_CRIATURA = new NamespacedKey("rpgatributos", "criatura_das_almas");
    private static final NamespacedKey K_PENADA = new NamespacedKey("rpgatributos", "alma_penada_viva");
    private static final NamespacedKey K_BARQUEIRO = new NamespacedKey("rpgatributos", "barqueiro");
    private static final NamespacedKey K_ILEN = new NamespacedKey("rpgatributos", "ilen");
    private static final NamespacedKey K_JUIZ = new NamespacedKey("rpgatributos", "juiz");
    private static final NamespacedKey K_CAPITULO = new NamespacedKey("rpgatributos", "diario_almas_capitulo");
    private static final NamespacedKey K_LIDOS = new NamespacedKey("rpgatributos", "diario_almas_lidos");
    private static final NamespacedKey K_VENCEU_JUIZ = new NamespacedKey("rpgatributos", "venceu_juiz");
    private static final NamespacedKey K_ESPECTRAL = new NamespacedKey("rpgatributos", "arma_espectral");

    /** A Cidade dos Antigos: sempre no mesmo lugar, dentro da borda. */
    private static final int CX = 1200, CY = 125, CZ = -1200, RAIO = 34;
    private static final int PRECO = 3;

    private final RPGAtributos plugin;
    private final File arquivo;
    private final List<int[]> cais = new ArrayList<>();
    private boolean cidade;
    private boolean construindo;
    private final Map<String, Long> baus = new HashMap<>();
    private long juizVolta;
    private UUID juiz;
    private BossBar barraJuiz;
    private boolean juizChamou;
    private int ciclo;

    public MundoDasAlmas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "mundo_almas.yml");
    }

    private World mundo() {
        return plugin.dimensoes().mundo(Dimensoes.Dimensao.ALMAS);
    }

    private static boolean aqui(World w) {
        return Dimensoes.de(w) == Dimensoes.Dimensao.ALMAS;
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String s : y.getStringList("cais")) {
            String[] p = s.split(",");
            if (p.length == 3) cais.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])});
        }
        cidade = y.getBoolean("cidade");
        juizVolta = y.getLong("juiz-volta");
        ConfigurationSection b = y.getConfigurationSection("baus");
        if (b != null) for (String k : b.getKeys(false)) baus.put(k.replace('_', ','), b.getLong(k));
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (int[] c : cais) l.add(c[0] + "," + c[1] + "," + c[2]);
        y.set("cais", l);
        y.set("cidade", cidade);
        y.set("juiz-volta", juizVolta);
        for (Map.Entry<String, Long> e : baus.entrySet()) y.set("baus." + e.getKey().replace(',', '_'), e.getValue());
        try {
            y.save(arquivo);
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui salvar mundo_almas.yml: " + e.getMessage());
        }
    }

    // =====================================================================
    //  Os itens
    // =====================================================================

    public static ItemStack laminaEspectral() {
        ItemStack i = new ItemStack(Material.IRON_SWORD);
        i.editMeta(m -> {
            m.itemName(Component.text("Lâmina Espectral", COR));
            m.lore(List.of(Component.text("Corta o que já morreu: +80% de dano nas criaturas", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("do Mundo das Almas e no Juiz.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_ESPECTRAL, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "lamina_espectral");
        });
        return i;
    }

    public static ItemStack mantoEspectral() {
        ItemStack i = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        i.editMeta(m -> {
            m.itemName(Component.text("Manto Espectral", COR));
            m.lore(List.of(Component.text("Tecido de névoa dos Antigos.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_ESPECTRAL, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "manto_espectral");
        });
        return i;
    }

    // =====================================================================
    //  O Diário dos Antigos, XI a XV
    // =====================================================================

    private static final String[][] DIARIO = {
            {"XI. A Margem",
                    "Cheguei. Não sei dizer se morri: o corpo ficou lá em cima, deitado, e eu estou aqui, leve como fumaça.",
                    "Na margem há um barqueiro. Ele não fala o meu nome, mas sabe quem eu sou. Para atravessar, ele pede almas. Não a minha: as que vagam perdidas.",
                    "Juntei três e ele me levou. Do outro lado do rio está a nossa cidade. Nós a construímos sem saber. — Ilen"},
            {"XII. A Cidade",
                    "Todos os Antigos que caíram, que voaram, que se perderam estão aqui. Fizemos casas de pedra cinza para guardar as lembranças.",
                    "Cada casa guarda uma parte de quem fomos: o cobre, o cristal, o foguete, as luas. Escrevo um capítulo em cada uma.",
                    "Mas ninguém sai. Há um tribunal no meio da cidade, e no tribunal há um sino. — Ilen"},
            {"XIII. O Juiz",
                    "O Juiz chegou antes de nós. Ele pesa cada alma numa balança que ninguém vê. As nossas pesaram demais.",
                    "Cavamos fundo demais, voamos alto demais. Ele diz que quem fugiu do mundo não pode voltar a ele.",
                    "Quem toca o sino é julgado na hora. Nenhum de nós teve coragem. — Ilen"},
            {"XIV. A Lanterna",
                    "Descobri por que as lanternas azuis funcionam: a areia das almas guarda um pedaço de quem passou por ela. A chama é esse pedaço.",
                    "Por isso a chama apaga. Por isso quem vem de lá de cima com uma lanterna pode voltar, e nós não: as nossas já apagaram.",
                    "Se você tem uma lanterna acesa, você ainda pertence ao mundo. Toque o sino. — Ilen"},
            {"XV. A Travessia",
                    "O Juiz caiu. A balança quebrou e, pela primeira vez, o Barqueiro sorriu.",
                    "Os Antigos estão atravessando o rio, um de cada vez, para onde quer que as almas devam ir. Eu vou por último: alguém precisa contar a história.",
                    "Obrigada por ter lembrado de nós. O cobre, o cristal, as luas: tudo o que fizemos agora é seu. Use melhor do que nós. — Ilen"}};

    private static ItemStack capitulo(int n) {
        ItemStack b = Luas.diario(DIARIO[n]);
        b.editMeta(m -> m.getPersistentDataContainer().set(K_CAPITULO, PersistentDataType.INTEGER, n));
        return b;
    }

    /** Leu um capítulo novo (abrindo o atril ou recebendo da Ilen): conta para o Leitor do Fim. */
    private void leu(Player p, int n) {
        String lidos = p.getPersistentDataContainer().getOrDefault(K_LIDOS, PersistentDataType.STRING, "");
        String id = String.valueOf(n);
        if (List.of(lidos.split(",")).contains(id)) return;
        p.getPersistentDataContainer().set(K_LIDOS, PersistentDataType.STRING, lidos.isEmpty() ? id : lidos + "," + id);
        plugin.titulos().registrar(p, "diario_fim", 1);
        p.sendActionBar(Component.text("📖 Diário dos Antigos " + DIARIO[n][0], Antigos.COR));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoLerAtril(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || !aqui(e.getClickedBlock().getWorld())) return;
        Block b = e.getClickedBlock();
        if (b.getType() == Material.LECTERN && b.getState() instanceof Lectern l) {
            ItemStack livro = l.getInventory().getItem(0);
            Integer n = livro == null ? null : livro.getPersistentDataContainer().get(K_CAPITULO, PersistentDataType.INTEGER);
            if (n != null) leu(e.getPlayer(), n);
        } else if (b.getType() == Material.BELL && perto(b.getLocation(), CX, CZ, RAIO)) {
            tocarSino(e.getPlayer(), b);
        }
    }

    /** Tirar o livro do atril dá uma cópia (o atril continua com o dele para quem vier depois). */
    @EventHandler(ignoreCancelled = true)
    public void aoTirarLivro(PlayerTakeLecternBookEvent e) {
        if (!aqui(e.getLectern().getWorld())) return;
        ItemStack livro = e.getBook();
        if (livro == null || !livro.getPersistentDataContainer().has(K_CAPITULO)) return;
        e.setCancelled(true);
        e.getPlayer().closeInventory();
        e.getPlayer().getInventory().addItem(livro.clone()).values().forEach(s -> e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), s));
    }

    // =====================================================================
    //  Os cais e o Barqueiro
    // =====================================================================

    /**
     * Onde a alma chega: no cais mais perto (até 300 blocos) ou num cais novo, construído ali.
     * Chamado pelo ritual (Almas).
     */
    public Location chegadaNoCais(Location chegada) {
        World w = chegada.getWorld();
        int[] melhor = null;
        double d = 300 * 300;
        for (int[] c : cais) {
            double dd = Math.pow(c[0] - chegada.getX(), 2) + Math.pow(c[2] - chegada.getZ(), 2);
            if (dd < d) { d = dd; melhor = c; }
        }
        if (melhor != null) return new Location(w, melhor[0] + 0.5, melhor[1] + 1, melhor[2] - 2.5);
        Block chao = chegada.clone().add(0, -1, 0).getBlock();
        construirCais(chao);
        cais.add(new int[]{chao.getX(), chao.getY(), chao.getZ()});
        salvar();
        return new Location(w, chao.getX() + 0.5, chao.getY() + 1, chao.getZ() - 2.5);
    }

    /** Um píer de madeira de abeto (5 x 9) com postes e lanternas azuis; o Barqueiro na ponta. */
    private void construirCais(Block c) {
        World w = c.getWorld();
        int x = c.getX(), y = c.getY(), z = c.getZ();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -4; dz <= 4; dz++) {
            w.getBlockAt(x + dx, y, z + dz).setType(Material.SPRUCE_PLANKS, false);
            for (int dy = 1; dy <= 4; dy++) w.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR, false);
            // Escoras até o chão (onde o píer passa do barranco).
            for (int dy = 1; dy <= 8; dy++) {
                Block b = w.getBlockAt(x + dx, y - dy, z + dz);
                if (!b.getType().isAir() && !b.isLiquid()) break;
                if (Math.abs(dx) == 2 && Math.abs(dz) % 4 == 0) b.setType(Material.STRIPPED_SPRUCE_LOG, false);
                else break;
            }
        }
        for (int dx : new int[]{-2, 2}) for (int dz : new int[]{-4, 0, 4}) {
            w.getBlockAt(x + dx, y + 1, z + dz).setType(Material.SPRUCE_FENCE);
            w.getBlockAt(x + dx, y + 2, z + dz).setType(Material.SOUL_LANTERN, false);
        }
        barqueiro(new Location(w, x + 0.5, y + 1, z + 3.5, 180, 0), false);
    }

    private void barqueiro(Location l, boolean daCidade) {
        l.getWorld().spawn(l, WanderingTrader.class, t -> {
            t.customName(Component.text("O Barqueiro", COR, TextDecoration.BOLD));
            t.setCustomNameVisible(true);
            t.setAI(false);
            t.setInvulnerable(true);
            t.setSilent(true);
            t.setPersistent(true);
            t.setRemoveWhenFarAway(false);
            t.setDespawnDelay(0);
            t.getPersistentDataContainer().set(K_BARQUEIRO, PersistentDataType.BYTE, (byte) (daCidade ? 1 : 0));
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFalar(PlayerInteractEntityEvent e) {
        Entity en = e.getRightClicked();
        Player p = e.getPlayer();
        var pdc = en.getPersistentDataContainer();
        if (pdc.has(K_BARQUEIRO)) {
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND) falarBarqueiro(p, pdc.getOrDefault(K_BARQUEIRO, PersistentDataType.BYTE, (byte) 0) == 1);
        } else if (pdc.has(K_ILEN)) {
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND) falarIlen(p);
        } else if (pdc.has(K_PENADA)) {
            e.setCancelled(true);
            if (e.getHand() != EquipmentSlot.HAND) return;
            if (!Almas.ehLanterna(p.getInventory().getItemInMainHand()) && !Almas.ehLanterna(p.getInventory().getItemInOffHand())) {
                p.sendActionBar(Component.text("Só a Lanterna das Almas recolhe uma alma penada.", NamedTextColor.GRAY));
                return;
            }
            recolher(p, en);
        }
    }

    private void falarBarqueiro(Player p, boolean daCidade) {
        Component titulo = Component.text("🛶 O Barqueiro", COR, TextDecoration.BOLD);
        List<Telas.Opcao> op = new ArrayList<>();
        if (daCidade) {
            op.add(Telas.Opcao.de("Voltar à Margem", "Para o cais mais perto de onde você chegou", q -> {
                if (!aqui(q.getWorld()) || cais.isEmpty()) return;
                int[] c = cais.get(ThreadLocalRandom.current().nextInt(cais.size()));
                atravessar(q, new Location(q.getWorld(), c[0] + 0.5, c[1] + 1, c[2] - 2.5), "A Margem");
            }));
            Telas.conversa(p, titulo, Almas.lanterna(), List.of(Component.text("\"De volta? A margem não vai a lugar nenhum.\"", NamedTextColor.WHITE)), op);
            return;
        }
        int tem = contar(p, Raro.ALMA_PENADA);
        op.add(Telas.Opcao.de("Atravessar até a Cidade dos Antigos (" + PRECO + " Almas Penadas)", "Você tem " + tem, q -> {
            if (!aqui(q.getWorld())) return;
            if (contar(q, Raro.ALMA_PENADA) < PRECO) {
                q.sendMessage(Component.text("\"Três almas. Nem uma a menos.\" Recolha Almas Penadas com a lanterna.", COR));
                return;
            }
            tirar(q, Raro.ALMA_PENADA, PRECO);
            irParaCidade(q);
        }));
        op.add(Telas.Opcao.de("Quem é você?", null, q -> Telas.conversa(q, titulo, Almas.lanterna(), List.of(
                Component.text("\"Eu levo quem pode ir. Do outro lado do Rio do Esquecimento fica a cidade dos que vieram antes de você.\"", NamedTextColor.WHITE),
                Component.text("\"Não cobro a sua alma. Cobro as que vagam perdidas. Três por viagem.\"", NamedTextColor.WHITE),
                Component.text("\"E a sua chama está queimando. Não demore.\"", NamedTextColor.GRAY)), List.of())));
        Telas.conversa(p, titulo, Almas.lanterna(), List.of(
                Component.text("\"Uma lanterna acesa. Faz tempo que não vejo uma.\"", NamedTextColor.WHITE),
                Component.text("Almas Penadas: " + tem + " de " + PRECO + ".", NamedTextColor.GRAY)), op);
    }

    private void atravessar(Player p, Location l, String onde) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
        p.playSound(p.getLocation(), Sound.ENTITY_BOAT_PADDLE_WATER, 1f, 0.6f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || !aqui(p.getWorld())) return;
            p.setFallDistance(0);
            p.teleport(l);
            p.showTitle(Title.title(Component.text(onde, COR, TextDecoration.BOLD), Component.text("O barco some na névoa.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(800))));
        }, 30L);
    }

    private void irParaCidade(Player p) {
        World w = p.getWorld();
        Location chegada = new Location(w, CX + 0.5, CY + 1, CZ - RAIO + 6.5, 0, 0);
        if (cidade) {
            atravessar(p, chegada, "A Cidade dos Antigos");
            return;
        }
        if (construindo) {
            p.sendMessage(Component.text("\"A névoa ainda está abrindo. Mais um instante.\"", COR));
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline() && cidade) atravessar(p, chegada, "A Cidade dos Antigos"); }, 100L);
            return;
        }
        construindo = true;
        p.sendMessage(Component.text("\"Segure firme. A cidade fica longe.\"", COR));
        List<CompletableFuture<Chunk>> f = new ArrayList<>();
        int c0x = (CX - RAIO - 8) >> 4, c1x = (CX + RAIO + 8) >> 4, c0z = (CZ - RAIO - 8) >> 4, c1z = (CZ + RAIO + 8) >> 4;
        for (int cx = c0x; cx <= c1x; cx++) for (int cz = c0z; cz <= c1z; cz++) f.add(w.getChunkAtAsync(cx, cz, true));
        CompletableFuture.allOf(f.toArray(new CompletableFuture[0])).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                construirCidade(w);
                cidade = true;
                salvar();
            } finally {
                construindo = false;
            }
            if (p.isOnline() && aqui(p.getWorld())) atravessar(p, chegada, "A Cidade dos Antigos");
        }));
    }

    // =====================================================================
    //  A Cidade dos Antigos
    // =====================================================================

    /** A ilha da cidade, o Tribunal, as quatro casas, a Ilen e o cais da cidade. */
    public void construirCidade(World w) {
        // A ilha: um prato de tufo afinando para baixo, com a praça de calcita em cima.
        for (int dx = -RAIO - 2; dx <= RAIO + 2; dx++) for (int dz = -RAIO - 2; dz <= RAIO + 2; dz++) {
            double d = Math.hypot(dx, dz);
            int x = CX + dx, z = CZ + dz;
            for (int y = CY + 1; y <= CY + 30; y++) w.getBlockAt(x, y, z).setType(Material.AIR, false);
            if (d > RAIO + 0.5) continue;
            int fundo = (int) ((RAIO - d) * 0.8) + 3;
            for (int y = CY - fundo; y < CY; y++) w.getBlockAt(x, y, z).setType(y < CY - 2 ? Material.TUFF : Material.POLISHED_TUFF, false);
            Material topo = d > RAIO - 1.5 ? Material.TUFF_BRICKS : ((int) d) % 6 == 0 ? Material.POLISHED_TUFF : Material.CALCITE;
            w.getBlockAt(x, CY, z).setType(topo, false);
            if (d > RAIO - 0.8 && (dx * 7 + dz * 3) % 5 != 0) w.getBlockAt(x, CY + 1, z).setType(Material.TUFF_BRICK_WALL);
        }
        // O Tribunal: um estrado redondo, o trono, o sino e fogueiras de alma em volta.
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            double d = Math.hypot(dx, dz);
            if (d <= 7.5) w.getBlockAt(CX + dx, CY + 1, CZ + dz).setType(d > 6.5 ? Material.POLISHED_DEEPSLATE : Material.CHISELED_TUFF_BRICKS, false);
        }
        w.getBlockAt(CX, CY + 2, CZ + 5).setBlockData(Bukkit.createBlockData("minecraft:quartz_stairs[facing=south]"), false);
        w.getBlockAt(CX - 1, CY + 2, CZ + 5).setType(Material.QUARTZ_PILLAR, false);
        w.getBlockAt(CX + 1, CY + 2, CZ + 5).setType(Material.QUARTZ_PILLAR, false);
        w.getBlockAt(CX, CY + 2, CZ).setBlockData(Bukkit.createBlockData("minecraft:bell[attachment=floor,facing=north]"), false);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int x = CX + (int) Math.round(Math.cos(a) * 10), z = CZ + (int) Math.round(Math.sin(a) * 10);
            w.getBlockAt(x, CY + 1, z).setType(Material.SOUL_CAMPFIRE);
        }
        // As quatro casas da memória (XI a XIV), nas diagonais, com a porta virada para o Tribunal.
        int[][] casas = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        for (int i = 0; i < 4; i++) casa(w, CX + casas[i][0] * 18, CZ + casas[i][1] * 18, casas[i][0], casas[i][1], i);
        // A Ilen, perto do trono; o Barqueiro no cais da cidade (do lado da chegada).
        ilen(new Location(w, CX + 3.5, CY + 2, CZ + 6.5, 180, 0));
        for (int dx = -1; dx <= 1; dx++) for (int dz = -RAIO - 6; dz <= -RAIO; dz++) w.getBlockAt(CX + dx, CY, CZ + dz).setType(Material.SPRUCE_PLANKS, false);
        for (int dz = -RAIO - 6; dz <= -RAIO + 6; dz++) w.getBlockAt(CX, CY + 1, CZ + dz).setType(Material.AIR, false);
        w.getBlockAt(CX - 1, CY + 1, CZ - RAIO - 6).setType(Material.SPRUCE_FENCE);
        w.getBlockAt(CX - 1, CY + 2, CZ - RAIO - 6).setType(Material.SOUL_LANTERN, false);
        barqueiro(new Location(w, CX + 1.5, CY + 1, CZ - RAIO - 4.5, 0, 0), true);
        plugin.getLogger().info("Mundo das Almas: a Cidade dos Antigos foi construída em " + CX + " " + CY + " " + CZ + ".");
    }

    /** Uma casa de 7 x 7 de tijolos de tufo: atril com um capítulo e um baú. (sx, sz): para onde fica o centro. */
    private void casa(World w, int x0, int z0, int sx, int sz, int cap) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean borda = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            w.getBlockAt(x0 + dx, CY, z0 + dz).setType(Material.POLISHED_TUFF, false);
            for (int dy = 1; dy <= 4; dy++)
                w.getBlockAt(x0 + dx, CY + dy, z0 + dz).setType(borda ? (dy == 4 ? Material.CHISELED_TUFF_BRICKS : Material.TUFF_BRICKS) : Material.AIR, false);
            w.getBlockAt(x0 + dx, CY + 5, z0 + dz).setType(borda ? Material.TUFF_BRICK_SLAB : Material.POLISHED_TUFF, false);
        }
        // A porta (2 de altura) e as janelas.
        int px = x0 - sx * 3, pz = z0;
        for (int dy = 1; dy <= 2; dy++) w.getBlockAt(px, CY + dy, pz).setType(Material.AIR, false);
        w.getBlockAt(x0, CY + 2, z0 - 3).setType(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        w.getBlockAt(x0, CY + 2, z0 + 3).setType(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        w.getBlockAt(x0, CY + 4, z0).setType(Material.AIR, false);
        w.getBlockAt(x0, CY + 4, z0).setBlockData(Bukkit.createBlockData("minecraft:soul_lantern[hanging=true]"), false);
        // O atril (virado para a porta) e o baú no fundo.
        Block at = w.getBlockAt(x0, CY + 1, z0);
        at.setBlockData(Bukkit.createBlockData("minecraft:lectern[facing=" + (sx > 0 ? "west" : "east") + ",has_book=true]"), false);
        if (at.getState() instanceof Lectern l) l.getInventory().setItem(0, capitulo(cap));
        Block bau = w.getBlockAt(x0 + sx * 2, CY + 1, z0);
        bau.setBlockData(Bukkit.createBlockData("minecraft:chest[facing=" + (sx > 0 ? "west" : "east") + "]"), false);
        baus.put(bau.getX() + "," + bau.getY() + "," + bau.getZ(), 0L);
    }

    private void ilen(Location l) {
        l.getWorld().spawn(l, Mannequin.class, m -> {
            m.customName(Component.text("Ilen", Antigos.COR, TextDecoration.BOLD));
            m.setCustomNameVisible(true);
            m.setDescription(Component.text("Engenheira dos Antigos", NamedTextColor.GRAY));
            m.setImmovable(true);
            m.setInvulnerable(true);
            m.setPersistent(true);
            m.getPersistentDataContainer().set(K_ILEN, PersistentDataType.BYTE, (byte) 1);
            ItemStack peito = new ItemStack(Material.LEATHER_CHESTPLATE);
            peito.editMeta(org.bukkit.inventory.meta.LeatherArmorMeta.class, a -> a.setColor(org.bukkit.Color.fromRGB(0xC77A3A)));
            m.getEquipment().setChestplate(peito);
            m.getEquipment().setItemInMainHand(new ItemStack(Material.COPPER_INGOT));
        });
    }

    private void falarIlen(Player p) {
        Component titulo = Component.text("Ilen, engenheira dos Antigos", Antigos.COR, TextDecoration.BOLD);
        boolean venceu = p.getPersistentDataContainer().has(K_VENCEU_JUIZ);
        if (!venceu) {
            Telas.conversa(p, titulo, Antigos.diario(0), List.of(
                    Component.text("\"Você tem uma lanterna acesa. Então ainda está vivo lá em cima.\"", NamedTextColor.WHITE),
                    Component.text("\"Eu sou Ilen. Desenhei o foguete, as colônias... e trouxe todos nós para cá sem querer.\"", NamedTextColor.WHITE),
                    Component.text("\"O Juiz não deixa ninguém atravessar o rio. Toque o sino do Tribunal e enfrente-o. Os baús das casas guardam o que sobrou das nossas armas.\"", NamedTextColor.WHITE)), List.of());
            return;
        }
        List<Telas.Opcao> op = List.of(Telas.Opcao.de("Receber o último capítulo", "Diário dos Antigos XV", q -> {
            q.getInventory().addItem(capitulo(4)).values().forEach(s -> q.getWorld().dropItem(q.getLocation(), s));
            leu(q, 4);
            plugin.diario().marco(q, "adeus_ilen", "Ouviu o adeus da Ilen");
        }));
        Telas.conversa(p, titulo, Antigos.diario(0), List.of(
                Component.text("\"A balança quebrou. Os nossos estão atravessando.\"", NamedTextColor.WHITE),
                Component.text("\"Eu escrevi o fim da história. Leve com você: alguém lá em cima precisa saber.\"", NamedTextColor.WHITE)), op);
    }

    /** Baús da cidade: enchem de novo 30 minutos depois de esvaziados. */
    @EventHandler(ignoreCancelled = true)
    public void aoAbrirBau(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof Chest c) || !aqui(c.getWorld())) return;
        String k = c.getX() + "," + c.getY() + "," + c.getZ();
        Long antes = baus.get(k);
        if (antes == null || System.currentTimeMillis() - antes < 30 * 60_000L || !e.getInventory().isEmpty()) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> l = new ArrayList<>();
        if (r.nextDouble() < 0.5) l.add(laminaEspectral());
        if (r.nextDouble() < 0.35) l.add(mantoEspectral());
        if (r.nextDouble() < 0.7) l.add(Raro.ESSENCIA_ETEREA.criar(1 + r.nextInt(3)));
        if (r.nextDouble() < 0.5) l.add(Raro.ALMA_PENADA.criar(1 + r.nextInt(2)));
        if (r.nextDouble() < 0.4) l.add(new ItemStack(Material.GOLDEN_APPLE, 1 + r.nextInt(2)));
        if (r.nextDouble() < 0.5) {
            ItemStack cura = new ItemStack(Material.SPLASH_POTION, 2);
            cura.editMeta(org.bukkit.inventory.meta.PotionMeta.class, m -> m.setBasePotionType(org.bukkit.potion.PotionType.HEALING));
            l.add(cura);
        }
        for (ItemStack i : l) e.getInventory().setItem(r.nextInt(e.getInventory().getSize()), i);
        baus.put(k, System.currentTimeMillis());
        salvar();
    }

    // =====================================================================
    //  O Juiz
    // =====================================================================

    private void tocarSino(Player p, Block sino) {
        Entity j = juiz == null ? null : Bukkit.getEntity(juiz);
        if (j != null && j.isValid()) return;
        long falta = juizVolta - System.currentTimeMillis();
        if (falta > 0) {
            p.sendActionBar(Component.text("O sino não ecoa. O Juiz volta em " + (falta / 60000 + 1) + " min.", NamedTextColor.GRAY));
            return;
        }
        World w = sino.getWorld();
        Location l = new Location(w, CX + 0.5, CY + 2, CZ + 3.5, 180, 0);
        w.playSound(l, Sound.BLOCK_BELL_RESONATE, 2f, 0.5f);
        w.strikeLightningEffect(l);
        WitherSkeleton s = w.spawn(l, WitherSkeleton.class, x -> {
            x.customName(Component.text("O Juiz", JUIZ, TextDecoration.BOLD));
            x.setCustomNameVisible(true);
            x.setPersistent(false);
            x.setRemoveWhenFarAway(false);
            x.getPersistentDataContainer().set(K_JUIZ, PersistentDataType.BYTE, (byte) 1);
            base(x, Attribute.MAX_HEALTH, 320);
            x.setHealth(320);
            base(x, Attribute.SCALE, 1.8);
            base(x, Attribute.ATTACK_DAMAGE, 3);
            base(x, Attribute.KNOCKBACK_RESISTANCE, 0.8);
            base(x, Attribute.FOLLOW_RANGE, 40);
            x.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
            x.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
            x.getEquipment().setItemInMainHandDropChance(0);
            x.getEquipment().setHelmetDropChance(0);
            x.setTarget(p);
        });
        juiz = s.getUniqueId();
        juizChamou = false;
        if (barraJuiz == null) barraJuiz = BossBar.bossBar(Component.text("O Juiz", JUIZ, TextDecoration.BOLD), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
        barraJuiz.progress(1f);
        for (Player o : w.getPlayers()) if (o.getLocation().distanceSquared(l) < 60 * 60) {
            o.showTitle(Title.title(Component.text("O Juiz", JUIZ, TextDecoration.BOLD), Component.text("\"Quem fugiu do mundo não volta a ele.\"", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
        }
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        var i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    /** A cada segundo, com o Juiz de pé: a barra e, a cada 10 s, o Julgamento (puxa e prende quem está perto). */
    private void juizTick() {
        if (juiz == null) return;
        Entity e = Bukkit.getEntity(juiz);
        if (!(e instanceof WitherSkeleton j) || !j.isValid() || j.isDead()) {
            if (barraJuiz != null) for (Player o : Bukkit.getOnlinePlayers()) o.hideBossBar(barraJuiz);
            if (e == null || !e.isValid()) juiz = null;
            return;
        }
        double max = j.getAttribute(Attribute.MAX_HEALTH).getValue();
        barraJuiz.progress((float) Math.max(0, Math.min(1, j.getHealth() / max)));
        for (Player o : j.getWorld().getPlayers()) {
            if (o.getLocation().distanceSquared(j.getLocation()) < 50 * 50) o.showBossBar(barraJuiz);
            else o.hideBossBar(barraJuiz);
        }
        j.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, j.getLocation().add(0, 2, 0), 6, 0.6, 1, 0.6, 0.01);
        if (ciclo % 10 == 0) {
            boolean alguem = false;
            for (Player o : j.getWorld().getPlayers()) {
                if (o.getGameMode() == GameMode.CREATIVE || o.getGameMode() == GameMode.SPECTATOR || o.getLocation().distanceSquared(j.getLocation()) > 14 * 14) continue;
                alguem = true;
                Vector puxa = j.getLocation().toVector().subtract(o.getLocation().toVector()).normalize().multiply(0.9).setY(0.3);
                o.setVelocity(puxa);
                o.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
                o.sendActionBar(Component.text("⚖ Julgado!", JUIZ, TextDecoration.BOLD));
            }
            if (alguem) j.getWorld().playSound(j.getLocation(), Sound.BLOCK_BELL_USE, 1.5f, 0.5f);
        }
        if (!juizChamou && j.getHealth() < max * 0.5) {
            juizChamou = true;
            for (int i = 0; i < 3; i++) sombra(j.getLocation().add(ThreadLocalRandom.current().nextDouble(-3, 3), 2, ThreadLocalRandom.current().nextDouble(-3, 3)));
            j.getWorld().playSound(j.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.5f, 0.6f);
            for (Player o : j.getWorld().getPlayers())
                if (o.getLocation().distanceSquared(j.getLocation()) < 40 * 40) o.sendMessage(Component.text("⚖ \"Testemunhas! Venham pesar esta alma.\"", JUIZ));
        }
    }

    // =====================================================================
    //  Almas penadas e criaturas
    // =====================================================================

    public void segundo() {
        ciclo++;
        juizTick();
        World w = mundo();
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (p.isDead()) continue;
            if (ciclo % 5 == 0) criaturas(p);
            if (ciclo % 10 == 0) conferirBarqueiros(p);
        }
        if (ciclo % 3 == 0) for (Entity e : w.getEntitiesByClass(Allay.class))
            if (e.getPersistentDataContainer().has(K_PENADA)) w.spawnParticle(Particle.SOUL, e.getLocation().add(0, 0.4, 0), 2, 0.2, 0.2, 0.2, 0.01);
    }

    private void criaturas(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        World w = p.getWorld();
        int penadas = 0, monstros = 0;
        for (Entity e : w.getNearbyEntities(p.getLocation(), 40, 30, 40)) {
            if (e.getPersistentDataContainer().has(K_PENADA)) penadas++;
            else if (e.getPersistentDataContainer().has(K_CRIATURA)) monstros++;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location l = lugarPerto(p, r);
        if (l == null) return;
        if (penadas < 3 && r.nextDouble() < 0.55) {
            w.spawn(l.clone().add(0, 1.5, 0), Allay.class, a -> {
                a.customName(Component.text("Alma Penada", COR));
                a.setPersistent(false);
                a.setRemoveWhenFarAway(true);
                a.setCanPickupItems(false);
                a.getPersistentDataContainer().set(K_PENADA, PersistentDataType.BYTE, (byte) 1);
            });
            return;
        }
        // Na cidade e perto do cais é mais calmo.
        if (monstros >= 4 || perto(p.getLocation(), CX, CZ, RAIO + 10) || p.getGameMode() == GameMode.CREATIVE) return;
        if (r.nextDouble() < 0.6) sombra(l.clone().add(0, 2, 0));
        else w.spawn(l, Husk.class, h -> {
            h.customName(Component.text("Penitente", NamedTextColor.GRAY));
            marcar(h);
            base(h, Attribute.SCALE, 1.15);
            base(h, Attribute.MOVEMENT_SPEED, 0.2);
            h.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            h.getEquipment().setHelmetDropChance(0);
        });
    }

    private void sombra(Location l) {
        l.getWorld().spawn(l, Vex.class, v -> {
            v.customName(Component.text("Sombra Errante", TextColor.color(0x5A6070)));
            marcar(v);
            base(v, Attribute.MAX_HEALTH, 16);
            v.setHealth(16);
            v.getEquipment().setItemInMainHand(null);
        });
    }

    private static void marcar(LivingEntity e) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(true);
        e.getPersistentDataContainer().set(K_CRIATURA, PersistentDataType.BYTE, (byte) 1);
    }

    private static Location lugarPerto(Player p, ThreadLocalRandom r) {
        World w = p.getWorld();
        double a = r.nextDouble() * Math.PI * 2, dist = 12 + r.nextDouble() * 16;
        int x = (int) (p.getLocation().getX() + Math.cos(a) * dist), z = (int) (p.getLocation().getZ() + Math.sin(a) * dist);
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
        Block b = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING);
        if (b.getY() < 40 || b.isLiquid() || !b.getType().isSolid()) return null;
        return b.getLocation().add(0.5, 1, 0.5);
    }

    private static boolean perto(Location l, int x, int z, int raio) {
        return Math.pow(l.getX() - x, 2) + Math.pow(l.getZ() - z, 2) < (double) raio * raio;
    }

    private void recolher(Player p, Entity alma) {
        Location l = alma.getLocation();
        alma.remove();
        l.getWorld().spawnParticle(Particle.SOUL, l.clone().add(0, 0.5, 0), 20, 0.3, 0.4, 0.3, 0.04);
        p.playSound(l, Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 1.2f);
        List<ItemStack> ganhou = new ArrayList<>(List.of(Raro.ALMA_PENADA.criar(1)));
        if (ThreadLocalRandom.current().nextDouble() < 0.2) ganhou.add(Raro.ESSENCIA_ETEREA.criar(1));
        for (ItemStack i : ganhou) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.sendActionBar(Component.text("🏮 Uma alma penada entrou na lanterna.", COR));
        plugin.titulos().registrar(p, "almas_coletadas", 1);
    }

    /** Se algum Barqueiro sumiu (morto por comando, por exemplo), ele volta para o cais. */
    private void conferirBarqueiros(Player p) {
        World w = p.getWorld();
        for (int[] c : cais) {
            if (Math.pow(c[0] - p.getLocation().getX(), 2) + Math.pow(c[2] - p.getLocation().getZ(), 2) > 48 * 48) continue;
            Location onde = new Location(w, c[0] + 0.5, c[1] + 1, c[2] + 3.5, 180, 0);
            boolean tem = false;
            for (Entity e : w.getNearbyEntities(onde, 4, 3, 4)) if (e.getPersistentDataContainer().has(K_BARQUEIRO)) tem = true;
            if (!tem) barqueiro(onde, false);
        }
    }

    // =====================================================================
    //  Combate e mortes
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        if (!aqui(e.getEntity().getWorld())) return;
        Entity d = e.getDamager();
        Player p = d instanceof Player pl ? pl : d instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null) return;
        var pdc = e.getEntity().getPersistentDataContainer();
        if (!pdc.has(K_CRIATURA) && !pdc.has(K_JUIZ)) return;
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.getPersistentDataContainer().has(K_ESPECTRAL)) e.setDamage(e.getDamage() * 1.8);
    }

    /** Almas penadas não apanham (são recolhidas, não mortas). */
    @EventHandler(ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(K_PENADA) || e.getEntity().getPersistentDataContainer().has(K_BARQUEIRO)
                || e.getEntity().getPersistentDataContainer().has(K_ILEN)) e.setCancelled(true);
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        var pdc = e.getEntity().getPersistentDataContainer();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (pdc.has(K_CRIATURA)) {
            e.getDrops().clear();
            if (r.nextDouble() < 0.4) e.getDrops().add(Raro.ESSENCIA_ETEREA.criar(1));
            e.setDroppedExp(6);
            return;
        }
        if (!pdc.has(K_JUIZ)) return;
        e.getDrops().clear();
        e.getDrops().add(Raro.ESSENCIA_ETEREA.criar(8 + r.nextInt(5)));
        e.getDrops().add(Raro.ALMA_PENADA.criar(4));
        if (r.nextDouble() < 0.3) e.getDrops().add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
        e.setDroppedExp(300);
        juiz = null;
        juizVolta = System.currentTimeMillis() + 10 * 60_000L;
        salvar();
        if (barraJuiz != null) for (Player o : Bukkit.getOnlinePlayers()) o.hideBossBar(barraJuiz);
        Location l = e.getEntity().getLocation();
        l.getWorld().playSound(l, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
        l.getWorld().spawnParticle(Particle.SOUL, l.clone().add(0, 1.5, 0), 80, 1, 1.5, 1, 0.05);
        for (Player o : l.getWorld().getPlayers()) {
            if (o.getLocation().distanceSquared(l) > 50 * 50) continue;
            o.showTitle(Title.title(Component.text("⚖ A balança quebrou", JUIZ, TextDecoration.BOLD), Component.text("Fale com a Ilen.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
            if (!o.getPersistentDataContainer().has(K_VENCEU_JUIZ)) {
                o.getPersistentDataContainer().set(K_VENCEU_JUIZ, PersistentDataType.BYTE, (byte) 1);
                plugin.diario().marco(o, "juiz", "Quebrou a balança do Juiz no Mundo das Almas");
            }
            plugin.titulos().registrar(o, "juiz_julgado", 1);
        }
    }

    // =====================================================================
    //  Utilidades
    // =====================================================================

    private static int contar(Player p, Raro r) {
        int n = 0;
        for (ItemStack i : p.getInventory().getContents()) if (i != null && Raro.de(i) == r) n += i.getAmount();
        return n;
    }

    private static void tirar(Player p, Raro r, int qtd) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (qtd <= 0) return;
            if (i == null || Raro.de(i) != r) continue;
            int t = Math.min(qtd, i.getAmount());
            i.setAmount(i.getAmount() - t);
            qtd -= t;
        }
    }

    /** Admin: a cidade já existe? Onde fica? */
    public String info() {
        return "Cidade dos Antigos " + (cidade ? "construída" : "ainda não construída") + " em " + CX + " " + CY + " " + CZ + "; " + cais.size() + " cais.";
    }

    /** Admin: leva até a cidade (construindo, se preciso). */
    public void irCidadeAdmin(Player p) {
        if (!aqui(p.getWorld())) {
            p.sendMessage(Component.text("Entre no Mundo das Almas primeiro (/rpgadmin dimensao almas).", NamedTextColor.RED));
            return;
        }
        irParaCidade(p);
    }

    public void desligar() {
        if (barraJuiz != null) for (Player o : Bukkit.getOnlinePlayers()) o.hideBossBar(barraJuiz);
        Entity j = juiz == null ? null : Bukkit.getEntity(juiz);
        if (j != null) j.remove();
    }

}
