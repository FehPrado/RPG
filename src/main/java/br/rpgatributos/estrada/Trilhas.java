package br.rpgatributos.estrada;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.detalhes.Carrocas;
import br.rpgatributos.territorio.Flag;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trilhas (2.94): a grama (ou terra, podzol, micélio) muito pisada vira caminho de terra. A pé conta 1 passo,
 * montado 2 e de carroça 3; com {@code passos-para-trilha} passos sem ficar uma hora esquecido, o caminho
 * aparece. As trilhas feitas assim ficam anotadas no próprio pedaço do mundo (PDC do chunk, com o bloco
 * de antes e o último dia em que alguém passou) e voltam a ser grama depois de {@code dias-para-sumir}
 * dias do jogo sem ninguém passar. Caminho feito com pá nunca é anotado: não some.
 * Em território, só com a regra "Trilhas" ligada.
 */
public final class Trilhas implements Listener {

    /** O que vira trilha (e o índice guardado, para voltar ao mesmo bloco). */
    private static final Material[] SOLOS = {Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.MYCELIUM, Material.ROOTED_DIRT};
    /** O que pode estar em cima e some quando a trilha se forma. */
    private static final Set<Material> PISOTEAVEL = EnumSet.of(Material.SHORT_GRASS, Material.FERN, Material.SNOW);
    private static final long ESQUECE_MS = 60L * 60 * 1000;
    private static final int MAX_CONTAGENS = 150_000;

    private final RPGAtributos plugin;
    private final NamespacedKey chave;
    /** Bloco → {passos, último passo em ms}. */
    private final Map<Pos, long[]> passos = new HashMap<>();
    /** O último bloco de cada jogador (só conta ao pisar num bloco novo). */
    private final Map<UUID, Pos> ultimo = new HashMap<>();
    /** Trilhas que já tiveram o dia renovado hoje (para não reescrever o chunk a cada passo). */
    private final Set<Pos> renovadas = new HashSet<>();
    private long diaRenovadas = -1;

    record Pos(UUID mundo, int x, int y, int z) {
        static Pos de(Block b) {
            return new Pos(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
        }
    }

    public Trilhas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.chave = new NamespacedKey(plugin, "trilhas");
    }

    private static long dia(World w) {
        return w.getFullTime() / 24000L;
    }

    // =====================================================================
    //  Passos
    // =====================================================================

    /** A cada 4 ticks. */
    public void tick() {
        if (!plugin.settings().estradaTrilhas) return;
        long agora = System.currentTimeMillis();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isGliding()) continue;
            Entity pe = p.getVehicle() != null ? p.getVehicle() : p;
            if (!pe.isOnGround()) continue;
            Block b = Pisos.debaixo(pe);
            Pos pos = Pos.de(b);
            if (pos.equals(ultimo.put(p.getUniqueId(), pos))) continue;
            Material t = b.getType();
            if (t == Material.DIRT_PATH) {
                renovar(b, pos);
                continue;
            }
            if (indice(t) < 0 || !plugin.territorios().flagAqui(b.getLocation(), Flag.TRILHAS)) continue;
            int peso = pe == p ? 1 : Carrocas.eCarroca(pe) ? 3 : 2;
            long[] c = passos.computeIfAbsent(pos, k -> new long[2]);
            if (agora - c[1] > ESQUECE_MS) c[0] = 0;
            c[0] += peso;
            c[1] = agora;
            if (c[0] >= plugin.settings().estradaPassos && formar(b)) passos.remove(pos);
        }
    }

    /** A cada 5 minutos: esquece as contagens velhas (e tudo, se a lista crescer demais). */
    public void limpar() {
        long agora = System.currentTimeMillis();
        passos.values().removeIf(c -> agora - c[1] > ESQUECE_MS);
        if (passos.size() > MAX_CONTAGENS) passos.clear();
    }

    private static int indice(Material m) {
        for (int i = 0; i < SOLOS.length; i++) if (SOLOS[i] == m) return i;
        return -1;
    }

    /** Vira trilha (se o que está em cima deixar) e anota no chunk. */
    public boolean formar(Block b) {
        int ind = indice(b.getType());
        if (ind < 0) return false;
        Block cima = b.getRelative(BlockFace.UP);
        if (!cima.getType().isAir() && !PISOTEAVEL.contains(cima.getType())) return false;
        if (!cima.getType().isAir()) cima.setType(Material.AIR, false);
        b.setType(Material.DIRT_PATH, false);
        anotar(b, ind);
        b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.05, 0.3, 0, SOLOS[ind].createBlockData());
        b.getWorld().playSound(b.getLocation().add(0.5, 1, 0.5), Sound.BLOCK_ROOTED_DIRT_STEP, 0.4f, 0.8f);
        return true;
    }

    // =====================================================================
    //  Anotações no chunk: x(4) z(4) y+2048(12) solo(4) dia(40)
    // =====================================================================

    private static long empacotar(Block b, int solo, long dia) {
        return ((long) (b.getX() & 15)) | ((long) (b.getZ() & 15) << 4) | ((long) (b.getY() + 2048) << 8)
                | ((long) solo << 20) | (dia << 24);
    }

    private static int lx(long v) { return (int) (v & 15); }
    private static int lz(long v) { return (int) ((v >> 4) & 15); }
    private static int y(long v) { return (int) ((v >> 8) & 4095) - 2048; }
    private static int solo(long v) { return (int) ((v >> 20) & 15); }
    private static long diaDe(long v) { return v >>> 24; }

    private static boolean mesmoBloco(long v, Block b) {
        return lx(v) == (b.getX() & 15) && lz(v) == (b.getZ() & 15) && y(v) == b.getY();
    }

    private List<Long> ler(Chunk c) {
        long[] a = c.getPersistentDataContainer().get(chave, PersistentDataType.LONG_ARRAY);
        List<Long> l = new ArrayList<>();
        if (a != null) for (long v : a) l.add(v);
        return l;
    }

    private void gravar(Chunk c, List<Long> l) {
        PersistentDataContainer pdc = c.getPersistentDataContainer();
        if (l.isEmpty()) {
            pdc.remove(chave);
            return;
        }
        long[] a = new long[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        pdc.set(chave, PersistentDataType.LONG_ARRAY, a);
    }

    private void anotar(Block b, int solo) {
        Chunk c = b.getChunk();
        List<Long> l = ler(c);
        l.removeIf(v -> mesmoBloco(v, b));
        l.add(empacotar(b, solo, dia(b.getWorld())));
        gravar(c, l);
    }

    /** Alguém passou numa trilha: o dia dela recomeça (uma vez por dia). */
    private void renovar(Block b, Pos pos) {
        long hoje = dia(b.getWorld());
        if (hoje != diaRenovadas) {
            renovadas.clear();
            diaRenovadas = hoje;
        }
        if (!renovadas.add(pos)) return;
        Chunk c = b.getChunk();
        if (!c.getPersistentDataContainer().has(chave)) return;
        List<Long> l = ler(c);
        boolean mudou = false;
        for (int i = 0; i < l.size(); i++) {
            long v = l.get(i);
            if (mesmoBloco(v, b) && diaDe(v) != hoje) {
                l.set(i, empacotar(b, solo(v), hoje));
                mudou = true;
            }
        }
        if (mudou) gravar(c, l);
    }

    /** As trilhas esquecidas do chunk voltam a ser o que eram; as que alguém mudou saem da lista. */
    int envelhecer(Chunk c) {
        return envelhecer(c, false);
    }

    /** {@code todas}: volta todas agora, sem olhar o dia (o admin testando). */
    public int envelhecer(Chunk c, boolean todas) {
        if (!c.getPersistentDataContainer().has(chave)) return 0;
        long hoje = dia(c.getWorld());
        int dias = plugin.settings().estradaDiasSumir;
        List<Long> l = ler(c);
        int voltaram = 0;
        for (Iterator<Long> it = l.iterator(); it.hasNext(); ) {
            long v = it.next();
            Block b = c.getBlock(lx(v), y(v), lz(v));
            if (b.getType() != Material.DIRT_PATH) {
                it.remove();
                continue;
            }
            if (todas || (dias > 0 && hoje - diaDe(v) > dias)) {
                int s = solo(v);
                b.setType(s < SOLOS.length ? SOLOS[s] : Material.GRASS_BLOCK, false);
                it.remove();
                voltaram++;
            }
        }
        gravar(c, l);
        return voltaram;
    }

    @EventHandler
    public void aoCarregar(ChunkLoadEvent e) {
        if (!e.isNewChunk()) envelhecer(e.getChunk());
    }

    /** A cada 10 minutos: os pedaços que nunca descarregam (perto da base) também envelhecem. */
    public void envelhecerCarregados() {
        for (World w : plugin.getServer().getWorlds()) for (Chunk c : w.getLoadedChunks()) envelhecer(c);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ultimo.remove(e.getPlayer().getUniqueId());
    }

    /** Quantas trilhas anotadas tem neste chunk (para o admin). */
    public int anotadas(Chunk c) {
        return ler(c).size();
    }
}
