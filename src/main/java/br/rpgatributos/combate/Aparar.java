package br.rpgatributos.combate;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aparar (2.54): levantar o escudo bem na hora do golpe (até 0,3 s antes; 0,5 s com o Escudo de
 * Duelo) apara: você não leva nada, o atacante fica desequilibrado (lento e fraco) e o seu próximo
 * golpe em 1,5 s sai mais forte (+50%; +80% com o Escudo de Duelo). Flechas e projéteis aparados
 * voltam. Cada aparada devolve Vigor.
 */
public final class Aparar implements Listener {

    public static final TextColor COR = TextColor.color(0xFFE082);
    private static final NamespacedKey K_DUELO = new NamespacedKey("rpgatributos", "escudo_duelo");
    private static final int JANELA = 6, JANELA_DUELO = 10;

    private final RPGAtributos plugin;
    /** Quem aparou há pouco: até quando vale o contra-ataque, e o bônus. */
    private final Map<UUID, double[]> contra = new HashMap<>();

    public Aparar(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack escudoDeDuelo() {
        ItemStack i = new ItemStack(Material.SHIELD);
        i.editMeta(m -> {
            m.itemName(Component.text("Escudo de Duelo", COR));
            m.lore(List.of(
                    Component.text("Levante bem na hora do golpe para aparar:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("janela maior (0,5 s) e contra-ataque +80%.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_DUELO, PersistentDataType.BYTE, (byte) 1);
            m.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    public static boolean ehDuelo(ItemStack i) {
        return i != null && i.getType() == Material.SHIELD && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(K_DUELO);
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "escudo_duelo"), escudoDeDuelo());
        r.shape("IGI", " S ");
        r.setIngredient('I', Material.IRON_INGOT);
        r.setIngredient('G', Material.GOLD_INGOT);
        r.setIngredient('S', Material.SHIELD);
        Bukkit.addRecipe(r);
    }

    /** O escudo levantado agora (ou null) e há quantos ticks. */
    private static ItemStack escudoErguido(Player p) {
        if (!p.isHandRaised()) return null;
        ItemStack ativo = p.getActiveItem();
        return ativo != null && ativo.getType() == Material.SHIELD ? ativo : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack escudo = escudoErguido(p);
        if (escudo == null) return;
        boolean duelo = ehDuelo(escudo);
        if (p.getHandRaisedTime() > (duelo ? JANELA_DUELO : JANELA)) return;
        Entity d = e.getDamager();
        // O golpe tem que vir da frente.
        Vector olhar = p.getLocation().getDirection().setY(0), deOnde = d.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
        if (olhar.lengthSquared() > 1e-4 && deOnde.lengthSquared() > 1e-4 && olhar.normalize().dot(deOnde.normalize()) < 0.2) return;
        e.setCancelled(true);
        aparou(p, d, duelo);
    }

    private void aparou(Player p, Entity d, boolean duelo) {
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.9f);
        p.getWorld().spawnParticle(Particle.CRIT, p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.8)), 15, 0.2, 0.2, 0.2, 0.3);
        if (d instanceof Projectile pr) {
            // A flecha volta.
            Vector v = pr.getVelocity();
            pr.setVelocity(v.multiply(-0.8));
            if (pr.getShooter() instanceof Entity atirador) pr.setShooter(p);
        } else if (d instanceof LivingEntity atacante) {
            atacante.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, true, true));
            atacante.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, true, true));
            Vector longe = atacante.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (longe.lengthSquared() > 1e-4) atacante.setVelocity(longe.normalize().multiply(0.6).setY(0.25));
        }
        contra.put(p.getUniqueId(), new double[]{System.currentTimeMillis() + 1500, duelo ? 1.8 : 1.5});
        plugin.combos().darVigor(p, 10);
        plugin.titulos().registrar(p, "aparadas", 1);
        p.sendActionBar(Component.text("✦ Aparou! Contra-ataque " + (duelo ? "+80%" : "+50%"), COR, TextDecoration.BOLD));
    }

    /** O contra-ataque logo depois de aparar. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoContraAtacar(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        double[] c = contra.remove(p.getUniqueId());
        if (c == null || System.currentTimeMillis() > c[0]) return;
        e.setDamage(e.getDamage() * c[1]);
        e.getEntity().getWorld().spawnParticle(Particle.ENCHANTED_HIT, e.getEntity().getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.2);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        contra.remove(e.getPlayer().getUniqueId());
    }
}
