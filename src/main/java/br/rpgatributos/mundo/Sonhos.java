package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.estruturas.Estrutura;
import br.rpgatributos.estruturas.Estruturas;
import br.rpgatributos.oculto.Locais;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sonhos: quem dorme a noite inteira às vezes sonha com um lugar que ainda não conhece — uma
 * estrutura (até naufrágios e cidades submersas) ou um Local Oculto. Ao acordar, sabe para que
 * lado fica. Um sonho por dia, nunca o mesmo lugar duas vezes.
 */
public final class Sonhos implements Listener {

    public static final TextColor COR = TextColor.color(0xB39DDB);
    private static final double CHANCE = 0.4;

    private final RPGAtributos plugin;
    private final NamespacedKey kSonhados, kDia;
    private final Set<UUID> dormiram = new HashSet<>();

    public Sonhos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kSonhados = new NamespacedKey(plugin, "sonhos_vistos");
        this.kDia = new NamespacedKey(plugin, "sonho_dia");
    }

    /** Dormiu de verdade (o jogo conta como sono profundo depois de uns segundos na cama). */
    @EventHandler(ignoreCancelled = true)
    public void aoDormir(PlayerDeepSleepEvent e) {
        dormiram.add(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void aoAcordar(PlayerBedLeaveEvent e) {
        Player p = e.getPlayer();
        if (!dormiram.remove(p.getUniqueId()) || !p.getWorld().isDayTime()) return;
        long dia = p.getWorld().getFullTime() / 24000L;
        if (p.getPersistentDataContainer().getOrDefault(kDia, PersistentDataType.LONG, -1L) == dia) return;
        if (ThreadLocalRandom.current().nextDouble() >= CHANCE) return;
        // Logo depois de levantar, para o título não se perder no "bom dia" do jogo.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) sonhar(p, dia); }, 20L);
    }

    private Set<String> sonhados(Player p) {
        String s = p.getPersistentDataContainer().get(kSonhados, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    /** Sonha agora (também pelo /rpgadmin sonho). @return false se não há nada para sonhar. */
    public boolean sonhar(Player p, long dia) {
        Location l = p.getLocation();
        Set<String> sonhados = sonhados(p);
        Estruturas.PistaSonho est = plugin.estruturas().paraSonhar(p, l, sonhados);
        Locais.PistaSonho oculto = plugin.locais().paraSonhar(p, l, sonhados);
        boolean usarOculto = oculto != null && (est == null || ThreadLocalRandom.current().nextDouble() < 0.35);
        if (est == null && oculto == null) return false;
        String id, visao, fim, marco;
        if (usarOculto) {
            id = oculto.id();
            visao = visao(oculto.tipo().name());
            double d = Math.hypot(oculto.x() - l.getX(), oculto.z() - l.getZ());
            fim = "Ao acordar, só resta a sensação de que fica " + direcao(l, oculto.x(), oculto.z()) + ", " + (d < 1500 ? "longe daqui" : "muito longe daqui") + ".";
            marco = "Sonhou com um lugar escondido " + direcao(l, oculto.x(), oculto.z());
        } else {
            id = est.id();
            visao = visao(est.tipo(), est.nome());
            int d = (int) Math.max(50, Math.round(Math.hypot(est.x() - l.getX(), est.z() - l.getZ()) / 50.0) * 50);
            fim = "Ao acordar, você sabe: fica " + direcao(l, est.x(), est.z()) + ", a uns " + d + " blocos daqui.";
            marco = "Sonhou com " + est.tipo().comArtigoIndefinido() + " " + direcao(l, est.x(), est.z());
        }
        sonhados.add(id);
        p.getPersistentDataContainer().set(kSonhados, PersistentDataType.STRING, String.join(",", sonhados));
        p.getPersistentDataContainer().set(kDia, PersistentDataType.LONG, dia);
        p.showTitle(Title.title(Component.text("☾ Um sonho...", COR, TextDecoration.ITALIC),
                Component.text(fim.replace("Ao acordar, ", ""), TextColor.color(0xD1C4E9)),
                Title.Times.times(Duration.ofMillis(600), Duration.ofMillis(3500), Duration.ofMillis(1000))));
        p.sendMessage(Component.text("☾ " + visao, COR, TextDecoration.ITALIC));
        p.sendMessage(Component.text("   " + fim, TextColor.color(0xD1C4E9)));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.6, 0), 40, 0.4, 0.4, 0.4, 0.6);
        plugin.diario().marco(p, "sonho_" + id, marco);
        plugin.titulos().registrar(p, "sonhos", 1);
        return true;
    }

    private static String direcao(Location de, int x, int z) {
        int setor = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-(z - de.getZ()), x - de.getX())) / 45.0), 8);
        return Estruturas.DIRECOES[setor];
    }

    private static String visao(Estrutura t, String nome) {
        return switch (t) {
            case TORRE_DE_VIGIA -> "Um sino tocava sozinho no alto de uma torre de pedra. Lá de cima, alguém ainda vigiava.";
            case ACAMPAMENTO -> "Fumaça de uma fogueira entre tendas. Risadas, e o tilintar de moedas roubadas.";
            case CEMITERIO -> "Lápides tortas sob a lua. A terra se mexia, como se respirasse.";
            case MINA -> "Trilhos que desciam para o escuro, e um carrinho cheio de pedras que brilhavam.";
            case POCO_DOS_DESEJOS -> "Uma pedra verde caía num poço, e a água devolvia um brilho.";
            case CABANA_DO_EREMITA -> "Uma cabana sozinha na mata. Um velho mexia uma panela e dizia o seu nome.";
            case SANTUARIO_ESQUECIDO -> "Um altar coberto de musgo. Alguém sussurrava uma prece a um deus esquecido.";
            case OASIS -> "Palmeiras e água fresca no meio da areia. Um camelo olhava para você.";
            case CABANA_DA_BRUXA -> "Uma casa sobre palafitas, um caldeirão borbulhando e uma risada fina.";
            case FAROL -> "Uma torre listrada na beira do mar, com a lanterna apagada esperando por você.";
            case EXPEDICAO -> "Barracas soterradas na neve, e um diário congelado nas mãos de alguém.";
            case VILA_SAQUEADA -> "Casas ainda soltando fumaça. Alguém pedia ajuda bem baixinho.";
            case TORRE_DO_MAGO -> "Livros que viravam as páginas sozinhos no alto de uma torre em ruínas.";
            case CIRCULO_DE_PEDRAS -> "Pedras em roda sob a lua cheia. Elas lembravam de você.";
            case FORJA_DOS_ANOES -> "O som de martelos debaixo da montanha, e um fogo que nunca apagou.";
            case FORTIM -> "Soldados de ferro e osso marchando numa muralha esquecida.";
            case NAUFRAGIO -> "Um navio deitado no fundo do mar. O capitão escrevia no diário um nome: " + (nome == null ? "?" : nome) + ".";
            case CIDADE_SUBMERSA -> "Torres de pedra-do-mar brilhando debaixo d'água, e um templo que chamava por você: "
                    + (nome == null ? "a cidade" : nome) + ".";
            case ILHA_DO_CEU -> "Uma ilha flutuando acima das nuvens, e no chão uma pedra que soprava o vento para o alto: "
                    + (nome == null ? "a ilha" : nome) + ".";
        };
    }

    private static String visao(String tipoLocal) {
        return switch (tipoLocal) {
            case "TUMULO_DO_FERREIRO" -> "Um martelo cravado numa pedra, aquecido por um fogo que ninguém acendeu.";
            case "BIBLIOTECA_PROIBIDA" -> "Estantes sem fim debaixo da terra, e um livro que chamava o seu nome.";
            case "COVIL_DAS_FERAS" -> "Olhos de fera no escuro, maiores que os de qualquer lobo.";
            case "CRIPTA_DO_REI_CAIDO" -> "Uma coroa caída num salão frio. Um rei que não aceitou morrer.";
            case "SANTUARIO_DAS_SOMBRAS" -> "Sombras que dançavam sem ninguém para fazê-las.";
            case "OBSERVATORIO_CELESTE" -> "Uma luneta antiga apontada para uma estrela que só você via.";
            case "CAPELA_PROFANADA" -> "Um sino rachado tocando numa capela onde ninguém reza mais.";
            default -> "Um lugar escondido, que a terra guarda há muito tempo.";
        };
    }
}
