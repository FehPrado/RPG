package br.rpgatributos.oficio;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;

/**
 * Materiais do curtume e da tecelagem: as peles cruas que as criaturas deixam, os couros já
 * curtidos no Ateliê do Curtidor e os tecidos do tear.
 */
public enum MaterialOficio {
    // ---------- peles cruas (das criaturas) ----------
    PELE_DE_LOBO("Pele de Lobo", Tipo.PELE, "rabbit_hide", 0x8A8A8A, "Cai dos lobos selvagens."),
    PELE_DE_URSO("Pele de Urso Polar", Tipo.PELE, "rabbit_hide", 0xF2F2F2, "Cai dos ursos polares."),
    PELE_DE_RAPOSA("Pele de Raposa", Tipo.PELE, "rabbit_hide", 0xE07B2E, "Cai das raposas."),
    ESCAMA_DE_GUARDIAO("Escama de Guardião", Tipo.PELE, "prismarine_shard", 0x5FA89A, "Cai dos guardiões do mar."),
    COURO_DE_HOGLIN("Couro de Hoglin", Tipo.PELE, "rabbit_hide", 0xA0523A, "Cai dos hoglins e zoglins do Nether."),
    // ---------- couros curtidos ----------
    COURO_LOBO("Couro de Lobo Curtido", Tipo.COURO, "leather", 0x6D6460, "Para o Conjunto do Caçador."),
    COURO_URSO("Couro de Urso Curtido", Tipo.COURO, "leather", 0xE6E1D6, "Para o Conjunto do Urso Polar."),
    COURO_RAPOSA("Couro de Raposa Curtido", Tipo.COURO, "leather", 0xC9682A, "Para o Conjunto da Raposa."),
    COURO_ESCAMAS("Couro de Escamas", Tipo.COURO, "leather", 0x3E8C82, "Para o Conjunto da Maré."),
    COURO_BRASA("Couro de Brasa", Tipo.COURO, "leather", 0x8B2E22, "Para o Conjunto da Brasa."),
    COURO_NOTURNO("Couro Noturno", Tipo.COURO, "leather", 0x3B2E5A, "Membrana de phantom curtida. Para o Conjunto da Noite."),
    // ---------- tecidos ----------
    TECIDO("Tecido de Linha", Tipo.TECIDO, "paper", 0xEDE6D6, "Linha tecida no tear."),
    FELTRO("Feltro", Tipo.TECIDO, "paper", 0xB7A089, "Lã batida até virar um pano grosso e quente."),
    TECIDO_DE_ALGA("Tecido de Alga", Tipo.TECIDO, "paper", 0x4E7A3A, "Alga seca trançada: não apodrece na água.");

    public enum Tipo { PELE, COURO, TECIDO }

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "oficio");

    private final String nome;
    private final Tipo tipo;
    private final NamespacedKey modelo;
    private final TextColor cor;
    private final String descricao;

    MaterialOficio(String nome, Tipo tipo, String modelo, int cor, String descricao) {
        this.nome = nome;
        this.tipo = tipo;
        this.modelo = NamespacedKey.minecraft(modelo);
        this.cor = TextColor.color(cor);
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public Tipo tipo() { return tipo; }
    public TextColor cor() { return cor; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.setItemModel(modelo);
            String rodape = switch (tipo) {
                case PELE -> "✂ Pele crua: curta no Ateliê do Curtidor";
                case COURO -> "✂ Couro curtido";
                case TECIDO -> "✂ Tecido";
            };
            m.lore(List.of(
                    Component.text(descricao, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text(rodape, cor).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "oficio_" + id());
        });
        return i;
    }

    public static MaterialOficio de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static MaterialOficio porId(String id) {
        for (MaterialOficio m : values()) if (m.id().equalsIgnoreCase(id)) return m;
        return null;
    }
}
