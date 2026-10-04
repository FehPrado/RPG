package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefes;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Combos de arma: com uma arma na mão, uma sequência de 3 cliques solta um golpe. Cada
 * tipo de arma tem a sua proficiência (1 a 20), que sobe usando a arma e libera golpes
 * novos; o jogador escolhe quais 4 ficam nas sequências. Os golpes gastam Vigor.
 */
public final class Combos implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFB74D);
    public static final int NIVEL_MAXIMO = 20;
    private static final int S_INFO = 8, S_NIVEL = 13, S_FECHAR = 49;
    private static final int[] S_ABAS = {1, 2, 3, 4, 5, 6};
    private static final int[] S_ESPACOS = {19, 21, 23, 25};
    private static final int[] S_GOLPES = {37, 38, 39, 40, 41, 42, 43};
    private static final int[] S_EXTRAS = {28, 29, 30, 31, 32, 33, 34};

    /** Sequência sendo digitada. */
    private static final class Entrada {
        final TipoArma tipo;
        final StringBuilder seq = new StringBuilder();
        long ultimo;
        /** Força do ataque no último tick (a estocada da lança só aparece por aqui). */
        float forcaAtaque = 1f;

        Entrada(TipoArma tipo) { this.tipo = tipo; }
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        TipoArma tipo;
        int espaco = -1;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final Golpes golpes;
    private final Map<UUID, Entrada> entradas = new HashMap<>();
    private final Map<UUID, Double> vigor = new HashMap<>();
    private final Map<UUID, Long> emCombate = new HashMap<>();
    private final Map<UUID, Long> mostrarAte = new HashMap<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private final Map<UUID, Map<Golpe, Long>> recargas = new HashMap<>();
    /** Finalizador carregado: até quando vale. */
    private final Map<UUID, Long> finalizador = new HashMap<>();
    /** Golpes recentes de cada jogador (golpe, quando) para o finalizador em cadeia. */
    private final Map<UUID, java.util.Deque<Object[]>> cadeias = new HashMap<>();
    /** Tick do último clique direito/soltar item: o braço balança junto e não pode contar como esquerdo. */
    private final Map<UUID, Integer> semEsquerdo = new HashMap<>();
    private final Map<UUID, Long> esperaXp = new HashMap<>();
    /** Tick do último clique esquerdo contado (o balanço e a estocada da lança não contam duas vezes). */
    private final Map<UUID, Integer> ultimoEsquerdo = new HashMap<>();
    private final Map<TipoArma, NamespacedKey> kXp = new EnumMap<>(TipoArma.class);
    private final Map<TipoArma, NamespacedKey> kEspacos = new EnumMap<>(TipoArma.class);

    public Combos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.golpes = new Golpes(plugin);
        for (TipoArma t : TipoArma.values()) {
            kXp.put(t, new NamespacedKey(plugin, "prof_" + t.id()));
            kEspacos.put(t, new NamespacedKey(plugin, "combos_" + t.id()));
        }
    }

    public Golpes golpes() { return golpes; }
    private Settings cfg() { return plugin.settings(); }
    private static long agora() { return System.currentTimeMillis(); }

    // =====================================================================
    //  Proficiência
    // =====================================================================

    /** XP para ir do nível n para o n+1. */
    public double xpPara(int n) {
        return cfg().comXpBase * Math.pow(n, cfg().comXpExpoente);
    }

    public double xp(Player p, TipoArma t) {
        Double v = p.getPersistentDataContainer().get(kXp.get(t), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }

    public int nivel(Player p, TipoArma t) {
        double resto = xp(p, t);
        int n = 1;
        while (n < NIVEL_MAXIMO && resto >= xpPara(n)) {
            resto -= xpPara(n);
            n++;
        }
        return n;
    }

    /** Quanto falta (0 a 1) para o próximo nível. */
    public double progresso(Player p, TipoArma t) {
        double resto = xp(p, t);
        int n = 1;
        while (n < NIVEL_MAXIMO && resto >= xpPara(n)) {
            resto -= xpPara(n);
            n++;
        }
        return n >= NIVEL_MAXIMO ? 1 : resto / xpPara(n);
    }

    public void darXp(Player p, TipoArma t, double qtd) {
        if (qtd <= 0) return;
        int antes = nivel(p, t);
        p.getPersistentDataContainer().set(kXp.get(t), PersistentDataType.DOUBLE, xp(p, t) + qtd * cfg().multiplicadorXp);
        int depois = nivel(p, t);
        if (depois > antes) subiu(p, t, antes, depois);
    }

    /** Admin: define o nível direto. */
    public void definirNivel(Player p, TipoArma t, int nivel) {
        int antes = nivel(p, t);
        double total = 0;
        for (int n = 1; n < Math.max(1, Math.min(NIVEL_MAXIMO, nivel)); n++) total += xpPara(n);
        p.getPersistentDataContainer().set(kXp.get(t), PersistentDataType.DOUBLE, total + 0.01);
        int depois = nivel(p, t);
        if (depois > antes) subiu(p, t, antes, depois);
    }

    private void subiu(Player p, TipoArma t, int antes, int depois) {
        p.showTitle(Title.title(Component.text(t.simbolo() + " " + t.nome() + " " + depois, t.cor(), TextDecoration.BOLD),
                Component.text("Proficiência de arma subiu!", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        Golpe[] esp = equipados(p, t);
        boolean mudou = false;
        for (Golpe g : Golpe.da(t)) {
            if (g.nivel() <= antes || g.nivel() > depois) continue;
            p.sendMessage(Component.text("⚔ Novo golpe de " + t.nome() + ": ", COR)
                    .append(Component.text(g.nome(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" — " + g.descricao() + " (/combos)", NamedTextColor.GRAY)));
            // Preenche o primeiro espaço vazio sozinho.
            for (int i = 0; i < esp.length; i++) {
                if (esp[i] == null && !contem(esp, g)) {
                    esp[i] = g;
                    mudou = true;
                    break;
                }
            }
        }
        if (mudou) salvarEspacos(p, t, esp);
        atualizarTitulos(p);
    }

    private static boolean contem(Golpe[] arr, Golpe g) {
        for (Golpe x : arr) if (x == g) return true;
        return false;
    }

    /** Os 4 golpes nas sequências (null = vazio). Sem nada salvo, usa os liberados em ordem. */
    public Golpe[] equipados(Player p, TipoArma t) {
        Golpe[] r = new Golpe[4];
        String s = p.getPersistentDataContainer().get(kEspacos.get(t), PersistentDataType.STRING);
        int nivel = nivel(p, t);
        if (s == null) {
            int i = 0;
            for (Golpe g : Golpe.da(t)) if (g.nivel() <= nivel && i < 4) r[i++] = g;
            return r;
        }
        String[] partes = s.split(",", -1);
        for (int i = 0; i < 4 && i < partes.length; i++) {
            Golpe g = Golpe.porId(partes[i]);
            if (g == null) continue;
            // Golpes de lenda/classe ficam guardados mesmo sem a lenda/classe agora (avisa na hora de usar).
            if (g.extra() ? (g.tipo() == null || g.tipo() == t) : (g.tipo() == t && g.nivel() <= nivel)) r[i] = g;
        }
        return r;
    }

    private void salvarEspacos(Player p, TipoArma t, Golpe[] esp) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(i == 0 ? "" : ",").append(esp[i] == null ? "-" : esp[i].id());
        p.getPersistentDataContainer().set(kEspacos.get(t), PersistentDataType.STRING, sb.toString());
    }

    // =====================================================================
    //  Vigor
    // =====================================================================

    public double vigorMaximo(Player p) {
        return cfg().comVigorMaximo + 0.5 * plugin.stats().getNivel(p, Skill.COMBATE) + plugin.talentos().bonusVigor(p);
    }

    public double vigor(Player p) {
        return vigor.getOrDefault(p.getUniqueId(), vigorMaximo(p));
    }

    public void darVigor(Player p, double qtd) {
        vigor.put(p.getUniqueId(), Math.max(0, Math.min(vigorMaximo(p), vigor(p) + qtd)));
        mostrarAte.put(p.getUniqueId(), agora() + 3000);
    }

    private void marcarCombate(Entity e) {
        if (e instanceof Player p) emCombate.put(p.getUniqueId(), agora());
    }

    /** A cada 10 ticks: Vigor volta, barras e sequências que expiraram. */
    public void tick() {
        long t = agora();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            double max = vigorMaximo(p), v = vigor(p);
            if (v < max) {
                boolean luta = t - emCombate.getOrDefault(id, 0L) < 5000;
                double porSeg = luta ? cfg().comVigorPorSegundo : cfg().comVigorForaDeCombate;
                v = Math.min(max, v + porSeg / 2);
                vigor.put(id, v);
            }
            Entrada en = entradas.get(id);
            if (en != null && t - en.ultimo > cfg().comJanelaMs) {
                entradas.remove(id);
                if (en.seq.length() >= 2) p.sendActionBar(Component.text("✖ Combo perdido", NamedTextColor.DARK_GRAY));
            }
            barra(p, v, max, t);
        }
        esperaXp.values().removeIf(x -> t - x > 5000);
    }

    private void barra(Player p, double v, double max, long t) {
        UUID id = p.getUniqueId();
        boolean mostrar = v < max - 0.01 || t < mostrarAte.getOrDefault(id, 0L);
        BossBar b = barras.get(id);
        if (!mostrar) {
            if (b != null) {
                p.hideBossBar(b);
                barras.remove(id);
            }
            return;
        }
        Component nome = Component.text("⚡ Vigor " + (int) v + " / " + (int) max, COR);
        float prog = (float) Math.max(0, Math.min(1, v / max));
        if (b == null) {
            b = BossBar.bossBar(nome, prog, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
            barras.put(id, b);
            p.showBossBar(b);
        } else {
            b.name(nome);
            b.progress(prog);
        }
    }

    public void parar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            BossBar b = barras.remove(p.getUniqueId());
            if (b != null) p.hideBossBar(b);
        }
        golpes.parar();
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    private boolean podeUsar(Player p) {
        if (!cfg().comAtivo || p.isSneaking()) return false;
        GameMode g = p.getGameMode();
        if (g == GameMode.SPECTATOR) return false;
        return !plugin.maldicoes().transformado(p);
    }

    /** Recebe um clique. @return true se ele entrou numa sequência. */
    private boolean clique(Player p, char c, TipoArma t) {
        UUID id = p.getUniqueId();
        long t0 = agora();
        Entrada en = entradas.get(id);
        if (en != null && (t0 - en.ultimo > cfg().comJanelaMs || en.tipo != t)) {
            entradas.remove(id);
            en = null;
        }
        if (en == null) {
            if (c != t.inicio()) return false;
            en = new Entrada(t);
            en.forcaAtaque = p.getAttackCooldown();
            entradas.put(id, en);
        }
        en.seq.append(c);
        en.ultimo = t0;
        int n = en.seq.length();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.35f, 0.9f + 0.25f * n);
        if (n < 3) {
            p.sendActionBar(progresso(en.seq.toString(), t));
            return true;
        }
        entradas.remove(id);
        executar(p, t, en.seq.toString());
        return true;
    }

    private static Component progresso(String feito, TipoArma t) {
        Component c = Component.text(t.simbolo() + "  ", t.cor());
        for (int i = 0; i < 3; i++) {
            if (i > 0) c = c.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            c = c.append(i < feito.length() ? Component.text(String.valueOf(feito.charAt(i)), NamedTextColor.YELLOW, TextDecoration.BOLD)
                    : Component.text("?", NamedTextColor.DARK_GRAY));
        }
        return c;
    }

    private void executar(Player p, TipoArma t, String seq) {
        String[] seqs = t.sequencias();
        int espaco = -1;
        for (int i = 0; i < seqs.length; i++) if (seqs[i].equals(seq)) espaco = i;
        if (espaco < 0) return;
        Golpe g = equipados(p, t)[espaco];
        if (g == null) {
            falhar(p, "Nenhum golpe em " + TipoArma.bonita(seq) + " (escolha em /combos)");
            return;
        }
        String falta = faltando(p, g);
        if (falta != null) {
            falhar(p, g.nome() + ": " + falta);
            return;
        }
        long t0 = agora();
        Long pronto = recargas.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Golpe.class)).get(g);
        if (pronto != null && pronto > t0) {
            falhar(p, g.nome() + " em recarga (" + String.format("%.1f", (pronto - t0) / 1000.0) + " s)");
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && vigor(p) < g.vigor()) {
            falhar(p, "Vigor insuficiente para " + g.nome() + " (" + (int) g.vigor() + ")");
            return;
        }
        double forca = 1 + cfg().comDanoPorNivel * (nivel(p, t) - 1);
        Long carga = finalizador.get(p.getUniqueId());
        boolean finaliza = carga != null && carga > t0;
        if (finaliza) forca *= 1.5;
        if (!golpes.usar(p, g, forca)) return;
        if (!criativo) darVigor(p, -g.vigor());
        else mostrarAte.put(p.getUniqueId(), t0 + 3000);
        recargas.get(p.getUniqueId()).put(g, t0 + g.recarga() * 1000L);
        emCombate.put(p.getUniqueId(), t0);
        plugin.titulos().registrar(p, "combos", 1);
        if (finaliza) {
            finalizador.remove(p.getUniqueId());
            golpes.finalizar(p, forca);
            plugin.titulos().registrar(p, "finalizadores", 1);
            p.showTitle(Title.title(Component.empty(), Component.text("⚡ FINALIZADOR! ⚡", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(900), Duration.ofMillis(300))));
        } else {
            encadear(p, g, t0);
        }
        p.sendActionBar(Component.text(t.simbolo() + " " + g.nome() + "!", t.cor(), TextDecoration.BOLD));
    }

    /** Finalizador em cadeia: 3 golpes diferentes em 10 s carregam o próximo (+50% e explosão). */
    private void encadear(Player p, Golpe g, long t0) {
        java.util.Deque<Object[]> fila = cadeias.computeIfAbsent(p.getUniqueId(), k -> new java.util.ArrayDeque<>());
        fila.addLast(new Object[]{g, t0});
        fila.removeIf(x -> t0 - (long) x[1] > 10_000);
        java.util.Set<Golpe> diferentes = java.util.EnumSet.noneOf(Golpe.class);
        for (Object[] x : fila) diferentes.add((Golpe) x[0]);
        if (diferentes.size() < 3) return;
        fila.clear();
        finalizador.put(p.getUniqueId(), t0 + 8000);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.8f);
        p.showTitle(Title.title(Component.empty(), Component.text("⚡ Finalizador pronto: o próximo combo é devastador!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(1200), Duration.ofMillis(300))));
    }

    /** O que falta para usar o golpe agora (null = pode). */
    public String faltando(Player p, Golpe g) {
        if (g.lenda() != null) return temLenda(p, g.lenda()) ? null
                : "precisa da lenda " + g.lenda().nome() + " (arma: na mão; armadura ou ferramenta: com você)";
        if (g.classe() != null) {
            var ativa = plugin.classes().ativa(p);
            boolean ok = ativa == g.classe() || (ativa != null && ativa.requisitos().contains(g.classe()));
            return ok ? null : "precisa da classe " + g.classe().nome() + " ativa";
        }
        return null;
    }

    public boolean disponivel(Player p, Golpe g) {
        return faltando(p, g) == null;
    }

    /** Lenda de arma: na mão. Armadura e ferramenta: em qualquer lugar do inventário. */
    private boolean temLenda(Player p, br.rpgatributos.lenda.Lenda l) {
        if (plugin.lendas().lenda(p.getInventory().getItemInMainHand()) == l) return true;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && TipoArma.de(i) == null && plugin.lendas().lenda(i) == l) return true;
        }
        return false;
    }

    /** Zera a recarga de um golpe (ex.: Decapitar devolve o Salto Esmagador). */
    void zerarRecarga(Player p, Golpe g) {
        Map<Golpe, Long> m = recargas.get(p.getUniqueId());
        if (m != null) m.remove(g);
    }

    private static void falhar(Player p, String msg) {
        p.sendActionBar(Component.text("✖ " + msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.6f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoInteragir(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) {
            // No meio de um combo (do 2º clique em diante), o escudo/item da outra mão não é usado.
            // Um clique só ainda levanta o escudo normalmente.
            Entrada en = entradas.get(p.getUniqueId());
            if (en != null && en.seq.length() >= 2) e.setUseItemInHand(Event.Result.DENY);
            return;
        }
        if (!podeUsar(p)) return;
        TipoArma t = TipoArma.de(e.getItem());
        if (t == null) return;
        if (a == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && (b.getType().isInteractable() || plugin.ehEstacao(b))) return; // abrir baú, porta...
        }
        semEsquerdo.put(p.getUniqueId(), Bukkit.getCurrentTick());
        boolean meio = entradas.containsKey(p.getUniqueId());
        if (clique(p, 'D', t) && (meio || t == TipoArma.ARCO)) {
            // No meio da sequência não puxa a flecha nem arremessa o tridente.
            e.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoBalancar(PlayerArmSwingEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Integer tick = semEsquerdo.get(p.getUniqueId());
        if (tick != null && tick >= Bukkit.getCurrentTick() - 1) return;
        if (!podeUsar(p)) return;
        Integer ult = ultimoEsquerdo.get(p.getUniqueId());
        if (ult != null && ult >= Bukkit.getCurrentTick() - 1) return;
        ultimoEsquerdo.put(p.getUniqueId(), Bukkit.getCurrentTick());
        TipoArma t = TipoArma.de(p.getInventory().getItemInMainHand());
        if (t != null) clique(p, 'E', t);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoSoltar(PlayerDropItemEvent e) {
        semEsquerdo.put(e.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
    }

    // =====================================================================
    //  XP de proficiência
    // =====================================================================

    private static boolean alvoContaXp(Entity e) {
        return e instanceof LivingEntity && !(e instanceof ArmorStand) && (e instanceof Enemy || e instanceof Player
                || Chefes.ehChefe(e) || e instanceof Mob);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        marcarCombate(e.getEntity());
        Player p = null;
        TipoArma t = null;
        if (e.getDamager() instanceof Player pl) {
            p = pl;
            t = TipoArma.de(pl.getInventory().getItemInMainHand());
        } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl) {
            p = pl;
            t = TipoArma.de(pl.getInventory().getItemInMainHand());
            if (t == null && TipoArma.de(pl.getInventory().getItemInOffHand()) == TipoArma.ARCO) t = TipoArma.ARCO;
        }
        if (p == null) return;
        marcarCombate(p);
        if (t == null || !alvoContaXp(e.getEntity()) || e.getEntity() == p) return;
        long t0 = agora();
        Long ultimo = esperaXp.get(p.getUniqueId());
        if (ultimo != null && t0 - ultimo < 300) return;
        esperaXp.put(p.getUniqueId(), t0);
        double xp = cfg().comXpAcerto;
        if (e.getEntity().fromMobSpawner()) xp *= 0.25;
        darXp(p, t, xp);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || morto == p) return;
        TipoArma t = TipoArma.de(p.getInventory().getItemInMainHand());
        if (t == null && morto.getLastDamageCause() instanceof EntityDamageByEntityEvent ev && ev.getDamager() instanceof Projectile
                && TipoArma.de(p.getInventory().getItemInOffHand()) == TipoArma.ARCO) t = TipoArma.ARCO;
        if (t == null) return;
        double xp;
        if (Chefes.ehChefe(morto)) xp = cfg().comXpChefe;
        else if (morto instanceof Enemy || morto instanceof Player) xp = cfg().comXpAbate;
        else if (morto instanceof Mob) xp = cfg().comXpAbate / 3;
        else return;
        if (morto.fromMobSpawner()) xp *= 0.25;
        darXp(p, t, xp);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        marcarCombate(e.getEntity());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        entradas.remove(id);
        barras.remove(id);
        semEsquerdo.remove(id);
        emCombate.remove(id);
        mostrarAte.remove(id);
    }

    // =====================================================================
    //  /combos
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        TipoArma t = args.length > 0 ? TipoArma.porId(args[0]) : null;
        if (t == null) t = TipoArma.de(p.getInventory().getItemInMainHand());
        abrirMenu(p, t == null ? TipoArma.ESPADA : t);
        return true;
    }

    public void abrirMenu(Player p, TipoArma t) {
        Tela tela = new Tela();
        tela.tipo = t;
        tela.inventario = Bukkit.createInventory(tela, 54, Component.text("⚔ Combos de arma"));
        desenhar(p, tela);
        p.openInventory(tela.inventario);
    }

    private void desenhar(Player p, Tela tela) {
        Inventory inv = tela.inventario;
        inv.clear();
        TipoArma t = tela.tipo;
        int nivel = nivel(p, t);
        TipoArma[] tipos = TipoArma.values();
        for (int i = 0; i < tipos.length && i < S_ABAS.length; i++) {
            TipoArma x = tipos[i];
            ItemStack aba = item(x.icone(), Component.text(x.simbolo() + " " + x.nome(), x.cor(), TextDecoration.BOLD), List.of(
                    Component.text("Proficiência " + nivel(p, x) + "/" + NIVEL_MAXIMO, NamedTextColor.GRAY),
                    x == t ? Component.text("● Aberta", NamedTextColor.GREEN) : Component.text("» Clique para ver", NamedTextColor.YELLOW)));
            if (x == t) aba.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_ABAS[i], aba);
        }
        inv.setItem(S_INFO, item(Material.GLOWSTONE_DUST, Component.text("⚡ Vigor", COR, TextDecoration.BOLD), List.of(
                Component.text((int) vigor(p) + " / " + (int) vigorMaximo(p), NamedTextColor.WHITE),
                Component.text("Volta sozinho, mais rápido fora de luta.", NamedTextColor.GRAY),
                Component.text("O máximo cresce com o nível de Combate.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Como usar: com a arma na mão, faça", NamedTextColor.GRAY),
                Component.text("3 cliques seguidos (D = direito, E = esquerdo).", NamedTextColor.GRAY),
                Component.text(t == TipoArma.ARCO ? "Arco e besta começam com E." : "Armas corpo a corpo começam com D.", NamedTextColor.YELLOW),
                Component.text("Agachado não solta combo.", NamedTextColor.DARK_GRAY))));
        List<Component> loreNivel = new ArrayList<>();
        loreNivel.add(Component.text("Nível " + nivel + "/" + NIVEL_MAXIMO, NamedTextColor.WHITE));
        if (nivel < NIVEL_MAXIMO) loreNivel.add(barra(progresso(p, t)));
        loreNivel.add(Component.text("Golpes +" + Math.round(cfg().comDanoPorNivel * 100 * (nivel - 1)) + "% de dano", NamedTextColor.GRAY));
        loreNivel.add(Component.text("Sobe acertando e derrotando inimigos", NamedTextColor.DARK_GRAY));
        loreNivel.add(Component.text("com " + t.nome().toLowerCase() + " na mão.", NamedTextColor.DARK_GRAY));
        inv.setItem(S_NIVEL, item(Material.EXPERIENCE_BOTTLE, Component.text(t.simbolo() + " Proficiência: " + t.nome(), t.cor(), TextDecoration.BOLD), loreNivel));

        Golpe[] esp = equipados(p, t);
        String[] seqs = t.sequencias();
        for (int i = 0; i < 4; i++) {
            Golpe g = esp[i];
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Sequência: " + TipoArma.bonita(seqs[i]), NamedTextColor.YELLOW));
            if (g != null) {
                lore.add(Component.text(g.descricao(), NamedTextColor.GRAY));
                lore.add(Component.text("⚡ " + (int) g.vigor() + " vigor · ⏱ " + g.recarga() + " s", NamedTextColor.DARK_GRAY));
            } else {
                lore.add(Component.text("Vazio.", NamedTextColor.DARK_GRAY));
            }
            lore.add(Component.empty());
            lore.add(tela.espaco == i ? Component.text("● Selecionado: clique num golpe abaixo", NamedTextColor.GREEN)
                    : Component.text("» Clique para trocar o golpe daqui", NamedTextColor.YELLOW));
            if (g != null) lore.add(Component.text("Shift + clique: esvaziar", NamedTextColor.DARK_GRAY));
            ItemStack it = item(g == null ? Material.GRAY_STAINED_GLASS_PANE : g.icone(),
                    Component.text(TipoArma.bonita(seqs[i]) + "  ", NamedTextColor.GOLD, TextDecoration.BOLD)
                            .append(Component.text(g == null ? "—" : g.nome(), g == null ? NamedTextColor.DARK_GRAY : NamedTextColor.WHITE)), lore);
            if (tela.espaco == i) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_ESPACOS[i], it);
        }

        List<Golpe> todos = Golpe.da(t);
        for (int i = 0; i < todos.size() && i < S_GOLPES.length; i++) {
            Golpe g = todos.get(i);
            boolean livre = g.nivel() <= nivel;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(g.descricao(), NamedTextColor.GRAY));
            lore.add(Component.text("⚡ " + (int) g.vigor() + " vigor · ⏱ " + g.recarga() + " s", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            if (!livre) lore.add(Component.text("✖ Libera na proficiência " + g.nivel(), NamedTextColor.RED));
            else if (contem(esp, g)) lore.add(Component.text("● Em uso", NamedTextColor.GREEN));
            else lore.add(Component.text(tela.espaco >= 0 ? "» Clique para colocar no espaço selecionado" : "Selecione um espaço acima primeiro",
                    tela.espaco >= 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            ItemStack it = item(livre ? g.icone() : Material.GRAY_DYE,
                    Component.text(g.nome(), livre ? t.cor() : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore);
            if (livre && contem(esp, g)) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_GOLPES[i], it);
        }
        List<Golpe> extras = extrasVisiveis(p, t, esp);
        for (int i = 0; i < extras.size() && i < S_EXTRAS.length; i++) {
            Golpe g = extras.get(i);
            String falta = faltando(p, g);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(g.lenda() != null ? "✦ Golpe da lenda " + g.lenda().nome() : "⚜ Golpe de classe: " + g.classe().nome(),
                    g.lenda() != null ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GOLD));
            lore.add(Component.text(g.descricao(), NamedTextColor.GRAY));
            lore.add(Component.text("⚡ " + (int) g.vigor() + " vigor · ⏱ " + g.recarga() + " s", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            if (falta != null) lore.add(Component.text("✖ " + falta.substring(0, 1).toUpperCase() + falta.substring(1), NamedTextColor.RED));
            else if (contem(esp, g)) lore.add(Component.text("● Em uso", NamedTextColor.GREEN));
            else lore.add(Component.text(tela.espaco >= 0 ? "» Clique para colocar no espaço selecionado" : "Selecione um espaço acima primeiro",
                    tela.espaco >= 0 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            ItemStack it = item(falta == null ? g.icone() : Material.GRAY_DYE,
                    Component.text(g.nome(), falta == null ? (g.lenda() != null ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GOLD) : NamedTextColor.DARK_GRAY,
                            TextDecoration.BOLD), lore);
            if (falta == null && contem(esp, g)) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_EXTRAS[i], it);
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static Component barra(double prog) {
        int cheios = (int) Math.round(prog * 20);
        return Component.text("■".repeat(cheios), NamedTextColor.GOLD).append(Component.text("■".repeat(20 - cheios), NamedTextColor.DARK_GRAY));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela tela)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        TipoArma[] tipos = TipoArma.values();
        for (int i = 0; i < tipos.length && i < S_ABAS.length; i++) {
            if (S_ABAS[i] == slot) {
                tela.tipo = tipos[i];
                tela.espaco = -1;
                desenhar(p, tela);
                return;
            }
        }
        Golpe[] esp = equipados(p, tela.tipo);
        for (int i = 0; i < 4; i++) {
            if (S_ESPACOS[i] != slot) continue;
            if (e.isShiftClick() && esp[i] != null) {
                esp[i] = null;
                salvarEspacos(p, tela.tipo, esp);
                tela.espaco = -1;
            } else {
                tela.espaco = tela.espaco == i ? -1 : i;
            }
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            desenhar(p, tela);
            return;
        }
        List<Golpe> todos = Golpe.da(tela.tipo);
        for (int i = 0; i < todos.size() && i < S_GOLPES.length; i++) {
            if (S_GOLPES[i] != slot) continue;
            Golpe g = todos.get(i);
            if (g.nivel() > nivel(p, tela.tipo)) {
                falhar(p, "Libera na proficiência " + g.nivel());
                return;
            }
            if (tela.espaco < 0) {
                falhar(p, "Clique primeiro num espaço de sequência");
                return;
            }
            // Se já estava em outro espaço, troca os dois de lugar.
            for (int k = 0; k < 4; k++) if (esp[k] == g) esp[k] = esp[tela.espaco];
            esp[tela.espaco] = g;
            salvarEspacos(p, tela.tipo, esp);
            tela.espaco = -1;
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.2f);
            desenhar(p, tela);
            return;
        }
        List<Golpe> extras = extrasVisiveis(p, tela.tipo, esp);
        for (int i = 0; i < extras.size() && i < S_EXTRAS.length; i++) {
            if (S_EXTRAS[i] != slot) continue;
            Golpe g = extras.get(i);
            String falta = faltando(p, g);
            if (falta != null) {
                falhar(p, falta);
                return;
            }
            if (tela.espaco < 0) {
                falhar(p, "Clique primeiro num espaço de sequência");
                return;
            }
            for (int k = 0; k < 4; k++) if (esp[k] == g) esp[k] = esp[tela.espaco];
            esp[tela.espaco] = g;
            salvarEspacos(p, tela.tipo, esp);
            tela.espaco = -1;
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 0.6f, 1.2f);
            desenhar(p, tela);
            return;
        }
    }

    /** Golpes de classe da arma (sempre aparecem) e de lenda (só os que você tem ou já pôs numa sequência). */
    private List<Golpe> extrasVisiveis(Player p, TipoArma t, Golpe[] esp) {
        List<Golpe> l = new ArrayList<>();
        for (Golpe g : Golpe.extras(t)) {
            if (g.classe() != null || disponivel(p, g) || contem(esp, g)) l.add(g);
        }
        return l;
    }

    /** Contadores dos títulos de combate (proficiência 20 em cada arma). */
    private void atualizarTitulos(Player p) {
        int mestradas = 0;
        for (TipoArma x : TipoArma.values()) {
            int n = nivel(p, x);
            plugin.titulos().definirMinimo(p, "proficiencia_" + x.id(), n);
            if (n >= NIVEL_MAXIMO) mestradas++;
        }
        plugin.titulos().definirMinimo(p, "armas_mestradas", mestradas);
    }

    @EventHandler
    public void aoEntrar(org.bukkit.event.player.PlayerJoinEvent e) {
        atualizarTitulos(e.getPlayer());
    }

    /** A cada tick: a estocada (clique esquerdo) da lança não gera evento nenhum; ela só zera a força do ataque. */
    public void tickRapido() {
        if (entradas.isEmpty()) return;
        int agoraTick = Bukkit.getCurrentTick();
        for (Map.Entry<UUID, Entrada> en : new ArrayList<>(entradas.entrySet())) {
            Entrada e = en.getValue();
            if (e.tipo != TipoArma.LANCA) continue;
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null) continue;
            float f = p.getAttackCooldown();
            Integer semE = semEsquerdo.get(p.getUniqueId());
            boolean logoAposDireito = semE != null && semE >= agoraTick - 2;
            if (f + 0.35f < e.forcaAtaque && !logoAposDireito && TipoArma.de(p.getInventory().getItemInMainHand()) == TipoArma.LANCA) {
                Integer ult = ultimoEsquerdo.get(p.getUniqueId());
                if (ult == null || ult < agoraTick - 1) {
                    ultimoEsquerdo.put(p.getUniqueId(), agoraTick);
                    if (podeUsar(p)) clique(p, 'E', TipoArma.LANCA);
                }
            }
            Entrada atual = entradas.get(en.getKey());
            if (atual != null) atual.forcaAtaque = f;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
