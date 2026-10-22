package br.rpgatributos.masmorra;

import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.classe.Classe;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

/**
 * Os Monarcas (2.74; classes lendárias desde a 2.76). Os quatro primeiros são classes lendárias da
 * árvore, de um dono só: quem vence o Portal do Monarca herda a classe. O da Destruição é o chefe
 * final e não vira classe.
 */
public enum Monarca {
    CHAMAS_BRANCAS("Monarca das Chamas Brancas", "Baran, Monarca das Chamas Brancas", 0xE3F2FD, Chefe.CHAMAS, Material.BLAZE_ROD,
            new String[]{"Chifre de Baran", "Brasa Branca", "Corrente Demoníaca", "Olho do Relâmpago", "Coroa das Chamas"}),
    GEADA("Monarca da Geada", "Monarca da Geada", 0x80D8FF, Chefe.LICH, Material.BLUE_ICE,
            new String[]{"Lasca do Trono Gelado", "Lágrima Congelada", "Manto de Neve", "Coração de Inverno", "Coroa de Gelo"}),
    FERAS("Monarca das Feras", "Rakan, Monarca das Feras", 0xA1887F, Chefe.GOLEM, Material.BONE,
            new String[]{"Presa de Rakan", "Juba Antiga", "Garra Partida", "Uivo Petrificado", "Coroa das Feras"}),
    INSETOS("Monarca dos Insetos", "Querehsha, Monarca dos Insetos", 0x9CCC65, Chefe.RAINHA, Material.SPIDER_EYE,
            new String[]{"Ferrão de Querehsha", "Asa Translúcida", "Carapaça Real", "Ovo do Enxame", "Coroa dos Insetos"}),
    DESTRUICAO("Monarca da Destruição", "Antares, Monarca da Destruição", 0xD50000, Chefe.ARAUTO, Material.DRAGON_HEAD,
            new String[]{"Escama de Antares", "Dente do Dragão Rei", "Chama Negra", "Asa do Fim", "Coroa da Destruição"});

    public final String nome, chefe;
    public final TextColor cor;
    public final Chefe base;
    public final Material icone;
    /** As 5 relíquias deste Monarca (o mural tem uma linha de 5 para cada um). */
    public final String[] reliquias;

    Monarca(String nome, String chefe, int cor, Chefe base, Material icone, String[] reliquias) {
        this.nome = nome;
        this.chefe = chefe;
        this.cor = TextColor.color(cor);
        this.base = base;
        this.icone = icone;
        this.reliquias = reliquias;
    }

    /** A classe lendária que este Monarca dá (null para o da Destruição). */
    public Classe classe() {
        return switch (this) {
            case CHAMAS_BRANCAS -> Classe.MONARCA_CHAMAS_BRANCAS;
            case GEADA -> Classe.MONARCA_GEADA;
            case FERAS -> Classe.MONARCA_FERAS;
            case INSETOS -> Classe.MONARCA_INSETOS;
            case DESTRUICAO -> null;
        };
    }

    /** Vira classe (os 4 primeiros); o da Destruição é só o chefe final. */
    public boolean jogavel() {
        return classe() != null;
    }
}
