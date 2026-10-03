package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Allay;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import static br.rpgatributos.arcano.Conjuracao.*;

/**
 * As formas novas (Raio, Sopro, Chuva, Invocação, Encantar arma), os modificadores
 * (Dividir, Teleguiado, Ricochete, Ampliar, Persistente, Mina, Canalizar, Eco) e
 * os poderes que ficam no mundo por um tempo (minas, invocações, armas encantadas).
 */
final class Feiticos {

    /** Prioridade do elemento que decide o tipo de elemental invocado. */
    private static final List<Essencia> PRIORIDADE_INVOCACAO = List.of(
            Essencia.VIDA, Essencia.TERRA, Essencia.FOGO, Essencia.GELO, Essencia.ENERGIA,
            Essencia.SOMBRA, Essencia.VAZIO, Essencia.AGUA, Essencia.VENTO, Essencia.NATUREZA);
    private static final int MAX_MINAS = 3;
    private static final int MAX_INVOCACOES = 3;
    private static final int SOLTOU_TICKS = 8;

    /** Tudo o que uma magia precisa para acontecer. */
    static final class Lance {
        final Player p;
        final Perfil pf;
        final Set<Essencia> es;
        final double pot;
        final Set<Modificador> mods;
        final Estilo estilo;

        Lance(Player p, Perfil pf, Set<Essencia> es, double pot, Set<Modificador> mods) {
            this.p = p;
            this.pf = pf;
            this.es = es;
            this.pot = pot;
            this.mods = mods;
            this.estilo = pf.estilo;
        }

        boolean tem(Modificador m) { return mods.contains(m); }

        Lance comPot(double novo) { return new Lance(p, pf, es, novo, mods); }
    }

    /** Arma carregada com o elemento por uma magia de "Encantar arma". */
    static final class ArmaEncantada {
        final Set<Essencia> es;
        final double pot;
        final Receita receita;
        int golpes;
        final int ate;

        ArmaEncantada(Set<Essencia> es, double pot, Receita receita, int golpes, int ate) {
            this.es = es;
            this.pot = pot;
            this.receita = receita;
            this.golpes = golpes;
            this.ate = ate;
        }
    }

    /** Clique segurado: canalizando (carregando) ou mantendo um raio. */
    private static final class Canal {
        int inicio;
        int ultimo;
        boolean raio;
        BukkitRunnable tarefa;
    }

    private record Mina(UUID dono, Location local, Set<Essencia> es, double pot, long expira) {}

    private record Invocado(UUID entidade, UUID dono, Set<Essencia> es, double pot, int ate, Receita receita) {}

    private final RPGAtributos plugin;
    private final Conjuracao conj;
    private final NamespacedKey kInvocacao;
    private final Map<UUID, Canal> canais = new HashMap<>();
    private final List<Mina> minas = new ArrayList<>();
    private final Map<UUID, Invocado> invocados = new HashMap<>();
    private int ciclo;

    Feiticos(RPGAtributos plugin, Conjuracao conj) {
        this.plugin = plugin;
        this.conj = conj;
        this.kInvocacao = new NamespacedKey(plugin, "invocacao_dono");
    }

    private static int agora() { return Bukkit.getCurrentTick(); }

    /** Nome da variante que o elemento ganha na maestria 5. */
    static String variante(Essencia e) {
        return switch (e) {
            case FOGO -> "Chama Azul";
            case GELO -> "Gelo Eterno";
            case VENTO -> "Vendaval";
            case AGUA -> "Maré Curativa";
            case VIDA -> "Bênção";
            case TERRA -> "Pele de Rocha";
            case SOMBRA -> "Abismo";
            case VAZIO -> "Gravidade";
            case ENERGIA -> "Sobrecarga";
            case NATUREZA -> "Toxina";
        };
    }

    /** O que a variante faz (para o menu de maestria). */
    static String descricaoVariante(Essencia e) {
        return switch (e) {
            case FOGO -> "o fogo queima 50% mais e dá dano extra";
            case GELO -> "lentidão mais forte e congela por mais tempo";
            case VENTO -> "empurra 40% mais longe e para cima";
            case AGUA -> "cada acerto também cura você";
            case VIDA -> "as curas dão absorção";
            case TERRA -> "cada acerto te dá Resistência por 3s";
            case SOMBRA -> "Escuridão no alvo e o dobro de dreno";
            case VAZIO -> "puxa 40% mais forte e dá dano extra";
            case ENERGIA -> "30% de chance do choque saltar para outro inimigo";
            case NATUREZA -> "veneno nível II";
        };
    }

    // =====================================================================
    //  Clique segurado (Canalizar e Raio)
    // =====================================================================

    /** O jogador está segurando o clique de um raio ou canalização? Então só renova e não lança outra. */
    boolean continuar(Player p) {
        Canal c = canais.get(p.getUniqueId());
        if (c == null) return false;
        c.ultimo = agora();
        return true;
    }

    /** Carrega enquanto o clique estiver segurado; ao soltar, lança com até o dobro da força. */
    void canalizar(Player p, double pot, DoubleConsumer soltar) {
        Canal c = new Canal();
        c.inicio = agora();
        c.ultimo = c.inicio;
        canais.put(p.getUniqueId(), c);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.8f);
        c.tarefa = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || p.isDead()) { parar(p, c); return; }
                int t = agora();
                double carga = Math.min(1, (t - c.inicio) / 40.0);
                int cheios = (int) Math.round(carga * 10);
                p.sendActionBar(Component.text("✦ Canalizando ", Arcano.COR)
                        .append(Component.text("▮".repeat(cheios), NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("▮".repeat(10 - cheios), NamedTextColor.DARK_GRAY))
                        .append(Component.text(" " + Math.round(carga * 100) + "%", NamedTextColor.WHITE)));
                Location l = p.getLocation().add(0, 1, 0);
                double a = t * 0.5;
                p.getWorld().spawnParticle(Particle.ENCHANT, l.clone().add(Math.cos(a), 0.3, Math.sin(a)), 3, 0, 0, 0, 0.5);
                if (carga >= 1 && (t - c.inicio) % 10 == 0) p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 2f);
                if (t - c.ultimo > SOLTOU_TICKS || t - c.inicio > 80) {
                    parar(p, c);
                    soltar.accept(pot * (1 + carga));
                }
            }
        };
        c.tarefa.runTaskTimer(plugin, 2L, 2L);
    }

    private void parar(Player p, Canal c) {
        if (c.tarefa != null) c.tarefa.cancel();
        canais.remove(p.getUniqueId(), c);
    }

    // =====================================================================
    //  Executar uma magia comum (sem receita secreta)
    // =====================================================================

    boolean executar(Lance l, Forma forma) {
        return switch (forma) {
            case TOQUE -> toque(l);
            case PROJETIL -> projetil(l);
            case AURA -> aura(l);
            case CORPO -> conj.corpo(l.p, l.es, l.pot);
            case CRIACAO -> criacao(l);
            case SOPRO -> sopro(l, 1, null, l.tem(Modificador.AMPLIAR) ? 9.6 : 6);
            case CHUVA -> chuva(l, 1, null, 60);
            case INVOCACAO -> invocar(l, null);
            case ARMA -> encantarArma(l, null);
            case RAIO, PROIBIDA -> false; // o raio vai por raio(); as proibidas são sempre receita
        };
    }

    /** Eco: a mesma magia de novo, 1s depois, com metade da força. */
    void eco(Lance l, Forma forma) {
        Set<Modificador> sem = EnumSet.noneOf(Modificador.class);
        sem.addAll(l.mods);
        sem.remove(Modificador.ECO);
        sem.remove(Modificador.CANALIZAR);
        Lance eco = new Lance(l.p, l.pf, l.es, l.pot * 0.5, sem);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!l.p.isOnline() || l.p.isDead()) return;
                l.p.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, l.p.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.02);
                l.p.getWorld().playSound(l.p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.3f, 2f);
                executar(eco, forma);
            }
        }.runTaskLater(plugin, 20L);
    }

    // =====================================================================
    //  Toque, Projétil, Aura e Criação (com modificadores)
    // =====================================================================

    private boolean toque(Lance l) {
        Player p = l.p;
        RayTraceResult r = conj.mirar(p, 5);
        if (r == null) {
            falhar(p, "Nada ao alcance (5 blocos).");
            return false;
        }
        Location alvoLoc = r.getHitPosition().toLocation(p.getWorld());
        feixe(p.getEyeLocation(), alvoLoc, l.es);
        if (r.getHitEntity() instanceof LivingEntity alvo) {
            if (inimigo(p, alvo)) {
                conj.atingir(p, alvo, l.es, l.pot, p.getLocation());
                if (l.tem(Modificador.RICOCHETE)) ricochete(l, alvo, l.pot * 0.7, 3);
            } else {
                conj.ajudar(alvo, l.es, l.pot);
            }
            brilho(meio(alvo), l.es, 10, l.estilo);
        } else if (r.getHitBlock() != null) {
            if (l.tem(Modificador.MINA)) {
                colocarMina(l, r.getHitBlock().getRelative(r.getHitBlockFace() == null ? BlockFace.UP : r.getHitBlockFace())
                        .getLocation().toCenterLocation().subtract(0, 0.4, 0), l.pot);
                return true;
            }
            conj.bloco(p, r.getHitBlock(), r.getHitBlockFace(), l.es, l.pot);
            if (l.es.contains(Essencia.VAZIO)) conj.teleportarPara(p, r.getHitBlock());
            brilho(alvoLoc, l.es, 10, l.estilo);
        }
        if (l.tem(Modificador.PERSISTENTE)) conj.zona(p, alvoLoc, 3, 100, l.es, l.pot * 0.3);
        return true;
    }

    private boolean projetil(Lance l) {
        boolean dividir = l.tem(Modificador.DIVIDIR);
        double pot = dividir ? l.pot * 0.6 : l.pot;
        Vector base = l.p.getEyeLocation().getDirection().normalize();
        int n = dividir ? 3 : 1;
        for (int i = 0; i < n; i++) {
            Vector dir = dividir ? base.clone().rotateAroundY(Math.toRadians((i - 1) * 14)) : base.clone();
            boolean primeiro = i == (n - 1) / 2;
            voar(l, l.p.getEyeLocation(), dir, 1.4, 30, l.tem(Modificador.TELEGUIADO), imp -> impacto(l, imp, pot, primeiro));
        }
        l.p.getWorld().playSound(l.p.getEyeLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.6f);
        return true;
    }

    private record Acerto(Location local, LivingEntity entidade, Block bloco, BlockFace face) {}

    private void impacto(Lance l, Acerto imp, double pot, boolean podeTeleportar) {
        Player p = l.p;
        if (l.tem(Modificador.MINA) && imp.entidade() == null) {
            colocarMina(l, imp.local(), pot);
            return;
        }
        if (imp.entidade() != null) {
            conj.atingir(p, imp.entidade(), l.es, pot, imp.local());
            if (l.tem(Modificador.RICOCHETE)) ricochete(l, imp.entidade(), pot * 0.7, 3);
        }
        if (imp.bloco() != null) {
            conj.bloco(p, imp.bloco(), imp.face(), l.es, pot);
            if (podeTeleportar && l.es.contains(Essencia.VAZIO)) conj.teleportarPara(p, imp.bloco());
        }
        double raio = l.tem(Modificador.AMPLIAR) ? 3.2 : 2;
        for (LivingEntity e : perto(imp.local(), raio)) {
            if (e != imp.entidade() && inimigo(p, e)) conj.atingir(p, e, l.es, pot * 0.5, imp.local());
        }
        if (l.tem(Modificador.PERSISTENTE)) conj.zona(p, imp.local(), 3, 100, l.es, pot * 0.3);
        brilho(imp.local(), l.es, 25, l.estilo);
    }

    /** Projétil de partículas; com {@code teleguiado}, vira para o inimigo mais perto da mira. */
    private void voar(Lance l, Location origem, Vector direcao, double velocidade, int maxTicks, boolean teleguiado,
                      Consumer<Acerto> aoAcertar) {
        Player p = l.p;
        World w = origem.getWorld();
        Location pos = origem.clone();
        Vector dir = direcao.clone().normalize();
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (teleguiado && t > 2) {
                    LivingEntity alvo = alvoNaFrente(p, pos, dir, 16);
                    if (alvo != null) {
                        Vector para = meio(alvo).toVector().subtract(pos.toVector()).normalize();
                        dir.multiply(0.7).add(para.multiply(0.3)).normalize();
                    }
                }
                RayTraceResult r = w.rayTrace(pos, dir, velocidade, FluidCollisionMode.NEVER, true, 0.35,
                        e -> e instanceof LivingEntity && e != p && !(e instanceof ArmorStand) && e.isValid());
                if (r != null) {
                    aoAcertar.accept(new Acerto(r.getHitPosition().toLocation(w), r.getHitEntity() instanceof LivingEntity le ? le : null,
                            r.getHitBlock(), r.getHitBlockFace()));
                    cancel();
                    return;
                }
                pos.add(dir.clone().multiply(velocidade));
                for (Essencia e : l.es) w.spawnParticle(Particle.DUST, pos, 3, 0.08, 0.08, 0.08, 0, e.poeira(1.3f));
                if (l.estilo.particula() != null && t % 2 == 0) w.spawnParticle(l.estilo.particula(), pos, 1, 0.05, 0.05, 0.05, 0);
                if (++t >= maxTicks || !pos.isChunkLoaded()) {
                    aoAcertar.accept(new Acerto(pos.clone(), null, null, null));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Inimigo mais perto da linha de mira (na frente, até {@code alcance} blocos). */
    private static LivingEntity alvoNaFrente(Player p, Location de, Vector dir, double alcance) {
        LivingEntity melhor = null;
        double melhorNota = 0;
        for (LivingEntity e : perto(de, alcance)) {
            if (!inimigo(p, e)) continue;
            Vector v = meio(e).toVector().subtract(de.toVector());
            double dist = v.length();
            if (dist < 0.5) continue;
            double cos = v.normalize().dot(dir);
            if (cos < 0.6) continue;
            double nota = cos / dist;
            if (nota > melhorNota) {
                melhorNota = nota;
                melhor = e;
            }
        }
        return melhor;
    }

    /** O golpe salta de inimigo em inimigo. */
    private void ricochete(Lance l, LivingEntity de, double pot, int saltos) {
        Set<UUID> atingidos = new HashSet<>();
        atingidos.add(de.getUniqueId());
        new BukkitRunnable() {
            LivingEntity atual = de;
            double forca = pot;
            int restam = saltos;
            @Override
            public void run() {
                if (restam <= 0 || !l.p.isOnline() || !atual.isValid()) { cancel(); return; }
                LivingEntity prox = null;
                double melhor = Double.MAX_VALUE;
                for (LivingEntity e : perto(atual.getLocation(), 6)) {
                    if (atingidos.contains(e.getUniqueId()) || !inimigo(l.p, e)) continue;
                    double d = e.getLocation().distanceSquared(atual.getLocation());
                    if (d < melhor) { melhor = d; prox = e; }
                }
                if (prox == null) { cancel(); return; }
                feixe(meio(atual), meio(prox), l.es);
                conj.atingir(l.p, prox, l.es, forca, atual.getLocation());
                atingidos.add(prox.getUniqueId());
                atual.getWorld().playSound(prox.getLocation(), Sound.ENTITY_SLIME_JUMP_SMALL, 0.8f, 1.6f);
                atual = prox;
                forca *= 0.7;
                restam--;
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

    private boolean aura(Lance l) {
        Player p = l.p;
        double raio = l.tem(Modificador.AMPLIAR) ? 8 : 5;
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || t > 120) { cancel(); return; }
                anel(p.getLocation().add(0, 0.2, 0), raio, l.es);
                if (t % 20 == 0) {
                    for (LivingEntity e : perto(p.getLocation(), raio)) {
                        if (inimigo(p, e)) conj.atingir(p, e, l.es, l.pot * 0.5, p.getLocation());
                        else if (e != p) conj.ajudar(e, l.es, l.pot * 0.5);
                    }
                    if (l.es.contains(Essencia.VIDA)) curar(p, l.pot * 0.75);
                    int r = (int) raio - 1;
                    for (int i = 0; i < 10; i++) {
                        Block b = p.getLocation().getBlock().getRelative(rnd().nextInt(-r, r + 1), rnd().nextInt(-1, 2), rnd().nextInt(-r, r + 1));
                        conj.bloco(p, b, BlockFace.UP, l.es, l.pot * 0.5);
                    }
                    if (l.estilo.particula() != null) p.getWorld().spawnParticle(l.estilo.particula(), p.getLocation().add(0, 1, 0), 6, raio / 2, 0.5, raio / 2, 0);
                }
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
        return true;
    }

    private boolean criacao(Lance l) {
        if (l.tem(Modificador.MINA)) {
            RayTraceResult r = l.p.getWorld().rayTraceBlocks(l.p.getEyeLocation(), l.p.getEyeLocation().getDirection(), 6,
                    FluidCollisionMode.NEVER, true);
            if (r == null || r.getHitBlock() == null || r.getHitBlockFace() == null) {
                falhar(l.p, "Olhe para um bloco próximo (até 6 blocos).");
                return false;
            }
            colocarMina(l, r.getHitBlock().getRelative(r.getHitBlockFace()).getLocation().toCenterLocation().subtract(0, 0.4, 0), l.pot);
            return true;
        }
        return conj.criacao(l.p, l.es, l.pot, l.tem(Modificador.AMPLIAR) ? 1.6 : 1);
    }

    // =====================================================================
    //  Sopro
    // =====================================================================

    /**
     * Cone na frente do jogador. {@code mult} multiplica o dano; {@code extra} roda em cada
     * inimigo atingido (receitas).
     */
    boolean sopro(Lance l, double mult, Consumer<LivingEntity> extra, double alcance) {
        Player p = l.p;
        World w = p.getWorld();
        Location olho = p.getEyeLocation();
        Vector dir = olho.getDirection().normalize();
        double cosMax = Math.cos(Math.toRadians(35));
        for (LivingEntity e : perto(olho, alcance + 1)) {
            if (e == p) continue;
            Vector v = meio(e).toVector().subtract(olho.toVector());
            if (v.length() > alcance || v.normalize().dot(dir) < cosMax) continue;
            if (inimigo(p, e)) {
                conj.atingir(p, e, l.es, l.pot * 0.7 * mult, olho);
                if (extra != null) extra.accept(e);
            } else {
                conj.ajudar(e, l.es, l.pot * 0.5);
            }
        }
        for (int i = 0; i < 8; i++) {
            Vector d = espalhar(dir, 30).multiply(rnd().nextDouble(1.5, alcance));
            Block b = olho.clone().add(d).getBlock();
            conj.bloco(p, b, BlockFace.UP, l.es, l.pot * 0.4);
        }
        if (l.tem(Modificador.PERSISTENTE)) conj.zona(p, olho.clone().add(dir.clone().multiply(alcance * 0.6)).subtract(0, 1, 0), 3, 100, l.es, l.pot * 0.3);
        List<Essencia> lista = new ArrayList<>(l.es);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t++ > 7) { cancel(); return; }
                for (int i = 0; i < 14; i++) {
                    Vector d = espalhar(dir, 32).multiply(rnd().nextDouble(0.5, alcance) * (t / 8.0 + 0.3));
                    Location pt = olho.clone().add(d);
                    Essencia e = lista.get(i % lista.size());
                    w.spawnParticle(Particle.DUST, pt, 1, 0.1, 0.1, 0.1, 0, e.poeira(1.5f));
                    if (i % 3 == 0) w.spawnParticle(extraDe(e), pt, 1, 0.05, 0.05, 0.05, 0.01);
                }
                if (l.estilo.particula() != null) w.spawnParticle(l.estilo.particula(), olho.clone().add(dir.clone().multiply(alcance / 2)), 3, 0.6, 0.4, 0.6, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        w.playSound(olho, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.7f, 1.4f);
        return true;
    }

    /** Uma direção aleatória a até {@code graus} da direção dada. */
    private static Vector espalhar(Vector dir, double graus) {
        Vector v = dir.clone();
        v.rotateAroundY(Math.toRadians(rnd().nextDouble(-graus, graus)));
        Vector eixo = v.clone().crossProduct(new Vector(0, 1, 0));
        if (eixo.lengthSquared() > 1e-4) v.rotateAroundAxis(eixo.normalize(), Math.toRadians(rnd().nextDouble(-graus * 0.6, graus * 0.6)));
        return v.normalize();
    }

    static Particle extraDe(Essencia e) {
        return switch (e) {
            case FOGO -> Particle.FLAME;
            case GELO -> Particle.SNOWFLAKE;
            case VENTO -> Particle.CLOUD;
            case AGUA -> Particle.SPLASH;
            case VIDA -> Particle.HEART;
            case TERRA -> Particle.CRIT;
            case SOMBRA -> Particle.SQUID_INK;
            case VAZIO -> Particle.PORTAL;
            case ENERGIA -> Particle.ELECTRIC_SPARK;
            case NATUREZA -> Particle.HAPPY_VILLAGER;
        };
    }

    // =====================================================================
    //  Chuva
    // =====================================================================

    /** Área longe do jogador onde a magia cai do céu. {@code pulso} roda a cada meio segundo (receitas). */
    boolean chuva(Lance l, double mult, Consumer<Location> pulso, int duracao) {
        Player p = l.p;
        RayTraceResult r = p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), 30,
                FluidCollisionMode.NEVER, true, 0.5, e -> e instanceof LivingEntity && e != p);
        if (r == null) {
            falhar(p, "Olhe para um lugar até 30 blocos.");
            return false;
        }
        Location centro = r.getHitPosition().toLocation(p.getWorld());
        double raio = l.tem(Modificador.AMPLIAR) ? 6.4 : 4;
        int dur = l.tem(Modificador.PERSISTENTE) ? (int) (duracao * 1.7) : duracao;
        World w = centro.getWorld();
        List<Essencia> lista = new ArrayList<>(l.es);
        w.playSound(centro, Sound.WEATHER_RAIN_ABOVE, 1f, 1.2f);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > dur || !centro.isChunkLoaded()) { cancel(); return; }
                for (int i = 0; i < 10; i++) {
                    double a = rnd().nextDouble(Math.PI * 2), d = Math.sqrt(rnd().nextDouble()) * raio;
                    Location pt = centro.clone().add(Math.cos(a) * d, rnd().nextDouble(0, 7), Math.sin(a) * d);
                    Essencia e = lista.get(i % lista.size());
                    w.spawnParticle(Particle.DUST, pt, 1, 0, 0.3, 0, 0, e.poeira(1.1f));
                    if (i % 4 == 0) w.spawnParticle(extraDe(e), pt.clone().subtract(0, pt.getY() - centro.getY(), 0), 1, 0.1, 0, 0.1, 0);
                }
                if (t % 10 == 0) {
                    anel(centro.clone().add(0, 0.1, 0), raio, l.es);
                    for (LivingEntity e : perto(centro, raio)) {
                        if (p.isOnline() && inimigo(p, e)) conj.atingir(p, e, l.es, l.pot * 0.35 * mult, centro);
                        else conj.ajudar(e, l.es, l.pot * 0.35);
                    }
                    if (pulso != null) pulso.accept(centro);
                }
                if (t % 20 == 0) {
                    int rr = (int) raio;
                    for (int i = 0; i < 3; i++) {
                        Block b = centro.getBlock().getRelative(rnd().nextInt(-rr, rr + 1), 0, rnd().nextInt(-rr, rr + 1));
                        conj.bloco(p, b, BlockFace.UP, l.es, l.pot * 0.3);
                    }
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        return true;
    }

    // =====================================================================
    //  Raio (feixe contínuo)
    // =====================================================================

    /**
     * Feixe enquanto o clique estiver segurado (ou {@code automatico}: 3 segundos, sem gastar mana,
     * usado pelos pergaminhos). Gasta um pouco de mana a cada pulso.
     */
    void raio(Lance l, Receita receita, double custoPulso, boolean automatico, Runnable aoFim) {
        Player p = l.p;
        Canal c = new Canal();
        c.raio = true;
        c.inicio = agora();
        c.ultimo = c.inicio;
        if (!automatico) canais.put(p.getUniqueId(), c);
        double alcance = l.tem(Modificador.AMPLIAR) ? 22 : 14;
        double mult = receita == Receita.RAIO_SOLAR ? 1.8 : 1;
        Map<UUID, Integer> congelando = new HashMap<>();
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        c.tarefa = new BukkitRunnable() {
            int pulso = 0;
            @Override
            public void run() {
                int t = agora();
                boolean acabou = !p.isOnline() || p.isDead()
                        || (automatico ? t - c.inicio > 60 : t - c.ultimo > SOLTOU_TICKS || t - c.inicio > 100);
                if (!acabou && !automatico && custoPulso > 0) {
                    if (l.pf.mana < custoPulso) {
                        acabou = true;
                        p.sendActionBar(Component.text("Sem mana para manter o raio.", NamedTextColor.RED));
                    }
                }
                if (acabou) {
                    cancel();
                    canais.remove(p.getUniqueId(), c);
                    if (aoFim != null) aoFim.run();
                    return;
                }
                Location olho = p.getEyeLocation();
                Vector dir = olho.getDirection();
                RayTraceResult r = p.getWorld().rayTrace(olho, dir, alcance, FluidCollisionMode.NEVER, true, 0.3,
                        e -> e instanceof LivingEntity && e != p && !(e instanceof ArmorStand) && e.isValid());
                Location fim = r != null ? r.getHitPosition().toLocation(p.getWorld()) : olho.clone().add(dir.clone().multiply(alcance));
                feixe(olho.clone().add(dir.clone().multiply(0.6)), fim, l.es);
                if (l.estilo.particula() != null) p.getWorld().spawnParticle(l.estilo.particula(), fim, 2, 0.1, 0.1, 0.1, 0);
                if (pulso % 2 == 0) {
                    if (!automatico && custoPulso > 0) plugin.arcano().gastarMana(p, l.pf, custoPulso);
                    if (r != null && r.getHitEntity() instanceof LivingEntity alvo && inimigo(p, alvo)) {
                        conj.atingir(p, alvo, l.es, l.pot * 0.3 * mult, olho);
                        brilho(meio(alvo), l.es, 4, l.estilo);
                        if (receita == Receita.RAIO_SOLAR) alvo.setFireTicks(Math.max(alvo.getFireTicks(), 60));
                        if (receita == Receita.DRENO_DE_ALMA) {
                            curar(p, 0.5 * l.pot);
                            feixeParticula(meio(alvo), p.getLocation().add(0, 1, 0), Particle.DAMAGE_INDICATOR);
                        }
                        if (receita == Receita.RAIO_CONGELANTE) {
                            int n = congelando.merge(alvo.getUniqueId(), 1, Integer::sum);
                            efeito(alvo, PotionEffectType.SLOWNESS, 40, Math.min(5, n / 2));
                            if (n == 8) {
                                efeito(alvo, PotionEffectType.SLOWNESS, 60, 6);
                                alvo.setFreezeTicks(alvo.getMaxFreezeTicks() + 80);
                                p.getWorld().spawnParticle(Particle.BLOCK, meio(alvo), 30, 0.4, 0.5, 0.4, Material.ICE.createBlockData());
                                p.sendActionBar(Component.text("✦ Congelado!", NamedTextColor.AQUA, TextDecoration.BOLD));
                            }
                        }
                        if (l.tem(Modificador.RICOCHETE) && pulso % 6 == 0) ricochete(l, alvo, l.pot * 0.2, 2);
                    } else if (r != null && r.getHitBlock() != null && pulso % 6 == 0) {
                        conj.bloco(p, r.getHitBlock(), r.getHitBlockFace(), l.es, l.pot * 0.3);
                    }
                    if (pulso % 8 == 0) p.getWorld().playSound(olho, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2f);
                }
                pulso++;
            }
        };
        c.tarefa.runTaskTimer(plugin, 0L, 2L);
    }

    // =====================================================================
    //  Minas
    // =====================================================================

    private void colocarMina(Lance l, Location onde, double pot) {
        long agora = System.currentTimeMillis();
        List<Mina> minhas = minas.stream().filter(m -> m.dono().equals(l.p.getUniqueId())).toList();
        if (minhas.size() >= MAX_MINAS) minas.remove(minhas.getFirst());
        minas.add(new Mina(l.p.getUniqueId(), onde.clone(), EnumSet.copyOf(l.es), pot, agora + 60_000));
        onde.getWorld().playSound(onde, Sound.BLOCK_TRIPWIRE_ATTACH, 1f, 0.8f);
        brilho(onde, l.es, 8, l.estilo);
        l.p.sendActionBar(Component.text("✦ Mina armada (" + Math.min(MAX_MINAS, minhas.size() + 1) + "/" + MAX_MINAS + ")", Arcano.COR));
    }

    private void tickMinas() {
        long agora = System.currentTimeMillis();
        Iterator<Mina> it = minas.iterator();
        while (it.hasNext()) {
            Mina m = it.next();
            Player dono = Bukkit.getPlayer(m.dono());
            if (dono == null || agora > m.expira() || !m.local().isChunkLoaded()) {
                it.remove();
                continue;
            }
            World w = m.local().getWorld();
            if (ciclo % 4 == 0) {
                for (Essencia e : m.es()) w.spawnParticle(Particle.DUST, m.local(), 2, 0.25, 0.02, 0.25, 0, e.poeira(0.8f));
            }
            boolean ativou = false;
            for (LivingEntity e : perto(m.local(), 1.6)) {
                if (inimigo(dono, e)) { ativou = true; break; }
            }
            if (!ativou) continue;
            it.remove();
            for (LivingEntity e : perto(m.local(), 2.8)) {
                if (inimigo(dono, e)) conj.atingir(dono, e, m.es(), m.pot() * 1.2, m.local());
            }
            brilho(m.local().clone().add(0, 0.5, 0), m.es(), 40);
            w.spawnParticle(Particle.EXPLOSION, m.local(), 1);
            w.playSound(m.local(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
        }
    }

    // =====================================================================
    //  Invocação
    // =====================================================================

    boolean invocar(Lance l, Receita receita) {
        Player p = l.p;
        Location onde = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(1.5)).add(0, 0.5, 0);
        if (!onde.getBlock().isPassable()) onde = p.getLocation().add(0, 0.5, 0);
        Essencia principal = PRIORIDADE_INVOCACAO.stream().filter(l.es::contains).findFirst().orElse(Essencia.VAZIO);
        int duracao = (int) Math.min(600, 400 * Math.min(1.5, l.pot));
        int qtd = 1;
        if (receita == Receita.GOLEM_DE_PEDRA) { principal = Essencia.TERRA; duracao = 1200; }
        if (receita == Receita.SOMBRAS_GEMEAS) { principal = Essencia.SOMBRA; duracao = 600; qtd = 3; }
        if (receita == Receita.ESPIRITO_DA_FLORESTA) { principal = Essencia.VIDA; duracao = 900; }
        World w = onde.getWorld();
        for (int i = 0; i < qtd; i++) {
            LivingEntity ent;
            String nome;
            if (principal == Essencia.VIDA) {
                Allay a = w.spawn(onde, Allay.class);
                a.setCanPickupItems(false);
                a.setCanDuplicate(false);
                ent = a;
                nome = receita == Receita.ESPIRITO_DA_FLORESTA ? "Espírito da Floresta" : "Espírito da Vida";
                vida(a, 20 + 10 * l.pot);
            } else if (principal == Essencia.TERRA) {
                IronGolem g = w.spawn(onde, IronGolem.class);
                g.setPlayerCreated(true);
                boolean grande = receita == Receita.GOLEM_DE_PEDRA;
                AttributeInstance escala = g.getAttribute(Attribute.SCALE);
                if (escala != null) escala.setBaseValue(grande ? 1.3 : 0.7);
                vida(g, Math.min(300, (grande ? 100 : 40) * l.pot));
                ent = g;
                nome = grande ? "Golem de Pedra" : "Elemental de Terra";
            } else {
                Vex v = w.spawn(onde.clone().add(rnd().nextDouble(-0.6, 0.6), 0.5, rnd().nextDouble(-0.6, 0.6)), Vex.class);
                v.setLimitedLifetime(false);
                vida(v, 14 + 6 * l.pot);
                ent = v;
                nome = receita == Receita.SOMBRAS_GEMEAS ? "Sombra Gêmea" : "Elemental de " + principal.nome();
            }
            ent.customName(Component.text("✦ " + nome, principal.cor()).append(Component.text(" de " + p.getName(), NamedTextColor.GRAY)));
            ent.setCustomNameVisible(true);
            ent.setPersistent(false);
            ent.getPersistentDataContainer().set(kInvocacao, PersistentDataType.STRING, p.getUniqueId().toString());
            invocados.put(ent.getUniqueId(), new Invocado(ent.getUniqueId(), p.getUniqueId(), EnumSet.copyOf(l.es), l.pot,
                    agora() + duracao, receita));
            w.spawnParticle(Particle.DUST, ent.getLocation().add(0, 0.5, 0), 30, 0.4, 0.5, 0.4, 0, principal.poeira(1.6f));
        }
        // Limite por jogador: os mais antigos somem.
        List<Invocado> meus = new ArrayList<>(invocados.values().stream().filter(iv -> iv.dono().equals(p.getUniqueId()))
                .sorted((a, b) -> Integer.compare(a.ate(), b.ate())).toList());
        while (meus.size() > MAX_INVOCACOES) dispensar(meus.removeFirst());
        w.playSound(onde, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 1.2f);
        return true;
    }

    private static void vida(LivingEntity e, double vida) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max == null) return;
        max.setBaseValue(Math.max(1, vida));
        e.setHealth(Math.max(1, vida));
    }

    private void dispensar(Invocado iv) {
        invocados.remove(iv.entidade());
        Entity e = Bukkit.getEntity(iv.entidade());
        if (e == null) return;
        e.getWorld().spawnParticle(Particle.POOF, e.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
        e.remove();
    }

    /** Dono da invocação (ou null se a entidade não é uma). */
    UUID donoInvocacao(Entity e) {
        Invocado iv = invocados.get(e.getUniqueId());
        return iv == null ? null : iv.dono();
    }

    /** Uma invocação acertou alguém: soma o elemento dela no golpe. */
    void golpeDeInvocacao(EntityDamageByEntityEvent ev, LivingEntity alvo) {
        Invocado iv = invocados.get(ev.getDamager().getUniqueId());
        if (iv == null) return;
        Player dono = Bukkit.getPlayer(iv.dono());
        if (dono == null || !inimigo(dono, alvo)) {
            ev.setCancelled(true);
            return;
        }
        ev.setDamage(2 + 1.5 * iv.pot());
        if (ev.getDamager() instanceof IronGolem) ev.setDamage(5 + 3 * iv.pot());
        conj.atingir(dono, alvo, iv.es(), iv.pot() * 0.3, ev.getDamager().getLocation());
        if (iv.receita() == Receita.SOMBRAS_GEMEAS) {
            efeito(alvo, PotionEffectType.WITHER, 60, 0);
            curar(dono, 0.5);
        }
    }

    private void tickInvocados() {
        int t = agora();
        for (Invocado iv : List.copyOf(invocados.values())) {
            Entity ent = Bukkit.getEntity(iv.entidade());
            Player dono = Bukkit.getPlayer(iv.dono());
            if (!(ent instanceof LivingEntity le) || !le.isValid() || dono == null || t > iv.ate() || !dono.getWorld().equals(le.getWorld())) {
                dispensar(iv);
                continue;
            }
            if (le.getLocation().distanceSquared(dono.getLocation()) > 24 * 24) le.teleport(dono.getLocation().add(0, 1, 0));
            if (le instanceof Allay) {
                if (ciclo % 4 == 0) {
                    for (LivingEntity e : perto(le.getLocation(), 6)) {
                        if (e instanceof Player outro && (outro == dono || !inimigo(dono, outro))) {
                            curar(outro, 1 + 0.5 * iv.pot());
                            outro.getWorld().spawnParticle(Particle.HEART, outro.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3);
                        }
                    }
                    if (iv.receita() == Receita.ESPIRITO_DA_FLORESTA) {
                        for (int i = 0; i < 6; i++) {
                            Block b = le.getLocation().getBlock().getRelative(rnd().nextInt(-5, 6), rnd().nextInt(-2, 2), rnd().nextInt(-5, 6));
                            if (crescer(b)) le.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.3, 0.3);
                        }
                    }
                }
                continue;
            }
            if (le instanceof Mob mob) {
                LivingEntity alvo = mob.getTarget();
                if (alvo == null || !alvo.isValid() || !inimigo(dono, alvo) || alvo.getLocation().distanceSquared(le.getLocation()) > 20 * 20) {
                    mob.setTarget(escolherAlvo(dono, le));
                }
            }
            for (Essencia e : iv.es()) le.getWorld().spawnParticle(Particle.DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 1, 0.2, 0.2, 0.2, 0, e.poeira(0.9f));
        }
    }

    /** Monstro mais perto (ou quem está mirando o dono). */
    private static LivingEntity escolherAlvo(Player dono, LivingEntity invocado) {
        LivingEntity melhor = null;
        double d = Double.MAX_VALUE;
        for (LivingEntity e : perto(invocado.getLocation(), 14)) {
            if (e == dono || !inimigo(dono, e)) continue;
            boolean ameaca = e instanceof Enemy || (e instanceof Mob m && dono.equals(m.getTarget()));
            if (!ameaca) continue;
            double dist = e.getLocation().distanceSquared(invocado.getLocation());
            if (dist < d) { d = dist; melhor = e; }
        }
        return melhor;
    }

    void dispensarTodos() {
        for (Invocado iv : List.copyOf(invocados.values())) dispensar(iv);
        for (Canal c : List.copyOf(canais.values())) if (c.tarefa != null) c.tarefa.cancel();
        canais.clear();
        minas.clear();
    }

    void aoSair(Player p) {
        Canal c = canais.remove(p.getUniqueId());
        if (c != null && c.tarefa != null) c.tarefa.cancel();
        for (Invocado iv : List.copyOf(invocados.values())) if (iv.dono().equals(p.getUniqueId())) dispensar(iv);
        minas.removeIf(m -> m.dono().equals(p.getUniqueId()));
    }

    // =====================================================================
    //  Encantar arma
    // =====================================================================

    boolean encantarArma(Lance l, Receita receita) {
        int golpes = receita == Receita.LAMINA_VAMPIRICA ? 12 : receita != null ? 10 : 5 + (int) Math.round(l.pot);
        l.pf.arma = new ArmaEncantada(EnumSet.copyOf(l.es), l.pot, receita, golpes, agora() + 600);
        l.p.getWorld().playSound(l.p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.4f);
        l.p.getWorld().playSound(l.p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
        brilho(l.p.getLocation().add(0, 1, 0), l.es, 20, l.estilo);
        l.p.sendActionBar(Component.text("✦ Arma encantada: " + golpes + " golpes", Arcano.COR));
        return true;
    }

    /** Golpe corpo a corpo de quem está com a arma encantada. */
    void golpeEncantado(Player p, LivingEntity alvo, EntityDamageByEntityEvent ev) {
        Perfil pf = plugin.arcano().perfil(p);
        ArmaEncantada a = pf.arma;
        if (a == null) return;
        if (agora() > a.ate || a.golpes <= 0) {
            pf.arma = null;
            return;
        }
        if (!inimigo(p, alvo)) return;
        conj.atingir(p, alvo, a.es, a.pot * 0.45, p.getLocation());
        if (a.receita == Receita.LAMINA_FLAMEJANTE) {
            for (LivingEntity e : perto(alvo.getLocation(), 3)) {
                if (!inimigo(p, e)) continue;
                e.setFireTicks(Math.max(e.getFireTicks(), 80));
                if (e != alvo) conj.ferir(p, e, 1.5 * a.pot);
            }
            alvo.getWorld().spawnParticle(Particle.FLAME, meio(alvo), 30, 0.6, 0.4, 0.6, 0.05);
            alvo.getWorld().playSound(alvo.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.7f, 1.2f);
        } else if (a.receita == Receita.LAMINA_VAMPIRICA) {
            curar(p, ev.getFinalDamage() * 0.3);
            alvo.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, meio(alvo), 6, 0.3, 0.3, 0.3, 0.1);
        } else if (a.receita == Receita.LAMINA_DO_TROVAO && rnd().nextDouble() < 0.35) {
            alvo.getWorld().strikeLightningEffect(alvo.getLocation());
            conj.ferir(p, alvo, 4 * a.pot);
        }
        a.golpes--;
        if (a.golpes <= 0) {
            pf.arma = null;
            p.sendActionBar(Component.text("A arma voltou ao normal.", NamedTextColor.GRAY));
        } else {
            p.sendActionBar(Component.text("✦ Arma encantada: " + a.golpes + " golpes", Arcano.COR));
        }
    }

    // =====================================================================
    //  Tomos Proibidos
    // =====================================================================

    boolean chuvaDeMeteoros(Player p, double pot) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 40, FluidCollisionMode.NEVER, true);
        if (r == null) { falhar(p, "Olhe para o chão (até 40 blocos)."); return false; }
        Location centro = r.getHitPosition().toLocation(p.getWorld());
        for (int i = 0; i < 8; i++) {
            Location alvo = centro.clone().add(rnd().nextDouble(-7, 7), 0, rnd().nextDouble(-7, 7));
            Location ceu = alvo.clone().add(rnd().nextDouble(-4, 4), 22, rnd().nextDouble(-4, 4));
            Vector passo = alvo.toVector().subtract(ceu.toVector()).multiply(1 / 18.0);
            new BukkitRunnable() {
                int t = 0;
                final Location pos = ceu.clone();
                @Override
                public void run() {
                    if (t++ >= 18) {
                        cancel();
                        World w = alvo.getWorld();
                        w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 1);
                        w.spawnParticle(Particle.LAVA, alvo, 20, 1, 0.5, 1);
                        w.playSound(alvo, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
                        for (LivingEntity e : perto(alvo, 3.2)) {
                            if (!inimigo(p, e)) continue;
                            conj.ferir(p, e, 5 * pot);
                            e.setFireTicks(Math.max(e.getFireTicks(), 100));
                        }
                        return;
                    }
                    pos.add(passo);
                    pos.getWorld().spawnParticle(Particle.FLAME, pos, 8, 0.3, 0.3, 0.3, 0.02);
                    pos.getWorld().spawnParticle(Particle.LARGE_SMOKE, pos, 3, 0.2, 0.2, 0.2, 0.01);
                }
            }.runTaskTimer(plugin, i * 6L, 1L);
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        return true;
    }

    boolean zeroAbsoluto(Player p, double pot) {
        World w = p.getWorld();
        for (LivingEntity e : perto(p.getLocation(), 10)) {
            if (!inimigo(p, e)) continue;
            efeito(e, PotionEffectType.SLOWNESS, e instanceof Player ? 60 : 100, 6);
            e.setFreezeTicks(e.getMaxFreezeTicks() + 140);
            conj.ferir(p, e, 4 * pot);
            w.spawnParticle(Particle.BLOCK, meio(e), 30, 0.4, 0.6, 0.4, Material.BLUE_ICE.createBlockData());
        }
        for (int i = 0; i < 3; i++) anel(p.getLocation().add(0, 0.2 + i * 0.6, 0), 3 + i * 3, EnumSet.of(Essencia.GELO, Essencia.AGUA));
        w.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 300, 6, 1.5, 6, 0.05);
        w.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.5f);
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
        return true;
    }

    boolean julgamento(Player p, double pot) {
        World w = p.getWorld();
        int raios = 0;
        for (LivingEntity e : perto(p.getLocation(), 14)) {
            if (e == p) continue;
            if (inimigo(p, e) && raios < 6) {
                w.strikeLightningEffect(e.getLocation());
                conj.ferir(p, e, 6 * pot);
                raios++;
            } else if (e instanceof Player aliado && !inimigo(p, aliado)) {
                curar(aliado, 4 * pot);
                w.spawnParticle(Particle.HEART, aliado.getLocation().add(0, 2, 0), 4, 0.3, 0.2, 0.3);
            }
        }
        curar(p, 4 * pot);
        w.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 2, 0), 60, 0.5, 1.5, 0.5, 0.05);
        if (raios == 0) p.sendActionBar(Component.text("Nenhum inimigo por perto, mas a luz te curou.", NamedTextColor.YELLOW));
        return true;
    }

    boolean ruptura(Player p, double pot) {
        RayTraceResult r = p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), 20,
                FluidCollisionMode.NEVER, true, 0.5, e -> e instanceof LivingEntity && e != p);
        Location centro = r != null ? r.getHitPosition().toLocation(p.getWorld())
                : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(20));
        conj.vortice(p, centro, 8, 60, 0.5, () -> {
            World w = centro.getWorld();
            w.spawnParticle(Particle.EXPLOSION_EMITTER, centro, 1);
            w.spawnParticle(Particle.REVERSE_PORTAL, centro, 200, 2, 2, 2, 0.4);
            w.playSound(centro, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.8f);
            for (LivingEntity e : perto(centro, 5)) if (inimigo(p, e)) conj.ferir(p, e, 8 * pot);
        });
        return true;
    }

    boolean renascer(Player p, double pot) {
        World w = p.getWorld();
        for (LivingEntity e : perto(p.getLocation(), 12)) {
            if (e instanceof Player aliado && (aliado == p || !inimigo(p, aliado))) {
                AttributeInstance max = aliado.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) aliado.setHealth(max.getValue());
                aliado.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                w.spawnParticle(Particle.HEART, aliado.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
            }
        }
        for (int i = 0; i < 40; i++) {
            Block b = p.getLocation().getBlock().getRelative(rnd().nextInt(-8, 9), rnd().nextInt(-2, 3), rnd().nextInt(-8, 9));
            if (crescer(b)) w.spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3);
        }
        w.spawnParticle(Particle.CHERRY_LEAVES, p.getLocation().add(0, 2, 0), 200, 6, 2, 6, 0);
        w.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.8f);
        return true;
    }

    /** Nuvem de veneno que fica no chão (Nuvem Tóxica). */
    static void nuvemToxica(Player p, Location onde) {
        AreaEffectCloud n = onde.getWorld().spawn(onde, AreaEffectCloud.class);
        n.setSource(p);
        n.setRadius(3);
        n.setDuration(160);
        n.setBasePotionType(PotionType.STRONG_POISON);
        n.setRadiusPerTick(-0.005f);
    }

    // =====================================================================
    //  Tick (a cada 5 ticks)
    // =====================================================================

    void tick() {
        ciclo++;
        if (!minas.isEmpty()) tickMinas();
        if (ciclo % 2 == 0 && !invocados.isEmpty()) tickInvocados();
        if (ciclo % 2 == 0) {
            int t = agora();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                Perfil pf = plugin.arcano().perfil(p);
                ArmaEncantada a = pf.arma;
                if (a == null) continue;
                if (t > a.ate) {
                    pf.arma = null;
                    p.sendActionBar(Component.text("O encanto da arma acabou.", NamedTextColor.GRAY));
                    continue;
                }
                Location mao = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().rotateAroundY(-0.6).multiply(0.4)).add(0, 0.9, 0);
                for (Essencia e : a.es) p.getWorld().spawnParticle(Particle.DUST, mao, 2, 0.08, 0.15, 0.08, 0, e.poeira(0.8f));
            }
        }
    }
}
