package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.forja.ConjuntoMineral.Peca;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Os bônus dos {@link ConjuntoMineral conjuntos dos minerais} (2 e 4 peças vestidas), os poderes
 * das picaretas de Âmbar e de Magma e o conserto na bigorna só com o mineral certo.
 */
public final class EquipamentoMineral implements Listener {

    private static final NamespacedKey K_VIDA = new NamespacedKey("rpgatributos", "mineral_vida");
    private static final NamespacedKey K_EMPURRAO = new NamespacedKey("rpgatributos", "mineral_empurrao");

    private final RPGAtributos plugin;
    private int segundos;

    public EquipamentoMineral(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static Map<ConjuntoMineral, Integer> vestidos(Player p) {
        return ConjuntoMineral.vestidos(p.getInventory().getArmorContents());
    }

    private static int pecas(Map<ConjuntoMineral, Integer> v, ConjuntoMineral c) {
        return v.getOrDefault(c, 0);
    }

    // =====================================================================
    //  A cada segundo: atributos e efeitos
    // =====================================================================

    public void tick() {
        if (!plugin.settings().conjuntosMinerais) return;
        segundos++;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Map<ConjuntoMineral, Integer> v = vestidos(p);
            int jade = pecas(v, ConjuntoMineral.JADE), sangue = pecas(v, ConjuntoMineral.PEDRA_SANGUE);
            double vida = (jade >= 4 ? 4 : jade >= 2 ? 2 : 0) - (sangue >= 4 ? 2 : 0);
            modificador(p, Attribute.MAX_HEALTH, K_VIDA, vida);
            modificador(p, Attribute.KNOCKBACK_RESISTANCE, K_EMPURRAO, pecas(v, ConjuntoMineral.FERRO_NEGRO) >= 2 ? 0.6 : 0);
            if (v.isEmpty() || p.getGameMode() == GameMode.SPECTATOR) continue;
            int ambar = pecas(v, ConjuntoMineral.AMBAR);
            if (ambar >= 2) efeito(p, PotionEffectType.LUCK, ambar >= 4 ? 1 : 0);
            if (jade >= 4 && segundos % 8 == 0) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && p.getHealth() < max.getValue() && !p.isDead()) p.setHealth(Math.min(max.getValue(), p.getHealth() + 1));
            }
            int perola = pecas(v, ConjuntoMineral.PEROLA);
            if (perola >= 2 && p.isInWater()) efeito(p, PotionEffectType.WATER_BREATHING, 0);
            if (perola >= 4 && p.isInWater()) efeito(p, PotionEffectType.DOLPHINS_GRACE, 0);
            if (pecas(v, ConjuntoMineral.GELO) >= 2 && p.getFreezeTicks() > 0) p.setFreezeTicks(0);
            if (pecas(v, ConjuntoMineral.MAGMA) >= 4) efeito(p, PotionEffectType.FIRE_RESISTANCE, 0);
            if (pecas(v, ConjuntoMineral.QUARTZO_NEGRO) >= 4 && p.getEyeLocation().getBlock().getLightLevel() < 7) efeito(p, PotionEffectType.STRENGTH, 0);
            // 3.3: os conjuntos do Abismo.
            if (pecas(v, ConjuntoMineral.ANTIGO) >= 4) efeito(p, PotionEffectType.JUMP_BOOST, 0);
            if (pecas(v, ConjuntoMineral.ABISSAL) >= 2 && p.getEyeLocation().getBlock().getLightLevel() < 7) efeito(p, PotionEffectType.NIGHT_VISION, 0);
            if (pecas(v, ConjuntoMineral.ABISSAL) >= 4 && plugin.arcano() != null) plugin.arcano().darMana(p, 1);
            int esporo = pecas(v, ConjuntoMineral.ESPORO);
            if (esporo >= 2 && p.hasPotionEffect(PotionEffectType.POISON)) p.removePotionEffect(PotionEffectType.POISON);
            if (esporo >= 4 && segundos % 6 == 0) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && p.getHealth() < max.getValue() && !p.isDead()) p.setHealth(Math.min(max.getValue(), p.getHealth() + 1));
            }
            if (pecas(v, ConjuntoMineral.FERRO_NEGRO) >= 4) efeito(p, PotionEffectType.RESISTANCE, 0);
        }
    }

    private static void efeito(Player p, PotionEffectType t, int nivel) {
        PotionEffect atual = p.getPotionEffect(t);
        if (atual != null && (atual.getAmplifier() > nivel || atual.getDuration() > 300)) return;
        if (atual == null || atual.getAmplifier() != nivel || atual.getDuration() < 40) p.addPotionEffect(new PotionEffect(t, 60, nivel, true, false, true));
    }

    private static void modificador(Player p, Attribute a, NamespacedKey k, double valor) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(k);
        if (valor == 0) {
            if (atual != null) inst.removeModifier(k);
        } else if (atual == null || atual.getAmount() != valor) {
            if (atual != null) inst.removeModifier(k);
            inst.addTransientModifier(new AttributeModifier(k, valor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
        if (a == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    // =====================================================================
    //  Combate
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!plugin.settings().conjuntosMinerais || !(e.getEntity() instanceof Player p)) return;
        switch (e.getCause()) {
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> {
                if (pecas(vestidos(p), ConjuntoMineral.MAGMA) >= 2) e.setDamage(e.getDamage() * 0.7);
            }
            case FALL -> {
                int antigo = pecas(vestidos(p), ConjuntoMineral.ANTIGO);
                if (antigo >= 4 && p.getFallDistance() <= 12) e.setCancelled(true);
                else if (antigo >= 2) e.setDamage(e.getDamage() * 0.6);
            }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        if (!plugin.settings().conjuntosMinerais) return;
        Entity d = e.getDamager();
        LivingEntity atacante = d instanceof LivingEntity l ? l : d instanceof Projectile pr && pr.getShooter() instanceof LivingEntity l ? l : null;
        // Quem apanha: Gelo (4) deixa o atacante lento.
        if (e.getEntity() instanceof Player vitima && atacante != null && atacante != vitima
                && pecas(vestidos(vitima), ConjuntoMineral.GELO) >= 4) {
            atacante.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true, true));
            atacante.getWorld().spawnParticle(Particle.SNOWFLAKE, atacante.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.01);
        }
        // Quem bate: Pérola (4) na água ou na chuva.
        if (atacante instanceof Player p && pecas(vestidos(p), ConjuntoMineral.PEROLA) >= 4 && (p.isInWater() || naChuva(p))) {
            e.setDamage(e.getDamage() * 1.15);
        }
    }

    private static boolean naChuva(Player p) {
        return p.getWorld().hasStorm() && p.getLocation().getBlockY() >= p.getWorld().getHighestBlockYAt(p.getLocation());
    }

    /** Pedra-Sangue: depois do dano final, a cura. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoRoubar(EntityDamageByEntityEvent e) {
        if (!plugin.settings().conjuntosMinerais || !(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity)) return;
        int n = pecas(vestidos(p), ConjuntoMineral.PEDRA_SANGUE);
        if (n < 2) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max == null || p.isDead()) return;
        double cura = e.getFinalDamage() * (n >= 4 ? 0.12 : 0.05);
        if (cura > 0) p.setHealth(Math.min(max.getValue(), p.getHealth() + cura));
    }

    /** Quartzo Negro (2): os piglins não escolhem você como alvo. */
    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetLivingEntityEvent e) {
        if (!plugin.settings().conjuntosMinerais || !(e.getEntity() instanceof Piglin) || !(e.getTarget() instanceof Player p)) return;
        if (pecas(vestidos(p), ConjuntoMineral.QUARTZO_NEGRO) >= 2) e.setCancelled(true);
    }

    // =====================================================================
    //  As picaretas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMinerar(BlockDropItemEvent e) {
        if (!plugin.settings().conjuntosMinerais) return;
        Material bloco = e.getBlockState().getType();
        if (!bloco.name().endsWith("_ORE")) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        ConjuntoMineral ferramenta = mao.getType() == Peca.PICARETA.base ? ConjuntoMineral.de(mao) : null;
        double chanceExtra = (ferramenta == ConjuntoMineral.AMBAR ? 0.10 : 0) + (pecas(vestidos(p), ConjuntoMineral.AMBAR) >= 4 ? 0.10 : 0);
        boolean fundir = ferramenta == ConjuntoMineral.MAGMA;
        if (chanceExtra == 0 && !fundir) return;
        int fundidos = 0;
        for (Item it : e.getItems()) {
            ItemStack s = it.getItemStack();
            if (s.getType() == bloco) continue; // toque suave: sai o bloco, nada muda
            if (fundir) {
                Material lingote = switch (s.getType()) {
                    case RAW_IRON -> Material.IRON_INGOT;
                    case RAW_GOLD -> Material.GOLD_INGOT;
                    case RAW_COPPER -> Material.COPPER_INGOT;
                    default -> null;
                };
                if (lingote != null) {
                    fundidos += s.getAmount();
                    s = new ItemStack(lingote, s.getAmount());
                }
            }
            if (chanceExtra > 0 && ThreadLocalRandom.current().nextDouble() < chanceExtra) s.setAmount(s.getAmount() + 1);
            it.setItemStack(s);
        }
        if (fundidos > 0) {
            p.giveExp(fundidos);
            p.getWorld().spawnParticle(Particle.FLAME, e.getBlock().getLocation().add(0.5, 0.5, 0.5), 8, 0.25, 0.25, 0.25, 0.01);
        }
    }

    // =====================================================================
    //  Conserto na bigorna: só com o mineral do plugin
    // =====================================================================

    /** A peça e o mineral certo (a bigorna não pode recusar). */
    public static boolean consertoComMineral(ItemStack peca, ItemStack mineral) {
        ConjuntoMineral c = ConjuntoMineral.de(peca);
        return c != null && mineral != null && Raro.de(mineral) == c.mineral;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararBigorna(PrepareAnvilEvent e) {
        ItemStack a = e.getInventory().getFirstItem(), b = e.getInventory().getSecondItem();
        ConjuntoMineral c = ConjuntoMineral.de(a);
        if (c == null || b == null || b.isEmpty()) return;
        // O item comum do jogo por baixo do mineral (bruto de ouro, de cobre...) não conserta.
        if (b.getType() == c.mineral.base() && Raro.de(b) != c.mineral) e.setResult(null);
    }
}
