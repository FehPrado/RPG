package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Apito do Domador (2.57): chama os seus companheiros e montarias (domados por você) que estão a até
 * 100 blocos. Eles vêm andando até você (nada de teletransporte); quem estava sentado levanta. Os
 * que já estão montados ou presos na guia ficam onde estão.
 */
public final class Apito implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "apito_domador");
    private static final NamespacedKey K_ALCANCE = new NamespacedKey("rpgatributos", "apito_alcance");
    private static final TextColor COR = TextColor.color(0xC8A26B);
    private static final double RAIO = 100;
    private static final long RECARGA = 8000;

    private final RPGAtributos plugin;
    private final Map<UUID, Long> usado = new HashMap<>();

    public Apito(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack item() {
        ItemStack i = new ItemStack(Material.IRON_NUGGET);
        i.editMeta(m -> {
            m.itemName(Component.text("Apito do Domador", COR));
            m.lore(List.of(
                    Component.text("Clique direito: seus companheiros e montarias", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("a até 100 blocos vêm andando até você.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "apito_domador");
        });
        return i;
    }

    private static boolean eApito(ItemStack i) {
        return i != null && i.getType() == Material.IRON_NUGGET && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public void registrarReceitas() {
        ShapelessRecipe r = new ShapelessRecipe(new NamespacedKey(plugin, "apito_domador"), item());
        r.addIngredient(Material.BAMBOO);
        r.addIngredient(Material.IRON_NUGGET);
        r.addIngredient(Material.STRING);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoApitar(PlayerInteractEvent e) {
        if (!plugin.settings().confApito || e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!eApito(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        long agora = System.currentTimeMillis();
        long falta = usado.getOrDefault(p.getUniqueId(), 0L) + RECARGA - agora;
        if (falta > 0) {
            p.sendActionBar(Component.text("📯 Recuperando o fôlego... " + (falta / 1000 + 1) + " s", NamedTextColor.GRAY));
            return;
        }
        usado.put(p.getUniqueId(), agora);
        List<Mob> vem = new ArrayList<>();
        for (Entity x : p.getWorld().getNearbyEntities(p.getLocation(), RAIO, 48, RAIO)) {
            if (!(x instanceof Mob m) || !meu(p, x) || m.isLeashed() || !m.getPassengers().isEmpty() || m.isInsideVehicle()) continue;
            if (m.getLocation().distanceSquared(p.getLocation()) > RAIO * RAIO) continue;
            vem.add(m);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1.2f, 1.8f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1f, 1.6f);
        if (vem.isEmpty()) {
            p.sendActionBar(Component.text("📯 Nenhum companheiro seu ouviu (a até 100 blocos).", NamedTextColor.GRAY));
            return;
        }
        p.sendActionBar(Component.text("📯 " + vem.size() + (vem.size() == 1 ? " companheiro ouviu" : " companheiros ouviram") + " e está vindo.", COR));
        for (Mob m : vem) chamar(p, m);
    }

    /** É seu: companheiro do plugin, animal domado ou cavalo seu. */
    private static boolean meu(Player p, Entity x) {
        if (Companheiros.eh(x)) return p.getUniqueId().equals(Companheiros.dono(x));
        if (x instanceof Tameable t && t.isTamed() && t.getOwnerUniqueId() != null) return t.getOwnerUniqueId().equals(p.getUniqueId());
        return false;
    }

    private void chamar(Player p, Mob m) {
        if (m instanceof Sittable s && s.isSitting()) s.setSitting(false);
        // O caminho do jogo só vai até o alcance de seguir da criatura: aumenta enquanto ela vem.
        AttributeInstance alcance = m.getAttribute(Attribute.FOLLOW_RANGE);
        if (alcance != null && alcance.getModifier(K_ALCANCE) == null) {
            alcance.addTransientModifier(new AttributeModifier(K_ALCANCE, 110, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
        double velocidade = m instanceof AbstractHorse ? 2.0 : 1.4;
        int[] vezes = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, tarefa -> {
            vezes[0]++;
            boolean chegou = !m.isValid() || !p.isOnline() || !m.getWorld().equals(p.getWorld())
                    || m.getLocation().distanceSquared(p.getLocation()) < 9;
            if (chegou || vezes[0] > 30) {
                if (alcance != null) alcance.removeModifier(K_ALCANCE);
                tarefa.cancel();
                return;
            }
            m.getPathfinder().moveTo(p, velocidade);
        }, 0L, 20L);
    }
}
