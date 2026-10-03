package br.rpgatributos.oculto;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Um Local Oculto: onde fica, se já foi construído, e o andamento (enigma, guardião, selo).
 * As cores das alavancas e a ordem certa saem do id (sempre as mesmas para o mesmo local).
 */
final class Local {

    /** Cores possíveis das alavancas e o que cada uma significa no enigma. */
    static final Material[] CORES = {Material.RED_WOOL, Material.BLUE_WOOL, Material.YELLOW_WOOL, Material.GREEN_WOOL,
            Material.WHITE_WOOL, Material.BLACK_WOOL};
    static final String[] FRASES = {"o sangue derramado", "o mar profundo", "o sol nascente", "a floresta antiga",
            "a neve eterna", "a noite sem lua"};

    final String id;
    final TipoLocal tipo;
    final String mundo;
    final int x, z;
    int y = Integer.MIN_VALUE;
    boolean construido;

    // Cores das 4 alavancas (da esquerda para a direita) e a ordem certa (índices das alavancas).
    final int[] cores = new int[4];
    final int[] ordem = new int[4];

    // Andamento (não é salvo: volta ao começo se o servidor reiniciar).
    boolean agendado;
    int progresso;
    boolean aberto;
    UUID guardiao;
    BossBar barra;
    long vencidoAte;

    Local(String id, TipoLocal tipo, String mundo, int x, int z) {
        this.id = id;
        this.tipo = tipo;
        this.mundo = mundo;
        this.x = x;
        this.z = z;
        Random r = new Random(id.hashCode() * 31L + x * 7L + z);
        List<Integer> todas = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collections.shuffle(todas, r);
        for (int i = 0; i < 4; i++) cores[i] = todas.get(i);
        List<Integer> seq = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(seq, r);
        for (int i = 0; i < 4; i++) ordem[i] = seq.get(i);
    }

    World world() { return Bukkit.getWorld(mundo); }

    /** Chão da antessala (onde as pessoas pisam). */
    int yAntessala() { return y - 14; }

    /** Posição de cada alavanca (na parede norte da antessala). */
    Location alavanca(int i) {
        return new Location(world(), x - 3 + i * 2, yAntessala() + 1, z - 5);
    }

    Location selo() {
        return new Location(world(), x - 19, yAntessala(), z);
    }

    Location centroSalao() {
        return new Location(world(), x - 14 + 0.5, yAntessala(), z + 0.5);
    }

    /** Está dentro do salão do guardião? */
    boolean noSalao(Location l) {
        int ya = yAntessala();
        return l.getWorld() != null && l.getWorld().getName().equals(mundo) && l.getX() >= x - 21 && l.getX() < x - 7
                && l.getZ() >= z - 7 && l.getZ() < z + 8 && l.getY() >= ya - 1 && l.getY() <= ya + 7;
    }

    /** Está dentro da construção (superfície, poço, antessala e salão)? Usado para proteger os blocos. */
    boolean dentro(Location l) {
        int ya = yAntessala();
        return l.getWorld() != null && l.getWorld().getName().equals(mundo) && l.getX() >= x - 23 && l.getX() < x + 8
                && l.getZ() >= z - 9 && l.getZ() < z + 9 && l.getY() >= ya - 2 && l.getY() <= y + 5;
    }

    /** A frase do enigma com a ordem certa das cores. */
    String enigma() {
        String[] f = new String[4];
        for (int i = 0; i < 4; i++) f[i] = FRASES[cores[ordem[i]]];
        return "Primeiro " + f[0] + ", depois " + f[1] + ", então " + f[2] + " e, por fim, " + f[3] + ".";
    }

    boolean vencido() {
        return vencidoAte > System.currentTimeMillis();
    }
}
