package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/** Armas com nome também se acham pelo mundo: em baús de estruturas e com os chefes. */
public final class NomeadasNoMundo implements Listener {

    private final RPGAtributos plugin;

    public NomeadasNoMundo(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Raridade de uma arma achada num baú: Rara, às vezes Épica ou Única. */
    public static Raridade raridadeDeBau() {
        double r = ThreadLocalRandom.current().nextDouble();
        return r < 0.5 ? Raridade.RARO : r < 0.85 ? Raridade.EPICO : Raridade.UNICO;
    }

    /** Raridade de uma arma deixada por um chefe: Épica, às vezes Única ou Lendária. */
    public static Raridade raridadeDeChefe() {
        double r = ThreadLocalRandom.current().nextDouble();
        return r < 0.55 ? Raridade.EPICO : r < 0.9 ? Raridade.UNICO : Raridade.LENDARIO;
    }

    /** Com a chance da config, uma arma com nome para um baú (null se não saiu). */
    public ItemStack talvezParaBau() {
        if (!plugin.settings().nomeadasAtivadas || ThreadLocalRandom.current().nextDouble() >= plugin.settings().nomeadasChanceBau) return null;
        return plugin.forja().criarNomeada(raridadeDeBau());
    }

    /** Para os chefes: com a chance dada, uma arma com nome (null se não saiu). */
    public ItemStack talvezDeChefe(double chance) {
        if (!plugin.settings().nomeadasAtivadas || ThreadLocalRandom.current().nextDouble() >= chance) return null;
        return plugin.forja().criarNomeada(raridadeDeChefe());
    }

    /** Baús das estruturas do jogo (masmorras, minas, templos...) quando o saque é gerado. */
    @EventHandler(ignoreCancelled = true)
    public void aoGerarSaque(LootGenerateEvent e) {
        if (!(e.getInventoryHolder() instanceof Container)) return;
        ItemStack i = talvezParaBau();
        if (i != null) e.getLoot().add(i);
    }
}
