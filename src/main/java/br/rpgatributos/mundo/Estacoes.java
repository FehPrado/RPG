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
    /**
     * Blocos que a estação pôs no mundo (para desfazer quando ela acabar): local → {o que era
     * antes, o que foi posto}. Inverno: neve e gelo; primavera: pétalas e flores silvestres;
     * verão: arbustos de vaga-lumes; outono: folhas secas no chão.
     */
    private final Map<Location, Material[]> inverno = new LinkedHashMap<>();
    private long desvio;
    private Estacao anunciada;
    private boolean festivalAnunciado;
    private int segundos;
    /** Quando a limpeza dos blocos de outras estações volta a olhar a lista (0 = já). */
    private long proximaLimpeza;

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
            proximaLimpeza = 0;
            salvar();
        }
        if (festival() && !festivalAnunciado) {
            festivalAnunciado = true;
            anunciarFestival(e);
        }
        if (festival() && segundos % 30 == 0) for (Player p : Bukkit.getOnlinePlayers()) lembranca(p, e);
        if (!inverno.isEmpty()) derreter(e, 200);
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
        boolean muda = plugin.settings().invernoMudaBlocos;
        boolean noite = Ceu.noite(p.getWorld());
        switch (e) {
            case INVERNO -> {
                p.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 6, 0), 40, 8, 3, 8, 0.01);
                if (muda) for (int i = 0; i < 6; i++) nevar(p);
                // 3.12: rios e lagos congelam em volta (viram caminho) e a neve acumula nas nevascas.
                if (muda) for (int i = 0; i < 10; i++) congelarAgua(p);
                if (muda && p.getWorld().hasStorm()) for (int i = 0; i < 3; i++) acumularNeve(p);
            }
            case OUTONO -> {
                // Folhas laranja, vermelhas e marrons caindo, e folhas secas se juntando no chão.
                int[] cores = {0xD35400, 0xC0392B, 0xE67E22, 0x8D5524, 0xF1C40F};
                for (int i = 0; i < 3; i++) {
                    p.spawnParticle(Particle.TINTED_LEAVES, l.clone().add(rnd().nextDouble(-7, 7), rnd().nextDouble(3, 7), rnd().nextDouble(-7, 7)),
                            1, 0, 0, 0, 0, org.bukkit.Color.fromRGB(cores[rnd().nextInt(cores.length)]));
                }
                if (muda) for (int i = 0; i < 3; i++) folhasSecas(p);
                // 3.12: cogumelos debaixo das árvores e abóboras silvestres nas planícies.
                if (muda && rnd().nextDouble() < 0.25) cogumelo(p);
                if (muda && rnd().nextDouble() < 0.03) aboboraSilvestre(p);
            }
            case PRIMAVERA -> {
                if (rnd().nextDouble() < 0.5) p.spawnParticle(Particle.CHERRY_LEAVES, l.clone().add(0, 4, 0), 3, 6, 2, 6, 0);
                if (muda) for (int i = 0; i < 2; i++) florir(p);
                // 3.12: brotos perto das florestas (os que crescerem ficam) e lama nas margens.
                if (muda && rnd().nextDouble() < 0.15) broto(p);
                if (muda && rnd().nextDouble() < 0.5) lama(p);
            }
            case VERAO -> {
                if (noite) p.spawnParticle(Particle.FIREFLY, l.clone().add(0, 1.5, 0), 6, 7, 1.5, 7, 0);
                if (muda && rnd().nextDouble() < 0.2) vagalumes(p);
                // 3.12: a grama seca nas savanas, planícies e terras áridas.
                if (muda && !p.getWorld().hasStorm()) for (int i = 0; i < 2; i++) secar(p);
            }
        }
    }

    private Block chaoLivre(Player p, int raio) {
        if (inverno.size() >= plugin.settings().invernoNeveMax) return null;
        World w = p.getWorld();
        int x = p.getLocation().getBlockX() + rnd().nextInt(-raio, raio + 1), z = p.getLocation().getBlockZ() + rnd().nextInt(-raio, raio + 1);
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
        Block topo = w.getHighestBlockAt(x, z, org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (plugin.territorios().em(topo.getLocation()) != null) return null;
        return topo;
    }

    private void por(Block b, Material antes, String dados) {
        org.bukkit.block.data.BlockData d = org.bukkit.Bukkit.createBlockData(dados);
        inverno.put(b.getLocation(), new Material[]{antes, d.getMaterial()});
        b.setBlockData(d, false);
    }

    private static String direcao() {
        return new String[]{"north", "south", "east", "west"}[rnd().nextInt(4)];
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
            inverno.put(topo.getLocation(), new Material[]{Material.WATER, Material.ICE});
            topo.setType(Material.ICE);
            return;
        }
        Block cima = topo.getRelative(0, 1, 0);
        if (topo.getType().isOccluding() && cima.getType().isAir()) {
            inverno.put(cima.getLocation(), new Material[]{Material.AIR, Material.SNOW});
            cima.setType(Material.SNOW);
        }
    }

    /** Primavera: pétalas e flores silvestres brotam na grama. */
    private void florir(Player p) {
        Block topo = chaoLivre(p, 14);
        if (topo == null || topo.getType() != Material.GRASS_BLOCK) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir() || !aoArLivre(cima)) return;
        String bioma = p.getWorld().getBiome(cima.getX(), cima.getY(), cima.getZ()).getKey().getKey();
        boolean petalas = bioma.contains("cherry") || bioma.contains("forest") && rnd().nextBoolean();
        por(cima, Material.AIR, (petalas ? "pink_petals" : "wildflowers") + "[flower_amount=" + (1 + rnd().nextInt(4)) + ",facing=" + direcao() + "]");
    }

    /** Verão: arbustos de vaga-lumes (que brilham à noite) aparecem aqui e ali. */
    private void vagalumes(Player p) {
        Block topo = chaoLivre(p, 16);
        if (topo == null || topo.getType() != Material.GRASS_BLOCK) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir() || !aoArLivre(cima)) return;
        por(cima, Material.AIR, "firefly_bush");
    }

    /** Outono: folhas secas se juntam no chão debaixo das árvores. */
    private void folhasSecas(Player p) {
        Block topo = chaoLivre(p, 14);
        if (topo == null) return;
        Material m = topo.getType();
        if (m != Material.GRASS_BLOCK && m != Material.PODZOL && m != Material.DIRT && m != Material.COARSE_DIRT) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir()) return;
        boolean copa = false;
        for (int dy = 2; dy <= 10 && !copa; dy++) copa = org.bukkit.Tag.LEAVES.isTagged(topo.getRelative(0, dy, 0).getType());
        if (!copa) return;
        por(cima, Material.AIR, "leaf_litter[segment_amount=" + (1 + rnd().nextInt(4)) + ",facing=" + direcao() + "]");
    }

    // =====================================================================
    //  3.12: as estações mudam mais o mapa (e tudo volta quando a estação acaba)
    // =====================================================================

    private String bioma(Block b) {
        return b.getWorld().getBiome(b.getX(), b.getY(), b.getZ()).getKey().getKey();
    }

    /** Inverno: a superfície da água parada perto do jogador congela (rios e lagos viram caminho). */
    private void congelarAgua(Player p) {
        Block topo = chaoLivre(p, 12);
        if (topo == null || topo.getType() != Material.WATER || !(topo.getBlockData() instanceof Levelled lv) || lv.getLevel() != 0) return;
        if (!topo.getRelative(0, 1, 0).getType().isAir()) return;
        String b = bioma(topo);
        if (b.contains("ocean") && !b.contains("frozen")) return; // o mar aberto não congela
        por(topo, Material.WATER, "ice");
    }

    /** Inverno, nevando: a neve posta pela estação vai engrossando (até 3 camadas). */
    private void acumularNeve(Player p) {
        Block topo = chaoLivre(p, 14);
        if (topo == null) return;
        Block neve = topo.getType() == Material.SNOW ? topo : null;
        if (neve == null || !inverno.containsKey(neve.getLocation())) return;
        if (neve.getBlockData() instanceof org.bukkit.block.data.type.Snow s && s.getLayers() < 3) {
            s.setLayers(s.getLayers() + 1);
            neve.setBlockData(s, false);
        }
    }

    /** Verão: grama seca vira terra grossa nas savanas, planícies e terras áridas (até o outono). */
    private void secar(Player p) {
        Block topo = chaoLivre(p, 16);
        if (topo == null || topo.getType() != Material.GRASS_BLOCK) return;
        String b = bioma(topo);
        if (!b.contains("savanna") && !b.contains("plains") && !b.contains("badlands") && !b.contains("desert")) return;
        if (!topo.getRelative(0, 1, 0).getType().isAir() || rnd().nextDouble() < 0.6) return;
        // Perto da água a grama aguenta.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
            if (topo.getRelative(dx, 0, dz).getType() == Material.WATER) return;
        por(topo, Material.GRASS_BLOCK, "coarse_dirt");
    }

    /** Verão: um raio numa mata seca pode começar um incêndio (se o fogo do servidor espalha, espalha). */
    @EventHandler(ignoreCancelled = true)
    public void aoCairRaio(org.bukkit.event.weather.LightningStrikeEvent e) {
        if (atual() != Estacao.VERAO || !plugin.settings().invernoMudaBlocos || e.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        if (e.getCause() != org.bukkit.event.weather.LightningStrikeEvent.Cause.WEATHER || rnd().nextDouble() > 0.4) return;
        Location l = e.getLightning().getLocation();
        Block chao = l.getWorld().getHighestBlockAt(l);
        String b = bioma(chao);
        if (!b.contains("forest") && !b.contains("savanna") && !b.contains("plains") && !b.contains("taiga") && !b.contains("jungle")) return;
        if (plugin.territorios().em(chao.getLocation()) != null) return;
        int acesos = 0;
        for (int i = 0; i < 6; i++) {
            Block alvo = l.getWorld().getHighestBlockAt(chao.getX() + rnd().nextInt(-2, 3), chao.getZ() + rnd().nextInt(-2, 3));
            Block cima = alvo.getRelative(0, 1, 0);
            if (cima.getType().isAir() && (org.bukkit.Tag.LEAVES.isTagged(alvo.getType()) || alvo.getType() == Material.GRASS_BLOCK || alvo.getType().isFlammable())) {
                cima.setType(Material.FIRE);
                acesos++;
            }
        }
        if (acesos == 0) return;
        for (Player p : l.getWorld().getPlayers())
            if (p.getLocation().distanceSquared(l) < 160 * 160)
                p.sendMessage(Component.text("🔥 Um raio incendiou a mata seca perto de " + chao.getX() + ", " + chao.getZ() + "!", Estacao.VERAO.cor));
    }

    /** Outono: um cogumelo debaixo da copa (na sombra). */
    private void cogumelo(Player p) {
        Block topo = chaoLivre(p, 14);
        if (topo == null) return;
        Material m = topo.getType();
        if (m != Material.GRASS_BLOCK && m != Material.PODZOL && m != Material.DIRT && m != Material.MYCELIUM) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir()) return;
        boolean copa = false;
        for (int dy = 2; dy <= 10 && !copa; dy++) copa = org.bukkit.Tag.LEAVES.isTagged(topo.getRelative(0, dy, 0).getType());
        if (!copa) return;
        por(cima, Material.AIR, rnd().nextBoolean() ? "brown_mushroom" : "red_mushroom");
    }

    /** Outono: uma abóbora silvestre na planície (quem colher fica com ela). */
    private void aboboraSilvestre(Player p) {
        Block topo = chaoLivre(p, 18);
        if (topo == null || topo.getType() != Material.GRASS_BLOCK) return;
        String b = bioma(topo);
        if (!b.contains("plains") && !b.contains("meadow") && !b.contains("forest")) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir() || !aoArLivre(cima)) return;
        por(cima, Material.AIR, "pumpkin");
    }

    /** Primavera: um broto de árvore perto de outras árvores (se crescer, a árvore fica). */
    private void broto(Player p) {
        Block topo = chaoLivre(p, 16);
        if (topo == null || topo.getType() != Material.GRASS_BLOCK) return;
        Block cima = topo.getRelative(0, 1, 0);
        if (!cima.getType().isAir() || !aoArLivre(cima)) return;
        // Só onde já tem árvore perto (a floresta avança, o campo continua campo).
        Material tronco = null;
        for (int dx = -6; dx <= 6 && tronco == null; dx += 2) for (int dz = -6; dz <= 6 && tronco == null; dz += 2)
            for (int dy = 1; dy <= 4; dy++) {
                Material m = topo.getRelative(dx, dy, dz).getType();
                if (org.bukkit.Tag.LOGS.isTagged(m)) { tronco = m; break; }
            }
        if (tronco == null) return;
        String muda = switch (tronco) {
            case BIRCH_LOG -> "birch_sapling";
            case SPRUCE_LOG -> "spruce_sapling";
            case JUNGLE_LOG -> "jungle_sapling";
            case ACACIA_LOG -> "acacia_sapling";
            case CHERRY_LOG -> "cherry_sapling";
            case DARK_OAK_LOG -> "dark_oak_sapling";
            default -> "oak_sapling";
        };
        por(cima, Material.AIR, muda);
    }

    /** Primavera: o degelo deixa lama nas margens (terra e grama encostadas na água). */
    private void lama(Player p) {
        Block topo = chaoLivre(p, 14);
        if (topo == null || (topo.getType() != Material.DIRT && topo.getType() != Material.GRASS_BLOCK)) return;
        boolean margem = false;
        for (org.bukkit.block.BlockFace f : new org.bukkit.block.BlockFace[]{org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST})
            if (topo.getRelative(f).getType() == Material.WATER) margem = true;
        if (!margem || !topo.getRelative(0, 1, 0).getType().isAir()) return;
        por(topo, topo.getType(), "mud");
    }

    /** De que estação é um bloco posto (para tirar só os das outras estações). */
    private static Estacao estacaoDe(Material posto) {
        return switch (posto) {
            case PINK_PETALS, WILDFLOWERS, MUD, OAK_SAPLING, BIRCH_SAPLING, SPRUCE_SAPLING, JUNGLE_SAPLING, ACACIA_SAPLING,
                 CHERRY_SAPLING, DARK_OAK_SAPLING -> Estacao.PRIMAVERA;
            case FIREFLY_BUSH, COARSE_DIRT -> Estacao.VERAO;
            case LEAF_LITTER, BROWN_MUSHROOM, RED_MUSHROOM, PUMPKIN -> Estacao.OUTONO;
            default -> Estacao.INVERNO;
        };
    }

    /**
     * Desfaz até {@code quantos} blocos postos por outras estações. Quando sobra menos que isso
     * para desfazer (o resto está em chunks descarregados), só volta a olhar daqui a 30 s.
     */
    private void derreter(Estacao atual, int quantos) {
        long agora = System.currentTimeMillis();
        if (agora < proximaLimpeza) return;
        int cota = quantos;
        Iterator<Map.Entry<Location, Material[]>> it = inverno.entrySet().iterator();
        while (it.hasNext() && quantos > 0) {
            Map.Entry<Location, Material[]> en = it.next();
            Material[] v = en.getValue();
            if (estacaoDe(v[1]) == atual) continue;
            Location l = en.getKey();
            if (!l.isChunkLoaded()) continue;
            quantos--;
            Block b = l.getBlock();
            if (b.getType() == v[1]) b.setType(v[0]);
            it.remove();
        }
        if (quantos == cota || quantos > 0) proximaLimpeza = agora + 30_000L;
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
            if (p.length != 5 && p.length != 6) continue;
            World w = Bukkit.getWorld(p[0]);
            Material m = Material.matchMaterial(p[4]);
            // Arquivos antigos só tinham o "antes": água virou gelo, ar virou neve.
            Material posto = p.length == 6 ? Material.matchMaterial(p[5]) : m == Material.WATER ? Material.ICE : Material.SNOW;
            if (w == null || m == null || posto == null) continue;
            try { inverno.put(new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])), new Material[]{m, posto}); }
            catch (NumberFormatException ignored) { }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("desvio", desvio);
        List<String> l = new ArrayList<>();
        for (Map.Entry<Location, Material[]> e : inverno.entrySet()) {
            Location x = e.getKey();
            l.add(x.getWorld().getName() + "," + x.getBlockX() + "," + x.getBlockY() + "," + x.getBlockZ() + "," + e.getValue()[0].name() + "," + e.getValue()[1].name());
        }
        y.set("inverno", l);
        try { y.save(arquivo); } catch (IOException ex) { plugin.getLogger().warning("Não foi possível salvar estacoes.yml"); }
    }
}
