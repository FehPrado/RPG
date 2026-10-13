package br.rpgatributos.colonia;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Laços com os moradores (referência: Persona e Fire Emblem). Conversar uma vez por dia, dar o
 * presente favorito e atender pedidos aproximam você de um morador. A cada um dos 5 capítulos ele
 * conta um pedaço da história dele; no último, te ensina algo da profissão dele, para sempre.
 * O laço é entre um jogador e um morador. Fica em lacos.yml; os ensinamentos ficam no jogador.
 */
public final class Lacos implements Listener {

    /** Pontos para chegar a cada capítulo (1 a 5). */
    static final int[] CAPITULOS = {3, 8, 14, 21, 30};
    private static final TextColor COR = TextColor.color(0xFF8FA8);
    private static final NamespacedKey K_ENSINAMENTOS = new NamespacedKey("rpgatributos", "ensinamentos");
    private static final NamespacedKey K_VELOCIDADE = new NamespacedKey("rpgatributos", "ensinamento_tempo_livre");
    private static final NamespacedKey K_GUARDA = new NamespacedKey("rpgatributos", "ensinamento_postura_de_guarda");

    /** A história de cada profissão: de onde veio, o medo, o sonho, o segredo e o que ensina. */
    public record Historia(String origem, String medo, String sonho, String segredo, String ensinamento, String efeito, Material[] presentes) { }

    private static final Map<Profissao, Historia> HISTORIAS = new EnumMap<>(Profissao.class);

    private static void h(Profissao p, String origem, String medo, String sonho, String segredo, String ensinamento, String efeito, Material... presentes) {
        HISTORIAS.put(p, new Historia(origem, medo, sonho, segredo, ensinamento, efeito, presentes));
    }

    static {
        h(Profissao.DESEMPREGADO, "uma vila que a guerra apagou do mapa", "de nunca achar o lugar dele no mundo",
                "ter um ofício que alguém lembre depois que ele se for", "um caderno com desenhos de todas as casas da colônia",
                "Tempo Livre", "+5% de velocidade a pé, para sempre", Material.COOKIE, Material.DANDELION);
        h(Profissao.FAZENDEIRO, "um vale onde a chuva parou de cair por três anos", "de ver a terra secar outra vez",
                "plantar um pomar que dê fruta para os netos dos netos", "um saquinho com a última semente da fazenda da mãe",
                "Mão na Terra", "10% de chance de colheita em dobro", Material.PUMPKIN_PIE, Material.BONE_MEAL);
        h(Profissao.LENHADOR, "uma serraria no norte, onde o inverno dura metade do ano", "do barulho de uma árvore caindo do lado errado",
                "construir uma casa inteira com madeira que ele mesmo cortou", "o nome de cada árvore grande que ele não teve coragem de derrubar",
                "Corte Limpo", "machados se gastam pela metade", Material.COOKED_BEEF, Material.APPLE);
        h(Profissao.PESCADOR, "uma ilha que hoje está debaixo d'água", "de tempestade no mar aberto",
                "pescar o peixe que o avô jurava ter visto e ninguém acreditou", "um anzol de osso que o avô fez",
                "Paciência do Rio", "os peixes mordem a isca mais rápido", Material.COOKED_SALMON, Material.LANTERN);
        h(Profissao.MINERADOR, "as minas do sul, de onde saem os anões da Forja", "de escuro sem fim e de desabamento",
                "achar um veio que ninguém nunca viu", "uma pedra que brilha sozinha e que ele nunca mostrou a ninguém",
                "Faro de Veio", "8% de chance de minério em dobro", Material.BAKED_POTATO, Material.LANTERN);
        h(Profissao.COZINHEIRO, "a cozinha de um castelo que caiu", "de servir um prato e ver alguém não gostar",
                "abrir uma taverna com o nome da colônia", "a receita do ensopado da avó, que ele nunca acertou",
                "Tempero de Casa", "comer cura 1 coração a mais", Material.SUGAR, Material.HONEY_BOTTLE);
        h(Profissao.FERREIRO, "uma forja na fronteira, onde se faziam armas para os dois lados", "de ver uma lâmina que ele fez ferir um amigo",
                "forjar uma ferramenta que dure cem anos", "a primeira faca que fez, torta e cheia de marcas",
                "Ouvido do Ferro", "armas, ferramentas e armaduras se gastam 25% menos", Material.IRON_INGOT, Material.COAL);
        h(Profissao.TRATADOR, "uma fazenda de cavalos selvagens nas planícies", "de um animal sofrendo sem ele poder ajudar",
                "domar um cavalo que nunca aceitou ninguém", "o apito que acalma qualquer bicho, menos ele mesmo",
                "Voz Mansa", "seus companheiros evoluem 20% mais rápido", Material.WHEAT, Material.CARROT);
        h(Profissao.ALQUIMISTA, "uma torre de magos de onde ela foi expulsa", "de uma poção que deu errado e ninguém sabe",
                "descobrir um elixir que cure a Penumbra", "um frasco que ela nunca abriu, com o nome do mestre dela",
                "Gota a Mais", "poções que você bebe duram 25% mais", Material.NETHER_WART, Material.GLOWSTONE_DUST);
        h(Profissao.MERCADOR, "as caravanas do deserto, de oásis em oásis", "de voltar a ser pobre",
                "ver as caravanas passando pela colônia", "uma dívida antiga que ele ainda paga em segredo",
                "Olho para o Valor", "5% de chance de uma esmeralda ao derrotar um monstro", Material.EMERALD, Material.GOLD_NUGGET);
        h(Profissao.BIBLIOTECARIO, "um mosteiro que guardava livros proibidos", "de que as histórias se percam",
                "escrever a história desta colônia", "uma página arrancada de um livro que fala do Livro das Lendas",
                "Leitura Atenta", "+25% de experiência (das orbes)", Material.BOOK, Material.INK_SAC);
        h(Profissao.CONSTRUTOR, "uma cidade que ele ajudou a erguer e depois viu cair", "de altura, mesmo trabalhando nos andaimes",
                "levantar uma torre que dê para ver de qualquer lugar do mapa", "a planta de uma casa só para ele, que nunca construiu",
                "Mãos Firmes", "quedas tiram 3 de vida a menos", Material.BRICK, Material.COOKIE);
        h(Profissao.SOLDADO, "um exército que perdeu a última batalha", "de deixar alguém para trás de novo",
                "ver a colônia em paz por uma estação inteira", "o elmo de um amigo que ele carrega sempre",
                "Postura de Guarda", "+1 de armadura, para sempre", Material.BREAD, Material.IRON_NUGGET);
        h(Profissao.ARQUEIRO, "uma floresta onde aprendeu a caçar com o pai", "de errar a flecha que importa",
                "acertar um alvo do outro lado do vale", "uma flecha com a pena de uma ave que ninguém mais viu",
                "Olho Firme", "+10% de dano com flechas", Material.FEATHER, Material.ARROW);
        h(Profissao.CAVALEIRO, "uma ordem de cavaleiros que jurou proteger um rei que não existe mais", "de quebrar um juramento",
                "cavalgar até o fim do mundo e voltar", "o juramento escrito à mão, que ele ainda lê toda noite",
                "Rédeas Firmes", "+10% de dano montado", Material.GOLDEN_CARROT, Material.APPLE);
    }

    /** Um laço entre um jogador e um morador. */
    static final class Laco {
        int pontos, capitulo;
        long diaConversa = -1, diaPresente = -1;
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    /** jogador → morador → laço */
    private final Map<UUID, Map<UUID, Laco>> lacos = new HashMap<>();

    public Lacos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "lacos.yml");
    }

    // =====================================================================
    //  Salvar e carregar
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String j : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(j);
            if (s == null) continue;
            try {
                Map<UUID, Laco> m = lacos.computeIfAbsent(UUID.fromString(j), k -> new HashMap<>());
                for (String c : s.getKeys(false)) {
                    String[] v = s.getString(c, "").split(",");
                    if (v.length < 4) continue;
                    Laco l = new Laco();
                    l.pontos = Integer.parseInt(v[0]);
                    l.capitulo = Integer.parseInt(v[1]);
                    l.diaConversa = Long.parseLong(v[2]);
                    l.diaPresente = Long.parseLong(v[3]);
                    m.put(UUID.fromString(c), l);
                }
            } catch (IllegalArgumentException ignorado) {
                // linha estragada: pula
            }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (var en : lacos.entrySet()) {
            for (var l : en.getValue().entrySet()) {
                Laco x = l.getValue();
                y.set(en.getKey() + "." + l.getKey(), x.pontos + "," + x.capitulo + "," + x.diaConversa + "," + x.diaPresente);
            }
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não salvei lacos.yml: " + ex.getMessage());
        }
    }

    Laco laco(Player p, Colonia.Cidadao ci) {
        return lacos.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).computeIfAbsent(ci.entidade, k -> new Laco());
    }

    private static long dia(Player p) {
        return p.getWorld().getFullTime() / 24000;
    }

    static Historia historia(Colonia.Cidadao ci) {
        return HISTORIAS.getOrDefault(ci.profissao, HISTORIAS.get(Profissao.DESEMPREGADO));
    }

    // =====================================================================
    //  Aproximar
    // =====================================================================

    /**
     * Com quem é o laço: um morador da colônia (ensina algo da profissão no capítulo 5) ou um NPC
     * de vila (dá um presente único no capítulo 5).
     */
    public record Alvo(UUID id, String nome, Historia historia, Profissao profissao, java.util.function.Supplier<ItemStack> presente) { }

    private static Alvo alvo(Colonia.Cidadao ci) {
        return new Alvo(ci.entidade, ci.nome, historia(ci), ci.profissao, null);
    }

    private Laco laco(Player p, UUID id) {
        return lacos.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).computeIfAbsent(id, k -> new Laco());
    }

    void interagir(Player p, Colonia.Cidadao ci) {
        interagir(p, alvo(ci), "Sabe, a colônia anda mais bonita.", "Hoje o trabalho rendeu.", "Ouvi uns barulhos estranhos ontem à noite.",
                "Às vezes eu penso na minha terra.", "Obrigado por parar para conversar.", "Você parece cansado. Come alguma coisa!");
    }

    /** Clique no botão do laço: entrega o presente que está na mão ou conversa (uma vez por dia cada). */
    public void interagir(Player p, Alvo a, String... falas) {
        Laco l = laco(p, a.id());
        Historia h = a.historia();
        ItemStack mao = p.getInventory().getItemInMainHand();
        long hoje = dia(p);
        boolean favorito = false;
        for (Material m : h.presentes()) favorito |= mao.getType() == m;
        if (favorito) {
            if (l.diaPresente == hoje) {
                p.sendActionBar(Component.text(a.nome() + " já ganhou um presente seu hoje.", NamedTextColor.GRAY));
                return;
            }
            mao.setAmount(mao.getAmount() - 1);
            l.diaPresente = hoje;
            p.sendMessage(Component.text("♥ " + a.nome() + ": \"Para mim? Você lembrou!\"", COR));
            somar(p, a, 3);
            return;
        }
        if (l.diaConversa == hoje) {
            p.sendActionBar(Component.text("Vocês já conversaram hoje. Volte amanhã (ou traga um presente).", NamedTextColor.GRAY));
            return;
        }
        l.diaConversa = hoje;
        p.sendMessage(Component.text("♥ " + a.nome() + ": \"" + falas[ThreadLocalRandom.current().nextInt(falas.length)] + "\"", COR));
        somar(p, a, 1);
    }

    /** Soma pontos de laço (conversa, presente, pedido atendido) e abre o próximo capítulo, se chegou. */
    public void somar(Player p, Colonia.Cidadao ci, int pontos) {
        somar(p, alvo(ci), pontos);
    }

    public void somar(Player p, Alvo a, int pontos) {
        Laco l = laco(p, a.id());
        l.pontos += pontos;
        while (l.capitulo < CAPITULOS.length && l.pontos >= CAPITULOS[l.capitulo]) {
            l.capitulo++;
            capitulo(p, a, l.capitulo);
        }
        salvar();
    }

    private void capitulo(Player p, Alvo a, int n) {
        Historia h = a.historia();
        String texto = switch (n) {
            case 1 -> a.nome() + " conta que veio de " + h.origem() + ".";
            case 2 -> a.nome() + " confessa que tem medo " + h.medo() + ".";
            case 3 -> a.nome() + " diz que sonha em " + h.sonho() + ".";
            case 4 -> a.nome() + " te mostra, em segredo, " + h.segredo() + ".";
            default -> a.profissao() != null ? a.nome() + " te ensina o que mais sabe: " + h.ensinamento() + "."
                    : a.nome() + " te entrega " + h.ensinamento() + ": \"" + h.efeito() + "\"";
        };
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("♥ Laço com " + a.nome() + " · Capítulo " + n + " de 5", COR, TextDecoration.BOLD));
        p.sendMessage(Component.text("  " + texto, NamedTextColor.WHITE));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, n == 5 ? 1.6f : 1.2f);
        if (n == 5) {
            if (a.profissao() != null) {
                Set<String> ens = ensinamentos(p);
                if (ens.add(a.profissao().name())) {
                    p.getPersistentDataContainer().set(K_ENSINAMENTOS, PersistentDataType.STRING, String.join(",", ens));
                    p.sendMessage(Component.text("  ✦ Ensinamento aprendido: " + h.ensinamento() + " (" + h.efeito() + ").", NamedTextColor.GOLD));
                    aplicarPermanentes(p);
                } else {
                    p.sendMessage(Component.text("  Você já tinha esse ensinamento, mas agora tem um amigo a mais.", NamedTextColor.GRAY));
                }
            } else if (a.presente() != null) {
                ItemStack presente = a.presente().get();
                for (ItemStack sobra : p.getInventory().addItem(presente).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("  ✦ Presente: " + h.ensinamento() + ".", NamedTextColor.GOLD));
            }
            plugin.titulos().registrar(p, "lacos_completos", 1);
        }
        plugin.diario().marco(p, "laco_" + a.id() + "_" + n, "Laço com " + a.nome() + ": capítulo " + n);
    }

    // =====================================================================
    //  Ensinamentos (para sempre, no jogador)
    // =====================================================================

    Set<String> ensinamentos(Player p) {
        String s = p.getPersistentDataContainer().get(K_ENSINAMENTOS, PersistentDataType.STRING);
        Set<String> r = new HashSet<>();
        if (s != null && !s.isBlank()) for (String x : s.split(",")) r.add(x.trim());
        return r;
    }

    public boolean sabe(Player p, Profissao prof) {
        return p != null && ensinamentos(p).contains(prof.name());
    }

    /** Os ensinamentos que mexem em atributos (velocidade, armadura). */
    private void aplicarPermanentes(Player p) {
        modificador(p, Attribute.MOVEMENT_SPEED, K_VELOCIDADE, sabe(p, Profissao.DESEMPREGADO) ? 0.05 : 0, AttributeModifier.Operation.ADD_SCALAR);
        modificador(p, Attribute.ARMOR, K_GUARDA, sabe(p, Profissao.SOLDADO) ? 1 : 0, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void modificador(Player p, Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(k);
        if (v != 0) inst.addModifier(new AttributeModifier(k, v, op, EquipmentSlotGroup.ANY));
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        aplicarPermanentes(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (!e.isDropItems()) return;
        boolean colheita = Tag.CROPS.isTagged(b.getType()) && b.getBlockData() instanceof Ageable a && a.getAge() >= a.getMaximumAge();
        boolean minerio = b.getType().name().endsWith("_ORE");
        double chance = colheita && sabe(p, Profissao.FAZENDEIRO) ? 0.10 : minerio && sabe(p, Profissao.MINERADOR) ? 0.08 : 0;
        if (chance == 0 || ThreadLocalRandom.current().nextDouble() >= chance) return;
        for (ItemStack i : b.getDrops(p.getInventory().getItemInMainHand(), p)) b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), i);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGastar(PlayerItemDamageEvent e) {
        Player p = e.getPlayer();
        String tipo = e.getItem().getType().name();
        if (tipo.endsWith("_AXE") && sabe(p, Profissao.LENHADOR) && ThreadLocalRandom.current().nextBoolean()) {
            e.setCancelled(true);
            return;
        }
        if (sabe(p, Profissao.FERREIRO) && ThreadLocalRandom.current().nextDouble() < 0.25) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.FISHING || !sabe(e.getPlayer(), Profissao.PESCADOR)) return;
        var anzol = e.getHook();
        anzol.setWaitTime(Math.max(1, (int) (anzol.getMinWaitTime() * 0.7)), Math.max(2, (int) (anzol.getMaxWaitTime() * 0.7)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack i = e.getItem();
        if (i.getType().isEdible() && sabe(p, Profissao.COZINHEIRO)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && !p.isDead()) p.setHealth(Math.min(max.getValue(), p.getHealth() + 2));
            });
        }
        if (i.getItemMeta() instanceof PotionMeta pm && sabe(p, Profissao.ALQUIMISTA)) {
            List<org.bukkit.potion.PotionEffectType> tipos = new ArrayList<>();
            if (pm.getBasePotionType() != null) pm.getBasePotionType().getPotionEffects().forEach(x -> tipos.add(x.getType()));
            pm.getCustomEffects().forEach(x -> tipos.add(x.getType()));
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (var t : tipos) {
                    PotionEffect atual = p.getPotionEffect(t);
                    if (atual == null || atual.isInfinite()) continue;
                    p.addPotionEffect(atual.withDuration((int) (atual.getDuration() * 1.25)));
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Enemy) || !(e.getEntity().getKiller() instanceof Player p)) return;
        if (sabe(p, Profissao.MERCADOR) && ThreadLocalRandom.current().nextDouble() < 0.05) e.getDrops().add(new ItemStack(Material.EMERALD));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoXp(PlayerExpChangeEvent e) {
        if (e.getAmount() > 0 && sabe(e.getPlayer(), Profissao.BIBLIOTECARIO)) {
            double v = e.getAmount() * 1.25;
            e.setAmount((int) v + (ThreadLocalRandom.current().nextDouble() < v - (int) v ? 1 : 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p && sabe(p, Profissao.CONSTRUTOR)) {
            e.setDamage(Math.max(0, e.getDamage() - 3));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof AbstractArrow f && f.getShooter() instanceof Player p && sabe(p, Profissao.ARQUEIRO)) {
            e.setDamage(e.getDamage() * 1.10);
        } else if (e.getDamager() instanceof Player p && p.getVehicle() != null && sabe(p, Profissao.CAVALEIRO)) {
            e.setDamage(e.getDamage() * 1.10);
        }
    }

    // =====================================================================
    //  Para o menu do morador
    // =====================================================================

    /** O texto do botão do laço no menu do morador. */
    List<Component> descricao(Player p, Colonia.Cidadao ci) {
        return descricao(p, alvo(ci), "Atender um pedido dele: +2");
    }

    /** O texto do botão do laço (morador da colônia ou NPC de vila). */
    public List<Component> descricao(Player p, Alvo a, String extra) {
        Laco l = laco(p, a.id());
        Historia h = a.historia();
        boolean ensina = a.profissao() != null;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("♥".repeat(l.capitulo) + "♡".repeat(5 - l.capitulo) + "  Capítulo " + l.capitulo + " de 5", COR));
        if (l.capitulo < CAPITULOS.length) {
            lore.add(Component.text("Próximo capítulo: " + l.pontos + "/" + CAPITULOS[l.capitulo] + " pontos", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Laço completo. " + (ensina ? "Ensinamento: " : "Presente: ") + h.ensinamento(), NamedTextColor.GOLD));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Conversar: +1 (uma vez por dia)", NamedTextColor.GRAY));
        StringBuilder pres = new StringBuilder();
        for (Material m : h.presentes()) pres.append(pres.isEmpty() ? "" : " ou ").append(nomeItem(m));
        lore.add(Component.text("Presente favorito (" + pres + "): +3 por dia", NamedTextColor.GRAY));
        if (extra != null) lore.add(Component.text(extra, NamedTextColor.GRAY));
        lore.add(Component.empty());
        if (ensina) {
            lore.add(Component.text("No capítulo 5 ele ensina: " + h.ensinamento(), NamedTextColor.GOLD));
            lore.add(Component.text("(" + h.efeito() + ")", NamedTextColor.DARK_GRAY));
        } else {
            lore.add(Component.text("No capítulo 5: um presente que só ele tem.", NamedTextColor.GOLD));
        }
        lore.add(Component.empty());
        lore.add(Component.text("» Clique para conversar (com o presente na mão, para dar)", NamedTextColor.YELLOW));
        return lore;
    }

    private static String nomeItem(Material m) {
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return switch (m) {
            case COOKIE -> "biscoito";
            case DANDELION -> "dente-de-leão";
            case PUMPKIN_PIE -> "torta de abóbora";
            case BONE_MEAL -> "farinha de osso";
            case COOKED_BEEF -> "bife";
            case APPLE -> "maçã";
            case COOKED_SALMON -> "salmão assado";
            case LANTERN -> "lanterna";
            case BAKED_POTATO -> "batata assada";
            case SUGAR -> "açúcar";
            case HONEY_BOTTLE -> "mel";
            case IRON_INGOT -> "lingote de ferro";
            case COAL -> "carvão";
            case WHEAT -> "trigo";
            case CARROT -> "cenoura";
            case NETHER_WART -> "fungo do Nether";
            case GLOWSTONE_DUST -> "pó de pedra luminosa";
            case EMERALD -> "esmeralda";
            case GOLD_NUGGET -> "pepita de ouro";
            case BOOK -> "livro";
            case INK_SAC -> "bolsa de tinta";
            case BRICK -> "tijolo";
            case BREAD -> "pão";
            case IRON_NUGGET -> "pepita de ferro";
            case FEATHER -> "pena";
            case ARROW -> "flecha";
            case GOLDEN_CARROT -> "cenoura dourada";
            default -> s;
        };
    }
}
