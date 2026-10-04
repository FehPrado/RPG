package br.rpgatributos.detalhes;

import br.rpgatributos.MarcadorDeBlocos;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Itens pequenos da versão "Detalhes":
 * - Ferradura (bancada: 5 lingotes de ferro em U): num companheiro seu, +15% de velocidade e
 *   +10% de pulo, sem gastar espaço de componente. Volta quando ele é libertado ou morre.
 * - Ninho de Pássaro: às vezes cai ao quebrar folhas naturais. Clique com ele para abrir:
 *   ovos, penas, sementes e, raramente, pepitas ou uma esmeralda.
 */
public final class ItensDetalhes implements Listener {

    public static final TextColor COR = TextColor.color(0xB0855A);
    public static final NamespacedKey K_FERRADURA = new NamespacedKey("rpgatributos", "ferradura");
    public static final NamespacedKey K_NINHO = new NamespacedKey("rpgatributos", "ninho");
    /** No companheiro: tem ferradura (INTEGER 1, para ser copiado quando ele se transforma). */
    public static final NamespacedKey K_TEM_FERRADURA = new NamespacedKey("rpgatributos", "companheiro_ferradura");

    private final RPGAtributos plugin;
    private final NamespacedKey mVelocidade, mPulo, kReceita;
    private final MarcadorDeBlocos folhasColocadas;

    public ItensDetalhes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.mVelocidade = new NamespacedKey(plugin, "ferradura_velocidade");
        this.mPulo = new NamespacedKey(plugin, "ferradura_pulo");
        this.kReceita = new NamespacedKey(plugin, "ferradura");
        this.folhasColocadas = new MarcadorDeBlocos(plugin, "folhas_colocadas");
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    public static ItemStack ferradura(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Ferradura", COR));
            m.setItemModel(NamespacedKey.minecraft("iron_nugget"));
            m.setMaxStackSize(16);
            m.lore(List.of(Component.text("Clique num companheiro seu (cavalo, burro,", NamedTextColor.GRAY),
                            Component.text("camelo...): +15% de velocidade e +10% de pulo.", NamedTextColor.GRAY),
                            Component.text("Não ocupa espaço de componente.", NamedTextColor.DARK_GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_FERRADURA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "ferradura");
        });
        return i;
    }

    public static ItemStack ninho(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Ninho de Pássaro", COR));
            m.setItemModel(NamespacedKey.minecraft("bowl"));
            m.lore(List.of(Component.text("Caiu de uma árvore. Clique com ele", NamedTextColor.GRAY),
                            Component.text("na mão para ver o que tem dentro.", NamedTextColor.GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_NINHO, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "ninho_de_passaro");
        });
        return i;
    }

    public static boolean ehFerradura(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_FERRADURA);
    }

    public static boolean ehNinho(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_NINHO);
    }

    public void registrarReceita() {
        if (Bukkit.getRecipe(kReceita) != null) return;
        ShapedRecipe r = new ShapedRecipe(kReceita, ferradura(1));
        r.shape("I I", "I I", " I ");
        r.setIngredient('I', Material.IRON_INGOT);
        Bukkit.addRecipe(r);
    }

    // =====================================================================
    //  Ferradura
    // =====================================================================

    public static boolean temFerradura(LivingEntity e) {
        return e.getPersistentDataContainer().has(K_TEM_FERRADURA);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerrar(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (!ehFerradura(mao) || !(e.getRightClicked() instanceof LivingEntity alvo)) return;
        e.setCancelled(true);
        if (!Companheiros.eh(alvo) || !plugin.companheiros().eDono(p, alvo)) {
            p.sendActionBar(Component.text("✖ A ferradura vai num companheiro seu (/pets).", NamedTextColor.RED));
            return;
        }
        if (temFerradura(alvo)) {
            p.sendActionBar(Component.text("✖ " + Companheiros.nome(alvo) + " já tem ferradura.", NamedTextColor.RED));
            return;
        }
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        alvo.getPersistentDataContainer().set(K_TEM_FERRADURA, PersistentDataType.INTEGER, 1);
        aplicar(alvo);
        alvo.getWorld().playSound(alvo.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.5f);
        alvo.getWorld().spawnParticle(Particle.CRIT, alvo.getLocation().add(0, 0.3, 0), 20, 0.4, 0.1, 0.4, 0.1);
        p.sendMessage(Component.text("♞ " + Companheiros.nome(alvo) + " ganhou ferraduras: +15% de velocidade e +10% de pulo.", COR));
        plugin.stats().darXp(p, Skill.DOMA, 10);
    }

    /** Põe (ou tira, se ele não tiver ferradura) os bônus da ferradura. */
    public void aplicar(LivingEntity e) {
        boolean tem = temFerradura(e);
        mod(e, Attribute.MOVEMENT_SPEED, mVelocidade, tem ? 0.15 : 0);
        mod(e, Attribute.JUMP_STRENGTH, mPulo, tem ? 0.10 : 0);
    }

    private static void mod(LivingEntity e, Attribute a, NamespacedKey k, double v) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(k);
        if (v != 0) inst.addModifier(new AttributeModifier(k, v, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
    }

    /** O companheiro foi libertado ou morreu: a ferradura cai no chão. */
    public void soltarFerradura(LivingEntity e) {
        if (!temFerradura(e)) return;
        e.getPersistentDataContainer().remove(K_TEM_FERRADURA);
        aplicar(e);
        e.getWorld().dropItemNaturally(e.getLocation(), ferradura(1));
    }

    // =====================================================================
    //  Ninho de Pássaro
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocarFolha(BlockPlaceEvent e) {
        if (Tag.LEAVES.isTagged(e.getBlockPlaced().getType())) folhasColocadas.marcar(e.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrarFolha(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!Tag.LEAVES.isTagged(b.getType())) return;
        if (folhasColocadas.desmarcar(b)) return;
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        if (ThreadLocalRandom.current().nextDouble() >= plugin.settings().detNinhoChance) return;
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), ninho(1));
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 0.8f, 1.2f);
        p.sendActionBar(Component.text("🪺 Um ninho caiu da árvore!", COR));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoAbrirNinho(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (!ehNinho(mao)) return;
        e.setCancelled(true);
        mao.setAmount(mao.getAmount() - 1);
        List<ItemStack> achados = conteudoNinho();
        for (ItemStack i : achados) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1.1f);
        p.getWorld().spawnParticle(Particle.ITEM, p.getLocation().add(0, 1.2, 0), 10, 0.2, 0.2, 0.2, 0.05, new ItemStack(Material.WHEAT_SEEDS));
        plugin.stats().darXp(p, Skill.AGRICULTURA, 4);
        p.sendActionBar(Component.text("🪺 No ninho: " + achados.size() + " achado(s)", COR));
    }

    private static List<ItemStack> conteudoNinho() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> l = new ArrayList<>();
        if (r.nextDouble() < 0.6) {
            Material[] ovos = {Material.EGG, Material.BROWN_EGG, Material.BLUE_EGG};
            l.add(new ItemStack(ovos[r.nextInt(ovos.length)], 1 + r.nextInt(2)));
        }
        if (r.nextDouble() < 0.5) l.add(new ItemStack(Material.FEATHER, 1 + r.nextInt(3)));
        if (r.nextDouble() < 0.35) {
            Material[] sementes = {Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS};
            l.add(new ItemStack(sementes[r.nextInt(sementes.length)], 1 + r.nextInt(3)));
        }
        if (r.nextDouble() < 0.05) l.add(new ItemStack(r.nextBoolean() ? Material.TORCHFLOWER_SEEDS : Material.PITCHER_POD));
        if (r.nextDouble() < 0.04) l.add(new ItemStack(Material.GOLD_NUGGET, 2 + r.nextInt(3)));
        if (r.nextDouble() < 0.015) l.add(new ItemStack(Material.EMERALD));
        if (l.isEmpty()) l.add(new ItemStack(Material.STICK, 2));
        return l;
    }
}
