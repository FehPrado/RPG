package br.rpgatributos.pesca;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pesca na lava (2.56). Com a Vara Ígnea, a boia não afunda na lava do Nether: ela boia, e depois de
 * um tempo o peixe morde (estalo de lava e um mergulho da boia). Recolhendo logo em seguida vem um
 * dos peixes da lava, um tesouro da lava, lixo do Nether ou, às vezes, um cubo de magma ou um strider.
 */
public final class PescaLava implements Listener {

    public static final NamespacedKey K_VARA = new NamespacedKey("rpgatributos", "vara_ignea");
    private static final TextColor COR = TextColor.color(0xFF7043);
    /** Ticks que a mordida dura (para recolher). */
    private static final int JANELA = 30;

    private final RPGAtributos plugin;

    /** Boia na lava: quando morde e até quando dá para recolher (em ticks do servidor). */
    private static final class Boia {
        final UUID dono;
        long mordeEm = -1, ate = -1;
        boolean naLava;

        Boia(UUID dono) {
            this.dono = dono;
        }
    }

    private final Map<UUID, Boia> boias = new HashMap<>();
    private long tick;

    public PescaLava(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  A vara
    // =====================================================================

    public static ItemStack vara() {
        ItemStack i = new ItemStack(Material.FISHING_ROD);
        i.editMeta(m -> {
            m.itemName(Component.text("Vara Ígnea", COR));
            m.lore(List.of(
                    Component.text("Pesca na lava do Nether: a boia não afunda.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Recolha logo depois do estalo da lava.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_VARA, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    public static boolean eVara(ItemStack i) {
        return i != null && i.getType() == Material.FISHING_ROD && i.hasItemMeta() && i.getPersistentDataContainer().has(K_VARA);
    }

    public void registrarReceitas() {
        ShapelessRecipe r = new ShapelessRecipe(new NamespacedKey(plugin, "vara_ignea"), vara());
        r.addIngredient(Material.FISHING_ROD);
        r.addIngredient(new RecipeChoice.ExactChoice(Raro.CRISTAL_DE_MAGMA.criar(1)));
        r.addIngredient(new RecipeChoice.ExactChoice(Raro.CRISTAL_DE_MAGMA.criar(1)));
        r.addIngredient(Material.BLAZE_ROD);
        Bukkit.addRecipe(r);
    }

    private static boolean comVara(Player p) {
        return eVara(p.getInventory().getItemInMainHand()) || eVara(p.getInventory().getItemInOffHand());
    }

    // =====================================================================
    //  Lançar e recolher
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        Player p = e.getPlayer();
        FishHook hook = e.getHook();
        switch (e.getState()) {
            case FISHING -> {
                if (comVara(p)) boias.put(hook.getUniqueId(), new Boia(p.getUniqueId()));
            }
            case REEL_IN, FAILED_ATTEMPT, IN_GROUND -> {
                Boia b = boias.remove(hook.getUniqueId());
                if (b == null || !b.naLava) return;
                if (b.mordeEm >= 0 && tick >= b.mordeEm && tick <= b.ate) fisgar(p, hook.getLocation());
                else p.sendActionBar(Component.text("A lava só borbulhou. Espere o estalo e recolha logo.", NamedTextColor.GRAY));
            }
            default -> boias.remove(hook.getUniqueId());
        }
    }

    /** A cada tick: segura a boia em cima da lava e faz o peixe morder. */
    public void tick() {
        tick++;
        if (boias.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Boia>> it = boias.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Boia> en = it.next();
            if (!(Bukkit.getEntity(en.getKey()) instanceof FishHook hook) || !hook.isValid()) {
                it.remove();
                continue;
            }
            Boia b = en.getValue();
            Location l = hook.getLocation();
            // Pega a boia antes de ela tocar a lava (um ou dois blocos acima também contam).
            Block aqui = l.getBlock(), baixo = aqui.getRelative(0, -1, 0), baixo2 = baixo.getRelative(0, -1, 0);
            Block lava = aqui.getType() == Material.LAVA ? aqui : baixo.getType() == Material.LAVA ? baixo
                    : baixo2.getType() == Material.LAVA && hook.getVelocity().getY() < 0 ? baixo2 : null;
            if (lava != null && !b.naLava && l.getY() > lava.getY() + 1.6 && lava != aqui) lava = null; // ainda caindo de longe
            if (lava == null) {
                if (b.naLava) { // saiu da lava (puxada): para de contar
                    b.naLava = false;
                    b.mordeEm = -1;
                }
                continue;
            }
            // Boia em cima da lava: sem cair e sem pegar fogo.
            hook.setGravity(false);
            hook.setFireTicks(0);
            hook.setVisualFire(false);
            double topo = lava.getY() + 0.92;
            boolean mordendo = b.mordeEm >= 0 && tick >= b.mordeEm && tick <= b.ate;
            double alvoY = mordendo ? topo - 0.25 : topo + Math.sin(tick / 6.0) * 0.04;
            hook.setVelocity(new Vector(0, 0, 0));
            if (Math.abs(l.getY() - alvoY) > 0.02) hook.teleport(new Location(l.getWorld(), l.getX(), alvoY, l.getZ()));
            if (!b.naLava) {
                b.naLava = true;
                agendar(b);
                l.getWorld().playSound(l, Sound.BLOCK_LAVA_POP, 0.6f, 1.4f);
            }
            if (tick == b.mordeEm) {
                World w = l.getWorld();
                w.spawnParticle(Particle.LAVA, l, 8, 0.2, 0.1, 0.2, 0);
                w.spawnParticle(Particle.FLAME, l, 10, 0.25, 0.1, 0.25, 0.02);
                w.playSound(l, Sound.BLOCK_LAVA_POP, 1.2f, 0.7f);
                w.playSound(l, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.6f, 0.5f);
            } else if (tick > b.ate && b.mordeEm >= 0) {
                agendar(b); // escapou: espera outra mordida
            } else if (tick % 10 == 0) {
                l.getWorld().spawnParticle(Particle.SMOKE, l.clone().add(0, 0.2, 0), 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    private void agendar(Boia b) {
        Player p = Bukkit.getPlayer(b.dono);
        int nivel = p == null ? 0 : plugin.stats().getNivel(p, Skill.PESCA);
        double f = Math.max(0.4, 1 - nivel * plugin.settings().pesIscaRapida);
        int espera = (int) Math.round((120 + ThreadLocalRandom.current().nextInt(200)) * f);
        b.mordeEm = tick + espera;
        b.ate = b.mordeEm + JANELA;
    }

    // =====================================================================
    //  O que vem
    // =====================================================================

    private void fisgar(Player p, Location onde) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int nivel = plugin.stats().getNivel(p, Skill.PESCA);
        double fracao = Math.min(1, nivel / (double) plugin.settings().nivelMaximo);
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        var titulos = plugin.titulos();
        titulos.registrar(p, "peixes", 1);
        titulos.registrar(p, "peixes_lava", 1);
        double x = r.nextDouble();
        // Criatura: um cubo de magma (ou um strider perdido) sai da lava.
        if (x < 0.06 && p.getWorld().getDifficulty() != org.bukkit.Difficulty.PEACEFUL) {
            if (r.nextDouble() < 0.7) {
                onde.getWorld().spawn(onde, MagmaCube.class, m -> m.setSize(1 + r.nextInt(2)));
                p.sendActionBar(Component.text("Um cubo de magma veio na linha!", COR));
            } else {
                onde.getWorld().spawn(onde, Strider.class);
                p.sendActionBar(Component.text("Um strider perdido subiu da lava.", COR));
            }
            onde.getWorld().playSound(onde, Sound.ENTITY_MAGMA_CUBE_JUMP, 1f, 0.7f);
            return;
        }
        ItemStack pego;
        double xp;
        if (x < 0.06 + 0.12 + fracao * 0.06) { // tesouro da lava
            pego = tesouro(r);
            xp = plugin.settings().pesXpTesouro * 1.5;
            titulos.registrar(p, "tesouros", 1);
            p.showTitle(net.kyori.adventure.title.Title.title(Component.text("✦ Tesouro da Lava!", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Algo derretido veio na linha", COR)));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        } else if (x < 0.75) { // peixe da lava
            PeixeRaro pr = sortear(onde, nivel);
            Qualidade q = Qualidade.sortear(fracao);
            pego = pr.criar(1, q);
            PesoPeixe.pesar(plugin, p, pego, pr, q, fracao);
            xp = pr.xp();
            titulos.registrar(p, "peixes_raros", 1);
            p.sendMessage(Component.text("🔥 Você pescou um ", COR)
                    .append(Component.text(pr.nome(), pr.cor(), TextDecoration.BOLD)).append(Component.text(" na lava!", COR)));
            plugin.pesca().marcarVistoLava(p, pr);
            if (pr == PeixeRaro.SALAMANDRA_DE_LAVA) {
                Bukkit.broadcast(Component.text("🔥 ", COR).append(Component.text(p.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" pescou uma ", NamedTextColor.GRAY))
                        .append(Component.text(pr.nome(), pr.cor(), TextDecoration.BOLD))
                        .append(Component.text(" na lava do Nether!", NamedTextColor.GRAY)));
            }
        } else { // lixo do Nether
            Material[] lixo = {Material.GOLD_NUGGET, Material.QUARTZ, Material.MAGMA_CREAM, Material.BLAZE_POWDER, Material.COAL, Material.BASALT};
            pego = new ItemStack(lixo[r.nextInt(lixo.length)], 1 + r.nextInt(3));
            xp = plugin.settings().pesXpLixo;
        }
        if (!criativo) plugin.stats().darXp(p, Skill.PESCA, xp);
        // O item voa da lava até o jogador, como na pesca normal (e não queima no caminho).
        Item item = onde.getWorld().dropItem(onde.clone().add(0, 0.5, 0), pego);
        item.setInvulnerable(true);
        Vector v = p.getLocation().toVector().subtract(onde.toVector());
        double d = v.length();
        item.setVelocity(new Vector(v.getX() * 0.1, v.getY() * 0.1 + Math.sqrt(d) * 0.08 + 0.1, v.getZ() * 0.1));
        onde.getWorld().spawnParticle(Particle.LAVA, onde, 6, 0.2, 0.1, 0.2, 0);
        onde.getWorld().playSound(onde, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
    }

    private PeixeRaro sortear(Location onde, int nivel) {
        String bioma = onde.getWorld().getBiome(onde.getBlockX(), onde.getBlockY(), onde.getBlockZ()).getKey().getKey();
        int max = plugin.settings().nivelMaximo;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        java.util.List<PeixeRaro> possiveis = new java.util.ArrayList<>();
        int total = 0;
        for (PeixeRaro pr : PeixeRaro.values()) {
            if (!pr.daLava() || nivel < pr.nivelNecessario(max)) continue;
            if (pr == PeixeRaro.ENGUIA_DE_MAGMA && !bioma.contains("basalt")) continue;
            if (pr == PeixeRaro.CARPA_DE_ALMAS && !bioma.contains("soul")) continue;
            possiveis.add(pr);
            total += pr.peso();
        }
        if (total <= 0) return PeixeRaro.PEIXE_BRASA;
        int x = r.nextInt(total);
        for (PeixeRaro pr : possiveis) {
            x -= pr.peso();
            if (x < 0) return pr;
        }
        return PeixeRaro.PEIXE_BRASA;
    }

    private static ItemStack tesouro(ThreadLocalRandom r) {
        double x = r.nextDouble();
        if (x < 0.02) return new ItemStack(Material.ANCIENT_DEBRIS);
        if (x < 0.06) return new ItemStack(Material.NETHERITE_SCRAP);
        if (x < 0.22) return Raro.CRISTAL_DE_MAGMA.criar(1 + r.nextInt(2));
        if (x < 0.32) return Raro.AMBAR_INFERNAL.criar(1);
        if (x < 0.40) return Raro.QUARTZO_NEGRO.criar(1);
        if (x < 0.52) {
            ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
            livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(
                    r.nextBoolean() ? Enchantment.FIRE_PROTECTION : Enchantment.FIRE_ASPECT, 1 + r.nextInt(2), true));
            return livro;
        }
        if (x < 0.62) return new ItemStack(Material.GOLD_BLOCK);
        return new ItemStack(Material.GOLD_INGOT, 3 + r.nextInt(6));
    }
}
