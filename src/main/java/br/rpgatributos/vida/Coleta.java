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
import java.util.function.Predicate;

/**
 * Coisas para colher pelo mundo: aparecem no chão perto de quem explora, conforme a estação e
 * o bioma, e somem quando a estação acaba. Todas se comem (com um efeito pequeno).
 */
public enum Coleta {
    // ---------- primavera ----------
    MOREL("Cogumelo-Morel", Estacao.PRIMAVERA, 0xB08850, "Regeneração por 10 s", new PotionEffect(PotionEffectType.REGENERATION, 200, 0),
            b -> b.contains("forest") || b.contains("taiga") || b.contains("grove")),
    ALHO_SELVAGEM("Alho-Selvagem", Estacao.PRIMAVERA, 0xF2EEDD, "Resistência por 30 s", new PotionEffect(PotionEffectType.RESISTANCE, 600, 0),
            b -> b.contains("plains") || b.contains("forest") || b.contains("meadow")),
    BROTO_DE_SAMAMBAIA("Broto de Samambaia", Estacao.PRIMAVERA, 0x7CB342, "Velocidade por 30 s", new PotionEffect(PotionEffectType.SPEED, 600, 0),
            b -> b.contains("jungle") || b.contains("forest") || b.contains("swamp") || b.contains("taiga")),
    // ---------- verão ----------
    AMORA_SILVESTRE("Amora-Silvestre", Estacao.VERAO, 0x4A148C, "Velocidade por 20 s", new PotionEffect(PotionEffectType.SPEED, 400, 0),
            b -> b.contains("forest") || b.contains("plains") || b.contains("meadow")),
    ALGA_DOCE("Alga-Doce", Estacao.VERAO, 0x2E7D32, "Fôlego por 30 s", new PotionEffect(PotionEffectType.WATER_BREATHING, 600, 0),
            b -> b.contains("beach") || b.contains("river") || b.contains("shore")),
    GROSELHA("Groselha", Estacao.VERAO, 0xE53935, "Sorte por 1 minuto", new PotionEffect(PotionEffectType.LUCK, 1200, 0),
            b -> b.contains("plains") || b.contains("savanna") || b.contains("meadow") || b.contains("forest")),
    // ---------- outono ----------
    CASTANHA("Castanha", Estacao.OUTONO, 0x6D4C41, "Saciedade", new PotionEffect(PotionEffectType.SATURATION, 4, 0),
            b -> b.contains("forest") || b.contains("taiga")),
    COGUMELO_DO_BOSQUE("Cogumelo-do-Bosque", Estacao.OUTONO, 0xC0702A, "Visão noturna por 30 s", new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0),
            b -> b.contains("forest") || b.contains("taiga") || b.contains("swamp")),
    AVELA("Avelã", Estacao.OUTONO, 0xA1887F, "Pressa por 30 s", new PotionEffect(PotionEffectType.HASTE, 600, 0),
            b -> b.contains("forest") || b.contains("plains") || b.contains("meadow")),
    // ---------- inverno ----------
    RAIZ_DE_INVERNO("Raiz de Inverno", Estacao.INVERNO, 0x8D6E63, "Resistência ao fogo por 1 minuto (esquenta)", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0),
            b -> !b.contains("ocean") && !b.contains("river") && !b.contains("desert")),
    COGUMELO_DE_NEVE("Cogumelo-de-Neve", Estacao.INVERNO, 0xE3F2FD, "Regeneração por 10 s", new PotionEffect(PotionEffectType.REGENERATION, 200, 0),
            b -> b.contains("snowy") || b.contains("taiga") || b.contains("grove") || b.contains("forest")),
    FRUTO_DE_ZIMBRO("Fruto de Zimbro", Estacao.INVERNO, 0x3949AB, "Força por 20 s", new PotionEffect(PotionEffectType.STRENGTH, 400, 0),
            b -> b.contains("taiga") || b.contains("grove") || b.contains("snowy") || b.contains("windswept"));

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "coleta");

    private final String nome, descricaoEfeito;
    private final Estacao estacao;
    private final TextColor cor;
    private final PotionEffect efeito;
    private final Predicate<String> bioma;

    Coleta(String nome, Estacao estacao, int cor, String descricaoEfeito, PotionEffect efeito, Predicate<String> bioma) {
        this.nome = nome;
        this.estacao = estacao;
        this.cor = TextColor.color(cor);
        this.descricaoEfeito = descricaoEfeito;
        this.efeito = efeito;
        this.bioma = bioma;
    }

    public String nome() { return nome; }
    public Estacao estacao() { return estacao; }
    public TextColor cor() { return cor; }
    public PotionEffect efeito() { return efeito; }
    public boolean aceita(String b) { return bioma.test(b); }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(Material.APPLE, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            m.lore(List.of(Component.text("Ao comer: " + descricaoEfeito + ".", NamedTextColor.GRAY),
                            Component.text("Aparece pelo mundo na " + estacao.nome().toLowerCase(Locale.ROOT) + ".", NamedTextColor.DARK_GRAY),
                            Component.empty(), Component.text(estacao.icone() + " Coleta da estação", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "coleta_" + id());
        });
        return i;
    }

    public static Coleta de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Coleta porId(String id) {
        for (Coleta c : values()) if (c.id().equalsIgnoreCase(id)) return c;
        return null;
    }
}
