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
            "Ecoa uma voz do fim do mundo.", "Infunda no corpo para um poder lendário."),

    // 2.33: o céu
    FRAGMENTO_CELESTE("Fragmento Celeste", Material.FEATHER, 0x9FD8F5,
            "Uma lasca de céu que o vento esqueceu de levar.", "Na Bancada Alquímica: Amuleto dos Ventos."),

    // 2.46: minerais de bioma (cada um só nasce num tipo de lugar)
    AMBAR("Âmbar", Material.RAW_GOLD, 0xF2A33A,
            "Resina de árvores que o deserto engoliu. Às vezes guarda algo dentro.", "Bancada Alquímica: Pingente de Âmbar. O Joalheiro compra."),
    JADE("Jade", Material.RAW_COPPER, 0x4CAF7D,
            "A pedra que protege, dizem os mais velhos da selva.", "Bancada Alquímica: Anel de Jade. O Joalheiro compra."),
    PEROLA("Pérola do Mar", Material.QUARTZ, 0xF3E9F0,
            "Uma ostra levou anos fazendo isso.", "Bancada Alquímica: Colar de Pérolas. O Joalheiro compra."),
    CRISTAL_DE_GELO("Cristal de Gelo", Material.PRISMARINE_CRYSTALS, 0xA8E4FF,
            "Não derrete nem na mão de quem o segura.", "Bancada Alquímica: Anel do Gelo Eterno."),
    PEDRA_SANGUE("Pedra-Sangue", Material.NETHER_BRICK, 0xB0102A,
            "Pulsa devagar, como se tivesse um coração.", "Bancada Alquímica: Amuleto de Sangue."),

    // 2.56: minerais do Nether
    CRISTAL_DE_MAGMA("Cristal de Magma", Material.MAGMA_CREAM, 0xFF5A1F,
            "Ainda está quente. Nasce no basalto dos deltas e nas terras do Nether.", "Faz a Lança de Basalto. Incruste numa arma (Forja)."),
    AMBAR_INFERNAL("Âmbar Infernal", Material.GLOWSTONE_DUST, 0xFF9A1F,
            "Resina dos fungos gigantes, endurecida pelo calor.", "Incruste numa arma (Forja). O Ferreiro Piglin paga bem."),
    QUARTZO_NEGRO("Quartzo Negro", Material.QUARTZ, 0x4A4060,
            "Cresce onde as almas se deitam, no vale de areia de almas.", "Incruste numa arma (Forja). O Ferreiro Piglin paga bem."),

    // 3.3: minerais do Abismo
    COBRE_ANTIGO("Cobre dos Antigos", Material.RAW_COPPER, 0xC8703A,
            "Não enferruja. Os Antigos sabiam fazer cobre que dura para sempre.", "Faz o conjunto Antigo (bancada, como o ferro)."),
    CRISTAL_ABISSAL("Cristal Abissal", Material.AMETHYST_SHARD, 0x8A5AE0,
            "Brilha mais quanto mais fundo você está.", "Faz o conjunto Abissal (bancada, como o ferro)."),
    ESPORO_FOSSIL("Esporo Fóssil", Material.GLOW_INK_SAC, 0x3AB0A0,
            "Um esporo que virou pedra há mil anos. Ainda está vivo.", "Faz o conjunto de Esporo (bancada, como o ferro)."),
    FERRO_NEGRO("Ferro Negro", Material.NETHERITE_SCRAP, 0x3A3A4E,
            "Pesado como o próprio Fundo. Nem o sculk o ouve.", "Faz o conjunto de Ferro Negro (bancada, como o ferro)."),
    // 3.5: o mineral das Luas
    PRATA_LUNAR("Prata Lunar", Material.IRON_NUGGET, 0xD8DCE8,
            "Fria ao toque. Corta o que não devia estar vivo.", "Faz o conjunto Lunar (bancada, como o ferro)."),
    // 3.6: os minerais das outras luas
    ONIX_ESTELAR("Ônix Estelar", Material.COAL, 0x3A3A6E,
            "Preto como a Lua Escura, com pontos de luz que parecem estrelas.", "Faz o conjunto Estelar (bancada, como o ferro)."),
    RUBI_SANGUINEO("Rubi Sanguíneo", Material.REDSTONE, 0xE53935,
            "Quente. Pulsa mais rápido perto de quem está ferido.", "Faz o conjunto Sanguíneo (bancada, como o ferro)."),
    CRISTAL_CELESTE("Cristal Celeste", Material.PRISMARINE_CRYSTALS, 0x64B5F6,
            "Canta baixinho quando a Lua Azul está no céu.", "Faz o conjunto Celeste (bancada, como o ferro)."),
    FRAGMENTO_ECLIPSE("Fragmento de Eclipse", Material.AMETHYST_SHARD, 0x8E24AA,
            "Um pedaço da sombra que cobre o sol. Ainda está frio.", "Faz o conjunto do Eclipse (bancada, como o ferro)."),
    // 3.8: o Mundo das Almas
    ESSENCIA_ETEREA("Essência Etérea", Material.CLAY_BALL, 0x7FD8E8,
            "Névoa que não se desfaz na mão. Pesa menos que nada.", "Faz o conjunto Etéreo (bancada, como o ferro)."),
    ALMA_PENADA("Alma Penada", Material.GHAST_TEAR, 0xB0E0EA,
            "Uma alma perdida, presa na luz da lanterna.", "Paga a travessia do Barqueiro (3 por viagem)."),

    // 3.16: animais e assombros novos
    GALHADA("Galhada de Cervo", Material.BONE, 0xA1887F,
            "Caiu de um cervo velho. Cada ponta é um inverno.", "Troféu: exponha na Estante de Troféus."),
    PRESA_DE_JAVALI("Presa de Javali", Material.BONE_MEAL, 0xF3E9D2,
            "Curva e afiada como uma foice pequena.", "Material de caça (alquimia e forja, em breve)."),
    PENA_DE_CORUJA("Pena de Coruja", Material.FEATHER, 0x8D6E63,
            "Não faz barulho nenhum quando cai.", "Material de caça (alquimia e flechas, em breve)."),
    PALHA_ENCANTADA("Palha Encantada", Material.WHEAT, 0xE8C15A,
            "Ainda mexe de leve, como se lembrasse do vento.", "Clique numa plantação: ela cresce um estágio."),
    LAGRIMA_DA_BANSHEE("Lágrima da Banshee", Material.GHAST_TEAR, 0xCFE8F0,
            "Fria e salgada. Dá para ouvir um grito lá dentro.", "Material de assombração (alquimia, em breve)."),
    MUSGO_VIVO("Musgo Vivo", Material.CLAY_BALL, 0x6AAA3A,
            "Cresce até na sua mão.", "Clique numa pedra: vira pedra com musgo. Em terra: vira grama."),
    SEDA_FINA("Seda Fina", Material.STRING, 0xF5F5F5,
            "Mais forte que a linha comum e brilha à luz da lua.", "Material da Aranha-Tecelã (ofícios e forja, em breve)."),
    CABECA_DO_CAVALEIRO("Cabeça do Cavaleiro", Material.JACK_O_LANTERN, 0xFF8F00,
            "Ainda sorri. Ninguém sabe por quê.", "Troféu: exponha na Estante de Troféus."),
    CORRENTE_DO_AFOGADO("Corrente do Afogado", Material.IRON_NUGGET, 0x5E7D6E,
            "Enferrujada, pesada, e sempre molhada.", "Material de assombração (forja, em breve)."),

    // 3.19: os lingotes de liga do Cadinho
    ACO_LUNAR("Lingote de Aço Lunar", Material.IRON_INGOT, 0x9AA2C0,
            "Prata das Luas batida com Ferro Negro.", "Faz o conjunto de Aço Lunar (bancada, como o ferro)."),
    AMBAR_SANGUE("Lingote de Âmbar-Sangue", Material.GOLD_INGOT, 0xC0392B,
            "Âmbar fundido com Pedra-Sangue. Ainda está morno.", "Faz o conjunto de Âmbar-Sangue (bancada, como o ferro)."),
    GELO_MARINHO("Lingote de Gelo Marinho", Material.IRON_INGOT, 0x8AD0E8,
            "Gelo que não derrete, com pó de pérola.", "Faz o conjunto de Gelo Marinho (bancada, como o ferro)."),
    BRONZE_VULCANICO("Lingote de Bronze Vulcânico", Material.COPPER_INGOT, 0xD8682A,
            "Cobre dos Antigos temperado no magma.", "Faz o conjunto de Bronze Vulcânico (bancada, como o ferro)."),

    // 2.84: o Altar da Convergência
    ALMA_DESPERTA("Alma Desperta", Material.FIREWORK_STAR, 0xB388FF,
            "O que sobra de um chefe que voltou mais forte.", "Cai dos chefes Despertos. Usada no Altar da Convergência.");

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
