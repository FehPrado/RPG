package br.rpgatributos.combate;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.alquimia.Acessorio;
import br.rpgatributos.aventura.Chefes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mobilidade de combate: esquiva (dois toques rápidos para o lado ou para trás), Gancho
 * de Escalada (vara de pescar que puxa até o bloco) e Capa Planadora (acessório: no
 * ar, pular faz planar).
 */
public final class Mobilidade implements Listener {

    public static final TextColor COR = TextColor.color(0xA0C4FF);
    private static final NamespacedKey K_GANCHO = new NamespacedKey("rpgatributos", "gancho");
    private static final int ESQUERDA = 0, DIREITA = 1, TRAS = 2;

    private final RPGAtributos plugin;
    private final Map<UUID, Input> entradas = new HashMap<>();
    private final Map<UUID, long[]> toques = new HashMap<>();
    private final Map<UUID, Long> esperaEsquiva = new HashMap<>();
    private final Map<UUID, Long> invulneravel = new HashMap<>();
    private final Map<UUID, Long> semQueda = new HashMap<>();
    private final Map<UUID, Long> esperaGancho = new HashMap<>();
    private final Set<UUID> planando = new HashSet<>();
    private int ciclo;

    public Mobilidade(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Settings cfg() { return plugin.settings(); }
    private static long agora() { return System.currentTimeMillis(); }

    // =====================================================================
    //  Gancho de Escalada
    // =====================================================================

    public static ItemStack gancho() {
        ItemStack i = new ItemStack(Material.FISHING_ROD);
        i.editMeta(m -> {
            m.itemName(Component.text("Gancho de Escalada", COR));
            m.lore(List.of(
                    Component.text("Lance num bloco e recolha: você é puxado até lá.", NamedTextColor.GRAY),
                    Component.text("Num monstro: puxa ele até você.", NamedTextColor.GRAY),
                    Component.text("Num chefe: puxa você até ele.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("❖ Gancho", COR)).stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_GANCHO, PersistentDataType.BYTE, (byte) 1);
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "gancho");
        });
        return i;
    }

    public static boolean ehGancho(ItemStack i) {
        return i != null && i.getType() == Material.FISHING_ROD && i.hasItemMeta()
                && i.getItemMeta().getPersistentDataContainer().has(K_GANCHO);
    }

    /** Bancada: vara de pescar + gancho de armadilha + 3 lingotes de ferro. */
    public void registrarReceita() {
        NamespacedKey chave = new NamespacedKey(plugin, "gancho_de_escalada");
        Bukkit.removeRecipe(chave);
        ShapelessRecipe r = new ShapelessRecipe(chave, gancho());
        r.addIngredient(Material.FISHING_ROD);
        r.addIngredient(Material.TRIPWIRE_HOOK);
        r.addIngredient(3, Material.IRON_INGOT);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        Player p = e.getPlayer();
        ItemStack vara = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
        if (!ehGancho(vara)) return;
        FishHook h = e.getHook();
        switch (e.getState()) {
            case FISHING -> h.setVelocity(h.getVelocity().multiply(1.8));
            case CAUGHT_ENTITY -> {
                Entity c = e.getCaught();
                if (!(c instanceof LivingEntity le)) return;
                boolean grande = le instanceof Player || Chefes.ehChefe(le) || le.getHeight() > 2.5 || le.getWidth() > 2;
                if (grande) puxar(p, le.getLocation().add(0, 1, 0));
                else {
                    Vector v = p.getLocation().toVector().subtract(le.getLocation().toVector());
                    if (v.lengthSquared() > 1) le.setVelocity(v.normalize().multiply(Math.min(1.8, 0.25 * v.length())).setY(0.45));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.7f);
                }
            }
            case IN_GROUND, REEL_IN, FAILED_ATTEMPT -> {
                Location hl = h.getLocation();
                if (h.isOnGround() || blocoPerto(hl)) puxar(p, hl);
            }
            default -> { }
        }
    }

    private static boolean blocoPerto(Location l) {
        double[] d = {-0.35, 0, 0.35};
        for (double x : d) for (double y : d) for (double z : d) {
            if (l.clone().add(x, y, z).getBlock().getType().isSolid()) return true;
        }
        return false;
    }

    private void puxar(Player p, Location destino) {
        long t = agora();
        Long espera = esperaGancho.get(p.getUniqueId());
        if (espera != null && espera > t) return;
        Vector v = destino.toVector().subtract(p.getLocation().toVector());
        double dist = v.length();
        if (dist < 1.5) return;
        if (dist > cfg().mobGanchoAlcance) {
            p.sendActionBar(Component.text("✖ Longe demais para o gancho", NamedTextColor.RED));
            return;
        }
        esperaGancho.put(p.getUniqueId(), t + 800);
        Vector vel = v.normalize().multiply(Math.min(2.6, 0.5 + dist * 0.12));
        vel.setY(vel.getY() + 0.35 + (destino.getY() > p.getLocation().getY() + 1 ? 0.2 : 0));
        p.setVelocity(vel);
        semQueda.put(p.getUniqueId(), t + 4000);
        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1.4f);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1.2f);
    }

    // =====================================================================
    //  Teclas: esquiva e planador
    // =====================================================================

    @EventHandler
    public void aoApertar(PlayerInputEvent e) {
        Player p = e.getPlayer();
        Input novo = e.getInput();
        Input antigo = entradas.put(p.getUniqueId(), novo);
        if (antigo == null) return;
        if (novo.isJump() && !antigo.isJump()) pular(p);
        if (novo.isSneak() && !antigo.isSneak() && planando.remove(p.getUniqueId())) {
            p.sendActionBar(Component.text("≋ Você soltou a capa", NamedTextColor.GRAY));
        }
        if (novo.isLeft() && !antigo.isLeft()) toque(p, ESQUERDA);
        if (novo.isRight() && !antigo.isRight()) toque(p, DIREITA);
        if (novo.isBackward() && !antigo.isBackward()) toque(p, TRAS);
    }

    private void toque(Player p, int direcao) {
        long[] t = toques.computeIfAbsent(p.getUniqueId(), k -> new long[3]);
        long agora = agora();
        if (agora - t[direcao] <= cfg().mobEsquivaJanelaMs) {
            t[direcao] = 0;
            esquivar(p, direcao);
        } else {
            t[direcao] = agora;
        }
    }

    private void esquivar(Player p, int direcao) {
        if (!cfg().mobEsquivaAtiva || p.isSneaking() || p.isInsideVehicle() || p.isGliding() || p.isFlying()
                || planando.contains(p.getUniqueId()) || p.getGameMode() == GameMode.SPECTATOR) return;
        long t = agora();
        Long espera = esperaEsquiva.get(p.getUniqueId());
        if (espera != null && espera > t) return;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && plugin.combos().vigor(p) < cfg().mobEsquivaVigor) {
            p.sendActionBar(Component.text("✖ Vigor insuficiente para esquivar", NamedTextColor.RED));
            return;
        }
        double yaw = Math.toRadians(p.getLocation().getYaw());
        Vector frente = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector direita = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector dir = switch (direcao) {
            case ESQUERDA -> direita.multiply(-1);
            case DIREITA -> direita;
            default -> frente.multiply(-1);
        };
        p.setVelocity(dir.multiply(1.15).setY(p.isOnGround() ? 0.22 : 0.08));
        esperaEsquiva.put(p.getUniqueId(), t + cfg().mobEsquivaRecargaMs);
        invulneravel.put(p.getUniqueId(), t + 400);
        semQueda.put(p.getUniqueId(), t + 2000);
        if (!criativo) plugin.combos().darVigor(p, -cfg().mobEsquivaVigor);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.3, 0), 12, 0.3, 0.1, 0.3, 0.03);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.7f, 1.4f);
    }

    private boolean temPlanador(Player p) {
        return plugin.acessorios().tem(p, Acessorio.Especial.PLANADOR);
    }

    private void pular(Player p) {
        if (!temPlanador(p)) return;
        if (plugin.runas().saltoDisponivel(p)) return; // o pulo duplo da Runa do Salto vem antes de planar
        if (planando.remove(p.getUniqueId())) {
            p.sendActionBar(Component.text("≋ Você soltou a capa", NamedTextColor.GRAY));
            return;
        }
        if (p.isOnGround() || p.isInWater() || p.isGliding() || p.isFlying() || p.isInsideVehicle()) return;
        if (p.getVelocity().getY() > 0.1) return; // ainda subindo do pulo
        planando.add(p.getUniqueId());
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 0.8f, 1.2f);
        p.sendActionBar(Component.text("≋ Planando (pule ou agache para soltar)", COR));
    }

    /** A cada tick: quem está planando desce devagar e vai para onde olha. */
    public void tick() {
        ciclo++;
        if (planando.isEmpty()) return;
        for (Iterator<UUID> it = planando.iterator(); it.hasNext(); ) {
            Player p = Bukkit.getPlayer(it.next());
            if (p == null || p.isOnGround() || p.isInWater() || p.isInsideVehicle() || p.isGliding() || p.isFlying() || p.isDead() || !temPlanador(p)) {
                it.remove();
                continue;
            }
            Vector olhar = p.getLocation().getDirection().setY(0);
            if (olhar.lengthSquared() < 1e-4) olhar = new Vector(0, 0, 0);
            else olhar.normalize().multiply(0.62);
            Vector atual = p.getVelocity();
            Vector nova = atual.clone().multiply(0.5).add(olhar.multiply(0.5));
            nova.setY(atual.getY() > 0 ? atual.getY() * 0.9 : Math.max(atual.getY(), -0.11));
            p.setVelocity(nova);
            p.setFallDistance(0);
            if (ciclo % 5 == 0) p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 2, 0.2, 0, 0.2, 0);
        }
    }

    // =====================================================================
    //  Dano
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        long t = agora();
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.FALL) {
            Long ate = semQueda.get(p.getUniqueId());
            if ((ate != null && ate > t) || planando.contains(p.getUniqueId())) {
                e.setCancelled(true);
                semQueda.remove(p.getUniqueId());
            }
            return;
        }
        if (c == EntityDamageEvent.DamageCause.VOID || c == EntityDamageEvent.DamageCause.KILL
                || c == EntityDamageEvent.DamageCause.STARVATION || c == EntityDamageEvent.DamageCause.SUFFOCATION) return;
        Long ate = invulneravel.get(p.getUniqueId());
        if (ate != null && ate > t) {
            e.setCancelled(true);
            p.sendActionBar(Component.text("✧ Esquivou!", NamedTextColor.AQUA));
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.02);
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        entradas.remove(id);
        toques.remove(id);
        planando.remove(id);
        invulneravel.remove(id);
    }
}
