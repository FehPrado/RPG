package br.rpgatributos.territorio;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Um território: um dono, um Marco, os chunks protegidos, membros e regras. */
public final class Territorio {

    private final UUID dono;
    private String nomeDono;
    private final String mundo;
    private int marcoX, marcoY, marcoZ;
    private ItemStack bandeira;
    private final Set<Long> chunks = new HashSet<>();
    private final Map<UUID, String> membros = new LinkedHashMap<>();
    private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

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
