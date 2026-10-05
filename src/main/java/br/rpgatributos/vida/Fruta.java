package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.mundo.Estacoes;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** As árvores do pomar: cada uma cresce de uma muda e dá fruta em certas estações. */
public enum Fruta {
    LARANJA("Laranja", "Laranjeira", 0xFF9800, Material.OAK_SAPLING, EnumSet.of(Estacoes.Estacao.OUTONO, Estacoes.Estacao.INVERNO),
            "Regeneração por 8 s", new PotionEffect(PotionEffectType.REGENERATION, 160, 0)),
    PESSEGO("Pêssego", "Pessegueiro", 0xFFAB91, Material.BIRCH_SAPLING, EnumSet.of(Estacoes.Estacao.VERAO),
            "Velocidade por 30 s", new PotionEffect(PotionEffectType.SPEED, 600, 0)),
    CEREJA("Cereja", "Cerejeira", 0xD81B60, Material.CHERRY_SAPLING, EnumSet.of(Estacoes.Estacao.PRIMAVERA),
            "Sorte por 1 minuto", new PotionEffect(PotionEffectType.LUCK, 1200, 0)),
    LIMAO("Limão", "Limoeiro", 0xCDDC39, Material.ACACIA_SAPLING, EnumSet.of(Estacoes.Estacao.PRIMAVERA, Estacoes.Estacao.VERAO),
            "Pressa por 30 s", new PotionEffect(PotionEffectType.HASTE, 600, 0)),
    MACA_DOURADA("Maçã Dourada do Pomar", "Macieira Dourada", 0xFFD54F, Material.OAK_SAPLING, EnumSet.of(Estacoes.Estacao.OUTONO),
            "Absorção por 1 minuto", new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));

    public static final NamespacedKey K_FRUTA = new NamespacedKey("rpgatributos", "fruta");
    public static final NamespacedKey K_MUDA = new NamespacedKey("rpgatributos", "muda");

    private final String nome, arvore, descricaoEfeito;
    private final TextColor cor;
    private final Material muda;
    private final Set<Estacoes.Estacao> estacoes;
    private final PotionEffect efeito;

    Fruta(String nome, String arvore, int cor, Material muda, Set<Estacoes.Estacao> estacoes, String descricaoEfeito, PotionEffect efeito) {
        this.nome = nome;
        this.arvore = arvore;
        this.cor = TextColor.color(cor);
        this.muda = muda;
        this.estacoes = estacoes;
        this.descricaoEfeito = descricaoEfeito;
        this.efeito = efeito;
    }

    public String nome() { return nome; }
    public String arvore() { return arvore; }
    public TextColor cor() { return cor; }
    public Set<Estacoes.Estacao> estacoes() { return estacoes; }
    public PotionEffect efeito() { return efeito; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String nomesEstacoes() {
        List<String> l = new ArrayList<>();
        for (Estacoes.Estacao e : estacoes) l.add(e.nome());
        return String.join(" e ", l);
    }

    public ItemStack fruta(int qtd) {
        ItemStack i = new ItemStack(Material.APPLE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text("Ao comer: " + descricaoEfeito + ".", NamedTextColor.GRAY),
                            Component.text("Dá no " + nomesEstacoes() + ".", NamedTextColor.DARK_GRAY),
                            Component.empty(), Component.text("🍊 Fruta do pomar", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (this == MACA_DOURADA) m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_FRUTA, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "fruta_" + id());
        });
        return i;
    }

    public ItemStack muda(int qtd) {
        ItemStack i = new ItemStack(muda, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Muda de " + arvore, cor));
            m.lore(List.of(Component.text("Plante: cresce uma " + arvore + " que dá", NamedTextColor.GRAY),
                            Component.text(nome + " no " + nomesEstacoes() + ".", NamedTextColor.GRAY),
                            Component.text("As frutas aparecem penduradas nas folhas.", NamedTextColor.DARK_GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_MUDA, PersistentDataType.STRING, name());
        });
        return i;
    }

    public static Fruta de(ItemStack i) {
        return ler(i, K_FRUTA);
    }

    public static Fruta daMuda(ItemStack i) {
        return ler(i, K_MUDA);
    }

    private static Fruta ler(ItemStack i, NamespacedKey k) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(k, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Muda aleatória (a dourada é rara). */
    public static Fruta sortear(java.util.Random r) {
        return r.nextDouble() < 0.1 ? MACA_DOURADA : values()[r.nextInt(values().length - 1)];
    }
}
