package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cristal de Portal (2.68): feito no Altar do Portal com uma oferenda. Guarda o rank, o objetivo e
 * os modificadores da masmorra que ele abre. Clique no altar com ele na mão para usar.
 */
public final class CristalPortal {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "cristal_portal");
    private static final NamespacedKey K_OBJETIVO = new NamespacedKey("rpgatributos", "cristal_objetivo");
    private static final NamespacedKey K_MODS = new NamespacedKey("rpgatributos", "cristal_mods");

    private static final NamespacedKey K_INSC = new NamespacedKey("rpgatributos", "cristal_inscricoes");

    /** {@code insc}: as Inscrições aplicadas na Mesa de Inscrição (2.72). */
    public record Dados(Rank rank, Objetivo objetivo, EnumSet<ModificadorPortal> mods, EnumMap<Inscricao, Integer> insc) {
        public Dados(Rank rank, Objetivo objetivo, EnumSet<ModificadorPortal> mods) {
            this(rank, objetivo, mods, new EnumMap<>(Inscricao.class));
        }

        public int quantas(Inscricao i) { return insc.getOrDefault(i, 0); }

        /** Os minutos do portal: 15, -4 com Relógio Curto, +3 por Inscrição de Tempo. */
        public int minutos() {
            return 15 - (mods.contains(ModificadorPortal.RELOGIO_CURTO) ? 4 : 0) + 3 * quantas(Inscricao.TEMPO);
        }
    }

    private CristalPortal() { }

    public static ItemStack criar(Rank r, Objetivo o, EnumSet<ModificadorPortal> mods) {
        return criar(new Dados(r, o, mods));
    }

    public static ItemStack criar(Dados d) {
        Rank r = d.rank();
        Objetivo o = d.objetivo();
        ItemStack i = new ItemStack(Material.AMETHYST_SHARD);
        i.editMeta(m -> {
            m.itemName(Component.text("Cristal de Portal · " + r.nome(), r.cor()));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(r.nomeCompleto(), r.cor()));
            lore.add(Component.text("Objetivo: " + o.nome(), NamedTextColor.WHITE));
            lore.add(Component.text(o.descricao(), NamedTextColor.GRAY));
            if (!d.mods().isEmpty()) {
                lore.add(Component.empty());
                for (ModificadorPortal x : d.mods()) {
                    lore.add(Component.text((x.bom() ? "✚ " : "☠ ") + x.nome() + ": " + x.descricao(), x.bom() ? NamedTextColor.GREEN : NamedTextColor.RED));
                }
            }
            int n = Inscricao.total(d.insc());
            if (n > 0) {
                lore.add(Component.empty());
                lore.add(Component.text("✎ Inscrições (" + n + "/" + Inscricao.MAXIMO + ")", NamedTextColor.AQUA));
                if (d.quantas(Inscricao.TEMPO) > 0) lore.add(Component.text("  +" + 3 * d.quantas(Inscricao.TEMPO) + " minutos", NamedTextColor.AQUA));
                if (d.quantas(Inscricao.TESOURO) > 0) lore.add(Component.text("  +" + d.quantas(Inscricao.TESOURO) + " sala(s) de tesouro", NamedTextColor.AQUA));
                if (d.quantas(Inscricao.MINA) > 0) lore.add(Component.text("  +" + d.quantas(Inscricao.MINA) + " Mina(s) de Cristal", NamedTextColor.AQUA));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Clique no Altar do Portal com ele na mão.", NamedTextColor.DARK_GRAY));
            lore.add(Component.text(d.minutos() + " minutos. Não saiu a tempo: o que pegou lá dentro fica.", NamedTextColor.DARK_GRAY));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, r.name());
            m.getPersistentDataContainer().set(K_OBJETIVO, PersistentDataType.STRING, o.name());
            m.getPersistentDataContainer().set(K_MODS, PersistentDataType.STRING, ModificadorPortal.texto(d.mods()));
            if (n > 0) m.getPersistentDataContainer().set(K_INSC, PersistentDataType.STRING, Inscricao.texto(d.insc()));
        });
        EquipamentoPortal.aplicarVisual(i);
        return i;
    }

    public static Dados ler(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        var pdc = i.getPersistentDataContainer();
        Rank r = Rank.porId(pdc.get(CHAVE, PersistentDataType.STRING));
        if (r == null) return null;
        Objetivo o = Objetivo.porId(pdc.get(K_OBJETIVO, PersistentDataType.STRING));
        return new Dados(r, o == null ? Objetivo.CACADA : o, ModificadorPortal.ler(pdc.get(K_MODS, PersistentDataType.STRING)),
                Inscricao.ler(pdc.get(K_INSC, PersistentDataType.STRING)));
    }

    /** 2.72: o motivo de a Inscrição não caber neste cristal, ou null. */
    public static String impedimento(Dados d, Inscricao i) {
        if (Inscricao.total(d.insc()) >= Inscricao.MAXIMO) return "Este cristal já tem " + Inscricao.MAXIMO + " inscrições.";
        if (d.quantas(i) >= i.maximo()) return "Este cristal já tem o máximo dessa inscrição.";
        if (i.mod() != null && d.mods().contains(i.mod())) return "Este cristal já tem " + i.mod().nome() + ".";
        if (i.mod() != null && d.mods().size() >= 4) return "Este cristal já tem 4 modificadores.";
        if (i.objetivo() != null && i.objetivo() == d.objetivo()) return "O objetivo já é " + d.objetivo().nome() + ".";
        return null;
    }

    /** Os dados do cristal com a Inscrição aplicada (confira {@link #impedimento} antes). */
    public static Dados inscrever(Dados d, Inscricao i) {
        EnumSet<ModificadorPortal> mods = d.mods().isEmpty() ? EnumSet.noneOf(ModificadorPortal.class) : EnumSet.copyOf(d.mods());
        if (i.mod() != null) mods.add(i.mod());
        EnumMap<Inscricao, Integer> insc = new EnumMap<>(d.insc());
        insc.merge(i, 1, Integer::sum);
        return new Dados(d.rank(), i.objetivo() != null ? i.objetivo() : d.objetivo(), mods, insc);
    }

    /** Um cristal novo: objetivo sorteado e de 0 a 2 modificadores. */
    public static ItemStack sortear(Rank r) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Objetivo[] os = Objetivo.values();
        Objetivo o = os[rnd.nextInt(os.length)];
        EnumSet<ModificadorPortal> mods = EnumSet.noneOf(ModificadorPortal.class);
        int n = rnd.nextDouble() < 0.35 ? 0 : rnd.nextDouble() < 0.6 ? 1 : 2;
        ModificadorPortal[] ms = ModificadorPortal.values();
        while (mods.size() < n) mods.add(ms[rnd.nextInt(ms.length)]);
        return criar(r, o, mods);
    }

    // =====================================================================
    //  Oferenda: o altar pede 3 coisas sorteadas de cada rank (fica a mesma até ser paga)
    // =====================================================================

    private static final Object[][][] POOL = {
            /* E */ {{Material.BONE, 16}, {Material.ROTTEN_FLESH, 16}, {Material.COBBLESTONE, 32}, {Material.IRON_INGOT, 4}, {Material.STRING, 12}, {Material.WHEAT, 16}},
            /* D */ {{Material.IRON_INGOT, 8}, {Material.ARROW, 24}, {Material.GOLD_INGOT, 4}, {Material.GUNPOWDER, 8}, {Material.SPIDER_EYE, 6}, {Material.LEATHER, 8}},
            /* C */ {{Material.DIAMOND, 2}, {Material.LAPIS_LAZULI, 16}, {Material.GOLD_INGOT, 8}, {Material.ENDER_PEARL, 4}, {Material.REDSTONE, 24}, {Material.SLIME_BALL, 6}},
            /* B */ {{Material.DIAMOND, 4}, {Material.ENDER_PEARL, 8}, {Material.BLAZE_ROD, 4}, {Material.OBSIDIAN, 8}, {Material.EMERALD, 12}, {Material.MAGMA_CREAM, 6}},
            /* A */ {{Material.DIAMOND, 8}, {Material.NETHERITE_SCRAP, 2}, {Material.GHAST_TEAR, 4}, {Material.CRYING_OBSIDIAN, 8}, {Material.EMERALD, 24}, {Material.PHANTOM_MEMBRANE, 6}},
            /* S */ {{Material.NETHERITE_INGOT, 1}, {Material.DIAMOND, 12}, {Material.GHAST_TEAR, 6}, {Material.GOLDEN_APPLE, 4}, {Material.ECHO_SHARD, 2}, {Material.SHULKER_SHELL, 2}}};

    /** O valor em Cristais de Mana que paga o cristal no lugar da oferenda. */
    public static int valorMana(Rank r) {
        return new int[]{4, 8, 16, 32, 64, 128}[r.ordinal()];
    }

    private static NamespacedKey chaveOferenda(Rank r) {
        return new NamespacedKey("rpgatributos", "oferenda_" + r.id());
    }

    /** A oferenda que o altar pede deste jogador para o rank (sorteia na primeira vez). */
    public static List<ItemStack> oferenda(Player p, Rank r) {
        String s = p.getPersistentDataContainer().get(chaveOferenda(r), PersistentDataType.STRING);
        if (s == null) {
            List<Integer> idx = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
            java.util.Collections.shuffle(idx);
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < 3; k++) {
                Object[] it = POOL[r.ordinal()][idx.get(k)];
                if (!b.isEmpty()) b.append(';');
                b.append(((Material) it[0]).name()).append(':').append(it[1]);
            }
            s = b.toString();
            p.getPersistentDataContainer().set(chaveOferenda(r), PersistentDataType.STRING, s);
        }
        List<ItemStack> l = new ArrayList<>();
        for (String x : s.split(";")) {
            String[] kv = x.split(":");
            Material m = Material.matchMaterial(kv[0]);
            if (m != null) l.add(new ItemStack(m, Integer.parseInt(kv[1])));
        }
        return l;
    }

    public static boolean temOferenda(Player p, Rank r) {
        for (ItemStack i : oferenda(p, r)) if (!p.getInventory().containsAtLeast(new ItemStack(i.getType()), i.getAmount())) return false;
        return true;
    }

    /** Tira a oferenda do inventário e sorteia a próxima. */
    public static void pagarOferenda(Player p, Rank r) {
        for (ItemStack i : oferenda(p, r)) p.getInventory().removeItem(new ItemStack(i.getType(), i.getAmount()));
        p.getPersistentDataContainer().remove(chaveOferenda(r));
    }
}
