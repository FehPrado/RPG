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
                        .append(t("2  Atributos\n3  Forja\n7  Infusor/Magias\n13 Altar\n16 Fazenda/Cozinha\n"
                                + "21 Reciclagem\n22 Party/Território\n25 Domador\n27 Masmorras\n29 Classes/Lendas\n32 Viagem/Ocultos\n34 Conquistas")),
                // 2
                titulo("Atributos", TITULO)
                        .append(t("Sobem fazendo as coisas (até o nível " + nivelMaximo + "):\n\n"))
                        .append(t("⛏ minerar ♣ cortar\n» correr ⬆ pular\n⚔ lutar ⚒ forjar\n✦ magia ≈ nadar\n☘ colher ♨ cozinhar\n♞ domar\n"))
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
                titulo("Altar: criar", ALTAR)
                        .append(t("1. Coloque uma "))
                        .append(d("obsidiana chorosa"))
                        .append(t(".\n2. Jogue em cima (tecla Q):\n"))
                        .append(d(" • 1 olho do ender\n • 4 velas\n\n"))
                        .append(t("Clique nele para ver o que ele invoca.")),
                // 14
                titulo("Altar: invocar", ALTAR)
                        .append(t("Jogue a oferenda em cima do altar ou use o menu dele.\n\n"))
                        .append(d("Chefes, Lua de Sangue, Meteoros, Ondas de Monstros, Mercador Arcano"))
                        .append(t(" e "))
                        .append(d("Caçador de Recompensas")).append(t(".")),
                // 15
                titulo("Chefes", ALTAR)
                        .append(t("Golem, Rainha Aracnídea, Senhor das Chamas, Lich, Tempestade Viva e Arauto do Fim.\n\n"))
                        .append(t("Deixam "))
                        .append(d("Fragmentos de Forja"))
                        .append(t(", Pedras de Proteção e "))
                        .append(d("núcleos"))
                        .append(t(": infunda para um poder lendário!")),
                // 16
                titulo("Plantações", FAZENDA)
                        .append(t("Colher plantação madura dá XP de Agricultura.\n\n"))
                        .append(t("Nível alto: colheita em dobro, qualidade "))
                        .append(d("★ a ★★★"))
                        .append(t(", plantas crescendo perto de você e às vezes uma colheita "))
                        .append(d("GIGANTE")).append(t("!")),
                // 17
                titulo("Adubo", FAZENDA)
                        .append(d("Adubo Rico"))
                        .append(t(": 3 farinhas de osso + carne podre + terra. Clique na plantação: colheita melhor.\n\n"))
                        .append(d("Espantalho"))
                        .append(t(": suporte com abóbora na cabeça. Ninguém pisoteia perto.")),
                // 18
                titulo("Sementes raras", FAZENDA)
                        .append(t("Plantas diferentes maduras lado a lado:\n"))
                        .append(d("Trigo+Cenoura\n")).append(t("→ Trigo Dourado\n"))
                        .append(d("Cenoura+Batata\n")).append(t("→Cenoura Cristalina\n"))
                        .append(d("Batata+Beterraba\n")).append(t("→ Batata Ancestral\n"))
                        .append(d("Beterraba+Trigo\n")).append(t("→ Beterraba Rubi")),
                // 19
                titulo("Cozinha: criar", COZINHA)
                        .append(t("1. Coloque um "))
                        .append(d("defumador"))
                        .append(t(".\n2. Jogue em cima (tecla Q):\n"))
                        .append(d(" • 1 caldeirão\n • 1 balde de água\n\n"))
                        .append(d("Clique")).append(t(": receitas\n"))
                        .append(d("Agachado")).append(t(": defumador")),
                // 20
                titulo("Pratos", COZINHA)
                        .append(t("Comer um prato dá buffs. Culinária alta libera receitas e buffs mais longos.\n\n"))
                        .append(t("Ingredientes "))
                        .append(d("★★"))
                        .append(t(" melhoram o prato. Sementes raras entram nos melhores!\n\n"))
                        .append(d("/receitas")),
                // 21
                titulo("Reciclagem", FORJA)
                        .append(t("1. Coloque um "))
                        .append(d("rebolo"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 funil\n • 1 tesoura\n\n"))
                        .append(t("Desmonta itens e devolve parte do material. Encantos podem virar livro!")),
                // 22
                titulo("Party", PARTY)
                        .append(t("Jogue em grupo:\n"))
                        .append(d("/party convidar <nick>\n"))
                        .append(t("• sem fogo amigo\n• XP de Combate dividido com quem está perto\n• chat: "))
                        .append(d("/pc\n"))
                        .append(t("• aviso de vida baixa")),
                // 23
                titulo("Território", TERRITORIO)
                        .append(t("1. Coloque uma "))
                        .append(d("magnetita"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 estandarte\n • 4 esmeraldas\n\n"))
                        .append(t("Vira o Marco: protege 3x3 chunks. Nível alto = mais chunks.")),
                // 24
                titulo("Proteção", TERRITORIO)
                        .append(t("Só você e seus membros constroem, abrem baús e mexem em animais.\n\n"))
                        .append(d("/territorio"))
                        .append(t(": mapa, membros e regras (PvP, portas, baús, explosões...).")),
                // 25
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
                // 26
                titulo("Companheiros", DOMADOR)
                        .append(t("Mão vazia + clique: ordens (seguir, ficar, guardar, patrulhar). Nunca teleporta.\n"))
                        .append(t("No altar: componentes (sela, baú, asas...).\n"))
                        .append(d("Sela + élitro = voa!\n"))
                        .append(d("/pets")),
                // 27
                titulo("Masmorras", MASMORRA)
                        .append(t("1. Coloque "))
                        .append(d("tijolos de pedra entalhados"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 1 olho do ender\n • 1 bússola\n\n"))
                        .append(t("Clique, escolha a dificuldade e suba no portal com a party.")),
                // 28
                titulo("Lá dentro", MASMORRA)
                        .append(t("Salas trancam e soltam ondas de monstros. Tesouros e um chefe no fim.\n\n"))
                        .append(t("Morreu? Itens no "))
                        .append(d("Cofre das Almas"))
                        .append(t(": 2 diamantes no Portal.\n"))
                        .append(d("/masmorra sair")),
                // 29
                titulo("Classes", CLASSE)
                        .append(t("Escolha um caminho no "))
                        .append(d("Santuário"))
                        .append(t(" (púlpito + livro e pena + diamante) ou no "))
                        .append(d("/classe"))
                        .append(t(".\nFaça as tarefas e passe na "))
                        .append(d("Prova"))
                        .append(t(".\n\nGuerreiro, Arqueiro, Mago, Ladino, Ferreiro e Domador.")),
                // 30
                titulo("Habilidades", CLASSE)
                        .append(d("Agache + F"))
                        .append(t(": usa\n"))
                        .append(d("Agache + clique esq."))
                        .append(t(": troca\n\n"))
                        .append(t("Classe no nível 20 = "))
                        .append(d("Dominada"))
                        .append(t(": libera as avançadas (algumas pedem duas!).")),
                // 31
                titulo("Lendas", LENDA)
                        .append(t("Itens "))
                        .append(d("Especiais"))
                        .append(t(" e únicos que nascem dos seus feitos. Os feitos são segredo: decifre o enigma ou ache "))
                        .append(d("Páginas do Livro das Lendas"))
                        .append(t(".\n"))
                        .append(d("/lendas")),
                // 32
                titulo("Pedras de Viagem", VIAGEM)
                        .append(t("1. Coloque uma "))
                        .append(d("pedra entalhada de ardósia"))
                        .append(t(".\n2. Jogue em cima:\n"))
                        .append(d(" • 2 pérolas do ender\n • 4 ametistas\n\n"))
                        .append(t("Clique nas pedras que achar e viaje entre elas pagando XP.")),
                // 33
                titulo("Locais Ocultos", OCULTO)
                        .append(t("Ruínas escondidas. Siga "))
                        .append(d("Mapas Rasgados"))
                        .append(t(" e os sussurros.\nResolva o enigma, vença o guardião e toque o "))
                        .append(d("selo"))
                        .append(t(": relíquias e uma "))
                        .append(d("classe lendária"))
                        .append(t("!")),
                // 34
                titulo("Conquistas", TITULO)
                        .append(d("/titulos"))
                        .append(t(": títulos que dão bônus. Só o que está em uso conta.\n\n"))
                        .append(d("Agache + clique num aldeão"))
                        .append(t(": pedidos do dia. Cumprir dá esmeraldas e descontos.")),
                // 35
                titulo("Comandos", TITULO)
                        .append(d("/atributos /classe\n/forja /grimorio\n/receitas /pets\n/titulos /missoes\n/party /territorio\n/masmorra /lendas\n/locais /cosmeticos\n/chapeu /tag\n/guia"))
                        .append(t("\n\nBoa aventura!"))
        );
    }
}
