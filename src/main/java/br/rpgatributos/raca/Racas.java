package br.rpgatributos.raca;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Raças (2.52): cada jogador escolhe uma raça no {@code /raca}. A raça muda o tamanho (atributo
 * de escala, que todos veem sem mod), dá uma fraqueza e cinco estágios de Despertar. A Essência
 * vem do XP dos atributos (cheia nos da raça) e cada estágio pede uma oferenda.
 *
 * <p>Tudo que é número fica aqui: os atributos são modificadores com chave própria (tirados e
 * postos de novo a cada mudança), e os efeitos de lugar (visão nas cavernas, guelras...) são
 * conferidos uma vez por segundo. As asas do Povo-Pássaro ficam em {@link Asas}.
 */
public final class Racas implements Listener, CommandExecutor, TabCompleter {

    private static final TextColor COR = TextColor.color(0xFFD180);
    private static final int CUSTO_TROCA = 30;
    private static final double VIGOR_BATIDA = 12, VIGOR_BATIDA_IV = 8, VIGOR_VOO = 0.35;

    private final RPGAtributos plugin;
    private final NamespacedKey kRaca, kEssencia, kEstagio, kHeranca, kAvisado;
    private final Asas asas;

    /** As chaves dos modificadores de atributo da raça. */
    private static final NamespacedKey K_ESCALA = chave("raca_escala"), K_VIDA = chave("raca_vida"), K_VEL = chave("raca_velocidade"),
            K_MINERAR = chave("raca_minerar"), K_ALCANCE = chave("raca_alcance"), K_ALCANCE_E = chave("raca_alcance_entidade"),
            K_EMPURRAO = chave("raca_empurrao"), K_SORTE = chave("raca_sorte"), K_QUEDA = chave("raca_queda"), K_GRAVIDADE = chave("raca_gravidade"),
            K_PULO = chave("raca_pulo"), K_AR = chave("raca_ar"), K_AGUA = chave("raca_agua"), K_SUBMERSO = chave("raca_submerso"),
            K_AGACHADO = chave("raca_agachado");
    private static final NamespacedKey[] CHAVES = {K_ESCALA, K_VIDA, K_VEL, K_MINERAR, K_ALCANCE, K_ALCANCE_E, K_EMPURRAO, K_SORTE, K_QUEDA,
            K_GRAVIDADE, K_PULO, K_AR, K_AGUA, K_SUBMERSO, K_AGACHADO};
    private static final Attribute[] ATRIBUTOS = {Attribute.SCALE, Attribute.MAX_HEALTH, Attribute.MOVEMENT_SPEED, Attribute.BLOCK_BREAK_SPEED,
            Attribute.BLOCK_INTERACTION_RANGE, Attribute.ENTITY_INTERACTION_RANGE, Attribute.KNOCKBACK_RESISTANCE, Attribute.LUCK,
            Attribute.SAFE_FALL_DISTANCE, Attribute.GRAVITY, Attribute.JUMP_STRENGTH, Attribute.OXYGEN_BONUS, Attribute.WATER_MOVEMENT_EFFICIENCY,
            Attribute.SUBMERGED_MINING_SPEED, Attribute.SNEAKING_SPEED};

    /** Efeitos de lugar que a raça pôs (para tirar só os nossos quando a condição acaba). */
    private final Map<UUID, Set<PotionEffectType>> efeitosDados = new HashMap<>();
    private final Map<UUID, Long> agachouEm = new HashMap<>(), sentidosAte = new HashMap<>(), bateuEm = new HashMap<>();
    private int ciclo;

    public Racas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kRaca = new NamespacedKey(plugin, "raca");
        this.kEssencia = new NamespacedKey(plugin, "raca_essencia");
        this.kEstagio = new NamespacedKey(plugin, "raca_estagio");
        this.kHeranca = new NamespacedKey(plugin, "raca_heranca");
        this.kAvisado = new NamespacedKey(plugin, "raca_avisado");
        this.asas = new Asas(plugin, this);
    }

    private static NamespacedKey chave(String s) {
        return new NamespacedKey("rpgatributos", s);
    }

    public Asas asas() { return asas; }

    // =====================================================================
    //  Dados do jogador
    // =====================================================================

    public Raca de(Player p) {
        return Raca.porId(p.getPersistentDataContainer().get(kRaca, PersistentDataType.STRING));
    }

    public int estagio(Player p) {
        if (de(p) == null) return 0;
        return Math.max(1, Math.min(5, p.getPersistentDataContainer().getOrDefault(kEstagio, PersistentDataType.INTEGER, 1)));
    }

    public double essencia(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kEssencia, PersistentDataType.DOUBLE, 0.0);
    }

    /** A herança do humano no estágio V (o dom de outra raça), ou null. */
    public Raca heranca(Player p) {
        Raca h = Raca.porId(p.getPersistentDataContainer().get(kHeranca, PersistentDataType.STRING));
        return h == Raca.HUMANO ? null : h;
    }

    /** Raça {@code r} no estágio {@code e} ou acima? */
    private boolean eh(Player p, Raca r, int e) {
        return de(p) == r && estagio(p) >= e;
    }

    /** Muda a raça (zera a Essência e volta ao estágio I). Null tira a raça. */
    public void definir(Player p, Raca r, int estagio) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (r == null) {
            pdc.remove(kRaca);
            pdc.remove(kEstagio);
            pdc.remove(kEssencia);
        } else {
            pdc.set(kRaca, PersistentDataType.STRING, r.id());
            int e = Math.max(1, Math.min(5, estagio));
            pdc.set(kEstagio, PersistentDataType.INTEGER, e);
            pdc.set(kEssencia, PersistentDataType.DOUBLE, Raca.ESSENCIA[e - 1]);
        }
        pdc.remove(kHeranca);
        tirarEfeitos(p);
        asas.fechar(p.getUniqueId());
        aplicar(p);
    }

    // =====================================================================
    //  Atributos
    // =====================================================================

    private record Mod(Attribute a, NamespacedKey k, double v, AttributeModifier.Operation op) { }

    private static final AttributeModifier.Operation SOMA = AttributeModifier.Operation.ADD_NUMBER;
    private static final AttributeModifier.Operation ESCALA = AttributeModifier.Operation.ADD_SCALAR;
    private static final AttributeModifier.Operation MULT = AttributeModifier.Operation.MULTIPLY_SCALAR_1;

    /** Os modificadores da raça e do estágio. */
    private List<Mod> mods(Player p) {
        List<Mod> l = new ArrayList<>();
        Raca r = de(p);
        if (r == null) return l;
        int e = estagio(p);
        if (r.escala() != 1.0) l.add(new Mod(Attribute.SCALE, K_ESCALA, r.escala() - 1.0, ESCALA));
        switch (r) {
            case HUMANO -> {
                if (e >= 4) l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, 4, SOMA));
                Raca h = e >= 5 ? heranca(p) : null;
                if (h != null) switch (h) {
                    case ANAO -> l.add(new Mod(Attribute.BLOCK_BREAK_SPEED, K_MINERAR, 0.2, MULT));
                    case ELFO -> l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, 0.08, MULT));
                    case PEQUENINO -> l.add(new Mod(Attribute.LUCK, K_SORTE, 2, SOMA));
                    case PASSARO -> l.add(new Mod(Attribute.SAFE_FALL_DISTANCE, K_QUEDA, 6, SOMA));
                    case MAR -> {
                        l.add(new Mod(Attribute.OXYGEN_BONUS, K_AR, 3, SOMA));
                        l.add(new Mod(Attribute.WATER_MOVEMENT_EFFICIENCY, K_AGUA, 0.33, SOMA));
                    }
                    case FERA -> l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, 0.04, MULT));
                    case ESPECTRAL -> l.add(new Mod(Attribute.SAFE_FALL_DISTANCE, K_QUEDA, 6, SOMA));
                    case GNOMO -> l.add(new Mod(Attribute.BLOCK_INTERACTION_RANGE, K_ALCANCE, 1.0, SOMA));
                    default -> { } // a herança draconato (fogo pela metade) é tratada no dano
                }
            }
            case DRACONATO -> {
                if (e >= 4) l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, 4, SOMA));
            }
            case FERA -> {
                l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, 0.04, MULT));
                l.add(new Mod(Attribute.ENTITY_INTERACTION_RANGE, K_ALCANCE_E, 0.2, SOMA));
            }
            case ESPECTRAL -> {
                l.add(new Mod(Attribute.SAFE_FALL_DISTANCE, K_QUEDA, 6, SOMA));
                l.add(new Mod(Attribute.GRAVITY, K_GRAVIDADE, -0.15, MULT));
            }
            case GNOMO -> {
                l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, -4, SOMA));
                // Menor, mas o braço alcança o mesmo de antes.
                l.add(new Mod(Attribute.BLOCK_INTERACTION_RANGE, K_ALCANCE, 1.0, SOMA));
                l.add(new Mod(Attribute.ENTITY_INTERACTION_RANGE, K_ALCANCE_E, 0.5, SOMA));
            }
            case ANAO -> {
                l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, e >= 4 ? 8 : 4, SOMA));
                l.add(new Mod(Attribute.BLOCK_BREAK_SPEED, K_MINERAR, e >= 3 ? 0.25 : 0.10, MULT));
                l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, -0.08, MULT));
                // Menor, mas o braço alcança o mesmo de antes.
                l.add(new Mod(Attribute.BLOCK_INTERACTION_RANGE, K_ALCANCE, 0.5, SOMA));
                l.add(new Mod(Attribute.ENTITY_INTERACTION_RANGE, K_ALCANCE_E, 0.3, SOMA));
                if (e >= 3) l.add(new Mod(Attribute.KNOCKBACK_RESISTANCE, K_EMPURRAO, 0.3, SOMA));
            }
            case ELFO -> {
                l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, -4, SOMA));
                l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, e >= 4 ? 0.12 : 0.06, MULT));
                if (e >= 4) {
                    l.add(new Mod(Attribute.JUMP_STRENGTH, K_PULO, 0.08, SOMA));
                    l.add(new Mod(Attribute.SAFE_FALL_DISTANCE, K_QUEDA, 3, SOMA));
                }
            }
            case PEQUENINO -> {
                l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, -4, SOMA));
                l.add(new Mod(Attribute.LUCK, K_SORTE, e >= 4 ? 3 : 1, SOMA));
                l.add(new Mod(Attribute.SNEAKING_SPEED, K_AGACHADO, 0.2, SOMA));
                l.add(new Mod(Attribute.BLOCK_INTERACTION_RANGE, K_ALCANCE, 1.0, SOMA));
                l.add(new Mod(Attribute.ENTITY_INTERACTION_RANGE, K_ALCANCE_E, 0.5, SOMA));
                if (e >= 4) l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, 0.10, MULT));
            }
            case PASSARO -> {
                l.add(new Mod(Attribute.MAX_HEALTH, K_VIDA, -4, SOMA));
                l.add(new Mod(Attribute.SAFE_FALL_DISTANCE, K_QUEDA, 8, SOMA));
                l.add(new Mod(Attribute.GRAVITY, K_GRAVIDADE, -0.1, MULT));
                if (e >= 4) l.add(new Mod(Attribute.MOVEMENT_SPEED, K_VEL, 0.10, MULT));
            }
            case MAR -> {
                l.add(new Mod(Attribute.OXYGEN_BONUS, K_AR, 3, SOMA));
                l.add(new Mod(Attribute.WATER_MOVEMENT_EFFICIENCY, K_AGUA, 0.33, SOMA));
                if (e >= 3) l.add(new Mod(Attribute.SUBMERGED_MINING_SPEED, K_SUBMERSO, 0.8, SOMA));
            }
        }
        return l;
    }

    /** Tira todos os modificadores da raça e põe os de agora. */
    public void aplicar(Player p) {
        for (Attribute a : ATRIBUTOS) {
            AttributeInstance i = p.getAttribute(a);
            if (i == null) continue;
            for (NamespacedKey k : CHAVES) i.removeModifier(k);
        }
        for (Mod m : mods(p)) {
            AttributeInstance i = p.getAttribute(m.a());
            if (i != null) i.addModifier(new AttributeModifier(m.k(), m.v(), m.op(), EquipmentSlotGroup.ANY));
        }
        AttributeInstance vida = p.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null && p.getHealth() > vida.getValue()) p.setHealth(vida.getValue());
    }

    // =====================================================================
    //  Ganchos de outros sistemas
    // =====================================================================

    /** Multiplicador de XP (humanos). */
    public double multiplicadorXp(Player p) {
        if (de(p) != Raca.HUMANO) return 1;
        int e = estagio(p);
        return e >= 3 ? 1.20 : 1.10;
    }

    /** Vigor máximo a mais (humano no estágio II). */
    public double bonusVigor(Player p) {
        return eh(p, Raca.HUMANO, 2) ? 10 : 0;
    }

    /** O Povo-Pássaro plana sem a Capa Planadora a partir do estágio II; o Draconato, com as Asas Curtas (III). */
    public boolean plana(Player p) {
        return eh(p, Raca.PASSARO, 2) || eh(p, Raca.DRACONATO, 3);
    }

    /** Gnomo no estágio V: chance a mais de a Forja subir uma raridade. */
    public double chanceRaridadeExtra(Player p) {
        return eh(p, Raca.GNOMO, 5) ? 0.10 : 0;
    }

    /** A classe ativa combina com a raça? (+6% de dano e -5% de dano recebido) */
    public boolean sinergia(Player p) {
        Raca r = de(p);
        if (r == null) return false;
        br.rpgatributos.classe.Classe c = plugin.classes().ativa(p);
        return c != null && r.sinergia().contains(c);
    }

    /** Velocidade do planar (o Povo-Pássaro no estágio IV vai mais rápido). */
    public double velocidadePlanar(Player p, double base) {
        return eh(p, Raca.PASSARO, 4) ? base * 1.25 : base;
    }

    /** Armadura pesada no peito prende as asas. */
    public static boolean pesada(Player p) {
        ItemStack c = p.getInventory().getChestplate();
        if (c == null) return false;
        String n = c.getType().name();
        return n.equals("IRON_CHESTPLATE") || n.equals("GOLDEN_CHESTPLATE") || n.equals("DIAMOND_CHESTPLATE") || n.equals("NETHERITE_CHESTPLATE");
    }

    /**
     * Pulou enquanto plana: o Povo-Pássaro no estágio III bate as asas (sobe e vai para frente,
     * gasta Vigor). @return true se a raça cuidou do pulo (o planar continua).
     */
    public boolean baterAsas(Player p) {
        if (!eh(p, Raca.PASSARO, 3)) return false;
        if (pesada(p)) {
            p.sendActionBar(Component.text("🪶 A armadura pesada prende as asas", NamedTextColor.RED));
            return false;
        }
        long agora = System.currentTimeMillis();
        if (agora - bateuEm.getOrDefault(p.getUniqueId(), 0L) < 450) return true;
        double custo = estagio(p) >= 4 ? VIGOR_BATIDA_IV : VIGOR_BATIDA;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && plugin.combos().vigor(p) < custo) {
            p.sendActionBar(Component.text("🪶 Sem Vigor para bater as asas", NamedTextColor.RED));
            return true;
        }
        if (!criativo) plugin.combos().darVigor(p, -custo);
        bateuEm.put(p.getUniqueId(), agora);
        Vector d = p.getLocation().getDirection().setY(0);
        if (d.lengthSquared() > 1e-4) d.normalize().multiply(0.35);
        p.setVelocity(d.setY(estagio(p) >= 4 ? 0.75 : 0.6));
        p.setFallDistance(0);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.7f);
        asas.batida(p);
        return true;
    }

    public boolean bateuHaPouco(Player p, long ms) {
        return System.currentTimeMillis() - bateuEm.getOrDefault(p.getUniqueId(), 0L) < ms;
    }

    /** Chamado pelo {@code StatsManager}: o XP dos atributos vira Essência. */
    public void aoGanharXp(Player p, Skill s, double xp) {
        Raca r = de(p);
        if (r == null || xp <= 0) return;
        darEssencia(p, xp * r.fatorEssencia(s));
    }

    public void darEssencia(Player p, double qtd) {
        Raca r = de(p);
        if (r == null || qtd <= 0) return;
        int e = estagio(p);
        double antes = essencia(p), depois = antes + qtd;
        p.getPersistentDataContainer().set(kEssencia, PersistentDataType.DOUBLE, depois);
        if (e < 5 && antes < Raca.ESSENCIA[e] && depois >= Raca.ESSENCIA[e]) {
            p.sendMessage(Component.text(r.icone() + " Sua Essência de " + r.nome() + " está pronta: você pode despertar o estágio "
                            + Raca.ROMANO[e] + " (" + r.estagio(e + 1) + ").", r.cor())
                    .append(Component.text(" [abrir]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/raca"))));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
        }
    }

    // =====================================================================
    //  Despertar
    // =====================================================================

    /** O que falta para o próximo estágio (vazio = pode despertar). */
    public List<String> falta(Player p) {
        List<String> l = new ArrayList<>();
        Raca r = de(p);
        int e = estagio(p);
        if (r == null || e >= 5) return List.of("Estágio máximo");
        double precisa = Raca.ESSENCIA[e];
        if (essencia(p) < precisa) l.add("Essência: " + (int) essencia(p) + "/" + (int) precisa);
        Material m = r.oferenda(e + 1);
        int q = r.quantidade(e + 1);
        if (m != null && p.getGameMode() != GameMode.CREATIVE && !p.getInventory().containsAtLeast(new ItemStack(m), q)) {
            l.add("Oferenda: " + q + "× " + nomeItem(m));
        }
        return l;
    }

    public boolean despertar(Player p) {
        Raca r = de(p);
        int e = estagio(p);
        if (r == null || e >= 5 || !falta(p).isEmpty()) return false;
        Material m = r.oferenda(e + 1);
        if (m != null && p.getGameMode() != GameMode.CREATIVE) p.getInventory().removeItem(new ItemStack(m, r.quantidade(e + 1)));
        p.getPersistentDataContainer().set(kEstagio, PersistentDataType.INTEGER, e + 1);
        aplicar(p);
        String est = Raca.ROMANO[e] + " · " + r.estagio(e + 1);
        p.showTitle(Title.title(Component.text(r.icone() + " Despertar " + Raca.ROMANO[e], r.cor(), TextDecoration.BOLD),
                Component.text(r.estagio(e + 1) + ": " + r.efeito(e + 1), NamedTextColor.WHITE)));
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        plugin.diario().marco(p, "raca_" + r.id() + "_" + (e + 1), "Despertou o estágio " + est + " de " + r.nome());
        if (e + 1 == 5) plugin.titulos().registrar(p, "raca_v_" + r.id(), 1);
        if (r == Raca.HUMANO && e + 1 == 5) p.sendMessage(Component.text("☺ Escolha sua Herança no /raca: o dom de outra raça.", r.cor()));
        return true;
    }

    // =====================================================================
    //  Efeitos de lugar (uma vez por segundo) e asas (a cada tick)
    // =====================================================================

    /** A cada tick: as asas e o voo do Povo-Pássaro. */
    public void tick() {
        ciclo++;
        asas.tick(ciclo);
        if (ciclo % 20 == 0) segundo();
    }

    /** Voo do estágio V: segurando o pulo enquanto plana, sobe gastando Vigor. Chamado pelas asas. */
    boolean voar(Player p) {
        if (!eh(p, Raca.PASSARO, 5) || pesada(p) || !p.getCurrentInput().isJump()) return false;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && plugin.combos().vigor(p) < VIGOR_VOO) return false;
        if (!criativo) plugin.combos().darVigor(p, -VIGOR_VOO);
        Vector v = p.getVelocity();
        if (v.getY() < 0.3) p.setVelocity(v.setY(Math.min(0.3, v.getY() + 0.12)));
        p.setFallDistance(0);
        return true;
    }

    private void segundo() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Raca r = de(p);
            if (r == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
            int e = estagio(p);
            Set<PotionEffectType> agora = new HashSet<>();
            boolean caverna = naCaverna(p);
            boolean naAgua = p.isInWater();
            switch (r) {
                case ANAO -> {
                    if (e >= 2 && caverna) agora.add(PotionEffectType.NIGHT_VISION);
                    if (e >= 5 && caverna) agora.add(PotionEffectType.HASTE);
                }
                case ELFO -> {
                    if (e >= 5 && ciclo % 60 == 0 && naFloresta(p)) curar(p, 1);
                    Long desde = agachouEm.get(p.getUniqueId());
                    if (e >= 3 && desde != null && p.isSneaking() && System.currentTimeMillis() - desde >= 2000) sentidos(p);
                }
                case MAR -> {
                    if (e >= 2 && p.isUnderWater()) agora.add(PotionEffectType.NIGHT_VISION);
                    if (e >= 3 && naAgua) agora.add(PotionEffectType.DOLPHINS_GRACE);
                    if (e >= 4 && naAgua) agora.add(PotionEffectType.CONDUIT_POWER);
                    if (!naAgua && !p.isInRain() && seco(p)) agora.add(PotionEffectType.WEAKNESS);
                }
                case DRACONATO -> {
                    if (e >= 4) agora.add(PotionEffectType.FIRE_RESISTANCE);
                    if (frio(p)) {
                        agora.add(PotionEffectType.SLOWNESS);
                        agora.add(PotionEffectType.WEAKNESS);
                    }
                }
                case FERA -> {
                    boolean noite = br.rpgatributos.mundo.Ceu.noite(p.getWorld()) && p.getWorld().getEnvironment() == World.Environment.NORMAL;
                    if (noite) agora.add(PotionEffectType.NIGHT_VISION);
                    if (e >= 4 && noite) {
                        agora.add(PotionEffectType.SPEED);
                        if (ciclo % 100 == 0) curar(p, 1);
                    }
                    if (e >= 5 && noite) agora.add(PotionEffectType.STRENGTH);
                }
                case ESPECTRAL -> {
                    boolean noite = br.rpgatributos.mundo.Ceu.noite(p.getWorld()) || p.getWorld().getEnvironment() != World.Environment.NORMAL;
                    if (noite) agora.add(PotionEffectType.NIGHT_VISION);
                    else if (!p.getWorld().hasStorm() && p.getEyeLocation().getBlock().getLightFromSky() >= 15) agora.add(PotionEffectType.WEAKNESS);
                }
                case GNOMO -> {
                    if (ciclo % 600 == 0) remendar(p);
                }
                default -> { }
            }
            efeitos(p, agora);
            // Senhor da Lua: na lua cheia a Força vira Força II.
            if (r == Raca.FERA && e >= 5 && plugin.ceu().luaCheia() && agora.contains(PotionEffectType.STRENGTH)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 1, true, false, true));
            }
        }
    }

    /** Debaixo da terra: sem luz do céu e abaixo do y 60 (ou em qualquer canto do Nether). */
    private static boolean naCaverna(Player p) {
        if (p.getWorld().getEnvironment() == World.Environment.NETHER) return true;
        Block b = p.getEyeLocation().getBlock();
        return p.getLocation().getY() < 60 && b.getLightFromSky() == 0;
    }

    private static boolean naFloresta(Player p) {
        String b = p.getLocation().getBlock().getBiome().getKey().getKey();
        return b.contains("forest") || b.contains("taiga") || b.contains("jungle") || b.contains("grove") || b.contains("cherry");
    }

    private static boolean seco(Player p) {
        if (p.getWorld().getEnvironment() == World.Environment.NETHER) return true;
        String b = p.getLocation().getBlock().getBiome().getKey().getKey();
        return b.contains("desert") || b.contains("badlands");
    }

    /** Draconato no frio: em cima de neve ou gelo, ou num bioma gelado. */
    private static boolean frio(Player p) {
        Block b = p.getLocation().getBlock();
        Material chao = b.getRelative(org.bukkit.block.BlockFace.DOWN).getType();
        return b.getTemperature() < 0.15 || b.getType() == Material.SNOW || chao == Material.SNOW_BLOCK || chao == Material.ICE
                || chao == Material.PACKED_ICE || chao == Material.BLUE_ICE || chao == Material.POWDER_SNOW;
    }

    /** Gnomo: a ferramenta ou arma da mão recupera um pouco de durabilidade. */
    private static void remendar(Player p) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.isEmpty() || mao.getType().getMaxDurability() <= 0 || !(mao.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d) || d.getDamage() <= 0) return;
        int conserto = Math.max(1, mao.getType().getMaxDurability() / 100);
        mao.editMeta(org.bukkit.inventory.meta.Damageable.class, m -> m.setDamage(Math.max(0, m.getDamage() - conserto)));
        p.spawnParticle(Particle.WAX_OFF, p.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.02);
    }

    private static void curar(Player p, double qtd) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max != null && p.getHealth() < max.getValue()) p.setHealth(Math.min(max.getValue(), p.getHealth() + qtd));
    }

    /** Põe os efeitos que valem agora e tira os nossos que não valem mais. */
    private void efeitos(Player p, Set<PotionEffectType> agora) {
        Set<PotionEffectType> antes = efeitosDados.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        for (PotionEffectType t : List.copyOf(antes)) {
            if (agora.contains(t)) continue;
            PotionEffect atual = p.getPotionEffect(t);
            if (atual != null && atual.isAmbient()) p.removePotionEffect(t);
            antes.remove(t);
        }
        for (PotionEffectType t : agora) {
            PotionEffect atual = p.getPotionEffect(t);
            if (atual != null && !atual.isAmbient()) continue; // um efeito de verdade (poção, farol...) fica
            // Visão noturna pisca abaixo de 10 s: dá 15 s e renova todo segundo.
            int dur = t == PotionEffectType.NIGHT_VISION ? 300 : 60;
            p.addPotionEffect(new PotionEffect(t, dur, 0, true, false, true));
            antes.add(t);
        }
    }

    private void tirarEfeitos(Player p) {
        efeitos(p, Set.of());
        efeitosDados.remove(p.getUniqueId());
    }

    /** Sentidos Élficos: os monstros por perto brilham por 5 s (a cada 30 s). */
    private void sentidos(Player p) {
        long agora = System.currentTimeMillis();
        if (agora < sentidosAte.getOrDefault(p.getUniqueId(), 0L)) return;
        sentidosAte.put(p.getUniqueId(), agora + 30_000);
        int n = 0;
        for (Entity en : p.getNearbyEntities(24, 12, 24)) {
            if (en instanceof Monster m) {
                m.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false, false));
                n++;
            }
        }
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
        p.sendActionBar(Component.text("❦ Sentidos Élficos: " + n + (n == 1 ? " monstro" : " monstros") + " por perto", Raca.ELFO.cor()));
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        aplicar(p);
        // Quem já estava no estágio V antes da 2.53 ganha o título da raça.
        if (de(p) != null && estagio(p) >= 5) plugin.titulos().definirMinimo(p, "raca_v_" + de(p).id(), 1);
        if (de(p) != null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || de(p) != null) return;
            p.sendMessage(Component.text("✦ Você ainda não tem raça! ", COR, TextDecoration.BOLD)
                    .append(Component.text("Anão, Elfo, Pequenino, Povo-Pássaro, Povo do Mar ou Humano: cada um cresce em 5 estágios.", NamedTextColor.WHITE))
                    .append(Component.text(" [escolher]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/raca"))));
        }, 120L);
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) aplicar(p); });
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        efeitosDados.remove(id);
        agachouEm.remove(id);
        sentidosAte.remove(id);
        bateuEm.remove(id);
        ultimoAgachar.remove(id);
        asas.fechar(id);
    }

    @EventHandler
    public void aoAgachar(PlayerToggleSneakEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (e.isSneaking()) {
            long agora = System.currentTimeMillis();
            Long antes = agachouEm.put(id, agora);
            // Agachar duas vezes rápido (2.53): a habilidade da raça.
            Long ultimo = ultimoAgachar.put(id, agora);
            if (ultimo != null && agora - ultimo < 350 && (antes == null || agora - antes < 600)) {
                ultimoAgachar.remove(id);
                habilidade(e.getPlayer());
            }
        } else {
            agachouEm.remove(id);
        }
    }

    // =====================================================================
    //  2.53: as habilidades e os dons da segunda onda
    // =====================================================================

    private final Map<UUID, Long> ultimoAgachar = new HashMap<>(), recarga = new HashMap<>();
    private final NamespacedKey kVeu = new NamespacedKey("rpgatributos", "raca_veu");

    /** Recarga da habilidade de raça: false (e avisa) se ainda não deu o tempo. */
    private boolean pronta(Player p, long ms) {
        long agora = System.currentTimeMillis();
        long ate = recarga.getOrDefault(p.getUniqueId(), 0L);
        if (agora < ate) {
            p.sendActionBar(Component.text("Habilidade da raça em " + (ate - agora + 999) / 1000 + " s", NamedTextColor.GRAY));
            return false;
        }
        recarga.put(p.getUniqueId(), agora + ms);
        return true;
    }

    private void habilidade(Player p) {
        Raca r = de(p);
        if (r == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) return;
        int e = estagio(p);
        switch (r) {
            case DRACONATO -> { if (e >= 2) sopro(p, e); }
            case FERA -> { if (e >= 3) uivo(p); }
            case ESPECTRAL -> { if (e >= 3) desvanecer(p); }
            case GNOMO -> { if (e >= 4) automato(p); }
            default -> { }
        }
    }

    /** Draconato: sopro de fogo em cone (5 blocos; 8 no estágio V). */
    private void sopro(Player p, int e) {
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && plugin.combos().vigor(p) < 20) {
            p.sendActionBar(Component.text("✹ Sem Vigor para o sopro", NamedTextColor.RED));
            return;
        }
        if (!pronta(p, 8000)) return;
        if (!criativo) plugin.combos().darVigor(p, -20);
        double alcance = e >= 5 ? 8 : 5, dano = e >= 5 ? 6 : 4;
        org.bukkit.Location boca = p.getEyeLocation().subtract(0, 0.2, 0);
        p.getWorld().playSound(boca, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.7f);
        p.getWorld().playSound(boca, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.35f, 1.6f);
        Set<UUID> acertados = new HashSet<>();
        for (int t = 0; t < 6; t++) {
            int passo = t;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                org.bukkit.Location de = p.getEyeLocation().subtract(0, 0.2, 0);
                Vector dir = p.getEyeLocation().getDirection().normalize();
                for (double s = 0.6; s <= alcance; s += 0.6) {
                    double abre = s * 0.18;
                    p.getWorld().spawnParticle(Particle.FLAME, de.clone().add(dir.clone().multiply(s)), 3, abre, abre, abre, 0.02);
                }
                if (passo % 2 != 0) return;
                for (Entity en : p.getNearbyEntities(alcance, alcance, alcance)) {
                    if (!(en instanceof LivingEntity alvo) || !plugin.alvos().inimigo(p, en) || acertados.contains(en.getUniqueId())) continue;
                    Vector para = en.getLocation().add(0, en.getHeight() / 2, 0).toVector().subtract(de.toVector());
                    if (para.length() > alcance || para.normalize().dot(dir) < 0.8) continue;
                    acertados.add(en.getUniqueId());
                    plugin.alvos().ferir(p, alvo, dano);
                    alvo.setFireTicks(Math.max(alvo.getFireTicks(), 80));
                }
            }, t * 2L);
        }
    }

    /** Homem-Fera: uivo que dá Força (você e a party perto) e espanta os monstros. */
    private void uivo(Player p) {
        if (!pronta(p, 45_000)) return;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.4f, 0.6f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, true, true));
        for (Player o : p.getWorld().getPlayers()) {
            if (o == p || o.getLocation().distanceSquared(p.getLocation()) > 16 * 16) continue;
            if (plugin.parties().mesmaParty(p.getUniqueId(), o.getUniqueId())) {
                o.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, true, true));
                o.sendActionBar(Component.text("☾ O uivo de " + p.getName() + " dá Força!", Raca.FERA.cor()));
            }
        }
        for (Entity en : p.getNearbyEntities(8, 4, 8)) {
            if (!(en instanceof Monster m) || br.rpgatributos.aventura.Chefes.ehChefe(m)) continue;
            Vector longe = m.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
            if (longe.lengthSquared() < 1e-4) continue;
            m.setVelocity(longe.normalize().multiply(0.9).setY(0.3));
            if (p.equals(m.getTarget())) m.setTarget(null);
            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true, true));
        }
        p.sendActionBar(Component.text("☾ Uivo!", Raca.FERA.cor()));
    }

    /** Espectral: invisível por 8 s, e quem mirava nele perde o alvo. */
    private void desvanecer(Player p) {
        if (!pronta(p, 45_000)) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0, false, false, true));
        for (Entity en : p.getNearbyEntities(24, 12, 24)) if (en instanceof org.bukkit.entity.Mob m && p.equals(m.getTarget())) m.setTarget(null);
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.8f, 0.6f);
        p.sendActionBar(Component.text("✧ Você desvaneceu", Raca.ESPECTRAL.cor()));
    }

    /** Gnomo: um pequeno autômato de ferro luta ao seu lado por 40 s. */
    private void automato(Player p) {
        if (!pronta(p, 120_000)) return;
        org.bukkit.Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(1.5));
        org.bukkit.entity.IronGolem g = p.getWorld().spawn(l, org.bukkit.entity.IronGolem.class, x -> {
            x.setPlayerCreated(true);
            x.setPersistent(false);
            x.customName(Component.text("⚙ Autômato de " + p.getName(), Raca.GNOMO.cor()));
            x.setCustomNameVisible(true);
            AttributeInstance esc = x.getAttribute(Attribute.SCALE);
            if (esc != null) esc.setBaseValue(0.5);
            AttributeInstance vida = x.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) {
                vida.setBaseValue(40);
                x.setHealth(40);
            }
        });
        p.getWorld().playSound(l, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1.4f);
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, l.clone().add(0, 0.8, 0), 25, 0.4, 0.5, 0.4, 0.05);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!g.isValid()) return;
            g.getWorld().spawnParticle(Particle.SMOKE, g.getLocation().add(0, 0.6, 0), 15, 0.3, 0.4, 0.3, 0.02);
            g.remove();
        }, 20L * 40);
        // Mira o monstro mais perto do dono a cada segundo enquanto existir.
        for (int s = 1; s < 40; s++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!g.isValid() || !p.isOnline() || (g.getTarget() != null && g.getTarget().isValid())) return;
                Monster melhor = null;
                double d = 12 * 12;
                for (Entity en : p.getNearbyEntities(12, 6, 12)) {
                    if (en instanceof Monster m && m.getLocation().distanceSquared(p.getLocation()) < d) {
                        d = m.getLocation().distanceSquared(p.getLocation());
                        melhor = m;
                    }
                }
                if (melhor != null) g.setTarget(melhor);
                else if (g.getLocation().distanceSquared(p.getLocation()) > 36) g.getPathfinder().moveTo(p.getLocation());
            }, 20L * s);
        }
    }

    /** O autômato nunca ataca o dono nem outros jogadores. */
    @EventHandler(ignoreCancelled = true)
    public void aoMirarAutomato(EntityTargetLivingEntityEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.IronGolem g && g.customName() != null && e.getTarget() instanceof Player
                && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(g.customName()).startsWith("⚙ Autômato")) {
            e.setCancelled(true);
        }
    }

    /** Fogo e lava: Draconato leva metade (e nada no estágio IV); o Humano com a herança draconata também leva metade. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQueimar(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c != EntityDamageEvent.DamageCause.FIRE && c != EntityDamageEvent.DamageCause.FIRE_TICK
                && c != EntityDamageEvent.DamageCause.LAVA && c != EntityDamageEvent.DamageCause.HOT_FLOOR) return;
        if (eh(p, Raca.DRACONATO, 4)) {
            e.setCancelled(true);
            p.setFireTicks(0);
        } else if (eh(p, Raca.DRACONATO, 1) || (eh(p, Raca.HUMANO, 5) && heranca(p) == Raca.DRACONATO)) {
            e.setDamage(e.getDamage() * 0.5);
        }
    }

    /** Espectral: os mortos-vivos só notam ele bem de perto (estágio II). */
    @EventHandler(ignoreCancelled = true)
    public void aoMirarMorto(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p) || !eh(p, Raca.ESPECTRAL, 2)) return;
        Entity m = e.getEntity();
        boolean morto = m instanceof org.bukkit.entity.Zombie || m instanceof org.bukkit.entity.AbstractSkeleton || m instanceof org.bukkit.entity.Phantom
                || m instanceof org.bukkit.entity.Zoglin || m instanceof org.bukkit.entity.Wither;
        if (morto && m.getLocation().distanceSquared(p.getLocation()) > 36 && e.getReason() == EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER) {
            e.setCancelled(true);
        }
    }

    /** Espectral V: uma vez por hora, ao cair morto, volta com metade da vida. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoMorrerEspectral(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !eh(p, Raca.ESPECTRAL, 5) || e.getFinalDamage() < p.getHealth()) return;
        if (p.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || p.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        long agora = System.currentTimeMillis();
        long ultimo = p.getPersistentDataContainer().getOrDefault(kVeu, PersistentDataType.LONG, 0L);
        if (agora - ultimo < 3_600_000) return;
        p.getPersistentDataContainer().set(kVeu, PersistentDataType.LONG, agora);
        e.setCancelled(true);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(Math.max(1, (max == null ? 20 : max.getValue()) / 2));
        p.setFireTicks(0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false, true));
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 1.8f);
        p.showTitle(Title.title(Component.text("✧ Além do Véu", Raca.ESPECTRAL.cor(), TextDecoration.BOLD),
                Component.text("Você voltou do outro lado (de novo em 1 hora).", NamedTextColor.GRAY)));
    }

    /** Gnomo II: 15% de chance de não gastar durabilidade. */
    @EventHandler(ignoreCancelled = true)
    public void aoGastar(org.bukkit.event.player.PlayerItemDamageEvent e) {
        if (eh(e.getPlayer(), Raca.GNOMO, 2) && ThreadLocalRandom.current().nextDouble() < 0.15) e.setCancelled(true);
    }

    /** Quem soltou a explosão/foguete/frasco (para o dom do Gnomo). */
    private static Player autor(Entity d) {
        if (d instanceof org.bukkit.entity.TNTPrimed t && t.getSource() instanceof Player p) return p;
        if (d instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    /** Gnomo III: explosivos e frascos +30% e não ferem o próprio gnomo. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoExplodir(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        if (!(d instanceof org.bukkit.entity.TNTPrimed || d instanceof org.bukkit.entity.Firework || d instanceof org.bukkit.entity.ThrownPotion
                || d instanceof org.bukkit.entity.Snowball)) return;
        Player autor = autor(d);
        if (autor == null || !eh(autor, Raca.GNOMO, 3)) return;
        if (e.getEntity().equals(autor)) {
            e.setCancelled(true);
            return;
        }
        e.setDamage(e.getDamage() * 1.3);
    }

    /** Sinergia de raça e classe, garras do Homem-Fera, ouro contra ele, Toque Gélido e o fogo do Draconato V. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGolpear(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player alvo) {
            if (sinergia(alvo)) e.setDamage(e.getDamage() * 0.95);
            if (eh(alvo, Raca.FERA, 1) && e.getDamager() instanceof LivingEntity at && at.getEquipment() != null
                    && at.getEquipment().getItemInMainHand().getType().name().startsWith("GOLDEN_")) e.setDamage(e.getDamage() * 1.25);
        }
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity alvo)) return;
        if (sinergia(p)) e.setDamage(e.getDamage() * 1.06);
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (eh(p, Raca.FERA, 2) && p.getInventory().getItemInMainHand().isEmpty()) e.setDamage(e.getDamage() + 5);
        if (eh(p, Raca.ESPECTRAL, 4)) {
            alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true, true));
            curar(p, e.getDamage() * 0.08);
        }
        if (eh(p, Raca.DRACONATO, 5)) alvo.setFireTicks(Math.max(alvo.getFireTicks(), 60));
    }

    /** Anão: minério dobrado no estágio IV (8%) e V (20%). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!eh(p, Raca.ANAO, 4) || p.getGameMode() == GameMode.CREATIVE) return;
        Block b = e.getBlock();
        if (!b.getType().name().endsWith("_ORE") || !e.isDropItems()) return;
        ItemStack ferramenta = p.getInventory().getItemInMainHand();
        if (ferramenta.containsEnchantment(Enchantment.SILK_TOUCH)) return;
        if (ThreadLocalRandom.current().nextDouble() >= (estagio(p) >= 5 ? 0.20 : 0.08)) return;
        var extra = b.getDrops(ferramenta, p);
        if (extra.isEmpty()) return;
        var loc = b.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack i : extra) b.getWorld().dropItemNaturally(loc, i);
        b.getWorld().spawnParticle(Particle.WAX_OFF, loc, 8, 0.3, 0.3, 0.3, 0.05);
    }

    /** Pequenino: comer enche mais (estágio II). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if (!eh(p, Raca.PEQUENINO, 2) || !e.getItem().getType().isEdible()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + 2));
            p.setSaturation(Math.min(p.getFoodLevel(), p.getSaturation() + 2));
        });
    }

    /** Pequenino: agachado, os monstros longe não notam (estágio III). */
    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p) || !(e.getEntity() instanceof Monster m)) return;
        if (!p.isSneaking() || !eh(p, Raca.PEQUENINO, 3)) return;
        if (e.getReason() != EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER) return;
        if (m.getLocation().distanceSquared(p.getLocation()) > 25) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        // Pequenino V: desvia de um golpe.
        if (e.getEntity() instanceof Player alvo && eh(alvo, Raca.PEQUENINO, 5)
                && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && ThreadLocalRandom.current().nextDouble() < 0.12) {
            e.setCancelled(true);
            alvo.getWorld().spawnParticle(Particle.CLOUD, alvo.getLocation().add(0, 0.5, 0), 6, 0.2, 0.2, 0.2, 0.02);
            alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1f, 1.5f);
            alvo.sendActionBar(Component.text("✿ Desviou!", Raca.PEQUENINO.cor()));
            return;
        }
        if (!(e.getEntity() instanceof LivingEntity)) return;
        // Elfo: flechas.
        if (e.getDamager() instanceof AbstractArrow a && !(a instanceof Trident) && a.getShooter() instanceof Player arqueiro && eh(arqueiro, Raca.ELFO, 2)) {
            e.setDamage(e.getDamage() * (estagio(arqueiro) >= 5 ? 1.30 : 1.15));
            return;
        }
        // Povo do Mar V: na água ou na chuva (corpo a corpo ou tridente).
        Player atacante = e.getDamager() instanceof Player pp ? pp
                : e.getDamager() instanceof Trident t && t.getShooter() instanceof Player pt ? pt : null;
        if (atacante != null && eh(atacante, Raca.MAR, 5) && (atacante.isInWater() || atacante.isInRain())) {
            e.setDamage(e.getDamage() * 1.25);
        }
    }

    // =====================================================================
    //  Menu (/raca)
    // =====================================================================

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        final Map<Integer, Runnable> acoes = new HashMap<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int[] S_RACAS = {10, 11, 12, 13, 14, 15, 16, 21, 22, 23};
    private static final int[] S_ESTAGIOS = {20, 21, 22, 23, 24};

    private Tela nova(int linhas, String titulo, Material fundo) {
        Tela t = new Tela();
        t.inv = Bukkit.createInventory(t, linhas * 9, Component.text(titulo));
        ItemStack vidro = new ItemStack(fundo);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < t.inv.getSize(); i++) t.inv.setItem(i, vidro);
        return t;
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return i;
    }

    private static void quebrar(List<Component> lore, String texto, TextColor cor) {
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (linha.length() + palavra.length() > 36 && !linha.isEmpty()) {
                lore.add(Component.text(linha.toString(), cor));
                linha.setLength(0);
            }
            if (!linha.isEmpty()) linha.append(' ');
            linha.append(palavra);
        }
        if (!linha.isEmpty()) lore.add(Component.text(linha.toString(), cor));
    }

    /** /raca: escolher (sem raça) ou ver a sua. */
    public void abrir(Player p) {
        if (de(p) == null) abrirEscolha(p, false);
        else abrirRaca(p, de(p), false, false);
    }

    private void abrirEscolha(Player p, boolean troca) {
        Tela t = nova(4, troca ? "✦ Trocar de raça" : "✦ Escolha sua raça", Material.BLACK_STAINED_GLASS_PANE);
        Raca[] rs = Raca.values();
        for (int i = 0; i < rs.length && i < S_RACAS.length; i++) {
            Raca r = rs[i];
            List<Component> lore = new ArrayList<>();
            quebrar(lore, r.descricao(), NamedTextColor.GRAY);
            lore.add(Component.empty());
            lore.add(Component.text("Tamanho: " + Math.round(r.escala() * 100) + "%", NamedTextColor.WHITE));
            lore.add(Component.text("Fraqueza: ", NamedTextColor.RED).append(Component.text(r.fraqueza(), NamedTextColor.GRAY)));
            lore.add(Component.empty());
            for (int e = 1; e <= 5; e++) lore.add(Component.text(Raca.ROMANO[e - 1] + " " + r.estagio(e), r.cor()));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para ver tudo", NamedTextColor.YELLOW));
            t.inv.setItem(S_RACAS[i], item(r.item(), Component.text(r.icone() + " " + r.nome(), r.cor(), TextDecoration.BOLD), lore));
            t.acoes.put(S_RACAS[i], () -> abrirRaca(p, r, true, troca));
        }
        p.openInventory(t.inv);
    }

    /** A ficha de uma raça: os 5 estágios e, se for a sua, o progresso e o Despertar. */
    private void abrirRaca(Player p, Raca r, boolean escolhendo, boolean troca) {
        Tela t = nova(5, r.icone() + " " + r.nome(), Material.GRAY_STAINED_GLASS_PANE);
        boolean minha = !escolhendo && de(p) == r;
        int est = minha ? estagio(p) : 0;

        List<Component> topo = new ArrayList<>();
        quebrar(topo, r.descricao(), NamedTextColor.GRAY);
        topo.add(Component.empty());
        topo.add(Component.text("Tamanho: " + Math.round(r.escala() * 100) + "%", NamedTextColor.WHITE));
        topo.add(Component.text("Fraqueza: ", NamedTextColor.RED).append(Component.text(r.fraqueza(), NamedTextColor.GRAY)));
        topo.add(Component.text("Essência: " + afinidades(r), NamedTextColor.DARK_AQUA));
        if (minha) {
            topo.add(Component.empty());
            topo.add(Component.text("Estágio " + Raca.ROMANO[est - 1] + " · " + r.estagio(est), r.cor()));
            if (est < 5) {
                double atual = essencia(p), precisa = Raca.ESSENCIA[est];
                topo.add(barra(atual / precisa).append(Component.text(" " + (int) Math.min(atual, precisa) + "/" + (int) precisa, NamedTextColor.GRAY)));
            }
        }
        t.inv.setItem(4, item(r.item(), Component.text(r.icone() + " " + r.nome(), r.cor(), TextDecoration.BOLD), topo));

        // 2.53: sinergia com as classes e o título do estágio V.
        List<Component> sin = new ArrayList<>();
        sin.add(Component.text("Com uma destas classes ativa:", NamedTextColor.GRAY));
        for (br.rpgatributos.classe.Classe c : r.sinergia()) sin.add(Component.text(" • " + c.nome(), NamedTextColor.WHITE));
        sin.add(Component.text("+6% de dano e -5% de dano recebido.", NamedTextColor.GREEN));
        if (minha) sin.add(sinergia(p) ? Component.text("✔ Ativa agora", NamedTextColor.GREEN) : Component.text("✖ Sua classe ativa não combina", NamedTextColor.DARK_GRAY));
        sin.add(Component.empty());
        sin.add(Component.text("No estágio V: o título da raça.", NamedTextColor.GOLD));
        t.inv.setItem(42, item(Material.NETHER_STAR, Component.text("✦ Sinergia de raça e classe", r.cor(), TextDecoration.BOLD), sin));
        t.inv.setItem(41, item(Material.LEATHER_HELMET, Component.text("Enfeites de cabeça", r.cor(), TextDecoration.BOLD), List.of(
                Component.text("Orelhas, barba, chifres, barbatanas...", NamedTextColor.GRAY),
                Component.text("Vista na cabeça ou ponha no Guarda-roupa", NamedTextColor.GRAY),
                Component.text("por cima do capacete (pacote de recursos).", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique para escolher", NamedTextColor.YELLOW))));
        t.acoes.put(41, () -> abrirEnfeites(p, r));

        for (int e = 1; e <= 5; e++) {
            boolean tem = minha && est >= e;
            List<Component> lore = new ArrayList<>();
            quebrar(lore, r.efeito(e), NamedTextColor.WHITE);
            if (e > 1) {
                lore.add(Component.empty());
                lore.add(Component.text("Precisa: " + (int) Raca.ESSENCIA[e - 1] + " de Essência", NamedTextColor.GRAY));
                lore.add(Component.text("Oferenda: " + r.quantidade(e) + "× " + nomeItem(r.oferenda(e)), NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(tem ? Component.text("✔ Desperto", NamedTextColor.GREEN) : Component.text("✖ Ainda não", NamedTextColor.DARK_GRAY));
            t.inv.setItem(S_ESTAGIOS[e - 1], item(tem ? Material.LIME_STAINED_GLASS_PANE : e == est + 1 && minha ? Material.YELLOW_STAINED_GLASS_PANE
                            : Material.WHITE_STAINED_GLASS_PANE,
                    Component.text(Raca.ROMANO[e - 1] + " · " + r.estagio(e), r.cor(), TextDecoration.BOLD), lore));
        }

        if (escolhendo) {
            boolean podeTrocar = p.getGameMode() == GameMode.CREATIVE || p.getLevel() >= CUSTO_TROCA;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Você começa no estágio I.", NamedTextColor.GRAY));
            if (troca) {
                lore.add(Component.text("Trocar custa " + CUSTO_TROCA + " níveis e zera a Essência.", podeTrocar ? NamedTextColor.GRAY : NamedTextColor.RED));
            } else {
                lore.add(Component.text("A primeira escolha é de graça.", NamedTextColor.GRAY));
                lore.add(Component.text("Trocar depois custa " + CUSTO_TROCA + " níveis.", NamedTextColor.DARK_GRAY));
            }
            t.inv.setItem(40, item(Material.LIME_CONCRETE, Component.text("✔ Ser " + r.nome(), NamedTextColor.GREEN, TextDecoration.BOLD), lore));
            t.acoes.put(40, () -> escolher(p, r, troca));
            t.inv.setItem(36, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.GRAY), List.of()));
            t.acoes.put(36, () -> abrirEscolha(p, troca));
        } else if (minha) {
            if (est < 5) {
                List<String> falta = falta(p);
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(r.estagio(est + 1) + ": " + r.efeito(est + 1), NamedTextColor.WHITE));
                lore.add(Component.empty());
                if (falta.isEmpty()) lore.add(Component.text("» Clique para despertar", NamedTextColor.YELLOW));
                else for (String f : falta) lore.add(Component.text("Falta: " + f, NamedTextColor.RED));
                t.inv.setItem(40, item(falta.isEmpty() ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE,
                        Component.text("✦ Despertar o estágio " + Raca.ROMANO[est], r.cor(), TextDecoration.BOLD), lore));
                t.acoes.put(40, () -> {
                    if (despertar(p)) abrirRaca(p, r, false, false);
                    else p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
                });
            }
            if (r == Raca.HUMANO && est >= 5) {
                Raca h = heranca(p);
                t.inv.setItem(38, item(h == null ? Material.NETHER_STAR : h.item(), Component.text("☺ Herança: " + (h == null ? "escolha!" : h.nome()),
                        r.cor(), TextDecoration.BOLD), List.of(Component.text("O dom de outra raça. Pode trocar de graça.", NamedTextColor.GRAY),
                        Component.text("» Clique para escolher", NamedTextColor.YELLOW))));
                t.acoes.put(38, () -> abrirHeranca(p));
            }
            t.inv.setItem(44, item(Material.ENDER_EYE, Component.text("Trocar de raça", NamedTextColor.RED), List.of(
                    Component.text("Custa " + CUSTO_TROCA + " níveis e volta ao estágio I.", NamedTextColor.GRAY))));
            t.acoes.put(44, () -> abrirEscolha(p, true));
            t.inv.setItem(36, item(Material.BOOK, Component.text("Ver as outras raças", NamedTextColor.GRAY), List.of()));
            t.acoes.put(36, () -> abrirEscolha(p, true));
        }
        p.openInventory(t.inv);
    }

    private void abrirHeranca(Player p) {
        Tela t = nova(4, "☺ Herança", Material.BLACK_STAINED_GLASS_PANE);
        Raca[] rs = Raca.values();
        int s = 0;
        for (int i = 1; i < rs.length; i++, s++) {
            Raca r = rs[i];
            boolean atual = heranca(p) == r;
            String dom = switch (r) {
                case ANAO -> "Minera 20% mais rápido";
                case ELFO -> "8% mais rápido";
                case PEQUENINO -> "+2 de sorte";
                case PASSARO -> "Sem dano de queda até 6 blocos";
                case MAR -> "Fôlego maior e nada mais rápido";
                case DRACONATO -> "Leva metade do dano de fogo e lava";
                case FERA -> "4% mais rápido";
                case ESPECTRAL -> "Sem dano de queda até 6 blocos";
                case GNOMO -> "+1 de alcance para mexer em blocos";
                default -> "";
            };
            t.inv.setItem(S_RACAS[s], item(r.item(), Component.text(r.icone() + " Herança " + (r == Raca.ANAO ? "anã" : "de " + r.nome()), r.cor(), TextDecoration.BOLD),
                    List.of(Component.text(dom, NamedTextColor.WHITE), Component.empty(),
                            atual ? Component.text("✔ A sua", NamedTextColor.GREEN) : Component.text("» Clique para escolher", NamedTextColor.YELLOW))));
            t.acoes.put(S_RACAS[s], () -> {
                p.getPersistentDataContainer().set(kHeranca, PersistentDataType.STRING, r.id());
                aplicar(p);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
                abrirRaca(p, Raca.HUMANO, false, false);
            });
        }
        p.openInventory(t.inv);
    }

    /** Os enfeites de cabeça (de graça, um por clique; o da sua raça vem primeiro). */
    private void abrirEnfeites(Player p, Raca voltar) {
        Tela t = nova(3, "Enfeites de cabeça", Material.BLACK_STAINED_GLASS_PANE);
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        Enfeites.Enfeite[] es = Enfeites.Enfeite.values();
        for (int i = 0; i < es.length && i < slots.length; i++) {
            Enfeites.Enfeite en = es[i];
            ItemStack icone = Enfeites.criar(en);
            icone.editMeta(m -> {
                List<Component> lore = new ArrayList<>(m.lore() == null ? List.of() : m.lore());
                lore.add(Component.empty());
                lore.add(Component.text("» Clique para pegar", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                m.lore(lore);
            });
            t.inv.setItem(slots[i], icone);
            t.acoes.put(slots[i], () -> {
                for (ItemStack sobra : p.getInventory().addItem(Enfeites.criar(en)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.8f, 1.2f);
                p.sendActionBar(Component.text("Enfeite no inventário: clique direito com ele para vestir.", NamedTextColor.GREEN));
            });
        }
        t.inv.setItem(18, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.GRAY), List.of()));
        t.acoes.put(18, () -> abrir(p));
        p.openInventory(t.inv);
    }

    private void escolher(Player p, Raca r, boolean troca) {
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (de(p) == r) {
            p.closeInventory();
            return;
        }
        if (troca && de(p) != null) {
            if (!criativo && p.getLevel() < CUSTO_TROCA) {
                p.sendMessage(Component.text("Trocar de raça custa " + CUSTO_TROCA + " níveis de experiência.", NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
                return;
            }
            if (!criativo) p.setLevel(p.getLevel() - CUSTO_TROCA);
        }
        definir(p, r, 1);
        p.closeInventory();
        p.showTitle(Title.title(Component.text(r.icone() + " " + r.nome(), r.cor(), TextDecoration.BOLD),
                Component.text("Estágio I · " + r.estagio(1), NamedTextColor.WHITE)));
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
        p.sendMessage(Component.text(r.icone() + " Agora você é " + r.nome() + ". A Essência vem de " + afinidades(r)
                + "; veja os estágios no /raca.", r.cor()));
        plugin.diario().marco(p, "raca_" + r.id(), "Tornou-se " + r.nome());
    }

    private static String afinidades(Raca r) {
        if (r.afinidades().isEmpty()) return "todo XP (pela metade)";
        List<String> n = new ArrayList<>();
        for (Skill s : r.afinidades()) n.add(s.nome());
        n.sort(String::compareTo);
        return String.join(", ", n) + " (e um pouco do resto)";
    }

    private static Component barra(double f) {
        int cheio = (int) Math.round(Math.max(0, Math.min(1, f)) * 20);
        return Component.text("|".repeat(cheio), NamedTextColor.AQUA).append(Component.text("|".repeat(20 - cheio), NamedTextColor.DARK_GRAY));
    }

    private static final Map<Material, String> NOMES = new EnumMap<>(Material.class);
    static {
        Object[] kv = {Material.BOOK, "Livro", Material.EMERALD, "Esmeralda", Material.GOLDEN_APPLE, "Maçã Dourada",
                Material.TOTEM_OF_UNDYING, "Totem da Imortalidade", Material.COAL, "Carvão", Material.IRON_INGOT, "Lingote de Ferro",
                Material.GOLD_INGOT, "Lingote de Ouro", Material.DIAMOND, "Diamante", Material.ARROW, "Flecha", Material.GLOW_BERRIES, "Baga Brilhante",
                Material.AMETHYST_SHARD, "Fragmento de Ametista", Material.BREAD, "Pão", Material.HONEY_BOTTLE, "Garrafa de Mel",
                Material.PUMPKIN_PIE, "Torta de Abóbora", Material.GOLDEN_CARROT, "Cenoura Dourada", Material.FEATHER, "Pena",
                Material.PHANTOM_MEMBRANE, "Membrana de Phantom", Material.BREEZE_ROD, "Vara de Breeze", Material.ELYTRA, "Élitros",
                Material.PRISMARINE_SHARD, "Fragmento de Prismarinho", Material.PRISMARINE_CRYSTALS, "Cristal de Prismarinho",
                Material.NAUTILUS_SHELL, "Concha de Náutilo", Material.HEART_OF_THE_SEA, "Coração do Mar"};
        for (int i = 0; i + 1 < kv.length; i += 2) NOMES.put((Material) kv[i], (String) kv[i + 1]);
    }

    static String nomeItem(Material m) {
        if (m == null) return "-";
        String n = NOMES.get(m);
        return n != null ? n : m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        Runnable r = t.acoes.get(e.getRawSlot());
        if (r == null) return;
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
        r.run();
    }

    // =====================================================================
    //  Comando
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("definir")) {
            if (!s.hasPermission("rpg.admin")) {
                s.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                s.sendMessage(Component.text("/raca definir <jogador> <raça|nenhuma> [estágio 1-5]", NamedTextColor.GRAY));
                return true;
            }
            Player alvo = Bukkit.getPlayerExact(args[1]);
            if (alvo == null) {
                s.sendMessage(Component.text("Jogador não encontrado.", NamedTextColor.RED));
                return true;
            }
            Raca r = Raca.porId(args[2]);
            if (r == null && !args[2].equalsIgnoreCase("nenhuma")) {
                s.sendMessage(Component.text("Raça desconhecida.", NamedTextColor.RED));
                return true;
            }
            int est = 1;
            if (args.length >= 4) {
                try {
                    est = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    est = 1;
                }
            }
            definir(alvo, r, est);
            s.sendMessage(Component.text(alvo.getName() + ": " + (r == null ? "sem raça" : r.nome() + " estágio " + Raca.ROMANO[Math.max(1, Math.min(5, est)) - 1]),
                    NamedTextColor.GREEN));
            return true;
        }
        if (!(s instanceof Player p)) {
            s.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        if (!s.hasPermission("rpg.admin")) return List.of();
        if (args.length == 1) return List.of("definir");
        if (args.length == 2 && args[0].equalsIgnoreCase("definir")) return null;
        if (args.length == 3 && args[0].equalsIgnoreCase("definir")) {
            List<String> l = new ArrayList<>();
            for (Raca r : Raca.values()) l.add(r.id());
            l.add("nenhuma");
            return l;
        }
        if (args.length == 4) return List.of("1", "2", "3", "4", "5");
        return List.of();
    }
}
