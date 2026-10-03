package br.rpgatributos.forja;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Que tipo de equipamento pode ser forjado e onde ele funciona. */
public enum Categoria {
    //                       tipo                 onde funciona                 material gasto
    ESPADA("Espada",     Tipo.CORPO_A_CORPO, EquipmentSlotGroup.MAINHAND, 2),
    MACHADO("Machado",   Tipo.CORPO_A_CORPO, EquipmentSlotGroup.MAINHAND, 3),
    LANCA("Lança",       Tipo.CORPO_A_CORPO, EquipmentSlotGroup.MAINHAND, 1),
    MACA("Maça",         Tipo.CORPO_A_CORPO, EquipmentSlotGroup.MAINHAND, 1),
    ARCO("Arco",         Tipo.DISTANCIA,     EquipmentSlotGroup.HAND,     1),
    BESTA("Besta",       Tipo.DISTANCIA,     EquipmentSlotGroup.HAND,     1),
    PICARETA("Picareta", Tipo.FERRAMENTA,    EquipmentSlotGroup.MAINHAND, 3),
    PA("Pá",             Tipo.FERRAMENTA,    EquipmentSlotGroup.MAINHAND, 1),
    ENXADA("Enxada",     Tipo.FERRAMENTA,    EquipmentSlotGroup.MAINHAND, 2),
    CAPACETE("Capacete", Tipo.ARMADURA,      EquipmentSlotGroup.HEAD,     5),
    PEITORAL("Peitoral", Tipo.ARMADURA,      EquipmentSlotGroup.CHEST,    8),
    CALCA("Calça",       Tipo.ARMADURA,      EquipmentSlotGroup.LEGS,     7),
    BOTAS("Botas",       Tipo.ARMADURA,      EquipmentSlotGroup.FEET,     4),
    ESCUDO("Escudo",     Tipo.ESCUDO,        EquipmentSlotGroup.HAND,     1);

    public enum Tipo { CORPO_A_CORPO, DISTANCIA, FERRAMENTA, ARMADURA, ESCUDO }

    private final String nome;
    private final Tipo tipo;
    private final EquipmentSlotGroup grupo;
    private final int custo;

    Categoria(String nome, Tipo tipo, EquipmentSlotGroup grupo, int custo) {
        this.nome = nome;
        this.tipo = tipo;
        this.grupo = grupo;
        this.custo = custo;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String nome() { return nome; }
    public Tipo tipo() { return tipo; }
    public EquipmentSlotGroup grupo() { return grupo; }
    /** Quantas unidades do material a receita gasta (usado no XP de Ferraria). */
    public int custo() { return custo; }

    public boolean armaCorpoACorpo() { return tipo == Tipo.CORPO_A_CORPO; }
    public boolean armaDistancia() { return tipo == Tipo.DISTANCIA; }
    /** Machado também é ferramenta. */
    public boolean ferramenta() { return tipo == Tipo.FERRAMENTA || this == MACHADO; }
    public boolean armadura() { return tipo == Tipo.ARMADURA; }
    /** Peças que contam na defesa: armadura e escudo. */
    public boolean defesa() { return tipo == Tipo.ARMADURA || tipo == Tipo.ESCUDO; }

    /** O item funciona nesse slot? (ex.: espada na cabeça não dá bônus) */
    public boolean ativoEm(EquipmentSlot slot) {
        return grupo.test(slot);
    }

    public static Categoria de(Material m) {
        if (m == null || m.isAir()) return null;
        if (m == Material.MACE) return MACA;
        if (m == Material.BOW) return ARCO;
        if (m == Material.CROSSBOW) return BESTA;
        if (m == Material.SHIELD) return ESCUDO;
        String n = m.name();
        if (n.endsWith("_SWORD")) return ESPADA;
        if (n.endsWith("_PICKAXE")) return PICARETA;
        if (n.endsWith("_AXE")) return MACHADO;
        if (n.endsWith("_SHOVEL")) return PA;
        if (n.endsWith("_HOE")) return ENXADA;
        if (n.endsWith("_SPEAR")) return LANCA;
        if (n.endsWith("_HELMET")) return CAPACETE;
        if (n.endsWith("_CHESTPLATE")) return PEITORAL;
        if (n.endsWith("_LEGGINGS")) return CALCA;
        if (n.endsWith("_BOOTS")) return BOTAS;
        return null;
    }

    // ---------- grupos usados por Stat e Efeito ----------

    static Set<Categoria> de(Categoria... c) {
        return EnumSet.copyOf(Arrays.asList(c));
    }

    static Set<Categoria> doTipo(Tipo... tipos) {
        Set<Categoria> s = EnumSet.noneOf(Categoria.class);
        for (Categoria c : values()) {
            for (Tipo t : tipos) if (c.tipo == t) s.add(c);
        }
        return s;
    }

    static Set<Categoria> ferramentas() {
        return de(PICARETA, PA, ENXADA, MACHADO);
    }

    static Set<Categoria> todas() {
        return EnumSet.allOf(Categoria.class);
    }
}
