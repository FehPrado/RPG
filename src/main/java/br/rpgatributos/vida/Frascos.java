package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

/**
 * Frascos de arremesso (bancada): fumaça (os monstros perdem você de vista), cola (prende quem
 * estiver perto), fogo-grego (incendeia em volta) e cura em área (para você e quem estiver perto).
 */
public final class Frascos implements Listener {

    public static final NamespacedKey K_FRASCO = new NamespacedKey("rpgatributos", "frasco");

    public enum Tipo {
        FUMACA("Frasco de Fumaça", 0x9E9E9E, "Os monstros em volta perdem você de vista e ficam cegos."),
        COLA("Frasco de Cola", 0x9CCC65, "Prende quem estiver perto (lentidão forte)."),
        FOGO_GREGO("Fogo-Grego", 0xFF7043, "Incendeia tudo em volta (não queima blocos)."),
        CURA("Frasco de Cura", 0xF06292, "Cura você e quem estiver perto.");

        private final String nome, descricao;
        private final TextColor cor;

        Tipo(String nome, int cor, String descricao) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.descricao = descricao;
        }

        public String nome() { return nome; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    private final RPGAtributos plugin;

    public Frascos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack criar(Tipo t, int qtd) {
        ItemStack i = new ItemStack(Material.SNOWBALL, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(t.nome, t.cor));
            m.setMaxStackSize(16);
            m.lore(List.of(Component.text(t.descricao, NamedTextColor.GRAY), Component.empty(),
                            Component.text("Clique para arremessar", NamedTextColor.YELLOW), Component.text("⚗ Frasco de arremesso", t.cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_FRASCO, PersistentDataType.STRING, t.name());
            PacoteRecursos.marcar(m, "frasco_" + t.id());
        });
        return i;
    }

    public static Tipo tipo(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_FRASCO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Tipo.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void registrarReceitas() {
        receita(Tipo.FUMACA, new RecipeChoice.MaterialChoice(Material.GUNPOWDER), RecipeChoice.exactChoice(Erva.MUSGO_LUNAR.criar(1)));
        receita(Tipo.COLA, new RecipeChoice.MaterialChoice(Material.SLIME_BALL), new RecipeChoice.MaterialChoice(Material.SLIME_BALL));
        receita(Tipo.FOGO_GREGO, new RecipeChoice.MaterialChoice(Material.GUNPOWDER), RecipeChoice.exactChoice(Erva.ERVA_DE_SOL.criar(1)));
        receita(Tipo.CURA, new RecipeChoice.MaterialChoice(Material.GLISTERING_MELON_SLICE), RecipeChoice.exactChoice(Erva.SALVIA.criar(1)));
    }

    private void receita(Tipo t, RecipeChoice a, RecipeChoice b) {
        NamespacedKey k = new NamespacedKey(plugin, "frasco_" + t.id());
        if (Bukkit.getRecipe(k) != null) return;
        ShapelessRecipe r = new ShapelessRecipe(k, criar(t, 2));
        r.addIngredient(Material.GLASS_BOTTLE);
        r.addIngredient(a);
        r.addIngredient(b);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoArremessar(PlayerLaunchProjectileEvent e) {
        Tipo t = tipo(e.getItemStack());
        if (t == null || !(e.getProjectile() instanceof Snowball s)) return;
        s.getPersistentDataContainer().set(K_FRASCO, PersistentDataType.STRING, t.name());
        s.setItem(e.getItemStack().asOne());
        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoQuebrar(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball s)) return;
        String id = s.getPersistentDataContainer().get(K_FRASCO, PersistentDataType.STRING);
        if (id == null) return;
        Tipo t;
        try {
            t = Tipo.valueOf(id);
        } catch (IllegalArgumentException ex) {
            return;
        }
        Player dono = s.getShooter() instanceof Player p ? p : null;
        Location l = s.getLocation();
        l.getWorld().playSound(l, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 1f);
        switch (t) {
            case FUMACA -> {
                l.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, l, 60, 2, 1, 2, 0.02);
                for (Entity en : l.getWorld().getNearbyEntities(l, 6, 3, 6)) {
                    if (en instanceof Mob m && !(en instanceof Tameable tm && tm.isTamed())) {
                        m.setTarget(null);
                        m.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
                        m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                    }
                }
            }
            case COLA -> {
                l.getWorld().spawnParticle(Particle.ITEM_SLIME, l, 50, 1.5, 0.4, 1.5, 0.05);
                for (Entity en : l.getWorld().getNearbyEntities(l, 3.5, 2, 3.5)) {
                    if (en instanceof LivingEntity le && le != dono) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3));
                    }
                }
            }
            case FOGO_GREGO -> {
                l.getWorld().spawnParticle(Particle.FLAME, l, 80, 2, 0.5, 2, 0.05);
                l.getWorld().spawnParticle(Particle.LAVA, l, 10, 1.5, 0.3, 1.5, 0);
                for (Entity en : l.getWorld().getNearbyEntities(l, 3.5, 2, 3.5)) {
                    if (en instanceof LivingEntity le && le != dono) le.setFireTicks(Math.max(le.getFireTicks(), 120));
                }
            }
            case CURA -> {
                l.getWorld().spawnParticle(Particle.HEART, l.clone().add(0, 1, 0), 15, 1.5, 0.5, 1.5, 0);
                for (Entity en : l.getWorld().getNearbyEntities(l, 4.5, 2.5, 4.5)) {
                    if (!(en instanceof Player pl)) continue;
                    AttributeInstance max = pl.getAttribute(Attribute.MAX_HEALTH);
                    pl.setHealth(Math.min(max == null ? 20 : max.getValue(), pl.getHealth() + 6));
                    pl.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                }
            }
        }
    }
}
