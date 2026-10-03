package br.rpgatributos.arcano;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Os 10 elementos. Todo item do jogo carrega uma ou mais essências; infundir
 * itens no corpo é o que libera as essências para criar magias.
 */
public enum Essencia {
    FOGO("Fogo", "♨", 0xFF6A2B, Material.BLAZE_POWDER, "queima"),
    GELO("Gelo", "❄", 0x9FE7FF, Material.BLUE_ICE, "congela e desacelera"),
    VENTO("Vento", "☁", 0xDDEFEF, Material.FEATHER, "empurra e dá agilidade"),
    AGUA("Água", "≈", 0x3D8BFF, Material.PRISMARINE_SHARD, "molha, apaga e hidrata"),
    VIDA("Vida", "❤", 0xFF5C8A, Material.GOLDEN_APPLE, "cura e faz crescer"),
    TERRA("Terra", "⛰", 0xB58A55, Material.COBBLESTONE, "cria blocos e endurece"),
    SOMBRA("Sombra", "☠", 0x9A86B8, Material.WITHER_ROSE, "cega e drena vida"),
    VAZIO("Vazio", "◎", 0xC77DFF, Material.ENDER_PEARL, "teleporta e puxa"),
    ENERGIA("Energia", "⚡", 0xFFE14D, Material.GLOWSTONE_DUST, "eletrifica e acelera"),
    NATUREZA("Natureza", "☘", 0x5BD15B, Material.OAK_SAPLING, "faz brotar e envenena");

    private final String nome;
    private final String simbolo;
    private final TextColor cor;
    private final Color corParticula;
    private final Material icone;
    private final String papel;

    Essencia(String nome, String simbolo, int cor, Material icone, String papel) {
        this.nome = nome;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.corParticula = Color.fromRGB(cor);
        this.icone = icone;
        this.papel = papel;
    }

    public String nome() { return nome; }
    public String simbolo() { return simbolo; }
    public TextColor cor() { return cor; }
    public Material icone() { return icone; }
    /** O que a essência faz numa magia. */
    public String papel() { return papel; }

    public Particle.DustOptions poeira(float tamanho) {
        return new Particle.DustOptions(corParticula, tamanho);
    }

    /** "♨ Fogo" na cor da essência. */
    public Component rotulo() {
        return Component.text(simbolo + " " + nome, cor);
    }

    public static Essencia porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
