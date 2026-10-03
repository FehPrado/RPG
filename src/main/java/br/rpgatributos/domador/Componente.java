package br.rpgatributos.domador;

import org.bukkit.Material;

/**
 * Peças que o domador coloca numa criatura no Altar do Domador. Cada uma dá uma
 * habilidade e pede um nível de Doma (em fração do nível máximo).
 */
public enum Componente {
    SELA("Sela do Domador", Material.SADDLE, 0, null,
            "Dá para montar e guiar: WASD anda, espaço pula."),
    AGILIDADE("Agilidade", Material.SUGAR, 0.05, null,
            "+30% de velocidade, a pé e montado."),
    ALFORJE("Alforje", Material.CHEST, 0.10, null,
            "Carrega 27 itens (abra no menu do companheiro)."),
    FAROL("Farol", Material.GLOWSTONE, 0.10, null,
            "Brilha (dá para achar de longe) e dá Visão Noturna a quem monta."),
    COURACA("Couraça", Material.IRON_CHESTPLATE, 0.15, null,
            "+8 de armadura e +25% de vida."),
    GARRAS("Garras", Material.IRON_SWORD, 0.20, null,
            "Luta contra monstros (até uma vaca!) e bate +50% mais forte."),
    COLETOR("Coletor", Material.HOPPER, 0.25, ALFORJE,
            "Pega os itens do chão por perto e guarda no Alforje."),
    MARE("Coração do Mar", Material.HEART_OF_THE_SEA, 0.30, null,
            "Não se afoga e nada rápido; quem monta respira debaixo d'água."),
    CHAMAS("Chamas", Material.BLAZE_ROD, 0.35, null,
            "Imune a fogo e lava; os ataques põem fogo no alvo."),
    TOTEM("Segunda Vida", Material.TOTEM_OF_UNDYING, 0.40, null,
            "Escapa da morte uma vez (o totem se gasta)."),
    ASAS("Asas", Material.ELYTRA, 0.50, SELA,
            "Montado, voa: espaço sobe, agachar desce. Sem dano de queda."),
    NETHERITE("Pele de Netherite", Material.NETHERITE_INGOT, 0.60, null,
            "+6 de armadura, +4 de resistência, quase não é empurrado e é imune a fogo.");

    private final String nome;
    private final Material item;
    private final double nivel;
    private final Componente precisa;
    private final String descricao;

    Componente(String nome, Material item, double nivel, Componente precisa, String descricao) {
        this.nome = nome;
        this.item = item;
        this.nivel = nivel;
        this.precisa = precisa;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    /** O item que vira esse componente. */
    public Material item() { return item; }
    /** Componente que precisa estar na criatura antes (ex.: Asas pedem Sela), ou null. */
    public Componente precisa() { return precisa; }
    public String descricao() { return descricao; }

    public int nivelNecessario(int nivelMaximo) {
        return (int) Math.ceil(nivel * nivelMaximo);
    }

    public static Componente porItem(Material m) {
        for (Componente c : values()) if (c.item == m) return c;
        return null;
    }
}
