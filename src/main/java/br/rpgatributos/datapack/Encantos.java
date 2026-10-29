package br.rpgatributos.datapack;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.mundo.Ceu;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encantamentos do RPGAtributos (3.1). Moram no datapack (mesa de encantamento, livros, bigorna,
 * baús e aldeões como os do jogo); vários funcionam só pelo datapack. Os que precisam do plugin:
 * <ul>
 *   <li><b>Golpe Lunar</b>: dano extra à noite, mais na lua cheia e na Lua de Sangue;</li>
 *   <li><b>Sifão de Mana</b>: cada golpe corpo a corpo devolve mana;</li>
 *   <li><b>Corrente de Trovão</b>: às vezes um raio cai no alvo e salta para os monstros perto;</li>
 *   <li><b>Estilhaço de Gelo</b> (machado): às vezes o golpe estoura em gelo e congela quem está perto;</li>
 *   <li><b>Lâmina do Vento</b>: o empurrão é do datapack; aqui só a rajada que se vê;</li>
 *   <li><b>Flecha Explosiva</b> (besta): a flecha explode onde acerta, sem quebrar blocos;</li>
 *   <li><b>Onda de Choque</b> (botas): cair de alto empurra e machuca os monstros em volta;</li>
 *   <li><b>Passos de Brasa</b> e <b>Trilha Florida</b> (botas): rastro de fogo correndo; flores por onde passa.</li>
 * </ul>
 * Sem o datapack carregado (antes do primeiro reinício) os encantamentos não existem e nada acontece.
 */
public final class Encantos implements Listener {

    /** id → nome, na ordem do Guia. */
    public static final String[][] TODOS = {
            {"roubo_de_vida", "Roubo de Vida"}, {"golpe_lunar", "Golpe Lunar"}, {"ceifador", "Ceifador"},
            {"sifao_de_mana", "Sifão de Mana"}, {"saltador", "Saltador"}, {"passo_ligeiro", "Passo Ligeiro"},
            {"vigor", "Vigor"}, {"braco_longo", "Braço Longo"}, {"veio_generoso", "Veio Generoso"},
            {"flecha_gelida", "Flecha Gélida"}, {"maldicao_do_peso", "Maldição do Peso"},
            {"corrente_de_trovao", "Corrente de Trovão"}, {"estilhaco_de_gelo", "Estilhaço de Gelo"},
            {"lamina_do_vento", "Lâmina do Vento"}, {"flecha_explosiva", "Flecha Explosiva"},
            {"onda_de_choque", "Onda de Choque"}, {"passos_de_brasa", "Passos de Brasa"},
            {"trilha_florida", "Trilha Florida"}};

    private static final NamespacedKey K_EXPLOSIVA = new NamespacedKey("rpgatributos", "flecha_explosiva");
    private static final Material[] FLORES = {Material.POPPY, Material.DANDELION, Material.CORNFLOWER, Material.OXEYE_DAISY,
            Material.AZURE_BLUET, Material.ALLIUM, Material.BLUE_ORCHID, Material.PINK_TULIP, Material.WHITE_TULIP,
            Material.ORANGE_TULIP, Material.RED_TULIP, Material.LILY_OF_THE_VALLEY};

    private final RPGAtributos plugin;
    /** Enquanto o plugin aplica o dano de um efeito (raio, gelo, onda): esse dano não dispara outros efeitos. */
    private boolean emEfeito;

    public Encantos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** O encantamento do datapack; null se o datapack ainda não foi carregado. */
    public static Enchantment pegar(String id) {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(new NamespacedKey("rpgatributos", id));
    }

    /** Quantos encantamentos do RPG o jogo conhece (0 = datapack não carregado). */
    public static int carregados() {
        int n = 0;
        for (String[] e : TODOS) if (pegar(e[0]) != null) n++;
        return n;
    }

    public static List<String> ids() {
        List<String> l = new ArrayList<>();
        for (String[] e : TODOS) l.add(e[0]);
        return l;
    }

    private static int nivel(ItemStack item, String id) {
        if (item == null || item.isEmpty()) return 0;
        Enchantment e = pegar(id);
        return e == null ? 0 : item.getEnchantmentLevel(e);
    }

    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }

    /** Pode levar o efeito em área: monstro ou bicho sem dono, nunca jogador, dono ou companheiro. */
    private static boolean alvoDeArea(Entity x, Player p) {
        if (!(x instanceof LivingEntity) || x instanceof Player || x instanceof ArmorStand || x.equals(p)) return false;
        return !(x instanceof Tameable t && t.isTamed());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (emEfeito) return;
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getEntity() instanceof LivingEntity alvo) || alvo instanceof ArmorStand) return;
        ItemStack arma = p.getInventory().getItemInMainHand();
        World w = p.getWorld();
        Location meio = alvo.getLocation().add(0, alvo.getHeight() * 0.5, 0);

        int lunar = nivel(arma, "golpe_lunar");
        if (lunar > 0) {
            double extra = 0;
            if (plugin.ceu() != null && plugin.ceu().luaDeSangue() && Ceu.noite(w)) extra = 2.5 * lunar;
            else if (w.getEnvironment() == World.Environment.NORMAL && Ceu.noite(w)) extra = (Ceu.fase(w) == 0 ? 2.0 : 1.0) * lunar;
            if (extra > 0) {
                e.setDamage(e.getDamage() + extra);
                w.spawnParticle(Particle.END_ROD, meio, 4, 0.2, 0.3, 0.2, 0.02);
            }
        }

        int sifao = nivel(arma, "sifao_de_mana");
        if (sifao > 0 && plugin.arcano() != null) {
            plugin.arcano().darMana(p, 1.5 * sifao);
            w.spawnParticle(Particle.WITCH, meio, 3, 0.2, 0.2, 0.2, 0);
        }

        int vento = nivel(arma, "lamina_do_vento");
        if (vento > 0) {
            w.spawnParticle(Particle.GUST, meio, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.CLOUD, meio, 6 * vento, 0.3, 0.3, 0.3, 0.08);
            w.playSound(meio, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.7f, 1.3f);
        }

        int trovao = nivel(arma, "corrente_de_trovao");
        if (trovao > 0 && rnd().nextDouble() < 0.07 + 0.05 * (trovao - 1)) semRepetir(() -> trovao(p, alvo, trovao));

        int gelo = nivel(arma, "estilhaco_de_gelo");
        if (gelo > 0 && rnd().nextDouble() < 0.12 + 0.08 * (gelo - 1)) semRepetir(() -> gelo(p, alvo, gelo));
    }

    private void semRepetir(Runnable efeito) {
        emEfeito = true;
        try {
            efeito.run();
        } finally {
            emEfeito = false;
        }
    }

    /** O raio cai no alvo (só o clarão: não queima nada nem acerta você) e salta para até 1 monstro por nível. */
    private void trovao(Player p, LivingEntity alvo, int nivel) {
        World w = alvo.getWorld();
        w.strikeLightningEffect(alvo.getLocation());
        DamageSource raio = DamageSource.builder(DamageType.LIGHTNING_BOLT).withCausingEntity(p).withDirectEntity(p).build();
        alvo.damage(3 + nivel, raio);
        LivingEntity de = alvo;
        List<Entity> ja = new ArrayList<>(List.of(alvo));
        for (int i = 0; i < nivel; i++) {
            LivingEntity prox = null;
            double melhor = 6 * 6;
            for (Entity x : w.getNearbyEntities(de.getLocation(), 6, 4, 6)) {
                if (!(x instanceof Enemy) || ja.contains(x) || !alvoDeArea(x, p)) continue;
                double d = x.getLocation().distanceSquared(de.getLocation());
                if (d < melhor) {
                    melhor = d;
                    prox = (LivingEntity) x;
                }
            }
            if (prox == null) break;
            Location a = de.getLocation().add(0, de.getHeight() * 0.6, 0), b = prox.getLocation().add(0, prox.getHeight() * 0.6, 0);
            Vector passo = b.toVector().subtract(a.toVector());
            int n = (int) Math.max(4, passo.length() * 3);
            passo.multiply(1.0 / n);
            Location c = a.clone();
            for (int k = 0; k <= n; k++) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, c, 2, 0.08, 0.08, 0.08, 0.02, null, true);
                c.add(passo);
            }
            prox.damage(2 + nivel, raio);
            ja.add(prox);
            de = prox;
        }
        w.playSound(alvo.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);
    }

    /** O golpe estoura em gelo: quem está a 3 blocos fica lento e congelando. */
    private void gelo(Player p, LivingEntity alvo, int nivel) {
        World w = alvo.getWorld();
        Location c = alvo.getLocation().add(0, 0.8, 0);
        w.spawnParticle(Particle.SNOWFLAKE, c, 40, 1.4, 0.6, 1.4, 0.05, null, true);
        w.spawnParticle(Particle.BLOCK, c, 30, 1.2, 0.5, 1.2, 0, Material.ICE.createBlockData());
        w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        w.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 1f);
        DamageSource frio = DamageSource.builder(DamageType.FREEZE).withCausingEntity(p).withDirectEntity(p).build();
        for (Entity x : w.getNearbyEntities(c, 3, 2, 3)) {
            if (!alvoDeArea(x, p)) continue;
            LivingEntity v = (LivingEntity) x;
            v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, nivel, false, true));
            v.setFreezeTicks(Math.max(v.getFreezeTicks(), 160));
            if (v != alvo) v.damage(1 + nivel, frio);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoAtirar(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getProjectile() instanceof AbstractArrow flecha)) return;
        int n = nivel(e.getBow(), "flecha_explosiva");
        if (n > 0) flecha.getPersistentDataContainer().set(K_EXPLOSIVA, PersistentDataType.INTEGER, n);
    }

    @EventHandler
    public void aoAcertar(ProjectileHitEvent e) {
        Integer n = e.getEntity().getPersistentDataContainer().get(K_EXPLOSIVA, PersistentDataType.INTEGER);
        if (n == null) return;
        Location l = e.getEntity().getLocation();
        ProjectileSource dono = e.getEntity().getShooter();
        e.getEntity().remove();
        l.getWorld().createExplosion(l, 1.2f + 0.4f * n, false, false, dono instanceof Entity x ? x : null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        int n = nivel(p.getInventory().getBoots(), "onda_de_choque");
        float queda = p.getFallDistance();
        if (n <= 0 || queda < 4) return;
        e.setDamage(e.getDamage() * Math.max(0.25, 1 - 0.2 * n));
        World w = p.getWorld();
        Location c = p.getLocation();
        double raio = 3 + n;
        double dano = Math.min(12, queda * 0.5) * (0.5 + 0.25 * n);
        Block chao = c.clone().add(0, -1, 0).getBlock();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            Location anel = c.clone().add(Math.cos(a) * raio * 0.8, 0.1, Math.sin(a) * raio * 0.8);
            w.spawnParticle(Particle.BLOCK, anel, 6, 0.3, 0.1, 0.3, 0, chao.getType().isSolid() ? chao.getBlockData() : Material.DIRT.createBlockData());
        }
        w.spawnParticle(Particle.EXPLOSION, c, 3, 0.8, 0.2, 0.8, 0);
        w.playSound(c, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
        DamageSource golpe = DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(p).withDirectEntity(p).build();
        semRepetir(() -> {
            for (Entity x : w.getNearbyEntities(c, raio, 2, raio)) {
                if (!alvoDeArea(x, p)) continue;
                Vector fora = x.getLocation().toVector().subtract(c.toVector()).setY(0);
                if (fora.lengthSquared() < 0.01) fora = new Vector(1, 0, 0);
                x.setVelocity(fora.normalize().multiply(0.6 + 0.2 * n).setY(0.45));
                if (x instanceof Enemy) ((LivingEntity) x).damage(dano, golpe);
            }
        });
    }

    /** A cada 4 ticks: rastro de brasa de quem corre e flores de quem anda (botas). */
    public void tick() {
        ThreadLocalRandom r = rnd();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || !p.isOnGround()) continue;
            ItemStack botas = p.getInventory().getBoots();
            if (botas == null || botas.isEmpty()) continue;
            int brasa = nivel(botas, "passos_de_brasa");
            if (brasa > 0 && p.isSprinting()) {
                Location l = p.getLocation().add(0, 0.1, 0);
                World w = p.getWorld();
                w.spawnParticle(Particle.FLAME, l, 4, 0.25, 0.05, 0.25, 0.01);
                w.spawnParticle(Particle.SMALL_FLAME, l, 3, 0.3, 0.05, 0.3, 0.01);
                if (r.nextInt(4) == 0) w.spawnParticle(Particle.LAVA, l, 1, 0.2, 0, 0.2, 0);
                if (brasa >= 2) {
                    for (Entity x : w.getNearbyEntities(l, 1.5, 1.5, 1.5)) {
                        if (x instanceof Enemy && alvoDeArea(x, p)) x.setFireTicks(Math.max(x.getFireTicks(), 60));
                    }
                }
            }
            if (nivel(botas, "trilha_florida") > 0 && r.nextDouble() < 0.35) florir(p);
        }
    }

    private void florir(Player p) {
        Block pe = p.getLocation().getBlock();
        Block chao = pe.getRelative(0, -1, 0);
        if (chao.getType() != Material.GRASS_BLOCK || !pe.getType().isAir()) return;
        if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, pe)) return;
        Block alvo = pe;
        // Às vezes a flor nasce ao lado, para a trilha ficar mais larga que o seu pé.
        ThreadLocalRandom r = rnd();
        if (r.nextBoolean()) {
            Block lado = pe.getRelative(r.nextInt(-1, 2), 0, r.nextInt(-1, 2));
            if (lado.getType().isAir() && lado.getRelative(0, -1, 0).getType() == Material.GRASS_BLOCK) alvo = lado;
        }
        alvo.setType(FLORES[r.nextInt(FLORES.length)]);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, alvo.getLocation().add(0.5, 0.3, 0.5), 2, 0.2, 0.1, 0.2, 0);
    }
}
