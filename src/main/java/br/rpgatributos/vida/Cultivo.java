package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.mundo.Estacoes.Estacao;
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

/**
 * As plantações da estação: cada uma só cresce na sua estação, em terra arada molhada,
 * em 4 fases desenhadas. Algumas rebrotam depois da colheita.
 */
public enum Cultivo {
    MORANGO("Morango", Estacao.PRIMAVERA, true, 0xE53935, "Velocidade por 30 s", new PotionEffect(PotionEffectType.SPEED, 600, 0)),
    COUVE_FLOR("Couve-Flor", Estacao.PRIMAVERA, false, 0xF5F0E0, "Regeneração por 10 s", new PotionEffect(PotionEffectType.REGENERATION, 200, 0)),
    MIRTILO("Mirtilo", Estacao.VERAO, true, 0x3F51B5, "Visão noturna por 1 minuto", new PotionEffect(PotionEffectType.NIGHT_VISION, 1200, 0)),
    PIMENTA("Pimenta", Estacao.VERAO, false, 0xD32F2F, "Força por 20 s", new PotionEffect(PotionEffectType.STRENGTH, 400, 0)),
    UVA("Uva", Estacao.OUTONO, true, 0x7B1FA2, "Sorte por 1 minuto", new PotionEffect(PotionEffectType.LUCK, 1200, 0)),
    ABOBORA_MORANGA("Abóbora-Moranga", Estacao.OUTONO, false, 0xEF6C00, "Absorção por 30 s", new PotionEffect(PotionEffectType.ABSORPTION, 600, 0)),
    COUVE_GELADA("Couve Gelada", Estacao.INVERNO, false, 0x5E9C7A, "Resistência por 30 s", new PotionEffect(PotionEffectType.RESISTANCE, 600, 0)),
    NABO_DE_NEVE("Nabo-de-Neve", Estacao.INVERNO, false, 0xEDE7F6, "Resistência ao fogo por 1 minuto (esquenta)", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));

    public static final NamespacedKey K_SEMENTE = new NamespacedKey("rpgatributos", "semente_estacao");
    public static final NamespacedKey K_COLHEITA = new NamespacedKey("rpgatributos", "colheita_estacao");

    private final String nome, descricaoEfeito;
    private final Estacao estacao;
    private final boolean rebrota;
    private final TextColor cor;
    private final PotionEffect efeito;

    Cultivo(String nome, Estacao estacao, boolean rebrota, int cor, String descricaoEfeito, PotionEffect efeito) {
        this.nome = nome;
        this.estacao = estacao;
        this.rebrota = rebrota;
        this.cor = TextColor.color(cor);
        this.descricaoEfeito = descricaoEfeito;
        this.efeito = efeito;
    }

    public String nome() { return nome; }
    public Estacao estacao() { return estacao; }
    public boolean rebrota() { return rebrota; }
    public TextColor cor() { return cor; }
    public PotionEffect efeito() { return efeito; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    private String quando() {
        return "na " + estacao.nome().toLowerCase(Locale.ROOT);
    }

    public ItemStack semente(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Sementes de " + nome, cor));
            m.setItemModel(NamespacedKey.minecraft("wheat_seeds"));
            m.lore(List.of(Component.text("Plante em terra arada (clique com elas).", NamedTextColor.GRAY),
                            Component.text("Só cresce " + quando() + ", com a terra molhada.", NamedTextColor.GRAY),
                            Component.text(rebrota ? "Rebrota depois de colher." : "Colha e plante de novo.", NamedTextColor.DARK_GRAY),
                            Component.empty(), Component.text(estacao.icone() + " Semente da estação", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_SEMENTE, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "semente_" + id());
        });
        return i;
    }

    public ItemStack colheita(int qtd) {
        ItemStack i = new ItemStack(Material.APPLE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text("Ao comer: " + descricaoEfeito + ".", NamedTextColor.GRAY),
                            Component.text("Colhido " + quando() + ".", NamedTextColor.DARK_GRAY),
                            Component.empty(), Component.text(estacao.icone() + " Plantação da estação", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_COLHEITA, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "colheita_" + id());
        });
        return i;
    }

    /** O que aparece em cima da terra em cada fase (0 a 3). Sem o pacote, parece capim. */
    public ItemStack fase(int fase) {
        ItemStack i = new ItemStack(Material.SHORT_GRASS);
        i.editMeta(m -> PacoteRecursos.marcar(m, "planta_" + id() + "_" + fase));
        return i;
    }

    public static Cultivo daSemente(ItemStack i) {
        return ler(i, K_SEMENTE);
    }

    public static Cultivo daColheita(ItemStack i) {
        return ler(i, K_COLHEITA);
    }

    private static Cultivo ler(ItemStack i, NamespacedKey k) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(k, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Cultivo porId(String id) {
        for (Cultivo c : values()) if (c.id().equalsIgnoreCase(id)) return c;
        return null;
    }
}
