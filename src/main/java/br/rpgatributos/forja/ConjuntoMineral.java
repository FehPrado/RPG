package br.rpgatributos.forja;

import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.ArteTrajes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.Repairable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Os conjuntos dos minerais (2.64): espada, picareta, machado, pá, enxada e as 4 peças de armadura
 * feitas com os minerais do mundo, no padrão do jogo (mesma receita, o mineral no lugar do ferro).
 * <ul>
 *   <li>Força entre o ferro e o diamante: a base é a de ferro, com +1 de armadura e +1 de dureza
 *       por peça, +0,5 de dano na espada e 60% mais durabilidade nas ferramentas (50% na armadura).</li>
 *   <li>A armadura aparece no corpo com a cor do mineral (equipamento próprio no pacote).</li>
 *   <li>Bônus com 2 e com 4 peças vestidas ({@code EquipamentoMineral}); duas picaretas têm poder
 *       próprio (Âmbar e Magma).</li>
 *   <li>Conserta na bigorna com o mineral; vai à Forja (sai com raridade) e a Reciclagem devolve o
 *       mineral.</li>
 * </ul>
 * O Mitrilo não entra: ele já tem as peças "✦ Mitrilo" (netherite aprimorada).
 */
public enum ConjuntoMineral implements ArteTrajes.Paleta {
    AMBAR("Âmbar", Raro.AMBAR, 0xF2A33A, Traje.Estilo.PLACAS,
            0xFFE09A2C, 0xFFFFD27A, 0xFF9A5A10, 0xFF6D3A12, 0xFFFFE9A8,
            "Sorte I.", "Sorte II e +10% de chance de minério a mais com qualquer picareta.",
            "Picareta: 10% de chance de o minério soltar um a mais."),
    JADE("Jade", Raro.JADE, 0x4CAF7D, Traje.Estilo.ESCAMAS,
            0xFF3E9B6B, 0xFF8FE0B0, 0xFF1F5E3E, 0xFFD4AF37, 0xFFB9F6CA,
            "+1 ❤.", "+2 ❤ e recupera meio coração a cada 8 segundos.", null),
    PEROLA("Pérola", Raro.PEROLA, 0xF3E9F0, Traje.Estilo.PLACAS,
            0xFFE8E0EC, 0xFFFFFFFF, 0xFFA898B8, 0xFF5FA8C8, 0xFFFFF0F8,
            "Respira debaixo d'água.", "Nada como golfinho e +15% de dano na água ou na chuva.", null),
    GELO("Gelo", Raro.CRISTAL_DE_GELO, 0xA8E4FF, Traje.Estilo.PLACAS,
            0xFF8FD3F0, 0xFFE6F8FF, 0xFF4A90B8, 0xFF2E5E80, 0xFFCFF4FF,
            "Não congela na neve fofa.", "Quem bate em você fica lento.", null),
    PEDRA_SANGUE("Pedra-Sangue", Raro.PEDRA_SANGUE, 0xD9475A, Traje.Estilo.ESCAMAS,
            0xFF8E1426, 0xFFD9475A, 0xFF4A0612, 0xFF2B2B2B, 0xFFFF6B7F,
            "Rouba 5% do dano que você causa em vida.", "Rouba 12%, mas a vida máxima cai 1 ❤.", null),
    MAGMA("Magma", Raro.CRISTAL_DE_MAGMA, 0xFF5A1F, Traje.Estilo.PLACAS,
            0xFFE0541C, 0xFFFFB04A, 0xFF7A1E0A, 0xFF2E2424, 0xFFFFE07A,
            "-30% de dano de fogo e lava.", "Não queima (Resistência ao Fogo).",
            "Picareta: funde o minério (ferro, ouro e cobre saem em lingote)."),
    QUARTZO_NEGRO("Quartzo Negro", Raro.QUARTZO_NEGRO, 0x9A8ABF, Traje.Estilo.PLACAS,
            0xFF4A4060, 0xFF8A7FA8, 0xFF221C30, 0xFFD4AF37, 0xFFB39DDB,
            "Os piglins não atacam você.", "Força I no escuro.", null),
    // 3.3: os minerais do Abismo, um conjunto por camada.
    ANTIGO("Antigo", Raro.COBRE_ANTIGO, 0xC8703A, Traje.Estilo.PLACAS,
            0xFFC8703A, 0xFFF2A36A, 0xFF7A3A14, 0xFF4E9A7E, 0xFFFFD8B0,
            "Queda: −40% de dano.", "Salto I e nenhum dano de queda de até 12 blocos.", null),
    ABISSAL("Abissal", Raro.CRISTAL_ABISSAL, 0x8A5AE0, Traje.Estilo.ESCAMAS,
            0xFF6A3ED0, 0xFFB898FF, 0xFF2A0E5A, 0xFFD4AF37, 0xFFD8C0FF,
            "Visão Noturna no escuro.", "+1 de mana por segundo.", null),
    ESPORO("Esporo", Raro.ESPORO_FOSSIL, 0x3AB0A0, Traje.Estilo.TECIDO,
            0xFF2A8A80, 0xFF8AE8D8, 0xFF0E3A3A, 0xFF5A4A3A, 0xFFB8FFE8,
            "Veneno não pega em você.", "Recupera meio coração a cada 6 segundos.", null),
    FERRO_NEGRO("Ferro Negro", Raro.FERRO_NEGRO, 0x5A5A7E, Traje.Estilo.PLACAS,
            0xFF2A2A38, 0xFF6A6A8A, 0xFF0A0A10, 0xFF8A2A2A, 0xFFB8B8E0,
            "Não é empurrado pelos golpes.", "Resistência I.", null),
    // 3.5: o mineral das Luas.
    LUNAR("Lunar", Raro.PRATA_LUNAR, 0xD8DCE8, Traje.Estilo.PLACAS,
            0xFFC8CCD8, 0xFFFFFFFF, 0xFF6A7080, 0xFF8A5AE0, 0xFFE6F0FF,
            "+25% de dano em mortos-vivos.", "Salto II e +50% de dano em mortos-vivos e lobisomens.", null),
    // 3.6: os minerais das outras luas.
    ESTELAR("Estelar", Raro.ONIX_ESTELAR, 0x3A3A6E, Traje.Estilo.ESCAMAS,
            0xFF1A1A2E, 0xFF5A5AAE, 0xFF000000, 0xFFE6E6FF, 0xFF8A8AFF,
            "Visão Noturna.", "Agachado e parado, você fica invisível.", null),
    SANGUINEO("Sanguíneo", Raro.RUBI_SANGUINEO, 0xE53935, Traje.Estilo.PLACAS,
            0xFFB71C1C, 0xFFFF8A8A, 0xFF4A0008, 0xFFD4AF37, 0xFFFF5252,
            "+10% de dano.", "Cada monstro abatido cura 1 coração.", null),
    CELESTE("Celeste", Raro.CRISTAL_CELESTE, 0x64B5F6, Traje.Estilo.ESCAMAS,
            0xFF2E7AD0, 0xFFE0F4FF, 0xFF0A3A7A, 0xFFFFFFFF, 0xFF90CAF9,
            "Pressa I.", "Pressa II e Sorte I.", null),
    ECLIPSE("Eclipse", Raro.FRAGMENTO_ECLIPSE, 0x8E24AA, Traje.Estilo.PLACAS,
            0xFF4A0A6E, 0xFFCE93D8, 0xFF14001E, 0xFFFFD27F, 0xFFE1BEE7,
            "Murchidão não pega em você.", "Força I e Velocidade I à noite e no eclipse.", null);

    /** As 9 peças, cada uma com a base de ferro do jogo. */
    public enum Peca {
        ESPADA("Espada", Material.IRON_SWORD, false),
        PICARETA("Picareta", Material.IRON_PICKAXE, false),
        MACHADO("Machado", Material.IRON_AXE, false),
        PA("Pá", Material.IRON_SHOVEL, false),
        ENXADA("Enxada", Material.IRON_HOE, false),
        ELMO("Elmo", Material.IRON_HELMET, true),
        PEITORAL("Peitoral", Material.IRON_CHESTPLATE, true),
        GREVAS("Grevas", Material.IRON_LEGGINGS, true),
        BOTAS("Botas", Material.IRON_BOOTS, true);

        public final String nome;
        public final Material base;
        public final boolean armadura;

        Peca(String nome, Material base, boolean armadura) {
            this.nome = nome;
            this.base = base;
            this.armadura = armadura;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }

        public static Peca de(Material m) {
            for (Peca p : values()) if (p.base == m) return p;
            return null;
        }

        /** A peça de Traje equivalente (para o desenho da armadura). */
        public Traje.Peca traje() {
            return switch (this) {
                case ELMO -> Traje.Peca.CAPACETE;
                case PEITORAL -> Traje.Peca.PEITORAL;
                case GREVAS -> Traje.Peca.CALCA;
                case BOTAS -> Traje.Peca.BOTAS;
                default -> null;
            };
        }

        EquipmentSlotGroup grupo() {
            return switch (this) {
                case ELMO -> EquipmentSlotGroup.HEAD;
                case PEITORAL -> EquipmentSlotGroup.CHEST;
                case GREVAS -> EquipmentSlotGroup.LEGS;
                case BOTAS -> EquipmentSlotGroup.FEET;
                default -> EquipmentSlotGroup.MAINHAND;
            };
        }
    }

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "conjunto_mineral");
    private static final NamespacedKey K_FORJADO = new NamespacedKey("rpgatributos", "forja_raridade");
    private static final NamespacedKey K_REFINO = new NamespacedKey("rpgatributos", "forja_refino");
    private static final String MARCA = "◆ ";

    public final String nome;
    public final Raro mineral;
    private final TextColor cor;
    private final Traje.Estilo estilo;
    private final int base, luz, sombra, detalhe, gema;
    public final String bonus2, bonus4, poder;

    ConjuntoMineral(String nome, Raro mineral, int cor, Traje.Estilo estilo, int base, int luz, int sombra, int detalhe, int gema,
                    String bonus2, String bonus4, String poder) {
        this.nome = nome;
        this.mineral = mineral;
        this.cor = TextColor.color(cor);
        this.estilo = estilo;
        this.base = base;
        this.luz = luz;
        this.sombra = sombra;
        this.detalhe = detalhe;
        this.gema = gema;
        this.bonus2 = bonus2;
        this.bonus4 = bonus4;
        this.poder = poder;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public TextColor cor() { return cor; }
    @Override public int base() { return base; }
    @Override public int luz() { return luz; }
    @Override public int sombra() { return sombra; }
    @Override public int detalhe() { return detalhe; }
    @Override public int gema() { return gema; }
    @Override public Traje.Estilo estilo() { return estilo; }

    /** "Conjunto de Jade". */
    public String nomeConjunto() { return "Conjunto de " + nome; }

    /** "Espada de Jade". */
    public String nome(Peca p) { return p.nome + " de " + nome; }

    /** O visual da peça no inventário ("mineral_jade_espada"). */
    public String visual(Peca p) { return "mineral_" + id() + "_" + p.id(); }

    /** O equipamento (a textura no corpo de quem veste). */
    public String equipamento() { return "mineral_" + id(); }

    public NamespacedKey chaveReceita(Peca p) { return new NamespacedKey("rpgatributos", "mineral_" + id() + "_" + p.id()); }

    public static ConjuntoMineral porId(String id) {
        if (id == null) return null;
        for (ConjuntoMineral c : values()) if (c.id().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) return c;
        return null;
    }

    public static ConjuntoMineral de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return porId(i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING));
    }

    public static ConjuntoMineral de(ItemMeta m) {
        return porId(m.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING));
    }

    // =====================================================================
    //  O item
    // =====================================================================

    /** A peça feita na bancada (ainda não forjada). */
    public ItemStack criar(Peca p) {
        ItemStack i = new ItemStack(p.base);
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, id());
            decorar(m, p.base);
        });
        visual(i);
        return i;
    }

    /** Desenho no inventário e no corpo, durabilidade maior e o conserto com o mineral. */
    private void visual(ItemStack i) {
        Peca p = Peca.de(i.getType());
        if (p == null) return;
        i.setData(DataComponentTypes.ITEM_MODEL, Key.key("rpgatributos", visual(p)));
        int max = (int) Math.round(p.base.getMaxDurability() * (p.armadura ? 1.5 : 1.6));
        i.setData(DataComponentTypes.MAX_DAMAGE, max);
        i.setData(DataComponentTypes.REPAIRABLE, Repairable.repairable(RegistrySet.keySet(RegistryKey.ITEM,
                TypedKey.create(RegistryKey.ITEM, mineral.base().getKey()))));
        if (p.armadura) {
            Equippable eq = i.getData(DataComponentTypes.EQUIPPABLE);
            if (eq != null) i.setData(DataComponentTypes.EQUIPPABLE, eq.toBuilder().assetId(Key.key("rpgatributos", equipamento())).build());
        }
    }

    private static NamespacedKey chave(String o, Peca p) {
        return new NamespacedKey("rpgatributos", "mineral_" + o + "_" + p.id());
    }

    /**
     * Nome, descrição e os atributos a mais do mineral. Chamado ao criar e pela Forja depois de
     * refazer o item (que troca nome, descrição e atributos).
     */
    public static void decorar(ItemMeta meta, Material tipo) {
        ConjuntoMineral c = de(meta);
        Peca p = Peca.de(tipo);
        if (c == null || p == null) return;
        boolean forjado = meta.getPersistentDataContainer().has(K_FORJADO);
        boolean bonus = p.armadura || p == Peca.ESPADA;
        if (bonus && !meta.hasAttributeModifiers()) {
            tipo.getDefaultAttributeModifiers().forEach((a, m) -> meta.addAttributeModifier(a, new AttributeModifier(m.getKey(), m.getAmount(),
                    m.getOperation(), m.getSlotGroup() == EquipmentSlotGroup.ANY ? p.grupo() : m.getSlotGroup())));
        }
        if (p.armadura) {
            trocar(meta, Attribute.ARMOR, chave("armadura", p), 1, p.grupo());
            trocar(meta, Attribute.ARMOR_TOUGHNESS, chave("dureza", p), 1, p.grupo());
        } else if (p == Peca.ESPADA) {
            trocar(meta, Attribute.ATTACK_DAMAGE, chave("dano", p), 0.5, p.grupo());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        TextColor corNome = c.cor;
        if (forjado && meta.hasItemName() && meta.itemName().color() != null) corNome = meta.itemName().color();
        Component nome = Component.text(c.nome(p), corNome);
        int refino = meta.getPersistentDataContainer().getOrDefault(K_REFINO, PersistentDataType.INTEGER, 0);
        if (forjado && refino > 0) nome = nome.append(Component.text(" +" + refino, corNome));
        meta.itemName(nome);

        List<Component> lore = new ArrayList<>();
        if (forjado && meta.lore() != null) {
            for (Component l : meta.lore()) {
                String t = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(l);
                if (t.startsWith(MARCA) || t.startsWith(" 2 peças") || t.startsWith(" 4 peças") || t.startsWith("   ") || t.startsWith(" ⛏")) continue;
                lore.add(l);
            }
            lore.add(Component.empty());
        } else {
            lore.add(Component.text(linhaBase(p), NamedTextColor.GRAY));
            lore.add(Component.text("Conserta na bigorna com " + c.mineral.nome() + ".", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
        }
        lore.add(Component.text(MARCA + c.nomeConjunto(), c.cor, TextDecoration.BOLD));
        if (forjado && p.armadura) lore.add(Component.text(MARCA + "+1 armadura e +1 dureza do mineral", NamedTextColor.DARK_GRAY));
        if (forjado && p == Peca.ESPADA) lore.add(Component.text(MARCA + "+0,5 de dano do mineral", NamedTextColor.DARK_GRAY));
        if (p.armadura) {
            lore.addAll(quebrar(" 2 peças: ", c.bonus2));
            lore.addAll(quebrar(" 4 peças: ", c.bonus4));
        } else if (p == Peca.PICARETA && c.poder != null) {
            lore.addAll(quebrar(" ⛏ ", c.poder.replace("Picareta: ", "")));
        } else {
            lore.add(Component.text(MARCA + (p == Peca.ESPADA ? "Mais forte e mais durável que o ferro." : "Dura 60% mais que o ferro."), NamedTextColor.DARK_GRAY));
        }
        meta.lore(lore.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
    }

    private static String linhaBase(Peca p) {
        if (p.armadura) {
            int armadura = 1;
            for (AttributeModifier am : p.base.getDefaultAttributeModifiers().get(Attribute.ARMOR)) armadura += (int) am.getAmount();
            return "🛡 Armadura " + armadura + " · dureza 1";
        }
        double dano = 1 + (p == Peca.ESPADA ? 0.5 : 0);
        for (AttributeModifier am : p.base.getDefaultAttributeModifiers().get(Attribute.ATTACK_DAMAGE)) dano += am.getAmount();
        String d = dano == Math.floor(dano) ? String.valueOf((int) dano) : String.format(Locale.ROOT, "%.1f", dano).replace('.', ',');
        return "⚔ " + d + " de dano · dura 60% mais que o ferro";
    }

    private static void trocar(ItemMeta meta, Attribute a, NamespacedKey k, double valor, EquipmentSlotGroup g) {
        var atuais = meta.getAttributeModifiers(a);
        if (atuais != null) for (AttributeModifier am : new ArrayList<>(atuais)) if (am.getKey().equals(k)) meta.removeAttributeModifier(a, am);
        meta.addAttributeModifier(a, new AttributeModifier(k, valor, AttributeModifier.Operation.ADD_NUMBER, g));
    }

    private static List<Component> quebrar(String prefixo, String texto) {
        List<Component> l = new ArrayList<>();
        StringBuilder linha = new StringBuilder(prefixo);
        for (String s : texto.split(" ")) {
            if (linha.length() + s.length() > 40) {
                l.add(Component.text(linha.toString(), NamedTextColor.GRAY));
                linha = new StringBuilder("   ");
            }
            linha.append(s).append(' ');
        }
        l.add(Component.text(linha.toString().stripTrailing(), NamedTextColor.GRAY));
        return l;
    }

    // =====================================================================
    //  Receitas (as do jogo, com o mineral no lugar do ferro)
    // =====================================================================

    public static void registrarReceitas(Plugin plugin) {
        for (ConjuntoMineral c : values()) {
            RecipeChoice mineral = new RecipeChoice.ExactChoice(c.mineral.criar(1));
            for (Peca p : Peca.values()) {
                ShapedRecipe r = new ShapedRecipe(c.chaveReceita(p), c.criar(p));
                switch (p) {
                    case ESPADA -> r.shape("M", "M", "S");
                    case PICARETA -> r.shape("MMM", " S ", " S ");
                    case MACHADO -> r.shape("MM", "MS", " S");
                    case PA -> r.shape("M", "S", "S");
                    case ENXADA -> r.shape("MM", " S", " S");
                    case ELMO -> r.shape("MMM", "M M");
                    case PEITORAL -> r.shape("M M", "MMM", "MMM");
                    case GREVAS -> r.shape("MMM", "M M", "M M");
                    case BOTAS -> r.shape("M M", "M M");
                }
                r.setIngredient('M', mineral);
                if (!p.armadura) r.setIngredient('S', Material.STICK);
                Bukkit.addRecipe(r);
            }
        }
    }

    /** A receita da peça (a Reciclagem usa para devolver o mineral). */
    public static Recipe receita(ItemStack item) {
        ConjuntoMineral c = de(item);
        Peca p = item == null ? null : Peca.de(item.getType());
        return c == null || p == null ? null : Bukkit.getRecipe(c.chaveReceita(p));
    }

    /** Quantas peças de armadura de cada conjunto estão vestidas. */
    public static java.util.Map<ConjuntoMineral, Integer> vestidos(ItemStack[] armadura) {
        java.util.Map<ConjuntoMineral, Integer> m = new java.util.EnumMap<>(ConjuntoMineral.class);
        for (ItemStack i : armadura) {
            ConjuntoMineral c = de(i);
            if (c != null) m.merge(c, 1, Integer::sum);
        }
        return m;
    }
}
