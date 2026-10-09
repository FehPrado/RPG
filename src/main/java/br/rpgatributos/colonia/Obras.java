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

    private enum Tipo { CATALOGO, LISTA, OBRA }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        final Colonia colonia;
        final List<String> ids = new ArrayList<>();
        UUID obra;
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
    private static final int O_INFO = 4, O_GIRAR = 45, O_MOVER = 46, O_CONTORNO = 47, O_TRAZER = 48, O_VOLTAR = 49, O_CANCELAR = 53;
    private static final int L_INFO = 4, VOLTAR = 49;

    private final RPGAtributos plugin;
    private final Colonias colonias;
    private final Plantas plantas;
    private final MarcadorDeBlocos colocados;
    /** "mundo;x;y;z" de cada bloco de cerca → a obra. */
    private final Map<String, Obra> cercas = new HashMap<>();
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

    /** /rpgadmin recarregar: lê os projetos de novo. */
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
        if (o.y < c.y - Colonia.ABAIXO || o.y + p.altura() > c.y + Colonia.ACIMA) return "Alto ou fundo demais em relação à Prefeitura.";
        if (o.y + p.altura() >= w.getMaxHeight() || o.y - 4 <= w.getMinHeight()) return "Não cabe nessa altura.";
        if (c.x >= a[0] - 1 && c.x <= a[2] + 1 && c.z >= a[1] - 1 && c.z <= a[3] + 1 && Math.abs(c.y - o.y) < p.altura() + 3) {
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
            Obra fantasma = new Obra(UUID.randomUUID(), k.planta(), k.mundo(), k.x(), k.y(), k.z(), k.rot());
            if (sobrepoe(o.area(p, 1), fantasma.area(pk, 0)) && Math.abs(k.y() - o.y) < Math.max(p.altura(), pk.altura())) {
                return "Fica em cima de uma construção pronta (" + pk.nome() + ").";
            }
        }
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
        Block portao = o.bloco(w, p, p.largura() / 2, 0, p.profundidade() + 1);
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
        jogador.sendMessage(Component.text("  1. Agachado + clique na cerca: girar, mudar de lugar ou ver os materiais.", NamedTextColor.GRAY));
        jogador.sendMessage(Component.text("  2. Ponha um baú DENTRO da cerca (fora da obra) com os materiais.", NamedTextColor.GRAY));
        jogador.sendMessage(Component.text("  3. Um Construtor livre (posto: bancada de trabalho) vem erguer a obra.", NamedTextColor.GRAY));
        colonias.salvar();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Obra o = daCerca(e.getBlock());
        if (o == null) return;
        e.setCancelled(true);
        Planta p = plantas.de(o.planta);
        e.getPlayer().sendActionBar(Component.text("Essa cerca marca a obra" + (p == null ? "" : " (" + p.nome() + ")")
                + ". Agachado + clique nela abre o menu.", Prefeituras.COR));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST && b.getType() != Material.BARREL) return;
        for (Colonia c : colonias.todas()) {
            for (Obra o : c.obras) {
                Planta p = plantas.de(o.planta);
                if (p == null || !o.mundo.equals(b.getWorld().getName()) || !o.dentro(p, b.getX(), b.getZ(), 1)) continue;
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
                colonias.avisar(c, "Obra parada: cada Construtor precisa de uma bancada de trabalho no território (posto).");
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
        List<Location> baus = new ArrayList<>();
        for (Planta.Bloco pb : p.ordem()) {
            BlockData d = pb.dados();
            Block b = o.bloco(w, p, pb.lx(), pb.ly(), pb.lz());
            if (d instanceof Fence || d instanceof GlassPane || d instanceof Wall) ligar.add(b);
            Material m = d.getMaterial();
            if (m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) baus.add(b.getLocation());
        }
        conectar(ligar);
        removerCerca(o);
        c.obras.remove(o);
        c.construcoes.add(new Construcao(p.id(), o.mundo, o.x, o.y, o.z, o.rot));
        Component extra = Component.empty();
        switch (p.efeito()) {
            case TORRE_DE_VIGIA -> {
                int n = plugin.territorios().expandirPorTorre(c.dono, o.centro(w));
                extra = Component.text(" O território cresceu " + n + (n == 1 ? " chunk" : " chunks") + " em volta da torre.", NamedTextColor.GREEN);
            }
            case ARMAZEM -> {
                int n = 0;
                for (Location l : baus) {
                    boolean ja = false;
                    for (Location d : c.depositos) if (d.getBlock().equals(l.getBlock())) ja = true;
                    if (!ja) { c.depositos.add(l); n++; }
                }
                extra = Component.text(" " + n + " baús viraram depósito da colônia.", NamedTextColor.GREEN);
            }
            case PRACA -> extra = Component.text(" A colônia ficou mais feliz.", NamedTextColor.GREEN);
            default -> { }
        }
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

    /** Quantas construções prontas desse tipo a colônia tem. */
    int quantas(Colonia c, Planta.Efeito e) {
        int n = 0;
        for (Construcao k : c.construcoes) {
            Planta p = plantas.de(k.planta());
            if (p != null && p.efeito() == e) n++;
        }
        return n;
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
                Component.text("1. Pegue um projeto aqui.", NamedTextColor.GRAY),
                Component.text("2. Clique com ele no chão: o lado em que", NamedTextColor.GRAY),
                Component.text("   você está vira a ENTRADA. Aparece uma", NamedTextColor.GRAY),
                Component.text("   cerca em volta do canteiro.", NamedTextColor.GRAY),
                Component.text("3. Agachado + clique na cerca: girar ou", NamedTextColor.GRAY),
                Component.text("   mudar de lugar (antes de começar).", NamedTextColor.GRAY),
                Component.text("4. Ponha um baú dentro da cerca com os", NamedTextColor.GRAY),
                Component.text("   materiais da lista.", NamedTextColor.GRAY),
                Component.text("5. O Construtor livre vem e ergue a obra.", NamedTextColor.GRAY),
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
            int prontas = 0;
            for (Construcao k : c.construcoes) if (k.planta().equals(pl.id())) prontas++;
            if (prontas > 0) lore.add(Colonias.linha("Prontas na colônia: ", String.valueOf(prontas), NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text("Materiais:", NamedTextColor.GRAY));
            lore.addAll(listaMateriais(pl.materiais(), 8));
            lore.add(Component.empty());
            if (!liberada) lore.add(Component.text("✖ Colônia nível " + pl.nivel(), NamedTextColor.RED));
            else lore.add(Component.text("» Clique para pegar o projeto", NamedTextColor.YELLOW));
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
        Map<String, Integer> prontas = new LinkedHashMap<>();
        for (Construcao k : c.construcoes) {
            Planta pk = plantas.de(k.planta());
            prontas.merge(pk == null ? k.planta() : pk.nome(), 1, Integer::sum);
        }
        List<Component> info = new ArrayList<>();
        info.add(Colonias.linha("Obras: ", c.obras.size() + " / " + maxObras(c), NamedTextColor.WHITE));
        info.add(Colonias.linha("Construtores: ", c.quantos(Profissao.CONSTRUTOR) + "  ·  bancadas: " + c.postos.getOrDefault(Profissao.CONSTRUTOR, 0),
                NamedTextColor.WHITE));
        info.add(Component.empty());
        info.add(Component.text("Construções prontas:", NamedTextColor.GRAY));
        if (prontas.isEmpty()) info.add(Component.text(" nenhuma ainda", NamedTextColor.DARK_GRAY));
        prontas.forEach((n, q) -> info.add(Component.text(" " + q + "x " + n, NamedTextColor.GREEN)));
        inv.setItem(L_INFO, Colonias.item(Material.BRICKS, Component.text("Obras e construções", Prefeituras.COR, TextDecoration.BOLD), info, false));
        for (int i = 0; i < c.obras.size() && i < SLOTS_LISTA.length; i++) {
            Obra o = c.obras.get(i);
            Planta pl = plantas.de(o.planta);
            List<Component> lore = new ArrayList<>(resumo(c, o, pl));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para ver a obra", NamedTextColor.YELLOW));
            inv.setItem(SLOTS_LISTA[i], Colonias.item(pl == null ? Material.BARRIER : pl.icone(),
                    Component.text(pl == null ? o.planta : pl.nome(), Prefeituras.COR, TextDecoration.BOLD), lore, false));
            t.ids.add(o.id.toString());
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
        l.add(Colonias.linha("Onde: ", o.x + ", " + o.y + ", " + o.z, NamedTextColor.GRAY));
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
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⌂ Obra: " + (pl == null ? o.planta : pl.nome())));
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
        if (p != null) {
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
                List.of(Component.text("Partículas no tamanho da construção.", NamedTextColor.GRAY)), false));
        inv.setItem(O_TRAZER, Colonias.item(Material.HOPPER, Component.text("Trazer do depósito", NamedTextColor.YELLOW), List.of(
                Component.text("Passa o que falta (e que o depósito", NamedTextColor.GRAY),
                Component.text("da colônia tiver) para o baú da obra.", NamedTextColor.GRAY)), false));
        inv.setItem(O_VOLTAR, Colonias.item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(O_CANCELAR, Colonias.item(Material.RED_DYE, Component.text("Cancelar a obra", NamedTextColor.RED), List.of(
                Component.text("Tira a cerca e devolve o projeto.", NamedTextColor.GRAY),
                Component.text("O que já foi construído fica.", NamedTextColor.GRAY),
                Component.text("Shift + clique para confirmar.", NamedTextColor.DARK_GRAY)), false));
        Colonias.preencher(inv);
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
        if (t.tipo != Tipo.OBRA && slot == VOLTAR) { colonias.abrir(p, c); return; }
        switch (t.tipo) {
            case CATALOGO -> {
                for (int i = 0; i < t.ids.size(); i++) {
                    if (SLOTS_LISTA[i] != slot) continue;
                    Planta pl = plantas.de(t.ids.get(i));
                    if (pl == null) return;
                    if (!colonias.gerencia(p, c)) { Colonias.erro(p, "Só o dono da colônia e quem gerencia o território pegam projetos."); return; }
                    if (c.nivel < pl.nivel()) { Colonias.erro(p, "Libera no nível " + pl.nivel() + " da colônia."); return; }
                    for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    p.sendMessage(Component.text("⌂ Projeto: " + pl.nome() + ". Clique com ele no chão onde quer a obra (o seu lado vira a entrada).",
                            Prefeituras.COR));
                    p.closeInventory();
                    return;
                }
            }
            case LISTA -> {
                for (int i = 0; i < t.ids.size(); i++) {
                    if (SLOTS_LISTA[i] != slot) continue;
                    Obra o = obra(c, UUID.fromString(t.ids.get(i)));
                    if (o != null) abrirObra(p, c, o);
                    return;
                }
            }
            case OBRA -> cliqueObra(p, t, slot, e.isShiftClick());
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
            case O_MOVER -> {
                if (o.comecou() || pl == null) { Colonias.erro(p, "A obra já começou: cancele se quiser tirar."); return; }
                removerCerca(o);
                c.obras.remove(o);
                for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("⌂ Cerca tirada. O projeto voltou: clique no chão em outro lugar.", Prefeituras.COR));
                colonias.salvar();
                p.closeInventory();
            }
            case O_TRAZER -> {
                if (!colonias.daColonia(p, c) || pl == null) return;
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
                if (pl != null) for (ItemStack sobra : p.getInventory().addItem(plantas.item(pl)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
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
