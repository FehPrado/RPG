package br.rpgatributos.vida;

import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.mundo.Estacoes.Estacao;

import static br.rpgatributos.mundo.Estacoes.Estacao.INVERNO;
import static br.rpgatributos.mundo.Estacoes.Estacao.OUTONO;
import static br.rpgatributos.mundo.Estacoes.Estacao.PRIMAVERA;
import static br.rpgatributos.mundo.Estacoes.Estacao.VERAO;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * As plantações da estação: cada uma só cresce na sua estação, em terra arada molhada,
 * em 4 fases desenhadas. Algumas rebrotam depois da colheita.
 */
public enum Cultivo {
    // ---------- Primavera ----------
    MORANGO("Morango", e(PRIMAVERA), true, 12, false, 0xE53935, "Velocidade por 30 s", new PotionEffect(PotionEffectType.SPEED, 600, 0)),
    COUVE_FLOR("Couve-Flor", e(PRIMAVERA), false, 12, true, 0xF5F0E0, "Regeneração por 10 s", new PotionEffect(PotionEffectType.REGENERATION, 200, 0)),
    PASTINACA("Pastinaca", e(PRIMAVERA), false, 6, false, 0xF3E5AB, "Sacia mais", new PotionEffect(PotionEffectType.SATURATION, 40, 0)),
    ALHO("Alho", e(PRIMAVERA), false, 8, false, 0xF8F4E8, "Resistência por 20 s", new PotionEffect(PotionEffectType.RESISTANCE, 400, 0)),
    FEIJAO_VERDE("Feijão-Verde", e(PRIMAVERA), true, 10, false, 0x7CB342, "Pressa por 30 s", new PotionEffect(PotionEffectType.HASTE, 600, 0)),
    // ---------- Verão ----------
    MIRTILO("Mirtilo", e(VERAO), true, 12, false, 0x3F51B5, "Visão noturna por 1 minuto", new PotionEffect(PotionEffectType.NIGHT_VISION, 1200, 0)),
    PIMENTA("Pimenta", e(VERAO), false, 12, false, 0xD32F2F, "Força por 20 s", new PotionEffect(PotionEffectType.STRENGTH, 400, 0)),
    TOMATE("Tomate", e(VERAO), true, 11, false, 0xE53935, "Regeneração por 10 s", new PotionEffect(PotionEffectType.REGENERATION, 200, 0)),
    MILHO("Milho", e(VERAO, OUTONO), true, 14, false, 0xFBC02D, "Sacia mais", new PotionEffect(PotionEffectType.SATURATION, 60, 0)),
    MELAO("Melão", e(VERAO), false, 14, true, 0x8BC34A, "Resistência ao fogo por 30 s (refresca no calor)", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0)),
    // ---------- Outono ----------
    UVA("Uva", e(OUTONO), true, 12, false, 0x7B1FA2, "Sorte por 1 minuto", new PotionEffect(PotionEffectType.LUCK, 1200, 0)),
    ABOBORA_MORANGA("Abóbora-Moranga", e(OUTONO), false, 12, true, 0xEF6C00, "Absorção por 30 s", new PotionEffect(PotionEffectType.ABSORPTION, 600, 0)),
    BERINJELA("Berinjela", e(OUTONO), true, 10, false, 0x6A1B9A, "Força por 15 s", new PotionEffect(PotionEffectType.STRENGTH, 300, 0)),
    INHAME("Inhame", e(OUTONO), false, 12, false, 0x8D6E63, "Absorção por 30 s", new PotionEffect(PotionEffectType.ABSORPTION, 600, 0)),
    OXICOCO("Oxicoco", e(OUTONO), true, 13, false, 0xB71C1C, "Sorte por 1 minuto", new PotionEffect(PotionEffectType.LUCK, 1200, 0)),
    // ---------- Inverno ----------
    COUVE_GELADA("Couve Gelada", e(INVERNO), false, 12, false, 0x5E9C7A, "Resistência por 30 s", new PotionEffect(PotionEffectType.RESISTANCE, 600, 0)),
    NABO_DE_NEVE("Nabo-de-Neve", e(INVERNO), false, 12, false, 0xEDE7F6, "Resistência ao fogo por 1 minuto (esquenta)", new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0)),
    CEBOLA("Cebola", e(INVERNO, PRIMAVERA), false, 10, false, 0xAD5A8C, "Visão noturna por 30 s", new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0)),
    RABANETE_GELADO("Rabanete-Gelado", e(INVERNO), false, 7, false, 0xEF5350, "Velocidade por 20 s", new PotionEffect(PotionEffectType.SPEED, 400, 0)),
    ALHO_PORO("Alho-Poró", e(INVERNO), false, 11, false, 0xC5E1A5, "Regeneração por 15 s", new PotionEffect(PotionEffectType.REGENERATION, 300, 0));

    public static final NamespacedKey K_SEMENTE = new NamespacedKey("rpgatributos", "semente_estacao");
    public static final NamespacedKey K_COLHEITA = new NamespacedKey("rpgatributos", "colheita_estacao");

    private final String nome, descricaoEfeito;
    private final EnumSet<Estacao> estacoes;
    private final boolean rebrota, gigante;
    /** Minutos, em média, para passar de uma fase para a outra (com a terra molhada). */
    private final int minutosPorFase;
    private final TextColor cor;
    private final PotionEffect efeito;

    Cultivo(String nome, EnumSet<Estacao> estacoes, boolean rebrota, int minutosPorFase, boolean gigante, int cor, String descricaoEfeito,
            PotionEffect efeito) {
        this.nome = nome;
        this.estacoes = estacoes;
        this.rebrota = rebrota;
        this.minutosPorFase = minutosPorFase;
        this.gigante = gigante;
        this.cor = TextColor.color(cor);
        this.descricaoEfeito = descricaoEfeito;
        this.efeito = efeito;
    }

    private static EnumSet<Estacao> e(Estacao primeira, Estacao... outras) { return EnumSet.of(primeira, outras); }

    public String nome() { return nome; }
    /** A estação principal (a primeira em que cresce). */
    public Estacao estacao() { return estacoes.iterator().next(); }
    /** Cresce nessa estação? (o milho cresce no verão e no outono, a cebola no inverno e na primavera) */
    public boolean cresceEm(Estacao e) { return estacoes.contains(e); }
    public java.util.Set<Estacao> estacoes() { return java.util.Collections.unmodifiableSet(estacoes); }
    public boolean rebrota() { return rebrota; }
    /** Nove maduras num quadrado 3×3 podem virar uma colheita gigante. */
    public boolean gigante() { return gigante; }
    public int minutosPorFase() { return minutosPorFase; }
    public TextColor cor() { return cor; }
    public PotionEffect efeito() { return efeito; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** "na primavera", "no verão e no outono"... */
    public String quando() {
        List<String> l = new java.util.ArrayList<>();
        for (Estacao e : estacoes) l.add((e == Estacao.PRIMAVERA ? "na " : "no ") + e.nome().toLowerCase(Locale.ROOT));
        return String.join(" e ", l);
    }

    public ItemStack semente(int qtd) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Sementes de " + nome, cor));
            m.setItemModel(NamespacedKey.minecraft("wheat_seeds"));
            m.lore(List.of(Component.text("Plante em terra arada (clique com elas).", NamedTextColor.GRAY),
                            Component.text("Só cresce " + quando() + ", com a terra molhada.", NamedTextColor.GRAY),
                            Component.text(rebrota ? "Rebrota depois de colher." : "Colha e plante de novo.", NamedTextColor.DARK_GRAY),
                            Component.text(velocidade() + ".", NamedTextColor.DARK_GRAY),
                            Component.text(gigante ? "9 juntas (3×3) podem virar uma gigante!" : "Na estufa (teto de vidro) cresce o ano todo.", NamedTextColor.DARK_GRAY),
                            Component.empty(), Component.text(estacao().icone() + " Semente da estação", cor))
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
                            Component.empty(), Component.text(estacao().icone() + " Plantação da estação", cor))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_COLHEITA, PersistentDataType.STRING, name());
            PacoteRecursos.marcar(m, "colheita_" + id());
        });
        return i;
    }

    private String velocidade() {
        return minutosPorFase <= 8 ? "Cresce rápido" : minutosPorFase >= 14 ? "Demora a crescer" : "Cresce no tempo normal";
    }

    /** A planta murcha (morreu fora de época). */
    public static ItemStack murcha() {
        ItemStack i = new ItemStack(Material.DEAD_BUSH);
        i.editMeta(m -> PacoteRecursos.marcar(m, "planta_murcha"));
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
