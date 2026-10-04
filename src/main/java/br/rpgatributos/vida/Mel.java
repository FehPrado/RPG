package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

/** Méis da Colmeia do Apicultor: cada um nasce de um jeito e dá um efeito ao beber. */
public enum Mel {
    SILVESTRE("Mel Silvestre", 0xE8A33D, "O mel de todo dia.", new PotionEffect(PotionEffectType.REGENERATION, 100, 0)),
    DOURADO("Mel Dourado", 0xFFD54F, "Nasce com girassóis por perto. Absorção II.", new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1)),
    GELADO("Mel Gelado", 0xB3E5FC, "Nasce no frio ou no inverno. Resistência ao fogo.", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 2400, 0)),
    NOTURNO("Mel Noturno", 0x5C6BC0, "Nasce à noite. Visão noturna.", new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0)),
    FLORAL("Mel Floral", 0xF48FB1, "Nasce com 5 flores diferentes por perto. Regeneração.", new PotionEffect(PotionEffectType.REGENERATION, 300, 0));

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "mel");

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
        ItemStack i = new ItemStack(Material.HONEY_BOTTLE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text(descricao, NamedTextColor.GRAY), Component.empty(),
                            Component.text("🐝 Da Colmeia do Apicultor", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "mel_" + id());
        });
        return i;
    }

    public static Mel de(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
