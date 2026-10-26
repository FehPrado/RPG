package br.rpgatributos.visual;

import org.bukkit.Material;
import java.util.Locale;

/** Objetos ornamentais: o custo é gasto uma vez ao fabricar; recuperar devolve a peça. */
public enum ObjetoDecorativo {
    BANDEIRA("Estandarte", Material.WHITE_WOOL, 4), ESTANTE("Estante de grimórios", Material.OAK_PLANKS, 8),
    MESA("Mesa de taverna", Material.OAK_PLANKS, 6), CADEIRA("Cadeira", Material.OAK_PLANKS, 3),
    ARMAS("Suporte de armas", Material.IRON_INGOT, 4), ESCUDO("Escudo ornamental", Material.IRON_INGOT, 3),
    BRASEIRO("Braseiro", Material.COAL, 4), LANTERNA("Lanterna de chão", Material.IRON_NUGGET, 8),
    CRISTAL("Cristal luminoso", Material.AMETHYST_SHARD, 4), ALTAR("Altar ornamental", Material.STONE_BRICKS, 8),
    CAIXOTES("Caixotes", Material.OAK_PLANKS, 4), VASO("Vaso com raízes", Material.BRICK, 4),
    PLACA("Placa de comércio", Material.OAK_PLANKS, 3), BAU("Moldura de tesouro", Material.GOLD_NUGGET, 8),
    PORTICO("Pórtico em ruínas", Material.STONE_BRICKS, 12);

    public final String nome;
    public final Material custo;
    public final int quantidade;
    ObjetoDecorativo(String nome, Material custo, int quantidade) { this.nome = nome; this.custo = custo; this.quantidade = quantidade; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static ObjetoDecorativo porId(String id) {
        for (var o : values()) if (o.id().equals(id)) return o;
        return null;
    }
}
