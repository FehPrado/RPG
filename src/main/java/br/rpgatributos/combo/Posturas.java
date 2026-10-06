package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.classe.Classe;
import br.rpgatributos.fe.Deus;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As posturas em jogo: guarda a escolhida, aplica os efeitos (dano, dano recebido, velocidade,
 * empurrão), mostra a aura e a pegada da arma (pelo pacote de recursos), os números de dano dos
 * golpes e o menu /postura.
 */
public final class Posturas implements Listener, CommandExecutor {

    private static final NamespacedKey K_VEL = new NamespacedKey("rpgatributos", "postura_velocidade");
    private static final NamespacedKey K_EMPURRAO = new NamespacedKey("rpgatributos", "postura_empurrao");
    private static final int[] S_POSTURAS = {10, 11, 12, 13, 15};

    private final RPGAtributos plugin;
    private final Combos combos;
    private final NamespacedKey kPostura;
    private final Map<UUID, Long> ultimoPasso = new HashMap<>();
    private final Map<UUID, double[]> aplicado = new HashMap<>();
    private int ciclo;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    Posturas(RPGAtributos plugin, Combos combos) {
        this.plugin = plugin;
        this.combos = combos;
        this.kPostura = new NamespacedKey(plugin, "postura");
    }

    // =====================================================================
    //  Qual vale agora
    // =====================================================================

    public Postura escolhida(Player p) {
        Postura ps = Postura.porId(p.getPersistentDataContainer().getOrDefault(kPostura, PersistentDataType.STRING, "neutra"));
        return ps == null ? Postura.NEUTRA : ps;
    }

    /** A postura que vale com a arma da mão (null = sem arma de combo na mão). */
    public Postura ativa(Player p) {
        TipoArma t = TipoArma.de(p.getInventory().getItemInMainHand());
        if (t == null) return null;
        Postura ps = escolhida(p);
        if (ps == Postura.MESTRA && combos.nivel(p, t) < Combos.NIVEL_MAXIMO) return Postura.NEUTRA;
        return ps;
    }

    private static TipoArma arma(Player p) {
        return TipoArma.de(p.getInventory().getItemInMainHand());
    }

    public void escolher(Player p, Postura ps) {
        p.getPersistentDataContainer().set(kPostura, PersistentDataType.STRING, ps.id());
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.8f, 1.2f);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(ps.corParticula(), 1.4f));
        TipoArma t = arma(p);
        String nome = ps == Postura.MESTRA && t != null ? "Mestra (" + Postura.nomeMestra(t) + ")" : ps.nome();
        p.sendActionBar(Component.text(ps.simbolo() + " Postura " + nome, ps.cor(), TextDecoration.BOLD));
        if (ps == Postura.MESTRA && t != null && combos.nivel(p, t) < Combos.NIVEL_MAXIMO) {
            p.sendMessage(Component.text("✦ A postura Mestra de " + t.nome() + " libera na proficiência " + Combos.NIVEL_MAXIMO
                    + ". Até lá ela vale como Neutra.", NamedTextColor.GRAY));
        }
        aplicar(p);
        pegada(p);
    }

    /** Nome curto para a barra de vigor: "▲ Ofensiva". */
    String rotulo(Player p) {
        Postura ps = ativa(p);
        if (ps == null || ps == Postura.NEUTRA) return null;
        TipoArma t = arma(p);
        return ps.simbolo() + " " + (ps == Postura.MESTRA && t != null ? Postura.nomeMestra(t) : ps.nome());
    }

    // =====================================================================
    //  Números da postura
    // =====================================================================

    private boolean naChuvaOuAgua(Player p) {
        if (p.isInWater()) return true;
        Location l = p.getLocation();
        return p.getWorld().hasStorm() && p.getWorld().getHighestBlockYAt(l) <= l.getBlockY();
    }

    // =====================================================================
    //  Afinidade: a classe e o deus combinam com uma das posturas gerais
    // =====================================================================

    static Postura afinidade(Classe c) {
        if (c == null) return null;
        return switch (c) {
            case GUERREIRO, BERSERKER, ASSASSINO, SENHOR_DA_GUERRA, LAMINA_FANTASMA, SOBERANO_DAS_SOMBRAS -> Postura.OFENSIVA;
            case CAVALEIRO, PALADINO, SANTO_PALADINO, FERREIRO, MESTRE_FORJADOR, HERDEIRO_DO_FERREIRO -> Postura.DEFENSIVA;
            default -> Postura.AGIL;
        };
    }

    static Postura afinidade(Deus d) {
        if (d == null) return null;
        return switch (d) {
            case BELLUM, MORTIS -> Postura.OFENSIVA;
            case FERRUM, SYLVA -> Postura.DEFENSIVA;
            case MARIS, ARCANUS -> Postura.AGIL;
        };
    }

    private static String combinaCom(Postura ps) {
        return switch (ps) {
            case OFENSIVA -> "guerreiros, berserkers e assassinos · Bellum e Mortis";
            case DEFENSIVA -> "cavaleiros, paladinos e ferreiros · Ferrum e Sylva";
            case AGIL -> "arqueiros, ladinos, magos e domadores · Maris e Arcanus";
            default -> null;
        };
    }

    /** A classe do jogador combina com essa postura: +8% de dano e metade das penalidades. */
    private boolean afimClasse(Player p, Postura ps) {
        return ps != Postura.NEUTRA && ps != Postura.MESTRA && afinidade(plugin.classes().ativa(p)) == ps;
    }

    /** O deus do jogador combina com essa postura: +1% de dano por nível de fé. */
    private int afimDeus(Player p, Postura ps) {
        if (ps == Postura.NEUTRA || ps == Postura.MESTRA) return 0;
        return afinidade(plugin.deuses().deus(p)) == ps ? plugin.deuses().nivel(p) : 0;
    }

    /** Multiplicador do dano que o jogador causa nesse alvo. */
    double multDano(Player p, LivingEntity alvo) {
        Postura ps = ativa(p);
        if (ps == null) return 1;
        if (ps != Postura.MESTRA) {
            boolean classe = afimClasse(p, ps);
            double m = ps.dano();
            if (classe && m < 1) m = 1 - (1 - m) / 2;
            return m * (1 + (classe ? 0.08 : 0) + 0.01 * afimDeus(p, ps));
        }
        return switch (arma(p)) {
            case ESPADA -> 1.15;
            case MACHADO -> {
                var max = alvo.getAttribute(Attribute.MAX_HEALTH);
                yield max != null && alvo.getHealth() < max.getValue() * 0.35 ? 1.40 : 1;
            }
            case LANCA -> 1.10;
            case TRIDENTE -> naChuvaOuAgua(p) ? 1.30 : 1;
            case MACA -> 1.10;
            case ARCO -> System.currentTimeMillis() - ultimoPasso.getOrDefault(p.getUniqueId(), 0L) >= 1000 ? 1.35 : 1;
            case null -> 1;
        };
    }

    /** Multiplicador do dano que o jogador recebe desse atacante. */
    double multRecebido(Player p, Entity atacante) {
        Postura ps = ativa(p);
        if (ps == null) return 1;
        if (ps == Postura.MESTRA && arma(p) == TipoArma.LANCA && atacante != null) {
            Vector para = atacante.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            Vector frente = p.getLocation().getDirection().setY(0);
            if (para.lengthSquared() > 1e-4 && frente.lengthSquared() > 1e-4 && para.normalize().dot(frente.normalize()) > 0.4) return 0.75;
        }
        double r = ps.recebido();
        return r > 1 && afimClasse(p, ps) ? 1 + (r - 1) / 2 : r;
    }

    double multEquilibrio(Player p) {
        Postura ps = ativa(p);
        if (ps == null) return 1;
        return ps == Postura.MESTRA && arma(p) == TipoArma.MACA ? 2.0 : ps.equilibrio();
    }

    double multVigor(Player p) {
        Postura ps = ativa(p);
        return ps == null ? 1 : ps.vigor();
    }

    double multRecarga(Player p) {
        Postura ps = ativa(p);
        return ps == null ? 1 : ps.recarga();
    }

    /** Esquiva da postura Ágil custa metade do vigor. */
    public double multEsquiva(Player p) {
        return ativa(p) == Postura.AGIL ? 0.5 : 1;
    }

    /** Janela (ms depois do 2º clique) do golpe no tempo certo. */
    long[] janelaPerfeita(Player p) {
        return ativa(p) == Postura.MESTRA && arma(p) == TipoArma.ESPADA ? new long[]{200, 750} : new long[]{280, 600};
    }

    boolean curaNoGolpe(Player p) {
        return ativa(p) == Postura.DEFENSIVA;
    }

    private double velocidade(Player p) {
        Postura ps = ativa(p);
        if (ps == null) return 0;
        if (ps != Postura.MESTRA) return ps.velocidade();
        TipoArma t = arma(p);
        if (t == TipoArma.TRIDENTE) return naChuvaOuAgua(p) ? 0.20 : 0;
        return t == TipoArma.MACA ? -0.05 : 0;
    }

    private double empurrao(Player p) {
        Postura ps = ativa(p);
        if (ps == Postura.DEFENSIVA) return 0.4;
        return ps == Postura.MESTRA && arma(p) == TipoArma.MACA ? 0.3 : 0;
    }

    // =====================================================================
    //  Efeitos
    // =====================================================================

    /** Velocidade e resistência a empurrão (só muda o atributo quando o valor muda). */
    private void aplicar(Player p) {
        double vel = velocidade(p), emp = empurrao(p);
        double[] antes = aplicado.get(p.getUniqueId());
        if (antes != null && antes[0] == vel && antes[1] == emp) return;
        aplicado.put(p.getUniqueId(), new double[]{vel, emp});
        modificador(p, Attribute.MOVEMENT_SPEED, K_VEL, vel, AttributeModifier.Operation.ADD_SCALAR);
        modificador(p, Attribute.KNOCKBACK_RESISTANCE, K_EMPURRAO, emp, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void modificador(Player p, Attribute a, NamespacedKey k, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(k);
        if (valor != 0) inst.addTransientModifier(new AttributeModifier(k, valor, op, EquipmentSlotGroup.ANY));
    }

    /** A cada 10 ticks: atributos; a cada 20, a aura (só com arma na mão e postura que não seja a Neutra). */
    void tick() {
        ciclo++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            aplicar(p);
            if (ciclo % 2 != 0 || p.getGameMode() == GameMode.SPECTATOR || p.isInvisible()) continue;
            Postura ps = ativa(p);
            if (ps == null || ps == Postura.NEUTRA) continue;
            Location pe = p.getLocation().add(0, 0.1, 0);
            Particle.DustOptions cor = new Particle.DustOptions(ps.corParticula(), 0.9f);
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + ciclo * 0.3;
                p.getWorld().spawnParticle(Particle.DUST, pe.getX() + Math.cos(a) * 0.55, pe.getY(), pe.getZ() + Math.sin(a) * 0.55, 1, 0, 0, 0, 0, cor);
            }
        }
    }

    /** A pegada da arma na mão (pelo pacote): a postura vai na 2ª marca do item. */
    void pegada(Player p) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        TipoArma t = TipoArma.de(mao);
        if (t != TipoArma.ESPADA && t != TipoArma.MACHADO && t != TipoArma.MACA) return;
        Postura ps = ativa(p);
        String id = ps == null || ps == Postura.NEUTRA ? null : ps == Postura.MESTRA ? Postura.pegadaMestra(t) : ps.id();
        if (PacoteRecursos.postura(mao).equals(id == null ? "" : id)) return;
        mao.editMeta(m -> PacoteRecursos.marcarPostura(m, id));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTrocarItem(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack antigo = p.getInventory().getItem(e.getPreviousSlot());
        if (antigo != null && !PacoteRecursos.postura(antigo).isEmpty()) antigo.editMeta(m -> PacoteRecursos.marcarPostura(m, null));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            aplicar(p);
            pegada(p);
        });
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (e.getPlayer().isOnline()) pegada(e.getPlayer());
        }, 20L);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        aplicado.remove(e.getPlayer().getUniqueId());
        ultimoPasso.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAndar(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()
                || e.getFrom().distanceSquared(e.getTo()) > 0.01) {
            ultimoPasso.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) Equilibrio.aoCarregar(x);
    }

    // =====================================================================
    //  Dano
    // =====================================================================

    private static Player atacante(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) return p;
        if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Player p = atacante(e);
        if (p != null && e.getEntity() instanceof LivingEntity alvo && alvo != p) {
            double m = multDano(p, alvo);
            if (ativa(p) != null) m *= 1 + combos.estilo().rank(p).dano();
            if (combos.equilibrio().quebrado(alvo)) m *= Equilibrio.DANO_QUEBRADO;
            if (m != 1) e.setDamage(e.getDamage() * m);
        }
        if (e.getEntity() instanceof Player v) {
            Entity fonte = e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : e.getDamager();
            double m = multRecebido(v, fonte);
            if (m != 1) e.setDamage(e.getDamage() * m);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getFinalDamage() > 0) combos.estilo().apanhou(p, e.getFinalDamage());
    }

    /** Número do dano subindo de quem levou o golpe de combo. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerirNumero(EntityDamageByEntityEvent e) {
        if (!combos.golpes().ferindo || !(e.getEntity() instanceof LivingEntity alvo) || e.getFinalDamage() <= 0) return;
        boolean forte = combos.equilibrio().quebrado(alvo) || combos.golpes().perfeitoAgora;
        Location l = alvo.getLocation().add((Math.random() - 0.5) * 0.6, alvo.getHeight() + 0.2, (Math.random() - 0.5) * 0.6);
        String txt = (forte ? "✦ " : "") + String.format("%.1f", e.getFinalDamage());
        TextDisplay d = alvo.getWorld().spawn(l, TextDisplay.class, x -> {
            x.text(Component.text(txt, forte ? NamedTextColor.GOLD : NamedTextColor.YELLOW, TextDecoration.BOLD));
            x.setBillboard(Display.Billboard.CENTER);
            x.setPersistent(false);
            x.setShadowed(true);
            x.setSeeThrough(true);
            x.setTeleportDuration(3);
            x.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        });
        int[] passo = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, tarefa -> {
            if (!d.isValid() || ++passo[0] > 6) {
                d.remove();
                tarefa.cancel();
                return;
            }
            d.teleport(d.getLocation().add(0, 0.18, 0));
        }, 3L, 3L);
    }

    // =====================================================================
    //  /postura
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length > 0) {
            Postura ps = Postura.porId(args[0]);
            if (ps != null) {
                escolher(p, ps);
                return true;
            }
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 27, Component.text("⚔ Postura de combate"));
        Postura atual = escolhida(p);
        TipoArma arma = arma(p);
        Postura[] ps = Postura.values();
        for (int i = 0; i < ps.length; i++) {
            Postura x = ps[i];
            List<Component> lore = new ArrayList<>();
            if (x == Postura.MESTRA) {
                lore.add(Component.text("Cada arma tem a sua, liberada na", NamedTextColor.GRAY));
                lore.add(Component.text("proficiência " + Combos.NIVEL_MAXIMO + " dela:", NamedTextColor.GRAY));
                for (TipoArma ta : TipoArma.values()) {
                    boolean livre = combos.nivel(p, ta) >= Combos.NIVEL_MAXIMO;
                    lore.add(Component.text((livre ? "✔ " : "✖ ") + ta.nome() + " → " + Postura.nomeMestra(ta), livre ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
                }
                if (arma != null) {
                    lore.add(Component.empty());
                    lore.add(Component.text(Postura.nomeMestra(arma) + ":", Postura.MESTRA.cor()));
                    for (String l : quebrar(Postura.descricaoMestra(arma))) lore.add(Component.text(l, NamedTextColor.WHITE));
                }
            } else {
                for (String l : quebrar(x.descricao())) lore.add(Component.text(l, NamedTextColor.GRAY));
                String combina = combinaCom(x);
                if (combina != null) {
                    lore.add(Component.empty());
                    lore.add(Component.text("Combina com:", NamedTextColor.DARK_AQUA));
                    for (String l : quebrar(combina)) lore.add(Component.text(l, NamedTextColor.DARK_AQUA));
                    if (afimClasse(p, x)) lore.add(Component.text("★ Sua classe: +8% de dano, metade", NamedTextColor.AQUA));
                    if (afimClasse(p, x)) lore.add(Component.text("  das penalidades", NamedTextColor.AQUA));
                    int fe = afimDeus(p, x);
                    if (fe > 0) lore.add(Component.text("✝ Seu deus: +" + fe + "% de dano (fé " + fe + ")", NamedTextColor.GOLD));
                }
            }
            lore.add(Component.empty());
            lore.add(x == atual ? Component.text("● Em uso", NamedTextColor.GREEN) : Component.text("» Clique para usar", NamedTextColor.YELLOW));
            ItemStack it = new ItemStack(x.icone());
            it.editMeta(m -> {
                m.itemName(Component.text(x.simbolo() + " " + x.nome(), x.cor(), TextDecoration.BOLD));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                m.addItemFlags(ItemFlag.values());
                if (x == atual) m.setEnchantmentGlintOverride(true);
            });
            t.inventario.setItem(S_POSTURAS[i], it);
        }
        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(m -> {
            m.itemName(Component.text("Como funciona", NamedTextColor.YELLOW, TextDecoration.BOLD));
            m.lore(List.of(
                    Component.text("A postura vale enquanto você segura", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("uma arma de combo. Espada, machado e", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("maça mudam o jeito de segurar (pacote).", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Também: /postura <nome>", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        });
        t.inventario.setItem(22, info);
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < 27; i++) if (t.inventario.getItem(i) == null) t.inventario.setItem(i, vidro);
        p.openInventory(t.inventario);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= 27) return;
        for (int i = 0; i < S_POSTURAS.length; i++) {
            if (S_POSTURAS[i] != e.getRawSlot()) continue;
            escolher(p, Postura.values()[i]);
            abrir(p);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static List<String> quebrar(String texto) {
        List<String> l = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String w : texto.split(" ")) {
            if (atual.length() > 0 && atual.length() + 1 + w.length() > 36) {
                l.add(atual.toString());
                atual.setLength(0);
            }
            if (atual.length() > 0) atual.append(' ');
            atual.append(w);
        }
        if (atual.length() > 0) l.add(atual.toString());
        return l;
    }
}
