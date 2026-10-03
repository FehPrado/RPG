package br.rpgatributos.portal;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.classe.Classe;
import br.rpgatributos.colonia.Colonia;
import br.rpgatributos.masmorra.Dificuldade;
import br.rpgatributos.masmorra.Masmorra;
import br.rpgatributos.masmorra.Masmorras;
import br.rpgatributos.masmorra.Tesouro;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Portais do mundo (Solo Leveling): aparecem sozinhos perto dos jogadores. Quem entra cai
 * numa masmorra; vencer o chefe fecha o portal. Se ninguém fechar a tempo, ele TRANSBORDA:
 * ondas de monstros e um chefe saem para a região (e para as colônias perto).
 * O raro Portal Pesadelo com o Eco do Soberano desperta o Soberano das Sombras.
 */
public final class Portais implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x40C4FF);
    private static final NamespacedKey K_MONSTRO = new NamespacedKey("rpgatributos", "portal_monstro");
    private static final int RAIO_COLONIA = 160;
    private static final int LIMITE_TRANSBORDO_SEG = 1800;

    static final class Portal {
        final UUID id;
        final Location centro;
        final float yaw;
        final Dificuldade d;
        final long criado;
        long transbordaEm;
        final boolean eco;
        UUID masmorra;
        int avisos;
        // transbordo
        boolean transbordando;
        int onda, ondas, ultimaOnda, segundos, proximaOnda;
        final Set<UUID> monstros = new HashSet<>();
        UUID chefe;
        BossBar barra;
        UUID display;

        Portal(UUID id, Location centro, float yaw, Dificuldade d, long criado, long transbordaEm, boolean eco) {
            this.id = id;
            this.centro = centro;
            this.yaw = yaw;
            this.d = d;
            this.criado = criado;
            this.transbordaEm = transbordaEm;
            this.eco = eco;
        }

        Vector frente() {
            double r = Math.toRadians(yaw);
            return new Vector(-Math.sin(r), 0, Math.cos(r));
        }

        Vector lado() {
            double r = Math.toRadians(yaw);
            return new Vector(Math.cos(r), 0, Math.sin(r));
        }

        String onde() {
            return centro.getWorld().getName() + " perto de " + centro.getBlockX() + ", " + centro.getBlockZ();
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Portal> portais = new LinkedHashMap<>();
    private final Map<UUID, Long> esperaToque = new HashMap<>();
    private final File arquivo;
    private int ciclo;
    private int segundosAteSorteio;

    public Portais(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "portais.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }
    private Masmorras masmorras() { return plugin.masmorras(); }

    public static String nome(Dificuldade d) {
        return "Portal " + d.nome();
    }

    /** Fácil dura mais, Pesadelo transborda mais rápido. */
    private long duracaoMs(Dificuldade d) {
        double mult = switch (d) {
            case FACIL -> 1.5;
            case NORMAL -> 1.25;
            case DIFICIL -> 1.0;
            case PESADELO -> 0.75;
        };
        return (long) (plugin.settings().porHorasTransbordar * mult * 3_600_000L);
    }

    private static int tesouroExtra(Dificuldade d) {
        return 2 + d.nivel() * 2;
    }

    private static String tempo(long ms) {
        long s = Math.max(0, ms / 1000);
        return s >= 3600 ? String.format("%dh%02d", s / 3600, (s % 3600) / 60) : String.format("%d:%02d", s / 60, s % 60);
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        portais.clear();
        segundosAteSorteio = plugin.settings().porIntervaloMin * 60;
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection raiz = y.getConfigurationSection("portais");
        if (raiz == null) return;
        for (String k : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(k);
            if (s == null) continue;
            World w = Bukkit.getWorld(s.getString("mundo", ""));
            Dificuldade d = Dificuldade.porId(s.getString("dificuldade", ""));
            if (w == null || d == null) continue;
            try {
                Location c = new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
                portais.put(UUID.fromString(k), new Portal(UUID.fromString(k), c, (float) s.getDouble("yaw"), d,
                        s.getLong("criado"), s.getLong("transborda"), s.getBoolean("eco")));
            } catch (IllegalArgumentException ignorado) {
                // linha estranha
            }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Portal p : portais.values()) {
            String b = "portais." + p.id + ".";
            y.set(b + "mundo", p.centro.getWorld().getName());
            y.set(b + "x", p.centro.getX());
            y.set(b + "y", p.centro.getY());
            y.set(b + "z", p.centro.getZ());
            y.set(b + "yaw", p.yaw);
            y.set(b + "dificuldade", p.d.id());
            y.set(b + "criado", p.criado);
            y.set(b + "transborda", p.transbordaEm);
            y.set(b + "eco", p.eco);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar portais.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Abrir portais
    // =====================================================================

    private Dificuldade sortearDificuldade() {
        int[] pesos = plugin.settings().porPesos;
        int total = 0;
        for (int p : pesos) total += p;
        if (total <= 0) return Dificuldade.FACIL;
        int r = rnd().nextInt(total);
        Dificuldade[] ds = Dificuldade.values();
        for (int i = 0; i < ds.length && i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return ds[i];
        }
        return Dificuldade.FACIL;
    }

    private boolean soberanoLivre() {
        return plugin.classes().donoLendaria(Classe.SOBERANO_DAS_SOMBRAS) == null;
    }

    /** Tenta abrir um portal perto de algum jogador. @return o portal, ou null. */
    public Portal sortear() {
        if (portais.size() >= plugin.settings().porMaximo) return null;
        List<Player> candidatos = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getEnvironment() != World.Environment.NORMAL || masmorras().ehMundo(p.getWorld())) continue;
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) candidatos.add(p);
        }
        if (candidatos.isEmpty()) return null;
        Player alvo = candidatos.get(rnd().nextInt(candidatos.size()));
        Location l = lugar(alvo);
        if (l == null) return null;
        Dificuldade d = sortearDificuldade();
        boolean eco = d == Dificuldade.PESADELO && soberanoLivre() && rnd().nextDouble() < plugin.settings().porChanceEco;
        return abrir(l, d, eco);
    }

    /** Um chão firme, fora de territórios e longe de outros portais. */
    private Location lugar(Player p) {
        World w = p.getWorld();
        int min = plugin.settings().porDistMin, max = plugin.settings().porDistMax;
        for (int t = 0; t < 20; t++) {
            double ang = rnd().nextDouble(Math.PI * 2), dist = rnd().nextDouble(min, max);
            int x = (int) Math.floor(p.getLocation().getX() + Math.cos(ang) * dist);
            int z = (int) Math.floor(p.getLocation().getZ() + Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = w.getHighestBlockYAt(x, z);
            Block chao = w.getBlockAt(x, y, z);
            if (!chao.getType().isSolid() || chao.isLiquid() || Tag.LEAVES.isTagged(chao.getType())) continue;
            boolean livre = true;
            for (int dy = 1; dy <= 4 && livre; dy++) if (!w.getBlockAt(x, y + dy, z).isPassable() || w.getBlockAt(x, y + dy, z).isLiquid()) livre = false;
            if (!livre) continue;
            Location c = new Location(w, x + 0.5, y + 1, z + 0.5);
            if (plugin.territorios().em(c) != null) continue;
            boolean perto = false;
            for (Portal o : portais.values()) {
                if (o.centro.getWorld().equals(w) && o.centro.distanceSquared(c) < 96 * 96) perto = true;
            }
            if (perto) continue;
            // O portal fica de frente para quem estava perto.
            Vector para = p.getLocation().toVector().subtract(c.toVector()).setY(0);
            c.setYaw(para.lengthSquared() < 1e-4 ? 0 : (float) Math.toDegrees(Math.atan2(-para.getX(), para.getZ())));
            return c;
        }
        return null;
    }

    public Portal abrir(Location centro, Dificuldade d, boolean eco) {
        long agora = System.currentTimeMillis();
        Location c = centro.getBlock().getLocation().add(0.5, 0, 0.5);
        Portal p = new Portal(UUID.randomUUID(), c, centro.getYaw(), d, agora, agora + duracaoMs(d), eco);
        portais.put(p.id, p);
        salvar();
        World w = c.getWorld();
        w.strikeLightningEffect(c);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 3f, 0.7f);
        plugin.getServer().broadcast(Component.text("⛩ Um ", COR)
                .append(Component.text(nome(d), d.cor(), TextDecoration.BOLD))
                .append(Component.text(" se abriu em " + p.onde() + "! Ele transborda em " + tempo(p.transbordaEm - agora)
                        + " se ninguém o fechar. (/portais)", COR)));
        if (eco) {
            plugin.getServer().broadcast(Component.text("☾ Algo antigo chama do outro lado deste portal... (Eco do Soberano)",
                    TextColor.color(0x7E57C2), TextDecoration.ITALIC));
        }
        return p;
    }

    // =====================================================================
    //  A cada 5 ticks: partículas e quem toca o portal
    // =====================================================================

    public void tick() {
        ciclo++;
        for (Portal p : new ArrayList<>(portais.values())) {
            if (!p.centro.isChunkLoaded()) continue;
            desenhar(p);
            if (!p.transbordando) tocar(p);
        }
        if (ciclo % 4 == 0) segundo();
    }

    private void desenhar(Portal p) {
        World w = p.centro.getWorld();
        Vector lado = p.lado();
        int cor = p.transbordando ? 0xD50000 : p.d.cor().value();
        Particle.DustOptions po = new Particle.DustOptions(Color.fromRGB(cor), 1.5f);
        double alto = 2.1, largo = 1.4;
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28 + ciclo * 0.05;
            Location l = p.centro.clone().add(lado.clone().multiply(Math.cos(a) * largo)).add(0, alto + Math.sin(a) * alto, 0);
            w.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, po);
        }
        for (int i = 0; i < 8; i++) {
            double a = rnd().nextDouble(Math.PI * 2), r = Math.sqrt(rnd().nextDouble());
            Location l = p.centro.clone().add(lado.clone().multiply(Math.cos(a) * largo * r)).add(0, alto + Math.sin(a) * alto * r, 0);
            w.spawnParticle(p.transbordando ? Particle.LARGE_SMOKE : p.eco ? Particle.SCULK_SOUL : Particle.PORTAL, l, 1, 0.05, 0.05, 0.05, 0.01);
        }
        if (ciclo % 10 == 0) w.playSound(p.centro, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, p.transbordando ? 0.5f : 1.3f);
    }

    private void tocar(Portal p) {
        Vector frente = p.frente(), lado = p.lado();
        for (Player j : p.centro.getWorld().getPlayers()) {
            Vector rel = j.getLocation().toVector().subtract(p.centro.toVector());
            double dy = rel.getY();
            rel.setY(0);
            if (dy < -0.5 || dy > 3.5 || Math.abs(rel.dot(lado)) > 1.2 || Math.abs(rel.dot(frente)) > 0.8) continue;
            if (j.getGameMode() == GameMode.SPECTATOR || masmorras().ehMundo(j.getWorld())) continue;
            Long espera = esperaToque.get(j.getUniqueId());
            if (espera != null && espera > System.currentTimeMillis()) continue;
            esperaToque.put(j.getUniqueId(), System.currentTimeMillis() + 1500);
            String erro = entrar(j, p);
            if (erro != null) {
                j.sendActionBar(Component.text("⛩ " + erro, NamedTextColor.RED));
                Vector fora = rel.lengthSquared() < 1e-3 ? frente.clone() : rel.clone().normalize();
                j.setVelocity(fora.multiply(0.6).setY(0.3));
            }
        }
    }

    /** @return por que não entrou (a masmorra se formando também conta), ou null se entrou. */
    private String entrar(Player j, Portal p) {
        Masmorra m = masmorras().ativa(p.masmorra);
        if (m == null) {
            String erro = masmorras().impedimentoPortal(j, p.d);
            if (erro != null) return erro;
            m = masmorras().criarParaPortal(j, p.d, tesouroExtra(p.d), mm -> fechado(p, mm));
            if (m == null) return "O portal está instável agora (masmorras demais abertas). Tente daqui a pouco.";
            p.masmorra = m.id();
            p.centro.getWorld().playSound(p.centro, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.5f, 0.6f);
            return "O portal desperta... entre de novo em alguns segundos.";
        }
        return masmorras().entrarPortal(j, m);
    }

    // =====================================================================
    //  A cada segundo: contagem, avisos e transbordo
    // =====================================================================

    private void segundo() {
        long agora = System.currentTimeMillis();
        if (--segundosAteSorteio <= 0) {
            segundosAteSorteio = plugin.settings().porIntervaloMin * 60;
            if (rnd().nextDouble() < plugin.settings().porChance) sortear();
        }
        esperaToque.values().removeIf(t -> t < agora);
        for (Portal p : new ArrayList<>(portais.values())) {
            if (p.transbordando) {
                transbordar(p);
                continue;
            }
            long falta = p.transbordaEm - agora;
            if (falta <= 30 * 60_000L && (p.avisos & 1) == 0 && falta > 5 * 60_000L) {
                p.avisos |= 1;
                aviso(p, "transborda em 30 minutos!");
            } else if (falta <= 5 * 60_000L && (p.avisos & 2) == 0 && falta > 0) {
                p.avisos |= 3;
                aviso(p, "transborda em 5 minutos!");
            }
            if (!p.centro.isChunkLoaded()) continue;
            if (falta <= 0) {
                iniciarTransbordo(p);
                continue;
            }
            Masmorra m = masmorras().ativa(p.masmorra);
            String estado = m == null ? "Entre para fechá-lo"
                    : m.estado() == Masmorra.Estado.GERANDO ? "Se abrindo..." : m.participantes().size() + " caçador(es) lá dentro";
            atualizarPlaca(p, Component.text("⛩ " + nome(p.d), p.d.cor(), TextDecoration.BOLD)
                    .append(Component.newline())
                    .append(Component.text("Transborda em " + tempo(falta), falta < 10 * 60_000L ? NamedTextColor.RED : NamedTextColor.WHITE))
                    .append(Component.newline())
                    .append(Component.text(estado, NamedTextColor.GRAY))
                    .append(p.eco ? Component.newline().append(Component.text("☾ Eco do Soberano", TextColor.color(0x7E57C2))) : Component.empty()));
        }
    }

    private void aviso(Portal p, String texto) {
        plugin.getServer().broadcast(Component.text("⚠ O ", NamedTextColor.GOLD)
                .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                .append(Component.text(" em " + p.onde() + " " + texto, NamedTextColor.GOLD)));
    }

    private void atualizarPlaca(Portal p, Component texto) {
        Entity e = p.display == null ? null : Bukkit.getEntity(p.display);
        TextDisplay td = e instanceof TextDisplay t && t.isValid() ? t : null;
        if (td == null) {
            Location l = p.centro.clone().add(0, 4.9, 0);
            td = l.getWorld().spawn(l, TextDisplay.class, t -> {
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.setShadowed(true);
                t.setDefaultBackground(false);
                t.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            });
            p.display = td.getUniqueId();
        }
        td.text(texto);
    }

    private void tirarVisual(Portal p) {
        Entity e = p.display == null ? null : Bukkit.getEntity(p.display);
        if (e != null) e.remove();
        p.display = null;
        if (p.barra != null) for (Player j : Bukkit.getOnlinePlayers()) j.hideBossBar(p.barra);
    }

    // =====================================================================
    //  Fechar (venceram a masmorra)
    // =====================================================================

    private void fechado(Portal p, Masmorra m) {
        if (portais.remove(p.id) == null) return;
        tirarVisual(p);
        salvar();
        World w = p.centro.getWorld();
        w.spawnParticle(Particle.END_ROD, p.centro.clone().add(0, 2, 0), 80, 0.6, 1.5, 0.6, 0.1);
        w.playSound(p.centro, Sound.BLOCK_BEACON_DEACTIVATE, 2f, 0.8f);
        List<String> nomes = new ArrayList<>();
        int xp = 60 * (p.d.nivel() + 1);
        for (UUID id : m.participantes()) {
            Player j = Bukkit.getPlayer(id);
            if (j == null) continue;
            nomes.add(j.getName());
            plugin.stats().darXp(j, Skill.COMBATE, xp);
            plugin.titulos().registrar(j, "portais", 1);
            j.sendMessage(Component.text("⛩ O portal lá fora se fechou! O baú do chefe tem tesouro a mais.", COR));
        }
        plugin.getServer().broadcast(Component.text("⛩ O ", COR)
                .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                .append(Component.text(" em " + p.onde() + " foi fechado" + (nomes.isEmpty() ? "" : " por " + String.join(", ", nomes)) + "!", COR)));
        if (p.eco) despertar(m);
    }

    /** Eco do Soberano: o primeiro digno (quem matou o chefe, o dono, depois os outros) vira o Soberano. */
    private void despertar(Masmorra m) {
        if (!soberanoLivre()) return;
        List<UUID> ordem = new ArrayList<>();
        if (m.matadorChefe() != null) ordem.add(m.matadorChefe());
        if (!ordem.contains(m.dono())) ordem.add(m.dono());
        for (UUID id : m.participantes()) if (!ordem.contains(id)) ordem.add(id);
        for (UUID id : ordem) {
            Player j = Bukkit.getPlayer(id);
            if (j == null || !m.participantes().contains(id)) continue;
            if (!plugin.classes().faltando(j, Classe.SOBERANO_DAS_SOMBRAS).isEmpty()) continue;
            if (plugin.classes().reivindicarLendaria(j, Classe.SOBERANO_DAS_SOMBRAS) != null) continue;
            j.getWorld().playSound(j.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1.2f, 0.7f);
            j.getWorld().spawnParticle(Particle.SCULK_SOUL, j.getLocation().add(0, 1, 0), 80, 0.8, 1.2, 0.8, 0.05);
            j.sendMessage(Component.text("☾ Uma voz ecoa: \"Levante-se.\" Os mortos agora te obedecem. (/sombras)", TextColor.color(0x7E57C2)));
            return;
        }
        plugin.getServer().broadcast(Component.text("☾ O Eco do Soberano procurou um herdeiro... e ninguém era digno."
                + " (é preciso uma classe avançada dominada)", TextColor.color(0x7E57C2), TextDecoration.ITALIC));
    }

    // =====================================================================
    //  Transbordo
    // =====================================================================

    private void iniciarTransbordo(Portal p) {
        p.transbordando = true;
        p.onda = 0;
        p.ondas = 2 + p.d.nivel();
        p.segundos = 0;
        p.proximaOnda = 3;
        Masmorra m = masmorras().ativa(p.masmorra);
        if (m != null) masmorras().encerrar(m, "⚠ O portal transbordou! Você foi lançado para fora.");
        p.masmorra = null;
        p.barra = BossBar.bossBar(Component.text("⚠ " + nome(p.d) + " transbordou!", NamedTextColor.RED), 1f,
                BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        World w = p.centro.getWorld();
        w.playSound(p.centro, Sound.EVENT_RAID_HORN, 6f, 0.6f);
        w.strikeLightningEffect(p.centro);
        plugin.getServer().broadcast(Component.text("⚠ O ", NamedTextColor.RED)
                .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                .append(Component.text(" em " + p.onde() + " TRANSBORDOU! Monstros invadem a região!", NamedTextColor.RED, TextDecoration.BOLD)));
        if (p.eco) {
            plugin.getServer().broadcast(Component.text("☾ O Eco do Soberano se perdeu no transbordo...", TextColor.color(0x7E57C2), TextDecoration.ITALIC));
        }
        for (Colonia c : coloniasPerto(p)) {
            plugin.colonias().alterarFelicidade(c, -10);
            Player dono = Bukkit.getPlayer(c.dono());
            if (dono != null) dono.sendMessage(Component.text("⚠ Um portal transbordou perto da sua colônia! Os moradores estão com medo.", NamedTextColor.RED));
        }
        atualizarPlaca(p, Component.text("⚠ TRANSBORDANDO ⚠", NamedTextColor.DARK_RED, TextDecoration.BOLD));
    }

    private List<Colonia> coloniasPerto(Portal p) {
        List<Colonia> l = new ArrayList<>();
        for (Colonia c : plugin.colonias().todas()) {
            Location pref = c.prefeitura();
            if (pref != null && pref.getWorld().equals(p.centro.getWorld()) && pref.distanceSquared(p.centro) < RAIO_COLONIA * RAIO_COLONIA) l.add(c);
        }
        return l;
    }

    private List<Player> perto(Portal p, double raio) {
        List<Player> l = new ArrayList<>();
        for (Player j : p.centro.getWorld().getPlayers()) {
            if (j.getGameMode() == GameMode.SPECTATOR || j.isDead()) continue;
            if (j.getLocation().distanceSquared(p.centro) < raio * raio) l.add(j);
        }
        return l;
    }

    private void transbordar(Portal p) {
        if (!p.centro.isChunkLoaded()) return; // ninguém por perto: o transbordo espera
        p.monstros.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity e) || !e.isValid() || e.isDead());
        List<Player> vendo = perto(p, 80);
        boolean gente = !perto(p, 96).isEmpty();
        if (gente) p.segundos++;
        if (p.segundos > LIMITE_TRANSBORDO_SEG) {
            esgotar(p);
            return;
        }
        LivingEntity chefe = p.chefe == null ? null : Bukkit.getEntity(p.chefe) instanceof LivingEntity le && le.isValid() ? le : null;
        if (p.chefe != null && chefe == null) p.chefe = null; // sumiu sem morrer (ficou sozinho): volta quando alguém chegar
        if (gente && p.chefe == null) {
            boolean ondaLimpa = p.monstros.size() <= Math.max(1, p.ultimaOnda * 3 / 10);
            if (p.onda < p.ondas && (ondaLimpa || p.segundos >= p.proximaOnda)) {
                p.onda++;
                onda(p);
                p.proximaOnda = p.segundos + 45;
            } else if (p.onda >= p.ondas && (p.monstros.isEmpty() || p.segundos >= p.proximaOnda) && !perto(p, 48).isEmpty()) {
                invocarChefe(p);
            }
        }
        if (p.segundos % 3 == 0) guiar(p);
        // Barra
        Component titulo;
        float prog;
        if (chefe != null) {
            AttributeInstance max = chefe.getAttribute(Attribute.MAX_HEALTH);
            titulo = Component.text("☠ O chefe do " + nome(p.d) + " saiu!", NamedTextColor.DARK_RED, TextDecoration.BOLD);
            prog = (float) (chefe.getHealth() / (max == null ? chefe.getHealth() : max.getValue()));
        } else {
            titulo = Component.text("⚠ " + nome(p.d) + " transbordou! Onda " + Math.max(1, p.onda) + "/" + p.ondas + " • "
                    + p.monstros.size() + " inimigos", NamedTextColor.RED);
            prog = Math.max(0, Math.min(1, 1f - (p.onda - 1 + (p.ultimaOnda == 0 ? 0 : 1f - p.monstros.size() / (float) p.ultimaOnda)) / (p.ondas + 1f)));
        }
        p.barra.name(titulo);
        p.barra.progress(Math.max(0, Math.min(1, prog)));
        for (Player j : Bukkit.getOnlinePlayers()) {
            if (vendo.contains(j)) j.showBossBar(p.barra);
            else j.hideBossBar(p.barra);
        }
    }

    private void onda(Portal p) {
        World w = p.centro.getWorld();
        int jogadores = perto(p, 64).size();
        int quantos = Math.min(20, 4 + 2 * p.d.nivel() + 2 * jogadores + p.onda);
        List<EntityType> tipos = p.d.monstros();
        boolean ravager = false;
        int feitos = 0;
        for (int i = 0; i < quantos; i++) {
            EntityType tipo = tipos.get(rnd().nextInt(tipos.size()));
            if (tipo == EntityType.RAVAGER) {
                if (ravager) tipo = EntityType.VINDICATOR;
                ravager = true;
            }
            double a = rnd().nextDouble(Math.PI * 2), r = rnd().nextDouble(2, 6);
            int x = (int) Math.floor(p.centro.getX() + Math.cos(a) * r), z = (int) Math.floor(p.centro.getZ() + Math.sin(a) * r);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Location l = new Location(w, x + 0.5, w.getHighestBlockYAt(x, z) + 1, z + 0.5);
            if (monstro(p, l, tipo) != null) feitos++;
        }
        p.ultimaOnda = Math.max(1, feitos);
        w.playSound(p.centro, Sound.EVENT_RAID_HORN, 4f, 1.2f);
        for (Player j : perto(p, 80)) {
            j.showTitle(Title.title(Component.empty(), Component.text("⚠ Onda " + p.onda + "/" + p.ondas + " sai do portal!", NamedTextColor.RED),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }
    }

    private LivingEntity monstro(Portal p, Location l, EntityType tipo) {
        if (tipo.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(tipo.getEntityClass())) return null;
        @SuppressWarnings("unchecked")
        Class<LivingEntity> classe = (Class<LivingEntity>) tipo.getEntityClass();
        Dificuldade d = p.d;
        LivingEntity e = l.getWorld().spawn(l, classe, CreatureSpawnEvent.SpawnReason.CUSTOM, mob -> {
            multiplicar(mob, Attribute.MAX_HEALTH, d.vida() * 0.8);
            multiplicar(mob, Attribute.ATTACK_DAMAGE, d.dano() * 0.8);
            AttributeInstance alcance = mob.getAttribute(Attribute.FOLLOW_RANGE);
            if (alcance != null) alcance.setBaseValue(Math.max(alcance.getBaseValue(), 40));
            AttributeInstance vida = mob.getAttribute(Attribute.MAX_HEALTH);
            if (vida != null) mob.setHealth(vida.getValue());
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(false);
            mob.getPersistentDataContainer().set(K_MONSTRO, PersistentDataType.STRING, p.id.toString());
            if (mob instanceof Zombie z) {
                z.setShouldBurnInDay(false);
                z.setCanBreakDoors(false);
            }
            if (mob instanceof AbstractSkeleton s) s.setShouldBurnInDay(false);
            if (mob instanceof Raider r) r.setCanJoinRaid(false);
        });
        plugin.perigo().envelhecer(e);
        if (rnd().nextDouble() < 0.06 * (d.nivel() + 1)) plugin.perigo().tornarElite(e, d.nivel() >= 2 ? 2 : 1);
        p.monstros.add(e.getUniqueId());
        l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        return e;
    }

    private static void multiplicar(LivingEntity e, Attribute a, double f) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * f);
    }

    /** Monstros sem alvo: caçam quem estiver perto, senão marcham para a colônia mais próxima. */
    private void guiar(Portal p) {
        List<Colonia> colonias = coloniasPerto(p);
        for (UUID id : p.monstros) {
            if (!(Bukkit.getEntity(id) instanceof Mob m) || (m.getTarget() != null && m.getTarget().isValid())) continue;
            Player alvo = null;
            double d = 40 * 40;
            for (Player j : m.getWorld().getPlayers()) {
                if (j.getGameMode() != GameMode.SURVIVAL && j.getGameMode() != GameMode.ADVENTURE) continue;
                double x = j.getLocation().distanceSquared(m.getLocation());
                if (x < d) { d = x; alvo = j; }
            }
            if (alvo != null) {
                m.setTarget(alvo);
                continue;
            }
            Colonia perto = null;
            double dc = Double.MAX_VALUE;
            for (Colonia c : colonias) {
                double x = c.prefeitura().distanceSquared(m.getLocation());
                if (x < dc) { dc = x; perto = c; }
            }
            if (perto == null) continue;
            Villager v = moradorPerto(perto, m.getLocation());
            if (v != null && v.getLocation().distanceSquared(m.getLocation()) < 24 * 24) m.setTarget(v);
            else m.getPathfinder().moveTo(perto.prefeitura(), 1.1);
        }
    }

    private static Villager moradorPerto(Colonia c, Location l) {
        Villager melhor = null;
        double d = Double.MAX_VALUE;
        for (Colonia.Cidadao ci : c.cidadaos()) {
            if (Bukkit.getEntity(ci.entidade()) instanceof Villager v && v.getWorld().equals(l.getWorld())) {
                double x = v.getLocation().distanceSquared(l);
                if (x < d) { d = x; melhor = v; }
            }
        }
        return melhor;
    }

    private void invocarChefe(Portal p) {
        List<Chefe> opcoes = p.d.chefes();
        Chefe c = opcoes.get(rnd().nextInt(opcoes.size()));
        Location chao = p.centro.clone().subtract(0, 1, 0).getBlock().getLocation();
        LivingEntity boss = plugin.chefes().invocar(c, chao, null, false);
        multiplicar(boss, Attribute.MAX_HEALTH, p.d.vidaChefe());
        AttributeInstance vida = boss.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) boss.setHealth(vida.getValue());
        p.chefe = boss.getUniqueId();
        plugin.getServer().broadcast(Component.text("☠ O chefe do ", NamedTextColor.DARK_RED)
                .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                .append(Component.text(" saiu: " + c.nome() + "! Derrote-o para fechar o portal.", NamedTextColor.DARK_RED)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        for (Portal p : new ArrayList<>(portais.values())) {
            if (id.equals(p.chefe)) {
                vencer(p);
                return;
            }
        }
    }

    /** Derrotaram o chefe do transbordo: o portal se fecha e deixa um baú. */
    private void vencer(Portal p) {
        portais.remove(p.id);
        tirarVisual(p);
        salvar();
        World w = p.centro.getWorld();
        Block b = p.centro.getBlock();
        if (b.isPassable() && !b.isLiquid()) {
            b.setType(Material.CHEST, false);
            if (b.getState() instanceof Chest bau) {
                Tesouro.encher(plugin, bau.getBlockInventory(), p.d, 4 + p.d.nivel() * 2 + perto(p, 64).size(), true, new Random());
            }
        }
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, p.centro.clone().add(0, 1.5, 0), 120, 1, 1.5, 1, 0.4);
        w.playSound(p.centro, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2f, 1f);
        List<String> nomes = new ArrayList<>();
        for (Player j : perto(p, 64)) {
            nomes.add(j.getName());
            plugin.stats().darXp(j, Skill.COMBATE, 80 * (p.d.nivel() + 1));
            plugin.titulos().registrar(j, "portais", 1);
            j.showTitle(Title.title(Component.text("⛩ Portal fechado! ⛩", COR, TextDecoration.BOLD),
                    Component.text("O baú do portal ficou para vocês", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(700))));
        }
        for (UUID id : p.monstros) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        for (Colonia c : coloniasPerto(p)) plugin.colonias().alterarFelicidade(c, 10);
        plugin.getServer().broadcast(Component.text("⛩ O transbordo do ", COR)
                .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                .append(Component.text(" foi contido" + (nomes.isEmpty() ? "" : " por " + String.join(", ", nomes)) + "!", COR)));
    }

    /** Ninguém deu conta em 30 minutos: os monstros se dispersam e o portal some, deixando o medo. */
    private void esgotar(Portal p) {
        portais.remove(p.id);
        tirarVisual(p);
        salvar();
        for (UUID id : p.monstros) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) {
                e.getWorld().spawnParticle(Particle.LARGE_SMOKE, e.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                e.remove();
            }
        }
        Entity chefe = p.chefe == null ? null : Bukkit.getEntity(p.chefe);
        if (chefe != null) chefe.remove();
        for (Colonia c : coloniasPerto(p)) plugin.colonias().alterarFelicidade(c, -10);
        plugin.getServer().broadcast(Component.text("⛩ O " + nome(p.d) + " em " + p.onde()
                + " se esgotou sozinho... e deixou a região em ruínas.", NamedTextColor.GRAY));
    }

    // =====================================================================
    //  Admin e listagem
    // =====================================================================

    private Portal maisProximo(Player j) {
        Portal melhor = null;
        double d = Double.MAX_VALUE;
        for (Portal p : portais.values()) {
            if (!p.centro.getWorld().equals(j.getWorld())) continue;
            double x = p.centro.distanceSquared(j.getLocation());
            if (x < d) { d = x; melhor = p; }
        }
        return melhor;
    }

    /** Admin: faz o portal mais perto transbordar agora. */
    public String transbordarAgora(Player j) {
        Portal p = maisProximo(j);
        if (p == null) return "Nenhum portal neste mundo.";
        if (p.transbordando) return "Esse portal já está transbordando.";
        p.transbordaEm = System.currentTimeMillis();
        salvar();
        return null;
    }

    /** Admin: apaga o portal mais perto (sem prêmio). */
    public String fecharAdmin(Player j) {
        Portal p = maisProximo(j);
        if (p == null) return "Nenhum portal neste mundo.";
        portais.remove(p.id);
        tirarVisual(p);
        Masmorra m = masmorras().ativa(p.masmorra);
        if (m != null) masmorras().encerrar(m, "O portal foi fechado por um administrador.");
        for (UUID id : p.monstros) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        salvar();
        return null;
    }

    public int quantos() {
        return portais.size();
    }

    public void parar() {
        for (Portal p : portais.values()) {
            tirarVisual(p);
            for (UUID id : p.monstros) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            p.monstros.clear();
        }
        salvar();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long agora = System.currentTimeMillis();
        if (portais.isEmpty()) {
            sender.sendMessage(Component.text("⛩ Nenhum portal aberto agora. Eles surgem sozinhos perto de quem está explorando.", COR));
            return true;
        }
        sender.sendMessage(Component.text("⛩ Portais abertos:", COR, TextDecoration.BOLD));
        for (Portal p : portais.values()) {
            String dist = "";
            if (sender instanceof Player j && j.getWorld().equals(p.centro.getWorld())) {
                dist = " (" + (int) j.getLocation().distance(p.centro) + " blocos)";
            }
            Component estado = p.transbordando ? Component.text("TRANSBORDANDO!", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                    : Component.text("transborda em " + tempo(p.transbordaEm - agora), NamedTextColor.GRAY);
            sender.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(nome(p.d), p.d.cor(), TextDecoration.BOLD))
                    .append(Component.text(" — " + p.onde() + dist + " — ", NamedTextColor.WHITE))
                    .append(estado)
                    .append(p.eco ? Component.text(" ☾", TextColor.color(0x7E57C2)) : Component.empty()));
        }
        sender.sendMessage(Component.text("Entre no portal para cair numa masmorra; vencer o chefe o fecha.", NamedTextColor.DARK_GRAY));
        return true;
    }
}
