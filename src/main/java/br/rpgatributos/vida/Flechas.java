package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Flechas especiais (bancada): de fogo, de gelo, rastreadora (persegue o monstro mais perto),
 * de corda (puxa você até onde ela crava) e explosiva (explosão pequena que não quebra blocos).
 * Funcionam no arco e na besta.
 */
public final class Flechas implements Listener {

    public static final TextColor COR = TextColor.color(0xD7CCC8);
    public static final NamespacedKey K_FLECHA = new NamespacedKey("rpgatributos", "flecha");

    public enum Tipo {
        FOGO("Flecha de Fogo", 0xFF7043, "Incendeia o alvo.", Material.BLAZE_POWDER, 4),
        GELO("Flecha de Gelo", 0x81D4FA, "Congela e deixa o alvo bem lento.", Material.PACKED_ICE, 4),
        RASTREADORA("Flecha Rastreadora", 0xCE93D8, "Persegue o monstro mais perto.", Material.ENDER_PEARL, 4),
        CORDA("Flecha de Corda", 0xA1887F, "Onde ela cravar, você é puxado até lá.", Material.LEAD, 2),
        EXPLOSIVA("Flecha Explosiva", 0xE53935, "Explode ao acertar (não quebra blocos).", Material.TNT, 4);

        private final String nome, descricao;
        private final TextColor cor;
        private final Material ingrediente;
        private final int rende;

        Tipo(String nome, int cor, String descricao, Material ingrediente, int rende) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.descricao = descricao;
            this.ingrediente = ingrediente;
            this.rende = rende;
        }

        public String nome() { return nome; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    private final RPGAtributos plugin;
    /** Flechas rastreadoras no ar → quando param de perseguir. */
    private final Map<UUID, Long> rastreando = new HashMap<>();
    private final Map<UUID, Long> semQueda = new HashMap<>();
    /** Quem acertou uma flecha explosiva há pouco (para o segredo do creeper). */
    private final Map<UUID, Long> explodiu = new HashMap<>();

    public Flechas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack criar(Tipo t, int qtd) {
        ItemStack i = new ItemStack(Material.ARROW, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(t.nome, t.cor));
            m.lore(List.of(Component.text(t.descricao, NamedTextColor.GRAY), Component.empty(),
                            Component.text("➶ Flecha especial", t.cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_FLECHA, PersistentDataType.STRING, t.name());
            PacoteRecursos.marcar(m, "flecha_" + t.id());
        });
        return i;
    }

    public static Tipo tipo(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_FLECHA, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Tipo.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Tipo tipo(Entity e) {
        String s = e.getPersistentDataContainer().get(K_FLECHA, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Tipo.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void registrarReceitas() {
        for (Tipo t : Tipo.values()) {
            NamespacedKey k = new NamespacedKey(plugin, "flecha_" + t.id());
            if (Bukkit.getRecipe(k) != null) continue;
            ShapelessRecipe r = new ShapelessRecipe(k, criar(t, t.rende));
            r.addIngredient(t.rende, Material.ARROW);
            r.addIngredient(t.ingrediente);
            Bukkit.addRecipe(r);
        }
    }

    // =====================================================================
    //  Atirar e acertar
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAtirar(EntityShootBowEvent e) {
        Tipo t = tipo(e.getConsumable());
        if (t == null || !(e.getProjectile() instanceof AbstractArrow a)) return;
        a.getPersistentDataContainer().set(K_FLECHA, PersistentDataType.STRING, t.name());
        if (t == Tipo.CORDA || t == Tipo.EXPLOSIVA) a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        if (t == Tipo.FOGO) a.setFireTicks(200);
        if (t == Tipo.RASTREADORA) rastreando.put(a.getUniqueId(), System.currentTimeMillis() + 3000);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoAcertar(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow a)) return;
        Tipo t = tipo(a);
        if (t == null) return;
        rastreando.remove(a.getUniqueId());
        Player dono = a.getShooter() instanceof Player p ? p : null;
        Location l = a.getLocation();
        LivingEntity alvo = e.getHitEntity() instanceof LivingEntity le ? le : null;
        switch (t) {
            case FOGO -> {
                if (alvo != null) alvo.setFireTicks(Math.max(alvo.getFireTicks(), 120));
                l.getWorld().spawnParticle(Particle.FLAME, l, 12, 0.2, 0.2, 0.2, 0.03);
            }
            case GELO -> {
                if (alvo != null) {
                    alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                    alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), 160));
                }
                l.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 20, 0.3, 0.3, 0.3, 0.02);
                l.getWorld().playSound(l, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.6f);
            }
            case CORDA -> {
                if (dono != null && e.getHitBlock() != null && dono.getWorld().equals(l.getWorld())) {
                    Vector v = l.toVector().subtract(dono.getLocation().toVector());
                    double dist = v.length();
                    if (dist > 2 && dist < 40) {
                        dono.setVelocity(v.normalize().multiply(Math.min(2.6, 0.9 + dist * 0.06)).setY(Math.min(1.4, v.getY() * 0.12 + 0.5)));
                        semQueda.put(dono.getUniqueId(), System.currentTimeMillis() + 4000);
                        dono.playSound(dono.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
                    }
                }
                a.remove();
            }
            case EXPLOSIVA -> {
                l.getWorld().createExplosion(l, 1.6f, false, false, dono);
                if (dono != null) explodiu.put(dono.getUniqueId(), System.currentTimeMillis() + 3000);
                a.remove();
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        Long ate = semQueda.get(p.getUniqueId());
        if (ate != null && ate > System.currentTimeMillis()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || e.getEntity().getType() != org.bukkit.entity.EntityType.CREEPER) return;
        Long ate = explodiu.get(p.getUniqueId());
        if (ate != null && ate > System.currentTimeMillis()) plugin.segredos().conceder(p, Segredos.Segredo.CREEPER_EXPLOSIVO);
    }

    /** Todo tick: as rastreadoras viram na direção do monstro mais perto à frente. */
    public void tick() {
        if (rastreando.isEmpty()) return;
        long agora = System.currentTimeMillis();
        rastreando.entrySet().removeIf(en -> {
            if (!(Bukkit.getEntity(en.getKey()) instanceof AbstractArrow a) || !a.isValid() || a.isInBlock() || agora > en.getValue()) return true;
            Vector vel = a.getVelocity();
            double rapidez = vel.length();
            if (rapidez < 0.2) return true;
            LivingEntity melhor = null;
            double melhorD = Double.MAX_VALUE;
            for (Entity e : a.getNearbyEntities(14, 8, 14)) {
                if (!(e instanceof LivingEntity le) || !(e instanceof Enemy) || le.isDead()) continue;
                Vector para = le.getLocation().add(0, le.getHeight() / 2, 0).toVector().subtract(a.getLocation().toVector());
                if (para.clone().normalize().dot(vel.clone().normalize()) < 0.3) continue;
                double d = para.lengthSquared();
                if (d < melhorD) { melhorD = d; melhor = le; }
            }
            if (melhor != null) {
                Vector para = melhor.getLocation().add(0, melhor.getHeight() / 2, 0).toVector().subtract(a.getLocation().toVector()).normalize();
                a.setVelocity(vel.clone().normalize().multiply(0.7).add(para.multiply(0.3)).normalize().multiply(rapidez));
                a.getWorld().spawnParticle(Particle.WITCH, a.getLocation(), 1, 0, 0, 0, 0);
            }
            return false;
        });
        if (semQueda.size() > 50) semQueda.values().removeIf(t -> t < agora);
        if (explodiu.size() > 50) explodiu.values().removeIf(t -> t < agora);
    }
}
