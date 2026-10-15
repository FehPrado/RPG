package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Artefatos de utilidade (2.49): Garras de Escalada, Corda de Rapel, Pêndulo do Rabdomante, Cantil e
 * Bolsa de Sementes. Feitos na bancada; as Garras, a Corda e a Bolsa gastam Vigor.
 */
public final class Artefatos implements Listener {

    public static final TextColor COR = TextColor.color(0xC9A26B);
    private static final NamespacedKey K_ARTEFATO = new NamespacedKey("rpgatributos", "artefato");
    private static final NamespacedKey K_USOS = new NamespacedKey("rpgatributos", "artefato_usos");
    private static final NamespacedKey K_SEMENTE = new NamespacedKey("rpgatributos", "bolsa_semente");

    public enum Tipo {
        GARRAS("garras_escalada", Material.IRON_NUGGET, "Garras de Escalada",
                "De frente para a parede, segure pular para subir.", "Agachado na parede, fica pendurado. Gasta Vigor; vidro e gelo escorregam."),
        CORDA("corda_rapel", Material.STRING, "Corda de Rapel",
                "Na beira de um barranco, clique para descer pela corda.", "Agachado segura; pular sobe um pouco. 30 descidas."),
        PENDULO("pendulo_rabdomante", Material.FLINT, "Pêndulo do Rabdomante",
                "Na mão, balança mais forte perto de estruturas não", "descobertas e de minerais raros (quente ou frio)."),
        CANTIL("cantil", Material.RABBIT_HIDE, "Cantil",
                "Clique olhando para a água para encher (3 goles).", "Um gole tira o calor por 5 minutos e apaga o fogo."),
        BOLSA("bolsa_sementes", Material.LEATHER, "Bolsa de Sementes",
                "Clique na terra arada para plantar em 3×3.", "Agachado troca a semente. Usa as do inventário e gasta Vigor.");

        public final String id;
        final Material base;
        final String nome, linha1, linha2;

        Tipo(String id, Material base, String nome, String linha1, String linha2) {
            this.id = id;
            this.base = base;
            this.nome = nome;
            this.linha1 = linha1;
            this.linha2 = linha2;
        }
    }

    private static final int USOS_CORDA = 30, GOLES = 3, CORDA_MAX = 40;
    private static final double VIGOR_SUBIR = 0.12, VIGOR_PENDURAR = 0.035, VIGOR_BOLSA = 2;
    private static final Map<Material, Material> SEMENTES = new LinkedHashMap<>();

    static {
        SEMENTES.put(Material.WHEAT_SEEDS, Material.WHEAT);
        SEMENTES.put(Material.CARROT, Material.CARROTS);
        SEMENTES.put(Material.POTATO, Material.POTATOES);
        SEMENTES.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        SEMENTES.put(Material.MELON_SEEDS, Material.MELON_STEM);
        SEMENTES.put(Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM);
        SEMENTES.put(Material.TORCHFLOWER_SEEDS, Material.TORCHFLOWER_CROP);
        SEMENTES.put(Material.PITCHER_POD, Material.PITCHER_CROP);
    }

    private final RPGAtributos plugin;
    /** Rapel: o ponto de onde a corda sai. */
    private final Map<UUID, Location> rapel = new HashMap<>();
    /** Quem está sem Vigor nas garras (para avisar uma vez só). */
    private final Set<UUID> semFolego = new HashSet<>();
    /** Rapel e garras: sem dano de queda até este instante. */
    private final Map<UUID, Long> semQueda = new HashMap<>();
    private final Map<UUID, Long> esperaCorda = new HashMap<>();

    public Artefatos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Itens e receitas
    // =====================================================================

    public static ItemStack criar(Tipo t) {
        ItemStack i = new ItemStack(t.base);
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(K_ARTEFATO, PersistentDataType.STRING, t.id);
            if (t == Tipo.CORDA) m.getPersistentDataContainer().set(K_USOS, PersistentDataType.INTEGER, USOS_CORDA);
            if (t == Tipo.CANTIL) m.getPersistentDataContainer().set(K_USOS, PersistentDataType.INTEGER, 0);
            m.setMaxStackSize(1);
            PacoteRecursos.marcar(m, t.id);
            descrever(m, t);
        });
        return i;
    }

    private static void descrever(ItemMeta m, Tipo t) {
        m.itemName(Component.text(t.nome, COR));
        List<Component> lore = new ArrayList<>(List.of(
                Component.text(t.linha1, NamedTextColor.GRAY),
                Component.text(t.linha2, NamedTextColor.DARK_GRAY)));
        Integer usos = m.getPersistentDataContainer().get(K_USOS, PersistentDataType.INTEGER);
        if (t == Tipo.CORDA && usos != null) lore.add(Component.text("Descidas: " + usos + "/" + USOS_CORDA, NamedTextColor.WHITE));
        if (t == Tipo.CANTIL && usos != null) lore.add(Component.text("Goles: " + "💧".repeat(usos) + "·".repeat(GOLES - usos), NamedTextColor.AQUA));
        if (t == Tipo.BOLSA) {
            String s = m.getPersistentDataContainer().get(K_SEMENTE, PersistentDataType.STRING);
            Material sem = s == null ? null : Material.matchMaterial(s);
            lore.add(Component.text("Semente: ", NamedTextColor.WHITE).append(sem == null ? Component.text("a primeira que tiver", NamedTextColor.GRAY)
                    : Component.translatable(sem.translationKey(), NamedTextColor.GREEN)));
        }
        m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
    }

    public static Tipo tipo(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return null;
        String id = i.getItemMeta().getPersistentDataContainer().get(K_ARTEFATO, PersistentDataType.STRING);
        if (id == null) return null;
        for (Tipo t : Tipo.values()) if (t.id.equals(id)) return t;
        return null;
    }

    private static boolean segura(Player p, Tipo t) {
        return tipo(p.getInventory().getItemInMainHand()) == t || tipo(p.getInventory().getItemInOffHand()) == t;
    }

    public void registrarReceitas() {
        ShapedRecipe g = new ShapedRecipe(new NamespacedKey(plugin, "garras_escalada"), criar(Tipo.GARRAS));
        g.shape("F F", "ILI");
        g.setIngredient('F', Material.FLINT);
        g.setIngredient('I', Material.IRON_INGOT);
        g.setIngredient('L', Material.LEATHER);
        Bukkit.addRecipe(g);
        ShapedRecipe c = new ShapedRecipe(new NamespacedKey(plugin, "corda_rapel"), criar(Tipo.CORDA));
        c.shape("SSS", "SHS", "SSS");
        c.setIngredient('S', Material.STRING);
        c.setIngredient('H', Material.TRIPWIRE_HOOK);
        Bukkit.addRecipe(c);
        ShapedRecipe p = new ShapedRecipe(new NamespacedKey(plugin, "pendulo_rabdomante"), criar(Tipo.PENDULO));
        p.shape(" S ", " G ", " A ");
        p.setIngredient('S', Material.STRING);
        p.setIngredient('G', Material.GOLD_NUGGET);
        p.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(p);
        ShapedRecipe ct = new ShapedRecipe(new NamespacedKey(plugin, "cantil"), criar(Tipo.CANTIL));
        ct.shape(" S ", "LGL", " L ");
        ct.setIngredient('S', Material.STRING);
        ct.setIngredient('L', Material.LEATHER);
        ct.setIngredient('G', Material.GLASS_BOTTLE);
        Bukkit.addRecipe(ct);
        ShapedRecipe b = new ShapedRecipe(new NamespacedKey(plugin, "bolsa_sementes"), criar(Tipo.BOLSA));
        b.shape("S S", "LWL", "LLL");
        b.setIngredient('S', Material.STRING);
        b.setIngredient('L', Material.LEATHER);
        b.setIngredient('W', Material.WHEAT_SEEDS);
        Bukkit.addRecipe(b);
    }

    // =====================================================================
    //  Cliques: corda, cantil e bolsa
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack mao = e.getItem();
        Tipo t = tipo(mao);
        if (t == null || t == Tipo.GARRAS || t == Tipo.PENDULO) return;
        e.setCancelled(true); // a linha não vira fio de armadilha, o couro não faz nada
        Player p = e.getPlayer();
        switch (t) {
            case CORDA -> descer(p, mao);
            case CANTIL -> cantil(p, mao);
            case BOLSA -> {
                if (p.isSneaking()) trocarSemente(p, mao);
                else if (e.getClickedBlock() != null) plantar(p, mao, e.getClickedBlock());
            }
            default -> { }
        }
    }

    // ---------- Corda de Rapel ----------

    private void descer(Player p, ItemStack mao) {
        if (rapel.containsKey(p.getUniqueId())) {
            parar(p, "Você soltou a corda.");
            return;
        }
        if (System.currentTimeMillis() - esperaCorda.getOrDefault(p.getUniqueId(), 0L) < 2000) return;
        if (!p.isOnGround()) {
            p.sendActionBar(Component.text("Fique em pé na beira do barranco.", NamedTextColor.GRAY));
            return;
        }
        // A beira: o bloco à frente (na altura dos pés) tem pelo menos 4 de vazio para baixo.
        BlockFace f = p.getFacing();
        Block frente = p.getLocation().getBlock().getRelative(f);
        int vazio = 0;
        for (int i = 0; i < 6 && frente.getRelative(BlockFace.DOWN, i).isPassable() && !frente.getRelative(BlockFace.DOWN, i).isLiquid(); i++) vazio++;
        if (vazio < 4) {
            p.sendActionBar(Component.text("Aqui não há onde descer: chegue na beira de um barranco.", NamedTextColor.GRAY));
            return;
        }
        int usos = mao.getItemMeta().getPersistentDataContainer().getOrDefault(K_USOS, PersistentDataType.INTEGER, USOS_CORDA) - 1;
        if (usos <= 0) {
            mao.setAmount(0);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            p.sendMessage(Component.text("A Corda de Rapel se desfez de tanto uso.", NamedTextColor.GRAY));
        } else {
            mao.editMeta(m -> {
                m.getPersistentDataContainer().set(K_USOS, PersistentDataType.INTEGER, usos);
                descrever(m, Tipo.CORDA);
            });
        }
        rapel.put(p.getUniqueId(), p.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5));
        Vector dir = f.getDirection().multiply(0.35);
        p.setVelocity(dir.setY(0.05));
        p.playSound(p.getLocation(), Sound.ITEM_LEAD_TIED, 1f, 0.8f);
        p.sendActionBar(Component.text("🪢 Descendo... agachado segura, pular sobe, clique de novo solta.", COR));
    }

    private void parar(Player p, String msg) {
        rapel.remove(p.getUniqueId());
        esperaCorda.put(p.getUniqueId(), System.currentTimeMillis());
        semQueda.put(p.getUniqueId(), System.currentTimeMillis() + 3000);
        if (msg != null) p.sendActionBar(Component.text(msg, NamedTextColor.GRAY));
    }

    // ---------- Cantil ----------

    private void cantil(Player p, ItemStack mao) {
        int goles = mao.getItemMeta().getPersistentDataContainer().getOrDefault(K_USOS, PersistentDataType.INTEGER, 0);
        Block alvo = p.getTargetBlockExact(4, FluidCollisionMode.ALWAYS);
        if (alvo != null && alvo.getType() == Material.WATER) {
            if (goles >= GOLES) {
                p.sendActionBar(Component.text("O Cantil já está cheio.", NamedTextColor.GRAY));
                return;
            }
            mao.editMeta(m -> {
                m.getPersistentDataContainer().set(K_USOS, PersistentDataType.INTEGER, GOLES);
                descrever(m, Tipo.CANTIL);
            });
            p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 0.9f);
            p.sendActionBar(Component.text("💧 Cantil cheio (3 goles).", NamedTextColor.AQUA));
            return;
        }
        if (goles <= 0) {
            p.sendActionBar(Component.text("O Cantil está vazio: olhe para a água e clique para encher.", NamedTextColor.GRAY));
            return;
        }
        mao.editMeta(m -> {
            m.getPersistentDataContainer().set(K_USOS, PersistentDataType.INTEGER, goles - 1);
            descrever(m, Tipo.CANTIL);
        });
        plugin.temperatura().refrescar(p, 300);
        p.setFireTicks(0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false, true));
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1.1f);
        p.sendActionBar(Component.text("💧 Água fresca: sem calor por 5 minutos.", NamedTextColor.AQUA));
    }

    // ---------- Bolsa de Sementes ----------

    private static boolean semeSimples(ItemStack i) {
        return i != null && SEMENTES.containsKey(i.getType()) && i.isSimilar(new ItemStack(i.getType()));
    }

    private void trocarSemente(Player p, ItemStack mao) {
        List<Material> tem = new ArrayList<>();
        for (Material m : SEMENTES.keySet()) {
            for (ItemStack i : p.getInventory().getContents()) {
                if (semeSimples(i) && i.getType() == m) {
                    tem.add(m);
                    break;
                }
            }
        }
        if (tem.isEmpty()) {
            p.sendActionBar(Component.text("Você não tem sementes no inventário.", NamedTextColor.GRAY));
            return;
        }
        String atual = mao.getItemMeta().getPersistentDataContainer().get(K_SEMENTE, PersistentDataType.STRING);
        int i = atual == null ? -1 : tem.indexOf(Material.matchMaterial(atual));
        Material prox = tem.get((i + 1) % tem.size());
        mao.editMeta(m -> {
            m.getPersistentDataContainer().set(K_SEMENTE, PersistentDataType.STRING, prox.name());
            descrever(m, Tipo.BOLSA);
        });
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.8f, 1.2f);
        p.sendActionBar(Component.text("Bolsa: ", COR).append(Component.translatable(prox.translationKey(), NamedTextColor.GREEN)));
    }

    private void plantar(Player p, ItemStack mao, Block clicado) {
        Block chao = clicado.getType() == Material.FARMLAND ? clicado : clicado.getRelative(BlockFace.DOWN);
        if (chao.getType() != Material.FARMLAND) {
            p.sendActionBar(Component.text("Clique na terra arada.", NamedTextColor.GRAY));
            return;
        }
        if (plugin.combos().vigor(p) < VIGOR_BOLSA) {
            p.sendActionBar(Component.text("Sem Vigor para a Bolsa.", NamedTextColor.RED));
            return;
        }
        String escolhida = mao.getItemMeta().getPersistentDataContainer().get(K_SEMENTE, PersistentDataType.STRING);
        Material semente = escolhida == null ? null : Material.matchMaterial(escolhida);
        if (semente == null) {
            for (Material m : SEMENTES.keySet()) {
                if (contar(p, m) > 0) {
                    semente = m;
                    break;
                }
            }
        }
        if (semente == null || contar(p, semente) == 0) {
            p.sendActionBar(Component.text("Sem sementes desse tipo no inventário.", NamedTextColor.GRAY));
            return;
        }
        int plantadas = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            Block terra = chao.getRelative(x, 0, z);
            Block cima = terra.getRelative(BlockFace.UP);
            if (terra.getType() != Material.FARMLAND || !cima.getType().isAir() || !plugin.territorios().podeConstruir(p, cima)) continue;
            if (!tirarUma(p, semente)) break;
            cima.setType(SEMENTES.get(semente));
            plantadas++;
        }
        if (plantadas == 0) {
            p.sendActionBar(Component.text("Não há terra arada livre em volta.", NamedTextColor.GRAY));
            return;
        }
        plugin.combos().darVigor(p, -VIGOR_BOLSA);
        p.playSound(chao.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
        chao.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, chao.getLocation().add(0.5, 1.2, 0.5), 10, 1, 0.1, 1, 0);
        p.sendActionBar(Component.text("🌱 " + plantadas + " plantadas.", COR));
    }

    private static int contar(Player p, Material m) {
        int n = 0;
        for (ItemStack i : p.getInventory().getContents()) if (semeSimples(i) && i.getType() == m) n += i.getAmount();
        return n;
    }

    private static boolean tirarUma(Player p, Material m) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (semeSimples(i) && i.getType() == m) {
                i.setAmount(i.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  Todo tick: garras e rapel
    // =====================================================================

    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isInsideVehicle()) {
                if (rapel.containsKey(p.getUniqueId())) parar(p, null);
                continue;
            }
            Location ancora = rapel.get(p.getUniqueId());
            if (ancora != null) {
                tickRapel(p, ancora);
                continue;
            }
            if (segura(p, Tipo.GARRAS)) tickGarras(p);
        }
    }

    private void tickRapel(Player p, Location ancora) {
        Location l = p.getLocation();
        if (l.getWorld() != ancora.getWorld() || ancora.getY() - l.getY() > CORDA_MAX || p.isInWater()
                || (p.isOnGround() && ancora.getY() - l.getY() > 1.5)) {
            parar(p, ancora.getY() - l.getY() > CORDA_MAX ? "A corda acabou: segure-se!" : "Você chegou embaixo.");
            return;
        }
        org.bukkit.Input in = p.getCurrentInput();
        Vector v = p.getVelocity();
        double vy = in.isSneak() ? 0 : in.isJump() && l.getY() < ancora.getY() - 1 ? 0.15 : -0.16;
        // Puxa de leve para perto da linha da corda (não fica balançando longe da parede).
        double dx = ancora.getX() - l.getX(), dz = ancora.getZ() - l.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 1.8) {
            v.setX(v.getX() + dx / dist * 0.04);
            v.setZ(v.getZ() + dz / dist * 0.04);
        }
        v.setY(vy);
        p.setVelocity(v);
        p.setFallDistance(0);
        if (p.getTicksLived() % 2 == 0) {
            Location mao = l.clone().add(0, 1.3, 0);
            Vector d = ancora.toVector().subtract(mao.toVector());
            double tam = d.length();
            d.normalize();
            Particle.DustOptions corda = new Particle.DustOptions(Color.fromRGB(0x9C7A4A), 0.6f);
            for (double s = 0; s < tam; s += 0.5) p.getWorld().spawnParticle(Particle.DUST, mao.clone().add(d.clone().multiply(s)), 1, 0, 0, 0, 0, corda);
        }
    }

    private void tickGarras(Player p) {
        org.bukkit.Input in = p.getCurrentInput();
        boolean subir = in.isJump(), pendurar = in.isSneak() && !p.isOnGround();
        if (!subir && !pendurar) {
            semFolego.remove(p.getUniqueId());
            return;
        }
        BlockFace f = p.getFacing();
        Block pes = p.getLocation().getBlock().getRelative(f), olhos = p.getEyeLocation().getBlock().getRelative(f);
        if (!agarra(pes) && !agarra(olhos)) return;
        double custo = subir ? VIGOR_SUBIR : VIGOR_PENDURAR;
        if (plugin.combos().vigor(p) < custo) {
            if (semFolego.add(p.getUniqueId())) p.sendActionBar(Component.text("Sem Vigor: as garras escorregam!", NamedTextColor.RED));
            return;
        }
        plugin.combos().darVigor(p, -custo);
        Vector v = p.getVelocity();
        v.setY(subir ? 0.2 : 0);
        p.setVelocity(v);
        p.setFallDistance(0);
        semQueda.put(p.getUniqueId(), System.currentTimeMillis() + 1500);
        if (subir && p.getTicksLived() % 6 == 0) {
            Block b = agarra(olhos) ? olhos : pes;
            p.getWorld().playSound(b.getLocation(), b.getBlockData().getSoundGroup().getHitSound(), 0.4f, 1.2f);
            p.getWorld().spawnParticle(Particle.BLOCK, p.getEyeLocation().add(f.getDirection().multiply(0.4)), 3, 0.15, 0.15, 0.15, b.getBlockData());
        }
    }

    /** Dá para cravar as garras: sólido e áspero (vidro, gelo, slime e mel escorregam). */
    private static boolean agarra(Block b) {
        Material m = b.getType();
        if (!m.isSolid() || m.name().contains("GLASS") || m.name().contains("ICE") || m == Material.SLIME_BLOCK || m == Material.HONEY_BLOCK
                || m == Material.BARRIER || m.name().endsWith("_DOOR") || m.name().endsWith("_TRAPDOOR")) return false;
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p
                && semQueda.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis()) e.setCancelled(true);
    }

    // =====================================================================
    //  A cada segundo: o pêndulo
    // =====================================================================

    public void tickPendulo() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!segura(p, Tipo.PENDULO)) continue;
            Location l = p.getLocation();
            double est = plugin.estruturas().naoDescobertaMaisPerto(p, l, 160);
            double min = plugin.mineracao().distanciaMaisPerto(l, 24);
            Component c = Component.text("〰 Pêndulo  ", COR)
                    .append(Component.text("Estrutura: ", NamedTextColor.GRAY)).append(nivel(est, 100, 50, 20))
                    .append(Component.text("   Mineral: ", NamedTextColor.GRAY)).append(nivel(min, 16, 8, 4));
            p.sendActionBar(c);
            double perto = Math.max(est < 0 ? 0 : 1 - est / 160, min < 0 ? 0 : 1 - min / 24);
            if (perto > 0) p.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f + (float) perto * 0.5f, 0.6f + (float) perto * 1.2f);
        }
    }

    private static Component nivel(double d, double frio, double morno, double quente) {
        if (d < 0) return Component.text("parado", NamedTextColor.DARK_GRAY);
        if (d > frio) return Component.text("frio", NamedTextColor.AQUA);
        if (d > morno) return Component.text("morno", NamedTextColor.YELLOW);
        if (d > quente) return Component.text("quente", NamedTextColor.GOLD);
        return Component.text("fervendo!", NamedTextColor.RED);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        rapel.remove(id);
        semFolego.remove(id);
        semQueda.remove(id);
        esperaCorda.remove(id);
    }

    /** Para o admin: todos os artefatos. */
    public static Map<String, Tipo> porId() {
        Map<String, Tipo> m = new LinkedHashMap<>();
        for (Tipo t : Tipo.values()) m.put(t.id, t);
        return m;
    }
}
