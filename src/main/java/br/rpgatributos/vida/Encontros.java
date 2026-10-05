package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.detalhes.ItensDetalhes;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.masmorra.Masmorras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encontros na estrada: de vez em quando, quem explora sozinho a céu aberto (fora de
 * territórios) encontra algo a poucos blocos: uma carroça tombada com carga, um acampamento de
 * bandidos guardando o saque, um viajante ferido que pede ajuda ou uma estrela cadente. Tudo
 * some sozinho depois de alguns minutos.
 */
public final class Encontros implements Listener {

    public static final TextColor COR = TextColor.color(0xBCAAA4);
    /** Marca das criaturas e objetos de encontro (o Controle limpa as que sobrarem). */
    public static final NamespacedKey K_ENCONTRO = new NamespacedKey("rpgatributos", "encontro");
    private static final String[] DIRECOES = {"sul", "sudoeste", "oeste", "noroeste", "norte", "nordeste", "leste", "sudeste"};
    private static final long DURACAO = 10 * 60_000L;

    private record Ativo(List<UUID> entidades, List<UUID> bandidos, UUID bau, long ate) { }

    private record Estrela(Location onde, long ate) { }

    private final RPGAtributos plugin;
    private final Map<UUID, Long> esperaDe = new HashMap<>();
    private final List<Ativo> ativos = new ArrayList<>();
    private final List<Estrela> estrelas = new ArrayList<>();
    private int passos;

    public Encontros(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static String direcao(Location de, Location para) {
        double ang = Math.toDegrees(Math.atan2(-(para.getX() - de.getX()), para.getZ() - de.getZ()));
        return DIRECOES[(int) Math.round(((ang % 360) + 360) % 360 / 45.0) % 8];
    }

    private static void marcar(Entity e) {
        e.getPersistentDataContainer().set(K_ENCONTRO, PersistentDataType.BYTE, (byte) 1);
        e.setPersistent(false);
    }

    // =====================================================================
    //  Sortear
    // =====================================================================

    /** A cada 20 s (sorteia encontros a cada 2 minutos). */
    public void tick() {
        long agora = System.currentTimeMillis();
        limpar(agora);
        if (++passos % 6 != 0 || !plugin.settings().vidaEncontros) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (agora < esperaDe.getOrDefault(p.getUniqueId(), 0L) || !pode(p)) continue;
            if (rnd().nextDouble() >= 0.25) continue;
            esperaDe.put(p.getUniqueId(), agora + plugin.settings().vidaEncontroMinutos * 60_000L);
            sortear(p);
        }
    }

    private boolean pode(Player p) {
        World w = p.getWorld();
        Block b = p.getLocation().getBlock();
        return p.getGameMode() == GameMode.SURVIVAL && w.getEnvironment() == World.Environment.NORMAL
                && !w.getName().equals(Masmorras.MUNDO) && b.getLightFromSky() >= 14
                && plugin.territorios().em(p.getLocation()) == null && !p.isInsideVehicle();
    }

    /** Começa um encontro (o admin escolhe o tipo, ou null para sortear). @return o nome do encontro, ou null. */
    public String sortear(Player p, String tipo) {
        String t = tipo != null ? tipo : switch (rnd().nextInt(4)) {
            case 0 -> "carroca";
            case 1 -> "bandidos";
            case 2 -> "viajante";
            default -> "estrela";
        };
        boolean dia = !br.rpgatributos.mundo.Ceu.noite(p.getWorld());
        if (t.equals("viajante") && !dia && tipo == null) t = "carroca";
        Location onde = lugar(p.getLocation(), t.equals("estrela") ? 25 : 12, t.equals("estrela") ? 40 : 20);
        if (onde == null) return null;
        return switch (t) {
            case "carroca" -> carroca(p, onde);
            case "bandidos" -> bandidos(p, onde);
            case "viajante" -> viajante(p, onde);
            default -> estrela(p, onde);
        };
    }

    private void sortear(Player p) {
        sortear(p, null);
    }

    private static Location lugar(Location centro, double min, double max) {
        World w = centro.getWorld();
        for (int i = 0; i < 12; i++) {
            double ang = rnd().nextDouble() * Math.PI * 2, dist = min + rnd().nextDouble() * (max - min);
            int x = (int) (centro.getX() + Math.cos(ang) * dist), z = (int) (centro.getZ() + Math.sin(ang) * dist);
            Block chao = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (chao.isLiquid() || !chao.getType().isSolid() || Math.abs(chao.getY() - centro.getY()) > 12) continue;
            return chao.getLocation().add(0.5, 1, 0.5);
        }
        return null;
    }

    private void avisar(Player p, Location onde, String texto) {
        p.sendMessage(Component.text("🧭 " + texto + " a " + Math.round(p.getLocation().distance(onde)) + " blocos ao "
                + direcao(p.getLocation(), onde) + ".", COR));
        p.playSound(p.getLocation(), Sound.ITEM_SPYGLASS_USE, 1f, 1f);
    }

    // =====================================================================
    //  Tipos
    // =====================================================================

    private StorageMinecart bau(Location onde, String nome, List<ItemStack> carga) {
        return onde.getWorld().spawn(onde, StorageMinecart.class, c -> {
            c.customName(Component.text(nome, COR));
            c.setCustomNameVisible(true);
            marcar(c);
            for (ItemStack i : carga) c.getInventory().addItem(i);
        });
    }

    private BlockDisplay enfeite(Location onde, Material m, float x, float z, float giro) {
        return onde.getWorld().spawn(onde.clone().add(x, 0, z), BlockDisplay.class, d -> {
            d.setBlock(m.createBlockData());
            d.setTransformation(new Transformation(new Vector3f(-0.5f, 0, -0.5f), new AxisAngle4f(giro, 0, 1, 0),
                    new Vector3f(1, 1, 1), new AxisAngle4f()));
            marcar(d);
        });
    }

    private List<ItemStack> carga(boolean melhor) {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.EMERALD, (melhor ? 6 : 3) + rnd().nextInt(6)));
        l.add(new ItemStack(Material.BREAD, 2 + rnd().nextInt(4)));
        l.add(new ItemStack(rnd().nextBoolean() ? Material.IRON_INGOT : Material.GOLD_INGOT, 2 + rnd().nextInt(4)));
        if (rnd().nextDouble() < (melhor ? 0.6 : 0.3)) l.add(Album.carta(Carta.values()[rnd().nextInt(Carta.values().length)], rnd().nextDouble() < 0.05));
        if (rnd().nextDouble() < 0.3) l.add(ItensDetalhes.ferradura(1));
        if (rnd().nextDouble() < 0.25) l.add(Fruta.sortear(rnd()).muda(1));
        if (rnd().nextDouble() < 0.4) l.add(Bebida.values()[rnd().nextInt(Bebida.values().length)].criar(melhor ? 2 : 1));
        if (melhor && rnd().nextDouble() < 0.4) l.add(Raro.FRAGMENTO_DE_FORJA.criar(1));
        if (rnd().nextDouble() < (melhor ? 0.3 : 0.15)) l.add(Gema.values()[rnd().nextInt(Gema.values().length)].criar(1, 1));
        return l;
    }

    private String carroca(Player p, Location onde) {
        List<UUID> ids = new ArrayList<>();
        StorageMinecart c = bau(onde, "Carroça tombada", carga(false));
        ids.add(c.getUniqueId());
        ids.add(enfeite(onde, Material.HAY_BLOCK, 1.3f, 0.4f, 0.4f).getUniqueId());
        ids.add(enfeite(onde, Material.OAK_FENCE, -1.2f, 0.8f, 1.2f).getUniqueId());
        ids.add(enfeite(onde, Material.BARREL, 0.3f, -1.4f, 0.8f).getUniqueId());
        ativos.add(new Ativo(ids, List.of(), c.getUniqueId(), System.currentTimeMillis() + DURACAO));
        avisar(p, onde, "Uma carroça tombada na estrada");
        return "carroça";
    }

    private String bandidos(Player p, Location onde) {
        List<UUID> ids = new ArrayList<>(), bandidos = new ArrayList<>();
        StorageMinecart c = bau(onde, "Saque dos bandidos", carga(true));
        ids.add(c.getUniqueId());
        ids.add(enfeite(onde, Material.CAMPFIRE, 2f, 1f, 0).getUniqueId());
        int n = 3 + rnd().nextInt(2);
        for (int i = 0; i < n; i++) {
            Location l = onde.clone().add(rnd().nextDouble() * 6 - 3, 0, rnd().nextDouble() * 6 - 3);
            l.setY(l.getWorld().getHighestBlockYAt(l, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1);
            Pillager b = l.getWorld().spawn(l, Pillager.class, m -> {
                m.customName(Component.text("Bandido", NamedTextColor.RED));
                m.setPatrolLeader(false);
                m.setCanJoinRaid(false);
                marcar(m);
            });
            ids.add(b.getUniqueId());
            bandidos.add(b.getUniqueId());
        }
        ativos.add(new Ativo(ids, bandidos, c.getUniqueId(), System.currentTimeMillis() + DURACAO));
        avisar(p, onde, "Um acampamento de bandidos com um saque");
        return "bandidos";
    }

    private String viajante(Player p, Location onde) {
        Villager v = onde.getWorld().spawn(onde, Villager.class, m -> {
            m.customName(Component.text("Viajante ferido", NamedTextColor.YELLOW));
            m.setCustomNameVisible(true);
            m.setAI(false);
            m.setProfession(Villager.Profession.NONE);
            m.setHealth(Math.min(6, m.getHealth()));
            m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 600, 3, false, false));
            marcar(m);
        });
        ativos.add(new Ativo(new ArrayList<>(List.of(v.getUniqueId())), List.of(), null, System.currentTimeMillis() + DURACAO));
        avisar(p, onde, "Um viajante ferido pede ajuda (leve comida ou cura)");
        return "viajante";
    }

    private String estrela(Player p, Location onde) {
        World w = onde.getWorld();
        Location ceu = onde.clone().add(-20, 60, -20);
        for (int i = 0; i <= 20; i++) {
            final int passo = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location l = ceu.clone().add(onde.clone().subtract(ceu).toVector().multiply(passo / 20.0));
                w.spawnParticle(Particle.FIREWORK, l, 6, 0.2, 0.2, 0.2, 0.02);
                w.spawnParticle(Particle.END_ROD, l, 3, 0.1, 0.1, 0.1, 0.01);
                if (passo == 20) {
                    w.spawnParticle(Particle.EXPLOSION, onde, 3, 0.5, 0.3, 0.5, 0);
                    w.playSound(onde, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.6f);
                    for (ItemStack it : List.of(Reagente.CRISTAL_DE_MANA.criar(1 + rnd().nextInt(2)),
                            rnd().nextDouble() < 0.5 ? Gema.values()[rnd().nextInt(Gema.values().length)].criar(1, 1) : Reagente.PO_ARCANO.criar(3),
                            rnd().nextDouble() < 0.1 ? Raro.ESSENCIA_PRIMORDIAL.criar(1) : new ItemStack(Material.GLOWSTONE_DUST, 4))) {
                        Item item = w.dropItem(onde, it);
                        item.setGlowing(true);
                    }
                }
            }, i);
        }
        w.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.5f);
        estrelas.add(new Estrela(onde, System.currentTimeMillis() + 2 * 60_000L));
        p.sendMessage(Component.text("✦ Uma estrela cadente caiu a " + Math.round(p.getLocation().distance(onde)) + " blocos ao "
                + direcao(p.getLocation(), onde) + "!", NamedTextColor.GOLD));
        return "estrela cadente";
    }

    // =====================================================================
    //  Regras de cada encontro
    // =====================================================================

    private Ativo doBau(Entity e) {
        for (Ativo a : ativos) if (e.getUniqueId().equals(a.bau())) return a;
        return null;
    }

    private static boolean vivos(Ativo a) {
        for (UUID id : a.bandidos()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid() && !e.isDead()) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAbrirBau(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof StorageMinecart c)) return;
        Ativo a = doBau(c);
        if (a == null || !vivos(a)) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(Component.text("✖ Os bandidos ainda guardam o saque!", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrarBau(VehicleDestroyEvent e) {
        Ativo a = doBau(e.getVehicle());
        if (a != null && vivos(a)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAjudar(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Villager v) || !v.getPersistentDataContainer().has(K_ENCONTRO)) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        Elixir el = Elixir.de(mao);
        boolean cura = mao.getType().isEdible() || el == Elixir.CURA || mao.getType() == Material.GOLDEN_APPLE;
        if (!cura) {
            p.sendActionBar(Component.text("O viajante precisa de comida ou de algo que cure.", NamedTextColor.YELLOW));
            return;
        }
        if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        v.getPersistentDataContainer().remove(K_ENCONTRO);
        v.setHealth(v.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        v.removePotionEffect(PotionEffectType.SLOWNESS);
        v.getWorld().spawnParticle(Particle.HEART, v.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4, 0);
        v.getWorld().playSound(v.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        List<ItemStack> premio = new ArrayList<>();
        premio.add(new ItemStack(Material.EMERALD, 4 + rnd().nextInt(7)));
        premio.add(switch (rnd().nextInt(5)) {
            case 0 -> Album.carta(Carta.values()[rnd().nextInt(Carta.values().length)], false);
            case 1 -> plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, v.getLocation());
            case 2 -> Variedade.values()[rnd().nextInt(Variedade.values().length)].semente(2);
            case 3 -> ItensDetalhes.ferradura(1);
            default -> Gema.values()[rnd().nextInt(Gema.values().length)].criar(1, 1);
        });
        for (ItemStack i : premio) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.sendMessage(Component.text("🧭 Viajante: \"Muito obrigado, aventureiro! Leve isto, é tudo o que tenho.\"", COR));
        plugin.titulos().registrar(p, "viajantes", 1);
        plugin.diario().marco(p, "viajante", "Ajudou um viajante ferido na estrada");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!v.isValid()) return;
            v.getWorld().spawnParticle(Particle.CLOUD, v.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
            v.remove();
        }, 100L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerirViajante(EntityDamageEvent e) {
        if (e.getEntity() instanceof Villager v && v.getPersistentDataContainer().has(K_ENCONTRO)) e.setCancelled(true);
    }

    private void limpar(long agora) {
        ativos.removeIf(a -> {
            boolean saqueado = a.bau() != null && (Bukkit.getEntity(a.bau()) instanceof StorageMinecart c ? c.getInventory().isEmpty() : true);
            if (agora < a.ate() && !saqueado) return false;
            for (UUID id : a.entidades()) {
                Entity e = Bukkit.getEntity(id);
                if (e != null && e.isValid() && !(e instanceof LivingEntity le && !le.getPersistentDataContainer().has(K_ENCONTRO))) e.remove();
            }
            return true;
        });
        estrelas.removeIf(s -> agora > s.ate());
        for (Estrela s : estrelas) {
            if (s.onde().isChunkLoaded()) s.onde().getWorld().spawnParticle(Particle.END_ROD, s.onde().clone().add(0, 3, 0), 15, 0.1, 3, 0.1, 0.01);
        }
    }

    public void parar() {
        for (Ativo a : ativos) for (UUID id : a.entidades()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        ativos.clear();
    }
}
