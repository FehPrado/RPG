package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pomar: mudas especiais (Laranjeira, Pessegueiro, Cerejeira, Limoeiro e Macieira Dourada)
 * crescem como árvores do jogo, mas na estação certa as frutas aparecem penduradas embaixo das
 * folhas. Clique na fruta para colher. Fora da estação, as frutas caem. O Fazendeiro da colônia
 * colhe as frutas maduras em volta da Prefeitura.
 */
public final class Pomar implements Listener {

    private static final String DADO_MUDA = "muda";
    private static final NamespacedKey K_FRUTA_ENT = new NamespacedKey("rpgatributos", "fruta_pendurada");
    private static final int MAX_FRUTAS = 6;

    private static final class Arvore {
        String mundo;
        int x, y, z;
        Fruta fruta;
        final List<int[]> frutas = new ArrayList<>();
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<String, Arvore> arvores = new LinkedHashMap<>();
    /** "mundo,x,y,z" da folha → [display, interação]. */
    private final Map<String, UUID[]> visiveis = new HashMap<>();
    private boolean sujo;

    public Pomar(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "pomares.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static String chave(String mundo, int x, int y, int z) {
        return mundo + "," + x + "," + y + "," + z;
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String linha : y.getStringList("arvores")) {
            String[] partes = linha.split(";");
            String[] a = partes[0].split(",");
            if (a.length != 5) continue;
            try {
                Arvore ar = new Arvore();
                ar.mundo = a[0];
                ar.x = Integer.parseInt(a[1]);
                ar.y = Integer.parseInt(a[2]);
                ar.z = Integer.parseInt(a[3]);
                ar.fruta = Fruta.valueOf(a[4]);
                for (int i = 1; i < partes.length; i++) {
                    String[] f = partes[i].split(",");
                    ar.frutas.add(new int[]{Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2])});
                }
                arvores.put(chave(ar.mundo, ar.x, ar.y, ar.z), ar);
            } catch (RuntimeException ignorado) {
                // linha inválida
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        List<String> l = new ArrayList<>();
        for (Arvore a : arvores.values()) {
            StringBuilder sb = new StringBuilder(chave(a.mundo, a.x, a.y, a.z) + "," + a.fruta.name());
            for (int[] f : a.frutas) sb.append(';').append(f[0]).append(',').append(f[1]).append(',').append(f[2]);
            l.add(sb.toString());
        }
        YamlConfiguration y = new YamlConfiguration();
        y.set("arvores", l);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar pomares.yml: " + ex.getMessage());
        }
    }

    public int quantas() {
        return arvores.size();
    }

    // =====================================================================
    //  Plantar e crescer
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPlantar(BlockPlaceEvent e) {
        Fruta f = Fruta.daMuda(e.getItemInHand());
        if (f == null) return;
        DadosBloco.gravar(DADO_MUDA, e.getBlockPlaced().getLocation(), f.name());
        e.getPlayer().sendActionBar(Component.text("🍊 " + f.arvore() + " plantada. Use farinha de osso ou espere crescer.", f.cor()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrarMuda(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!Tag.SAPLINGS.isTagged(b.getType())) return;
        String s = DadosBloco.ler(DADO_MUDA, b.getLocation());
        if (s == null) return;
        DadosBloco.apagar(DADO_MUDA, b.getLocation());
        try {
            Fruta f = Fruta.valueOf(s);
            e.setDropItems(false);
            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), f.muda(1));
        } catch (IllegalArgumentException ignorado) {
            // dado inválido: a muda comum cai
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCrescer(StructureGrowEvent e) {
        Location l = e.getLocation();
        String s = DadosBloco.ler(DADO_MUDA, l);
        if (s == null) return;
        DadosBloco.apagar(DADO_MUDA, l);
        Fruta f;
        try {
            f = Fruta.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return;
        }
        Arvore a = new Arvore();
        a.mundo = l.getWorld().getName();
        a.x = l.getBlockX();
        a.y = l.getBlockY();
        a.z = l.getBlockZ();
        a.fruta = f;
        arvores.put(chave(a.mundo, a.x, a.y, a.z), a);
        sujo = true;
        if (e.getPlayer() != null) {
            e.getPlayer().sendMessage(Component.text("🍊 A " + f.arvore() + " cresceu! Ela dá " + f.nome() + " no " + f.nomesEstacoes() + ".", f.cor()));
        }
    }

    /** A árvore ainda está de pé (tronco no lugar da muda). */
    private static boolean viva(World w, Arvore a) {
        return Tag.LOGS.isTagged(w.getBlockAt(a.x, a.y, a.z).getType());
    }

    // =====================================================================
    //  Frutas
    // =====================================================================

    /** A cada 5 s: frutas nascem na estação, caem fora dela, e aparecem para quem está perto. */
    public void tick() {
        var estacao = plugin.estacoes().atual();
        Iterator<Map.Entry<String, Arvore>> it = arvores.entrySet().iterator();
        while (it.hasNext()) {
            Arvore a = it.next().getValue();
            World w = Bukkit.getWorld(a.mundo);
            if (w == null || !w.isChunkLoaded(a.x >> 4, a.z >> 4)) continue;
            if (!viva(w, a)) {
                for (int[] f : a.frutas) esconder(chave(a.mundo, f[0], f[1], f[2]));
                it.remove();
                sujo = true;
                continue;
            }
            boolean naEstacao = a.fruta.estacoes().contains(estacao);
            // Frutas sem a folha em cima, ou fora da estação, caem.
            for (Iterator<int[]> fi = a.frutas.iterator(); fi.hasNext(); ) {
                int[] f = fi.next();
                if (naEstacao && Tag.LEAVES.isTagged(w.getBlockAt(f[0], f[1], f[2]).getType())) continue;
                esconder(chave(a.mundo, f[0], f[1], f[2]));
                fi.remove();
                sujo = true;
            }
            if (naEstacao && a.frutas.size() < MAX_FRUTAS && rnd().nextDouble() < 0.12) nascer(w, a);
            boolean perto = false;
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(new Location(w, a.x, a.y, a.z)) < 64 * 64) { perto = true; break; }
            }
            if (perto) for (int[] f : a.frutas) mostrar(w, a, f);
        }
        if (sujo) salvar();
    }

    private void nascer(World w, Arvore a) {
        for (int tentativa = 0; tentativa < 16; tentativa++) {
            int x = a.x + rnd().nextInt(-4, 5), y = a.y + rnd().nextInt(2, 10), z = a.z + rnd().nextInt(-4, 5);
            Block folha = w.getBlockAt(x, y, z);
            if (!Tag.LEAVES.isTagged(folha.getType()) || !folha.getRelative(0, -1, 0).getType().isAir()) continue;
            boolean usada = false;
            for (int[] f : a.frutas) if (f[0] == x && f[1] == y && f[2] == z) { usada = true; break; }
            if (usada) continue;
            a.frutas.add(new int[]{x, y, z});
            sujo = true;
            return;
        }
    }

    private void mostrar(World w, Arvore a, int[] f) {
        String k = chave(a.mundo, f[0], f[1], f[2]);
        UUID[] ids = visiveis.get(k);
        if (ids != null && Bukkit.getEntity(ids[0]) instanceof ItemDisplay d && d.isValid()) return;
        esconder(k);
        Location l = new Location(w, f[0] + 0.5, f[1] - 0.3, f[2] + 0.5);
        ItemDisplay d = w.spawn(l, ItemDisplay.class, x -> {
            x.setItemStack(a.fruta.fruta(1));
            x.setBillboard(Display.Billboard.VERTICAL);
            x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.45f, 0.45f, 0.45f), new AxisAngle4f()));
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_FRUTA_ENT, PersistentDataType.STRING, k);
        });
        Interaction i = w.spawn(l.clone().subtract(0, 0.25, 0), Interaction.class, x -> {
            x.setInteractionWidth(0.5f);
            x.setInteractionHeight(0.5f);
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_FRUTA_ENT, PersistentDataType.STRING, k);
        });
        visiveis.put(k, new UUID[]{d.getUniqueId(), i.getUniqueId()});
    }

    private void esconder(String k) {
        UUID[] ids = visiveis.remove(k);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoColher(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i)) return;
        String k = i.getPersistentDataContainer().get(K_FRUTA_ENT, PersistentDataType.STRING);
        if (k == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!plugin.territorios().podeConstruir(p.getUniqueId(), i.getLocation())) {
            p.sendActionBar(Component.text("✖ Essa árvore é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        String[] c = k.split(",");
        int fx = Integer.parseInt(c[1]), fy = Integer.parseInt(c[2]), fz = Integer.parseInt(c[3]);
        for (Arvore a : arvores.values()) {
            if (!a.mundo.equals(c[0])) continue;
            for (Iterator<int[]> fi = a.frutas.iterator(); fi.hasNext(); ) {
                int[] f = fi.next();
                if (f[0] != fx || f[1] != fy || f[2] != fz) continue;
                fi.remove();
                sujo = true;
                esconder(k);
                Location l = i.getLocation();
                l.getWorld().dropItemNaturally(l, a.fruta.fruta(1));
                l.getWorld().playSound(l, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1.2f);
                l.getWorld().spawnParticle(Particle.ITEM, l, 6, 0.1, 0.1, 0.1, 0.05, a.fruta.fruta(1));
                plugin.stats().darXp(p, Skill.AGRICULTURA, 2);
                plugin.titulos().registrar(p, "frutas", 1);
                plugin.diario().marco(p, "primeira_fruta", "Colheu a primeira fruta do pomar: " + a.fruta.nome());
                return;
            }
        }
        esconder(k);
    }

    /** O Fazendeiro da colônia: colhe até {@code maximo} frutas maduras perto de {@code centro}. */
    /** Colhe até {@code maximo} frutas maduras de árvores dentro da área (o Fazendeiro da colônia). */
    public List<ItemStack> colherEm(java.util.function.Predicate<Location> area, int maximo) {
        List<ItemStack> l = new ArrayList<>();
        for (Arvore a : arvores.values()) {
            if (l.size() >= maximo) break;
            World w = Bukkit.getWorld(a.mundo);
            if (w == null || a.frutas.isEmpty() || !area.test(new Location(w, a.x, a.y, a.z))) continue;
            while (!a.frutas.isEmpty() && l.size() < maximo) {
                int[] f = a.frutas.removeFirst();
                esconder(chave(a.mundo, f[0], f[1], f[2]));
                l.add(a.fruta.fruta(1));
                sujo = true;
            }
        }
        return l;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        Fruta f = Fruta.de(e.getItem());
        if (f == null) return;
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (p.isOnline()) p.addPotionEffect(f.efeito()); });
    }

    /** Frutas desta estação, para o /calendario. */
    public String frutasDaEstacao() {
        var e = plugin.estacoes().atual();
        List<String> l = new ArrayList<>();
        for (Fruta f : Fruta.values()) if (f.estacoes().contains(e)) l.add(f.nome());
        return l.isEmpty() ? "nenhuma" : String.join(", ", l);
    }

    public void parar() {
        for (String k : new ArrayList<>(visiveis.keySet())) esconder(k);
        salvar();
    }
}
