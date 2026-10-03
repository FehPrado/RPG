package br.rpgatributos.forja;

import br.rpgatributos.StatsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Bônus de conjunto: vestindo as 4 peças de armadura forjadas, a raridade mais
 * baixa entre elas decide o bônus extra.
 */
public final class Conjuntos {

    /** vida em pontos (2 = 1 coração); velocidade em fração (0,05 = 5%). */
    public record Bonus(double armadura, double vida, double velocidade, double resistencia,
                        boolean regeneracao, boolean resistenciaPocao) {}

    private Conjuntos() {}

    public static Bonus de(Raridade r) {
        return switch (r) {
            case COMUM -> new Bonus(1, 0, 0, 0, false, false);
            case RARO -> new Bonus(2, 2, 0, 0, false, false);
            case EPICO -> new Bonus(2, 4, 0.05, 0, false, false);
            case UNICO -> new Bonus(3, 4, 0.05, 1, false, false);
            case LENDARIO -> new Bonus(4, 6, 0.08, 2, true, false);
            case MITICO -> new Bonus(5, 8, 0.10, 3, true, true);
        };
    }

    /** "+2 armadura, +2 ❤, +5% velocidade" */
    public static String texto(Raridade r) {
        Bonus b = de(r);
        List<String> partes = new ArrayList<>();
        if (b.armadura() > 0) partes.add("+" + StatsManager.fmt(b.armadura()) + " armadura");
        if (b.vida() > 0) partes.add("+" + StatsManager.fmt(b.vida() / 2) + " ❤");
        if (b.velocidade() > 0) partes.add("+" + StatsManager.fmt(b.velocidade() * 100) + "% velocidade");
        if (b.resistencia() > 0) partes.add("+" + StatsManager.fmt(b.resistencia()) + " resistência");
        if (b.regeneracao()) partes.add("Regeneração I");
        if (b.resistenciaPocao()) partes.add("Resistência I");
        return String.join(", ", partes);
    }
}
