package br.rpgatributos.estruturas;

import br.rpgatributos.fe.Deus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Uma estrutura já construída no mundo (o que fica salvo em estruturas.yml). */
final class Sitio {

    final String id;
    final Estrutura tipo;
    final String mundo;
    final int x, y, z, rot;
    /** Mina: profundidade do túnel; Santuário: o deus. */
    int fundo;
    Deus deus;
    /** Os guardas já apareceram (só uma vez). */
    boolean guardas;
    /** Cemitério e bruxa: última vez que a noite trouxe monstros. Círculo: dia da última lua cheia com o guardião. */
    long ronda;
    /** O morador (eremita, nômade ou bruxa) e o dia em que ele renovou as ofertas. */
    UUID eremita;
    long diaOfertas = -1;
    /** Farol: até quando a lanterna fica acesa (0 = apagada). */
    long acesoAte;
    /** Vila Saqueada: de onde vieram os atacantes (só na hora de construir). */
    String pista;
    final Set<UUID> visitantes = new HashSet<>();

    Sitio(String id, Estrutura tipo, String mundo, int x, int y, int z, int rot) {
        this.id = id;
        this.tipo = tipo;
        this.mundo = mundo;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rot = rot;
    }

    World world() {
        return Bukkit.getWorld(mundo);
    }

    /** Um bloco em coordenadas locais da estrutura (a mesma conta da {@link Obra}). */
    Block bloco(World w, int dx, int dy, int dz) {
        return new Obra(w, x, y, z, rot, null).bloco(dx, dy, dz);
    }

    /** O centro de um bloco local, pronto para nascer criatura. */
    Location ponto(World w, int dx, int dy, int dz) {
        return bloco(w, dx, dy, dz).getLocation().add(0.5, 0, 0.5);
    }

    double distancia2(Location l) {
        double dx = l.getX() - x - 0.5, dz = l.getZ() - z - 0.5;
        return dx * dx + dz * dz;
    }
}
