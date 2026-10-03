package br.rpgatributos.reino;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.colonia.Colonia;
import br.rpgatributos.territorio.Territorio;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Reinos: um Rei com cargos (Nobre, Cavaleiro, Cidadão), tesouro de esmeraldas e guerras
 * marcadas no horário de guerra do servidor. Na guerra o PvP entre os dois reinos fica livre
 * e dá para capturar os Marcos do inimigo (o território dele perde chunks). Construções nunca
 * são destruídas: a proteção dos blocos continua valendo.
 */
public final class Reinos implements Listener {

    private static final double RAIO_CAPTURA = 6;
    private static final long CONVITE_MS = 120_000;
    private static final NamedTextColor[] CORES = {NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.GOLD,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.AQUA, NamedTextColor.YELLOW, NamedTextColor.DARK_AQUA, NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_GREEN, NamedTextColor.DARK_RED, NamedTextColor.WHITE};
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("EEEE dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private record Convite(UUID reino, long expira) {}

    private static final class Tela implements InventoryHolder {
        final Reino reino;
        Inventory inventario;

        Tela(Reino reino) { this.reino = reino; }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Reino> reinos = new HashMap<>();
    private final Map<UUID, UUID> porJogador = new HashMap<>();
    private final Map<UUID, Convite> convites = new HashMap<>();
    private final List<Guerra> guerras = new ArrayList<>();
    /** Rei que pediu paz → reino com quem quer paz. */
    private final Map<UUID, UUID> pedidosDePaz = new HashMap<>();

    public Reinos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "reinos.yml");
    }

    private Settings cfg() { return plugin.settings(); }

    // =====================================================================
    //  Consultas
    // =====================================================================

    public Reino de(UUID jogador) {
        UUID id = porJogador.get(jogador);
        return id == null ? null : reinos.get(id);
    }

    public Reino de(Player p) { return de(p.getUniqueId()); }

    public Reino porNome(String nome) {
        for (Reino r : reinos.values()) if (r.nome.equalsIgnoreCase(nome)) return r;
        return null;
    }

    public Collection<Reino> todos() { return reinos.values(); }

    /** Os dois jogadores são do mesmo reino? */
    public boolean aliados(UUID a, UUID b) {
        UUID ra = porJogador.get(a);
        return ra != null && ra.equals(porJogador.get(b));
    }

    /** Guerra acontecendo agora entre os dois reinos (ou null). */
    public Guerra guerraAtiva(UUID reinoA, UUID reinoB) {
        if (reinoA == null || reinoB == null || reinoA.equals(reinoB)) return null;
        long agora = System.currentTimeMillis();
        for (Guerra g : guerras) if (g.envolve(reinoA) && g.envolve(reinoB) && g.ativa(agora)) return g;
        return null;
    }

    /** Os dois jogadores estão em reinos em guerra agora? */
    public boolean inimigosEmGuerra(UUID a, UUID b) {
        return guerraAtiva(porJogador.get(a), porJogador.get(b)) != null;
    }

    /** Algum reino em guerra agora contra esse? */
    public boolean emGuerra(UUID reino) {
        long agora = System.currentTimeMillis();
        for (Guerra g : guerras) if (g.envolve(reino) && g.ativa(agora)) return true;
        return false;
    }

    /** Reinos em guerra agora contra esse. */
    public List<UUID> inimigosAgora(UUID reino) {
        List<UUID> l = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (Guerra g : guerras) if (g.envolve(reino) && g.ativa(agora)) l.add(g.outro(reino));
        return l;
    }

    /** Chunks a mais para o território de um jogador (só o Rei, pelas conquistas). */
    public int chunksExtras(UUID jogador) {
        Reino r = de(jogador);
        return r != null && r.rei.equals(jogador) ? r.chunksConquistados : 0;
    }

    /** "♛ Reino de Avalon" em cima da cabeça. */
    public Component linhaDaTag(Player p) {
        Reino r = de(p);
        if (r == null) return null;
        Cargo c = r.cargo(p.getUniqueId());
        return Component.text(c.icone() + " " + r.nome, r.cor);
    }

    // =====================================================================
    //  Fundar e membros
    // =====================================================================

    public String fundar(Player p, String nome) {
        if (de(p) != null) return "Você já está num reino.";
        Colonia c = plugin.colonias().de(p.getUniqueId());
        if (c == null || c.nivel() < cfg().reiNivelColonia) return "Precisa de uma colônia nível " + cfg().reiNivelColonia + " para fundar um reino.";
        String n = nome.replaceAll("[&§]", "").trim();
        if (n.length() < 3 || n.length() > 20) return "O nome precisa ter de 3 a 20 letras.";
        if (porNome(n) != null) return "Já existe um reino com esse nome.";
        if (p.getGameMode() != GameMode.CREATIVE) {
            if (!p.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), cfg().reiCustoFundar)) {
                return "Fundar um reino custa " + cfg().reiCustoFundar + " esmeraldas.";
            }
            p.getInventory().removeItem(new ItemStack(Material.EMERALD, cfg().reiCustoFundar));
        }
        NamedTextColor cor = CORES[reinos.size() % CORES.length];
        for (NamedTextColor x : CORES) {
            if (reinos.values().stream().noneMatch(r -> r.cor == x)) { cor = x; break; }
        }
        Reino r = new Reino(UUID.randomUUID(), n, cor, p.getUniqueId(), p.getName());
        reinos.put(r.id, r);
        porJogador.put(p.getUniqueId(), r.id);
        Bukkit.broadcast(Component.text("♛ ", NamedTextColor.GOLD).append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" fundou o reino ", NamedTextColor.GRAY)).append(Component.text(n, cor, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GRAY)));
        for (Player o : Bukkit.getOnlinePlayers()) o.playSound(o.getLocation(), Sound.EVENT_RAID_HORN, 0.4f, 1.2f);
        atualizarTag(p);
        plugin.colonias().atualizarUniformes(p.getUniqueId());
        salvar();
        return null;
    }

    public String convidar(Player p, Player alvo) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        if (!r.cargo(p.getUniqueId()).podeConvidar()) return "Só Rei, Nobres e Cavaleiros convidam.";
        if (de(alvo) != null) return alvo.getName() + " já está num reino.";
        convites.put(alvo.getUniqueId(), new Convite(r.id, System.currentTimeMillis() + CONVITE_MS));
        alvo.sendMessage(Component.text("♛ " + p.getName() + " te convidou para o reino ", NamedTextColor.GOLD)
                .append(Component.text(r.nome, r.cor, TextDecoration.BOLD)).append(Component.text(".  ", NamedTextColor.GOLD))
                .append(Component.text("[Aceitar]", NamedTextColor.GREEN, TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/reino aceitar")))
                .append(Component.text("  "))
                .append(Component.text("[Recusar]", NamedTextColor.RED, TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/reino recusar"))));
        alvo.playSound(alvo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        return null;
    }

    public String aceitar(Player p) {
        Convite c = convites.remove(p.getUniqueId());
        if (c == null || System.currentTimeMillis() > c.expira()) return "Você não tem convite (ou ele expirou).";
        Reino r = reinos.get(c.reino());
        if (r == null) return "Esse reino não existe mais.";
        if (de(p) != null) return "Você já está num reino.";
        r.membros.put(p.getUniqueId(), Cargo.CIDADAO);
        r.nomes.put(p.getUniqueId(), p.getName());
        porJogador.put(p.getUniqueId(), r.id);
        avisarReino(r, Component.text("♛ " + p.getName() + " entrou no reino!", NamedTextColor.GREEN));
        atualizarTag(p);
        plugin.colonias().atualizarUniformes(p.getUniqueId());
        salvar();
        return null;
    }

    public void recusar(Player p) {
        convites.remove(p.getUniqueId());
    }

    public String sair(Player p) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        if (r.rei.equals(p.getUniqueId())) return "O Rei não pode sair: passe a coroa (/reino coroa <nick>) ou desfaça o reino.";
        if (emGuerra(r.id)) return "Não dá para sair no meio de uma guerra.";
        remover(r, p.getUniqueId());
        avisarReino(r, Component.text("♛ " + p.getName() + " saiu do reino.", NamedTextColor.GRAY));
        p.sendMessage(Component.text("Você saiu do reino " + r.nome + ".", NamedTextColor.GRAY));
        return null;
    }

    public String expulsar(Player p, String nome) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        UUID alvo = membroPorNome(r, nome);
        if (alvo == null) return "Não há ninguém com esse nome no reino.";
        Cargo meu = r.cargo(p.getUniqueId()), dele = r.cargo(alvo);
        if (!meu.administra() || !meu.acimaDe(dele)) return "Você só expulsa quem está abaixo do seu cargo.";
        remover(r, alvo);
        avisarReino(r, Component.text("♛ " + nome + " foi expulso do reino.", NamedTextColor.RED));
        Player o = Bukkit.getPlayer(alvo);
        if (o != null) o.sendMessage(Component.text("Você foi expulso do reino " + r.nome + ".", NamedTextColor.RED));
        return null;
    }

    public String definirCargo(Player p, String nome, Cargo cargo) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        if (!r.rei.equals(p.getUniqueId())) return "Só o Rei muda cargos.";
        if (cargo == Cargo.REI) return "Para passar a coroa use /reino coroa <nick>.";
        UUID alvo = membroPorNome(r, nome);
        if (alvo == null) return "Não há ninguém com esse nome no reino.";
        if (alvo.equals(r.rei)) return "O Rei é o Rei.";
        r.membros.put(alvo, cargo);
        avisarReino(r, Component.text("♛ " + nome + " agora é " + cargo.nome() + ".", cargo.cor()));
        Player o = Bukkit.getPlayer(alvo);
        if (o != null) atualizarTag(o);
        salvar();
        return null;
    }

    public String passarCoroa(Player p, String nome) {
        Reino r = de(p);
        if (r == null || !r.rei.equals(p.getUniqueId())) return "Só o Rei passa a coroa.";
        UUID alvo = membroPorNome(r, nome);
        if (alvo == null || alvo.equals(r.rei)) return "Escolha outro membro do reino.";
        r.membros.put(r.rei, Cargo.NOBRE);
        r.membros.put(alvo, Cargo.REI);
        r.rei = alvo;
        Bukkit.broadcast(Component.text("♛ " + nome + " é o novo Rei de " + r.nome + "!", r.cor));
        atualizarTag(p);
        Player o = Bukkit.getPlayer(alvo);
        if (o != null) atualizarTag(o);
        salvar();
        return null;
    }

    public String desfazer(Player p) {
        Reino r = de(p);
        if (r == null || !r.rei.equals(p.getUniqueId())) return "Só o Rei desfaz o reino.";
        if (emGuerra(r.id)) return "Não dá para desfazer no meio de uma guerra.";
        guerras.removeIf(g -> g.envolve(r.id));
        for (UUID m : List.copyOf(r.membros.keySet())) {
            porJogador.remove(m);
            Player o = Bukkit.getPlayer(m);
            if (o != null) {
                o.sendMessage(Component.text("♛ O reino " + r.nome + " foi desfeito.", NamedTextColor.GRAY));
                atualizarTag(o);
            }
            plugin.colonias().atualizarUniformes(m);
        }
        reinos.remove(r.id);
        // O que sobrou do tesouro volta para o Rei.
        devolverEsmeraldas(p, r.tesouro);
        salvar();
        return null;
    }

    private void remover(Reino r, UUID jogador) {
        r.membros.remove(jogador);
        porJogador.remove(jogador);
        Player o = Bukkit.getPlayer(jogador);
        if (o != null) atualizarTag(o);
        plugin.colonias().atualizarUniformes(jogador);
        salvar();
    }

    private static UUID membroPorNome(Reino r, String nome) {
        for (Map.Entry<UUID, String> e : r.nomes.entrySet()) {
            if (e.getValue().equalsIgnoreCase(nome) && r.membros.containsKey(e.getKey())) return e.getKey();
        }
        return null;
    }

    private void atualizarTag(Player p) {
        plugin.tags().garantir(p);
        plugin.tags().atualizarTexto(p);
    }

    private void avisarReino(Reino r, Component msg) {
        for (UUID m : r.membros.keySet()) {
            Player o = Bukkit.getPlayer(m);
            if (o != null) o.sendMessage(msg);
        }
    }

    // =====================================================================
    //  Tesouro
    // =====================================================================

    public String depositar(Player p, int qtd) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        if (qtd <= 0) return "Quantidade inválida.";
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), qtd)) return "Você não tem " + qtd + " esmeraldas.";
        p.getInventory().removeItem(new ItemStack(Material.EMERALD, qtd));
        r.tesouro += qtd;
        avisarReino(r, Component.text("♛ " + p.getName() + " depositou " + qtd + " esmeraldas no tesouro (" + r.tesouro + ").", NamedTextColor.GREEN));
        salvar();
        return null;
    }

    public String sacar(Player p, int qtd) {
        Reino r = de(p);
        if (r == null) return "Você não está num reino.";
        if (!r.cargo(p.getUniqueId()).administra()) return "Só o Rei e os Nobres mexem no tesouro.";
        if (qtd <= 0 || qtd > r.tesouro) return "O tesouro tem " + r.tesouro + " esmeraldas.";
        r.tesouro -= qtd;
        devolverEsmeraldas(p, qtd);
        avisarReino(r, Component.text("♛ " + p.getName() + " sacou " + qtd + " esmeraldas do tesouro (" + r.tesouro + ").", NamedTextColor.GOLD));
        salvar();
        return null;
    }

    private static void devolverEsmeraldas(Player p, long qtd) {
        while (qtd > 0) {
            int n = (int) Math.min(64, qtd);
            p.getInventory().addItem(new ItemStack(Material.EMERALD, n)).values()
                    .forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
            qtd -= n;
        }
    }

    // =====================================================================
    //  Guerra
    // =====================================================================

    /** Primeiro horário de guerra depois do aviso mínimo. */
    private long proximoHorario(long depoisDe) {
        ZonedDateTime minimo = Instant.ofEpochMilli(depoisDe).atZone(ZoneId.systemDefault());
        ZonedDateTime c = minimo.with(TemporalAdjusters.nextOrSame(cfg().gueDia))
                .withHour(cfg().gueHora).withMinute(0).withSecond(0).withNano(0);
        if (c.isBefore(minimo)) c = c.plusWeeks(1);
        return c.toInstant().toEpochMilli();
    }

    public static String quando(long ms) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DATA);
    }

    public String declararGuerra(Player p, String nomeAlvo) {
        Reino r = de(p);
        if (r == null || !r.rei.equals(p.getUniqueId())) return "Só o Rei declara guerra.";
        Reino alvo = porNome(nomeAlvo);
        if (alvo == null) return "Não existe reino com esse nome.";
        if (alvo == r) return "Seu próprio reino?";
        for (Guerra g : guerras) if (g.envolve(r.id) && g.envolve(alvo.id)) return "Já há uma guerra marcada entre vocês (" + quando(g.inicio) + ").";
        Long tregua = r.treguas.get(alvo.id);
        if (tregua != null && System.currentTimeMillis() < tregua) return "Vocês estão em trégua até " + quando(tregua) + ".";
        long inicio = proximoHorario(System.currentTimeMillis() + cfg().gueAvisoHoras * 3_600_000L);
        Guerra g = new Guerra(r.id, alvo.id, inicio, inicio + cfg().gueDuracaoMin * 60_000L);
        guerras.add(g);
        Bukkit.broadcast(Component.text("⚔ ", NamedTextColor.DARK_RED).append(Component.text(r.nome, r.cor, TextDecoration.BOLD))
                .append(Component.text(" declarou guerra a ", NamedTextColor.RED)).append(Component.text(alvo.nome, alvo.cor, TextDecoration.BOLD))
                .append(Component.text("! A batalha será " + quando(inicio) + ".", NamedTextColor.RED)));
        for (Player o : Bukkit.getOnlinePlayers()) o.playSound(o.getLocation(), Sound.EVENT_RAID_HORN, 0.8f, 0.8f);
        salvar();
        return null;
    }

    public String pedirPaz(Player p, String nomeAlvo) {
        Reino r = de(p);
        if (r == null || !r.rei.equals(p.getUniqueId())) return "Só o Rei pede paz.";
        Reino alvo = porNome(nomeAlvo);
        if (alvo == null) return "Não existe reino com esse nome.";
        Guerra g = null;
        for (Guerra x : guerras) if (x.envolve(r.id) && x.envolve(alvo.id)) g = x;
        if (g == null) return "Vocês não estão em guerra.";
        if (alvo.id.equals(pedidosDePaz.get(alvo.rei)) || r.id.equals(pedidosDePaz.get(alvo.rei))) {
            pedidosDePaz.remove(alvo.rei);
            encerrar(g, true);
            return null;
        }
        pedidosDePaz.put(p.getUniqueId(), alvo.id);
        Player outroRei = Bukkit.getPlayer(alvo.rei);
        if (outroRei != null) {
            outroRei.sendMessage(Component.text("☮ O Rei de " + r.nome + " pede paz. ", NamedTextColor.AQUA)
                    .append(Component.text("[Aceitar paz]", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/reino paz " + r.nome))));
        }
        p.sendMessage(Component.text("☮ Pedido de paz enviado ao Rei de " + alvo.nome + ".", NamedTextColor.AQUA));
        return null;
    }

    /** Para testar: guerra começando agora. */
    public String guerraAgora(String a, String b) {
        Reino ra = porNome(a), rb = porNome(b);
        if (ra == null || rb == null || ra == rb) return "Reinos inválidos.";
        guerras.removeIf(g -> g.envolve(ra.id) && g.envolve(rb.id));
        long agora = System.currentTimeMillis();
        guerras.add(new Guerra(ra.id, rb.id, agora, agora + cfg().gueDuracaoMin * 60_000L));
        return null;
    }

    public int encerrarTodas() {
        int n = 0;
        for (Guerra g : List.copyOf(guerras)) {
            if (g.ativa(System.currentTimeMillis())) { encerrar(g, false); n++; }
        }
        return n;
    }

    /** Roda 1x por segundo. */
    public void tick() {
        long agora = System.currentTimeMillis();
        for (Guerra g : List.copyOf(guerras)) {
            Reino a = reinos.get(g.atacante), d = reinos.get(g.defensor);
            if (a == null || d == null) { guerras.remove(g); continue; }
            if (!g.comecou) {
                long falta = g.inicio - agora;
                for (int min : new int[]{60, 10, 1}) {
                    if (falta <= min * 60_000L && falta > 0 && g.avisos.add(min)) {
                        Bukkit.broadcast(Component.text("⚔ A guerra entre " + a.nome + " e " + d.nome + " começa em " + min
                                + (min == 1 ? " minuto!" : " minutos!"), NamedTextColor.RED));
                    }
                }
                if (agora >= g.inicio) comecar(g, a, d);
                continue;
            }
            if (agora >= g.fim) { encerrar(g, false); continue; }
            capturas(g, a, d);
            capturas(g, d, a);
        }
    }

    private void comecar(Guerra g, Reino a, Reino d) {
        g.comecou = true;
        for (Player o : Bukkit.getOnlinePlayers()) {
            Reino r = de(o);
            boolean lutando = r == a || r == d;
            if (lutando) {
                o.showTitle(Title.title(Component.text("⚔ GUERRA ⚔", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                        Component.text(a.nome + " × " + d.nome, NamedTextColor.RED),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(800))));
            }
            o.playSound(o.getLocation(), Sound.EVENT_RAID_HORN, 1f, lutando ? 0.7f : 1f);
        }
        Bukkit.broadcast(Component.text("⚔ Começou a guerra entre ", NamedTextColor.RED).append(Component.text(a.nome, a.cor, TextDecoration.BOLD))
                .append(Component.text(" e ", NamedTextColor.RED)).append(Component.text(d.nome, d.cor, TextDecoration.BOLD))
                .append(Component.text(". PvP livre entre os dois e Marcos podem ser capturados por " + cfg().gueDuracaoMin + " minutos!", NamedTextColor.RED)));
    }

    /** Membros de {@code atacante} perto dos Marcos de {@code defensor}, sem defensores: capturam. */
    private void capturas(Guerra g, Reino atacante, Reino defensor) {
        for (UUID membro : defensor.membros.keySet()) {
            if (g.capturados.contains(membro)) continue;
            Territorio t = plugin.territorios().de(membro);
            if (t == null) continue;
            Location marco = t.marco();
            if (marco == null || !marco.isChunkLoaded()) continue;
            Location centro = marco.clone().add(0.5, 0.5, 0.5);
            int atacantes = 0, defensores = 0;
            for (Player o : centro.getWorld().getPlayers()) {
                if (o.isDead() || o.getGameMode() == GameMode.SPECTATOR || o.getLocation().distanceSquared(centro) > RAIO_CAPTURA * RAIO_CAPTURA) continue;
                UUID ro = porJogador.get(o.getUniqueId());
                if (atacante.id.equals(ro)) atacantes++;
                else if (defensor.id.equals(ro)) defensores++;
            }
            defensores += plugin.colonias().soldadosPerto(defensor.id, centro, RAIO_CAPTURA);
            int prog = g.captura.getOrDefault(membro, 0);
            if (atacantes > 0 && defensores == 0) prog++;
            else if (atacantes == 0 && prog > 0) prog = Math.max(0, prog - 2);
            g.captura.put(membro, prog);
            BossBar barra = g.barras.computeIfAbsent(membro, k -> BossBar.bossBar(Component.empty(), 0, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10));
            if (prog <= 0) {
                esconder(barra);
                continue;
            }
            float f = Math.min(1, prog / (float) cfg().gueCapturaSeg);
            barra.name(Component.text("⚔ " + atacante.nome + " capturando o Marco de " + t.nomeDono()
                    + (defensores > 0 && atacantes > 0 ? " (disputado!)" : ""), atacante.cor));
            barra.progress(f);
            for (Player o : centro.getWorld().getPlayers()) {
                if (o.getLocation().distanceSquared(centro) < 48 * 48) o.showBossBar(barra);
                else o.hideBossBar(barra);
            }
            if (prog >= cfg().gueCapturaSeg) capturar(g, atacante, defensor, t, centro);
        }
    }

    private static void esconder(BossBar b) {
        for (Player o : Bukkit.getOnlinePlayers()) o.hideBossBar(b);
    }

    private void capturar(Guerra g, Reino atacante, Reino defensor, Territorio t, Location centro) {
        g.capturados.add(t.dono());
        BossBar b = g.barras.remove(t.dono());
        if (b != null) esconder(b);
        g.pontuar(atacante.id, 10);
        // O território capturado perde os chunks mais longe do Marco.
        int perde = cfg().gueChunksPorCaptura;
        long centroChunk = t.chunkDoMarco();
        int mx = Territorio.chunkX(centroChunk), mz = Territorio.chunkZ(centroChunk);
        List<Long> ordem = new ArrayList<>(t.chunks());
        ordem.remove(centroChunk);
        ordem.sort(Comparator.comparingLong((Long k) -> {
            long dx = Territorio.chunkX(k) - mx, dz = Territorio.chunkZ(k) - mz;
            return dx * dx + dz * dz;
        }).reversed());
        int perdidos = 0;
        for (Long k : ordem) {
            if (perdidos >= perde) break;
            if (plugin.territorios().liberar(t, t.mundo(), Territorio.chunkX(k), Territorio.chunkZ(k)) == null) perdidos++;
        }
        atacante.chunksConquistados += perdidos;
        centro.getWorld().strikeLightningEffect(centro);
        Bukkit.broadcast(Component.text("⚔ ", NamedTextColor.DARK_RED).append(Component.text(atacante.nome, atacante.cor, TextDecoration.BOLD))
                .append(Component.text(" capturou o Marco de " + t.nomeDono() + " (" + defensor.nome + ")! "
                        + perdidos + " chunks perdidos.", NamedTextColor.RED)));
        salvar();
    }

    private void encerrar(Guerra g, boolean paz) {
        guerras.remove(g);
        for (BossBar b : g.barras.values()) esconder(b);
        Reino a = reinos.get(g.atacante), d = reinos.get(g.defensor);
        if (a == null || d == null) return;
        long tregua = System.currentTimeMillis() + cfg().gueTreguaDias * 86_400_000L;
        a.treguas.put(d.id, tregua);
        d.treguas.put(a.id, tregua);
        if (paz) {
            Bukkit.broadcast(Component.text("☮ " + a.nome + " e " + d.nome + " fizeram as pazes.", NamedTextColor.AQUA));
            salvar();
            return;
        }
        Reino vencedor = g.pontosAtacante > g.pontosDefensor ? a : g.pontosDefensor > g.pontosAtacante ? d : null;
        Component placar = Component.text(" (" + a.nome + " " + g.pontosAtacante + " × " + g.pontosDefensor + " " + d.nome + ")", NamedTextColor.GRAY);
        if (vencedor == null) {
            Bukkit.broadcast(Component.text("⚔ A guerra entre " + a.nome + " e " + d.nome + " terminou empatada.", NamedTextColor.YELLOW).append(placar));
        } else {
            Reino perdedor = vencedor == a ? d : a;
            long saque = (long) Math.floor(perdedor.tesouro * cfg().gueTesouroVencedor);
            perdedor.tesouro -= saque;
            vencedor.tesouro += saque;
            vencedor.vitorias++;
            Bukkit.broadcast(Component.text("⚔ ", NamedTextColor.GOLD).append(Component.text(vencedor.nome, vencedor.cor, TextDecoration.BOLD))
                    .append(Component.text(" venceu a guerra contra " + perdedor.nome + " e levou " + saque + " esmeraldas do tesouro!", NamedTextColor.GOLD))
                    .append(placar));
            for (UUID m : vencedor.membros.keySet()) {
                Player o = Bukkit.getPlayer(m);
                if (o != null) {
                    o.playSound(o.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    plugin.titulos().registrar(o, "guerras_vencidas", 1);
                }
            }
        }
        salvar();
    }

    /** Pontos por abate durante a guerra (jogador = 2, soldado = 1). */
    public void pontuarAbate(UUID reinoAssassino, UUID reinoVitima, int pontos) {
        Guerra g = guerraAtiva(reinoAssassino, reinoVitima);
        if (g != null) g.pontuar(reinoAssassino, pontos);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(PlayerDeathEvent e) {
        Player vitima = e.getEntity(), assassino = vitima.getKiller();
        if (assassino == null) return;
        UUID ra = porJogador.get(assassino.getUniqueId()), rv = porJogador.get(vitima.getUniqueId());
        if (ra != null && rv != null) pontuarAbate(ra, rv, 2);
    }

    // =====================================================================
    //  Menu (/reino)
    // =====================================================================

    public void abrir(Player p) {
        Reino r = de(p);
        if (r == null) {
            p.sendMessage(Component.text("♛ Você não está num reino. Funde um com /reino fundar <nome> (precisa de colônia nível "
                    + cfg().reiNivelColonia + " e " + cfg().reiCustoFundar + " esmeraldas) ou aceite um convite.", NamedTextColor.GOLD));
            return;
        }
        Tela t = new Tela(r);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♛ " + r.nome));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Reino r = t.reino;
        Inventory inv = t.inventario;
        inv.clear();
        int soldados = 0;
        for (UUID m : r.membros.keySet()) {
            Colonia c = plugin.colonias().de(m);
            if (c != null) soldados += plugin.colonias().quantosSoldados(c);
        }
        inv.setItem(4, item(Material.GOLDEN_HELMET, Component.text("♛ " + r.nome, r.cor, TextDecoration.BOLD), List.of(
                linha("Rei: ", r.nomeRei(), NamedTextColor.GOLD),
                linha("Membros: ", String.valueOf(r.membros.size()), NamedTextColor.WHITE),
                linha("Soldados: ", String.valueOf(soldados), NamedTextColor.RED),
                linha("Tesouro: ", r.tesouro + " esmeraldas", NamedTextColor.GREEN),
                linha("Vitórias: ", String.valueOf(r.vitorias), NamedTextColor.YELLOW),
                linha("Chunks conquistados: ", "+" + r.chunksConquistados + " para o Rei", NamedTextColor.AQUA),
                Component.empty(),
                Component.text("Seu cargo: " + r.cargo(p.getUniqueId()).nome(), r.cargo(p.getUniqueId()).cor()))));
        inv.setItem(10, item(Material.EMERALD, Component.text("Depositar 16 esmeraldas", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text("Qualquer membro pode depositar.", NamedTextColor.GRAY),
                Component.text("/reino tesouro depositar <qtd>", NamedTextColor.DARK_GRAY))));
        inv.setItem(11, item(Material.EMERALD_BLOCK, Component.text("Sacar 16 esmeraldas", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Só o Rei e os Nobres.", NamedTextColor.GRAY),
                Component.text("/reino tesouro sacar <qtd>", NamedTextColor.DARK_GRAY))));

        List<Component> guerra = new ArrayList<>();
        long agora = System.currentTimeMillis();
        boolean alguma = false;
        for (Guerra g : guerras) {
            if (!g.envolve(r.id)) continue;
            alguma = true;
            Reino o = reinos.get(g.outro(r.id));
            if (o == null) continue;
            if (g.ativa(agora)) {
                guerra.add(Component.text("⚔ AGORA contra " + o.nome, NamedTextColor.RED, TextDecoration.BOLD));
                guerra.add(Component.text("  Placar: " + g.pontos(r.id) + " × " + g.pontos(o.id), NamedTextColor.WHITE));
                guerra.add(Component.text("  Acaba " + quando(g.fim), NamedTextColor.GRAY));
            } else {
                guerra.add(Component.text("⚔ Contra " + o.nome + ": " + quando(g.inicio), NamedTextColor.GOLD));
            }
        }
        if (!alguma) guerra.add(Component.text("Nenhuma guerra marcada.", NamedTextColor.GRAY));
        guerra.add(Component.empty());
        guerra.add(Component.text("Guerras: " + diaPt(cfg().gueDia) + " às " + cfg().gueHora + "h, por " + cfg().gueDuracaoMin + " min.", NamedTextColor.GRAY));
        guerra.add(Component.text("Capture Marcos inimigos ficando " + cfg().gueCapturaSeg + "s perto", NamedTextColor.GRAY));
        guerra.add(Component.text("deles sem nenhum defensor por perto.", NamedTextColor.GRAY));
        guerra.add(Component.text("Abate de jogador = 2 pontos, soldado = 1,", NamedTextColor.GRAY));
        guerra.add(Component.text("Marco capturado = 10.", NamedTextColor.GRAY));
        guerra.add(Component.empty());
        guerra.add(Component.text("/reino guerra <reino>  ·  /reino paz <reino>", NamedTextColor.DARK_GRAY));
        inv.setItem(13, item(Material.IRON_SWORD, Component.text("Guerras", NamedTextColor.RED, TextDecoration.BOLD), guerra));

        List<Component> outros = new ArrayList<>();
        for (Reino o : reinos.values()) {
            if (o == r) continue;
            Long tregua = r.treguas.get(o.id);
            outros.add(Component.text(" " + o.nome, o.cor).append(Component.text(" — " + o.membros.size() + " membros, " + o.vitorias + " vitórias"
                    + (tregua != null && tregua > agora ? " (trégua)" : ""), NamedTextColor.GRAY)));
        }
        if (outros.isEmpty()) outros.add(Component.text("Nenhum outro reino ainda.", NamedTextColor.GRAY));
        inv.setItem(15, item(Material.FILLED_MAP, Component.text("Outros reinos", NamedTextColor.AQUA, TextDecoration.BOLD), outros));
        inv.setItem(16, item(Material.BOOK, Component.text("Comandos", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("/reino convidar <nick>", NamedTextColor.WHITE),
                Component.text("/reino cargo <nick> <nobre|cavaleiro|cidadao>", NamedTextColor.WHITE),
                Component.text("/reino expulsar <nick>  ·  /reino sair", NamedTextColor.WHITE),
                Component.text("/reino coroa <nick>  ·  /reino desfazer", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Soldados: dê a profissão Soldado ou", NamedTextColor.GRAY),
                Component.text("Arqueiro a moradores da sua colônia.", NamedTextColor.GRAY))));

        int[] slots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int i = 0;
        List<Map.Entry<UUID, Cargo>> lista = new ArrayList<>(r.membros.entrySet());
        lista.sort(Comparator.comparingInt(e -> e.getValue().ordinal()));
        for (Map.Entry<UUID, Cargo> e : lista) {
            if (i >= slots.length) break;
            OfflinePlayer o = Bukkit.getOfflinePlayer(e.getKey());
            ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
            cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(o));
            inv.setItem(slots[i++], enfeitar(cabeca, Component.text(e.getValue().icone() + " " + r.nomeDe(e.getKey()), e.getValue().cor(), TextDecoration.BOLD),
                    List.of(Component.text(e.getValue().nome(), e.getValue().cor()),
                            Component.text(o.isOnline() ? "● online" : "○ offline", o.isOnline() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY))));
        }
        inv.setItem(49, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int k = 0; k < inv.getSize(); k++) if (inv.getItem(k) == null) inv.setItem(k, vidro);
    }

    private static String diaPt(java.time.DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "segunda";
            case TUESDAY -> "terça";
            case WEDNESDAY -> "quarta";
            case THURSDAY -> "quinta";
            case FRIDAY -> "sexta";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        String erro = null;
        switch (e.getSlot()) {
            case 49 -> { p.closeInventory(); return; }
            case 10 -> erro = depositar(p, 16);
            case 11 -> erro = sacar(p, 16);
            default -> { return; }
        }
        if (erro != null) {
            p.sendMessage(Component.text(erro, NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
        } else {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1f);
        }
        if (reinos.containsKey(t.reino.id)) desenhar(p, t);
        else p.closeInventory();
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Salvar e carregar
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection sec = y.getConfigurationSection("reinos");
        if (sec != null) {
            for (String chave : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(chave);
                if (s == null) continue;
                try {
                    UUID id = UUID.fromString(chave);
                    UUID rei = UUID.fromString(s.getString("rei", ""));
                    NamedTextColor cor = NamedTextColor.NAMES.value(s.getString("cor", "gold"));
                    Reino r = new Reino(id, s.getString("nome", "Reino"), cor == null ? NamedTextColor.GOLD : cor, rei, "?");
                    r.membros.clear();
                    ConfigurationSection ms = s.getConfigurationSection("membros");
                    if (ms != null) {
                        for (String m : ms.getKeys(false)) {
                            UUID u = UUID.fromString(m);
                            Cargo c = Cargo.porId(ms.getString(m + ".cargo", "cidadao"));
                            r.membros.put(u, c == null ? Cargo.CIDADAO : c);
                            r.nomes.put(u, ms.getString(m + ".nome", "?"));
                            porJogador.put(u, id);
                        }
                    }
                    r.membros.put(rei, Cargo.REI);
                    porJogador.put(rei, id);
                    r.tesouro = s.getLong("tesouro");
                    r.chunksConquistados = s.getInt("chunks-conquistados");
                    r.vitorias = s.getInt("vitorias");
                    ConfigurationSection ts = s.getConfigurationSection("treguas");
                    if (ts != null) for (String o : ts.getKeys(false)) r.treguas.put(UUID.fromString(o), ts.getLong(o));
                    reinos.put(id, r);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        for (String linha : y.getStringList("guerras")) {
            String[] p = linha.split(";");
            if (p.length < 4) continue;
            try {
                Guerra g = new Guerra(UUID.fromString(p[0]), UUID.fromString(p[1]), Long.parseLong(p[2]), Long.parseLong(p[3]));
                if (p.length >= 6) {
                    g.pontosAtacante = Integer.parseInt(p[4]);
                    g.pontosDefensor = Integer.parseInt(p[5]);
                }
                g.comecou = System.currentTimeMillis() >= g.inicio;
                guerras.add(g);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Reino r : reinos.values()) {
            String b = "reinos." + r.id + ".";
            y.set(b + "nome", r.nome);
            y.set(b + "cor", NamedTextColor.NAMES.key(r.cor));
            y.set(b + "rei", r.rei.toString());
            y.set(b + "tesouro", r.tesouro);
            y.set(b + "chunks-conquistados", r.chunksConquistados);
            y.set(b + "vitorias", r.vitorias);
            r.membros.forEach((u, c) -> {
                y.set(b + "membros." + u + ".cargo", c.id());
                y.set(b + "membros." + u + ".nome", r.nomeDe(u));
            });
            r.treguas.forEach((o, ate) -> y.set(b + "treguas." + o, ate));
        }
        List<String> gs = new ArrayList<>();
        for (Guerra g : guerras) gs.add(g.atacante + ";" + g.defensor + ";" + g.inicio + ";" + g.fim + ";" + g.pontosAtacante + ";" + g.pontosDefensor);
        y.set("guerras", gs);
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar reinos.yml: " + ex.getMessage());
        }
    }

    /** Esconde as barras de captura (ao desligar). */
    public void parar() {
        for (Guerra g : guerras) for (BossBar b : g.barras.values()) esconder(b);
        salvar();
    }

    // =====================================================================
    //  Ajudantes
    // =====================================================================

    private static Component linha(String rotulo, String valor, net.kyori.adventure.text.format.TextColor cor) {
        return Component.text(rotulo, NamedTextColor.GRAY).append(Component.text(valor, cor));
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return enfeitar(new ItemStack(m), nome, lore);
    }

    private static ItemStack enfeitar(ItemStack i, Component nome, List<Component> lore) {
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
