package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mineração e biomas (2.46). Cada bioma tem um mineral próprio (Âmbar no deserto, Jade na selva,
 * Pérola no fundo do mar, Cristal de Gelo nas montanhas geladas, Turfa no pântano e Pedra-Sangue no
 * Nether). Nas cavernas há Geodos de Cristal, lapidados com o Cinzel. Nos rios, a Bateia garimpa
 * pepitas e pedras. Minerando perto de algo raro, a pedra "canta" (Eco da pedra, sem dizer onde).
 * Os blocos são estados do bloco de cogumelo marrom que a natureza não gera (como o Mitrilo), com
 * textura pelo pacote; as posições ficam marcadas no próprio chunk.
 */
public final class Mineracao implements Listener {

    /** Um mineral (ou o cristal do geodo): o estado do bloco, onde nasce e o que deixa. */
    public enum Tipo {
        AMBAR("Âmbar", 3, 0.40, 30, 75, 2, 4),
        JADE("Jade", 5, 0.35, 0, 60, 2, 4),
        PEROLA("Ostra com Pérola", 9, 0.35, 0, 0, 1, 3),
        CRISTAL_DE_GELO("Cristal de Gelo", 17, 0.35, 40, 140, 2, 4),
        PEDRA_SANGUE("Pedra-Sangue", 33, 0.30, 10, 110, 2, 4),
        TURFA("Turfa", 7, 0.50, 0, 0, 3, 6),
        CRISTAL_BRUTO("Cristal Bruto", 11, 0, 0, 0, 0, 0);

        final String nome;
        /** Bits do bloco de cogumelo marrom (sempre com a face de baixo: a natureza nunca faz isso). */
        public final int estado;
        final double chance;
        final int yMin, yMax, veioMin, veioMax;

        Tipo(String nome, int estado, double chance, int yMin, int yMax, int veioMin, int veioMax) {
            this.nome = nome;
            this.estado = estado;
            this.chance = chance;
            this.yMin = yMin;
            this.yMax = yMax;
            this.veioMin = veioMin;
            this.veioMax = veioMax;
        }

        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }

        static Tipo doEstado(int bits) {
            for (Tipo t : values()) if (t.estado == bits) return t;
            return null;
        }
    }

    private static final TextColor COR = TextColor.color(0xD8B46A);
    public static final NamespacedKey K_FERRAMENTA = new NamespacedKey("rpgatributos", "ferramenta_mineral");
    public static final NamespacedKey K_TURFA = new NamespacedKey("rpgatributos", "turfa");

    private final RPGAtributos plugin;
    private final NamespacedKey kGerado, kPos;
    /** Chave do chunk → (posição empacotada → tipo). */
    private final Map<Long, Map<Integer, Tipo>> marcados = new HashMap<>();
    private final Map<UUID, Long> ultimoEco = new HashMap<>();
    /** Cinzel: bloco → [cliques bons, último clique em ms]. */
    private final Map<Block, long[]> lapidando = new HashMap<>();
    private final Map<UUID, Integer> garimpando = new HashMap<>();

    public Mineracao(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kGerado = new NamespacedKey(plugin, "minerais_gerado");
        this.kPos = new NamespacedKey(plugin, "minerais_pos");
    }

    // =====================================================================
    //  Itens: Cinzel, Bateia e Turfa
    // =====================================================================

    public static ItemStack cinzel() {
        ItemStack i = new ItemStack(Material.BRUSH);
        i.editMeta(m -> {
            m.itemName(Component.text("Cinzel do Lapidador", COR));
            m.lore(List.of(
                    Component.text("Clique no Cristal Bruto de um geodo para lapidar.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Com calma sai uma gema; com pressa, ela racha.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_FERRAMENTA, PersistentDataType.STRING, "cinzel");
            PacoteRecursos.marcar(m, "cinzel");
        });
        return i;
    }

    public static ItemStack bateia() {
        ItemStack i = new ItemStack(Material.BOWL);
        i.editMeta(m -> {
            m.itemName(Component.text("Bateia", COR));
            m.lore(List.of(
                    Component.text("Em pé na água rasa, clique para garimpar.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Pepitas, pedras e, às vezes, algo raro. Nas badlands rende mais.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_FERRAMENTA, PersistentDataType.STRING, "bateia");
            m.setMaxStackSize(1);
            PacoteRecursos.marcar(m, "bateia");
        });
        return i;
    }

    public static ItemStack turfa(int qtd) {
        ItemStack i = new ItemStack(Material.COAL, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Turfa", TextColor.color(0x7A5A36)));
            m.lore(List.of(Component.text("Combustível do pântano: queima duas vezes e meia mais que o carvão.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_TURFA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "turfa");
        });
        return i;
    }

    private static String ferramenta(ItemStack i) {
        return i == null || !i.hasItemMeta() ? null : i.getItemMeta().getPersistentDataContainer().get(K_FERRAMENTA, PersistentDataType.STRING);
    }

    public void registrarReceitas() {
        ShapedRecipe c = new ShapedRecipe(new NamespacedKey(plugin, "cinzel"), cinzel());
        c.shape("  A", " I ", "S  ");
        c.setIngredient('A', Material.AMETHYST_SHARD);
        c.setIngredient('I', Material.IRON_INGOT);
        c.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(c);
        ShapedRecipe b = new ShapedRecipe(new NamespacedKey(plugin, "bateia"), bateia());
        b.shape("N N", " B ");
        b.setIngredient('N', Material.IRON_NUGGET);
        b.setIngredient('B', Material.BOWL);
        Bukkit.addRecipe(b);
    }

    // =====================================================================
    //  Marcas no chunk
    // =====================================================================

    private static long chave(World w, int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL) ^ ((long) w.getName().hashCode() << 48);
    }

    private static int empacotar(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y + 2048) << 8);
    }

    private Tipo tipo(Block b) {
        if (b.getType() != Material.BROWN_MUSHROOM_BLOCK) return null;
        Map<Integer, Tipo> m = marcados.get(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4));
        return m == null ? null : m.get(empacotar(b.getX(), b.getY(), b.getZ()));
    }

    private void salvarChunk(Chunk c) {
        Map<Integer, Tipo> m = marcados.get(chave(c.getWorld(), c.getX(), c.getZ()));
        if (m == null || m.isEmpty()) {
            c.getPersistentDataContainer().remove(kPos);
            return;
        }
        int[] v = new int[m.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Tipo> en : m.entrySet()) {
            v[i++] = en.getKey();
            v[i++] = en.getValue().ordinal();
        }
        c.getPersistentDataContainer().set(kPos, PersistentDataType.INTEGER_ARRAY, v);
    }

    private void marcar(Block b, Tipo t) {
        b.setBlockData(dados(t), false);
        marcados.computeIfAbsent(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4), k -> new HashMap<>())
                .put(empacotar(b.getX(), b.getY(), b.getZ()), t);
    }

    private void desmarcar(Block b) {
        Map<Integer, Tipo> m = marcados.get(chave(b.getWorld(), b.getX() >> 4, b.getZ() >> 4));
        if (m == null) return;
        m.remove(empacotar(b.getX(), b.getY(), b.getZ()));
        salvarChunk(b.getChunk());
    }

    static MultipleFacing dados(Tipo t) {
        MultipleFacing d = (MultipleFacing) Material.BROWN_MUSHROOM_BLOCK.createBlockData();
        BlockFace[] ordem = {BlockFace.DOWN, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.WEST};
        for (int i = 0; i < 6; i++) d.setFace(ordem[i], (t.estado >> i & 1) == 1);
        return d;
    }

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        Chunk c = e.getChunk();
        World w = c.getWorld();
        if (plugin.masmorras().ehMundo(w) || w.getEnvironment() == World.Environment.THE_END) return;
        var pdc = c.getPersistentDataContainer();
        if (!pdc.has(kGerado)) {
            pdc.set(kGerado, PersistentDataType.BYTE, (byte) 1);
            gerar(c);
            salvarChunk(c);
            return;
        }
        int[] v = pdc.get(kPos, PersistentDataType.INTEGER_ARRAY);
        if (v == null || v.length < 2) return;
        Map<Integer, Tipo> m = new HashMap<>();
        Tipo[] tipos = Tipo.values();
        for (int i = 0; i + 1 < v.length; i += 2) if (v[i + 1] >= 0 && v[i + 1] < tipos.length) m.put(v[i], tipos[v[i + 1]]);
        marcados.put(chave(w, c.getX(), c.getZ()), m);
    }

    @EventHandler
    public void aoDescarregarChunk(ChunkUnloadEvent e) {
        marcados.remove(chave(e.getWorld(), e.getChunk().getX(), e.getChunk().getZ()));
    }

    // =====================================================================
    //  Geração (na primeira vez que o chunk carrega com o plugin; vale até para áreas antigas)
    // =====================================================================

    private void gerar(Chunk c) {
        World w = c.getWorld();
        Random r = new Random(w.getSeed() ^ (c.getX() * 734287L) ^ (c.getZ() * 912931L) ^ 0x6E1A2L);
        // Perto de territórios ninguém mexe no chão.
        if (plugin.territorios().em(w.getName(), c.getX(), c.getZ()) != null) return;
        if (w.getEnvironment() == World.Environment.NETHER) {
            if (r.nextDouble() < Tipo.PEDRA_SANGUE.chance) veios(c, r, Tipo.PEDRA_SANGUE, m -> m == Material.NETHERRACK);
            return;
        }
        String bioma = c.getBlock(8, Math.min(w.getMaxHeight() - 1, w.getHighestBlockYAt(c.getX() * 16 + 8, c.getZ() * 16 + 8)), 8)
                .getBiome().getKey().getKey();
        if ((bioma.contains("desert") || bioma.contains("badlands")) && r.nextDouble() < Tipo.AMBAR.chance) {
            veios(c, r, Tipo.AMBAR, m -> m == Material.SANDSTONE || m.name().endsWith("TERRACOTTA") || m == Material.STONE);
        }
        if ((bioma.contains("jungle") || bioma.contains("bamboo")) && r.nextDouble() < Tipo.JADE.chance) {
            veios(c, r, Tipo.JADE, m -> m == Material.STONE || m == Material.ANDESITE || m == Material.DIORITE || m == Material.GRANITE);
        }
        if ((bioma.contains("snowy") || bioma.contains("frozen") || bioma.contains("ice") || bioma.contains("grove") || bioma.contains("jagged"))
                && r.nextDouble() < Tipo.CRISTAL_DE_GELO.chance) {
            veios(c, r, Tipo.CRISTAL_DE_GELO, m -> m == Material.STONE || m == Material.PACKED_ICE || m == Material.CALCITE);
        }
        if ((bioma.contains("swamp") || bioma.contains("mangrove")) && r.nextDouble() < Tipo.TURFA.chance) turfa(c, r);
        if (bioma.contains("ocean") && !bioma.contains("frozen") && r.nextDouble() < Tipo.PEROLA.chance) ostras(c, r);
        if (r.nextDouble() < 0.04) geodo(c, r);
    }

    private void veios(Chunk c, Random r, Tipo t, java.util.function.Predicate<Material> hospede) {
        World w = c.getWorld();
        int yMin = Math.max(w.getMinHeight() + 2, t.yMin), yMax = Math.min(w.getMaxHeight() - 2, t.yMax);
        if (yMax <= yMin) return;
        int veios = 1 + r.nextInt(2);
        for (int v = 0, tentativas = 0; v < veios && tentativas < 30; tentativas++) {
            int x = 1 + r.nextInt(10), z = 1 + r.nextInt(10), y = yMin + r.nextInt(yMax - yMin);
            if (!hospede.test(c.getBlock(x, y, z).getType())) continue;
            int tam = t.veioMin + r.nextInt(t.veioMax - t.veioMin + 1);
            for (int i = 0; i < tam; i++) {
                int bx = x + (i % 2 == 0 ? i / 2 : 0), by = y + (i % 3 == 2 ? 1 : 0), bz = z + (i % 2 == 1 ? i / 2 + 1 : 0);
                Block b = c.getBlock(Math.min(15, bx), by, Math.min(15, bz));
                if (hospede.test(b.getType())) marcar(b, t);
            }
            v++;
        }
    }

    /** Turfa: um bolsão logo abaixo da lama ou da terra do pântano. */
    private void turfa(Chunk c, Random r) {
        World w = c.getWorld();
        int x = 3 + r.nextInt(10), z = 3 + r.nextInt(10);
        int topo = w.getHighestBlockYAt(c.getX() * 16 + x, c.getZ() * 16 + z, HeightMap.OCEAN_FLOOR);
        for (int i = 0; i < 12; i++) {
            int dx = x + r.nextInt(3) - 1, dz = z + r.nextInt(3) - 1, dy = topo - 1 - r.nextInt(3);
            Block b = c.getBlock(dx, dy, dz);
            if (b.getType() == Material.MUD || b.getType() == Material.DIRT || b.getType() == Material.CLAY) marcar(b, Tipo.TURFA);
        }
    }

    /** Ostras no fundo do mar (areia, cascalho ou argila). */
    private void ostras(Chunk c, Random r) {
        World w = c.getWorld();
        int n = 1 + r.nextInt(3);
        for (int i = 0, t = 0; i < n && t < 12; t++) {
            int x = 1 + r.nextInt(14), z = 1 + r.nextInt(14);
            int y = w.getHighestBlockYAt(c.getX() * 16 + x, c.getZ() * 16 + z, HeightMap.OCEAN_FLOOR) - 1;
            Block b = c.getBlock(x, y, z);
            if (b.getRelative(BlockFace.UP).getType() != Material.WATER) continue;
            if (b.getType() == Material.SAND || b.getType() == Material.GRAVEL || b.getType() == Material.CLAY) {
                marcar(b, Tipo.PEROLA);
                i++;
            }
        }
    }

    private static boolean pedra(Material m) {
        return m == Material.STONE || m == Material.DEEPSLATE || m == Material.TUFF || m == Material.ANDESITE
                || m == Material.DIORITE || m == Material.GRANITE || m == Material.CALCITE || m == Material.SMOOTH_BASALT;
    }

    /** Geodo de Cristal: casca de basalto liso e calcita, forrado de Cristal Bruto, oco por dentro. */
    private void geodo(Chunk c, Random r) {
        World w = c.getWorld();
        int cx = 5 + r.nextInt(6), cz = 5 + r.nextInt(6);
        int yMin = Math.max(w.getMinHeight() + 8, -48), yMax = 24;
        int cy = yMin + r.nextInt(yMax - yMin);
        // Só em rocha maciça (nada de abrir no meio de uma caverna).
        int[][] pontos = {{0, 0, 0}, {4, 0, 0}, {-4, 0, 0}, {0, 4, 0}, {0, -4, 0}, {0, 0, 4}, {0, 0, -4}};
        for (int[] p : pontos) if (!pedra(c.getBlock(cx + p[0], cy + p[1], cz + p[2]).getType())) return;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d > 4.6) continue;
                    int x = cx + dx, z = cz + dz;
                    if (x < 0 || x > 15 || z < 0 || z > 15) continue;
                    Block b = c.getBlock(x, cy + dy, z);
                    // Só rocha natural vira geodo (o que alguém construiu fica intacto, mesmo em área antiga).
                    if (!pedra(b.getType())) continue;
                    if (d <= 2.4) b.setType(Material.AIR, false);
                    else if (d <= 3.1) {
                        if (r.nextDouble() < 0.35) marcar(b, Tipo.CRISTAL_BRUTO);
                        else b.setType(Material.CALCITE, false);
                    } else if (d <= 3.7) b.setType(Material.CALCITE, false);
                    else if (pedra(b.getType())) b.setType(Material.SMOOTH_BASALT, false);
                }
            }
        }
    }

    // =====================================================================
    //  Minerar
    // =====================================================================

    private static boolean picareta(ItemStack i) {
        return i != null && Tag.ITEMS_PICKAXES.isTagged(i.getType()) && i.getType() != Material.WOODEN_PICKAXE;
    }

    private static boolean pa(ItemStack i) {
        return i != null && Tag.ITEMS_SHOVELS.isTagged(i.getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void aoBater(BlockDamageEvent e) {
        Tipo t = tipo(e.getBlock());
        if (t == null) return;
        ItemStack mao = e.getItemInHand();
        boolean leve = t == Tipo.TURFA || t == Tipo.PEROLA;
        if (!leve && !picareta(mao)) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("✖ " + t.nome + " só sai com picareta de pedra ou melhor.", NamedTextColor.RED));
            return;
        }
        e.setInstaBreak(false);
        // O cogumelo quebra na hora: com cansaço de mineração ele fica duro como minério.
        int nivel = leve ? (pa(mao) ? 0 : 1) : 1;
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, nivel, false, false, false));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        Tipo t = tipo(b);
        Player p = e.getPlayer();
        if (t == null) {
            eco(p, b);
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (t != Tipo.TURFA && t != Tipo.PEROLA && !picareta(mao) && p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            return;
        }
        e.setDropItems(false);
        e.setExpToDrop(t == Tipo.TURFA ? 1 : 4);
        desmarcar(b);
        p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int fortuna = mao.getEnchantmentLevel(Enchantment.FORTUNE);
        int extra = fortuna > 0 && r.nextInt(fortuna + 2) >= 2 ? 1 : 0;
        List<ItemStack> drops = new ArrayList<>();
        int xp;
        switch (t) {
            case AMBAR -> {
                drops.add(Raro.AMBAR.criar(1 + extra));
                if (r.nextDouble() < 0.04) {
                    drops.add(plugin.arqueologia().reliquiaAleatoria());
                    p.sendMessage(Component.text("✦ Tinha algo preso dentro do âmbar!", COR));
                }
                xp = 35;
            }
            case JADE -> { drops.add(Raro.JADE.criar(1 + extra)); xp = 35; }
            case CRISTAL_DE_GELO -> { drops.add(Raro.CRISTAL_DE_GELO.criar(1 + extra)); xp = 35; }
            case PEDRA_SANGUE -> { drops.add(Raro.PEDRA_SANGUE.criar(1 + extra)); xp = 40; }
            case TURFA -> { drops.add(turfa(2 + r.nextInt(2) + extra)); xp = 8; }
            case PEROLA -> {
                if (r.nextDouble() < 0.6) drops.add(Raro.PEROLA.criar(1 + extra));
                else {
                    drops.add(new ItemStack(Material.SAND));
                    p.sendActionBar(Component.text("A ostra estava vazia.", NamedTextColor.GRAY));
                }
                xp = 20;
            }
            case CRISTAL_BRUTO -> {
                // Na picareta o cristal estilhaça: só fragmentos (o certo é o Cinzel).
                drops.add(new ItemStack(Material.AMETHYST_SHARD, 1 + r.nextInt(3)));
                if (r.nextDouble() < 0.1) drops.add(Gema.values()[r.nextInt(Gema.values().length)].criar(1, 1));
                p.sendActionBar(Component.text("O cristal estilhaçou. Com o Cinzel do Lapidador ele viraria uma gema.", NamedTextColor.GRAY));
                xp = 10;
            }
            default -> xp = 0;
        }
        for (ItemStack d : drops) b.getWorld().dropItemNaturally(c, d);
        b.getWorld().spawnParticle(Particle.WAX_ON, c, 10, 0.3, 0.3, 0.3, 0);
        b.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.1f);
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.MINERACAO, xp);
        if (t != Tipo.CRISTAL_BRUTO && t != Tipo.TURFA) plugin.titulos().registrar(p, "minerais_bioma", 1);
    }

    // =====================================================================
    //  Cinzel: lapidar o Cristal Bruto do geodo
    // =====================================================================

    private static final int CLIQUES_CINZEL = 4;

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        String f = ferramenta(e.getItem());
        if ("bateia".equals(f) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            garimpar(p);
            return;
        }
        if (!"cinzel".equals(f) || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (tipo(b) != Tipo.CRISTAL_BRUTO) return;
        e.setCancelled(true);
        long agora = System.currentTimeMillis();
        long[] estado = lapidando.computeIfAbsent(b, k -> new long[]{0, 0});
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        if (estado[1] != 0 && agora - estado[1] < 450) {
            // Pressa: chance de rachar.
            if (r.nextDouble() < 0.35) {
                lapidando.remove(b);
                desmarcar(b);
                b.setType(Material.CALCITE, false);
                b.getWorld().dropItemNaturally(c, new ItemStack(Material.AMETHYST_SHARD, 1 + r.nextInt(2)));
                b.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
                p.sendActionBar(Component.text("✖ Rachou! Devagar com o Cinzel.", NamedTextColor.RED));
                gastar(p, e.getItem());
                return;
            }
        }
        estado[0]++;
        estado[1] = agora;
        b.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 0.8f + estado[0] * 0.2f);
        b.getWorld().spawnParticle(Particle.WAX_OFF, c, 6, 0.3, 0.3, 0.3, 0);
        if (estado[0] < CLIQUES_CINZEL) {
            p.sendActionBar(Component.text("Lapidando... " + "◆".repeat((int) estado[0]) + "◇".repeat(CLIQUES_CINZEL - (int) estado[0]), COR));
            return;
        }
        lapidando.remove(b);
        desmarcar(b);
        b.setType(Material.CALCITE, false);
        double nivel = plugin.stats().getNivel(p, Skill.MINERACAO) / (double) Math.max(1, plugin.settings().nivelMaximo);
        double x = r.nextDouble();
        int grau = x < 0.05 + 0.15 * nivel ? 3 : x < 0.35 + 0.25 * nivel ? 2 : 1;
        Gema g = Gema.values()[r.nextInt(Gema.values().length)];
        b.getWorld().dropItemNaturally(c, g.criar(grau, 1));
        b.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
        b.getWorld().spawnParticle(Particle.END_ROD, c, 15, 0.3, 0.3, 0.3, 0.03);
        p.sendActionBar(Component.text("✦ " + g.nome(grau) + "!", COR, TextDecoration.BOLD));
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.MINERACAO, 40 + grau * 20);
        plugin.titulos().registrar(p, "geodos", 1);
        gastar(p, e.getItem());
    }

    private static void gastar(Player p, ItemStack i) {
        if (p.getGameMode() != GameMode.CREATIVE && i != null) i.damage(1, p);
    }

    // =====================================================================
    //  Bateia: garimpar na água rasa
    // =====================================================================

    private void garimpar(Player p) {
        if (garimpando.containsKey(p.getUniqueId())) return;
        Block pe = p.getLocation().getBlock();
        Material chao = pe.getRelative(BlockFace.DOWN).getType();
        boolean agua = pe.getType() == Material.WATER || p.isInWater();
        if (!agua || !(chao == Material.SAND || chao == Material.GRAVEL || chao == Material.CLAY || chao == Material.DIRT || chao == Material.MUD
                || chao == Material.RED_SAND)) {
            p.sendActionBar(Component.text("Fique em pé na água rasa, com areia, cascalho ou argila no fundo.", NamedTextColor.GRAY));
            return;
        }
        Location inicio = p.getLocation();
        int[] passos = {0};
        int tarefa = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !p.getWorld().equals(inicio.getWorld()) || p.getLocation().distanceSquared(inicio) > 1) {
                parar(p);
                if (p.isOnline()) p.sendActionBar(Component.text("A água turvou. Tente de novo parado.", NamedTextColor.GRAY));
                return;
            }
            passos[0]++;
            p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(0, 0.6, 0), 8, 0.3, 0.1, 0.3, 0.05);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.4f, 1.6f);
            if (passos[0] < 6) {
                p.sendActionBar(Component.text("Garimpando " + "●".repeat(passos[0]) + "○".repeat(6 - passos[0]), COR));
                return;
            }
            parar(p);
            premio(p, pe.getBiome().getKey().getKey());
        }, 10L, 10L).getTaskId();
        garimpando.put(p.getUniqueId(), tarefa);
    }

    private void parar(Player p) {
        Integer t = garimpando.remove(p.getUniqueId());
        if (t != null) Bukkit.getScheduler().cancelTask(t);
    }

    private void premio(Player p, String bioma) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean badlands = bioma.contains("badlands"), deserto = bioma.contains("desert") || badlands, mar = bioma.contains("ocean") || bioma.contains("beach");
        double x = r.nextDouble();
        ItemStack i;
        if (x < (badlands ? 0.2 : 0.35)) i = null;
        else if (x < 0.58) i = new ItemStack(Material.GOLD_NUGGET, 1 + r.nextInt(badlands ? 5 : 3));
        else if (x < 0.72) i = new ItemStack(Material.IRON_NUGGET, 1 + r.nextInt(3));
        else if (x < 0.81) i = new ItemStack(r.nextBoolean() ? Material.FLINT : Material.CLAY_BALL, 1 + r.nextInt(2));
        else if (x < 0.89) i = new ItemStack(Material.RAW_GOLD, badlands ? 2 : 1);
        else if (x < 0.94) i = Gema.values()[r.nextInt(Gema.values().length)].criar(1, 1);
        else if (x < 0.97) i = deserto ? Raro.AMBAR.criar(1) : new ItemStack(Material.GOLD_NUGGET, 3);
        else if (x < 0.99) i = mar ? Raro.PEROLA.criar(1) : new ItemStack(Material.IRON_NUGGET, 3);
        else i = plugin.arqueologia().reliquiaAleatoria();
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.MINERACAO, 10);
        plugin.titulos().registrar(p, "garimpos", 1);
        if (i == null) {
            p.sendActionBar(Component.text("Só areia e pedrinhas desta vez.", NamedTextColor.GRAY));
            return;
        }
        for (ItemStack sobra : p.getInventory().addItem(i).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        String nome = i.getItemMeta() != null && i.getItemMeta().hasItemName()
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(i.getItemMeta().itemName())
                : i.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        p.sendActionBar(Component.text("✦ Na bateia: " + i.getAmount() + "x " + nome, COR));
    }

    // =====================================================================
    //  Eco da pedra (Mineração 20% do máximo): a pedra canta perto de algo raro
    // =====================================================================

    private static final double RAIO_ECO = 6;

    private void eco(Player p, Block b) {
        if (!pedra(b.getType()) && b.getType() != Material.NETHERRACK && b.getType() != Material.SANDSTONE) return;
        int minimo = (int) Math.ceil(0.2 * plugin.settings().nivelMaximo);
        if (plugin.stats().getNivel(p, Skill.MINERACAO) < minimo) return;
        long agora = System.currentTimeMillis();
        if (agora - ultimoEco.getOrDefault(p.getUniqueId(), 0L) < 4000) return;
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        double d = -1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Map<Integer, Tipo> m = marcados.get(chave(b.getWorld(), (b.getX() >> 4) + dx, (b.getZ() >> 4) + dz));
                if (m == null) continue;
                int baseX = ((b.getX() >> 4) + dx) * 16, baseZ = ((b.getZ() >> 4) + dz) * 16;
                for (int k : m.keySet()) {
                    double ox = baseX + (k & 15) + 0.5, oz = baseZ + ((k >> 4) & 15) + 0.5, oy = (k >> 8) - 2048 + 0.5;
                    double dist = Math.sqrt((ox - l.getX()) * (ox - l.getX()) + (oy - l.getY()) * (oy - l.getY()) + (oz - l.getZ()) * (oz - l.getZ()));
                    if (dist <= RAIO_ECO && (d < 0 || dist < d)) d = dist;
                }
            }
        }
        double mit = plugin.mitrilo().distanciaMaisPerto(l, RAIO_ECO);
        if (mit >= 0 && (d < 0 || mit < d)) d = mit;
        if (d < 0) return;
        ultimoEco.put(p.getUniqueId(), agora);
        float tom = (float) (0.6 + (1 - d / RAIO_ECO) * 1.2);
        p.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, tom);
        p.spawnParticle(Particle.WAX_ON, l, 4, 0.2, 0.2, 0.2, 0);
        p.sendActionBar(Component.text("♪ A pedra canta... há algo raro por perto (quanto mais agudo, mais perto).", COR));
    }

    // =====================================================================
    //  Turfa queima mais; explosões e pistões não levam os minerais
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQueimar(FurnaceBurnEvent e) {
        ItemStack f = e.getFuel();
        if (f.hasItemMeta() && f.getItemMeta().getPersistentDataContainer().has(K_TURFA)) e.setBurnTime(4000);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> tipo(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> tipo(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (tipo(b) != null) { e.setCancelled(true); return; }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (tipo(b) != null) { e.setCancelled(true); return; }
    }

    // =====================================================================
    //  Admin e pacote de recursos
    // =====================================================================

    /** Admin: põe um mineral (ou cristal de geodo) no bloco. */
    public void colocar(Block b, Tipo t) {
        marcar(b, t);
        salvarChunk(b.getChunk());
    }

    /** Admin: gera um geodo perto (no chunk do bloco). */
    public void geodoAqui(Block b) {
        Chunk c = b.getChunk();
        geodo(c, new Random());
        salvarChunk(c);
    }

    /** Textura 16x16 do bloco de cada mineral. */
    static BufferedImage textura(Tipo t) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(t.ordinal() * 7919L + 13);
        int[] base = switch (t) {
            case AMBAR -> new int[]{0xD8C08A, 0xC8AE76, 0xE6D2A0};
            case JADE, CRISTAL_DE_GELO -> new int[]{0x7E7E7E, 0x6E6E6E, 0x8E8E8E};
            case PEROLA -> new int[]{0xD8CC96, 0xC8BA84, 0xE4DAA8};
            case PEDRA_SANGUE -> new int[]{0x6E2A2A, 0x5A1E1E, 0x823434};
            case TURFA -> new int[]{0x3E2C1C, 0x34241A, 0x4A3624};
            case CRISTAL_BRUTO -> new int[]{0xE6E2DA, 0xD6D2CA, 0xF2EEE8};
        };
        int[] pedra = switch (t) {
            case AMBAR -> new int[]{0x5A2E06, 0xB0600E, 0xF2A33A, 0xFFD27F};
            case JADE -> new int[]{0x14402A, 0x2E7D50, 0x4CAF7D, 0x9FE3BE};
            case PEROLA -> new int[]{0x8A7A88, 0xD3C3CF, 0xF3E9F0, 0xFFFFFF};
            case CRISTAL_DE_GELO -> new int[]{0x2E6E94, 0x6FB6DE, 0xA8E4FF, 0xE6F7FF};
            case PEDRA_SANGUE -> new int[]{0x3A0008, 0x7A0A1E, 0xB0102A, 0xFF4D6A};
            case TURFA -> new int[]{0x1E140C, 0x5A4026, 0x6B8E3A, 0x8AAE4A};
            case CRISTAL_BRUTO -> new int[]{0x4A1A7A, 0x7A3AB0, 0xB66BFF, 0xE1BEFF};
        };
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) img.setRGB(x, y, 0xFF000000 | base[r.nextInt(3)]);
        }
        int[][] pontos = t == Tipo.TURFA ? new int[][]{{2, 3}, {8, 2}, {12, 6}, {4, 9}, {10, 11}, {1, 13}, {13, 13}}
                : new int[][]{{3, 3}, {10, 2}, {6, 8}, {12, 10}, {2, 12}, {9, 13}};
        for (int[] c : pontos) {
            img.setRGB(c[0], c[1], 0xFF000000 | pedra[0]);
            img.setRGB(c[0] + 1, c[1], 0xFF000000 | pedra[1]);
            img.setRGB(c[0], c[1] + 1, 0xFF000000 | pedra[2]);
            img.setRGB(c[0] + 1, c[1] + 1, 0xFF000000 | pedra[3]);
            if (c[0] + 2 < 16) img.setRGB(c[0] + 2, c[1] + 1, 0xFF000000 | pedra[1]);
        }
        return img;
    }

    /** Os estados do cogumelo marrom que viram minerais (para o pacote de recursos). */
    public static Map<Integer, String> modelos() {
        Map<Integer, String> m = new HashMap<>();
        for (Tipo t : Tipo.values()) m.put(t.estado, "rpgatributos:block/mineral_" + t.id());
        return m;
    }

    public static Map<String, BufferedImage> texturas() {
        Map<String, BufferedImage> m = new HashMap<>();
        for (Tipo t : Tipo.values()) m.put("block/mineral_" + t.id(), textura(t));
        return m;
    }
}
