package br.rpgatributos.colonia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma construção da colônia: primeiro um canteiro (o projeto no chão, cercado), depois a obra
 * que o Construtor ergue bloco a bloco com o material do baú da obra, e no fim a construção pronta.
 * O ponto (x, y, z) é o centro da planta no chão (camada 0).
 */
final class Obra {

    enum Fase { CANTEIRO, LIMPANDO, ERGUENDO, PRONTA }

    final UUID id;
    final String planta;
    final String mundo;
    int x, y, z, rot;
    Fase fase = Fase.CANTEIRO;
    /** Próximo bloco da fase atual. */
    int passo;
    /** O Construtor (entidade) que está nela. */
    UUID construtor;
    /** Do jeito do jogador: ele mesmo constrói dentro da cerca e pede a vistoria (sem Construtor). */
    boolean livre;
    /** Blocos a mais de cada lado da área do projeto (só nas livres). */
    int extra;
    /** Blocos da cerca do canteiro (x, y, z), para tirar no fim. */
    final List<int[]> cerca = new ArrayList<>();

    // ---------- não é salvo ----------
    /** Baú da obra achado por último (dentro da cerca, fora da planta). */
    Location bau;
    /** O que faltou por último (null = nada). */
    org.bukkit.Material falta;
    int faltaQtd;
    long ultimoAvisoMs;
    /** Quantas vezes seguidas o Construtor estava longe. */
    int longe;

    Obra(UUID id, String planta, String mundo, int x, int y, int z, int rot) {
        this.id = id;
        this.planta = planta;
        this.mundo = mundo;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rot = rot;
    }

    World world() { return Bukkit.getWorld(mundo); }

    boolean comecou() { return !livre && (fase != Fase.CANTEIRO || passo > 0); }

    /** Bloco do mundo de um ponto da planta. */
    Block bloco(World w, Planta p, int lx, int ly, int lz) {
        int[] g = Planta.girar(lx - p.largura() / 2, lz - p.profundidade() / 2, rot);
        return w.getBlockAt(x + g[0], y + ly, z + g[1]);
    }

    /** Retângulo (minX, minZ, maxX, maxZ) da planta no mundo, com {@code folga} blocos a mais de cada lado. */
    int[] area(Planta p, int folga) {
        folga += extra;
        int[] a = Planta.girar(-p.largura() / 2, -p.profundidade() / 2, rot);
        int[] b = Planta.girar(p.largura() - 1 - p.largura() / 2, p.profundidade() - 1 - p.profundidade() / 2, rot);
        return new int[]{x + Math.min(a[0], b[0]) - folga, z + Math.min(a[1], b[1]) - folga,
                x + Math.max(a[0], b[0]) + folga, z + Math.max(a[1], b[1]) + folga};
    }

    /** O ponto bem na frente da entrada, do lado de dentro da cerca (onde o Construtor fica). */
    Location entrada(World w, Planta p) {
        Block b = bloco(w, p, p.largura() / 2, 0, p.profundidade() + extra);
        return b.getLocation().add(0.5, 1, 0.5);
    }

    /** Onde fica o portão da cerca (no meio da frente). */
    Block portao(World w, Planta p) {
        return bloco(w, p, p.largura() / 2, 0, p.profundidade() + 1 + extra);
    }

    Location centro(World w) { return new Location(w, x + 0.5, y + 1, z + 0.5); }

    boolean dentro(Planta p, int bx, int bz, int folga) {
        int[] a = area(p, folga);
        return bx >= a[0] && bx <= a[2] && bz >= a[1] && bz <= a[3];
    }

    // ---------- arquivo ----------

    void salvar(ConfigurationSection s) {
        s.set("planta", planta);
        s.set("mundo", mundo);
        s.set("lugar", x + "," + y + "," + z + "," + rot);
        s.set("fase", fase.name());
        s.set("passo", passo);
        if (construtor != null) s.set("construtor", construtor.toString());
        if (livre) s.set("livre", true);
        if (extra > 0) s.set("extra", extra);
        List<String> l = new ArrayList<>();
        for (int[] c : cerca) l.add(c[0] + "," + c[1] + "," + c[2]);
        s.set("cerca", l);
    }

    static Obra ler(String chave, ConfigurationSection s) {
        try {
            String[] p = s.getString("lugar", "").split(",");
            Obra o = new Obra(UUID.fromString(chave), s.getString("planta", "?"), s.getString("mundo", "world"),
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            o.fase = Fase.valueOf(s.getString("fase", "CANTEIRO"));
            o.passo = Math.max(0, s.getInt("passo"));
            o.livre = s.getBoolean("livre");
            o.extra = Math.max(0, s.getInt("extra"));
            String c = s.getString("construtor");
            if (c != null) o.construtor = UUID.fromString(c);
            for (String linha : s.getStringList("cerca")) {
                String[] q = linha.split(",");
                o.cerca.add(new int[]{Integer.parseInt(q[0]), Integer.parseInt(q[1]), Integer.parseInt(q[2])});
            }
            return o;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
