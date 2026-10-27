package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.detalhes.Natureza;
import br.rpgatributos.estrada.Placas;
import br.rpgatributos.estruturas.Estruturas;
import br.rpgatributos.masmorra.Masmorras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bandos que atravessam o mapa (2.97). De vez em quando, perto de quem explora a céu aberto, aparece um
 * bando do bioma (manada de cavalos, rebanho de cabras, alcateia, raposas, camelos, lhamas, papagaios)
 * que anda num rumo: para a estrutura mais perto que aquele jogador ainda não visitou (até 800 blocos),
 * ou só para longe. Quem segue o bando chega lá; ao chegar, o líder vira um animal raro (de Ouro ou de
 * Neve) que fica no lugar, e o resto se espalha. Os bichos do bando não são salvos no mundo (somem se
 * ninguém estiver perto), menos o que for domado no caminho e o líder que chegou.
 */
public final class Bandos implements Listener {

    public static final TextColor COR = TextColor.color(0xB5D99C);
    private static final long VIDA_MS = 8 * 60_000L;
    private static final int PASSO = 20;

    private record Tipo(String nome, EntityType bicho, int min, int max, Sound som) { }

    private static final class Bando {
        final List<UUID> membros = new ArrayList<>();
        final UUID dono;
        final Location destino;
        final String nomeDestino;
        final Tipo tipo;
        final long nasceu = System.currentTimeMillis();
        Location ultimo;
        int parado;
        long semNinguem;

        Bando(UUID dono, Location destino, String nomeDestino, Tipo tipo) {
            this.dono = dono;
            this.destino = destino;
            this.nomeDestino = nomeDestino;
            this.tipo = tipo;
        }

        UUID lider() { return membros.isEmpty() ? null : membros.get(0); }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kBando;
    private final List<Bando> bandos = new ArrayList<>();
    private final Map<UUID, Long> espera = new HashMap<>();
    private int passos;

    public Bandos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kBando = new NamespacedKey(plugin, "bando");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static Tipo tipoDo(String bioma, boolean inverno) {
        if (bioma.contains("jungle")) return new Tipo("Bando de papagaios", EntityType.PARROT, 5, 8, Sound.ENTITY_PARROT_AMBIENT);
        if (bioma.contains("desert")) return new Tipo("Caravana de camelos selvagens", EntityType.CAMEL, 2, 4, Sound.ENTITY_CAMEL_AMBIENT);
        if (bioma.contains("badlands")) return new Tipo("Tropa de lhamas", EntityType.LLAMA, 3, 5, Sound.ENTITY_LLAMA_AMBIENT);
        if (bioma.contains("savanna")) return new Tipo("Manada de cavalos selvagens", EntityType.HORSE, 4, 6, Sound.ENTITY_HORSE_GALLOP);
        if (bioma.contains("slopes") || bioma.contains("peaks") || bioma.contains("grove") || bioma.contains("windswept") || bioma.contains("stony")) {
            return new Tipo("Rebanho de cabras-monteses", EntityType.GOAT, 4, 6, Sound.ENTITY_GOAT_AMBIENT);
        }
        if ((bioma.contains("taiga") || bioma.contains("snowy")) && inverno) return new Tipo("Alcateia", EntityType.WOLF, 3, 5, Sound.ENTITY_WOLF_GROWL);
        if (bioma.contains("taiga")) return new Tipo("Família de raposas", EntityType.FOX, 3, 4, Sound.ENTITY_FOX_AMBIENT);
        if (bioma.contains("plains") || bioma.contains("meadow")) return new Tipo("Manada de cavalos selvagens", EntityType.HORSE, 4, 6, Sound.ENTITY_HORSE_GALLOP);
        if (bioma.contains("forest") || bioma.contains("birch")) return new Tipo("Família de raposas", EntityType.FOX, 3, 4, Sound.ENTITY_FOX_AMBIENT);
        return null;
    }

    private boolean pode(Player p) {
        World w = p.getWorld();
        long t = w.getTime();
        return p.getGameMode() == GameMode.SURVIVAL && w.getEnvironment() == World.Environment.NORMAL && !w.getName().equals(Masmorras.MUNDO)
                && t > 500 && t < 11500 && p.getLocation().getBlock().getLightFromSky() >= 14 && !p.isInsideVehicle()
                && plugin.territorios().em(p.getLocation()) == null;
    }

    /** A cada 2 s: os bandos andam; a cada 20 s, talvez nasça um. */
    public void tick() {
        long agora = System.currentTimeMillis();
        andar(agora);
        if (!plugin.settings().mundoBandos || ++passos % 10 != 0) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (agora < espera.getOrDefault(p.getUniqueId(), 0L) || !pode(p) || rnd().nextDouble() >= 0.12) continue;
            espera.put(p.getUniqueId(), agora + plugin.settings().mundoBandosMinutos * 60_000L);
            comecar(p, null);
        }
    }

    /** Começa um bando perto do jogador (o admin pode forçar mesmo sem o bioma certo: usa cavalos). @return o nome ou null. */
    public String comecar(Player p, String forcado) {
        World w = p.getWorld();
        String bioma = p.getLocation().getBlock().getBiome().getKey().getKey();
        boolean inverno = plugin.estacoes() != null && plugin.estacoes().atual() == br.rpgatributos.mundo.Estacoes.Estacao.INVERNO;
        Tipo tipo = tipoDo(bioma, inverno);
        if (tipo == null && forcado != null) tipo = new Tipo("Manada de cavalos selvagens", EntityType.HORSE, 4, 6, Sound.ENTITY_HORSE_GALLOP);
        if (tipo == null) return null;
        Estruturas.Lugar alvo = plugin.estruturas().naoVisitadaMaisPerto(p.getUniqueId(), p.getLocation(), 800);
        Location destino;
        String nomeDestino;
        if (alvo != null) {
            destino = new Location(w, alvo.x() + 0.5, 64, alvo.z() + 0.5);
            nomeDestino = alvo.nome();
        } else {
            double ang = rnd().nextDouble(Math.PI * 2);
            destino = p.getLocation().add(Math.cos(ang) * 300, 0, Math.sin(ang) * 300);
            nomeDestino = null;
        }
        // Nasce de lado, a 35–50 blocos, e passa na frente do jogador rumo ao destino.
        double rumo = Math.atan2(destino.getZ() - p.getLocation().getZ(), destino.getX() - p.getLocation().getX());
        double lado = rumo + (rnd().nextBoolean() ? Math.PI / 2 : -Math.PI / 2) + rnd().nextDouble(-0.5, 0.5);
        double d = rnd().nextDouble(35, 50);
        Location nasce = chao(p.getLocation().add(Math.cos(lado) * d, 0, Math.sin(lado) * d));
        if (nasce == null || !nasce.isChunkLoaded()) return null;
        Bando b = new Bando(p.getUniqueId(), destino, nomeDestino, tipo);
        int n = rnd().nextInt(tipo.min(), tipo.max() + 1);
        for (int i = 0; i < n; i++) {
            Location l = chao(nasce.clone().add(rnd().nextDouble(-4, 4), 0, rnd().nextDouble(-4, 4)));
            if (l == null) l = nasce;
            if (tipo.bicho() == EntityType.PARROT) l.add(0, 6, 0);
            Entity e = w.spawnEntity(l, tipo.bicho());
            e.setPersistent(false);
            e.getPersistentDataContainer().set(kBando, PersistentDataType.BYTE, (byte) 1);
            if (e instanceof LivingEntity le) le.setRemoveWhenFarAway(true);
            b.membros.add(e.getUniqueId());
        }
        bandos.add(b);
        w.playSound(nasce, tipo.som(), SoundCategory.NEUTRAL, 1.5f, 1f);
        p.sendActionBar(Component.text("🐾 " + tipo.nome() + " passa ao longe, rumo ao " + Placas.rumo(nasce, destino) + ". Siga o bando.", COR));
        return tipo.nome();
    }

    private void andar(long agora) {
        for (Iterator<Bando> it = bandos.iterator(); it.hasNext(); ) {
            Bando b = it.next();
            Entity lider = b.lider() == null ? null : plugin.getServer().getEntity(b.lider());
            if (lider == null || !lider.isValid() || agora - b.nasceu > VIDA_MS) {
                espalhar(b);
                it.remove();
                continue;
            }
            boolean alguem = false;
            for (Player p : lider.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(lider.getLocation()) < 96 * 96) { alguem = true; break; }
            }
            if (!alguem) {
                if (b.semNinguem == 0) b.semNinguem = agora;
                if (agora - b.semNinguem > 40_000) {
                    remover(b);
                    it.remove();
                }
                continue;
            }
            b.semNinguem = 0;
            Location l = lider.getLocation();
            double falta = Math.hypot(b.destino.getX() - l.getX(), b.destino.getZ() - l.getZ());
            if (falta < 14) {
                chegar(b, lider);
                it.remove();
                continue;
            }
            // O líder anda até um ponto 20 blocos à frente; se não andou, pula um pedaço.
            double ang = Math.atan2(b.destino.getZ() - l.getZ(), b.destino.getX() - l.getX());
            Location prox = chao(l.clone().add(Math.cos(ang) * Math.min(PASSO, falta), 0, Math.sin(ang) * Math.min(PASSO, falta)));
            if (b.ultimo != null && b.ultimo.distanceSquared(l) < 4) b.parado++;
            else b.parado = 0;
            b.ultimo = l.clone();
            if (prox != null && lider instanceof Mob m) {
                if (b.parado >= 2 && prox.isChunkLoaded()) {
                    Location salto = chao(l.clone().add(Math.cos(ang) * 6, 0, Math.sin(ang) * 6));
                    if (salto != null) lider.teleport(salto);
                    b.parado = 0;
                }
                if (b.tipo.bicho() == EntityType.PARROT) prox.add(0, 5, 0);
                m.getPathfinder().moveTo(prox, 1.25);
            }
            for (int i = 1; i < b.membros.size(); i++) {
                Entity e = plugin.getServer().getEntity(b.membros.get(i));
                if (!(e instanceof Mob m) || !e.isValid()) continue;
                if (e.getLocation().distanceSquared(l) > 24 * 24) {
                    Location perto = chao(l.clone().add(rnd().nextDouble(-3, 3), 0, rnd().nextDouble(-3, 3)));
                    e.teleport(perto != null ? perto : l);
                } else m.getPathfinder().moveTo(l.clone().add(rnd().nextDouble(-3, 3), 0, rnd().nextDouble(-3, 3)), 1.3);
            }
            if (rnd().nextInt(3) == 0) l.getWorld().playSound(l, b.tipo.som(), SoundCategory.NEUTRAL, 1f, 1f);
        }
    }

    private void chegar(Bando b, Entity lider) {
        if (lider instanceof LivingEntity le) {
            plugin.natureza().tornarRaro(le, rnd().nextBoolean() ? Natureza.Raridade.OURO : Natureza.Raridade.NEVE);
            le.setPersistent(true);
            le.getPersistentDataContainer().remove(kBando);
        }
        Location l = lider.getLocation();
        for (Player p : l.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(l) < 64 * 64) {
                p.sendMessage(Component.text("🐾 O bando chegou" + (b.nomeDestino != null ? " perto de " + b.nomeDestino : "")
                        + ". O líder ficou: um animal raro.", COR));
            }
        }
        if (b.nomeDestino != null) plugin.noticias().registrar("Um bando foi visto chegando perto de " + b.nomeDestino + ".");
        b.membros.remove(0);
        espalhar(b);
    }

    /** Os outros se espalham e somem daqui a pouco. */
    private void espalhar(Bando b) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> remover(b), 60 * 20L);
    }

    private void remover(Bando b) {
        for (UUID id : b.membros) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null && e.getPersistentDataContainer().has(kBando)) e.remove();
        }
    }

    /** Domou um bicho do bando: ele fica (é salvo no mundo). */
    @EventHandler
    public void aoDomar(EntityTameEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(kBando)) return;
        e.getEntity().getPersistentDataContainer().remove(kBando);
        e.getEntity().setPersistent(true);
    }

    public void desligar() {
        for (Bando b : bandos) remover(b);
        bandos.clear();
    }

    private static Location chao(Location l) {
        World w = l.getWorld();
        if (!w.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) return null;
        Block topo = w.getHighestBlockAt(l.getBlockX(), l.getBlockZ());
        if (topo.isLiquid()) return null;
        return topo.getLocation().add(0.5, 1, 0.5);
    }
}
