package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.domador.Companheiros;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rotina dos animais (2.97): o Comedouro é o "celeiro". No fim da tarde ele toca um sino de vaca e os
 * bichos da fazenda a até 24 blocos vão para perto dele (se o comedouro está debaixo de um teto, eles
 * dormem cobertos); de noite, quem se afasta volta. De manhã, cada um sai para um pedaço de pasto a céu
 * aberto a 8–16 blocos dali. Bichos presos no laço, montados, companheiros e guardiões ficam de fora.
 */
public final class RotinaAnimais {

    private static final Set<EntityType> FAZENDA = EnumSet.of(EntityType.COW, EntityType.SHEEP, EntityType.PIG,
            EntityType.CHICKEN, EntityType.GOAT, EntityType.MOOSHROOM, EntityType.RABBIT);
    private static final int RAIO = 24;

    private enum Fase { RECOLHER, NOITE, SOLTAR, DIA }

    private final RPGAtributos plugin;
    /** Bicho → para onde ele está indo agora (o lugar sorteado fica o mesmo até a fase mudar). */
    private final Map<UUID, Location> destino = new HashMap<>();
    private final Map<UUID, Fase> faseDe = new HashMap<>();
    /** Comedouros que já tocaram o sino neste fim de tarde (chave: mundo,x,y,z + dia). */
    private final Set<String> tocados = new HashSet<>();

    public RotinaAnimais(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static Fase fase(long t) {
        if (t >= 12000 && t < 13500) return Fase.RECOLHER;
        if (t >= 13500 && t < 23300) return Fase.NOITE;
        if (t >= 23300 || t < 1500) return Fase.SOLTAR;
        return Fase.DIA;
    }

    /** A cada 2 s. */
    public void tick() {
        if (!plugin.settings().mundoRotina) return;
        Set<Location> comedouros = new HashSet<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            comedouros.addAll(plugin.comedouros().perto(p.getLocation(), 96));
        }
        Set<UUID> vistos = new HashSet<>();
        for (Location c : comedouros) {
            World w = c.getWorld();
            Fase f = fase(w.getTime());
            if (f == Fase.DIA) continue;
            Location centro = c.clone().add(0.5, 0, 0.5);
            if (f == Fase.RECOLHER) {
                String chave = w.getName() + "," + c.getBlockX() + "," + c.getBlockY() + "," + c.getBlockZ() + "," + (w.getFullTime() / 24000);
                if (tocados.add(chave)) w.playSound(centro, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1.2f, 0.9f);
            }
            for (Entity e : w.getNearbyEntities(centro, RAIO, 8, RAIO)) {
                if (!(e instanceof Animals a) || !FAZENDA.contains(e.getType()) || !vistos.add(e.getUniqueId())) continue;
                if (a.isLeashed() || a.isInsideVehicle() || !a.getPassengers().isEmpty() || a.isLoveMode() || Companheiros.eh(a)) continue;
                mover(a, centro, f);
            }
        }
        destino.keySet().retainAll(vistos);
        faseDe.keySet().retainAll(vistos);
        if (tocados.size() > 500) tocados.clear();
    }

    private void mover(Animals a, Location comedouro, Fase f) {
        Fase antes = faseDe.put(a.getUniqueId(), f);
        boolean recolher = f == Fase.RECOLHER || f == Fase.NOITE;
        boolean mudou = antes == null || (antes == Fase.SOLTAR) == recolher;
        Location alvo = destino.get(a.getUniqueId());
        if (mudou || alvo == null) {
            alvo = recolher ? pertoDe(comedouro) : pasto(comedouro);
            if (alvo == null) return;
            destino.put(a.getUniqueId(), alvo);
        }
        double d2 = a.getLocation().distanceSquared(alvo);
        // De noite só corrige quem se afastou; no resto, segue até chegar.
        if (f == Fase.NOITE && a.getLocation().distanceSquared(comedouro) < 36) return;
        if (d2 > 4) a.getPathfinder().moveTo(alvo, recolher ? 1.1 : 0.9);
    }

    /** Um lugar de pé a até 3 blocos do comedouro. */
    private static Location pertoDe(Location c) {
        for (int i = 0; i < 12; i++) {
            Block b = c.getBlock().getRelative(rnd().nextInt(-3, 4), 0, rnd().nextInt(-3, 4));
            Block pe = chao(b);
            if (pe != null) return pe.getLocation().add(0.5, 0, 0.5);
        }
        return c;
    }

    /** Um pedaço de pasto a céu aberto a 8–16 blocos. */
    private static Location pasto(Location c) {
        for (int i = 0; i < 16; i++) {
            double ang = rnd().nextDouble(Math.PI * 2), dist = rnd().nextDouble(8, 16);
            Block b = c.getBlock().getRelative((int) Math.round(Math.cos(ang) * dist), 0, (int) Math.round(Math.sin(ang) * dist));
            Block pe = chao(b);
            if (pe != null && pe.getLightFromSky() >= 14 && pe.getRelative(BlockFace.DOWN).getType() == Material.GRASS_BLOCK) {
                return pe.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    /** O espaço livre em cima de chão firme, procurando 3 blocos para cima e para baixo. */
    private static Block chao(Block b) {
        for (int dy = 3; dy >= -3; dy--) {
            Block pe = b.getRelative(0, dy, 0);
            if (pe.isPassable() && !pe.isLiquid() && pe.getRelative(BlockFace.UP).isPassable()
                    && pe.getRelative(BlockFace.DOWN).getType().isSolid()) return pe;
        }
        return null;
    }
}
