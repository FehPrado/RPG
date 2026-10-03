package br.rpgatributos.torre;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
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
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;

/**
 * Obelisco da Torre: obsidiana chorona com 1 olho do ender e 1 bloco de ouro jogados em
 * cima. Clicar abre a Torre Infinita (subir com a party e ver o ranking).
 */
public final class ObeliscoTorre extends Estacao {

    public ObeliscoTorre(RPGAtributos plugin) {
        super(plugin, "obeliscos_torre", "obelisco_torre_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CRYING_OBSIDIAN;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENDER_EYE || m == Material.GOLD_BLOCK;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CRYING_OBSIDIAN ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.ENDER_EYE) < 1 || contar(itens, Material.GOLD_BLOCK) < 1) return false;
        tirar(itens, Material.ENDER_EYE, 1);
        tirar(itens, Material.GOLD_BLOCK, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENDER_EYE);
    }

    @Override
    protected float escalaItem() {
        return 0.7f;
    }

    @Override
    protected double alturaItem() {
        return 1.7;
    }

    @Override
    protected Component nome() {
        return Component.text("⛩ Obelisco da Torre ⛩", Torre.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.END_ROD, c, 60, 0.3, 1.2, 0.3, 0.05);
        w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("⛩ Obelisco da Torre ⛩", Torre.COR, TextDecoration.BOLD),
                Component.text("A Torre Infinita espera", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.END_ROD, centro.clone().add(0, 0.6, 0), 1, 0.05, 0.4, 0.05, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.ENDER_EYE));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GOLD_BLOCK));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        plugin.torre().abrirMenu(p, b.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 0.8f);
    }
}
