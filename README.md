# RPGAtributos

Plugin para **Paper 26.3** (Minecraft 26.3, Java 25) que transforma o servidor num RPG estilo Overgeared:
- **13 atributos** que evoluem com o uso (até o nível 100);
- **forja de itens** com raridade, refino e conjuntos, e **reciclagem** de itens;
- **infusão de itens no corpo** e **magias** criadas pelo jogador;
- **Altar Ritualístico** com chefes e eventos;
- **plantações** com qualidade e sementes raras, e uma **Cozinha** com pratos que dão buffs;
- **party** (grupo) e **territórios** com proteção própria (não precisa de WorldGuard nem GriefPrevention);
- **domador**: companheiros com ordens (patrulhar, guardar...) e componentes (sela, alforje, asas...), inclusive montarias voadoras;
- **masmorras aleatórias** em 4 dificuldades, com salas geradas pelo plugin e salas construídas por você;
- **classes** numa árvore com pré-requisitos, provas de mudança de classe e habilidades ativas;
- **lendas**: itens **Especiais**, únicos no servidor, que nascem dos feitos (escondidos) do jogador e despertam com o uso;
- **Locais Ocultos** gerados pelo mundo, com enigmas, guardiões e **classes lendárias** (um dono por vez), e **Pedras de Viagem**;
- **pesca** com peixes raros, tesouros e criaturas marinhas, e **alquimia** com elixires, **gemas** engastadas nos itens forjados e **acessórios** (anéis, amuletos, cintos, lanterna de bolso...);
- **guarda-roupa**: muda a aparência da armadura sem tirá-la;
- **portais** que se abrem pelo mundo e **transbordam** monstros se ninguém os fechar, a classe única **Soberano das Sombras** e a **Torre Infinita** com ranking semanal;
- **combos de arma**: 3 cliques com a arma na mão soltam golpes (42 no total), com Vigor e proficiência por tipo de arma;
- **fé e mistério**: seis deuses com santuários, oferendas e milagres, constelações observadas pela luneta, transmutação alquímica e runas gravadas nos equipamentos;
- **exploração**: **Ilhas do Céu** (lançado por uma Plataforma de Vento) e as **Profundezas** escuras com Raízes de Luz e a Penumbra, Enciclopédia do Mundo, o minério **mitrilo** no fundo do mundo, **mapas do tesouro** com X de verdade, **arqueologia** com relíquias e a campanha **As Crônicas do Mundo** contada pelo Cronista;
- **progressão extra**: **talentos** em 4 árvores, **renascer** com bônus permanente (até ★★★★★) e **evolução dos companheiros** (Lobo → Lobo Alfa → Lobo das Sombras);
- **detalhes do dia a dia**: barco, carrinho, vara, élitro e armadura de cavalo forjados, Boneco de Treino, Pedra de Amolar, Fogueira de Acampamento (sentar e descansar), Bebedouro, troféus de chefe, túmulo ao morrer, animais raros, ninhos, recordes e o Diário de Viagem;
- **vida no mundo**: Barril de Envelhecimento, apicultura, Canteiro de Ervas, Álbum de Cartas com Duelo de Cartas (contra moradores e jogadores), Mercador Itinerante, encontros na estrada, segredos, flechas especiais e frascos de arremesso;
- **conforto**: menu central `/rpg` e placar lateral opcional;
- **títulos** e **missões de aldeões**.

Tudo usa os atributos, efeitos, partículas e sons do próprio Minecraft: não precisa de mod. O plugin gera um **pacote de recursos opcional** com texturas próprias: mitrilo, Cajado Arcano, Gancho, Capa Planadora, Ferradura, Ninho de Pássaro, os 12 peixes raros, os 16 pratos, as variedades raras, os componentes alquímicos, os elixires, as gemas, os materiais raros, os núcleos dos chefes, os acessórios, 7 lendas e os itens novos (bebidas, cartas, flechas, frascos, méis e ervas).

**Guias em HTML:** [docs/jogadores.html](docs/jogadores.html) (tudo para os jogadores) e [docs/admin.html](docs/admin.html) (instalação, configuração, comandos de admin e testes).

## Atributos

| Atributo | Como ganha XP | No nível máximo (padrão) |
|---|---|---|
| ⛏ Mineração | Quebrar blocos de picareta (minério vale mais) | +100% velocidade de mineração, 50% de minério em dobro |
| ♣ Corte de Madeira | Quebrar troncos | +100% velocidade no tronco, 50% de tronco em dobro |
| » Corrida | Correr (sprint) | +20% velocidade |
| ⬆ Pulo e Queda | Pular e levar dano de queda | +30% pulo, +5 blocos de queda segura, -50% dano de queda |
| ⚔ Combate | Matar mobs, jogadores e chefes | +3 de dano, +5 corações |
| ⚒ Ferraria | Forjar, refinar, melhorar para Netherite, fundir lingotes | Itens forjados mais raros e mais fortes |
| ✦ Arcano | Infundir itens, lançar magias, descobrir segredos | +150 de mana, mais carga, partes do corpo e espaços de magia |
| ≈ Natação | Nadar e andar debaixo d'água (com tecla de movimento apertada) | +50% agilidade na água, +3 de fôlego, +60% mineração submersa |
| ☘ Agricultura | Colher plantações maduras, melancias e abóboras naturais, frutas | 50% de colheita dupla, colheitas de mais qualidade, toque verde, colheita gigante |
| ♨ Culinária | Cozinhar pratos na Cozinha | Libera receitas, pratos de mais qualidade, buffs +50% mais longos |
| ♞ Doma | Domar e cruzar animais, vincular/invocar companheiros, colocar componentes, andar montado, companheiro derrotar monstros | 5 companheiros, 6 componentes em cada, +100% vida e +50% dano deles, +40% velocidade montado |
| ⚓ Pesca | Pescar (peixes raros, tesouros e criaturas marinhas valem mais) | Isca 50% mais rápida, 30% de peixe em dobro, 15% de peixe raro, mais tesouros |
| ⚗ Alquimia | Criar na Bancada Alquímica, engastar gemas, fazer poções no suporte de poções | Elixires 60% mais longos e de mais qualidade, 40% de render o dobro de componentes |

**Nível máximo 100, balanceado:** no config os bônus são escritos como "quanto valem no nível máximo", e o plugin divide pelos níveis sozinho. Mudar o nível máximo não deixa ninguém forte demais. Chegar no 50 leva cerca de 20% do caminho; o 100 é para quem joga muito.

**Anti-farm:**
- Blocos colocados por jogadores e pedra de gerador não dão XP (a marca fica salva no chunk e acompanha o bloco empurrado por pistão).
- Pular parado no lugar não dá XP.
- Ser levado pela correnteza sem apertar nada não dá XP de Natação.
- Melancia e abóbora colocadas por jogadores não dão XP de Agricultura; plantação verde também não.
- Mobs de spawner dão só 25% do XP.
- Matar o mesmo jogador seguidas vezes, ou uma conta do mesmo IP, não dá XP.
- Material raro não pode ser gasto em receitas comuns (bancada, bigorna, fornalha, poções).

## Interface: /rpg, Guia e Jornada

- **`/rpg`:** o menu central. No topo, **"Seu próximo passo"**: 3 sugestões que mudam conforme o progresso (o passo da Jornada, pontos de talento para gastar, colônia ou reino que já dá para fundar, deus para seguir...). Abaixo, **7 categorias** (Personagem, Combate, Magia e fé, Ofícios, Mundo e aventura, Comunidade, Coleções) e atalhos para os menus mais usados.
- **Guia interativo:** cada assunto é uma tela curta com o **ritual desenhado em itens** (o bloco, os itens que você joga em cima e o que ele vira), uma dica, o botão para abrir o sistema e os assuntos ligados. Clique numa categoria do `/rpg` e depois num assunto.
- **Busca:** `/guia <palavra>` (ex.: `/guia reciclar`, `/guia alquimia`) mostra os assuntos no chat, para clicar.
- **Manual em janela (2.59):** `/manual` (ou `/wiki`, ou o botão 📖 no `/rpg`) abre o Manual numa **janela nativa do jogo** (sem mod): tela inicial com as 7 áreas, um campo de busca e as novidades; cada área lista os assuntos (passe o mouse para ver do que se trata); cada assunto mostra o texto, o ritual, a dica, os assuntos ligados, um botão que abre o sistema e outro que abre o **site do Manual no navegador**. `/manual <palavra>` busca direto.
- **Link do site:** `/manual link` manda no chat um link clicável (o jogo pergunta antes de abrir), e o Manual também aparece no menu de pausa: **Esc → Links do servidor**. O endereço fica em `manual.url` no config.
- **Dicas na hora certa:** na primeira vez que você segura, pega ou encontra algo novo (uma arma do arsenal, um mineral, uma arma com nome, o Livro do Criador, uma semente da estação, uma pele, um cavalo, as Profundezas, o céu...), aparece uma dica curta no chat com **[ver como]**, que abre o assunto. Cada dica aparece uma vez só; `/guia dicas` desliga ou liga.
- **O que é isso?:** com um item na mão, o botão no `/rpg` ou `/guia mão` abre o assunto daquele item.
- **Novidades da versão:** botão no `/rpg` (ou `/guia novidades`) com o que entrou na última versão, cada coisa levando ao assunto dela. Depois de uma atualização, quem entra recebe um aviso clicável.
- **Jornada do Aventureiro** (`/jornada`): 12 passos que ensinam o básico na ordem (subir um atributo, forjar, soltar combos, cozinhar, alquimia, classe, magias, território, fé, estrutura, masmorra e colônia). Cada passo se completa sozinho e dá uma recompensa.
- **Visual:** com o pacote de recursos, os menus do `/rpg`, do Guia e da Jornada têm **fundo desenhado** (papel e madeira); sem o pacote, aparecem com vidros.
- **Livro:** na primeira entrada o jogador ainda recebe o livro Guia do Aventureiro (80 páginas); `/guia livro` dá outro.

## Forja do Ferreiro

**Ritual:** jogue (tecla Q) **2 blocos de ferro** e **1 balde de lava** em cima de uma **bigorna**. Ela vira a Forja do Ferreiro (o balde volta vazio). A bigorna da forja não cai; quebrar a forja devolve os blocos de ferro.
- **Clique direito:** bancada do ferreiro. Armas, ferramentas, armaduras, arcos, bestas, maças, lanças e escudos feitos nela saem **forjados**. Na bancada comum saem normais (dá para mudar no config: `forja.so-na-forja-do-ferreiro`).
- **Agachado + clique:** tela de **refino**.

| Raridade | Bônus | Efeitos | Força | Chance no nível 0 → 100 de Ferraria |
|---|---|---|---|---|
| ★ Comum | 1 | 0 | ×1,0 | 70% → 25% |
| ★★ Raro | 2 | 0 | ×1,3 | 24% → 35% |
| ★★★ Épico | 3 | 1 | ×1,7 | 5% → 22% |
| ★★★★ Único | 3 | 1 | ×2,1 | 1% → 11% |
| ★★★★★ Lendário | 4 | 2 | ×2,6 | 0% → 5% |
| ★★★★★★ Mítico | 5 | 3 | ×3,3 | 0% → 1,5% |

**Bônus:** dano, velocidade de ataque, alcance, armadura, vida, velocidade, pulo, mineração e outros, além dos especiais: crítico, roubo de vida, dano de flecha, esquiva, espinhos e chance de não gastar durabilidade.
**Efeitos (Épico+):** passivos (Força, Velocidade, Visão noturna...), ao acertar (Lentidão, Veneno, Chamas, Congelar...), ao ser atingido (Absorção, Rajada de vento...) e ao minerar (Frenesi, Fundição...). Raio e Execução são exclusivos dos Míticos.

**Refino +1 a +10:** cada nível dá +6% nos bônus da forja e +4% no dano/armadura base.

| Nível | Custo | Chance |
|---|---|---|
| +1 a +3 | lingotes de ferro e ouro | 100% → 95% |
| +4 a +6 | diamantes (+6 pede 1 Fragmento de Forja) | 85% → 65% |
| +7 a +9 | lingotes de netherite e Fragmentos de Forja | 50% → 30% |
| +10 | 4 netherite, 8 Fragmentos e 1 Essência Primordial | 20% |

Até +5, falhar só gasta o material. De +6 a +8, o item perde 1 nível. No +9 e +10, perde nível ou **quebra** (1 em 3). A **Pedra de Proteção** evita as duas coisas. Ferraria alta soma até +10% de chance.

**Conjuntos:** vestindo as 4 peças de armadura forjadas, a raridade mais baixa entre elas dá um bônus extra. Vai de +1 de armadura (Comum) até +5 de armadura, +4 corações, +10% de velocidade, Regeneração e Resistência (Mítico).

**Mesa de ferraria:** subir um item forjado para Netherite mantém a raridade, em qualquer mesa. Item achado no mundo é forjado ao ser melhorado numa mesa a até 6 blocos de uma Forja do Ferreiro.
**Armas com nome:** às vezes a peça já nasce com **nome, história e visual próprios**: 2% das Comuns forjadas, 5% das Raras, 10% das Épicas, 18% das Únicas, 30% das Lendárias e 45% das Míticas (`forja.armas-com-nome`). Elas também aparecem em **5% dos baús** de estruturas (do plugin e do jogo) e com os chefes (Altar, Sumo-Sacerdote, Guardião dos Ventos e Coração da Penumbra), já forjadas. São 66: **espadas** (Presa do Inverno, Lâmina de Thalassa, Aurora de Aerília, Juramento do Fortim, Brasa dos Anões, Sussurro da Penumbra, Lâmina da Raiz de Luz, Fio do Guardião dos Ventos, Promessa do Primeiro Colono, Lâmina do Eclipse), **machados e maças** (Talha-Raízes, Fúria do Saqueador, Machado do Faroleiro, Machado do Velho Lenhador, Mordida da Geada, Sino de Maris, Martelo do Eremita, Martelo do Mestre de Obras, Coração Roubado), **arcos e bestas** (Arco do Caçador Silencioso, Asa de Zéfira, Arco das Brasas, Arco das Folhas Caídas, Besta do Capitão, Besta Glacial, Besta da Torre de Vigia) e **ferramentas** (Picareta do Mineiro Perdido, Quebra-Cristais, Picareta das Profundezas, Picareta do Poço Fundo, Pá do Coveiro, Pá do Arqueólogo, Enxada da Colheita Dourada, Enxada das Quatro Estações, Enxada do Jardim Suspenso, Vara do Pescador do Gelo, Vara de Coral) e **lanças e tridentes** (Lança da Sentinela, Chifre do Qilin, Agulha dos Ventos, Pique do Cavaleiro Errante, Lâmina do Crescente Verde, Espeto do Bastião; Tridente de Maris, Garfo do Afogado-Rei, Tridente do Pescador de Pérolas, Tridente da Tempestade) e **o arsenal** (adagas Presas Gêmeas e Beijo da Víbora, faca Pena do Corvo, Machadinha do Velho Anão, foices Foice do Ceifador de Trigo e Lua Minguante, bastões Bastão do Monge da Montanha e Bordão do Peregrino, chicotes Chicote do Domador de Feras e Língua de Brasa; uma adaga forjada só pode virar uma adaga com nome, e assim por diante) e **as armas dos minérios** (Coração da Montanha e Lâmina do Rei Gigante no Montante, Retorno do Escorpião no Bumerangue, Asas de Jade nos Leques, Presa do Kraken e Arpão do Velho Baleeiro no Arpão, Estrela da Geada no Mangual, Presas do Lobisomem nas Garras, Tocha do Pântano na Clava). **Só muda a aparência:** a força continua sendo a da raridade. Com o pacote de recursos, no inventário aparece o desenho delas e na mão, no chão, no suporte e na moldura um **modelo 3D** (espadas, machados, maças, ferramentas, lanças e tridentes; o tridente arremessado continua com a cara do jogo; os arcos e as bestas mudam de desenho conforme a corda é puxada). O nome e a história ficam ao refinar e reforjar.

**Trajes (armaduras com nome):** a armadura forjada também pode nascer com nome (mesmas chances; 30% das armas com nome de baús e chefes são peças de Traje). São **8 Trajes** de 4 peças, cada um com história, ícone próprio e **textura própria no corpo de quem veste**, pintada pelo plugin: **da Penumbra** (escamas roxas), **dos Ventos** (placas brancas e douradas), **de Thalassa** (escamas verde-mar e coral), **do Fortim** (placas de aço e vermelho), **dos Anões** (bronze), **da Geada** (gelo), **da Colheita** (tecido de palha) e **da Raiz de Luz** (tecido de raiz com veios âmbar). As peças se chamam Elmo/Peitoral/Grevas/Botas (placas), Elmo/Cota/Grevas/Botas (escamas) ou Capuz/Gibão/Calças/Botas (tecido), por exemplo **Elmo da Penumbra** e **Gibão da Colheita**. Só aparência, como as armas; funcionam com o Guarda-roupa e continuam ao virar Netherite.

## Bancada de Reciclagem

**Ritual:** jogue **1 funil** e **1 tesoura** em cima de um **rebolo**. Clique nela, clique nos itens do seu inventário para pô-los na bancada (**até 9 de uma vez**; clique de novo para tirar) e em **"Reciclar tudo"**. O que ficar na bancada volta para você ao fechar.
- Recicla tudo o que não empilha e tem receita de bancada: armas, ferramentas, armaduras, arcos, escudos, carrinhos, barcos...
- Volta **50% do material** da receita (até **75%** com Ferraria no máximo). Item gasto devolve menos.
- **Reciclagem 2.0 (2.60):**
  - **O material principal vem primeiro:** a parte quebrada vira chance (a picareta de ferro dá 1 ferro garantido e 50% de mais 1). O graveto, a tábua e a linha não "roubam" o material.
  - **Item pouco gasto** (mais da metade da durabilidade) devolve **pelo menos 1 do material mais valioso**: a pá de ferro nova devolve 1 ferro (antes, só um graveto).
  - **Armas dos minérios** devolvem o mineral do plugin (a Lança de Basalto devolve Cristal de Magma; o Montante, Lingote de Mitrilo). Antes saía o item comum por baixo dele.
  - Peças **"✦ Mitrilo"** devolvem o Lingote de Mitrilo (com chance).
  - **Encantamentos devolvem XP**, como o rebolo do jogo, além da chance de livro.
  - **Confirmação:** com item valioso na bancada (forjado Épico ou melhor, com nome, com gema ou refino, ou Mitrilo), o botão pede um segundo clique.
- **Reciclagem automática (2.62):** um **funil em cima da bancada** joga os itens dentro dela, um a um (a cada 2 s), e o material vai para o **baú (ou barril, ou funil) embaixo do rebolo**; sem baú, cai no chão. Recicla a 50%, com 25% de chance de livro. Item valioso nunca é reciclado sozinho: fica no funil. Boa para fazendas de monstros (espadas e armaduras de zumbi).
- Netherite volta como diamante + sucata de netherite.
- Item encantado: 25% a 50% de chance de um **livro** com um dos encantos.
- Lendário ou Mítico de diamante/netherite: chance de **Fragmento de Forja** (15% / 50%). Refino +5 ou mais devolve 1 Fragmento garantido.
- Dá XP de Ferraria. Material raro, pratos e o grimório não são reciclados.

## Arcano: Infusor Corpóreo e magias

**Ritual:** caldeirão **com água** + jogue dentro **1 mesa de encantamento** e **1 bigorna**.

**Infundir:** clique no infusor, escolha um item do inventário e uma parte do corpo. O item é consumido, gasta XP e pode falhar.

| Parte | Bônus (por ponto de poder do item) | Libera no Arcano |
|---|---|---|
| Mente | +8 de mana máxima | 0 |
| Coração | +½ ❤ | 0 |
| Braços | +0,25 de dano | 20 |
| Pernas | +1,5% de velocidade | 40 |
| Sangue | +0,4 de mana por segundo | 70 |

**10 essências** em todos os itens do jogo (Fogo, Gelo, Vento, Água, Vida, Terra, Sombra, Vazio, Energia, Natureza).

**Limites:**
- Passar da carga do corpo causa **rejeição**.
- Ao morrer, as infusões ficam **instáveis** por 5 minutos.

**Infusões lendárias:** os **núcleos** que os chefes deixam cair podem ser infundidos e dão um poder único:

| Núcleo | Poder |
|---|---|
| Núcleo de Ferro Ancestral | Pele de Ferro: +4 armadura e resistência a empurrões |
| Presa da Rainha Aracnídea | Imune a veneno e envenena quem te ataca |
| Brasa Eterna | Imune a fogo e incendeia quem te ataca |
| Filactério do Lich | Imune a Definhamento e rouba 8% do dano que causa |
| Olho da Tempestade | **Pulo duplo** |
| Coroa do Arauto | +15% de dano e +4 corações |

**Grimório** (pegue no infusor por 1 livro):
- **Clique direito:** lança.
- **Clique esquerdo:** troca de magia.
- **Agachado + direito:** abre o grimório.

A mana aparece numa barra no topo da tela. Para **criar magias**, escolha de 1 a 4 essências do corpo, uma forma e até 2 modificadores. Todas as combinações funcionam.

**10 formas:**

| Forma | Como age |
|---|---|
| Toque, Projétil, Aura, Corpo, Criação | As originais: perto, à distância, em volta de você, em você mesmo, criando blocos |
| **Raio** | Feixe contínuo enquanto você **segura o clique** (gasta mana aos poucos) |
| **Sopro** | Cone na sua frente, como um dragão |
| **Chuva** | Cai do céu numa área até 30 blocos, por alguns segundos |
| **Invocação** | Um elemental luta ao seu lado (Vida = espírito que cura, Terra = golem, os outros = espíritos voadores) |
| **Encantar arma** | Os próximos golpes corpo a corpo carregam o elemento |

**10 modificadores** (até 2 por magia, liberados pelo nível de Arcano): **Rápida** (recarga pela metade, mais fraca), **Ampliar** (área maior), **Dividir** (3 projéteis), **Teleguiado** (persegue o inimigo), **Canalizar** (segure para carregar até o dobro da força), **Ricochete** (salta para 3 inimigos), **Persistente** (deixa uma zona no chão), **Mina** (armadilha que explode quando alguém chega perto), **Eco** (repete 1s depois) e **Potente** (mais forte, recarga maior).

**Reações elementais:** quem leva um elemento fica marcado por 6s; um segundo elemento causa uma reação. Funciona entre jogadores, então combos de party ficam muito fortes.

| Reação | Elementos | Efeito |
|---|---|---|
| Eletrocutado | Água + Energia | O choque salta para até 4 inimigos |
| Congelado | Água + Gelo | O alvo fica quase parado |
| Derreter | Fogo + Gelo | Muito dano |
| Incêndio | Fogo + Vento | O fogo se espalha |
| Queimada | Fogo + Natureza | Chamas fortes que pulam para outros |
| Lama | Terra + Água | Lentidão forte |
| Corrupção | Sombra + Vida | Dano, definhamento e você se cura |
| Tempestade | Vento + Energia | Um raio cai no alvo |
| Colapso | Vazio + Sombra | Puxa os inimigos para o alvo |
| Estilhaçar | Gelo + Terra | Quebra o gelo: muito dano |

**Maestria:** cada elemento sobe de nível (1 a 10) conforme você o usa, com +3% de força por nível. No nível 5 o elemento ganha uma **variante** (Chama Azul, Gelo Eterno, Vendaval, Maré Curativa, Bênção, Pele de Rocha, Abismo, Gravidade, Sobrecarga, Toxina), e no 10 as reações com ele ficam 50% mais fortes. Grimório → Maestria e reações.

**Estilo:** uma marca visual em todas as suas magias (Estelar, Almas, Cristal, Cerejeira, Brasas, Eco, Melodia). Grimório → Estilo.

**Cajado Arcano:** na **Forja do Ferreiro**, 1 ametista na ponta + 2 varas de blaze na diagonal. Sai com raridade e bônus próprios (potência, mana economizada, recarga menor), aceita refino e **lança magias como o grimório**. Gemas engastadas nele fortalecem elementos: Rubi = Fogo, Safira = Água e Gelo, Esmeralda = Natureza e Vida, Topázio = Energia e Vento, Ametista = Vazio, Ônix = Sombra e Terra.

**Pergaminhos:** na Bancada Alquímica (aba Pergaminhos), escreva uma magia do seu grimório num papel. Qualquer um pode lançá-la uma vez, sem mana nem essências.

**50 magias secretas** com efeito único. Alguns exemplos:

| Tipo | Magias |
|---|---|
| Pares | Bola de Fogo, Nevasca, Passo do Vazio, Drenar, Ponte de Gelo, Terremoto, Corrente Elétrica, Escudo de Gelo, Chama Sagrada, Retorno, Chamar a Chuva, Olho Vigilante |
| Trios | Fardo Fértil, Chamado da Tempestade, Fantasma, Meteoro, Matilha, Prisão de Gelo |
| Quartetos | Fênix, Buraco Negro, Gênese, Avatar Elemental, Santuário |
| Formas novas | Raio Solar, Dreno de Alma, Raio Congelante, Sopro do Dragão, Sopro Gélido, Nuvem Tóxica, Chuva Ácida, Tempestade Elétrica, Granizo, Golem de Pedra, Sombras Gêmeas, Espírito da Floresta, Lâmina Flamejante, Lâmina Vampírica, Lâmina do Trovão |
| **Proibidas** | Chuva de Meteoros, Zero Absoluto, Julgamento, Ruptura Dimensional, Renascer da Floresta |

Ninguém conta a receita, mas o grimório dá uma dica de cada (quantas essências e uma delas). As **proibidas** não podem ser criadas: vêm de **Tomos Proibidos**, que caem dos chefes do Altar, dos selos dos Locais Ocultos e dos chefes de masmorra Difícil/Pesadelo. Clicar numa magia secreta já descoberta coloca ela de volta no grimório.

## Altar Ritualístico

**Ritual:** em cima de uma **obsidiana chorosa**, jogue **1 olho do ender** e **4 velas**. Clique no altar para ver tudo o que ele invoca. Para invocar, jogue a oferenda em cima dele ou entregue pelo menu.

| Invocação | Oferenda | O que é |
|---|---|---|
| Golem Ancestral ★ | 4 blocos de ferro + 1 abóbora esculpida | Tremor, arremesso, escudo de ferro |
| Rainha Aracnídea ★ | 8 olhos de aranha + 16 linhas | Filhotes, teias, nuvem venenosa, saltos |
| Senhor das Chamas ★★ | 8 varas de blaze + 4 cremes de magma | Bolas de fogo, anel de fogo, meteoros |
| Lich ★★ | 1 crânio de esqueleto + 32 ossos | Invoca mortos, raio sombrio, teleporte, dreno de vida |
| Tempestade Viva ★★★ | 4 varas de breeze + 8 cargas de vento | Rajadas, raios, ciclone |
| Arauto do Fim ★★★★ | 1 estrela do Nether + 4 fragmentos de eco + 1 bafo de dragão | Golpe do vazio, vexes, onda sônica, escuridão |
| Lua de Sangue | 1 rosa do wither + 16 carnes podres (só à noite) | Até amanhecer: monstros mais fortes, dobro de XP e drops, ninguém dorme |
| Chuva de Meteoros | 8 cargas de fogo + 1 diamante | 90 s de meteoros que espalham minérios |
| Ondas de Monstros | 8 carnes podres, 8 ossos, 8 linhas, 8 pólvoras | 5 ondas + Campeão; tesouro no final |
| Mercador Arcano | 24 esmeraldas + 1 olho do ender | 10 min de trocas raras (Pedra de Proteção, Fragmentos...) |
| Caçador de Recompensas | 16 esmeraldas + 1 espada de ferro | Contratos para matar chefes, com prêmios grandes |

**Os chefes:**
- A vida aumenta com o número de jogadores perto.
- Têm barra de vida no topo da tela e entram em **fúria** abaixo de 30% de vida.
- São imunes a queda, lava e sufocamento e ficam presos perto do altar (sem trapaça).
- Somem se ninguém lutar por 1 minuto.
- Deixam o **núcleo** deles, **Fragmentos de Forja**, chance de **Pedra de Proteção** e de **Essência Primordial**, além de itens comuns. Quem ajudou ganha XP de Combate.

## Plantações

Colher plantação **madura** (trigo, cenoura, batata, beterraba, fungo do Nether, cacau), melancias e abóboras naturais e frutas dá XP de Agricultura.
- **Qualidade:** a colheita sai Normal, Boa ★, Ótima ★★ ou Perfeita ★★★ (mais chance com nível alto). Ingredientes de qualidade melhoram os pratos.
- **Colheita dupla** (até 50%), **toque verde** (plantas perto de você crescem sozinhas) e, raramente, **colheita gigante** (×8, com animação).
- **Adubo Rico** (bancada: 3 farinhas de osso + 1 carne podre + 1 terra = 4): clique numa plantação, a próxima colheita dela sai com uma qualidade a mais.
- **Espantalho:** um suporte de armadura com abóbora esculpida (ou lanterna de abóbora) na cabeça impede que jogadores e mobs pisoteiem a terra arada a até 10 blocos.

**Sementes raras:** quando uma plantação amadurece ao lado de outra diferente (também madura), há 3% de chance de nascer uma semente mutante. Plantada, ela dá a variedade rara (e mais sementes):

| Cruzamento | Variedade |
|---|---|
| Trigo + Cenoura | Trigo Dourado |
| Cenoura + Batata | Cenoura Cristalina |
| Batata + Beterraba | Batata Ancestral |
| Beterraba + Trigo | Beterraba Rubi |

## Cozinha

**Ritual:** jogue **1 caldeirão** e **1 balde de água** em cima de um **defumador** (o balde volta vazio).
- **Clique direito:** menu de receitas. Clique num prato para cozinhar com os ingredientes do inventário.
- **Agachado + clique:** o defumador normal (continua funcionando).
- `/receitas` mostra o livro de receitas em qualquer lugar.

**42 pratos**, liberados pelo nível de Culinária. Comer dá buffs (a duração fica gravada no prato):

| Prato | Nível | Efeito |
|---|---|---|
| Pão Caseiro | 0 | Pressa |
| Sopa do Pescador | 10 | Respiração aquática e Sorte |
| Ensopado de Legumes | 0 | Regeneração |
| Torta de Abóbora da Vovó | 10 | Sorte e Velocidade |
| Salmão com Ervas | 15 | Respiração aquática e Graça do golfinho |
| Batata Recheada | 20 | Absorção e Resistência |
| Biscoito de Mel | 25 | Pressa II |
| Sushi Real | 45 | Graça do golfinho, Poder do conduíte e Pressa (pede Salmão-Rei) |
| Ensopado do Caçador | 30 | Força |
| Salada Arcana | 40 | Visão noturna e +60 de mana |
| Banquete do Guerreiro | 50 | Força, Resistência e Regeneração (pede Trigo Dourado) |
| Pão Dourado | 60 | Absorção II e saciedade cheia (Trigo Dourado) |
| Sopa Cristalina | 70 | Resistência ao fogo e Velocidade II (Cenoura Cristalina, Batata Ancestral) |
| Caldeirada Abissal | 75 | Resistência, Visão noturna e Força (Peixe Abissal, Batata Ancestral) |
| Torta Rubi | 80 | Vida extra II e Regeneração II (Beterraba Rubi) |
| Banquete Lendário | 90 | Força, Resistência, Regeneração e Velocidade **para todos a até 10 blocos** (as 4 variedades) |
| Caldo Quente do Pescador | 15 | Resistência ao fogo por 8 min (protege do frio) e Regeneração (Truta-do-Gelo) |
| Sorvete de Cristal | 50 | Velocidade II e Pressa II (Enguia de Cristal) |
| Caviar Ancestral | 80 | Força, Resistência, Vida extra II e Sorte (Esturjão Ancestral) |
| Pão com Geleia | 5 | Velocidade e Pressa (pão + geleia) |
| Salada da Primavera | 10 | Regeneração e Velocidade (pastinaca, alho, feijão-verde) |
| Milho Assado com Mel | 15 | Pressa e saciedade (milho + mel) |
| Omelete de Queijo | 20 | Absorção e Pressa (ovos, queijo, alho-poró) |
| Torta de Morango | 25 | Velocidade II e Sorte |
| Creme de Abóbora-Moranga | 30 | Absorção II e Resistência (abóbora-moranga, cebola) |
| Sorvete de Melão | 30 | Velocidade e Resistência ao fogo |
| Pizza da Roça | 35 | Força e Absorção (tomate, queijo, trigo) |
| Sopa de Inverno | 40 | Resistência ao fogo (esquenta) e Regeneração (as 4 do inverno) |
| Ratatouille | 50 | Força, Regeneração e Resistência (berinjela, tomate, pimenta, cebola) |
| Bolo de Oxicoco | 55 | Vida extra e Regeneração |
| Risoto de Trufa | 70 | Sorte II, Visão noturna e Regeneração II (trufa, queijo) |
| Banquete da Fazenda | 85 | Força, Resistência, Regeneração e Pressa **para todos a até 10 blocos** (queijo de cabra, trufa, uva, milho, ovos) |

**Pratos de monstro** (2.44, referência: Dungeon Meshi): o que os monstros deixam também vai para a panela, e cada um tem um efeito estranho e útil, além dos buffs.

| Prato | Nível | Ingredientes | Efeito |
|---|---|---|---|
| Ensopado do Necromante | 10 | 4 carnes podres, batata, cogumelo marrom, tigela | Sacia e, por 5 min, fome e veneno não pegam em você |
| Omelete de Olho de Aranha | 15 | 2 olhos de aranha, 2 ovos | Visão noturna e, por 45 s, **sobe paredes** andando contra elas |
| Gelatina de Slime | 20 | 3 bolas de slime, açúcar, tigela | Pulo II e 2 min **sem dano de queda** |
| Caldo de Ossos | 25 | 4 ossos, cenoura, tigela | Resistência e, por 3 min, quase não é empurrado |
| Crocante de Phantom | 30 | 2 membranas de phantom, mel, trigo | Queda lenta e os phantoms esquecem você (como se tivesse dormido) |
| Caldo do Guardião | 40 | 2 cristais e 1 fragmento de prismarinho, bacalhau, tigela | Poder do conduíte por 4 min |
| Espetinho de Blaze | 45 | pó de blaze, creme de magma, bife | Resistência ao fogo e, por 2 min, **seus golpes põem fogo** |
| Sashimi de Brisa | 50 | vara de brisa, salmão, alga | Pulo III, queda lenta e sem dano de queda |
| Pudim do Fim | 55 | pérola do ender, fruta do coro, ovo, açúcar | Velocidade e, por 90 s, ao levar um golpe às vezes **some e reaparece atrás de quem bateu** |
| Lágrima de Ghast Caramelizada | 60 | lágrima de ghast, 2 açúcares, bagas brilhantes | Regeneração II e Absorção |

O livro de receitas tem páginas (setas embaixo).

**Qualidade do prato:** sorteada pelo nível de Culinária. Se a maioria dos ingredientes for Ótima ou melhor, o prato sobe uma qualidade; todos Perfeitos = prato Perfeito. Qualidade aumenta a duração (até +50%), e o nível de Culinária soma mais até +50%.

## Criação de animais

Como em Harvest Moon e Stardew Valley. **Agachado + clique com a mão vazia** numa galinha, vaca, vaca-cogumelo, cabra, ovelha, porco ou coelho adulto: ele passa a ser **da sua fazenda** (até 40), ganha um nome e mostra os **corações** em cima da cabeça. `/animais` lista todos.
- **Todo dia** (dia do jogo): **carinho** (clique com a mão vazia) e **comida**: a ração dele na mão (trigo, sementes, cenoura...), **pasto** (de dia, ao ar livre, em cima da grama, menos no inverno) ou um **fardo de feno** a até 6 blocos (cada fardo dá 9 porções e acaba).
- **Comedouro:** jogue **1 fardo de feno e 1 tigela** em cima de um **baú**. Guarde a comida dentro (ou encha com um funil): na virada do dia, cada animal seu a até 12 blocos que não comeu come uma porção do que é dele (galinha: sementes; vaca, cabra e ovelha: trigo; porco: cenoura, batata ou beterraba; coelho: cenoura ou dente-de-leão). Um **fardo de feno** vira 9 trigos. Clique para abrir; agachado + clique mostra quantas porções tem e para quantos dias dá.
- **Afeto** de 0 a 5 corações: sobe com carinho e comida; cai se ele ficar sem carinho, com fome ou passar a **noite de inverno ao ar livre** (deixe num celeiro com teto).
- **Produtos** (com qualidade pelo afeto e pela Doma): **ovo** todo dia (2 com 4 corações), **leite** e **leite de cabra** (balde vazio, uma vez por dia), **lã** de qualidade na tosquia (+1 com 3 corações), **trufas** que o porco fuça ao ar livre (menos no inverno), **pele e pé de coelho**.
- **Filhotes:** dois animais seus que cruzam têm o filhote já na fazenda; na primavera, às vezes **gêmeos**.
- Ninguém de fora machuca seus animais. Nome novo: use uma etiqueta com nome. Dá XP de Doma.
- **Linhagens de cavalo (2.50):** cavalos, burros, mulas, lhamas e camelos guardam **pai, mãe e geração**. O filhote herda dos pais (como no jogo) e cada atributo tem 25% de chance de **melhorar** 3% a 8% (sem passar do máximo do jogo; a lhama pode ganhar força de carga). Da **5ª geração** em diante a família ganha um **nome de linhagem** (ex.: "Linhagem do Vento"), que os descendentes carregam. O **Livro do Criador** (livro + cenoura dourada + couro, sem forma) mostra, clicando no animal: velocidade em blocos/s, altura do pulo, vida, geração, pais, linhagem e quem criou, com barras comparando com o máximo do jogo. **Árvore da família:** quem nasce de um cruzamento guarda a ficha dos **pais e dos 4 avós** (nome, geração, linhagem, velocidade, pulo e vida); o Livro mostra a árvore e setas ↑/↓ comparando o filhote com o melhor dos pais. No **Estábulo**, cada montaria guardada mostra linhagem, geração e números, e **Shift+clique** nela abre a ficha completa sem tirá-la de lá.
- **Estábulo (2.50):** jogue **1 sela e 1 porteira** (de qualquer madeira) em cima de um **fardo de feno** (laço + sela no fardo é o Altar do Domador). Clique nele para **guardar** a montaria em que você está (ou a sua mais perto, até 10 blocos) e para **chamar** as guardadas: até **4 por estábulo**, só quem guardou chama. A montaria sai do mundo e volta igual (nome, linhagem, sela, armadura, componentes e alforje do Domador). Vale para cavalo, burro, mula, lhama, camelo e porco/lavagante selados; as lendárias não entram. Quebrar o estábulo solta todas ali mesmo.

## Tacho do Artesão

**Ritual:** jogue **4 garrafas de vidro** e **1 açúcar** em cima de um **vaso decorado**. Clique com o ingrediente na mão (até 8 iguais de uma vez) e espere; clique de novo para pegar. O produto guarda a qualidade do ingrediente e dá efeitos ao comer.

| Ingrediente | Vira | Tempo |
|---|---|---|
| Leite (o balde volta) | Queijo | 20 min |
| Leite de cabra | Queijo de Cabra | 25 min |
| Ovo | Maionese | 5 min |
| Frutas (morango, mirtilo, uva, oxicoco, melão, do pomar, maçã, bagas) | Geleia de... | 15 min |
| Legumes (as outras plantações da estação, cenoura, batata, beterraba) | Conserva de... | 15 min |
| Trufa | Óleo de Trufa | 10 min |

## Party

`/party` abre o menu (ou `/party convidar <nick>`; o convite chega no chat com botões **[Aceitar] [Recusar]**). Até 6 jogadores.
- **Sem fogo amigo:** ninguém da party fere o outro, nem com flecha, poção, pet ou magia. O líder pode ligar o fogo amigo para treinar.
- **XP dividido:** quando você mata um monstro, quem da party está a até 32 blocos ganha **50% do XP de Combate**, e você ganha **+10% por membro perto** (até +30%).
- **Chat da party:** `/pc <mensagem>`, ou ligue o modo chat no menu e tudo o que você escrever vai só para a party.
- **Aviso de vida baixa:** se alguém fica com menos de 30% de vida, a party fica sabendo.
- O menu mostra a vida, o nível total e a distância de cada membro. O líder passa a liderança (shift + clique esquerdo) ou tira alguém (shift + clique direito).

## Territórios

**Ritual:** coloque uma **magnetita** e jogue em cima **1 estandarte** e **4 esmeraldas**. Ela vira o **Marco do Território**, com o seu estandarte girando em cima, e já protege **3×3 chunks** (48×48 blocos) em volta.
- **Tamanho:** 9 chunks no começo, **+1 chunk a cada 20 níveis somados** de todos os atributos, até 60. Chunk novo precisa encostar no território.
- **Expansões:** quer mais terra em outro lugar? Faça o mesmo ritual numa magnetita longe do seu território, com **1 bloco de esmeralda a mais**: ela vira um **Marco de Expansão**, outra área do **mesmo** território (mesmos membros e regras), com **+9 chunks** (3×3) além do seu limite. 1 expansão a cada 300 níveis somados, até 2. Para desfazer: menu → Expansões → shift + clique dentro dela (os chunks que só se ligavam a ela são liberados).
- **Um território por jogador** (com as expansões dele). Clique no Marco (ou `/territorio`) para o menu:
  - **Mapa:** os chunks em volta, coloridos (seu, de amigo, de outro, livre). Clique num livre para reivindicar; shift + clique num seu para liberar.
  - **Membros:** constroem e mexem em tudo. A **party** do dono também (regra "Party pode construir").
  - **Regras:** PvP, portas para visitantes, baús para visitantes, monstros nascem, explosões, fogo, reino e party ("Reino pode construir": membros do seu reino constroem aqui) e trilhas (2.94: "Trilhas se formam", grama muito pisada vira caminho).
  - **Mostrar bordas:** partículas nas bordas por 20 segundos.
  - **Placa de boas-vindas (2.57):** `/territorio boasvindas <frase>` (até 60 letras). Quem entra no seu território lê a frase em cima do aviso de território. `apagar` tira.
  - Segurando outro estandarte, clique na bandeira do menu para trocar.
- Ao entrar num território aparece "Território de Fulano" na tela (e o reino, se for uma província).

**O que é protegido (para quem não é dono/membro):** quebrar e colocar blocos, baldes, placas, baús e qualquer bloco com inventário, portas/botões/alavancas, pisotear plantação, animais, aldeões (dá para comerciar), suportes de armadura, molduras, barcos e carrinhos, isqueiro, farinha de osso, enxada/machado/pá no bloco, estações (o defumador da Cozinha conta como baú; o Altar só invoca para quem mora lá).

**O mundo respeita a borda:** líquidos e pistões não atravessam, fogo de fora não entra, explosões não quebram blocos (com a regra desligada), endermans não pegam blocos, mobs não pisoteiam a plantação, árvores (inclusive a magia Gênese) não crescem para dentro. As magias que colocam blocos também respeitam os territórios.

Para remover o Marco: **Abandonar território** no menu (as esmeraldas não voltam; o estandarte cai no chão).

## Colônias

Um jeito simples de ter uma vila com moradores trabalhando, no estilo Minecolonies, usando o que o plugin já tem.

**Ritual:** no **seu** território, jogue **1 bloco de esmeralda** e **1 cama** em cima de um **sino**: ele vira a **Prefeitura** e os 2 primeiros moradores chegam. Clique no sino abre a colônia (agachado toca o sino); `/colonia` abre de qualquer lugar.
- **Moradores** são aldeões de verdade: andam até o posto de dia e dormem à noite. Clique num morador para dar um trabalho a ele.
- **Depósito:** defina até 4 baús perto da Prefeitura (até 32 blocos). Os moradores guardam ali o que produzem e pegam ali comida e materiais. Sem depósito, ninguém trabalha.
- **Comida e felicidade:** cada morador come um pouco por turno. Pratos da Cozinha alimentam mais e deixam todos felizes. Camas, comida e felicidade aumentam a produção (de ×0,5 a ×1,2) e trazem moradores novos a cada 4 minutos.
- **Nível do morador:** de 1 a 10, cada nível dá +10% de produção.
- **A colônia é o seu território inteiro** (com as expansões): camas, plantações, postos de trabalho, água, troncos e o pomar contam em qualquer chunk dele, de 40 blocos abaixo a 60 acima da Prefeitura. Chunks longe (descarregados) guardam a última contagem.

| Profissão | Posto (no território) | Produz | Colônia nível |
|---|---|---|---|
| Fazendeiro | Composteira | Trigo, cenoura, batata e beterraba das plantações em volta (com qualidade ★, às vezes uma variedade rara), frutas maduras do pomar e plantações da estação | 1 |
| Lenhador | Bancada de flechas | Troncos (do tipo de árvore que houver perto), mudas e maçãs | 1 |
| Pescador | Barril (com água perto) | Peixes com qualidade, às vezes peixes raros | 1 |
| Construtor | Bancada de trabalho | Ergue as construções dos projetos (veja abaixo) | 1 |
| Minerador | Cortador de pedras | Pedra, carvão, minérios brutos e, às vezes, diamante | 2 |
| Cozinheiro | Cozinha | Pratos com os ingredientes do depósito | 2 |
| Mercador | Mesa de cartografia | Vende na estrada a **sobra** do depósito (o que passa de 1 pack, ou de 2 de comida e 3 de pedregulho e troncos) e põe **esmeraldas** no depósito: até 1 + nível÷3 por turno. Nunca vende diamantes, itens do plugin nem o que não está na tabela de preços | 2 |
| Ferreiro | Forja do Ferreiro | Conserta equipamentos do depósito e funde minérios brutos | 3 |
| Tratador | Altar do Domador | Couro, lã, ovos, penas e carne | 3 |
| Bibliotecário | Atril | 3 canas → papel; 3 papéis + 1 couro → livro; 1 livro + 4 lápis-lazúli → **livro encantado** ao acaso (o nível do encantamento sobe com o do bibliotecário; do nível 7 em diante, às vezes um raro como Remendo) | 3 |
| Cartógrafo (2.99) | Mesa do Cartógrafo (a estação, não a mesa comum) | Com **mapas vazios** no depósito, de vez em quando desenha um **Mapa da Região** da colônia ou um **Mapa de Estrutura** (até uma estrutura que o dono ainda não visitou, com um X) |
| Alquimista | Bancada Alquímica | Pó Arcano e Óleo de Peixe | 4 |

Cada morador precisa do **seu** posto: 2 fazendeiros precisam de 2 composteiras.

**Níveis da colônia** (Prefeitura → Melhorar, pago com o depósito): cada nível libera +3 moradores (até 15) e novas profissões. O nível 5 dá +20% de produção. Os moradores ficam protegidos de outros jogadores e não fazem comércio. Se a Prefeitura for quebrada, eles viram aldeões comuns.

### Jeito e pedidos dos moradores

Cada morador chega com **1 ou 2 traços** (o "jeito" dele, no menu do morador), que nunca se contradizem:

| Traço | Efeito |
|---|---|
| Dedicação / Preguiça | Produz 15% a mais / a menos |
| Apetite grande / Come pouco | Come quase o dobro (mas pratos da Cozinha o deixam radiante) / pouco mais da metade |
| Bom humor / Mau humor | A colônia fica mais / menos feliz |
| Coragem / Medo | Como soldado: +20% / -15% de vida e dano |
| Acorda cedo | Trabalha também no começo e no fim da noite |
| Curiosidade | Ganha 50% a mais de XP |
| Sorte | Às vezes rende um item a mais |

**Pedidos pessoais:** de vez em quando um morador pede alguma coisa e um **❗** aparece sobre a cabeça dele (no máximo 2 pedidos abertos por colônia). Metade das vezes é algo da profissão dele (enxada de ferro para o Fazendeiro, vara de pescar para o Pescador, 16 carvões para o Ferreiro, escudo para o Soldado, maçãs para o cavalo do Cavaleiro...), e na outra metade algo de qualquer um: um prato da Cozinha, flores, frutas do pomar, uma coleta da estação, mel, uma bebida do barril, uma lanterna, um livro. Clique no morador e no botão do pedido para entregar (sai do seu inventário; qualquer jogador da colônia pode atender).
- **Atendido:** a colônia ganha **+8% de felicidade** e o morador fica **contente** (+15% de produção por 20 turnos).
- **Esquecido** (o prazo é de 30 turnos, ~30 min com a colônia carregada): **-5% de felicidade** e o morador fica **chateado** (-10% por 10 turnos).

### Laços com os moradores

Cada morador tem uma história (referência: Persona e Fire Emblem). No menu dele, o botão **♥ Laço** mostra o quanto vocês são próximos:
- **Conversar:** +1 ponto, uma vez por dia (dia do jogo).
- **Presente favorito** (cada profissão gosta de 2 coisas, listadas no botão): segure na mão e clique, +3 por dia.
- **Atender um pedido dele:** +2.

São **5 capítulos** (3, 8, 14, 21 e 30 pontos). Em cada um ele conta um pedaço da vida dele: de onde veio, do que tem medo, com o que sonha e um segredo. No **5º capítulo** ele te **ensina algo para sempre**, um por profissão:

| Profissão | Ensinamento |
|---|---|
| Fazendeiro | Mão na Terra: 10% de chance de colheita em dobro |
| Lenhador | Corte Limpo: machados se gastam pela metade |
| Pescador | Paciência do Rio: os peixes mordem mais rápido |
| Minerador | Faro de Veio: 8% de chance de minério em dobro |
| Cozinheiro | Tempero de Casa: comer cura 1 coração a mais |
| Ferreiro | Ouvido do Ferro: armas, ferramentas e armaduras se gastam 25% menos |
| Tratador | Voz Mansa: seus companheiros evoluem 20% mais rápido |
| Alquimista | Gota a Mais: poções duram 25% mais |
| Mercador | Olho para o Valor: 5% de chance de esmeralda ao derrotar monstros |
| Bibliotecário | Leitura Atenta: +25% de experiência |
| Cartógrafo | Pés na Estrada: +5% de velocidade em qualquer estrada |
| Construtor | Mãos Firmes: quedas tiram 3 de vida a menos |
| Soldado | Postura de Guarda: +1 de armadura |
| Arqueiro | Olho Firme: +10% de dano com flechas |
| Cavaleiro | Rédeas Firmes: +10% de dano montado |
| Sem profissão | Tempo Livre: +5% de velocidade |

Qualquer pessoa da colônia pode criar laços; cada jogador tem o seu com cada morador. Completar um laço dá o título **Confidente**. Os laços ficam em `lacos.yml`.

### Construções (projetos)

No estilo MineColonies: a colônia cresce com construções que os **Construtores** erguem bloco a bloco.
1. **Pegue um projeto** na Prefeitura → **📜 Projetos**.
2. **Clique com o projeto no chão** onde quer a obra. **O lado em que você está vira a entrada.** Aparece uma **cerca** em volta do canteiro, com um **portão** na entrada e um corredor de 1 bloco entre a cerca e a obra.
3. **Agachado + clique na cerca** abre o menu da obra: **girar 90°**, **mudar de lugar** (a cerca some e o projeto volta), ver o contorno em partículas, a lista de materiais e **trazer do depósito** o que faltar. Girar e mudar só até a obra começar.
4. **Ponha um baú dentro da cerca** (no corredor, fora da obra) com os materiais.
5. Um **Construtor** livre (posto: bancada de trabalho; um posto por Construtor) vai até lá, **limpa o terreno** (o que sai vai para o baú da obra), tapa buracos sob o piso e **ergue a construção** de dia, tirando tudo do baú. Se faltar algo, ele avisa quanto falta e espera.
6. Pronta, a cerca some e a construção entra na lista da Prefeitura → **⚒ Obras**.

**Projetos de vila (2.90).** Os 16 projetos foram refeitos no estilo de uma vila medieval: base de pedregulho e musgo, enxaimel de carvalho escuro com bétula, telhados de duas águas com beiral, chaminés com fumaça de verdade (fogueira acesa no topo), venezianas, lanternas e interior mobiliado. Eles pedem mais material que os antigos. A maioria já sai pronta no **nível II**; a Fazenda, a Biblioteca e a Ferraria saem no **III**, e a Mina sai no I.

Os projetos antigos (as caixas de 7×7, com os ids `casa`, `oficina`...) continuam valendo para as construções que já foram feitas com eles, mas não aparecem mais no menu. Os novos têm id com `_vila` (`casa_vila`, `oficina_vila`...).

| Projeto | Tamanho | Nível da colônia | Como é |
|---|---|---|---|
| Casa | 11×11, 13 de altura | 1 | Sobrado de enxaimel com 4 camas, lareira, mesa, varanda coberta com banco e chaminé |
| Oficina do Construtor | 13×11 | 1 | Galpão de barro e abeto com bancadas, cortador, andaime e um telheiro com toras empilhadas |
| Fazenda | 15×13 | 1 | Canteiros de trigo, cenoura, batata e beterraba com 2 canais de água, espantalho, composteiras e postes de luz, tudo cercado |
| Poço da Praça | 9×9 | 1 | Poço de pedra com telhado em pirâmide, lanterna na corrente, bancos e flores |
| Torre de Vigia | 9×9, 24 de altura | 1 | Base de pedra, corpo de enxaimel, escada de mão até a sacada com sino e telhado pontudo |
| Cabana do Lenhador | 12×10 | 1 | Cabana de toras de abeto com telheiro, pilha de toras, cepo e chaminé |
| Cabana do Pescador | 11×12 | 1 | Palafita de bétula com deque, barris, caldeirão e varal |
| Armazém | 13×13 | 2 | Galpão com portão duplo, prateleiras de baús e barris e fardos de feno |
| Biblioteca | 13×13, 14 de altura | 2 | Salão de 2 andares com estantes até o teto, mezanino, janelões e 3 atris |
| Quartel | 15×13 | 2 | Alojamento de pedra com 4 camas e um pátio de treino cercado, com alvos e bonecos de palha |
| Mina | 13×12 | 2 | Casa da mina de pedra, trilhos e uma torre de içar com corrente sobre o poço |
| Mercado | 15×12 | 2 | Salão com arcada, 3 bancas de toldo listrado, balcão e a mesa de cartografia |
| Taverna | 13×13, 14 de altura | 2 | 2 andares com balcão de barris, 4 mesas, lareira, quartos em cima e placa pendurada |
| Ferraria | 12×11 | 3 | Forja de pedra com frente aberta, grande chaminé de tijolos, bigornas, alto-forno e água de têmpera |
| Estábulo | 13×13 | 3 | Celeiro com portão grande, baias, sótão de feno e cocho |
| Laboratório | 11×11, 15 de altura | 4 | Ardósia com torrinha de cúpula de cobre, suportes de poções, mesa de encantamento e jardim de ervas |

O Quartel novo tem o pátio de treino dentro da área, então pede menos piso coberto (50%, 55% e 60%).

**Do seu jeito (botão direito no projeto):** em vez do desenho pronto, você mesmo constrói. A cerca marca a área (agachado + clique: **área maior/menor**, até +6 de cada lado, e girar a entrada). Construa lá dentro e peça a **vistoria** no menu da cerca: se atender o **mínimo do nível 1**, vira construção da colônia e a cerca some. O menu mostra, nível por nível, o que já tem (✔) e o que falta (✖).

**Níveis das construções** (como os prédios do MineColonies): cada projeto tem 3 níveis, cada um com um mínimo de **blocos construídos**, camas, portas, luzes, baús, **% do piso com teto**, altura e blocos certos (atris, estantes, alvos, terra arada...). Melhore a construção e peça a vistoria de novo em **⚒ Obras**: o nível sobe (ou desce, se você desmontar; no nível 0 ela "precisa de reparo" e não conta).

| Projeto | Nível 1 (mínimo) | O que o nível dá |
|---|---|---|
| Casa | 40 blocos, 2 camas, 1 porta, 1 luz, 70% coberto | As camas trazem moradores |
| Fazenda | 16 terras aradas, 1 composteira | Fazendeiro +10% por nível |
| Poço da Praça | 15 blocos, 1 luz | Felicidade +5% por nível (até 15%) |
| Torre de Vigia | 80 blocos, 9 de altura, 6 escadas de mão, 1 luz | Vigia 3×3 chunks, 5×5 no nível 2, 7×7 no 3 |
| Armazém | 50 blocos, 4 baús, porta, luz, 70% coberto | Todos os baús dela viram depósito |
| Biblioteca | 80 blocos, 1 atril, 10 estantes, porta, luz, 70% coberto | Bibliotecário +10% por nível |
| Quartel | 70 blocos, 2 camas, 1 alvo, porta, luz, 70% coberto | Alvos = vagas de soldado |

**Placa da construção** (o "bloco do prédio" do MineColonies): toda construção pronta ganha uma placa no chão, do lado de dentro perto da entrada, com o nome, o nível e o dono. **Clique nela** abre o menu da construção. **Quebrar a placa desfaz a construção** (pede para quebrar de novo em 10 segundos; os blocos ficam): a Torre de Vigia devolve os chunks que ela tinha posto no território (menos os que outra torre ainda vigia) e o Armazém tira seus baús do depósito. Explosão e pistão não mexem nela. Se a placa sumir de outro jeito, a construção fica "precisando de reparo" (nível 0) até uma nova vistoria pôr outra.

**Cada profissão tem a sua construção** (as cabanas do MineColonies; `colonia.trabalho-nas-construcoes`): o posto só conta se estiver **dentro** da construção pronta (nível 1 ou mais), e ela dá **+10% de produção por nível** a quem trabalha lá.

| Profissão | Construção | Posto lá dentro | Nível da colônia |
|---|---|---|---|
| Construtor | Oficina do Construtor | — (1 Construtor trabalha sem ela; +1 vaga por nível da Oficina) | 1 |
| Fazendeiro | Fazenda | Composteira | 1 |
| Lenhador | Cabana do Lenhador | Bancada de flechas | 1 |
| Pescador | Cabana do Pescador (perto da água) | Barril | 1 |
| Minerador | Mina | Cortador de pedras | 2 |
| Mercador | Mercado | Mesa de cartografia | 2 |
| Cozinheiro | Taverna | Cozinha (faça o ritual no defumador) | 2 |
| Bibliotecário | Biblioteca | Atril | 3 |
| Cartógrafo | Biblioteca | Mesa do Cartógrafo | 3 |
| Soldado, Arqueiro, Cavaleiro | Quartel | Alvo (4 vagas cada) | 2 |
| Ferreiro | Ferraria | Forja do Ferreiro (ritual na bigorna) | 3 |
| Tratador | Estábulo | Altar do Domador (ritual no fardo de feno) | 3 |
| Alquimista | Laboratório | Bancada Alquímica (ritual no suporte de poções) | 4 |

**Casa de cada morador** (`colonia.moradia-nas-casas`): cada morador mora numa **Casa** da colônia. A colônia distribui sozinha pelas camas das casas prontas, e o aldeão **dorme na cama da casa dele**. **Sem casa:** fica triste (até -20% de felicidade) e produz 15% a menos. **Casa de nível 2 ou 3:** +5% de produção por nível acima do 1. **Morador novo só chega se houver vaga nas casas** (os 2 fundadores dormem na Prefeitura; soldados moram no quartel). O menu mostra a casa de cada um e, na Casa, quem mora nela.

Com as duas opções em `false`, volta o jeito antigo: posto em qualquer lugar do território e qualquer cama serve.

"Blocos construídos" são blocos firmes que não são terreno natural (terra, pedra, areia, troncos e minérios só contam se alguém colocou). Os números estão em `niveis:` de cada `plantas/*.yml`; o plugin avisa no console se o desenho pronto não atende o próprio nível 1.

- A colônia toca **1 + o nível** obras ao mesmo tempo. A obra precisa caber inteira (com a cerca) no território, longe da Prefeitura, e não aceita baús, camas, estações do plugin nem coisas construídas por jogadores na área (o chão aplainado pode).
- Os blocos que o Construtor põe contam como **colocados**: quebrar a casa não dá XP de atributo.
- **Projetos próprios:** os projetos ficam em `plugins/RPGAtributos/plantas/*.yml`. Cada arquivo tem a legenda (uma letra por bloco, como `oak_stairs[facing=east]`) e as camadas de baixo para cima; em cada camada a 1ª linha é o **fundo** e a última é a **frente** (a entrada), e `south` é o lado da entrada. `.` é ar e `~` é "não mexe". Crie um arquivo novo e use `/rpgadmin reload`.

### Exército

Três profissões de guerra na colônia: **Soldado** (espada, liberado no nível 2), **Arqueiro** (arco, nível 3) e **Cavaleiro** (nível 4). O posto delas é o **Quartel**, um **bloco de alvo** perto da Prefeitura, e cada um abriga 4 soldados.
- O **Cavaleiro** luta **montado** num cavalo selado da colônia: +10 de vida e +1,5 de dano, enxerga inimigos mais longe e o cavalo o leva até eles. Se o cavalo morrer, ele pega outro em 2 minutos. O cavalo é protegido como o próprio Cavaleiro e some junto com ele.
- O morador **veste a armadura**: vira um guerreiro com o uniforme na **cor do reino**. Não queima no sol e os aldeões não têm medo dele.
- Os soldados atacam monstros sempre. Na **guerra**, também atacam os jogadores e os soldados do reino inimigo. Nunca atacam aldeões, aliados nem o dono.
- **Ordens** (Prefeitura → Exército, clique): **guardar** a colônia, **seguir você** ou **atacar** o Marco inimigo mais perto (só na guerra).
- **Shift + clique** no Exército equipa todos com as **melhores armas e armaduras do depósito**. Itens forjados contam, com os bônus deles. O cavalo do Cavaleiro também ganha a melhor armadura de cavalo do depósito.
- Soldados sobem de nível ao vencer inimigos (mais vida e dano) e se curam quando descansam.

## Reinos e guerras

**Fundar:** `/reino fundar <nome>` (precisa de colônia nível 2 e 32 esmeraldas). O reino ganha uma cor própria e o nome aparece em cima da cabeça dos membros.
- **Cargos:** **Rei** (guerra, paz, cargos e coroa), **Nobre** (tesouro, expulsar), **Cavaleiro** (pode convidar) e **Cidadão**.
- **Tesouro** de esmeraldas: qualquer membro deposita; o Rei e os Nobres sacam.
- Membros do mesmo reino **não se machucam**.
- **Províncias:** o território de cada membro é uma província do reino. Ao entrar aparece o nome do reino; no mapa (`/reino mapa`) cada chunk mostra de que reino é. Com a regra "Reino pode construir" (ligada por padrão), os membros do reino constroem nas províncias uns dos outros.
- **Leis do reino** (só o Rei, pelo menu ou `/reino lei <lei>`): **Lei da Paz** (sem PvP nas províncias; a guerra continua valendo), **Lei contra Explosões**, **Lei contra Incêndios** e **Lei das Muralhas** (monstros não nascem). A lei vale em todas as províncias, mesmo que o dono tenha a regra ligada.
- `/reino` abre o menu. Comandos: `convidar`, `aceitar`, `sair`, `expulsar`, `cargo <nick> <nobre|cavaleiro|cidadao>`, `coroa <nick>`, `tesouro <depositar|sacar> <qtd>`, `guerra <reino>`, `paz <reino>`, `lei <paz|explosoes|incendios|muralhas>`, `mapa`, `provincias`, `lista`, `desfazer confirmar`.

**Guerra com horário marcado:** o Rei declara (`/reino guerra <reino>`) e a batalha acontece no **horário de guerra do servidor** (padrão: sábado às 20h, por 60 minutos, com pelo menos 24h de aviso; muda no `config.yml`). Todo mundo é avisado 1h, 10 min e 1 min antes.
- Durante a guerra, o **PvP entre os dois reinos é livre** em qualquer lugar.
- **Capturar um Marco:** fique a até 6 blocos do Marco do Território de um inimigo por 2 minutos **sem nenhum defensor por perto** (jogadores ou soldados dele). Uma barra mostra o progresso.
- **Pontos:** Marco capturado = 10, jogador abatido = 2, soldado abatido = 1.
- **Resultado:** cada Marco capturado faz aquele território **perder 4 chunks** (os mais longe do Marco), que viram chunks a mais para o Rei vencedor. No fim, quem fizer mais pontos leva **30% do tesouro** do outro.
- **Construções nunca são destruídas**: a proteção dos blocos continua valendo na guerra.
- Depois de uma guerra, os dois reinos ficam **7 dias em trégua**. Os dois Reis podem fazer as pazes antes com `/reino paz <reino>`.

## Domador

**Ritual:** jogue **1 laço** e **1 sela** em cima de um **fardo de feno**. Ele vira o **Altar do Domador**.

**Conseguir companheiros** (clique no altar):
- **Vincular:** traga um animal preso no seu laço (ou um pet que você domou) até perto do altar e clique nele no menu.
- **Invocar:** entregue os "componentes" da criatura e ela nasce no altar, já como seu companheiro. São 20 criaturas, por exemplo: vaca (4 couros + 2 carnes), galinha, lobo (8 ossos + 4 carnes), vaca de cogumelo, cavalo, camelo, urso polar, golem de ferro (4 blocos de ferro + abóbora + papoula), strider, farejador...
- Quantos companheiros: 1 no começo, até **5** com Doma alta.

**Ordens** (clique no companheiro com a mão vazia, ou `/pets`). Nenhuma ordem teleporta: ele sempre vai **andando**, e lobos e gatos também perdem o teleporte até o dono.

| Ordem | O que faz | Doma |
|---|---|---|
| Seguir | Anda atrás de você e, se souber lutar, ataca quem estiver mirando em você | 0 |
| Ficar | Fica onde você está (quem senta, senta) | 0 |
| Guardar | Fica no lugar e ataca monstros que chegarem perto (raio 6 a 16) | 15 |
| Patrulhar | Anda pela área em volta de onde você deu a ordem, atacando monstros (raio 8 a 24) | 25 |
| Solto | Faz o que quiser, como um animal comum | 0 |

**Componentes** (colocados no menu do companheiro, com ele perto de um Altar do Domador; 1 espaço no começo, até **6**):

| Componente | Item | Doma | Efeito |
|---|---|---|---|
| Sela do Domador | sela | 0 | Monta e guia com WASD, espaço pula |
| Agilidade | açúcar | 5 | +30% de velocidade |
| Alforje | baú | 10 | Carrega 27 itens |
| Farol | pedra luminosa | 10 | Brilha e dá Visão Noturna a quem monta |
| Couraça | peitoral de ferro | 15 | +8 armadura e +25% vida |
| Garras | espada de ferro | 20 | Luta contra monstros (até uma vaca!) e bate +50% |
| Coletor | funil | 25 | Pega os itens do chão e guarda no Alforje (pede Alforje) |
| Coração do Mar | coração do mar | 30 | Não se afoga, nada rápido; quem monta respira debaixo d'água e **mergulha** junto (andando para a frente, olhe para baixo para descer e para cima para subir; parado, fica na mesma altura; espaço sobe; agache para desmontar) |
| Chamas | vara de blaze | 35 | Imune a fogo; os ataques incendeiam |
| Segunda Vida | totem da imortalidade | 40 | Escapa da morte uma vez |
| Asas | élitro | 50 | **Voa** montado: espaço sobe, agachar desce (pede Sela) |
| Pele de Netherite | lingote de netherite | 60 | +6 armadura, +4 resistência, quase não é empurrado, imune a fogo |

**Exemplo:** vaca de cogumelo + Sela + Asas = montaria voadora (Doma 50). Patrulhar + Coletor = um ajudante que recolhe os itens de uma fazenda.

**Visual (com o pacote de recursos):** armaduras e partículas. As peças 3D que seguiam o bicho (sela, alforje, lanterna, asas e auréola) saíram na 2.36.1 porque pesavam no jogo; os componentes continuam funcionando igual.
- **Armaduras próprias em cavalos e lobos:** Couraça (aço com rebites), Pele de Netherite (escura com brilho roxo), Chamas (fogo) e Coração do Mar (ondas turquesa). Vale a do componente mais forte. É só aparência: a proteção continua vindo do componente, ela não se gasta, não cai quando o bicho morre e não sai com tesoura nem pelo inventário do cavalo. Se você puser uma armadura de verdade no cavalo ou no lobo, a sua fica no lugar.
- Chamas soltam faíscas de fogo e o Coração do Mar pinga água.

**Montarias lendárias (os Guardiões):** cada uma é **única no servidor**. No Altar do Domador, o botão **✦ Fusão Lendária** funde dois companheiros seus no **estágio III** (perto do altar) e alguns itens num **Ovo Lendário**. Os componentes e o alforje dos dois caem no chão. Coloque o ovo no chão, no lugar certo: ele choca em 10 minutos (`doma.lendarias`) e nasce quando você está por perto. Clique no ovo para ver quanto falta; agache e bata nele para pegar de volta. A lendária nasce no nível 1, evolui como os outros companheiros, tem +50% de vida e dano e uma **pele própria** (com o pacote de recursos). Os poderes já vêm de nascença e não ocupam espaço de componente.

| Lendária | Fusão | Onde choca | Poderes |
|---|---|---|---|
| **Qilin** 麒麟 | Cavalo III + Raposa III + 1 Essência Primordial + 2 blocos de esmeralda + 1 maçã dourada | Num campo de flores (8 flores perto), de dia | Anda sobre a água e a lava, imune a fogo (quem monta não se queima), flores brotam onde passa, Bênção: você e a party perto se curam devagar |
| **Fenghuang** 凤凰 | Cavalo III + Papagaio III + 1 Fragmento Celeste + 8 varas de blaze + 1 maçã dourada | Perto de fogo, lava, fogueira acesa ou magma | Voa, imune a fogo, ataques incendeiam, rastro de brasas; **Renascer**: ao morrer vira um ovo em chamas que renasce no mesmo nível |
| **Longma** 龙马 | Cavalo III + Tatu III + 1 coração do mar + 16 cristais de prismarinho + 1 maçã dourada | Na beira da água (6 blocos de água perto) | Galopa por cima da água, nada rápido e quem monta respira debaixo d'água; **Onda**: montado, empurra para longe os monstros em volta |
| **Baihu** 白虎 | Lobo III + Gato III + 1 chifre de cabra + 16 ossos + 1 maçã dourada | Na neve (lugar frio ou 6 blocos de neve perto) | Um **tigre branco que dá para montar** (maior que um lobo): rápido, pula longe, luta (Garras); **Rugido**: monstros perto ficam fracos e lentos |
| **Xuanwu** 玄武 | Urso Polar III + Tatu III + 8 escamas de tartaruga + 1 concha de náutilo + 1 maçã dourada | Na areia, à noite (6 blocos de areia perto) | Uma **tartaruga gigante**: lenta em terra, rápida na água, muita armadura e imune a fogo; **Escudo**: você e a party perto recebem menos dano |

Os Guardiões ficam numa bússola no menu: Qilin no centro, Fenghuang ao sul, Longma a leste, Baihu a oeste e Xuanwu ao norte. Se um deles (menos o Fenghuang) morre ou é libertado, volta a estar livre para uma nova fusão, de qualquer jogador. O Xuanwu não ganha pele nova (a tartaruga do jogo não aceita), só o tamanho e a aura escura.

**Modelos 3D com os mods EMF + ETF (opcional):** quem joga com **Entity Model Features** e **Entity Texture Features** (Fabric ou NeoForge) vê os **cinco Guardiões** com modelos próprios feitos no Blockbench: o **Qilin** de jade e ouro com chifres, o **Fenghuang** como uma ave de penas de fogo (bate as asas mais forte no ar), o **Longma** azul com chifres de dragão, o **Baihu** como um tigre branco listrado e o **Xuanwu** como uma tartaruga com uma serpente sobre o casco. Pernas, cabeça e cauda andam e olham junto com a criatura. Quem não tem os mods continua vendo a pele normal; o mesmo pacote serve para os dois. O modelo é escolhido pelo nome, por isso o nome de uma lendária não muda (etiqueta não funciona nela). A sela, a coleira e a pele não aparecem por cima do modelo.

**Bestas Errantes:** três lendárias que não nascem de fusão. De vez em quando (a cada 5 minutos, 12% de chance para cada uma que esteja livre) uma delas aparece a uns 30 blocos de um jogador com **Doma 40%** do máximo, no lugar por onde ela vaga, e fica lá uns 15 minutos (`doma.lendarias.errantes`). Quem está perto recebe o aviso, sem coordenadas: é preciso procurar pelas partículas e pelo som. Solta, ela não se fere, não aceita laço, sela nem osso, e some se ninguém estiver perto ou quando o servidor reinicia. Quem cumprir a **prova** a conquista: ela vira companheira, única no servidor como os Guardiões. Se morrer, volta a vagar um dia.

| Besta | Criatura | Onde vaga | A prova | Poderes |
|---|---|---|---|---|
| **Pixiu** 貔貅 | lobo dourado (1,3x) | Nas badlands, de dia, ou nas cavernas abaixo do y 0 | Alimentar com **64 lingotes de ouro** (bloco vale 9). Foge de quem chega de mãos vazias; se ele for embora, o ouro dado fica com ele | Garras, Alforje e Coletor de nascença; **Fortuna**: minério quebrado pelo dono a até 16 blocos rende em dobro (25%); pepita ou lingote cura ele |
| **Baize** 白泽 | cavalo branco com olhos azuis pelo corpo | Florestas, taigas e campos, à noite, sem chuva | Responder a **3 perguntas** sobre o mundo (Guardiões, Profundezas, céu, criaturas). Errou: ele some na névoa e você espera 30 min | Montaria com Farol; **Sabedoria**: monstros perto do dono brilham através das paredes e o dono vê a vida do que mira |
| **Kunpeng** 鲲鹏 | ghast feliz com arreio azul | Sobre o oceano ou perto das Ilhas do Céu (acima do y 140) | Dar **16 bolas de neve** e por fim **1 Fragmento Celeste** | Montaria voadora do jogo para até 4 pessoas (o arreio já vem), voa 60% mais rápido; quem desce no ar cai devagar |

No menu da Fusão Lendária, as três aparecem embaixo da bússola, com a história, por onde vagam, a prova e onde estão agora. Conquistar uma dá o título **Domador de Lendas**.

**Combate montado:** montado num companheiro, aperte **F** (sem agachar; agachar + F continua sendo a habilidade de classe) para o **golpe da montaria**. O dano cresce com a sua Doma (até o dobro) e só acerta monstros.

| Montaria | Golpe | Recarga | O que faz |
|---|---|---|---|
| Qualquer uma (e o Pixiu) | Investida | 8 s | Dispara para a frente e derruba os monstros no caminho |
| Qilin | Chama de Jade | 15 s | Cone de fogo verde à frente; você e a party perto se curam |
| Fenghuang | Mergulho de Fogo | 15 s | No ar, mergulha e explode em fogo onde toca o chão; no chão, uma explosão menor |
| Longma | Maremoto | 12 s | Uma onda em linha reta que arrasta os monstros e apaga o fogo da party |
| Baihu | Bote do Tigre | 10 s | Salta sobre o monstro que você mira: dano alto e deixa lento |
| Xuanwu | Casco Inabalável | 18 s | Empurra todos em volta; você e a party ganham Resistência II |
| Baize | Olhar da Verdade | 12 s | Monstros à frente ficam visíveis (até invisíveis), marcados e fracos |
| Kunpeng | Rajada do Céu | 12 s | Uma ventania que joga longe os monstros em volta |

Correndo montado (com a tecla de correr), a montaria também **atropela** os monstros que encontra no caminho.

- Componentes precisam do item **inteiro** (sem desgaste). Tirar devolve o item; se o companheiro morrer, componentes e alforje caem no chão.
- A vida, a armadura e o dano dos companheiros crescem com a Doma do dono. Ninguém da party (nem outros jogadores, onde não há PvP) fere os seus companheiros.
- `/pets` mostra todos, com vida, ordem e distância (ou a última posição, se estiverem longe). Shift + clique chama de volta (andando).

## Masmorras

**Ritual:** coloque **tijolos de pedra entalhados** e jogue em cima **1 olho do ender** e **1 bússola**. Vira o **Altar do Portal** (até a 2.67, Portal da Masmorra).

### Masmorra 2.0: Cristais de Portal (2.68)

1. **Faça um Cristal de Portal:** clique no altar e escolha o rank (**E, D, C, B, A ou S**). Clique esquerdo paga a **oferenda** (3 coisas sorteadas para você, que ficam as mesmas até você pagar); clique direito paga com **Cristais de Mana** (E 4, D 8, C 16, B 32, A 64, S 128 de mana). O altar faz cristais até **um rank acima do seu Rank de Caçador**.
2. **Use o cristal:** clique no altar com ele na mão. A masmorra se forma e o portal abre por 30 segundos; suba nele com a sua party.
3. **Cumpra o objetivo** do cristal em **15 minutos**:

| Objetivo | O que fazer |
|---|---|
| Caçada | derrotar o chefe (como sempre) |
| Saque | achar 75% das **relíquias** (vasos dourados) espalhadas pelas salas; cada uma dá Cristais de Mana |
| Monólitos | despertar as **magnetitas** (clique): cada uma solta guardiões; vencidos, ela acende |

No Saque e nos Monólitos, o tesouro e a saída aparecem na sala de entrada.

- **Tempo:** o relógio aparece no topo. Se o tempo acabar com você lá dentro, **o que você pegou na masmorra vai para o Cofre das Almas** (o que você levou fica com você).
- **Rank:** cada rank usa a dificuldade de sempre (tabela abaixo) e ajusta a força: E é um Fácil mais leve (monstros x0,8), D um Fácil mais forte (x1,15), C Normal, B Difícil, A Pesadelo e S um Pesadelo bem mais forte (x1,45). Ranks altos dão mais tesouro e mais Cristais de Mana.
- **Modificadores** (às vezes, no cristal): Fúria (+30% de dano dos monstros), Couraça (+40% de vida), Relógio Curto (4 min a menos), Sem Cura (a fome cheia não cura), Onda Extra; os ruins aumentam o tesouro. Os bons: Tesouro Farto, Bênção (Regeneração I) e Pressa (Velocidade I).
- **Baús de raridade:** madeira, ferro, ouro e **cristal**, com mais sorteios e Cristais de Mana quanto mais raros.
- **Cristais de Mana** (pequeno, médio, grande e perfeito, valendo 1, 4, 16 e 64): caem dos monstros dos portais, das relíquias e dos baús. Pagam cristais no altar e a magia oculta.
- **Rank de Caçador** (`/masmorra rank`): sobe vencendo portais (do altar e do mundo): E → D (100 XP) → C (300) → B (700) → A (1500) → S (3000). O portal do mundo dá 25% a mais e o Portal Vermelho, o dobro.
- **Portais do mundo** também têm rank (E a S) e, raramente, são **Vermelhos**: lá dentro não há saída (nem `/masmorra sair`) até vencer.
- **Magia oculta: Abrir Portal.** O **Grimório Oculto** (raro nos tesouros dos portais B, A e S e nos baús de cristal) ensina a magia na primeira vez que você clica com ele. Depois, com ele na mão: abre um portal do mundo na sua frente, **com o rank do seu Rank de Caçador**, gastando a mana quase cheia e um **Cristal de Mana grande**, uma vez por dia de jogo. 10% de chance de sair um rank acima e 3% de abrir um **Portal Vermelho**. Você e a sua party têm 30 segundos para entrar antes de todo mundo.

### Geração nova: o vault (2.69)

Desde a 2.69 toda masmorra (do altar e dos portais do mundo) é montada no molde do Vault Hunters:
- **Grade de chunks com tamanho fixo pelo rank:** E 6×6 chunks (~7 salas), D 8×8 (~11), C 10×10 (~16), B 10×10 (~20), A 12×12 (~25), S 14×14 (~34).
- **Salas de 2×2 chunks**; às vezes (7%) uma sala grande de **3×3, 3×2 ou 2×3** chunks. A **sala do portal** (entrada) fica no meio.
- **Corredores** de 8 blocos ligam salas vizinhas; alguns são **pontes sobre abismo** ou **túneis**, e há **atalhos** que fecham voltas (o mapa não é só uma árvore). Salas podem ficar 2 blocos acima ou abaixo: o corredor ganha **degraus**.
- **Andares:** C e B têm 2, S tem 3. Ligados por **Escadarias** (uma sala alta com escada encostada na parede norte e o piso de cima).
- **Estilos de sala:** pilares, arena, ponte sobre abismo (as pontes saem das portas até o meio), biblioteca, alagada (caminhos secos até o meio), mezanino (varanda em volta com escada), cripta e jardim.
- **Minas de Cristal** (D para cima): veios de ametista nas paredes. Quebrar um dá **Cristais de Mana**.
- **Salas secretas** (C para cima): atrás de uma **parede rachada**; um golpe derruba a passagem. Dentro, um **Baú de Cristal**.
- O chefe fica na sala mais longe da entrada (sala grande vale mais). As salas construídas por admins entram no meio da sala sorteada, com os caminhos até as portas abertos.

### Dentro do vault (2.70)

- **Fases do chefe:** com 66% e 33% de vida o chefe muda de fase: fica protegido por 3 s, chama reforços e passa a soltar **golpes marcados no chão** (um círculo vermelho avisa 1,5 s antes; saia dele). Na fase 3 os golpes vêm mais rápido e caem também em lugares aleatórios. (Só no vault; os chefes do Altar Ritualístico continuam como antes.)
- **Armadilhas:** placas de pedra-negra no meio de corredores e perto das portas. Pisou: **espinhos** (dano e lentidão), **flechas** que saem das paredes ou **chamas**. Cada placa espera 3 s para disparar de novo.
- **Mímicos:** alguns baús (4% a 9%, mais nos ranks altos) são mímicos: ao abrir, viram um monstro com o baú na cabeça. Vença e o tesouro do baú cai.
- **Pilares de Bênção:** 1 a 3 por vault, cada um de um deus da Devoção. Clique: a party ganha a bênção até o fim do portal (Ferrum: Resistência; Sylva: Regeneração; Maris: Velocidade e Graça do golfinho; Bellum: Força; Arcanus: Pressa e mana; Mortis: Absorção). Quem segue aquele deus ganha o nível II.
- **Nota da corrida (S, A, B, C, D):** sai pelo tempo usado, pelas mortes (-15 cada), pelas salas limpas, pelos segredos achados (+10) e pelo chefe vencido. **B** dá +1 sorteio no tesouro final, **A** +2, **S** +4.
- **`/masmorra stats`**: portais vencidos por rank, melhor nota, melhores tempos, segredos, relíquias, mímicos e mortes. **`/masmorra recordes [rank]`**: os 5 portais mais rápidos de cada rank no servidor.

### Equipamento dos portais (2.71)

- **Variedade (2.79):** seis famílias — Clássico, Vigia, Predador, Rúnico, Espinhos e Abissal — para cada rank e arma de chefe: **180 peças de Caçador e 36 armas de chefe**. Mudam os desenhos, formas das lâminas, guardas, detalhes dos arcos e o padrão das armaduras no corpo. O estilo é sorteado no saque e fica salvo no item ao refinar; itens antigos ficam com o estilo Clássico. O bônus do conjunto continua valendo com famílias misturadas. Para receber uma família completa: `/rpgadmin pecacacador s runico`; para as seis armas na mesma família: `/rpgadmin armachefe abissal`. IDs: `classico`, `vigia`, `predador`, `runico`, `espinho`, `abissal`.
- **Monstros (2.79):** 17 espécies comuns têm quatro modelos temáticos cada (Cripta, Caverna, Inferno e Gelo), dentro e fora dos portais. As criaturas novas das masmorras recebem o nome visual do tema; fora delas as variantes são sorteadas pelo EMF. As regras dos chefes, Elites e personagens têm prioridade, e os cinco Monarcas agora têm regras para seus nomes próprios. São necessários EMF, ETF e o pacote atualizado. Se usa ZIP manual, desative o ZIP antigo do RPG e coloque o novo acima dos outros pacotes de criaturas. Os modelos ainda precisam de conferência dentro do jogo.
- **Visuais próprios (2.78):** os seis ranks de Caçador têm ícones, espadas 3D e armaduras com textura no corpo. As seis armas dos chefes têm visual temático (o Arco do Lich mantém as três fases de puxar a corda). Cristais de Mana, Cristais de Portal, as 14 Inscrições, as 25 Relíquias, o Grimório Oculto e equipamentos não identificados também têm ícones próprios, incluídos no pacote de recursos do plugin. Equipamentos antigos recebem os visuais ao entrar, abrir baús ou recolher itens. O tridente arremessado continua com a aparência da entidade do jogo; o item na mão e no inventário usa o visual próprio.
- **Conferir como administrador:** `/rpgadmin pecacacador s` entrega o conjunto S (troque a letra para os outros ranks); `/rpgadmin armachefe` entrega as seis armas. O efeito da flecha do Lich usa o arco que a disparou, mesmo depois de trocar de item na mão.
- **Equipamento não identificado:** as armas e armaduras dos baús do vault vêm como **"❓ Espada não identificada"** (sem atributos), com a faixa de raridade possível. **Clique direito** revela: o item é forjado na hora com a raridade sorteada.
- **Avaliador:** clique no **Altar do Portal** com o item não identificado na mão: ele revela por **1 de mana** em Cristais de Mana, com **15% de chance de subir uma raridade** (pode passar do máximo da faixa).
- **Equipamento de Caçador:** espada, elmo, peitoral, grevas e botas "de Caçador" de cada rank (E/D de ferro, C/B de diamante, A/S de netherite; Raro, Épico, Épico, Único, Único e Lendário). Cada peça dá **+1% a +6% de dano nos monstros dos portais** e **o mesmo a menos do dano deles** (pelo rank da peça); com as **4 peças de armadura**, +50% nisso (a redução para em 50%). Sai em **35%** dos tesouros finais e na Loja do Sistema.
- **Armas de chefe** (Lendárias, só quando o chefe do vault é vencido; 4% no E até 25% no S): **Punho do Golem Ancestral** (maça: empurra e deixa lento), **Ferrão da Rainha** (veneno), **Machado do Senhor das Chamas** (fogo), **Arco do Lich** (flechas que definham), **Tridente da Tempestade Viva** (20%: um raio e +5 de dano, também arremessado) e **Lâmina do Arauto do Fim** (definha e cura você). Refazer na Forja mantém o nome e o efeito.
- **Loja do Sistema** (**`/masmorra loja`** ou o botão no Altar do Portal), paga com Cristais de Mana:

| Item | Preço | O que faz |
|---|---|---|
| Poção do Caçador | 6 | Cura II na hora e Regeneração por 10 s |
| Poção de Mana | 6 | +50 de mana |
| Poção de Força | 10 | Força II por 60 s |
| Pergaminho de Retorno | 16 | Dentro de um portal: sai na hora **com tudo o que pegou** (não vale no Portal Vermelho) |
| Medidor de Mana | 12 | Luneta: clique e veja o portal mais perto (até 64 blocos): rank, se está acima do seu, quando transborda, quem está dentro e reserva |
| Chave Instantânea | 24 + 16 por rank | Abre na hora, onde você está, um portal do seu Rank de Caçador (objetivo sorteado, sem modificadores) |
| Peça de Caçador | 30 + 20 por rank | Uma peça do seu rank |

### Caçador: Árvore, Despertar e Inscrições (2.72)

- **Janela do Sistema** (**`/masmorra arvore`** ou o botão do caçador no Altar do Portal): o Rank de Caçador, a Árvore de Caçador e as características.
- **Árvore de Caçador:** **1 ponto no E e +3 a cada rank** (D 4, C 7, B 10, A 13, S 16; no S, +1 a cada 500 XP, até 26). Cada nó pede um rank mínimo. **Os bônus valem só dentro dos portais.** Refazer custa 16 de mana.

| Ramo | Nó (graus, rank) | Efeito |
|---|---|---|
| Lâmina | Golpe de Caçador (3, E) | +4% de dano nos monstros dos portais por grau |
| Lâmina | Execução (2, D) | +10% por grau em monstros com menos de 35% de vida |
| Lâmina | Caça-Chefes (2, C) | +6% por grau no chefe |
| Lâmina | Frenesi (1, B) | abater dá Força I por 4 s |
| Escudo | Pele Grossa (3, E) | -4% do dano dos monstros por grau |
| Escudo | Vigor do Portal (2, D) | +1 ❤ de vida máxima por grau |
| Escudo | Recuperação (2, C) | abater cura meio ❤ por grau |
| Escudo | Passo Leve (1, B) | armadilhas dão metade do dano e não deixam lento |
| Fortuna | Olho de Mana (3, E) | +15% de chance de Cristal de Mana por grau |
| Fortuna | Saqueador (2, D) | +1 sorteio no tesouro final por grau (vale o melhor da party) |
| Fortuna | Faro (1, C) | paredes rachadas e mímicos brilham para você |
| Fortuna | Sorte do Avaliador (2, B) | +5% por grau de subir a raridade ao revelar |

- **Despertar:** ao virar **caçador D** você desperta e ganha uma **característica** sorteada (Comum 55%, Rara 33%, Épica 12%); no **B** ganha a segunda e no **S** a terceira. Valem nos portais. **Re-despertar** (Shift + clique, 48 de mana) sorteia todas de novo.
  - Comuns: **Pele de Aço** (-8% de dano dos monstros), **Olhos de Gato** (visão noturna), **Pés Ligeiros** (Velocidade I), **Mana Farta** (+20% de Cristal de Mana).
  - Raras: **Segunda Chance** (uma vez por portal, o golpe fatal te deixa com 3 ❤; o totem vem primeiro), **Vampírico** (cura 4% do dano causado), **Sangue Frio** (+12% de dano abaixo da metade da vida), **Instinto** (armadilhas brilham).
  - Épicas: **Caçador Nato** (+15% de XP de caçador), **Rei dos Baús** (+1 sorteio no tesouro final), **Inabalável** (imune a lentidão, fraqueza e veneno dos monstros), **Assassino** (+50% no primeiro golpe em monstro de vida cheia).
- **Mesa de Inscrição:** jogue **1 livro + 1 bolsa de tinta brilhante** numa **mesa de flecheiro**. Clique direito num tipo: escreve uma **Inscrição** (1 papel comum + mana). Com um **Cristal de Portal na mão**, clique esquerdo: aplica uma Inscrição que você tem. **Até 4 por cristal**:
  - um **modificador** (os 8 de sempre; 4 de mana nos que dão risco, 8 a 12 nos que ajudam);
  - **Tempo** (+3 minutos, até 2), **Tesouro** (+1 sala de tesouro, até 2), **Mina** (+1 Mina de Cristal, até 2);
  - **Objetivo** (vira Caçada, Saque ou Monólitos).
  - Os baús do vault às vezes trazem Inscrições (12%; o do chefe, 50%). O vault sempre deixa pelo menos 2 salas de luta além do chefe.

### Missão Diária e janelas do Sistema (2.73)

- **Missão Diária** ("Preparação para ficar forte", **sem punição**): num dia (real), **agachar 100 vezes, pular 100 vezes, correr 2 km e abater 20 monstros** (os números vêm das estatísticas do jogo). Concluída: **XP de caçador** (10 + 5 por rank) e **Cristais de Mana médios**; a cada **7 dias seguidos**, mais 1 Cristal de Mana grande e 1 Inscrição. **`/masmorra missao`** mostra o progresso.
- **Janelas do Sistema:** os avisos de rank novo, Despertar, Monarca e Missão Diária aparecem numa moldura azul no chat, como no anime.

### Um esquema só de classes (2.76)

A 2.73 tinha trazido "classes de caçador" separadas, com F duas vezes. Na 2.76 elas **se juntaram à árvore de classes** (`/classe`): uma classe só, as mesmas teclas (**Agache + F** usa a habilidade escolhida, **Agache + clique esquerdo no ar** troca) e as habilidades valem **em todo lugar**. **Dentro dos portais, as habilidades de classe ficam 20% mais fortes.**

- **Classes básicas novas:** **Tanque** (⛨, +3 ❤ e +4 de armadura; **Provocar** e **Muralha**) e **Curandeiro** (✙, +1 ❤, +30 de mana e +1 de mana/s, e a cada 5 s cura um pouco você e a party perto; **Cura** e **Purificar**). Elas seguem o caminho de sempre: tarefas e Prova num Santuário das Classes.
- **Habilidades novas em classes que já existiam:** Guerreiro ganhou **Disparada**, Berserker **Sede de Sangue**, Mago **Bola de Fogo** (não acende blocos), Elementalista **Nova de Gelo** e Assassino **Mutilação**. O Arqueiro já tinha Chuva de Flechas e Salto Evasivo, e o Ladino a Bomba de Fumaça (a antiga Furtividade).
- Os **Monarcas viraram classes lendárias** da árvore (veja abaixo). A árvore ganhou uma página **"✪ Lendárias e Monarcas"**, com o Soberano das Sombras no meio dos quatro Monarcas.

### Relíquias, Mural e os Monarcas (2.74)

- **Relíquias do Monarca:** 25 no total, **5 de cada Monarca** (ex.: "Coroa de Gelo"). Caem nos tesouros finais dos portais **A (6%)** e **S (12%)**.
- **Mural das Relíquias:** jogue **1 obsidiana chorona + 1 bloco de ouro** numa **pedra-negra polida entalhada**. Ele tem 5 linhas de 5: clique numa relíquia que falta (ou em "Colocar todas"). Qualquer um pode colocar. Se o mural for quebrado, as relíquias caem no chão.
- **Portal do Monarca:** com uma **linha completa**, um caçador **A ou S** abre ali o Portal daquele Monarca (as 5 relíquias se gastam): rank S, Caçada, **25 minutos**; suba no portal em até 30 s. O chefe é o próprio Monarca (2,5× a vida de um chefe S e mais dano). Tesouro: 2 Cristais de Mana perfeitos e a arma do chefe, sempre.
- **Monarcas** (desde a 2.76, **classes lendárias** da árvore, um dono por servidor, como o Soberano das Sombras): **quem dá o golpe final** (ou o dono do portal, ou outro da party) **herda a classe**, se ela está livre e a pessoa ainda não é Monarca. Ela vira a classe ativa, sobe de nível como as outras e usa as teclas de sempre (Agache + F). 30 dias sem entrar a liberam (como as outras lendárias); `/masmorra monarca abdicar` também.

| Monarca | Passivo | Habilidades |
|---|---|---|
| Chamas Brancas (Baran) | +3 ❤, +60 de mana, +15% de dano; 25% de raio branco nos golpes (+4 de dano) | **Relâmpago Branco** (18 s): raios em até 5 inimigos a até 12 blocos · **Chama Branca** (35 s): anel de fogo branco a até 7 blocos |
| Geada | +100 de mana, +2 de mana/s, magias 20% mais baratas; os golpes deixam lento | **Lança de Gelo** (12 s): no inimigo da mira (até 20 blocos), dano e congela · **Inverno Eterno** (40 s): dano e Lentidão IV a até 10 blocos |
| Feras (Rakan) | +3 ❤, +10% de velocidade e +15% de dano corpo a corpo | **Investida da Fera** (15 s): avança ferindo quem está no caminho · **Matilha** (45 s): 3 lobos lutam com você por 20 s |
| Insetos (Querehsha) | +10% de velocidade, 10% de esquiva, +10% de dano; os golpes envenenam | **Enxame** (20 s): Veneno II, cegueira e dano a até 8 blocos · **Carapaça** (40 s): Resistência II por 6 s; quem te bate leva 3 de dano |

- **Monarca da Destruição (Antares):** o **chefe final**. O portal dele pede **as 25 relíquias** no mural (todas se gastam). 4× a vida de um chefe S. Não vira classe: vencer é anunciado no servidor e dá 4 Cristais perfeitos, 3 Inscrições, uma peça de Caçador S e a arma do chefe.
- **`/masmorra monarcas`**: quem é cada Monarca agora.

### Câmara das Estátuas e Ataque de Rank S (2.75)

- **Masmorra Dupla:** às vezes um portal **E ou D** (5% dos portais do mundo, 3% dos cristais) esconde algo. Quando o chefe cai, uma **passagem escondida** se abre na sala dele e leva à **Câmara das Estátuas**, embaixo do vault. A masmorra espera até 15 minutos enquanto a câmara não termina. Os mandamentos estão nas placas da parede:
  1. **Adore o deus:** agache-se diante da estátua por 3 segundos (quem não fizer em 40 s é punido).
  2. **Louve o deus:** toque os **quatro sinos** (em até 60 s).
  3. **Prove a sua fé:** chegue ao **altar dourado**. A cada 7 s os olhos do deus **abrem** por 2,5 s (há um aviso 1 s antes): **quem se mexer** leva dano e volta 5 blocos.
- Quem entrou na câmara e chegou ao altar ganha o **Tesouro da Câmara** (1 ou 2 Relíquias do Monarca, Cristais de Mana grandes, Inscrições, uma peça de Caçador C e, às vezes, o Grimório Oculto), **100 XP de caçador** e o anúncio no servidor. A saída aparece em cima do altar.
- **Ataque de Rank S:** a cada **3 dias** (com 2 ou mais jogadores online), um **Portal do Ataque de Rank S** se abre perto de alguém e é anunciado para todos. **Qualquer party** entra. O chefe é **Beru, o Rei das Formigas**, e ganha **+60% de vida por caçador** na luta. Vencer: XP de caçador em dobro, **25% de chance de uma Relíquia do Monarca para cada um** e mais tesouro. Se ninguém fechar, ele **transborda em 1 hora**.

**Masmorras antigas:** a tabela abaixo é a base de cada rank (salas, monstros, chefe e o nível total para entrar). O custo em esmeraldas/diamantes deu lugar à oferenda do cristal.

| Dificuldade | Salas | Monstros | Chefe | Nível total | Custo | Tesouro |
|---|---|---|---|---|---|---|
| Fácil | 5–6, 2 ondas | zumbi, esqueleto, aranha | Golem ou Rainha | 0 | 8 esmeraldas | forjados de ferro, Raro a Épico |
| Normal | 7–8, 2 ondas (vida ×1,6) | + husk, stray, aranha da caverna, bruxa | Senhor das Chamas ou Lich | 110 | 2 diamantes | diamante, Épico a Único |
| Difícil | 9–10, 3 ondas (vida ×2,4) | esqueleto do Wither, blaze, vingador, saqueador... | Tempestade Viva | 330 | 6 diamantes | diamante, Único a Lendário |
| Pesadelo | 11–13, 3 ondas (vida ×3,5) | + invocador, piglin bruto, devastador | Arauto do Fim | 550 | 1 lingote de netherite | netherite, Lendário a Mítico |

**Como é lá dentro:**
- Toda masmorra é **diferente**: a planta é sorteada (uma árvore de salas) e o **tema** também (Cripta, Caverna Profunda, Fornalha Infernal ou Palácio Gelado).
- **Salas de combate:** quando você entra, as **grades descem** e vêm as **ondas de monstros**, mais fortes com mais gente na party. Limpou, as portas abrem, a party ganha XP de Combate e às vezes aparece um baú.
- **Salas de tesouro:** baús com esmeraldas, diamantes, livros encantados, Fragmentos de Forja, Pedras de Proteção e **equipamentos forjados**.
- **Sala do chefe:** a maior, sempre na ponta mais longe da entrada. Vencer dá o **baú do chefe** (com equipamento forjado garantido), os drops do chefe (núcleo etc.), muito XP e um **portal de saída**.
- No topo da tela aparecem as salas limpas e o tempo que falta (40 min). Pérola do ender e chorus não funcionam, e ninguém quebra nem coloca blocos lá dentro.

**Morreu lá dentro?** Você volta para onde entrou, sem perder os níveis, e os itens vão para o **Cofre das Almas**. Clique em qualquer Portal da Masmorra e pague **2 diamantes** para recuperar as coisas de cada morte.

**Sair:** `/masmorra sair` (sem perder nada), pelo portal de saída no fim, ou quando o tempo acaba. Se o servidor reiniciar, quem estava lá volta para onde entrou.

### Salas construídas por você

Você constrói salas e o plugin as sorteia no meio das geradas (60% de chance quando há salas do tipo).
1. `/rpgadmin sala modelo combate` monta uma **moldura de vidro** do tamanho certo, 2 blocos à sua frente, e já marca os cantos. As partes verdes são onde as portas vão abrir.
2. Construa a sala dentro da moldura (paredes, chão, teto, armadilhas, decoração).
3. Coloque **placas** como marcas (a primeira linha é o tipo):
   - `[monstro]`: lugar onde os monstros nascem. Na 2ª linha dá para pedir um tipo, ex.: `zombie`, `blaze`.
   - `[bau]`: baú de tesouro.
   - `[chefe]`: onde o chefe aparece.
   - `[entrada]`: onde a party chega.
4. `/rpgadmin sala salvar combate cripta_do_rei` salva a sala, que já entra no sorteio.

| Tipo | Tamanho (com paredes) |
|---|---|
| `inicio`, `combate`, `tesouro` | 19 × 9 × 19 (largura × altura × comprimento) |
| `chefe` | 27 × 12 × 27 |

As portas abrem no meio de cada parede (3 de largura, 4 de altura, a partir do chão). As salas ficam em `plugins/RPGAtributos/salas/<tipo>/<nome>.nbt`. Para levar salas para outro servidor, basta copiar os arquivos.

## Mundo perigoso

- **Monstros de Elite:** às vezes um monstro nasce Elite (1 modificador), **Campeão** (2) ou **Senhor** (3). Ele tem nome com estrelas, barra de vida, muito mais vida e dano, e loot melhor: esmeraldas, Pó Arcano, gemas, Fragmentos de Forja e, raramente, um Tomo Proibido. Matar um Senhor é anunciado no servidor.

| Modificador | Efeito |
|---|---|
| Veloz | 40% mais rápido |
| Blindado | Muita armadura e não é empurrado |
| Vampiro | Se cura com o dano que causa |
| Explosivo | Explode ao morrer (sem quebrar blocos) |
| Invocador | Chama ajudantes durante a luta |
| Gigante | Enorme: muita vida e dano, mais lento |
| Fantasma | Fica invisível e teleporta para as suas costas |
| Venenoso / Gélido / Incendiário | Golpes envenenam, congelam ou incendeiam |
| Antimagia | Magias causam 70% menos dano nele |
| Regenerante | Recupera vida sem parar |
| Enfurecido | Com pouca vida fica muito mais forte |

- **O mundo envelhece:** a cada semana do servidor, os monstros ficam +5% mais fortes (até +50%) e os Elites, mais comuns. Ajustável no config.
- **Ninhos de monstros:** às vezes surge um ninho perto de quem está explorando (fora dos territórios), e você recebe um aviso da direção. O ninho solta ondas de guardiões enquanto há alguém perto. Derrote os guardiões e quebre o núcleo para pegar o saque. Somem sozinhos depois de 2 horas.
- **Ataques à colônia:** em algumas noites, com alguém da colônia por perto, os moradores tocam o sino e **1 minuto depois uma horda ataca**. Ela é mais forte conforme o nível da colônia (no nível 5 vem até um Ravager), sempre com um capitão Elite. Os soldados ajudam. Vencer põe saque no depósito e deixa todos felizes; se a horda durar até o amanhecer, ela recua e a colônia fica triste.
- **Chefe Mundial:** 2 vezes por semana (com pelo menos 2 jogadores online), um dos chefes do Altar aparece **muito mais forte** num lugar do mundo. O servidor é avisado com as coordenadas e 10 minutos de antecedência, e ele espera até alguém chegar. O saque inclui Essência Primordial, gemas Lapidadas e 50% de chance de Tomo Proibido, para todos que lutaram.
- **Bestiário** (`/bestiario`): conta os monstros que você derrotou. Nos marcos de 10, 50, 200, 500 e 1000 abates você causa +3% de dano e recebe -2% de dano daquele tipo, por marco (Elites contam 5).

## Mundo vivo

**Estações do ano** (`/calendario`): cada estação dura **uma semana real** (muda em `mundo-vivo.horas-por-estacao`; o placar e o `/calendario` mostram quanto falta) e o ano recomeça a cada 4 semanas.

| Estação | O que muda |
|---|---|
| ✿ Primavera | Plantações crescem 40% mais rápido; Koi Celeste e Truta mais comuns; Elites Regenerantes |
| ☀ Verão | Ondas de calor e tempestades; Baiacu-Rei e Carpa Dourada; Elites Incendiários |
| ☘ Outono | Colheitas um pouco mais rápidas, ventanias e neblina; Salmão-Rei e Peixe-Fantasma; Elites Fantasmas |
| ❄ Inverno | Neve caindo, **neve no chão e água congelando** perto dos jogadores (tudo some sozinho na primavera e nunca dentro dos territórios); plantações ao ar livre quase param, mas **estufas** (com teto) crescem normal; Peixe-Gelo; Elites Gélidos |

**O chão muda com a estação** (perto dos jogadores, fora dos territórios, e some quando a estação acaba):
- ✿ **Primavera:** pétalas e flores silvestres brotam na grama, e pétalas voam no ar.
- ☀ **Verão:** arbustos de vaga-lumes aparecem e, de noite, vaga-lumes voam em volta.
- ☘ **Outono:** folhas laranja, vermelhas e marrons caem das árvores e se juntam no chão debaixo delas.
- ❄ **Inverno:** neve no chão e água congelando.

### Coisas para colher

Cada estação espalha **3 coisas** pelo mundo, aos poucos, perto de quem explora (até 4 por jogador num raio de 32 blocos, conforme o bioma; elas aparecem devagar). Procure o **brilho verde** no chão e clique (ou bata) para pegar (às vezes vêm 2). Todas **se comem** e dão um efeito pequeno; somem quando a estação acaba. Dão XP de Agricultura.

| Estação | Coletas |
|---|---|
| ✿ Primavera | Cogumelo-Morel (florestas, regeneração), Alho-Selvagem (resistência), Broto de Samambaia (velocidade) |
| ☀ Verão | Amora-Silvestre (velocidade), Alga-Doce (praias e rios, fôlego), Groselha (sorte) |
| ☘ Outono | Castanha (saciedade), Cogumelo-do-Bosque (visão noturna), Avelã (pressa) |
| ❄ Inverno | Raiz de Inverno (esquenta), Cogumelo-de-Neve (regeneração), Fruto de Zimbro (força) |

### Plantações da estação

**20 plantas**, 5 por estação (algumas em duas estações), com visual próprio crescendo em 4 fases em cima da terra. Plante clicando com as sementes na **terra arada**. Só crescem **na estação delas** e com a **terra molhada** (água perto ou chuva; na chuva crescem mais rápido), mais ou menos 12 minutos por fase. **Farinha de osso** adianta uma fase. Clique na planta madura para colher; bater numa planta ainda crescendo arranca ela (a semente volta). As sementes saem do **mato quebrado** (4%) na estação certa, e às vezes voltam na colheita.

| Estação | Plantas |
|---|---|
| ✿ Primavera | Morango (rebrota, velocidade), Couve-Flor (gigante, regeneração), Pastinaca (rápida, sacia), Alho (resistência), Feijão-Verde (rebrota, pressa) |
| ☀ Verão | Mirtilo (rebrota, visão noturna), Pimenta (força), Tomate (rebrota, regeneração), **Milho** (verão e outono, rebrota, sacia), Melão (gigante, refresca) |
| ☘ Outono | Uva (rebrota, sorte), Abóbora-Moranga (gigante, absorção), Berinjela (rebrota, força), Inhame (absorção), Oxicoco (rebrota, sorte) |
| ❄ Inverno | Couve Gelada (resistência), Nabo-de-Neve (esquenta), **Cebola** (inverno e primavera, visão noturna), Rabanete-Gelado (rápido, velocidade), Alho-Poró (regeneração) |

- **Tempo:** cada planta tem o seu (a pastinaca e o rabanete são rápidos, o milho e o melão demoram).
- **Estufa:** com **teto de vidro** em cima da planta (o primeiro bloco acima dela, até 12 de altura), ela cresce em **qualquer estação** e não murcha.
- **Murchar:** quando a estação troca, a planta que não é da estação nova **murcha** (como em Stardew Valley; só sai arrancando). Desliga com `mundo-vivo.plantas-murcham-fora-de-epoca`.
- **Gigantes:** 9 couves-flores, melões ou abóboras-morangas maduras num quadrado 3×3 podem virar **uma gigante** (rende 15 a 21 de uma vez e sementes).

A colheita sai com qualidade ★ (pelo nível de Agricultura) e dá XP de Agricultura. Fora de época (sem estufa), a planta para de crescer e murcha na troca de estação. Pisar e desfazer a terra arada perde a planta (a semente volta).

**Festivais:** no 4º dia de cada estação (Festa das Flores, Festival do Sol, Festa da Colheita, Festival do Gelo) há +25% de XP nos atributos do tema, e cada jogador ganha uma **lembrança**, um enfeite de cabeça para o guarda-roupa.

**Clima:** de vez em quando acontece um evento de 4 minutos, sorteado pela estação:
- **Nevasca:** lentidão ao ar livre.
- **Neblina:** não dá para ver longe.
- **Ventania:** desvia flechas e projéteis.
- **Tempestade elétrica:** raios caem em quem está **com metal na mão** ao ar livre.
- **Onda de calor:** cansa e dá fome mais rápido.

**Lua e céu:**
- **Lua cheia:** monstros +20% de dano e **lobisomens**.
- **Lua nova:** magias +15% e **vampiros**.
- **Lua de Sangue** (às vezes, na lua cheia): monstros mais fortes e rápidos, 3× mais Elites e o dobro de XP.
- **Chuva de meteoros:** meteoritos caem pelo mundo; quebre para pegar minério, diamante e Fragmentos.
- **Eclipse** (raro, ao meio-dia): fica escuro por 2,5 minutos; magias +30% e mais Elites.
- **Aurora** (noites de inverno): mana extra para quem está ao ar livre.
- **Arco-íris (2.58):** quando a chuva para de dia, 40% de chance de um arco-íris aparecer na frente de quem está ao ar livre (`mundo-vivo.arco-iris` e `chance-arco-iris`). Ele fica 3 minutos e, **onde a ponta da direita toca o chão**, brilha o **Pote do Arco-Íris**: quem pegar primeiro leva esmeraldas, ouro e às vezes um pacote de cartas, um mineral de bioma ou um diamante. 3 potes: título **Caçador de Arco-Íris**.
- **Estrelas cadentes (2.58):** em noites limpas, uma a cada uns 4 minutos (`mundo-vivo.estrelas-cadentes-por-minuto`). Quem está ao ar livre vê a estrela riscar o céu e tem **6 segundos para agachar olhando para cima** e fazer um pedido: ganha uma bênção até o amanhecer (Sorte, Velocidade, Regeneração, visão noturna, Resistência ou Pressa). Um pedido por noite. 10 pedidos: título **Pedido às Estrelas**.
- **O Nian** 年 (noites de inverno, 25% por noite, `perigo.chance-nian-por-noite`): a fera do fim do ano desce das montanhas perto de alguém a céu aberto. Ele é enorme e bate forte, e **arma nenhuma o derruba** (só 20% do dano entra e ele nunca morre). Ele só **foge com medo**: fogos de artifício estourando perto dele (com ou sem estrela), fogo e lava, explosões e o som dos **chifres de cabra**. A barra mostra a coragem dele. Espantado, ele deixa 1 Essência Primordial, 2 blocos de ouro, 12 esmeraldas e 16 fogos; quem ajudou ganha o título **Espanta-Nian** e uma página no diário. Se amanhecer antes, ele volta sozinho para as montanhas.

**Temperatura** (leve, dá para desligar no config):
- **Frio:** na nevasca, em biomas gelados à noite e nas noites de inverno, ao ar livre. Dá lentidão. Protegem 2 peças de couro, uma fogueira ou fornalha perto, ou resistência ao fogo.
- **Calor:** no deserto, savana e terras áridas de dia no verão, e no Nether. Dá fome mais rápido. Protegem resistência ao fogo ou estar na água.

**Maldições** (`/maldicao`): a mordida de um lobisomem ou vampiro pode passar a maldição. Quem não quiser cura com o **Elixir da Purificação** (Bancada Alquímica).
- 🐺 **Licantropia:** na lua cheia (ou à noite com `/maldicao transformar`) vira lobisomem, com força II, velocidade II, pulo, regeneração e visão noturna. Transformado fica **sem armadura e sem magias**, e armas de ouro causam +50% de dano nele.
- 🦇 **Vampirismo:** rouba 15% do dano corpo a corpo como vida e, à noite, tem visão noturna e velocidade. O **sol queima** se estiver ao ar livre sem capacete.

## Portais, Soberano das Sombras e Torre Infinita

**Portais** (`/portais`): de vez em quando (a cada 30 min, 60% de chance, até 3 ao mesmo tempo) um portal se abre **perto de alguém que está explorando**, fora dos territórios. O servidor é avisado com as coordenadas. Cada portal tem uma dificuldade (Fácil, Normal, Difícil ou Pesadelo, este bem raro) e uma placa flutuante com o tempo que falta.
- **Entrar:** ande para dentro do anel. Na primeira vez ele "desperta" e forma uma masmorra; entre de novo em alguns segundos. **Qualquer um pode entrar** (precisa do nível total da dificuldade). Morrer lá dentro funciona como nas masmorras (Cofre das Almas).
- **Fechar:** vença o chefe da masmorra. O baú do chefe vem com **tesouro a mais**, todos ganham XP de Combate e o portal some do mundo.
- **Transbordar:** se ninguém fechar a tempo (Fácil 3h, Normal 2h30, Difícil 2h, Pesadelo 1h30), o portal **transborda**: quem estava dentro é jogado para fora e saem **ondas de monstros** (mais uma por dificuldade) e depois **um chefe**. Os monstros caçam quem estiver perto e marcham para a **colônia mais próxima** (até 160 blocos), que fica com medo. Vencer o chefe contém o transbordo e deixa um baú; se ninguém conseguir em 30 minutos, o portal se esgota e a região fica em ruínas.

**☾ Soberano das Sombras** (classe lendária, só um no servidor): alguns Portais Pesadelo trazem o **Eco do Soberano** (só enquanto a classe estiver livre). Quem fechar esse portal e tiver **uma classe avançada dominada** desperta a classe (primeiro quem matou o chefe, depois o dono, depois os outros que estavam lá dentro). Se o portal transbordar, o Eco se perde.
- **Passivo:** +3 ❤, +60 de mana, +15% de dano corpo a corpo.
- **Levante-se** (agache + F): os inimigos que você (ou suas sombras) derrotou nos últimos 30 segundos, perto de você, **se levantam como sombras** e entram no seu exército para sempre. Chefes viram **Marechais**, muito mais fortes; Elites viram sombras de Elite.
- **Exército das Sombras:** chama as sombras guardadas; se já estão lutando, ficam furiosas (Força II, Velocidade II, cura) e atacam o que você olha.
- O exército guarda **5 + nível da classe** sombras e luta com até **2 + nível/3** ao mesmo tempo. Sombra que cai volta para a sua sombra (dá para chamar de novo). Elas seguem você, não atacam aliados nem jogadores (só em guerra entre reinos ou se alguém te atacar) e não queimam no sol. `/sombras` mostra o exército (shift + clique liberta uma).

**Torre Infinita** (`/torre`): jogue **1 olho do ender** e **1 bloco de ouro** em cima de uma **obsidiana chorona** para criar um **Obelisco da Torre**. Clique nele para subir com a sua party (quem estiver a até 10 blocos).
- Cada party sobe numa **arena só dela**. Cada andar tem ondas de monstros, +7% de vida e +3,5% de dano a cada andar; o visual muda a cada 10 andares.
- **A cada 10 andares:** um chefe do Altar e um **baú** no centro (cada vez melhor).
- Dá para recomeçar do andar 11, 21, 31... até onde você já chegou.
- **Morrer na Torre não perde nada:** a subida só acaba. `/torre sair` sai a qualquer hora.
- **Ranking** do andar mais alto: **da semana** (o campeão é anunciado quando a semana vira) e **de todos os tempos** (`/torre ranking`).

## Combos de arma

Com uma arma na mão, **3 cliques seguidos** soltam um golpe (`D` = clique direito, `E` = clique esquerdo). Cada clique tem até 0,9 s para o próximo; a barra de ação mostra o progresso (`D · E · ?`).
- **Espada, machado, lança, tridente e maça** começam com o **direito** (o esquerdo continua sendo o ataque normal): `D D D`, `D E D`, `D D E` e `D E E`.
- O clique direito num **monstro** ou no **Boneco de Treino** também conta como `D`: mirar neles não quebra a sequência.
- **Arco e besta** começam com o **esquerdo**, porque o direito puxa a flecha: `E E E`, `E D E`, `E E D` e `E D D`.
- **Arsenal novo (2.51):** adaga, Lâminas Duplas (uma adaga em cada mão), foice, bastão e chicote começam com o direito; o **Arremesso** (faca e machadinha) começa com o esquerdo, porque o direito arremessa.
- Agachado não solta combo (agachar continua sendo das habilidades de classe). Clicar com o direito em baú, porta ou estação não conta. Um clique só ainda levanta o escudo.
- **Vigor:** cada golpe gasta vigor (barra amarela no topo). Ele volta sozinho, bem mais rápido fora de luta. O máximo é 100 + 0,5 por nível de Combate. Cada golpe também tem uma recarga curta.
- **Proficiência:** cada tipo de arma tem um nível de 1 a 20, que sobe acertando e derrotando inimigos com ela (mobs de spawner dão menos). Cada nível dá +2% de dano nos golpes e libera golpes novos.
- `/combos` abre o menu: escolha qual golpe fica em cada uma das 4 sequências (clique na sequência, depois no golpe).
- O dano sai como golpe seu, então passivos de classe, efeitos da forja, raridade, refino e reações elementais também valem.
- **Alvos:** os golpes, as habilidades de classe e as magias acertam monstros, chefes, qualquer outra criatura, o **Boneco de Treino** (que mede o dano e volta para o lugar) e jogadores. **Jogadores:** fora de território vale tudo, mesmo com o PvP do servidor desligado (`combate.pvp-livre-fora-de-territorio`); dentro de território vale a regra de PvP dele e a Lei da Paz do reino. Party sem fogo amigo e membros do mesmo reino nunca se acertam; na guerra, os inimigos sempre. Ficam de fora aldeões e NPCs, os seus pets e companheiros, os de quem você não pode atacar e animais com etiqueta de nome. O admin pode limitar a monstros com `combate.golpes-em-qualquer-criatura: false`.

| Arma | Golpes (nível de proficiência) |
|---|---|
| ⚔ Espada | Estocada (1), Corte Giratório (3), Lâmina Cruzada (5, sangra), Execução (8, x4 em quem está com pouca vida), Tempestade de Lâminas (12), Passo Fantasma (16), Julgamento da Lâmina (20) |
| ✠ Machado | Salto Esmagador (1), Arremesso do Machado (3, volta), Rachar Escudo (5, +25% de dano no alvo), Fúria do Lenhador (8), Redemoinho de Aço (12), Decapitar (16), Cisão da Terra (20) |
| ↟ Lança | Estocada Longa (1), Varredura (3), Arremesso da Lança (5), Salto do Dragão (8), Muralha de Lanças (12), Perfuração (16), Lança Celeste (20) |
| ♆ Tridente | Onda (1), Puxão (3), Redemoinho (5), Tridente do Trovão (8), Maré Alta (12, ajuda a party), Mergulho (16), Fúria de Poseidon (20) |
| ⚒ Maça | Abalo Sísmico (1), Martelo Ascendente (3), Atordoar (5), Quebra-Armadura (8), Salto Meteoro (12, mais dano quanto maior a queda), Eco do Impacto (16), Martelo dos Deuses (20) |
| ➹ Arco e besta | Tiro Triplo (1), Flecha Perfurante (3), Salto Tático (5), Flecha Explosiva (8), Saraivada (12), Flecha Sombria (16, persegue o inimigo), Tiro Celestial (20) |

**Golpes de lenda:** cada uma das 12 lendas tem um golpe só dela, que aparece no `/combos` e vai em qualquer sequência. Lenda que é arma precisa estar na mão; lenda de armadura ou a picareta só precisa estar com você.

| Lenda | Golpe |
|---|---|
| Lâmina do Exorcista | Purificação Sagrada: explosão de luz, dano enorme em mortos-vivos e cura os aliados |
| Presa da Tecelã | Teia da Rainha: cone de teias que prende e envenena |
| Machado do Lenhador Eterno | Queda da Floresta: um tronco gigante cai numa linha de 9 blocos |
| Picareta do Abismo | Colapso do Abismo: o chão afunda e prende os inimigos em volta |
| Arco da Tempestade | Tempestade de Raios: 6 raios onde você olha |
| Coroa do Fim | Vazio do Fim: puxa os inimigos para um ponto e explode |
| Égide do Guardião | Égide: Absorção IV por 6 s e quem te bate leva metade do dano de volta |
| Botas do Andarilho | Passo do Andarilho: avança 12 blocos atravessando inimigos e ganha Velocidade III |
| Lâmina do Carrasco | Sentença: executa quem está com menos de 25% de vida (chefes levam dano x5) |
| Martelo do Forjador Lendário | Bigorna Celeste: uma bigorna de luz cai no alvo e atordoa |
| Lâmina do Arquimago | Lâmina Arcana Suprema: 5 projéteis que perseguem os inimigos e devolvem mana |
| Elmo do Senhor das Feras | Chamado da Alcateia: 3 lobos espectrais lutam por 20 s |

**Golpes de classe:** cada classe avançada tem um golpe para a arma dela, que vale enquanto ela (ou uma lendária que nasce dela) estiver ativa: Berserker — Fúria Sangrenta (machado), Cavaleiro — Carga do Cavaleiro (lança, o dobro montado), Paladino — Martelo Sagrado (maça), Atirador de Elite — Tiro na Cabeça (arco), Caçador — Flechas da Matilha (arco, os companheiros atacam o alvo), Elementalista — Prisma Elemental (tridente), Feiticeiro de Batalha — Lâmina Rúnica (espada), Assassino — Mil Cortes (espada), Mestre Forjador — Martelo Incandescente (maça), Mestre das Feras — Investida da Fera (lança).

**Finalizador em cadeia:** acertar **3 golpes de combo diferentes em 10 s** carrega o finalizador. O próximo combo (até 8 s depois) sai com +50% de dano e uma explosão em volta.

**Lança:** o clique esquerdo da lança é a estocada dela, que não é um balanço normal do braço; o plugin percebe a estocada pela força do ataque e conta como `E` normalmente.


### Raças (2.52)

Use **`/raca`** para escolher: **Anão** (65% do tamanho), **Elfo** (110%), **Pequenino** (50%), **Povo-Pássaro** (90%), **Povo do Mar**, **Humano** e, desde a 2.53, **Draconato**, **Homem-Fera**, **Espectral** e **Gnomo Inventor** (60%). O tamanho muda de verdade (todos veem, sem mod). Cada raça tem uma fraqueza e **5 estágios de Despertar**: a **Essência** vem do XP dos atributos da raça (cheia nos dela, 20% nos outros) e cada estágio pede uma oferenda. A primeira escolha é de graça; trocar custa 30 níveis e volta ao estágio I.

| Raça | Essência de | Fraqueza | I → V |
|---|---|---|---|
| **Humano** | todo XP (metade) | nenhuma | +10% XP → +10 Vigor → +20% XP → +2 ❤ → Herança (dom de outra raça) |
| **Anão** | Mineração, Ferraria | -8% velocidade | +2 ❤ e minera 10% mais rápido → visão nas cavernas → minera 25% mais rápido e resiste a empurrão → +2 ❤ e minério dobrado (8%) → Pressa nas cavernas e 20% dobrado |
| **Elfo** | Madeira, Corrida, Arcano | -2 ❤ | 6% mais rápido → +15% com flechas → Sentidos Élficos (agache 2 s: monstros brilham) → pulo e 12% de velocidade → cura nas florestas e +30% com flechas |
| **Pequenino** | Agricultura, Culinária, Pesca | -4 ❤ | sorte → comida enche mais → agachado, monstros longe não notam → 10% mais rápido e sorte 3 → 12% de desviar |
| **Povo-Pássaro** | Pulo, Corrida | -2 ❤; armadura pesada no peito prende as asas | sem dano de queda até 8 blocos → **planar** (pule no ar) → **asas** (pule planando para bater, gasta Vigor) → mais rápido → **voo** (segure o pulo planando) |
| **Povo do Mar** | Natação, Pesca | fraco no Nether e nos desertos | fôlego e nado → visão debaixo d'água → Graça do Golfinho → respira debaixo d'água → +25% de dano na água ou na chuva |

As asas do Povo-Pássaro aparecem do estágio III em diante, enquanto ele plana ou voa: com o pacote de recursos são **asas de verdade** que batem; sem o pacote, asas desenhadas com **partículas**.

#### A segunda onda (2.53)

| Raça | Tamanho | Essência de | Fraqueza | I → V |
|---|---|---|---|---|
| **Draconato** | 105% | Combate, Ferraria, Arcano | lento e fraco no frio (neve, gelo, biomas gelados) | metade do dano de fogo e lava → **Sopro de Fogo** → **asas curtas** (planar) → +2 ❤ e imune a fogo/lava → sopro mais longo e golpes que incendeiam |
| **Homem-Fera** | 105% | Combate, Corrida, Doma | armas de ouro ferem 25% mais | visão noturna e +4% de velocidade → **garras** (mãos vazias +5 de dano) → **Uivo** → regenera e corre à noite → imune à licantropia e Força à noite (II na lua cheia) |
| **Espectral** | 100% | Arcano, Alquimia, Pulo | Fraqueza ao sol | cai devagar e enxerga à noite → mortos-vivos só notam de perto → **Desvanecer** (invisível 8 s) → golpes deixam lento e roubam vida → **Além do Véu** (volta da morte 1×/hora) |
| **Gnomo Inventor** | 60% | Ferraria, Alquimia, Mineração | -4 ❤ | ferramenta da mão se conserta → 15% de não gastar durabilidade → explosivos e frascos +30% sem ferir você → **Autômato** de ferro por 40 s → +10% de raridade na Forja |

- **Habilidade da raça:** **agache duas vezes rápido**. Sopro de Fogo (Draconato II, gasta 20 de Vigor), Uivo (Homem-Fera III: Força para você e a party, espanta os monstros), Desvanecer (Espectral III) e Autômato (Gnomo IV).
- **Sinergia de raça e classe:** com uma classe que combina com a sua raça ativa (ex.: Anão + Ferreiro, Elfo + Arqueiro, Draconato + Berserker, Espectral + Assassino), **+6% de dano e -5% de dano recebido**. O `/raca` mostra quais são.
- **Títulos de raça:** despertar o estágio V dá o título da raça: Coração Humano, Filho da Montanha, Guardião da Floresta, Sorte em Pessoa, Senhor dos Céus, Voz do Oceano, Sangue de Dragão, Alfa da Lua, Andarilho do Véu e Grande Inventor.
- **Enfeites de cabeça:** no `/raca` → **Enfeites de cabeça**, de graça: Orelhas Élficas, Barba de Anão, Chifres de Dragão, Barbatanas, Orelhas de Lobo, Óculos de Inventor e Auréola Espectral. Vista com o clique direito, ou ponha no **Guarda-roupa** por cima do capacete (o capacete some e o enfeite aparece). Precisam do pacote de recursos.
- O Humano no estágio V também pode escolher a Herança das raças novas.

### O arsenal novo (2.51)

Seis armas novas, cada uma com **proficiência, 7 golpes e uma Postura Mestra**. Todas usam a base de espada do jogo (madeira, pedra, cobre, ferro, ouro, diamante e netherite pela mesa de ferraria), são feitas na **bancada** e entram na **Forja do Ferreiro** como as outras armas (raridade, status, gemas, runas, refino). `/rpgadmin arma <tipo> [material] [forjar]` dá uma pronta.

| Arma | Receita (M = material) | Jeito | Postura Mestra |
|---|---|---|---|
| **Adaga** | `M` e graveto na diagonal | Rápida (2,4 golpes/s), -2 de dano. **Pelas costas: +50%.** Uma em cada mão = **Lâminas Duplas**: às vezes a segunda adaga acerta junto | **Sombra**: +10% de dano e velocidade, pelas costas x1,8. Duplas: **Tempestade Gêmea** (a segunda adaga acerta sempre) |
| **Faca de Arremesso** | `M` sobre pena | **Clique direito arremessa** (dano x1,3); a faca fica no chão brilhando e só você pega (volta sozinha em 1 min) | **Olho de Águia**: arremessos +25% e a arma volta para a mão |
| **Machadinha** | `MM` / graveto e pena | Arremesso pesado (dano x1,7) | Olho de Águia |
| **Foice** | `MMM` e 2 gravetos | Corte largo, +1 de dano, +0,5 de alcance; cada inimigo derrotado cura 1,5 ❤ | **Ceifador**: +10% de dano, cada abate cura 3 ❤ e dá Força |
| **Bastão** | `M` e 2 gravetos na diagonal | +1 de alcance e mais empurrão | **Monge**: -15% de dano recebido e desequilibra 50% mais |
| **Chicote** | `M`, couro e graveto | **+3 de alcance** | **Domador de Feras**: +10% de dano e cada acerto deixa lento |

Golpes (nível de proficiência 1, 3, 5, 8, 12, 16 e 20):
- **Adaga:** Punhalada (x2 pelas costas), Corte Venenoso, Rolamento, Nuvem de Fumaça, Dança das Lâminas, Sombra Assassina, Marca da Morte.
- **Lâminas Duplas:** Corte Duplo, Redemoinho Gêmeo, X Cruzado, Investida Gêmea, Chuva de Aço, Dança do Vento, Ciclone Carmesim (rouba vida).
- **Arremesso:** Leque de Facas, Ricochete, Lâmina que Prende, Machadinha Giratória, Chuva de Lâminas, Lâmina Explosiva, Tempestade de Aço.
- **Foice:** Ceifar, Gancho da Foice, Colheita (cura), Foice Giratória, Ceifa das Almas, Passo do Ceifador, Colheita Final (executa quem tem menos de 20%).
- **Bastão:** Varredura Baixa, Estocada do Bastão, Salto com Vara, Moinho, Bastão Sísmico, Defesa do Monge, Pilar Celeste.
- **Chicote:** Chicotada, Laço, Desarmar, Estalo, Balanço (puxa você até um bloco), Chicote de Fogo, Serpente.

Títulos: **Sombra das Lâminas** (Lâminas Duplas no 20), **Ceifador** (foice no 20) e **Mestre do Arsenal** (12 armas no 20).

### As armas dos minérios (ajustes da 2.51)

Oito armas feitas com os minerais do mundo (a oitava, a Lança de Basalto, é do Nether, 2.56), cada uma com **mecânica própria, 7 golpes e Postura Mestra**. Têm **uma receita só** (com o mineral do plugin), nome e base fixos, e entram na **Forja do Ferreiro** como as outras (raridade, status, gemas, runas, refino). O Montante (base de diamante) ainda vira Netherite na mesa de ferraria. `/rpgadmin arma <tipo> [forjar]` dá uma pronta.

| Arma | Receita | Jeito | Postura Mestra |
|---|---|---|---|
| **Montante de Mitrilo** | 2 Lingotes de Mitrilo em pé, embaixo diamante, graveto e diamante | +4 de dano, lento (0,8 golpe/s). **Duas mãos:** varre quem está perto do alvo (40% do golpe); com algo na outra mão, -40% de dano | **Titã**: +20% de dano e a varredura vai a 70% |
| **Bumerangue de Âmbar** | Tábua e Âmbar em cima, tábua embaixo à direita | **Clique direito arremessa:** vai até 11 blocos (ou até bater), volta para a mão e acerta na ida e na volta | **Vento que Volta**: +30% de dano e a volta acerta com o dobro |
| **Leques de Guerra de Jade** | Jade, papel e Jade; graveto embaixo | Rápidos (2 golpes/s), +0,5 de alcance; às vezes uma **rajada de vento** afasta quem está em volta | **Garça de Jade**: +10% de dano, +15% de velocidade e a rajada sai em todo golpe |
| **Arpão de Pérola** | Pérola, lingote de ferro e graveto na diagonal | +1 de alcance. **Clique direito arpoa:** acertando alguém, puxa até você; acertando um bloco, puxa **você**. Na água, dano x1,5 | **Baleeiro**: arpão +30% de dano e puxa com o dobro da força |
| **Mangual de Gelo** | Cristal de Gelo, corrente e graveto na diagonal | +2 de dano; cada golpe deixa lento e às vezes **congela** | **Avalanche**: +10% de dano e lentidão forte em todo golpe |
| **Garras de Pedra-Sangue** | 2 Pedras-Sangue em cima, 3 lingotes de ferro embaixo | Muito rápidas (2,6 golpes/s); **cada golpe rouba 10% do dano em vida** | **Fera Sangrenta**: +10% de dano e o dobro de roubo de vida |
| **Clava Fumegante** | Turfa, tronco e graveto em pé | +2 de dano e empurrão. Com **Turfa** no inventário os golpes queimam (1 Turfa a cada 8 queimas) | **Fornalha**: +10% de dano e sempre queima, sem gastar Turfa |
| **Lança de Basalto** (2.56) | Cristal de Magma, basalto polido e vara de blaze na diagonal | +2 de dano e +1,5 de alcance. **Todo golpe põe fogo**; em quem já está queimando, +25% de dano | **Vulcão**: +10% de dano, fogo mais longo e +40% em quem queima |

Golpes (nível de proficiência 1, 3, 5, 8, 12, 16 e 20):
- **Montante:** Golpe Pesado, Varredura do Titã, Investida do Montante, Lâmina Erguida, Corte do Horizonte, Fúria do Gigante, Queda da Montanha.
- **Bumerangue:** Arco de Âmbar, Duplo Retorno, Pancada Atordoante, Órbita, Sol do Deserto, Ricochete de Âmbar, Tempestade de Areia.
- **Leques:** Corte do Leque, Rajada de Vento, Dança da Garça, Pétalas de Jade (cura), Vendaval, Leque Espelho, Furacão de Jade.
- **Arpão:** Arpoar, Estocada do Arpão, Corrente Marinha, Mergulho do Tubarão, Arpão Triplo, Onda Quebrando, Fúria do Leviatã (mais fortes na água).
- **Mangual:** Golpe Gelado, Giro da Corrente, Quebra-Gelo, Corrente que Prende, Nevasca, Prisão de Gelo, Avalanche.
- **Garras:** Rasgar, Salto da Fera, Sangria, Frenesi, Garras Cruzadas, Predador (marca o alvo), Banquete de Sangue.
- **Clava:** Pancada em Brasa, Fumaça Sufocante, Brasas Espalhadas, Golpe Sísmico, Chama do Pântano, Fornalha Viva, Erupção.
- **Lança de Basalto:** Estocada de Magma, Muralha de Basalto, Chuva de Cinzas, Salto do Strider, Rio de Lava, Pele de Obsidiana, Lança do Inferno.

Bumerangue e Arpão começam os combos com o **esquerdo** (o direito arremessa); as outras, com o direito.

Com o pacote de recursos, as 8 ficam em **3D na mão** (no inventário, o desenho). A adaga, a faca, a machadinha, a foice e o bastão da bancada têm **desenho por material**: a lâmina (ou as ponteiras do bastão) pega a cor da madeira, pedra, cobre, ferro, ouro, diamante ou netherite.

**Incrustar minério:** com uma arma corpo a corpo na mão e um mineral na outra, clique na **Forja do Ferreiro** (custa 2 níveis de XP e o mineral). Cada arma aceita uma incrustação; uma nova troca a antiga, e ela continua ao refinar e reforjar.

| Mineral | Efeito na arma |
|---|---|
| Jade | +1 ❤ de vida máxima |
| Âmbar | +1 de sorte |
| Pérola | +25% de dano na água ou na chuva |
| Cristal de Gelo | cada acerto deixa o inimigo lento |
| Pedra-Sangue | rouba 6% do dano em vida |
| Turfa | 15% de chance de pôr fogo |
| Cristal de Magma (2.56) | +20% de dano em quem está pegando fogo |
| Âmbar Infernal (2.56) | +15% de dano no Nether |
| Quartzo Negro (2.56) | Golpe crítico (caindo) faz +25% |
### Posturas, estilo e equilíbrio

**Posturas** (`/postura` ou o botão no `/combos`): valem enquanto você segura uma arma de combo.

| Postura | Efeito |
|---|---|
| ◇ Neutra | Sem bônus e sem perdas (padrão) |
| ▲ Ofensiva | +15% de dano e desequilibra +25%; leva +10% de dano e os golpes custam +10% de vigor |
| ■ Defensiva | -20% de dano recebido, resiste a empurrões, cada golpe de combo cura ½ ❤; -10% de dano e 5% mais lento |
| ≈ Ágil | +10% de velocidade, golpes recarregam 20% mais rápido, esquiva custa metade do vigor; -8% de dano |
| ✦ Mestra | A da arma na mão, na proficiência 20: **Duelista** (espada: +15% de dano e janela maior do golpe perfeito), **Carrasco** (machado: +40% em quem tem menos de 35% da vida), **Falange** (lança: +10% de dano e -25% de dano de quem está na frente), **Maré** (tridente: na água ou chuva +30% de dano e +20% de velocidade), **Colosso** (maça: +10% de dano, desequilibra o dobro, resiste a empurrões), **Atirador Paciente** (arco: parado há 1 s, +35% de dano); as do arsenal novo estão na tabela acima |

- **Visual:** aura de partículas na cor da postura. Com o pacote de recursos, **espada, machado e maça mudam o jeito de segurar** (erguida, em guarda, invertida e a pegada da Mestra). Lança, tridente e arco ficam com a pegada normal.
- **Golpe no tempo certo:** depois do 2º clique, um "tim" com faíscas marca o momento do 3º. Acertando a janela, o golpe sai **perfeito**: +25% de dano, 30% menos vigor, mais estilo e mais equilíbrio.
- **Medidor de estilo (D, C, B, A, S):** golpes diferentes, golpes perfeitos, acertos no ar, finalizadores e quebrar o equilíbrio sobem o rank; repetir o mesmo golpe, apanhar ou parar de lutar derrubam. Bônus: C +4% de dano, B +8% e +10% de XP de proficiência, A +12% e +20%, S +18% e +30%. O rank aparece na barra de vigor.
- **Equilíbrio dos inimigos:** quem tem 40 de vida ou mais (chefes, elites, guardiões...) tem uma barra de equilíbrio que os golpes de combo enchem. Cheia, o inimigo fica **parado por 4 s e leva +50% de dano** (chefes não soltam habilidades). Depois ele fica 8 s sem poder ser desequilibrado.
- **No ar:** golpe de combo em quem está no ar (lançado ou pulando) dá +30% de dano e segura o alvo no ar por um instante.
- **Impacto:** o dano dos golpes aparece subindo do alvo, cada acerto dá um tranco curto, e um "plim" avisa quando o golpe sai da recarga.
- **Afinidade:** sua classe e seu deus combinam com uma das posturas gerais (aparece no menu do `/postura`). Classe que combina: **+8% de dano e metade das penalidades** da postura. Deus que combina: **+1% de dano por nível de fé**.

| Postura | Classes | Deuses |
|---|---|---|
| ▲ Ofensiva | Guerreiro, Berserker, Assassino, Senhor da Guerra, Lâmina Fantasma, Soberano das Sombras | Bellum, Mortis |
| ■ Defensiva | Cavaleiro, Paladino, Santo Paladino, Ferreiro, Mestre Forjador, Herdeiro do Ferreiro | Ferrum, Sylva |
| ≈ Ágil | Arqueiro, Ladino, Atirador, Caçador, Arqueiro Celestial, Mago, Elementalista, Feiticeiro, Arquimago, Domador, Mestre das Feras, Senhor das Feras | Maris, Arcanus |

**Maestria de postura (2.54):** cada acerto com uma postura conta para a maestria dela, que sobe 5 níveis (200 / 600 / 1.500 / 3.500 acertos). Cada nível soma **2%** no que a postura faz (Ofensiva: mais dano; Defensiva: menos dano recebido; Ágil: menos recarga). O nível aparece no menu de posturas; 3 posturas no nível V dão o título **Mestre das Posturas**.

### Técnicas secretas e Mestres de Armas (2.54)

Cada tipo de arma tem uma **técnica secreta**, um golpe extra que só um **Mestre de Armas** ensina. Os mestres moram nas vilas (fale com eles e escolha "Técnicas"). Cada técnica pede **proficiência 12** na arma e **24 esmeraldas**; aprender aumenta o laço com o mestre e vai para o Diário. Depois disso ela aparece no `/combos` com a marca **✪ Técnica secreta**.

| Mestre | Vila | Ensina |
|---|---|---|
| Mestra das Marés | Pescadores | Tridente (Maremoto), Lança (Mil Estocadas), Arpão (Fisgada do Leviatã), Arremesso (Mil Lâminas), Bumerangue (Órbita Eterna) |
| Mestre do Bastão | Mosteiro | Bastão (Montanha Inabalável), Leques (Vendaval de Pétalas), Chicote (Serpente-Relâmpago), Adaga (Dança das Sombras), Lâminas Duplas (Tempestade Escarlate) |
| Mestre de Guerra | Anões | Machado (Machados Ancestrais), Maça (Sino do Juízo), Montante (Céu Partido), Mangual (Nevasca de Ferro), Clava (Erupção Vulcânica) |
| Mestra da Lâmina | Élfica | Espada (Vento Cortante), Arco (Flecha Cometa), Foice (Lua de Sangue), Garras (Frenesi da Fera) |
| Ferreiro Piglin (2.56) | Forja dos Piglins (Nether) | Lança de Basalto (Coração do Vulcão). Ele cobra **24 lingotes de ouro** em vez de esmeraldas |

Títulos: **Discípulo** (3 técnicas) e **Herdeiro das Técnicas** (as 20).

### Aparar, fraquezas e combate montado (2.54)

- **Aparar:** levante o escudo **bem na hora** em que o golpe chega (um quarto de segundo) e de frente para o atacante. O golpe é cancelado, flechas e bolas de fogo voltam, o atacante fica lento e fraco, você ganha +10 de Vigor e o próximo golpe em 1,5 s faz **×1,5**. O **Escudo de Duelo** (lingote de ferro, ouro, lingote de ferro em cima; escudo no meio) dá quase o dobro do tempo e contra-ataque de ×1,8. 50 aparadas: título **Muralha Viva**.
- **Fraquezas:** cada família de criatura tem armas fortes e fracas (esqueletos apanham mais de maça, clava, mangual e bastão e menos de flecha; aranhas de lança e foice; slimes de maça e montante; criaturas de fogo de tridente e arpão (a clava rende pouco nelas); voadores de arco e bumerangue; guardiões de tridente e arpão...). O aviso aparece na barra no primeiro golpe e a lista fica no Bestiário.
- **Clima:** na chuva, tridente e arpão fazem +15% (+25% na tempestade), a clava e o arco perdem um pouco e o fogo se apaga; no frio o mangual ganha +15%; à noite os mortos-vivos aguentam um pouco mais.
- **Combate montado:** golpe de cima da montaria correndo é uma **carga** (até +50% conforme a velocidade; lança, arpão, tridente e montante ganham mais 20%), flecha atirada montado faz +15%, escudo levantado faz a montaria levar metade do dano, e lutar montado dá XP de Doma.
- **Chefes com armas dos minérios:** chefes do Altar (20% + 5% por estrela), Cidades Submersas, Ilhas do Céu e Profundezas (15%) podem deixar uma arma dos minérios já forjada, Épica ou Única.

### Ligações: óleos, magia, party e companheiros

**Óleos de lâmina** (Bancada Alquímica, aba Componentes; todos levam 1 Óleo de Peixe): segure o óleo e **clique com o botão direito**. Ele vai na arma de combo da outra mão (ou na 1ª arma de combo da barra) e dura **60 golpes** (+1 a cada 2 níveis de Alquimia). A arma mostra o óleo e os golpes que restam.

| Óleo | Receita (+ Óleo de Peixe) | Efeito | Elemento |
|---|---|---|---|
| Óleo de Fogo | pó de blaze, frasco | incendeia o alvo | Fogo |
| Óleo Gélido | gelo compactado, 2 bolas de neve | lentidão II 2 s e congela | Gelo |
| Óleo Venenoso | 2 olhos de aranha, batata venenosa | veneno 3 s | Natureza |
| Óleo Trovejante | Mercúrio Vivo, 2 lingotes de cobre | 15% de chance de uma faísca saltar para outro inimigo (3 de dano) | Energia |
| Óleo de Prata | 6 pepitas de ferro, pó de glowstone | +40% de dano em mortos-vivos | Vida |

- **Combos × magia:** com óleo (ou com runa de Explosão, Gelo, Tempestade ou Vampírica), cada golpe de combo marca o alvo com o elemento e faz **reações** com as magias: as suas e as da party. Ex.: Óleo Gélido + magia de Fogo = Derreter; Óleo de Fogo + Óleo Venenoso de um amigo = Queimada; Óleo de Prata + Runa Vampírica = Corrupção. A força da reação sobe com a proficiência da arma. Runas: Explosão = Fogo, Gelo = Gelo, Tempestade = Energia, Vampírica = Sombra (o óleo vale mais que a runa).
- **Ataque conjunto:** dois da mesma party acertam combos no mesmo inimigo com até 3 s de diferença: **+50% do golpe como dano extra**, muito equilíbrio, +15 de estilo para os dois e um raio de luz ligando os dois ao alvo. 4 s de recarga por alvo.
- **Companheiros:** no **finalizador**, seus companheiros a até 24 blocos partem para cima do alvo do combo.

## Mobilidade e mochila

- **Esquiva:** dois toques rápidos em **A**, **D** ou **S** fazem um salto rápido para o lado ou para trás, com um instante de invulnerabilidade. Gasta 15 de vigor e tem 1,2 s de recarga. Não funciona agachado, montado ou voando.
- **Gancho de Escalada:** bancada com **1 vara de pescar + 1 gancho de armadilha + 3 lingotes de ferro**. Lance num bloco e recolha: você é puxado até lá (até 40 blocos), sem dano de queda. Num monstro, puxa ele até você; num chefe ou jogador, puxa você até ele.
- **Capa Planadora:** acessório de bolso feito na Bancada Alquímica (4 membranas de phantom, 6 penas, 2 couros). No ar, aperte **pular** para planar: você desce devagar para onde olha e não leva dano de queda. Pule de novo ou agache para soltar.
- **Balão de Cristal:** a versão barata da capa (Bancada Alquímica: 4 lãs brancas + 4 linhas). Abre do mesmo jeito, desce ainda mais devagar, mas anda bem mais devagar e **estoura no fim do 5º voo** (a descrição mostra os usos que restam).
- **Mochila** (`/mochila`): um baú pessoal que começa com 1 fileira e ganha mais uma a cada 150 níveis somados, até 6 (54 espaços). Fica salva no jogador e não cai quando ele morre.

## Equilíbrio de criaturas

- O limite de monstros por jogador em cada mundo é 45 (o padrão do Minecraft é 70). Muda em `criaturas.limite-de-monstros` (−1 = não mexer).
- As criaturas de evento do plugin (hordas, lobisomens, lacaios de Elites e de chefes, guardiões de ninho, monstros de portal) **não ficam mais gravadas no mundo**: se o chunk descarrega, elas somem. Sobras de versões antigas são removidas sozinhas quando o chunk carrega.
- O modificador **Invocador** dos Elites chama no máximo 4 lacaios vivos por vez, e um portal transbordando não solta onda nova com 30 monstros dele ainda vivos.
- `/rpgadmin criaturas` mostra quantas criaturas há em cada mundo e os chunks mais cheios; `/rpgadmin limparcriaturas` remove as sobras e os monstros sem nome que nunca somem (pets, moradores, sombras, chefes e companheiros ficam).
## Classes

**Santuário das Classes:** jogue **1 livro e pena** e **1 diamante** em cima de um **púlpito**. Clique nele, ou use `/classe` em qualquer lugar (lá dá para ver tudo e aceitar caminhos).

**Como funciona:**
1. **Escolha um caminho:** clique numa classe cujos requisitos você cumpre. A partir daí as **tarefas** começam a contar.
2. **Faça as tarefas** (ex.: Guerreiro = derrotar 100 monstros corpo a corpo).
3. **Passe na Prova**, que começa num Santuário: uma arena só sua com **ondas de monstros**, **alvos só com flechas** ou um **duelo contra o Guardião da classe**, em 4 minutos. Morrer na prova não perde nada; falhou, espera 5 minutos.
4. A classe sobe do **nível 1 ao 20** com o XP dos atributos dela. No 20 ela fica **Dominada** e entra no seu **histórico para sempre**.
- Você usa **uma classe por vez**. Voltar para uma classe que já aprendeu custa 10 esmeraldas, num Santuário.
- **Habilidades:** **agache + F** usa a selecionada; **agache + clique esquerdo no ar** troca. A força cresce com o nível da classe e fica **20% maior dentro dos portais** (2.76). A classe aparece na tag acima da cabeça.

**A árvore** (as avançadas pedem classes **dominadas**, às vezes duas):

| Classe | Pede | Passivo (no nível 20) | Habilidades |
|---|---|---|---|
| ⚔ Guerreiro | Combate 15 | +2 ❤, +10% dano corpo a corpo | Investida, Grito de Guerra, Disparada |
| ➹ Arqueiro | Combate 15 | +15% dano de flecha, +5% velocidade | Chuva de Flechas, Salto Evasivo |
| ✦ Mago | Arcano 15 | +40 mana, +1 mana/s | Míssil Arcano, Barreira Arcana, Bola de Fogo |
| ✧ Ladino | Corrida 15 | +12% velocidade, 8% esquiva | Bomba de Fumaça, Rolamento |
| ⚒ Ferreiro | Ferraria 15 | 5% de forjar uma raridade acima, +3% refino | Reparo Rápido, Armadura Reforçada |
| ♞ Domador | Doma 15 | +1 companheiro, +15% dano deles | Chamado, Fúria da Matilha |
| ⛨ Tanque | Combate 15 | +3 ❤, +4 armadura | Provocar, Muralha |
| ✙ Curandeiro | Arcano 15 | +1 ❤, +30 mana, +1 mana/s; cura a party perto a cada 5 s | Cura, Purificar |
| ☠ Berserker | Guerreiro | +15% dano, até +40% com pouca vida | Fúria, Redemoinho, Sede de Sangue |
| ♜ Cavaleiro | Guerreiro + Domador | +3 ❤, +4 armadura, +25% dano montado | Carga, Baluarte |
| ✚ Paladino | Guerreiro + Mago | +3 ❤, +3 armadura, +30 mana | Luz Sagrada, Escudo Divino |
| ➶ Atirador de Elite | Arqueiro | +30% dano de flecha, 15% de crítico x2 | Tiro Perfurante, Olho de Águia |
| ☤ Caçador | Arqueiro + Domador | +20% dano de flecha e dos companheiros | Marca do Caçador, Armadilha |
| ❄ Elementalista | Mago | +80 mana, +2 mana/s, magias 20% mais baratas | Tempestade Elemental, Escudo Elemental, Nova de Gelo |
| ✴ Feiticeiro de Batalha | Mago + Guerreiro | +40 mana, +10% dano; golpes devolvem mana | Lâmina Arcana, Passo Arcano |
| ✝ Assassino | Ladino | +15% velocidade, 12% esquiva, +50% dano pelas costas | Passos das Sombras, Golpe Letal, Mutilação |
| ⚒ Mestre Forjador | Ferreiro | 12% de forjar uma raridade acima, +8% refino | Afiar, Martelo do Trovão |
| ♞ Mestre das Feras | Domador | +2 companheiros, +1 componente, +40% dano deles | Rugido, Vínculo Vital |

As básicas também pedem **nível total 55**. As avançadas pedem o atributo da classe no **nível 40**. As **classes lendárias** ficam escondidas na árvore até você achar o Local Oculto delas (veja abaixo).

## Lendas e Feitos

`/lendas` abre o **Livro das Lendas**. Uma lenda é um item **✦ Especial ✦** com nome próprio, um **poder único** e a sua história no texto ("Forjada por Felipe em 02/10/2026, que derrotou 1.204 mortos-vivos"). **Cada lenda existe uma vez só no servidor.**

**Forjar:**
- Cumpra os **feitos** da lenda.
- Tenha **Ferraria 50**.
- Fique perto de uma **Forja do Ferreiro**, segure o item certo e clique na lenda no livro.
- Custo: **1 Essência Primordial, 6 Fragmentos de Forja e 30 níveis de XP**.
- O item vira a lenda com status de Mítico. Dá para refinar, mas não para reciclar.

| Lenda | Item | Enigma (dica no livro) | Poder |
|---|---|---|---|
| Lâmina do Exorcista | espada/machado | "Os mortos só descansam pelas mãos de quem já os fez descansar mil vezes." | +60% de dano em mortos-vivos (que pegam fogo); derrotar um cura |
| Presa da Tecelã | espada/lança | "Quem quebra a teia da rainha mais de uma vez herda o veneno dela." | Envenena; 20% de prender numa teia |
| Machado do Lenhador Eterno | machado | "A floresta respeita quem já derrubou uma floresta inteira." | Derruba a árvore inteira (só árvores naturais) |
| Picareta do Abismo | picareta | "O abismo se abre para quem já arrancou milhares de tesouros da pedra." | Agachado, minera o veio inteiro |
| Arco da Tempestade | arco/besta | "A tempestade empresta o raio a quem a venceu e nunca erra de longe." | 25% das flechas chamam raio |
| Coroa do Fim | capacete | "Só quem derrota o fim várias vezes pode se coroar com ele." | +25% de dano em chefes; Resistência II com pouca vida |
| Égide do Guardião | peitoral/escudo | "O escudo nasce de quem sobrevive onde ninguém deveria sobreviver." | Devolve dano (15%); Absorção ao apanhar |
| Botas do Andarilho | botas | "O caminho reconhece quem já correu o mundo inteiro." | Velocidade e nenhum dano de queda |
| Lâmina do Carrasco | espada/machado | "A lâmina pesa com o sangue dos que caíram em duelo." | Dano em dobro em quem está com menos de 20% de vida |
| Martelo do Forjador Lendário | maça/machado | "O martelo escolhe quem já fez o metal cantar, e cantar os mitos." | Trovão ao acertar; +10% de forjar uma raridade acima |
| Lâmina do Arquimago | espada | "O aço se curva para quem conhece todos os segredos da magia." | Magias 30% mais baratas; golpes devolvem mana |
| Elmo do Senhor das Feras | capacete | "As feras coroam quem lutou ao lado delas até o céu." | Companheiros perto ganham Força e Regeneração |

**Feitos escondidos:** o livro mostra só o enigma. Cada feito aparece quando o jogador **já fez metade dele**, ou quando lê uma **Página do Livro das Lendas** (vem dos selos dos Locais Ocultos e das masmorras), que revela todos os feitos de uma lenda. Admin: `/rpgadmin revelar <lenda|todas>`. Os números exatos estão no código (`Lenda.java`), não neste README, para não estragar o segredo.

**Despertar:** a lenda fica mais forte com os abates feitos com ela: **I** (0), **II** (100), **III** (300), **IV** (700), **V** (1.500).

**Perdeu a lenda?** O dono pode forjá-la de novo no livro, e a cópia antiga se apaga. Com shift + clique o dono **abre mão** dela, e a lenda fica livre para outro jogador.

## Pedras de Viagem

**Ritual:** jogue **2 pérolas do ender** e **4 fragmentos de ametista** em cima de uma **pedra entalhada de ardósia**.
- Clique numa pedra para **descobri-la**. No menu de qualquer pedra aparecem as outras que você já descobriu.
- **Viajar** custa **1 nível de XP a cada 1.000 blocos** (mínimo 1; outro mundo: 3) e pede **3 segundos parado**. Mexer ou tomar dano cancela.
- O dono **renomeia** a pedra com uma etiqueta de nome feita na bigorna e escolhe se ela é **pública** (todos) ou **privada** (só ele e a party).
- Não funcionam no mundo das masmorras.

## Locais Ocultos e Classes Lendárias

Na primeira vez que liga, o plugin escolhe **14 lugares** pelo mundo (2 de cada tipo, entre 600 e 4.500 blocos do spawn, fora do mar). Cada um é **construído quando alguém chega perto pela primeira vez**: uma ruína pequena com um **poço** que desce até lá embaixo.
- **Como achar:** **Mapas Rasgados** (das masmorras e dos selos) viram uma **Bússola Antiga** que aponta para perto (±60 blocos) do local mais próximo que você não conhece. Chegando perto você ouve **sussurros**. `/locais` mostra os que você já achou.
- **Enigma:** na antessala há **4 alavancas** com cores em cima e um **livro no púlpito** com o enigma ("Primeiro o sangue derramado, depois o mar profundo..."). Na ordem certa o portão abre; errou, os guardiões menores acordam.
- **Guardião:** no salão, um guardião forte (400 de vida, mais por jogador) protege o **selo**.
- **Selo:** vencido o guardião, toque o selo (clique direito). Cada jogador ganha **uma vez por local**: Página do Livro das Lendas, Mapa Rasgado, Fragmentos de Forja, chance de Essência Primordial, diamantes e esmeraldas. Depois de 30 minutos o local volta ao começo para os próximos.
- Os blocos do local não quebram (ninguém cava em volta do enigma).

**Classes lendárias:** o selo também concede a classe lendária do local, **se você cumpre o requisito e ela estiver livre**. **Só pode haver um dono por classe** no servidor:
- O dono pode abrir mão dela no Santuário (shift + clique).
- Quem fica **30 dias sem entrar** perde a classe, que fica livre de novo.

| Local | Guardião | Classe Lendária | Pede | Habilidades |
|---|---|---|---|---|
| Túmulo do Ferreiro Antigo | Guardião da Forja Antiga | ✪ Herdeiro do Ferreiro Lendário | Mestre Forjador | Martelo Celestial, Bênção do Ferreiro |
| Biblioteca Proibida | Bibliotecário Amaldiçoado | ✺ Arquimago Primordial | Elementalista | Meteoro Arcano, Tempo Suspenso |
| Covil das Feras Ancestrais | Fera Ancestral | ❂ Senhor das Feras Ancestrais | Mestre das Feras | Chamado Ancestral, Forma Bestial |
| Cripta do Rei Caído | Rei Caído | ♛ Senhor da Guerra | Berserker | Terremoto, Estandarte de Guerra |
| Santuário das Sombras | Sombra Sem Nome | ☽ Lâmina Fantasma | Assassino | Dança das Lâminas, Véu Sombrio |
| Observatório Celeste | Arqueiro Espectral | ✵ Arqueiro Celestial | Atirador de Elite | Flecha Estelar, Chuva de Estrelas |
| Capela Profanada | Paladino Corrompido | ☀ Santo Paladino | Paladino | Julgamento Divino, Aura Sagrada |

(Cada uma também pede o atributo da classe no nível 40 e a classe de requisito **dominada**.)

A oitava classe lendária, **☾ Soberano das Sombras**, não tem Local Oculto: ela desperta num Portal Pesadelo (veja [Portais](#portais-soberano-das-sombras-e-torre-infinita)).

Os quatro **Monarcas** (desde a 2.76) também são classes lendárias sem Local Oculto: vêm do **Portal do Monarca**, aberto no Mural das Relíquias (veja [Relíquias, Mural e os Monarcas](#relíquias-mural-e-os-monarcas-274)). Na árvore, as lendárias ficam na página **"✪ Lendárias e Monarcas"**.

## Fé e mistério

### Deuses

**Ritual:** jogue **1 maçã dourada** e **1 vela** em cima de um **bloco de quartzo entalhado**: ele vira o **Santuário dos Deuses**. Clique para escolher o seu deus (um por vez), rezar e pedir o milagre; `/deus` mostra a sua fé de qualquer lugar.
- **Devoção:** jogue **oferendas** com Q em cima do Santuário (cada deus aceita coisas diferentes, listadas no menu), **reze** uma vez por dia (+15) e use os atributos do deus (o XP deles também conta).
- **Fé 1 a 5** (100, 300, 700 e 1.500 de devoção): fortalece o passivo. Da **fé 2** em diante dá para pedir o **milagre** a cada 20 minutos (`/deus milagre`).
- **Trocar de deus** (shift + clique) corta pela metade a devoção no deus que você deixou.
- **Deus do reino:** o Rei torna o deus que ele segue o deus oficial; quem segue o deus oficial ganha +25% de devoção.

| Deus | Passivo (cresce com a fé) | Milagre | Devoção também por |
|---|---|---|---|
| ⚒ Ferrum, Deus da Forja | +armadura e mineração mais rápida | Bênção da Bigorna: conserta o equipamento e Resistência II | Ferraria, Mineração |
| ☘ Sylva, Deusa da Natureza | +vida máxima e cura aos poucos | Florescer: plantações perto amadurecem e a party se cura | Agricultura, Madeira, Doma |
| ≈ Maris, Deusa do Mar | +fôlego e agilidade na água; Graça do golfinho na fé 3 | Maré Divina: chuva, Poder do conduíte e Graça do golfinho por 5 min | Pesca, Natação |
| ⚔ Bellum, Deus da Guerra | +dano; +1 ❤ na fé 5 | Fúria de Bellum: Força II e Resistência para a party | Combate, Corrida |
| ✦ Arcanus, Deus da Magia | +mana máxima e mana por segundo | Maré Arcana: mana cheia e +5 de mana/s por 60 s | Arcano, Alquimia |
| ☠ Mortis, Deusa da Morte | roubo de vida; mortos-vivos te ignoram na fé 5 | Segunda Chance: a próxima morte em 10 min é evitada | derrotar mortos-vivos e chefes |

### Constelações que assistem

Os seis deuses **acompanham os seus feitos** (referência: Omniscient Reader's Viewpoint). Quando algo marcante acontece, um deles comenta no chat, como "[Bellum, o Deus da Guerra] aplaude o seu golpe.", e manda **Moedas Estelares**. Feitos com a **vida baixa** (30% ou menos) chamam o dobro de atenção.
- **Bellum:** chefes, Elites, masmorras, hordas, guerras, golpes perfeitos, espantar o Nian, matar um monstro forte com a vida baixa.
- **Ferrum:** núcleos de chefe, gemas perfeitas, mitrilo, meteoritos.
- **Sylva:** animais raros, mutações, colheitas da estação, Raízes de Luz, Bestas Errantes, companheiros no estágio III.
- **Maris:** peixes novos, criaturas marinhas, tesouros enterrados, cidades submersas.
- **Arcanus:** magias novas, reações, runas, transmutação, segredos, sonhos.
- **Mortis:** sobreviver por um fio, sacerdotes, selos, lendas, classes lendárias, renascer, Locais Ocultos.

`/constelacoes` mostra as moedas e troca por **bênçãos**: Fúria do Espetáculo (Bellum, Força por 5 min, 10), Mãos da Forja (Ferrum, Pressa II por 10 min, 8), Bênção do Bosque (Sylva, Regeneração por 3 min, 10), Graça das Marés (Maris, respirar e nadar rápido por 10 min, 6), Céu Estrelado (Arcanus, +80 de mana, 6) e Segunda Chance (Mortis, o próximo golpe mortal te deixa com meio coração, 25). O deus que mais te acompanha cobra **30% menos**. Desligue em `constelacoes.ativadas`.

### Astronomia

À noite, com o céu aberto em cima de você, olhe para cima pela **luneta** por 5 segundos. Cada noite mostra uma das **12 constelações** (O Ferreiro, O Pescador, A Coruja, O Lobo, O Viajante, A Fênix, O Sábio, A Âncora, O Mago, A Coroa, A Serpente e O Gigante). Ela entra no seu diário (`/estrelas`) e dá uma **bênção até o amanhecer** (XP extra, Sorte, Visão noturna, Força, Velocidade, Regeneração, mana, devoção em dobro...). Uma observação por noite.

### Transmutação

**Ritual:** jogue **1 Mercúrio Vivo** e **1 lingote de ouro** em cima de um **bloco de ametista**: ele vira o **Círculo de Transmutação**.

| Receita | Ingredientes | Resultado | Chance base |
|---|---|---|---|
| Cobre em Ferro | 8 lingotes de cobre + 1 Pó Arcano | 3 lingotes de ferro | 85% |
| Ferro em Ouro | 8 lingotes de ferro + 1 Pó Arcano | 3 lingotes de ouro | 80% |
| Redstone em Luz | 9 redstone + 1 Pó Arcano | 3 pós de pedra luminosa | 90% |
| Lápis em Esmeralda | 8 lápis-lazúli + 1 Pó Arcano | 1 esmeralda | 75% |
| Ouro em Diamante | 16 lingotes de ouro + 2 Mercúrio Vivo | 1 diamante | 60% |
| Diamante em Netherite | 4 diamantes + 2 Mercúrio Vivo + 1 Cristal de Mana | 1 sucata de netherite | 45% |
| Pedra Filosofal | 1 bloco de diamante + 4 Mercúrio Vivo + 4 Cristais de Mana + 1 Essência Primordial | Pedra Filosofal | 50% |

A chance sobe com o nível de Alquimia, e a **Pedra Filosofal** no inventário dá +20% (em todas, menos nela mesma). Falhou: os ingredientes se perdem e, às vezes, a mistura explode.

### Runas

**Ritual:** jogue **4 fragmentos de ametista** e **1 bloco de lápis-lazúli** em cima de um **tufo entalhado**: ele vira a **Mesa Rúnica**. Escolha a peça (a da mão, capacete, peitoral, calça ou botas) e grave uma runa, pagando materiais e níveis de XP. Cada peça tem uma runa; gravar outra troca.

| Runa | Vai em | Efeito |
|---|---|---|
| ᛖ Salto | botas | Pulo duplo (aperte pular de novo no ar) |
| ᛉ Proteção | peitoral | Com menos de 30% de vida: Absorção II por 8 s (a cada 60 s) |
| ᛟ Visão | capacete | Visão noturna em lugares escuros |
| ᚱ Ímpeto | calça | +8% de velocidade |
| ᚦ Explosão | espada, machado, lança, maça | 8% de chance de explodir no alvo |
| ᛁ Gelo | espada, machado, lança, maça | 15% de chance de congelar por 2 s |
| ᛋ Tempestade | armas, arco e besta | 6% de chance de um raio no alvo |
| ᚹ Vampírica | espada, machado, lança, maça | Rouba 8% do dano como vida |
| ᚲ Fusão | picareta, machado, pá | Minérios, areia e pedra já saem fundidos |
| ᛇ Eco | arco e besta | Cada flecha dispara uma segunda, com metade do dano |

## Exploração

### Enciclopédia do Mundo

`/enciclopedia` registra sozinha cada **bioma** em que você pisa, cada **estrutura** em que entra (vila, templo, fortaleza, cidade antiga...) e cada **item** que pega pela primeira vez, além das relíquias. Cada bioma novo soma **+0,2% de XP** em todos os atributos, até **+15%**.

### Mitrilo

Um minério novo que só aparece **no fundo do mundo** (Y -58 a -24), trocando ardósia profunda e tufo. Sai em veios pequenos e brilha de leve.
- Só quebra com **picareta de diamante ou netherita**, devagar, e dá **Mitrilo Bruto** e muito XP de Mineração.
- **Mitrilo Bruto** no forno (ou alto-forno) vira **Lingote de Mitrilo**.
- Na **mesa de ferraria**: **molde de melhoria de netherita + peça de netherita + lingote de mitrilo** = peça **✦ Mitrilo**: 50% mais durabilidade e um bônus extra (arma: +1,5 de dano; armadura: +1 de armadura e +1 de resistência; ferramenta: +20% de velocidade). Funciona em itens forjados também.

### Mineração e biomas (2.46)

Cada bioma tem um **mineral próprio**, que só nasce lá. Eles aparecem até nas áreas já exploradas (na primeira vez que o chunk carrega com esta versão), longe de territórios, sem mexer em nada construído.

| Mineral | Onde | Como sai | Para que serve |
|---|---|---|---|
| **Âmbar** | Deserto e badlands (arenito e terracota, y 30–75) | Picareta de pedra ou melhor. 4% de chance de vir uma **relíquia presa dentro** | Pingente de Âmbar; o Joalheiro compra |
| **Jade** | Selva e bambuzal (pedra, y 0–60) | Picareta de pedra ou melhor | Anel de Jade; o Joalheiro compra |
| **Ostra com Pérola** | Fundo do mar (areia, cascalho ou argila) | Qualquer ferramenta. 60% tem pérola | Colar de Pérolas; o Joalheiro compra |
| **Cristal de Gelo** | Montanhas e biomas gelados (pedra e gelo compacto, y 40–140) | Picareta de pedra ou melhor | Anel do Gelo Eterno |
| **Pedra-Sangue** | Nether (netherrack) | Picareta de pedra ou melhor | Amuleto de Sangue |
| **Turfa** | Pântano e mangue (lama ou terra, logo abaixo do chão) | Pá ou mão | **Combustível**: queima 2,5 vezes mais que o carvão |
| **Cristal de Magma** (2.56) | Nether: deltas de basalto (basalto, pedra-negra, magma) e um pouco nos ermos | Picareta de pedra ou melhor | Lança de Basalto, Vara Ígnea, incrustar |
| **Âmbar Infernal** (2.56) | Nether: florestas carmesim e distorcida (netherrack) | Picareta de pedra ou melhor | Incrustar; o Ferreiro Piglin compra |
| **Quartzo Negro** (2.56) | Nether: vale de areia de almas (terra de almas, basalto, pedra-negra) | Picareta de pedra ou melhor | Incrustar; o Ferreiro Piglin compra |

Os minerais dão XP de Mineração e não são levados por explosões nem empurrados por pistões.

**Geodos de Cristal:** bolsões ocos nas cavernas (y −48 a 24), com casca de basalto e calcita e forrados de **Cristal Bruto**. Na picareta, o cristal estilhaça em fragmentos de ametista. Com o **Cinzel do Lapidador** (bancada: fragmento de ametista, lingote de ferro e graveto na diagonal), clique 4 vezes **com calma** para lapidar: sai uma **gema** de grau 1 a 3. A chance de grau alto cresce com a Mineração. Clicar rápido demais pode rachar o cristal.

**Bateia:** na bancada, 2 pepitas de ferro e uma tigela. **Em pé na água rasa**, com areia, cascalho ou argila no fundo, clique e fique parado 3 segundos: sai pepita de ouro ou ferro, pederneira, argila, ouro bruto e, mais raro, uma gema, um âmbar (deserto e badlands), uma pérola (mar e praia) ou uma relíquia. **Nas badlands rende mais ouro.**

**Eco da pedra** (Mineração 20% do máximo): minerando pedra perto (6 blocos) de um mineral raro ou de mitrilo, você ouve a pedra **cantar**. Quanto mais agudo, mais perto. Não mostra a direção: é preciso procurar.

Títulos: **Geólogo** (50 minerais de bioma), **Coração de Cristal** (10 cristais lapidados) e **Garimpeiro do Rio** (100 garimpos).

### Mapas do Tesouro

Mapas de verdade, com um **X vermelho** no lugar do tesouro. Caem de **elites** (4%), da **pesca** (raro), dos baús de **chefes e masmorras** (15%), dos **sítios de arqueologia** e do **Cronista**. Há três tipos: **comum**, **raro** e **lendário**.
- Chegue a 14 blocos do X com o mapa no inventário: o tesouro é desenterrado, mas **guardiões** saem da terra.
- O baú tem itens do nível do mapa, esmeraldas, às vezes uma relíquia, e o lendário traz mitrilo.

### Arqueologia e relíquias

Pelo mundo aparecem **sítios de arqueologia**: pedras em pé com **areia e cascalho suspeitos** em volta. Use o **pincel** neles: além dos cacos de cerâmica, saem **12 relíquias** (Moeda do Rei Caído, Lâmina Partida, Tábua de Ferrum, Estatueta de Sylva, Concha de Maris, Estandarte de Bellum, Pergaminho Arcano, Máscara de Mortis, Mapa das Estrelas, Chave da Torre, Olho do Arauto e Coração de Sombra). Cada uma tem a sua história e entra na coleção da Enciclopédia ao ser pega.

### As Crônicas do Mundo (campanha)

O **Cronista** é um aldeão bibliotecário que o admin coloca no spawn (`/rpgadmin cronista`). Clique nele (ou use `/cronista`) para ler o livro do seu capítulo; quando cumprir o pedido, volte até ele e clique em **entregar**.

| Capítulo | Pedido | Recompensa |
|---|---|---|
| 1. O Aventureiro | Nível total 50 | 16 esmeraldas e um mapa do tesouro comum |
| 2. O Fogo da Forja | Forjar 5 itens | 4 Fragmentos de Forja |
| 3. Ecos Arcanos | Lançar 100 magias | Pó Arcano e Cristais de Mana |
| 4. Os Deuses Esquecidos | Fé 2 com um deus | Uma relíquia e 10 esmeraldas |
| 5. Sob a Terra | Vencer 3 masmorras | Pedra de Proteção |
| 6. O Céu Fala | Observar 4 constelações | Mapa do tesouro raro |
| 7. As Ruínas Contam | 6 relíquias diferentes | Essência Primordial |
| 8. O Selo | Romper o selo de um Local Oculto | 2 lingotes de mitrilo e um mapa lendário |

### Estruturas pelo mundo

Vinte estruturas prontas (as dezesseis abaixo e as quatro vilas com moradores) surgem sozinhas em **terra nunca visitada** (chunks novos), longe de territórios, longe umas das outras (160 blocos) e das estruturas do próprio Minecraft, e só onde o chão é firme e quase plano. Cada uma cabe dentro de um chunk e o terreno é nivelado com cuidado (árvores no caminho são derrubadas inteiras). Ao chegar perto aparece o nome dela, entra no Diário e conta para o título **Andarilho das Ruínas** (12 estruturas).

| Estrutura | Onde | O que tem |
|---|---|---|
| 🗼 **Torre de Vigia Abandonada** | Qualquer terra firme | Três andares com escada, sino e baú no alto (saque de masmorra, esmeraldas, mapa rasgado, luneta). **Sentinelas Esquecidas** (esqueletos) acordam quando você chega |
| ⛺ **Acampamento de Bandidos** | Planícies, florestas, taigas, savanas | Fogueira, três tendas, mantimentos e o baú do saque. **Bandidos** e o **Chefe dos Bandidos** (dá esmeraldas e às vezes um mapa) |
| ⚰ **Cemitério Antigo** | Planícies, florestas, taigas, neve, pântano | Lápides, mausoléu com escada para a **cripta** (baú com página de lenda e relíquia). **À noite, os mortos se levantam** das covas |
| ⛏ **Mina Abandonada** | Terra firme (não deserto nem selva) | Cavalete e poço com escada até túneis escorados, trilhos, minérios nas paredes, baú de ferramentas e **carrinho de minério** (às vezes Mitrilo bruto). **Mineiros Perdidos** e aranhas lá embaixo |
| ⛲ **Poço dos Desejos** | Planícies, florestas, savanas, cerejeiras | Jogue **1 esmeralda** na água: um desejo por dia (Sorte, Vigor, Ligeireza, Pressa, Herói da Vila, Fôlego ou Visão), às vezes com presente. 10 desejos = título **Desejoso** |
| 🛖 **Cabana do Eremita** | Florestas, taigas, pântanos, selvas | O **Eremita** mora lá: troca ervas, mudas, elixires, mapas e cartas (ofertas novas todo dia) e **conta boatos** de onde há outra estrutura que você ainda não viu |
| ✧ **Santuário Esquecido** | Qualquer terra firme | Um altar antigo a um dos seis deuses, com a cara dele. Quem segue aquele deus **reza no altar** (clique) uma vez por dia: **+30 devoção** (o dobro da prece) e uma bênção do deus |
| 🌴 **Oásis do Deserto** | Deserto | Lagoa com palmeiras, juncos e vitórias-régias, um camelo e a tenda do **Mercador Nômade** (gemas, bebidas, elixires, mudas de limoeiro, mapas) |
| 🧙 **Cabana da Bruxa** | Pântano (sobre palafitas, na água) | A **Bruxa** vende elixires ótimos, componentes de alquimia, frascos e ervas **de dia**. À noite não atende, e as **Aprendizes da Bruxa** saem da cabana |
| 🗼 **Farol Abandonado** | Praias e costas | Torre listrada com escada até a lanterna e o baú do faroleiro. **Clique na lanterna com um bloco de pedra luminosa** para acender: por 7 dias quem navega perto (96 blocos) tem **Sorte** (pesca e saque melhores) |
| ❄ **Expedição Perdida** | Neve e gelo | Barracas soterradas, um explorador congelado, o **diário da expedição** e o **trenó** com o mapa que eles seguiam. **Exploradores Congelados** (esqueletos do gelo) guardam o lugar |
| 🏚 **Vila Saqueada** | Planícies, savanas, taigas | Casas queimadas ainda soltando fumaça, poço quebrado e o **diário de um sobrevivente**, que conta de onde vieram os bandidos (o acampamento mais perto). Os **Aldeões Perdidos** são aldeões-zumbis: dá para curar (fraqueza + maçã dourada) |
| 🔮 **Torre do Mago em Ruínas** | Florestas, taigas, planícies | Três andares com estantes e mesa de encantamento. As **anotações do mago revelam uma magia secreta de verdade**. No alto, o **Aprendiz Corrompido** (ilusionista) guarda o baú (às vezes um Tomo Proibido) |
| ◯ **Círculo de Pedras Antigas** | Quase todo lugar | De noite as pedras brilham. **Na lua cheia**, quem entra no círculo acorda o **Guardião Ancestral** com dois Espíritos do Vento: ele deixa uma relíquia (e às vezes página de lenda ou Essência Primordial). Uma vez por lua cheia |
| ⚒ **Forja dos Anões** | Montanhas, prados, taigas | Salão meio enterrado com lava, bigorna, altos-fornos e uma chaminé que ainda solta fumaça. Baú com Fragmentos de Forja, lingotes e às vezes Mitrilo. **Anões Espectrais** lá dentro |
| 🏰 **Fortim em Ruínas** | Planícies, florestas, savanas | Muralha com torres, pátio e a torre de menagem. **Soldados Esquecidos** e o **Capitão Esquecido** (deixa Fragmento e às vezes Pedra de Proteção). Bom para party |

Os guardas aparecem só uma vez. Configure em `estruturas:` no config (chance, distância e se vale só para chunks novos).

### Vilas com moradores (2.45)

Quatro tipos de vila surgem do mesmo jeito que as estruturas acima. Cada uma tem **4 moradores** com papel próprio. Clique num deles para abrir o menu dele:
- **♥ Laço:** os mesmos 5 capítulos dos moradores da colônia (conversar, presente favorito e usar o serviço sobem o laço). No capítulo 5, em vez de um ensinamento, ele te dá um **presente único**.
- **Loja:** 5 ofertas que mudam a cada dia. Alguns **compram** coisas suas por esmeraldas.
- **✦ Serviço:** o que só ele faz (alguns uma vez por dia, outros pagos).

| Vila | Onde | Moradores |
|---|---|---|
| 🎣 **Vila de Pescadores** | Praias e rios (sobre estacas) | **Velho Pescador** (varas encantadas, mapa; presente: Vara de Coral), **Peixeira** (compra bacalhau, salmão, baiacu, peixe tropical e tinta), **Carpinteiro Naval** (barcos, lunetas; serviço: barco com baú novo por 3 esmeraldas), **Menina do Cais** (serviço: um pedido por dia, como 5 bacalhaus, e ela retribui com esmeraldas e às vezes uma carta) |
| 🔔 **Mosteiro da Montanha** | Prados, bosques de cerejeira, encostas e montanhas | **Mestre** (serviço: treino diário, XP de Combate e Força por 5 min; presente: Agulha dos Ventos), **Monge Herbalista** (ervas, elixires, chás, mudas), **Guardião do Sino** (serviço: o sino dá Resistência e Regeneração por 3 min a você e à party, uma vez por dia), **Peregrino** (mapas, cartas; serviço: uma história por dia, às vezes uma Página do Livro das Lendas) |
| ⛏ **Aldeia dos Anões** | Montanhas, picos, encostas pedregosas e badlands | **Mestre Mineiro** (tochas, lanternas, TNT, picareta encantada; presente: Picareta do Poço Fundo), **Joalheiro** (gemas; compra diamante, ametista e lápis), **Ferreiro Anão** (serviço: conserta o item da mão, de 2 a 20 esmeraldas), **Taverneiro** (bebidas, comida; compra trigo). Os anões são menores |
| 🌳 **Vila Élfica do Bosque** | Florestas, bosques de bétula, floresta escura e floresta antiga | **Druida** (mudas, farinha de osso; serviço: bênção diária com saciedade e Regeneração), **Arqueira** (flechas de fogo, gelo, rastreadoras...; presente: Arco das Folhas Caídas), **Curandeira** (serviço: vida cheia, tira venenos e a **Penumbra** por 5 esmeraldas), **Tecelã** (lãs, estandartes, linha) |

Os moradores das vilas não se ferem, voltam se sumirem e não entram nos pedidos de aldeões.

### No fundo do mar

| Estrutura | Onde | O que tem |
|---|---|---|
| ⚓ **Naufrágio** | Fundo de qualquer oceano | Um navio adernado, de proa enterrada, com rombo no casco, mastro caído e âncora. Cada um tem **nome** e é de um tipo: **mercante** (esmeraldas, lã, especiarias), **pirata** (ouro, mapa do tesouro, às vezes mapa rasgado), **galeão real** (gema, blocos de esmeralda, às vezes Pedra de Proteção) ou **de expedição** (bússola, mapa, Pérola Negra, às vezes relíquia). O **diário do capitão** no baú da cabine conta a história e, se houver uma cidade submersa perto, **aponta a direção dela** |
| 🔱 **Cidade Submersa** | Mar profundo, longe do spawn. Rara: no máximo uma a cada 3000 × 3000 blocos | Uma cidade de pedra-do-mar de 73 blocos que adorava **Maris**: muralha com portões, avenidas com postes de luz, casas, o **Arquivo** (3 livros com a história da cidade), uma torre, um jardim de corais com a estátua de Maris e o **Templo** no centro, com o **cofre** embaixo (3 baús). Chegando perto, os **Guardas Afogados** e as **Sentinelas das Marés** (guardiões) atacam |

**O coração da cidade:** quem entra no santuário do templo acorda o **Sumo-Sacerdote** (afogado gigante de 450 de vida, com tridente e barra de chefe). Ele chama afogados e puxa quem está perto com um redemoinho. Derrotado, deixa o **Coração da cidade** (um condutor), pérolas negras, escamas do abismo, esmeraldas, relíquia e às vezes o tridente dele. Ponha o coração **bem no meio da moldura do altar**: a cidade **desperta**, os guardas somem para sempre, o condutor funciona ali, quem nada nela ganha Graça do Golfinho e quem segue Maris ganha **+80 de devoção**. Quem preferir ficar com o condutor pode: a cidade continua amaldiçoada e o Sumo-Sacerdote volta em 7 dias.

Para respirar, valem as coisas do jogo: poção de respiração, Respiração Aquática no capacete e condutores. Configure em `estruturas.cidade-submersa` (região, chance, distância do spawn e dias para os guardas voltarem).

### Ilhas do Céu

Arquipélagos raros lá no alto (do y 200 até o teto do mundo, no máximo um a cada 2500 × 2500 blocos, em terra firme): uma ilha principal com uma "raiz" de pedra pendurada embaixo, 3 ilhotas em volta, uma ponte de cordas e uma construção:

| Ilha | O que tem |
|---|---|
| 🌸 **Jardim Suspenso** | A **Árvore Celeste** (cerejeira com copa de azaleia florida) e um laguinho. De tempos em tempos (5 min) a árvore solta uma **Fruta Celeste** para quem está embaixo |
| 🔭 **Observatório Antigo** | Anel de pilares de quartzo, piso de calcita e o púlpito do astrônomo; baú com luneta, Sal Lunar e Pó Arcano |
| ⚒ **Forja dos Ventos** | Forja em ruínas (alto-forno, bigorna, ferraria) e um moinho de pás de lã; baú com Fragmento de Forja e lingotes |
| 🪺 **Ninho do Guardião** | Um ninho enorme com ovos. Quem pisa nele acorda o **Guardião dos Ventos** (breeze gigante de 320 de vida, com barra de chefe; com menos da metade da vida, as rajadas jogam todo mundo para o alto). Deixa **Fragmentos Celestes**, Frutas Celestes, membranas, varas de breeze e esmeraldas, e volta depois de `cidade-submersa.dias-para-voltar` dias |

Todo baú das ilhas tem lã e linha (para o Balão de Cristal).

**Como subir:** no chão, a 28 blocos da ilha (do lado sem ilhotas, em chão plano e aberto), fica a **Plataforma de Vento**: um patamar de pedra com 4 lanternas azuis, uma placa de aviso e uma **placa de pressão** no meio. Pisou, você é **lançado** até 12 blocos acima da ilha e empurrado para dentro dela (esse pouso não machuca).
- **Não há volta segura.** Quem subiu sem **Capa Planadora**, **Balão de Cristal** ou élitro só desce caindo (a placa e a tela avisam). Água embaixo, pérola do ender ou uma **Fruta Celeste** (Queda Lenta por 90 s e pulo mais alto por 60 s) também resolvem.
- **Fragmento Celeste** (material raro) → na Bancada Alquímica: **Amuleto dos Ventos** (+6 blocos de queda segura e +8% de velocidade).
- Os sonhos também apontam para as ilhas.

Configure em `estruturas.ilha-do-ceu` (região, chance e distância do spawn).

### O Nether (2.56)

O Nether ganhou duas estruturas (nascem nas cavernas largas, com metade da chance das do mundo normal) e o resto do pacote do Nether: minerais, a Lança de Basalto, a pesca na lava e a cozinha.

- **Forja dos Piglins** (florestas carmesim e ermos): uma forja de pedra-negra e ouro, com lava atrás da grade, altos-fornos, bigorna e um baú com ouro, minerais do Nether e, às vezes, sucata ou o molde de netherite. Mora ali o **Ferreiro Piglin**, um morador como os das vilas (laço com história, duelo de cartas **difícil**), mas que só aceita **ouro**:
  - **Loja do dia:** Cristais de Magma, a Lança de Basalto, sucata de netherite, pedra-negra dourada, lanternas de alma e, às vezes, o molde de netherite; ele **compra** Âmbar Infernal (6 ouros) e Quartzo Negro (8 ouros).
  - **Técnica secreta da Lança de Basalto** (Coração do Vulcão: o chão vira magma por 4 s): proficiência 12 e **24 lingotes de ouro**.
  - O presente do laço completo é uma Lança de Basalto forjada épica. Ele não vira zumbi no mundo de cima.
- **Primeiro Portal em Ruínas**: uma moldura de obsidiana gigante e partida, com obsidiana chorona, braseiros de alma, um púlpito com **"O Primeiro Portal"** (a história de quem abriu o caminho entre os mundos) e um baú. Clicando na **pedra que chora no meio da moldura**, ela solta a relíquia **Chave do Primeiro Portal** (uma por jogador; vale para a coleção de relíquias e dá o título **Portador da Chave**).
- **Cozinha do Nether** (na Cozinha, como os outros pratos): Ensopado Carmesim (Força e Resistência ao Fogo), Caldo Distorcido (**hoglins e piglins brutos não te atacam**), Espeto de Hoglin (**piglins te deixam em paz** mesmo sem ouro), Peixe-Brasa na Pedra (golpes de fogo), Caldo Vulcânico, Ensopado das Almas e o Banquete Infernal (vale para todos em volta).
- Títulos novos: **Portador da Chave**, **Pescador do Inferno** (25 pescas na lava) e **Garimpeiro do Inferno** (30 minerais do Nether).

### Profundezas

Abaixo do **y 0** ficam as Profundezas. Desde a 2.44 elas não escurecem mais a tela; as **Raízes de Luz** continuam sendo a luz das cavernas fundas e o que afasta a Penumbra.
- **Raízes de Luz:** nascem adormecidas nas cavernas fundas (um tronco de raízes de mangue com um bulbo marrom em cima, que brilha de leve quando você chega perto). **Clique no bulbo:** ela acende para sempre, para todos, e ilumina a caverna em volta (cerca de 20 blocos); até 40 blocos dela não há escuridão nem Penumbra. Quem acende ganha XP e **Sementes Brilhantes**.
- **Semente Brilhante:** arremesse (como bola de neve) e o lugar onde ela cai fica iluminado por 2 minutos.
- **Penumbra:** parte dos monstros que nascem no escuro das profundezas é **da Penumbra** (nome roxo). Cada golpe deles **rouba 1 coração máximo** (até 5, `profundezas.penumbra-maxima`). Os corações voltam **ao sol** (1 a cada 10 s), **perto de uma raiz acesa**, ao acender uma raiz, ao morrer ou com o **Elixir da Purificação**.
- **Coração da Penumbra:** às vezes (15%) acender uma raiz acorda o chefe (esqueleto wither gigante de 350 de vida, com barra de chefe, que rouba corações e chama até 6 zumbis da Penumbra; continua sendo chefe depois de reiniciar o servidor). Deixa Sementes Brilhantes, Fragmento de Forja, Pedra de Proteção, fragmentos de eco e às vezes Essência Primordial.

- **Poço das Profundezas (colônia):** com a **Mina no nível 3** e uma **Raiz de Luz acesa debaixo do território** da colônia, o Minerador desce ao fundo: traz ardósia, ferro, ouro, redstone, lápis, **ametista**, mais diamante, **mitrilo bruto**, **Sementes Brilhantes**, às vezes **fragmento de eco** e, bem raro, uma arma com nome perdida lá embaixo. Mas às vezes (3% por Minerador e turno) **algo da Penumbra sobe pelo poço** e aparece na Mina. A placa da Mina mostra se o poço está aberto.

Configure em `profundezas` (ligar, altura, máximo da Penumbra, chance de raiz por chunk, de Penumbra, do Coração e do poço da colônia).

### Pacote de recursos do servidor

O plugin gera o **seu próprio pacote de recursos** (`plugins/RPGAtributos/pacote/RPGAtributos-recursos.zip`) com as texturas do minério e dos itens de mitrilo, do **Cajado Arcano**, do **Gancho de Escalada** (que muda quando é lançado), da **Capa Planadora**, da **Ferradura**, do **Ninho de Pássaro**, dos **12 peixes raros**, dos **16 pratos da Cozinha**, das **variedades raras** (e das sementes de trigo e beterraba), dos **componentes alquímicos**, dos **12 elixires** (cada um com o seu frasco) , das **18 gemas** (bruta, lapidada e perfeita), dos **materiais raros e núcleos dos chefes**, dos **acessórios**, de **7 lendas** (as armas e ferramentas; o arco, o escudo e as armaduras ficam com o visual do jogo, porque têm animação ou acabamentos que o pacote não pode copiar) e dos itens novos: bebidas, cartas, flechas, frascos, méis e ervas. A plantação no chão continua com o visual do jogo: só os itens mudam. Ele é **opcional**: sem ele tudo funciona com a aparência do jogo (o minério parece um bloco de cogumelo, o lingote parece ferro, o cajado parece uma vara de breeze...).

**Editar as texturas à mão:** os desenhos originais ficam em `pacote/texturas-padrao/`. Copie um PNG para `pacote/texturas/`, edite (mesmo nome, imagem quadrada: 16×16, 32×32...) e reinicie: ele entra no lugar do original. Apagar o arquivo volta ao original. Itens feitos antes ganham o visual sozinhos quando o jogador entra. O pacote **se soma** ao pacote do servidor (não o troca). Para enviar, ponha um link em `pacote-de-recursos.url` ou ligue o servidor embutido (`porta` + `endereco`); veja `/rpgadmin pacote`.

**Editor de pegadas (2.54):** `/rpgadmin pegada <id> 3p|1p rx ry rz tx ty tz` muda como um item fica na mão (os mesmos números do Blockbench), `padrao` volta ao original. Fica salvo em `pacote/pegadas.yml` e o pacote é refeito na hora.

### Modelos EMF/ETF para editar (2.55)

Para quem joga com **EMF + ETF**, o plugin aceita modelos próprios para **91 criaturas** que ele reconhece pelo nome: cada morador das vilas (mestres de armas incluídos), o Cronista e o Mercador Arcano, os 6 chefes do Altar, as Bestas Errantes (Pixiu, Baize, Kunpeng), os companheiros nos estágios II e III (lobo, gato, cavalo, golem, urso, raposa, camelo, lhama, papagaio), as invocações (lobos espectrais e ancestrais, Autômato, golem e elementais dos feitiços, espíritos), as Sombras e as Elites por afixo, Nian, Lobisomem, Vampiro, Coração da Penumbra, Guardião dos Ventos, Sumo-Sacerdote e os sete guardiões dos locais ocultos.

- **Os 91 modelos já vão no jar** (`resources/modelos/visual`, ver `ferramentas/MODELOS-EMF.txt`). Um `<id>.jem`/`.png` com o mesmo nome em `pacote/modelos/` passa na frente do que está no jar.

- Os arquivos de partida ficam em `blockbench/preparados/` (gerados, fora do git): um `.jem` com a forma vanilla exata e um `.png` com a textura vanilla de cada criatura. Abra o `.jem` no Blockbench, edite e exporte por cima. O `LEIA-ME.txt` lista todos.
- Coloque o `<id>.jem` e o `<id>.png` em `plugins/RPGAtributos/pacote/modelos/` (pode usar subpastas) e use **`/rpgadmin modelos`**. O plugin numera os arquivos, junta as regras por nome com as dos Guardiões e, no aldeão com modelo próprio, esconde as camadas de bioma e profissão (a roupa passa a ser a sua textura).
- Só entra o que tiver `.jem`; sem nenhum, o pacote não muda. Quem não tem os mods vê o normal. Companheiro renomeado pelo jogador volta ao visual normal (a regra é pelo nome do estágio).

## Talentos, renascer e evolução dos companheiros

### Talentos

`/talentos` (ou o botão no `/atributos`): **1 ponto a cada 25 níveis somados** nos atributos (até 52 no nível máximo), mais **8 a cada renascimento**. Cada árvore tem 4 faixas: a 2ª abre com 5 pontos gastos nela, a 3ª com 10 e o talento final com 15. **Refazer** devolve todos os pontos por 16 esmeraldas.

| Árvore | Faixa 1 | Faixa 2 | Faixa 3 | Final |
|---|---|---|---|---|
| ⚔ Guerreiro | Força Bruta (+2% dano corpo a corpo, 5 graus), Pele Grossa (+1 armadura, 5) | Vigor de Batalha (+15 vigor, 3), Sede de Sangue (derrotar cura ½ ❤, 3) | Golpe Crítico (4% de +50% de dano, 3), Inabalável (-4% dano recebido e +10% resistência a empurrões, 2) | **Fúria Imortal**: com menos de 25% de vida, +25% de dano e Regeneração II por 6 s (a cada 60 s) |
| ✦ Arcano | Mente Ampla (+10 mana, 5), Fluxo (+0,2 mana/s, 5) | Economia Arcana (-4% custo, 3), Eco de Mana (6% de magia de graça, 3) | Escudo de Mana (10% do dano sai da mana, 2), Erudito (+2% XP em tudo, 3) | **Sobrecarga**: a cada 30 s, a próxima magia não gasta mana |
| ⚒ Artesão | Mãos de Ferreiro (+1% raridade acima, 5), Colheita Farta (+4% minério/tronco/colheita em dobro, 5) | Refinador (+2% refino, 3), Alquimista Nato (+5% de render o dobro, 3) | Mestre-Cuca (pratos +15% de duração, 2), Engenhoso (10% de não gastar durabilidade, 3) | **Obra-Prima**: +5% raridade acima e, quando sobe, 25% de subir mais uma |
| ➶ Explorador | Passos Leves (+2% velocidade, 5), Sorte do Viajante (+0,4 sorte, 5) | Queda Suave (+2 blocos sem dano de queda, 3), Esquiva Ágil (-15% recarga e -3 vigor na esquiva, 3) | Caçador de Tesouros (mapas +50%, 2), Garimpeiro (+15% de mitrilo em dobro, 3) | **Andarilho Eterno**: fora de combate, Velocidade I e Pressa I o tempo todo |

Cada árvore tem 22 pontos. Com 52 pontos dá para fechar duas árvores e ainda sobra um pouco; quem renasceu 5 vezes completa as quatro.

### Renascer

`/renascer` (ou o botão nos talentos): com **todos os atributos no nível 100**, os atributos voltam a 0 e os talentos são devolvidos. Em troca, para sempre e a cada renascimento:
- **+10% de XP** em tudo, **+1 ❤**, **+2% de dano corpo a corpo** e **+10 de mana**;
- **+8 pontos de talento** e uma **★** antes do nome na tag (até ★★★★★).

Itens, classes, lendas, companheiros, títulos, mochila, proficiência e todo o resto continuam. O que já foi conquistado não se perde: para quem renasceu, a mochila, o tamanho do território, as dificuldades de masmorra e os requisitos de classe contam como nível máximo. Pode renascer até 5 vezes, e cada renascimento é anunciado no servidor.

### Evolução dos companheiros

Os companheiros ganham XP lutando (5 por monstro, 20 por Elite, 60 por chefe), as montarias ganham sendo montadas, e todos ganham um pouco só por estar perto do dono. Eles sobem do nível 1 ao 30, com +2% de vida e dano por nível, e **evoluem**:

| Estágio | Nível | O que ganha |
|---|---|---|
| ★ I | 1 | — |
| ★★ II | 10 | +12% de tamanho, +10% de vida e dano, se regenera fora de luta e ganha o **Golpe Feroz** (15% de chance: +50% de dano e lentidão) |
| ★★★ III | 25 | +25% de tamanho, mais +15%, Golpe Feroz com 25% e o **Vínculo Ancestral**: o dono a até 12 blocos recebe 8% menos dano |

Quem não tem nome dado pelo jogador ganha o nome do estágio: Lobo Alfa → **Lobo das Sombras**, Cavalo → Corcel de Guerra → **Corcel Celeste**, Golem de Ferro → Golem de Guerra → **Colosso de Ferro**, Gato → Gato Selvagem → **Pantera Sombria**... (as outras criaturas viram "Veterano" e "Ancestral"). O nível e o estágio aparecem no `/pets`. Libertar o companheiro zera a evolução.

**Evolução visível:** no estágio II o companheiro solta um pó prateado; no III, pó dourado e partículas da própria criatura (fumaça no Lobo das Sombras, chamas azuis na Raposa Espiritual, flocos no Urso do Inverno Eterno, faíscas na Ave do Trovão...). O **Corcel Celeste** ganha uma armadura branca e dourada e o **Lobo das Sombras** uma roxa-escura (se não tiverem a de um componente).

**Natureza e potencial** (2.44, referência: Pokémon): cada companheiro nasce com uma **natureza**, um ponto forte e um fraco, sorteada quando ele vira companheiro (os antigos ganham a deles na hora). Ela aparece no menu dele.

| Natureza | Efeito |
|---|---|
| Valente | +10% de dano, −10% de velocidade |
| Robusto | +12% de vida, −8% de dano |
| Ligeiro | +12% de velocidade, −10% de vida |
| Teimoso | +3 de armadura, −8% de velocidade |
| Feroz | +15% de dano, −2 de armadura |
| Calmo | +8% de vida e +4% de velocidade, −8% de dano |
| Leal | Evolui 25% mais rápido |
| Curioso | Evolui 15% mais rápido, −5% de dano |
| Medroso | +18% de velocidade, −12% de dano |
| Equilibrado | Sem ponto forte nem fraco |

Junto vem o **potencial**, de ★ a ★★★★★ (+4% de vida e dano por estrela acima da primeira; 5 estrelas é raro). Ele fica **escondido até o estágio II**; quem tem o **Baize** vê na hora.
## Detalhes do dia a dia

### Mais coisas na Forja

Na Forja do Ferreiro, estes itens também saem **forjados** (com raridade, bônus e refino):

| Item | Bônus |
|---|---|
| **Barco** (todos, com baú também) | Isca mais rápida pescando dentro dele e menos dano para quem vai dentro. Ao quebrar, devolve o próprio barco forjado |
| **Carrinho de mina** | Mais rápido nos trilhos e protege quem vai dentro |
| **Vara de pescar** | Isca mais rápida, mais chance de peixe raro e sorte |
| **Élitro** | Fogos de artifício dão mais impulso, além de armadura e vida |
| **Armadura de cavalo** | Velocidade, pulo, vida, armadura e resistência **para a montaria** |

**Reforjar:** élitro, armaduras de cavalo de ferro, ouro e diamante e qualquer equipamento achado no mundo não têm receita. Jogue o item e **1 Fragmento de Forja** em cima da Forja do Ferreiro: ele vira um item forjado. O **tridente** também (ele conta como lança na Forja): sai com raridade e bônus e pode nascer com nome.

**Ferradura** (bancada: 5 lingotes de ferro em U): clique num companheiro seu (cavalo, burro, camelo...) para dar **+15% de velocidade e +10% de pulo**, sem gastar espaço de componente. Ela cai de volta se ele for libertado ou morrer.

### Estações pequenas

| Estação | Ritual (jogue com Q) | O que faz |
|---|---|---|
| 🎯 **Boneco de Treino** | Fardo de feno + 1 abóbora esculpida + 1 suporte de armadura | Um boneco que nunca quebra: mostra o dano de cada golpe, o golpe de combo usado, o dano por segundo e o seu maior golpe. **Clique no fardo** para ver o treino **por arma** (dano por segundo, média, maior e os melhores combos, para comparar adaga, foice, espada...); agachado + clique zera. Só mede: **não dá XP** de Combate nem de proficiência. Os combos funcionam nele sem quebrar a sequência (o clique direito no boneco conta como D) |
| ⚔ **Pedra de Amolar** | Rebolo, pedra lisa ou laje de pedra lisa + 2 pederneiras + 1 lingote de ferro | Clique com a arma e 1 pederneira: **+10% de dano** pelos próximos 100 golpes |
| ♨ **Fogueira de Acampamento** | Fogueira acesa + 1 lã + 1 tronco | Sentado perto: Regeneração e, depois de 30 s, **Descansado** (+5% de XP por 20 min). Comida crua jogada do lado do fogo assa sozinha |
| ☘ **Bebedouro** | Caldeirão com água + 1 fardo de feno | Filhotes a até 8 blocos crescem 5x mais rápido, adultos esperam menos para cruzar e companheiros se curam |
| 🏆 **Estante de Troféus** | Pilar de quartzo + o **núcleo de um chefe** | O núcleo fica girando com quem venceu e quando. Cada troféu diferente dentro do seu território dá +1% de XP para quem pode construir lá |
| 🧭 **Mesa do Cartógrafo** | Mesa de cartografia + 1 bússola + 3 papéis | Com um Mapa do Tesouro na mão: diz a direção e a distância do X e desenha o caminho. Sem mapa: abre os seus Locais Ocultos |

### O mundo

- **Lápide:** ao morrer, os itens vão para um **túmulo** (a sua cabeça) onde você caiu, protegido por 15 minutos. Só você abre nesse tempo. Ao renascer, você ganha uma **Bússola do Túmulo** que aponta para ele. As masmorras e a Torre continuam com o Cofre das Almas.
- **Sentar:** clique com a mão vazia em cima de uma escada ou laje, ou use `/sentar`. Agache para levantar.
- **Ninhos de pássaro:** quebrar folhas naturais às vezes derruba um ninho. Clique com ele para abrir: ovos, penas, sementes e, raramente, pepitas ou uma esmeralda.
- **Animais raros:** 1 em 500 animais nasce **de Ouro** ou **de Neve**, com nome, brilho e o dobro de drops (mais ouro ou uma gema). Domar um dá muito XP de Doma, e os filhotes deles têm chance de nascer raros também.
- **Achados:** quebrar areia ou cascalho natural às vezes revela pepitas, pederneira, cacos de cerâmica, uma esmeralda ou uma gema bruta.
- **Orvalho da manhã:** colher no amanhecer, a céu aberto e sem chuva, dá 50% mais XP de Agricultura.

### Recordes e Diário

- **Recordes** (`/recordes`): maior golpe, maior nível total e os maiores números de Elites, chefes, masmorras, portais, itens forjados, peixes raros, magias, tesouros, minérios e animais raros. Quem toma um recorde de outra pessoa é anunciado. Escreva **`[recordes]`** na primeira linha de uma placa e ela vira um painel que mostra um recorde de cada vez.
- **Diário de Viagem** (`/diario`): um livro que se escreve sozinho com os marcos da sua jornada (primeiro diamante, primeiro chefe, Nether, End, títulos, níveis, renascimentos, companheiro lendário...), cada um com a data.
## Vida no mundo

### Ofícios

| Estação | Ritual (jogue com Q) | O que faz |
|---|---|---|
| 🛢 **Barril de Envelhecimento** | Barril vazio + 2 lingotes de ferro + 1 favo de mel | Escolha uma bebida (Hidromel, Cidra, Vinho de Frutas, Cerveja de Trigo, Licor Arcano, Licor de Ervas). Ela envelhece em **dias reais**: Nova, Envelhecida (1 dia), Reserva (3 dias) e Lendária (7 dias), cada grau mais forte e mais longo. Engarrafe com 3 garrafas de vidro |
| 🐝 **Colmeia do Apicultor** | Colmeia + 1 favo de mel + 1 flor | Com 4 flores perto, faz um mel a cada 5 min (até 6). **Mel Dourado** (girassóis por perto, Absorção II), **Gelado** (frio ou inverno, resistência ao fogo), **Floral** (5 flores diferentes, Regeneração), **Noturno** (à noite, visão noturna) ou **Silvestre**. Tire com garrafas de vidro |
| ☘ **Canteiro de Ervas** | Bloco de musgo + 1 farinha de osso + 1 vaso de flor | Uma erva a cada 10 min (até 4), que depende do lugar: **Sálvia** (lugares amenos), **Erva-de-Sol** (quente), **Musgo Lunar** (pântano), **Folha Gélida** (frio ou inverno), **Raiz Abissal** (embaixo da terra), **Flor-de-Cristal** (cerejeiras e flores) |

As ervas entram nos frascos de arremesso e no Licor de Ervas.

### Pomar

Mudas especiais crescem como árvores do jogo, mas na estação certa as **frutas aparecem penduradas embaixo das folhas**, e é assim que você sabe qual árvore colher. Clique na fruta para colher. Fora da estação, as frutas caem.

| Árvore (muda) | Fruta | Estação | Ao comer |
|---|---|---|---|
| Laranjeira (muda de carvalho) | Laranja | Outono e inverno | Regeneração por 8 s |
| Pessegueiro (muda de bétula) | Pêssego | Verão | Velocidade por 30 s |
| Cerejeira (muda de cerejeira) | Cereja | Primavera | Sorte por 1 min |
| Limoeiro (muda de acácia) | Limão | Primavera e verão | Pressa por 30 s |
| Macieira Dourada (rara) | Maçã Dourada do Pomar | Outono | Absorção por 1 min |

- **Mudas:** caem de folhas naturais (raro), vêm em ninhos de pássaro, com o Mercador Itinerante e nas carroças tombadas.
- **Colônia:** o Lenhador não derruba árvores de verdade (a produção dele é do depósito), e o **Fazendeiro colhe as frutas maduras e as plantações da estação** do território e guarda no depósito.
- **Barril:** o **Licor de Frutas** leva 6 frutas (do pomar, morango, mirtilo, uva, amora, groselha ou zimbro) e 1 garrafa de mel.
- O `/calendario` mostra as frutas da estação. Árvores em território alheio só o dono (e membros) colhem.
### Álbum de Cartas (`/album`) e Duelo de Cartas (2.47)

Monstros, chefes e peixes raros às vezes deixam uma **carta** (5% delas são **brilhantes**). Clique com ela na mão para guardar. São **86 cartas em 11 páginas**, e completar uma página dá um bônus para sempre:

| Página | Como se consegue | Bônus da página completa |
|---|---|---|
| Mortos-vivos, Artrópodes, Nether, Illagers, Estranhos, Fim | Monstros (e Pacotes) | +5% de dano naquele grupo |
| Chefes | Chefes do Altar | +5% de dano em chefes |
| Peixes raros | Pescando | +2% de chance de peixe raro |
| **Lendas** (9) | Chocar ou conquistar uma lendária (vem brilhante) e espantar o Nian | +3% de dano em tudo |
| **Mundo** (7) | Chefes das estruturas: Chefe dos Bandidos, Aprendiz Corrompido, Capitão Esquecido, Guardião Ancestral, Sumo-Sacerdote, Guardião dos Ventos e Coração da Penumbra (30%) | +5% de dano em Elites |
| **Moradores** (16) | Capítulo 5 do Laço com cada morador das vilas | +5% de XP em todos os atributos |

- **Pacote de Cartas:** 3 cartas sorteadas (chance de rara, épica e brilhante; Lendas e Moradores não saem em pacote). Vende no Peregrino e no Taverneiro (8 esmeraldas), cai de Elites (3%), dos marcos do álbum e dos duelos.
- **Pó de Carta:** o botão **Moer repetidas** transforma da 3ª cópia em diante em pó (comum 5, rara 15, épica 40); ficam sempre 2 de cada, para o baralho. Em **Criar carta** o pó vira a carta que falta, ou a 2ª cópia (comum 40, rara 100, épica 300). Lendas e Moradores não se criam.
- **Marcos:** 10, 25, 50 e 80 cartas diferentes dão Pacotes, esmeraldas, pó e uma carta brilhante.
- **Na parede:** a carta numa moldura de item mostra o ícone do bicho desenhado nela (com o pacote de recursos).

**Duelo de Cartas.** Cada carta tem custo (⚡), força (⚔), vida (❤) e às vezes um **dom**. As brilhantes têm +1/+1. A mesa tem **3 trilhas**; cada lado começa com 20 de vida e a energia cresce 1 por turno (até 6). Na sua vez você compra 1 carta, baixa as que couberem na energia e **passa o turno**: cada carta sua ataca a da frente, e se a trilha da frente estiver vazia o golpe vai direto no adversário. Carta que acabou de entrar só ataca no turno seguinte (a não ser que seja Rápida).

| Dom | Efeito |
|---|---|
| Voo | Só é bloqueada por quem também voa: senão ataca direto |
| Veneno | Quem leva dano dela perde 1 de vida por turno (2 turnos) |
| Escudo | Ignora o primeiro dano |
| Roubo de vida | Cura o dono no tanto de dano que causar |
| Explosão | Ao cair, causa 2 de dano na carta da frente |
| Rápida | Ataca no turno em que entra |
| Invocar | Ao entrar, cria um Lacaio 1/1 numa trilha livre |
| Teimosa | Na primeira vez que cai, volta com 1 de vida |
| Cura | No fim do seu turno, cura 1 de cada carta sua |
| Fúria | +1 de força cada vez que leva dano |
| Muralha | Não ataca, mas segura a trilha |

- **Baralho:** no `/album` → **Baralho**, escolha 12 cartas do seu álbum (até 2 de cada). Faltando cartas, o duelo completa com cartas de treino.
- **Contra moradores:** todo morador das vilas tem o botão **🃏 Duelo de Cartas**, com um baralho temático (o Velho Pescador joga com peixes, o Ferreiro Anão com o Nether...) e uma dificuldade: Fácil, Médio ou Difícil (Mestre, Taverneiro e Ferreiro Anão, que ainda começam com 24 de vida). Vencer dá esmeraldas (mais nos difíceis) e 30% de chance de um Pacote. A **primeira vitória do dia contra cada morador** dá um Pacote garantido.
- **Contra jogadores:** `/duelo <jogador>` desafia; o outro clica em **[Aceitar]** ou usa `/duelo aceitar` (vale 60 s). `/duelo abrir` reabre a mesa e `/duelo desistir` entrega. Cada um tem 90 s por turno (depois passa sozinho) e quem sai do jogo perde. É só pela glória: ninguém perde nem troca cartas.
- Títulos: **Duelista** (10 vitórias), **Campeão das Vilas** (10 vitórias contra moradores difíceis), **Rival de Mesa** (5 vitórias contra jogadores), **Abridor de Pacotes** (25 pacotes) e **Curador do Álbum** (os 4 marcos).

### Vanilla+ (2.48)

Pequenas melhorias com cara de jogo normal, sem item nem textura nova (cada uma pode ser desligada em `vanilla-mais` no config):

- **Mudas criam raiz:** uma muda que fica no chão por uns 40 s (fora de territórios) se planta sozinha, se o chão servir e não houver outra muda ou tronco colado. As florestas se refazem sozinhas.
- **Folhas soltam gravetos:** folha que cai sozinha às vezes deixa um graveto (8%); a de carvalho ainda tem +1% de maçã.
- **Cabeças de monstro:** zumbi, esqueleto, creeper e piglin têm 1% de deixar a cabeça (cada nível de Pilhagem soma metade disso).
- **Placa mostra o baú:** olhando para um baú ou barril com uma placa pregada nele, a barra mostra o nome da placa e o que tem dentro (os 4 itens com mais quantidade). Em território alheio, só se os baús estiverem liberados para visitantes.
- **Peixes pulando:** em rios, lagos e no mar, peixes pulam fora d'água perto de você (salmão no rio e no frio, bacalhau no mar, peixe tropical nas águas quentes). Pescar ali até 40 s depois tem 30% de chance de vir um peixe a mais.
- **Gelo fino:** ficar parado em cima de gelo sobre a água faz ele rachar (som e lascas) e quebrar em 3 s. Agachado, o gelo aguenta. Não vale dentro de territórios.
- **2.49:** **ovelhas de cores misturadas** (pais de cores que o jogo não mistura: 30% de o filhote sair a cor do meio); **animais seguem de longe** (veem a comida na sua mão a 16 blocos, no jogo são 10); **suporte de armadura com braços** (todos, inclusive os que já existiam; agachado + graveto liga e desliga, e o desligado fica assim); **escadas de mão mais rápidas**; **lanternas rangem na chuva** quando penduradas ao ar livre; **raio queima árvore** (folhas pegam fogo, se o fogo for permitido ali, e sobra carvão vegetal no pé).
- **2.56: abelhas polinizam:** colmeia ou ninho de abelha com abelhas (dentro ou voando perto) a até 8 blocos faz trigo, cenoura, batata, beterraba, frutas silvestres, caules de melão e abóbora, tocha-flor e jarro crescerem mais rápido (40% de chance de um estágio a mais), com pólen no ar. `vanilla-mais.abelhas-polinizam`.
- **2.58: estante viva:** olhando para um livro numa **estante entalhada**, a barra mostra o título e o autor (livro encantado: os encantamentos). **Agachado + clique direito** no livro abre para ler sem tirar da estante (rascunhos também). A estante vira uma biblioteca. `vanilla-mais.estante-viva`.
- **2.93: bigorna sem "Caro demais":** o custo da bigorna para em **30 níveis** (`vanilla-mais.bigorna-custo-maximo`; 0 deixa como no jogo). Dá para continuar melhorando a mesma peça para sempre.
- **2.93: consertar a bigorna:** clique numa bigorna **danificada** ou **lascada** com um **bloco de ferro** na mão: ela volta um estágio (danificada → lascada → nova). Em território alheio, só quem pode construir ali. `vanilla-mais.consertar-bigorna`.
- **2.93: separar encantamentos:** no **rebolo**, ponha um **livro encantado com 2 ou mais encantamentos** e um **livro comum**: sai um livro só com o **primeiro** encantamento, e o livro de cima fica com os outros. Repita até separar todos. Não gasta XP nem devolve XP. `vanilla-mais.separar-encantamentos`.
- **2.93: XP em garrafas:** **agachado**, clique na **mesa de encantamento** com um **frasco vazio**: 11 pontos de XP viram **1 frasco de experiência** (um por clique). Arremessado, ele devolve de 3 a 11 pontos, como no jogo, então guardar perde um pouco. `vanilla-mais.xp-em-garrafas`.
- **2.93: moldura invisível:** clique com uma **tesoura** numa moldura **com item**: a moldura some e só o item fica à vista (gasta 1 de durabilidade). Tesoura de novo: ela volta. Se o item sair, a moldura reaparece sozinha. `vanilla-mais.moldura-invisivel`.
- **2.93: bater na porta:** clique esquerdo com a **mão vazia** numa porta faz o som de batida (madeira ou ferro), para quem está do outro lado ouvir. `vanilla-mais.bater-na-porta`.

### Conforto no dia a dia (2.57)

Coisas pequenas que tiram a chatice de toda hora. Cada uma pode ser desligada em `conforto` no config.

- **Colher com clique:** clique direito em trigo, cenoura, batata, beterraba ou verruga do Nether **madura** colhe e já replanta (usa uma semente do que caiu ou do inventário). Conta como colher de verdade: qualidade ★, variedades e XP de Agricultura. Agachado, não colhe. Em território alheio, só quem pode quebrar ali.
- **Ordenar com clique:** com a mão vazia, **clique direito num espaço vazio** de um baú, barril, caixa de shulker ou baú do ender: tudo se organiza (junta as pilhas e agrupa por tipo). No seu próprio inventário, organiza a mochila (a barra de atalhos e a armadura ficam como estão).
- **Repor a mão:** acabou a pilha na mão (bloco colocado ou comida) ou a ferramenta quebrou: outra igual do inventário vai para a mão sozinha (a ferramenta pode ser de outro material, mas do mesmo tipo).
- **Portas duplas:** abrir uma porta de madeira abre a do lado (as duas folhas); portões lado a lado também. Agachado, abre só uma.
- **Elevador:** 4 lingotes de ferro em cruz com uma pérola do ender no meio (sai 2). Ponha um acima do outro, na mesma coluna: em cima dele, **pule para subir** até o próximo e **agache para descer** (até 16 blocos; precisa de 2 blocos livres em cima do destino). Quebrado, devolve o Elevador; explosões e pistões não mexem nele. Na **2.81.1**, a subida usa a posição onde o salto começou e transporta no tick seguinte ao cancelamento do pulo, evitando que o processamento do salto desfaça o transporte. O pacote de recursos da 2.81.0 continua compatível.
- **Apito do Domador:** bambu, pepita de ferro e linha (sem forma). Clique direito: seus companheiros e animais domados a até 100 blocos **vêm andando** até você (quem estava sentado levanta). 8 s de fôlego entre um apito e outro.
- **Placa de boas-vindas:** `/territorio boasvindas <frase>` (veja Territórios).

### O próprio Minecraft, com mais vida (2.61 e 2.62)

Coisas que aproveitam o que o jogo já tem. Tudo pode ser desligado em `integracao` no config.

- **Conquistas (tecla L):** uma aba **RPGAtributos** na tela de Conquistas do jogo, com 37 conquistas em árvore (Forja, combate, magia, exploração, pesca, companheiros, vilas...). Aparecem no canto e no chat como as do vanilla. Quem já tinha feito a coisa antes ganha ao entrar.
- **Livro de receitas:** as receitas do plugin (armas do arsenal, Elevador, Vara Ígnea, Apito...) aparecem no livro de receitas assim que você tem um dos ingredientes.
- **Baús das estruturas do jogo:** vilas, templos, minas, fortalezas, bastiões, portais em ruínas, cidades do End, cidades antigas, mansões, postos de saqueadores, iglus e câmaras de provação ganham, com chance, itens do plugin que combinam com o lugar (cartas, minerais, relíquias, tomos, páginas de lenda, fragmentos).
- **Conversa com os moradores:** no menu de cada morador das vilas, **💬 Conversar** abre uma janela de conversa: a conversa do dia (soma laço) e perguntas que se abrem conforme o laço cresce (de onde veio, do que tem medo, o sonho, o segredo, do que gosta de ganhar).
- **Vilas do jogo com vida:** os aldeões ganham **nome** (aparece quando você olha); **tocar o sino** da vila chama os aldeões para a praça; a cada 3 dias é **dia de feira** (de dia): festa em volta do sino, trocas **20% mais baratas** e às vezes chega um mercador ambulante.
- **Ferrovias:** em trilho reto e plano (8 blocos ou mais), o carrinho com gente acelera até o **dobro da velocidade** e volta ao normal antes das curvas. Um **sino ao lado do trilho** vira estação: toca quando o carrinho passa.
- **Barco a vela:** a **Vela de Barco** (lã em cruz com 2 gravetos) vai num barco comum com um clique. A vela ocupa o segundo lugar do barco; agachado + clique (mão vazia) tira.
- **O vento (2.66):** sopra de um lado e muda 4 vezes por dia de jogo (a virada é aos poucos). Remando para a frente com vela, o vento **por trás** dá a velocidade máxima, **de lado** a metade e **de frente** quase nada (sobra o remo). A força vem do tempo: **fraco** com tempo bom, **bom** na chuva e **forte** na Ventania (que ainda empurra o barco um pouco para o lado do vento). A vela gira para pegar o vento, a barra mostra "Vento a favor / de lado / contra", e o vento aparece no `/calendario` e no placar (quando você está num barco). Os projéteis desviados pela Ventania seguem esse mesmo vento.
- **Barco Grande (2.63):** 7 tábuas e 2 barcos comuns na bancada (`PBP / PBP / PPP`, barcos no meio). Clique na água com ele: aparece um barco comprido de abeto com convés, bordas e 2 bancos. Leva **5 pessoas**: o primeiro a subir pilota, os outros sentam nos 4 bancos (é só clicar no barco). Com a Vela de Barco, o mastro ocupa um banco (**4 pessoas**) e ele corre como o barco a vela. Agachado + clique com a mão vazia tira a vela; sem vela e sem ninguém a bordo, recolhe o barco (volta o item).
- **Comboio de barcos (2.63):** do próprio jogo: ponha o laço num barco e, agachado, clique em outro barco: o primeiro fica amarrado no segundo. Um barco a vela puxa os de trás (2 pessoas em cada, ou baús nos barcos com baú). O Barco Grande também entra no comboio, puxando ou sendo puxado.
- **Para-raios guarda o raio:** quando um raio cai num para-raios, ele solta faíscas por 15 minutos. Clique nele para pegar a carga: Pressa II e Velocidade por 3 minutos, e a próxima peça forjada (em até 30 minutos) tem **+8% de chance de sair uma raridade acima**.
- **Começo guiado:** quem entra pela primeira vez recebe uma janela de boas-vindas com 3 primeiros passos (raça, Jornada, Manual). Com 30 minutos de jogo, uma janela sobre Forja, classe e companheiro; com 2 horas, sobre party, território, colônia e reino. Quem desligou as dicas (`/guia dicas`) não recebe as duas últimas.

### Conjuntos dos minerais (2.64)

Espada, picareta, machado, pá, enxada e as 4 peças de armadura de **Âmbar, Jade, Pérola, Gelo (Cristal de Gelo), Pedra-Sangue, Magma (Cristal de Magma) e Quartzo Negro**. A receita é a do jogo, com o mineral do plugin no lugar do ferro (o item comum por baixo dele não serve). O Mitrilo fica de fora: ele já tem as peças **✦ Mitrilo**.

- **Força:** entre o ferro e o diamante. Cada peça de armadura tem +1 de armadura e +1 de dureza sobre a de ferro; a espada, +0,5 de dano; as ferramentas duram 60% mais que as de ferro (a armadura, 50%).
- **No corpo:** a armadura aparece com a cor do mineral (pacote de recursos).
- **Bônus de conjunto:**

| Conjunto | 2 peças | 4 peças |
|---|---|---|
| Âmbar | Sorte I | Sorte II e +10% de chance de minério a mais com qualquer picareta |
| Jade | +1 ❤ | +2 ❤ e recupera meio coração a cada 8 s |
| Pérola | Respira debaixo d'água | Nada como golfinho e +15% de dano na água ou na chuva |
| Gelo | Não congela na neve fofa | Quem bate em você fica lento |
| Pedra-Sangue | Rouba 5% do dano em vida | Rouba 12%, mas a vida máxima cai 1 ❤ |
| Magma | -30% de dano de fogo e lava | Resistência ao Fogo |
| Quartzo Negro | Os piglins não atacam | Força I no escuro |

- **Picaretas com poder:** a de **Âmbar** dá 10% de chance de o minério soltar um a mais; a de **Magma** funde o minério (ferro, ouro e cobre saem em lingote, com um pouco de XP).
- **Conserto:** na bigorna, só com o mineral do plugin.
- **Forja e Reciclagem:** forjadas na Forja do Ferreiro, saem com raridade e continuam do conjunto; a Reciclagem devolve o mineral.

### Utilidades do dia a dia (2.65)

| Utilidade | Receita | O que faz |
|---|---|---|
| **Chave de Construtor** | `I I / S / S` (2 ferros, 2 gravetos) | Clique direito num bloco: gira (escadas, troncos, alçapões, cabeças, fornalhas...). Agachado: vira de cabeça para baixo (escada, laje, alçapão). Portas, camas e baús duplos não giram. Respeita territórios. |
| **Funil com filtro** | — | Uma moldura com um item presa no funil: o funil só aceita aquele item (várias molduras: qualquer um deles). |
| **Regador** | 4 lingotes de cobre e 1 balde | Clique na água: enche (20 regadas). Clique na plantação: molha um 3x3 e cada planta tem 40% de chance de subir um estágio. |
| **Lanterna de mão** | — | Segurando tocha, lanterna, pedra luminosa e afins (em qualquer mão), o lugar onde você está fica iluminado enquanto anda. A luz é desenhada só para quem está perto: o mundo não muda. |
| **Bancada de Bolso** | bancada de trabalho e 3 couros | Clique direito: bancada de trabalho. Agachado: pedra de amolar, tear, cortador de pedras, mesa de cartografia ou de ferraria. |
| **Balde de Gelo** | 3 Cristais de Gelo e 1 balde | Guarda até 4 baldes de água ou de lava (um líquido só). Mirando numa fonte, enche; agachado (ou sem fonte na mira), despeja. Não serve para leite, peixes nem caldeirão. |
| **Trena** | corante amarelo, 3 ferros e linha | Clique num bloco e depois noutro: largura × altura × comprimento, quantos blocos e a distância, com uma caixa de partículas por 15 s. Agachado + clique apaga. |
| **Cinto de Ferramentas** | 3 couros, 2 linhas e 1 ferro | Clique direito: guarde até 9 ferramentas. Segurando o cinto, bata num bloco: a melhor ferramenta para ele vem para a mão (e troca sozinha em outro tipo de bloco). Parou 4 s, trocou de espaço, abriu um inventário, soltou ou morreu: ela volta para o cinto. |

Cada uma pode ser desligada em `utilidades:` no config.

### Carroça (2.67)

**Receita:** baú em cima, um barco comum no meio entre 2 tábuas e 2 gravetos embaixo (`C / PBP / S S`). Clique no chão para pôr.

- **Atrelar:** laço na carroça (ela fica presa em você) e depois clique num **cavalo, mula, burro, lhama ou camelo domado**. O animal anda 15% mais devagar enquanto puxa.
- **Usar:** clique abre a carga (27 espaços; 54 com o Baú duplo). Agachado + clique solta do animal (o laço fica no chão) ou, com o Banco, senta. Batendo, ela quebra e volta a ser item, com as melhorias (a carga cai no chão).
- **Como anda:** ela é um barco com baú do próprio jogo; o plugin só ajusta a velocidade dela enquanto está atrelada (sem peças desenhadas seguindo o animal). Sobe degraus de 1 bloco com um pulinho, segue o trote e o galope, e na água boia como barco. Se ficar a mais de 10 blocos do animal, ela se solta.
- **Melhorias** (clique na carroça com o item):

| Melhoria | Item | O que faz |
|---|---|---|
| Rodas de ferro | 1 bloco de ferro | mais rápida em estrada (caminho, cascalho, pedra, tijolo) |
| Baú duplo | 1 baú | 54 espaços |
| Toldo | 3 lãs | monstros, flechas de esqueleto e explosões não a quebram |
| Lanterna | 1 lanterna | ilumina em volta enquanto anda (luz só desenhada, o mundo não muda) |
| Freio | 1 alavanca | solta e parada, não escorrega (gelo, ladeira) |
| Banco | 1 laje de madeira | leva uma pessoa sentada em cima da carga |

- **Visual:** sem mod, é o barco com baú de abeto. Com EMF + ETF, vira uma carroça com 4 rodas e sem remos (2.67.1; ela se chama "Carroça", com o nome escondido, e o modelo vale por esse nome).

### Estradas (2.94–2.98)

Quem constrói a estrada é o jogador, com os blocos que quiser; o plugin reconhece o piso. Tudo se desliga em `estradas` no config.

- **Trilhas que se formam:** grama, terra, terra grossa, podzol, micélio ou terra enraizada **muito pisados** viram **caminho de terra**. Cada vez que você pisa num bloco conta 1 passo (montado, 2; de carroça, 3); com **20 passos** no mesmo bloco, sem ficar uma hora esquecido, a trilha aparece (o mato de cima some; com flor ou bloco em cima, não forma). Assim os caminhos que todo mundo usa se desenham sozinhos.
- **Trilha esquecida volta a ser grama:** se ninguém passar por ela em **10 dias do jogo**, ela volta a ser o bloco de antes. Só as trilhas que nasceram sozinhas somem; caminho feito com pá fica para sempre.
- **Em território:** a regra **"Trilhas se formam"** (ligada por padrão, também nos territórios antigos) deixa o dono desligar.
- **Mais rápido na estrada** (a pé ou montado; o bônus vai para a montaria):

| Piso | Blocos | Velocidade |
|---|---|---|
| Trilha | caminho de terra | +5% |
| Cascalho | cascalho, barro compactado | +8% |
| Calçada | pedregulho (e musgoso), pedregulho de ardósia, tijolos de todo tipo, ladrilhos, laje e escada de pedra | +12% |
| Estrada Real | pedra polida (andesito, diorito, granito, ardósia, pedra-negra, tufo...), pedra lisa e arenito cortado | +15% |

  Lajes e escadas contam como o bloco delas. Pedra comum inteira não conta (senão as cavernas seriam estradas). Pulando, o bônus continua; nadando, voando ou planando, sai.
- A **Carroça** também reconhece todos esses pisos como estrada (as Rodas de ferro rendem neles).
- **2.95: pontes e neve.** Tábua, laje ou escada de madeira com **água ou vão embaixo** é ponte e conta como cascalho (+8%); no chão, como piso de casa, não conta. Camada de neve por cima não apaga a estrada: vale o piso de baixo.
- **2.95: placa de direção.** Escreva `[direção]` na primeira linha e o nome de um lugar nas outras. Valem as **colônias** ("Colônia de Fulano" ou só "Fulano"), as **Pedras de Viagem públicas** e as **estruturas que você já visitou**. A placa se reescreve com o **rumo** (Norte, Nordeste...), o nome e a **distância contada da própria placa** ("» Nordeste « / Colônia de Ana / a 340 blocos") e fica encerada. Nome errado: a placa avisa e mostra os lugares mais perto. Nada aparece na tela; é só a placa. `estradas.placas-de-direcao`.
- **2.95: Pouso de Viajante.** Uma **Fogueira de Acampamento** a até 4 blocos de um piso de estrada vira pouso: sentado perto dela, você fica Descansado em **15 s** (em vez de 30) e, enquanto estiver Descansado, anda **+5%** a mais na estrada. `estradas.pouso` e `estradas.bonus.pouso`.
- **2.96: marcos e estradas com nome.** Uma placa com `[estrada]` na primeira linha e o **nome** nas duas seguintes, a até 2 blocos do piso, é um **marco**. Ao pôr um marco, o plugin procura os marcos da **mesma estrada** a até **160 blocos** e confere se dá para ir de um ao outro **só pisando em estrada** (trilha, cascalho, calçada, Estrada Real ou ponte; subindo e descendo 1 bloco, também na diagonal). Achou: o trecho fica ligado. A placa vira "═ Estrada ═ / nome / marco nº 3" e fica encerada.
  - **Clique no marco:** confere os trechos dele de novo (completou a estrada? clique) e mostra quantos marcos estão ligados, o comprimento, a **iluminação** (quanto do caminho tem luz de tocha, lanterna etc.), as colônias por onde ela passa e o rumo dos marcos vizinhos.
  - Quebrar a placa tira o marco da estrada.
- **2.96: colônias ligadas.** Se uma estrada contínua passa por **duas colônias** (marco dentro da colônia ou a até 48 blocos da prefeitura), elas ficam ligadas:
  - o **Mercador** vende 1 lote a mais por colônia ligada (até +2);
  - a **felicidade** sobe (+5 por ligação, até +10: os moradores se visitam);
  - saem **caravanas**: a cada 40 min, uma caravana leva as **sobras do depósito** (2 + nível da colônia em lotes, como o Mercador) para a outra colônia e **volta com esmeraldas** (1,5 por lote) para o depósito de origem. A colônia de destino ganha um pouco de felicidade (dia de feira). Nada vai de um jogador para outro: a carga sai e volta para a mesma colônia.
  - **Assaltos:** a chance vai de 3% a 35% e cai com a **iluminação** da estrada e com **soldados** na colônia. Se ninguém estiver perto, a carga se perde. Se alguém estiver perto da caravana, ela aparece **andando pela estrada** (uma mula com carga e o nome da colônia) e o assalto vira luta: **salteadores** atacam a mula. Mate todos e a caravana segue (quem defendeu ganha 2 esmeraldas); se a mula morrer, a carga se perde.
- **2.96: encontros na estrada.** Andando numa estrada **iluminada**, os bandidos dão lugar a um viajante ou a uma carroça; de noite, numa estrada **escura**, a estrela cadente vira bandidos.

- **2.98: gente nas estradas.** De dia, perto de uma estrada com marcos ligados, às vezes alguém vem andando por ela (aldeões que não ficam salvos no mundo e somem quando ninguém está perto). Clique para falar:
  - **Visita** (estrada que passa por 2 colônias): um morador de verdade de uma colônia, com o nome e a roupa dele, indo visitar a outra.
  - **Peregrino:** conta as **notícias** do mundo (chefes derrotados, caravanas, Lua de Sangue, bandos...) e dá a **Bênção do Caminho** (Sorte por 5 min, uma vez por peregrino).
  - **Mensageiro:** pede uma coisa para a viagem (6 pães, 16 tochas, 3 couros...). Segure e clique: paga em esmeraldas.
  - **Mercador de Estrada:** 3 trocas sorteadas de coisas de estrada (andesito polido para calçar, lanternas, mapa vazio, placas, pão; compra cascalho e couro).
  - **Visita na praça:** na prefeitura de uma colônia ligada, às vezes há um morador da outra colônia de visita, parado na praça por uns minutos.
  - `estradas.andarilhos` e `andarilhos-minutos`.

### Mundo vivo (2.97)

Tudo se desliga em `mundo-vivo` no config.

- **Rotina dos animais:** o **Comedouro** vira o celeiro. No fim da tarde ele toca um **sino de vaca** e os bichos da fazenda a até 24 blocos (vaca, ovelha, porco, galinha, cabra, coelho) vão para perto dele; de noite, quem se afasta volta. Com o comedouro debaixo de um teto, eles dormem cobertos (e o afeto não cai no inverno). De manhã, cada um sai para um pedaço de pasto a céu aberto. Bichos no laço, montados, companheiros e guardiões ficam de fora.
- **Sons do mundo:** na **lua cheia**, em florestas, taigas, neve e montanhas, ouvem-se lobos ao longe (o 26.3 não tem mais o uivo; é o ganido grave do lobo). De noite, nos **pântanos**, um coro de sapos. Ao **amanhecer**, o sino da prefeitura de cada colônia toca duas vezes.
- **Bandos que atravessam o mapa:** quem explora de dia, a céu aberto e fora de território, às vezes vê passar um bando do bioma: **manada de cavalos** (planície, savana), **rebanho de cabras-monteses** (montanha), **alcateia** (taiga e neve no inverno), **raposas** (floresta, taiga), **camelos** (deserto), **lhamas** (badlands) ou **papagaios** (selva). O bando anda num rumo, para a **estrutura mais perto que você ainda não visitou** (até 800 blocos), ou só para longe. **Siga o bando:** ao chegar, o líder vira um **animal raro** (de Ouro ou de Neve) e fica ali; os outros se espalham. Os bichos do bando somem se ninguém estiver perto, menos os que você domar no caminho. No máximo um bando a cada 30 min por jogador.

### Mapas (2.99)

Feitos na **Mesa do Cartógrafo** (a estação: mesa de cartografia + bússola + 3 papéis). `mapas.ativados`.

| Na mão, clique na mesa | Sai |
|---|---|
| 1 mapa vazio | **Mapa da Região**: 1024 blocos em volta da mesa |
| Agachado, 4 ou 9 mapas vazios | **Mural** 2×2 ou 3×3: mapas de 512 blocos, lado a lado, com o nome "linha, coluna" |
| Bússola (e 1 mapa vazio no inventário) | **Mapa do Explorador**: 2048 blocos |

- **Por cima do mapa**, o plugin desenha sozinho as **estradas com nome** (marrom), as **colônias** (bandeira com o nome), as **Pedras de Viagem públicas** e as **caravanas** andando (marcador azul; X vermelho quando estão sendo assaltadas). O terreno do Mapa da Região e do Mural se desenha como no jogo: segure e ande pela área. Pendurado na parede, o mural continua mostrando as caravanas se mexendo.
- **Mapa do Explorador:** se preenche sozinho por onde você anda com ele **no inventário** (não precisa segurar), uns 64 blocos em volta. Marca as **estruturas que você já visitou**, as colônias, as estradas e a sua seta.
- **Cartógrafo da colônia:** profissão nova (nível 3 da colônia; posto: Mesa do Cartógrafo, dentro da Biblioteca). Com mapas vazios no depósito, de vez em quando desenha um Mapa da Região da colônia ou um **Mapa de Estrutura** com um X numa estrutura que o dono ainda não visitou (até 1500 blocos). O Laço dele ensina **Pés na Estrada** (+5% na estrada).
- O desenho por cima fica anotado em `mapas.yml` (e os pixels do Explorador em `mapas/<id>.bin`) e volta quando o servidor liga.

### Artefatos de utilidade (2.49)

Feitos na bancada; as Garras, a Corda e a Bolsa gastam **Vigor**.

| Artefato | Receita | Uso |
|---|---|---|
| Garras de Escalada | 2 pederneiras, 2 ferros e 1 couro (`F F` / `ILI`) | Na mão (qualquer uma), de frente para a parede: segure pular para subir; agachado no ar fica pendurado. Vidro, gelo, slime e mel escorregam. Sem Vigor, escorrega. |
| Corda de Rapel | 8 linhas em volta de um gancho | Em pé na beira de um barranco (4+ blocos de queda), clique: desce devagar pela corda (até 40 blocos). Agachado segura, pular sobe, clique de novo solta. Sem dano de queda. 30 descidas. |
| Pêndulo do Rabdomante | linha, pepita de ouro e fragmento de ametista (em coluna) | Na mão, diz se uma estrutura ainda não descoberta (até 160 blocos) e um mineral raro (até 24) estão frios, mornos, quentes ou "fervendo". |
| Cantil | linha, 4 couros e uma garrafa | Olhando para a água, clique para encher (3 goles). Um gole tira o calor por 5 minutos, apaga o fogo e cura um pouco. |
| Bolsa de Sementes | 2 linhas, 5 couros e uma semente | Clique na terra arada: planta 3×3 com as sementes do inventário. Agachado troca a semente. |


### No mundo

- **Mercador Itinerante:** de tempos em tempos (cerca de 1h30) ele arma a tenda perto de alguém por 20 minutos, com 6 ofertas raras (entre elas, às vezes, sementes da estação, um óleo de lâmina ou peles para o curtume): Fragmento de Forja, Pedra de Proteção, gemas, elixires, mapas, cartas, bebidas e às vezes Essência Primordial. O servidor é avisado de onde ele está.
- **Encontros na estrada:** quem explora sozinho a céu aberto às vezes encontra uma **carroça tombada** com carga, um **acampamento de bandidos** guardando um saque (só abre depois de vencer todos), um **viajante ferido** (dê comida ou cura e ele recompensa) ou uma **estrela cadente** que deixa cristais e gemas.
- **Segredos** (`/segredos`): 12 conquistas escondidas (dormir no Nether e sobreviver, cair de 60 blocos, pescar num eclipse...). Cada uma dá esmeraldas e XP e entra no Diário.

### Flechas e frascos

| Item | Receita (bancada) | Efeito |
|---|---|---|
| Flecha de Fogo | 4 flechas + pó de blaze → 4 | Incendeia o alvo |
| Flecha de Gelo | 4 flechas + gelo compactado → 4 | Congela e deixa bem lento |
| Flecha Rastreadora | 4 flechas + pérola do ender → 4 | Persegue o monstro mais perto |
| Flecha de Corda | 2 flechas + laço → 2 | Onde cravar, você é puxado até lá |
| Flecha Explosiva | 4 flechas + TNT → 4 | Explosão pequena que não quebra blocos |
| Frasco de Fumaça | garrafa + pólvora + Musgo Lunar → 2 | Os monstros perdem você de vista |
| Frasco de Cola | garrafa + 2 slimes → 2 | Prende quem estiver perto |
| Fogo-Grego | garrafa + pólvora + Erva-de-Sol → 2 | Incendeia em volta |
| Frasco de Cura | garrafa + melancia reluzente + Sálvia → 2 | Cura você e quem estiver perto |

### Conforto

- **`/rpg`:** o menu central, com os próximos passos, as categorias e a Jornada (veja "Interface: /rpg, Guia e Jornada").
- **`/placar`:** liga ou desliga um placar do lado da tela com nível, vigor, mana, estação, lua, hora, Descansado e pontos de talento.
## Pesca

Pescar dá XP de Pesca. Com o nível sobem a velocidade da isca, a chance de vir 2 peixes e a qualidade (★ a ★★★) dos peixes, que melhora pratos e elixires. Sorte do Mar na vara também ajuda.
- **Peixes raros:** 12 espécies que só mordem em certos lugares, horários ou climas (mais 5 do gelo e 4 da lava, abaixo). `/peixes` abre o **Diário do Pescador**, com onde procurar cada um.
- **Tesouros do Mar:** esmeraldas, ouro, diamantes, livros encantados, Pérolas Negras e, raramente, Mapa Rasgado ou Fragmento de Forja.
- **Criaturas marinhas:** às vezes algo grande morde a isca e luta de volta (Afogado Faminto, Guardião das Ondas, Capitão Afogado e o gigante Senhor das Marés). Elas deixam Escamas do Abismo e Pérolas Negras.
- **Peso dos peixes (2.50):** todo peixe vem com **peso** ("🎣 Salmão — 3,2 kg"), que depende da espécie, da qualidade ★ e do nível de Pesca (os raros pesam mais). O maior de cada espécie vira **recorde pessoal** e, se for o maior do servidor, entra nos `/recordes` (e é anunciado quando alguém toma o recorde). Para os peixes continuarem empilhando, só os **troféus** (novo recorde pessoal ou peixe enorme) levam o peso escrito no item. O Diário do Pescador mostra seus recordes e os do servidor.

| Peixe | Onde/quando | Pesca |
|---|---|---|
| Carpa Dourada | Rios e lagos, longe do mar | 0 |
| Truta Arco-Íris | Rios, de dia | 0 |
| Peixe-Lua | Qualquer água, de noite | 5 |
| Peixe-Pedra | Águas de caverna (abaixo da altura 45) | 10 |
| Enguia Elétrica | Debaixo de chuva | 15 |
| Peixe-Gelo | Biomas de neve | 20 |
| Koi Celeste | Cerejeiras e campos de flores | 25 |
| Baiacu-Rei | Oceanos mornos ou quentes | 30 |
| Salmão-Rei | Rios e oceanos frios | 35 |
| Peixe-Fantasma | Pântanos e manguezais, de noite | 45 |
| Peixe Abissal | Oceanos profundos, de noite | 60 |
| Peixe-Dragão | Oceano, durante tempestade (raríssimo e anunciado) | 85 |

### Pesca no gelo

Quebre o gelo de um lago ou mar **gelado** (ou de **qualquer lago congelado no inverno**) e pesque no buraco: o anzol precisa estar numa água cercada de gelo (vale gelo, gelo compactado, gelo azul e bloco de neve). No buraco há **+5% de chance de peixe raro** e 5 peixes que só saem ali:

| Peixe do gelo | Quando | Pesca | Para quê |
|---|---|---|---|
| Truta-do-Gelo | De dia | 10 | Caldo Quente do Pescador (Cozinha) |
| Lúcio Polar | Qualquer hora | 25 | Óleo Gélido sem gelo compactado (Alquimia, sai 3) |
| Peixe-Lanterna Glacial | De noite | 35 | Destilado vira 2 Cristais de Mana (Alquimia) |
| Enguia de Cristal | Durante uma nevasca | 50 | Sorvete de Cristal (Cozinha) |
| Esturjão Ancestral | De noite, no inverno (raríssimo e anunciado) | 80 | Caviar Ancestral (Cozinha) |

**Cabana de Pesca:** jogue **1 vara de pescar, 1 bacalhau cru e 4 tábuas de abeto** numa **fogueira**. A até 12 blocos, a isca afunda **30% mais rápido**, há **+4% de peixe raro** (em qualquer água) e **ninguém sente frio**. Quebrar a fogueira devolve a vara.

### Pesca na lava (2.56)

A **Vara Ígnea** (vara de pescar + 2 Cristais de Magma + 1 vara de blaze, sem forma) pesca na **lava do Nether**: a boia não afunda, fica boiando em cima. Depois de um tempo a lava **estala** e a boia mergulha: recolha **logo em seguida** (1,5 s). Pesca alta faz a mordida chegar mais rápido. Na água, a Vara Ígnea pesca como uma vara comum.

Na lava vêm peixes da lava (~57%), **Tesouros da Lava** (18% a 24%, conforme a Pesca: ouro, Cristais de Magma, Âmbar Infernal, Quartzo Negro, livros de Proteção contra Fogo ou Aspecto Flamejante, sucata de netherite e, raramente, detrito ancestral), lixo do Nether (pepitas de ouro, quartzo, creme de magma, pó de blaze, basalto) e às vezes um **cubo de magma** ou um **strider** perdido.

| Peixe da lava | Onde | Pesca | Para quê |
|---|---|---|---|
| Peixe-Brasa | Qualquer lava do Nether | 10% | Peixe-Brasa na Pedra |
| Enguia de Magma | Deltas de basalto | 30% | Caldo Vulcânico |
| Carpa de Almas | Vale de areia de almas | 40% | Ensopado das Almas |
| Salamandra de Lava | Qualquer lava (raríssima e anunciada) | 75% | Banquete Infernal |

Título **Pescador do Inferno**: pescar 25 vezes na lava.

## Curtume e tecelagem

**Ritual:** jogue **1 tesoura, 4 couros e 2 linhas** em cima de um **tear**: ele vira o **Ateliê do Curtidor** (agachado + clique abre o tear normal). Três abas:

- **Curtir:** pele crua + 1 farinha de osso = couro curtido. **2 membranas de phantom** + 1 farinha de osso = Couro Noturno.
- **Tecer:** 4 linhas = Tecido de Linha · 3 lãs (qualquer cor) = Feltro · 4 algas secas = Tecido de Alga. Shift + clique faz até 8.
- **Costurar:** as 24 peças dos 6 conjuntos. Capuz = 3 couros + 1 tecido · Gibão = 5 + 2 · Calças = 4 + 2 · Botas = 2 + 1.

**Peles:** lobos selvagens (50%), ursos polares (65%, até 2), raposas (50%), guardiões (35%; o guardião ancião deixa 3 a 5) e hoglins/zoglins (50%, até 2). Elites sempre deixam uma a mais. Fazer as peças dá XP de Ferraria.

| Conjunto | Couro + tecido | 2 peças | 4 peças |
|---|---|---|---|
| Caçador | Lobo + Linha | +5% de velocidade | +10% de dano com flechas e tridente; na postura Ágil, +8% de dano e a esquiva gasta 25% menos vigor |
| Urso Polar | Urso + Feltro | +1 ❤ de vida máxima | -10% de dano recebido e resiste a empurrões |
| Raposa | Raposa + Linha | Sorte (saque e pesca melhores) | De noite: visão noturna e velocidade |
| Maré | Escamas + Alga | Respira debaixo d'água | Graça do golfinho na água e +5% de peixe raro |
| Noite | Noturno + Linha | -50% de dano de queda | Caindo do alto, você desce devagar |
| Brasa | Brasa + Feltro | -30% de dano de fogo e lava | Não sente calor e, com pouca vida, ganha Força |

Cada peça tem cor própria e **+1 de armadura** além do couro comum, e protege do frio como todo couro. **Na Forja:** jogue a peça e 1 Fragmento de Forja na Forja do Ferreiro (o Reforjar): ela sai com raridade e status de forja, e continua contando para o conjunto. O refino também funciona.

## Sonhos

Quem **dorme a noite inteira** tem 40% de chance de sonhar com um lugar que ainda não conhece: uma estrutura (até naufrágios e cidades submersas, a até 3000 blocos) ou um **Local Oculto**. Ao acordar, o jogador sabe para que lado fica (e a distância, no caso das estruturas). Um sonho por dia, nunca o mesmo lugar duas vezes; os sonhos ficam anotados no `/diario`.

## Alquimia

**Ritual:** jogue **2 garrafas de vidro** e **1 pó de blaze** em cima de um **suporte de poções**: ele vira a **Bancada Alquímica**.
- **Clique direito:** a bancada, com 5 abas (Elixires, Componentes, Gemas, Acessórios e Engastar). **Shift + clique** cria até 8 de uma vez.
- **Agachado + clique:** o suporte de poções normal. Poções comuns feitas nele também dão XP de Alquimia.
- `/alquimia` mostra todas as receitas em qualquer lugar.

**Componentes:** Pó Arcano, Óleo de Peixe, Tintura Vital, Sal Lunar, Mercúrio Vivo, Cristal de Mana e Solvente Alquímico são feitos na bancada (vários usam peixes raros). Pérola Negra e Escama do Abismo só vêm da pesca. Os **óleos de lâmina** (Fogo, Gélido, Venenoso, Trovejante e Prata) também ficam nesta aba; veja "Ligações" em Combos.

**11 elixires:** Cura, Rapidez, Minerador, Mergulhador, Sorte, Pedra, Sombras, Mana, Gigante, Fênix e Titã. São mais fortes e mais longos que as poções comuns, e vários têm 2 a 4 efeitos. A qualidade (sorteada pelo nível, melhor com ingredientes ★★+) e o nível de Alquimia aumentam a duração.

### Gemas

Rubi, Safira, Esmeralda, Topázio, Ametista e Ônix, em 3 graus: **Bruta**, **Lapidada** e **Perfeita** (3 de um grau + material = 1 do próximo). Elas são engastadas em **itens forjados** na aba Engastar.
- **Engastes por raridade:** Comum 0, Raro e Épico 1, Único e Lendário 2, Mítico 3.
- Tirar uma gema gasta 1 Solvente Alquímico e devolve a gema inteira. Reciclar o item também devolve as gemas.

| Gema | Arma | Arco/besta | Armadura/escudo | Ferramenta |
|---|---|---|---|---|
| Rubi | Dano | Dano de flecha | Vida máxima | Vel. de mineração |
| Safira | Crítico | Crítico | Armadura | Eficiência |
| Esmeralda | Roubo de vida | Roubo de vida | Esquiva | Sorte |
| Topázio | Vel. de ataque | Durabilidade | Velocidade | Durabilidade |
| Ametista | Alcance | Dano de flecha | Resistência | Alcance de blocos |
| Ônix | Repulsão | Durabilidade | Reflete dano | — |

### Acessórios

`/acessorios` (ou o botão no menu de atributos): **2 anéis, 1 amuleto, 1 cinto e 2 espaços de bolso**. Eles não ocupam a armadura e **não caem quando o jogador morre**. Não dá para usar dois acessórios iguais.

| Tipo | Acessórios |
|---|---|
| Anéis | Vigor (+2 ❤), Força (+1 dano), Vento (+5% velocidade), Marés (respiração aquática), Brasas (resistência ao fogo), Minerador (Pressa), Fortuna (+sorte), Sábio (+5% XP em tudo), **Jade** (+1 ❤ e +1 armadura; 3 Jades), **Gelo Eterno** (+2 de eficiência de mineração e mais rápido debaixo d'água; 3 Cristais de Gelo) |
| Amuletos | Mana (+40 mana, +1/s), Guardião (+2 armadura, +1 resistência), Vida (Regeneração), **Fênix** (escapa da morte a cada 10 minutos), **Ventos** (+6 de queda segura e +8% de velocidade; 2 Fragmentos Celestes), **Pingente de Âmbar** (+5% XP e +1 sorte; 3 Âmbares), **Colar de Pérolas** (fôlego e mineração debaixo d'água; 4 Pérolas), **Sangue** (+2 de dano, −1 ❤; 3 Pedras-Sangue) |
| Cintos | Atleta (pulo e queda segura), Andarilho (sobe blocos sem pular), Titã (+2 ❤ e resistência a repulsão) |
| Bolso | **Lanterna de Bolso** (ilumina em volta, sem colocar blocos), Ímã (puxa itens do chão), Relógio (agachado: hora, coordenadas e bioma), **Capa Planadora** (no ar, pule para planar), **Balão de Cristal** (4 lãs brancas + 4 linhas: plana mais devagar e estoura em 5 usos) |

## Guarda-roupa

`/guardaroupa` (ou Cosméticos → Guarda-roupa): escolha uma peça de armadura do inventário para ser a **aparência** de cada parte do corpo. Você continua com a proteção da armadura de verdade, só o visual muda, e todos veem igual (sem mod).
- Na **cabeça** vale qualquer item (bloco, flor, cabeça...).
- Dá para **esconder** uma peça (ela fica invisível).
- A aparência só aparece por cima de uma peça vestida. Ao tirar a peça, ela volta ao normal sozinha.

**Personalização do personagem:** o botão **Personalizar personagem** abre nove espaços independentes: **Barba, Chifres, Orelhas, Rosto, Coroa/Auréola, Barbatanas, Cabelo, Bigode e Brincos**. Dá para usar barba e chifres juntos e combinar até um enfeite de cada categoria. Abra **Coleção de enfeites**, pegue as peças gratuitas e clique nelas no inventário com o Guarda-roupa aberto. Clique em um enfeite usado para devolvê-lo. O botão **Aparências de armadura** volta à página das roupas.

São **27 enfeites**: os sete originais, dez opções de barba, chifres, orelhas e rosto da 2.80, e dez opções de cabelo, bigode e brincos da 2.81. A escolha é livre e não troca a raça. Os enfeites combinados funcionam **com ou sem capacete**; quando existe um capacete, substituem sua aparência e mantêm a proteção, os encantamentos e os efeitos. As aparências das outras partes da armadura continuam funcionando. O enfeite antigo do espaço de cabeça migra para sua categoria. A composição visual exige o pacote de recursos atualizado; a movimentação dos itens ainda precisa de validação no jogo.

## Altar da Convergência (2.83)

Uma **âncora de renascimento** com 1 olho do ender, 1 diamante e 4 fragmentos de ametista jogados em cima vira o altar (`/rpgadmin convergencia` cria direto). Em volta dele ficam até **8 pedestais**, a 3 blocos: 4 em cruz e 4 na diagonal (2+2), na mesma altura do altar ou 1 acima/abaixo. Pedestal é qualquer bloco lapidado (pedra, ardósia, quartzo, pedra-negra, tufo, arenito, tijolos do Nether, resina) ou pilar de quartzo/púrpura, e pode ter 2 blocos de altura.

- Clique num pedestal com um item para pôr nele (agachado: só 1); clique de novo para tirar. O item fica flutuando e girando em cima, e é guardado ali mesmo (sobrevive a reinícios). Pedestal quebrado, empurrado ou explodido derruba o item.
- Com o **catalisador** na mão, clique no altar. Os pedestais com item precisam bater exatamente com a receita, um ingrediente por pedestal.
- **Cerimônia de 10 s**: os itens sobem dos pedestais, giram em volta do altar cada vez mais rápido e mais perto, com feixes até o centro; o céu de quem está perto escurece até a meia-noite e chove; caem raios (só visuais) e a câmera treme; os itens são sugados para o centro e explodem (sem dano, só empurra de leve). O resultado cai brilhando, e só quem acendeu pode pegar.
- Altar quebrado no meio: tudo volta para o chão. Servidor desligando: o resultado cai no altar.
- Mão vazia no altar abre o **Grimório** com todas as receitas; agachado, conta os pedestais.

| Transmutar (na mão → pedestais) | Resultado |
|---|---|
| 4 fragmentos de ametista → 16 sculk, 4 pérolas | 2 Fragmentos de Eco |
| 2 lingotes de ouro → 16 quartzo, 8 areia das almas | 2 Lágrimas de Ghast |
| 4 cristais de prismarinho → 16 fragmentos de prismarinho, 16 algas | 2 Conchas de Náutilo |
| 2 diamantes → 32 netherrack, 1 bloco de ouro, 4 obsidianas | Molde de Netherite |
| 16 garrafas → 16 lápis, 16 redstone | 16 Garrafas de Experiência |
| 2 cargas de fogo → 16 gravetos, 8 pó de pedra luminosa, 4 blocos de magma | 4 Varas de Blaze |
| 8 lápis → 8 bolas de slime, 2 obsidianas | 4 Pérolas do Fim |
| 4 pérolas → 16 púrpura, 16 frutas do coro | 2 Cascas de Shulker |

**Infundir:** o equipamento na mão sobe 1 nível de encantamento, passando do limite normal. Fio Afiado (Afiação até VII), Lâmina Flamejante (Aspecto Flamejante IV), Saque Arcano (Saque V), Pele de Pedra (Proteção VI), Pés de Pena (Peso-Pena VI), Mão Ligeira (Eficiência VII), Fortuna Arcana (Fortuna V), Arco Certeiro (Força VII) e Eterna (Inquebrável +1 até V e Remendo). Cada item aceita até **3 infusões**, que ficam escritas na descrição; encantamentos que não combinam (Fortuna com Toque Suave) são recusados.

O Grimório tem uma aba por tipo de ritual (Transmutar, Infundir, Raros e únicos, Invocações maiores, Guardiões e ajudantes).

### Raros, relíquias e chefes Despertos (2.84)

**Raros e únicos** (na mão → pedestais):

| Na mão | Pedestais | Resultado |
|---|---|---|
| 1 bloco de ouro | 2 blocos de esmeralda, 4 maçãs douradas | Totem da Imortalidade |
| 1 maçã dourada | 8 blocos de ouro, 16 garrafas de experiência | Maçã Dourada Encantada |
| 8 cristais de prismarinho | 4 conchas de náutilo, 2 diamantes | Coração do Mar |
| 1 crânio de esqueleto wither | 16 areias das almas, 1 Alma Desperta, 2 blocos de ouro | Estrela do Nether |
| 8 membranas de phantom | 2 cascas de shulker, 32 penas, 1 Alma Desperta, 4 diamantes | Élitro |
| 1 bloco de diamante | 2 Almas Despertas, 8 Fragmentos de Forja, 16 fragmentos de ametista | Essência Primordial |

As **Relíquias da Convergência** são feitas uma vez por jogador. A peça de netherite (ou o arco) vai na mão, e os pedestais levam 2 Almas Despertas, 1 Essência Primordial e mais dois ingredientes. A peça ganha nome, não queima e recebe encantamentos acima do normal:
- **Lâmina:** Afiação VII, Saque IV, Aspecto Flamejante III, Varredura III, Inquebrável V, Remendo. Mais 1 estrela do Nether e 2 blocos de diamante.
- **Égide:** Proteção VI, Espinhos III, Inquebrável V, Remendo. Mais 1 totem e 2 blocos de diamante.
- **Picareta:** Eficiência VII, Fortuna V, Inquebrável V, Remendo. Mais 4 blocos de esmeralda e 8 blocos de redstone.
- **Arco:** Força VII, Impacto II, Chama, Infinidade, Inquebrável V. Mais 32 flechas espectrais e 4 varas de breeze.

**Invocações maiores:** o **núcleo** do chefe vai na mão, e os pedestais levam a oferenda reforçada. O chefe volta **Desperto**: 2,5× a vida (até 2500), +50% de dano, +6 de armadura, 15% maior e brilhando, com aviso para o servidor. Saque: outro núcleo, 1 a 3 **Almas Despertas** (uma por participante), Essência Primordial e Pedra de Proteção garantidas, o dobro de Fragmentos de Forja, mais uma chance de arma nomeada e o dobro de XP. O altar não desperta outro chefe enquanto um está lutando nele.

| Chefe Desperto | Pedestais |
|---|---|
| Golem Ancestral | 8 blocos de ferro, 1 abóbora entalhada, 2 diamantes |
| Rainha Aracnídea | 16 olhos de aranha, 32 linhas, 4 olhos de aranha fermentados |
| Senhor das Chamas | 12 varas de blaze, 8 cremes de magma, 2 lágrimas de ghast |
| Lich | 1 crânio de esqueleto, 16 blocos de osso, 8 terras das almas |
| Tempestade Viva | 6 varas de breeze, 16 cargas de vento, 16 penas |
| Arauto do Fim | 1 estrela do Nether, 8 fragmentos de eco, 2 bafos de dragão, 1 cristal do End |

### Guardiões e ajudantes (2.85)

O ritual dá um **Selo**: com ele na mão, clique num bloco e o guardião fica ali, de posto (precisa de 2 blocos livres e permissão de construir). Cada jogador pode ter até **8** (6 na 2.85). Agachado, clique no seu guardião com a mão vazia para guardá-lo de volta no Selo, com as asas. Com a mão vazia (sem agachar), clique para ver a vida e o dono. Jogadores e outros guardiões não ferem os guardiões. Eles não atacam companheiros, não pisoteiam plantação e se curam 1 ponto de vida a cada 2 s.

| Guardião | Na mão → pedestais | O que faz |
|---|---|---|
| Espírito Guardião (allay) | olho do ender → 4 blocos de ametista, 4 varas de blaze, 2 blocos de redstone, 2 blocos de ferro | Fica parado e atira um raio (6 de dano a cada 1,5 s) no inimigo mais perto a até 12 blocos |
| Golem Guardião (golem de ferro) | abóbora entalhada → 8 blocos de ferro, 2 diamantes, 1 maçã dourada | 200 de vida; ataca inimigos a até 12 blocos do posto e volta para ele |
| Golem de Fazenda (golem de ferro pequeno) | abóbora entalhada → 4 blocos de ferro, 8 fardos de feno, 32 farinhas de osso, 1 composteira | Colhe trigo, cenoura, batata, beterraba e fungo do Nether maduros a até 8 blocos, replanta com uma semente da colheita e guarda o resto no baú ou barril mais perto (sem baú, deixa no chão) |
| Golem de Cobre (o do jogo) | abóbora entalhada → 4 blocos de cobre, 4 favos de mel, 2 baús | Leva os itens do baú de cobre para os outros baús; já vem encerado e nunca vira estátua |
| Asas de Guardião | 4 membranas de phantom → 32 penas, 2 varas de breeze, 8 fragmentos de ametista | Clique no Espírito ou num golem de ferro: asas de partículas nas costas e voo. O Espírito vai até o inimigo (18 blocos, raio de 7). O Golem Guardião voa atrás até dos inimigos no alto e pousa no posto. O de Fazenda voa por cima das plantações |

Os dados ficam no próprio bicho (tipo, dono, posto, asas, nível, modo e mochila) e em `guardioes.yml` (quantos cada jogador tem). Admin: `/rpgadmin selo <tipo|asas|cetro> [nível 1-5]`.

### Altar nível II, níveis dos guardiões e rituais do mundo (2.86)

**Novos guardiões:**

| Guardião | Na mão → pedestais | O que faz |
|---|---|---|
| Golem Lenhador | abóbora entalhada → 4 blocos de ferro, 1 machado de ferro, 8 mudas de carvalho, 1 baú | Acha uma árvore **natural** perto do posto (troncos sobre terra, com folhas que não foram postas por jogador) e corta um tronco por vez, de cima para baixo. Replanta a muda certa e guarda a madeira. Não mexe em casas de troncos |
| Golem Minerador | abóbora entalhada → 4 blocos de ferro, 1 picareta de ferro, 32 tochas, 1 baú | Fica no posto, numa mina, e tira um minério **à vista** (encostado no ar) a cada 3 s, a até 4 blocos. Deixa pedra, ardósia ou netherrack no lugar, então não segue o veio para dentro da parede. Detrito ancestral a partir do nível IV |
| Pastor de Neve (boneco de neve) | abóbora entalhada → 4 blocos de neve, 1 tesoura, 16 trigos, 2 laços | Tosquia as ovelhas, junta ovos, lã, penas, couro e escamas do chão e traz de volta os animais que fugiram para além do raio. Não derrete e não deixa neve |
| Espírito Curandeiro (allay) | fatia de melancia reluzente → 2 blocos de ametista, 2 maçãs douradas, 1 lágrima de ghast, 4 garrafas de mel | A cada 2 s cura jogadores e guardiões a até 6 blocos (1 + nível de vida). A partir do nível III, tira veneno e murchidão |
| Torre Arcana (blaze) | vara de blaze → 4 blocos de ametista, 8 blocos de magma, 16 cargas de fogo, 8 obsidianas; sacrifício: 1 blaze | Fica parada e, a cada 3 s, explode fogo arcano em volta do inimigo mais perto a até 14 blocos (5 de dano em área de 3 blocos, sem quebrar nada) |
| Cetro de Comando | vara de blaze → 8 fragmentos de ametista, 4 lingotes de ouro, 4 linhas | Clique no seu guardião: ele passa a seguir você (os espíritos voam, os golens andam ou voam com asas, e todos se teleportam se ficarem a mais de 40 blocos). Clique de novo: fica de posto onde está |

Agora cabem **8 guardiões** por jogador.

**Níveis I a V:** guarde o guardião no Selo (agachado, mão vazia). Depois segure o Selo no altar com:
- o pedido do nível nos pedestais;
- o **tributo** do tipo dele: 8 × o nível novo de ametista, ferro, trigo, cobre, carvão vegetal, carvão, lã branca, melancia reluzente ou pó de blaze;
- o **sacrifício**.

| Nível | Pedestais (mais o tributo) | Sacrifício | Altar |
|---|---|---|---|
| II | 2 blocos de ferro, 16 lápis | 2 galinhas | I |
| III | 2 blocos de ouro, 2 diamantes | 1 ovelha | I |
| IV | 1 bloco de diamante, 1 Alma Desperta, 8 obsidianas | 2 vacas | II |
| V | 1 estrela do Nether, 2 Almas Despertas, 1 Essência Primordial | 1 zumbi | II |

Cada nível dá +35% de vida, +25% de dano e +2 blocos de alcance (Minerador: +1). O Espírito atira mais rápido e a Torre explode mais rápido. O Golem de Cobre anda mais rápido. No **nível III** os trabalhadores (Fazenda, Lenhador, Minerador e Pastor) ganham uma **mochila** de 27 espaços, usada quando não há baú ou ele está cheio. Clique no guardião com a mão vazia para abrir; a mochila vai junto para o Selo e cai no chão se ele morrer. No **nível V** o guardião brilha e se cura 2 pontos a cada 2 s.

**Sacrifício:** o mob precisa estar vivo a até 5 blocos do altar, sem nome e sem dono. Não pode ser companheiro nem guardião. Laço ou armadilha ajudam. Na cerimônia ele para, sobe, gira junto com os itens, encolhe e se desfaz em almas na explosão. Se o altar for quebrado no meio, ele é solto.

**Ritual de grupo:** a Bênção da Aliança precisa de 3 jogadores; o Arauto Desperto e a Lua Azul precisam de 2. Os outros ficam a até 6 blocos do altar, cada um com o catalisador na mão, que também é gasto.

**Altar nível II:** o ritual **Despertar o Altar** sobe o altar de nível. Ele usa estrela do Nether na mão, 8 obsidianas choronas, 2 Almas Despertas, 8 blocos de lápis e 2 cristais do End, mais o sacrifício de 2 vacas. O altar ganha mais 8 pedestais a 5 blocos: (±5, 0), (0, ±5), (4, 3), (−4, −3), (3, −4) e (−3, 4). O item girando vira uma estrela do Nether, e o nível fica guardado no chunk.
- Só funcionam no nível II: as Relíquias, a Reforja, o Arauto Desperto, a Lua Azul e os níveis IV e V dos guardiões.
- A Estrela do Nether passou a pedir o sacrifício de 1 esqueleto wither. O Élitro pede 1 phantom e o Lich Desperto, 2 esqueletos.

**Céu, território e altar** (nova aba do Grimório):

| Ritual | Na mão → pedestais | Efeito |
|---|---|---|
| Chamar a Chuva | balde de água → 16 algas, 8 lápis | Tempestade por 10 min |
| Céu Limpo | girassol → 4 pedras luminosas, 8 penas | Sem chuva por 20 min |
| Chamar o Dia / a Noite | relógio → 32 tochas e 4 ouros / 8 bolsas de tinta e 2 blocos de carvão | Vira 1000 / 13000 |
| Lua Azul (2 jogadores, altar II, só à noite) | bloco de lápis → 8 cristais de prismarinho, 8 bolsas de tinta brilhante, 1 Alma Desperta | Até o amanhecer: monstros mortos por jogadores têm 6% de soltar um material raro e 0,5% de soltar uma Alma Desperta; barra azul no topo |
| Aura do Crescimento | 16 farinhas de osso → 8 fardos de feno, 4 esmeraldas, 8 blocos de musgo | 2 h: cada vez que uma planta do território cresce, cresce um estágio a mais |
| Aura da Paz | estandarte branco → 2 maçãs douradas, 8 esmeraldas, 32 tochas | 2 h: monstros não nascem sozinhos no território |
| Aura do Vigor | 16 açúcares → 4 blocos de redstone, 4 esmeraldas, 1 pé de coelho | 2 h: quem está no território ganha Pressa e Velocidade |
| Bênção da Aliança (3 jogadores) | maçã dourada → 4 blocos de esmeralda, 1 totem, 8 garrafas de experiência | Força, Resistência e Regeneração por 30 min para quem está a até 12 blocos |

As auras só funcionam com o altar dentro de um território e para quem pode construir nele. Elas ficam em `auras.yml` e valem mesmo com o servidor reiniciando.

**Reforjar Relíquia** (altar II): a relíquia vai na mão. Os pedestais levam um livro encantado, 1 Alma Desperta e 4 blocos de lápis. O encantamento do livro entra na relíquia um nível acima do livro, até 2 acima do máximo normal, e tira o que não combina com ele (por exemplo, Fortuna no lugar de Toque Suave).

**Ritual que sai errado:** se quem acendeu se afastar mais de 24 blocos, cair ou sair do jogo, ou se um pedestal usado for quebrado, o ritual falha.
- Os ingredientes e o sacrifício se perdem.
- O céu fica escuro de tinta e sai do altar uma **Aberração do Ritual**: um evocador de 60 de vida com 3 vexes.
- Equipamento, relíquia e Selo voltam ao chão; o resto não.
- Se o próprio altar for quebrado no meio, tudo volta, como antes.

### Ferramentas, insumos e túneis (2.87)

Cada guardião que trabalha precisa da **ferramenta** dele: clique nele com ela, e a antiga volta para você. A ferramenta define a **velocidade**: madeira 0,5, pedra 0,7, cobre 0,85, ferro 1, ouro 1,2, diamante 1,4 e netherite 1,8, com +15% por nível de Eficiência e +10% por nível do guardião. Ela **gasta durabilidade** (o Inquebrável vale) e, quando quebra, o guardião para e avisa o dono. A ferramenta vai junto para o Selo e cai no chão se ele morrer.

O nome do guardião mostra o que falta, em vermelho: "⚠ sem picareta", "⚠ faltam 4 cercas e 3 tábuas no baú", "⚠ sem sementes no baú", "⚠ sem munição (fraco)". O clique com a mão vazia mostra um painel com vida, ferramenta e durabilidade, munição e o progresso do túnel.

Os **insumos** saem do baú (ou barril) mais perto do posto e, do nível III em diante, também da mochila.

| Guardião | Ferramenta | Insumos | O que passou a fazer |
|---|---|---|---|
| Golem Minerador | picareta | 4 cercas e 3 tábuas por sustentação, tochas | **Túnel de mineração** a partir do posto, na direção em que você olhava ao pôr o Selo (ou ao dar a ordem de posto com o Cetro). Detalhes abaixo |
| Golem Lenhador | machado | mudas | Corta as árvores naturais e, sem árvore por perto, **planta floresta**: uma muda do baú a cada 4 blocos, longe de outras árvores. Junta do chão mudas, gravetos, maçãs e troncos |
| Golem de Fazenda | enxada (melhor = mais longe) | sementes, farinha de osso | Colhe e replanta. **Planta** sementes do baú na terra arada vazia e **ara** a terra ao lado da plantação, só onde há água a até 4 blocos, então a plantação cresce a partir da sua. Também **aduba** as plantas com farinha de osso |
| Pastor de Neve | tesoura | trigo, cenoura, sementes, baldes | Tosquia e junta do chão. A cada 30 s faz **2 animais procriarem** com a comida do baú (trigo para vaca, ovelha e cabra; cenoura para porco e coelho; sementes para galinha), até 8 + 2 × nível por tipo. A cada minuto **ordenha** uma vaca com um balde vazio do baú |
| Golem Guardião | espada (opcional) | lingotes de ferro | A espada dá dano a mais: madeira e ouro +1, pedra e cobre +2, ferro +3, diamante +4, netherite +5, e +0,5 por nível de Afiação. Abaixo de 70% de vida, ele **se conserta** com um lingote do baú (+25), a cada 5 s |
| Espírito Guardião | — | fragmento de ametista (4 raios) ou de eco (8 raios, 1,6× de dano) | Sem munição: metade do dano e atira na metade da velocidade |
| Torre Arcana | — | pó de blaze (normal) ou carga de fogo (1,5× de dano, área 4,5) | Sem munição: metade do dano, área 2,5 e metade da velocidade |
| Espírito Curandeiro | — | garrafa de mel ou melancia reluzente (5 curas cada) | Sem munição: cura 1 e não tira veneno. Com mel: 1 + nível, e tira veneno a partir do nível III. Com melancia: 2 + 1,5 × nível, e sempre tira veneno |
| Golem de Cobre | — | — | Igual (o jogo controla quanto ele carrega) |

Munição: clique no guardião com ela (todo o monte entra), ou deixe num baú a até 8 blocos do posto. Quando acaba, ele pega até 16 do baú.

**O túnel do Minerador:**
- **Corredor:** 3 de largura e 3 de altura. A cada 16 blocos, as últimas 5 fatias formam uma **câmara 5×5**.
- **Sustentação:** a cada 4 blocos, duas colunas de cerca nas laterais e uma viga de 3 tábuas no teto. Sem cercas ou tábuas no baú, ele continua cavando e avisa.
- **Tocha:** na parede da direita, a cada 8 blocos (na 2ª, 10ª, 18ª...).
- **Ritmo:** um bloco por vez, no tempo da picareta. Com netherite e Eficiência V, uns 10 blocos de túnel por minuto.
- **Segurança:** antes de cavar um bloco, tampa a lava em volta com pedregulho, a água com pedra, e o cascalho ou areia do teto com pedra.
- **Minérios das paredes:** cada minério que fica à vista nas paredes, no teto e no chão é minerado e trocado por pedra (ardósia ou netherrack, conforme o lugar). Ele não segue o veio para dentro.
- **Para sozinho** em rocha indestrutível, num bloco com coisas dentro (baú, fornalha...), numa estação, em território de outra pessoa, ou em obsidiana e detrito ancestral sem picareta de diamante.
- **O que sai:** tudo vai para o baú perto do começo do túnel e depois para a mochila. Se não couber, pedregulho, terra, cascalho e outras pedras somem; o resto cai no chão.
- **Comprimento por nível:** I 24, II 40, III 56, IV 72, V 96 blocos. Para subir de nível, ele vai para o Selo. Ponha-o de volta no mesmo lugar, olhando para o mesmo lado: ele passa depressa pelo trecho já aberto e continua. Com o Cetro, "ficar de posto aqui" começa um túnel novo na direção em que você olha.

### Guardiões novos, fusão, altar nível III e rituais (2.89)

**Cinco guardiões novos.** Sobem do I ao V como os outros, com o tributo deles.

| Guardião | Na mão → pedestais | Precisa | O que faz |
|---|---|---|---|
| Golem Pescador | abóbora entalhada → 4 blocos de ferro, 1 vara de pesca, 16 bacalhaus, 1 baú | vara de pesca; isca no baú (carne podre, olho de aranha ou sementes), opcional | Pesca do posto na água aberta mais perto, a até 8 blocos (+2 por nível): um peixe a cada 20 s no nível I. O encantamento Isca tira 3 s por nível, e a isca do baú, mais 25%. Sai peixe (bacalhau, salmão, baiacu, tropical), às vezes lixo e às vezes tesouro (etiqueta, sela, náutilo, arco, vara ou livro encantados). Sorte do Mar e a isca dão mais tesouro. Gasta a vara e solta um pouco de XP |
| Abelha Apicultora (abelha) | favo de mel → 1 colmeia, 2 blocos de mel, 8 dentes-de-leão, 1 fogueira | tesoura; garrafas no baú, opcional | Voa até as colmeias e ninhos cheios a até 12 blocos e os acalma com fumaça, então as abelhas não saem bravas. Com garrafa vazia no baú, tira uma garrafa de mel; sem garrafa, 3 favos (e aí gasta a tesoura). No nível III, **poliniza**: a cada minuto, uma colmeia com flores a até 4 blocos ganha mel |
| Golem Ferreiro | abóbora entalhada → 6 blocos de ferro, 1 bigorna, 1 alto-forno, 1 rebolo | bigorna a até 3 blocos do posto; baú perto | Conserta o que estiver gasto no baú, 25% da durabilidade por material, a cada 3 s. Usa o material de cada item: tábuas, pedregulho, couro, lingote de ferro, ouro ou cobre, diamante, fragmento de netherite, linha (arco, besta, vara), membrana (élitro), prismarinho (tridente), vara de breeze (maça). A bigorna gasta como no jogo (12%, menos 2% por nível) até quebrar. No **nível III**, também conserta as ferramentas dos seus outros guardiões a até 16 blocos. As peças que só a bigorna conserta com o próprio mineral (conjuntos minerais) ficam de fora |
| Espírito Coletor (allay) | bloco musical → 2 blocos de ametista, 4 funis, 2 baús, 4 pérolas do Fim | um baú perto | Voa até os itens caídos a até 12 blocos e guarda cada um no baú que já tem aquele item. Se nenhum tem, guarda no mais perto, e depois na mochila. Não pega item com dono (o resultado do altar), item jogado por alguém há menos de 1 minuto, nem o que está em cima de estações. No nível III, a cada 30 s **organiza**: tira até 8 montes do baú mais perto (a "entrada") e leva para o baú que já guarda aquele item |
| Vigia (papagaio) | luneta → 1 sino, 4 lâmpadas de redstone, 2 blocos de ferro, 16 penas | — | Olha a até 16 blocos do posto (+2 por nível). Quando chega um monstro ou um **estranho**, que é quem não pode construir ali, toca os sinos de verdade e acende as lâmpadas de redstone e de cobre até 20 s depois que o último sai. Também marca os intrusos (eles brilham através das paredes) e avisa você por mensagem. No nível III, manda seus Golens Guardiões para cima dos monstros; no nível IV, os monstros ficam lentos |

**Fundir Guardiões** (aba Guardiões, altar nível II): na mão vai o Selo do guardião que fica com o corpo e o trabalho. No pedestal vai o Selo de um guardião de **apoio**: Espírito, Torre, Curandeiro, Vigia ou Ferreiro. Os outros pedestais levam 1 Alma Desperta, 4 blocos de ametista e 1 bloco de diamante.
- O fundido faz as duas coisas: um Golem de Fazenda + Curandeiro colhe e cura quem está perto, e um Lenhador + Torre corta árvores e explode monstros. A função de apoio acontece de onde ele estiver, sem sair do trabalho.
- Ele fica com o **maior nível** dos dois e **25% mais vida**. O nome mostra os dois ("Golem de Fazenda + Espírito Curandeiro").
- Não se fundem: dois do mesmo tipo, o Golem de Cobre, um Selo que já é fundido, dois que usam munição (Espírito, Torre e Curandeiro) e um Selo de pedestal com itens na mochila. Se o de apoio estiver na mão e o trabalhador no pedestal, o altar avisa para trocar.
- A munição do Selo de apoio passa para o fundido.

**Transferir Encantamentos** (aba Infundir): na mão vai o item que vai **receber**. Num pedestal vai o item encantado (a origem); nos outros, 1 livro (a ponte), 2 blocos de lápis e 8 garrafas de experiência.
- Passam os encantamentos que o item da mão aceita, que não brigam com os dele e que sobem de nível. Maldições ficam.
- A origem volta junto, sem os encantamentos que passaram.
- Relíquias da Convergência não entram, nem como origem nem como destino.
- Se o ritual sair errado, a origem volta, mas os encantamentos se perdem.

**Trazer de Volta** (aba Invocações): quando um companheiro morre, cai a **Coleira de \<nome\>** junto com os componentes. Ela guarda a lembrança dele, com tipo, nome, variante, nível e evolução. As 10 lembranças mais recentes de cada dono ficam em `lembrancas.yml`.
- Com a coleira na mão (ou uma etiqueta renomeada com o nome exato do companheiro), os pedestais levam 2 maçãs douradas, 2 lágrimas de ghast, 4 blocos de osso e 8 melancias reluzentes.
- Ele volta do altar com vida cheia, **sem** o que caiu quando morreu (sela, armadura, baú, componentes, alforje e ferradura).
- Só o dono traz de volta, e precisa ter vaga de companheiro.
- As montarias lendárias não deixam coleira (têm o próprio jeito de voltar).
- A coleira serve uma vez. Se o ritual falhar, ela volta.

**Altar nível III:** o ritual **Ascender o Altar** precisa do altar nível II.
- **Na mão:** 2 estrelas do Nether.
- **Nos pedestais:** 4 Almas Despertas, 1 Essência Primordial, 1 sinalizador, 16 obsidianas choronas, 4 cristais do End e 8 fragmentos de eco.
- **Sacrifício:** 2 endermen.
- **Resultado:** mais 8 pedestais a 7 blocos, em (±7, 0), (0, ±7) e (±5, ±5), somando 24. O item girando vira um sinalizador. `/rpgadmin convergencia <1-3>` olhando para um altar muda o nível dele.

As lendárias do nível III:

| Receita | Na mão | Pedestais | Resultado |
|---|---|---|---|
| Ovo do Dragão | cabeça de dragão | 4 cristais do End, 8 bafos de dragão, 2 Almas Despertas, 1 Essência Primordial, 32 obsidianas; sacrifício: 2 endermen | Um segundo ovo do dragão |
| Tridente da Convergência | tridente | 3 Almas, 2 Essências, 1 estrela do Nether, 1 coração do mar, 32 cristais de prismarinho | Penetração VII, Lealdade IV, Condutividade, Inquebrável V, Remendo |
| Maça da Convergência | maça | 3 Almas, 2 Essências, 1 estrela do Nether, 1 núcleo pesado, 8 varas de breeze | Densidade VII, Explosão de Vento III, Inquebrável V, Remendo |
| Botas da Convergência | botas de netherite | 3 Almas, 2 Essências, 1 estrela do Nether, 16 membranas de phantom, 4 pés de coelho | Proteção VI, Peso-Pena VI, Passos Profundos III, Velocidade das Almas IV, Inquebrável V, Remendo |

As três relíquias são uma por jogador, como as do nível II.

**Rituais de mundo novos** (aba Céu, território e altar):

| Ritual | Na mão → pedestais | Efeito |
|---|---|---|
| Chuva de Meteoros (altar II) | luneta → 16 blocos de magma, 2 blocos de ferro, 1 bloco de ouro, 8 cargas de fogo | 12 meteoros, um a cada 5 s, de 12 a 40 blocos do altar. Os meteoritos (blocos de magma) podem cair no território de quem chamou, e quebrá-los dá o mesmo saque da chuva do céu. Só no mundo normal |
| Abrir um Portal | olho do ender → 8 obsidianas, 4 blocos de ametista, 4 pérolas do Fim, 2 blocos de lápis | Um portal do seu rank de Caçador abre num chão livre a 6–9 blocos do altar (5% de sair Vermelho). Você tem 30 s para entrar antes dos outros. Um a cada 6 horas por jogador |
| Chamar o Mercador | 16 esmeraldas → 2 laços, 4 fardos de feno, 1 bloco de esmeralda | O Mercador Itinerante arma a tenda perto de você na hora. Se ele já estiver no mundo, o altar recusa; se não achar lugar, as esmeraldas voltam |

## Círculos mágicos (2.91–2.92)

Toda magia desenha um círculo de partículas no mesmo traço dos rituais: anel de fora na cor do primeiro elemento, anel de dentro branco girando ao contrário, 12 marcas de runa entre os anéis e, no meio, a figura do elemento (do segundo, se a magia tiver dois): Fogo triângulo, Gelo hexagrama, Vento estrela de 8 pontas, Água pentágono, Vida hexágono, Terra quadrado, Sombra pentagrama, Vazio heptagrama, Energia decagrama e Natureza estrela de 7 pontas. É só visual (não muda dano, alcance nem colisão) e não precisa do pacote de texturas.

| Forma | Onde | Tamanho | Quanto tempo |
|---|---|---|---|
| Projétil, Toque, Sopro, Proibida | De pé, 1,3 bloco à frente dos olhos, virado para onde você olha | 1,1 (Sopro 1,4) | 0,7 s |
| Receitas secretas | Na frente e outro no chão em volta de você | 1,6 e 2,2 | 1 s |
| Raio | Na frente, seguindo o olhar | 1,0 | Enquanto o feixe dura |
| Canalizar | Na frente, crescendo com a carga | até 1,6 | Enquanto você segura |
| Aura / Corpo / Encantar arma | No chão, seguindo você | 2,6 / 1,5 / 1,1 | 1,7 s / 1,2 s / 0,9 s |
| Criação / Invocação | No chão do bloco olhado | 1,4 / 1,8 | 1,2 s / 2 s |
| Chuva | No céu, 8 blocos acima da área | raio da chuva + 1 | Enquanto a chuva cai |

Cada jogador tem no máximo 6 círculos ao mesmo tempo (96 no servidor). Desliga com `embelezamento.circulos-magicos: false`. A runa do pacote de texturas da 2.81 continua junto.

**2.92: círculos no resto do jogo** (desliga com `embelezamento.circulos-sistemas: false`):

| Onde | Como é |
|---|---|
| Golpe marcado do chefe (masmorras) | No lugar do anel simples, um pentagrama vermelho com um anel de carga que cresce do centro e chega na borda na hora do golpe (1,75 s) |
| Reforços do chefe e ondas da Fenda | Cada criatura sai de um pentagrama pequeno no chão |
| Invocar Mortos (chefes do Altar) | Pentagrama de 3 blocos em volta do chefe |
| Habilidade de classe | Círculo de 1,7 bloco na cor da classe: quadrado nas básicas, hexagrama nas avançadas e ocultas, estrela de 8 nas lendárias |
| Levante-se (Soberano) | Círculo roxo escuro com feixe curto embaixo de cada corpo |
| Portal abrindo | Círculo gigante no céu, 22 blocos acima, na cor do rank (E triângulo, D quadrado, C pentágono, B hexagrama, A heptagrama, S estrela de 8; Vermelho pentagrama escuro), visto de longe por 8 s, e outro no chão com feixe |
| Pedras de Viagem | Círculo de partida enquanto a viagem carrega e outro na chegada |
| Nível de atributo | Círculo pequeno na cor do atributo; a cada 10 níveis, maior e com feixe |
| Renascer | Círculo dourado de 3,2 blocos com feixe de 30 e outro no céu |
| Rank de Caçador e Despertar | Círculo na cor do rank (ou da característica) com feixe; o Despertar também no céu |
| Forja | Peça Lendária: hexagrama; Mítica: estrela de 8 com feixe, em volta do ferreiro |
| Prece e milagre | Círculo na cor do deus com a figura dele (Ferrum quadrado, Sylva estrela de 7, Maris pentágono, Bellum triângulo, Arcanus hexagrama, Mortis pentagrama); o milagre é maior, com feixe e outro no céu |
| Constelação vista pela luneta | As estrelas acendem uma a uma e se ligam no céu, na direção em que você olha (só você vê) |
| Lua Azul, Eclipse, Lua de Sangue | Um círculo enorme no céu sobre cada jogador do mundo quando o evento começa |
| Marco do território criado | Círculo de 6 blocos com feixe |
| Captura do Marco na guerra | Círculo do tamanho da área de captura, na cor do reino atacante, que enche com a captura |
| Guardião posto | Círculo onde ele aparece (estrela de 8 se for fundido) |

## Círculo Arcano (2.90)

Um pentagrama desenhado no chão em volta de uma **obsidiana** (o centro):
- **Velas acesas nas 5 pontas**, a 5 blocos do centro: (0, −5), (5, −2), (3, 4), (−3, 4) e (−5, −2), com o −5 para o norte. A vela pode estar no piso ou em cima de um bloco.
- **O traço da estrela**, ligando cada ponta à segunda seguinte (33 casas). Vale pó de redstone em cima do piso ou blocos no próprio piso: obsidiana (chorona também), pedra-negra (polida, tijolos, dourada), tijolos do Nether (e vermelhos), bloco de redstone, ametista, areia e terra das almas, pedra luminosa, luz-cogumelo, nylium carmesim, magma, concreto ou lã preta, vermelha ou roxa. Bastam 85% do traço.

Jogue **1 olho do ender e 4 pós de pedra luminosa** na obsidiana: o círculo desperta. Se o desenho estiver incompleto, a mensagem diz quantas velas e casas faltam. `/rpgadmin pentagrama` desenha um pronto em volta do bloco olhado.

**Ritual:** jogue as oferendas dentro do círculo (a até 5 blocos do centro) e clique na obsidiana com o catalisador.
- O traço acende ponta por ponta, com chamas azuis nas velas.
- Um **círculo gigante de luz** se desenha no céu, 16 blocos acima: anel duplo, a estrela e um anelzinho em cada ponta, crescendo e girando cada vez mais rápido. Um feixe liga o céu ao chão, com trovões.
- Depois de 6 s, um clarão, e o ritual acontece.
- Se quem acendeu se afastar mais de 16 blocos, ou uma vela apagar, ou o traço quebrar, o ritual **se volta contra você**: as oferendas se perdem, saem 4 vexes e você fica com Murchidão por 8 s.
- Mão vazia na obsidiana abre o grimório; agachado, confere o desenho.
- Parado, o círculo desenha um anel fraco no céu quando alguém está perto.

| Ritual | Na mão | No círculo | Efeito |
|---|---|---|---|
| Pacto da Força | 2 varas de blaze | 16 carnes podres, 8 ossos, 8 redstone | 1 h: +25% de dano corpo a corpo; preço: −2 corações |
| Pacto da Sombra | 1 fragmento de eco | 4 membranas de phantom, 8 bolsas de tinta, 4 areias das almas | 1 h: no escuro (noite ou luz ≤ 8), Invisibilidade e Velocidade; preço: sob o sol, Fraqueza |
| Pacto da Fortuna | 8 esmeraldas | 8 lingotes de ouro, 1 pé de coelho, 16 lápis | 1 h: Sorte II, Pressa e Herói da Vila II; preço: Fome o tempo todo |
| Tempestade Arcana | 1 para-raios | 8 lingotes de cobre, 2 varas de breeze, 8 pós de pedra luminosa | Tempestade de 5 min; por 2 min, raios (12 de dano e fogo) nos monstros a até 48 blocos |
| Fenda do Vazio | 4 pérolas do Fim | 8 obsidianas, 8 frutas do coro, 2 fragmentos de eco | Uma fenda sobre o círculo solta 3 ondas (endermites e vexes; endermen e vexes; phantoms e endermen) e o **Guardião da Fenda** (enderman 1,6× maior, 160 de vida). Selada: pérolas, eco, cascas de shulker, Fragmentos de Forja, 25% de Alma Desperta e 10% de élitro. Fecha sozinha, sem tesouro, em 4 min. Uma por vez no servidor |
| Conjurar Corcel Espectral | sela | 4 blocos de osso, 8 areias das almas, 4 cenouras douradas | Um cavalo esqueleto selado e domado, que vira seu companheiro se houver vaga |
| Conjurar Espíritos Aliados | lágrima de ghast | 8 penas, 8 pós de pedra luminosa, 4 areias das almas | 3 vexes com espada lutam ao seu lado por 3 min, só contra monstros |
| Chamar o Eclipse | relógio | 4 blocos de carvão, 16 bolsas de tinta, 4 obsidianas | De dia, no mundo principal: o eclipse do céu (2,5 min) |
| Chamar a Lua de Sangue | 2 blocos de redstone | 16 carnes podres, 16 ossos, 4 olhos de aranha | À noite, no mundo principal: Lua de Sangue até o amanhecer |
| Banimento | lanterna das almas | 16 farinhas de osso, 8 pós de pedra luminosa, 1 vela branca | Os monstros a até 32 blocos se desfazem (chefes e monstros com nome ficam) e nenhum nasce sozinho a até 48 blocos por 10 min |
| Revelação Astral | luneta | 4 bolsas de tinta brilhante, 8 fragmentos de ametista, 1 olho do ender | Por 1 min, toda criatura a até 96 blocos brilha; Visão Noturna por 5 min; e o rumo e a distância da estrutura mais perto que você ainda não viu |

Os pactos ficam no jogador (um por vez) e valem mesmo se ele sair e voltar. Os modificadores da Força voltam a cada 2 s e saem quando o pacto acaba. Títulos contam `rituais_circulo` e `fendas_seladas`.

## Salas e encontros das masmorras (2.82)

Novas instâncias podem sortear **Forjas** (oficinas nas laterais) e **Observatórios** (anéis no piso e pilares luminosos), além dos nove estilos anteriores. Os caminhos das portas continuam sendo abertos depois da decoração.

Salas de combate além das primeiras podem ter três encontros: **Patrulha**, com a composição habitual; **Horda**, com até três criaturas extras por onda, 75% da vida e 85% do dano habitual; e **Sentinela**, com metade do grupo arredondada para cima e um líder luminoso com o dobro da vida e 15% mais dano. Todas respeitam o limite de 14 criaturas por onda. A alteração de dano vale para o atributo de ataque corpo a corpo. A primeira visita informa o nome temático, o andar e uma dica; cada jogador recebe o aviso uma vez por sala.

Uma tesouraria nativa com espaço livre pode conter a **Sequência Rúnica**: leia a ordem indicada e clique nas runas I, II e III. Errar reinicia a sequência. O progresso é compartilhado pela equipe; concluir produz uma única recompensa extra junto das runas. É opcional e não bloqueia a conclusão da masmorra. Salas personalizadas não recebem esse desafio nem os novos cenários automáticos.

As entradas das arenas nativas avisam que o chefe está adiante. O golpe marcado no chão tem aviso sonoro e 1,5 segundo para esquivar; é cancelado se o chefe morrer, a sala reiniciar ou a masmorra terminar. Seu dano fica restrito aos participantes presentes na arena.

Configuração: `masmorra.encontros-variados`, `masmorra.enigma-runas` e `masmorra.avisos-salas`. As mudanças usam blocos e textos nativos: o pacote de recursos **2.81.0** continua compatível com o plugin **2.82.0**. Gere uma masmorra nova para ver os cenários e encontros sorteados.

Testes automatizados verificam as seis ordens rúnicas, erro/reinício, conclusão única, limites dos encontros e nomes de todos os estilos/temas. Ainda falta validar no jogo: circulação pelos dois cenários, sequência com dois jogadores, prêmio único e cancelamento do golpe ao derrotar o chefe durante a preparação.

## Estúdio e decoração (2.81)

`/aparencia` (também `/estudio` e `/personalizar`) abre o estúdio: mochila, aljava ou capa nas costas, colar, oito cores para os enfeites, auras e rastros de passos. Há três combinações salvas: Exploração, Combate e Cidade. Clique direito para salvar; esquerdo para usar. Salvam enfeites e opções cosméticas, sem copiar equipamentos. Acessórios do corpo se escondem ao nadar, planar, dormir ou ficar invisível.

O guarda-roupa agora tem **27 enfeites em nove categorias**: às seis anteriores somam-se Cabelo, Bigode e Brincos. Entraram seis penteados, dois bigodes e dois pares de brincos. Barba, cabelo, chifres e brincos podem ser usados juntos.

`/decoracoes` (também `/decoracao` e `/moveis`) fabrica **15 ornamentos em quatro temas**, totalizando 60 variantes. As abas mostram Cripta, Caverna, Inferno e Gelo; cada receita informa o material gasto. Clique num bloco para colocar, clique na peça para girar e agache ao clicar para recolher. Só o dono ou um administrador pode manipular a peça, respeitando a proteção do território. Placas e estandartes renomeados na bigorna mostram o nome escolhido. São objetos decorativos: baús não armazenam e braseiros não causam dano. Limite de 48 peças por chunk.

Salas novas geradas pelo plugin recebem adereços temáticos; salas de modelos construídos pelo jogador permanecem como estavam. Baús raros têm um efeito na primeira abertura. Masmorras e ornamentos próximos recebem partículas de ambiente.

As magias ganham círculos de conjuração, orbes em movimento, impactos, pilar de fogo e um modelo giratório para o buraco negro. Invocações elementais recebem dez modelos, um por essência, via EMF/ETF. Armas de chefe, espadas de Caçador de rank S e elixires recebem texturas animadas. Os efeitos de dano e as receitas continuam sendo os das magias existentes. Os modelos temporários somem ao terminar, ao desconectar ou trocar de mundo; há limite global de 96 e de 12 por jogador.

As opções `embelezamento.magias`, `personagens`, `cenarios` e `limite-animacoes` ficam no config. Para usar os visuais, instale **RPGAtributos-2.81.0.jar** no servidor e ative **RPGAtributos-recursos-2.81.0.zip** no cliente, acima dos demais pacotes de criaturas. EMF/ETF são necessários para os modelos de criaturas. O pacote manual antigo precisa ser desativado.

Validação automatizada cobre geração dos modelos, referências de texturas, regras das invocações, quadros animados, limites de escala e gravação de combinações. **Ainda é necessária uma rodada dentro do Minecraft**, incluindo equipar/tirar capacete, salvar/carregar visuais, colocar/girar/recolher decoração, reiniciar o servidor e conjurar com vários jogadores. As prévias em `docs/` são renderizações dos modelos fora do jogo.

## Títulos

`/titulos`: 127 títulos com requisitos (em páginas), por exemplo Sangue de Dragão, Alfa da Lua, Andarilho do Véu, Grande Inventor, Mestre do Arsenal, Sombra das Lâminas, Ceifador, Criador de Linhagens, Pescador de Troféus, Duelista, Campeão das Vilas, Rival de Mesa, Abridor de Pacotes, Curador do Álbum, Geólogo, Coração de Cristal, Garimpeiro do Rio, Confidente, Domador de Lendas, Espanta-Nian, Irmãos de Armas, Algoz das Marés, Coração do Mar, Pescador do Gelo, Mestre Curtidor, Sonhador, Coletor da Estação, Lavrador das Estações, Andarilho das Ruínas, Desejoso, Pomicultor, Colecionador de Cartas, Guardião de Segredos, Mestre Cervejeiro, Apicultor, Bom Samaritano, Colecionador de Troféus, Caçador de Raridades, Eterno, Renascido, Mestre dos Talentos, Vínculo Eterno, Cronista, Cartógrafo, Arqueólogo, Mineiro das Profundezas, Devoto, Astrônomo, Mestre Rúnico, Mestre da Lâmina, Arsenal Vivo, Mestre dos Combos, Finalizador, Minerador, Nadador, Pescador, Alquimista, Lenda dos Mares, Caçador de Tesouros, Lapidário, Fazendeiro, Chef, Domador, Grão-Mestre, Ferreiro Lendário, Reciclador, Botânico, Mestre-Cuca, Senhor das Feras, Arquimago, Mata-Gigantes, Fim dos Tempos, Explorador de Masmorras, Senhor das Masmorras, Desperto, Mestre de Armas, Portador de Lenda, Desbravador, Herdeiro Lendário, Viajante, Herói do Povo, Companheiro, Fundador, Senhor das Terras...
- **Só o título em uso dá bônus** (+vida, +dano, +mana, +XP...).
- O título aparece acima da cabeça.
- Os mais difíceis são anunciados para o servidor todo.

## Missões dos aldeões

**Agache e clique com o botão direito num aldeão com profissão:** ele mostra 3 pedidos do dia, conforme a profissão. Podem ser entregar itens, caçar monstros ou entregar um equipamento forjado de certa raridade (armeiros e ferreiros).
- Cumprir dá esmeraldas, XP, às vezes Fragmento ou Pedra de Proteção, e **reputação**: o aldeão e os vizinhos passam a dar **desconto** no comércio.
- Até 3 missões ao mesmo tempo. `/missoes` mostra as suas.

## Comandos

| Comando | Quem pode | O que faz |
|---|---|---|
| `/atributos [jogador]` | todos | Menu de atributos |
| `/forja` | todos | Guia da forja: raridades e chances no seu nível |
| `/grimorio` | todos | Grimório (magias, criador, magias secretas) |
| `/titulos` | todos | Títulos |
| `/missoes` | todos | Suas missões de aldeões |
| `/receitas` | todos | Livro de receitas da Cozinha |
| `/party` | todos | Menu da party (`convidar`, `aceitar`, `recusar`, `sair`, `expulsar`, `lider`, `desfazer`, `chat`, `fogoamigo`) |
| `/pc <mensagem>` | todos | Chat da party (sem mensagem: liga/desliga o modo chat) |
| `/pets` | todos | Seus companheiros (domador) |
| `/constelacoes` | todos | Moedas Estelares e bênçãos dos deuses que assistem aos seus feitos |
| `/masmorra [sair\|rank\|arvore\|missao\|monarcas\|stats\|recordes\|loja]` | todos | Informações da masmorra / sair dela / Rank de Caçador / Árvore e Despertar / Missão Diária / quem é cada Monarca / estatísticas / recordes / Loja do Sistema |
| `/masmorra monarca abdicar` | todos | Deixa a classe do seu Monarca livre para outro |
| `/classe` | todos | Árvore de classes, caminho, tarefas e habilidades |
| `/lendas` | todos | Livro das Lendas: enigmas, donos e Forja Lendária |
| `/locais` | todos | Os Locais Ocultos que você já achou |
| `/peixes` | todos | Diário do Pescador (peixes raros, onde e quando aparecem) |
| `/alquimia` | todos | Livro de Alquimia (elixires, componentes, gemas e acessórios) |
| `/acessorios` | todos | Seus espaços de acessório |
| `/guardaroupa` | todos | Guarda-roupa (aparência da armadura) |
| `/colonia` | todos | Sua colônia: moradores, profissões, depósito e melhorias |
| `/reino` | todos | Seu reino: menu, convites, cargos, tesouro, guerra e paz |
| `/bestiario` | todos | Monstros derrotados, marcos e a idade do mundo |
| `/calendario` | todos | Estação, dia, fase da lua, clima e eventos do céu |
| `/maldicao [transformar]` | todos | Sua maldição; o lobisomem se transforma à noite |
| `/raca` | todos | Escolher a raça, ver os estágios e despertar o próximo |
| `/raca definir <jogador> <raça\|nenhuma> [estágio]` | admin | Muda a raça de alguém (testes) |
| `/portais` | todos | Portais abertos: onde estão e quanto falta para transbordarem |
| `/sombras` | Soberano | O exército das sombras |
| `/torre [ranking\|sair]` | todos | Torre Infinita: recordes, ranking e sair da subida |
| `/postura [nome]` | todos | Postura de combate (menu ou direto) |
| `/combos [arma]` | todos | Combos de arma: proficiência e golpes de cada sequência |
| `/mochila` | todos | Sua mochila (cresce com o nível total) |
| `/deus [milagre]` | todos | Sua fé: deus, devoção, oração e milagre |
| `/estrelas` | todos | Diário das Estrelas (constelações observadas e a desta noite) |
| `/enciclopedia` | todos | Enciclopédia do Mundo (biomas, estruturas, itens e relíquias) |
| `/cronista [entregar]` | todos | Sua campanha: lê o capítulo e entrega perto do Cronista |
| `/talentos` | todos | Árvore de talentos (pontos, faixas, refazer) |
| `/renascer [confirmar]` | todos | Renascer: o que falta e a confirmação |
| `/diario` | todos | Diário de Viagem (os marcos da sua jornada) |
| `/recordes` | todos | Recordes do servidor |
| `/sentar` | todos | Senta no chão (ou levanta) |
| `/rpg` | todos | Menu central: próximos passos, categorias e atalhos |
| `/guia [palavra\|livro\|mão\|dicas\|novidades]` | todos | Guia interativo e busca; `livro` dá o livro, `mão` explica o item da mão, `dicas` liga/desliga as dicas, `novidades` mostra o que entrou na versão |
| `/manual [link\|palavra]` (`/wiki`) | todos | O Manual numa janela dentro do jogo, com busca; `link` manda o link do site no chat |
| `/jornada` | todos | Jornada do Aventureiro (12 primeiros passos) |
| `/placar` | todos | Liga ou desliga o placar lateral |
| `/album` | todos | Álbum de Cartas: páginas, pacotes, pó, marcos e baralho |
| `/duelo <jogador\|aceitar\|abrir\|desistir>` | todos | Duelo de Cartas entre jogadores |
| `/segredos` | todos | Segredos descobertos e dicas dos outros |
| `/territorio` | todos | Menu do território (`mapa`, `bordas`, `info`, `reivindicar`, `liberar`, `adicionar`, `remover`, `boasvindas <frase>`, `abandonar confirmar`) |
| `/cosmeticos`, `/chapeu`, `/tag` | todos | Item na cabeça e tag personalizada |
| `/rpgadmin` | OP | Mostra todos os comandos de admin |

**Comandos de admin úteis para testar:**

| Comando | O que faz |
|---|---|
| `/rpgadmin teste` | **Área de testes (2.88):** um mundo plano à parte (`rpg_teste`), no criativo, com uma cópia do seu inventário. Lá nada conta: XP de atributos e armas, títulos, conquistas (do plugin e do jogo), XP de Caçador, recordes e o limite de guardiões. Ao entrar, o plugin guarda em `teste/<uuid>.yml` o inventário, o baú do fim, XP, vida, fome, efeitos, posição, modo de jogo e todos os dados do plugin no jogador. Ao sair (o mesmo comando, outro teleporte ou entrar de novo depois de cair), tudo volta como estava. Masmorras abertas no teste também são teste. O mundo não tem monstros nascendo, chuva nem estruturas do plugin, e o inventário fica ao morrer. Quando o último testador sai, o mundo de testes é **apagado** (só a pasta `dimensions/minecraft/rpg_teste`, nunca o mundo principal) e nasce limpo na próxima vez; ao ligar o servidor sem ninguém testando, uma sobra também é apagada. O que fica em arquivo do servidor (territórios, colônias, dona de classe lendária) não volta |
| `/rpgadmin teste limpar` | Na área de testes, remove criaturas e itens soltos a até 64 blocos (os moradores das colônias de teste ficam) |
| `/rpgadmin teste estrada` | **2.99.1:** na área de testes, monta duas colônias de teste (Aurora e Brisa, a 90 blocos) com depósito cheio de sobras, ligadas por uma Estrada Real iluminada com um marco em cada ponta, passando por você na direção em que olha. Nas colônias de teste você mexe como dono. Elas e os marcos somem quando o mundo de testes é apagado |
| `/rpgadmin set <jogador> <atributo> <nível>` | Define o nível de um atributo |
| `/rpgadmin forjar [raridade]` | Forja o item da mão |
| `/rpgadmin refino <0-10>` | Define o refino do item da mão |
| `/rpgadmin forjaferreiro` | Cria a Forja olhando para uma bigorna |
| `/rpgadmin infusor` | Cria o Infusor olhando para um caldeirão com água |
| `/rpgadmin altar` | Cria o Altar olhando para uma obsidiana chorosa |
| `/rpgadmin chefe <id>` | Invoca um chefe no altar que você olha |
| `/rpgadmin evento <id>` | Começa um evento no altar que você olha |
| `/rpgadmin raro <material> [qtd]` | Dá um material raro ou núcleo |
| `/rpgadmin infundir <parte>` | Infunde o item da mão sem custo |
| `/rpgadmin grimorio`, `/rpgadmin mana`, `/rpgadmin descobrir` | Grimório, mana cheia, todas as secretas |
| `/rpgadmin conquista <contador> <qtd>` | Soma progresso de conquista |
| `/rpgadmin cozinha` | Cria a Cozinha olhando para um defumador |
| `/rpgadmin reciclagem` | Cria a Bancada de Reciclagem olhando para um rebolo |
| `/rpgadmin variedade <id> [qtd] [semente]` | Dá uma variedade rara (ou as sementes dela) |
| `/rpgadmin prato <id> [qualidade]` | Dá um prato pronto |
| `/rpgadmin adubo [qtd]` | Dá Adubo Rico |
| `/rpgadmin marco` | Cria o seu Marco do Território olhando para uma magnetita |
| `/rpgadmin ignorar` | Liga/desliga ignorar as proteções (e gerenciar qualquer território pelo menu) |
| `/rpgadmin apagarterritorio <dono>` | Apaga o território de alguém |
| `/rpgadmin altardomador` | Cria o Altar do Domador olhando para um fardo de feno |
| `/rpgadmin invocar <criatura>` | Invoca um companheiro seu, sem custo |
| `/rpgadmin portalmasmorra` | Cria o Portal da Masmorra olhando para tijolos de pedra entalhados |
| `/rpgadmin masmorra <dificuldade>` | Abre uma masmorra só para você, sem custo nem requisito |
| `/rpgadmin sala modelo\|pos1\|pos2\|salvar\|lista\|apagar` | Ferramentas para construir salas de masmorra |
| `/rpgadmin fecharmasmorras` | Fecha todas as masmorras (todos voltam sem perder nada) |
| `/rpgadmin santuario` | Cria o Santuário das Classes olhando para um púlpito |
| `/rpgadmin classe <jogador> <classe> [nível]` | Dá uma classe (e o nível dela) direto |
| `/rpgadmin prova` | Começa a prova do seu caminho atual sem precisar das tarefas |
| `/rpgadmin lenda <lenda>` | Transforma o item da mão na lenda (sem feitos nem custo) |
| `/rpgadmin liberarlenda <lenda>` | Tira a lenda do dono atual |
| `/rpgadmin revelar <lenda\|todas>` | Mostra para você os feitos escondidos de uma lenda |
| `/rpgadmin pedraviagem` | Cria a Pedra de Viagem olhando para uma pedra entalhada de ardósia |
| `/rpgadmin local <tipo>` | Constrói um Local Oculto 8 blocos a leste de você (para testar) |
| `/rpgadmin locais` | Lista onde ficam todos os Locais Ocultos |
| `/rpgadmin liberarclasse <classe lendária>` | Tira a classe lendária do dono atual |
| `/rpgadmin bancadaalquimica` | Cria a Bancada Alquímica olhando para um suporte de poções |
| `/rpgadmin peixe <peixe> [qtd]` | Dá um peixe raro |
| `/rpgadmin oficio <material> [qtd]` | Dá uma pele, couro ou tecido do curtume |
| `/rpgadmin coleta <coleta> [qtd]` | Dá uma coleta da estação |
| `/rpgadmin semente <planta> [qtd]` | Dá sementes de uma plantação da estação |
| `/rpgadmin coletanascer` | Faz uma coleta da estação nascer perto de você |
| `/rpgadmin conjunto <conjunto>` | Dá as 4 peças de um conjunto |
| `/rpgadmin atelie` | Transforma o tear que você olha num Ateliê do Curtidor |
| `/rpgadmin sonho` | Faz você sonhar agora |
| `/rpgadmin cabanapesca` | Transforma a fogueira que você olha numa Cabana de Pesca |
| `/rpgadmin componente <componente> [qtd]` | Dá componentes alquímicos |
| `/rpgadmin gema <gema> [grau] [qtd]` | Dá gemas (grau 1 a 3) |
| `/rpgadmin elixir <elixir> [qualidade]` | Dá um elixir pronto |
| `/rpgadmin acessorio <acessório>` | Dá um acessório |
| `/rpgadmin cajado [raridade]` | Dá um Cajado Arcano forjado |
| `/rpgadmin tomo <magia>` | Dá um Tomo Proibido |
| `/rpgadmin maestria <jogador> <elemento> <nível>` | Define a maestria de um elemento (0 a 10) |
| `/rpgadmin prefeitura` | Cria a Prefeitura da sua colônia olhando para um sino |
| `/rpgadmin colono [qtd]` | Moradores chegam agora na sua colônia |
| `/rpgadmin turnocolonia` | Faz um turno de trabalho agora |
| `/rpgadmin nivelcolonia <1-5>` | Define o nível da sua colônia |
| `/rpgadmin guerra iniciar <reino A> <reino B>` | Começa uma guerra agora (para testar) |
| `/rpgadmin guerra encerrar` | Encerra as guerras em andamento |
| `/rpgadmin elite [1-3]` | O monstro que você olha vira Elite |
| `/rpgadmin ninho [tipo]` | Cria um ninho à sua frente |
| `/rpgadmin horda` | Sua colônia é atacada em 1 minuto |
| `/rpgadmin chefemundial` | O Chefe Mundial aparece em 30 segundos |
| `/rpgadmin idademundo <semanas>` | Muda a idade do mundo |
| `/rpgadmin estacao <estação>` | Muda a estação |
| `/rpgadmin festival` | Hoje vira dia de festival |
| `/rpgadmin clima <evento>` | Começa um evento de clima |
| `/rpgadmin ceu <luadesangue|meteoros|eclipse|aurora>` | Começa um evento do céu |
| `/rpgadmin maldicao <jogador> <tipo|nenhuma>` | Dá ou tira uma maldição |
| `/rpgadmin portal <dificuldade> [eco]` | Abre um portal 4 blocos à sua frente (`eco` = com o Eco do Soberano) |
| `/rpgadmin portalsortear` | Sorteia um portal perto de alguém agora |
| `/rpgadmin portaltransbordar` | O portal mais perto transborda agora |
| `/rpgadmin portalfechar` | Apaga o portal mais perto (sem prêmio) |
| `/rpgadmin obelisco` | Cria o Obelisco da Torre olhando para uma obsidiana chorona |
| `/rpgadmin torre <andar>` | Sobe a Torre sozinho a partir de um andar |
| `/rpgadmin proficiencia <jogador> <arma> <1-20>` | Define a proficiência de uma arma (combos) |
| `/rpgadmin vigor [jogador]` | Enche o vigor |
| `/rpgadmin gancho [jogador]` | Dá um Gancho de Escalada |
| `/rpgadmin criaturas` | Quantas criaturas há em cada mundo e os chunks mais cheios |
| `/rpgadmin limparcriaturas` | Remove sobras de eventos e monstros que nunca somem |
| `/rpgadmin santuariodivino`, `circulo`, `mesarunica` | Cria o Santuário dos Deuses, o Círculo de Transmutação ou a Mesa Rúnica olhando para o bloco |
| `/rpgadmin devocao <jogador> <qtd>` | Soma devoção ao deus que o jogador segue |
| `/rpgadmin pedrafilosofal` | Dá uma Pedra Filosofal |
| `/rpgadmin runa <runa>` | Grava a runa no item da sua mão, sem custo |
| `/rpgadmin mitrilo [minerio\|bruto\|lingote] [qtd]` | Põe um minério de mitrilo no bloco que você olha, ou dá os itens |
| `/rpgadmin mineral <ambar, jade, perola, cristal_de_gelo, pedra_sangue, turfa, cristal_bruto, geodo, cinzel ou bateia>` | Põe um mineral no bloco que você olha, tenta abrir um geodo no chunk, ou dá o Cinzel ou a Bateia |
| `/rpgadmin mapatesouro [comum\|raro\|lendario]` | Dá um mapa do tesouro |
| `/rpgadmin sitio` | Cria um sítio de arqueologia à sua frente |
| `/rpgadmin reliquia [relíquia]` | Dá uma relíquia |
| `/rpgadmin cronista` | Coloca o Cronista onde você está |
| `/rpgadmin capitulo <jogador> <0-8>` | Define o capítulo da campanha |
| `/rpgadmin pacote` | Mostra onde está o pacote de recursos e se ele está sendo enviado |
| `/rpgadmin talentopontos <jogador> <qtd>` | Pontos de talento a mais (negativo tira) |
| `/rpgadmin renascer <jogador> <0-5>` | Define quantas vezes o jogador renasceu (não mexe nos atributos) |
| `/rpgadmin companheironivel <1-30>` | Define o nível do companheiro que você olha |
| `/rpgadmin boneco`, `pedraamolar`, `fogueira`, `bebedouro`, `comedouro`, `cartografo` | Cria a estação olhando para o bloco dela |
| `/rpgadmin ferradura [qtd]`, `/rpgadmin ninhopassaro [qtd]` | Dá ferraduras ou ninhos de pássaro |
| `/rpgadmin animalraro [ouro\|neve]` | O animal que você olha vira raro |
| `/rpgadmin barril`, `colmeia`, `canteiro` | Cria a estação olhando para o bloco dela |
| `/rpgadmin carta <carta> [brilhante]` | Dá uma carta do álbum |
| `/rpgadmin flecha\|frasco\|erva\|mel <tipo> [qtd]` | Dá flechas, frascos, ervas ou méis |
| `/rpgadmin bebida <tipo> [grau 0-3]` | Dá uma bebida do barril |
| `/rpgadmin muda\|fruta <fruta> [qtd]` | Dá mudas ou frutas do pomar |
| `/rpgadmin mercador` | O Mercador Itinerante aparece perto de você |
| `/rpgadmin encontro [carroca\|bandidos\|viajante\|estrela]` | Começa um encontro perto de você |
| `/rpgadmin estrutura <tipo>` | Constrói a estrutura à sua frente (pelo console: `<tipo> <mundo> <x> <z>`) |
| `/rpgadmin estruturas` | Lista as estruturas mais perto de você |
| `/rpgadmin nomeada <arma ou aleatoria> [raridade]` | Uma arma com nome já forjada (padrão: Épica) |
| `/rpgadmin lendaria <qilin, fenghuang, longma, baihu, xuanwu, pixiu, baize ou kunpeng> <ovo, nascer, livre, estado ou aparecer>` | Lendárias: dá o ovo (Guardiões), faz nascer aqui como seu companheiro, volta ao começo, mostra onde está ou solta a Besta Errante aqui perto (para testar a prova) |
| `/rpgadmin nian [estado ou embora]` | Faz o Nian descer aqui perto (ou mostra onde está, ou manda embora) |
| `/rpgadmin constelacao <moedas N ou testar>` | Dá Moedas Estelares ou faz uma constelação comentar (para testar) |
| `/rpgadmin traje <traje ou aleatorio> [capacete/peitoral/calca/botas/todas] [raridade]` | Peças de um Traje (armadura com nome) já forjadas |
| `/rpgadmin conjuntomineral <ambar/jade/perola/gelo/pedra_sangue/magma/quartzo_negro> [forjar]` | As 9 peças de um conjunto dos minerais (forjadas, se pedir) |
| `/rpgadmin utilidade <chave/regador/bancada/balde_gelo/trena/cinto/todas>` | Os itens das utilidades do dia a dia |
| `/rpgadmin carroca [completa]` | A Carroça (completa = com todas as melhorias) |
| `/rpgadmin estrada [lista\|caravana\|assalto]` | Marcos, trechos e estradas; `caravana` manda já uma caravana da sua colônia (na área de testes, da colônia mais perto; chega em 1 min); `assalto` faz os salteadores atacarem a caravana que está na estrada |
| `/rpgadmin andarilho [peregrino\|mensageiro\|mercador\|visita\|praca]` | Alguém vem andando pela estrada mais perto (até 64 blocos); `praca`: um visitante na prefeitura de uma colônia ligada a até 40 blocos de você |
| `/rpgadmin bando` | Um bando passa perto de você agora (fora do bioma certo, cavalos) |
| `/rpgadmin trilha [sumir\|info]` | Forma uma trilha no bloco que você olha; `sumir` volta agora as trilhas do pedaço; `info` mostra o piso e quantas trilhas há no pedaço |
| `/rpgadmin cristal <e..s> [cacada/saque/monolitos]` | Um Cristal de Portal |
| `/rpgadmin cristalmana` | 4 Cristais de Mana de cada tamanho |
| `/rpgadmin grimoriooculto` | O Grimório Oculto (zera a magia e a recarga) |
| `/rpgadmin cacador <xp>` | Define o XP de caçador (D=100, C=300, B=700, A=1500, S=3000) |
| `/rpgadmin naoidentificado [e..s]` | 3 equipamentos não identificados da faixa daquele rank |
| `/rpgadmin pecacacador <e..s>` | As 5 peças de Caçador do rank |
| `/rpgadmin armachefe` | As 6 armas de chefe |
| `/rpgadmin itemsistema` | Os itens da Loja do Sistema (poções, pergaminho, medidor, chave) |
| `/rpgadmin inscricao [todas/<tipo>]` | Inscrições (2 de cada, ou 4 de um tipo) |
| `/rpgadmin despertar [zerar/<característica>]` | Desperta (se o rank já dá), zera as características e a árvore, ou dá uma característica |
| `/rpgadmin missao [zerar]` | Completa as tarefas da Missão Diária de hoje (o prêmio vem em até 5 s), ou recomeça o dia |
| `/rpgadmin reliquiamonarca [todas/<monarca>]` | As Relíquias do Monarca (as 25, ou as 5 de um) |
| `/rpgadmin monarca <chamas_brancas/geada/feras/insetos/nenhum>` | Dá a você a classe lendária daquele Monarca no nível 20 (ou tira) |
| `/rpgadmin dupla` | A masmorra em que você está vira Dupla (a Câmara das Estátuas abre quando o chefe cair) |
| `/rpgadmin ataquerank` | Abre um Ataque de Rank S perto de você |
| `/rpgadmin raiz` | Planta uma Raiz de Luz no chão embaixo de você (precisa de 5 blocos de ar em cima) |
| `/rpgadmin coracaopenumbra` | O Coração da Penumbra aparece à sua frente |
| `/rpgadmin penumbra` | Zera a sua Penumbra |
| `/rpgadmin itemceu <fruta_celeste|semente_brilhante> [qtd]` | Dá Fruta Celeste ou Semente Brilhante (o Fragmento Celeste vem com `/rpgadmin raro fragmento_celeste`) |
| `/rpgadmin esquecerestrutura` | Tira do registro a estrutura mais perto (os blocos ficam) |
| `/rpgadmin reload` | Recarrega o config |

Permissões (todas liberadas por padrão): `rpg.atributos`, `rpg.atributos.outros`, `rpg.chapeu`, `rpg.tag`, `rpg.tag.cores`, `rpg.forja`, `rpg.arcano`, `rpg.guia`, `rpg.titulos`, `rpg.missoes`, `rpg.receitas`, `rpg.party`, `rpg.territorio`, `rpg.pets`, `rpg.masmorra`, `rpg.classe`, `rpg.lendas`, `rpg.locais`, `rpg.pesca`, `rpg.alquimia`, `rpg.colonia`, `rpg.reino`, `rpg.bestiario`, `rpg.calendario`, `rpg.portais`, `rpg.torre`, `rpg.combos`, `rpg.mochila`, `rpg.fe`, `rpg.exploracao`, `rpg.progressao`, `rpg.detalhes`, `rpg.vida`. A permissão `rpg.admin` é só para OP.

## Mod de apoio (opcional, no cliente)

O servidor **não precisa** de nada além do plugin. O **mod de apoio** (`rpgapoio-0.2.0.jar`, para **NeoForge 26.3**; precisa do plugin 2.77 ou mais novo para as telas e o HUD) vai só no jogo de quem quiser, por exemplo no modpack. Ele cria teclas próprias em **Opções > Controles > RPGAtributos**, que dá para trocar como qualquer tecla do jogo:

| Tecla (padrão) | Ação | Sem o mod |
|---|---|---|
| **R** | Golpe da montaria | F montado |
| **V** | Habilidade de classe: **toque** usa; **segure** abre a **roda de habilidades** (aponte e solte) | Agachar + F |
| **B** | Trocar de habilidade | Agachar + clique esquerdo no ar |
| **Alt esquerdo** | Esquiva (para o lado que você aperta, ou para trás) | Dois toques em A, D ou S |
| **N** | Próxima postura | `/postura` |
| **J** | Menu do RPG | `/rpg` |
| **K** | Janela do Sistema (Árvore de Caçador e Despertar) | `/masmorra arvore` |
| **U** | Missão Diária | `/masmorra missao` |
| **Y** | Loja do Sistema | `/masmorra loja` |
| **O** | Árvore de classes | `/classe` |

**Telas (0.2.0):** com o mod, **todos os menus do plugin** ganham uma "pele": fundo com a cor do assunto (portais em roxo, classes em dourado, forja em laranja, colônia e reino em verde...), sem os vidros de enfeite e com um quadrinho atrás de cada item. Quatro menus têm **tela própria**: a **janela do Sistema** (rank e barra de XP, os ramos da Árvore de Caçador ligados por linhas, as características com nome), a **árvore de classes** (básicas em cima, avançadas embaixo ligadas a elas, e a página das lendárias com o Soberano no meio dos Monarcas), a **Loja do Sistema** (cartões com o preço em verde ou vermelho) e o **Mural das Relíquias** (uma linha por Monarca com a barra de quantas já estão lá). Por baixo é o mesmo menu do servidor: os cliques (esquerdo, direito, Shift) fazem o mesmo de sempre.

**HUD (0.2.0):** embaixo à direita, a sua classe com cada habilidade (a escolhida marcada, a recarga em segundos) e a mana com os Cristais de Mana; dentro de um portal, no alto à esquerda, o rank, o tempo e o objetivo (e a barra de ação fica livre); na Câmara das Estátuas, no meio da tela, o mandamento da vez e o aviso dos olhos ("vão abrir" / "NÃO SE MEXA").

Quem não tem o mod continua usando os atalhos e os menus de sempre; com o mod, os dois jeitos funcionam. Ao entrar, aparece "Mod de apoio conectado" na barra. Em servidores sem o RPGAtributos, as teclas não fazem nada (e o jogo avisa uma vez).

**Como funciona:** o mod manda o nome da tecla pelo canal `rpgatributos:tecla`; desde a 2.77 o plugin manda pelo `rpgatributos:dados` o HUD (a cada meio segundo) e qual menu está abrindo, para o mod desenhar a tela dele. O código fica em `mod-apoio/` (compile com `gradlew build`, Java 25; o jar sai em `mod-apoio/build/libs/`).

## Como gerar o .jar

1. Instale o **Java 25** (JDK, ex.: Adoptium Temurin) e o **Maven**.
2. Rode `mvn package` nesta pasta (também roda os testes de `src/test`).
3. O plugin fica em `target/RPGAtributos-2.99.1.jar`.

Outra opção é subir a pasta no GitHub: a aba **Actions** compila sozinha (o jar fica como artefato da execução). O jar de cada versão também fica na raiz do repositório (ex.: `RPGAtributos-2.51.0.jar`). Para publicar uma versão, crie uma tag (`git tag v2.51.0 && git push origin v2.51.0`) e o jar aparece em **Releases**.

Ao ligar, o plugin faz um **backup diário** dos seus `.yml` em `plugins/RPGAtributos/backups/` e guarda os últimos 7 dias (`backup.dias`).

## Instalar

1. Coloque o `.jar` em `plugins/` e reinicie.
2. Deixe **só uma versão** do plugin na pasta.

## Bom saber

- **Atualizando de uma versão antiga:** o `config.yml` antigo é guardado como `config-antigo-v1.yml` e um novo (nível 100, valores rebalanceados) é criado sozinho. Os níveis que os jogadores já têm continuam iguais.
- **Atualizando de versões anteriores:** as opções novas (`natacao`, `agricultura`, `culinaria`, `pesca`, `alquimia`, `reino`, `guerra`, `perigo`, `mundo-vivo`, `doma`, `classes`, `viagem`, `locais-ocultos`, `masmorra`, `party`, `territorio`) são acrescentadas sozinhas ao seu `config.yml`, sem mudar o que você já configurou.
- Parties, territórios e companheiros ficam salvos em `parties.yml`, `territorios.yml` e `companheiros.yml`, na pasta do plugin (os dados de cada companheiro também ficam na própria criatura).
- **Nível total com 13 atributos:** requisitos medidos em fração do nível total máximo (dificuldades de masmorra, título Veterano, classes básicas) passam a pedir um pouco mais, porque agora há 13 atributos para somar.
- Acessórios e o guarda-roupa ficam salvos no próprio jogador. Ao desligar o plugin, as armaduras voltam à aparência normal.
- Os reinos e as guerras marcadas ficam em `reinos.yml`.
- Os túmulos ficam em `lapides.yml` e os recordes (e as placas de recordes) em `recordes.yml`.
- A idade do mundo e o próximo Chefe Mundial ficam em `mundo.yml`. Ninhos e hordas somem ao desligar o servidor.
- As estações e a neve do inverno ficam em `estacoes.yml`. A estação conta a partir da data em que o plugin foi instalado.
- As colônias ficam em `colonias.yml` (os moradores são aldeões salvos no próprio mundo).
- Lendas, classes lendárias, Pedras de Viagem e Locais Ocultos ficam em `lendas.yml`, `classes_lendarias.yml`, `pedras.yml` e `locais.yml`. Para sortear novos Locais Ocultos, apague o `locais.yml` (os já construídos continuam no mundo como ruínas).
- As lendárias (quem tem cada uma, onde está o ovo e quanto já chocou) ficam em `lendarias.yml`. As Bestas Errantes soltas não são salvas: ao reiniciar elas voltam a estar livres. Para começar de novo, use `/rpgadmin lendaria <nome> livre`.
- O mundo `rpg_masmorras` é **apagado e recriado vazio** sempre que o servidor liga (as masmorras são temporárias). Não construa nada nele. O Cofre das Almas fica em `cofre.yml` e não se perde.
- Os recordes dos portais ficam em `recordes_portais.yml`; os donos dos Monarcas, como os das outras classes lendárias, em `classes_lendarias.yml`. O Rank de Caçador, a árvore, as características e a Missão Diária ficam no próprio jogador; as relíquias de cada Mural, no chunk dele.
- Se você já usa outro plugin de proteção (WorldGuard, GriefPrevention...), os dois funcionam juntos: basta um deles bloquear para a ação ser bloqueada.
- A tag usa um time de scoreboard; se usar outro plugin de nametag (ex.: TAB), desative a parte de nametag de um deles.
- Mudar o `multiplicador` das raridades no config vale para itens forjados depois disso.
- O efeito do Fardo Fértil, os chefes e os eventos em andamento acabam se o servidor reiniciar (por segurança, nada fica "perdido" no mundo).
- Magias e efeitos com poção em jogadores seguem a regra de PvP do mundo; proteções de região bloqueiam o dano, mas podem não bloquear esses efeitos.
