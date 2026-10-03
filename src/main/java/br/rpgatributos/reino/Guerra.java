package br.rpgatributos.reino;

import net.kyori.adventure.bossbar.BossBar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Uma guerra marcada entre dois reinos: começa e acaba no horário de guerra do servidor. */
public final class Guerra {

    final UUID atacante;
    final UUID defensor;
    final long inicio;
    final long fim;
    boolean comecou;
    int pontosAtacante;
    int pontosDefensor;
    /** Avisos de "falta X minutos" já mandados. */
    final Set<Integer> avisos = new HashSet<>();
    /** Dono do território → segundos de captura do Marco dele. */
    final Map<UUID, Integer> captura = new HashMap<>();
    /** Territórios já capturados nesta guerra (cada um só uma vez). */
    final Set<UUID> capturados = new HashSet<>();
    /** Barras de captura de cada Marco. */
    final transient Map<UUID, BossBar> barras = new HashMap<>();

    Guerra(UUID atacante, UUID defensor, long inicio, long fim) {
        this.atacante = atacante;
        this.defensor = defensor;
        this.inicio = inicio;
        this.fim = fim;
    }

    public UUID atacante() { return atacante; }
    public UUID defensor() { return defensor; }
    public long inicio() { return inicio; }
    public long fim() { return fim; }

    public boolean envolve(UUID reino) {
        return atacante.equals(reino) || defensor.equals(reino);
    }

    public UUID outro(UUID reino) {
        return atacante.equals(reino) ? defensor : atacante;
    }

    public boolean ativa(long agora) {
        return agora >= inicio && agora < fim;
    }

    void pontuar(UUID reino, int pontos) {
        if (atacante.equals(reino)) pontosAtacante += pontos;
        else if (defensor.equals(reino)) pontosDefensor += pontos;
    }

    int pontos(UUID reino) {
        return atacante.equals(reino) ? pontosAtacante : pontosDefensor;
    }
}
