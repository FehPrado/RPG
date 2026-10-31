package br.rpgatributos.mundo;

import br.rpgatributos.ModoTeste;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.Display;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.11: a natureza viva, na pegada do jogo de sempre.
 * <ul>
 *   <li><b>Animais lendários:</b> um de cada no mundo inteiro (Cervo Dourado, Baleia Celeste, Lobo Lunar,
 *       Fênix e Urso Ancião). De tempos em tempos corre um boato com o lugar aproximado; quem achar e
 *       clicar nele ganha a lembrança (um troféu) e o servidor fica sabendo. Ninguém mata: eles somem.</li>
 *   <li><b>Migração:</b> no outono e na primavera passam bandos de pássaros no céu (para o sul no outono,
 *       para o norte na primavera) e manadas de cavalos selvagens atravessam as planícies (dá para domar).</li>
 *   <li><b>Ninhos selvagens:</b> o mel tirado de um ninho de abelha natural às vezes vem com o sabor das
 *       flores de perto (Mel Rubro, Solar, Azul, de Tulipa, Branco) e, raramente, uma Geleia Real.</li>
 *   <li><b>Rede de pesca:</b> jogue a rede na água e volte depois: ela pega peixes sozinha (até 6).</li>
 *   <li><b>Árvores anciãs:</b> algumas árvores das florestas são muito velhas e escorrem resina; com um
 *       frasco, uma vez por dia, sai a Seiva Anciã.</li>
 * </ul>
 */
public final class NaturezaViva implements Listener {

    public static final TextColor COR = TextColor.color(0x7CB342);
    public static final NamespacedKey K_LEMBRANCA = new NamespacedKey("rpgatributos", "lembranca_lendaria");
    private static final NamespacedKey K_LENDARIO = new NamespacedKey("rpgatributos", "animal_lendario");
    private static final NamespacedKey K_ACHADOS = new NamespacedKey("rpgatributos", "lendarios_achados");
    private static final NamespacedKey K_MANADA = new NamespacedKey("rpgatributos", "manada");
    private static final NamespacedKey K_MEL = new NamespacedKey("rpgatributos", "mel_silvestre");
    private static final NamespacedKey K_REDE_ITEM = new NamespacedKey("rpgatributos", "rede_de_pesca");
    private static final NamespacedKey K_REDE = new NamespacedKey("rpgatributos", "rede_na_agua");
    private static final NamespacedKey K_ANCIA = new NamespacedKey("rpgatributos", "arvore_ancia");
    private static final NamespacedKey K_ANCIA_DIA = new NamespacedKey("rpgatributos", "arvore_ancia_dia");
    private static final NamespacedKey K_SEIVA = new NamespacedKey("rpgatributos", "seiva_ancia");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =====================================================================
    //  Os lendários
    // =====================================================================

    public enum Lendario {
        CERVO_DOURADO("Cervo Dourado", 0xFFD54F, "Chifre do Cervo Dourado", Material.GOAT_HORN, "florestas", "forest", "birch", "cherry", "meadow"),
        BALEIA_CELESTE("Baleia Celeste", 0x80DEEA, "Escama da Baleia Celeste", Material.PRISMARINE_SHARD, "oceanos", "ocean"),
        LOBO_LUNAR("Lobo Lunar", 0xE0E0FF, "Presa do Lobo Lunar", Material.BONE, "taigas, à noite", "taiga", "grove", "snowy"),
        FENIX("Fênix", 0xFF7043, "Pena da Fênix", Material.FEATHER, "savanas, selvas e terras áridas", "savanna", "jungle", "badlands", "desert"),
        URSO_ANCIAO("Urso Ancião", 0xA1887F, "Garra do Urso Ancião", Material.FLINT, "montanhas e neve", "snowy", "frozen", "ice", "peaks", "slopes", "taiga");

        final String nome, lembranca, onde;
        final TextColor cor;
        final Material item;
        final String[] biomas;

        Lendario(String nome, int cor, String lembranca, Material item, String onde, String... biomas) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.lembranca = lembranca;
            this.item = item;
            this.onde = onde;
            this.biomas = biomas;
        }

        boolean combina(String bioma) {
            if (this == BALEIA_CELESTE) return bioma.contains("ocean") && !bioma.contains("frozen");
            for (String b : biomas) if (bioma.contains(b)) return true;
            return false;
        }
    }

    private final RPGAtributos plugin;
    private final Map<Lendario, UUID> vivos = new HashMap<>();
    private final Map<Lendario, Long> somem = new HashMap<>();
    private final List<UUID> manadas = new ArrayList<>();
    private long manadaSome;
    private int ciclo;

    public NaturezaViva(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private boolean ligado(String chave) {
        return plugin.getConfig().getBoolean("natureza." + chave, true);
    }

    /** O mundo principal (lendários, manadas e pássaros ficam nele). */
    private static World principal() {
        return Bukkit.getWorlds().getFirst();
    }

    private static String bioma(Location l) {
        return l.getWorld().getBiome(l).getKey().getKey();
    }

    public static ItemStack lembranca(Lendario l, String quem) {
        ItemStack i = new ItemStack(l.item);
        i.editMeta(m -> {
            m.itemName(Component.text(l.lembranca, l.cor, TextDecoration.BOLD));
            m.lore(List.of(Component.text("Lembrança de um encontro com o " + l.nome + ".", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Encontrado por " + quem + " em " + LocalDate.now().format(DATA) + ".", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Exponha na Estante de Troféus.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(K_LEMBRANCA, PersistentDataType.STRING, l.name());
            PacoteRecursos.marcar(m, "lembranca_" + l.name().toLowerCase());
        });
        return i;
    }

    /** A cada minuto: talvez um boato novo; e os lendários que ninguém achou vão embora. */
    private void minutoLendarios() {
        if (!ligado("lendarios")) return;
        long agora = System.currentTimeMillis();
        for (Lendario l : Lendario.values()) {
            UUID id = vivos.get(l);
            if (id == null) continue;
            Entity e = Bukkit.getEntity(id);
            if (e == null || !e.isValid() || agora > somem.getOrDefault(l, 0L)) {
                if (e != null && e.isValid()) {
                    e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);
                    e.remove();
                }
                vivos.remove(l);
            }
        }
        int minutos = Math.max(5, plugin.getConfig().getInt("natureza.lendarios-minutos", 40));
        if (ThreadLocalRandom.current().nextInt(minutos) != 0) return;
        soltarLendario(null, null);
    }

    /** Solta um lendário perto de alguém (ou do jogador dado), num bioma que combine. @return mensagem para o admin. */
    public String soltarLendario(Lendario escolhido, Player perto) {
        List<Player> gente = new ArrayList<>();
        for (Player p : principal().getPlayers()) if (!ModoTeste.ehTeste(p) && p.getGameMode() != GameMode.SPECTATOR) gente.add(p);
        if (perto != null) gente = List.of(perto);
        if (gente.isEmpty()) return "Ninguém no mundo principal.";
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int t = 0; t < 30; t++) {
            Player p = gente.get(r.nextInt(gente.size()));
            double a = r.nextDouble() * Math.PI * 2, d = perto != null ? 20 + r.nextDouble() * 20 : 90 + r.nextDouble() * 70;
            int x = (int) (p.getLocation().getX() + Math.cos(a) * d), z = (int) (p.getLocation().getZ() + Math.sin(a) * d);
            World w = p.getWorld();
            if (w != principal()) continue;
            Block chao = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location l = chao.getLocation().add(0.5, 1, 0.5);
            String b = bioma(l);
            List<Lendario> possiveis = new ArrayList<>();
            for (Lendario le : Lendario.values()) {
                if (vivos.containsKey(le) || (escolhido != null && le != escolhido)) continue;
                if (le == Lendario.LOBO_LUNAR && !Ceu.noite(w) && perto == null) continue;
                if (le.combina(b)) possiveis.add(le);
            }
            if (possiveis.isEmpty()) continue;
            Lendario le = possiveis.get(r.nextInt(possiveis.size()));
            if (le == Lendario.BALEIA_CELESTE) {
                if (!chao.isLiquid()) continue;
                l = chao.getLocation().add(0.5, -2, 0.5);
            } else if (chao.isLiquid() || !chao.getType().isSolid()) continue;
            LivingEntity e = criar(le, l);
            vivos.put(le, e.getUniqueId());
            somem.put(le, System.currentTimeMillis() + 20 * 60_000L);
            int bx = (int) (Math.round(l.getX() / 50.0) * 50 + r.nextInt(-30, 31)), bz = (int) (Math.round(l.getZ() / 50.0) * 50 + r.nextInt(-30, 31));
            Bukkit.broadcast(Component.text("🍃 Boato: viram o ", COR).append(Component.text(le.nome, le.cor, TextDecoration.BOLD))
                    .append(Component.text(" perto de " + bx + ", " + bz + ". Ele não fica muito tempo.", COR)));
            return le.nome + " solto em " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + ".";
        }
        return "Não achei um lugar que combine perto de ninguém (tente em outro bioma).";
    }

    private LivingEntity criar(Lendario le, Location l) {
        World w = l.getWorld();
        java.util.function.Consumer<LivingEntity> marca = e -> {
            e.customName(Component.text(le.nome, le.cor, TextDecoration.BOLD));
            e.setCustomNameVisible(true);
            e.setInvulnerable(true);
            e.setPersistent(false);
            e.setRemoveWhenFarAway(false);
            e.getPersistentDataContainer().set(K_LENDARIO, PersistentDataType.STRING, le.name());
        };
        LivingEntity e = switch (le) {
            case CERVO_DOURADO -> w.spawn(l, Goat.class, g -> { marca.accept(g); escala(g, 1.5); });
            case BALEIA_CELESTE -> w.spawn(l, Dolphin.class, d -> { marca.accept(d); escala(d, 4); });
            case LOBO_LUNAR -> w.spawn(l, Wolf.class, x -> { marca.accept(x); escala(x, 1.6); x.setVariant(Wolf.Variant.SNOWY); });
            case FENIX -> w.spawn(l.clone().add(0, 3, 0), Parrot.class, x -> { marca.accept(x); escala(x, 2.2); x.setVariant(Parrot.Variant.RED); });
            case URSO_ANCIAO -> w.spawn(l, PolarBear.class, x -> { marca.accept(x); escala(x, 1.8); });
        };
        w.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.6f);
        return e;
    }

    private static void escala(LivingEntity e, double v) {
        var a = e.getAttribute(Attribute.SCALE);
        if (a != null) a.setBaseValue(v);
    }

    private static Lendario lendario(Entity e) {
        String s = e.getPersistentDataContainer().get(K_LENDARIO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Lendario.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTocarLendario(PlayerInteractEntityEvent e) {
        Lendario le = lendario(e.getRightClicked());
        if (le == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Entity ent = e.getRightClicked();
        Location l = ent.getLocation();
        ent.remove();
        vivos.remove(le);
        l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0, 1, 0), 60, 0.6, 0.8, 0.6, 0.3);
        l.getWorld().playSound(l, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.getInventory().addItem(lembranca(le, p.getName())).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.giveExp(250);
        p.showTitle(Title.title(Component.text(le.nome, le.cor, TextDecoration.BOLD), Component.text("Ele deixou uma lembrança e sumiu na mata.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
        Bukkit.broadcast(Component.text("🍃 " + p.getName() + " encontrou o ", COR).append(Component.text(le.nome, le.cor, TextDecoration.BOLD)).append(Component.text("!", COR)));
        String achados = p.getPersistentDataContainer().getOrDefault(K_ACHADOS, PersistentDataType.STRING, "");
        if (!List.of(achados.split(",")).contains(le.name())) {
            p.getPersistentDataContainer().set(K_ACHADOS, PersistentDataType.STRING, achados.isEmpty() ? le.name() : achados + "," + le.name());
            plugin.titulos().registrar(p, "lendarios_achados", 1);
            plugin.diario().marco(p, "lendario_" + le.name().toLowerCase(), "Encontrou o " + le.nome);
        }
    }

    /** Lendários não atacam ninguém (o urso e o lobo também não). */
    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetEvent e) {
        if (lendario(e.getEntity()) != null) e.setCancelled(true);
    }

    private void particulasLendarios() {
        for (Map.Entry<Lendario, UUID> en : vivos.entrySet()) {
            Entity e = Bukkit.getEntity(en.getValue());
            if (e == null) continue;
            Particle p = en.getKey() == Lendario.FENIX ? Particle.FLAME : en.getKey() == Lendario.BALEIA_CELESTE ? Particle.BUBBLE_POP : Particle.END_ROD;
            e.getWorld().spawnParticle(p, e.getLocation().add(0, e.getHeight() * 0.6, 0), 4, 0.4, 0.4, 0.4, 0.01);
        }
    }

    // =====================================================================
    //  Migração: pássaros no céu e manadas de cavalos
    // =====================================================================

    private void minutoMigracao() {
        if (!ligado("migracao") || plugin.estacoes() == null) return;
        Estacoes.Estacao est = plugin.estacoes().atual();
        if (est != Estacoes.Estacao.OUTONO && est != Estacoes.Estacao.PRIMAVERA) return;
        World w = principal();
        long t = w.getTime();
        if (t > 12500) return; // de dia
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (Player p : w.getPlayers()) {
            if (ModoTeste.ehTeste(p) || p.getLocation().getBlockY() < w.getHighestBlockYAt(p.getLocation()) - 2) continue;
            if (r.nextDouble() < 0.08) bando(p, est == Estacoes.Estacao.OUTONO);
        }
        // Manadas: uma por vez, no outono e na primavera, numa planície ou savana perto de alguém.
        if (System.currentTimeMillis() > manadaSome) limparManada();
        if (manadas.isEmpty() && r.nextDouble() < 0.05) {
            List<Player> gente = new ArrayList<>(w.getPlayers());
            gente.removeIf(ModoTeste::ehTeste);
            if (!gente.isEmpty()) manada(gente.get(r.nextInt(gente.size())));
        }
    }

    /** Um bando em V (penas voando bem alto), atravessando o céu por cima do jogador. */
    public void bando(Player p, boolean paraSul) {
        World w = p.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double dirZ = paraSul ? 1 : -1, dirX = r.nextDouble(-0.3, 0.3);
        Vector dir = new Vector(dirX, 0, dirZ).normalize();
        Location inicio = p.getLocation().clone().add(dir.clone().multiply(-60)).add(r.nextDouble(-10, 10), 0, 0);
        inicio.setY(Math.max(p.getLocation().getY() + 30, w.getHighestBlockYAt(p.getLocation()) + 25));
        Vector lado = new Vector(-dir.getZ(), 0, dir.getX());
        List<ItemDisplay> aves = new ArrayList<>();
        int n = 7 + r.nextInt(6);
        for (int i = 0; i < n; i++) {
            int fila = (i + 1) / 2;
            double s = i % 2 == 0 ? 1 : -1;
            Location l = inicio.clone().add(dir.clone().multiply(-fila * 2.2)).add(lado.clone().multiply(s * fila * 2.0));
            aves.add(w.spawn(l, ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.FEATHER));
                d.setBillboard(Display.Billboard.CENTER);
                d.setPersistent(false);
                d.setTeleportDuration(10);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.4f, 1.4f, 1.4f), new AxisAngle4f()));
            }));
        }
        w.playSound(p.getLocation(), Sound.ENTITY_PARROT_AMBIENT, 0.6f, 0.5f);
        int[] passo = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            passo[0]++;
            for (int i = 0; i < aves.size(); i++) {
                ItemDisplay d = aves.get(i);
                if (!d.isValid()) continue;
                double bate = Math.sin((passo[0] + i) * 0.9) * 0.3;
                d.teleport(d.getLocation().add(dir.clone().multiply(6)).add(0, bate, 0));
            }
            if (passo[0] % 6 == 0) w.playSound(aves.getFirst().getLocation(), Sound.ENTITY_PARROT_FLY, 1.2f, 0.7f);
            if (passo[0] >= 24) {
                aves.forEach(Entity::remove);
                task.cancel();
            }
        }, 10L, 10L);
    }

    /** Uma manada de cavalos selvagens que atravessa a planície. Domados, ficam. */
    public String manada(Player p) {
        World w = p.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int t = 0; t < 15; t++) {
            double a = r.nextDouble() * Math.PI * 2;
            int x = (int) (p.getLocation().getX() + Math.cos(a) * 45), z = (int) (p.getLocation().getZ() + Math.sin(a) * 45);
            Block chao = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (chao.isLiquid()) continue;
            Location l = chao.getLocation().add(0.5, 1, 0.5);
            String b = bioma(l);
            if (!b.contains("plains") && !b.contains("savanna") && !b.contains("meadow")) continue;
            Location destino = l.clone().add(-Math.cos(a) * 140, 0, -Math.sin(a) * 140);
            Horse.Color[] cores = Horse.Color.values();
            int n = 5 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                Location li = l.clone().add(r.nextDouble(-4, 4), 0, r.nextDouble(-4, 4));
                Horse h = w.spawn(li, Horse.class, x2 -> {
                    x2.setColor(cores[r.nextInt(cores.length)]);
                    x2.setStyle(Horse.Style.values()[r.nextInt(Horse.Style.values().length)]);
                    x2.getPersistentDataContainer().set(K_MANADA, PersistentDataType.BYTE, (byte) 1);
                    x2.setRemoveWhenFarAway(false);
                    if (r.nextDouble() < 0.25) x2.setAdult(); else if (r.nextDouble() < 0.3) x2.setBaby();
                });
                manadas.add(h.getUniqueId());
            }
            manadaSome = System.currentTimeMillis() + 5 * 60_000L;
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                boolean algum = false;
                for (UUID id : manadas) {
                    if (!(Bukkit.getEntity(id) instanceof Horse h) || !h.isValid() || h.isTamed() || h.getPassengers().size() > 0) continue;
                    algum = true;
                    h.getPathfinder().moveTo(destino.clone().add(r.nextDouble(-5, 5), 0, r.nextDouble(-5, 5)), 1.6);
                }
                if (!algum || System.currentTimeMillis() > manadaSome) task.cancel();
            }, 20L, 100L);
            w.playSound(l, Sound.ENTITY_HORSE_GALLOP, 2f, 0.8f);
            p.sendMessage(Component.text("🐎 Uma manada de cavalos selvagens está passando por perto. Dá para domar um!", COR));
            return "Manada solta em " + l.getBlockX() + " " + l.getBlockZ() + ".";
        }
        return "Nenhuma planície ou savana perto.";
    }

    /** A manada vai embora (os domados e os montados ficam). */
    private void limparManada() {
        for (UUID id : manadas) {
            if (Bukkit.getEntity(id) instanceof Horse h && h.isValid() && !h.isTamed() && h.getPassengers().isEmpty()) h.remove();
            else if (Bukkit.getEntity(id) instanceof Horse h) h.getPersistentDataContainer().remove(K_MANADA);
        }
        manadas.clear();
    }

    // =====================================================================
    //  Ninhos selvagens: mel das flores e Geleia Real
    // =====================================================================

    private enum Mel {
        RUBRO("Mel Rubro", 0xE53935, PotionEffectType.STRENGTH, 30),
        SOLAR("Mel Solar", 0xFDD835, PotionEffectType.HASTE, 60),
        AZUL("Mel Azul", 0x42A5F5, PotionEffectType.NIGHT_VISION, 90),
        TULIPA("Mel de Tulipa", 0xF06292, PotionEffectType.REGENERATION, 12),
        BRANCO("Mel Branco", 0xF5F5F5, PotionEffectType.RESISTANCE, 30),
        GELEIA("Geleia Real", 0xFFB300, PotionEffectType.ABSORPTION, 120);

        final String nome;
        final TextColor cor;
        final PotionEffectType efeito;
        final int segundos;

        Mel(String nome, int cor, PotionEffectType efeito, int segundos) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.efeito = efeito;
            this.segundos = segundos;
        }
    }

    private static ItemStack mel(Mel m) {
        ItemStack i = new ItemStack(Material.HONEY_BOTTLE);
        i.editMeta(x -> {
            x.itemName(Component.text(m.nome, m.cor));
            x.lore(List.of(Component.text(m == Mel.GELEIA ? "Rara: o alimento da rainha." : "Mel de ninho selvagem, com o sabor das flores de perto.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            x.getPersistentDataContainer().set(K_MEL, PersistentDataType.STRING, m.name());
            PacoteRecursos.marcar(x, "mel_" + m.name().toLowerCase());
        });
        return i;
    }

    /** Qual mel as flores perto do ninho dão (a flor que mais aparece até 5 blocos). */
    private static Mel melDasFlores(Block ninho) {
        Map<Mel, Integer> c = new HashMap<>();
        for (int dx = -5; dx <= 5; dx++) for (int dy = -3; dy <= 3; dy++) for (int dz = -5; dz <= 5; dz++) {
            Material m = ninho.getRelative(dx, dy, dz).getType();
            Mel me = switch (m) {
                case POPPY, ROSE_BUSH -> Mel.RUBRO;
                case DANDELION, SUNFLOWER -> Mel.SOLAR;
                case BLUE_ORCHID, CORNFLOWER -> Mel.AZUL;
                case RED_TULIP, ORANGE_TULIP, PINK_TULIP, WHITE_TULIP, PEONY -> Mel.TULIPA;
                case LILY_OF_THE_VALLEY, OXEYE_DAISY, AZURE_BLUET -> Mel.BRANCO;
                default -> null;
            };
            if (me != null) c.merge(me, 1, Integer::sum);
        }
        return c.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTirarMel(PlayerInteractEvent e) {
        if (!ligado("ninhos") || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.BEE_NEST) return;
        if (e.getItem() == null || e.getItem().getType() != Material.GLASS_BOTTLE) return;
        if (!(e.getClickedBlock().getBlockData() instanceof Beehive bh) || bh.getHoneyLevel() < bh.getMaximumHoneyLevel()) return;
        if (plugin.ehEstacao(e.getClickedBlock())) return; // a Colmeia do Apicultor tem as regras dela
        Player p = e.getPlayer();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Mel m = r.nextDouble() < 0.04 ? Mel.GELEIA : r.nextDouble() < 0.45 ? melDasFlores(e.getClickedBlock()) : null;
        if (m == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.getInventory().addItem(mel(m)).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
            p.sendActionBar(Component.text("🍯 O ninho selvagem deu " + m.nome + "!", m.cor));
            plugin.stats().darXp(p, Skill.AGRICULTURA, m == Mel.GELEIA ? 60 : 15);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void aoBeber(PlayerItemConsumeEvent e) {
        var pdc = e.getItem().getPersistentDataContainer();
        Player p = e.getPlayer();
        String mel = pdc.get(K_MEL, PersistentDataType.STRING);
        if (mel != null) {
            try {
                Mel m = Mel.valueOf(mel);
                p.addPotionEffect(new PotionEffect(m.efeito, m.segundos * 20, m == Mel.GELEIA ? 1 : 0, false, true, true));
                if (m == Mel.BRANCO) p.removePotionEffect(PotionEffectType.POISON);
            } catch (IllegalArgumentException ignorado) { }
            return;
        }
        if (pdc.has(K_SEIVA)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 40, 0, false, false, true));
            plugin.stats().darXp(p, Skill.AGRICULTURA, 40);
        }
    }

    // =====================================================================
    //  Rede de pesca
    // =====================================================================

    public static ItemStack rede() {
        ItemStack i = new ItemStack(Material.COBWEB);
        i.editMeta(m -> {
            m.itemName(Component.text("Rede de Pesca", TextColor.color(0x4FC3F7)));
            m.lore(List.of(Component.text("Clique na água para lançar. Volte depois: ela pega até 6 peixes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Clique na rede para tirar os peixes; agachado, recolhe a rede.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(16);
            m.getPersistentDataContainer().set(K_REDE_ITEM, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "rede_de_pesca");
        });
        return i;
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "rede_de_pesca"), rede());
        r.shape("S S", "SGS", "S S");
        r.setIngredient('S', Material.STRING);
        r.setIngredient('G', Material.STICK);
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoLancarRede(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getItem() == null || !e.getItem().getPersistentDataContainer().has(K_REDE_ITEM)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        var hit = p.rayTraceBlocks(6, FluidCollisionMode.ALWAYS);
        Block agua = hit == null ? null : hit.getHitBlock();
        if (agua == null || agua.getType() != Material.WATER || agua.getRelative(BlockFace.UP).getType() != Material.AIR) {
            p.sendActionBar(Component.text("Mire na superfície da água.", NamedTextColor.RED));
            return;
        }
        if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, agua)) {
            p.sendActionBar(Component.text("Aqui você não pode lançar a rede.", NamedTextColor.RED));
            return;
        }
        int minhas = 0;
        for (Interaction i : p.getWorld().getEntitiesByClass(Interaction.class))
            if (p.getUniqueId().toString().equals(i.getPersistentDataContainer().get(K_REDE, PersistentDataType.STRING))) minhas++;
        if (minhas >= 3) {
            p.sendActionBar(Component.text("Você já tem 3 redes na água neste mundo.", NamedTextColor.RED));
            return;
        }
        Location l = agua.getLocation().add(0.5, 0.9, 0.5);
        ItemDisplay visual = p.getWorld().spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(Material.COBWEB));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0), new Vector3f(1.6f, 1.6f, 1.6f), new AxisAngle4f()));
        });
        p.getWorld().spawn(l.clone().add(0, -0.2, 0), Interaction.class, i -> {
            i.setInteractionWidth(1.4f);
            i.setInteractionHeight(0.5f);
            i.setResponsive(true);
            var pdc = i.getPersistentDataContainer();
            pdc.set(K_REDE, PersistentDataType.STRING, p.getUniqueId().toString());
            pdc.set(new NamespacedKey("rpgatributos", "rede_desde"), PersistentDataType.LONG, System.currentTimeMillis());
            pdc.set(new NamespacedKey("rpgatributos", "rede_visual"), PersistentDataType.STRING, visual.getUniqueId().toString());
        });
        if (p.getGameMode() != GameMode.CREATIVE) e.getItem().setAmount(e.getItem().getAmount() - 1);
        p.getWorld().playSound(l, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 0.8f);
        p.sendActionBar(Component.text("🎣 Rede na água. Volte em alguns minutos.", TextColor.color(0x4FC3F7)));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMexerNaRede(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction i) || e.getHand() != EquipmentSlot.HAND) return;
        var pdc = i.getPersistentDataContainer();
        String dono = pdc.get(K_REDE, PersistentDataType.STRING);
        if (dono == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!dono.equals(p.getUniqueId().toString()) && !p.hasPermission("rpgatributos.admin")) {
            p.sendActionBar(Component.text("A rede é de outra pessoa.", NamedTextColor.GRAY));
            return;
        }
        NamespacedKey kDesde = new NamespacedKey("rpgatributos", "rede_desde");
        long desde = pdc.getOrDefault(kDesde, PersistentDataType.LONG, System.currentTimeMillis());
        double minutos = (System.currentTimeMillis() - desde) / 60000.0;
        boolean chuva = i.getWorld().hasStorm();
        int peixes = (int) Math.min(6, minutos / (chuva ? 1.5 : 2.5));
        List<ItemStack> pegou = peixes(i.getLocation(), peixes);
        for (ItemStack s : pegou) p.getInventory().addItem(s).values().forEach(x -> p.getWorld().dropItem(p.getLocation(), x));
        if (!pegou.isEmpty()) plugin.stats().darXp(p, Skill.PESCA, 6 * pegou.size());
        if (p.isSneaking()) {
            Entity v = visual(i);
            if (v != null) v.remove();
            i.remove();
            p.getInventory().addItem(rede()).values().forEach(x -> p.getWorld().dropItem(p.getLocation(), x));
            p.sendActionBar(Component.text("Rede recolhida" + (pegou.isEmpty() ? "." : " com " + pegou.size() + " achado(s)."), TextColor.color(0x4FC3F7)));
        } else {
            pdc.set(kDesde, PersistentDataType.LONG, System.currentTimeMillis());
            p.sendActionBar(Component.text(pegou.isEmpty() ? "Nada ainda. Espere mais um pouco." : "🎣 A rede trouxe " + pegou.size() + " achado(s).", TextColor.color(0x4FC3F7)));
        }
        p.playSound(i.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1f);
    }

    private static Entity visual(Interaction i) {
        String s = i.getPersistentDataContainer().get(new NamespacedKey("rpgatributos", "rede_visual"), PersistentDataType.STRING);
        try {
            return s == null ? null : Bukkit.getEntity(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** O que a rede pega, pelo lugar (mar, rio, pântano) e um pouco de sorte. */
    private static List<ItemStack> peixes(Location l, int n) {
        List<ItemStack> l2 = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String b = bioma(l);
        for (int i = 0; i < n; i++) {
            double x = r.nextDouble();
            Material m;
            if (x < 0.06) m = r.nextDouble() < 0.15 ? Material.NAUTILUS_SHELL : Material.INK_SAC;
            else if (x < 0.14) m = Material.KELP;
            else if (b.contains("ocean")) m = b.contains("warm") || b.contains("lukewarm") ? (r.nextBoolean() ? Material.TROPICAL_FISH : Material.PUFFERFISH) : (r.nextDouble() < 0.6 ? Material.COD : Material.SALMON);
            else if (b.contains("river")) m = r.nextDouble() < 0.6 ? Material.SALMON : Material.COD;
            else m = Material.COD;
            l2.add(new ItemStack(m));
        }
        return l2;
    }

    // =====================================================================
    //  Árvores anciãs
    // =====================================================================

    @EventHandler
    public void aoNascerChunk(ChunkLoadEvent e) {
        if (!e.isNewChunk() || !ligado("arvores-ancias") || e.getWorld() != principal()) return;
        Chunk c = e.getChunk();
        Random r = new Random(e.getWorld().getSeed() ^ (c.getX() * 341873128712L) ^ (c.getZ() * 132897987541L) ^ 0xA2C1A5L);
        if (r.nextDouble() >= 0.03) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (c.isLoaded()) ancia(c, r);
        });
    }

    /** Acha um tronco no chunk (numa floresta) e põe a resina que escorre dele. @return true se pôs. */
    public boolean ancia(Chunk c, Random r) {
        World w = c.getWorld();
        for (int t = 0; t < 24; t++) {
            int x = c.getX() * 16 + 2 + r.nextInt(12), z = c.getZ() * 16 + 2 + r.nextInt(12);
            Block topo = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING);
            if (!Tag.LEAVES.isTagged(topo.getType())) continue;
            if (!bioma(topo.getLocation()).contains("forest") && !bioma(topo.getLocation()).contains("taiga") && !bioma(topo.getLocation()).contains("jungle")) continue;
            Block b = topo;
            while (b.getY() > w.getMinHeight() + 1 && !Tag.LOGS.isTagged(b.getType()) && (Tag.LEAVES.isTagged(b.getType()) || b.getType().isAir())) b = b.getRelative(BlockFace.DOWN);
            if (!Tag.LOGS.isTagged(b.getType())) continue;
            while (Tag.LOGS.isTagged(b.getRelative(BlockFace.DOWN).getType())) b = b.getRelative(BlockFace.DOWN);
            Block base = b;
            int postas = 0;
            for (int dy = 1; dy <= 4 && postas < 3; dy++) {
                Block tronco = base.getRelative(0, dy, 0);
                if (!Tag.LOGS.isTagged(tronco.getType())) break;
                for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                    Block ar = tronco.getRelative(f);
                    if (!ar.getType().isAir() || r.nextDouble() < 0.5) continue;
                    ar.setBlockData(Bukkit.createBlockData("minecraft:resin_clump[" + f.getOppositeFace().name().toLowerCase() + "=true]"), false);
                    postas++;
                    break;
                }
            }
            // Musgo nas raízes.
            for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                Block chao = base.getRelative(f).getRelative(BlockFace.DOWN);
                if (chao.getType() == Material.GRASS_BLOCK || chao.getType() == Material.DIRT || chao.getType() == Material.PODZOL) chao.setType(Material.MOSS_BLOCK, false);
            }
            if (postas == 0) continue;
            c.getPersistentDataContainer().set(K_ANCIA, PersistentDataType.STRING, base.getX() + "," + base.getY() + "," + base.getZ());
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoColherSeiva(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.RESIN_CLUMP) return;
        if (e.getItem() == null || e.getItem().getType() != Material.GLASS_BOTTLE || e.getHand() != EquipmentSlot.HAND) return;
        Chunk c = e.getClickedBlock().getChunk();
        String s = c.getPersistentDataContainer().get(K_ANCIA, PersistentDataType.STRING);
        if (s == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        long dia = e.getClickedBlock().getWorld().getFullTime() / 24000;
        if (c.getPersistentDataContainer().getOrDefault(K_ANCIA_DIA, PersistentDataType.LONG, -1L) == dia) {
            p.sendActionBar(Component.text("A seiva ainda está escorrendo. Volte amanhã.", NamedTextColor.GRAY));
            return;
        }
        c.getPersistentDataContainer().set(K_ANCIA_DIA, PersistentDataType.LONG, dia);
        e.getItem().setAmount(e.getItem().getAmount() - 1);
        ItemStack seiva = new ItemStack(Material.HONEY_BOTTLE);
        seiva.editMeta(m -> {
            m.itemName(Component.text("Seiva Anciã", TextColor.color(0xE08A2E)));
            m.lore(List.of(Component.text("Escorre de uma árvore mais velha que as vilas.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Beber: Regeneração II e saciedade.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_SEIVA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "seiva_ancia");
        });
        p.getInventory().addItem(seiva).values().forEach(x -> p.getWorld().dropItem(p.getLocation(), x));
        p.playSound(e.getClickedBlock().getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 0.7f);
        e.getClickedBlock().getWorld().spawnParticle(Particle.DRIPPING_HONEY, e.getClickedBlock().getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0);
        plugin.titulos().registrar(p, "seiva_ancia", 1);
        String[] b = s.split(",");
        if (!p.getPersistentDataContainer().has(new NamespacedKey("rpgatributos", "ancia_" + b[0] + "_" + b[2]))) {
            p.getPersistentDataContainer().set(new NamespacedKey("rpgatributos", "ancia_" + b[0] + "_" + b[2]), PersistentDataType.BYTE, (byte) 1);
            plugin.diario().marco(p, "arvore_ancia", "Achou uma árvore anciã");
        }
    }

    // =====================================================================
    //  Relógio
    // =====================================================================

    public void segundo() {
        ciclo++;
        particulasLendarios();
        if (ciclo % 60 == 0) {
            minutoLendarios();
            minutoMigracao();
        }
    }

    /** Lendários não apanham (já são invulneráveis; isto cobre o resto, como o vazio). */
    @EventHandler(ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (lendario(e.getEntity()) != null && e.getCause() != EntityDamageEvent.DamageCause.VOID) e.setCancelled(true);
    }

    public void desligar() {
        for (UUID id : vivos.values()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        vivos.clear();
        limparManada();
    }

    public static Lendario lendarioPorNome(String s) {
        for (Lendario l : Lendario.values()) if (l.name().equalsIgnoreCase(s)) return l;
        return null;
    }

    /** Para o mapa e a galeria: o lendário desta lembrança. */
    public static Lendario deLembranca(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return null;
        return lendarioPorNome(i.getPersistentDataContainer().getOrDefault(K_LEMBRANCA, PersistentDataType.STRING, ""));
    }

}
