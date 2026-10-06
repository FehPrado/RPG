package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.fe.Runa;
import br.rpgatributos.fe.Runas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ligações do combate com o resto do plugin:
 * <ul>
 *   <li><b>Óleos de lâmina</b> (Alquimia): passados na arma, valem 60 golpes (fogo, gelo, trovão, veneno, prata);</li>
 *   <li><b>Combos × magia</b>: o golpe de combo leva o elemento do óleo ou da runa da arma e reage com as magias;</li>
 *   <li><b>Ataque conjunto</b>: dois da mesma party acertando combos no mesmo alvo em 3 s;</li>
 *   <li><b>Companheiros</b>: no finalizador, os seus companheiros atacam o alvo.</li>
 * </ul>
 */
public final class Ligacoes implements Listener {

    private static final NamespacedKey K_OLEO = new NamespacedKey("rpgatributos", "oleo");
    private static final NamespacedKey K_OLEO_GOLPES = new NamespacedKey("rpgatributos", "oleo_golpes");
    private static final String MARCA_LORE = "🜄 ";
    private static final int GOLPES_POR_OLEO = 60;
    private static final long CONJUNTO_MS = 3000, RECARGA_CONJUNTO_MS = 4000;

    private final RPGAtributos plugin;
    private final Combos combos;
    /** Alvo → {quem acertou o último combo, quando}. */
    private final Map<UUID, Object[]> ultimoCombo = new HashMap<>();
    private final Map<UUID, Long> recargaConjunto = new HashMap<>();
    /** Jogador → o último alvo de combo dele (para os companheiros no finalizador). */
    private final Map<UUID, UUID> ultimoAlvo = new HashMap<>();
    /** A faísca do Óleo Trovejante está ferindo (não salta de novo nem gasta óleo). */
    private boolean faiscando;

    Ligacoes(RPGAtributos plugin, Combos combos) {
        this.plugin = plugin;
        this.combos = combos;
    }

    private static boolean mortoVivo(Entity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof Phantom || e instanceof Wither || e instanceof Zoglin;
    }

    // =====================================================================
    //  Óleos de lâmina
    // =====================================================================

    public static Reagente oleo(ItemStack arma) {
        if (arma == null || arma.isEmpty() || !arma.hasItemMeta()) return null;
        String id = arma.getPersistentDataContainer().get(K_OLEO, PersistentDataType.STRING);
        Reagente r = id == null ? null : Reagente.porId(id);
        return r != null && r.oleo() ? r : null;
    }

    private static Essencia elemento(Reagente oleo) {
        return switch (oleo) {
            case OLEO_DE_FOGO -> Essencia.FOGO;
            case OLEO_GELIDO -> Essencia.GELO;
            case OLEO_TROVEJANTE -> Essencia.ENERGIA;
            case OLEO_VENENOSO -> Essencia.NATUREZA;
            case OLEO_DE_PRATA -> Essencia.VIDA;
            default -> null;
        };
    }

    private static Essencia elemento(Runa r) {
        if (r == null) return null;
        return switch (r) {
            case EXPLOSAO -> Essencia.FOGO;
            case GELO -> Essencia.GELO;
            case TEMPESTADE -> Essencia.ENERGIA;
            case VAMPIRICA -> Essencia.SOMBRA;
            default -> null;
        };
    }

    /** Segurar um óleo e clicar com o direito: vai na arma da outra mão (ou na 1ª arma da barra). */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsarOleo(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        Reagente r = Reagente.de(mao);
        if (r == null || !r.oleo()) return;
        // Baú, porta, alavanca...: o clique é para o bloco, não para passar o óleo.
        if (e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) return;
        e.setCancelled(true);
        ItemStack arma = p.getInventory().getItemInOffHand();
        if (TipoArma.de(arma) == null) {
            arma = null;
            for (int i = 0; i < 9; i++) {
                ItemStack x = p.getInventory().getItem(i);
                if (TipoArma.de(x) != null) {
                    arma = x;
                    break;
                }
            }
        }
        if (arma == null) {
            p.sendActionBar(Component.text("✖ Ponha uma arma na outra mão (F) ou na sua barra.", NamedTextColor.RED));
            return;
        }
        int golpes = GOLPES_POR_OLEO + plugin.stats().getNivel(p, br.rpgatributos.Skill.ALQUIMIA) / 2;
        ItemStack alvo = arma;
        alvo.editMeta(m -> {
            m.getPersistentDataContainer().set(K_OLEO, PersistentDataType.STRING, r.id());
            m.getPersistentDataContainer().set(K_OLEO_GOLPES, PersistentDataType.INTEGER, golpes);
        });
        lore(alvo, r, golpes);
        mao.setAmount(mao.getAmount() - 1);
        p.playSound(p.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.8f, 1.4f);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1.2, 0), 25, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(r.cor().value()), 1.2f));
        p.sendActionBar(Component.text("🜄 " + r.nome() + " na arma (" + golpes + " golpes)", r.cor()));
    }

    /** A linha do óleo no texto da arma (troca a antiga, se houver). */
    private static void lore(ItemStack arma, Reagente r, int golpes) {
        arma.editMeta(m -> lore(m, r, golpes));
    }

    private static void lore(org.bukkit.inventory.meta.ItemMeta m, Reagente r, int golpes) {
        List<Component> l = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
        l.removeIf(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c).startsWith(MARCA_LORE));
        if (r != null) l.add(Component.text(MARCA_LORE + r.nome() + " · " + golpes + " golpes", r.cor()).decoration(TextDecoration.ITALIC, false));
        m.lore(l);
    }

    /** A Forja refez a arma (refino, reforjar): a linha do óleo volta se ainda houver óleo. */
    public static void decorar(org.bukkit.inventory.meta.ItemMeta m) {
        String id = m.getPersistentDataContainer().get(K_OLEO, PersistentDataType.STRING);
        Reagente r = id == null ? null : Reagente.porId(id);
        if (r == null || !r.oleo()) return;
        lore(m, r, m.getPersistentDataContainer().getOrDefault(K_OLEO_GOLPES, PersistentDataType.INTEGER, 0));
    }

    private static Player atacante(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) return p;
        if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerirPrata(EntityDamageByEntityEvent e) {
        Player p = atacante(e);
        if (p == null || !mortoVivo(e.getEntity())) return;
        if (oleo(p.getInventory().getItemInMainHand()) == Reagente.OLEO_DE_PRATA) e.setDamage(e.getDamage() * 1.4);
    }

    /** O golpe acertou: efeito do óleo e um uso a menos. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerirOleo(EntityDamageByEntityEvent e) {
        Player p = atacante(e);
        if (faiscando || p == null || !(e.getEntity() instanceof LivingEntity alvo) || alvo == p) return;
        ItemStack arma = p.getInventory().getItemInMainHand();
        Reagente r = oleo(arma);
        if (r == null) return;
        switch (r) {
            case OLEO_DE_FOGO -> alvo.setFireTicks(Math.max(alvo.getFireTicks(), 80));
            case OLEO_GELIDO -> {
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true));
                alvo.setFreezeTicks(Math.min(alvo.getMaxFreezeTicks(), alvo.getFreezeTicks() + 60));
            }
            case OLEO_VENENOSO -> { if (!mortoVivo(alvo)) alvo.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true, true)); }
            case OLEO_TROVEJANTE -> {
                if (!faiscando && ThreadLocalRandom.current().nextDouble() < 0.15) {
                    for (Entity x : alvo.getNearbyEntities(4, 3, 4)) {
                        if (!(x instanceof LivingEntity o) || o == p || !plugin.alvos().inimigo(p, o)) continue;
                        Location a = alvo.getLocation().add(0, 1, 0), b = o.getLocation().add(0, 1, 0);
                        Vector d = b.toVector().subtract(a.toVector());
                        for (double s = 0; s <= 1; s += 0.1) a.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, a.clone().add(d.clone().multiply(s)), 1, 0, 0, 0, 0);
                        faiscando = true;
                        try {
                            plugin.alvos().ferir(p, o, 3);
                        } finally {
                            faiscando = false;
                        }
                        o.getWorld().playSound(b, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.8f);
                        break;
                    }
                }
            }
            case OLEO_DE_PRATA -> { if (mortoVivo(alvo)) alvo.getWorld().spawnParticle(Particle.END_ROD, alvo.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.02); }
            default -> { }
        }
        int resta = arma.getPersistentDataContainer().getOrDefault(K_OLEO_GOLPES, PersistentDataType.INTEGER, 0) - 1;
        if (resta <= 0) {
            arma.editMeta(m -> {
                m.getPersistentDataContainer().remove(K_OLEO);
                m.getPersistentDataContainer().remove(K_OLEO_GOLPES);
            });
            lore(arma, null, 0);
            p.sendActionBar(Component.text("🜄 O " + r.nome() + " acabou.", NamedTextColor.GRAY));
            return;
        }
        arma.editMeta(m -> m.getPersistentDataContainer().set(K_OLEO_GOLPES, PersistentDataType.INTEGER, resta));
        if (resta % 10 == 0) lore(arma, r, resta);
    }

    // =====================================================================
    //  Golpe de combo: elemento, ataque conjunto e alvo dos companheiros
    // =====================================================================

    /** Chamado pelos golpes de combo, depois do dano. */
    void aoGolpe(Player p, LivingEntity alvo, double dano) {
        ItemStack arma = p.getInventory().getItemInMainHand();
        Reagente r = oleo(arma);
        Essencia el = r != null ? elemento(r) : elemento(Runas.runa(arma));
        if (el != null && alvo.isValid() && !alvo.isDead()) {
            TipoArma t = TipoArma.de(arma);
            double pot = 1.5 + 0.075 * (t == null ? 1 : combos.nivel(p, t));
            plugin.arcano().conjuracao().elementoDeGolpe(p, alvo, el, pot);
        }
        ultimoAlvo.put(p.getUniqueId(), alvo.getUniqueId());
        conjunto(p, alvo, dano);
    }

    private void conjunto(Player p, LivingEntity alvo, double dano) {
        long t = System.currentTimeMillis();
        UUID id = alvo.getUniqueId();
        Object[] antes = ultimoCombo.put(id, new Object[]{p.getUniqueId(), t});
        if (antes == null || antes[0].equals(p.getUniqueId()) || t - (long) antes[1] > CONJUNTO_MS) return;
        if (t < recargaConjunto.getOrDefault(id, 0L)) return;
        UUID outroId = (UUID) antes[0];
        if (!plugin.parties().mesmaParty(p.getUniqueId(), outroId)) return;
        Player outro = Bukkit.getPlayer(outroId);
        if (outro == null || !alvo.isValid() || alvo.isDead()) return;
        recargaConjunto.put(id, t + RECARGA_CONJUNTO_MS);
        ultimoCombo.remove(id);
        // Os dois golpes se somam: dano extra, muito equilíbrio e estilo para os dois.
        plugin.alvos().ferir(p, alvo, dano * 0.5);
        combos.equilibrio().golpe(p, alvo, dano * 1.5, combos.estilo());
        combos.estilo().bonus(p, 15);
        combos.estilo().bonus(outro, 15);
        Location c = alvo.getLocation().add(0, alvo.getHeight() / 2, 0);
        for (Player x : List.of(p, outro)) {
            Location de = x.getLocation().add(0, 1.2, 0);
            Vector d = c.toVector().subtract(de.toVector());
            for (double s = 0; s <= 1; s += 0.08) c.getWorld().spawnParticle(Particle.END_ROD, de.clone().add(d.clone().multiply(s)), 1, 0, 0, 0, 0);
            x.showTitle(Title.title(Component.empty(), Component.text("⚔ ATAQUE CONJUNTO!", Combos.COR, TextDecoration.BOLD),
                    Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(900), Duration.ofMillis(250))));
        }
        c.getWorld().spawnParticle(Particle.EXPLOSION, c, 1, 0, 0, 0, 0);
        c.getWorld().playSound(c, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f);
        plugin.titulos().registrar(p, "ataques_conjuntos", 1);
        plugin.titulos().registrar(outro, "ataques_conjuntos", 1);
    }

    /** Finalizador: os companheiros perto partem para cima do último alvo do combo. */
    void finalizador(Player p) {
        UUID alvoId = ultimoAlvo.get(p.getUniqueId());
        if (!(alvoId != null && Bukkit.getEntity(alvoId) instanceof LivingEntity alvo) || !alvo.isValid() || alvo.isDead()) return;
        int n = 0;
        for (UUID cid : plugin.companheiros().ativos()) {
            if (!(Bukkit.getEntity(cid) instanceof Mob m) || !p.getUniqueId().equals(Companheiros.dono(m))) continue;
            if (!m.getWorld().equals(p.getWorld()) || m.getLocation().distanceSquared(p.getLocation()) > 24 * 24) continue;
            m.setTarget(alvo);
            m.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, m.getLocation().add(0, m.getHeight(), 0), 3, 0.2, 0.2, 0.2, 0);
            n++;
        }
        if (n > 0) p.sendMessage(Component.text("🐾 Seus companheiros atacam o alvo do finalizador!", NamedTextColor.GOLD));
    }

    /** A cada meio segundo: tira o que ficou velho. */
    void tick() {
        long t = System.currentTimeMillis();
        ultimoCombo.values().removeIf(x -> t - (long) x[1] > CONJUNTO_MS * 2);
        recargaConjunto.values().removeIf(x -> x < t);
    }
}
