package br.rpgatributos.dimensao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.estruturas.Estrutura;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.5: a vida nas Luas. Gravidade baixa (pulo alto, queda lenta e macia), sem ar (o Capacete Lunar
 * respira por você; sem ele, 30 segundos de fôlego e depois dano), noite sem fim, os Vagantes
 * Lunares e os Ácaros de Poeira, e a Colônia Lunar dos Antigos (com o capítulo VI do diário).
 */
public final class Luas implements Listener {

    public static final TextColor COR = TextColor.color(0xD8DCE8);
    private static final NamespacedKey K_CAPACETE = new NamespacedKey("rpgatributos", "capacete_lunar");
    private static final NamespacedKey K_GRAV = new NamespacedKey("rpgatributos", "lua_gravidade");
    private static final NamespacedKey K_PULO = new NamespacedKey("rpgatributos", "lua_pulo");
    private static final NamespacedKey K_QUEDA = new NamespacedKey("rpgatributos", "lua_queda");
    private static final NamespacedKey K_LUNAR = new NamespacedKey("rpgatributos", "criatura_lunar");
    private static final int FOLEGO = 30;

    private final RPGAtributos plugin;
    private final Map<UUID, Integer> ar = new HashMap<>();
    /** 3.6: quando cada um chegou na Lua Sombria (ela fecha em 10 minutos). */
    private final Map<UUID, Long> sombria = new HashMap<>();
    private int ciclo;

    public Luas(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  O Capacete Lunar
    // =====================================================================

    public static ItemStack capacete() {
        ItemStack i = new ItemStack(Material.GOLDEN_HELMET);
        i.editMeta(m -> {
            m.itemName(Component.text("Capacete Lunar", COR));
            m.lore(List.of(Component.text("Vidro dos Antigos com um Cristal Abissal que guarda ar.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Na lua, respira por você.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setUnbreakable(true);
            m.getPersistentDataContainer().set(K_CAPACETE, PersistentDataType.BYTE, (byte) 1);
            m.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey("rpgatributos", "capacete_lunar_armadura"), 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
            PacoteRecursos.marcar(m, "capacete_lunar");
        });
        return i;
    }

    public static boolean ehCapacete(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_CAPACETE);
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "capacete_lunar"), capacete());
        r.shape("GCG", "G G");
        r.setIngredient('G', Material.GLASS);
        r.setIngredient('C', new RecipeChoice.ExactChoice(Raro.CRISTAL_ABISSAL.criar(1)));
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    // =====================================================================
    //  A cada segundo
    // =====================================================================

    public void segundo() {
        ciclo++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Dimensoes.Dimensao d = Dimensoes.de(p.getWorld());
            boolean naLua = d != null && d.lua();
            gravidade(p, naLua);
            if (!naLua) {
                ar.remove(p.getUniqueId());
                sombria.remove(p.getUniqueId());
                continue;
            }
            if (d == Dimensoes.Dimensao.LUA_SOMBRIA) tempoSombria(p);
            if (p.isDead() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            respirar(p);
        }
        if (ciclo % 10 == 0) for (Dimensoes.Dimensao d : Dimensoes.Dimensao.values()) {
            if (!d.lua()) continue;
            World w = plugin.dimensoes().mundo(d);
            if (w == null) continue;
            if (w.getTime() != 18000) w.setTime(18000);
            for (Player p : w.getPlayers()) criaturas(p);
        }
    }

    /** A Lua Sombria fecha 10 minutos depois que você chega: avisos e, no fim, a volta à força. */
    private void tempoSombria(Player p) {
        long chegou = sombria.computeIfAbsent(p.getUniqueId(), k -> System.currentTimeMillis());
        long falta = 600_000L - (System.currentTimeMillis() - chegou);
        if (falta <= 0) {
            sombria.remove(p.getUniqueId());
            p.sendMessage(Component.text("✦ A Lua Sombria se fechou. O foguete arrancou sozinho.", TextColor.color(0x8E24AA)));
            plugin.foguetes().forcarVolta(p);
            return;
        }
        long s = falta / 1000;
        if (s % 60 == 0 || s <= 10) p.sendMessage(Component.text("✦ A Lua Sombria fecha em " + (s >= 60 ? s / 60 + " min" : s + " s") + ".", TextColor.color(0x8E24AA)));
    }

    /** Na lua: gravidade a 30%, pulo mais alto e queda segura de mais longe. Fora dela, tudo volta. */
    private static void gravidade(Player p, boolean naLua) {
        modificador(p, Attribute.GRAVITY, K_GRAV, naLua ? -0.056 : 0);
        modificador(p, Attribute.JUMP_STRENGTH, K_PULO, naLua ? 0.25 : 0);
        modificador(p, Attribute.SAFE_FALL_DISTANCE, K_QUEDA, naLua ? 10 : 0);
    }

    private static void modificador(LivingEntity e, Attribute a, NamespacedKey k, double v) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(k);
        if (v == 0) {
            if (atual != null) inst.removeModifier(k);
            return;
        }
        if (atual != null && atual.getAmount() == v) return;
        if (atual != null) inst.removeModifier(k);
        inst.addTransientModifier(new AttributeModifier(k, v, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    /** Sem o Capacete Lunar, o fôlego dura 30 s; depois, 2 de dano por segundo. Com ele, o fôlego volta. */
    private void respirar(Player p) {
        int f = ar.getOrDefault(p.getUniqueId(), FOLEGO);
        boolean capacete = ehCapacete(p.getInventory().getHelmet());
        if (capacete) f = Math.min(FOLEGO, f + 3);
        else f--;
        ar.put(p.getUniqueId(), f);
        if (capacete && f >= FOLEGO) return;
        StringBuilder barra = new StringBuilder();
        int cheias = Math.max(0, (int) Math.ceil(f / 3.0));
        for (int i = 0; i < 10; i++) barra.append(i < cheias ? "●" : "○");
        p.sendActionBar(Component.text("Ar ", NamedTextColor.AQUA).append(Component.text(barra.toString(), f > 9 ? NamedTextColor.AQUA : NamedTextColor.RED))
                .append(Component.text(capacete ? "" : "  (sem Capacete Lunar)", NamedTextColor.GRAY)));
        if (f <= 0) {
            p.damage(2, br.rpgatributos.datapack.Mortes.fonte("sem_ar", null));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1f, 0.8f);
        }
    }

    // =====================================================================
    //  As criaturas da lua
    // =====================================================================

    private void criaturas(Player p) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        World w = p.getWorld();
        int perto = 0;
        for (Entity e : w.getNearbyEntities(p.getLocation(), 40, 30, 40)) if (e.getPersistentDataContainer().has(K_LUNAR)) perto++;
        if (perto >= 6) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble() * Math.PI * 2, dist = 18 + r.nextDouble() * 14;
        int x = (int) (p.getLocation().getX() + Math.cos(a) * dist), z = (int) (p.getLocation().getZ() + Math.sin(a) * dist);
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return;
        Location l = new Location(w, x + 0.5, w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING) + 1, z + 0.5);
        Dimensoes.Dimensao d = Dimensoes.de(w);
        if (d != Dimensoes.Dimensao.LUA_PRATEADA) {
            criaturaDaLua(d, l, r);
            return;
        }
        if (r.nextDouble() < 0.6) {
            Stray s = w.spawn(l, Stray.class, e -> {
                e.customName(Component.text("Vagante Lunar", COR));
                e.setPersistent(false);
                e.setRemoveWhenFarAway(true);
                e.getPersistentDataContainer().set(K_LUNAR, PersistentDataType.BYTE, (byte) 1);
            });
            modificador(s, Attribute.GRAVITY, K_GRAV, -0.056);
            modificador(s, Attribute.JUMP_STRENGTH, K_PULO, 0.25);
        } else {
            for (int i = 0; i < 3; i++) {
                Endermite m = w.spawn(l.clone().add(r.nextDouble(-1, 1), 0, r.nextDouble(-1, 1)), Endermite.class, e -> {
                    e.customName(Component.text("Ácaro de Poeira", NamedTextColor.GRAY));
                    e.setPersistent(false);
                    e.setRemoveWhenFarAway(true);
                    e.getPersistentDataContainer().set(K_LUNAR, PersistentDataType.BYTE, (byte) 1);
                });
                modificador(m, Attribute.GRAVITY, K_GRAV, -0.056);
            }
        }
    }

    /** 3.6: cada lua com os seus: Sombras Famintas na Escura, Sanguessugas na Vermelha, Sentinelas de
     *  Cristal na de Cristal (shulkers que atiram de longe) e Cavaleiros do Eclipse na Sombria. */
    private void criaturaDaLua(Dimensoes.Dimensao d, Location l, ThreadLocalRandom r) {
        World w = l.getWorld();
        java.util.function.Consumer<LivingEntity> marca = e -> {
            e.setPersistent(false);
            e.setRemoveWhenFarAway(true);
            e.getPersistentDataContainer().set(K_LUNAR, PersistentDataType.BYTE, (byte) 1);
            modificador(e, Attribute.GRAVITY, K_GRAV, -0.056);
        };
        switch (d) {
            case LUA_ESCURA -> {
                var e = w.spawn(l, org.bukkit.entity.Enderman.class, x -> { x.customName(Component.text("Sombra Faminta", NamedTextColor.DARK_GRAY)); marca.accept(x); });
                e.setScreaming(true);
            }
            case LUA_VERMELHA -> {
                for (int i = 0; i < 2; i++) w.spawn(l.clone().add(r.nextDouble(-1, 1), 0, r.nextDouble(-1, 1)), org.bukkit.entity.Husk.class,
                        x -> { x.customName(Component.text("Sanguessuga", NamedTextColor.RED)); x.setShouldBurnInDay(false); marca.accept(x); });
            }
            case LUA_CRISTAL -> w.spawn(l, org.bukkit.entity.Shulker.class, x -> {
                x.customName(Component.text("Sentinela de Cristal", TextColor.color(0x64B5F6)));
                x.setColor(org.bukkit.DyeColor.LIGHT_BLUE);
                marca.accept(x);
            });
            case LUA_SOMBRIA -> {
                w.spawn(l, org.bukkit.entity.WitherSkeleton.class, x -> { x.customName(Component.text("Cavaleiro do Eclipse", TextColor.color(0x8E24AA))); marca.accept(x); });
                for (int i = 0; i < 2; i++) w.spawn(l.clone().add(0, 2, 0), org.bukkit.entity.Vex.class, x -> { x.customName(Component.text("Eco do Eclipse", TextColor.color(0xCE93D8))); marca.accept(x); });
            }
            default -> { }
        }
    }

    // =====================================================================
    //  A Colônia Lunar dos Antigos (construída quando o chunk da lua nasce)
    // =====================================================================

    private static final String[] DIARIO_VI = {"VI. A Colônia Lunar",
            "Chegamos. O céu aqui é preto o tempo todo e a poeira sobe devagar quando a gente anda. Construímos a cúpula com o vidro que trouxemos.",
            "O Núcleo aponta para outras luas. Uma escura, que só aparece quando a nossa se apaga. Uma vermelha, que sangra. Uma de cristal, azul, rara. E uma sombria, que só abre no eclipse.",
            "A Prata daqui corta o que não devia estar vivo. Guardem. — Ilen"};

    /** 3.6: um capítulo em cada lua. */
    private static final Map<Dimensoes.Dimensao, String[]> DIARIOS = Map.of(
            Dimensoes.Dimensao.LUA_ESCURA, new String[]{"VII. A Lua Escura",
                    "Aqui a luz não chega. Andamos de mãos dadas e contando os passos. Alguma coisa anda junto, um passo atrás.",
                    "O ônix daqui guarda pontinhos de luz, como se tivesse engolido estrelas. Com ele, a gente some no escuro também.",
                    "Perdi dois aqui. Não ouvi nada. — Ilen"},
            Dimensoes.Dimensao.LUA_VERMELHA, new String[]{"VIII. A Lua Vermelha",
                    "Só se chega quando o céu sangra lá embaixo. A areia é quente e tem gosto de ferro.",
                    "As coisas daqui bebem sangue. O rubi pulsa mais rápido perto de quem está ferido. Aprendemos a usar isso.",
                    "Se a lua lá embaixo voltar ao normal, o caminho fecha. Não fiquem. — Ilen"},
            Dimensoes.Dimensao.LUA_CRISTAL, new String[]{"IX. A Lua de Cristal",
                    "A mais bonita. O chão canta quando a gente pisa e as sentinelas de cristal atiram em quem canta alto demais.",
                    "O cristal celeste deixa as mãos rápidas. Com ele forjamos as ferramentas da última viagem.",
                    "A última viagem não é para outra lua. É para baixo. Para onde as almas vão. — Ilen"},
            Dimensoes.Dimensao.LUA_SOMBRIA, new String[]{"X. A Lua Sombria",
                    "Só abre no eclipse, e por pouco tempo. Quem fica quando fecha volta à força, com a cabeça zunindo.",
                    "Daqui dá para ver um mundo cinza, longe, com um rio e um barqueiro. É para lá que os nossos foram. É para lá que eu vou.",
                    "Se você leu tudo até aqui: obrigado por lembrar de nós. Agora vá buscá-los. — Ilen"});

    public static ItemStack diarioVI() {
        return diario(DIARIO_VI);
    }

    static ItemStack diario(String[] cap) {
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.setTitle("Diário dos Antigos");
            m.setAuthor("Ilen, engenheira");
            List<Component> pg = new ArrayList<>();
            pg.add(Component.text(cap[0] + "\n\n", Antigos.COR, TextDecoration.BOLD).append(Component.text(cap[1], NamedTextColor.BLACK)));
            for (int i = 2; i < cap.length; i++) pg.add(Component.text(cap[i], NamedTextColor.BLACK));
            m.pages(pg);
        });
        return livro;
    }

    @EventHandler
    public void aoNascer(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;
        Dimensoes.Dimensao d = Dimensoes.de(e.getWorld());
        if (d == null || !d.lua()) return;
        Chunk c = e.getChunk();
        Random r = new Random(e.getWorld().getSeed() ^ (c.getX() * 49157L) ^ (c.getZ() * 3125L) ^ 0x1A7AL);
        if (r.nextDouble() >= 0.008) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (c.isLoaded()) colonia(c, r);
        });
    }

    /** Uma cúpula de vidro com base de cobre: o atril, o baú e uma antena. Cabe no chunk. */
    public boolean colonia(Chunk c, Random r) {
        World w = c.getWorld();
        int x0 = c.getX() * 16 + 8, z0 = c.getZ() * 16 + 8;
        int y = w.getHighestBlockYAt(x0, z0, HeightMap.MOTION_BLOCKING);
        for (int dx = -5; dx <= 5; dx += 5) for (int dz = -5; dz <= 5; dz += 5)
            if (Math.abs(w.getHighestBlockYAt(x0 + dx, z0 + dz, HeightMap.MOTION_BLOCKING) - y) > 3) return false;
        int raio = 6;
        for (int dx = -raio; dx <= raio; dx++) for (int dz = -raio; dz <= raio; dz++) {
            double d = Math.hypot(dx, dz);
            if (d > raio + 0.5) continue;
            w.getBlockAt(x0 + dx, y, z0 + dz).setType(d > raio - 0.8 ? Material.WAXED_OXIDIZED_CUT_COPPER : (dx + dz) % 2 == 0 ? Material.SMOOTH_STONE : Material.WAXED_CUT_COPPER, false);
            for (int h = 1; h <= raio; h++) {
                double dist = Math.sqrt(dx * dx + dz * dz + h * h * 1.0);
                Block b = w.getBlockAt(x0 + dx, y + h, z0 + dz);
                if (dist > raio - 0.6 && dist <= raio + 0.4) b.setType(r.nextDouble() < 0.12 ? Material.AIR : Material.LIGHT_GRAY_STAINED_GLASS, false);
                else if (dist <= raio - 0.6) b.setType(Material.AIR, false);
            }
        }
        // Porta, luz, a antena e o que os Antigos deixaram.
        for (int h = 1; h <= 2; h++) w.getBlockAt(x0 + raio, y + h, z0).setType(Material.AIR, false);
        w.getBlockAt(x0, y + raio - 1, z0).setBlockData(Bukkit.createBlockData("minecraft:copper_bulb[lit=true,powered=false]"), false);
        w.getBlockAt(x0, y + raio + 1, z0).setType(Material.LIGHTNING_ROD, false);
        w.getBlockAt(x0, y + raio, z0).setType(Material.WAXED_COPPER_BLOCK, false);
        Block atril = w.getBlockAt(x0 - 2, y + 1, z0);
        atril.setType(Material.LECTERN, false);
        Dimensoes.Dimensao lua = Dimensoes.de(w);
        if (atril.getState() instanceof Lectern l) l.getInventory().setItem(0, DIARIOS.containsKey(lua) ? diario(DIARIOS.get(lua)) : diarioVI());
        Block bau = w.getBlockAt(x0 - 2, y + 1, z0 + 2);
        bau.setType(Material.CHEST, false);
        if (bau.getState() instanceof Chest ch) {
            Raro mineral = switch (lua == null ? Dimensoes.Dimensao.LUA_PRATEADA : lua) {
                case LUA_ESCURA -> Raro.ONIX_ESTELAR;
                case LUA_VERMELHA -> Raro.RUBI_SANGUINEO;
                case LUA_CRISTAL -> Raro.CRISTAL_CELESTE;
                case LUA_SOMBRIA -> Raro.FRAGMENTO_ECLIPSE;
                default -> Raro.PRATA_LUNAR;
            };
            List<ItemStack> l = new ArrayList<>(List.of(mineral.criar(3 + r.nextInt(4)), new ItemStack(Material.GLASS, 8 + r.nextInt(8)),
                    Raro.CRISTAL_ABISSAL.criar(1 + r.nextInt(2))));
            if (r.nextDouble() < 0.4) l.add(capacete());
            if (r.nextDouble() < 0.3) l.add(plugin.arqueologia().reliquiaAleatoria());
            ch.getBlockInventory().addItem(l.toArray(new ItemStack[0]));
        }
        w.getBlockAt(x0 + 2, y + 1, z0 - 2).setType(Material.CARTOGRAPHY_TABLE, false);
        w.getBlockAt(x0 + 2, y + 1, z0 + 2).setType(Material.BARREL, false);
        plugin.estruturas().anotarRuina(w, Estrutura.COLONIA_LUNAR, x0, y + 1, z0);
        return true;
    }

    /** Quando o plugin desliga: tira a gravidade da lua de quem estiver online (volta no próximo segundo). */
    public void desligar() {
        for (Player p : Bukkit.getOnlinePlayers()) gravidade(p, false);
    }

    /** Ao ligar: noite parada nas luas. */
    public void preparar() {
        for (Dimensoes.Dimensao d : Dimensoes.Dimensao.values()) {
            if (!d.lua()) continue;
            World w = plugin.dimensoes().mundo(d);
            if (w == null) continue;
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setTime(18000);
        }
    }
}
