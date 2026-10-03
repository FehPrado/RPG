package br.rpgatributos.masmorra;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uma masmorra em andamento: as salas, quem está nela e o estado. */
public final class Masmorra {

    public enum Estado { GERANDO, PRONTA, CONCLUIDA }

    final UUID id = UUID.randomUUID();
    final int vaga;
    final Dificuldade dificuldade;
    final Tema tema;
    final World mundo;
    final UUID dono;
    final List<Sala> salas = new ArrayList<>();
    Sala inicio, chefe;

    /** Quem está jogando (entrou e ainda não saiu nem morreu). */
    final Set<UUID> participantes = new HashSet<>();
    /** Quem já saiu ou morreu: não entra de novo. */
    final Set<UUID> sairam = new HashSet<>();

    Estado estado = Estado.GERANDO;
    final long criadaEm = System.currentTimeMillis();
    long limite;
    long ultimaPresenca = System.currentTimeMillis();
    long concluidaEm;
    /** Portal de saída (aparece no fim, na sala do chefe). */
    Location saida;
    int minX, minZ, maxX, maxZ;

    Masmorra(int vaga, Dificuldade dificuldade, Tema tema, World mundo, UUID dono) {
        this.vaga = vaga;
        this.dificuldade = dificuldade;
        this.tema = tema;
        this.mundo = mundo;
        this.dono = dono;
    }

    /** Está na área dessa masmorra (todas as salas e corredores)? */
    boolean contem(Location l) {
        return l.getWorld() == mundo && l.getX() >= minX - 4 && l.getX() <= maxX + 4
                && l.getZ() >= minZ - 4 && l.getZ() <= maxZ + 4;
    }

    Sala salaEm(Location l) {
        for (Sala s : salas) if (s.dentro(l, -0.5)) return s;
        return null;
    }

    int salasLimpas() {
        int n = 0;
        for (Sala s : salas) if (s.estado == Sala.Estado.LIMPA && (s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.CHEFE)) n++;
        return n;
    }

    int salasDeLuta() {
        int n = 0;
        for (Sala s : salas) if (s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.CHEFE) n++;
        return n;
    }

    public Dificuldade dificuldade() { return dificuldade; }
    public Tema tema() { return tema; }
    public Estado estado() { return estado; }
}
