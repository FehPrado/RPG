package br.rpgatributos;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * O "Guia do Aventureiro": livro que todo jogador recebe na primeira vez que entra,
 * explicando atributos, Forja, Infusor, magias, Altar, plantações, Cozinha, Reciclagem,
 * party, territórios, domador, masmorras, classes, títulos e missões.
 * As cores são escuras porque a página do livro é clara.
 */
public final class Guia implements Listener {

    private static final TextColor TITULO = NamedTextColor.DARK_PURPLE;
    private static final TextColor FORJA = TextColor.color(0xA0430A);
    private static final TextColor ARCANO = TextColor.color(0x6A1B9A);
    private static final TextColor ALTAR = TextColor.color(0x8B0000);
    private static final TextColor FAZENDA = TextColor.color(0x2E7D32);
    private static final TextColor COZINHA = TextColor.color(0xB35900);
    private static final TextColor PARTY = TextColor.color(0x1E6FA8);
    private static final TextColor TERRITORIO = TextColor.color(0x2E7D32);
    private static final TextColor DOMADOR = TextColor.color(0x8B5A2B);
    private static final TextColor MASMORRA = TextColor.color(0x5E35B1);
    private static final TextColor CLASSE = TextColor.color(0xB8860B);
    private static final TextColor LENDA = TextColor.color(0xC2185B);
    private static final TextColor VIAGEM = TextColor.color(0x5E35B1);
    private static final TextColor OCULTO = TextColor.color(0x4E342E);
    private static final TextColor PESCA = TextColor.color(0x1565C0);
    private static final TextColor ALQUIMIA = TextColor.color(0x00796B);
    private static final TextColor VISUAL = TextColor.color(0xAD1457);
    private static final TextColor PORTAL = TextColor.color(0x0277BD);
    private static final TextColor SOMBRA = TextColor.color(0x4527A0);
    private static final TextColor COMBO = TextColor.color(0xB35900);
    private static final TextColor FE = TextColor.color(0x8D6E00);
    private static final TextColor EXPLORACAO = TextColor.color(0x5D4037);
    private static final TextColor PROGRESSAO = TextColor.color(0x6A4C93);
    private static final TextColor DETALHES = TextColor.color(0x6D4C41);
    private static final TextColor TEXTO = NamedTextColor.BLACK;
    private static final TextColor DESTAQUE = NamedTextColor.DARK_BLUE;

    private final RPGAtributos plugin;
    private final NamespacedKey kRecebido;

    public Guia(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kRecebido = new NamespacedKey(plugin, "guia_recebido");
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.settings().guiaAoEntrar || p.getPersistentDataContainer().has(kRecebido)) return;
        p.getPersistentDataContainer().set(kRecebido, PersistentDataType.BYTE, (byte) 1);
        // Um pouco depois de entrar, para a mensagem não se perder no meio das outras.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            dar(p);
            p.sendMessage(Component.text("✎ Você recebeu o ", NamedTextColor.GOLD)
                    .append(Component.text("Guia do Aventureiro", NamedTextColor.YELLOW))
                    .append(Component.text(". Leia para saber como ficar mais forte!", NamedTextColor.GOLD)));
        }, 40L);
    }

    public void dar(Player p) {
        p.getInventory().addItem(criar()).values()
                .forEach(sobra -> p.getWorld().dropItemNaturally(p.getLocation(), sobra));
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    public ItemStack criar() {
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text("Guia do Aventureiro"));
            m.author(Component.text("O Servidor"));
            m.pages(paginas(plugin.settings().nivelMaximo));
        });
        return livro;
    }

    private static Component t(String texto) {
        return Component.text(texto, TEXTO);
    }

    private static Component titulo(String texto, TextColor cor) {
        return Component.text(texto + "\n\n", cor, TextDecoration.BOLD);
    }

    private static Component d(String texto) {
        return Component.text(texto, DESTAQUE);
    }

    // Uma página de livro cabe ~14 linhas de ~19 letras: os textos são curtos de propósito.
    private static List<Component> paginas(int nivelMaximo) {
        return List.of(
                // 1
                Component.text("   Guia do\n  Aventureiro\n", TITULO, TextDecoration.BOLD)
                        .append(t("3  Forja\n7  Infusor/Magias\n17 Altar/Fazenda\n"
                                + "26 Party/Reinos\n33 Domador\n35 Masmorras/Perigo\n39 Céu/Portais/Torre\n45 Combos/Classes\n52 Fé/Exploração\n61 Talentos e mais\n71 Pesca/Alquimia\n78 Conquistas")),
                // 2
                titulo("Atributos", TITULO)
                        .append(t("Sobem fazendo as coisas (até o nível " + nivelMaximo + "):\n\n"))
                        .append(t("⛏ minerar ♣ cortar\n» correr ⬆ pular\n⚔ lutar ⚒ forjar\n✦ magia ≈ nadar\n☘ colher ♨ cozinhar\n♞ domar ⚓ pescar\n⚗ alquimia\n"))
                        .append(t("Veja: ")).append(d("/atributos")),
                // 3
                titulo("Forja: criar", FORJA)
                        .append(t("1. Coloque uma "))
                        .append(d("bigorna"))
                        .append(t(".\n2. Jogue em cima (tecla Q):\n"))
                        .append(d(" • 2 blocos de ferro\n • 1 balde de lava\n\n"))
                        .append(t("A bigorna vira a Forja do Ferreiro.")),
                // 4
                titulo("Forja: usar", FORJA)
                        .append(d("Clique direito"))
                        .append(t(": bancada do ferreiro.\n\n"))
                        .append(t("Armas, ferramentas e armaduras feitas nela saem com raridade, bônus e efeitos.\n\n"))
                        .append(t("Bancada comum: itens normais.")),
                // 5
                titulo("Raridades", FORJA)
                        .append(t("★ Comum\n★★ Raro\n★★★ Épico\n★★★★ Único\n★★★★★ Lendário\n★★★★★★ Mítico\n\n"))
                        .append(t("Ferraria alta = chances melhores.\n"))
                        .append(t("4 peças: bônus de conjunto.")),
                // 6
                titulo("Refino", FORJA)
                        .append(d("Agachado + clique"))
                        .append(t(" na Forja: refinar de +1 a +10.\n"))
                        .append(t("Cada nível deixa o item mais forte.\n\n"))
                        .append(t("Do +6 pode perder nível; no +9 e +10 pode quebrar!\n"))
                        .append(d("Pedra de Proteção")).append(t(" evita.")),
                // 7
                titulo("Infusor: criar", ARCANO)
                        .append(t("1. Coloque um "))
                        .append(d("caldeirão com água"))
                        .append(t(".\n2. Jogue dentro (tecla Q):\n"))
                        .append(d(" • 1 mesa de encantamento\n • 1 bigorna\n\n"))
                        .append(t("Uma mesa mágica vai flutuar em cima.")),
                // 8
                titulo("Infundir", ARCANO)
                        .append(t("Clique no Infusor, escolha um item e uma parte do corpo.\n"))
                        .append(d("A parte"))
                        .append(t(" dá bônus: mana, vida, dano ou velocidade.\n"))
                        .append(d("O item"))
                        .append(t(" dá essências, que liberam magias.\n"))
                        .append(t("Gasta XP e pode falhar.")),
                // 9
                titulo("Cuidado!", NamedTextColor.DARK_RED)
                        .append(t("Cada item pesa no corpo. Passou da "))
                        .append(d("carga"))
                        .append(t(", o corpo rejeita: fome, fraqueza e náusea.\n\n"))
                        .append(t("Ao morrer, as infusões ficam "))
                        .append(d("instáveis"))
                        .append(t(" por 5 minutos: magias mais fracas e caras.")),
                // 10
                titulo("Grimório", ARCANO)
                        .append(t("Pegue no Infusor (custa 1 livro).\n\nSegurando ele:\n"))
                        .append(d("Direito"))
                        .append(t(": lança\n"))
                        .append(d("Esquerdo"))
                        .append(t(": troca\n"))
                        .append(d("Agachado+direito"))
                        .append(t(": abre\n\n"))
                        .append(t("A mana fica no topo da tela.")),
                // 11
                titulo("Criar magias", ARCANO)
                        .append(t("No grimório, escolha de 1 a 4 essências do seu corpo e uma forma:\n\n"))
                        .append(d("Toque, Projétil, Aura, Corpo ou Criação.\n\n"))
                        .append(t("Mesmas essências com outra forma = outra magia!")),
                // 12
                titulo("Segredos", ARCANO)
                        .append(t("Algumas combinações exatas têm efeitos únicos.\n\n"))
                        .append(t("O grimório dá uma dica de cada uma. Experimente!\n\n"))
                        .append(t("Quem descobre, o servidor inteiro fica sabendo.")),
                // 13
                titulo("Formas novas", ARCANO)
                        .append(d("Raio")).append(t(": segure o clique.\n"))
                        .append(d("Sopro")).append(t(": cone.\n"))
                        .append(d("Chuva")).append(t(": cai do céu.\n"))
                        .append(d("Invocação")).append(t(": um elemental luta por você.\n"))
                        .append(d("Encantar arma")).append(t(": golpes com o elemento.")),
                // 14
                titulo("Modificadores", ARCANO)
                        .append(t("Até 2 por magia:\n"))
                        .append(d("Dividir, Teleguiado, Ricochete, Ampliar, Mina, Eco, Canalizar"))
                        .append(t(" (segure para carregar), "))
                        .append(d("Persistente, Rápida, Potente"))
                        .append(t(".")),
                // 15
                titulo("Reações", ARCANO)
                        .append(t("Um elemento marca o alvo; outro causa uma reação:\n"))
                        .append(d("Água+Energia")).append(t(" choque\n"))
                        .append(d("Água+Gelo")).append(t(" congela\n"))
                        .append(d("Fogo+Gelo")).append(t(" derrete\n"))
                        .append(t("...e mais 7. Combine com a party!")),
                // 16
                titulo("Maestria e cajado", ARCANO)
                        .append(t("Usar um elemento sobe a maestria dele (nível 5: variante).\n\n"))
                        .append(d("Cajado Arcano")).append(t(": 1 ametista + 2 varas de blaze na Forja. Lança magias e leva gemas.")),
                // 17
                titulo("Altar: criar", ALTAR)
                        .append(t("1. Coloque uma "))
                        .append(d("obsidiana chorosa"))
                        .append(t(".\n2. Jogue em cima (tecla Q):\n"))
                        .append(d(" • 1 olho do ender\n • 4 velas\n\n"))
                        .append(t("Clique nele para ver o que ele invoca.")),
                // 18
                titulo("Altar: invocar", ALTAR)
                        .append(t("Jogue a oferenda em cima do altar ou use o menu dele.\n\n"))
                        .append(d("Chefes, Lua de Sangue, Meteoros, Ondas de Monstros, Mercador Arcano"))
                        .append(t(" e "))
                        .append(d("Caçador de Recompensas")).append(t(".")),
                // 19
                titulo("Chefes", ALTAR)
                        .append(t("Golem, Rainha Aracnídea, Senhor das Chamas, Lich, Tempestade Viva e Arauto do Fim.\n\n"))
                        .append(t("Deixam "))
                        .append(d("Fragmentos de Forja"))
                        .append(t(", Pedras de Proteção e "))
                        .append(d("núcleos"))
                        .append(t(": infunda para um poder lendário!")),
                // 20
                titulo("Plantações", FAZENDA)
                        .append(t("Colher plantação madura dá XP de Agricultura.\n\n"))
                        .append(t("Nível alto: colheita em dobro, qualidade "))
                        .append(d("★ a ★★★"))
                        .append(t(", plantas crescendo perto de você e às vezes uma colheita "))
                        .append(d("GIGANTE")).append(t("!")),
                // 21
                titulo("Adubo", FAZENDA)
                        .append(d("Adubo Rico"))
                        .append(t(": 3 farinhas de osso + carne podre + terra. Clique na plantação: colheita melhor.\n\n"))
                        .append(d("Espantalho"))
                        .append(t(": suporte com abóbora na cabeça. Ninguém pisoteia perto.")),
                // 22
                titulo("Sementes raras", FAZENDA)
                        .append(t("Plantas diferentes maduras lado a lado:\n"))
                        .append(d("Trigo+Cenoura\n")).append(t("→ Trigo Dourado\n"))
                        .append(d("Cenoura+Batata\n")).append(t("→Cenoura Cristalina\n"))
                        .append(d("Batata+Beterraba\n")).append(t("→ Batata Ancestral\n"))
                        .append(d("Beterraba+Trigo\n")).append(t("→ Beterraba Rubi")),
                // 23
                titulo("Cozinha: criar", COZINHA)
                        .append(t("1. Coloque um "))
                        .append(d("defumador"))
                        .append(t(".\n2. Jogue em cima (tecla Q):\n"))
                        .append(d(" • 1 caldeirão\n • 1 balde de água\n\n"))
                        .append(d("Clique")).append(t(": receitas\n"))
                        .append(d("Agachado")).append(t(": defumador")),
                // 24
                titulo("Pratos", COZINHA)
                        .append(t("Comer um prato dá buffs. Culinária alta libera receitas e buffs mais longos.\n\n"))
                        .append(t("Ingredientes "))
                        .append(d("★★"))
                        .append(t(" melhoram o prato. Sementes raras entram nos melhores!\n\n"))
                        .append(d("/receitas")),
                // 25
                titulo("Reciclagem", FORJA)
                        .append(t("1. Coloque um "))
                        .append(d("rebolo"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 funil\n • 1 tesoura\n\n"))
                        .append(t("Desmonta itens e devolve parte do material. Encantos podem virar livro!")),
                // 26
                titulo("Party", PARTY)
                        .append(t("Jogue em grupo:\n"))
                        .append(d("/party convidar <nick>\n"))
                        .append(t("• sem fogo amigo\n• XP de Combate dividido com quem está perto\n• chat: "))
                        .append(d("/pc\n"))
                        .append(t("• aviso de vida baixa")),
                // 27
                titulo("Território", TERRITORIO)
                        .append(t("1. Coloque uma "))
                        .append(d("magnetita"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 estandarte\n • 4 esmeraldas\n"))
                        .append(t("Vira o Marco: protege 3x3 chunks. Nível alto = mais chunks.\n"))
                        .append(d("Expansão"))
                        .append(t(": outro Marco longe, +1 bloco de esmeralda.")),
                // 28
                titulo("Proteção", TERRITORIO)
                        .append(t("Só você e seus membros constroem, abrem baús e mexem em animais.\n\n"))
                        .append(d("/territorio"))
                        .append(t(": mapa, membros e regras (PvP, portas, baús, explosões...).")),
                // 29
                titulo("Colônia", TERRITORIO)
                        .append(t("No seu território, jogue em cima de um "))
                        .append(d("sino"))
                        .append(t(":\n"))
                        .append(d(" • 1 bloco de esmeralda\n • 1 cama\n\n"))
                        .append(t("Vira a Prefeitura. Moradores chegam se houver camas e comida.")),
                // 30
                titulo("Moradores", TERRITORIO)
                        .append(t("Dê um trabalho a cada um. Precisam de posto perto: composteira, barril, Cozinha, Forja...\n"))
                        .append(t("Produzem no "))
                        .append(d("baú de depósito"))
                        .append(t(".\n"))
                        .append(d("/colonia")),
                // 31
                titulo("Reino", TERRITORIO)
                        .append(t("Com colônia nível 2: "))
                        .append(d("/reino fundar <nome>"))
                        .append(t(".\nCada território de membro é uma "))
                        .append(d("província"))
                        .append(t(". O Rei decreta leis (paz, incêndios...).\n"))
                        .append(t("Moradores viram "))
                        .append(d("Soldados"))
                        .append(t(" (Quartel = bloco de alvo).")),
                // 32
                titulo("Guerra", TERRITORIO)
                        .append(d("/reino guerra <reino>"))
                        .append(t(": marcada para o horário de guerra do servidor.\nPvP livre e captura de Marcos (fique perto sem defensores). Quem perde, perde chunks e tesouro. Nada é destruído.")),
                // 33
                titulo("Domador", DOMADOR)
                        .append(t("1. Coloque um "))
                        .append(d("fardo de feno"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 laço\n • 1 sela\n\n"))
                        .append(t("Traga um animal no laço até o altar para "))
                        .append(d("vincular"))
                        .append(t(", ou "))
                        .append(d("invoque"))
                        .append(t(" um com os componentes dele.")),
                // 34
                titulo("Companheiros", DOMADOR)
                        .append(t("Mão vazia + clique: ordens (seguir, ficar, guardar, patrulhar). Nunca teleporta.\n"))
                        .append(t("No altar: componentes (sela, baú, asas...).\n"))
                        .append(d("Sela + élitro = voa!\n"))
                        .append(d("/pets")),
                // 35
                titulo("Masmorras", MASMORRA)
                        .append(t("1. Coloque "))
                        .append(d("tijolos de pedra entalhados"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 olho do ender\n • 1 bússola\n\n"))
                        .append(t("Clique, escolha a dificuldade e suba no portal com a party.")),
                // 36
                titulo("Lá dentro", MASMORRA)
                        .append(t("Salas trancam e soltam ondas de monstros. Tesouros e um chefe no fim.\n\n"))
                        .append(t("Morreu? Itens no "))
                        .append(d("Cofre das Almas"))
                        .append(t(": 2 diamantes no Portal.\n"))
                        .append(d("/masmorra sair")),
                // 37
                titulo("Mundo perigoso", MASMORRA)
                        .append(d("Elites"))
                        .append(t(": monstros com poderes (Veloz, Vampiro, Gigante...) e loot melhor.\n"))
                        .append(d("Ninhos"))
                        .append(t(": derrote os guardiões e quebre o núcleo.\n"))
                        .append(t("A cada semana o mundo fica mais forte.")),
                // 38
                titulo("Hordas e chefes", MASMORRA)
                        .append(t("Em algumas noites a "))
                        .append(d("colônia é atacada"))
                        .append(t(": defenda!\n"))
                        .append(d("Chefe Mundial"))
                        .append(t(": aparece anunciado no mapa.\n"))
                        .append(d("/bestiario"))
                        .append(t(": +dano contra o que você caça.")),
                // 39
                titulo("Estações", FAZENDA)
                        .append(t("Cada uma dura 1 semana real.\n"))
                        .append(d("Primavera")).append(t(": plantas rápidas\n"))
                        .append(d("Inverno")).append(t(": neve, gelo e plantas lentas fora de estufa.\n"))
                        .append(t("Dia 4: "))
                        .append(d("festival"))
                        .append(t(" com XP extra e lembrança.\n"))
                        .append(d("/calendario")),
                // 40
                titulo("Lua e céu", MASMORRA)
                        .append(d("Lua cheia")).append(t(": monstros fortes e lobisomens.\n"))
                        .append(d("Lua nova")).append(t(": magias fortes e vampiros.\n"))
                        .append(t("Lua de Sangue, meteoros, eclipse e aurora!\nMordido? "))
                        .append(d("/maldicao")),
                // 41
                titulo("Portais", PORTAL)
                        .append(t("Portais se abrem sozinhos perto de quem explora.\n\n"))
                        .append(d("Entre"))
                        .append(t(": é uma masmorra aberta a todos. Vença o chefe e o portal se fecha.\n"))
                        .append(d("/portais")),
                // 42
                titulo("Transbordar", PORTAL)
                        .append(t("Ninguém fechou a tempo? O portal "))
                        .append(d("transborda"))
                        .append(t(": ondas de monstros e um chefe atacam a região e as colônias.\n"))
                        .append(t("Vença o chefe para conter!")),
                // 43
                titulo("Soberano", SOMBRA)
                        .append(t("Um Portal Pesadelo raro traz o "))
                        .append(d("Eco do Soberano"))
                        .append(t(". Quem o fechar com uma classe avançada dominada vira o Soberano das Sombras (só um!).\n"))
                        .append(d("/sombras")),
                // 44
                titulo("Torre Infinita", PORTAL)
                        .append(t("Obsidiana chorona + olho do ender + bloco de ouro (Q).\n"))
                        .append(t("Chefe e baú a cada 10 andares. Morrer não perde itens!\n"))
                        .append(d("/torre")),
                // 45
                titulo("Combos de arma", COMBO)
                        .append(t("Arma na mão + 3 cliques seguidos:\n"))
                        .append(d("D D D, D E D, D D E, D E E"))
                        .append(t("\n(D = direito, E = esquerdo; arco começa com E)\n"))
                        .append(t("Gasta "))
                        .append(d("vigor"))
                        .append(t(". Usar a arma sobe a proficiência e libera golpes. Lendas e classes têm golpes próprios.\n"))
                        .append(d("/combos")),
                // 46
                titulo("Mobilidade", COMBO)
                        .append(d("A A, D D ou S S"))
                        .append(t(" rápido: esquiva (gasta vigor).\n"))
                        .append(d("Gancho"))
                        .append(t(": vara + gancho de armadilha + 3 ferros.\n"))
                        .append(d("Capa Planadora"))
                        .append(t(": no ar, pule para planar.\n"))
                        .append(d("/mochila"))
                        .append(t(": cresce com o nível.")),
                // 47
                titulo("Classes", CLASSE)
                        .append(t("Escolha um caminho no "))
                        .append(d("Santuário"))
                        .append(t(" (púlpito + livro e pena + diamante) ou no "))
                        .append(d("/classe"))
                        .append(t(".\nFaça as tarefas e passe na "))
                        .append(d("Prova"))
                        .append(t(".\n\nGuerreiro, Arqueiro, Mago, Ladino, Ferreiro e Domador.")),
                // 48
                titulo("Habilidades", CLASSE)
                        .append(d("Agache + F"))
                        .append(t(": usa\n"))
                        .append(d("Agache + clique esq."))
                        .append(t(": troca\n\n"))
                        .append(t("Classe no nível 20 = "))
                        .append(d("Dominada"))
                        .append(t(": libera as avançadas (algumas pedem duas!).")),
                // 49
                titulo("Lendas", LENDA)
                        .append(t("Itens "))
                        .append(d("Especiais"))
                        .append(t(" e únicos que nascem dos seus feitos. Os feitos são segredo: decifre o enigma ou ache "))
                        .append(d("Páginas do Livro das Lendas"))
                        .append(t(".\n"))
                        .append(d("/lendas")),
                // 50
                titulo("Pedras de Viagem", VIAGEM)
                        .append(t("1. Coloque uma "))
                        .append(d("pedra entalhada de ardósia"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 2 pérolas do ender\n • 4 ametistas\n\n"))
                        .append(t("Clique nas pedras que achar e viaje entre elas pagando XP.")),
                // 51
                titulo("Locais Ocultos", OCULTO)
                        .append(t("Ruínas escondidas. Siga "))
                        .append(d("Mapas Rasgados"))
                        .append(t(" e os sussurros.\nResolva o enigma, vença o guardião e toque o "))
                        .append(d("selo"))
                        .append(t(": relíquias e uma "))
                        .append(d("classe lendária"))
                        .append(t("!")),
                // 52
                titulo("Deuses", FE)
                        .append(t("Quartzo entalhado + maçã dourada + vela (Q): "))
                        .append(d("Santuário"))
                        .append(t(". Escolha um dos 6 deuses e jogue oferendas (Q) nele.\n"))
                        .append(t("Fé 1 a 5 fortalece o passivo; na 2 libera o "))
                        .append(d("milagre"))
                        .append(t(".\n"))
                        .append(d("/deus")),
                // 53
                titulo("Astronomia", FE)
                        .append(t("À noite, olhe para o céu aberto pela "))
                        .append(d("luneta"))
                        .append(t(" por 5 s.\n\nCada noite tem uma das 12 constelações, com uma bênção até o amanhecer.\n"))
                        .append(d("/estrelas")),
                // 54
                titulo("Transmutação", FE)
                        .append(t("Bloco de ametista + Mercúrio Vivo + lingote de ouro (Q).\n\n"))
                        .append(t("Ferro em ouro, ouro em diamante... pode falhar ou explodir!\n"))
                        .append(d("Pedra Filosofal"))
                        .append(t(": +20% de chance.")),
                // 55
                titulo("Runas", FE)
                        .append(t("Tufo entalhado + 4 ametistas + bloco de lápis (Q): "))
                        .append(d("Mesa Rúnica"))
                        .append(t(".\n\nUma runa por peça: pulo duplo, explosão, gelo, raio, fusão...")),
                // 56
                titulo("Mitrilo", EXPLORACAO)
                        .append(t("Minério raro no fundo da ardósia (abaixo de -24). Só com picareta de "))
                        .append(d("diamante ou netherite"))
                        .append(t(".\n\nFunda o bruto: o lingote aprimora netherite na mesa de ferraria (com o molde).")),
                // 57
                titulo("Mapas do Tesouro", EXPLORACAO)
                        .append(t("Caem de Elites, pesca, masmorras e ruínas.\n\nSiga o "))
                        .append(d("X"))
                        .append(t(" no centro do mapa. Perto dele, o baú aparece enterrado... com guardiões!")),
                // 58
                titulo("Arqueologia", EXPLORACAO)
                        .append(t("Pedras antigas em volta de areia ou cascalho: é um "))
                        .append(d("sítio"))
                        .append(t(".\n\nUse o pincel nos blocos suspeitos: relíquias, mapas e achados.")),
                // 59
                titulo("Enciclopédia", EXPLORACAO)
                        .append(t("Biomas, estruturas, itens e relíquias que você descobriu.\n\nCada bioma novo: "))
                        .append(d("+0,2% de XP"))
                        .append(t(" em tudo.\n"))
                        .append(d("/enciclopedia")),
                // 60
                titulo("O Cronista", EXPLORACAO)
                        .append(t("Um velho bibliotecário guarda a história do mundo. Clique nele: "))
                        .append(d("8 capítulos"))
                        .append(t(", cada um com uma tarefa e uma recompensa.\n"))
                        .append(d("/cronista")),
                // 61
                titulo("Talentos", PROGRESSAO)
                        .append(t("1 ponto a cada "))
                        .append(d("25 níveis"))
                        .append(t(" somados. 4 árvores: Guerreiro, Arcano, Artesão e Explorador. Faixas novas com 5, 10 e 15 pontos.\n"))
                        .append(d("/talentos")),
                // 62
                titulo("Renascer", PROGRESSAO)
                        .append(t("Tudo no nível máximo? Os atributos voltam a 0, mas você ganha "))
                        .append(d("XP, vida, dano, mana, pontos e uma ★"))
                        .append(t(" para sempre. Até 5 vezes.\n"))
                        .append(d("/renascer")),
                // 63
                titulo("Companheiros", DOMADOR)
                        .append(t("Eles sobem de nível lutando (e montarias, sendo montadas). No "))
                        .append(d("10 e no 25"))
                        .append(t(" evoluem: maiores, mais fortes, Golpe Feroz e, no fim, Vínculo Ancestral.")),
                // 64
                titulo("Forja: e mais", FORJA)
                        .append(t("Barco, carrinho, vara, élitro e armadura de cavalo também saem "))
                        .append(d("forjados"))
                        .append(t(".\n\nItem achado + 1 Fragmento jogados na Forja: "))
                        .append(d("Reforjar")),
                // 65
                titulo("Estações", DETALHES)
                        .append(d("Boneco"))
                        .append(t(": feno + abóbora + suporte.\n"))
                        .append(d("Amolar"))
                        .append(t(": rebolo ou pedra lisa + 2 pederneiras + ferro.\n"))
                        .append(d("Fogueira"))
                        .append(t(": lã + tronco.\n"))
                        .append(d("Bebedouro"))
                        .append(t(": caldeirão + feno.")),
                // 66
                titulo("Detalhes", DETALHES)
                        .append(t("Túmulo ao morrer, ninhos nas folhas, animais raros, achados na areia, orvalho, "))
                        .append(d("sentar"))
                        .append(t(" em escadas.\n"))
                        .append(d("/diario /recordes")),
                // 67
                titulo("Ofícios", DETALHES)
                        .append(d("Barril"))
                        .append(t(": 2 ferros + favo. Bebidas melhoram com os dias.\n"))
                        .append(d("Colmeia"))
                        .append(t(": favo + flor. Méis especiais.\n"))
                        .append(d("Canteiro"))
                        .append(t(": musgo + farinha de osso + vaso.")),
                // 68
                titulo("Pomar", DETALHES)
                        .append(t("Mudas de Laranjeira, Pessegueiro, Cerejeira, Limoeiro e Macieira Dourada. Na estação certa, as frutas "))
                        .append(d("penduram nas folhas"))
                        .append(t(": clique para colher.")),
                // 68b
                titulo("Estruturas", EXPLORACAO)
                        .append(t("16 tipos surgem em terras novas: torres, minas, fortins, oásis, faróis, vilas saqueadas...\n"))
                        .append(d("Poço"))
                        .append(t(": 1 esmeralda.\n"))
                        .append(d("Farol"))
                        .append(t(": acenda com pedra luminosa.\n"))
                        .append(d("Círculo"))
                        .append(t(": lua cheia!\n"))
                        .append(d("Moradores"))
                        .append(t(": trocas e boatos.")),
                // 69
                titulo("Aventura", DETALHES)
                        .append(d("/album"))
                        .append(t(": cartas de monstros e peixes.\nMercador Itinerante, encontros na estrada e "))
                        .append(d("/segredos"))
                        .append(t(" escondidos.")),
                // 70
                titulo("Flechas", DETALHES)
                        .append(t("Bancada: 4 flechas + pó de blaze, gelo, pérola, laço ou TNT.\n\n"))
                        .append(d("Frascos"))
                        .append(t(": garrafa + ervas, pólvora ou slime. Arremesse!")),
                // 71
                titulo("Pesca", PESCA)
                        .append(t("Pescar dá XP. Nível alto: isca mais rápida, peixes "))
                        .append(d("★ a ★★★"))
                        .append(t(", em dobro, "))
                        .append(d("Tesouros do Mar"))
                        .append(t(" e "))
                        .append(d("criaturas marinhas"))
                        .append(t(" que lutam de volta!")),
                // 72
                titulo("Peixes raros", PESCA)
                        .append(t("12 peixes que só mordem em certos biomas, horários ou climas (noite, chuva, tempestade...).\n\n"))
                        .append(t("São ingredientes da Alquimia.\n"))
                        .append(d("/peixes")).append(t(": o diário")),
                // 73
                titulo("Alquimia", ALQUIMIA)
                        .append(t("1. Coloque um "))
                        .append(d("suporte de poções"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 2 garrafas de vidro\n • 1 pó de blaze\n\n"))
                        .append(d("Clique")).append(t(": bancada\n"))
                        .append(d("Agachado")).append(t(": suporte")),
                // 74
                titulo("Elixires", ALQUIMIA)
                        .append(t("Poções mais fortes e longas, várias com 2 ou 3 efeitos.\n\n"))
                        .append(d("Componentes"))
                        .append(t(" (Pó Arcano, Sal Lunar...) são a base de tudo.\n"))
                        .append(d("/alquimia")),
                // 75
                titulo("Gemas", ALQUIMIA)
                        .append(t("Itens forjados têm "))
                        .append(d("engastes"))
                        .append(t(" (Raro 1, Único 2, Mítico 3). Cada gema dá um bônus diferente em armas, arcos, armaduras e ferramentas.\n"))
                        .append(t("3 gemas = 1 melhor.")),
                // 76
                titulo("Acessórios", ALQUIMIA)
                        .append(d("2 anéis, 1 amuleto,\n1 cinto e 2 bolsos"))
                        .append(t(" que não ocupam a armadura nem caem ao morrer.\n\n"))
                        .append(t("Lanterna de Bolso, Ímã, Amuleto da Fênix...\n"))
                        .append(d("/acessorios")),
                // 77
                titulo("Guarda-roupa", VISUAL)
                        .append(t("Mude a "))
                        .append(d("aparência"))
                        .append(t(" da armadura sem tirá-la, ou esconda uma peça.\n\nNa cabeça vale qualquer item!\n"))
                        .append(d("/guardaroupa")),
                // 78
                titulo("Conquistas", TITULO)
                        .append(d("/titulos"))
                        .append(t(": títulos que dão bônus. Só o que está em uso conta.\n\n"))
                        .append(d("Agache + clique num aldeão"))
                        .append(t(": pedidos do dia. Cumprir dá esmeraldas e descontos.")),
                // 79
                titulo("Comandos", TITULO)
                        .append(d("/atributos /classe\n/forja /grimorio\n/receitas /pets\n/peixes /alquimia\n/colonia /reino\n/titulos /bestiario\n/party /territorio\n/masmorra /lendas\n/locais /combos\n/portais /torre\n/mochila /deus /enc\n/cronista /talentos\n/rpg /album /placar"))
        );
    }
}
