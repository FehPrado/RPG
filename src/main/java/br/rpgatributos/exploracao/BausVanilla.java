package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Itens do plugin nos baús das estruturas do jogo (2.61): vilas, templos, minas, fortalezas, cidades
 * do End e as outras ganham, com chance, coisas do RPGAtributos que combinam com o lugar (minerais,
 * relíquias, tomos, cartas, páginas de lenda, fragmentos). Só no primeiro saque de cada baú.
 */
public final class BausVanilla implements Listener {

    private record Extra(double chance, Supplier<ItemStack> item) { }

    private final RPGAtributos plugin;

    public BausVanilla(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static Supplier<ItemStack> raro(Raro... opcoes) {
        return () -> opcoes[rnd().nextInt(opcoes.length)].criar(1);
    }

    private static Supplier<ItemStack> carta() {
        return () -> br.rpgatributos.vida.Album.carta(br.rpgatributos.vida.Album.sortear(rnd(), false), false);
    }

    private Supplier<ItemStack> reliquia() {
        return () -> plugin.arqueologia().reliquiaAleatoria();
    }

    private static Supplier<ItemStack> tomo() {
        return br.rpgatributos.arcano.ItensMagicos::tomoAleatorio;
    }

    /** O que cada tipo de baú pode ganhar (pelo caminho da tabela de saque do jogo). */
    private List<Extra> extras(String tabela) {
        if (tabela.startsWith("chests/village/")) {
            return List.of(new Extra(0.25, carta()), new Extra(0.10, raro(Raro.AMBAR, Raro.JADE, Raro.PEROLA)));
        }
        return switch (tabela) {
            case "chests/simple_dungeon", "chests/abandoned_mineshaft" -> List.of(new Extra(0.15, raro(Raro.AMBAR, Raro.JADE, Raro.CRISTAL_DE_GELO)),
                    new Extra(0.10, raro(Raro.MAPA_RASGADO)), new Extra(0.06, reliquia()));
            case "chests/desert_pyramid", "chests/jungle_temple" -> List.of(new Extra(0.30, reliquia()), new Extra(0.20, raro(Raro.AMBAR, Raro.JADE)),
                    new Extra(0.08, tomo()));
            case "chests/stronghold_library", "chests/stronghold_corridor", "chests/stronghold_crossing" -> List.of(
                    new Extra(0.20, raro(Raro.PAGINA_DE_LENDA)), new Extra(0.20, tomo()), new Extra(0.10, reliquia()));
            case "chests/shipwreck_treasure", "chests/shipwreck_supply", "chests/shipwreck_map", "chests/buried_treasure",
                 "chests/underwater_ruin_big", "chests/underwater_ruin_small" -> List.of(new Extra(0.30, raro(Raro.PEROLA)), new Extra(0.12, carta()));
            case "chests/nether_bridge", "chests/bastion_treasure", "chests/bastion_other", "chests/bastion_bridge", "chests/bastion_hoglin_stable" ->
                    List.of(new Extra(0.30, raro(Raro.CRISTAL_DE_MAGMA, Raro.AMBAR_INFERNAL, Raro.QUARTZO_NEGRO, Raro.PEDRA_SANGUE)),
                            new Extra(0.10, raro(Raro.FRAGMENTO_DE_FORJA)));
            case "chests/ruined_portal" -> List.of(new Extra(0.25, raro(Raro.QUARTZO_NEGRO, Raro.CRISTAL_DE_MAGMA)));
            case "chests/end_city_treasure" -> List.of(new Extra(0.25, raro(Raro.FRAGMENTO_CELESTE)), new Extra(0.15, tomo()),
                    new Extra(0.10, raro(Raro.PAGINA_DE_LENDA)));
            case "chests/ancient_city", "chests/ancient_city_ice_box" -> List.of(new Extra(0.20, tomo()), new Extra(0.20, reliquia()));
            case "chests/woodland_mansion" -> List.of(new Extra(0.25, tomo()), new Extra(0.15, carta()));
            case "chests/pillager_outpost" -> List.of(new Extra(0.20, carta()), new Extra(0.10, raro(Raro.MAPA_RASGADO)));
            case "chests/igloo_chest" -> List.of(new Extra(0.35, raro(Raro.CRISTAL_DE_GELO)));
            default -> tabela.startsWith("chests/trial_chambers/") ? List.of(new Extra(0.12, raro(Raro.FRAGMENTO_DE_FORJA))) : List.of();
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGerarSaque(LootGenerateEvent e) {
        if (!plugin.settings().bausVanilla) return;
        var chave = e.getLootTable().getKey();
        if (!chave.getNamespace().equals("minecraft")) return;
        List<Extra> l = extras(chave.getKey());
        if (l.isEmpty()) return;
        List<ItemStack> saque = new java.util.ArrayList<>(e.getLoot());
        boolean mudou = false;
        for (Extra x : l) {
            if (rnd().nextDouble() >= x.chance()) continue;
            ItemStack i = x.item().get();
            if (i != null && !i.isEmpty()) {
                saque.add(i);
                mudou = true;
            }
        }
        if (mudou) e.setLoot(saque);
    }
}
