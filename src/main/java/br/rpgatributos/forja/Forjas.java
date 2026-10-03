package br.rpgatributos.forja;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A Forja do Ferreiro: uma bigorna onde se jogou 2 blocos de ferro e 1 balde de
 * lava. Clique direito abre a bancada do ferreiro, a única que forja itens.
 */
public final class Forjas extends Estacao {

    private static final Set<Material> BIGORNAS = EnumSet.of(
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    private static final int BLOCOS_DE_FERRO = 2;
    private static final double ALCANCE = 8;

    /** Quem está com a bancada de uma forja aberta → qual forja. */
    private final Map<UUID, Location> abertas = new HashMap<>();

    public Forjas(RPGAtributos plugin) {
        super(plugin, "forjas", "forja_display");
    }

    /** O jogador está fabricando numa Forja do Ferreiro? */
    public boolean naForja(Player p, Inventory inv) {
        if (abertas.containsKey(p.getUniqueId()) && p.getOpenInventory().getTopInventory().getType() == InventoryType.WORKBENCH) {
            return true;
        }
        Location l = inv == null ? null : inv.getLocation();
        return l != null && eh(l.getBlock());
    }

    // =====================================================================
    //  Ritual: 2 blocos de ferro + 1 balde de lava em cima da bigorna
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return BIGORNAS.contains(m);
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.IRON_BLOCK || m == Material.LAVA_BUCKET;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return BIGORNAS.contains(abaixo.getType()) ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.IRON_BLOCK) < BLOCOS_DE_FERRO || contar(itens, Material.LAVA_BUCKET) < 1) return false;
        tirar(itens, Material.IRON_BLOCK, BLOCOS_DE_FERRO);
        tirar(itens, Material.LAVA_BUCKET, 1);
        // A lava vai para a forja; o balde volta vazio.
        bloco.getWorld().dropItemNaturally(bloco.getLocation().add(0.5, 1.2, 0.5), new ItemStack(Material.BUCKET));
        return true;
    }

    // =====================================================================
    //  Aparência
    // =====================================================================

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.MAGMA_BLOCK);
    }

    @Override
    protected float escalaItem() {
        return 0.3f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("⚒ Forja do Ferreiro ⚒", Skill.FERRARIA.cor())
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.LAVA, c, 30, 0.4, 0.2, 0.4);
        w.spawnParticle(Particle.FLAME, c, 80, 0.4, 0.5, 0.4, 0.05);
        w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.5, 0), 20, 0.3, 0.4, 0.3, 0.02);
        w.playSound(c, Sound.ITEM_BUCKET_EMPTY_LAVA, 1f, 0.8f);
        w.playSound(c, Sound.BLOCK_ANVIL_USE, 1f, 0.6f);
        w.playSound(c, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1f, 1f);
        if (quem == null) return;
        quem.showTitle(Title.title(
                Component.text("⚒ Forja do Ferreiro ⚒", Skill.FERRARIA.cor(), TextDecoration.BOLD),
                Component.text("Clique com o botão direito para forjar", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.FERRARIA, 25);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        World w = centro.getWorld();
        w.spawnParticle(Particle.SMALL_FLAME, centro.clone().subtract(0, 0.3, 0), 3, 0.25, 0.05, 0.25, 0.01);
        if (ciclo % 3 == 0) w.spawnParticle(Particle.SMOKE, centro, 2, 0.1, 0.1, 0.1, 0.02);
        if (ciclo % 8 == 0) {
            w.spawnParticle(Particle.LAVA, centro, 1);
            w.playSound(centro, Sound.BLOCK_LAVA_POP, 0.3f, 1f);
        }
    }

    @Override
    protected void devolver(Location centro) {
        World w = centro.getWorld();
        w.dropItemNaturally(centro, new ItemStack(Material.IRON_BLOCK, BLOCOS_DE_FERRO));
        w.spawnParticle(Particle.LARGE_SMOKE, centro, 20, 0.3, 0.3, 0.3, 0.02);
        w.playSound(centro, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.8f);
    }

    // =====================================================================
    //  Usar
    // =====================================================================

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        if (agachado) {
            plugin.refinaria().abrir(p, b.getLocation());
            return;
        }
        InventoryView view = MenuType.CRAFTING.builder()
                .title(Component.text("⚒ Forja do Ferreiro"))
                .location(b.getLocation())
                .checkReachable(false) // a bigorna não é bancada; a distância é conferida no aoTick
                .build(p);
        view.open();
        abertas.put(p.getUniqueId(), b.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.3f);
    }

    /** Fecha a bancada de quem se afastou ou cuja forja foi quebrada. */
    @Override
    protected void aoTick() {
        List<Player> fechar = new ArrayList<>();
        abertas.entrySet().removeIf(en -> {
            Player p = plugin.getServer().getPlayer(en.getKey());
            if (p == null) return true;
            Location forja = en.getValue();
            boolean longe = !p.getWorld().equals(forja.getWorld())
                    || p.getLocation().distanceSquared(forja.clone().add(0.5, 0.5, 0.5)) > ALCANCE * ALCANCE;
            if (!longe && eh(forja.getBlock())) return false;
            fechar.add(p);
            return true;
        });
        // Fecha depois de sair do loop (fechar dispara o InventoryCloseEvent, que mexe no mapa).
        for (Player p : fechar) {
            if (p.getOpenInventory().getTopInventory().getType() == InventoryType.WORKBENCH) p.closeInventory();
        }
    }

    @EventHandler
    public void aoFechar(InventoryCloseEvent e) {
        abertas.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        abertas.remove(e.getPlayer().getUniqueId());
    }

    /** A bigorna da forja não cai (bigorna normal cai sem apoio). */
    @EventHandler(ignoreCancelled = true)
    public void aoCair(EntityChangeBlockEvent e) {
        if (eh(e.getBlock())) e.setCancelled(true);
    }
}
