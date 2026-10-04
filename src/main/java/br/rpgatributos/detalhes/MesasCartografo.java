package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Mesa do Cartógrafo: 1 bússola e 3 papéis jogados numa mesa de cartografia. Clique nela com um
 * Mapa do Tesouro na mão: ela diz a direção e a distância do X e desenha o caminho no ar. Sem
 * mapa, mostra os Locais Ocultos que você já achou.
 */
public final class MesasCartografo extends Estacao {

    public static final TextColor COR = TextColor.color(0x8D6E63);
    private static final NamespacedKey K_MUNDO = new NamespacedKey("rpgatributos", "tesouro_mundo");
    private static final NamespacedKey K_X = new NamespacedKey("rpgatributos", "tesouro_x");
    private static final NamespacedKey K_Z = new NamespacedKey("rpgatributos", "tesouro_z");
    private static final String[] DIRECOES = {"sul", "sudoeste", "oeste", "noroeste", "norte", "nordeste", "leste", "sudeste"};

    public MesasCartografo(RPGAtributos plugin) {
        super(plugin, "mesas_cartografo", "mesa_cartografo_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CARTOGRAPHY_TABLE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.COMPASS || m == Material.PAPER;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CARTOGRAPHY_TABLE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.COMPASS) < 1 || contar(itens, Material.PAPER) < 3) return false;
        tirar(itens, Material.COMPASS, 1);
        tirar(itens, Material.PAPER, 3);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.COMPASS);
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
        return Component.text("🧭 Mesa do Cartógrafo", COR)
                .append(Component.newline())
                .append(Component.text("clique com um mapa do tesouro", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.ENCHANT, c, 30, 0.4, 0.3, 0.4, 0.5);
        c.getWorld().playSound(c, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("🧭 Mesa do Cartógrafo pronta. Clique nela com um Mapa do Tesouro na mão.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 10 == 0) centro.getWorld().spawnParticle(Particle.ENCHANT, centro, 2, 0.2, 0.2, 0.2, 0.3);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.COMPASS));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.PAPER, 3));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        ItemStack mapa = p.getInventory().getItemInMainHand();
        PersistentDataContainer pdc = mapa.isEmpty() || !mapa.hasItemMeta() ? null : mapa.getItemMeta().getPersistentDataContainer();
        if (pdc == null || !pdc.has(K_X)) {
            p.sendMessage(Component.text("🧭 Sem Mapa do Tesouro na mão: aqui estão os Locais Ocultos que você já achou.", COR));
            p.performCommand("locais");
            return;
        }
        String mundo = pdc.get(K_MUNDO, PersistentDataType.STRING);
        int x = pdc.getOrDefault(K_X, PersistentDataType.INTEGER, 0), z = pdc.getOrDefault(K_Z, PersistentDataType.INTEGER, 0);
        Location mesa = b.getLocation().add(0.5, 1.2, 0.5);
        if (mundo != null && !mundo.equals(mesa.getWorld().getName())) {
            p.sendMessage(Component.text("🧭 Esse tesouro fica em outro mundo (" + mundo + ").", COR));
            return;
        }
        Vector dir = new Vector(x + 0.5 - mesa.getX(), 0, z + 0.5 - mesa.getZ());
        double dist = dir.length();
        if (dist < 1) {
            p.sendMessage(Component.text("🧭 O X é aqui mesmo!", COR));
            return;
        }
        double angulo = Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        int i = (int) Math.round(((angulo % 360) + 360) % 360 / 45.0) % 8;
        p.sendMessage(Component.text("🧭 O X fica a ", COR).append(Component.text(Math.round(dist) + " blocos", NamedTextColor.WHITE))
                .append(Component.text(" ao " + DIRECOES[i] + " daqui.", COR)));
        dir.normalize();
        for (double d = 0.6; d <= 6; d += 0.3) {
            mesa.getWorld().spawnParticle(Particle.END_ROD, mesa.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0);
        }
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }
}
