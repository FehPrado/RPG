package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Equilíbrio dos inimigos fortes (chefes, elites, guardiões... quem tem 40 de vida ou mais):
 * os golpes de combo enchem uma barra; cheia, o inimigo fica desequilibrado por alguns
 * segundos (parado e levando +50% de dano). Fora de luta a barra esvazia.
 */
public final class Equilibrio {

    private static final double VIDA_MINIMA = 40, FRACAO_DA_VIDA = 0.5;
    private static final long QUEBRA_MS = 4000, IMUNE_MS = 8000, SEM_GOLPE_MS = 3000;
    public static final double DANO_QUEBRADO = 1.5;

    private static final class Estado {
        double valor;
        long ultimoGolpe, quebradoAte, imuneAte;
        boolean desligouIa;
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Estado> estados = new HashMap<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private final Map<UUID, Long> mostrarAte = new HashMap<>();

    Equilibrio(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static final NamespacedKey K_PARADO = new NamespacedKey("rpgatributos", "equilibrio_parado");

    private static void acordar(Mob m) {
        m.setAware(true);
        m.getPersistentDataContainer().remove(K_PARADO);
    }

    /** Criatura que descarregou desequilibrada: volta a agir quando carrega de novo. */
    static void aoCarregar(org.bukkit.entity.Entity e) {
        if (e instanceof Mob m && m.getPersistentDataContainer().has(K_PARADO)) acordar(m);
    }

    private static double maximo(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? 0 : a.getValue();
    }

    /** Só os inimigos fortes têm equilíbrio (e nunca jogadores). */
    public static boolean tem(LivingEntity e) {
        return !(e instanceof Player) && maximo(e) >= VIDA_MINIMA;
    }

    public boolean quebrado(LivingEntity e) {
        Estado s = estados.get(e.getUniqueId());
        return s != null && s.quebradoAte > System.currentTimeMillis();
    }

    /** Um golpe de combo acertou. {@code forca}: o dano do golpe já com os multiplicadores de equilíbrio. */
    void golpe(Player p, LivingEntity alvo, double forca, Estilo estilo) {
        if (!tem(alvo) || !alvo.isValid() || alvo.isDead()) return;
        long t = System.currentTimeMillis();
        Estado s = estados.computeIfAbsent(alvo.getUniqueId(), k -> new Estado());
        if (s.quebradoAte > t || s.imuneAte > t) return;
        double max = Math.max(20, maximo(alvo) * FRACAO_DA_VIDA);
        s.valor = Math.min(max, s.valor + forca);
        s.ultimoGolpe = t;
        if (s.valor >= max) {
            quebrar(alvo, s, t);
            estilo.bonus(p, 15);
        }
        mostrar(p, alvo, s, max, t);
    }

    private void quebrar(LivingEntity alvo, Estado s, long t) {
        s.quebradoAte = t + QUEBRA_MS;
        s.imuneAte = t + QUEBRA_MS + IMUNE_MS;
        s.valor = 0;
        int ticks = (int) (QUEBRA_MS / 50);
        alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 5, false, false, true));
        alvo.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 3, false, false, true));
        if (alvo instanceof Mob m && m.isAware()) {
            m.setAware(false);
            m.getPersistentDataContainer().set(K_PARADO, PersistentDataType.BYTE, (byte) 1);
            s.desligouIa = true;
        }
        Location c = alvo.getLocation().add(0, alvo.getHeight() + 0.3, 0);
        alvo.getWorld().spawnParticle(Particle.CRIT, c, 40, 0.5, 0.3, 0.5, 0.3);
        alvo.getWorld().spawnParticle(Particle.FLASH, c, 1, 0, 0, 0, 0);
        alvo.getWorld().playSound(c, Sound.ITEM_SHIELD_BREAK, 1f, 0.6f);
        alvo.getWorld().playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.6f);
        for (Player o : alvo.getWorld().getPlayers()) {
            if (o.getLocation().distanceSquared(alvo.getLocation()) > 24 * 24) continue;
            o.showTitle(Title.title(Component.empty(), Component.text("✦ EQUILÍBRIO QUEBRADO: dano +50% por 4 s", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(1500), Duration.ofMillis(300))));
        }
    }

    private void mostrar(Player p, LivingEntity alvo, Estado s, double max, long t) {
        BossBar b = barras.get(p.getUniqueId());
        Component nome = Component.text("⚖ Equilíbrio: ", NamedTextColor.GOLD)
                .append(alvo.customName() != null ? alvo.customName() : Component.translatable(alvo.getType().translationKey()));
        float prog = s.quebradoAte > t ? 1f : (float) Math.max(0, Math.min(1, s.valor / max));
        if (b == null) {
            b = BossBar.bossBar(nome, prog, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_6);
            barras.put(p.getUniqueId(), b);
            p.showBossBar(b);
        } else {
            b.name(nome);
            b.progress(prog);
        }
        b.color(s.quebradoAte > t ? BossBar.Color.YELLOW : BossBar.Color.WHITE);
        mostrarAte.put(p.getUniqueId(), t + 3500);
    }

    /** A cada meio segundo: a barra esvazia fora de luta, quem foi quebrado volta a agir. */
    void tick() {
        long t = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Estado>> it = estados.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Estado> en = it.next();
            Estado s = en.getValue();
            if (s.desligouIa && s.quebradoAte <= t) {
                if (plugin.getServer().getEntity(en.getKey()) instanceof Mob m && m.isValid()) acordar(m);
                s.desligouIa = false;
            }
            if (s.quebradoAte <= t && t - s.ultimoGolpe > SEM_GOLPE_MS) s.valor = Math.max(0, s.valor * 0.85 - 1);
            if (s.valor <= 0 && !s.desligouIa && s.imuneAte <= t && t - s.ultimoGolpe > 20_000) it.remove();
        }
        Iterator<Map.Entry<UUID, Long>> mt = mostrarAte.entrySet().iterator();
        while (mt.hasNext()) {
            Map.Entry<UUID, Long> en = mt.next();
            if (en.getValue() > t) continue;
            mt.remove();
            BossBar b = barras.remove(en.getKey());
            Player p = plugin.getServer().getPlayer(en.getKey());
            if (b != null && p != null) p.hideBossBar(b);
        }
    }

    /** Ao desligar: ninguém fica parado para sempre. */
    void parar() {
        for (Map.Entry<UUID, Estado> en : estados.entrySet()) {
            if (en.getValue().desligouIa && plugin.getServer().getEntity(en.getKey()) instanceof Mob m) acordar(m);
        }
        estados.clear();
        for (Map.Entry<UUID, BossBar> en : barras.entrySet()) {
            Player p = plugin.getServer().getPlayer(en.getKey());
            if (p != null) p.hideBossBar(en.getValue());
        }
        barras.clear();
    }
}
