package br.rpgatributos.arcano;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

import static br.rpgatributos.arcano.Forma.*;

/**
 * Modificadores: até 2 por magia, escolhidos no criador. Mudam como a forma se comporta
 * (dividir, ricochetear, perseguir, virar mina...). Cada um libera num nível de Arcano.
 */
public enum Modificador {
    RAPIDA("Rápida", Material.SUGAR, 0, 0.8, "Recarga pela metade, mas 30% mais fraca",
            EnumSet.complementOf(EnumSet.of(PROIBIDA))),
    AMPLIAR("Ampliar", Material.SPYGLASS, 0.1, 1.5, "Área e alcance 60% maiores",
            EnumSet.of(PROJETIL, AURA, SOPRO, CHUVA, CRIACAO, RAIO)),
    DIVIDIR("Dividir", Material.PRISMARINE_SHARD, 0.15, 1.5, "Lança 3 projéteis em leque (60% da força cada)",
            EnumSet.of(PROJETIL)),
    TELEGUIADO("Teleguiado", Material.SPECTRAL_ARROW, 0.2, 1.2, "O projétil persegue o inimigo mais perto da mira",
            EnumSet.of(PROJETIL)),
    CANALIZAR("Canalizar", Material.LIGHTNING_ROD, 0.2, 1.0, "Segure o clique para carregar: até o dobro da força em 2s",
            EnumSet.of(TOQUE, PROJETIL, CORPO, CRIACAO, SOPRO, CHUVA, ARMA)),
    RICOCHETE("Ricochete", Material.SLIME_BALL, 0.25, 1.4, "Salta para até 3 inimigos por perto (70% da força)",
            EnumSet.of(PROJETIL, TOQUE, RAIO)),
    PERSISTENTE("Persistente", Material.LINGERING_POTION, 0.3, 1.4, "Deixa uma zona do elemento no chão por 5s",
            EnumSet.of(PROJETIL, TOQUE, SOPRO, CHUVA)),
    MINA("Mina", Material.TRIPWIRE_HOOK, 0.35, 1.3, "Vira uma armadilha onde cair: explode quando um inimigo chega perto",
            EnumSet.of(PROJETIL, TOQUE, CRIACAO)),
    ECO("Eco", Material.ECHO_SHARD, 0.4, 1.6, "Repete a magia 1s depois com metade da força",
            EnumSet.of(TOQUE, PROJETIL, AURA, CRIACAO, SOPRO, CHUVA)),
    POTENTE("Potente", Material.BLAZE_POWDER, 0.5, 1.5, "35% mais forte, mas a recarga fica 50% maior",
            EnumSet.complementOf(EnumSet.of(PROIBIDA)));

    public static final int MAXIMO = 2;

    private final String nome;
    private final Material icone;
    private final double nivel;
    private final double multCusto;
    private final String descricao;
    private final Set<Forma> formas;

    Modificador(String nome, Material icone, double nivel, double multCusto, String descricao, Set<Forma> formas) {
        this.nome = nome;
        this.icone = icone;
        this.nivel = nivel;
        this.multCusto = multCusto;
        this.descricao = descricao;
        this.formas = formas;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    public double multCusto() { return multCusto; }
    public String descricao() { return descricao; }
    /** Nível de Arcano para liberar (fração do máximo). */
    public int nivelNecessario(int nivelMaximo) { return (int) Math.ceil(nivel * nivelMaximo); }
    public boolean serveEm(Forma f) { return formas.contains(f); }

    /** Dois modificadores que não combinam. */
    public boolean conflita(Modificador outro) {
        return (this == RAPIDA && outro == POTENTE) || (this == POTENTE && outro == RAPIDA);
    }

    public static Modificador porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
