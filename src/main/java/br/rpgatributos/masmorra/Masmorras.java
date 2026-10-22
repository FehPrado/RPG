package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.inventory.Inventory;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As masmorras: um mundo vazio só para elas, o portal de entrada, as salas que trancam
 * e soltam ondas de monstros, o chefe no fim e o Cofre das Almas (quem morre lá dentro
 * paga diamantes para pegar as coisas de volta).
 */
public final class Masmorras implements Listener {

    public static final String MUNDO = "rpg_masmorras";
    public static final TextColor COR = TextColor.color(0x7E57C2);
    private static final NamespacedKey K_MOB = new NamespacedKey("rpgatributos", "masmorra_mob");
    private static final NamespacedKey K_RETORNO = new NamespacedKey("rpgatributos", "masmorra_retorno");
    private static final long PORTAL_MS = 30_000;

    /** Gera um mundo sem nada (as masmorras são construídas pelo plugin). */
    private static final class MundoVazio extends ChunkGenerator { }

    private record Portal(UUID masmorra, long ate) {}

    private final RPGAtributos plugin;
    private final SalasPersonalizadas personalizadas;
    private final Gerador gerador;
    private World mundo;
    private final Map<UUID, Masmorra> ativas = new LinkedHashMap<>();
    private final Map<UUID, Deque<Runnable>> obras = new LinkedHashMap<>();
    /** De onde cada masmorra foi aberta (o Portal da Masmorra), ou null (admin). */
    private final Map<UUID, Location> origem = new HashMap<>();
    private final Map<Location, Portal> portais = new HashMap<>();
    private final Map<UUID, Masmorra> chefes = new HashMap<>();
    private final Map<UUID, Location> retornos = new HashMap<>();
    private final Map<UUID, Location> morreram = new HashMap<>();
    private final Map<UUID, List<List<ItemStack>>> cofre = new HashMap<>();
    private final File arquivoCofre;
    private int proximaVaga;
    private int ciclo;
    /** 2.70: estatísticas e recordes dos portais. */
    private final Estatisticas estatisticas;
    /** 2.70: o tesouro que cada mímico guarda (cai quando ele morre). */
    private final Map<UUID, List<ItemStack>> tesouroDoMimico = new HashMap<>();

    public Estatisticas estatisticas() { return estatisticas; }

    public Masmorras(RPGAtributos plugin) {
        this.plugin = plugin;
        this.personalizadas = new SalasPersonalizadas(plugin);
        this.gerador = new Gerador(plugin, personalizadas);
        this.arquivoCofre = new File(plugin.getDataFolder(), "cofre.yml");
        this.estatisticas = new Estatisticas(plugin);
    }

    private Settings cfg() { return plugin.settings(); }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
    public SalasPersonalizadas salas() { return personalizadas; }

    // =====================================================================
    //  Mundo das masmorras
    // =====================================================================

    /** Apaga e recria o mundo das masmorras (começa sempre limpo) e carrega o cofre. */
    public void iniciar() {
        carregarCofre();
        if (Bukkit.getWorld(MUNDO) == null) apagar(new File(Bukkit.getWorldContainer(), MUNDO));
        mundo = new WorldCreator(MUNDO).generator(new MundoVazio()).generateStructures(false).createWorld();
        if (mundo == null) {
            plugin.getLogger().warning("Não consegui criar o mundo das masmorras.");
            return;
        }
        mundo.setGameRule(GameRules.ADVANCE_TIME, false);
        mundo.setGameRule(GameRules.ADVANCE_WEATHER, false);
        mundo.setGameRule(GameRules.SPAWN_MOBS, false);
        mundo.setGameRule(GameRules.SPAWN_MONSTERS, false);
        mundo.setGameRule(GameRules.SPAWN_PATROLS, false);
        mundo.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        mundo.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        mundo.setGameRule(GameRules.SPAWN_WARDENS, false);
        mundo.setGameRule(GameRules.MOB_GRIEFING, false);
        mundo.setGameRule(GameRules.KEEP_INVENTORY, false);
        mundo.setGameRule(GameRules.TNT_EXPLODES, false);
        mundo.setGameRule(GameRules.PROJECTILES_CAN_BREAK_BLOCKS, false);
        mundo.setTime(18_000);
        mundo.setDifficulty(Difficulty.HARD);
    }

    private static void apagar(File f) {
        File[] filhos = f.listFiles();
        if (filhos != null) for (File c : filhos) apagar(c);
        if (f.exists() && !f.delete()) f.deleteOnExit();
    }

    public boolean ehMundo(World w) {
        return mundo != null && mundo.equals(w);
    }

    /** O mundo das masmorras (também usado pelas provas de classe), ou null. */
    public World mundo() {
        return mundo;
    }

    /** Guarda para onde o jogador volta (sobrevive a quedas do servidor). */
    public void guardarRetorno(Player p, Location volta) {
        retornos.put(p.getUniqueId(), volta);
        p.getPersistentDataContainer().set(K_RETORNO, PersistentDataType.STRING, texto(volta));
    }

    /** Leva o jogador de volta para onde entrou e esquece o retorno. */
    public void voltar(Player p) {
        Location volta = retorno(p);
        limparRetorno(p);
        if (ehMundo(p.getWorld())) p.teleport(volta);
    }

    /** Para onde o jogador volta (sem esquecer). */
    public Location ondeVolta(Player p) {
        return retorno(p);
    }

    public Masmorra de(Player p) {
        for (Masmorra m : ativas.values()) if (m.participantes.contains(p.getUniqueId())) return m;
        return null;
    }

    /** O dono, ou alguém da party dele, já tem uma masmorra em andamento? */
    private boolean ocupado(Player p) {
        Party pt = plugin.parties().party(p);
        for (Masmorra m : ativas.values()) {
            if (m.dono.equals(p.getUniqueId()) || m.participantes.contains(p.getUniqueId())) return true;
            if (pt != null && (pt.tem(m.dono) || m.participantes.stream().anyMatch(pt::tem))) return true;
        }
        return false;
    }

    public boolean portalAberto(Location estacao) {
        Portal p = portais.get(estacao);
        return p != null && p.ate > System.currentTimeMillis();
    }

    public boolean formando(Location estacao) {
        for (Map.Entry<UUID, Location> en : origem.entrySet()) {
            Masmorra m = ativas.get(en.getKey());
            if (m != null && m.estado == Masmorra.Estado.GERANDO && estacao.equals(en.getValue())) return true;
        }
        return false;
    }

    // =====================================================================
    //  Abrir
    // =====================================================================

    /** Abre uma masmorra no Portal (cobra o custo). @return motivo de não ter aberto, ou null. */
    public String abrir(Player p, Dificuldade d, Location estacao) {
        if (mundo == null) return "O mundo das masmorras não está disponível.";
        if (estacao != null && (portalAberto(estacao) || formando(estacao))) return "Este portal já está em uso. Espere um pouco.";
        if (ativas.size() >= cfg().masMaximo) return "Já há masmorras demais abertas agora. Tente daqui a pouco.";
        if (ocupado(p)) return "Você (ou alguém da sua party) já está numa masmorra.";
        int precisa = d.nivelTotalNecessario(cfg().nivelMaximo * Skill.values().length);
        if (plugin.renascimento().nivelTotalEfetivo(p) < precisa) return "Precisa de nível total " + precisa + " para a dificuldade " + d.nome() + ".";
        if (p.getGameMode() != GameMode.CREATIVE) {
            if (!p.getInventory().containsAtLeast(new ItemStack(d.custo()), d.qtdCusto())) {
                return "O portal pede " + d.qtdCusto() + "x " + nomeMaterial(d.custo()) + ".";
            }
            p.getInventory().removeItem(new ItemStack(d.custo(), d.qtdCusto()));
        }
        criar(p, d, estacao);
        p.sendMessage(Component.text("۞ A masmorra está se formando... o portal abre em instantes.", COR));
        return null;
    }

    /** Admin: abre sem custo e leva só ele quando ficar pronta. */
    public String abrirAdmin(Player p, Dificuldade d) {
        if (mundo == null) return "O mundo das masmorras não está disponível.";
        if (de(p) != null) return "Você já está numa masmorra.";
        criar(p, d, null);
        p.sendMessage(Component.text("۞ Gerando masmorra " + d.nome() + "...", COR));
        return null;
    }

    /**
     * 2.68: abre a masmorra de um Cristal de Portal (o cristal já foi gasto por quem chamou).
     * @return motivo de não ter aberto, ou null.
     */
    public String abrirComCristal(Player p, CristalPortal.Dados c, Location estacao) {
        if (mundo == null) return "O mundo das masmorras não está disponível.";
        if (estacao != null && (portalAberto(estacao) || formando(estacao))) return "Este altar já está em uso. Espere um pouco.";
        if (ativas.size() >= cfg().masMaximo) return "Já há masmorras demais abertas agora. Tente daqui a pouco.";
        if (ocupado(p)) return "Você (ou alguém da sua party) já está numa masmorra.";
        int precisa = c.rank().dificuldade().nivelTotalNecessario(cfg().nivelMaximo * Skill.values().length);
        if (p.getGameMode() != GameMode.CREATIVE && plugin.renascimento().nivelTotalEfetivo(p) < precisa) {
            return "Precisa de nível total " + precisa + " para um portal " + c.rank().nome() + ".";
        }
        // As Inscrições mudam a planta (salas de tesouro e minas): tudo antes de planejar.
        criar(p, c.rank(), estacao, m -> {
            m.objetivo = c.objetivo();
            m.mods.addAll(c.mods());
            m.cristal = true;
            m.salasTesouroExtra = c.quantas(Inscricao.TESOURO);
            m.minasExtra = c.quantas(Inscricao.MINA);
            m.minutosExtra = 3 * c.quantas(Inscricao.TEMPO);
            if (c.rank().ordinal() <= Rank.D.ordinal() && rnd().nextDouble() < 0.03) m.dupla = true; // 2.75: Masmorra Dupla
        });
        p.sendMessage(Component.text("۞ O cristal se parte... um portal " + c.rank().nome() + " está se formando.", c.rank().cor()));
        return null;
    }

    /**
     * 2.74: o Portal do Monarca, aberto no Mural das Relíquias (as relíquias já foram conferidas;
     * quem chamou só as gasta se isto der certo). Rank S, Caçada, 25 minutos.
     * @return motivo de não ter aberto, ou null.
     */
    public String abrirPortalMonarca(Player p, Monarca mo, Location estacao) {
        if (mundo == null) return "O mundo das masmorras não está disponível.";
        if (estacao != null && (portalAberto(estacao) || formando(estacao))) return "Este mural já tem um portal. Espere um pouco.";
        if (ativas.size() >= cfg().masMaximo) return "Já há masmorras demais abertas agora. Tente daqui a pouco.";
        if (ocupado(p)) return "Você (ou alguém da sua party) já está numa masmorra.";
        int precisa = Rank.S.dificuldade().nivelTotalNecessario(cfg().nivelMaximo * Skill.values().length);
        if (p.getGameMode() != GameMode.CREATIVE && plugin.renascimento().nivelTotalEfetivo(p) < precisa) {
            return "Precisa de nível total " + precisa + " para um Portal do Monarca.";
        }
        criar(p, Rank.S, estacao, m -> {
            m.objetivo = Objetivo.CACADA;
            m.cristal = true;
            m.monarca = mo;
            m.minutosExtra = 10;
        });
        p.sendMessage(Component.text("♛ As relíquias se partem... o Portal do " + mo.nome + " está se formando.", mo.cor));
        return null;
    }

    /**
     * 2.74: o Monarca caiu. Quem deu o golpe final (ou o dono do portal) herda a classe lendária
     * dele (2.76), se está livre e ele ainda não é Monarca. O da Destruição é anunciado para todos.
     */
    private void vencerMonarca(Masmorra m) {
        Monarca mo = m.monarca;
        List<Player> party = new ArrayList<>();
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) party.add(p);
        }
        if (!mo.jogavel()) {
            plugin.getServer().broadcast(Component.text("♛ ", mo.cor).append(Component.text(String.join(", ", party.stream().map(Player::getName).toList()),
                    NamedTextColor.YELLOW, TextDecoration.BOLD)).append(Component.text(" derrotou " + mo.chefe + "!", mo.cor, TextDecoration.BOLD)));
            for (Player p : party) plugin.titulos().registrar(p, "monarca_destruicao", 1);
            return;
        }
        List<Player> candidatos = new ArrayList<>();
        Player matador = m.matadorChefe == null ? null : Bukkit.getPlayer(m.matadorChefe);
        if (matador != null && party.contains(matador)) candidatos.add(matador);
        Player dono = Bukkit.getPlayer(m.dono);
        if (dono != null && party.contains(dono) && !candidatos.contains(dono)) candidatos.add(dono);
        for (Player p : party) if (!candidatos.contains(p)) candidatos.add(p);
        for (Player p : candidatos) {
            if (plugin.classes().concederMonarca(p, mo.classe())) {
                JanelaSistema.mostrar(p, "Monarca", Component.text("Você herdou a classe " + mo.nome + ".", mo.cor),
                        Component.text("Passivo: " + mo.classe().passivo().texto(), NamedTextColor.GRAY),
                        Component.text("Agache + F usa a habilidade; agache + clique esquerdo no ar troca.", NamedTextColor.WHITE),
                        Component.text("Só pode haver um. 30 dias sem entrar e ela fica livre.", NamedTextColor.DARK_GRAY));
                return;
            }
        }
        for (Player p : party) {
            p.sendMessage(Component.text("♛ " + mo.nome + ": a classe já tem dono (ou vocês já são Monarcas). O tesouro é de vocês.", NamedTextColor.GRAY));
        }
    }

    private Masmorra criar(Player dono, Dificuldade d, Location estacao) {
        return criar(dono, Rank.de(d), estacao);
    }

    private Masmorra criar(Player dono, Rank rank, Location estacao) {
        return criar(dono, rank, estacao, null);
    }

    /** {@code preparar}: ajusta a masmorra antes da planta (as Inscrições do cristal). */
    private Masmorra criar(Player dono, Rank rank, Location estacao, java.util.function.Consumer<Masmorra> preparar) {
        Dificuldade d = rank.dificuldade();
        Masmorra m = new Masmorra(proximaVaga++, d, Tema.sortear(), mundo, dono.getUniqueId());
        m.rank = rank;
        if (preparar != null) preparar.accept(m);
        Random r = new Random();
        gerador.planejar(m, r);
        ativas.put(m.id, m);
        obras.put(m.id, gerador.obras(m, r));
        origem.put(m.id, estacao);
        return m;
    }

    // =====================================================================
    //  Portais do mundo (Fase C): masmorra pública, sem custo
    // =====================================================================

    /** Motivo de o jogador não poder entrar numa masmorra dessa dificuldade, ou null. */
    public String impedimentoPortal(Player p, Dificuldade d) {
        if (mundo == null) return "O mundo das masmorras não está disponível.";
        if (de(p) != null) return "Você já está numa masmorra.";
        int precisa = d.nivelTotalNecessario(cfg().nivelMaximo * Skill.values().length);
        if (p.getGameMode() != GameMode.CREATIVE && plugin.renascimento().nivelTotalEfetivo(p) < precisa) {
            return "O portal te repele: precisa de nível total " + precisa + ".";
        }
        return null;
    }

    /**
     * Gera a masmorra de um Portal do mundo. Qualquer um pode entrar enquanto ela existir.
     * @return a masmorra (ainda se formando), ou null se não há vaga agora.
     */
    public Masmorra criarParaPortal(Player quem, Dificuldade d, int tesouroExtra, java.util.function.Consumer<Masmorra> aoConcluir) {
        return criarParaPortal(quem, Rank.de(d), tesouroExtra, aoConcluir, false);
    }

    /** 2.68: o portal do mundo com rank (e o Portal Vermelho, sem saída até vencer). */
    public Masmorra criarParaPortal(Player quem, Rank rank, int tesouroExtra, java.util.function.Consumer<Masmorra> aoConcluir, boolean vermelho) {
        if (mundo == null || ativas.size() >= cfg().masMaximo + 2) return null;
        Masmorra m = criar(quem, rank, null);
        m.vermelho = vermelho;
        m.publica = true;
        // 2.75: às vezes um portal E ou D esconde a Câmara das Estátuas (Masmorra Dupla).
        if (rank.ordinal() <= Rank.D.ordinal() && rnd().nextDouble() < 0.05) m.dupla = true;
        m.tesouroExtra = tesouroExtra;
        m.aoConcluir = aoConcluir;
        return m;
    }

    /** Entra na masmorra de um Portal do mundo. @return motivo de não ter entrado, ou null. */
    public String entrarPortal(Player p, Masmorra m) {
        if (!ativas.containsKey(m.id)) return "Esse portal perdeu a força.";
        if (m.estado == Masmorra.Estado.GERANDO) return "O portal ainda está se formando...";
        if (m.estado == Masmorra.Estado.CONCLUIDA) return "Esse portal já foi vencido.";
        if (m.sairam.contains(p.getUniqueId())) return "O portal não deixa você voltar.";
        String erro = impedimentoPortal(p, m.dificuldade);
        if (erro != null) return erro;
        entrar(p, m);
        return null;
    }

    /** A masmorra ainda existe? */
    public Masmorra ativa(UUID id) {
        return id == null ? null : ativas.get(id);
    }

    private static String nomeMaterial(Material m) {
        return switch (m) {
            case EMERALD -> "esmeralda";
            case DIAMOND -> "diamante";
            case NETHERITE_INGOT -> "lingote de netherite";
            default -> m.name().toLowerCase();
        };
    }

    /** A cada tick: constrói um pouco de cada masmorra que está se formando. */
    public void construir() {
        for (Iterator<Map.Entry<UUID, Deque<Runnable>>> it = obras.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Deque<Runnable>> en = it.next();
            Masmorra m = ativas.get(en.getKey());
            if (m == null) {
                it.remove();
                continue;
            }
            Deque<Runnable> fila = en.getValue();
            for (int i = 0; i < 2 && !fila.isEmpty(); i++) fila.poll().run();
            if (fila.isEmpty()) {
                it.remove();
                pronta(m);
            }
        }
    }

    private void pronta(Masmorra m) {
        m.estado = Masmorra.Estado.PRONTA;
        long minutos = m.cristal ? 15 - (m.mods.contains(ModificadorPortal.RELOGIO_CURTO) ? 4 : 0) + m.minutosExtra : cfg().masTempoLimiteMin;
        m.limite = System.currentTimeMillis() + minutos * 60_000L;
        m.inicioMs = System.currentTimeMillis();
        m.duracaoMs = minutos * 60_000L;
        prepararObjetivo(m);
        Player dono = Bukkit.getPlayer(m.dono);
        if (m.publica) return; // o Portal do mundo cuida de quem entra
        Location estacao = origem.get(m.id);
        if (estacao == null) {
            if (dono != null) entrar(dono, m);
            else encerrar(m, null);
            return;
        }
        portais.put(estacao, new Portal(m.id, System.currentTimeMillis() + PORTAL_MS));
        estacao.getWorld().playSound(estacao, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1.2f);
        List<Player> avisar = new ArrayList<>();
        if (dono != null) avisar.add(dono);
        Party pt = plugin.parties().party(m.dono);
        if (pt != null) for (Player o : pt.online()) if (!avisar.contains(o)) avisar.add(o);
        for (Player o : avisar) {
            o.sendMessage(Component.text("۞ O portal " + m.rank().nomeCompleto() + " (" + m.tema.nome() + ", " + m.objetivo.nome()
                    + ") está aberto! Suba no altar em até 30 s.", m.rank().cor()));
            if (o.getWorld().equals(estacao.getWorld()) && o.getLocation().distanceSquared(estacao) < 40 * 40) {
                o.showTitle(Title.title(Component.text("۞ Portal aberto ۞", COR, TextDecoration.BOLD),
                        Component.text("Suba no portal para entrar", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(500))));
            }
        }
    }

    // =====================================================================
    //  Entrar e sair
    // =====================================================================

    /** Portais abertos: partículas e quem sobe neles entra. A cada 5 ticks. */
    public void tickPortais() {
        ciclo++;
        long agora = System.currentTimeMillis();
        for (Iterator<Map.Entry<Location, Portal>> it = portais.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, Portal> en = it.next();
            Location est = en.getKey();
            Masmorra m = ativas.get(en.getValue().masmorra);
            if (m == null || agora > en.getValue().ate) {
                it.remove();
                if (m != null && m.participantes.isEmpty()) encerrar(m, null);
                continue;
            }
            World w = est.getWorld();
            if (!est.isChunkLoaded()) continue;
            double cx = est.getBlockX() + 0.5, cy = est.getBlockY() + 1, cz = est.getBlockZ() + 0.5;
            for (int i = 0; i < 12; i++) {
                double ang = Math.toRadians(i * 30 + ciclo * 18);
                w.spawnParticle(Particle.PORTAL, cx + Math.cos(ang) * 0.9, cy + (i % 3) * 0.7, cz + Math.sin(ang) * 0.9, 1, 0, 0, 0, 0);
            }
            w.spawnParticle(Particle.REVERSE_PORTAL, cx, cy + 1, cz, 6, 0.3, 0.6, 0.3, 0.01);
            if (ciclo % 8 == 0) w.playSound(est, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 1.4f);
            for (Player p : w.getPlayers()) {
                Location l = p.getLocation();
                double dx = l.getX() - cx, dz = l.getZ() - cz, dy = l.getY() - cy;
                if (dx * dx + dz * dz > 1.2 * 1.2 || dy < -0.5 || dy > 2.5) continue;
                tentarEntrar(p, m);
            }
        }
    }

    private void tentarEntrar(Player p, Masmorra m) {
        if (m.participantes.contains(p.getUniqueId()) || m.sairam.contains(p.getUniqueId())) return;
        boolean pode = p.getUniqueId().equals(m.dono) || plugin.parties().mesmaParty(m.dono, p.getUniqueId());
        if (!pode) {
            p.sendActionBar(Component.text("✖ Esse portal é da party de outra pessoa.", NamedTextColor.RED));
            return;
        }
        if (de(p) != null) return;
        entrar(p, m);
    }

    private void entrar(Player p, Masmorra m) {
        Location volta = p.getLocation().clone();
        org.bukkit.util.Vector frente = volta.getDirection().setY(0);
        if (!ehMundo(volta.getWorld()) && frente.lengthSquared() > 1e-4) {
            // Um passo para trás, para não cair de novo no portal ao voltar.
            volta.subtract(frente.normalize().multiply(1.5));
        }
        retornos.put(p.getUniqueId(), volta);
        p.getPersistentDataContainer().set(K_RETORNO, PersistentDataType.STRING, texto(volta));
        m.participantes.add(p.getUniqueId());
        // 2.68: o que ele tinha ao entrar (o que pegar lá dentro fica se o tempo acabar com ele lá).
        List<ItemStack> tinha = new ArrayList<>();
        for (ItemStack i : p.getInventory().getContents()) if (i != null && !i.isEmpty()) tinha.add(i.clone());
        m.aoEntrar.putIfAbsent(p.getUniqueId(), tinha);
        Location entrada = m.inicio.entrada != null ? m.inicio.entrada : m.inicio.centro(mundo);
        p.teleport(entrada);
        p.playSound(entrada, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        Rank r = m.rank();
        p.showTitle(Title.title(Component.text("۞ " + m.tema.nome() + " ۞", r.cor(), TextDecoration.BOLD),
                Component.text(r.nomeCompleto() + " • " + m.objetivo.nome() + " • " + m.salas.size() + " salas", r.cor()),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))));
        p.sendMessage(Component.text("۞ Objetivo: " + m.objetivo.descricao() + " Morreu? Seus itens vão para o Cofre das Almas.", r.cor()));
        if (!m.mods.isEmpty()) {
            for (ModificadorPortal x : m.mods) p.sendMessage(Component.text("   " + (x.bom() ? "✚ " : "☠ ") + x.nome() + ": " + x.descricao(),
                    x.bom() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        if (m.cristal) p.sendMessage(Component.text("   ⏱ Saia antes do tempo acabar: quem fica perde o que pegou aqui dentro.", NamedTextColor.GOLD));
        if (m.vermelho) p.sendMessage(Component.text("   ⛔ PORTAL VERMELHO: não há saída até vencer.", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        else p.sendMessage(Component.text("   Para sair: /masmorra sair", NamedTextColor.GRAY));
    }

    /** Portal Vermelho ainda não vencido: ninguém sai por vontade própria. */
    public boolean presoNoVermelho(Masmorra m) {
        return m != null && m.vermelho && m.estado != Masmorra.Estado.CONCLUIDA;
    }

    /**
     * O tempo acabou com ele lá dentro: o que pegou na masmorra (o que não tinha ao entrar) vai
     * para o Cofre das Almas. O que ele levou fica com ele.
     */
    private void perderOQuePegou(Player p, Masmorra m) {
        List<ItemStack> antes = m.aoEntrar.get(p.getUniqueId());
        if (antes == null) return;
        List<ItemStack> sobra = new ArrayList<>();
        for (ItemStack i : antes) sobra.add(i.clone());
        List<ItemStack> pegou = new ArrayList<>();
        ItemStack[] c = p.getInventory().getContents();
        for (int s = 0; s < c.length; s++) {
            ItemStack i = c[s];
            if (i == null || i.isEmpty()) continue;
            int resta = i.getAmount();
            for (ItemStack a : sobra) {
                if (a.getAmount() <= 0 || !mesmoItem(a, i)) continue;
                int usa = Math.min(resta, a.getAmount());
                a.setAmount(a.getAmount() - usa);
                resta -= usa;
                if (resta == 0) break;
            }
            if (resta > 0) {
                ItemStack g = i.clone();
                g.setAmount(resta);
                pegou.add(g);
                int fica = i.getAmount() - resta;
                if (fica <= 0) c[s] = null;
                else i.setAmount(fica);
            }
        }
        p.getInventory().setContents(c);
        if (pegou.isEmpty()) return;
        cofre.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(pegou);
        salvarCofre();
        p.sendMessage(Component.text("⏱ O portal desabou com você lá dentro: o que você pegou nele (" + pegou.size()
                + " item(ns)) foi para o Cofre das Almas.", NamedTextColor.GOLD));
    }

    /** O mesmo item, sem contar a quantidade e o desgaste (uma espada usada lá dentro continua sendo a sua). */
    private static boolean mesmoItem(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) return false;
        ItemStack x = a.clone(), y = b.clone();
        x.setAmount(1);
        y.setAmount(1);
        if (x.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) x.editMeta(org.bukkit.inventory.meta.Damageable.class, d -> d.setDamage(0));
        if (y.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable) y.editMeta(org.bukkit.inventory.meta.Damageable.class, d -> d.setDamage(0));
        return x.isSimilar(y);
    }

    /** Tira o jogador da masmorra (volta para onde entrou). */
    public void sair(Player p, Masmorra m, String motivo) {
        m.participantes.remove(p.getUniqueId());
        m.sairam.add(p.getUniqueId());
        Location volta = retorno(p);
        limparRetorno(p);
        if (ehMundo(p.getWorld())) p.teleport(volta);
        if (motivo != null) p.sendMessage(Component.text("۞ " + motivo, COR));
        if (m.participantes.isEmpty() && m.estado != Masmorra.Estado.GERANDO && !portalAbertoPara(m) && fechaVazia(m)) encerrar(m, null);
    }

    /** Masmorra sem ninguém fecha na hora? (a de um Portal do mundo espera outros entrarem) */
    private static boolean fechaVazia(Masmorra m) {
        return !m.publica || m.estado == Masmorra.Estado.CONCLUIDA;
    }

    private boolean portalAbertoPara(Masmorra m) {
        long agora = System.currentTimeMillis();
        for (Portal p : portais.values()) if (p.masmorra.equals(m.id) && p.ate > agora) return true;
        return false;
    }

    private Location retorno(Player p) {
        Location l = retornos.get(p.getUniqueId());
        if (l == null) l = ler(p.getPersistentDataContainer().get(K_RETORNO, PersistentDataType.STRING));
        if (l == null || l.getWorld() == null || ehMundo(l.getWorld())) {
            l = p.getRespawnLocation() != null ? p.getRespawnLocation() : Bukkit.getWorlds().getFirst().getSpawnLocation();
        }
        return l;
    }

    /** Esquece para onde o jogador voltaria (ele saiu de outro jeito, ex.: morreu na Torre). */
    public void esquecerRetorno(Player p) {
        limparRetorno(p);
    }

    private void limparRetorno(Player p) {
        retornos.remove(p.getUniqueId());
        p.getPersistentDataContainer().remove(K_RETORNO);
    }

    private static String texto(Location l) {
        return l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + l.getYaw() + ";" + l.getPitch();
    }

    private static Location ler(String s) {
        if (s == null) return null;
        String[] p = s.split(";");
        if (p.length != 6) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Fecha a masmorra: tira todo mundo de lá e some com os monstros. */
    public void encerrar(Masmorra m, String motivo) {
        if (ativas.remove(m.id) == null) return;
        if (m.camara != null) m.camara.parar();
        obras.remove(m.id);
        origem.remove(m.id);
        portais.values().removeIf(p -> p.masmorra.equals(m.id));
        for (UUID id : new ArrayList<>(m.participantes)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && ehMundo(p.getWorld())) sair(p, m, motivo);
        }
        m.participantes.clear();
        for (Entity e : mundo.getEntities()) {
            if (e instanceof Player) continue;
            if (m.contem(e.getLocation())) e.remove();
        }
        chefes.values().removeIf(x -> x == m);
    }

    public void encerrarTodas() {
        for (Masmorra m : new ArrayList<>(ativas.values())) encerrar(m, "O servidor está fechando as masmorras.");
        salvarCofre();
    }

    public int quantas() {
        return ativas.size();
    }

    // =====================================================================
    //  Dentro da masmorra: a cada 10 ticks
    // =====================================================================

    public void tick() {
        long agora = System.currentTimeMillis();
        for (Masmorra m : new ArrayList<>(ativas.values())) {
            if (m.estado == Masmorra.Estado.GERANDO) continue;
            List<Player> presentes = presentes(m);
            if (!presentes.isEmpty()) m.ultimaPresenca = agora;
            else if (agora - m.ultimaPresenca > (m.publica && m.estado != Masmorra.Estado.CONCLUIDA ? 600_000 : 60_000) && !portalAbertoPara(m)) {
                encerrar(m, null);
                continue;
            }
            long falta = m.limite - agora;
            if (falta <= 0) {
                if (m.cristal && m.estado != Masmorra.Estado.CONCLUIDA) {
                    for (Player p : presentes) perderOQuePegou(p, m);
                    encerrar(m, "O tempo acabou e o portal desabou.");
                } else {
                    encerrar(m, "O tempo da masmorra acabou. Você voltou com tudo o que tinha.");
                }
                continue;
            }
            efeitosDosModificadores(m, presentes);
            efeitosDasBencaos(m, presentes);
            tickMonolitos(m, presentes);
            // Concluída, ela some em 3 minutos (15 enquanto a Câmara das Estátuas não terminou).
            if (m.estado == Masmorra.Estado.CONCLUIDA && agora - m.concluidaEm > (m.camara != null && !m.camara.terminou() ? 900_000 : 180_000)) {
                encerrar(m, "A masmorra se desfez.");
                continue;
            }
            for (Sala s : m.salas) atualizar(m, s, presentes, agora);
            if (m.estado == Masmorra.Estado.CONCLUIDA && m.saida != null) portalDeSaida(m, presentes);
            if (m.camara != null) m.camara.portalDeEntrada(presentes);
            if (ciclo % 2 == 0) hud(m, presentes, falta);
        }
    }

    private List<Player> presentes(Masmorra m) {
        List<Player> l = new ArrayList<>();
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !p.isDead() && ehMundo(p.getWorld()) && m.contem(p.getLocation())) l.add(p);
        }
        return l;
    }

    /**
     * 2.77: o resumo do portal para o HUD do mod de apoio (null fora de masmorra): título, cor,
     * segundos que faltam, objetivo e, na Câmara das Estátuas, o mandamento e os olhos.
     */
    public com.google.gson.JsonObject resumoHud(Player p) {
        Masmorra m = de(p);
        if (m == null || m.estado == Masmorra.Estado.GERANDO) return null;
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("titulo", (m.monarca != null ? m.monarca.nome : m.ataque ? "Ataque de Rank S" : m.rank().nomeCompleto()) + (m.vermelho ? " ⛔" : ""));
        o.addProperty("cor", (m.vermelho ? 0xD50000 : m.rank().cor().value()));
        o.addProperty("falta", Math.max(0, (m.limite - System.currentTimeMillis()) / 1000));
        String objetivo = switch (m.objetivo) {
            case SAQUE -> "Relíquias " + m.reliquiasPegas + "/" + m.reliquiasPrecisa;
            case MONOLITOS -> "Monólitos " + m.monolitosVencidos + "/" + m.monolitosTotal;
            case CACADA -> "Salas " + m.salasLimpas() + "/" + m.salasDeLuta();
        };
        if (m.estado == Masmorra.Estado.CONCLUIDA) objetivo = "Concluída: saia pelo portal";
        o.addProperty("objetivo", objetivo);
        if (m.camara != null && m.camara.dentroDela(p.getLocation()) && m.camara.olhos() != null) {
            com.google.gson.JsonObject c = new com.google.gson.JsonObject();
            c.addProperty("mandamento", m.camara.mandamento());
            c.addProperty("olhos", m.camara.olhos());
            o.add("camara", c);
        }
        return o;
    }

    private void hud(Masmorra m, List<Player> presentes, long falta) {
        long seg = falta / 1000;
        String objetivo = switch (m.objetivo) {
            case SAQUE -> "Relíquias " + m.reliquiasPegas + "/" + m.reliquiasPrecisa;
            case MONOLITOS -> "Monólitos " + m.monolitosVencidos + "/" + m.monolitosTotal;
            case CACADA -> "Salas " + m.salasLimpas() + "/" + m.salasDeLuta();
        };
        if (m.estado == Masmorra.Estado.CONCLUIDA) objetivo = "✔ Concluída: saia pelo portal";
        Component c = Component.text("۞ " + m.rank().nome() + (m.vermelho ? " ⛔" : ""), m.vermelho ? NamedTextColor.DARK_RED : m.rank().cor())
                .append(Component.text("  •  " + objetivo, NamedTextColor.WHITE))
                .append(Component.text(String.format("  •  ⏱ %d:%02d", seg / 60, seg % 60), seg < 300 ? NamedTextColor.RED : NamedTextColor.GRAY));
        // Com o mod de apoio, isto aparece no HUD dele (a barra de ação fica livre).
        for (Player p : presentes) if (plugin.canalApoio() == null || !plugin.canalApoio().temMod(p)) p.sendActionBar(c);
    }

    private void atualizar(Masmorra m, Sala s, List<Player> presentes, long agora) {
        List<Player> naSala = new ArrayList<>();
        for (Player p : presentes) if (s.dentro(p.getLocation(), -0.5)) naSala.add(p);
        switch (s.estado) {
            case AGUARDANDO -> {
                if (s.tipo != TipoSala.COMBATE && s.tipo != TipoSala.CHEFE) {
                    s.estado = Sala.Estado.LIMPA;
                    return;
                }
                boolean entrou = false;
                for (Player p : naSala) if (s.dentro(p.getLocation(), 2)) entrou = true;
                if (entrou) iniciarSala(m, s, presentes);
            }
            case LUTANDO -> {
                if (naSala.isEmpty()) {
                    if (s.semJogadoresDesde == 0) s.semJogadoresDesde = agora;
                    else if (agora - s.semJogadoresDesde > 25_000) reiniciarSala(m, s);
                    return;
                }
                s.semJogadoresDesde = 0;
                if (s.tipo == TipoSala.CHEFE) {
                    Entity chefe = s.chefe == null ? null : Bukkit.getEntity(s.chefe);
                    if (chefe == null || !chefe.isValid()) reiniciarSala(m, s); // sumiu sem morrer
                    else if (chefe instanceof LivingEntity vivo) fasesDoChefe(m, s, vivo, naSala, agora);
                    return;
                }
                s.mobs.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || e.isDead() || !e.isValid());
                // Monstro que saiu da sala volta para dentro.
                for (UUID id : s.mobs) {
                    if (Bukkit.getEntity(id) instanceof LivingEntity e && !s.dentro(e.getLocation(), -2)) e.teleport(pontoLivre(s, naSala));
                }
                if (!s.mobs.isEmpty()) return;
                if (s.onda < ondas(m)) {
                    if (s.proximaOnda == 0) {
                        s.proximaOnda = agora + 2500;
                        for (Player p : naSala) p.sendActionBar(Component.text("⚔ Próxima onda!", NamedTextColor.RED));
                    } else if (agora >= s.proximaOnda) {
                        s.proximaOnda = 0;
                        onda(m, s, naSala, presentes.size());
                    }
                } else {
                    limparSala(m, s, naSala);
                }
            }
            case LIMPA -> { }
        }
    }

    private void iniciarSala(Masmorra m, Sala s, List<Player> presentes) {
        s.estado = Sala.Estado.LUTANDO;
        s.onda = 0;
        s.proximaOnda = 0;
        s.semJogadoresDesde = 0;
        fecharPortas(m, s, presentes);
        if (s.tipo == TipoSala.CHEFE) {
            iniciarChefe(m, s, presentes);
        } else {
            List<Player> naSala = new ArrayList<>();
            for (Player p : presentes) if (s.dentro(p.getLocation(), -0.5)) naSala.add(p);
            for (Player p : naSala) p.showTitle(Title.title(Component.empty(), Component.text("⚔ As portas se fecharam!", NamedTextColor.RED),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(300))));
            onda(m, s, naSala, presentes.size());
        }
    }

    private void reiniciarSala(Masmorra m, Sala s) {
        for (UUID id : s.mobs) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        s.mobs.clear();
        if (s.chefe != null) {
            chefes.remove(s.chefe);
            Entity e = Bukkit.getEntity(s.chefe);
            if (e != null) e.remove();
            s.chefe = null;
        }
        abrirPortas(m, s);
        s.estado = Sala.Estado.AGUARDANDO;
    }

    private void limparSala(Masmorra m, Sala s, List<Player> naSala) {
        s.estado = Sala.Estado.LIMPA;
        abrirPortas(m, s);
        int xp = 15 * (m.dificuldade.nivel() + 1);
        for (Player p : naSala) {
            plugin.stats().darXp(p, Skill.COMBATE, xp);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
            p.showTitle(Title.title(Component.empty(), Component.text("✔ Sala limpa! As portas abriram.", NamedTextColor.GREEN),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }
        // Às vezes a sala deixa um baú de recompensa.
        if (rnd().nextDouble() < 0.3) {
            Block b = mundo.getBlockAt(s.centroX, s.minY + 1, s.centroZ);
            if (b.isEmpty()) {
                b.setType(Material.CHEST, false);
                if (b.getState() instanceof Chest bau) Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, rolagens(m, 2), false, new Random(), true);
                mundo.spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 1, 0.5), 30, 0.4, 0.5, 0.4, 0.2);
            }
        }
    }

    // ---------- portas ----------

    private void fecharPortas(Masmorra m, Sala s, List<Player> presentes) {
        for (Sala.Porta d : s.portas) {
            BlockFace f = d.face();
            // Quem está parado na porta é empurrado para dentro antes das grades descerem.
            for (Player p : presentes) {
                if (naPorta(s, d, p.getLocation())) p.teleport(p.getLocation().add(-f.getModX() * 2.0, 0, -f.getModZ() * 2.0));
            }
            porta(s, d, Material.IRON_BARS);
        }
        mundo.playSound(s.centro(mundo), Sound.BLOCK_IRON_DOOR_CLOSE, 1.5f, 0.6f);
    }

    private void abrirPortas(Masmorra m, Sala s) {
        for (Sala.Porta d : s.portas) porta(s, d, Material.AIR);
        mundo.playSound(s.centro(mundo), Sound.BLOCK_IRON_DOOR_OPEN, 1.5f, 0.8f);
    }

    private static boolean naPorta(Sala s, Sala.Porta d, Location l) {
        BlockFace f = d.face();
        double plano = switch (f) {
            case EAST -> s.maxX + 0.5;
            case WEST -> s.minX + 0.5;
            case SOUTH -> s.maxZ + 0.5;
            default -> s.minZ + 0.5;
        };
        boolean emX = f == BlockFace.EAST || f == BlockFace.WEST;
        double dist = Math.abs((emX ? l.getX() : l.getZ()) - plano);
        double lado = Math.abs((emX ? l.getZ() : l.getX()) - (d.linha() + 0.5));
        return dist < 1.3 && lado < 2 && Math.abs(l.getY() - (d.y() + 1)) < 2.5;
    }

    /** As grades (ou o ar) na porta, na altura do piso dela. */
    private void porta(Sala s, Sala.Porta d, Material m) {
        for (int lado = -1; lado <= 1; lado++) {
            for (int dy = 1; dy <= 4; dy++) {
                int[] xz = Gerador.naParede(s, d, lado);
                mundo.getBlockAt(xz[0], d.y() + dy, xz[1]).setType(m, true);
            }
        }
    }

    // ---------- monstros ----------

    private void onda(Masmorra m, Sala s, List<Player> naSala, int jogadores) {
        s.onda++;
        int t = m.dificuldade.nivel();
        int quantos = Math.min(14, 3 + t + (int) Math.round((Math.max(1, jogadores) - 1) * 1.5) + s.onda - 1);
        List<EntityType> tipos = m.dificuldade.monstros();
        for (int i = 0; i < quantos; i++) {
            Location l;
            EntityType tipo = tipos.get(rnd().nextInt(tipos.size()));
            if (!s.pontosMonstro.isEmpty()) {
                Location marca = s.pontosMonstro.get(i % s.pontosMonstro.size());
                EntityType pedido = gerador.tiposMarcados.get(marca);
                if (pedido != null) tipo = pedido;
                l = marca.clone().add(0.5 + rnd().nextDouble(-0.8, 0.8), 0, 0.5 + rnd().nextDouble(-0.8, 0.8));
            } else {
                l = pontoLivre(s, naSala);
            }
            // Ravager só um por onda (é grande).
            if (tipo == EntityType.RAVAGER && i > 0) tipo = EntityType.VINDICATOR;
            LivingEntity mob = monstro(m, l, tipo);
            if (mob != null) s.mobs.add(mob.getUniqueId());
        }
        for (Player p : naSala) {
            p.sendActionBar(Component.text("⚔ Onda " + s.onda + "/" + ondas(m), NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.5f, 1.4f);
        }
    }

    /** Um lugar livre no chão da sala, longe dos jogadores. */
    private Location pontoLivre(Sala s, List<Player> jogadores) {
        for (int tentativa = 0; tentativa < 25; tentativa++) {
            int x = rnd().nextInt(s.minX + 2, s.maxX - 1), z = rnd().nextInt(s.minZ + 2, s.maxZ - 1);
            Block pe = mundo.getBlockAt(x, s.minY + 1, z);
            if (!pe.isPassable() || pe.isLiquid() || !pe.getRelative(0, 1, 0).isPassable()) continue;
            if (!pe.getRelative(0, -1, 0).getType().isSolid()) continue; // nada de nascer em cima do abismo ou da água
            Location l = pe.getLocation().add(0.5, 0, 0.5);
            boolean longe = true;
            for (Player p : jogadores) if (p.getLocation().distanceSquared(l) < 16) longe = false;
            if (longe || tentativa > 20) return l;
        }
        return s.centro(mundo);
    }

    private LivingEntity monstro(Masmorra m, Location l, EntityType tipo) {
        if (tipo.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(tipo.getEntityClass())) return null;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        Dificuldade d = m.dificuldade;
        double forca = m.rank().forca();
        double vidaMods = m.mods.contains(ModificadorPortal.COURACA) ? 1.4 : 1, danoMods = m.mods.contains(ModificadorPortal.FURIA) ? 1.3 : 1;
        LivingEntity e = mundo.spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            multiplicar(mob, Attribute.MAX_HEALTH, d.vida() * forca * vidaMods);
            multiplicar(mob, Attribute.ATTACK_DAMAGE, d.dano() * forca * danoMods);
            AttributeInstance alcance = mob.getAttribute(Attribute.FOLLOW_RANGE);
            if (alcance != null) alcance.setBaseValue(Math.max(alcance.getBaseValue(), 32));
            AttributeInstance vida = mob.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) mob.setHealth(vida.getValue());
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(false);
            mob.getPersistentDataContainer().set(K_MOB, PersistentDataType.STRING, m.id.toString());
            equipar(mob, d);
        });
        mundo.spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        if (e instanceof Mob mob) {
            Player alvo = null;
            double melhor = Double.MAX_VALUE;
            for (UUID id : m.participantes) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || !ehMundo(p.getWorld())) continue;
                double d2 = p.getLocation().distanceSquared(l);
                if (d2 < melhor) { melhor = d2; alvo = p; }
            }
            if (alvo != null) mob.setTarget(alvo);
        }
        return e;
    }

    private static void multiplicar(LivingEntity e, Attribute a, double fator) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * fator);
    }

    /** Zumbis e esqueletos vêm com armadura conforme a dificuldade (que não cai). */
    private static void equipar(LivingEntity e, Dificuldade d) {
        if (!(e instanceof Zombie) && !(e instanceof AbstractSkeleton)) return;
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;
        String m = switch (d) {
            case FACIL -> "CHAINMAIL_";
            case NORMAL -> "IRON_";
            case DIFICIL -> "DIAMOND_";
            case PESADELO -> "NETHERITE_";
        };
        eq.setHelmet(new ItemStack(Material.valueOf(m + "HELMET")));
        eq.setChestplate(new ItemStack(Material.valueOf(m + "CHESTPLATE")));
        eq.setHelmetDropChance(0);
        eq.setChestplateDropChance(0);
    }

    // ---------- chefe ----------

    private void iniciarChefe(Masmorra m, Sala s, List<Player> presentes) {
        List<Chefe> opcoes = m.dificuldade.chefes();
        // 2.74: no Portal do Monarca o chefe é o próprio Monarca (o corpo vem de um dos chefes).
        // 2.75: no Ataque de Rank S, o Rei das Formigas (corpo da Rainha Aracnídea).
        Chefe c = m.monarca != null ? m.monarca.base : m.ataque ? Chefe.RAINHA : opcoes.get(rnd().nextInt(opcoes.size()));
        m.chefeTipo = c;
        Player quem = presentes.isEmpty() ? null : presentes.getFirst();
        Location onde = s.pontoChefe != null ? s.pontoChefe : new Location(mundo, s.centroX, s.minY, s.centroZ);
        LivingEntity chefe = plugin.chefes().invocar(c, onde, quem, false);
        double vidaMonarca = m.monarca == null ? 1 : m.monarca.jogavel() ? 2.5 : 4;
        // O Ataque cresce com quem está na luta: +60% de vida por caçador a mais.
        double vidaAtaque = m.ataque ? 1.5 + 0.6 * Math.max(0, presentes.size() - 1) : 1;
        multiplicar(chefe, Attribute.MAX_HEALTH, m.dificuldade.vidaChefe() * m.rank().forca() * vidaMonarca * vidaAtaque * (m.mods.contains(ModificadorPortal.COURACA) ? 1.4 : 1));
        AttributeInstance vida = chefe.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) chefe.setHealth(vida.getValue());
        if (m.monarca != null) {
            multiplicar(chefe, Attribute.ATTACK_DAMAGE, m.monarca.jogavel() ? 1.3 : 1.6);
            plugin.chefes().renomear(chefe, m.monarca.chefe, m.monarca.cor);
            for (Player p : presentes) {
                JanelaSistema.mostrar(p, "Alerta", Component.text(m.monarca.chefe + " despertou.", m.monarca.cor),
                        Component.text(m.monarca.jogavel() ? "Quem der o golpe final herda o poder dele." : "O chefe final. Boa sorte, caçadores.", NamedTextColor.GRAY));
            }
        } else if (m.ataque) {
            plugin.chefes().renomear(chefe, "Beru, Rei das Formigas", net.kyori.adventure.text.format.TextColor.color(0x8D6E63));
            for (Player p : presentes) {
                JanelaSistema.mostrar(p, "Ataque de Rank S", Component.text("Beru, o Rei das Formigas, apareceu.", NamedTextColor.GOLD),
                        Component.text(presentes.size() + " caçador(es) na luta: ele ficou mais forte.", NamedTextColor.GRAY));
            }
        }
        s.chefe = chefe.getUniqueId();
        chefes.put(chefe.getUniqueId(), m);
    }

    /** Ondas por sala (a dificuldade e a Onda Extra). */
    private static int ondas(Masmorra m) {
        return m.dificuldade.ondas() + (m.mods.contains(ModificadorPortal.ONDA_EXTRA) ? 1 : 0);
    }

    /** Sorteios de um baú: a base, o rank e os modificadores. */
    static int rolagens(Masmorra m, int base) {
        return (int) Math.round((base + m.rank().tesouroExtra()) * ModificadorPortal.multTesouro(m.mods));
    }

    private void concluir(Masmorra m) {
        concluir(m, m.chefe);
    }

    /** Venceram (o chefe ou o objetivo): o baú e o portal de saída aparecem na sala {@code s}. */
    private void concluir(Masmorra m, Sala s) {
        if (m.estado == Masmorra.Estado.CONCLUIDA) return;
        m.estado = Masmorra.Estado.CONCLUIDA;
        m.concluidaEm = System.currentTimeMillis();
        if (s == m.chefe) {
            s.estado = Sala.Estado.LIMPA;
            s.chefe = null;
            abrirPortas(m, s);
        }
        // 2.70: a nota da corrida (tempo, mortes, salas limpas, segredos e o chefe). Nota boa = mais tesouro.
        long usado = Math.max(0, System.currentTimeMillis() - m.inicioMs);
        double fracao = m.duracaoMs > 0 ? usado / (double) m.duracaoMs : 0.5;
        int pontos = 80 + (fracao < 0.4 ? 25 : fracao < 0.6 ? 15 : fracao < 0.8 ? 5 : -10) - 15 * m.mortes + 10 * m.segredosAchados;
        int luta = m.salasDeLuta();
        pontos += luta == 0 ? 10 : (int) Math.round(20.0 * m.salasLimpas() / luta);
        if (m.chefe != null && m.chefe.estado == Sala.Estado.LIMPA) pontos += 10;
        Estatisticas.Nota nota = Estatisticas.nota(pontos);
        int segundos = (int) (usado / 1000);
        // Baú do chefe no meio da sala e o portal de saída ao lado.
        int jogadores = Math.max(1, presentes(m).size());
        Block b = mundo.getBlockAt(s.centroX, s.minY + 1, s.centroZ);
        if (!b.isEmpty()) b = mundo.getBlockAt(s.centroX - 2, s.minY + 1, s.centroZ);
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest bau) {
            bau.customName(Component.text("Tesouro do " + m.rank().nome(), m.rank().cor()));
            bau.update();
            // 2.72: Saqueador (Árvore) e Rei dos Baús (Despertar): o melhor da party.
            int extraCacador = PoderCacador.sorteiosExtra(presentes(m));
            Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, rolagens(m, 4 + m.dificuldade.nivel() + jogadores + m.tesouroExtra + nota.tesouro + extraCacador), true, new Random(), true);
            Inventory inv = bau.getBlockInventory();
            ManaCristal.Tamanho t = m.rank().ordinal() >= Rank.A.ordinal() && rnd().nextDouble() < 0.3 ? ManaCristal.Tamanho.PERFEITO : ManaCristal.Tamanho.GRANDE;
            inv.addItem(ManaCristal.criar(t, 1 + (m.rank().ordinal() >= Rank.B.ordinal() ? 1 : 0)));
            // O Grimório Oculto (a magia de abrir portais) é raro e só aparece nos ranks altos.
            double grimorio = switch (m.rank()) { case S -> 0.06; case A -> 0.03; case B -> 0.01; default -> 0; };
            if (rnd().nextDouble() < grimorio) inv.addItem(MagiaOculta.grimorio());
            // 2.71: uma peça de Caçador do rank (35%) e, com o chefe vencido, às vezes a arma dele.
            EquipamentoPortal eq = plugin.equipamentoPortal();
            if (eq != null) {
                if (rnd().nextDouble() < 0.35) inv.addItem(eq.pecaCacadorAleatoria(m.rank()));
                double arma = new double[]{0.04, 0.06, 0.08, 0.12, 0.18, 0.25}[m.rank().ordinal()];
                if (m.chefeTipo != null && m.chefe != null && m.chefe.estado == Sala.Estado.LIMPA && rnd().nextDouble() < arma) {
                    inv.addItem(eq.armaDeChefe(m.chefeTipo));
                }
            }
            // 2.74: Relíquias do Monarca (A e S) e o tesouro do Portal do Monarca.
            if (m.monarca == null && rnd().nextDouble() < Reliquias.chance(m.rank())) inv.addItem(Reliquias.sortear());
            if (m.monarca != null && m.chefe != null && m.chefe.estado == Sala.Estado.LIMPA) {
                inv.addItem(ManaCristal.criar(ManaCristal.Tamanho.PERFEITO, m.monarca.jogavel() ? 2 : 4));
                if (eq != null) inv.addItem(eq.armaDeChefe(m.chefeTipo));
                if (!m.monarca.jogavel()) {
                    for (int i = 0; i < 3; i++) inv.addItem(Inscricao.values()[rnd().nextInt(Inscricao.values().length)].criar(1));
                    if (eq != null) inv.addItem(eq.pecaCacador(Rank.S, EquipamentoPortal.Peca.values()[rnd().nextInt(EquipamentoPortal.Peca.values().length)]));
                }
            }
        }
        m.saida = new Location(mundo, s.centroX + 4.5, s.minY + 1, s.centroZ + 0.5);
        // 2.75: Masmorra Dupla: uma passagem escondida se abre atrás do chefe.
        if (m.dupla && m.camara == null) abrirCamara(m, s);
        int xp = 80 * (m.dificuldade.nivel() + 1);
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            plugin.stats().darXp(p, Skill.COMBATE, xp);
            Cacador.darXp(p, (int) Math.round(m.rank().xpCacador() * (m.vermelho ? 2 : 1) * (m.cristal ? 1.0 : 1.25)));
            plugin.titulos().registrar(p, "masmorras", 1);
            plugin.titulos().registrar(p, "masmorra_" + m.dificuldade.id(), 1);
            estatisticas.venceu(p, m.rank(), segundos, nota);
            p.showTitle(Title.title(Component.text("۞ CONCLUÍDA • NOTA ", COR, TextDecoration.BOLD).append(Component.text(nota.name(), nota.cor, TextDecoration.BOLD)),
                    Component.text("Pegue o tesouro e entre no portal de saída", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
            p.sendMessage(Component.text("۞ Nota ", COR).append(Component.text(nota.name(), nota.cor, TextDecoration.BOLD))
                    .append(Component.text("  (" + pontos + " pontos: tempo " + Estatisticas.tempo(segundos) + ", " + m.mortes + " morte(s), salas "
                            + m.salasLimpas() + "/" + luta + ", " + m.segredosAchados + " segredo(s))" + (nota.tesouro > 0 ? " • +" + nota.tesouro + " no tesouro" : ""), NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.sendMessage(Component.text("۞ A masmorra some em 3 minutos: pegue o baú e entre no portal (ou /masmorra sair).", COR));
        }
        if (m.monarca != null && m.chefe != null && m.chefe.estado == Sala.Estado.LIMPA) vencerMonarca(m);
        if (m.ataque && m.chefe != null && m.chefe.estado == Sala.Estado.LIMPA) vencerAtaque(m);
        if (m.aoConcluir != null) {
            try {
                m.aoConcluir.accept(m);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Erro ao fechar o portal da masmorra: " + ex);
            }
            return; // o Portal anuncia do jeito dele
        }
        if (m.dificuldade.nivel() >= 2) {
            Player dono = Bukkit.getPlayer(m.dono);
            plugin.getServer().broadcast(Component.text("۞ ", COR)
                    .append(Component.text(dono == null ? "Uma party" : "A party de " + dono.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" venceu uma masmorra ", NamedTextColor.GRAY))
                    .append(Component.text(m.dificuldade.nome(), m.dificuldade.cor(), TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY)));
        }
    }

    private void portalDeSaida(Masmorra m, List<Player> presentes) {
        List<Location> saidas = new ArrayList<>();
        saidas.add(m.saida);
        if (m.camara != null && m.camara.saida != null) saidas.add(m.camara.saida); // 2.75: a saída da Câmara
        for (Location s : saidas) {
            for (int i = 0; i < 10; i++) {
                double ang = Math.toRadians(i * 36 + ciclo * 20);
                mundo.spawnParticle(Particle.END_ROD, s.getX() + Math.cos(ang) * 0.8, s.getY() + (i % 3) * 0.7, s.getZ() + Math.sin(ang) * 0.8, 1, 0, 0, 0, 0);
            }
            for (Player p : presentes) {
                if (p.getLocation().distanceSquared(s) < 1.3 * 1.3) sair(p, m, "Você saiu da masmorra. Bom saque!");
            }
        }
    }

    // =====================================================================
    //  2.75: Masmorra Dupla (Câmara das Estátuas) e Ataque de Rank S
    // =====================================================================

    /** O chefe de uma Masmorra Dupla caiu: a câmara é montada embaixo do vault e uma passagem se abre. */
    private void abrirCamara(Masmorra m, Sala s) {
        Location entrada = new Location(mundo, s.centroX - 3.5, s.minY + 1, s.centroZ + 0.5);
        CamaraEstatuas c = new CamaraEstatuas(plugin, m, entrada);
        c.construir();
        m.camara = c;
        m.limite = Math.max(m.limite, System.currentTimeMillis() + 15 * 60_000L);
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            JanelaSistema.mostrar(p, "Masmorra Dupla", Component.text("O chão treme... uma passagem escondida se abriu na sala do chefe.", CamaraEstatuas.COR),
                    Component.text("Entre nela por sua conta e risco.", NamedTextColor.GRAY));
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.5f);
        }
    }

    /** O Ataque de Rank S foi contido: XP de caçador em dobro e 25% de uma Relíquia do Monarca para cada um. */
    private void vencerAtaque(Masmorra m) {
        List<String> nomes = new ArrayList<>();
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            nomes.add(p.getName());
            Cacador.darXp(p, m.rank().xpCacador());
            plugin.titulos().registrar(p, "ataque_rank_s", 1);
            if (rnd().nextDouble() < 0.25) {
                for (ItemStack sobra : p.getInventory().addItem(Reliquias.sortear()).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendMessage(Component.text("♛ Você ganhou uma Relíquia do Monarca!", MuralReliquias.COR));
            }
        }
        plugin.getServer().broadcast(Component.text("⚔ O Ataque de Rank S foi contido por ", NamedTextColor.GOLD)
                .append(Component.text(nomes.isEmpty() ? "caçadores sem nome" : String.join(", ", nomes), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GOLD)));
    }

    /** Admin: a masmorra em que o jogador está vira Dupla (a Câmara abre quando o chefe cair). */
    public String tornarDupla(Player p) {
        Masmorra m = de(p);
        if (m == null) return "Você não está numa masmorra.";
        if (m.estado == Masmorra.Estado.CONCLUIDA) return "Essa masmorra já terminou.";
        m.dupla = true;
        return null;
    }

    /** 2.75: chamado pelo Portal do Ataque de Rank S. */
    public void marcarAtaque(Masmorra m) {
        m.ataque = true;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTocarSino(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null
                || e.getClickedBlock().getType() != Material.BELL || !ehMundo(e.getClickedBlock().getWorld())) return;
        Masmorra m = de(e.getPlayer());
        if (m != null && m.camara != null) m.camara.sino(e.getClickedBlock());
    }

    // =====================================================================
    //  2.68: objetivos (Saque e Monólitos), modificadores e Cristais de Mana
    // =====================================================================

    /** Salas onde dá para pôr vasos e monólitos (nem a entrada, nem a do chefe). */
    private static List<Sala> salasDoObjetivo(Masmorra m) {
        List<Sala> l = new ArrayList<>();
        for (Sala s : m.salas) if (s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.TESOURO) l.add(s);
        Collections.shuffle(l);
        return l;
    }

    /** Põe as relíquias (vasos dourados) ou os monólitos (magnetitas) nas salas. */
    private void prepararObjetivo(Masmorra m) {
        List<Sala> salas = salasDoObjetivo(m);
        if (m.objetivo == Objetivo.SAQUE) {
            for (Sala s : salas) {
                for (int i = 0; i < 2 && m.reliquias.size() < 10; i++) {
                    Block b = pontoLivre(s, List.of()).getBlock();
                    if (!b.isEmpty() || b.getRelative(0, -1, 0).isEmpty()) continue;
                    b.setType(Material.DECORATED_POT, false);
                    m.reliquias.add(b.getLocation());
                }
            }
            if (m.reliquias.isEmpty()) m.objetivo = Objetivo.CACADA;
            else m.reliquiasPrecisa = Math.max(1, (int) Math.ceil(m.reliquias.size() * 0.75));
        } else if (m.objetivo == Objetivo.MONOLITOS) {
            for (Sala s : salas) {
                if (m.monolitos.size() >= 4) break;
                Block b = mundo.getBlockAt(s.centroX + 3, s.minY + 1, s.centroZ + 3);
                if (!b.isEmpty()) b = pontoLivre(s, List.of()).getBlock();
                if (!b.isEmpty()) continue;
                b.setType(Material.LODESTONE, false);
                m.monolitos.add(b.getLocation());
            }
            if (m.monolitos.isEmpty()) m.objetivo = Objetivo.CACADA;
            else m.monolitosTotal = m.monolitos.size();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarObjetivo(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || !ehMundo(e.getClickedBlock().getWorld())) return;
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();
        Masmorra m = de(p);
        if (m == null) return;
        Location l = b.getLocation();
        // 2.70: mímico (o baú) e Pilar de Bênção.
        if (m.mimicos.contains(l)) {
            e.setCancelled(true);
            if (e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) despertarMimico(m, p, b);
            return;
        }
        br.rpgatributos.fe.Deus deus = m.pilares.get(l);
        if (deus != null) {
            e.setCancelled(true);
            if (e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) usarPilar(m, p, b, deus);
            return;
        }
        if (m.reliquias.contains(l)) {
            e.setCancelled(true);
            if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
            m.reliquias.remove(l);
            b.setType(Material.AIR, false);
            m.reliquiasPegas++;
            Estatisticas.reliquia(p);
            mundo.spawnParticle(Particle.TOTEM_OF_UNDYING, l.clone().add(0.5, 0.6, 0.5), 20, 0.3, 0.4, 0.3, 0.15);
            mundo.playSound(l, Sound.BLOCK_DECORATED_POT_SHATTER, 1f, 1f);
            for (ItemStack s : p.getInventory().addItem(ManaCristal.criar(ManaCristal.Tamanho.PEQUENO, 1 + m.rank().ordinal() / 2)).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
            for (Player o : presentes(m)) o.sendActionBar(Component.text("✦ Relíquia " + m.reliquiasPegas + "/" + m.reliquiasPrecisa, NamedTextColor.GOLD));
            if (m.reliquiasPegas >= m.reliquiasPrecisa) objetivoCumprido(m, "As relíquias estão reunidas!");
        } else if (m.monolitos.contains(l)) {
            e.setCancelled(true);
            if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
            m.monolitos.remove(l);
            Sala s = m.salaEm(l.clone().add(0.5, 0, 0.5));
            Set<UUID> mobs = new java.util.HashSet<>();
            int quantos = 3 + m.dificuldade.nivel() + presentes(m).size();
            List<EntityType> tipos = m.dificuldade.monstros();
            for (int i = 0; i < quantos; i++) {
                double ang = Math.PI * 2 * i / quantos;
                Location onde = l.clone().add(0.5 + Math.cos(ang) * 3, 1, 0.5 + Math.sin(ang) * 3);
                if (!onde.getBlock().isPassable()) onde = s != null ? pontoLivre(s, List.of(p)) : l.clone().add(0.5, 1, 0.5);
                LivingEntity mob = monstro(m, onde, tipos.get(rnd().nextInt(tipos.size())));
                if (mob != null) mobs.add(mob.getUniqueId());
            }
            m.monolitosLutando.put(l, mobs);
            b.setType(Material.CRYING_OBSIDIAN, false);
            mundo.playSound(l, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f, 0.6f);
            for (Player o : presentes(m)) o.sendActionBar(Component.text("⚡ O monólito despertou! Vença os guardiões.", NamedTextColor.RED));
        }
    }

    /** A cada 10 ticks: monólito cujos guardiões caíram fica vencido. */
    private void tickMonolitos(Masmorra m, List<Player> presentes) {
        if (m.monolitosLutando.isEmpty()) return;
        for (Iterator<Map.Entry<Location, Set<UUID>>> it = m.monolitosLutando.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Location, Set<UUID>> en = it.next();
            en.getValue().removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity x) || x.isDead() || !x.isValid());
            if (!en.getValue().isEmpty()) continue;
            it.remove();
            m.monolitosVencidos++;
            Block b = en.getKey().getBlock();
            b.setType(Material.BEACON, false);
            mundo.spawnParticle(Particle.END_ROD, en.getKey().clone().add(0.5, 1.2, 0.5), 30, 0.3, 0.6, 0.3, 0.05);
            mundo.playSound(en.getKey(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
            for (Player o : presentes) o.sendActionBar(Component.text("✦ Monólito " + m.monolitosVencidos + "/" + m.monolitosTotal, NamedTextColor.GOLD));
            if (m.monolitosVencidos >= m.monolitosTotal) objetivoCumprido(m, "Os monólitos despertaram!");
        }
    }

    private void objetivoCumprido(Masmorra m, String texto) {
        for (Player o : presentes(m)) {
            o.showTitle(Title.title(Component.text("✦ " + texto, NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("O tesouro e a saída apareceram na entrada", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        }
        concluir(m, m.inicio);
    }

    // =====================================================================
    //  2.70: fases do chefe, armadilhas, mímicos e Pilares de Bênção
    // =====================================================================

    /**
     * O chefe do vault muda de fase com 66% e 33% de vida: fica protegido por 3 s, chama reforços e
     * passa a soltar golpes marcados no chão (o círculo vermelho avisa 1,5 s antes).
     */
    private void fasesDoChefe(Masmorra m, Sala s, LivingEntity chefe, List<Player> naSala, long agora) {
        AttributeInstance max = chefe.getAttribute(Attribute.MAX_HEALTH);
        if (max == null) return;
        double rel = chefe.getHealth() / max.getValue();
        int alvo = rel < 0.33 ? 2 : rel < 0.66 ? 1 : 0;
        if (alvo > s.fase) {
            s.fase = alvo;
            chefe.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 60, 3, false, true));
            mundo.playSound(chefe.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.2f, s.fase == 2 ? 0.6f : 0.9f);
            mundo.spawnParticle(Particle.SOUL_FIRE_FLAME, chefe.getLocation().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.05);
            for (Player p : naSala) {
                p.showTitle(Title.title(Component.text("☠ Fase " + (s.fase + 1) + "/3", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                        Component.text(s.fase == 2 ? "O chefe está desesperado" : "O chefe muda de forma", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
            }
            int reforcos = 2 + m.dificuldade.nivel() + s.fase;
            List<EntityType> tipos = m.dificuldade.monstros();
            for (int i = 0; i < reforcos; i++) {
                LivingEntity mob = monstro(m, pontoLivre(s, naSala), tipos.get(rnd().nextInt(tipos.size())));
                if (mob != null) s.mobs.add(mob.getUniqueId());
            }
            s.proximoGolpe = agora + 2500;
        }
        if (s.fase >= 1 && agora >= s.proximoGolpe) {
            s.proximoGolpe = agora + (s.fase == 1 ? 7000 : 4500);
            golpeMarcado(m, s, chefe, naSala);
        }
    }

    /** Círculos vermelhos no chão (em cima de cada um, e mais alguns na fase 3); 1,5 s depois, dano em quem ficou. */
    private void golpeMarcado(Masmorra m, Sala s, LivingEntity chefe, List<Player> naSala) {
        List<Location> alvos = new ArrayList<>();
        for (Player p : naSala) {
            if (alvos.size() >= 4) break;
            alvos.add(p.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5));
        }
        if (s.fase >= 2) for (int i = 0; i < 2; i++) alvos.add(pontoLivre(s, naSala).clone().add(0, 0.1, 0));
        double raio = 2.5, dano = (3 + 2 * m.dificuldade.nivel()) * m.rank().forca();
        Particle.DustOptions vermelho = new Particle.DustOptions(org.bukkit.Color.fromRGB(0xD50000), 1.2f);
        UUID idChefe = chefe.getUniqueId();
        UUID idMasmorra = m.id;
        plugin.getServer().getScheduler().runTaskTimer(plugin, new java.util.function.Consumer<org.bukkit.scheduler.BukkitTask>() {
            int passos;

            @Override
            public void accept(org.bukkit.scheduler.BukkitTask tarefa) {
                if (!ativas.containsKey(idMasmorra)) { tarefa.cancel(); return; }
                passos++;
                for (Location c : alvos) {
                    for (int i = 0; i < 20; i++) {
                        double a = Math.PI * 2 * i / 20;
                        mundo.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * raio, c.getY(), c.getZ() + Math.sin(a) * raio, 1, 0, 0, 0, 0, vermelho);
                    }
                    if (passos % 2 == 0) mundo.spawnParticle(Particle.DUST, c, 6, raio / 2, 0, raio / 2, 0, vermelho);
                }
                if (passos < 6) return;
                tarefa.cancel();
                Entity e = Bukkit.getEntity(idChefe);
                for (Location c : alvos) {
                    mundo.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 3, 0.8, 0.3, 0.8, 0);
                    mundo.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                    for (Player p : mundo.getPlayers()) {
                        Location pl = p.getLocation();
                        if (Math.abs(pl.getY() - c.getY()) < 2 && Math.hypot(pl.getX() - c.getX(), pl.getZ() - c.getZ()) <= raio) {
                            if (e instanceof LivingEntity vivo && vivo.isValid()) p.damage(dano, vivo);
                            else p.damage(dano);
                        }
                    }
                }
            }
        }, 0L, 5L);
    }

    /** Placa-armadilha pisada por um jogador: espinhos, flechas das paredes ou chamas. */
    @EventHandler(ignoreCancelled = false)
    public void aoPisar(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.PHYSICAL || e.getClickedBlock() == null || !ehMundo(e.getClickedBlock().getWorld())) return;
        Player p = e.getPlayer();
        Masmorra m = de(p);
        if (m == null) return;
        Location l = e.getClickedBlock().getLocation();
        Masmorra.Armadilha a = m.armadilhas.get(l);
        if (a == null) return;
        long agora = System.currentTimeMillis();
        if (m.armadilhaPronta.getOrDefault(l, 0L) > agora) return;
        m.armadilhaPronta.put(l, agora + 3000);
        // 2.72: Passo Leve (Árvore de Caçador): metade do dano e sem lentidão.
        boolean leve = PoderCacador.passoLeve(p);
        double dano = (2 + m.rank().ordinal()) * (leve ? 0.5 : 1);
        Location c = l.clone().add(0.5, 0.2, 0.5);
        switch (a) {
            case ESPINHOS -> {
                p.damage(dano);
                if (!leve) p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1));
                mundo.spawnParticle(Particle.CRIT, c, 25, 0.4, 0.3, 0.4, 0.2);
                mundo.playSound(c, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1f, 0.8f);
            }
            case FLECHAS -> {
                org.bukkit.util.Vector dir = p.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() < 1e-4) dir = new org.bukkit.util.Vector(1, 0, 0);
                org.bukkit.util.Vector lado = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();
                for (int i = 0; i < 3; i++) {
                    org.bukkit.util.Vector de = lado.clone().multiply(i == 1 ? -1.6 : 1.6);
                    Location saida = p.getLocation().add(de).add(0, 1.2 + (i == 2 ? 0.4 : 0), 0);
                    org.bukkit.util.Vector rumo = p.getLocation().add(0, 1, 0).toVector().subtract(saida.toVector());
                    org.bukkit.entity.Arrow f = mundo.spawnArrow(saida, rumo, 1.4f, 4f);
                    f.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
                    f.setDamage(dano / 2);
                    plugin.getServer().getScheduler().runTaskLater(plugin, f::remove, 60L);
                }
                mundo.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1f, 0.7f);
            }
            case CHAMAS -> {
                p.setFireTicks(80);
                p.damage(dano / 2);
                mundo.spawnParticle(Particle.FLAME, c, 40, 0.4, 1.2, 0.4, 0.03);
                mundo.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1f, 0.9f);
            }
        }
        p.sendActionBar(Component.text("⚠ Armadilha!", NamedTextColor.RED));
    }

    /** Mímico: o baú abre a boca e vira um monstro que guarda o tesouro. */
    private void despertarMimico(Masmorra m, Player p, Block b) {
        m.mimicos.remove(b.getLocation());
        List<ItemStack> tesouro = new ArrayList<>();
        if (b.getState() instanceof Chest bau) {
            for (ItemStack i : bau.getBlockInventory().getContents()) if (i != null && !i.isEmpty()) tesouro.add(i.clone());
            bau.getBlockInventory().clear();
        }
        b.setType(Material.AIR, false);
        Location l = b.getLocation().add(0.5, 0, 0.5);
        double forca = m.rank().forca();
        LivingEntity mob = mundo.spawn(l, org.bukkit.entity.Zombie.class, CreatureSpawnEvent.SpawnReason.CUSTOM, z -> {
            z.setAdult();
            z.customName(Component.text("☠ Mímico", NamedTextColor.GOLD, TextDecoration.BOLD));
            z.setCustomNameVisible(true);
            AttributeInstance vida = z.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) vida.setBaseValue(30 * (1 + m.dificuldade.nivel()) * forca);
            if (vida != null) z.setHealth(vida.getValue());
            AttributeInstance dano = z.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue((4 + 2 * m.dificuldade.nivel()) * forca);
            AttributeInstance escala = z.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(1.25);
            AttributeInstance vel = z.getAttribute(Attribute.MOVEMENT_SPEED);
            if (vel != null) vel.setBaseValue(0.3);
            if (z.getEquipment() != null) {
                z.getEquipment().setHelmet(new ItemStack(Material.CHEST));
                z.getEquipment().setHelmetDropChance(0);
            }
            z.setRemoveWhenFarAway(false);
            z.setPersistent(false);
            z.getPersistentDataContainer().set(K_MOB, PersistentDataType.STRING, m.id.toString());
            z.setTarget(p);
        });
        tesouroDoMimico.put(mob.getUniqueId(), tesouro);
        mundo.playSound(l, Sound.BLOCK_CHEST_CLOSE, 1.5f, 0.5f);
        mundo.playSound(l, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.6f);
        p.showTitle(Title.title(Component.empty(), Component.text("☠ É um mímico!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(1200), Duration.ofMillis(300))));
    }

    /** O mímico caiu: o tesouro do baú sai junto. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrerMimico(EntityDeathEvent e) {
        List<ItemStack> tesouro = tesouroDoMimico.remove(e.getEntity().getUniqueId());
        if (tesouro == null) return;
        e.getDrops().addAll(tesouro);
        if (e.getEntity().getKiller() != null) Estatisticas.mimico(e.getEntity().getKiller());
    }

    /** Pilar de Bênção: a party ganha a bênção do deus até o fim da corrida (mais forte para quem o segue). */
    private void usarPilar(Masmorra m, Player p, Block b, br.rpgatributos.fe.Deus d) {
        m.pilares.remove(b.getLocation());
        b.setType(Material.QUARTZ_PILLAR, false);
        m.bencaos.add(d);
        Location l = b.getLocation().add(0.5, 1, 0.5);
        mundo.spawnParticle(Particle.END_ROD, l, 40, 0.4, 0.8, 0.4, 0.05);
        mundo.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
        for (Entity x : mundo.getNearbyEntities(l, 2, 3, 2)) if (x instanceof org.bukkit.entity.TextDisplay td) td.remove();
        for (Player o : presentes(m)) {
            o.sendMessage(Component.text(d.simbolo() + " " + d.nome() + " abençoa a party: " + bencao(d) + " até o fim do portal.", d.cor()));
        }
    }

    private static String bencao(br.rpgatributos.fe.Deus d) {
        return switch (d) {
            case FERRUM -> "Resistência";
            case SYLVA -> "Regeneração";
            case MARIS -> "Velocidade e Graça do golfinho";
            case BELLUM -> "Força";
            case ARCANUS -> "Pressa e mana";
            case MORTIS -> "Absorção";
        };
    }

    /** As bênçãos dos pilares, renovadas a cada 2 s (nível II para quem segue o deus). */
    private void efeitosDasBencaos(Masmorra m, List<Player> presentes) {
        if (m.bencaos.isEmpty() || ciclo % 4 != 0) return;
        for (Player p : presentes) {
            br.rpgatributos.fe.Deus seu = plugin.deuses() == null ? null : plugin.deuses().deus(p);
            for (br.rpgatributos.fe.Deus d : m.bencaos) {
                int nv = d == seu ? 1 : 0;
                switch (d) {
                    case FERRUM -> efeito(p, org.bukkit.potion.PotionEffectType.RESISTANCE, nv);
                    case SYLVA -> efeito(p, org.bukkit.potion.PotionEffectType.REGENERATION, nv);
                    case MARIS -> {
                        efeito(p, org.bukkit.potion.PotionEffectType.SPEED, nv);
                        efeito(p, org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 0);
                    }
                    case BELLUM -> efeito(p, org.bukkit.potion.PotionEffectType.STRENGTH, nv);
                    case ARCANUS -> {
                        efeito(p, org.bukkit.potion.PotionEffectType.HASTE, nv);
                        plugin.arcano().darMana(p, nv == 1 ? 3 : 1.5);
                    }
                    case MORTIS -> efeito(p, org.bukkit.potion.PotionEffectType.ABSORPTION, nv);
                }
            }
        }
    }

    private static void efeito(Player p, org.bukkit.potion.PotionEffectType t, int nivel) {
        org.bukkit.potion.PotionEffect atual = p.getPotionEffect(t);
        if (atual != null && atual.getAmplifier() > nivel) return;
        if (t == org.bukkit.potion.PotionEffectType.ABSORPTION && atual != null && atual.getDuration() > 40) return; // não recarrega o escudo sem parar
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(t, t == org.bukkit.potion.PotionEffectType.ABSORPTION ? 400 : 60, nivel, true, false, true));
    }

    /** Bênção e Pressa: os efeitos bons, renovados enquanto está lá dentro. */
    private void efeitosDosModificadores(Masmorra m, List<Player> presentes) {
        if (m.mods.isEmpty() || ciclo % 4 != 0) return;
        for (Player p : presentes) {
            if (m.mods.contains(ModificadorPortal.BENCAO)) p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 60, 0, true, false, true));
            if (m.mods.contains(ModificadorPortal.PRESSA)) p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 60, 0, true, false, true));
        }
    }

    /** Sem Cura: a fome cheia não cura ninguém lá dentro. */
    @EventHandler(ignoreCancelled = true)
    public void aoCurar(org.bukkit.event.entity.EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p) || !ehMundo(p.getWorld())) return;
        if (e.getRegainReason() != org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED) return;
        Masmorra m = de(p);
        if (m != null && m.mods.contains(ModificadorPortal.SEM_CURA)) e.setCancelled(true);
    }

    /** Os monstros dos portais às vezes deixam um Cristal de Mana. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrerMonstro(EntityDeathEvent e) {
        String id = e.getEntity().getPersistentDataContainer().get(K_MOB, PersistentDataType.STRING);
        if (id == null) return;
        Masmorra m;
        try { m = ativas.get(UUID.fromString(id)); } catch (IllegalArgumentException ex) { return; }
        if (m == null || rnd().nextDouble() >= m.rank().chanceMana() * PoderCacador.multMana(e.getEntity().getKiller())) return;
        ManaCristal.Tamanho t = m.rank().ordinal() >= Rank.B.ordinal() && rnd().nextDouble() < 0.25 ? ManaCristal.Tamanho.MEDIO : ManaCristal.Tamanho.PEQUENO;
        e.getDrops().add(ManaCristal.criar(t, 1));
    }

    /** Cristais de Mana são botões de ametista por baixo: não se colocam no chão. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoColocarCristal(BlockPlaceEvent e) {
        if (ManaCristal.de(e.getItemInHand()) != null) e.setCancelled(true);
    }

    // =====================================================================
    //  Cofre das Almas
    // =====================================================================

    public int pacotes(UUID jogador) {
        List<List<ItemStack>> l = cofre.get(jogador);
        return l == null ? 0 : l.size();
    }

    public int itensNoCofre(UUID jogador) {
        int n = 0;
        List<List<ItemStack>> l = cofre.get(jogador);
        if (l != null) for (List<ItemStack> pacote : l) n += pacote.size();
        return n;
    }

    /** Devolve o pacote mais antigo, cobrando os diamantes. @return motivo de não ter dado certo, ou null. */
    public String recuperar(Player p) {
        List<List<ItemStack>> l = cofre.get(p.getUniqueId());
        if (l == null || l.isEmpty()) return "Seu Cofre das Almas está vazio.";
        int custo = cfg().masCustoCofre;
        if (p.getGameMode() != GameMode.CREATIVE) {
            if (!p.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), custo)) return "O Cofre das Almas pede " + custo + " diamantes.";
            p.getInventory().removeItem(new ItemStack(Material.DIAMOND, custo));
        }
        List<ItemStack> pacote = l.removeFirst();
        if (l.isEmpty()) cofre.remove(p.getUniqueId());
        for (ItemStack i : pacote) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        salvarCofre();
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.8f);
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.03);
        p.sendMessage(Component.text("۞ Você recuperou " + pacote.size() + " item(ns) do Cofre das Almas.", COR));
        return null;
    }

    private void carregarCofre() {
        cofre.clear();
        if (!arquivoCofre.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivoCofre);
        ConfigurationSection raiz = y.getConfigurationSection("cofre");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(id);
            if (s == null) continue;
            List<List<ItemStack>> pacotes = new ArrayList<>();
            for (String k : s.getKeys(false)) {
                List<ItemStack> pacote = new ArrayList<>();
                for (Object o : s.getList(k, List.of())) if (o instanceof ItemStack i) pacote.add(i);
                if (!pacote.isEmpty()) pacotes.add(pacote);
            }
            try {
                if (!pacotes.isEmpty()) cofre.put(UUID.fromString(id), pacotes);
            } catch (IllegalArgumentException ignorado) {
                // linha estranha no arquivo
            }
        }
    }

    private void salvarCofre() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, List<List<ItemStack>>> en : cofre.entrySet()) {
            for (int i = 0; i < en.getValue().size(); i++) y.set("cofre." + en.getKey() + "." + i, en.getValue().get(i));
        }
        try {
            y.save(arquivoCofre);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar cofre.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrerChefe(EntityDeathEvent e) {
        Masmorra m = chefes.remove(e.getEntity().getUniqueId());
        if (m == null || !ativas.containsKey(m.id)) return;
        Player matador = e.getEntity().getKiller();
        if (matador != null) m.matadorChefe = matador.getUniqueId();
        concluir(m);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Masmorra m = de(p);
        if (m == null || !ehMundo(p.getWorld())) return;
        List<ItemStack> itens = new ArrayList<>();
        for (ItemStack i : e.getDrops()) if (i != null && !i.isEmpty()) itens.add(i);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        if (!itens.isEmpty()) {
            cofre.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(itens);
            salvarCofre();
        }
        m.mortes++;
        Estatisticas.morreu(p);
        morreram.put(p.getUniqueId(), retorno(p));
        limparRetorno(p);
        m.participantes.remove(p.getUniqueId());
        m.sairam.add(p.getUniqueId());
        if (m.participantes.isEmpty() && fechaVazia(m)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (ativas.containsKey(m.id) && m.participantes.isEmpty()) encerrar(m, null);
            }, 100L);
        }
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Location volta = morreram.remove(p.getUniqueId());
        if (volta == null) return;
        e.setRespawnLocation(volta);
        int custo = cfg().masCustoCofre;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> p.sendMessage(Component.text(
                "☠ Você morreu na masmorra. Seus itens estão no Cofre das Almas: clique num Portal da Masmorra e pague "
                        + custo + " diamantes para recuperar.", NamedTextColor.LIGHT_PURPLE)), 20L);
    }

    @EventHandler
    public void aoEntrarNoServidor(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean tinhaRetorno = p.getPersistentDataContainer().has(K_RETORNO);
        Masmorra m = de(p);
        if (m != null && ehMundo(p.getWorld())) return; // voltou para a masmorra que ainda existe
        if (!tinhaRetorno && !ehMundo(p.getWorld())) return;
        // Saiu do jogo dentro de uma masmorra que já acabou (ou o servidor reiniciou): volta para fora.
        if (m != null) {
            m.participantes.remove(p.getUniqueId());
            m.sairam.add(p.getUniqueId());
        }
        Location volta = retorno(p);
        limparRetorno(p);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            p.teleport(volta);
            p.sendMessage(Component.text("۞ A masmorra em que você estava acabou. Você voltou com tudo o que tinha.", COR));
        });
    }

    @EventHandler
    public void aoTrocarDeMundo(PlayerChangedWorldEvent e) {
        if (!ehMundo(e.getFrom())) return;
        Player p = e.getPlayer();
        Masmorra m = de(p);
        if (m == null) return;
        // Saiu por outro caminho (comando, magia de retorno...): conta como saída.
        m.participantes.remove(p.getUniqueId());
        m.sairam.add(p.getUniqueId());
        limparRetorno(p);
        if (m.participantes.isEmpty() && !portalAbertoPara(m) && fechaVazia(m)) encerrar(m, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTeleportar(PlayerTeleportEvent e) {
        if (!ehMundo(e.getFrom().getWorld())) return;
        PlayerTeleportEvent.TeleportCause c = e.getCause();
        if (c == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || c == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("✖ A magia da masmorra impede esse teleporte.", NamedTextColor.RED));
        }
    }

    // ---------- as paredes da masmorra não quebram ----------

    private boolean protegido(Player p, World w) {
        return ehMundo(w) && !plugin.territorios().ignorando(p);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (!protegido(e.getPlayer(), e.getBlock().getWorld())) return;
        e.setCancelled(true);
        Masmorra m = de(e.getPlayer());
        if (m == null) return;
        Location l = e.getBlock().getLocation();
        // 2.69: a parede rachada da sala secreta cai inteira com um golpe.
        Integer grupo = m.segredos.get(l);
        if (grupo != null) {
            for (java.util.Iterator<Map.Entry<Location, Integer>> it = m.segredos.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Location, Integer> en = it.next();
                if (!en.getValue().equals(grupo)) continue;
                en.getKey().getBlock().setType(Material.AIR, false);
                mundo.spawnParticle(Particle.BLOCK, en.getKey().clone().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, 0, e.getBlock().getBlockData());
                it.remove();
            }
            mundo.playSound(l, Sound.BLOCK_STONE_BREAK, 1.2f, 0.6f);
            m.segredosAchados++;
            Estatisticas.segredo(e.getPlayer());
            e.getPlayer().sendActionBar(Component.text("✦ Uma passagem secreta!", NamedTextColor.GOLD));
            return;
        }
        // 2.69: os veios de ametista das Minas de Cristal dão Cristais de Mana.
        if (m.minerios.remove(l)) {
            e.getBlock().setType(Material.AIR, false);
            mundo.playSound(l, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1f);
            ManaCristal.Tamanho t = rnd().nextDouble() < 0.1 + m.rank().ordinal() * 0.03 ? ManaCristal.Tamanho.MEDIO : ManaCristal.Tamanho.PEQUENO;
            mundo.dropItemNaturally(l.clone().add(0.5, 0.5, 0.5), ManaCristal.criar(t, 1 + rnd().nextInt(2)));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (protegido(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEsvaziar(PlayerBucketEmptyEvent e) {
        if (protegido(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoEncher(PlayerBucketFillEvent e) {
        if (protegido(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        if (ehMundo(e.getLocation().getWorld())) e.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        if (ehMundo(e.getBlock().getWorld())) e.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void aoAcender(BlockIgniteEvent e) {
        if (ehMundo(e.getBlock().getWorld()) && e.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQueimar(BlockBurnEvent e) {
        if (ehMundo(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMudarBloco(EntityChangeBlockEvent e) {
        if (ehMundo(e.getBlock().getWorld()) && !(e.getEntity() instanceof Player)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoQuebrarPendurado(HangingBreakEvent e) {
        if (ehMundo(e.getEntity().getWorld()) && !(e instanceof org.bukkit.event.hanging.HangingBreakByEntityEvent h
                && h.getRemover() instanceof Player p && plugin.territorios().ignorando(p))) {
            e.setCancelled(true);
        }
    }
}
