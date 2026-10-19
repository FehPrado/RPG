package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.colonia.Lacos;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.forja.Nomeada;
import br.rpgatributos.forja.Raridade;
import br.rpgatributos.party.Party;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Bebida;
import br.rpgatributos.vida.Carta;
import br.rpgatributos.vida.Erva;
import br.rpgatributos.vida.Flechas;
import br.rpgatributos.vida.Fruta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

/**
 * As vilas com vários moradores (2.45): Vila de Pescadores, Mosteiro da Montanha, Aldeia dos Anões
 * e Vila Élfica do Bosque. Cada uma tem 4 NPCs com papel próprio: loja (as ofertas mudam a cada dia),
 * um serviço e um Laço (5 capítulos de história e um presente único no fim). Os NPCs nascem quando
 * alguém chega perto e voltam se sumirem.
 */
public final class Vilas implements Listener {

    public static final NamespacedKey K_NPC = new NamespacedKey("rpgatributos", "vila_npc");
    private static final NamespacedKey K_DIA = new NamespacedKey("rpgatributos", "vila_npc_dia");
    private static final TextColor COR = TextColor.color(0xF4C27A);

    // =====================================================================
    //  Os papéis (16 NPCs, 4 por vila)
    // =====================================================================

    public enum Papel {
        VELHO_PESCADOR(Estrutura.VILA_PESCADORES, "Velho Pescador", new String[]{"Tião", "Joca", "Seu Bento", "Dito"},
                Villager.Profession.FISHERMAN, Villager.Type.PLAINS, 1, new int[]{-4, 2, -3},
                "Vende varas e iscas e conta as lendas do mar.", null,
                new Lacos.Historia("uma ilha que o mar engoliu numa noite só", "do dia em que o mar ficar em silêncio",
                        "pescar o peixe dourado que o avô dele jurava existir", "um anzol feito com o dente de um tubarão de dez metros",
                        "a Vara de Coral dele", "Cuida dela. Ela já pegou mais peixe do que eu tenho de cabelo.",
                        new Material[]{Material.COOKED_COD, Material.LANTERN}),
                "O mar hoje está manso.", "Já viu o farol aceso? Dá sorte a quem pesca.", "Nunca pesque de costas para a lua.", "As baleias cantam antes da tempestade."),
        PEIXEIRA(Estrutura.VILA_PESCADORES, "Peixeira", new String[]{"Dona Cida", "Marlene", "Iara", "Conceição"},
                Villager.Profession.FISHERMAN, Villager.Type.SWAMP, 1, new int[]{4, 2, -2},
                "Compra os seus peixes por esmeraldas.", null,
                new Lacos.Historia("uma cidade de comerciantes no sul", "de voltar a passar fome como na infância",
                        "abrir uma peixaria em cada vila do mundo", "a balança que ela usa, que mente um pouquinho a favor do pescador",
                        "o Mapa da Enseada", "Onde o X está, meu marido escondeu o que pescou de mais valioso.",
                        new Material[]{Material.COOKED_SALMON, Material.SUGAR}),
                "Peixe fresco, freguês!", "Salmão está em alta esta semana.", "Quem pesca bem, come bem.", "Traz bacalhau que eu pago bem."),
        CARPINTEIRO_NAVAL(Estrutura.VILA_PESCADORES, "Carpinteiro Naval", new String[]{"Mestre Davi", "Zé do Casco", "Lauro", "Benedito"},
                Villager.Profession.FLETCHER, Villager.Type.PLAINS, 1, new int[]{4, 2, 1},
                "Faz barcos, cordas e lunetas.", "Consertar o barco: um barco com baú novo (3 esmeraldas).",
                new Lacos.Historia("um estaleiro de navios de guerra", "de um casco rachado no meio do oceano",
                        "terminar o barco que está montando há dez anos", "o nome da amada dele, entalhado em cada barco que faz",
                        "o Barco do Carpinteiro", "Ele não afunda. Eu prometo.",
                        new Material[]{Material.OAK_LOG, Material.IRON_NUGGET}),
                "Madeira boa faz barco bom.", "Esse casco aqui ainda vai cruzar o mar.", "Cuidado com o prego solto.", "Sabe nadar? Melhor saber."),
        MENINA_DO_CAIS(Estrutura.VILA_PESCADORES, "Menina do Cais", new String[]{"Lia", "Nina", "Bia", "Malu"},
                Villager.Profession.NONE, Villager.Type.PLAINS, 0.65, new int[]{0, 2, 6},
                "Todo dia pede uma ajuda e retribui do jeito dela.", "Pedido do dia: traga o que ela pede.",
                new Lacos.Historia("esta mesma vila; nunca saiu daqui", "de que o pai não volte do mar",
                        "ver as Ilhas do Céu de perto", "uma concha que, encostada no ouvido, fala com voz de gente",
                        "a Concha que Fala", "Guarda. Quando você estiver perdido, ela canta.",
                        new Material[]{Material.COOKIE, Material.SWEET_BERRIES}),
                "Você já viu uma baleia?", "O meu pai vai voltar hoje!", "Eu sei assobiar com dois dedos.", "Achei uma concha azul!"),

        MESTRE(Estrutura.MOSTEIRO, "Mestre", new String[]{"Mestre Wen", "Mestre Lian", "Mestra Mei", "Mestre Shou"},
                Villager.Profession.WEAPONSMITH, Villager.Type.SNOW, 1, new int[]{0, 1, -1},
                "Ensina a lutar com calma. Treino uma vez por dia.", "Treinar: XP de Combate e Força por 5 min (1 vez por dia).",
                new Lacos.Historia("um vilarejo onde aprendeu a lutar para defender o irmão", "de lutar com raiva e perder quem é",
                        "ter um aluno melhor do que ele", "a faixa do próprio mestre, que nunca lavou",
                        "a Agulha dos Ventos", "Não é a lança que fere. É a sua calma.",
                        new Material[]{Material.BAMBOO, Material.HONEY_BOTTLE}),
                "Respire. Primeiro o ar, depois o golpe.", "A montanha não se apressa.", "Força sem calma é só barulho.", "Treine o corpo, mas descanse a mente."),
        HERBALISTA(Estrutura.MOSTEIRO, "Monge Herbalista", new String[]{"Irmão Kai", "Irmão Ping", "Irmã Lan", "Irmão Hao"},
                Villager.Profession.CLERIC, Villager.Type.SNOW, 1, new int[]{-4, 1, 1},
                "Chás, ervas e elixires da montanha.", null,
                new Lacos.Historia("uma família de boticários pobres", "de errar a dose de um remédio",
                        "achar a erva que cura qualquer febre", "um jardim secreto atrás da cachoeira",
                        "dois Elixires Ótimos", "Para os dias em que o corpo pede mais do que tem.",
                        new Material[]{Material.SWEET_BERRIES, Material.HONEY_BOTTLE}),
                "Este chá acalma o coração.", "A erva certa na hora certa.", "As plantas escutam, sabia?", "Beba devagar."),
        GUARDIAO_DO_SINO(Estrutura.MOSTEIRO, "Guardião do Sino", new String[]{"Irmão Bao", "Irmão Tai", "Irmã Yun", "Irmão Lei"},
                Villager.Profession.LIBRARIAN, Villager.Type.SNOW, 1, new int[]{5, 1, 1},
                "Toca o sino e abençoa quem passa.", "Tocar o sino: Resistência e Regeneração por 3 min para você e a party (1 vez por dia).",
                new Lacos.Historia("um mosteiro que caiu numa avalanche", "do silêncio depois do último toque",
                        "que o som do sino chegue até a colônia de vocês", "uma rachadura no sino que só ele sabe onde fica",
                        "a Pedra de Proteção do mosteiro", "Ela guardou o sino por cem anos. Agora guarda você.",
                        new Material[]{Material.CANDLE, Material.BREAD}),
                "Cada toque é uma oração.", "Ouviu? O vento respondeu.", "O sino acorda até os mortos... mas não se preocupe.", "Silêncio também é música."),
        PEREGRINO(Estrutura.MOSTEIRO, "Peregrino", new String[]{"Velho Huo", "Andarilho Jin", "Peregrina Su", "Velho Ming"},
                Villager.Profession.CARTOGRAPHER, Villager.Type.TAIGA, 1, new int[]{2, 1, 5},
                "Conhece o mundo inteiro e conta histórias.", "Ouvir uma história (1 vez por dia; às vezes ele dá uma Página do Livro das Lendas).",
                new Lacos.Historia("um lugar que ele não lembra mais o nome", "de parar de andar e o mundo acabar",
                        "chegar ao fim do mapa e ver o que tem depois", "um caderno com o nome de cada pessoa que conheceu",
                        "uma Página do Livro das Lendas", "Escrevi essa história para você terminar.",
                        new Material[]{Material.PAPER, Material.APPLE}),
                "O mundo é maior do que o seu mapa.", "Já ouviu falar das Bestas Errantes?", "Andar é a melhor forma de pensar.", "Todo caminho conta uma história."),

        MESTRE_MINEIRO(Estrutura.ALDEIA_ANOES, "Mestre Mineiro", new String[]{"Thorgrim", "Balin", "Durgan", "Hilda"},
                Villager.Profession.MASON, Villager.Type.TAIGA, 0.75, new int[]{4, 1, 4},
                "Tudo para quem desce: tochas, lanternas, explosivos.", null,
                new Lacos.Historia("as minas fundas do sul, perto da Forja dos Anões", "do teto que range antes de cair",
                        "achar mitrilo puro, sem pedra nenhuma", "um túnel que só ele conhece e que vai até as Profundezas",
                        "a Picareta do Poço Fundo", "Bate com respeito. A pedra lembra de quem a machucou.",
                        new Material[]{Material.TORCH, Material.COOKED_BEEF}),
                "Pedra boa é pedra que canta.", "Desce com luz, sobe com ouro.", "Ouviu isso? Era a montanha respirando.", "Anão não se perde no escuro."),
        JOALHEIRO(Estrutura.ALDEIA_ANOES, "Joalheiro", new String[]{"Gimli", "Nori", "Brunhilda", "Orin"},
                Villager.Profession.TOOLSMITH, Villager.Type.TAIGA, 0.75, new int[]{-4, 1, -3},
                "Vende gemas e compra diamantes e ametista.", null,
                new Lacos.Historia("uma família que lapida há sete gerações", "de rachar a pedra mais bonita do mundo",
                        "fazer uma coroa para um rei que mereça", "uma gema que muda de cor com o humor de quem segura",
                        "duas gemas lapidadas por ele", "Cada faceta é um ano da minha vida.",
                        new Material[]{Material.AMETHYST_SHARD, Material.GOLD_NUGGET}),
                "Olha esse brilho!", "Gema torta é gema sem alma.", "Diamante bruto é só pedra teimosa.", "Paciência faz a faceta."),
        FERREIRO_ANAO(Estrutura.ALDEIA_ANOES, "Ferreiro Anão", new String[]{"Dwalin", "Brokk", "Sindri", "Gerda"},
                Villager.Profession.ARMORER, Villager.Type.TAIGA, 0.75, new int[]{3, 1, -4},
                "Conserta qualquer coisa por esmeraldas.", "Consertar o item da mão (esmeraldas conforme o estrago).",
                new Lacos.Historia("a Forja dos Anões, antes de ela ser abandonada", "de o fogo da forja apagar de vez",
                        "reacender a Forja dos Anões", "o martelo do avô, que ninguém mais consegue levantar",
                        "dois Fragmentos de Forja", "Daqui sai a melhor arma que você vai ter.",
                        new Material[]{Material.IRON_INGOT, Material.COAL}),
                "Bigorna boa não reclama.", "Ferro quente, conversa fria.", "Isso aí precisa de conserto.", "O fogo é meu irmão mais velho."),
        TAVERNEIRO(Estrutura.ALDEIA_ANOES, "Taverneiro", new String[]{"Barlo", "Grumm", "Helga", "Torvi"},
                Villager.Profession.BUTCHER, Villager.Type.TAIGA, 0.75, new int[]{-3, 1, 0},
                "Bebidas fortes e comida quente.", null,
                new Lacos.Historia("uma taverna que pegou fogo numa briga", "de servir o último barril",
                        "uma taverna onde anões, elfos e humanos bebam juntos", "a receita do hidromel dos reis, escrita num guardanapo",
                        "duas bebidas da reserva dele", "Só abro esse barril para amigo.",
                        new Material[]{Material.WHEAT, Material.HONEY_BOTTLE}),
                "Mais uma rodada?", "Cerveja de anão derruba até troll.", "Senta, descansa as pernas.", "Aqui ninguém fica com fome."),

        DRUIDA(Estrutura.VILA_ELFICA, "Druida", new String[]{"Elarion", "Faelith", "Thalion", "Ilyana"},
                Villager.Profession.FARMER, Villager.Type.JUNGLE, 1.05, new int[]{0, 1, 2},
                "Mudas, ervas e a bênção do bosque.", "Bênção do bosque: saciedade e Regeneração por 2 min (1 vez por dia).",
                new Lacos.Historia("uma floresta tão antiga que as árvores tinham nome", "do machado que não pede licença",
                        "plantar uma floresta inteira antes de partir", "uma semente que nunca germinou e que ele ainda rega",
                        "duas mudas raras", "Plante onde você quer voltar.",
                        new Material[]{Material.OAK_SAPLING, Material.GLOW_BERRIES}),
                "As árvores lembram de você.", "Pise devagar, há vida no chão.", "O bosque está feliz hoje.", "Cada folha é uma palavra."),
        ARQUEIRA(Estrutura.VILA_ELFICA, "Arqueira", new String[]{"Lirael", "Sylvara", "Aerin", "Nimue"},
                Villager.Profession.FLETCHER, Villager.Type.JUNGLE, 1.05, new int[]{-4, 1, 4},
                "Flechas especiais: fogo, gelo, rastreadoras.", null,
                new Lacos.Historia("as árvores mais altas do bosque, onde se aprende a mirar antes de andar", "de errar e alguém pagar pelo erro",
                        "acertar uma estrela cadente", "uma flecha que nunca disparou, com o nome da irmã",
                        "o Arco das Folhas Caídas", "Ele erra menos que eu. E eu não erro.",
                        new Material[]{Material.FEATHER, Material.APPLE}),
                "Mire no que você não vê.", "O vento está do nosso lado.", "Silêncio. Tem algo ali.", "Uma flecha, um propósito."),
        CURANDEIRA(Estrutura.VILA_ELFICA, "Curandeira", new String[]{"Aelora", "Mirwen", "Celebrin", "Ithil"},
                Villager.Profession.CLERIC, Villager.Type.JUNGLE, 1.05, new int[]{4, 1, -1},
                "Cura, tira venenos e a Penumbra.", "Curar: vida cheia, tira efeitos ruins e a Penumbra (5 esmeraldas).",
                new Lacos.Historia("um templo à beira de um lago que hoje secou", "de chegar tarde demais",
                        "curar a própria floresta da Penumbra", "uma lágrima de unicórnio guardada num frasco",
                        "duas maçãs douradas e um elixir", "Para o dia em que eu não estiver perto.",
                        new Material[]{Material.GLOW_BERRIES, Material.LILY_OF_THE_VALLEY}),
                "Deixa eu ver esse machucado.", "Respira fundo. Isso, assim.", "A floresta cura quem a respeita.", "Nenhuma ferida é para sempre."),
        TECELA(Estrutura.VILA_ELFICA, "Tecelã", new String[]{"Elenwë", "Tauriel", "Vanya", "Lúthien"},
                Villager.Profession.SHEPHERD, Villager.Type.JUNGLE, 1.05, new int[]{-3, 1, -2},
                "Lãs, estandartes e fios élficos.", null,
                new Lacos.Historia("uma família que tecia as velas dos navios élficos", "de o último fio acabar",
                        "tecer um manto com todas as cores do bosque", "um tear que tece sozinho nas noites de lua cheia",
                        "uma carta brilhante e fios élficos", "Um fio meu sempre volta para casa.",
                        new Material[]{Material.STRING, Material.PINK_TULIP}),
                "Cada fio conta uma história.", "Essa cor veio de uma flor da manhã.", "O tear canta quando trabalha.", "Paciência é o fio mais forte."),

        // ---------- 2.54: os Mestres de Armas (cada um ensina as técnicas secretas das armas do seu povo) ----------
        MESTRA_DAS_MARES(Estrutura.VILA_PESCADORES, "Mestra das Marés", new String[]{"Iemanja", "Marisa", "Dona Olinda", "Celeste"},
                Villager.Profession.WEAPONSMITH, Villager.Type.SWAMP, 1, new int[]{-3, 2, 3},
                "Ensina as técnicas do tridente, da lança, do arpão e das armas de arremesso.",
                "Ensinar uma técnica secreta (proficiência 12 e 24 esmeraldas).",
                new Lacos.Historia("um navio de guerra que afundou com toda a tripulação, menos ela", "do silêncio antes da onda grande",
                        "ensinar alguém a vencer o mar sem odiá-lo", "o arpão que matou o kraken do porto velho",
                        "um tridente forjado do mar", "Ele já voltou para a minha mão mil vezes. Agora volta para a sua.",
                        new Material[]{Material.PRISMARINE_SHARD, Material.COOKED_SALMON}),
                "A onda não briga com a pedra. Ela espera.", "Firme o pé. A maré puxa quem hesita.", "Jogue com o ombro, não com o braço.", "O mar ensina quem aguenta apanhar dele."),
        MESTRE_DO_BASTAO(Estrutura.MOSTEIRO, "Mestre do Bastão", new String[]{"Shen", "Wu Lan", "Velho Bai", "Ming Yue"},
                Villager.Profession.WEAPONSMITH, Villager.Type.SNOW, 1, new int[]{-3, 1, -4},
                "Ensina as técnicas do bastão, dos leques, do chicote, da adaga e das lâminas duplas.",
                "Ensinar uma técnica secreta (proficiência 12 e 24 esmeraldas).",
                new Lacos.Historia("um mosteiro que nenhum mapa mostra", "de usar a força para ferir quem não precisava",
                        "ver um discípulo passar dele", "o bastão de bambu que já pertenceu a nove mestres",
                        "um bastão forjado do mosteiro", "O bambu dobra e não quebra. Lembre disso quando apanhar.",
                        new Material[]{Material.BAMBOO, Material.SWEET_BERRIES}),
                "Respire. O golpe nasce do silêncio.", "Mais devagar. A pressa é a primeira derrota.", "O vento não tem músculos e derruba árvores.", "Volte amanhã. O corpo aprende dormindo."),
        MESTRE_DE_GUERRA_ANAO(Estrutura.ALDEIA_ANOES, "Mestre de Guerra", new String[]{"Thorgar", "Bruni", "Hildra", "Grom"},
                Villager.Profession.WEAPONSMITH, Villager.Type.TAIGA, 0.65, new int[]{0, 1, 4},
                "Ensina as técnicas do machado, da maça, do montante, do mangual e da clava.",
                "Ensinar uma técnica secreta (proficiência 12 e 24 esmeraldas).",
                new Lacos.Historia("a guerra das Montanhas Partidas", "de a forja da aldeia se apagar",
                        "erguer de novo o salão dos ancestrais", "o martelo do pai, que ele nunca conseguiu levantar",
                        "uma maça anã forjada", "Pesada como a promessa de um anão. Não deixe cair.",
                        new Material[]{Material.IRON_INGOT, Material.COOKED_BEEF}),
                "Golpe pesado, coração leve!", "Escudo em cima, cabeça embaixo.", "Um anão nunca recua. Ele só... muda de lugar.", "Mais força no quadril, garoto!"),
        MESTRA_ELFICA(Estrutura.VILA_ELFICA, "Mestra da Lâmina", new String[]{"Galadwen", "Arwyn", "Thalindra", "Eldarya"},
                Villager.Profession.WEAPONSMITH, Villager.Type.JUNGLE, 1.05, new int[]{3, 1, 4},
                "Ensina as técnicas da espada, do arco, da foice e das garras.",
                "Ensinar uma técnica secreta (proficiência 12 e 24 esmeraldas).",
                new Lacos.Historia("a guarda da rainha élfica, que ela deixou sem explicar", "de ensinar alguém que use a lâmina por vaidade",
                        "dançar uma última vez a dança das espadas sob a lua", "uma cicatriz que ela diz ser de um dragão",
                        "um arco élfico forjado", "Ele escolhe o alvo antes de você. Confie nele.",
                        new Material[]{Material.ARROW, Material.GLOW_BERRIES}),
                "A lâmina é a extensão do pensamento.", "Leve. Mais leve. Assim.", "Quem dança não tropeça.", "O arco não perdoa um coração agitado."),

        // ---------- 2.56: o Nether (a NPC é um piglin; paga-se em ouro) ----------
        FERREIRO_PIGLIN(Estrutura.FORJA_PIGLIN, "Ferreiro Piglin", new String[]{"Grunk", "Zogrul", "Barrok", "Tuska"},
                null, null, 1, Ruinas.FERREIRO_PIGLIN,
                "Troca ouro por peças do Nether e ensina a técnica da Lança de Basalto.",
                "Ensinar a técnica secreta da Lança de Basalto (proficiência 12 e 24 lingotes de ouro).",
                new Lacos.Historia("um bastião que caiu quando os hoglins fugiram", "de que o ouro do mundo de cima acabe um dia",
                        "forjar uma coroa digna do primeiro ferreiro", "o martelo dele é feito de um pedaço do Primeiro Portal",
                        "uma Lança de Basalto forjada", "Quente ainda. Segure pelo cabo, humano. Pelo cabo!",
                        new Material[]{Material.GOLD_INGOT, Material.COOKED_PORKCHOP}),
                "Ouro primeiro. Conversa depois.", "Humano com ouro é humano amigo.", "A lava canta quando o metal está pronto.", "Grunf! Esse basalto é dos bons.");

        /** 2.54: as armas cujas técnicas secretas o mestre ensina (vazio = não é mestre). */
        public java.util.List<br.rpgatributos.combo.TipoArma> armas() {
            return switch (this) {
                case MESTRA_DAS_MARES -> java.util.List.of(br.rpgatributos.combo.TipoArma.TRIDENTE, br.rpgatributos.combo.TipoArma.LANCA,
                        br.rpgatributos.combo.TipoArma.ARPAO, br.rpgatributos.combo.TipoArma.ARREMESSO, br.rpgatributos.combo.TipoArma.BUMERANGUE);
                case MESTRE_DO_BASTAO -> java.util.List.of(br.rpgatributos.combo.TipoArma.BASTAO, br.rpgatributos.combo.TipoArma.LEQUES,
                        br.rpgatributos.combo.TipoArma.CHICOTE, br.rpgatributos.combo.TipoArma.ADAGA, br.rpgatributos.combo.TipoArma.DUPLAS);
                case MESTRE_DE_GUERRA_ANAO -> java.util.List.of(br.rpgatributos.combo.TipoArma.MACHADO, br.rpgatributos.combo.TipoArma.MACA,
                        br.rpgatributos.combo.TipoArma.MONTANTE, br.rpgatributos.combo.TipoArma.MANGUAL, br.rpgatributos.combo.TipoArma.CLAVA);
                case MESTRA_ELFICA -> java.util.List.of(br.rpgatributos.combo.TipoArma.ESPADA, br.rpgatributos.combo.TipoArma.ARCO,
                        br.rpgatributos.combo.TipoArma.FOICE, br.rpgatributos.combo.TipoArma.GARRAS);
                case FERREIRO_PIGLIN -> java.util.List.of(br.rpgatributos.combo.TipoArma.LANCA_BASALTO);
                default -> java.util.List.of();
            };
        }

        final Estrutura vila;
        final String titulo, servico, descricao;
        final String[] nomes, falas;
        final Villager.Profession profissao;
        final Villager.Type tipo;
        final double escala;
        final int[] lugar;
        final Lacos.Historia historia;

        Papel(Estrutura vila, String titulo, String[] nomes, Villager.Profession profissao, Villager.Type tipo, double escala, int[] lugar,
              String descricao, String servico, Lacos.Historia historia, String... falas) {
            this.vila = vila;
            this.titulo = titulo;
            this.nomes = nomes;
            this.profissao = profissao;
            this.tipo = tipo;
            this.escala = escala;
            this.lugar = lugar;
            this.descricao = descricao;
            this.servico = servico;
            this.historia = historia;
            this.falas = falas;
        }

        String nome(Sitio s) {
            return titulo + " " + nomes[Math.floorMod((s.id + name()).hashCode(), nomes.length)];
        }

        /** 2.56: a NPC é um piglin (sem profissão de aldeão; preços em ouro). */
        boolean piglin() {
            return profissao == null;
        }

        static List<Papel> da(Estrutura e) {
            List<Papel> l = new ArrayList<>();
            for (Papel p : values()) if (p.vila == e) l.add(p);
            return l;
        }
    }

    private final RPGAtributos plugin;
    private final Estruturas estruturas;
    /** "sitio:papel" → a entidade (para não procurar toda vez). */
    private final Map<String, UUID> vivos = new HashMap<>();
    /** "jogador:sitio:papel" → o dia em que o serviço foi usado. */
    private final Map<String, Long> servicoUsado = new HashMap<>();

    Vilas(RPGAtributos plugin, Estruturas estruturas) {
        this.plugin = plugin;
        this.estruturas = estruturas;
    }

    private static long hoje() {
        return LocalDate.now().toEpochDay();
    }

    // =====================================================================
    //  Os NPCs: nascem quando alguém chega perto e voltam se sumirem
    // =====================================================================

    /** Chamado pelo tick das estruturas quando um jogador está a até 40 blocos da vila. */
    void garantir(Sitio s, World w) {
        for (Papel papel : Papel.da(s.tipo)) {
            String chave = s.id + ":" + papel.name();
            UUID id = vivos.get(chave);
            Entity e = id == null ? null : Bukkit.getEntity(id);
            if (e != null && e.isValid()) {
                renovarLoja(s, papel, e);
                continue;
            }
            Location onde = s.ponto(w, papel.lugar[0], papel.lugar[1], papel.lugar[2]);
            if (!onde.isChunkLoaded() || !onde.getChunk().isEntitiesLoaded()) continue;
            Entity achado = null;
            for (Entity x : w.getNearbyEntities(onde, 12, 8, 12)) {
                if (chave.equals(x.getPersistentDataContainer().get(K_NPC, PersistentDataType.STRING))) achado = x;
            }
            if (achado == null) achado = nascer(s, papel, onde);
            vivos.put(chave, achado.getUniqueId());
        }
    }

    private Entity nascer(Sitio s, Papel papel, Location onde) {
        BlockFace frente = new Obra(onde.getWorld(), s.x, s.y, s.z, s.rot, null).face(BlockFace.SOUTH);
        onde.setDirection(frente.getDirection());
        if (papel.piglin()) {
            // 2.56: o Ferreiro Piglin (não vira zumbi no mundo de cima; a loja dele é à parte, em ouro).
            return onde.getWorld().spawn(onde, org.bukkit.entity.Piglin.class, m -> {
                m.customName(Component.text(papel.nome(s), s.tipo.cor()));
                m.setCustomNameVisible(true);
                m.setAI(false);
                m.setInvulnerable(true);
                m.setPersistent(true);
                m.setRemoveWhenFarAway(false);
                m.setImmuneToZombification(true);
                m.setAdult();
                m.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_AXE));
                m.getEquipment().setItemInMainHandDropChance(0);
                m.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                m.getEquipment().setHelmetDropChance(0);
                m.getPersistentDataContainer().set(K_NPC, PersistentDataType.STRING, s.id + ":" + papel.name());
            });
        }
        Villager v = onde.getWorld().spawn(onde, Villager.class, m -> {
            m.customName(Component.text(papel.nome(s), s.tipo.cor()));
            m.setCustomNameVisible(true);
            m.setAI(false);
            m.setInvulnerable(true);
            m.setPersistent(true);
            m.setRemoveWhenFarAway(false);
            m.setProfession(papel.profissao);
            m.setVillagerType(papel.tipo);
            m.setVillagerLevel(5);
            if (papel == Papel.MENINA_DO_CAIS) m.setBaby();
            m.getPersistentDataContainer().set(K_NPC, PersistentDataType.STRING, s.id + ":" + papel.name());
            var esc = m.getAttribute(Attribute.SCALE);
            if (esc != null && papel.escala != 1 && papel != Papel.MENINA_DO_CAIS) esc.setBaseValue(papel.escala);
        });
        renovarLoja(s, papel, v);
        return v;
    }

    /** 2.56: a loja do piglin (não é aldeão): uma por forja, refeita a cada dia. */
    private final Map<String, org.bukkit.inventory.Merchant> lojasPiglin = new HashMap<>();
    private final Map<String, Long> diaLojaPiglin = new HashMap<>();

    private org.bukkit.inventory.Merchant lojaPiglin(Sitio s, Papel papel, Entity npc) {
        String chave = s.id + ":" + papel.name();
        org.bukkit.inventory.Merchant m = lojasPiglin.get(chave);
        if (m == null || diaLojaPiglin.getOrDefault(chave, -1L) != hoje()) {
            m = Bukkit.createMerchant(Component.text(papel.nome(s), s.tipo.cor()));
            m.setRecipes(ofertas(s, papel, npc.getLocation()));
            lojasPiglin.put(chave, m);
            diaLojaPiglin.put(chave, hoje());
        }
        return m;
    }

    /** As ofertas mudam a cada dia (sempre as mesmas no mesmo dia). */
    private void renovarLoja(Sitio s, Papel papel, Entity e) {
        if (!(e instanceof Villager v)) return;
        Long dia = v.getPersistentDataContainer().get(K_DIA, PersistentDataType.LONG);
        if (dia != null && dia == hoje()) return;
        v.setRecipes(ofertas(s, papel, v.getLocation()));
        v.getPersistentDataContainer().set(K_DIA, PersistentDataType.LONG, hoje());
    }

    // =====================================================================
    //  Lojas
    // =====================================================================

    private record Oferta(ItemStack item, ItemStack preco, int usos) { }

    private static Oferta vende(ItemStack item, int esmeraldas, int usos) {
        return new Oferta(item, new ItemStack(Material.EMERALD, esmeraldas), usos);
    }

    /** O NPC compra: o jogador dá o item e recebe esmeraldas. */
    private static Oferta compra(Material m, int qtd, int esmeraldas, int usos) {
        return new Oferta(new ItemStack(Material.EMERALD, esmeraldas), new ItemStack(m, qtd), usos);
    }

    private static ItemStack encantado(Material m, Enchantment e, int nivel) {
        ItemStack i = new ItemStack(m);
        i.addUnsafeEnchantment(e, nivel);
        return i;
    }

    private List<MerchantRecipe> ofertas(Sitio s, Papel papel, Location onde) {
        Random r = new Random((s.id + papel.name()).hashCode() * 31L + hoje());
        List<Oferta> l = new ArrayList<>();
        String nome = papel.nome(s);
        switch (papel) {
            case VELHO_PESCADOR -> {
                l.add(vende(encantado(Material.FISHING_ROD, Enchantment.LURE, 2), 8, 2));
                l.add(vende(encantado(Material.FISHING_ROD, Enchantment.LUCK_OF_THE_SEA, 2), 10, 2));
                l.add(vende(new ItemStack(Material.NAME_TAG), 5, 2));
                l.add(vende(Album.carta(Carta.values()[r.nextInt(Carta.values().length)], false), 5, 2));
                l.add(vende(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde), 16, 1));
            }
            case PEIXEIRA -> {
                l.add(compra(Material.COD, 6, 1, 8));
                l.add(compra(Material.SALMON, 4, 1, 8));
                l.add(compra(Material.TROPICAL_FISH, 2, 1, 6));
                l.add(compra(Material.PUFFERFISH, 1, 1, 6));
                l.add(compra(Material.INK_SAC, 6, 1, 6));
            }
            case CARPINTEIRO_NAVAL -> {
                l.add(vende(new ItemStack(Material.OAK_BOAT), 2, 3));
                l.add(vende(new ItemStack(Material.OAK_CHEST_BOAT), 4, 2));
                l.add(vende(new ItemStack(Material.LEAD, 2), 2, 4));
                l.add(vende(new ItemStack(Material.SPYGLASS), 6, 1));
                l.add(vende(new ItemStack(Material.MAP), 3, 2));
            }
            case MESTRE -> {
                l.add(vende(br.rpgatributos.arcano.ItensMagicos.tomoAleatorio(), 14, 1));
                l.add(vende(new ItemStack(Material.BAMBOO, 16), 1, 4));
                l.add(vende(new ItemStack(Material.SHIELD), 5, 2));
                l.add(vende(Elixir.values()[r.nextInt(Elixir.values().length)].criar(Qualidade.BOA, 1.1, nome), 12, 2));
                l.add(vende(new ItemStack(Material.TARGET), 2, 3));
            }
            case HERBALISTA -> {
                Erva[] ervas = Erva.values();
                l.add(vende(ervas[r.nextInt(ervas.length)].criar(3), 4, 4));
                l.add(vende(ervas[r.nextInt(ervas.length)].criar(3), 4, 4));
                l.add(vende(Elixir.values()[r.nextInt(Elixir.values().length)].criar(Qualidade.BOA, 1.1, nome), 12, 2));
                l.add(vende(Bebida.values()[r.nextInt(Bebida.values().length)].criar(1), 8, 2));
                l.add(vende(Fruta.sortear(r).muda(1), 7, 2));
            }
            case GUARDIAO_DO_SINO -> {
                l.add(vende(new ItemStack(Material.CANDLE, 4), 2, 4));
                l.add(vende(new ItemStack(Material.BELL), 12, 1));
                l.add(vende(new ItemStack(Material.LANTERN, 2), 2, 4));
                l.add(vende(Raro.PEDRA_DE_PROTECAO.criar(1), 24, 1));
                l.add(vende(new ItemStack(Material.BOOK, 2), 2, 4));
            }
            case PEREGRINO -> {
                l.add(vende(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde), 16, 1));
                l.add(vende(Raro.MAPA_RASGADO.criar(1), 14, 1));
                l.add(vende(Album.pacote(1), 8, 2));
                l.add(vende(new ItemStack(Material.COMPASS), 4, 2));
                l.add(vende(new ItemStack(Material.BREAD, 6), 1, 4));
            }
            case MESTRE_MINEIRO -> {
                l.add(vende(new ItemStack(Material.TORCH, 24), 1, 6));
                l.add(vende(new ItemStack(Material.LANTERN, 3), 2, 4));
                l.add(vende(new ItemStack(Material.TNT, 3), 4, 2));
                l.add(vende(encantado(Material.IRON_PICKAXE, Enchantment.EFFICIENCY, 3), 12, 1));
                l.add(vende(Raro.MAPA_RASGADO.criar(1), 14, 1));
            }
            case JOALHEIRO -> {
                l.add(vende(Gema.values()[r.nextInt(Gema.values().length)].criar(1, 1), 10, 2));
                l.add(vende(Gema.values()[r.nextInt(Gema.values().length)].criar(2, 1), 18, 1));
                l.add(compra(Material.DIAMOND, 1, 3, 6));
                l.add(compra(Material.AMETHYST_SHARD, 8, 1, 6));
                l.add(compra(Material.LAPIS_LAZULI, 16, 1, 6));
                // 2.46: os minerais de bioma.
                l.add(new Oferta(new ItemStack(Material.EMERALD, 2), Raro.AMBAR.criar(2), 6));
                l.add(new Oferta(new ItemStack(Material.EMERALD, 2), Raro.JADE.criar(2), 6));
                l.add(new Oferta(new ItemStack(Material.EMERALD, 3), Raro.PEROLA.criar(2), 6));
            }
            case FERREIRO_ANAO -> {
                l.add(vende(Raro.FRAGMENTO_DE_FORJA.criar(1), 20, 1));
                l.add(vende(new ItemStack(Material.ANVIL), 8, 1));
                l.add(vende(new ItemStack(Material.IRON_INGOT, 4), 3, 4));
                l.add(vende(encantado(Material.IRON_AXE, Enchantment.SHARPNESS, 3), 12, 1));
                l.add(compra(Material.COAL, 16, 1, 8));
            }
            case TAVERNEIRO -> {
                Bebida[] b = Bebida.values();
                l.add(vende(b[r.nextInt(b.length)].criar(1), 6, 3));
                l.add(vende(b[r.nextInt(b.length)].criar(1), 6, 3));
                l.add(vende(new ItemStack(Material.COOKED_BEEF, 6), 2, 4));
                l.add(vende(Album.pacote(1), 8, 2));
                l.add(vende(new ItemStack(Material.BREAD, 8), 1, 4));
                l.add(compra(Material.WHEAT, 20, 1, 8));
            }
            case DRUIDA -> {
                l.add(vende(Fruta.sortear(r).muda(1), 7, 2));
                l.add(vende(new ItemStack(Material.BONE_MEAL, 16), 2, 4));
                l.add(vende(new ItemStack(Material.MOSS_BLOCK, 8), 2, 4));
                l.add(vende(new ItemStack(Material.CHERRY_SAPLING, 2), 3, 3));
                l.add(vende(Erva.values()[r.nextInt(Erva.values().length)].criar(3), 4, 4));
            }
            case ARQUEIRA -> {
                Flechas.Tipo[] tipos = Flechas.Tipo.values();
                l.add(vende(Flechas.criar(tipos[r.nextInt(tipos.length)], 8), 6, 3));
                l.add(vende(Flechas.criar(tipos[r.nextInt(tipos.length)], 8), 6, 3));
                l.add(vende(new ItemStack(Material.ARROW, 32), 2, 4));
                l.add(vende(encantado(Material.BOW, Enchantment.POWER, 3), 12, 1));
                l.add(compra(Material.FEATHER, 16, 1, 6));
            }
            case CURANDEIRA -> {
                l.add(vende(Elixir.values()[r.nextInt(Elixir.values().length)].criar(Qualidade.OTIMA, 1.2, nome), 16, 2));
                l.add(vende(new ItemStack(Material.GOLDEN_APPLE), 18, 1));
                l.add(vende(Reagente.values()[r.nextInt(Reagente.values().length)].criar(2), 8, 3));
                l.add(vende(new ItemStack(Material.GLOW_BERRIES, 8), 1, 4));
                l.add(vende(new ItemStack(Material.HONEY_BOTTLE, 3), 2, 4));
            }
            case FERREIRO_PIGLIN -> {
                // Tudo em lingotes de ouro: o piglin vende peças do Nether e compra os minerais de lá.
                l.add(new Oferta(Raro.CRISTAL_DE_MAGMA.criar(2), new ItemStack(Material.GOLD_INGOT, 10), 3));
                l.add(new Oferta(br.rpgatributos.combo.Arsenal.criar(br.rpgatributos.combo.Arsenal.Arma.LANCA_BASALTO,
                        br.rpgatributos.combo.Arsenal.Arma.LANCA_BASALTO.base), new ItemStack(Material.GOLD_INGOT, 32), 1));
                l.add(new Oferta(new ItemStack(Material.NETHERITE_SCRAP), new ItemStack(Material.GOLD_INGOT, 24), 1));
                l.add(new Oferta(new ItemStack(Material.GILDED_BLACKSTONE, 4), new ItemStack(Material.GOLD_INGOT, 6), 3));
                l.add(new Oferta(new ItemStack(Material.SOUL_LANTERN, 3), new ItemStack(Material.GOLD_INGOT, 4), 3));
                l.add(new Oferta(new ItemStack(Material.GOLD_INGOT, 6), Raro.AMBAR_INFERNAL.criar(1), 6));
                l.add(new Oferta(new ItemStack(Material.GOLD_INGOT, 8), Raro.QUARTZO_NEGRO.criar(1), 6));
                if (r.nextBoolean()) l.add(new Oferta(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Material.GOLD_INGOT, 40), 1));
            }
            case TECELA -> {
                Material[] laas = {Material.LIME_WOOL, Material.GREEN_WOOL, Material.PINK_WOOL, Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL};
                l.add(vende(new ItemStack(laas[r.nextInt(laas.length)], 16), 2, 4));
                l.add(vende(new ItemStack(Material.WHITE_BANNER), 2, 3));
                l.add(vende(new ItemStack(Material.STRING, 16), 2, 4));
                l.add(vende(Album.carta(Carta.values()[r.nextInt(Carta.values().length)], false), 5, 2));
                l.add(compra(Material.WHITE_WOOL, 12, 1, 6));
            }
            default -> { }
        }
        if (l.isEmpty()) return List.of();
        Collections.shuffle(l, r);
        List<MerchantRecipe> receitas = new ArrayList<>();
        for (Oferta o : l) {
            MerchantRecipe m = new MerchantRecipe(o.item(), 0, o.usos(), false, 0, 0f);
            m.addIngredient(o.preco());
            receitas.add(m);
        }
        return receitas;
    }

    // =====================================================================
    //  O menu do NPC: laço, loja e serviço
    // =====================================================================

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        Sitio sitio;
        Papel papel;
        UUID npc;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int M_INFO = 4, M_LACO = 11, M_LOJA = 13, M_SERVICO = 15, M_DUELO = 22, M_CONVERSA = 20;

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEntityEvent e) {
        String v = e.getRightClicked().getPersistentDataContainer().get(K_NPC, PersistentDataType.STRING);
        if (v == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        int i = v.lastIndexOf(':');
        Sitio s = estruturas.sitio(v.substring(0, i));
        Papel papel;
        try {
            papel = Papel.valueOf(v.substring(i + 1));
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (s == null) return;
        abrir(e.getPlayer(), s, papel, e.getRightClicked());
    }

    private Lacos.Alvo alvo(Player p, Sitio s, Papel papel, Entity npc) {
        return new Lacos.Alvo(npc.getUniqueId(), papel.nome(s), papel.historia, null, () -> {
            // O capítulo 5 também dá a carta do morador (só se ganha assim).
            Carta carta = Carta.porId(papel.name());
            if (carta != null) plugin.album().darCarta(p, carta, false); // os Mestres de Armas (2.54) ainda não têm carta
            return presente(papel, p.getLocation());
        });
    }

    /** O serviço só pode ser usado uma vez por dia (o conserto e a cura são pagos e não têm limite). */
    private static boolean diario(Papel papel) {
        return papel != Papel.FERREIRO_ANAO && papel != Papel.CURANDEIRA && papel != Papel.CARPINTEIRO_NAVAL;
    }

    private void abrir(Player p, Sitio s, Papel papel, Entity npc) {
        Tela t = new Tela();
        t.sitio = s;
        t.papel = papel;
        t.npc = npc.getUniqueId();
        t.inv = Bukkit.createInventory(t, 27, Component.text(papel.nome(s)));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.itemName(Component.text(" ")));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        t.inv.setItem(M_INFO, item(Material.NAME_TAG, Component.text(papel.nome(s), s.tipo.cor(), TextDecoration.BOLD), List.of(
                Component.text(s.tipo.nome(), NamedTextColor.GRAY),
                Component.text(papel.descricao, NamedTextColor.WHITE),
                Component.empty(),
                Component.text("\"" + papel.falas[new Random(hoje() + papel.ordinal()).nextInt(papel.falas.length)] + "\"", COR))));
        t.inv.setItem(M_LACO, item(Material.POPPY, Component.text("♥ Laço com " + papel.nome(s), TextColor.color(0xFF8FA8), TextDecoration.BOLD),
                plugin.lacos().descricao(p, alvo(p, s, papel, npc), papel.servico != null ? "Usar o serviço dele: +1 (uma vez por dia)" : null)));
        if (npc instanceof Villager v && !v.getRecipes().isEmpty() || papel.piglin()) {
            t.inv.setItem(M_LOJA, item(papel.piglin() ? Material.GOLD_INGOT : Material.EMERALD,
                    Component.text("Loja", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                    Component.text(papel.piglin() ? "Só aceita ouro. As ofertas mudam a cada dia." : "As ofertas mudam a cada dia.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        }
        if (papel.servico != null) {
            boolean usado = diario(papel) && servicoUsado.getOrDefault(p.getUniqueId() + ":" + s.id + ":" + papel.name(), -1L) == hoje();
            t.inv.setItem(M_SERVICO, item(Material.BELL, Component.text("✦ Serviço", COR, TextDecoration.BOLD), List.of(
                    Component.text(papel.servico, NamedTextColor.WHITE),
                    Component.empty(),
                    Component.text(usado ? "Já usado hoje. Volte amanhã." : "» Clique para usar", usado ? NamedTextColor.DARK_GRAY : NamedTextColor.YELLOW))));
        }
        t.inv.setItem(M_CONVERSA, item(Material.WRITABLE_BOOK, Component.text("💬 Conversar", TextColor.color(0xFF8FA8), TextDecoration.BOLD), List.of(
                Component.text("Uma conversa de verdade, com perguntas", NamedTextColor.GRAY),
                Component.text("que se abrem conforme o laço cresce.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique para conversar", NamedTextColor.YELLOW))));
        t.inv.setItem(M_DUELO, item(Material.PAPER, Component.text("🃏 Duelo de Cartas", TextColor.color(0xF2C14E), TextDecoration.BOLD), List.of(
                Component.text("Dificuldade: " + br.rpgatributos.vida.Duelos.nomeDificuldade(papel.name()), NamedTextColor.WHITE),
                Component.text("Vença para ganhar esmeraldas e Pacotes", NamedTextColor.GRAY),
                Component.text("de Cartas (um garantido por dia).", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique para duelar", NamedTextColor.YELLOW))));
        p.openInventory(t.inv);
        p.playSound(npc.getLocation(), papel.piglin() ? Sound.ENTITY_PIGLIN_AMBIENT : Sound.ENTITY_VILLAGER_AMBIENT, 1f, papel.escala < 1 ? 0.8f : 1.1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, c.hasDecoration(TextDecoration.ITALIC))).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        Entity npc = Bukkit.getEntity(t.npc);
        if (npc == null || !npc.isValid()) {
            p.closeInventory();
            return;
        }
        switch (e.getSlot()) {
            case M_LACO -> {
                plugin.lacos().interagir(p, alvo(p, t.sitio, t.papel, npc), t.papel.falas);
                abrir(p, t.sitio, t.papel, npc);
            }
            case M_LOJA -> {
                if (npc instanceof Villager v && !v.getRecipes().isEmpty()) p.openMerchant(v, true);
                else if (t.papel.piglin()) p.openMerchant(lojaPiglin(t.sitio, t.papel, npc), true);
            }
            case M_SERVICO -> {
                if (t.papel.servico == null) return;
                if (!t.papel.armas().isEmpty()) { // Mestre de Armas: a tela das técnicas
                    abrirTecnicas(p, t.sitio, t.papel, npc);
                    return;
                }
                String chave = p.getUniqueId() + ":" + t.sitio.id + ":" + t.papel.name();
                boolean jaHoje = servicoUsado.getOrDefault(chave, -1L) == hoje();
                if (diario(t.papel) && jaHoje) {
                    p.sendActionBar(Component.text("Já usado hoje. Volte amanhã.", NamedTextColor.GRAY));
                    return;
                }
                if (servico(p, t.papel, npc)) {
                    if (!jaHoje) plugin.lacos().somar(p, alvo(p, t.sitio, t.papel, npc), 1);
                    servicoUsado.put(chave, hoje());
                }
                p.closeInventory();
            }
            case M_DUELO -> plugin.duelos().contraNpc(p, t.papel.name(), t.papel.nome(t.sitio));
            case M_CONVERSA -> {
                p.closeInventory();
                Sitio s = t.sitio;
                Papel papel = t.papel;
                plugin.conversas().abrir(p, alvo(p, s, papel, npc), papel.piglin() ? Material.PIGLIN_HEAD : Material.EMERALD, papel.falas,
                        () -> abrir(p, s, papel, npc));
            }
            default -> { }
        }
    }

    // =====================================================================
    //  Serviços
    // =====================================================================

    // =====================================================================
    //  2.54: Mestres de Armas e as técnicas secretas
    // =====================================================================

    private static final int TECNICA_PROFICIENCIA = 12, TECNICA_CUSTO = 24;

    private static final class TelaTecnicas implements InventoryHolder {
        Inventory inv;
        Sitio sitio;
        Papel papel;
        UUID npc;
        final Map<Integer, br.rpgatributos.combo.Golpe> golpes = new HashMap<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private void abrirTecnicas(Player p, Sitio s, Papel papel, Entity npc) {
        TelaTecnicas t = new TelaTecnicas();
        t.sitio = s;
        t.papel = papel;
        t.npc = npc.getUniqueId();
        t.inv = Bukkit.createInventory(t, 27, Component.text("✪ Técnicas de " + papel.nome(s)));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.itemName(Component.text(" ")));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        int[] slots = {11, 12, 13, 14, 15};
        List<br.rpgatributos.combo.TipoArma> armas = papel.armas();
        for (int i = 0; i < armas.size() && i < slots.length; i++) {
            br.rpgatributos.combo.TipoArma arma = armas.get(i);
            br.rpgatributos.combo.Golpe g = br.rpgatributos.combo.Combos.tecnicaDe(arma);
            if (g == null) continue;
            boolean sabe = plugin.combos().aprendeu(p, g);
            int prof = plugin.combos().nivel(p, arma);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(arma.simbolo() + " " + arma.nome(), arma.cor()));
            lore.add(Component.text(g.descricao().replace("Técnica secreta: ", ""), NamedTextColor.GRAY));
            lore.add(Component.text("⚡ " + (int) g.vigor() + " vigor · ⏱ " + g.recarga() + " s", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            if (sabe) {
                lore.add(Component.text("✔ Você já sabe. Ponha no /combos.", NamedTextColor.GREEN));
            } else {
                lore.add(Component.text((prof >= TECNICA_PROFICIENCIA ? "✔" : "✖") + " Proficiência " + TECNICA_PROFICIENCIA + " (você: " + prof + ")",
                        prof >= TECNICA_PROFICIENCIA ? NamedTextColor.GREEN : NamedTextColor.RED));
                lore.add(Component.text("Custa " + TECNICA_CUSTO + (papel.piglin() ? " lingotes de ouro" : " esmeraldas"), NamedTextColor.WHITE));
                lore.add(Component.text("» Clique para aprender", NamedTextColor.YELLOW));
            }
            t.inv.setItem(slots[i], item(g.icone(), Component.text("✪ " + g.nome(), NamedTextColor.AQUA, TextDecoration.BOLD), lore));
            t.golpes.put(slots[i], g);
        }
        t.inv.setItem(22, item(Material.BOOK, Component.text("Como funciona", COR, TextDecoration.BOLD), List.of(
                Component.text("Cada arma tem uma técnica secreta, um golpe", NamedTextColor.GRAY),
                Component.text("extra que vai em qualquer sequência no /combos.", NamedTextColor.GRAY),
                Component.text("Os outros povos ensinam as outras armas.", NamedTextColor.DARK_GRAY))));
        p.openInventory(t.inv);
    }

    @EventHandler
    public void aoClicarTecnica(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TelaTecnicas t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        br.rpgatributos.combo.Golpe g = t.golpes.get(e.getRawSlot());
        if (g == null || plugin.combos().aprendeu(p, g)) return;
        Entity npc = Bukkit.getEntity(t.npc);
        if (npc == null || !npc.isValid()) {
            p.closeInventory();
            return;
        }
        if (plugin.combos().nivel(p, g.tipo()) < TECNICA_PROFICIENCIA) {
            p.sendActionBar(Component.text("✖ Treine mais: precisa de proficiência " + TECNICA_PROFICIENCIA + " com " + g.tipo().nome() + ".", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            return;
        }
        if (!(t.papel.piglin() ? pagarOuro(p, TECNICA_CUSTO) : pagar(p, TECNICA_CUSTO))) return;
        plugin.combos().ensinar(p, g);
        plugin.lacos().somar(p, alvo(p, t.sitio, t.papel, npc), 2);
        plugin.titulos().registrar(p, "tecnicas", 1);
        plugin.diario().marco(p, "tecnica_" + g.id(), "Aprendeu a técnica secreta " + g.nome() + " com " + t.papel.nome(t.sitio));
        p.showTitle(net.kyori.adventure.title.Title.title(Component.text("✪ " + g.nome(), NamedTextColor.AQUA, TextDecoration.BOLD),
                Component.text("Técnica secreta aprendida. Ponha no /combos.", NamedTextColor.GRAY)));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        p.sendMessage(Component.text("✪ \"" + t.papel.falas[0] + "\"", COR));
        abrirTecnicas(p, t.sitio, t.papel, npc);
    }

    @EventHandler
    public void aoArrastarTecnica(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TelaTecnicas) e.setCancelled(true);
    }

    /** 2.56: o Ferreiro Piglin só aceita ouro. */
    private static boolean pagarOuro(Player p, int lingotes) {
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.GOLD_INGOT), lingotes)) {
            p.sendActionBar(Component.text("✖ Precisa de " + lingotes + " lingotes de ouro.", NamedTextColor.RED));
            return false;
        }
        p.getInventory().removeItem(new ItemStack(Material.GOLD_INGOT, lingotes));
        return true;
    }

    private static boolean pagar(Player p, int esmeraldas) {
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), esmeraldas)) {
            p.sendActionBar(Component.text("✖ Precisa de " + esmeraldas + " esmeraldas.", NamedTextColor.RED));
            return false;
        }
        p.getInventory().removeItem(new ItemStack(Material.EMERALD, esmeraldas));
        return true;
    }

    /** Faz o serviço do NPC. @return se aconteceu (para contar o laço e o "uma vez por dia"). */
    private boolean servico(Player p, Papel papel, Entity npc) {
        World w = p.getWorld();
        Location aqui = npc.getLocation().add(0, 1.2, 0);
        switch (papel) {
            case CARPINTEIRO_NAVAL -> {
                if (!pagar(p, 3)) return false;
                for (ItemStack sobra : p.getInventory().addItem(new ItemStack(Material.OAK_CHEST_BOAT)).values()) w.dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("⚓ \"Pronto, novinho em folha. Não bata em pedra.\"", COR));
                return true;
            }
            case MENINA_DO_CAIS -> {
                Material[][] pedidos = {{Material.COD}, {Material.SALMON}, {Material.TROPICAL_FISH}, {Material.SEA_PICKLE}, {Material.KELP}};
                int[] quantos = {5, 3, 1, 2, 8};
                int i = (int) Math.floorMod(hoje() + p.getUniqueId().hashCode(), pedidos.length);
                Material m = pedidos[i][0];
                if (!p.getInventory().containsAtLeast(new ItemStack(m), quantos[i])) {
                    p.sendMessage(Component.text("❀ \"Hoje eu queria " + quantos[i] + " " + nomeItem(m) + "... você me ajuda?\"", COR));
                    return false;
                }
                p.getInventory().removeItem(new ItemStack(m, quantos[i]));
                java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
                List<ItemStack> premio = new ArrayList<>(List.of(new ItemStack(Material.EMERALD, 4 + r.nextInt(5))));
                if (r.nextDouble() < 0.4) premio.add(Album.carta(Carta.values()[r.nextInt(Carta.values().length)], r.nextDouble() < 0.15));
                for (ItemStack it : premio) for (ItemStack sobra : p.getInventory().addItem(it).values()) w.dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("❀ \"Obrigada! Toma, guardei para você.\"", COR));
                w.spawnParticle(Particle.HEART, aqui, 5, 0.3, 0.3, 0.3);
                return true;
            }
            case MESTRE -> {
                plugin.stats().darXp(p, Skill.COMBATE, 120);
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 300, 0));
                p.sendMessage(Component.text("☯ \"De novo. Mais devagar. Agora sim.\" Você treinou com o Mestre (+XP de Combate e Força por 5 min).", COR));
                w.playSound(aqui, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                return true;
            }
            case GUARDIAO_DO_SINO -> {
                Party pt = plugin.parties().party(p);
                for (Player x : w.getNearbyPlayers(aqui, 16)) {
                    if (!x.equals(p) && (pt == null || !pt.tem(x.getUniqueId()))) continue;
                    x.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 180, 0));
                    x.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 180, 0));
                }
                w.playSound(aqui, Sound.BLOCK_BELL_USE, 2f, 0.8f);
                w.playSound(aqui, Sound.BLOCK_BELL_RESONATE, 1.5f, 0.8f);
                p.sendMessage(Component.text("🔔 O sino ecoa pela montanha. Você e a sua party se sentem protegidos.", COR));
                return true;
            }
            case PEREGRINO -> {
                String[] historias = {
                        "Dizem que o Qilin só aparece onde há paz de verdade.",
                        "No fundo do mar existe uma cidade que ainda reza para Maris.",
                        "Os anões juram que a Forja deles vai reacender um dia.",
                        "Vi um tigre branco na neve. Ele me olhou como gente.",
                        "Quem acende uma Raiz de Luz devolve um pedaço do sol às cavernas.",
                        "O Nian tem medo de barulho. Guarde fogos para o inverno.",
                        "Acima das nuvens há ilhas onde o vento ainda lembra de quem morou lá.",
                        "O Pixiu come ouro e nunca devolve. Mas protege quem o alimenta."};
                java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
                p.sendMessage(Component.text("✎ " + papelNome(npc) + ": \"" + historias[r.nextInt(historias.length)] + "\"", COR));
                if (r.nextDouble() < 0.2) {
                    for (ItemStack sobra : p.getInventory().addItem(Raro.PAGINA_DE_LENDA.criar(1)).values()) w.dropItemNaturally(p.getLocation(), sobra);
                    p.sendMessage(Component.text("  Ele arranca uma página do caderno e te dá.", NamedTextColor.GOLD));
                }
                return true;
            }
            case FERREIRO_ANAO -> {
                ItemStack mao = p.getInventory().getItemInMainHand();
                if (!(mao.getItemMeta() instanceof Damageable d) || !d.hasDamage() || d.getDamage() <= 0) {
                    p.sendActionBar(Component.text("Segure na mão o item que precisa de conserto.", NamedTextColor.GRAY));
                    return false;
                }
                int custo = Math.max(2, Math.min(20, d.getDamage() / 100 + 2));
                if (!pagar(p, custo)) return false;
                mao.editMeta(Damageable.class, x -> x.setDamage(0));
                w.playSound(aqui, Sound.BLOCK_ANVIL_USE, 1f, 1f);
                w.spawnParticle(Particle.LAVA, aqui, 6, 0.3, 0.2, 0.3);
                p.sendMessage(Component.text("⚒ \"Pronto. Melhor do que quando saiu da forja.\" (" + custo + " esmeraldas)", COR));
                return true;
            }
            case DRUIDA -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20, 4));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 120, 0));
                w.spawnParticle(Particle.CHERRY_LEAVES, p.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.02);
                p.sendMessage(Component.text("❦ O Druida encosta a mão na sua testa. O bosque te abraça.", COR));
                return true;
            }
            case CURANDEIRA -> {
                if (!pagar(p, 5)) return false;
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(max.getValue());
                for (PotionEffectType t : new PotionEffectType[]{PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.WEAKNESS,
                        PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
                        PotionEffectType.HUNGER, PotionEffectType.DARKNESS}) p.removePotionEffect(t);
                plugin.profundezas().curarPenumbra(p);
                w.spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
                p.sendMessage(Component.text("✚ \"Pronto. Agora vá devagar.\" Vida cheia, venenos e Penumbra fora.", COR));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static String papelNome(Entity npc) {
        Component n = npc.customName();
        return n == null ? "Ele" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(n);
    }

    private static String nomeItem(Material m) {
        return switch (m) {
            case COD -> "bacalhaus";
            case SALMON -> "salmões";
            case TROPICAL_FISH -> "peixe tropical";
            case SEA_PICKLE -> "pepinos-do-mar";
            case KELP -> "algas";
            default -> m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    // =====================================================================
    //  O presente do capítulo 5 do laço
    // =====================================================================

    private ItemStack presente(Papel papel, Location onde) {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        return switch (papel) {
            case VELHO_PESCADOR -> plugin.forja().criarNomeada(Nomeada.VARA_DE_CORAL, Raridade.EPICO);
            case PEIXEIRA -> plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde);
            case CARPINTEIRO_NAVAL -> nomeado(new ItemStack(Material.OAK_CHEST_BOAT), "Barco do Carpinteiro");
            case MENINA_DO_CAIS -> nomeado(new ItemStack(Material.NAUTILUS_SHELL), "Concha que Fala");
            case MESTRE -> plugin.forja().criarNomeada(Nomeada.AGULHA_DOS_VENTOS, Raridade.EPICO);
            case HERBALISTA -> Elixir.values()[r.nextInt(Elixir.values().length)].criar(Qualidade.OTIMA, 1.3, "Monge Herbalista");
            case GUARDIAO_DO_SINO -> Raro.PEDRA_DE_PROTECAO.criar(1);
            case PEREGRINO -> Raro.PAGINA_DE_LENDA.criar(1);
            case MESTRE_MINEIRO -> plugin.forja().criarNomeada(Nomeada.PICARETA_DO_POCO_FUNDO, Raridade.EPICO);
            case JOALHEIRO -> Gema.values()[r.nextInt(Gema.values().length)].criar(2, 2);
            case FERREIRO_ANAO -> Raro.FRAGMENTO_DE_FORJA.criar(2);
            case TAVERNEIRO -> Bebida.values()[r.nextInt(Bebida.values().length)].criar(2);
            case DRUIDA -> Fruta.sortear(new Random()).muda(2);
            case ARQUEIRA -> plugin.forja().criarNomeada(Nomeada.ARCO_DAS_FOLHAS_CAIDAS, Raridade.EPICO);
            case CURANDEIRA -> new ItemStack(Material.GOLDEN_APPLE, 2);
            case TECELA -> Album.carta(Album.sortear(r, true), true);
            case MESTRA_DAS_MARES -> plugin.forja().forjar(null, new ItemStack(Material.TRIDENT), Raridade.EPICO);
            case MESTRE_DO_BASTAO -> plugin.forja().forjar(null, br.rpgatributos.combo.Arsenal.criar(br.rpgatributos.combo.Arsenal.Arma.BASTAO, Material.DIAMOND_SWORD), Raridade.EPICO);
            case MESTRE_DE_GUERRA_ANAO -> plugin.forja().forjar(null, new ItemStack(Material.MACE), Raridade.EPICO);
            case MESTRA_ELFICA -> plugin.forja().forjar(null, new ItemStack(Material.BOW), Raridade.EPICO);
            case FERREIRO_PIGLIN -> plugin.forja().forjar(null, br.rpgatributos.combo.Arsenal.criar(br.rpgatributos.combo.Arsenal.Arma.LANCA_BASALTO,
                    br.rpgatributos.combo.Arsenal.Arma.LANCA_BASALTO.base), Raridade.EPICO);
        };
    }

    private static ItemStack nomeado(ItemStack i, String nome) {
        i.editMeta(m -> m.itemName(Component.text(nome, COR)));
        return i;
    }

    // =====================================================================
    //  As construções (coordenadas locais; frente para o sul, chão em dy 0)
    // =====================================================================

    /** Vila de Pescadores: deck de tábuas sobre estacas, cabana, banca de peixe, estaleiro e píer. */
    static void pescadores(Obra o) {
        o.encher(-7, 7, 2, 9, -7, 7, Material.AIR);
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 2; dz++) o.por(dx, 1, dz, Material.SPRUCE_PLANKS);
        for (int dx = -1; dx <= 1; dx++) for (int dz = 3; dz <= 7; dz++) o.por(dx, 1, dz, Material.OAK_PLANKS);
        for (int dx : new int[]{-6, -2, 2, 6}) for (int dz : new int[]{-6, -2, 2}) for (int dy = -2; dy <= 0; dy++) o.por(dx, dy, dz, "spruce_log[axis=y]");
        for (int dx : new int[]{-1, 1}) for (int dz : new int[]{5, 7}) for (int dy = -2; dy <= 0; dy++) o.por(dx, dy, dz, "oak_log[axis=y]");
        // Lanternas na ponta do píer.
        for (int dx : new int[]{-1, 1}) {
            o.por(dx, 2, 7, Material.OAK_FENCE);
            o.por(dx, 3, 7, Material.LANTERN);
        }
        // Cabana do velho pescador.
        for (int dx = -6; dx <= -2; dx++) {
            for (int dz = -6; dz <= -2; dz++) {
                boolean parede = dx == -6 || dx == -2 || dz == -6 || dz == -2;
                boolean canto = (dx == -6 || dx == -2) && (dz == -6 || dz == -2);
                for (int dy = 2; dy <= 4; dy++) {
                    if (canto) o.por(dx, dy, dz, "stripped_spruce_log[axis=y]");
                    else if (parede) o.por(dx, dy, dz, Material.SPRUCE_PLANKS);
                }
            }
        }
        o.por(-4, 2, -2, "spruce_door[facing=north,half=lower,hinge=left]");
        o.por(-4, 3, -2, "spruce_door[facing=north,half=upper,hinge=left]");
        o.por(-6, 3, -4, Material.GLASS_PANE);
        o.por(-4, 3, -6, Material.GLASS_PANE);
        for (int dx = -7; dx <= -1; dx++) for (int dz = -7; dz <= -1; dz++) o.por(dx, 5, dz, "spruce_slab[type=bottom]");
        for (int dx = -5; dx <= -3; dx++) for (int dz = -5; dz <= -3; dz++) o.por(dx, 6, dz, "spruce_slab[type=bottom]");
        o.por(-5, 2, -5, "barrel[facing=up]");
        o.por(-3, 2, -5, "red_bed[facing=west,part=head]");
        o.por(-3, 2, -4, Material.AIR);
        o.por(-5, 4, -3, "lantern[hanging=true]");
        // Banca da peixeira: toldo listrado, barris e gelo.
        for (int[] p : new int[][]{{2, -6}, {6, -6}, {2, -3}, {6, -3}}) for (int dy = 2; dy <= 4; dy++) o.por(p[0], dy, p[1], Material.SPRUCE_FENCE);
        for (int dx = 2; dx <= 6; dx++) for (int dz = -6; dz <= -3; dz++) o.por(dx, 5, dz, (dx % 2 == 0) ? Material.LIGHT_BLUE_WOOL : Material.WHITE_WOOL);
        for (int dx = 3; dx <= 5; dx++) o.por(dx, 2, -4, "barrel[facing=up]");
        o.por(3, 2, -5, Material.PACKED_ICE);
        o.por(5, 2, -5, "water_cauldron[level=3]");
        o.por(4, 2, -6, Material.SMOKER);
        // Estaleiro: casco em construção, toras e bancada.
        for (int dx = 1; dx <= 6; dx++) o.por(dx, 2, 2, dx == 1 || dx == 6 ? "oak_stairs[facing=east]" : "oak_slab[type=bottom]");
        o.por(6, 2, 0, Material.CRAFTING_TABLE);
        o.por(6, 2, -1, "oak_log[axis=x]");
        o.por(5, 2, -1, "oak_log[axis=x]");
        o.por(2, 2, 0, Material.GRINDSTONE);
        // Varais de peixe e redes (teias) nas estacas.
        o.por(0, 2, -6, Material.SPRUCE_FENCE);
        o.por(0, 3, -6, Material.COBWEB);
        o.por(-1, 2, 2, "barrel[facing=up]");
    }

    /** Mosteiro da Montanha: pátio murado, salão de pilares vermelhos, torre do sino e horta. */
    static void mosteiro(Obra o) {
        o.encher(-7, 7, 1, 12, -7, 7, Material.AIR);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) o.por(dx, 0, dz, o.um(Material.STONE_BRICKS, 5, Material.POLISHED_ANDESITE, 2));
        // Muro baixo com portão ao sul.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean borda = Math.abs(dx) == 7 || Math.abs(dz) == 7;
                if (!borda || (dz == 7 && Math.abs(dx) <= 1)) continue;
                o.por(dx, 1, dz, Material.STONE_BRICK_WALL);
            }
        }
        for (int[] p : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) o.por(p[0], 2, p[1], Material.LANTERN);
        // Salão: pilares vermelhos, paredes claras, telhado de telhas escuras em dois níveis.
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -7; dz <= -3; dz++) {
                o.por(dx, 0, dz, Material.DARK_OAK_PLANKS);
                boolean borda = Math.abs(dx) == 4 || dz == -7 || dz == -3;
                boolean pilar = (Math.abs(dx) == 4 || dx == 0) && (dz == -7 || dz == -3) || Math.abs(dx) == 4 && dz == -5;
                for (int dy = 1; dy <= 4; dy++) {
                    if (pilar) o.por(dx, dy, dz, "stripped_mangrove_log[axis=y]");
                    else if (borda && dy <= 3) o.por(dx, dy, dz, Material.WHITE_TERRACOTTA);
                    else if (borda) o.por(dx, dy, dz, Material.DARK_OAK_PLANKS);
                }
            }
        }
        o.por(0, 1, -3, Material.AIR);
        o.por(0, 2, -3, Material.AIR);
        o.por(-1, 1, -3, Material.AIR);
        o.por(-1, 2, -3, Material.AIR);
        o.por(1, 1, -3, Material.AIR);
        o.por(1, 2, -3, Material.AIR);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -7; dz <= -2; dz++) {
                boolean beiral = Math.abs(dx) == 5 || dz == -2;
                o.por(dx, 5, dz, beiral ? "deepslate_tile_slab[type=bottom]" : "deepslate_tiles");
            }
        }
        for (int[] c : new int[][]{{-5, -2}, {5, -2}}) o.por(c[0], 5, c[1], "deepslate_tile_stairs[facing=north,half=top]");
        for (int dx = -3; dx <= 3; dx++) for (int dz = -6; dz <= -3; dz++) o.por(dx, 6, dz, Math.abs(dx) == 3 || dz == -3 ? "deepslate_tile_slab[type=bottom]" : "deepslate_tiles");
        for (int dx = -1; dx <= 1; dx++) o.por(dx, 7, -5, "deepslate_tile_slab[type=bottom]");
        o.por(0, 7, -4, Material.GOLD_BLOCK);
        // Dentro: altar com velas e um púlpito.
        o.por(0, 1, -6, Material.CHISELED_STONE_BRICKS);
        o.por(0, 2, -6, "candle[candles=3,lit=true]");
        o.por(-2, 1, -6, Material.LECTERN);
        o.por(2, 1, -6, "potted_bamboo");
        o.por(0, 4, -5, "lantern[hanging=true]");
        // Torre do sino.
        for (int[] p : new int[][]{{4, 1}, {6, 1}, {4, 3}, {6, 3}}) for (int dy = 1; dy <= 4; dy++) o.por(p[0], dy, p[1], "stripped_mangrove_log[axis=y]");
        for (int dx = 3; dx <= 7; dx++) for (int dz = 0; dz <= 4; dz++) o.por(dx, 5, dz, "deepslate_tile_slab[type=bottom]");
        o.por(5, 4, 2, "bell[attachment=ceiling,facing=south]");
        // Horta das ervas.
        for (int dx = -6; dx <= -3; dx++) {
            for (int dz = 2; dz <= 6; dz++) {
                o.por(dx, 0, dz, dx == -5 && dz == 4 ? Material.WATER : Material.FARMLAND);
                if (dx == -5 && dz == 4 || !o.chance(0.7)) continue;
                if (o.chance(0.5)) o.por(dx, 1, dz, "sweet_berry_bush[age=3]");
                else o.por(dx, 1, dz, o.um(Material.FERN, 1, Material.AZALEA, 1));
            }
        }
        // Pátio de treino: alvos e fardos.
        o.por(-1, 1, 3, Material.TARGET);
        o.por(1, 1, 3, Material.TARGET);
        o.por(0, 1, 4, Material.HAY_BLOCK);
        // Lanternas de pedra no caminho.
        for (int dx : new int[]{-3, 3}) {
            o.por(dx, 1, 6, Material.STONE_BRICK_WALL);
            o.por(dx, 2, 6, Material.LANTERN);
        }
    }

    /** Aldeia dos Anões: salão de pedra com forja, joalheria e taverna, e a boca da mina. */
    static void anoes(Obra o) {
        o.encher(-7, 7, 1, 10, -7, 7, Material.AIR);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) o.por(dx, 0, dz, dz <= 2 ? Material.POLISHED_DEEPSLATE : o.um(Material.COBBLESTONE, 3, Material.GRAVEL, 1));
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 2; dz++) {
                boolean parede = Math.abs(dx) == 6 || dz == -6 || dz == 2;
                for (int dy = 1; dy <= 5; dy++) {
                    if (parede) o.por(dx, dy, dz, dy == 1 ? Material.DEEPSLATE_BRICKS : o.um(Material.STONE_BRICKS, 4, Material.CRACKED_STONE_BRICKS, 1));
                }
                o.por(dx, 6, dz, Material.STONE_BRICKS);
            }
        }
        // Portão largo com arco.
        for (int dx = -1; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++) o.por(dx, dy, 2, Material.AIR);
        o.por(-1, 3, 2, "stone_brick_stairs[facing=east,half=top]");
        o.por(1, 3, 2, "stone_brick_stairs[facing=west,half=top]");
        o.por(-2, 3, 3, Material.LANTERN);
        o.por(2, 3, 3, Material.LANTERN);
        o.por(-2, 2, 3, Material.STONE_BRICK_WALL);
        o.por(2, 2, 3, Material.STONE_BRICK_WALL);
        o.por(-2, 1, 3, Material.STONE_BRICK_WALL);
        o.por(2, 1, 3, Material.STONE_BRICK_WALL);
        // Forja.
        o.por(5, 1, -5, Material.BLAST_FURNACE);
        o.por(4, 1, -5, Material.ANVIL);
        o.por(3, 1, -5, Material.LAVA_CAULDRON);
        o.por(5, 1, -3, Material.GRINDSTONE);
        for (int dy = 6; dy <= 8; dy++) o.por(5, dy, -5, Material.BRICKS);
        o.por(5, 9, -5, "campfire[lit=true]");
        // Joalheria.
        o.por(-5, 1, -5, Material.SMITHING_TABLE);
        o.por(-5, 2, -5, Material.AMETHYST_CLUSTER);
        o.por(-3, 1, -5, "chest[facing=south]");
        o.por(-4, 1, -5, Material.CARTOGRAPHY_TABLE);
        // Taverna.
        o.por(-5, 1, -1, "barrel[facing=east]");
        o.por(-5, 2, -1, "barrel[facing=east]");
        o.por(-5, 1, 0, "barrel[facing=east]");
        o.por(-5, 1, 1, Material.CAMPFIRE);
        o.por(-2, 1, 1, Material.OAK_FENCE);
        o.por(-2, 2, 1, "oak_pressure_plate");
        o.por(-1, 1, 1, "oak_stairs[facing=west]");
        // Luz por dentro.
        for (int[] p : new int[][]{{-3, -3}, {3, -3}, {0, 0}, {0, -4}}) o.por(p[0], 5, p[1], "lantern[hanging=true]");
        // Boca da mina: arco de toras e trilhos descendo.
        for (int dz = 4; dz <= 7; dz++) o.por(5, 1, dz, "rail[shape=north_south]");
        for (int dy = 1; dy <= 3; dy++) {
            o.por(4, dy, 6, "spruce_log[axis=y]");
            o.por(6, dy, 6, "spruce_log[axis=y]");
        }
        for (int dx = 4; dx <= 6; dx++) o.por(dx, 4, 6, "spruce_log[axis=x]");
        o.por(5, 3, 6, "lantern[hanging=true]");
    }

    /** Vila Élfica: duas árvores grandes com plataformas, lago, tear e o canto da curandeira. */
    static void elfica(Obra o) {
        o.encher(-7, 7, 1, 14, -7, 7, Material.AIR);
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) o.por(dx, 0, dz, o.um(Material.MOSS_BLOCK, 3, Material.GRASS_BLOCK, 2));
        arvoreElfica(o, -4, -4, Material.AZALEA_LEAVES);
        arvoreElfica(o, 4, -4, Material.CHERRY_LEAVES);
        // Lago com vitórias-régias.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = 4; dz <= 6; dz++) {
                if (Math.abs(dx) == 2 && dz != 5) continue;
                o.por(dx, -1, dz, Material.CLAY);
                o.por(dx, 0, dz, Material.WATER);
                if (o.chance(0.3)) o.por(dx, 1, dz, Material.LILY_PAD);
            }
        }
        // Flores e cogumelos pelo chão.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -1; dz <= 7; dz++) {
                if (Math.abs(dx) <= 2 && dz >= 3 || !o.chance(0.18)) continue;
                if (dx == -4 && dz == 4 || dx == 0 && dz == 2 || dx == 4 && dz == -1 || dx == -3 && dz == -2) continue; // onde os moradores ficam
                o.por(dx, 1, dz, o.um(Material.ALLIUM, 2, Material.LILY_OF_THE_VALLEY, 2, Material.PINK_TULIP, 1, Material.RED_MUSHROOM, 1, Material.FERN, 2));
            }
        }
        // Tecelã: tear e lãs.
        o.por(-2, 1, -2, Material.LOOM);
        o.por(-2, 1, -1, Material.LIME_WOOL);
        o.por(-1, 1, -1, Material.PINK_WOOL);
        // Curandeira: alambique e caldeirão.
        o.por(5, 1, -1, Material.BREWING_STAND);
        o.por(5, 1, 0, "water_cauldron[level=3]");
        o.por(6, 1, -1, "potted_red_mushroom");
        // Arqueira: alvos.
        o.por(-6, 1, 6, Material.TARGET);
        o.por(-6, 1, 5, Material.HAY_BLOCK);
        // Lanternas penduradas nas árvores e luzes no chão.
        o.por(0, 1, -1, "lantern");
        o.por(-6, 1, 1, Material.SHROOMLIGHT);
        o.por(6, 1, 3, Material.SHROOMLIGHT);
    }

    /** Tronco 2x2 de carvalho escuro, plataforma de bétula com cerca, escada e copa. */
    private static void arvoreElfica(Obra o, int cx, int cz, Material folha) {
        for (int dy = 1; dy <= 9; dy++) {
            for (int ax = 0; ax <= 1; ax++) for (int az = 0; az <= 1; az++) o.por(cx + ax - 1, dy, cz + az - 1, "dark_oak_log[axis=y]");
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = cx + dx, z = cz + dz;
                if (dx >= -1 && dx <= 0 && dz >= -1 && dz <= 0) continue;
                o.por(x, 5, z, Material.BIRCH_PLANKS);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) o.por(x, 6, z, Material.BIRCH_FENCE);
            }
        }
        o.por(cx, 6, cz + 2, Material.AIR);
        for (int dy = 1; dy <= 5; dy++) o.por(cx, dy, cz + 1, "ladder[facing=south]");
        o.por(cx + 1, 6, cz + 1, Material.LANTERN);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 9; dy <= 12; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + (dy - 10) * (dy - 10) * 1.6);
                    if (dist > 3.4) continue;
                    if (dx >= -1 && dx <= 0 && dz >= -1 && dz <= 0 && dy <= 9) continue;
                    o.por(cx + dx, dy, cz + dz, dist < 1.2 ? "shroomlight" : folha.getKey().getKey() + "[persistent=true]");
                }
            }
        }
    }
}
