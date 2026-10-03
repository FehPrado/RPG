# RPGAtributos

Plugin para **Paper 26.3** (Minecraft 26.3, Java 25) que transforma o servidor num RPG estilo Overgeared:
- **11 atributos** que evoluem com o uso (até o nível 100);
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
- **títulos** e **missões de aldeões**.

Tudo usa os atributos, efeitos, partículas e sons do próprio Minecraft: não precisa de mod nem resource pack.

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

**Nível máximo 100, balanceado:** no config os bônus são escritos como "quanto valem no nível máximo", e o plugin divide pelos níveis sozinho. Mudar o nível máximo não deixa ninguém forte demais. Chegar no 50 leva cerca de 20% do caminho; o 100 é para quem joga muito.

**Anti-farm:**
- Blocos colocados por jogadores e pedra de gerador não dão XP (a marca fica salva no chunk e acompanha o bloco empurrado por pistão).
- Pular parado no lugar não dá XP.
- Ser levado pela correnteza sem apertar nada não dá XP de Natação.
- Melancia e abóbora colocadas por jogadores não dão XP de Agricultura; plantação verde também não.
- Mobs de spawner dão só 25% do XP.
- Matar o mesmo jogador seguidas vezes, ou uma conta do mesmo IP, não dá XP.
- Material raro não pode ser gasto em receitas comuns (bancada, bigorna, fornalha, poções).

## Guia do Aventureiro

Na primeira vez que entra no servidor, o jogador recebe um livro de 35 páginas que explica tudo. `/guia` dá outro.

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

A mana aparece numa barra no topo da tela. Para **criar magias**, escolha de 1 a 4 essências do corpo e uma forma (Toque, Projétil, Aura, Corpo, Criação): são 1.925 magias possíveis, e todas funcionam.

**30 magias secretas** com efeito único. Alguns exemplos:

| Tipo | Magias |
|---|---|
| Pares | Bola de Fogo, Nevasca, Passo do Vazio, Drenar, Ponte de Gelo, Terremoto, Corrente Elétrica, Escudo de Gelo, Chama Sagrada, Retorno, Chamar a Chuva, Olho Vigilante |
| Trios | Fardo Fértil, Chamado da Tempestade, Fantasma, Meteoro, Matilha, Prisão de Gelo |
| Quartetos | Fênix, Buraco Negro, Gênese, Avatar Elemental, Santuário |

Ninguém conta a receita, mas o grimório dá uma dica de cada (quantas essências e uma delas).

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

**13 pratos**, liberados pelo nível de Culinária. Comer dá buffs (a duração fica gravada no prato):

| Prato | Nível | Efeito |
|---|---|---|
| Pão Caseiro | 0 | Pressa |
| Ensopado de Legumes | 0 | Regeneração |
| Torta de Abóbora da Vovó | 10 | Sorte e Velocidade |
| Salmão com Ervas | 15 | Respiração aquática e Graça do golfinho |
| Batata Recheada | 20 | Absorção e Resistência |
| Biscoito de Mel | 25 | Pressa II |
| Ensopado do Caçador | 30 | Força |
| Salada Arcana | 40 | Visão noturna e +60 de mana |
| Banquete do Guerreiro | 50 | Força, Resistência e Regeneração (pede Trigo Dourado) |
| Pão Dourado | 60 | Absorção II e saciedade cheia (Trigo Dourado) |
| Sopa Cristalina | 70 | Resistência ao fogo e Velocidade II (Cenoura Cristalina, Batata Ancestral) |
| Torta Rubi | 80 | Vida extra II e Regeneração II (Beterraba Rubi) |
| Banquete Lendário | 90 | Força, Resistência, Regeneração e Velocidade **para todos a até 10 blocos** (as 4 variedades) |

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
- **Um território por jogador.** Clique no Marco (ou `/territorio`) para o menu:
  - **Mapa:** os chunks em volta, coloridos (seu, de amigo, de outro, livre). Clique num livre para reivindicar; shift + clique num seu para liberar.
  - **Membros:** constroem e mexem em tudo. A **party** do dono também (regra "Party pode construir").
  - **Regras:** PvP, portas para visitantes, baús para visitantes, monstros nascem, explosões, fogo e party.
  - **Mostrar bordas:** partículas nas bordas por 20 segundos.
  - Segurando outro estandarte, clique na bandeira do menu para trocar.
- Ao entrar num território aparece "Território de Fulano" na tela.

**O que é protegido (para quem não é dono/membro):** quebrar e colocar blocos, baldes, placas, baús e qualquer bloco com inventário, portas/botões/alavancas, pisotear plantação, animais, aldeões (dá para comerciar), suportes de armadura, molduras, barcos e carrinhos, isqueiro, farinha de osso, enxada/machado/pá no bloco, estações (o defumador da Cozinha conta como baú; o Altar só invoca para quem mora lá).

**O mundo respeita a borda:** líquidos e pistões não atravessam, fogo de fora não entra, explosões não quebram blocos (com a regra desligada), endermans não pegam blocos, mobs não pisoteiam a plantação, árvores (inclusive a magia Gênese) não crescem para dentro. As magias que colocam blocos também respeitam os territórios.

Para remover o Marco: **Abandonar território** no menu (as esmeraldas não voltam; o estandarte cai no chão).

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

## Títulos

`/titulos`: 46 títulos com requisitos, por exemplo Minerador, Nadador, Fazendeiro, Chef, Domador, Grão-Mestre, Ferreiro Lendário, Reciclador, Botânico, Mestre-Cuca, Senhor das Feras, Arquimago, Mata-Gigantes, Fim dos Tempos, Explorador de Masmorras, Senhor das Masmorras, Desperto, Mestre de Armas, Portador de Lenda, Desbravador, Herdeiro Lendário, Viajante, Herói do Povo, Companheiro, Fundador, Senhor das Terras...
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
| `/territorio` | todos | Menu do território (`mapa`, `bordas`, `info`, `reivindicar`, `liberar`, `adicionar`, `remover`, `abandonar confirmar`) |
| `/cosmeticos`, `/chapeu`, `/tag` | todos | Item na cabeça e tag personalizada |
| `/guia` | todos | Livro Guia do Aventureiro |
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
| `/rpgadmin reload` | Recarrega o config |

Permissões (todas liberadas por padrão): `rpg.atributos`, `rpg.atributos.outros`, `rpg.chapeu`, `rpg.tag`, `rpg.tag.cores`, `rpg.forja`, `rpg.arcano`, `rpg.guia`, `rpg.titulos`, `rpg.missoes`, `rpg.receitas`, `rpg.party`, `rpg.territorio`, `rpg.pets`, `rpg.masmorra`, `rpg.classe`, `rpg.lendas`, `rpg.locais`. A permissão `rpg.admin` é só para OP.

## Como gerar o .jar

1. Instale o **Java 25** (JDK, ex.: Adoptium Temurin) e o **Maven**.
2. Rode `mvn package` nesta pasta.
3. O plugin fica em `target/RPGAtributos-2.7.0.jar`.

Outra opção é subir a pasta no GitHub: a aba **Actions** compila sozinha.

## Instalar

1. Coloque o `.jar` em `plugins/` e reinicie.
2. Deixe **só uma versão** do plugin na pasta.

## Bom saber

- **Atualizando de uma versão antiga:** o `config.yml` antigo é guardado como `config-antigo-v1.yml` e um novo (nível 100, valores rebalanceados) é criado sozinho. Os níveis que os jogadores já têm continuam iguais.
- **Atualizando de versões anteriores:** as opções novas (`natacao`, `agricultura`, `culinaria`, `doma`, `classes`, `viagem`, `locais-ocultos`, `masmorra`, `party`, `territorio`) são acrescentadas sozinhas ao seu `config.yml`, sem mudar o que você já configurou.
- Parties, territórios e companheiros ficam salvos em `parties.yml`, `territorios.yml` e `companheiros.yml`, na pasta do plugin (os dados de cada companheiro também ficam na própria criatura).
- Lendas, classes lendárias, Pedras de Viagem e Locais Ocultos ficam em `lendas.yml`, `classes_lendarias.yml`, `pedras.yml` e `locais.yml`. Para sortear novos Locais Ocultos, apague o `locais.yml` (os já construídos continuam no mundo como ruínas).
- O mundo `rpg_masmorras` é **apagado e recriado vazio** sempre que o servidor liga (as masmorras são temporárias). Não construa nada nele. O Cofre das Almas fica em `cofre.yml` e não se perde.
- Se você já usa outro plugin de proteção (WorldGuard, GriefPrevention...), os dois funcionam juntos: basta um deles bloquear para a ação ser bloqueada.
- A tag usa um time de scoreboard; se usar outro plugin de nametag (ex.: TAB), desative a parte de nametag de um deles.
- Mudar o `multiplicador` das raridades no config vale para itens forjados depois disso.
- O efeito do Fardo Fértil, os chefes e os eventos em andamento acabam se o servidor reiniciar (por segurança, nada fica "perdido" no mundo).
- Magias e efeitos com poção em jogadores seguem a regra de PvP do mundo; proteções de região bloqueiam o dano, mas podem não bloquear esses efeitos.
