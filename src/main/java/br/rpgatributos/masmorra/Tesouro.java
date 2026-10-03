package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.forja.Raridade;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** O que cai nos baús da masmorra: cresce com a dificuldade; o baú do chefe é o maior. */
public final class Tesouro {

    private static final List<Enchantment> ENCANTOS = List.of(
            Enchantment.SHARPNESS, Enchantment.PROTECTION, Enchantment.EFFICIENCY, Enchantment.UNBREAKING,
            Enchantment.FORTUNE, Enchantment.LOOTING, Enchantment.POWER, Enchantment.FEATHER_FALLING,
            Enchantment.SILK_TOUCH, Enchantment.MENDING, Enchantment.FIRE_PROTECTION, Enchantment.RESPIRATION,
            Enchantment.DEPTH_STRIDER, Enchantment.SWEEPING_EDGE, Enchantment.INFINITY, Enchantment.THORNS);
    private static final String[] PECAS = {"SWORD", "AXE", "PICKAXE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

    private Tesouro() { }

    /** Enche o baú. {@code rolagens}: quantos sorteios; {@code chefe}: o baú final (com equipamento garantido). */
    public static void encher(RPGAtributos plugin, Inventory inv, Dificuldade d, int rolagens, boolean chefe, Random r) {
        List<ItemStack> itens = sortear(plugin, d, rolagens, chefe, r);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, r);
        int i = 0;
        for (ItemStack it : itens) {
            if (i < slots.size()) inv.setItem(slots.get(i++), it);
            else inv.addItem(it);
        }
    }

    static List<ItemStack> sortear(RPGAtributos plugin, Dificuldade d, int rolagens, boolean chefe, Random r) {
        int t = d.nivel();
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.EMERALD, 3 + r.nextInt(6 * (t + 1))));
        for (int i = 0; i < rolagens; i++) {
            switch (r.nextInt(8)) {
                case 0 -> l.add(new ItemStack(t == 0 ? Material.IRON_INGOT : Material.GOLD_INGOT, 3 + r.nextInt(6)));
                case 1 -> l.add(new ItemStack(Material.DIAMOND, 1 + r.nextInt(1 + t * 2)));
                case 2 -> l.add(livro(t, r));
                case 3 -> l.add(t == 3 && r.nextDouble() < 0.15 ? new ItemStack(Material.ENCHANTED_GOLDEN_APPLE)
                        : new ItemStack(Material.GOLDEN_APPLE, 1 + r.nextInt(2)));
                case 4 -> l.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 4 + r.nextInt(8 + t * 4)));
                case 5 -> {
                    ItemStack raro = raro(t, chefe, r);
                    l.add(raro != null ? raro : new ItemStack(Material.EMERALD, 4 + r.nextInt(6)));
                }
                case 6 -> l.add(equipamento(plugin, d, false, r));
                default -> l.add(new ItemStack(t >= 2 ? Material.NETHERITE_SCRAP : Material.IRON_INGOT, t >= 2 ? 1 : 4 + r.nextInt(5)));
            }
        }
        if (chefe) {
            l.add(equipamento(plugin, d, true, r));
            if (t >= 1) l.add(Raro.FRAGMENTO_DE_FORJA.criar(1 + r.nextInt(t + 1)));
            if (t >= 2 && r.nextDouble() < 0.5) l.add(Raro.PEDRA_DE_PROTECAO.criar(1));
            if (t == 3 && r.nextDouble() < 0.35) l.add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
            // Pistas dos Locais Ocultos e das lendas.
            if (r.nextDouble() < 0.6) l.add(Raro.MAPA_RASGADO.criar(1));
            if (r.nextDouble() < 0.4) l.add(Raro.PAGINA_DE_LENDA.criar(1));
            if (t >= 2 && r.nextDouble() < (t == 3 ? 0.2 : 0.08)) l.add(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio());
        } else {
            if (r.nextDouble() < 0.08) l.add(Raro.MAPA_RASGADO.criar(1));
            if (r.nextDouble() < 0.05) l.add(Raro.PAGINA_DE_LENDA.criar(1));
        }
        return l;
    }

    private static ItemStack raro(int t, boolean chefe, Random r) {
        if (t >= 1 && r.nextDouble() < 0.6) return Raro.FRAGMENTO_DE_FORJA.criar(1 + r.nextInt(t));
        if (t >= 2 && r.nextDouble() < 0.4) return Raro.PEDRA_DE_PROTECAO.criar(1);
        if (t == 3 && chefe) return Raro.ESSENCIA_PRIMORDIAL.criar(1);
        return null;
    }

    private static ItemStack livro(int t, Random r) {
        Enchantment e = ENCANTOS.get(r.nextInt(ENCANTOS.size()));
        int max = e.getMaxLevel();
        // Dificuldades altas puxam para o nível máximo do encanto.
        int nivel = Math.min(max, 1 + r.nextInt(max) + (t >= 2 ? 1 : 0));
        ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
        livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(e, nivel, true));
        return livro;
    }

    /** Arma ou armadura forjada, do material da dificuldade. {@code melhor}: puxa para a raridade máxima. */
    private static ItemStack equipamento(RPGAtributos plugin, Dificuldade d, boolean melhor, Random r) {
        String prefixo = switch (d.material()) {
            case IRON_INGOT -> "IRON_";
            case NETHERITE_INGOT -> "NETHERITE_";
            default -> "DIAMOND_";
        };
        Material m = r.nextInt(8) == 0 ? Material.BOW : Material.matchMaterial(prefixo + PECAS[r.nextInt(PECAS.length)]);
        if (m == null) m = Material.IRON_SWORD;
        Raridade[] todas = Raridade.values();
        int min = d.raridadeMin().ordinal(), max = d.raridadeMax().ordinal();
        int escolhida;
        if (melhor) escolhida = r.nextDouble() < 0.5 ? max : min + r.nextInt(max - min + 1);
        else escolhida = r.nextDouble() < 0.7 ? min : min + r.nextInt(max - min + 1);
        return plugin.forja().forjar(null, new ItemStack(m), todas[escolhida]);
    }
}
