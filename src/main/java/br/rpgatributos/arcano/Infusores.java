package br.rpgatributos.arcano;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * O Infusor Corpóreo: caldeirão com água onde se jogou uma mesa de encantamento
 * e uma bigorna. Tem uma mesa de encantamento girando em cima.
 */
public final class Infusores extends Estacao {

    private static final Set<Material> CALDEIROES = EnumSet.of(
            Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON);
    private static final Set<Material> BIGORNAS = EnumSet.of(
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    private static final Particle.DustOptions ROXO = new Particle.DustOptions(Color.fromRGB(0xC77DFF), 1.2f);

    public Infusores(RPGAtributos plugin) {
        super(plugin, "infusores", "infusor_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return CALDEIROES.contains(m);
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENCHANTING_TABLE || BIGORNAS.contains(m);
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block b = item.getLocation().getBlock();
        return b.getType() == Material.WATER_CAULDRON ? b : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        Item mesa = null;
        Item bigorna = null;
        for (Item i : itens) {
            Material t = i.getItemStack().getType();
            if (mesa == null && t == Material.ENCHANTING_TABLE) mesa = i;
            else if (bigorna == null && BIGORNAS.contains(t)) bigorna = i;
        }
        if (mesa == null || bigorna == null) return false;
        tirar(mesa, 1);
        tirar(bigorna, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENCHANTING_TABLE);
    }

    @Override
    protected float escalaItem() {
        return 0.55f;
    }

    @Override
    protected double alturaItem() {
        return 1.35;
    }

    @Override
    protected Component nome() {
        return Component.text("✦ Infusor Corpóreo ✦", Arcano.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().toCenterLocation();
        w.spawnParticle(Particle.ENCHANT, c.clone().add(0, 1, 0), 200, 0.6, 0.8, 0.6, 1.5);
        w.spawnParticle(Particle.WITCH, c.clone().add(0, 0.6, 0), 40, 0.4, 0.4, 0.4, 0.05);
        w.spawnParticle(Particle.DUST, c.clone().add(0, 1, 0), 60, 0.5, 0.6, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(0xC77DFF), 1.6f));
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.6f);
        w.playSound(c, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.7f);
        if (quem == null) return;
        quem.showTitle(Title.title(
                Component.text("✦ Infusor Corpóreo ✦", Arcano.COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito no caldeirão", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.ARCANO, 25);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        centro.getWorld().spawnParticle(Particle.ENCHANT, centro, 6, 0.4, 0.3, 0.4, 0.6);
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.DUST, centro, 3, 0.3, 0.3, 0.3, 0, ROXO);
    }

    @Override
    protected void devolver(Location centro) {
        World w = centro.getWorld();
        w.dropItemNaturally(centro, new ItemStack(Material.ENCHANTING_TABLE));
        w.dropItemNaturally(centro, new ItemStack(Material.ANVIL));
        w.spawnParticle(Particle.WITCH, centro, 30, 0.4, 0.4, 0.4, 0.05);
        w.playSound(centro, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.2f);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        plugin.arcano().menus().abrirInfusor(p, b.getLocation());
    }
}
