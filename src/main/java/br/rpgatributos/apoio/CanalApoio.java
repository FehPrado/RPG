package br.rpgatributos.apoio;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.classe.Classe;
import br.rpgatributos.classe.Habilidade;
import br.rpgatributos.masmorra.ArvoreCacador;
import br.rpgatributos.masmorra.Cacador;
import br.rpgatributos.masmorra.Despertar;
import br.rpgatributos.masmorra.ManaCristal;
import br.rpgatributos.masmorra.Rank;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conversa com o mod de apoio (opcional, só no cliente). O mod manda pelo canal
 * "rpgatributos:tecla" o nome da tecla apertada; aqui ela vira a mesma ação dos atalhos de sempre.
 * Desde a 2.77 o plugin também manda pelo "rpgatributos:dados" o HUD (classe, mana, portal) e qual
 * tela de menu está abrindo, para o mod desenhar a dele. Quem não tem o mod não percebe nada.
 */
public final class CanalApoio implements PluginMessageListener, Listener {

    public static final String CANAL = "rpgatributos:tecla";
    public static final String CANAL_DADOS = "rpgatributos:dados";
    /** Intervalo mínimo entre duas teclas iguais (o mod já evita repetir, isso é só uma trava). */
    private static final long INTERVALO_MS = 150;

    private final RPGAtributos plugin;
    private final Set<UUID> comMod = new HashSet<>();
    private final Map<UUID, Map<String, Long>> ultima = new HashMap<>();

    public CanalApoio(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public void iniciar() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CANAL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CANAL_DADOS);
    }

    public void parar() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CANAL, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CANAL_DADOS);
    }

    /** O jogador está com o mod de apoio? */
    public boolean temMod(Player p) {
        return comMod.contains(p.getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String canal, Player p, byte[] dados) {
        if (!CANAL.equals(canal)) return;
        String acao = ler(dados);
        if (acao == null || acao.isEmpty() || acao.length() > 64) return;
        long agora = System.currentTimeMillis();
        Map<String, Long> doJogador = ultima.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        if (agora - doJogador.getOrDefault(acao, 0L) < INTERVALO_MS) return;
        doJogador.put(acao, agora);
        // As mensagens chegam na thread principal do servidor (o Paper entrega assim).
        if (acao.startsWith("ola")) {
            if (comMod.add(p.getUniqueId())) {
                p.sendActionBar(Component.text("✦ Mod de apoio conectado: as teclas do RPGAtributos estão em Opções > Controles.", NamedTextColor.GREEN));
                plugin.getLogger().info(p.getName() + " está com o mod de apoio (" + acao.substring(3).trim() + ")"
                        + (p.getListeningPluginChannels().contains(CANAL_DADOS) ? "." : "; sem o canal de dados (mod antigo): só as teclas."));
                hud(p);
            }
            return;
        }
        // 2.77: a roda de habilidades manda "habilidade:<n>".
        if (acao.startsWith("habilidade:")) {
            try { plugin.classes().usarHabilidade(p, Integer.parseInt(acao.substring(11))); } catch (NumberFormatException ignorado) { }
            return;
        }
        switch (acao) {
            case "golpe_montaria" -> plugin.combateMontado().golpePorTecla(p);
            case "habilidade" -> plugin.classes().usarPorTecla(p);
            case "trocar_habilidade" -> plugin.classes().trocarHabilidade(p);
            case "esquiva" -> plugin.mobilidade().esquivarPorTecla(p);
            case "postura" -> plugin.combos().posturas().proxima(p);
            case "menu" -> p.performCommand("rpg");
            // 2.77: teclas dos menus
            case "sistema" -> plugin.sistemaCacador().abrir(p);
            case "missao" -> plugin.missaoDiaria().mostrar(p);
            case "loja" -> plugin.lojaSistema().abrir(p);
            case "classes" -> p.performCommand("classe");
            default -> { } // tecla de uma versão mais nova do mod: ignora
        }
    }

    // =====================================================================
    //  2.77: dados para o mod (HUD e telas)
    // =====================================================================

    /** Manda uma mensagem ao mod, se ele está lá e escuta o canal de dados. */
    public void enviar(Player p, String mensagem) {
        if (!temMod(p) || !p.getListeningPluginChannels().contains(CANAL_DADOS)) return;
        p.sendPluginMessage(plugin, CANAL_DADOS, escrever(mensagem));
    }

    /** A cada 10 ticks: o HUD de quem tem o mod. */
    public void tick() {
        for (UUID id : comMod) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) hud(p);
        }
    }

    private void hud(Player p) {
        JsonObject o = new JsonObject();
        Classe c = plugin.classes().ativa(p);
        if (c != null) {
            JsonObject cl = new JsonObject();
            cl.addProperty("nome", c.nome());
            cl.addProperty("simbolo", c.simbolo());
            cl.addProperty("cor", c.cor().value());
            cl.addProperty("nivel", plugin.classes().nivel(p, c));
            cl.addProperty("sel", plugin.classes().indiceSelecionado(p));
            JsonArray hs = new JsonArray();
            for (Habilidade h : plugin.classes().habilidadesDe(p)) {
                JsonObject ho = new JsonObject();
                ho.addProperty("nome", h.nome());
                ho.addProperty("falta", plugin.classes().recargaRestante(p, h) / 1000.0);
                ho.addProperty("total", h.recarga());
                ho.addProperty("mana", h.mana());
                hs.add(ho);
            }
            cl.add("hab", hs);
            o.add("classe", cl);
        }
        var arc = plugin.arcano().perfil(p);
        o.addProperty("mana", arc.mana());
        o.addProperty("manaMax", plugin.arcano().manaMax(p, arc));
        o.addProperty("cristais", ManaCristal.valor(p));
        JsonObject portal = plugin.masmorras() == null ? null : plugin.masmorras().resumoHud(p);
        if (portal != null) o.add("portal", portal);
        enviar(p, "hud " + o);
    }

    /** Menus do plugin: o mod pinta todos ("pele") e desenha os 4 principais do jeito dele. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAbrir(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p) || !temMod(p) || e.getInventory().getType() != InventoryType.CHEST) return;
        InventoryHolder h = e.getInventory().getHolder(false);
        if (h == null || !h.getClass().getName().startsWith("br.rpgatributos.")) return;
        String nome = h.getClass().getName();
        String tela = nome.startsWith("br.rpgatributos.masmorra.SistemaCacador") ? "sistema"
                : nome.startsWith("br.rpgatributos.classe.Santuarios") ? "classes"
                : nome.startsWith("br.rpgatributos.masmorra.LojaSistema") ? "loja"
                : nome.startsWith("br.rpgatributos.masmorra.MuralReliquias") ? "mural"
                : "pele";
        JsonObject o = tela.equals("sistema") ? dadosSistema(p) : new JsonObject();
        o.addProperty("tema", tema(nome));
        enviar(p, "tela " + tela + " " + o);
    }

    /** O Sistema mudou (ponto gasto, re-despertar): manda os números de novo. */
    public void sistemaMudou(Player p) {
        JsonObject o = dadosSistema(p);
        o.addProperty("tema", "sistema");
        enviar(p, "dados sistema " + o);
    }

    private static JsonObject dadosSistema(Player p) {
        JsonObject o = new JsonObject();
        Rank r = Cacador.rank(p);
        o.addProperty("rank", r.nome());
        o.addProperty("cor", r.cor().value());
        o.addProperty("xp", Cacador.xp(p));
        o.addProperty("falta", Cacador.falta(p));
        o.addProperty("progresso", Cacador.progresso(p));
        o.addProperty("pontos", ArvoreCacador.pontos(p));
        o.addProperty("livres", ArvoreCacador.livres(p));
        o.addProperty("despertou", Despertar.despertou(p));
        o.addProperty("mana", ManaCristal.valor(p));
        return o;
    }

    /** O tema de cor da pele, pelo pacote do menu. */
    private static String tema(String classe) {
        String pacote = classe.substring("br.rpgatributos.".length());
        if (pacote.startsWith("masmorra.") || pacote.startsWith("portal.") || pacote.startsWith("sombra.") || pacote.startsWith("torre.")) return "portal";
        if (pacote.startsWith("classe.") || pacote.startsWith("titulos.") || pacote.startsWith("progressao.")) return "classes";
        if (pacote.startsWith("arcano.") || pacote.startsWith("alquimia.") || pacote.startsWith("fe.")) return "arcano";
        if (pacote.startsWith("forja.") || pacote.startsWith("oficio.") || pacote.startsWith("combo.")) return "forja";
        if (pacote.startsWith("colonia.") || pacote.startsWith("reino.") || pacote.startsWith("territorio.") || pacote.startsWith("party.")) return "reino";
        if (pacote.startsWith("domador.") || pacote.startsWith("vida.") || pacote.startsWith("fazenda.") || pacote.startsWith("pesca.")) return "natureza";
        return "rpg";
    }

    /** Uma String no formato do Minecraft: tamanho em VarInt e os bytes em UTF-8. */
    static String ler(byte[] d) {
        int tamanho = 0, pos = 0, desloc = 0;
        while (pos < d.length) {
            int b = d[pos++] & 0xFF;
            tamanho |= (b & 0x7F) << desloc;
            if ((b & 0x80) == 0) break;
            desloc += 7;
            if (desloc > 28) return null;
        }
        if (tamanho < 0 || pos + tamanho > d.length) return null;
        return new String(d, pos, tamanho, StandardCharsets.UTF_8);
    }

    /** O contrário de {@link #ler}. */
    static byte[] escrever(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length + 5);
        int v = b.length;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
        out.writeBytes(b);
        return out.toByteArray();
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        comMod.remove(e.getPlayer().getUniqueId());
        ultima.remove(e.getPlayer().getUniqueId());
    }
}
