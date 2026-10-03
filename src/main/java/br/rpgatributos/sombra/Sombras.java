package br.rpgatributos.sombra;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.classe.Classe;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O exército do Soberano das Sombras: quem ele derrota deixa uma sombra por alguns
 * segundos; "Levante-se" a transforma num soldado guardado para sempre, que luta ao lado
 * dele e volta para a sombra quando cai (dá para chamar de novo).
 */
public final class Sombras implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x7E57C2);
    private static final NamespacedKey K_SOMBRA = new NamespacedKey("rpgatributos", "sombra_dono");
    /** A mesma chave das invocações do Arcano: magias, soldados da colônia e afins já sabem que é aliado. */
    private static final NamespacedKey K_INVOCACAO = new NamespacedKey("rpgatributos", "invocacao_dono");
    private static final NamespacedKey K_EXERCITO = new NamespacedKey("rpgatributos", "exercito_sombras");
    private static final long CADAVER_MS = 30_000;
    private static final int S_INFO = 49, S_RECOLHER = 48, S_FECHAR = 53;

    /** Só criaturas que andam e lutam bem viram sombras (nada de creeper explodindo no dono). */
    private static final Set<EntityType> PERMITIDOS = EnumSet.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.BOGGED, EntityType.WITHER_SKELETON,
            EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.RAVAGER,
            EntityType.BLAZE, EntityType.BREEZE, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.ENDERMAN, EntityType.IRON_GOLEM);

    /** Um soldado guardado: o tipo, o grau (0 comum, 1 elite, 2 marechal) e o chefe de origem. */
    record Soldado(EntityType tipo, int grau, String chefe) {
        String texto() { return tipo.name() + ":" + grau + ":" + chefe; }

        static Soldado ler(String s) {
            String[] p = s.split(":", -1);
            if (p.length < 2) return null;
            try {
                EntityType t = EntityType.valueOf(p[0]);
                if (!PERMITIDOS.contains(t)) return null;
                return new Soldado(t, Integer.parseInt(p[1]), p.length > 2 ? p[2] : "");
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private record Cadaver(UUID dono, Soldado soldado, Location onde, long ate) {}

    private record Golpe(UUID dono, long quando) {}

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final List<Cadaver> cadaveres = new ArrayList<>();
    /** Sombras em campo: dono → (entidade → índice no exército). */
    private final Map<UUID, Map<UUID, Integer>> campo = new HashMap<>();
    /** Última sombra que acertou cada criatura (para a sombra que mata também deixar cadáver). */
    private final Map<UUID, Golpe> ultimoGolpe = new HashMap<>();
    private final Map<UUID, Long> avisoCadaver = new HashMap<>();
    private int ciclo;

    public Sombras(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Quem é sombra
    // =====================================================================

    public static boolean eh(Entity e) {
        return e != null && e.getPersistentDataContainer().has(K_SOMBRA);
    }

    public static UUID donoDe(Entity e) {
        if (e == null) return null;
        String s = e.getPersistentDataContainer().get(K_SOMBRA, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean soberano(Player p) {
        return p != null && plugin.classes().ativa(p) == Classe.SOBERANO_DAS_SOMBRAS;
    }

    private int nivel(Player p) {
        return plugin.classes().nivel(p, Classe.SOBERANO_DAS_SOMBRAS);
    }

    /** Quantas sombras cabem no exército guardado. */
    public static int capacidade(int nivel) {
        return 5 + nivel;
    }

    /** Quantas lutam ao mesmo tempo. */
    public static int maxEmCampo(int nivel) {
        return 2 + nivel / 3;
    }

    // =====================================================================
    //  Exército guardado (no próprio jogador)
    // =====================================================================

    List<Soldado> exercito(Player p) {
        List<Soldado> l = new ArrayList<>();
        String s = p.getPersistentDataContainer().get(K_EXERCITO, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return l;
        for (String parte : s.split("\\|")) {
            Soldado so = Soldado.ler(parte);
            if (so != null) l.add(so);
        }
        return l;
    }

    private void salvar(Player p, List<Soldado> l) {
        if (l.isEmpty()) {
            p.getPersistentDataContainer().remove(K_EXERCITO);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Soldado s : l) sb.append(sb.isEmpty() ? "" : "|").append(s.texto());
        p.getPersistentDataContainer().set(K_EXERCITO, PersistentDataType.STRING, sb.toString());
    }

    private Map<UUID, Integer> emCampo(UUID dono) {
        Map<UUID, Integer> m = campo.get(dono);
        if (m == null) return Map.of();
        m.keySet().removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || !e.isValid() || e.isDead());
        return m;
    }

    // =====================================================================
    //  Habilidades
    // =====================================================================

    /** Levante-se: os cadáveres perto viram soldados. @return false se não havia nada para levantar. */
    public boolean levantar(Player p) {
        List<Cadaver> meus = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (Cadaver c : cadaveres) {
            if (c.dono.equals(p.getUniqueId()) && c.ate > agora && c.onde.getWorld().equals(p.getWorld())
                    && c.onde.distanceSquared(p.getLocation()) < 16 * 16) meus.add(c);
        }
        if (meus.isEmpty()) {
            falhar(p, "☾ Não há sombras para levantar aqui. Derrote inimigos primeiro.");
            return false;
        }
        int nivel = nivel(p);
        List<Soldado> ex = exercito(p);
        int cap = capacidade(nivel), max = maxEmCampo(nivel);
        int levantados = 0, guardados = 0;
        for (Cadaver c : meus) {
            if (ex.size() >= cap) {
                p.sendMessage(Component.text("☾ Seu exército está cheio (" + cap + "). Liberte sombras em /sombras.", COR));
                break;
            }
            cadaveres.remove(c);
            ex.add(c.soldado);
            World w = c.onde.getWorld();
            w.spawnParticle(Particle.SCULK_SOUL, c.onde.clone().add(0, 0.5, 0), 25, 0.4, 0.6, 0.4, 0.04);
            w.spawnParticle(Particle.SQUID_INK, c.onde.clone().add(0, 0.2, 0), 20, 0.5, 0.1, 0.5, 0.02);
            if (emCampo(p.getUniqueId()).size() < max && invocar(p, c.soldado, ex.size() - 1, c.onde, nivel) != null) levantados++;
            else guardados++;
        }
        if (levantados + guardados == 0) return false;
        salvar(p, ex);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.4f);
        p.showTitle(Title.title(Component.empty(), Component.text("Levante-se.", COR, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1500), Duration.ofMillis(500))));
        p.sendActionBar(Component.text("☾ " + (levantados + guardados) + " sombra(s) se juntaram ao exército"
                + (guardados > 0 ? " (" + guardados + " guardada(s))" : "") + " • " + ex.size() + "/" + cap, COR));
        return true;
    }

    /** Exército das Sombras: chama as guardadas, ou enfurece as que já estão lutando. */
    public boolean exercito(Player p, LivingEntity alvo) {
        Map<UUID, Integer> meus = emCampo(p.getUniqueId());
        if (!meus.isEmpty()) {
            for (UUID id : meus.keySet()) {
                if (!(Bukkit.getEntity(id) instanceof LivingEntity e)) continue;
                e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1, false, true));
                e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1, false, true));
                AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) e.setHealth(Math.min(max.getValue(), e.getHealth() + max.getValue() * 0.3));
                if (alvo != null && e instanceof Mob m && inimigoDaSombra(p.getUniqueId(), alvo)) m.setTarget(alvo);
                e.getWorld().spawnParticle(Particle.SCULK_SOUL, e.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.03);
            }
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_ROAR, 0.7f, 1.3f);
            p.sendActionBar(Component.text("☾ As sombras ficam furiosas!", COR));
            return true;
        }
        List<Soldado> ex = exercito(p);
        if (ex.isEmpty()) {
            falhar(p, "☾ Seu exército está vazio. Derrote inimigos e use Levante-se.");
            return false;
        }
        int nivel = nivel(p), max = maxEmCampo(nivel);
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < ex.size(); i++) ordem.add(i);
        ordem.sort((a, b) -> Integer.compare(ex.get(b).grau, ex.get(a).grau)); // os mais fortes primeiro
        int chamados = 0;
        for (int i = 0; i < ordem.size() && chamados < max; i++) {
            double ang = Math.PI * 2 * chamados / Math.max(1, Math.min(max, ex.size()));
            Location onde = p.getLocation().add(Math.cos(ang) * 2.5, 0, Math.sin(ang) * 2.5);
            if (!onde.getBlock().isPassable() || !onde.clone().add(0, 1, 0).getBlock().isPassable()) onde = p.getLocation();
            if (invocar(p, ex.get(ordem.get(i)), ordem.get(i), onde, nivel) != null) chamados++;
        }
        if (chamados == 0) return false;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.2f);
        p.sendActionBar(Component.text("☾ " + chamados + " sombra(s) saem da sua sombra.", COR));
        return true;
    }

    private static void falhar(Player p, String msg) {
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
    }

    // =====================================================================
    //  Invocar uma sombra
    // =====================================================================

    private LivingEntity invocar(Player dono, Soldado s, int indice, Location onde, int nivel) {
        Class<? extends Entity> cl = s.tipo.getEntityClass();
        if (cl == null || !Mob.class.isAssignableFrom(cl)) return null;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) cl;
        LivingEntity e = onde.getWorld().spawn(onde, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, m -> configurar(m, dono, s, nivel));
        campo.computeIfAbsent(dono.getUniqueId(), k -> new LinkedHashMap<>()).put(e.getUniqueId(), indice);
        e.getWorld().spawnParticle(Particle.SQUID_INK, e.getLocation().add(0, 0.5, 0), 25, 0.3, 0.6, 0.3, 0.03);
        return e;
    }

    static String nome(Soldado s) {
        if (s.grau >= 2) {
            Chefe c = Chefe.porId(s.chefe);
            return "Marechal " + (c != null ? c.nome() : "das Sombras");
        }
        String base = switch (s.tipo) {
            case ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER -> "Morto-vivo";
            case SKELETON, STRAY, BOGGED -> "Arqueiro";
            case WITHER_SKELETON -> "Cavaleiro Negro";
            case SPIDER, CAVE_SPIDER -> "Aranha";
            case VINDICATOR -> "Carrasco";
            case PILLAGER -> "Besteiro";
            case RAVAGER -> "Devastador";
            case BLAZE -> "Chama Negra";
            case BREEZE -> "Vendaval";
            case PIGLIN, PIGLIN_BRUTE, ZOMBIFIED_PIGLIN -> "Bruto";
            case HOGLIN, ZOGLIN -> "Fera";
            case ENDERMAN -> "Andarilho";
            case IRON_GOLEM -> "Colosso";
            default -> "Soldado";
        };
        return base + (s.grau == 1 ? " de Elite" : "");
    }

    private void configurar(LivingEntity e, Player dono, Soldado s, int nivel) {
        String id = dono.getUniqueId().toString();
        e.getPersistentDataContainer().set(K_SOMBRA, PersistentDataType.STRING, id);
        e.getPersistentDataContainer().set(K_INVOCACAO, PersistentDataType.STRING, id);
        e.customName(Component.text("☾ " + nome(s), s.grau >= 2 ? TextColor.color(0xB388FF) : COR, s.grau >= 2 ? TextDecoration.BOLD : TextDecoration.ITALIC)
                .append(Component.text(" de " + dono.getName(), NamedTextColor.DARK_GRAY)));
        e.setCustomNameVisible(s.grau >= 2);
        e.setPersistent(false);
        e.setRemoveWhenFarAway(false);
        e.setCanPickupItems(false);
        e.setSilent(true);
        double f = 1 + 0.04 * nivel;
        Chefe chefe = s.grau >= 2 ? Chefe.porId(s.chefe) : null;
        if (chefe != null) {
            base(e, Attribute.MAX_HEALTH, chefe.vida() * 0.3 * f);
            base(e, Attribute.ATTACK_DAMAGE, chefe.dano() * 0.6 * f);
            base(e, Attribute.SCALE, Math.max(1, chefe.escala() * 0.8));
            base(e, Attribute.ARMOR, chefe.armadura() * 0.5);
            base(e, Attribute.KNOCKBACK_RESISTANCE, 0.8);
        } else {
            double grau = s.grau == 1 ? 1.6 : 1;
            multiplicar(e, Attribute.MAX_HEALTH, f * grau);
            multiplicar(e, Attribute.ATTACK_DAMAGE, f * (s.grau == 1 ? 1.4 : 1));
        }
        AttributeInstance alcance = e.getAttribute(Attribute.FOLLOW_RANGE);
        if (alcance != null) alcance.setBaseValue(24);
        multiplicar(e, Attribute.MOVEMENT_SPEED, 1.1);
        AttributeInstance vida = e.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) e.setHealth(vida.getValue());
        e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        if (e instanceof Zombie z) {
            z.setShouldBurnInDay(false);
            z.setCanBreakDoors(false);
        }
        if (e instanceof AbstractSkeleton sk) sk.setShouldBurnInDay(false);
        if (e instanceof Ageable a) a.setAdult();
        if (e instanceof PiglinAbstract pa) pa.setImmuneToZombification(true);
        if (e instanceof Hoglin h) h.setImmuneToZombification(true);
        if (e instanceof Raider r) {
            r.setCanJoinRaid(false);
            r.setPatrolLeader(false);
        }
        if (e instanceof IronGolem g) g.setPlayerCreated(true);
        EntityEquipment eq = e.getEquipment();
        if (eq != null) {
            if (e instanceof Zombie || e instanceof AbstractSkeleton) {
                ItemStack elmo = new ItemStack(Material.LEATHER_HELMET);
                elmo.editMeta(LeatherArmorMeta.class, m -> m.setColor(Color.fromRGB(0x1A1030)));
                eq.setHelmet(elmo);
            }
            eq.setHelmetDropChance(0);
            eq.setChestplateDropChance(0);
            eq.setLeggingsDropChance(0);
            eq.setBootsDropChance(0);
            eq.setItemInMainHandDropChance(0);
            eq.setItemInOffHandDropChance(0);
        }
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    private static void multiplicar(LivingEntity e, Attribute a, double f) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * f);
    }

    /** Some com as sombras em campo (elas continuam guardadas). */
    public void recolher(UUID dono) {
        Map<UUID, Integer> m = campo.remove(dono);
        if (m == null) return;
        for (UUID id : m.keySet()) {
            Entity e = Bukkit.getEntity(id);
            if (e == null) continue;
            e.getWorld().spawnParticle(Particle.SQUID_INK, e.getLocation().add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.02);
            e.remove();
        }
    }

    public void recolherTodas() {
        for (UUID dono : new ArrayList<>(campo.keySet())) recolher(dono);
        cadaveres.clear();
    }

    // =====================================================================
    //  Quem a sombra pode atacar
    // =====================================================================

    /** Uma sombra de {@code dono} pode atacar {@code alvo}? */
    public boolean inimigoDaSombra(UUID dono, Entity alvo) {
        if (!(alvo instanceof LivingEntity le) || !le.isValid() || le.isDead() || alvo instanceof ArmorStand) return false;
        Player d = Bukkit.getPlayer(dono);
        UUID outra = donoDe(alvo);
        if (outra != null) return !outra.equals(dono) && pvp(d, Bukkit.getPlayer(outra));
        if (alvo instanceof Player p) {
            return !p.getUniqueId().equals(dono) && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) && pvp(d, p);
        }
        if (Companheiros.eh(alvo)) {
            UUID c = Companheiros.dono(alvo);
            return c != null && !c.equals(dono) && pvp(d, Bukkit.getPlayer(c));
        }
        if (alvo instanceof Tameable t && t.isTamed()) return false;
        String inv = alvo.getPersistentDataContainer().get(K_INVOCACAO, PersistentDataType.STRING);
        if (inv != null) {
            try {
                UUID u = UUID.fromString(inv);
                return !u.equals(dono) && pvp(d, Bukkit.getPlayer(u));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        UUID colono = plugin.colonias().colonoDe(alvo);
        if (colono != null) return plugin.reinos().inimigosEmGuerra(dono, colono);
        if (alvo instanceof Enemy || Chefes.ehChefe(alvo) || Chefes.ehLacaio(alvo)) return true;
        // Qualquer bicho que esteja atacando o Soberano também.
        return alvo instanceof Mob m && m.getTarget() != null && m.getTarget().getUniqueId().equals(dono);
    }

    private boolean pvp(Player a, Player b) {
        return a != null && b != null && plugin.pvpPermitido(a, b);
    }

    /** As habilidades de {@code p} podem acertar essa sombra? */
    public boolean inimigoDoJogador(Player p, Entity sombra) {
        UUID dono = donoDe(sombra);
        if (dono == null || dono.equals(p.getUniqueId())) return false;
        return pvp(p, Bukkit.getPlayer(dono));
    }

    // =====================================================================
    //  A cada meio segundo
    // =====================================================================

    public void tick() {
        ciclo++;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Map<UUID, Integer>>> it = campo.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<UUID, Integer>> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            if (!soberano(p)) {
                for (UUID id : en.getValue().keySet()) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null) e.remove();
                }
                it.remove();
                continue;
            }
            Map<UUID, Integer> meus = emCampo(p.getUniqueId());
            if (meus.isEmpty()) {
                it.remove();
                continue;
            }
            List<LivingEntity> inimigos = null;
            for (UUID id : meus.keySet()) {
                if (!(Bukkit.getEntity(id) instanceof Mob s)) continue;
                Location lp = p.getLocation();
                if (!s.getWorld().equals(p.getWorld()) || s.getLocation().distanceSquared(lp) > 32 * 32) {
                    s.teleport(lp);
                    s.setTarget(null);
                    continue;
                }
                LivingEntity alvo = s.getTarget();
                if (alvo != null && (!inimigoDaSombra(p.getUniqueId(), alvo) || !alvo.getWorld().equals(lp.getWorld())
                        || alvo.getLocation().distanceSquared(lp) > 24 * 24)) {
                    s.setTarget(null);
                    alvo = null;
                }
                if (alvo == null) {
                    if (inimigos == null) inimigos = inimigosPerto(p);
                    LivingEntity melhor = null;
                    double d = Double.MAX_VALUE;
                    for (LivingEntity i : inimigos) {
                        double x = i.getLocation().distanceSquared(s.getLocation());
                        if (x < d) { d = x; melhor = i; }
                    }
                    if (melhor != null) s.setTarget(melhor);
                    else if (s.getLocation().distanceSquared(lp) > 5 * 5) s.getPathfinder().moveTo(lp, 1.3);
                }
                if (ciclo % 2 == 0) s.getWorld().spawnParticle(Particle.SQUID_INK, s.getLocation().add(0, 0.3, 0), 1, 0.25, 0.1, 0.25, 0);
            }
        }
        for (Iterator<Cadaver> it = cadaveres.iterator(); it.hasNext(); ) {
            Cadaver c = it.next();
            if (c.ate < agora || Bukkit.getPlayer(c.dono) == null) {
                it.remove();
                continue;
            }
            if (c.onde.isChunkLoaded()) {
                c.onde.getWorld().spawnParticle(Particle.SOUL, c.onde.clone().add(0, 0.3, 0), 2, 0.3, 0.2, 0.3, 0.01);
                if (ciclo % 4 == 0) c.onde.getWorld().spawnParticle(Particle.SQUID_INK, c.onde.clone().add(0, 0.1, 0), 3, 0.4, 0, 0.4, 0);
            }
        }
        if (ciclo % 20 == 0) {
            ultimoGolpe.values().removeIf(g -> agora - g.quando > 15_000);
            avisoCadaver.values().removeIf(t -> agora - t > 60_000);
        }
    }

    private List<LivingEntity> inimigosPerto(Player p) {
        List<LivingEntity> l = new ArrayList<>();
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), 14, 8, 14)) {
            if (eh(e) || !(e instanceof LivingEntity le)) continue;
            // Jogadores: só os de reinos em guerra com o Soberano (os outros só se atacarem; ver aoFerir).
            if (e instanceof Player o && !plugin.reinos().inimigosEmGuerra(p.getUniqueId(), o.getUniqueId())) continue;
            if (!(e instanceof Enemy || Chefes.ehChefe(e) || e instanceof Player
                    || (e instanceof Mob m && m.getTarget() != null && m.getTarget().getUniqueId().equals(p.getUniqueId())))) continue;
            if (inimigoDaSombra(p.getUniqueId(), e)) l.add(le);
        }
        return l;
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    private static Entity fonte(Entity damager) {
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) return atirador;
        return damager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMirar(EntityTargetEvent e) {
        Entity alvo = e.getTarget();
        if (alvo == null) return;
        UUID dono = donoDe(e.getEntity());
        if (dono != null) {
            if (!inimigoDaSombra(dono, alvo)) e.setCancelled(true);
            return;
        }
        // Golens, bonecos de neve e lobos não caçam as sombras sozinhos.
        if (eh(alvo) && (e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman || e.getEntity() instanceof Wolf)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Entity f = fonte(e.getDamager());
        UUID donoAtacante = donoDe(f);
        if (donoAtacante != null) {
            if (!inimigoDaSombra(donoAtacante, e.getEntity())) {
                e.setCancelled(true);
                return;
            }
            ultimoGolpe.put(e.getEntity().getUniqueId(), new Golpe(donoAtacante, System.currentTimeMillis()));
            return;
        }
        if (e.getEntity() instanceof Player vitima && f instanceof Player agressor && vitima != agressor && campo.containsKey(vitima.getUniqueId())) {
            revidar(vitima.getUniqueId(), agressor); // bateram no Soberano: as sombras vão para cima
            return;
        }
        UUID donoVitima = donoDe(e.getEntity());
        if (donoVitima == null) return;
        if (f instanceof Player p) {
            if (!inimigoDoJogador(p, e.getEntity())) e.setCancelled(true);
            else revidar(donoVitima, p);
            return;
        }
        if (Companheiros.eh(f) && donoVitima.equals(Companheiros.dono(f))) {
            e.setCancelled(true);
            return;
        }
        String inv = f.getPersistentDataContainer().get(K_INVOCACAO, PersistentDataType.STRING);
        if (donoVitima.toString().equals(inv)) e.setCancelled(true);
    }

    /** As sombras de {@code dono} atacam quem agrediu (se o PvP deixa). */
    private void revidar(UUID dono, Player agressor) {
        Map<UUID, Integer> m = campo.get(dono);
        if (m == null || !inimigoDaSombra(dono, agressor)) return;
        for (UUID id : m.keySet()) {
            if (Bukkit.getEntity(id) instanceof Mob s && !(s.getTarget() instanceof Player)) s.setTarget(agressor);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        UUID donoSombra = donoDe(morto);
        if (donoSombra != null) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            Map<UUID, Integer> m = campo.get(donoSombra);
            if (m != null) m.remove(morto.getUniqueId());
            morto.getWorld().spawnParticle(Particle.SQUID_INK, morto.getLocation().add(0, 0.5, 0), 20, 0.3, 0.5, 0.3, 0.03);
            Player d = Bukkit.getPlayer(donoSombra);
            if (d != null) d.sendActionBar(Component.text("☾ Uma sombra caiu e voltou para você.", COR));
            return;
        }
        Golpe g = ultimoGolpe.remove(morto.getUniqueId());
        Player dono = morto.getKiller();
        if (!soberano(dono)) dono = g != null && System.currentTimeMillis() - g.quando < 10_000 ? Bukkit.getPlayer(g.dono) : null;
        if (!soberano(dono)) return;
        Soldado s = soldadoDe(morto);
        if (s == null) return;
        int meus = 0;
        for (Cadaver c : cadaveres) if (c.dono.equals(dono.getUniqueId())) meus++;
        if (meus >= 20) return;
        cadaveres.add(new Cadaver(dono.getUniqueId(), s, morto.getLocation().clone(), System.currentTimeMillis() + CADAVER_MS));
        Long ultimo = avisoCadaver.get(dono.getUniqueId());
        if (ultimo == null || System.currentTimeMillis() - ultimo > 8_000 || s.grau >= 2) {
            avisoCadaver.put(dono.getUniqueId(), System.currentTimeMillis());
            dono.sendActionBar(Component.text(s.grau >= 2 ? "☾ A sombra de um chefe espera o seu chamado... (agache + F)"
                    : "☾ Uma sombra pode ser levantada (Levante-se: agache + F)", COR));
        }
    }

    /** Que soldado esse morto vira (ou null se não vira sombra). */
    private static Soldado soldadoDe(LivingEntity morto) {
        if (Chefes.ehLacaio(morto)) return null;
        if (Chefes.ehChefe(morto)) {
            String id = morto.getPersistentDataContainer().get(Chefes.CHAVE_CHEFE, PersistentDataType.STRING);
            Chefe c = id == null ? null : Chefe.porId(id);
            if (c == null || !PERMITIDOS.contains(c.tipo())) return null;
            return new Soldado(c.tipo(), 2, c.id());
        }
        if (!(morto instanceof Enemy) || !PERMITIDOS.contains(morto.getType())) return null;
        return new Soldado(morto.getType(), br.rpgatributos.perigo.Perigo.ehElite(morto) ? 1 : 0, "");
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPegarFogo(EntityCombustEvent e) {
        if (e instanceof EntityCombustByEntityEvent || e instanceof EntityCombustByBlockEvent) return;
        if (eh(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTransformar(EntityTransformEvent e) {
        if (eh(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        recolher(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void aoMorrerJogador(PlayerDeathEvent e) {
        recolher(e.getEntity().getUniqueId());
    }

    // =====================================================================
    //  /sombras
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (!soberano(p)) {
            p.sendMessage(Component.text("☾ Só o Soberano das Sombras comanda um exército de sombras.", NamedTextColor.GRAY));
            return true;
        }
        abrirMenu(p);
        return true;
    }

    public void abrirMenu(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☾ Exército das Sombras"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        List<Soldado> ex = exercito(p);
        Map<UUID, Integer> meus = emCampo(p.getUniqueId());
        Set<Integer> lutando = new java.util.HashSet<>(meus.values());
        int nivel = nivel(p);
        for (int i = 0; i < ex.size() && i < 45; i++) {
            Soldado s = ex.get(i);
            Material ovo = Material.matchMaterial(s.tipo.name() + "_SPAWN_EGG");
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(s.grau >= 2 ? "Marechal (sombra de um chefe)" : s.grau == 1 ? "Elite" : "Soldado", NamedTextColor.GRAY));
            lore.add(lutando.contains(i) ? Component.text("● Em campo", NamedTextColor.GREEN) : Component.text("Guardado na sua sombra", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Shift + clique: libertar para sempre", NamedTextColor.DARK_GRAY));
            ItemStack item = item(ovo == null ? Material.WITHER_SKELETON_SKULL : ovo,
                    Component.text("☾ " + nome(s), s.grau >= 2 ? TextColor.color(0xB388FF) : COR, TextDecoration.BOLD), lore);
            if (lutando.contains(i)) item.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(i, item);
        }
        inv.setItem(S_RECOLHER, item(Material.SOUL_LANTERN, Component.text("Recolher as sombras", COR, TextDecoration.BOLD), List.of(
                Component.text(meus.size() + " em campo agora.", NamedTextColor.GRAY),
                Component.text("» Clique para todas voltarem para a sua sombra", NamedTextColor.YELLOW))));
        inv.setItem(S_INFO, item(Material.WITHER_SKELETON_SKULL, Component.text("☾ Seu exército", COR, TextDecoration.BOLD), List.of(
                Component.text("Guardadas: " + ex.size() + "/" + capacidade(nivel), NamedTextColor.WHITE),
                Component.text("Em campo ao mesmo tempo: até " + maxEmCampo(nivel), NamedTextColor.WHITE),
                Component.text("(crescem com o nível da classe)", NamedTextColor.DARK_GRAY),
                Component.empty(),
                Component.text("Levante-se: quem você derrotou há pouco", NamedTextColor.GRAY),
                Component.text("vira sombra (30 s para levantar).", NamedTextColor.GRAY),
                Component.text("Exército das Sombras: chama as guardadas", NamedTextColor.GRAY),
                Component.text("ou deixa as que lutam furiosas.", NamedTextColor.GRAY),
                Component.text("Chefes viram Marechais!", NamedTextColor.LIGHT_PURPLE))));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 45; i < 54; i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        if (slot == S_RECOLHER) {
            recolher(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_DIG, 0.6f, 1.4f);
            desenhar(p, t);
            return;
        }
        List<Soldado> ex = exercito(p);
        if (slot < 45 && slot < ex.size() && e.isShiftClick()) {
            recolher(p.getUniqueId()); // os índices mudam: todas voltam antes
            Soldado s = ex.remove(slot);
            salvar(p, ex);
            p.playSound(p.getLocation(), Sound.ENTITY_ALLAY_DEATH, 0.6f, 0.6f);
            p.sendMessage(Component.text("☾ " + nome(s) + " foi libertado.", COR));
            desenhar(p, t);
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
