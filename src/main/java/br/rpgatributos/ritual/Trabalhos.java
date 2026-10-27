package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.domador.Companheiros;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 2.86–2.87: o trabalho dos ajudantes. Todos precisam da ferramenta (dada com um clique), que
 * define a velocidade e gasta durabilidade. Golem de Fazenda (ara, planta, aduba, colhe e replanta),
 * Golem Lenhador (uma árvore de verdade por vez e planta floresta com as mudas), Golem Minerador
 * (o túnel, em {@link Escavacao}) e Pastor de Neve (tosquia, procria, ordenha e junta o que cai).
 * O que eles pegam vai para o baú mais perto do posto; sem baú, para a mochila (nível III+) ou o chão.
 */
final class Trabalhos {

    private static final Set<Material> PLANTAS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART);
    private static final Map<Material, Material> SEMENTES = Map.of(Material.WHEAT_SEEDS, Material.WHEAT, Material.CARROT, Material.CARROTS,
            Material.POTATO, Material.POTATOES, Material.BEETROOT_SEEDS, Material.BEETROOTS);
    private static final Set<Material> SOLO = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.PODZOL, Material.COARSE_DIRT,
            Material.ROOTED_DIRT, Material.MOSS_BLOCK, Material.MUD, Material.MYCELIUM, Material.PALE_MOSS_BLOCK);
    private static final Set<Material> MINERIOS = EnumSet.noneOf(Material.class);
    private static final Map<EntityType, Material> COMIDA = Map.of(EntityType.COW, Material.WHEAT, EntityType.SHEEP, Material.WHEAT,
            EntityType.GOAT, Material.WHEAT, EntityType.PIG, Material.CARROT, EntityType.RABBIT, Material.CARROT,
            EntityType.CHICKEN, Material.WHEAT_SEEDS);

    static {
        for (Material m : Material.values()) if (!m.isLegacy() && m.isBlock() && m.name().endsWith("_ORE")) MINERIOS.add(m);
    }

    static boolean minerio(Material m, int nivel) {
        return MINERIOS.contains(m) || (m == Material.ANCIENT_DEBRIS && nivel >= 4);
    }

    private final RPGAtributos plugin;
    private final Escavacao escavacao;
    /** Planta ou lugar que cada um está indo buscar. */
    private final Map<UUID, Location> alvo = new HashMap<>();
    /** Troncos que o lenhador ainda vai cortar (de cima para baixo). */
    private final Map<UUID, Deque<Block>> arvores = new HashMap<>();
    private final Map<UUID, Integer> espera = new HashMap<>();
    private final Map<UUID, Integer> rodadas = new HashMap<>();

    Trabalhos(RPGAtributos plugin, Guardioes guardioes) {
        this.plugin = plugin;
        this.escavacao = new Escavacao(plugin);
    }

    void esquecer(UUID id) {
        alvo.remove(id);
        arvores.remove(id);
        espera.remove(id);
        rodadas.remove(id);
        forja.remove(id);
        comIsca.remove(id);
        itemAlvo.remove(id);
        escavacao.esquecer(id);
    }

    /** Um passo de trabalho. @return o aviso para o nome (ou null se está tudo bem). */
    String trabalhar(Mob g, Guardioes.Tipo t, boolean asas, int nivel, int ciclo) {
        if (Equipamento.ferramentaDoTipo(t) != null && Equipamento.ferramenta(g) == null) {
            pousar(g, asas);
            return "⚠ sem " + Equipamento.ferramentaDoTipo(t);
        }
        return switch (t) {
            case FAZENDA -> fazenda(g, asas, nivel, ciclo);
            case LENHADOR -> lenhador(g, asas, nivel, ciclo);
            case MINERADOR -> escavacao.trabalhar(g, nivel, asas, ciclo);
            case PASTOR -> pastor(g, asas, nivel, ciclo);
            case PESCADOR -> pescador(g, asas, nivel, ciclo);
            case APICULTOR -> apicultor(g, asas, nivel, ciclo);
            case FERREIRO -> ferreiro(g, nivel, ciclo, true);
            case COLETOR -> coletor(g, asas, nivel, ciclo);
            default -> null;
        };
    }

    // =====================================================================
    //  Comum
    // =====================================================================

    private static void irPara(Mob g, Location l, boolean asas, int ciclo) {
        Guardioes.Tipo t = Guardioes.tipo(g);
        if (t != null && t.semIa()) Guardioes.passo(g, l.clone().add(0, 1, 0), 0.5);
        else if (asas) Guardioes.voarPara(g, l.clone().add(0, 0.6, 0), 0.4);
        else if (ciclo % 10 == 0) g.getPathfinder().moveTo(l, 0.9);
    }

    private static void pousar(Mob g, boolean asas) {
        if (asas && !Guardioes.tipo(g).semIa()) g.setGravity(true);
    }

    private static void voltarAoPosto(Mob g, Location posto, boolean asas, int ciclo) {
        if (g.getLocation().distanceSquared(posto) > 4 * 4) irPara(g, posto, asas, ciclo);
        else pousar(g, asas);
    }

    /** Pode mexer nesse bloco (território do dono, ou sem território)? */
    private boolean pode(Mob g, Block b) {
        UUID d = Guardioes.dono(g);
        return d == null ? plugin.territorios().em(b) == null : plugin.territorios().podeConstruir(d, b.getLocation());
    }

    /** Conta a cada ciclo; true quando passou o intervalo (mais curto com ferramenta melhor e nível maior). */
    private boolean pronto(Mob g, int base, int nivel) {
        return pronto(g, base, nivel, Equipamento.velocidade(Equipamento.ferramenta(g)));
    }

    private boolean pronto(Mob g, int base, int nivel, double velocidadeFerramenta) {
        double vel = velocidadeFerramenta * (1 + 0.1 * (nivel - 1));
        int intervalo = (int) Math.max(2, Math.round(base / Math.max(0.3, vel)));
        int n = espera.merge(g.getUniqueId(), 1, Integer::sum);
        if (n < intervalo) return false;
        espera.put(g.getUniqueId(), 0);
        return true;
    }

    /** Gasta a ferramenta; se quebrar, avisa o dono. */
    private boolean gastar(Mob g, Guardioes.Tipo t) {
        if (!Equipamento.gastar(g)) return false;
        UUID d = Guardioes.dono(g);
        var p = d == null ? null : plugin.getServer().getPlayer(d);
        if (p != null) p.sendMessage(net.kyori.adventure.text.Component.text("⚒ A " + Equipamento.ferramentaDoTipo(t) + " do "
                + t.nome + " quebrou. Dê outra para ele continuar.", Guardioes.COR));
        return true;
    }

    private void guardar(Mob g, Location posto, double raio, Collection<ItemStack> itens) {
        for (ItemStack s : Equipamento.guardar(g, posto, raio + 2, itens)) g.getWorld().dropItemNaturally(g.getLocation(), s);
    }

    /** Junta do chão (no raio do posto) os itens que servem para esse ajudante. */
    private void juntar(Mob g, Location posto, double raio, java.util.function.Predicate<Material> serve) {
        List<ItemStack> pegou = new ArrayList<>();
        for (Entity e : g.getWorld().getNearbyEntities(posto, raio, 4, raio)) {
            if (e instanceof Item i && i.isValid() && i.getPickupDelay() < 20 && serve.test(i.getItemStack().getType())) {
                pegou.add(i.getItemStack());
                i.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, i.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                i.remove();
            }
        }
        if (!pegou.isEmpty()) guardar(g, posto, raio, pegou);
    }

    // =====================================================================
    //  Golem de Fazenda: colhe, planta, ara e aduba
    // =====================================================================

    private String fazenda(Mob g, boolean asas, int nivel, int ciclo) {
        Location posto = Guardioes.posto(g);
        ItemStack enxada = Equipamento.ferramenta(g);
        // A enxada melhor alcança mais longe.
        double raio = Guardioes.alcance(Guardioes.Tipo.FAZENDA, nivel, asas) + (int) Math.round(Equipamento.velocidade(enxada) * 2);
        Location destino = alvo.get(g.getUniqueId());
        if (destino != null && tarefa(destino.getBlock(), posto) == null) destino = null;
        if (destino == null) {
            alvo.remove(g.getUniqueId());
            if (ciclo % 10 != 0) return null;
            destino = procurarTarefa(g, posto, (int) raio);
            if (destino == null) { voltarAoPosto(g, posto, asas, ciclo); return null; }
            alvo.put(g.getUniqueId(), destino);
        }
        Location centro = destino.clone().add(0.5, 0, 0.5);
        if (g.getLocation().distanceSquared(centro) > 2.2 * 2.2) { irPara(g, centro, asas, ciclo); return null; }
        pousar(g, asas);
        if (!pronto(g, 6, nivel)) return null;
        Block b = destino.getBlock();
        String tarefa = tarefa(b, posto);
        alvo.remove(g.getUniqueId());
        if (tarefa == null || !pode(g, b)) return null;
        String aviso = null;
        switch (tarefa) {
            case "colher" -> {
                colher(g, b, posto, raio);
                if (Math.random() < 0.2) gastar(g, Guardioes.Tipo.FAZENDA);
            }
            case "plantar" -> {
                Material semente = Equipamento.tirar(g, posto, raio + 2, SEMENTES::containsKey, 1);
                if (semente == null) aviso = "⚠ sem sementes no baú";
                else {
                    b.getRelative(BlockFace.UP).setType(SEMENTES.get(semente));
                    b.getWorld().playSound(b.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
                }
            }
            case "arar" -> {
                b.setType(Material.FARMLAND);
                b.getWorld().playSound(b.getLocation(), Sound.ITEM_HOE_TILL, 1f, 1f);
                gastar(g, Guardioes.Tipo.FAZENDA);
            }
            case "adubar" -> {
                if (Equipamento.tirar(g, posto, raio + 2, m -> m == Material.BONE_MEAL, 1) != null) {
                    b.applyBoneMeal(BlockFace.UP);
                }
            }
            default -> { }
        }
        g.swingMainHand();
        return aviso;
    }

    /** O que fazer nesse bloco: colher (planta madura), plantar (terra arada vazia), arar ou adubar; null = nada. */
    private String tarefa(Block b, Location posto) {
        if (madura(b)) return "colher";
        if (b.getType() == Material.FARMLAND && b.getRelative(BlockFace.UP).getType().isAir()) return "plantar";
        if ((b.getType() == Material.GRASS_BLOCK || b.getType() == Material.DIRT) && b.getRelative(BlockFace.UP).getType().isAir()
                && vizinhoArado(b) && aguaPerto(b)) return "arar";
        if (PLANTAS.contains(b.getType()) && b.getType() != Material.NETHER_WART && !madura(b)) return "adubar";
        return null;
    }

    private Location procurarTarefa(Mob g, Location posto, int r) {
        Block c = posto.getBlock();
        boolean temSemente = Equipamento.contar(g, posto, r + 2, SEMENTES::containsKey) > 0;
        boolean temAdubo = Equipamento.contar(g, posto, r + 2, m -> m == Material.BONE_MEAL) > 0;
        Block melhor = null;
        int prioridade = 9;
        double d = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -2; y <= 2; y++) {
            Block b = c.getRelative(x, y, z);
            String t = tarefa(b, posto);
            if (t == null) continue;
            int p = switch (t) {
                case "colher" -> 0;
                case "plantar" -> temSemente ? 1 : 9;
                case "arar" -> temSemente ? 2 : 9;
                default -> temAdubo ? 3 : 9;
            };
            if (p == 9) continue;
            double dd = x * x + z * z + y * y;
            if (p < prioridade || (p == prioridade && dd < d)) { prioridade = p; d = dd; melhor = b; }
        }
        return melhor == null ? null : melhor.getLocation();
    }

    /** Só ara terra ao lado de uma que já está arada (a plantação cresce a partir da sua). */
    private static boolean vizinhoArado(Block b) {
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (b.getRelative(f).getType() == Material.FARMLAND) return true;
        }
        return false;
    }

    /** Água a até 4 blocos (senão a terra arada seca). */
    private static boolean aguaPerto(Block b) {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = 0; y <= 1; y++) {
            if (b.getRelative(x, y, z).getType() == Material.WATER) return true;
        }
        return false;
    }

    private static boolean madura(Block b) {
        return PLANTAS.contains(b.getType()) && b.getBlockData() instanceof Ageable a && a.getAge() >= a.getMaximumAge();
    }

    private void colher(Mob g, Block b, Location posto, double raio) {
        List<ItemStack> drops = new ArrayList<>(b.getDrops(Equipamento.ferramenta(g)));
        Material semente = switch (b.getType()) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> Material.NETHER_WART;
        };
        // Replanta usando uma semente da própria colheita.
        for (ItemStack s : drops) {
            if (s.getType() == semente) { s.setAmount(s.getAmount() - 1); break; }
        }
        Ageable a = (Ageable) b.getBlockData();
        a.setAge(0);
        b.setBlockData(a);
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, 0);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CROP_BREAK, 1f, 1f);
        guardar(g, posto, raio, drops);
    }

    // =====================================================================
    //  Golem Lenhador: corta árvores naturais e planta floresta
    // =====================================================================

    private String lenhador(Mob g, boolean asas, int nivel, int ciclo) {
        Location posto = Guardioes.posto(g);
        double raio = Guardioes.alcance(Guardioes.Tipo.LENHADOR, nivel, asas);
        if (ciclo % 25 == 0) juntar(g, posto, raio, m -> Tag.SAPLINGS.isTagged(m) || m == Material.STICK || m == Material.APPLE
                || m == Material.MANGROVE_PROPAGULE || Tag.LOGS.isTagged(m));
        Deque<Block> fila = arvores.get(g.getUniqueId());
        if (fila == null || fila.isEmpty()) {
            arvores.remove(g.getUniqueId());
            if (ciclo % 10 != 0) return null;
            fila = procurarArvore(g, posto, (int) raio);
            if (fila == null) return plantarFloresta(g, posto, (int) raio, asas, ciclo);
            arvores.put(g.getUniqueId(), fila);
        }
        Block base = fila.peekLast();
        Location pe = base.getLocation().add(0.5, 0, 0.5);
        double dx = g.getLocation().getX() - pe.getX(), dz = g.getLocation().getZ() - pe.getZ();
        if (dx * dx + dz * dz > 3 * 3) { irPara(g, pe, asas, ciclo); return null; }
        pousar(g, asas);
        if (!pronto(g, 10, nivel)) return null;
        Block tronco = fila.pollFirst();
        if (Tag.LOGS.isTagged(tronco.getType()) && pode(g, tronco)) {
            Material tipo = tronco.getType();
            Collection<ItemStack> drops = tronco.getDrops(Equipamento.ferramenta(g));
            Guardioes.raio(g.getEyeLocation(), tronco.getLocation().add(0.5, 0.5, 0.5), Color.fromRGB(0xA0522D));
            tronco.getWorld().spawnParticle(Particle.BLOCK, tronco.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0, tronco.getBlockData());
            tronco.getWorld().playSound(tronco.getLocation(), Sound.BLOCK_WOOD_BREAK, 1f, 0.9f);
            tronco.setType(Material.AIR);
            g.swingMainHand();
            guardar(g, posto, raio, drops);
            if (fila.isEmpty()) replantar(tronco, tipo);
            if (gastar(g, Guardioes.Tipo.LENHADOR)) arvores.remove(g.getUniqueId());
        }
        if (fila.isEmpty()) arvores.remove(g.getUniqueId());
        return null;
    }

    /** Sem árvore para cortar: planta uma muda do baú em grade (de 4 em 4 blocos) perto do posto. */
    private String plantarFloresta(Mob g, Location posto, int r, boolean asas, int ciclo) {
        int n = rodadas.merge(g.getUniqueId(), 1, Integer::sum);
        if (n % 5 != 0) { voltarAoPosto(g, posto, asas, ciclo); return null; }
        if (Equipamento.contar(g, posto, r + 2, m -> Tag.SAPLINGS.isTagged(m)) <= 0) {
            voltarAoPosto(g, posto, asas, ciclo);
            return null;
        }
        Block c = posto.getBlock();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (Math.floorMod(x, 4) != 0 || Math.floorMod(z, 4) != 0 || (x == 0 && z == 0)) continue;
            for (int y = -2; y <= 2; y++) {
                Block chao = c.getRelative(x, y - 1, z), lugar = c.getRelative(x, y, z);
                if (!SOLO.contains(chao.getType()) || !lugar.getType().isAir() || !lugar.getRelative(BlockFace.UP).getType().isAir()) continue;
                if (!pode(g, lugar) || temArvorePerto(lugar)) continue;
                Material muda = Equipamento.tirar(g, posto, r + 2, m -> Tag.SAPLINGS.isTagged(m), 1);
                if (muda == null) return null;
                lugar.setType(muda);
                lugar.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, lugar.getLocation().add(0.5, 0.3, 0.5), 8, 0.3, 0.2, 0.3, 0);
                lugar.getWorld().playSound(lugar.getLocation(), Sound.BLOCK_GRASS_PLACE, 1f, 1f);
                return null;
            }
        }
        return null;
    }

    private static boolean temArvorePerto(Block b) {
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y <= 2; y++) {
            Material m = b.getRelative(x, y, z).getType();
            if (Tag.LOGS.isTagged(m) || Tag.SAPLINGS.isTagged(m)) return true;
        }
        return false;
    }

    /** Uma árvore de verdade (troncos sobre terra, com folhas naturais) perto do posto: os troncos de cima para baixo. */
    private Deque<Block> procurarArvore(Mob g, Location posto, int r) {
        Block c = posto.getBlock();
        Block melhor = null;
        double d = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -2; y <= 4; y++) {
            Block b = c.getRelative(x, y, z);
            if (!Tag.LOGS.isTagged(b.getType()) || !SOLO.contains(b.getRelative(BlockFace.DOWN).getType())) continue;
            double dd = x * x + z * z;
            if (dd < d) { d = dd; melhor = b; }
        }
        if (melhor == null) return null;
        List<Block> troncos = arvore(melhor);
        if (troncos == null) return null;
        for (Block b : troncos) if (!pode(g, b)) return null;
        troncos.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        return new ArrayDeque<>(troncos);
    }

    /** Os troncos ligados à base, ou null se não parece uma árvore natural (construção de jogador). */
    private static List<Block> arvore(Block base) {
        List<Block> troncos = new ArrayList<>();
        Set<Block> vistos = new HashSet<>();
        Deque<Block> fila = new ArrayDeque<>();
        fila.add(base);
        vistos.add(base);
        int folhas = 0;
        while (!fila.isEmpty() && troncos.size() < 128) {
            Block b = fila.poll();
            troncos.add(b);
            for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP}) {
                Block v = b.getRelative(f);
                if (v.getBlockData() instanceof Leaves l && !l.isPersistent()) folhas++;
            }
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                Block v = b.getRelative(x, y, z);
                if (vistos.contains(v) || !Tag.LOGS.isTagged(v.getType())) continue;
                if (Math.abs(v.getX() - base.getX()) > 7 || Math.abs(v.getZ() - base.getZ()) > 7 || v.getY() - base.getY() > 32) continue;
                vistos.add(v);
                fila.add(v);
            }
        }
        return folhas >= 4 ? troncos : null;
    }

    private static void replantar(Block base, Material tronco) {
        if (!base.getType().isAir() || !SOLO.contains(base.getRelative(BlockFace.DOWN).getType())) return;
        String n = tronco.name().replace("STRIPPED_", "").replace("_LOG", "").replace("_WOOD", "");
        Material muda = n.equals("MANGROVE") ? Material.MANGROVE_PROPAGULE : Material.matchMaterial(n + "_SAPLING");
        if (muda == null) muda = Material.OAK_SAPLING;
        base.setType(muda);
        base.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, base.getLocation().add(0.5, 0.3, 0.5), 8, 0.3, 0.2, 0.3, 0);
    }

    // =====================================================================
    //  Pastor de Neve: tosquia, procria, ordenha, junta
    // =====================================================================

    private static boolean coletavel(Material m) {
        String n = m.name();
        return n.endsWith("_WOOL") || (n.endsWith("EGG") && !n.endsWith("SPAWN_EGG")) || n.endsWith("_SCUTE")
                || m == Material.FEATHER || m == Material.LEATHER || m == Material.RABBIT_HIDE;
    }

    private static boolean doCercado(Entity e) {
        if (!(e instanceof Animals a) || a.isLeashed() || Companheiros.eh(a) || Guardioes.tipo(a) != null || a.customName() != null) return false;
        return !(a instanceof Tameable t && t.isTamed());
    }

    private String pastor(Mob g, boolean asas, int nivel, int ciclo) {
        if (ciclo % 10 != 0) return null;
        Location posto = Guardioes.posto(g);
        double raio = Guardioes.alcance(Guardioes.Tipo.PASTOR, nivel, asas);
        int rodada = rodadas.merge(g.getUniqueId(), 1, Integer::sum);
        String aviso = null;
        // 1. Junta o que caiu no chão (ovos, lã, penas, couro...).
        juntar(g, posto, raio, Trabalhos::coletavel);
        // 2. Traz de volta os animais que fugiram do cercado.
        for (Entity e : g.getWorld().getNearbyEntities(posto, raio + 10, 6, raio + 10)) {
            if (doCercado(e) && e.getLocation().distanceSquared(posto) > raio * raio) ((Animals) e).getPathfinder().moveTo(posto, 1.0);
        }
        // 3. Procria, a cada 30 s, com a comida do baú (até um limite de animais por tipo).
        if (rodada % 30 == 0) aviso = procriar(g, posto, raio, nivel);
        // 4. Ordenha, a cada minuto, com um balde vazio do baú.
        if (rodada % 60 == 0) ordenhar(g, posto, raio);
        // 5. Tosquia a ovelha mais perto.
        Sheep ovelha = null;
        double d = raio * raio;
        for (Entity e : g.getWorld().getNearbyEntities(posto, raio, 4, raio)) {
            if (e instanceof Sheep s && s.readyToBeSheared() && doCercado(s)) {
                double dd = s.getLocation().distanceSquared(g.getLocation());
                if (dd < d) { d = dd; ovelha = s; }
            }
        }
        if (ovelha == null) { voltarAoPosto(g, posto, asas, 0); return aviso; }
        if (d < 2.5 * 2.5) {
            ovelha.shear();
            g.getWorld().playSound(ovelha.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHEAR, 1f, 1f);
            gastar(g, Guardioes.Tipo.PASTOR);
            pousar(g, asas);
        } else {
            irPara(g, ovelha.getLocation(), asas, 0);
        }
        return aviso;
    }

    private String procriar(Mob g, Location posto, double raio, int nivel) {
        Map<EntityType, List<Animals>> porTipo = new HashMap<>();
        for (Entity e : g.getWorld().getNearbyEntities(posto, raio, 4, raio)) {
            if (doCercado(e) && COMIDA.containsKey(e.getType())) porTipo.computeIfAbsent(e.getType(), k -> new ArrayList<>()).add((Animals) e);
        }
        int limite = 8 + 2 * nivel;
        String aviso = null;
        for (Map.Entry<EntityType, List<Animals>> en : porTipo.entrySet()) {
            List<Animals> l = en.getValue();
            if (l.size() >= limite) continue;
            List<Animals> prontos = new ArrayList<>();
            for (Animals a : l) if (a.isAdult() && a.canBreed() && !a.isLoveMode()) prontos.add(a);
            if (prontos.size() < 2) continue;
            Material comida = COMIDA.get(en.getKey());
            if (Equipamento.tirar(g, posto, raio + 2, m -> m == comida, 2) == null) {
                aviso = "⚠ falta " + ReceitaRitual.bonito(comida).toLowerCase() + " no baú";
                continue;
            }
            for (int k = 0; k < 2; k++) {
                Animals a = prontos.get(k);
                a.setLoveModeTicks(600);
                a.getWorld().spawnParticle(Particle.HEART, a.getLocation().add(0, a.getHeight(), 0), 3, 0.3, 0.2, 0.3, 0);
            }
        }
        return aviso;
    }

    private void ordenhar(Mob g, Location posto, double raio) {
        for (Entity e : g.getWorld().getNearbyEntities(posto, raio, 4, raio)) {
            if (!(e instanceof Cow v) || !v.isAdult() || !doCercado(v)) continue;
            if (Equipamento.tirar(g, posto, raio + 2, m -> m == Material.BUCKET, 1) == null) return;
            guardar(g, posto, raio, List.of(new ItemStack(Material.MILK_BUCKET)));
            v.getWorld().playSound(v.getLocation(), Sound.ENTITY_COW_MILK, 1f, 1f);
            return;
        }
    }

    // =====================================================================
    //  2.89 · Golem Pescador: pesca do posto, na água mais perto
    // =====================================================================

    private static final Set<Material> ISCAS = EnumSet.of(Material.ROTTEN_FLESH, Material.SPIDER_EYE, Material.WHEAT_SEEDS);
    /** Se a última pesca usou isca (a próxima espera é mais curta). */
    private final Set<UUID> comIsca = new HashSet<>();

    private String pescador(Mob g, boolean asas, int nivel, int ciclo) {
        UUID id = g.getUniqueId();
        Location posto = Guardioes.posto(g);
        double raio = Guardioes.alcance(Guardioes.Tipo.PESCADOR, nivel, asas);
        voltarAoPosto(g, posto, asas, ciclo);
        Location agua = alvo.get(id);
        if (agua != null && !aguaAberta(agua.getBlock())) agua = null;
        if (agua == null) {
            alvo.remove(id);
            if (ciclo % 10 != 0) return null;
            agua = procurarAgua(posto, (int) raio);
            if (agua == null) return "⚠ sem água perto do posto";
            alvo.put(id, agua);
        }
        Location boia = agua.clone().add(0.5, 0.9, 0.5);
        if (ciclo % 10 == 0) g.getWorld().spawnParticle(Particle.FISHING, boia, 2, 0.1, 0, 0.1, 0);
        if (ciclo % 30 == 0) Guardioes.raio(g.getEyeLocation(), boia, Color.fromRGB(0xEEEEEE));
        ItemStack vara = Equipamento.ferramenta(g);
        int isca = comIsca.contains(id) ? 1 : 0;
        // 20 s por peixe no nível I; Isca (encantamento) tira 3 s por nível e a isca do baú, 25%.
        int base = (int) Math.round((200 - 30 * vara.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LURE)) * (isca == 1 ? 0.75 : 1));
        if (!pronto(g, Math.max(60, base), nivel, 1.0)) return null;
        // Pegou: a isca da próxima sai do baú, se tiver.
        boolean usou = Equipamento.tirar(g, posto, raio + 2, ISCAS::contains, 1) != null;
        if (usou) comIsca.add(id); else comIsca.remove(id);
        ItemStack peixe = pescar(vara.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA), isca == 1);
        World w = g.getWorld();
        w.spawnParticle(Particle.SPLASH, boia, 20, 0.3, 0.1, 0.3, 0.1);
        w.spawnParticle(Particle.BUBBLE, boia.clone().add(0, -0.4, 0), 10, 0.2, 0.1, 0.2, 0.05);
        w.playSound(boia, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.8f, 1f);
        w.playSound(g.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 1f);
        Guardioes.raio(boia, g.getEyeLocation(), Color.fromRGB(0x4FC3F7));
        g.swingMainHand();
        guardar(g, posto, raio, List.of(peixe));
        w.spawn(g.getLocation().add(0, 0.5, 0), org.bukkit.entity.ExperienceOrb.class,
                o -> o.setExperience(1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(3)));
        gastar(g, Guardioes.Tipo.PESCADOR);
        return null;
    }

    private static boolean aguaAberta(Block b) {
        return b.getType() == Material.WATER && b.getRelative(BlockFace.UP).getType().isAir();
    }

    private static Location procurarAgua(Location posto, int r) {
        Block c = posto.getBlock(), melhor = null;
        double d = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -4; y <= 1; y++) {
            Block b = c.getRelative(x, y, z);
            if (!aguaAberta(b)) continue;
            double dd = x * x + z * z + y * y;
            if (dd < d) { d = dd; melhor = b; }
        }
        return melhor == null ? null : melhor.getLocation();
    }

    /** A pesca: peixe, lixo ou tesouro (Sorte do Mar e isca dão mais tesouro e menos lixo). */
    static ItemStack pescar(int sorte, boolean isca) {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        double tesouro = 0.05 + 0.02 * sorte + (isca ? 0.02 : 0), lixo = Math.max(0.02, 0.10 - 0.025 * sorte);
        double x = r.nextDouble();
        if (x < tesouro) {
            return switch (r.nextInt(6)) {
                case 0 -> new ItemStack(Material.NAME_TAG);
                case 1 -> new ItemStack(Material.SADDLE);
                case 2 -> new ItemStack(Material.NAUTILUS_SHELL);
                case 3 -> org.bukkit.Bukkit.getItemFactory().enchantItem(new ItemStack(Material.BOW), 30, true);
                case 4 -> org.bukkit.Bukkit.getItemFactory().enchantItem(new ItemStack(Material.FISHING_ROD), 30, true);
                default -> org.bukkit.Bukkit.getItemFactory().enchantItem(new ItemStack(Material.BOOK), 30, true);
            };
        }
        if (x < tesouro + lixo) {
            Material[] l = {Material.LILY_PAD, Material.BOWL, Material.LEATHER, Material.STICK, Material.STRING, Material.ROTTEN_FLESH,
                    Material.BONE, Material.INK_SAC, Material.TRIPWIRE_HOOK, Material.LEATHER_BOOTS};
            return new ItemStack(l[r.nextInt(l.length)]);
        }
        double y = r.nextDouble();
        return new ItemStack(y < 0.6 ? Material.COD : y < 0.85 ? Material.SALMON : y < 0.98 ? Material.PUFFERFISH : Material.TROPICAL_FISH);
    }

    // =====================================================================
    //  2.89 · Abelha Apicultora: tira o mel das colmeias cheias com fumaça
    // =====================================================================

    private String apicultor(Mob g, boolean asas, int nivel, int ciclo) {
        UUID id = g.getUniqueId();
        Location posto = Guardioes.posto(g);
        double raio = Guardioes.alcance(Guardioes.Tipo.APICULTOR, nivel, asas);
        Location destino = alvo.get(id);
        if (destino != null && !colmeiaCheia(destino.getBlock())) destino = null;
        if (destino == null) {
            alvo.remove(id);
            if (ciclo % 10 != 0) return null;
            // A partir do nível III, poliniza: uma colmeia com flores perto enche um pouco a cada minuto.
            if (nivel >= 3 && rodadas.merge(id, 1, Integer::sum) % 60 == 0) polinizar(posto, (int) raio);
            destino = procurarColmeia(g, posto, (int) raio);
            if (destino == null) {
                Location pouso = posto.clone().add(0, 1.5, 0);
                if (g.getLocation().distanceSquared(pouso) > 1) Guardioes.passo(g, pouso, 0.45);
                return null;
            }
            alvo.put(id, destino);
        }
        Block colmeia = destino.getBlock();
        Location frente = ladoLivre(colmeia);
        if (g.getLocation().distanceSquared(frente) > 1.2 * 1.2) { Guardioes.passo(g, frente, 0.45); return null; }
        Location c = colmeia.getLocation().add(0.5, 0.5, 0.5);
        g.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 0.6, 0), 1, 0.2, 0.1, 0.2, 0.01);
        if (!pronto(g, 10, nivel)) return null;
        alvo.remove(id);
        if (!(colmeia.getBlockData() instanceof org.bukkit.block.data.type.Beehive dados) || !pode(g, colmeia)) return null;
        List<ItemStack> mel = new ArrayList<>();
        if (Equipamento.tirar(g, posto, raio + 2, m -> m == Material.GLASS_BOTTLE, 1) != null) {
            mel.add(new ItemStack(Material.HONEY_BOTTLE));
            g.getWorld().playSound(c, Sound.ITEM_BOTTLE_FILL, 1f, 1f);
        } else {
            mel.add(new ItemStack(Material.HONEYCOMB, 3));
            g.getWorld().playSound(c, Sound.BLOCK_BEEHIVE_SHEAR, 1f, 1f);
            gastar(g, Guardioes.Tipo.APICULTOR);
        }
        dados.setHoneyLevel(0);
        colmeia.setBlockData(dados);
        // A fumaça acalma: as abelhas de dentro não saem bravas (o plugin mexe no bloco direto).
        g.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.clone().add(0, 0.6, 0), 8, 0.3, 0.2, 0.3, 0.02);
        g.getWorld().spawnParticle(Particle.DRIPPING_HONEY, c, 10, 0.4, 0.3, 0.4, 0);
        guardar(g, posto, raio, mel);
        return null;
    }

    private static boolean colmeiaCheia(Block b) {
        return (b.getType() == Material.BEEHIVE || b.getType() == Material.BEE_NEST)
                && b.getBlockData() instanceof org.bukkit.block.data.type.Beehive h && h.getHoneyLevel() >= h.getMaximumHoneyLevel();
    }

    private Location procurarColmeia(Mob g, Location posto, int r) {
        Block c = posto.getBlock(), melhor = null;
        double d = Double.MAX_VALUE;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -4; y <= 6; y++) {
            Block b = c.getRelative(x, y, z);
            if (!colmeiaCheia(b) || !pode(g, b)) continue;
            double dd = x * x + z * z + y * y;
            if (dd < d) { d = dd; melhor = b; }
        }
        return melhor == null ? null : melhor.getLocation();
    }

    /** Um lugar livre encostado na colmeia (para a abelha parar ali). */
    private static Location ladoLivre(Block b) {
        for (BlockFace f : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN}) {
            Block v = b.getRelative(f);
            if (v.isPassable()) return v.getLocation().add(0.5, 0.2, 0.5);
        }
        return b.getLocation().add(0.5, 1.2, 0.5);
    }

    private void polinizar(Location posto, int r) {
        Block c = posto.getBlock();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -4; y <= 6; y++) {
            Block b = c.getRelative(x, y, z);
            if ((b.getType() != Material.BEEHIVE && b.getType() != Material.BEE_NEST)
                    || !(b.getBlockData() instanceof org.bukkit.block.data.type.Beehive h) || h.getHoneyLevel() >= h.getMaximumHoneyLevel()) continue;
            if (!florPerto(b)) continue;
            h.setHoneyLevel(h.getHoneyLevel() + 1);
            b.setBlockData(h);
            b.getWorld().spawnParticle(Particle.WAX_ON, b.getLocation().add(0.5, 0.8, 0.5), 8, 0.4, 0.3, 0.4, 0);
            return;
        }
    }

    private static boolean florPerto(Block b) {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = -2; y <= 2; y++) {
            if (Tag.FLOWERS.isTagged(b.getRelative(x, y, z).getType())) return true;
        }
        return false;
    }

    // =====================================================================
    //  2.89 · Golem Ferreiro: conserta o que está no baú, na bigorna
    // =====================================================================

    /** O material que conserta cada item (e o nome dele para o aviso). */
    private record Conserto(java.util.function.Predicate<Material> material, String nome) { }

    private static Conserto conserto(Material m) {
        String n = m.name();
        if (n.startsWith("WOODEN_") || m == Material.SHIELD) return new Conserto(Tag.PLANKS::isTagged, "tábuas");
        if (n.startsWith("STONE_")) return new Conserto(x -> x == Material.COBBLESTONE || x == Material.COBBLED_DEEPSLATE || x == Material.BLACKSTONE, "pedregulho");
        if (n.startsWith("LEATHER_")) return new Conserto(x -> x == Material.LEATHER, "couro");
        if (n.startsWith("GOLDEN_")) return new Conserto(x -> x == Material.GOLD_INGOT, "lingote de ouro");
        if (n.startsWith("DIAMOND_")) return new Conserto(x -> x == Material.DIAMOND, "diamante");
        if (n.startsWith("NETHERITE_")) return new Conserto(x -> x == Material.NETHERITE_SCRAP, "fragmento de netherite");
        if (n.startsWith("COPPER_")) return new Conserto(x -> x == Material.COPPER_INGOT, "lingote de cobre");
        if (m == Material.TURTLE_HELMET) return new Conserto(x -> x == Material.TURTLE_SCUTE, "escama de tartaruga");
        if (m == Material.ELYTRA) return new Conserto(x -> x == Material.PHANTOM_MEMBRANE, "membrana de phantom");
        if (m == Material.BOW || m == Material.CROSSBOW || m == Material.FISHING_ROD || n.endsWith("_ON_A_STICK")) {
            return new Conserto(x -> x == Material.STRING, "linha");
        }
        if (m == Material.TRIDENT) return new Conserto(x -> x == Material.PRISMARINE_SHARD, "fragmento de prismarinho");
        if (m == Material.MACE) return new Conserto(x -> x == Material.BREEZE_ROD, "vara de breeze");
        if (m == Material.BRUSH) return new Conserto(x -> x == Material.FEATHER, "pena");
        if (m == Material.WOLF_ARMOR) return new Conserto(x -> x == Material.ARMADILLO_SCUTE, "escama de tatu");
        return new Conserto(x -> x == Material.IRON_INGOT, "lingote de ferro");
    }

    /** Gasto e consertável pelo Ferreiro (as peças que só a bigorna conserta com o próprio mineral ficam de fora). */
    private static boolean gasto(ItemStack i) {
        if (i == null || i.isEmpty() || i.getType().getMaxDurability() <= 0) return false;
        if (i.isDataOverridden(io.papermc.paper.datacomponent.DataComponentTypes.REPAIRABLE)) return false;
        return i.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable d && d.getDamage() > 0 && !d.isUnbreakable();
    }

    private static ItemStack consertado(ItemStack i, int nivel) {
        ItemStack r = i.clone();
        int max = r.getType().getMaxDurability();
        r.editMeta(org.bukkit.inventory.meta.Damageable.class, d -> d.setDamage(Math.max(0, d.getDamage() - max / 4 - max * (nivel - 1) / 40)));
        return r;
    }

    private final Map<UUID, Integer> forja = new HashMap<>();

    /**
     * Um passo do Ferreiro (também como função de apoio de um guardião fundido: {@code principal} false,
     * aí ele não anda). @return o aviso para o nome, ou null.
     */
    String ferreiro(Mob g, int nivel, int ciclo, boolean principal) {
        Location posto = Guardioes.posto(g);
        if (principal) voltarAoPosto(g, posto, Guardioes.temAsas(g), ciclo);
        int n = forja.merge(g.getUniqueId(), 1, Integer::sum);
        if (n < Math.max(6, Math.round(30 / (1 + 0.15 * (nivel - 1))))) return null;
        forja.put(g.getUniqueId(), 0);
        Block bigorna = bigornaPerto(posto);
        if (bigorna == null) return "⚠ sem bigorna perto";
        double raio = 10;
        org.bukkit.block.Container bau = Equipamento.bauPerto(posto, raio);
        if (bau == null) return "⚠ sem baú perto";
        String falta = null;
        // 1. Os itens gastos do baú.
        org.bukkit.inventory.Inventory inv = bau.getInventory();
        for (int k = 0; k < inv.getSize(); k++) {
            ItemStack i = inv.getItem(k);
            if (!gasto(i)) continue;
            Conserto c = conserto(i.getType());
            if (Equipamento.tirar(g, posto, raio, c.material(), 1) == null) { falta = c.nome(); continue; }
            inv.setItem(k, consertado(inv.getItem(k), nivel));
            martelar(g, bigorna, nivel);
            return null;
        }
        // 2. A partir do nível III, as ferramentas dos outros guardiões do mesmo dono.
        if (nivel >= 3) {
            UUID dono = Guardioes.dono(g);
            for (Entity e : g.getWorld().getNearbyEntities(posto, 16, 8, 16)) {
                if (e == g || Guardioes.tipo(e) == null || !java.util.Objects.equals(Guardioes.dono(e), dono)) continue;
                ItemStack f = Equipamento.ferramenta(e);
                if (!gasto(f)) continue;
                Conserto c = conserto(f.getType());
                if (Equipamento.tirar(g, posto, raio, c.material(), 1) == null) { falta = c.nome(); continue; }
                Equipamento.definirFerramenta(e, consertado(f, nivel));
                Guardioes.raio(bigorna.getLocation().add(0.5, 1, 0.5), e.getLocation().add(0, 1, 0), Color.fromRGB(0xFFB74D));
                martelar(g, bigorna, nivel);
                return null;
            }
        }
        return falta == null ? null : "⚠ falta " + falta + " no baú";
    }

    private static Block bigornaPerto(Location posto) {
        Block c = posto.getBlock();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -2; y <= 2; y++) {
            Block b = c.getRelative(x, y, z);
            if (b.getType() == Material.ANVIL || b.getType() == Material.CHIPPED_ANVIL || b.getType() == Material.DAMAGED_ANVIL) return b;
        }
        return null;
    }

    /** As marteladas: faíscas e som; a bigorna gasta como a do jogo (menos nos níveis altos). */
    private static void martelar(Mob g, Block bigorna, int nivel) {
        Location l = bigorna.getLocation().add(0.5, 1, 0.5);
        World w = bigorna.getWorld();
        g.swingMainHand();
        w.spawnParticle(Particle.LAVA, l, 4, 0.2, 0.1, 0.2, 0);
        w.spawnParticle(Particle.CRIT, l, 15, 0.3, 0.2, 0.3, 0.2);
        w.playSound(l, Sound.BLOCK_ANVIL_USE, 0.8f, 1f);
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= 0.12 - 0.02 * (nivel - 1)) return;
        Material proximo = switch (bigorna.getType()) {
            case ANVIL -> Material.CHIPPED_ANVIL;
            case CHIPPED_ANVIL -> Material.DAMAGED_ANVIL;
            default -> Material.AIR;
        };
        if (proximo == Material.AIR) {
            bigorna.setType(Material.AIR);
            w.playSound(l, Sound.BLOCK_ANVIL_DESTROY, 1f, 1f);
            return;
        }
        org.bukkit.block.data.BlockData antes = bigorna.getBlockData();
        org.bukkit.block.data.BlockData depois = proximo.createBlockData();
        if (antes instanceof org.bukkit.block.data.Directional a && depois instanceof org.bukkit.block.data.Directional d) d.setFacing(a.getFacing());
        bigorna.setBlockData(depois);
    }

    // =====================================================================
    //  2.89 · Espírito Coletor: junta o que cai e guarda no baú certo
    // =====================================================================

    private static final org.bukkit.NamespacedKey K_IGNORAR = new org.bukkit.NamespacedKey("rpgatributos", "coletor_ignora");
    private final Map<UUID, UUID> itemAlvo = new HashMap<>();

    private String coletor(Mob g, boolean asas, int nivel, int ciclo) {
        UUID id = g.getUniqueId();
        Location posto = Guardioes.posto(g);
        double raio = Guardioes.alcance(Guardioes.Tipo.COLETOR, nivel, asas);
        UUID alvoId = itemAlvo.get(id);
        Item item = alvoId != null && org.bukkit.Bukkit.getEntity(alvoId) instanceof Item i && coletavel(g, i, posto, raio) ? i : null;
        if (item == null) {
            itemAlvo.remove(id);
            Location pouso = posto.clone().add(0, 1.5, 0);
            if (g.getLocation().distanceSquared(pouso) > 1) Guardioes.passo(g, pouso, 0.5);
            if (ciclo % 10 != 0) return null;
            if (Equipamento.bausPerto(posto, raio + 2).isEmpty()) return "⚠ sem baú perto";
            if (nivel >= 3 && rodadas.merge(id, 1, Integer::sum) % 30 == 0) organizar(posto, raio + 2);
            double d = Double.MAX_VALUE;
            for (Entity e : g.getWorld().getNearbyEntities(posto, raio, 6, raio)) {
                if (!(e instanceof Item i) || !coletavel(g, i, posto, raio)) continue;
                double dd = e.getLocation().distanceSquared(g.getLocation());
                if (dd < d) { d = dd; item = i; }
            }
            if (item == null) return null;
            itemAlvo.put(id, item.getUniqueId());
        }
        Location l = item.getLocation().add(0, 0.6, 0);
        if (g.getLocation().distanceSquared(l) > 1.5 * 1.5) { Guardioes.passo(g, l, 0.45 + 0.05 * nivel); return null; }
        ItemStack s = item.getItemStack();
        Location onde = item.getLocation();
        item.remove();
        itemAlvo.remove(id);
        g.getWorld().playSound(onde, Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.8f, 1.2f);
        List<ItemStack> sobra = guardarOrganizado(g, posto, raio + 2, s, onde);
        for (ItemStack r : sobra) {
            g.getWorld().dropItem(onde, r, i -> i.getPersistentDataContainer().set(K_IGNORAR, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1));
        }
        return sobra.isEmpty() ? null : "⚠ os baús estão cheios";
    }

    /** O Coletor não pega: itens com dono, jogados há pouco por alguém, em cima de estações ou fora do território do dono. */
    private boolean coletavel(Mob g, Item i, Location posto, double raio) {
        if (!i.isValid() || i.getOwner() != null || i.getPersistentDataContainer().has(K_IGNORAR)) return false;
        if (i.getThrower() != null && i.getTicksLived() < 1200) return false;
        if (i.getLocation().distanceSquared(posto) > (raio + 2) * (raio + 2)) return false;
        Block b = i.getLocation().getBlock();
        if (plugin.ehEstacao(b.getRelative(BlockFace.DOWN)) || plugin.ehEstacao(b)) return false;
        return pode(g, b);
    }

    /** Guarda primeiro no baú que já tem aquele item; depois no mais perto; depois na mochila (nível III+). */
    private List<ItemStack> guardarOrganizado(Mob g, Location posto, double raio, ItemStack s, Location de) {
        List<org.bukkit.block.Container> baus = Equipamento.bausPerto(posto, raio);
        List<org.bukkit.block.Container> ordem = new ArrayList<>();
        for (org.bukkit.block.Container c : baus) if (c.getInventory().containsAtLeast(s, 1)) ordem.add(c);
        for (org.bukkit.block.Container c : baus) if (!ordem.contains(c)) ordem.add(c);
        ItemStack resto = s;
        for (org.bukkit.block.Container c : ordem) {
            Map<Integer, ItemStack> nao = c.getInventory().addItem(resto);
            if (nao.size() < 1 || nao.get(0).getAmount() < resto.getAmount()) {
                Guardioes.raio(de.clone().add(0, 0.5, 0), c.getLocation().add(0.5, 0.8, 0.5), Color.fromRGB(0xFFD54F));
            }
            if (nao.isEmpty()) return List.of();
            resto = nao.get(0);
        }
        if (Guardioes.nivel(g) >= 3) {
            org.bukkit.inventory.Inventory temp = org.bukkit.Bukkit.createInventory(null, 27);
            temp.setContents(Guardioes.mochila(g));
            Map<Integer, ItemStack> nao = temp.addItem(resto);
            Guardioes.guardarMochila(g, temp.getContents());
            return new ArrayList<>(nao.values());
        }
        return List.of(resto);
    }

    /** Nível III+: o que está no baú mais perto (a "entrada") vai para o baú que já guarda aquele item. */
    private static void organizar(Location posto, double raio) {
        List<org.bukkit.block.Container> baus = Equipamento.bausPerto(posto, raio);
        if (baus.size() < 2) return;
        org.bukkit.inventory.Inventory entrada = baus.getFirst().getInventory();
        int movidos = 0;
        for (int k = 0; k < entrada.getSize() && movidos < 8; k++) {
            ItemStack s = entrada.getItem(k);
            if (s == null || s.isEmpty()) continue;
            for (org.bukkit.block.Container c : baus.subList(1, baus.size())) {
                org.bukkit.inventory.Inventory inv = c.getInventory();
                if (java.util.Objects.equals(inv.getLocation(), entrada.getLocation()) || !inv.containsAtLeast(s, 1)) continue;
                Map<Integer, ItemStack> nao = inv.addItem(s.clone());
                entrada.setItem(k, nao.isEmpty() ? null : nao.get(0));
                movidos++;
                break;
            }
        }
    }
}
