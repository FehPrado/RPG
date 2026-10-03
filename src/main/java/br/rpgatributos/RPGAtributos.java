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

        registrar(skillListener, tags, menus, new ForjaListener(this), efeitos, forjas, refinaria, guia, titulos,
                chefes, eventos, mercadores, altares, new MateriaisListener(), missoes, agricultura, cozinha, reciclagem,
                parties, menuParty, territorios, marcos, menusTerritorio, new ProtecaoListener(this),
                companheiros, comportamento, altaresDomador, menusDomador, masmorras, portaisMasmorra,
                classes, provas, santuarios, lendas, menuLendas, pedras, locais, pesca, alquimia, acessorios, guardaRoupa, colonias, prefeituras, reinos, perigo, bestiario, ninhos, chefeMundial,
                estacoes, ceu, maldicoes, portais, sombras, torre, obeliscos);
        locais.iniciar();
        masmorras.iniciar();
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

    /** O bloco é alguma estação de ritual (Forja, Infusor, Altares, Cozinha, Reciclagem, Marco)? */
    public boolean ehEstacao(Block b) {
        return forjas.eh(b) || arcano.infusores().eh(b) || altares.eh(b) || cozinha.eh(b) || reciclagem.eh(b) || marcos.eh(b)
                || altaresDomador.eh(b) || portaisMasmorra.eh(b) || santuarios.eh(b) || pedras.eh(b) || alquimia.eh(b) || prefeituras.eh(b) || obeliscos.eh(b);
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
