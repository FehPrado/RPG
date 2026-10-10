package br.rpgatributos.forja;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Trajes: armaduras com nome, as irmãs das {@link Nomeada armas com nome}. Cada traje tem quatro
 * peças (elmo, peitoral, grevas e botas) com nome, história e visual próprios, no inventário e
 * no corpo de quem veste (o componente de equipamento do jogo aponta para a textura do pacote).
 * Só aparência: a força continua sendo a da raridade. O desenho fica no {@code ArteTrajes}.
 */
public enum Traje {
    PENUMBRA("da Penumbra", Estilo.ESCAMAS, "Escamas escuras achadas abaixo das raízes. Quem a veste não teme o escuro.",
            0xFF2E1F47, 0xFF5B3F8C, 0xFF160E24, 0xFF9575CD, 0xFFB388FF),
    VENTOS("dos Ventos", Estilo.PLACAS, "Dos guardiões das ilhas do céu. Leve como uma nuvem.",
            0xFFDDE4EE, 0xFFFFFFFF, 0xFF97A8C2, 0xFFE2B53C, 0xFF90CAF9),
    THALASSA("de Thalassa", Estilo.ESCAMAS, "A guarda da cidade submersa usava escamas que o sal não vence.",
            0xFF00897B, 0xFF4DB6AC, 0xFF004D40, 0xFFFF8A65, 0xFFB2FFF0),
    FORTIM("do Fortim", Estilo.PLACAS, "Os soldados do fortim nunca recuaram. As placas lembram.",
            0xFF78909C, 0xFFB0BEC5, 0xFF37474F, 0xFF8B1A1A, 0xFFE53935),
    ANOES("dos Anões", Estilo.PLACAS, "Batida na forja que nunca apagou, debaixo da montanha.",
            0xFFB8732E, 0xFFE0A060, 0xFF6D3A12, 0xFFFFC107, 0xFFFF6F00),
    GEADA("da Geada", Estilo.PLACAS, "Da Expedição Perdida do norte. Ainda tem gelo nas juntas.",
            0xFFA9DCF5, 0xFFF2FBFF, 0xFF4F8DB5, 0xFF37474F, 0xFF4FC3F7),
    COLHEITA("da Colheita", Estilo.TECIDO, "Do fazendeiro que venceu todos os festivais da colheita.",
            0xFFC9A66B, 0xFFE8D5A0, 0xFF8D6E3F, 0xFF558B2F, 0xFFFFD54F),
    RAIZ_DE_LUZ("da Raiz de Luz", Estilo.TECIDO, "Tecida com as fibras das raízes que iluminam o fundo do mundo.",
            0xFF5D4037, 0xFF8D6E63, 0xFF3E2723, 0xFFFFB300, 0xFFFFE082);

    /** Como o traje é desenhado (e como as peças se chamam). */
    public enum Estilo { PLACAS, ESCAMAS, TECIDO }

    /** As quatro peças de um traje. */
    public enum Peca {
        CAPACETE("_HELMET", Categoria.CAPACETE),
        PEITORAL("_CHESTPLATE", Categoria.PEITORAL),
        CALCA("_LEGGINGS", Categoria.CALCA),
        BOTAS("_BOOTS", Categoria.BOTAS);

        private final String sufixo;
        private final Categoria categoria;

        Peca(String sufixo, Categoria categoria) {
            this.sufixo = sufixo;
            this.categoria = categoria;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
        public String sufixo() { return sufixo; }
        public Categoria categoria() { return categoria; }

        public static Peca de(Material m) {
            if (m == null) return null;
            for (Peca p : values()) if (m.name().endsWith(p.sufixo)) return p;
            return null;
        }

        public static Peca porId(String id) {
            for (Peca p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
            return null;
        }
    }

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "traje");
    private static final NamespacedKey K_LENDA = new NamespacedKey("rpgatributos", "lenda");
    private static final NamespacedKey K_CONJUNTO = new NamespacedKey("rpgatributos", "conjunto");

    private final String sufixo, historia;
    private final Estilo estilo;
    private final int base, luz, sombra, detalhe, gema;

    Traje(String sufixo, Estilo estilo, String historia, int base, int luz, int sombra, int detalhe, int gema) {
        this.sufixo = sufixo;
        this.estilo = estilo;
        this.historia = historia;
        this.base = base;
        this.luz = luz;
        this.sombra = sombra;
        this.detalhe = detalhe;
        this.gema = gema;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public Estilo estilo() { return estilo; }
    public String historia() { return historia; }
    public int base() { return base; }
    public int luz() { return luz; }
    public int sombra() { return sombra; }
    public int detalhe() { return detalhe; }
    public int gema() { return gema; }

    /** "Traje da Penumbra". */
    public String nome() {
        return "Traje " + sufixo;
    }

    /** O nome da peça: "Elmo da Penumbra", "Gibão da Colheita"... */
    public String nome(Peca p) {
        String peca = switch (p) {
            case CAPACETE -> estilo == Estilo.TECIDO ? "Capuz" : "Elmo";
            case PEITORAL -> estilo == Estilo.TECIDO ? "Gibão" : estilo == Estilo.ESCAMAS ? "Cota" : "Peitoral";
            case CALCA -> estilo == Estilo.TECIDO ? "Calças" : "Grevas";
            case BOTAS -> "Botas";
        };
        return peca + " " + sufixo;
    }

    /** O id do visual de uma peça no inventário (textura do item). */
    public String visual(Peca p) {
        return "traje_" + id() + "_" + p.id();
    }

    /** O id do equipamento (a textura no corpo de quem veste). */
    public String equipamento() {
        return "traje_" + id();
    }

    public static Traje sortear() {
        Traje[] t = values();
        return t[ThreadLocalRandom.current().nextInt(t.length)];
    }

    /** O material da peça: ferro e diamante são os mais comuns. */
    public static Material sortearMaterial(Peca p) {
        double r = ThreadLocalRandom.current().nextDouble();
        String tier = r < 0.35 ? "IRON" : r < 0.60 ? "DIAMOND" : r < 0.75 ? "CHAINMAIL" : r < 0.85 ? "GOLDEN" : r < 0.93 ? "LEATHER" : "NETHERITE";
        Material m = Material.matchMaterial(tier + p.sufixo);
        return m != null ? m : Material.matchMaterial("IRON" + p.sufixo);
    }

    /** Pode virar traje? (peças de conjunto do Ateliê e lendas ficam com o que já têm.) */
    public static boolean aceita(ItemStack item) {
        if (item == null || Peca.de(item.getType()) == null || item.getType() == Material.TURTLE_HELMET) return false;
        var pdc = item.getPersistentDataContainer();
        return !pdc.has(K_LENDA) && !pdc.has(K_CONJUNTO);
    }

    /**
     * Chamado pela Forja depois de montar o item: nome da peça, a história no topo da descrição e
     * o visual no inventário. O visual no corpo é posto por {@link #vestir(ItemStack)}.
     */
    public static void decorar(ItemMeta meta, Material tipo, int refino, TextColor cor) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(K_LENDA) || pdc.has(K_CONJUNTO)) return;
        Traje t = porId(pdc.getOrDefault(CHAVE, PersistentDataType.STRING, ""));
        Peca p = Peca.de(tipo);
        if (t == null || p == null) return;
        meta.itemName(Component.text(t.nome(p) + (refino > 0 ? " +" + refino : ""), cor));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(0, Component.text("“" + t.historia + "”", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true));
        lore.add(1, Component.text(t.nome() + " · peça com nome (só aparência)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(2, Component.empty());
        meta.lore(lore);
        br.rpgatributos.exploracao.PacoteRecursos.marcar(meta, t.visual(p));
    }

    /** Põe a textura do traje no corpo de quem veste (componente de equipamento do jogo). */
    public static void vestir(ItemStack item) {
        Traje t = de(item);
        if (t == null || !aceita(item)) return;
        Equippable base = item.getData(DataComponentTypes.EQUIPPABLE);
        if (base == null) return;
        Key k = Key.key("rpgatributos", t.equipamento());
        if (k.equals(base.assetId())) return;
        item.setData(DataComponentTypes.EQUIPPABLE, base.toBuilder().assetId(k).build());
    }

    public static Traje de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return porId(i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING));
    }

    public static Traje porId(String id) {
        if (id == null) return null;
        for (Traje t : values()) if (t.id().equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) return t;
        return null;
    }
}
