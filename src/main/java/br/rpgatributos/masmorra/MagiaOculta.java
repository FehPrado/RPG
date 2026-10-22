package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A magia oculta Abrir Portal (2.68). Só se aprende pelo Grimório Oculto (raro nos baús de cristal e
 * nos tesouros dos portais B, A e S). Com o grimório na mão: a primeira vez ensina; depois, conjura.
 * <ul>
 *   <li>Abre um portal do mundo na sua frente, com o rank do seu Rank de Caçador.</li>
 *   <li>Gasta toda a mana (precisa estar quase cheia) e um Cristal de Mana grande. Uma vez por dia de jogo.</li>
 *   <li>10% de chance de sair um rank acima; 3% de sair um Portal Vermelho (sem saída até vencer).</li>
 *   <li>Quem conjura e a party dele têm 30 segundos para entrar antes de todo mundo.</li>
 * </ul>
 */
public final class MagiaOculta implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "grimorio_oculto");
    private static final NamespacedKey K_SABE = new NamespacedKey("rpgatributos", "magia_abrir_portal");
    private static final NamespacedKey K_RECARGA = new NamespacedKey("rpgatributos", "magia_abrir_portal_ate");
    private static final TextColor COR = TextColor.color(0x7C4DFF);
    private static final long DIA = 24_000L;

    private final RPGAtributos plugin;

    public MagiaOculta(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static ItemStack grimorio() {
        ItemStack i = new ItemStack(Material.ENCHANTED_BOOK);
        i.editMeta(m -> {
            m.itemName(Component.text("Grimório Oculto", COR));
            m.lore(List.of(
                    Component.text("Um livro que não devia existir.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true),
                    Component.text("Clique direito: aprende (e depois conjura)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("a magia oculta Abrir Portal.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    public static boolean eGrimorio(ItemStack i) {
        return i != null && i.getType() == Material.ENCHANTED_BOOK && i.hasItemMeta() && i.getPersistentDataContainer().has(CHAVE);
    }

    public static boolean sabe(Player p) {
        return p.getPersistentDataContainer().has(K_SABE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !eGrimorio(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!sabe(p)) {
            p.getPersistentDataContainer().set(K_SABE, PersistentDataType.BYTE, (byte) 1);
            p.showTitle(Title.title(Component.text("[Sistema]", NamedTextColor.AQUA, TextDecoration.BOLD),
                    Component.text("Habilidade oculta: Abrir Portal", COR, TextDecoration.BOLD),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.6f);
            p.sendMessage(Component.text("[Sistema] Você aprendeu Abrir Portal. Com o grimório na mão, clique de novo para conjurar:"
                    + " gasta toda a mana e um Cristal de Mana grande, uma vez por dia. O rank vem do seu Rank de Caçador.", COR));
            return;
        }
        conjurar(p);
    }

    private void conjurar(Player p) {
        if (plugin.masmorras().ehMundo(p.getWorld()) || p.getWorld().getEnvironment() != World.Environment.NORMAL) {
            falha(p, "A magia não pega aqui: só no mundo de cima.");
            return;
        }
        long agora = p.getWorld().getFullTime();
        long ate = p.getPersistentDataContainer().getOrDefault(K_RECARGA, PersistentDataType.LONG, 0L);
        if (ate > agora) {
            falha(p, "Sua mana ainda não se recuperou da última abertura (" + ((ate - agora) / 1000 + 1) + " h de jogo).");
            return;
        }
        Location onde = lugar(p);
        if (onde == null) {
            falha(p, "Não há chão livre na sua frente para o portal.");
            return;
        }
        if (plugin.territorios().em(onde) != null && !plugin.territorios().podeConstruir(p, onde)) {
            falha(p, "Este território não é seu: o portal não se abre aqui.");
            return;
        }
        var pf = plugin.arcano().perfil(p);
        double max = plugin.arcano().manaMax(p, pf);
        if (pf.mana() < max * 0.9) {
            falha(p, "Precisa da mana quase cheia (" + (int) pf.mana() + "/" + (int) max + ").");
            return;
        }
        if (!ManaCristal.gastarUm(p, ManaCristal.Tamanho.GRANDE)) {
            falha(p, "Precisa de um Cristal de Mana grande (ou perfeito).");
            return;
        }
        plugin.arcano().gastarMana(p, pf, pf.mana());
        p.getPersistentDataContainer().set(K_RECARGA, PersistentDataType.LONG, agora + DIA);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Rank rank = Cacador.rank(p);
        boolean fugiu = false, vermelho = false;
        double x = rnd.nextDouble();
        if (x < 0.03) vermelho = true;
        else if (x < 0.13 && rank != Rank.S) {
            rank = rank.acima();
            fugiu = true;
        }
        World w = onde.getWorld();
        w.spawnParticle(Particle.REVERSE_PORTAL, onde.clone().add(0, 1.5, 0), 120, 0.6, 1.2, 0.6, 0.08);
        w.playSound(onde, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.7f);
        if (fugiu || vermelho) {
            p.showTitle(Title.title(Component.text("⚠ O mana fugiu do controle", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text(vermelho ? "Um Portal Vermelho se abriu" : "O portal saiu " + rank.nome(), NamedTextColor.GOLD),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
        plugin.portais().abrirPorMagia(onde, rank, vermelho, p.getUniqueId());
    }

    /** O chão 3 blocos na frente de quem conjura. */
    private static Location lugar(Player p) {
        Vector frente = p.getLocation().getDirection().setY(0);
        if (frente.lengthSquared() < 1e-4) frente = new Vector(0, 0, 1);
        Location alvo = p.getLocation().add(frente.normalize().multiply(3));
        World w = p.getWorld();
        for (int dy = 2; dy >= -3; dy--) {
            Block b = w.getBlockAt(alvo.getBlockX(), alvo.getBlockY() + dy, alvo.getBlockZ());
            if (!b.getType().isSolid()) continue;
            boolean livre = true;
            for (int k = 1; k <= 4; k++) if (!b.getRelative(0, k, 0).isPassable() || b.getRelative(0, k, 0).isLiquid()) livre = false;
            if (!livre) continue;
            Location l = b.getLocation().add(0.5, 1, 0.5);
            Vector para = p.getLocation().toVector().subtract(l.toVector()).setY(0);
            l.setYaw(para.lengthSquared() < 1e-4 ? 0 : (float) Math.toDegrees(Math.atan2(-para.getX(), para.getZ())));
            return l;
        }
        return null;
    }

    private static void falha(Player p, String msg) {
        p.sendActionBar(Component.text("✦ " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.5f);
    }

    /** Para o /rpgadmin: esquece a magia e a recarga. */
    public static void esquecer(Player p) {
        p.getPersistentDataContainer().remove(K_SABE);
        p.getPersistentDataContainer().remove(K_RECARGA);
    }
}
