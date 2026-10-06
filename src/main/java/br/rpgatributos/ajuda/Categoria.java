package br.rpgatributos.ajuda;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Locale;

/** As 7 grandes áreas do /rpg e do Guia. */
public enum Categoria {
    PERSONAGEM("Personagem", "Atributos, talentos, classe, títulos e acessórios", Material.PLAYER_HEAD, 0xFFD54F),
    COMBATE("Combate", "Combos, lendas, masmorras, chefes e perigos", Material.IRON_SWORD, 0xE05050),
    MAGIA("Magia e fé", "Grimório, magias, runas, deuses e estrelas", Material.ENCHANTED_BOOK, 0xC77DFF),
    OFICIOS("Ofícios", "Forja, alquimia, cozinha, fazenda, pesca e mais", Material.SMITHING_TABLE, 0xE8823A),
    MUNDO("Mundo e aventura", "Estações, estruturas, tesouros e viagens", Material.COMPASS, 0x4FC3F7),
    COMUNIDADE("Comunidade", "Party, território, colônia, reino e companheiros", Material.BELL, 0x8BC34A),
    COLECOES("Coleções", "Álbum, bestiário, recordes, segredos e diário", Material.FILLED_MAP, 0xB388FF);

    private final String nome, descricao;
    private final Material icone;
    private final TextColor cor;

    Categoria(String nome, String descricao, Material icone, int cor) {
        this.nome = nome;
        this.descricao = descricao;
        this.icone = icone;
        this.cor = TextColor.color(cor);
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public Material icone() { return icone; }
    public TextColor cor() { return cor; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
}
