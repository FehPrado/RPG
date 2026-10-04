package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bebedouro: 1 fardo de feno jogado num caldeirão com água. Os animais a até 8 blocos crescem
 * bem mais rápido e esperam menos para cruzar de novo; companheiros por perto se curam.
 */
public final class Bebedouros extends Estacao {

    public static final TextColor COR = TextColor.color(0x7CB342);
    private int passos;

    public Bebedouros(RPGAtributos plugin) {
        super(plugin, "bebedouros", "bebedouro_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.WATER_CAULDRON;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.HAY_BLOCK;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block b = item.getLocation().getBlock();
        return b.getType() == Material.WATER_CAULDRON ? b : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.HAY_BLOCK) < 1) return false;
        tirar(itens, Material.HAY_BLOCK, 1);
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
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("☘ Bebedouro", COR)
                .append(Component.newline())
                .append(Component.text("animais por perto crescem mais rápido", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.SPLASH, c, 30, 0.3, 0.1, 0.3, 0.1);
        c.getWorld().playSound(c, Sound.ENTITY_COW_AMBIENT, 1f, 1.1f);
        if (quem != null) quem.sendMessage(Component.text("☘ Bebedouro pronto: os animais a até 8 blocos crescem mais rápido.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 8 == 0) centro.getWorld().spawnParticle(Particle.SPLASH, centro.clone().subtract(0, 0.6, 0), 3, 0.2, 0.05, 0.2, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.HAY_BLOCK));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        p.sendMessage(Component.text("☘ Bebedouro: filhotes a até 8 blocos crescem 5x mais rápido, os adultos esperam menos para cruzar"
                + " e os companheiros se curam.", COR));
    }

    @Override
    protected void aoTick() {
        if (++passos % 20 != 0) return; // a cada 5 s
        Set<Location> feitos = new HashSet<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            for (Location l : perto(p.getLocation(), 48)) {
                if (!feitos.add(l) || l.getBlock().getType() != Material.WATER_CAULDRON) continue;
                Location c = l.clone().add(0.5, 0.5, 0.5);
                for (Entity e : c.getWorld().getNearbyEntities(c, 8, 4, 8)) {
                    if (e instanceof Animals a && a instanceof Ageable ag) {
                        int idade = ag.getAge();
                        if (idade < 0) ag.setAge(Math.min(0, idade + 400)); // filhote: +20 s de crescimento a cada 5 s
                        else if (idade > 0) ag.setAge(Math.max(0, idade - 400)); // adulto: espera menos para cruzar
                        if (idade != 0 && Math.random() < 0.2) a.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, a.getLocation().add(0, a.getHeight(), 0), 2, 0.2, 0.1, 0.2, 0);
                    }
                    if (e instanceof LivingEntity le && Companheiros.eh(le)) {
                        AttributeInstance max = le.getAttribute(Attribute.MAX_HEALTH);
                        if (max != null && le.getHealth() < max.getValue()) le.setHealth(Math.min(max.getValue(), le.getHealth() + 2));
                    }
                }
            }
        }
    }
}
