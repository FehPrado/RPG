package br.rpgatributos.arcano;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemRarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static br.rpgatributos.arcano.Essencia.*;
import static org.bukkit.Material.*;

/**
 * Quais essências cada item do jogo carrega.
 *
 * Itens marcantes têm valores escolhidos à mão (tabela abaixo). Todo o resto
 * é decidido por regras (nome do item, tags do jogo, se é comida etc.), então
 * qualquer item funciona, inclusive os que vierem em versões novas.
 * Itens raros/épicos do jogo ganham pontos extras.
 */
public final class Catalogo {

    private static final Map<Material, Map<Essencia, Integer>> TABELA = new HashMap<>();
    private static final Map<Material, Map<Essencia, Integer>> CACHE = new HashMap<>();

    private Catalogo() {}

    private static void d(Material m, Object... pares) {
        Map<Essencia, Integer> e = new EnumMap<>(Essencia.class);
        for (int i = 0; i < pares.length; i += 2) e.put((Essencia) pares[i], (Integer) pares[i + 1]);
        TABELA.put(m, e);
    }

    static {
        // Fogo
        d(BLAZE_ROD, FOGO, 3);
        d(BLAZE_POWDER, FOGO, 2);
        d(MAGMA_CREAM, FOGO, 2, NATUREZA, 1);
        d(FIRE_CHARGE, FOGO, 2, VENTO, 1);
        d(LAVA_BUCKET, FOGO, 3, TERRA, 1);
        d(MAGMA_BLOCK, FOGO, 1, TERRA, 1);
        d(CAMPFIRE, FOGO, 1, NATUREZA, 1);
        d(SOUL_CAMPFIRE, FOGO, 1, SOMBRA, 1);
        d(SHROOMLIGHT, FOGO, 1, ENERGIA, 1);
        d(FLINT_AND_STEEL, FOGO, 1, TERRA, 1);
        d(COAL, FOGO, 1);
        d(CHARCOAL, FOGO, 1);
        d(COAL_BLOCK, FOGO, 2);
        d(GHAST_TEAR, VIDA, 2, FOGO, 1);
        d(GUNPOWDER, FOGO, 1, ENERGIA, 1);
        // Gelo
        d(ICE, GELO, 1);
        d(PACKED_ICE, GELO, 2);
        d(BLUE_ICE, GELO, 3);
        d(SNOWBALL, GELO, 1);
        d(SNOW_BLOCK, GELO, 1);
        d(POWDER_SNOW_BUCKET, GELO, 2, AGUA, 1);
        // Vento
        d(FEATHER, VENTO, 2);
        d(PHANTOM_MEMBRANE, VENTO, 2, SOMBRA, 1);
        d(BREEZE_ROD, VENTO, 3);
        d(WIND_CHARGE, VENTO, 2);
        d(ELYTRA, VENTO, 4, VAZIO, 1);
        d(RABBIT_FOOT, VENTO, 2, VIDA, 1);
        d(SLIME_BALL, VENTO, 1, NATUREZA, 1);
        d(GOAT_HORN, VENTO, 2, TERRA, 1);
        // Água
        d(WATER_BUCKET, AGUA, 2);
        d(PRISMARINE_SHARD, AGUA, 1, TERRA, 1);
        d(PRISMARINE_CRYSTALS, AGUA, 1, ENERGIA, 1);
        d(HEART_OF_THE_SEA, AGUA, 4, VIDA, 1);
        d(NAUTILUS_SHELL, AGUA, 2);
        d(TURTLE_SCUTE, AGUA, 2, TERRA, 1);
        d(SPONGE, AGUA, 1);
        d(WET_SPONGE, AGUA, 2);
        d(KELP, AGUA, 1, NATUREZA, 1);
        d(PUFFERFISH, AGUA, 1, SOMBRA, 1);
        d(TRIDENT, AGUA, 3, ENERGIA, 2);
        d(CONDUIT, AGUA, 4, ENERGIA, 2);
        d(INK_SAC, AGUA, 1, SOMBRA, 1);
        d(GLOW_INK_SAC, AGUA, 1, ENERGIA, 1);
        // Vida
        d(APPLE, VIDA, 1, NATUREZA, 1);
        d(GOLDEN_APPLE, VIDA, 3);
        d(ENCHANTED_GOLDEN_APPLE, VIDA, 5, ENERGIA, 1);
        d(GOLDEN_CARROT, VIDA, 2);
        d(GLISTERING_MELON_SLICE, VIDA, 2);
        d(TOTEM_OF_UNDYING, VIDA, 5, VAZIO, 1);
        d(HONEY_BOTTLE, VIDA, 1, NATUREZA, 1);
        d(HONEYCOMB, NATUREZA, 1);
        d(EGG, VIDA, 1);
        d(MILK_BUCKET, VIDA, 1, AGUA, 1);
        d(CAKE, VIDA, 2);
        // Terra
        d(RAW_IRON, TERRA, 1);
        d(IRON_INGOT, TERRA, 2);
        d(GOLD_INGOT, TERRA, 1, VIDA, 1);
        d(COPPER_INGOT, TERRA, 1, ENERGIA, 1);
        d(DIAMOND, TERRA, 3);
        d(EMERALD, TERRA, 2, VIDA, 1);
        d(NETHERITE_SCRAP, TERRA, 2, FOGO, 1);
        d(NETHERITE_INGOT, TERRA, 4, FOGO, 1);
        d(ANCIENT_DEBRIS, TERRA, 2, FOGO, 1);
        d(OBSIDIAN, TERRA, 2, VAZIO, 1);
        d(CRYING_OBSIDIAN, TERRA, 1, VAZIO, 2);
        d(CLAY_BALL, TERRA, 1, AGUA, 1);
        d(FLINT, TERRA, 1);
        d(IRON_BLOCK, TERRA, 3);
        d(DIAMOND_BLOCK, TERRA, 4);
        d(HEAVY_CORE, TERRA, 5, VENTO, 1);
        d(ARMADILLO_SCUTE, TERRA, 2, VIDA, 1);
        d(LODESTONE, TERRA, 2, VAZIO, 1);
        d(AMETHYST_SHARD, TERRA, 1, ENERGIA, 1);
        // Sombra
        d(BONE, SOMBRA, 1);
        d(BONE_MEAL, NATUREZA, 1, VIDA, 1);
        d(ROTTEN_FLESH, SOMBRA, 1);
        d(SPIDER_EYE, SOMBRA, 1, NATUREZA, 1);
        d(FERMENTED_SPIDER_EYE, SOMBRA, 2);
        d(WITHER_ROSE, SOMBRA, 3);
        d(WITHER_SKELETON_SKULL, SOMBRA, 4);
        d(ECHO_SHARD, SOMBRA, 3, VAZIO, 1);
        d(SCULK, SOMBRA, 1);
        d(SCULK_CATALYST, SOMBRA, 2, VIDA, 1);
        d(SCULK_SHRIEKER, SOMBRA, 2, ENERGIA, 1);
        d(SOUL_SAND, SOMBRA, 1);
        d(SOUL_SOIL, SOMBRA, 1);
        d(RECOVERY_COMPASS, VAZIO, 2, SOMBRA, 2);
        // Vazio
        d(ENDER_PEARL, VAZIO, 2);
        d(ENDER_EYE, VAZIO, 2, ENERGIA, 1);
        d(CHORUS_FRUIT, VAZIO, 2, NATUREZA, 1);
        d(POPPED_CHORUS_FRUIT, VAZIO, 1);
        d(SHULKER_SHELL, VAZIO, 3, TERRA, 1);
        d(DRAGON_BREATH, VAZIO, 3, FOGO, 1);
        d(DRAGON_EGG, VAZIO, 5, VIDA, 2);
        d(ENDER_CHEST, VAZIO, 2);
        d(END_CRYSTAL, VAZIO, 3, ENERGIA, 2);
        d(COMPASS, VAZIO, 1, TERRA, 1);
        // Energia
        d(REDSTONE, ENERGIA, 1);
        d(REDSTONE_BLOCK, ENERGIA, 2);
        d(GLOWSTONE_DUST, ENERGIA, 1);
        d(GLOWSTONE, ENERGIA, 2);
        d(LIGHTNING_ROD, ENERGIA, 2, TERRA, 1);
        d(BEACON, ENERGIA, 4, VIDA, 2);
        d(NETHER_STAR, ENERGIA, 4, VIDA, 2, SOMBRA, 1);
        d(TRIAL_KEY, ENERGIA, 1, VAZIO, 1);
        d(OMINOUS_TRIAL_KEY, ENERGIA, 2, SOMBRA, 1);
        d(EXPERIENCE_BOTTLE, ENERGIA, 2);
        d(CLOCK, ENERGIA, 1, TERRA, 1);
        d(SEA_LANTERN, ENERGIA, 1, AGUA, 1);
        // Natureza
        d(MOSS_BLOCK, NATUREZA, 2);
        d(VINE, NATUREZA, 1);
        d(BAMBOO, NATUREZA, 1, VENTO, 1);
        d(CACTUS, NATUREZA, 1, TERRA, 1);
        d(SWEET_BERRIES, NATUREZA, 1, VIDA, 1);
        d(GLOW_BERRIES, NATUREZA, 1, ENERGIA, 1);
        d(SPORE_BLOSSOM, NATUREZA, 2, VIDA, 1);
        d(TORCHFLOWER, NATUREZA, 2);
        d(PITCHER_PLANT, NATUREZA, 2);
        d(SNIFFER_EGG, NATUREZA, 3, VIDA, 2);
        d(CREAKING_HEART, NATUREZA, 2, SOMBRA, 2);
        d(RESIN_CLUMP, NATUREZA, 1, TERRA, 1);
        d(HAY_BLOCK, NATUREZA, 2, VIDA, 1);
        d(WHEAT, NATUREZA, 1, VIDA, 1);
    }

    /** Essências do item (vazio = item sem essência, não pode ser infundido). */
    public static Map<Essencia, Integer> de(Material m) {
        return CACHE.computeIfAbsent(m, Catalogo::calcular);
    }

    public static boolean temEssencia(Material m) {
        return m != null && !m.isAir() && !de(m).isEmpty();
    }

    /** Soma dos pontos de essência: quanto mais poder, maior o bônus da parte do corpo. */
    public static int poder(Material m) {
        int p = 0;
        for (int v : de(m).values()) p += v;
        return p;
    }

    /** 0 = comum, 1 = incomum, 2 = raro, 3 = épico (raridade do próprio Minecraft). */
    public static int raridade(Material m) {
        if (!m.isItem() || m.asItemType() == null) return 0;
        ItemRarity r = m.asItemType().getItemRarity();
        if (r == ItemRarity.EPIC) return 3;
        if (r == ItemRarity.RARE) return 2;
        if (r == ItemRarity.UNCOMMON) return 1;
        return 0;
    }

    /** Quanto o item pesa no corpo. */
    public static int carga(Material m) {
        return 1 + raridade(m) + (poder(m) >= 4 ? 1 : 0);
    }

    private static Map<Essencia, Integer> calcular(Material m) {
        if (m == null || m.isAir() || !m.isItem()) return Collections.emptyMap();
        Map<Essencia, Integer> tabela = TABELA.get(m);
        if (tabela != null) return tabela;

        Map<Essencia, Integer> e = new EnumMap<>(Essencia.class);
        String n = m.name();

        if (tem(n, "FIRE", "FLAME", "BLAZE", "MAGMA", "LAVA", "NETHERRACK", "NETHER_BRICK", "CRIMSON", "TORCH")) e.put(FOGO, 1);
        if (n.equals("ICE") || tem(n, "_ICE", "SNOW", "FROST")) e.put(GELO, 1); // "_ICE" para não pegar MELON_SLICE
        if (tem(n, "FEATHER", "WIND", "BREEZE", "PHANTOM", "WOOL", "STRING", "PAPER")) e.put(VENTO, 1);
        if (tem(n, "PRISMARINE", "CORAL", "SEA", "KELP", "WATER", "SPONGE", "COD", "SALMON", "FISH", "TURTLE", "NAUTILUS")
                || Tag.CORALS.isTagged(m) || Tag.ITEMS_FISHES.isTagged(m)) e.put(AGUA, 1);
        if (tem(n, "_ORE", "INGOT", "RAW_", "STONE", "DEEPSLATE", "GRANITE", "DIORITE", "ANDESITE", "TUFF", "BRICK",
                "IRON", "GOLD", "DIAMOND", "EMERALD", "CLAY", "DIRT", "SAND", "GRAVEL", "MUD", "QUARTZ", "CALCITE", "BASALT")) {
            e.put(TERRA, 1);
        }
        if (tem(n, "ENDER", "END_", "CHORUS", "SHULKER", "PURPUR", "OBSIDIAN")) e.put(VAZIO, 1);
        if (tem(n, "REDSTONE", "GLOWSTONE", "COPPER", "LIGHTNING", "AMETHYST", "LANTERN", "LAMP", "OBSERVER", "PISTON")) e.put(ENERGIA, 1);
        if (tem(n, "BONE", "SKULL", "WITHER", "SOUL", "SCULK", "ECHO", "ROTTEN", "SPIDER", "BLACKSTONE")) e.put(SOMBRA, 1);
        if (tem(n, "SAPLING", "LEAVES", "FLOWER", "SEEDS", "MOSS", "VINE", "MUSHROOM", "BAMBOO", "_LOG", "_WOOD", "PLANKS",
                "BUSH", "GRASS", "FERN", "ROOT", "PETAL", "LILY", "AZALEA", "BERRIES", "CACTUS", "WHEAT", "CARROT", "POTATO",
                "BEETROOT", "MELON", "PUMPKIN", "SUGAR_CANE", "COCOA", "TULIP", "ORCHID", "DAISY", "POPPY", "ROSE", "LILAC",
                "PEONY", "ALLIUM", "BLUET", "CORNFLOWER", "DANDELION", "EYEBLOSSOM", "HANGING_MOSS", "STEM", "FUNGUS", "WART")
                || Tag.FLOWERS.isTagged(m) || Tag.SAPLINGS.isTagged(m) || Tag.LEAVES.isTagged(m) || Tag.LOGS.isTagged(m)) {
            e.put(NATUREZA, 1);
        }
        if (m.isEdible() || Tag.ITEMS_MEAT.isTagged(m)) e.merge(VIDA, 1, Integer::sum);

        // Bloco comum sem nenhuma pista: um pouco de Terra.
        if (e.isEmpty() && m.isBlock()) e.put(TERRA, 1);

        // Raridade do próprio jogo reforça a essência principal.
        int raridade = raridade(m);
        if (raridade >= 2 && !e.isEmpty()) {
            Essencia principal = e.keySet().iterator().next();
            e.merge(principal, raridade - 1, Integer::sum);
        }
        return e;
    }

    private static boolean tem(String nome, String... partes) {
        for (String p : partes) if (nome.contains(p)) return true;
        return false;
    }
}
