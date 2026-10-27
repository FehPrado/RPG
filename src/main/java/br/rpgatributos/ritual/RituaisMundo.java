package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Eventos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.territorio.Territorio;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 2.87: o que o Altar da Convergência faz no mundo: chuva, céu limpo, dia, noite, a Lua Azul,
 * as auras do território, a Bênção da Aliança e o despertar do próprio altar.
 */
public final class RituaisMundo implements Listener {

    public static final TextColor AZUL = TextColor.color(0x5C8DFF);
    private static final long DURACAO_AURA_MS = 2 * 60 * 60 * 1000L;
    private static final Raro[] SAQUE_LUA = {Raro.AMBAR, Raro.JADE, Raro.PEROLA, Raro.CRISTAL_DE_GELO, Raro.PEDRA_SANGUE,
            Raro.FRAGMENTO_DE_FORJA, Raro.PEDRA_DE_PROTECAO};

    public enum Aura {
        CRESCIMENTO("Aura do Crescimento"), PAZ("Aura da Paz"), VIGOR("Aura do Vigor");

        public final String nome;

        Aura(String nome) { this.nome = nome; }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    /** Dono do território → aura → até quando vale. */
    private final Map<UUID, Map<Aura, Long>> auras = new HashMap<>();
    /** Mundos com Lua Azul. */
    private final Set<UUID> luaAzul = new HashSet<>();
    private final BossBar barraLua = BossBar.bossBar(Component.text("☾ Lua Azul", AZUL, TextDecoration.BOLD), 1f,
            BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    private final Set<UUID> veemLua = new HashSet<>();

    public RituaisMundo(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "auras.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        long agora = System.currentTimeMillis();
        for (String dono : y.getKeys(false)) {
            var sec = y.getConfigurationSection(dono);
            if (sec == null) continue;
            for (String a : sec.getKeys(false)) {
                try {
                    long ate = sec.getLong(a);
                    if (ate > agora) auras.computeIfAbsent(UUID.fromString(dono), k -> new EnumMap<>(Aura.class)).put(Aura.valueOf(a), ate);
                } catch (IllegalArgumentException ignorado) { }
            }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Map<Aura, Long>> en : auras.entrySet()) {
            for (Map.Entry<Aura, Long> a : en.getValue().entrySet()) y.set(en.getKey() + "." + a.getKey().name(), a.getValue());
        }
        try { y.save(arquivo); } catch (IOException e) { plugin.getLogger().warning("Não deu para salvar auras.yml: " + e.getMessage()); }
    }

    // =====================================================================
    //  Os efeitos (usados pelas receitas)
    // =====================================================================

    private static void avisar(Location altar, Component texto) {
        for (Player p : altar.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(altar) < 96 * 96) p.sendMessage(texto);
        }
    }

    static ReceitaRitual.Efeito chuva() {
        return (plugin, altar, quem, grupo) -> {
            World w = altar.getWorld();
            w.setStorm(true);
            w.setThundering(true);
            w.setWeatherDuration(12000);
            w.setThunderDuration(12000);
            avisar(altar, Component.text("☂ O céu se fecha: tempestade por 10 minutos.", AZUL));
        };
    }

    static ReceitaRitual.Efeito ceuLimpo() {
        return (plugin, altar, quem, grupo) -> {
            World w = altar.getWorld();
            w.setStorm(false);
            w.setThundering(false);
            w.setClearWeatherDuration(24000);
            avisar(altar, Component.text("☀ As nuvens se abrem: céu limpo por 20 minutos.", NamedTextColor.YELLOW));
        };
    }

    static ReceitaRitual.Efeito hora(long tempo, String frase) {
        return (plugin, altar, quem, grupo) -> {
            World w = altar.getWorld();
            long dia = w.getFullTime() - w.getTime();
            w.setFullTime(dia + (tempo > w.getTime() ? 0 : 24000) + tempo);
            avisar(altar, Component.text("✦ " + frase + " pelo Altar da Convergência.", AltarPedestais.COR));
        };
    }

    static ReceitaRitual.Efeito luaAzul() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                plugin.rituaisMundo().comecarLuaAzul(altar.getWorld());
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                if (!Eventos.noite(altar.getWorld())) return "A Lua Azul só pode ser chamada à noite.";
                if (plugin.rituaisMundo().luaAzul.contains(altar.getWorld().getUID())) return "Já é Lua Azul!";
                return null;
            }
        };
    }

    static ReceitaRitual.Efeito aura(Aura a) {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                Territorio t = plugin.territorios().em(altar);
                if (t == null) return;
                plugin.rituaisMundo().auras.computeIfAbsent(t.dono(), k -> new EnumMap<>(Aura.class))
                        .put(a, System.currentTimeMillis() + DURACAO_AURA_MS);
                plugin.rituaisMundo().salvar();
                avisar(altar, Component.text("✦ " + a.nome + " cobre o território de " + t.nomeDono() + " por 2 horas.", AltarPedestais.COR));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                Territorio t = plugin.territorios().em(altar);
                if (t == null) return "As auras precisam de um altar dentro de um território.";
                if (!plugin.territorios().podeConstruir(quem, altar)) return "Só quem mora no território pode encantá-lo.";
                return null;
            }
        };
    }

    static ReceitaRitual.Efeito bencao() {
        return (plugin, altar, quem, grupo) -> {
            int duracao = 30 * 60 * 20;
            for (Player p : altar.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(altar) > 12 * 12) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duracao, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duracao, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duracao, 0));
                p.sendMessage(Component.text("✦ Bênção da Aliança: Força, Resistência e Regeneração por 30 minutos.", NamedTextColor.GOLD));
            }
        };
    }

    static ReceitaRitual.Efeito despertarAltar() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                plugin.altarConvergencia().definirNivel(altar, 2);
                avisar(altar, Component.text("✦ O Altar da Convergência despertou: nível II. Uma segunda volta de pedestais (a 5 blocos) se abre.",
                        AltarPedestais.COR));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                return plugin.altarConvergencia().nivel(altar) >= 2 ? "Esse altar já está no nível II." : null;
            }
        };
    }

    // =====================================================================
    //  2.89: altar nível III, meteoros, portal e mercador
    // =====================================================================

    static ReceitaRitual.Efeito ascenderAltar() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                plugin.altarConvergencia().definirNivel(altar, 3);
                altar.getWorld().strikeLightningEffect(altar.clone().add(0.5, 1, 0.5));
                avisar(altar, Component.text("✦ O Altar da Convergência ascendeu: nível III. Uma terceira volta de pedestais (a 7 blocos) "
                        + "e as receitas lendárias se abrem.", AltarPedestais.COR, TextDecoration.BOLD));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                return plugin.altarConvergencia().nivel(altar) >= 3 ? "Esse altar já está no nível III." : null;
            }
        };
    }

    private static boolean superficie(World w) {
        return w.getEnvironment() == World.Environment.NORMAL && !w.getName().equals(br.rpgatributos.masmorra.Masmorras.MUNDO);
    }

    static ReceitaRitual.Efeito meteoros() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                plugin.ceu().meteorosEm(altar.clone().add(0.5, 0, 0.5), quem == null ? null : quem.getUniqueId());
                avisar(altar, Component.text("☄ O céu se abre em fogo: meteoritos vão cair em volta do altar por 1 minuto!", TextColor.color(0xFF9A3C)));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                return superficie(altar.getWorld()) ? null : "Os meteoros só caem no mundo normal, a céu aberto.";
            }
        };
    }

    private static final org.bukkit.NamespacedKey K_PORTAL = new org.bukkit.NamespacedKey("rpgatributos", "portal_altar_ate");
    private static final long ESPERA_PORTAL_MS = 6 * 60 * 60 * 1000L;

    static ReceitaRitual.Efeito portal() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) {
                if (quem == null) return;
                var rank = br.rpgatributos.masmorra.Cacador.rank(quem);
                boolean vermelho = ThreadLocalRandom.current().nextDouble() < 0.05;
                plugin.portais().abrirPorMagia(lugarDoPortal(altar), rank, vermelho, quem.getUniqueId());
                quem.getPersistentDataContainer().set(K_PORTAL, org.bukkit.persistence.PersistentDataType.LONG,
                        System.currentTimeMillis() + ESPERA_PORTAL_MS);
                if (vermelho) quem.sendMessage(Component.text("⚠ O ritual saiu do controle: o portal é Vermelho!", NamedTextColor.RED, TextDecoration.BOLD));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                if (!superficie(altar.getWorld())) return "Os portais só se abrem no mundo normal.";
                long ate = quem.getPersistentDataContainer().getOrDefault(K_PORTAL, org.bukkit.persistence.PersistentDataType.LONG, 0L);
                long falta = ate - System.currentTimeMillis();
                if (falta > 0) return "Você abriu um portal há pouco: espere mais " + (falta / 60_000 + 1) + " minutos.";
                return null;
            }
        };
    }

    /** Um chão livre a 6–9 blocos do altar (fora dos pedestais); sem lugar, em cima do altar. */
    static Location lugarDoPortal(Location altar) {
        org.bukkit.block.Block c = altar.getBlock();
        int[][] dirs = {{0, -8}, {8, 0}, {0, 8}, {-8, 0}, {6, -6}, {6, 6}, {-6, 6}, {-6, -6}, {0, -9}, {9, 0}, {0, 9}, {-9, 0}};
        for (int[] d : dirs) {
            for (int dy = 3; dy >= -3; dy--) {
                org.bukkit.block.Block chao = c.getRelative(d[0], dy, d[1]);
                if (!chao.getType().isSolid() || !chao.getRelative(0, 1, 0).isPassable() || !chao.getRelative(0, 2, 0).isPassable()
                        || !chao.getRelative(0, 3, 0).isPassable()) continue;
                Location l = chao.getLocation().add(0.5, 1, 0.5);
                l.setDirection(altar.toVector().subtract(l.toVector()));
                return l;
            }
        }
        return altar.clone().add(0.5, 1, 0.5);
    }

    static ReceitaRitual.Efeito mercador() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) { }

            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo, ItemStack catalisador) {
                if (quem != null && plugin.mercador().aparecer(quem)) return;
                // Sem lugar para a tenda: as esmeraldas voltam.
                altar.getWorld().dropItemNaturally(altar.clone().add(0.5, 1.2, 0.5), catalisador);
                avisar(altar, Component.text("✦ O Mercador não achou lugar para a tenda por aqui (precisa de chão firme a céu aberto).", AltarPedestais.COR));
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem) {
                if (!superficie(altar.getWorld())) return "O Mercador só anda pelo mundo normal.";
                return plugin.mercador().presente() ? "O Mercador Itinerante já está no mundo: espere ele seguir viagem." : null;
            }
        };
    }

    // =====================================================================
    //  Lua Azul
    // =====================================================================

    void comecarLuaAzul(World w) {
        luaAzul.add(w.getUID());
        plugin.circulosMagicos().ceuParaTodos(w, org.bukkit.Color.fromRGB(0x5C8DFF), br.rpgatributos.visual.CirculosMagicos.BRANCO, 7, 3);
        for (Player p : w.getPlayers()) {
            p.sendMessage(Component.text("☾ A Lua Azul nasceu. Até o amanhecer, os monstros soltam materiais raros.", AZUL, TextDecoration.BOLD));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.6f);
        }
    }

    public boolean luaAzul(World w) {
        return luaAzul.contains(w.getUID());
    }

    @EventHandler
    public void aoMorrerNaLua(EntityDeathEvent e) {
        if (luaAzul.isEmpty() || !(e.getEntity() instanceof Enemy) || e.getEntity().getKiller() == null) return;
        if (!luaAzul.contains(e.getEntity().getWorld().getUID())) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextDouble() < 0.06) e.getDrops().add(SAQUE_LUA[r.nextInt(SAQUE_LUA.length)].criar());
        if (r.nextDouble() < 0.005) e.getDrops().add(Raro.ALMA_DESPERTA.criar());
        e.getEntity().getWorld().spawnParticle(Particle.DUST, e.getEntity().getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(0x5C8DFF), 1.2f));
    }

    // =====================================================================
    //  Auras do território
    // =====================================================================

    public boolean temAura(Territorio t, Aura a) {
        if (t == null) return false;
        Map<Aura, Long> m = auras.get(t.dono());
        Long ate = m == null ? null : m.get(a);
        return ate != null && ate > System.currentTimeMillis();
    }

    @EventHandler(ignoreCancelled = true)
    public void aoCrescer(BlockGrowEvent e) {
        if (auras.isEmpty() || !(e.getNewState().getBlockData() instanceof Ageable a)) return;
        if (!temAura(plugin.territorios().em(e.getBlock()), Aura.CRESCIMENTO)) return;
        a.setAge(Math.min(a.getMaximumAge(), a.getAge() + 1));
        e.getNewState().setBlockData(a);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (auras.isEmpty() || !(e.getEntity() instanceof Enemy)) return;
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) return;
        if (temAura(plugin.territorios().em(e.getLocation()), Aura.PAZ)) e.setCancelled(true);
    }

    /** A cada 5 segundos: Vigor, fim das auras e da Lua Azul, barra da Lua. */
    public void tick() {
        long agora = System.currentTimeMillis();
        boolean mudou = false;
        for (Map<Aura, Long> m : auras.values()) mudou |= m.values().removeIf(ate -> ate <= agora);
        mudou |= auras.values().removeIf(Map::isEmpty);
        if (mudou) salvar();
        if (!auras.isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!temAura(plugin.territorios().em(p.getLocation()), Aura.VIGOR)) continue;
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 140, 0, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 0, true, false));
            }
        }
        for (UUID id : Set.copyOf(luaAzul)) {
            World w = Bukkit.getWorld(id);
            if (w == null || !Eventos.noite(w)) {
                luaAzul.remove(id);
                if (w != null) for (Player p : w.getPlayers()) p.sendMessage(Component.text("☾ A Lua Azul se pôs.", AZUL));
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean deve = luaAzul.contains(p.getWorld().getUID());
            if (deve && veemLua.add(p.getUniqueId())) p.showBossBar(barraLua);
            else if (!deve && veemLua.remove(p.getUniqueId())) p.hideBossBar(barraLua);
        }
    }
}
