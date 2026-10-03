package br.rpgatributos.arcano;

import org.bukkit.Material;

/** Como a magia age. As mesmas essências viram magias diferentes em cada forma. */
public enum Forma {
    TOQUE("Toque", Material.STICK, 1.0, "No bloco ou criatura que você olha, de perto"),
    PROJETIL("Projétil", Material.SNOWBALL, 1.2, "Lançada à distância, explode onde acertar"),
    AURA("Aura", Material.BEACON, 1.5, "Pulsa em volta de você por alguns segundos"),
    CORPO("Corpo", Material.ARMOR_STAND, 1.0, "Age em você mesmo"),
    CRIACAO("Criação", Material.BRICKS, 1.3, "Materializa algo no lugar que você olha");

    private final String nome;
    private final Material icone;
    private final double multCusto;
    private final String descricao;

    Forma(String nome, Material icone, double multCusto, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.multCusto = multCusto;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    public double multCusto() { return multCusto; }
    public String descricao() { return descricao; }

    public static Forma porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
