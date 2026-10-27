package br.rpgatributos.ritual;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * 2.87: o que o guardião carrega e o que ele gasta. A ferramenta (picareta, machado, enxada,
 * tesoura ou espada) fica guardada no próprio guardião e define a velocidade; ela gasta
 * durabilidade e quebra. Insumos (tábuas, tochas, sementes, comida) saem do baú do posto ou
 * da mochila. A munição dos guardiões de defesa fica num estoque dentro deles.
 */
final class Equipamento {

    static final NamespacedKey K_FERRAMENTA = new NamespacedKey("rpgatributos", "guardiao_ferramenta");
    static final NamespacedKey K_MUNICAO = new NamespacedKey("rpgatributos", "guardiao_municao");
    static final NamespacedKey K_MUNICAO_QTD = new NamespacedKey("rpgatributos", "guardiao_municao_qtd");
    static final NamespacedKey K_DISPAROS = new NamespacedKey("rpgatributos", "guardiao_disparos");

    private Equipamento() { }

    // =====================================================================
    //  Ferramenta
    // =====================================================================

    /** A ferramenta que esse tipo usa (ou null se não usa). */
    static String ferramentaDoTipo(Guardioes.Tipo t) {
        return switch (t) {
            case MINERADOR -> "picareta";
            case LENHADOR -> "machado";
            case FAZENDA -> "enxada";
            case PASTOR, APICULTOR -> "tesoura";
            case FERRO -> "espada";
            case PESCADOR -> "vara de pesca";
            default -> null;
        };
    }

    static boolean serve(Guardioes.Tipo t, ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        String n = i.getType().name();
        return switch (t) {
            case MINERADOR -> n.endsWith("_PICKAXE");
            case LENHADOR -> n.endsWith("_AXE");
            case FAZENDA -> n.endsWith("_HOE");
            case PASTOR, APICULTOR -> i.getType() == Material.SHEARS;
            case FERRO -> n.endsWith("_SWORD");
            case PESCADOR -> i.getType() == Material.FISHING_ROD;
            default -> false;
        };
    }

    static ItemStack ferramenta(Entity g) {
        byte[] b = g.getPersistentDataContainer().get(K_FERRAMENTA, PersistentDataType.BYTE_ARRAY);
        return b == null ? null : ItemStack.deserializeBytes(b);
    }

    static void definirFerramenta(Entity g, ItemStack i) {
        if (i == null || i.isEmpty()) g.getPersistentDataContainer().remove(K_FERRAMENTA);
        else g.getPersistentDataContainer().set(K_FERRAMENTA, PersistentDataType.BYTE_ARRAY, i.serializeAsBytes());
    }

    /** Quão rápido a ferramenta trabalha: madeira 0,5 ... netherite 1,8 (+15% por nível de Eficiência). */
    static double velocidade(ItemStack i) {
        if (i == null) return 0;
        String n = i.getType().name();
        double base = n.startsWith("WOODEN") ? 0.5 : n.startsWith("STONE") ? 0.7 : n.startsWith("COPPER") ? 0.85
                : n.startsWith("IRON") || i.getType() == Material.SHEARS ? 1.0 : n.startsWith("GOLDEN") ? 1.2
                : n.startsWith("DIAMOND") ? 1.4 : n.startsWith("NETHERITE") ? 1.8 : 1.0;
        return base * (1 + 0.15 * i.getEnchantmentLevel(Enchantment.EFFICIENCY));
    }

    /** Dano a mais da espada do Golem Guardião. */
    static double bonusEspada(ItemStack i) {
        if (i == null) return 0;
        String n = i.getType().name();
        double b = n.startsWith("WOODEN") || n.startsWith("GOLDEN") ? 1 : n.startsWith("STONE") || n.startsWith("COPPER") ? 2
                : n.startsWith("IRON") ? 3 : n.startsWith("DIAMOND") ? 4 : n.startsWith("NETHERITE") ? 5 : 0;
        return b + 0.5 * i.getEnchantmentLevel(Enchantment.SHARPNESS);
    }

    /** Ferramentas que, pelo tipo, cavam esse bloco (obsidiana e detrito pedem diamante). */
    static boolean cavaObsidiana(ItemStack i) {
        return i != null && (i.getType() == Material.DIAMOND_PICKAXE || i.getType() == Material.NETHERITE_PICKAXE);
    }

    /**
     * Gasta 1 de durabilidade (com a chance do Inquebrável). @return true se a ferramenta quebrou
     * (e já foi tirada do guardião).
     */
    static boolean gastar(Entity g) {
        ItemStack i = ferramenta(g);
        if (i == null) return false;
        int inq = i.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (inq > 0 && ThreadLocalRandom.current().nextInt(inq + 1) != 0) return false;
        if (!(i.getItemMeta() instanceof Damageable d) || i.getType().getMaxDurability() <= 0) return false;
        int dano = d.getDamage() + 1;
        if (dano >= i.getType().getMaxDurability()) {
            definirFerramenta(g, null);
            return true;
        }
        i.editMeta(Damageable.class, m -> m.setDamage(dano));
        definirFerramenta(g, i);
        return false;
    }

    static String durabilidade(ItemStack i) {
        if (i == null || i.getType().getMaxDurability() <= 0 || !(i.getItemMeta() instanceof Damageable d)) return "";
        return " (" + (i.getType().getMaxDurability() - d.getDamage()) + "/" + i.getType().getMaxDurability() + ")";
    }

    // =====================================================================
    //  Insumos: baú do posto e mochila
    // =====================================================================

    static Container bauPerto(Location posto, double raio) {
        List<Container> l = bausPerto(posto, raio);
        return l.isEmpty() ? null : l.getFirst();
    }

    /** 2.89: todos os baús e barris a até {@code raio} do posto, do mais perto ao mais longe. */
    static List<Container> bausPerto(Location posto, double raio) {
        List<Container> l = new ArrayList<>();
        int cx = posto.getBlockX() >> 4, cz = posto.getBlockZ() >> 4, alc = Math.max(1, (int) Math.ceil(raio / 16));
        for (int x = cx - alc; x <= cx + alc; x++) for (int z = cz - alc; z <= cz + alc; z++) {
            if (!posto.getWorld().isChunkLoaded(x, z)) continue;
            for (BlockState s : posto.getWorld().getChunkAt(x, z).getTileEntities(false)) {
                if (!(s instanceof Container c) || !(s.getType() == Material.CHEST || s.getType() == Material.BARREL || s.getType() == Material.TRAPPED_CHEST)) continue;
                if (s.getLocation().add(0.5, 0, 0.5).distanceSquared(posto) < raio * raio) l.add(c);
            }
        }
        l.sort(java.util.Comparator.comparingDouble(c -> c.getLocation().add(0.5, 0, 0.5).distanceSquared(posto)));
        // As duas metades de um baú duplo são o mesmo inventário: fica só uma.
        java.util.Set<Location> vistos = new java.util.HashSet<>();
        l.removeIf(c -> !vistos.add(c.getInventory().getLocation()));
        return l;
    }

    /** Os inventários de onde o guardião tira e onde guarda: o baú do posto e a mochila (nível III+). */
    private static List<Inventory> estoques(Entity g, Location posto, double raio) {
        List<Inventory> l = new ArrayList<>();
        Container bau = bauPerto(posto, raio);
        if (bau != null) l.add(bau.getInventory());
        if (Guardioes.nivel(g) >= 3) {
            Inventory m = Bukkit.createInventory(null, 27);
            m.setContents(Guardioes.mochila(g));
            l.add(m);
        }
        return l;
    }

    static int contar(Entity g, Location posto, double raio, Predicate<Material> tipo) {
        int n = 0;
        for (Inventory inv : estoques(g, posto, raio)) {
            for (ItemStack i : inv.getContents()) if (i != null && !i.isEmpty() && tipo.test(i.getType()) && comum(i)) n += i.getAmount();
        }
        return n;
    }

    /** Tira {@code qtd} itens desse tipo (do baú e depois da mochila). @return o material tirado, ou null se não tinha tudo. */
    static Material tirar(Entity g, Location posto, double raio, Predicate<Material> tipo, int qtd) {
        if (contar(g, posto, raio, tipo) < qtd) return null;
        Material qual = null;
        int falta = qtd;
        Container bau = bauPerto(posto, raio);
        List<Inventory> invs = new ArrayList<>();
        if (bau != null) invs.add(bau.getInventory());
        Inventory mochila = null;
        if (Guardioes.nivel(g) >= 3) {
            mochila = Bukkit.createInventory(null, 27);
            mochila.setContents(Guardioes.mochila(g));
            invs.add(mochila);
        }
        for (Inventory inv : invs) {
            ItemStack[] c = inv.getContents();
            for (int k = 0; k < c.length && falta > 0; k++) {
                ItemStack i = c[k];
                if (i == null || i.isEmpty() || !tipo.test(i.getType()) || !comum(i)) continue;
                if (qual == null) qual = i.getType();
                if (i.getType() != qual) continue;
                int tira = Math.min(falta, i.getAmount());
                i.setAmount(i.getAmount() - tira);
                falta -= tira;
            }
            inv.setContents(c);
        }
        if (mochila != null) Guardioes.guardarMochila(g, mochila.getContents());
        return qual;
    }

    private static boolean comum(ItemStack i) {
        return !i.hasItemMeta() || i.getItemMeta().getPersistentDataContainer().getKeys().isEmpty();
    }

    /** Guarda no baú do posto; sem lugar, na mochila (nível III+). @return o que não coube. */
    static List<ItemStack> guardar(Entity g, Location posto, double raio, Collection<ItemStack> itens) {
        List<ItemStack> sobra = new ArrayList<>();
        Container bau = bauPerto(posto, raio);
        for (ItemStack s : itens) {
            if (s == null || s.getAmount() <= 0) continue;
            if (bau == null) sobra.add(s);
            else sobra.addAll(bau.getInventory().addItem(s).values());
        }
        if (sobra.isEmpty() || Guardioes.nivel(g) < 3) return sobra;
        Inventory temp = Bukkit.createInventory(null, 27);
        temp.setContents(Guardioes.mochila(g));
        List<ItemStack> resto = new ArrayList<>();
        for (ItemStack s : sobra) resto.addAll(temp.addItem(s).values());
        Guardioes.guardarMochila(g, temp.getContents());
        return resto;
    }

    // =====================================================================
    //  Munição (Espírito, Torre, Curandeiro)
    // =====================================================================

    /** Munição aceita: normal e forte (ou null se não usa). */
    static Material[] municoes(Guardioes.Tipo t) {
        return switch (t) {
            case ESPIRITO -> new Material[]{Material.AMETHYST_SHARD, Material.ECHO_SHARD};
            case TORRE -> new Material[]{Material.BLAZE_POWDER, Material.FIRE_CHARGE};
            case CURANDEIRO -> new Material[]{Material.HONEY_BOTTLE, Material.GLISTERING_MELON_SLICE};
            default -> null;
        };
    }

    /** Quantos disparos cada unidade de munição rende. */
    static int disparosPor(Material m) {
        return switch (m) {
            case AMETHYST_SHARD -> 4;
            case ECHO_SHARD -> 8;
            case BLAZE_POWDER, FIRE_CHARGE -> 1;
            default -> 5;
        };
    }

    static Material municao(Entity g) {
        String s = g.getPersistentDataContainer().get(K_MUNICAO, PersistentDataType.STRING);
        return s == null ? null : Material.matchMaterial(s);
    }

    static int municaoQtd(Entity g) {
        return g.getPersistentDataContainer().getOrDefault(K_MUNICAO_QTD, PersistentDataType.INTEGER, 0);
    }

    static void definirMunicao(Entity g, Material m, int qtd) {
        if (m == null || qtd <= 0) {
            g.getPersistentDataContainer().remove(K_MUNICAO);
            g.getPersistentDataContainer().remove(K_MUNICAO_QTD);
            return;
        }
        g.getPersistentDataContainer().set(K_MUNICAO, PersistentDataType.STRING, m.name());
        g.getPersistentDataContainer().set(K_MUNICAO_QTD, PersistentDataType.INTEGER, qtd);
    }

    /**
     * Usa um disparo. @return 0 = sem munição (fraco), 1 = munição normal, 2 = munição forte.
     * Sem estoque, tenta recarregar do baú do posto (até 16).
     */
    static int disparar(Entity g, Guardioes.Tipo t, Location posto) {
        Material[] aceitas = municoes(t);
        if (aceitas == null) return 0;
        Material m = municao(g);
        int qtd = municaoQtd(g);
        if (m == null || qtd <= 0) {
            for (int k = aceitas.length - 1; k >= 0 && (m == null || qtd <= 0); k--) {
                Material a = aceitas[k];
                int tem = contar(g, posto, 8, x -> x == a);
                if (tem <= 0) continue;
                int pega = Math.min(16, tem);
                tirar(g, posto, 8, x -> x == a, pega);
                m = a;
                qtd = pega;
                definirMunicao(g, m, qtd);
            }
            if (m == null || qtd <= 0) return 0;
        }
        int disparos = g.getPersistentDataContainer().getOrDefault(K_DISPAROS, PersistentDataType.INTEGER, 0) + 1;
        if (disparos >= disparosPor(m)) {
            disparos = 0;
            definirMunicao(g, m, qtd - 1);
        }
        g.getPersistentDataContainer().set(K_DISPAROS, PersistentDataType.INTEGER, disparos);
        return m == aceitas[aceitas.length - 1] ? 2 : 1;
    }
}
