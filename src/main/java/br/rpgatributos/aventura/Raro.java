package br.rpgatributos.aventura;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Materiais raros: caem dos chefes e são usados no refino e nas infusões lendárias.
 * A base de cada um é um item que não vira bloco, não é combustível nem ingrediente
 * de poção; o MateriaisListener impede que sejam gastos em receitas comuns.
 */
public enum Raro {
    FRAGMENTO_DE_FORJA("Fragmento de Forja", Material.NETHERITE_SCRAP, 0xE8823A,
            "Metal que só os chefes guardam.", "Usado para refinar itens acima de +5."),
    PEDRA_DE_PROTECAO("Pedra de Proteção", Material.PRISMARINE_CRYSTALS, 0x7FE3D4,
            "Brilha quando o metal ameaça quebrar.", "No refino, impede perder nível ou quebrar o item."),
    ESSENCIA_PRIMORDIAL("Essência Primordial", Material.HEART_OF_THE_SEA, 0xB66BFF,
            "A matéria de que o mundo foi feito.", "Necessária para refinar até +10."),

    // Pistas: usadas com o clique direito.
    MAPA_RASGADO("Mapa Rasgado", Material.PAPER, 0xC8A165,
            "Rabiscos de um lugar que ninguém lembra.", "Clique direito: aponta para um Local Oculto."),
    PAGINA_DE_LENDA("Página do Livro das Lendas", Material.PAPER, 0xFF5FD7,
            "Letras que brilham no escuro.", "Clique direito: revela os feitos de uma lenda."),

    // Núcleos de chefe: infundidos no corpo dão um poder lendário.
    NUCLEO_GOLEM("Núcleo de Ferro Ancestral", Material.IRON_NUGGET, 0xD8D8D8,
            "Ainda pulsa como um coração de ferro.", "Infunda no corpo para um poder lendário."),
    NUCLEO_RAINHA("Presa da Rainha Aracnídea", Material.DISC_FRAGMENT_5, 0x8E5B9E,
            "Um veneno que não seca nunca.", "Infunda no corpo para um poder lendário."),
    NUCLEO_CHAMAS("Brasa Eterna", Material.NETHER_BRICK, 0xFF6A2B,
            "Queima sem consumir nada.", "Infunda no corpo para um poder lendário."),
    NUCLEO_LICH("Filactério do Lich", Material.NAUTILUS_SHELL, 0x9A86B8,
            "Guarda uma alma que se recusa a partir.", "Infunda no corpo para um poder lendário."),
    NUCLEO_TEMPESTADE("Olho da Tempestade", Material.PRISMARINE_SHARD, 0xDDEFEF,
            "O vento gira em volta dele sem parar.", "Infunda no corpo para um poder lendário."),
    NUCLEO_ARAUTO("Coroa do Arauto", Material.ECHO_SHARD, 0xC77DFF,
            "Ecoa uma voz do fim do mundo.", "Infunda no corpo para um poder lendário.");

    /** Chave fixa (namespace do plugin) para não depender da instância do plugin. */
    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "raro");

    private final String nome;
    private final Material base;
    private final TextColor cor;
    private final String frase;
    private final String uso;

    Raro(String nome, Material base, int cor, String frase, String uso) {
        this.nome = nome;
        this.base = base;
        this.cor = TextColor.color(cor);
        this.frase = frase;
        this.uso = uso;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public Material base() { return base; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public boolean nucleo() { return name().startsWith("NUCLEO_"); }

    public ItemStack criar(int quantidade) {
        ItemStack item = new ItemStack(base, quantidade);
        item.editMeta(m -> {
            m.itemName(Component.text(nome, cor));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(frase, NamedTextColor.GRAY, TextDecoration.ITALIC));
            lore.add(Component.empty());
            lore.add(Component.text(uso, NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(Component.text(nucleo() ? "✦ Núcleo de chefe" : "✦ Material raro", cor));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC,
                    c.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, name());
            br.rpgatributos.exploracao.PacoteRecursos.marcar(m, "raro_" + id());
        });
        return item;
    }

    public ItemStack criar() {
        return criar(1);
    }

    /** Qual material raro é o item, ou null. */
    public static Raro de(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        String id = item.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (id == null) return null;
        try {
            return valueOf(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Raro porId(String texto) {
        for (Raro r : values()) if (r.id().equalsIgnoreCase(texto) || r.name().equalsIgnoreCase(texto)) return r;
        return null;
    }

    public boolean eh(ItemStack item) {
        return de(item) == this;
    }

    /** Quantos desse material o jogador tem no inventário. */
    public int contar(org.bukkit.inventory.PlayerInventory inv) {
        int n = 0;
        for (ItemStack i : inv.getStorageContents()) if (eh(i)) n += i.getAmount();
        return n;
    }

    /** Tira {@code qtd} do inventário. @return false se não tinha o suficiente (não tira nada). */
    public boolean tirar(org.bukkit.inventory.PlayerInventory inv, int qtd) {
        if (contar(inv) < qtd) return false;
        ItemStack[] conteudo = inv.getStorageContents();
        for (int i = 0; i < conteudo.length && qtd > 0; i++) {
            ItemStack s = conteudo[i];
            if (!eh(s)) continue;
            int tira = Math.min(qtd, s.getAmount());
            s.setAmount(s.getAmount() - tira);
            qtd -= tira;
            conteudo[i] = s.getAmount() > 0 ? s : null;
        }
        inv.setStorageContents(conteudo);
        return true;
    }
}
