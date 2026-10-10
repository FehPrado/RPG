package br.rpgatributos.forja;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Armas e ferramentas com nome: já nascem assim quando são forjadas (com sorte) ou encontradas
 * em baús e chefes. O nome, a textura e uma linha de história são só aparência (não dão bônus).
 * O desenho de cada uma fica no {@code ArteNomeadas} (molde + paleta).
 */
public enum Nomeada {
    // ---------- espadas ----------
    PRESA_DO_INVERNO("Presa do Inverno", Categoria.ESPADA, "espada", "Forjada no gelo da Expedição Perdida. Ainda está fria ao toque.",
            'o', 0xFF0D2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFB3E5FC, 'd', 0xFF4FC3F7, 'g', 0xFF90A4AE, 'w', 0xFF37474F, 'p', 0xFF80DEEA, 'a', 0xFFE1F5FE),
    LAMINA_DE_THALASSA("Lâmina de Thalassa", Categoria.ESPADA, "larga", "Dos guardas da cidade submersa. O sal nunca a enferrujou.",
            'o', 0xFF003A3A, 'h', 0xFFB2FFF0, 'm', 0xFF26C6DA, 'd', 0xFF00838F, 'g', 0xFFFF8A65, 'w', 0xFF00695C, 'p', 0xFFFF7043, 'a', 0xFFFFAB91),
    AURORA_DE_AERILIA("Aurora de Aerília", Categoria.ESPADA, "sabre", "Caiu de uma ilha do céu numa noite de tempestade.",
            'o', 0xFF3A2E0A, 'h', 0xFFFFFFFF, 'm', 0xFFFFF3C4, 'd', 0xFFE2B53C, 'g', 0xFFFFD54F, 'w', 0xFF8D6E63, 'p', 0xFF90CAF9, 'a', 0xFFFFF59D),
    JURAMENTO_DO_FORTIM("Juramento do Fortim", Categoria.ESPADA, "larga", "Os soldados do fortim juravam por ela antes da última batalha.",
            'o', 0xFF1A1A1A, 'h', 0xFFB0BEC5, 'm', 0xFF607D8B, 'd', 0xFF37474F, 'g', 0xFF8B0000, 'w', 0xFF3E2723, 'p', 0xFFB71C1C, 'a', 0xFFE53935),
    BRASA_DOS_ANOES("Brasa dos Anões", Categoria.ESPADA, "espada", "Saiu da forja que nunca apagou, debaixo da montanha.",
            'o', 0xFF2A1000, 'h', 0xFFFFE0B2, 'm', 0xFFFF8F00, 'd', 0xFFBF360C, 'g', 0xFF795548, 'w', 0xFF4E342E, 'p', 0xFFFFC107, 'a', 0xFFFF6F00),
    SUSSURRO_DA_PENUMBRA("Sussurro da Penumbra", Categoria.ESPADA, "sabre", "Achada no escuro, abaixo das raízes. Ninguém lembra de forjá-la.",
            'o', 0xFF0A0614, 'h', 0xFFB39DDB, 'm', 0xFF5E35B1, 'd', 0xFF311B92, 'g', 0xFF212121, 'w', 0xFF1A1226, 'p', 0xFF7C4DFF, 'a', 0xFF7C4DFF),
    // ---------- machados e maças ----------
    TALHA_RAIZES("Talha-Raízes", Categoria.MACHADO, "machado", "Do lenhador que abriu a primeira estrada no meio da mata.",
            'o', 0xFF1B2A10, 'h', 0xFFDCE775, 'm', 0xFF9E9D24, 'd', 0xFF5D5A0E, 'w', 0xFF6D4C41, 'a', 0xFF43A047),
    FURIA_DO_SAQUEADOR("Fúria do Saqueador", Categoria.MACHADO, "machado2", "Dos bandidos que queimaram a vila. Ainda cheira a fumaça.",
            'o', 0xFF1A0000, 'h', 0xFFBDBDBD, 'm', 0xFF757575, 'd', 0xFF424242, 'w', 0xFF3E2723, 'a', 0xFFC62828),
    MACHADO_DO_FAROLEIRO("Machado do Faroleiro", Categoria.MACHADO, "machado2", "O faroleiro cortava lenha para a lanterna com ele.",
            'o', 0xFF2A1A0A, 'h', 0xFFFFFFFF, 'm', 0xFFE53935, 'd', 0xFF8E1B1B, 'w', 0xFF5D4037, 'a', 0xFFFFD54F),
    SINO_DE_MARIS("Sino de Maris", Categoria.MACA, "maca", "Tocava no templo de Maris para chamar a maré.",
            'o', 0xFF0A1A3A, 'h', 0xFFE3F2FD, 'm', 0xFF42A5F5, 'd', 0xFF1565C0, 'w', 0xFF5D4037, 'p', 0xFFFFD54F, 'a', 0xFFFFE082),
    MARTELO_DO_EREMITA("Martelo do Eremita", Categoria.MACA, "martelo", "O eremita dizia que ele só bate em quem merece.",
            'o', 0xFF1A2A10, 'h', 0xFFB0BEC5, 'm', 0xFF78909C, 'd', 0xFF455A64, 'w', 0xFF5D4037, 'p', 0xFF7CB342, 'a', 0xFF9CCC65),
    // ---------- arcos e bestas ----------
    ARCO_DO_CACADOR_SILENCIOSO("Arco do Caçador Silencioso", Categoria.ARCO, "arco", "Nunca rangeu. Os cervos nem viam de onde vinha.",
            'w', 0xFF2E4A1E, 'h', 0xFF6B8E23, 'a', 0xFF1B2A10),
    ASA_DE_ZEFIRA("Asa de Zéfira", Categoria.ARCO, "arco", "Feito com penas que o vento trouxe de uma ilha do céu.",
            'w', 0xFFD9DEE8, 'h', 0xFFFFFFFF, 'a', 0xFF90CAF9),
    ARCO_DAS_BRASAS("Arco das Brasas", Categoria.ARCO, "arco", "Os anões o temperaram na lava. A corda nunca esfria.",
            'w', 0xFF6D2A0A, 'h', 0xFFFF8F00, 'a', 0xFFFFD54F),
    BESTA_DO_CAPITAO("Besta do Capitão", Categoria.BESTA, "besta", "Do capitão de um galeão real que hoje dorme no fundo do mar.",
            'w', 0xFF3E2723, 'h', 0xFF8D6E63, 'a', 0xFFC9A227),
    BESTA_GLACIAL("Besta Glacial", Categoria.BESTA, "besta", "Feita na expedição do norte. O gelo segura a corda.",
            'w', 0xFF455A64, 'h', 0xFFB3E5FC, 'a', 0xFFE1F5FE),
    // ---------- ferramentas ----------
    PICARETA_DO_MINEIRO_PERDIDO("Picareta do Mineiro Perdido", Categoria.PICARETA, "picareta", "Achada no fundo da mina abandonada, ao lado de um capacete.",
            'o', 0xFF2A1A0A, 'h', 0xFFD7A86E, 'm', 0xFFA0522D, 'd', 0xFF6D3A1A, 'w', 0xFF5D4037, 'a', 0xFFFFB74D),
    QUEBRA_CRISTAIS("Quebra-Cristais", Categoria.PICARETA, "picareta", "Ela canta quando acerta um geodo.",
            'o', 0xFF1A0A2A, 'h', 0xFFE1BEE7, 'm', 0xFFAB47BC, 'd', 0xFF6A1B9A, 'w', 0xFF4E342E, 'a', 0xFFCE93D8),
    PICARETA_DAS_PROFUNDEZAS("Picareta das Profundezas", Categoria.PICARETA, "picareta", "Bate melhor no escuro. Dizem que veio de baixo das raízes.",
            'o', 0xFF0A0A0A, 'h', 0xFF9E9E9E, 'm', 0xFF4E4E52, 'd', 0xFF2A2A2E, 'w', 0xFF3E2723, 'a', 0xFFFFC107),
    PA_DO_COVEIRO("Pá do Coveiro", Categoria.PA, "pa", "Cavou todas as covas do cemitério antigo.",
            'o', 0xFF1A1A1A, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF616161, 'w', 0xFFD7CCC8, 'p', 0xFFBDBDBD),
    PA_DO_ARQUEOLOGO("Pá do Arqueólogo", Categoria.PA, "pa", "Desenterrou metade das relíquias do mundo.",
            'o', 0xFF3A2A0A, 'h', 0xFFFFE0B2, 'm', 0xFFC9A227, 'd', 0xFF8D6E1A, 'w', 0xFF795548, 'p', 0xFFFFD54F),
    ENXADA_DA_COLHEITA_DOURADA("Enxada da Colheita Dourada", Categoria.ENXADA, "enxada", "Onde ela passa, o trigo cresce mais alto.",
            'o', 0xFF3A2A00, 'h', 0xFFFFF59D, 'm', 0xFFFFD54F, 'd', 0xFFC9A227, 'w', 0xFF8D6E63, 'p', 0xFF7CB342),
    ENXADA_DO_JARDIM_SUSPENSO("Enxada do Jardim Suspenso", Categoria.ENXADA, "enxada", "Cuidava da Árvore Celeste, lá no alto.",
            'o', 0xFF3A1A2A, 'h', 0xFFFCE4EC, 'm', 0xFFF48FB1, 'd', 0xFFAD1457, 'w', 0xFF5D2E3A, 'p', 0xFFB39DDB),
    VARA_DO_PESCADOR_DO_GELO("Vara do Pescador do Gelo", Categoria.VARA, "vara", "Pescou o primeiro Esturjão Ancestral.",
            'o', 0xFF263238, 'w', 0xFF90A4AE, 's', 0xFFE0E0E0, 'a', 0xFF4FC3F7, 'g', 0xFF37474F, 'p', 0xFF4FC3F7),
    VARA_DE_CORAL("Vara de Coral", Categoria.VARA, "vara", "Feita com um galho de coral da cidade submersa.",
            'o', 0xFF4A1A0A, 'w', 0xFFFF8A65, 's', 0xFFE0E0E0, 'a', 0xFFFFD54F, 'g', 0xFFBF360C, 'p', 0xFF26C6DA);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "nomeada");

    private final String nome;
    private final Categoria categoria;
    private final String molde, historia;
    private final Object[] cores;

    Nomeada(String nome, Categoria categoria, String molde, String historia, Object... cores) {
        this.nome = nome;
        this.categoria = categoria;
        this.molde = molde;
        this.historia = historia;
        this.cores = cores;
    }

    public String nome() { return nome; }
    public Categoria categoria() { return categoria; }
    public String molde() { return molde; }
    public String historia() { return historia; }
    /** Paleta (pares letra, cor ARGB) para o desenho. */
    public Object[] cores() { return cores; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    /** O id da textura e da marca no item. */
    public String visual() { return "nomeada_" + id(); }

    /** Uma nomeada ao acaso para esse tipo de item (null se a categoria não tem nenhuma). */
    public static Nomeada sortear(Material tipo) {
        Categoria c = Categoria.de(tipo);
        List<Nomeada> l = new ArrayList<>();
        for (Nomeada n : values()) if (n.categoria == c) l.add(n);
        return l.isEmpty() ? null : l.get(ThreadLocalRandom.current().nextInt(l.size()));
    }

    private static final NamespacedKey K_LENDA = new NamespacedKey("rpgatributos", "lenda");

    /**
     * Chamado pela Forja depois de montar o item: nome, a história no topo da descrição e o
     * visual. Lendas ficam com o que já têm.
     */
    public static void decorar(org.bukkit.inventory.meta.ItemMeta meta, int refino, net.kyori.adventure.text.format.TextColor cor) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(K_LENDA)) return;
        Nomeada n = porId(pdc.getOrDefault(CHAVE, PersistentDataType.STRING, ""));
        if (n == null) return;
        meta.itemName(net.kyori.adventure.text.Component.text(n.nome + (refino > 0 ? " +" + refino : ""), cor));
        List<net.kyori.adventure.text.Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(0, net.kyori.adventure.text.Component.text("“" + n.historia + "”", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true));
        lore.add(1, net.kyori.adventure.text.Component.empty());
        meta.lore(lore);
        br.rpgatributos.exploracao.PacoteRecursos.marcar(meta, n.visual());
    }

    /** Chance de um item forjado nascer com nome, pela raridade. */
    public static double chance(Raridade r) {
        return switch (r) {
            case COMUM -> 0.02;
            case RARO -> 0.05;
            case EPICO -> 0.10;
            case UNICO -> 0.18;
            case LENDARIO -> 0.30;
            case MITICO -> 0.45;
        };
    }

    /** Um item do jogo para essa nomeada (o material da arma é sorteado: ferro e diamante são os mais comuns). */
    public Material sortearMaterial() {
        String sufixo = switch (categoria) {
            case ESPADA -> "_SWORD";
            case MACHADO -> "_AXE";
            case PICARETA -> "_PICKAXE";
            case PA -> "_SHOVEL";
            case ENXADA -> "_HOE";
            case MACA -> null;
            case ARCO -> null;
            case BESTA -> null;
            default -> null;
        };
        if (sufixo == null) {
            return switch (categoria) {
                case MACA -> Material.MACE;
                case ARCO -> Material.BOW;
                case BESTA -> Material.CROSSBOW;
                default -> Material.FISHING_ROD;
            };
        }
        double r = ThreadLocalRandom.current().nextDouble();
        String tier = r < 0.40 ? "IRON" : r < 0.70 ? "DIAMOND" : r < 0.82 ? "GOLDEN" : r < 0.95 ? "STONE" : "NETHERITE";
        Material m = Material.matchMaterial(tier + sufixo);
        return m != null ? m : Material.matchMaterial("IRON" + sufixo);
    }

    public static Nomeada de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Nomeada porId(String id) {
        for (Nomeada n : values()) if (n.id().equalsIgnoreCase(id) || n.name().equalsIgnoreCase(id)) return n;
        return null;
    }
}
