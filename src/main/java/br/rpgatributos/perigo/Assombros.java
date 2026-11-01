package br.rpgatributos.perigo;

import br.rpgatributos.ModoTeste;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Variedade;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Assombros (3.16): monstros novos feitos de monstros do jogo.
 * <ul>
 *   <li><b>Espantalho:</b> de noite, perto de plantações. Só anda quando ninguém está olhando.</li>
 *   <li><b>Banshee:</b> pântanos de noite. O grito deixa lento e escurece a vista.</li>
 *   <li><b>Golem de Musgo:</b> cavernas exuberantes. Lento e forte; deixa Musgo Vivo e sementes raras.</li>
 *   <li><b>Aranha-Tecelã:</b> prende quem caça em teias e chama aranhas pequenas. Deixa Seda Fina.</li>
 *   <li><b>Cavaleiro Sem Cabeça:</b> lua cheia, em quem anda pela estrada. Deixa a Cabeça (troféu).</li>
 *   <li><b>Afogado-Âncora:</b> rios. Puxa para o fundo quem está na água.</li>
 * </ul>
 * Nascem no lugar de um monstro comum (respeitam o limite de monstros) e não são salvos no mundo.
 */
public final class Assombros implements Listener {

    public enum Tipo {
        ESPANTALHO("Espantalho", 0xE8C15A), BANSHEE("Banshee", 0xCFE8F0), GOLEM_DE_MUSGO("Golem de Musgo", 0x6AAA3A),
        TECELA("Aranha-Tecelã", 0xEDE7F6), CAVALEIRO("Cavaleiro Sem Cabeça", 0xFF8F00), AFOGADO_ANCORA("Afogado-Âncora", 0x5E7D6E);

        final String nome;
        final TextColor cor;

        Tipo(String nome, int cor) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
        }

        public String nome() { return nome; }
    }

    static final NamespacedKey K_ASSOMBRO = new NamespacedKey("rpgatributos", "assombro");
    private static final NamespacedKey K_FILHOTE = new NamespacedKey("rpgatributos", "tecela_filhote");

    private final RPGAtributos plugin;
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private final Map<UUID, Long> ultimoPoder = new HashMap<>();
    private int ciclo;

    public Assombros(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private boolean ligado(String chave) {
        return plugin.getConfig().getBoolean("assombros." + chave, true);
    }

    public static Tipo tipo(Entity e) {
        String s = e.getPersistentDataContainer().get(K_ASSOMBRO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Tipo.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Tipo tipoPorNome(String s) {
        for (Tipo t : Tipo.values()) if (t.name().equalsIgnoreCase(s)) return t;
        return null;
    }

    private static boolean noite(World w) {
        long t = w.getTime();
        return t >= 12800 && t <= 23200;
    }

    private static boolean mundoValido(World w) {
        return w.equals(Bukkit.getWorlds().getFirst()) || ModoTeste.ehTeste(w);
    }

    private static String bioma(Location l) {
        return l.getWorld().getBiome(l).getKey().getKey();
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    private static void vida(LivingEntity e, double v) {
        base(e, Attribute.MAX_HEALTH, v);
        e.setHealth(v);
    }

    private static void marcar(LivingEntity e, Tipo t) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(true);
        e.getPersistentDataContainer().set(K_ASSOMBRO, PersistentDataType.STRING, t.name());
        e.customName(Component.text(t.nome, t.cor));
        e.setCustomNameVisible(false);
    }

    private int contar(Location l, Tipo t, double raio) {
        int n = 0;
        for (Entity e : l.getWorld().getNearbyEntities(l, raio, 32, raio)) if (tipo(e) == t) n++;
        return n;
    }

    // =====================================================================
    //  Nascer no lugar de um monstro comum
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !mundoValido(e.getLocation().getWorld())) return;
        Location l = e.getLocation();
        EntityType et = e.getEntityType();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String b = bioma(l);
        boolean noite = noite(l.getWorld());
        Tipo t = null;
        boolean comum = et == EntityType.ZOMBIE || et == EntityType.SKELETON || et == EntityType.HUSK;
        if (et == EntityType.DROWNED && b.contains("river") && ligado("afogado-ancora") && r.nextDouble() < 0.25) t = Tipo.AFOGADO_ANCORA;
        else if (et == EntityType.SPIDER && ligado("tecela") && r.nextDouble() < 0.12) t = Tipo.TECELA;
        else if (b.equals("lush_caves") && (comum || et == EntityType.CREEPER) && ligado("golem-de-musgo") && r.nextDouble() < 0.15) t = Tipo.GOLEM_DE_MUSGO;
        else if (noite && b.contains("swamp") && (comum || et == EntityType.WITCH) && ligado("banshee") && r.nextDouble() < 0.15) t = Tipo.BANSHEE;
        else if (noite && comum && ligado("espantalho") && r.nextDouble() < 0.3 && plantacaoPerto(l.getBlock())) t = Tipo.ESPANTALHO;
        if (t == null) return;
        int limite = t == Tipo.TECELA || t == Tipo.ESPANTALHO || t == Tipo.AFOGADO_ANCORA ? 2 : 1;
        if (contar(l, t, 48) >= limite) return;
        e.setCancelled(true);
        Tipo final_ = t;
        Bukkit.getScheduler().runTask(plugin, () -> soltar(final_, l));
    }

    private static boolean plantacaoPerto(Block b) {
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) for (int dy = -2; dy <= 1; dy++) {
            if (b.getRelative(dx, dy, dz).getType() == Material.FARMLAND) return true;
        }
        return false;
    }

    /** Faz nascer um assombro (também pelo /rpgadmin). */
    public LivingEntity soltar(Tipo t, Location l) {
        World w = l.getWorld();
        return switch (t) {
            case ESPANTALHO -> w.spawn(l, Zombie.class, z -> {
                marcar(z, t);
                z.setAdult();
                z.setShouldBurnInDay(false);
                base(z, Attribute.SCALE, 1.15);
                vida(z, 30);
                base(z, Attribute.MOVEMENT_SPEED, 0.34);
                EntityEquipment eq = z.getEquipment();
                eq.setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                eq.setChestplate(couro(Material.LEATHER_CHESTPLATE, Color.fromRGB(0x8D6E63)));
                eq.setLeggings(couro(Material.LEATHER_LEGGINGS, Color.fromRGB(0x5D4037)));
                eq.setItemInMainHand(new ItemStack(Material.WOODEN_HOE));
                semDrop(eq);
            });
            case BANSHEE -> w.spawn(l.clone().add(0, 1.5, 0), Vex.class, v -> {
                marcar(v, t);
                v.setLimitedLifetime(false);
                base(v, Attribute.SCALE, 1.5);
                vida(v, 30);
                v.getEquipment().setItemInMainHand(null);
            });
            case GOLEM_DE_MUSGO -> w.spawn(l, IronGolem.class, g -> {
                marcar(g, t);
                g.setPlayerCreated(false);
                base(g, Attribute.SCALE, 0.9);
                vida(g, 70);
                base(g, Attribute.MOVEMENT_SPEED, 0.2);
            });
            case TECELA -> w.spawn(l, Spider.class, s -> {
                marcar(s, t);
                base(s, Attribute.SCALE, 1.5);
                vida(s, 30);
            });
            case AFOGADO_ANCORA -> w.spawn(l, Drowned.class, d -> {
                marcar(d, t);
                d.setAdult();
                base(d, Attribute.SCALE, 1.4);
                vida(d, 40);
                d.getEquipment().setItemInMainHand(new ItemStack(Material.ANVIL));
                d.getEquipment().setItemInMainHandDropChance(0);
            });
            case CAVALEIRO -> cavaleiro(l);
        };
    }

    private static ItemStack couro(Material m, Color c) {
        ItemStack i = new ItemStack(m);
        i.editMeta(LeatherArmorMeta.class, x -> x.setColor(c));
        return i;
    }

    private static void semDrop(EntityEquipment eq) {
        eq.setHelmetDropChance(0);
        eq.setChestplateDropChance(0);
        eq.setLeggingsDropChance(0);
        eq.setBootsDropChance(0);
        eq.setItemInMainHandDropChance(0);
        eq.setItemInOffHandDropChance(0);
    }

    /** O Cavaleiro: um zumbi invisível de armadura (só a armadura aparece: sem cabeça), num cavalo esqueleto. */
    private LivingEntity cavaleiro(Location l) {
        World w = l.getWorld();
        SkeletonHorse cavalo = w.spawn(l, SkeletonHorse.class, h -> {
            h.setPersistent(false);
            h.setRemoveWhenFarAway(true);
            h.setTamed(true);
            vida(h, 40);
            base(h, Attribute.MOVEMENT_SPEED, 0.32);
            h.getPersistentDataContainer().set(K_ASSOMBRO, PersistentDataType.STRING, "CAVALO");
        });
        Zombie z = w.spawn(l, Zombie.class, x -> {
            marcar(x, Tipo.CAVALEIRO);
            x.setAdult();
            x.setShouldBurnInDay(false);
            x.setCustomNameVisible(true);
            vida(x, 100);
            base(x, Attribute.ATTACK_DAMAGE, 7);
            base(x, Attribute.FOLLOW_RANGE, 40);
            x.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
            EntityEquipment eq = x.getEquipment();
            eq.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            eq.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            eq.setItemInOffHand(new ItemStack(Material.JACK_O_LANTERN));
            semDrop(eq);
        });
        cavalo.addPassenger(z);
        BossBar barra = BossBar.bossBar(Component.text("🎃 Cavaleiro Sem Cabeça", Tipo.CAVALEIRO.cor), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        barras.put(z.getUniqueId(), barra);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(l) > 80 * 80) continue;
            p.sendMessage(Component.text("Um galope ecoa na estrada... a lua está cheia.", Tipo.CAVALEIRO.cor));
            p.playSound(p.getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, 1.2f, 0.6f);
        }
        return z;
    }

    // =====================================================================
    //  Comportamento
    // =====================================================================

    /** A cada 2 ticks: o Espantalho congela quando alguém olha para ele. */
    public void tick() {
        for (World w : Bukkit.getWorlds()) {
            if (!mundoValido(w)) continue;
            for (Zombie z : w.getEntitiesByClass(Zombie.class)) {
                if (tipo(z) != Tipo.ESPANTALHO) continue;
                boolean visto = false;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() == GameMode.SPECTATOR) continue;
                    Location olho = p.getEyeLocation();
                    Vector ate = z.getEyeLocation().toVector().subtract(olho.toVector());
                    if (ate.lengthSquared() > 40 * 40) continue;
                    if (ate.normalize().dot(olho.getDirection()) > 0.8 && p.hasLineOfSight(z)) { visto = true; break; }
                }
                if (visto == z.hasAI()) {
                    z.setAI(!visto);
                    if (!visto) w.playSound(z.getLocation(), Sound.BLOCK_BAMBOO_WOOD_STEP, 0.6f, 0.5f);
                }
            }
        }
    }

    /** Chamado a cada segundo pelo plugin. */
    public void segundo() {
        ciclo++;
        long agora = System.currentTimeMillis();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (World w : Bukkit.getWorlds()) {
            if (!mundoValido(w)) continue;
            boolean noite = noite(w);
            for (LivingEntity e : w.getLivingEntities()) {
                Tipo t = tipo(e);
                if (t == null) continue;
                // De dia, as assombrações da noite somem.
                if (!noite && (t == Tipo.ESPANTALHO || t == Tipo.BANSHEE || t == Tipo.CAVALEIRO)) {
                    w.spawnParticle(Particle.LARGE_SMOKE, e.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.02);
                    if (e.getVehicle() != null) e.getVehicle().remove();
                    e.remove();
                    continue;
                }
                switch (t) {
                    case BANSHEE -> {
                        if (agora - ultimoPoder.getOrDefault(e.getUniqueId(), 0L) < 8000) break;
                        Player alvo = maisPerto(e, 12);
                        if (alvo == null) break;
                        ultimoPoder.put(e.getUniqueId(), agora);
                        w.playSound(e.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.6f, 0.6f);
                        w.spawnParticle(Particle.SCULK_SOUL, e.getLocation().add(0, 1, 0), 30, 1.5, 1, 1.5, 0.02);
                        for (Player p : w.getPlayers()) {
                            if (p.getLocation().distanceSquared(e.getLocation()) > 10 * 10 || p.getGameMode() == GameMode.CREATIVE) continue;
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                        }
                    }
                    case GOLEM_DE_MUSGO -> {
                        if (e instanceof Mob m && !(m.getTarget() instanceof Player)) {
                            Player alvo = maisPerto(e, 16);
                            if (alvo != null) m.setTarget(alvo);
                        }
                        if (ciclo % 2 == 0) w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, e.getLocation().add(0, 1.5, 0), 4, 0.5, 0.6, 0.5, 0);
                    }
                    case TECELA -> {
                        if (agora - ultimoPoder.getOrDefault(e.getUniqueId(), 0L) < 6000) break;
                        Player alvo = e instanceof Mob m && m.getTarget() instanceof Player tp ? tp : null;
                        if (alvo == null || alvo.getLocation().distanceSquared(e.getLocation()) > 10 * 10) break;
                        ultimoPoder.put(e.getUniqueId(), agora);
                        teia(alvo.getLocation().getBlock());
                        w.playSound(e.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.6f);
                        int filhotes = 0;
                        for (Entity o : e.getNearbyEntities(12, 6, 12)) if (o.getPersistentDataContainer().has(K_FILHOTE)) filhotes++;
                        for (int i = filhotes; i < Math.min(4, filhotes + 2); i++) {
                            w.spawn(e.getLocation(), CaveSpider.class, c -> {
                                c.setPersistent(false);
                                c.setRemoveWhenFarAway(true);
                                c.getPersistentDataContainer().set(K_FILHOTE, PersistentDataType.BYTE, (byte) 1);
                                c.setTarget(alvo);
                            });
                        }
                    }
                    case CAVALEIRO -> {
                        if (e instanceof Mob m && !(m.getTarget() instanceof Player)) {
                            Player alvo = maisPerto(e, 32);
                            if (alvo != null) m.setTarget(alvo);
                        }
                        // O cavalo segue o alvo do cavaleiro.
                        if (e.getVehicle() instanceof Mob cavalo && e instanceof Mob m && m.getTarget() != null) {
                            cavalo.getPathfinder().moveTo(m.getTarget().getLocation(), 2.0);
                        }
                        BossBar barra = barras.get(e.getUniqueId());
                        if (barra != null) {
                            var max = e.getAttribute(Attribute.MAX_HEALTH);
                            barra.progress((float) Math.max(0, Math.min(1, e.getHealth() / (max == null ? 100 : max.getValue()))));
                            for (Player p : w.getPlayers()) {
                                if (p.getLocation().distanceSquared(e.getLocation()) < 40 * 40) p.showBossBar(barra);
                                else p.hideBossBar(barra);
                            }
                        }
                        if (ciclo % 3 == 0) w.spawnParticle(Particle.FLAME, e.getLocation().add(0, 1.4, 0), 3, 0.2, 0.2, 0.2, 0.01);
                    }
                    default -> { }
                }
            }
        }
        if (ciclo % 30 == 0) cavaleiroNaEstrada(r);
        // Barras de cavaleiros que sumiram.
        for (Iterator<Map.Entry<UUID, BossBar>> it = barras.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity e = Bukkit.getEntity(en.getKey());
            if (e != null && e.isValid()) continue;
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(en.getValue());
            it.remove();
        }
    }

    private static Player maisPerto(Entity e, double raio) {
        Player melhor = null;
        double md = raio * raio;
        for (Player p : e.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            double d = p.getLocation().distanceSquared(e.getLocation());
            if (d < md) { md = d; melhor = p; }
        }
        return melhor;
    }

    /** Uma teia nos pés do alvo que se desfaz sozinha. */
    private void teia(Block b) {
        if (!b.getType().isAir()) return;
        b.setType(Material.COBWEB);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (b.getType() == Material.COBWEB) b.setType(Material.AIR); }, 120);
    }

    /** Lua cheia: quem anda na estrada (caminho de terra, cascalho) pode ouvir o galope. */
    private void cavaleiroNaEstrada(ThreadLocalRandom r) {
        if (!ligado("cavaleiro")) return;
        for (World w : Bukkit.getWorlds()) {
            if (!mundoValido(w) || !noite(w) || (w.getFullTime() / 24000) % 8 != 0) continue;
            boolean jaTem = false;
            for (Zombie z : w.getEntitiesByClass(Zombie.class)) if (tipo(z) == Tipo.CAVALEIRO) { jaTem = true; break; }
            if (jaTem) continue;
            for (Player p : w.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                Material chao = p.getLocation().clone().subtract(0, 0.2, 0).getBlock().getType();
                if (chao != Material.DIRT_PATH && chao != Material.GRAVEL && chao != Material.COARSE_DIRT) continue;
                if (r.nextDouble() > 0.08) continue;
                Location l = longeNaFrente(p, r);
                if (l != null) { soltar(Tipo.CAVALEIRO, l); return; }
            }
        }
    }

    private static Location longeNaFrente(Player p, ThreadLocalRandom r) {
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
        dir.normalize().multiply(24 + r.nextInt(8));
        Location l = p.getLocation().add(dir);
        if (!l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) return null;
        Block chao = l.getWorld().getHighestBlockAt(l.getBlockX(), l.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (chao.isLiquid()) return null;
        return chao.getLocation().add(0.5, 1, 0.5);
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (tipo(e.getDamager()) == Tipo.AFOGADO_ANCORA && e.getEntity() instanceof Player p && p.isInWater()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.setVelocity(p.getVelocity().setY(-0.9));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1));
            });
            p.sendActionBar(Component.text("⚓ A corrente te puxa para o fundo!", Tipo.AFOGADO_ANCORA.cor));
        }
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        Tipo t = tipo(e.getEntity());
        if (t == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> drops = e.getDrops();
        drops.clear();
        switch (t) {
            case ESPANTALHO -> {
                drops.add(Raro.PALHA_ENCANTADA.criar(1 + r.nextInt(2)));
                drops.add(new ItemStack(Material.WHEAT, 2 + r.nextInt(3)));
                if (r.nextDouble() < 0.15) drops.add(Variedade.values()[r.nextInt(Variedade.values().length)].semente(1));
            }
            case BANSHEE -> drops.add(Raro.LAGRIMA_DA_BANSHEE.criar());
            case GOLEM_DE_MUSGO -> {
                drops.add(Raro.MUSGO_VIVO.criar(1 + r.nextInt(2)));
                drops.add(new ItemStack(Material.MOSS_BLOCK, 2 + r.nextInt(3)));
                if (r.nextDouble() < 0.25) drops.add(Variedade.values()[r.nextInt(Variedade.values().length)].semente(1));
            }
            case TECELA -> {
                drops.add(Raro.SEDA_FINA.criar(1 + r.nextInt(2)));
                drops.add(new ItemStack(Material.STRING, 2));
            }
            case CAVALEIRO -> {
                drops.add(Raro.CABECA_DO_CAVALEIRO.criar());
                drops.add(new ItemStack(Material.EMERALD, 3 + r.nextInt(4)));
            }
            case AFOGADO_ANCORA -> {
                drops.add(Raro.CORRENTE_DO_AFOGADO.criar());
                if (r.nextDouble() < 0.1) drops.add(new ItemStack(Material.NAUTILUS_SHELL));
            }
        }
        e.setDroppedExp(e.getDroppedExp() + 10);
        BossBar barra = barras.remove(e.getEntity().getUniqueId());
        if (barra != null) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
        if (e.getEntity().getVehicle() != null) e.getEntity().getVehicle().remove();
        ultimoPoder.remove(e.getEntity().getUniqueId());
        Player k = e.getEntity().getKiller();
        if (k == null) return;
        plugin.stats().darXp(k, Skill.COMBATE, t == Tipo.CAVALEIRO ? 120 : 30);
        plugin.titulos().registrar(k, "assombros", 1);
        if (t == Tipo.CAVALEIRO) {
            plugin.titulos().registrar(k, "cavaleiro", 1);
            Bukkit.broadcast(Component.text("🎃 " + k.getName() + " derrubou o Cavaleiro Sem Cabeça!", Tipo.CAVALEIRO.cor));
        }
    }

    /** Palha Encantada (plantação cresce) e Musgo Vivo (pedra vira musgo, terra vira grama). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Raro r = Raro.de(e.getItem());
        if (r != Raro.PALHA_ENCANTADA && r != Raro.MUSGO_VIVO) return;
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        e.setCancelled(true);
        if (!plugin.territorios().podeConstruir(p.getUniqueId(), b.getLocation())) {
            p.sendActionBar(Component.text("✖ Este lugar é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        boolean usou = false;
        if (r == Raro.PALHA_ENCANTADA && b.getBlockData() instanceof Ageable a && a.getAge() < a.getMaximumAge()) {
            a.setAge(a.getAge() + 1);
            b.setBlockData(a);
            b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3);
            plugin.stats().darXp(p, Skill.AGRICULTURA, 2);
            usou = true;
        }
        if (r == Raro.MUSGO_VIVO) {
            Material novo = switch (b.getType()) {
                case STONE, COBBLESTONE -> Material.MOSSY_COBBLESTONE;
                case STONE_BRICKS -> Material.MOSSY_STONE_BRICKS;
                case DIRT, COARSE_DIRT -> Material.GRASS_BLOCK;
                case COBBLESTONE_WALL -> Material.MOSSY_COBBLESTONE_WALL;
                default -> null;
            };
            if (novo != null) {
                b.setType(novo);
                b.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, b.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.2, 0.3);
                usou = true;
            }
        }
        if (usou) {
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_MOSS_PLACE, 0.8f, 1.2f);
            if (p.getGameMode() != GameMode.CREATIVE) e.getItem().setAmount(e.getItem().getAmount() - 1);
        }
    }

    /** Material raro não vira bloco (a Cabeça do Cavaleiro é uma abóbora por baixo). */
    @EventHandler(ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (Raro.de(e.getItemInHand()) != null) e.setCancelled(true);
    }

    public void desligar() {
        for (BossBar b : barras.values()) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(b);
        barras.clear();
    }
}
