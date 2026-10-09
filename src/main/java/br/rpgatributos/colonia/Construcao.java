package br.rpgatributos.colonia;

/** Uma construção pronta da colônia (o projeto e onde ficou). */
record Construcao(String planta, String mundo, int x, int y, int z, int rot) {

    String salvar() {
        return planta + ";" + mundo + ";" + x + ";" + y + ";" + z + ";" + rot;
    }

    static Construcao ler(String s) {
        String[] p = s.split(";");
        if (p.length < 6) return null;
        try {
            return new Construcao(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
