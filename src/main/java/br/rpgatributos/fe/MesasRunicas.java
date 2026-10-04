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
 * Mesa Rúnica: tufo entalhado com 4 fragmentos de ametista e 1 bloco de lápis-lazúli
 * jogados em cima. Grava runas nos equipamentos.
 */
public final class MesasRunicas extends Estacao {

    public MesasRunicas(RPGAtributos plugin) {
        super(plugin, "mesas_runicas", "mesa_runica_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHISELED_TUFF;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.AMETHYST_SHARD || m == Material.LAPIS_BLOCK;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHISELED_TUFF ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.AMETHYST_SHARD) < 4 || contar(itens, Material.LAPIS_BLOCK) < 1) return false;
        tirar(itens, Material.AMETHYST_SHARD, 4);
        tirar(itens, Material.LAPIS_BLOCK, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENCHANTED_BOOK);
    }

    @Override
    protected float escalaItem() {
        return 0.55f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("ᚱ Mesa Rúnica ᚱ", Runas.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.ENCHANT, c, 80, 0.5, 0.8, 0.5, 1);
        c.getWorld().playSound(c, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.6f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("ᚱ Mesa Rúnica ᚱ", Runas.COR, TextDecoration.BOLD),
                Component.text("Grave runas nos seus equipamentos", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.ENCHANT, centro, 3, 0.3, 0.3, 0.3, 0.5);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.AMETHYST_SHARD, 4));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.LAPIS_BLOCK));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        plugin.runas().abrirMenu(p, b.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
    }
}
