package br.rpgatributos.territorio;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Os territórios dos jogadores: quem é dono de cada chunk, quem pode construir,
 * reivindicar e liberar chunks, membros, regras e o arquivo territorios.yml.
 */
public final class Territorios implements Listener {

    /** Como o jogador se relaciona com um território. */
    public enum Relacao { PROPRIO, AMIGO, OUTRO, LIVRE }

    public static final TextColor COR = TextColor.color(0x4CAF50);
    private static final UUID LIVRE = new UUID(0, 0);
    private static final int[][] VIZINHOS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Territorio> porDono = new HashMap<>();
    /** mundo → chunk → território. */
    private final Map<String, Map<Long, Territorio>> indice = new HashMap<>();
    /** Admins ignorando as proteções. */
    private final Set<UUID> ignorando = new HashSet<>();
    /** Em que território cada jogador estava (para avisar quando muda). */
    private final Map<UUID, UUID> ondeEsta = new HashMap<>();
    private final Map<UUID, BukkitTask> bordas = new HashMap<>();

    public Territorios(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "territorios.yml");
    }

    private Settings cfg() { return plugin.settings(); }

    // =====================================================================
    //  Consultas
    // =====================================================================

    public Territorio em(Location l) {
        if (l == null || l.getWorld() == null) return null;
        return em(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
    }

    public Territorio em(Block b) {
        return em(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4);
    }

    public Territorio em(String mundo, int cx, int cz) {
        Map<Long, Territorio> m = indice.get(mundo);
        return m == null ? null : m.get(Territorio.chave(cx, cz));
    }

    public Territorio de(UUID dono) {
        return porDono.get(dono);
    }

    public Collection<Territorio> todos() {
        return Collections.unmodifiableCollection(porDono.values());
    }

    /** Dono, membro ou (com a regra ligada) alguém da party do dono. */
    public boolean amigo(Territorio t, UUID id) {
        if (t.dono().equals(id) || t.membro(id)) return true;
        return t.flag(Flag.PARTY) && plugin.parties().mesmaParty(t.dono(), id);
    }

    public boolean podeConstruir(Player p, Location l) {
        return ignorando.contains(p.getUniqueId()) || podeConstruir(p.getUniqueId(), l);
    }

    public boolean podeConstruir(Player p, Block b) {
        return podeConstruir(p, b.getLocation());
    }

    /** Funciona também com o jogador offline (ex.: quem jogou os itens de um ritual). */
    public boolean podeConstruir(UUID id, Location l) {
        Territorio t = em(l);
        return t == null || amigo(t, id) || ignorando.contains(id);
    }

    /** Pode construir ali, ou a regra está liberada para visitantes. */
    public boolean permite(Player p, Location l, Flag f) {
        Territorio t = em(l);
        return t == null || t.flag(f) || podeConstruir(p, l);
    }

    /** A regra vale nesse lugar? Fora de território, tudo funciona como no jogo normal. */
    public boolean flagAqui(Location l, Flag f) {
        Territorio t = em(l);
        return t == null || t.flag(f);
    }

    public Relacao relacao(Player p, Territorio t) {
        if (t == null) return Relacao.LIVRE;
        if (t.dono().equals(p.getUniqueId())) return Relacao.PROPRIO;
        return amigo(t, p.getUniqueId()) ? Relacao.AMIGO : Relacao.OUTRO;
    }

    public boolean ignorando(Player p) {
        return ignorando.contains(p.getUniqueId());
    }

    /** Liga/desliga o modo admin que ignora as proteções. @return se ficou ligado. */
    public boolean alternarIgnorar(Player p) {
        if (ignorando.remove(p.getUniqueId())) return false;
        ignorando.add(p.getUniqueId());
        return true;
    }

    /** Quantos chunks o jogador pode ter: base + 1 a cada N níveis somados, até o máximo. */
    public int limite(Player p) {
        int extra = plugin.stats().nivelTotal(p) / cfg().terNiveisPorChunk;
        // Chunks conquistados em guerra (do Rei) passam do máximo normal.
        return Math.min(cfg().terChunksMax, cfg().terChunksIniciais + extra) + plugin.reinos().chunksExtras(p.getUniqueId());
    }

    /** Pode gerenciar (reivindicar, membros, regras)? O dono, ou um admin ignorando as proteções. */
    public boolean gerencia(Player p, Territorio t) {
        return t != null && (t.dono().equals(p.getUniqueId()) || ignorando(p));
    }

    // =====================================================================
    //  Fundar, reivindicar, liberar, abandonar
    // =====================================================================

    /** Motivo para não poder fundar um território com o Marco nesse bloco, ou null. */
    public String podeFundar(Player p, Block marco) {
        if (cfg().terMundosBloqueados.contains(marco.getWorld().getName())) return "Não dá para criar território neste mundo.";
        Territorio meu = de(p.getUniqueId());
        if (meu != null) {
            return "Você já tem um território (Marco em " + meu.marcoX() + " " + meu.marcoY() + " " + meu.marcoZ()
                    + "). Para mudar de lugar, abandone o antigo pelo menu.";
        }
        Territorio la = em(marco);
        if (la != null) return "Esse chunk já é do território de " + la.nomeDono() + ".";
        return null;
    }

    /** Cria o território: o chunk do Marco e os 8 em volta (os que estiverem livres). */
    public Territorio fundar(Player p, Block marco, ItemStack bandeira) {
        Territorio t = new Territorio(p.getUniqueId(), p.getName(), marco.getLocation(), bandeira);
        porDono.put(t.dono(), t);
        int cx = marco.getX() >> 4, cz = marco.getZ() >> 4;
        indexar(t, cx, cz);
        int limite = limite(p);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && t.chunks().size() < limite && em(t.mundo(), cx + dx, cz + dz) == null) {
                    indexar(t, cx + dx, cz + dz);
                }
            }
        }
        plugin.titulos().registrar(p, "territorio", 1);
        plugin.titulos().definirMinimo(p, "chunks", t.chunks().size());
        salvar();
        ondeEsta.remove(p.getUniqueId());
        return t;
    }

    private void indexar(Territorio t, int cx, int cz) {
        long k = Territorio.chave(cx, cz);
        t.chunksEditaveis().add(k);
        indice.computeIfAbsent(t.mundo(), m -> new HashMap<>()).put(k, t);
    }

    private void desindexar(Territorio t, long k) {
        t.chunksEditaveis().remove(k);
        Map<Long, Territorio> m = indice.get(t.mundo());
        if (m != null && m.get(k) == t) m.remove(k);
    }

    /** O chunk (cx, cz) pode ser reivindicado por esse jogador? Motivo, ou null se pode. */
    public String impedimentoReivindicar(Player p, String mundo, int cx, int cz) {
        Territorio t = de(p.getUniqueId());
        if (t == null) return "Você ainda não tem território. Crie um Marco do Território (veja o /guia).";
        if (!t.mundo().equals(mundo)) return "Seu território fica em outro mundo.";
        Territorio dono = em(mundo, cx, cz);
        if (dono == t) return "Esse chunk já é seu.";
        if (dono != null) return "Esse chunk é do território de " + dono.nomeDono() + ".";
        int limite = limite(p);
        if (t.chunks().size() >= limite) {
            return "Limite de " + limite + " chunks. Suba seus atributos para ganhar mais (+1 a cada "
                    + cfg().terNiveisPorChunk + " níveis somados).";
        }
        if (cfg().terExigirVizinho && !encosta(t, cx, cz)) return "O chunk precisa encostar no seu território.";
        return null;
    }

    private boolean encosta(Territorio t, int cx, int cz) {
        for (int[] v : VIZINHOS) if (t.chunks().contains(Territorio.chave(cx + v[0], cz + v[1]))) return true;
        return false;
    }

    /** @return motivo de não ter conseguido, ou null se reivindicou. */
    public String reivindicar(Player p, String mundo, int cx, int cz) {
        String erro = impedimentoReivindicar(p, mundo, cx, cz);
        if (erro != null) return erro;
        Territorio t = de(p.getUniqueId());
        indexar(t, cx, cz);
        plugin.titulos().definirMinimo(p, "chunks", t.chunks().size());
        salvar();
        ondeEsta.remove(p.getUniqueId());
        return null;
    }

    /** @return motivo de não ter conseguido, ou null se liberou. */
    public String liberar(Territorio t, String mundo, int cx, int cz) {
        if (t == null) return "Você não tem território.";
        long k = Territorio.chave(cx, cz);
        if (!t.mundo().equals(mundo) || !t.chunks().contains(k)) return "Esse chunk não é do território.";
        if (k == t.chunkDoMarco()) return "O chunk do Marco não pode ser liberado.";
        desindexar(t, k);
        salvar();
        return null;
    }

    /** Apaga o território (o Marco volta a ser uma magnetita comum e a bandeira cai no chão). */
    public void abandonar(Territorio t) {
        for (Long k : new ArrayList<>(t.chunks())) desindexar(t, k);
        porDono.remove(t.dono());
        Location marco = t.marco();
        if (marco != null) {
            if (plugin.marcos().eh(marco.getBlock())) plugin.marcos().remover(marco, false);
            marco.getWorld().dropItemNaturally(marco.clone().add(0.5, 1.2, 0.5), t.bandeira());
            marco.getWorld().playSound(marco, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
        }
        ondeEsta.clear();
        salvar();
    }

    public String adicionarMembro(Territorio t, OfflinePlayer alvo) {
        if (alvo.getUniqueId().equals(t.dono())) return "O dono já manda em tudo.";
        if (t.membro(alvo.getUniqueId())) return alvo.getName() + " já é membro.";
        if (t.membros().size() >= cfg().terMembrosMax) return "Limite de " + cfg().terMembrosMax + " membros.";
        t.membrosEditaveis().put(alvo.getUniqueId(), alvo.getName() == null ? "?" : alvo.getName());
        salvar();
        Player online = alvo.getPlayer();
        if (online != null) {
            online.sendMessage(Component.text("⚑ Agora você é membro do território de " + t.nomeDono()
                    + ": pode construir e mexer em tudo lá.", COR));
        }
        return null;
    }

    public void removerMembro(Territorio t, UUID id) {
        if (t.membrosEditaveis().remove(id) == null) return;
        salvar();
        Player online = plugin.getServer().getPlayer(id);
        if (online != null) online.sendMessage(Component.text("⚑ Você não é mais membro do território de " + t.nomeDono() + ".", NamedTextColor.GRAY));
    }

    public void alternarFlag(Territorio t, Flag f) {
        t.flag(f, !t.flag(f));
        salvar();
    }

    public void trocarBandeira(Territorio t, ItemStack bandeira) {
        t.bandeira(bandeira);
        salvar();
        Location marco = t.marco();
        if (marco != null && plugin.marcos().eh(marco.getBlock())) plugin.marcos().atualizarDisplays(marco);
    }

    // =====================================================================
    //  Aviso ao entrar/sair de um território
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAndar(PlayerMoveEvent e) {
        Location de = e.getFrom(), para = e.getTo();
        if (de.getWorld() == para.getWorld() && de.getBlockX() >> 4 == para.getBlockX() >> 4
                && de.getBlockZ() >> 4 == para.getBlockZ() >> 4) return;
        verificarEntrada(e.getPlayer(), para);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTeleportar(PlayerTeleportEvent e) {
        verificarEntrada(e.getPlayer(), e.getTo());
    }

    private void verificarEntrada(Player p, Location para) {
        Territorio t = em(para);
        UUID agora = t == null ? LIVRE : t.dono();
        UUID antes = ondeEsta.put(p.getUniqueId(), agora);
        if (antes == null || antes.equals(agora)) return;
        Component texto = switch (relacao(p, t)) {
            case LIVRE -> Component.text("☘ Terras livres", NamedTextColor.GRAY);
            case PROPRIO -> Component.text("⚑ Seu território", COR);
            case AMIGO -> Component.text("⚑ Território de " + t.nomeDono() + " (você constrói aqui)", NamedTextColor.AQUA);
            case OUTRO -> Component.text("⚑ Território de " + t.nomeDono(), NamedTextColor.GOLD);
        };
        if (t != null && !t.flag(Flag.PVP)) texto = texto.append(Component.text("  ☮ sem PvP", NamedTextColor.GRAY));
        p.showTitle(Title.title(Component.empty(), texto,
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1500), Duration.ofMillis(400))));
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean mudou = false;
        Territorio meu = de(p.getUniqueId());
        if (meu != null && !p.getName().equals(meu.nomeDono())) {
            meu.nomeDono(p.getName());
            mudou = true;
        }
        for (Territorio t : porDono.values()) {
            if (t.membro(p.getUniqueId()) && !p.getName().equals(t.membros().get(p.getUniqueId()))) {
                t.membrosEditaveis().put(p.getUniqueId(), p.getName());
                mudou = true;
            }
        }
        if (mudou) salvar();
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ondeEsta.remove(id);
        ignorando.remove(id);
        BukkitTask b = bordas.remove(id);
        if (b != null) b.cancel();
    }

    // =====================================================================
    //  Mostrar as bordas com partículas (só o jogador vê)
    // =====================================================================

    public void mostrarBordas(Player p, int segundos) {
        BukkitTask antiga = bordas.remove(p.getUniqueId());
        if (antiga != null) antiga.cancel();
        int[] vezes = {segundos * 2};
        BukkitTask nova = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || vezes[0]-- <= 0) {
                BukkitTask t = bordas.remove(p.getUniqueId());
                if (t != null) t.cancel();
                return;
            }
            desenharBordas(p);
        }, 0L, 10L);
        bordas.put(p.getUniqueId(), nova);
    }

    private void desenharBordas(Player p) {
        World w = p.getWorld();
        String mundo = w.getName();
        int pcx = p.getLocation().getBlockX() >> 4, pcz = p.getLocation().getBlockZ() >> 4;
        double y = p.getLocation().getY() + 1.2;
        for (int cx = pcx - 4; cx <= pcx + 4; cx++) {
            for (int cz = pcz - 4; cz <= pcz + 4; cz++) {
                Territorio t = em(mundo, cx, cz);
                if (t == null) continue;
                Particle.DustOptions cor = new Particle.DustOptions(switch (relacao(p, t)) {
                    case PROPRIO -> Color.fromRGB(0x55FF55);
                    case AMIGO -> Color.fromRGB(0x55FFFF);
                    default -> Color.fromRGB(0xFF5555);
                }, 1.3f);
                int x0 = cx << 4, z0 = cz << 4;
                // Só desenha os lados que dão para fora do território.
                if (em(mundo, cx, cz - 1) != t) linha(p, x0, z0, 1, 0, y, cor);
                if (em(mundo, cx, cz + 1) != t) linha(p, x0, z0 + 16, 1, 0, y, cor);
                if (em(mundo, cx - 1, cz) != t) linha(p, x0, z0, 0, 1, y, cor);
                if (em(mundo, cx + 1, cz) != t) linha(p, x0 + 16, z0, 0, 1, y, cor);
            }
        }
    }

    private static void linha(Player p, int x, int z, int dx, int dz, double y, Particle.DustOptions cor) {
        for (int i = 0; i <= 16; i += 2) {
            p.spawnParticle(Particle.DUST, x + dx * i, y, z + dz * i, 1, 0, 0, 0, 0, cor);
            p.spawnParticle(Particle.DUST, x + dx * i, y + 1.5, z + dz * i, 1, 0, 0, 0, 0, cor);
        }
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        porDono.clear();
        indice.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection raiz = y.getConfigurationSection("territorios");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(id);
            if (s == null) continue;
            try {
                String[] m = s.getString("marco", "0,0,0").split(",");
                Territorio t = new Territorio(UUID.fromString(id), s.getString("nome", "?"), s.getString("mundo", "world"),
                        Integer.parseInt(m[0]), Integer.parseInt(m[1]), Integer.parseInt(m[2]), s.getItemStack("bandeira"));
                for (String c : s.getStringList("chunks")) {
                    String[] xz = c.split(",");
                    indexar(t, Integer.parseInt(xz[0]), Integer.parseInt(xz[1]));
                }
                ConfigurationSection mem = s.getConfigurationSection("membros");
                if (mem != null) for (String u : mem.getKeys(false)) t.membrosEditaveis().put(UUID.fromString(u), mem.getString(u, "?"));
                for (String f : s.getStringList("regras")) {
                    Flag flag = Flag.porId(f);
                    if (flag != null) t.flag(flag, true);
                }
                porDono.put(t.dono(), t);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Território inválido em territorios.yml (" + id + "): " + ex.getMessage());
            }
        }
        plugin.getLogger().info(porDono.size() + " território(s) carregado(s).");
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Territorio t : porDono.values()) {
            String base = "territorios." + t.dono() + ".";
            y.set(base + "nome", t.nomeDono());
            y.set(base + "mundo", t.mundo());
            y.set(base + "marco", t.marcoX() + "," + t.marcoY() + "," + t.marcoZ());
            y.set(base + "bandeira", t.bandeira());
            List<String> chunks = new ArrayList<>();
            for (long k : t.chunks()) chunks.add(Territorio.chunkX(k) + "," + Territorio.chunkZ(k));
            y.set(base + "chunks", chunks);
            for (Map.Entry<UUID, String> m : t.membros().entrySet()) y.set(base + "membros." + m.getKey(), m.getValue());
            List<String> regras = new ArrayList<>();
            for (Flag f : t.flagsLigadas()) regras.add(f.id());
            y.set(base + "regras", regras);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar territorios.yml: " + ex.getMessage());
        }
    }

    public void pararBordas() {
        bordas.values().forEach(BukkitTask::cancel);
        bordas.clear();
    }

    /** Ícone (para menus) do chunk, conforme a relação. */
    public static Material vidro(Relacao r) {
        return switch (r) {
            case PROPRIO -> Material.LIME_STAINED_GLASS_PANE;
            case AMIGO -> Material.CYAN_STAINED_GLASS_PANE;
            case OUTRO -> Material.RED_STAINED_GLASS_PANE;
            case LIVRE -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }
}
