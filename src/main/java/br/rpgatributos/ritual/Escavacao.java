package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 2.87: o Golem Minerador cava um túnel de mineração a partir do posto, na direção em que o dono
 * olhava ao pôr o Selo. Corredor 3x3, com sustentação (2 pares de cercas e uma viga de tábuas)
 * a cada 4 blocos, tocha a cada 8 e uma câmara 5x5 a cada 16. Um bloco por vez, na velocidade
 * da picareta. Tampa lava, água e cascalho solto em volta, pega os minérios que aparecem nas
 * paredes e guarda tudo no baú do começo do túnel. O comprimento máximo sobe com o nível.
 */
final class Escavacao {

    static final NamespacedKey K_DIRECAO = new NamespacedKey("rpgatributos", "tunel_direcao");
    static final NamespacedKey K_PROGRESSO = new NamespacedKey("rpgatributos", "tunel_progresso");
    private static final NamespacedKey K_PONTEIRO = new NamespacedKey("rpgatributos", "tunel_ponteiro");
    private static final int[] COMPRIMENTO = {24, 40, 56, 72, 96};
    /** Pedra e terra: se não couber no baú, some (não enche o chão de itens). */
    private static final Set<Material> ENTULHO = EnumSet.of(Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.DIRT,
            Material.GRAVEL, Material.NETHERRACK, Material.TUFF, Material.GRANITE, Material.DIORITE, Material.ANDESITE,
            Material.CALCITE, Material.BASALT, Material.BLACKSTONE, Material.SAND, Material.SANDSTONE, Material.FLINT,
            Material.SMOOTH_BASALT, Material.DRIPSTONE_BLOCK, Material.POINTED_DRIPSTONE, Material.CLAY_BALL);
    private static final Set<Material> DUROS = EnumSet.of(Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.ANCIENT_DEBRIS,
            Material.RESPAWN_ANCHOR, Material.NETHERITE_BLOCK);

    private final RPGAtributos plugin;
    private final Map<UUID, Integer> espera = new HashMap<>();

    Escavacao(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    void esquecer(UUID id) {
        espera.remove(id);
    }

    static int comprimentoMax(int nivel) {
        return COMPRIMENTO[Math.max(0, Math.min(4, nivel - 1))];
    }

    static BlockFace direcao(Mob g) {
        String s = g.getPersistentDataContainer().get(K_DIRECAO, PersistentDataType.STRING);
        try { return s == null ? BlockFace.EAST : BlockFace.valueOf(s); } catch (IllegalArgumentException e) { return BlockFace.EAST; }
    }

    /** Novo túnel: direção e progresso do zero (ao pôr o Selo ou mudar o posto). */
    static void comecar(org.bukkit.entity.Entity g, BlockFace dir) {
        g.getPersistentDataContainer().set(K_DIRECAO, PersistentDataType.STRING, dir.name());
        g.getPersistentDataContainer().set(K_PROGRESSO, PersistentDataType.INTEGER, 0);
        g.getPersistentDataContainer().set(K_PONTEIRO, PersistentDataType.INTEGER, 0);
    }

    static int progresso(org.bukkit.entity.Entity g) {
        return g.getPersistentDataContainer().getOrDefault(K_PROGRESSO, PersistentDataType.INTEGER, 0);
    }

    private static BlockFace lado(BlockFace dir) {
        return switch (dir) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case EAST -> BlockFace.SOUTH;
            default -> BlockFace.NORTH;
        };
    }

    /** Fatia {@code k} faz parte de uma câmara 5x5 (as 5 últimas de cada 16)? */
    private static boolean camara(int k) {
        return (k - 1) % 16 >= 11;
    }

    private static int meiaLargura(int k) {
        return camara(k) ? 2 : 1;
    }

    private static Block em(Block posto, BlockFace dir, int along, int side, int dy) {
        BlockFace l = lado(dir);
        return posto.getRelative(dir.getModX() * along + l.getModX() * side, dy, dir.getModZ() * along + l.getModZ() * side);
    }

    /** Os blocos da fatia {@code k}, de cima para baixo. */
    private static List<Block> fatia(Block posto, BlockFace dir, int k) {
        List<Block> l = new ArrayList<>();
        int h = meiaLargura(k);
        for (int dy = 2; dy >= 0; dy--) for (int s = -h; s <= h; s++) l.add(em(posto, dir, k, s, dy));
        return l;
    }

    /** O bloco já é (ou vai ser) parte do túnel até a fatia {@code ate}? */
    private static boolean dentro(Block posto, BlockFace dir, Block b, int ate) {
        BlockFace l = lado(dir);
        int dx = b.getX() - posto.getX(), dz = b.getZ() - posto.getZ(), dy = b.getY() - posto.getY();
        int along = dx * dir.getModX() + dz * dir.getModZ();
        int side = dx * l.getModX() + dz * l.getModZ();
        return along >= 0 && along <= ate && dy >= 0 && dy <= 2 && Math.abs(side) <= (along == 0 ? 1 : meiaLargura(along));
    }

    /**
     * Um passo do minerador. @return o aviso para mostrar no nome (ou null se está tudo bem).
     */
    String trabalhar(Mob g, int nivel, boolean asas, int ciclo) {
        ItemStack picareta = Equipamento.ferramenta(g);
        if (picareta == null) return "⚠ sem picareta";
        Block posto = Guardioes.posto(g).getBlock();
        BlockFace dir = direcao(g);
        int feitas = progresso(g);
        int max = comprimentoMax(nivel);
        if (feitas >= max) return "túnel pronto (" + feitas + " blocos)";
        int k = feitas + 1;
        // Fica dentro do túnel, de frente para a fatia que está cavando.
        Location pe = em(posto, dir, Math.max(0, k - 2), 0, 0).getLocation().add(0.5, 0, 0.5);
        if (g.getLocation().distanceSquared(pe) > 3.5 * 3.5) {
            if (asas) Guardioes.voarPara(g, pe.clone().add(0, 0.5, 0), 0.4);
            else if (ciclo % 10 == 0) g.getPathfinder().moveTo(pe, 1.0);
            return null;
        }
        if (asas) g.setGravity(true);
        double vel = Equipamento.velocidade(picareta) * (1 + 0.1 * (nivel - 1));
        int intervalo = (int) Math.max(2, Math.round(6 / vel));
        int n = espera.merge(g.getUniqueId(), 1, Integer::sum);
        if (n < intervalo) return null;
        espera.put(g.getUniqueId(), 0);

        List<Block> blocos = fatia(posto, dir, k);
        int ponteiro = g.getPersistentDataContainer().getOrDefault(K_PONTEIRO, PersistentDataType.INTEGER, 0);
        while (ponteiro < blocos.size()) {
            Block b = blocos.get(ponteiro);
            if (b.getType().isAir()) { ponteiro++; continue; }
            String erro = podeCavar(g, b, picareta);
            if (erro != null) {
                // Parede que não dá para cavar: o túnel acaba aqui.
                g.getPersistentDataContainer().set(K_PROGRESSO, PersistentDataType.INTEGER, max);
                avisarDono(g, "⛏ O túnel parou: " + erro + ".");
                return "túnel parou: " + erro;
            }
            selar(posto, dir, b, k);
            cavar(g, b, picareta, posto);
            ponteiro++;
            if (Equipamento.gastar(g)) {
                g.getPersistentDataContainer().set(K_PONTEIRO, PersistentDataType.INTEGER, ponteiro);
                avisarDono(g, "⛏ A picareta do Golem Minerador quebrou. Dê outra para ele continuar.");
                return "⚠ sem picareta";
            }
            break; // um bloco por passo
        }
        if (ponteiro < blocos.size()) {
            g.getPersistentDataContainer().set(K_PONTEIRO, PersistentDataType.INTEGER, ponteiro);
            return null;
        }
        // Fatia pronta: minérios das paredes, sustentação, tocha.
        g.getPersistentDataContainer().set(K_PONTEIRO, PersistentDataType.INTEGER, 0);
        g.getPersistentDataContainer().set(K_PROGRESSO, PersistentDataType.INTEGER, k);
        mineriosDaParede(g, posto, dir, k, picareta);
        String aviso = null;
        if (k % 4 == 0 && !camara(k) && !sustentar(g, posto, dir, k)) aviso = "⚠ faltam 4 cercas e 3 tábuas no baú";
        if (k % 8 == 2 && !camara(k) && !tocha(g, posto, dir, k)) aviso = "⚠ faltam tochas no baú";
        if (k == max) avisarDono(g, "⛏ O túnel ficou pronto: " + max + " blocos. (Mais nível, mais túnel.)");
        return aviso;
    }

    private String podeCavar(Mob g, Block b, ItemStack picareta) {
        Material m = b.getType();
        if (m.getHardness() < 0) return "rocha indestrutível";
        if (b.getState() instanceof TileState) return "um bloco com coisas dentro (baú, fornalha...)";
        if (plugin.ehEstacao(b)) return "uma estação";
        if (DUROS.contains(m) && !Equipamento.cavaObsidiana(picareta)) return "obsidiana (precisa de picareta de diamante)";
        UUID d = Guardioes.dono(g);
        boolean pode = d == null ? plugin.territorios().em(b) == null : plugin.territorios().podeConstruir(d, b.getLocation());
        return pode ? null : "território de outra pessoa";
    }

    /** Tampa lava, água e o que pode cair (areia, cascalho) em volta do bloco, fora do túnel. */
    private static void selar(Block posto, BlockFace dir, Block b, int k) {
        for (BlockFace f : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block v = b.getRelative(f);
            if (dentro(posto, dir, v, k)) continue;
            Material m = v.getType();
            if (m == Material.LAVA || m == Material.WATER || (f == BlockFace.UP && m.hasGravity())) {
                v.setType(m == Material.LAVA ? Material.COBBLESTONE : Material.STONE);
            }
        }
        if (b.isLiquid()) b.setType(Material.AIR);
    }

    private void cavar(Mob g, Block b, ItemStack picareta, Block posto) {
        Collection<ItemStack> drops = b.getDrops(picareta);
        Guardioes.raio(g.getEyeLocation(), b.getLocation().add(0.5, 0.5, 0.5), Color.fromRGB(0xFFD54F));
        b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0, b.getBlockData());
        b.getWorld().playSound(b.getLocation(), b.getBlockSoundGroup().getBreakSound(), 0.8f, 0.9f);
        b.setType(Material.AIR);
        g.swingMainHand();
        guardar(g, posto, drops);
    }

    private void guardar(Mob g, Block posto, Collection<ItemStack> drops) {
        for (ItemStack s : Equipamento.guardar(g, posto.getLocation().add(0.5, 0, 0.5), 6, drops)) {
            if (!ENTULHO.contains(s.getType())) g.getWorld().dropItemNaturally(g.getLocation(), s);
        }
    }

    /** Minérios que ficaram à vista nas paredes, no teto e no chão da fatia (deixa pedra no lugar). */
    private void mineriosDaParede(Mob g, Block posto, BlockFace dir, int k, ItemStack picareta) {
        for (Block b : fatia(posto, dir, k)) {
            for (BlockFace f : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block v = b.getRelative(f);
                if (dentro(posto, dir, v, k + 1) || !Trabalhos.minerio(v.getType(), Guardioes.nivel(g)) || podeCavar(g, v, picareta) != null) continue;
                String n = v.getType().name();
                Collection<ItemStack> drops = v.getDrops(picareta);
                Guardioes.raio(g.getEyeLocation(), v.getLocation().add(0.5, 0.5, 0.5), Color.fromRGB(0x4FC3F7));
                v.setType(n.contains("DEEPSLATE") ? Material.DEEPSLATE
                        : n.startsWith("NETHER") || n.equals("ANCIENT_DEBRIS") ? Material.NETHERRACK : Material.STONE);
                guardar(g, posto, drops);
                if (Equipamento.gastar(g)) return;
            }
        }
    }

    /** Duas colunas de cerca nas laterais e uma viga de tábuas no teto. */
    private boolean sustentar(Mob g, Block posto, BlockFace dir, int k) {
        Location c = posto.getLocation().add(0.5, 0, 0.5);
        Material cerca = Equipamento.tirar(g, c, 6, m -> Tag.WOODEN_FENCES.isTagged(m), 4);
        if (cerca == null) return false;
        Material tabua = Equipamento.tirar(g, c, 6, m -> Tag.PLANKS.isTagged(m), 3);
        if (tabua == null) {
            Equipamento.guardar(g, c, 6, List.of(new ItemStack(cerca, 4)));
            return false;
        }
        for (int s : new int[]{-1, 1}) for (int dy = 0; dy <= 1; dy++) em(posto, dir, k, s, dy).setType(cerca);
        for (int s = -1; s <= 1; s++) em(posto, dir, k, s, 2).setType(tabua);
        Block meio = em(posto, dir, k, 0, 2);
        meio.getWorld().playSound(meio.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 0.9f);
        meio.getWorld().spawnParticle(Particle.BLOCK, meio.getLocation().add(0.5, 0, 0.5), 10, 0.8, 0.1, 0.3, 0, meio.getBlockData());
        return true;
    }

    /** Tocha na parede da direita. */
    private boolean tocha(Mob g, Block posto, BlockFace dir, int k) {
        Block onde = em(posto, dir, k, 1, 1);
        if (!onde.getType().isAir() || !onde.getRelative(lado(dir)).getType().isSolid()) return true;
        if (Equipamento.tirar(g, posto.getLocation().add(0.5, 0, 0.5), 6, m -> m == Material.TORCH, 1) == null) return false;
        onde.setType(Material.WALL_TORCH);
        if (onde.getBlockData() instanceof Directional t) {
            t.setFacing(lado(dir).getOppositeFace());
            onde.setBlockData(t);
        }
        return true;
    }

    private void avisarDono(Mob g, String texto) {
        UUID d = Guardioes.dono(g);
        var p = d == null ? null : plugin.getServer().getPlayer(d);
        if (p != null) p.sendMessage(net.kyori.adventure.text.Component.text(texto, Guardioes.COR));
    }
}
