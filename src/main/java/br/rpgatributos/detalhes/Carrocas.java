package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carroça (2.67): um barco com baú do próprio jogo (de abeto) que um cavalo, mula, burro, lhama ou
 * camelo domado puxa. Laço na carroça, depois clique no animal: fica atrelada. Clique abre a carga;
 * agachado + clique solta do animal (ou senta, com o Banco). Batendo, ela quebra e volta a ser item
 * (a carga cai, como num barco com baú).
 * <p>Movimento: nada de peças desenhadas seguindo o animal. A carroça é uma entidade do jogo e o
 * plugin só ajusta a velocidade dela, a cada tick, enquanto está atrelada e longe do lugar atrás
 * do animal. Paradas, não custam nada. Na água, ela boia e vira barco.
 */
public final class Carrocas implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "carroca");
    private static final NamespacedKey K_MELHORIAS = new NamespacedKey("rpgatributos", "carroca_melhorias");
    private static final NamespacedKey K_EXTRA = new NamespacedKey("rpgatributos", "carroca_extra");
    private static final NamespacedKey K_LASTRO = new NamespacedKey("rpgatributos", "carroca_lastro");
    private static final NamespacedKey K_PESO = new NamespacedKey("rpgatributos", "carroca_peso");
    private static final TextColor COR = TextColor.color(0xB98A55);
    /** Quanto da velocidade sobra depois do atrito do barco no chão (medido no servidor de teste). */
    private static final double ATRITO = 0.78;

    /** O que dá para pôr na carroça (clicando nela com o item). */
    public enum Melhoria {
        RODAS("Rodas de ferro", "mais rápida em estrada", Material.IRON_BLOCK, 1),
        BAU("Baú duplo", "54 espaços", Material.CHEST, 1),
        TOLDO("Toldo", "monstros e explosões não a quebram", null, 3),
        LANTERNA("Lanterna", "ilumina em volta", Material.LANTERN, 1),
        FREIO("Freio", "não escorrega parada", Material.LEVER, 1),
        BANCO("Banco", "leva uma pessoa sentada", null, 1);

        public final String nome, efeito;
        final Material item;
        final int qtd;

        Melhoria(String nome, String efeito, Material item, int qtd) {
            this.nome = nome;
            this.efeito = efeito;
            this.item = item;
            this.qtd = qtd;
        }

        /** O item na mão serve para esta melhoria? */
        boolean serve(ItemStack i) {
            if (i == null || i.isEmpty()) return false;
            return switch (this) {
                case TOLDO -> Tag.WOOL.isTagged(i.getType());
                case BANCO -> Tag.WOODEN_SLABS.isTagged(i.getType());
                default -> i.getType() == item;
            };
        }

        String comoPor() {
            return switch (this) {
                case RODAS -> "1 bloco de ferro";
                case BAU -> "1 baú";
                case TOLDO -> "3 lãs";
                case LANTERNA -> "1 lanterna";
                case FREIO -> "1 alavanca";
                case BANCO -> "1 laje de madeira";
            };
        }
    }

    /** As estradas: aqui as Rodas de ferro rendem. */
    private static final Set<Material> ESTRADA = EnumSet.of(Material.DIRT_PATH, Material.GRAVEL, Material.COBBLESTONE,
            Material.STONE_BRICKS, Material.STONE, Material.SMOOTH_STONE, Material.ANDESITE, Material.POLISHED_ANDESITE,
            Material.COBBLED_DEEPSLATE, Material.MUD_BRICKS, Material.BRICKS, Material.SANDSTONE, Material.SMOOTH_SANDSTONE,
            Material.COARSE_DIRT, Material.PACKED_MUD);

    /** A janela de carga de uma carroça com baú duplo. */
    private static final class Carga implements InventoryHolder {
        Inventory inv;
        UUID carroca;
        @Override public Inventory getInventory() { return inv; }
    }

    private final RPGAtributos plugin;
    /** Carroças atreladas (as únicas que o plugin acompanha). */
    private final Set<UUID> atreladas = new HashSet<>();
    /** Quem está com a carga aberta (uma pessoa por vez, para nada duplicar). */
    private final Map<UUID, UUID> abertas = new HashMap<>();
    /** A luz da Lanterna de cada carroça e quem a vê. */
    private final Map<UUID, Location> luzes = new HashMap<>();
    private final Map<UUID, Integer> pulos = new HashMap<>();
    private int ticks;

    public Carrocas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  O item e a receita
    // =====================================================================

    public static ItemStack item(Set<Melhoria> melhorias) {
        ItemStack i = new ItemStack(Material.SPRUCE_CHEST_BOAT);
        i.editMeta(m -> {
            m.itemName(Component.text("Carroça", COR));
            List<Component> lore = new ArrayList<>(List.of(
                    Component.text("Clique no chão para pôr. Laço na carroça e", NamedTextColor.GRAY),
                    Component.text("depois no cavalo (ou mula, burro, lhama, camelo).", NamedTextColor.GRAY),
                    Component.text("Clique: a carga. Agachado: solta (ou senta).", NamedTextColor.DARK_GRAY)));
            if (!melhorias.isEmpty()) {
                lore.add(Component.empty());
                for (Melhoria x : melhorias) lore.add(Component.text("✦ " + x.nome + ": " + x.efeito, COR));
            }
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
            m.getPersistentDataContainer().set(K_MELHORIAS, PersistentDataType.STRING, texto(melhorias));
        });
        return i;
    }

    public static boolean eItem(ItemStack i) {
        return i != null && i.getType() == Material.SPRUCE_CHEST_BOAT && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public static boolean eCarroca(Entity e) {
        return e instanceof ChestBoat && e.getPersistentDataContainer().has(CHAVE);
    }

    private static String texto(Set<Melhoria> m) {
        return String.join(",", m.stream().map(Enum::name).toList());
    }

    private static Set<Melhoria> melhorias(String s) {
        Set<Melhoria> r = EnumSet.noneOf(Melhoria.class);
        if (s == null || s.isBlank()) return r;
        for (String x : s.split(",")) {
            try { r.add(Melhoria.valueOf(x.trim())); } catch (IllegalArgumentException ignored) { }
        }
        return r;
    }

    private static Set<Melhoria> melhorias(Entity e) {
        return melhorias(e.getPersistentDataContainer().get(K_MELHORIAS, PersistentDataType.STRING));
    }

    public void registrarReceitas() {
        List<Material> barcos = new ArrayList<>();
        for (Material m : Material.values()) {
            String n = m.name();
            if (!m.isLegacy() && m.isItem() && (n.endsWith("_BOAT") || n.endsWith("_RAFT")) && !n.contains("CHEST")) barcos.add(m);
        }
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "carroca"), item(EnumSet.noneOf(Melhoria.class)));
        r.shape(" C ", "PBP", "S S");
        r.setIngredient('C', Material.CHEST);
        r.setIngredient('P', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        r.setIngredient('B', new RecipeChoice.MaterialChoice(barcos));
        r.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(r);
    }

    // =====================================================================
    //  Pôr no chão e quebrar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (!eItem(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!plugin.settings().carroca || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getBlockFace() == null) return;
        Block onde = e.getClickedBlock().getRelative(e.getBlockFace());
        if (!onde.isPassable()) return;
        if (!plugin.territorios().podeConstruir(p, onde)) {
            p.sendActionBar(Component.text("🛒 Aqui é um território que não é seu.", NamedTextColor.RED));
            return;
        }
        ItemStack mao = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        Set<Melhoria> m = melhorias(mao.getPersistentDataContainer().get(K_MELHORIAS, PersistentDataType.STRING));
        Location l = onde.getLocation().add(0.5, 0.05, 0.5);
        l.setYaw(p.getLocation().getYaw());
        ChestBoat c = (ChestBoat) p.getWorld().spawnEntity(l, EntityType.SPRUCE_CHEST_BOAT);
        c.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        c.getPersistentDataContainer().set(K_MELHORIAS, PersistentDataType.STRING, texto(m));
        // 3.14: o desenho da carroça é do plugin (peças presas a ela), sem mod.
        montarModelo(c);
        c.setCustomNameVisible(false);
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        p.playSound(l, Sound.BLOCK_WOOD_PLACE, 1f, 0.8f);
        p.sendActionBar(Component.text("🛒 Carroça no chão: laço nela e depois clique num cavalo para atrelar.", COR));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoDispensar(BlockDispenseEvent e) {
        if (eItem(e.getItem())) e.setCancelled(true);
    }

    /** Toldo: só jogador quebra a carroça (monstros, flechas de esqueleto e explosões não). */
    @EventHandler(ignoreCancelled = true)
    public void aoApanhar(VehicleDamageEvent e) {
        if (!eCarroca(e.getVehicle()) || !melhorias(e.getVehicle()).contains(Melhoria.TOLDO)) return;
        Entity a = e.getAttacker();
        boolean jogador = a instanceof Player || a instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player;
        if (!jogador) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(VehicleDestroyEvent e) {
        if (!eCarroca(e.getVehicle())) return;
        e.setCancelled(true);
        ChestBoat c = (ChestBoat) e.getVehicle();
        Location l = c.getLocation();
        World w = l.getWorld();
        for (ItemStack i : c.getInventory().getContents()) if (i != null && !i.isEmpty()) w.dropItemNaturally(l, i);
        for (ItemStack i : extra(c)) if (i != null && !i.isEmpty()) w.dropItemNaturally(l, i);
        c.getInventory().clear();
        c.getPersistentDataContainer().remove(K_EXTRA);
        Set<Melhoria> m = melhorias(c);
        soltar(c, false);
        tirarLastro(c);
        tirarModelo(c);
        c.eject();
        c.remove();
        w.dropItemNaturally(l, item(m));
        w.playSound(l, Sound.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
    }

    // =====================================================================
    //  Clicar na carroça e no animal
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEntityEvent e) {
        if (!plugin.settings().carroca) return;
        Player p = e.getPlayer();
        Entity alvo = e.getRightClicked();
        if (alvo instanceof AbstractHorse cavalo) {
            if (e.getHand() != EquipmentSlot.HAND) return;
            atrelarNoAnimal(e, p, cavalo);
            return;
        }
        if (!eCarroca(alvo)) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        ChestBoat c = (ChestBoat) alvo;
        ItemStack mao = p.getInventory().getItemInMainHand();
        // Laço: o jogo prende a carroça em você.
        if (mao.getType() == Material.LEAD) {
            if (c.isLeashed()) return;
            if (c.setLeashHolder(p)) {
                if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
                c.getWorld().playSound(c.getLocation(), Sound.ITEM_LEAD_TIED, 1f, 1f);
                p.sendActionBar(Component.text("🛒 Agora clique num cavalo, mula, burro, lhama ou camelo domado.", COR));
            }
            return;
        }
        // Melhorias.
        Set<Melhoria> m = melhorias(c);
        for (Melhoria x : Melhoria.values()) {
            if (!x.serve(mao)) continue;
            if (m.contains(x)) {
                p.sendActionBar(Component.text("🛒 A carroça já tem " + x.nome + ".", NamedTextColor.GRAY));
                return;
            }
            if (mao.getAmount() < x.qtd && p.getGameMode() != GameMode.CREATIVE) {
                p.sendActionBar(Component.text("🛒 " + x.nome + " pede " + x.comoPor() + ".", NamedTextColor.RED));
                return;
            }
            if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - x.qtd);
            m.add(x);
            c.getPersistentDataContainer().set(K_MELHORIAS, PersistentDataType.STRING, texto(m));
            montarModelo(c);
            c.getWorld().playSound(c.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.8f, 1.2f);
            p.sendActionBar(Component.text("🛒 " + x.nome + ": " + x.efeito + ".", COR));
            return;
        }
        if (p.isSneaking()) {
            if (c.isLeashed()) {
                soltar(c, true);
                p.sendActionBar(Component.text("🛒 Carroça solta (o laço ficou no chão).", COR));
            } else if (m.contains(Melhoria.BANCO)) {
                sentar(p, c);
            } else {
                p.sendActionBar(Component.text("🛒 Para levar alguém sentado, ponha um Banco (laje de madeira).", NamedTextColor.GRAY));
            }
            return;
        }
        abrir(p, c);
    }

    /** Clique no animal com uma carroça presa no laço da mão: ela passa para o animal. */
    private void atrelarNoAnimal(PlayerInteractEntityEvent e, Player p, AbstractHorse cavalo) {
        ChestBoat achada = null;
        for (Entity x : p.getNearbyEntities(10, 5, 10)) {
            if (eCarroca(x) && ((ChestBoat) x).isLeashed() && ((ChestBoat) x).getLeashHolder() == p) {
                achada = (ChestBoat) x;
                break;
            }
        }
        if (achada == null) return;
        e.setCancelled(true);
        if (!cavalo.isTamed()) {
            p.sendActionBar(Component.text("🛒 Só um animal domado puxa a carroça.", NamedTextColor.RED));
            return;
        }
        if (achada.setLeashHolder(cavalo)) {
            atreladas.add(achada.getUniqueId());
            peso(cavalo, true);
            cavalo.getWorld().playSound(cavalo.getLocation(), Sound.ITEM_LEAD_TIED, 1f, 0.9f);
            p.sendActionBar(Component.text("🛒 Carroça atrelada. Agachado + clique nela solta.", COR));
        }
    }

    /** Solta do animal (o jogo deixa o laço no chão, ao lado da carroça). */
    private void soltar(ChestBoat c, boolean devolverLaco) {
        Entity dono = c.isLeashed() ? c.getLeashHolder() : null;
        if (dono != null) {
            c.setLeashHolder(null);
            if (dono instanceof AbstractHorse h) peso(h, false);
        }
        atreladas.remove(c.getUniqueId());
        apagarLuz(c.getUniqueId());
    }

    /** O peso da carga: o animal fica 15% mais lento enquanto puxa. */
    private static void peso(AbstractHorse h, boolean puxando) {
        AttributeInstance a = h.getAttribute(Attribute.MOVEMENT_SPEED);
        if (a == null) return;
        if (a.getModifier(K_PESO) != null) a.removeModifier(K_PESO);
        if (puxando) a.addTransientModifier(new AttributeModifier(K_PESO, -0.15, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
    }

    // =====================================================================
    //  3.14: o modelo da carroça (peças desenhadas presas a ela, sem mod)
    // =====================================================================

    private static final NamespacedKey K_PECA = new NamespacedKey("rpgatributos", "carroca_peca");
    /** Onde o jogo põe os passageiros do barco (do 2º em diante): as peças compensam isso. */
    private static final float PRESO_Y = 0.1875f, PRESO_Z = -0.6f;

    /** Monta (de novo) o desenho da carroça: caixa de tábuas, 4 rodas, varais e o que as melhorias põem. */
    public void montarModelo(ChestBoat c) {
        tirarModelo(c);
        c.customName(null);
        World w = c.getWorld();
        Location l = c.getLocation();
        Set<Melhoria> m = melhorias(c);
        List<Entity> pecas = new ArrayList<>();
        // A base invisível vai no 1º lugar (o lugar que muda de posição); ela também é o "lastro" do Banco.
        pecas.add(w.spawn(l, ItemDisplay.class, d -> {
            marcarPeca(d);
            d.getPersistentDataContainer().set(K_LASTRO, PersistentDataType.BYTE, (byte) 1);
        }));
        var tabua = Material.SPRUCE_PLANKS.createBlockData();
        var tronco = Material.STRIPPED_SPRUCE_LOG.createBlockData();
        var escura = Material.DARK_OAK_PLANKS.createBlockData();
        // A caixa (cobre o casco do barco).
        pecas.add(peca(w, l, tabua, -0.82f, 0.12f, -0.98f, 0.14f, 0.62f, 1.96f));   // lado esquerdo
        pecas.add(peca(w, l, tabua, 0.68f, 0.12f, -0.98f, 0.14f, 0.62f, 1.96f));    // lado direito
        pecas.add(peca(w, l, tabua, -0.82f, 0.12f, -1.0f, 1.64f, 0.62f, 0.14f));    // traseira
        pecas.add(peca(w, l, tabua, -0.82f, 0.12f, 0.86f, 1.64f, 0.62f, 0.14f));    // frente
        pecas.add(peca(w, l, escura, -0.84f, 0.7f, -1.02f, 1.68f, 0.06f, 0.06f));   // beiral de trás
        pecas.add(peca(w, l, escura, -0.84f, 0.7f, 0.96f, 1.68f, 0.06f, 0.06f));    // beiral da frente
        // As rodas: dois quadrados girados (fica um octógono) com o cubo no meio.
        var roda = (m.contains(Melhoria.RODAS) ? Material.POLISHED_BLACKSTONE : Material.STRIPPED_DARK_OAK_LOG).createBlockData();
        var cubo = (m.contains(Melhoria.RODAS) ? Material.IRON_BLOCK : Material.SPRUCE_PLANKS).createBlockData();
        for (float x : new float[]{-0.98f, 0.86f}) for (float z : new float[]{-0.62f, 0.55f}) {
            pecas.add(roda(w, l, roda, x, 0.3f, z, 0.12f, 0.62f, 0));
            pecas.add(roda(w, l, roda, x, 0.3f, z, 0.12f, 0.62f, 45));
            pecas.add(roda(w, l, cubo, x + (x < 0 ? -0.03f : 0.03f), 0.3f, z, 0.12f, 0.16f, 0));
        }
        // Os varais (para o animal) e a barra da frente.
        pecas.add(peca(w, l, tronco, -0.42f, 0.38f, 0.95f, 0.08f, 0.08f, 1.7f));
        pecas.add(peca(w, l, tronco, 0.34f, 0.38f, 0.95f, 0.08f, 0.08f, 1.7f));
        pecas.add(peca(w, l, tronco, -0.42f, 0.38f, 2.55f, 0.84f, 0.08f, 0.08f));
        if (m.contains(Melhoria.BANCO)) {
            pecas.add(peca(w, l, tabua, -0.68f, 0.72f, 0.45f, 1.36f, 0.1f, 0.38f));      // o banco
            pecas.add(peca(w, l, escura, -0.68f, 0.82f, 0.8f, 1.36f, 0.25f, 0.06f));      // o encosto
        }
        if (m.contains(Melhoria.TOLDO)) {
            var lona = Material.WHITE_WOOL.createBlockData();
            for (float z : new float[]{-0.95f, 0.0f, 0.85f}) {
                pecas.add(peca(w, l, tronco, -0.8f, 0.74f, z, 0.06f, 0.75f, 0.06f));
                pecas.add(peca(w, l, tronco, 0.74f, 0.74f, z, 0.06f, 0.75f, 0.06f));
            }
            pecas.add(peca(w, l, lona, -0.86f, 1.48f, -1.0f, 1.72f, 0.06f, 2.0f));        // o teto
            pecas.add(peca(w, l, lona, -0.86f, 1.02f, -1.0f, 0.04f, 0.46f, 2.0f));        // lona do lado esquerdo
            pecas.add(peca(w, l, lona, 0.82f, 1.02f, -1.0f, 0.04f, 0.46f, 2.0f));         // lona do lado direito
        }
        if (m.contains(Melhoria.LANTERNA)) {
            pecas.add(peca(w, l, tronco, 0.66f, 0.74f, 0.88f, 0.06f, 0.75f, 0.06f));
            pecas.add(peca(w, l, Material.LANTERN.createBlockData("[hanging=true]"), 0.5f, 1.05f, 0.95f, 0.38f, 0.38f, 0.38f));
        }
        if (m.contains(Melhoria.BAU)) {
            pecas.add(peca(w, l, Material.BARREL.createBlockData(), -0.62f, 0.62f, -0.35f, 0.5f, 0.5f, 0.5f));
            pecas.add(peca(w, l, Material.HAY_BLOCK.createBlockData(), 0.05f, 0.62f, -0.35f, 0.5f, 0.42f, 0.5f));
        }
        if (m.contains(Melhoria.FREIO))
            pecas.add(peca(w, l, Material.LEVER.createBlockData("[face=floor,facing=north]"), 0.62f, 0.72f, 0.6f, 0.4f, 0.4f, 0.4f));
        // Quem já estava sentado vai para o fim da fila (o lugar dele não muda).
        List<Entity> sentados = new ArrayList<>();
        for (Entity x : List.copyOf(c.getPassengers())) if (x instanceof Player) {
            c.removePassenger(x);
            sentados.add(x);
        }
        for (Entity x : pecas) c.addPassenger(x);
        for (Entity x : sentados) c.addPassenger(x);
    }

    private static void marcarPeca(Entity e) {
        e.setPersistent(false);
        e.getPersistentDataContainer().set(K_PECA, PersistentDataType.BYTE, (byte) 1);
    }

    private static org.bukkit.entity.BlockDisplay peca(World w, Location l, org.bukkit.block.data.BlockData b, float x, float y, float z, float sx, float sy, float sz) {
        return w.spawn(l, org.bukkit.entity.BlockDisplay.class, d -> {
            d.setBlock(b);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(x, y - PRESO_Y, z - PRESO_Z), new org.joml.AxisAngle4f(),
                    new org.joml.Vector3f(sx, sy, sz), new org.joml.AxisAngle4f()));
            marcarPeca(d);
        });
    }

    /**
     * Uma roda: um bloco fino (espessura {@code larg}, lado {@code lado}) girado {@code graus} em volta do
     * eixo da roda, com o centro em (x + larg/2, y, z).
     */
    private static org.bukkit.entity.BlockDisplay roda(World w, Location l, org.bukkit.block.data.BlockData b, float x, float y, float z, float larg, float lado, float graus) {
        double a = Math.toRadians(graus);
        float h = lado / 2;
        // Onde o centro do bloco escalado vai parar depois de girar em volta do eixo X.
        float cy = (float) (h * Math.cos(a) - h * Math.sin(a)), cz = (float) (h * Math.sin(a) + h * Math.cos(a));
        float ty = y - cy, tz = z - cz;
        return w.spawn(l, org.bukkit.entity.BlockDisplay.class, d -> {
            d.setBlock(b);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(x, ty - PRESO_Y, tz - PRESO_Z),
                    new org.joml.Quaternionf().rotateX((float) a), new org.joml.Vector3f(larg, lado, lado), new org.joml.Quaternionf()));
            marcarPeca(d);
        });
    }

    private static void tirarModelo(ChestBoat c) {
        for (Entity x : List.copyOf(c.getPassengers())) {
            if (x.getPersistentDataContainer().has(K_PECA)) {
                c.removePassenger(x);
                x.remove();
            }
        }
    }

    /**
     * Banco: a pessoa vai sentada em cima da carga. Um lastro (invisível) fica no 1º lugar, para
     * ninguém virar "piloto" da carroça: quem a move é o animal (e o plugin), não quem senta.
     */
    private void sentar(Player p, ChestBoat c) {
        if (c.getPassengers().stream().anyMatch(x -> x instanceof Player)) {
            p.sendActionBar(Component.text("🛒 O banco já está ocupado.", NamedTextColor.GRAY));
            return;
        }
        if (c.getPassengers().stream().noneMatch(x -> x.getPersistentDataContainer().has(K_LASTRO))) {
            ItemDisplay lastro = c.getWorld().spawn(c.getLocation(), ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.getPersistentDataContainer().set(K_LASTRO, PersistentDataType.BYTE, (byte) 1);
            });
            c.addPassenger(lastro);
        }
        c.addPassenger(p);
    }

    private static void tirarLastro(ChestBoat c) {
        for (Entity x : List.copyOf(c.getPassengers())) {
            if (x.getPersistentDataContainer().has(K_LASTRO) && !x.getPersistentDataContainer().has(K_PECA)) {
                c.removePassenger(x);
                x.remove();
            }
        }
    }

    /** Ninguém entra como no barco (clicar abre a carga); sentar é só pelo Banco. */
    @EventHandler(ignoreCancelled = true)
    public void aoEntrar(VehicleEnterEvent e) {
        if (!eCarroca(e.getVehicle()) || !(e.getEntered() instanceof org.bukkit.entity.LivingEntity)) return;
        if (e.getEntered() instanceof Player && melhorias(e.getVehicle()).contains(Melhoria.BANCO)
                && e.getVehicle().getPassengers().stream().anyMatch(x -> x.getPersistentDataContainer().has(K_LASTRO))) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void aoDescer(org.bukkit.event.entity.EntityDismountEvent e) {
        if (e.getEntity() instanceof Player && eCarroca(e.getDismounted())) {
            ChestBoat c = (ChestBoat) e.getDismounted();
            Bukkit.getScheduler().runTask(plugin, () -> tirarLastro(c));
        }
    }

    // =====================================================================
    //  A carga
    // =====================================================================

    private static List<ItemStack> extra(ChestBoat c) {
        byte[] b = c.getPersistentDataContainer().get(K_EXTRA, PersistentDataType.BYTE_ARRAY);
        List<ItemStack> l = new ArrayList<>();
        if (b != null) for (ItemStack i : ItemStack.deserializeItemsFromBytes(b)) l.add(i);
        return l;
    }

    private void abrir(Player p, ChestBoat c) {
        UUID quem = abertas.get(c.getUniqueId());
        if (quem != null && !quem.equals(p.getUniqueId()) && Bukkit.getPlayer(quem) != null) {
            p.sendActionBar(Component.text("🛒 Alguém já está mexendo na carga.", NamedTextColor.GRAY));
            return;
        }
        if (!melhorias(c).contains(Melhoria.BAU)) {
            p.openInventory(c.getInventory());
            return;
        }
        Carga h = new Carga();
        h.carroca = c.getUniqueId();
        h.inv = Bukkit.createInventory(h, 54, Component.text("Carroça (baú duplo)", COR));
        ItemStack[] a = c.getInventory().getContents();
        for (int i = 0; i < 27 && i < a.length; i++) h.inv.setItem(i, a[i]);
        List<ItemStack> b = extra(c);
        for (int i = 0; i < 27 && i < b.size(); i++) h.inv.setItem(27 + i, b.get(i));
        c.getInventory().clear();
        c.getPersistentDataContainer().remove(K_EXTRA);
        abertas.put(c.getUniqueId(), p.getUniqueId());
        p.openInventory(h.inv);
    }

    @EventHandler
    public void aoFechar(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Carga h)) return;
        abertas.remove(h.carroca);
        ItemStack[] tudo = h.inv.getContents();
        Entity ent = Bukkit.getEntity(h.carroca);
        if (!(ent instanceof ChestBoat c) || !c.isValid()) {
            // A carroça sumiu com a carga aberta: nada se perde, vai para quem estava mexendo.
            for (ItemStack i : tudo) if (i != null && !i.isEmpty()) for (ItemStack s : e.getPlayer().getInventory().addItem(i).values())
                e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), s);
            return;
        }
        ItemStack[] a = new ItemStack[27], b = new ItemStack[27];
        System.arraycopy(tudo, 0, a, 0, 27);
        System.arraycopy(tudo, 27, b, 0, 27);
        c.getInventory().setContents(a);
        List<ItemStack> l = new ArrayList<>();
        for (ItemStack i : b) l.add(i == null ? new ItemStack(Material.AIR) : i);
        c.getPersistentDataContainer().set(K_EXTRA, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(l));
    }

    // =====================================================================
    //  Andar atrás do animal
    // =====================================================================

    @EventHandler
    public void aoCarregar(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) {
            if (eCarroca(x) && ((ChestBoat) x).isLeashed()) atreladas.add(x.getUniqueId());
            if (eCarroca(x)) Bukkit.getScheduler().runTask(plugin, () -> { if (x.isValid()) montarModelo((ChestBoat) x); });
            if (x.getPersistentDataContainer().has(K_LASTRO) && !(x.getVehicle() instanceof ChestBoat)) x.remove();
        }
    }

    /** Na partida: as carroças atreladas que já estão carregadas. */
    public void restaurar() {
        for (World w : Bukkit.getWorlds()) {
            for (ChestBoat c : w.getEntitiesByClass(ChestBoat.class)) {
                if (eCarroca(c) && c.isLeashed()) atreladas.add(c.getUniqueId());
                if (eCarroca(c)) montarModelo(c);
            }
        }
    }

    public void desligar() {
        for (UUID id : List.copyOf(luzes.keySet())) apagarLuz(id);
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Carga) p.closeInventory();
    }

    /**
     * A cada tick: cada carroça atrelada vai para o lugar 3 blocos atrás do animal. Só a velocidade
     * dela muda (sem teleporte); longe demais (mais de 10 blocos), ela se solta.
     */
    public void tick() {
        if (atreladas.isEmpty()) return;
        ticks++;
        for (Iterator<UUID> it = atreladas.iterator(); it.hasNext(); ) {
            Entity e = Bukkit.getEntity(it.next());
            if (!(e instanceof ChestBoat c) || !c.isValid()) {
                it.remove();
                continue;
            }
            if (!c.isLeashed() || !(c.getLeashHolder() instanceof AbstractHorse h) || !h.isValid()) {
                it.remove();
                apagarLuz(c.getUniqueId());
                continue;
            }
            Location lh = h.getLocation(), lc = c.getLocation();
            if (!lh.getWorld().equals(lc.getWorld()) || lh.distanceSquared(lc) > 100) {
                soltar(c, true);
                it.remove();
                continue;
            }
            Set<Melhoria> m = melhorias(c);
            if (m.contains(Melhoria.LANTERNA) && ticks % 5 == 0) luz(c);
            if (c.getStatus() == Boat.Status.IN_WATER || c.getStatus() == Boat.Status.UNDER_WATER) continue; // na água: o laço do jogo puxa
            Vector atras = lh.getDirection().setY(0);
            if (atras.lengthSquared() < 1e-4) continue;
            atras.normalize().multiply(-3);
            Vector alvo = lh.toVector().add(atras), falta = alvo.subtract(lc.toVector());
            double dy = falta.getY();
            falta.setY(0);
            double dist = falta.length();
            Vector v = c.getVelocity();
            if (dist < 0.6) {
                c.setVelocity(new Vector(v.getX() * 0.5, v.getY(), v.getZ() * 0.5));
                continue;
            }
            Material piso = lc.clone().add(0, -0.2, 0).getBlock().getType();
            boolean estrada = ESTRADA.contains(piso) || br.rpgatributos.estrada.Pisos.nivelEm(lc.clone().add(0, -0.2, 0).getBlock()) > 0;
            // Velocidade de verdade (o chão freia o barco uns 22%: por isso a divisão logo abaixo).
            double max = m.contains(Melhoria.RODAS) && estrada ? 0.9 : estrada ? 0.7 : 0.6;
            Vector nova = falta.normalize().multiply(Math.min(max, dist * 0.3) / ATRITO);
            double vy = v.getY();
            // Degrau: o animal mais alto e a carroça presa: um pulinho (no máximo um a cada meio segundo).
            if (dy > 0.5 && Math.hypot(v.getX(), v.getZ()) < 0.05 && pulos.getOrDefault(c.getUniqueId(), 0) <= ticks) {
                vy = 0.45;
                pulos.put(c.getUniqueId(), ticks + 10);
            }
            nova.setY(vy);
            c.setVelocity(nova);
            c.setRotation(lh.getYaw(), 0);
        }
    }

    /** Freio: parada e solta, a carroça não escorrega (gelo, ladeira). */
    @EventHandler(ignoreCancelled = true)
    public void aoMover(org.bukkit.event.vehicle.VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof ChestBoat c) || !eCarroca(c) || c.isLeashed()) return;
        if (c.getStatus() != Boat.Status.ON_LAND || !melhorias(c).contains(Melhoria.FREIO)) return;
        if (c.getPassengers().stream().anyMatch(x -> x instanceof Player)) return;
        Vector v = c.getVelocity();
        if (Math.abs(v.getX()) + Math.abs(v.getZ()) > 0.001) c.setVelocity(new Vector(0, v.getY(), 0));
    }

    // =====================================================================
    //  Lanterna (luz desenhada só para quem está perto; o mundo não muda)
    // =====================================================================

    private void luz(ChestBoat c) {
        Block b = c.getLocation().add(0, 1, 0).getBlock();
        Location onde = b.getLocation();
        Location antes = luzes.get(c.getUniqueId());
        if (onde.equals(antes)) return;
        apagarLuz(c.getUniqueId());
        if (!b.getType().isAir()) return;
        Light luz = (Light) Material.LIGHT.createBlockData();
        luz.setLevel(14);
        for (Player v : c.getWorld().getPlayers()) if (v.getLocation().distanceSquared(onde) < 48 * 48) v.sendBlockChange(onde, luz);
        luzes.put(c.getUniqueId(), onde);
    }

    private void apagarLuz(UUID id) {
        Location l = luzes.remove(id);
        if (l == null || l.getWorld() == null) return;
        for (Player v : l.getWorld().getPlayers()) if (v.getLocation().distanceSquared(l) < 64 * 64) v.sendBlockChange(l, l.getBlock().getBlockData());
    }

    /** Para o /rpgadmin: a carroça com todas as melhorias. */
    public static ItemStack completa() {
        return item(EnumSet.allOf(Melhoria.class));
    }

    static String nomeMelhorias(Set<Melhoria> m) {
        return String.join(", ", m.stream().map(x -> x.nome.toLowerCase(Locale.ROOT)).toList());
    }
}
