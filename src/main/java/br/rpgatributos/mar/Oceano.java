package br.rpgatributos.mar;

import br.rpgatributos.ModoTeste;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.mundo.Ceu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * O mar aberto vivo (3.0, "Mundo e Mar"): cardumes e baleias, a tempestade no mar, o Kraken e o
 * Navio Fantasma. As estruturas fixas do mar (ilhotas, navio pirata, iceberg, barco mercante e
 * Ilha-Tartaruga) ficam em {@link br.rpgatributos.estruturas.MarAberto}.
 */
public final class Oceano implements Listener {

    private final RPGAtributos plugin;
    private final Cardumes cardumes;
    private final Kraken kraken;
    private final NavioFantasma fantasma;
    private final Tempestade tempestade;
    private int ciclo;
    /** Para as contas estáticas que precisam do Barco Grande. */
    private static RPGAtributos instancia;

    public Oceano(RPGAtributos plugin) {
        this.plugin = plugin;
        instancia = plugin;
        this.cardumes = new Cardumes(plugin);
        this.kraken = new Kraken(plugin);
        this.fantasma = new NavioFantasma(plugin);
        this.tempestade = new Tempestade(plugin);
    }

    public Cardumes cardumes() {
        return cardumes;
    }

    public void carregar() {
        fantasma.carregar();
    }

    // =====================================================================
    //  Onde é mar
    // =====================================================================

    /** O lugar é mar aberto (bioma de oceano; na área de testes, qualquer água funda). */
    public static boolean noMar(Location l) {
        World w = l.getWorld();
        if (ModoTeste.ehTeste(w)) return superficie(w, l.getBlockX(), l.getBlockZ(), 4) != null;
        return w.getBiome(l.getBlockX(), Math.min(l.getBlockY(), w.getSeaLevel() - 1), l.getBlockZ()).getKey().getKey().contains("ocean");
    }

    /** Mar fundo (bioma "deep"; na área de testes, 8 blocos de água). */
    public static boolean marFundo(Location l) {
        World w = l.getWorld();
        if (ModoTeste.ehTeste(w)) return superficie(w, l.getBlockX(), l.getBlockZ(), 8) != null;
        String b = w.getBiome(l.getBlockX(), Math.min(l.getBlockY(), w.getSeaLevel() - 1), l.getBlockZ()).getKey().getKey();
        return b.contains("ocean") && b.contains("deep");
    }

    /**
     * A altura da superfície da água na coluna (o topo do bloco de água mais alto), se houver ao
     * menos {@code profundidade} blocos de água. Null se for raso ou terra.
     */
    public static Double superficie(World w, int x, int z, int profundidade) {
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
        int y = w.getHighestBlockYAt(x, z);
        Block topo = w.getBlockAt(x, y, z);
        if (topo.getType() != Material.WATER) return null;
        for (int d = 1; d < profundidade; d++) {
            Material m = w.getBlockAt(x, y - d, z).getType();
            if (m != Material.WATER && m != Material.KELP && m != Material.KELP_PLANT && m != Material.SEAGRASS && m != Material.TALL_SEAGRASS) return null;
        }
        return y + 1.0;
    }

    /** O barco do jogador (pilotando, num barco comum ou num banco do Barco Grande). */
    public static Boat barco(Player p) {
        Entity v = p.getVehicle();
        if (v instanceof Boat b) return b;
        return v != null && instancia != null ? instancia.barcoGrande().barcoDe(p) : null;
    }

    /** O jogador está na superfície (de barco, nadando ou boiando). */
    static boolean naSuperficie(Player p) {
        if (barco(p) != null) return true;
        Location l = p.getLocation();
        Double s = superficie(l.getWorld(), l.getBlockX(), l.getBlockZ(), 2);
        return s != null && Math.abs(l.getY() - s) <= 2.5;
    }

    // =====================================================================
    //  O relógio do mar
    // =====================================================================

    /** A cada 2 ticks: o Kraken mexe os tentáculos. */
    public void tick() {
        kraken.mover();
        if (Bukkit.getCurrentTick() % 10 == 0) cardumes.mostrar();
    }

    /** A cada segundo. */
    public void segundo() {
        ciclo++;
        cardumes.segundo(ciclo);
        kraken.segundo();
        fantasma.segundo(ciclo);
        tempestade.segundo(ciclo);
        if (ciclo % 60 == 0) talvezPerigos();
    }

    /** De minuto em minuto: o mar pode trazer o Kraken ou o Navio Fantasma para quem navega. */
    private void talvezPerigos() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long agora = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR || p.getGameMode() == org.bukkit.GameMode.CREATIVE) continue;
            Boat b = barco(p);
            if (b == null || ModoTeste.ehTeste(p.getWorld()) || plugin.masmorras().ehMundo(p.getWorld())) continue;
            Location l = b.getLocation();
            World w = l.getWorld();
            boolean noite = Ceu.noite(w);
            if (plugin.settings().marKraken && !kraken.ativo() && agora >= kraken.proximoNatural && marFundo(l)
                    && (noite || w.isThundering()) && r.nextDouble() < plugin.settings().marKrakenChance) {
                if (kraken.invocar(p) == null) kraken.proximoNatural = agora + 30 * 60_000L;
                return;
            }
            boolean nevoa = plugin.ceu().luaNova() || plugin.ceu().clima() == Ceu.Clima.NEBLINA;
            if (plugin.settings().marFantasma && !fantasma.ativo() && agora >= fantasma.proximoNatural && noite && nevoa && noMar(l)
                    && r.nextDouble() < plugin.settings().marFantasmaChance) {
                if (fantasma.surgir(p) == null) fantasma.proximoNatural = agora + 60 * 60_000L;
                return;
            }
        }
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageEvent e) {
        kraken.aoFerir(e);
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        kraken.aoMorrer(e);
        fantasma.aoMorrer(e);
    }

    /** Peixes de cardume, baleias, Kraken e tripulação fantasma que ficaram para trás somem. */
    @EventHandler
    public void aoCarregar(EntitiesLoadEvent e) {
        for (Entity x : e.getEntities()) {
            if (Cardumes.sobra(x) && !cardumes.conhecido(x)) x.remove();
            else if (Kraken.sobra(x) && !kraken.conhecido(x)) x.remove();
            else if (NavioFantasma.sobra(x) && !fantasma.conhecido(x)) x.remove();
        }
    }

    // =====================================================================
    //  Administrador
    // =====================================================================

    /** {@code /rpgadmin mar <...>}. @return a mensagem para o administrador. */
    public String admin(Player p, String o) {
        return switch (o) {
            case "cardume" -> ok(cardumes.criarAqui(p, false), "Cardume aqui (dura uns 5 min).");
            case "cardumegrande", "baleia" -> ok(cardumes.criarAqui(p, true), "Cardume grande com baleia aqui (dura uns 8 min).");
            case "kraken" -> ok(kraken.invocar(p), "O Kraken subiu em volta de você.");
            case "fantasma" -> ok(fantasma.surgir(p), "O Navio Fantasma surgiu à sua frente (some ao amanhecer).");
            case "fantasmasumir" -> {
                fantasma.sumir(true);
                yield "O Navio Fantasma sumiu.";
            }
            case "tempestade" -> {
                World w = p.getWorld();
                w.setStorm(true);
                w.setThundering(true);
                w.setThunderDuration(6000);
                w.setWeatherDuration(6000);
                yield "Trovoada por 5 minutos neste mundo.";
            }
            case "limpar" -> {
                cardumes.limpar();
                kraken.retirar(null);
                fantasma.sumir(false);
                yield "Cardumes, Kraken e Navio Fantasma tirados.";
            }
            default -> "Use: /rpgadmin mar <cardume|cardumegrande|kraken|fantasma|fantasmasumir|tempestade|itens|limpar>";
        };
    }

    private static String ok(String erro, String sucesso) {
        return erro != null ? erro : sucesso;
    }

    public String resumo() {
        return cardumes.quantos() + " cardume(s), Kraken " + (kraken.ativo() ? "no mar" : "dormindo")
                + ", Navio Fantasma " + (fantasma.ativo() ? "no mar" : "longe");
    }

    public void desligar() {
        kraken.retirar(null);
        cardumes.limpar();
        fantasma.sumir(false);
    }
}
