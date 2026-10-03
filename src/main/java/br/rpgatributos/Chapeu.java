package br.rpgatributos;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Lógica de "item na cabeça", usada tanto pelo /chapeu quanto pelo menu. */
public final class Chapeu {

    /** Slot do capacete no inventário do jogador. */
    public static final int SLOT_CAPACETE = 39;

    public record Resultado(boolean ok, String mensagem) {}

    private Chapeu() {}

    private static boolean vazio(ItemStack i) {
        return i == null || i.isEmpty();
    }

    private static boolean preso(Player p, ItemStack capacete) {
        return !vazio(capacete)
                && capacete.containsEnchantment(Enchantment.BINDING_CURSE)
                && p.getGameMode() != GameMode.CREATIVE;
    }

    /** Coloca 1 unidade do item que está no slot {@code slot} na cabeça. */
    public static Resultado colocar(Player p, int slot) {
        PlayerInventory inv = p.getInventory();
        if (slot == SLOT_CAPACETE) return tirar(p);

        ItemStack fonte = inv.getItem(slot);
        if (vazio(fonte)) return new Resultado(false, "Escolha um item para colocar na cabeça.");

        ItemStack capacete = inv.getHelmet();
        if (preso(p, capacete)) return new Resultado(false, "Seu capacete tem Maldição do Ligamento, não dá pra trocar.");

        ItemStack paraCabeca = fonte.asOne();
        ItemStack resto = fonte.clone();
        resto.setAmount(fonte.getAmount() - 1);
        inv.setItem(slot, resto.getAmount() > 0 ? resto : null);
        inv.setHelmet(paraCabeca);
        if (!vazio(capacete)) devolver(p, capacete);

        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
        return new Resultado(true, "Belo chapéu!");
    }

    public static Resultado tirar(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack capacete = inv.getHelmet();
        if (vazio(capacete)) return new Resultado(false, "Você não tem nada na cabeça.");
        if (preso(p, capacete)) return new Resultado(false, "Seu capacete tem Maldição do Ligamento, não dá pra tirar.");
        inv.setHelmet(null);
        devolver(p, capacete);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 0.8f);
        return new Resultado(true, "Item tirado da cabeça.");
    }

    private static void devolver(Player p, ItemStack item) {
        p.getInventory().addItem(item).values()
                .forEach(sobra -> p.getWorld().dropItemNaturally(p.getLocation(), sobra));
    }
}
