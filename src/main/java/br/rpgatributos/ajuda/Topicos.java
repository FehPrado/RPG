package br.rpgatributos.ajuda;

import org.bukkit.Material;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static br.rpgatributos.ajuda.Categoria.COLECOES;
import static br.rpgatributos.ajuda.Categoria.COMBATE;
import static br.rpgatributos.ajuda.Categoria.COMUNIDADE;
import static br.rpgatributos.ajuda.Categoria.MAGIA;
import static br.rpgatributos.ajuda.Categoria.MUNDO;
import static br.rpgatributos.ajuda.Categoria.OFICIOS;
import static br.rpgatributos.ajuda.Categoria.PERSONAGEM;

/**
 * Os assuntos do Guia interativo: curtos, com o ritual desenhado em itens. A ordem aqui é a
 * ordem em que aparecem em cada categoria (o mais básico primeiro).
 */
public final class Topicos {

    private static final List<Topico> TODOS = new ArrayList<>();

    private Topicos() { }

    private static final class B {
        final String id, nome;
        final Categoria cat;
        final Material icone;
        List<String> texto = List.of();
        Material bloco;
        String nomeBloco;
        List<Object[]> itens = List.of();
        String dica, comando, palavras = "";
        List<String> ligados = List.of();

        B(String id, Categoria cat, String nome, Material icone) {
            this.id = id;
            this.cat = cat;
            this.nome = nome;
            this.icone = icone;
        }

        B texto(String... t) { texto = List.of(t); return this; }

        /** Bloco e itens jogados em cima, como pares (material, quantidade). */
        B ritual(Material bloco, String nomeBloco, Object... pares) {
            this.bloco = bloco;
            this.nomeBloco = nomeBloco;
            List<Object[]> l = new ArrayList<>();
            for (int i = 0; i + 1 < pares.length; i += 2) l.add(new Object[]{pares[i], pares[i + 1]});
            itens = l;
            return this;
        }

        B dica(String d) { dica = d; return this; }
        B cmd(String c) { comando = c; return this; }
        B ligados(String... l) { ligados = List.of(l); return this; }
        B palavras(String p) { palavras = p; return this; }

        void pronto() {
            TODOS.add(new Topico(id, cat, nome, icone, texto, bloco, nomeBloco, itens, dica, comando, ligados, palavras));
        }
    }

    private static B t(String id, Categoria c, String nome, Material icone) {
        return new B(id, c, nome, icone);
    }

    static {
        // ===================== Personagem =====================
        t("atributos", PERSONAGEM, "Atributos", Material.PLAYER_HEAD)
                .texto("São 13 atributos que sobem fazendo as coisas: minerar, cortar madeira, correr, lutar, forjar, pescar...",
                        "Cada nível dá um bônus. No nível 100 o bônus é o máximo.")
                .dica("Faça um pouco de tudo no começo: vários sistemas pedem nível somado.")
                .cmd("atributos").ligados("talentos", "classes", "titulos").palavras("nivel xp mineracao combate corrida pulo").pronto();
        t("talentos", PERSONAGEM, "Talentos", Material.EXPERIENCE_BOTTLE)
                .texto("A cada 25 níveis somados você ganha 1 ponto de talento.",
                        "São 4 árvores (Guerreiro, Arcano, Artesão e Andarilho) com talentos que liberam em degraus.")
                .dica("Refazer os pontos custa esmeraldas: pense antes no final da árvore.")
                .cmd("talentos").ligados("renascer", "atributos").palavras("pontos arvore").pronto();
        t("renascer", PERSONAGEM, "Renascer", Material.TOTEM_OF_UNDYING)
                .texto("Com os 13 atributos no 100, você pode renascer: os atributos voltam a 0, mas você ganha bônus para sempre.",
                        "Cada vez: +XP, +1 ❤, +dano, +mana e pontos de talento.")
                .dica("Itens, classes, lendas e títulos continuam com você.")
                .cmd("renascer").ligados("talentos").palavras("renascimento prestigio").pronto();
        t("classes", PERSONAGEM, "Classes", Material.LECTERN)
                .texto("Escolha um caminho, faça as tarefas dele e passe na Prova para ganhar a classe.",
                        "Cada classe tem passivo e habilidades. Classes dominadas liberam as avançadas.")
                .ritual(Material.LECTERN, "Púlpito", Material.WRITABLE_BOOK, 1, Material.DIAMOND, 1)
                .dica("Habilidade: agache + F. Trocar a habilidade: agache + clique esquerdo no ar.")
                .cmd("classe").ligados("atributos", "combos", "locais").palavras("santuario prova habilidade guerreiro mago arqueiro").pronto();
        t("titulos", PERSONAGEM, "Títulos", Material.NAME_TAG)
                .texto("Conquistas que você libera jogando. O título em uso dá um bônus e aparece acima da sua cabeça.")
                .dica("Os títulos mais difíceis são anunciados para o servidor inteiro.")
                .cmd("titulos").ligados("segredos", "recordes").palavras("conquistas bonus tag").pronto();
        t("acessorios", PERSONAGEM, "Acessórios", Material.GOLD_NUGGET)
                .texto("2 anéis, 1 amuleto, 1 cinto e 2 bolsos. Não ocupam a armadura e não caem quando você morre.",
                        "São feitos na Bancada Alquímica, na aba Acessórios.")
                .dica("Não dá para usar dois acessórios iguais.")
                .cmd("acessorios").ligados("alquimia", "gemas").palavras("anel amuleto cinto bolso lanterna ima capa planadora").pronto();
        t("mochila", PERSONAGEM, "Mochila", Material.CHEST)
                .texto("Um baú só seu que vai para todo lugar. Ganha uma linha a mais a cada 150 níveis somados.")
                .cmd("mochila").ligados("atributos").palavras("bau inventario").pronto();
        t("guardaroupa", PERSONAGEM, "Guarda-roupa e cosméticos", Material.ARMOR_STAND)
                .texto("Escolha a aparência de cada parte da armadura sem perder a proteção. Na cabeça vale qualquer item.")
                .cmd("guardaroupa").ligados("acessorios").palavras("aparencia visual cosmetico chapeu tag").pronto();

        // ===================== Combate =====================
        t("combos", COMBATE, "Combos de arma", Material.IRON_SWORD)
                .texto("Com a arma na mão, 3 cliques seguidos soltam um golpe (D = direito, E = esquerdo).",
                        "Espada, machado, lança, tridente e maça começam com D; arco e besta com E.",
                        "Cada arma tem proficiência de 1 a 20, que libera golpes novos.")
                .dica("Escolha quais golpes ficam em cada sequência no /combos. Agachado não solta combo.")
                .cmd("combos").ligados("posturas", "mobilidade", "boneco").palavras("golpe vigor proficiencia ataque").pronto();
        t("posturas", COMBATE, "Posturas, estilo e equilíbrio", Material.SHIELD)
                .texto("Postura (/postura): Ofensiva (+dano), Defensiva (leva menos dano e cura nos golpes), Ágil (velocidade e recarga) ou a Mestra da arma (proficiência 20).",
                        "Variar os golpes sobe o estilo (D até S): mais dano e XP. Apanhar ou repetir o mesmo golpe derruba.",
                        "Combos enchem o equilíbrio dos inimigos fortes; cheio, eles ficam parados e levam +50% de dano.")
                .dica("Depois do 2º clique, um \"tim\" marca o tempo certo do 3º: golpe perfeito, mais forte e mais barato. Acertar quem está no ar dá +30%.")
                .cmd("postura").ligados("combos", "ligacoes", "boneco", "mobilidade").palavras("postura estilo rank equilibrio perfeito aereo ofensiva defensiva agil mestra").pronto();
        t("ligacoes", COMBATE, "Combos com magia, party e feras", Material.IRON_CHAIN)
                .texto("Óleo de lâmina ou runa na arma: os golpes de combo levam o elemento e fazem reações com as magias (as suas ou as da party).",
                        "Ataque conjunto: dois da mesma party acertam combos no mesmo inimigo em até 3 s. Dano extra, muito equilíbrio e estilo.",
                        "No finalizador, seus companheiros partem para cima do alvo.",
                        "Sua classe e seu deus combinam com uma postura: veja no /postura.")
                .dica("Guerreiro de Óleo Gélido + mago de fogo no mesmo alvo = Derreter. Óleo de Fogo + Óleo Venenoso de um amigo = Queimada.")
                .cmd("postura").ligados("oleos", "posturas", "magias", "party", "companheiros").palavras("ligacao elemento reacao conjunto party companheiro afinidade classe deus").pronto();
        t("mobilidade", COMBATE, "Esquiva, gancho e planador", Material.FEATHER)
                .texto("Esquiva: dois toques rápidos em A, D ou S.",
                        "Gancho de Escalada: vara de pesca + gancho de armadilha + 3 ferros na bancada.",
                        "Capa Planadora: acessório; no ar, aperte pular para planar.")
                .ligados("combos", "acessorios").palavras("esquiva dash gancho escalada capa planar").pronto();
        t("boneco", COMBATE, "Boneco de Treino", Material.HAY_BLOCK)
                .texto("Bata nele (com arma, combo, habilidade ou magia) para ver o dano de cada golpe e o dano por segundo.",
                        "Dá um pouco de XP de Combate, com limite por hora.")
                .ritual(Material.HAY_BLOCK, "Fardo de feno", Material.CARVED_PUMPKIN, 1, Material.ARMOR_STAND, 1)
                .ligados("combos", "amolar").palavras("treino dps dano").pronto();
        t("amolar", COMBATE, "Pedra de Amolar", Material.GRINDSTONE)
                .texto("Clique com a arma: ela fica Afiada (+10% de dano) por 100 golpes. Gasta 1 pederneira.")
                .ritual(Material.GRINDSTONE, "Rebolo (ou pedra lisa)", Material.FLINT, 2, Material.IRON_INGOT, 1)
                .ligados("boneco", "forja").palavras("afiar afiada").pronto();
        t("lendas", COMBATE, "Lendas", Material.NETHER_STAR)
                .texto("Itens únicos no servidor, com poder próprio e golpe de combo só deles.",
                        "Cumpra os feitos da lenda e forje perto de uma Forja do Ferreiro (Ferraria 50).")
                .dica("Os feitos aparecem como enigma; a Página do Livro das Lendas revela.")
                .cmd("lendas").ligados("forja", "locais").palavras("especial unico feito").pronto();
        t("altar", COMBATE, "Altar e chefes", Material.CRYING_OBSIDIAN)
                .texto("Invoque chefes jogando a oferenda em cima do altar. Os chefes deixam núcleos e Fragmentos de Forja.")
                .ritual(Material.CRYING_OBSIDIAN, "Obsidiana chorona", Material.ENDER_EYE, 1, Material.CANDLE, 4)
                .dica("Clique no altar para ver cada chefe e a oferenda dele.")
                .ligados("masmorras", "trofeus").palavras("chefe boss invocar golem rainha oferenda").pronto();
        t("masmorras", COMBATE, "Masmorras", Material.CHISELED_STONE_BRICKS)
                .texto("Entre com a sua party numa masmorra gerada na hora: salas com ondas, armadilhas e um chefe no fim.",
                        "Quatro dificuldades; o tesouro cresce com ela.")
                .ritual(Material.CHISELED_STONE_BRICKS, "Tijolos de pedra entalhados", Material.ENDER_EYE, 1, Material.COMPASS, 1)
                .cmd("masmorra").ligados("party", "torre", "portais").palavras("dungeon portal cofre").pronto();
        t("torre", COMBATE, "Torre Infinita", Material.CRYING_OBSIDIAN)
                .texto("Andares cada vez mais difíceis, com chefe e baú a cada 10. Ranking semanal e de todos os tempos.",
                        "Morrer na Torre não perde itens.")
                .ritual(Material.CRYING_OBSIDIAN, "Obsidiana chorona", Material.ENDER_EYE, 1, Material.GOLD_BLOCK, 1)
                .cmd("torre ranking").ligados("masmorras", "party").palavras("obelisco andar ranking").pronto();
        t("portais", COMBATE, "Portais pelo mundo", Material.END_PORTAL_FRAME)
                .texto("Portais aparecem perto de quem explora. Entre e vença a masmorra antes que ele transborde e monstros saiam.")
                .cmd("portais").ligados("masmorras", "perigo").palavras("gate transbordar soberano sombras").pronto();
        t("perigo", COMBATE, "Mundo perigoso", Material.ZOMBIE_HEAD)
                .texto("O mundo envelhece e os monstros ficam mais fortes. Elites com poderes, ninhos, hordas à noite e chefes mundiais.",
                        "Na lua de sangue, tudo fica pior.")
                .cmd("bestiario").ligados("estacoes", "bestiario").palavras("elite horda ninho chefe mundial lua sangue").pronto();

        // ===================== Magia e fé =====================
        t("infusor", MAGIA, "Infusor e grimório", Material.CAULDRON)
                .texto("Infunda itens no corpo (mente, coração, braços...) para ganhar mana e bônus.",
                        "No infusor você pega o grimório, onde ficam as suas magias.")
                .ritual(Material.WATER_CAULDRON, "Caldeirão com água", Material.ENCHANTING_TABLE, 1, Material.ANVIL, 1)
                .cmd("grimorio").ligados("magias", "cajado").palavras("infundir mana arcano grimorio").pronto();
        t("magias", MAGIA, "Criar magias", Material.ENCHANTED_BOOK)
                .texto("No grimório, combine essências (fogo, gelo, raio...) com uma forma (projétil, aura, raio...).",
                        "Algumas combinações exatas são magias secretas.")
                .dica("Dois elementos no mesmo alvo causam uma reação.")
                .cmd("grimorio").ligados("infusor", "cajado", "runas").palavras("essencia forma reacao maestria secreta").pronto();
        t("cajado", MAGIA, "Cajado Arcano", Material.BREEZE_ROD)
                .texto("Forjado na Forja do Ferreiro com uma vara de breeze. Lança as magias como o grimório, com bônus mágicos.",
                        "Gemas no cajado aumentam a força dos elementos.")
                .ligados("forja", "magias", "gemas").palavras("cajado mago").pronto();
        t("runas", MAGIA, "Runas", Material.CHISELED_TUFF)
                .texto("Grave uma runa num item: chama, gelo, salto duplo, vampirismo e outras.")
                .ritual(Material.CHISELED_TUFF, "Tufo entalhado", Material.AMETHYST_SHARD, 4, Material.LAPIS_BLOCK, 1)
                .ligados("magias", "transmutacao").palavras("gravar mesa runica").pronto();
        t("transmutacao", MAGIA, "Transmutação", Material.AMETHYST_BLOCK)
                .texto("Transforme materiais em outros mais raros. A Pedra Filosofal melhora tudo.")
                .ritual(Material.AMETHYST_BLOCK, "Bloco de ametista", br.rpgatributos.alquimia.Reagente.MERCURIO_VIVO.criar(1), 1, Material.GOLD_INGOT, 1)
                .dica("O Mercúrio Vivo é feito na Bancada Alquímica.")
                .ligados("alquimia", "runas").palavras("circulo pedra filosofal mercurio").pronto();
        t("deuses", MAGIA, "Deuses e fé", Material.CHISELED_QUARTZ_BLOCK)
                .texto("Siga um dos 6 deuses. Oferendas e preces dão devoção; a fé sobe de 1 a 5 e libera um milagre.")
                .ritual(Material.CHISELED_QUARTZ_BLOCK, "Quartzo entalhado", Material.GOLDEN_APPLE, 1, Material.CANDLE, 1)
                .dica("Santuários Esquecidos pelo mundo dão devoção dobrada.")
                .cmd("deus").ligados("astronomia", "estruturas").palavras("ferrum sylva maris bellum arcanus mortis milagre devocao santuario").pronto();
        t("astronomia", MAGIA, "Astronomia", Material.SPYGLASS)
                .texto("À noite, olhe para o céu com a luneta por 5 segundos: você observa uma constelação e ganha a bênção dela até o amanhecer.")
                .cmd("estrelas").ligados("deuses", "estacoes").palavras("estrelas constelacao luneta").pronto();

        // ===================== Ofícios =====================
        t("forja", OFICIOS, "Forja do Ferreiro", Material.ANVIL)
                .texto("Armas, ferramentas e armaduras feitas nela saem forjadas: de Comum a Mítico, com bônus e efeitos.",
                        "Agachado + clique: refino.")
                .ritual(Material.ANVIL, "Bigorna", Material.IRON_BLOCK, 2, Material.LAVA_BUCKET, 1)
                .dica("Ferraria alta deixa os itens mais raros. O balde volta vazio.")
                .cmd("forja").ligados("reciclagem", "gemas", "lendas").palavras("forjar raridade refino ferreiro mitico lendario").pronto();
        t("reciclagem", OFICIOS, "Reciclagem (desmanchar)", Material.GRINDSTONE)
                .texto("Desmanche armas, ferramentas e armaduras e recupere de 50% a 75% do material.",
                        "Itens encantados podem devolver um livro; gemas voltam inteiras.")
                .ritual(Material.GRINDSTONE, "Rebolo", Material.HOPPER, 1, Material.SHEARS, 1)
                .ligados("forja", "amolar").palavras("desmanchar reciclar desmontar").pronto();
        t("alquimia", OFICIOS, "Bancada Alquímica", Material.BREWING_STAND)
                .texto("Elixires, componentes, gemas e acessórios. Clique para abrir; agachado + clique é o suporte normal.")
                .ritual(Material.BREWING_STAND, "Suporte de poções", Material.GLASS_BOTTLE, 2, Material.BLAZE_POWDER, 1)
                .dica("/alquimia mostra todas as receitas em qualquer lugar.")
                .cmd("alquimia").ligados("oleos", "gemas", "acessorios", "pesca").palavras("elixir componente pocao bancada").pronto();
        t("oleos", OFICIOS, "Óleos de lâmina", Material.HONEY_BOTTLE)
                .texto("Feitos na Bancada Alquímica (aba Componentes) com Óleo de Peixe.",
                        "Segure o óleo e clique com o direito: ele vai na arma da outra mão (ou na 1ª arma da barra) e dura 60 golpes.",
                        "Fogo incendeia · Gélido deixa lento · Trovejante solta faíscas · Venenoso envenena · Prata +40% em mortos-vivos.")
                .dica("Cada 2 níveis de Alquimia dá +1 golpe. O óleo também dá elemento aos combos.")
                .cmd("alquimia").ligados("alquimia", "ligacoes", "combos").palavras("oleo lamina veneno fogo gelo prata trovao arma").pronto();
        t("gemas", OFICIOS, "Gemas", Material.EMERALD)
                .texto("6 gemas em 3 graus. Engaste em itens forjados (aba Engastar da Bancada Alquímica).",
                        "Cada gema dá um bônus diferente em arma, arco, armadura ou ferramenta.")
                .ligados("alquimia", "forja").palavras("rubi safira topazio ametista onix engastar").pronto();
        t("cozinha", OFICIOS, "Cozinha", Material.SMOKER)
                .texto("Pratos com buffs fortes. Ingredientes de qualidade ★ deixam o prato melhor.")
                .ritual(Material.SMOKER, "Defumador", Material.CAULDRON, 1, Material.WATER_BUCKET, 1)
                .cmd("receitas").ligados("fazenda", "pesca").palavras("prato receita comida culinaria").pronto();
        t("fazenda", OFICIOS, "Plantações e sementes raras", Material.WHEAT)
                .texto("Colheita com qualidade ★ a ★★★, colheita dupla e gigante.",
                        "Plantas maduras lado a lado às vezes geram uma semente rara.")
                .dica("Espantalho: suporte de armadura com abóbora esculpida na cabeça.")
                .ligados("pomar", "cozinha").palavras("agricultura adubo espantalho variedade semente").pronto();
        t("pomar", OFICIOS, "Pomar", Material.APPLE)
                .texto("Mudas especiais viram árvores de laranja, pêssego, cereja, limão ou maçã dourada.",
                        "Na estação certa as frutas penduram nas folhas: clique para colher.")
                .ligados("fazenda", "estacoes", "barril").palavras("fruta muda arvore laranja cereja limao").pronto();
        t("pesca", OFICIOS, "Pesca", Material.FISHING_ROD)
                .texto("Peixes com qualidade, 12 peixes raros por bioma e clima, Tesouros do Mar e criaturas que lutam de volta.")
                .cmd("peixes").ligados("pescagelo", "alquimia", "cozinha").palavras("peixe raro tesouro mar vara").pronto();
        t("pescagelo", OFICIOS, "Pesca no gelo", Material.PACKED_ICE)
                .texto("Quebre o gelo de um lago ou mar gelado (ou de qualquer lago no inverno) e pesque no buraco: +5% de peixe raro.",
                        "5 peixes só do gelo: Truta-do-Gelo, Lúcio Polar, Peixe-Lanterna, Enguia de Cristal e o raríssimo Esturjão Ancestral.",
                        "Cabana de Pesca: a até 12 blocos, isca 30% mais rápida, +4% de raro e ninguém sente frio.")
                .ritual(Material.CAMPFIRE, "Fogueira", Material.FISHING_ROD, 1, Material.COD, 1, Material.SPRUCE_PLANKS, 4)
                .dica("O Esturjão Ancestral só morde de noite, no inverno. Com ele sai o Caviar Ancestral.")
                .cmd("peixes").ligados("pesca", "cozinha", "alquimia").palavras("gelo buraco inverno cabana neve truta lucio esturjao caviar").pronto();
        t("barril", OFICIOS, "Barril de Envelhecimento", Material.BARREL)
                .texto("Bebidas que melhoram com os dias reais: Nova, Envelhecida, Reserva e Lendária.")
                .ritual(Material.BARREL, "Barril vazio", Material.IRON_INGOT, 2, Material.HONEYCOMB, 1)
                .ligados("colmeia", "pomar").palavras("bebida vinho cerveja licor").pronto();
        t("colmeia", OFICIOS, "Colmeia do Apicultor", Material.BEEHIVE)
                .texto("Com flores perto, faz mel especial: Dourado, Gelado, Floral, Noturno ou Silvestre. Tire com garrafas.")
                .ritual(Material.BEEHIVE, "Colmeia", Material.HONEYCOMB, 1, Material.POPPY, 1)
                .ligados("barril", "canteiro").palavras("mel abelha").pronto();
        t("canteiro", OFICIOS, "Canteiro de Ervas", Material.MOSS_BLOCK)
                .texto("Uma erva a cada 10 minutos, que depende do lugar (calor, frio, pântano, subsolo...).",
                        "As ervas entram nos frascos e no Licor de Ervas.")
                .ritual(Material.MOSS_BLOCK, "Bloco de musgo", Material.BONE_MEAL, 1, Material.FLOWER_POT, 1)
                .ligados("flechas", "colmeia").palavras("erva salvia").pronto();
        t("flechas", OFICIOS, "Flechas e frascos", Material.TIPPED_ARROW)
                .texto("Na bancada: 4 flechas + pó de blaze, gelo, pérola ou TNT. Frascos de arremesso com ervas, pólvora ou slime.")
                .ligados("canteiro", "combos").palavras("flecha fogo gelo explosiva frasco fumaca").pronto();
        t("mitrilo", OFICIOS, "Mitrilo", Material.RAW_IRON)
                .texto("Minério raro no fundo do mundo (picareta de diamante ou netherita). Funda e use na mesa de ferraria para melhorar netherita.")
                .ligados("forja", "mapas").palavras("minerio raro lingote").pronto();

        // ===================== Mundo e aventura =====================
        t("estacoes", MUNDO, "Estações, clima e céu", Material.CLOCK)
                .texto("Cada estação dura uma semana real e muda plantas, pesca e monstros. Festival no 4º dia.",
                        "Lua cheia, lua de sangue, eclipse, aurora e chuva de meteoros.")
                .cmd("calendario").ligados("pomar", "perigo", "astronomia").palavras("estacao inverno verao clima lua festival").pronto();
        t("estruturas", MUNDO, "Estruturas", Material.MOSSY_STONE_BRICKS)
                .texto("16 tipos surgem em terras novas: torres, minas, fortins, oásis, faróis, vilas saqueadas, santuários... E no mar, naufrágios e cidades submersas.",
                        "Alguns têm guardas, outros têm moradores que negociam.")
                .dica("Poço dos Desejos: jogue 1 esmeralda. Farol: acenda com um bloco de pedra luminosa.")
                .ligados("mar", "enciclopedia", "mapas", "locais").palavras("torre mina cemiterio poco eremita bruxa farol fortim oasis circulo").pronto();
        t("mar", MUNDO, "Naufrágios e a cidade submersa", Material.PRISMARINE_BRICKS)
                .texto("Naufrágios no fundo do mar: cada navio tem nome, carga e o diário do capitão (às vezes ele aponta para uma cidade submersa).",
                        "Cidade Submersa: rara, no mar profundo. Templo de Maris, casas, arquivo com a história, cofre e guardas afogados.",
                        "No santuário do templo acorda o Sumo-Sacerdote. Ele guarda o Coração da cidade (um condutor).")
                .dica("Ponha o Coração bem no meio da moldura do altar: a cidade desperta. Leve poção de respiração.")
                .ligados("estruturas", "deuses", "pesca").palavras("naufragio navio cidade submersa mar oceano templo maris condutor coracao sacerdote").pronto();
        t("locais", MUNDO, "Locais Ocultos", Material.CHISELED_DEEPSLATE)
                .texto("14 ruínas escondidas com um enigma de alavancas e um guardião. O selo dá tesouros e libera classes lendárias.",
                        "O Mapa Rasgado vira uma bússola que aponta para perto deles.")
                .cmd("locais").ligados("classes", "lendas").palavras("selo enigma guardiao bussola mapa rasgado").pronto();
        t("mapas", MUNDO, "Mapas do Tesouro", Material.FILLED_MAP)
                .texto("Mapas com um X vermelho. Chegue perto do X com o mapa: o baú sai da terra e guardiões aparecem.")
                .ligados("arqueologia", "cartografo").palavras("tesouro x enterrado").pronto();
        t("cartografo", MUNDO, "Mesa do Cartógrafo", Material.CARTOGRAPHY_TABLE)
                .texto("Com um mapa do tesouro na mão, ela mostra a direção e a distância do X.")
                .ritual(Material.CARTOGRAPHY_TABLE, "Mesa de cartografia", Material.COMPASS, 1, Material.PAPER, 3)
                .ligados("mapas").palavras("direcao mapa").pronto();
        t("arqueologia", MUNDO, "Arqueologia", Material.BRUSH)
                .texto("Ache pedras antigas com areia ou cascalho suspeito em volta e use o pincel: saem relíquias com história.")
                .cmd("enciclopedia").ligados("enciclopedia", "mapas").palavras("reliquia pincel sitio").pronto();
        t("enciclopedia", MUNDO, "Enciclopédia", Material.BOOK)
                .texto("Biomas, estruturas, itens e relíquias que você já viu. Cada bioma novo dá +0,2% de XP em tudo.")
                .cmd("enciclopedia").ligados("arqueologia", "estruturas").palavras("bioma descoberta").pronto();
        t("cronista", MUNDO, "As Crônicas (campanha)", Material.LECTERN)
                .texto("O Cronista conta a história do mundo em 8 capítulos, cada um com um pedido e uma recompensa.")
                .cmd("cronista").ligados("enciclopedia").palavras("campanha historia capitulo").pronto();
        t("viagem", MUNDO, "Pedras de Viagem", Material.CHISELED_DEEPSLATE)
                .texto("Clique numa pedra para descobri-la; depois viaje entre as que você conhece. Custa XP pela distância.")
                .ritual(Material.CHISELED_DEEPSLATE, "Ardósia entalhada", Material.ENDER_PEARL, 2, Material.AMETHYST_SHARD, 4)
                .ligados("territorio").palavras("teleporte viajar").pronto();
        t("vida", MUNDO, "Mercador e encontros", Material.EMERALD)
                .texto("O Mercador Itinerante aparece por 20 minutos com ofertas raras. Na estrada: carroças, bandidos, viajantes feridos e estrelas cadentes.")
                .ligados("estruturas").palavras("mercador encontro bandidos viajante estrela").pronto();
        t("descanso", MUNDO, "Fogueira e Bebedouro", Material.CAMPFIRE)
                .texto("Fogueira de Acampamento: fogueira acesa + 1 lã + 1 tronco. Sentado perto, você fica Descansado (+5% XP).",
                        "Bebedouro: caldeirão com água + 1 fardo de feno; filhotes crescem rápido e companheiros se curam.")
                .ritual(Material.CAMPFIRE, "Fogueira acesa", Material.WHITE_WOOL, 1, Material.OAK_LOG, 1)
                .ligados("companheiros").palavras("fogueira descansado sentar bebedouro").pronto();

        // ===================== Comunidade =====================
        t("party", COMUNIDADE, "Party", Material.CAKE)
                .texto("Até 6 jogadores, sem fogo amigo, XP de Combate dividido e chat próprio (/pc).")
                .cmd("party").ligados("masmorras", "territorio").palavras("grupo amigos").pronto();
        t("territorio", COMUNIDADE, "Território", Material.LODESTONE)
                .texto("Protege 3×3 chunks (mais com nível). Só você e os membros constroem e abrem baús.",
                        "Expansão: o mesmo ritual longe, com 1 bloco de esmeralda a mais.")
                .ritual(Material.LODESTONE, "Magnetita", Material.WHITE_BANNER, 1, Material.EMERALD, 4)
                .cmd("territorio").ligados("colonia", "reino").palavras("marco protecao chunk expansao").pronto();
        t("colonia", COMUNIDADE, "Colônia", Material.BELL)
                .texto("Moradores de verdade trabalham para você no seu território inteiro, guardando o que produzem nos baús de depósito.")
                .ritual(Material.BELL, "Sino (no seu território)", Material.EMERALD_BLOCK, 1, Material.RED_BED, 1)
                .dica("Cada morador precisa do seu posto: composteira, barril, cortador de pedras...")
                .cmd("colonia").ligados("territorio", "reino").palavras("prefeitura morador vila exercito soldado").pronto();
        t("reino", COMUNIDADE, "Reino e guerra", Material.GOLDEN_HELMET)
                .texto("Com colônia nível 2: /reino fundar <nome>. Cada território de membro é uma província; o Rei decreta leis.",
                        "Guerras com horário marcado e captura de Marcos.")
                .cmd("reino").ligados("territorio", "colonia").palavras("rei provincia lei guerra tesouro").pronto();
        t("companheiros", COMUNIDADE, "Companheiros (Domador)", Material.LEAD)
                .texto("Vincule animais ou invoque criaturas no Altar do Domador. Eles obedecem ordens, ganham componentes e evoluem.")
                .ritual(Material.HAY_BLOCK, "Fardo de feno", Material.LEAD, 1, Material.SADDLE, 1)
                .cmd("pets").ligados("descanso").palavras("pet domar montaria altar domador").pronto();

        // ===================== Coleções =====================
        t("album", COLECOES, "Álbum de Cartas", Material.FILLED_MAP)
                .texto("Monstros, chefes e peixes raros às vezes deixam uma carta. Página completa = bônus para sempre.")
                .cmd("album").ligados("bestiario").palavras("carta brilhante").pronto();
        t("bestiario", COLECOES, "Bestiário", Material.ZOMBIE_HEAD)
                .texto("Os monstros que você derrotou. Marcos de abates dão mais dano contra aquele tipo.")
                .cmd("bestiario").ligados("album", "perigo").palavras("monstros abates").pronto();
        t("trofeus", COLECOES, "Estante de Troféus", Material.QUARTZ_PILLAR)
                .texto("Exponha o núcleo de um chefe. Cada troféu diferente no seu território dá +1% de XP.")
                .ritual(Material.QUARTZ_PILLAR, "Pilar de quartzo", br.rpgatributos.aventura.Raro.NUCLEO_GOLEM.criar(1), 1)
                .ligados("altar").palavras("trofeu nucleo").pronto();
        t("recordes", COLECOES, "Recordes", Material.GOLD_INGOT)
                .texto("Maior golpe, maior nível e os maiores números do servidor. Escreva [recordes] numa placa para virar um painel.")
                .cmd("recordes").ligados("titulos").palavras("ranking placa").pronto();
        t("segredos", COLECOES, "Segredos", Material.AMETHYST_SHARD)
                .texto("Conquistas escondidas: descubra fazendo coisas inesperadas pelo mundo.")
                .cmd("segredos").ligados("titulos").palavras("secreto").pronto();
        t("diario", COLECOES, "Diário", Material.WRITABLE_BOOK)
                .texto("Os marcos da sua jornada, cada um com a data em que aconteceu.")
                .cmd("diario").ligados("recordes").palavras("historia marcos").pronto();
        t("missoes", COLECOES, "Missões dos aldeões", Material.PAPER)
                .texto("Aldeões fazem pedidos simples em troca de esmeraldas e XP.")
                .cmd("missoes").ligados("vida").palavras("pedido aldeao").pronto();
    }

    public static List<Topico> todos() {
        return Collections.unmodifiableList(TODOS);
    }

    public static List<Topico> de(Categoria c) {
        return TODOS.stream().filter(t -> t.categoria() == c).toList();
    }

    public static Topico porId(String id) {
        for (Topico t : TODOS) if (t.id().equals(id)) return t;
        return null;
    }

    /** Assuntos com todas as palavras buscadas (nome primeiro). */
    public static List<Topico> buscar(String texto) {
        String[] termos = normalizar(texto).trim().split("\\s+");
        List<Topico> noNome = new ArrayList<>(), noTexto = new ArrayList<>();
        for (Topico t : TODOS) {
            String nome = normalizar(t.nome()), tudo = t.indice();
            boolean todosTermos = Arrays.stream(termos).allMatch(tudo::contains);
            if (!todosTermos) continue;
            if (Arrays.stream(termos).anyMatch(nome::contains)) noNome.add(t);
            else noTexto.add(t);
        }
        noNome.addAll(noTexto);
        return noNome;
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
