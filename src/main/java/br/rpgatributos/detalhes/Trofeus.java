package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estante de Troféus: o núcleo de um chefe jogado num pilar de quartzo. O núcleo fica girando
 * em cima, com quem venceu e quando. Cada troféu diferente dentro de um território dá +1% de
 * XP a quem pode construir nele e está lá dentro (até +6%, um de cada chefe).
 */
public final class Trofeus extends Estacao {

    public static final TextColor COR = TextColor.color(0xFFC107);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Map<UUID, Double> bonus = new HashMap<>();
    private int passos;

    public Trofeus(RPGAtributos plugin) {
        super(plugin, "trofeus", "trofeu_display");
    }

    private NamespacedKey chave(Location l) {
        return new NamespacedKey(plugin, "trofeu_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    /** [núcleo, quem, data] do troféu, ou null. */
    private String[] dados(Location l) {
        String s = l.getChunk().getPersistentDataContainer().get(chave(l), PersistentDataType.STRING);
        return s == null ? null : s.split("\\|", 3);
    }

    private Raro nucleo(Location l) {
        String[] d = dados(l);
        if (d == null) return null;
        try {
            return Raro.valueOf(d[0]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.QUARTZ_PILLAR;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        for (Raro r : Raro.values()) if (r.nucleo() && r.base() == m) return true;
        return false;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.QUARTZ_PILLAR ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        for (Item i : itens) {
            Raro r = Raro.de(i.getItemStack());
            if (r == null || !r.nucleo()) continue;
            UUID quem = i.getThrower();
            String nome = quem == null ? "?" : Bukkit.getOfflinePlayer(quem).getName();
            bloco.getChunk().getPersistentDataContainer().set(chave(bloco.getLocation()), PersistentDataType.STRING,
                    r.name() + "|" + nome + "|" + LocalDate.now().format(DATA));
            tirar(i, 1);
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.GOLD_INGOT);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        Raro r = nucleo(bloco);
        return r == null ? itemFlutuante() : r.criar();
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.55;
    }

    @Override
    protected Component nome() {
        return Component.text("🏆 Troféu", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        String[] d = dados(bloco);
        Raro r = nucleo(bloco);
        if (d == null || r == null) return nome();
        return Component.text("🏆 " + r.nome(), COR, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text(d[1] + " · " + d[2], NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.4, 0.5);
        c.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, c, 40, 0.3, 0.4, 0.3, 0.3);
        c.getWorld().playSound(c, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        if (quem != null) {
            plugin.titulos().registrar(quem, "trofeus", 1);
            quem.sendMessage(Component.text("🏆 Troféu exposto! Dentro do seu território, cada troféu diferente dá +1% de XP.", COR));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 5 == 0) centro.getWorld().spawnParticle(Particle.WAX_ON, centro, 2, 0.2, 0.2, 0.2, 0);
    }

    @Override
    protected void devolver(Location centro) {
        Raro r = nucleo(centro.getBlock().getLocation());
        if (r != null) centro.getWorld().dropItemNaturally(centro, r.criar());
    }

    @Override
    protected void aoRemover(Location bloco) {
        bloco.getChunk().getPersistentDataContainer().remove(chave(bloco));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        String[] d = dados(b.getLocation());
        Raro r = nucleo(b.getLocation());
        if (d == null || r == null) return;
        p.sendMessage(Component.text("🏆 " + r.nome() + ", trazido por " + d[1] + " em " + d[2] + ".", COR));
    }

    // =====================================================================
    //  Bônus no território
    // =====================================================================

    public double multiplicadorXp(Player p) {
        return 1 + bonus.getOrDefault(p.getUniqueId(), 0.0);
    }

    @Override
    protected void aoTick() {
        if (++passos % 40 != 0) return; // a cada 10 s
        bonus.clear();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            var t = plugin.territorios().em(p.getLocation());
            if (t == null || !plugin.territorios().podeConstruir(p.getUniqueId(), p.getLocation())) continue;
            Set<Raro> tipos = EnumSet.noneOf(Raro.class);
            for (Location l : perto(p.getLocation(), 96)) {
                Raro r = nucleo(l);
                if (r != null && t.equals(plugin.territorios().em(l))) tipos.add(r);
            }
            if (!tipos.isEmpty()) bonus.put(p.getUniqueId(), 0.01 * tipos.size());
        }
    }
}
