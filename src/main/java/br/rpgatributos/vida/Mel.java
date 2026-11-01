package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Os méis (3.15: um catálogo só). A Colmeia do Apicultor e os ninhos selvagens fazem os mesmos méis:
 * o sabor vem das flores por perto (ou do frio, da noite, dos girassóis). A Geleia Real é rara e sai
 * dos dois. Todos entram nas receitas que pedem mel.
 */
public enum Mel {
    SILVESTRE("Mel Silvestre", 0xE8A33D, "O mel de todo dia.", new PotionEffect(PotionEffectType.REGENERATION, 100, 0)),
    DOURADO("Mel Dourado", 0xFFD54F, "Nasce com girassóis por perto. Absorção II.", new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1)),
    GELADO("Mel Gelado", 0xB3E5FC, "Nasce no frio ou no inverno. Resistência ao fogo.", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 2400, 0)),
    NOTURNO("Mel Noturno", 0x5C6BC0, "Nasce à noite. Visão noturna.", new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0)),
    FLORAL("Mel Floral", 0xF48FB1, "Nasce com 5 flores diferentes por perto. Regeneração.", new PotionEffect(PotionEffectType.REGENERATION, 300, 0)),
    // 3.15: os méis do ninho selvagem (3.11) entram no mesmo catálogo.
    RUBRO("Mel Rubro", 0xE53935, "Papoulas e roseiras por perto. Força.", new PotionEffect(PotionEffectType.STRENGTH, 600, 0)),
    SOLAR("Mel Solar", 0xFDD835, "Dentes-de-leão por perto. Pressa.", new PotionEffect(PotionEffectType.HASTE, 1200, 0)),
    AZUL("Mel Azul", 0x42A5F5, "Orquídeas azuis e centáureas por perto. Visão noturna.", new PotionEffect(PotionEffectType.NIGHT_VISION, 1800, 0)),
    TULIPA("Mel de Tulipa", 0xF06292, "Tulipas e peônias por perto. Regeneração.", new PotionEffect(PotionEffectType.REGENERATION, 240, 0)),
    BRANCO("Mel Branco", 0xF5F5F5, "Lírios e margaridas por perto. Resistência e tira o veneno.", new PotionEffect(PotionEffectType.RESISTANCE, 600, 0)),
    GELEIA_REAL("Geleia Real", 0xFFB300, "Rara: o alimento da rainha. Absorção II.", new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "mel");
    /** A chave dos méis de ninho da 3.11 (antes do catálogo único). */
    private static final NamespacedKey CHAVE_ANTIGA = new NamespacedKey("rpgatributos", "mel_silvestre");

    private final String nome, descricao;
    private final TextColor cor;
    private final PotionEffect efeito;

    Mel(String nome, int cor, String descricao, PotionEffect efeito) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.descricao = descricao;
        this.efeito = efeito;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public PotionEffect efeito() { return efeito; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        return criar(qtd, Qualidade.NORMAL);
    }

    public ItemStack criar(int qtd, Qualidade q) {
        ItemStack i = new ItemStack(Material.HONEY_BOTTLE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text(descricao, NamedTextColor.GRAY), Component.empty(),
                            Component.text("🍯 Mel · entra nas receitas com mel", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            if (this == GELEIA_REAL) m.setEnchantmentGlintOverride(true);
            PacoteRecursos.marcar(m, "mel_" + id());
        });
        q.aplicar(i);
        return i;
    }

    /** A flor que mais aparece perto (até 5 blocos, pelo menos {@code minimo} dela) escolhe o mel; null se nenhuma der sabor. */
    public static Mel dasFlores(Block centro, int minimo) {
        Map<Mel, Integer> c = new EnumMap<>(Mel.class);
        for (int dx = -5; dx <= 5; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -5; dz <= 5; dz++) {
            Mel me = switch (centro.getRelative(dx, dy, dz).getType()) {
                case POPPY, ROSE_BUSH -> RUBRO;
                case DANDELION -> SOLAR;
                case BLUE_ORCHID, CORNFLOWER -> AZUL;
                case RED_TULIP, ORANGE_TULIP, PINK_TULIP, WHITE_TULIP, PEONY -> TULIPA;
                case LILY_OF_THE_VALLEY, OXEYE_DAISY, AZURE_BLUET -> BRANCO;
                default -> null;
            };
            if (me != null) c.merge(me, 1, Integer::sum);
        }
        return c.entrySet().stream().filter(e -> e.getValue() >= minimo).max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public static Mel de(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        var pdc = i.getPersistentDataContainer();
        String s = pdc.get(CHAVE, PersistentDataType.STRING);
        if (s == null) {
            s = pdc.get(CHAVE_ANTIGA, PersistentDataType.STRING);
            if ("GELEIA".equals(s)) s = GELEIA_REAL.name();
        }
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
