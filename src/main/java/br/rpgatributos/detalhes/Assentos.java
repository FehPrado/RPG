package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

/**
 * Sentar: clique com a mão vazia em cima de uma escada ou laje (a metade de baixo) para sentar,
 * ou use /sentar para sentar no chão. Agachar levanta. Sentado perto de uma Fogueira de
 * Acampamento você descansa.
 */
public final class Assentos implements Listener, CommandExecutor {

    private final RPGAtributos plugin;
    private final NamespacedKey kAssento;

    public Assentos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kAssento = new NamespacedKey(plugin, "assento");
    }

    private boolean ehAssento(Entity e) {
        return e instanceof ArmorStand && e.getPersistentDataContainer().has(kAssento);
    }

    public boolean sentado(Player p) {
        return ehAssento(p.getVehicle());
    }

    /** Senta o jogador em {@code onde} (a altura do assento). */
    public boolean sentar(Player p, Location onde) {
        if (p.isInsideVehicle() || p.isSneaking() || p.isFlying() || p.isGliding() || !p.isOnGround()) return false;
        onde.setYaw(p.getLocation().getYaw());
        ArmorStand banco = onde.getWorld().spawn(onde, ArmorStand.class, a -> {
            a.setInvisible(true);
            a.setMarker(true);
            a.setSmall(true);
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setSilent(true);
            a.setPersistent(false);
            a.getPersistentDataContainer().set(kAssento, PersistentDataType.BYTE, (byte) 1);
        });
        if (!banco.addPassenger(p)) {
            banco.remove();
            return false;
        }
        p.sendActionBar(Component.text("Agache para levantar.", NamedTextColor.GRAY));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        if (!p.getInventory().getItemInMainHand().isEmpty() || p.isSneaking() || !plugin.settings().detSentar) return;
        Block b = e.getClickedBlock();
        if (plugin.ehEstacao(b) || e.getBlockFace() != BlockFace.UP && !(b.getBlockData() instanceof Stairs)) return;
        double altura;
        if (b.getBlockData() instanceof Stairs s && s.getHalf() == Bisected.Half.BOTTOM) altura = 0.5;
        else if (b.getBlockData() instanceof Slab s && s.getType() == Slab.Type.BOTTOM) altura = 0.5;
        else return;
        if (!b.getRelative(BlockFace.UP).isPassable() || p.getLocation().distanceSquared(b.getLocation().add(0.5, 0.5, 0.5)) > 9) return;
        if (Tag.STAIRS.isTagged(b.getType()) || Tag.SLABS.isTagged(b.getType())) {
            if (sentar(p, b.getLocation().add(0.5, altura - 0.35, 0.5))) e.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (sentado(p)) {
            levantar(p);
            return true;
        }
        if (!sentar(p, p.getLocation().add(0, -0.35, 0))) {
            p.sendMessage(Component.text("Fique parado no chão para sentar.", NamedTextColor.RED));
        }
        return true;
    }

    public void levantar(Player p) {
        Entity v = p.getVehicle();
        if (ehAssento(v)) {
            v.removePassenger(p);
            v.remove();
        }
    }

    @EventHandler
    public void aoLevantar(EntityDismountEvent e) {
        if (!ehAssento(e.getDismounted())) return;
        Entity banco = e.getDismounted();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            banco.remove();
            if (e.getEntity() instanceof Player p && p.isOnline()) p.teleport(p.getLocation().add(0, 0.4, 0));
        });
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        levantar(e.getPlayer());
    }

    @EventHandler
    public void aoMorrer(PlayerDeathEvent e) {
        levantar(e.getPlayer());
    }

    /** Assentos que sobraram (servidor caiu com alguém sentado) somem. */
    @EventHandler
    public void aoCarregar(EntitiesLoadEvent e) {
        for (Entity ent : e.getEntities()) if (ehAssento(ent) && ent.getPassengers().isEmpty()) ent.remove();
    }
}
