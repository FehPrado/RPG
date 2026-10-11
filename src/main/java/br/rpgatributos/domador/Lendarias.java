package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As montarias lendárias: a fusão no Altar do Domador, o Ovo Lendário (um bloco que choca no
 * lugar certo), o nascimento, a regra de "uma só no servidor", a morte (e o renascer da fênix)
 * e os poderes que rodam a cada 2 s. O estado de cada uma fica em lendarias.yml.
 */
public final class Lendarias implements Listener {

    public static final NamespacedKey K_OVO = new NamespacedKey("rpgatributos", "ovo_lendario");
    private static final NamespacedKey K_OVO_XP = new NamespacedKey("rpgatributos", "ovo_lendario_xp");
    private static final NamespacedKey K_OVO_PROGRESSO = new NamespacedKey("rpgatributos", "ovo_lendario_progresso");
    /** Até onde do altar os dois companheiros da fusão precisam estar. */
    private static final double RAIO_FUSAO = 10;
    /** O dono precisa estar a essa distância do ovo pronto para ele chocar. */
    private static final double RAIO_DONO = 48;

    public enum Fase { LIVRE, OVO, VIVA }

    /** Onde está cada lendária agora. */
    public static final class Estado {
        public Fase fase = Fase.LIVRE;
        public UUID dono, entidade;
        /** Onde o ovo foi posto (null: o ovo está num inventário ou no chão como item). */
        public Location ovo;
        /** Segundos chocando. */
        public int progresso;
        /** XP guardado no ovo (a fênix renasce com o nível que tinha). */
        public int xp;
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<Lendaria, Estado> estados = new EnumMap<>(Lendaria.class);
    private int ciclo;

    public Lendarias(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "lendarias.yml");
        for (Lendaria l : Lendaria.values()) estados.put(l, new Estado());
    }

    private Companheiros comp() { return plugin.companheiros(); }

    public Estado estado(Lendaria l) { return estados.get(l); }

    private int segundosParaChocar() { return Math.max(10, plugin.settings().domaLendariasMinutos * 60); }

    // =====================================================================
    //  Salvar e carregar
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (Lendaria l : Lendaria.values()) {
            ConfigurationSection s = y.getConfigurationSection(l.id());
            if (s == null) continue;
            Estado e = estados.get(l);
            try {
                e.fase = Fase.valueOf(s.getString("fase", "LIVRE"));
            } catch (IllegalArgumentException ex) {
                e.fase = Fase.LIVRE;
            }
            e.dono = uuid(s.getString("dono"));
            e.entidade = uuid(s.getString("entidade"));
            e.progresso = s.getInt("progresso");
            e.xp = s.getInt("xp");
            String ovo = s.getString("ovo");
            if (ovo != null) {
                String[] p = ovo.split(",");
                World w = p.length == 4 ? Bukkit.getWorld(p[0]) : null;
                if (w != null) e.ovo = new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<Lendaria, Estado> en : estados.entrySet()) {
            Estado e = en.getValue();
            String k = en.getKey().id();
            y.set(k + ".fase", e.fase.name());
            if (e.dono != null) y.set(k + ".dono", e.dono.toString());
            if (e.entidade != null) y.set(k + ".entidade", e.entidade.toString());
            if (e.ovo != null) y.set(k + ".ovo", e.ovo.getWorld().getName() + "," + e.ovo.getBlockX() + "," + e.ovo.getBlockY() + "," + e.ovo.getBlockZ());
            y.set(k + ".progresso", e.progresso);
            y.set(k + ".xp", e.xp);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não salvei lendarias.yml: " + ex.getMessage());
        }
    }

    private static UUID uuid(String s) {
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String nomeJogador(UUID id) {
        if (id == null) return "alguém";
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "alguém" : n;
    }

    /** "Livre", "Ovo com Fulano" ou "Vive com Fulano" (para os menus). */
    public String situacao(Lendaria l) {
        Estado e = estados.get(l);
        return switch (e.fase) {
            case LIVRE -> "Ainda não existe no mundo";
            case OVO -> "O ovo está com " + nomeJogador(e.dono);
            case VIVA -> "Vive com " + nomeJogador(e.dono);
        };
    }

    // =====================================================================
    //  Fusão
    // =====================================================================

    /** Os dois companheiros (no estágio III, perto do altar) que entram na fusão, ou null. */
    public LivingEntity[] ingredientes(Player p, Lendaria l, Location altar) {
        LivingEntity a = null, b = null;
        for (Companheiros.Registro r : comp().de(p.getUniqueId())) {
            if (!(Bukkit.getEntity(r.id()) instanceof LivingEntity e) || !e.isValid() || Lendaria.de(e) != null) continue;
            if (!e.getWorld().equals(altar.getWorld()) || e.getLocation().distanceSquared(altar) > RAIO_FUSAO * RAIO_FUSAO) continue;
            if (Evolucao.estagio(e) < 3) continue;
            if (a == null && e.getType() == l.base()) a = e;
            else if (b == null && e.getType() == l.segunda()) b = e;
        }
        return a == null || b == null ? null : new LivingEntity[]{a, b};
    }

    public static int contar(PlayerInventory inv, Object chave) {
        int n = 0;
        for (ItemStack i : inv.getStorageContents()) {
            if (i == null || i.isEmpty()) continue;
            if (chave instanceof Raro r ? Raro.de(i) == r : (i.getType() == chave && Raro.de(i) == null && !i.getPersistentDataContainer().has(K_OVO))) {
                n += i.getAmount();
            }
        }
        return n;
    }

    private static void tirar(PlayerInventory inv, Object chave, int qtd) {
        ItemStack[] cont = inv.getStorageContents();
        for (int k = 0; k < cont.length && qtd > 0; k++) {
            ItemStack i = cont[k];
            if (i == null || i.isEmpty()) continue;
            boolean serve = chave instanceof Raro r ? Raro.de(i) == r : (i.getType() == chave && Raro.de(i) == null);
            if (!serve) continue;
            int usa = Math.min(qtd, i.getAmount());
            i.setAmount(i.getAmount() - usa);
            qtd -= usa;
            cont[k] = i.getAmount() <= 0 ? null : i;
        }
        inv.setStorageContents(cont);
    }

    /** Funde os dois companheiros num Ovo Lendário. @return o motivo de não ter dado certo, ou null. */
    public String fundir(Player p, Lendaria l, Location altar) {
        if (!plugin.settings().domaLendarias) return "As montarias lendárias estão desligadas neste servidor.";
        Estado est = estados.get(l);
        if (est.fase != Fase.LIVRE) return "Só existe um " + l.nome() + " no mundo. " + situacao(l) + ".";
        LivingEntity[] dupla = ingredientes(p, l, altar);
        if (dupla == null) {
            return "Traga até o altar um " + Companheiros.nomeTipo(l.base()) + " e um " + Companheiros.nomeTipo(l.segunda())
                    + " seus, os dois no estágio III.";
        }
        PlayerInventory inv = p.getInventory();
        for (Map.Entry<Object, Integer> en : l.itens().entrySet()) {
            if (contar(inv, en.getKey()) < en.getValue()) return "Faltam itens para a fusão.";
        }
        for (Map.Entry<Object, Integer> en : l.itens().entrySet()) tirar(inv, en.getKey(), en.getValue());
        Location centro = altar.clone().add(0.5, 1.2, 0.5);
        World w = centro.getWorld();
        for (LivingEntity e : dupla) {
            Location de = e.getLocation().add(0, e.getHeight() / 2, 0);
            comp().libertar(e); // componentes e alforje caem no chão
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, de, 60, 0.4, 0.6, 0.4, 0.3);
            // Um rastro de luz até o altar.
            org.bukkit.util.Vector passo = centro.toVector().subtract(de.toVector());
            double dist = passo.length();
            passo.normalize().multiply(0.4);
            Location ponto = de.clone();
            for (double d = 0; d < dist; d += 0.4) {
                w.spawnParticle(Particle.END_ROD, ponto, 1, 0, 0, 0, 0);
                ponto.add(passo);
            }
            e.remove();
        }
        w.spawnParticle(Particle.END_ROD, centro, 50, 0.3, 0.3, 0.3, 0.25);
        w.spawnParticle(Particle.DUST, centro, 60, 0.6, 0.6, 0.6, 0, new Particle.DustOptions(org.bukkit.Color.fromRGB(l.cor()), 1.6f));
        w.playSound(centro, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.4f);
        w.playSound(centro, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.8f);
        est.fase = Fase.OVO;
        est.dono = p.getUniqueId();
        est.entidade = null;
        est.ovo = null;
        est.progresso = 0;
        est.xp = 0;
        salvar();
        for (ItemStack sobra : inv.addItem(ovo(l, 0, 0)).values()) w.dropItem(centro, sobra);
        TextColor cor = TextColor.color(l.cor());
        plugin.getServer().broadcast(Component.text("✦ ", cor).append(Component.text(p.getName(), NamedTextColor.WHITE))
                .append(Component.text(" fundiu dois companheiros e um ", cor))
                .append(Component.text("Ovo do " + l.nome(), cor, TextDecoration.BOLD))
                .append(Component.text(" surgiu no Altar do Domador!", cor)));
        p.sendMessage(Component.text("  " + l.chocar() + " Coloque o ovo no chão.", NamedTextColor.GRAY));
        plugin.diario().marco(p, "ovo_" + l.id(), "Fundiu dois companheiros num Ovo do " + l.nome());
        return null;
    }

    // =====================================================================
    //  O ovo
    // =====================================================================

    public ItemStack ovo(Lendaria l, int xp, int progresso) {
        ItemStack i = new ItemStack(Material.DRAGON_EGG);
        i.editMeta(m -> {
            m.itemName(Component.text("Ovo do " + l.nome() + " " + l.hanzi(), TextColor.color(l.cor()), TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("“" + l.historia() + "”", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true));
            lore.add(Component.empty());
            lore.add(Component.text(l.chocar(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Coloque no chão. Agache e bata nele para pegar de volta.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            if (progresso > 0) {
                lore.add(Component.text("Chocando: " + Math.min(100, progresso * 100 / segundosParaChocar()) + "%", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (xp > 0) {
                lore.add(Component.text("Renasce no nível " + Evolucao.nivelDoXp(xp), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            }
            m.lore(lore);
            m.setMaxStackSize(1);
            m.setEnchantmentGlintOverride(true);
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(K_OVO, PersistentDataType.STRING, l.name());
            pdc.set(K_OVO_XP, PersistentDataType.INTEGER, xp);
            pdc.set(K_OVO_PROGRESSO, PersistentDataType.INTEGER, progresso);
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "ovo_" + l.id());
        });
        return i;
    }

    public static Lendaria doOvo(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_OVO, PersistentDataType.STRING);
        return s == null ? null : Lendaria.porId(s);
    }

    private Lendaria ovoNoBloco(Block b) {
        for (Map.Entry<Lendaria, Estado> en : estados.entrySet()) {
            Location o = en.getValue().ovo;
            if (en.getValue().fase == Fase.OVO && o != null && o.getWorld().equals(b.getWorld())
                    && o.getBlockX() == b.getX() && o.getBlockY() == b.getY() && o.getBlockZ() == b.getZ()) {
                return en.getKey();
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        Lendaria l = doOvo(e.getItemInHand());
        if (l == null) return;
        Estado est = estados.get(l);
        if (est.fase == Fase.VIVA || (est.fase == Fase.OVO && est.ovo != null)) {
            // Cópia de um ovo (criativo, item duplicado): só existe uma.
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("✖ Só existe um " + l.nome() + " no mundo. " + situacao(l) + ".", NamedTextColor.RED));
            return;
        }
        Integer xp = e.getItemInHand().getPersistentDataContainer().get(K_OVO_XP, PersistentDataType.INTEGER);
        Integer prog = e.getItemInHand().getPersistentDataContainer().get(K_OVO_PROGRESSO, PersistentDataType.INTEGER);
        est.fase = Fase.OVO;
        est.dono = e.getPlayer().getUniqueId();
        est.entidade = null;
        est.ovo = e.getBlockPlaced().getLocation();
        est.xp = xp == null ? 0 : xp;
        est.progresso = prog == null ? 0 : prog;
        salvar();
        e.getPlayer().sendMessage(Component.text("✦ O Ovo do " + l.nome() + " está no chão. " + l.chocar(), TextColor.color(l.cor())));
        e.getPlayer().sendMessage(Component.text("  Clique nele para ver quanto falta. Ele choca quando você estiver por perto.", NamedTextColor.GRAY));
    }

    /** Clique no ovo: mostra o progresso (e não deixa ele teleportar, como o ovo de dragão faz). */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarOvo(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.DRAGON_EGG) return;
        Lendaria l = ovoNoBloco(e.getClickedBlock());
        if (l == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Estado est = estados.get(l);
        if (e.getAction() == Action.LEFT_CLICK_BLOCK && p.isSneaking()) {
            if (!p.getUniqueId().equals(est.dono)) {
                p.sendActionBar(Component.text("✖ Esse ovo não é seu.", NamedTextColor.RED));
                return;
            }
            e.getClickedBlock().setType(Material.AIR);
            est.ovo = null;
            salvar();
            for (ItemStack sobra : p.getInventory().addItem(ovo(l, est.xp, est.progresso)).values()) p.getWorld().dropItem(p.getLocation(), sobra);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
            return;
        }
        int pct = Math.min(100, est.progresso * 100 / segundosParaChocar());
        boolean ok = condicao(l, e.getClickedBlock());
        p.sendActionBar(Component.text("✦ Ovo do " + l.nome() + " · " + pct + "% · ", TextColor.color(l.cor()))
                .append(Component.text(ok ? "chocando" : "parado: " + l.chocar(), ok ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoQuebrar(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.DRAGON_EGG && ovoNoBloco(e.getBlock()) != null) e.setCancelled(true);
    }

    /** O ovo de dragão teleporta quando batem nele: o lendário não. */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoTeleportarOvo(BlockFromToEvent e) {
        if (e.getBlock().getType() == Material.DRAGON_EGG && ovoNoBloco(e.getBlock()) != null) e.setCancelled(true);
    }

    /** Não cai (o ovo de dragão tem gravidade). */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoCair(EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof FallingBlock && e.getBlock().getType() == Material.DRAGON_EGG && ovoNoBloco(e.getBlock()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> b.getType() == Material.DRAGON_EGG && ovoNoBloco(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> b.getType() == Material.DRAGON_EGG && ovoNoBloco(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> b.getType() == Material.DRAGON_EGG && ovoNoBloco(b) != null)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoPuxar(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(b -> b.getType() == Material.DRAGON_EGG && ovoNoBloco(b) != null)) e.setCancelled(true);
    }

    /** O ovo jogado no chão não some, não queima e não explode. */
    @EventHandler
    public void aoSurgirItem(ItemSpawnEvent e) {
        if (doOvo(e.getEntity().getItemStack()) == null) return;
        Item i = e.getEntity();
        i.setInvulnerable(true);
        i.setUnlimitedLifetime(true);
        i.setGlowing(true);
    }

    /** O nome de uma lendária não muda (o modelo 3D, com os mods, é escolhido pelo nome). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoRenomear(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        if (Lendaria.de(e.getRightClicked()) == null) return;
        ItemStack mao = e.getPlayer().getInventory().getItem(e.getHand());
        if (mao.getType() != Material.NAME_TAG) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(Component.text("✖ O nome de uma lendária não muda.", NamedTextColor.RED));
    }

    /** O lugar está bom para chocar? */
    private boolean condicao(Lendaria l, Block ovo) {
        return switch (l) {
            case QILIN -> {
                long t = ovo.getWorld().getTime();
                if (t > 12300 && t < 23800) yield false;
                int flores = 0;
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -4; dz <= 4; dz++) {
                            if (Tag.FLOWERS.isTagged(ovo.getRelative(dx, dy, dz).getType()) && ++flores >= 8) yield true;
                        }
                    }
                }
                yield false;
            }
            case FENGHUANG -> {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            Block b = ovo.getRelative(dx, dy, dz);
                            Material m = b.getType();
                            if (m == Material.LAVA || m == Material.FIRE || m == Material.SOUL_FIRE || m == Material.MAGMA_BLOCK) yield true;
                            if ((m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE) && b.getBlockData() instanceof Lightable lt && lt.isLit()) yield true;
                        }
                    }
                }
                yield false;
            }
            case LONGMA -> contarPerto(ovo, 2, 1, m -> m == Material.WATER) >= 6;
            case BAIHU -> ovo.getTemperature() <= 0.15
                    || contarPerto(ovo, 3, 1, m -> m == Material.SNOW || m == Material.SNOW_BLOCK || m == Material.POWDER_SNOW) >= 6;
            case XUANWU -> {
                long t = ovo.getWorld().getTime();
                yield t > 12500 && t < 23500 && contarPerto(ovo, 2, 1, m -> m == Material.SAND || m == Material.RED_SAND) >= 6;
            }
        };
    }

    private static int contarPerto(Block centro, int r, int ry, java.util.function.Predicate<Material> tipo) {
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -r; dz <= r; dz++) if (tipo.test(centro.getRelative(dx, dy, dz).getType())) n++;
            }
        }
        return n;
    }

    // =====================================================================
    //  A cada 2 s: ovos chocando e poderes das lendárias
    // =====================================================================

    public void tick() {
        ciclo++;
        if (ciclo % 5 == 0) chocar(10);
        for (Map.Entry<Lendaria, Estado> en : estados.entrySet()) {
            Estado est = en.getValue();
            if (est.fase != Fase.VIVA || est.entidade == null) continue;
            if (Bukkit.getEntity(est.entidade) instanceof LivingEntity e && e.isValid()) poderes(en.getKey(), e);
        }
    }

    private void chocar(int segundos) {
        boolean mudou = false;
        for (Map.Entry<Lendaria, Estado> en : estados.entrySet()) {
            Lendaria l = en.getKey();
            Estado est = en.getValue();
            if (est.fase != Fase.OVO || est.ovo == null || !est.ovo.isChunkLoaded()) continue;
            Block b = est.ovo.getBlock();
            if (b.getType() != Material.DRAGON_EGG) {
                // Sumiu de algum jeito (comando, mundo editado): volta como item para o dono.
                est.ovo = null;
                mudou = true;
                Player d = est.dono == null ? null : Bukkit.getPlayer(est.dono);
                if (d != null) {
                    for (ItemStack sobra : d.getInventory().addItem(ovo(l, est.xp, est.progresso)).values()) d.getWorld().dropItem(d.getLocation(), sobra);
                    d.sendMessage(Component.text("✦ O Ovo do " + l.nome() + " voltou para você.", TextColor.color(l.cor())));
                }
                continue;
            }
            Location centro = b.getLocation().add(0.5, 0.6, 0.5);
            if (condicao(l, b)) {
                est.progresso += segundos;
                mudou = true;
                b.getWorld().spawnParticle(particulaDoOvo(l), centro, 6, 0.3, 0.3, 0.3, 0.01);
            } else {
                b.getWorld().spawnParticle(Particle.SMOKE, centro, 2, 0.2, 0.2, 0.2, 0.01);
            }
            if (est.progresso < segundosParaChocar()) continue;
            Player d = est.dono == null ? null : Bukkit.getPlayer(est.dono);
            if (d == null || !d.getWorld().equals(b.getWorld()) || d.getLocation().distanceSquared(centro) > RAIO_DONO * RAIO_DONO) {
                b.getWorld().spawnParticle(Particle.END_ROD, centro, 4, 0.3, 0.4, 0.3, 0.02);
                continue; // pronto: espera o dono chegar
            }
            nascer(l, d, centro);
            mudou = true;
        }
        if (mudou) salvar();
    }

    /** O ovo choca (ou o admin manda nascer): a lendária nasce como companheira do jogador. */
    public LivingEntity nascer(Lendaria l, Player dono, Location onde) {
        Estado est = estados.get(l);
        if (est.ovo != null && est.ovo.getBlock().getType() == Material.DRAGON_EGG) est.ovo.getBlock().setType(Material.AIR);
        World w = onde.getWorld();
        LivingEntity e = (LivingEntity) w.spawnEntity(onde, l.base(), org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, x -> {
            if (x instanceof org.bukkit.entity.Ageable a) a.setAdult();
            if (x instanceof Horse h) {
                h.setColor(l == Lendaria.FENGHUANG ? Horse.Color.CHESTNUT : l == Lendaria.LONGMA ? Horse.Color.GRAY : Horse.Color.WHITE);
                h.setStyle(Horse.Style.NONE);
            }
            if (x instanceof org.bukkit.entity.Wolf lobo) lobo.setVariant(org.bukkit.entity.Wolf.Variant.SNOWY);
            x.getPersistentDataContainer().set(Lendaria.CHAVE, PersistentDataType.STRING, l.name());
            if (est.xp > 0) x.getPersistentDataContainer().set(Evolucao.K_XP, PersistentDataType.INTEGER, est.xp);
            x.customName(Component.text(l.nome() + " " + l.hanzi(), TextColor.color(l.cor()), TextDecoration.BOLD));
            x.setCustomNameVisible(true);
        });
        String erro = comp().vincular(dono, e, true);
        if (erro != null) plugin.getLogger().warning("Lendária " + l.nome() + ": " + erro);
        est.fase = Fase.VIVA;
        est.dono = dono.getUniqueId();
        est.entidade = e.getUniqueId();
        est.ovo = null;
        est.progresso = 0;
        est.xp = 0;
        salvar();
        if (l == Lendaria.FENGHUANG) {
            w.spawnParticle(Particle.FLAME, onde, 120, 0.8, 1, 0.8, 0.15);
            w.spawnParticle(Particle.LAVA, onde, 20, 0.6, 0.6, 0.6, 0);
            w.playSound(onde, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);
        } else {
            w.spawnParticle(Particle.CHERRY_LEAVES, onde, 80, 1.2, 1, 1.2, 0.02);
            w.spawnParticle(Particle.HAPPY_VILLAGER, onde, 40, 1, 1, 1, 0);
            w.playSound(onde, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.7f);
        }
        w.playSound(onde, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.6f);
        w.playSound(onde, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        TextColor cor = TextColor.color(l.cor());
        plugin.getServer().broadcast(Component.text("✦ O " + l.nome() + " " + l.hanzi() + " nasceu! ", cor, TextDecoration.BOLD)
                .append(Component.text("Ele agora vive com " + dono.getName() + ".", cor).decoration(TextDecoration.BOLD, false)));
        for (String linha : l.descricao()) dono.sendMessage(Component.text("  • " + linha, NamedTextColor.GRAY));
        plugin.diario().marco(dono, "lendaria_" + l.id(), "Chocou o " + l.nome());
        return e;
    }

    // =====================================================================
    //  Morte, liberdade e poderes
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        Lendaria l = Lendaria.de(e.getEntity());
        if (l == null) return;
        Estado est = estados.get(l);
        if (!e.getEntity().getUniqueId().equals(est.entidade)) return;
        TextColor cor = TextColor.color(l.cor());
        Location onde = e.getEntity().getLocation();
        if (l == Lendaria.FENGHUANG) {
            // Renascer: vira um ovo em chamas, com o mesmo nível.
            est.fase = Fase.OVO;
            est.entidade = null;
            est.ovo = null;
            est.progresso = 0;
            est.xp = Evolucao.xp(e.getEntity());
            e.getDrops().add(ovo(l, est.xp, 0));
            onde.getWorld().spawnParticle(Particle.FLAME, onde.clone().add(0, 1, 0), 120, 0.6, 1, 0.6, 0.12);
            onde.getWorld().playSound(onde, Sound.ENTITY_BLAZE_DEATH, 1f, 0.6f);
            plugin.getServer().broadcast(Component.text("🔥 O Fenghuang virou cinzas... e das cinzas ficou um ovo em chamas.", cor));
        } else {
            est.fase = Fase.LIVRE;
            est.dono = null;
            est.entidade = null;
            plugin.getServer().broadcast(Component.text("☠ O " + l.nome() + " morreu. Ele só volta com uma nova fusão no Altar do Domador.", cor));
        }
        salvar();
    }

    /** Chamado pelo Companheiros ao libertar: a lendária volta para a natureza (some). */
    void aoLibertar(LivingEntity e) {
        Lendaria l = Lendaria.de(e);
        if (l == null) return;
        Estado est = estados.get(l);
        est.fase = Fase.LIVRE;
        est.dono = null;
        est.entidade = null;
        salvar();
        Location c = e.getLocation().add(0, e.getHeight() / 2, 0);
        c.getWorld().spawnParticle(Particle.END_ROD, c, 60, 0.6, 0.8, 0.6, 0.08);
        c.getWorld().playSound(c, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
        e.remove();
        plugin.getServer().broadcast(Component.text("✦ O " + l.nome() + " foi libertado e voltou para a natureza.", TextColor.color(l.cor())));
    }

    /** Admin: volta a lendária ao começo (some do mundo, o ovo posto some). */
    public void reiniciar(Lendaria l) {
        Estado est = estados.get(l);
        if (est.entidade != null && Bukkit.getEntity(est.entidade) instanceof LivingEntity e) {
            comp().libertar(e);
            e.remove();
        }
        if (est.ovo != null && est.ovo.isChunkLoaded() && est.ovo.getBlock().getType() == Material.DRAGON_EGG) est.ovo.getBlock().setType(Material.AIR);
        estados.put(l, new Estado());
        salvar();
    }

    /** Admin: o ovo na mão (a lendária passa a estar "em ovo" com esse jogador). */
    public ItemStack darOvo(Lendaria l, Player p) {
        reiniciar(l);
        Estado est = estados.get(l);
        est.fase = Fase.OVO;
        est.dono = p.getUniqueId();
        salvar();
        return ovo(l, 0, 0);
    }

    private void poderes(Lendaria l, LivingEntity e) {
        World w = e.getWorld();
        Player dono = Bukkit.getPlayer(Companheiros.dono(e));
        Location pe = e.getLocation();
        switch (l) {
            case QILIN -> {
                // Bênção: dono e party perto se curam devagar.
                if (dono != null && dono.getWorld().equals(w)) {
                    Party pt = plugin.parties().party(dono);
                    for (Player p : w.getNearbyPlayers(pe, 8)) {
                        if (!p.equals(dono) && (pt == null || !pt.tem(p.getUniqueId()))) continue;
                        if (!p.hasPotionEffect(PotionEffectType.REGENERATION)) p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 0, true, false, true));
                    }
                }
                // Flores onde passa (só onde o dono pode construir).
                Block chao = pe.getBlock().getRelative(0, -1, 0);
                if (ThreadLocalRandom.current().nextDouble() < 0.5 && chao.getType() == Material.GRASS_BLOCK && pe.getBlock().getType().isAir()
                        && dono != null && plugin.territorios().podeConstruir(dono.getUniqueId(), pe)) {
                    Material[] flores = {Material.POPPY, Material.DANDELION, Material.CORNFLOWER, Material.OXEYE_DAISY, Material.AZURE_BLUET, Material.ALLIUM};
                    pe.getBlock().setType(flores[ThreadLocalRandom.current().nextInt(flores.length)]);
                }
                for (Entity p : e.getPassengers()) {
                    if (p instanceof Player pl) pl.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 80, 0, true, false, true));
                }
            }
            case FENGHUANG -> {
                for (Entity p : e.getPassengers()) {
                    if (p instanceof Player pl) pl.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 80, 0, true, false, true));
                }
                if (!e.isOnGround()) w.spawnParticle(Particle.FLAME, pe.clone().add(0, e.getHeight() * 0.4, 0), 8, 0.4, 0.2, 0.4, 0.01);
            }
            case LONGMA -> {
                // Onda: montado, a cada 4 s empurra os monstros em volta.
                if (e.getPassengers().isEmpty() || ciclo % 2 != 0) return;
                boolean empurrou = false;
                for (LivingEntity m : w.getNearbyLivingEntities(pe, 5)) {
                    if (!(m instanceof org.bukkit.entity.Enemy) || Companheiros.eh(m)) continue;
                    org.bukkit.util.Vector fora = m.getLocation().toVector().subtract(pe.toVector()).setY(0);
                    if (fora.lengthSquared() < 0.01) fora = new org.bukkit.util.Vector(1, 0, 0);
                    m.setVelocity(fora.normalize().multiply(1.2).setY(0.45));
                    empurrou = true;
                }
                if (empurrou) {
                    w.spawnParticle(Particle.SPLASH, pe.clone().add(0, 0.5, 0), 80, 2.5, 0.3, 2.5, 0.2);
                    w.playSound(pe, Sound.ENTITY_GENERIC_SPLASH, 1f, 0.8f);
                }
            }
            case BAIHU -> {
                // Rugido: a cada 6 s, monstros perto ficam fracos e lentos.
                if (ciclo % 3 != 0) return;
                boolean rugiu = false;
                for (LivingEntity m : w.getNearbyLivingEntities(pe, 7)) {
                    if (!(m instanceof org.bukkit.entity.Enemy) || Companheiros.eh(m)) continue;
                    m.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
                    m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                    rugiu = true;
                }
                if (rugiu) w.playSound(pe, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
            }
            case XUANWU -> {
                // Escudo: dono e party perto recebem menos dano.
                if (dono == null || !dono.getWorld().equals(w)) return;
                Party pt = plugin.parties().party(dono);
                for (Player p : w.getNearbyPlayers(pe, 7)) {
                    if (!p.equals(dono) && (pt == null || !pt.tem(p.getUniqueId()))) continue;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                }
            }
        }
    }

    private static Particle particulaDoOvo(Lendaria l) {
        return switch (l) {
            case QILIN -> Particle.HAPPY_VILLAGER;
            case FENGHUANG -> Particle.FLAME;
            case LONGMA -> Particle.SPLASH;
            case BAIHU -> Particle.SNOWFLAKE;
            case XUANWU -> Particle.SQUID_INK;
        };
    }

    /** Qilin (água e lava) e Longma (água): ficam na superfície (chamado ao guiar a montaria). */
    public static boolean andaSobreLiquido(LivingEntity e) {
        Lendaria l = Lendaria.de(e);
        return (l == Lendaria.QILIN && (e.isInWater() || e.isInLava())) || (l == Lendaria.LONGMA && e.isInWater());
    }
}
