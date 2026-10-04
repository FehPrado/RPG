package br.rpgatributos.detalhes;

import br.rpgatributos.MarcadorDeBlocos;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.domador.Companheiros;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Detalhes do mundo natural:
 * - Achados: quebrar areia ou cascalho natural às vezes revela pepitas, pederneira, cacos, uma
 *   esmeralda ou uma gema bruta.
 * - Orvalho da manhã: colher no amanhecer, a céu aberto, dá 50% mais XP de Agricultura.
 * - Animais raros: 1 em 500 animais nasce "de Ouro" ou "de Neve", com nome, brilho e drops
 *   especiais. Filhotes de um animal raro têm chance de nascer raros também.
 */
public final class Natureza implements Listener {

    public static final NamespacedKey K_RARO = new NamespacedKey("rpgatributos", "animal_raro");
    public static final TextColor OURO = TextColor.color(0xFFD54F), NEVE = TextColor.color(0xE3F2FD);

    public enum Raridade { OURO, NEVE }

    private static final Set<Material> AREIAS = Set.of(Material.SAND, Material.RED_SAND, Material.GRAVEL);
    private static final Material[] CACOS = {Material.ANGLER_POTTERY_SHERD, Material.ARCHER_POTTERY_SHERD, Material.ARMS_UP_POTTERY_SHERD,
            Material.BLADE_POTTERY_SHERD, Material.BREWER_POTTERY_SHERD, Material.BURN_POTTERY_SHERD, Material.DANGER_POTTERY_SHERD,
            Material.EXPLORER_POTTERY_SHERD, Material.FRIEND_POTTERY_SHERD, Material.HEART_POTTERY_SHERD, Material.HOWL_POTTERY_SHERD,
            Material.MINER_POTTERY_SHERD, Material.MOURNER_POTTERY_SHERD, Material.PLENTY_POTTERY_SHERD, Material.PRIZE_POTTERY_SHERD,
            Material.SHEAF_POTTERY_SHERD, Material.SHELTER_POTTERY_SHERD, Material.SKULL_POTTERY_SHERD, Material.SNORT_POTTERY_SHERD};

    private final RPGAtributos plugin;
    private final NamespacedKey mEscala;
    private final MarcadorDeBlocos areiasColocadas;
    private final Set<UUID> raros = new HashSet<>();

    public Natureza(RPGAtributos plugin) {
        this.plugin = plugin;
        this.mEscala = new NamespacedKey(plugin, "animal_raro_escala");
        this.areiasColocadas = new MarcadorDeBlocos(plugin, "areias_colocadas");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Achados na areia e no cascalho
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (AREIAS.contains(e.getBlockPlaced().getType())) areiasColocadas.marcar(e.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!AREIAS.contains(b.getType())) return;
        if (areiasColocadas.desmarcar(b)) return;
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        if (rnd().nextDouble() >= plugin.settings().detAchadoChance) return;
        ItemStack achado = achado();
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), achado);
        b.getWorld().playSound(b.getLocation(), Sound.ITEM_BRUSH_BRUSHING_SAND_COMPLETE, 1f, 1.3f);
        b.getWorld().spawnParticle(Particle.WAX_OFF, b.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0);
        p.sendActionBar(Component.text("✦ Algo brilhou " + (b.getType() == Material.GRAVEL ? "no cascalho" : "na areia") + "!", OURO));
    }

    private static ItemStack achado() {
        double r = rnd().nextDouble();
        if (r < 0.32) return new ItemStack(Material.GOLD_NUGGET, 1 + rnd().nextInt(3));
        if (r < 0.60) return new ItemStack(Material.IRON_NUGGET, 1 + rnd().nextInt(3));
        if (r < 0.75) return new ItemStack(Material.FLINT, 2);
        if (r < 0.88) return new ItemStack(CACOS[rnd().nextInt(CACOS.length)]);
        if (r < 0.95) return new ItemStack(Material.EMERALD);
        Gema[] g = Gema.values();
        return g[rnd().nextInt(g.length)].criar(1, 1);
    }

    // =====================================================================
    //  Orvalho da manhã
    // =====================================================================

    /** 1,5 se é amanhecer, sem chuva e o bloco está a céu aberto; senão 1. */
    public double orvalho(Block b) {
        World w = b.getWorld();
        long hora = w.getTime();
        boolean amanhecer = hora >= 23000 || hora <= 1500;
        if (!amanhecer || w.hasStorm() || b.getLightFromSky() < 15) return 1;
        w.spawnParticle(Particle.DRIPPING_WATER, b.getLocation().add(0.5, 0.8, 0.5), 6, 0.3, 0.2, 0.3, 0);
        return 1.5;
    }

    // =====================================================================
    //  Animais raros
    // =====================================================================

    public static Raridade raridade(Entity e) {
        String s = e.getPersistentDataContainer().get(K_RARO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Raridade.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String nome(Entity e, Raridade r) {
        return Companheiros.nomeTipo(e.getType()) + (r == Raridade.OURO ? " de Ouro" : " de Neve");
    }

    /** Transforma o animal num raro (usado também pelo admin). */
    public void tornarRaro(LivingEntity e, Raridade r) {
        e.getPersistentDataContainer().set(K_RARO, PersistentDataType.STRING, r.name());
        if (e.customName() == null) {
            e.customName(Component.text("✦ " + nome(e, r), r == Raridade.OURO ? OURO : NEVE));
            e.setCustomNameVisible(true);
        }
        AttributeInstance escala = e.getAttribute(Attribute.SCALE);
        if (escala != null) {
            escala.removeModifier(mEscala);
            escala.addModifier(new AttributeModifier(mEscala, 0.1, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        }
        e.setRemoveWhenFarAway(false);
        raros.add(e.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Animals a)) return;
        CreatureSpawnEvent.SpawnReason why = e.getSpawnReason();
        if (why != CreatureSpawnEvent.SpawnReason.NATURAL && why != CreatureSpawnEvent.SpawnReason.CHUNK_GEN) return;
        if (rnd().nextDouble() >= plugin.settings().detAnimalRaroChance) return;
        tornarRaro(a, rnd().nextBoolean() ? Raridade.OURO : Raridade.NEVE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCruzar(EntityBreedEvent e) {
        Raridade r = raridade(e.getMother());
        if (r == null) r = raridade(e.getFather());
        if (r != null && rnd().nextDouble() < 0.10) tornarRaro(e.getEntity(), r);
    }

    @EventHandler
    public void aoAparecer(EntityAddToWorldEvent e) {
        if (raridade(e.getEntity()) != null) raros.add(e.getEntity().getUniqueId());
    }

    @EventHandler
    public void aoSumir(EntityRemoveFromWorldEvent e) {
        raros.remove(e.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoDomar(EntityTameEvent e) {
        if (raridade(e.getEntity()) == null || !(e.getOwner() instanceof Player p)) return;
        plugin.stats().darXp(p, Skill.DOMA, 100);
        plugin.titulos().registrar(p, "animais_raros", 1);
        plugin.diario().marco(p, "animal_raro", "Domou um animal raro: " + nome(e.getEntity(), raridade(e.getEntity())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        Raridade r = raridade(e.getEntity());
        if (r == null || Companheiros.eh(e.getEntity())) return;
        raros.remove(e.getEntity().getUniqueId());
        List<ItemStack> extras = new ArrayList<>();
        for (ItemStack d : e.getDrops()) extras.add(d.clone()); // drops em dobro
        e.getDrops().addAll(extras);
        if (r == Raridade.OURO) e.getDrops().add(new ItemStack(Material.GOLD_INGOT, 1 + rnd().nextInt(3)));
        else {
            Gema[] g = Gema.values();
            e.getDrops().add(g[rnd().nextInt(g.length)].criar(1, 1));
        }
        Player p = e.getEntity().getKiller();
        if (p != null) {
            plugin.titulos().registrar(p, "animais_raros", 1);
            plugin.diario().marco(p, "animal_raro", "Encontrou um animal raro: " + nome(e.getEntity(), r));
        }
    }

    /** A cada 2 s: brilho dos animais raros. */
    public void tick() {
        for (UUID id : List.copyOf(raros)) {
            Entity e = Bukkit.getEntity(id);
            if (e == null || !e.isValid()) {
                raros.remove(id);
                continue;
            }
            Raridade r = raridade(e);
            e.getWorld().spawnParticle(r == Raridade.OURO ? Particle.WAX_ON : Particle.SNOWFLAKE,
                    e.getLocation().add(0, e.getHeight() * 0.8, 0), 3, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /** Linha para os menus. */
    public static Component linha(Entity e) {
        Raridade r = raridade(e);
        return r == null ? null : Component.text("✦ Animal raro (" + (r == Raridade.OURO ? "de Ouro" : "de Neve") + ")",
                r == Raridade.OURO ? OURO : NamedTextColor.AQUA);
    }
}
