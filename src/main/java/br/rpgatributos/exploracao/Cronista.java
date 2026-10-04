package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A campanha: o Cronista (um bibliotecário que o admin coloca no mundo) conta a história em
 * capítulos. Cada capítulo pede algo dos sistemas do plugin e dá uma recompensa. O livro
 * abre clicando nele ou com /cronista; entregar o capítulo precisa ser perto dele.
 */
public final class Cronista implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x6D4C41);
    private static final NamespacedKey K_NPC = new NamespacedKey("rpgatributos", "cronista");

    private final RPGAtributos plugin;
    private final NamespacedKey kCapitulo;

    public Cronista(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kCapitulo = new NamespacedKey(plugin, "cronista_capitulo");
    }

    public int capitulo(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kCapitulo, PersistentDataType.INTEGER, 0);
    }

    public void definir(Player p, int n) {
        p.getPersistentDataContainer().set(kCapitulo, PersistentDataType.INTEGER, Math.max(0, Math.min(Capitulo.values().length, n)));
    }

    private long progresso(Player p, Capitulo c) {
        return c.contador() == null ? plugin.stats().nivelTotal(p) : plugin.titulos().contador(p, c.contador());
    }

    // =====================================================================
    //  O NPC
    // =====================================================================

    public static boolean eh(Entity e) {
        return e != null && e.getPersistentDataContainer().has(K_NPC);
    }

    public Villager colocar(Location l) {
        return l.getWorld().spawn(l, Villager.class, v -> {
            v.setProfession(Villager.Profession.LIBRARIAN);
            v.setVillagerLevel(5);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.customName(Component.text("✎ O Cronista", COR, TextDecoration.BOLD));
            v.setCustomNameVisible(true);
            v.getPersistentDataContainer().set(K_NPC, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private boolean pertoDoCronista(Player p) {
        for (Entity e : p.getNearbyEntities(8, 4, 8)) if (eh(e)) return true;
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEntityEvent e) {
        if (!eh(e.getRightClicked())) return;
        e.setCancelled(true);
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        e.getPlayer().playSound(e.getRightClicked().getLocation(), Sound.ENTITY_VILLAGER_WORK_LIBRARIAN, 1f, 0.9f);
        abrirLivro(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (eh(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTransformar(EntityTransformEvent e) {
        if (eh(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMudarProfissao(VillagerCareerChangeEvent e) {
        if (eh(e.getEntity())) e.setCancelled(true);
    }

    // =====================================================================
    //  O livro
    // =====================================================================

    public void abrirLivro(Player p) {
        int n = capitulo(p);
        Capitulo[] todos = Capitulo.values();
        List<Component> paginas = new ArrayList<>();
        if (n >= todos.length) {
            paginas.add(Component.text("✎ Epílogo\n\n", COR, TextDecoration.BOLD)
                    .append(Component.text("Você agora sabe tanto quanto eu. Talvez mais.\n\nOs deuses voltaram a falar, os selos foram rompidos e as estrelas estão de novo no céu.", NamedTextColor.BLACK)));
            paginas.add(Component.text("O resto desta história não está escrito em lugar nenhum.\n\n", NamedTextColor.BLACK)
                    .append(Component.text("Escreva você.", COR, TextDecoration.BOLD)));
        } else {
            Capitulo c = todos[n];
            if (n == 0) {
                paginas.add(Component.text("✎ O Cronista\n\n", COR, TextDecoration.BOLD)
                        .append(Component.text("Eu guardo a história deste mundo. Ou o que sobrou dela.\n\nAjude-me a lembrar, e eu conto tudo o que sei.", NamedTextColor.BLACK)));
            }
            paginas.add(Component.text("Capítulo " + (n + 1) + "\n" + c.titulo() + "\n\n", COR, TextDecoration.BOLD)
                    .append(Component.text(c.abertura(), NamedTextColor.BLACK)));
            paginas.add(Component.text(c.historia(), NamedTextColor.BLACK));
            long feito = Math.min(c.meta(), progresso(p, c));
            boolean pronto = feito >= c.meta();
            Component tarefa = Component.text("Tarefa\n\n", COR, TextDecoration.BOLD)
                    .append(Component.text(c.objetivo() + "\n\n", NamedTextColor.BLACK))
                    .append(Component.text((pronto ? "✔ " : "✖ ") + feito + " / " + c.meta() + "\n\n", pronto ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
            if (pronto) {
                tarefa = tarefa.append(Component.text("[ Entregar ao Cronista ]", NamedTextColor.DARK_BLUE, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/cronista entregar"))
                        .hoverEvent(HoverEvent.showText(Component.text("Clique perto de um Cronista"))));
            } else {
                tarefa = tarefa.append(Component.text("Volte quando terminar.", NamedTextColor.DARK_GRAY));
            }
            paginas.add(tarefa);
        }
        p.openBook(Book.book(Component.text("Crônicas do Mundo"), Component.text("O Cronista"), paginas));
    }

    /** @return motivo de não ter entregado, ou null. */
    public String entregar(Player p) {
        int n = capitulo(p);
        Capitulo[] todos = Capitulo.values();
        if (n >= todos.length) return "Você já terminou a campanha.";
        Capitulo c = todos[n];
        if (progresso(p, c) < c.meta()) return "Ainda não: " + c.objetivo() + ".";
        if (!pertoDoCronista(p)) return "Entregue perto de um Cronista.";
        for (ItemStack i : recompensa(p, c)) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        definir(p, n + 1);
        plugin.titulos().definirMinimo(p, "campanha", n + 1);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 80, 0.5, 0.8, 0.5, 0.8);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.1f);
        boolean fim = n + 1 >= todos.length;
        p.showTitle(Title.title(Component.text(fim ? "✎ Campanha concluída ✎" : "✎ Capítulo " + (n + 1) + " concluído", COR, TextDecoration.BOLD),
                Component.text(fim ? "O Cronista não tem mais nada a ensinar" : "Fale com o Cronista para o próximo", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(700))));
        if (fim) {
            plugin.getServer().broadcast(Component.text("✎ " + p.getName() + " completou as Crônicas do Mundo!", COR));
        }
        return null;
    }

    private List<ItemStack> recompensa(Player p, Capitulo c) {
        MapasDoTesouro mapas = plugin.mapas();
        return switch (c) {
            case AVENTUREIRO -> List.of(new ItemStack(Material.EMERALD, 16), mapas.criar(MapasDoTesouro.Tipo.COMUM, p.getLocation()));
            case FORJA -> List.of(Raro.FRAGMENTO_DE_FORJA.criar(4));
            case ECOS -> List.of(Reagente.PO_ARCANO.criar(8), Reagente.CRISTAL_DE_MANA.criar(2));
            case DEUSES -> List.of(plugin.arqueologia().reliquiaAleatoria(), new ItemStack(Material.EMERALD, 10));
            case MASMORRAS -> List.of(Raro.PEDRA_DE_PROTECAO.criar());
            case ESTRELAS -> List.of(mapas.criar(MapasDoTesouro.Tipo.RARO, p.getLocation()));
            case RUINAS -> List.of(Raro.ESSENCIA_PRIMORDIAL.criar());
            case SELO -> List.of(Mitrilo.lingote(2), mapas.criar(MapasDoTesouro.Tipo.LENDARIO, p.getLocation()));
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("entregar")) {
            String erro = entregar(p);
            if (erro != null) p.sendMessage(Component.text(erro, NamedTextColor.RED));
            return true;
        }
        abrirLivro(p);
        return true;
    }
}
