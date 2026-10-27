package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Uma receita do Altar da Convergência: o catalisador vai na mão de quem acende o altar,
 * e cada ingrediente fica num pedestal diferente.
 *
 * @param desperto o chefe que a cerimônia desperta (invocação maior), ou null se dá um item
 * @param unica    relíquia que cada jogador só faz uma vez
 * @param extra    o resto (sacrifício, efeito no mundo, nível do altar, ritual de grupo...)
 */
public record ReceitaRitual(String nome, Categoria categoria, Material icone,
                            Predicate<ItemStack> catalisador, String nomeCatalisador, int qtdCatalisador,
                            List<Ingrediente> pedestais, Function<ItemStack, ItemStack> resultado,
                            Function<ItemStack, String> impedimento, String descricao,
                            Chefe desperto, boolean unica, Extra extra) {

    public enum Categoria {
        TRANSMUTAR("Transmutar", Material.GOLD_INGOT, TextColor.color(0xFFC14D)),
        INFUNDIR("Infundir", Material.ENCHANTED_BOOK, TextColor.color(0xC77DFF)),
        RARO("Raros e únicos", Material.NETHER_STAR, TextColor.color(0x4FD3E3)),
        INVOCAR("Invocações maiores", Material.WITHER_SKELETON_SKULL, TextColor.color(0xFF5252)),
        GUARDIAO("Guardiões e ajudantes", Material.CARVED_PUMPKIN, TextColor.color(0x7FD8FF)),
        MUNDO("Céu, território e altar", Material.CLOCK, TextColor.color(0x9CCC65));

        public final String nome;
        public final Material icone;
        public final TextColor cor;

        Categoria(String nome, Material icone, TextColor cor) {
            this.nome = nome;
            this.icone = icone;
            this.cor = cor;
        }
    }

    /**
     * O que um pedestal precisa ter: um item comum do jogo, um material raro do plugin ou (2.89)
     * qualquer item que passe no {@code teste} (um item encantado, um Selo...).
     */
    public record Ingrediente(Material material, Raro raro, int qtd, Predicate<ItemStack> teste, String nomeTeste) {
        Ingrediente(Material material, Raro raro, int qtd) { this(material, raro, qtd, null, null); }

        boolean serve(ItemStack i) {
            if (i == null || i.isEmpty() || i.getAmount() < qtd) return false;
            if (teste != null) return teste.test(i);
            if (raro != null) return Raro.de(i) == raro;
            return i.getType() == material && (!i.hasItemMeta() || i.getItemMeta().getPersistentDataContainer().getKeys().isEmpty());
        }

        String nomeBonito() {
            return qtd + "x " + (teste != null ? nomeTeste : raro != null ? raro.nome() : bonito(material));
        }
    }

    /** Um mob vivo que precisa estar perto do altar (a até 5 blocos) e é sacrificado na cerimônia. */
    public record Sacrificio(EntityType tipo, int qtd, String nome) { }

    /** Algo que a cerimônia faz no mundo em vez de dar um item. */
    public interface Efeito {
        void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo);

        default String impedimento(RPGAtributos plugin, Location altar, Player quem) { return null; }

        /** 2.89: os efeitos que dependem do catalisador (a coleira do companheiro) usam estes. */
        default void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo, ItemStack catalisador) {
            aplicar(plugin, altar, quem, grupo);
        }

        default String impedimento(RPGAtributos plugin, Location altar, Player quem, ItemStack catalisador) {
            return impedimento(plugin, altar, quem);
        }
    }

    /**
     * @param comPedestais resultado que depende do que estava nos pedestais (na ordem dos ingredientes)
     * @param nivelAltar   nível mínimo do altar (2 = segunda volta de pedestais, 3 = terceira)
     * @param jogadores    quantos jogadores, cada um com o catalisador na mão
     * @param oculta       não aparece no Grimório (as elevações de guardião, mostradas de forma resumida)
     * @param porque       por que os pedestais não servem para esse catalisador (ou null se servem)
     * @param sobras       itens que voltam junto com o resultado (o item de onde saíram os encantamentos)
     */
    public record Extra(Sacrificio sacrificio, Efeito efeito, BiFunction<ItemStack, List<ItemStack>, ItemStack> comPedestais,
                        int nivelAltar, int jogadores, boolean oculta, BiFunction<ItemStack, List<ItemStack>, String> porque,
                        BiFunction<ItemStack, List<ItemStack>, List<ItemStack>> sobras) {
        static final Extra NADA = new Extra(null, null, null, 1, 1, false, null, null);

        Extra sac(EntityType t, int q, String nome) { return new Extra(new Sacrificio(t, q, nome), efeito, comPedestais, nivelAltar, jogadores, oculta, porque, sobras); }
        Extra efeito(Efeito e) { return new Extra(sacrificio, e, comPedestais, nivelAltar, jogadores, oculta, porque, sobras); }
        Extra comPedestais(BiFunction<ItemStack, List<ItemStack>, ItemStack> f) { return new Extra(sacrificio, efeito, f, nivelAltar, jogadores, oculta, porque, sobras); }
        Extra nivel(int n) { return new Extra(sacrificio, efeito, comPedestais, n, jogadores, oculta, porque, sobras); }
        Extra grupo(int n) { return new Extra(sacrificio, efeito, comPedestais, nivelAltar, n, oculta, porque, sobras); }
        Extra escondida() { return new Extra(sacrificio, efeito, comPedestais, nivelAltar, jogadores, true, porque, sobras); }
        Extra porque(BiFunction<ItemStack, List<ItemStack>, String> f) { return new Extra(sacrificio, efeito, comPedestais, nivelAltar, jogadores, oculta, f, sobras); }
        Extra sobras(BiFunction<ItemStack, List<ItemStack>, List<ItemStack>> f) { return new Extra(sacrificio, efeito, comPedestais, nivelAltar, jogadores, oculta, porque, f); }
    }

    ReceitaRitual com(Extra e) {
        return new ReceitaRitual(nome, categoria, icone, catalisador, nomeCatalisador, qtdCatalisador, pedestais, resultado,
                impedimento, descricao, desperto, unica, e);
    }

    /** Se falhar, o catalisador volta (equipamento, relíquia, selo): só os pedestais se perdem. */
    boolean devolveCatalisadorAoFalhar() {
        return categoria == Categoria.INFUNDIR || unica || extra.comPedestais() != null || extra.oculta()
                || (categoria == Categoria.INVOCAR && extra.efeito() != null);
    }

    public static final NamespacedKey K_INFUSOES = new NamespacedKey("rpgatributos", "infusoes_altar");
    public static final NamespacedKey K_RELIQUIA = new NamespacedKey("rpgatributos", "reliquia_convergencia");
    public static final int MAX_INFUSOES = 3;

    static String bonito(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Ingrediente i(Material m, int q) { return new Ingrediente(m, null, q); }
    private static Ingrediente r(Raro r, int q) { return new Ingrediente(null, r, q); }

    private static boolean comum(ItemStack s) {
        return !s.hasItemMeta() || s.getItemMeta().getPersistentDataContainer().getKeys().isEmpty();
    }

    private static ReceitaRitual simples(String nome, Categoria cat, Material catalisador, int qtdCat, Material icone,
                                         Function<ItemStack, ItemStack> res, String desc, Ingrediente... peds) {
        return new ReceitaRitual(nome, cat, icone, s -> s.getType() == catalisador && comum(s), bonito(catalisador), qtdCat,
                List.of(peds), res, s -> null, desc, null, false, Extra.NADA);
    }

    private static ReceitaRitual transmutar(String nome, Material cat, int qtdCat, Material res, int qtdRes, String desc, Ingrediente... peds) {
        return simples(nome, Categoria.TRANSMUTAR, cat, qtdCat, res, s -> new ItemStack(res, qtdRes), desc, peds);
    }

    /** Um item raro do jogo (ou do plugin) feito no altar. */
    private static ReceitaRitual raro(String nome, Material cat, int qtdCat, Material icone, java.util.function.Supplier<ItemStack> res,
                                      String desc, Ingrediente... peds) {
        return simples(nome, Categoria.RARO, cat, qtdCat, icone, s -> res.get(), desc, peds);
    }

    /** Guardião: o ritual dá o Selo (ou as Asas, ou o Cetro), que põe o guardião no mundo. */
    private static ReceitaRitual guardiao(String nome, Material cat, int qtdCat, Material icone, java.util.function.Supplier<ItemStack> res,
                                          String desc, Ingrediente... peds) {
        return simples(nome, Categoria.GUARDIAO, cat, qtdCat, icone, s -> res.get(), desc, peds);
    }

    private static ReceitaRitual selo(Guardioes.Tipo t, Material cat, Ingrediente... peds) {
        return guardiao(t.nome, cat, 1, t.icone, () -> Guardioes.selo(t, 1, false), t.descricao, peds);
    }

    /** Ritual no mundo: o catalisador e os pedestais viram um efeito (chuva, noite, aura...). */
    private static ReceitaRitual mundo(String nome, Material cat, int qtdCat, Material icone, String desc, Efeito ef, Ingrediente... peds) {
        return simples(nome, Categoria.MUNDO, cat, qtdCat, icone, s -> null, desc, peds).com(Extra.NADA.efeito(ef));
    }

    /** Infusão: sobe um encantamento 1 nível (podendo passar do limite normal, até {@code teto}). */
    private static ReceitaRitual infundir(String nome, Enchantment e, int teto, String paraQue, String desc, Ingrediente... peds) {
        return new ReceitaRitual(nome, Categoria.INFUNDIR, Material.ENCHANTED_BOOK, e::canEnchantItem, paraQue, 1, List.of(peds),
                s -> {
                    ItemStack r = s.clone();
                    r.setAmount(1);
                    r.addUnsafeEnchantment(e, Math.max(1, r.getEnchantmentLevel(e) + 1));
                    marcarInfusao(r, nome);
                    return r;
                },
                s -> {
                    if (s.getEnchantmentLevel(e) >= teto) return "Esse item já está no máximo de " + nome + " (" + teto + ").";
                    for (Enchantment outro : s.getEnchantments().keySet()) {
                        if (outro != e && outro.conflictsWith(e)) return "Esse item tem um encantamento que não combina com " + nome + ".";
                    }
                    return infusoes(s) >= MAX_INFUSOES ? "Esse item já recebeu " + MAX_INFUSOES + " infusões (o máximo)." : null;
                }, desc, null, false, Extra.NADA);
    }

    /** Eterna: Inquebrável +1 e Remendo, se ainda não tiver. */
    private static ReceitaRitual eterna() {
        String nome = "Eterna";
        return new ReceitaRitual(nome, Categoria.INFUNDIR, Material.EXPERIENCE_BOTTLE,
                s -> s.getType().getMaxDurability() > 0, "Qualquer item com durabilidade", 1,
                List.of(i(Material.EXPERIENCE_BOTTLE, 16), i(Material.DIAMOND, 2), i(Material.AMETHYST_SHARD, 16), i(Material.OBSIDIAN, 4)),
                s -> {
                    ItemStack r = s.clone();
                    r.setAmount(1);
                    r.addUnsafeEnchantment(Enchantment.UNBREAKING, r.getEnchantmentLevel(Enchantment.UNBREAKING) + 1);
                    r.addUnsafeEnchantment(Enchantment.MENDING, 1);
                    marcarInfusao(r, nome);
                    return r;
                },
                s -> {
                    if (s.getEnchantmentLevel(Enchantment.UNBREAKING) >= 5 && s.getEnchantmentLevel(Enchantment.MENDING) > 0) {
                        return "Esse item já é eterno (Inquebrável V e Remendo).";
                    }
                    return infusoes(s) >= MAX_INFUSOES ? "Esse item já recebeu " + MAX_INFUSOES + " infusões (o máximo)." : null;
                }, "Inquebrável +1 (até V) e Remendo.", null, false, Extra.NADA);
    }

    /** Relíquia única: o item na mão vira uma peça lendária com encantamentos acima do normal. */
    private static ReceitaRitual reliquia(String nome, Material base, Map<Enchantment, Integer> encantos, String desc, Ingrediente... peds) {
        return reliquia(nome, base, encantos, desc, 2, peds);
    }

    private static ReceitaRitual reliquia(String nome, Material base, Map<Enchantment, Integer> encantos, String desc, int nivelAltar,
                                          Ingrediente... peds) {
        return new ReceitaRitual(nome, Categoria.RARO, base, s -> s.getType() == base && !s.getPersistentDataContainer().has(K_RELIQUIA),
                bonito(base), 1, List.of(peds),
                s -> {
                    ItemStack r = s.clone();
                    r.setAmount(1);
                    for (Map.Entry<Enchantment, Integer> en : encantos.entrySet()) {
                        r.addUnsafeEnchantment(en.getKey(), Math.max(en.getValue(), r.getEnchantmentLevel(en.getKey())));
                    }
                    r.editMeta(m -> {
                        m.customName(Component.text(nome, AltarPedestais.COR, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text(desc, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true));
                        lore.add(Component.empty());
                        lore.add(Component.text("✦ Relíquia única da Convergência", AltarPedestais.COR).decoration(TextDecoration.ITALIC, false));
                        m.lore(lore);
                        m.setEnchantmentGlintOverride(true);
                        m.setFireResistant(true);
                        m.getPersistentDataContainer().set(K_RELIQUIA, PersistentDataType.STRING, nome);
                    });
                    return r;
                }, s -> null, desc, null, true, Extra.NADA.nivel(nivelAltar));
    }

    // =====================================================================
    //  2.89: transferir encantamentos, fundir guardiões, trazer de volta
    // =====================================================================

    /** Os encantamentos da origem que o alvo pode receber (sem maldição, sem briga, só os que sobem de nível). */
    static Map<Enchantment, Integer> transferiveis(ItemStack alvo, ItemStack origem) {
        Map<Enchantment, Integer> m = new LinkedHashMap<>();
        ItemStack tipo = new ItemStack(alvo.getType());
        for (Map.Entry<Enchantment, Integer> en : origem.getEnchantments().entrySet()) {
            Enchantment e = en.getKey();
            if (e.isCursed() || !e.canEnchantItem(tipo) || en.getValue() <= alvo.getEnchantmentLevel(e)) continue;
            boolean briga = false;
            for (Enchantment outro : alvo.getEnchantments().keySet()) if (outro != e && outro.conflictsWith(e)) briga = true;
            for (Enchantment outro : m.keySet()) if (outro.conflictsWith(e)) briga = true;
            if (!briga) m.put(e, en.getValue());
        }
        return m;
    }

    /**
     * Transferir: os encantamentos do item no pedestal passam para o da mão. O livro é a ponte;
     * a origem volta junto, sem os encantamentos que passaram.
     */
    private static ReceitaRitual transferir() {
        Predicate<ItemStack> origem = s -> !s.getEnchantments().isEmpty() && !s.getPersistentDataContainer().has(K_RELIQUIA);
        return new ReceitaRitual("Transferir Encantamentos", Categoria.INFUNDIR, Material.BOOK,
                s -> s.getType().getMaxStackSize() == 1 && s.getType() != Material.ENCHANTED_BOOK && !s.getPersistentDataContainer().has(K_RELIQUIA),
                "O item que vai receber os encantamentos", 1,
                List.of(new Ingrediente(null, null, 1, origem, "Item encantado (a origem)"), i(Material.BOOK, 1),
                        i(Material.LAPIS_BLOCK, 2), i(Material.EXPERIENCE_BOTTLE, 8)),
                s -> s.clone(), s -> null,
                "Os encantamentos do item no pedestal passam para o da mão (o livro é a ponte). A origem volta sem eles.", null, false,
                Extra.NADA
                        .porque((s, peds) -> transferiveis(s, peds.getFirst()).isEmpty()
                                ? "Nenhum encantamento desse item serve (ou sobe de nível) no " + bonito(s.getType()).toLowerCase() + "." : null)
                        .comPedestais((s, peds) -> {
                            Map<Enchantment, Integer> m = transferiveis(s, peds.getFirst());
                            if (m.isEmpty()) return null;
                            ItemStack r = s.clone();
                            r.setAmount(1);
                            m.forEach(r::addUnsafeEnchantment);
                            return r;
                        })
                        .sobras((s, peds) -> {
                            ItemStack o = peds.getFirst().clone();
                            o.setAmount(1);
                            for (Enchantment e : transferiveis(s, peds.getFirst()).keySet()) o.removeEnchantment(e);
                            return List.of(o);
                        }));
    }

    /** Fundir: o Selo da mão (corpo e trabalho) recebe a função de apoio do Selo do pedestal. */
    private static ReceitaRitual fundir() {
        return new ReceitaRitual("Fundir Guardiões", Categoria.GUARDIAO, Material.LODESTONE, s -> Guardioes.tipoSelo(s) != null,
                "O Selo do guardião que fica com o corpo", 1,
                List.of(new Ingrediente(null, null, 1, s -> Guardioes.tipoSelo(s) != null, "Selo do guardião de apoio"),
                        r(Raro.ALMA_DESPERTA, 1), i(Material.AMETHYST_BLOCK, 4), i(Material.DIAMOND_BLOCK, 1)),
                s -> s.clone(), s -> null,
                "Dois viram um: o corpo e o trabalho do Selo da mão, mais a função do outro (Espírito, Torre, Curandeiro, Vigia ou Ferreiro). "
                        + "Fica com o maior nível e 25% mais vida.", null, false,
                Extra.NADA.nivel(2)
                        .porque((s, peds) -> Guardioes.porqueNaoFunde(s, peds.getFirst()))
                        .comPedestais((s, peds) -> Guardioes.fundir(s, peds.getFirst())));
    }

    /** Trazer de volta: a coleira (ou uma etiqueta com o nome) traz o companheiro morto, com o nível e a evolução dele. */
    private static ReceitaRitual trazerDeVolta() {
        return new ReceitaRitual("Trazer de Volta", Categoria.INVOCAR, Material.LEAD, Lembrancas::serveDeCatalisador,
                "A coleira do companheiro (ou etiqueta com o nome dele)", 1,
                List.of(i(Material.GOLDEN_APPLE, 2), i(Material.GHAST_TEAR, 2), i(Material.BONE_BLOCK, 4), i(Material.GLISTERING_MELON_SLICE, 8)),
                s -> null, s -> null,
                "O companheiro que morreu volta do altar com o nível e a evolução (os componentes ficaram no chão quando ele morreu).",
                null, false, Extra.NADA.efeito(Lembrancas.efeito()));
    }

    /**
     * Reforjar: o livro encantado do primeiro pedestal entra na relíquia 1 nível acima do livro
     * (até 2 acima do máximo normal), tirando o que não combina com ele.
     */
    private static ReceitaRitual reforjar() {
        return new ReceitaRitual("Reforjar Relíquia", Categoria.RARO, Material.ENCHANTED_BOOK,
                s -> s.getPersistentDataContainer().has(K_RELIQUIA), "Uma Relíquia da Convergência", 1,
                List.of(i(Material.ENCHANTED_BOOK, 1), r(Raro.ALMA_DESPERTA, 1), i(Material.LAPIS_BLOCK, 4)),
                s -> s.clone(), s -> null,
                "Troca um encantamento da relíquia pelo do livro (1 nível acima do livro).", null, false,
                Extra.NADA.nivel(2).comPedestais((s, peds) -> {
                    if (peds.isEmpty() || !(peds.getFirst().getItemMeta() instanceof EnchantmentStorageMeta livro)) return null;
                    ItemStack r = s.clone();
                    boolean mudou = false;
                    for (Map.Entry<Enchantment, Integer> en : livro.getStoredEnchants().entrySet()) {
                        Enchantment e = en.getKey();
                        if (!e.canEnchantItem(new ItemStack(r.getType()))) continue;
                        for (Enchantment outro : List.copyOf(r.getEnchantments().keySet())) {
                            if (outro != e && outro.conflictsWith(e)) r.removeEnchantment(outro);
                        }
                        int nivel = Math.min(e.getMaxLevel() + 2, Math.max(en.getValue(), r.getEnchantmentLevel(e)) + 1);
                        r.addUnsafeEnchantment(e, nivel);
                        mudou = true;
                    }
                    return mudou ? r : null;
                }));
    }

    private static Map<Enchantment, Integer> enc(Object... pares) {
        Map<Enchantment, Integer> m = new LinkedHashMap<>();
        for (int k = 0; k < pares.length; k += 2) m.put((Enchantment) pares[k], (Integer) pares[k + 1]);
        return m;
    }

    /** Invocação maior: o núcleo do chefe na mão desperta a versão mais forte dele. */
    private static ReceitaRitual invocar(Chefe c, String nome, Ingrediente... peds) {
        return new ReceitaRitual(nome, Categoria.INVOCAR, c.nucleo().base(), s -> Raro.de(s) == c.nucleo(), c.nucleo().nome(), 1,
                List.of(peds), s -> s.clone(), s -> null,
                "2,5x a vida e +50% de dano. Saque dobrado, Essência garantida e Alma Desperta.", c, false, Extra.NADA);
    }

    // =====================================================================
    //  Elevar guardião (nível 1 → 5)
    // =====================================================================

    /** O que cada nível pede além do tributo do guardião (o nível que ele vai ter). */
    static List<Ingrediente> pedidoNivel(int n) {
        return switch (n) {
            case 2 -> List.of(i(Material.IRON_BLOCK, 2), i(Material.LAPIS_LAZULI, 16));
            case 3 -> List.of(i(Material.GOLD_BLOCK, 2), i(Material.DIAMOND, 2));
            case 4 -> List.of(i(Material.DIAMOND_BLOCK, 1), r(Raro.ALMA_DESPERTA, 1), i(Material.OBSIDIAN, 8));
            default -> List.of(i(Material.NETHER_STAR, 1), r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1));
        };
    }

    static Sacrificio sacrificioNivel(int n) {
        return switch (n) {
            case 2 -> new Sacrificio(EntityType.CHICKEN, 2, "2 galinhas");
            case 3 -> new Sacrificio(EntityType.SHEEP, 1, "1 ovelha");
            case 4 -> new Sacrificio(EntityType.COW, 2, "2 vacas");
            default -> new Sacrificio(EntityType.ZOMBIE, 1, "1 zumbi");
        };
    }

    private static List<ReceitaRitual> elevacoes() {
        List<ReceitaRitual> l = new ArrayList<>();
        for (int n = 2; n <= 5; n++) {
            final int nivel = n;
            // A versão que aparece no Grimório (nunca casa com nada: só explica).
            List<Ingrediente> mostrar = new ArrayList<>(pedidoNivel(n));
            l.add(new ReceitaRitual("Elevar guardião ao nível " + romano(n), Categoria.GUARDIAO, Material.EXPERIENCE_BOTTLE, s -> false,
                    "O Selo de um guardião de nível " + romano(n - 1), 1, mostrar, s -> null, s -> null,
                    "Mais o tributo do guardião: " + (8 * n) + "x do item dele (veja abaixo).", null, false,
                    Extra.NADA.sac(sacrificioNivel(n).tipo(), sacrificioNivel(n).qtd(), sacrificioNivel(n).nome()).nivel(n >= 4 ? 2 : 1)));
            for (Guardioes.Tipo t : Guardioes.Tipo.values()) {
                List<Ingrediente> peds = new ArrayList<>(pedidoNivel(n));
                peds.add(i(t.tributo, 8 * n));
                Sacrificio sac = sacrificioNivel(n);
                l.add(new ReceitaRitual("Elevar " + t.nome + " ao nível " + romano(n), Categoria.GUARDIAO, t.icone,
                        s -> Guardioes.tipoSelo(s) == t && Guardioes.nivelSelo(s) == nivel - 1, "Selo: " + t.nome + " nível " + romano(n - 1), 1,
                        peds, s -> Guardioes.elevarSelo(s, nivel), s -> null,
                        t.nome + " sobe para o nível " + romano(n) + ".", null, false,
                        Extra.NADA.sac(sac.tipo(), sac.qtd(), sac.nome()).nivel(n >= 4 ? 2 : 1).escondida()));
            }
        }
        return l;
    }

    static String romano(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; };
    }

    public static int infusoes(ItemStack s) {
        if (!s.hasItemMeta()) return 0;
        return s.getItemMeta().getPersistentDataContainer().getOrDefault(K_INFUSOES, PersistentDataType.INTEGER, 0);
    }

    private static void marcarInfusao(ItemStack r, String nome) {
        r.editMeta(m -> {
            int n = m.getPersistentDataContainer().getOrDefault(K_INFUSOES, PersistentDataType.INTEGER, 0) + 1;
            m.getPersistentDataContainer().set(K_INFUSOES, PersistentDataType.INTEGER, n);
            List<Component> lore = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
            lore.add(Component.text("✦ Infusão: " + nome, Categoria.INFUNDIR.cor).decoration(TextDecoration.ITALIC, false));
            m.lore(lore);
            m.setEnchantmentGlintOverride(true);
        });
    }

    public static final List<ReceitaRitual> TODAS = montar();

    private static List<ReceitaRitual> montar() {
        List<ReceitaRitual> l = new ArrayList<>(List.of(
            // Transmutar: materiais difíceis de achar a partir de outros comuns.
            transmutar("Fragmentos de Eco", Material.AMETHYST_SHARD, 4, Material.ECHO_SHARD, 2,
                    "O eco da cidade antiga, sem ir até ela.", i(Material.SCULK, 16), i(Material.ENDER_PEARL, 4)),
            transmutar("Lágrimas de Ghast", Material.GOLD_INGOT, 2, Material.GHAST_TEAR, 2,
                    "Almas do Nether choram em ouro.", i(Material.QUARTZ, 16), i(Material.SOUL_SAND, 8)),
            transmutar("Conchas de Náutilo", Material.PRISMARINE_CRYSTALS, 4, Material.NAUTILUS_SHELL, 2,
                    "O mar devolve o que guarda.", i(Material.PRISMARINE_SHARD, 16), i(Material.KELP, 16)),
            transmutar("Molde de Netherite", Material.DIAMOND, 2, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1,
                    "Um molde novo, sem procurar bastião.", i(Material.NETHERRACK, 32), i(Material.GOLD_BLOCK, 1), i(Material.OBSIDIAN, 4)),
            transmutar("Garrafas de Experiência", Material.GLASS_BOTTLE, 16, Material.EXPERIENCE_BOTTLE, 16,
                    "Saber engarrafado.", i(Material.LAPIS_LAZULI, 16), i(Material.REDSTONE, 16)),
            transmutar("Varas de Blaze", Material.FIRE_CHARGE, 2, Material.BLAZE_ROD, 4,
                    "Fogo preso em madeira.", i(Material.STICK, 16), i(Material.GLOWSTONE_DUST, 8), i(Material.MAGMA_BLOCK, 4)),
            transmutar("Pérolas do Fim", Material.LAPIS_LAZULI, 8, Material.ENDER_PEARL, 4,
                    "Um pedaço do vazio, condensado.", i(Material.SLIME_BALL, 8), i(Material.OBSIDIAN, 2)),
            transmutar("Cascas de Shulker", Material.ENDER_PEARL, 4, Material.SHULKER_SHELL, 2,
                    "Para quem não aguenta mais caçar shulkers.", i(Material.PURPUR_BLOCK, 16), i(Material.CHORUS_FRUIT, 16)),

            // Infundir: o equipamento na mão sobe além do limite normal (até 3 infusões por item).
            infundir("Fio Afiado", Enchantment.SHARPNESS, 7, "Espada, machado ou lança",
                    "Afiação +1 (até VII).", i(Material.DIAMOND, 4), i(Material.QUARTZ_BLOCK, 8), i(Material.FLINT, 16), i(Material.AMETHYST_SHARD, 8)),
            infundir("Lâmina Flamejante", Enchantment.FIRE_ASPECT, 4, "Espada ou lança",
                    "Aspecto Flamejante +1 (até IV).", i(Material.BLAZE_ROD, 6), i(Material.MAGMA_CREAM, 8), i(Material.FIRE_CHARGE, 8)),
            infundir("Saque Arcano", Enchantment.LOOTING, 5, "Espada ou lança",
                    "Saque +1 (até V).", i(Material.EMERALD, 8), i(Material.RABBIT_FOOT, 2), i(Material.GOLD_BLOCK, 2)),
            infundir("Pele de Pedra", Enchantment.PROTECTION, 6, "Qualquer armadura",
                    "Proteção +1 (até VI).", i(Material.OBSIDIAN, 8), i(Material.IRON_BLOCK, 2), i(Material.DIAMOND, 2)),
            infundir("Pés de Pena", Enchantment.FEATHER_FALLING, 6, "Botas",
                    "Peso-Pena +1 (até VI).", i(Material.FEATHER, 32), i(Material.PHANTOM_MEMBRANE, 4)),
            infundir("Mão Ligeira", Enchantment.EFFICIENCY, 7, "Ferramenta",
                    "Eficiência +1 (até VII).", i(Material.REDSTONE_BLOCK, 4), i(Material.GOLD_BLOCK, 2), i(Material.SUGAR, 16)),
            infundir("Fortuna Arcana", Enchantment.FORTUNE, 5, "Picareta, machado, pá ou enxada",
                    "Fortuna +1 (até V).", i(Material.LAPIS_BLOCK, 4), i(Material.EMERALD, 8), i(Material.GOLD_BLOCK, 2)),
            infundir("Arco Certeiro", Enchantment.POWER, 7, "Arco",
                    "Força +1 (até VII).", i(Material.ARROW, 32), i(Material.STRING, 16), i(Material.GLOWSTONE_DUST, 16)),
            eterna(),
            transferir(),

            // Raros: itens que o jogo quase não dá, e as relíquias (uma de cada por jogador, altar nível II).
            raro("Totem da Imortalidade", Material.GOLD_BLOCK, 1, Material.TOTEM_OF_UNDYING, () -> new ItemStack(Material.TOTEM_OF_UNDYING),
                    "Uma segunda vida, sem invadir uma mansão.", i(Material.EMERALD_BLOCK, 2), i(Material.GOLDEN_APPLE, 4)),
            raro("Maçã Dourada Encantada", Material.GOLDEN_APPLE, 1, Material.ENCHANTED_GOLDEN_APPLE, () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                    "A receita que o mundo esqueceu.", i(Material.GOLD_BLOCK, 8), i(Material.EXPERIENCE_BOTTLE, 16)),
            raro("Coração do Mar", Material.PRISMARINE_CRYSTALS, 8, Material.HEART_OF_THE_SEA, () -> new ItemStack(Material.HEART_OF_THE_SEA),
                    "Para o seu conduíte.", i(Material.NAUTILUS_SHELL, 4), i(Material.DIAMOND, 2)),
            raro("Estrela do Nether", Material.WITHER_SKELETON_SKULL, 1, Material.NETHER_STAR, () -> new ItemStack(Material.NETHER_STAR),
                    "Um sinalizador sem enfrentar o Wither.", i(Material.SOUL_SAND, 16), r(Raro.ALMA_DESPERTA, 1), i(Material.GOLD_BLOCK, 2))
                    .com(Extra.NADA.sac(EntityType.WITHER_SKELETON, 1, "1 esqueleto wither")),
            raro("Élitro", Material.PHANTOM_MEMBRANE, 8, Material.ELYTRA, () -> new ItemStack(Material.ELYTRA),
                    "Asas, sem procurar navio no Fim.", i(Material.SHULKER_SHELL, 2), i(Material.FEATHER, 32), r(Raro.ALMA_DESPERTA, 1), i(Material.DIAMOND, 4))
                    .com(Extra.NADA.sac(EntityType.PHANTOM, 1, "1 phantom")),
            raro("Essência Primordial", Material.DIAMOND_BLOCK, 1, Material.HEART_OF_THE_SEA, () -> Raro.ESSENCIA_PRIMORDIAL.criar(),
                    "A matéria do refino +10, sem esperar a sorte.", r(Raro.ALMA_DESPERTA, 2), r(Raro.FRAGMENTO_DE_FORJA, 8), i(Material.AMETHYST_SHARD, 16)),
            reliquia("Lâmina da Convergência", Material.NETHERITE_SWORD,
                    enc(Enchantment.SHARPNESS, 7, Enchantment.LOOTING, 4, Enchantment.FIRE_ASPECT, 3, Enchantment.SWEEPING_EDGE, 3,
                            Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "Forjada no ponto onde tudo se encontra.",
                    r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.NETHER_STAR, 1), i(Material.DIAMOND_BLOCK, 2)),
            reliquia("Égide da Convergência", Material.NETHERITE_CHESTPLATE,
                    enc(Enchantment.PROTECTION, 6, Enchantment.THORNS, 3, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "Nada que atravessa o centro sai inteiro.",
                    r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.TOTEM_OF_UNDYING, 1), i(Material.DIAMOND_BLOCK, 2)),
            reliquia("Picareta da Convergência", Material.NETHERITE_PICKAXE,
                    enc(Enchantment.EFFICIENCY, 7, Enchantment.FORTUNE, 5, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "A pedra se abre antes do golpe.",
                    r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.EMERALD_BLOCK, 4), i(Material.REDSTONE_BLOCK, 8)),
            reliquia("Arco da Convergência", Material.BOW,
                    enc(Enchantment.POWER, 7, Enchantment.PUNCH, 2, Enchantment.FLAME, 1, Enchantment.INFINITY, 1, Enchantment.UNBREAKING, 5),
                    "Cada flecha volta para a aljava.",
                    r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.SPECTRAL_ARROW, 32), i(Material.BREEZE_ROD, 4)),
            reforjar(),

            // 2.89: lendárias do altar nível III (24 pedestais).
            raro("Ovo do Dragão", Material.DRAGON_HEAD, 1, Material.DRAGON_EGG, () -> new ItemStack(Material.DRAGON_EGG),
                    "Um segundo ovo, chocado no ponto onde tudo se encontra.", i(Material.END_CRYSTAL, 4), i(Material.DRAGON_BREATH, 8),
                    r(Raro.ALMA_DESPERTA, 2), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.OBSIDIAN, 32))
                    .com(Extra.NADA.nivel(3).sac(EntityType.ENDERMAN, 2, "2 endermen")),
            reliquia("Tridente da Convergência", Material.TRIDENT,
                    enc(Enchantment.IMPALING, 7, Enchantment.LOYALTY, 4, Enchantment.CHANNELING, 1, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "O mar e o céu respondem ao mesmo chamado.", 3,
                    r(Raro.ALMA_DESPERTA, 3), r(Raro.ESSENCIA_PRIMORDIAL, 2), i(Material.NETHER_STAR, 1), i(Material.HEART_OF_THE_SEA, 1),
                    i(Material.PRISMARINE_CRYSTALS, 32)),
            reliquia("Maça da Convergência", Material.MACE,
                    enc(Enchantment.DENSITY, 7, Enchantment.WIND_BURST, 3, Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "Quanto mais alto o salto, mais fundo o golpe.", 3,
                    r(Raro.ALMA_DESPERTA, 3), r(Raro.ESSENCIA_PRIMORDIAL, 2), i(Material.NETHER_STAR, 1), i(Material.HEAVY_CORE, 1),
                    i(Material.BREEZE_ROD, 8)),
            reliquia("Botas da Convergência", Material.NETHERITE_BOOTS,
                    enc(Enchantment.PROTECTION, 6, Enchantment.FEATHER_FALLING, 6, Enchantment.DEPTH_STRIDER, 3, Enchantment.SOUL_SPEED, 4,
                            Enchantment.UNBREAKING, 5, Enchantment.MENDING, 1),
                    "Nenhum chão é longe demais.", 3,
                    r(Raro.ALMA_DESPERTA, 3), r(Raro.ESSENCIA_PRIMORDIAL, 2), i(Material.NETHER_STAR, 1), i(Material.PHANTOM_MEMBRANE, 16),
                    i(Material.RABBIT_FOOT, 4)),

            // Invocações maiores: o chefe Desperto (a oferenda do Altar Ritualístico, reforçada).
            invocar(Chefe.GOLEM, "Golem Ancestral Desperto", i(Material.IRON_BLOCK, 8), i(Material.CARVED_PUMPKIN, 1), i(Material.DIAMOND, 2)),
            invocar(Chefe.RAINHA, "Rainha Aracnídea Desperta", i(Material.SPIDER_EYE, 16), i(Material.STRING, 32), i(Material.FERMENTED_SPIDER_EYE, 4)),
            invocar(Chefe.CHAMAS, "Senhor das Chamas Desperto", i(Material.BLAZE_ROD, 12), i(Material.MAGMA_CREAM, 8), i(Material.GHAST_TEAR, 2)),
            invocar(Chefe.LICH, "Lich Desperto", i(Material.SKELETON_SKULL, 1), i(Material.BONE_BLOCK, 16), i(Material.SOUL_SOIL, 8))
                    .com(Extra.NADA.sac(EntityType.SKELETON, 2, "2 esqueletos")),
            invocar(Chefe.TEMPESTADE, "Tempestade Viva Desperta", i(Material.BREEZE_ROD, 6), i(Material.WIND_CHARGE, 16), i(Material.FEATHER, 16)),
            invocar(Chefe.ARAUTO, "Arauto do Fim Desperto", i(Material.NETHER_STAR, 1), i(Material.ECHO_SHARD, 8), i(Material.DRAGON_BREATH, 2), i(Material.END_CRYSTAL, 1))
                    .com(Extra.NADA.nivel(2).grupo(2).sac(EntityType.ENDERMAN, 1, "1 enderman")),
            trazerDeVolta(),

            // Guardiões e ajudantes: o Selo põe o guardião onde você clicar.
            selo(Guardioes.Tipo.ESPIRITO, Material.ENDER_EYE, i(Material.AMETHYST_BLOCK, 4), i(Material.BLAZE_ROD, 4), i(Material.REDSTONE_BLOCK, 2), i(Material.IRON_BLOCK, 2)),
            selo(Guardioes.Tipo.FERRO, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 8), i(Material.DIAMOND, 2), i(Material.GOLDEN_APPLE, 1)),
            selo(Guardioes.Tipo.FAZENDA, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 4), i(Material.HAY_BLOCK, 8), i(Material.BONE_MEAL, 32), i(Material.COMPOSTER, 1)),
            selo(Guardioes.Tipo.COBRE, Material.CARVED_PUMPKIN, i(Material.COPPER_BLOCK, 4), i(Material.HONEYCOMB, 4), i(Material.CHEST, 2)),
            selo(Guardioes.Tipo.LENHADOR, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 4), i(Material.IRON_AXE, 1), i(Material.OAK_SAPLING, 8), i(Material.CHEST, 1)),
            selo(Guardioes.Tipo.MINERADOR, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 4), i(Material.IRON_PICKAXE, 1), i(Material.TORCH, 32), i(Material.CHEST, 1)),
            selo(Guardioes.Tipo.PASTOR, Material.CARVED_PUMPKIN, i(Material.SNOW_BLOCK, 4), i(Material.SHEARS, 1), i(Material.WHEAT, 16), i(Material.LEAD, 2)),
            selo(Guardioes.Tipo.CURANDEIRO, Material.GLISTERING_MELON_SLICE, i(Material.AMETHYST_BLOCK, 2), i(Material.GOLDEN_APPLE, 2), i(Material.GHAST_TEAR, 1), i(Material.HONEY_BOTTLE, 4)),
            selo(Guardioes.Tipo.TORRE, Material.BLAZE_ROD, i(Material.AMETHYST_BLOCK, 4), i(Material.MAGMA_BLOCK, 8), i(Material.FIRE_CHARGE, 16), i(Material.OBSIDIAN, 8))
                    .com(Extra.NADA.sac(EntityType.BLAZE, 1, "1 blaze")),
            selo(Guardioes.Tipo.PESCADOR, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 4), i(Material.FISHING_ROD, 1), i(Material.COD, 16), i(Material.CHEST, 1)),
            selo(Guardioes.Tipo.APICULTOR, Material.HONEYCOMB, i(Material.BEEHIVE, 1), i(Material.HONEY_BLOCK, 2), i(Material.DANDELION, 8), i(Material.CAMPFIRE, 1)),
            selo(Guardioes.Tipo.FERREIRO, Material.CARVED_PUMPKIN, i(Material.IRON_BLOCK, 6), i(Material.ANVIL, 1), i(Material.BLAST_FURNACE, 1), i(Material.GRINDSTONE, 1)),
            selo(Guardioes.Tipo.COLETOR, Material.NOTE_BLOCK, i(Material.AMETHYST_BLOCK, 2), i(Material.HOPPER, 4), i(Material.CHEST, 2), i(Material.ENDER_PEARL, 4)),
            selo(Guardioes.Tipo.VIGIA, Material.SPYGLASS, i(Material.BELL, 1), i(Material.REDSTONE_LAMP, 4), i(Material.IRON_BLOCK, 2), i(Material.FEATHER, 16)),
            guardiao("Asas de Guardião", Material.PHANTOM_MEMBRANE, 4, Material.ELYTRA, Guardioes::asas,
                    "Clique no seu guardião com elas: ele passa a voar.", i(Material.FEATHER, 32), i(Material.BREEZE_ROD, 2), i(Material.AMETHYST_SHARD, 8)),
            guardiao("Cetro de Comando", Material.BLAZE_ROD, 1, Material.BLAZE_ROD, Guardioes::cetro,
                    "Clique no seu guardião: ele passa a seguir você, ou fica de posto onde está.", i(Material.AMETHYST_SHARD, 8), i(Material.GOLD_INGOT, 4), i(Material.STRING, 4)),
            fundir(),

            // Céu, território e altar.
            mundo("Chamar a Chuva", Material.WATER_BUCKET, 1, Material.WATER_BUCKET, "Uma tempestade por 10 minutos.",
                    RituaisMundo.chuva(), i(Material.KELP, 16), i(Material.LAPIS_LAZULI, 8)),
            mundo("Céu Limpo", Material.SUNFLOWER, 1, Material.SUNFLOWER, "Sem chuva por 20 minutos.",
                    RituaisMundo.ceuLimpo(), i(Material.GLOWSTONE, 4), i(Material.FEATHER, 8)),
            mundo("Chamar o Dia", Material.CLOCK, 1, Material.TORCH, "O sol nasce agora.",
                    RituaisMundo.hora(1000, "O sol nasce"), i(Material.TORCH, 32), i(Material.GOLD_INGOT, 4)),
            mundo("Chamar a Noite", Material.CLOCK, 1, Material.INK_SAC, "A noite cai agora.",
                    RituaisMundo.hora(13000, "A noite cai"), i(Material.INK_SAC, 8), i(Material.COAL_BLOCK, 2)),
            mundo("Lua Azul", Material.LAPIS_BLOCK, 1, Material.BLUE_STAINED_GLASS, "Até o amanhecer, os monstros soltam materiais raros (só à noite).",
                    RituaisMundo.luaAzul(), i(Material.PRISMARINE_CRYSTALS, 8), i(Material.GLOW_INK_SAC, 8), r(Raro.ALMA_DESPERTA, 1))
                    .com(Extra.NADA.efeito(RituaisMundo.luaAzul()).nivel(2).grupo(2)),
            mundo("Aura do Crescimento", Material.BONE_MEAL, 16, Material.WHEAT, "Por 2 horas, as plantas do seu território crescem o dobro.",
                    RituaisMundo.aura(RituaisMundo.Aura.CRESCIMENTO), i(Material.HAY_BLOCK, 8), i(Material.EMERALD, 4), i(Material.MOSS_BLOCK, 8)),
            mundo("Aura da Paz", Material.WHITE_BANNER, 1, Material.WHITE_BANNER, "Por 2 horas, nenhum monstro nasce no seu território.",
                    RituaisMundo.aura(RituaisMundo.Aura.PAZ), i(Material.GOLDEN_APPLE, 2), i(Material.EMERALD, 8), i(Material.TORCH, 32)),
            mundo("Aura do Vigor", Material.SUGAR, 16, Material.SUGAR, "Por 2 horas, quem está no seu território ganha Pressa e Velocidade.",
                    RituaisMundo.aura(RituaisMundo.Aura.VIGOR), i(Material.REDSTONE_BLOCK, 4), i(Material.EMERALD, 4), i(Material.RABBIT_FOOT, 1)),
            mundo("Bênção da Aliança", Material.GOLDEN_APPLE, 1, Material.GOLDEN_APPLE, "Quem está perto ganha Força, Resistência e Regeneração por 30 minutos. 3 jogadores, cada um com uma maçã dourada.",
                    RituaisMundo.bencao(), i(Material.EMERALD_BLOCK, 4), i(Material.TOTEM_OF_UNDYING, 1), i(Material.EXPERIENCE_BOTTLE, 8))
                    .com(Extra.NADA.efeito(RituaisMundo.bencao()).grupo(3)),
            mundo("Despertar o Altar", Material.NETHER_STAR, 1, Material.END_CRYSTAL,
                    "O altar sobe para o nível II: uma segunda volta de 8 pedestais (a 5 blocos) e as receitas mais fortes.",
                    RituaisMundo.despertarAltar(), i(Material.CRYING_OBSIDIAN, 8), r(Raro.ALMA_DESPERTA, 2), i(Material.LAPIS_BLOCK, 8), i(Material.END_CRYSTAL, 2))
                    .com(Extra.NADA.efeito(RituaisMundo.despertarAltar()).sac(EntityType.COW, 2, "2 vacas")),
            mundo("Ascender o Altar", Material.NETHER_STAR, 2, Material.BEACON,
                    "O altar sobe para o nível III: uma terceira volta de 8 pedestais (a 7 blocos) e as receitas lendárias.",
                    RituaisMundo.ascenderAltar(), r(Raro.ALMA_DESPERTA, 4), r(Raro.ESSENCIA_PRIMORDIAL, 1), i(Material.BEACON, 1),
                    i(Material.CRYING_OBSIDIAN, 16), i(Material.END_CRYSTAL, 4), i(Material.ECHO_SHARD, 8))
                    .com(Extra.NADA.efeito(RituaisMundo.ascenderAltar()).nivel(2).sac(EntityType.ENDERMAN, 2, "2 endermen")),
            mundo("Chuva de Meteoros", Material.SPYGLASS, 1, Material.MAGMA_BLOCK,
                    "Por 1 minuto, meteoritos caem em volta do altar (de 12 a 40 blocos). Quebre as pedras para pegar o que trazem.",
                    RituaisMundo.meteoros(), i(Material.MAGMA_BLOCK, 16), i(Material.IRON_BLOCK, 2), i(Material.GOLD_BLOCK, 1), i(Material.FIRE_CHARGE, 8))
                    .com(Extra.NADA.efeito(RituaisMundo.meteoros()).nivel(2)),
            mundo("Abrir um Portal", Material.ENDER_EYE, 1, Material.END_PORTAL_FRAME,
                    "Um portal do seu rank de caçador se abre perto do altar (5% de sair Vermelho). Você tem 30 s para entrar antes dos outros. Um a cada 6 horas.",
                    RituaisMundo.portal(), i(Material.OBSIDIAN, 8), i(Material.AMETHYST_BLOCK, 4), i(Material.ENDER_PEARL, 4), i(Material.LAPIS_BLOCK, 2)),
            mundo("Chamar o Mercador", Material.EMERALD, 16, Material.LEAD,
                    "O Mercador Itinerante arma a tenda perto de você agora.",
                    RituaisMundo.mercador(), i(Material.LEAD, 2), i(Material.HAY_BLOCK, 4), i(Material.EMERALD_BLOCK, 1))));
        l.addAll(elevacoes());
        return List.copyOf(l);
    }

    public List<Component> lore() {
        List<Component> l = new ArrayList<>();
        l.add(Component.text(descricao, NamedTextColor.GRAY));
        l.add(Component.empty());
        l.add(Component.text("Na mão: " + (qtdCatalisador > 1 ? qtdCatalisador + "x " : "") + nomeCatalisador, NamedTextColor.YELLOW));
        l.add(Component.text("Nos pedestais (um em cada):", NamedTextColor.AQUA));
        for (Ingrediente g : pedestais) l.add(Component.text(" • " + g.nomeBonito(), NamedTextColor.WHITE));
        if (extra.sacrificio() != null) l.add(Component.text("Sacrifício: " + extra.sacrificio().nome() + " (a até 5 blocos do altar)", NamedTextColor.RED));
        if (extra.jogadores() > 1) l.add(Component.text(extra.jogadores() + " jogadores, cada um com o catalisador na mão.", NamedTextColor.GOLD));
        if (extra.nivelAltar() > 1) l.add(Component.text("Precisa do altar nível " + romano(extra.nivelAltar()) + ".", NamedTextColor.LIGHT_PURPLE));
        if (unica) l.add(Component.text("Uma por jogador.", NamedTextColor.LIGHT_PURPLE));
        if (desperto != null) l.add(Component.text("O chefe aparece em cima do altar: prepare-se.", NamedTextColor.RED));
        if (nome.startsWith("Elevar guardião")) {
            l.add(Component.text("Tributo (8 por nível):", NamedTextColor.AQUA));
            for (Guardioes.Tipo t : Guardioes.Tipo.values()) l.add(Component.text(" • " + t.nome + ": " + bonito(t.tributo), NamedTextColor.WHITE));
        }
        return l;
    }
}
