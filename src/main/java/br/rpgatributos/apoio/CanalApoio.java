package br.rpgatributos.apoio;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conversa com o mod de apoio (opcional, só no cliente). O mod manda pelo canal
 * "rpgatributos:tecla" o nome da tecla apertada; aqui ela vira a mesma ação dos atalhos de sempre.
 * Quem não tem o mod não percebe nada: os atalhos sem mod continuam valendo.
 */
public final class CanalApoio implements PluginMessageListener, Listener {

    public static final String CANAL = "rpgatributos:tecla";
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
    }

    public void parar() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CANAL, this);
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
                plugin.getLogger().info(p.getName() + " está com o mod de apoio (" + acao.substring(3).trim() + ").");
            }
            return;
        }
        switch (acao) {
            case "golpe_montaria" -> plugin.combateMontado().golpePorTecla(p);
            case "habilidade" -> plugin.classes().usarPorTecla(p);
            case "trocar_habilidade" -> plugin.classes().trocarHabilidade(p);
            case "esquiva" -> plugin.mobilidade().esquivarPorTecla(p);
            case "postura" -> plugin.combos().posturas().proxima(p);
            case "menu" -> p.performCommand("rpg");
            default -> { } // tecla de uma versão mais nova do mod: ignora
        }
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

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        comMod.remove(e.getPlayer().getUniqueId());
        ultima.remove(e.getPlayer().getUniqueId());
    }
}
