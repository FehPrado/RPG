package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Diário de Viagem (/diario): um livro que se escreve sozinho com os marcos de cada jogador —
 * primeiro diamante, primeiro chefe, títulos, níveis, renascimentos... com a data de cada um.
 */
public final class Diario implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x6D4C41);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int POR_PAGINA = 3;

    /** Primeira vez que um contador dos títulos chega a 1 → texto do marco. */
    private static final Map<String, String> PRIMEIRAS = Map.ofEntries(
            Map.entry("masmorras", "Venceu a primeira masmorra"),
            Map.entry("forjados", "Forjou o primeiro item na Forja do Ferreiro"),
            Map.entry("forjado_lendario", "Forjou o primeiro item Lendário"),
            Map.entry("forjado_mitico", "Forjou o primeiro item Mítico"),
            Map.entry("magias", "Lançou a primeira magia"),
            Map.entry("tesouros_enterrados", "Desenterrou o primeiro tesouro de mapa"),
            Map.entry("reliquias", "Achou a primeira relíquia"),
            Map.entry("constelacoes", "Observou a primeira constelação"),
            Map.entry("campanha", "Completou o primeiro capítulo das Crônicas"),
            Map.entry("elites", "Derrotou o primeiro Elite"),
            Map.entry("portais", "Fechou o primeiro portal"),
            Map.entry("peixes_raros", "Pescou o primeiro peixe raro"),
            Map.entry("lendas", "Forjou a primeira lenda"),
            Map.entry("classes", "Aprendeu a primeira classe"),
            Map.entry("mitrilo", "Minerou o primeiro mitrilo"),
            Map.entry("milagres", "Recebeu o primeiro milagre"),
            Map.entry("selos", "Rompeu o selo de um Local Oculto"),
            Map.entry("companheiros", "Ganhou o primeiro companheiro"),
            Map.entry("guerras_vencidas", "Venceu a primeira guerra"),
            Map.entry("runas", "Gravou a primeira runa"),
            Map.entry("trofeus", "Expôs o primeiro troféu"));

    private final RPGAtributos plugin;
    private final NamespacedKey kEntradas, kIds;

    public Diario(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kEntradas = new NamespacedKey(plugin, "diario");
        this.kIds = new NamespacedKey(plugin, "diario_ids");
    }

    private Set<String> ids(Player p) {
        String s = p.getPersistentDataContainer().get(kIds, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    /** Anota um marco (uma vez só por id). */
    public void marco(Player p, String id, String texto) {
        Set<String> feitos = ids(p);
        if (!feitos.add(id)) return;
        p.getPersistentDataContainer().set(kIds, PersistentDataType.STRING, String.join(",", feitos));
        String antes = p.getPersistentDataContainer().getOrDefault(kEntradas, PersistentDataType.STRING, "");
        String linha = LocalDate.now().format(DATA) + "|" + texto.replace("\n", " ");
        p.getPersistentDataContainer().set(kEntradas, PersistentDataType.STRING, antes.isEmpty() ? linha : antes + "\n" + linha);
        p.sendActionBar(Component.text("📖 Diário: " + texto, COR));
    }

    /** Chamado pelos títulos quando um contador muda. */
    public void aoContar(Player p, String id, int valor) {
        String t = PRIMEIRAS.get(id);
        if (t != null && valor >= 1) marco(p, "primeiro_" + id, t);
    }

    // =====================================================================
    //  Marcos que o diário percebe sozinho
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMinerar(BlockBreakEvent e) {
        Material m = e.getBlock().getType();
        if (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE) marco(e.getPlayer(), "diamante", "Encontrou o primeiro diamante");
        else if (m == Material.ANCIENT_DEBRIS) marco(e.getPlayer(), "netherita", "Encontrou o primeiro detrito ancestral");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoTrocarDeMundo(PlayerChangedWorldEvent e) {
        World.Environment amb = e.getPlayer().getWorld().getEnvironment();
        if (amb == World.Environment.NETHER) marco(e.getPlayer(), "nether", "Entrou no Nether pela primeira vez");
        else if (amb == World.Environment.THE_END) marco(e.getPlayer(), "end", "Chegou ao End");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        if (Chefes.ehChefe(e.getEntity())) marco(p, "chefe", "Derrotou o primeiro chefe");
        if (e.getEntity().getType() == org.bukkit.entity.EntityType.ENDER_DRAGON) marco(p, "dragao", "Derrotou o Dragão do End");
        if (e.getEntity().getType() == org.bukkit.entity.EntityType.WITHER) marco(p, "wither", "Derrotou o Wither");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(PlayerDeathEvent e) {
        marco(e.getPlayer(), "morte", "Morreu pela primeira vez");
    }

    // =====================================================================
    //  O livro
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        String txt = p.getPersistentDataContainer().getOrDefault(kEntradas, PersistentDataType.STRING, "");
        List<Component> paginas = new ArrayList<>();
        paginas.add(Component.text("Diário de Viagem\n", COR, TextDecoration.BOLD)
                .append(Component.text("de " + p.getName() + "\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text(txt.isEmpty() ? "As páginas ainda estão em branco. Vá viver uma aventura!"
                        : "Os marcos da sua jornada, um por um.", NamedTextColor.BLACK)));
        if (!txt.isEmpty()) {
            String[] linhas = txt.split("\n");
            Component pagina = Component.empty();
            int n = 0;
            for (String l : linhas) {
                String[] d = l.split("\\|", 2);
                if (d.length < 2) continue;
                pagina = pagina.append(Component.text(d[0] + "\n", NamedTextColor.DARK_GRAY))
                        .append(Component.text(d[1] + "\n\n", NamedTextColor.BLACK));
                if (++n == POR_PAGINA) {
                    paginas.add(pagina);
                    pagina = Component.empty();
                    n = 0;
                }
            }
            if (n > 0) paginas.add(pagina);
        }
        p.openBook(Book.book(Component.text("Diário de Viagem"), Component.text(p.getName()), paginas));
    }
}
