package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

/**
 * Balde de Gelo (2.65): feito com Cristal de Gelo, guarda até 4 baldes de um líquido só (água ou
 * lava; o cristal não derrete). Mirando numa fonte, enche; agachado, ou sem fonte na mira, despeja
 * no bloco mirado. Não serve para leite, peixes nem caldeirão.
 */
final class BaldeGelo implements Listener {

    private static final NamespacedKey K_LIQUIDO = new NamespacedKey("rpgatributos", "balde_liquido");
    private static final NamespacedKey K_QTD = new NamespacedKey("rpgatributos", "balde_qtd");
    static final int MAXIMO = 4;

    private final RPGAtributos plugin;

    BaldeGelo(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Guarda o líquido no balde e mostra no nome e no desenho. */
    static ItemStack montar(ItemStack i, Material liquido, int qtd) {
        Material l = qtd <= 0 ? null : liquido;
        i.editMeta(m -> {
            if (l == null) m.getPersistentDataContainer().remove(K_LIQUIDO);
            else m.getPersistentDataContainer().set(K_LIQUIDO, PersistentDataType.STRING, l.name());
            m.getPersistentDataContainer().set(K_QTD, PersistentDataType.INTEGER, Math.max(0, qtd));
            String nome = Utilidades.Item.BALDE.nome + (l == null ? " (vazio)" : " (" + (l == Material.LAVA ? "lava" : "água") + " " + qtd + "/" + MAXIMO + ")");
            m.itemName(Component.text(nome, Utilidades.COR));
            PacoteRecursos.marcar(m, l == null ? "balde_gelo" : l == Material.LAVA ? "balde_gelo_lava" : "balde_gelo_agua");
        });
        return i;
    }

    private static Material liquido(ItemStack i) {
        String s = i.getPersistentDataContainer().get(K_LIQUIDO, PersistentDataType.STRING);
        return s == null ? null : Material.matchMaterial(s);
    }

    private static int qtd(ItemStack i) {
        return i.getPersistentDataContainer().getOrDefault(K_QTD, PersistentDataType.INTEGER, 0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (!Utilidades.Item.BALDE.e(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true);
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        if (e.getHand() != EquipmentSlot.HAND || !plugin.settings().utilBalde) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        Material liq = liquido(mao);
        int n = qtd(mao);

        // Encher: uma fonte na mira (sem estar agachado).
        RayTraceResult fonte = p.rayTraceBlocks(5, FluidCollisionMode.SOURCE_ONLY);
        Block f = fonte == null ? null : fonte.getHitBlock();
        Material tipoFonte = f == null ? null : fonteDe(f);
        if (!p.isSneaking() && tipoFonte != null && n < MAXIMO && (liq == null || liq == tipoFonte)) {
            if (!plugin.territorios().podeConstruir(p, f)) {
                p.sendActionBar(Component.text("🧊 Essa fonte é de um território que não é seu.", NamedTextColor.RED));
                return;
            }
            if (f.getBlockData() instanceof Waterlogged w && w.isWaterlogged()) {
                w.setWaterlogged(false);
                f.setBlockData(w, true);
            } else {
                f.setType(Material.AIR, true);
            }
            montar(mao, tipoFonte, n + 1);
            p.playSound(f.getLocation(), tipoFonte == Material.LAVA ? Sound.ITEM_BUCKET_FILL_LAVA : Sound.ITEM_BUCKET_FILL, 1f, 1f);
            return;
        }
        if (tipoFonte != null && !p.isSneaking() && n >= MAXIMO) {
            p.sendActionBar(Component.text("🧊 O balde está cheio (" + MAXIMO + "/" + MAXIMO + "): agachado despeja.", NamedTextColor.GRAY));
            return;
        }
        if (tipoFonte != null && !p.isSneaking() && liq != null && liq != tipoFonte) {
            p.sendActionBar(Component.text("🧊 O balde guarda um líquido só: esvazie antes.", NamedTextColor.GRAY));
            return;
        }
        // Despejar: no bloco mirado (do lado da face mirada).
        if (liq == null || n <= 0) return;
        RayTraceResult bloco = p.rayTraceBlocks(5, FluidCollisionMode.NEVER);
        if (bloco == null || bloco.getHitBlock() == null || bloco.getHitBlockFace() == null) return;
        Block alvo = bloco.getHitBlock();
        BlockData dAlvo = alvo.getBlockData();
        Block onde;
        if (liq == Material.WATER && dAlvo instanceof Waterlogged w0 && !w0.isWaterlogged()) onde = alvo;
        else onde = alvo.getRelative(bloco.getHitBlockFace());
        if (!plugin.territorios().podeConstruir(p, onde)) {
            p.sendActionBar(Component.text("🧊 Aqui é um território que não é seu.", NamedTextColor.RED));
            return;
        }
        World w = onde.getWorld();
        if (liq == Material.WATER && w.getEnvironment() == World.Environment.NETHER) {
            w.spawnParticle(Particle.LARGE_SMOKE, onde.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.01);
            w.playSound(onde.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 2f);
            montar(mao, liq, n - 1);
            return;
        }
        if (onde.getBlockData() instanceof Waterlogged wl && liq == Material.WATER && !wl.isWaterlogged()) {
            wl.setWaterlogged(true);
            onde.setBlockData(wl, true);
        } else if (onde.getType().isAir() || onde.isReplaceable() && !onde.isLiquid() || onde.isLiquid() && onde.getBlockData() instanceof Levelled lv && lv.getLevel() > 0) {
            onde.setType(liq, true);
        } else {
            return;
        }
        montar(mao, liq, n - 1);
        p.playSound(onde.getLocation(), liq == Material.LAVA ? Sound.ITEM_BUCKET_EMPTY_LAVA : Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
    }

    /** Água ou lava de fonte (inclusive bloco com água dentro). */
    private static Material fonteDe(Block b) {
        if (b.getBlockData() instanceof Waterlogged w && w.isWaterlogged()) return Material.WATER;
        if ((b.getType() == Material.WATER || b.getType() == Material.LAVA) && b.getBlockData() instanceof Levelled l && l.getLevel() == 0) return b.getType();
        return null;
    }

    // O resto do que um balde faz no jogo não vale para ele (leite, peixes, caldeirão, dispensador).

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoEncher(PlayerBucketFillEvent e) {
        if (Utilidades.Item.BALDE.e(e.getPlayer().getInventory().getItem(e.getHand()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoPegarBicho(PlayerBucketEntityEvent e) {
        if (Utilidades.Item.BALDE.e(e.getOriginalBucket())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoOrdenhar(PlayerInteractEntityEvent e) {
        if ((e.getRightClicked() instanceof org.bukkit.entity.AbstractCow || e.getRightClicked() instanceof org.bukkit.entity.Goat)
                && Utilidades.Item.BALDE.e(e.getPlayer().getInventory().getItem(e.getHand()))) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoDispensar(BlockDispenseEvent e) {
        if (Utilidades.Item.BALDE.e(e.getItem())) e.setCancelled(true);
    }
}
