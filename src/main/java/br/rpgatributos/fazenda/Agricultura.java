package br.rpgatributos.fazenda;

import br.rpgatributos.MarcadorDeBlocos;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.arcano.BlocosTemporarios;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Plantações: XP de Agricultura, qualidade das colheitas, colheita farta, toque verde,
 * colheita gigante, sementes mutantes (variedades), adubo e espantalho.
 */
public final class Agricultura implements Listener {

    /** Plantações com idade (as que contam como "maduras" no último estágio). */
    private static final Set<Material> PLANTACOES = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART, Material.COCOA);
    /** O que a colheita dá (recebe qualidade, conta para colheita farta e gigante). */
    private static final Set<Material> PRODUTOS = Set.of(
            Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.NETHER_WART,
            Material.COCOA_BEANS, Material.MELON_SLICE, Material.PUMPKIN, Material.SWEET_BERRIES, Material.GLOW_BERRIES);
    private static final double RAIO_ESPANTALHO = 10;

    /** O que aconteceu na quebra, para o drop (que vem logo depois) saber o que fazer. */
    private record Colheita(Player jogador, boolean madura, Variedade variedade, boolean adubada) {}

    private final RPGAtributos plugin;
    private final MarcadorDeBlocos colocados;
    private final MarcadorDeBlocos adubados;
    private final Map<Variedade, MarcadorDeBlocos> variedades = new EnumMap<>(Variedade.class);
    private final Map<Block, Colheita> colheitas = new HashMap<>();
    private final NamespacedKey kAdubo;

    public Agricultura(RPGAtributos plugin) {
        this.plugin = plugin;
        this.colocados = new MarcadorDeBlocos(plugin, "colocados"); // a mesma marca usada pela Mineração
        this.adubados = new MarcadorDeBlocos(plugin, "adubados");
        for (Variedade v : Variedade.values()) variedades.put(v, new MarcadorDeBlocos(plugin, "var_" + v.id()));
        this.kAdubo = new NamespacedKey(plugin, "adubo");
    }

    private Settings cfg() { return plugin.settings(); }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static boolean ganhaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    private double fracao(Player p) {
        return plugin.stats().getNivel(p, Skill.AGRICULTURA) / (double) cfg().nivelMaximo;
    }

    private static boolean madura(Block b) {
        return b.getBlockData() instanceof Ageable a && a.getAge() >= a.getMaximumAge();
    }

    // =====================================================================
    //  Adubo (receita de bancada comum)
    // =====================================================================

    public ItemStack adubo(int qtd) {
        ItemStack i = new ItemStack(Material.BROWN_DYE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Adubo Rico", NamedTextColor.GOLD));
            m.lore(List.of(Component.text("Clique numa plantação: a próxima", NamedTextColor.GRAY),
                            Component.text("colheita dela sai com mais qualidade.", NamedTextColor.GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(kAdubo, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    public boolean ehAdubo(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(kAdubo);
    }

    /** 3 farinhas de osso + 1 carne podre + 1 terra = 4 Adubos Ricos. */
    public void registrarReceitas() {
        NamespacedKey chave = new NamespacedKey(plugin, "adubo_rico");
        if (Bukkit.getRecipe(chave) != null) return;
        ShapelessRecipe r = new ShapelessRecipe(chave, adubo(4));
        r.addIngredient(3, Material.BONE_MEAL);
        r.addIngredient(Material.ROTTEN_FLESH);
        r.addIngredient(Material.DIRT);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoAdubar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || !ehAdubo(e.getItem())) return;
        Block b = e.getClickedBlock();
        if (b == null || !PLANTACOES.contains(b.getType())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!plugin.territorios().podeConstruir(p, b)) return; // plantação do território de outro
        if (adubados.marcado(b)) {
            p.sendActionBar(Component.text("Essa plantação já está adubada.", NamedTextColor.GRAY));
            return;
        }
        adubados.marcar(b);
        if (p.getGameMode() != GameMode.CREATIVE) e.getItem().setAmount(e.getItem().getAmount() - 1);
        b.getWorld().spawnParticle(Particle.COMPOSTER, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.2, 0.3);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1f, 1f);
    }

    // =====================================================================
    //  Plantar
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPlantar(BlockPlaceEvent e) {
        if (BlocosTemporarios.simulando()) return; // teste de proteção das magias
        Block b = e.getBlockPlaced();
        Material t = b.getType();
        if (t == Material.MELON || t == Material.PUMPKIN || t == Material.CARVED_PUMPKIN) {
            colocados.marcar(b); // melancia/abóbora colocada não dá XP ao quebrar
            return;
        }
        if (!PLANTACOES.contains(t)) return;
        // Limpa marcas velhas e marca a variedade, se a semente for mutante.
        for (MarcadorDeBlocos m : variedades.values()) m.desmarcar(b);
        adubados.desmarcar(b);
        Variedade v = Variedade.de(e.getItemInHand());
        if (v != null && v.planta() == t) variedades.get(v).marcar(b);
    }

    private Variedade variedadeDe(Block b) {
        for (Map.Entry<Variedade, MarcadorDeBlocos> en : variedades.entrySet()) {
            if (en.getKey().planta() == b.getType() && en.getValue().marcado(b)) return en.getKey();
        }
        return null;
    }

    // =====================================================================
    //  Colher
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        Material t = b.getType();
        Player p = e.getPlayer();
        if (PLANTACOES.contains(t)) {
            boolean madura = madura(b);
            Variedade v = variedadeDe(b);
            boolean adubada = adubados.desmarcar(b);
            if (v != null) variedades.get(v).desmarcar(b);
            colheitas.put(b, new Colheita(p, madura, v, adubada));
            plugin.getServer().getScheduler().runTask(plugin, () -> colheitas.remove(b));
            if (madura && ganhaXp(p)) {
                double xp = switch (t) {
                    case NETHER_WART, COCOA -> 2;
                    default -> 1.5;
                };
                plugin.stats().darXp(p, Skill.AGRICULTURA, v != null ? xp * 3 : xp);
            }
        } else if (t == Material.MELON || t == Material.PUMPKIN) {
            boolean natural = !colocados.desmarcar(b);
            if (!natural) return;
            colheitas.put(b, new Colheita(p, true, null, false));
            plugin.getServer().getScheduler().runTask(plugin, () -> colheitas.remove(b));
            if (ganhaXp(p)) plugin.stats().darXp(p, Skill.AGRICULTURA, 3);
        } else if (t == Material.CARVED_PUMPKIN) {
            colocados.desmarcar(b); // abóbora colocada que foi esculpida com tesoura
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void aoCairColheita(BlockDropItemEvent e) {
        Colheita c = colheitas.remove(e.getBlock());
        if (c == null) return;
        List<Item> itens = e.getItems();
        Location centro = e.getBlock().getLocation().toCenterLocation();

        // Variedade: a colheita vira o produto especial (e devolve sementes da variedade).
        if (c.variedade() != null) {
            Variedade v = c.variedade();
            itens.clear(); // tira o drop comum (os itens ainda não apareceram no mundo)
            World w = e.getBlock().getWorld();
            if (c.madura()) {
                Qualidade q = qualidade(c);
                ItemStack produto = v.produto(rnd().nextInt(1, 3));
                q.aplicar(produto);
                w.dropItemNaturally(centro, produto);
                w.dropItemNaturally(centro, v.semente(rnd().nextInt(1, 3)));
                w.spawnParticle(Particle.HAPPY_VILLAGER, centro, 15, 0.3, 0.3, 0.3);
            } else {
                w.dropItemNaturally(centro, v.semente(1));
            }
            return;
        }
        if (!c.madura()) return;

        Qualidade q = qualidade(c);
        double fracao = fracao(c.jogador());
        boolean dupla = rnd().nextDouble() < Math.min(1, plugin.stats().getNivel(c.jogador(), Skill.AGRICULTURA) * cfg().agrChanceDupla);
        boolean gigante = rnd().nextDouble() < 0.002 + cfg().agrChanceGigante * fracao;
        List<ItemStack> extras = new ArrayList<>();
        ItemStack exemplo = null;
        for (Item i : itens) {
            ItemStack s = i.getItemStack();
            if (!PRODUTOS.contains(s.getType())) continue;
            if (gigante) s.setAmount(Math.min(s.getMaxStackSize(), s.getAmount() * 8));
            q.aplicar(s);
            i.setItemStack(s);
            if (exemplo == null) exemplo = s;
            if (dupla) extras.add(s.clone());
        }
        for (ItemStack s : extras) e.getBlock().getWorld().dropItemNaturally(centro, s);
        if (gigante) colheitaGigante(c.jogador(), centro, exemplo);
    }

    /** Frutas vermelhas e brilhantes (colhidas com clique direito). */
    @EventHandler(ignoreCancelled = true)
    public void aoColherFrutas(PlayerHarvestBlockEvent e) {
        Player p = e.getPlayer();
        if (ganhaXp(p)) plugin.stats().darXp(p, Skill.AGRICULTURA, 1);
        Qualidade q = Qualidade.sortear(fracao(p));
        boolean dupla = rnd().nextDouble() < Math.min(1, plugin.stats().getNivel(p, Skill.AGRICULTURA) * cfg().agrChanceDupla);
        List<ItemStack> extras = new ArrayList<>();
        for (ItemStack s : e.getItemsHarvested()) {
            if (!PRODUTOS.contains(s.getType())) continue;
            q.aplicar(s);
            if (dupla) extras.add(s.clone());
        }
        e.getItemsHarvested().addAll(extras);
    }

    private Qualidade qualidade(Colheita c) {
        Qualidade q = Qualidade.sortear(fracao(c.jogador()));
        if (c.adubada()) q = q == Qualidade.NORMAL ? Qualidade.BOA : q.acima();
        return q;
    }

    private void colheitaGigante(Player p, Location centro, ItemStack exemplo) {
        World w = centro.getWorld();
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, centro, 40, 0.5, 0.5, 0.5, 0.3);
        w.playSound(centro, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.6f);
        p.sendActionBar(Component.text("✦ COLHEITA GIGANTE! ✦", NamedTextColor.GOLD, TextDecoration.BOLD));
        if (exemplo == null) return;
        // Uma versão enorme da colheita "estoura" no lugar.
        ItemDisplay d = w.spawn(centro, ItemDisplay.class, it -> {
            ItemStack visual = exemplo.clone();
            visual.setAmount(1);
            it.setItemStack(visual);
            it.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.5f), new AxisAngle4f()));
            it.setPersistent(false);
        });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(12);
            d.setTransformation(new Transformation(new Vector3f(0, 1, 0), new AxisAngle4f(), new Vector3f(3f), new AxisAngle4f()));
        }, 2L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            w.spawnParticle(Particle.POOF, centro.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.05);
            d.remove();
        }, 30L);
    }

    // =====================================================================
    //  Sementes mutantes
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCrescer(BlockGrowEvent e) {
        Block b = e.getBlock();
        if (!(e.getNewState().getBlockData() instanceof Ageable a) || a.getAge() < a.getMaximumAge()) return;
        Material tipo = e.getNewState().getType();
        if (!PLANTACOES.contains(tipo) || variedadeDe(b) != null) return;
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block vizinho = b.getRelative(f);
            if (vizinho.getType() == tipo || !madura(vizinho)) continue;
            for (Variedade v : Variedade.values()) {
                if (!v.cruzamento(tipo, vizinho.getType()) || rnd().nextDouble() >= cfg().agrChanceMutacao) continue;
                Location l = b.getLocation().toCenterLocation();
                b.getWorld().dropItemNaturally(l, v.semente(1));
                b.getWorld().spawnParticle(Particle.END_ROD, l, 20, 0.3, 0.4, 0.3, 0.03);
                b.getWorld().playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
                for (Player p : b.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(l) > 24 * 24) continue;
                    p.sendMessage(Component.text("✦ Um cruzamento gerou uma ", NamedTextColor.GREEN)
                            .append(Component.text("Semente de " + v.nome(), v.cor(), TextDecoration.BOLD))
                            .append(Component.text("! Ela caiu na plantação.", NamedTextColor.GREEN)));
                    plugin.titulos().registrar(p, "mutacoes", 1);
                }
                return;
            }
        }
    }

    // =====================================================================
    //  Toque verde (a cada 4 segundos)
    // =====================================================================

    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!ganhaXp(p)) continue;
            double chance = plugin.stats().getNivel(p, Skill.AGRICULTURA) * cfg().agrToqueVerde;
            if (chance <= 0) continue;
            Block centro = p.getLocation().getBlock();
            for (int i = 0; i < 8; i++) {
                Block b = centro.getRelative(rnd().nextInt(-5, 6), rnd().nextInt(-2, 2), rnd().nextInt(-5, 6));
                if (!PLANTACOES.contains(b.getType()) || !(b.getBlockData() instanceof Ageable a)
                        || a.getAge() >= a.getMaximumAge() || rnd().nextDouble() >= chance) continue;
                a.setAge(a.getAge() + 1);
                b.setBlockData(a);
                b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2);
            }
        }
    }

    // =====================================================================
    //  Espantalho: suporte de armadura com abóbora na cabeça protege a plantação
    // =====================================================================

    private static boolean pertoDeEspantalho(Block b) {
        for (Entity e : b.getWorld().getNearbyEntities(b.getLocation(), RAIO_ESPANTALHO, 4, RAIO_ESPANTALHO)) {
            if (!(e instanceof ArmorStand s) || s.getEquipment() == null) continue;
            Material cabeca = s.getEquipment().getHelmet().getType();
            if (cabeca == Material.CARVED_PUMPKIN || cabeca == Material.JACK_O_LANTERN) return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPisarJogador(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.FARMLAND && pertoDeEspantalho(e.getClickedBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPisarCriatura(EntityInteractEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND && pertoDeEspantalho(e.getBlock())) e.setCancelled(true);
    }
}
