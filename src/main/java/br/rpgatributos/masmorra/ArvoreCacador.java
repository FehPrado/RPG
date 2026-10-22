package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Árvore de Caçador (2.72): pontos que vêm do Rank de Caçador, gastos em 3 ramos (Lâmina, Escudo e
 * Fortuna). Os bônus valem só nos portais. Os efeitos ficam no {@link PoderCacador}; o menu, no
 * {@link SistemaCacador}.
 */
public final class ArvoreCacador {

    public enum Ramo {
        LAMINA("Lâmina", Material.IRON_SWORD, 0xE57373),
        ESCUDO("Escudo", Material.SHIELD, 0x64B5F6),
        FORTUNA("Fortuna", Material.GOLD_INGOT, 0xFFD54F);

        public final String nome;
        public final Material icone;
        public final TextColor cor;

        Ramo(String nome, Material icone, int cor) {
            this.nome = nome;
            this.icone = icone;
            this.cor = TextColor.color(cor);
        }
    }

    /** Os nós: ramo, nome, graus, rank mínimo e o que fazem. */
    public enum No {
        GOLPE(Ramo.LAMINA, "Golpe de Caçador", 3, Rank.E, Material.IRON_SWORD, "+4% de dano nos monstros dos portais por grau."),
        EXECUCAO(Ramo.LAMINA, "Execução", 2, Rank.D, Material.IRON_AXE, "+10% de dano por grau em monstros com menos de 35% de vida."),
        CACA_CHEFE(Ramo.LAMINA, "Caça-Chefes", 2, Rank.C, Material.WITHER_SKELETON_SKULL, "+6% de dano por grau no chefe do portal."),
        FRENESI(Ramo.LAMINA, "Frenesi", 1, Rank.B, Material.BLAZE_POWDER, "Abater um monstro do portal dá Força I por 4 s."),
        PELE(Ramo.ESCUDO, "Pele Grossa", 3, Rank.E, Material.LEATHER_CHESTPLATE, "-4% do dano dos monstros dos portais por grau."),
        VIGOR(Ramo.ESCUDO, "Vigor do Portal", 2, Rank.D, Material.GOLDEN_APPLE, "+1 ❤ de vida máxima por grau dentro dos portais."),
        RECUPERACAO(Ramo.ESCUDO, "Recuperação", 2, Rank.C, Material.GLISTERING_MELON_SLICE, "Abater um monstro do portal cura meio ❤ por grau."),
        PASSO_LEVE(Ramo.ESCUDO, "Passo Leve", 1, Rank.B, Material.FEATHER, "Armadilhas dão metade do dano e não deixam lento."),
        OLHO_MANA(Ramo.FORTUNA, "Olho de Mana", 3, Rank.E, Material.AMETHYST_SHARD, "+15% de chance por grau de Cristal de Mana dos monstros."),
        SAQUEADOR(Ramo.FORTUNA, "Saqueador", 2, Rank.D, Material.CHEST, "+1 sorteio por grau no tesouro final."),
        FARO(Ramo.FORTUNA, "Faro", 1, Rank.C, Material.SPYGLASS, "Paredes rachadas e mímicos brilham para você (até 10 blocos)."),
        SORTE(Ramo.FORTUNA, "Sorte do Avaliador", 2, Rank.B, Material.EMERALD, "+5% por grau de subir a raridade ao revelar um item.");

        public final Ramo ramo;
        public final String nome, descricao;
        public final int maximo;
        public final Rank rank;
        public final Material icone;

        No(Ramo ramo, String nome, int maximo, Rank rank, Material icone, String descricao) {
            this.ramo = ramo;
            this.nome = nome;
            this.maximo = maximo;
            this.rank = rank;
            this.icone = icone;
            this.descricao = descricao;
        }
    }

    private static final NamespacedKey K = new NamespacedKey("rpgatributos", "cacador_arvore");
    private static final Map<UUID, EnumMap<No, Integer>> CACHE = new HashMap<>();

    private ArvoreCacador() { }

    /** Pontos da árvore: 1 no E, +3 por rank e, no S, +1 a cada 500 XP (até +10). */
    public static int pontos(Player p) {
        Rank r = Cacador.rank(p);
        int extra = r == Rank.S ? Math.min(10, Math.max(0, Cacador.xp(p) - 3000) / 500) : 0;
        return 1 + 3 * r.ordinal() + extra;
    }

    public static EnumMap<No, Integer> graus(Player p) {
        return CACHE.computeIfAbsent(p.getUniqueId(), id -> ler(p.getPersistentDataContainer().get(K, PersistentDataType.STRING)));
    }

    public static int grau(Player p, No n) {
        return graus(p).getOrDefault(n, 0);
    }

    public static int usados(Player p) {
        int s = 0;
        for (int v : graus(p).values()) s += v;
        return s;
    }

    public static int livres(Player p) {
        return pontos(p) - usados(p);
    }

    /** @return o motivo de não poder subir esse nó, ou null. */
    public static String impedimento(Player p, No n) {
        if (grau(p, n) >= n.maximo) return "Esse nó já está no máximo.";
        if (Cacador.rank(p).ordinal() < n.rank.ordinal()) return "Precisa ser caçador " + n.rank.nome() + ".";
        if (livres(p) <= 0) return "Sem pontos livres. Suba de rank para ganhar mais.";
        return null;
    }

    public static void subir(Player p, No n) {
        EnumMap<No, Integer> g = new EnumMap<>(graus(p));
        g.merge(n, 1, Integer::sum);
        gravar(p, g);
    }

    public static void refazer(Player p) {
        gravar(p, new EnumMap<>(No.class));
    }

    /** Se o rank caiu (admin), a árvore volta a zero para não sobrar ponto gasto a mais. */
    public static void conferir(Player p) {
        if (usados(p) > pontos(p)) {
            refazer(p);
            p.sendMessage(Component.text("[Sistema] Sua Árvore de Caçador foi refeita (os pontos mudaram).", NamedTextColor.AQUA));
        }
    }

    public static void esquecer(UUID id) {
        CACHE.remove(id);
    }

    private static void gravar(Player p, EnumMap<No, Integer> g) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<No, Integer> e : g.entrySet()) {
            if (e.getValue() <= 0) continue;
            if (!sb.isEmpty()) sb.append(',');
            sb.append(e.getKey().name()).append(':').append(e.getValue());
        }
        if (sb.isEmpty()) p.getPersistentDataContainer().remove(K);
        else p.getPersistentDataContainer().set(K, PersistentDataType.STRING, sb.toString());
        CACHE.put(p.getUniqueId(), g);
    }

    private static EnumMap<No, Integer> ler(String s) {
        EnumMap<No, Integer> m = new EnumMap<>(No.class);
        if (s == null || s.isBlank()) return m;
        for (String par : s.split(",")) {
            String[] x = par.split(":");
            try {
                No n = No.valueOf(x[0]);
                m.put(n, Math.min(n.maximo, Integer.parseInt(x[1])));
            } catch (RuntimeException ignorado) { }
        }
        return m;
    }
}
