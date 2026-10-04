package br.rpgatributos.fe;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.party.Party;
import br.rpgatributos.reino.Cargo;
import br.rpgatributos.reino.Reino;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fé: cada jogador segue um deus por vez. Oferendas (jogadas com Q num Santuário dos
 * Deuses), uma oração por dia e usar os atributos do deus dão devoção. A devoção sobe o
 * nível de fé (1 a 5), que fortalece o passivo; do nível 2 em diante há um milagre a cada
 * 20 minutos. O Rei pode tornar um deus o deus oficial do reino (+25% de devoção para quem
 * segue ele).
 */
public final class Deuses implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFE082);
    private static final long MILAGRE_MS = 20 * 60 * 1000L;
    private static final int S_STATUS = 4, S_OFERENDAS = 29, S_REZAR = 31, S_MILAGRE = 33, S_REINO = 40, S_FECHAR = 49;
    private static final int[] S_DEUSES = {10, 11, 12, 14, 15, 16};

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location santuario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kDeus, kMilagre, kOracao, kSegunda;
    private final Map<Deus, NamespacedKey> kDevocao = new java.util.EnumMap<>(Deus.class);
    private final Map<Attribute, NamespacedKey> kMods = new HashMap<>();
    /** Oferendas jogadas: item → quem jogou e até quando conta. */
    private final Map<UUID, Object[]> oferendas = new HashMap<>();
    private final Map<UUID, Long> manaArcana = new HashMap<>();
    private final Map<UUID, Long> feridoPor = new HashMap<>();
    private final Map<String, Deus> deusDoReino = new HashMap<>();
    private final File arquivo;
    private int ciclo;

    public Deuses(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kDeus = new NamespacedKey(plugin, "deus");
        this.kMilagre = new NamespacedKey(plugin, "milagre_ate");
        this.kOracao = new NamespacedKey(plugin, "oracao_dia");
        this.kSegunda = new NamespacedKey(plugin, "segunda_chance_ate");
        for (Deus d : Deus.values()) kDevocao.put(d, new NamespacedKey(plugin, "devocao_" + d.id()));
        for (Attribute a : List.of(Attribute.ARMOR, Attribute.BLOCK_BREAK_SPEED, Attribute.MAX_HEALTH, Attribute.OXYGEN_BONUS,
                Attribute.WATER_MOVEMENT_EFFICIENCY, Attribute.ATTACK_DAMAGE)) {
            kMods.put(a, new NamespacedKey(plugin, "deus_" + a.getKey().getKey()));
        }
        this.arquivo = new File(plugin.getDataFolder(), "deuses.yml");
    }

    private static long agora() { return System.currentTimeMillis(); }

    // =====================================================================
    //  Dados
    // =====================================================================

    public void carregar() {
        deusDoReino.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        var sec = y.getConfigurationSection("reinos");
        if (sec == null) return;
        for (String reino : sec.getKeys(false)) {
            Deus d = Deus.porId(sec.getString(reino, ""));
            if (d != null) deusDoReino.put(reino, d);
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Deus> en : deusDoReino.entrySet()) y.set("reinos." + en.getKey(), en.getValue().id());
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar deuses.yml: " + ex.getMessage());
        }
    }

    public Deus deus(Player p) {
        return Deus.porId(p.getPersistentDataContainer().getOrDefault(kDeus, PersistentDataType.STRING, ""));
    }

    public double devocao(Player p, Deus d) {
        return p.getPersistentDataContainer().getOrDefault(kDevocao.get(d), PersistentDataType.DOUBLE, 0.0);
    }

    /** Nível de fé no deus que segue (0 = nenhum deus). */
    public int nivel(Player p) {
        Deus d = deus(p);
        return d == null ? 0 : Deus.nivel(devocao(p, d));
    }

    private Deus oficial(Player p) {
        Reino r = plugin.reinos().de(p);
        return r == null ? null : deusDoReino.get(r.nome());
    }

    /** Soma devoção ao deus que o jogador segue (com os bônus do reino e da constelação). */
    public void darDevocao(Player p, double qtd) {
        Deus d = deus(p);
        if (d == null || qtd <= 0) return;
        if (oficial(p) == d) qtd *= 1.25;
        qtd *= plugin.astronomia().multiplicadorDevocao(p);
        int antes = Deus.nivel(devocao(p, d));
        p.getPersistentDataContainer().set(kDevocao.get(d), PersistentDataType.DOUBLE, devocao(p, d) + qtd);
        int depois = Deus.nivel(devocao(p, d));
        if (depois > antes) {
            p.showTitle(Title.title(Component.text(d.simbolo() + " Fé " + depois + " " + d.simbolo(), d.cor(), TextDecoration.BOLD),
                    Component.text(d.nome() + " ouviu suas preces", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2200), Duration.ofMillis(600))));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
            if (depois == 2) p.sendMessage(Component.text(d.simbolo() + " Milagre liberado: " + d.milagre() + " (/deus milagre)", d.cor()));
            aplicar(p);
            plugin.titulos().definirMinimo(p, "devocao_maxima", depois);
        }
    }

    /** XP de atributo também dá devoção, se o atributo for do deus. */
    public void aoGanharXp(Player p, Skill s, double qtd) {
        Deus d = deus(p);
        if (d != null && d.atributos().contains(s)) darDevocao(p, qtd * 0.02);
    }

    /** Começa a seguir um deus: a devoção no deus antigo cai pela metade. */
    public void seguir(Player p, Deus d) {
        Deus antigo = deus(p);
        if (antigo == d) return;
        if (antigo != null) p.getPersistentDataContainer().set(kDevocao.get(antigo), PersistentDataType.DOUBLE, devocao(p, antigo) / 2);
        p.getPersistentDataContainer().set(kDeus, PersistentDataType.STRING, d.id());
        aplicar(p);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.05);
        p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1.2f);
        p.showTitle(Title.title(Component.text(d.simbolo() + " " + d.nome() + " " + d.simbolo(), d.cor(), TextDecoration.BOLD),
                Component.text("Você agora segue " + d.titulo(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.titulos().definirMinimo(p, "devocao_maxima", nivel(p));
    }

    // =====================================================================
    //  Passivos
    // =====================================================================

    public void aplicar(Player p) {
        Deus d = deus(p);
        int n = d == null ? 0 : Deus.nivel(devocao(p, d));
        definir(p, Attribute.ARMOR, d == Deus.FERRUM ? 0.5 * n : 0);
        definir(p, Attribute.BLOCK_BREAK_SPEED, d == Deus.FERRUM ? 0.05 * n : 0);
        definir(p, Attribute.MAX_HEALTH, d == Deus.SYLVA ? n : d == Deus.BELLUM && n >= 5 ? 2 : 0);
        definir(p, Attribute.OXYGEN_BONUS, d == Deus.MARIS ? n : 0);
        definir(p, Attribute.WATER_MOVEMENT_EFFICIENCY, d == Deus.MARIS ? 0.08 * n : 0);
        definir(p, Attribute.ATTACK_DAMAGE, d == Deus.BELLUM ? 0.4 * n : 0);
    }

    private void definir(Player p, Attribute a, double valor) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        NamespacedKey k = kMods.get(a);
        AttributeModifier atual = inst.getModifier(k);
        if (atual != null) {
            if (Math.abs(atual.getAmount() - valor) < 1e-6) return;
            inst.removeModifier(k);
        }
        if (valor != 0) inst.addModifier(new AttributeModifier(k, valor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    public double bonusMana(Player p) {
        return deus(p) == Deus.ARCANUS ? 10 * nivel(p) : 0;
    }

    public double bonusRegen(Player p) {
        double r = deus(p) == Deus.ARCANUS ? 0.3 * nivel(p) : 0;
        Long ate = manaArcana.get(p.getUniqueId());
        if (ate != null && ate > agora()) r += 5;
        return r;
    }

    /** A cada segundo: cura da Sylva, golfinho da Maris e as oferendas jogadas. */
    public void tick() {
        ciclo++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Deus d = deus(p);
            if (d == null) continue;
            int n = nivel(p);
            if (d == Deus.SYLVA && ciclo % 10 == 0 && !p.isDead()) {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && p.getHealth() < max.getValue()) p.setHealth(Math.min(max.getValue(), p.getHealth() + 0.5 * n));
            }
            if (d == Deus.MARIS && n >= 3 && p.isInWater()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, false, false, true));
            }
        }
        verificarOferendas();
    }

    // =====================================================================
    //  Oferendas (Q em cima do Santuário)
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoJogar(PlayerDropItemEvent e) {
        oferendas.put(e.getItemDrop().getUniqueId(), new Object[]{e.getPlayer().getUniqueId(), agora() + 20_000});
    }

    private void verificarOferendas() {
        if (oferendas.isEmpty()) return;
        long t = agora();
        for (Iterator<Map.Entry<UUID, Object[]>> it = oferendas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Object[]> en = it.next();
            if (!(Bukkit.getEntity(en.getKey()) instanceof Item item) || !item.isValid() || (long) en.getValue()[1] < t) {
                it.remove();
                continue;
            }
            Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
            if (!plugin.santuariosDivinos().eh(abaixo)) continue;
            Player p = Bukkit.getPlayer((UUID) en.getValue()[0]);
            it.remove();
            if (p == null) continue;
            Deus d = deus(p);
            if (d == null) {
                p.sendActionBar(Component.text("Escolha um deus no Santuário antes de fazer oferendas.", NamedTextColor.RED));
                continue;
            }
            ItemStack s = item.getItemStack();
            int valor = d.valor(s.getType());
            if (valor <= 0 || s.hasItemMeta() && s.getItemMeta().hasItemName()) {
                p.sendActionBar(Component.text(d.nome() + " não aceita isso.", NamedTextColor.GRAY));
                continue;
            }
            Location c = abaixo.getLocation().add(0.5, 1.2, 0.5);
            item.remove();
            darDevocao(p, (double) valor * s.getAmount());
            c.getWorld().spawnParticle(Particle.END_ROD, c, 15 + Math.min(40, s.getAmount()), 0.3, 0.4, 0.3, 0.05);
            c.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            p.sendActionBar(Component.text(d.simbolo() + " " + d.nome() + " aceitou a oferenda (+" + (valor * s.getAmount()) + " devoção)", d.cor()));
        }
    }

    // =====================================================================
    //  Oração e milagre
    // =====================================================================

    private static long hoje() {
        return LocalDate.now().toEpochDay();
    }

    public String rezar(Player p) {
        Deus d = deus(p);
        if (d == null) return "Você ainda não segue nenhum deus.";
        long dia = p.getPersistentDataContainer().getOrDefault(kOracao, PersistentDataType.LONG, -1L);
        if (dia == hoje()) return "Você já rezou hoje. Volte amanhã.";
        p.getPersistentDataContainer().set(kOracao, PersistentDataType.LONG, hoje());
        darDevocao(p, 15);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 0));
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 60, 0.5, 0.8, 0.5, 0.5);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.6f);
        p.sendMessage(Component.text(d.simbolo() + " Você reza para " + d.nome() + ". (+15 devoção, Regeneração por 30 s)", d.cor()));
        return null;
    }

    public long recargaMilagre(Player p) {
        return Math.max(0, p.getPersistentDataContainer().getOrDefault(kMilagre, PersistentDataType.LONG, 0L) - agora());
    }

    public String milagre(Player p) {
        Deus d = deus(p);
        if (d == null) return "Você ainda não segue nenhum deus.";
        if (nivel(p) < 2) return "O milagre libera com fé 2 (" + Deus.DEVOCAO[1] + " de devoção).";
        long falta = recargaMilagre(p);
        if (falta > 0) return d.nome() + " ainda não vai te atender: falta " + (falta / 60_000 + 1) + " min.";
        p.getPersistentDataContainer().set(kMilagre, PersistentDataType.LONG, agora() + MILAGRE_MS);
        World w = p.getWorld();
        Location l = p.getLocation();
        switch (d) {
            case FERRUM -> {
                List<ItemStack> itens = new ArrayList<>(List.of(p.getInventory().getArmorContents()));
                itens.add(p.getInventory().getItemInMainHand());
                itens.add(p.getInventory().getItemInOffHand());
                for (ItemStack i : itens) {
                    if (i != null && !i.isEmpty()) i.editMeta(Damageable.class, m -> m.setDamage(0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 1));
                w.spawnParticle(Particle.LAVA, l.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0);
                w.playSound(l, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
            }
            case SYLVA -> {
                int r = 12;
                for (int x = -r; x <= r; x++) for (int y = -3; y <= 3; y++) for (int z = -r; z <= r; z++) {
                    Block b = l.getBlock().getRelative(x, y, z);
                    if (b.getBlockData() instanceof Ageable a && a.getAge() < a.getMaximumAge()) {
                        a.setAge(a.getMaximumAge());
                        b.setBlockData(a);
                    }
                }
                for (Player a : aliados(p, 16)) curar(a, 10);
                w.spawnParticle(Particle.HAPPY_VILLAGER, l, 150, r / 2.0, 1, r / 2.0, 0);
                w.playSound(l, Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.5f, 0.8f);
            }
            case MARIS -> {
                w.setStorm(true);
                w.setWeatherDuration(6000);
                p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 6000, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 6000, 0));
                w.spawnParticle(Particle.SPLASH, l.clone().add(0, 1, 0), 120, 1, 1, 1, 0.2);
                w.playSound(l, Sound.BLOCK_CONDUIT_ACTIVATE, 1f, 1f);
            }
            case BELLUM -> {
                for (Player a : aliados(p, 16)) {
                    a.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1));
                    a.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 0));
                    a.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, a.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
                }
                w.playSound(l, Sound.EVENT_RAID_HORN, 1f, 1.2f);
            }
            case ARCANUS -> {
                plugin.arcano().encherMana(p);
                manaArcana.put(p.getUniqueId(), agora() + 60_000);
                w.spawnParticle(Particle.WITCH, l.clone().add(0, 1, 0), 80, 0.6, 1, 0.6, 0.1);
                w.playSound(l, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.2f);
            }
            case MORTIS -> {
                p.getPersistentDataContainer().set(kSegunda, PersistentDataType.LONG, agora() + 10 * 60_000L);
                w.spawnParticle(Particle.SOUL, l.clone().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.05);
                w.playSound(l, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.6f);
            }
        }
        p.sendMessage(Component.text(d.simbolo() + " " + d.milagre() + "! " + d.descricaoMilagre(), d.cor()));
        plugin.titulos().registrar(p, "milagres", 1);
        return null;
    }

    private List<Player> aliados(Player p, double raio) {
        List<Player> l = new ArrayList<>();
        l.add(p);
        Party pt = plugin.parties().party(p);
        if (pt != null) for (Player o : pt.online()) {
            if (o != p && o.getWorld().equals(p.getWorld()) && o.getLocation().distanceSquared(p.getLocation()) < raio * raio) l.add(o);
        }
        return l;
    }

    private static void curar(LivingEntity e, double qtd) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null && !e.isDead()) e.setHealth(Math.min(max.getValue(), e.getHealth() + qtd));
    }

    // =====================================================================
    //  Eventos de combate (Mortis)
    // =====================================================================

    private static boolean mortoVivo(Entity e) {
        return e instanceof org.bukkit.entity.Zombie || e instanceof org.bukkit.entity.AbstractSkeleton || e instanceof org.bukkit.entity.Phantom
                || e instanceof org.bukkit.entity.Wither || e instanceof org.bukkit.entity.Zoglin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Entity fonte = e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : e.getDamager();
        if (fonte instanceof Player p) {
            feridoPor.put(e.getEntity().getUniqueId(), agora());
            if (deus(p) == Deus.MORTIS && e.getDamager() == p) curar(p, e.getFinalDamage() * 0.02 * nivel(p));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p) || !mortoVivo(e.getEntity()) || deus(p) != Deus.MORTIS || nivel(p) < 5) return;
        Long ferido = feridoPor.get(e.getEntity().getUniqueId());
        if (ferido == null || agora() - ferido > 10_000) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoMorrerQuase(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.getHealth() - e.getFinalDamage() > 0) return;
        long ate = p.getPersistentDataContainer().getOrDefault(kSegunda, PersistentDataType.LONG, 0L);
        if (ate < agora()) return;
        p.getPersistentDataContainer().remove(kSegunda);
        e.setCancelled(true);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 10 : max.getValue() / 2);
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.08);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 0.6f);
        p.showTitle(Title.title(Component.text("☠ Segunda Chance ☠", Deus.MORTIS.cor(), TextDecoration.BOLD),
                Component.text("Mortis ainda não quer você", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2000), Duration.ofMillis(500))));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || deus(p) != Deus.MORTIS) return;
        if (br.rpgatributos.aventura.Chefes.ehChefe(e.getEntity())) darDevocao(p, 25);
        else if (mortoVivo(e.getEntity())) darDevocao(p, 2);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        aplicar(e.getPlayer());
    }

    // =====================================================================
    //  Menu e /deus
    // =====================================================================

    public void abrirMenu(Player p, Location santuario) {
        Tela t = new Tela();
        t.santuario = santuario;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✧ Santuário dos Deuses"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Deus meu = deus(p);
        int n = nivel(p);
        List<Component> status = new ArrayList<>();
        if (meu == null) {
            status.add(Component.text("Você não segue nenhum deus.", NamedTextColor.GRAY));
            status.add(Component.text("Escolha um abaixo (num Santuário).", NamedTextColor.YELLOW));
        } else {
            double dev = devocao(p, meu);
            status.add(Component.text("Segue " + meu.nome() + ", " + meu.titulo(), meu.cor()));
            status.add(Component.text("Fé " + n + "/5 · devoção " + (int) dev + (n < 5 ? " / " + Deus.DEVOCAO[n] : ""), NamedTextColor.WHITE));
            status.add(Component.text("Passivo: " + meu.passivo(), NamedTextColor.GRAY));
            Deus of = oficial(p);
            if (of != null) status.add(Component.text("Deus do seu reino: " + of.nome() + (of == meu ? " (+25% de devoção)" : ""), NamedTextColor.GOLD));
        }
        inv.setItem(S_STATUS, item(meu == null ? Material.CANDLE : meu.icone(), Component.text("✧ Sua fé", COR, TextDecoration.BOLD), status));
        Deus[] todos = Deus.values();
        for (int i = 0; i < todos.length; i++) {
            Deus d = todos[i];
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(d.titulo(), NamedTextColor.GRAY));
            lore.add(Component.text("Passivo: " + d.passivo(), NamedTextColor.WHITE));
            lore.add(Component.text("Milagre: " + d.milagre() + " — " + d.descricaoMilagre(), NamedTextColor.AQUA));
            List<String> attrs = new ArrayList<>();
            for (Skill s : d.atributos()) attrs.add(s.nome());
            lore.add(Component.text("Devoção também por: " + (attrs.isEmpty() ? "derrotar mortos-vivos" : String.join(", ", attrs)), NamedTextColor.DARK_GRAY));
            double dev = devocao(p, d);
            if (dev > 0) lore.add(Component.text("Sua devoção: " + (int) dev, NamedTextColor.GRAY));
            lore.add(Component.empty());
            if (d == meu) lore.add(Component.text("● Você segue este deus", NamedTextColor.GREEN));
            else if (t.santuario == null) lore.add(Component.text("Escolha num Santuário dos Deuses.", NamedTextColor.DARK_GRAY));
            else lore.add(Component.text(meu == null ? "» Clique para seguir" : "» Shift + clique para trocar (a devoção atual cai pela metade)",
                    NamedTextColor.YELLOW));
            ItemStack it = item(d.icone(), Component.text(d.simbolo() + " " + d.nome(), d.cor(), TextDecoration.BOLD), lore);
            if (d == meu) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_DEUSES[i], it);
        }
        if (meu != null) {
            List<Component> of = new ArrayList<>();
            of.add(Component.text("Jogue (Q) em cima do Santuário:", NamedTextColor.GRAY));
            meu.oferendas().entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .forEach(en -> of.add(Component.text(" • " + nomeMaterial(en.getKey()) + ": " + en.getValue(), NamedTextColor.WHITE)));
            inv.setItem(S_OFERENDAS, item(Material.CHEST, Component.text("Oferendas que " + meu.nome() + " aceita", meu.cor(), TextDecoration.BOLD), of));
            boolean rezou = p.getPersistentDataContainer().getOrDefault(kOracao, PersistentDataType.LONG, -1L) == hoje();
            inv.setItem(S_REZAR, item(rezou ? Material.GRAY_DYE : Material.CANDLE, Component.text("Rezar", COR, TextDecoration.BOLD), List.of(
                    Component.text("Uma vez por dia: +15 devoção e Regeneração.", NamedTextColor.GRAY),
                    rezou ? Component.text("Você já rezou hoje.", NamedTextColor.DARK_GRAY)
                            : t.santuario == null ? Component.text("Reze num Santuário dos Deuses.", NamedTextColor.DARK_GRAY)
                            : Component.text("» Clique para rezar", NamedTextColor.YELLOW))));
            long falta = recargaMilagre(p);
            inv.setItem(S_MILAGRE, item(n >= 2 && falta == 0 ? Material.NETHER_STAR : Material.GRAY_DYE,
                    Component.text("Milagre: " + meu.milagre(), meu.cor(), TextDecoration.BOLD), List.of(
                            Component.text(meu.descricaoMilagre(), NamedTextColor.GRAY),
                            Component.text("A cada 20 minutos, a partir da fé 2.", NamedTextColor.DARK_GRAY),
                            n < 2 ? Component.text("✖ Precisa de fé 2", NamedTextColor.RED)
                                    : falta > 0 ? Component.text("Pronto em " + (falta / 60_000 + 1) + " min", NamedTextColor.RED)
                                    : Component.text("» Clique para pedir (ou /deus milagre)", NamedTextColor.YELLOW))));
        }
        Reino r = plugin.reinos().de(p);
        if (r != null) {
            Deus of = deusDoReino.get(r.nome());
            boolean rei = r.cargo(p.getUniqueId()) == Cargo.REI;
            inv.setItem(S_REINO, item(Material.GOLDEN_HELMET, Component.text("Deus do reino " + r.nome(), NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    Component.text(of == null ? "Nenhum deus oficial." : "Deus oficial: " + of.nome(), NamedTextColor.WHITE),
                    Component.text("Quem segue o deus oficial ganha +25% de devoção.", NamedTextColor.GRAY),
                    rei && meu != null ? Component.text("» Clique para tornar " + meu.nome() + " o deus oficial", NamedTextColor.YELLOW)
                            : Component.text("Só o Rei escolhe (o deus que ele segue).", NamedTextColor.DARK_GRAY))));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static String nomeMaterial(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        boolean noSantuario = t.santuario != null && plugin.santuariosDivinos().eh(t.santuario.getBlock());
        String erro = null;
        if (slot == S_REZAR) erro = noSantuario ? rezar(p) : "Reze num Santuário dos Deuses.";
        else if (slot == S_MILAGRE) erro = milagre(p);
        else if (slot == S_REINO) {
            Reino r = plugin.reinos().de(p);
            Deus meu = deus(p);
            if (r == null || meu == null) return;
            if (r.cargo(p.getUniqueId()) != Cargo.REI) erro = "Só o Rei escolhe o deus do reino.";
            else {
                deusDoReino.put(r.nome(), meu);
                salvar();
                plugin.getServer().broadcast(Component.text("✧ " + meu.nome() + " agora é o deus oficial do reino " + r.nome() + "!", meu.cor()));
            }
        } else {
            Deus[] todos = Deus.values();
            for (int i = 0; i < todos.length; i++) {
                if (S_DEUSES[i] != slot) continue;
                if (!noSantuario) erro = "Escolha um deus num Santuário dos Deuses.";
                else if (deus(p) == todos[i]) return;
                else if (deus(p) != null && !e.isShiftClick()) erro = "Shift + clique para trocar de deus (a devoção atual cai pela metade).";
                else seguir(p, todos[i]);
            }
        }
        if (erro != null) {
            p.sendMessage(Component.text(erro, NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        }
        desenhar(p, t);
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("milagre")) {
            String erro = milagre(p);
            if (erro != null) p.sendMessage(Component.text(erro, NamedTextColor.RED));
            return true;
        }
        abrirMenu(p, null);
        return true;
    }
}
