package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.mundo.Estacoes.Estacao;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As plantações da estação: plantadas em terra arada, crescem em 4 fases desenhadas (só na
 * estação delas e com a terra molhada), e se colhem com um clique. Farinha de osso adianta
 * uma fase. As sementes saem do mato quebrado na estação certa.
 */
public final class Cultivos implements Listener {

    private static final NamespacedKey K_ENT = new NamespacedKey("rpgatributos", "cultivo_ponto");
    /** Chance de passar de fase a cada minuto (em média ~12 min por fase). */
    private static final double CHANCE_FASE = 1 / 12.0;

    private static final class Planta {
        String mundo;
        int x, y, z;
        Cultivo tipo;
        int fase;
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<String, Planta> plantas = new HashMap<>();
    private final Map<String, UUID[]> visiveis = new HashMap<>();
    /** Fase que está aparecendo em cada planta (para trocar o modelo quando cresce). */
    private final Map<String, Integer> faseVisivel = new HashMap<>();
    private boolean sujo;
    private int ciclos;

    public Cultivos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "cultivos.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static String chave(String mundo, int x, int y, int z) {
        return mundo + "," + x + "," + y + "," + z;
    }

    public void carregar() {
        if (!arquivo.exists()) return;
        for (String linha : YamlConfiguration.loadConfiguration(arquivo).getStringList("plantas")) {
            String[] a = linha.split(",");
            if (a.length != 6) continue;
            try {
                Planta p = new Planta();
                p.mundo = a[0];
                p.x = Integer.parseInt(a[1]);
                p.y = Integer.parseInt(a[2]);
                p.z = Integer.parseInt(a[3]);
                p.tipo = Cultivo.valueOf(a[4]);
                p.fase = Integer.parseInt(a[5]);
                plantas.put(chave(p.mundo, p.x, p.y, p.z), p);
            } catch (RuntimeException ignorado) {
                // linha inválida
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        List<String> l = new ArrayList<>();
        for (Planta p : plantas.values()) l.add(chave(p.mundo, p.x, p.y, p.z) + "," + p.tipo.name() + "," + p.fase);
        YamlConfiguration y = new YamlConfiguration();
        y.set("plantas", l);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar cultivos.yml: " + ex.getMessage());
        }
    }

    public int quantas() {
        return plantas.size();
    }

    // =====================================================================
    //  Plantar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoPlantar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        ItemStack mao = e.getPlayer().getInventory().getItemInMainHand();
        Cultivo c = Cultivo.daSemente(mao);
        if (c == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Block terra = e.getClickedBlock();
        if (terra.getType() != Material.FARMLAND) {
            p.sendActionBar(Component.text("✖ Plante em terra arada (use uma enxada).", NamedTextColor.RED));
            return;
        }
        if (!terra.getRelative(0, 1, 0).getType().isAir()) return;
        if (!plugin.territorios().podeConstruir(p.getUniqueId(), terra.getLocation())) {
            p.sendActionBar(Component.text("✖ Essa terra é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        String k = chave(terra.getWorld().getName(), terra.getX(), terra.getY(), terra.getZ());
        if (plantas.containsKey(k)) return;
        Planta pl = new Planta();
        pl.mundo = terra.getWorld().getName();
        pl.x = terra.getX();
        pl.y = terra.getY();
        pl.z = terra.getZ();
        pl.tipo = c;
        plantas.put(k, pl);
        sujo = true;
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        terra.getWorld().playSound(terra.getLocation().add(0.5, 1, 0.5), Sound.ITEM_CROP_PLANT, 1f, 1f);
        mostrar(terra.getWorld(), k, pl);
        Estacao agora = plugin.estacoes().atual();
        p.sendActionBar(Component.text(c.estacao().icone() + " " + c.nome() + " plantado"
                + (agora == c.estacao() ? ". Mantenha a terra molhada." : ". Ele só cresce na " + c.estacao().nome().toLowerCase(java.util.Locale.ROOT) + "."), c.cor()));
    }

    // =====================================================================
    //  Crescer (a cada 5 s; a chance de fase conta por minuto)
    // =====================================================================

    public void tick() {
        ciclos++;
        boolean minuto = ciclos % 12 == 0;
        Estacao estacao = plugin.estacoes().atual();
        for (Iterator<Map.Entry<String, Planta>> it = plantas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Planta> en = it.next();
            Planta pl = en.getValue();
            World w = Bukkit.getWorld(pl.mundo);
            if (w == null || !w.isChunkLoaded(pl.x >> 4, pl.z >> 4)) continue;
            Block terra = w.getBlockAt(pl.x, pl.y, pl.z);
            // A terra virou terra comum (pisoteada) ou alguém pôs um bloco em cima: a planta se perde.
            if (terra.getType() != Material.FARMLAND || !terra.getRelative(0, 1, 0).getType().isAir()) {
                esconder(en.getKey());
                it.remove();
                sujo = true;
                w.dropItemNaturally(terra.getLocation().add(0.5, 1.2, 0.5), pl.tipo.semente(1));
                continue;
            }
            if (minuto && pl.fase < 3 && pl.tipo.estacao() == estacao && terra.getBlockData() instanceof Farmland f && f.getMoisture() > 0) {
                double chance = CHANCE_FASE * (w.hasStorm() ? 1.5 : 1);
                if (rnd().nextDouble() < chance) {
                    pl.fase++;
                    sujo = true;
                }
            }
            boolean perto = false;
            for (Player p : w.getPlayers()) {
                double dx = pl.x - p.getLocation().getX(), dz = pl.z - p.getLocation().getZ();
                if (dx * dx + dz * dz < 48 * 48) { perto = true; break; }
            }
            if (perto) mostrar(w, en.getKey(), pl);
        }
        if (sujo && ciclos % 6 == 0) salvar();
    }

    private void mostrar(World w, String k, Planta pl) {
        UUID[] ids = visiveis.get(k);
        if (ids != null && Bukkit.getEntity(ids[0]) instanceof ItemDisplay d && d.isValid()) {
            if (faseVisivel.getOrDefault(k, -1) == pl.fase) return;
            d.setItemStack(pl.tipo.fase(pl.fase));
            faseVisivel.put(k, pl.fase);
            return;
        }
        esconder(k);
        Location l = new Location(w, pl.x + 0.5, pl.y + 1.4375, pl.z + 0.5);
        ItemDisplay d = w.spawn(l, ItemDisplay.class, x -> {
            x.setItemStack(pl.tipo.fase(pl.fase));
            x.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            x.setBillboard(Display.Billboard.FIXED);
            x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
        });
        Interaction i = w.spawn(new Location(w, pl.x + 0.5, pl.y + 0.9375, pl.z + 0.5), Interaction.class, x -> {
            x.setInteractionWidth(0.9f);
            x.setInteractionHeight(0.9f);
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
        });
        visiveis.put(k, new UUID[]{d.getUniqueId(), i.getUniqueId()});
        faseVisivel.put(k, pl.fase);
    }

    private void esconder(String k) {
        faseVisivel.remove(k);
        UUID[] ids = visiveis.remove(k);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
    }

    // =====================================================================
    //  Colher, adubar e perder
    // =====================================================================

    /**
     * Bater na planta: madura, colhe; ainda crescendo, arranca (a semente volta). É o jeito de
     * tirar uma planta, já que ela cobre a terra arada.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoBater(io.papermc.paper.event.player.PrePlayerAttackEntityEvent e) {
        if (!(e.getAttacked() instanceof Interaction i)) return;
        String k = i.getPersistentDataContainer().get(K_ENT, PersistentDataType.STRING);
        if (k == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Planta pl = plantas.get(k);
        if (pl == null) {
            esconder(k);
            return;
        }
        if (!plugin.territorios().podeConstruir(p.getUniqueId(), i.getLocation())) {
            p.sendActionBar(Component.text("✖ Essa plantação é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        if (pl.fase >= 3) {
            colher(p, i, k, pl);
            return;
        }
        plantas.remove(k);
        esconder(k);
        sujo = true;
        Location l = i.getLocation().add(0, 0.6, 0);
        l.getWorld().dropItemNaturally(l, pl.tipo.semente(1));
        l.getWorld().playSound(l, Sound.BLOCK_CROP_BREAK, 1f, 0.9f);
        p.sendActionBar(Component.text("🌱 " + pl.tipo.nome() + " arrancado (a semente voltou).", NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i)) return;
        String k = i.getPersistentDataContainer().get(K_ENT, PersistentDataType.STRING);
        if (k == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Planta pl = plantas.get(k);
        if (pl == null) {
            esconder(k);
            return;
        }
        if (!plugin.territorios().podeConstruir(p.getUniqueId(), i.getLocation())) {
            p.sendActionBar(Component.text("✖ Essa plantação é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        World w = i.getWorld();
        Location l = i.getLocation().add(0, 0.6, 0);
        ItemStack mao = p.getInventory().getItemInMainHand();
        boolean naEstacao = pl.tipo.estacao() == plugin.estacoes().atual();
        if (pl.fase < 3) {
            if (mao.getType() == Material.BONE_MEAL && mao.getPersistentDataContainer().getKeys().isEmpty()) {
                if (!naEstacao) {
                    p.sendActionBar(Component.text("✖ Fora de época: " + pl.tipo.nome() + " só cresce na " + pl.tipo.estacao().nome().toLowerCase(java.util.Locale.ROOT) + ".", NamedTextColor.RED));
                    return;
                }
                if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
                pl.fase++;
                sujo = true;
                w.spawnParticle(Particle.HAPPY_VILLAGER, l, 10, 0.3, 0.3, 0.3, 0);
                w.playSound(l, Sound.ITEM_BONE_MEAL_USE, 1f, 1f);
                mostrar(w, k, pl);
                return;
            }
            p.sendActionBar(Component.text(pl.tipo.estacao().icone() + " " + pl.tipo.nome() + ": fase " + (pl.fase + 1) + " de 4"
                    + (naEstacao ? " (cresce com a terra molhada)" : " — fora de época, só cresce na " + pl.tipo.estacao().nome().toLowerCase(java.util.Locale.ROOT)), pl.tipo.cor()));
            return;
        }
        colher(p, i, k, pl);
    }

    private void colher(Player p, Interaction i, String k, Planta pl) {
        World w = i.getWorld();
        Location l = i.getLocation().add(0, 0.6, 0);
        int nivel = plugin.stats().getNivel(p, Skill.AGRICULTURA);
        double fracao = Math.min(1, nivel / (double) plugin.settings().nivelMaximo);
        int qtd = 1 + rnd().nextInt(2) + (rnd().nextDouble() < fracao * 0.5 ? 1 : 0);
        ItemStack colheita = pl.tipo.colheita(qtd);
        Qualidade.sortear(fracao).aplicar(colheita);
        w.dropItemNaturally(l, colheita);
        w.playSound(l, Sound.BLOCK_CROP_BREAK, 1f, 1.1f);
        w.spawnParticle(Particle.ITEM, l, 10, 0.2, 0.2, 0.2, 0.05, pl.tipo.colheita(1));
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.AGRICULTURA, 5);
        plugin.titulos().registrar(p, "colheitas_estacao", 1);
        plugin.diario().marco(p, "primeira_" + pl.tipo.id(), "Colheu " + pl.tipo.nome() + " pela primeira vez");
        if (pl.tipo.rebrota()) {
            pl.fase = 1;
            sujo = true;
            mostrar(w, k, pl);
        } else {
            plantas.remove(k);
            esconder(k);
            sujo = true;
            if (rnd().nextDouble() < 0.4) w.dropItemNaturally(l, pl.tipo.semente(1 + rnd().nextInt(2)));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        // A terra arada da planta: ela se perde e devolve a semente.
        if (b.getType() == Material.FARMLAND) {
            String k = chave(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
            Planta pl = plantas.remove(k);
            if (pl != null) {
                esconder(k);
                sujo = true;
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 1, 0.5), pl.tipo.semente(1));
            }
            return;
        }
        // Mato quebrado na estação: às vezes cai uma semente da estação.
        Material m = b.getType();
        if ((m != Material.SHORT_GRASS && m != Material.TALL_GRASS && m != Material.FERN && m != Material.LARGE_FERN)
                || e.getPlayer().getGameMode() == GameMode.CREATIVE || b.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        if (rnd().nextDouble() >= 0.04) return;
        Estacao estacao = plugin.estacoes().atual();
        List<Cultivo> daEstacao = new ArrayList<>();
        for (Cultivo c : Cultivo.values()) if (c.estacao() == estacao) daEstacao.add(c);
        if (daEstacao.isEmpty()) return;
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.3, 0.5), daEstacao.get(rnd().nextInt(daEstacao.size())).semente(1));
    }

    /**
     * O Fazendeiro da colônia: colhe até {@code maximo} plantações da estação maduras dentro da
     * área. As que rebrotam voltam à fase 2; as outras são replantadas (o fazendeiro guarda a semente).
     */
    public List<ItemStack> colherEm(java.util.function.Predicate<Location> area, int maximo) {
        List<ItemStack> l = new ArrayList<>();
        for (Map.Entry<String, Planta> en : plantas.entrySet()) {
            if (l.size() >= maximo) break;
            Planta pl = en.getValue();
            World w = Bukkit.getWorld(pl.mundo);
            if (pl.fase < 3 || w == null || !area.test(new Location(w, pl.x, pl.y, pl.z))) continue;
            l.add(pl.tipo.colheita(1 + rnd().nextInt(2)));
            pl.fase = pl.tipo.rebrota() ? 1 : 0;
            sujo = true;
            if (w.isChunkLoaded(pl.x >> 4, pl.z >> 4)) mostrar(w, en.getKey(), pl);
        }
        return l;
    }

    public void parar() {
        for (String k : new ArrayList<>(visiveis.keySet())) esconder(k);
        salvar();
    }
}
