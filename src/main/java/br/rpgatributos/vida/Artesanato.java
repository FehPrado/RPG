package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.fazenda.Qualidade;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * O que sai do Tacho do Artesão (como as máquinas de Stardew Valley): queijo do leite, geleia das
 * frutas, conserva dos legumes, maionese do ovo e óleo da trufa. Guarda a qualidade do que entrou.
 */
public enum Artesanato {
    QUEIJO("Queijo", Material.BREAD, 0xFBC02D, 20, "Absorção e saciedade",
            new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0), new PotionEffect(PotionEffectType.SATURATION, 40, 0)),
    QUEIJO_DE_CABRA("Queijo de Cabra", Material.BREAD, 0xF5F0E0, 25, "Absorção II e saciedade",
            new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1), new PotionEffect(PotionEffectType.SATURATION, 40, 0)),
    MAIONESE("Maionese", Material.HONEY_BOTTLE, 0xFFF3C4, 5, "Saciedade",
            new PotionEffect(PotionEffectType.SATURATION, 80, 0)),
    GELEIA("Geleia", Material.HONEY_BOTTLE, 0xC2185B, 15, "Velocidade e cura lenta",
            new PotionEffect(PotionEffectType.SPEED, 900, 0), new PotionEffect(PotionEffectType.REGENERATION, 300, 0)),
    CONSERVA("Conserva", Material.HONEY_BOTTLE, 0x689F38, 15, "Resistência",
            new PotionEffect(PotionEffectType.RESISTANCE, 900, 0)),
    OLEO_DE_TRUFA("Óleo de Trufa", Material.HONEY_BOTTLE, 0xD4A017, 10, "Sorte e pressa",
            new PotionEffect(PotionEffectType.LUCK, 3600, 0), new PotionEffect(PotionEffectType.HASTE, 1200, 0));

    public static final NamespacedKey K_TIPO = new NamespacedKey("rpgatributos", "artesanato");
    public static final NamespacedKey K_ORIGEM = new NamespacedKey("rpgatributos", "artesanato_origem");

    private final String nome, descricao;
    private final Material base;
    private final TextColor cor;
    /** Minutos (reais) no Tacho. */
    private final int minutos;
    private final List<PotionEffect> efeitos;

    Artesanato(String nome, Material base, int cor, int minutos, String descricao, PotionEffect... efeitos) {
        this.nome = nome;
        this.base = base;
        this.cor = TextColor.color(cor);
        this.minutos = minutos;
        this.descricao = descricao;
        this.efeitos = List.of(efeitos);
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public int minutos() { return minutos; }
    public List<PotionEffect> efeitos() { return efeitos; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Geleia e conserva levam o nome do que entrou ("Geleia de Morango"). */
    public String nomeCom(String origem) {
        return origem == null || origem.isEmpty() ? nome : nome + " de " + origem;
    }

    public ItemStack criar(String origem, Qualidade q, int qtd) {
        ItemStack i = new ItemStack(base, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nomeCom(origem), cor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Ao comer: " + descricao + ".", NamedTextColor.GRAY));
            lore.add(Component.text("Feito no Tacho do Artesão.", NamedTextColor.DARK_GRAY));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_TIPO, PersistentDataType.STRING, name());
            if (origem != null) m.getPersistentDataContainer().set(K_ORIGEM, PersistentDataType.STRING, origem);
            PacoteRecursos.marcar(m, "artesao_" + id());
        });
        if (q != null && q != Qualidade.NORMAL) q.aplicar(i);
        return i;
    }

    public static Artesanato de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_TIPO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
