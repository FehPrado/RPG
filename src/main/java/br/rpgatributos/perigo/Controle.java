package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Equilíbrio de criaturas: limite de monstros por jogador em cada mundo, limpeza das
 * criaturas temporárias do plugin que ficaram gravadas no mundo (hordas, lobisomens,
 * lacaios...) e um relatório para o admin achar onde há criatura demais.
 */
public final class Controle implements Listener {

    /** Marcas das criaturas que só existem durante um evento: se aparecerem num chunk carregado do disco, sobraram. */
    private static final Set<String> TEMPORARIAS = Set.of("horda", "ninho_guardiao", "criatura_maldita", "elite_lacaio",
            "portal_monstro", "torre_mob", "onda", "lacaio", "tesouro_guardiao", "encontro", "mercador_itinerante", "guarda_estrutura");
    /** Marcas de criaturas que nunca devem ser limpas. */
    private static final Set<String> PROTEGIDAS = Set.of("companheiro_dono", "sombra_dono", "invocacao_dono", "chefe", "uniforme", "eremita");

    private final RPGAtributos plugin;
    private int limpasAoCarregar;

    public Controle(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static boolean marcada(Entity e, Set<String> chaves) {
        for (NamespacedKey k : e.getPersistentDataContainer().getKeys()) {
            if (k.getNamespace().equals("rpgatributos") && chaves.contains(k.getKey())) return true;
        }
        return false;
    }

    public static boolean temporaria(Entity e) {
        return marcada(e, TEMPORARIAS);
    }

    // =====================================================================
    //  Limites de spawn
    // =====================================================================

    public void aplicarLimites() {
        for (World w : Bukkit.getWorlds()) aplicar(w);
    }

    private void aplicar(World w) {
        if (plugin.masmorras().ehMundo(w)) return;
        int monstros = plugin.settings().criLimiteMonstros, animais = plugin.settings().criLimiteAnimais;
        if (monstros >= 0) w.setSpawnLimit(SpawnCategory.MONSTER, monstros);
        if (animais >= 0) w.setSpawnLimit(SpawnCategory.ANIMAL, animais);
    }

    @EventHandler
    public void aoCarregarMundo(WorldLoadEvent e) {
        aplicar(e.getWorld());
    }

    // =====================================================================
    //  Limpeza
    // =====================================================================

    /** Criaturas de evento nunca são gravadas; se uma veio do disco, é sobra de uma versão antiga: some. */
    @EventHandler(priority = EventPriority.LOW)
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            if (en instanceof Player || !temporaria(en)) continue;
            en.remove();
            limpasAoCarregar++;
        }
    }

    /** A cada 5 minutos: avisa no console quantas sobras foram limpas. */
    public void relatarLimpeza() {
        if (limpasAoCarregar <= 0) return;
        plugin.getLogger().info(limpasAoCarregar + " criatura(s) que sobraram de eventos antigos foram removidas.");
        limpasAoCarregar = 0;
    }

    /** Monstro sem nome que nunca some sozinho e não pertence a ninguém. */
    private boolean presa(Entity e) {
        return e instanceof Monster m && !m.getRemoveWhenFarAway() && m.customName() == null && !marcada(e, PROTEGIDAS)
                && plugin.colonias().colonoDe(e) == null;
    }

    /** Admin: remove as sobras de eventos e os monstros presos (que nunca somem). @return quantos removeu. */
    public int limpar() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            if (plugin.masmorras().ehMundo(w)) continue;
            for (Entity e : w.getEntities()) {
                if (e instanceof Player) continue;
                // Sobras de evento gravadas no disco (as atuais nunca são gravadas) e monstros presos.
                if ((temporaria(e) && e.isPersistent()) || presa(e)) {
                    e.remove();
                    n++;
                }
            }
        }
        return n;
    }

    /** Admin: quantas criaturas há em cada mundo e os chunks mais cheios. */
    public void relatorio(CommandSender s) {
        s.sendMessage(Component.text("☠ Criaturas carregadas agora:", NamedTextColor.GOLD));
        for (World w : Bukkit.getWorlds()) {
            int vivos = 0, monstros = 0, animais = 0, presas = 0, temporarias = 0;
            Map<Chunk, Integer> porChunk = new HashMap<>();
            for (LivingEntity e : w.getLivingEntities()) {
                if (e instanceof Player) continue;
                vivos++;
                if (e instanceof Monster) monstros++;
                if (e instanceof Animals) animais++;
                if (presa(e)) presas++;
                if (temporaria(e) && e.isPersistent()) temporarias++;
                porChunk.merge(e.getChunk(), 1, Integer::sum);
            }
            int jogadores = w.getPlayers().size();
            s.sendMessage(Component.text(" • " + w.getName() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(vivos + " criaturas (" + monstros + " monstros, " + animais + " animais) · "
                            + jogadores + " jogador(es) · limite de monstros " + w.getSpawnLimit(SpawnCategory.MONSTER) + " por jogador",
                            NamedTextColor.WHITE)));
            if (presas + temporarias > 0) {
                s.sendMessage(Component.text("   " + presas + " monstro(s) preso(s) que nunca somem, " + temporarias
                        + " sobra(s) de evento → /rpgadmin limparcriaturas", NamedTextColor.RED));
            }
            List<Map.Entry<Chunk, Integer>> top = new ArrayList<>(porChunk.entrySet());
            top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                Chunk c = top.get(i).getKey();
                if (top.get(i).getValue() < 15) break;
                s.sendMessage(Component.text("   chunk perto de " + (c.getX() * 16 + 8) + ", " + (c.getZ() * 16 + 8) + ": "
                        + top.get(i).getValue() + " criaturas", NamedTextColor.GRAY));
            }
        }
    }
}
