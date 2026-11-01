package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A barra do martelo (3.19): na Forja do Ferreiro, um clique só (sem shift) abre a barra na tela.
 * Um marcador corre de um lado para o outro; bata (clique esquerdo) 3 vezes quando ele passar na
 * faixa dourada. Cada acerto sobe a chance de a peça sair uma raridade acima (+5%, +12%, +25%).
 * A Ferraria alta alarga a faixa. Shift + clique na bancada pula a barra (chance normal).
 */
public final class Martelo implements Listener {

    private static final int LARGURA = 21, BATIDAS = 3;
    private static final long LIMITE_MS = 9000;
    private static final double[] BONUS = {0, 0.05, 0.12, 0.25};

    private static final class Sessao {
        final ItemStack base;
        final Location onde;
        final long inicio = System.currentTimeMillis();
        int pos, dir = 1, batidas, acertos, ultimoTick;
        final int meia;

        Sessao(ItemStack base, Location onde, int meia) {
            this.base = base;
            this.onde = onde;
            this.meia = meia;
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Sessao> sessoes = new HashMap<>();
    private int tickAtual;

    public Martelo(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public boolean ligado(Player p) {
        return plugin.getConfig().getBoolean("forja.barra-martelo", true)
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)
                && !sessoes.containsKey(p.getUniqueId());
    }

    public boolean martelando(Player p) {
        return sessoes.containsKey(p.getUniqueId());
    }

    /** Começa a barra: os materiais já foram gastos; a peça sai no fim (sempre sai). */
    public void comecar(Player p, ItemStack base, Location onde) {
        int nivel = plugin.stats().getNivel(p, Skill.FERRARIA);
        double fr = nivel / (double) Math.max(1, plugin.settings().nivelMaximo);
        int meia = 1 + (int) Math.round(fr * 2); // faixa de 3 a 7 casas
        sessoes.put(p.getUniqueId(), new Sessao(base, onde, meia));
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.6f, 1.2f);
        p.showTitle(Title.title(Component.empty(), Component.text("⚒ Bata quando o marcador passar no dourado (clique esquerdo)", NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(300))));
    }

    /** A cada tick: o marcador anda. */
    public void tick() {
        tickAtual++;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Sessao>> it = sessoes.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Player p = plugin.getServer().getPlayer(en.getKey());
            Sessao s = en.getValue();
            if (p == null) { it.remove(); continue; }
            if (agora - s.inicio > LIMITE_MS) {
                it.remove();
                terminar(p, s);
                continue;
            }
            s.pos += s.dir;
            if (s.pos <= 0 || s.pos >= LARGURA - 1) s.dir = -s.dir;
            p.sendActionBar(barra(s));
        }
    }

    private static Component barra(Sessao s) {
        int centro = LARGURA / 2;
        Component c = Component.text("⚒ ", NamedTextColor.GOLD);
        for (int i = 0; i < LARGURA; i++) {
            boolean dourado = Math.abs(i - centro) <= s.meia;
            if (i == s.pos) c = c.append(Component.text("█", NamedTextColor.WHITE, TextDecoration.BOLD));
            else c = c.append(Component.text(dourado ? "▒" : "─", dourado ? TextColor.color(0xFFC93C) : NamedTextColor.DARK_GRAY));
        }
        StringBuilder marcas = new StringBuilder("  ");
        for (int i = 0; i < BATIDAS; i++) marcas.append(i < s.batidas ? (i < s.acertos ? "◆" : "◇") : "·");
        return c.append(Component.text(marcas.toString(), NamedTextColor.YELLOW));
    }

    private void bater(Player p) {
        Sessao s = sessoes.get(p.getUniqueId());
        if (s == null || tickAtual - s.ultimoTick < 3) return;
        s.ultimoTick = tickAtual;
        s.batidas++;
        boolean acertou = Math.abs(s.pos - LARGURA / 2) <= s.meia;
        if (acertou) s.acertos++;
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, acertou ? 1.8f : 0.7f);
        p.getWorld().spawnParticle(acertou ? Particle.ELECTRIC_SPARK : Particle.SMOKE, s.onde.clone().add(0.5, 1.1, 0.5), acertou ? 12 : 4, 0.2, 0.1, 0.2, 0.05);
        if (s.batidas >= BATIDAS) {
            sessoes.remove(p.getUniqueId());
            terminar(p, s);
        }
    }

    private void terminar(Player p, Sessao s) {
        double bonus = BONUS[Math.min(BATIDAS, s.acertos)];
        Forja forja = plugin.forja();
        ItemStack item = forja.forjar(p, s.base, null, bonus);
        Tempera.emBrasa(item);
        for (ItemStack sobra : p.getInventory().addItem(item).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        List<ItemStack> feitos = List.of(item);
        plugin.stats().darXp(p, Skill.FERRARIA, forja.xpFabricacao(item.getType()) * (1 + 0.1 * s.acertos));
        forja.celebrar(p, s.onde, feitos);
        forja.registrarConquistas(p, feitos);
        if (s.acertos >= BATIDAS) {
            plugin.titulos().registrar(p, "martelo_perfeito", 1);
            p.sendActionBar(Component.text("✦ Três batidas perfeitas! (+25% de chance de raridade)", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else {
            p.sendActionBar(Component.text("⚒ " + s.acertos + " de " + BATIDAS + " batidas no ponto (+" + (int) Math.round(bonus * 100) + "% de chance de raridade)", NamedTextColor.YELLOW));
        }
    }

    // O clique esquerdo durante a barra é a martelada (e não quebra bloco nem bate em ninguém).

    @EventHandler(priority = EventPriority.LOW)
    public void aoBalancar(PlayerAnimationEvent e) {
        if (sessoes.containsKey(e.getPlayer().getUniqueId())) bater(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoInteragir(PlayerInteractEvent e) {
        if (!sessoes.containsKey(e.getPlayer().getUniqueId())) return;
        if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && sessoes.containsKey(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Sessao s = sessoes.remove(e.getPlayer().getUniqueId());
        if (s != null) terminar(e.getPlayer(), s);
    }

    /** Ao desligar o servidor: ninguém perde a peça. */
    public void parar() {
        for (var en : sessoes.entrySet()) {
            Player p = plugin.getServer().getPlayer(en.getKey());
            if (p != null) terminar(p, en.getValue());
        }
        sessoes.clear();
    }
}
