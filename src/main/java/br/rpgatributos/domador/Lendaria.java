package br.rpgatributos.domador;

import br.rpgatributos.aventura.Raro;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Montarias lendárias (os Guardiões). Cada uma é única no servidor: nasce de um Ovo Lendário,
 * que sai da fusão de dois companheiros no estágio III com itens raros no Altar do Domador, e
 * choca num lugar que combina com ela. Já nascem com alguns poderes (como componentes que não
 * ocupam espaço nem caem quando ela morre).
 */
public enum Lendaria {
    QILIN("Qilin", "麒麟", 0x7FD8A6, VisualDomador.Armadura.QILIN,
            EntityType.HORSE, EntityType.FOX,
            new Object[]{Raro.ESSENCIA_PRIMORDIAL, 1, Material.EMERALD_BLOCK, 2, Material.GOLDEN_APPLE, 1},
            EnumSet.of(Componente.SELA, Componente.CHAMAS, Componente.MARE),
            "O guardião do centro. Aparece onde há paz e anda sobre a água e a lava sem se queimar.",
            "Choca num campo de flores (8 flores perto), de dia.",
            new String[]{
                    "Anda sobre a água e a lava; imune a fogo",
                    "Quem monta não se queima",
                    "Flores brotam onde ele passa",
                    "Bênção: você e a party perto se curam devagar"}),
    FENGHUANG("Fenghuang", "凤凰", 0xFF7A3C, VisualDomador.Armadura.FENGHUANG,
            EntityType.HORSE, EntityType.PARROT,
            new Object[]{Raro.FRAGMENTO_CELESTE, 1, Material.BLAZE_ROD, 8, Material.GOLDEN_APPLE, 1},
            EnumSet.of(Componente.SELA, Componente.ASAS, Componente.CHAMAS),
            "O guardião do sul. Uma fênix que nunca morre de verdade: renasce do próprio fogo.",
            "Choca perto de fogo, lava, fogueira ou magma (até 2 blocos).",
            new String[]{
                    "Voa: espaço sobe, agachar desce",
                    "Imune a fogo; os ataques incendeiam",
                    "Deixa um rastro de brasas no ar",
                    "Renascer: ao morrer vira um ovo em chamas (com o mesmo nível)"}),
    LONGMA("Longma", "龙马", 0x4FA3E0, VisualDomador.Armadura.LONGMA,
            EntityType.HORSE, EntityType.ARMADILLO,
            new Object[]{Material.HEART_OF_THE_SEA, 1, Material.PRISMARINE_CRYSTALS, 16, Material.GOLDEN_APPLE, 1},
            EnumSet.of(Componente.SELA, Componente.MARE),
            "O guardião do leste. Metade cavalo, metade dragão: saiu do rio trazendo nas costas o desenho do mundo.",
            "Choca na beira da água (6 blocos de água perto).",
            new String[]{
                    "Galopa por cima da água; nada rápido e quem monta respira debaixo d'água",
                    "Onda: montado, empurra para longe os monstros em volta"}),
    BAIHU("Baihu", "白虎", 0xE8EEF5, VisualDomador.Armadura.BAIHU,
            EntityType.WOLF, EntityType.CAT,
            new Object[]{Material.GOAT_HORN, 1, Material.BONE, 16, Material.GOLDEN_APPLE, 1},
            EnumSet.of(Componente.SELA, Componente.AGILIDADE, Componente.GARRAS),
            "O guardião do oeste. Um tigre branco das montanhas geladas, o mais feroz dos cinco.",
            "Choca na neve (num lugar frio ou com 6 blocos de neve perto).",
            new String[]{
                    "Um tigre que dá para montar: rápido e pula longe",
                    "Luta e bate forte (Garras)",
                    "Rugido: monstros perto ficam fracos e lentos"}),
    XUANWU("Xuanwu", "玄武", 0x3E4A3D, null,
            EntityType.TURTLE, EntityType.POLAR_BEAR,
            new Object[]{Material.TURTLE_SCUTE, 8, Material.NAUTILUS_SHELL, 1, Material.GOLDEN_APPLE, 1},
            EnumSet.of(Componente.SELA, Componente.MARE, Componente.COURACA, Componente.NETHERITE),
            "O guardião do norte. A tartaruga negra com uma serpente enrolada no casco: lenta e impossível de derrubar.",
            "Choca na areia, à noite (6 blocos de areia perto).",
            new String[]{
                    "Uma tartaruga gigante: lenta em terra, rápida na água",
                    "Casco: muita armadura, quase não é empurrado, imune a fogo",
                    "Escudo: você e a party perto recebem menos dano"});

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "lendaria");

    private final String nome, hanzi;
    private final int cor;
    private final VisualDomador.Armadura pele;
    private final EntityType base, segunda;
    private final Map<Object, Integer> itens = new LinkedHashMap<>();
    private final Set<Componente> poderes;
    private final String historia, chocar;
    private final String[] descricao;

    Lendaria(String nome, String hanzi, int cor, VisualDomador.Armadura pele, EntityType base, EntityType segunda, Object[] itens,
             Set<Componente> poderes, String historia, String chocar, String[] descricao) {
        this.nome = nome;
        this.hanzi = hanzi;
        this.cor = cor;
        this.pele = pele;
        this.base = base;
        this.segunda = segunda;
        for (int i = 0; i + 1 < itens.length; i += 2) this.itens.put(itens[i], (Integer) itens[i + 1]);
        this.poderes = poderes;
        this.historia = historia;
        this.chocar = chocar;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    public String hanzi() { return hanzi; }
    public int cor() { return cor; }
    /** A pele pintada (null: a criatura não aceita pele pelo pacote, como a tartaruga). */
    VisualDomador.Armadura pele() { return pele; }
    /** Tamanho a mais (o lobo e a tartaruga crescem para dar para montar). */
    public double escala() {
        return switch (this) {
            case BAIHU -> 0.6;
            case XUANWU -> 1.6;
            default -> 0;
        };
    }
    /** A criatura que ela é (e o primeiro companheiro da fusão). */
    public EntityType base() { return base; }
    /** O segundo companheiro da fusão. */
    public EntityType segunda() { return segunda; }
    /** Itens da fusão: Raro ou Material → quantidade. */
    public Map<Object, Integer> itens() { return itens; }
    /** Componentes que ela já tem de nascença. */
    public Set<Componente> poderes() { return EnumSet.copyOf(poderes); }
    public String historia() { return historia; }
    public String chocar() { return chocar; }
    public String[] descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Lendaria de(Entity e) {
        String s = e == null ? null : e.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        return s == null ? null : porId(s);
    }

    public static Lendaria porId(String id) {
        for (Lendaria l : values()) if (l.name().equalsIgnoreCase(id) || l.id().equalsIgnoreCase(id)) return l;
        return null;
    }
}
