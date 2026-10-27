package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Retoques vanilla+ (2.93), cada um desligável em {@code vanilla-mais}:
 * <ul>
 *   <li><b>Bigorna sem "Caro demais":</b> o custo para em {@code bigorna-custo-maximo} níveis.</li>
 *   <li><b>Consertar a bigorna:</b> clique na bigorna lascada (ou danificada) com um bloco de ferro.</li>
 *   <li><b>Separar encantamentos:</b> no rebolo, livro com vários encantamentos + livro comum tira o
 *       primeiro encantamento para o livro comum, sem perder os outros.</li>
 *   <li><b>XP em garrafas:</b> agachado, clique na mesa de encantamento com um frasco vazio.</li>
 *   <li><b>Moldura invisível:</b> tesoura numa moldura com item esconde a moldura (de novo, mostra).</li>
 *   <li><b>Bater na porta:</b> clique esquerdo com a mão vazia numa porta faz o som de batida.</li>
 * </ul>
 */
public final class Retoques implements Listener {

    private static final TextColor COR = TextColor.color(0xC8E6A0);
    /** Pontos de XP por garrafa: o máximo que uma garrafa devolve (com o bônus do Bibliotecário, ainda não rende mais do que custa). */
    static final int XP_POR_GARRAFA = 11;
    private static final int SLOT_RESULTADO_REBOLO = 2;

    private final RPGAtributos plugin;
    /** Última batida de cada jogador (para não virar metralhadora). */
    private final Map<UUID, Long> ultimaBatida = new HashMap<>();

    public Retoques(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Bigorna sem "Caro demais"
    // =====================================================================

    /** O limite do jogo (40) sai ao abrir; o custo de verdade é cortado no prepararBigorna. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAbrirBigorna(InventoryOpenEvent e) {
        if (plugin.settings().vanillaBigornaMax <= 0 || !(e.getView() instanceof AnvilView v)) return;
        v.setMaximumRepairCost(Integer.MAX_VALUE);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararBigorna(PrepareAnvilEvent e) {
        int max = plugin.settings().vanillaBigornaMax;
        if (max <= 0 || e.getResult() == null || e.getResult().getType().isAir()) return;
        AnvilView v = e.getView();
        v.setMaximumRepairCost(Integer.MAX_VALUE);
        if (v.getRepairCost() > max) v.setRepairCost(max);
    }

    // =====================================================================
    //  Consertar a bigorna com bloco de ferro
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoConsertarBigorna(PlayerInteractEvent e) {
        if (!plugin.settings().vanillaConsertoBigorna || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.useInteractedBlock() == Event.Result.DENY || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        Material antes = b.getType();
        Material depois = antes == Material.DAMAGED_ANVIL ? Material.CHIPPED_ANVIL : antes == Material.CHIPPED_ANVIL ? Material.ANVIL : null;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (depois == null || mao.getType() != Material.IRON_BLOCK || plugin.ehEstacao(b)) return;
        e.setCancelled(true);
        if (!plugin.territorios().podeConstruir(p, b)) return;
        BlockData nova = depois.createBlockData();
        if (b.getBlockData() instanceof Directional velha && nova instanceof Directional d) d.setFacing(velha.getFacing());
        b.setBlockData(nova);
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        p.swingMainHand();
        Location c = b.getLocation().add(0.5, 1.0, 0.5);
        b.getWorld().playSound(c, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        b.getWorld().spawnParticle(Particle.BLOCK, c, 16, 0.3, 0.1, 0.3, 0, Material.IRON_BLOCK.createBlockData());
        p.sendActionBar(Component.text(depois == Material.ANVIL ? "Bigorna consertada." : "Bigorna menos lascada (falta mais um bloco de ferro).", COR));
    }

    // =====================================================================
    //  Separar encantamentos no rebolo
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararRebolo(PrepareGrindstoneEvent e) {
        if (!plugin.settings().vanillaSepararEncantos) return;
        ItemStack r = separado(e.getInventory());
        if (r != null) e.setResult(r);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoTirarDoRebolo(InventoryClickEvent e) {
        if (!plugin.settings().vanillaSepararEncantos || !(e.getClickedInventory() instanceof GrindstoneInventory inv)
                || e.getSlot() != SLOT_RESULTADO_REBOLO || !(e.getWhoClicked() instanceof Player p)) return;
        ItemStack r = separado(inv);
        if (r == null) return;
        e.setCancelled(true);
        boolean shift = e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT;
        if (shift) {
            p.getInventory().addItem(r).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        } else if ((e.getClick() == ClickType.LEFT || e.getClick() == ClickType.RIGHT)
                && (p.getItemOnCursor() == null || p.getItemOnCursor().getType().isAir())) {
            p.setItemOnCursor(r);
        } else {
            return;
        }
        // Tira o encantamento do livro de cima e gasta o livro comum.
        boolean cimaEncantado = encantadoComVarios(inv.getUpperItem());
        ItemStack encantado = cimaEncantado ? inv.getUpperItem() : inv.getLowerItem();
        ItemStack comum = cimaEncantado ? inv.getLowerItem() : inv.getUpperItem();
        Enchantment tirado = r.getItemMeta() instanceof EnchantmentStorageMeta m ? m.getStoredEnchants().keySet().iterator().next() : null;
        ItemStack resto = encantado.clone();
        resto.editMeta(EnchantmentStorageMeta.class, m -> m.removeStoredEnchant(tirado));
        ItemStack sobra = comum.getAmount() > 1 ? comum.clone().subtract() : null;
        if (cimaEncantado) {
            inv.setUpperItem(resto);
            inv.setLowerItem(sobra);
        } else {
            inv.setLowerItem(resto);
            inv.setUpperItem(sobra);
        }
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.1f);
    }

    /** O livro com o primeiro encantamento, se o rebolo tem um livro com 2 ou mais e um livro comum; senão null. */
    private static ItemStack separado(GrindstoneInventory inv) {
        ItemStack cima = inv.getUpperItem(), baixo = inv.getLowerItem();
        ItemStack encantado;
        if (encantadoComVarios(cima) && comum(baixo)) encantado = cima;
        else if (encantadoComVarios(baixo) && comum(cima)) encantado = baixo;
        else return null;
        Map.Entry<Enchantment, Integer> primeiro = ((EnchantmentStorageMeta) encantado.getItemMeta()).getStoredEnchants().entrySet().iterator().next();
        ItemStack r = new ItemStack(Material.ENCHANTED_BOOK);
        r.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(primeiro.getKey(), primeiro.getValue(), true));
        return r;
    }

    private static boolean encantadoComVarios(ItemStack i) {
        return i != null && i.getType() == Material.ENCHANTED_BOOK && i.getAmount() == 1
                && i.getItemMeta() instanceof EnchantmentStorageMeta m && m.getStoredEnchants().size() >= 2;
    }

    private static boolean comum(ItemStack i) {
        return i != null && i.getType() == Material.BOOK && !i.hasItemMeta();
    }

    // =====================================================================
    //  XP em garrafas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoEngarrafar(PlayerInteractEvent e) {
        if (!plugin.settings().vanillaXpGarrafa || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.ENCHANTING_TABLE) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (!p.isSneaking() || mao.getType() != Material.GLASS_BOTTLE || mao.hasItemMeta() || plugin.ehEstacao(e.getClickedBlock())) return;
        e.setCancelled(true);
        int total = p.calculateTotalExperiencePoints();
        if (total < XP_POR_GARRAFA) {
            p.sendActionBar(Component.text("Falta experiência: cada garrafa guarda " + XP_POR_GARRAFA + " pontos.", COR));
            return;
        }
        p.setExperienceLevelAndProgress(total - XP_POR_GARRAFA);
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        p.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE)).values()
                .forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.swingMainHand();
        Location c = e.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        p.playSound(c, Sound.ITEM_BOTTLE_FILL, 0.8f, 1.2f);
        p.playSound(c, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 0.8f);
        p.getWorld().spawnParticle(Particle.ENCHANT, c, 12, 0.3, 0.2, 0.3, 0.5);
    }

    // =====================================================================
    //  Moldura invisível
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoTosquiarMoldura(PlayerInteractEntityEvent e) {
        if (!plugin.settings().vanillaMolduraInvisivel || e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof ItemFrame f)) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.getType() != Material.SHEARS || f.isFixed()) return;
        boolean esconder = f.isVisible();
        if (esconder && f.getItem().getType().isAir()) return; // moldura vazia invisível sumiria de vista
        e.setCancelled(true);
        if (!plugin.territorios().podeConstruir(p, f.getLocation())) return;
        f.setVisible(!esconder);
        if (esconder && p.getGameMode() != GameMode.CREATIVE) mao.damage(1, p);
        p.swingMainHand();
        f.getWorld().playSound(f.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 0.6f, 1.4f);
    }

    /** Tiraram o item de uma moldura invisível: ela volta a aparecer (senão ficaria perdida na parede). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTirarDaMoldura(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof ItemFrame f) || f.isVisible()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (f.isValid() && !f.isVisible() && f.getItem().getType().isAir()) f.setVisible(true);
        });
    }

    // =====================================================================
    //  Bater na porta
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoBater(PlayerInteractEvent e) {
        if (!plugin.settings().vanillaBaterPorta || e.getAction() != Action.LEFT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.getClickedBlock() == null || !Tag.DOORS.isTagged(e.getClickedBlock().getType())) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || !p.getInventory().getItemInMainHand().getType().isAir()) return;
        long agora = System.currentTimeMillis();
        Long antes = ultimaBatida.get(p.getUniqueId());
        if (antes != null && agora - antes < 350) return;
        ultimaBatida.put(p.getUniqueId(), agora);
        Block b = e.getClickedBlock();
        boolean ferro = b.getType() == Material.IRON_DOOR;
        float tom = 1.5f + (float) Math.random() * 0.3f;
        b.getWorld().playSound(b.getLocation().add(0.5, 0.5, 0.5),
                ferro ? Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR : Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, ferro ? 0.25f : 0.35f, tom);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ultimaBatida.remove(e.getPlayer().getUniqueId());
    }
}
