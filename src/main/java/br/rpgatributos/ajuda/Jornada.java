package br.rpgatributos.ajuda;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.MapasDoTesouro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Jornada do Aventureiro: 12 passos que ensinam o básico na ordem certa. Cada passo se
 * completa sozinho quando o jogador faz a coisa (mesmo se já tinha feito antes), dá uma
 * pequena recompensa e aponta o próximo.
 */
public final class Jornada {

    public static final TextColor COR = TextColor.color(0xF2C14E);

    /** Um passo: o que fazer, onde aprender (assunto do Guia), quando conta e o que ganha. */
    public record Passo(String nome, String objetivo, Material icone, String topico, BiPredicate<RPGAtributos, Player> feito,
                        String recompensa, BiFunction<RPGAtributos, Player, List<ItemStack>> premio) { }

    private static int maxAtributo(RPGAtributos pl, Player p) {
        int m = 0;
        for (Skill s : Skill.values()) m = Math.max(m, pl.stats().getNivel(p, s));
        return m;
    }

    private static List<ItemStack> esmeraldas(int n) {
        return List.of(new ItemStack(Material.EMERALD, n));
    }

    public static final List<Passo> PASSOS = List.of(
            new Passo("Primeiros níveis", "Leve qualquer atributo ao nível 5 (minere, corte madeira, lute...).", Material.EXPERIENCE_BOTTLE,
                    "atributos", (pl, p) -> maxAtributo(pl, p) >= 5, "4 esmeraldas", (pl, p) -> esmeraldas(4)),
            new Passo("O fogo da forja", "Monte a Forja do Ferreiro e forje um item.", Material.ANVIL,
                    "forja", (pl, p) -> pl.titulos().contador(p, "forjados") >= 1, "6 lingotes de ferro",
                    (pl, p) -> List.of(new ItemStack(Material.IRON_INGOT, 6))),
            new Passo("Primeiro combo", "Com uma arma na mão, solte 5 combos (3 cliques seguidos).", Material.IRON_SWORD,
                    "combos", (pl, p) -> pl.titulos().contador(p, "combos") >= 5, "6 esmeraldas", (pl, p) -> esmeraldas(6)),
            new Passo("Barriga cheia", "Monte a Cozinha e cozinhe um prato.", Material.SMOKER,
                    "cozinha", (pl, p) -> pl.titulos().contador(p, "pratos") >= 1, "4 esmeraldas e 8 pães",
                    (pl, p) -> List.of(new ItemStack(Material.EMERALD, 4), new ItemStack(Material.BREAD, 8))),
            new Passo("Aprendiz de alquimista", "Monte a Bancada Alquímica e crie qualquer coisa nela.", Material.BREWING_STAND,
                    "alquimia", (pl, p) -> pl.titulos().contador(p, "alquimias") >= 1, "2 Pós Arcanos",
                    (pl, p) -> List.of(Reagente.PO_ARCANO.criar(2))),
            new Passo("Um caminho", "Monte um Santuário das Classes e escolha uma classe.", Material.LECTERN,
                    "classes", (pl, p) -> pl.classes().ativa(p) != null, "8 esmeraldas", (pl, p) -> esmeraldas(8)),
            new Passo("Faíscas de magia", "Monte o Infusor, pegue o grimório e lance 10 magias.", Material.ENCHANTED_BOOK,
                    "magias", (pl, p) -> pl.titulos().contador(p, "magias") >= 10, "1 Cristal de Mana",
                    (pl, p) -> List.of(Reagente.CRISTAL_DE_MANA.criar(1))),
            new Passo("Terra sua", "Crie o Marco do Território.", Material.LODESTONE,
                    "territorio", (pl, p) -> pl.territorios().de(p.getUniqueId()) != null, "8 esmeraldas", (pl, p) -> esmeraldas(8)),
            new Passo("Fé", "Monte um Santuário dos Deuses e siga um deus.", Material.CHISELED_QUARTZ_BLOCK,
                    "deuses", (pl, p) -> pl.deuses().deus(p) != null, "1 maçã dourada",
                    (pl, p) -> List.of(new ItemStack(Material.GOLDEN_APPLE))),
            new Passo("Ruínas", "Descubra uma estrutura pelo mundo (vá para terras novas).", Material.MOSSY_STONE_BRICKS,
                    "estruturas", (pl, p) -> pl.titulos().contador(p, "estruturas") >= 1, "1 Mapa do Tesouro",
                    (pl, p) -> List.of(pl.mapas().criar(MapasDoTesouro.Tipo.COMUM, p.getLocation()))),
            new Passo("Lá embaixo", "Vença uma masmorra (sozinho ou com a party).", Material.CHISELED_STONE_BRICKS,
                    "masmorras", (pl, p) -> pl.titulos().contador(p, "masmorras") >= 1, "1 Fragmento de Forja",
                    (pl, p) -> List.of(Raro.FRAGMENTO_DE_FORJA.criar(1))),
            new Passo("Uma vila", "Funde a sua colônia (sino no seu território).", Material.BELL,
                    "colonia", (pl, p) -> pl.colonias().de(p.getUniqueId()) != null, "16 esmeraldas", (pl, p) -> esmeraldas(16)));

    private final RPGAtributos plugin;
    private final NamespacedKey kPasso;

    public Jornada(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kPasso = new NamespacedKey(plugin, "jornada_passo");
    }

    /** Quantos passos já foram feitos (0 a 12). */
    public int feitos(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kPasso, PersistentDataType.INTEGER, 0);
    }

    public boolean completa(Player p) {
        return feitos(p) >= PASSOS.size();
    }

    /** O passo atual, ou null se a jornada acabou. */
    public Passo atual(Player p) {
        int i = feitos(p);
        return i < PASSOS.size() ? PASSOS.get(i) : null;
    }

    /** A cada poucos segundos: completa os passos já cumpridos (um de cada vez, em ordem). */
    public void verificar(Player p) {
        int i = feitos(p);
        if (i >= PASSOS.size()) return;
        Passo ps = PASSOS.get(i);
        if (!ps.feito().test(plugin, p)) return;
        p.getPersistentDataContainer().set(kPasso, PersistentDataType.INTEGER, i + 1);
        for (ItemStack it : ps.premio().apply(plugin, p)) {
            p.getInventory().addItem(it).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        Passo prox = atual(p);
        p.showTitle(Title.title(Component.text("✔ " + ps.nome(), COR, TextDecoration.BOLD),
                Component.text("Jornada " + (i + 1) + "/" + PASSOS.size() + " · você ganhou " + ps.recompensa(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2600), Duration.ofMillis(600))));
        if (prox != null) {
            p.sendMessage(Component.text("✦ Jornada: ", COR, TextDecoration.BOLD)
                    .append(Component.text("próximo passo — " + prox.nome() + ": " + prox.objetivo(), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(" [ver como]", NamedTextColor.YELLOW)
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guia " + prox.topico()))));
        } else {
            plugin.getServer().broadcast(Component.text("✦ " + p.getName() + " completou a Jornada do Aventureiro!", COR));
            plugin.diario().marco(p, "jornada", "Completou a Jornada do Aventureiro");
        }
    }
}
