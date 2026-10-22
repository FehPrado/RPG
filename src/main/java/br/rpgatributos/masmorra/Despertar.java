package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O Despertar (2.72): ao virar caçador D o jogador desperta e ganha uma característica sorteada;
 * no B ganha a segunda e no S a terceira. Elas valem nos portais e ficam para sempre (o
 * Re-despertar sorteia de novo, pagando mana).
 */
public final class Despertar {

    public enum Raridade {
        COMUM("Comum", 0xB0BEC5, 55), RARA("Rara", 0x4FC3F7, 33), EPICA("Épica", 0xBA68C8, 12);

        public final String nome;
        public final TextColor cor;
        final int peso;

        Raridade(String nome, int cor, int peso) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.peso = peso;
        }
    }

    public enum Caracteristica {
        PELE_DE_ACO("Pele de Aço", Raridade.COMUM, Material.IRON_CHESTPLATE, "-8% do dano dos monstros dos portais."),
        OLHOS_DE_GATO("Olhos de Gato", Raridade.COMUM, Material.ENDER_EYE, "Visão noturna dentro dos portais."),
        PES_LIGEIROS("Pés Ligeiros", Raridade.COMUM, Material.SUGAR, "Velocidade I dentro dos portais."),
        MANA_FARTA("Mana Farta", Raridade.COMUM, Material.AMETHYST_CLUSTER, "+20% de chance de Cristal de Mana dos monstros."),
        SEGUNDA_CHANCE("Segunda Chance", Raridade.RARA, Material.TOTEM_OF_UNDYING, "Uma vez por portal, um golpe fatal te deixa com 3 ❤."),
        VAMPIRICO("Vampírico", Raridade.RARA, Material.REDSTONE, "Cura 4% do dano que você causa nos monstros dos portais."),
        SANGUE_FRIO("Sangue Frio", Raridade.RARA, Material.BLUE_ICE, "+12% de dano com menos da metade da vida."),
        INSTINTO("Instinto", Raridade.RARA, Material.TRIPWIRE_HOOK, "Armadilhas brilham para você (até 10 blocos)."),
        CACADOR_NATO("Caçador Nato", Raridade.EPICA, Material.NAME_TAG, "+15% de XP de caçador."),
        REI_DOS_BAUS("Rei dos Baús", Raridade.EPICA, Material.ENDER_CHEST, "+1 sorteio no tesouro final."),
        INABALAVEL("Inabalável", Raridade.EPICA, Material.OBSIDIAN, "Imune a lentidão, fraqueza e veneno dos monstros dos portais."),
        ASSASSINO("Assassino", Raridade.EPICA, Material.NETHERITE_SWORD, "+50% de dano no primeiro golpe em monstro de vida cheia.");

        public final String nome, descricao;
        public final Raridade raridade;
        public final Material icone;

        Caracteristica(String nome, Raridade raridade, Material icone, String descricao) {
            this.nome = nome;
            this.raridade = raridade;
            this.icone = icone;
            this.descricao = descricao;
        }
    }

    /** Re-despertar: troca todas as características. */
    public static final int PRECO_REDESPERTAR = 48;
    private static final NamespacedKey K = new NamespacedKey("rpgatributos", "cacador_caracteristicas");
    private static final Map<UUID, List<Caracteristica>> CACHE = new HashMap<>();

    private Despertar() { }

    /** Quantas características o rank dá: D 1, B 2, S 3. */
    public static int vagas(Rank r) {
        return r.ordinal() >= Rank.S.ordinal() ? 3 : r.ordinal() >= Rank.B.ordinal() ? 2 : r.ordinal() >= Rank.D.ordinal() ? 1 : 0;
    }

    public static List<Caracteristica> de(Player p) {
        return CACHE.computeIfAbsent(p.getUniqueId(), id -> ler(p.getPersistentDataContainer().get(K, PersistentDataType.STRING)));
    }

    public static boolean tem(Player p, Caracteristica c) {
        return de(p).contains(c);
    }

    public static boolean despertou(Player p) {
        return !de(p).isEmpty();
    }

    /** Dá as características que faltam para o rank (ao subir de rank e ao entrar no servidor). */
    public static void verificar(Player p) {
        List<Caracteristica> atuais = new ArrayList<>(de(p));
        int vagas = vagas(Cacador.rank(p));
        if (atuais.size() >= vagas) return;
        boolean primeira = atuais.isEmpty();
        List<Caracteristica> novas = new ArrayList<>();
        while (atuais.size() < vagas) {
            Caracteristica c = sortear(atuais);
            if (c == null) break;
            atuais.add(c);
            novas.add(c);
        }
        gravar(p, atuais);
        anunciar(p, novas, primeira ? "DESPERTAR" : "NOVA CARACTERÍSTICA");
    }

    /** Sorteia tudo de novo (mesma quantidade). */
    public static void redespertar(Player p) {
        int n = de(p).size();
        List<Caracteristica> novas = new ArrayList<>();
        while (novas.size() < n) {
            Caracteristica c = sortear(novas);
            if (c == null) break;
            novas.add(c);
        }
        gravar(p, novas);
        anunciar(p, novas, "RE-DESPERTAR");
    }

    public static void esquecer(UUID id) {
        CACHE.remove(id);
    }

    /** Admin: define as características (lista vazia = zera). */
    public static void definir(Player p, List<Caracteristica> l) {
        gravar(p, new ArrayList<>(l));
    }

    private static Caracteristica sortear(List<Caracteristica> fora) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        EnumSet<Caracteristica> livres = EnumSet.allOf(Caracteristica.class);
        fora.forEach(livres::remove);
        if (livres.isEmpty()) return null;
        int total = 0;
        for (Raridade x : Raridade.values()) total += x.peso;
        int sorte = r.nextInt(total);
        Raridade escolhida = Raridade.COMUM;
        for (Raridade x : Raridade.values()) {
            if (sorte < x.peso) { escolhida = x; break; }
            sorte -= x.peso;
        }
        List<Caracteristica> daRaridade = new ArrayList<>();
        for (Caracteristica c : livres) if (c.raridade == escolhida) daRaridade.add(c);
        List<Caracteristica> de = daRaridade.isEmpty() ? new ArrayList<>(livres) : daRaridade;
        return de.get(r.nextInt(de.size()));
    }

    private static void anunciar(Player p, List<Caracteristica> novas, String titulo) {
        if (novas.isEmpty()) return;
        Caracteristica c = novas.getLast();
        p.showTitle(Title.title(Component.text("[Sistema] " + titulo, NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text(c.nome + " (" + c.raridade.nome + ")", c.raridade.cor, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
        List<Component> linhas = new ArrayList<>();
        for (Caracteristica n : novas) {
            linhas.add(Component.text("Característica: ", NamedTextColor.WHITE)
                    .append(Component.text(n.nome, n.raridade.cor, TextDecoration.BOLD))
                    .append(Component.text(" (" + n.raridade.nome + ") — " + n.descricao, NamedTextColor.GRAY)));
        }
        linhas.add(Component.text("Veja tudo em /masmorra arvore.", NamedTextColor.DARK_GRAY));
        JanelaSistema.mostrar(p, titulo.charAt(0) + titulo.substring(1).toLowerCase(java.util.Locale.ROOT), linhas);
    }

    private static void gravar(Player p, List<Caracteristica> l) {
        if (l.isEmpty()) p.getPersistentDataContainer().remove(K);
        else p.getPersistentDataContainer().set(K, PersistentDataType.STRING, String.join(",", l.stream().map(Enum::name).toList()));
        CACHE.put(p.getUniqueId(), List.copyOf(l));
    }

    private static List<Caracteristica> ler(String s) {
        List<Caracteristica> l = new ArrayList<>();
        if (s == null || s.isBlank()) return List.of();
        for (String x : s.split(",")) {
            try {
                Caracteristica c = Caracteristica.valueOf(x);
                if (!l.contains(c)) l.add(c);
            } catch (IllegalArgumentException ignorado) { }
        }
        return List.copyOf(l);
    }
}
