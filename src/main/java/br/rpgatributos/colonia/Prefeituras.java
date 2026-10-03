package br.rpgatributos.colonia;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Bell;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * A Prefeitura da colônia: um sino onde se jogou 1 bloco de esmeralda e 1 cama.
 * Só no seu próprio território, uma por jogador. Clique abre a colônia; agachado toca o sino.
 */
public final class Prefeituras extends Estacao {

    public static final TextColor COR = TextColor.color(0xD4A017);

    public Prefeituras(RPGAtributos plugin) {
        super(plugin, "prefeituras", "prefeitura_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.BELL;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.EMERALD_BLOCK || Tag.ITEMS_BEDS.isTagged(m);
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block aqui = item.getLocation().getBlock();
        if (aqui.getType() == Material.BELL) return aqui;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.BELL ? abaixo : null;
    }

    @Override
    protected String impedimento(Block b, UUID quem) {
        if (quem == null) return "Quem faz o ritual precisa estar por perto.";
        var t = plugin.territorios().em(b.getLocation());
        if (t == null || !t.dono().equals(quem)) return "A Prefeitura só pode ser fundada no SEU território (faça um Marco primeiro).";
        if (plugin.colonias().de(quem) != null) return "Você já tem uma colônia.";
        return null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        int camas = 0;
        for (Item i : itens) if (i.isValid() && Tag.ITEMS_BEDS.isTagged(i.getItemStack().getType())) camas += i.getItemStack().getAmount();
        if (contar(itens, Material.EMERALD_BLOCK) < 1 || camas < 1) return false;
        tirar(itens, Material.EMERALD_BLOCK, 1);
        for (Item i : itens) {
            if (i.isValid() && Tag.ITEMS_BEDS.isTagged(i.getItemStack().getType())) {
                tirar(i, 1);
                break;
            }
        }
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.EMERALD);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("⌂ Prefeitura ⌂", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected Component nome(Location bloco) {
        Colonia c = plugin.colonias().naPrefeitura(bloco);
        if (c == null) return nome();
        return Component.text("⌂ Colônia de " + c.nomeDono() + " ⌂", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.HAPPY_VILLAGER, c, 40, 0.5, 0.5, 0.5);
        w.playSound(c, Sound.BLOCK_BELL_USE, 1f, 1f);
        w.playSound(c, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        if (quem == null) return;
        plugin.colonias().fundar(quem, b);
        atualizarDisplays(b.getLocation());
        quem.showTitle(Title.title(Component.text("⌂ Colônia fundada! ⌂", COR, TextDecoration.BOLD),
                Component.text("Os primeiros moradores estão chegando", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2800), Duration.ofMillis(700))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, centro.clone().add(0, 0.2, 0), 1, 0.2, 0.1, 0.2);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.EMERALD_BLOCK));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.WHITE_BED));
    }

    @Override
    protected void aoRemover(Location bloco) {
        plugin.colonias().aoPerderPrefeitura(bloco);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        if (agachado && b.getState() instanceof Bell sino) {
            sino.ring(p);
            return;
        }
        Colonia c = plugin.colonias().naPrefeitura(b.getLocation());
        if (c == null) {
            p.sendActionBar(Component.text("Essa Prefeitura não tem colônia.", NamedTextColor.RED));
            return;
        }
        plugin.colonias().abrir(p, c);
    }
}
