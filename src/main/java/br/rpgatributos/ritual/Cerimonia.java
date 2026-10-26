package br.rpgatributos.ritual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Os 10 segundos do ritual no Altar da Convergência:
 * 0–2 s os itens sobem dos pedestais e o céu escurece; 2–7,5 s giram em volta do altar,
 * cada vez mais rápido e mais perto, com feixes, trovões e o chão tremendo;
 * 7,5–9 s são sugados para o centro; 9 s explode, e o resultado fica flutuando até cair.
 * 2.86: os sacrificados sobem e giram junto e se desfazem na explosão. Se quem acendeu se
 * afastar (ou cair) ou um pedestal usado quebrar, o ritual sai errado: os ingredientes se
 * perdem e uma Aberração sai do altar.
 */
final class Cerimonia {

    static final int DURACAO = 200;
    private static final int SUBIR = 40, GIRAR = 150, SUGAR = 180;
    private static final double ALTURA = 2.6, LONGE = 24;

    private final AltarPedestais altar;
    final Location bloco;
    private final Location centro;
    private final World w;
    private final UUID dono;
    private final ReceitaRitual receita;
    private final ItemStack catalisador, resultado;
    /** O que foi tirado de cada pedestal (volta para o chão se o ritual for interrompido). */
    private final List<ItemStack> gastos;
    private final List<ItemDisplay> voando = new ArrayList<>();
    private final List<Location> origens = new ArrayList<>();
    private final Set<UUID> assistindo = new HashSet<>();
    private final Color cor;
    private final List<LivingEntity> vitimas = new ArrayList<>();
    private final List<Double> escalaVitimas = new ArrayList<>();
    private List<Player> grupo = List.of();
    private List<ItemStack> doGrupo = List.of();
    private ItemDisplay nucleo;
    private BukkitTask tarefa;
    private int t;
    private double giro;
    private final long horaMundo;

    Cerimonia(AltarPedestais altar, Location bloco, UUID dono, ReceitaRitual receita, ItemStack catalisador,
              ItemStack resultado, List<Location> pedestais, List<ItemStack> gastos) {
        this.altar = altar;
        this.bloco = bloco;
        this.centro = bloco.clone().add(0.5, 1, 0.5);
        this.w = bloco.getWorld();
        this.dono = dono;
        this.receita = receita;
        this.catalisador = catalisador;
        this.resultado = resultado;
        this.gastos = gastos;
        this.horaMundo = w.getTime();
        var c = receita.categoria().cor;
        this.cor = Color.fromRGB(c.red(), c.green(), c.blue());
        for (int k = 0; k < pedestais.size(); k++) {
            Location o = pedestais.get(k).clone().add(0.5, 1.25, 0.5);
            origens.add(o);
            ItemStack visual = gastos.get(k).clone();
            visual.setAmount(1);
            voando.add(exibir(o, visual, 0.5f));
        }
        nucleo = exibir(centro.clone().add(0, 0.3, 0), catalisador, 0.6f);
    }

    /** Os mobs do sacrifício: param, ficam imunes e sobem com os itens. */
    void sacrificios(List<LivingEntity> l) {
        for (LivingEntity v : l) {
            if (v instanceof Mob m) m.setAI(false);
            v.setInvulnerable(true);
            v.setGravity(false);
            if (v.isLeashed()) v.setLeashHolder(null);
            AttributeInstance e = v.getAttribute(Attribute.SCALE);
            escalaVitimas.add(e == null ? 1 : e.getBaseValue());
            vitimas.add(v);
        }
    }

    void grupo(List<Player> jogadores, List<ItemStack> catalisadores) {
        this.grupo = jogadores;
        this.doGrupo = catalisadores;
    }

    private ItemDisplay exibir(Location l, ItemStack item, float escala) {
        return w.spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setPersistent(false);
            d.setTeleportDuration(2);
            d.setInterpolationDuration(4);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setGlowing(true);
            d.setGlowColorOverride(cor);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(escala, escala, escala), new AxisAngle4f()));
        });
    }

    void comecar() {
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(centro) > 48 * 48) continue;
            assistindo.add(p.getUniqueId());
            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
        w.playSound(centro, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.6f);
        w.playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f, 0.8f);
        w.playSound(centro, Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.2f, 0.7f);
        tarefa = Bukkit.getScheduler().runTaskTimer(altar.plugin(), this::passo, 1L, 1L);
    }

    private List<Player> quemAssiste() {
        List<Player> l = new ArrayList<>();
        for (UUID id : assistindo) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getWorld() == w) l.add(p);
        }
        return l;
    }

    /** Por que o ritual sai errado agora, ou null se está tudo certo. */
    private String motivoFalha() {
        Player p = Bukkit.getPlayer(dono);
        if (p == null || p.isDead() || p.getWorld() != w) return "quem acendeu o altar caiu ou foi embora";
        if (p.getLocation().distanceSquared(centro) > LONGE * LONGE) return p.getName() + " se afastou do altar";
        for (Location o : origens) {
            if (!altar.pedestalInteiro(o.clone().subtract(0.5, 1.25, 0.5))) return "um pedestal foi quebrado";
        }
        return null;
    }

    private void passo() {
        t++;
        if (!altar.eh(bloco.getBlock()) || !bloco.isChunkLoaded()) {
            interromper();
            return;
        }
        if (t % 10 == 0 && t <= SUGAR) {
            String falha = motivoFalha();
            if (falha != null) {
                falhar(falha);
                return;
            }
        }
        escurecer();
        int n = voando.size();
        Location alvo = centro.clone().add(0, ALTURA, 0);

        if (t <= SUBIR) {
            // Sobem dos pedestais, girando.
            double f = suave(t / (double) SUBIR);
            for (int k = 0; k < n; k++) {
                Location l = origens.get(k).clone().add(0, f * 1.6, 0);
                mover(voando.get(k), l, t * 12f);
                if (t % 2 == 0) w.spawnParticle(Particle.END_ROD, l, 1, 0.05, 0.05, 0.05, 0.01);
            }
            mover(nucleo, centro.clone().add(0, 0.3 + f * (ALTURA - 0.3), 0), t * 9f);
            for (LivingEntity v : vitimas) {
                if (t % 2 == 0) w.spawnParticle(Particle.SCULK_SOUL, v.getLocation().add(0, v.getHeight() / 2, 0), 1, 0.2, 0.2, 0.2, 0.01);
            }
            if (t % 8 == 0) w.playSound(centro, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.6f + t / 80f);
            w.spawnParticle(Particle.ENCHANT, centro.clone().add(0, 0.8, 0), 12, 0.6, 0.4, 0.6, 1.2);
        } else if (t <= GIRAR) {
            // Giram em volta do centro, cada vez mais rápido e mais perto.
            double f = (t - SUBIR) / (double) (GIRAR - SUBIR);
            giro += 0.04 + f * 0.22;
            double raio = 3.0 - f * 1.9;
            for (int k = 0; k < n; k++) {
                double ang = giro + k * Math.PI * 2 / n;
                double y = ALTURA - 1.0 + f * 1.0 + Math.sin(giro * 2 + k) * 0.35;
                Location l = centro.clone().add(Math.cos(ang) * raio, y, Math.sin(ang) * raio);
                mover(voando.get(k), l, t * 20f);
                if (t % 2 == 0) feixe(l, alvo);
                w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, new Particle.DustOptions(cor, 1.4f));
            }
            girarVitimas(f);
            mover(nucleo, alvo.clone().add(0, Math.sin(t * 0.2) * 0.1, 0), t * 15f);
            escala(nucleo, 0.6f + (float) f * 0.5f);
            anel(centro.clone().add(0, 0.15, 0), 3.2, t * 0.05, Particle.SOUL_FIRE_FLAME, 16);
            if (t % 3 == 0) w.spawnParticle(Particle.ENCHANT, alvo, 25, 0.3, 0.3, 0.3, 2);
            if (t % 10 == 0) w.playSound(centro, Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.6f + (float) f * 0.8f);
            if (t % 20 == 0) w.playSound(centro, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.5f + (float) f);
            if (t == 70 || t == 100 || t == 125 || t == 145) trovao();
            if (t >= 90 && t % 6 == 0) tremer(0.3 + f * 0.7);
        } else if (t <= SUGAR) {
            // Sugados para o centro.
            double f = suave((t - GIRAR) / (double) (SUGAR - GIRAR));
            giro += 0.3;
            double raio = 1.1 * (1 - f);
            for (int k = 0; k < n; k++) {
                double ang = giro + k * Math.PI * 2 / n;
                Location l = alvo.clone().add(Math.cos(ang) * raio, Math.sin(giro + k) * 0.2 * (1 - f), Math.sin(ang) * raio);
                mover(voando.get(k), l, t * 30f);
                escala(voando.get(k), 0.5f * (float) (1 - f) + 0.05f);
            }
            sugarVitimas(f);
            mover(nucleo, alvo, t * 30f);
            escala(nucleo, 1.1f + (float) f * 0.6f);
            w.spawnParticle(Particle.REVERSE_PORTAL, alvo, 40, 1.5, 1.5, 1.5, 0.05);
            w.spawnParticle(Particle.ELECTRIC_SPARK, alvo, 6, 0.4, 0.4, 0.4, 0.2);
            if (t % 4 == 0) tremer(1);
            if (t == GIRAR + 1) {
                w.playSound(centro, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.6f);
                w.playSound(centro, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.4f);
            }
        } else if (t == SUGAR + 1) {
            explodir(alvo);
        } else if (t < DURACAO) {
            // O resultado flutua, e um anel de fogo se espalha pelo chão.
            mover(nucleo, alvo.clone().add(0, Math.sin(t * 0.25) * 0.15, 0), t * 6f);
            double f = (t - SUGAR) / (double) (DURACAO - SUGAR);
            anel(centro.clone().add(0, 0.1, 0), 1 + f * 7, 0, Particle.SOUL_FIRE_FLAME, 40);
            w.spawnParticle(Particle.END_ROD, alvo, 3, 0.3, 0.3, 0.3, 0.02);
        } else {
            entregar();
        }
    }

    // ---------- Sacrifício ----------

    private void girarVitimas(double f) {
        int n = vitimas.size();
        for (int k = 0; k < n; k++) {
            LivingEntity v = vitimas.get(k);
            if (!v.isValid()) continue;
            double ang = -giro * 0.7 + k * Math.PI * 2 / Math.max(1, n);
            double raio = 2.4 - f * 0.8;
            Location l = centro.clone().add(Math.cos(ang) * raio, 0.4 + f * 1.6, Math.sin(ang) * raio);
            l.setDirection(centro.toVector().subtract(l.toVector()));
            v.teleport(l);
            if (t % 3 == 0) w.spawnParticle(Particle.SCULK_SOUL, v.getLocation().add(0, v.getHeight() / 2, 0), 1, 0.2, 0.2, 0.2, 0.02);
            if (t % 25 == 0) v.playHurtAnimation(0);
        }
    }

    private void sugarVitimas(double f) {
        Location alvo = centro.clone().add(0, ALTURA - 0.5, 0);
        for (int k = 0; k < vitimas.size(); k++) {
            LivingEntity v = vitimas.get(k);
            if (!v.isValid()) continue;
            Location l = v.getLocation();
            l.add(alvo.toVector().subtract(l.toVector()).multiply(0.15));
            v.teleport(l);
            AttributeInstance e = v.getAttribute(Attribute.SCALE);
            if (e != null) e.setBaseValue(Math.max(0.1, escalaVitimas.get(k) * (1 - f)));
            w.spawnParticle(Particle.SOUL, v.getLocation().add(0, 0.3, 0), 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void consumirVitimas() {
        for (LivingEntity v : vitimas) {
            if (!v.isValid()) continue;
            w.spawnParticle(Particle.SOUL, v.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
            v.remove();
        }
        vitimas.clear();
    }

    /** Os sacrificados voltam ao normal (altar quebrado no meio). */
    private void soltarVitimas() {
        for (int k = 0; k < vitimas.size(); k++) {
            LivingEntity v = vitimas.get(k);
            if (!v.isValid()) continue;
            if (v instanceof Mob m) m.setAI(true);
            v.setInvulnerable(false);
            v.setGravity(true);
            AttributeInstance e = v.getAttribute(Attribute.SCALE);
            if (e != null) e.setBaseValue(escalaVitimas.get(k));
        }
        vitimas.clear();
    }

    // ---------- Efeitos ----------

    private static double suave(double x) {
        return x * x * (3 - 2 * x);
    }

    private void mover(ItemDisplay d, Location l, float yaw) {
        if (!d.isValid()) return;
        l.setYaw(yaw % 360);
        d.teleport(l);
    }

    private void escala(ItemDisplay d, float e) {
        if (!d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(e, e, e), new AxisAngle4f()));
    }

    private void feixe(Location de, Location ate) {
        Vector v = ate.toVector().subtract(de.toVector());
        int passos = (int) Math.max(3, v.length() * 3);
        v.multiply(1.0 / passos);
        Location l = de.clone();
        Particle.DustOptions pd = new Particle.DustOptions(cor, 0.7f);
        for (int k = 0; k < passos; k++) {
            l.add(v);
            w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, pd);
        }
    }

    private void anel(Location c, double raio, double fase, Particle p, int pontos) {
        for (int k = 0; k < pontos; k++) {
            double a = fase + k * Math.PI * 2 / pontos;
            w.spawnParticle(p, c.clone().add(Math.cos(a) * raio, 0, Math.sin(a) * raio), 1, 0, 0, 0, 0);
        }
    }

    /** O céu escurece até a meia-noite nos primeiros 2 segundos (só para quem está assistindo). */
    private void escurecer() {
        if (t > SUBIR) return;
        long noite = 18000;
        long base = horaMundo % 24000;
        long alvo = noite >= base ? noite : noite + 24000;
        long hora = base + Math.round((alvo - base) * suave(t / (double) SUBIR));
        long dia = horaMundo - base;
        for (Player p : quemAssiste()) p.setPlayerTime(dia + hora, false);
    }

    private void trovao() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble(Math.PI * 2), d = r.nextDouble(5, 10);
        Location l = centro.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        l.setY(w.getHighestBlockYAt(l) + 1);
        w.strikeLightningEffect(l);
        w.playSound(centro, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.7f + r.nextFloat() * 0.3f);
    }

    private void tremer(double forca) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Player p : quemAssiste()) {
            if (p.getLocation().distanceSquared(centro) > 20 * 20) continue;
            if (r.nextDouble() < forca) p.sendHurtAnimation(r.nextFloat() * 360);
        }
        var chao = bloco.clone().add(0, -1, 0).getBlock();
        w.spawnParticle(Particle.BLOCK, centro.clone().add(0, -0.4, 0), (int) (20 * forca), 3, 0.1, 3, 0,
                (chao.getType().isAir() ? bloco.getBlock() : chao).getBlockData());
        if (r.nextDouble() < 0.4) w.playSound(centro, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
    }

    private void explodir(Location alvo) {
        for (ItemDisplay d : voando) d.remove();
        voando.clear();
        consumirVitimas();
        if (resultado != null) nucleo.setItemStack(resultado);
        escala(nucleo, 0.9f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 2, 0.2, 0.2, 0.2, 0);
        w.spawnParticle(Particle.FIREWORK, alvo, 120, 0.2, 0.2, 0.2, 0.45);
        w.spawnParticle(Particle.END_ROD, alvo, 80, 0.1, 0.1, 0.1, 0.35);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, alvo, 60, 0.4, 0.4, 0.4, 0.6);
        w.spawnParticle(Particle.SONIC_BOOM, alvo, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.DUST, alvo, 120, 1.8, 1.8, 1.8, 0, new Particle.DustOptions(cor, 2f));
        w.strikeLightningEffect(centro);
        w.playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
        w.playSound(centro, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.5f);
        w.playSound(centro, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        empurrar();
        Player p = Bukkit.getPlayer(dono);
        Title titulo = Title.title(Component.text("✦ " + receita.nome() + " ✦", receita.categoria().cor, TextDecoration.BOLD),
                Component.text(receita.descricao(), receita.categoria().cor),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2200), Duration.ofMillis(700)));
        if (p != null) p.showTitle(titulo);
        for (Player o : grupo) if (o.isOnline()) o.showTitle(titulo);
    }

    private void empurrar() {
        for (Player p : quemAssiste()) {
            if (p.getLocation().distanceSquared(centro) < 12 * 12) {
                p.sendHurtAnimation(0);
                Vector empurra = p.getLocation().toVector().subtract(centro.toVector()).setY(0);
                if (empurra.lengthSquared() > 0.01) p.setVelocity(empurra.normalize().multiply(0.6).setY(0.35));
            }
        }
    }

    private void entregar() {
        parar();
        Player p = Bukkit.getPlayer(dono);
        ReceitaRitual.Efeito efeito = receita.extra().efeito();
        if (efeito != null) {
            // Ritual no mundo: o efeito acontece e não sobra item.
            efeito.aplicar(altar.plugin(), bloco, p, grupo);
            altar.concluida(this, dono, receita);
            return;
        }
        if (receita.desperto() != null) {
            // Invocação maior: o núcleo se quebra e o chefe Desperto sai do altar.
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 80, 0.8, 1.2, 0.8, 0.05);
            altar.plugin().chefes().invocarDesperto(receita.desperto(), bloco, p);
            altar.concluida(this, dono, receita);
            return;
        }
        Location l = centro.clone().add(0, 1.2, 0);
        Item it = w.dropItem(l, resultado, i -> {
            i.setVelocity(new Vector());
            i.setGravity(true);
            i.setGlowing(true);
            i.setUnlimitedLifetime(true);
            i.setPickupDelay(10);
            i.setOwner(dono);
        });
        w.spawnParticle(Particle.END_ROD, it.getLocation(), 20, 0.2, 0.2, 0.2, 0.05);
        altar.concluida(this, dono, receita);
    }

    /** O altar sumiu no meio: os itens voltam para o chão e os sacrificados são soltos. */
    void interromper() {
        parar();
        soltarVitimas();
        Location l = centro.clone().add(0, 0.5, 0);
        w.dropItemNaturally(l, catalisador);
        for (ItemStack s : gastos) w.dropItemNaturally(l, s);
        for (ItemStack s : doGrupo) w.dropItemNaturally(l, s);
        w.spawnParticle(Particle.LARGE_SMOKE, l, 40, 0.6, 0.6, 0.6, 0.05);
        w.playSound(l, Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.6f);
        altar.concluida(this, null, null);
    }

    /**
     * O ritual saiu errado: os ingredientes e o sacrifício se perdem e uma Aberração sai do altar.
     * Equipamento, relíquia e Selo voltam (só eles).
     */
    private void falhar(String motivo) {
        List<Player> viram = quemAssiste();
        parar();
        consumirVitimas();
        Location l = centro.clone().add(0, 0.5, 0);
        if (receita.devolveCatalisadorAoFalhar()) w.dropItemNaturally(l, catalisador);
        w.spawnParticle(Particle.SQUID_INK, l, 120, 1.5, 1.5, 1.5, 0.1);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, l, 1);
        w.strikeLightningEffect(l);
        w.playSound(l, Sound.ENTITY_WITHER_SPAWN, 1f, 1.4f);
        w.playSound(l, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
        Evoker aberracao = w.spawn(l.clone().add(0, 0.5, 0), Evoker.class, e -> {
            e.customName(Component.text("☠ Aberração do Ritual", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
            e.setPersistent(false);
            e.setRemoveWhenFarAway(true);
            AttributeInstance v = e.getAttribute(Attribute.MAX_HEALTH);
            if (v != null) { v.setBaseValue(60); e.setHealth(60); }
            AttributeInstance s = e.getAttribute(Attribute.SCALE);
            if (s != null) s.setBaseValue(1.3);
        });
        for (int k = 0; k < 3; k++) {
            w.spawn(l.clone().add(ThreadLocalRandom.current().nextDouble(-1, 1), 1.5, ThreadLocalRandom.current().nextDouble(-1, 1)), Vex.class, v -> {
                v.setPersistent(false);
                v.setLimitedLifetimeTicks(20 * 30);
                v.setSummoner(aberracao);
            });
        }
        Component msg = Component.text("✖ O ritual " + receita.nome() + " saiu errado (" + motivo + ")! Uma Aberração saiu do altar.",
                NamedTextColor.DARK_RED, TextDecoration.BOLD);
        for (Player p : viram) p.sendMessage(msg);
        Player p = Bukkit.getPlayer(dono);
        if (p != null && !viram.contains(p)) p.sendMessage(msg);
        altar.concluida(this, null, null);
    }

    /** O servidor está desligando: termina na hora, sem animação. */
    void terminarAgora() {
        if (tarefa == null || tarefa.isCancelled()) return;
        parar();
        soltarVitimas();
        // Sem chefe nem efeito com o servidor desligando: quem acendeu recebe o catalisador de volta.
        boolean semItem = receita.desperto() != null || receita.extra().efeito() != null || resultado == null;
        w.dropItem(centro.clone().add(0, 0.5, 0), semItem ? catalisador : resultado);
    }

    private void parar() {
        if (tarefa != null) tarefa.cancel();
        for (ItemDisplay d : voando) d.remove();
        voando.clear();
        if (nucleo != null) nucleo.remove();
        for (Player p : quemAssiste()) {
            p.resetPlayerTime();
            p.resetPlayerWeather();
        }
        assistindo.clear();
    }
}
