package br.rpgatributos.oculto;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.lenda.Lenda;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Switch;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Locais Ocultos: o plugin escolhe lugares pelo mundo e constrói cada um quando o chunk
 * carrega pela primeira vez. Sussurros perto, enigma das alavancas, guardião e o selo
 * (relíquias e a classe lendária). Mapas Rasgados apontam o caminho.
 */
public final class Locais implements Listener {

    private static final NamespacedKey K_DESCOBERTOS = new NamespacedKey("rpgatributos", "locais_descobertos");
    private static final NamespacedKey K_SAQUEADOS = new NamespacedKey("rpgatributos", "locais_saqueados");
    private static final NamespacedKey K_GUARDIAO = new NamespacedKey("rpgatributos", "guardiao_local");

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<String, Local> locais = new LinkedHashMap<>();
    private final Map<UUID, Long> sussurros = new HashMap<>();
    private final Map<UUID, Local> guardioes = new HashMap<>();

    public Locais(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "locais.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Planejar e construir
    // =====================================================================

    public void iniciar() {
        carregar();
        if (locais.isEmpty()) planejar();
        for (Local l : locais.values()) if (!l.construido) talvezConstruir(l);
    }

    /** Escolhe os lugares (longe do spawn, longe uns dos outros, fora do mar). */
    private void planejar() {
        World w = Bukkit.getWorlds().getFirst();
        Location spawn = w.getSpawnLocation();
        int porTipo = plugin.settings().locPorTipo;
        double min = plugin.settings().locDistMin, max = plugin.settings().locDistMax;
        for (TipoLocal t : TipoLocal.values()) {
            for (int n = 1; n <= porTipo; n++) {
                for (int tentativa = 0; tentativa < 60; tentativa++) {
                    double ang = rnd().nextDouble() * Math.PI * 2, d = rnd().nextDouble(min, max);
                    int x = (int) (spawn.getX() + Math.cos(ang) * d), z = (int) (spawn.getZ() + Math.sin(ang) * d);
                    if (perto(w.getName(), x, z, 400)) continue;
                    String bioma = w.getBiome(x, 64, z).getKey().getKey();
                    if (bioma.contains("ocean") || bioma.contains("river")) continue;
                    Local l = new Local(t.id() + "_" + n, t, w.getName(), x, z);
                    locais.put(l.id, l);
                    break;
                }
            }
        }
        salvar();
        plugin.getLogger().info(locais.size() + " Locais Ocultos foram escolhidos pelo mundo.");
    }

    private boolean perto(String mundo, int x, int z, double dist) {
        for (Local l : locais.values()) {
            if (l.mundo.equals(mundo) && (double) (l.x - x) * (l.x - x) + (double) (l.z - z) * (l.z - z) < dist * dist) return true;
        }
        return false;
    }

    private void talvezConstruir(Local l) {
        World w = l.world();
        if (w == null || l.construido || l.agendado || !w.isChunkLoaded(l.x >> 4, l.z >> 4)) return;
        l.agendado = true;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Construtor.construir(l);
            salvar();
        });
    }

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        for (Local l : locais.values()) {
            if (!l.construido && l.mundo.equals(e.getWorld().getName()) && l.x >> 4 == e.getChunk().getX() && l.z >> 4 == e.getChunk().getZ()) {
                talvezConstruir(l);
            }
        }
    }

    /** Admin: cria um local novo aqui mesmo. */
    public Local criarAqui(TipoLocal t, Location onde) {
        int n = 1;
        while (locais.containsKey(t.id() + "_" + n)) n++;
        Local l = new Local(t.id() + "_" + n, t, onde.getWorld().getName(), onde.getBlockX() + 8, onde.getBlockZ());
        locais.put(l.id, l);
        Construtor.construir(l);
        salvar();
        return l;
    }

    public List<String> listar() {
        List<String> s = new ArrayList<>();
        for (Local l : locais.values()) {
            s.add(l.tipo.nome() + " (" + l.id + "): " + l.x + " " + (l.construido ? l.y : "?") + " " + l.z + (l.construido ? "" : " — ainda não construído"));
        }
        return s;
    }

    // =====================================================================
    //  Descoberta
    // =====================================================================

    private static List<String> lista(Player p, NamespacedKey k) {
        String s = p.getPersistentDataContainer().getOrDefault(k, PersistentDataType.STRING, "");
        return s.isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(s.split(",")));
    }

    private static void adicionar(Player p, NamespacedKey k, String id) {
        List<String> l = lista(p, k);
        if (l.contains(id)) return;
        l.add(id);
        p.getPersistentDataContainer().set(k, PersistentDataType.STRING, String.join(",", l));
    }

    public boolean descobriu(Player p, Local l) {
        return lista(p, K_DESCOBERTOS).contains(l.id);
    }

    /** Já achou algum local desse tipo? (mostra a classe lendária no Santuário) */
    public boolean descobriuTipo(Player p, TipoLocal t) {
        for (String id : lista(p, K_DESCOBERTOS)) {
            Local l = locais.get(id);
            if (l != null && l.tipo == t) return true;
        }
        return false;
    }

    /** Um Local Oculto que pode aparecer num sonho. */
    public record PistaSonho(String id, TipoLocal tipo, int x, int z) { }

    /** O Local Oculto mais perto que o jogador ainda não achou nem sonhou. */
    public PistaSonho paraSonhar(Player p, Location daqui, java.util.Set<String> sonhados) {
        List<String> achados = lista(p, K_DESCOBERTOS);
        Local melhor = null;
        double best = Double.MAX_VALUE;
        for (Local l : locais.values()) {
            if (!l.mundo.equals(daqui.getWorld().getName()) || achados.contains(l.id) || sonhados.contains(l.id)) continue;
            double d2 = (l.x - daqui.getX()) * (l.x - daqui.getX()) + (l.z - daqui.getZ()) * (l.z - daqui.getZ());
            if (d2 < best) {
                best = d2;
                melhor = l;
            }
        }
        return melhor == null ? null : new PistaSonho(melhor.id, melhor.tipo, melhor.x, melhor.z);
    }

    /** Locais que o jogador já achou (para o /locais). */
    public List<String> descobertos(Player p) {
        List<String> s = new ArrayList<>();
        for (String id : lista(p, K_DESCOBERTOS)) {
            Local l = locais.get(id);
            if (l != null) s.add(l.tipo.nome() + " — " + l.x + " " + l.y + " " + l.z + " (" + l.mundo + ")"
                    + (lista(p, K_SAQUEADOS).contains(id) ? " ✔ selo tocado" : ""));
        }
        return s;
    }

    public int total() { return locais.size(); }

    private void descobrir(Player p, Local l) {
        adicionar(p, K_DESCOBERTOS, l.id);
        plugin.titulos().registrar(p, "locais", 1);
        p.showTitle(Title.title(Component.text("✦ " + l.tipo.nome() + " ✦", l.tipo.cor(), TextDecoration.BOLD),
                Component.text("Você encontrou um Local Oculto", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3500), Duration.ofMillis(900))));
        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.8f);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.6f);
        p.sendMessage(Component.text("✦ Desça pelo poço. O livro no púlpito guarda o enigma.", l.tipo.cor()));
    }

    // =====================================================================
    //  A cada segundo
    // =====================================================================

    public void tick() {
        long agora = System.currentTimeMillis();
        for (Local l : locais.values()) {
            if (!l.construido) continue;
            World w = l.world();
            if (w == null) continue;
            // Selo vencido que já passou da hora: tudo volta ao começo.
            if (l.aberto && l.vencidoAte > 0 && !l.vencido()) reiniciar(l);
            Location boca = new Location(w, l.x + 0.5, l.y, l.z + 0.5);
            List<Player> noSalao = new ArrayList<>();
            for (Player p : w.getPlayers()) {
                double d2 = p.getLocation().distanceSquared(boca);
                if (d2 > 64 * 64 && !l.dentro(p.getLocation())) continue;
                if (!descobriu(p, l)) {
                    if (d2 < 14 * 14 || l.dentro(p.getLocation())) descobrir(p, l);
                    else if (agora - sussurros.getOrDefault(p.getUniqueId(), 0L) > 30_000) {
                        sussurros.put(p.getUniqueId(), agora);
                        p.sendActionBar(Component.text("Você ouve sussurros por perto...", NamedTextColor.DARK_PURPLE, TextDecoration.ITALIC));
                        p.playSound(p.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.8f, 1.2f);
                    }
                }
                if (l.noSalao(p.getLocation())) noSalao.add(p);
            }
            guardiao(l, noSalao);
            if (l.vencido()) w.spawnParticle(Particle.END_ROD, l.selo().add(0.5, 1.2, 0.5), 3, 0.2, 0.4, 0.2, 0.01);
        }
    }

    // =====================================================================
    //  Enigma: alavancas na ordem certa
    // =====================================================================

    private Local daAlavanca(Block b) {
        for (Local l : locais.values()) {
            if (!l.construido || !l.mundo.equals(b.getWorld().getName())) continue;
            for (int i = 0; i < 4; i++) if (l.alavanca(i).getBlock().equals(b)) return l;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoInteragir(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (usarPista(p, e.getItem())) {
                e.setCancelled(true);
                return;
            }
        }
        Block b = e.getClickedBlock();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || b == null) return;
        if (b.getType() == Material.BEACON) {
            for (Local l : locais.values()) {
                if (l.construido && l.selo().getBlock().equals(b)) {
                    e.setCancelled(true);
                    tocarSelo(p, l);
                    return;
                }
            }
            return;
        }
        if (b.getType() != Material.LEVER) return;
        Local l = daAlavanca(b);
        if (l == null) return;
        if (l.aberto || !(b.getBlockData() instanceof Switch s) || s.isPowered()) {
            e.setCancelled(true); // já resolvido, ou alavanca já ligada: não desliga
            return;
        }
        int indice = -1;
        for (int i = 0; i < 4; i++) if (l.alavanca(i).getBlock().equals(b)) indice = i;
        if (indice == l.ordem[l.progresso]) {
            l.progresso++;
            p.playSound(b.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f + l.progresso * 0.2f);
            if (l.progresso >= 4) {
                l.aberto = true;
                Construtor.abrirPortao(l);
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.5f, 0.5f);
                p.showTitle(Title.title(Component.empty(), Component.text("O portão se abriu...", l.tipo.cor()),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
            }
        } else {
            // Errou: as alavancas voltam e os guardiões menores acordam.
            l.progresso = 0;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> Construtor.desligarAlavancas(l), 10L);
            p.sendActionBar(Component.text("✖ Ordem errada! Os guardiões acordaram.", NamedTextColor.RED));
            b.getWorld().playSound(b.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);
            for (int i = 0; i < 3; i++) {
                Location onde = new Location(b.getWorld(), l.x + rnd().nextInt(-4, 5) + 0.5, l.yAntessala(), l.z + rnd().nextInt(-4, 3) + 0.5);
                if (onde.getBlock().isPassable()) {
                    b.getWorld().spawnEntity(onde, i == 0 ? EntityType.SKELETON : EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.CUSTOM);
                }
            }
        }
    }

    // =====================================================================
    //  Guardião
    // =====================================================================

    private void guardiao(Local l, List<Player> noSalao) {
        Entity g = l.guardiao == null ? null : Bukkit.getEntity(l.guardiao);
        if (g != null && (!g.isValid() || g.isDead())) g = null;
        if (g == null && l.guardiao != null) {
            guardioes.remove(l.guardiao);
            l.guardiao = null;
        }
        if (g == null && l.aberto && !l.vencido() && !noSalao.isEmpty()) g = invocarGuardiao(l, noSalao);
        if (g == null) {
            if (l.barra != null) {
                for (Player p : l.world().getPlayers()) p.hideBossBar(l.barra);
                l.barra = null;
            }
            return;
        }
        LivingEntity guarda = (LivingEntity) g;
        if (!l.noSalao(guarda.getLocation())) guarda.teleport(l.centroSalao());
        if (guarda instanceof Mob m && (m.getTarget() == null || !noSalao.contains(m.getTarget())) && !noSalao.isEmpty()) {
            m.setTarget(noSalao.get(rnd().nextInt(noSalao.size())));
        }
        if (l.barra != null) {
            AttributeInstance max = guarda.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) l.barra.progress((float) Math.max(0, Math.min(1, guarda.getHealth() / max.getValue())));
            for (Player p : l.world().getPlayers()) {
                if (noSalao.contains(p)) p.showBossBar(l.barra);
                else p.hideBossBar(l.barra);
            }
        }
    }

    private LivingEntity invocarGuardiao(Local l, List<Player> jogadores) {
        TipoLocal t = l.tipo;
        double vida = 400 * (1 + 0.5 * (jogadores.size() - 1));
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) t.guardiao().getEntityClass();
        Location onde = l.centroSalao();
        LivingEntity g = onde.getWorld().spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            AttributeInstance v = e.getAttribute(Attribute.MAX_HEALTH);
            if (v != null) v.setBaseValue(vida);
            e.setHealth(vida);
            AttributeInstance dano = e.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue(dano.getBaseValue() * 2.2);
            AttributeInstance kb = e.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.setBaseValue(0.8);
            AttributeInstance arm = e.getAttribute(Attribute.ARMOR);
            if (arm != null) arm.setBaseValue(arm.getBaseValue() + 8);
            e.customName(Component.text("✦ " + t.nomeGuardiao() + " ✦", t.cor(), TextDecoration.BOLD));
            e.setCustomNameVisible(true);
            e.setPersistent(false);
            e.setRemoveWhenFarAway(false);
            e.getPersistentDataContainer().set(K_GUARDIAO, PersistentDataType.STRING, l.id);
            equipar(e, t);
        });
        l.guardiao = g.getUniqueId();
        guardioes.put(g.getUniqueId(), l);
        l.barra = BossBar.bossBar(Component.text("✦ " + t.nomeGuardiao(), t.cor()), 1f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_20);
        onde.getWorld().strikeLightningEffect(onde);
        for (Player p : jogadores) {
            p.showTitle(Title.title(Component.text("✦ " + t.nomeGuardiao() + " ✦", t.cor(), TextDecoration.BOLD),
                    Component.text("O guardião do selo despertou", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(500))));
        }
        return g;
    }

    private static void equipar(LivingEntity e, TipoLocal t) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;
        switch (t) {
            case CRIPTA_DO_REI_CAIDO -> {
                eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                eq.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            }
            case SANTUARIO_DAS_SOMBRAS -> {
                eq.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                e.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
                e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));
            }
            case OBSERVATORIO_CELESTE -> {
                ItemStack arco = new ItemStack(Material.BOW);
                arco.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.POWER, 5);
                eq.setItemInMainHand(arco);
                eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            }
            case CAPELA_PROFANADA -> {
                eq.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                eq.setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
            }
            default -> { }
        }
        eq.setHelmetDropChance(0);
        eq.setChestplateDropChance(0);
        eq.setLeggingsDropChance(0);
        eq.setItemInMainHandDropChance(0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrerGuardiao(EntityDeathEvent e) {
        Local l = guardioes.remove(e.getEntity().getUniqueId());
        if (l == null) return;
        l.guardiao = null;
        l.vencidoAte = System.currentTimeMillis() + plugin.settings().locResetMin * 60_000L;
        e.getDrops().clear();
        e.getDrops().add(Raro.FRAGMENTO_DE_FORJA.criar(2));
        e.setDroppedExp(300);
        if (l.barra != null) {
            for (Player p : l.world().getPlayers()) p.hideBossBar(l.barra);
            l.barra = null;
        }
        World w = l.world();
        w.playSound(l.selo(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 0.6f);
        for (Player p : w.getPlayers()) {
            if (!l.dentro(p.getLocation())) continue;
            p.showTitle(Title.title(Component.text("O selo despertou", l.tipo.cor(), TextDecoration.BOLD),
                    Component.text("Toque nele (clique direito)", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))));
        }
    }

    // =====================================================================
    //  Selo: relíquias e classe lendária
    // =====================================================================

    private void tocarSelo(Player p, Local l) {
        if (!l.vencido()) {
            p.sendActionBar(Component.text("O selo está adormecido. Derrote o guardião deste lugar.", NamedTextColor.GRAY));
            return;
        }
        if (!lista(p, K_SAQUEADOS).contains(l.id)) {
            adicionar(p, K_SAQUEADOS, l.id);
            List<ItemStack> premio = new ArrayList<>();
            premio.add(Raro.PAGINA_DE_LENDA.criar(1));
            premio.add(Raro.MAPA_RASGADO.criar(1));
            premio.add(Raro.FRAGMENTO_DE_FORJA.criar(2 + rnd().nextInt(3)));
            if (rnd().nextDouble() < 0.4) premio.add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
            if (rnd().nextDouble() < 0.25) premio.add(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio());
            premio.add(new ItemStack(Material.DIAMOND, 2 + rnd().nextInt(4)));
            premio.add(new ItemStack(Material.EMERALD, 8 + rnd().nextInt(9)));
            for (ItemStack i : premio) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
            plugin.titulos().registrar(p, "selos", 1);
            p.sendMessage(Component.text("✦ O selo te deu relíquias: uma Página do Livro das Lendas, um Mapa Rasgado e mais.", l.tipo.cor()));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        var classe = l.tipo.classe();
        if (plugin.classes().perfil(p).aprendeu(classe)) return;
        String erro = plugin.classes().reivindicarLendaria(p, classe);
        if (erro != null) {
            p.sendMessage(Component.text("✪ " + classe.nome() + ": " + erro, NamedTextColor.GRAY));
        }
    }

    private void reiniciar(Local l) {
        l.aberto = false;
        l.progresso = 0;
        l.vencidoAte = 0;
        Construtor.fecharPortao(l);
        Construtor.desligarAlavancas(l);
        Block pulpito = l.world().getBlockAt(l.x + 4, l.yAntessala(), l.z + 3);
        if (pulpito.getState(false) instanceof org.bukkit.block.Lectern lec && lec.getInventory().getItem(0) == null) {
            lec.getInventory().setItem(0, Construtor.livro(l));
        }
    }

    // =====================================================================
    //  Pistas: Mapa Rasgado e Página do Livro das Lendas
    // =====================================================================

    private boolean usarPista(Player p, ItemStack item) {
        Raro r = Raro.de(item);
        if (r == Raro.MAPA_RASGADO) {
            Local alvo = null;
            double melhor = Double.MAX_VALUE;
            for (Local l : locais.values()) {
                if (descobriu(p, l) || !l.mundo.equals(p.getWorld().getName())) continue;
                double d2 = Math.pow(l.x - p.getLocation().getX(), 2) + Math.pow(l.z - p.getLocation().getZ(), 2);
                if (d2 < melhor) { melhor = d2; alvo = l; }
            }
            if (alvo == null) {
                p.sendActionBar(Component.text("O mapa não mostra nada que você já não conheça (neste mundo).", NamedTextColor.GRAY));
                return true;
            }
            item.setAmount(item.getAmount() - 1);
            int ax = alvo.x + rnd().nextInt(-60, 61), az = alvo.z + rnd().nextInt(-60, 61);
            ItemStack bussola = new ItemStack(Material.COMPASS);
            Local destino = alvo;
            bussola.editMeta(CompassMeta.class, m -> {
                m.setLodestone(new Location(p.getWorld(), ax, 64, az));
                m.setLodestoneTracked(false);
                m.itemName(Component.text("Bússola Antiga", destino.tipo.cor()));
                m.lore(List.of(Component.text("Aponta para perto de: " + destino.tipo.nome(), NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("(uns 60 blocos de diferença; procure os sussurros)", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
            });
            p.getInventory().addItem(bussola).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
            p.sendMessage(Component.text("✦ O mapa mostra o caminho para um " + alvo.tipo.nome() + " perto de X " + ax + ", Z " + az
                    + ". Você ganhou uma Bússola Antiga.", alvo.tipo.cor()));
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.7f);
            return true;
        }
        if (r == Raro.PAGINA_DE_LENDA) {
            Lenda l = plugin.lendas().revelarAleatoria(p);
            if (l == null) {
                p.sendActionBar(Component.text("Você já conhece os feitos de todas as lendas.", NamedTextColor.GRAY));
                return true;
            }
            item.setAmount(item.getAmount() - 1);
            p.sendMessage(Component.text("✦ A página revela os feitos da lenda ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(l.nome(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.text("! Veja no /lendas.", NamedTextColor.LIGHT_PURPLE)));
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.4f);
            return true;
        }
        return false;
    }

    // =====================================================================
    //  As paredes dos locais não quebram (nada de cavar em volta do enigma)
    // =====================================================================

    private Local em(Location l) {
        for (Local x : locais.values()) if (x.construido && x.dentro(l)) return x;
        return null;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (plugin.territorios().ignorando(e.getPlayer())) return;
        if (em(e.getBlock().getLocation()) != null) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("Uma força antiga protege este lugar.", NamedTextColor.DARK_PURPLE));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> em(b.getLocation()) != null);
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    private void carregar() {
        locais.clear();
        if (!arquivo.exists()) return;
        ConfigurationSection raiz = YamlConfiguration.loadConfiguration(arquivo).getConfigurationSection("locais");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(id);
            TipoLocal t = s == null ? null : TipoLocal.porId(s.getString("tipo", ""));
            if (t == null) continue;
            Local l = new Local(id, t, s.getString("mundo", "world"), s.getInt("x"), s.getInt("z"));
            l.construido = s.getBoolean("construido");
            l.y = s.getInt("y", Integer.MIN_VALUE);
            locais.put(id, l);
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Local l : locais.values()) {
            String b = "locais." + l.id + ".";
            y.set(b + "tipo", l.tipo.id());
            y.set(b + "mundo", l.mundo);
            y.set(b + "x", l.x);
            y.set(b + "z", l.z);
            y.set(b + "construido", l.construido);
            y.set(b + "y", l.y);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar locais.yml: " + ex.getMessage());
        }
    }

    public void parar() {
        for (Local l : locais.values()) {
            Entity g = l.guardiao == null ? null : Bukkit.getEntity(l.guardiao);
            if (g != null) g.remove();
            if (l.barra != null) for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(l.barra);
        }
    }
}
