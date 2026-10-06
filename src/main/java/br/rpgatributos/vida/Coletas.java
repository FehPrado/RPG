package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.mundo.Estacoes.Estacao;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Coletas da estação: aparecem aos poucos no chão perto de quem explora, conforme o bioma, e
 * somem quando a estação acaba. Clique para pegar.
 */
public final class Coletas implements Listener {

    private static final NamespacedKey K_ENT = new NamespacedKey("rpgatributos", "coleta_ponto");
    private static final Set<Material> CHAO = Set.of(Material.GRASS_BLOCK, Material.PODZOL, Material.DIRT, Material.COARSE_DIRT,
            Material.MOSS_BLOCK, Material.SAND, Material.SNOW_BLOCK, Material.MYCELIUM, Material.ROOTED_DIRT, Material.MUD);
    private static final int MAX_TOTAL = 250;

    private static final class Ponto {
        String mundo;
        int x, y, z;
        Coleta tipo;
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<String, Ponto> pontos = new HashMap<>();
    private final Map<String, UUID[]> visiveis = new HashMap<>();
    private boolean sujo;

    public Coletas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "coletas.yml");
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
        for (String linha : YamlConfiguration.loadConfiguration(arquivo).getStringList("pontos")) {
            String[] a = linha.split(",");
            if (a.length != 5) continue;
            try {
                Ponto p = new Ponto();
                p.mundo = a[0];
                p.x = Integer.parseInt(a[1]);
                p.y = Integer.parseInt(a[2]);
                p.z = Integer.parseInt(a[3]);
                p.tipo = Coleta.valueOf(a[4]);
                pontos.put(chave(p.mundo, p.x, p.y, p.z), p);
            } catch (RuntimeException ignorado) {
                // linha inválida
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        List<String> l = new ArrayList<>();
        for (Ponto p : pontos.values()) l.add(chave(p.mundo, p.x, p.y, p.z) + "," + p.tipo.name());
        YamlConfiguration y = new YamlConfiguration();
        y.set("pontos", l);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar coletas.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  A cada 5 s
    // =====================================================================

    public void tick() {
        if (!plugin.settings().coletasAtivas) return;
        Estacao estacao = plugin.estacoes().atual();
        // A estação virou: o que era da anterior some.
        for (Iterator<Map.Entry<String, Ponto>> it = pontos.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Ponto> en = it.next();
            if (en.getValue().tipo.estacao() == estacao) continue;
            esconder(en.getKey());
            it.remove();
            sujo = true;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getEnvironment() != World.Environment.NORMAL || plugin.masmorras().ehMundo(p.getWorld())) continue;
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            int perto = 0;
            Location l = p.getLocation();
            for (Ponto pt : pontos.values()) {
                if (!pt.mundo.equals(p.getWorld().getName())) continue;
                double dx = pt.x - l.getX(), dz = pt.z - l.getZ();
                if (dx * dx + dz * dz < 32 * 32) perto++;
            }
            if (perto < plugin.settings().coletasPorJogador && pontos.size() < MAX_TOTAL && rnd().nextDouble() < 0.5) nascer(p, estacao);
        }
        // Mostra para quem está perto e dá um brilho de vez em quando.
        for (Map.Entry<String, Ponto> en : pontos.entrySet()) {
            Ponto pt = en.getValue();
            World w = Bukkit.getWorld(pt.mundo);
            if (w == null || !w.isChunkLoaded(pt.x >> 4, pt.z >> 4)) continue;
            boolean alguem = false;
            for (Player p : w.getPlayers()) {
                double dx = pt.x - p.getLocation().getX(), dz = pt.z - p.getLocation().getZ();
                if (dx * dx + dz * dz < 48 * 48) { alguem = true; break; }
            }
            if (!alguem) continue;
            mostrar(w, en.getKey(), pt);
            if (rnd().nextDouble() < 0.4) w.spawnParticle(Particle.HAPPY_VILLAGER, pt.x + 0.5, pt.y + 0.4, pt.z + 0.5, 2, 0.15, 0.1, 0.15, 0);
        }
        if (sujo) salvar();
    }

    private void nascer(Player p, Estacao estacao) {
        World w = p.getWorld();
        for (int tentativa = 0; tentativa < 6; tentativa++) {
            double ang = rnd().nextDouble() * Math.PI * 2, d = rnd().nextDouble(8, 28);
            int x = p.getLocation().getBlockX() + (int) (Math.cos(ang) * d), z = p.getLocation().getBlockZ() + (int) (Math.sin(ang) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block chao = w.getHighestBlockAt(x, z);
            if (!CHAO.contains(chao.getType())) continue;
            Block cima = chao.getRelative(0, 1, 0);
            if (!cima.getType().isAir() && cima.getType() != Material.SNOW && cima.getType() != Material.SHORT_GRASS) continue;
            if (pontos.containsKey(chave(w.getName(), x, cima.getY(), z))) continue;
            String bioma = w.getBiome(x, cima.getY(), z).getKey().getKey();
            List<Coleta> opcoes = new ArrayList<>();
            for (Coleta c : Coleta.values()) if (c.estacao() == estacao && c.aceita(bioma)) opcoes.add(c);
            if (opcoes.isEmpty()) continue;
            Ponto pt = new Ponto();
            pt.mundo = w.getName();
            pt.x = x;
            pt.y = cima.getY();
            pt.z = z;
            pt.tipo = opcoes.get(rnd().nextInt(opcoes.size()));
            pontos.put(chave(pt.mundo, pt.x, pt.y, pt.z), pt);
            sujo = true;
            return;
        }
    }

    private void mostrar(World w, String k, Ponto pt) {
        UUID[] ids = visiveis.get(k);
        if (ids != null && Bukkit.getEntity(ids[0]) instanceof ItemDisplay d && d.isValid()) return;
        esconder(k);
        Location l = new Location(w, pt.x + 0.5, pt.y + 0.3, pt.z + 0.5);
        ItemDisplay d = w.spawn(l, ItemDisplay.class, x -> {
            x.setItemStack(pt.tipo.criar(1));
            x.setBillboard(Display.Billboard.VERTICAL);
            x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.55f, 0.55f, 0.55f), new AxisAngle4f()));
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
        });
        Interaction i = w.spawn(l.clone().subtract(0, 0.3, 0), Interaction.class, x -> {
            x.setInteractionWidth(0.6f);
            x.setInteractionHeight(0.6f);
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
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
    public void aoPegar(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i)) return;
        String k = i.getPersistentDataContainer().get(K_ENT, PersistentDataType.STRING);
        if (k == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Ponto pt = pontos.remove(k);
        esconder(k);
        if (pt == null) return;
        sujo = true;
        Player p = e.getPlayer();
        int qtd = 1 + (rnd().nextDouble() < 0.15 + plugin.stats().getNivel(p, Skill.AGRICULTURA) * 0.003 ? 1 : 0);
        Location l = i.getLocation().add(0, 0.3, 0);
        l.getWorld().dropItemNaturally(l, pt.tipo.criar(qtd));
        l.getWorld().playSound(l, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1.3f);
        l.getWorld().spawnParticle(Particle.ITEM, l, 8, 0.1, 0.1, 0.1, 0.05, pt.tipo.criar(1));
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.AGRICULTURA, 4);
        plugin.titulos().registrar(p, "coletas", 1);
        plugin.diario().marco(p, "primeira_coleta", "Colheu a primeira coleta da estação: " + pt.tipo.nome());
        p.sendActionBar(Component.text(pt.tipo.estacao().icone() + " " + pt.tipo.nome() + (qtd > 1 ? " x" + qtd : ""), pt.tipo.cor()));
    }

    /** Comer uma coleta ou uma colheita da estação dá o efeito dela. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        Coleta c = Coleta.de(e.getItem());
        Cultivo cv = c == null ? Cultivo.daColheita(e.getItem()) : null;
        if (c == null && cv == null) return;
        Player p = e.getPlayer();
        var efeito = c != null ? c.efeito() : cv.efeito();
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (p.isOnline()) p.addPotionEffect(efeito); });
    }

    /** Pelo /rpgadmin: faz uma coleta da estação nascer perto. */
    public boolean nascerAgora(Player p) {
        int antes = pontos.size();
        for (int i = 0; i < 10 && pontos.size() == antes; i++) nascer(p, plugin.estacoes().atual());
        return pontos.size() > antes;
    }

    public void parar() {
        for (String k : new ArrayList<>(visiveis.keySet())) esconder(k);
        salvar();
    }
}
