package br.rpgatributos;

import br.rpgatributos.forja.Raridade;
import br.rpgatributos.forja.Tier;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Valores do config.yml já lidos, para não consultar o arquivo a cada evento. */
public final class Settings {

    public final int nivelMaximo;
    public final double xpBase;
    public final double xpExpoente;
    public final double multiplicadorXp;
    public final boolean actionBar;

    public final double minXpPedra, minXpMinerio, minVelocidade, minChanceDupla;
    public final double madXpTronco, madVelocidade, madChanceDupla;
    public final double corXpPorBloco, corVelocidade;
    public final double puloXpPulo, puloXpQueda, puloForca, puloQuedaSegura, puloReducaoQueda, puloReducaoMax;
    public final double comXpMonstro, comXpAnimal, comXpJogador, comMultSpawner, comDano, comVida;

    public final double natXpPorBloco, natAgilidade, natFolego, natMineracao;
    public final double agrChanceDupla, agrToqueVerde, agrChanceGigante, agrChanceMutacao;
    public final double culDuracaoExtra;

    // pesca e alquimia
    public final double pesXpPeixe, pesXpTesouro, pesXpLixo, pesIscaRapida, pesChanceDupla;
    public final double pesChanceRaroBase, pesChanceRaroMax, pesChanceTesouroBase, pesChanceTesouroMax;
    public final double pesChanceCriaturaBase, pesChanceCriaturaMax;
    public final double alqDuracaoExtra, alqChanceExtra, alqXpPocaoComum;

    // reinos e guerra
    public final int reiCustoFundar, reiNivelColonia, gueHora, gueDuracaoMin, gueAvisoHoras, gueCapturaSeg, gueChunksPorCaptura, gueTreguaDias;
    public final java.time.DayOfWeek gueDia;

    // mundo perigoso
    public final double perEliteChance, perEliteChanceSemana, perAumentoSemana, perAumentoMax, perNinhoChance, perHordaChance;
    public final int perNinhoMax, perChefePorSemana, perChefeMinJogadores;

    // mundo vivo
    public final boolean temperaturaAtiva, invernoMudaBlocos;
    public final int invernoNeveMax;
    public final double cliChanceEvento, ceuChanceSangue, ceuChanceMeteoros, ceuChanceEclipse, ceuChanceAurora, malChanceMordida;
    public final double gueTesouroVencedor;

    // portais, sombras e torre (Fase C)
    public final int porIntervaloMin, porMaximo, porDistMin, porDistMax;
    public final double porChance, porHorasTransbordar, porChanceEco;
    public final int[] porPesos;
    public final double torVidaPorAndar, torDanoPorAndar;

    // combos de arma
    public final boolean comAtivo;
    public final double comVigorMaximo, comVigorPorSegundo, comVigorForaDeCombate, comXpBase, comXpExpoente;
    public final double comXpAcerto, comXpAbate, comXpChefe, comDanoPorNivel;
    public final long comJanelaMs;

    // mobilidade, mochila e criaturas
    public final boolean mobEsquivaAtiva, mochilaAtiva;
    public final double mobEsquivaVigor, mobGanchoAlcance;
    public final long mobEsquivaRecargaMs, mobEsquivaJanelaMs;
    public final int mochilaNiveisPorFileira, criLimiteMonstros, criLimiteAnimais;

    // pacote de recursos do plugin
    public final boolean pacAtivado, pacObrigatorio;
    public final String pacUrl, pacEndereco;
    public final int pacPorta;

    // progressão extra: talentos, renascer e evolução dos companheiros
    public final boolean talAtivado, renAtivado, evoAtivada;
    public final int talNiveisPorPonto, talCustoRefazer, renNivel, renMaximo, renPontosTalento;
    public final double renXp, renVida, renDano, renMana;

    // detalhes: estações pequenas, natureza e túmulos
    public final boolean detSentar, detLapide;
    public final int detDescansadoMinutos, detAmolarGolpes, detLapideMinutos;
    public final double detNinhoChance, detAchadoChance, detAnimalRaroChance, detDescansadoXp, detBonecoXpHora, detAmolarDano;

    // vida no mundo: ofícios, cartas, mercador e encontros
    public final boolean vidaEncontros;
    public final int vidaCanteiroMinutos, vidaColmeiaMinutos, vidaMercadorMinutos, vidaMercadorDuracao, vidaEncontroMinutos;
    public final int[] vidaBarrilDias;
    public final double vidaCartaChance;

    // doma (companheiros)
    public final double domaXpDomar, domaXpCruzar, domaXpVincular, domaXpComponente, domaXpPorBloco, domaXpAbate;
    public final double domaVida, domaDano, domaVelocidade;
    private final int domaCompanheirosMax, domaComponentesMax;

    // classes
    public final double claNivelTotalBasica, claAtributoBasica, claAtributoAvancada, claXpBase, claXpExpoente, claMultAvancada;
    public final int claCustoTroca, claRecargaProvaMin, claDiasInativoLendaria;

    // viagem e locais ocultos
    public final int viaBlocosPorNivel, viaNiveisOutroMundo, viaEsperaSeg;
    public final int locPorTipo, locResetMin;
    public final double locDistMin, locDistMax;

    // masmorras
    public final int masMaximo, masTempoLimiteMin, masCustoCofre;
    public final double masChanceSalaPersonalizada;

    // party e territórios
    public final int partyTamanhoMax;
    public final double partyRaioXp, partyXpCompartilhado, partyBonusPorMembro, partyBonusMax;
    public final boolean partyAvisoVida;
    public final int terChunksIniciais, terNiveisPorChunk, terChunksMax, terMembrosMax;
    public final boolean terExigirVizinho;
    public final List<String> terMundosBloqueados;

    public final boolean tagMostrarNivel;
    public final int tagTamanhoMax;
    public final double tagAltura;

    // anti-farm
    public final double puloDistanciaMinima;
    public final long comCooldownMesmoJogadorMs;
    public final boolean comSemXpMesmoIp;

    // ferraria
    public final Map<Tier, Double> ferXpPorMaterial = new EnumMap<>(Tier.class);
    public final double ferXpNetherite, ferXpLingote;

    // forja
    public final boolean forjaAtivada;
    public final boolean forjaExigeEstacao;
    public final boolean guiaAoEntrar;
    public final Raridade forjaAnunciar;
    public final boolean forjaRaioMitico;
    public final Map<Raridade, Double> forjaPesoInicial = new EnumMap<>(Raridade.class);
    public final Map<Raridade, Double> forjaPesoFinal = new EnumMap<>(Raridade.class);
    public final Map<Raridade, Double> forjaMultiplicador = new EnumMap<>(Raridade.class);
    public final double forjaEsquivaMax, forjaEspinhosMax;
    public final int forjaIntervaloAtaque, forjaIntervaloDefesa;

    // arcano (infusão e magias)
    public final double arcManaBase, arcManaPorNivel, arcRegenBase, arcRegenPorNivel, arcCustoBase;
    public final int arcCargaBase, arcSobrecargaMax, arcInstabilidadeSeg;
    public final double arcCargaPorNivel, arcChanceBase, arcChancePorNivel, arcDanoPvp;
    public final double arcXpInfusao, arcXpMagia, arcXpDescoberta;
    public final boolean arcAnunciarDescoberta;

    /** Versão do config.yml. Configs mais antigos são trocados (com backup) ao ligar o servidor. */
    public static final int VERSAO_CONFIG = 2;

    public Settings(FileConfiguration c) {
        nivelMaximo = Math.max(1, c.getInt("nivel-maximo", 100));
        xpBase = c.getDouble("xp-base", 20);
        xpExpoente = c.getDouble("xp-expoente", 1.3);
        multiplicadorXp = c.getDouble("multiplicador-xp", 1.0);
        actionBar = c.getBoolean("mostrar-xp-na-actionbar", true);

        // Os bônus no config são "quanto vale no nível máximo"; aqui viram "por nível".
        // Assim, mudar o nível máximo não deixa ninguém forte demais.
        double n = nivelMaximo;
        minXpPedra = c.getDouble("mineracao.xp-pedra", 1);
        minXpMinerio = c.getDouble("mineracao.xp-minerio", 6);
        minVelocidade = c.getDouble("mineracao.velocidade-maxima", 1.0) / n;
        minChanceDupla = c.getDouble("mineracao.chance-dupla-maxima", 0.5) / n;

        madXpTronco = c.getDouble("madeira.xp-tronco", 2);
        madVelocidade = c.getDouble("madeira.velocidade-maxima", 1.0) / n;
        madChanceDupla = c.getDouble("madeira.chance-dupla-maxima", 0.5) / n;

        corXpPorBloco = c.getDouble("corrida.xp-por-bloco", 0.1);
        corVelocidade = c.getDouble("corrida.velocidade-maxima", 0.2) / n;

        puloXpPulo = c.getDouble("pulo.xp-por-pulo", 0.5);
        puloXpQueda = c.getDouble("pulo.xp-por-dano-de-queda", 1.0);
        puloForca = c.getDouble("pulo.pulo-maximo", 0.3) / n;
        puloQuedaSegura = c.getDouble("pulo.queda-segura-maxima", 5) / n;
        puloReducaoMax = c.getDouble("pulo.reducao-dano-queda-maxima", 0.5);
        puloReducaoQueda = puloReducaoMax / n;

        comXpMonstro = c.getDouble("combate.xp-monstro", 8);
        comXpAnimal = c.getDouble("combate.xp-animal", 2);
        comXpJogador = c.getDouble("combate.xp-jogador", 15);
        comMultSpawner = c.getDouble("combate.multiplicador-mob-de-spawner", 0.25);
        comDano = c.getDouble("combate.dano-maximo", 3) / n;
        comVida = c.getDouble("combate.vida-maxima", 10) / n;

        natXpPorBloco = c.getDouble("natacao.xp-por-bloco", 0.15);
        natAgilidade = c.getDouble("natacao.agilidade-maxima", 0.5) / n;
        natFolego = c.getDouble("natacao.folego-maximo", 3) / n;
        natMineracao = c.getDouble("natacao.mineracao-submersa-maxima", 0.6) / n;

        agrChanceDupla = c.getDouble("agricultura.chance-dupla-maxima", 0.5) / n;
        agrToqueVerde = c.getDouble("agricultura.toque-verde-maximo", 0.3) / n;
        agrChanceGigante = c.getDouble("agricultura.chance-gigante-maxima", 0.03);
        agrChanceMutacao = c.getDouble("agricultura.chance-mutacao", 0.03);

        culDuracaoExtra = c.getDouble("culinaria.duracao-extra-maxima", 0.5) / n;

        pesXpPeixe = c.getDouble("pesca.xp-peixe", 8);
        pesXpTesouro = c.getDouble("pesca.xp-tesouro", 25);
        pesXpLixo = c.getDouble("pesca.xp-lixo", 3);
        pesIscaRapida = Math.min(0.9, c.getDouble("pesca.isca-mais-rapida-maxima", 0.5)) / n;
        pesChanceDupla = c.getDouble("pesca.chance-dupla-maxima", 0.3) / n;
        pesChanceRaroBase = c.getDouble("pesca.chance-peixe-raro-inicial", 0.03);
        pesChanceRaroMax = c.getDouble("pesca.chance-peixe-raro-maxima", 0.15);
        pesChanceTesouroBase = c.getDouble("pesca.chance-tesouro-inicial", 0.01);
        pesChanceTesouroMax = c.getDouble("pesca.chance-tesouro-maxima", 0.06);
        pesChanceCriaturaBase = c.getDouble("pesca.chance-criatura-inicial", 0.01);
        pesChanceCriaturaMax = c.getDouble("pesca.chance-criatura-maxima", 0.05);

        alqDuracaoExtra = c.getDouble("alquimia.duracao-extra-maxima", 0.6) / n;
        alqChanceExtra = c.getDouble("alquimia.chance-rendimento-extra-maxima", 0.4) / n;
        alqXpPocaoComum = c.getDouble("alquimia.xp-por-pocao-no-suporte", 3);

        reiCustoFundar = Math.max(0, c.getInt("reino.esmeraldas-para-fundar", 32));
        reiNivelColonia = Math.max(1, c.getInt("reino.nivel-minimo-da-colonia", 2));
        java.time.DayOfWeek dia;
        try { dia = java.time.DayOfWeek.valueOf(c.getString("guerra.dia-da-semana", "SATURDAY").toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { dia = java.time.DayOfWeek.SATURDAY; }
        gueDia = dia;
        gueHora = Math.max(0, Math.min(23, c.getInt("guerra.hora", 20)));
        gueDuracaoMin = Math.max(10, c.getInt("guerra.duracao-minutos", 60));
        gueAvisoHoras = Math.max(0, c.getInt("guerra.aviso-minimo-horas", 24));
        gueCapturaSeg = Math.max(10, c.getInt("guerra.segundos-para-capturar-marco", 120));
        gueChunksPorCaptura = Math.max(0, c.getInt("guerra.chunks-por-captura", 4));
        gueTesouroVencedor = Math.max(0, Math.min(1, c.getDouble("guerra.parte-do-tesouro-para-o-vencedor", 0.3)));
        gueTreguaDias = Math.max(0, c.getInt("guerra.dias-de-tregua", 7));

        perEliteChance = c.getDouble("perigo.chance-elite", 0.025);
        perEliteChanceSemana = c.getDouble("perigo.chance-elite-a-mais-por-semana", 0.004);
        perAumentoSemana = c.getDouble("perigo.forca-a-mais-por-semana", 0.05);
        perAumentoMax = c.getDouble("perigo.forca-a-mais-maxima", 0.5);
        perNinhoChance = c.getDouble("perigo.chance-ninho", 0.12);
        perNinhoMax = Math.max(0, c.getInt("perigo.ninhos-no-maximo", 8));
        perHordaChance = c.getDouble("perigo.chance-horda-por-noite", 0.2);
        perChefePorSemana = Math.max(0, c.getInt("perigo.chefes-mundiais-por-semana", 2));
        perChefeMinJogadores = Math.max(1, c.getInt("perigo.chefe-mundial-minimo-de-jogadores", 2));

        temperaturaAtiva = c.getBoolean("mundo-vivo.temperatura", true);
        invernoMudaBlocos = c.getBoolean("mundo-vivo.neve-e-gelo-no-inverno", true);
        invernoNeveMax = Math.max(0, c.getInt("mundo-vivo.blocos-de-neve-no-maximo", 4000));
        cliChanceEvento = c.getDouble("mundo-vivo.chance-evento-de-clima", 0.08);
        ceuChanceSangue = c.getDouble("mundo-vivo.chance-lua-de-sangue", 0.2);
        ceuChanceMeteoros = c.getDouble("mundo-vivo.chance-chuva-de-meteoros", 0.06);
        ceuChanceEclipse = c.getDouble("mundo-vivo.chance-eclipse", 0.02);
        ceuChanceAurora = c.getDouble("mundo-vivo.chance-aurora-no-inverno", 0.25);
        malChanceMordida = c.getDouble("mundo-vivo.chance-mordida-amaldicoar", 0.2);

        porIntervaloMin = Math.max(5, c.getInt("portais.intervalo-minutos", 30));
        porChance = c.getDouble("portais.chance", 0.6);
        porMaximo = Math.max(0, c.getInt("portais.no-maximo", 3));
        porDistMin = Math.max(16, c.getInt("portais.distancia-minima", 48));
        porDistMax = Math.max(porDistMin + 8, c.getInt("portais.distancia-maxima", 96));
        porHorasTransbordar = Math.max(0.1, c.getDouble("portais.horas-ate-transbordar", 2.0));
        porChanceEco = c.getDouble("portais.chance-eco-do-soberano", 0.35);
        porPesos = new int[]{Math.max(0, c.getInt("portais.peso-facil", 45)), Math.max(0, c.getInt("portais.peso-normal", 33)),
                Math.max(0, c.getInt("portais.peso-dificil", 17)), Math.max(0, c.getInt("portais.peso-pesadelo", 5))};
        torVidaPorAndar = Math.max(0, c.getDouble("torre.vida-por-andar", 0.07));
        torDanoPorAndar = Math.max(0, c.getDouble("torre.dano-por-andar", 0.035));

        comAtivo = c.getBoolean("combos.ativado", true);
        comVigorMaximo = Math.max(10, c.getDouble("combos.vigor-maximo", 100));
        comVigorPorSegundo = Math.max(0, c.getDouble("combos.vigor-por-segundo-em-luta", 6));
        comVigorForaDeCombate = Math.max(0, c.getDouble("combos.vigor-por-segundo-fora-de-luta", 15));
        comJanelaMs = Math.max(300, c.getLong("combos.milissegundos-entre-cliques", 900));
        comXpBase = Math.max(1, c.getDouble("combos.xp-base", 30));
        comXpExpoente = Math.max(1, c.getDouble("combos.xp-expoente", 1.5));
        comXpAcerto = Math.max(0, c.getDouble("combos.xp-por-acerto", 1));
        comXpAbate = Math.max(0, c.getDouble("combos.xp-por-abate", 6));
        comXpChefe = Math.max(0, c.getDouble("combos.xp-por-chefe", 40));
        comDanoPorNivel = Math.max(0, c.getDouble("combos.dano-a-mais-por-nivel", 0.02));

        mobEsquivaAtiva = c.getBoolean("mobilidade.esquiva-ativada", true);
        mobEsquivaVigor = Math.max(0, c.getDouble("mobilidade.vigor-da-esquiva", 15));
        mobEsquivaRecargaMs = Math.max(0, c.getLong("mobilidade.recarga-da-esquiva-ms", 1200));
        mobEsquivaJanelaMs = Math.max(100, c.getLong("mobilidade.tempo-entre-toques-ms", 300));
        mobGanchoAlcance = Math.max(5, c.getDouble("mobilidade.alcance-do-gancho", 40));
        mochilaAtiva = c.getBoolean("mochila.ativada", true);
        mochilaNiveisPorFileira = Math.max(1, c.getInt("mochila.niveis-por-fileira", 150));
        criLimiteMonstros = c.getInt("criaturas.limite-de-monstros", 45);
        criLimiteAnimais = c.getInt("criaturas.limite-de-animais", -1);
        pacAtivado = c.getBoolean("pacote-de-recursos.ativado", true);
        pacUrl = c.getString("pacote-de-recursos.url", "");
        pacPorta = Math.max(0, c.getInt("pacote-de-recursos.porta", 0));
        pacEndereco = c.getString("pacote-de-recursos.endereco", "");
        pacObrigatorio = c.getBoolean("pacote-de-recursos.obrigatorio", false);

        talAtivado = c.getBoolean("talentos.ativado", true);
        talNiveisPorPonto = Math.max(1, c.getInt("talentos.niveis-por-ponto", 25));
        talCustoRefazer = Math.max(0, c.getInt("talentos.esmeraldas-para-refazer", 16));
        renAtivado = c.getBoolean("renascer.ativado", true);
        renNivel = Math.max(1, c.getInt("renascer.nivel-exigido", 100));
        renMaximo = Math.max(0, c.getInt("renascer.maximo", 5));
        renPontosTalento = Math.max(0, c.getInt("renascer.pontos-de-talento", 8));
        renXp = Math.max(0, c.getDouble("renascer.xp-extra", 0.10));
        renVida = Math.max(0, c.getDouble("renascer.vida-extra", 2));
        renDano = Math.max(0, c.getDouble("renascer.dano-extra", 0.02));
        renMana = Math.max(0, c.getDouble("renascer.mana-extra", 10));
        evoAtivada = c.getBoolean("doma.evolucao-dos-companheiros", true);

        detSentar = c.getBoolean("detalhes.sentar", true);
        detLapide = c.getBoolean("detalhes.lapide.ativada", true);
        detLapideMinutos = Math.max(0, c.getInt("detalhes.lapide.minutos-de-protecao", 15));
        detNinhoChance = Math.max(0, c.getDouble("detalhes.chance-de-ninho", 0.01));
        detAchadoChance = Math.max(0, c.getDouble("detalhes.chance-de-achado", 0.015));
        detAnimalRaroChance = Math.max(0, c.getDouble("detalhes.chance-de-animal-raro", 0.002));
        detDescansadoXp = Math.max(0, c.getDouble("detalhes.fogueira.xp-descansado", 0.05));
        detDescansadoMinutos = Math.max(1, c.getInt("detalhes.fogueira.minutos-descansado", 20));
        detBonecoXpHora = Math.max(0, c.getDouble("detalhes.boneco.xp-por-hora", 120));
        detAmolarGolpes = Math.max(1, c.getInt("detalhes.amolar.golpes", 100));
        detAmolarDano = Math.max(0, c.getDouble("detalhes.amolar.dano-extra", 0.10));

        vidaCanteiroMinutos = Math.max(1, c.getInt("vida.canteiro-minutos", 10));
        vidaColmeiaMinutos = Math.max(1, c.getInt("vida.colmeia-minutos", 5));
        vidaBarrilDias = new int[]{Math.max(0, c.getInt("vida.barril.dias-envelhecida", 1)), Math.max(0, c.getInt("vida.barril.dias-reserva", 3)),
                Math.max(0, c.getInt("vida.barril.dias-lendaria", 7))};
        vidaCartaChance = Math.max(0, c.getDouble("vida.chance-de-cartas", 1.0));
        vidaMercadorMinutos = Math.max(10, c.getInt("vida.mercador.intervalo-minutos", 90));
        vidaMercadorDuracao = Math.max(5, c.getInt("vida.mercador.duracao-minutos", 20));
        vidaEncontros = c.getBoolean("vida.encontros.ativados", true);
        vidaEncontroMinutos = Math.max(5, c.getInt("vida.encontros.espera-minutos", 30));

        domaXpDomar = c.getDouble("doma.xp-domar", 25);
        domaXpCruzar = c.getDouble("doma.xp-cruzar", 2);
        domaXpVincular = c.getDouble("doma.xp-vincular", 30);
        domaXpComponente = c.getDouble("doma.xp-componente", 15);
        domaXpPorBloco = c.getDouble("doma.xp-por-bloco-montado", 0.05);
        domaXpAbate = c.getDouble("doma.xp-abate-do-companheiro", 4);
        domaVida = c.getDouble("doma.vida-extra-maxima", 1.0) / n;
        domaDano = c.getDouble("doma.dano-extra-maximo", 0.5) / n;
        domaVelocidade = c.getDouble("doma.velocidade-montaria-maxima", 0.4) / n;
        domaCompanheirosMax = Math.max(1, c.getInt("doma.companheiros-no-maximo", 5));
        domaComponentesMax = Math.max(1, c.getInt("doma.componentes-no-maximo", 6));

        claNivelTotalBasica = c.getDouble("classes.nivel-total-classe-basica", 0.05);
        claAtributoBasica = c.getDouble("classes.atributo-classe-basica", 0.15);
        claAtributoAvancada = c.getDouble("classes.atributo-classe-avancada", 0.40);
        claXpBase = c.getDouble("classes.xp-base", 12);
        claXpExpoente = c.getDouble("classes.xp-expoente", 1.5);
        claMultAvancada = c.getDouble("classes.multiplicador-xp-avancada", 2.0);
        claCustoTroca = Math.max(0, c.getInt("classes.esmeraldas-para-trocar", 10));
        claRecargaProvaMin = Math.max(0, c.getInt("classes.espera-depois-de-falhar-minutos", 5));
        claDiasInativoLendaria = Math.max(0, c.getInt("classes.dias-sem-entrar-para-perder-lendaria", 30));

        viaBlocosPorNivel = Math.max(1, c.getInt("viagem.blocos-por-nivel-de-xp", 1000));
        viaNiveisOutroMundo = Math.max(0, c.getInt("viagem.niveis-para-outro-mundo", 3));
        viaEsperaSeg = Math.max(0, c.getInt("viagem.segundos-parado", 3));
        locPorTipo = Math.max(0, c.getInt("locais-ocultos.quantos-de-cada-tipo", 2));
        locDistMin = c.getDouble("locais-ocultos.distancia-minima-do-spawn", 600);
        locDistMax = Math.max(locDistMin + 100, c.getDouble("locais-ocultos.distancia-maxima-do-spawn", 4500));
        locResetMin = Math.max(1, c.getInt("locais-ocultos.minutos-para-o-selo-voltar", 30));

        masMaximo = Math.max(1, c.getInt("masmorra.maximo-ao-mesmo-tempo", 6));
        masTempoLimiteMin = Math.max(5, c.getInt("masmorra.tempo-limite-minutos", 40));
        masCustoCofre = Math.max(0, c.getInt("masmorra.diamantes-cofre-das-almas", 2));
        masChanceSalaPersonalizada = c.getDouble("masmorra.chance-sala-personalizada", 0.6);

        partyTamanhoMax = Math.max(2, c.getInt("party.tamanho-maximo", 6));
        partyRaioXp = c.getDouble("party.raio-xp-compartilhado", 32);
        partyXpCompartilhado = c.getDouble("party.xp-compartilhado", 0.5);
        partyBonusPorMembro = c.getDouble("party.bonus-por-membro-perto", 0.1);
        partyBonusMax = c.getDouble("party.bonus-maximo", 0.3);
        partyAvisoVida = c.getBoolean("party.avisar-vida-baixa", true);

        terChunksIniciais = Math.max(1, c.getInt("territorio.chunks-iniciais", 9));
        terNiveisPorChunk = Math.max(1, c.getInt("territorio.niveis-por-chunk-extra", 20));
        terChunksMax = Math.max(terChunksIniciais, c.getInt("territorio.chunks-maximo", 60));
        terMembrosMax = Math.max(0, c.getInt("territorio.membros-maximo", 10));
        terExigirVizinho = c.getBoolean("territorio.chunks-precisam-ser-vizinhos", true);
        terMundosBloqueados = List.copyOf(c.getStringList("territorio.mundos-bloqueados"));

        tagMostrarNivel = c.getBoolean("tag.mostrar-nivel", true);
        tagTamanhoMax = c.getInt("tag.tamanho-maximo", 32);
        tagAltura = c.getDouble("tag.altura-extra", 0.3);

        puloDistanciaMinima = c.getDouble("pulo.distancia-minima-entre-pulos", 2.5);
        comCooldownMesmoJogadorMs = (long) (c.getDouble("combate.cooldown-mesmo-jogador-segundos", 600) * 1000);
        comSemXpMesmoIp = c.getBoolean("combate.sem-xp-mesmo-ip", true);

        for (Tier t : Tier.values()) {
            if (t == Tier.NETHERITE) continue; // netherite vem da mesa de ferraria (xp-netherite)
            ferXpPorMaterial.put(t, c.getDouble("ferraria.xp-por-material." + t.id(), XP_MATERIAL_PADRAO.getOrDefault(t, 1.0)));
        }
        ferXpNetherite = c.getDouble("ferraria.xp-netherite", 80);
        ferXpLingote = c.getDouble("ferraria.xp-por-lingote-fundido", 0.5);

        forjaAtivada = c.getBoolean("forja.ativada", true);
        forjaExigeEstacao = c.getBoolean("forja.so-na-forja-do-ferreiro", true);
        guiaAoEntrar = c.getBoolean("guia.dar-ao-entrar-pela-primeira-vez", true);
        Raridade anunciar = Raridade.porId(c.getString("forja.anunciar-no-chat-a-partir-de", "lendario"));
        forjaAnunciar = anunciar == null ? Raridade.LENDARIO : anunciar;
        forjaRaioMitico = c.getBoolean("forja.raio-ao-forjar-mitico", true);
        for (Raridade r : Raridade.values()) {
            String base = "forja.raridades." + r.id() + ".";
            forjaPesoInicial.put(r, Math.max(0, c.getDouble(base + "peso-inicial", r.pesoInicialPadrao())));
            forjaPesoFinal.put(r, Math.max(0, c.getDouble(base + "peso-final", r.pesoFinalPadrao())));
            forjaMultiplicador.put(r, Math.max(0, c.getDouble(base + "multiplicador", r.multiplicadorPadrao())));
        }
        forjaEsquivaMax = c.getDouble("forja.esquiva-maxima", 0.35);
        forjaEspinhosMax = c.getDouble("forja.espinhos-maximo", 0.6);
        forjaIntervaloAtaque = (int) Math.round(c.getDouble("forja.intervalo-efeito-ao-acertar", 0.5) * 20);
        forjaIntervaloDefesa = (int) Math.round(c.getDouble("forja.intervalo-efeito-ao-ser-atingido", 2) * 20);

        arcManaBase = c.getDouble("arcano.mana-base", 100);
        arcManaPorNivel = c.getDouble("arcano.mana-extra-no-maximo", 150) / n;
        arcRegenBase = c.getDouble("arcano.regeneracao-base", 2);
        arcRegenPorNivel = c.getDouble("arcano.regeneracao-extra-no-maximo", 3) / n;
        arcCustoBase = c.getDouble("arcano.custo-base-magia", 10);
        arcCargaBase = c.getInt("arcano.carga-base", 6);
        arcCargaPorNivel = c.getDouble("arcano.carga-extra-no-maximo", 10) / n;
        arcSobrecargaMax = c.getInt("arcano.sobrecarga-maxima", 4);
        arcChanceBase = c.getDouble("arcano.chance-infusao-base", 0.7);
        arcChancePorNivel = (c.getDouble("arcano.chance-infusao-no-maximo", 1.0) - arcChanceBase) / n;
        arcInstabilidadeSeg = c.getInt("arcano.instabilidade-ao-morrer-segundos", 300);
        arcDanoPvp = c.getDouble("arcano.dano-em-jogadores", 0.6);
        arcXpInfusao = c.getDouble("arcano.xp-infusao", 15);
        arcXpMagia = c.getDouble("arcano.xp-por-essencia-lancada", 2);
        arcXpDescoberta = c.getDouble("arcano.xp-descoberta", 100);
        arcAnunciarDescoberta = c.getBoolean("arcano.anunciar-descoberta", true);
    }

    /** Quantos companheiros o jogador pode ter: 1 no nível 0, crescendo até o máximo do config. */
    public int domaCompanheiros(int nivel) {
        return 1 + (int) ((long) nivel * (domaCompanheirosMax - 1) / nivelMaximo);
    }

    /** Quantos componentes cabem em cada companheiro: 1 no nível 0, crescendo até o máximo do config. */
    public int domaComponentes(int nivel) {
        return 1 + (int) ((long) nivel * (domaComponentesMax - 1) / nivelMaximo);
    }

    private static final Map<Tier, Double> XP_MATERIAL_PADRAO = Map.of(
            Tier.MADEIRA, 0.5, Tier.COURO, 0.6, Tier.PEDRA, 0.7, Tier.COBRE, 1.5,
            Tier.OURO, 2.0, Tier.MALHA, 3.0, Tier.FERRO, 5.0, Tier.DIAMANTE, 15.0, Tier.ESPECIAL, 4.0);
}
