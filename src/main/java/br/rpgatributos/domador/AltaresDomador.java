package br.rpgatributos.domador;

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
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;

/**
 * Altar do Domador: um fardo de feno com 1 laço e 1 sela jogados em cima.
 * Nele o jogador vincula criaturas, invoca criaturas e coloca componentes.
 */
public final class AltaresDomador extends Estacao {

    public AltaresDomador(RPGAtributos plugin) {
        super(plugin, "altares_domador", "altar_domador_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.HAY_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.LEAD || m == Material.SADDLE;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.HAY_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.LEAD) < 1 || contar(itens, Material.SADDLE) < 1) return false;
        tirar(itens, Material.LEAD, 1);
        tirar(itens, Material.SADDLE, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.SADDLE);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.3;
    }

    @Override
    protected Component nome() {
        return Component.text("♞ Altar do Domador ♞", Companheiros.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.HEART, c, 15, 0.5, 0.4, 0.5);
        w.playSound(c, Sound.ENTITY_HORSE_SADDLE, 1f, 1f);
        w.playSound(c, Sound.ENTITY_HORSE_AMBIENT, 1f, 1f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("♞ Altar do Domador ♞", Companheiros.COR, TextDecoration.BOLD),
                Component.text("Traga criaturas com um laço e clique no altar", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.DOMA, 20);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 20 == 0) centro.getWorld().spawnParticle(Particle.HEART, centro.clone().add(0, 0.4, 0), 1, 0.2, 0.1, 0.2);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.LEAD));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.SADDLE));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        plugin.menusDomador().abrirAltar(p, b.getLocation());
    }
}
