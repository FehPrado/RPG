package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Estante viva (Vanilla+, 2.58): olhando para um livro numa estante entalhada, a barra mostra o título
 * e o autor (ou os encantamentos do livro encantado). Agachado + clique direito no livro abre para ler,
 * sem tirar da estante. Assim a estante vira uma biblioteca de verdade.
 */
public final class Estantes implements Listener {

    private static final TextColor COR = TextColor.color(0xC8A26B);
    private final RPGAtributos plugin;
    /** O que cada jogador estava vendo (para não repetir a barra sem mudar). */
    private final Map<UUID, String> vendo = new HashMap<>();
    private int ticks;

    public Estantes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** O livro para onde o jogador olha numa estante entalhada (ou null). */
    private static ItemStack livroMirado(Player p, RayTraceResult r) {
        if (r == null || r.getHitBlock() == null || r.getHitBlock().getType() != Material.CHISELED_BOOKSHELF) return null;
        Block b = r.getHitBlock();
        if (!(b.getState(false) instanceof ChiseledBookshelf est)) return null;
        Vector rel = r.getHitPosition().clone().subtract(b.getLocation().toVector());
        int slot = est.getSlot(rel);
        if (slot < 0 || slot >= est.getInventory().getSize()) return null;
        ItemStack i = est.getInventory().getItem(slot);
        return i == null || i.getType().isAir() ? null : i;
    }

    /** A cada tick: mostra o livro mirado (a cada 5 ticks por jogador). */
    public void tick() {
        if (!plugin.settings().vanillaEstantes || ++ticks % 5 != 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            RayTraceResult r = p.rayTraceBlocks(5);
            ItemStack livro = livroMirado(p, r);
            String texto = livro == null ? null : descrever(livro);
            String antes = vendo.get(p.getUniqueId());
            if (texto == null) {
                if (antes != null) vendo.remove(p.getUniqueId());
                continue;
            }
            if (texto.equals(antes) && ticks % 40 != 0) continue;
            vendo.put(p.getUniqueId(), texto);
            p.sendActionBar(Component.text(texto, COR));
        }
    }

    private static String descrever(ItemStack i) {
        return switch (i.getType()) {
            case WRITTEN_BOOK -> {
                BookMeta m = (BookMeta) i.getItemMeta();
                String titulo = m.hasTitle() ? PlainTextComponentSerializer.plainText().serialize(m.title()) : "Livro sem título";
                String autor = m.hasAuthor() ? PlainTextComponentSerializer.plainText().serialize(m.author()) : "?";
                yield "📖 " + titulo + " — " + autor + "  (agache + clique para ler)";
            }
            case WRITABLE_BOOK -> {
                BookMeta m = (BookMeta) i.getItemMeta();
                yield "✎ Rascunho (" + m.getPageCount() + (m.getPageCount() == 1 ? " página" : " páginas") + ")  (agache + clique para ler)";
            }
            case ENCHANTED_BOOK -> {
                if (!(i.getItemMeta() instanceof EnchantmentStorageMeta m) || m.getStoredEnchants().isEmpty()) yield "✦ Livro encantado";
                List<String> l = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> en : m.getStoredEnchants().entrySet()) {
                    l.add(PlainTextComponentSerializer.plainText().serialize(en.getKey().displayName(en.getValue())));
                }
                yield "✦ " + String.join(", ", l);
            }
            case BOOK -> "📕 Livro em branco";
            case KNOWLEDGE_BOOK -> "📗 Livro do conhecimento";
            default -> null;
        };
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoLer(PlayerInteractEvent e) {
        if (!plugin.settings().vanillaEstantes || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.useInteractedBlock() == Event.Result.DENY) return;
        Player p = e.getPlayer();
        if (!p.isSneaking() || e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.CHISELED_BOOKSHELF) return;
        ItemStack livro = livroMirado(p, p.rayTraceBlocks(5));
        if (livro == null) return;
        if (livro.getType() == Material.WRITTEN_BOOK) {
            e.setCancelled(true);
            p.openBook(livro.clone());
        } else if (livro.getType() == Material.WRITABLE_BOOK) {
            // O rascunho abre como um livro só de leitura (a estante continua com o original).
            e.setCancelled(true);
            BookMeta original = (BookMeta) livro.getItemMeta();
            ItemStack leitura = new ItemStack(Material.WRITTEN_BOOK);
            leitura.editMeta(BookMeta.class, m -> {
                m.title(Component.text("Rascunho"));
                m.author(Component.text("?"));
                m.pages(original.pages().isEmpty() ? List.of(Component.text("(em branco)", NamedTextColor.GRAY)) : original.pages());
            });
            p.openBook(leitura);
        }
    }
}
