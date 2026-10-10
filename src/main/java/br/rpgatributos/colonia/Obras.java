package br.rpgatributos.colonia;

import br.rpgatributos.MarcadorDeBlocos;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Ingrediente;
import br.rpgatributos.territorio.Territorio;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Construções da colônia no estilo MineColonies: o jogador pega um projeto na Prefeitura, coloca
 * no chão (o lado em que ele está vira a entrada) e aparece uma cerca em volta do canteiro, com um
 * portão na entrada. Dá para girar ou mudar o canteiro de lugar até a obra começar. Um baú com os
 * materiais dentro da cerca (fora da planta) e um Construtor livre: ele vai até lá, limpa o terreno
 * e ergue a construção bloco a bloco, tirando tudo do baú da obra.
 */
public final class Obras implements Listener {

    /** Construções ao mesmo tempo: 1 + nível da colônia. */
    private static int maxObras(Colonia c) { return 1 + c.nivel; }

    /** Blocos que o Construtor nunca derruba nem aceita na área (baús, fornalhas, camas...). */
    private static final Set<Material> PROTEGIDOS = Set.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.FURNACE,
            Material.BLAST_FURNACE, Material.SMOKER, Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.BREWING_STAND,
            Material.CRAFTER, Material.DECORATED_POT, Material.JUKEBOX, Material.CHISELED_BOOKSHELF, Material.LECTERN, Material.BEACON,
            Material.ENDER_CHEST, Material.BELL, Material.BEDROCK, Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
            Material.END_PORTAL_FRAME, Material.REINFORCED_DEEPSLATE, Material.ENCHANTING_TABLE, Material.RESPAWN_ANCHOR);
    /** Terreno: pode estar marcado como "colocado por jogador" e ainda assim ficar na área (quem aplainou o chão). */
    private static final Set<Material> TERRENO = Set.of(Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.ROOTED_DIRT,
            Material.PODZOL, Material.MUD, Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.STONE, Material.COBBLESTONE,
            Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.ANDESITE, Material.DIORITE, Material.GRANITE, Material.TUFF,
            Material.SANDSTONE, Material.CLAY, Material.SNOW_BLOCK, Material.DIRT_PATH, Material.FARMLAND, Material.MOSS_BLOCK);
    private static final Material CERCA = Material.OAK_FENCE, PORTAO = Material.OAK_FENCE_GATE;

    private enum Tipo { CATALOGO, LISTA, OBRA, CONSTRUCAO }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        final Colonia colonia;
        final List<String> ids = new ArrayList<>();
        UUID obra;
        int construcao = -1;
        Inventory inventario;

        Tela(Tipo tipo, Colonia colonia) {
            this.tipo = tipo;
            this.colonia = colonia;
        }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private static final int[] SLOTS_LISTA = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] SLOTS_MATERIAIS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int O_INFO = 4, O_GIRAR = 45, O_MOVER = 46, O_CONTORNO = 47, O_TRAZER = 48, O_VOLTAR = 49, O_AREA_MENOS = 50,
            O_AREA_MAIS = 51, O_CANCELAR = 53;
    /** Do seu jeito: até quantos blocos a mais de cada lado a área pode crescer. */
    private static final int EXTRA_MAX = 6;
    private static final int L_INFO = 4, VOLTAR = 49;

    private final RPGAtributos plugin;
    private final Colonias colonias;
    private final Plantas plantas;
    private final MarcadorDeBlocos colocados;
    /** "mundo;x;y;z" de cada bloco de cerca → a obra. */
    private final Map<String, Obra> cercas = new HashMap<>();
    /** "mundo;x;y;z" da placa de cada construção pronta → a construção. */
    private final Map<String, Construcao> placas = new HashMap<>();
    /** Quem quebrou uma placa e precisa quebrar de novo para confirmar → (chave da placa, até quando). */
    private final Map<UUID, Map.Entry<String, Long>> confirmando = new HashMap<>();
    private int ciclo;

    Obras(RPGAtributos plugin, Colonias colonias) {
        this.plugin = plugin;
        this.colonias = colonias;
        this.plantas = new Plantas(plugin);
        this.colocados = new MarcadorDeBlocos(plugin, "colocados");
    }

    public Plantas plantas() { return plantas; }

    void iniciar() {
        plantas.carregar();
    }

    /** /rpgadmin reload: lê os projetos de novo. */
    public void recarregarPlantas() {
        plantas.carregar();
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static String chave(String mundo, int x, int y, int z) { return mundo + ";" + x + ";" + y + ";" + z; }

    /** Uma obra (carregada do arquivo) entra no índice das cercas. */
    void indexar(Obra o) {
        for (int[] c : o.cerca) cercas.put(chave(o.mundo, c[0], c[1], c[2]), o);
    }

    void desindexar(Obra o) {
        for (int[] c : o.cerca) cercas.remove(chave(o.mundo, c[0], c[1], c[2]));
    }

    private Obra daCerca(Block b) {
        return cercas.get(chave(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()));
    }

    private Colonia coloniaDa(Obra o) {
        for (Colonia c : colonias.todas()) if (c.obras.contains(o)) return c;
        return null;
    }

    private Obra obra(Colonia c, UUID id) {
        for (Obra o : c.obras) if (o.id.equals(id)) return o;
        return null;
    }

    /** A colônia de um lugar: a do território, ou a que enxerga esse lugar. */
    private Colonia coloniaEm(Location l) {
        Territorio t = plugin.territorios().em(l);
        if (t != null) {
            Colonia c = colonias.de(t.dono());
            return c != null && colonias.naColonia(c, l) ? c : null;
        }
        for (Colonia c : colonias.todas()) if (colonias.naColonia(c, l)) return c;
        return null;
    }

    // =====================================================================
    //  Onde pode construir
    // =====================================================================

    /** Motivo para não poder pôr essa obra aqui, ou null. */
    private String impedimento(Colonia c, Planta p, Obra o) {
        World w = o.world();
        if (w == null || !w.getName().equals(c.mundo)) return "A obra precisa ficar no mundo da colônia.";
        int[] a = o.area(p, 2);
        Territorio t = colonias.territorioDe(c);
        for (int cx = a[0] >> 4; cx <= a[2] >> 4; cx++) {
            for (int cz = a[1] >> 4; cz <= a[3] >> 4; cz++) {
                if (!w.isChunkLoaded(cx, cz)) return "Chegue mais perto: parte do lugar não está carregada.";
                if (t != null && plugin.territorios().em(w.getName(), cx, cz) != t) return "A obra e a cerca precisam caber inteiras no território da colônia.";
            }
        }
        if (t == null) {
            for (int[] canto : new int[][]{{a[0], a[1]}, {a[2], a[1]}, {a[0], a[3]}, {a[2], a[3]}}) {
                if (!c.dentro(new Location(w, canto[0], o.y, canto[1]))) return "A obra precisa caber perto da Prefeitura.";
            }
        }
        int alt = o.livre ? alturaVistoria(p) : p.altura();
        if (o.y < c.y - Colonia.ABAIXO || o.y + alt > c.y + Colonia.ACIMA) return "Alto ou fundo demais em relação à Prefeitura.";
        if (o.y + alt >= w.getMaxHeight() || o.y - 4 <= w.getMinHeight()) return "Não cabe nessa altura.";
        if (c.x >= a[0] - 1 && c.x <= a[2] + 1 && c.z >= a[1] - 1 && c.z <= a[3] + 1 && Math.abs(c.y - o.y) < alt + 3) {
            return "Muito perto da Prefeitura (o sino precisa ficar livre).";
        }
        for (Obra outra : c.obras) {
            if (outra == o || outra.id.equals(o.id)) continue;
            Planta po = plantas.de(outra.planta);
            if (po != null && outra.mundo.equals(o.mundo) && sobrepoe(a, outra.area(po, 2))) return "Encosta em outra obra (" + po.nome() + ").";
        }
        for (Construcao k : c.construcoes) {
            Planta pk = plantas.de(k.planta());
            if (pk == null || !k.mundo().equals(o.mundo)) continue;
            if (sobrepoe(o.area(p, 1), k.area().area(pk, 0)) && Math.abs(k.y - o.y) < Math.max(alt, pk.altura())) {
                return "Fica em cima de uma construção pronta (" + pk.nome() + ").";
            }
        }
        // Do seu jeito: o que já tem na área é do jogador e conta na vistoria.
        if (o.livre) return null;
        // O que já tem na área: nada de baús, estações ou coisas que alguém construiu.
        for (int ly = 0; ly < p.altura(); ly++) {
            for (int lz = 0; lz < p.profundidade(); lz++) {
                for (int lx = 0; lx < p.largura(); lx++) {
                    if (p.em(lx, ly, lz) == null) continue;
                    Block b = o.bloco(w, p, lx, ly, lz);
                    Material m = b.getType();
                    if (m.isAir()) continue;
                    String onde = " (" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ")";
                    if (PROTEGIDOS.contains(m) || m.name().endsWith("SHULKER_BOX") || Tag.BEDS.isTagged(m)) {
                        return "Tem um bloco que o Construtor não pode derrubar na área" + onde + ". Tire ele ou escolha outro lugar.";
                    }
                    if (plugin.ehEstacao(b)) return "Tem uma estação do plugin na área" + onde + ". Escolha outro lugar.";
                    if (!TERRENO.contains(m) && colocados.marcado(b)) {
                        return "Tem algo que alguém construiu na área" + onde + ". Tire ou escolha outro lugar.";
                    }
                }
            }
        }
        return null;
    }

    /** Até que altura a vistoria olha numa área "do seu jeito". */
    static int alturaVistoria(Planta p) {
        int pedida = 0;
        for (Vistoria.Requisito r : p.niveis()) pedida = Math.max(pedida, r.altura());
        return Math.min(40, Math.max(p.altura(), pedida) + 6);
    }

    private static boolean sobrepoe(int[] a, int[] b) {
        return a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3];
    }

    // =====================================================================
    //  A cerca do canteiro
    // =====================================================================

    /** A altura do chão numa coluna perto de y (o primeiro bloco firme de cima para baixo), ou null. */
    private static Integer chao(World w, int x, int y, int z) {
        for (int yy = y + 3; yy >= y - 4; yy--) {
            Block b = w.getBlockAt(x, yy, z);
            if (b.getType().isSolid() && !Tag.LEAVES.isTagged(b.getType())) return yy;
        }
        return null;
    }

    private void criarCerca(Obra o, Planta p) {
        World w = o.world();
        if (w == null) return;
        int[] a = o.area(p, 2);
        Block portao = o.portao(w, p);
        List<Block> postas = new ArrayList<>();
        for (int x = a[0]; x <= a[2]; x++) {
            for (int z = a[1]; z <= a[3]; z++) {
                if (x != a[0] && x != a[2] && z != a[1] && z != a[3]) continue;
                Integer chao = chao(w, x, o.y, z);
                if (chao == null) continue;
                Block b = w.getBlockAt(x, chao + 1, z);
                if (!b.getType().isAir() && !b.isReplaceable()) continue;
                if (b.isLiquid()) continue;
                boolean ehPortao = x == portao.getX() && z == portao.getZ();
                if (ehPortao) {
                    Gate g = (Gate) PORTAO.createBlockData();
                    g.setFacing(Planta.face(BlockFace.SOUTH, o.rot));
                    b.setBlockData(g, false);
                } else {
                    b.setType(CERCA, false);
                    postas.add(b);
                }
                o.cerca.add(new int[]{b.getX(), b.getY(), b.getZ()});
            }
        }
        for (Block b : postas) {
            if (!(b.getBlockData() instanceof Fence f)) continue;
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                Material v = b.getRelative(face).getType();
                f.setFace(face, v == CERCA || v == PORTAO);
            }
            b.setBlockData(f, false);
        }
        indexar(o);
    }

    private void removerCerca(Obra o) {
        desindexar(o);
        World w = o.world();
        if (w != null) {
            for (int[] c : o.cerca) {
                if (!w.isChunkLoaded(c[0] >> 4, c[2] >> 4)) continue;
                Block b = w.getBlockAt(c[0], c[1], c[2]);
                if (b.getType() == CERCA || b.getType() == PORTAO) b.setType(Material.AIR, false);
            }
        }
        o.cerca.clear();
    }

    /** O contorno da planta em partículas por uns segundos (verde = ok, vermelho = não pode). */
    private void mostrarContorno(Obra o, Planta p, boolean ok) {
        World w = o.world();
        if (w == null) return;
        int[] a = o.area(p, 0);
        Particle.DustOptions cor = new Particle.DustOptions(ok ? org.bukkit.Color.LIME : org.bukkit.Color.RED, 1.2f);
        Location entrada = o.entrada(w, p);
        for (int vez = 0; vez < 8; vez++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double y0 = o.y + 1.05, y1 = o.y + p.altura();
                for (int x = a[0]; x <= a[2] + 1; x++) {
                    for (double y : new double[]{y0, y1}) {
                        w.spawnParticle(Particle.DUST, x, y, a[1], 1, 0, 0, 0, 0, cor);
                        w.spawnParticle(Particle.DUST, x, y, a[3] + 1, 1, 0, 0, 0, 0, cor);
                    }
                }
                for (int z = a[1]; z <= a[3] + 1; z++) {
                    for (double y : new double[]{y0, y1}) {
                        w.spawnParticle(Particle.DUST, a[0], y, z, 1, 0, 0, 0, 0, cor);
                        w.spawnParticle(Particle.DUST, a[2] + 1, y, z, 1, 0, 0, 0, 0, cor);
                    }
                }
                for (double y = y0; y <= y1; y += 0.5) {
                    for (double[] q : new double[][]{{a[0], a[1]}, {a[2] + 1, a[1]}, {a[0], a[3] + 1}, {a[2] + 1, a[3] + 1}}) {
                        w.spawnParticle(Particle.DUST, q[0], y, q[1], 1, 0, 0, 0, 0, cor);
                    }
                }
                w.spawnParticle(Particle.HAPPY_VILLAGER, entrada.clone().add(0, 0.3, 0), 6, 0.3, 0.3, 0.3, 0);
            }, vez * 10L);
        }
    }

    // =====================================================================
    //  Jogador: pôr o projeto, menu da cerca, proteger a cerca
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player jogador = e.getPlayer();
        Block clicado = e.getClickedBlock();
        // Clique na placa de uma construção: o menu dela (como o bloco do prédio no MineColonies).
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && clicado != null) {
            Construcao k = placas.get(chave(clicado.getWorld().getName(), clicado.getX(), clicado.getY(), clicado.getZ()));
            if (k != null) {
                e.setCancelled(true);
                Colonia c = coloniaDa(k);
                if (c == null) return;
                if (!colonias.daColonia(jogador, c)) {
                    Planta pk = plantas.de(k.planta);
                    jogador.sendActionBar(Component.text("⌂ " + (pk == null ? k.planta : pk.nome()) + " da colônia de " + c.nomeDono + " · nível " + k.nivel,
                            Prefeituras.COR));
                    return;
                }
                abrirConstrucao(jogador, c, c.construcoes.indexOf(k));
                return;
            }
        }
        // Agachado + clique na cerca: menu da obra.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && clicado != null && jogador.isSneaking()) {
            Obra o = daCerca(clicado);
            if (o != null) {
                e.setCancelled(true);
                Colonia c = coloniaDa(o);
                if (c == null) return;
                if (!colonias.daColonia(jogador, c)) { Colonias.erro(jogador, "Essa obra é de outra colônia."); return; }
                abrirObra(jogador, c, o);
                return;
            }
        }
        Planta p = plantas.deItem(e.getItem());
        if (p == null) return;
        boolean livre = plantas.livre(e.getItem());
        e.setCancelled(true);
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || clicado == null) {
            jogador.sendActionBar(Component.text("Clique com o botão direito no CHÃO onde quer a obra.", Prefeituras.COR));
            return;
        }
        Block chao = clicado.isReplaceable() ? clicado.getRelative(BlockFace.DOWN) : clicado;
        Colonia c = coloniaEm(chao.getLocation());
        if (c == null) { Colonias.erro(jogador, "Use o projeto dentro do território da sua colônia."); return; }
        if (!colonias.gerencia(jogador, c)) { Colonias.erro(jogador, "Só o dono da colônia e quem gerencia o território podem marcar obras."); return; }
        if (c.nivel < p.nivel()) { Colonias.erro(jogador, p.nome() + " precisa da colônia no nível " + p.nivel() + "."); return; }
        if (c.obras.size() >= maxObras(c)) {
            Colonias.erro(jogador, "No nível " + c.nivel + " a colônia toca até " + maxObras(c) + " obras ao mesmo tempo. Termine ou cancele uma.");
            return;
        }
        // O lado em que o jogador está vira a frente: ele olha para o norte → a entrada fica ao sul (virada para ele).
        BlockFace frente = jogador.getFacing().getOppositeFace();
        int rot = 0;
        for (int i = 0; i < 4; i++) if (Planta.GIRO[i] == frente) rot = i;
        Obra o = new Obra(UUID.randomUUID(), p.id(), chao.getWorld().getName(), chao.getX(), chao.getY(), chao.getZ(), rot);
        o.livre = livre;
        String erro = impedimento(c, p, o);
        if (erro != null) {
            Colonias.erro(jogador, "✖ " + erro);
            mostrarContorno(o, p, false);
            return;
        }
        criarCerca(o, p);
        c.obras.add(o);
        ItemStack mao = jogador.getInventory().getItemInMainHand();
        mao.setAmount(mao.getAmount() - 1);
        jogador.getInventory().setItemInMainHand(mao.getAmount() > 0 ? mao : null);
        mostrarContorno(o, p, true);
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 0.8f);
        jogador.sendMessage(Component.text("⌂ Canteiro da obra marcado: " + p.nome() + ". ", Prefeituras.COR, TextDecoration.BOLD)
                .append(Component.text("A entrada fica do lado do portão.", NamedTextColor.GRAY)));
        if (livre) {
            jogador.sendMessage(Component.text("  1. Agachado + clique na cerca: girar, aumentar a área ou ver o mínimo de cada nível.", NamedTextColor.GRAY));
            jogador.sendMessage(Component.text("  2. Construa do seu jeito DENTRO da cerca, atendendo o mínimo do nível 1.", NamedTextColor.GRAY));
            jogador.sendMessage(Component.text("  3. Peça a vistoria no menu da cerca: passou, vira construção da colônia.", NamedTextColor.GRAY));
        } else {
            jogador.sendMessage(Component.text("  1. Agachado + clique na cerca: girar, mudar de lugar ou ver os materiais.", NamedTextColor.GRAY));
            jogador.sendMessage(Component.text("  2. Ponha um baú DENTRO da cerca (fora da obra) com os materiais.", NamedTextColor.GRAY));
            jogador.sendMessage(Component.text("  3. Um Construtor livre (posto: bancada de trabalho) vem erguer a obra.", NamedTextColor.GRAY));
        }
        colonias.salvar();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b0 = e.getBlock();
        String kp = chave(b0.getWorld().getName(), b0.getX(), b0.getY(), b0.getZ());
        Construcao k = placas.get(kp);
        if (k != null) {
            Player jog = e.getPlayer();
            Colonia c = coloniaDa(k);
            Planta pk = plantas.de(k.planta);
            String nome = pk == null ? k.planta : pk.nome();
            if (c == null || !colonias.gerencia(jog, c)) {
                e.setCancelled(true);
                jog.sendActionBar(Component.text("Essa é a placa de uma construção da colônia (" + nome + ").", NamedTextColor.RED));
                return;
            }
            Map.Entry<String, Long> conf = confirmando.get(jog.getUniqueId());
            if (conf == null || !conf.getKey().equals(kp) || System.currentTimeMillis() > conf.getValue()) {
                e.setCancelled(true);
                confirmando.put(jog.getUniqueId(), Map.entry(kp, System.currentTimeMillis() + 10_000));
                jog.sendMessage(Component.text("⌂ Quebrar a placa DESFAZ a construção (" + nome + ")"
                        + (pk != null && pk.efeito() == Planta.Efeito.TORRE_DE_VIGIA ? " e o território que ela vigia" : "")
                        + ". Os blocos ficam. Quebre de novo em 10 segundos para confirmar.", NamedTextColor.GOLD));
                return;
            }
            confirmando.remove(jog.getUniqueId());
            e.setDropItems(false);
            removerConstrucao(c, k, false);
            jog.sendMessage(Component.text("⌂ " + nome + " deixou de ser uma construção da colônia.", NamedTextColor.GRAY));
            return;
        }
        Obra o = daCerca(e.getBlock());
        if (o == null) return;
        e.setCancelled(true);
        Planta p = plantas.de(o.planta);
        e.getPlayer().sendActionBar(Component.text("Essa cerca marca a obra" + (p == null ? "" : " (" + p.nome() + ")")
                + ". Agachado + clique nela abre o menu.", Prefeituras.COR));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExplodirEntidade(org.bukkit.event.entity.EntityExplodeEvent e) {
        e.blockList().removeIf(this::protegido);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExplodirBloco(org.bukkit.event.block.BlockExplodeEvent e) {
        e.blockList().removeIf(this::protegido);
    }

    /** Cerca de canteiro ou placa de construção (explosão e pistão não mexem). */
    private boolean protegido(Block b) {
        String k = chave(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        return cercas.containsKey(k) || placas.containsKey(k);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEmpurrar(org.bukkit.event.block.BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (protegido(b)) { e.setCancelled(true); return; }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPuxar(org.bukkit.event.block.BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (protegido(b)) { e.setCancelled(true); return; }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST && b.getType() != Material.BARREL) return;
        for (Colonia c : colonias.todas()) {
            for (Obra o : c.obras) {
                Planta p = plantas.de(o.planta);
                if (p == null || o.livre || !o.mundo.equals(b.getWorld().getName()) || !o.dentro(p, b.getX(), b.getZ(), 1)) continue;
                if (o.dentro(p, b.getX(), b.getZ(), 0)) {
                    e.getPlayer().sendMessage(Component.text("⌂ Esse baú está dentro da área da construção: o Construtor vai precisar do lugar. "
                            + "Ponha entre a obra e a cerca.", NamedTextColor.GOLD));
                } else {
                    o.bau = b.getLocation();
                    e.getPlayer().sendMessage(Component.text("⌂ Baú da obra (" + p.nome() + ") definido. Encha com os materiais.", Prefeituras.COR));
                }
                return;
            }
        }
    }

    // =====================================================================
    //  O baú da obra
    // =====================================================================

    /** Procura o baú dentro da cerca, fora da planta. */
    private Location acharBau(Obra o, Planta p, World w) {
        if (o.bau != null && o.bau.getBlock().getState() instanceof Container) return o.bau;
        int[] a = o.area(p, 1);
        for (int x = a[0]; x <= a[2]; x++) {
            for (int z = a[1]; z <= a[3]; z++) {
                if (x != a[0] && x != a[2] && z != a[1] && z != a[3]) continue;
                for (int y = o.y - 1; y <= o.y + 3; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    Material m = b.getType();
                    if ((m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) && b.getState() instanceof Container) {
                        return b.getLocation();
                    }
                }
            }
        }
        return null;
    }

    private static Inventory inventario(Location bau) {
        return bau != null && bau.getBlock().getState() instanceof Container ct ? ct.getInventory() : null;
    }

    private static Predicate<ItemStack> simples(Material m) {
        return s -> s != null && s.getType() == m && Ingrediente.simples(s);
    }

    private static int contar(Inventory inv, Material m) {
        int n = 0;
        if (inv == null) return 0;
        Predicate<ItemStack> t = simples(m);
        for (ItemStack s : inv.getContents()) if (t.test(s)) n += s.getAmount();
        return n;
    }

    private static void tirar(Inventory inv, Material m, int qtd) {
        Predicate<ItemStack> t = simples(m);
        ItemStack[] cont = inv.getContents();
        for (int i = 0; i < cont.length && qtd > 0; i++) {
            if (!t.test(cont[i])) continue;
            int tira = Math.min(qtd, cont[i].getAmount());
            cont[i].setAmount(cont[i].getAmount() - tira);
            if (cont[i].getAmount() <= 0) cont[i] = null;
            qtd -= tira;
        }
        inv.setContents(cont);
    }

    /** O que ainda falta pôr (o que o baú precisa ter daqui até o fim). */
    private static Map<Material, Integer> restante(Obra o, Planta p) {
        Map<Material, Integer> m = new EnumMap<>(Material.class);
        List<Planta.Bloco> ordem = p.ordem();
        int de = o.fase == Obra.Fase.ERGUENDO ? o.passo : 0;
        for (int i = de; i < ordem.size(); i++) {
            Planta.Bloco b = ordem.get(i);
            if (b.item() != null) m.merge(b.item(), b.qtd(), Integer::sum);
        }
        return m;
    }

    // =====================================================================
    //  O Construtor trabalhando (a cada meio segundo)
    // =====================================================================

    public void tick() {
        ciclo++;
        if (ciclo % 120 == 0) conferirPlacas();
        for (Colonia c : List.copyOf(colonias.todas())) {
            for (Obra o : List.copyOf(c.obras)) {
                try {
                    tickObra(c, o);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Erro na obra " + o.planta + " da colônia de " + c.nomeDono + ": " + ex);
                }
            }
        }
    }

    private void tickObra(Colonia c, Obra o) {
        if (o.livre) return; // do seu jeito: quem constrói é o jogador
        World w = o.world();
        if (w == null || !w.isChunkLoaded(o.x >> 4, o.z >> 4)) return;
        Planta p = plantas.de(o.planta);
        if (p == null) return;
        if (o.bau == null || ciclo % 4 == 0) o.bau = acharBau(o, p, w);
        Inventory inv = inventario(o.bau);
        if (inv == null) return; // sem baú, o canteiro espera
        Colonia.Cidadao ci = construtor(c, o);
        if (ci == null) return;
        if (!(Bukkit.getEntity(ci.entidade) instanceof Villager v) || !v.isValid()) return;
        long hora = w.getTime();
        if (hora > 12500 && hora < 23500 && !ci.tem(Traco.MADRUGADOR)) return; // de noite dorme
        // Vai até a obra; se ficar preso longe, chega de outro jeito.
        Location entrada = o.entrada(w, p);
        double raio = Math.max(p.largura(), p.profundidade()) / 2.0 + 6;
        Location pe = v.getLocation();
        if (!pe.getWorld().equals(w) || Math.hypot(pe.getX() - (o.x + 0.5), pe.getZ() - (o.z + 0.5)) > raio) {
            if (ciclo % 4 == 0) v.getPathfinder().moveTo(entrada, 0.7);
            if (++o.longe > 40) {
                v.teleport(entrada);
                o.longe = 0;
            }
            return;
        }
        o.longe = 0;
        if (o.fase == Obra.Fase.CANTEIRO) {
            o.fase = Obra.Fase.LIMPANDO;
            o.passo = 0;
            avisarDono(c, Component.text("⌂ " + ci.nome + " (Construtor) começou a obra: " + p.nome() + ".", Prefeituras.COR));
        }
        int forca = 1 + ci.nivel / 4 + (ci.tem(Traco.DEDICACAO) ? 1 : 0) - (ci.tem(Traco.PREGUICA) && rnd().nextBoolean() ? 1 : 0);
        if (c.fatorFelicidade() < 0.8 && rnd().nextDouble() < 0.3) forca = 0;
        if (forca <= 0) return;
        Block ultimo = o.fase == Obra.Fase.LIMPANDO ? limpar(o, p, w, inv, forca * 3) : erguer(c, o, p, w, inv, forca, v, entrada);
        if (ultimo != null) {
            v.swingMainHand();
            v.lookAt(ultimo.getLocation().add(0.5, 0.5, 0.5));
            if (ciclo % 10 == 0) {
                // Anda um pouco em volta, entre a obra e a cerca.
                int[] a = o.area(p, 1);
                int bx = rnd().nextBoolean() ? a[0] : a[2], bz = a[1] + rnd().nextInt(a[3] - a[1] + 1);
                if (rnd().nextBoolean()) { bz = rnd().nextBoolean() ? a[1] : a[3]; bx = a[0] + rnd().nextInt(a[2] - a[0] + 1); }
                Integer chao = chao(w, bx, o.y, bz);
                if (chao != null) v.getPathfinder().moveTo(new Location(w, bx + 0.5, chao + 1, bz + 0.5), 0.6);
            }
            colonias.ganharXp(c, ci, 0.25);
        }
        if (o.fase == Obra.Fase.PRONTA) concluir(c, o, p, ci);
    }

    /** O Construtor da obra (ou um livre, que passa a ser dela). */
    private Colonia.Cidadao construtor(Colonia c, Obra o) {
        if (o.construtor != null) {
            Colonia.Cidadao ci = colonias.cidadao(c, o.construtor);
            if (ci != null && ci.profissao == Profissao.CONSTRUTOR) return ci;
            o.construtor = null;
        }
        Set<UUID> ocupados = new HashSet<>();
        for (Obra outra : c.obras) if (outra.construtor != null) ocupados.add(outra.construtor);
        int postos = c.postos.getOrDefault(Profissao.CONSTRUTOR, 0);
        if (ocupados.size() >= postos) {
            if (c.quantos(Profissao.CONSTRUTOR) > 0) {
                colonias.avisar(c, plugin.settings().colTrabalhoNasConstrucoes
                        ? "Obra parada: todos os Construtores com vaga estão ocupados. Cada nível de Oficina do Construtor dá mais uma vaga."
                        : "Obra parada: cada Construtor precisa de uma bancada de trabalho no território (posto).");
            } else {
                colonias.avisar(c, "Obra parada: dê a profissão Construtor a um morador (posto: bancada de trabalho).");
            }
            return null;
        }
        Colonia.Cidadao melhor = null;
        double perto = Double.MAX_VALUE;
        World w = o.world();
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (ci.profissao != Profissao.CONSTRUTOR || ocupados.contains(ci.entidade)) continue;
            if (!(Bukkit.getEntity(ci.entidade) instanceof Villager v) || !v.getWorld().equals(w)) continue;
            double d = v.getLocation().distanceSquared(o.centro(w));
            if (d < perto) { perto = d; melhor = ci; }
        }
        if (melhor == null) return null;
        o.construtor = melhor.entidade;
        Planta p = plantas.de(o.planta);
        avisarDono(c, Component.text("⌂ " + melhor.nome + " (Construtor) está indo para a obra" + (p == null ? "" : ": " + p.nome()) + ".", Prefeituras.COR));
        return melhor;
    }

    /** Limpa a área de cima para baixo (o que sai vai para o baú da obra). @return o último bloco mexido. */
    private Block limpar(Obra o, Planta p, World w, Inventory inv, int maxTirar) {
        int porCamada = p.largura() * p.profundidade();
        int total = porCamada * (p.altura() - 1);
        int tirados = 0, vistos = 0;
        Block ultimo = null;
        while (o.passo < total && tirados < maxTirar && vistos < 160) {
            int ly = p.altura() - 1 - o.passo / porCamada, resto = o.passo % porCamada;
            int lz = resto / p.largura(), lx = resto % p.largura();
            o.passo++;
            vistos++;
            BlockData alvo = p.em(lx, ly, lz);
            if (alvo == null) continue;
            Block b = o.bloco(w, p, lx, ly, lz);
            Material m = b.getType();
            if (m.isAir()) continue;
            boolean liquido = b.isLiquid();
            if (!alvo.getMaterial().isAir()) {
                if (b.getBlockData().equals(Planta.girar(alvo, o.rot))) continue;
                if (b.isReplaceable() && !liquido) continue; // grama alta, neve: o bloco novo vai por cima
            }
            if (PROTEGIDOS.contains(m) || m.name().endsWith("SHULKER_BOX") || plugin.ehEstacao(b)) continue;
            if (!liquido) for (ItemStack d : b.getDrops()) inv.addItem(d);
            colocados.desmarcar(b);
            b.setType(Material.AIR, false);
            tirados++;
            ultimo = b;
        }
        if (o.passo >= total) {
            o.fase = Obra.Fase.ERGUENDO;
            o.passo = 0;
        }
        return ultimo;
    }

    /** Põe os próximos blocos da planta. @return o último bloco posto. */
    private Block erguer(Colonia c, Obra o, Planta p, World w, Inventory inv, int maxPor, Villager v, Location entrada) {
        List<Planta.Bloco> ordem = p.ordem();
        int postos = 0, vistos = 0;
        Block ultimo = null;
        o.falta = null;
        while (o.passo < ordem.size() && postos < maxPor && vistos < 64) {
            vistos++;
            Planta.Bloco pb = ordem.get(o.passo);
            Block b = o.bloco(w, p, pb.lx(), pb.ly(), pb.lz());
            BlockData alvo = Planta.girar(pb.dados(), o.rot);
            if (b.getBlockData().equals(alvo)) { o.passo++; continue; }
            if (PROTEGIDOS.contains(b.getType()) || plugin.ehEstacao(b)) { o.passo++; continue; } // alguém pôs um baú ali: respeita
            if (pb.item() != null) {
                if (contar(inv, pb.item()) < pb.qtd()) {
                    o.falta = pb.item();
                    o.faltaQtd = restante(o, p).getOrDefault(pb.item(), pb.qtd()) - contar(inv, pb.item());
                    avisarFalta(c, o, p);
                    break;
                }
                tirar(inv, pb.item(), pb.qtd());
            }
            if (!b.getType().isAir() && !b.isReplaceable()) for (ItemStack d : b.getDrops()) inv.addItem(d);
            // Não empareda o Construtor: se ele está bem onde vai o bloco, sai para a entrada.
            if (v.getLocation().getBlock().equals(b) || v.getEyeLocation().getBlock().equals(b)) v.teleport(entrada);
            b.setBlockData(alvo, false);
            colocados.marcar(b); // quebrar a casa não dá XP de atributo
            if (pb.ly() == 0) {
                // Alicerce: tapa buracos logo abaixo do piso.
                for (int k = 1; k <= 4; k++) {
                    Block abaixo = b.getRelative(BlockFace.DOWN, k);
                    if (!abaixo.getType().isAir() && !abaixo.isLiquid() && !abaixo.isReplaceable()) break;
                    abaixo.setType(Material.DIRT, false);
                    colocados.marcar(abaixo);
                }
            }
            o.passo++;
            postos++;
            ultimo = b;
        }
        if (ultimo != null) {
            BlockData bd = ultimo.getBlockData();
            w.spawnParticle(Particle.BLOCK, ultimo.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, bd);
            w.playSound(ultimo.getLocation(), bd.getSoundGroup().getPlaceSound(), 0.7f, 0.9f);
        }
        if (o.passo >= ordem.size()) o.fase = Obra.Fase.PRONTA;
        return ultimo;
    }

    private void avisarFalta(Colonia c, Obra o, Planta p) {
        long agora = System.currentTimeMillis();
        if (agora - o.ultimoAvisoMs < 90_000) return;
        o.ultimoAvisoMs = agora;
        avisarDono(c, Component.text("⌂ A obra (" + p.nome() + ") parou: faltam " + Math.max(1, o.faltaQtd) + "x ", NamedTextColor.GOLD)
                .append(Component.translatable(o.falta.translationKey(), NamedTextColor.WHITE))
                .append(Component.text(" no baú da obra.", NamedTextColor.GOLD)));
    }

    private static void avisarDono(Colonia c, Component msg) {
        Player dono = Bukkit.getPlayer(c.dono);
        if (dono != null) dono.sendMessage(msg);
    }

    /** Liga cercas, painéis e muros da construção aos vizinhos (foram postos sem física). */
    private static void conectar(List<Block> blocos) {
        BlockFace[] lados = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (Block b : blocos) {
            BlockData d = b.getBlockData();
            if (d instanceof Wall muro) {
                int n = 0;
                boolean ns = false, lo = false;
                for (BlockFace f : lados) {
                    boolean liga = ligaEm(b.getRelative(f), true);
                    muro.setHeight(f, liga ? Wall.Height.LOW : Wall.Height.NONE);
                    if (liga) {
                        n++;
                        if (f == BlockFace.NORTH || f == BlockFace.SOUTH) ns = true;
                        else lo = true;
                    }
                }
                boolean reto = n == 2 && (ns != lo);
                muro.setUp(!reto || !b.getRelative(BlockFace.UP).isEmpty());
                b.setBlockData(muro, false);
            } else if (d instanceof Fence || d instanceof GlassPane) {
                MultipleFacing mf = (MultipleFacing) d;
                for (BlockFace f : lados) if (mf.getAllowedFaces().contains(f)) mf.setFace(f, ligaEm(b.getRelative(f), d instanceof Fence));
                b.setBlockData(mf, false);
            }
        }
    }

    private static boolean ligaEm(Block v, boolean cercaOuMuro) {
        BlockData d = v.getBlockData();
        if (v.getType().isOccluding()) return true;
        if (cercaOuMuro) return d instanceof Fence || d instanceof Wall || d instanceof Gate;
        return d instanceof GlassPane || v.getType() == Material.IRON_BARS;
    }

    private void concluir(Colonia c, Obra o, Planta p, Colonia.Cidadao ci) {
        World w = o.world();
        List<Block> ligar = new ArrayList<>();
        for (Planta.Bloco pb : p.ordem()) {
            BlockData d = pb.dados();
            Block b = o.bloco(w, p, pb.lx(), pb.ly(), pb.lz());
            if (d instanceof Fence || d instanceof GlassPane || d instanceof Wall) ligar.add(b);
        }
        conectar(ligar);
        removerCerca(o);
        c.obras.remove(o);
        // O desenho pronto é pelo menos o nível 1; se tiver mais, a vistoria diz.
        Construcao k = new Construcao(p.id(), o.mundo, o.x, o.y, o.z, o.rot, 0, false, 1);
        Vistoria vk = vistoriar(w, p, k.area());
        k.nivel = Math.max(1, vk.nivel(p.niveis()));
        k.camas = vk.camas;
        c.construcoes.add(k);
        colocarPlaca(c, k, p);
        Component extra = aplicarNivel(c, k, p);
        colonias.alterarFelicidade(c, 3);
        colonias.ganharXp(c, ci, 20);
        colonias.varrer(c);
        colonias.salvar();
        Location centro = o.centro(w);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, centro.clone().add(0, 2, 0), 50, 2, 1.5, 2, 0.2);
        w.playSound(centro, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        w.playSound(centro, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        Player dono = Bukkit.getPlayer(c.dono);
        if (dono != null) {
            dono.sendMessage(Component.text("⌂ " + ci.nome + " terminou: " + p.nome() + "!", Prefeituras.COR, TextDecoration.BOLD).append(extra));
            dono.showTitle(Title.title(Component.text("⌂ " + p.nome() + " pronta! ⌂", Prefeituras.COR, TextDecoration.BOLD),
                    Component.text("Obra de " + ci.nome, NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
    }

    /** O morador saiu da colônia: a obra dele fica sem Construtor (outro pega). */
    void liberar(UUID cidadao) {
        for (Colonia c : colonias.todas()) for (Obra o : c.obras) if (cidadao.equals(o.construtor)) o.construtor = null;
    }

    /** A colônia acabou: as cercas somem (o que já foi construído fica). */
    void encerrar(Colonia c) {
        for (Obra o : c.obras) removerCerca(o);
        c.obras.clear();
    }

    /** A soma dos níveis das construções prontas desse tipo. */
    int niveis(Colonia c, Planta.Efeito e) {
        int n = 0;
        for (Construcao k : c.construcoes) {
            Planta p = plantas.de(k.planta());
            if (p != null && p.efeito() == e) n += k.nivel;
        }
        return n;
    }

    /** Bônus de produção de quem trabalha com uma construção da colônia: +10% por nível da melhor delas. */
    double bonus(Colonia c, Profissao prof) {
        int melhor = 0;
        for (Construcao k : c.construcoes) {
            Planta p = plantas.de(k.planta());
            if (p != null && p.efeito().trabalhador() == prof) melhor = Math.max(melhor, k.nivel);
        }
        return 1 + 0.1 * melhor;
    }

    // =====================================================================
    //  A placa da construção (o "bloco do prédio")
    // =====================================================================

    private Colonia coloniaDa(Construcao k) {
        for (Colonia c : colonias.todas()) if (c.construcoes.contains(k)) return c;
        return null;
    }

    /** Uma construção (carregada do arquivo) entra no índice das placas. */
    void indexar(Construcao k) {
        if (k.placa != null) placas.put(chave(k.mundo, k.placa[0], k.placa[1], k.placa[2]), k);
    }

    private boolean temPlaca(Construcao k) {
        if (k.placa == null) return false;
        World w = Bukkit.getWorld(k.mundo);
        if (w == null || !w.isChunkLoaded(k.placa[0] >> 4, k.placa[2] >> 4)) return true; // longe: não dá para saber, confia
        return Tag.SIGNS.isTagged(w.getBlockAt(k.placa[0], k.placa[1], k.placa[2]).getType());
    }

    /**
     * Põe a placa no chão, do lado de dentro e perto da entrada, virada para ela.
     * @return false se não achou lugar.
     */
    private boolean colocarPlaca(Colonia c, Construcao k, Planta p) {
        World w = Bukkit.getWorld(k.mundo);
        if (w == null) return false;
        if (k.placa != null) placas.remove(chave(k.mundo, k.placa[0], k.placa[1], k.placa[2]));
        k.placa = null;
        Obra area = k.area();
        int meio = p.largura() / 2;
        Block achado = null;
        // Da frente para o fundo, do meio para os lados.
        for (int lz = p.profundidade() - 2 + k.extra; lz >= 1 - k.extra && achado == null; lz--) {
            for (int d = 0; d <= meio + k.extra && achado == null; d++) {
                for (int lx : new int[]{meio - d, meio + d}) {
                    for (int ly = 1; ly <= 3 && achado == null; ly++) {
                        Block b = area.bloco(w, p, lx, ly, lz);
                        Block baixo = b.getRelative(BlockFace.DOWN);
                        if ((b.getType().isAir() || (b.isReplaceable() && !b.isLiquid())) && baixo.getType().isSolid() && !Tag.SIGNS.isTagged(baixo.getType())) {
                            achado = b;
                        }
                    }
                }
            }
        }
        if (achado == null) return false;
        org.bukkit.block.data.type.Sign dados = (org.bukkit.block.data.type.Sign) Material.OAK_SIGN.createBlockData();
        dados.setRotation(Planta.face(BlockFace.SOUTH, k.rot));
        achado.setBlockData(dados, false);
        k.placa = new int[]{achado.getX(), achado.getY(), achado.getZ()};
        indexar(k);
        escreverPlaca(c, k, p);
        return true;
    }

    private void escreverPlaca(Colonia c, Construcao k, Planta p) {
        if (k.placa == null) return;
        World w = Bukkit.getWorld(k.mundo);
        if (w == null || !w.isChunkLoaded(k.placa[0] >> 4, k.placa[2] >> 4)) return;
        if (!(w.getBlockAt(k.placa[0], k.placa[1], k.placa[2]).getState() instanceof org.bukkit.block.Sign placa)) return;
        var lado = placa.getSide(org.bukkit.block.sign.Side.FRONT);
        lado.line(0, Component.text("⌂ " + p.nome(), NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
        lado.line(1, Component.text(k.nivel == 0 ? "precisa de reparo" : "Nível " + k.nivel + " / " + p.nivelMaximo(),
                k.nivel == 0 ? NamedTextColor.DARK_RED : NamedTextColor.BLACK));
        lado.line(2, Component.text("de " + c.nomeDono, NamedTextColor.DARK_GRAY));
        lado.line(3, Component.text("(clique: menu)", NamedTextColor.GRAY));
        placa.setWaxed(true);
        placa.update(true, false);
    }

    /** A cada minuto: placa que sumiu (o chão saiu de baixo, alguém tirou...) deixa a construção precisando de reparo. */
    private void conferirPlacas() {
        for (Colonia c : colonias.todas()) {
            for (Construcao k : c.construcoes) {
                if (k.nivel <= 0 || temPlaca(k)) continue;
                if (k.placa != null) placas.remove(chave(k.mundo, k.placa[0], k.placa[1], k.placa[2]));
                k.placa = null;
                k.nivel = 0;
                Planta p = plantas.de(k.planta);
                avisarDono(c, Component.text("⌂ A placa da construção " + (p == null ? k.planta : p.nome()) + " sumiu: ela precisa de reparo. "
                        + "Peça a vistoria em Obras para pôr outra.", NamedTextColor.GOLD));
                colonias.salvar();
            }
        }
    }

    /** O posto (x, y, z) está dentro de uma construção pronta desse tipo? */
    boolean postoNaConstrucao(Colonia c, Planta.Efeito tipo, int x, int y, int z) {
        for (Construcao k : c.construcoes) {
            if (k.nivel <= 0 || !k.mundo.equals(c.mundo)) continue;
            Planta p = plantas.de(k.planta);
            if (p == null || p.efeito() != tipo) continue;
            Obra area = k.area();
            int topo = k.livre ? alturaVistoria(p) : p.altura() + 2;
            if (area.dentro(p, x, z, 0) && y >= k.y && y < k.y + topo) return true;
        }
        return false;
    }

    /** A construção deixa de ser da colônia: a placa sai e o que ela dava acaba. */
    private void removerConstrucao(Colonia c, Construcao k, boolean tirarPlaca) {
        Planta p = plantas.de(k.planta);
        World w = Bukkit.getWorld(k.mundo);
        if (k.placa != null) {
            placas.remove(chave(k.mundo, k.placa[0], k.placa[1], k.placa[2]));
            if (tirarPlaca && w != null) {
                Block b = w.getBlockAt(k.placa[0], k.placa[1], k.placa[2]);
                if (Tag.SIGNS.isTagged(b.getType())) b.setType(Material.AIR, false);
            }
        }
        c.construcoes.remove(k);
        if (p != null && w != null) {
            Obra area = k.area();
            if (p.efeito() == Planta.Efeito.TORRE_DE_VIGIA) {
                int n = plugin.territorios().removerTorre(c.dono, area.centro(w));
                if (n > 0) avisarDono(c, Component.text("⌂ Sem a torre, " + n + " chunks saíram do território.", NamedTextColor.GOLD));
            } else if (p.efeito() == Planta.Efeito.ARMAZEM) {
                c.depositos.removeIf(d -> d.getWorld() != null && d.getWorld().getName().equals(k.mundo)
                        && area.dentro(p, d.getBlockX(), d.getBlockZ(), 0) && d.getBlockY() >= k.y && d.getBlockY() < k.y + alturaVistoria(p));
            }
        }
        colonias.varrer(c);
        colonias.salvar();
    }

    // =====================================================================
    //  Moradia: cada morador numa Casa
    // =====================================================================

    /** Camas de cada Casa (posição da cabeceira), achadas no último turno. */
    private final Map<String, List<Location>> camasDasCasas = new HashMap<>();

    /** Vagas de Construtor: 1 de graça (para erguer a primeira Oficina) + o nível de cada Oficina pronta. */
    int vagasConstrutor(Colonia c) {
        return 1 + niveis(c, Planta.Efeito.OFICINA);
    }

    private List<Construcao> casas(Colonia c) {
        List<Construcao> l = new ArrayList<>();
        for (Construcao k : c.construcoes) {
            Planta p = plantas.de(k.planta);
            if (p != null && p.efeito() == Planta.Efeito.CASA && k.nivel >= 1) l.add(k);
        }
        return l;
    }

    Construcao casaDe(Colonia c, Colonia.Cidadao ci) {
        if (ci.casa == null) return null;
        for (Construcao k : casas(c)) if (k.chave().equals(ci.casa)) return k;
        return null;
    }

    /** Quantos moradores cabem nas Casas (as camas delas). */
    int vagasCasas(Colonia c) {
        int n = 0;
        for (Construcao k : casas(c)) n += Math.max(0, k.camas);
        return n;
    }

    /** As cabeceiras das camas de uma casa, numa ordem fixa. */
    private List<Location> acharCamas(Construcao k, Planta p) {
        List<Location> l = new ArrayList<>();
        World w = Bukkit.getWorld(k.mundo);
        if (w == null || !w.isChunkLoaded(k.x >> 4, k.z >> 4)) return l;
        Obra area = k.area();
        int[] a = area.area(p, 0);
        int topo = k.livre ? alturaVistoria(p) : p.altura() + 2;
        for (int x = a[0]; x <= a[2]; x++) {
            for (int z = a[1]; z <= a[3]; z++) {
                for (int ly = 0; ly < topo; ly++) {
                    Block b = w.getBlockAt(x, k.y + ly, z);
                    if (b.getBlockData() instanceof org.bukkit.block.data.type.Bed cama && cama.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD) {
                        l.add(b.getLocation());
                    }
                }
            }
        }
        return l;
    }

    /**
     * A cada turno: quem perdeu a casa (desfeita, cheia, sem camas) fica sem; quem está sem ganha
     * uma com vaga (como no MineColonies, a colônia distribui sozinha). Guarda as camas de cada casa.
     */
    void distribuirCasas(Colonia c) {
        Map<String, Integer> livres = new LinkedHashMap<>();
        camasDasCasas.keySet().removeIf(k -> k.startsWith(c.dono + "|"));
        for (Construcao k : casas(c)) {
            Planta p = plantas.de(k.planta);
            List<Location> camas = p == null ? List.of() : acharCamas(k, p);
            if (!camas.isEmpty()) {
                k.camas = camas.size();
                camasDasCasas.put(c.dono + "|" + k.chave(), camas);
            }
            livres.put(k.chave(), Math.max(0, k.camas));
        }
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (ci.profissao.soldado()) { ci.casa = null; continue; }
            if (ci.casa == null) continue;
            Integer vagas = livres.get(ci.casa);
            if (vagas == null || vagas <= 0) ci.casa = null;
            else livres.put(ci.casa, vagas - 1);
        }
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (ci.casa != null || ci.profissao.soldado()) continue;
            for (Map.Entry<String, Integer> e : livres.entrySet()) {
                if (e.getValue() <= 0) continue;
                ci.casa = e.getKey();
                e.setValue(e.getValue() - 1);
                break;
            }
        }
    }

    /** A cama do morador na casa dele (a IA do aldeão dorme nela), ou null. */
    Location camaDe(Colonia c, Colonia.Cidadao ci) {
        if (ci.casa == null) return null;
        List<Location> camas = camasDasCasas.get(c.dono + "|" + ci.casa);
        if (camas == null || camas.isEmpty()) return null;
        int i = 0;
        for (Colonia.Cidadao outro : c.cidadaos) {
            if (outro == ci) break;
            if (ci.casa.equals(outro.casa)) i++;
        }
        return i < camas.size() ? camas.get(i) : null;
    }

    /** Quem mora nessa casa. */
    List<Colonia.Cidadao> moradores(Colonia c, Construcao k) {
        List<Colonia.Cidadao> l = new ArrayList<>();
        for (Colonia.Cidadao ci : c.cidadaos) if (k.chave().equals(ci.casa)) l.add(ci);
        return l;
    }

    // =====================================================================
    //  Vistoria e níveis
    // =====================================================================

    private static long chaveBloco(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    /** Conta o que tem na área (da planta, ou maior nas do seu jeito). */
    Vistoria vistoriar(World w, Planta p, Obra area) {
        int[] a = area.area(p, 0);
        int topo = area.livre ? alturaVistoria(p) : p.altura() + 2;
        Set<Long> marcados = new HashSet<>();
        for (int cx = a[0] >> 4; cx <= a[2] >> 4; cx++) {
            for (int cz = a[1] >> 4; cz <= a[3] >> 4; cz++) {
                if (!w.isChunkLoaded(cx, cz)) continue;
                for (Block b : colocados.todos(w.getChunkAt(cx, cz))) marcados.add(chaveBloco(b.getX(), b.getY(), b.getZ()));
            }
        }
        Vistoria v = new Vistoria();
        for (int x = a[0]; x <= a[2]; x++) {
            for (int z = a[1]; z <= a[3]; z++) {
                boolean coberta = false;
                for (int ly = 0; ly < topo; ly++) {
                    Block b = w.getBlockAt(x, area.y + ly, z);
                    Material m = b.getType();
                    if (m.isAir()) continue;
                    boolean colocado = !Vistoria.natural(m) || marcados.contains(chaveBloco(b.getX(), b.getY(), b.getZ()));
                    v.contar(ly, b.getBlockData(), colocado);
                    if (ly >= 2 && m.isSolid() && !Tag.LEAVES.isTagged(m)) coberta = true;
                }
                if (x != a[0] && x != a[2] && z != a[1] && z != a[3]) v.coluna(coberta);
            }
        }
        return v;
    }

    /** O que o nível da construção dá (torre: mais território; armazém: baús viram depósito). @return a frase para o dono. */
    private Component aplicarNivel(Colonia c, Construcao k, Planta p) {
        World w = Bukkit.getWorld(k.mundo);
        if (w == null || k.nivel <= 0) return Component.empty();
        Obra area = k.area();
        switch (p.efeito()) {
            case TORRE_DE_VIGIA -> {
                int raio = Math.min(3, plugin.settings().terRaioTorre + k.nivel - 1);
                int n = plugin.territorios().expandirPorTorre(c.dono, area.centro(w), raio);
                int lado = 2 * raio + 1;
                return Component.text(" A torre vigia " + lado + "×" + lado + " chunks" + (n > 0 ? " (+" + n + " no território)." : "."), NamedTextColor.GREEN);
            }
            case ARMAZEM -> {
                int[] a = area.area(p, 0);
                int n = 0, topo = area.livre ? alturaVistoria(p) : p.altura();
                for (int x = a[0]; x <= a[2]; x++) {
                    for (int z = a[1]; z <= a[3]; z++) {
                        for (int ly = 0; ly < topo; ly++) {
                            Block b = w.getBlockAt(x, area.y + ly, z);
                            Material m = b.getType();
                            if (m != Material.CHEST && m != Material.TRAPPED_CHEST && m != Material.BARREL) continue;
                            boolean ja = false;
                            for (Location d : c.depositos) if (d.getBlock().equals(b)) ja = true;
                            if (!ja) { c.depositos.add(b.getLocation()); n++; }
                        }
                    }
                }
                return Component.text(n > 0 ? " " + n + " baús viraram depósito da colônia." : "", NamedTextColor.GREEN);
            }
            case PRACA -> { return Component.text(" A colônia ficou mais feliz.", NamedTextColor.GREEN); }
            case FAZENDA, BIBLIOTECA -> {
                return Component.text(" " + p.efeito().trabalhador().nome() + " produz +" + 10 * k.nivel + "%.", NamedTextColor.GREEN);
            }
            default -> { return Component.empty(); }
        }
    }

    /**
     * Pede a vistoria: numa área do seu jeito que atende o nível 1, ela vira construção da colônia;
     * numa construção pronta, o nível sobe (ou desce) conforme o que tem lá agora.
     */
    private void pedirVistoria(Player jogador, Colonia c, Obra o, Construcao k) {
        Planta p = plantas.de(o != null ? o.planta : k.planta);
        World w = Bukkit.getWorld(o != null ? o.mundo : k.mundo);
        if (p == null || w == null) return;
        Obra area = o != null ? o : k.area();
        int[] a = area.area(p, 0);
        for (int cx = a[0] >> 4; cx <= a[2] >> 4; cx++) {
            for (int cz = a[1] >> 4; cz <= a[3] >> 4; cz++) {
                if (!w.isChunkLoaded(cx, cz)) { Colonias.erro(jogador, "Chegue mais perto da construção para a vistoria."); return; }
            }
        }
        Vistoria v = vistoriar(w, p, area);
        int nivel = v.nivel(p.niveis());
        if (p.niveis().isEmpty()) nivel = 1;
        if (o != null) {
            if (nivel < 1) {
                Colonias.erro(jogador, "✖ Ainda não atende o mínimo do nível 1. Veja no menu o que falta.");
                return;
            }
            Construcao nova = new Construcao(p.id(), o.mundo, o.x, o.y, o.z, o.rot, o.extra, true, nivel);
            nova.camas = v.camas;
            if (!colocarPlaca(c, nova, p)) {
                Colonias.erro(jogador, "✖ Atende o nível " + nivel + ", mas falta lugar para a placa da construção: deixe 1 bloco livre no chão, "
                        + "do lado de dentro, perto da entrada.");
                return;
            }
            removerCerca(o);
            c.obras.remove(o);
            c.construcoes.add(nova);
            Component extra = aplicarNivel(c, nova, p);
            colonias.alterarFelicidade(c, 3);
            colonias.varrer(c);
            colonias.salvar();
            Location centro = area.centro(w);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, centro.clone().add(0, 2, 0), 50, 2, 1.5, 2, 0.2);
            w.playSound(centro, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.1f);
            jogador.sendMessage(Component.text("⌂ Vistoria aprovada: " + p.nome() + " (nível " + nivel + "/" + p.nivelMaximo() + "), feita do seu jeito!",
                    Prefeituras.COR, TextDecoration.BOLD).append(extra));
            jogador.showTitle(Title.title(Component.text("⌂ " + p.nome() + " aprovada! ⌂", Prefeituras.COR, TextDecoration.BOLD),
                    Component.text("Nível " + nivel, NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
            return;
        }
        int antes = k.nivel;
        // Sem a placa a construção não vale: a vistoria põe ela de volta (se tiver lugar).
        if (!temPlaca(k) && !colocarPlaca(c, k, p)) {
            Colonias.erro(jogador, "✖ A placa da construção sumiu e não há lugar para pôr outra: deixe 1 bloco livre no chão perto da entrada.");
            nivel = 0;
        }
        k.nivel = nivel;
        k.camas = v.camas;
        escreverPlaca(c, k, p);
        colonias.salvar();
        if (nivel > antes) {
            Component extra = aplicarNivel(c, k, p);
            Location centro = area.centro(w);
            w.spawnParticle(Particle.HAPPY_VILLAGER, centro.clone().add(0, 2, 0), 40, 2, 1.5, 2, 0);
            w.playSound(centro, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            jogador.sendMessage(Component.text("⌂ " + p.nome() + " subiu para o nível " + nivel + "/" + p.nivelMaximo() + "!", Prefeituras.COR, TextDecoration.BOLD)
                    .append(extra));
        } else if (nivel < antes) {
            jogador.sendMessage(Component.text("⌂ " + p.nome() + " caiu para o nível " + nivel + (nivel == 0 ? " (precisa de reparo: não conta para nada)" : "")
                    + ". Veja no menu o que falta.", NamedTextColor.RED));
        } else {
            jogador.sendMessage(Component.text("⌂ " + p.nome() + " continua no nível " + nivel + "/" + p.nivelMaximo()
                    + (nivel < p.nivelMaximo() ? ". Veja no menu o que falta para o próximo." : " (máximo)."), Prefeituras.COR));
        }
    }

    // =====================================================================
    //  Menus
    // =====================================================================

    /** Os projetos que dá para pegar na Prefeitura. */
    void abrirCatalogo(Player p, Colonia c) {
        Tela t = new Tela(Tipo.CATALOGO, c);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⌂ Projetos de construção"));
        Inventory inv = t.inventario;
        inv.setItem(L_INFO, Colonias.item(Material.WRITABLE_BOOK, Component.text("Como construir", Prefeituras.COR, TextDecoration.BOLD), List.of(
                Component.text("Clique: projeto pronto. O Construtor ergue", NamedTextColor.GRAY),
                Component.text("  o desenho com o material de um baú.", NamedTextColor.GRAY),
                Component.text("Botão direito: do seu jeito. Você constrói", NamedTextColor.GRAY),
                Component.text("  na área e pede a vistoria: precisa ter o", NamedTextColor.GRAY),
                Component.text("  mínimo do nível 1.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Com o projeto, clique no chão: o lado em", NamedTextColor.GRAY),
                Component.text("que você está vira a ENTRADA e aparece uma", NamedTextColor.GRAY),
                Component.text("cerca. Agachado + clique nela abre o menu.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Toda construção tem níveis: melhore e peça", NamedTextColor.GRAY),
                Component.text("a vistoria de novo em Obras.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Obras ao mesmo tempo: " + c.obras.size() + " / " + maxObras(c), NamedTextColor.WHITE)), false));
        int i = 0;
        for (Planta pl : plantas.todas()) {
            if (i >= SLOTS_LISTA.length) break;
            boolean liberada = c.nivel >= pl.nivel();
            List<Component> lore = new ArrayList<>();
            for (String d : pl.descricao()) lore.add(Component.text(d, NamedTextColor.GRAY));
            lore.add(Colonias.linha("Tamanho: ", pl.largura() + "×" + pl.profundidade() + ", altura " + pl.altura(), NamedTextColor.WHITE));
            lore.add(Component.text(pl.efeito().texto(), NamedTextColor.AQUA));
            lore.add(Colonias.linha("Níveis: ", String.valueOf(pl.nivelMaximo()), NamedTextColor.WHITE));
            int prontas = 0;
            for (Construcao k : c.construcoes) if (k.planta.equals(pl.id())) prontas++;
            if (prontas > 0) lore.add(Colonias.linha("Prontas na colônia: ", String.valueOf(prontas), NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text("Materiais do projeto pronto:", NamedTextColor.GRAY));
            lore.addAll(listaMateriais(pl.materiais(), 6));
            if (!pl.niveis().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Do seu jeito, o mínimo (nível 1):", NamedTextColor.GOLD));
                lore.addAll(linhasRequisito(pl.niveis().get(0), null));
            }
            lore.add(Component.empty());
            if (!liberada) lore.add(Component.text("✖ Colônia nível " + pl.nivel(), NamedTextColor.RED));
            else {
                lore.add(Component.text("» Clique: projeto pronto (Construtor)", NamedTextColor.YELLOW));
                if (!pl.niveis().isEmpty()) lore.add(Component.text("» Botão direito: do seu jeito (você)", NamedTextColor.YELLOW));
            }
            inv.setItem(SLOTS_LISTA[i], Colonias.item(liberada ? pl.icone() : Material.GRAY_DYE,
                    Component.text(pl.nome(), liberada ? Prefeituras.COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore, false));
            t.ids.add(pl.id());
            i++;
        }
        inv.setItem(VOLTAR, Colonias.item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        Colonias.preencher(inv);
        p.openInventory(inv);
    }

    private static List<Component> listaMateriais(Map<Material, Integer> mats, int max) {
        List<Map.Entry<Material, Integer>> l = new ArrayList<>(mats.entrySet());
        l.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Component> r = new ArrayList<>();
        for (int i = 0; i < l.size() && i < max; i++) {
            r.add(Component.text(" " + l.get(i).getValue() + "x ", NamedTextColor.WHITE)
                    .append(Component.translatable(l.get(i).getKey().translationKey(), NamedTextColor.GRAY)));
        }
        if (l.size() > max) r.add(Component.text(" ... e mais " + (l.size() - max), NamedTextColor.DARK_GRAY));
        return r;
    }

    /** As linhas de um nível; com a vistoria, cada uma vem com ✔ / ✖ e o que tem. */
    private static List<Component> linhasRequisito(Vistoria.Requisito r, Vistoria v) {
        List<Component> l = new ArrayList<>();
        for (Vistoria.Linha x : r.linhas(v == null ? new Vistoria() : v)) {
            Component nome = x.bloco() == null ? Component.text(x.nome()) : Component.translatable(x.bloco().translationKey());
            if (v == null) {
                l.add(Component.text(" " + x.minimo() + " · ", NamedTextColor.WHITE).append(nome.color(NamedTextColor.GRAY)));
            } else {
                boolean ok = x.ok();
                l.add(Component.text(ok ? " ✔ " : " ✖ ", ok ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(nome.color(NamedTextColor.GRAY))
                        .append(Component.text(": " + x.tem() + " / " + x.minimo(), ok ? NamedTextColor.GREEN : NamedTextColor.RED)));
            }
        }
        return l;
    }

    /** As obras em andamento e as construções prontas. */
    void abrirLista(Player p, Colonia c) {
        Tela t = new Tela(Tipo.LISTA, c);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⌂ Obras da colônia"));
        desenharLista(t);
        p.openInventory(t.inventario);
    }

    private void desenharLista(Tela t) {
        Colonia c = t.colonia;
        Inventory inv = t.inventario;
        inv.clear();
        t.ids.clear();
        List<Component> info = new ArrayList<>();
        info.add(Colonias.linha("Obras: ", c.obras.size() + " / " + maxObras(c), NamedTextColor.WHITE));
        info.add(Colonias.linha("Construções prontas: ", String.valueOf(c.construcoes.size()), NamedTextColor.GREEN));
        info.add(Colonias.linha("Construtores: ", c.quantos(Profissao.CONSTRUTOR) + "  ·  bancadas: " + c.postos.getOrDefault(Profissao.CONSTRUTOR, 0),
                NamedTextColor.WHITE));
        info.add(Component.empty());
        info.add(Component.text("Primeiro as obras, depois as prontas.", NamedTextColor.GRAY));
        info.add(Component.text("Numa pronta: vistoria para subir de nível.", NamedTextColor.GRAY));
        inv.setItem(L_INFO, Colonias.item(Material.BRICKS, Component.text("Obras e construções", Prefeituras.COR, TextDecoration.BOLD), info, false));
        int slot = 0;
        for (Obra o : c.obras) {
            if (slot >= SLOTS_LISTA.length) break;
            Planta pl = plantas.de(o.planta);
            List<Component> lore = new ArrayList<>(resumo(c, o, pl));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para ver a obra", NamedTextColor.YELLOW));
            inv.setItem(SLOTS_LISTA[slot++], Colonias.item(pl == null ? Material.BARRIER : pl.icone(),
                    Component.text((o.livre ? "📐 " : "⚒ ") + (pl == null ? o.planta : pl.nome()), Prefeituras.COR, TextDecoration.BOLD), lore, false));
            t.ids.add("o:" + o.id);
        }
        for (int i = 0; i < c.construcoes.size() && slot < SLOTS_LISTA.length; i++) {
            Construcao k = c.construcoes.get(i);
            Planta pl = plantas.de(k.planta);
            List<Component> lore = new ArrayList<>();
            lore.add(Colonias.linha("Nível: ", k.nivel + " / " + (pl == null ? "?" : pl.nivelMaximo()) + (k.nivel == 0 ? " (precisa de reparo)" : ""),
                    k.nivel == 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
            lore.add(Colonias.linha("Feita: ", k.livre ? "do seu jeito" : "pelo Construtor", NamedTextColor.WHITE));
            lore.add(Colonias.linha("Onde: ", k.x + ", " + k.y + ", " + k.z, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique: vistoria e o que falta", NamedTextColor.YELLOW));
            inv.setItem(SLOTS_LISTA[slot++], Colonias.item(pl == null ? Material.BARRIER : pl.icone(),
                    Component.text("✔ " + (pl == null ? k.planta : pl.nome()), NamedTextColor.GREEN, TextDecoration.BOLD), lore, false));
            t.ids.add("k:" + i);
        }
        inv.setItem(VOLTAR, Colonias.item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        Colonias.preencher(inv);
    }

    private static int progresso(Obra o, Planta p) {
        return switch (o.fase) {
            case CANTEIRO -> 0;
            case LIMPANDO -> Math.min(10, o.passo * 10 / Math.max(1, p.largura() * p.profundidade() * (p.altura() - 1)));
            case ERGUENDO -> 10 + o.passo * 90 / Math.max(1, p.ordem().size());
            case PRONTA -> 100;
        };
    }

    /** As linhas de estado de uma obra. */
    private List<Component> resumo(Colonia c, Obra o, Planta p) {
        List<Component> l = new ArrayList<>();
        if (p == null) {
            l.add(Component.text("Projeto sumiu (plantas/" + o.planta + ".yml).", NamedTextColor.RED));
            return l;
        }
        int[] a = o.area(p, 0);
        l.add(Colonias.linha("Área: ", (a[2] - a[0] + 1) + "×" + (a[3] - a[1] + 1), NamedTextColor.WHITE));
        l.add(Colonias.linha("Onde: ", o.x + ", " + o.y + ", " + o.z, NamedTextColor.GRAY));
        if (o.livre) {
            l.add(Colonias.linha("Quem constrói: ", "você, do seu jeito", NamedTextColor.WHITE));
            l.add(Component.text("Atenda o mínimo do nível 1 e peça a vistoria.", NamedTextColor.GRAY));
            return l;
        }
        World w = o.world();
        if (o.bau == null && w != null && w.isChunkLoaded(o.x >> 4, o.z >> 4)) o.bau = acharBau(o, p, w);
        String fase = switch (o.fase) {
            case CANTEIRO -> "Canteiro (ainda dá para girar e mudar)";
            case LIMPANDO -> "Limpando o terreno";
            case ERGUENDO -> "Erguendo";
            case PRONTA -> "Pronta";
        };
        l.add(Colonias.linha("Fase: ", fase, NamedTextColor.WHITE));
        l.add(Colonias.linha("Progresso: ", progresso(o, p) + "%", NamedTextColor.GREEN));
        Colonia.Cidadao ci = o.construtor == null ? null : colonias.cidadao(c, o.construtor);
        l.add(Colonias.linha("Construtor: ", ci == null ? "esperando um livre" : ci.nome + " (nível " + ci.nivel + ")",
                ci == null ? NamedTextColor.RED : NamedTextColor.WHITE));
        l.add(Colonias.linha("Baú da obra: ", o.bau == null ? "falta (dentro da cerca, fora da obra)" : "ok", o.bau == null ? NamedTextColor.RED : NamedTextColor.GREEN));
        if (o.falta != null) {
            l.add(Component.text("Parou: faltam " + Math.max(1, o.faltaQtd) + "x ", NamedTextColor.GOLD)
                    .append(Component.translatable(o.falta.translationKey(), NamedTextColor.WHITE)));
        }
        return l;
    }

    void abrirObra(Player p, Colonia c, Obra o) {
        Tela t = new Tela(Tipo.OBRA, c);
        t.obra = o.id;
        Planta pl = plantas.de(o.planta);
        t.inventario = Bukkit.createInventory(t, 54, Component.text((o.livre ? "⌂ Do seu jeito: " : "⌂ Obra: ") + (pl == null ? o.planta : pl.nome())));
        desenharObra(t, o);
        p.openInventory(t.inventario);
    }

    private void desenharObra(Tela t, Obra o) {
        Colonia c = t.colonia;
        Inventory inv = t.inventario;
        inv.clear();
        Planta p = plantas.de(o.planta);
        inv.setItem(O_INFO, Colonias.item(p == null ? Material.BARRIER : p.icone(),
                Component.text(p == null ? o.planta : p.nome(), Prefeituras.COR, TextDecoration.BOLD), resumo(c, o, p), false));
        if (p != null && o.livre) {
            // Do seu jeito: o que já tem na área, nível por nível.
            World w = o.world();
            Vistoria v = w != null && w.isChunkLoaded(o.x >> 4, o.z >> 4) ? vistoriar(w, p, o) : null;
            desenharNiveis(inv, p, v, v == null ? 0 : v.nivel(p.niveis()));
        } else if (p != null) {
            Inventory bau = inventario(o.bau);
            List<Map.Entry<Material, Integer>> falta = new ArrayList<>(restante(o, p).entrySet());
            falta.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            inv.setItem(13, Colonias.item(Material.CHEST, Component.text("Materiais que faltam pôr", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    Component.text("Verde: o baú da obra já tem tudo.", NamedTextColor.GRAY),
                    Component.text("Vermelho: falta pôr no baú.", NamedTextColor.GRAY)), false));
            for (int i = 0; i < falta.size() && i < SLOTS_MATERIAIS.length; i++) {
                Material m = falta.get(i).getKey();
                int precisa = falta.get(i).getValue(), tem = contar(bau, m);
                boolean ok = tem >= precisa;
                ItemStack icone = Colonias.item(m.isItem() ? m : Material.PAPER, Component.translatable(m.translationKey(),
                        ok ? NamedTextColor.GREEN : NamedTextColor.RED), List.of(
                        Colonias.linha("Precisa: ", String.valueOf(precisa), NamedTextColor.WHITE),
                        Colonias.linha("No baú: ", String.valueOf(tem), ok ? NamedTextColor.GREEN : NamedTextColor.RED)), false);
                icone.setAmount(Math.max(1, Math.min(64, precisa)));
                inv.setItem(SLOTS_MATERIAIS[i], icone);
            }
            if (falta.size() > SLOTS_MATERIAIS.length) {
                inv.setItem(35, Colonias.item(Material.PAPER, Component.text("... e mais " + (falta.size() - SLOTS_MATERIAIS.length) + " tipos", NamedTextColor.GRAY),
                        List.of(), false));
            }
        }
        boolean podeMexer = !o.comecou();
        inv.setItem(O_GIRAR, Colonias.item(podeMexer ? Material.COMPASS : Material.GRAY_DYE, Component.text("Girar 90°", podeMexer ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY),
                List.of(Component.text(podeMexer ? "A entrada passa para o lado seguinte." : "A obra já começou.", NamedTextColor.GRAY)), false));
        inv.setItem(O_MOVER, Colonias.item(podeMexer ? Material.PAPER : Material.GRAY_DYE, Component.text("Mudar de lugar", podeMexer ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY),
                List.of(Component.text(podeMexer ? "Tira a cerca e devolve o projeto." : "A obra já começou.", NamedTextColor.GRAY)), false));
        inv.setItem(O_CONTORNO, Colonias.item(Material.SPYGLASS, Component.text("Mostrar o contorno", NamedTextColor.YELLOW),
                List.of(Component.text("Partículas no tamanho da área.", NamedTextColor.GRAY)), false));
        if (o.livre) {
            inv.setItem(O_TRAZER, Colonias.item(Material.WRITABLE_BOOK, Component.text("✔ Pedir a vistoria", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                    Component.text("Confere o que você construiu na área.", NamedTextColor.GRAY),
                    Component.text("Atendeu o nível 1: vira construção da", NamedTextColor.GRAY),
                    Component.text("colônia (e a cerca some).", NamedTextColor.GRAY)), true));
            int[] a = p == null ? new int[]{0, 0, 0, 0} : o.area(p, 0);
            inv.setItem(O_AREA_MENOS, Colonias.item(Material.RED_STAINED_GLASS_PANE, Component.text("Área menor", NamedTextColor.YELLOW), List.of(
                    Component.text("Agora: " + (a[2] - a[0] + 1) + "×" + (a[3] - a[1] + 1), NamedTextColor.GRAY),
                    Component.text("O menor é o tamanho do projeto.", NamedTextColor.DARK_GRAY)), false));
            inv.setItem(O_AREA_MAIS, Colonias.item(Material.LIME_STAINED_GLASS_PANE, Component.text("Área maior", NamedTextColor.YELLOW), List.of(
                    Component.text("+1 bloco de cada lado (até +" + EXTRA_MAX + ").", NamedTextColor.GRAY)), false));
        } else {
            inv.setItem(O_TRAZER, Colonias.item(Material.HOPPER, Component.text("Trazer do depósito", NamedTextColor.YELLOW), List.of(
                    Component.text("Passa o que falta (e que o depósito", NamedTextColor.GRAY),
                    Component.text("da colônia tiver) para o baú da obra.", NamedTextColor.GRAY)), false));
        }
        inv.setItem(O_VOLTAR, Colonias.item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(O_CANCELAR, Colonias.item(Material.RED_DYE, Component.text("Cancelar a obra", NamedTextColor.RED), List.of(
                Component.text("Tira a cerca e devolve o projeto.", NamedTextColor.GRAY),
                Component.text("O que já foi construído fica.", NamedTextColor.GRAY),
                Component.text("Shift + clique para confirmar.", NamedTextColor.DARK_GRAY)), false));
        Colonias.preencher(inv);
    }

    /** Um item por nível (19, 20, 21...), com o que cada um pede; com a vistoria, o que já tem. */
    private void desenharNiveis(Inventory inv, Planta p, Vistoria v, int atual) {
        inv.setItem(13, Colonias.item(Material.BOOK, Component.text("Níveis da construção", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Cada nível pede um mínimo. O nível 1 é o", NamedTextColor.GRAY),
                Component.text("que você precisa para ela valer; os outros,", NamedTextColor.GRAY),
                Component.text("para ela dar mais (" + p.efeito().texto().toLowerCase(java.util.Locale.ROOT) + ").", NamedTextColor.GRAY)), false));
        List<Vistoria.Requisito> niveis = p.niveis();
        for (int i = 0; i < niveis.size() && i < SLOTS_MATERIAIS.length; i++) {
            int n = i + 1;
            boolean feito = v != null && n <= atual;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(feito ? "✔ Atingido" : n == atual + 1 ? "Próximo nível" : "Mais adiante",
                    feito ? NamedTextColor.GREEN : n == atual + 1 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            lore.addAll(linhasRequisito(niveis.get(i), v));
            ItemStack it = Colonias.item(feito ? Material.LIME_DYE : n == atual + 1 ? Material.YELLOW_DYE : Material.GRAY_DYE,
                    Component.text("Nível " + n, feito ? NamedTextColor.GREEN : NamedTextColor.GOLD, TextDecoration.BOLD), lore, n == atual + 1);
            it.setAmount(n);
            inv.setItem(SLOTS_MATERIAIS[i], it);
        }
    }

    /** Uma construção pronta: nível, o que falta para o próximo e a vistoria. */
    private void abrirConstrucao(Player p, Colonia c, int indice) {
        if (indice < 0 || indice >= c.construcoes.size()) { abrirLista(p, c); return; }
        Construcao k = c.construcoes.get(indice);
        Planta pl = plantas.de(k.planta);
        Tela t = new Tela(Tipo.CONSTRUCAO, c);
        t.construcao = indice;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⌂ " + (pl == null ? k.planta : pl.nome()) + " · nível " + k.nivel));
        Inventory inv = t.inventario;
        World w = Bukkit.getWorld(k.mundo);
        Vistoria v = null;
        if (pl != null && w != null && w.isChunkLoaded(k.x >> 4, k.z >> 4)) v = vistoriar(w, pl, k.area());
        List<Component> info = new ArrayList<>();
        info.add(Colonias.linha("Nível: ", k.nivel + " / " + (pl == null ? "?" : pl.nivelMaximo()) + (k.nivel == 0 ? " (precisa de reparo)" : ""),
                k.nivel == 0 ? NamedTextColor.RED : NamedTextColor.GREEN));
        if (pl != null) info.add(Component.text(pl.efeito().texto(), NamedTextColor.AQUA));
        info.add(Colonias.linha("Feita: ", k.livre ? "do seu jeito" : "pelo Construtor", NamedTextColor.WHITE));
        info.add(Colonias.linha("Onde: ", k.x + ", " + k.y + ", " + k.z, NamedTextColor.GRAY));
        if (pl != null && pl.efeito() == Planta.Efeito.CASA) {
            List<Colonia.Cidadao> mor = moradores(c, k);
            info.add(Colonias.linha("Moradores: ", mor.size() + " / " + Math.max(0, k.camas) + " camas", NamedTextColor.WHITE));
            for (Colonia.Cidadao ci : mor) info.add(Component.text(" ⌂ " + ci.nome + " (" + ci.profissao.nome() + ")", NamedTextColor.GRAY));
        }
        if (v == null) info.add(Component.text("Chegue perto para ver o que tem lá agora.", NamedTextColor.DARK_GRAY));
        inv.setItem(O_INFO, Colonias.item(pl == null ? Material.BARRIER : pl.icone(),
                Component.text(pl == null ? k.planta : pl.nome(), Prefeituras.COR, TextDecoration.BOLD), info, false));
        if (pl != null) desenharNiveis(inv, pl, v, k.nivel);
        inv.setItem(O_CONTORNO, Colonias.item(Material.SPYGLASS, Component.text("Mostrar o contorno", NamedTextColor.YELLOW),
                List.of(Component.text("Partículas no tamanho da área.", NamedTextColor.GRAY)), false));
        inv.setItem(O_TRAZER, Colonias.item(Material.WRITABLE_BOOK, Component.text("✔ Pedir a vistoria", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text("Melhorou a construção? A vistoria", NamedTextColor.GRAY),
                Component.text("confere e sobe o nível.", NamedTextColor.GRAY)), true));
        inv.setItem(O_VOLTAR, Colonias.item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(O_CANCELAR, Colonias.item(Material.RED_DYE, Component.text("Desfazer a construção", NamedTextColor.RED), List.of(
                Component.text("Os blocos ficam; ela só deixa de contar", NamedTextColor.GRAY),
                Component.text("(a torre devolve o território que vigiava).", NamedTextColor.GRAY),
                Component.text("Shift + clique para confirmar.", NamedTextColor.DARK_GRAY)), false));
        Colonias.preencher(inv);
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        Colonia c = t.colonia;
        if (colonias.de(c.dono) != c) { p.closeInventory(); return; }
        int slot = e.getSlot();
        if ((t.tipo == Tipo.CATALOGO || t.tipo == Tipo.LISTA) && slot == VOLTAR) { colonias.abrir(p, c); return; }
        switch (t.tipo) {
            case CATALOGO -> {
                for (int i = 0; i < t.ids.size(); i++) {
                    if (SLOTS_LISTA[i] != slot) continue;
                    Planta pl = plantas.de(t.ids.get(i));
                    if (pl == null) return;
                    if (!colonias.gerencia(p, c)) { Colonias.erro(p, "Só o dono da colônia e quem gerencia o território pegam projetos."); return; }
                    if (c.nivel < pl.nivel()) { Colonias.erro(p, "Libera no nível " + pl.nivel() + " da colônia."); return; }
                    boolean livre = e.isRightClick() && !pl.niveis().isEmpty();
                    for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl, livre)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    p.sendMessage(Component.text(livre
                            ? "⌂ Projeto do seu jeito: " + pl.nome() + ". Clique no chão onde quer a área (o seu lado vira a entrada)."
                            : "⌂ Projeto: " + pl.nome() + ". Clique com ele no chão onde quer a obra (o seu lado vira a entrada).", Prefeituras.COR));
                    p.closeInventory();
                    return;
                }
            }
            case LISTA -> {
                for (int i = 0; i < t.ids.size(); i++) {
                    if (SLOTS_LISTA[i] != slot) continue;
                    String id = t.ids.get(i);
                    if (id.startsWith("o:")) {
                        Obra o = obra(c, UUID.fromString(id.substring(2)));
                        if (o != null) abrirObra(p, c, o);
                    } else {
                        abrirConstrucao(p, c, Integer.parseInt(id.substring(2)));
                    }
                    return;
                }
            }
            case OBRA -> cliqueObra(p, t, slot, e.isShiftClick());
            case CONSTRUCAO -> cliqueConstrucao(p, t, slot, e.isShiftClick());
        }
    }

    private void cliqueConstrucao(Player p, Tela t, int slot, boolean shift) {
        Colonia c = t.colonia;
        if (t.construcao < 0 || t.construcao >= c.construcoes.size()) { abrirLista(p, c); return; }
        Construcao k = c.construcoes.get(t.construcao);
        Planta pl = plantas.de(k.planta);
        switch (slot) {
            case O_VOLTAR -> abrirLista(p, c);
            case O_CONTORNO -> {
                if (pl != null) mostrarContorno(k.area(), pl, true);
                p.closeInventory();
            }
            case O_TRAZER -> {
                if (!colonias.daColonia(p, c)) return;
                pedirVistoria(p, c, null, k);
                abrirConstrucao(p, c, t.construcao);
            }
            case O_CANCELAR -> {
                if (!colonias.gerencia(p, c)) { Colonias.erro(p, "Só o dono da colônia e quem gerencia o território."); return; }
                if (!shift) { Colonias.erro(p, "Shift + clique para confirmar."); return; }
                removerConstrucao(c, k, true);
                p.sendMessage(Component.text("⌂ Construção desfeita (os blocos continuam lá).", NamedTextColor.GRAY));
                abrirLista(p, c);
            }
            default -> { }
        }
    }

    private void cliqueObra(Player p, Tela t, int slot, boolean shift) {
        Colonia c = t.colonia;
        Obra o = obra(c, t.obra);
        if (o == null) { abrirLista(p, c); return; }
        Planta pl = plantas.de(o.planta);
        if (slot == O_VOLTAR) { abrirLista(p, c); return; }
        if (slot == O_CONTORNO) {
            if (pl != null) mostrarContorno(o, pl, true);
            p.closeInventory();
            return;
        }
        if (!colonias.gerencia(p, c) && slot != O_TRAZER) { Colonias.erro(p, "Só o dono da colônia e quem gerencia o território mexem na obra."); return; }
        switch (slot) {
            case O_GIRAR -> {
                if (o.comecou() || pl == null) { Colonias.erro(p, "A obra já começou: não dá mais para girar."); return; }
                Obra nova = new Obra(o.id, o.planta, o.mundo, o.x, o.y, o.z, (o.rot + 1) % 4);
                nova.livre = o.livre;
                nova.extra = o.extra;
                String erro = impedimento(c, pl, nova);
                if (erro != null) { Colonias.erro(p, "✖ Girando não cabe: " + erro); return; }
                removerCerca(o);
                o.rot = nova.rot;
                o.bau = null;
                criarCerca(o, pl);
                mostrarContorno(o, pl, true);
                p.playSound(p.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 1.1f);
                colonias.salvar();
                desenharObra(t, o);
            }
            case O_AREA_MENOS, O_AREA_MAIS -> {
                if (!o.livre || pl == null) return;
                int novo = o.extra + (slot == O_AREA_MAIS ? 1 : -1);
                if (novo < 0 || novo > EXTRA_MAX) { Colonias.erro(p, novo < 0 ? "Já está no tamanho do projeto." : "Esse é o maior tamanho."); return; }
                Obra nova = new Obra(o.id, o.planta, o.mundo, o.x, o.y, o.z, o.rot);
                nova.livre = true;
                nova.extra = novo;
                String erro = impedimento(c, pl, nova);
                if (erro != null) { Colonias.erro(p, "✖ Desse tamanho não cabe: " + erro); return; }
                removerCerca(o);
                o.extra = novo;
                criarCerca(o, pl);
                mostrarContorno(o, pl, true);
                p.playSound(p.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 1.1f);
                colonias.salvar();
                desenharObra(t, o);
            }
            case O_MOVER -> {
                if (o.comecou() || pl == null) { Colonias.erro(p, "A obra já começou: cancele se quiser tirar."); return; }
                removerCerca(o);
                c.obras.remove(o);
                for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl, o.livre)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("⌂ Cerca tirada. O projeto voltou: clique no chão em outro lugar.", Prefeituras.COR));
                colonias.salvar();
                p.closeInventory();
            }
            case O_TRAZER -> {
                if (!colonias.daColonia(p, c) || pl == null) return;
                if (o.livre) {
                    pedirVistoria(p, c, o, null);
                    if (c.obras.contains(o)) desenharObra(t, o);
                    else p.closeInventory();
                    return;
                }
                Inventory bau = inventario(o.bau);
                if (bau == null) { Colonias.erro(p, "Ponha primeiro um baú dentro da cerca (fora da obra)."); return; }
                int movidos = 0;
                for (Map.Entry<Material, Integer> en : restante(o, pl).entrySet()) {
                    int falta = en.getValue() - contar(bau, en.getKey());
                    if (falta <= 0) continue;
                    int tem = colonias.contar(c, simples(en.getKey()));
                    int n = Math.min(falta, tem);
                    if (n <= 0) continue;
                    colonias.tirar(c, simples(en.getKey()), n);
                    while (n > 0) {
                        int lote = Math.min(n, en.getKey().getMaxStackSize());
                        for (ItemStack sobra : bau.addItem(new ItemStack(en.getKey(), lote)).values()) colonias.depositar(c, sobra);
                        n -= lote;
                        movidos += lote;
                    }
                }
                p.sendMessage(Component.text(movidos == 0 ? "⌂ O depósito não tem nada do que falta." : "⌂ " + movidos + " itens foram para o baú da obra.",
                        Prefeituras.COR));
                desenharObra(t, o);
            }
            case O_CANCELAR -> {
                if (!shift) { Colonias.erro(p, "Shift + clique para confirmar."); return; }
                removerCerca(o);
                c.obras.remove(o);
                if (pl != null) for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl, o.livre)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("⌂ Obra cancelada. O projeto voltou para você.", NamedTextColor.GRAY));
                colonias.salvar();
                abrirLista(p, c);
            }
            default -> { }
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }
}
