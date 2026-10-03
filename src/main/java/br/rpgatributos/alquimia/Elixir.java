package br.rpgatributos.alquimia;

import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Elixires da Bancada Alquímica: poções mais fortes e mais longas que as comuns,
 * várias com mais de um efeito. A qualidade e o nível de Alquimia aumentam a duração.
 */
public enum Elixir {
    CURA("Elixir de Cura", 0xFF4D6D, List.of(e(PotionEffectType.INSTANT_HEALTH, 1, 1), e(PotionEffectType.REGENERATION, 30, 0)), 0),
    RAPIDEZ("Elixir da Rapidez", 0x7FD7FF, List.of(e(PotionEffectType.SPEED, 240, 1), e(PotionEffectType.JUMP_BOOST, 240, 0)), 0),
    MINERADOR("Elixir do Minerador", 0xFFC93C, List.of(e(PotionEffectType.HASTE, 300, 1), e(PotionEffectType.NIGHT_VISION, 300, 0)), 0),
    MERGULHADOR("Elixir do Mergulhador", 0x2E86DE, List.of(e(PotionEffectType.WATER_BREATHING, 480, 0),
            e(PotionEffectType.NIGHT_VISION, 480, 0), e(PotionEffectType.DOLPHINS_GRACE, 120, 0)), 0),
    SORTE("Elixir da Sorte", 0x5BD15B, List.of(e(PotionEffectType.LUCK, 600, 1), e(PotionEffectType.HERO_OF_THE_VILLAGE, 600, 0)), 0),
    PEDRA("Elixir de Pedra", 0x8C8C8C, List.of(e(PotionEffectType.RESISTANCE, 300, 0), e(PotionEffectType.FIRE_RESISTANCE, 300, 0)), 0),
    SOMBRAS("Elixir das Sombras", 0x4B3B6B, List.of(e(PotionEffectType.INVISIBILITY, 360, 0), e(PotionEffectType.SPEED, 360, 0)), 0),
    MANA("Elixir de Mana", 0x7F8CFF, List.of(), 150),
    GIGANTE("Elixir do Gigante", 0xD35400, List.of(e(PotionEffectType.STRENGTH, 180, 1), e(PotionEffectType.HEALTH_BOOST, 300, 0)), 0),
    FENIX("Elixir da Fênix", 0xFF7A1A, List.of(e(PotionEffectType.REGENERATION, 60, 1), e(PotionEffectType.ABSORPTION, 180, 1),
            e(PotionEffectType.FIRE_RESISTANCE, 180, 0)), 0),
    TITA("Elixir do Titã", 0xFFD23F, List.of(e(PotionEffectType.STRENGTH, 240, 0), e(PotionEffectType.RESISTANCE, 240, 0),
            e(PotionEffectType.HEALTH_BOOST, 240, 1), e(PotionEffectType.REGENERATION, 240, 0)), 0),
    PURIFICACAO("Elixir da Purificação", 0xFFFFE0, List.of(e(PotionEffectType.REGENERATION, 15, 0)), 0);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "elixir");

    /** Efeito do elixir: tipo, duração base em segundos (1 = instantâneo) e nível (0 = I). */
    public record Efeito(PotionEffectType tipo, int segundos, int nivel) {}

    private static Efeito e(PotionEffectType t, int seg, int nivel) { return new Efeito(t, seg, nivel); }

    private final String nome;
    private final TextColor cor;
    private final List<Efeito> efeitos;
    private final int mana;

    Elixir(String nome, int cor, List<Efeito> efeitos, int mana) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.efeitos = efeitos;
        this.mana = mana;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public List<Efeito> efeitos() { return efeitos; }
    /** Mana que o elixir devolve ao beber (0 = nenhuma). */
    public int mana() { return mana; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    private static String romano(int n) {
        return switch (n) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "I"; };
    }

    public List<Component> descrever(double fator) {
        List<Component> l = new ArrayList<>();
        for (Efeito ef : efeitos) {
            int seg = (int) Math.round(ef.segundos() * fator);
            String duracao = ef.segundos() <= 1 ? "" : " (" + seg / 60 + ":" + String.format("%02d", seg % 60) + ")";
            l.add(Component.text(" ✧ ", cor).append(Component.translatable(ef.tipo().translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(ef.nivel() > 0 ? " " + romano(ef.nivel() + 1) : "", NamedTextColor.WHITE))
                    .append(Component.text(duracao, NamedTextColor.GRAY)));
        }
        if (mana > 0) l.add(Component.text(" ✧ Recupera " + mana + " de mana", NamedTextColor.LIGHT_PURPLE));
        return l;
    }

    /** O frasco pronto: os efeitos já saem com a duração final gravada. */
    public ItemStack criar(Qualidade q, double fator, String alquimista) {
        ItemStack i = new ItemStack(Material.POTION);
        i.editMeta(PotionMeta.class, m -> {
            m.itemName(Component.text(nome + (q == Qualidade.NORMAL ? "" : " " + q.estrelas()), q == Qualidade.NORMAL ? cor : q.cor()));
            m.setColor(Color.fromRGB(cor.value()));
            for (Efeito ef : efeitos) {
                int ticks = ef.segundos() <= 1 ? 1 : (int) Math.round(ef.segundos() * 20 * fator);
                m.addCustomEffect(new PotionEffect(ef.tipo(), ticks, ef.nivel()), true);
            }
            List<Component> lore = new ArrayList<>();
            if (q != Qualidade.NORMAL) lore.add(Component.text(q.estrelas() + " Qualidade " + q.nome(), q.cor()));
            lore.add(Component.text("Ao beber:", NamedTextColor.GRAY));
            lore.addAll(descrever(fator));
            lore.add(Component.empty());
            lore.add(Component.text("⚗ Feito por " + alquimista, NamedTextColor.DARK_GRAY));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            m.getPersistentDataContainer().set(Qualidade.CHAVE, PersistentDataType.STRING, q.name());
            if (q == Qualidade.PERFEITA) m.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    public static Elixir de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Elixir porId(String id) {
        for (Elixir x : values()) if (x.id().equalsIgnoreCase(id)) return x;
        return null;
    }
}
