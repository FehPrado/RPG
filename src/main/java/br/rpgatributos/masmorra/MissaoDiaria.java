package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Missão Diária (2.73): "Preparação para ficar forte", como no Solo Leveling, sem punição. Agachar,
 * pular, correr e abater monstros num dia (real) dão XP de caçador e Cristais de Mana; 7 dias
 * seguidos dão um prêmio a mais. Os números vêm das estatísticas do jogo (menos o agachar).
 */
public final class MissaoDiaria implements Listener {

    /** As tarefas: nome, quanto e a unidade mostrada. */
    public enum Tarefa {
        AGACHAR("Agachar", 100, "vezes"),
        PULAR("Pular", 100, "vezes"),
        CORRER("Correr", 2000, "m"),
        ABATER("Abater monstros", 20, "");

        public final String nome, unidade;
        public final int meta;

        Tarefa(String nome, int meta, String unidade) {
            this.nome = nome;
            this.meta = meta;
            this.unidade = unidade;
        }
    }

    private static final NamespacedKey K_DIA = new NamespacedKey("rpgatributos", "missao_dia");
    private static final NamespacedKey K_BASE = new NamespacedKey("rpgatributos", "missao_base");
    private static final NamespacedKey K_AGACHAR = new NamespacedKey("rpgatributos", "missao_agachar");
    private static final NamespacedKey K_FEITA = new NamespacedKey("rpgatributos", "missao_feita");
    private static final NamespacedKey K_SEQ = new NamespacedKey("rpgatributos", "missao_seq");

    private final RPGAtributos plugin;

    public MissaoDiaria(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    static long hoje() {
        return LocalDate.now().toEpochDay();
    }

    // =====================================================================
    //  Progresso
    // =====================================================================

    /** Começa o dia se ainda não começou: guarda as estatísticas de agora como ponto de partida. */
    private static boolean comecarDia(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (pdc.getOrDefault(K_DIA, PersistentDataType.LONG, -1L) == hoje()) return false;
        pdc.set(K_DIA, PersistentDataType.LONG, hoje());
        pdc.set(K_BASE, PersistentDataType.STRING, p.getStatistic(Statistic.JUMP) + "," + p.getStatistic(Statistic.SPRINT_ONE_CM)
                + "," + p.getStatistic(Statistic.MOB_KILLS));
        pdc.set(K_AGACHAR, PersistentDataType.INTEGER, 0);
        return true;
    }

    public static int progresso(Player p, Tarefa t) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (t == Tarefa.AGACHAR) return pdc.getOrDefault(K_AGACHAR, PersistentDataType.INTEGER, 0);
        String[] b = pdc.getOrDefault(K_BASE, PersistentDataType.STRING, "0,0,0").split(",");
        long base;
        try { base = Long.parseLong(b[t == Tarefa.PULAR ? 0 : t == Tarefa.CORRER ? 1 : 2]); } catch (RuntimeException e) { base = 0; }
        return switch (t) {
            case PULAR -> (int) Math.max(0, p.getStatistic(Statistic.JUMP) - base);
            case CORRER -> (int) Math.max(0, (p.getStatistic(Statistic.SPRINT_ONE_CM) - base) / 100);
            case ABATER -> (int) Math.max(0, p.getStatistic(Statistic.MOB_KILLS) - base);
            default -> 0;
        };
    }

    public static boolean feitaHoje(Player p) {
        return p.getPersistentDataContainer().getOrDefault(K_FEITA, PersistentDataType.LONG, -1L) == hoje();
    }

    public static int sequencia(Player p) {
        return p.getPersistentDataContainer().getOrDefault(K_SEQ, PersistentDataType.INTEGER, 0);
    }

    private static boolean completa(Player p) {
        for (Tarefa t : Tarefa.values()) if (progresso(p, t) < t.meta) return false;
        return true;
    }

    /** As linhas de progresso (para a janela e o /masmorra missao). */
    public static List<Component> linhas(Player p) {
        List<Component> l = new ArrayList<>();
        for (Tarefa t : Tarefa.values()) {
            int v = Math.min(t.meta, progresso(p, t));
            boolean ok = v >= t.meta;
            l.add(Component.text((ok ? "✔ " : "• ") + t.nome + ": " + v + "/" + t.meta + (t.unidade.isEmpty() ? "" : " " + t.unidade),
                    ok ? NamedTextColor.GREEN : NamedTextColor.WHITE));
        }
        l.add(Component.text(feitaHoje(p) ? "Concluída hoje. Volte amanhã!" : "Prêmio: XP de caçador e Cristais de Mana.",
                feitaHoje(p) ? NamedTextColor.AQUA : NamedTextColor.GRAY));
        int seq = sequencia(p);
        if (seq > 0) l.add(Component.text("Dias seguidos: " + seq + " (a cada 7, um prêmio a mais)", NamedTextColor.GRAY));
        return l;
    }

    public void mostrar(Player p) {
        comecarDia(p);
        JanelaSistema.mostrar(p, "Missão Diária", linhas(p));
    }

    // =====================================================================
    //  Eventos e o relógio
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoAgachar(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        comecarDia(p);
        if (feitaHoje(p)) return;
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(K_AGACHAR, PersistentDataType.INTEGER, pdc.getOrDefault(K_AGACHAR, PersistentDataType.INTEGER, 0) + 1);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (comecarDia(p)) {
                List<Component> l = new ArrayList<>();
                l.add(Component.text("Missão Diária: Preparação para ficar forte.", NamedTextColor.WHITE));
                l.addAll(linhas(p));
                l.add(Component.text("Veja o progresso em /masmorra missao.", NamedTextColor.DARK_GRAY));
                JanelaSistema.mostrar(p, "Nova missão", l);
            }
        }, 100L);
    }

    /** A cada 5 segundos: o dia virou? a missão ficou completa? */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            comecarDia(p);
            if (!feitaHoje(p) && completa(p)) premiar(p);
        }
    }

    private void premiar(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        long ontem = hoje() - 1;
        long ultima = pdc.getOrDefault(K_FEITA, PersistentDataType.LONG, -10L);
        int seq = ultima == ontem ? sequencia(p) + 1 : 1;
        pdc.set(K_FEITA, PersistentDataType.LONG, hoje());
        pdc.set(K_SEQ, PersistentDataType.INTEGER, seq);
        Rank r = Cacador.rank(p);
        int xp = 10 + 5 * r.ordinal();
        int cristais = 1 + r.ordinal() / 2;
        List<ItemStack> premios = new ArrayList<>();
        premios.add(ManaCristal.criar(ManaCristal.Tamanho.MEDIO, cristais));
        boolean semana = seq % 7 == 0;
        if (semana) {
            premios.add(ManaCristal.criar(ManaCristal.Tamanho.GRANDE, 1));
            Inscricao[] is = Inscricao.values();
            premios.add(is[ThreadLocalRandom.current().nextInt(is.length)].criar(1));
        }
        for (ItemStack i : premios) for (ItemStack sobra : p.getInventory().addItem(i).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        p.showTitle(Title.title(Component.text("[Sistema]", JanelaSistema.AZUL, TextDecoration.BOLD),
                Component.text("Missão Diária concluída!", NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);
        List<Component> l = new ArrayList<>();
        l.add(Component.text("Missão Diária concluída!", NamedTextColor.GREEN));
        l.add(Component.text("Prêmio: " + xp + " XP de caçador e " + cristais + " Cristal(is) de Mana médio(s).", NamedTextColor.WHITE));
        if (semana) l.add(Component.text(seq + " dias seguidos: + 1 Cristal de Mana grande e 1 Inscrição.", NamedTextColor.AQUA));
        else l.add(Component.text("Dias seguidos: " + seq + ".", NamedTextColor.GRAY));
        JanelaSistema.mostrar(p, "Missão Diária", l);
        Cacador.darXp(p, xp);
    }

    // ---- admin ----

    /** Admin: completa as tarefas de hoje (o prêmio vem no próximo tick). */
    public static void completarAdmin(Player p) {
        comecarDia(p);
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(K_AGACHAR, PersistentDataType.INTEGER, Tarefa.AGACHAR.meta);
        pdc.set(K_BASE, PersistentDataType.STRING, (p.getStatistic(Statistic.JUMP) - Tarefa.PULAR.meta) + ","
                + (p.getStatistic(Statistic.SPRINT_ONE_CM) - Tarefa.CORRER.meta * 100L) + "," + (p.getStatistic(Statistic.MOB_KILLS) - Tarefa.ABATER.meta));
    }

    /** Admin: recomeça o dia (e a missão de hoje pode ser feita de novo). */
    public static void zerarAdmin(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.remove(K_DIA);
        pdc.remove(K_FEITA);
        comecarDia(p);
    }
}
