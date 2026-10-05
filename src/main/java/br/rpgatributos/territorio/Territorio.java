package br.rpgatributos.territorio;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Um território: um dono, um Marco (e até alguns Marcos de Expansão), os chunks protegidos, membros e regras. */
public final class Territorio {

    private final UUID dono;
    private String nomeDono;
    private final String mundo;
    private int marcoX, marcoY, marcoZ;
    private ItemStack bandeira;
    private final Set<Long> chunks = new HashSet<>();
    private final Map<UUID, String> membros = new LinkedHashMap<>();
    private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    private final List<int[]> expansoes = new ArrayList<>();

    Territorio(UUID dono, String nomeDono, Location marco, ItemStack bandeira) {
        this.dono = dono;
        this.nomeDono = nomeDono;
        this.mundo = marco.getWorld().getName();
        moverMarco(marco);
        this.bandeira = bandeira;
        for (Flag f : Flag.values()) if (f.padrao()) flags.add(f);
    }

    Territorio(UUID dono, String nomeDono, String mundo, int x, int y, int z, ItemStack bandeira) {
        this.dono = dono;
        this.nomeDono = nomeDono;
        this.mundo = mundo;
        this.marcoX = x;
        this.marcoY = y;
        this.marcoZ = z;
        this.bandeira = bandeira;
    }

    /** Chave de um chunk (x, z) num número só. */
    public static long chave(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    public static int chunkX(long chave) { return (int) (chave >> 32); }
    public static int chunkZ(long chave) { return (int) chave; }

    public UUID dono() { return dono; }
    public String nomeDono() { return nomeDono; }
    void nomeDono(String n) { nomeDono = n; }
    public String mundo() { return mundo; }
    public World world() { return Bukkit.getWorld(mundo); }

    /** Bloco do Marco (null se o mundo não está carregado). */
    public Location marco() {
        World w = world();
        return w == null ? null : new Location(w, marcoX, marcoY, marcoZ);
    }

    int marcoX() { return marcoX; }
    int marcoY() { return marcoY; }
    int marcoZ() { return marcoZ; }

    void moverMarco(Location l) {
        marcoX = l.getBlockX();
        marcoY = l.getBlockY();
        marcoZ = l.getBlockZ();
    }

    public long chunkDoMarco() { return chave(marcoX >> 4, marcoZ >> 4); }

    /** Marcos de Expansão (x, y, z): outras áreas do mesmo território, em outro lugar. */
    public List<int[]> expansoes() { return Collections.unmodifiableList(expansoes); }
    List<int[]> expansoesEditaveis() { return expansoes; }

    /** É o Marco principal ou o de uma expansão? */
    public boolean ehMarco(int x, int y, int z) {
        if (x == marcoX && y == marcoY && z == marcoZ) return true;
        return expansao(x, y, z) >= 0;
    }

    /** Índice da expansão com o Marco nesse bloco, ou -1. */
    public int expansao(int x, int y, int z) {
        for (int i = 0; i < expansoes.size(); i++) {
            int[] e = expansoes.get(i);
            if (e[0] == x && e[1] == y && e[2] == z) return i;
        }
        return -1;
    }

    /** O chunk tem um Marco (principal ou de expansão)? */
    public boolean chunkComMarco(long k) {
        if (k == chunkDoMarco()) return true;
        for (int[] e : expansoes) if (chave(e[0] >> 4, e[2] >> 4) == k) return true;
        return false;
    }

    public ItemStack bandeira() {
        return bandeira == null ? new ItemStack(Material.WHITE_BANNER) : bandeira.clone();
    }

    void bandeira(ItemStack b) { bandeira = b; }

    public Set<Long> chunks() { return Collections.unmodifiableSet(chunks); }
    Set<Long> chunksEditaveis() { return chunks; }

    public Map<UUID, String> membros() { return Collections.unmodifiableMap(membros); }
    Map<UUID, String> membrosEditaveis() { return membros; }

    public boolean membro(UUID id) { return membros.containsKey(id); }

    public boolean flag(Flag f) { return flags.contains(f); }

    void flag(Flag f, boolean ligada) {
        if (ligada) flags.add(f);
        else flags.remove(f);
    }

    Set<Flag> flagsLigadas() { return flags; }
}
