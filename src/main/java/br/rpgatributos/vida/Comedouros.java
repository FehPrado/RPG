package br.rpgatributos.vida;

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
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Comedouro: um baú com 1 fardo de feno e 1 tigela jogados em cima. Guarde a comida dentro (ou
 * encha com funis): na virada do dia, cada animal da fazenda a até 12 blocos que não comeu come
 * uma porção do que é dele (trigo, sementes, cenoura...). Um fardo de feno vira 9 trigos.
 */
public final class Comedouros extends Estacao {

    public static final TextColor COR = TextColor.color(0xE0A030);
    public static final double RAIO = 12;

    public Comedouros(RPGAtributos plugin) {
        super(plugin, "comedouros", "comedouro_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHEST;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.HAY_BLOCK || m == Material.BOWL;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHEST ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.HAY_BLOCK) < 1 || contar(itens, Material.BOWL) < 1) return false;
        tirar(itens, Material.HAY_BLOCK, 1);
        tirar(itens, Material.BOWL, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.WHEAT);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.35;
    }

    @Override
    protected Component nome() {
        return Component.text("🌾 Comedouro", COR)
                .append(Component.newline())
                .append(Component.text("os animais a até " + (int) RAIO + " blocos comem daqui", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.BLOCK, c, 25, 0.3, 0.2, 0.3, Material.HAY_BLOCK.createBlockData());
        c.getWorld().playSound(c, Sound.ENTITY_HORSE_EAT, 1f, 1f);
        c.getWorld().playSound(c, Sound.ENTITY_COW_AMBIENT, 0.8f, 1.1f);
        if (quem != null) {
            quem.sendMessage(Component.text("🌾 Comedouro pronto! Guarde a comida dentro (ou encha com um funil):", COR));
            quem.sendMessage(Component.text("  na virada do dia, os animais da fazenda a até " + (int) RAIO
                    + " blocos que não comeram comem daqui. Agache e clique para ver quanto dura.", NamedTextColor.GRAY));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 16 == 0) centro.getWorld().spawnParticle(Particle.BLOCK, centro.clone().subtract(0, 0.5, 0), 2, 0.2, 0.05, 0.2, Material.HAY_BLOCK.createBlockData());
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.HAY_BLOCK));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.BOWL));
    }

    /** Clique: abre o baú. Agachado: quanto de comida tem e quantos animais estão perto. */
    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        if (!(b.getState() instanceof Chest bau)) return;
        if (!agachado) {
            p.openInventory(bau.getInventory());
            return;
        }
        int porcoes = 0;
        for (ItemStack i : bau.getInventory().getContents()) {
            if (i == null || i.isEmpty()) continue;
            if (i.getType() == Material.HAY_BLOCK) porcoes += 9 * i.getAmount();
            else if (Criacao.comidaDeAlguem(i.getType())) porcoes += i.getAmount();
        }
        int animais = plugin.criacao().animaisPerto(b.getLocation().add(0.5, 0.5, 0.5), RAIO);
        p.sendMessage(Component.text("🌾 Comedouro: " + porcoes + " porções de comida · " + animais + " animal(is) seu(s) a até " + (int) RAIO + " blocos", COR));
        if (animais > 0) {
            p.sendMessage(Component.text("  Dá para uns " + porcoes / animais + " dia(s). Galinha come sementes, vaca/cabra/ovelha trigo"
                    + " (ou fardo de feno), porco cenoura/batata/beterraba, coelho cenoura ou dente-de-leão.", NamedTextColor.GRAY));
        }
    }

    /**
     * Um animal que ficou sem comer pega uma porção de um comedouro perto.
     * @return true se comeu.
     */
    public boolean alimentar(LivingEntity e, Set<Material> comida) {
        for (Location l : perto(e.getLocation(), RAIO)) {
            Block b = l.getBlock();
            if (!eh(b) || !(b.getState() instanceof Chest bau)) continue;
            Inventory inv = bau.getInventory();
            boolean comeu = false;
            for (Material m : comida) {
                if (inv.containsAtLeast(new ItemStack(m), 1)) {
                    inv.removeItem(new ItemStack(m, 1));
                    comeu = true;
                    break;
                }
            }
            // Fardo de feno: vira 9 trigos (come 1, os outros 8 voltam para o baú).
            if (!comeu && comida.contains(Material.WHEAT) && inv.containsAtLeast(new ItemStack(Material.HAY_BLOCK), 1)) {
                inv.removeItem(new ItemStack(Material.HAY_BLOCK, 1));
                for (ItemStack sobra : inv.addItem(new ItemStack(Material.WHEAT, 8)).values()) b.getWorld().dropItemNaturally(l.clone().add(0.5, 1, 0.5), sobra);
                comeu = true;
            }
            if (!comeu) continue;
            e.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 4, 0.3, 0.2, 0.3, 0);
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.5f, 1.2f);
            return true;
        }
        return false;
    }
}
