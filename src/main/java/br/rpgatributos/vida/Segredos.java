package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conquistas secretas: escondidas até acontecerem. Cada uma dá esmeraldas e XP, entra no Diário
 * e conta para o título Guardião de Segredos. /segredos mostra as descobertas e dá dicas das outras.
 */
public final class Segredos implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xB388FF);

    public enum Segredo {
        CAMA_NO_NETHER("Sono Explosivo", "Tentou dormir no Nether e sobreviveu", "Há lugares onde dormir não é boa ideia..."),
        QUEDA_LONGA("Asas de Pedra", "Caiu de mais de 60 blocos e sobreviveu", "O chão está muito, muito longe."),
        TOPO_DO_MUNDO("Teto do Mundo", "Chegou ao limite de altura do mundo", "Mais alto do que isso não dá."),
        TRES_MUNDOS("Viajante dos Mundos", "Esteve no mundo normal, no Nether e no End no mesmo dia", "Três mundos, um só dia."),
        FOGUEIRA_LONGA("Contador de Histórias", "Ficou 10 minutos sentado perto de uma fogueira", "Às vezes, só sentar e olhar o fogo."),
        GOLPE_50("Golpe Lendário", "Acertou um golpe de 50 de dano ou mais", "Um golpe que todo mundo vai lembrar."),
        BEBIDA_LENDARIA("Paladar Refinado", "Bebeu uma bebida Lendária do barril", "A paciência tem gosto."),
        MERCADOR("Freguês de Estrada", "Encontrou o Mercador Itinerante", "Ele nunca fica muito tempo no mesmo lugar."),
        PESCA_ECLIPSE("Pescador das Sombras", "Pescou um peixe raro durante um eclipse", "Quando o sol some, a água fica estranha."),
        CREEPER_EXPLOSIVO("Fogo Contra Fogo", "Derrotou um creeper com uma flecha explosiva", "Pague na mesma moeda."),
        TEMPESTADE("Sono Pesado", "Dormiu durante uma tempestade de raios", "Nem os trovões tiram o sono."),
        CARTA_BRILHANTE("Brilho Raro", "Achou uma carta brilhante", "Algumas cartas brilham.");

        private final String nome, descricao, dica;

        Segredo(String nome, String descricao, String dica) {
            this.nome = nome;
            this.descricao = descricao;
            this.dica = dica;
        }

        public String nome() { return nome; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kSegredos, kMundos;
    private final Map<UUID, Integer> sentadoSegundos = new HashMap<>();

    public Segredos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kSegredos = new NamespacedKey(plugin, "segredos");
        this.kMundos = new NamespacedKey(plugin, "mundos_do_dia");
    }

    private Set<String> feitos(Player p) {
        String s = p.getPersistentDataContainer().get(kSegredos, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    public void conceder(Player p, Segredo s) {
        Set<String> f = feitos(p);
        if (!f.add(s.name())) return;
        p.getPersistentDataContainer().set(kSegredos, PersistentDataType.STRING, String.join(",", f));
        p.showTitle(Title.title(Component.text("✦ Segredo descoberto!", COR, TextDecoration.BOLD),
                Component.text(s.nome + ": " + s.descricao, NamedTextColor.GRAY)));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.4f);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 40, 0.5, 0.6, 0.5, 0.6);
        p.getInventory().addItem(new ItemStack(Material.EMERALD, 8)).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        p.giveExp(150);
        plugin.titulos().registrar(p, "segredos", 1);
        plugin.diario().marco(p, "segredo_" + s.name().toLowerCase(java.util.Locale.ROOT), "Segredo: " + s.nome + " (" + s.descricao + ")");
    }

    // =====================================================================
    //  Gatilhos
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoUsarCama(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || !Tag.BEDS.isTagged(e.getClickedBlock().getType())) return;
        Player p = e.getPlayer();
        if (p.getWorld().getEnvironment() == World.Environment.NORMAL) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && !p.isDead()) conceder(p, Segredo.CAMA_NO_NETHER);
        }, 60L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        if (p.getFallDistance() >= 60 && p.getHealth() - e.getFinalDamage() > 0) conceder(p, Segredo.QUEDA_LONGA);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoGolpear(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && e.getFinalDamage() >= 50) conceder(p, Segredo.GOLPE_50);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoDeitar(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK && e.getPlayer().getWorld().isThundering()) {
            conceder(e.getPlayer(), Segredo.TEMPESTADE);
        }
    }

    @EventHandler
    public void aoTrocarDeMundo(PlayerChangedWorldEvent e) {
        anotarMundo(e.getPlayer());
    }

    @EventHandler
    public void aoEntrar(org.bukkit.event.player.PlayerJoinEvent e) {
        anotarMundo(e.getPlayer());
    }

    private void anotarMundo(Player p) {
        String hoje = LocalDate.now().toString();
        String s = p.getPersistentDataContainer().getOrDefault(kMundos, PersistentDataType.STRING, "");
        Set<String> mundos = new HashSet<>();
        if (s.startsWith(hoje + "|")) mundos.addAll(Arrays.asList(s.substring(hoje.length() + 1).split(",")));
        mundos.add(p.getWorld().getEnvironment().name());
        p.getPersistentDataContainer().set(kMundos, PersistentDataType.STRING, hoje + "|" + String.join(",", mundos));
        if (mundos.contains("NORMAL") && mundos.contains("NETHER") && mundos.contains("THE_END")) conceder(p, Segredo.TRES_MUNDOS);
    }

    /** A cada 2 s: altura máxima e tempo sentado perto da fogueira. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getLocation().getY() >= p.getWorld().getMaxHeight() - 1) conceder(p, Segredo.TOPO_DO_MUNDO);
            boolean perto = plugin.assentos().sentado(p) && !plugin.fogueiras().perto(p.getLocation(), 6).isEmpty();
            if (!perto) {
                sentadoSegundos.remove(p.getUniqueId());
                continue;
            }
            if (sentadoSegundos.merge(p.getUniqueId(), 2, Integer::sum) >= 600) conceder(p, Segredo.FOGUEIRA_LONGA);
        }
    }

    // =====================================================================
    //  Comando
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        Set<String> f = feitos(p);
        p.sendMessage(Component.text("✦ Segredos: " + f.size() + "/" + Segredo.values().length, COR, TextDecoration.BOLD));
        for (Segredo s : Segredo.values()) {
            if (f.contains(s.name())) p.sendMessage(Component.text(" ✔ " + s.nome + ": ", COR).append(Component.text(s.descricao, NamedTextColor.GRAY)));
            else p.sendMessage(Component.text(" ??? — " + s.dica, NamedTextColor.DARK_GRAY));
        }
        return true;
    }
}
