package br.rpgatributos.pesca;

import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Peixes raros: só mordem em certos lugares, horários ou climas. Cada um é ingrediente
 * de um elixir, componente ou acessório da Alquimia (e alguns, de pratos da Cozinha).
 */
public enum PeixeRaro {
    CARPA_DOURADA("Carpa Dourada", Material.COD, 0xFFC93C, 0, 10, 15,
            "Rios e lagos, longe do mar.", "Ingrediente do Elixir da Sorte.",
            c -> !c.oceano()),
    TRUTA_ARCO_IRIS("Truta Arco-Íris", Material.SALMON, 0xFF8FB1, 0, 10, 15,
            "Rios, durante o dia.", "Ingrediente do Elixir da Rapidez.",
            c -> c.rio() && !c.noite()),
    PEIXE_LUA("Peixe-Lua", Material.TROPICAL_FISH, 0xC9D6FF, 0.05, 8, 18,
            "Qualquer água, de noite.", "Destilado vira Sal Lunar.",
            PeixeRaro.Contexto::noite),
    PEIXE_PEDRA("Peixe-Pedra", Material.COD, 0x8C8C8C, 0.1, 8, 20,
            "Águas de caverna (bem lá embaixo).", "Ingrediente do Elixir do Minerador.",
            c -> c.y() < 45),
    ENGUIA_ELETRICA("Enguia Elétrica", Material.COD, 0xFFF04D, 0.15, 7, 25,
            "Qualquer água, debaixo de chuva.", "Destilada vira Mercúrio Vivo.",
            PeixeRaro.Contexto::chuva),
    PEIXE_GELO("Peixe-Gelo", Material.COD, 0xA8E8FF, 0.2, 7, 25,
            "Águas geladas (biomas de neve).", "Ingrediente do Elixir de Pedra.",
            c -> c.temperatura() < 0.2),
    KOI_CELESTE("Koi Celeste", Material.TROPICAL_FISH, 0xFFB7D5, 0.25, 6, 30,
            "Bosques de cerejeira e campos de flores.", "Ingrediente do Anel da Fortuna.",
            c -> c.bioma().contains("cherry") || c.bioma().contains("flower") || c.bioma().contains("meadow")),
    BAIACU_REI("Baiacu-Rei", Material.PUFFERFISH, 0xFFD23F, 0.3, 6, 30,
            "Oceanos mornos ou quentes.", "Ingrediente do Elixir do Gigante.",
            c -> c.oceano() && (c.bioma().contains("warm") || c.bioma().contains("lukewarm"))),
    SALMAO_REI("Salmão-Rei", Material.SALMON, 0xFF6F3C, 0.35, 6, 35,
            "Rios e oceanos frios.", "Ingrediente do Sushi Real (Cozinha).",
            c -> c.rio() || (c.oceano() && c.bioma().contains("cold"))),
    PEIXE_FANTASMA("Peixe-Fantasma", Material.SALMON, 0xB8C7C9, 0.45, 5, 40,
            "Pântanos e manguezais, de noite.", "Ingrediente do Elixir das Sombras.",
            c -> c.noite() && (c.bioma().contains("swamp") || c.bioma().contains("mangrove"))),
    PEIXE_ABISSAL("Peixe Abissal", Material.COD, 0x3B2E7E, 0.6, 4, 60,
            "Oceanos profundos, de noite.", "Ingrediente da Caldeirada Abissal e do Elixir do Titã.",
            c -> c.noite() && c.bioma().contains("deep")),
    PEIXE_DRAGAO("Peixe-Dragão", Material.TROPICAL_FISH, 0xFF4058, 0.85, 1, 150,
            "Oceano, no meio de uma tempestade.", "Coração do Amuleto da Fênix.",
            c -> c.oceano() && c.tempestade());

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "peixe_raro");

    /** Onde e quando o anzol estava na hora da mordida. */
    public record Contexto(String bioma, double temperatura, int y, boolean noite, boolean chuva, boolean tempestade) {
        public boolean oceano() { return bioma.contains("ocean"); }
        public boolean rio() { return bioma.contains("river"); }
    }

    private final String nome;
    private final Material base;
    private final TextColor cor;
    private final double nivel;
    private final int peso;
    private final double xp;
    private final String onde;
    private final String uso;
    private final Predicate<Contexto> condicao;

    PeixeRaro(String nome, Material base, int cor, double nivel, int peso, double xp, String onde, String uso,
              Predicate<Contexto> condicao) {
        this.nome = nome;
        this.base = base;
        this.cor = TextColor.color(cor);
        this.nivel = nivel;
        this.peso = peso;
        this.xp = xp;
        this.onde = onde;
        this.uso = uso;
        this.condicao = condicao;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public Material base() { return base; }
    /** Nível de Pesca para poder fisgar (fração do nível máximo). */
    public int nivelNecessario(int nivelMaximo) { return (int) Math.ceil(nivel * nivelMaximo); }
    /** Peso no sorteio entre os peixes possíveis (maior = mais comum). */
    public int peso() { return peso; }
    public double xp() { return xp; }
    public String onde() { return onde; }
    public String uso() { return uso; }
    public boolean pode(Contexto c) { return condicao.test(c); }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public ItemStack criar(int qtd, Qualidade q) {
        ItemStack i = new ItemStack(base, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Peixe raro.", NamedTextColor.GRAY));
            lore.add(Component.text(uso, NamedTextColor.DARK_AQUA));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
        });
        if (q != null) q.aplicar(i);
        return i;
    }

    public static PeixeRaro de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static PeixeRaro porId(String id) {
        for (PeixeRaro p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
