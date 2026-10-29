package br.rpgatributos.ajuda;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.combo.Arsenal;
import br.rpgatributos.combo.TipoArma;
import br.rpgatributos.exploracao.Mineracao;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.forja.Nomeada;
import br.rpgatributos.forja.Traje;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dicas na hora certa: na primeira vez que o jogador segura, pega ou encontra algo novo, uma dica
 * curta aparece no chat com um "[ver como]" que abre o assunto do Guia. Cada dica aparece uma vez
 * só por jogador (no máximo uma a cada 15 s) e {@code /guia dicas} desliga ou liga todas.
 */
public final class Dicas implements Listener {

    private static final TextColor COR = TextColor.color(0xFFE082);
    private static final long INTERVALO_MS = 15_000;

    /** Uma dica: id (para não repetir), o texto e o assunto do Guia que ela abre. */
    public record Dica(String id, String texto, String topico) { }

    private final RPGAtributos plugin;
    private final NamespacedKey kVistas, kDesligadas;
    private final Map<UUID, Long> ultima = new HashMap<>();

    public Dicas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kVistas = new NamespacedKey(plugin, "dicas_vistas");
        this.kDesligadas = new NamespacedKey(plugin, "dicas_desligadas");
    }

    // =====================================================================
    //  O que é esse item?
    // =====================================================================

    /** A dica (e o assunto do Guia) de um item, ou null se ele não tem nada a ensinar. */
    public static Dica doItem(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        Arsenal.Arma a = Arsenal.de(i);
        if (Nomeada.de(i) != null) {
            return new Dica("nomeada", "Arma com nome! Só muda a aparência; o nome e a história ficam ao refinar e reforjar.", "nomeadas");
        }
        if (Traje.de(i) != null) {
            return new Dica("traje", "Peça de um Traje: armadura com nome e textura própria no corpo. Junte as quatro!", "nomeadas");
        }
        if (a != null) {
            String combo = a.tipo().inicio() == 'D' ? "direito" : "esquerdo";
            return new Dica("arma_" + a.id, a.nome + ": " + a.descricao + " Combos: 3 cliques começando pelo " + combo + ".",
                    a.deMinerio() ? "armas_minerios" : "arsenal");
        }
        if (i.getPersistentDataContainer().has(Mineracao.K_TURFA)) {
            return new Dica("turfa", "Turfa: queima bem mais que o carvão, faz a Clava Fumegante e pode ser incrustada numa arma.", "minerais");
        }
        Raro r = Raro.de(i);
        if (r != null) {
            switch (r) {
                case JADE, AMBAR, PEROLA, CRISTAL_DE_GELO, PEDRA_SANGUE -> {
                    return new Dica("mineral", "Mineral de bioma! Serve para acessórios, para as armas dos minérios e para incrustar numa arma na Forja.", "minerais");
                }
                case COBRE_ANTIGO, CRISTAL_ABISSAL, ESPORO_FOSSIL, FERRO_NEGRO -> {
                    return new Dica("mineral_abismo", "Mineral do Abismo! Faz um conjunto de armadura e ferramentas na bancada, como o ferro, com bônus próprio.", "abismo");
                }
                case CRISTAL_DE_MAGMA, AMBAR_INFERNAL, QUARTZO_NEGRO -> {
                    return new Dica("mineral_nether", "Mineral do Nether! Incruste numa arma na Forja; o Cristal de Magma faz a Lança de Basalto e a Vara Ígnea.", "nether");
                }
                default -> { }
            }
        }
        if (Mitrilo.ehLingote(i)) {
            return new Dica("mitrilo", "Lingote de Mitrilo: aprimora a netherite e faz o Montante, a arma mais pesada do arsenal.", "mitrilo");
        }
        if (br.rpgatributos.domador.Linhagens.ehLivro(i)) {
            return new Dica("livro_criador", "Livro do Criador: clique num cavalo para ver a ficha, a linhagem e a árvore da família.", "linhagens");
        }
        if (br.rpgatributos.vida.Cultivo.daSemente(i) != null) {
            return new Dica("semente", "Semente da estação: só cresce na estação dela (ou numa estufa com teto de vidro).", "plantacoes");
        }
        var mo = br.rpgatributos.oficio.MaterialOficio.de(i);
        if (mo != null && mo.tipo() == br.rpgatributos.oficio.MaterialOficio.Tipo.PELE) {
            return new Dica("pele", "Pele de criatura: no Ateliê do Curtidor ela vira couro para uma armadura leve.", "atelie");
        }
        TipoArma t = TipoArma.de(i);
        if (t != null) {
            return new Dica("combos", "Com uma arma na mão, 3 cliques seguidos soltam um golpe de combo. Veja e troque os golpes no /combos.", "combos");
        }
        return null;
    }

    // =====================================================================
    //  Mostrar
    // =====================================================================

    public boolean desligadas(Player p) {
        return p.getPersistentDataContainer().has(kDesligadas);
    }

    /** /guia dicas: liga ou desliga. @return true se ficaram ligadas. */
    public boolean alternar(Player p) {
        if (desligadas(p)) {
            p.getPersistentDataContainer().remove(kDesligadas);
            return true;
        }
        p.getPersistentDataContainer().set(kDesligadas, PersistentDataType.BYTE, (byte) 1);
        return false;
    }

    private Set<String> vistas(Player p) {
        String s = p.getPersistentDataContainer().get(kVistas, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    /** Mostra a dica se ela ainda não apareceu para esse jogador (e se não apareceu outra há pouco). */
    public void mostrar(Player p, Dica d) {
        if (d == null || desligadas(p) || Topicos.porId(d.topico()) == null) return;
        Set<String> v = vistas(p);
        if (v.contains(d.id())) return;
        long agora = System.currentTimeMillis();
        if (agora - ultima.getOrDefault(p.getUniqueId(), 0L) < INTERVALO_MS) return; // volta a tentar depois
        ultima.put(p.getUniqueId(), agora);
        v.add(d.id());
        p.getPersistentDataContainer().set(kVistas, PersistentDataType.STRING, String.join(",", v));
        p.sendMessage(Component.text("💡 ", COR).append(Component.text(d.texto(), NamedTextColor.WHITE))
                .append(Component.text(" [ver como]", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/guia " + d.topico()))
                        .hoverEvent(HoverEvent.showText(Component.text("Abre o Guia neste assunto", NamedTextColor.GRAY)))));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.6f);
    }

    // =====================================================================
    //  Quando aparecem
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTrocarDeItem(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        mostrar(p, doItem(p.getInventory().getItem(e.getNewSlot())));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPegar(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack i = e.getItem().getItemStack();
        Bukkit.getScheduler().runTask(plugin, () -> mostrar(p, doItem(i)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMontar(EntityMountEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(e.getMount() instanceof AbstractHorse)) return;
        mostrar(p, new Dica("cavalo", "Cavalos guardam pai, mãe e linhagem; o Livro do Criador mostra a ficha e o Estábulo guarda até 4 montarias.", "linhagens"));
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ultima.remove(e.getPlayer().getUniqueId());
    }

    /** A cada 3 s: dicas de lugar (abaixo do y 0, lá no alto...). */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            double y = p.getLocation().getY();
            if (y < 0) {
                mostrar(p, new Dica("fundo", "Você chegou às Profundezas: procure as Raízes de Luz adormecidas e cuidado com a Penumbra.", "profundezas"));
            } else if (y > 200) {
                mostrar(p, new Dica("alto", "Lá no alto ficam as Ilhas do Céu: as Plataformas de Vento no chão lançam você até elas.", "ceu"));
            }
        }
    }
}
