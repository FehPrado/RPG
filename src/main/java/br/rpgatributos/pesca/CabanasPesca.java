package br.rpgatributos.pesca;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Cabana de Pesca: 1 vara de pescar, 1 bacalhau cru e 4 tábuas de abeto jogados numa fogueira.
 * A até {@link #RAIO} blocos: a isca afunda 30% mais rápido, +4% de peixe raro e ninguém
 * sente frio. Feita para acompanhar a pesca no gelo.
 */
public final class CabanasPesca extends Estacao {

    public static final TextColor COR = TextColor.color(0x8FD3FF);
    public static final double RAIO = 12;

    public CabanasPesca(RPGAtributos plugin) {
        super(plugin, "cabanas_pesca", "cabana_pesca_display");
    }

    /** Há uma cabana de pesca perto deste lugar? */
    public boolean perto(Location l) {
        return !perto(l, RAIO).isEmpty();
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.FISHING_ROD || m == Material.COD || m == Material.SPRUCE_PLANKS;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block b = item.getLocation().getBlock();
        if (blocoValido(b.getType())) return b;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return blocoValido(abaixo.getType()) ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.FISHING_ROD) < 1 || contar(itens, Material.COD) < 1 || contar(itens, Material.SPRUCE_PLANKS) < 4) return false;
        tirar(itens, Material.FISHING_ROD, 1);
        tirar(itens, Material.COD, 1);
        tirar(itens, Material.SPRUCE_PLANKS, 4);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.FISHING_ROD);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.6;
    }

    @Override
    protected Component nome() {
        return Component.text("⚓ Cabana de Pesca", COR)
                .append(Component.newline())
                .append(Component.text("isca mais rápida e calor por perto", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 30, 0.6, 0.4, 0.6, 0.02);
        c.getWorld().playSound(c, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 0.9f);
        if (quem != null) {
            quem.sendMessage(Component.text("⚓ Cabana de Pesca pronta: a até " + (int) RAIO + " blocos a isca afunda mais rápido, "
                    + "os peixes raros aparecem mais e ninguém sente frio. Abra um buraco no gelo e pesque!", COR));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 10 == 0) centro.getWorld().spawnParticle(Particle.SNOWFLAKE, centro, 2, 0.4, 0.3, 0.4, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.FISHING_ROD));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        p.sendMessage(Component.text("⚓ Cabana de Pesca: a até " + (int) RAIO + " blocos, isca 30% mais rápida, +4% de peixe raro "
                + "e ninguém sente frio. Os peixes do gelo só mordem num buraco no gelo (bioma gelado ou qualquer lago no inverno).", COR));
    }
}
