package br.rpgatributos.aventura;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Eventos do altar: Lua de Sangue, Chuva de Meteoros e Ondas de Monstros. */
public final class Eventos implements Listener {

    public static final NamespacedKey CHAVE_LUA = new NamespacedKey("rpgatributos", "lua");
    public static final NamespacedKey CHAVE_ONDA = new NamespacedKey("rpgatributos", "onda");

    private static final TextColor COR_METEORO = TextColor.color(0xFF8C00);
    private static final int ONDAS = 5;

    private final RPGAtributos plugin;
    /** Mundo com Lua de Sangue → barra mostrada para quem está nele. */
    private final Map<UUID, BossBar> luas = new HashMap<>();
    private final Map<UUID, Set<UUID>> luaVeem = new HashMap<>();
    /** Altares com chuva de meteoros acontecendo. */
    private final Set<Location> meteoros = new HashSet<>();
    private final Map<Location, Arena> arenas = new HashMap<>();
    private int ciclo;

    public Eventos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static boolean valido(Player p) {
        return p.isOnline() && !p.isDead()
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE);
    }

    public boolean ocupado(Location altar) {
        return meteoros.contains(altar) || arenas.containsKey(altar);
    }

    public static boolean noite(World w) {
        long t = w.getTime();
        return t >= 13000 && t <= 23000;
    }

    public boolean luaAtiva(World w) {
        return luas.containsKey(w.getUID());
    }

    /** Roda a cada segundo. */
    public void tick() {
        ciclo++;
        tickLuas();
        for (Iterator<Arena> it = arenas.values().iterator(); it.hasNext(); ) {
            if (it.next().tick()) it.remove();
        }
    }

    // =====================================================================
    //  Lua de Sangue
    // =====================================================================

    public void iniciarLua(World w, Player quem) {
        BossBar barra = BossBar.bossBar(Component.text("☾ Lua de Sangue ☾", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        luas.put(w.getUID(), barra);
        luaVeem.put(w.getUID(), new HashSet<>());
        for (Player p : w.getPlayers()) {
            p.showTitle(Title.title(Component.text("☾ LUA DE SANGUE ☾", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                    Component.text("Os monstros estão mais fortes até o amanhecer", NamedTextColor.RED),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 0.7f);
        }
        plugin.getServer().broadcast(Component.text("☾ ", NamedTextColor.DARK_RED)
                .append(Component.text(quem == null ? "Alguém" : quem.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" invocou a ", NamedTextColor.GRAY))
                .append(Component.text("Lua de Sangue", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text(" em " + w.getName() + ": dobro de XP e drops até o amanhecer!", NamedTextColor.GRAY)));
    }

    private void tickLuas() {
        for (Iterator<Map.Entry<UUID, BossBar>> it = luas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, BossBar> en = it.next();
            World w = Bukkit.getWorld(en.getKey());
            BossBar barra = en.getValue();
            Set<UUID> veem = luaVeem.get(en.getKey());
            if (w == null || !noite(w)) {
                for (UUID id : veem) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.hideBossBar(barra);
                        p.sendMessage(Component.text("☀ O sol nasceu. A Lua de Sangue acabou.", NamedTextColor.GOLD));
                    }
                }
                luaVeem.remove(en.getKey());
                it.remove();
                continue;
            }
            barra.progress((float) Math.max(0, Math.min(1, (23000 - w.getTime()) / 10000.0)));
            Set<UUID> agora = new HashSet<>();
            for (Player p : w.getPlayers()) {
                agora.add(p.getUniqueId());
                if (veem.add(p.getUniqueId())) p.showBossBar(barra);
            }
            for (Iterator<UUID> v = veem.iterator(); v.hasNext(); ) {
                UUID id = v.next();
                if (agora.contains(id)) continue;
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.hideBossBar(barra);
                v.remove();
            }
            // Monstros extras perto de quem está na superfície (a cada 10 segundos).
            if (ciclo % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (!valido(p) || p.getLocation().getBlockY() < w.getHighestBlockYAt(p.getLocation()) - 2) continue;
                    if (p.getNearbyEntities(32, 16, 32).stream().filter(e -> e instanceof Enemy).count() > 20) continue;
                    for (int i = 0; i < 2; i++) monstroDaLua(p);
                }
            }
        }
    }

    private void monstroDaLua(Player p) {
        double ang = rnd().nextDouble(Math.PI * 2);
        double dist = rnd().nextDouble(16, 26);
        int x = (int) (p.getLocation().getX() + Math.cos(ang) * dist);
        int z = (int) (p.getLocation().getZ() + Math.sin(ang) * dist);
        World w = p.getWorld();
        Location onde = w.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
        if (onde.getBlock().isLiquid() || onde.clone().subtract(0, 1, 0).getBlock().isLiquid()) return;
        EntityType[] tipos = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ZOMBIE, EntityType.HUSK};
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipos[rnd().nextInt(tipos.length)].getEntityClass();
        w.spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, this::fortalecer);
    }

    /** Monstro da lua: +50% vida, +30% dano, +10% velocidade e o dobro de drops. */
    private void fortalecer(LivingEntity e) {
        multiplicar(e, Attribute.MAX_HEALTH, 1.5);
        AttributeInstance vida = e.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) e.setHealth(vida.getValue());
        multiplicar(e, Attribute.ATTACK_DAMAGE, 1.3);
        multiplicar(e, Attribute.MOVEMENT_SPEED, 1.1);
        e.getPersistentDataContainer().set(CHAVE_LUA, PersistentDataType.BYTE, (byte) 1);
    }

    private static void multiplicar(LivingEntity e, Attribute a, double fator) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * fator);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (e.getEntity() instanceof Enemy && luaAtiva(e.getEntity().getWorld())) fortalecer(e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (m.getPersistentDataContainer().has(CHAVE_ONDA)) {
            e.getDrops().clear(); // a recompensa das ondas vem no final
            return;
        }
        if (!m.getPersistentDataContainer().has(CHAVE_LUA) || m.getKiller() == null) return;
        List<ItemStack> copias = new ArrayList<>();
        for (ItemStack i : e.getDrops()) copias.add(i.clone());
        e.getDrops().addAll(copias);
        e.setDroppedExp(e.getDroppedExp() * 2);
        Player p = m.getKiller();
        if (valido(p)) plugin.stats().darXp(p, Skill.COMBATE, plugin.settings().comXpMonstro);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoDeitar(PlayerBedEnterEvent e) {
        if (!luaAtiva(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(Component.text("☾ Os monstros da Lua de Sangue não deixam você dormir!", NamedTextColor.DARK_RED));
    }

    // =====================================================================
    //  Chuva de Meteoros
    // =====================================================================

    public void iniciarMeteoros(Location altar, Player quem) {
        meteoros.add(altar);
        World w = altar.getWorld();
        for (Player p : perto(altar, 60)) {
            p.showTitle(Title.title(Component.text("☄ Chuva de Meteoros ☄", COR_METEORO, TextDecoration.BOLD),
                    Component.text("Cuidado com os impactos!", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
        w.playSound(altar, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.6f);
        int[] t = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, tarefa -> {
            t[0] += 10;
            if (t[0] > 1800) { // 90 segundos
                tarefa.cancel();
                meteoros.remove(altar);
                for (Player p : perto(altar, 60)) p.sendMessage(Component.text("☄ A chuva de meteoros acabou.", COR_METEORO));
                return;
            }
            // Um meteoro a cada 10 segundos, e às vezes um extra.
            if (t[0] % 200 == 0 || rnd().nextInt(40) == 0) meteoro(altar);
        }, 0L, 10L);
    }

    private void meteoro(Location altar) {
        World w = altar.getWorld();
        double ang = rnd().nextDouble(Math.PI * 2);
        double dist = rnd().nextDouble(5, 20);
        int x = (int) (altar.getX() + Math.cos(ang) * dist);
        int z = (int) (altar.getZ() + Math.sin(ang) * dist);
        Location chao = w.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
        Location ceu = chao.clone().add(rnd().nextDouble(-8, 8), 35, rnd().nextDouble(-8, 8));
        Vector passo = chao.toVector().subtract(ceu.toVector()).multiply(1 / 30.0);
        Location pos = ceu.clone();
        w.playSound(chao, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2f, 0.5f);
        plugin.getServer().getScheduler().runTaskTimer(plugin, tarefa -> {
            // Aviso no chão.
            for (int i = 0; i < 12; i++) {
                double a = 2 * Math.PI * i / 12;
                w.spawnParticle(Particle.DUST, chao.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.ORANGE, 1.3f));
            }
            pos.add(passo);
            w.spawnParticle(Particle.FLAME, pos, 15, 0.4, 0.4, 0.4, 0.02);
            w.spawnParticle(Particle.LARGE_SMOKE, pos, 5, 0.3, 0.3, 0.3, 0.01);
            w.spawnParticle(Particle.LAVA, pos, 2, 0.2, 0.2, 0.2);
            if (pos.getY() > chao.getY()) return;
            tarefa.cancel();
            impactoMeteoro(chao);
        }, 0L, 1L);
    }

    private void impactoMeteoro(Location chao) {
        World w = chao.getWorld();
        w.spawnParticle(Particle.EXPLOSION_EMITTER, chao, 1);
        w.spawnParticle(Particle.LAVA, chao, 30, 1, 0.5, 1);
        w.playSound(chao, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
        for (Entity e : w.getNearbyEntities(chao, 3, 3, 3)) {
            if (!(e instanceof LivingEntity le) || e instanceof Player p && !valido(p)) continue;
            le.damage(5);
            le.setFireTicks(Math.max(le.getFireTicks(), 60));
        }
        // O meteoro se parte e espalha minérios.
        List<ItemStack> saque = new ArrayList<>();
        saque.add(new ItemStack(Material.IRON_INGOT, rnd().nextInt(2, 7)));
        if (rnd().nextDouble() < 0.5) saque.add(new ItemStack(Material.GOLD_INGOT, rnd().nextInt(1, 5)));
        if (rnd().nextDouble() < 0.35) saque.add(new ItemStack(Material.EMERALD, rnd().nextInt(1, 4)));
        if (rnd().nextDouble() < 0.3) saque.add(new ItemStack(Material.LAPIS_LAZULI, rnd().nextInt(4, 9)));
        if (rnd().nextDouble() < 0.3) saque.add(new ItemStack(Material.REDSTONE, rnd().nextInt(4, 9)));
        if (rnd().nextDouble() < 0.3) saque.add(new ItemStack(Material.AMETHYST_SHARD, rnd().nextInt(2, 5)));
        if (rnd().nextDouble() < 0.15) saque.add(new ItemStack(Material.DIAMOND, rnd().nextInt(1, 3)));
        if (rnd().nextDouble() < 0.08) saque.add(Raro.FRAGMENTO_DE_FORJA.criar());
        if (rnd().nextDouble() < 0.03) saque.add(new ItemStack(Material.NETHERITE_SCRAP));
        for (ItemStack i : saque) {
            Item item = w.dropItem(chao.clone().add(0, 0.5, 0), i);
            item.setVelocity(new Vector(rnd().nextDouble(-0.25, 0.25), 0.45, rnd().nextDouble(-0.25, 0.25)));
        }
    }

    // =====================================================================
    //  Ondas de Monstros
    // =====================================================================

    /** @return false se não há jogadores perto do altar para lutar. */
    public boolean iniciarOndas(Location altar, Player quem) {
        Arena a = new Arena(altar);
        for (Player p : perto(altar, 30)) a.jogadores.add(p.getUniqueId());
        if (a.jogadores.isEmpty()) return false;
        arenas.put(altar, a);
        for (Player p : a.online()) {
            p.showTitle(Title.title(Component.text("⚔ Ondas de Monstros ⚔", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("Prepare-se! A primeira onda chega em 5 segundos", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 1f);
        }
        return true;
    }

    private final class Arena {
        final Location altar;
        final Set<UUID> jogadores = new HashSet<>();
        final List<UUID> vivos = new ArrayList<>();
        final BossBar barra = BossBar.bossBar(Component.text("⚔ Ondas de Monstros"), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_6);
        final Set<UUID> veem = new HashSet<>();
        int onda;
        int espera = 5;
        int semJogadores;

        Arena(Location altar) {
            this.altar = altar;
        }

        List<Player> online() {
            List<Player> l = new ArrayList<>();
            for (UUID id : jogadores) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) l.add(p);
            }
            return l;
        }

        /** @return true quando a arena acabou. */
        boolean tick() {
            for (Player p : perto(altar, 30)) jogadores.add(p.getUniqueId());
            vivos.removeIf(id -> {
                Entity e = Bukkit.getEntity(id);
                return e == null || e.isDead() || !e.isValid();
            });

            boolean alguem = !perto(altar, 40).isEmpty();
            semJogadores = alguem ? 0 : semJogadores + 1;
            if (semJogadores >= 30) {
                terminar(false);
                return true;
            }

            if (vivos.isEmpty()) {
                if (onda >= ONDAS) {
                    terminar(true);
                    return true;
                }
                if (espera-- <= 0) {
                    onda++;
                    soltarOnda();
                    espera = 5;
                }
            } else {
                // Monstros perdidos longe da arena voltam para perto do altar.
                for (UUID id : vivos) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null && e.getLocation().distanceSquared(altar) > 45 * 45) e.teleport(pontoDaArena());
                    if (e instanceof Mob m && !(m.getTarget() instanceof Player)) m.setTarget(proximo(e.getLocation()));
                }
            }
            atualizarBarra();
            return false;
        }

        private Player proximo(Location l) {
            Player melhor = null;
            double d = Double.MAX_VALUE;
            for (Player p : online()) {
                if (!valido(p) || !p.getWorld().equals(l.getWorld())) continue;
                double x = p.getLocation().distanceSquared(l);
                if (x < d) { d = x; melhor = p; }
            }
            return melhor;
        }

        private void atualizarBarra() {
            String texto = vivos.isEmpty() && onda < ONDAS
                    ? "⚔ Próxima onda em " + Math.max(0, espera + 1) + "s"
                    : "⚔ Onda " + onda + "/" + ONDAS + "  ·  " + vivos.size() + " monstros";
            barra.name(Component.text(texto, NamedTextColor.RED, TextDecoration.BOLD));
            barra.progress((float) Math.max(0, Math.min(1, onda / (double) ONDAS)));
            for (Player p : online()) if (veem.add(p.getUniqueId())) p.showBossBar(barra);
        }

        private Location pontoDaArena() {
            World w = altar.getWorld();
            for (int tentativa = 0; tentativa < 10; tentativa++) {
                double ang = rnd().nextDouble(Math.PI * 2);
                double dist = rnd().nextDouble(7, 12);
                int x = (int) (altar.getX() + Math.cos(ang) * dist);
                int z = (int) (altar.getZ() + Math.sin(ang) * dist);
                Location l = w.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
                if (Math.abs(l.getY() - altar.getY()) <= 6 && !l.getBlock().isLiquid()) return l;
            }
            return altar.clone().add(0.5, 1, 0.5);
        }

        private void soltarOnda() {
            int n = online().size();
            double escala = 1 + 0.5 * Math.max(0, n - 1);
            Map<EntityType, Integer> m = new LinkedHashMap<>();
            switch (onda) {
                case 1 -> { m.put(EntityType.ZOMBIE, 6); m.put(EntityType.SKELETON, 2); }
                case 2 -> { m.put(EntityType.SKELETON, 5); m.put(EntityType.SPIDER, 4); m.put(EntityType.ZOMBIE, 2); }
                case 3 -> { m.put(EntityType.HUSK, 4); m.put(EntityType.STRAY, 3); m.put(EntityType.WITCH, 2); }
                case 4 -> { m.put(EntityType.VINDICATOR, 3); m.put(EntityType.PILLAGER, 3); m.put(EntityType.WITCH, 1); }
                default -> { m.put(EntityType.RAVAGER, 1); m.put(EntityType.VINDICATOR, 2); m.put(EntityType.EVOKER, 1); }
            }
            m.forEach((tipo, qtd) -> {
                for (int i = 0; i < Math.round(qtd * escala); i++) monstro(tipo, false);
            });
            if (onda == ONDAS) monstro(EntityType.ZOMBIE, true);
            for (Player p : online()) {
                p.showTitle(Title.title(Component.text("Onda " + onda, NamedTextColor.RED, TextDecoration.BOLD),
                        Component.text(onda == ONDAS ? "A última! Cuidado com o Campeão" : vivos.size() + " monstros", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
                p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.8f, 1.2f);
            }
        }

        private void monstro(EntityType tipo, boolean campeao) {
            Location onde = pontoDaArena();
            @SuppressWarnings("unchecked")
            Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
            LivingEntity e = onde.getWorld().spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, l -> {
                l.getPersistentDataContainer().set(CHAVE_ONDA, PersistentDataType.BYTE, (byte) 1);
                l.setPersistent(false);
                l.setRemoveWhenFarAway(false);
                l.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
                if (campeao) {
                    l.customName(Component.text("☠ Campeão da Arena", NamedTextColor.RED, TextDecoration.BOLD));
                    l.setCustomNameVisible(true);
                    AttributeInstance vida = l.getAttribute(Attribute.MAX_HEALTH);
                    if (vida != null) { vida.setBaseValue(120); l.setHealth(120); }
                    AttributeInstance escala = l.getAttribute(Attribute.SCALE);
                    if (escala != null) escala.setBaseValue(1.8);
                    EntityEquipment eq = l.getEquipment();
                    if (eq != null) {
                        eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                        eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                        eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                        eq.setHelmetDropChance(0);
                        eq.setChestplateDropChance(0);
                        eq.setItemInMainHandDropChance(0);
                    }
                }
            });
            if (e instanceof Mob mob) mob.setTarget(proximo(onde));
            vivos.add(e.getUniqueId());
            onde.getWorld().spawnParticle(Particle.LARGE_SMOKE, onde, 10, 0.3, 0.6, 0.3, 0.02);
        }

        private void terminar(boolean venceu) {
            for (UUID id : veem) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.hideBossBar(barra);
            }
            for (UUID id : vivos) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            if (!venceu) {
                for (Player p : online()) p.sendMessage(Component.text("⚔ A arena foi abandonada. As Ondas de Monstros acabaram.", NamedTextColor.GRAY));
                return;
            }
            World w = altar.getWorld();
            Location topo = altar.clone().add(0.5, 1.2, 0.5);
            List<ItemStack> premio = new ArrayList<>(List.of(
                    new ItemStack(Material.EMERALD, rnd().nextInt(12, 25)),
                    new ItemStack(Material.DIAMOND, rnd().nextInt(2, 5)),
                    new ItemStack(Material.GOLDEN_APPLE, 2),
                    new ItemStack(Material.EXPERIENCE_BOTTLE, rnd().nextInt(8, 17)),
                    Raro.FRAGMENTO_DE_FORJA.criar(rnd().nextInt(2, 4))));
            if (rnd().nextDouble() < 0.3) premio.add(Raro.PEDRA_DE_PROTECAO.criar());
            if (rnd().nextDouble() < 0.05) premio.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            for (ItemStack i : premio) w.dropItemNaturally(topo, i);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, topo, 120, 1, 1.5, 1, 0.5);
            w.playSound(topo, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);
            List<String> nomes = new ArrayList<>();
            for (Player p : online()) {
                nomes.add(p.getName());
                if (valido(p)) plugin.stats().darXp(p, Skill.COMBATE, 400);
                plugin.titulos().registrar(p, "ondas", 1);
                p.showTitle(Title.title(Component.text("⚔ VITÓRIA! ⚔", NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.text("O tesouro caiu em cima do altar", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))));
            }
            plugin.getServer().broadcast(Component.text("⚔ ", NamedTextColor.RED)
                    .append(Component.text(String.join(", ", nomes), NamedTextColor.YELLOW))
                    .append(Component.text(" venceram as 5 Ondas de Monstros!", NamedTextColor.GRAY)));
        }
    }

    private static List<Player> perto(Location l, double raio) {
        List<Player> lista = new ArrayList<>();
        for (Player p : l.getWorld().getPlayers()) {
            if (valido(p) && p.getLocation().distanceSquared(l) <= raio * raio) lista.add(p);
        }
        return lista;
    }

    /** Desliga tudo (servidor desligando). */
    public void encerrarTudo() {
        for (Arena a : arenas.values()) a.terminar(false);
        arenas.clear();
        for (Map.Entry<UUID, BossBar> en : luas.entrySet()) {
            for (UUID id : luaVeem.getOrDefault(en.getKey(), Set.of())) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.hideBossBar(en.getValue());
            }
        }
        luas.clear();
        luaVeem.clear();
    }
}
