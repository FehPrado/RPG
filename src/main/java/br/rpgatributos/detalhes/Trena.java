package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Trena (2.65): clique num bloco (o primeiro canto) e depois noutro (o segundo): a barra mostra
 * largura × altura × comprimento, quantos blocos cabem e a distância, e uma caixa de partículas
 * (só para quem mede) fica 15 segundos no lugar. Agachado + clique apaga.
 */
final class Trena implements Listener {

    private static final Particle.DustOptions COR_CAIXA = new Particle.DustOptions(Color.fromRGB(0xF2C12E), 0.7f);
    private static final Particle.DustOptions COR_PONTO = new Particle.DustOptions(Color.fromRGB(0x4FC3F7), 1.0f);

    private static final class Medida {
        Location a, b;
        long ate;
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Medida> medidas = new HashMap<>();
    private int ticks;

    Trena(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || !Utilidades.Item.TRENA.e(e.getItem())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Block bl = e.getClickedBlock();
        if (!plugin.settings().utilTrena || bl == null) return;
        if (p.isSneaking()) {
            medidas.remove(p.getUniqueId());
            p.sendActionBar(Component.text("📏 Medida apagada.", NamedTextColor.GRAY));
            return;
        }
        Medida m = medidas.computeIfAbsent(p.getUniqueId(), k -> new Medida());
        Location l = bl.getLocation();
        if (m.a == null || m.b != null || !m.a.getWorld().equals(l.getWorld())) {
            m.a = l;
            m.b = null;
            m.ate = System.currentTimeMillis() + 60_000;
            p.sendActionBar(Component.text("📏 Primeiro canto marcado: agora clique no outro.", Utilidades.COR));
            p.playSound(l, Sound.UI_BUTTON_CLICK, 0.5f, 1.6f);
            return;
        }
        m.b = l;
        m.ate = System.currentTimeMillis() + 15_000;
        int x = Math.abs(m.a.getBlockX() - l.getBlockX()) + 1, y = Math.abs(m.a.getBlockY() - l.getBlockY()) + 1, z = Math.abs(m.a.getBlockZ() - l.getBlockZ()) + 1;
        double dist = m.a.distance(l);
        p.sendActionBar(Component.text("📏 " + x + " × " + y + " × " + z + " (" + (x * y * z) + " blocos) · distância " + Utilidades.fmt(Math.round(dist * 10) / 10.0), Utilidades.COR));
        p.playSound(l, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    /** A cada tick (desenha a cada 10): o primeiro canto e a caixa. */
    void tick() {
        if (medidas.isEmpty() || ++ticks % 10 != 0) return;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Medida>> it = medidas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Medida> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            Medida m = en.getValue();
            if (p == null || m.ate < agora || m.a == null || !p.getWorld().equals(m.a.getWorld())) {
                it.remove();
                continue;
            }
            if (m.b == null) {
                p.spawnParticle(Particle.DUST, m.a.clone().add(0.5, 1.1, 0.5), 4, 0.2, 0.05, 0.2, 0, COR_PONTO);
                continue;
            }
            caixa(p, m.a, m.b);
        }
    }

    /** As 12 arestas da caixa que vai de um canto ao outro (pontos a cada meio bloco, até 400). */
    private static void caixa(Player p, Location a, Location b) {
        double x1 = Math.min(a.getBlockX(), b.getBlockX()), y1 = Math.min(a.getBlockY(), b.getBlockY()), z1 = Math.min(a.getBlockZ(), b.getBlockZ());
        double x2 = Math.max(a.getBlockX(), b.getBlockX()) + 1, y2 = Math.max(a.getBlockY(), b.getBlockY()) + 1, z2 = Math.max(a.getBlockZ(), b.getBlockZ()) + 1;
        double perimetro = 4 * ((x2 - x1) + (y2 - y1) + (z2 - z1));
        double passo = Math.max(0.5, perimetro / 400);
        double[][] cantos = {{x1, y1, z1}, {x2, y1, z1}, {x1, y2, z1}, {x1, y1, z2}, {x2, y2, z1}, {x2, y1, z2}, {x1, y2, z2}, {x2, y2, z2}};
        int[][] arestas = {{0, 1}, {0, 2}, {0, 3}, {1, 4}, {1, 5}, {2, 4}, {2, 6}, {3, 5}, {3, 6}, {4, 7}, {5, 7}, {6, 7}};
        for (int[] ar : arestas) {
            double[] c1 = cantos[ar[0]], c2 = cantos[ar[1]];
            double dx = c2[0] - c1[0], dy = c2[1] - c1[1], dz = c2[2] - c1[2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            for (double t = 0; t <= len; t += passo) {
                double f = len == 0 ? 0 : t / len;
                p.spawnParticle(Particle.DUST, c1[0] + dx * f, c1[1] + dy * f, c1[2] + dz * f, 1, 0, 0, 0, 0, COR_CAIXA);
            }
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        medidas.remove(e.getPlayer().getUniqueId());
    }
}
