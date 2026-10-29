import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabelas de saque das estruturas grandes (3.1): baús, escavação, cofre e equipamento do Coliseu,
 * e a configuração dos spawners de desafio. Uso: java GerarSaque <datapack/data/rpgatributos>
 */
public class GerarSaque {

    static File base;

    static String it(String n, int peso, int min, int max) {
        String f = min == 1 && max == 1 ? "" : ", \"functions\": [ { \"function\": \"minecraft:set_count\", \"count\": { \"type\": \"minecraft:uniform\", \"min\": " + min + ", \"max\": " + max + " } } ]";
        return "{ \"type\": \"minecraft:item\", \"name\": \"minecraft:" + n + "\", \"weight\": " + peso + f + " }";
    }

    static String it(String n, int peso) { return it(n, peso, 1, 1); }

    static String enc(String n, int peso) {
        return "{ \"type\": \"minecraft:item\", \"name\": \"minecraft:" + n + "\", \"weight\": " + peso + ", \"functions\": [ { \"function\": \"minecraft:enchant_randomly\" } ] }";
    }

    static String niv(String n, int peso) {
        return "{ \"type\": \"minecraft:item\", \"name\": \"minecraft:" + n + "\", \"weight\": " + peso
                + ", \"functions\": [ { \"function\": \"minecraft:enchant_with_levels\", \"levels\": { \"type\": \"minecraft:uniform\", \"min\": 20, \"max\": 39 } } ] }";
    }

    /** Um item do RPG: o datapack solta um papel marcado e o plugin troca pelo item de verdade quando ele cai (TrocaDeSaque). */
    static String rpg(String tipo, int peso) {
        return "{ \"type\": \"minecraft:item\", \"name\": \"minecraft:paper\", \"weight\": " + peso
                + ", \"functions\": [ { \"function\": \"minecraft:set_custom_data\", \"tag\": \"{PublicBukkitValues:{\\\"rpgatributos:troca\\\":\\\"" + tipo + "\\\"}}\" } ] }";
    }

    static void tab(String nome, String tipo, int min, int max, String... entradas) throws Exception {
        String rolls = min == max ? String.valueOf(min) : "{ \"type\": \"minecraft:uniform\", \"min\": " + min + ", \"max\": " + max + " }";
        String json = "{\n  \"type\": \"minecraft:" + tipo + "\",\n  \"pools\": [\n    {\n      \"rolls\": " + rolls + ",\n      \"entries\": [\n        "
                + String.join(",\n        ", entradas) + "\n      ]\n    }\n  ],\n  \"random_sequence\": \"rpgatributos:" + nome + "\"\n}\n";
        escrever("loot_table/" + nome + ".json", json);
    }

    static void escrever(String caminho, String texto) throws Exception {
        File f = new File(base, caminho);
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), texto);
    }

    /** Um spawner de desafio do Coliseu: normal e ameaçador. */
    static void spawner(String id, String equip, double simult, String... mobs) throws Exception {
        for (boolean ominoso : new boolean[]{false, true}) {
            List<String> pots = new ArrayList<>();
            for (String m : mobs) {
                String eq = equip == null ? "" : ", \"equipment\": { \"loot_table\": \"rpgatributos:equipment/" + equip + "\", \"slot_drop_chances\": 0.0 }";
                pots.add("{ \"data\": { \"entity\": { \"id\": \"minecraft:" + m + "\" }" + eq + " }, \"weight\": 1 }");
            }
            String ejetar = ominoso ? "  \"loot_tables_to_eject\": [ { \"data\": \"minecraft:spawners/ominous/trial_chamber/key\", \"weight\": 3 }, { \"data\": \"minecraft:spawners/ominous/trial_chamber/consumables\", \"weight\": 7 } ],\n" : "";
            String json = "{\n" + ejetar
                    + "  \"simultaneous_mobs\": " + (ominoso ? simult + 1 : simult) + ",\n"
                    + "  \"simultaneous_mobs_added_per_player\": 0.5,\n"
                    + "  \"total_mobs\": " + (ominoso ? 10.0 : 7.0) + ",\n"
                    + "  \"spawn_potentials\": [ " + String.join(", ", pots) + " ],\n"
                    + "  \"ticks_between_spawn\": " + (ominoso ? 20 : 30) + "\n}\n";
            escrever("trial_spawner/coliseu/" + id + "/" + (ominoso ? "ominous" : "normal") + ".json", json);
        }
    }

    public static void main(String[] a) throws Exception {
        base = new File(a[0]);
        tab("chests/arvore_raizes", "chest", 5, 8, it("golden_apple", 10, 1, 2), it("diamond", 6, 1, 3), it("emerald", 12, 2, 6), enc("book", 10),
                it("glow_berries", 14, 3, 9), it("moss_block", 12, 3, 8), it("oak_sapling", 12, 2, 5), it("experience_bottle", 10, 2, 6),
                it("enchanted_golden_apple", 1), niv("diamond_axe", 3));
        tab("chests/arvore_copa", "chest", 4, 7, it("apple", 16, 2, 6), it("honey_bottle", 10, 1, 3), it("string", 12, 2, 6), it("arrow", 12, 6, 16),
                enc("bow", 4), it("azalea", 8, 1, 2), it("flowering_azalea", 6, 1, 2), it("spore_blossom", 3), it("emerald", 8, 1, 4), it("feather", 10, 2, 6));
        tab("chests/colosso_coracao", "chest", 5, 8, it("diamond", 10, 2, 4), it("gold_ingot", 16, 3, 9), it("iron_ingot", 16, 4, 10), enc("book", 10),
                it("netherite_scrap", 2), it("totem_of_undying", 1), niv("iron_chestplate", 5), it("amethyst_shard", 10, 3, 8), it("echo_shard", 2, 1, 2));
        tab("chests/colosso_cabeca", "chest", 4, 6, it("gold_ingot", 14, 2, 6), it("lapis_lazuli", 14, 4, 12), it("book", 12, 2, 5), it("emerald", 12, 2, 5),
                it("name_tag", 6), it("experience_bottle", 10, 2, 5), enc("book", 6));
        tab("chests/cidade_ana_forja", "chest", 4, 7, it("iron_ingot", 18, 4, 12), it("gold_ingot", 12, 2, 8), it("coal", 14, 6, 16), it("netherite_scrap", 3),
                it("netherite_upgrade_smithing_template", 2), niv("diamond_pickaxe", 4), niv("iron_axe", 8), it("lava_bucket", 6), it("anvil", 4));
        tab("chests/cidade_ana_mina", "chest", 4, 7, it("raw_iron", 16, 4, 12), it("raw_gold", 10, 2, 8), it("raw_copper", 14, 4, 12), it("coal", 16, 6, 18),
                it("torch", 14, 8, 20), it("rail", 10, 6, 16), it("diamond", 3, 1, 2), enc("iron_pickaxe", 6));
        tab("chests/cidade_ana_quartel", "chest", 4, 6, enc("iron_helmet", 8), enc("iron_chestplate", 6), enc("iron_axe", 8), enc("crossbow", 6),
                it("bread", 16, 3, 8), it("cooked_beef", 12, 2, 6), it("arrow", 12, 8, 20), it("shield", 6));
        tab("chests/cidade_ana_tesouro", "chest", 5, 9, it("gold_block", 6, 1, 3), it("gold_ingot", 16, 4, 12), it("diamond", 10, 2, 5), it("emerald", 14, 3, 9),
                it("enchanted_golden_apple", 2), niv("golden_helmet", 5), niv("diamond_sword", 3), it("raw_gold_block", 4, 1, 2));
        tab("chests/ossada_acampamento", "chest", 4, 6, it("brush", 10), it("paper", 14, 3, 8), it("bread", 14, 2, 6), it("map", 8), it("bone", 14, 3, 9),
                it("torch", 12, 4, 10), it("compass", 4), it("dragon_breath", 2, 1, 2));
        tab("archaeology/ossada", "archaeology", 1, 1, it("bone", 30, 1, 3), it("bone_meal", 20, 2, 6), it("gold_nugget", 14, 2, 6), it("emerald", 10, 1, 2),
                it("skull_pottery_sherd", 6), it("danger_pottery_sherd", 5), it("diamond", 3), it("dragon_breath", 4), it("dragon_head", 1),
                rpg("reliquia", 6), rpg("pagina", 3));
        // 3.2: as estruturas pequenas e as escavações delas.
        tab("archaeology/estatua", "archaeology", 1, 1, it("gold_nugget", 20, 1, 4), it("emerald", 10), it("brick", 14), it("flower_pot", 8),
                it("arms_up_pottery_sherd", 6), it("heart_pottery_sherd", 5), rpg("reliquia", 10), rpg("pagina", 4), it("diamond", 2));
        tab("archaeology/arco", "archaeology", 1, 1, it("gold_nugget", 18, 1, 4), it("emerald", 10), it("stick", 14), it("lapis_lazuli", 12, 1, 4),
                it("archer_pottery_sherd", 6), it("prize_pottery_sherd", 5), rpg("reliquia", 10), rpg("tomo", 4), rpg("mapa", 4));
        tab("archaeology/cripta", "archaeology", 1, 1, it("bone", 24, 1, 3), it("gold_nugget", 14, 1, 4), it("candle", 10), it("emerald", 8),
                it("skull_pottery_sherd", 6), it("mourner_pottery_sherd", 5), rpg("reliquia", 12), rpg("tomo", 6), rpg("pagina", 4));
        tab("archaeology/sitio", "archaeology", 1, 1, it("coal", 14, 1, 3), it("gold_nugget", 14, 1, 4), it("emerald", 10), it("brick", 10),
                it("miner_pottery_sherd", 5), it("explorer_pottery_sherd", 5), it("burn_pottery_sherd", 4), it("sheaf_pottery_sherd", 4),
                rpg("reliquia", 14), rpg("carta", 6), rpg("mapa", 5), rpg("pagina", 3), it("diamond", 2));
        tab("chests/cacadores", "chest", 3, 6, it("leather", 16, 2, 6), it("rabbit_hide", 12, 1, 4), it("cooked_beef", 12, 1, 4), it("arrow", 14, 6, 16),
                enc("bow", 5), it("lead", 6), it("rabbit_foot", 3), it("iron_axe", 4), it("string", 10, 2, 5));
        tab("chests/santuario_rio", "chest", 3, 5, it("lily_pad", 10, 1, 3), it("emerald", 12, 1, 4), it("candle", 12, 1, 3), enc("fishing_rod", 5),
                it("nautilus_shell", 2), it("prismarine_crystals", 6, 1, 3), it("honey_bottle", 8), it("golden_apple", 2));
        tab("chests/cadafalso", "chest", 3, 5, it("iron_nugget", 14, 2, 8), it("emerald", 8, 1, 3), it("rotten_flesh", 14, 1, 5), it("lead", 6),
                it("iron_sword", 4), it("iron_chain", 8, 1, 3), it("map", 4));
        tab("chests/cripta_poco", "chest", 4, 7, it("gold_ingot", 12, 1, 4), it("bone", 14, 2, 6), it("candle", 10, 1, 3), enc("book", 8),
                it("emerald", 10, 1, 4), it("ender_pearl", 4), it("golden_apple", 3), it("experience_bottle", 8, 1, 4));
        tab("chests/caravana_perdida", "chest", 4, 7, it("wheat", 14, 4, 12), it("bread", 12, 2, 6), it("emerald", 12, 2, 6), it("gold_ingot", 8, 1, 4),
                it("saddle", 4), it("lead", 6), it("bundle", 4), it("cocoa_beans", 6, 2, 6), it("sugar", 8, 2, 6));
        tab("chests/sitio_escavacao", "chest", 3, 6, it("brush", 12), it("paper", 12, 2, 6), it("bread", 12, 2, 5), it("torch", 10, 4, 10),
                it("map", 6), it("decorated_pot", 4), it("compass", 3), it("emerald", 8, 1, 3));
        tab("chests/coliseu", "chest", 4, 6, it("arrow", 14, 8, 20), enc("iron_sword", 8), enc("golden_chestplate", 6), it("bread", 14, 3, 8),
                it("gold_ingot", 12, 2, 6), it("trial_key", 6), it("shield", 8));
        tab("coliseu/recompensa", "chest", 2, 3, it("emerald", 20, 3, 8), it("diamond", 10, 1, 3), it("golden_apple", 10, 1, 2), niv("diamond_sword", 5),
                niv("diamond_chestplate", 3), enc("book", 12), it("enchanted_golden_apple", 2), it("ominous_bottle", 4), it("heavy_core", 1));
        tab("chests/castelo_trono", "chest", 5, 8, it("gold_ingot", 16, 4, 10), it("diamond", 8, 1, 3), niv("golden_helmet", 6), niv("diamond_sword", 3),
                enc("book", 10), it("emerald", 12, 3, 7), it("enchanted_golden_apple", 1), it("golden_apple", 8, 1, 2));
        tab("chests/castelo_biblioteca", "chest", 4, 7, it("book", 18, 2, 6), enc("book", 12), it("paper", 14, 4, 10), it("map", 8),
                it("experience_bottle", 12, 3, 8), it("ink_sac", 10, 2, 5), it("writable_book", 6));
        escrever("loot_table/equipment/gladiador.json", "{\n  \"type\": \"minecraft:equipment\",\n  \"pools\": [\n"
                + "    { \"rolls\": 1, \"entries\": [ " + it("iron_helmet", 3) + ", " + it("golden_helmet", 2) + ", { \"type\": \"minecraft:empty\", \"weight\": 1 } ] },\n"
                + "    { \"rolls\": 1, \"entries\": [ " + it("iron_sword", 4) + ", " + it("iron_axe", 3) + ", " + it("stone_sword", 2) + " ] },\n"
                + "    { \"rolls\": 1, \"entries\": [ " + it("chainmail_chestplate", 2) + ", { \"type\": \"minecraft:empty\", \"weight\": 3 } ] }\n"
                + "  ],\n  \"random_sequence\": \"rpgatributos:equipment/gladiador\"\n}\n");
        escrever("loot_table/equipment/campeao.json", "{\n  \"type\": \"minecraft:equipment\",\n  \"pools\": [\n"
                + "    { \"rolls\": 1, \"entries\": [ " + it("iron_axe", 3) + ", " + it("diamond_axe", 1) + " ] },\n"
                + "    { \"rolls\": 1, \"entries\": [ " + it("iron_chestplate", 1) + ", { \"type\": \"minecraft:empty\", \"weight\": 2 } ] }\n"
                + "  ],\n  \"random_sequence\": \"rpgatributos:equipment/campeao\"\n}\n");
        spawner("gladiadores", "gladiador", 3, "zombie", "husk");
        spawner("arqueiros", null, 3, "skeleton", "stray");
        spawner("feras", null, 3, "spider", "cave_spider");
        spawner("campeoes", "campeao", 2, "vindicator", "evoker");
        System.out.println("saque ok");
    }
}
