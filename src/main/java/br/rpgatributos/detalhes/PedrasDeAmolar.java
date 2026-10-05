package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.combo.TipoArma;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedra de Amolar: 2 pederneiras e 1 lingote de ferro jogados num rebolo, numa pedra lisa ou numa
 * laje de pedra lisa. Clique nela com
 * uma arma corpo a corpo na mão e 1 pederneira no inventário: a arma fica "Afiada" (+10% de
 * dano) pelos próximos 100 golpes.
 */
public final class PedrasDeAmolar extends Estacao {

    public static final TextColor COR = TextColor.color(0xB0BEC5);
    private static final String MARCA = "⚔ Afiada";

    private final NamespacedKey kAfiada;

    public PedrasDeAmolar(RPGAtributos plugin) {
        super(plugin, "pedras_amolar", "pedra_amolar_display");
        this.kAfiada = new NamespacedKey(plugin, "afiada");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.SMOOTH_STONE || m == Material.SMOOTH_STONE_SLAB || m == Material.GRINDSTONE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.FLINT || m == Material.IRON_INGOT;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        if (blocoValido(abaixo.getType())) return abaixo;
        Block noLugar = item.getLocation().getBlock(); // laje e rebolo: o item fica dentro do bloco
        return blocoValido(noLugar.getType()) ? noLugar : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.FLINT) < 2 || contar(itens, Material.IRON_INGOT) < 1) return false;
        tirar(itens, Material.FLINT, 2);
        tirar(itens, Material.IRON_INGOT, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.IRON_SWORD);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("⚔ Pedra de Amolar", COR)
                .append(Component.newline())
                .append(Component.text("clique com a arma e 1 pederneira", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 20, 0.3, 0.2, 0.3, 0.05);
        c.getWorld().playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("⚔ Pedra de Amolar pronta. Clique nela com uma arma e 1 pederneira no inventário.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 6 == 0) centro.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, centro.clone().subtract(0, 0.4, 0), 1, 0.2, 0.05, 0.2, 0.02);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.FLINT, 2));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.IRON_INGOT));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        ItemStack arma = p.getInventory().getItemInMainHand();
        TipoArma t = TipoArma.de(arma);
        if (t == null || t == TipoArma.ARCO) {
            p.sendMessage(Component.text("⚔ Segure uma arma corpo a corpo (espada, machado, lança, tridente ou maça).", NamedTextColor.RED));
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && !p.getInventory().containsAtLeast(new ItemStack(Material.FLINT), 1)) {
            p.sendMessage(Component.text("⚔ Precisa de 1 pederneira no inventário.", NamedTextColor.RED));
            return;
        }
        if (!criativo) p.getInventory().removeItem(new ItemStack(Material.FLINT));
        int golpes = plugin.settings().detAmolarGolpes;
        arma.editMeta(m -> {
            m.getPersistentDataContainer().set(kAfiada, PersistentDataType.INTEGER, golpes);
            linha(m, golpes);
        });
        Location c = b.getLocation().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 30, 0.3, 0.2, 0.3, 0.15);
        c.getWorld().playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1f, 1.3f);
        p.sendMessage(Component.text("⚔ Arma afiada: +" + Math.round(plugin.settings().detAmolarDano * 100) + "% de dano pelos próximos "
                + golpes + " golpes.", COR));
    }

    /** Troca a linha "⚔ Afiada" da descrição (ou tira, com 0). */
    private void linha(ItemMeta m, int golpes) {
        List<Component> lore = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
        lore.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c).startsWith(MARCA));
        if (golpes > 0) {
            lore.add(Component.text(MARCA + ": " + golpes + " golpes (+" + Math.round(plugin.settings().detAmolarDano * 100) + "% de dano)", COR)
                    .decoration(TextDecoration.ITALIC, false));
        }
        m.lore(lore);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGolpear(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c != EntityDamageEvent.DamageCause.ENTITY_ATTACK && c != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        ItemStack arma = p.getInventory().getItemInMainHand();
        if (arma.isEmpty() || !arma.hasItemMeta()) return;
        Integer resta = arma.getItemMeta().getPersistentDataContainer().get(kAfiada, PersistentDataType.INTEGER);
        if (resta == null || resta <= 0) return;
        e.setDamage(e.getDamage() * (1 + plugin.settings().detAmolarDano));
        int depois = resta - 1;
        arma.editMeta(m -> {
            if (depois <= 0) m.getPersistentDataContainer().remove(kAfiada);
            else m.getPersistentDataContainer().set(kAfiada, PersistentDataType.INTEGER, depois);
            if (depois <= 0 || depois % 10 == 0) linha(m, depois);
        });
        if (depois <= 0) {
            p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BREAK, 0.5f, 1.6f);
            p.sendActionBar(Component.text("⚔ Sua arma perdeu o fio.", NamedTextColor.GRAY));
        }
    }
}
