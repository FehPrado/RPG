package br.rpgatributos.classe;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.domador.Modo;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** O que cada habilidade de classe faz. {@code forca} vai de 0,6 (nível 1) a 1 (nível 20). */
public final class Habilidades {

    private record Armadilha(UUID dono, Location lugar, long ate, double forca) {}

    private final RPGAtributos plugin;
    // Efeitos que duram um tempo (jogador → até quando).
    private final Map<UUID, Long> furia = new HashMap<>();
    private final Map<UUID, Long> aguia = new HashMap<>();
    private final Map<UUID, Long> golpeLetal = new HashMap<>();
    private final Map<UUID, Long> afiado = new HashMap<>();
    private final Map<UUID, Long> escudoElemental = new HashMap<>();
    private final Map<UUID, Integer> lamina = new HashMap<>();
    private final Map<UUID, Long> laminaAte = new HashMap<>();
    /** Marca do Caçador: alvo → (caçador, até quando). */
    private final Map<UUID, UUID> marcaDe = new HashMap<>();
    private final Map<UUID, Long> marcaAte = new HashMap<>();
    private final List<Armadilha> armadilhas = new ArrayList<>();

    Habilidades(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
    private static long agora() { return System.currentTimeMillis(); }

    private static boolean ativo(Map<UUID, Long> m, UUID id) {
        Long ate = m.get(id);
        return ate != null && ate > agora();
    }

    boolean emFuria(Player p) { return ativo(furia, p.getUniqueId()); }
    boolean aguia(Player p) { return ativo(aguia, p.getUniqueId()); }
    boolean afiado(Player p) { return ativo(afiado, p.getUniqueId()); }
    boolean escudoElemental(Player p) { return ativo(escudoElemental, p.getUniqueId()); }

    boolean consumirGolpeLetal(Player p) {
        return ativo(golpeLetal, p.getUniqueId()) && golpeLetal.remove(p.getUniqueId()) != null;
    }

    boolean consumirLamina(Player p) {
        if (!ativo(laminaAte, p.getUniqueId())) return false;
        int n = lamina.getOrDefault(p.getUniqueId(), 0);
        if (n <= 0) return false;
        lamina.put(p.getUniqueId(), n - 1);
        return true;
    }

    boolean marcado(Player cacador, Entity alvo) {
        UUID id = alvo.getUniqueId();
        return cacador.getUniqueId().equals(marcaDe.get(id)) && ativo(marcaAte, id);
    }

    void esquecer(UUID id) {
        veu.remove(id);
        furia.remove(id);
        aguia.remove(id);
        golpeLetal.remove(id);
        afiado.remove(id);
        escudoElemental.remove(id);
        lamina.remove(id);
        laminaAte.remove(id);
    }

    // =====================================================================
    //  Quem é inimigo e quem é aliado
    // =====================================================================

    /** Pode ser atingido por uma habilidade desse jogador? (monstros, chefes e jogadores onde há PvP) */
    private boolean inimigo(Player p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isValid() || e instanceof ArmorStand) return false;
        if (Companheiros.eh(e)) return false;
        if (br.rpgatributos.sombra.Sombras.eh(e)) return plugin.sombras().inimigoDoJogador(p, e);
        if (e instanceof Player outro) return plugin.pvpPermitido(p, outro) && outro.getGameMode() != org.bukkit.GameMode.CREATIVE
                && outro.getGameMode() != org.bukkit.GameMode.SPECTATOR;
        return e instanceof Enemy || Chefes.ehChefe(e) || Chefes.ehLacaio(e);
    }

    private List<LivingEntity> inimigos(Player p, Location centro, double raio) {
        List<LivingEntity> l = new ArrayList<>();
        for (Entity e : centro.getWorld().getNearbyEntities(centro, raio, raio, raio)) {
            if (inimigo(p, e) && e.getLocation().distanceSquared(centro) <= raio * raio) l.add((LivingEntity) e);
        }
        return l;
    }

    /** Você e a sua party por perto. */
    private List<Player> aliados(Player p, double raio) {
        List<Player> l = new ArrayList<>();
        l.add(p);
        Party pt = plugin.parties().party(p);
        if (pt == null) return l;
        for (Player o : pt.online()) {
            if (o != p && o.getWorld().equals(p.getWorld()) && o.getLocation().distanceSquared(p.getLocation()) <= raio * raio) l.add(o);
        }
        return l;
    }

    private List<LivingEntity> companheiros(Player p, double raio) {
        List<LivingEntity> l = new ArrayList<>();
        for (var r : plugin.companheiros().de(p.getUniqueId())) {
            if (Bukkit.getEntity(r.id()) instanceof LivingEntity e && e.isValid() && e.getWorld().equals(p.getWorld())
                    && e.getLocation().distanceSquared(p.getLocation()) <= raio * raio) l.add(e);
        }
        return l;
    }

    private static void efeito(LivingEntity e, PotionEffectType t, int seg, int nivel) {
        e.addPotionEffect(new PotionEffect(t, seg * 20, nivel, false, true, true));
    }

    private static void empurrar(LivingEntity e, Location de, double forca, double cima) {
        Vector v = e.getLocation().toVector().subtract(de.toVector()).setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1));
        e.setVelocity(e.getVelocity().add(v.normalize().multiply(forca).setY(cima)));
    }

    private static boolean mortoVivo(Entity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof Phantom || e instanceof Wither || e instanceof Zoglin;
    }

    /** Ponto que o jogador está olhando (bloco ou alcance máximo). */
    private static Location mira(Player p, double alcance) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), alcance);
        if (r != null && r.getHitBlock() != null) return r.getHitPosition().toLocation(p.getWorld());
        return p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(alcance));
    }

    /** Criatura que o jogador está olhando. */
    private LivingEntity alvo(Player p, double alcance) {
        RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), alcance, 0.6,
                e -> inimigo(p, e));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity le)) return null;
        RayTraceResult parede = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(),
                p.getEyeLocation().distance(le.getEyeLocation()));
        return parede != null && parede.getHitBlock() != null ? null : le;
    }

    private static void falhar(Player p, String msg) {
        p.sendActionBar(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
    }

    // =====================================================================
    //  Usar
    // =====================================================================

    /** @return false se não deu para usar (sem alvo etc.): aí a recarga não começa. */
    boolean usar(Player p, Habilidade h, double forca) {
        World w = p.getWorld();
        Location l = p.getLocation();
        UUID id = p.getUniqueId();
        switch (h) {
            case INVESTIDA -> investida(p, 1.6, 6 * forca, false);
            case GRITO_DE_GUERRA -> {
                for (Player a : aliados(p, 8)) {
                    efeito(a, PotionEffectType.STRENGTH, 8, 0);
                    efeito(a, PotionEffectType.RESISTANCE, 8, 0);
                }
                for (LivingEntity e : inimigos(p, l, 8)) efeito(e, PotionEffectType.WEAKNESS, 8, 0);
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.2f);
                w.spawnParticle(Particle.SONIC_BOOM, l.clone().add(0, 1, 0), 1);
            }
            case FURIA -> {
                efeito(p, PotionEffectType.STRENGTH, 10, 1);
                efeito(p, PotionEffectType.HASTE, 10, 1);
                furia.put(id, agora() + 10_000);
                w.playSound(l, Sound.ENTITY_WARDEN_ROAR, 0.6f, 1.4f);
                w.spawnParticle(Particle.ANGRY_VILLAGER, l.clone().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
            }
            case REDEMOINHO -> {
                for (LivingEntity e : inimigos(p, l, 4)) {
                    e.damage(7 * forca, p);
                    empurrar(e, l, 0.9, 0.35);
                }
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5);
                    w.spawnParticle(Particle.SWEEP_ATTACK, l.clone().add(Math.cos(a) * 2.5, 1, Math.sin(a) * 2.5), 1);
                }
                w.playSound(l, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
            }
            case CARGA -> {
                if (p.getVehicle() instanceof LivingEntity montaria) {
                    montaria.setVelocity(p.getLocation().getDirection().setY(0.1).multiply(2.2));
                    investida(p, 0, 12 * forca, true);
                } else {
                    investida(p, 1.8, 8 * forca, false);
                }
                efeito(p, PotionEffectType.RESISTANCE, 3, 1);
            }
            case BALUARTE -> {
                efeito(p, PotionEffectType.RESISTANCE, 5, 2);
                efeito(p, PotionEffectType.SLOWNESS, 5, 1);
                for (LivingEntity e : inimigos(p, l, 10)) if (e instanceof Mob m) m.setTarget(p);
                w.playSound(l, Sound.ITEM_SHIELD_BLOCK, 1f, 0.6f);
                w.spawnParticle(Particle.WAX_ON, l.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6);
            }
            case LUZ_SAGRADA -> {
                for (Player a : aliados(p, 8)) {
                    AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                    a.setHealth(Math.min(max == null ? 20 : max.getValue(), a.getHealth() + 8 * forca));
                    w.spawnParticle(Particle.HEART, a.getLocation().add(0, 2, 0), 4, 0.3, 0.2, 0.3);
                }
                for (LivingEntity e : inimigos(p, l, 8)) {
                    if (!mortoVivo(e)) continue;
                    e.damage(10 * forca, p);
                    e.setFireTicks(80);
                }
                w.spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 60, 3, 1, 3, 0.02);
                w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.6f);
            }
            case ESCUDO_DIVINO -> {
                for (Player a : aliados(p, 8)) {
                    efeito(a, PotionEffectType.ABSORPTION, 10, 2);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, a.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.1);
                }
                w.playSound(l, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
            }
            case CHUVA_DE_FLECHAS -> {
                Location alvo = mira(p, 30);
                for (int i = 0; i < 18; i++) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        Location de = alvo.clone().add(rnd().nextDouble(-4, 4), 12, rnd().nextDouble(-4, 4));
                        Arrow a = w.spawnArrow(de, new Vector(0, -1, 0), 1.6f, 4f);
                        a.setShooter(p);
                        a.setDamage(3 * forca);
                        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    }, i * 2L);
                }
                w.playSound(alvo, Sound.ENTITY_ARROW_SHOOT, 1f, 0.6f);
            }
            case SALTO_EVASIVO -> {
                p.setVelocity(p.getLocation().getDirection().setY(0).normalize().multiply(-1.3).setY(0.55));
                efeito(p, PotionEffectType.SPEED, 4, 1);
                w.spawnParticle(Particle.CLOUD, l, 15, 0.3, 0.1, 0.3, 0.05);
            }
            case TIRO_PERFURANTE -> {
                Arrow a = p.launchProjectile(Arrow.class, p.getLocation().getDirection().multiply(4));
                a.setDamage(4 * forca);
                a.setPierceLevel(4);
                a.setCritical(true);
                a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                w.playSound(l, Sound.ITEM_CROSSBOW_SHOOT, 1f, 0.6f);
            }
            case OLHO_DE_AGUIA -> {
                aguia.put(id, agora() + 8000);
                for (LivingEntity e : inimigos(p, l, 30)) efeito(e, PotionEffectType.GLOWING, 8, 0);
                w.playSound(l, Sound.ENTITY_PARROT_IMITATE_PHANTOM, 1f, 1.2f);
            }
            case MARCA_DO_CACADOR -> {
                LivingEntity alvo = alvo(p, 30);
                if (alvo == null) { falhar(p, "Olhe para um inimigo."); return false; }
                marcaDe.put(alvo.getUniqueId(), id);
                marcaAte.put(alvo.getUniqueId(), agora() + 15_000);
                efeito(alvo, PotionEffectType.GLOWING, 15, 0);
                w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, alvo.getLocation().add(0, alvo.getHeight() + 0.3, 0), 15, 0.2, 0.2, 0.2, 0.01);
                p.playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
                for (LivingEntity c : companheiros(p, 48)) if (c instanceof Mob m) m.setTarget(alvo);
            }
            case ARMADILHA -> {
                armadilhas.add(new Armadilha(id, l.getBlock().getLocation().add(0.5, 0, 0.5), agora() + 30_000, forca));
                w.playSound(l, Sound.BLOCK_TRIPWIRE_ATTACH, 1f, 0.8f);
            }
            case MISSIL_ARCANO -> {
                LivingEntity alvo = alvo(p, 25);
                Location fim = alvo != null ? alvo.getLocation().add(0, alvo.getHeight() / 2, 0) : mira(p, 25);
                raio(p.getEyeLocation(), fim, Particle.WITCH);
                if (alvo != null) alvo.damage(6 * forca, p);
                w.playSound(l, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.6f);
            }
            case BARREIRA_ARCANA -> {
                efeito(p, PotionEffectType.ABSORPTION, 8, 1);
                w.spawnParticle(Particle.ENCHANT, l.clone().add(0, 1, 0), 60, 0.6, 0.8, 0.6, 0.5);
                w.playSound(l, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
            }
            case TEMPESTADE_ELEMENTAL -> {
                Location alvo = mira(p, 25);
                w.strikeLightningEffect(alvo);
                w.spawnParticle(Particle.FLAME, alvo, 60, 2, 1, 2, 0.05);
                w.spawnParticle(Particle.SNOWFLAKE, alvo, 60, 2, 1, 2, 0.05);
                for (LivingEntity e : inimigos(p, alvo, 4)) {
                    e.damage(9 * forca, p);
                    e.setFireTicks(80);
                    efeito(e, PotionEffectType.SLOWNESS, 3, 1);
                }
            }
            case ESCUDO_ELEMENTAL -> {
                efeito(p, PotionEffectType.FIRE_RESISTANCE, 10, 0);
                efeito(p, PotionEffectType.RESISTANCE, 10, 0);
                escudoElemental.put(id, agora() + 10_000);
                w.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.02);
                w.playSound(l, Sound.BLOCK_GLASS_BREAK, 1f, 1.6f);
            }
            case LAMINA_ARCANA -> {
                lamina.put(id, 3);
                laminaAte.put(id, agora() + 15_000);
                w.spawnParticle(Particle.WITCH, l.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
                w.playSound(l, Sound.ITEM_TRIDENT_RETURN, 1f, 1.4f);
            }
            case PASSO_ARCANO -> {
                Location destino = passo(p, 8);
                if (destino == null) { falhar(p, "Não há espaço à frente."); return false; }
                w.spawnParticle(Particle.PORTAL, l.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.3);
                p.teleport(destino);
                w.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
            }
            case BOMBA_DE_FUMACA -> {
                efeito(p, PotionEffectType.INVISIBILITY, 5, 0);
                efeito(p, PotionEffectType.SPEED, 5, 1);
                for (LivingEntity e : inimigos(p, l, 6)) {
                    efeito(e, PotionEffectType.BLINDNESS, 3, 0);
                    if (e instanceof Mob m && p.equals(m.getTarget())) m.setTarget(null);
                }
                w.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 80, 1.2, 1, 1.2, 0.02);
                w.playSound(l, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 0.6f);
            }
            case ROLAMENTO -> {
                Input in = p.getCurrentInput();
                double yaw = Math.toRadians(l.getYaw());
                Vector frente = new Vector(-Math.sin(yaw), 0, Math.cos(yaw)), direita = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
                Vector dir = new Vector();
                if (in.isForward()) dir.add(frente);
                if (in.isBackward()) dir.subtract(frente);
                if (in.isRight()) dir.add(direita);
                if (in.isLeft()) dir.subtract(direita);
                if (dir.lengthSquared() < 1e-4) dir = frente;
                p.setVelocity(dir.normalize().multiply(1.3).setY(0.25));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 15, 4, false, false, false));
                w.spawnParticle(Particle.CLOUD, l, 10, 0.3, 0.1, 0.3, 0.03);
            }
            case PASSOS_DAS_SOMBRAS -> {
                LivingEntity alvo = alvo(p, 12);
                if (alvo == null) { falhar(p, "Olhe para um inimigo (até 12 blocos)."); return false; }
                Vector costas = alvo.getLocation().getDirection().setY(0);
                if (costas.lengthSquared() < 1e-4) costas = new Vector(0, 0, 1);
                Location destino = alvo.getLocation().subtract(costas.normalize().multiply(1.3));
                destino.setDirection(alvo.getLocation().toVector().subtract(destino.toVector()));
                if (!destino.getBlock().isPassable() || !destino.clone().add(0, 1, 0).getBlock().isPassable()) {
                    falhar(p, "Não há espaço atrás do alvo.");
                    return false;
                }
                w.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.02);
                p.teleport(destino);
                w.playSound(destino, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
            }
            case GOLPE_LETAL -> {
                golpeLetal.put(id, agora() + 5000);
                w.playSound(l, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.6f);
                w.spawnParticle(Particle.DUST, l.clone().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(0x880000), 1.2f));
            }
            case REPARO_RAPIDO -> {
                ItemStack mao = p.getInventory().getItemInMainHand();
                if (!(mao.getItemMeta() instanceof Damageable d) || !d.hasDamage() || mao.getType().getMaxDurability() <= 0) {
                    falhar(p, "Segure um item gasto.");
                    return false;
                }
                int conserta = (int) Math.ceil(mao.getType().getMaxDurability() * 0.25 * forca);
                mao.editMeta(Damageable.class, m -> m.setDamage(Math.max(0, m.getDamage() - conserta)));
                w.playSound(l, Sound.BLOCK_ANVIL_USE, 0.8f, 1.4f);
                w.spawnParticle(Particle.CRIT, l.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            }
            case ARMADURA_REFORCADA -> {
                efeito(p, PotionEffectType.RESISTANCE, 8, 1);
                w.playSound(l, Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 0.8f);
            }
            case AFIAR -> {
                afiado.put(id, agora() + 60_000);
                w.playSound(l, Sound.BLOCK_GRINDSTONE_USE, 1f, 1.2f);
                w.spawnParticle(Particle.SCRAPE, l.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3);
            }
            case MARTELO_DO_TROVAO -> {
                Location alvo = mira(p, 20);
                w.strikeLightningEffect(alvo);
                for (LivingEntity e : inimigos(p, alvo, 3)) {
                    e.damage(10 * forca, p);
                    efeito(e, PotionEffectType.SLOWNESS, 2, 4);
                }
            }
            case CHAMADO -> {
                List<LivingEntity> comp = companheiros(p, 64);
                if (comp.isEmpty()) { falhar(p, "Nenhum companheiro seu perto (64 blocos)."); return false; }
                for (LivingEntity c : comp) {
                    plugin.companheiros().comandar(c, Modo.SEGUIR, null);
                    efeito(c, PotionEffectType.SPEED, 10, 2);
                    efeito(c, PotionEffectType.REGENERATION, 10, 1);
                    if (c instanceof Mob m) m.getPathfinder().moveTo(p, 1.8);
                }
                w.playSound(l, Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
            case FURIA_DA_MATILHA -> {
                List<LivingEntity> comp = companheiros(p, 32);
                if (comp.isEmpty()) { falhar(p, "Nenhum companheiro seu perto."); return false; }
                for (LivingEntity c : comp) {
                    efeito(c, PotionEffectType.STRENGTH, 15, 1);
                    efeito(c, PotionEffectType.SPEED, 15, 1);
                    w.spawnParticle(Particle.ANGRY_VILLAGER, c.getLocation().add(0, c.getHeight(), 0), 4, 0.3, 0.2, 0.3);
                }
                w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1f, 0.8f);
            }
            case RUGIDO -> {
                for (LivingEntity e : inimigos(p, l, 8)) {
                    efeito(e, PotionEffectType.WEAKNESS, 6, 1);
                    efeito(e, PotionEffectType.SLOWNESS, 6, 1);
                    empurrar(e, l, 1.2, 0.4);
                }
                w.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 1f, 0.7f);
                w.spawnParticle(Particle.SONIC_BOOM, l.clone().add(0, 1, 0), 1);
            }
            case VINCULO_VITAL -> {
                List<LivingEntity> comp = companheiros(p, 32);
                if (comp.isEmpty()) { falhar(p, "Nenhum companheiro seu perto."); return false; }
                for (LivingEntity c : comp) {
                    AttributeInstance max = c.getAttribute(Attribute.MAX_HEALTH);
                    double vmax = max == null ? 20 : max.getValue();
                    c.setHealth(Math.min(vmax, c.getHealth() + vmax * 0.5 * forca));
                    efeito(c, PotionEffectType.RESISTANCE, 10, 1);
                    w.spawnParticle(Particle.HEART, c.getLocation().add(0, c.getHeight(), 0), 5, 0.3, 0.2, 0.3);
                }
                efeito(p, PotionEffectType.REGENERATION, 5, 0);
                w.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            }

            // ---------------- lendárias ----------------
            case MARTELO_CELESTIAL -> {
                Location alvo = mira(p, 25);
                w.strikeLightningEffect(alvo);
                w.spawnParticle(Particle.FLASH, alvo, 2);
                w.spawnParticle(Particle.END_ROD, alvo, 80, 2, 0.5, 2, 0.1);
                for (LivingEntity e : inimigos(p, alvo, 5)) {
                    e.damage(14 * forca, p);
                    e.setVelocity(e.getVelocity().setY(0.9));
                }
                w.playSound(alvo, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, 1.5f, 0.7f);
            }
            case BENCAO_DO_FERREIRO -> {
                for (Player a : aliados(p, 12)) {
                    efeito(a, PotionEffectType.STRENGTH, 20, 0);
                    efeito(a, PotionEffectType.RESISTANCE, 20, 0);
                    efeito(a, PotionEffectType.HASTE, 20, 1);
                    w.spawnParticle(Particle.LAVA, a.getLocation().add(0, 1, 0), 10, 0.4, 0.5, 0.4);
                }
                w.playSound(l, Sound.BLOCK_ANVIL_LAND, 1f, 1.4f);
            }
            case METEORO_ARCANO -> {
                Location alvo = mira(p, 30);
                for (int i = 0; i < 20; i++) {
                    Location trilha = alvo.clone().add(i * 0.4, 12 - i * 0.6, i * 0.4);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> w.spawnParticle(Particle.FLAME, trilha, 8, 0.3, 0.3, 0.3, 0.02), i);
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 1);
                    w.spawnParticle(Particle.LAVA, alvo, 40, 2, 0.5, 2);
                    w.playSound(alvo, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                    for (LivingEntity e : inimigos(p, alvo, 5)) {
                        e.damage(18 * forca, p);
                        e.setFireTicks(100);
                        empurrar(e, alvo, 1.2, 0.6);
                    }
                }, 20L);
            }
            case TEMPO_SUSPENSO -> {
                for (LivingEntity e : inimigos(p, l, 10)) {
                    efeito(e, PotionEffectType.SLOWNESS, 5, 4);
                    efeito(e, PotionEffectType.WEAKNESS, 5, 1);
                    efeito(e, PotionEffectType.LEVITATION, 1, 0);
                }
                w.spawnParticle(Particle.REVERSE_PORTAL, l.clone().add(0, 1, 0), 150, 5, 1.5, 5, 0.02);
                w.playSound(l, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.6f);
            }
            case CHAMADO_ANCESTRAL -> {
                for (int i = 0; i < 3; i++) {
                    Location onde = l.clone().add(Math.cos(i * 2.1) * 2, 0, Math.sin(i * 2.1) * 2);
                    org.bukkit.entity.Wolf lobo = w.spawn(onde, org.bukkit.entity.Wolf.class, o -> {
                        o.setTamed(true);
                        o.setOwner(p);
                        o.setPersistent(false);
                        o.setGlowing(true);
                        o.customName(Component.text("Lobo Ancestral", NamedTextColor.AQUA));
                        AttributeInstance vida = o.getAttribute(Attribute.MAX_HEALTH);
                        if (vida != null) { vida.setBaseValue(40); o.setHealth(40); }
                        AttributeInstance dano = o.getAttribute(Attribute.ATTACK_DAMAGE);
                        if (dano != null) dano.setBaseValue(8 * forca);
                    });
                    w.spawnParticle(Particle.SOUL, onde.clone().add(0, 0.5, 0), 20, 0.3, 0.4, 0.3, 0.02);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (lobo.isValid()) {
                            lobo.getWorld().spawnParticle(Particle.SOUL, lobo.getLocation().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.02);
                            lobo.remove();
                        }
                    }, 600L);
                }
                w.playSound(l, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.6f);
            }
            case FORMA_BESTIAL -> {
                efeito(p, PotionEffectType.STRENGTH, 15, 1);
                efeito(p, PotionEffectType.SPEED, 15, 1);
                efeito(p, PotionEffectType.JUMP_BOOST, 15, 1);
                efeito(p, PotionEffectType.REGENERATION, 15, 0);
                w.playSound(l, Sound.ENTITY_POLAR_BEAR_WARNING, 1f, 0.7f);
                w.spawnParticle(Particle.ANGRY_VILLAGER, l.clone().add(0, 2, 0), 10, 0.5, 0.3, 0.5);
            }
            case TERREMOTO -> {
                for (LivingEntity e : inimigos(p, l, 6)) {
                    e.damage(12 * forca, p);
                    e.setVelocity(e.getVelocity().setY(1.0));
                    efeito(e, PotionEffectType.SLOWNESS, 4, 2);
                }
                w.spawnParticle(Particle.BLOCK, l, 200, 3, 0.2, 3, l.clone().subtract(0, 1, 0).getBlock().getBlockData());
                w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
            }
            case ESTANDARTE_DE_GUERRA -> {
                for (Player a : aliados(p, 12)) {
                    efeito(a, PotionEffectType.STRENGTH, 20, 0);
                    efeito(a, PotionEffectType.RESISTANCE, 20, 0);
                    efeito(a, PotionEffectType.REGENERATION, 20, 0);
                }
                w.spawnParticle(Particle.DUST, l.clone().add(0, 2.5, 0), 60, 0.3, 1.2, 0.3, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(0xB71C1C), 2f));
                w.playSound(l, Sound.EVENT_RAID_HORN, 1f, 1.2f);
            }
            case DANCA_DAS_LAMINAS -> {
                List<LivingEntity> alvos = inimigos(p, l, 8);
                if (alvos.isEmpty()) { falhar(p, "Nenhum inimigo perto."); return false; }
                int n = Math.min(5, alvos.size());
                for (int i = 0; i < n; i++) {
                    LivingEntity alvo = alvos.get(i);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!p.isOnline() || !alvo.isValid()) return;
                        Location atras = alvo.getLocation().subtract(alvo.getLocation().getDirection().setY(0).multiply(1.2));
                        if (atras.getBlock().isPassable() && atras.clone().add(0, 1, 0).getBlock().isPassable()) {
                            atras.setDirection(alvo.getLocation().toVector().subtract(atras.toVector()));
                            p.teleport(atras);
                        }
                        alvo.damage(8 * forca, p);
                        w.spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, 1, 0), 2);
                        w.playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
                    }, i * 4L);
                }
            }
            case VEU_SOMBRIO -> {
                efeito(p, PotionEffectType.INVISIBILITY, 8, 0);
                veu.put(id, agora() + 8000);
                for (LivingEntity e : inimigos(p, l, 16)) if (e instanceof Mob m && p.equals(m.getTarget())) m.setTarget(null);
                w.spawnParticle(Particle.SQUID_INK, l.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.02);
                w.playSound(l, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 0.8f);
            }
            case FLECHA_ESTELAR -> {
                Location olho = p.getEyeLocation();
                Vector dir = olho.getDirection();
                RayTraceResult parede = w.rayTraceBlocks(olho, dir, 40);
                double alcance = parede == null ? 40 : olho.toVector().distance(parede.getHitPosition());
                Set<UUID> atingidos = new HashSet<>();
                for (double d = 0; d < alcance; d += 0.5) {
                    Location ponto = olho.clone().add(dir.clone().multiply(d));
                    w.spawnParticle(Particle.END_ROD, ponto, 1, 0, 0, 0, 0);
                    for (LivingEntity e : inimigos(p, ponto, 1.2)) {
                        if (atingidos.add(e.getUniqueId())) e.damage(16 * forca, p);
                    }
                }
                w.playSound(l, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.8f);
            }
            case CHUVA_DE_ESTRELAS -> {
                Location alvo = mira(p, 35);
                for (int i = 0; i < 30; i++) {
                    long quando = i * 2L;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        Location de = alvo.clone().add(rnd().nextDouble(-5, 5), 14, rnd().nextDouble(-5, 5));
                        Arrow a = w.spawnArrow(de, new Vector(0, -1, 0), 2f, 3f);
                        a.setShooter(p);
                        a.setDamage(5 * forca);
                        a.setGlowing(true);
                        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    }, quando);
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> w.strikeLightningEffect(alvo), 30L);
            }
            case JULGAMENTO_DIVINO -> {
                Location alvo = mira(p, 30);
                for (double y = 0; y < 12; y += 0.5) w.spawnParticle(Particle.END_ROD, alvo.clone().add(0, y, 0), 3, 0.3, 0, 0.3, 0);
                for (LivingEntity e : inimigos(p, alvo, 3.5)) {
                    e.damage((mortoVivo(e) ? 20 : 12) * forca, p);
                    if (mortoVivo(e)) e.setFireTicks(100);
                }
                for (Player a : aliados(p, 30)) {
                    if (!a.getWorld().equals(alvo.getWorld()) || a.getLocation().distanceSquared(alvo) > 25) continue;
                    AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                    a.setHealth(Math.min(max == null ? 20 : max.getValue(), a.getHealth() + 6 * forca));
                }
                w.playSound(alvo, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.8f);
            }
            case AURA_SAGRADA -> {
                for (Player a : aliados(p, 12)) {
                    for (PotionEffectType ruim : List.of(PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS,
                            PotionEffectType.SLOWNESS, PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS, PotionEffectType.NAUSEA)) {
                        a.removePotionEffect(ruim);
                    }
                    efeito(a, PotionEffectType.REGENERATION, 15, 1);
                    efeito(a, PotionEffectType.RESISTANCE, 15, 0);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, a.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
                }
                w.playSound(l, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.6f);
            }
            case LEVANTE_SE -> {
                return plugin.sombras().levantar(p);
            }
            case EXERCITO_DAS_SOMBRAS -> {
                return plugin.sombras().exercito(p, alvo(p, 32));
            }
        }
        return true;
    }

    /** Véu Sombrio: o próximo golpe dá dano x3. */
    private final Map<UUID, Long> veu = new HashMap<>();

    boolean consumirVeu(Player p) {
        return ativo(veu, p.getUniqueId()) && veu.remove(p.getUniqueId()) != null;
    }

    /** Avança e acerta quem estiver no caminho durante os próximos ticks. */
    private void investida(Player p, double impulso, double dano, boolean montado) {
        if (impulso > 0) p.setVelocity(p.getLocation().getDirection().setY(0).normalize().multiply(impulso).setY(0.2));
        Set<UUID> acertados = new HashSet<>();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1f, 1.2f);
        for (int t = 1; t <= 10; t++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                Location c = (montado && p.getVehicle() != null ? p.getVehicle() : p).getLocation();
                p.getWorld().spawnParticle(Particle.CLOUD, c, 3, 0.2, 0.1, 0.2, 0.01);
                for (LivingEntity e : inimigos(p, c, 2.2)) {
                    if (!acertados.add(e.getUniqueId())) continue;
                    e.damage(dano, p);
                    empurrar(e, c, 1.0, 0.45);
                }
            }, t);
        }
    }

    /** Lugar livre até {@code max} blocos à frente (para o Passo Arcano). */
    private static Location passo(Player p, double max) {
        Location olho = p.getEyeLocation();
        Vector dir = olho.getDirection().normalize();
        RayTraceResult r = p.getWorld().rayTraceBlocks(olho, dir, max);
        double dist = r == null ? max : Math.max(0, olho.toVector().distance(r.getHitPosition()) - 0.8);
        for (double d = dist; d >= 1.5; d -= 0.5) {
            Location l = p.getLocation().add(dir.clone().multiply(d));
            if (l.getBlock().isPassable() && l.clone().add(0, 1, 0).getBlock().isPassable()) {
                l.setDirection(dir);
                return l;
            }
        }
        return null;
    }

    private static void raio(Location de, Location ate, Particle particula) {
        Vector passo = ate.toVector().subtract(de.toVector());
        double dist = passo.length();
        if (dist < 0.1) return;
        passo.normalize().multiply(0.4);
        Location l = de.clone();
        for (double d = 0; d < dist; d += 0.4) {
            de.getWorld().spawnParticle(particula, l, 2, 0.02, 0.02, 0.02, 0);
            l.add(passo);
        }
    }

    /** Armadilhas armadas (a cada 5 ticks). */
    public void tick() {
        long t = agora();
        for (Iterator<Armadilha> it = armadilhas.iterator(); it.hasNext(); ) {
            Armadilha a = it.next();
            Player dono = Bukkit.getPlayer(a.dono());
            if (dono == null || t > a.ate() || !a.lugar().isChunkLoaded()) {
                it.remove();
                continue;
            }
            a.lugar().getWorld().spawnParticle(Particle.CRIT, a.lugar().clone().add(0, 0.1, 0), 2, 0.3, 0, 0.3, 0);
            for (LivingEntity e : inimigos(dono, a.lugar(), 1.5)) {
                e.damage(8 * a.forca(), dono);
                efeito(e, PotionEffectType.SLOWNESS, 4, 4);
                a.lugar().getWorld().playSound(a.lugar(), Sound.BLOCK_CHAIN_PLACE, 1f, 0.6f);
                a.lugar().getWorld().spawnParticle(Particle.BLOCK, a.lugar(), 20, 0.3, 0.2, 0.3, org.bukkit.Material.COBWEB.createBlockData());
                it.remove();
                break;
            }
        }
        marcaAte.entrySet().removeIf(en -> en.getValue() < t);
        marcaDe.keySet().retainAll(marcaAte.keySet());
    }
}
