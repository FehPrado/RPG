package br.rpgatributos.masmorra;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uma sala da masmorra: posição na grade, limites no mundo, portas e o andamento da luta. */
public final class Sala {

    public enum Estado { AGUARDANDO, LUTANDO, LIMPA }

    /** Posição na grade de salas. */
    final int gx, gz;
    TipoSala tipo;
    /** Distância (em salas) até a entrada. */
    final int profundidade;
    /** Portas abertas para as salas vizinhas. */
    final EnumSet<BlockFace> portas = EnumSet.noneOf(BlockFace.class);

    // Limites no mundo (com as paredes).
    int minX, minY, minZ, maxX, maxY, maxZ;
    int centroX, centroZ;
    /** Se veio de uma sala construída por um admin, o nome dela. */
    String personalizada;

    // Marcas (placas nas salas personalizadas) ou pontos escolhidos pelo gerador.
    final List<Location> pontosMonstro = new ArrayList<>();
    final List<Location> pontosBau = new ArrayList<>();
    Location pontoChefe;
    Location entrada;

    Estado estado = Estado.AGUARDANDO;
    final Set<UUID> mobs = new HashSet<>();
    int onda;
    long proximaOnda;
    long semJogadoresDesde;
    UUID chefe;

    Sala(int gx, int gz, TipoSala tipo, int profundidade) {
        this.gx = gx;
        this.gz = gz;
        this.tipo = tipo;
        this.profundidade = profundidade;
    }

    /** Define os limites a partir do centro da célula e da altura do piso. */
    void posicionar(int cx, int y, int cz) {
        int meio = (tipo.tamanho() - 1) / 2;
        centroX = cx;
        centroZ = cz;
        minX = cx - meio;
        maxX = cx + meio;
        minZ = cz - meio;
        maxZ = cz + meio;
        minY = y;
        maxY = y + tipo.altura() - 1;
    }

    /** Está dentro da sala, a pelo menos {@code margem} blocos das paredes? */
    boolean dentro(Location l, double margem) {
        return l.getX() >= minX + 1 + margem && l.getX() < maxX - margem
                && l.getZ() >= minZ + 1 + margem && l.getZ() < maxZ - margem
                && l.getY() >= minY && l.getY() <= maxY;
    }

    /** Ponto central do chão (onde as pessoas ficam em pé). */
    Location centro(World w) {
        return new Location(w, centroX + 0.5, minY + 1, centroZ + 0.5);
    }

    public TipoSala tipo() { return tipo; }
    public Estado estado() { return estado; }
}
