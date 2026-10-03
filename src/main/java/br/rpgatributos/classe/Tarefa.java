package br.rpgatributos.classe;

/** Uma tarefa do caminho de classe: fazer algo {@code qtd} vezes depois de aceitar o caminho. */
public record Tarefa(Tipo tipo, int qtd) {

    /**
     * O que conta. Quase tudo vem dos contadores de conquistas (títulos); a corrida
     * vem da estatística do próprio jogo.
     */
    public enum Tipo {
        ABATES_CORPO("abates_corpo", "Derrote %d monstros corpo a corpo"),
        ABATES_FLECHA("abates_flecha", "Derrote %d monstros com flechas"),
        MAGIAS("magias", "Lance %d magias do grimório"),
        FORJAR("forjados", "Forje %d itens na Forja do Ferreiro"),
        CHEFES("chefes", "Derrote %d chefes"),
        MASMORRAS("masmorras", "Vença %d masmorras"),
        COMPANHEIROS("companheiros", "Vincule %d companheiros"),
        ABATES_COMPANHEIRO("abates_companheiro", "Seus companheiros derrotam %d monstros"),
        CORRER(null, "Corra %d blocos");

        private final String contador;
        private final String texto;

        Tipo(String contador, String texto) {
            this.contador = contador;
            this.texto = texto;
        }

        /** Contador de conquistas que mede essa tarefa (null = corrida). */
        public String contador() { return contador; }
        public String texto(int qtd) { return String.format(texto, qtd); }
    }

    static Tarefa t(Tipo tipo, int qtd) {
        return new Tarefa(tipo, qtd);
    }
}
