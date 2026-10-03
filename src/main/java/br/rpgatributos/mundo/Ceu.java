package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O céu e o clima do mundo principal: fases da lua (cheia = monstros mais fortes e lobisomens,
 * nova = magias mais fortes), Lua de Sangue, chuva de meteoros, eclipse, aurora no inverno e
 * eventos de clima (nevasca, neblina, ventania, tempestade elétrica, onda de calor).
 */
public final class Ceu implements Listener {

    public enum Clima {
        NEVASCA("Nevasca", 0xDDEFFF, "Neve grossa: tudo fica lento e frio."),
        NEBLINA("Neblina", 0xCCCCCC, "Não dá para ver longe."),
        VENTANIA("Ventania", 0xA8E8FF, "O vento desvia flechas e projéteis."),
        TEMPESTADE("Tempestade elétrica", 0xFFE14D, "Raios caem perto de quem está ao ar livre com metal na mão!"),
        CALOR("Onda de calor", 0xFF9A3C, "Ao ar livre você cansa (e fica com fome) mais rápido.");

        final String nome;
        final TextColor cor;
        final String descricao;

        Clima(String nome, int cor, String descricao) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.descricao = descricao;
        }

        public String nome() { return nome; }
    }

    private static final String[] FASES = {"Lua Cheia", "Lua Minguante Gibosa", "Quarto Minguante", "Lua Minguante",
            "Lua Nova", "Lua Crescente", "Quarto Crescente", "Lua Crescente Gibosa"};

    private final RPGAtributos plugin;
    private Clima clima;
    private int climaAte;
    private Vector vento = new Vector(1, 0, 0);
    private boolean luaDeSangue, aurora, eclipse;
    private int meteorosAte, eclipseAte;
    private long ultimaHora = -1;
    private int segundos;
    /** Meteoritos caídos (bloco → até quando ficam). */
    private final Map<Location, Long> meteoritos = new HashMap<>();

    public Ceu(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private World mundo() { return Bukkit.getWorlds().getFirst(); }

    public static int fase(World w) {
        return (int) ((w.getFullTime() / 24000) % 8);
    }

    public static String nomeFase(World w) {
        return FASES[fase(w)];
    }

    public static boolean noite(World w) {
        long t = w.getTime();
        return t >= 13000 && t <= 23000;
    }

    public boolean luaCheia() {
        World w = mundo();
        return noite(w) && fase(w) == 0;
    }

    public boolean luaNova() {
        World w = mundo();
        return noite(w) && fase(w) == 4;
    }

    public boolean luaDeSangue() { return luaDeSangue; }
    public boolean eclipse() { return eclipse; }
    public boolean aurora() { return aurora; }
    public Clima clima() { return clima; }

    private static boolean aoArLivre(Player p) {
        return p.getLocation().getBlock().getLightFromSky() >= 14;
    }

    // ---------- bônus para os outros sistemas ----------

    /** Força das magias: lua nova +15%, eclipse +30%. */
    public double bonusMagia(Player p) {
        if (!p.getWorld().equals(mundo())) return 1;
        return (luaNova() ? 1.15 : 1) * (eclipse ? 1.3 : 1);
    }

    /** Mana a mais por segundo na aurora (ao ar livre). */
    public double bonusRegen(Player p) {
        return aurora && p.getWorld().equals(mundo()) && aoArLivre(p) ? 2 : 0;
    }

    /** Elites mais comuns na Lua de Sangue e no eclipse. */
    public double multiplicadorElite(World w) {
        if (!w.equals(mundo())) return 1;
        return (luaDeSangue ? 3 : 1) * (eclipse ? 2 : 1);
    }

    // =====================================================================
    //  A cada segundo
    // =====================================================================

    public void tick() {
        segundos++;
        World w = mundo();
        long hora = w.getTime();
        if (ultimaHora >= 0) {
            if (ultimaHora < 13000 && hora >= 13000) comecoDaNoite(w);
            if (ultimaHora < 23000 && hora >= 23000 || hora < ultimaHora && hora < 13000) fimDaNoite();
            if (ultimaHora < 6000 && hora >= 6000 && !eclipse && rnd().nextDouble() < plugin.settings().ceuChanceEclipse) comecarEclipse(w);
        }
        ultimaHora = hora;

        if (eclipse && segundos >= eclipseAte) terminarEclipse(w);
        if (meteorosAte > segundos && segundos % 4 == 0) meteoro(w);
        if (clima != null && segundos >= climaAte) terminarClima(w);
        if (clima == null && segundos % 150 == 0 && rnd().nextDouble() < plugin.settings().cliChanceEvento) comecarClima(w, null);
        for (Player p : w.getPlayers()) efeitos(p);
        if (segundos % 60 == 0) {
            long agora = System.currentTimeMillis();
            for (Iterator<Map.Entry<Location, Long>> it = meteoritos.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Location, Long> en = it.next();
                if (agora < en.getValue()) continue;
                if (en.getKey().isChunkLoaded() && en.getKey().getBlock().getType() == Material.MAGMA_BLOCK) en.getKey().getBlock().setType(Material.AIR);
                it.remove();
            }
        }
    }

    private void comecoDaNoite(World w) {
        if (fase(w) == 0) {
            if (rnd().nextDouble() < plugin.settings().ceuChanceSangue) {
                luaDeSangue = true;
                anunciar("☾ LUA DE SANGUE ☾", "Monstros mais fortes, mais Elites e o dobro de XP esta noite", NamedTextColor.DARK_RED,
                        Sound.ENTITY_WITHER_SPAWN);
            } else {
                anunciar("☾ Lua Cheia", "Os monstros estão mais fortes... e há lobisomens por aí", NamedTextColor.GOLD, Sound.ENTITY_WOLF_GROWL);
            }
        } else if (fase(w) == 4) {
            Bukkit.broadcast(Component.text("☾ Lua Nova: as magias estão mais fortes esta noite (e os vampiros saem).", NamedTextColor.LIGHT_PURPLE));
        }
        if (rnd().nextDouble() < plugin.settings().ceuChanceMeteoros) comecarMeteoros();
        if (plugin.estacoes().atual() == Estacoes.Estacao.INVERNO && rnd().nextDouble() < plugin.settings().ceuChanceAurora) {
            aurora = true;
            Bukkit.broadcast(Component.text("✧ Uma aurora ilumina o céu: mana extra para quem estiver ao ar livre.", TextColor.color(0x7FFFD4)));
        }
    }

    private void fimDaNoite() {
        if (luaDeSangue) Bukkit.broadcast(Component.text("☀ A Lua de Sangue passou.", NamedTextColor.GRAY));
        luaDeSangue = false;
        aurora = false;
    }

    private void anunciar(String titulo, String sub, TextColor cor, Sound som) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(Component.text(titulo, cor, TextDecoration.BOLD), Component.text(sub, NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), som, 0.8f, 0.8f);
        }
    }

    // ---------- Lua de Sangue e lua cheia ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(e.getEntity() instanceof Monster m)) return;
        if (!m.getWorld().equals(mundo())) return;
        double bonus = (luaCheia() ? 0.2 : 0) + (luaDeSangue ? 0.3 : 0) + (eclipse ? 0.2 : 0);
        if (bonus <= 0) return;
        AttributeInstance dano = m.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dano != null) dano.setBaseValue(dano.getBaseValue() * (1 + bonus));
        if (luaDeSangue) m.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(EntityDeathEvent e) {
        if ((luaDeSangue || eclipse) && e.getEntity() instanceof Monster && e.getEntity().getWorld().equals(mundo())) {
            e.setDroppedExp(e.getDroppedExp() * 2);
        }
    }

    // ---------- meteoros ----------

    public void comecarMeteoros() {
        meteorosAte = segundos + 120;
        anunciar("☄ Chuva de Meteoros", "Meteoritos caem pelo mundo: procure as pedras brilhantes!", TextColor.color(0xFF9A3C),
                Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR);
    }

    private void meteoro(World w) {
        Player p = null;
        for (Player x : w.getPlayers()) if (x.getGameMode() == GameMode.SURVIVAL && aoArLivre(x) && rnd().nextBoolean()) { p = x; break; }
        if (p == null) return;
        double a = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(30, 80);
        int x = (int) (p.getLocation().getX() + Math.cos(a) * d), z = (int) (p.getLocation().getZ() + Math.sin(a) * d);
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return;
        Block chao = w.getHighestBlockAt(x, z);
        Location alvo = chao.getLocation().add(0.5, 1, 0.5);
        Location ceu = alvo.clone().add(rnd().nextDouble(-20, 20), 70, rnd().nextDouble(-20, 20));
        Vector passo = alvo.toVector().subtract(ceu.toVector()).multiply(1 / 30.0);
        Location pos = ceu.clone();
        plugin.getServer().getScheduler().runTaskTimer(plugin, tarefa -> {
            pos.add(passo);
            w.spawnParticle(Particle.FLAME, pos, 10, 0.3, 0.3, 0.3, 0.02);
            w.spawnParticle(Particle.LARGE_SMOKE, pos, 4, 0.2, 0.2, 0.2, 0.01);
            if (pos.distanceSquared(alvo) < 4) {
                tarefa.cancel();
                w.spawnParticle(Particle.EXPLOSION_EMITTER, alvo, 1);
                w.playSound(alvo, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.6f);
                Block b = alvo.getBlock();
                if (b.isEmpty() && !chao.isLiquid() && plugin.territorios().em(b.getLocation()) == null) {
                    b.setType(Material.MAGMA_BLOCK);
                    meteoritos.put(b.getLocation(), System.currentTimeMillis() + 20 * 60_000L);
                }
            }
        }, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrarMeteorito(BlockBreakEvent e) {
        if (meteoritos.remove(e.getBlock().getLocation()) == null) return;
        e.setDropItems(false);
        Location c = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        c.getWorld().dropItemNaturally(c, new ItemStack(Material.RAW_IRON, 2 + rnd().nextInt(3)));
        c.getWorld().dropItemNaturally(c, new ItemStack(Material.RAW_GOLD, 1 + rnd().nextInt(3)));
        if (rnd().nextDouble() < 0.35) c.getWorld().dropItemNaturally(c, new ItemStack(Material.DIAMOND));
        if (rnd().nextDouble() < 0.25) c.getWorld().dropItemNaturally(c, Raro.FRAGMENTO_DE_FORJA.criar(1));
        if (rnd().nextDouble() < 0.04) c.getWorld().dropItemNaturally(c, Raro.ESSENCIA_PRIMORDIAL.criar(1));
        c.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, c, 30, 0.4, 0.4, 0.4, 0.2);
        e.getPlayer().sendMessage(Component.text("☄ Você recolheu um meteorito!", TextColor.color(0xFF9A3C)));
        plugin.titulos().registrar(e.getPlayer(), "meteoritos", 1);
    }

    // ---------- eclipse ----------

    public void comecarEclipse(World w) {
        eclipse = true;
        eclipseAte = segundos + 150;
        w.setTime(18000);
        ultimaHora = 18000;
        anunciar("◐ ECLIPSE ◐", "O sol sumiu: monstros e magias ficam mais fortes", NamedTextColor.DARK_PURPLE, Sound.AMBIENT_CAVE);
    }

    private void terminarEclipse(World w) {
        eclipse = false;
        w.setTime(6500);
        ultimaHora = 6500;
        Bukkit.broadcast(Component.text("☀ O eclipse terminou.", NamedTextColor.GRAY));
    }

    // ---------- clima ----------

    /** Começa um evento de clima (null = sorteado pela estação). */
    public void comecarClima(World w, Clima c) {
        if (c == null) {
            Clima[] opcoes = switch (plugin.estacoes().atual()) {
                case INVERNO -> new Clima[]{Clima.NEVASCA, Clima.NEVASCA, Clima.NEBLINA};
                case PRIMAVERA -> new Clima[]{Clima.TEMPESTADE, Clima.NEBLINA};
                case VERAO -> new Clima[]{Clima.CALOR, Clima.CALOR, Clima.TEMPESTADE};
                case OUTONO -> new Clima[]{Clima.VENTANIA, Clima.NEBLINA, Clima.VENTANIA};
            };
            c = opcoes[rnd().nextInt(opcoes.length)];
        }
        clima = c;
        climaAte = segundos + 240;
        vento = new Vector(rnd().nextDouble(-1, 1), 0, rnd().nextDouble(-1, 1)).normalize();
        if (c == Clima.NEVASCA || c == Clima.TEMPESTADE) {
            w.setStorm(true);
            w.setWeatherDuration(240 * 20);
            if (c == Clima.TEMPESTADE) {
                w.setThundering(true);
                w.setThunderDuration(240 * 20);
            }
        }
        Bukkit.broadcast(Component.text("☁ " + c.nome + ": " + c.descricao, c.cor));
    }

    private void terminarClima(World w) {
        if (clima == Clima.NEVASCA || clima == Clima.TEMPESTADE) {
            w.setStorm(false);
            w.setThundering(false);
        }
        clima = null;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoLancar(ProjectileLaunchEvent e) {
        if (clima != Clima.VENTANIA || !e.getEntity().getWorld().equals(mundo())) return;
        Projectile pr = e.getEntity();
        plugin.getServer().getScheduler().runTask(plugin, () -> pr.setVelocity(pr.getVelocity().add(vento.clone().multiply(0.35))));
    }

    private void efeitos(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        boolean fora = aoArLivre(p);
        Location l = p.getLocation();
        if (luaDeSangue && segundos % 2 == 0) {
            p.spawnParticle(Particle.DUST, l.clone().add(0, 8, 0), 10, 10, 2, 10, 0, new Particle.DustOptions(Color.fromRGB(0x8B0000), 2f));
        }
        if (aurora && fora && segundos % 2 == 0) {
            for (int i = 0; i < 12; i++) {
                double a = i / 12.0 * Math.PI * 2 + segundos * 0.05;
                p.spawnParticle(Particle.DUST, l.clone().add(Math.cos(a) * 18, 22 + Math.sin(a * 2) * 2, Math.sin(a) * 18), 2, 1, 0.5, 1, 0,
                        new Particle.DustOptions(Color.fromRGB(i % 2 == 0 ? 0x7FFFD4 : 0xB66BFF), 3f));
            }
        }
        if (clima == null) return;
        switch (clima) {
            case NEVASCA -> {
                if (!fora) return;
                p.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 2, 0), 80, 4, 2, 4, 0.05);
                if (!p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, false));
            }
            case NEBLINA -> {
                for (int i = 0; i < 25; i++) {
                    double a = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(4, 12);
                    p.spawnParticle(Particle.DUST, l.clone().add(Math.cos(a) * d, rnd().nextDouble(0, 3), Math.sin(a) * d), 2, 0.5, 0.5, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(0xE6E6E6), 3.5f));
                }
            }
            case VENTANIA -> {
                if (fora && segundos % 2 == 0) p.spawnParticle(Particle.CLOUD, l.clone().add(vento.clone().multiply(-6)).add(0, 1.5, 0), 6, 1, 1, 1, 0.3);
            }
            case TEMPESTADE -> {
                if (!fora || segundos % 5 != 0) return;
                Material mao = p.getInventory().getItemInMainHand().getType();
                boolean metal = mao.name().startsWith("IRON_") || mao.name().startsWith("GOLDEN_") || mao.name().startsWith("NETHERITE_")
                        || mao.name().startsWith("COPPER_") || mao == Material.TRIDENT || mao == Material.MACE;
                if (metal && rnd().nextDouble() < 0.12) {
                    p.getWorld().strikeLightningEffect(l);
                    p.damage(5);
                    p.sendActionBar(Component.text("⚡ Um raio atraído pelo metal na sua mão!", NamedTextColor.YELLOW));
                } else if (rnd().nextDouble() < 0.2) {
                    p.getWorld().strikeLightningEffect(l.clone().add(rnd().nextDouble(-12, 12), 0, rnd().nextDouble(-12, 12)));
                }
            }
            case CALOR -> {
                if (!fora || p.getWorld().getTime() > 12000 || p.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;
                p.setExhaustion(p.getExhaustion() + 0.25f);
                if (segundos % 3 == 0) p.spawnParticle(Particle.WHITE_ASH, l.clone().add(0, 1, 0), 15, 2, 1, 2, 0);
            }
        }
    }

    // ---------- para o /calendario ----------

    public String resumo() {
        World w = mundo();
        StringBuilder sb = new StringBuilder(nomeFase(w));
        if (luaDeSangue) sb.append(" · Lua de Sangue");
        if (eclipse) sb.append(" · Eclipse");
        if (aurora) sb.append(" · Aurora");
        if (meteorosAte > segundos) sb.append(" · Chuva de meteoros");
        if (clima != null) sb.append(" · ").append(clima.nome);
        return sb.toString();
    }

    public void forcarLuaDeSangue() {
        luaDeSangue = true;
        anunciar("☾ LUA DE SANGUE ☾", "Monstros mais fortes, mais Elites e o dobro de XP esta noite", NamedTextColor.DARK_RED, Sound.ENTITY_WITHER_SPAWN);
    }

    public void forcarAurora() {
        aurora = true;
    }

    public void parar() {
        World w = mundo();
        if (eclipse) w.setTime(6500);
        for (Location l : meteoritos.keySet()) if (l.isChunkLoaded() && l.getBlock().getType() == Material.MAGMA_BLOCK) l.getBlock().setType(Material.AIR);
        meteoritos.clear();
    }
}
