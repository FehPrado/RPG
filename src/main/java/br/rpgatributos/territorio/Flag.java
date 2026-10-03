package br.rpgatributos.territorio;

import org.bukkit.Material;

import java.util.Locale;

/** Regras que o dono liga e desliga no território. */
public enum Flag {
    PVP("PvP", Material.IRON_SWORD, false,
            "Jogadores podem se atacar aqui dentro."),
    PORTAS("Portas para visitantes", Material.OAK_DOOR, false,
            "Visitantes usam portas, portões, alçapões, botões, alavancas e placas de pressão."),
    BAUS("Baús para visitantes", Material.CHEST, false,
            "Visitantes abrem baús, barris, fornalhas, funis e outros blocos com inventário."),
    MONSTROS("Monstros nascem", Material.ZOMBIE_HEAD, true,
            "Monstros nascem naturalmente no território."),
    EXPLOSOES("Explosões quebram blocos", Material.TNT, false,
            "Creepers, TNT e outras explosões quebram blocos aqui."),
    FOGO("Fogo se espalha", Material.FLINT_AND_STEEL, false,
            "O fogo se espalha e queima blocos."),
    PARTY("Party pode construir", Material.CAKE, true,
            "Quem está na sua party constrói e mexe em tudo, como um membro.");

    private final String nome;
    private final Material icone;
    private final boolean padrao;
    private final String descricao;

    Flag(String nome, Material icone, boolean padrao, String descricao) {
        this.nome = nome;
        this.icone = icone;
        this.padrao = padrao;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Material icone() { return icone; }
    public boolean padrao() { return padrao; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Flag porId(String id) {
        for (Flag f : values()) if (f.id().equalsIgnoreCase(id)) return f;
        return null;
    }
}
