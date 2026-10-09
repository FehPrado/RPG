package br.rpgatributos.colonia;

import br.rpgatributos.alquimia.Ingrediente;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * O Construtor da colônia: acha um lugar plano e livre no território e ergue, bloco a bloco,
 * uma casa 7×7 pronta para morar (piso de pedra, paredes de tábua com pilares de tronco,
 * telhado de escadas, janelas, porta, 2 camas e luz), tirando o material do depósito.
 */
final class Obras {

    /** Uma casa em construção. */
    static final class Obra {
        final String mundo;
        final int x, y, z, rot;
        int passo;
        Material madeira, pedra;
        boolean limpa;
        /** Tábuas que sobraram de um tronco serrado e meias-lajes que sobraram de uma tábua. */
        int sobraTabuas, sobraLajes;
        /** O que faltou no último turno (não é salvo). */
        String falta;

        Obra(String mundo, int x, int y, int z, int rot) {
            this.mundo = mundo;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rot = rot;
        }

        int total() { return PLANTA.size(); }

        Location centro(World w) { return new Location(w, x + 3.5, y + 1, z + 3.5); }

        String salvar() {
            return mundo + "," + x + "," + y + "," + z + "," + rot + "," + passo + "," + madeira.name() + "," + pedra.name() + ","
                    + limpa + "," + sobraTabuas + "," + sobraLajes;
        }

        static Obra ler(String s) {
            String[] p = s.split(",");
            if (p.length < 11) return null;
            try {
                Obra o = new Obra(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                o.passo = Integer.parseInt(p[5]);
                o.madeira = Material.matchMaterial(p[6]);
                o.pedra = Material.matchMaterial(p[7]);
                o.limpa = Boolean.parseBoolean(p[8]);
                o.sobraTabuas = Integer.parseInt(p[9]);
                o.sobraLajes = Integer.parseInt(p[10]);
                if (o.madeira == null) o.madeira = Material.OAK_PLANKS;
                if (o.pedra == null) o.pedra = Material.COBBLESTONE;
                return o;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }

    private enum Peca { PISO, PILAR, PAREDE, TELHADO, ESCADA, LAJE, VIDRO, PORTA, CAMA, LUZ }

    /** Um bloco da planta, em coordenadas da casa (0 a 6; a frente é z = 0). */
    private record Bloco(int lx, int ly, int lz, Peca peca, BlockFace face) { }

    static final int LADO = 7;
    private static final List<Bloco> PLANTA = planta();

    /** Chão natural onde dá para construir. */
    private static final Set<Material> CHAO = Set.of(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
            Material.ROOTED_DIRT, Material.MYCELIUM, Material.MOSS_BLOCK, Material.PALE_MOSS_BLOCK, Material.SAND, Material.RED_SAND,
            Material.GRAVEL, Material.STONE, Material.SNOW_BLOCK, Material.MUD, Material.CLAY, Material.TERRACOTTA, Material.DEEPSLATE,
            Material.ANDESITE, Material.DIORITE, Material.GRANITE, Material.TUFF, Material.SANDSTONE, Material.RED_SANDSTONE);
    private static final List<Material> PEDRAS = List.of(Material.COBBLESTONE, Material.STONE, Material.STONE_BRICKS,
            Material.MOSSY_COBBLESTONE, Material.COBBLED_DEEPSLATE);

    private final Colonias colonias;

    Obras(Colonias colonias) {
        this.colonias = colonias;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  A planta
    // =====================================================================

    private static List<Bloco> planta() {
        List<Bloco> l = new ArrayList<>();
        int max = LADO - 1, meio = LADO / 2;
        // Piso de pedra.
        for (int lz = 0; lz < LADO; lz++) for (int lx = 0; lx < LADO; lx++) l.add(new Bloco(lx, 0, lz, Peca.PISO, null));
        // Paredes, fileira por fileira (pilares de tronco nos cantos; vãos da porta e das janelas ficam para o fim).
        for (int ly = 1; ly <= 3; ly++) {
            for (int lx = 0; lx < LADO; lx++) for (int lz = 0; lz < LADO; lz++) {
                if (lx != 0 && lx != max && lz != 0 && lz != max) continue;
                boolean canto = (lx == 0 || lx == max) && (lz == 0 || lz == max);
                if (lx == meio && lz == 0 && ly <= 2) continue; // porta
                if (ly == 2 && ((lz == meio && (lx == 0 || lx == max)) || (lx == meio && lz == max))) continue; // janelas
                l.add(new Bloco(lx, ly, lz, canto ? Peca.PILAR : Peca.PAREDE, null));
            }
        }
        // Telhado de duas águas: escadas subindo dos lados, oitões de tábua na frente e no fundo, cumeeira com meia-laje.
        for (int k = 0; k < 3; k++) {
            int ly = 4 + k;
            for (int lz = 0; lz < LADO; lz++) {
                l.add(new Bloco(k, ly, lz, Peca.ESCADA, BlockFace.EAST));
                l.add(new Bloco(max - k, ly, lz, Peca.ESCADA, BlockFace.WEST));
            }
            for (int lx = k + 1; lx <= max - k - 1; lx++) {
                if (k == 2) continue; // a cumeeira vem logo abaixo
                l.add(new Bloco(lx, ly, 0, Peca.TELHADO, null));
                l.add(new Bloco(lx, ly, max, Peca.TELHADO, null));
            }
        }
        for (int lz = 0; lz < LADO; lz++) l.add(new Bloco(meio, 6, lz, Peca.TELHADO, null));
        for (int lz = 0; lz < LADO; lz++) l.add(new Bloco(meio, 7, lz, Peca.LAJE, null));
        // Acabamento: janelas (a face é o sentido da parede), porta, 2 camas e a luz.
        l.add(new Bloco(0, 2, meio, Peca.VIDRO, BlockFace.SOUTH));
        l.add(new Bloco(max, 2, meio, Peca.VIDRO, BlockFace.SOUTH));
        l.add(new Bloco(meio, 2, max, Peca.VIDRO, BlockFace.EAST));
        l.add(new Bloco(meio, 1, 0, Peca.PORTA, BlockFace.SOUTH));
        l.add(new Bloco(1, 1, max - 2, Peca.CAMA, BlockFace.SOUTH));
        l.add(new Bloco(2, 1, max - 2, Peca.CAMA, BlockFace.SOUTH));
        l.add(new Bloco(max - 1, 1, max - 1, Peca.LUZ, null));
        return Collections.unmodifiableList(l);
    }

    /** Gira uma direção 90° no sentido horário, {@code vezes} vezes. */
    private static BlockFace girar(BlockFace f, int vezes) {
        for (int i = 0; i < vezes; i++) {
            f = switch (f) {
                case NORTH -> BlockFace.EAST;
                case EAST -> BlockFace.SOUTH;
                case SOUTH -> BlockFace.WEST;
                case WEST -> BlockFace.NORTH;
                default -> f;
            };
        }
        return f;
    }

    private static Block bloco(World w, Obra o, int lx, int ly, int lz) {
        int m = LADO - 1;
        int wx, wz;
        switch (o.rot) {
            case 1 -> { wx = o.x + m - lz; wz = o.z + lx; }
            case 2 -> { wx = o.x + m - lx; wz = o.z + m - lz; }
            case 3 -> { wx = o.x + lz; wz = o.z + m - lx; }
            default -> { wx = o.x + lx; wz = o.z + lz; }
        }
        return w.getBlockAt(wx, o.y + ly, wz);
    }

    // =====================================================================
    //  Achar um lugar
    // =====================================================================

    /** Os chunks de um quadrado de LADO + 2 blocos a partir de (x, z) estão carregados? (não carrega nenhum) */
    private static boolean carregado(World w, int x, int z) {
        for (int cx = x >> 4; cx <= (x + LADO + 1) >> 4; cx++) {
            for (int cz = z >> 4; cz <= (z + LADO + 1) >> 4; cz++) if (!w.isChunkLoaded(cx, cz)) return false;
        }
        return true;
    }

    private static boolean livre(Block b) {
        return b.getType().isAir() || (b.isReplaceable() && !b.isLiquid());
    }

    /** Tenta alguns lugares ao acaso no território (perto da Prefeitura primeiro). */
    Obra procurarLugar(Colonia c, List<long[]> chunks) {
        World w = c.world();
        if (w == null || chunks.isEmpty()) return null;
        List<long[]> perto = new ArrayList<>(), carregados = new ArrayList<>();
        for (long[] ch : chunks) {
            if (!w.isChunkLoaded((int) ch[0], (int) ch[1])) continue;
            carregados.add(ch);
            if (Math.abs(ch[0] - (c.x >> 4)) <= 3 && Math.abs(ch[1] - (c.z >> 4)) <= 3) perto.add(ch);
        }
        if (carregados.isEmpty()) return null;
        for (int tentativa = 0; tentativa < 30; tentativa++) {
            List<long[]> de = !perto.isEmpty() && tentativa < 20 ? perto : carregados;
            long[] ch = de.get(rnd().nextInt(de.size()));
            int x0 = ((int) ch[0] << 4) + rnd().nextInt(16) - 3, z0 = ((int) ch[1] << 4) + rnd().nextInt(16) - 3;
            Obra o = testar(c, w, x0, z0);
            if (o != null) return o;
        }
        return null;
    }

    private Obra testar(Colonia c, World w, int x0, int z0) {
        int fim = LADO - 1;
        // Longe da Prefeitura (o sino fica livre).
        if (x0 - 2 <= c.x && c.x <= x0 + fim + 2 && z0 - 2 <= c.z && c.z <= z0 + fim + 2) return null;
        if (!carregado(w, x0 - 1, z0 - 1)) return null;
        int y0 = w.getHighestBlockYAt(x0 + 3, z0 + 3, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (!CHAO.contains(w.getBlockAt(x0 + 3, y0, z0 + 3).getType())) return null;
        for (int[] canto : new int[][]{{x0, z0}, {x0 + fim, z0}, {x0, z0 + fim}, {x0 + fim, z0 + fim}}) {
            if (!colonias.naColonia(c, new Location(w, canto[0], y0, canto[1]))) return null;
        }
        // Chão firme e quase plano (no máximo 1 bloco abaixo) em todas as casas do piso.
        for (int dx = 0; dx <= fim; dx++) for (int dz = 0; dz <= fim; dz++) {
            int top = w.getHighestBlockYAt(x0 + dx, z0 + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (top != y0 && top != y0 - 1) return null;
            if (!CHAO.contains(w.getBlockAt(x0 + dx, top, z0 + dz).getType())) return null;
        }
        // Espaço livre para a casa e o telhado, com 1 bloco de folga em volta.
        for (int dx = -1; dx <= fim + 1; dx++) for (int dz = -1; dz <= fim + 1; dz++) {
            boolean borda = dx < 0 || dz < 0 || dx > fim || dz > fim;
            for (int dy = 1; dy <= (borda ? 3 : 8); dy++) {
                if (!livre(w.getBlockAt(x0 + dx, y0 + dy, z0 + dz))) return null;
            }
        }
        Obra o = new Obra(w.getName(), x0, y0, z0, rnd().nextInt(4));
        o.madeira = Material.OAK_PLANKS;
        o.pedra = Material.COBBLESTONE;
        return o;
    }

    // =====================================================================
    //  Construir
    // =====================================================================

    /** Coloca até {@code quantos} blocos. @return quantos colocou (o que faltou fica em {@code o.falta}). */
    int construir(Colonia c, Obra o, int quantos) {
        o.falta = null;
        World w = c.world();
        if (w == null || !w.getName().equals(o.mundo)) return 0;
        if (!carregado(w, o.x - 1, o.z - 1)) return 0;
        if (!o.limpa) {
            // Tira a grama alta, flores e neve do lugar.
            for (int lx = 0; lx < LADO; lx++) for (int lz = 0; lz < LADO; lz++) for (int ly = 1; ly <= 8; ly++) {
                Block b = bloco(w, o, lx, ly, lz);
                if (!b.getType().isAir() && livre(b)) b.setType(Material.AIR, false);
            }
            escolherMateriais(c, o);
            o.limpa = true;
        }
        int feitos = 0;
        Block ultimo = null;
        while (feitos < quantos && o.passo < PLANTA.size()) {
            Bloco pb = PLANTA.get(o.passo);
            Block b = bloco(w, o, pb.lx(), pb.ly(), pb.lz());
            boolean pode = pb.peca() == Peca.PISO ? livre(b) || CHAO.contains(b.getType()) : livre(b);
            if (pb.peca() == Peca.PORTA) pode &= livre(b.getRelative(BlockFace.UP));
            if (pb.peca() == Peca.CAMA) pode &= livre(b.getRelative(girar(pb.face(), o.rot)));
            if (!pode) { o.passo++; continue; } // alguém pôs um bloco ali: o Construtor respeita
            if (!colocar(c, o, pb, b)) return feitos;
            o.passo++;
            feitos++;
            ultimo = b;
        }
        if (ultimo != null) {
            BlockData bd = ultimo.getBlockData();
            w.spawnParticle(Particle.BLOCK, ultimo.getLocation().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, bd);
            w.playSound(ultimo.getLocation(), bd.getSoundGroup().getPlaceSound(), 0.8f, 0.9f);
        }
        return feitos;
    }

    boolean pronta(Obra o) { return o.passo >= PLANTA.size(); }

    /** A madeira e a pedra da casa: as que o depósito tem mais. */
    private void escolherMateriais(Colonia c, Obra o) {
        Material m = maisComum(c, s -> s.getType().name().endsWith("_PLANKS"));
        if (m == null) {
            Material tronco = maisComum(c, s -> tabua(s.getType()) != null);
            if (tronco != null) m = tabua(tronco);
        }
        if (m != null) o.madeira = m;
        Material p = maisComum(c, s -> PEDRAS.contains(s.getType()));
        if (p != null) o.pedra = p;
    }

    private Material maisComum(Colonia c, Predicate<ItemStack> teste) {
        java.util.Map<Material, Integer> n = colonias.contarPorTipo(c, s -> teste.test(s) && Ingrediente.simples(s));
        Material melhor = null;
        int max = 0;
        for (var e : n.entrySet()) if (e.getValue() > max) { max = e.getValue(); melhor = e.getKey(); }
        return melhor;
    }

    /** Tábua que sai de um tronco (null se não for tronco). */
    private static Material tabua(Material tronco) {
        String n = tronco.name();
        if (n.startsWith("STRIPPED_")) n = n.substring("STRIPPED_".length());
        for (String fim : new String[]{"_LOG", "_WOOD", "_STEM", "_HYPHAE"}) {
            if (n.endsWith(fim)) return Material.matchMaterial(n.substring(0, n.length() - fim.length()) + "_PLANKS");
        }
        return null;
    }

    private static Material daMadeira(Material tabua, String fim) {
        String base = tabua.name().substring(0, tabua.name().length() - "_PLANKS".length());
        Material m = Material.matchMaterial(base + fim);
        if (m == null && fim.equals("_LOG")) m = Material.matchMaterial(base + "_STEM");
        return m;
    }

    private boolean tirar(Colonia c, Predicate<ItemStack> teste, int qtd) {
        Predicate<ItemStack> t = s -> teste.test(s) && Ingrediente.simples(s);
        if (colonias.contar(c, t) < qtd) return false;
        colonias.tirar(c, t, qtd);
        return true;
    }

    private boolean tirarTipo(Colonia c, Material m) {
        return m != null && tirar(c, s -> s.getType() == m, 1);
    }

    /** Uma tábua da madeira da casa (ou serra um tronco; se acabou, passa para outra madeira). */
    private boolean tabua(Colonia c, Obra o) {
        if (o.sobraTabuas > 0) { o.sobraTabuas--; return true; }
        for (int volta = 0; volta < 2; volta++) {
            if (tirarTipo(c, o.madeira)) return true;
            Material madeira = o.madeira;
            if (tirar(c, s -> madeira.equals(tabua(s.getType())), 1)) { o.sobraTabuas += 3; return true; }
            Material outra = maisComum(c, s -> s.getType().name().endsWith("_PLANKS") || tabua(s.getType()) != null);
            if (outra == null) break;
            o.madeira = outra.name().endsWith("_PLANKS") ? outra : tabua(outra);
        }
        o.falta = "madeira (tábuas ou troncos)";
        return false;
    }

    private boolean colocar(Colonia c, Obra o, Bloco pb, Block b) {
        BlockFace face = pb.face() == null ? null : girar(pb.face(), o.rot);
        switch (pb.peca()) {
            case PISO -> {
                if (!tirarTipo(c, o.pedra)) {
                    Material outra = maisComum(c, s -> PEDRAS.contains(s.getType()));
                    if (outra == null) { o.falta = "pedra (pedregulho, pedra ou tijolos de pedra)"; return false; }
                    o.pedra = outra;
                    tirarTipo(c, outra);
                }
                b.setType(o.pedra, false);
            }
            case PILAR -> {
                Material tronco = daMadeira(o.madeira, "_LOG");
                if (tirarTipo(c, tronco)) b.setType(tronco, false);
                else {
                    if (!tabua(c, o)) return false;
                    b.setType(o.madeira, false);
                }
            }
            case PAREDE, TELHADO -> {
                if (!tabua(c, o)) return false;
                b.setType(o.madeira, false);
            }
            case ESCADA -> {
                Material escada = daMadeira(o.madeira, "_STAIRS");
                if (!tirarTipo(c, escada) && !tabua(c, o)) return false;
                if (escada == null) { b.setType(o.madeira, false); break; }
                Stairs st = (Stairs) escada.createBlockData();
                st.setFacing(face);
                st.setHalf(Bisected.Half.BOTTOM);
                b.setBlockData(st, false);
            }
            case LAJE -> {
                Material laje = daMadeira(o.madeira, "_SLAB");
                if (o.sobraLajes > 0) o.sobraLajes--;
                else if (!tirarTipo(c, laje)) {
                    if (!tabua(c, o)) return false;
                    o.sobraLajes++;
                }
                if (laje == null) { b.setType(o.madeira, false); break; }
                Slab sl = (Slab) laje.createBlockData();
                sl.setType(Slab.Type.BOTTOM);
                b.setBlockData(sl, false);
            }
            case VIDRO -> {
                if (tirarTipo(c, Material.GLASS_PANE)) {
                    MultipleFacing mf = (MultipleFacing) Material.GLASS_PANE.createBlockData();
                    mf.setFace(face, true);
                    mf.setFace(face.getOppositeFace(), true);
                    b.setBlockData(mf, false);
                } else if (tirarTipo(c, Material.GLASS)) {
                    b.setType(Material.GLASS, false);
                } else {
                    o.falta = "vidro (3 blocos ou painéis)";
                    return false;
                }
            }
            case PORTA -> {
                Material porta = colonias.primeiroTipo(c, s -> Tag.WOODEN_DOORS.isTagged(s.getType()) && Ingrediente.simples(s));
                if (porta != null) tirarTipo(c, porta);
                else {
                    porta = daMadeira(o.madeira, "_DOOR");
                    if (porta == null) porta = Material.OAK_DOOR;
                    if (!tabua(c, o)) return false;
                    if (!tabua(c, o)) return false;
                }
                for (Bisected.Half h : Bisected.Half.values()) {
                    Door d = (Door) porta.createBlockData();
                    d.setFacing(face);
                    d.setHalf(h);
                    d.setHinge(Door.Hinge.LEFT);
                    (h == Bisected.Half.BOTTOM ? b : b.getRelative(BlockFace.UP)).setBlockData(d, false);
                }
            }
            case CAMA -> {
                Material cama = colonias.primeiroTipo(c, s -> Tag.BEDS.isTagged(s.getType()) && Ingrediente.simples(s));
                if (cama != null) tirarTipo(c, cama);
                else {
                    Material la = colonias.primeiroTipo(c, s -> Tag.WOOL.isTagged(s.getType()) && Ingrediente.simples(s));
                    cama = la == null ? null : Material.matchMaterial(la.name().replace("_WOOL", "_BED"));
                    if (cama == null || !tirar(c, s -> s.getType() == la, 3)) { o.falta = "cama (ou 3 lãs)"; return false; }
                }
                for (Bed.Part parte : Bed.Part.values()) {
                    Bed bd = (Bed) cama.createBlockData();
                    bd.setFacing(face);
                    bd.setPart(parte);
                    (parte == Bed.Part.FOOT ? b : b.getRelative(face)).setBlockData(bd, false);
                }
            }
            case LUZ -> {
                Material luz = colonias.primeiroTipo(c, s -> s.getType().name().endsWith("LANTERN") && s.getType() != Material.SEA_LANTERN
                        && s.getType() != Material.JACK_O_LANTERN && Ingrediente.simples(s));
                if (luz != null) tirarTipo(c, luz);
                else if (tirarTipo(c, Material.TORCH)) luz = Material.TORCH;
                else if (colonias.contar(c, s -> (s.getType() == Material.COAL || s.getType() == Material.CHARCOAL) && Ingrediente.simples(s)) > 0
                        && tirarTipo(c, Material.STICK)) {
                    tirar(c, s -> s.getType() == Material.COAL || s.getType() == Material.CHARCOAL, 1);
                    luz = Material.TORCH;
                } else {
                    o.falta = "luz (lanterna, tocha ou carvão e graveto)";
                    return false;
                }
                b.setType(luz, false);
            }
        }
        return true;
    }
}
