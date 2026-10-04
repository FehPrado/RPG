package br.rpgatributos.progressao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renascer: com todos os atributos no nível exigido (o máximo, por padrão), o jogador volta
 * os atributos para 0 e ganha um bônus permanente, pontos de talento e uma ★ na tag. Itens,
 * classes, lendas, companheiros, títulos e todo o resto ficam. Até 5 vezes (padrão).
 */
public final class Renascimento implements CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFD54F);

    private final RPGAtributos plugin;
    private final NamespacedKey kVezes;
    private final Map<UUID, Long> confirmarAte = new HashMap<>();

    public Renascimento(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kVezes = new NamespacedKey(plugin, "renascimentos");
    }

    private Settings cfg() { return plugin.settings(); }

    public int vezes(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kVezes, PersistentDataType.INTEGER, 0);
    }

    public void definir(Player p, int n) {
        int v = Math.max(0, Math.min(cfg().renMaximo, n));
        if (v == 0) p.getPersistentDataContainer().remove(kVezes);
        else p.getPersistentDataContainer().set(kVezes, PersistentDataType.INTEGER, v);
        plugin.talentos().conferir(p);
        plugin.bonus().aplicar(p);
        plugin.tags().atualizarTexto(p);
    }

    /**
     * Nível total que conta para o que já foi conquistado (mochila, território, masmorras,
     * requisitos de classe): quem renasceu conta como se tivesse todos os atributos no máximo.
     */
    public int nivelTotalEfetivo(Player p) {
        int total = plugin.stats().nivelTotal(p);
        return vezes(p) > 0 ? Math.max(total, cfg().nivelMaximo * Skill.values().length) : total;
    }

    public int nivelExigido() {
        return Math.max(1, Math.min(cfg().renNivel, cfg().nivelMaximo));
    }

    public List<Skill> faltando(Player p) {
        List<Skill> l = new ArrayList<>();
        int exige = nivelExigido();
        for (Skill s : Skill.values()) if (plugin.stats().getNivel(p, s) < exige) l.add(s);
        return l;
    }

    /** ★★ antes do nome na tag, ou null. */
    public Component estrelas(Player p) {
        int n = vezes(p);
        return n <= 0 ? null : Component.text("★".repeat(n) + " ", COR);
    }

    // =====================================================================
    //  Menu e comando
    // =====================================================================

    public ItemStack itemDoMenu(Player p) {
        int n = vezes(p);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Renascimentos: " + n + "/" + cfg().renMaximo, COR));
        lore.add(Component.empty());
        lore.add(Component.text("Com todos os atributos no nível " + nivelExigido() + ":", NamedTextColor.GRAY));
        lore.add(Component.text("os atributos voltam a 0 e você ganha, para sempre:", NamedTextColor.GRAY));
        lore.addAll(bonusPorVez());
        lore.add(Component.text("Itens, classes, lendas e o resto ficam.", NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        if (n >= cfg().renMaximo) {
            lore.add(Component.text("✔ Você já renasceu o máximo de vezes.", NamedTextColor.GOLD));
        } else {
            List<Skill> f = faltando(p);
            if (f.isEmpty()) lore.add(Component.text("» Clique para renascer", NamedTextColor.GREEN));
            else lore.add(Component.text("✖ Faltam " + f.size() + " atributo(s) no nível " + nivelExigido(), NamedTextColor.RED));
        }
        ItemStack i = new ItemStack(Material.NETHER_STAR);
        i.editMeta(m -> {
            m.itemName(Component.text("☀ Renascer", COR, TextDecoration.BOLD));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        });
        return i;
    }

    private List<Component> bonusPorVez() {
        Settings c = cfg();
        List<Component> l = new ArrayList<>();
        l.add(Component.text(" +" + StatsManager.fmt(c.renXp * 100) + "% de XP em tudo", NamedTextColor.GREEN));
        if (c.renVida > 0) l.add(Component.text(" +" + StatsManager.fmt(c.renVida / 2) + " ❤", NamedTextColor.GREEN));
        if (c.renDano > 0) l.add(Component.text(" +" + StatsManager.fmt(c.renDano * 100) + "% de dano corpo a corpo", NamedTextColor.GREEN));
        if (c.renMana > 0) l.add(Component.text(" +" + StatsManager.fmt(c.renMana) + " de mana", NamedTextColor.GREEN));
        l.add(Component.text(" +" + c.renPontosTalento + " pontos de talento e uma ★ na tag", NamedTextColor.GREEN));
        return l;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("confirmar")) confirmar(p);
        else pedir(p);
        return true;
    }

    /** Mostra o que falta ou pede a confirmação. */
    public void pedir(Player p) {
        if (!cfg().renAtivado) {
            p.sendMessage(Component.text("Renascer está desligado neste servidor.", NamedTextColor.RED));
            return;
        }
        int n = vezes(p);
        p.sendMessage(Component.text("☀ Renascer (" + n + "/" + cfg().renMaximo + ")", COR, TextDecoration.BOLD));
        if (n >= cfg().renMaximo) {
            p.sendMessage(Component.text("Você já renasceu o máximo de vezes.", NamedTextColor.GOLD));
            return;
        }
        List<Skill> f = faltando(p);
        if (!f.isEmpty()) {
            p.sendMessage(Component.text("Precisa de todos os atributos no nível " + nivelExigido() + ". Faltam:", NamedTextColor.GRAY));
            for (Skill s : f) {
                p.sendMessage(Component.text(" " + s.icone() + " " + s.nome() + ": " + plugin.stats().getNivel(p, s) + "/" + nivelExigido(), s.cor()));
            }
            return;
        }
        p.sendMessage(Component.text("Seus atributos voltam ao nível 0 e os talentos são devolvidos. Para sempre, você ganha:", NamedTextColor.GRAY));
        bonusPorVez().forEach(p::sendMessage);
        p.sendMessage(Component.text("Itens, classes, lendas, companheiros, títulos e o resto continuam com você.", NamedTextColor.DARK_GRAY));
        confirmarAte.put(p.getUniqueId(), System.currentTimeMillis() + 30_000);
        p.sendMessage(Component.text("[Renascer agora]", NamedTextColor.GOLD, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/renascer confirmar"))
                .hoverEvent(HoverEvent.showText(Component.text("Não dá para desfazer.", NamedTextColor.RED)))
                .append(Component.text("  (vale por 30 s)", NamedTextColor.DARK_GRAY)));
    }

    private void confirmar(Player p) {
        Long ate = confirmarAte.remove(p.getUniqueId());
        if (ate == null || ate < System.currentTimeMillis()) {
            pedir(p);
            return;
        }
        if (!cfg().renAtivado || vezes(p) >= cfg().renMaximo || !faltando(p).isEmpty()) {
            pedir(p);
            return;
        }
        renascer(p);
    }

    public void renascer(Player p) {
        for (Skill s : Skill.values()) plugin.stats().setXp(p, s, 0);
        int n = vezes(p) + 1;
        p.getPersistentDataContainer().set(kVezes, PersistentDataType.INTEGER, n);
        plugin.talentos().zerar(p);
        plugin.bonus().aplicar(p);
        plugin.tags().atualizarTexto(p);
        plugin.titulos().registrar(p, "renascimentos", 1);
        plugin.diario().marco(p, "renascer_" + n, "Renasceu pela " + n + "ª vez");
        plugin.titulos().verificar(p);

        p.getWorld().strikeLightningEffect(p.getLocation());
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 120, 0.6, 1, 0.6, 0.5);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 60, 0.4, 1.2, 0.4, 0.08);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.8f);
        p.showTitle(Title.title(Component.text("☀ Renascido " + "★".repeat(n), COR, TextDecoration.BOLD),
                Component.text("Seus atributos recomeçam, mais fortes do que nunca", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
        p.sendMessage(Component.text("☀ Você renasceu! Tem " + plugin.talentos().pontosTotais(p) + " pontos de talento: /talentos", COR));
        plugin.getServer().broadcast(Component.text("☀ ", COR).append(Component.text(p.getName(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" renasceu pela " + n + "ª vez! " + "★".repeat(n), COR)));
    }
}
