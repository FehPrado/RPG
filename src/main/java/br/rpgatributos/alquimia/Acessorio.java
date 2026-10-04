package br.rpgatributos.alquimia;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Acessórios: anéis, amuletos, cintos e itens de bolso. Ficam nos espaços de acessório
 * (/acessorios), sem ocupar a armadura, e funcionam enquanto estão lá.
 */
public enum Acessorio {
    // ---------- anéis ----------
    ANEL_VIGOR("Anel do Vigor", Tipo.ANEL, "gold_nugget", 0xE0115F, "+2 ❤ de vida máxima",
            List.of(m(Attribute.MAX_HEALTH, 4, Operation.ADD_NUMBER)), null, Especial.NENHUM),
    ANEL_FORCA("Anel da Força", Tipo.ANEL, "iron_nugget", 0x5A5A6E, "+1 de dano",
            List.of(m(Attribute.ATTACK_DAMAGE, 1, Operation.ADD_NUMBER)), null, Especial.NENHUM),
    ANEL_VENTO("Anel do Vento", Tipo.ANEL, "gold_nugget", 0xFFC93C, "+5% de velocidade",
            List.of(m(Attribute.MOVEMENT_SPEED, 0.05, Operation.ADD_SCALAR)), null, Especial.NENHUM),
    ANEL_MARES("Anel das Marés", Tipo.ANEL, "nautilus_shell", 0x2E86DE, "Respiração aquática e +30% de agilidade na água",
            List.of(m(Attribute.WATER_MOVEMENT_EFFICIENCY, 0.3, Operation.ADD_NUMBER)), pocao(PotionEffectType.WATER_BREATHING, 0), Especial.NENHUM),
    ANEL_BRASAS("Anel das Brasas", Tipo.ANEL, "blaze_powder", 0xFF6A2B, "Resistência ao fogo",
            List.of(), pocao(PotionEffectType.FIRE_RESISTANCE, 0), Especial.NENHUM),
    ANEL_MINERADOR("Anel do Minerador", Tipo.ANEL, "amethyst_shard", 0xB66BFF, "Pressa I",
            List.of(), pocao(PotionEffectType.HASTE, 0), Especial.NENHUM),
    ANEL_FORTUNA("Anel da Fortuna", Tipo.ANEL, "emerald", 0x2ECC71, "+1,5 de sorte (baús e pesca melhores)",
            List.of(m(Attribute.LUCK, 1.5, Operation.ADD_NUMBER)), null, Especial.NENHUM),
    ANEL_SABIO("Anel do Sábio", Tipo.ANEL, "prismarine_crystals", 0x7FD7FF, "+5% de XP em todos os atributos",
            List.of(), null, Especial.XP),

    // ---------- amuletos ----------
    AMULETO_MANA("Amuleto de Mana", Tipo.AMULETO, "heart_of_the_sea", 0x7F8CFF, "+40 de mana máxima e +1 de mana por segundo",
            List.of(), null, Especial.MANA),
    AMULETO_GUARDIAO("Amuleto do Guardião", Tipo.AMULETO, "prismarine_shard", 0x3D6BFF, "+2 de armadura e +1 de resistência",
            List.of(m(Attribute.ARMOR, 2, Operation.ADD_NUMBER), m(Attribute.ARMOR_TOUGHNESS, 1, Operation.ADD_NUMBER)), null, Especial.NENHUM),
    AMULETO_VIDA("Amuleto da Vida", Tipo.AMULETO, "golden_apple", 0xFF4D6D, "Regeneração I",
            List.of(), pocao(PotionEffectType.REGENERATION, 0), Especial.NENHUM),
    AMULETO_FENIX("Amuleto da Fênix", Tipo.AMULETO, "totem_of_undying", 0xFF7A1A, "Escapa da morte uma vez a cada 10 minutos",
            List.of(), null, Especial.FENIX),

    // ---------- cintos ----------
    CINTO_ATLETA("Cinto do Atleta", Tipo.CINTO, "lead", 0x55FF55, "+10% de pulo e +2 blocos de queda segura",
            List.of(m(Attribute.JUMP_STRENGTH, 0.1, Operation.ADD_SCALAR), m(Attribute.SAFE_FALL_DISTANCE, 2, Operation.ADD_NUMBER)), null, Especial.NENHUM),
    CINTO_ANDARILHO("Cinto do Andarilho", Tipo.CINTO, "leather", 0xC9955C, "Sobe blocos sem pular e +3% de velocidade",
            List.of(m(Attribute.STEP_HEIGHT, 0.5, Operation.ADD_NUMBER), m(Attribute.MOVEMENT_SPEED, 0.03, Operation.ADD_SCALAR)), null, Especial.NENHUM),
    CINTO_TITA("Cinto do Titã", Tipo.CINTO, "iron_ingot", 0xAAAAAA, "+2 ❤ de vida máxima e +10% de resistência a repulsão",
            List.of(m(Attribute.MAX_HEALTH, 4, Operation.ADD_NUMBER), m(Attribute.KNOCKBACK_RESISTANCE, 0.1, Operation.ADD_NUMBER)), null, Especial.NENHUM),

    // ---------- bolso ----------
    LANTERNA_BOLSO("Lanterna de Bolso", Tipo.BOLSO, "lantern", 0xFFD27F, "Ilumina em volta de você",
            List.of(), null, Especial.LANTERNA),
    IMA_BOLSO("Ímã de Bolso", Tipo.BOLSO, "lodestone", 0xD8D8E8, "Puxa os itens do chão a até 6 blocos",
            List.of(), null, Especial.IMA),
    RELOGIO_BOLSO("Relógio de Bolso", Tipo.BOLSO, "gold_ingot", 0xFFC93C, "Agachado: mostra hora, coordenadas e bioma",
            List.of(), null, Especial.RELOGIO),
    CAPA_PLANADORA("Capa Planadora", Tipo.BOLSO, "phantom_membrane", 0xA0C4FF, "No ar, aperte pular para planar (agache para soltar)",
            List.of(), null, Especial.PLANADOR);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "acessorio");

    /** Onde o acessório fica. */
    public enum Tipo {
        ANEL("Anel", 2), AMULETO("Amuleto", 1), CINTO("Cinto", 1), BOLSO("Bolso", 2);

        private final String nome;
        private final int espacos;

        Tipo(String nome, int espacos) {
            this.nome = nome;
            this.espacos = espacos;
        }

        public String nome() { return nome; }
        public int espacos() { return espacos; }
    }

    /** Algo que o plugin faz além de atributos e efeitos de poção. */
    public enum Especial { NENHUM, MANA, XP, FENIX, LANTERNA, IMA, RELOGIO, PLANADOR }

    public record Mod(Attribute atributo, double valor, Operation operacao) {}

    public record Pocao(PotionEffectType tipo, int nivel) {}

    private static Mod m(Attribute a, double v, Operation op) { return new Mod(a, v, op); }

    private static Pocao pocao(PotionEffectType t, int nivel) { return new Pocao(t, nivel); }

    private final String nome;
    private final Tipo tipo;
    private final NamespacedKey modelo;
    private final TextColor cor;
    private final String efeito;
    private final List<Mod> mods;
    private final Pocao pocao;
    private final Especial especial;

    Acessorio(String nome, Tipo tipo, String modelo, int cor, String efeito, List<Mod> mods, Pocao pocao, Especial especial) {
        this.nome = nome;
        this.tipo = tipo;
        this.modelo = NamespacedKey.minecraft(modelo);
        this.cor = TextColor.color(cor);
        this.efeito = efeito;
        this.mods = mods;
        this.pocao = pocao;
        this.especial = especial;
    }

    public String nome() { return nome; }
    public Tipo tipo() { return tipo; }
    public TextColor cor() { return cor; }
    public String efeito() { return efeito; }
    public List<Mod> mods() { return mods; }
    public Pocao pocao() { return pocao; }
    public Especial especial() { return especial; }
    public NamespacedKey modelo() { return modelo; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar() {
        ItemStack i = new ItemStack(Material.QUARTZ);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.setItemModel(modelo);
            m.setMaxStackSize(1);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("✧ " + efeito, NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(Component.text("Coloque num espaço de " + tipo.nome().toLowerCase(Locale.ROOT) + ":", NamedTextColor.GRAY));
            lore.add(Component.text("/acessorios", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("❖ Acessório · " + tipo.nome(), cor));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, id()); // visual próprio, se o pacote tiver
        });
        return i;
    }

    public static Acessorio de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Acessorio porId(String id) {
        for (Acessorio a : values()) if (a.id().equalsIgnoreCase(id)) return a;
        return null;
    }
}
