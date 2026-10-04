package br.rpgatributos.progressao;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

/** As quatro árvores de talentos. */
public enum Arvore {
    GUERREIRO("Guerreiro", "⚔", TextColor.color(0xE05050), Material.IRON_SWORD, "Dano, defesa e vigor."),
    ARCANO("Arcano", "✦", TextColor.color(0xC77DFF), Material.AMETHYST_SHARD, "Mana, magias mais baratas e escudo de mana."),
    ARTESAO("Artesão", "⚒", TextColor.color(0xE8823A), Material.ANVIL, "Forja, refino, colheitas, alquimia e cozinha."),
    EXPLORADOR("Explorador", "➶", TextColor.color(0x7BC043), Material.COMPASS, "Velocidade, sorte, esquiva, mapas e mitrilo.");

    private final String nome, icone, descricao;
    private final TextColor cor;
    private final Material item;

    Arvore(String nome, String icone, TextColor cor, Material item, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.cor = cor;
        this.item = item;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String icone() { return icone; }
    public TextColor cor() { return cor; }
    public Material item() { return item; }
    public String descricao() { return descricao; }
}
