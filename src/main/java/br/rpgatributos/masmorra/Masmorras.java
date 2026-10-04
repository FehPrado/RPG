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

    public Masmorras(RPGAtributos plugin) {
        this.plugin = plugin;
        this.personalizadas = new SalasPersonalizadas(plugin);
        this.gerador = new Gerador(plugin, personalizadas);
        this.arquivoCofre = new File(plugin.getDataFolder(), "cofre.yml");
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

    private Masmorra criar(Player dono, Dificuldade d, Location estacao) {
        Masmorra m = new Masmorra(proximaVaga++, d, Tema.sortear(), mundo, dono.getUniqueId());
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
        if (mundo == null || ativas.size() >= cfg().masMaximo + 2) return null;
        Masmorra m = criar(quem, d, null);
        m.publica = true;
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
        m.limite = System.currentTimeMillis() + cfg().masTempoLimiteMin * 60_000L;
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
            o.sendMessage(Component.text("۞ O portal da masmorra " + m.dificuldade.nome() + " (" + m.tema.nome()
                    + ") está aberto! Suba no Portal em até 30 s.", COR));
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
        Location entrada = m.inicio.entrada != null ? m.inicio.entrada : m.inicio.centro(mundo);
        p.teleport(entrada);
        p.playSound(entrada, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        p.showTitle(Title.title(Component.text("۞ " + m.tema.nome() + " ۞", COR, TextDecoration.BOLD),
                Component.text("Masmorra " + m.dificuldade.nome() + " • " + m.salas.size() + " salas", m.dificuldade.cor()),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))));
        p.sendMessage(Component.text("۞ Limpe as salas e derrote o chefe. Morreu? Seus itens vão para o Cofre das Almas."
                + " Para sair: /masmorra sair", COR));
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
                encerrar(m, "O tempo da masmorra acabou. Você voltou com tudo o que tinha.");
                continue;
            }
            if (m.estado == Masmorra.Estado.CONCLUIDA && agora - m.concluidaEm > 180_000) {
                encerrar(m, "A masmorra se desfez.");
                continue;
            }
            for (Sala s : m.salas) atualizar(m, s, presentes, agora);
            if (m.estado == Masmorra.Estado.CONCLUIDA && m.saida != null) portalDeSaida(m, presentes);
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

    private void hud(Masmorra m, List<Player> presentes, long falta) {
        long seg = falta / 1000;
        Component c = Component.text("۞ " + m.dificuldade.nome(), m.dificuldade.cor())
                .append(Component.text("  •  Salas " + m.salasLimpas() + "/" + m.salasDeLuta(), NamedTextColor.WHITE))
                .append(Component.text(String.format("  •  ⏱ %d:%02d", seg / 60, seg % 60), seg < 300 ? NamedTextColor.RED : NamedTextColor.GRAY));
        for (Player p : presentes) p.sendActionBar(c);
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
                    return;
                }
                s.mobs.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || e.isDead() || !e.isValid());
                // Monstro que saiu da sala volta para dentro.
                for (UUID id : s.mobs) {
                    if (Bukkit.getEntity(id) instanceof LivingEntity e && !s.dentro(e.getLocation(), -2)) e.teleport(pontoLivre(s, naSala));
                }
                if (!s.mobs.isEmpty()) return;
                if (s.onda < m.dificuldade.ondas()) {
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
                if (b.getState() instanceof Chest bau) Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, 2, false, new Random());
                mundo.spawnParticle(Particle.TOTEM_OF_UNDYING, b.getLocation().add(0.5, 1, 0.5), 30, 0.4, 0.5, 0.4, 0.2);
            }
        }
    }

    // ---------- portas ----------

    private void fecharPortas(Masmorra m, Sala s, List<Player> presentes) {
        for (BlockFace f : s.portas) {
            // Quem está parado na porta é empurrado para dentro antes das grades descerem.
            for (Player p : presentes) {
                if (naPorta(s, f, p.getLocation())) p.teleport(p.getLocation().add(-f.getModX() * 2.0, 0, -f.getModZ() * 2.0));
            }
            porta(s, f, Material.IRON_BARS);
        }
        mundo.playSound(s.centro(mundo), Sound.BLOCK_IRON_DOOR_CLOSE, 1.5f, 0.6f);
    }

    private void abrirPortas(Masmorra m, Sala s) {
        for (BlockFace f : s.portas) porta(s, f, Material.AIR);
        mundo.playSound(s.centro(mundo), Sound.BLOCK_IRON_DOOR_OPEN, 1.5f, 0.8f);
    }

    private static boolean naPorta(Sala s, BlockFace f, Location l) {
        double plano = switch (f) {
            case EAST -> s.maxX + 0.5;
            case WEST -> s.minX + 0.5;
            case SOUTH -> s.maxZ + 0.5;
            default -> s.minZ + 0.5;
        };
        boolean emX = f == BlockFace.EAST || f == BlockFace.WEST;
        double dist = Math.abs((emX ? l.getX() : l.getZ()) - plano);
        double lado = Math.abs((emX ? l.getZ() : l.getX()) - ((emX ? s.centroZ : s.centroX) + 0.5));
        return dist < 1.3 && lado < 2;
    }

    private void porta(Sala s, BlockFace f, Material m) {
        boolean emX = f == BlockFace.EAST || f == BlockFace.WEST;
        int plano = switch (f) {
            case EAST -> s.maxX;
            case WEST -> s.minX;
            case SOUTH -> s.maxZ;
            default -> s.minZ;
        };
        for (int lado = -1; lado <= 1; lado++) {
            for (int dy = 1; dy <= 4; dy++) {
                Block b = emX ? mundo.getBlockAt(plano, s.minY + dy, s.centroZ + lado) : mundo.getBlockAt(s.centroX + lado, s.minY + dy, plano);
                b.setType(m, true);
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
            p.sendActionBar(Component.text("⚔ Onda " + s.onda + "/" + m.dificuldade.ondas(), NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.5f, 1.4f);
        }
    }

    /** Um lugar livre no chão da sala, longe dos jogadores. */
    private Location pontoLivre(Sala s, List<Player> jogadores) {
        for (int tentativa = 0; tentativa < 25; tentativa++) {
            int x = rnd().nextInt(s.minX + 2, s.maxX - 1), z = rnd().nextInt(s.minZ + 2, s.maxZ - 1);
            Block pe = mundo.getBlockAt(x, s.minY + 1, z);
            if (!pe.isPassable() || pe.isLiquid() || !pe.getRelative(0, 1, 0).isPassable()) continue;
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
        LivingEntity e = mundo.spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            multiplicar(mob, Attribute.MAX_HEALTH, d.vida());
            multiplicar(mob, Attribute.ATTACK_DAMAGE, d.dano());
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
        Chefe c = opcoes.get(rnd().nextInt(opcoes.size()));
        Player quem = presentes.isEmpty() ? null : presentes.getFirst();
        Location onde = s.pontoChefe != null ? s.pontoChefe : new Location(mundo, s.centroX, s.minY, s.centroZ);
        LivingEntity chefe = plugin.chefes().invocar(c, onde, quem, false);
        multiplicar(chefe, Attribute.MAX_HEALTH, m.dificuldade.vidaChefe());
        AttributeInstance vida = chefe.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) chefe.setHealth(vida.getValue());
        s.chefe = chefe.getUniqueId();
        chefes.put(chefe.getUniqueId(), m);
    }

    private void concluir(Masmorra m) {
        if (m.estado == Masmorra.Estado.CONCLUIDA) return;
        m.estado = Masmorra.Estado.CONCLUIDA;
        m.concluidaEm = System.currentTimeMillis();
        Sala s = m.chefe;
        s.estado = Sala.Estado.LIMPA;
        s.chefe = null;
        abrirPortas(m, s);
        // Baú do chefe no meio da sala e o portal de saída ao lado.
        int jogadores = Math.max(1, presentes(m).size());
        Block b = mundo.getBlockAt(s.centroX, s.minY + 1, s.centroZ);
        b.setType(Material.CHEST, false);
        if (b.getState() instanceof Chest bau) {
            Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, 4 + m.dificuldade.nivel() + jogadores + m.tesouroExtra, true, new Random());
        }
        m.saida = new Location(mundo, s.centroX + 4.5, s.minY + 1, s.centroZ + 0.5);
        int xp = 80 * (m.dificuldade.nivel() + 1);
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            plugin.stats().darXp(p, Skill.COMBATE, xp);
            plugin.titulos().registrar(p, "masmorras", 1);
            plugin.titulos().registrar(p, "masmorra_" + m.dificuldade.id(), 1);
            p.showTitle(Title.title(Component.text("۞ MASMORRA CONCLUÍDA ۞", COR, TextDecoration.BOLD),
                    Component.text("Pegue o tesouro e entre no portal de saída", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.sendMessage(Component.text("۞ A masmorra some em 3 minutos: pegue o baú e entre no portal (ou /masmorra sair).", COR));
        }
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
        Location s = m.saida;
        for (int i = 0; i < 10; i++) {
            double ang = Math.toRadians(i * 36 + ciclo * 20);
            mundo.spawnParticle(Particle.END_ROD, s.getX() + Math.cos(ang) * 0.8, s.getY() + (i % 3) * 0.7, s.getZ() + Math.sin(ang) * 0.8, 1, 0, 0, 0, 0);
        }
        for (Player p : presentes) {
            if (p.getLocation().distanceSquared(s) < 1.3 * 1.3) sair(p, m, "Você saiu da masmorra. Bom saque!");
        }
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
        if (protegido(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
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
