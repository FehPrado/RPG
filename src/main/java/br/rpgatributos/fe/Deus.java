package br.rpgatributos.fe;

import br.rpgatributos.Skill;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Os seis deuses. Cada um gosta de certas oferendas (jogadas com Q num Santuário dos
 * Deuses) e dá devoção também quando você usa os atributos dele. A devoção sobe o nível
 * de fé (1 a 5), que fortalece o passivo; do nível 2 em diante dá para pedir o milagre.
 */
public enum Deus {
    FERRUM("Ferrum", "o Deus da Forja", "⚒", 0xE8823A, Material.ANVIL, EnumSet.of(Skill.FERRARIA, Skill.MINERACAO),
            Map.ofEntries(Map.entry(Material.IRON_INGOT, 2), Map.entry(Material.GOLD_INGOT, 3), Map.entry(Material.COPPER_INGOT, 1),
                    Map.entry(Material.IRON_BLOCK, 18), Map.entry(Material.GOLD_BLOCK, 27), Map.entry(Material.DIAMOND, 15),
                    Map.entry(Material.DIAMOND_BLOCK, 135), Map.entry(Material.NETHERITE_SCRAP, 25), Map.entry(Material.NETHERITE_INGOT, 100),
                    Map.entry(Material.COAL, 1), Map.entry(Material.ANVIL, 40)),
            "+armadura e mineração mais rápida",
            "Bênção da Bigorna", "Conserta todo o equipamento que você usa e dá Resistência II por 30 s."),
    SYLVA("Sylva", "a Deusa da Natureza", "☘", 0x4CAF50, Material.FLOWERING_AZALEA, EnumSet.of(Skill.AGRICULTURA, Skill.MADEIRA, Skill.DOMA),
            Map.ofEntries(Map.entry(Material.WHEAT, 1), Map.entry(Material.CARROT, 1), Map.entry(Material.POTATO, 1), Map.entry(Material.BEETROOT, 1),
                    Map.entry(Material.MELON_SLICE, 1), Map.entry(Material.PUMPKIN, 2), Map.entry(Material.APPLE, 2), Map.entry(Material.BONE_MEAL, 1),
                    Map.entry(Material.HAY_BLOCK, 9), Map.entry(Material.GOLDEN_CARROT, 5), Map.entry(Material.GOLDEN_APPLE, 20),
                    Map.entry(Material.HONEY_BOTTLE, 4), Map.entry(Material.SWEET_BERRIES, 1)),
            "+vida máxima e se cura aos poucos",
            "Florescer", "Todas as plantações perto amadurecem na hora e você e a party se curam."),
    MARIS("Maris", "a Deusa do Mar", "≈", 0x29B6F6, Material.NAUTILUS_SHELL, EnumSet.of(Skill.PESCA, Skill.NATACAO),
            Map.ofEntries(Map.entry(Material.COD, 1), Map.entry(Material.SALMON, 2), Map.entry(Material.TROPICAL_FISH, 3),
                    Map.entry(Material.PUFFERFISH, 3), Map.entry(Material.PRISMARINE_SHARD, 2), Map.entry(Material.PRISMARINE_CRYSTALS, 2),
                    Map.entry(Material.NAUTILUS_SHELL, 15), Map.entry(Material.HEART_OF_THE_SEA, 60), Map.entry(Material.KELP, 1),
                    Map.entry(Material.SEA_PICKLE, 2), Map.entry(Material.INK_SAC, 1)),
            "+fôlego e agilidade na água (Graça do golfinho no nível 3)",
            "Maré Divina", "Chama a chuva e você ganha Poder do conduíte e Graça do golfinho por 5 min."),
    BELLUM("Bellum", "o Deus da Guerra", "⚔", 0xE53935, Material.IRON_SWORD, EnumSet.of(Skill.COMBATE, Skill.CORRIDA),
            Map.ofEntries(Map.entry(Material.ROTTEN_FLESH, 1), Map.entry(Material.BONE, 1), Map.entry(Material.STRING, 1),
                    Map.entry(Material.GUNPOWDER, 2), Map.entry(Material.SPIDER_EYE, 2), Map.entry(Material.ENDER_PEARL, 4),
                    Map.entry(Material.BLAZE_ROD, 5), Map.entry(Material.GHAST_TEAR, 10), Map.entry(Material.PHANTOM_MEMBRANE, 4),
                    Map.entry(Material.WITHER_SKELETON_SKULL, 40), Map.entry(Material.IRON_SWORD, 6)),
            "+dano corpo a corpo (+1 ❤ no nível 5)",
            "Fúria de Bellum", "Você e a party perto ganham Força II e Resistência por 60 s."),
    ARCANUS("Arcanus", "o Deus da Magia", "✦", 0x8E24AA, Material.ENCHANTED_BOOK, EnumSet.of(Skill.ARCANO, Skill.ALQUIMIA),
            Map.ofEntries(Map.entry(Material.LAPIS_LAZULI, 2), Map.entry(Material.REDSTONE, 1), Map.entry(Material.GLOWSTONE_DUST, 2),
                    Map.entry(Material.AMETHYST_SHARD, 2), Map.entry(Material.BLAZE_POWDER, 3), Map.entry(Material.EXPERIENCE_BOTTLE, 6),
                    Map.entry(Material.ENCHANTED_BOOK, 20), Map.entry(Material.DRAGON_BREATH, 15), Map.entry(Material.LAPIS_BLOCK, 18),
                    Map.entry(Material.ENDER_EYE, 8)),
            "+mana máxima e mana por segundo",
            "Maré Arcana", "Enche a sua mana e dá +5 de mana por segundo por 60 s."),
    MORTIS("Mortis", "a Deusa da Morte", "☠", 0x78909C, Material.WITHER_ROSE, EnumSet.noneOf(Skill.class),
            Map.ofEntries(Map.entry(Material.BONE, 1), Map.entry(Material.ROTTEN_FLESH, 1), Map.entry(Material.BONE_BLOCK, 9),
                    Map.entry(Material.SOUL_SAND, 1), Map.entry(Material.SOUL_SOIL, 1), Map.entry(Material.SKELETON_SKULL, 15),
                    Map.entry(Material.ZOMBIE_HEAD, 15), Map.entry(Material.WITHER_ROSE, 20), Map.entry(Material.ECHO_SHARD, 30),
                    Map.entry(Material.WITHER_SKELETON_SKULL, 50)),
            "roubo de vida (mortos-vivos te ignoram no nível 5)",
            "Segunda Chance", "Pelos próximos 10 min, a primeira morte é evitada e você volta com metade da vida.");

    /** Devoção para chegar em cada nível de fé (índice = nível - 1). */
    public static final int[] DEVOCAO = {0, 100, 300, 700, 1500};

    private final String nome, titulo, simbolo, passivo, milagre, descricaoMilagre;
    private final TextColor cor;
    private final Material icone;
    private final Set<Skill> atributos;
    private final Map<Material, Integer> oferendas;

    Deus(String nome, String titulo, String simbolo, int cor, Material icone, Set<Skill> atributos, Map<Material, Integer> oferendas,
         String passivo, String milagre, String descricaoMilagre) {
        this.nome = nome;
        this.titulo = titulo;
        this.simbolo = simbolo;
        this.cor = TextColor.color(cor);
        this.icone = icone;
        this.atributos = atributos;
        this.oferendas = oferendas;
        this.passivo = passivo;
        this.milagre = milagre;
        this.descricaoMilagre = descricaoMilagre;
    }

    public String nome() { return nome; }
    public String titulo() { return titulo; }
    public String simbolo() { return simbolo; }
    public TextColor cor() { return cor; }
    public Material icone() { return icone; }
    /** Atributos cujo XP também dá devoção. */
    public Set<Skill> atributos() { return atributos; }
    /** Quanto de devoção vale cada item oferecido (0 = o deus não aceita). */
    public int valor(Material m) { return oferendas.getOrDefault(m, 0); }
    public Map<Material, Integer> oferendas() { return oferendas; }
    public String passivo() { return passivo; }
    public String milagre() { return milagre; }
    public String descricaoMilagre() { return descricaoMilagre; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static int nivel(double devocao) {
        int n = 1;
        for (int i = 1; i < DEVOCAO.length; i++) if (devocao >= DEVOCAO[i]) n = i + 1;
        return n;
    }

    public static Deus porId(String s) {
        for (Deus d : values()) if (d.id().equalsIgnoreCase(s) || d.nome.equalsIgnoreCase(s)) return d;
        return null;
    }
}
