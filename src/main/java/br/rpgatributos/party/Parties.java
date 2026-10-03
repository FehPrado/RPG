package br.rpgatributos.party;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * As parties: convites, entrar e sair, líder, fogo amigo, chat da party,
 * XP de Combate dividido com quem está perto e aviso de vida baixa.
 */
public final class Parties implements Listener {

    public static final TextColor COR = TextColor.color(0x55CDFC);
    private static final long VALIDADE_CONVITE_MS = 60_000;
    private static final long INTERVALO_AVISO_VIDA_MS = 15_000;

    private record Convite(UUID party, UUID quem, long expira) {}

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Party> porId = new HashMap<>();
    /** Lido também pela thread do chat. */
    private final Map<UUID, Party> porJogador = new ConcurrentHashMap<>();
    private final Map<UUID, Convite> convites = new HashMap<>();
    private final Set<UUID> chatAtivo = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> avisoVida = new HashMap<>();

    public Parties(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "parties.yml");
    }

    private Settings cfg() { return plugin.settings(); }

    private static void msg(Player p, String texto) {
        p.sendMessage(Component.text("☺ ", COR).append(Component.text(texto, NamedTextColor.WHITE)));
    }

    /** Mensagem para todos os membros online. */
    private static void avisar(Party pt, Component texto) {
        Component linha = Component.text("[Party] ", COR, TextDecoration.BOLD).append(texto.colorIfAbsent(NamedTextColor.WHITE));
        for (Player m : pt.online()) m.sendMessage(linha);
    }

    // =====================================================================
    //  Consultas
    // =====================================================================

    public Party party(UUID jogador) { return porJogador.get(jogador); }
    public Party party(Player p) { return porJogador.get(p.getUniqueId()); }

    public boolean mesmaParty(UUID a, UUID b) {
        Party pa = porJogador.get(a);
        return pa != null && !a.equals(b) && pa.tem(b);
    }

    public boolean temConvite(Player p) {
        Convite c = convites.get(p.getUniqueId());
        return c != null && c.expira() > System.currentTimeMillis() && porId.containsKey(c.party());
    }

    /** Nome de quem convidou (para o menu), ou null. */
    public String quemConvidou(Player p) {
        Convite c = convites.get(p.getUniqueId());
        if (c == null) return null;
        Party pt = porId.get(c.party());
        return pt == null ? null : pt.membros().getOrDefault(c.quem(), "?");
    }

    public boolean chatAtivo(Player p) { return chatAtivo.contains(p.getUniqueId()); }

    // =====================================================================
    //  Ações (cada uma devolve o motivo de não ter dado certo, ou null)
    // =====================================================================

    public String criar(Player p) {
        if (party(p) != null) return "Você já está numa party.";
        Party pt = new Party(UUID.randomUUID(), p.getUniqueId(), p.getName());
        porId.put(pt.id(), pt);
        porJogador.put(p.getUniqueId(), pt);
        salvar();
        msg(p, "Party criada! Convide amigos com /party convidar <nick>.");
        return null;
    }

    public String convidar(Player p, Player alvo) {
        if (alvo.equals(p)) return "Você não pode se convidar.";
        Party pt = party(p);
        if (pt != null && !pt.lider().equals(p.getUniqueId())) return "Só o líder (" + pt.nomeLider() + ") convida.";
        if (party(alvo) != null) return alvo.getName() + " já está numa party.";
        if (pt != null && pt.tamanho() >= cfg().partyTamanhoMax) return "A party está cheia (" + cfg().partyTamanhoMax + ").";
        if (pt == null) {
            criar(p);
            pt = party(p);
        }
        convites.put(alvo.getUniqueId(), new Convite(pt.id(), p.getUniqueId(), System.currentTimeMillis() + VALIDADE_CONVITE_MS));
        alvo.sendMessage(Component.text("☺ ", COR)
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" convidou você para a party! ", NamedTextColor.WHITE))
                .append(Component.text("[Aceitar]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/party aceitar"))
                        .hoverEvent(HoverEvent.showText(Component.text("Entrar na party"))))
                .append(Component.text(" "))
                .append(Component.text("[Recusar]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/party recusar"))));
        alvo.playSound(alvo.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
        msg(p, "Convite enviado para " + alvo.getName() + " (vale por 1 minuto).");
        return null;
    }

    public String aceitar(Player p) {
        Convite c = convites.remove(p.getUniqueId());
        if (c == null || c.expira() < System.currentTimeMillis()) return "Você não tem convite (ou ele expirou).";
        Party pt = porId.get(c.party());
        if (pt == null) return "Essa party não existe mais.";
        if (party(p) != null) return "Saia da sua party primeiro (/party sair).";
        if (pt.tamanho() >= cfg().partyTamanhoMax) return "A party está cheia.";
        pt.membrosEditaveis().put(p.getUniqueId(), p.getName());
        porJogador.put(p.getUniqueId(), pt);
        salvar();
        avisar(pt, Component.text(p.getName() + " entrou na party!", NamedTextColor.GREEN));
        for (Player m : pt.online()) m.playSound(m.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        return null;
    }

    public String recusar(Player p) {
        Convite c = convites.remove(p.getUniqueId());
        if (c == null) return "Você não tem convite.";
        msg(p, "Convite recusado.");
        Player quem = plugin.getServer().getPlayer(c.quem());
        if (quem != null) msg(quem, p.getName() + " recusou o convite.");
        return null;
    }

    public String sair(Player p) {
        Party pt = party(p);
        if (pt == null) return "Você não está numa party.";
        tirar(pt, p.getUniqueId());
        msg(p, "Você saiu da party.");
        if (porId.containsKey(pt.id())) avisar(pt, Component.text(p.getName() + " saiu da party.", NamedTextColor.GRAY));
        return null;
    }

    public String expulsar(Player lider, UUID alvo) {
        Party pt = party(lider);
        if (pt == null) return "Você não está numa party.";
        if (!pt.lider().equals(lider.getUniqueId())) return "Só o líder expulsa.";
        if (alvo.equals(lider.getUniqueId())) return "Para sair, use /party sair.";
        if (!pt.tem(alvo)) return "Esse jogador não é da sua party.";
        String nome = pt.membros().get(alvo);
        tirar(pt, alvo);
        Player expulso = plugin.getServer().getPlayer(alvo);
        if (expulso != null) msg(expulso, "Você foi tirado da party por " + lider.getName() + ".");
        if (porId.containsKey(pt.id())) avisar(pt, Component.text(nome + " foi tirado da party.", NamedTextColor.GRAY));
        return null;
    }

    public String passarLideranca(Player lider, UUID alvo) {
        Party pt = party(lider);
        if (pt == null) return "Você não está numa party.";
        if (!pt.lider().equals(lider.getUniqueId())) return "Só o líder passa a liderança.";
        if (!pt.tem(alvo) || alvo.equals(lider.getUniqueId())) return "Escolha outro membro da party.";
        pt.lider(alvo);
        salvar();
        avisar(pt, Component.text(pt.nomeLider() + " agora é o líder da party.", NamedTextColor.GOLD));
        return null;
    }

    public String desfazer(Player lider) {
        Party pt = party(lider);
        if (pt == null) return "Você não está numa party.";
        if (!pt.lider().equals(lider.getUniqueId())) return "Só o líder desfaz a party.";
        avisar(pt, Component.text("A party foi desfeita.", NamedTextColor.RED));
        apagar(pt);
        return null;
    }

    public String alternarFogoAmigo(Player lider) {
        Party pt = party(lider);
        if (pt == null) return "Você não está numa party.";
        if (!pt.lider().equals(lider.getUniqueId())) return "Só o líder muda o fogo amigo.";
        pt.fogoAmigo(!pt.fogoAmigo());
        salvar();
        avisar(pt, Component.text("Fogo amigo " + (pt.fogoAmigo() ? "LIGADO: vocês podem se ferir." : "desligado."),
                pt.fogoAmigo() ? NamedTextColor.RED : NamedTextColor.GREEN));
        return null;
    }

    /** Liga/desliga o chat da party. @return se ficou ligado. */
    public boolean alternarChat(Player p) {
        if (chatAtivo.remove(p.getUniqueId())) return false;
        chatAtivo.add(p.getUniqueId());
        return true;
    }

    public String enviarChat(Player p, Component mensagem) {
        Party pt = party(p);
        if (pt == null) return "Você não está numa party.";
        Component linha = Component.text("[Party] ", COR, TextDecoration.BOLD)
                .append(Component.text(p.getName() + ": ", NamedTextColor.AQUA))
                .append(mensagem.colorIfAbsent(NamedTextColor.WHITE));
        for (Player m : pt.online()) m.sendMessage(linha);
        plugin.getLogger().info("[Party " + pt.nomeLider() + "] " + p.getName() + ": "
                + PlainTextComponentSerializer.plainText().serialize(mensagem));
        return null;
    }

    /** Tira o jogador; se era o líder, passa para o próximo; se sobrar só um, a party acaba. */
    private void tirar(Party pt, UUID jogador) {
        pt.membrosEditaveis().remove(jogador);
        porJogador.remove(jogador);
        chatAtivo.remove(jogador);
        if (pt.tamanho() <= 1) {
            for (UUID resto : new ArrayList<>(pt.membros().keySet())) {
                Player r = plugin.getServer().getPlayer(resto);
                if (r != null) msg(r, "A party acabou (só sobrou você).");
            }
            apagar(pt);
            return;
        }
        if (pt.lider().equals(jogador)) {
            pt.lider(pt.membros().keySet().iterator().next());
            avisar(pt, Component.text(pt.nomeLider() + " agora é o líder da party.", NamedTextColor.GOLD));
        }
        salvar();
    }

    private void apagar(Party pt) {
        for (UUID m : pt.membros().keySet()) {
            porJogador.remove(m);
            chatAtivo.remove(m);
        }
        porId.remove(pt.id());
        convites.values().removeIf(c -> c.party().equals(pt.id()));
        salvar();
    }

    // =====================================================================
    //  XP de Combate dividido
    // =====================================================================

    /**
     * Chamado quando um jogador mata um mob. Quem da party está perto ganha uma parte
     * do XP, e quem matou ganha um bônus por membro perto.
     */
    public void compartilharXp(Player matador, Location onde, double xp) {
        Party pt = party(matador);
        if (pt == null || xp <= 0) return;
        double raio = cfg().partyRaioXp;
        List<Player> perto = new ArrayList<>();
        for (Player m : pt.online()) {
            if (m == matador || m.isDead() || !m.getWorld().equals(onde.getWorld())) continue;
            if (m.getGameMode() != GameMode.SURVIVAL && m.getGameMode() != GameMode.ADVENTURE) continue;
            if (m.getLocation().distanceSquared(onde) <= raio * raio) perto.add(m);
        }
        if (perto.isEmpty()) return;
        double bonus = Math.min(cfg().partyBonusMax, perto.size() * cfg().partyBonusPorMembro);
        if (bonus > 0) plugin.stats().darXp(matador, Skill.COMBATE, xp * bonus);
        for (Player m : perto) plugin.stats().darXp(m, Skill.COMBATE, xp * cfg().partyXpCompartilhado);
        plugin.titulos().registrar(matador, "abates_party", 1);
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoFalar(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!chatAtivo.contains(p.getUniqueId()) || !porJogador.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);
        Component mensagem = e.message();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && enviarChat(p, mensagem) != null) chatAtivo.remove(p.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!cfg().partyAvisoVida || !(e.getEntity() instanceof Player p)) return;
        Party pt = party(p);
        if (pt == null) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double vidaMax = max == null ? 20 : max.getValue();
        double depois = p.getHealth() - e.getFinalDamage();
        if (depois <= 0 || depois > vidaMax * 0.3) return;
        long agora = System.currentTimeMillis();
        Long ultimo = avisoVida.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < INTERVALO_AVISO_VIDA_MS) return;
        avisoVida.put(p.getUniqueId(), agora);
        String coracoes = String.format("%.1f", depois / 2);
        for (Player m : pt.online()) {
            if (m == p) continue;
            m.sendActionBar(Component.text("⚠ " + p.getName() + " está com pouca vida (" + coracoes + " ❤)!", NamedTextColor.RED));
            m.playSound(m.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
        }
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Party pt = party(p);
        if (pt == null) return;
        if (!p.getName().equals(pt.membros().get(p.getUniqueId()))) {
            pt.membrosEditaveis().put(p.getUniqueId(), p.getName());
            salvar();
        }
        for (Player m : pt.online()) {
            if (m != p) m.sendMessage(Component.text("[Party] ", COR, TextDecoration.BOLD)
                    .append(Component.text(p.getName() + " entrou no servidor.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        avisoVida.remove(p.getUniqueId());
        Party pt = party(p);
        if (pt == null) return;
        for (Player m : pt.online()) {
            if (m != p) m.sendMessage(Component.text("[Party] ", COR, TextDecoration.BOLD)
                    .append(Component.text(p.getName() + " saiu do servidor.", NamedTextColor.GRAY)));
        }
    }

    /** Limpa convites vencidos (1x por minuto). */
    public void limparConvites() {
        long agora = System.currentTimeMillis();
        for (Iterator<Convite> it = convites.values().iterator(); it.hasNext(); ) {
            if (it.next().expira() < agora) it.remove();
        }
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        porId.clear();
        porJogador.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection raiz = y.getConfigurationSection("parties");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(id);
            ConfigurationSection mem = s == null ? null : s.getConfigurationSection("membros");
            if (mem == null) continue;
            try {
                UUID lider = UUID.fromString(s.getString("lider", ""));
                Party pt = new Party(UUID.fromString(id), lider, mem.getString(lider.toString(), "?"));
                for (String u : mem.getKeys(false)) pt.membrosEditaveis().put(UUID.fromString(u), mem.getString(u, "?"));
                pt.fogoAmigo(s.getBoolean("fogo-amigo", false));
                porId.put(pt.id(), pt);
                for (UUID u : pt.membros().keySet()) porJogador.put(u, pt);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Party inválida em parties.yml (" + id + "): " + ex.getMessage());
            }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Party pt : porId.values()) {
            String base = "parties." + pt.id() + ".";
            y.set(base + "lider", pt.lider().toString());
            y.set(base + "fogo-amigo", pt.fogoAmigo());
            for (Map.Entry<UUID, String> m : pt.membros().entrySet()) y.set(base + "membros." + m.getKey(), m.getValue());
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar parties.yml: " + ex.getMessage());
        }
    }
}
