package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bebidas do Barril de Envelhecimento. A idade (dias reais no barril) define o grau: Nova,
 * Envelhecida (1 dia), Reserva (3 dias) e Lendária (7 dias). Cada grau dura mais e fica mais forte.
 */
public enum Bebida {
    HIDROMEL("Hidromel", 0xE8A33D, Map.of(Material.HONEY_BOTTLE, 3), 0, "Regeneração e saciedade",
            new Efeito(PotionEffectType.REGENERATION, 30, 0), new Efeito(PotionEffectType.SATURATION, 1, 0)),
    CIDRA("Cidra", 0xE57373, Map.of(Material.APPLE, 6), 0, "Velocidade e pressa",
            new Efeito(PotionEffectType.SPEED, 120, 0), new Efeito(PotionEffectType.HASTE, 60, 0)),
    VINHO("Vinho de Frutas", 0x8E244D, Map.of(Material.SWEET_BERRIES, 12), 0, "Força e sorte",
            new Efeito(PotionEffectType.STRENGTH, 60, 0), new Efeito(PotionEffectType.LUCK, 120, 0)),
    CERVEJA("Cerveja de Trigo", 0xD4A017, Map.of(Material.WHEAT, 8), 0, "Resistência e absorção (sobe um pouco à cabeça)",
            new Efeito(PotionEffectType.RESISTANCE, 90, 0), new Efeito(PotionEffectType.ABSORPTION, 60, 0)),
    LICOR_ARCANO("Licor Arcano", 0x9575CD, Map.of(Material.GLOW_BERRIES, 6), 2, "Visão noturna e mana",
            new Efeito(PotionEffectType.NIGHT_VISION, 180, 0)),
    LICOR_DE_ERVAS("Licor de Ervas", 0x7CB342, Map.of(Material.HONEY_BOTTLE, 1), 0, "Resistência ao fogo e cura lenta (pede 3 ervas)",
            new Efeito(PotionEffectType.FIRE_RESISTANCE, 120, 0), new Efeito(PotionEffectType.REGENERATION, 20, 0));

    public record Efeito(PotionEffectType tipo, int segundos, int nivel) { }

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "bebida");
    public static final NamespacedKey CHAVE_GRAU = new NamespacedKey("rpgatributos", "bebida_grau");
    public static final String[] GRAUS = {"Nova", "Envelhecida", "Reserva", "Lendária"};

    private final String nome, descricao;
    private final TextColor cor;
    private final Map<Material, Integer> ingredientes;
    /** Pó Arcano a mais (Licor Arcano). */
    private final int poArcano;
    private final List<Efeito> efeitos;

    Bebida(String nome, int cor, Map<Material, Integer> ingredientes, int poArcano, String descricao, Efeito... efeitos) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.ingredientes = ingredientes;
        this.poArcano = poArcano;
        this.descricao = descricao;
        this.efeitos = List.of(efeitos);
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public String descricao() { return descricao; }
    public Map<Material, Integer> ingredientes() { return ingredientes; }
    public int poArcano() { return poArcano; }
    public boolean pedeErvas() { return this == LICOR_DE_ERVAS; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Grau (0 a 3) pela idade em dias. */
    public static int grau(double dias, int[] limites) {
        int g = 0;
        for (int i = 0; i < limites.length; i++) if (dias >= limites[i]) g = i + 1;
        return Math.min(3, g);
    }

    public ItemStack criar(int grau) {
        int g = Math.max(0, Math.min(3, grau));
        double duracao = 1 + 0.5 * g;
        ItemStack i = new ItemStack(Material.POTION);
        i.editMeta(PotionMeta.class, m -> {
            m.itemName(Component.text(nome + " " + GRAUS[g] + (g > 0 ? " " + "★".repeat(g) : ""), g == 3 ? NamedTextColor.GOLD : cor));
            m.setColor(Color.fromRGB(cor.value()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Ao beber:", NamedTextColor.GRAY));
            for (Efeito ef : efeitos) {
                int nivel = ef.nivel() + (g == 3 && ef.segundos() > 1 ? 1 : 0);
                int ticks = ef.segundos() <= 1 ? 1 : (int) Math.round(ef.segundos() * 20 * duracao);
                m.addCustomEffect(new PotionEffect(ef.tipo(), ticks, nivel), true);
                lore.add(Component.text(" ✧ " + nomeEfeito(ef.tipo()) + (nivel > 0 ? " " + (nivel + 1) : "")
                        + (ticks > 20 ? " (" + ticks / 20 + " s)" : ""), NamedTextColor.WHITE));
            }
            if (this == LICOR_ARCANO) lore.add(Component.text(" ✧ Recupera " + (40 + 20 * g) + " de mana", NamedTextColor.LIGHT_PURPLE));
            if (this == CERVEJA && g < 3) lore.add(Component.text(" ✧ Tontura por alguns segundos", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("🛢 Envelhecida no barril · " + GRAUS[g], cor));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            if (g == 3) m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            m.getPersistentDataContainer().set(CHAVE_GRAU, PersistentDataType.INTEGER, g);
            PacoteRecursos.marcar(m, "bebida_" + id());
        });
        return i;
    }

    private static String nomeEfeito(PotionEffectType t) {
        if (t == PotionEffectType.REGENERATION) return "Regeneração";
        if (t == PotionEffectType.SATURATION) return "Saciedade";
        if (t == PotionEffectType.SPEED) return "Velocidade";
        if (t == PotionEffectType.HASTE) return "Pressa";
        if (t == PotionEffectType.STRENGTH) return "Força";
        if (t == PotionEffectType.LUCK) return "Sorte";
        if (t == PotionEffectType.RESISTANCE) return "Resistência";
        if (t == PotionEffectType.ABSORPTION) return "Absorção";
        if (t == PotionEffectType.NIGHT_VISION) return "Visão noturna";
        if (t == PotionEffectType.FIRE_RESISTANCE) return "Resistência ao fogo";
        return t.getKey().getKey();
    }

    public static Bebida de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static int grauDo(ItemStack i) {
        return i == null || !i.hasItemMeta() ? 0 : i.getPersistentDataContainer().getOrDefault(CHAVE_GRAU, PersistentDataType.INTEGER, 0);
    }
}
