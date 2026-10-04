package br.rpgatributos;

import br.rpgatributos.arcano.BlocosTemporarios;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Escuta o que o jogador faz e transforma em XP. */
public final class SkillListener implements Listener {

    private final RPGAtributos plugin;

    /** Blocos colocados por jogadores: quebrar eles não dá XP (evita farm de colocar/quebrar). */
    private final MarcadorDeBlocos colocados;
    /** Blocos quebrados naturalmente que podem dar drop duplo. */
    private final Set<String> podeDuplicar = new HashSet<>();
    /** Último valor da estatística de corrida de cada jogador. */
    private final Map<UUID, Integer> ultimaCorrida = new HashMap<>();
    private final Map<UUID, Integer> ultimoNado = new HashMap<>();
    /** Onde o jogador ganhou XP de pulo pela última vez (pular parado no lugar não conta). */
    private final Map<UUID, Location> ultimoPulo = new HashMap<>();
    /** "matador:vítima" → quando ganhou XP por essa morte (evita farm entre dois jogadores). */
    private final Map<String, Long> ultimaMortePvp = new HashMap<>();

    public SkillListener(RPGAtributos plugin) {
        this.plugin = plugin;
        this.colocados = new MarcadorDeBlocos(plugin, "colocados");
    }

    private StatsManager stats() { return plugin.stats(); }
    private Settings cfg() { return plugin.settings(); }

    private static String chave(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private static boolean ehMinerio(Material m) {
        return m.name().endsWith("_ORE");
    }

    private static boolean contaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    /** Blocos que dão XP ao quebrar: só esses precisam ser lembrados. */
    private static boolean daXp(Material m) {
        return Tag.LOGS.isTagged(m) || Tag.MINEABLE_PICKAXE.isTagged(m);
    }

    // ---------------- entrar / sair ----------------

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.bonus().aplicar(p);
        ultimaCorrida.put(p.getUniqueId(), p.getStatistic(Statistic.SPRINT_ONE_CM));
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ultimaCorrida.remove(e.getPlayer().getUniqueId());
        ultimoNado.remove(e.getPlayer().getUniqueId());
        ultimoPulo.remove(e.getPlayer().getUniqueId());
    }

    // ---------------- mineração e madeira ----------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (BlocosTemporarios.simulando()) return; // teste de proteção das magias: nada foi colocado
        Block b = e.getBlockPlaced();
        if (daXp(b.getType())) colocados.marcar(b);
    }

    /** Pedra de gerador de lava+água (e concreto) não dá XP: evita farm de gerador de pedregulho. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFormar(BlockFormEvent e) {
        if (daXp(e.getNewState().getType())) colocados.marcar(e.getBlock());
    }

    /** Pistão empurrando bloco colocado: a marca vai junto com o bloco. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoEmpurrar(BlockPistonExtendEvent e) {
        moverMarcas(e.getBlock(), e.getBlocks(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPuxar(BlockPistonRetractEvent e) {
        moverMarcas(e.getBlock(), e.getBlocks(), false);
    }

    private void moverMarcas(Block pistao, List<Block> blocos, boolean estendendo) {
        if (!(pistao.getBlockData() instanceof Directional d)) return;
        BlockFace sentido = estendendo ? d.getFacing() : d.getFacing().getOppositeFace();
        List<Block> marcados = new ArrayList<>();
        for (Block b : blocos) {
            if (colocados.desmarcar(b)) marcados.add(b);
        }
        for (Block b : marcados) colocados.marcar(b.getRelative(sentido));
    }

    /** Quando o jogador começa a quebrar, ajusta a velocidade conforme o tipo de bloco. */
    @EventHandler(ignoreCancelled = true)
    public void aoComecarQuebrar(BlockDamageEvent e) {
        Player p = e.getPlayer();
        Material tipo = e.getBlock().getType();
        double bonus = 0;
        if (Tag.LOGS.isTagged(tipo)) {
            bonus = stats().getNivel(p, Skill.MADEIRA) * cfg().madVelocidade;
        } else if (Tag.MINEABLE_PICKAXE.isTagged(tipo)) {
            bonus = stats().getNivel(p, Skill.MINERACAO) * cfg().minVelocidade;
        }
        plugin.bonus().definirVelocidadeQuebra(p, bonus);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Material tipo = b.getType();
        if (!daXp(tipo)) return;
        boolean foiColocado = colocados.desmarcar(b);
        if (foiColocado || !contaXp(p)) return;

        String k = chave(b);
        if (Tag.LOGS.isTagged(tipo)) {
            stats().darXp(p, Skill.MADEIRA, cfg().madXpTronco);
            plugin.titulos().registrar(p, "troncos", 1); // feito das lendas
            marcarDuplicavel(k);
        } else if (ehMinerio(tipo)) {
            stats().darXp(p, Skill.MINERACAO, cfg().minXpMinerio);
            plugin.titulos().registrar(p, "minerios", 1); // feito das lendas
            marcarDuplicavel(k);
        } else if (Tag.MINEABLE_PICKAXE.isTagged(tipo)) {
            stats().darXp(p, Skill.MINERACAO, cfg().minXpPedra);
        }
    }

    private void marcarDuplicavel(String k) {
        podeDuplicar.add(k);
        // O drop acontece no mesmo tick; limpa depois por segurança.
        plugin.getServer().getScheduler().runTask(plugin, () -> podeDuplicar.remove(k));
    }

    /** Chance de drop em dobro para minérios e troncos. */
    @EventHandler(ignoreCancelled = true)
    public void aoDropar(BlockDropItemEvent e) {
        if (!podeDuplicar.remove(chave(e.getBlock()))) return;
        Player p = e.getPlayer();
        Material tipo = e.getBlockState().getType();

        double chance;
        if (Tag.LOGS.isTagged(tipo)) {
            chance = stats().getNivel(p, Skill.MADEIRA) * cfg().madChanceDupla + plugin.talentos().chanceDupla(p);
        } else if (ehMinerio(tipo)) {
            chance = stats().getNivel(p, Skill.MINERACAO) * cfg().minChanceDupla + plugin.talentos().chanceDupla(p);
        } else {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        boolean duplicou = false;
        Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (Item item : e.getItems()) {
            ItemStack stack = item.getItemStack();
            // Com Toque Suave o minério dropa ele mesmo: não duplica para não virar exploit.
            if (ehMinerio(tipo) && stack.getType() == tipo) continue;
            loc.getWorld().dropItemNaturally(loc, stack.clone());
            duplicou = true;
        }
        if (duplicou) {
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.3, 0.3, 0.3);
            p.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
        }
    }

    // ---------------- pulo e queda ----------------

    /** Só conta pulo depois de andar um pouco desde o último que deu XP (pular parado não farma). */
    @EventHandler(ignoreCancelled = true)
    public void aoPular(PlayerJumpEvent e) {
        Player p = e.getPlayer();
        if (!contaXp(p)) return;
        Location agora = e.getFrom();
        Location antes = ultimoPulo.get(p.getUniqueId());
        if (antes != null && antes.getWorld().equals(agora.getWorld())) {
            double dx = agora.getX() - antes.getX();
            double dz = agora.getZ() - antes.getZ();
            double min = cfg().puloDistanciaMinima;
            if (dx * dx + dz * dz < min * min) return;
        }
        ultimoPulo.put(p.getUniqueId(), agora.clone());
        stats().darXp(p, Skill.PULO, cfg().puloXpPulo);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p) || !contaXp(p)) return;
        stats().darXp(p, Skill.PULO, e.getFinalDamage() * cfg().puloXpQueda);
    }

    // ---------------- combate ----------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || p == morto || !contaXp(p)) return;

        double xp;
        if (morto instanceof Player vitima) {
            if (!podeGanharXpPvp(p, vitima)) return;
            xp = cfg().comXpJogador;
        }
        else if (morto instanceof Enemy) xp = cfg().comXpMonstro;
        else xp = cfg().comXpAnimal;

        if (morto.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            xp *= cfg().comMultSpawner;
        }
        stats().darXp(p, Skill.COMBATE, xp);
        // Party: quem está perto ganha uma parte, e quem matou ganha um bônus.
        if (!(morto instanceof Player)) plugin.parties().compartilharXp(p, morto.getLocation(), xp);
    }

    /** Bloqueia farm de PvP: conta do mesmo IP e matar a mesma pessoa várias vezes seguidas. */
    private boolean podeGanharXpPvp(Player matador, Player vitima) {
        if (cfg().comSemXpMesmoIp && matador.getAddress() != null && vitima.getAddress() != null
                && matador.getAddress().getAddress().equals(vitima.getAddress().getAddress())) {
            return false;
        }
        long agora = System.currentTimeMillis();
        long espera = cfg().comCooldownMesmoJogadorMs;
        if (ultimaMortePvp.size() > 500) ultimaMortePvp.values().removeIf(t -> agora - t >= espera);
        String par = matador.getUniqueId() + ":" + vitima.getUniqueId();
        Long ultima = ultimaMortePvp.get(par);
        if (ultima != null && agora - ultima < espera) return false;
        ultimaMortePvp.put(par, agora);
        return true;
    }

    // ---------------- corrida (roda 1x por segundo) ----------------

    public void verificarCorrida() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            int agora = p.getStatistic(Statistic.SPRINT_ONE_CM);
            Integer antes = ultimaCorrida.put(p.getUniqueId(), agora);
            if (antes != null && agora > antes && contaXp(p)) {
                stats().darXp(p, Skill.CORRIDA, (agora - antes) / 100.0 * cfg().corXpPorBloco);
            }
            verificarNado(p);
        }
    }

    /**
     * Natação: nadar e andar debaixo d'água. Só conta com o jogador apertando para
     * se mover (ser levado pela correnteza parado não dá XP).
     */
    private void verificarNado(Player p) {
        int agora = p.getStatistic(Statistic.SWIM_ONE_CM) + p.getStatistic(Statistic.WALK_UNDER_WATER_ONE_CM);
        Integer antes = ultimoNado.put(p.getUniqueId(), agora);
        if (antes == null || agora <= antes || !contaXp(p) || !p.isInWater()) return;
        Input in = p.getCurrentInput();
        if (!(in.isForward() || in.isBackward() || in.isLeft() || in.isRight() || in.isJump())) return;
        double blocos = Math.min(12, (agora - antes) / 100.0); // limite por segundo contra correntezas fortes
        stats().darXp(p, Skill.NATACAO, blocos * cfg().natXpPorBloco);
    }
}
