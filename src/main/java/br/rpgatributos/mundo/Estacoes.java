package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.perigo.Afixo;
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
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As estações do ano: cada uma dura uma semana real. Mudam o crescimento das plantações,
 * os peixes raros, os modificadores dos Elites e, no inverno, cai neve e a água congela
 * perto dos jogadores (tudo some sozinho na primavera). No 4º dia de cada estação há festival.
 */
public final class Estacoes implements Listener {

    public enum Estacao {
        PRIMAVERA("Primavera", 0x7BC043, "✿", "As plantações crescem mais rápido."),
        VERAO("Verão", 0xFFC93C, "☀", "Dias de calor e tempestades; peixes de água quente."),
        OUTONO("Outono", 0xD35400, "☘", "Boa colheita, ventanias e neblina."),
        INVERNO("Inverno", 0x9FE7FF, "❄", "Neve, água congelada e plantações lentas ao ar livre (estufas funcionam!).");

        final String nome;
        final TextColor cor;
        final String icone;
        final String descricao;

        Estacao(String nome, int cor, String icone, String descricao) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.icone = icone;
            this.descricao = descricao;
        }

        public String nome() { return nome; }
        public TextColor cor() { return cor; }
        public String icone() { return icone; }
        public String descricao() { return descricao; }
    }

    private static final Set<Material> PLANTAS = Set.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.MELON_STEM, Material.PUMPKIN_STEM, Material.SWEET_BERRY_BUSH, Material.COCOA, Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP);

    private final RPGAtributos plugin;
    private final File arquivo;
    /** Blocos que o inverno mudou (para desfazer na primavera): local → o que era antes. */
    private final Map<Location, Material> inverno = new LinkedHashMap<>();
    private long desvio;
    private Estacao anunciada;
    private boolean festivalAnunciado;
    private int segundos;

    public Estacoes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "estacoes.yml");
        carregar();
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** Duração de uma estação (padrão: uma semana real). */
    private long semana() {
        return plugin.settings().horasPorEstacao * 3_600_000L;
    }

    /** Um "dia" da estação: 1/7 dela. */
    private long dia7() {
        return semana() / 7;
    }

    /** Quanto falta para a próxima estação (ms). */
    public long restante() {
        return semana() - tempo() % semana();
    }

    /** "5d 3h", "4h 20min" ou "12 min". */
    public static String duracao(long ms) {
        long min = Math.max(1, ms / 60_000), h = min / 60, d = h / 24;
        if (d > 0) return d + "d " + h % 24 + "h";
        if (h > 0) return h + "h " + min % 60 + "min";
        return min + " min";
    }

    private long tempo() {
        return System.currentTimeMillis() - plugin.perigo().inicio() + desvio;
    }

    public Estacao atual() {
        return Estacao.values()[(int) ((tempo() / semana()) % 4)];
    }

    /** Dia da estação (1 a 7). */
    public int dia() {
        return (int) ((tempo() % semana()) / dia7()) + 1;
    }

    public boolean festival() {
        return dia() == 4;
    }

    /** Para o /rpgadmin: pula para o começo da estação. */
    public void definir(Estacao e) {
        long t = tempo();
        long ano = t / (semana() * 4);
        long alvo = ano * semana() * 4 + e.ordinal() * semana();
        desvio += alvo - t;
        salvar();
    }

    public void definirFestival() {
        long t = tempo();
        desvio += (t / semana()) * semana() + 3 * dia7() - t;
        salvar();
    }

    // =====================================================================
    //  Efeitos
    // =====================================================================

    /** Peixes raros que aparecem mais em cada estação. */
    public double pesoPeixe(PeixeRaro p) {
        return switch (atual()) {
            case PRIMAVERA -> p == PeixeRaro.KOI_CELESTE || p == PeixeRaro.TRUTA_ARCO_IRIS ? 3 : 1;
            case VERAO -> p == PeixeRaro.BAIACU_REI || p == PeixeRaro.CARPA_DOURADA ? 3 : 1;
            case OUTONO -> p == PeixeRaro.SALMAO_REI || p == PeixeRaro.PEIXE_FANTASMA ? 3 : 1;
            case INVERNO -> p == PeixeRaro.PEIXE_GELO ? 4 : p == PeixeRaro.PEIXE_LUA ? 2 : 1;
        };
    }

    /** Modificador que os Elites tendem a ter na estação (ou null). */
    public Afixo afixoDaEstacao() {
        return switch (atual()) {
            case INVERNO -> Afixo.GELIDO;
            case VERAO -> Afixo.INCENDIARIO;
            case OUTONO -> Afixo.FANTASMA;
            case PRIMAVERA -> Afixo.REGENERANTE;
        };
    }

    /** XP extra no festival (atributos do tema da estação). */
    public double multiplicadorXp(Skill s) {
        if (!festival()) return 1;
        boolean tema = switch (atual()) {
            case PRIMAVERA -> s == Skill.AGRICULTURA || s == Skill.DOMA;
            case VERAO -> s == Skill.PESCA || s == Skill.NATACAO;
            case OUTONO -> s == Skill.CULINARIA || s == Skill.AGRICULTURA;
            case INVERNO -> s == Skill.FERRARIA || s == Skill.ALQUIMIA;
        };
        return tema ? 1.25 : 1;
    }

    private static boolean aoArLivre(Block b) {
        return b.getLightFromSky() >= 14;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCrescer(BlockGrowEvent e) {
        Block b = e.getBlock();
        if (!PLANTAS.contains(b.getType()) || b.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        switch (atual()) {
            case INVERNO -> {
                if (aoArLivre(b) && rnd().nextDouble() < 0.75) e.setCancelled(true);
            }
            case PRIMAVERA, OUTONO -> {
                double extra = atual() == Estacao.PRIMAVERA ? 0.4 : 0.15;
                if (rnd().nextDouble() < extra) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (b.getBlockData() instanceof Ageable a && a.getAge() < a.getMaximumAge()) {
                            a.setAge(a.getAge() + 1);
                            b.setBlockData(a);
                        }
                    });
                }
            }
            default -> { }
        }
    }

    // =====================================================================
    //  A cada segundo: troca de estação, festival, neve
    // =====================================================================

    public void tick() {
        segundos++;
        Estacao e = atual();
        if (anunciada != e) {
            boolean primeira = anunciada == null;
            anunciada = e;
            festivalAnunciado = false;
            if (!primeira) anunciarEstacao(e);
            salvar();
        }
        if (festival() && !festivalAnunciado) {
            festivalAnunciado = true;
            anunciarFestival(e);
        }
        if (festival() && segundos % 30 == 0) for (Player p : Bukkit.getOnlinePlayers()) lembranca(p, e);
        if (e != Estacao.INVERNO && !inverno.isEmpty()) derreter(200);
        if (segundos % 2 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) efeitosVisuais(p, e);
        }
    }

    private void anunciarEstacao(Estacao e) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(Component.text(e.icone + " " + e.nome + " " + e.icone, e.cor, TextDecoration.BOLD),
                    Component.text(e.descricao, NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
        }
        Bukkit.broadcast(Component.text(e.icone + " Começou o " + e.nome.toLowerCase(java.util.Locale.ROOT) + ": " + e.descricao, e.cor));
    }

    private static String nomeFestival(Estacao e) {
        return switch (e) {
            case PRIMAVERA -> "Festa das Flores";
            case VERAO -> "Festival do Sol";
            case OUTONO -> "Festa da Colheita";
            case INVERNO -> "Festival do Gelo";
        };
    }

    private void anunciarFestival(Estacao e) {
        String temas = switch (e) {
            case PRIMAVERA -> "Agricultura e Doma";
            case VERAO -> "Pesca e Natação";
            case OUTONO -> "Culinária e Agricultura";
            case INVERNO -> "Ferraria e Alquimia";
        };
        Bukkit.broadcast(Component.text("✦ Hoje é dia de " + nomeFestival(e) + "! +25% de XP em " + temas
                + " e uma lembrança para quem estiver online.", e.cor, TextDecoration.BOLD));
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
    }

    /** A lembrança do festival (um enfeite de cabeça), uma vez por festival. */
    private void lembranca(Player p, Estacao e) {
        long ano = tempo() / (semana() * 4);
        NamespacedKey k = new NamespacedKey(plugin, "festival_" + e.name().toLowerCase(java.util.Locale.ROOT) + "_" + ano);
        if (p.getPersistentDataContainer().has(k)) return;
        p.getPersistentDataContainer().set(k, PersistentDataType.BYTE, (byte) 1);
        String[] item = switch (e) {
            case PRIMAVERA -> new String[]{"Coroa de Flores", "flowering_azalea"};
            case VERAO -> new String[]{"Chapéu de Palha", "hay_block"};
            case OUTONO -> new String[]{"Lanterna de Abóbora", "jack_o_lantern"};
            case INVERNO -> new String[]{"Cabeça de Boneco de Neve", "carved_pumpkin"};
        };
        ItemStack i = new ItemStack(Material.QUARTZ);
        i.editMeta(m -> {
            m.itemName(Component.text(item[0], e.cor));
            m.setItemModel(NamespacedKey.minecraft(item[1]));
            m.setMaxStackSize(1);
            m.lore(List.of(Component.text("Lembrança da " + nomeFestival(e) + " (ano " + (ano + 1) + ")", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Use no guarda-roupa (cabeça) ou /chapeu", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(new NamespacedKey(plugin, "lembranca"), PersistentDataType.BYTE, (byte) 1);
        });
        p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.sendMessage(Component.text("✦ Você ganhou a lembrança do festival: " + item[0] + "!", e.cor));
    }

    private void efeitosVisuais(Player p, Estacao e) {
        if (p.getWorld().getEnvironment() != World.Environment.NORMAL || p.getGameMode() == GameMode.SPECTATOR) return;
        Location l = p.getLocation();
        if (!aoArLivre(l.getBlock())) return;
        switch (e) {
            case INVERNO -> {
                p.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 6, 0), 40, 8, 3, 8, 0.01);
                if (plugin.settings().invernoMudaBlocos) for (int i = 0; i < 6; i++) nevar(p);
            }
            case OUTONO -> p.spawnParticle(Particle.DUST, l.clone().add(0, 4, 0), 6, 6, 2, 6, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(rnd().nextBoolean() ? 0xD35400 : 0xC98A3C), 1.2f));
            case PRIMAVERA -> {
                if (rnd().nextDouble() < 0.4) p.spawnParticle(Particle.CHERRY_LEAVES, l.clone().add(0, 4, 0), 3, 6, 2, 6, 0);
            }
            default -> { }
        }
    }

    /** Neve num chão ao ar livre ou gelo numa água parada, perto do jogador (fora dos territórios). */
    private void nevar(Player p) {
        if (inverno.size() >= plugin.settings().invernoNeveMax) return;
        World w = p.getWorld();
        int x = p.getLocation().getBlockX() + rnd().nextInt(-16, 17), z = p.getLocation().getBlockZ() + rnd().nextInt(-16, 17);
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return;
        Block topo = w.getHighestBlockAt(x, z);
        if (plugin.territorios().em(topo.getLocation()) != null) return;
        if (topo.getType() == Material.WATER && topo.getBlockData() instanceof Levelled lv && lv.getLevel() == 0) {
            inverno.put(topo.getLocation(), Material.WATER);
            topo.setType(Material.ICE);
            return;
        }
        Block cima = topo.getRelative(0, 1, 0);
        if (topo.getType().isOccluding() && cima.getType().isAir()) {
            inverno.put(cima.getLocation(), Material.AIR);
            cima.setType(Material.SNOW);
        }
    }

    private void derreter(int quantos) {
        Iterator<Map.Entry<Location, Material>> it = inverno.entrySet().iterator();
        while (it.hasNext() && quantos-- > 0) {
            Map.Entry<Location, Material> en = it.next();
            Location l = en.getKey();
            if (!l.isChunkLoaded()) continue;
            Block b = l.getBlock();
            if ((en.getValue() == Material.WATER && b.getType() == Material.ICE) || (en.getValue() == Material.AIR && b.getType() == Material.SNOW)) {
                b.setType(en.getValue());
            }
            it.remove();
        }
    }

    // =====================================================================
    //  Salvar e carregar
    // =====================================================================

    private void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        desvio = y.getLong("desvio", 0);
        for (String s : y.getStringList("inverno")) {
            String[] p = s.split(",");
            if (p.length != 5) continue;
            World w = Bukkit.getWorld(p[0]);
            Material m = Material.matchMaterial(p[4]);
            if (w == null || m == null) continue;
            try { inverno.put(new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])), m); }
            catch (NumberFormatException ignored) { }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("desvio", desvio);
        List<String> l = new ArrayList<>();
        for (Map.Entry<Location, Material> e : inverno.entrySet()) {
            Location x = e.getKey();
            l.add(x.getWorld().getName() + "," + x.getBlockX() + "," + x.getBlockY() + "," + x.getBlockZ() + "," + e.getValue().name());
        }
        y.set("inverno", l);
        try { y.save(arquivo); } catch (IOException ex) { plugin.getLogger().warning("Não foi possível salvar estacoes.yml"); }
    }
}
