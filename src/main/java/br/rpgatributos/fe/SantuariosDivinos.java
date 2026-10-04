package br.rpgatributos.fe;

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
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;

/**
 * Santuário dos Deuses: bloco de quartzo entalhado com 1 maçã dourada e 1 vela jogadas em
 * cima. Clique para escolher o seu deus, rezar e pedir o milagre; jogue oferendas (Q) em
 * cima dele.
 */
public final class SantuariosDivinos extends Estacao {

    public SantuariosDivinos(RPGAtributos plugin) {
        super(plugin, "santuarios_divinos", "santuario_divino_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHISELED_QUARTZ_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.GOLDEN_APPLE || m == Material.CANDLE;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHISELED_QUARTZ_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.GOLDEN_APPLE) < 1 || contar(itens, Material.CANDLE) < 1) return false;
        tirar(itens, Material.GOLDEN_APPLE, 1);
        tirar(itens, Material.CANDLE, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.NETHER_STAR);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.6;
    }

    @Override
    protected Component nome() {
        return Component.text("✧ Santuário dos Deuses ✧", Deuses.COR)
                .append(Component.newline())
                .append(Component.text("clique · jogue oferendas com Q", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.END_ROD, c, 80, 0.4, 1, 0.4, 0.05);
        c.getWorld().playSound(c, Sound.BLOCK_BELL_RESONATE, 1f, 1.4f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("✧ Santuário dos Deuses ✧", Deuses.COR, TextDecoration.BOLD),
                Component.text("Escolha a quem você vai servir", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.END_ROD, centro, 1, 0.2, 0.3, 0.2, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GOLDEN_APPLE));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.CANDLE));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        plugin.deuses().abrirMenu(p, b.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1f);
    }
}
