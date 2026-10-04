package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fogueira de Acampamento: 1 lã e 1 tronco jogados numa fogueira acesa. Sentado perto dela
 * (escada, laje ou /sentar) você regenera e, depois de 30 s, fica "Descansado": +5% de XP em
 * tudo por 20 minutos. Comida crua jogada do lado do fogo assa sozinha.
 */
public final class Fogueiras extends Estacao {

    public static final TextColor COR = TextColor.color(0xFF8A3D);
    private static final Map<Material, Material> ASSA = Map.of(
            Material.BEEF, Material.COOKED_BEEF, Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.CHICKEN, Material.COOKED_CHICKEN, Material.MUTTON, Material.COOKED_MUTTON,
            Material.RABBIT, Material.COOKED_RABBIT, Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON, Material.POTATO, Material.BAKED_POTATO, Material.KELP, Material.DRIED_KELP);

    private final NamespacedKey kDescansado;
    private final Map<UUID, Integer> descanso = new HashMap<>();
    private int passos;

    public Fogueiras(RPGAtributos plugin) {
        super(plugin, "fogueiras", "fogueira_display");
        this.kDescansado = new NamespacedKey(plugin, "descansado_ate");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return Tag.WOOL.isTagged(m) || Tag.LOGS.isTagged(m);
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
        Item la = null, tronco = null;
        for (Item i : itens) {
            Material m = i.getItemStack().getType();
            if (la == null && Tag.WOOL.isTagged(m)) la = i;
            else if (tronco == null && Tag.LOGS.isTagged(m)) tronco = i;
        }
        if (la == null || tronco == null) return false;
        tirar(la, 1);
        tirar(tronco, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.COOKED_MUTTON);
    }

    @Override
    protected float escalaItem() {
        return 0.4f;
    }

    @Override
    protected double alturaItem() {
        return 1.6;
    }

    @Override
    protected Component nome() {
        return Component.text("♨ Fogueira de Acampamento", COR)
                .append(Component.newline())
                .append(Component.text("sente-se por perto para descansar", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c, 10, 0.2, 0.3, 0.2, 0.02);
        c.getWorld().playSound(c, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("♨ Fogueira de Acampamento pronta. Sente-se perto (escada, laje ou /sentar) para descansar.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.SMALL_FLAME, centro.clone().subtract(0, 0.8, 0), 2, 0.2, 0.1, 0.2, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.WHITE_WOOL));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.OAK_LOG));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        long resta = descansadoAte(p) - System.currentTimeMillis();
        if (resta > 0) {
            p.sendMessage(Component.text("♨ Você está Descansado por mais " + (resta / 60000 + 1) + " min (+"
                    + Math.round(plugin.settings().detDescansadoXp * 100) + "% de XP).", COR));
        } else {
            p.sendMessage(Component.text("♨ Sente-se perto do fogo (escada, laje ou /sentar) por 30 s para ficar Descansado.", COR));
        }
    }

    private static boolean acesa(Block b) {
        return b.getBlockData() instanceof Lightable l && l.isLit();
    }

    // =====================================================================
    //  Descanso e comida assando
    // =====================================================================

    public long descansadoAte(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kDescansado, PersistentDataType.LONG, 0L);
    }

    public double multiplicadorXp(Player p) {
        return System.currentTimeMillis() < descansadoAte(p) ? 1 + plugin.settings().detDescansadoXp : 1;
    }

    @Override
    protected void aoTick() {
        if (++passos % 20 != 0) return; // a cada 5 s
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location perto = null;
            for (Location l : perto(p.getLocation(), 6)) {
                if (acesa(l.getBlock())) { perto = l; break; }
            }
            if (perto == null || !plugin.assentos().sentado(p)) {
                descanso.remove(p.getUniqueId());
            } else {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false, true));
                int s = descanso.merge(p.getUniqueId(), 5, Integer::sum);
                if (s >= 30 && System.currentTimeMillis() >= descansadoAte(p) - 60_000) {
                    long ate = System.currentTimeMillis() + plugin.settings().detDescansadoMinutos * 60_000L;
                    p.getPersistentDataContainer().set(kDescansado, PersistentDataType.LONG, ate);
                    descanso.remove(p.getUniqueId());
                    p.playSound(p.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 0.8f);
                    p.sendMessage(Component.text("♨ Descansado! ", COR, TextDecoration.BOLD)
                            .append(Component.text("+" + Math.round(plugin.settings().detDescansadoXp * 100) + "% de XP em tudo por "
                                    + plugin.settings().detDescansadoMinutos + " minutos.", NamedTextColor.GRAY)));
                }
            }
            if (perto != null) assar(perto);
        }
    }

    /** Comida crua no chão, do lado do fogo, há pelo menos 10 s: assa. */
    private void assar(Location fogo) {
        Location c = fogo.clone().add(0.5, 0.5, 0.5);
        for (Entity e : c.getWorld().getNearbyEntities(c, 2.5, 1.5, 2.5)) {
            if (!(e instanceof Item item) || item.getTicksLived() < 200) continue;
            ItemStack s = item.getItemStack();
            Material assado = ASSA.get(s.getType());
            if (assado == null || s.hasItemMeta() && s.getItemMeta().hasItemName()) continue;
            item.setItemStack(new ItemStack(assado, s.getAmount()));
            item.getWorld().spawnParticle(Particle.SMOKE, item.getLocation(), 6, 0.1, 0.1, 0.1, 0.01);
            item.getWorld().playSound(item.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.8f);
        }
    }
}
