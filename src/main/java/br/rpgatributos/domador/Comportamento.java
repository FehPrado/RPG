package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Strider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O que os companheiros fazem sozinhos (seguir, ficar, guardar, patrulhar, lutar, coletar)
 * e o controle da montaria pelo teclado de quem monta (andar, pular, voar).
 */
public final class Comportamento implements Listener {

    /** Criaturas que já sabem atacar: só precisam de um alvo. */
    private static final Set<EntityType> LUTADORES = EnumSet.of(
            EntityType.WOLF, EntityType.IRON_GOLEM, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM,
            EntityType.GOAT, EntityType.LLAMA, EntityType.TRADER_LLAMA);

    private final RPGAtributos plugin;
    /** Patrulha: para onde cada um está indo e até quando. */
    private final Map<UUID, Location> pontos = new HashMap<>();
    private final Map<UUID, Long> trocarPonto = new HashMap<>();
    private final Map<UUID, Long> proximoGolpe = new HashMap<>();
    /** Companheiros com alguém montado (controlados a cada tick). */
    private final Set<UUID> montados = new HashSet<>();
    private final Map<UUID, Location> ultimaPosicao = new HashMap<>();
    private final Map<UUID, Long> avisoDescer = new HashMap<>();
    private int ciclo;

    public Comportamento(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Companheiros comp() { return plugin.companheiros(); }
    private Settings cfg() { return plugin.settings(); }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static Player montador(Entity e) {
        for (Entity p : e.getPassengers()) if (p instanceof Player pl) return pl;
        return null;
    }

    private static boolean ganhaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    // =====================================================================
    //  Clique no companheiro: montar ou abrir o menu
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoClicar(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof LivingEntity le) || !Companheiros.eh(le)) return;
        Player p = e.getPlayer();
        if (!comp().eDono(p, le)) return;
        // Com algo na mão, vale o normal do jogo (dar comida, tosquiar, ordenhar...).
        if (!p.getInventory().getItemInMainHand().isEmpty()) return;
        // Cavalo e outros que o jogo já deixa montar (sem a Sela do Domador): monta do jeito normal.
        if (!p.isSneaking() && !comp().tem(le, Componente.SELA) && (le instanceof AbstractHorse || selado(le))) return;
        e.setCancelled(true);
        if (!p.isSneaking() && comp().tem(le, Componente.SELA) && le.getPassengers().isEmpty()) {
            le.addPassenger(p);
            return;
        }
        plugin.menusDomador().abrirCompanheiro(p, le);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMontar(EntityMountEvent e) {
        if (!(e.getEntity() instanceof Player p) || !Companheiros.eh(e.getMount())) return;
        UUID dono = Companheiros.dono(e.getMount());
        Party pt = plugin.parties().party(p);
        boolean pode = p.getUniqueId().equals(dono) || (pt != null && pt.tem(dono));
        if (!pode) {
            e.setCancelled(true);
            p.sendActionBar(Component.text("✖ Esse companheiro não é seu.", NamedTextColor.RED));
            return;
        }
        if (e.getMount() instanceof Sittable s) s.setSitting(false);
        montados.add(e.getMount().getUniqueId());
        ultimaPosicao.put(e.getMount().getUniqueId(), e.getMount().getLocation());
        if (comp().tem(e.getMount(), Componente.ASAS)) {
            p.sendActionBar(Component.text("♞ Espaço sobe, agachar desce. Olhe para cima/baixo para subir ou descer voando.", Companheiros.COR));
        }
    }

    /** Voando, agachar desce em vez de desmontar (desmonta quando encosta no chão). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDesmontar(EntityDismountEvent e) {
        Entity montaria = e.getDismounted();
        if (!(e.getEntity() instanceof Player p) || !Companheiros.eh(montaria)) return;
        if (e.isCancellable() && p.isOnline() && !p.isDead() && comp().tem(montaria, Componente.ASAS)
                && !montaria.isOnGround() && !montaria.isInWater()) {
            e.setCancelled(true);
            long agora = System.currentTimeMillis();
            if (agora - avisoDescer.getOrDefault(p.getUniqueId(), 0L) > 4000) {
                avisoDescer.put(p.getUniqueId(), agora);
                p.sendActionBar(Component.text("♞ Desça até o chão para desmontar.", Companheiros.COR));
            }
            return;
        }
        montaria.setGravity(true);
        montados.remove(montaria.getUniqueId());
        ultimaPosicao.remove(montaria.getUniqueId());
    }

    // =====================================================================
    //  Montaria: a cada tick
    // =====================================================================

    public void controlarMontarias() {
        for (Iterator<UUID> it = montados.iterator(); it.hasNext(); ) {
            Entity ent = Bukkit.getEntity(it.next());
            if (!(ent instanceof Mob e) || !e.isValid()) {
                it.remove();
                continue;
            }
            Player p = montador(e);
            if (p == null) {
                e.setGravity(true);
                it.remove();
                continue;
            }
            Set<Componente> c = comp().componentes(e);
            if (!c.contains(Componente.SELA) || controleNativo(e, p)) continue;
            guiar(e, p, c);
        }
    }

    /** Está com a sela comum do jogo? */
    private static boolean selado(LivingEntity e) {
        EntityEquipment eq = e.getEquipment();
        return eq != null && e.canUseEquipmentSlot(EquipmentSlot.SADDLE) && !eq.getItem(EquipmentSlot.SADDLE).isEmpty();
    }

    /** Cavalo com sela comum, porco com cenoura no palito...: o próprio jogo controla. */
    private static boolean controleNativo(Mob e, Player p) {
        boolean selado = selado(e);
        if (e instanceof Pig) return selado && segura(p, Material.CARROT_ON_A_STICK);
        if (e instanceof Strider) return selado && segura(p, Material.WARPED_FUNGUS_ON_A_STICK);
        if (e.getType() == EntityType.HAPPY_GHAST) return !e.getEquipment().getItem(EquipmentSlot.BODY).isEmpty();
        return selado;
    }

    private static boolean segura(Player p, Material m) {
        return p.getInventory().getItemInMainHand().getType() == m || p.getInventory().getItemInOffHand().getType() == m;
    }

    private void guiar(Mob e, Player p, Set<Componente> c) {
        Input in = p.getCurrentInput();
        double yaw = Math.toRadians(p.getYaw());
        Vector frente = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector direita = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector mov = new Vector();
        if (in.isForward()) mov.add(frente);
        if (in.isBackward()) mov.subtract(frente.clone().multiply(0.6));
        if (in.isRight()) mov.add(direita);
        if (in.isLeft()) mov.subtract(direita);

        int nivel = plugin.stats().getNivel(p, Skill.DOMA);
        double vel = 0.28 * (1 + nivel * cfg().domaVelocidade) * (c.contains(Componente.AGILIDADE) ? 1.3 : 1)
                * (in.isSprint() ? 1.25 : 1);
        boolean naAgua = e.isInWater();
        if (naAgua) vel *= c.contains(Componente.MARE) ? 1.4 : 0.45;
        boolean voando = c.contains(Componente.ASAS) && !naAgua && (!e.isOnGround() || in.isJump());
        if (voando) vel *= 1.5;
        if (mov.lengthSquared() > 0) mov.normalize().multiply(vel);

        double y;
        if (voando) {
            e.setGravity(false);
            if (in.isJump()) y = 0.4;
            else if (in.isSneak()) y = -0.45;
            else if (in.isForward()) y = -Math.sin(Math.toRadians(p.getPitch())) * vel * 0.9; // olhar para cima sobe
            else y = 0;
        } else {
            e.setGravity(true);
            y = e.getVelocity().getY();
            if (in.isJump() && e.isOnGround()) y = 0.55;
            else if (in.isJump() && naAgua) y = 0.2;
        }
        e.getPathfinder().stopPathfinding();
        e.setVelocity(new Vector(mov.getX(), y, mov.getZ()));
        e.setRotation(p.getYaw(), 0);
    }

    // =====================================================================
    //  Comportamento: a cada 10 ticks
    // =====================================================================

    public void tick() {
        ciclo++;
        for (UUID id : new ArrayList<>(comp().ativos())) {
            if (!(Bukkit.getEntity(id) instanceof Mob e) || !e.isValid()) continue;
            Set<Componente> c = comp().componentes(e);
            Player montado = montador(e);
            if (montado != null) {
                montado(e, montado, c);
                continue;
            }
            if (ciclo % 2 == 0 && c.contains(Componente.COLETOR) && c.contains(Componente.ALFORJE)) coletar(e);
            switch (comp().modo(e)) {
                case SEGUIR -> seguir(e, c);
                case FICAR -> voltarAoCentro(e, true);
                case GUARDAR -> guardar(e, c);
                case PATRULHAR -> patrulhar(e, c);
                case SOLTO -> { }
            }
        }
    }

    private void montado(Mob e, Player p, Set<Componente> c) {
        if (c.contains(Componente.FAROL)) refrescar(p, PotionEffectType.NIGHT_VISION, 300);
        if (c.contains(Componente.MARE) && (e.isInWater() || p.isInWater())) {
            refrescar(p, PotionEffectType.WATER_BREATHING, 100);
            refrescar(p, PotionEffectType.DOLPHINS_GRACE, 100);
        }
        // XP de Doma por distância montado (só andando de verdade, com tecla apertada).
        Location antes = ultimaPosicao.put(e.getUniqueId(), e.getLocation());
        Input in = p.getCurrentInput();
        boolean andando = in.isForward() || in.isBackward() || in.isLeft() || in.isRight() || in.isJump();
        if (antes != null && andando && ganhaXp(p) && antes.getWorld().equals(e.getWorld())) {
            double dist = Math.min(8, antes.distance(e.getLocation()));
            if (dist > 0.3) plugin.stats().darXp(p, Skill.DOMA, dist * cfg().domaXpPorBloco);
        }
    }

    private static void refrescar(Player p, PotionEffectType tipo, int ticks) {
        PotionEffect atual = p.getPotionEffect(tipo);
        if (atual == null || atual.getDuration() < ticks - 40) p.addPotionEffect(new PotionEffect(tipo, ticks, 0, true, false, true));
    }

    private void seguir(Mob e, Set<Componente> c) {
        Player d = Bukkit.getPlayer(Companheiros.dono(e));
        if (d == null || !d.getWorld().equals(e.getWorld()) || d.isDead()) return;
        if (e instanceof Sittable s && s.isSitting()) s.setSitting(false);
        // Guarda-costas: ataca quem está atrás do dono.
        if (podeLutar(e, c)) {
            LivingEntity alvo = inimigoPerto(d.getLocation(), 8, d);
            if (alvo != null) {
                atacar(e, alvo);
                return;
            }
        }
        double dist2 = e.getLocation().distanceSquared(d.getLocation());
        if (dist2 > 3.5 * 3.5) e.getPathfinder().moveTo(d, dist2 > 20 * 20 ? 1.5 : 1.2);
        else if (dist2 < 2 * 2) e.getPathfinder().stopPathfinding();
    }

    /** Ficar (e voltar depois de lutar): anda de volta para o centro; perto dele, senta (quem senta). */
    private void voltarAoCentro(Mob e, boolean sentar) {
        Location centro = comp().centro(e);
        if (e.getLocation().distanceSquared(centro) > 2.5 * 2.5) {
            if (e instanceof Sittable s) s.setSitting(false);
            e.setTarget(null);
            e.getPathfinder().moveTo(centro, 1.1);
        } else if (sentar && e instanceof Sittable s) {
            s.setSitting(true);
        } else if (sentar) {
            e.getPathfinder().stopPathfinding();
        }
    }

    private double raioGuarda(Mob e) {
        return 6 + comp().nivelDono(e) / (double) cfg().nivelMaximo * 10;
    }

    private double raioPatrulha(Mob e) {
        return 8 + comp().nivelDono(e) / (double) cfg().nivelMaximo * 16;
    }

    private void guardar(Mob e, Set<Componente> c) {
        if (e instanceof Sittable s && s.isSitting()) s.setSitting(false);
        Location centro = comp().centro(e);
        double raio = raioGuarda(e);
        if (podeLutar(e, c) && e.getLocation().distanceSquared(centro) < (raio + 6) * (raio + 6)) {
            LivingEntity alvo = inimigoPerto(centro, raio, null);
            if (alvo != null) {
                atacar(e, alvo);
                return;
            }
        }
        voltarAoCentro(e, false);
    }

    private void patrulhar(Mob e, Set<Componente> c) {
        if (e instanceof Sittable s && s.isSitting()) s.setSitting(false);
        Location centro = comp().centro(e);
        double raio = raioPatrulha(e);
        if (podeLutar(e, c) && e.getLocation().distanceSquared(centro) < (raio + 6) * (raio + 6)) {
            LivingEntity alvo = inimigoPerto(centro, raio + 4, null);
            if (alvo != null) {
                atacar(e, alvo);
                return;
            }
        }
        UUID id = e.getUniqueId();
        long agora = System.currentTimeMillis();
        Location ponto = pontos.get(id);
        boolean trocar = ponto == null || !ponto.getWorld().equals(e.getWorld())
                || e.getLocation().distanceSquared(ponto) < 2.5 * 2.5 || agora > trocarPonto.getOrDefault(id, 0L);
        if (trocar) {
            ponto = pontoAleatorio(centro, raio);
            pontos.put(id, ponto);
            trocarPonto.put(id, agora + 10_000);
            e.setTarget(null);
            e.getPathfinder().moveTo(ponto, 0.9);
        } else if (!e.getPathfinder().hasPath()) {
            e.getPathfinder().moveTo(ponto, 0.9);
        }
    }

    /** Um lugar para andar na patrulha: chão firme com espaço para a criatura. */
    private static Location pontoAleatorio(Location centro, double raio) {
        World w = centro.getWorld();
        for (int tentativa = 0; tentativa < 8; tentativa++) {
            double ang = rnd().nextDouble() * Math.PI * 2, d = rnd().nextDouble() * raio;
            int x = (int) Math.floor(centro.getX() + Math.cos(ang) * d);
            int z = (int) Math.floor(centro.getZ() + Math.sin(ang) * d);
            for (int y = centro.getBlockY() + 4; y >= centro.getBlockY() - 6; y--) {
                Block pe = w.getBlockAt(x, y, z);
                if (pe.isPassable() && pe.getRelative(0, 1, 0).isPassable() && pe.getRelative(0, -1, 0).getType().isSolid()) {
                    return pe.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return centro;
    }

    // =====================================================================
    //  Luta
    // =====================================================================

    private static boolean podeLutar(Mob e, Set<Componente> c) {
        return c.contains(Componente.GARRAS) || LUTADORES.contains(e.getType());
    }

    /**
     * Monstro mais perto do ponto, dentro do raio. Com {@code protegido}, só os que estão
     * mirando nele ou já muito perto dele (guarda-costas).
     */
    private static LivingEntity inimigoPerto(Location l, double raio, Player protegido) {
        LivingEntity melhor = null;
        double melhorDist = Double.MAX_VALUE;
        for (LivingEntity v : l.getWorld().getNearbyLivingEntities(l, raio)) {
            if (!(v instanceof Enemy) || v.isDead() || Companheiros.eh(v)) continue;
            double d2 = v.getLocation().distanceSquared(l);
            if (protegido != null && !(v instanceof Mob m && protegido.equals(m.getTarget())) && d2 > 4 * 4) continue;
            if (d2 < melhorDist) {
                melhor = v;
                melhorDist = d2;
            }
        }
        return melhor;
    }

    private void atacar(Mob e, LivingEntity alvo) {
        if (e instanceof Sittable s && s.isSitting()) s.setSitting(false);
        if (LUTADORES.contains(e.getType())) {
            if (e.getTarget() != alvo) e.setTarget(alvo);
            return;
        }
        // Quem não sabe atacar sozinho (vaca com Garras...): chega perto e bate.
        double alcance = 1.6 + e.getWidth() / 2 + alvo.getWidth() / 2;
        if (e.getLocation().distanceSquared(alvo.getLocation()) > alcance * alcance) {
            e.getPathfinder().moveTo(alvo, 1.3);
            return;
        }
        long agora = System.currentTimeMillis();
        if (agora < proximoGolpe.getOrDefault(e.getUniqueId(), 0L)) return;
        proximoGolpe.put(e.getUniqueId(), agora + 1000);
        e.getPathfinder().stopPathfinding();
        e.lookAt(alvo);
        e.swingMainHand();
        alvo.damage(comp().danoAtaque(e), e);
        Vector empurrao = alvo.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);
        if (empurrao.lengthSquared() > 1e-4) alvo.setVelocity(alvo.getVelocity().add(empurrao.normalize().multiply(0.4).setY(0.25)));
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 1.2f);
    }

    // =====================================================================
    //  Coletor
    // =====================================================================

    private void coletar(Mob e) {
        UUID dono = Companheiros.dono(e);
        Inventory inv = comp().alforje(e);
        boolean pegou = false;
        for (Item item : e.getWorld().getNearbyEntitiesByType(Item.class, e.getLocation(), 3.5)) {
            if (!item.isValid() || item.getPickupDelay() > 0 || item.getTicksLived() < 20) continue;
            if (dono != null && !plugin.territorios().podeConstruir(dono, item.getLocation())) continue; // nada de pegar no território dos outros
            ItemStack s = item.getItemStack();
            Map<Integer, ItemStack> sobra = inv.addItem(s.clone());
            if (sobra.isEmpty()) {
                item.remove();
                pegou = true;
            } else {
                int restou = sobra.values().iterator().next().getAmount();
                if (restou < s.getAmount()) {
                    s.setAmount(restou);
                    item.setItemStack(s);
                    pegou = true;
                }
            }
        }
        if (pegou) {
            comp().salvarAlforje(e.getUniqueId());
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.2f);
        }
    }

    /** Ao desligar: ninguém fica flutuando sem gravidade. */
    public void parar() {
        for (UUID id : montados) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.setGravity(true);
        }
        montados.clear();
    }
}
