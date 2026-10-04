package br.rpgatributos;

import br.rpgatributos.alquimia.Acessorios;
import br.rpgatributos.alquimia.Alquimia;
import br.rpgatributos.arcano.Arcano;
import br.rpgatributos.aventura.Altares;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.aventura.Eventos;
import br.rpgatributos.aventura.MateriaisListener;
import br.rpgatributos.aventura.Mercadores;
import br.rpgatributos.forja.EfeitosListener;
import br.rpgatributos.forja.Forja;
import br.rpgatributos.forja.ForjaListener;
import br.rpgatributos.forja.Forjas;
import br.rpgatributos.classe.Classes;
import br.rpgatributos.colonia.Colonias;
import br.rpgatributos.colonia.Prefeituras;
import br.rpgatributos.reino.ComandoReino;
import br.rpgatributos.reino.Reinos;
import br.rpgatributos.perigo.Bestiario;
import br.rpgatributos.perigo.ChefeMundial;
import br.rpgatributos.perigo.Hordas;
import br.rpgatributos.perigo.Ninhos;
import br.rpgatributos.perigo.Perigo;
import br.rpgatributos.mundo.Ceu;
import br.rpgatributos.mundo.Estacoes;
import br.rpgatributos.mundo.Maldicoes;
import br.rpgatributos.mundo.Temperatura;
import br.rpgatributos.combo.Combos;
import br.rpgatributos.exploracao.Arqueologia;
import br.rpgatributos.exploracao.Cronista;
import br.rpgatributos.exploracao.Enciclopedia;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.fe.Astronomia;
import br.rpgatributos.fe.Deuses;
import br.rpgatributos.fe.MesasRunicas;
import br.rpgatributos.fe.Runas;
import br.rpgatributos.fe.SantuariosDivinos;
import br.rpgatributos.fe.Transmutacao;
import br.rpgatributos.combate.Mobilidade;
import br.rpgatributos.mochila.Mochila;
import br.rpgatributos.perigo.Controle;
import br.rpgatributos.portal.Portais;
import br.rpgatributos.sombra.Sombras;
import br.rpgatributos.torre.ObeliscoTorre;
import br.rpgatributos.torre.Torre;
import br.rpgatributos.classe.Provas;
import br.rpgatributos.classe.Santuarios;
import br.rpgatributos.lenda.Lendas;
import br.rpgatributos.lenda.MenuLendas;
import br.rpgatributos.oculto.Locais;
import br.rpgatributos.pesca.Pesca;
import br.rpgatributos.visual.GuardaRoupa;
import br.rpgatributos.viagem.PedrasDeViagem;
import br.rpgatributos.domador.AltaresDomador;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.domador.Evolucao;
import br.rpgatributos.detalhes.Assentos;
import br.rpgatributos.detalhes.Bebedouros;
import br.rpgatributos.detalhes.BonecosTreino;
import br.rpgatributos.detalhes.Diario;
import br.rpgatributos.detalhes.Fogueiras;
import br.rpgatributos.detalhes.ItensDetalhes;
import br.rpgatributos.detalhes.Lapides;
import br.rpgatributos.detalhes.MesasCartografo;
import br.rpgatributos.detalhes.Natureza;
import br.rpgatributos.detalhes.PedrasDeAmolar;
import br.rpgatributos.detalhes.Recordes;
import br.rpgatributos.detalhes.Trofeus;
import br.rpgatributos.forja.ForjadosEspeciais;
import br.rpgatributos.conforto.MenuRpg;
import br.rpgatributos.conforto.Placar;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Barris;
import br.rpgatributos.vida.Canteiros;
import br.rpgatributos.vida.Colmeias;
import br.rpgatributos.vida.Encontros;
import br.rpgatributos.vida.Flechas;
import br.rpgatributos.vida.Frascos;
import br.rpgatributos.vida.MercadorItinerante;
import br.rpgatributos.vida.Segredos;
import br.rpgatributos.progressao.Renascimento;
import br.rpgatributos.progressao.Talentos;
import br.rpgatributos.domador.Comportamento;
import br.rpgatributos.domador.MenusDomador;
import br.rpgatributos.masmorra.Masmorras;
import br.rpgatributos.masmorra.PortaisMasmorra;
import br.rpgatributos.fazenda.Agricultura;
import br.rpgatributos.fazenda.Cozinha;
import br.rpgatributos.forja.Reciclagem;
import br.rpgatributos.forja.Refinaria;
import br.rpgatributos.missoes.Missoes;
import br.rpgatributos.party.ComandoParty;
import br.rpgatributos.party.MenuParty;
import br.rpgatributos.party.Parties;
import br.rpgatributos.party.Party;
import br.rpgatributos.territorio.ComandoTerritorio;
import br.rpgatributos.territorio.Flag;
import br.rpgatributos.territorio.Marcos;
import br.rpgatributos.territorio.MenuTerritorio;
import br.rpgatributos.territorio.ProtecaoListener;
import br.rpgatributos.territorio.Territorios;
import br.rpgatributos.titulos.Titulos;
import org.bukkit.GameRules;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class RPGAtributos extends JavaPlugin {

    private Settings settings;
    private StatsManager stats;
    private BonusManager bonus;
    private TagManager tags;
    private SkillListener skillListener;
    private MenuManager menus;
    private Forja forja;
    private EfeitosListener efeitos;
    private Arcano arcano;
    private Forjas forjas;
    private Refinaria refinaria;
    private Guia guia;
    private Titulos titulos;
    private Chefes chefes;
    private Eventos eventos;
    private Mercadores mercadores;
    private Altares altares;
    private Missoes missoes;
    private Agricultura agricultura;
    private Cozinha cozinha;
    private Reciclagem reciclagem;
    private Parties parties;
    private MenuParty menuParty;
    private Territorios territorios;
    private Marcos marcos;
    private MenuTerritorio menusTerritorio;
    private Companheiros companheiros;
    private Comportamento comportamento;
    private AltaresDomador altaresDomador;
    private MenusDomador menusDomador;
    private Masmorras masmorras;
    private PortaisMasmorra portaisMasmorra;
    private Classes classes;
    private Provas provas;
    private Santuarios santuarios;
    private Lendas lendas;
    private MenuLendas menuLendas;
    private PedrasDeViagem pedras;
    private Locais locais;
    private Pesca pesca;
    private Alquimia alquimia;
    private Acessorios acessorios;
    private GuardaRoupa guardaRoupa;
    private Colonias colonias;
    private Prefeituras prefeituras;
    private Reinos reinos;
    private Perigo perigo;
    private Bestiario bestiario;
    private Ninhos ninhos;
    private Hordas hordas;
    private ChefeMundial chefeMundial;
    private Estacoes estacoes;
    private Ceu ceu;
    private Maldicoes maldicoes;
    private Temperatura temperatura;
    private Portais portais;
    private Sombras sombras;
    private Torre torre;
    private ObeliscoTorre obeliscos;
    private Combos combos;
    private Mobilidade mobilidade;
    private Mochila mochila;
    private Controle controle;
    private Deuses deuses;
    private SantuariosDivinos santuariosDivinos;
    private Astronomia astronomia;
    private Transmutacao transmutacao;
    private Runas runas;
    private MesasRunicas mesasRunicas;
    private PacoteRecursos pacote;
    private Mitrilo mitrilo;
    private MapasDoTesouro mapas;
    private Arqueologia arqueologia;
    private Enciclopedia enciclopedia;
    private Cronista cronista;
    private Talentos talentos;
    private Renascimento renascimento;
    private Evolucao evolucao;
    private ForjadosEspeciais forjadosEspeciais;
    private ItensDetalhes itensDetalhes;
    private Natureza natureza;
    private Assentos assentos;
    private Fogueiras fogueiras;
    private BonecosTreino bonecos;
    private PedrasDeAmolar pedrasAmolar;
    private Bebedouros bebedouros;
    private Trofeus trofeus;
    private MesasCartografo mesasCartografo;
    private Lapides lapides;
    private Recordes recordes;
    private Diario diario;
    private Canteiros canteiros;
    private Colmeias colmeias;
    private Barris barris;
    private Album album;
    private MercadorItinerante mercador;
    private Encontros encontros;
    private Flechas flechas;
    private Frascos frascos;
    private Segredos segredos;
    private MenuRpg menuRpg;
    private Placar placar;

    private static RPGAtributos instancia;

    /** Para código estático (ex.: as magias) chegar no plugin. */
    public static RPGAtributos instancia() { return instancia; }

    @Override
    public void onEnable() {
        instancia = this;
        atualizarConfig();
        saveDefaultConfig();
        completarConfig();
        settings = new Settings(getConfig());
        stats = new StatsManager(this);
        bonus = new BonusManager(this);
        talentos = new Talentos(this);
        renascimento = new Renascimento(this);
        diario = new Diario(this);
        recordes = new Recordes(this);
        recordes.carregar();
        segredos = new Segredos(this);
        album = new Album(this);
        tags = new TagManager(this);
        skillListener = new SkillListener(this);
        menus = new MenuManager(this);
        forja = new Forja(this);
        forjas = new Forjas(this);
        refinaria = new Refinaria(this);
        efeitos = new EfeitosListener(this);
        arcano = new Arcano(this);
        guia = new Guia(this);
        titulos = new Titulos(this);
        chefes = new Chefes(this);
        eventos = new Eventos(this);
        mercadores = new Mercadores(this);
        altares = new Altares(this);
        missoes = new Missoes(this);
        agricultura = new Agricultura(this);
        cozinha = new Cozinha(this);
        reciclagem = new Reciclagem(this);
        parties = new Parties(this);
        parties.carregar();
        menuParty = new MenuParty(this);
        territorios = new Territorios(this);
        territorios.carregar();
        marcos = new Marcos(this);
        menusTerritorio = new MenuTerritorio(this);
        companheiros = new Companheiros(this);
        evolucao = new Evolucao(this);
        comportamento = new Comportamento(this);
        altaresDomador = new AltaresDomador(this);
        menusDomador = new MenusDomador(this);
        masmorras = new Masmorras(this);
        portaisMasmorra = new PortaisMasmorra(this);
        classes = new Classes(this);
        provas = new Provas(this);
        santuarios = new Santuarios(this);
        lendas = new Lendas(this);
        lendas.carregar();
        menuLendas = new MenuLendas(this);
        pedras = new PedrasDeViagem(this);
        pedras.carregar();
        locais = new Locais(this);
        classes.carregarLendarias();
        pesca = new Pesca(this);
        alquimia = new Alquimia(this);
        acessorios = new Acessorios(this);
        guardaRoupa = new GuardaRoupa(this);
        colonias = new Colonias(this);
        colonias.carregar();
        prefeituras = new Prefeituras(this);
        reinos = new Reinos(this);
        reinos.carregar();
        perigo = new Perigo(this);
        bestiario = new Bestiario(this);
        ninhos = new Ninhos(this);
        hordas = new Hordas(this);
        chefeMundial = new ChefeMundial(this);
        estacoes = new Estacoes(this);
        ceu = new Ceu(this);
        maldicoes = new Maldicoes(this);
        temperatura = new Temperatura(this);
        portais = new Portais(this);
        portais.carregar();
        sombras = new Sombras(this);
        torre = new Torre(this);
        torre.carregar();
        obeliscos = new ObeliscoTorre(this);
        combos = new Combos(this);
        mobilidade = new Mobilidade(this);
        mochila = new Mochila(this);
        controle = new Controle(this);
        deuses = new Deuses(this);
        deuses.carregar();
        santuariosDivinos = new SantuariosDivinos(this);
        astronomia = new Astronomia(this);
        transmutacao = new Transmutacao(this);
        runas = new Runas(this);
        mesasRunicas = new MesasRunicas(this);
        pacote = new PacoteRecursos(this);
        mitrilo = new Mitrilo(this);
        mapas = new MapasDoTesouro(this);
        arqueologia = new Arqueologia(this);
        enciclopedia = new Enciclopedia(this);
        cronista = new Cronista(this);
        forjadosEspeciais = new ForjadosEspeciais(this);
        itensDetalhes = new ItensDetalhes(this);
        natureza = new Natureza(this);
        assentos = new Assentos(this);
        fogueiras = new Fogueiras(this);
        bonecos = new BonecosTreino(this);
        pedrasAmolar = new PedrasDeAmolar(this);
        bebedouros = new Bebedouros(this);
        trofeus = new Trofeus(this);
        mesasCartografo = new MesasCartografo(this);
        lapides = new Lapides(this);
        lapides.carregar();
        canteiros = new Canteiros(this);
        colmeias = new Colmeias(this);
        barris = new Barris(this);
        mercador = new MercadorItinerante(this);
        encontros = new Encontros(this);
        flechas = new Flechas(this);
        frascos = new Frascos(this);
        menuRpg = new MenuRpg(this);
        placar = new Placar(this);

        registrar(skillListener, tags, menus, new ForjaListener(this), efeitos, forjas, refinaria, guia, titulos,
                chefes, eventos, mercadores, altares, new MateriaisListener(), missoes, agricultura, cozinha, reciclagem,
                parties, menuParty, territorios, marcos, menusTerritorio, new ProtecaoListener(this),
                companheiros, comportamento, altaresDomador, menusDomador, masmorras, portaisMasmorra,
                classes, provas, santuarios, lendas, menuLendas, pedras, locais, pesca, alquimia, acessorios, guardaRoupa, colonias, prefeituras, reinos, perigo, bestiario, ninhos, chefeMundial,
                estacoes, ceu, maldicoes, portais, sombras, torre, obeliscos, combos, combos.golpes(), mobilidade, mochila, controle, deuses, santuariosDivinos, transmutacao, runas, mesasRunicas, pacote, mitrilo, mapas, arqueologia, enciclopedia, cronista,
                talentos, evolucao,
                forjadosEspeciais, itensDetalhes, natureza, assentos, fogueiras, bonecos, pedrasAmolar, bebedouros, trofeus,
                mesasCartografo, lapides, recordes, diario,
                canteiros, colmeias, barris, album, mercador, encontros, flechas, frascos, segredos, menuRpg, placar);
        locais.iniciar();
        masmorras.iniciar();
        controle.aplicarLimites();
        mobilidade.registrarReceita();
        mitrilo.registrarReceitas();
        itensDetalhes.registrarReceita();
        flechas.registrarReceitas();
        frascos.registrarReceitas();
        pacote.iniciar();
        companheiros.carregar();
        agricultura.registrarReceitas();

        Comandos comandos = new Comandos(this);
        for (String nome : new String[]{"atributos", "rpgadmin", "chapeu", "tag", "cosmeticos", "forja", "grimorio",
                "guia", "titulos", "missoes", "receitas", "pets", "masmorra", "classe", "lendas", "locais", "peixes", "alquimia",
                "acessorios", "guardaroupa", "colonia", "bestiario", "calendario", "maldicao"}) {
            PluginCommand c = getCommand(nome);
            if (c != null) {
                c.setExecutor(comandos);
                c.setTabCompleter(comandos);
            }
        }
        ComandoParty cmdParty = new ComandoParty(this);
        ComandoTerritorio cmdTerritorio = new ComandoTerritorio(this);
        PluginCommand cpo = getCommand("portais");
        if (cpo != null) cpo.setExecutor(portais);
        PluginCommand cso = getCommand("sombras");
        if (cso != null) cso.setExecutor(sombras);
        PluginCommand cco = getCommand("combos");
        if (cco != null) cco.setExecutor(combos);
        PluginCommand cmo = getCommand("mochila");
        if (cmo != null) cmo.setExecutor(mochila);
        PluginCommand cde = getCommand("deus");
        if (cde != null) cde.setExecutor(deuses);
        PluginCommand ces = getCommand("estrelas");
        if (ces != null) ces.setExecutor(astronomia);
        PluginCommand cen = getCommand("enciclopedia");
        if (cen != null) cen.setExecutor(enciclopedia);
        PluginCommand ccr = getCommand("cronista");
        if (ccr != null) ccr.setExecutor(cronista);
        PluginCommand cta = getCommand("talentos");
        if (cta != null) cta.setExecutor(talentos);
        PluginCommand cre = getCommand("renascer");
        if (cre != null) cre.setExecutor(renascimento);
        PluginCommand cse = getCommand("sentar");
        if (cse != null) cse.setExecutor(assentos);
        PluginCommand cdi = getCommand("diario");
        if (cdi != null) cdi.setExecutor(diario);
        PluginCommand crc = getCommand("recordes");
        if (crc != null) crc.setExecutor(recordes);
        PluginCommand crp = getCommand("rpg");
        if (crp != null) crp.setExecutor(menuRpg);
        PluginCommand cpl = getCommand("placar");
        if (cpl != null) cpl.setExecutor(placar);
        PluginCommand cal = getCommand("album");
        if (cal != null) cal.setExecutor(album);
        PluginCommand csg = getCommand("segredos");
        if (csg != null) csg.setExecutor(segredos);
        PluginCommand cto = getCommand("torre");
        if (cto != null) {
            cto.setExecutor(torre);
            cto.setTabCompleter(torre);
        }
        ComandoReino cmdReino = new ComandoReino(this);
        PluginCommand cr = getCommand("reino");
        if (cr != null) {
            cr.setExecutor(cmdReino);
            cr.setTabCompleter(cmdReino);
        }
        for (String nome : new String[]{"party", "pc", "territorio"}) {
            PluginCommand c = getCommand(nome);
            if (c == null) continue;
            var executor = nome.equals("territorio") ? cmdTerritorio : cmdParty;
            c.setExecutor(executor);
            c.setTabCompleter(executor);
        }

        var agenda = getServer().getScheduler();
        // Corrida: confere a distância corrida 1x por segundo.
        agenda.runTaskTimer(this, skillListener::verificarCorrida, 20L, 20L);
        // Tags: seguem o jogador a cada tick.
        agenda.runTaskTimer(this, tags::tick, 1L, 1L);
        // Efeitos passivos (itens forjados, conjuntos, títulos, núcleos): 1x por segundo.
        efeitos.registrarFonte(titulos::efeitos);
        efeitos.registrarFonte(lendas::efeitos);
        efeitos.registrarFonte(acessorios::efeitos);
        agenda.runTaskTimer(this, efeitos::aplicarPassivos, 20L, 20L);
        // Estações de ritual: Forja do Ferreiro e Altar Ritualístico.
        forjas.iniciar();
        agenda.runTaskTimer(this, forjas::tick, 5L, 5L);
        altares.iniciar();
        agenda.runTaskTimer(this, altares::tick, 5L, 5L);
        cozinha.iniciar();
        agenda.runTaskTimer(this, cozinha::tick, 5L, 5L);
        reciclagem.iniciar();
        agenda.runTaskTimer(this, reciclagem::tick, 5L, 5L);
        marcos.iniciar();
        agenda.runTaskTimer(this, marcos::tick, 5L, 5L);
        agenda.runTaskTimer(this, parties::limparConvites, 1200L, 1200L);
        // Domador: altar, ordens dos companheiros (meio segundo), montaria (todo tick) e registro (30 s).
        altaresDomador.iniciar();
        // Masmorras: obras (todo tick), portais (5 ticks), salas e ondas (10 ticks).
        portaisMasmorra.iniciar();
        agenda.runTaskTimer(this, portaisMasmorra::tick, 5L, 5L);
        agenda.runTaskTimer(this, masmorras::construir, 1L, 1L);
        agenda.runTaskTimer(this, masmorras::tickPortais, 5L, 5L);
        agenda.runTaskTimer(this, masmorras::tick, 10L, 10L);
        // Classes: santuários e armadilhas (5 ticks), provas (10 ticks).
        santuarios.iniciar();
        agenda.runTaskTimer(this, santuarios::tick, 5L, 5L);
        agenda.runTaskTimer(this, classes.habilidades()::tick, 5L, 5L);
        agenda.runTaskTimer(this, provas::tick, 10L, 10L);
        agenda.runTaskTimer(this, lendas::tick, 40L, 40L);
        // Pedras de Viagem (5 ticks) e Locais Ocultos (1 segundo).
        pedras.iniciar();
        agenda.runTaskTimer(this, pedras::tick, 5L, 5L);
        agenda.runTaskTimer(this, locais::tick, 20L, 20L);
        // Alquimia: bancada (5 ticks), acessórios de bolso (2 ticks) e guarda-roupa (2 segundos).
        alquimia.iniciar();
        agenda.runTaskTimer(this, alquimia::tick, 5L, 5L);
        agenda.runTaskTimer(this, acessorios::tick, 2L, 2L);
        agenda.runTaskTimer(this, guardaRoupa::tick, 40L, 40L);
        // Colônias: Prefeituras (5 ticks) e turnos de trabalho (1 segundo).
        prefeituras.iniciar();
        agenda.runTaskTimer(this, prefeituras::tick, 5L, 5L);
        agenda.runTaskTimer(this, colonias::tick, 20L, 20L);
        agenda.runTaskTimer(this, colonias::tickSoldados, 10L, 10L);
        // Reinos: guerras marcadas e captura de Marcos (1 segundo).
        agenda.runTaskTimer(this, reinos::tick, 20L, 20L);
        // Mundo perigoso: Elites (meio segundo), ninhos, hordas e Chefe Mundial (1 segundo).
        agenda.runTaskTimer(this, perigo::tick, 10L, 10L);
        agenda.runTaskTimer(this, ninhos::tick, 20L, 20L);
        agenda.runTaskTimer(this, hordas::tick, 20L, 20L);
        agenda.runTaskTimer(this, chefeMundial::tick, 20L, 20L);
        // Mundo vivo: estações e céu (1 segundo), maldições e temperatura (2 segundos).
        agenda.runTaskTimer(this, estacoes::tick, 20L, 20L);
        agenda.runTaskTimer(this, ceu::tick, 20L, 20L);
        agenda.runTaskTimer(this, maldicoes::tick, 40L, 40L);
        agenda.runTaskTimer(this, temperatura::tick, 40L, 40L);
        // Fase C: portais do mundo (5 ticks), sombras do Soberano e Torre Infinita (meio segundo).
        agenda.runTaskTimer(this, portais::tick, 5L, 5L);
        agenda.runTaskTimer(this, sombras::tick, 10L, 10L);
        agenda.runTaskTimer(this, torre::tick, 10L, 10L);
        agenda.runTaskTimer(this, combos::tick, 10L, 10L);
        agenda.runTaskTimer(this, combos::tickRapido, 1L, 1L);
        agenda.runTaskTimer(this, mobilidade::tick, 1L, 1L);
        agenda.runTaskTimer(this, controle::relatarLimpeza, 6000L, 6000L);
        // Fase F: fé, céu, transmutação e runas.
        santuariosDivinos.iniciar();
        transmutacao.iniciar();
        mesasRunicas.iniciar();
        agenda.runTaskTimer(this, santuariosDivinos::tick, 5L, 5L);
        agenda.runTaskTimer(this, transmutacao::tick, 5L, 5L);
        agenda.runTaskTimer(this, mesasRunicas::tick, 5L, 5L);
        agenda.runTaskTimer(this, deuses::tick, 20L, 20L);
        agenda.runTaskTimer(this, astronomia::tick, 10L, 10L);
        agenda.runTaskTimer(this, runas::tick, 2L, 2L);
        // Fase G: mitrilo (brilho), mapas do tesouro e enciclopédia.
        agenda.runTaskTimer(this, mitrilo::tick, 40L, 40L);
        agenda.runTaskTimer(this, mapas::tick, 20L, 20L);
        agenda.runTaskTimer(this, enciclopedia::tick, 40L, 40L);
        // Fase I: Andarilho Eterno e evolução dos companheiros.
        agenda.runTaskTimer(this, talentos::tick, 40L, 40L);
        agenda.runTaskTimer(this, evolucao::tick, 40L, 40L);
        // Detalhes: forjados especiais, estações pequenas, natureza, túmulos e recordes.
        for (Estacao e : List.of(fogueiras, bonecos, pedrasAmolar, bebedouros, trofeus, mesasCartografo, canteiros, colmeias, barris)) {
            e.iniciar();
            agenda.runTaskTimer(this, e::tick, 5L, 5L);
        }
        agenda.runTaskTimer(this, forjadosEspeciais::tick, 10L, 10L);
        agenda.runTaskTimer(this, natureza::tick, 40L, 40L);
        agenda.runTaskTimer(this, lapides::tick, 40L, 40L);
        agenda.runTaskTimer(this, recordes::tick, 100L, 100L);
        // Vida no mundo: mercador (1 min), encontros (20 s), flechas rastreadoras (todo tick), segredos (2 s), placar (1 s).
        agenda.runTaskTimer(this, mercador::tick, 1200L, 1200L);
        agenda.runTaskTimer(this, encontros::tick, 400L, 400L);
        agenda.runTaskTimer(this, flechas::tick, 1L, 1L);
        agenda.runTaskTimer(this, segredos::tick, 40L, 40L);
        agenda.runTaskTimer(this, placar::tick, 20L, 20L);
        obeliscos.iniciar();
        agenda.runTaskTimer(this, obeliscos::tick, 5L, 5L);
        agenda.runTaskTimer(this, altaresDomador::tick, 5L, 5L);
        agenda.runTaskTimer(this, comportamento::tick, 10L, 10L);
        agenda.runTaskTimer(this, comportamento::controlarMontarias, 1L, 1L);
        agenda.runTaskTimer(this, companheiros::atualizarRegistros, 600L, 600L);
        // Toque verde da Agricultura: a cada 4 segundos.
        agenda.runTaskTimer(this, agricultura::tick, 80L, 80L);
        // Chefes (meio segundo) e eventos do altar (1 segundo).
        agenda.runTaskTimer(this, chefes::tick, 10L, 10L);
        agenda.runTaskTimer(this, eventos::tick, 20L, 20L);
        // Infusão e magias: registra seus próprios eventos e tarefas.
        arcano.iniciar();

        // Caso o plugin seja carregado com gente online (/reload).
        for (Player p : getServer().getOnlinePlayers()) {
            bonus.aplicar(p);
            tags.garantir(p);
            titulos.aplicarBonus(p);
            classes.aplicar(p);
            acessorios.carregar(p);
        }
        getLogger().info("RPGAtributos ativado!");
    }

    private void registrar(Listener... listeners) {
        for (Listener l : listeners) getServer().getPluginManager().registerEvents(l, this);
    }

    @Override
    public void onDisable() {
        if (menus != null) menus.fecharTodos();
        if (guardaRoupa != null) guardaRoupa.desfazerTodos();
        if (acessorios != null) acessorios.apagarTodas();
        if (tags != null) tags.removerTodos();
        if (efeitos != null) efeitos.removerTodos();
        if (chefes != null) chefes.removerTodos();
        if (eventos != null) eventos.encerrarTudo();
        if (mercadores != null) mercadores.removerTodos();
        if (arcano != null) arcano.parar();
        if (comportamento != null) comportamento.parar();
        if (companheiros != null) companheiros.desligar();
        if (combos != null) combos.parar();
        if (pacote != null) pacote.parar();
        if (lapides != null) lapides.salvar();
        if (mercador != null) mercador.parar();
        if (encontros != null) encontros.parar();
        if (placar != null) placar.desligarTodos();
        if (recordes != null) recordes.salvar();
        if (sombras != null) sombras.recolherTodas();
        if (torre != null) torre.encerrarTodas();
        if (portais != null) portais.parar();
        if (provas != null) provas.encerrarTodas();
        if (locais != null) locais.parar();
        if (masmorras != null) masmorras.encerrarTodas();
        if (parties != null) parties.salvar();
        if (colonias != null) colonias.salvar();
        if (reinos != null) reinos.parar();
        if (perigo != null) perigo.parar();
        if (ninhos != null) ninhos.parar();
        if (hordas != null) hordas.parar();
        if (chefeMundial != null) chefeMundial.parar();
        if (estacoes != null) estacoes.salvar();
        if (ceu != null) ceu.parar();
        if (maldicoes != null) maldicoes.parar();
        if (territorios != null) {
            territorios.pararBordas();
            territorios.salvar();
        }
    }

    /**
     * Config de uma versão antiga do plugin (ex.: nível máximo 50, bônus "por nível"):
     * guarda como backup e deixa o saveDefaultConfig criar o novo.
     */
    private void atualizarConfig() {
        File arquivo = new File(getDataFolder(), "config.yml");
        if (!arquivo.exists()) return;
        int versao = YamlConfiguration.loadConfiguration(arquivo).getInt("config-versao", 1);
        if (versao >= Settings.VERSAO_CONFIG) return;
        File backup = new File(getDataFolder(), "config-antigo-v" + versao + ".yml");
        if (arquivo.renameTo(backup)) {
            getLogger().warning("O config.yml era de uma versão antiga e foi salvo como " + backup.getName()
                    + ". Um config novo (nível máximo 100, valores rebalanceados) foi criado.");
        }
    }

    /** Opções novas (de uma versão mais nova do plugin) são escritas no config.yml, sem mexer nas existentes. */
    private void completarConfig() {
        var cfg = getConfig();
        if (cfg.getDefaults() == null) return;
        boolean faltando = cfg.getDefaults().getKeys(true).stream().anyMatch(k -> !cfg.isSet(k));
        if (!faltando) return;
        cfg.options().copyDefaults(true);
        saveConfig();
        getLogger().info("Opções novas foram adicionadas ao config.yml.");
    }

    public void recarregar() {
        reloadConfig();
        settings = new Settings(getConfig());
        if (controle != null) controle.aplicarLimites();
        for (Player p : getServer().getOnlinePlayers()) {
            bonus.aplicar(p);
            tags.garantir(p);
            tags.atualizarTexto(p);
        }
    }

    public Settings settings() { return settings; }
    public EfeitosListener efeitosPassivos() { return efeitos; }
    public StatsManager stats() { return stats; }
    public BonusManager bonus() { return bonus; }
    public TagManager tags() { return tags; }
    public MenuManager menus() { return menus; }
    public Forja forja() { return forja; }
    public Arcano arcano() { return arcano; }
    public Forjas forjas() { return forjas; }
    public Refinaria refinaria() { return refinaria; }
    public Guia guia() { return guia; }
    public Titulos titulos() { return titulos; }
    public Chefes chefes() { return chefes; }
    public Eventos eventos() { return eventos; }
    public Mercadores mercadores() { return mercadores; }
    public Altares altares() { return altares; }
    public Missoes missoes() { return missoes; }
    public Agricultura agricultura() { return agricultura; }
    public Cozinha cozinha() { return cozinha; }
    public Reciclagem reciclagem() { return reciclagem; }
    public Parties parties() { return parties; }
    public MenuParty menuParty() { return menuParty; }
    public Territorios territorios() { return territorios; }
    public Marcos marcos() { return marcos; }
    public MenuTerritorio menusTerritorio() { return menusTerritorio; }
    public Companheiros companheiros() { return companheiros; }
    public AltaresDomador altaresDomador() { return altaresDomador; }
    public MenusDomador menusDomador() { return menusDomador; }
    public Masmorras masmorras() { return masmorras; }
    public PortaisMasmorra portaisMasmorra() { return portaisMasmorra; }
    public Classes classes() { return classes; }
    public Provas provas() { return provas; }
    public Santuarios santuarios() { return santuarios; }
    public Lendas lendas() { return lendas; }
    public MenuLendas menuLendas() { return menuLendas; }
    public PedrasDeViagem pedras() { return pedras; }
    public Locais locais() { return locais; }
    public Pesca pesca() { return pesca; }
    public Alquimia alquimia() { return alquimia; }
    public Acessorios acessorios() { return acessorios; }
    public GuardaRoupa guardaRoupa() { return guardaRoupa; }
    public Colonias colonias() { return colonias; }
    public Prefeituras prefeituras() { return prefeituras; }
    public Reinos reinos() { return reinos; }
    public Perigo perigo() { return perigo; }
    public Bestiario bestiario() { return bestiario; }
    public Ninhos ninhos() { return ninhos; }
    public Hordas hordas() { return hordas; }
    public ChefeMundial chefeMundial() { return chefeMundial; }
    public Estacoes estacoes() { return estacoes; }
    public Ceu ceu() { return ceu; }
    public Maldicoes maldicoes() { return maldicoes; }
    public Portais portais() { return portais; }
    public Sombras sombras() { return sombras; }
    public Torre torre() { return torre; }
    public ObeliscoTorre obeliscos() { return obeliscos; }
    public Combos combos() { return combos; }
    public Mobilidade mobilidade() { return mobilidade; }
    public Mochila mochila() { return mochila; }
    public Controle controle() { return controle; }
    public Deuses deuses() { return deuses; }
    public SantuariosDivinos santuariosDivinos() { return santuariosDivinos; }
    public Astronomia astronomia() { return astronomia; }
    public Transmutacao transmutacao() { return transmutacao; }
    public Runas runas() { return runas; }
    public MesasRunicas mesasRunicas() { return mesasRunicas; }
    public PacoteRecursos pacote() { return pacote; }
    public Mitrilo mitrilo() { return mitrilo; }
    public MapasDoTesouro mapas() { return mapas; }
    public Arqueologia arqueologia() { return arqueologia; }
    public Enciclopedia enciclopedia() { return enciclopedia; }
    public Cronista cronista() { return cronista; }
    public Talentos talentos() { return talentos; }
    public Renascimento renascimento() { return renascimento; }
    public Evolucao evolucao() { return evolucao; }
    public ForjadosEspeciais forjadosEspeciais() { return forjadosEspeciais; }
    public ItensDetalhes itensDetalhes() { return itensDetalhes; }
    public Natureza natureza() { return natureza; }
    public Assentos assentos() { return assentos; }
    public Fogueiras fogueiras() { return fogueiras; }
    public BonecosTreino bonecos() { return bonecos; }
    public PedrasDeAmolar pedrasAmolar() { return pedrasAmolar; }
    public Bebedouros bebedouros() { return bebedouros; }
    public Trofeus trofeus() { return trofeus; }
    public MesasCartografo mesasCartografo() { return mesasCartografo; }
    public Lapides lapides() { return lapides; }
    public Recordes recordes() { return recordes; }
    public Diario diario() { return diario; }
    public Canteiros canteiros() { return canteiros; }
    public Colmeias colmeias() { return colmeias; }
    public Barris barris() { return barris; }
    public Album album() { return album; }
    public MercadorItinerante mercador() { return mercador; }
    public Encontros encontros() { return encontros; }
    public Flechas flechas() { return flechas; }
    public Frascos frascos() { return frascos; }
    public Segredos segredos() { return segredos; }
    public MenuRpg menuRpg() { return menuRpg; }
    public Placar placar() { return placar; }

    /** O bloco é alguma estação de ritual (Forja, Infusor, Altares, Cozinha, Reciclagem, Marco)? */
    public boolean ehEstacao(Block b) {
        return forjas.eh(b) || arcano.infusores().eh(b) || altares.eh(b) || cozinha.eh(b) || reciclagem.eh(b) || marcos.eh(b)
                || altaresDomador.eh(b) || portaisMasmorra.eh(b) || santuarios.eh(b) || pedras.eh(b) || alquimia.eh(b) || prefeituras.eh(b) || obeliscos.eh(b)
                || santuariosDivinos.eh(b) || transmutacao.eh(b) || mesasRunicas.eh(b)
                || fogueiras.eh(b) || bonecos.eh(b) || pedrasAmolar.eh(b) || bebedouros.eh(b) || trofeus.eh(b) || mesasCartografo.eh(b)
                || canteiros.eh(b) || colmeias.eh(b) || barris.eh(b);
    }

    /**
     * Um jogador pode ferir (ou lançar magia ruim em) outro? Junta a regra de PvP do mundo,
     * o fogo amigo da party e a regra de PvP dos territórios onde os dois estão.
     */
    public boolean pvpPermitido(Player atacante, Player vitima) {
        if (atacante.equals(vitima)) return true;
        if (Boolean.FALSE.equals(vitima.getWorld().getGameRuleValue(GameRules.PVP))) return false;
        // Reino: sem fogo amigo entre membros; na guerra, o PvP entre os dois reinos é livre em qualquer lugar.
        if (reinos.aliados(atacante.getUniqueId(), vitima.getUniqueId())) return false;
        if (reinos.inimigosEmGuerra(atacante.getUniqueId(), vitima.getUniqueId())) return true;
        Party pt = parties.party(atacante);
        if (pt != null && pt.tem(vitima.getUniqueId()) && !pt.fogoAmigo()) return false;
        return territorios.flagAqui(vitima.getLocation(), Flag.PVP) && territorios.flagAqui(atacante.getLocation(), Flag.PVP);
    }
}
