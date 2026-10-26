package br.rpgatributos.titulos;

import br.rpgatributos.RPGAtributos;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conquistas do RPGAtributos (2.61) na tela de Conquistas do próprio jogo (tecla L): uma aba com
 * árvore, aviso no canto e mensagem no chat como as do vanilla. Cada uma acende quando um contador
 * dos títulos chega ao mínimo (os mesmos contadores que já existiam), então quem já tinha feito a
 * coisa ganha a conquista ao entrar. Carregadas pelo plugin a cada início (sem datapack).
 */
public final class Conquistas implements Listener {

    /** frame: task (comum), goal (objetivo, borda arredondada) ou challenge (desafio, roxa). */
    private record C(String id, String pai, String contador, int minimo, String titulo, String descricao, String icone, String frame) { }

    private static final String RAIZ = "raiz";
    private static final List<C> TODAS = new ArrayList<>();

    private static void c(String id, String pai, String contador, int minimo, String titulo, String descricao, String icone, String frame) {
        TODAS.add(new C(id, pai, contador, minimo, titulo, descricao, icone, frame));
    }

    static {
        // Forja e ofícios
        c("forja", RAIZ, "forjados", 1, "Primeira Forja", "Forje uma peça na Forja do Ferreiro", "minecraft:anvil", "task");
        c("obra_prima", "forja", "forjado_lendario", 1, "Obra-Prima", "Forje uma peça Lendária", "minecraft:golden_sword", "goal");
        c("lenda_da_bigorna", "obra_prima", "forjado_mitico", 1, "Lenda da Bigorna", "Forje uma peça Mítica", "minecraft:netherite_sword", "challenge");
        c("refino_maximo", "forja", "refino10", 1, "Polido ao Extremo", "Refine uma peça até +10", "minecraft:grindstone", "challenge");
        c("reciclagem", "forja", "reciclados", 1, "Nada se Perde", "Recicle um item na Bancada de Reciclagem", "minecraft:hopper", "task");
        c("mitrilo", "forja", "mitrilo", 1, "Metal das Profundezas", "Consiga Mitrilo", "minecraft:iron_ingot", "goal");
        c("prato", RAIZ, "pratos", 1, "Mão na Massa", "Cozinhe um prato na Cozinha", "minecraft:mushroom_stew", "task");
        c("banquete", "prato", "banquete", 1, "Banquete", "Sirva um banquete para todos em volta", "minecraft:suspicious_stew", "goal");
        // Combate
        c("combo", RAIZ, "combos", 1, "Primeiro Combo", "Solte um golpe de combo com uma arma", "minecraft:iron_sword", "task");
        c("golpe_perfeito", "combo", "golpes_perfeitos", 1, "No Tempo Certo", "Acerte um golpe perfeito", "minecraft:clock", "task");
        c("aparar", "combo", "aparadas", 1, "Aparar", "Apare um golpe levantando o escudo na hora certa", "minecraft:shield", "task");
        c("tecnica", "combo", "tecnicas", 1, "Segredo do Mestre", "Aprenda uma técnica secreta com um Mestre de Armas", "minecraft:nether_star", "goal");
        c("todas_tecnicas", "tecnica", "tecnicas", 20, "Herdeiro das Técnicas", "Aprenda as 20 técnicas secretas", "minecraft:beacon", "challenge");
        c("chefe", "combo", "chefes", 1, "Chefe Derrotado", "Vença um chefe do Altar Ritualístico", "minecraft:wither_skeleton_skull", "goal");
        c("chefe_mundial", "chefe", "chefe_mundial", 1, "Mata-Gigantes", "Vença um Chefe Mundial", "minecraft:dragon_head", "challenge");
        c("masmorra", "combo", "masmorras", 1, "Desbravador", "Termine uma masmorra", "minecraft:mossy_cobblestone", "goal");
        // Magia
        c("magia", RAIZ, "magias", 1, "Primeira Magia", "Lance uma magia do grimório", "minecraft:enchanted_book", "task");
        c("infusao", "magia", "infusoes", 1, "Corpo Arcano", "Faça uma infusão no Infusor Corpóreo", "minecraft:amethyst_shard", "goal");
        c("runa", "magia", "runas", 1, "Escrita Antiga", "Grave uma runa numa arma", "minecraft:chiseled_tuff", "task");
        // Exploração
        c("estrutura", RAIZ, "estruturas", 1, "Ruínas pelo Caminho", "Encontre uma estrutura do RPGAtributos", "minecraft:mossy_stone_bricks", "task");
        c("andarilho", "estrutura", "estruturas", 10, "Andarilho", "Encontre 10 estruturas", "minecraft:compass", "goal");
        c("local_oculto", "estrutura", "locais", 1, "Lugar Oculto", "Encontre um Local Oculto", "minecraft:chiseled_deepslate", "goal");
        c("primeiro_portal", "estrutura", "primeiro_portal", 1, "A Chave do Portal", "Ache a Chave do Primeiro Portal no Nether", "minecraft:crying_obsidian", "challenge");
        c("arco_iris", "estrutura", "arco_iris", 1, "Fim do Arco-Íris", "Ache o Pote do Arco-Íris", "minecraft:decorated_pot", "goal");
        c("estrela", "estrutura", "pedidos_estrela", 1, "Pedido às Estrelas", "Faça um pedido a uma estrela cadente", "minecraft:nether_star", "task");
        c("mineral", "estrutura", "minerais_bioma", 1, "Pedra de Bioma", "Minere um mineral de bioma", "minecraft:raw_gold", "task");
        c("mineral_nether", "mineral", "minerais_nether", 1, "Pedra do Inferno", "Minere um mineral do Nether", "minecraft:magma_cream", "goal");
        c("geodo", "mineral", "geodos", 1, "Lapidário", "Lapide um cristal de geodo com o Cinzel", "minecraft:amethyst_cluster", "task");
        // Pesca
        c("peixe_raro", RAIZ, "peixes_raros", 1, "Peixe Raro", "Pesque um peixe raro", "minecraft:tropical_fish", "task");
        c("peixe_gelo", "peixe_raro", "peixes_do_gelo", 1, "Buraco no Gelo", "Pesque um peixe do gelo", "minecraft:packed_ice", "goal");
        c("peixe_lava", "peixe_raro", "peixes_lava", 1, "Pescador do Inferno", "Pesque na lava com a Vara Ígnea", "minecraft:lava_bucket", "goal");
        // Companheiros e vilas
        c("companheiro", RAIZ, "companheiros", 1, "Companheiro", "Ganhe um companheiro no Altar do Domador", "minecraft:lead", "task");
        c("companheiro_lendario", "companheiro", "companheiro_lendario", 1, "Companheiro Lendário", "Evolua um companheiro ao estágio III", "minecraft:golden_carrot", "goal");
        c("besta_errante", "companheiro", "besta_errante", 1, "Besta Errante", "Conquiste uma das Bestas Errantes", "minecraft:gold_block", "challenge");
        c("laco", RAIZ, "lacos_completos", 1, "Amigo da Vila", "Complete o laço com um morador", "minecraft:poppy", "goal");
        c("duelo", "laco", "duelos_vencidos", 1, "Duelista de Cartas", "Vença um Duelo de Cartas", "minecraft:paper", "task");
        c("lenda", RAIZ, "lendas", 1, "Portador de Lenda", "Tenha uma das Lendas do servidor", "minecraft:totem_of_undying", "challenge");
    }

    private final RPGAtributos plugin;
    private final Map<String, NamespacedKey> chaves = new LinkedHashMap<>();

    public Conquistas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static NamespacedKey chave(String id) {
        return new NamespacedKey("rpgatributos", "conquistas/" + id);
    }

    // =====================================================================
    //  Carregar no jogo
    // =====================================================================

    @SuppressWarnings("deprecation")
    public void carregar() {
        Map<Key, String> json = new LinkedHashMap<>();
        json.put(chave(RAIZ), raiz());
        for (C c : TODAS) json.put(chave(c.id()), json(c));
        int n = 0;
        for (Map.Entry<Key, String> en : json.entrySet()) {
            NamespacedKey k = (NamespacedKey) en.getKey();
            if (Bukkit.getAdvancement(k) != null) Bukkit.getUnsafe().removeAdvancement(k);
        }
        try {
            n = Bukkit.getUnsafe().loadAdvancements(json, false).size();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Não consegui carregar as conquistas: " + ex.getMessage());
        }
        for (C c : TODAS) chaves.put(c.id(), chave(c.id()));
        plugin.getLogger().info(n + " conquistas do RPGAtributos na tela de Conquistas do jogo.");
    }

    private static JsonObject texto(String s) {
        JsonObject o = new JsonObject();
        o.addProperty("text", s);
        return o;
    }

    private static JsonObject icone(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        return o;
    }

    private static String raiz() {
        JsonObject o = new JsonObject();
        JsonObject crit = new JsonObject(), tick = new JsonObject();
        tick.addProperty("trigger", "minecraft:tick");
        crit.add("entrou", tick);
        o.add("criteria", crit);
        JsonObject d = new JsonObject();
        d.add("title", texto("RPGAtributos"));
        d.add("description", texto("O Minecraft que você conhece, com mais história"));
        d.add("icon", icone("minecraft:written_book"));
        d.addProperty("background", "minecraft:block/dark_oak_planks");
        d.addProperty("show_toast", false);
        d.addProperty("announce_to_chat", false);
        o.add("display", d);
        return o.toString();
    }

    private static String json(C c) {
        JsonObject o = new JsonObject();
        o.addProperty("parent", "rpgatributos:conquistas/" + c.pai());
        JsonObject crit = new JsonObject(), imp = new JsonObject();
        imp.addProperty("trigger", "minecraft:impossible");
        crit.add("feito", imp);
        o.add("criteria", crit);
        JsonArray req = new JsonArray(), r1 = new JsonArray();
        r1.add("feito");
        req.add(r1);
        o.add("requirements", req);
        JsonObject d = new JsonObject();
        d.add("title", texto(c.titulo()));
        d.add("description", texto(c.descricao()));
        d.add("icon", icone(c.icone()));
        d.addProperty("frame", c.frame());
        d.addProperty("show_toast", true);
        d.addProperty("announce_to_chat", true);
        o.add("display", d);
        return o.toString();
    }

    // =====================================================================
    //  Dar a conquista
    // =====================================================================

    /** Chamado pelos títulos sempre que um contador muda. */
    public void aoContar(Player p, String contador, int valor) {
        if (br.rpgatributos.ModoTeste.ehTeste(p)) return;
        for (C c : TODAS) if (c.contador().equals(contador) && valor >= c.minimo()) dar(p, c);
    }

    private void dar(Player p, C c) {
        NamespacedKey k = chaves.get(c.id());
        Advancement a = k == null ? null : Bukkit.getAdvancement(k);
        if (a == null) return;
        AdvancementProgress pr = p.getAdvancementProgress(a);
        if (!pr.isDone()) pr.awardCriteria("feito");
    }

    /** Ao entrar: quem já tinha feito a coisa (antes da 2.61) ganha a conquista. */
    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            for (C c : TODAS) if (plugin.titulos().contador(p, c.contador()) >= c.minimo()) dar(p, c);
        }, 40L);
    }
}
