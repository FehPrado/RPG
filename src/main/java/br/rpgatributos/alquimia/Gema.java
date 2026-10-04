package br.rpgatributos.alquimia;

import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Raridade;
import br.rpgatributos.forja.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;

/**
 * Gemas: feitas na Bancada Alquímica e engastadas em itens forjados. Cada gema dá um bônus
 * diferente conforme o item (arma, arco, armadura ou ferramenta). Três graus: Bruta,
 * Lapidada e Perfeita (3 de um grau viram 1 do próximo).
 */
public enum Gema {
    //       nome         modelo           cor      fem.   corpo a corpo                          distância                               armadura                               ferramenta
    RUBI("Rubi", "redstone", 0xE0115F, false,
            e(Stat.DANO, 0.75, 1.5, 2.5), e(Stat.DANO_FLECHA, 0.04, 0.08, 0.12),
            e(Stat.VIDA, 1, 2, 3.5), e(Stat.VEL_MINERACAO, 0.05, 0.1, 0.16)),
    SAFIRA("Safira", "lapis_lazuli", 0x3D6BFF, true,
            e(Stat.CRITICO, 0.03, 0.05, 0.08), e(Stat.CRITICO, 0.03, 0.05, 0.08),
            e(Stat.ARMADURA, 0.5, 1, 1.5), e(Stat.EFICIENCIA, 1, 2, 3.5)),
    ESMERALDA("Esmeralda", "emerald", 0x2ECC71, true,
            e(Stat.ROUBO_VIDA, 0.015, 0.03, 0.05), e(Stat.ROUBO_VIDA, 0.015, 0.03, 0.05),
            e(Stat.ESQUIVA, 0.01, 0.02, 0.03), e(Stat.SORTE, 0.5, 1, 1.5)),
    TOPAZIO("Topázio", "gold_nugget", 0xFFC93C, false,
            e(Stat.VEL_ATAQUE, 0.08, 0.15, 0.25), e(Stat.INQUEBRAVEL, 0.08, 0.15, 0.25),
            e(Stat.VELOCIDADE, 0.015, 0.03, 0.045), e(Stat.INQUEBRAVEL, 0.08, 0.15, 0.25)),
    AMETISTA("Ametista", "amethyst_shard", 0xB66BFF, true,
            e(Stat.ALCANCE, 0.2, 0.4, 0.6), e(Stat.DANO_FLECHA, 0.03, 0.06, 0.1),
            e(Stat.RESISTENCIA, 0.5, 1, 1.5), e(Stat.ALCANCE_BLOCO, 0.5, 1, 1.5)),
    ONIX("Ônix", "coal", 0x5A5A6E, false,
            e(Stat.REPULSAO, 0.15, 0.3, 0.5), e(Stat.INQUEBRAVEL, 0.08, 0.15, 0.25),
            e(Stat.ESPINHOS, 0.04, 0.07, 0.1), null);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "gema");
    public static final NamespacedKey CHAVE_GRAU = new NamespacedKey("rpgatributos", "gema_grau");
    public static final int GRAU_MAXIMO = 3;

    /** Bônus de uma gema num tipo de item: o atributo e o valor nos graus I, II e III. */
    public record Bonus(Stat stat, double[] valores) {
        public double valor(int grau) { return valores[Math.max(1, Math.min(GRAU_MAXIMO, grau)) - 1]; }
    }

    private static Bonus e(Stat s, double a, double b, double c) {
        return new Bonus(s, new double[]{a, b, c});
    }

    private final String nome;
    private final NamespacedKey modelo;
    private final TextColor cor;
    private final boolean feminino;
    private final Bonus corpo, distancia, armadura, ferramenta;

    Gema(String nome, String modelo, int cor, boolean feminino, Bonus corpo, Bonus distancia, Bonus armadura, Bonus ferramenta) {
        this.nome = nome;
        this.modelo = NamespacedKey.minecraft(modelo);
        this.cor = TextColor.color(cor);
        this.feminino = feminino;
        this.corpo = corpo;
        this.distancia = distancia;
        this.armadura = armadura;
        this.ferramenta = ferramenta;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** "Rubi Lapidado", "Safira Perfeita"... */
    public String nome(int grau) {
        String g = switch (grau) {
            case 2 -> feminino ? "Lapidada" : "Lapidado";
            case 3 -> feminino ? "Perfeita" : "Perfeito";
            default -> feminino ? "Bruta" : "Bruto";
        };
        return nome + " " + g;
    }

    /** O bônus dessa gema num item da categoria (null = não serve nesse item). */
    public Bonus bonus(Categoria cat) {
        return switch (cat.tipo()) {
            case CORPO_A_CORPO -> corpo;
            case DISTANCIA -> distancia;
            case ARMADURA, ESCUDO -> armadura;
            case FERRAMENTA -> ferramenta;
            case MAGICO -> null; // no cajado a gema fortalece elementos (veja elementos())
            case PESCA, VOO, MONTARIA, VEICULO -> null;
        };
    }

    /** Elementos que a gema fortalece quando engastada num cajado. */
    public Set<Essencia> elementos() {
        return switch (this) {
            case RUBI -> EnumSet.of(Essencia.FOGO);
            case SAFIRA -> EnumSet.of(Essencia.AGUA, Essencia.GELO);
            case ESMERALDA -> EnumSet.of(Essencia.NATUREZA, Essencia.VIDA);
            case TOPAZIO -> EnumSet.of(Essencia.ENERGIA, Essencia.VENTO);
            case AMETISTA -> EnumSet.of(Essencia.VAZIO);
            case ONIX -> EnumSet.of(Essencia.SOMBRA, Essencia.TERRA);
        };
    }

    /** Potência a mais nas magias desses elementos (0,1 = +10%). */
    public double potenciaElemental(int grau) {
        return switch (Math.max(1, Math.min(GRAU_MAXIMO, grau))) { case 2 -> 0.18; case 3 -> 0.3; default -> 0.1; };
    }

    /** "Fogo" ou "Água e Gelo". */
    public String nomesElementos() {
        List<String> n = new ArrayList<>();
        for (Essencia e : elementos()) n.add(e.nome());
        return String.join(" e ", n);
    }

    /** Quantos engastes um item forjado tem, conforme a raridade. */
    public static int engastes(Raridade r) {
        return switch (r) {
            case COMUM -> 0;
            case RARO, EPICO -> 1;
            case UNICO, LENDARIO -> 2;
            case MITICO -> 3;
        };
    }

    public static String romano(int grau) {
        return switch (grau) { case 2 -> "II"; case 3 -> "III"; default -> "I"; };
    }

    private static Component linha(String onde, Bonus b, int grau) {
        if (b == null) return Component.text(" " + onde + ": ", NamedTextColor.GRAY).append(Component.text("—", NamedTextColor.DARK_GRAY));
        return Component.text(" " + onde + ": ", NamedTextColor.GRAY)
                .append(Component.text(b.stat().formatar(b.valor(grau)) + " " + b.stat().nome(), NamedTextColor.WHITE));
    }

    public List<Component> descricao(int grau) {
        List<Component> l = new ArrayList<>();
        l.add(Component.text("Engaste num item forjado:", NamedTextColor.GRAY));
        l.add(linha("Arma", corpo, grau));
        l.add(linha("Arco/besta", distancia, grau));
        l.add(linha("Armadura/escudo", armadura, grau));
        l.add(linha("Ferramenta", ferramenta, grau));
        l.add(Component.text(" Cajado: ", NamedTextColor.GRAY).append(Component.text("+" + Math.round(potenciaElemental(grau) * 100)
                + "% magias de " + nomesElementos(), NamedTextColor.LIGHT_PURPLE)));
        return l;
    }

    public ItemStack criar(int grau, int qtd) {
        int g = Math.max(1, Math.min(GRAU_MAXIMO, grau));
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome(g) + " " + "◆".repeat(g), cor));
            m.setItemModel(modelo);
            List<Component> lore = new ArrayList<>(descricao(g));
            lore.add(Component.empty());
            lore.add(Component.text("Engaste na Bancada Alquímica.", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("◆ Gema grau " + romano(g), cor));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (g == GRAU_MAXIMO) m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            m.getPersistentDataContainer().set(CHAVE_GRAU, PersistentDataType.INTEGER, g);
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "gema_" + name().toLowerCase(java.util.Locale.ROOT) + "_" + g);
        });
        return i;
    }

    public static Gema de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String n = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (n == null) return null;
        try {
            return valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int grau(ItemStack item) {
        if (item == null || item.isEmpty()) return 0;
        Integer g = item.getPersistentDataContainer().get(CHAVE_GRAU, PersistentDataType.INTEGER);
        return g == null ? 1 : g;
    }

    public static Gema porId(String id) {
        for (Gema g : values()) if (g.id().equalsIgnoreCase(id)) return g;
        return null;
    }
}
