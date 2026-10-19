package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Receitas do plugin no livro de receitas do jogo (2.61), como as do vanilla: cada uma aparece no
 * livro quando o jogador tem no inventário um ingrediente que importa (graveto, tábua e linha não
 * contam; ingrediente do plugin, como o Âmbar, precisa ser o item do plugin mesmo).
 */
public final class LivroReceitas implements Listener {

    private static final java.util.Set<Material> COMUNS = java.util.Set.of(Material.STICK, Material.STRING, Material.COBBLESTONE);

    private final RPGAtributos plugin;
    /** Receita do plugin → ingredientes que a revelam. */
    private final Map<NamespacedKey, List<RecipeChoice>> receitas = new LinkedHashMap<>();
    private int vez;

    public LivroReceitas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Lê as receitas do plugin (chamado depois que todas foram registradas). */
    public void iniciar() {
        receitas.clear();
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            Recipe r = it.next();
            if (!(r instanceof Keyed k) || !k.getKey().getNamespace().equals("rpgatributos")) continue;
            List<RecipeChoice> l = new ArrayList<>();
            if (r instanceof ShapedRecipe s) l.addAll(s.getChoiceMap().values());
            else if (r instanceof ShapelessRecipe s) l.addAll(s.getChoiceList());
            else if (r instanceof CookingRecipe<?> c) l.add(c.getInputChoice());
            l.removeIf(c -> c == null || comum(c));
            if (!l.isEmpty()) receitas.put(k.getKey(), l);
        }
        plugin.getLogger().info(receitas.size() + " receitas do plugin no livro de receitas.");
    }

    private static boolean comum(RecipeChoice c) {
        if (c instanceof RecipeChoice.MaterialChoice mc) {
            return mc.getChoices().stream().allMatch(m -> COMUNS.contains(m) || org.bukkit.Tag.PLANKS.isTagged(m) || org.bukkit.Tag.LOGS.isTagged(m));
        }
        return false;
    }

    /** A cada 5 s (chamado pelo agendador de 1 s): revela as receitas cujos ingredientes o jogador tem. */
    public void tick() {
        if (++vez % 5 != 0 || receitas.isEmpty()) return;
        for (Player p : Bukkit.getOnlinePlayers()) revelar(p);
    }

    private void revelar(Player p) {
        PlayerInventory inv = p.getInventory();
        List<NamespacedKey> novas = new ArrayList<>();
        for (Map.Entry<NamespacedKey, List<RecipeChoice>> en : receitas.entrySet()) {
            if (p.hasDiscoveredRecipe(en.getKey())) continue;
            if (temAlgum(inv, en.getValue())) novas.add(en.getKey());
        }
        if (!novas.isEmpty()) p.discoverRecipes(novas);
    }

    private static boolean temAlgum(PlayerInventory inv, List<RecipeChoice> ingredientes) {
        for (ItemStack i : inv.getContents()) {
            if (i == null || i.isEmpty()) continue;
            for (RecipeChoice c : ingredientes) if (c.test(i)) return true;
        }
        return false;
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (e.getPlayer().isOnline()) revelar(e.getPlayer());
        }, 60L);
    }
}
