package br.rpgatributos.arcano;

import org.bukkit.Material;
import org.bukkit.Particle;

/** A "assinatura" visual das magias do jogador: uma partícula a mais em todas elas. */
public enum Estilo {
    PADRAO("Padrão", Material.GLASS_PANE, null, "Só as cores dos elementos."),
    ESTELAR("Estelar", Material.END_ROD, Particle.END_ROD, "Rastros de luz branca."),
    ALMAS("Almas", Material.SOUL_LANTERN, Particle.SOUL_FIRE_FLAME, "Chamas azuis de alma."),
    CRISTAL("Cristal", Material.AMETHYST_CLUSTER, Particle.WAX_ON, "Brilhos de cristal."),
    CEREJEIRA("Cerejeira", Material.CHERRY_LEAVES, Particle.CHERRY_LEAVES, "Pétalas caindo."),
    BRASAS("Brasas", Material.CAMPFIRE, Particle.LAVA, "Fagulhas de lava."),
    ECO("Eco", Material.SCULK_SHRIEKER, Particle.SCULK_SOUL, "Almas do sculk."),
    NOTAS("Melodia", Material.NOTE_BLOCK, Particle.NOTE, "Notas musicais.");

    private final String nome;
    private final Material icone;
    private final Particle particula;
    private final String descricao;

    Estilo(String nome, Material icone, Particle particula, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.particula = particula;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    /** Partícula extra (null = nenhuma). */
    public Particle particula() { return particula; }
    public String descricao() { return descricao; }

    public Estilo proximo() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Estilo porNome(String nome) {
        try {
            return nome == null ? PADRAO : valueOf(nome);
        } catch (IllegalArgumentException e) {
            return PADRAO;
        }
    }
}
