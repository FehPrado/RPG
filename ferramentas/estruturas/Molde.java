import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Um molde de estrutura (o .nbt que o jogo usa). Células nulas não são gravadas: ali o terreno fica
 * como está. "minecraft:air" limpa o terreno. Coordenadas: x leste, y cima, z sul.
 */
public class Molde {
    final int sx, sy, sz;
    final String[] b;
    final Map<Integer, String> nbt = new HashMap<>();
    final Random r;

    Molde(int sx, int sy, int sz, long semente) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        b = new String[sx * sy * sz];
        r = new Random(semente);
    }

    int idx(int x, int y, int z) { return (y * sz + z) * sx + x; }

    boolean dentro(int x, int y, int z) { return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz; }

    String get(int x, int y, int z) { return dentro(x, y, z) ? b[idx(x, y, z)] : null; }

    void set(int x, int y, int z, String s) {
        if (!dentro(x, y, z)) return;
        int i = idx(x, y, z);
        b[i] = s == null ? null : s.contains(":") ? s : "minecraft:" + s;
        nbt.remove(i);
    }

    /** Só põe se a célula estiver vazia (nula ou ar). */
    void setSeVazio(int x, int y, int z, String s) {
        String a = get(x, y, z);
        if (a == null || a.equals("minecraft:air")) set(x, y, z, s);
    }

    void setNbt(int x, int y, int z, String s, String nbtSnbt) {
        set(x, y, z, s);
        if (dentro(x, y, z)) nbt.put(idx(x, y, z), nbtSnbt);
    }

    String pick(String... op) { return op[r.nextInt(op.length)]; }

    /** Pedra variada: a primeira opção sai mais. */
    String mix(double chanceBase, String base, String... outras) { return r.nextDouble() < chanceBase ? base : pick(outras); }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    interface Pal { String at(int x, int y, int z); }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, Pal p) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, p.at(x, y, z));
    }

    void cilindro(double cx, double cz, double raio, int y0, int y1, Pal p) {
        for (int x = (int) Math.floor(cx - raio); x <= Math.ceil(cx + raio); x++)
            for (int z = (int) Math.floor(cz - raio); z <= Math.ceil(cz + raio); z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= raio) for (int y = y0; y <= y1; y++) set(x, y, z, p.at(x, y, z));
            }
    }

    void anel(double cx, double cz, double rIn, double rOut, int y0, int y1, Pal p) {
        for (int x = (int) Math.floor(cx - rOut); x <= Math.ceil(cx + rOut); x++)
            for (int z = (int) Math.floor(cz - rOut); z <= Math.ceil(cz + rOut); z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= rOut && d > rIn) for (int y = y0; y <= y1; y++) set(x, y, z, p.at(x, y, z));
            }
    }

    /** Elipsoide cheio (com ruído na borda, 0 = liso). */
    void elipsoide(double cx, double cy, double cz, double rx, double ry, double rz, double ruido, Pal p) {
        for (int x = (int) Math.floor(cx - rx - 1); x <= Math.ceil(cx + rx + 1); x++)
            for (int y = (int) Math.floor(cy - ry - 1); y <= Math.ceil(cy + ry + 1); y++)
                for (int z = (int) Math.floor(cz - rz - 1); z <= Math.ceil(cz + rz + 1); z++) {
                    double d = sq((x - cx) / rx) + sq((y - cy) / ry) + sq((z - cz) / rz);
                    if (d <= 1 + (ruido > 0 ? (r.nextDouble() - 0.5) * ruido : 0)) set(x, y, z, p.at(x, y, z));
                }
    }

    static double sq(double v) { return v * v; }

    /** Uma "viga" grossa de (x0,y0,z0) a (x1,y1,z1). */
    void linha(double x0, double y0, double z0, double x1, double y1, double z1, double raio, Pal p) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = (int) Math.max(1, Math.ceil(len * 2));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            double cx = x0 + dx * t, cy = y0 + dy * t, cz = z0 + dz * t;
            if (raio < 0.6) { set((int) Math.round(cx), (int) Math.round(cy), (int) Math.round(cz), p.at((int) Math.round(cx), (int) Math.round(cy), (int) Math.round(cz))); continue; }
            for (int x = (int) Math.floor(cx - raio); x <= Math.ceil(cx + raio); x++)
                for (int y = (int) Math.floor(cy - raio); y <= Math.ceil(cy + raio); y++)
                    for (int z = (int) Math.floor(cz - raio); z <= Math.ceil(cz + raio); z++)
                        if (sq(x - cx) + sq(y - cy) + sq(z - cz) <= raio * raio) set(x, y, z, p.at(x, y, z));
        }
    }

    void bau(int x, int y, int z, String facing, String tabela) {
        setNbt(x, y, z, "chest[facing=" + facing + ",type=single,waterlogged=false]", "{id:\"minecraft:chest\",LootTable:\"" + tabela + "\"}");
    }

    void barril(int x, int y, int z, String tabela) {
        setNbt(x, y, z, "barrel[facing=up,open=false]", "{id:\"minecraft:barrel\",LootTable:\"" + tabela + "\"}");
    }

    /** Troca, nas células já postas, um bloco por outro com chance (musgo, rachaduras...). */
    void envelhecer(String de, double chance, String... para) {
        String chave = "minecraft:" + de;
        for (int i = 0; i < b.length; i++) if (chave.equals(b[i]) && r.nextDouble() < chance) b[i] = "minecraft:" + para[r.nextInt(para.length)];
    }

    /** Ruína: tira blocos do topo das paredes (de cima para baixo) em manchas. */
    void ruir(int y0, double chance, String... materiais) {
        Set<String> ms = new HashSet<>();
        for (String m : materiais) ms.add("minecraft:" + m);
        for (int x = 0; x < sx; x++)
            for (int z = 0; z < sz; z++) {
                if (r.nextDouble() >= chance) continue;
                int cortes = 1 + r.nextInt(4);
                for (int y = sy - 1; y >= y0 && cortes > 0; y--) {
                    String s = get(x, y, z);
                    if (s == null || s.equals("minecraft:air")) continue;
                    if (!ms.contains(s.replaceAll("\\[.*", ""))) break;
                    set(x, y, z, "air");
                    cortes--;
                }
            }
    }

    // =====================================================================
    //  Gravar
    // =====================================================================

    void gravar(File f) throws IOException {
        List<String> paleta = new ArrayList<>();
        Map<String, Integer> pi = new HashMap<>();
        List<int[]> blocos = new ArrayList<>();
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    String s = b[idx(x, y, z)];
                    if (s == null) continue;
                    Integer k = pi.get(s);
                    if (k == null) {
                        k = paleta.size();
                        paleta.add(s);
                        pi.put(s, k);
                    }
                    blocos.add(new int[]{x, y, z, k, idx(x, y, z)});
                }
        f.getParentFile().mkdirs();
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(f))))) {
            o.writeByte(10);
            o.writeUTF("");
            // size
            o.writeByte(9); o.writeUTF("size"); o.writeByte(3); o.writeInt(3); o.writeInt(sx); o.writeInt(sy); o.writeInt(sz);
            o.writeByte(9); o.writeUTF("entities"); o.writeByte(10); o.writeInt(0);
            o.writeByte(9); o.writeUTF("blocks"); o.writeByte(10); o.writeInt(blocos.size());
            for (int[] bl : blocos) {
                o.writeByte(9); o.writeUTF("pos"); o.writeByte(3); o.writeInt(3); o.writeInt(bl[0]); o.writeInt(bl[1]); o.writeInt(bl[2]);
                o.writeByte(3); o.writeUTF("state"); o.writeInt(bl[3]);
                String n = nbt.get(bl[4]);
                if (n != null) { o.writeByte(10); o.writeUTF("nbt"); Snbt.escrever(o, n); }
                o.writeByte(0);
            }
            o.writeByte(9); o.writeUTF("palette"); o.writeByte(10); o.writeInt(paleta.size());
            for (String s : paleta) {
                String nome = s, props = null;
                int a = s.indexOf('[');
                if (a >= 0) { nome = s.substring(0, a); props = s.substring(a + 1, s.length() - 1); }
                o.writeByte(8); o.writeUTF("id"); o.writeUTF(nome);
                if (props != null && !props.isEmpty()) {
                    o.writeByte(10); o.writeUTF("properties");
                    for (String kv : props.split(",")) {
                        String[] p = kv.split("=");
                        o.writeByte(8); o.writeUTF(p[0].trim()); o.writeUTF(p[1].trim());
                    }
                    o.writeByte(0);
                }
                o.writeByte(0);
            }
            o.writeByte(3); o.writeUTF("DataVersion"); o.writeInt(5023);
            o.writeByte(0);
        }
    }

    int contar() {
        int n = 0;
        for (String s : b) if (s != null) n++;
        return n;
    }
}
