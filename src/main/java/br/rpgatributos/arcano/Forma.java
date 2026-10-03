package br.rpgatributos.arcano;

import org.bukkit.Material;

/** Como a magia age. As mesmas essências viram magias diferentes em cada forma. */
public enum Forma {
    TOQUE("Toque", Material.STICK, 1.0, "No bloco ou criatura que você olha, de perto"),
    PROJETIL("Projétil", Material.SNOWBALL, 1.2, "Lançada à distância, explode onde acertar"),
    AURA("Aura", Material.BEACON, 1.5, "Pulsa em volta de você por alguns segundos"),
    CORPO("Corpo", Material.ARMOR_STAND, 1.0, "Age em você mesmo"),
    CRIACAO("Criação", Material.BRICKS, 1.3, "Materializa algo no lugar que você olha"),
    RAIO("Raio", Material.END_ROD, 0.8, "Feixe contínuo enquanto você segura o clique (gasta mana aos poucos)"),
    SOPRO("Sopro", Material.DRAGON_BREATH, 1.3, "Cone na sua frente, como o sopro de um dragão"),
    CHUVA("Chuva", Material.POINTED_DRIPSTONE, 1.6, "Cai do céu numa área longe de você por alguns segundos"),
    INVOCACAO("Invocação", Material.SOUL_LANTERN, 1.8, "Chama um elemental que luta ao seu lado"),
    ARMA("Encantar arma", Material.GOLDEN_SWORD, 1.1, "Seus próximos golpes carregam o elemento"),
    /** Forma dos Tomos Proibidos: não aparece no criador de magias. */
    PROIBIDA("Proibida", Material.ENCHANTED_BOOK, 2.0, "Magia aprendida num Tomo Proibido");

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
    /** Aparece no criador de magias? */
    public boolean criavel() { return this != PROIBIDA; }

    public static Forma porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
