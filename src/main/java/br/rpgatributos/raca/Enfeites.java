package br.rpgatributos.raca;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enfeites de cabeça (2.53): orelhas élficas, barba de anão, chifres de dragão, barbatanas,
 * orelhas de lobo e óculos de inventor. São itens com modelo 3D no pacote de recursos: vestidos
 * direto na cabeça (clique direito) ou postos no Guarda-roupa por cima do capacete (o capacete
 * some e o enfeite aparece no lugar). Sem o pacote aparecem como o item sem textura.
 *
 * Os modelos usam o espaço da cabeça de um bloco (0 a 16; a frente da cabeça é o lado norte,
 * z = 0), o mesmo que a abóbora esculpida usa quando é vestida.
 */
public final class Enfeites {

    private Enfeites() { }

    public static final NamespacedKey K_ENFEITE = new NamespacedKey("rpgatributos", "enfeite_cabeca");
    private static final TextColor COR = TextColor.color(0xFFD180);

    /** Uma caixa do modelo: de, até, se usa a cor de destaque (lado direito da textura) e uma rotação opcional. */
    private record Caixa(double[] de, double[] ate, boolean destaque, String eixo, double angulo, double[] origem) {
        Caixa(double[] de, double[] ate, boolean destaque) {
            this(de, ate, destaque, null, 0, null);
        }
    }

    public enum Enfeite {
        ORELHAS_ELFICAS("orelhas_elficas", "Orelhas Élficas", Raca.ELFO, 0xE8C4A0, 0xD49A7A, List.of(
                new Caixa(new double[]{-2, 7, 7.5}, new double[]{0, 10, 8.5}, false),
                new Caixa(new double[]{-3.5, 9, 7.5}, new double[]{-1.5, 13, 8.5}, true, "z", -22.5, new double[]{-1, 9, 8}),
                new Caixa(new double[]{16, 7, 7.5}, new double[]{18, 10, 8.5}, false),
                new Caixa(new double[]{17.5, 9, 7.5}, new double[]{19.5, 13, 8.5}, true, "z", 22.5, new double[]{17, 9, 8}))),
        BARBA_ANA("barba_ana", "Barba de Anão", Raca.ANAO, 0xB5652B, 0x8D4A1F, List.of(
                new Caixa(new double[]{3, -3, -1.5}, new double[]{13, 5, 1}, false),
                new Caixa(new double[]{3.5, 5, -2}, new double[]{12.5, 6.5, -0.5}, true),
                new Caixa(new double[]{7, -7, -1}, new double[]{9, -3, 0.5}, true))),
        CHIFRES_DRAGAO("chifres_dragao", "Chifres de Dragão", Raca.DRACONATO, 0x3E2723, 0xD7CCC8, List.of(
                new Caixa(new double[]{3, 15, 8}, new double[]{5, 20, 10}, false, "x", -22.5, new double[]{4, 15, 9}),
                new Caixa(new double[]{3.5, 19, 10}, new double[]{4.5, 23, 11}, true, "x", -45, new double[]{4, 19, 10.5}),
                new Caixa(new double[]{11, 15, 8}, new double[]{13, 20, 10}, false, "x", -22.5, new double[]{12, 15, 9}),
                new Caixa(new double[]{11.5, 19, 10}, new double[]{12.5, 23, 11}, true, "x", -45, new double[]{12, 19, 10.5}))),
        BARBATANAS("barbatanas", "Barbatanas", Raca.MAR, 0x26C6DA, 0x00838F, List.of(
                new Caixa(new double[]{-1.5, 5, 6}, new double[]{0, 12, 13}, false),
                new Caixa(new double[]{16, 5, 6}, new double[]{17.5, 12, 13}, false),
                new Caixa(new double[]{7.5, 16, 3}, new double[]{8.5, 20, 12}, true))),
        ORELHAS_LOBO("orelhas_lobo", "Orelhas de Lobo", Raca.FERA, 0x6D4C41, 0xD7A98C, List.of(
                new Caixa(new double[]{2, 15.5, 7}, new double[]{6, 20, 9}, false),
                new Caixa(new double[]{3, 19.5, 7.5}, new double[]{5, 22, 8.5}, true),
                new Caixa(new double[]{10, 15.5, 7}, new double[]{14, 20, 9}, false),
                new Caixa(new double[]{11, 19.5, 7.5}, new double[]{13, 22, 8.5}, true))),
        OCULOS_INVENTOR("oculos_inventor", "Óculos de Inventor", Raca.GNOMO, 0xC9A227, 0x80DEEA, List.of(
                new Caixa(new double[]{0, 10, -1}, new double[]{16, 11.5, 0}, false),
                new Caixa(new double[]{3, 9, -1.8}, new double[]{7, 13, -0.5}, true),
                new Caixa(new double[]{9, 9, -1.8}, new double[]{13, 13, -0.5}, true),
                new Caixa(new double[]{7, 10.5, -1.4}, new double[]{9, 11.5, -0.5}, false))),
        AUREOLA_ESPECTRAL("aureola", "Auréola Espectral", Raca.ESPECTRAL, 0xB3E5FC, 0xFFFFFF, List.of(
                new Caixa(new double[]{3, 19, 3}, new double[]{13, 19.8, 4.5}, false),
                new Caixa(new double[]{3, 19, 11.5}, new double[]{13, 19.8, 13}, false),
                new Caixa(new double[]{3, 19, 4.5}, new double[]{4.5, 19.8, 11.5}, true),
                new Caixa(new double[]{11.5, 19, 4.5}, new double[]{13, 19.8, 11.5}, true)));

        public final String id, nome;
        public final Raca raca;
        final int cor, destaque;
        final List<Caixa> caixas;

        Enfeite(String id, String nome, Raca raca, int cor, int destaque, List<Caixa> caixas) {
            this.id = id;
            this.nome = nome;
            this.raca = raca;
            this.cor = cor;
            this.destaque = destaque;
            this.caixas = caixas;
        }

        public String modelo() { return "rpgatributos:enfeite_" + id; }
    }

    /** O item do enfeite: vestível na cabeça, com o modelo 3D do pacote. */
    public static ItemStack criar(Enfeite e) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.setData(DataComponentTypes.ITEM_MODEL, Key.key(e.modelo()));
        i.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD).build());
        i.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        i.editMeta(m -> {
            m.itemName(Component.text(e.nome, TextColor.color(e.raca.cor().value())));
            m.lore(List.of(
                    Component.text("Enfeite de cabeça (" + e.raca.nome() + ")", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Clique direito para vestir, ou ponha no", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Guarda-roupa por cima do capacete.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_ENFEITE, PersistentDataType.STRING, e.id);
        });
        return i;
    }

    // =====================================================================
    //  Pacote de recursos
    // =====================================================================

    /** As texturas (16×16: metade da esquerda na cor principal, a da direita no destaque). */
    public static Map<String, BufferedImage> texturas() {
        Map<String, BufferedImage> m = new LinkedHashMap<>();
        for (Enfeite e : Enfeite.values()) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int base = x < 8 ? e.cor : e.destaque;
                    // Um pouco de variação para não ficar chapado (sempre igual: depende só do pixel).
                    double f = 0.88 + 0.12 * (((x * 7 + y * 13) % 5) / 4.0) - (y == 0 || y == 15 ? 0.1 : 0);
                    int r = (int) Math.min(255, ((base >> 16) & 255) * f), g = (int) Math.min(255, ((base >> 8) & 255) * f), b = (int) Math.min(255, (base & 255) * f);
                    img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                }
            }
            m.put("item/enfeite_" + e.id, img);
        }
        return m;
    }

    /** Os modelos 3D e as definições de item (assets/rpgatributos/items/enfeite_*.json). */
    public static Map<String, byte[]> arquivosDoPacote() {
        Map<String, byte[]> f = new LinkedHashMap<>();
        for (Enfeite e : Enfeite.values()) {
            f.put("assets/rpgatributos/items/enfeite_" + e.id + ".json",
                    ("{ \"model\": { \"type\": \"minecraft:model\", \"model\": \"rpgatributos:item/enfeite/" + e.id + "\" } }").getBytes(StandardCharsets.UTF_8));
            f.put("assets/rpgatributos/models/item/enfeite/" + e.id + ".json", modelo(e).getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }

    private static String n(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    private static String vetor(double[] v) {
        return "[" + n(v[0]) + ", " + n(v[1]) + ", " + n(v[2]) + "]";
    }

    private static String modelo(Enfeite e) {
        String tex = "rpgatributos:item/enfeite_" + e.id;
        List<String> els = new ArrayList<>();
        for (Caixa c : e.caixas) {
            String uv = c.destaque() ? "[8, 0, 16, 16]" : "[0, 0, 8, 16]";
            StringBuilder faces = new StringBuilder();
            for (String lado : new String[]{"north", "south", "east", "west", "up", "down"}) {
                if (faces.length() > 0) faces.append(", ");
                faces.append("\"").append(lado).append("\": { \"uv\": ").append(uv).append(", \"texture\": \"#t\" }");
            }
            String rot = c.eixo() == null ? "" : ", \"rotation\": { \"angle\": " + n(c.angulo()) + ", \"axis\": \"" + c.eixo() + "\", \"origin\": " + vetor(c.origem()) + " }";
            els.add("{ \"from\": " + vetor(c.de()) + ", \"to\": " + vetor(c.ate()) + rot + ", \"faces\": { " + faces + " } }");
        }
        // Na cabeça vale o jeito do bloco (sem transformação); no inventário, um pouco menor e centrado.
        return "{ \"textures\": { \"t\": \"" + tex + "\", \"particle\": \"" + tex + "\" }, \"elements\": [ " + String.join(", ", els) + " ], "
                + "\"display\": { "
                + "\"gui\": { \"rotation\": [30, 225, 0], \"translation\": [0, -1.5, 0], \"scale\": [0.55, 0.55, 0.55] }, "
                + "\"ground\": { \"translation\": [0, 3, 0], \"scale\": [0.25, 0.25, 0.25] }, "
                + "\"fixed\": { \"scale\": [0.5, 0.5, 0.5] }, "
                + "\"thirdperson_righthand\": { \"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.375, 0.375, 0.375] }, "
                + "\"firstperson_righthand\": { \"rotation\": [0, 45, 0], \"scale\": [0.4, 0.4, 0.4] } } }";
    }
}
