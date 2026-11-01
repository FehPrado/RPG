package br.rpgatributos.ajuda;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * O Limiar (3.20): o livro de missões, no estilo dos modpacks. Com o mod de apoio ele é um mapa
 * (missões em círculos ligadas por linhas, capítulos na barra da esquerda, arrastar e zoom); sem o mod,
 * um menu de baú com as mesmas missões. Abre com {@code /limiar} ou com a tecla do mod (H).
 * <ul>
 *   <li><b>O Começo:</b> os 12 passos da Jornada do Aventureiro (as recompensas continuam vindo dela).</li>
 *   <li><b>Caminhos do Ofício:</b> as mecânicas da 3.15 à 3.19 (cozinha, luta pesada, oficina, natureza).</li>
 *   <li><b>Além do Limiar:</b> as dimensões (Abismo, Luas, Mundo das Almas).</li>
 * </ul>
 * Uma missão abre quando as anteriores dela estão feitas e se completa sozinha (a cada 3 s) quando o
 * contador dela chega no alvo; a recompensa vai direto para o inventário.
 */
public final class Limiar implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xF2C14E);

    /** Um capítulo do livro. */
    public record Capitulo(String id, String nome, Material icone, String descricao) { }

    /**
     * Uma missão: posição no mapa (em casas), missões anteriores, o contador dos títulos que ela olha
     * (vários separados por "|" somam) e o alvo, o assunto do Guia, o prêmio.
     */
    public record Missao(String id, String capitulo, String nome, String descricao, Material icone, String visual, int x, int y,
                         List<String> antes, String contador, int alvo, String topico, String premioTexto,
                         Supplier<List<ItemStack>> premio, boolean grande) { }

    public static final List<Capitulo> CAPITULOS = List.of(
            new Capitulo("comeco", "O Começo", Material.GRASS_BLOCK, "Os primeiros passos: a Jornada do Aventureiro."),
            new Capitulo("oficio", "Caminhos do Ofício", Material.ANVIL, "Cozinha, luta pesada, a oficina do ferreiro e a natureza."),
            new Capitulo("alem", "Além do Limiar", Material.ENDER_EYE, "O Abismo, as Luas e o Mundo das Almas."));

    private static List<ItemStack> esm(int n) { return List.of(new ItemStack(Material.EMERALD, n)); }

    private static Missao m(String id, String cap, String nome, String desc, Material icone, String visual, int x, int y, String antes,
                            String contador, int alvo, String topico, String premioTexto, Supplier<List<ItemStack>> premio, boolean grande) {
        return new Missao(id, cap, nome, desc, icone, visual, x, y, antes == null ? List.of() : Arrays.asList(antes.split(",")),
                contador, alvo, topico, premioTexto, premio, grande);
    }

    public static final List<Missao> MISSOES = new ArrayList<>();

    static {
        // ---------- O Começo: a Jornada (as posições fazem um zigue-zague) ----------
        MISSOES.add(m("comeco_raiz", "comeco", "Bem-vindo ao Limiar", "Cada missão abre a próxima. Clique numa para ver como fazer no Guia.",
                Material.GRASS_BLOCK, null, 0, 0, null, null, 0, "atributos", "", List::of, true));
        String antes = "comeco_raiz";
        for (int i = 0; i < Jornada.PASSOS.size(); i++) {
            Jornada.Passo ps = Jornada.PASSOS.get(i);
            String id = "jornada_" + i;
            MISSOES.add(m(id, "comeco", ps.nome(), ps.objetivo(), ps.icone(), null, 2 + 2 * i, i % 2 == 0 ? -1 : 1, antes,
                    null, 0, ps.topico(), ps.recompensa(), List::of, i == Jornada.PASSOS.size() - 1));
            antes = id;
        }

        // ---------- Caminhos do Ofício (3.15–3.19): quatro ramos ----------
        MISSOES.add(m("oficio_raiz", "oficio", "O Ofício", "Quatro caminhos: a panela, a luta, a forja e o mato. Faça em qualquer ordem.",
                Material.ANVIL, null, 0, 0, null, null, 0, "oficina", "", List::of, true));
        // Cozinha (em cima)
        MISSOES.add(m("estacao", "oficio", "Da Estação", "Colha uma fruta do pomar ou uma coleta da estação. Tudo vem com qualidade ★.",
                Material.APPLE, "rpgatributos:fruta_laranja", 0, -2, "oficio_raiz", "coletas|frutas", 1, "coletas", "4 esmeraldas", () -> esm(4), false));
        MISSOES.add(m("mel", "oficio", "Mel das Flores", "Tire 3 méis (colmeia ou ninho). 3 flores iguais perto dão o mel delas.",
                Material.HONEY_BOTTLE, "rpgatributos:mel_rubro", -1, -4, "estacao", "meis", 3, "colmeia", "4 garrafas de vidro",
                () -> List.of(new ItemStack(Material.GLASS_BOTTLE, 4)), false));
        MISSOES.add(m("tempero", "oficio", "Uma Pitada de Erva", "Na Cozinha, shift + clique num prato com uma erva a mais: ele sobe uma qualidade.",
                Material.QUARTZ, "rpgatributos:erva_salvia", 1, -4, "estacao", "pratos_temperados", 1, "cozinha", "6 esmeraldas", () -> esm(6), false));
        MISSOES.add(m("banquete", "oficio", "Banquete", "Cozinhe um banquete (vale para todos em volta).",
                Material.SUSPICIOUS_STEW, "rpgatributos:prato_banquete_dragao", 0, -6, "mel,tempero", "banquete", 1, "cozinha", "1 maçã dourada",
                () -> List.of(new ItemStack(Material.GOLDEN_APPLE)), true));
        // Luta (à direita)
        MISSOES.add(m("aparar_amarelo", "oficio", "Apare o Amarelo", "Um inimigo forte brilhou amarelo? Levante o escudo bem na hora: o equilíbrio dele quebra.",
                Material.SHIELD, null, 2, 0, "oficio_raiz", "pesados_aparados", 1, "lutapesada", "8 esmeraldas", () -> esm(8), false));
        MISSOES.add(m("execucao", "oficio", "Execução", "Com o equilíbrio do inimigo quebrado: agachado + golpe.",
                Material.NETHERITE_AXE, null, 4, -1, "aparar_amarelo", "execucoes", 1, "lutapesada", "1 Fragmento de Forja",
                () -> List.of(Raro.FRAGMENTO_DE_FORJA.criar(1)), false));
        MISSOES.add(m("exorcista", "oficio", "Coisas da Noite", "Derrote 5 assombros (Espantalho, Banshee, Golem de Musgo, Aranha-Tecelã, Afogado-Âncora).",
                Material.CARVED_PUMPKIN, null, 4, 1, "aparar_amarelo", "assombros", 5, "assombros", "10 esmeraldas", () -> esm(10), false));
        MISSOES.add(m("golpe_lenda", "oficio", "A Arma Desperta", "Com uma arma Lendária (memória 1.000): agachado + clique direito solta o golpe dela.",
                Material.GOLDEN_SWORD, null, 6, -1, "execucao", "golpes_da_lenda", 1, "memoria", "1 Pedra de Proteção",
                () -> List.of(Raro.PEDRA_DE_PROTECAO.criar(1)), false));
        MISSOES.add(m("cavaleiro", "oficio", "O Cavaleiro Sem Cabeça", "Em noite de lua cheia, ande pela estrada... e derrube o Cavaleiro.",
                Material.JACK_O_LANTERN, null, 6, 1, "exorcista", "cavaleiro", 1, "assombros", "1 Essência Primordial",
                () -> List.of(Raro.ESSENCIA_PRIMORDIAL.criar(1)), true));
        // Forja (embaixo)
        MISSOES.add(m("martelo", "oficio", "Três Batidas", "Na Forja do Ferreiro, um clique abre a barra do martelo. Acerte as 3 no dourado.",
                Material.ANVIL, null, 0, 2, "oficio_raiz", "martelo_perfeito", 1, "oficina", "8 lingotes de ferro",
                () -> List.of(new ItemStack(Material.IRON_INGOT, 8)), false));
        MISSOES.add(m("liga", "oficio", "A Primeira Liga", "Monte o Cadinho (alto-forno + caldeirão + lava) e funda uma liga.",
                Material.BLAST_FURNACE, null, -1, 4, "martelo", "ligas", 1, "oficina", "8 carvões", () -> List.of(new ItemStack(Material.COAL, 8)), false));
        MISSOES.add(m("tempera", "oficio", "Em Brasa", "Monte a Tina de Têmpera e tempere uma peça que acabou de sair da Forja.",
                Material.WATER_BUCKET, null, 1, 4, "martelo", "temperas", 1, "oficina", "6 esmeraldas", () -> esm(6), false));
        MISSOES.add(m("treino", "oficio", "Com o Mestre", "Treine com um Mestre de Armas numa vila (tela das técnicas).",
                Material.TARGET, null, 0, 6, "liga,tempera", "treinos", 1, "tecnicas", "1 Fragmento de Forja",
                () -> List.of(Raro.FRAGMENTO_DE_FORJA.criar(1)), true));
        // Natureza (à esquerda)
        MISSOES.add(m("caca", "oficio", "Primeira Caça", "Cace um cervo, uma corça ou um javali (chegue agachado: eles fogem de quem corre).",
                Material.BONE, "rpgatributos:raro_galhada", -2, 0, "oficio_raiz", "caca", 1, "fauna", "4 esmeraldas", () -> esm(4), false));
        MISSOES.add(m("borboletas", "oficio", "Asas no Pote", "Pegue 3 borboletas ou mariposas com uma garrafa de vidro.",
                Material.PAPER, "rpgatributos:fauna_borboleta_azul", -4, -1, "caca", "borboletas", 3, "fauna", "4 garrafas de vidro",
                () -> List.of(new ItemStack(Material.GLASS_BOTTLE, 4)), false));
        MISSOES.add(m("tartaruga", "oficio", "Carona Lenta", "Ache a Tartaruga-Gigante numa praia e suba nela.",
                Material.TURTLE_SCUTE, null, -4, 1, "caca", "tartaruga", 1, "fauna", "6 esmeraldas", () -> esm(6), false));
        MISSOES.add(m("lendas_natureza", "oficio", "Lenda da Natureza", "Encontre um dos cinco animais lendários (siga os boatos).",
                Material.GOAT_HORN, null, -6, 0, "borboletas,tartaruga", "lendarios_achados", 1, "natureza", "16 esmeraldas", () -> esm(16), true));

        // ---------- Além do Limiar: as dimensões ----------
        MISSOES.add(m("alem_raiz", "alem", "O Limiar", "Do outro lado há um Abismo, cinco luas e o lugar para onde as almas vão.",
                Material.ENDER_EYE, null, 0, 0, null, null, 0, "abismo", "", List::of, true));
        MISSOES.add(m("fenda", "alem", "A Fenda", "Desça ao Abismo e minere um mineral de lá.",
                Material.AMETHYST_SHARD, "rpgatributos:raro_cristal_abissal", 0, 2, "alem_raiz", "minerais_abismo", 1, "abismo", "8 esmeraldas", () -> esm(8), false));
        MISSOES.add(m("ruinas", "alem", "Os Antigos", "Ache uma ruína dos Antigos na parede do Abismo.",
                Material.CHISELED_STONE_BRICKS, null, 0, 4, "fenda", "ruinas_antigos", 1, "abismo", "10 esmeraldas", () -> esm(10), false));
        MISSOES.add(m("vigia", "alem", "O Vigia do Fundo", "Desça até o Hangar no Fundo e vença o Vigia: ele guarda o Motor.",
                Material.SCULK_SHRIEKER, null, 0, 6, "ruinas", "vigia_fundo", 1, "abismo", "1 Essência Primordial",
                () -> List.of(Raro.ESSENCIA_PRIMORDIAL.criar(1)), true));
        MISSOES.add(m("decolagem", "alem", "Decolagem", "Monte o foguete dos Antigos e pouse numa lua.",
                Material.FIREWORK_ROCKET, null, 3, 6, "vigia", "pousos_lua", 1, "luas", "16 esmeraldas", () -> esm(16), false));
        MISSOES.add(m("cinco_luas", "alem", "Cinco Luas", "Pouse nas cinco luas.",
                Material.END_STONE, null, 5, 6, "decolagem", "luas_visitadas", 5, "luas", "1 Pedra de Proteção e 1 Fragmento de Forja",
                () -> List.of(Raro.PEDRA_DE_PROTECAO.criar(1), Raro.FRAGMENTO_DE_FORJA.criar(1)), true));
        MISSOES.add(m("outro_lado", "alem", "O Outro Lado", "Lanterna das Almas na mão esquerda, Punhal na direita, agachado: a alma sai do corpo.",
                Material.SOUL_LANTERN, null, -3, 2, "alem_raiz", "viagens_almas", 1, "almas", "8 esmeraldas", () -> esm(8), false));
        MISSOES.add(m("almas_penadas", "alem", "Almas Penadas", "Recolha 10 Almas Penadas com a lanterna.",
                Material.GHAST_TEAR, null, -5, 2, "outro_lado", "almas_coletadas", 10, "almas", "10 esmeraldas", () -> esm(10), false));
        MISSOES.add(m("juiz", "alem", "A Balança Quebrada", "Toque o sino do Tribunal e vença o Juiz (a Lâmina Espectral corta o escudo dele).",
                Material.WITHER_SKELETON_SKULL, null, -5, 4, "almas_penadas", "juiz_julgado", 1, "almas", "1 Essência Primordial",
                () -> List.of(Raro.ESSENCIA_PRIMORDIAL.criar(1)), true));
        MISSOES.add(m("diario_fim", "alem", "O Fim do Diário", "Leia os capítulos XI a XV do Diário dos Antigos.",
                Material.WRITTEN_BOOK, null, -3, 4, "juiz", "diario_fim", 5, "almas", "20 esmeraldas", () -> esm(20), true));
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kFeitas;

    public Limiar(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kFeitas = new NamespacedKey(plugin, "limiar_feitas");
    }

    public static Missao porId(String id) {
        for (Missao m : MISSOES) if (m.id().equals(id)) return m;
        return null;
    }

    // =====================================================================
    //  Estado
    // =====================================================================

    private Set<String> feitas(Player p) {
        String s = p.getPersistentDataContainer().get(kFeitas, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(Arrays.asList(s.split(",")));
    }

    public boolean feita(Player p, Missao m) {
        return feita(p, m, feitas(p), plugin.ajuda().jornada().feitos(p));
    }

    private static boolean feita(Player p, Missao m, Set<String> feitas, int jornada) {
        if (m.id().endsWith("_raiz")) return true;
        if (m.id().startsWith("jornada_")) return Integer.parseInt(m.id().substring(8)) < jornada;
        return feitas.contains(m.id());
    }

    private static boolean aberta(Player p, Missao m, Set<String> feitas, int jornada) {
        for (String a : m.antes()) {
            Missao am = porId(a);
            if (am != null && !feita(p, am, feitas, jornada)) return false;
        }
        return true;
    }

    private int contador(Player p, Missao m) {
        if (m.contador() == null) return 0;
        int n = 0;
        for (String c : m.contador().split("\\|")) n += plugin.titulos().contador(p, c);
        return n;
    }

    /** A cada 3 s (pelo Guia): completa as missões cumpridas e entrega o prêmio. */
    public void verificar(Player p) {
        Set<String> feitas = feitas(p);
        int jornada = plugin.ajuda().jornada().feitos(p);
        List<Missao> novas = new ArrayList<>();
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Missao m : MISSOES) {
                if (m.contador() == null || feita(p, m, feitas, jornada) || !aberta(p, m, feitas, jornada)) continue;
                if (contador(p, m) < m.alvo()) continue;
                feitas.add(m.id());
                novas.add(m);
                mudou = true;
            }
        }
        if (novas.isEmpty()) return;
        p.getPersistentDataContainer().set(kFeitas, PersistentDataType.STRING, String.join(",", feitas));
        for (Missao m : novas) {
            for (ItemStack it : m.premio().get()) p.getInventory().addItem(it).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
            p.sendMessage(Component.text("✦ Limiar: ", COR, TextDecoration.BOLD)
                    .append(Component.text("missão concluída — " + m.nome() + (m.premioTexto().isEmpty() ? "" : " (+" + m.premioTexto() + ")"), NamedTextColor.WHITE)
                            .decoration(TextDecoration.BOLD, false)));
        }
        Missao ultima = novas.getLast();
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.3f);
        p.showTitle(Title.title(Component.text("✦ " + ultima.nome(), COR, TextDecoration.BOLD),
                Component.text("Missão do Limiar concluída" + (novas.size() > 1 ? " (+" + (novas.size() - 1) + ")" : ""), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2200), Duration.ofMillis(500))));
        // Com o mod: o mapa aberto se atualiza (e o mod mostra o aviso no canto).
        enviar(p, false, ultima.nome());
    }

    // =====================================================================
    //  Para o mod: o livro em JSON
    // =====================================================================

    private JsonObject json(Player p, boolean abrir, String concluida) {
        Set<String> feitas = feitas(p);
        int jornada = plugin.ajuda().jornada().feitos(p);
        JsonObject o = new JsonObject();
        o.addProperty("abrir", abrir);
        o.addProperty("titulo", "Limiar");
        if (concluida != null) o.addProperty("concluida", concluida);
        int total = 0, feitasN = 0;
        JsonArray caps = new JsonArray();
        for (Capitulo c : CAPITULOS) {
            JsonObject co = new JsonObject();
            co.addProperty("id", c.id());
            co.addProperty("nome", c.nome());
            co.addProperty("icone", c.icone().getKey().toString());
            co.addProperty("desc", c.descricao());
            JsonArray ms = new JsonArray();
            int capF = 0, capT = 0;
            for (Missao m : MISSOES) {
                if (!m.capitulo().equals(c.id())) continue;
                boolean f = feita(p, m, feitas, jornada);
                boolean a = aberta(p, m, feitas, jornada);
                JsonObject mo = new JsonObject();
                mo.addProperty("id", m.id());
                mo.addProperty("nome", m.nome());
                mo.addProperty("desc", m.descricao());
                mo.addProperty("topico", m.topico());
                mo.addProperty("icone", m.icone().getKey().toString());
                if (m.visual() != null) mo.addProperty("cmd", m.visual());
                mo.addProperty("x", m.x());
                mo.addProperty("y", m.y());
                JsonArray antes = new JsonArray();
                m.antes().forEach(antes::add);
                mo.add("antes", antes);
                mo.addProperty("estado", f ? "feita" : a ? "aberta" : "travada");
                if (!m.premioTexto().isEmpty()) mo.addProperty("premio", m.premioTexto());
                if (m.contador() != null) mo.addProperty("prog", Math.min(contador(p, m), m.alvo()) + "/" + m.alvo());
                mo.addProperty("grande", m.grande());
                ms.add(mo);
                if (!m.id().endsWith("_raiz")) {
                    capT++;
                    if (f) capF++;
                }
            }
            co.add("missoes", ms);
            co.addProperty("feitas", capF);
            co.addProperty("total", capT);
            caps.add(co);
            total += capT;
            feitasN += capF;
        }
        o.add("capitulos", caps);
        o.addProperty("feitas", feitasN);
        o.addProperty("total", total);
        return o;
    }

    private void enviar(Player p, boolean abrir, String concluida) {
        if (plugin.canalApoio() == null || !plugin.canalApoio().temMod(p)) return;
        plugin.canalApoio().enviar(p, "limiar " + json(p, abrir, concluida));
    }

    /** Abre o Limiar: o mapa no mod, ou o menu de baú sem ele. */
    public void abrir(Player p) {
        verificar(p);
        if (plugin.canalApoio() != null && plugin.canalApoio().temMod(p) && p.getListeningPluginChannels().contains("rpgatributos:dados")) {
            enviar(p, true, null);
            return;
        }
        abrirMenu(p, 0);
    }

    /** O mod pediu: "limiar:guia:<assunto>" ao clicar numa missão. */
    public void doMod(Player p, String acao) {
        if (acao.startsWith("limiar:guia:")) {
            String topico = acao.substring(12);
            if (topico.matches("[a-z_]{1,32}")) p.performCommand("guia " + topico);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    // =====================================================================
    //  Sem o mod: o menu de baú
    // =====================================================================

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        int capitulo;
        final List<Missao> slots = new ArrayList<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int[] S_CAPS = {46, 49, 52};

    public void abrirMenu(Player p, int cap) {
        Capitulo c = CAPITULOS.get(cap);
        Tela t = new Tela();
        t.capitulo = cap;
        t.inv = Bukkit.createInventory(t, 54, Component.text("✦ Limiar · " + c.nome()));
        Set<String> feitas = feitas(p);
        int jornada = plugin.ajuda().jornada().feitos(p);
        int i = 0;
        for (Missao m : MISSOES) {
            if (!m.capitulo().equals(c.id()) || m.id().endsWith("_raiz") || i >= 45) continue;
            boolean f = feita(p, m, feitas, jornada), a = aberta(p, m, feitas, jornada);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(m.descricao(), NamedTextColor.GRAY));
            if (m.contador() != null) lore.add(Component.text("Progresso: " + Math.min(contador(p, m), m.alvo()) + "/" + m.alvo(), NamedTextColor.WHITE));
            if (!m.premioTexto().isEmpty()) lore.add(Component.text("Prêmio: " + m.premioTexto(), COR));
            lore.add(Component.empty());
            lore.add(f ? Component.text("✔ Feita", NamedTextColor.GREEN) : a ? Component.text("◆ Aberta: clique para ver como", NamedTextColor.YELLOW)
                    : Component.text("✖ Faça as anteriores primeiro", NamedTextColor.DARK_GRAY));
            ItemStack it = new ItemStack(f ? m.icone() : a ? m.icone() : Material.GRAY_DYE);
            it.editMeta(meta -> {
                meta.displayName(Component.text((f ? "✔ " : "") + m.nome(), f ? NamedTextColor.GREEN : a ? COR : NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, m.grande()));
                meta.lore(lore.stream().map(x -> x.decoration(TextDecoration.ITALIC, false)).toList());
                meta.addItemFlags(ItemFlag.values());
                if (f) meta.setEnchantmentGlintOverride(true);
            });
            t.inv.setItem(i++, it);
            t.slots.add(m);
        }
        for (int k = 0; k < CAPITULOS.size(); k++) {
            Capitulo ck = CAPITULOS.get(k);
            int f = 0, tot = 0;
            for (Missao m : MISSOES) {
                if (!m.capitulo().equals(ck.id()) || m.id().endsWith("_raiz")) continue;
                tot++;
                if (feita(p, m, feitas, jornada)) f++;
            }
            int ff = f, tt = tot;
            boolean atual = k == cap;
            ItemStack it = new ItemStack(ck.icone());
            it.editMeta(meta -> {
                meta.displayName(Component.text(ck.nome() + " (" + ff + "/" + tt + ")", atual ? COR : NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(Component.text(ck.descricao(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                meta.addItemFlags(ItemFlag.values());
                if (atual) meta.setEnchantmentGlintOverride(true);
            });
            t.inv.setItem(S_CAPS[k], it);
        }
        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(meta -> {
            meta.displayName(Component.text("✦ Limiar", COR, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("As missões se completam sozinhas e o prêmio vai", NamedTextColor.GRAY),
                            Component.text("direto para o inventário. Com o mod de apoio,", NamedTextColor.GRAY),
                            Component.text("o Limiar vira um mapa de missões (tecla H).", NamedTextColor.GRAY))
                    .stream().map(x -> x.decoration(TextDecoration.ITALIC, false)).toList());
        });
        t.inv.setItem(45, info);
        p.openInventory(t.inv);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        int s = e.getRawSlot();
        for (int k = 0; k < S_CAPS.length; k++) {
            if (S_CAPS[k] == s && k != t.capitulo) {
                abrirMenu(p, k);
                return;
            }
        }
        if (s >= 0 && s < t.slots.size()) {
            p.closeInventory();
            p.performCommand("guia " + t.slots.get(s).topico());
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    /** Ids que existem (para validar os "antes" nos testes). */
    public static Set<String> ids() {
        Set<String> s = new HashSet<>();
        for (Missao m : MISSOES) s.add(m.id());
        return s;
    }
}
