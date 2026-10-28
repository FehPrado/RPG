package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.entity.TeleportFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Barco Grande (2.63): um barco de 5 lugares (o piloto e 4 bancos). O Minecraft só tem barcos de 2
 * lugares, então ele é montado em peças:
 * <ul>
 *   <li>o casco é um barco de abeto normal, na frente: quem pilota rema nele (a água e as batidas são
 *       as do jogo);</li>
 *   <li>o convés, as bordas e os bancos são blocos desenhados, presos ao barco como passageiros (andam
 *       e viram junto, sem atraso);</li>
 *   <li>os 4 bancos de trás são assentos invisíveis que o plugin leva junto com o barco a cada tick.</li>
 * </ul>
 * 3.0: cada banco é um encaixe (veja {@link MelhoriasBarco}): assento de gente ou de bicho, o mastro
 * da vela (banco 1) ou um baú de convés. Ele pode ser amarrado com laço, como os outros barcos, e
 * puxar ou ser puxado num comboio.
 * <p>As peças não são salvas: quando o barco carrega de novo, o plugin monta outra vez.
 */
public final class BarcoGrande implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "barco_grande");
    private static final NamespacedKey K_PARTE = new NamespacedKey("rpgatributos", "barco_grande_parte");
    private static final NamespacedKey K_ASSENTO = new NamespacedKey("rpgatributos", "barco_grande_assento");
    /** O baú desenhado num banco (guarda o número do banco). */
    private static final NamespacedKey K_BAU_DISPLAY = new NamespacedKey("rpgatributos", "barco_grande_bau");
    private static final TextColor COR = TextColor.color(0xC9A26B);

    /** Passageiro do barco (que não é o primeiro) fica 0,1875 acima e 0,6 para trás do centro. */
    private static final float PRESO_Y = 0.1875f, PRESO_Z = -0.6f;
    /** Os 4 bancos (x, z no barco; +z é a frente). O banco 1 vira o mastro quando há vela. */
    private static final double[][] BANCOS = {{-0.4, -1.35}, {0.4, -1.35}, {-0.4, -2.4}, {0.4, -2.4}};
    private static final int BANCO_DO_MASTRO = 1;
    /** Os bancos de trás ficam acima da água (o convés é mais alto que o fundo do barco da frente). */
    private static final double ALTURA_ASSENTO = 0.55;

    private final RPGAtributos plugin;
    private final MelhoriasBarco melhorias;
    /** Barco → os assentos ocupados (null = livre). */
    private final Map<UUID, ItemDisplay[]> assentos = new HashMap<>();
    /** Os Barcos Grandes montados (para as peças virarem junto com o barco). */
    private final java.util.Set<UUID> montados = new java.util.HashSet<>();

    public BarcoGrande(RPGAtributos plugin) {
        this.plugin = plugin;
        this.melhorias = new MelhoriasBarco(plugin);
    }

    public MelhoriasBarco melhorias() {
        return melhorias;
    }

    // =====================================================================
    //  O item
    // =====================================================================

    public static ItemStack item() {
        ItemStack i = new ItemStack(Material.SPRUCE_BOAT);
        i.editMeta(m -> {
            m.itemName(Component.text("Barco Grande", COR));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        });
        escreverLore(i);
        return i;
    }

    /** As linhas do item: o básico e o que ele leva. */
    private static void escreverLore(ItemStack i) {
        i.editMeta(m -> {
            List<Component> l = new ArrayList<>();
            l.add(Component.text("Leva 5: quem pilota e 4 nos bancos.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            l.add(Component.text("Bancos viram mastro, baú ou lugar de bicho.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            l.addAll(MelhoriasBarco.resumo(m.getPersistentDataContainer()));
            l.add(Component.text("Agachado + mão vazia no barco: menu.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            m.lore(l);
        });
    }

    public static boolean eItem(ItemStack i) {
        return i != null && i.getType() == Material.SPRUCE_BOAT && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public static boolean eBarcoGrande(Entity e) {
        return e instanceof Boat && e.getPersistentDataContainer().has(CHAVE);
    }

    public void registrarReceitas() {
        List<Material> barcos = new ArrayList<>();
        for (Material m : Material.values()) {
            String n = m.name();
            if (!m.isLegacy() && m.isItem() && (n.endsWith("_BOAT") || n.endsWith("_RAFT")) && !n.contains("CHEST")) barcos.add(m);
        }
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "barco_grande"), item());
        r.shape("PBP", "PBP", "PPP");
        r.setIngredient('B', new RecipeChoice.MaterialChoice(barcos));
        r.setIngredient('P', new RecipeChoice.MaterialChoice(org.bukkit.Tag.PLANKS));
        Bukkit.addRecipe(r);
        melhorias.registrarReceitas();
    }

    // =====================================================================
    //  Pôr na água, montar e recolher
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!eItem(e.getItem())) return;
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!plugin.settings().barcoGrande) return;
        RayTraceResult r = p.rayTraceBlocks(5, FluidCollisionMode.SOURCE_ONLY);
        if (r == null || r.getHitBlock() == null || r.getHitBlock().getType() != Material.WATER) {
            p.sendActionBar(Component.text("⛵ O Barco Grande só vai na água: mire nela.", NamedTextColor.RED));
            return;
        }
        Location l = r.getHitPosition().toLocation(p.getWorld());
        l.setYaw(p.getLocation().getYaw());
        l.setPitch(0);
        ItemStack mao = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        Boat b = (Boat) p.getWorld().spawnEntity(l, EntityType.SPRUCE_BOAT);
        b.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        MelhoriasBarco.copiar(mao.getPersistentDataContainer(), b.getPersistentDataContainer());
        montar(b);
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        p.playSound(l, Sound.ENTITY_BOAT_PADDLE_WATER, 1f, 0.8f);
        p.sendActionBar(Component.text("⛵ Barco Grande na água: clique para subir (" + lugares(b) + " lugares).", COR));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoDispensar(BlockDispenseEvent e) {
        if (eItem(e.getItem())) e.setCancelled(true);
    }

    /** Pede as peças (convés, bordas, bancos, mastro, baús e a área de clique) presas ao barco. */
    private void montar(Boat b) {
        tirarPecas(b);
        World w = b.getWorld();
        Location l = b.getLocation();
        List<Entity> pecas = new ArrayList<>();
        // O lastro vai na frente da fila: assim nenhuma peça desenhada fica no 1º lugar (que muda de posição).
        pecas.add(w.spawn(l, ItemDisplay.class, d -> marcar(d)));
        BlockData tabua = Material.SPRUCE_PLANKS.createBlockData(), borda = Material.STRIPPED_SPRUCE_WOOD.createBlockData();
        pecas.add(bloco(w, l, tabua, -0.75f, 0.15f, -3.1f, 1.5f, 0.25f, 2.6f));       // convés
        pecas.add(bloco(w, l, borda, -0.9f, 0.05f, -3.1f, 0.15f, 0.7f, 2.6f));        // borda esquerda
        pecas.add(bloco(w, l, borda, 0.75f, 0.05f, -3.1f, 0.15f, 0.7f, 2.6f));        // borda direita
        pecas.add(bloco(w, l, borda, -0.9f, 0.05f, -3.25f, 1.8f, 0.75f, 0.15f));      // popa
        pecas.add(bloco(w, l, tabua, -0.75f, 0.4f, -1.55f, 1.5f, 0.1f, 0.4f));        // banco da frente
        pecas.add(bloco(w, l, tabua, -0.75f, 0.4f, -2.6f, 1.5f, 0.1f, 0.4f));         // banco de trás
        if (Velas.temVela(b)) pecas.add(mastro(w, l, b));
        for (int banco : MelhoriasBarco.baus(b.getPersistentDataContainer())) pecas.add(bau(w, l, banco));
        if (MelhoriasBarco.tem(b, MelhoriasBarco.Peca.REDE)) pecas.add(bloco(w, l, Material.COBWEB.createBlockData(), -0.6f, 0.05f, -3.55f, 1.2f, 0.4f, 0.3f));
        if (MelhoriasBarco.tem(b, MelhoriasBarco.Peca.ANCORA)) pecas.add(bloco(w, l, Material.ANVIL.createBlockData(), 0.45f, 0.4f, -0.2f, 0.35f, 0.35f, 0.35f));
        pecas.add(w.spawn(l, Interaction.class, i -> {
            i.setInteractionWidth(2.4f);
            i.setInteractionHeight(1.0f);
            i.setResponsive(true);
            marcar(i);
        }));
        for (Entity x : pecas) b.addPassenger(x);
        pilotoNaFrente(b);
        montados.add(b.getUniqueId());
    }

    private static void marcar(Entity e) {
        e.setPersistent(false);
        e.getPersistentDataContainer().set(K_PARTE, PersistentDataType.BYTE, (byte) 1);
    }

    private static BlockDisplay bloco(World w, Location l, BlockData bloco, float x, float y, float z, float sx, float sy, float sz) {
        return w.spawn(l, BlockDisplay.class, d -> {
            d.setBlock(bloco);
            d.setTransformation(new Transformation(new Vector3f(x, y - PRESO_Y, z - PRESO_Z), new AxisAngle4f(),
                    new Vector3f(sx, sy, sz), new AxisAngle4f()));
            marcar(d);
        });
    }

    /** O mastro com a vela (o estandarte do dono, se ele pôs um). */
    private static ItemDisplay mastro(World w, Location l, Boat b) {
        double[] m = BANCOS[BANCO_DO_MASTRO];
        return w.spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(Velas.bandeira(b));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTransformation(new Transformation(new Vector3f((float) m[0], 1.5f - PRESO_Y, (float) m[1] - PRESO_Z), new AxisAngle4f(),
                    new Vector3f(1.6f, 1.6f, 1.6f), new AxisAngle4f()));
            marcar(d);
            d.getPersistentDataContainer().set(Velas.K_DISPLAY, PersistentDataType.BYTE, (byte) 1);
        });
    }

    /** O baú de convés desenhado em cima do banco. */
    private static ItemDisplay bau(World w, Location l, int banco) {
        double[] m = BANCOS[banco];
        return w.spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.CHEST));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTransformation(new Transformation(new Vector3f((float) m[0], 0.78f - PRESO_Y, (float) m[1] - PRESO_Z),
                    new AxisAngle4f((float) Math.PI, 0, 1, 0), new Vector3f(0.62f, 0.62f, 0.62f), new AxisAngle4f()));
            marcar(d);
            d.getPersistentDataContainer().set(K_BAU_DISPLAY, PersistentDataType.INTEGER, banco);
        });
    }

    private static void tirarPecas(Boat b) {
        for (Entity x : List.copyOf(b.getPassengers())) {
            if (x.getPersistentDataContainer().has(K_PARTE)) {
                b.removePassenger(x);
                x.remove();
            }
        }
    }

    /** Quem pilota tem de ser o 1º passageiro: as peças vão para o fim da fila (na mesma ordem). */
    private static void pilotoNaFrente(Boat b) {
        List<Entity> l = b.getPassengers();
        if (l.isEmpty() || l.getFirst() instanceof Player || l.stream().noneMatch(x -> x instanceof Player)) return;
        for (Entity x : List.copyOf(l)) {
            if (x instanceof Player) continue;
            b.removePassenger(x);
            b.addPassenger(x);
        }
    }

    static Player piloto(Boat b) {
        List<Entity> l = b.getPassengers();
        return !l.isEmpty() && l.getFirst() instanceof Player p ? p : null;
    }

    /** Quantos estão a bordo (o piloto e quem está nos bancos, gente ou bicho). */
    private int lotacao(Boat b) {
        int n = piloto(b) != null ? 1 : 0;
        ItemDisplay[] a = assentos.get(b.getUniqueId());
        if (a != null) for (ItemDisplay d : a) if (d != null && d.isValid() && !d.getPassengers().isEmpty()) n++;
        return n;
    }

    /** Os jogadores a bordo (piloto e bancos). */
    public List<Player> aBordo(Boat b) {
        List<Player> l = new ArrayList<>();
        Player pi = piloto(b);
        if (pi != null) l.add(pi);
        ItemDisplay[] a = assentos.get(b.getUniqueId());
        if (a != null) for (ItemDisplay d : a) if (d != null && d.isValid()) for (Entity x : d.getPassengers()) if (x instanceof Player p) l.add(p);
        return l;
    }

    /** Os bichos sentados nos bancos. */
    List<Entity> bichos(Boat b) {
        List<Entity> l = new ArrayList<>();
        ItemDisplay[] a = assentos.get(b.getUniqueId());
        if (a != null) for (ItemDisplay d : a) if (d != null && d.isValid()) for (Entity x : d.getPassengers()) if (!(x instanceof Player)) l.add(x);
        return l;
    }

    /** O banco não é assento (mastro ou baú). */
    private static boolean fechado(Boat b, int banco) {
        return banco == BANCO_DO_MASTRO && Velas.temVela(b) || MelhoriasBarco.baus(b.getPersistentDataContainer()).contains(banco);
    }

    private int lugares(Boat b) {
        int n = 1;
        for (int i = 0; i < BANCOS.length; i++) if (!fechado(b, i)) n++;
        return n;
    }

    /** O barco virando item, com tudo o que leva. */
    private ItemStack comoItem(Boat b) {
        ItemStack i = item();
        i.editMeta(m -> MelhoriasBarco.copiar(b.getPersistentDataContainer(), m.getPersistentDataContainer()));
        escreverLore(i);
        return i;
    }

    /** Desmonta o barco e devolve o item (com as melhorias e os baús dentro) para quem recolheu, ou no chão. */
    private void recolher(Boat b, Player quem) {
        melhorias.fecharBaus(b);
        ItemStack it = comoItem(b);
        desfazer(b);
        Location l = b.getLocation();
        b.remove();
        if (quem != null) for (ItemStack s : quem.getInventory().addItem(it).values()) l.getWorld().dropItem(quem.getLocation(), s);
        else l.getWorld().dropItemNaturally(l, it);
        l.getWorld().playSound(l, Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
    }

    boolean recolherPeloMenu(Boat b, Player p) {
        if (lotacao(b) > 0) {
            p.sendActionBar(Component.text("⛵ Tem gente ou bicho a bordo: todos precisam descer para recolher o barco.", NamedTextColor.RED));
            return false;
        }
        recolher(b, p);
        return true;
    }

    /**
     * O casco chegou a zero: o barco afunda. Quem estava a bordo cai na água; o que estava nos
     * baús boia, e o barco (sem os baús, com o resto das melhorias) boia também.
     */
    public void afundar(Boat b, String motivo) {
        Location l = b.getLocation();
        World w = l.getWorld();
        List<Player> gente = aBordo(b);
        List<ItemStack> soltos = new ArrayList<>(melhorias.esvaziarBaus(b));
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        List<Integer> baus = MelhoriasBarco.baus(pdc);
        for (int i = 0; i < baus.size(); i++) soltos.add(new ItemStack(Material.CHEST));
        pdc.remove(MelhoriasBarco.K_BAUS);
        pdc.remove(MelhoriasBarco.K_ANCORADO);
        pdc.set(MelhoriasBarco.K_VIDA, PersistentDataType.INTEGER, 1);
        soltos.add(comoItem(b));
        desfazer(b);
        b.eject();
        b.remove();
        for (ItemStack i : soltos) w.dropItemNaturally(l.clone().add(0, 0.6, 0), i);
        w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.6f);
        w.playSound(l, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
        w.spawnParticle(Particle.BLOCK, l.clone().add(0, 0.5, 0), 60, 1.2, 0.4, 1.2, Material.SPRUCE_PLANKS.createBlockData());
        for (Player p : gente) p.showTitle(net.kyori.adventure.title.Title.title(Component.text("⛵ O barco afundou!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(motivo + ". Pegue o que está boiando.", NamedTextColor.GRAY)));
    }

    /** Tira as peças e os assentos (quem estava sentado desce). */
    private void desfazer(Boat b) {
        ItemDisplay[] a = assentos.remove(b.getUniqueId());
        if (a != null) for (ItemDisplay d : a) if (d != null) {
            d.eject();
            d.remove();
        }
        tirarPecas(b);
        melhorias.esquecer(b.getUniqueId());
    }

    // =====================================================================
    //  Peças que entram e saem com o barco montado
    // =====================================================================

    void porBau(Boat b, int banco) {
        b.addPassenger(bau(b.getWorld(), b.getLocation(), banco));
        moverAreaDeCliqueParaOFim(b);
    }

    void tirarBau(Boat b, int banco) {
        for (Entity x : List.copyOf(b.getPassengers())) {
            Integer n = x.getPersistentDataContainer().get(K_BAU_DISPLAY, PersistentDataType.INTEGER);
            if (n != null && n == banco) {
                b.removePassenger(x);
                x.remove();
            }
        }
    }

    /** Põe a vela de novo (com o estandarte atual). */
    void trocarVela(Boat b) {
        for (Entity x : List.copyOf(b.getPassengers())) {
            if (x.getPersistentDataContainer().has(Velas.K_DISPLAY)) {
                b.removePassenger(x);
                x.remove();
            }
        }
        if (Velas.temVela(b)) {
            b.addPassenger(mastro(b.getWorld(), b.getLocation(), b));
            moverAreaDeCliqueParaOFim(b);
        }
    }

    void tirarVela(Boat b, Player p) {
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        pdc.remove(Velas.CHAVE);
        byte[] est = pdc.get(MelhoriasBarco.K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
        pdc.remove(MelhoriasBarco.K_ESTANDARTE);
        trocarVela(b);
        for (ItemStack s : p.getInventory().addItem(Velas.item()).values()) p.getWorld().dropItem(p.getLocation(), s);
        if (est != null) for (ItemStack s : p.getInventory().addItem(ItemStack.deserializeBytes(est)).values()) p.getWorld().dropItem(p.getLocation(), s);
        p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.8f);
    }

    // =====================================================================
    //  Subir, a vela, os baús e o laço
    // =====================================================================

    /** O Barco Grande da entidade: o do banco onde ela está (ou o próprio barco, para o piloto); ou null. */
    public Boat barcoDe(Entity e) {
        Entity v = e.getVehicle();
        if (v instanceof Boat b && eBarcoGrande(b)) return b;
        if (v != null && v.getPersistentDataContainer().has(K_ASSENTO)) {
            for (Map.Entry<UUID, ItemDisplay[]> en : assentos.entrySet()) {
                for (ItemDisplay d : en.getValue()) if (d == v && Bukkit.getEntity(en.getKey()) instanceof Boat b) return b;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEntityEvent e) {
        Entity alvo = e.getRightClicked();
        Boat b = alvo instanceof Interaction && alvo.getPersistentDataContainer().has(K_PARTE) && alvo.getVehicle() instanceof Boat v ? v
                : eBarcoGrande(alvo) ? (Boat) alvo : null;
        if (b == null || !plugin.settings().barcoGrande) return;
        Player p = e.getPlayer();
        boolean areaDeClique = alvo instanceof Interaction;
        if (e.getHand() != EquipmentSlot.HAND) {
            if (areaDeClique) e.setCancelled(true);
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();

        // O baú do convés: abre para quem está a bordo ou de fora.
        int bau = bauMirado(p, b);
        if (bau >= 0 && !(p.isSneaking() && mao.getType().isAir())) {
            e.setCancelled(true);
            melhorias.abrirBau(p, b, bau);
            return;
        }
        boolean aBordo = barcoDe(p) == b;
        if (aBordo) {
            if (p.isSneaking() && mao.getType().isAir()) {
                e.setCancelled(true);
                melhorias.abrirMenu(p, b);
            }
            return;
        }
        if (p.getVehicle() != null && p.getVehicle().getPersistentDataContainer().has(K_ASSENTO)) return;

        // Laço: no casco, o jogo cuida; na área de clique, o plugin faz o mesmo que o jogo.
        if (mao.getType() == Material.LEAD && !p.isSneaking()) {
            if (!areaDeClique) return;
            e.setCancelled(true);
            if (!b.isLeashed() && b.setLeashHolder(p)) {
                if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
                b.getWorld().playSound(b.getLocation(), Sound.ITEM_LEAD_TIED, 1f, 1f);
            }
            return;
        }
        if (p.isSneaking() && amarrarNoBarco(p, b, areaDeClique)) {
            e.setCancelled(true);
            return;
        }

        if (Velas.eVela(mao) && !Velas.temVela(b)) {
            e.setCancelled(true);
            if (MelhoriasBarco.baus(b.getPersistentDataContainer()).contains(BANCO_DO_MASTRO)) {
                p.sendActionBar(Component.text("⛵ O banco do mastro tem um baú: tire o baú pelo menu do barco.", NamedTextColor.RED));
                return;
            }
            if (ocupado(b, BANCO_DO_MASTRO)) {
                p.sendActionBar(Component.text("⛵ O banco do mastro está ocupado: peça para a pessoa trocar de lugar.", NamedTextColor.RED));
                return;
            }
            if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
            b.getPersistentDataContainer().set(Velas.CHAVE, PersistentDataType.BYTE, (byte) 1);
            trocarVela(b);
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.2f);
            p.sendActionBar(Component.text("⛵ Vela posta: o mastro ocupa um banco (" + lugares(b) + " lugares). Reme para a frente na água.", COR));
            return;
        }
        if (!mao.getType().isAir() && melhorias.instalar(p, b, mao, this)) {
            e.setCancelled(true);
            return;
        }
        if (p.isSneaking() && mao.getType().isAir()) {
            e.setCancelled(true);
            melhorias.abrirMenu(p, b);
            return;
        }
        e.setCancelled(true);
        if (embarcarBichos(p, b) > 0) return;
        embarcar(p, b);
    }

    /** O banco do baú para onde o jogador está olhando (ou -1). */
    private int bauMirado(Player p, Boat b) {
        List<Integer> baus = MelhoriasBarco.baus(b.getPersistentDataContainer());
        if (baus.isEmpty()) return -1;
        Location olho = p.getEyeLocation();
        Vector dir = olho.getDirection();
        int melhor = -1;
        double menor = 0.5 * 0.5;
        for (int i : baus) {
            Vector c = posicao(b, i).add(0, 0.2, 0).toVector().subtract(olho.toVector());
            double t = c.dot(dir);
            if (t < 0 || t > 6) continue;
            double d2 = c.clone().subtract(dir.clone().multiply(t)).lengthSquared();
            if (d2 < menor) {
                menor = d2;
                melhor = i;
            }
        }
        return melhor;
    }

    /** Agachado com barcos amarrados em si: passa o laço deles para este barco (o comboio). */
    private boolean amarrarNoBarco(Player p, Boat b, boolean areaDeClique) {
        if (!areaDeClique) return false;
        boolean algum = false;
        for (Entity x : p.getNearbyEntities(12, 6, 12)) {
            if (x instanceof Boat outro && outro != b && outro.isLeashed() && outro.getLeashHolder() == p) {
                algum |= outro.setLeashHolder(b);
            }
        }
        if (algum) b.getWorld().playSound(b.getLocation(), Sound.ITEM_LEAD_TIED, 1f, 1f);
        return algum;
    }

    private void moverAreaDeCliqueParaOFim(Boat b) {
        for (Entity x : List.copyOf(b.getPassengers())) {
            if (x instanceof Interaction && x.getPersistentDataContainer().has(K_PARTE)) {
                b.removePassenger(x);
                b.addPassenger(x);
            }
        }
    }

    boolean ocupado(Boat b, int banco) {
        ItemDisplay[] a = assentos.get(b.getUniqueId());
        return a != null && a[banco] != null && a[banco].isValid() && !a[banco].getPassengers().isEmpty();
    }

    private void embarcar(Player p, Boat b) {
        if (piloto(b) == null) {
            b.addPassenger(p);
            pilotoNaFrente(b);
            p.sendActionBar(Component.text("⛵ Você pilota o Barco Grande: " + lotacao(b) + " de " + lugares(b) + " lugares.", COR));
            return;
        }
        if (sentar(p, b)) {
            p.sendActionBar(Component.text("⛵ Você sentou no Barco Grande: " + lotacao(b) + " de " + lugares(b) + " lugares.", COR));
            return;
        }
        p.sendActionBar(Component.text("⛵ O Barco Grande está cheio.", NamedTextColor.RED));
    }

    /** Senta a entidade (gente ou bicho) no primeiro banco livre. */
    private boolean sentar(Entity quem, Boat b) {
        ItemDisplay[] a = assentos.computeIfAbsent(b.getUniqueId(), k -> new ItemDisplay[BANCOS.length]);
        for (int i = 0; i < BANCOS.length; i++) {
            if (fechado(b, i) || ocupado(b, i)) continue;
            if (a[i] != null) a[i].remove();
            ItemDisplay s = b.getWorld().spawn(posicao(b, i), ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.setTeleportDuration(2);
                d.getPersistentDataContainer().set(K_ASSENTO, PersistentDataType.BYTE, (byte) 1);
            });
            s.addPassenger(quem);
            a[i] = s;
            return true;
        }
        return false;
    }

    /**
     * Quem clica no barco com bichos no laço (companheiro do Domador, cachorro, gato...) põe os
     * bichos a bordo, um por banco livre; os laços voltam para a mão.
     */
    private int embarcarBichos(Player p, Boat b) {
        int n = 0;
        for (Entity x : p.getNearbyEntities(10, 5, 10)) {
            if (!(x instanceof LivingEntity le) || x instanceof Player || x instanceof Boat || !le.isLeashed() || le.getLeashHolder() != p) continue;
            if (le.getVehicle() != null) continue;
            if (!sentar(le, b)) {
                p.sendActionBar(Component.text("⛵ Não há banco livre para o bicho.", NamedTextColor.RED));
                break;
            }
            le.setLeashHolder(null);
            if (p.getGameMode() != GameMode.CREATIVE) for (ItemStack s : p.getInventory().addItem(new ItemStack(Material.LEAD)).values()) p.getWorld().dropItem(p.getLocation(), s);
            n++;
        }
        if (n > 0) {
            b.getWorld().playSound(b.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.6f, 1.2f);
            p.sendActionBar(Component.text("⛵ " + (n == 1 ? "O bicho subiu" : n + " bichos subiram") + " no barco. Clique de novo para subir você.", COR));
        }
        return n;
    }

    /** Põe os bichos de volta na água ao lado do barco. */
    int desembarcarBichos(Boat b) {
        int n = 0;
        ItemDisplay[] a = assentos.get(b.getUniqueId());
        if (a == null) return 0;
        for (int i = 0; i < a.length; i++) {
            ItemDisplay d = a[i];
            if (d == null || !d.isValid()) continue;
            for (Entity x : List.copyOf(d.getPassengers())) {
                if (x instanceof Player) continue;
                d.removePassenger(x);
                x.teleport(b.getLocation().add(0, 0.8, 0));
                n++;
            }
            if (d.getPassengers().isEmpty()) {
                d.remove();
                a[i] = null;
            }
        }
        return n;
    }

    /** Diferença de ângulo entre -180 e 180. */
    private static float giro(float a) {
        a %= 360f;
        if (a > 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    /** Onde fica o banco i agora (o barco anda e vira). */
    private static Location posicao(Boat b, int i) {
        Location l = b.getLocation();
        double yaw = Math.toRadians(l.getYaw());
        double x = BANCOS[i][0], z = BANCOS[i][1];
        // No barco, +z é a frente (-sen, cos) e +x é (cos, sen), como nos blocos desenhados.
        double wx = x * Math.cos(yaw) - z * Math.sin(yaw), wz = x * Math.sin(yaw) + z * Math.cos(yaw);
        Location r = l.clone().add(wx, ALTURA_ASSENTO, wz);
        r.setPitch(0);
        return r;
    }

    @EventHandler
    public void aoDescer(EntityDismountEvent e) {
        Entity v = e.getDismounted();
        if (!(e.getEntity() instanceof Player) || !v.getPersistentDataContainer().has(K_ASSENTO)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (v.getPassengers().isEmpty()) v.remove();
        });
    }

    /** Dica do comboio: quem amarra um barco fica sabendo que dá para prender num outro. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAmarrar(PlayerLeashEntityEvent e) {
        if (!(e.getEntity() instanceof Boat) || !plugin.settings().barcoGrande) return;
        e.getPlayer().sendActionBar(Component.text("🪢 Agachado + clique em outro barco: este fica amarrado nele (comboio).", COR));
    }

    // =====================================================================
    //  Quebrar, sumir e carregar
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(VehicleDestroyEvent e) {
        if (!eBarcoGrande(e.getVehicle())) return;
        e.setCancelled(true);
        Boat b = (Boat) e.getVehicle();
        Player quem = e.getAttacker() instanceof Player p ? p : null;
        if (quem != null && lotacao(b) > 0) {
            quem.sendActionBar(Component.text("⛵ Tem gente ou bicho a bordo: todos precisam descer para recolher o barco.", NamedTextColor.RED));
            return;
        }
        if (quem == null) return; // só gente recolhe o barco (bichos, flechas e raios não)
        b.eject();
        recolher(b, quem);
    }

    @EventHandler
    public void aoSumir(EntityRemoveFromWorldEvent e) {
        if (!eBarcoGrande(e.getEntity())) return;
        Boat b = (Boat) e.getEntity();
        melhorias.fecharBaus(b);
        ItemDisplay[] a = assentos.remove(b.getUniqueId());
        if (a != null) for (ItemDisplay d : a) if (d != null) {
            d.eject();
            d.remove();
        }
        for (Entity x : List.copyOf(b.getPassengers())) if (x.getPersistentDataContainer().has(K_PARTE)) x.remove();
        melhorias.esquecer(b.getUniqueId());
    }

    @EventHandler
    public void aoCarregar(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) {
            if (eBarcoGrande(x)) Bukkit.getScheduler().runTask(plugin, () -> { if (x.isValid()) montar((Boat) x); });
            else if (x.getPersistentDataContainer().has(K_PARTE) || x.getPersistentDataContainer().has(K_ASSENTO)) x.remove();
        }
    }

    /** Na partida: monta os Barcos Grandes que já estão carregados. */
    public void restaurar() {
        for (World w : Bukkit.getWorlds()) {
            for (Boat b : w.getEntitiesByClass(Boat.class)) if (eBarcoGrande(b)) montar(b);
        }
    }

    /** Na saída: as peças não ficam salvas no mundo. */
    public void desligar() {
        melhorias.desligar();
        for (World w : Bukkit.getWorlds()) {
            for (Boat b : w.getEntitiesByClass(Boat.class)) if (eBarcoGrande(b)) desfazer(b);
        }
    }

    /** Os Barcos Grandes montados agora (para a rede, a âncora e os perigos do mar). */
    public List<Boat> montados() {
        List<Boat> l = new ArrayList<>();
        for (UUID u : montados) if (Bukkit.getEntity(u) instanceof Boat b && b.isValid()) l.add(b);
        return l;
    }

    // =====================================================================
    //  Os bancos andam com o barco
    // =====================================================================

    /** A cada tick: leva os assentos ocupados para o lugar do banco; segura o barco ancorado; a rede (1x por segundo). */
    public void tick() {
        boolean segundo = Bukkit.getCurrentTick() % 20 == 0;
        // O jogo vira as peças quando o piloto rema para o lado; aqui elas acertam o resto (empurrões, comandos).
        for (Iterator<UUID> it = montados.iterator(); it.hasNext(); ) {
            if (!(Bukkit.getEntity(it.next()) instanceof Boat b) || !b.isValid()) {
                it.remove();
                continue;
            }
            melhorias.segurar(b);
            if (segundo) melhorias.puxarRede(b);
            float yaw = b.getLocation().getYaw();
            for (Entity x : b.getPassengers()) {
                if (x instanceof Display && x.getPersistentDataContainer().has(K_PARTE) && Math.abs(giro(x.getLocation().getYaw() - yaw)) > 0.5f) {
                    x.setRotation(yaw, 0);
                }
            }
        }
        if (assentos.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, ItemDisplay[]>> it = assentos.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ItemDisplay[]> en = it.next();
            Entity e = Bukkit.getEntity(en.getKey());
            ItemDisplay[] a = en.getValue();
            boolean algum = false;
            for (int i = 0; i < a.length; i++) {
                ItemDisplay d = a[i];
                if (d == null) continue;
                if (!(e instanceof Boat b) || !b.isValid() || !d.isValid() || d.getPassengers().isEmpty()) {
                    d.eject();
                    d.remove();
                    a[i] = null;
                    continue;
                }
                algum = true;
                Location alvo = posicao(b, i), atual = d.getLocation();
                if (alvo.distanceSquared(atual) > 1e-6 || Math.abs(giro(alvo.getYaw() - atual.getYaw())) > 0.1f) {
                    d.teleport(alvo, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                }
            }
            if (!algum) it.remove();
        }
    }
}
