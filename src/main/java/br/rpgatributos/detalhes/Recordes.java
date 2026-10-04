package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recordes do servidor: o maior golpe, o maior nível e os maiores contadores (Elites, chefes,
 * masmorras, itens forjados...). Quem tomar um recorde de outra pessoa é anunciado. Uma placa
 * com "[recordes]" na primeira linha vira um painel que mostra os recordes, um de cada vez.
 * /recordes mostra todos.
 */
public final class Recordes implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFB300);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Recordes que vêm dos contadores dos títulos: id do contador → nome. */
    private static final Map<String, String> CONTADORES = new LinkedHashMap<>();
    static {
        CONTADORES.put("elites", "Elites derrotados");
        CONTADORES.put("chefes", "Chefes derrotados");
        CONTADORES.put("masmorras", "Masmorras vencidas");
        CONTADORES.put("portais", "Portais fechados");
        CONTADORES.put("forjados", "Itens forjados");
        CONTADORES.put("peixes_raros", "Peixes raros");
        CONTADORES.put("magias", "Magias lançadas");
        CONTADORES.put("tesouros_enterrados", "Tesouros desenterrados");
        CONTADORES.put("minerios", "Minérios quebrados");
        CONTADORES.put("animais_raros", "Animais raros");
    }

    private record Recorde(String nome, String quem, double valor, String data) { }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<String, Recorde> recordes = new LinkedHashMap<>();
    private final List<Location> placas = new ArrayList<>();
    private int vez;
    private boolean sujo;

    public Recordes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "recordes.yml");
    }

    private static String nome(String id) {
        return switch (id) {
            case "maior_golpe" -> "Maior golpe";
            case "nivel_total" -> "Maior nível total";
            default -> CONTADORES.getOrDefault(id, id);
        };
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        var s = y.getConfigurationSection("recordes");
        if (s != null) {
            for (String id : s.getKeys(false)) {
                recordes.put(id, new Recorde(nome(id), s.getString(id + ".quem", "?"), s.getDouble(id + ".valor"), s.getString(id + ".data", "")));
            }
        }
        for (String txt : y.getStringList("placas")) {
            String[] p = txt.split(",");
            World w = p.length == 4 ? Bukkit.getWorld(p[0]) : null;
            if (w == null) continue;
            try {
                placas.add(new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
            } catch (NumberFormatException ignorado) {
                // placa inválida
            }
        }
    }

    public void salvar() {
        if (!sujo) return;
        YamlConfiguration y = new YamlConfiguration();
        recordes.forEach((id, r) -> {
            y.set("recordes." + id + ".quem", r.quem());
            y.set("recordes." + id + ".valor", r.valor());
            y.set("recordes." + id + ".data", r.data());
        });
        List<String> l = new ArrayList<>();
        for (Location p : placas) l.add(p.getWorld().getName() + "," + p.getBlockX() + "," + p.getBlockY() + "," + p.getBlockZ());
        y.set("placas", l);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar recordes.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Atualizar
    // =====================================================================

    private void tentar(Player p, String id, double valor) {
        Recorde atual = recordes.get(id);
        if (atual != null && valor <= atual.valor()) return;
        boolean tomou = atual != null && !atual.quem().equals(p.getName());
        recordes.put(id, new Recorde(nome(id), p.getName(), valor, LocalDate.now().format(DATA)));
        sujo = true;
        if (tomou) {
            plugin.getServer().broadcast(Component.text("★ ", COR).append(Component.text(p.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                    .append(Component.text(" bateu o recorde de " + nome(id) + ": " + formatar(id, valor)
                            + " (era de " + atual.quem() + ")", COR)));
        }
    }

    public void golpe(Player p, double dano) {
        tentar(p, "maior_golpe", Math.round(dano * 10) / 10.0);
    }

    public void nivel(Player p, int nivelTotal) {
        tentar(p, "nivel_total", nivelTotal);
    }

    /** Chamado pelos títulos sempre que um contador muda. */
    public void aoContar(Player p, String id, int valor) {
        if (CONTADORES.containsKey(id)) tentar(p, id, valor);
    }

    private static String formatar(String id, double v) {
        return id.equals("maior_golpe") ? StatsManager.fmt(v) + " de dano" : String.valueOf((long) v);
    }

    // =====================================================================
    //  Comando e placas
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(Component.text("★ Recordes do servidor", COR, TextDecoration.BOLD));
        if (recordes.isEmpty()) {
            sender.sendMessage(Component.text("Ainda não há recordes. Vá bater um!", NamedTextColor.GRAY));
            return true;
        }
        recordes.forEach((id, r) -> sender.sendMessage(Component.text(" " + r.nome() + ": ", NamedTextColor.GRAY)
                .append(Component.text(formatar(id, r.valor()), NamedTextColor.WHITE))
                .append(Component.text(" — " + r.quem() + " (" + r.data() + ")", COR))));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoEscreverPlaca(SignChangeEvent e) {
        Component primeira = e.line(0);
        if (primeira == null || !PlainTextComponentSerializer.plainText().serialize(primeira).trim().equalsIgnoreCase("[recordes]")) return;
        placas.add(e.getBlock().getLocation());
        sujo = true;
        salvar();
        e.getPlayer().sendMessage(Component.text("★ Placa de recordes criada. Ela troca de recorde a cada poucos segundos.", COR));
        plugin.getServer().getScheduler().runTask(plugin, () -> desenhar(e.getBlock(), true));
    }

    /** A cada 5 s: as placas mostram o próximo recorde. */
    public void tick() {
        vez++;
        placas.removeIf(l -> l.isChunkLoaded() && !(l.getBlock().getState() instanceof Sign));
        for (Location l : placas) if (l.isChunkLoaded()) desenhar(l.getBlock(), false);
        if (sujo && vez % 12 == 0) salvar(); // no máximo 1x por minuto
    }

    private void desenhar(Block b, boolean primeira) {
        if (!(b.getState() instanceof Sign placa)) return;
        var lado = placa.getSide(Side.FRONT);
        if (recordes.isEmpty()) {
            lado.line(0, Component.text("★ Recordes ★", COR, TextDecoration.BOLD));
            lado.line(1, Component.text("ainda não há", NamedTextColor.GRAY));
            lado.line(2, Component.empty());
            lado.line(3, Component.empty());
        } else {
            List<Map.Entry<String, Recorde>> lista = new ArrayList<>(recordes.entrySet());
            Map.Entry<String, Recorde> en = lista.get(Math.floorMod(vez, lista.size()));
            lado.line(0, Component.text("★ Recorde ★", COR, TextDecoration.BOLD));
            lado.line(1, Component.text(en.getValue().nome(), NamedTextColor.DARK_GRAY));
            lado.line(2, Component.text(en.getValue().quem(), NamedTextColor.BLACK, TextDecoration.BOLD));
            lado.line(3, Component.text(formatar(en.getKey(), en.getValue().valor()), NamedTextColor.DARK_BLUE));
        }
        if (primeira) placa.setWaxed(true);
        placa.update();
    }
}
