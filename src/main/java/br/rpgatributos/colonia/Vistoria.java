package br.rpgatributos.colonia;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A vistoria de uma construção (como os níveis dos prédios do MineColonies): conta o que tem
 * na área e diz em que nível ela está. Serve para quem constrói do próprio jeito (tem que atender
 * o mínimo do nível 1) e para subir de nível uma construção pronta, melhorando ela.
 */
final class Vistoria {

    /** O mínimo de um nível. 0 = não pede. */
    record Requisito(int blocos, int camas, int portas, int luz, int baus, int coberto, int altura, Map<Material, Integer> itens) {

        static Requisito ler(ConfigurationSection s) {
            Map<Material, Integer> itens = new LinkedHashMap<>();
            ConfigurationSection it = s.getConfigurationSection("itens");
            if (it != null) {
                for (String k : it.getKeys(false)) {
                    Material m = Material.matchMaterial(k);
                    if (m == null) throw new IllegalArgumentException("bloco desconhecido nos requisitos: " + k);
                    itens.put(m, Math.max(1, it.getInt(k)));
                }
            }
            return new Requisito(s.getInt("blocos"), s.getInt("camas"), s.getInt("portas"), s.getInt("luz"), s.getInt("baus"),
                    Math.max(0, Math.min(100, s.getInt("coberto"))), s.getInt("altura"), Collections.unmodifiableMap(itens));
        }

        /** As linhas do que esse nível pede, com o que a vistoria achou (✔ / ✖). */
        List<Linha> linhas(Vistoria v) {
            List<Linha> l = new ArrayList<>();
            if (blocos > 0) l.add(new Linha("Blocos construídos", v.blocos, blocos, null));
            if (camas > 0) l.add(new Linha("Camas", v.camas, camas, null));
            if (portas > 0) l.add(new Linha("Portas", v.portas, portas, null));
            if (luz > 0) l.add(new Linha("Luzes (tocha, lanterna...)", v.luz, luz, null));
            if (baus > 0) l.add(new Linha("Baús", v.baus, baus, null));
            if (coberto > 0) l.add(new Linha("Piso coberto por teto (%)", v.coberto(), coberto, null));
            if (altura > 0) l.add(new Linha("Altura (blocos acima do chão)", v.altura, altura, null));
            for (Map.Entry<Material, Integer> e : itens.entrySet()) l.add(new Linha(null, v.itens.getOrDefault(e.getKey(), 0), e.getValue(), e.getKey()));
            return l;
        }

        boolean atende(Vistoria v) {
            for (Linha x : linhas(v)) if (!x.ok()) return false;
            return true;
        }
    }

    /** Uma exigência: o que tem e o mínimo. Com {@code bloco}, o nome é o do bloco no idioma do jogador. */
    record Linha(String nome, int tem, int minimo, Material bloco) {
        boolean ok() { return tem >= minimo; }
    }

    int blocos, camas, portas, luz, baus, altura;
    private int colunas, cobertas;
    final Map<Material, Integer> itens = new EnumMap<>(Material.class);

    /** Um bloco da área: {@code ly} é a altura acima do chão (0 = chão); {@code colocado} = posto por alguém. */
    void contar(int ly, BlockData d, boolean colocado) {
        Material m = d.getMaterial();
        if (m.isAir()) return;
        if (colocado && m.isSolid()) {
            blocos++;
            if (ly > altura) altura = ly;
        }
        itens.merge(m, 1, Integer::sum);
        if (Tag.BEDS.isTagged(m) && d instanceof Bed b && b.getPart() == Bed.Part.FOOT) camas++;
        else if (Tag.DOORS.isTagged(m) && d instanceof Bisected b && b.getHalf() == Bisected.Half.BOTTOM) portas++;
        else if (m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) baus++;
        if (luz(m)) luz++;
    }

    /** Uma coluna de dentro da área (fora da borda): {@code coberta} = tem teto em cima do piso. */
    void coluna(boolean coberta) {
        colunas++;
        if (coberta) cobertas++;
    }

    int coberto() { return colunas == 0 ? 0 : cobertas * 100 / colunas; }

    /** Em que nível a construção está (0 = não atende nem o mínimo). */
    int nivel(List<Requisito> niveis) {
        int n = 0;
        for (Requisito r : niveis) {
            if (!r.atende(this)) break;
            n++;
        }
        return n;
    }

    /** Terreno e coisas que nascem sozinhas: só contam como "construído" se alguém colocou (a marca do anti-farm). */
    static boolean natural(Material m) {
        if (!m.isSolid() || Tag.LEAVES.isTagged(m)) return true;
        String n = m.name();
        if (n.endsWith("_ORE") || n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")) return !n.startsWith("STRIPPED_");
        if (n.endsWith("TERRACOTTA") && !n.endsWith("GLAZED_TERRACOTTA")) return true;
        return switch (m) {
            case DIRT, GRASS_BLOCK, COARSE_DIRT, ROOTED_DIRT, PODZOL, MYCELIUM, MUD, CLAY, SAND, RED_SAND, GRAVEL, STONE, DEEPSLATE,
                 ANDESITE, DIORITE, GRANITE, TUFF, CALCITE, DRIPSTONE_BLOCK, SANDSTONE, RED_SANDSTONE, SNOW_BLOCK, ICE, PACKED_ICE,
                 BLUE_ICE, MOSS_BLOCK, PALE_MOSS_BLOCK, NETHERRACK, BASALT, BLACKSTONE, END_STONE, OBSIDIAN, BEDROCK, MAGMA_BLOCK,
                 SOUL_SAND, SOUL_SOIL, AMETHYST_BLOCK, BUDDING_AMETHYST, SMOOTH_BASALT, MUSHROOM_STEM, BROWN_MUSHROOM_BLOCK,
                 RED_MUSHROOM_BLOCK, PUMPKIN, MELON, BEE_NEST, FARMLAND, DIRT_PATH -> true;
            default -> false;
        };
    }

    static boolean luz(Material m) {
        String n = m.name();
        return n.endsWith("TORCH") || n.endsWith("LANTERN") || n.endsWith("CAMPFIRE") || n.endsWith("CANDLE") || n.endsWith("FROGLIGHT")
                || m == Material.GLOWSTONE || m == Material.SHROOMLIGHT || m == Material.END_ROD || m == Material.REDSTONE_LAMP
                || m == Material.BEACON || m == Material.SEA_PICKLE;
    }
}
