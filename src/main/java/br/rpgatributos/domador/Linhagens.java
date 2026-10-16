package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Linhagens (2.50): cavalos, burros, mulas, lhamas e camelos guardam pai, mãe e geração. O
 * filhote herda dos pais (como no jogo) e às vezes melhora um pouco; da 5ª geração em diante a
 * família ganha um nome de linhagem, que os descendentes carregam. O Livro do Criador mostra tudo.
 */
public final class Linhagens implements Listener {

    public static final TextColor COR = TextColor.color(0xD4A373);
    private static final NamespacedKey K_LIVRO = new NamespacedKey("rpgatributos", "livro_criador");
    private static final NamespacedKey K_PAI = new NamespacedKey("rpgatributos", "lin_pai");
    private static final NamespacedKey K_MAE = new NamespacedKey("rpgatributos", "lin_mae");
    private static final NamespacedKey K_GERACAO = new NamespacedKey("rpgatributos", "lin_geracao");
    private static final NamespacedKey K_LINHAGEM = new NamespacedKey("rpgatributos", "lin_nome");
    private static final NamespacedKey K_CRIADOR = new NamespacedKey("rpgatributos", "lin_criador");

    /** Os máximos do jogo (para não passar do que um cavalo pode ter). */
    private static final double MAX_VELOCIDADE = 0.3375, MAX_PULO = 1.0, MAX_VIDA = 30;
    private static final int GERACAO_LINHAGEM = 5;
    private static final String[] NOMES = {"do Vento", "do Trovão", "da Aurora", "das Brasas", "da Neblina", "do Crepúsculo", "das Estrelas",
            "do Rio Prateado", "da Montanha", "dos Campos Dourados", "da Lua Cheia", "do Orvalho", "da Tempestade", "do Sol Poente", "dos Pinheiros", "da Areia Rubra"};

    private final RPGAtributos plugin;

    public Linhagens(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  O Livro do Criador
    // =====================================================================

    public static ItemStack livro() {
        ItemStack i = new ItemStack(Material.BOOK);
        i.editMeta(m -> {
            m.itemName(Component.text("Livro do Criador", COR));
            m.lore(List.of(
                    Component.text("Clique num cavalo, burro, mula, lhama ou camelo:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("velocidade, pulo, vida, pais, geração e linhagem.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_LIVRO, PersistentDataType.BYTE, (byte) 1);
            m.setMaxStackSize(1);
            PacoteRecursos.marcar(m, "livro_criador");
        });
        return i;
    }

    public static boolean ehLivro(ItemStack i) {
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(K_LIVRO);
    }

    public void registrarReceitas() {
        ShapelessRecipe r = new ShapelessRecipe(new NamespacedKey(plugin, "livro_criador"), livro());
        r.addIngredient(Material.BOOK);
        r.addIngredient(Material.GOLDEN_CARROT);
        r.addIngredient(Material.LEATHER);
        Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoLer(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof AbstractHorse h)) return;
        Player p = e.getPlayer();
        if (!ehLivro(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        ficha(p, h);
    }

    private void ficha(Player p, AbstractHorse h) {
        PersistentDataContainer d = h.getPersistentDataContainer();
        int ger = d.getOrDefault(K_GERACAO, PersistentDataType.INTEGER, 0);
        String linhagem = d.get(K_LINHAGEM, PersistentDataType.STRING);
        p.sendMessage(Component.text("━━━ 📖 " + nome(h) + " ━━━", COR, TextDecoration.BOLD));
        if (linhagem != null) p.sendMessage(Component.text("  ✦ Linhagem " + linhagem, NamedTextColor.GOLD));
        double vel = valor(h, Attribute.MOVEMENT_SPEED), pulo = valor(h, Attribute.JUMP_STRENGTH), vida = valor(h, Attribute.MAX_HEALTH);
        if (vel > 0) p.sendMessage(linha("Velocidade", fmt(vel * 42.16) + " blocos/s", vel / MAX_VELOCIDADE));
        if (pulo > 0 && !(h instanceof Llama)) p.sendMessage(linha("Pulo", fmt(alturaPulo(pulo)) + " blocos", pulo / MAX_PULO));
        p.sendMessage(linha("Vida", fmt(vida / 2) + " ❤", vida / MAX_VIDA));
        if (h instanceof Llama l) p.sendMessage(linha("Força (carga)", l.getStrength() + "/5", l.getStrength() / 5.0));
        p.sendMessage(Component.text("  Geração: ", NamedTextColor.GRAY).append(Component.text(ger == 0 ? "selvagem" : ger + "ª", NamedTextColor.WHITE)));
        String pai = d.get(K_PAI, PersistentDataType.STRING), mae = d.get(K_MAE, PersistentDataType.STRING);
        if (pai != null || mae != null) {
            p.sendMessage(Component.text("  Pais: ", NamedTextColor.GRAY).append(Component.text((pai == null ? "?" : pai) + " × " + (mae == null ? "?" : mae), NamedTextColor.WHITE)));
        }
        String criador = d.get(K_CRIADOR, PersistentDataType.STRING);
        if (criador != null) p.sendMessage(Component.text("  Criado por " + criador, NamedTextColor.DARK_GRAY));
        if (ger < GERACAO_LINHAGEM && linhagem == null) {
            p.sendMessage(Component.text("  Na " + GERACAO_LINHAGEM + "ª geração a família ganha nome de linhagem.", NamedTextColor.DARK_GRAY));
        }
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
    }

    private static Component linha(String nome, String valor, double fracao) {
        int cheias = (int) Math.round(Math.max(0, Math.min(1, fracao)) * 10);
        TextColor cor = fracao >= 0.9 ? NamedTextColor.GOLD : fracao >= 0.7 ? NamedTextColor.GREEN : fracao >= 0.5 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        return Component.text("  " + nome + ": ", NamedTextColor.GRAY).append(Component.text(valor + "  ", NamedTextColor.WHITE))
                .append(Component.text("█".repeat(cheias), cor)).append(Component.text("█".repeat(10 - cheias), NamedTextColor.DARK_GRAY));
    }

    /** Altura do pulo em blocos (conta conhecida da comunidade para a força de pulo do cavalo). */
    private static double alturaPulo(double x) {
        return -0.1817584952 * x * x * x + 3.689713992 * x * x + 2.128599134 * x - 0.343930367;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    private static double valor(Entity e, Attribute a) {
        if (!(e instanceof org.bukkit.attribute.Attributable at)) return 0;
        AttributeInstance i = at.getAttribute(a);
        return i == null ? 0 : i.getBaseValue();
    }

    private static String nome(Entity e) {
        if (e.customName() != null) return PlainTextComponentSerializer.plainText().serialize(e.customName());
        return Estabulos.nomeTipo(e.getType()) + " #" + e.getUniqueId().toString().substring(0, 4);
    }

    // =====================================================================
    //  Cruzar
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCruzar(EntityBreedEvent e) {
        if (!(e.getEntity() instanceof AbstractHorse filhote) || !(e.getMother() instanceof AbstractHorse mae) || !(e.getFather() instanceof AbstractHorse pai)) return;
        Player criador = e.getBreeder() instanceof Player p ? p : null;
        // Um tick depois: o jogo já misturou os números dos pais no filhote.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!filhote.isValid()) return;
            PersistentDataContainer d = filhote.getPersistentDataContainer();
            int ger = Math.max(geracao(pai), geracao(mae)) + 1;
            d.set(K_GERACAO, PersistentDataType.INTEGER, ger);
            d.set(K_PAI, PersistentDataType.STRING, nome(pai));
            d.set(K_MAE, PersistentDataType.STRING, nome(mae));
            if (criador != null) d.set(K_CRIADOR, PersistentDataType.STRING, criador.getName());
            int melhorou = 0;
            melhorou += melhorar(filhote, Attribute.MOVEMENT_SPEED, MAX_VELOCIDADE);
            if (!(filhote instanceof Llama)) melhorou += melhorar(filhote, Attribute.JUMP_STRENGTH, MAX_PULO);
            melhorou += melhorar(filhote, Attribute.MAX_HEALTH, MAX_VIDA);
            if (filhote instanceof Llama l && l.getStrength() < 5 && ThreadLocalRandom.current().nextDouble() < 0.2) {
                l.setStrength(l.getStrength() + 1);
                melhorou++;
            }
            // Linhagem: herda a do pai ou da mãe (da geração mais alta); na 5ª, nasce uma nova.
            String lin = linhagem(geracao(pai) >= geracao(mae) ? pai : mae);
            if (lin == null) lin = linhagem(geracao(pai) >= geracao(mae) ? mae : pai);
            boolean nova = false;
            if (lin == null && ger >= GERACAO_LINHAGEM) {
                lin = NOMES[ThreadLocalRandom.current().nextInt(NOMES.length)];
                nova = true;
            }
            if (lin != null) d.set(K_LINHAGEM, PersistentDataType.STRING, lin);
            if (criador == null) return;
            double vel = valor(filhote, Attribute.MOVEMENT_SPEED);
            criador.sendMessage(Component.text("🐴 Nasceu um filhote! ", COR)
                    .append(Component.text(ger + "ª geração · " + fmt(vel * 42.16) + " blocos/s"
                            + (melhorou > 0 ? " · melhorou em " + melhorou + (melhorou == 1 ? " ponto" : " pontos") + "!" : ""), NamedTextColor.GRAY)));
            if (nova) {
                criador.sendMessage(Component.text("✦ Nasce a Linhagem " + lin + "! Os descendentes levam esse nome.", NamedTextColor.GOLD, TextDecoration.BOLD));
                criador.playSound(criador.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.3f);
                plugin.titulos().registrar(criador, "linhagens", 1);
                plugin.diario().marco(criador, "linhagem", "Fundou a Linhagem " + lin);
            }
        });
    }

    /** 25% de chance de subir 3% a 8% (sem passar do máximo do jogo). @return 1 se melhorou. */
    private static int melhorar(Entity e, Attribute a, double max) {
        if (!(e instanceof org.bukkit.attribute.Attributable at)) return 0;
        AttributeInstance i = at.getAttribute(a);
        if (i == null || ThreadLocalRandom.current().nextDouble() > 0.25 || i.getBaseValue() >= max) return 0;
        double novo = Math.min(max, i.getBaseValue() * (1.03 + ThreadLocalRandom.current().nextDouble() * 0.05));
        i.setBaseValue(novo);
        if (a == Attribute.MAX_HEALTH && e instanceof org.bukkit.entity.LivingEntity le) le.setHealth(Math.min(novo, le.getHealth() + 2));
        return 1;
    }

    private static int geracao(Entity e) {
        return e.getPersistentDataContainer().getOrDefault(K_GERACAO, PersistentDataType.INTEGER, 0);
    }

    private static String linhagem(Entity e) {
        return e.getPersistentDataContainer().get(K_LINHAGEM, PersistentDataType.STRING);
    }
}
