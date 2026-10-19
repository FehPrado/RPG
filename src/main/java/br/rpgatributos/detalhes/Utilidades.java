package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.Locale;

/**
 * As utilidades do dia a dia (2.65): Chave de Construtor, Funil com filtro, Regador, Lanterna de
 * mão, Bancada de Bolso, Balde de Gelo, Trena e Cinto de Ferramentas. Aqui ficam os itens e as
 * receitas; cada uma tem a sua classe com o que ela faz.
 */
public final class Utilidades {

    /** Todos os itens das utilidades têm esta marca, com o id da utilidade. */
    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "utilidade");
    static final TextColor COR = TextColor.color(0xD7C49E);

    /** Os itens: id, nome, material de base, visual e o texto. */
    public enum Item {
        CHAVE("chave", "Chave de Construtor", Material.PAPER,
                "Clique direito num bloco: gira (escadas, troncos, alçapões, cabeças...).",
                "Agachado: vira de cabeça para baixo (escada, laje, alçapão)."),
        REGADOR("regador", "Regador", Material.PAPER,
                "Clique na água para encher (20 regadas).",
                "Clique na plantação: ajuda a crescer em volta (3x3)."),
        BANCADA("bancada", "Bancada de Bolso", Material.PAPER,
                "Clique direito: abre a bancada de trabalho.",
                "Agachado: pedra de amolar, tear, cortador, cartografia ou ferraria."),
        BALDE("balde_gelo", "Balde de Gelo", Material.BUCKET,
                "Guarda até 4 baldes de água ou de lava.",
                "Clique na fonte: enche. Agachado (ou sem fonte na mira): despeja."),
        TRENA("trena", "Trena", Material.PAPER,
                "Clique num bloco e depois noutro: mostra a medida.",
                "Agachado + clique: apaga."),
        CINTO("cinto", "Cinto de Ferramentas", Material.PAPER,
                "Clique direito: guarde até 9 ferramentas.",
                "Segure o cinto e bata num bloco: a ferramenta certa vem para a mão.");

        public final String id, nome, linha1, linha2;
        public final Material base;

        Item(String id, String nome, Material base, String linha1, String linha2) {
            this.id = id;
            this.nome = nome;
            this.base = base;
            this.linha1 = linha1;
            this.linha2 = linha2;
        }

        public ItemStack criar() {
            ItemStack i = new ItemStack(base);
            i.editMeta(m -> {
                m.itemName(Component.text(nome, COR));
                m.lore(List.of(
                        Component.text(linha1, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text(linha2, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                m.setMaxStackSize(1);
                m.getPersistentDataContainer().set(Utilidades.CHAVE, PersistentDataType.STRING, id);
                PacoteRecursos.marcar(m, id);
            });
            return i;
        }

        public boolean e(ItemStack i) {
            return i != null && !i.isEmpty() && i.hasItemMeta() && id.equals(i.getPersistentDataContainer().get(Utilidades.CHAVE, PersistentDataType.STRING));
        }

        public static Item porId(String s) {
            for (Item x : values()) if (x.id.equalsIgnoreCase(s) || x.name().equalsIgnoreCase(s)) return x;
            return null;
        }
    }

    private final RPGAtributos plugin;
    private final ChaveConstrutor chave;
    private final FunilFiltro funil;
    private final Regador regador;
    private final LanternaMao lanterna;
    private final BancadaBolso bancada;
    private final BaldeGelo balde;
    private final Trena trena;
    private final CintoFerramentas cinto;

    public Utilidades(RPGAtributos plugin) {
        this.plugin = plugin;
        chave = new ChaveConstrutor(plugin);
        funil = new FunilFiltro(plugin);
        regador = new Regador(plugin);
        lanterna = new LanternaMao(plugin);
        bancada = new BancadaBolso(plugin);
        balde = new BaldeGelo(plugin);
        trena = new Trena(plugin);
        cinto = new CintoFerramentas(plugin);
    }

    public void registrar(PluginManager pm) {
        pm.registerEvents(chave, plugin);
        pm.registerEvents(funil, plugin);
        pm.registerEvents(regador, plugin);
        pm.registerEvents(lanterna, plugin);
        pm.registerEvents(bancada, plugin);
        pm.registerEvents(balde, plugin);
        pm.registerEvents(trena, plugin);
        pm.registerEvents(cinto, plugin);
    }

    /** A cada tick: a luz da lanterna na mão e o desenho da trena. */
    public void tick() {
        lanterna.tick();
        trena.tick();
    }

    /** A cada segundo: o cinto devolve a ferramenta que ficou parada na mão. */
    public void segundo() {
        cinto.segundo();
    }

    public void desligar() {
        lanterna.apagarTodas();
        cinto.devolverTodos();
    }

    public void registrarReceitas() {
        RecipeChoice ferro = new RecipeChoice.MaterialChoice(Material.IRON_INGOT);
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "util_chave"), Item.CHAVE.criar());
        r.shape("I I", " S ", " S ");
        r.setIngredient('I', ferro);
        r.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(r);

        r = new ShapedRecipe(new NamespacedKey(plugin, "util_regador"), Regador.encher(Item.REGADOR.criar(), 0));
        r.shape("C  ", "CBC", " C ");
        r.setIngredient('C', Material.COPPER_INGOT);
        r.setIngredient('B', Material.BUCKET);
        Bukkit.addRecipe(r);

        r = new ShapedRecipe(new NamespacedKey(plugin, "util_bancada"), Item.BANCADA.criar());
        r.shape(" L ", "LTL");
        r.setIngredient('L', Material.LEATHER);
        r.setIngredient('T', Material.CRAFTING_TABLE);
        Bukkit.addRecipe(r);

        r = new ShapedRecipe(new NamespacedKey(plugin, "util_balde_gelo"), BaldeGelo.montar(Item.BALDE.criar(), null, 0));
        r.shape(" G ", "GBG");
        r.setIngredient('G', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.CRISTAL_DE_GELO.criar(1)));
        r.setIngredient('B', Material.BUCKET);
        Bukkit.addRecipe(r);

        r = new ShapedRecipe(new NamespacedKey(plugin, "util_trena"), Item.TRENA.criar());
        r.shape(" Y ", "ISI", " I ");
        r.setIngredient('Y', Material.YELLOW_DYE);
        r.setIngredient('I', ferro);
        r.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(r);

        r = new ShapedRecipe(new NamespacedKey(plugin, "util_cinto"), Item.CINTO.criar());
        r.shape("LLL", "SIS");
        r.setIngredient('L', Material.LEATHER);
        r.setIngredient('S', Material.STRING);
        r.setIngredient('I', ferro);
        Bukkit.addRecipe(r);
    }

    static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }
}
