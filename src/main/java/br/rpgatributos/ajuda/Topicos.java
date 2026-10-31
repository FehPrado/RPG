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
        t("racas", PERSONAGEM, "Raças", Material.FEATHER)
                .texto("Escolha uma raça no /raca: Anão (65%), Elfo (110%), Pequenino (50%), Povo-Pássaro (90%), Povo do Mar ou Humano. O tamanho muda de verdade e todos veem.",
                        "Cada raça tem uma fraqueza e 5 estágios de Despertar. A Essência vem do XP dos atributos da raça (minerar para o Anão, nadar para o Povo do Mar...).",
                        "Com a Essência cheia, abra o /raca e faça a oferenda do estágio. O Povo-Pássaro plana no II, ganha asas no III e voa no V.")
                .dica("Trocar de raça custa 30 níveis e volta ao estágio I. O Humano escolhe uma Herança de outra raça no V.")
                .cmd("raca").ligados("atributos", "classes", "mobilidade").palavras("raca anao elfo pequenino passaro asas voar mar guelras humano despertar essencia tamanho").pronto();
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
                        "Cada classe tem passivo e habilidades. Classes dominadas liberam as avançadas.",
                        "Básicas: Guerreiro, Arqueiro, Mago, Ladino, Ferreiro, Domador, Tanque e Curandeiro. As lendárias (e os Monarcas) ficam na página \"Lendárias e Monarcas\". Nos portais, as habilidades ficam 20% mais fortes.")
                .ritual(Material.LECTERN, "Púlpito", Material.WRITABLE_BOOK, 1, Material.DIAMOND, 1)
                .dica("Habilidade: agache + F. Trocar a habilidade: agache + clique esquerdo no ar.")
                .cmd("classe").ligados("atributos", "combos", "locais").palavras("santuario prova habilidade guerreiro mago arqueiro tanque curandeiro monarca").pronto();
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
                        "Combos enchem o equilíbrio dos inimigos fortes; cheio, eles ficam parados e levam +50% de dano.",
                        "Maestria: os acertos com cada postura a sobem até o nível V (2% a mais por nível no que ela faz).")
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
                .texto("Bata nele (com arma, combo, habilidade ou magia) para ver o dano de cada golpe, o golpe de combo e o dano por segundo.",
                        "Clique no fardo: o treino por arma (dano por segundo, média, maior e os melhores combos). Agachado + clique zera.",
                        "Não dá XP: serve só para medir e treinar os combos.")
                .ritual(Material.HAY_BLOCK, "Fardo de feno", Material.CARVED_PUMPKIN, 1, Material.ARMOR_STAND, 1)
                .ligados("combos", "amolar").palavras("treino dps dano").pronto();
        t("arsenal", COMBATE, "Arsenal: adaga, foice, bastão...", Material.GOLDEN_SWORD)
                .texto("Seis armas novas feitas na bancada com o material da espada (madeira até diamante): Adaga, Faca e Machadinha de arremesso, Foice, Bastão e Chicote.",
                        "Adaga nas duas mãos = Lâminas Duplas. Faca e machadinha: clique direito arremessa (depois recolha).",
                        "Cada uma tem proficiência, 7 golpes de combo e uma Postura Mestra. Feitas na Forja do Ferreiro saem forjadas, com raridade.")
                .dica("Adaga pelas costas dá +50%. O Chicote alcança 3 blocos mais longe. Compare as armas no Boneco de Treino.")
                .cmd("combos").ligados("armas_minerios", "combos", "forja", "boneco", "nomeadas").palavras("adaga duplas faca machadinha foice bastao chicote arremesso").pronto();
        t("armas_minerios", COMBATE, "Armas dos minérios", Material.DIAMOND_SWORD)
                .texto("Oito armas feitas com os minerais do mundo, cada uma com mecânica própria, 7 golpes e Postura Mestra:",
                        "Montante (Mitrilo, duas mãos e varredura), Bumerangue (Âmbar, vai e volta), Leques de Guerra (Jade, rajada de vento), Arpão (Pérola, puxa o alvo ou você), Mangual (Cristal de Gelo, deixa lento), Garras (Pedra-Sangue, roubam vida), Clava Fumegante (Turfa, queima) e Lança de Basalto (Cristal de Magma do Nether, fogo e +25% em quem queima).",
                        "Uma receita só cada uma, com o mineral. Na Forja saem forjadas.")
                .dica("Bumerangue e Arpão: o clique direito arremessa, então os combos começam com o esquerdo.")
                .cmd("combos").ligados("minerais", "arsenal", "incrustar", "mitrilo").palavras("montante bumerangue leques arpao mangual garras clava mitrilo ambar jade perola gelo sangue turfa").pronto();
        t("tecnicas", COMBATE, "Técnicas secretas", Material.NETHER_STAR)
                .texto("Cada arma tem uma técnica secreta, um golpe a mais ensinado por um Mestre de Armas nas vilas.",
                        "Mestra das Marés (pescadores): tridente, lança, arpão, arremesso e bumerangue. Mestre do Bastão (mosteiro): bastão, leques, chicote, adaga e duplas.",
                        "Mestre de Guerra (anões): machado, maça, montante, mangual e clava. Mestra da Lâmina (élfica): espada, arco, foice e garras.",
                        "Ferreiro Piglin (Forja dos Piglins, no Nether): Lança de Basalto, pago em ouro.",
                        "Cada técnica pede proficiência 12 na arma e 24 esmeraldas. Depois ela aparece no /combos.")
                .dica("Aprender com um mestre também aumenta o laço com ele.")
                .cmd("combos").ligados("combos", "vilas").palavras("tecnica secreta mestre armas ensinar").pronto();
        t("aparar", COMBATE, "Aparar com o escudo", Material.SHIELD)
                .texto("Levante o escudo bem na hora do golpe, de frente para o atacante: o golpe some, flechas voltam e o atacante fica lento e fraco.",
                        "Você ganha 10 de Vigor e o próximo golpe em 1,5 s faz ×1,5.",
                        "Escudo de Duelo (ferro, ouro, ferro em cima; escudo no meio): mais tempo para aparar e contra-ataque de ×1,8.")
                .dica("Segurar o escudo levantado o tempo todo só bloqueia: o aparar é levantar no instante certo.")
                .ligados("posturas", "fraquezas").palavras("aparar parry escudo duelo contra-ataque").pronto();
        t("fraquezas", COMBATE, "Fraquezas e clima", Material.SPIDER_EYE)
                .texto("Cada família de criatura apanha mais de algumas armas e menos de outras: esqueletos de maça e clava, aranhas de lança e foice, criaturas de fogo de tridente e arpão, voadores de arco e bumerangue.",
                        "Na chuva tridente e arpão fazem mais (ainda mais na tempestade), a clava e o arco menos, e o fogo se apaga. No frio o mangual ganha 15%.",
                        "Montado: correndo, o golpe vira carga (até +50%), flecha faz +15% e o escudo levantado protege a montaria.")
                .dica("O aviso aparece na barra no primeiro golpe; a lista de cada criatura está no Bestiário.")
                .ligados("combos", "aparar").palavras("fraqueza clima chuva carga montado bestiario").pronto();
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
        t("masmorras", COMBATE, "Masmorras e Cristais de Portal", Material.CHISELED_STONE_BRICKS)
                .texto("O Altar do Portal faz um Cristal de Portal (rank E a S) com uma oferenda ou com Cristais de Mana. Clique no altar com o cristal na mão e suba nele com a party.",
                        "Cada cristal tem um objetivo: Caçada (o chefe), Saque (achar as relíquias, vasos dourados) ou Monólitos (despertar as magnetitas e vencer os guardiões). Às vezes vem com modificadores: mais risco, mais tesouro.",
                        "São 15 minutos. Quem não sair a tempo perde o que pegou lá dentro (vai para o Cofre das Almas). Os baús têm raridade: madeira, ferro, ouro e cristal.",
                        "Vencer dá XP de caçador: o Rank de Caçador (/masmorra rank) diz até que rank de cristal o altar faz para você.",
                        "O vault (2.69): salas em grade com corredores, atalhos, pontes e túneis; até 3 andares ligados por Escadarias. Minas de Cristal (quebre a ametista) e salas secretas atrás de paredes rachadas (um golpe derruba).",
                        "Cuidado (2.70): placas-armadilha, baús que são mímicos e o chefe com 3 fases (saia dos círculos vermelhos). Os Pilares de Bênção dão a bênção de um deus à party. No fim, uma nota de S a D: nota boa, mais tesouro. /masmorra stats e /masmorra recordes.",
                        "Equipamento (2.71): armas e armaduras do vault vêm não identificadas (clique direito revela; no altar, o Avaliador cobra 1 de mana e às vezes sobe a raridade). Peças de Caçador dão mais dano nos monstros dos portais e menos dano deles. Vencer o chefe às vezes dá a arma dele (Lendária). /masmorra loja: poções, Pergaminho de Retorno, Medidor de Mana e Chave Instantânea, pagos com Cristais de Mana.",
                        "Caçador (2.72): /masmorra arvore abre a Árvore de Caçador (pontos a cada rank; bônus só nos portais) e as características do Despertar (a 1ª no Rank D, a 2ª no B e a 3ª no S). Mesa de Inscrição: livro + tinta brilhante numa mesa de flecheiro; escreve Inscrições e as aplica no Cristal de Portal (até 4: modificador, mais tempo, sala de tesouro, mina ou objetivo).",
                        "Nos portais, as habilidades da sua classe ficam 20% mais fortes. Missão Diária (/masmorra missao): agachar, pular, correr e abater monstros dão XP de caçador e Cristais de Mana.",
                        "Monarcas: as Relíquias do Monarca caem nos portais A e S. No Mural das Relíquias, 5 de um Monarca abrem o Portal dele: quem der o golpe final herda a classe lendária daquele Monarca (uma por servidor). As 25 abrem o portal do Monarca da Destruição, o chefe final. /masmorra monarcas.",
                        "Masmorra Dupla (2.75): às vezes um portal E ou D esconde a Câmara das Estátuas atrás do chefe. Obedeça os mandamentos: agache diante do deus, toque os quatro sinos e chegue ao altar sem se mexer quando os olhos dele abrirem. A cada 3 dias, um Ataque de Rank S chama todas as parties contra Beru, o Rei das Formigas.")
                .dica("O Grimório Oculto (raro nos portais B, A e S) ensina a magia Abrir Portal: abre um portal do seu rank, gastando a mana e um Cristal de Mana grande.")
                .ritual(Material.CHISELED_STONE_BRICKS, "Tijolos de pedra entalhados", Material.ENDER_EYE, 1, Material.COMPASS, 1)
                .cmd("masmorra").ligados("party", "torre", "portais").palavras("dungeon portal cofre cristal rank cacador mana reliquia monolito grimorio vermelho loja sistema avaliador identificar pergaminho chave arvore despertar caracteristica inscricao mesa funcao lutador tanque curandeiro mago assassino arqueiro habilidade missao diaria monarca reliquias mural antares dupla camara estatuas sinos ataque beru formigas").pronto();
        t("inscricao", COMBATE, "Mesa de Inscrição", Material.FLETCHING_TABLE)
                .texto("Escreve Inscrições (1 papel comum + Cristais de Mana) e as aplica num Cristal de Portal: segure o cristal, clique na mesa e clique esquerdo na Inscrição.",
                        "Até 4 por cristal: um modificador (Fúria, Couraça, Tesouro Farto...), Tempo (+3 minutos), Tesouro (+1 sala de tesouro), Mina (+1 Mina de Cristal) ou Objetivo (Caçada, Saque ou Monólitos).",
                        "Os baús do vault às vezes trazem Inscrições prontas; o do chefe, metade das vezes.")
                .ritual(Material.FLETCHING_TABLE, "Mesa de flecheiro", Material.BOOK, 1, Material.GLOW_INK_SAC, 1)
                .ligados("masmorras").palavras("inscricao inscrever cristal portal modificador tempo mesa flecheiro").pronto();
        t("mural", COMBATE, "Mural das Relíquias", Material.CHISELED_POLISHED_BLACKSTONE)
                .texto("Guarda as Relíquias do Monarca: 5 linhas de 5, uma para cada Monarca. Qualquer um pode colocar; se o mural for quebrado, elas caem no chão.",
                        "Linha completa: um caçador A ou S abre ali o Portal daquele Monarca (rank S, 25 minutos). Quem der o golpe final herda a classe lendária do Monarca, se ela estiver livre.",
                        "As 25 juntas abrem o portal de Antares, o Monarca da Destruição: o chefe final.")
                .dica("As relíquias caem nos tesouros finais dos portais A (6%) e S (12%).")
                .ritual(Material.CHISELED_POLISHED_BLACKSTONE, "Pedra-negra polida entalhada", Material.CRYING_OBSIDIAN, 1, Material.GOLD_BLOCK, 1)
                .cmd("masmorra").ligados("masmorras").palavras("mural reliquia monarca portal antares baran geada feras insetos").pronto();
        t("torre", COMBATE, "Torre Infinita", Material.CRYING_OBSIDIAN)
                .texto("Andares cada vez mais difíceis, com chefe e baú a cada 10. Ranking semanal e de todos os tempos.",
                        "Morrer na Torre não perde itens.")
                .ritual(Material.CRYING_OBSIDIAN, "Obsidiana chorona", Material.ENDER_EYE, 1, Material.GOLD_BLOCK, 1)
                .cmd("torre ranking").ligados("masmorras", "party").palavras("obelisco andar ranking").pronto();
        t("portais", COMBATE, "Portais pelo mundo", Material.END_PORTAL_FRAME)
                .texto("Portais aparecem perto de quem explora, com rank de E a S. Entre e vença a masmorra antes que ele transborde e monstros saiam.",
                        "Portal Vermelho: lá dentro não há saída até vencer, mas o XP de caçador dobra.")
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
        t("circuloarcano", MAGIA, "Círculo Arcano (pentagrama)", Material.OBSIDIAN)
                .texto("Desenhe um pentagrama no chão em volta de uma obsidiana: velas acesas nas 5 pontas, a 5 blocos do centro, em (0,-5), (5,-2), (3,4), (-3,4) e (-5,-2), contando a partir da obsidiana (o -5 é para o norte). O traço da estrela liga cada ponta à segunda seguinte.",
                        "O traço pode ser pó de redstone em cima do piso ou blocos no próprio piso: obsidiana, pedra-negra, tijolos do Nether, ametista, areia das almas, pedra luminosa, concreto ou lã preta, vermelha ou roxa. Basta 85% do traço.",
                        "Jogue 1 olho do ender e 4 pós de pedra luminosa na obsidiana: o círculo desperta. Para um ritual, jogue as oferendas dentro do círculo e clique na obsidiana com o catalisador.",
                        "O traço acende ponta por ponta, um círculo gigante de luz com a estrela se desenha no céu, gira cada vez mais rápido e desce num feixe. Se você se afastar mais de 16 blocos ou o pentagrama quebrar, o ritual se volta contra você.",
                        "Pactos de 1 hora, um por vez: Força (+25% de dano, 2 corações a menos), Sombra (invisível e veloz no escuro, fraco ao sol) e Fortuna (sorte, pressa e desconto, mas sempre com fome).",
                        "Tempestade Arcana (raios nos monstros), Fenda do Vazio (3 ondas e o Guardião da Fenda, com tesouro), Corcel Espectral, Espíritos Aliados, Eclipse, Lua de Sangue, Banimento (some com os monstros e protege 10 minutos) e Revelação Astral.")
                .dica("Mão vazia na obsidiana abre o grimório com todos os rituais; agachado, confere o desenho.")
                .ritual(Material.OBSIDIAN, "Obsidiana (com o pentagrama)", Material.ENDER_EYE, 1, Material.GLOWSTONE_DUST, 4)
                .ligados("convergencia", "transmutacao").palavras("pentagrama circulo arcano vela pacto tempestade fenda vazio eclipse lua sangue banimento revelacao conjurar corcel espirito").pronto();
        t("transmutacao", MAGIA, "Transmutação", Material.AMETHYST_BLOCK)
                .texto("Transforme materiais em outros mais raros. A Pedra Filosofal melhora tudo.")
                .ritual(Material.AMETHYST_BLOCK, "Bloco de ametista", br.rpgatributos.alquimia.Reagente.MERCURIO_VIVO.criar(1), 1, Material.GOLD_INGOT, 1)
                .dica("O Mercúrio Vivo é feito na Bancada Alquímica.")
                .ligados("alquimia", "runas").palavras("circulo pedra filosofal mercurio").pronto();
        t("convergencia", MAGIA, "Altar da Convergência", Material.RESPAWN_ANCHOR)
                .texto("Até 8 pedestais em volta do altar (16 no nível II, 24 no III), a 3 blocos (4 em cruz e 4 na diagonal, 2+2), na mesma altura ou 1 acima ou abaixo. Pedestal: bloco talhado, o que tem desenho (tijolos de pedra talhados, ardósia, quartzo, tufo, arenito talhados...) ou pilar de quartzo ou púrpura. Tijolo comum não serve.",
                        "Clique num pedestal com um item para pôr nele (agachado: só 1); clique de novo para tirar. Com o catalisador na mão, clique no altar: 10 segundos de cerimônia e o resultado cai no centro.",
                        "Transmutar: Eco, Lágrimas de Ghast, Náutilo, Molde de Netherite, Garrafas de Experiência, Varas de Blaze, Pérolas e Cascas de Shulker. Infundir: o equipamento na mão sobe 1 nível de encantamento além do normal (até 3 infusões por item). Transferir Encantamentos: o item encantado vai num pedestal (com um livro, a ponte) e os encantamentos dele passam para o da mão; a origem volta sem eles.",
                        "Raros: Totem, Maçã Dourada Encantada, Coração do Mar, Estrela do Nether, Élitro e Essência Primordial, e as 4 Relíquias da Convergência (Lâmina, Égide, Picareta e Arco), uma de cada por jogador.",
                        "Invocações maiores: o núcleo de um chefe na mão desperta a versão Desperta dele (2,5x a vida, +50% de dano), com saque dobrado, Essência garantida e a Alma Desperta.",
                        "Trazer de Volta: quando um companheiro morre, cai a coleira dele. Com ela na mão (ou uma etiqueta com o nome dele), o ritual traz ele de volta com o nível e a evolução.",
                        "Céu, território e altar: chamar chuva, céu limpo, dia ou noite, a Lua Azul (monstros soltam materiais raros), auras de 2 horas no território (Crescimento, Paz, Vigor) e a Bênção da Aliança (3 jogadores). E também: Chuva de Meteoros em volta do altar, Abrir um Portal do seu rank (um a cada 6 horas) e Chamar o Mercador.",
                        "Algumas receitas pedem um sacrifício: o mob vivo a até 5 blocos do altar sobe e se desfaz na explosão. Outras pedem vários jogadores com o catalisador na mão.",
                        "Despertar o Altar (estrela do Nether e 2 vacas) sobe o altar para o nível II: mais 8 pedestais a 5 blocos e as receitas mais fortes (relíquias, Arauto, Lua Azul, níveis IV e V dos guardiões).",
                        "Ascender o Altar (2 estrelas do Nether, sinalizador e 2 endermen) sobe para o nível III: mais 8 pedestais a 7 blocos (4 em cruz e 4 na diagonal, 5+5) e as lendárias: Ovo do Dragão e o Tridente, a Maça e as Botas da Convergência.",
                        "Se quem acendeu se afastar mais de 24 blocos ou um pedestal usado quebrar, o ritual sai errado: os ingredientes se perdem e uma Aberração sai do altar.")
                .dica("Mão vazia no altar abre o Grimório com todas as receitas, em abas; agachado conta os pedestais e mostra o nível.")
                .ritual(Material.RESPAWN_ANCHOR, "Âncora de renascimento", Material.ENDER_EYE, 1, Material.DIAMOND, 1, Material.AMETHYST_SHARD, 4)
                .ligados("transmutacao", "infusor", "guardioesaltar").palavras("altar convergencia pedestal ritual infundir infusao transmutar grimorio desperto alma reliquia elitro sacrificio chuva noite lua azul aura bencao nivel transferir encantamento coleira reviver companheiro meteoro portal mercador dragao tridente maca botas").pronto();
        t("guardioesaltar", MAGIA, "Guardiões e ajudantes", Material.CARVED_PUMPKIN)
                .texto("Feitos no Altar da Convergência (aba Guardiões). O ritual dá um Selo: com ele na mão, clique num bloco e o guardião fica ali, de posto. Até 8 por jogador.",
                        "Defesa: Espírito Guardião (raios), Golem Guardião (golem de ferro), Torre Arcana (explosões de fogo em área) e Espírito Curandeiro (cura jogadores e guardiões).",
                        "Trabalho: Golem de Fazenda (colhe e replanta), Golem Lenhador (derruba árvores e replanta), Golem Minerador (tira minérios à vista e deixa pedra), Pastor de Neve (tosquia, junta ovos e lã, traz os animais de volta) e Golem de Cobre (organiza baús).",
                        "Novos: Golem Pescador (vara; pesca na água perto do posto, com isca no baú pesca mais rápido), Abelha Apicultora (tesoura; tira o mel das colmeias cheias com fumaça: garrafas, se tiver garrafas no baú, ou favos), Golem Ferreiro (bigorna perto; conserta o que está no baú com o material de cada item; no nível III, as ferramentas dos seus guardiões), Espírito Coletor (junta o que cai e guarda no baú que já tem aquele item) e Vigia (toca os sinos, acende as lâmpadas, marca e avisa quando chega monstro ou estranho).",
                        "Fundir Guardiões (altar nível II): o Selo da mão fica com o corpo e o trabalho, e o do pedestal empresta a função dele, se for de apoio (Espírito, Torre, Curandeiro, Vigia ou Ferreiro). Fica com o maior nível e 25% mais vida. Dois que usam munição não se fundem.",
                        "Níveis I a V: guarde o guardião no Selo, segure o Selo no altar com o pedido do nível, o tributo dele e um sacrifício (galinhas, ovelha, vacas, zumbi). Cada nível dá mais vida, força e alcance; no III os trabalhadores ganham mochila; no V brilham e se curam mais rápido.",
                        "Asas de Guardião fazem voar; o Cetro de Comando faz o guardião seguir você ou ficar de posto onde está.",
                        "Ferramenta: clique no guardião com ela (picareta no Minerador, machado no Lenhador, enxada na Fazenda, tesoura no Pastor, espada no Golem Guardião). Melhor ferramenta, mais rápido; ela gasta e quebra. Insumos (tábuas, cercas, tochas, sementes, farinha de osso, comida, baldes, lingotes) saem do baú perto do posto.",
                        "Minerador: cava um túnel 3x3 na direção em que você olhava ao pôr o Selo, com sustentação a cada 4 blocos (4 cercas e 3 tábuas), tocha a cada 8 e uma câmara 5x5 a cada 16. Tampa lava, água e cascalho e pega os minérios das paredes. Túnel de 24 a 96 blocos, conforme o nível.",
                        "Munição (Espírito: ametista ou eco; Torre: pó de blaze ou carga de fogo; Curandeiro: mel ou melancia reluzente): clique com ela ou deixe num baú perto. Sem munição eles continuam, mas fracos.")
                .dica("Agachado, clique no seu guardião com a mão vazia para guardá-lo no Selo (nível, asas e mochila ficam). Sem agachar, abre a mochila (nível III+).")
                .ligados("convergencia").palavras("guardiao guardioes golem fazenda colher cobre bau organizar sentinela espirito allay asas voar protetor lenhador arvore minerador minerio pastor ovelha curandeiro torre nivel cetro mochila sacrificio pescador pesca abelha apicultor mel colmeia ferreiro consertar bigorna coletor vigia alarme sino lampada fundir fusao").pronto();
        t("deuses", MAGIA, "Deuses e fé", Material.CHISELED_QUARTZ_BLOCK)
                .texto("Siga um dos 6 deuses. Oferendas e preces dão devoção; a fé sobe de 1 a 5 e libera um milagre.")
                .ritual(Material.CHISELED_QUARTZ_BLOCK, "Quartzo entalhado", Material.GOLDEN_APPLE, 1, Material.CANDLE, 1)
                .dica("Santuários Esquecidos pelo mundo dão devoção dobrada.")
                .cmd("deus").ligados("astronomia", "estruturas").palavras("ferrum sylva maris bellum arcanus mortis milagre devocao santuario").pronto();
        t("astronomia", MAGIA, "Astronomia", Material.SPYGLASS)
                .texto("À noite, olhe para o céu com a luneta por 5 segundos: você observa uma constelação e ganha a bênção dela até o amanhecer.")
                .cmd("estrelas").ligados("deuses", "estacoes").palavras("estrelas constelacao luneta").pronto();

        // ===================== Ofícios =====================
        t("encantos", MAGIA, "Encantamentos do RPG", Material.ENCHANTED_BOOK)
                .texto("18 encantamentos novos, de verdade: saem na mesa de encantamento, em livros, nos baús, com os aldeões e com o Bibliotecário, e juntam na bigorna.",
                        "Na mesa: Golpe Lunar (mais dano à noite, o dobro na lua cheia), Ceifador (mais XP dos monstros), Saltador (pula mais alto, cai sem dano de mais alto), Passo Ligeiro (calças, mais rápido), Braço Longo (ferramentas, alcança blocos mais longe), Veio Generoso (mais XP dos minérios) e Flecha Gélida (arco, a flecha deixa lento).",
                        "Raros (baús, aldeões, Bibliotecário nível 7+): Roubo de Vida (às vezes o golpe cura você), Sifão de Mana (cada golpe devolve mana) e Vigor (peitoral, +1 coração máximo por nível).",
                        "De luta, na mesa: Lâmina do Vento (empurra longe), Estilhaço de Gelo (machado: às vezes congela quem está perto), Flecha Explosiva (besta), Onda de Choque (botas: cair de alto empurra os monstros) e Trilha Florida (botas: flores por onde passa).",
                        "De luta, raros: Corrente de Trovão (às vezes um raio cai no alvo e salta para outros monstros) e Passos de Brasa (botas: rastro de fogo correndo).",
                        "Maldição do Peso: aparece em livros e armaduras dos baús e deixa você mais lento. Como as maldições do jogo, não sai no rebolo.")
                .dica("Na Lua de Sangue o Golpe Lunar bate ainda mais forte.")
                .ligados("magias", "forja").palavras("encantamento encantar livro mesa bigorna golpe lunar ceifador saltador passo ligeiro braco longo veio generoso flecha gelida roubo vida sifao mana vigor maldicao peso lamina vento estilhaco gelo explosiva onda choque trilha florida corrente trovao passos brasa").pronto();
        t("forja", OFICIOS, "Forja do Ferreiro", Material.ANVIL)
                .texto("Armas, ferramentas e armaduras feitas nela saem forjadas: de Comum a Mítico, com bônus e efeitos.",
                        "Agachado + clique: refino.")
                .ritual(Material.ANVIL, "Bigorna", Material.IRON_BLOCK, 2, Material.LAVA_BUCKET, 1)
                .dica("Ferraria alta deixa os itens mais raros. O balde volta vazio.")
                .cmd("forja").ligados("reciclagem", "gemas", "lendas").palavras("forjar raridade refino ferreiro mitico lendario").pronto();
        t("reciclagem", OFICIOS, "Reciclagem (desmanchar)", Material.GRINDSTONE)
                .texto("Desmanche até 9 itens de uma vez (armas, ferramentas, armaduras) e recupere de 50% a 75% do material.",
                        "O material principal vem primeiro: a sobra vira chance e item pouco gasto devolve ao menos 1 (a pá de ferro devolve 1 ferro).",
                        "Encantados devolvem XP e às vezes um livro; gemas voltam inteiras; armas dos minérios devolvem o mineral.")
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
        t("criacao", OFICIOS, "Criação de animais", Material.WHEAT)
                .texto("Agachado + clique com a mão vazia num animal adulto: ele vira da sua fazenda (até 40) e mostra os corações.",
                        "Todo dia: carinho e comida (ração na mão, pasto ou fardo de feno). Com afeto alto ele dá mais: ovos, leite, lã, trufas.",
                        "Inverno ao ar livre à noite derruba o afeto: deixe num celeiro com teto.")
                .cmd("animais").ligados("comedouro", "tacho", "linhagens").palavras("animal fazenda afeto coracao leite ovo la trufa galinha vaca cabra ovelha porco coelho").pronto();
        t("comedouro", OFICIOS, "Comedouro", Material.CHEST)
                .texto("Guarde a comida dos animais no baú: na virada do dia, cada animal seu a até 12 blocos que não comeu come uma porção.",
                        "Agachado + clique mostra para quantos dias a comida dá.")
                .ritual(Material.CHEST, "Baú", Material.HAY_BLOCK, 1, Material.BOWL, 1)
                .ligados("criacao").palavras("comida racao animais funil").pronto();
        t("tacho", OFICIOS, "Tacho do Artesão", Material.DECORATED_POT)
                .texto("Clique com o ingrediente na mão (até 8 iguais) e espere: leite vira queijo, fruta vira geleia, legume vira conserva, ovo vira maionese, trufa vira óleo.",
                        "O produto guarda a qualidade e dá efeitos ao comer.")
                .ritual(Material.DECORATED_POT, "Vaso decorado", Material.GLASS_BOTTLE, 4, Material.SUGAR, 1)
                .ligados("criacao", "cozinha").palavras("queijo geleia conserva maionese oleo trufa artesanato").pronto();
        t("pesca", OFICIOS, "Pesca", Material.FISHING_ROD)
                .texto("Peixes com qualidade, 21 peixes raros por bioma e clima (5 só no buraco no gelo, 4 só na lava do Nether com a Vara Ígnea), Tesouros do Mar e criaturas que lutam de volta.")
                .cmd("peixes").ligados("pescagelo", "alquimia", "cozinha").palavras("peixe raro tesouro mar vara").pronto();
        t("pescagelo", OFICIOS, "Pesca no gelo", Material.PACKED_ICE)
                .texto("Quebre o gelo de um lago ou mar gelado (ou de qualquer lago no inverno) e pesque no buraco: +5% de peixe raro.",
                        "5 peixes só do gelo: Truta-do-Gelo, Lúcio Polar, Peixe-Lanterna, Enguia de Cristal e o raríssimo Esturjão Ancestral.",
                        "Cabana de Pesca: a até 12 blocos, isca 30% mais rápida, +4% de raro e ninguém sente frio.")
                .ritual(Material.CAMPFIRE, "Fogueira", Material.FISHING_ROD, 1, Material.COD, 1, Material.SPRUCE_PLANKS, 4)
                .dica("O Esturjão Ancestral só morde de noite, no inverno. Com ele sai o Caviar Ancestral.")
                .cmd("peixes").ligados("pesca", "cozinha", "alquimia").palavras("gelo buraco inverno cabana neve truta lucio esturjao caviar").pronto();
        t("atelie", OFICIOS, "Curtume e tecelagem", Material.LOOM)
                .texto("Ateliê do Curtidor: curta as peles (com farinha de osso), teça linha, lã ou alga e costure armaduras leves.",
                        "Peles: lobos, ursos polares, raposas, guardiões e hoglins. Membrana de phantom vira Couro Noturno.",
                        "6 conjuntos (Caçador, Urso Polar, Raposa, Maré, Noite e Brasa), cada um com bônus de 2 e de 4 peças.")
                .ritual(Material.LOOM, "Tear", Material.SHEARS, 1, Material.LEATHER, 4, Material.STRING, 2)
                .dica("Jogue uma peça e um Fragmento de Forja na Forja: ela é reforjada com raridade e o conjunto continua valendo.")
                .ligados("forja", "posturas", "pescagelo").palavras("curtume couro pele tear tecido costurar conjunto armadura leve lobo urso raposa").pronto();
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
        t("incrustar", OFICIOS, "Incrustar minério", Material.EMERALD)
                .texto("Arma corpo a corpo na mão, um mineral na outra mão e clique na Forja do Ferreiro (custa 2 níveis de XP).",
                        "Jade: +1 ❤. Âmbar: +1 de sorte. Pérola: +25% de dano na água ou na chuva. Cristal de Gelo: deixa lento. Pedra-Sangue: rouba vida. Turfa: às vezes queima.",
                        "Uma incrustação por arma; uma nova troca a antiga. Ela fica ao refinar e reforjar.")
                .ligados("forja", "minerais", "armas_minerios").palavras("incrustacao engaste mineral jade ambar perola gelo sangue turfa").pronto();
        t("nomeadas", OFICIOS, "Armas com nome e Trajes", Material.DIAMOND_CHESTPLATE)
                .texto("Às vezes a peça forjada já nasce com nome, história e visual próprios (mais chance nas raridades altas). Também aparecem em baús e com chefes.",
                        "Armaduras viram peças de um Traje, com textura própria no corpo. As armas com nome ficam em 3D na mão.",
                        "Só muda a aparência: a força é a da raridade.")
                .dica("Uma adaga só vira adaga com nome, um Montante só vira Montante com nome, e assim por diante.")
                .ligados("forja", "arsenal", "guardaroupa").palavras("nomeada traje nome historia lendaria visual 3d").pronto();
        t("mitrilo", OFICIOS, "Mitrilo", Material.RAW_IRON)
                .texto("Minério raro no fundo do mundo (picareta de diamante ou netherita). Funda e use na mesa de ferraria para melhorar netherita.")
                .ligados("forja", "mapas").palavras("minerio raro lingote").pronto();
        t("conjuntos_minerais", OFICIOS, "Conjuntos dos minerais", Material.IRON_CHESTPLATE)
                .texto("Espada, picareta, machado, pá, enxada e as 4 peças de armadura de Âmbar, Jade, Pérola, Gelo, Pedra-Sangue, Magma e Quartzo Negro. A receita é a do jogo, com o mineral no lugar do ferro.",
                        "Mais fortes que o ferro: +1 de armadura e +1 de dureza por peça, a espada com +0,5 de dano e tudo mais durável. A armadura aparece no corpo com a cor do mineral.",
                        "Com 2 e 4 peças vestidas: Âmbar dá sorte; Jade, vida; Pérola, água; Gelo deixa lento quem bate; Pedra-Sangue rouba vida; Magma protege do fogo; Quartzo Negro acalma os piglins.",
                        "Picareta de Âmbar: às vezes o minério solta um a mais. Picareta de Magma: ferro, ouro e cobre já saem em lingote.")
                .dica("Conserta na bigorna com o mineral. Vai à Forja (sai com raridade) e a Reciclagem devolve o mineral.")
                .ligados("minerais", "forja", "reciclagem").palavras("conjunto armadura espada picareta ambar jade perola gelo sangue magma quartzo negro").pronto();

        // ===================== Mundo e aventura =====================
        t("estacoes", MUNDO, "Estações, clima e céu", Material.CLOCK)
                .texto("Cada estação dura uma semana real e muda plantas, pesca e monstros. Festival no 4º dia.",
                        "O chão muda: flores na primavera, vaga-lumes no verão, folhas secas no outono, neve no inverno.",
                        "Lua cheia, lua de sangue, eclipse, aurora e chuva de meteoros.",
                        "Arco-íris quando a chuva para de dia: o Pote do Arco-Íris fica onde ele toca o chão. Estrela cadente à noite: agache olhando para o céu e faça um pedido.")
                .cmd("calendario").ligados("coletas", "plantacoes", "pomar", "perigo").palavras("estacao inverno verao clima lua festival arco iris pote estrela cadente pedido").pronto();
        t("coletas", MUNDO, "Coisas para colher", Material.BROWN_MUSHROOM)
                .texto("Cada estação espalha 3 coisas pelo chão, conforme o bioma: cogumelos, bagas, nozes, raízes, algas...",
                        "Procure o brilho verde perto de você e clique para pegar. Elas somem quando a estação acaba.",
                        "Todas se comem e dão um efeito pequeno.")
                .dica("O /calendario diz o que dá para colher nesta estação.")
                .cmd("calendario").ligados("plantacoes", "estacoes", "pomar").palavras("coleta colher cogumelo morel amora castanha raiz alga").pronto();
        t("plantacoes", OFICIOS, "Plantações da estação", Material.WHEAT_SEEDS)
                .texto("20 plantas, 5 por estação (o milho vai do verão ao outono e a cebola do inverno à primavera). Numa estufa com teto de vidro, crescem o ano todo.",
                        "Plante na terra arada (clique com as sementes). Elas só crescem na estação delas e com a terra molhada.",
                        "4 fases; farinha de osso adianta uma. Morango, mirtilo e uva rebrotam depois de colher.")
                .dica("As sementes saem do mato quebrado na estação certa.")
                .cmd("calendario").ligados("coletas", "fazenda", "cozinha").palavras("plantar semente morango mirtilo uva pimenta couve abobora nabo").pronto();
        t("minerais", MUNDO, "Minerais dos biomas", Material.RAW_GOLD)
                .texto("Cada bioma tem um mineral: Âmbar (deserto), Jade (selva), Pérola (ostras no mar), Cristal de Gelo (montanhas geladas), Pedra-Sangue (Nether) e Turfa (pântano).",
                        "Servem para acessórios, para as armas dos minérios e para incrustar na Forja. Nos geodos há cristais para lapidar com o Cinzel; nos rios, a Bateia garimpa.")
                .dica("Minerando perto de algo raro a pedra \"canta\". O Pêndulo do Rabdomante diz se está quente ou frio.")
                .ligados("armas_minerios", "incrustar", "acessorios", "mitrilo").palavras("ambar jade perola ostra cristal gelo pedra sangue turfa geodo cinzel bateia").pronto();
        t("vilas", MUNDO, "Vilas e moradores", Material.OAK_DOOR)
                .texto("Quatro tipos de vila surgem em terra nunca visitada, cada uma com 4 moradores.",
                        "Clique num morador: Laço (5 capítulos; no último, um presente único), Loja (muda todo dia, alguns compram coisas suas) e Serviço (o que só ele faz).",
                        "Os moradores também jogam Duelo de Cartas.")
                .ligados("estruturas", "duelo", "colonia").palavras("vila morador loja servico laco pescadores mosteiro anoes elfica").pronto();
        t("conforto", MUNDO, "Conforto no dia a dia", Material.LODESTONE)
                .texto("Colher: clique direito na plantação madura (trigo, cenoura, batata, beterraba, verruga) colhe e replanta.",
                        "Ordenar: mão vazia, clique direito num espaço vazio do baú (ou da mochila): tudo se organiza.",
                        "Repor a mão: acabou a pilha ou a ferramenta quebrou, outra igual vem do inventário. Portas duplas abrem juntas (agachado, só uma).",
                        "Elevador (4 ferros em cruz + pérola do ender): pule para subir, agache para descer. Apito do Domador (bambu, pepita, linha): seus bichos vêm até você.")
                .dica("/territorio boasvindas <frase>: quem entra no seu território lê a sua frase.")
                .ligados("vanilla", "companheiros", "territorio").palavras("conforto colher replantar ordenar organizar bau repor porta dupla elevador apito boas vindas").pronto();
        t("integracao", MUNDO, "O Minecraft com mais vida", Material.BELL)
                .texto("Conquistas (tecla L): há uma aba RPGAtributos. O livro de receitas mostra as receitas do servidor quando você tem um ingrediente.",
                        "Baús das estruturas do próprio jogo às vezes trazem itens do servidor. No menu dos moradores, Conversar abre perguntas conforme o laço.",
                        "Vilas do jogo: aldeões com nome; o sino chama todos para a praça; a cada 3 dias é dia de feira (trocas 20% mais baratas).",
                        "Trilho reto: o carrinho corre o dobro; um sino ao lado do trilho toca quando você passa. Vela de Barco (lã em cruz + 2 gravetos): clique num barco e reme; o vento por trás dá mais velocidade, de frente quase nada (veja o vento no /calendario).",
                        "Barco Grande (7 tábuas + 2 barcos): 5 lugares, com baús, rede, âncora e casco reforçado (veja o tópico do barco). Comboio: laço num barco e, agachado, clique em outro barco: ficam amarrados.",
                        "Para-raios atingido solta faíscas: clique para ficar ligeiro e forjar com mais chance de raridade. Funil em cima da Bancada de Reciclagem e baú embaixo: ela recicla sozinha.")
                .ligados("vanilla", "conforto", "vilas").palavras("conquista avanco receita livro bau loot conversar aldeao sino feira trilho carrinho ferrovia vela barco grande comboio para raios cobre reciclagem funil").pronto();
        t("utilidades", MUNDO, "Utilidades do dia a dia", Material.PAPER)
                .texto("Chave de Construtor (2 ferros, 2 gravetos): gira o bloco clicado; agachado, vira de cabeça para baixo.",
                        "Funil com filtro: uma moldura com um item presa no funil faz ele só aceitar aquele item.",
                        "Regador (cobre e balde): enche na água; molha a plantação em 3x3 e ajuda a crescer. Lanterna de mão: segurando tocha ou lanterna, onde você está fica iluminado.",
                        "Bancada de Bolso (bancada e couro): abre a bancada; agachado, a pedra de amolar, o tear, o cortador, a cartografia ou a ferraria.",
                        "Balde de Gelo (Cristal de Gelo e balde): guarda 4 baldes de água ou de lava. Trena: mede entre dois blocos. Cinto de Ferramentas: segure e bata no bloco, a ferramenta certa vem para a mão.")
                .ligados("conforto", "integracao", "vanilla").palavras("chave construtor girar funil filtro regador lanterna luz bancada bolso balde gelo trena medir cinto ferramentas").pronto();
        t("carroca", MUNDO, "Carroça", Material.SPRUCE_CHEST_BOAT)
                .texto("Receita: baú em cima, um barco no meio entre 2 tábuas e 2 gravetos embaixo. Clique no chão para pôr.",
                        "Laço na carroça e depois clique num cavalo, mula, burro, lhama ou camelo domado: ela vai atrás (o animal fica um pouco mais lento).",
                        "Clique: a carga. Agachado + clique: solta (ou senta, com o Banco). Batendo, vira item de novo.",
                        "Melhorias, clicando com o item: bloco de ferro (rodas), baú (54 espaços), 3 lãs (toldo: monstros não quebram), lanterna (luz), alavanca (freio) e laje de madeira (banco).")
                .ligados("utilidades", "companheiros").palavras("carroca carroça cavalo mula burro lhama camelo carga baú puxar laço").pronto();
        t("estradas", MUNDO, "Estradas", Material.DIRT_PATH)
                .texto("Grama muito pisada vira caminho de terra sozinha (montado conta o dobro, de carroça o triplo). Trilha que ninguém usa por 10 dias volta a ser grama.",
                        "Na estrada você anda mais rápido, a pé ou montado: trilha +5%, cascalho +8%, calçada de pedregulho ou tijolo +12%, Estrada Real de pedra polida ou lisa +15%.",
                        "Lajes e escadas contam como o bloco delas; ponte de madeira sobre água conta como cascalho. Pedra comum não conta.",
                        "Placa com [direção] e o nome de um lugar (colônia, Pedra de Viagem pública, estrutura que você visitou) mostra o rumo e a distância.",
                        "Placa com [estrada] e o nome, na beira do piso, é um marco. Marcos a até 160 blocos se ligam se dá para ir de um ao outro só pela estrada.",
                        "Estrada que passa por duas colônias: o Mercador vende mais, a felicidade sobe e saem caravanas com as sobras, que voltam com esmeraldas.")
                .dica("Estrada iluminada espanta os salteadores. Fogueira de Acampamento na beira da estrada é um Pouso de Viajante.")
                .ligados("carroca", "territorio", "colonia").palavras("estrada trilha caminho terra cascalho calcada pedra polida rapido velocidade cavalo placa direcao marco caravana salteador pouso ponte").pronto();
        t("mundovivo", MUNDO, "Mundo vivo", Material.HAY_BLOCK)
                .texto("Rotina: o Comedouro é o celeiro. No fim da tarde toca um sino e os bichos da fazenda vão para perto dele; de manhã saem para o pasto.",
                        "Bandos de cavalos, cabras, raposas, camelos, lhamas, papagaios ou lobos atravessam o mapa. Siga: eles vão para uma estrutura que você não conhece, e o líder vira raro.",
                        "Nas estradas com marcos: visitas de outra colônia, peregrinos com notícias, mensageiros com pedidos e mercadores de estrada.")
                .dica("Lobos ao longe na lua cheia, sapos no pântano e o sino da prefeitura ao amanhecer.")
                .ligados("estradas", "colonia", "mapasregiao").palavras("rotina animais celeiro comedouro bando manada migracao lobos sapos sino peregrino mensageiro visita noticias").pronto();
        t("mapasregiao", MUNDO, "Mapas da Região", Material.FILLED_MAP)
                .texto("Mesa do Cartógrafo + mapa vazio: Mapa da Região. Agachado com 4 ou 9 mapas: Mural para a parede. Com bússola (e 1 mapa vazio no inventário): Mapa do Explorador.",
                        "Por cima aparecem as estradas com nome, as colônias, as Pedras de Viagem públicas e as caravanas andando.",
                        "O Mapa do Explorador se pinta sozinho por onde você anda com ele no inventário e marca as estruturas que você visitou.")
                .dica("O Cartógrafo da colônia desenha mapas com os mapas vazios do depósito; às vezes um X numa estrutura que você não conhece.")
                .ligados("estradas", "colonia", "mundovivo").palavras("mapa regiao mural parede explorador cartografo mesa bussola caravana").pronto();
        t("vanilla", MUNDO, "Vanilla+ e artefatos", Material.OAK_SAPLING)
                .texto("Detalhes com cara de jogo normal: mudas que se plantam sozinhas, folhas que soltam graveto, cabeças de monstro, placa que mostra o baú, peixes pulando e gelo fino.",
                        "Abelhas polinizam: colmeia ou ninho com abelhas a até 8 blocos faz a plantação crescer mais rápido.",
                        "Estante viva: olhando um livro na estante entalhada, a barra mostra o título; agachado + clique lê sem tirar.",
                        "Bigorna sem \"Caro demais\" (para em 30 níveis); bloco de ferro conserta a bigorna lascada. Rebolo: livro de vários encantamentos + livro comum separa um encantamento.",
                        "Agachado, frasco vazio na mesa de encantamento: guarda XP em garrafa. Tesoura na moldura com item: moldura invisível. Mão vazia na porta: bate.",
                        "Artefatos de bancada: Garras de Escalada, Corda de Rapel, Pêndulo do Rabdomante, Cantil e Bolsa de Sementes.")
                .ligados("minerais", "coletas").palavras("garras escalada corda rapel pendulo rabdomante cantil bolsa sementes muda graveto gelo bigorna caro demais rebolo separar encantamento xp garrafa frasco moldura invisivel tesoura bater porta").pronto();
        t("estruturas", MUNDO, "Estruturas", Material.MOSSY_STONE_BRICKS)
                .texto("18 tipos: 16 em terras novas (torres, minas, fortins, oásis, faróis, vilas saqueadas...) e 2 no mar (naufrágios e cidades submersas).",
                        "Alguns têm guardas, outros têm moradores que negociam.")
                .dica("Poço dos Desejos: jogue 1 esmeralda. Farol: acenda com um bloco de pedra luminosa.")
                .ligados("mar", "enciclopedia", "mapas", "locais").palavras("torre mina cemiterio poco eremita bruxa farol fortim oasis circulo").pronto();
        t("sonhos", MUNDO, "Sonhos", Material.RED_BED)
                .texto("Durma a noite inteira: às vezes você sonha com um lugar que ainda não conhece.",
                        "Pode ser uma estrutura, um naufrágio, uma cidade submersa ou até um Local Oculto.",
                        "Ao acordar, você sabe para que lado fica. Um sonho por dia, nunca o mesmo lugar.")
                .dica("Os sonhos ficam anotados no seu /diario.")
                .ligados("estruturas", "locais", "mar").palavras("sonho dormir cama pista lugar").pronto();
        t("mar", MUNDO, "Naufrágios e a cidade submersa", Material.PRISMARINE_BRICKS)
                .texto("Naufrágios no fundo do mar: cada navio tem nome, carga e o diário do capitão (às vezes ele aponta para uma cidade submersa).",
                        "Cidade Submersa: rara, no mar profundo. Templo de Maris, casas, arquivo com a história, cofre e guardas afogados.",
                        "No santuário do templo acorda o Sumo-Sacerdote. Ele guarda o Coração da cidade (um condutor).")
                .dica("Ponha o Coração bem no meio da moldura do altar: a cidade desperta. Leve poção de respiração.")
                .ligados("estruturas", "deuses", "pesca", "maraberto").palavras("naufragio navio cidade submersa mar oceano templo maris condutor coracao sacerdote").pronto();
        t("barco", MUNDO, "O Barco Grande e as melhorias", Material.SPRUCE_BOAT)
                .texto("Barco Grande (7 tábuas + 2 barcos): quem pilota e 4 bancos. Cada banco vira assento, mastro (da vela), baú ou lugar de bicho.",
                        "Baú de convés: clique um baú comum no barco (até 2). Clique no baú para abrir; ao recolher o barco, o que está dentro vai junto.",
                        "Bicho a bordo: segure o bicho no laço e clique no barco. Casco Reforçado (ferro e tábuas), Rede de Pesca (linha e vara), Âncora (corrente, ferro e bloco de ferro): clique o item no barco.",
                        "A rede pesca sozinha enquanto você navega e guarda no baú. A âncora baixada deixa o barco parado. Um estandarte clicado vira o pano da vela.",
                        "O casco tem vida: Kraken, canhões, raios e ondas tiram. Tábuas consertam (+4). Chegou a zero, o barco afunda e tudo boia.")
                .dica("Agachado + mão vazia no barco: o menu (âncora, rede, baús, bichos, casco e recolher).")
                .ligados("maraberto", "perigosmar", "integracao").palavras("barco grande vela bau conves rede pesca ancora casco reforcado estandarte bicho lobo afundar consertar tabua").pronto();
        t("maraberto", MUNDO, "O mar aberto", Material.HEART_OF_THE_SEA)
                .texto("Longe da costa: ilhotas (o náufrago, o esconderijo pirata com o X na areia, a ilha das aves), icebergs, barcos mercantes e a Ilha-Tartaruga.",
                        "Navio Pirata ancorado: os canhões atiram em quem chega perto. Derrube o capitão: os canhões calam e ele deixa um mapa do tesouro.",
                        "Ilha-Tartaruga: tem tesouro no alto, mas cavar ou acender fogo nela acorda a Tartaruga Ancestral.",
                        "Cardumes: água fervendo de peixe no mar aberto. Pescar ali dá mais peixe raro e em dobro; a rede do barco pega muito mais. Baleias acompanham os grandes.",
                        "Carta Náutica (do náufrago, do mercador do mar e dos baús): clique com ela e ela marca o lugar do mar mais perto que você ainda não viu.")
                .dica("Leve comida: o náufrago troca as cartas do barco dele por qualquer coisa de comer.")
                .ligados("barco", "perigosmar", "mar", "pesca").palavras("ilhota naufrago pirata navio canhao capitao iceberg mercador tartaruga cardume baleia carta nautica tesouro").pronto();
        t("gigantes", MUNDO, "Estruturas grandes", Material.OAK_SAPLING)
                .texto("Seis construções gigantes que nascem com o mundo, só em terreno novo (longe do que já foi explorado).",
                        "Árvore-Mundo (florestas): tronco oco com escada, vila na copa e o Guardião das Raízes lá embaixo. Colosso Caído (planícies): estátua deitada com o Coração no peito e a Sentinela.",
                        "Cidade Anã (montanhas): a casa do portão em cima e uma escada até o salão do Rei da Montanha, com forja, mina e tesouro. Ossada do Dragão (badlands e deserto): areia suspeita para o pincel e, à noite, o Espírito do Dragão.",
                        "Coliseu: lute contra os spawners de desafio da arena, pegue a chave e abra o cofre do camarote. Castelo do Rei Caído: cavaleiros no pátio e o rei no trono.")
                .dica("Cada uma dá um título: Amigo das Raízes, Quebra-Colosso, Regicida da Montanha, Caçador de Dragões, Campeão do Coliseu e Algoz do Rei Caído.")
                .ligados("estruturas", "titulos").palavras("arvore mundo colosso caido cidade ana anao ossada dragao coliseu arena castelo rei caido escavacao pincel trono chefe gigante").pronto();
        t("luas", MUNDO, "O Foguete e as Luas", Material.FIREWORK_ROCKET)
                .texto("Junte as cinco peças do Abismo (Planta, Casco, Núcleo, Tanque e Motor) na bancada: sai o Foguete dos Antigos. Clique no chão a céu aberto e ele fica de pé.",
                        "Clique no foguete: Lançar (contagem, decolagem e pouso na lua) ou Recolher. Na lua, clique de novo: Voltar para casa. Quem estiver a até 6 blocos vai junto.",
                        "Pode ter quantos foguetes quiser. Morreu na lua? O foguete fica lá esperando: faça outro e vá buscá-lo.",
                        "Na Lua Prateada a gravidade é baixa e não há ar: sem o Capacete Lunar (vidro e Cristal Abissal, formato de capacete) o fôlego dura 30 segundos.",
                        "Prata Lunar faz o conjunto Lunar (forte contra mortos-vivos). As Colônias Lunares dos Antigos guardam o capítulo VI do Diário.",
                        "O foguete vai para a lua do céu da hora: Escura na lua nova, Vermelha na Lua de Sangue, de Cristal na Lua Azul e Sombria no eclipse; no resto, a Prateada. Cada uma tem um mineral, um conjunto e as suas criaturas.",
                        "A Lua Sombria fecha 10 minutos depois que você chega: quem ficar volta à força.")
                .dica("Na lua, a queda de até 10 blocos não machuca: pule à vontade.")
                .ligados("abismo", "achados").palavras("foguete lua prateada escura vermelha cristal sombria eclipse sangue azul nova lancar pousar voltar capacete lunar ar oxigenio gravidade prata onix rubi celeste fragmento colonia astronauta viajante").pronto();
        t("almas", MUNDO, "O Mundo das Almas", Material.SOUL_LANTERN)
                .texto("Lanterna das Almas (lanterna das almas e 4 areias das almas em cruz) na mão esquerda e Punhal do Sacrifício (Quartzo Negro, lingote de ferro e graveto, em coluna) na direita. Agache e clique: o ritual.",
                        "O corpo fica deitado onde você estava, com tudo o que você carrega. A alma chega sem nada, só com a lanterna.",
                        "Lá a alma corre e pula mais, não sente fome e não constrói nem quebra. A chama da lanterna dura 15 minutos.",
                        "Para voltar: agache e clique com a lanterna. Você traz o que achou. Se a chama apagar ou você morrer lá, volta sem os achados.",
                        "Você chega num cais. Clique nas Almas Penadas com a lanterna para recolher; com 3, o Barqueiro leva até a Cidade dos Antigos.",
                        "Na cidade: o Diário XI a XIV nos atris, baús com a Lâmina e o Manto Espectral, e o sino do Tribunal, que acorda o Juiz. Vencido, a Ilen entrega o capítulo XV.",
                        "Essência Etérea (das criaturas, das almas e do Juiz) faz o conjunto Etéreo: cair devagar e, com 4 peças, sobreviver a um golpe mortal a cada 10 minutos.")
                .dica("Ninguém fica preso: se o servidor cair ou você sair do jogo, o corpo e tudo dele voltam.")
                .ligados("luas", "abismo").palavras("almas mundo lanterna punhal sacrificio ritual corpo alma chama morte espirito barqueiro antigos penada cidade juiz sino ilen tribunal eterea etereo espectral lamina manto cais").pronto();
        t("abismo", MUNDO, "O Abismo", Material.CRYING_OBSIDIAN)
                .texto("Uma dimensão nova: um planalto cortado por rachaduras com mais de 1.100 blocos de queda, em 5 camadas (Borda dos Ventos, Floresta Pendurada, Campos de Cristal, Mar de Fungos e O Fundo).",
                        "Entrada: pule numa Fenda do Abismo, uma rachadura rara no mundo normal com ametista e obsidiana chorona. A queda não machuca.",
                        "Saída: agache 2 segundos em cima do seu Rasgo de Volta, onde você chegou. Pedras de Viagem também funcionam lá.",
                        "Morrendo lá, você renasce em casa e os itens ficam num túmulo, lá dentro. Cama não funciona no Abismo.",
                        "Carga Abissal (bancada: ametista em cima, obsidiana chorona, TNT e obsidiana chorona no meio, olho do ender embaixo): clique no chão e se afaste. Ela abre uma Fenda do Abismo ali.",
                        "Ruínas dos Antigos nas paredes das rachaduras: Posto, Elevador, Laboratório e Refúgio, uma por camada, e o Hangar no Fundo. Cada uma tem um capítulo do Diário e uma peça do foguete; o Motor fica com o Vigia do Fundo.",
                        "Minerais: Cobre dos Antigos (camadas 1 e 2), Cristal Abissal (3), Esporo Fóssil (4) e Ferro Negro (5). Cada um faz um conjunto de armadura e ferramentas na bancada, como o ferro.")
                .dica("Leve um planador, Saltador nas botas e muitos blocos: subir de volta é com você.")
                .ligados("achados", "gigantes").palavras("abismo dimensao fenda rasgo volta camada borda ventos floresta pendurada cristal fungos fundo queda carga abissal cratera antigos ruina diario foguete peca motor vigia cobre antigo esporo fossil ferro negro").pronto();
        t("telas", MUNDO, "Menu rápido e conversas", Material.WRITABLE_BOOK)
                .texto("A tecla de Ações Rápidas do jogo (Opções, Controles) e o botão novo no menu de pausa abrem o Menu do RPG, com atalhos para os comandos principais.",
                        "O náufrago, o Cronista e o mensageiro da estrada conversam numa janela com botões: dar comida, entregar o capítulo, entregar o pedido.",
                        "Renascer pede confirmação numa janela antes de zerar os atributos.")
                .dica("Nos botões de entregar, o item sai da mochila: não precisa estar na mão.")
                .ligados("achados", "integracao").palavras("menu rapido acoes rapidas pausa tela conversa dialogo botao naufrago cronista mensageiro renascer").pronto();
        t("achados", MUNDO, "Achados pelo caminho", Material.BRUSH)
                .texto("Oito estruturas pequenas nascem com o mundo, só em terreno novo: acampamento de caçadores, estátua quebrada, arco antigo, santuário do rio, cadafalso, cripta do poço, caravana perdida e sítio de escavação.",
                        "Estátua, arco, cripta e o sítio de escavação têm areia ou cascalho suspeito: use o pincel do jogo.",
                        "Da escavação saem coisas do jogo e do RPG: relíquias, tomos, páginas de lenda, mapas rasgados e cartas.")
                .dica("O Sítio de Escavação tem o fundo da vala inteiro para escavar. Leve mais de um pincel.")
                .ligados("gigantes", "estruturas", "arqueologia").palavras("achado caminho acampamento cacador estatua arco santuario rio cadafalso forca cripta poco caravana sitio escavacao pincel arqueologia").pronto();
        t("perigosmar", MUNDO, "Perigos do mar", Material.INK_SAC)
                .texto("Kraken: no mar fundo, à noite ou na tempestade, ele cerca o barco. Corte os 4 tentáculos e o corpo sobe; aí ele apanha (cuidado com a tinta).",
                        "Navio Fantasma: em noite de lua nova ou de neblina, surge perto de quem navega, com marujos fantasmas e o Capitão Fantasma. Some ao amanhecer.",
                        "Tempestade no mar: na trovoada, ondas empurram o barco, raios caem por perto e destroços de outros navios boiam com carga.")
                .dica("O Casco Reforçado leva metade do dano. Leve tábuas para consertar no meio do mar.")
                .ligados("barco", "maraberto", "ceu").palavras("kraken tentaculo navio fantasma capitao lua nova neblina tempestade trovoada raio onda destrocos").pronto();
        t("ceu", MUNDO, "Ilhas do Céu", Material.FEATHER)
                .texto("Arquipélagos raros lá no alto: Jardim Suspenso, Observatório Antigo, Forja dos Ventos ou Ninho do Guardião.",
                        "No chão, um pouco ao lado da ilha, fica a Plataforma de Vento: pise na placa e ela te lança até lá.",
                        "Não há volta segura. Leve Capa Planadora, Balão de Cristal ou élitro, ou coma uma Fruta Celeste (Queda Lenta).",
                        "No Ninho mora o Guardião dos Ventos: ele deixa Fragmentos Celestes (Amuleto dos Ventos, na Bancada Alquímica).")
                .dica("Balão de Cristal: 4 lãs brancas + 4 linhas na Bancada Alquímica. Mais lento que a capa e estoura em 5 usos.")
                .ligados("estruturas", "profundezas", "acessorios").palavras("ilha ceu nuvem plataforma vento lancar balao capa fruta celeste guardiao ninho fragmento").pronto();
        t("nether", MUNDO, "O Nether", Material.CRYING_OBSIDIAN)
                .texto("Forja dos Piglins: nas florestas carmesim e nos ermos. O Ferreiro Piglin vende peças do Nether, compra minerais e ensina a técnica da Lança de Basalto. Tudo em ouro.",
                        "Primeiro Portal em Ruínas: uma moldura gigante partida. Leia o púlpito e toque na pedra que chora no meio do portal: ela guarda a Chave do Primeiro Portal (uma por jogador).",
                        "Minerais: Cristal de Magma (deltas de basalto e ermos), Âmbar Infernal (florestas) e Quartzo Negro (vale das almas). Servem para incrustar, para a Lança de Basalto e para a Vara Ígnea.",
                        "Pesca na lava: com a Vara Ígnea a boia não afunda. Recolha logo depois do estalo da lava. Os peixes da lava viram pratos do Nether na Cozinha.")
                .dica("O Espeto de Hoglin acalma os piglins e o Caldo Distorcido afasta hoglins e brutos.")
                .ligados("armas_minerios", "tecnicas", "pesca", "cozinha", "minerais").palavras("nether inferno piglin forja portal obsidiana chave magma ambar quartzo lava vara ignea").pronto();
        t("profundezas", MUNDO, "Profundezas", Material.SHROOMLIGHT)
                .texto("Abaixo do y 0 o mundo fica escuro de verdade (Visão Noturna ajuda).",
                        "Raízes de Luz adormecidas brilham de leve nas cavernas fundas: clique no bulbo e ela ilumina a área para sempre.",
                        "Criaturas da Penumbra roubam corações máximos. Eles voltam ao sol, perto de uma raiz acesa ou com o Elixir da Purificação.",
                        "Às vezes, acender uma raiz acorda o Coração da Penumbra.")
                .dica("Arremesse uma Semente Brilhante (das raízes) para iluminar um canto por 2 minutos.")
                .ligados("ceu", "mitrilo", "alquimia").palavras("profundeza escuro escuridao raiz luz penumbra coracao semente brilhante caverna").pronto();
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
                .texto("Moradores de verdade trabalham para você no seu território inteiro, guardando o que produzem nos baús de depósito.",
                        "Projetos de vila (2.90): sobrado, oficina, fazenda, poço, torre de vigia, cabanas, armazém, biblioteca, quartel, mina, mercado, taverna, ferraria, estábulo e laboratório, com enxaimel, telhado de duas águas, chaminés e interior mobiliado. A maioria já sai pronta no nível II.")
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
        t("estabulo", COMUNIDADE, "Estábulo", Material.SADDLE)
                .texto("Guarda até 4 montarias: elas saem do mundo e voltam iguais (nome, linhagem, sela, armadura e componentes).",
                        "Clique: guardar a montaria em que você está (ou a sua mais perto) e chamar as guardadas. Shift + clique numa guardada mostra a ficha dela.")
                .ritual(Material.HAY_BLOCK, "Fardo de feno", Material.SADDLE, 1, Material.OAK_FENCE_GATE, 1)
                .dica("Laço + sela no fardo de feno é o Altar do Domador; sela + porteira é o Estábulo.")
                .ligados("linhagens", "companheiros").palavras("estabulo cavalo montaria guardar chamar").pronto();
        t("linhagens", COMUNIDADE, "Cavalos e linhagens", Material.GOLDEN_CARROT)
                .texto("Cavalos, burros, mulas, lhamas e camelos guardam pai, mãe, avós e geração. O filhote às vezes sai melhor que os pais.",
                        "Na 5ª geração a família ganha um nome de linhagem. O Livro do Criador (livro + cenoura dourada + couro) mostra a ficha e a árvore da família.")
                .dica("No Livro, as setas ↑ ↓ dizem se o filhote ficou melhor que o melhor dos pais.")
                .ligados("estabulo", "companheiros", "criacao").palavras("linhagem livro criador cavalo cruzar arvore familia pai mae avo").pronto();
        t("guardioes", COMUNIDADE, "Montarias lendárias (Guardiões)", Material.DRAGON_EGG)
                .texto("Qilin, Fenghuang, Longma, Baihu e Xuanwu: cada um é único no servidor.",
                        "No Altar do Domador, a Fusão Lendária junta dois companheiros no estágio III e itens raros num Ovo Lendário, que choca no lugar certo.")
                .ligados("companheiros", "linhagens").palavras("lendaria guardiao qilin fenghuang longma baihu xuanwu ovo fusao").pronto();

        // ===================== Coleções =====================
        t("album", COLECOES, "Álbum de Cartas", Material.FILLED_MAP)
                .texto("Monstros, chefes e peixes raros às vezes deixam uma carta. Página completa = bônus para sempre.")
                .cmd("album").ligados("bestiario").palavras("carta brilhante").pronto();
        t("duelo", COLECOES, "Duelo de Cartas", Material.ITEM_FRAME)
                .texto("Jogo de cartas com as cartas do álbum: 3 trilhas, 20 de vida para cada lado e energia que cresce até 6.",
                        "Jogue contra os moradores das vilas (cada um com seu baralho; vitória dá esmeraldas e pacotes) ou contra jogadores com /duelo.")
                .cmd("duelo").ligados("album", "vilas").palavras("cartas duelo baralho trilha energia").pronto();
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
