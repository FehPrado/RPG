package br.rpgatributos.domador;

import org.bukkit.Material;

import java.util.Locale;

/** O que o companheiro está fazendo. Nenhum modo teleporta: ele sempre vai andando. */
public enum Modo {
    SEGUIR("Seguir", Material.LEAD, 0,
            "Anda atrás de você (sem teleportar) e te defende, se souber lutar."),
    FICAR("Ficar", Material.RED_BED, 0,
            "Fica parado onde você está agora."),
    GUARDAR("Guardar", Material.SHIELD, 0.15,
            "Fica onde você está e ataca os monstros que chegarem perto."),
    PATRULHAR("Patrulhar", Material.COMPASS, 0.25,
            "Anda pela área em volta de onde você está, atacando monstros."),
    SOLTO("Solto", Material.OAK_SAPLING, 0,
            "Faz o que quiser, como um animal comum.");

    private final String nome;
    private final Material icone;
    private final double nivel;
    private final String descricao;

    Modo(String nome, Material icone, double nivel, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public int nivelNecessario(int nivelMaximo) {
        return (int) Math.ceil(nivel * nivelMaximo);
    }

    /** Usa o lugar onde o dono está (Ficar, Guardar, Patrulhar). */
    public boolean usaCentro() {
        return this == FICAR || this == GUARDAR || this == PATRULHAR;
    }
}
