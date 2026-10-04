package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Itens ligados às magias: a receita do Cajado Arcano (forjado na Forja do Ferreiro),
 * os Pergaminhos (uma magia guardada, de uso único) e os Tomos Proibidos.
 */
public final class ItensMagicos implements Listener {

    public static final NamespacedKey CHAVE_PERGAMINHO = new NamespacedKey("rpgatributos", "pergaminho");
    public static final NamespacedKey CHAVE_PERGAMINHO_POT = new NamespacedKey("rpgatributos", "pergaminho_pot");
    public static final NamespacedKey CHAVE_TOMO = new NamespacedKey("rpgatributos", "tomo");
    private static final TextColor COR_TOMO = TextColor.color(0x8B0000);

    private final RPGAtributos plugin;
    private final NamespacedKey kReceita;
    private final Map<UUID, Integer> esperando = new HashMap<>();

    public ItensMagicos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kReceita = new NamespacedKey(plugin, "cajado_arcano");
    }

    // =====================================================================
    //  Cajado
    // =====================================================================

    /** Receita do cajado: 1 ametista na ponta e 2 varas de blaze na diagonal. */
    public void registrarReceita() {
        if (Bukkit.getRecipe(kReceita) != null) return;
        ItemStack resultado = new ItemStack(Material.BREEZE_ROD);
        resultado.editMeta(m -> {
            m.itemName(Component.text("Cajado Arcano (sem forja)", NamedTextColor.GRAY));
            m.lore(List.of(Component.text("Faça numa Forja do Ferreiro para", NamedTextColor.DARK_GRAY),
                            Component.text("ele nascer com raridade e poder.", NamedTextColor.DARK_GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "cajado_arcano");
        });
        ShapedRecipe r = new ShapedRecipe(kReceita, resultado);
        r.shape("  A", " B ", "B  ");
        r.setIngredient('A', Material.AMETHYST_SHARD);
        r.setIngredient('B', Material.BLAZE_ROD);
        Bukkit.addRecipe(r);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        e.getPlayer().discoverRecipe(kReceita);
    }

    // =====================================================================
    //  Pergaminho
    // =====================================================================

    public static ItemStack criarPergaminho(Magia m, double pot, String autor) {
        ItemStack i = new ItemStack(Material.PAPER);
        Receita r = m.receita();
        i.editMeta(meta -> {
            meta.itemName(Component.text("Pergaminho: " + m.nome(), r != null ? NamedTextColor.GOLD : Arcano.COR));
            List<Component> lore = new ArrayList<>();
            StringBuilder es = new StringBuilder();
            for (Essencia e : m.essencias()) es.append(e.simbolo()).append(' ');
            lore.add(Component.text(es.toString().trim() + "  ·  " + m.forma().nome(), NamedTextColor.GRAY));
            if (!m.mods().isEmpty()) {
                List<String> nomes = new ArrayList<>();
                for (Modificador mod : m.mods()) nomes.add(mod.nome());
                lore.add(Component.text("Modificadores: " + String.join(", ", nomes), NamedTextColor.LIGHT_PURPLE));
            }
            lore.add(Component.text("Força: " + Math.round(pot * 100) + "%", NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(Component.text("Clique direito: lança a magia", NamedTextColor.YELLOW));
            lore.add(Component.text("(qualquer um pode usar, uma vez)", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("✎ Escrito por " + autor, NamedTextColor.DARK_GRAY));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(CHAVE_PERGAMINHO, PersistentDataType.STRING, m.serializar());
            meta.getPersistentDataContainer().set(CHAVE_PERGAMINHO_POT, PersistentDataType.DOUBLE, Math.round(pot * 100) / 100.0);
        });
        return i;
    }

    public static boolean ehPergaminho(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(CHAVE_PERGAMINHO);
    }

    // =====================================================================
    //  Tomo Proibido
    // =====================================================================

    public static ItemStack criarTomo(Receita r) {
        ItemStack i = new ItemStack(Material.BOOK);
        i.editMeta(meta -> {
            meta.itemName(Component.text("Tomo Proibido: " + r.nome(), COR_TOMO));
            meta.setItemModel(NamespacedKey.minecraft("enchanted_book"));
            meta.lore(List.of(
                            Component.text(r.descricao(), NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("Clique direito: aprender a magia", NamedTextColor.YELLOW),
                            Component.text("(precisa das essências no corpo para lançar)", NamedTextColor.DARK_GRAY),
                            Component.empty(),
                            Component.text("☠ Magia proibida", COR_TOMO))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(CHAVE_TOMO, PersistentDataType.STRING, r.name());
        });
        return i;
    }

    public static boolean ehTomo(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(CHAVE_TOMO);
    }

    /** Um tomo aleatório (para os tesouros). */
    public static ItemStack tomoAleatorio() {
        List<Receita> proibidas = new ArrayList<>();
        for (Receita r : Receita.values()) if (r.forma() == Forma.PROIBIDA) proibidas.add(r);
        return criarTomo(proibidas.get(Conjuracao.rnd().nextInt(proibidas.size())));
    }

    // =====================================================================
    //  Usar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (ehPergaminho(item)) {
            e.setUseItemInHand(Event.Result.DENY);
            e.setCancelled(true);
            usarPergaminho(e.getPlayer(), item);
        } else if (ehTomo(item)) {
            e.setUseItemInHand(Event.Result.DENY);
            e.setCancelled(true);
            lerTomo(e.getPlayer(), item);
        }
    }

    private void usarPergaminho(Player p, ItemStack item) {
        int agora = Bukkit.getCurrentTick();
        Integer ultimo = esperando.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < 20) return;
        esperando.put(p.getUniqueId(), agora);
        Magia m = Magia.ler(item.getPersistentDataContainer().getOrDefault(CHAVE_PERGAMINHO, PersistentDataType.STRING, ""));
        double pot = item.getPersistentDataContainer().getOrDefault(CHAVE_PERGAMINHO_POT, PersistentDataType.DOUBLE, 1.0);
        if (m == null) return;
        if (!plugin.arcano().conjuracao().lancarPergaminho(p, m, pot)) return;
        item.setAmount(item.getAmount() - 1);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 30, 0.4, 0.4, 0.4, 0.5);
    }

    private void lerTomo(Player p, ItemStack item) {
        Receita r;
        try {
            r = Receita.valueOf(item.getPersistentDataContainer().getOrDefault(CHAVE_TOMO, PersistentDataType.STRING, ""));
        } catch (IllegalArgumentException ex) {
            return;
        }
        Arcano arc = plugin.arcano();
        Perfil pf = arc.perfil(p);
        boolean nova = pf.descobertas.add(r);
        boolean noGrimorio = false;
        for (int i = 0; i < Perfil.MAX_MAGIAS; i++) {
            Magia m = pf.magia(i);
            if (m != null && m.receita() == r) noGrimorio = true;
        }
        if (!nova && noGrimorio) {
            p.sendActionBar(Component.text("Você já conhece essa magia e ela já está no grimório.", NamedTextColor.RED));
            return;
        }
        if (!noGrimorio) {
            int livre = pf.espacoLivre(arc.espacosGrimorio(p));
            if (livre >= 0) pf.magias[livre] = new Magia(r.essencias(), r.forma(), r.nome());
            else p.sendMessage(Component.text("Grimório cheio: abra espaço e coloque a magia pelo menu de Magias Secretas.", NamedTextColor.YELLOW));
        }
        arc.salvar(p);
        item.setAmount(item.getAmount() - 1);
        p.showTitle(Title.title(Component.text("☠ " + r.nome() + " ☠", COR_TOMO, TextDecoration.BOLD),
                Component.text("Uma magia proibida agora é sua", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2600), Duration.ofMillis(700))));
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.4f);
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.05);
        if (nova) plugin.titulos().definirMinimo(p, "secretas", pf.descobertas().size());
    }
}
