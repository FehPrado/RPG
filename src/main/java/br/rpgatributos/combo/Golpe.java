package br.rpgatributos.combo;

import br.rpgatributos.classe.Classe;
import br.rpgatributos.lenda.Lenda;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Os golpes de combo. Cada arma tem 7: libera no nível de proficiência indicado, e o
 * jogador escolhe quais 4 ficam nas sequências. {@code vigor} é o custo e
 * {@code recarga} é em segundos.
 */
public enum Golpe {
    // ---------------- Espada ----------------
    ESTOCADA(TipoArma.ESPADA, 1, "Estocada", 15, 3, Material.IRON_SWORD,
            "Avança 6 blocos atravessando os inimigos no caminho."),
    CORTE_GIRATORIO(TipoArma.ESPADA, 3, "Corte Giratório", 20, 5, Material.GOLDEN_SWORD,
            "Gira a lâmina e acerta todos em volta, empurrando."),
    LAMINA_CRUZADA(TipoArma.ESPADA, 5, "Lâmina Cruzada", 20, 6, Material.SHEARS,
            "Corte em X na sua frente que faz os inimigos sangrarem."),
    EXECUCAO(TipoArma.ESPADA, 8, "Execução", 30, 10, Material.NETHERITE_SWORD,
            "Golpe pesado no alvo; quem estiver com menos de 30% de vida leva dano x4."),
    TEMPESTADE_DE_LAMINAS(TipoArma.ESPADA, 12, "Tempestade de Lâminas", 35, 12, Material.DIAMOND_SWORD,
            "Cinco giros seguidos em 2 segundos."),
    PASSO_FANTASMA(TipoArma.ESPADA, 16, "Passo Fantasma", 25, 8, Material.ECHO_SHARD,
            "Aparece nas costas do alvo e corta com dano dobrado."),
    JULGAMENTO_DA_LAMINA(TipoArma.ESPADA, 20, "Julgamento da Lâmina", 50, 25, Material.END_ROD,
            "Salta e crava uma lâmina de luz que corta uma linha de 9 blocos."),

    // ---------------- Machado ----------------
    SALTO_ESMAGADOR(TipoArma.MACHADO, 1, "Salto Esmagador", 20, 5, Material.IRON_AXE,
            "Salta para a frente e esmaga o chão onde cair, deixando os inimigos lentos."),
    ARREMESSO_DO_MACHADO(TipoArma.MACHADO, 3, "Arremesso do Machado", 15, 4, Material.GOLDEN_AXE,
            "Arremessa o machado girando; ele corta na ida e na volta."),
    RACHAR_ESCUDO(TipoArma.MACHADO, 5, "Rachar Escudo", 15, 6, Material.SHIELD,
            "Golpe que quebra a defesa: o alvo leva +25% de dano por 6 s e o escudo dele trava."),
    FURIA_DO_LENHADOR(TipoArma.MACHADO, 8, "Fúria do Lenhador", 30, 20, Material.BLAZE_POWDER,
            "Força e Pressa II por 8 s; cada golpe recupera vida."),
    REDEMOINHO_DE_ACO(TipoArma.MACHADO, 12, "Redemoinho de Aço", 35, 12, Material.DIAMOND_AXE,
            "Gira andando por 3 s, cortando tudo em volta."),
    DECAPITAR(TipoArma.MACHADO, 16, "Decapitar", 30, 10, Material.WITHER_SKELETON_SKULL,
            "Golpe enorme num alvo; se matar, devolve vigor e zera o Salto Esmagador."),
    CISAO_DA_TERRA(TipoArma.MACHADO, 20, "Cisão da Terra", 50, 25, Material.NETHERITE_AXE,
            "Racha o chão numa linha de 10 blocos e joga os inimigos para o alto."),

    // ---------------- Lança ----------------
    ESTOCADA_LONGA(TipoArma.LANCA, 1, "Estocada Longa", 15, 3, Material.IRON_SPEAR,
            "Perfura todos numa linha de 6 blocos."),
    VARREDURA(TipoArma.LANCA, 3, "Varredura", 20, 5, Material.STICK,
            "Arco de 180° na sua frente que empurra forte."),
    ARREMESSO_DA_LANCA(TipoArma.LANCA, 5, "Arremesso da Lança", 20, 6, Material.GOLDEN_SPEAR,
            "Arremessa uma lança espectral que atravessa tudo por 25 blocos."),
    SALTO_DO_DRAGAO(TipoArma.LANCA, 8, "Salto do Dragão", 30, 10, Material.DRAGON_BREATH,
            "Salta bem alto e mergulha onde você olha."),
    MURALHA_DE_LANCAS(TipoArma.LANCA, 12, "Muralha de Lanças", 30, 15, Material.POINTED_DRIPSTONE,
            "Por 5 s, quem chegar perto de você é ferido e empurrado."),
    PERFURACAO(TipoArma.LANCA, 16, "Perfuração", 25, 8, Material.DIAMOND_SPEAR,
            "Três estocadas rápidas em linha que quebram a defesa."),
    LANCA_CELESTE(TipoArma.LANCA, 20, "Lança Celeste", 50, 25, Material.NETHERITE_SPEAR,
            "Uma lança de luz cai do céu onde você olha."),

    // ---------------- Tridente ----------------
    ONDA(TipoArma.TRIDENTE, 1, "Onda", 15, 4, Material.WATER_BUCKET,
            "Uma onda na sua frente que fere e empurra."),
    PUXAO(TipoArma.TRIDENTE, 3, "Puxão", 15, 6, Material.FISHING_ROD,
            "Puxa o alvo que você olha até você."),
    REDEMOINHO(TipoArma.TRIDENTE, 5, "Redemoinho", 25, 10, Material.HEART_OF_THE_SEA,
            "Um redemoinho onde você olha que puxa e fere por 5 s (mais forte na chuva ou na água)."),
    TRIDENTE_TROVAO(TipoArma.TRIDENTE, 8, "Tridente do Trovão", 30, 12, Material.LIGHTNING_ROD,
            "Raio no alvo que salta para mais 2 inimigos."),
    MARE_ALTA(TipoArma.TRIDENTE, 12, "Maré Alta", 30, 15, Material.CONDUIT,
            "Você e a party nadam rápido, respiram e regeneram; inimigos perto ficam lentos."),
    MERGULHO(TipoArma.TRIDENTE, 16, "Mergulho", 25, 8, Material.PRISMARINE_SHARD,
            "Dispara para onde você olha (3x mais longe na água), ferindo quem atravessar."),
    FURIA_DE_POSEIDON(TipoArma.TRIDENTE, 20, "Fúria de Poseidon", 50, 25, Material.TRIDENT,
            "Tempestade de raios e ondas em volta de você por 3 s."),

    // ---------------- Maça ----------------
    ABALO_SISMICO(TipoArma.MACA, 1, "Abalo Sísmico", 20, 5, Material.MACE,
            "Bate no chão: fere e deixa lentos todos em volta."),
    MARTELO_ASCENDENTE(TipoArma.MACA, 3, "Martelo Ascendente", 20, 6, Material.PISTON,
            "Joga o alvo à sua frente para o alto."),
    ATORDOAR(TipoArma.MACA, 5, "Atordoar", 20, 8, Material.BELL,
            "Golpe em cone que atordoa por 2 s."),
    QUEBRA_ARMADURA(TipoArma.MACA, 8, "Quebra-Armadura", 25, 10, Material.CHAINMAIL_CHESTPLATE,
            "Golpe forte; o alvo leva +25% de dano por 8 s."),
    SALTO_METEORO(TipoArma.MACA, 12, "Salto Meteoro", 35, 12, Material.FIRE_CHARGE,
            "Salta alto e cai como um meteoro: quanto maior a queda, maior o dano."),
    ECO_DO_IMPACTO(TipoArma.MACA, 16, "Eco do Impacto", 30, 10, Material.SCULK_SHRIEKER,
            "Bate no chão e solta três ondas de choque cada vez maiores."),
    MARTELO_DOS_DEUSES(TipoArma.MACA, 20, "Martelo dos Deuses", 50, 25, Material.HEAVY_CORE,
            "Um martelo gigante cai do céu onde você olha."),

    // ---------------- Arco e besta ----------------
    TIRO_TRIPLO(TipoArma.ARCO, 1, "Tiro Triplo", 15, 3, Material.ARROW,
            "Três flechas em leque."),
    FLECHA_PERFURANTE(TipoArma.ARCO, 3, "Flecha Perfurante", 15, 5, Material.SPECTRAL_ARROW,
            "Flecha rapidíssima que atravessa vários inimigos."),
    SALTO_TATICO(TipoArma.ARCO, 5, "Salto Tático", 15, 6, Material.FEATHER,
            "Salta para trás e atira uma flecha para a frente."),
    FLECHA_EXPLOSIVA(TipoArma.ARCO, 8, "Flecha Explosiva", 25, 8, Material.TNT,
            "Flecha que explode ao acertar (sem quebrar blocos)."),
    SARAIVADA(TipoArma.ARCO, 12, "Saraivada", 30, 12, Material.TIPPED_ARROW,
            "Uma chuva de flechas cai onde você olha."),
    FLECHA_SOMBRIA(TipoArma.ARCO, 16, "Flecha Sombria", 25, 10, Material.ENDER_EYE,
            "Flecha que persegue o inimigo mais perto e o marca (+20% de dano)."),
    TIRO_CELESTIAL(TipoArma.ARCO, 20, "Tiro Celestial", 50, 25, Material.BEACON,
            "Carrega por 1 s e dispara um feixe de luz de 40 blocos que atravessa tudo."),

    // ---------------- Adaga (2.51) ----------------
    PUNHALADA(TipoArma.ADAGA, 1, "Punhalada", 12, 2, Material.IRON_SWORD,
            "Estocada rápida no alvo à frente; pelas costas, dano dobrado."),
    CORTE_VENENOSO(TipoArma.ADAGA, 3, "Corte Venenoso", 15, 5, Material.SPIDER_EYE,
            "Corte que envenena e faz sangrar."),
    ROLAMENTO(TipoArma.ADAGA, 5, "Rolamento", 15, 5, Material.RABBIT_FOOT,
            "Rola para trás e arremessa uma lâmina no inimigo mais perto."),
    NUVEM_DE_FUMACA(TipoArma.ADAGA, 8, "Nuvem de Fumaça", 25, 12, Material.GUNPOWDER,
            "Fumaça que cega os inimigos em volta; você fica invisível por 3 s."),
    DANCA_DAS_LAMINAS(TipoArma.ADAGA, 12, "Dança das Lâminas", 30, 12, Material.SHEARS,
            "Seis cortes rápidos em todos que estiverem perto."),
    SOMBRA_ASSASSINA(TipoArma.ADAGA, 16, "Sombra Assassina", 25, 10, Material.INK_SAC,
            "Aparece nas costas do alvo e crava a adaga (dano x2,5)."),
    MARCA_DA_MORTE(TipoArma.ADAGA, 20, "Marca da Morte", 50, 25, Material.WITHER_ROSE,
            "Marca o alvo: em 3 s ele leva um golpe enorme (mais forte quanto menos vida tiver)."),

    // ---------------- Lâminas Duplas (adaga em cada mão) ----------------
    CORTE_DUPLO(TipoArma.DUPLAS, 1, "Corte Duplo", 12, 2, Material.SHEARS,
            "Dois cortes seguidos no alvo, um com cada mão."),
    REDEMOINHO_GEMEO(TipoArma.DUPLAS, 3, "Redemoinho Gêmeo", 20, 5, Material.STRING,
            "Gira duas vezes acertando todos em volta."),
    X_CRUZADO(TipoArma.DUPLAS, 5, "X Cruzado", 20, 6, Material.REDSTONE,
            "Corte em X na sua frente que faz sangrar muito."),
    INVESTIDA_GEMEA(TipoArma.DUPLAS, 8, "Investida Gêmea", 25, 8, Material.FEATHER,
            "Avança 7 blocos cortando todos no caminho."),
    CHUVA_DE_ACO(TipoArma.DUPLAS, 12, "Chuva de Aço", 30, 12, Material.IRON_NUGGET,
            "Oito cortes em 2 segundos em volta de você."),
    DANCA_DO_VENTO(TipoArma.DUPLAS, 16, "Dança do Vento", 30, 10, Material.WIND_CHARGE,
            "Salta girando e cai cortando, jogando os inimigos para o alto."),
    CICLONE_CARMESIM(TipoArma.DUPLAS, 20, "Ciclone Carmesim", 50, 25, Material.REDSTONE_BLOCK,
            "Gira por 3 s andando, cortando tudo em volta e roubando vida."),

    // ---------------- Arremesso (faca e machadinha) ----------------
    LEQUE_DE_FACAS(TipoArma.ARREMESSO, 1, "Leque de Facas", 12, 3, Material.FLINT,
            "Três lâminas em leque."),
    RICOCHETE(TipoArma.ARREMESSO, 3, "Ricochete", 15, 5, Material.SLIME_BALL,
            "Uma lâmina que salta entre até 4 inimigos."),
    LAMINA_QUE_PRENDE(TipoArma.ARREMESSO, 5, "Lâmina que Prende", 15, 6, Material.COBWEB,
            "Crava o alvo no chão: fica quase parado por 3 s."),
    MACHADINHA_GIRATORIA(TipoArma.ARREMESSO, 8, "Machadinha Giratória", 20, 8, Material.IRON_AXE,
            "Uma machadinha que atravessa tudo e volta para você."),
    CHUVA_DE_LAMINAS(TipoArma.ARREMESSO, 12, "Chuva de Lâminas", 30, 12, Material.POINTED_DRIPSTONE,
            "Lâminas caem do céu onde você olha."),
    LAMINA_EXPLOSIVA(TipoArma.ARREMESSO, 16, "Lâmina Explosiva", 25, 10, Material.TNT,
            "Lâmina que explode ao acertar (sem quebrar blocos)."),
    TEMPESTADE_DE_ACO(TipoArma.ARREMESSO, 20, "Tempestade de Aço", 50, 25, Material.ANVIL,
            "Vinte lâminas voam em todas as direções em 2 segundos."),

    // ---------------- Foice ----------------
    CEIFAR(TipoArma.FOICE, 1, "Ceifar", 15, 3, Material.IRON_HOE,
            "Corte largo (200°) na sua frente."),
    GANCHO_DA_FOICE(TipoArma.FOICE, 3, "Gancho da Foice", 15, 5, Material.TRIPWIRE_HOOK,
            "Engancha o alvo e puxa até você."),
    COLHEITA(TipoArma.FOICE, 5, "Colheita", 20, 6, Material.WHEAT,
            "Corte em volta que cura você um pouco por inimigo atingido."),
    FOICE_GIRATORIA(TipoArma.FOICE, 8, "Foice Giratória", 25, 8, Material.NETHERITE_HOE,
            "A foice gira em volta de você por 2 s cortando quem chegar perto."),
    CEIFA_DAS_ALMAS(TipoArma.FOICE, 12, "Ceifa das Almas", 30, 12, Material.SOUL_LANTERN,
            "Corte de almas em área que faz os inimigos murcharem."),
    PASSO_DO_CEIFADOR(TipoArma.FOICE, 16, "Passo do Ceifador", 25, 10, Material.SOUL_SAND,
            "Atravessa 8 blocos, cortando e fazendo murchar quem estiver no caminho."),
    COLHEITA_FINAL(TipoArma.FOICE, 20, "Colheita Final", 50, 25, Material.WITHER_SKELETON_SKULL,
            "Uma foice gigante corta tudo em volta; quem tiver menos de 20% de vida cai."),

    // ---------------- Bastão ----------------
    VARREDURA_BAIXA(TipoArma.BASTAO, 1, "Varredura Baixa", 15, 3, Material.STICK,
            "Varre as pernas de quem estiver em volta: dano e lentidão."),
    ESTOCADA_DO_BASTAO(TipoArma.BASTAO, 3, "Estocada do Bastão", 15, 4, Material.BAMBOO,
            "Empurra o alvo para longe."),
    SALTO_COM_VARA(TipoArma.BASTAO, 5, "Salto com Vara", 15, 6, Material.BAMBOO_BLOCK,
            "Salto longo e alto para a frente, sem dano de queda."),
    MOINHO(TipoArma.BASTAO, 8, "Moinho", 25, 8, Material.WHITE_BANNER,
            "Gira o bastão por 2 s empurrando todos em volta."),
    TERREMOTO_DO_BASTAO(TipoArma.BASTAO, 12, "Bastão Sísmico", 30, 12, Material.DRIPSTONE_BLOCK,
            "Bate no chão: os inimigos em volta voam e ficam no ar."),
    DEFESA_DO_MONGE(TipoArma.BASTAO, 16, "Defesa do Monge", 25, 14, Material.SHIELD,
            "Por 5 s: Resistência II e quem te bater é empurrado."),
    PILAR_CELESTE(TipoArma.BASTAO, 20, "Pilar Celeste", 50, 25, Material.END_ROD,
            "Uma onda em linha de 10 blocos joga todos para o alto."),

    // ---------------- Chicote ----------------
    CHICOTADA(TipoArma.CHICOTE, 1, "Chicotada", 12, 2, Material.LEAD,
            "Estala no primeiro inimigo até 8 blocos à frente."),
    LACO(TipoArma.CHICOTE, 3, "Laço", 15, 5, Material.STRING,
            "Laça o alvo e puxa até você."),
    DESARMAR(TipoArma.CHICOTE, 5, "Desarmar", 20, 8, Material.IRON_SWORD,
            "O alvo fica com Fraqueza II por 6 s (e larga a arma, se for um monstro)."),
    ESTALO(TipoArma.CHICOTE, 8, "Estalo", 20, 6, Material.FIREWORK_STAR,
            "Estalo em cone de 7 blocos que atordoa por 1 s."),
    BALANCO(TipoArma.CHICOTE, 12, "Balanço", 25, 10, Material.VINE,
            "Laça um bloco até 15 blocos à frente e puxa você até ele."),
    CHICOTE_DE_FOGO(TipoArma.CHICOTE, 16, "Chicote de Fogo", 30, 10, Material.BLAZE_ROD,
            "Uma linha de fogo de 9 blocos que incendeia."),
    SERPENTE(TipoArma.CHICOTE, 20, "Serpente", 50, 25, Material.TWISTING_VINES,
            "Três chicotadas em círculo de 7 blocos que puxam todos para você."),

    // ---------------- Montante (minério) ----------------
    GOLPE_PESADO(TipoArma.MONTANTE, 1, "Golpe Pesado", 15, 3, Material.IRON_BLOCK,
            "Um corte de cima para baixo no alvo à frente (dano x2) que o empurra longe."),
    VARREDURA_DO_TITA(TipoArma.MONTANTE, 3, "Varredura do Titã", 20, 5, Material.DIAMOND_SWORD,
            "Corte largo em meia-lua que acerta todos na sua frente."),
    INVESTIDA_DO_MONTANTE(TipoArma.MONTANTE, 5, "Investida do Montante", 20, 6, Material.DIAMOND_HORSE_ARMOR,
            "Avança com a lâmina baixa, cortando quem estiver no caminho."),
    LAMINA_ERGUIDA(TipoArma.MONTANTE, 8, "Lâmina Erguida", 25, 8, Material.PISTON,
            "Golpe de baixo para cima que joga os inimigos em volta para o alto."),
    CORTE_DO_HORIZONTE(TipoArma.MONTANTE, 12, "Corte do Horizonte", 30, 12, Material.PRISMARINE_SHARD,
            "Uma onda de corte em linha de 10 blocos."),
    FURIA_DO_GIGANTE(TipoArma.MONTANTE, 16, "Fúria do Gigante", 30, 14, Material.BLAZE_POWDER,
            "Força II e Resistência por 6 s, com um giro que empurra todos."),
    QUEDA_DA_MONTANHA(TipoArma.MONTANTE, 20, "Queda da Montanha", 50, 25, Material.ANVIL,
            "Salta e cai com o Montante: dano enorme em 6 blocos e deixa todos lentos."),

    // ---------------- Bumerangue (minério) ----------------
    ARCO_DE_AMBAR(TipoArma.BUMERANGUE, 1, "Arco de Âmbar", 12, 3, Material.RAW_GOLD,
            "O bumerangue vai e volta atravessando todos no caminho."),
    DUPLO_RETORNO(TipoArma.BUMERANGUE, 3, "Duplo Retorno", 15, 5, Material.GOLD_NUGGET,
            "Dois bumerangues abertos em V, que voltam juntos."),
    PANCADA_ATORDOANTE(TipoArma.BUMERANGUE, 5, "Pancada Atordoante", 15, 6, Material.BELL,
            "Acerta um alvo em cheio: fica tonto (lento e cego) por 2 s."),
    ORBITA(TipoArma.BUMERANGUE, 8, "Órbita", 20, 8, Material.CLOCK,
            "O bumerangue gira duas vezes em volta de você."),
    SOL_DO_DESERTO(TipoArma.BUMERANGUE, 12, "Sol do Deserto", 25, 10, Material.SUNFLOWER,
            "Lança para o alto: ele cai em brasa onde você mira e incendeia em volta."),
    RICOCHETE_DE_AMBAR(TipoArma.BUMERANGUE, 16, "Ricochete de Âmbar", 25, 10, Material.HONEYCOMB,
            "Quica entre até 5 inimigos perto."),
    TEMPESTADE_DE_AREIA(TipoArma.BUMERANGUE, 20, "Tempestade de Areia", 50, 25, Material.SAND,
            "Oito bumerangues em todas as direções, que voltam e cegam."),

    // ---------------- Leques de Guerra (minério) ----------------
    CORTE_DO_LEQUE(TipoArma.LEQUES, 1, "Corte do Leque", 12, 2, Material.BAMBOO,
            "Abre os leques num corte rápido em leque à frente."),
    RAJADA_DE_VENTO(TipoArma.LEQUES, 3, "Rajada de Vento", 15, 5, Material.WIND_CHARGE,
            "Um sopro que joga longe quem estiver na sua frente."),
    DANCA_DA_GARCA(TipoArma.LEQUES, 5, "Dança da Garça", 15, 6, Material.FEATHER,
            "Salta para trás e solta uma lâmina de vento que atravessa."),
    PETALAS_DE_JADE(TipoArma.LEQUES, 8, "Pétalas de Jade", 20, 10, Material.PINK_PETALS,
            "Quatro giros cortando em volta; você recupera 2 ❤."),
    VENDAVAL(TipoArma.LEQUES, 12, "Vendaval", 25, 10, Material.BREEZE_ROD,
            "Levanta todos em 5 blocos para o alto."),
    LEQUE_ESPELHO(TipoArma.LEQUES, 16, "Leque Espelho", 25, 14, Material.GLASS_PANE,
            "Por 6 s: Absorção II e o vento afasta quem chegar perto."),
    FURACAO_DE_JADE(TipoArma.LEQUES, 20, "Furacão de Jade", 50, 25, Material.EMERALD,
            "Um furacão de 5 blocos que puxa e corta por 2 s."),

    // ---------------- Arpão (minério) ----------------
    ARPOAR(TipoArma.ARPAO, 1, "Arpoar", 12, 3, Material.TRIPWIRE_HOOK,
            "Arremessa o arpão: acerta o primeiro e puxa até você."),
    ESTOCADA_DO_ARPAO(TipoArma.ARPAO, 3, "Estocada do Arpão", 15, 4, Material.PRISMARINE_SHARD,
            "Estocada funda no alvo à frente, que faz sangrar."),
    CORRENTE_MARINHA(TipoArma.ARPAO, 5, "Corrente Marinha", 20, 6, Material.KELP,
            "Uma corrente de 9 blocos puxa todos na linha (mais forte na água)."),
    MERGULHO_DO_TUBARAO(TipoArma.ARPAO, 8, "Mergulho do Tubarão", 20, 8, Material.HEART_OF_THE_SEA,
            "Avança como um tubarão (o dobro na água), acertando quem estiver no caminho."),
    ARPAO_TRIPLO(TipoArma.ARPAO, 12, "Arpão Triplo", 25, 10, Material.TRIDENT,
            "Três arpões abertos que puxam os inimigos."),
    ONDA_DO_ARPAO(TipoArma.ARPAO, 16, "Onda Quebrando", 30, 12, Material.WATER_BUCKET,
            "Uma onda em volta empurra e deixa lentos todos em 5 blocos."),
    FURIA_DO_LEVIATA(TipoArma.ARPAO, 20, "Fúria do Leviatã", 50, 25, Material.NAUTILUS_SHELL,
            "Um arpão gigante atravessa 25 blocos e arrasta tudo para você."),

    // ---------------- Mangual (minério) ----------------
    GOLPE_GELADO(TipoArma.MANGUAL, 1, "Golpe Gelado", 12, 3, Material.SNOWBALL,
            "Pancada no alvo à frente que o deixa bem lento."),
    GIRO_DA_CORRENTE(TipoArma.MANGUAL, 3, "Giro da Corrente", 15, 5, Material.IRON_NUGGET,
            "Gira o mangual quatro vezes em volta de você."),
    QUEBRA_GELO(TipoArma.MANGUAL, 5, "Quebra-Gelo", 20, 6, Material.PACKED_ICE,
            "Bate no chão: dano e lentidão em todos perto."),
    CORRENTE_QUE_PRENDE(TipoArma.MANGUAL, 8, "Corrente que Prende", 20, 8, Material.LEAD,
            "A corrente enrola o alvo, puxa e prende no lugar."),
    NEVASCA(TipoArma.MANGUAL, 12, "Nevasca", 30, 12, Material.POWDER_SNOW_BUCKET,
            "Uma nevasca em volta por 3 s: dano, lentidão e frio."),
    PRISAO_DE_GELO(TipoArma.MANGUAL, 16, "Prisão de Gelo", 25, 12, Material.BLUE_ICE,
            "Congela o alvo por 3 s com uma pancada forte."),
    AVALANCHE(TipoArma.MANGUAL, 20, "Avalanche", 50, 25, Material.SNOW_BLOCK,
            "Três impactos de gelo em linha à sua frente, cada vez mais longe."),

    // ---------------- Garras (minério) ----------------
    RASGAR(TipoArma.GARRAS, 1, "Rasgar", 12, 2, Material.NETHER_BRICK,
            "Quatro garradas rápidas no alvo que roubam vida."),
    SALTO_DA_FERA(TipoArma.GARRAS, 3, "Salto da Fera", 15, 5, Material.RABBIT_FOOT,
            "Salta no alvo até 8 blocos e crava as garras."),
    SANGRIA(TipoArma.GARRAS, 5, "Sangria", 15, 6, Material.REDSTONE,
            "Um corte fundo: o alvo sangra por 6 s."),
    FRENESI(TipoArma.GARRAS, 8, "Frenesi", 20, 14, Material.SUGAR,
            "Por 8 s: Pressa II, Velocidade e Força."),
    GARRAS_CRUZADAS(TipoArma.GARRAS, 12, "Garras Cruzadas", 25, 8, Material.SHEARS,
            "Corte em X na sua frente que faz sangrar."),
    PREDADOR(TipoArma.GARRAS, 16, "Predador", 25, 12, Material.ENDER_EYE,
            "Some por 3 s e marca o alvo: ele leva +30% de dano por 8 s."),
    BANQUETE_DE_SANGUE(TipoArma.GARRAS, 20, "Banquete de Sangue", 50, 25, Material.CRIMSON_FUNGUS,
            "Quatro giros sangrentos que curam 25% do dano causado."),

    // ---------------- Clava Fumegante (minério) ----------------
    PANCADA_EM_BRASA(TipoArma.CLAVA, 1, "Pancada em Brasa", 12, 3, Material.COAL,
            "Pancada no alvo à frente que pega fogo."),
    FUMACA_SUFOCANTE(TipoArma.CLAVA, 3, "Fumaça Sufocante", 15, 6, Material.CAMPFIRE,
            "A turfa solta fumaça: cega e enfraquece quem estiver perto."),
    BRASAS_ESPALHADAS(TipoArma.CLAVA, 5, "Brasas Espalhadas", 20, 6, Material.FIRE_CHARGE,
            "Um leque de brasas que incendeia todos na sua frente."),
    GOLPE_SISMICO(TipoArma.CLAVA, 8, "Golpe Sísmico", 20, 8, Material.COARSE_DIRT,
            "Bate no chão e joga os inimigos em volta para cima."),
    CHAMA_DO_PANTANO(TipoArma.CLAVA, 12, "Chama do Pântano", 30, 12, Material.MUD,
            "Uma trilha de fogo de 9 blocos à sua frente."),
    FORNALHA_VIVA(TipoArma.CLAVA, 16, "Fornalha Viva", 30, 14, Material.BLAST_FURNACE,
            "Por 5 s: resistência ao fogo e um anel de brasas queima quem estiver perto."),
    ERUPCAO(TipoArma.CLAVA, 20, "Erupção", 50, 25, Material.MAGMA_BLOCK,
            "Salta e cai como um vulcão: explosão de fogo em 5 blocos."),

    // ---------------- 2.56: Lança de Basalto (minério do Nether) ----------------
    ESTOCADA_DE_MAGMA(TipoArma.LANCA_BASALTO, 1, "Estocada de Magma", 12, 3, Material.MAGMA_CREAM,
            "Estocada longa (5 blocos) que põe fogo no primeiro inimigo."),
    MURALHA_DE_BASALTO(TipoArma.LANCA_BASALTO, 3, "Muralha de Basalto", 15, 8, Material.POLISHED_BASALT,
            "Finca a lança: empurra quem está à frente e dá Resistência por 3 s."),
    CHUVA_DE_CINZAS(TipoArma.LANCA_BASALTO, 5, "Chuva de Cinzas", 20, 8, Material.GRAY_DYE,
            "Gira a lança: cinzas cegam e queimam quem estiver em volta."),
    SALTO_DO_STRIDER(TipoArma.LANCA_BASALTO, 8, "Salto do Strider", 20, 8, Material.WARPED_FUNGUS_ON_A_STICK,
            "Salta para a frente e cai com a ponta: dano e fogo onde pousa."),
    RIO_DE_LAVA(TipoArma.LANCA_BASALTO, 12, "Rio de Lava", 30, 12, Material.LAVA_BUCKET,
            "Arrasta a lança no chão: uma linha de fogo de 10 blocos."),
    PELE_DE_OBSIDIANA(TipoArma.LANCA_BASALTO, 16, "Pele de Obsidiana", 30, 16, Material.OBSIDIAN,
            "Por 6 s: Resistência II e Resistência ao Fogo; quem te bate pega fogo."),
    LANCA_DO_INFERNO(TipoArma.LANCA_BASALTO, 20, "Lança do Inferno", 50, 25, Material.BLAZE_ROD,
            "Arremessa a lança em chamas: atravessa 15 blocos e explode no fim."),

    // ---------------- 2.54: técnicas secretas (uma por arma, ensinadas pelos Mestres de Armas nas vilas) ----------------
    VENTO_CORTANTE(TipoArma.ESPADA, "Vento Cortante", 35, 14, Material.WIND_CHARGE,
            "Técnica secreta: um corte que vira uma lâmina de vento de 12 blocos.", true),
    MACHADOS_ANCESTRAIS(TipoArma.MACHADO, "Machados Ancestrais", 40, 18, Material.GOLDEN_AXE,
            "Técnica secreta: três machados espectrais giram em volta de você por 3 s.", true),
    MIL_ESTOCADAS(TipoArma.LANCA, "Mil Estocadas", 35, 14, Material.POINTED_DRIPSTONE,
            "Técnica secreta: oito estocadas rapidíssimas em linha.", true),
    MAREMOTO(TipoArma.TRIDENTE, "Maremoto", 40, 18, Material.WATER_BUCKET,
            "Técnica secreta: uma onda gigante varre 12 blocos à sua frente.", true),
    SINO_DO_JUIZO(TipoArma.MACA, "Sino do Juízo", 40, 18, Material.BELL,
            "Técnica secreta: o impacto atordoa todos num raio de 6 blocos.", true),
    FLECHA_COMETA(TipoArma.ARCO, "Flecha Cometa", 35, 16, Material.FIRE_CHARGE,
            "Técnica secreta: três flechas em chamas caem onde você mira e explodem.", true),
    DANCA_DAS_SOMBRAS(TipoArma.ADAGA, "Dança das Sombras", 35, 16, Material.ECHO_SHARD,
            "Técnica secreta: salta para as costas de até 3 inimigos, cortando cada um.", true),
    TEMPESTADE_ESCARLATE(TipoArma.DUPLAS, "Tempestade Escarlate", 40, 18, Material.REDSTONE,
            "Técnica secreta: doze cortes em 2 s em volta, que fazem sangrar.", true),
    MIL_LAMINAS(TipoArma.ARREMESSO, "Mil Lâminas", 40, 18, Material.IRON_NUGGET,
            "Técnica secreta: trinta lâminas em leque na sua frente.", true),
    LUA_DE_SANGUE(TipoArma.FOICE, "Lua de Sangue", 40, 18, Material.RED_DYE,
            "Técnica secreta: corte em círculo de 6 blocos que cura parte do dano.", true),
    MONTANHA_INABALAVEL(TipoArma.BASTAO, "Montanha Inabalável", 35, 20, Material.STONE,
            "Técnica secreta: 6 s de Resistência III e quem chegar perto é jogado longe.", true),
    SERPENTE_RELAMPAGO(TipoArma.CHICOTE, "Serpente-Relâmpago", 40, 16, Material.LIGHTNING_ROD,
            "Técnica secreta: um raio corre pelo chicote e salta por até 5 inimigos.", true),
    CEU_PARTIDO(TipoArma.MONTANTE, "Céu Partido", 45, 18, Material.NETHERITE_SWORD,
            "Técnica secreta: um corte de cima a baixo que racha 12 blocos à frente.", true),
    ORBITA_ETERNA(TipoArma.BUMERANGUE, "Órbita Eterna", 35, 16, Material.CLOCK,
            "Técnica secreta: o bumerangue gira em volta de você por 3 s.", true),
    VENDAVAL_DE_PETALAS(TipoArma.LEQUES, "Vendaval de Pétalas", 35, 14, Material.PINK_PETALS,
            "Técnica secreta: um vendaval de 8 blocos que corta e empurra.", true),
    FISGADA_DO_LEVIATA(TipoArma.ARPAO, "Fisgada do Leviatã", 40, 16, Material.FISHING_ROD,
            "Técnica secreta: fisga todos numa linha de 15 blocos e puxa até você.", true),
    NEVASCA_DE_FERRO(TipoArma.MANGUAL, "Nevasca de Ferro", 40, 18, Material.PACKED_ICE,
            "Técnica secreta: gira o mangual por 3 s, congelando quem estiver perto.", true),
    FRENESI_DA_FERA(TipoArma.GARRAS, "Frenesi da Fera", 40, 16, Material.BONE,
            "Técnica secreta: dez garradas rápidas no alvo, roubando vida.", true),
    ERUPCAO_VULCANICA(TipoArma.CLAVA, "Erupção Vulcânica", 45, 18, Material.MAGMA_BLOCK,
            "Técnica secreta: bate no chão e pilares de fogo sobem em volta.", true),
    CORACAO_DO_VULCAO(TipoArma.LANCA_BASALTO, "Coração do Vulcão", 45, 20, Material.RESPAWN_ANCHOR,
            "Técnica secreta: finca a lança e o chão vira magma em 6 blocos por 4 s, queimando e prendendo quem pisa.", true),

    // ---------------- Lendas (com a lenda na mão; armaduras e a picareta: carregando) ----------------
    PURIFICACAO_SAGRADA(Lenda.LAMINA_DO_EXORCISTA, "Purificação Sagrada", 40, 18, Material.TOTEM_OF_UNDYING,
            "Explosão de luz em volta: dano enorme em mortos-vivos e cura os aliados."),
    TEIA_DA_RAINHA(Lenda.PRESA_DA_TECELA, "Teia da Rainha", 35, 16, Material.COBWEB,
            "Cone de teias que prende e envenena os inimigos."),
    QUEDA_DA_FLORESTA(Lenda.MACHADO_DO_LENHADOR, "Queda da Floresta", 40, 18, Material.OAK_LOG,
            "Um tronco gigante cai numa linha de 9 blocos à sua frente."),
    COLAPSO_DO_ABISMO(Lenda.PICARETA_DO_ABISMO, "Colapso do Abismo", 40, 18, Material.DEEPSLATE,
            "O chão em volta afunda: puxa os inimigos para baixo e os prende."),
    TEMPESTADE_DE_RAIOS(Lenda.ARCO_DA_TEMPESTADE, "Tempestade de Raios", 45, 20, Material.LIGHTNING_ROD,
            "Seis raios caem onde você olha."),
    VAZIO_DO_FIM(Lenda.COROA_DO_FIM, "Vazio do Fim", 45, 20, Material.END_CRYSTAL,
            "Puxa todos os inimigos perto para um ponto e explode."),
    EGIDE(Lenda.EGIDE_DO_GUARDIAO, "Égide", 40, 25, Material.SHIELD,
            "Por 6 s: Absorção IV e quem te bater leva metade do dano de volta."),
    PASSO_DO_ANDARILHO(Lenda.BOTAS_DO_ANDARILHO, "Passo do Andarilho", 30, 12, Material.NETHERITE_BOOTS,
            "Avança 12 blocos atravessando inimigos e ganha Velocidade III."),
    SENTENCA(Lenda.LAMINA_DO_CARRASCO, "Sentença", 40, 20, Material.WITHER_ROSE,
            "Executa quem estiver com menos de 25% de vida (chefes levam dano x5)."),
    BIGORNA_CELESTE(Lenda.MARTELO_DO_FORJADOR, "Bigorna Celeste", 40, 18, Material.ANVIL,
            "Uma bigorna de luz cai no alvo e atordoa quem estiver perto."),
    LAMINA_ARCANA_SUPREMA(Lenda.LAMINA_DO_ARQUIMAGO, "Lâmina Arcana Suprema", 35, 16, Material.AMETHYST_CLUSTER,
            "Cinco projéteis arcanos perseguem os inimigos; devolve mana."),
    CHAMADO_DA_ALCATEIA(Lenda.ELMO_DAS_FERAS, "Chamado da Alcateia", 45, 30, Material.BONE,
            "Três lobos espectrais lutam ao seu lado por 20 s."),

    // ---------------- Classes (com a classe ativa, ou uma lendária que nasce dela) ----------------
    FURIA_SANGRENTA(Classe.BERSERKER, TipoArma.MACHADO, "Fúria Sangrenta", 30, 12, Material.REDSTONE,
            "Paga 4 de vida: giro devastador em volta que rouba vida."),
    CARGA_DO_CAVALEIRO(Classe.CAVALEIRO, TipoArma.LANCA, "Carga do Cavaleiro", 30, 10, Material.IRON_HORSE_ARMOR,
            "Investida de 10 blocos que arremessa os inimigos (o dobro montado)."),
    MARTELO_SAGRADO(Classe.PALADINO, TipoArma.MACA, "Martelo Sagrado", 35, 14, Material.GOLDEN_AXE,
            "Golpe de luz em área: mais forte em mortos-vivos e cura os aliados."),
    TIRO_NA_CABECA(Classe.ATIRADOR, TipoArma.ARCO, "Tiro na Cabeça", 30, 10, Material.TARGET,
            "Uma flecha rapidíssima com dano enorme."),
    FLECHAS_DA_MATILHA(Classe.CACADOR, TipoArma.ARCO, "Flechas da Matilha", 30, 12, Material.LEAD,
            "Três flechas que marcam o alvo; seus companheiros atacam ele."),
    PRISMA_ELEMENTAL(Classe.ELEMENTALISTA, TipoArma.TRIDENTE, "Prisma Elemental", 35, 12, Material.PRISMARINE_CRYSTALS,
            "Cone de fogo, gelo e raio ao mesmo tempo."),
    LAMINA_RUNICA(Classe.FEITICEIRO, TipoArma.ESPADA, "Lâmina Rúnica", 30, 10, Material.ENCHANTED_BOOK,
            "Três cortes arcanos em sequência que devolvem mana."),
    MIL_CORTES(Classe.ASSASSINO, TipoArma.ESPADA, "Mil Cortes", 35, 14, Material.IRON_SWORD,
            "Salta entre até 5 inimigos perto, cortando cada um."),
    MARTELO_INCANDESCENTE(Classe.MESTRE_FORJADOR, TipoArma.MACA, "Martelo Incandescente", 30, 10, Material.MAGMA_BLOCK,
            "Golpe em brasa: incendeia e quebra a defesa do alvo."),
    INVESTIDA_DA_FERA(Classe.MESTRE_DAS_FERAS, TipoArma.LANCA, "Investida da Fera", 30, 12, Material.SADDLE,
            "Seus companheiros saltam no alvo junto com a sua estocada.");

    private final TipoArma tipo;
    private final int nivel;
    private final String nome;
    private final double vigor;
    private final int recarga;
    private final Material icone;
    private final String descricao;
    private final Lenda lenda;
    private final Classe classe;
    /** 2.54: técnica secreta (ensinada por um Mestre de Armas nas vilas). */
    private final boolean tecnica;

    Golpe(TipoArma tipo, int nivel, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(tipo, nivel, nome, vigor, recarga, icone, descricao, null, null, false);
    }

    /** Golpe de lenda: vale com qualquer arma de combo, se você tiver a lenda (veja {@link Combos#disponivel}). */
    Golpe(Lenda lenda, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(null, 1, nome, vigor, recarga, icone, descricao, lenda, null, false);
    }

    /** Golpe de classe: vale com a arma da classe enquanto ela (ou uma lendária que nasce dela) estiver ativa. */
    Golpe(Classe classe, TipoArma tipo, String nome, double vigor, int recarga, Material icone, String descricao) {
        this(tipo, 1, nome, vigor, recarga, icone, descricao, null, classe, false);
    }

    /** Técnica secreta: vale com a arma dela depois que um Mestre de Armas ensina. */
    Golpe(TipoArma tipo, String nome, double vigor, int recarga, Material icone, String descricao, boolean tecnica) {
        this(tipo, 1, nome, vigor, recarga, icone, descricao, null, null, tecnica);
    }

    Golpe(TipoArma tipo, int nivel, String nome, double vigor, int recarga, Material icone, String descricao, Lenda lenda, Classe classe, boolean tecnica) {
        this.tipo = tipo;
        this.nivel = nivel;
        this.nome = nome;
        this.vigor = vigor;
        this.recarga = recarga;
        this.icone = icone;
        this.descricao = descricao;
        this.lenda = lenda;
        this.classe = classe;
        this.tecnica = tecnica;
    }

    /** A arma do golpe (null nos golpes de lenda: valem com qualquer arma). */
    public TipoArma tipo() { return tipo; }
    public Lenda lenda() { return lenda; }
    public Classe classe() { return classe; }
    /** Golpe de lenda ou de classe (não sobe com a proficiência). */
    public boolean extra() { return lenda != null || classe != null || tecnica; }
    public boolean tecnica() { return tecnica; }
    /** Nível de proficiência da arma que libera o golpe. */
    public int nivel() { return nivel; }
    public String nome() { return nome; }
    public double vigor() { return vigor; }
    /** Recarga em segundos. */
    public int recarga() { return recarga; }
    public Material icone() { return icone; }
    public String descricao() { return descricao; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Os 7 golpes normais da arma (os que sobem com a proficiência). */
    public static List<Golpe> da(TipoArma t) {
        List<Golpe> l = new ArrayList<>();
        for (Golpe g : values()) if (g.tipo == t && !g.extra()) l.add(g);
        return l;
    }

    /** Golpes de lenda e de classe que podem ir nas sequências dessa arma. */
    public static List<Golpe> extras(TipoArma t) {
        List<Golpe> l = new ArrayList<>();
        for (Golpe g : values()) if (g.extra() && (g.tipo == null || g.tipo == t)) l.add(g);
        return l;
    }

    public static Golpe porId(String id) {
        for (Golpe g : values()) if (g.id().equalsIgnoreCase(id)) return g;
        return null;
    }
}
