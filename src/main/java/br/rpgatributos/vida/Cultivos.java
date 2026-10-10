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
    /** Chance, a cada minuto, de 9 maduras em 3×3 virarem uma gigante. */
    private static final double CHANCE_GIGANTE = 0.02;

    private static final class Planta {
        String mundo;
        int x, y, z;
        Cultivo tipo;
        int fase;
        /** Murchou fora de época (só sai arrancando). */
        boolean murcha;
        /** É o centro de uma colheita gigante 3×3. */
        boolean gigante;
        /** A estação em que a planta foi vista por último (para murchar quando ela troca). */
        Estacao vista;

        /** O que está aparecendo (troca o modelo quando muda). */
        int visual() { return gigante ? 100 : murcha ? 50 : fase; }
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
            if (a.length < 6) continue;
            try {
                Planta p = new Planta();
                p.mundo = a[0];
                p.x = Integer.parseInt(a[1]);
                p.y = Integer.parseInt(a[2]);
                p.z = Integer.parseInt(a[3]);
                p.tipo = Cultivo.valueOf(a[4]);
                p.fase = Integer.parseInt(a[5]);
                if (a.length > 6) {
                    p.murcha = a[6].contains("M");
                    p.gigante = a[6].contains("G");
                }
                plantas.put(chave(p.mundo, p.x, p.y, p.z), p);
            } catch (RuntimeException ignorado) {
                // linha inválida
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        List<String> l = new ArrayList<>();
        for (Planta p : plantas.values()) {
            String marcas = (p.murcha ? "M" : "") + (p.gigante ? "G" : "");
            l.add(chave(p.mundo, p.x, p.y, p.z) + "," + p.tipo.name() + "," + p.fase + (marcas.isEmpty() ? "" : "," + marcas));
        }
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
        // Debaixo de uma gigante não dá.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Planta vizinha = plantas.get(chave(terra.getWorld().getName(), terra.getX() + dx, terra.getY(), terra.getZ() + dz));
                if (vizinha != null && vizinha.gigante) return;
            }
        }
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
        pl.vista = agora;
        boolean cresce = c.cresceEm(agora) || estufa(terra);
        p.sendActionBar(Component.text(c.estacao().icone() + " " + c.nome() + " plantado"
                + (cresce ? ". Mantenha a terra molhada." : ". Ele só cresce " + c.quando() + " (ou numa estufa).")
                + (!cresce && plugin.settings().plantasMurcham ? " Fora de época, murcha quando a estação trocar!" : ""), c.cor()));
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
                if (!pl.murcha) w.dropItemNaturally(terra.getLocation().add(0.5, 1.2, 0.5), pl.tipo.semente(1));
                continue;
            }
            // Troca de estação: quem não é desta estação (e não está na estufa) murcha.
            if (pl.vista == null) pl.vista = estacao;
            if (pl.vista != estacao) {
                pl.vista = estacao;
                if (!pl.murcha && !pl.gigante && !pl.tipo.cresceEm(estacao) && plugin.settings().plantasMurcham && !estufa(terra)) {
                    pl.murcha = true;
                    sujo = true;
                }
            }
            if (minuto && !pl.murcha && !pl.gigante && pl.fase < 3 && (pl.tipo.cresceEm(estacao) || estufa(terra))
                    && terra.getBlockData() instanceof Farmland f && f.getMoisture() > 0) {
                double chance = (1.0 / pl.tipo.minutosPorFase()) * (w.hasStorm() ? 1.5 : 1);
                if (rnd().nextDouble() < chance) {
                    pl.fase++;
                    sujo = true;
                }
            }
            if (minuto && pl.fase >= 3 && !pl.murcha && !pl.gigante && pl.tipo.gigante() && plugin.settings().colheitasGigantes
                    && rnd().nextDouble() < CHANCE_GIGANTE) {
                gigantes.add(en.getKey());
            }
            boolean perto = false;
            for (Player p : w.getPlayers()) {
                double dx = pl.x - p.getLocation().getX(), dz = pl.z - p.getLocation().getZ();
                if (dx * dx + dz * dz < 48 * 48) { perto = true; break; }
            }
            if (perto) mostrar(w, en.getKey(), pl);
        }
        for (String k : gigantes) formarGigante(k);
        gigantes.clear();
        if (sujo && ciclos % 6 == 0) salvar();
    }

    private final List<String> gigantes = new ArrayList<>();

    /** Estufa: o primeiro bloco acima da planta (até 12 de altura) é de vidro. Lá dentro, cresce em qualquer estação. */
    public static boolean estufa(Block terra) {
        for (int dy = 2; dy <= 12; dy++) {
            Material m = terra.getRelative(0, dy, 0).getType();
            if (m.isAir()) continue;
            return m.name().contains("GLASS");
        }
        return false;
    }

    /** Se as 9 em volta (3×3, esta no meio) são do mesmo tipo e estão maduras, viram uma gigante no meio. */
    private void formarGigante(String k) {
        Planta meio = plantas.get(k);
        if (meio == null || meio.gigante || meio.murcha) return;
        List<String> partes = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String kk = chave(meio.mundo, meio.x + dx, meio.y, meio.z + dz);
                Planta p = plantas.get(kk);
                if (p == null || p.tipo != meio.tipo || p.fase < 3 || p.murcha || p.gigante) return;
                if (dx != 0 || dz != 0) partes.add(kk);
            }
        }
        for (String kk : partes) {
            plantas.remove(kk);
            esconder(kk);
        }
        meio.gigante = true;
        sujo = true;
        World w = Bukkit.getWorld(meio.mundo);
        if (w == null) return;
        Location l = new Location(w, meio.x + 0.5, meio.y + 1.5, meio.z + 0.5);
        w.spawnParticle(Particle.HAPPY_VILLAGER, l, 60, 1.2, 0.8, 1.2, 0);
        w.playSound(l, Sound.BLOCK_GROWING_PLANT_CROP, 1f, 0.6f);
        mostrar(w, k, meio);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(l) < 48 * 48) {
                p.sendMessage(Component.text("✿ Uma " + meio.tipo.nome() + " GIGANTE cresceu na plantação!", meio.tipo.cor()));
            }
        }
    }

    private void mostrar(World w, String k, Planta pl) {
        UUID[] ids = visiveis.get(k);
        if (ids != null && Bukkit.getEntity(ids[0]) instanceof ItemDisplay d && d.isValid()) {
            if (faseVisivel.getOrDefault(k, -1) == pl.visual()) return;
            // Virou gigante: o tamanho muda, então cria de novo.
            if (!pl.gigante) {
                d.setItemStack(modelo(pl));
                faseVisivel.put(k, pl.visual());
                return;
            }
        }
        esconder(k);
        float escala = pl.gigante ? 3f : 1f;
        Location l = new Location(w, pl.x + 0.5, pl.y + (pl.gigante ? 2.5 : 1.4375), pl.z + 0.5);
        ItemDisplay d = w.spawn(l, ItemDisplay.class, x -> {
            x.setItemStack(modelo(pl));
            x.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            x.setBillboard(Display.Billboard.FIXED);
            x.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(escala, escala, escala), new AxisAngle4f()));
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
        });
        Interaction i = w.spawn(new Location(w, pl.x + 0.5, pl.y + (pl.gigante ? 1.0 : 0.9375), pl.z + 0.5), Interaction.class, x -> {
            x.setInteractionWidth(pl.gigante ? 2.9f : 0.9f);
            x.setInteractionHeight(pl.gigante ? 2.6f : 0.9f);
            x.setPersistent(false);
            x.getPersistentDataContainer().set(K_ENT, PersistentDataType.STRING, k);
        });
        visiveis.put(k, new UUID[]{d.getUniqueId(), i.getUniqueId()});
        faseVisivel.put(k, pl.visual());
    }

    private static ItemStack modelo(Planta pl) {
        if (pl.gigante) return pl.tipo.colheita(1);
        if (pl.murcha) return Cultivo.murcha();
        return pl.tipo.fase(pl.fase);
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
        if (pl.murcha) {
            arrancarMurcha(p, i, k, pl);
            return;
        }
        if (pl.fase >= 3 || pl.gigante) {
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
        if (pl.murcha) {
            arrancarMurcha(p, i, k, pl);
            return;
        }
        boolean naEstacao = pl.tipo.cresceEm(plugin.estacoes().atual()) || estufa(w.getBlockAt(pl.x, pl.y, pl.z));
        if (pl.fase < 3 && !pl.gigante) {
            if (mao.getType() == Material.BONE_MEAL && mao.getPersistentDataContainer().getKeys().isEmpty()) {
                if (!naEstacao) {
                    p.sendActionBar(Component.text("✖ Fora de época: " + pl.tipo.nome() + " só cresce " + pl.tipo.quando() + " (ou numa estufa).", NamedTextColor.RED));
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
                    + (naEstacao ? " (cresce com a terra molhada)" : " — fora de época, só cresce " + pl.tipo.quando()), pl.tipo.cor()));
            return;
        }
        colher(p, i, k, pl);
    }

    private void colher(Player p, Interaction i, String k, Planta pl) {
        World w = i.getWorld();
        Location l = i.getLocation().add(0, 0.6, 0);
        int nivel = plugin.stats().getNivel(p, Skill.AGRICULTURA);
        double fracao = Math.min(1, nivel / (double) plugin.settings().nivelMaximo);
        int qtd = pl.gigante ? 15 + rnd().nextInt(7) : 1 + rnd().nextInt(2) + (rnd().nextDouble() < fracao * 0.5 ? 1 : 0);
        // Cada unidade com a sua qualidade (a gigante sai em várias pilhas).
        Map<Qualidade, Integer> porQualidade = new java.util.EnumMap<>(Qualidade.class);
        for (int n = 0; n < (pl.gigante ? qtd : 1); n++) porQualidade.merge(Qualidade.sortear(fracao), pl.gigante ? 1 : qtd, Integer::sum);
        for (Map.Entry<Qualidade, Integer> en : porQualidade.entrySet()) {
            ItemStack colheita = pl.tipo.colheita(en.getValue());
            en.getKey().aplicar(colheita);
            w.dropItemNaturally(l, colheita);
        }
        if (pl.gigante) {
            w.dropItemNaturally(l, pl.tipo.semente(2 + rnd().nextInt(3)));
            w.spawnParticle(Particle.ITEM, l, 40, 1, 0.6, 1, 0.1, pl.tipo.colheita(1));
            w.playSound(l, Sound.BLOCK_WOOD_BREAK, 1f, 0.6f);
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.AGRICULTURA, 40);
            plugin.diario().marco(p, "gigante_" + pl.tipo.id(), "Colheu uma " + pl.tipo.nome() + " gigante");
            plantas.remove(k);
            esconder(k);
            sujo = true;
            return;
        }
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

    /** A planta murcha só sai arrancando (não devolve nada: ela morreu). */
    private void arrancarMurcha(Player p, Interaction i, String k, Planta pl) {
        plantas.remove(k);
        esconder(k);
        sujo = true;
        Location l = i.getLocation().add(0, 0.6, 0);
        l.getWorld().playSound(l, Sound.BLOCK_GRASS_BREAK, 1f, 0.8f);
        l.getWorld().spawnParticle(Particle.BLOCK, l, 12, 0.2, 0.2, 0.2, Material.DEAD_BUSH.createBlockData());
        p.sendActionBar(Component.text("🥀 " + pl.tipo.nome() + " murchou fora de época. Plante na estação certa " + "(" + pl.tipo.quando() + ") ou numa estufa.",
                NamedTextColor.GRAY));
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
                if (!pl.murcha) b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 1, 0.5), pl.tipo.semente(1));
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
        for (Cultivo c : Cultivo.values()) if (c.cresceEm(estacao)) daEstacao.add(c);
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
            if (pl.fase < 3 || pl.murcha || pl.gigante || w == null || !area.test(new Location(w, pl.x, pl.y, pl.z))) continue;
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
