package br.rpgatributos.torre;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.masmorra.Dificuldade;
import br.rpgatributos.masmorra.Masmorras;
import br.rpgatributos.masmorra.Tesouro;
import br.rpgatributos.party.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Torre Infinita: cada party sobe numa arena só dela (no mundo das masmorras). Andares com
 * ondas, chefe a cada 10, baú a cada 10. Morrer aqui não perde itens: a subida só acaba.
 * Ranking do andar mais alto: da semana e de todos os tempos.
 */
public final class Torre implements Listener, CommandExecutor, TabCompleter {

    public static final TextColor COR = TextColor.color(0xFFB300);
    private static final NamespacedKey K_MOB = new NamespacedKey("rpgatributos", "torre_mob");
    private static final int RAIO = 13, ALTURA = 8, Y = 80;
    private static final int S_INFO = 4, S_SEMANA = 38, S_COMO = 40, S_GERAL = 42, S_FECHAR = 49;
    private static final int[] S_INICIOS = {19, 20, 21, 22, 23, 24, 25};

    /** O visual de cada bloco de 10 andares. */
    private record Tema(String nome, Material chao, Material parede, Material luz, Material detalhe) {}

    private static final Tema[] TEMAS = {
            new Tema("Salões de Pedra", Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.GLOWSTONE, Material.CHISELED_STONE_BRICKS),
            new Tema("Profundezas", Material.DEEPSLATE_TILES, Material.POLISHED_DEEPSLATE, Material.SEA_LANTERN, Material.CHISELED_DEEPSLATE),
            new Tema("Ruínas de Barro", Material.MUD_BRICKS, Material.PACKED_MUD, Material.OCHRE_FROGLIGHT, Material.MOSSY_COBBLESTONE),
            new Tema("Fortaleza Infernal", Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.SHROOMLIGHT, Material.CHISELED_NETHER_BRICKS),
            new Tema("Bastião Negro", Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.PEARLESCENT_FROGLIGHT, Material.GILDED_BLACKSTONE),
            new Tema("Templo Afogado", Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.SEA_LANTERN, Material.PRISMARINE),
            new Tema("Cidade do Fim", Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.PEARLESCENT_FROGLIGHT, Material.PURPUR_PILLAR),
            new Tema("Abismo Silencioso", Material.DEEPSLATE_BRICKS, Material.SCULK, Material.VERDANT_FROGLIGHT, Material.REINFORCED_DEEPSLATE),
            new Tema("Salão Celestial", Material.QUARTZ_BRICKS, Material.CHISELED_QUARTZ_BLOCK, Material.SEA_LANTERN, Material.GOLD_BLOCK),
            new Tema("Trono do Vazio", Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.SHROOMLIGHT, Material.AMETHYST_BLOCK),
    };

    private enum Estado { PAUSA, LUTANDO }

    private static final class Subida {
        final int vaga;
        final UUID lider;
        final Location centro;
        final Set<UUID> jogadores = new LinkedHashSet<>();
        final Set<UUID> todos = new LinkedHashSet<>();
        final Set<UUID> mobs = new HashSet<>();
        Estado estado = Estado.PAUSA;
        int andar, melhor, onda, ondas, tema = -1;
        long proximo, proximaOnda, inicioAndar;
        UUID chefe;
        boolean chefeMorto;

        Subida(int vaga, UUID lider, Location centro) {
            this.vaga = vaga;
            this.lider = lider;
            this.centro = centro;
        }
    }

    private record Registro(String nome, int andar, long quando) {}

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location obelisco;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final Map<Integer, Subida> subidas = new LinkedHashMap<>();
    private final Map<UUID, Subida> porJogador = new HashMap<>();
    private final Map<UUID, Location> mortos = new HashMap<>();
    private final TreeSet<Integer> vagasLivres = new TreeSet<>();
    private int proximaVaga;
    private int ciclo;

    private final Map<UUID, Registro> recordes = new HashMap<>();
    private final Map<UUID, Registro> semanal = new HashMap<>();
    private String semana = "";
    private final File arquivo;

    public Torre(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "torre.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
    private Masmorras masmorras() { return plugin.masmorras(); }
    private World mundo() { return masmorras().mundo(); }

    // =====================================================================
    //  Ranking
    // =====================================================================

    private static String semanaAtual() {
        LocalDate d = LocalDate.now();
        return d.get(IsoFields.WEEK_BASED_YEAR) + "-S" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    public void carregar() {
        recordes.clear();
        semanal.clear();
        semana = semanaAtual();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ler(y.getConfigurationSection("recordes"), recordes);
        String s = y.getString("semana", "");
        if (s.equals(semana)) ler(y.getConfigurationSection("semanal"), semanal);
    }

    private static void ler(ConfigurationSection sec, Map<UUID, Registro> destino) {
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try {
                destino.put(UUID.fromString(k), new Registro(sec.getString(k + ".nome", "?"), sec.getInt(k + ".andar"), sec.getLong(k + ".quando")));
            } catch (IllegalArgumentException ignorado) {
                // linha estranha
            }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("semana", semana);
        for (Map.Entry<UUID, Registro> en : recordes.entrySet()) escrever(y, "recordes." + en.getKey(), en.getValue());
        for (Map.Entry<UUID, Registro> en : semanal.entrySet()) escrever(y, "semanal." + en.getKey(), en.getValue());
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar torre.yml: " + ex.getMessage());
        }
    }

    private static void escrever(YamlConfiguration y, String base, Registro r) {
        y.set(base + ".nome", r.nome);
        y.set(base + ".andar", r.andar);
        y.set(base + ".quando", r.quando);
    }

    /** Virou a semana: anuncia o campeão e zera o ranking semanal. */
    private void conferirSemana() {
        String atual = semanaAtual();
        if (atual.equals(semana)) return;
        List<Map.Entry<UUID, Registro>> top = top(semanal, 1);
        if (!top.isEmpty()) {
            Registro r = top.getFirst().getValue();
            plugin.getServer().broadcast(Component.text("⛩ Fim da semana na Torre Infinita! Campeão: ", COR)
                    .append(Component.text(r.nome + " (andar " + r.andar + ")", NamedTextColor.YELLOW, TextDecoration.BOLD)));
        }
        semanal.clear();
        semana = atual;
        salvar();
    }

    private static List<Map.Entry<UUID, Registro>> top(Map<UUID, Registro> m, int n) {
        List<Map.Entry<UUID, Registro>> l = new ArrayList<>(m.entrySet());
        l.sort((a, b) -> a.getValue().andar != b.getValue().andar ? Integer.compare(b.getValue().andar, a.getValue().andar)
                : Long.compare(a.getValue().quando, b.getValue().quando));
        return l.subList(0, Math.min(n, l.size()));
    }

    public int recorde(UUID id) {
        Registro r = recordes.get(id);
        return r == null ? 0 : r.andar;
    }

    private void registrar(Player p, int andar) {
        conferirSemana();
        long agora = System.currentTimeMillis();
        int topoAntes = top(recordes, 1).stream().mapToInt(en -> en.getValue().andar).findFirst().orElse(0);
        Registro r = recordes.get(p.getUniqueId());
        if (r == null || andar > r.andar) recordes.put(p.getUniqueId(), new Registro(p.getName(), andar, agora));
        Registro s = semanal.get(p.getUniqueId());
        if (s == null || andar > s.andar) semanal.put(p.getUniqueId(), new Registro(p.getName(), andar, agora));
        if (andar > topoAntes && andar >= 10 && andar % 5 == 0) {
            plugin.getServer().broadcast(Component.text("⛩ ", COR)
                    .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" chegou ao andar " + andar + " da Torre Infinita: novo recorde do servidor!", COR)));
        }
    }

    // =====================================================================
    //  Começar
    // =====================================================================

    public boolean naTorre(Player p) {
        return porJogador.containsKey(p.getUniqueId());
    }

    /** Andares de onde dá para começar (1, 11, 21... até o recorde). */
    private List<Integer> inicios(UUID id) {
        List<Integer> l = new ArrayList<>();
        int rec = recorde(id);
        for (int a = 1; a <= rec + 1; a += 10) l.add(a);
        while (l.size() > S_INICIOS.length) l.removeFirst();
        return l;
    }

    /** Começa uma subida com a party (quem estiver perto do obelisco). @return motivo de não ter começado, ou null. */
    public String iniciar(Player lider, int andarInicial, Location obelisco, boolean admin) {
        World w = mundo();
        if (w == null) return "O mundo da Torre não está disponível.";
        if (naTorre(lider) || masmorras().de(lider) != null || masmorras().ehMundo(lider.getWorld())) {
            return "Você já está numa subida ou masmorra.";
        }
        if (!admin && !inicios(lider.getUniqueId()).contains(andarInicial)) return "Você ainda não chegou a esse andar.";
        List<Player> grupo = new ArrayList<>();
        grupo.add(lider);
        Party pt = plugin.parties().party(lider);
        if (pt != null && obelisco != null) {
            for (Player o : pt.online()) {
                if (o == lider || naTorre(o) || masmorras().de(o) != null || !o.getWorld().equals(obelisco.getWorld())) continue;
                if (o.getLocation().distanceSquared(obelisco) <= 10 * 10 && !o.isDead()) grupo.add(o);
            }
        }
        int vaga = vagasLivres.isEmpty() ? proximaVaga++ : vagasLivres.pollFirst();
        Subida s = new Subida(vaga, lider.getUniqueId(), new Location(w, vaga * 300 + 0.5, Y, -100_000.5));
        subidas.put(vaga, s);
        s.andar = andarInicial - 1;
        s.melhor = s.andar;
        construir(s, tema(andarInicial));
        for (Player p : grupo) {
            masmorras().guardarRetorno(p, p.getLocation().clone());
            s.jogadores.add(p.getUniqueId());
            s.todos.add(p.getUniqueId());
            porJogador.put(p.getUniqueId(), s);
            p.teleport(s.centro.clone().add(rnd().nextDouble(-1.5, 1.5), 0, rnd().nextDouble(-1.5, 1.5)));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.7f);
            p.showTitle(Title.title(Component.text("⛩ Torre Infinita ⛩", COR, TextDecoration.BOLD),
                    Component.text("O andar " + andarInicial + " começa em instantes...", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(600))));
            p.sendMessage(Component.text("⛩ Morrer na Torre não perde itens: a subida só acaba. Sair: /torre sair", COR));
        }
        s.estado = Estado.PAUSA;
        s.proximo = System.currentTimeMillis() + 4000;
        return null;
    }

    private static int tema(int andar) {
        return Math.floorMod((andar - 1) / 10, TEMAS.length);
    }

    /** Arena redonda e fechada, com o visual do tema. */
    private void construir(Subida s, int tema) {
        s.tema = tema;
        Tema t = TEMAS[tema];
        World w = s.centro.getWorld();
        int cx = s.centro.getBlockX(), cz = s.centro.getBlockZ();
        for (int dx = -RAIO - 2; dx <= RAIO + 2; dx++) {
            for (int dz = -RAIO - 2; dz <= RAIO + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > RAIO + 1.5) continue;
                boolean parede = d > RAIO - 0.5;
                Material chao = d > RAIO - 3.5 && d <= RAIO - 2.5 ? t.detalhe : (dx == 0 && dz == 0 ? t.luz : t.chao);
                w.getBlockAt(cx + dx, Y - 1, cz + dz).setType(chao, false);
                w.getBlockAt(cx + dx, Y - 2, cz + dz).setType(Material.BEDROCK, false);
                for (int dy = 0; dy < ALTURA; dy++) {
                    Material m = Material.AIR;
                    if (parede) {
                        double ang = Math.atan2(dz, dx);
                        boolean luz = dy == 3 && Math.floorMod((int) Math.round(ang / (Math.PI / 8)), 2) == 0;
                        m = luz ? t.luz : t.parede;
                    }
                    w.getBlockAt(cx + dx, Y + dy, cz + dz).setType(m, false);
                }
                boolean luzTeto = Math.floorMod(dx, 6) == 0 && Math.floorMod(dz, 6) == 0;
                w.getBlockAt(cx + dx, Y + ALTURA, cz + dz).setType(luzTeto ? t.luz : t.parede, false);
            }
        }
    }

    // =====================================================================
    //  A cada meio segundo
    // =====================================================================

    public void tick() {
        ciclo++;
        if (ciclo % 120 == 0) conferirSemana();
        long agora = System.currentTimeMillis();
        for (Subida s : new ArrayList<>(subidas.values())) {
            List<Player> presentes = new ArrayList<>();
            for (UUID id : new ArrayList<>(s.jogadores)) {
                Player p = Bukkit.getPlayer(id);
                boolean dentro = p != null && !p.isDead() && p.getWorld().equals(s.centro.getWorld())
                        && p.getLocation().distanceSquared(s.centro) < (RAIO + 4) * (RAIO + 4);
                if (dentro) {
                    presentes.add(p);
                    continue;
                }
                if (p != null && p.isDead()) continue; // o evento de morte cuida
                s.jogadores.remove(id);
                porJogador.remove(id);
                if (p != null) {
                    if (masmorras().ehMundo(p.getWorld())) masmorras().voltar(p);
                    else masmorras().esquecerRetorno(p);
                    p.sendMessage(Component.text("⛩ Você saiu da Torre no andar " + s.andar + ".", COR));
                }
            }
            if (s.jogadores.isEmpty()) {
                terminar(s);
                continue;
            }
            switch (s.estado) {
                case PAUSA -> {
                    if (agora >= s.proximo) comecarAndar(s, presentes);
                }
                case LUTANDO -> lutar(s, presentes, agora);
            }
            if (ciclo % 2 == 0) hud(s, presentes);
        }
    }

    private void comecarAndar(Subida s, List<Player> presentes) {
        s.andar++;
        s.estado = Estado.LUTANDO;
        s.onda = 0;
        s.proximaOnda = 0;
        s.chefe = null;
        s.chefeMorto = false;
        s.inicioAndar = System.currentTimeMillis();
        int tema = tema(s.andar);
        if (tema != s.tema) {
            construir(s, tema);
            for (Player p : presentes) p.teleport(s.centro.clone().add(rnd().nextDouble(-1.5, 1.5), 0, rnd().nextDouble(-1.5, 1.5)));
        }
        boolean chefe = s.andar % 10 == 0;
        s.ondas = chefe ? 0 : s.andar < 5 ? 1 : s.andar < 20 ? 2 : 3;
        for (Player p : presentes) {
            p.showTitle(Title.title(Component.text("⛩ Andar " + s.andar, chefe ? NamedTextColor.DARK_RED : COR, TextDecoration.BOLD),
                    Component.text(chefe ? "☠ Andar do chefe!" : TEMAS[s.tema].nome, chefe ? NamedTextColor.RED : NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), chefe ? Sound.ENTITY_WITHER_SPAWN : Sound.EVENT_RAID_HORN, 0.6f, chefe ? 1f : 1.3f);
        }
        if (chefe) chefe(s, presentes);
        else onda(s, presentes);
    }

    private void lutar(Subida s, List<Player> presentes, long agora) {
        s.mobs.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || !e.isValid() || e.isDead());
        boolean demorando = agora - s.inicioAndar > 240_000;
        for (UUID id : s.mobs) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity e)) continue;
            if (e.getLocation().distanceSquared(s.centro) > (RAIO - 1) * (RAIO - 1) || e.getLocation().getY() < Y - 3) e.teleport(ponto(s));
            if (demorando && !e.isGlowing()) e.setGlowing(true);
        }
        if (s.andar % 10 == 0) {
            if (s.chefeMorto && s.mobs.isEmpty()) {
                andarLimpo(s, presentes);
                return;
            }
            Entity c = s.chefe == null ? null : Bukkit.getEntity(s.chefe);
            if (!s.chefeMorto && (c == null || !c.isValid())) chefe(s, presentes); // sumiu sem morrer: volta
            return;
        }
        if (!s.mobs.isEmpty()) return;
        if (s.onda < s.ondas) {
            if (s.proximaOnda == 0) {
                s.proximaOnda = agora + 2000;
                for (Player p : presentes) p.sendActionBar(Component.text("⚔ Próxima onda!", NamedTextColor.RED));
            } else if (agora >= s.proximaOnda) {
                s.proximaOnda = 0;
                onda(s, presentes);
            }
            return;
        }
        andarLimpo(s, presentes);
    }

    private static Dificuldade dificuldade(int andar) {
        return Dificuldade.values()[Math.min(3, (andar - 1) / 10)];
    }

    private Location ponto(Subida s) {
        double a = rnd().nextDouble(Math.PI * 2), r = rnd().nextDouble(6, RAIO - 2);
        return s.centro.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
    }

    private void onda(Subida s, List<Player> presentes) {
        s.onda++;
        int n = s.andar;
        int quantos = Math.min(14, 2 + n / 3 + (Math.max(1, presentes.size()) - 1) * 2);
        List<EntityType> tipos = dificuldade(n).monstros();
        boolean grande = false;
        for (int i = 0; i < quantos; i++) {
            EntityType tipo = tipos.get(rnd().nextInt(tipos.size()));
            if (tipo == EntityType.RAVAGER) {
                if (grande) tipo = EntityType.VINDICATOR;
                grande = true;
            }
            LivingEntity e = monstro(s, ponto(s), tipo, presentes);
            if (e != null && n >= 15 && rnd().nextDouble() < Math.min(0.4, (n - 10) * 0.01)) plugin.perigo().tornarElite(e, n >= 40 ? 2 : 1);
        }
        if (s.ondas > 1) for (Player p : presentes) p.sendActionBar(Component.text("⚔ Onda " + s.onda + "/" + s.ondas, NamedTextColor.RED));
    }

    private LivingEntity monstro(Subida s, Location l, EntityType tipo, List<Player> presentes) {
        if (tipo.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(tipo.getEntityClass())) return null;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        double vida = 1 + plugin.settings().torVidaPorAndar * (s.andar - 1);
        double dano = 1 + plugin.settings().torDanoPorAndar * (s.andar - 1);
        LivingEntity e = l.getWorld().spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            multiplicar(mob, Attribute.MAX_HEALTH, vida);
            multiplicar(mob, Attribute.ATTACK_DAMAGE, dano);
            AttributeInstance alcance = mob.getAttribute(Attribute.FOLLOW_RANGE);
            if (alcance != null) alcance.setBaseValue(Math.max(alcance.getBaseValue(), 40));
            AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) mob.setHealth(max.getValue());
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(false);
            mob.getPersistentDataContainer().set(K_MOB, PersistentDataType.INTEGER, s.vaga);
            if (mob instanceof Zombie z) z.setShouldBurnInDay(false);
            if (mob instanceof AbstractSkeleton sk) sk.setShouldBurnInDay(false);
            if (mob instanceof PiglinAbstract pa) pa.setImmuneToZombification(true);
            if (mob instanceof Raider r) r.setCanJoinRaid(false);
        });
        s.mobs.add(e.getUniqueId());
        l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        if (e instanceof Mob m && !presentes.isEmpty()) m.setTarget(presentes.get(rnd().nextInt(presentes.size())));
        return e;
    }

    private static void multiplicar(LivingEntity e, Attribute a, double f) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * f);
    }

    private void chefe(Subida s, List<Player> presentes) {
        int faixa = s.andar / 10 - 1;
        List<Chefe> opcoes = faixa < 4 ? Dificuldade.values()[faixa].chefes() : List.of(Chefe.values());
        Chefe c = opcoes.get(rnd().nextInt(opcoes.size()));
        Location altar = s.centro.clone().add(0, -1, -8).getBlock().getLocation();
        LivingEntity boss = plugin.chefes().invocar(c, altar, presentes.isEmpty() ? null : presentes.getFirst(), false);
        multiplicar(boss, Attribute.MAX_HEALTH, 1 + 0.15 * Math.max(0, faixa));
        multiplicar(boss, Attribute.ATTACK_DAMAGE, 1 + 0.08 * Math.max(0, faixa));
        AttributeInstance vida = boss.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) boss.setHealth(vida.getValue());
        s.chefe = boss.getUniqueId();
    }

    private void andarLimpo(Subida s, List<Player> presentes) {
        s.melhor = s.andar;
        s.estado = Estado.PAUSA;
        boolean marco = s.andar % 10 == 0;
        s.proximo = System.currentTimeMillis() + (marco ? 15_000 : 5_000);
        for (Player p : presentes) {
            plugin.stats().darXp(p, Skill.COMBATE, 8 + 2 * s.andar);
            registrar(p, s.andar);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
            p.showTitle(Title.title(Component.empty(), Component.text("✔ Andar " + s.andar + " limpo!" + (marco ? " Pegue o baú!" : ""), NamedTextColor.GREEN),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }
        if (marco) {
            Block b = s.centro.getBlock();
            b.setType(Material.CHEST, false);
            if (b.getState() instanceof Chest bau) {
                Tesouro.encher(plugin, bau.getBlockInventory(), dificuldade(s.andar), Math.min(14, 3 + s.andar / 10 + presentes.size()), true, new Random());
            }
            s.centro.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, s.centro.clone().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.2);
            for (Player p : presentes) p.sendMessage(Component.text("⛩ Baú do andar " + s.andar + " no centro! Ele some quando o próximo tema começar.", COR));
            if (s.andar % 20 == 0) {
                Player lider = Bukkit.getPlayer(s.lider);
                plugin.getServer().broadcast(Component.text("⛩ ", COR)
                        .append(Component.text(lider == null ? "Uma party" : "A party de " + lider.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" chegou ao andar " + s.andar + " da Torre Infinita!", COR)));
            }
            salvar();
        }
    }

    private void hud(Subida s, List<Player> presentes) {
        Component c;
        if (s.estado == Estado.PAUSA) {
            long seg = Math.max(0, (s.proximo - System.currentTimeMillis()) / 1000);
            c = Component.text("⛩ Andar " + (s.andar + 1) + " em " + seg + "s", COR);
        } else {
            c = Component.text("⛩ Andar " + s.andar, COR, TextDecoration.BOLD)
                    .append(Component.text(s.andar % 10 == 0 ? "  •  ☠ Chefe" : "  •  Onda " + s.onda + "/" + s.ondas, NamedTextColor.WHITE))
                    .append(Component.text("  •  " + s.mobs.size() + " inimigos", NamedTextColor.GRAY));
        }
        for (Player p : presentes) p.sendActionBar(c);
    }

    /** Fim da subida: limpa a arena e manda o resumo. */
    private void terminar(Subida s) {
        subidas.remove(s.vaga);
        vagasLivres.add(s.vaga);
        for (UUID id : s.mobs) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        if (s.chefe != null) {
            Entity e = Bukkit.getEntity(s.chefe);
            if (e != null) e.remove();
        }
        World w = s.centro.getWorld();
        for (Entity e : w.getNearbyEntities(s.centro, RAIO + 3, ALTURA + 3, RAIO + 3)) if (!(e instanceof Player)) e.remove();
        for (UUID id : s.jogadores) {
            porJogador.remove(id);
            Player p = Bukkit.getPlayer(id);
            if (p != null && masmorras().ehMundo(p.getWorld())) masmorras().voltar(p);
        }
        s.jogadores.clear();
        for (UUID id : s.todos) {
            porJogador.remove(id, s);
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.sendMessage(Component.text("⛩ A subida acabou. Andares limpos: até o " + s.melhor + ". Seu recorde: "
                    + recorde(id) + ". (/torre ranking)", COR));
        }
        salvar();
    }

    /** Sai da Torre (comando). */
    public void sair(Player p) {
        Subida s = porJogador.remove(p.getUniqueId());
        if (s == null) {
            p.sendMessage(Component.text("Você não está na Torre.", NamedTextColor.RED));
            return;
        }
        s.jogadores.remove(p.getUniqueId());
        masmorras().voltar(p);
        p.sendMessage(Component.text("⛩ Você saiu da Torre. Andares limpos: até o " + s.melhor + ".", COR));
        if (s.jogadores.isEmpty()) terminar(s);
    }

    public void encerrarTodas() {
        for (Subida s : new ArrayList<>(subidas.values())) terminar(s);
        salvar();
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrerMonstro(EntityDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        for (Subida s : subidas.values()) {
            if (id.equals(s.chefe)) {
                s.chefeMorto = true;
                return;
            }
        }
        Integer vaga = e.getEntity().getPersistentDataContainer().get(K_MOB, PersistentDataType.INTEGER);
        if (vaga != null) e.setDroppedExp(e.getDroppedExp() / 2);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Subida s = porJogador.remove(p.getUniqueId());
        if (s == null || !masmorras().ehMundo(p.getWorld())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        s.jogadores.remove(p.getUniqueId());
        mortos.put(p.getUniqueId(), masmorras().ondeVolta(p));
        masmorras().esquecerRetorno(p);
        if (s.jogadores.isEmpty()) plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (subidas.containsKey(s.vaga) && s.jogadores.isEmpty()) terminar(s);
        });
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Location volta = mortos.remove(e.getPlayer().getUniqueId());
        if (volta != null) e.setRespawnLocation(volta);
    }

    // =====================================================================
    //  Menu (obelisco ou /torre)
    // =====================================================================

    public void abrirMenu(Player p, Location obelisco) {
        conferirSemana();
        Tela t = new Tela();
        t.obelisco = obelisco;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⛩ Torre Infinita"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Registro sem = semanal.get(p.getUniqueId());
        inv.setItem(S_INFO, item(Material.ENDER_EYE, Component.text("⛩ Torre Infinita", COR, TextDecoration.BOLD), List.of(
                Component.text("Seu recorde: andar " + recorde(p.getUniqueId()), NamedTextColor.YELLOW),
                Component.text("Nesta semana: andar " + (sem == null ? 0 : sem.andar), NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("Sua party sobe junto (quem estiver a", NamedTextColor.GRAY),
                Component.text("até 10 blocos do obelisco).", NamedTextColor.GRAY))));
        List<Integer> inicios = inicios(p.getUniqueId());
        for (int i = 0; i < inicios.size(); i++) {
            int a = inicios.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(TEMAS[tema(a)].nome, NamedTextColor.GRAY));
            lore.add(Component.text("Monstros como os da masmorra " + dificuldade(a).nome(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(t.obelisco != null ? Component.text("» Clique para subir", NamedTextColor.YELLOW)
                    : Component.text("Vá até um Obelisco da Torre.", NamedTextColor.DARK_GRAY));
            ItemStack it = item(a == 1 ? Material.STONE_BRICKS : TEMAS[tema(a)].chao,
                    Component.text("Começar do andar " + a, COR, TextDecoration.BOLD), lore);
            inv.setItem(S_INICIOS[i], it);
        }
        inv.setItem(S_SEMANA, item(Material.CLOCK, Component.text("Ranking da semana", COR, TextDecoration.BOLD), ranking(semanal)));
        inv.setItem(S_GERAL, item(Material.NETHER_STAR, Component.text("Ranking de todos os tempos", COR, TextDecoration.BOLD), ranking(recordes)));
        inv.setItem(S_COMO, item(Material.BOOK, Component.text("Como funciona", COR, TextDecoration.BOLD), List.of(
                Component.text("Cada andar: ondas de monstros na arena.", NamedTextColor.GRAY),
                Component.text("Cada andar fica mais forte que o outro.", NamedTextColor.GRAY),
                Component.text("A cada 10 andares: um chefe e um baú.", NamedTextColor.GRAY),
                Component.text("Dá para recomeçar do 11, 21, 31...", NamedTextColor.GRAY),
                Component.text("até onde você já chegou.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Morrer aqui NÃO perde itens:", NamedTextColor.GREEN),
                Component.text("a subida só acaba.", NamedTextColor.GREEN),
                Component.text("Sair: /torre sair", NamedTextColor.DARK_GRAY))));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static List<Component> ranking(Map<UUID, Registro> m) {
        List<Component> l = new ArrayList<>();
        List<Map.Entry<UUID, Registro>> top = top(m, 10);
        if (top.isEmpty()) l.add(Component.text("Ninguém subiu ainda.", NamedTextColor.GRAY));
        for (int i = 0; i < top.size(); i++) {
            Registro r = top.get(i).getValue();
            NamedTextColor cor = i == 0 ? NamedTextColor.GOLD : i < 3 ? NamedTextColor.YELLOW : NamedTextColor.WHITE;
            l.add(Component.text((i + 1) + ". " + r.nome + " — andar " + r.andar, cor));
        }
        return l;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        List<Integer> inicios = inicios(p.getUniqueId());
        for (int i = 0; i < inicios.size(); i++) {
            if (S_INICIOS[i] != slot) continue;
            if (t.obelisco == null || !plugin.obeliscos().eh(t.obelisco.getBlock())) {
                p.sendMessage(Component.text("Para subir, use um Obelisco da Torre (obsidiana chorona + olho do ender + bloco de ouro).", NamedTextColor.RED));
                return;
            }
            p.closeInventory();
            String erro = iniciar(p, inicios.get(i), t.obelisco, false);
            if (erro != null) {
                p.sendMessage(Component.text(erro, NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            }
            return;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    // =====================================================================
    //  /torre
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("ranking")) {
            conferirSemana();
            sender.sendMessage(Component.text("⛩ Torre Infinita — semana:", COR, TextDecoration.BOLD));
            for (Component c : ranking(semanal)) sender.sendMessage(c);
            sender.sendMessage(Component.text("⛩ Todos os tempos:", COR, TextDecoration.BOLD));
            for (Component c : ranking(recordes)) sender.sendMessage(c);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("sair")) {
            sair(p);
            return true;
        }
        abrirMenu(p, null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> l = new ArrayList<>();
        for (String s : List.of("ranking", "sair")) if (s.startsWith(args[0].toLowerCase())) l.add(s);
        return l;
    }
}
