package br.rpgatributos.ritual;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 2.83: o Altar da Convergência. Uma âncora de renascimento com 1 olho do fim, 1 diamante e
 * 4 fragmentos de ametista jogados em cima vira o altar; até 8 pedestais (blocos lapidados,
 * a 3 blocos de distância) seguram os ingredientes. Com o catalisador na mão, um clique no
 * altar começa a cerimônia: os itens voam dos pedestais, giram e explodem no centro.
 * 2.86: o altar nível II ganha mais 8 pedestais (a 5 blocos), e algumas receitas pedem um
 * sacrifício (um mob vivo perto do altar) ou vários jogadores com o catalisador.
 */
public final class AltarPedestais extends Estacao {

    public static final TextColor COR = TextColor.color(0xB388FF);
    private static final Color ROXO = Color.fromRGB(0xB388FF);
    /** Onde ficam os pedestais: a primeira volta (4 em cruz e 4 nas diagonais) e a segunda (nível II). */
    private static final int[][] LUGARES = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {2, -2}, {-2, 2}, {-2, -2},
            {5, 0}, {-5, 0}, {0, 5}, {0, -5}, {4, 3}, {-4, -3}, {3, -4}, {-3, 4}};
    private static final double RAIO_SACRIFICIO = 5, RAIO_GRUPO = 6;
    public static final Set<Material> PEDESTAIS = EnumSet.of(
            Material.CHISELED_STONE_BRICKS, Material.CHISELED_DEEPSLATE, Material.CHISELED_QUARTZ_BLOCK,
            Material.CHISELED_POLISHED_BLACKSTONE, Material.CHISELED_TUFF, Material.CHISELED_TUFF_BRICKS,
            Material.CHISELED_SANDSTONE, Material.CHISELED_RED_SANDSTONE, Material.CHISELED_NETHER_BRICKS,
            Material.CHISELED_RESIN_BRICKS, Material.QUARTZ_PILLAR, Material.PURPUR_PILLAR);

    private final NamespacedKey kPedestal;
    /** No jogador: as relíquias únicas que ele já fez, separadas por "|". */
    private final NamespacedKey kUnicas;
    private final Map<Location, Cerimonia> cerimonias = new HashMap<>();
    private int voltas;

    private static final class Grimorio implements InventoryHolder {
        Inventory inventario;
        ReceitaRitual.Categoria aba;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public AltarPedestais(RPGAtributos plugin) {
        super(plugin, "altares_convergencia", "altar_convergencia_display");
        kPedestal = new NamespacedKey(plugin, "pedestal_altar");
        kUnicas = new NamespacedKey(plugin, "reliquias_convergencia");
    }

    RPGAtributos plugin() { return plugin; }

    // =====================================================================
    //  Nível do altar (guardado no chunk)
    // =====================================================================

    private NamespacedKey chaveNivel(Location l) {
        return new NamespacedKey(plugin, "altar_nivel_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    public int nivel(Location altar) {
        return altar.getChunk().getPersistentDataContainer().getOrDefault(chaveNivel(altar), PersistentDataType.INTEGER, 1);
    }

    public void definirNivel(Location altar, int n) {
        Location l = altar.getBlock().getLocation();
        l.getChunk().getPersistentDataContainer().set(chaveNivel(l), PersistentDataType.INTEGER, n);
        atualizarDisplays(l);
    }

    private int lugares(Location altar) {
        return nivel(altar) >= 2 ? LUGARES.length : 8;
    }

    // =====================================================================
    //  Ritual de criação e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.RESPAWN_ANCHOR;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENDER_EYE || m == Material.DIAMOND || m == Material.AMETHYST_SHARD;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.RESPAWN_ANCHOR ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.ENDER_EYE) < 1 || contar(itens, Material.DIAMOND) < 1 || contar(itens, Material.AMETHYST_SHARD) < 4) return false;
        tirar(itens, Material.ENDER_EYE, 1);
        tirar(itens, Material.DIAMOND, 1);
        tirar(itens, Material.AMETHYST_SHARD, 4);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.END_CRYSTAL);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        return new ItemStack(nivel(bloco) >= 2 ? Material.NETHER_STAR : Material.END_CRYSTAL);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("✦ Altar da Convergência ✦", COR)
                .append(Component.newline())
                .append(Component.text("ingredientes nos pedestais, catalisador na mão", NamedTextColor.GRAY));
    }

    @Override
    protected Component nome(Location bloco) {
        if (nivel(bloco) < 2) return nome();
        return Component.text("✦ Altar da Convergência · II ✦", COR, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("16 pedestais: ingredientes neles, catalisador na mão", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.REVERSE_PORTAL, c, 120, 0.6, 0.8, 0.6, 0.1);
        w.spawnParticle(Particle.END_ROD, c, 40, 0.4, 0.6, 0.4, 0.08);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.4f);
        w.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.6f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("✦ Altar da Convergência ✦", COR, TextDecoration.BOLD),
                Component.text("Agora coloque pedestais a 3 blocos dele", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))));
        quem.sendMessage(Component.text("✦ Pedestais: blocos lapidados (pedra, ardósia, quartzo, tufo...) ou pilares de quartzo/púrpura, "
                + "a 3 blocos do altar em cruz ou 2+2 na diagonal, na mesma altura (ou 1 acima/abaixo). Até 8.", COR));
        quem.sendMessage(Component.text("✦ Clique num pedestal com um item para pôr nele (agachado: só 1). Clique de novo para tirar. "
                + "Mão vazia no altar abre o Grimório.", NamedTextColor.GRAY));
        plugin.stats().darXp(quem, Skill.ARCANO, 30);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        centro.getWorld().spawnParticle(Particle.PORTAL, centro, 6, 0.3, 0.4, 0.3, 0.4);
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.DUST, centro, 2, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(ROXO, 1.2f));
    }

    @Override
    protected void devolver(Location centro) {
        World w = centro.getWorld();
        w.dropItemNaturally(centro, new ItemStack(Material.ENDER_EYE));
        w.dropItemNaturally(centro, new ItemStack(Material.DIAMOND));
        w.dropItemNaturally(centro, new ItemStack(Material.AMETHYST_SHARD, 4));
        w.playSound(centro, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
    }

    @Override
    protected void aoRemover(Location bloco) {
        // Os ingredientes ainda nos pedestais caem no chão (das duas voltas).
        for (int k = 0; k < LUGARES.length; k++) {
            Block p = pedestal(bloco, k);
            ItemDisplay d = p == null ? null : exibido(p);
            if (d != null) soltar(d);
        }
        bloco.getChunk().getPersistentDataContainer().remove(chaveNivel(bloco));
    }

    // =====================================================================
    //  Pedestais
    // =====================================================================

    /** O topo do pedestal no lugar {@code k} em volta do altar, ou null se não tem pedestal lá. */
    private Block pedestal(Location altar, int k) {
        Block base = altar.getBlock();
        for (int dy = 1; dy >= -1; dy--) {
            Block b = base.getRelative(LUGARES[k][0], dy, LUGARES[k][1]);
            if (PEDESTAIS.contains(b.getType()) && !PEDESTAIS.contains(b.getRelative(BlockFace.UP).getType())) return b;
        }
        return null;
    }

    /** O altar ao qual esse pedestal pertence, ou null. */
    private Location altarDe(Block b) {
        for (Location l : carregadas()) {
            if (l.getWorld() != b.getWorld() || Math.abs(b.getY() - l.getBlockY()) > 1) continue;
            int dx = b.getX() - l.getBlockX(), dz = b.getZ() - l.getBlockZ();
            if (Math.abs(dx) > 5 || Math.abs(dz) > 5) continue;
            int n = lugares(l);
            for (int k = 0; k < n; k++) {
                if (LUGARES[k][0] == dx && LUGARES[k][1] == dz && b.equals(pedestal(l, k))) return l;
            }
        }
        return null;
    }

    private static String chave(Block b) {
        return b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
    }

    /** O item em cima do pedestal (que guarda o próprio item), ou null. */
    private ItemDisplay exibido(Block topo) {
        String chave = chave(topo);
        for (Entity e : topo.getWorld().getNearbyEntities(topo.getLocation().add(0.5, 1.25, 0.5), 0.5, 0.6, 0.5)) {
            if (e instanceof ItemDisplay d && chave.equals(d.getPersistentDataContainer().get(kPedestal, PersistentDataType.STRING))) return d;
        }
        return null;
    }

    private void soltar(ItemDisplay d) {
        ItemStack s = d.getItemStack();
        Location l = d.getLocation();
        d.remove();
        if (!s.isEmpty()) l.getWorld().dropItemNaturally(l, s);
    }

    private void colocar(Block topo, ItemStack s) {
        String chave = chave(topo);
        topo.getWorld().spawn(topo.getLocation().add(0.5, 1.25, 0.5), ItemDisplay.class, d -> {
            d.setItemStack(s);
            d.setPersistent(true);
            d.setTeleportDuration(20);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(0.5f, 0.5f, 0.5f), new AxisAngle4f()));
            d.getPersistentDataContainer().set(kPedestal, PersistentDataType.STRING, chave);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarPedestal(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || !PEDESTAIS.contains(b.getType()) || plugin.ehEstacao(b)) return;
        Location altar = altarDe(b);
        if (altar == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!plugin.territorios().podeConstruir(p, b)) {
            p.sendActionBar(Component.text("✖ Esse altar é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        if (cerimonias.containsKey(altar)) {
            p.sendActionBar(Component.text("✦ O ritual já começou: espere terminar.", COR));
            return;
        }
        ItemDisplay d = exibido(b);
        if (d != null) {
            ItemStack s = d.getItemStack();
            d.remove();
            p.getInventory().addItem(s).values().forEach(r -> p.getWorld().dropItemNaturally(p.getLocation(), r));
            p.playSound(b.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 0.8f);
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.isEmpty()) {
            p.sendActionBar(Component.text("✦ Clique com um item na mão para pôr no pedestal.", COR));
            return;
        }
        ItemStack por = mao.clone();
        if (p.isSneaking()) por.setAmount(1);
        mao.setAmount(mao.getAmount() - por.getAmount());
        p.getInventory().setItemInMainHand(mao.getAmount() <= 0 ? null : mao);
        colocar(b, por);
        Location l = b.getLocation().add(0.5, 1.25, 0.5);
        b.getWorld().spawnParticle(Particle.ENCHANT, l, 20, 0.2, 0.3, 0.2, 0.8);
        b.getWorld().playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrarPedestal(BlockBreakEvent e) {
        if (!PEDESTAIS.contains(e.getBlock().getType())) return;
        ItemDisplay d = exibido(e.getBlock());
        if (d != null) soltar(d);
    }

    /** Um pedestal ainda está de pé? (A cerimônia falha se um pedestal usado sumir.) */
    boolean pedestalInteiro(Location topo) {
        return PEDESTAIS.contains(topo.getBlock().getType());
    }

    @Override
    protected void aoTick() {
        if (++voltas % 4 != 0) return; // 1 vez por segundo
        for (Location l : carregadas()) {
            if (!l.isChunkLoaded()) continue;
            boolean perto = false;
            for (Player p : l.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(l) < 24 * 24) { perto = true; break; }
            }
            if (!perto) continue;
            Location c = l.clone().add(0.5, 1, 0.5);
            for (Entity e : l.getWorld().getNearbyEntities(c, 6, 3, 6)) {
                if (!(e instanceof ItemDisplay d) || !d.getPersistentDataContainer().has(kPedestal)) continue;
                Block topo = d.getLocation().clone().subtract(0, 1.25, 0).getBlock();
                if (!PEDESTAIS.contains(topo.getType())) { soltar(d); continue; } // o pedestal sumiu (pistão, explosão)
                Location pos = d.getLocation();
                pos.setYaw(pos.getYaw() + 40);
                d.teleport(pos);
                l.getWorld().spawnParticle(Particle.ENCHANT, pos.clone().add(0, -0.3, 0), 2, 0.2, 0.1, 0.2, 0.5);
            }
        }
    }

    // =====================================================================
    //  Cerimônia
    // =====================================================================

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        if (cerimonias.containsKey(l)) {
            p.sendActionBar(Component.text("✦ O ritual já está acontecendo.", COR));
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.isEmpty()) {
            if (agachado) mostrarPedestais(p, l);
            else abrirGrimorio(p);
            return;
        }
        if (!plugin.territorios().podeConstruir(p, b)) {
            p.sendMessage(Component.text("✖ Só quem pode construir aqui acende esse altar.", NamedTextColor.RED));
            return;
        }
        tentar(p, l, mao);
    }

    private void mostrarPedestais(Player p, Location altar) {
        int n = lugares(altar), pedestais = 0, cheios = 0;
        for (int k = 0; k < n; k++) {
            Block t = pedestal(altar, k);
            if (t == null) continue;
            pedestais++;
            if (exibido(t) != null) cheios++;
        }
        p.sendMessage(Component.text("✦ Altar nível " + ReceitaRitual.romano(nivel(altar)) + " · pedestais: " + pedestais + "/" + n
                + " (" + cheios + " com ingrediente).", COR));
    }

    /** Mobs que podem ser sacrificados perto do altar (sem dono, sem nome, que não são guardiões nem companheiros). */
    private List<LivingEntity> sacrificaveis(Location altar, ReceitaRitual.Sacrificio s) {
        List<LivingEntity> l = new ArrayList<>();
        Location c = altar.clone().add(0.5, 1, 0.5);
        for (Entity e : altar.getWorld().getNearbyEntities(c, RAIO_SACRIFICIO, 3, RAIO_SACRIFICIO)) {
            if (e.getType() != s.tipo() || !(e instanceof LivingEntity v) || v.isDead()) continue;
            if (v.customName() != null || Companheiros.eh(v) || Guardioes.tipo(v) != null) continue;
            if (v instanceof Tameable t && t.isTamed()) continue;
            l.add(v);
            if (l.size() >= s.qtd()) break;
        }
        return l;
    }

    /** Os outros jogadores perto do altar com o catalisador na mão (ritual de grupo). */
    private List<Player> ajudantes(Player p, Location altar, ReceitaRitual r) {
        List<Player> l = new ArrayList<>();
        Location c = altar.clone().add(0.5, 1, 0.5);
        for (Player o : altar.getWorld().getPlayers()) {
            if (o == p || o.getLocation().distanceSquared(c) > RAIO_GRUPO * RAIO_GRUPO) continue;
            ItemStack s = o.getInventory().getItemInMainHand();
            if (!s.isEmpty() && r.catalisador().test(s) && s.getAmount() >= r.qtdCatalisador()) l.add(o);
        }
        return l;
    }

    private void tentar(Player p, Location altar, ItemStack mao) {
        List<Block> topos = new ArrayList<>();
        List<ItemDisplay> itens = new ArrayList<>();
        int n = lugares(altar);
        for (int k = 0; k < n; k++) {
            Block t = pedestal(altar, k);
            ItemDisplay d = t == null ? null : exibido(t);
            if (d == null) continue;
            topos.add(t);
            itens.add(d);
        }
        if (itens.isEmpty()) {
            p.sendMessage(Component.text("✦ Os pedestais estão vazios. Ponha os ingredientes neles antes (mão vazia no altar abre o Grimório).", COR));
            negar(p);
            return;
        }
        String erro = null;
        boolean algumCatalisador = false;
        for (ReceitaRitual r : ReceitaRitual.TODAS) {
            if (!r.catalisador().test(mao) || mao.getAmount() < r.qtdCatalisador()) continue;
            algumCatalisador = true;
            int[] onde = atribuir(r.pedestais(), itens);
            if (onde == null) continue;
            ReceitaRitual.Extra ex = r.extra();
            String imp = r.impedimento().apply(mao);
            if (imp == null && ex.nivelAltar() > nivel(altar)) imp = r.nome() + " precisa do altar nível II (ritual Despertar o Altar).";
            if (imp == null && r.unica() && jaFez(p, r)) imp = "Você já fez a " + r.nome() + ": é uma por jogador.";
            if (imp == null && r.desperto() != null && plugin.chefes().ocupado(altar)) imp = "Já tem um chefe lutando neste altar.";
            if (imp == null && ex.efeito() != null) imp = ex.efeito().impedimento(plugin, altar, p);
            List<LivingEntity> vitimas = List.of();
            if (imp == null && ex.sacrificio() != null) {
                vitimas = sacrificaveis(altar, ex.sacrificio());
                if (vitimas.size() < ex.sacrificio().qtd()) {
                    imp = "Falta o sacrifício: " + ex.sacrificio().nome() + " a até 5 blocos do altar (sem nome e sem dono).";
                }
            }
            List<Player> grupo = List.of();
            if (imp == null && ex.jogadores() > 1) {
                grupo = ajudantes(p, altar, r);
                if (grupo.size() < ex.jogadores() - 1) {
                    imp = r.nome() + " precisa de " + ex.jogadores() + " jogadores perto do altar, cada um com " + r.nomeCatalisador() + " na mão.";
                } else {
                    grupo = grupo.subList(0, ex.jogadores() - 1);
                }
            }
            ItemStack previa = null;
            if (imp == null && ex.comPedestais() != null) {
                List<ItemStack> peds = new ArrayList<>();
                for (int k = 0; k < onde.length; k++) peds.add(itens.get(onde[k]).getItemStack());
                previa = ex.comPedestais().apply(mao, peds);
                if (previa == null) imp = "Os itens dos pedestais não servem nesse " + r.nomeCatalisador() + ".";
            }
            if (imp != null) { erro = imp; continue; }
            iniciar(p, altar, r, mao, topos, itens, onde, vitimas, grupo, previa);
            return;
        }
        if (erro != null) p.sendMessage(Component.text("✖ " + erro, NamedTextColor.RED));
        else if (algumCatalisador) p.sendMessage(Component.text("✖ Os pedestais não formam nenhum ritual com esse item. "
                + "Cada ingrediente num pedestal, sem nada a mais (veja o Grimório).", NamedTextColor.RED));
        else p.sendMessage(Component.text("✖ Esse item não é catalisador de nenhum ritual (veja o Grimório).", NamedTextColor.RED));
        negar(p);
    }

    /**
     * Para cada ingrediente, qual pedestal o tem (todos os pedestais com item precisam ser usados).
     * @return o índice do pedestal de cada ingrediente, ou null se não fecha.
     */
    private static int[] atribuir(List<ReceitaRitual.Ingrediente> pedidos, List<ItemDisplay> itens) {
        if (pedidos.size() != itens.size()) return null;
        int[] onde = new int[pedidos.size()];
        return atribuir(pedidos, itens, onde, new boolean[itens.size()], 0) ? onde : null;
    }

    private static boolean atribuir(List<ReceitaRitual.Ingrediente> pedidos, List<ItemDisplay> itens, int[] onde, boolean[] usado, int k) {
        if (k == pedidos.size()) return true;
        for (int i = 0; i < itens.size(); i++) {
            if (usado[i] || !pedidos.get(k).serve(itens.get(i).getItemStack())) continue;
            usado[i] = true;
            onde[k] = i;
            if (atribuir(pedidos, itens, onde, usado, k + 1)) return true;
            usado[i] = false;
        }
        return false;
    }

    private static ItemStack tirarDaMao(Player p, int qtd) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        ItemStack tirado = mao.clone();
        tirado.setAmount(qtd);
        mao.setAmount(mao.getAmount() - qtd);
        p.getInventory().setItemInMainHand(mao.getAmount() <= 0 ? null : mao);
        return tirado;
    }

    private void iniciar(Player p, Location altar, ReceitaRitual r, ItemStack mao, List<Block> topos, List<ItemDisplay> itens, int[] onde,
                         List<LivingEntity> vitimas, List<Player> grupo, ItemStack previa) {
        ItemStack catalisador = tirarDaMao(p, r.qtdCatalisador());
        List<ItemStack> doGrupo = new ArrayList<>();
        for (Player o : grupo) {
            doGrupo.add(tirarDaMao(o, r.qtdCatalisador()));
            o.sendMessage(Component.text("✦ Você se juntou ao ritual " + r.nome() + " de " + p.getName() + ".", r.categoria().cor));
        }
        ItemStack resultado = previa != null ? previa : r.resultado().apply(catalisador);

        List<Location> origens = new ArrayList<>();
        List<ItemStack> gastos = new ArrayList<>();
        for (int k = 0; k < onde.length; k++) {
            ItemDisplay d = itens.get(onde[k]);
            ItemStack s = d.getItemStack();
            int qtd = r.pedestais().get(k).qtd();
            ItemStack gasto = s.clone();
            gasto.setAmount(qtd);
            s.setAmount(s.getAmount() - qtd);
            if (s.getAmount() <= 0) d.remove();
            else d.setItemStack(s);
            origens.add(topos.get(onde[k]).getLocation());
            gastos.add(gasto);
        }
        Cerimonia c = new Cerimonia(this, altar, p.getUniqueId(), r, catalisador, resultado, origens, gastos);
        c.sacrificios(vitimas);
        c.grupo(grupo, doGrupo);
        cerimonias.put(altar, c);
        c.comecar();
        p.sendMessage(Component.text("✦ O ritual " + r.nome() + " começou... não se afaste e não deixe quebrarem os pedestais.", r.categoria().cor));
    }

    void concluida(Cerimonia c, UUID dono, ReceitaRitual r) {
        cerimonias.remove(c.bloco);
        Player p = dono == null ? null : Bukkit.getPlayer(dono);
        if (p == null || r == null) return;
        plugin.stats().darXp(p, Skill.ARCANO, 40);
        plugin.titulos().registrar(p, "rituais_convergencia", 1);
        if (r.unica()) {
            String feitas = p.getPersistentDataContainer().getOrDefault(kUnicas, PersistentDataType.STRING, "");
            p.getPersistentDataContainer().set(kUnicas, PersistentDataType.STRING, feitas.isEmpty() ? r.nome() : feitas + "|" + r.nome());
            plugin.getServer().broadcast(Component.text("✦ " + p.getName() + " criou a ", COR)
                    .append(Component.text(r.nome(), COR, TextDecoration.BOLD))
                    .append(Component.text(" no Altar da Convergência!", COR)));
        }
    }

    private boolean jaFez(Player p, ReceitaRitual r) {
        String feitas = p.getPersistentDataContainer().getOrDefault(kUnicas, PersistentDataType.STRING, "");
        return List.of(feitas.split("\\|")).contains(r.nome());
    }

    private static void negar(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    /** O servidor vai desligar: os rituais terminam na hora. */
    public void desligar() {
        for (Cerimonia c : List.copyOf(cerimonias.values())) c.terminarAgora();
        cerimonias.clear();
    }

    // =====================================================================
    //  Grimório
    // =====================================================================

    private static final int[] S_RECEITAS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] S_ABAS = {10, 11, 12, 14, 15, 16};

    public void abrirGrimorio(Player p) {
        Grimorio g = new Grimorio();
        g.inventario = Bukkit.createInventory(g, 54, Component.text("✦ Grimório da Convergência"));
        desenharGrimorio(g, ReceitaRitual.Categoria.TRANSMUTAR);
        p.openInventory(g.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.9f);
    }

    private void desenharGrimorio(Grimorio g, ReceitaRitual.Categoria aba) {
        Inventory inv = g.inventario;
        inv.clear();
        inv.setItem(4, item(Material.END_CRYSTAL, Component.text("✦ Altar da Convergência", COR, TextDecoration.BOLD), List.of(
                Component.text("1. Ponha cada ingrediente num pedestal.", NamedTextColor.GRAY),
                Component.text("2. Traga o sacrifício, se a receita pedir.", NamedTextColor.GRAY),
                Component.text("3. Segure o catalisador e clique no altar.", NamedTextColor.GRAY),
                Component.text("4. Fique perto e assista: se você se afastar", NamedTextColor.GRAY),
                Component.text("   ou um pedestal quebrar, o ritual sai errado.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Sem nada a mais nos pedestais: cada ritual", NamedTextColor.DARK_GRAY),
                Component.text("usa exatamente os pedestais da receita.", NamedTextColor.DARK_GRAY),
                Component.text("Agachado + mão vazia: conta os pedestais.", NamedTextColor.DARK_GRAY))));
        ReceitaRitual.Categoria[] cats = ReceitaRitual.Categoria.values();
        for (int k = 0; k < cats.length && k < S_ABAS.length; k++) {
            ReceitaRitual.Categoria cat = cats[k];
            boolean aberta = cat == aba;
            ItemStack icone = item(cat.icone, Component.text((aberta ? "▶ " : "") + cat.nome, cat.cor, TextDecoration.BOLD),
                    List.of(Component.text(aberta ? "Aberta" : "Clique para ver", NamedTextColor.GRAY)));
            if (aberta) icone.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_ABAS[k], icone);
        }
        int n = 0;
        for (ReceitaRitual r : ReceitaRitual.TODAS) {
            if (r.categoria() != aba || r.extra().oculta() || n >= S_RECEITAS.length) continue;
            inv.setItem(S_RECEITAS[n++], item(r.icone(), Component.text(r.nome(), aba.cor, TextDecoration.BOLD), r.lore()));
        }
        g.aba = aba;
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < 18; i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarGrimorio(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Grimorio g)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= 54) return;
        ReceitaRitual.Categoria[] cats = ReceitaRitual.Categoria.values();
        for (int k = 0; k < cats.length && k < S_ABAS.length; k++) {
            if (S_ABAS[k] == e.getRawSlot() && cats[k] != g.aba) {
                desenharGrimorio(g, cats[k]);
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.1f);
            }
        }
    }

    @EventHandler
    public void aoArrastarGrimorio(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Grimorio) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
