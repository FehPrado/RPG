package br.rpgatributos.masmorra;

import java.util.*;

/** Regras de encontros e sequências opcionais, independentes das entidades do servidor. */
final class ConteudoSalas {
    enum Encontro {
        PATRULHA("Patrulha", "Vença as ondas de guardas."),
        HORDA("Horda", "Mais inimigos, com menos vida e dano."),
        SENTINELA("Sentinela", "Um guardião fortalecido lidera um grupo menor.");
        final String nome, dica;
        Encontro(String nome, String dica) { this.nome = nome; this.dica = dica; }
        int quantidade(int base) { return Math.min(14, Math.max(1, switch (this) {
            case PATRULHA -> base; case HORDA -> base + 3; case SENTINELA -> (base + 1) / 2;
        })); }
        double vida(int indice) { return this == HORDA ? .75 : this == SENTINELA && indice == 0 ? 2 : 1; }
        double dano(int indice) { return this == HORDA ? .85 : this == SENTINELA && indice == 0 ? 1.15 : 1; }
    }
    static final class Sequencia {
        private final List<Integer> ordem;
        private int progresso;
        private boolean concluida;
        Sequencia(Random r) { var o = new ArrayList<>(List.of(0,1,2)); Collections.shuffle(o,r); ordem = List.copyOf(o); }
        List<Integer> ordem() { return ordem; }
        int progresso() { return progresso; }
        boolean concluida() { return concluida; }
        boolean tocar(int runa) {
            if (concluida || runa < 0 || runa > 2) return false;
            if (ordem.get(progresso) != runa) { progresso = 0; return false; }
            progresso++; concluida = progresso == ordem.size(); return true;
        }
        String pista() { return String.join(" → ", ordem.stream().map(i -> List.of("I","II","III").get(i)).toList()); }
    }
    static String nome(Tema tema, Sala s) {
        String lugar = switch (s.tipo) {
            case INICIO -> "Vestíbulo"; case CHEFE -> "Trono"; case TESOURO -> "Tesouraria";
            case MINA -> "Veio de Cristais"; case ESCADA -> "Passagem Vertical"; case SECRETA -> "Refúgio Esquecido";
            case COMBATE -> switch (s.estilo) {
                case BIBLIOTECA -> "Arquivo"; case JARDIM -> "Jardim"; case PONTE -> "Travessia";
                case CRIPTA -> "Ossuário"; case ARENA -> "Arena"; case FORJA -> "Forja";
                case OBSERVATORIO -> "Observatório"; default -> "Galeria";
            };
        };
        return lugar + switch (tema) { case CRIPTA -> " das Almas"; case CAVERNA -> " das Profundezas"; case INFERNO -> " das Brasas"; case GELO -> " da Geada"; };
    }
}
