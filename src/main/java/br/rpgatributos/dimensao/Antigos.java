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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 3.3: os Antigos no Abismo. Uma civilização que dominou o cobre, o cristal e a redstone, cavou fundo
 * demais e construiu um foguete para fugir para as Luas. Ficaram:
 * <ul>
 *   <li><b>As ruínas</b>: uma por camada, cavadas na parede das rachaduras (sala na rocha e varanda
 *       sobre o vazio): Posto de Observação, Elevador Antigo, Laboratório de Cristal e Refúgio dos que
 *       Caíram; no chão do Fundo, o Hangar, com o Vigia do Fundo. O plugin as constrói quando o chunk
 *       nasce, sempre dentro do próprio chunk.</li>
 *   <li><b>O Diário dos Antigos</b>: um capítulo no atril de cada ruína.</li>
 *   <li><b>As peças do foguete</b>: Planta, Casco, Núcleo de Navegação e Tanque nos baús das ruínas;
 *       o Motor fica com o Vigia do Fundo. Elas montam o foguete das Luas.</li>
 *   <li><b>A Carga Abissal</b>: quem não acha uma Fenda pode abrir a sua (detona e abre uma ali).</li>
 * </ul>
 */
public final class Antigos implements Listener {

    public static final TextColor COR = TextColor.color(0xC8703A);
    private static final NamespacedKey K_PECA = new NamespacedKey("rpgatributos", "peca_foguete");
    private static final NamespacedKey K_CARGA = new NamespacedKey("rpgatributos", "carga_abissal");

    public enum Peca {
        PLANTA("Planta do Foguete", Material.PAPER, "peca_planta", "Linhas azuis num papel que não amarela. Um foguete inteiro, peça por peça."),
        CASCO("Casco do Foguete", Material.IRON_INGOT, "peca_casco", "Uma chapa curva de Cobre dos Antigos, sem uma mancha de ferrugem."),
        NUCLEO("Núcleo de Navegação", Material.HEART_OF_THE_SEA, "peca_nucleo", "Um cristal abissal preso num aro de cobre. Aponta sempre para a lua."),
        TANQUE("Tanque de Combustível", Material.BUCKET, "peca_tanque", "Cheio de algo verde que borbulha. Cheira a esporo."),
        MOTOR("Motor do Foguete", Material.FIREWORK_STAR, "peca_motor", "Pesado, quente, e faz um zumbido baixo. O Vigia não queria que ninguém o levasse.");

        public final String nome;
        final Material base;
        final String modelo, historia;

        Peca(String nome, Material base, String modelo, String historia) {
            this.nome = nome;
            this.base = base;
            this.modelo = modelo;
            this.historia = historia;
        }

        public ItemStack item() {
            ItemStack i = new ItemStack(base);
            i.editMeta(m -> {
                m.itemName(Component.text(nome, COR));
                m.lore(List.of(Component.text(historia, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Peça do foguete dos Antigos (" + (ordinal() + 1) + " de 5).", COR).decoration(TextDecoration.ITALIC, false)));
                m.setEnchantmentGlintOverride(true);
                m.getPersistentDataContainer().set(K_PECA, PersistentDataType.STRING, name());
                PacoteRecursos.marcar(m, modelo);
            });
            return i;
        }

        public static Peca de(ItemStack i) {
            if (i == null || i.isEmpty()) return null;
            String v = i.getPersistentDataContainer().get(K_PECA, PersistentDataType.STRING);
            if (v == null) return null;
            try { return valueOf(v); } catch (IllegalArgumentException e) { return null; }
        }
    }

    // =====================================================================
    //  O Diário dos Antigos (um capítulo por ruína)
    // =====================================================================

    private static final String[][] DIARIO = {
            {"I. O Posto da Borda",
                    "Escrevo isto do posto da borda. Quando a terra rachou, achamos que era o fim do mundo. Era só o começo de um muito mais fundo.",
                    "O cobre daqui não enferruja. Com ele fizemos tudo: as torres, as cordas, o elevador. Desenhei a planta de algo maior. Algo que sobe em vez de descer.",
                    "Deixei a planta no baú. Se você está lendo, desça. As outras partes ficaram lá embaixo. — Ilen, engenheira"},
            {"II. O Elevador",
                    "O elevador levava cem de nós por dia até a floresta pendurada. Lá embaixo, o ar cheira a musgo e as raízes cantam quando venta.",
                    "Forjamos o casco aqui, chapa por chapa. Ele precisava aguentar o céu. O céu é mais pesado do que parece.",
                    "Os cabos estão rompendo. Vou descer pela última vez. — Ilen"},
            {"III. O Laboratório",
                    "O Cristal Abissal brilha mais quanto mais fundo estamos. Descobrimos por quê: ele lembra de onde veio. E ele não veio daqui.",
                    "Prendemos um cristal num aro de cobre e ele passou a apontar para a lua. Todas as noites, para a mesma lua. É o nosso caminho.",
                    "Alguns começaram a ouvir vozes no cristal. Dizem que são os que morreram aqui. Eu não ouço. Ainda. — Ilen"},
            {"IV. O Refúgio",
                    "Os que caíram fizeram uma cidade entre os cogumelos. Não querem subir. Dizem que lá em cima os deuses voltaram e não perdoam.",
                    "Os esporos daqui queimam devagar e não se apagam. Destilamos o combustível do foguete com eles. O tanque está pronto.",
                    "Falta o motor. Ele está no hangar, no Fundo. E alguma coisa acordou lá embaixo. — Ilen"},
            {"V. O Hangar",
                    "O Vigia acordou quando ligamos o motor. Ele não enxerga: ele ouve. Cada passo, cada respiração. Perdemos metade de nós em uma noite.",
                    "Quem chegar aqui: junte as cinco peças e monte o foguete lá em cima, debaixo do céu aberto. Siga o Núcleo. Ele leva à Lua Prateada. Nós fomos antes de você.",
                    "Alguns de nós não chegaram a lugar nenhum. Dizem que as almas deles ficaram num mundo cinza, esperando o barqueiro. Se encontrar alguém lá, diga que eu tentei. — Ilen"}};

    public static ItemStack diario(int capitulo) {
        String[] c = DIARIO[Math.max(0, Math.min(DIARIO.length - 1, capitulo))];
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.setTitle("Diário dos Antigos");
            m.setAuthor("Ilen, engenheira");
            List<Component> paginas = new ArrayList<>();
            paginas.add(Component.text(c[0] + "\n\n", COR, TextDecoration.BOLD).append(Component.text(c[1], NamedTextColor.BLACK)));
            for (int i = 2; i < c.length; i++) paginas.add(Component.text(c[i], NamedTextColor.BLACK));
            m.pages(paginas);
        });
        return livro;
    }

    // =====================================================================
    //  A Carga Abissal
    // =====================================================================

    public static ItemStack cargaAbissal() {
        ItemStack i = new ItemStack(Material.TNT);
        i.editMeta(m -> {
            m.itemName(Component.text("Carga Abissal", TextColor.color(0x9575CD)));
            m.lore(List.of(Component.text("Clique no chão, em campo aberto, e se afaste:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("ela abre uma Fenda do Abismo ali.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_CARGA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "carga_abissal");
        });
        return i;
    }

    private final RPGAtributos plugin;

    public Antigos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public void registrarReceitas() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "carga_abissal"), cargaAbissal());
        r.shape(" A ", "OTO", " E ");
        r.setIngredient('A', Material.AMETHYST_BLOCK);
        r.setIngredient('O', Material.CRYING_OBSIDIAN);
        r.setIngredient('T', Material.TNT);
        r.setIngredient('E', Material.ENDER_EYE);
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    @EventHandler(ignoreCancelled = false)
    public void aoUsarCarga(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        ItemStack mao = e.getItem();
        if (mao == null || !mao.getPersistentDataContainer().has(K_CARGA)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        World w = b.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || Dimensoes.de(w) != null || plugin.masmorras().ehMundo(w)) {
            p.sendActionBar(Component.text("A Carga Abissal só abre fendas no mundo normal.", NamedTextColor.RED));
            return;
        }
        if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, b)) {
            p.sendActionBar(Component.text("Aqui você não pode construir.", NamedTextColor.RED));
            return;
        }
        if (plugin.estruturas().temEstruturaPerto(b.getLocation(), 20)) {
            p.sendActionBar(Component.text("Muito perto de outra estrutura. Escolha um campo aberto.", NamedTextColor.RED));
            return;
        }
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
        Location centro = b.getLocation().add(0.5, 1, 0.5);
        p.sendMessage(Component.text("✦ A Carga Abissal começou a pulsar. Afaste-se!", TextColor.color(0x9575CD)));
        for (int i = 0; i < 5; i++) {
            final int falta = 5 - i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                w.spawnParticle(Particle.REVERSE_PORTAL, centro, 60, 0.6, 0.4, 0.6, 0.08);
                w.playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f, 0.5f + falta * 0.1f);
            }, i * 20L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            w.createExplosion(centro, 3f, false, false);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, centro, 2, 0.5, 0.5, 0.5, 0);
            w.playSound(centro, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.7f);
            String erro = plugin.estruturas().construirEm(w, b.getX(), b.getZ(), Estrutura.FENDA_ABISMO);
            if (erro != null) p.sendMessage(Component.text(erro, NamedTextColor.RED));
            else p.sendMessage(Component.text("✦ O chão se abriu. Uma Fenda do Abismo: é só pular.", TextColor.color(0x9575CD)));
        }, 110L);
    }

    // =====================================================================
    //  As ruínas (construídas quando o chunk do Abismo nasce)
    // =====================================================================

    private record Camada(Estrutura tipo, int yMin, int yMax, int capitulo) { }

    private static final Camada[] CAMADAS = {
            new Camada(Estrutura.ANTIGOS_POSTO, 125, 195, 0),
            new Camada(Estrutura.ANTIGOS_ELEVADOR, -110, 110, 1),
            new Camada(Estrutura.ANTIGOS_LABORATORIO, -390, -130, 2),
            new Camada(Estrutura.ANTIGOS_REFUGIO, -690, -410, 3)};

    @EventHandler
    public void aoNascer(ChunkLoadEvent e) {
        if (!e.isNewChunk() || Dimensoes.de(e.getWorld()) != Dimensoes.Dimensao.ABISMO) return;
        Chunk c = e.getChunk();
        World w = c.getWorld();
        Random r = new Random(w.getSeed() ^ (c.getX() * 341873128712L) ^ (c.getZ() * 132897987541L) ^ 0xA715L);
        double sorte = r.nextDouble();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!c.isLoaded()) return;
            if (sorte < 0.09) paredao(c, r);
            else if (sorte < 0.115) hangar(c, r);
        });
    }

    /** Admin: uma ruína no chunk do jogador (posto, elevador, laboratorio, refugio ou hangar), perto da altura dele. */
    public String construirAqui(Player p, String tipo) {
        Chunk c = p.getLocation().getChunk();
        Random r = new Random();
        if (tipo.equalsIgnoreCase("hangar")) {
            int antes = plugin.estruturas().quantas();
            hangar(c, r);
            return plugin.estruturas().quantas() > antes ? null : "O hangar só cabe no chão do Fundo (y abaixo de -900), num chunk quase todo de chão.";
        }
        for (Camada cam : CAMADAS) {
            if (!cam.tipo().name().endsWith(tipo.toUpperCase(java.util.Locale.ROOT))) continue;
            int y = p.getLocation().getBlockY();
            int antes = plugin.estruturas().quantas();
            paredao(c, r, new Camada(cam.tipo(), y - 15, y + 15, cam.capitulo()));
            return plugin.estruturas().quantas() > antes ? null : "Não achei uma parede de rocha virada para o vazio neste chunk, perto da sua altura.";
        }
        return "Use: posto, elevador, laboratorio, refugio ou hangar.";
    }

    /** Uma ruína na parede de uma rachadura: acha a parede, cava a sala para dentro e põe a varanda. */
    private void paredao(Chunk c, Random r) {
        paredao(c, r, CAMADAS[r.nextInt(CAMADAS.length)]);
    }

    private void paredao(Chunk c, Random r, Camada cam) {
        World w = c.getWorld();
        BlockFace[] lados = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        // Procura com método (não ao acaso): andares da camada a partir de um ponto sorteado, os 4 lados e
        // só os lugares onde a sala e a varanda cabem neste chunk (para não gerar os chunks vizinhos).
        int altura = cam.yMax - cam.yMin, inicio = r.nextInt(Math.max(1, altura));
        for (int i = 0; i < altura; i += 2) {
            int y = cam.yMin + (inicio + i) % altura;
            int l0 = r.nextInt(4);
            for (int li = 0; li < 4; li++) {
                BlockFace vazio = lados[(l0 + li) % 4];
                BlockFace dentro = vazio.getOppositeFace();
                BlockFace direita = direita(dentro);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    Block parede = c.getBlock(x, y, z);
                    // A sala (7 fundo, 7 largura, 5 altura) e a varanda têm que caber neste chunk.
                    if (!cabe(c, parede, dentro, direita, vazio) || !parede.getType().isSolid()) continue;
                    // Do lado da rachadura: ar na frente e acima (onde fica a varanda).
                    boolean livre = true;
                    for (int k = 1; k <= 3 && livre; k++) for (int h = 0; h <= 3 && livre; h++)
                        if (!parede.getRelative(vazio, k).getRelative(0, h, 0).getType().isAir()) livre = false;
                    if (!livre) continue;
                    int rocha = 0, total = 0;
                    for (int k = 1; k <= 7; k++) for (int s = -3; s <= 3; s++) for (int h = 0; h <= 4; h++) {
                        total++;
                        if (local(parede, dentro, direita, k, s, h).getType().isSolid()) rocha++;
                    }
                    if (rocha < total * 0.8) continue;
                    construirSala(parede, dentro, direita, vazio, cam, r);
                    Block meio = local(parede, dentro, direita, 4, 0, 0);
                    plugin.estruturas().anotarRuina(w, cam.tipo(), meio.getX(), meio.getY(), meio.getZ());
                    return;
                }
            }
        }
    }

    private static BlockFace direita(BlockFace f) {
        return switch (f) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    /** O bloco {@code k} para dentro, {@code s} para a direita e {@code h} para cima, a partir da parede. */
    private static Block local(Block parede, BlockFace dentro, BlockFace direita, int k, int s, int h) {
        return parede.getRelative(dentro.getModX() * k + direita.getModX() * s, h, dentro.getModZ() * k + direita.getModZ() * s);
    }

    private static boolean cabe(Chunk c, Block parede, BlockFace dentro, BlockFace direita, BlockFace vazio) {
        for (int k = -3; k <= 8; k++) for (int s = -4; s <= 4; s++) {
            Block b = local(parede, dentro, direita, k, s, 0);
            if (b.getX() >> 4 != c.getX() || b.getZ() >> 4 != c.getZ()) return false;
        }
        return parede.getY() + 6 < parede.getWorld().getMaxHeight() && parede.getY() - 2 > parede.getWorld().getMinHeight();
    }

    private void construirSala(Block parede, BlockFace dentro, BlockFace direita, BlockFace vazio, Camada cam, Random r) {
        Material piso, parede1, parede2, luz;
        switch (cam.tipo()) {
            case ANTIGOS_POSTO -> { piso = Material.CUT_COPPER; parede1 = Material.OXIDIZED_CUT_COPPER; parede2 = Material.WEATHERED_COPPER; luz = Material.COPPER_BULB; }
            case ANTIGOS_ELEVADOR -> { piso = Material.POLISHED_DEEPSLATE; parede1 = Material.WAXED_EXPOSED_COPPER; parede2 = Material.DEEPSLATE_TILES; luz = Material.LANTERN; }
            case ANTIGOS_LABORATORIO -> { piso = Material.POLISHED_TUFF; parede1 = Material.AMETHYST_BLOCK; parede2 = Material.CALCITE; luz = Material.SEA_LANTERN; }
            default -> { piso = Material.SPRUCE_PLANKS; parede1 = Material.MUSHROOM_STEM; parede2 = Material.STRIPPED_SPRUCE_LOG; luz = Material.SHROOMLIGHT; }
        }
        // Casca da sala (a rocha em volta vira parede da ruína), o ar dentro e o piso.
        for (int k = 0; k <= 8; k++) for (int s = -4; s <= 4; s++) for (int h = -1; h <= 5; h++) {
            Block b = local(parede, dentro, direita, k, s, h);
            boolean borda = k == 0 || k == 8 || Math.abs(s) == 4 || h == -1 || h == 5;
            if (borda) {
                if (k == 0 && h >= 0 && h <= 2 && Math.abs(s) <= 1) b.setType(Material.AIR, false); // a porta para a varanda
                else b.setType(h == -1 ? piso : (r.nextDouble() < 0.7 ? parede1 : parede2), false);
            } else {
                b.setType(Material.AIR, false);
            }
        }
        // A varanda sobre o vazio, com grade.
        for (int k = 1; k <= 3; k++) for (int s = -2; s <= 2; s++) {
            Block chao = local(parede, dentro, direita, -k, s, -1);
            chao.setType(piso, false);
            if (k == 3 || Math.abs(s) == 2) local(parede, dentro, direita, -k, s, 0).setType(Material.COPPER_GRATE, false);
        }
        // Luzes nos cantos e um pilar em cada lado da porta.
        local(parede, dentro, direita, 1, -3, 4).setType(luz, false);
        local(parede, dentro, direita, 1, 3, 4).setType(luz, false);
        local(parede, dentro, direita, 7, -3, 4).setType(luz, false);
        local(parede, dentro, direita, 7, 3, 4).setType(luz, false);
        if (luz == Material.COPPER_BULB) for (int[] q : new int[][]{{1, -3}, {1, 3}, {7, -3}, {7, 3}}) {
            Block b = local(parede, dentro, direita, q[0], q[1], 4);
            BlockData d = Bukkit.createBlockData("minecraft:copper_bulb[lit=true,powered=false]");
            b.setBlockData(d, false);
        }
        // O que cada uma tem.
        switch (cam.tipo()) {
            case ANTIGOS_POSTO -> {
                local(parede, dentro, direita, 1, 0, 0).getRelative(vazio, 3).setType(Material.LIGHTNING_ROD, false);
                local(parede, dentro, direita, 6, -2, 0).setType(Material.CARTOGRAPHY_TABLE, false);
                local(parede, dentro, direita, 6, 2, 0).setType(Material.LIGHTNING_ROD, false);
            }
            case ANTIGOS_ELEVADOR -> {
                for (int h = 0; h <= 4; h++) local(parede, dentro, direita, 6, 0, h).setBlockData(Bukkit.createBlockData("minecraft:iron_chain[axis=y]"), false);
                for (int s = -1; s <= 1; s++) for (int h = 0; h <= 3; h++) {
                    if (s == 0 && h < 4) continue;
                    local(parede, dentro, direita, 5, s, h).setType(Material.IRON_BARS, false);
                }
                local(parede, dentro, direita, 4, 3, 1).setType(Material.LEVER, false);
            }
            case ANTIGOS_LABORATORIO -> {
                local(parede, dentro, direita, 6, -2, 0).setType(Material.BREWING_STAND, false);
                local(parede, dentro, direita, 6, 2, 0).setType(Material.BUDDING_AMETHYST, false);
                local(parede, dentro, direita, 6, 2, 1).setType(Material.AMETHYST_CLUSTER, false);
                local(parede, dentro, direita, 3, -3, 0).setType(Material.CRAFTER, false);
            }
            default -> {
                local(parede, dentro, direita, 6, -2, 0).setType(Material.SOUL_CAMPFIRE, false);
                local(parede, dentro, direita, 6, 2, 0).setType(Material.BARREL, false);
                local(parede, dentro, direita, 4, 3, 0).setType(Material.RED_MUSHROOM, false);
                local(parede, dentro, direita, 3, -3, 0).setType(Material.BROWN_MUSHROOM, false);
            }
        }
        // O atril com o capítulo do diário e o baú com a peça do foguete.
        Block atril = local(parede, dentro, direita, 7, -1, 0);
        atril.setType(Material.LECTERN, false);
        if (atril.getBlockData() instanceof Directional d) {
            d.setFacing(dentro.getOppositeFace());
            atril.setBlockData(d, false);
        }
        if (atril.getState() instanceof Lectern l) l.getInventory().setItem(0, diario(cam.capitulo()));
        Block bau = local(parede, dentro, direita, 7, 1, 0);
        bau.setType(Material.CHEST, false);
        if (bau.getBlockData() instanceof Directional d) {
            d.setFacing(dentro.getOppositeFace());
            bau.setBlockData(d, false);
        }
        if (bau.getState() instanceof Chest ch) ch.getBlockInventory().addItem(saque(cam, r).toArray(new ItemStack[0]));
    }

    private List<ItemStack> saque(Camada cam, Random r) {
        List<ItemStack> l = new ArrayList<>();
        switch (cam.tipo()) {
            case ANTIGOS_POSTO -> {
                l.add(Peca.PLANTA.item());
                l.add(new ItemStack(Material.COPPER_INGOT, 4 + r.nextInt(8)));
                l.add(Raro.COBRE_ANTIGO.criar(2 + r.nextInt(3)));
                if (r.nextBoolean()) l.add(new ItemStack(Material.SPYGLASS));
            }
            case ANTIGOS_ELEVADOR -> {
                l.add(Peca.CASCO.item());
                l.add(Raro.COBRE_ANTIGO.criar(3 + r.nextInt(4)));
                l.add(new ItemStack(Material.IRON_INGOT, 3 + r.nextInt(6)));
                l.add(new ItemStack(Material.IRON_CHAIN, 4 + r.nextInt(6)));
            }
            case ANTIGOS_LABORATORIO -> {
                l.add(Peca.NUCLEO.item());
                l.add(Raro.CRISTAL_ABISSAL.criar(2 + r.nextInt(3)));
                l.add(new ItemStack(Material.AMETHYST_SHARD, 6 + r.nextInt(10)));
                l.add(new ItemStack(Material.REDSTONE, 8 + r.nextInt(12)));
            }
            default -> {
                l.add(Peca.TANQUE.item());
                l.add(Raro.ESPORO_FOSSIL.criar(2 + r.nextInt(3)));
                l.add(new ItemStack(Material.BREAD, 3 + r.nextInt(5)));
                l.add(new ItemStack(Material.GLOW_BERRIES, 4 + r.nextInt(8)));
            }
        }
        if (r.nextDouble() < 0.35) l.add(plugin.arqueologia().reliquiaAleatoria());
        if (r.nextDouble() < 0.2) l.add(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio());
        return l;
    }

    /** O Hangar dos Antigos, no chão do Fundo (só onde uma rachadura chega lá embaixo). */
    private void hangar(Chunk c, Random r) {
        World w = c.getWorld();
        int x0 = c.getX() * 16, z0 = c.getZ() * 16;
        // O chão tem que ser o fundo da rachadura em quase todo o chunk.
        int baixos = 0, soma = 0;
        for (int x = 1; x < 15; x += 2) for (int z = 1; z < 15; z += 2) {
            int y = w.getHighestBlockYAt(x0 + x, z0 + z);
            if (y < -900) { baixos++; soma += y; }
        }
        if (baixos < 40) return;
        int piso = soma / baixos;
        // Piso, pilares, teto em arco e o motor desmontado no meio.
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) {
            w.getBlockAt(x0 + x, piso, z0 + z).setType((x + z) % 2 == 0 ? Material.POLISHED_DEEPSLATE : Material.WAXED_OXIDIZED_CUT_COPPER, false);
            for (int y = piso + 1; y <= piso + 10; y++) w.getBlockAt(x0 + x, y, z0 + z).setType(Material.AIR, false);
        }
        for (int[] q : new int[][]{{1, 1}, {1, 14}, {14, 1}, {14, 14}, {1, 7}, {14, 7}}) {
            for (int y = piso + 1; y <= piso + 9; y++) w.getBlockAt(x0 + q[0], y, z0 + q[1]).setType(Material.REINFORCED_DEEPSLATE, false);
            w.getBlockAt(x0 + q[0], piso + 9, z0 + q[1]).getRelative(0, 1, 0).setType(Material.SCULK_SENSOR, false);
        }
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) if ((x == 1 || x == 14 || z == 1 || z == 14) && r.nextDouble() < 0.7)
            w.getBlockAt(x0 + x, piso + 10, z0 + z).setType(Material.WAXED_OXIDIZED_COPPER, false);
        // O berço do motor (o motor em si fica com o Vigia).
        int mx = x0 + 7, mz = z0 + 7;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) w.getBlockAt(mx + dx, piso + 1, mz + dz).setType(Material.IRON_BLOCK, false);
        for (int y = piso + 2; y <= piso + 4; y++) w.getBlockAt(mx, y, mz).setType(Material.BLAST_FURNACE, false);
        w.getBlockAt(mx, piso + 5, mz).setType(Material.LIGHTNING_ROD, false);
        for (int y = piso + 2; y <= piso + 8; y++) w.getBlockAt(mx + 2, y, mz).setBlockData(Bukkit.createBlockData("minecraft:iron_chain[axis=y]"), false);
        // Atril (capítulo V) e baú.
        Block atril = w.getBlockAt(x0 + 3, piso + 1, z0 + 12);
        atril.setType(Material.LECTERN, false);
        if (atril.getState() instanceof Lectern l) l.getInventory().setItem(0, diario(4));
        Block bau = w.getBlockAt(x0 + 12, piso + 1, z0 + 12);
        bau.setType(Material.CHEST, false);
        if (bau.getState() instanceof Chest ch) {
            List<ItemStack> l = new ArrayList<>(List.of(Raro.FERRO_NEGRO.criar(3 + r.nextInt(4)), new ItemStack(Material.ECHO_SHARD, 1 + r.nextInt(3)),
                    new ItemStack(Material.NETHERITE_SCRAP, r.nextDouble() < 0.4 ? 2 : 1)));
            if (r.nextDouble() < 0.5) l.add(plugin.arqueologia().reliquiaAleatoria());
            ch.getBlockInventory().addItem(l.toArray(new ItemStack[0]));
        }
        for (int[] q : new int[][]{{3, 3}, {12, 3}, {3, 10}}) w.getBlockAt(x0 + q[0], piso + 9, z0 + q[1]).setType(Material.SHROOMLIGHT, false);
        plugin.estruturas().anotarRuina(w, Estrutura.ANTIGOS_HANGAR, mx, piso + 1, mz);
    }
}
