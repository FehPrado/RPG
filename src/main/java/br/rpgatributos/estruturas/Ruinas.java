package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.arcano.ItensMagicos;
import br.rpgatributos.arcano.Receita;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Carta;
import br.rpgatributos.vida.Fruta;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.boat.SpruceChestBoat;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

/**
 * As plantas das estruturas da 2.25: oásis, cabana da bruxa, farol, expedição perdida, vila
 * saqueada, torre do mago, círculo de pedras, forja dos anões e fortim. Mesmas regras de
 * {@link Projetos}: coordenadas locais, frente para +z, chão já nivelado.
 */
final class Ruinas {

    /** Onde ficam os moradores (x, y, z locais). */
    static final int[] NOMADE = {4, 1, 3};
    static final int[] BRUXA = {0, 4, -1};
    /** A lanterna do farol. */
    static final int[] LANTERNA = {0, 16, 0};
    static final int[][] GUARDAS_EXPEDICAO = {{-1, 1, 0}, {2, 1, -3}, {1, 1, 4}};
    static final int[][] GUARDAS_VILA = {{0, 1, 2}, {-2, 1, 0}, {2, 1, 0}, {0, 1, -2}};
    static final int[] APRENDIZ = {0, 11, 0};
    static final int[][] ANOES = {{-2, -1, 0}, {2, -1, 0}, {0, -1, -2}};
    static final int[] CAPITAO = {0, 5, -3};
    static final int[][] SOLDADOS = {{-4, 1, 2}, {4, 1, 2}, {-4, 1, -3}, {4, 1, -3}};
    /** As pedras do círculo (x, z). */
    static final int[][] PEDRAS = {{5, 0}, {3, 4}, {0, 5}, {-3, 4}, {-5, 0}, {-3, -4}, {0, -5}, {3, -4}};

    private static final String[] LIDERES = {"Capitão Ulrich", "Dona Helga", "Mestre Anselmo", "Sir Edmundo", "Ingrid, a Cartógrafa"};
    private static final String[] MAGOS = {"Mestre Valerius", "Arquimaga Selene", "Velho Orfeu", "Mestre Caius"};
    private static final String[] SOBREVIVENTES = {"Ana", "Tomé", "Rosa", "Bartolomeu", "Clara"};

    private final RPGAtributos plugin;
    private final Projetos base;

    Ruinas(RPGAtributos plugin, Projetos base) {
        this.plugin = plugin;
        this.base = base;
    }

    private static boolean octo(int dx, int dz, int r) {
        return Math.abs(dx) <= r && Math.abs(dz) <= r && !(Math.abs(dx) == r && Math.abs(dz) == r);
    }

    private static String lado(int dx, int dz, int a) {
        return dz == a ? "north" : dz == -a ? "south" : dx == a ? "west" : "east";
    }

    static ItemStack livro(String titulo, String autor, String... paginas) {
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text(titulo));
            m.author(Component.text(autor));
            for (String p : paginas) m.addPages(Component.text(p));
        });
        return livro;
    }

    static void porLivro(Block pulpito, ItemStack livro) {
        if (pulpito.getState(false) instanceof Lectern lec) lec.getInventory().setItem(0, livro);
    }

    // =====================================================================
    //  Oásis do Deserto
    // =====================================================================

    void oasis(Obra o) {
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) o.por(dx, 0, dz, Material.SAND);
        // Lagoa irregular em volta de (-2, -1).
        Set<Long> lagoa = new HashSet<>();
        for (int dx = -5; dx <= 1; dx++) {
            for (int dz = -4; dz <= 2; dz++) {
                int r2 = (dx + 2) * (dx + 2) + (dz + 1) * (dz + 1);
                if (r2 <= 5 || r2 <= 8 && o.chance(0.45)) lagoa.add(((long) dx << 32) | (dz & 0xffffffffL));
            }
        }
        for (long k : lagoa) {
            int dx = (int) (k >> 32), dz = (int) k;
            o.por(dx, -2, dz, Material.SAND);
            o.por(dx, -1, dz, Material.WATER);
            o.por(dx, 0, dz, Material.WATER);
        }
        // Margem de grama, com capim, juncos (cana) e vitórias-régias.
        for (int dx = -6; dx <= 2; dx++) {
            for (int dz = -5; dz <= 3; dz++) {
                if (lagoa.contains(((long) dx << 32) | (dz & 0xffffffffL))) continue;
                boolean margem = false;
                for (int ax = -1; ax <= 1 && !margem; ax++) {
                    for (int az = -1; az <= 1; az++) if (lagoa.contains(((long) (dx + ax) << 32) | ((dz + az) & 0xffffffffL))) margem = true;
                }
                if (!margem) continue;
                o.por(dx, 0, dz, Material.GRASS_BLOCK);
                boolean encostaNaAgua = lagoa.contains(((long) (dx + 1) << 32) | (dz & 0xffffffffL)) || lagoa.contains(((long) (dx - 1) << 32) | (dz & 0xffffffffL))
                        || lagoa.contains(((long) dx << 32) | ((dz + 1) & 0xffffffffL)) || lagoa.contains(((long) dx << 32) | ((dz - 1) & 0xffffffffL));
                if (encostaNaAgua && o.chance(0.2)) {
                    int h = 2 + o.r.nextInt(2);
                    for (int dy = 1; dy <= h; dy++) o.por(dx, dy, dz, Material.SUGAR_CANE);
                } else if (o.chance(0.35)) {
                    o.por(dx, 1, dz, o.um(Material.SHORT_GRASS, 3, Material.FERN, 1));
                }
            }
        }
        int vit = 0;
        for (long k : lagoa) {
            if (vit >= 3 || !o.chance(0.25)) continue;
            o.por((int) (k >> 32), 1, (int) k, Material.LILY_PAD);
            vit++;
        }
        // Palmeiras.
        palmeira(o, -4, 3, 0, 1);
        palmeira(o, 1, -3, 1, 0);
        palmeira(o, -4, -4, 1, 0);
        // Tenda do nômade: toldo listrado sobre quatro postes, tapetes e mantimentos.
        for (int[] p : new int[][]{{2, 1}, {6, 1}, {2, 5}, {6, 5}}) {
            o.por(p[0], 1, p[1], Material.SPRUCE_FENCE);
            o.por(p[0], 2, p[1], Material.SPRUCE_FENCE);
        }
        for (int dx = 2; dx <= 6; dx++) {
            for (int dz = 1; dz <= 5; dz++) {
                o.por(dx, 3, dz, dz % 2 == 0 ? Material.WHITE_WOOL : Material.ORANGE_WOOL);
                if (o.bloco(dx, 1, dz).isEmpty() && o.chance(0.7)) o.por(dx, 1, dz, o.um(Material.RED_CARPET, 2, Material.ORANGE_CARPET, 2, Material.YELLOW_CARPET, 1));
            }
        }
        o.por(5, 1, 5, "barrel[facing=up]");
        o.por(5, 2, 5, "barrel[facing=up]");
        o.por(3, 1, 5, "decorated_pot");
        List<ItemStack> sacos = new ArrayList<>(List.of(new ItemStack(Material.BREAD, 2 + o.r.nextInt(3)), new ItemStack(Material.DRIED_KELP, 4)));
        if (o.chance(0.5)) sacos.add(Fruta.sortear(o.r).fruta(2 + o.r.nextInt(3)));
        base.bau(o.bloco(5, 1, 5), null, o, sacos);
        // Fogueira com pedras.
        o.por(3, 1, -2, "campfire[lit=true]");
        for (int[] p : new int[][]{{2, -2}, {4, -2}, {3, -1}, {3, -3}}) o.por(p[0], 0, p[1], Material.SMOOTH_SANDSTONE);
        o.por(4, 1, -3, Material.CAULDRON);
    }

    /** Palmeira: tronco com uma leve curva para (ix, iz) e a copa em leque. */
    private void palmeira(Obra o, int x, int z, int ix, int iz) {
        int h = 5 + o.r.nextInt(2);
        int cx = x, cz = z;
        for (int dy = 1; dy <= h; dy++) {
            if (dy == h - 1) {
                cx += ix;
                cz += iz;
            }
            o.por(cx, dy, cz, "jungle_log[axis=y]");
        }
        String folha = "jungle_leaves[persistent=true]";
        o.por(cx, h + 1, cz, folha);
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            o.por(cx + d[0], h + 1, cz + d[1], folha);
            o.por(cx + 2 * d[0], h, cz + 2 * d[1], folha);
            o.por(cx + 3 * d[0], h - 1, cz + 3 * d[1], folha);
        }
        for (int[] d : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) o.por(cx + d[0], h, cz + d[1], folha);
        if (o.chance(0.6) && o.bloco(x, h - 2, z + 1).isEmpty()) o.por(x, h - 2, z + 1, "cocoa[age=2,facing=north]");
    }

    // =====================================================================
    //  Cabana da Bruxa (sobre palafitas no pântano)
    // =====================================================================

    void bruxa(Obra o) {
        // Palafitas até o fundo (passa pela água).
        for (int[] p : new int[][]{{-3, -4}, {3, -4}, {-3, 3}, {3, 3}, {-3, 0}, {3, 0}}) {
            for (int dy = 2; dy >= -8; dy--) {
                Block b = o.bloco(p[0], dy, p[1]);
                if (dy < 0 && b.getType().isSolid() && !b.isLiquid()) break;
                o.por(p[0], dy, p[1], "dark_oak_log[axis=y]");
            }
            if (o.chance(0.6)) for (int dy = 0; dy <= 2; dy++) {
                if (o.bloco(p[0], dy, p[1] + 1).isEmpty()) o.por(p[0], dy, p[1] + 1, "vine[north=true]");
            }
        }
        // Plataforma e varanda.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -4; dz <= 3; dz++) o.por(dx, 3, dz, o.um(Material.SPRUCE_PLANKS, 3, Material.DARK_OAK_PLANKS, 1));
        for (int dx = -3; dx <= 3; dx++) if (dx != 0) o.por(dx, 4, 3, Material.SPRUCE_FENCE);
        o.por(-3, 4, 2, Material.SPRUCE_FENCE);
        o.por(3, 4, 2, Material.SPRUCE_FENCE);
        // Escadinha até a água e um píer.
        o.por(0, 2, 4, "spruce_stairs[facing=north]");
        o.por(0, 1, 5, "spruce_stairs[facing=north]");
        o.por(0, 0, 6, Material.SPRUCE_PLANKS);
        o.por(0, 0, 7, Material.SPRUCE_PLANKS);
        // Casa (x -2..2, z -3..1), paredes de 3, janelas e porta.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -3; dz <= 1; dz++) {
                boolean parede = Math.abs(dx) == 2 || dz == -3 || dz == 1;
                boolean canto = Math.abs(dx) == 2 && (dz == -3 || dz == 1);
                for (int dy = 4; dy <= 6; dy++) {
                    if (canto) o.por(dx, dy, dz, "dark_oak_log[axis=y]");
                    else if (parede) o.por(dx, dy, dz, o.um(Material.SPRUCE_PLANKS, 3, Material.DARK_OAK_PLANKS, 1));
                    else o.por(dx, dy, dz, Material.AIR);
                }
            }
        }
        o.por(2, 5, -1, Material.BROWN_STAINED_GLASS_PANE);
        o.por(-2, 5, -1, Material.BROWN_STAINED_GLASS_PANE);
        o.por(0, 4, 1, "spruce_door[facing=north,half=lower,hinge=right]");
        o.por(0, 5, 1, "spruce_door[facing=north,half=upper,hinge=right]");
        // Telhado e empenas.
        for (int dz = -4; dz <= 2; dz++) {
            o.por(-3, 7, dz, "dark_oak_stairs[facing=east]");
            o.por(3, 7, dz, "dark_oak_stairs[facing=west]");
            o.por(-2, 8, dz, "dark_oak_stairs[facing=east]");
            o.por(2, 8, dz, "dark_oak_stairs[facing=west]");
            o.por(-1, 9, dz, "dark_oak_stairs[facing=east]");
            o.por(1, 9, dz, "dark_oak_stairs[facing=west]");
            o.por(0, 10, dz, "dark_oak_slab[type=bottom]");
        }
        for (int dz : new int[]{-3, 1}) {
            for (int dx = -2; dx <= 2; dx++) o.por(dx, 7, dz, Material.SPRUCE_PLANKS);
            for (int dx = -1; dx <= 1; dx++) o.por(dx, 8, dz, Material.SPRUCE_PLANKS);
            o.por(0, 9, dz, Material.SPRUCE_PLANKS);
        }
        for (int dx = -1; dx <= 1; dx++) o.por(dx, 7, -1, "dark_oak_log[axis=x]");
        o.por(0, 6, -1, "soul_lantern[hanging=true]");
        // Dentro: caldeirão borbulhando, alambique, cogumelos em vaso e o baú dos ingredientes.
        o.por(-1, 4, -2, "water_cauldron[level=3]");
        o.por(1, 4, -2, Material.BREWING_STAND);
        o.por(-1, 4, 0, Material.POTTED_RED_MUSHROOM);
        o.por(1, 4, 0, "barrel[facing=up]");
        List<ItemStack> ingred = new ArrayList<>();
        ingred.add(new ItemStack(Material.SPIDER_EYE, 1 + o.r.nextInt(3)));
        ingred.add(new ItemStack(Material.GLOWSTONE_DUST, 2 + o.r.nextInt(3)));
        ingred.add(Reagente.values()[o.r.nextInt(Reagente.values().length)].criar(1));
        base.bau(o.bloco(1, 4, 0), null, o, ingred);
        // Cogumelos e vitórias-régias em volta.
        for (int i = 0; i < 8; i++) {
            int dx = -5 + o.r.nextInt(11), dz = -5 + o.r.nextInt(11);
            if (Math.abs(dx) <= 3 && dz >= -4 && dz <= 7) continue;
            Block chao = o.bloco(dx, 0, dz);
            if (chao.getType() == Material.WATER && o.bloco(dx, 1, dz).isEmpty()) o.por(dx, 1, dz, Material.LILY_PAD);
            else if (chao.getType().isSolid() && o.bloco(dx, 1, dz).isEmpty()) o.por(dx, 1, dz, o.um(Material.RED_MUSHROOM, 1, Material.BROWN_MUSHROOM, 1));
        }
    }

    // =====================================================================
    //  Farol Abandonado
    // =====================================================================

    void farol(Obra o) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (octo(dx, dz, 3)) o.por(dx, 0, dz, o.um(Material.COBBLESTONE, 2, Material.STONE_BRICKS, 2, Material.MOSSY_COBBLESTONE, 1));
            }
        }
        // Torre listrada (branco e vermelho), com remendos de pedra.
        for (int dy = 1; dy <= 14; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (!octo(dx, dz, 2)) continue;
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        Material m = ((dy - 1) / 3) % 2 == 0 ? Material.CALCITE : Material.RED_TERRACOTTA;
                        o.por(dx, dy, dz, o.chance(0.07) ? Material.STONE_BRICKS : m);
                    } else {
                        o.por(dx, dy, dz, Material.AIR);
                    }
                }
            }
        }
        o.por(0, 1, 2, Material.AIR);
        o.por(0, 2, 2, Material.AIR);
        for (int dy : new int[]{5, 9, 13}) {
            o.por(2, dy, 0, Material.GLASS_PANE);
            o.por(-2, dy, 0, Material.GLASS_PANE);
        }
        for (int piso : new int[]{5, 10}) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!(dx == 0 && dz == -1)) o.por(dx, piso, dz, Material.SPRUCE_PLANKS);
        }
        for (int dy = 1; dy <= 15; dy++) o.por(0, dy, -1, "ladder[facing=south]");
        // Galeria com grade e a sala da lanterna.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!octo(dx, dz, 3) || dx == 0 && dz == -1) continue;
                o.por(dx, 15, dz, Material.SMOOTH_STONE);
                boolean borda = Math.abs(dx) == 3 || Math.abs(dz) == 3 || Math.abs(dx) == 2 && Math.abs(dz) == 2;
                if (borda && !o.chance(0.15)) o.por(dx, 16, dz, Material.IRON_BARS);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 16; dy <= 17; dy++) o.por(dx, dy, dz, dx == 0 && dz == -1 ? Material.AIR : Material.GLASS_PANE);
            }
        }
        o.por(LANTERNA[0], LANTERNA[1], LANTERNA[2], "redstone_lamp[lit=false]");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) o.por(dx, 18, dz, Material.RED_TERRACOTTA);
        o.por(0, 19, 0, Material.RED_TERRACOTTA);
        o.por(0, 20, 0, "lightning_rod[facing=up]");
        // O quarto do faroleiro.
        o.por(-1, 1, 1, "chest[facing=east]");
        List<ItemStack> extras = new ArrayList<>();
        extras.add(new ItemStack(Material.EMERALD, 2 + o.r.nextInt(4)));
        if (o.chance(0.3)) extras.add(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, o.bloco(0, 0, 0).getLocation()));
        if (o.chance(0.2)) extras.add(new ItemStack(Material.NAUTILUS_SHELL));
        extras.add(new ItemStack(Material.GLOWSTONE_DUST, 2 + o.r.nextInt(3)));
        base.bau(o.bloco(-1, 1, 1), LootTables.SHIPWRECK_SUPPLY, o, extras);
        o.por(1, 1, 1, "barrel[facing=up]");
        base.bau(o.bloco(1, 1, 1), null, o, List.of(new ItemStack(Material.COD, 3 + o.r.nextInt(4)), new ItemStack(Material.FISHING_ROD)));
        o.por(1, 1, 0, Material.WHITE_CARPET);
    }

    // =====================================================================
    //  Acampamento da Expedição Perdida (neve)
    // =====================================================================

    void expedicao(Obra o) {
        String lider = LIDERES[o.r.nextInt(LIDERES.length)];
        // Barraca de pé, com o diário e os mantimentos.
        Obra barraca = o.parte(-3, -2, 0);
        base.tenda(barraca, Material.WHITE_WOOL, false);
        List<ItemStack> coisas = new ArrayList<>();
        coisas.add(livro("Diário da Expedição", lider,
                "Dia 1.\n\nPartimos com seis homens, rumo ao norte. " + lider + " jura que o que procuramos está além do gelo.",
                "Dia 9.\n\nO frio levou dois. Os outros ouvem passos à noite, perto da fogueira apagada. Seguimos o mapa.",
                "Dia 14.\n\nNão dá mais para andar. Se alguém achar isto: o mapa está no trenó.\n\nTermine o que começamos."));
        coisas.add(new ItemStack(Material.COMPASS));
        base.bau(barraca.bloco(1, 1, -1), null, o, coisas);
        // Barraca desabada.
        for (int dx = 2; dx <= 6; dx++) {
            for (int dz = -4; dz <= -1; dz++) if (o.chance(0.55)) o.por(dx, 1, dz, o.um(Material.LIGHT_GRAY_CARPET, 2, Material.WHITE_CARPET, 2));
        }
        o.por(4, 1, -2, "spruce_fence");
        o.por(2, 1, -4, "spruce_log[axis=x]");
        // Fogueira apagada e troncos.
        o.por(0, 1, 2, "campfire[lit=false]");
        o.por(-2, 1, 2, "stripped_spruce_log[axis=z]");
        o.por(2, 1, 2, "stripped_spruce_log[axis=z]");
        // Gelo e neve por cima de tudo.
        for (int i = 0; i < 4; i++) {
            int dx = -5 + o.r.nextInt(11), dz = -5 + o.r.nextInt(11);
            if (o.bloco(dx, 1, dz).isEmpty()) o.por(dx, 1, dz, Material.PACKED_ICE);
        }
        // O explorador que ficou (suporte com roupa de couro e uma caveira).
        Block corpo = o.bloco(-1, 1, 4);
        corpo.getWorld().spawn(corpo.getLocation().add(0.5, 0, 0.5), ArmorStand.class, a -> {
            a.setBasePlate(false);
            a.setArms(true);
            a.setHeadPose(new EulerAngle(0.5, 0.3, 0));
            a.setLeftArmPose(new EulerAngle(-0.6, 0, -0.2));
            a.getEquipment().setHelmet(new ItemStack(Material.SKELETON_SKULL));
            a.getEquipment().setChestplate(couro(Material.LEATHER_CHESTPLATE, 0x8D6E63));
            a.getEquipment().setLeggings(couro(Material.LEATHER_LEGGINGS, 0x5D4037));
            a.getEquipment().setBoots(couro(Material.LEATHER_BOOTS, 0x3E2723));
            a.getEquipment().setItemInMainHand(new ItemStack(Material.LANTERN));
        });
        // O trenó (barco com baú), com o mapa que eles seguiam.
        Block treno = o.bloco(3, 1, 4);
        List<ItemStack> carga = new ArrayList<>();
        carga.add(plugin.mapas().criar(o.chance(0.4) ? MapasDoTesouro.Tipo.RARO : MapasDoTesouro.Tipo.COMUM, treno.getLocation()));
        carga.add(new ItemStack(Material.EMERALD, 3 + o.r.nextInt(5)));
        if (o.chance(0.25)) carga.add(Raro.MAPA_RASGADO.criar(1));
        org.bukkit.inventory.Inventory temp = org.bukkit.Bukkit.createInventory(null, 27);
        base.encher(temp, LootTables.IGLOO_CHEST, o, treno, carga);
        treno.getWorld().spawn(treno.getLocation().add(0.5, 0, 0.5), SpruceChestBoat.class, b -> {
            b.customName(Component.text("Trenó da expedição"));
            b.getInventory().setContents(temp.getContents());
        });
        // Neve fina por cima.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (o.bloco(dx, 1, dz).isEmpty() && o.bloco(dx, 0, dz).getType().isOccluding() && o.chance(0.55)) {
                    o.por(dx, 1, dz, "snow[layers=" + (1 + o.r.nextInt(2)) + "]");
                }
            }
        }
    }

    private static ItemStack couro(Material m, int cor) {
        ItemStack i = new ItemStack(m);
        i.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(Color.fromRGB(cor)));
        return i;
    }

    // =====================================================================
    //  Vila Saqueada
    // =====================================================================

    void vila(Obra o, String pista) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) if (o.chance(0.3)) o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 3, Material.DIRT, 1));
        }
        // Caminhos até as portas.
        for (int a = -4; a <= 4; a++) {
            o.por(a, 0, 0, Material.DIRT_PATH);
            o.por(0, 0, a, Material.DIRT_PATH);
        }
        // Poço quebrado no meio, com o sino caído.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    o.por(0, 0, 0, Material.WATER);
                    o.por(0, -1, 0, Material.WATER);
                    o.por(0, -2, 0, Material.COBBLESTONE);
                } else {
                    o.por(dx, 0, dz, Material.COBBLESTONE);
                    if (!(dx == 1 && dz == 1) && o.chance(0.75)) o.por(dx, 1, dz, "cobblestone_wall");
                }
            }
        }
        o.por(1, 1, 2, "bell[attachment=floor,facing=east]");
        String sobrevivente = SOBREVIVENTES[o.r.nextInt(SOBREVIVENTES.length)];
        casa(o.parte(-4, -4, 0), true, sobrevivente, pista);
        casa(o.parte(4, -4, 0), false, sobrevivente, pista);
        casa(o.parte(-4, 4, 2), false, sobrevivente, pista);
        // Horta pisoteada.
        for (int dx = 2; dx <= 6; dx++) {
            for (int dz = 2; dz <= 6; dz++) {
                boolean cerca = dx == 2 || dx == 6 || dz == 2 || dz == 6;
                if (cerca) {
                    if (o.chance(0.55)) o.por(dx, 1, dz, Material.OAK_FENCE);
                } else {
                    o.por(dx, 0, dz, Material.COARSE_DIRT);
                    if (o.chance(0.4)) o.por(dx, 1, dz, Material.DEAD_BUSH);
                }
            }
        }
        o.por(4, 1, 2, Material.AIR);
        o.por(6, 1, 6, Material.COMPOSTER);
    }

    /** Casa queimada: frente (porta) para +z. */
    private void casa(Obra c, boolean diario, String sobrevivente, String pista) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.por(dx, 0, dz, Material.COBBLESTONE);
                boolean parede = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean canto = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                for (int dy = 1; dy <= 3; dy++) {
                    if (!parede) { c.por(dx, dy, dz, Material.AIR); continue; }
                    double falta = dy == 1 ? 0.08 : dy == 2 ? 0.25 : 0.5;
                    if (c.chance(falta) && !canto) { c.por(dx, dy, dz, Material.AIR); continue; }
                    c.por(dx, dy, dz, canto ? Material.STRIPPED_DARK_OAK_LOG
                            : c.um(Material.DARK_OAK_PLANKS, 3, Material.COBBLESTONE, 2, Material.BLACKSTONE, 1, Material.COAL_BLOCK, 1));
                }
                if (c.chance(0.2)) c.por(dx, 4, dz, "dark_oak_slab[type=bottom]");
            }
        }
        c.por(0, 1, 2, Material.AIR);
        c.por(0, 2, 2, Material.AIR);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) if (c.chance(0.4)) c.por(dx, 1, dz, c.um(Material.GRAY_CARPET, 1, Material.LIGHT_GRAY_CARPET, 1));
        }
        if (diario) {
            c.por(-1, 1, -1, "lectern[facing=east]");
            String quem = pista != null ? "Os bandidos do acampamento " + pista + "." : "Não vimos de onde vieram. Só ouvimos os cavalos.";
            porLivro(c.bloco(-1, 1, -1), livro("Diário de " + sobrevivente, sobrevivente,
                    "Vieram à noite. Queimaram o celeiro primeiro, depois as casas.\n\n" + quem,
                    "Os que ficaram... não são mais os mesmos. Andam pela vila com os olhos vazios.\n\nDizem que uma maçã dourada e uma poção de fraqueza trazem alguém de volta.",
                    "Escondi o que sobrou no baú. Se você for gente boa, cuide da vila.\n\n— " + sobrevivente));
            c.por(1, 1, -1, "chest[facing=west]");
            List<ItemStack> extras = new ArrayList<>();
            extras.add(new ItemStack(Material.EMERALD, 3 + c.r.nextInt(6)));
            extras.add(new ItemStack(Material.GOLDEN_APPLE));
            if (c.chance(0.3)) extras.add(Album.carta(Carta.values()[c.r.nextInt(Carta.values().length)], false));
            base.bau(c.bloco(1, 1, -1), LootTables.VILLAGE_PLAINS_HOUSE, c, extras);
        } else {
            c.por(-1, 1, -1, "black_bed[facing=north,part=head]");
            c.por(-1, 1, 0, "black_bed[facing=north,part=foot]");
            if (c.chance(0.6)) c.por(1, 1, 0, "campfire[lit=true]");
        }
    }

    // =====================================================================
    //  Torre do Mago em Ruínas
    // =====================================================================

    void torreMago(Obra o) {
        String mago = MAGOS[o.r.nextInt(MAGOS.length)];
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) if (octo(dx, dz, 3)) o.por(dx, 0, dz, o.um(Material.POLISHED_DEEPSLATE, 2, Material.COBBLED_DEEPSLATE, 1));
        }
        for (int dy = 1; dy <= 14; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (!octo(dx, dz, 2)) continue;
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                        o.por(dx, dy, dz, dy == 5 || dy == 10 ? Material.AMETHYST_BLOCK
                                : o.um(Material.DEEPSLATE_BRICKS, 5, Material.CRACKED_DEEPSLATE_BRICKS, 2, Material.POLISHED_DEEPSLATE, 1));
                    } else {
                        o.por(dx, dy, dz, Material.AIR);
                    }
                }
            }
        }
        o.por(0, 1, 2, Material.AIR);
        o.por(0, 2, 2, Material.AIR);
        for (int dy : new int[]{3, 7, 8, 12}) {
            o.por(2, dy, 0, Material.PURPLE_STAINED_GLASS_PANE);
            o.por(-2, dy, 0, Material.PURPLE_STAINED_GLASS_PANE);
        }
        for (int piso : new int[]{5, 10}) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!(dx == 0 && dz == -1)) o.por(dx, piso, dz, Material.DARK_OAK_PLANKS);
        }
        for (int dy = 1; dy <= 14; dy++) o.por(0, dy, -1, "ladder[facing=south]");
        // Teto e o telhado cônico de púrpura.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (octo(dx, dz, 2)) o.por(dx, 15, dz, Material.DARK_OAK_PLANKS);
        for (int k = 0; k <= 3; k++) {
            int a = 3 - k, dy = 15 + k;
            if (a == 0) { o.por(0, dy, 0, Material.AMETHYST_BLOCK); break; }
            for (int dx = -a; dx <= a; dx++) {
                for (int dz = -a; dz <= a; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != a) continue;
                    if (Math.abs(dx) == a && Math.abs(dz) == a) {
                        if (a < 3) o.por(dx, dy, dz, Material.PURPUR_BLOCK);
                        continue;
                    }
                    o.por(dx, dy, dz, "purpur_stairs[facing=" + lado(dx, dz, a) + "]");
                }
            }
        }
        o.por(0, 19, 0, "end_rod[facing=up]");
        // Térreo: estantes, caldeirão.
        o.por(-1, 1, -1, Material.BOOKSHELF);
        o.por(1, 1, -1, Material.BOOKSHELF);
        o.por(-1, 2, -1, Material.BOOKSHELF);
        o.por(-1, 1, 1, Material.CAULDRON);
        o.por(1, 1, 1, Material.POTTED_ALLIUM);
        // 1º andar: a mesa de encantamento e as anotações.
        o.por(1, 6, 1, Material.ENCHANTING_TABLE);
        o.por(-1, 6, 1, "lectern[facing=east]");
        porLivro(o.bloco(-1, 6, 1), anotacoes(o.r, mago));
        o.por(-1, 6, -1, Material.BOOKSHELF);
        o.por(1, 6, -1, Material.BOOKSHELF);
        o.por(-1, 7, -1, Material.BOOKSHELF);
        o.por(1, 7, -1, Material.CHISELED_BOOKSHELF);
        // 2º andar: o baú do mago (o aprendiz está aqui).
        o.por(1, 11, 1, "chest[facing=west]");
        List<ItemStack> extras = new ArrayList<>();
        extras.add(Reagente.PO_ARCANO.criar(2 + o.r.nextInt(3)));
        if (o.chance(0.4)) extras.add(Reagente.CRISTAL_DE_MANA.criar(1 + o.r.nextInt(2)));
        if (o.chance(0.12)) extras.add(ItensMagicos.tomoAleatorio());
        extras.add(new ItemStack(Material.LAPIS_LAZULI, 4 + o.r.nextInt(8)));
        base.bau(o.bloco(1, 11, 1), LootTables.STRONGHOLD_LIBRARY, o, extras);
        o.por(-1, 11, 1, "amethyst_cluster[facing=up]");
        if (o.chance(0.6)) o.por(1, 14, -1, Material.COBWEB);
        if (o.chance(0.6)) o.por(-1, 14, 1, Material.COBWEB);
        // Pedras caídas em volta.
        for (int i = 0; i < 5; i++) {
            double ang = o.r.nextDouble() * Math.PI * 2;
            int dx = (int) Math.round(Math.cos(ang) * 3.6), dz = (int) Math.round(Math.sin(ang) * 3.6);
            if (o.bloco(dx, 1, dz).isEmpty()) o.por(dx, 1, dz, o.um(Material.COBBLED_DEEPSLATE, 2, Material.DEEPSLATE_BRICKS, 1));
        }
    }

    /** Anotações do mago, que revelam uma magia secreta de verdade. */
    private static ItemStack anotacoes(Random r, String mago) {
        List<Receita> boas = new ArrayList<>();
        for (Receita x : Receita.values()) if (x.forma().criavel()) boas.add(x);
        Receita rec = boas.get(r.nextInt(boas.size()));
        StringJoiner ess = new StringJoiner(" + ");
        for (Essencia e : rec.essencias()) ess.add(e.nome());
        return livro("Anotações de " + mago, mago,
                "Trinta anos estudando a magia nesta torre. Escrevo para não esquecer.",
                "Uma combinação que nunca contei a ninguém:\n\n" + ess + "\nem forma de " + rec.forma().nome() + ".\n\nChamei de " + rec.nome() + ".",
                "Meu aprendiz leu os livros proibidos. Ficou estranho. Some e aparece.\n\nSe ele ainda estiver lá em cima, não deixe que fale.");
    }

    // =====================================================================
    //  Círculo de Pedras Antigas
    // =====================================================================

    void circulo(Obra o) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int r2 = dx * dx + dz * dz;
                if (r2 >= 7 && r2 <= 12 && o.chance(0.6)) o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 2, Material.MOSSY_COBBLESTONE, 1, Material.DIRT_PATH, 1));
            }
        }
        o.por(0, 0, 0, Material.STONE_BRICKS);
        o.por(0, 1, 0, Material.CHISELED_STONE_BRICKS);
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) o.por(d[0], 1, d[1], "smooth_stone_slab[type=bottom]");
        int caida = o.r.nextInt(PEDRAS.length);
        for (int i = 0; i < PEDRAS.length; i++) {
            int x = PEDRAS[i][0], z = PEDRAS[i][1];
            Material m = o.um(Material.STONE, 3, Material.ANDESITE, 2, Material.MOSSY_COBBLESTONE, 2, Material.TUFF, 1);
            if (i == caida) {
                // Pedra caída, deitada ao lado de onde ficava.
                o.por(x, 1, z, m);
                int tx = z == 0 ? 0 : Integer.signum(z), tz = x == 0 ? 0 : -Integer.signum(x);
                if (tx == 0 && tz == 0) tz = 1;
                o.por(x + tx, 1, z + tz, m);
                o.por(x + 2 * tx, 1, z + 2 * tz, m);
                continue;
            }
            int h = 2 + o.r.nextInt(3);
            for (int dy = 1; dy <= h; dy++) o.por(x, dy, z, dy == h && h >= 3 ? Material.CHISELED_STONE_BRICKS : m);
            if (o.chance(0.4)) o.por(x, 0, z, Material.MOSSY_COBBLESTONE);
        }
    }

    // =====================================================================
    //  Forja dos Anões (meio enterrada)
    // =====================================================================

    void forjaAnoes(Obra o) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 4; dz++) {
                boolean parede = Math.abs(dx) == 5 || dz == -5 || dz == 4;
                boolean canto = Math.abs(dx) == 5 && (dz == -5 || dz == 4);
                o.por(dx, -3, dz, Material.DEEPSLATE);
                o.por(dx, -2, dz, parede ? Material.DEEPSLATE_BRICKS
                        : o.um(Material.DEEPSLATE_TILES, 3, Material.POLISHED_DEEPSLATE, 2, Material.CRACKED_DEEPSLATE_TILES, 1));
                for (int dy = -1; dy <= 1; dy++) {
                    if (!parede) { o.por(dx, dy, dz, Material.AIR); continue; }
                    o.por(dx, dy, dz, canto ? Material.CHISELED_DEEPSLATE
                            : o.um(Material.DEEPSLATE_BRICKS, 4, Material.CRACKED_DEEPSLATE_BRICKS, 2, Material.POLISHED_BLACKSTONE_BRICKS, 1));
                }
                o.por(dx, 2, dz, Material.DEEPSLATE_TILES);
                if (parede) o.por(dx, 3, dz, "deepslate_tile_slab[type=bottom]");
            }
        }
        // Entrada com degraus descendo do chão.
        o.por(0, -1, 4, Material.AIR);
        o.por(0, 0, 4, Material.AIR);
        o.por(0, 1, 4, Material.CHISELED_DEEPSLATE);
        o.por(0, -2, 5, Material.DEEPSLATE_BRICKS);
        o.por(0, -1, 5, "deepslate_brick_stairs[facing=south]");
        o.por(0, 0, 5, Material.AIR);
        o.por(0, 1, 5, Material.AIR);
        for (int lado : new int[]{-1, 1}) for (int dy = -2; dy <= 0; dy++) o.por(lado, dy, 5, Material.DEEPSLATE_BRICKS);
        o.por(0, 0, 6, "deepslate_brick_stairs[facing=south]");
        o.por(0, 1, 6, Material.AIR);
        // Poço de lava (fechado em volta e coberto com grade).
        for (int dx = -1; dx <= 1; dx++) {
            o.por(dx, -3, -4, Material.DEEPSLATE);
            o.por(dx, -2, -4, Material.LAVA);
            o.por(dx, -1, -4, Material.IRON_BARS);
        }
        o.por(-3, -1, -4, "blast_furnace[facing=south]");
        o.por(3, -1, -4, "blast_furnace[facing=south]");
        o.por(0, -1, -1, "anvil[facing=east]");
        o.por(-4, -1, 0, Material.SMITHING_TABLE);
        o.por(4, -1, 0, "grindstone[face=floor,facing=north]");
        o.por(-4, -1, -3, "barrel[facing=up]");
        o.por(-4, -1, 2, "barrel[facing=up]");
        for (int lado : new int[]{-3, 3}) {
            o.por(lado, 1, 0, "iron_chain[axis=y]");
            o.por(lado, 0, 0, "lantern[hanging=true]");
        }
        // O baú da forja.
        o.por(4, -1, -3, "chest[facing=west]");
        List<ItemStack> extras = new ArrayList<>();
        extras.add(Raro.FRAGMENTO_DE_FORJA.criar(1 + o.r.nextInt(3)));
        extras.add(new ItemStack(Material.IRON_INGOT, 4 + o.r.nextInt(8)));
        extras.add(new ItemStack(Material.GOLD_INGOT, 2 + o.r.nextInt(5)));
        if (o.chance(0.15)) extras.add(Mitrilo.lingote(1));
        if (o.chance(0.1)) extras.add(new ItemStack(Material.NETHERITE_SCRAP));
        if (o.chance(0.3)) extras.add(new ItemStack(Material.DIAMOND, 1 + o.r.nextInt(2)));
        base.bau(o.bloco(4, -1, -3), LootTables.VILLAGE_WEAPONSMITH, o, extras);
        // Chaminé que ainda solta fumaça.
        for (int dy = 3; dy <= 4; dy++) o.por(3, dy, -4, Material.DEEPSLATE_BRICKS);
        o.por(3, 5, -4, Material.HAY_BLOCK);
        o.por(3, 6, -4, "campfire[lit=true,signal_fire=true]");
    }

    // =====================================================================
    //  Fortim em Ruínas
    // =====================================================================

    void fortim(Obra o) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) if (o.chance(0.35)) o.por(dx, 0, dz, o.um(Material.COARSE_DIRT, 2, Material.GRAVEL, 1, Material.COBBLESTONE, 1));
        }
        // Muralha com ameias (partes caídas).
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 6) continue;
                if (dz == 6 && Math.abs(dx) <= 1) continue;
                int altura = o.chance(0.2) ? 1 + o.r.nextInt(2) : 3;
                for (int dy = 1; dy <= altura; dy++) o.por(dx, dy, dz, o.um(Material.STONE_BRICKS, 3, Material.COBBLESTONE, 3, Material.MOSSY_STONE_BRICKS, 2, Material.CRACKED_STONE_BRICKS, 1));
                if (altura == 3 && (dx + dz) % 2 == 0) o.por(dx, 4, dz, Material.STONE_BRICKS);
            }
        }
        for (int dx = -1; dx <= 1; dx++) o.por(dx, 3, 6, Material.STONE_BRICKS);
        // Torres nos cantos.
        for (int cx : new int[]{-6, 6}) {
            for (int cz : new int[]{-6, 6}) {
                for (int dx = cx - 1; dx <= cx + 1; dx++) {
                    for (int dz = cz - 1; dz <= cz + 1; dz++) {
                        for (int dy = 1; dy <= 5; dy++) o.por(dx, dy, dz, o.um(Material.STONE_BRICKS, 3, Material.MOSSY_STONE_BRICKS, 1, Material.CRACKED_STONE_BRICKS, 1));
                        boolean borda = Math.abs(dx - cx) == 1 || Math.abs(dz - cz) == 1;
                        if (borda && (dx + dz) % 2 == 0 && !o.chance(0.2)) o.por(dx, 6, dz, Material.STONE_BRICKS);
                    }
                }
            }
        }
        // Torre de menagem (x -2..2, z -5..-1), dois andares.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -5; dz <= -1; dz++) {
                boolean parede = Math.abs(dx) == 2 || dz == -5 || dz == -1;
                o.por(dx, 0, dz, Material.COBBLESTONE);
                for (int dy = 1; dy <= 7; dy++) o.por(dx, dy, dz, parede ? pedraFortim(o) : Material.AIR);
                if (!parede && !(dx == 0 && dz == -4)) o.por(dx, 4, dz, Material.SPRUCE_PLANKS);
                o.por(dx, 8, dz, Material.STONE_BRICKS);
                if (parede && (dx + dz) % 2 == 0) o.por(dx, 9, dz, Material.STONE_BRICKS);
            }
        }
        o.por(0, 1, -1, Material.AIR);
        o.por(0, 2, -1, Material.AIR);
        for (int dy = 1; dy <= 7; dy++) o.por(0, dy, -4, "ladder[facing=south]");
        for (int lado : new int[]{-2, 2}) {
            o.por(lado, 2, -3, Material.AIR);
            o.por(lado, 6, -3, Material.AIR);
        }
        o.por(-1, 1, -2, "barrel[facing=up]");
        o.por(1, 5, -2, "chest[facing=west]");
        List<ItemStack> extras = new ArrayList<>();
        extras.add(new ItemStack(Material.EMERALD, 4 + o.r.nextInt(8)));
        if (o.chance(0.25)) extras.add(Raro.PEDRA_DE_PROTECAO.criar(1));
        if (o.chance(0.4)) extras.add(Raro.FRAGMENTO_DE_FORJA.criar(1));
        if (o.chance(0.3)) extras.add(Raro.PAGINA_DE_LENDA.criar(1));
        base.bau(o.bloco(1, 5, -2), LootTables.SIMPLE_DUNGEON, o, extras);
        // Pátio: treino, barris e a fogueira apagada.
        o.por(-4, 1, 3, Material.HAY_BLOCK);
        o.por(-4, 2, 3, Material.TARGET);
        o.por(4, 1, 4, "barrel[facing=up]");
        o.por(4, 1, 3, "barrel[facing=up]");
        o.por(3, 1, 0, "campfire[lit=false]");
    }

    private static Material pedraFortim(Obra o) {
        return o.um(Material.STONE_BRICKS, 5, Material.MOSSY_STONE_BRICKS, 2, Material.CRACKED_STONE_BRICKS, 2);
    }
}
