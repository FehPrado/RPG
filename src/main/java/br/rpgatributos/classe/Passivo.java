package br.rpgatributos.classe;

/**
 * Bônus passivo de uma classe, escrito como "quanto vale no nível 20 da classe".
 * No nível 1 vale metade; cresce até o valor cheio.
 */
public final class Passivo {

    double vida, armadura, velocidade, danoCorpo, danoFlecha, mana, regen, custoMagia, esquiva;
    /** Chance de a forja sair uma raridade acima. */
    double raridade;
    /** Chance extra no refino. */
    double refino;
    int companheiros, componentes;
    double danoCompanheiro;
    String texto = "";

    static Passivo p() { return new Passivo(); }

    Passivo vida(double v) { vida = v; return this; }
    Passivo armadura(double v) { armadura = v; return this; }
    Passivo velocidade(double v) { velocidade = v; return this; }
    Passivo corpo(double v) { danoCorpo = v; return this; }
    Passivo flecha(double v) { danoFlecha = v; return this; }
    Passivo mana(double v) { mana = v; return this; }
    Passivo regen(double v) { regen = v; return this; }
    Passivo custoMagia(double v) { custoMagia = v; return this; }
    Passivo esquiva(double v) { esquiva = v; return this; }
    Passivo raridade(double v) { raridade = v; return this; }
    Passivo refino(double v) { refino = v; return this; }
    Passivo companheiros(int v) { companheiros = v; return this; }
    Passivo componentes(int v) { componentes = v; return this; }
    Passivo danoCompanheiro(double v) { danoCompanheiro = v; return this; }
    Passivo texto(String t) { texto = t; return this; }

    public String texto() { return texto; }

    /** Quanto do valor cheio vale no nível de classe {@code n} (metade no 1, tudo no 20). */
    static double fator(int n) {
        return 0.5 + 0.5 * Math.max(0, Math.min(20, n)) / 20.0;
    }
}
