package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.masmorra.Masmorras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lápide: ao morrer, os itens não ficam espalhados no chão. Eles vão para um túmulo (a cabeça
 * do jogador) onde ele caiu, protegido por 15 minutos: só o dono abre. Depois disso qualquer um
 * pode abrir. Ao renascer, o dono ganha uma bússola que aponta para o túmulo. Masmorras e a
 * Torre continuam com as regras delas (Cofre das Almas).
 */
public final class Lapides implements Listener {

    public static final TextColor COR = TextColor.color(0x90A4AE);

    private static final class Tumulo {
        UUID id, dono;
        String nome;
        Location onde;
        long criado;
        ItemStack[] itens;
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final NamespacedKey kBussola;
    private final Map<UUID, Tumulo> tumulos = new HashMap<>();
    /** Túmulo que acabou de ser criado → dono, para dar a bússola quando ele renascer. */
    private final Map<UUID, UUID> bussolaPendente = new HashMap<>();
    private boolean sujo;

    public Lapides(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "lapides.yml");
        this.kBussola = new NamespacedKey(plugin, "bussola_lapide");
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection s = y.getConfigurationSection("tumulos");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            ConfigurationSection t = s.getConfigurationSection(k);
            World w = t == null ? null : Bukkit.getWorld(t.getString("mundo", ""));
            if (w == null) continue;
            try {
                Tumulo tu = new Tumulo();
                tu.id = UUID.fromString(k);
                tu.dono = UUID.fromString(t.getString("dono", ""));
                tu.nome = t.getString("nome", "?");
                tu.onde = new Location(w, t.getInt("x"), t.getInt("y"), t.getInt("z"));
                tu.criado = t.getLong("criado");
                tu.itens = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(t.getString("itens", "")));
                tumulos.put(tu.id, tu);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Túmulo " + k + " não pôde ser lido: " + ex.getMessage());
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        YamlConfiguration y = new YamlConfiguration();
        for (Tumulo t : tumulos.values()) {
            String b = "tumulos." + t.id + ".";
            y.set(b + "dono", t.dono.toString());
            y.set(b + "nome", t.nome);
            y.set(b + "mundo", t.onde.getWorld().getName());
            y.set(b + "x", t.onde.getBlockX());
            y.set(b + "y", t.onde.getBlockY());
            y.set(b + "z", t.onde.getBlockZ());
            y.set(b + "criado", t.criado);
            y.set(b + "itens", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(t.itens)));
        }
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar lapides.yml: " + ex.getMessage());
        }
    }

    private Tumulo em(Block b) {
        for (Tumulo t : tumulos.values()) {
            if (t.onde.getWorld().equals(b.getWorld()) && t.onde.getBlockX() == b.getX() && t.onde.getBlockY() == b.getY()
                    && t.onde.getBlockZ() == b.getZ()) return t;
        }
        return null;
    }

    private long protecaoMs() {
        return plugin.settings().detLapideMinutos * 60_000L;
    }

    // =====================================================================
    //  Morrer e renascer
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(PlayerDeathEvent e) {
        if (!plugin.settings().detLapide || e.getKeepInventory() || e.getDrops().isEmpty()) return;
        Player p = e.getPlayer();
        if (p.getWorld().getName().equals(Masmorras.MUNDO)) return;
        Block lugar = lugar(p.getLocation());
        if (lugar == null) return; // caiu no vazio ou na lava funda: fica como no jogo
        Tumulo t = new Tumulo();
        t.id = UUID.randomUUID();
        t.dono = p.getUniqueId();
        t.nome = p.getName();
        t.onde = lugar.getLocation();
        t.criado = System.currentTimeMillis();
        t.itens = e.getDrops().toArray(new ItemStack[0]);
        e.getDrops().clear();
        lugar.setType(Material.PLAYER_HEAD);
        if (lugar.getState() instanceof Skull s) {
            s.setOwningPlayer(p);
            s.update();
        }
        tumulos.put(t.id, t);
        bussolaPendente.put(p.getUniqueId(), t.id);
        sujo = true;
        salvar();
        p.sendMessage(Component.text("⚰ Seus itens estão no túmulo em " + lugar.getX() + " " + lugar.getY() + " " + lugar.getZ()
                + " (protegido por " + plugin.settings().detLapideMinutos + " min).", COR));
    }

    /** Um bloco livre onde o túmulo cabe: no lugar da morte ou até 8 blocos acima. */
    private static Block lugar(Location l) {
        World w = l.getWorld();
        if (l.getBlockY() < w.getMinHeight() || l.getBlockY() >= w.getMaxHeight()) return null;
        Block b = l.getBlock();
        for (int i = 0; i < 8 && b.getY() < w.getMaxHeight() - 1; i++, b = b.getRelative(BlockFace.UP)) {
            Material m = b.getType();
            if ((m.isAir() || b.isReplaceable() || m == Material.WATER) && m != Material.LAVA) return b;
        }
        return null;
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        UUID id = bussolaPendente.remove(e.getPlayer().getUniqueId());
        Tumulo t = id == null ? null : tumulos.get(id);
        if (t == null) return;
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || !tumulos.containsKey(t.id)) return;
            ItemStack b = new ItemStack(Material.COMPASS);
            b.editMeta(CompassMeta.class, m -> {
                m.setLodestone(t.onde.clone().add(0.5, 0, 0.5));
                m.setLodestoneTracked(false);
                m.itemName(Component.text("⚰ Bússola do Túmulo", COR));
                m.lore(List.of(Component.text("Aponta para onde você morreu.", NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
                m.getPersistentDataContainer().set(kBussola, PersistentDataType.STRING, t.id.toString());
            });
            p.getInventory().addItem(b);
        });
    }

    // =====================================================================
    //  Abrir, quebrar, proteger
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null
                || e.getClickedBlock().getType() != Material.PLAYER_HEAD) return;
        Tumulo t = em(e.getClickedBlock());
        if (t == null) return;
        e.setCancelled(true);
        abrir(e.getPlayer(), t);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Tumulo t = e.getBlock().getType() == Material.PLAYER_HEAD ? em(e.getBlock()) : null;
        if (t == null) return;
        e.setCancelled(true);
        abrir(e.getPlayer(), t);
    }

    private void abrir(Player p, Tumulo t) {
        long resta = t.criado + protecaoMs() - System.currentTimeMillis();
        if (!p.getUniqueId().equals(t.dono) && resta > 0) {
            p.sendActionBar(Component.text("⚰ Túmulo de " + t.nome + ": protegido por mais " + (resta / 60000 + 1) + " min.", COR));
            return;
        }
        for (ItemStack i : t.itens) {
            if (i == null || i.isEmpty()) continue;
            p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        tumulos.remove(t.id);
        sujo = true;
        salvar();
        Block b = t.onde.getBlock();
        if (b.getType() == Material.PLAYER_HEAD) b.setType(Material.AIR);
        b.getWorld().spawnParticle(Particle.SOUL, b.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.03);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 0.8f);
        p.sendMessage(Component.text("⚰ Você recuperou os itens do túmulo de " + t.nome + ".", COR));
        // As bússolas desse túmulo somem do inventário do dono.
        Player dono = Bukkit.getPlayer(t.dono);
        if (dono != null) {
            for (ItemStack i : dono.getInventory().getContents()) {
                if (i != null && i.hasItemMeta() && t.id.toString().equals(i.getItemMeta().getPersistentDataContainer().get(kBussola, PersistentDataType.STRING))) {
                    i.setAmount(0);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> b.getType() == Material.PLAYER_HEAD && em(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> b.getType() == Material.PLAYER_HEAD && em(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (b.getType() == Material.PLAYER_HEAD && em(b) != null) { e.setCancelled(true); return; }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (b.getType() == Material.PLAYER_HEAD && em(b) != null) { e.setCancelled(true); return; }
    }

    /** A cada 2 s: almas em volta dos túmulos perto de jogadores; refaz a cabeça se ela sumiu. */
    public void tick() {
        List<Tumulo> lista = new ArrayList<>(tumulos.values());
        for (Tumulo t : lista) {
            if (!t.onde.isChunkLoaded()) continue;
            Block b = t.onde.getBlock();
            if (b.getType() != Material.PLAYER_HEAD && (b.getType().isAir() || b.isReplaceable())) b.setType(Material.PLAYER_HEAD);
            boolean perto = false;
            for (Player p : b.getWorld().getPlayers()) if (p.getLocation().distanceSquared(t.onde) < 1024) { perto = true; break; }
            if (perto) b.getWorld().spawnParticle(Particle.SOUL, b.getLocation().add(0.5, 0.6, 0.5), 2, 0.2, 0.2, 0.2, 0.01);
        }
    }

    public int quantos() {
        return tumulos.size();
    }
}
