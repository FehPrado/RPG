package br.rpgatributos.territorio;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A proteção dos territórios: quem não é dono, membro (ou da party, se a regra deixar)
 * não quebra, não coloca, não abre baús, não mexe em animais... E o mundo também
 * respeita a borda: líquidos, pistões, fogo, explosões e mobs que estragam blocos.
 */
public final class ProtecaoListener implements Listener {

    /** Blocos de uso comum que visitantes sempre podem usar. */
    private static final Set<Material> UTILIDADES = EnumSet.of(
            Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.ENDER_CHEST, Material.CARTOGRAPHY_TABLE,
            Material.LOOM, Material.SMITHING_TABLE, Material.STONECUTTER, Material.GRINDSTONE, Material.BELL,
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    /** Mobs que estragam blocos (pegam, quebram, comem plantação). */
    private static final Set<EntityType> ESTRAGAM = EnumSet.of(
            EntityType.ENDERMAN, EntityType.RAVAGER, EntityType.WITHER, EntityType.WITHER_SKULL, EntityType.SILVERFISH,
            EntityType.RABBIT, EntityType.FOX, EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
            EntityType.ZOMBIE_VILLAGER);

    private final RPGAtributos plugin;
    private final Map<UUID, Long> avisos = new HashMap<>();

    public ProtecaoListener(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Territorios ter() { return plugin.territorios(); }

    /** O jogador responsável por uma entidade: ele mesmo, quem atirou, quem acendeu a TNT, o dono do pet... */
    public static Player responsavel(Entity e) {
        for (int i = 0; i < 4 && e != null; i++) {
            if (e instanceof Player p) return p;
            if (Companheiros.eh(e)) {
                UUID dono = Companheiros.dono(e);
                return dono == null ? null : Bukkit.getPlayer(dono);
            }
            if (e instanceof Projectile pr) e = pr.getShooter() instanceof Entity atirador ? atirador : null;
            else if (e instanceof AreaEffectCloud n) e = n.getSource() instanceof Entity fonte ? fonte : null;
            else if (e instanceof TNTPrimed t) e = t.getSource();
            else if (e instanceof Tameable pet && pet.isTamed()) e = pet.getOwner() instanceof Player dono ? dono : null;
            else return null;
        }
        return null;
    }

    /** Avisa (no máximo 1x por segundo) que o lugar é protegido. */
    private void negar(Player p, Location l) {
        long agora = System.currentTimeMillis();
        Long ultimo = avisos.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < 1000) return;
        avisos.put(p.getUniqueId(), agora);
        Territorio t = ter().em(l);
        p.sendActionBar(Component.text("✖ Território de " + (t == null ? "?" : t.nomeDono())
                + ": você não pode mexer aqui.", NamedTextColor.RED));
    }

    private void negarPvp(Player p, Player alvo) {
        long agora = System.currentTimeMillis();
        Long ultimo = avisos.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < 1000) return;
        avisos.put(p.getUniqueId(), agora);
        boolean party = plugin.parties().mesmaParty(p.getUniqueId(), alvo.getUniqueId());
        p.sendActionBar(Component.text(party ? "☮ " + alvo.getName() + " é da sua party (fogo amigo desligado)."
                : "☮ PvP desligado neste território.", NamedTextColor.GRAY));
    }

    /** true (e avisa) se o jogador NÃO pode construir ali. */
    private boolean bloquear(Player p, Location l) {
        if (ter().podeConstruir(p, l)) return false;
        negar(p, l);
        return true;
    }

    /** O destino fica num território diferente da origem? (líquido, pistão, fogo... não atravessam a borda) */
    private boolean invade(Territorio origem, Block destino) {
        Territorio t = ter().em(destino);
        return t != null && t != origem;
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        avisos.remove(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Blocos (jogadores)
    // =====================================================================

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        if (plugin.marcos().eh(b) && !ter().ignorando(p)) {
            e.setCancelled(true);
            Territorio t = ter().em(b);
            if (t != null && t.dono().equals(p.getUniqueId())) {
                p.sendActionBar(Component.text("Para tirar o Marco, use \"Abandonar território\" no menu dele.", NamedTextColor.YELLOW));
            } else {
                negar(p, b.getLocation());
            }
            return;
        }
        if (bloquear(p, b.getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (bloquear(e.getPlayer(), e.getBlockPlaced().getLocation())) {
            e.setCancelled(true);
            return;
        }
        // Cama, porta...: as outras partes também precisam estar num lugar permitido.
        if (e instanceof BlockMultiPlaceEvent multi) {
            for (BlockState s : multi.getReplacedBlockStates()) {
                if (bloquear(e.getPlayer(), s.getLocation())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEsvaziarBalde(PlayerBucketEmptyEvent e) {
        if (bloquear(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEncherBalde(PlayerBucketFillEvent e) {
        if (bloquear(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoPegarComBalde(PlayerBucketEntityEvent e) {
        if (bloquear(e.getPlayer(), e.getEntity().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEscreverPlaca(SignChangeEvent e) {
        if (bloquear(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoTirarLivro(PlayerTakeLecternBookEvent e) {
        Location l = e.getLectern().getLocation();
        if (!ter().permite(e.getPlayer(), l, Flag.BAUS)) {
            e.setCancelled(true);
            negar(e.getPlayer(), l);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoFertilizar(BlockFertilizeEvent e) {
        if (e.getPlayer() != null && bloquear(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            return;
        }
        Territorio origem = ter().em(e.getBlock());
        e.getBlocks().removeIf(s -> invade(origem, s.getBlock()));
    }

    // =====================================================================
    //  Cliques em blocos
    // =====================================================================

    @EventHandler(priority = EventPriority.LOW)
    public void aoInteragir(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b == null || ter().em(b) == null) return;
        Player p = e.getPlayer();
        if (ter().podeConstruir(p, b)) return;
        switch (e.getAction()) {
            case PHYSICAL -> {
                // Placa de pressão e fio de armadilha contam como "porta"; pisotear plantação e ovos, nunca.
                Material t = b.getType();
                boolean porta = Tag.PRESSURE_PLATES.isTagged(t) || t == Material.TRIPWIRE;
                if (!porta || !ter().permite(p, b.getLocation(), Flag.PORTAS)) e.setCancelled(true);
            }
            case LEFT_CLICK_BLOCK -> {
                BlockFace face = e.getBlockFace();
                boolean apagaFogo = Tag.FIRE.isTagged(b.getRelative(face).getType());
                if (b.getType() == Material.DRAGON_EGG || apagaFogo) {
                    e.setCancelled(true);
                    negar(p, b.getLocation());
                }
            }
            case RIGHT_CLICK_BLOCK -> clicarBloco(e, p, b);
            default -> { }
        }
    }

    private void clicarBloco(PlayerInteractEvent e, Player p, Block b) {
        if (plugin.ehEstacao(b)) return; // a estação decide quem pode usar
        ItemStack mao = e.getItem();
        boolean negou = false;
        if (mao != null && modificaBloco(mao.getType())) {
            e.setUseItemInHand(Event.Result.DENY);
            negou = true;
        }
        if (interativo(b) && !UTILIDADES.contains(b.getType())) {
            Flag regra = regraDoBloco(b);
            if (regra == null || !ter().permite(p, b.getLocation(), regra)) {
                e.setUseInteractedBlock(Event.Result.DENY);
                negou = true;
            }
        }
        if (negou) negar(p, b.getLocation());
    }

    /** Que regra libera esse bloco para visitantes (null = só quem pode construir). */
    private static Flag regraDoBloco(Block b) {
        Material t = b.getType();
        if (Tag.DOORS.isTagged(t) || Tag.TRAPDOORS.isTagged(t) || Tag.FENCE_GATES.isTagged(t)
                || Tag.BUTTONS.isTagged(t) || t == Material.LEVER) return Flag.PORTAS;
        if (b.getState(false) instanceof BlockInventoryHolder) return Flag.BAUS;
        return null;
    }

    @SuppressWarnings("deprecation") // ainda é o jeito oficial de saber isso no Paper
    private static boolean interativo(Block b) {
        return b.getType().asBlockType() != null && b.getType().asBlockType().isInteractable();
    }

    /** Itens que mudam o bloco clicado (enxada, machado, farinha de osso, isqueiro, ovo de spawn...). */
    private static boolean modificaBloco(Material m) {
        return Tag.ITEMS_HOES.isTagged(m) || Tag.ITEMS_AXES.isTagged(m) || Tag.ITEMS_SHOVELS.isTagged(m)
                || Tag.ITEMS_DYES.isTagged(m) || m.name().endsWith("_SPAWN_EGG")
                || switch (m) {
                    case BONE_MEAL, HONEYCOMB, FLINT_AND_STEEL, FIRE_CHARGE, SHEARS, BRUSH, ENDER_EYE,
                         INK_SAC, GLOW_INK_SAC, POTION, LEAD, WIND_CHARGE -> true;
                    default -> false;
                };
    }

    // =====================================================================
    //  Entidades
    // =====================================================================

    /** Entidade do território que esse jogador não pode ferir nem usar (animais, aldeões, suportes...). */
    private boolean protegida(Player p, Entity v) {
        if (v instanceof Player || v instanceof Enemy || Chefes.ehChefe(v) || Chefes.ehLacaio(v)) return false;
        if (v instanceof Tameable pet && pet.isTamed() && p.getUniqueId().equals(pet.getOwnerUniqueId())) return false;
        if (p.getUniqueId().equals(Companheiros.dono(v))) return false; // o próprio companheiro
        if (ter().em(v.getLocation()) == null) return false;
        return !ter().podeConstruir(p, v.getLocation());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Player a = responsavel(e.getDamager());
        if (a == null) return;
        Entity v = e.getEntity();
        if (v instanceof Player vp) {
            if (a != vp && !plugin.pvpPermitido(a, vp)) {
                e.setCancelled(true);
                negarPvp(a, vp);
            }
            return;
        }
        if (protegida(a, v)) {
            e.setCancelled(true);
            negar(a, v.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQueimar(EntityCombustByEntityEvent e) {
        Player a = responsavel(e.getCombuster());
        if (a == null) return;
        Entity v = e.getEntity();
        boolean bloqueia = v instanceof Player vp ? a != vp && !plugin.pvpPermitido(a, vp) : protegida(a, v);
        if (bloqueia) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoUsarEntidade(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Entity v = e.getRightClicked();
        if (v instanceof AbstractVillager) return; // comércio e missões
        // Barco e carrinho comuns: dá para andar. Com baú/funil: precisa da regra de baús.
        if (v instanceof Vehicle && !(v instanceof LivingEntity)) {
            if (v instanceof InventoryHolder && !ter().permite(p, v.getLocation(), Flag.BAUS)) {
                e.setCancelled(true);
                negar(p, v.getLocation());
            }
            return;
        }
        if (protegida(p, v)) {
            e.setCancelled(true);
            negar(p, v.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoMexerSuporte(PlayerArmorStandManipulateEvent e) {
        if (bloquear(e.getPlayer(), e.getRightClicked().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        Entity pego = e.getCaught();
        if (pego == null || e.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        Player p = e.getPlayer();
        boolean bloqueia = pego instanceof Player alvo ? !plugin.pvpPermitido(p, alvo) : protegida(p, pego);
        if (bloqueia) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoColocarEntidade(EntityPlaceEvent e) {
        if (e.getPlayer() != null && bloquear(e.getPlayer(), e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoPendurar(HangingPlaceEvent e) {
        if (e.getPlayer() != null && bloquear(e.getPlayer(), e.getEntity().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQuebrarPendurado(HangingBreakEvent e) {
        Location l = e.getEntity().getLocation();
        if (e instanceof HangingBreakByEntityEvent porEntidade) {
            Player p = responsavel(porEntidade.getRemover());
            if (p != null) {
                if (bloquear(p, l)) e.setCancelled(true);
                return;
            }
        }
        if (e.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && !ter().flagAqui(l, Flag.EXPLOSOES)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoDanificarVeiculo(VehicleDamageEvent e) {
        Player p = responsavel(e.getAttacker());
        if (p != null && bloquear(p, e.getVehicle().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoDestruirVeiculo(VehicleDestroyEvent e) {
        Player p = responsavel(e.getAttacker());
        if (p != null && bloquear(p, e.getVehicle().getLocation())) e.setCancelled(true);
    }

    // ---------- poções jogadas ----------

    private static boolean nociva(Collection<PotionEffect> efeitos) {
        for (PotionEffect ef : efeitos) if (ef.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) return true;
        return false;
    }

    private boolean bloqueiaEfeito(Player a, LivingEntity v) {
        if (v == a) return false;
        if (v instanceof Player vp) return !plugin.pvpPermitido(a, vp);
        return protegida(a, v);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoPocao(PotionSplashEvent e) {
        Player a = e.getPotion().getShooter() instanceof Entity s ? responsavel(s) : null;
        if (a == null || !nociva(e.getPotion().getEffects())) return;
        for (LivingEntity v : e.getAffectedEntities()) if (bloqueiaEfeito(a, v)) e.setIntensity(v, 0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoNuvem(AreaEffectCloudApplyEvent e) {
        AreaEffectCloud n = e.getEntity();
        Player a = n.getSource() instanceof Entity s ? responsavel(s) : null;
        if (a == null) return;
        List<PotionEffect> efeitos = new ArrayList<>(n.getCustomEffects());
        if (n.getBasePotionType() != null) efeitos.addAll(n.getBasePotionType().getPotionEffects());
        if (!nociva(efeitos)) return;
        e.getAffectedEntities().removeIf(v -> bloqueiaEfeito(a, v));
    }

    // =====================================================================
    //  Mobs
    // =====================================================================

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r != CreatureSpawnEvent.SpawnReason.NATURAL && r != CreatureSpawnEvent.SpawnReason.PATROL) return;
        if (e.getEntity() instanceof Enemy && !ter().flagAqui(e.getLocation(), Flag.MONSTROS)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoMudarBloco(EntityChangeBlockEvent e) {
        Block b = e.getBlock();
        if (e.getEntity() instanceof Player p) {
            // Descascar tronco, fazer caminho, encerar cobre...
            if (bloquear(p, b.getLocation())) e.setCancelled(true);
        } else if (ESTRAGAM.contains(e.getEntityType()) && ter().em(b) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQuebrarPorta(EntityBreakDoorEvent e) {
        if (ter().em(e.getBlock()) != null) e.setCancelled(true);
    }

    /** Mobs pisando na plantação; flechas apertando botões. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoMobInteragir(EntityInteractEvent e) {
        Block b = e.getBlock();
        if (ter().em(b) == null) return;
        Material t = b.getType();
        if (t == Material.FARMLAND || t == Material.TURTLE_EGG || t == Material.SNIFFER_EGG) {
            e.setCancelled(true);
            return;
        }
        Player p = responsavel(e.getEntity());
        if (p != null && !ter().podeConstruir(p, b) && !ter().permite(p, b.getLocation(), Flag.PORTAS)) e.setCancelled(true);
    }

    // =====================================================================
    //  O mundo respeitando a borda
    // =====================================================================

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> !ter().flagAqui(b.getLocation(), Flag.EXPLOSOES));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> !ter().flagAqui(b.getLocation(), Flag.EXPLOSOES));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoAcenderTnt(TNTPrimeEvent e) {
        Player p = e.getPrimingEntity() == null ? null : responsavel(e.getPrimingEntity());
        if (p != null && bloquear(p, e.getBlock().getLocation())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQueimarBloco(BlockBurnEvent e) {
        if (!ter().flagAqui(e.getBlock().getLocation(), Flag.FOGO)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEspalhar(BlockSpreadEvent e) {
        if (Tag.FIRE.isTagged(e.getSource().getType()) && !ter().flagAqui(e.getBlock().getLocation(), Flag.FOGO)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoAcender(BlockIgniteEvent e) {
        Location l = e.getBlock().getLocation();
        Player p = e.getPlayer();
        if (p == null && e.getIgnitingEntity() != null) p = responsavel(e.getIgnitingEntity());
        if (p != null) {
            if (bloquear(p, l)) e.setCancelled(true);
            return;
        }
        if (e.getIgnitingBlock() != null && invade(ter().em(e.getIgnitingBlock()), e.getBlock())) {
            e.setCancelled(true); // isqueiro de um ejetor de fora
            return;
        }
        if (e.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && !ter().flagAqui(l, Flag.FOGO)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        Territorio origem = ter().em(e.getBlock());
        BlockFace d = e.getDirection();
        if (invade(origem, e.getBlock().getRelative(d))) {
            e.setCancelled(true);
            return;
        }
        for (Block b : e.getBlocks()) {
            if (invade(origem, b) || invade(origem, b.getRelative(d))) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        Territorio origem = ter().em(e.getBlock());
        for (Block b : e.getBlocks()) {
            if (invade(origem, b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEscorrer(BlockFromToEvent e) {
        Block de = e.getBlock(), para = e.getToBlock();
        if (de.getX() >> 4 == para.getX() >> 4 && de.getZ() >> 4 == para.getZ() >> 4) return; // mesmo chunk
        if (invade(ter().em(de), para)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEjetar(BlockDispenseEvent e) {
        Block b = e.getBlock();
        if (b.getBlockData() instanceof Directional d && invade(ter().em(b), b.getRelative(d.getFacing()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoCrescerEstrutura(org.bukkit.event.world.StructureGrowEvent e) {
        if (e.getPlayer() != null && bloquear(e.getPlayer(), e.getLocation())) {
            e.setCancelled(true);
            return;
        }
        Territorio origem = ter().em(e.getLocation());
        e.getBlocks().removeIf(s -> invade(origem, s.getBlock()));
    }
}
