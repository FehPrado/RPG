package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Faz as magias acontecerem.
 *
 * Qualquer combinação funciona: cada essência tem um papel em cada forma
 * (Fogo queima, Gelo congela, Vida cura...) e a magia aplica todos juntos.
 * As receitas secretas trocam esse comportamento por um efeito único.
 */
public final class Conjuracao {

    /** Ordem de quem decide o formato numa magia de Criação. */
    private static final List<Essencia> PRIORIDADE_CRIACAO = List.of(
            Essencia.TERRA, Essencia.GELO, Essencia.AGUA, Essencia.NATUREZA, Essencia.FOGO,
            Essencia.ENERGIA, Essencia.VENTO, Essencia.VAZIO, Essencia.SOMBRA, Essencia.VIDA);

    static final org.bukkit.NamespacedKey CHAVE_INVOCACAO = new org.bukkit.NamespacedKey("rpgatributos", "invocacao_dono");
    private static final int MAX_FARDOS_POR_JOGADOR = 3;
    private static final long DURACAO_FARDO_MS = 20 * 60 * 1000;

    private record Impacto(Location local, LivingEntity entidade, Block bloco, BlockFace face) {}
    private record Fardo(UUID dono, long expira) {}

    private final RPGAtributos plugin;
    private final Map<Location, Fardo> fardos = new HashMap<>();

    private final Feiticos feiticos;
    private final Reacoes reacoes;

    public Conjuracao(RPGAtributos plugin) {
        this.plugin = plugin;
        this.feiticos = new Feiticos(plugin, this);
        this.reacoes = new Reacoes(plugin, this);
    }

    Feiticos feiticos() { return feiticos; }

    Arcano arcano() { return plugin.arcano(); }
    private Settings cfg() { return plugin.settings(); }
    static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Lançar
    // =====================================================================

    public void lancar(Player p) {
        // Segurando o clique de um raio ou de uma canalização: só mantém.
        if (feiticos.continuar(p)) return;
        if (plugin.maldicoes().transformado(p)) {
            falhar(p, "A fera não usa magia (você está transformado em lobisomem).");
            return;
        }
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        Magia m = pf.magiaSelecionada();
        if (m == null) {
            falhar(p, "Nenhuma magia selecionada. Agache + clique direito para abrir o grimório.");
            return;
        }
        Map<Essencia, Integer> temNoCorpo = pf.essencias();
        for (Essencia e : m.essencias()) {
            if (!temNoCorpo.containsKey(e)) {
                falhar(p, "Você não tem mais essência de " + e.nome() + " no corpo.");
                return;
            }
        }
        int agora = Bukkit.getCurrentTick();
        Integer pronto = pf.recargas.get(m.chave());
        if (pronto != null && agora < pronto) {
            falhar(p, "Recarregando... " + StatsManager.fmt(Math.ceil((pronto - agora) / 2.0) / 10.0) + "s");
            return;
        }
        // Sobrecarga e Eco de Mana (talentos) podem deixar a magia de graça; o Raio nunca.
        boolean gratis = m.forma() != Forma.RAIO && plugin.talentos().magiaGratis(p);
        double custo = gratis ? 0 : arc.custoMana(m, pf) * plugin.classes().custoMagia(p) * plugin.lendas().custoMagia(p) * arc.custoDoCajado(p)
                * plugin.talentos().custoMagia(p);
        if (pf.mana < custo) {
            falhar(p, "Mana insuficiente (" + (int) pf.mana + "/" + (int) custo + ")");
            arc.mostrarMana(pf, 60);
            return;
        }

        double pot = arc.potencia(p, pf, m);
        Receita r = m.receita();
        int recarga = (int) Math.round(arc.recargaTicks(m) * arc.recargaDoCajado(p));

        // Raio: feixe enquanto o clique estiver segurado; a recarga começa quando ele acaba.
        if (m.forma() == Forma.RAIO) {
            arc.gastarMana(p, pf, custo);
            pf.recargas.put(m.chave(), agora + 20 * 60);
            feiticos.raio(new Feiticos.Lance(p, pf, m.essencias(), pot, m.mods()), r, custo * 0.12, false,
                    () -> pf.recargas.put(m.chave(), Bukkit.getCurrentTick() + recarga));
            depoisDeLancar(p, pf, m, false);
            return;
        }
        // Canalizar: carrega enquanto segura e lança ao soltar.
        if (m.tem(Modificador.CANALIZAR) && Modificador.CANALIZAR.serveEm(m.forma())) {
            arc.gastarMana(p, pf, custo);
            pf.recargas.put(m.chave(), agora + 20 * 60);
            feiticos.canalizar(p, pot, potFinal -> {
                boolean ok = executar(p, pf, m, potFinal);
                if (!ok) arc.darMana(p, custo); // não deu (sem alvo...): devolve a mana
                pf.recargas.put(m.chave(), Bukkit.getCurrentTick() + (ok ? recarga : 0));
                if (ok) depoisDeLancar(p, pf, m, true);
            });
            return;
        }

        if (!executar(p, pf, m, pot)) return;
        arc.gastarMana(p, pf, custo);
        pf.recargas.put(m.chave(), agora + recarga);
        depoisDeLancar(p, pf, m, true);
    }

    /** Faz a magia acontecer (receita secreta ou forma comum) e agenda o Eco. */
    private boolean executar(Player p, Perfil pf, Magia m, double pot) {
        Feiticos.Lance lance = new Feiticos.Lance(p, pf, m.essencias(), pot, m.mods());
        Receita r = m.receita();
        boolean ok = r != null ? receita(p, pf, r, pot, lance) : feiticos.executar(lance, m.forma());
        if (ok && r == null && m.tem(Modificador.ECO) && Modificador.ECO.serveEm(m.forma())) feiticos.eco(lance, m.forma());
        return ok;
    }

    private void depoisDeLancar(Player p, Perfil pf, Magia m, boolean som) {
        if (som) p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.6f, 1.4f);
        if (ganhaXp(p)) {
            plugin.stats().darXp(p, Skill.ARCANO, cfg().arcXpMagia * m.essencias().size());
            for (Essencia e : m.essencias()) arcano().darMaestria(p, pf, e, 2);
        }
        plugin.titulos().registrar(p, "magias", 1); // tarefas das classes
    }

    /** Pergaminho: lança a magia gravada nele, sem mana, essências nem recarga. */
    public boolean lancarPergaminho(Player p, Magia m, double pot) {
        Perfil pf = arcano().perfil(p);
        if (m.forma() == Forma.RAIO) {
            feiticos.raio(new Feiticos.Lance(p, pf, m.essencias(), pot, m.mods()), m.receita(), 0, true, null);
            return true;
        }
        // Canalizar num pergaminho já sai carregado pela metade.
        double forca = m.tem(Modificador.CANALIZAR) ? pot * 1.5 : pot;
        boolean ok = executar(p, pf, m, forca);
        if (ok) p.getWorld().playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.6f);
        return ok;
    }

    /** Reações e poderes que ficam no mundo (a cada 5 ticks). */
    public void tick() {
        feiticos.tick();
        reacoes.tick();
    }

    public void desligar() {
        feiticos.dispensarTodos();
    }

    public void aoSair(Player p) {
        feiticos.aoSair(p);
    }

    /** Dono de uma invocação (null se não for uma). */
    public UUID donoInvocacao(Entity e) {
        return feiticos.donoInvocacao(e);
    }

    public void golpeDeInvocacao(org.bukkit.event.entity.EntityDamageByEntityEvent ev, LivingEntity alvo) {
        feiticos.golpeDeInvocacao(ev, alvo);
    }

    public void golpeEncantado(Player p, LivingEntity alvo, org.bukkit.event.entity.EntityDamageByEntityEvent ev) {
        feiticos.golpeEncantado(p, alvo, ev);
    }

    static boolean ganhaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    static void falhar(Player p, String msg) {
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.6f);
    }

    // =====================================================================
    //  Formas genéricas
    // =====================================================================

    boolean corpo(Player p, Set<Essencia> es, double pot) {
        for (Essencia e : es) {
            switch (e) {
                case FOGO -> {
                    efeito(p, PotionEffectType.FIRE_RESISTANCE, 200 * pot, 0);
                    efeito(p, PotionEffectType.STRENGTH, 100 * pot, 0);
                }
                case GELO -> {
                    efeito(p, PotionEffectType.RESISTANCE, 100 * pot, 0);
                    passosGelidos(p, (int) (200 * pot));
                }
                case VENTO -> {
                    efeito(p, PotionEffectType.SPEED, 120 * pot, 1);
                    efeito(p, PotionEffectType.SLOW_FALLING, 100, 0);
                    p.setVelocity(p.getLocation().getDirection().setY(0).normalize().multiply(1 + 0.2 * pot).setY(0.35));
                }
                case AGUA -> {
                    efeito(p, PotionEffectType.WATER_BREATHING, 600 * pot, 0);
                    efeito(p, PotionEffectType.DOLPHINS_GRACE, 200 * pot, 0);
                    p.setFireTicks(0);
                }
                case VIDA -> {
                    curar(p, 3 * pot);
                    efeito(p, PotionEffectType.REGENERATION, 100 * pot, 0);
                }
                case TERRA -> {
                    efeito(p, PotionEffectType.RESISTANCE, 80 * pot, 1);
                    efeito(p, PotionEffectType.ABSORPTION, 200 * pot, 0);
                }
                case SOMBRA -> {
                    efeito(p, PotionEffectType.INVISIBILITY, 100 * pot, 0);
                    efeito(p, PotionEffectType.NIGHT_VISION, 400, 0);
                }
                case VAZIO -> piscar(p, 4 + 2 * pot);
                case ENERGIA -> {
                    efeito(p, PotionEffectType.HASTE, 200 * pot, 1);
                    efeito(p, PotionEffectType.SPEED, 200 * pot, 0);
                }
                case NATUREZA -> {
                    p.setFoodLevel(Math.min(20, p.getFoodLevel() + (int) Math.round(2 * pot)));
                    p.setSaturation((float) Math.min(p.getFoodLevel(), p.getSaturation() + pot));
                    efeito(p, PotionEffectType.REGENERATION, 60, 0);
                }
            }
        }
        espiral(p, es);
        return true;
    }

    boolean criacao(Player p, Set<Essencia> es, double pot, double escala) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 6,
                FluidCollisionMode.SOURCE_ONLY, true);
        if (r == null || r.getHitBlock() == null || r.getHitBlockFace() == null) {
            falhar(p, "Olhe para um bloco próximo (até 6 blocos).");
            return false;
        }
        Block base = r.getHitBlock().isLiquid() ? r.getHitBlock() : r.getHitBlock().getRelative(r.getHitBlockFace());
        Essencia principal = PRIORIDADE_CRIACAO.stream().filter(es::contains).findFirst().orElse(Essencia.TERRA);
        Set<Essencia> outras = EnumSet.copyOf(es);
        outras.remove(principal);
        int dur = (int) Math.min(800, 300 * pot);
        BlocosTemporarios temp = arcano().temporarios();

        boolean criou = switch (principal) {
            case TERRA -> parede(p, base, materialDaParede(outras), dur);
            case GELO -> {
                boolean algum = false;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    algum |= temp.colocar(p, base.getRelative(dx, 0, dz), Material.PACKED_ICE, dur);
                }
                yield algum;
            }
            case AGUA -> temp.colocar(p, base, Material.WATER, dur);
            case NATUREZA -> {
                Leaves folhas = (Leaves) Material.AZALEA_LEAVES.createBlockData();
                folhas.setPersistent(true);
                boolean algum = false;
                for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    algum |= temp.colocar(p, base.getRelative(dx, dy, dz), folhas, dur);
                }
                yield algum;
            }
            case FOGO -> temp.colocar(p, base, Material.CAMPFIRE, dur);
            case ENERGIA -> {
                boolean ok = temp.colocar(p, base, Material.LIGHT, (int) Math.min(2400, 1200 * pot));
                if (ok) zonaVisual(base.getLocation().toCenterLocation(), Particle.END_ROD, (int) Math.min(2400, 1200 * pot));
                yield ok;
            }
            case VENTO -> {
                correnteDeAr(base.getLocation().toCenterLocation(), 200);
                yield true;
            }
            case VAZIO -> {
                vortice(p, base.getLocation().toCenterLocation(), 5, 120, 0.35, null);
                yield true;
            }
            case SOMBRA -> {
                boolean algum = temp.colocar(p, base, Material.COBWEB, 200);
                for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                    algum |= temp.colocar(p, base.getRelative(f), Material.COBWEB, 200);
                }
                yield algum;
            }
            case VIDA -> {
                zona(p, base.getLocation().toCenterLocation(), 4 * escala, 200, EnumSet.of(Essencia.VIDA), pot);
                yield true;
            }
        };
        if (!criou) {
            falhar(p, "Não há espaço livre ali (ou a área é protegida).");
            return false;
        }
        // As outras essências viram uma zona mágica em volta da criação.
        if (!outras.isEmpty()) zona(p, base.getLocation().toCenterLocation(), 3 * escala, 200, outras, pot * 0.4);
        brilho(base.getLocation().toCenterLocation(), es, 30);
        base.getWorld().playSound(base.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
        return true;
    }

    private static Material materialDaParede(Set<Essencia> outras) {
        if (outras.contains(Essencia.GELO)) return Material.PACKED_ICE;
        if (outras.contains(Essencia.FOGO)) return Material.MAGMA_BLOCK;
        if (outras.contains(Essencia.NATUREZA)) return Material.MOSS_BLOCK;
        if (outras.contains(Essencia.SOMBRA)) return Material.BLACKSTONE;
        if (outras.contains(Essencia.VAZIO)) return Material.END_STONE;
        if (outras.contains(Essencia.AGUA)) return Material.PRISMARINE;
        if (outras.contains(Essencia.ENERGIA)) return Material.AMETHYST_BLOCK;
        return Material.COBBLESTONE;
    }

    /** Parede 3x3 de frente para o jogador. */
    private boolean parede(Player p, Block centro, Material m, int dur) {
        BlockFace olhar = p.getFacing();
        boolean eixoX = olhar == BlockFace.NORTH || olhar == BlockFace.SOUTH;
        boolean algum = false;
        for (int lado = -1; lado <= 1; lado++) {
            for (int y = 0; y <= 2; y++) {
                Block b = eixoX ? centro.getRelative(lado, y, 0) : centro.getRelative(0, y, lado);
                algum |= arcano().temporarios().colocar(p, b, m, dur);
            }
        }
        return algum;
    }

    // =====================================================================
    //  O papel de cada essência
    // =====================================================================

    /** Efeito ofensivo de todas as essências num alvo. O dano é somado num golpe só. */
    void atingir(Player p, LivingEntity alvo, Set<Essencia> es, double pot, Location origem) {
        Perfil pf = arcano().perfil(p);
        double dano = 0;
        for (Essencia e : es) {
            // Maestria 5: a variante do elemento.
            boolean v = arcano().nivelMaestria(pf, e) >= Arcano.MAESTRIA_VARIANTE;
            switch (e) {
                case FOGO -> {
                    alvo.setFireTicks(Math.max(alvo.getFireTicks(), (int) (60 * pot * (v ? 1.5 : 1))));
                    dano += (v ? 3 : 2) * pot;
                    if (v) alvo.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, meio(alvo), 8, 0.3, 0.4, 0.3, 0.02);
                }
                case GELO -> {
                    alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), alvo.getMaxFreezeTicks() + (int) (40 * pot * (v ? 2 : 1))));
                    efeito(alvo, PotionEffectType.SLOWNESS, 60 * pot, v ? 2 : 1);
                    dano += pot;
                }
                case VENTO -> {
                    empurrar(alvo, origem, (0.8 + 0.3 * pot) * (v ? 1.4 : 1), v ? 0.6 : 0.45);
                    dano += pot;
                }
                case AGUA -> {
                    alvo.setFireTicks(0);
                    dano += pot + (sensivelAgua(alvo) ? 4 * pot : 0);
                    efeito(alvo, PotionEffectType.SLOWNESS, 30, 0);
                    if (v) curar(p, 0.5 * pot);
                }
                case VIDA -> {
                    if (Tag.ENTITY_TYPES_UNDEAD.isTagged(alvo.getType())) dano += 3 * pot; // vida queima mortos-vivos
                    else curar(p, pot);
                    if (v) efeito(p, PotionEffectType.ABSORPTION, 100, 0);
                }
                case TERRA -> {
                    dano += 2 * pot;
                    efeito(alvo, PotionEffectType.SLOWNESS, 25, 3);
                    if (v) efeito(p, PotionEffectType.RESISTANCE, 60, 0);
                }
                case SOMBRA -> {
                    efeito(alvo, PotionEffectType.BLINDNESS, 40 * pot, 0);
                    efeito(alvo, PotionEffectType.WITHER, 60, 0);
                    if (v) efeito(alvo, PotionEffectType.DARKNESS, 60, 0);
                    curar(p, (v ? 1 : 0.5) * pot);
                }
                case VAZIO -> {
                    puxar(alvo, p.getLocation(), (0.6 + 0.25 * pot) * (v ? 1.4 : 1));
                    dano += (v ? 2 : 1) * pot;
                }
                case ENERGIA -> {
                    dano += 3 * pot;
                    efeito(alvo, PotionEffectType.SLOWNESS, 20, 0);
                    alvo.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, meio(alvo), 15, 0.3, 0.4, 0.3, 0.2);
                    if (v && rnd().nextDouble() < 0.3) {
                        for (LivingEntity outro : perto(alvo.getLocation(), 4)) {
                            if (outro == alvo || !inimigo(p, outro)) continue;
                            feixeParticula(meio(alvo), meio(outro), Particle.ELECTRIC_SPARK);
                            ferir(p, outro, 2 * pot);
                            break;
                        }
                    }
                }
                case NATUREZA -> {
                    efeito(alvo, PotionEffectType.POISON, 60 * pot, v ? 1 : 0);
                    efeito(alvo, PotionEffectType.SLOWNESS, 40, 0);
                }
            }
        }
        ferir(p, alvo, dano);
        brilho(meio(alvo), es, 8);
        if (alvo.isValid() && !alvo.isDead()) reacoes.aoAtingir(p, alvo, es, pot);
    }

    /** Em aliados: só as essências que ajudam. */
    void ajudar(LivingEntity alvo, Set<Essencia> es, double pot) {
        if (es.contains(Essencia.VIDA)) curar(alvo, 2 * pot);
        if (es.contains(Essencia.AGUA)) alvo.setFireTicks(0);
    }

    /** Efeito das essências num bloco. */
    void bloco(Player p, Block b, BlockFace face, Set<Essencia> es, double pot) {
        BlocosTemporarios temp = arcano().temporarios();
        for (Essencia e : es) {
            switch (e) {
                case FOGO -> {
                    Material t = b.getType();
                    if ((t == Material.ICE || t == Material.FROSTED_ICE) && temp.podeConstruir(p, b)) {
                        b.setType(Material.WATER);
                    } else if ((t == Material.SNOW || t == Material.SNOW_BLOCK || t == Material.POWDER_SNOW)
                            && temp.podeConstruir(p, b)) {
                        b.setType(Material.AIR);
                    } else if (b.getBlockData() instanceof Lightable l && !l.isLit()
                            && (Tag.CAMPFIRES.isTagged(t) || Tag.CANDLES.isTagged(t))) {
                        l.setLit(true);
                        b.setBlockData(l);
                    }
                }
                case GELO -> {
                    if (b.getType() == Material.WATER && b.getBlockData() instanceof Levelled lv && lv.getLevel() == 0) {
                        temp.colocar(p, b, Material.FROSTED_ICE, 400);
                    }
                }
                case AGUA -> {
                    Material t = b.getType();
                    if (b.getBlockData() instanceof Farmland f) {
                        f.setMoisture(f.getMaximumMoisture());
                        b.setBlockData(f);
                    } else if (t == Material.FIRE || t == Material.SOUL_FIRE) {
                        b.setType(Material.AIR);
                    } else {
                        Block cima = b.getRelative(BlockFace.UP);
                        if (cima.getType() == Material.FIRE || cima.getType() == Material.SOUL_FIRE) cima.setType(Material.AIR);
                    }
                }
                case VIDA -> {
                    if (temp.podeConstruir(p, b)) {
                        int vezes = pot >= 2 ? 2 : 1;
                        for (int i = 0; i < vezes; i++) b.applyBoneMeal(face == null ? BlockFace.UP : face);
                    }
                }
                case NATUREZA -> {
                    if (b.getType() == Material.DIRT && b.getRelative(BlockFace.UP).isEmpty() && temp.podeConstruir(p, b)) {
                        b.setType(Material.GRASS_BLOCK);
                    } else if (b.getType() == Material.GRASS_BLOCK && temp.podeConstruir(p, b)) {
                        b.applyBoneMeal(BlockFace.UP);
                    }
                }
                default -> { }
            }
        }
    }

    // =====================================================================
    //  Receitas secretas
    // =====================================================================

    boolean receita(Player p, Perfil pf, Receita r, double pot, Feiticos.Lance lance) {
        World w = p.getWorld();
        int agora = Bukkit.getCurrentTick();
        switch (r) {
            case BOLA_DE_FOGO -> {
                SmallFireball bola = p.launchProjectile(SmallFireball.class);
                bola.setIsIncendiary(false); // incendeia quem acertar, mas não o terreno
                bola.setYield(0);
                bola.setDirection(p.getEyeLocation().getDirection().multiply(1.5));
                w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                return true;
            }
            case NEVASCA -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 160) { cancel(); return; }
                        Location c = p.getLocation().add(0, 1.5, 0);
                        w.spawnParticle(Particle.SNOWFLAKE, c, 60, 6, 2, 6, 0.02);
                        w.spawnParticle(Particle.ITEM_SNOWBALL, c, 20, 6, 1.5, 6, 0.1);
                        if (t % 20 == 0) w.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1f, 0.6f);
                        for (LivingEntity e : perto(p.getLocation(), 7)) {
                            if (!inimigo(p, e)) continue;
                            e.setFreezeTicks(Math.max(e.getFreezeTicks(), e.getMaxFreezeTicks() + 40));
                            efeito(e, PotionEffectType.SLOWNESS, 40, 1);
                            if (t % 20 == 0) ferir(p, e, 0.5 * pot);
                        }
                        t += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                return true;
            }
            case PASSO_DO_VAZIO -> {
                piscar(p, 12);
                efeito(p, PotionEffectType.SLOW_FALLING, 60, 0);
                return true;
            }
            case CORACAO_PULSANTE -> {
                curar(p, 8);
                efeito(p, PotionEffectType.ABSORPTION, 600, 1);
                efeito(p, PotionEffectType.REGENERATION, 100, 1);
                w.spawnParticle(Particle.HEART, p.getLocation().add(0, 1.2, 0), 20, 0.6, 0.6, 0.6);
                w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 1.2f);
                return true;
            }
            case DRENAR -> {
                RayTraceResult rt = mirar(p, 8);
                if (rt == null || !(rt.getHitEntity() instanceof LivingEntity alvo) || !inimigo(p, alvo)) {
                    falhar(p, "Mire numa criatura (até 8 blocos).");
                    return false;
                }
                double dano = 6 * pot;
                ferir(p, alvo, dano);
                curar(p, dano * (alvo instanceof Player ? cfg().arcDanoPvp : 1));
                feixe(meio(alvo), p.getLocation().add(0, 1, 0), EnumSet.of(Essencia.SOMBRA, Essencia.VIDA));
                w.spawnParticle(Particle.SCULK_SOUL, meio(alvo), 12, 0.3, 0.4, 0.3, 0.03);
                w.playSound(alvo.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.6f);
                return true;
            }
            case RAIZES -> {
                lancarProjetil(p, r.essencias(), 1.2, 30, imp -> {
                    for (LivingEntity e : perto(imp.local(), 3.5)) {
                        if (!inimigo(p, e)) continue;
                        efeito(e, PotionEffectType.SLOWNESS, 60, 6);
                        e.setVelocity(new Vector());
                        ferir(p, e, 2 * pot);
                    }
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, imp.local(), 60, 2, 0.5, 2, 0);
                    w.spawnParticle(Particle.HAPPY_VILLAGER, imp.local(), 30, 2, 0.5, 2, 0);
                    w.playSound(imp.local(), Sound.BLOCK_ROOTED_DIRT_PLACE, 1.5f, 0.7f);
                });
                return true;
            }
            case VAPOR -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 120) { cancel(); return; }
                        w.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 40, 4, 1, 4, 0.01);
                        w.spawnParticle(Particle.WHITE_SMOKE, p.getLocation().add(0, 0.5, 0), 20, 4, 0.5, 4, 0.01);
                        if (t % 20 == 0) {
                            w.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
                            for (LivingEntity e : perto(p.getLocation(), 5)) {
                                if (inimigo(p, e)) efeito(e, PotionEffectType.BLINDNESS, 50, 0);
                                else {
                                    e.setFireTicks(0);
                                    efeito(e, PotionEffectType.REGENERATION, 60, 0);
                                }
                            }
                            p.setFireTicks(0);
                            efeito(p, PotionEffectType.REGENERATION, 60, 0);
                            for (int i = 0; i < 12; i++) {
                                Block b = p.getLocation().getBlock().getRelative(rnd().nextInt(-4, 5), rnd().nextInt(-1, 3), rnd().nextInt(-4, 5));
                                if (b.getType() == Material.FIRE || b.getType() == Material.SOUL_FIRE) b.setType(Material.AIR);
                            }
                        }
                        t += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                return true;
            }
            case ARVORE_INSTANTANEA -> {
                RayTraceResult rt = p.rayTraceBlocks(8);
                if (rt == null || rt.getHitBlock() == null) {
                    falhar(p, "Olhe para o chão (até 8 blocos).");
                    return false;
                }
                Block chao = rt.getHitBlock();
                Block onde = chao.getRelative(BlockFace.UP);
                if (!(Tag.DIRT.isTagged(chao.getType())) || !arcano().temporarios().podeConstruir(p, onde)) {
                    falhar(p, "Precisa ser terra ou grama em área livre.");
                    return false;
                }
                TreeType[] tipos = {TreeType.TREE, TreeType.BIRCH, TreeType.CHERRY, TreeType.AZALEA, TreeType.BIG_TREE};
                // A árvore não cresce para dentro do território dos outros.
                if (!w.generateTree(onde.getLocation(), rnd(), tipos[rnd().nextInt(tipos.length)],
                        (Predicate<BlockState>) s -> plugin.territorios().podeConstruir(p, s.getLocation()))) {
                    falhar(p, "Não há espaço para a árvore crescer.");
                    return false;
                }
                w.spawnParticle(Particle.HAPPY_VILLAGER, onde.getLocation().add(0.5, 2, 0.5), 60, 1.5, 2, 1.5);
                w.playSound(onde.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.5f, 0.8f);
                return true;
            }
            case PONTE_DE_GELO -> {
                Vector dir = p.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() < 1e-4) {
                    falhar(p, "Olhe para a frente, não para cima ou para baixo.");
                    return false;
                }
                dir.normalize();
                Location pe = p.getLocation().subtract(0, 1, 0);
                int colocados = 0;
                for (int i = 1; i <= 14; i++) {
                    Block b = pe.clone().add(dir.clone().multiply(i)).getBlock();
                    if (arcano().temporarios().colocar(p, b, Material.PACKED_ICE, 600)) colocados++;
                    w.spawnParticle(Particle.SNOWFLAKE, b.getLocation().add(0.5, 1, 0.5), 3, 0.3, 0.1, 0.3, 0);
                }
                if (colocados == 0) {
                    falhar(p, "Não há onde construir a ponte.");
                    return false;
                }
                w.playSound(p.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.6f);
                return true;
            }
            case IMA_DO_VAZIO -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 100) { cancel(); return; }
                        Location destino = p.getLocation().add(0, 0.5, 0);
                        for (Entity e : p.getNearbyEntities(16, 16, 16)) {
                            if (!(e instanceof Item) && !(e instanceof ExperienceOrb)) continue;
                            Vector v = destino.toVector().subtract(e.getLocation().toVector());
                            if (v.lengthSquared() < 1) continue;
                            e.setVelocity(v.normalize().multiply(0.6));
                            if (t % 6 == 0) w.spawnParticle(Particle.REVERSE_PORTAL, e.getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                        }
                        t += 2;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                w.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.4f, 2f);
                return true;
            }
            case BASTIAO -> {
                efeito(p, PotionEffectType.RESISTANCE, 200, 1);
                pf.bastiaoAte = agora + 200;
                w.spawnParticle(Particle.WAX_ON, p.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0);
                w.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
                return true;
            }
            case FARDO_FERTIL -> {
                return fardoFertil(p);
            }
            case TEMPESTADE -> {
                RayTraceResult rt = mirar(p, 25);
                if (rt == null) {
                    falhar(p, "Mire em algo (até 25 blocos).");
                    return false;
                }
                Location l = rt.getHitPosition().toLocation(w);
                w.strikeLightningEffect(l);
                double dano = 8 * pot * (w.hasStorm() ? 2 : 1);
                for (LivingEntity e : perto(l, 3)) {
                    if (!inimigo(p, e)) continue;
                    ferir(p, e, dano);
                    efeito(e, PotionEffectType.SLOWNESS, 40, 1);
                }
                w.spawnParticle(Particle.ELECTRIC_SPARK, l, 60, 1.5, 1, 1.5, 0.3);
                return true;
            }
            case FANTASMA -> {
                efeito(p, PotionEffectType.INVISIBILITY, 200, 0);
                efeito(p, PotionEffectType.SPEED, 200, 1);
                pf.fantasmaAte = agora + 200;
                for (Entity e : p.getNearbyEntities(24, 24, 24)) {
                    if (e instanceof Mob mob && mob.getTarget() == p) mob.setTarget(null);
                }
                w.spawnParticle(Particle.SQUID_INK, p.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.02);
                w.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 0.8f);
                return true;
            }
            case METEORO -> {
                lancarProjetil(p, r.essencias(), 1.2, 40, imp -> {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, imp.local(), 1);
                    w.spawnParticle(Particle.LAVA, imp.local(), 30, 1, 0.5, 1);
                    w.playSound(imp.local(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
                    for (LivingEntity e : perto(imp.local(), 4)) {
                        if (!inimigo(p, e)) continue;
                        ferir(p, e, 7 * pot);
                        e.setFireTicks(Math.max(e.getFireTicks(), 80));
                        empurrar(e, imp.local(), 1.2, 0.5);
                    }
                });
                return true;
            }
            case FENIX -> {
                pf.fenixAte = agora + 1200;
                efeito(p, PotionEffectType.FIRE_RESISTANCE, 1200, 0);
                w.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.05);
                w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
                p.sendActionBar(Component.text("♨ A Fênix protege você por 60 segundos.", Essencia.FOGO.cor()));
                return true;
            }
            case BURACO_NEGRO -> {
                lancarProjetil(p, r.essencias(), 1.0, 40, imp -> vortice(p, imp.local(), 8, 80, 0.45, () -> {
                    w.spawnParticle(Particle.SONIC_BOOM, imp.local(), 1);
                    w.playSound(imp.local(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.7f);
                    for (LivingEntity e : perto(imp.local(), 3.5)) {
                        if (!inimigo(p, e)) continue;
                        ferir(p, e, 7 * pot);
                        efeito(e, PotionEffectType.DARKNESS, 100, 0);
                    }
                }));
                return true;
            }
            case GENESE -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 240) { cancel(); return; }
                        Location c = p.getLocation();
                        w.spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 3, 0), 30, 7, 1, 7, 0);
                        w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, c.clone().add(0, 1, 0), 20, 7, 1, 7, 0);
                        if (t % 20 == 0) {
                            for (int i = 0; i < 16; i++) {
                                Block b = c.getBlock().getRelative(rnd().nextInt(-8, 9), rnd().nextInt(-2, 3), rnd().nextInt(-8, 9));
                                crescer(b);
                                if (b.getType() == Material.GRASS_BLOCK && rnd().nextDouble() < 0.3) b.applyBoneMeal(BlockFace.UP);
                            }
                            for (LivingEntity e : perto(c, 8)) {
                                if (e instanceof Animals a && t == 0 && a.isAdult() && a.canBreed()) a.setLoveModeTicks(600);
                                if (e instanceof Player outro && !inimigoPvp(p, outro)) curar(outro, 2);
                            }
                            curar(p, 2);
                            w.playSound(c, Sound.ITEM_BONE_MEAL_USE, 0.6f, 1.2f);
                        }
                        t += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                return true;
            }
            case TERREMOTO -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 120) { cancel(); return; }
                        Location c = p.getLocation();
                        w.spawnParticle(Particle.EXPLOSION, c, 4, 3, 0.1, 3);
                        w.spawnParticle(Particle.CLOUD, c, 30, 4, 0.1, 4, 0.05);
                        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
                        for (LivingEntity e : perto(c, 6)) {
                            if (!inimigo(p, e)) continue;
                            ferir(p, e, 2 * pot);
                            e.setVelocity(e.getVelocity().setY(0.7));
                            efeito(e, PotionEffectType.SLOWNESS, 40, 1);
                        }
                        t += 20;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
                return true;
            }
            case CORRENTE_ELETRICA -> {
                lancarProjetil(p, r.essencias(), 1.6, 30, imp -> {
                    if (imp.entidade() == null || !inimigo(p, imp.entidade())) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, imp.local(), 30, 0.5, 0.5, 0.5, 0.2);
                        return;
                    }
                    Set<LivingEntity> atingidos = new java.util.HashSet<>();
                    LivingEntity atual = imp.entidade();
                    double dano = 5 * pot;
                    for (int salto = 0; salto < 5 && atual != null; salto++) {
                        atingidos.add(atual);
                        ferir(p, atual, dano);
                        efeito(atual, PotionEffectType.SLOWNESS, 20, 1);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, meio(atual), 20, 0.3, 0.5, 0.3, 0.2);
                        w.playSound(atual.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);
                        LivingEntity proximo = null;
                        double melhor = 36;
                        for (LivingEntity e : perto(atual.getLocation(), 6)) {
                            if (atingidos.contains(e) || !inimigo(p, e)) continue;
                            double d = e.getLocation().distanceSquared(atual.getLocation());
                            if (d < melhor) { melhor = d; proximo = e; }
                        }
                        if (proximo != null) feixeParticula(meio(atual), meio(proximo), Particle.ELECTRIC_SPARK);
                        atual = proximo;
                        dano *= 0.85;
                    }
                });
                return true;
            }
            case ESCUDO_DE_GELO -> {
                efeito(p, PotionEffectType.ABSORPTION, 600, 2);
                efeito(p, PotionEffectType.RESISTANCE, 600, 0);
                w.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.02);
                w.playSound(p.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.5f);
                return true;
            }
            case CHAMA_SAGRADA -> {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 120) { cancel(); return; }
                        Location c = p.getLocation();
                        anel(c.clone().add(0, 0.2, 0), 7, r.essencias());
                        w.spawnParticle(Particle.FLAME, c.clone().add(0, 1, 0), 20, 3, 1, 3, 0.01);
                        for (LivingEntity e : perto(c, 7)) {
                            if (Tag.ENTITY_TYPES_UNDEAD.isTagged(e.getType()) && inimigo(p, e)) {
                                ferir(p, e, 4 * pot);
                                e.setFireTicks(Math.max(e.getFireTicks(), 60));
                            } else if (e instanceof Player outro && !inimigoPvp(p, outro)) {
                                curar(outro, 1.5 * pot);
                            }
                        }
                        curar(p, 1.5 * pot);
                        t += 20;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
                return true;
            }
            case ESPINHEIRO -> {
                RayTraceResult rt = p.rayTraceBlocks(8);
                if (rt == null || rt.getHitBlock() == null) {
                    falhar(p, "Olhe para o chão (até 8 blocos).");
                    return false;
                }
                Block centro = rt.getHitBlock().getRelative(BlockFace.UP);
                BlockData arbusto = Material.SWEET_BERRY_BUSH.createBlockData(d -> ((Ageable) d).setAge(3));
                int criados = 0;
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    Block b = centro.getRelative(dx, 0, dz);
                    if (Tag.DIRT.isTagged(b.getRelative(BlockFace.DOWN).getType())
                            && arcano().temporarios().colocar(p, b, arbusto, 400)) criados++;
                }
                if (criados == 0) {
                    falhar(p, "Precisa de terra/grama livre para o espinheiro brotar.");
                    return false;
                }
                zona(p, centro.getLocation().toCenterLocation(), 3, 400, EnumSet.of(Essencia.NATUREZA, Essencia.SOMBRA), pot * 0.4);
                w.playSound(centro.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1f, 0.8f);
                return true;
            }
            case RETORNO -> {
                Location inicio = p.getLocation();
                Location destino = p.getRespawnLocation() != null ? p.getRespawnLocation() : p.getWorld().getSpawnLocation();
                p.sendActionBar(Component.text("◎ Retornando... fique parado por 3 segundos.", Essencia.VAZIO.cor()));
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead()) { cancel(); return; }
                        if (!p.getWorld().equals(inicio.getWorld()) || p.getLocation().distanceSquared(inicio) > 1) {
                            p.sendActionBar(Component.text("◎ Retorno cancelado (você se mexeu).", NamedTextColor.RED));
                            cancel();
                            return;
                        }
                        w.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.5);
                        if ((t += 10) < 60) return;
                        cancel();
                        p.teleport(destino);
                        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, p.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.05);
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                return true;
            }
            case CHAMAR_CHUVA -> {
                w.setStorm(true);
                w.setWeatherDuration(6000);
                w.spawnParticle(Particle.SPLASH, p.getLocation().add(0, 2, 0), 80, 2, 1, 2);
                w.playSound(p.getLocation(), Sound.WEATHER_RAIN_ABOVE, 1f, 1f);
                p.getServer().broadcast(Component.text("≈ " + p.getName() + " chamou a chuva!", Essencia.AGUA.cor()));
                return true;
            }
            case OLHO_VIGILANTE -> {
                int revelados = 0;
                for (Entity e : p.getNearbyEntities(40, 20, 40)) {
                    if (e instanceof LivingEntity le && e != p) {
                        efeito(le, PotionEffectType.GLOWING, 600, 0);
                        revelados++;
                    }
                }
                w.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.6f);
                p.sendActionBar(Component.text("☠ " + revelados + " criaturas reveladas.", Essencia.SOMBRA.cor()));
                return true;
            }
            case MATILHA -> {
                RayTraceResult rt = p.rayTraceBlocks(6);
                Location onde = rt != null && rt.getHitBlock() != null
                        ? rt.getHitBlock().getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5) : p.getLocation();
                for (int i = 0; i < 3; i++) {
                    Location l = onde.clone().add(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1));
                    Wolf lobo = w.spawn(l, Wolf.class, CreatureSpawnEvent.SpawnReason.CUSTOM, wl -> {
                        wl.setOwner(p);
                        wl.setTamed(true);
                        wl.setPersistent(false);
                        wl.customName(Component.text("Lobo de " + p.getName(), Essencia.NATUREZA.cor()));
                    });
                    w.spawnParticle(Particle.HAPPY_VILLAGER, lobo.getLocation().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!lobo.isValid()) return;
                        lobo.getWorld().spawnParticle(Particle.POOF, lobo.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
                        lobo.remove();
                    }, 1200L);
                }
                w.playSound(onde, Sound.ENTITY_WOLF_GROWL, 1f, 0.8f);
                return true;
            }
            case PRISAO_DE_GELO -> {
                RayTraceResult rt = mirar(p, 12);
                if (rt == null || !(rt.getHitEntity() instanceof LivingEntity alvo) || !inimigo(p, alvo)) {
                    falhar(p, "Mire numa criatura (até 12 blocos).");
                    return false;
                }
                Block pe = alvo.getLocation().getBlock();
                int altura = (int) Math.ceil(alvo.getHeight()) + 1;
                for (int dy = -1; dy <= altura; dy++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    boolean dentro = dx == 0 && dz == 0 && dy >= 0 && dy < altura;
                    if (!dentro) arcano().temporarios().colocar(p, pe.getRelative(dx, dy, dz), Material.PACKED_ICE, 80);
                }
                alvo.teleport(pe.getLocation().add(0.5, 0, 0.5).setDirection(alvo.getLocation().getDirection()));
                efeito(alvo, PotionEffectType.SLOWNESS, 80, 6);
                alvo.setFreezeTicks(alvo.getMaxFreezeTicks() + 80);
                w.playSound(alvo.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.5f, 0.5f);
                return true;
            }
            case AVATAR -> {
                pf.avatarAte = agora + 400;
                efeito(p, PotionEffectType.STRENGTH, 400, 1);
                efeito(p, PotionEffectType.RESISTANCE, 400, 0);
                efeito(p, PotionEffectType.SPEED, 400, 0);
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!p.isOnline() || p.isDead() || t > 400) { cancel(); return; }
                        espiral(p, r.essencias());
                        t += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                w.playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.2f, 0.7f);
                p.sendActionBar(Component.text("✦ Avatar Elemental: seus golpes queimam, congelam e empurram!", NamedTextColor.GOLD));
                return true;
            }
            case SANTUARIO -> {
                RayTraceResult rt = p.rayTraceBlocks(8);
                Location centro = rt != null && rt.getHitBlock() != null
                        ? rt.getHitBlock().getRelative(BlockFace.UP).getLocation().toCenterLocation() : p.getLocation();
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (t > 600 || !centro.isChunkLoaded()) { cancel(); return; }
                        anel(centro, 6, r.essencias());
                        w.spawnParticle(Particle.END_ROD, centro.clone().add(0, 1.5, 0), 6, 2, 1, 2, 0.01);
                        if (t % 20 == 0) {
                            for (LivingEntity e : perto(centro, 6)) {
                                if (Tag.ENTITY_TYPES_UNDEAD.isTagged(e.getType()) && p.isOnline() && inimigo(p, e)) {
                                    ferir(p, e, 3 * pot);
                                    e.setFireTicks(Math.max(e.getFireTicks(), 40));
                                } else if (e instanceof Player outro && (outro == p || !inimigoPvp(p, outro))) {
                                    efeito(outro, PotionEffectType.REGENERATION, 60, 1);
                                    w.spawnParticle(Particle.HEART, outro.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3);
                                }
                            }
                        }
                        t += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                w.playSound(centro, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.2f);
                return true;
            }
            // ---------- formas novas (o Raio vai por lancarRaio) ----------
            case SOPRO_DO_DRAGAO -> {
                return feiticos.sopro(lance, 1.5, e -> e.setFireTicks(Math.max(e.getFireTicks(), 160)), 9);
            }
            case SOPRO_GELIDO -> {
                boolean ok = feiticos.sopro(lance, 1, e -> {
                    efeito(e, PotionEffectType.SLOWNESS, 80, 4);
                    e.setFreezeTicks(e.getMaxFreezeTicks() + 60);
                }, 7);
                passosGelidosEm(p, p.getEyeLocation(), p.getEyeLocation().getDirection(), 7);
                return ok;
            }
            case NUVEM_TOXICA -> {
                boolean ok = feiticos.sopro(lance, 1, e -> efeito(e, PotionEffectType.POISON, 100, 1), 6);
                Feiticos.nuvemToxica(p, p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3.5)));
                return ok;
            }
            case CHUVA_ACIDA -> {
                return feiticos.chuva(lance, 1.3, c -> {
                    for (LivingEntity e : perto(c, 4)) if (inimigo(p, e)) efeito(e, PotionEffectType.POISON, 60, 0);
                }, 100);
            }
            case TEMPESTADE_ELETRICA -> {
                int[] raios = {0};
                return feiticos.chuva(lance, 0.6, c -> {
                    if (raios[0] >= 6) return;
                    List<LivingEntity> alvos = new ArrayList<>();
                    for (LivingEntity e : perto(c, 5)) if (inimigo(p, e)) alvos.add(e);
                    if (alvos.isEmpty()) return;
                    LivingEntity e = alvos.get(rnd().nextInt(alvos.size()));
                    e.getWorld().strikeLightningEffect(e.getLocation());
                    ferir(p, e, 4 * pot);
                    raios[0]++;
                }, 80);
            }
            case GRANIZO -> {
                return feiticos.chuva(lance, 1.6, c -> {
                    for (LivingEntity e : perto(c, 4)) if (inimigo(p, e)) efeito(e, PotionEffectType.SLOWNESS, 40, 1);
                    c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 3, 0), 30, 3, 2, 3, Material.PACKED_ICE.createBlockData());
                }, 60);
            }
            case GOLEM_DE_PEDRA, SOMBRAS_GEMEAS, ESPIRITO_DA_FLORESTA -> {
                return feiticos.invocar(lance, r);
            }
            case LAMINA_FLAMEJANTE, LAMINA_VAMPIRICA, LAMINA_DO_TROVAO -> {
                return feiticos.encantarArma(lance, r);
            }
            // ---------- Tomos Proibidos ----------
            case CHUVA_DE_METEOROS -> { return feiticos.chuvaDeMeteoros(p, pot); }
            case ZERO_ABSOLUTO -> { return feiticos.zeroAbsoluto(p, pot); }
            case JULGAMENTO -> { return feiticos.julgamento(p, pot); }
            case RUPTURA_DIMENSIONAL -> { return feiticos.ruptura(p, pot); }
            case RENASCER_DA_FLORESTA -> { return feiticos.renascer(p, pot); }
            default -> { }
        }
        return false;
    }

    /** Congela a água numa linha à frente (Sopro Gélido). */
    private void passosGelidosEm(Player p, Location de, Vector dir, double alcance) {
        Vector d = dir.clone().setY(0);
        if (d.lengthSquared() < 1e-4) return;
        d.normalize();
        for (double s = 1; s <= alcance; s += 0.8) {
            Block centro = de.clone().add(d.clone().multiply(s)).getBlock();
            for (int dy = -3; dy <= 0; dy++) {
                Block b = centro.getRelative(0, dy, 0);
                if (b.getType() == Material.WATER && b.getBlockData() instanceof Levelled lv && lv.getLevel() == 0
                        && b.getRelative(BlockFace.UP).isEmpty()) {
                    arcano().temporarios().colocar(p, b, Material.FROSTED_ICE, 200);
                    break;
                }
            }
        }
    }

    static void feixeParticula(Location de, Location ate, Particle particula) {
        Vector passo = ate.toVector().subtract(de.toVector());
        double dist = passo.length();
        if (dist < 0.1) return;
        passo.normalize().multiply(0.4);
        Location l = de.clone();
        for (double d = 0; d < dist; d += 0.4) {
            l.getWorld().spawnParticle(particula, l, 1, 0, 0, 0, 0);
            l.add(passo);
        }
    }

    /** Avatar Elemental: golpes corpo a corpo queimam, congelam e empurram. */
    public void golpeDoAvatar(Player p, LivingEntity alvo) {
        if (Bukkit.getCurrentTick() >= arcano().perfil(p).avatarAte || !inimigo(p, alvo)) return;
        alvo.setFireTicks(Math.max(alvo.getFireTicks(), 60));
        alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), alvo.getMaxFreezeTicks() + 40));
        empurrar(alvo, p.getLocation(), 0.9, 0.35);
        alvo.getWorld().spawnParticle(Particle.SNOWFLAKE, meio(alvo), 8, 0.3, 0.4, 0.3, 0.02);
        alvo.getWorld().spawnParticle(Particle.FLAME, meio(alvo), 8, 0.3, 0.4, 0.3, 0.02);
    }

    // ---------- Fardo Fértil ----------

    private boolean fardoFertil(Player p) {
        RayTraceResult rt = p.rayTraceBlocks(6);
        if (rt == null || rt.getHitBlock() == null || rt.getHitBlockFace() == null) {
            falhar(p, "Olhe para um bloco próximo (até 6 blocos).");
            return false;
        }
        Block b = rt.getHitBlock().getRelative(rt.getHitBlockFace());
        if (!b.isEmpty() || !arcano().temporarios().podeConstruir(p, b)) {
            falhar(p, "Não dá para criar o fardo ali.");
            return false;
        }
        long meus = fardos.values().stream().filter(f -> f.dono().equals(p.getUniqueId())).count();
        if (meus >= MAX_FARDOS_POR_JOGADOR) {
            falhar(p, "Você já tem " + MAX_FARDOS_POR_JOGADOR + " Fardos Férteis ativos.");
            return false;
        }
        b.setType(Material.HAY_BLOCK);
        fardos.put(b.getLocation(), new Fardo(p.getUniqueId(), System.currentTimeMillis() + DURACAO_FARDO_MS));
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 1, 0.5), 30, 0.6, 0.5, 0.6);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GRASS_PLACE, 1f, 0.8f);
        p.sendActionBar(Component.text("❤ Fardo Fértil ativo por 20 minutos (raio de 6 blocos).", Essencia.VIDA.cor()));
        return true;
    }

    /** Roda a cada 2 segundos: os fardos fazem as plantações em volta crescerem. */
    public void tickFardos() {
        long agora = System.currentTimeMillis();
        Iterator<Map.Entry<Location, Fardo>> it = fardos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, Fardo> en = it.next();
            Location l = en.getKey();
            if (!l.isChunkLoaded()) continue;
            Block fardo = l.getBlock();
            if (fardo.getType() != Material.HAY_BLOCK || agora > en.getValue().expira()) {
                it.remove();
                l.getWorld().spawnParticle(Particle.POOF, l.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
                continue;
            }
            l.getWorld().spawnParticle(Particle.COMPOSTER, l.clone().add(0.5, 1.1, 0.5), 4, 0.3, 0.1, 0.3, 0);
            for (int i = 0; i < 8; i++) {
                Block b = fardo.getRelative(rnd().nextInt(-6, 7), rnd().nextInt(-1, 2), rnd().nextInt(-6, 7));
                if (crescer(b)) {
                    b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.6, 0.5), 3, 0.2, 0.2, 0.2);
                }
            }
        }
    }

    /** Faz a planta avançar um estágio. */
    static boolean crescer(Block b) {
        if (!(b.getBlockData() instanceof Ageable a) || a.getAge() >= a.getMaximumAge()) return false;
        if (!Tag.CROPS.isTagged(b.getType()) && b.getType() != Material.SWEET_BERRY_BUSH
                && b.getType() != Material.NETHER_WART && b.getType() != Material.COCOA) {
            return false;
        }
        a.setAge(a.getAge() + 1);
        b.setBlockData(a);
        return true;
    }

    // =====================================================================
    //  Peças reutilizadas
    // =====================================================================

    RayTraceResult mirar(Player p, double alcance) {
        return p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), alcance,
                FluidCollisionMode.NEVER, true, 0.3, e -> e instanceof LivingEntity && e != p && !(e instanceof ArmorStand));
    }

    /** Projétil feito de partículas que voa em linha reta. */
    private void lancarProjetil(Player p, Set<Essencia> es, double velocidade, int maxTicks, Consumer<Impacto> aoImpactar) {
        World w = p.getWorld();
        Location pos = p.getEyeLocation();
        Vector dir = pos.getDirection().normalize();
        w.playSound(pos, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.6f);
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                RayTraceResult r = w.rayTrace(pos, dir, velocidade, FluidCollisionMode.NEVER, true, 0.35,
                        e -> e instanceof LivingEntity && e != p && !(e instanceof ArmorStand) && e.isValid());
                if (r != null) {
                    Location l = r.getHitPosition().toLocation(w);
                    aoImpactar.accept(new Impacto(l, r.getHitEntity() instanceof LivingEntity le ? le : null,
                            r.getHitBlock(), r.getHitBlockFace()));
                    cancel();
                    return;
                }
                pos.add(dir.clone().multiply(velocidade));
                for (Essencia e : es) {
                    w.spawnParticle(Particle.DUST, pos, 3, 0.08, 0.08, 0.08, 0, e.poeira(1.3f));
                }
                if (++t >= maxTicks || !pos.isChunkLoaded()) {
                    aoImpactar.accept(new Impacto(pos.clone(), null, null, null));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Área que aplica as essências por um tempo (inimigos apanham, aliados recebem ajuda). */
    void zona(Player p, Location centro, double raio, int duracao, Set<Essencia> es, double pot) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > duracao || !centro.isChunkLoaded()) { cancel(); return; }
                anel(centro, raio, es);
                if (t % 20 == 0) {
                    for (LivingEntity e : perto(centro, raio)) {
                        if (p.isOnline() && inimigo(p, e)) atingir(p, e, es, pot, centro);
                        else ajudar(e, es, pot);
                    }
                }
                t += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void zonaVisual(Location centro, Particle particula, int duracao) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > duracao || !centro.isChunkLoaded()) { cancel(); return; }
                centro.getWorld().spawnParticle(particula, centro, 2, 0.3, 0.3, 0.3, 0.01);
                t += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /** Coluna de vento que lança para cima quem entrar. */
    private void correnteDeAr(Location centro, int duracao) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > duracao || !centro.isChunkLoaded()) { cancel(); return; }
                World w = centro.getWorld();
                w.spawnParticle(Particle.CLOUD, centro.clone().add(0, 1.5, 0), 8, 0.4, 1.5, 0.4, 0.05);
                if (t % 10 == 0) w.spawnParticle(Particle.GUST, centro, 1);
                for (Entity e : w.getNearbyEntities(centro.clone().add(0, 2, 0), 1.2, 3, 1.2)) {
                    if (!(e instanceof LivingEntity le)) continue;
                    Vector v = le.getVelocity();
                    le.setVelocity(v.setY(Math.max(v.getY(), 0.9)));
                    le.setFallDistance(0);
                    efeito(le, PotionEffectType.SLOW_FALLING, 60, 0);
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Puxa as criaturas para o centro; no final roda {@code aoFim}. */
    void vortice(Player p, Location centro, double raio, int duracao, double forca, Runnable aoFim) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t > duracao || !centro.isChunkLoaded()) {
                    cancel();
                    if (aoFim != null) aoFim.run();
                    return;
                }
                World w = centro.getWorld();
                w.spawnParticle(Particle.REVERSE_PORTAL, centro, 20, raio / 3, 0.6, raio / 3, 0.05);
                w.spawnParticle(Particle.SQUID_INK, centro, 4, 0.3, 0.3, 0.3, 0.01);
                if (t % 20 == 0) w.playSound(centro, Sound.BLOCK_PORTAL_TRIGGER, 0.3f, 0.6f);
                for (LivingEntity e : perto(centro, raio)) {
                    if (p.isOnline() && inimigo(p, e)) puxar(e, centro, forca);
                }
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Congela a água por onde o jogador anda. */
    private void passosGelidos(Player p, int duracao) {
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || t > duracao) { cancel(); return; }
                Block pe = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                    Block b = pe.getRelative(dx, 0, dz);
                    if (b.getType() == Material.WATER && b.getBlockData() instanceof Levelled lv && lv.getLevel() == 0
                            && b.getRelative(BlockFace.UP).isEmpty()) {
                        arcano().temporarios().colocar(p, b, Material.FROSTED_ICE, 160);
                    }
                }
                t += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    /** Teleporte curto para a frente, parando antes de paredes. */
    void piscar(Player p, double distancia) {
        World w = p.getWorld();
        Location origem = p.getLocation();
        Vector dir = origem.getDirection().normalize();
        RayTraceResult r = w.rayTraceBlocks(p.getEyeLocation(), dir, distancia, FluidCollisionMode.NEVER, true);
        double d = r == null ? distancia : Math.max(0, r.getHitPosition().distance(p.getEyeLocation().toVector()) - 0.8);
        Location destino = origem.clone();
        while (d > 0) {
            destino = origem.clone().add(dir.clone().multiply(d));
            if (destino.getBlock().isPassable() && destino.clone().add(0, 1, 0).getBlock().isPassable()) break;
            d -= 0.5;
        }
        if (d <= 0) return;
        w.spawnParticle(Particle.PORTAL, origem.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
        p.teleport(destino);
        p.setFallDistance(0);
        w.spawnParticle(Particle.REVERSE_PORTAL, destino.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.05);
        w.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
    }

    /** Teleporta o jogador para cima do bloco, se houver espaço. */
    void teleportarPara(Player p, Block b) {
        Block pe = b.getRelative(BlockFace.UP);
        if (!pe.isPassable() || !pe.getRelative(BlockFace.UP).isPassable()) return;
        Location destino = pe.getLocation().add(0.5, 0, 0.5);
        destino.setYaw(p.getLocation().getYaw());
        destino.setPitch(p.getLocation().getPitch());
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.3);
        p.teleport(destino);
        p.setFallDistance(0);
        p.getWorld().playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
    }

    // ---------- alvos ----------

    static List<LivingEntity> perto(Location l, double raio) {
        List<LivingEntity> lista = new ArrayList<>();
        for (Entity e : l.getWorld().getNearbyEntities(l, raio, raio, raio)) {
            if (e instanceof LivingEntity le && !(e instanceof ArmorStand) && le.isValid()
                    && e.getLocation().distanceSquared(l) <= raio * raio) {
                lista.add(le);
            }
        }
        return lista;
    }

    /** Dá para atacar? (não ataca você, seus pets nem jogadores com PvP desligado) */
    static boolean inimigo(Player p, LivingEntity e) {
        if (e == p || !e.isValid() || e instanceof ArmorStand) return false;
        if (e instanceof Tameable t && t.isTamed() && p.getUniqueId().equals(t.getOwnerUniqueId())) return false;
        // Invocações: as suas e as de aliados não são alvo.
        String dono = e.getPersistentDataContainer().get(CHAVE_INVOCACAO, org.bukkit.persistence.PersistentDataType.STRING);
        if (dono != null) {
            if (dono.equals(p.getUniqueId().toString())) return false;
            try {
                Player d = Bukkit.getPlayer(UUID.fromString(dono));
                if (d != null && !inimigoPvp(p, d)) return false;
            } catch (IllegalArgumentException ignored) { }
        }
        if (e instanceof Player outro) return inimigoPvp(p, outro);
        return true;
    }

    /** Regra do mundo, party (fogo amigo) e PvP dos territórios. */
    private static boolean inimigoPvp(Player p, Player outro) {
        return outro != p && RPGAtributos.instancia().pvpPermitido(p, outro) && outro.getGameMode() != GameMode.CREATIVE
                && outro.getGameMode() != GameMode.SPECTATOR;
    }

    private static boolean sensivelAgua(LivingEntity e) {
        EntityType t = e.getType();
        return t == EntityType.ENDERMAN || t == EntityType.BLAZE || t == EntityType.SNOW_GOLEM || t == EntityType.STRIDER;
    }

    void ferir(Player p, LivingEntity alvo, double dano) {
        if (dano <= 0 || alvo.isDead()) return;
        if (alvo instanceof Player) dano *= cfg().arcDanoPvp;
        plugin.alvos().ferir(p, alvo, dano, DamageSource.builder(DamageType.MAGIC).withCausingEntity(p).withDirectEntity(p).build());
    }

    static void curar(LivingEntity e, double quanto) {
        if (quanto <= 0 || e.isDead()) return;
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max == null) return;
        e.setHealth(Math.min(max.getValue(), e.getHealth() + quanto));
    }

    static void efeito(LivingEntity e, PotionEffectType tipo, double ticks, int nivel) {
        e.addPotionEffect(new PotionEffect(tipo, (int) Math.round(ticks), nivel));
    }

    static void empurrar(LivingEntity e, Location de, double forca, double y) {
        Vector v = e.getLocation().toVector().subtract(de.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1));
        e.setVelocity(v.normalize().multiply(forca).setY(y));
    }

    static void puxar(LivingEntity e, Location para, double forca) {
        Vector v = para.toVector().subtract(e.getLocation().toVector());
        if (v.lengthSquared() < 1) return;
        e.setVelocity(v.normalize().multiply(forca).setY(Math.max(0.1, v.getY() * 0.1)));
    }

    // ---------- visual ----------

    static Location meio(Entity e) {
        return e.getLocation().add(0, e.getHeight() / 2, 0);
    }

    static void brilho(Location l, Set<Essencia> es, int qtd) {
        World w = l.getWorld();
        for (Essencia e : es) {
            w.spawnParticle(Particle.DUST, l, qtd, 0.4, 0.4, 0.4, 0, e.poeira(1.4f));
            Particle extra = switch (e) {
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
            w.spawnParticle(extra, l, Math.max(2, qtd / 3), 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Brilho com a assinatura visual do jogador por cima. */
    static void brilho(Location l, Set<Essencia> es, int qtd, Estilo estilo) {
        brilho(l, es, qtd);
        if (estilo != null && estilo.particula() != null) {
            l.getWorld().spawnParticle(estilo.particula(), l, Math.max(3, qtd / 2), 0.4, 0.4, 0.4, 0.02);
        }
    }

    static void anelParticula(Location centro, double raio, Particle particula) {
        int pontos = (int) (raio * 8);
        for (int i = 0; i < pontos; i++) {
            double a = 2 * Math.PI * i / pontos;
            centro.getWorld().spawnParticle(particula, centro.clone().add(Math.cos(a) * raio, 0, Math.sin(a) * raio), 1, 0, 0, 0, 0);
        }
    }

    static void feixe(Location de, Location ate, Set<Essencia> es) {
        Vector passo = ate.toVector().subtract(de.toVector());
        double dist = passo.length();
        if (dist < 0.1) return;
        passo.normalize().multiply(0.3);
        Location l = de.clone();
        List<Essencia> lista = new ArrayList<>(es);
        for (int i = 0; i * 0.3 < dist; i++) {
            Essencia e = lista.get(i % lista.size());
            l.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, e.poeira(1f));
            l.add(passo);
        }
    }

    static void anel(Location centro, double raio, Set<Essencia> es) {
        List<Essencia> lista = new ArrayList<>(es);
        int pontos = (int) (raio * 8);
        for (int i = 0; i < pontos; i++) {
            double a = 2 * Math.PI * i / pontos;
            Location l = centro.clone().add(Math.cos(a) * raio, 0.1, Math.sin(a) * raio);
            centro.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, lista.get(i % lista.size()).poeira(1.1f));
        }
    }

    static void espiral(Player p, Set<Essencia> es) {
        List<Essencia> lista = new ArrayList<>(es);
        Location base = p.getLocation();
        for (int i = 0; i < 40; i++) {
            double a = i * 0.5;
            Location l = base.clone().add(Math.cos(a) * 0.8, i * 0.05, Math.sin(a) * 0.8);
            p.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, lista.get(i % lista.size()).poeira(1.2f));
        }
    }
}
