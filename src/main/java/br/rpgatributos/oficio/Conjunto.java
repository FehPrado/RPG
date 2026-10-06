package br.rpgatributos.oficio;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;

/**
 * Os conjuntos de armadura leve do Ateliê do Curtidor: 4 peças de couro com cor própria, mais
 * resistentes que o couro comum, e um bônus com 2 e outro com 4 peças vestidas.
 */
public enum Conjunto {
    CACADOR("do Caçador", 0x6D5A4A, MaterialOficio.COURO_LOBO, MaterialOficio.TECIDO,
            "+5% de velocidade",
            "+10% de dano com flechas e tridente; na postura Ágil, +8% de dano e a esquiva gasta 25% menos vigor"),
    URSO("do Urso Polar", 0xEDEDED, MaterialOficio.COURO_URSO, MaterialOficio.FELTRO,
            "+1 ❤ de vida máxima",
            "-10% de dano recebido e resiste a empurrões"),
    RAPOSA("da Raposa", 0xD9772B, MaterialOficio.COURO_RAPOSA, MaterialOficio.TECIDO,
            "Sorte (saque e pesca melhores)",
            "De noite: visão noturna e velocidade"),
    MARE("da Maré", 0x2E6F73, MaterialOficio.COURO_ESCAMAS, MaterialOficio.TECIDO_DE_ALGA,
            "Respira debaixo d'água",
            "Graça do golfinho na água e +5% de peixe raro"),
    NOITE("da Noite", 0x3B2E5A, MaterialOficio.COURO_NOTURNO, MaterialOficio.TECIDO,
            "-50% de dano de queda",
            "Caindo do alto, você desce devagar"),
    BRASA("da Brasa", 0x8B2E22, MaterialOficio.COURO_BRASA, MaterialOficio.FELTRO,
            "-30% de dano de fogo e lava",
            "Não sente calor e, com pouca vida, ganha Força");

    /** As peças: nome, material de couro, slot, couros e tecidos que gastam. */
    public enum Peca {
        CAPUZ("Capuz", Material.LEATHER_HELMET, EquipmentSlot.HEAD, 3, 1),
        GIBAO("Gibão", Material.LEATHER_CHESTPLATE, EquipmentSlot.CHEST, 5, 2),
        CALCAS("Calças", Material.LEATHER_LEGGINGS, EquipmentSlot.LEGS, 4, 2),
        BOTAS("Botas", Material.LEATHER_BOOTS, EquipmentSlot.FEET, 2, 1);

        final String nome;
        final Material material;
        final EquipmentSlot slot;
        final int couros, tecidos;

        Peca(String nome, Material material, EquipmentSlot slot, int couros, int tecidos) {
            this.nome = nome;
            this.material = material;
            this.slot = slot;
            this.couros = couros;
            this.tecidos = tecidos;
        }

        public String nome() { return nome; }
        public Material material() { return material; }
        public int couros() { return couros; }
        public int tecidos() { return tecidos; }

        static Peca de(Material m) {
            for (Peca p : values()) if (p.material == m) return p;
            return null;
        }
    }

    private final String de;
    private final int cor;
    private final MaterialOficio couro, tecido;
    private final String bonus2, bonus4;

    Conjunto(String de, int cor, MaterialOficio couro, MaterialOficio tecido, String bonus2, String bonus4) {
        this.de = de;
        this.cor = cor;
        this.couro = couro;
        this.tecido = tecido;
        this.bonus2 = bonus2;
        this.bonus4 = bonus4;
    }

    /** "Conjunto do Caçador". */
    public String nome() { return "Conjunto " + de; }
    /** "Gibão do Caçador". */
    public String nomePeca(Peca p) { return p.nome + " " + de; }
    public TextColor cor() { return TextColor.color(cor); }
    public Color corCouro() { return Color.fromRGB(cor); }
    public MaterialOficio couro() { return couro; }
    public MaterialOficio tecido() { return tecido; }
    public String bonus2() { return bonus2; }
    public String bonus4() { return bonus4; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Conjunto porId(String id) {
        for (Conjunto c : values()) if (c.id().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) return c;
        return null;
    }
}
