package br.rpgatributos.masmorra;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Uma sala da masmorra: o lugar na grade de chunks, os limites no mundo, as portas e o andamento
 * da luta. Desde a 2.69 a sala tem tamanho variável (2x2 chunks; às vezes 3x3, 3x2 ou 2x3), um
 * andar, e as portas ficam onde o corredor chega (não mais sempre no meio da parede).
 */
public final class Sala {

    public enum Estado { AGUARDANDO, LUTANDO, LIMPA }

    /** Uma porta: a parede, a coordenada ao longo dela (o meio dos 3 blocos) e a altura do piso. */
    public record Porta(BlockFace face, int linha, int y) { }

    /** Canto da sala na grade de chunks (relativo à masmorra) e o tamanho em chunks. */
    final int gx, gz;
    int cw = 2, ch = 2;
    /** O andar (0 = o da entrada). A Escadaria ocupa este andar e o de cima. */
    int nivel;
    TipoSala tipo;
    /** Distância (em salas) até a entrada. */
    int profundidade;
    final List<Porta> portas = new ArrayList<>();
    /** Estilo do interior (o gerador decide). */
    Gerador.Estilo estilo = Gerador.Estilo.SIMPLES;
    /** 2.70: tem um Pilar de Bênção num canto. */
    boolean pilar;
    ConteudoSalas.Encontro encontro = ConteudoSalas.Encontro.PATRULHA;
    final Set<UUID> visitantes = new HashSet<>();

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
    /** 2.70: a fase do chefe (0, 1 a partir de 66% de vida, 2 a partir de 33%) e o próximo golpe marcado. */
    int fase;
    long proximoGolpe;

    Sala(int gx, int gz, TipoSala tipo, int profundidade) {
        this.gx = gx;
        this.gz = gz;
        this.tipo = tipo;
        this.profundidade = profundidade;
    }

    /** Ocupa a célula (x, z) da grade de chunks? */
    boolean ocupa(int x, int z) {
        return x >= gx && x < gx + cw && z >= gz && z < gz + ch;
    }

    /** Sala grande (mais de 2x2 chunks)? */
    boolean grande() {
        return cw * ch > 4;
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
