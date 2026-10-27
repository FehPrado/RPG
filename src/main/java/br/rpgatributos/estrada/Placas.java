package br.rpgatributos.estrada;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.colonia.Colonia;
import br.rpgatributos.estruturas.Estruturas;
import br.rpgatributos.viagem.PedrasDeViagem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Placa de direção (2.95): uma placa com {@code [direção]} na primeira linha e o nome de um lugar nas
 * outras se reescreve com o rumo e a distância daquele lugar, contados da própria placa. Lugares: as
 * colônias ("Colônia de Fulano" ou só "Fulano"), as Pedras de Viagem públicas e as estruturas que quem
 * escreve já visitou. A placa fica encerada (não se edita sem querer). Nada aparece na tela: é a placa.
 */
public final class Placas implements Listener {

    public static final TextColor COR = TextColor.color(0xD7B98E);
    private static final String[] RUMOS = {"Leste", "Nordeste", "Norte", "Noroeste", "Oeste", "Sudoeste", "Sul", "Sudeste"};
    private static final int LARGURA = 15;

    private final RPGAtributos plugin;

    public record Destino(String nome, Location onde) { }

    public Placas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    static String texto(Component c) {
        return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c).trim();
    }

    static String normal(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** O rumo (8 direções) de um ponto a outro, como na bússola: norte é z menor. */
    public static String rumo(Location de, Location para) {
        double dx = para.getX() - de.getX(), dz = para.getZ() - de.getZ();
        return RUMOS[Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dz, dx)) / 45.0), 8)];
    }

    public static int distancia(Location de, Location para) {
        return (int) Math.round(Math.hypot(para.getX() - de.getX(), para.getZ() - de.getZ()));
    }

    /** Os lugares que podem ir numa placa deste mundo, para quem está escrevendo. */
    public List<Destino> destinos(Player p, World w) {
        List<Destino> l = new ArrayList<>();
        for (Colonia c : plugin.colonias().todas()) {
            Location pref = c.prefeitura();
            if (pref != null && pref.getWorld() == w) l.add(new Destino("Colônia de " + c.nomeDono(), pref));
        }
        for (PedrasDeViagem.Pedra pd : plugin.pedras().publicas()) {
            Location loc = pd.local();
            if (loc != null && loc.getWorld() == w && pd.nome() != null) l.add(new Destino(pd.nome(), loc));
        }
        for (Estruturas.Lugar lu : plugin.estruturas().visitadas(p.getUniqueId())) {
            if (lu.mundo().equals(w.getName())) l.add(new Destino(lu.nome(), new Location(w, lu.x(), 64, lu.z())));
        }
        return l;
    }

    static Destino achar(List<Destino> l, String busca) {
        String q = normal(busca);
        if (q.isEmpty()) return null;
        for (Destino d : l) if (normal(d.nome()).equals(q)) return d;
        for (Destino d : l) if (normal(d.nome()).endsWith(" " + q) || normal(d.nome()).startsWith(q)) return d;
        for (Destino d : l) if (normal(d.nome()).contains(q)) return d;
        return null;
    }

    /** Quebra o nome em até 2 linhas da placa. */
    static String[] duasLinhas(String nome) {
        if (nome.length() <= LARGURA) return new String[]{nome, ""};
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (String palavra : nome.split(" ")) {
            if (b.isEmpty() && a.length() + (a.isEmpty() ? 0 : 1) + palavra.length() <= LARGURA) {
                if (!a.isEmpty()) a.append(' ');
                a.append(palavra);
            } else {
                if (!b.isEmpty()) b.append(' ');
                b.append(palavra);
            }
        }
        String sb = b.length() > LARGURA ? b.substring(0, LARGURA - 1) + "…" : b.toString();
        String sa = a.isEmpty() ? nome.substring(0, LARGURA - 1) + "…" : a.toString();
        return new String[]{sa, sb};
    }

    static boolean ehPlaca(String primeira, String... palavras) {
        String n = normal(primeira).replace(" ", "");
        for (String w : palavras) if (n.equals("[" + w + "]")) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEscrever(SignChangeEvent e) {
        if (!plugin.settings().estradaPlacas || !ehPlaca(texto(e.line(0)), "direcao", "placa")) return;
        Player p = e.getPlayer();
        String busca = (texto(e.line(1)) + " " + texto(e.line(2)) + " " + texto(e.line(3))).trim();
        Block b = e.getBlock();
        Location aqui = b.getLocation().add(0.5, 0, 0.5);
        List<Destino> todos = destinos(p, b.getWorld());
        Destino d = achar(todos, busca);
        if (d == null) {
            e.line(0, Component.text("[direção]", NamedTextColor.RED));
            todos.sort(Comparator.comparingInt(x -> distancia(aqui, x.onde())));
            List<String> nomes = new ArrayList<>();
            for (int i = 0; i < Math.min(6, todos.size()); i++) nomes.add(todos.get(i).nome());
            p.sendMessage(Component.text("✖ Não achei \"" + busca + "\". ", NamedTextColor.RED)
                    .append(Component.text(nomes.isEmpty()
                            ? "Vale uma colônia, uma Pedra de Viagem pública ou uma estrutura que você já visitou."
                            : "Lugares perto: " + String.join(", ", nomes) + ".", NamedTextColor.GRAY)));
            return;
        }
        int dist = Math.max(10, (int) Math.round(distancia(aqui, d.onde()) / 10.0) * 10);
        String[] nome = duasLinhas(d.nome());
        e.line(0, Component.text("» " + rumo(aqui, d.onde()) + " «", COR, TextDecoration.BOLD));
        e.line(1, Component.text(nome[0], NamedTextColor.BLACK));
        e.line(2, Component.text(nome[1], NamedTextColor.BLACK));
        e.line(3, Component.text(dist < 20 ? "aqui perto" : "a " + dist + " blocos", NamedTextColor.DARK_GRAY));
        encerar(b);
        p.playSound(aqui, Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
    }

    /** Encera a placa no tick seguinte (depois que o jogo grava o texto). */
    void encerar(Block b) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (b.getState() instanceof Sign s && !s.isWaxed()) {
                s.setWaxed(true);
                s.update();
            }
        });
    }
}
