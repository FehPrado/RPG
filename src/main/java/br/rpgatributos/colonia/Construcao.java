package br.rpgatributos.colonia;

import java.util.UUID;

/**
 * Uma construção pronta da colônia: o projeto, onde ficou, se foi feita pelo jogador do jeito
 * dele ("livre") e o nível que a última vistoria deu.
 */
final class Construcao {

    final String planta, mundo;
    final int x, y, z, rot;
    /** Blocos a mais de cada lado (área maior que a do projeto, só nas livres). */
    final int extra;
    final boolean livre;
    int nivel;
    /** A placa da construção (x, y, z), como o bloco do prédio no MineColonies: clique abre o menu, quebrar desfaz. */
    int[] placa;
    /** Camas que a última vistoria achou (-1 = ainda não sabe). Numa Casa, é quantos moradores cabem. */
    int camas = -1;

    Construcao(String planta, String mundo, int x, int y, int z, int rot, int extra, boolean livre, int nivel) {
        this.planta = planta;
        this.mundo = mundo;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rot = rot;
        this.extra = extra;
        this.livre = livre;
        this.nivel = nivel;
    }

    String planta() { return planta; }

    /** Identifica a construção (o morador guarda qual é a casa dele). */
    String chave() { return mundo + "@" + x + "," + y + "," + z; }
    String mundo() { return mundo; }

    /** A mesma área como uma obra (para contas de lugar, contorno e vistoria). */
    Obra area() {
        Obra o = new Obra(UUID.randomUUID(), planta, mundo, x, y, z, rot);
        o.extra = extra;
        o.livre = livre;
        return o;
    }

    String salvar() {
        return planta + ";" + mundo + ";" + x + ";" + y + ";" + z + ";" + rot + ";" + extra + ";" + livre + ";" + nivel + ";"
                + (placa == null ? "-" : placa[0] + "," + placa[1] + "," + placa[2]) + ";" + camas;
    }

    static Construcao ler(String s) {
        String[] p = s.split(";");
        if (p.length < 6) return null;
        try {
            int extra = p.length > 6 ? Integer.parseInt(p[6]) : 0;
            boolean livre = p.length > 7 && Boolean.parseBoolean(p[7]);
            int nivel = p.length > 8 ? Integer.parseInt(p[8]) : 1;
            Construcao k = new Construcao(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                    extra, livre, nivel);
            if (p.length > 9 && !p[9].equals("-")) {
                String[] q = p[9].split(",");
                k.placa = new int[]{Integer.parseInt(q[0]), Integer.parseInt(q[1]), Integer.parseInt(q[2])};
            }
            if (p.length > 10) k.camas = Integer.parseInt(p[10]);
            return k;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
