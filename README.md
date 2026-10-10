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
- **exploração**: Enciclopédia do Mundo, o minério **mitrilo** no fundo do mundo, **mapas do tesouro** com X de verdade, **arqueologia** com relíquias e a campanha **As Crônicas do Mundo** contada pelo Cronista;
- **progressão extra**: **talentos** em 4 árvores, **renascer** com bônus permanente (até ★★★★★) e **evolução dos companheiros** (Lobo → Lobo Alfa → Lobo das Sombras);
- **detalhes do dia a dia**: barco, carrinho, vara, élitro e armadura de cavalo forjados, Boneco de Treino, Pedra de Amolar, Fogueira de Acampamento (sentar e descansar), Bebedouro, troféus de chefe, túmulo ao morrer, animais raros, ninhos, recordes e o Diário de Viagem;
- **vida no mundo**: Barril de Envelhecimento, apicultura, Canteiro de Ervas, Álbum de Cartas, Mercador Itinerante, encontros na estrada, segredos, flechas especiais e frascos de arremesso;
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

## Bancada de Reciclagem

**Ritual:** jogue **1 funil** e **1 tesoura** em cima de um **rebolo**. Clique nela, escolha um item do seu inventário e recicle.
- Recicla tudo o que não empilha e tem receita de bancada: armas, ferramentas, armaduras, arcos, escudos, carrinhos, barcos...
- Volta **50% do material** da receita (até **75%** com Ferraria no máximo). Item gasto devolve menos.
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

**16 pratos**, liberados pelo nível de Culinária. Comer dá buffs (a duração fica gravada no prato):

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

**Qualidade do prato:** sorteada pelo nível de Culinária. Se a maioria dos ingredientes for Ótima ou melhor, o prato sobe uma qualidade; todos Perfeitos = prato Perfeito. Qualidade aumenta a duração (até +50%), e o nível de Culinária soma mais até +50%.

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
  - **Regras:** PvP, portas para visitantes, baús para visitantes, monstros nascem, explosões, fogo, reino e party ("Reino pode construir": membros do seu reino constroem aqui).
  - **Mostrar bordas:** partículas nas bordas por 20 segundos.
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
| Fazendeiro | Composteira | Trigo, cenoura, batata e beterraba das plantações em volta (com qualidade ★, às vezes uma variedade rara) | 1 |
| Lenhador | Bancada de flechas | Troncos (do tipo de árvore que houver perto), mudas e maçãs | 1 |
| Pescador | Barril (com água perto) | Peixes com qualidade, às vezes peixes raros | 1 |
| Minerador | Cortador de pedras | Pedra, carvão, minérios brutos e, às vezes, diamante | 2 |
| Cozinheiro | Cozinha | Pratos com os ingredientes do depósito | 2 |
| Ferreiro | Forja do Ferreiro | Conserta equipamentos do depósito e funde minérios brutos | 3 |
| Tratador | Altar do Domador | Couro, lã, ovos, penas e carne | 3 |
| Alquimista | Bancada Alquímica | Pó Arcano e Óleo de Peixe | 4 |

Cada morador precisa do **seu** posto: 2 fazendeiros precisam de 2 composteiras.

**Níveis da colônia** (Prefeitura → Melhorar, pago com o depósito): cada nível libera +3 moradores (até 15) e novas profissões. O nível 5 dá +20% de produção. Os moradores ficam protegidos de outros jogadores e não fazem comércio. Se a Prefeitura for quebrada, eles viram aldeões comuns.

### Construções (projetos)

No estilo MineColonies: a colônia cresce com construções que os **Construtores** erguem bloco a bloco.
1. **Pegue um projeto** na Prefeitura → **📜 Projetos**.
2. **Clique com o projeto no chão** onde quer a obra. **O lado em que você está vira a entrada.** Aparece uma **cerca** em volta do canteiro, com um **portão** na entrada e um corredor de 1 bloco entre a cerca e a obra.
3. **Agachado + clique na cerca** abre o menu da obra: **girar 90°**, **mudar de lugar** (a cerca some e o projeto volta), ver o contorno em partículas, a lista de materiais e **trazer do depósito** o que faltar. Girar e mudar só até a obra começar.
4. **Ponha um baú dentro da cerca** (no corredor, fora da obra) com os materiais.
5. Um **Construtor** livre (posto: bancada de trabalho; um posto por Construtor) vai até lá, **limpa o terreno** (o que sai vai para o baú da obra), tapa buracos sob o piso e **ergue a construção** de dia, tirando tudo do baú. Se faltar algo, ele avisa quanto falta e espera.
6. Pronta, a cerca some e a construção entra na lista da Prefeitura → **⚒ Obras**.

| Projeto | Tamanho | Nível | O que faz |
|---|---|---|---|
| Casa | 7×7 | 1 | 2 camas, janelas, porta e lanterna |
| Fazenda | 9×9 | 1 | Plantação cercada com água, trigo e a composteira do Fazendeiro |
| Poço da Praça | 5×5 | 1 | Deixa a colônia mais feliz (+5 cada, até 2) |
| Torre de Vigia | 5×5, 12 de altura | 1 | **Aumenta o território**: soma os chunks em volta dela (3×3, `territorio.raio-torre-de-vigia`, +1 de raio por nível), além do limite normal |
| Armazém | 7×7 | 2 | 7 baús que viram depósito da colônia |
| Biblioteca | 9×9 | 2 | Estantes e 2 atris (postos do Bibliotecário) |
| Quartel | 9×7 | 2 | 4 camas e 2 alvos (8 vagas de soldado) |

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
| Bibliotecário | Biblioteca | Atril | 2 |
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

Duas profissões a mais na colônia: **Soldado** (espada, liberado no nível 2) e **Arqueiro** (arco, nível 3). O posto delas é o **Quartel**, um **bloco de alvo** perto da Prefeitura, e cada um abriga 4 soldados.
- O morador **veste a armadura**: vira um guerreiro com o uniforme na **cor do reino**. Não queima no sol e os aldeões não têm medo dele.
- Os soldados atacam monstros sempre. Na **guerra**, também atacam os jogadores e os soldados do reino inimigo. Nunca atacam aldeões, aliados nem o dono.
- **Ordens** (Prefeitura → Exército, clique): **guardar** a colônia, **seguir você** ou **atacar** o Marco inimigo mais perto (só na guerra).
- **Shift + clique** no Exército equipa todos com as **melhores armas e armaduras do depósito**. Itens forjados contam, com os bônus deles.
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
| Coração do Mar | coração do mar | 30 | Não se afoga, nada rápido; quem monta respira debaixo d'água |
| Chamas | vara de blaze | 35 | Imune a fogo; os ataques incendeiam |
| Segunda Vida | totem da imortalidade | 40 | Escapa da morte uma vez |
| Asas | élitro | 50 | **Voa** montado: espaço sobe, agachar desce (pede Sela) |
| Pele de Netherite | lingote de netherite | 60 | +6 armadura, +4 resistência, quase não é empurrado, imune a fogo |

**Exemplo:** vaca de cogumelo + Sela + Asas = montaria voadora (Doma 50). Patrulhar + Coletor = um ajudante que recolhe os itens de uma fazenda.

- Componentes precisam do item **inteiro** (sem desgaste). Tirar devolve o item; se o companheiro morrer, componentes e alforje caem no chão.
- A vida, a armadura e o dano dos companheiros crescem com a Doma do dono. Ninguém da party (nem outros jogadores, onde não há PvP) fere os seus companheiros.
- `/pets` mostra todos, com vida, ordem e distância (ou a última posição, se estiverem longe). Shift + clique chama de volta (andando).

## Masmorras

**Ritual:** coloque **tijolos de pedra entalhados** e jogue em cima **1 olho do ender** e **1 bússola**. Vira o **Portal da Masmorra**.

**Entrar:** clique no portal, escolha a dificuldade e pague. A masmorra é construída em alguns segundos num mundo só dela (`rpg_masmorras`). Depois o **portal abre por 30 segundos**: quem subir no bloco entra. Entram você e a sua **party**.

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

**8 plantas**, 2 por estação, com visual próprio crescendo em 4 fases em cima da terra. Plante clicando com as sementes na **terra arada**. Só crescem **na estação delas** e com a **terra molhada** (água perto ou chuva; na chuva crescem mais rápido), mais ou menos 12 minutos por fase. **Farinha de osso** adianta uma fase. Clique na planta madura para colher; bater numa planta ainda crescendo arranca ela (a semente volta). As sementes saem do **mato quebrado** (4%) na estação certa, e às vezes voltam na colheita.

| Estação | Plantas |
|---|---|
| ✿ Primavera | Morango (rebrota, velocidade), Couve-Flor (regeneração) |
| ☀ Verão | Mirtilo (rebrota, visão noturna), Pimenta (força) |
| ☘ Outono | Uva (rebrota, sorte), Abóbora-Moranga (absorção) |
| ❄ Inverno | Couve Gelada (resistência), Nabo-de-Neve (esquenta) |

A colheita sai com qualidade ★ (pelo nível de Agricultura) e dá XP de Agricultura. Fora de época, a planta para de crescer, mas não morre. Pisar e desfazer a terra arada perde a planta (a semente volta).

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
- **Arco e besta** começam com o **esquerdo**, porque o direito puxa a flecha: `E E E`, `E D E`, `E E D` e `E D D`.
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

### Posturas, estilo e equilíbrio

**Posturas** (`/postura` ou o botão no `/combos`): valem enquanto você segura uma arma de combo.

| Postura | Efeito |
|---|---|
| ◇ Neutra | Sem bônus e sem perdas (padrão) |
| ▲ Ofensiva | +15% de dano e desequilibra +25%; leva +10% de dano e os golpes custam +10% de vigor |
| ■ Defensiva | -20% de dano recebido, resiste a empurrões, cada golpe de combo cura ½ ❤; -10% de dano e 5% mais lento |
| ≈ Ágil | +10% de velocidade, golpes recarregam 20% mais rápido, esquiva custa metade do vigor; -8% de dano |
| ✦ Mestra | A da arma na mão, na proficiência 20: **Duelista** (espada: +15% de dano e janela maior do golpe perfeito), **Carrasco** (machado: +40% em quem tem menos de 35% da vida), **Falange** (lança: +10% de dano e -25% de dano de quem está na frente), **Maré** (tridente: na água ou chuva +30% de dano e +20% de velocidade), **Colosso** (maça: +10% de dano, desequilibra o dobro, resiste a empurrões), **Atirador Paciente** (arco: parado há 1 s, +35% de dano) |

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
- **Habilidades:** **agache + F** usa a selecionada; **agache + clique esquerdo no ar** troca. A força cresce com o nível da classe. A classe aparece na tag acima da cabeça.

**A árvore** (as avançadas pedem classes **dominadas**, às vezes duas):

| Classe | Pede | Passivo (no nível 20) | Habilidades |
|---|---|---|---|
| ⚔ Guerreiro | Combate 15 | +2 ❤, +10% dano corpo a corpo | Investida, Grito de Guerra |
| ➹ Arqueiro | Combate 15 | +15% dano de flecha, +5% velocidade | Chuva de Flechas, Salto Evasivo |
| ✦ Mago | Arcano 15 | +40 mana, +1 mana/s | Míssil Arcano, Barreira Arcana |
| ✧ Ladino | Corrida 15 | +12% velocidade, 8% esquiva | Bomba de Fumaça, Rolamento |
| ⚒ Ferreiro | Ferraria 15 | 5% de forjar uma raridade acima, +3% refino | Reparo Rápido, Armadura Reforçada |
| ♞ Domador | Doma 15 | +1 companheiro, +15% dano deles | Chamado, Fúria da Matilha |
| ☠ Berserker | Guerreiro | +15% dano, até +40% com pouca vida | Fúria, Redemoinho |
| ♜ Cavaleiro | Guerreiro + Domador | +3 ❤, +4 armadura, +25% dano montado | Carga, Baluarte |
| ✚ Paladino | Guerreiro + Mago | +3 ❤, +3 armadura, +30 mana | Luz Sagrada, Escudo Divino |
| ➶ Atirador de Elite | Arqueiro | +30% dano de flecha, 15% de crítico x2 | Tiro Perfurante, Olho de Águia |
| ☤ Caçador | Arqueiro + Domador | +20% dano de flecha e dos companheiros | Marca do Caçador, Armadilha |
| ❄ Elementalista | Mago | +80 mana, +2 mana/s, magias 20% mais baratas | Tempestade Elemental, Escudo Elemental |
| ✴ Feiticeiro de Batalha | Mago + Guerreiro | +40 mana, +10% dano; golpes devolvem mana | Lâmina Arcana, Passo Arcano |
| ✝ Assassino | Ladino | +15% velocidade, 12% esquiva, +50% dano pelas costas | Passos das Sombras, Golpe Letal |
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

Dezesseis estruturas prontas surgem sozinhas em **terra nunca visitada** (chunks novos), longe de territórios, longe umas das outras (160 blocos) e das estruturas do próprio Minecraft, e só onde o chão é firme e quase plano. Cada uma cabe dentro de um chunk e o terreno é nivelado com cuidado (árvores no caminho são derrubadas inteiras). Ao chegar perto aparece o nome dela, entra no Diário e conta para o título **Andarilho das Ruínas** (12 estruturas).

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

### No fundo do mar

| Estrutura | Onde | O que tem |
|---|---|---|
| ⚓ **Naufrágio** | Fundo de qualquer oceano | Um navio adernado, de proa enterrada, com rombo no casco, mastro caído e âncora. Cada um tem **nome** e é de um tipo: **mercante** (esmeraldas, lã, especiarias), **pirata** (ouro, mapa do tesouro, às vezes mapa rasgado), **galeão real** (gema, blocos de esmeralda, às vezes Pedra de Proteção) ou **de expedição** (bússola, mapa, Pérola Negra, às vezes relíquia). O **diário do capitão** no baú da cabine conta a história e, se houver uma cidade submersa perto, **aponta a direção dela** |
| 🔱 **Cidade Submersa** | Mar profundo, longe do spawn. Rara: no máximo uma a cada 3000 × 3000 blocos | Uma cidade de pedra-do-mar de 73 blocos que adorava **Maris**: muralha com portões, avenidas com postes de luz, casas, o **Arquivo** (3 livros com a história da cidade), uma torre, um jardim de corais com a estátua de Maris e o **Templo** no centro, com o **cofre** embaixo (3 baús). Chegando perto, os **Guardas Afogados** e as **Sentinelas das Marés** (guardiões) atacam |

**O coração da cidade:** quem entra no santuário do templo acorda o **Sumo-Sacerdote** (afogado gigante de 450 de vida, com tridente e barra de chefe). Ele chama afogados e puxa quem está perto com um redemoinho. Derrotado, deixa o **Coração da cidade** (um condutor), pérolas negras, escamas do abismo, esmeraldas, relíquia e às vezes o tridente dele. Ponha o coração **bem no meio da moldura do altar**: a cidade **desperta**, os guardas somem para sempre, o condutor funciona ali, quem nada nela ganha Graça do Golfinho e quem segue Maris ganha **+80 de devoção**. Quem preferir ficar com o condutor pode: a cidade continua amaldiçoada e o Sumo-Sacerdote volta em 7 dias.

Para respirar, valem as coisas do jogo: poção de respiração, Respiração Aquática no capacete e condutores. Configure em `estruturas.cidade-submersa` (região, chance, distância do spawn e dias para os guardas voltarem).

### Pacote de recursos do servidor

O plugin gera o **seu próprio pacote de recursos** (`plugins/RPGAtributos/pacote/RPGAtributos-recursos.zip`) com as texturas do minério e dos itens de mitrilo, do **Cajado Arcano**, do **Gancho de Escalada** (que muda quando é lançado), da **Capa Planadora**, da **Ferradura**, do **Ninho de Pássaro**, dos **12 peixes raros**, dos **16 pratos da Cozinha**, das **variedades raras** (e das sementes de trigo e beterraba), dos **componentes alquímicos**, dos **12 elixires** (cada um com o seu frasco) , das **18 gemas** (bruta, lapidada e perfeita), dos **materiais raros e núcleos dos chefes**, dos **acessórios**, de **7 lendas** (as armas e ferramentas; o arco, o escudo e as armaduras ficam com o visual do jogo, porque têm animação ou acabamentos que o pacote não pode copiar) e dos itens novos: bebidas, cartas, flechas, frascos, méis e ervas. A plantação no chão continua com o visual do jogo: só os itens mudam. Ele é **opcional**: sem ele tudo funciona com a aparência do jogo (o minério parece um bloco de cogumelo, o lingote parece ferro, o cajado parece uma vara de breeze...).

**Editar as texturas à mão:** os desenhos originais ficam em `pacote/texturas-padrao/`. Copie um PNG para `pacote/texturas/`, edite (mesmo nome, imagem quadrada: 16×16, 32×32...) e reinicie: ele entra no lugar do original. Apagar o arquivo volta ao original. Itens feitos antes ganham o visual sozinhos quando o jogador entra. O pacote **se soma** ao pacote do servidor (não o troca). Para enviar, ponha um link em `pacote-de-recursos.url` ou ligue o servidor embutido (`porta` + `endereco`); veja `/rpgadmin pacote`.

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

**Reforjar:** élitro, armaduras de cavalo de ferro, ouro e diamante e qualquer equipamento achado no mundo não têm receita. Jogue o item e **1 Fragmento de Forja** em cima da Forja do Ferreiro: ele vira um item forjado.

**Ferradura** (bancada: 5 lingotes de ferro em U): clique num companheiro seu (cavalo, burro, camelo...) para dar **+15% de velocidade e +10% de pulo**, sem gastar espaço de componente. Ela cai de volta se ele for libertado ou morrer.

### Estações pequenas

| Estação | Ritual (jogue com Q) | O que faz |
|---|---|---|
| 🎯 **Boneco de Treino** | Fardo de feno + 1 abóbora esculpida + 1 suporte de armadura | Um boneco que nunca quebra: mostra o dano de cada golpe, o dano por segundo e o seu maior golpe. Dá um pouco de XP de Combate (até 120 por hora) e proficiência até o nível 5 |
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
### Álbum de Cartas (`/album`)

Monstros, chefes e peixes raros às vezes deixam uma **carta** (5% delas são **brilhantes**). Clique com ela na mão para guardar. São **54 cartas em 8 páginas** (Mortos-vivos, Artrópodes, Nether, Illagers, Estranhos, Fim, Chefes e Peixes raros), e completar uma página dá um bônus para sempre: +5% de dano naquele grupo (ou +2% de peixe raro). **5 cartas repetidas** trocam por uma que falta.

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
- **Peixes raros:** 12 espécies que só mordem em certos lugares, horários ou climas. `/peixes` abre o **Diário do Pescador**, com onde procurar cada um.
- **Tesouros do Mar:** esmeraldas, ouro, diamantes, livros encantados, Pérolas Negras e, raramente, Mapa Rasgado ou Fragmento de Forja.
- **Criaturas marinhas:** às vezes algo grande morde a isca e luta de volta (Afogado Faminto, Guardião das Ondas, Capitão Afogado e o gigante Senhor das Marés). Elas deixam Escamas do Abismo e Pérolas Negras.

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
| Anéis | Vigor (+2 ❤), Força (+1 dano), Vento (+5% velocidade), Marés (respiração aquática), Brasas (resistência ao fogo), Minerador (Pressa), Fortuna (+sorte), Sábio (+5% XP em tudo) |
| Amuletos | Mana (+40 mana, +1/s), Guardião (+2 armadura, +1 resistência), Vida (Regeneração), **Fênix** (escapa da morte a cada 10 minutos) |
| Cintos | Atleta (pulo e queda segura), Andarilho (sobe blocos sem pular), Titã (+2 ❤ e resistência a repulsão) |
| Bolso | **Lanterna de Bolso** (ilumina em volta, sem colocar blocos), Ímã (puxa itens do chão), Relógio (agachado: hora, coordenadas e bioma), **Capa Planadora** (no ar, pule para planar) |

## Guarda-roupa

`/guardaroupa` (ou Cosméticos → Guarda-roupa): escolha uma peça de armadura do inventário para ser a **aparência** de cada parte do corpo. Você continua com a proteção da armadura de verdade, só o visual muda, e todos veem igual (sem mod).
- Na **cabeça** vale qualquer item (bloco, flor, cabeça...).
- Dá para **esconder** uma peça (ela fica invisível).
- A aparência só aparece por cima de uma peça vestida. Ao tirar a peça, ela volta ao normal sozinha.

## Títulos

`/titulos`: 92 títulos com requisitos (em páginas), por exemplo Irmãos de Armas, Algoz das Marés, Coração do Mar, Pescador do Gelo, Mestre Curtidor, Sonhador, Coletor da Estação, Lavrador das Estações, Andarilho das Ruínas, Desejoso, Pomicultor, Colecionador de Cartas, Guardião de Segredos, Mestre Cervejeiro, Apicultor, Bom Samaritano, Colecionador de Troféus, Caçador de Raridades, Eterno, Renascido, Mestre dos Talentos, Vínculo Eterno, Cronista, Cartógrafo, Arqueólogo, Mineiro das Profundezas, Devoto, Astrônomo, Mestre Rúnico, Mestre da Lâmina, Arsenal Vivo, Mestre dos Combos, Finalizador, Minerador, Nadador, Pescador, Alquimista, Lenda dos Mares, Caçador de Tesouros, Lapidário, Fazendeiro, Chef, Domador, Grão-Mestre, Ferreiro Lendário, Reciclador, Botânico, Mestre-Cuca, Senhor das Feras, Arquimago, Mata-Gigantes, Fim dos Tempos, Explorador de Masmorras, Senhor das Masmorras, Desperto, Mestre de Armas, Portador de Lenda, Desbravador, Herdeiro Lendário, Viajante, Herói do Povo, Companheiro, Fundador, Senhor das Terras...
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
| `/masmorra [sair]` | todos | Informações da masmorra / sair dela |
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
| `/guia [palavra\|livro]` | todos | Guia interativo e busca; `livro` dá o livro |
| `/jornada` | todos | Jornada do Aventureiro (12 primeiros passos) |
| `/placar` | todos | Liga ou desliga o placar lateral |
| `/album` | todos | Álbum de Cartas |
| `/segredos` | todos | Segredos descobertos e dicas dos outros |
| `/territorio` | todos | Menu do território (`mapa`, `bordas`, `info`, `reivindicar`, `liberar`, `adicionar`, `remover`, `abandonar confirmar`) |
| `/cosmeticos`, `/chapeu`, `/tag` | todos | Item na cabeça e tag personalizada |
| `/rpgadmin` | OP | Mostra todos os comandos de admin |

**Comandos de admin úteis para testar:**

| Comando | O que faz |
|---|---|
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
| `/rpgadmin mapatesouro [comum\|raro\|lendario]` | Dá um mapa do tesouro |
| `/rpgadmin sitio` | Cria um sítio de arqueologia à sua frente |
| `/rpgadmin reliquia [relíquia]` | Dá uma relíquia |
| `/rpgadmin cronista` | Coloca o Cronista onde você está |
| `/rpgadmin capitulo <jogador> <0-8>` | Define o capítulo da campanha |
| `/rpgadmin pacote` | Mostra onde está o pacote de recursos e se ele está sendo enviado |
| `/rpgadmin talentopontos <jogador> <qtd>` | Pontos de talento a mais (negativo tira) |
| `/rpgadmin renascer <jogador> <0-5>` | Define quantas vezes o jogador renasceu (não mexe nos atributos) |
| `/rpgadmin companheironivel <1-30>` | Define o nível do companheiro que você olha |
| `/rpgadmin boneco`, `pedraamolar`, `fogueira`, `bebedouro`, `cartografo` | Cria a estação olhando para o bloco dela |
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
| `/rpgadmin esquecerestrutura` | Tira do registro a estrutura mais perto (os blocos ficam) |
| `/rpgadmin reload` | Recarrega o config |

Permissões (todas liberadas por padrão): `rpg.atributos`, `rpg.atributos.outros`, `rpg.chapeu`, `rpg.tag`, `rpg.tag.cores`, `rpg.forja`, `rpg.arcano`, `rpg.guia`, `rpg.titulos`, `rpg.missoes`, `rpg.receitas`, `rpg.party`, `rpg.territorio`, `rpg.pets`, `rpg.masmorra`, `rpg.classe`, `rpg.lendas`, `rpg.locais`, `rpg.pesca`, `rpg.alquimia`, `rpg.colonia`, `rpg.reino`, `rpg.bestiario`, `rpg.calendario`, `rpg.portais`, `rpg.torre`, `rpg.combos`, `rpg.mochila`, `rpg.fe`, `rpg.exploracao`, `rpg.progressao`, `rpg.detalhes`, `rpg.vida`. A permissão `rpg.admin` é só para OP.

## Como gerar o .jar

1. Instale o **Java 25** (JDK, ex.: Adoptium Temurin) e o **Maven**.
2. Rode `mvn package` nesta pasta.
3. O plugin fica em `target/RPGAtributos-2.31.1.jar`.

Outra opção é subir a pasta no GitHub: a aba **Actions** compila sozinha.

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
- O mundo `rpg_masmorras` é **apagado e recriado vazio** sempre que o servidor liga (as masmorras são temporárias). Não construa nada nele. O Cofre das Almas fica em `cofre.yml` e não se perde.
- Se você já usa outro plugin de proteção (WorldGuard, GriefPrevention...), os dois funcionam juntos: basta um deles bloquear para a ação ser bloqueada.
- A tag usa um time de scoreboard; se usar outro plugin de nametag (ex.: TAB), desative a parte de nametag de um deles.
- Mudar o `multiplicador` das raridades no config vale para itens forjados depois disso.
- O efeito do Fardo Fértil, os chefes e os eventos em andamento acabam se o servidor reiniciar (por segurança, nada fica "perdido" no mundo).
- Magias e efeitos com poção em jogadores seguem a regra de PvP do mundo; proteções de região bloqueiam o dano, mas podem não bloquear esses efeitos.
