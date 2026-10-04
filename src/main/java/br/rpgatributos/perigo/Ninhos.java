package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ninhos de monstros: surgem longe dos territórios, perto de quem está explorando.
 * Enquanto houver alguém perto, soltam ondas de guardiões. O núcleo só quebra quando
 * os guardiões vivos forem derrotados, e então solta um bom saque.
 */
public final class Ninhos implements Listener {

    private static final long DURACAO_MS = 2 * 60 * 60 * 1000;
    private static final int MAX_VIVOS = 8;

    private enum Tipo {
        ZUMBIS("Ninho de Mortos-Vivos", Material.MOSSY_COBBLESTONE, 0x5BA052, EntityType.ZOMBIE, EntityType.HUSK),
        ESQUELETOS("Ossário", Material.BONE_BLOCK, 0xE0DCC8, EntityType.SKELETON, EntityType.STRAY),
        ARANHAS("Ninho de Aranhas", Material.COBWEB, 0x8E5B9E, EntityType.SPIDER, EntityType.CAVE_SPIDER),
        ILLAGERS("Acampamento de Saqueadores", Material.DARK_OAK_LOG, 0x6B5544, EntityType.PILLAGER, EntityType.VINDICATOR);

        final String nome;
        final Material nucleo;
        final TextColor cor;
        final EntityType comum, raro;

        Tipo(String nome, Material nucleo, int cor, EntityType comum, EntityType raro) {
            this.nome = nome;
            this.nucleo = nucleo;
            this.cor = TextColor.color(cor);
            this.comum = comum;
            this.raro = raro;
        }
    }

    private static final class Ninho {
        final UUID id = UUID.randomUUID();
        final Tipo tipo;
        final Location nucleo;
        final long expira;
        final Set<UUID> guardioes = new HashSet<>();
        UUID rotulo;
        int proximaOnda;

        Ninho(Tipo tipo, Location nucleo) {
            this.tipo = tipo;
            this.nucleo = nucleo;
            this.expira = System.currentTimeMillis() + DURACAO_MS;
        }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kGuardiao;
    private final List<Ninho> ninhos = new ArrayList<>();
    private int segundos;

    public Ninhos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kGuardiao = new NamespacedKey(plugin, "ninho_guardiao");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    /** Roda 1x por segundo. */
    public void tick() {
        segundos++;
        if (segundos % 600 == 0) tentarCriar();
        int agora = Bukkit.getCurrentTick();
        for (Ninho n : List.copyOf(ninhos)) {
            if (System.currentTimeMillis() > n.expira) {
                desfazer(n, true);
                continue;
            }
            if (!n.nucleo.isChunkLoaded()) continue;
            if (n.nucleo.getBlock().getType() != n.tipo.nucleo) {
                desfazer(n, false);
                continue;
            }
            n.guardioes.removeIf(id -> { Entity e = Bukkit.getEntity(id); return e == null || !e.isValid() || e.isDead(); });
            Location c = n.nucleo.clone().add(0.5, 0.5, 0.5);
            World w = c.getWorld();
            w.spawnParticle(Particle.DUST, c.clone().add(0, 0.8, 0), 4, 0.4, 0.4, 0.4, 0, new Particle.DustOptions(Color.fromRGB(n.tipo.cor.value()), 1.4f));
            Player perto = null;
            for (Player p : w.getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL && p.getLocation().distanceSquared(c) < 24 * 24) { perto = p; break; }
            }
            if (perto != null && agora >= n.proximaOnda && n.guardioes.size() < MAX_VIVOS) {
                onda(n, perto);
                n.proximaOnda = agora + 600;
            }
            atualizarRotulo(n);
        }
    }

    private void tentarCriar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (ninhos.size() >= plugin.settings().perNinhoMax) return;
            if (p.getGameMode() != GameMode.SURVIVAL || p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            if (plugin.territorios().em(p.getLocation()) != null || rnd().nextDouble() >= plugin.settings().perNinhoChance) continue;
            Location l = lugar(p);
            if (l != null) criar(l, null, p);
        }
    }

    private Location lugar(Player p) {
        World w = p.getWorld();
        for (int i = 0; i < 10; i++) {
            double a = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(35, 70);
            int x = (int) (p.getLocation().getX() + Math.cos(a) * d), z = (int) (p.getLocation().getZ() + Math.sin(a) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block chao = w.getHighestBlockAt(x, z);
            if (chao.isLiquid() || !chao.getType().isSolid()) continue;
            Block b = chao.getRelative(0, 1, 0);
            if (!b.isEmpty() || plugin.territorios().em(b.getLocation()) != null) continue;
            return b.getLocation();
        }
        return null;
    }

    /** Cria um ninho (tipo aleatório se null). */
    public boolean criar(Location onde, String tipoId, Player avisar) {
        Tipo t;
        if (tipoId != null) {
            try { t = Tipo.valueOf(tipoId.toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException e) { return false; }
        } else {
            Biome bioma = onde.getBlock().getBiome();
            String b = bioma.getKey().getKey();
            double x = rnd().nextDouble();
            t = b.contains("dark_forest") || x < 0.1 ? Tipo.ILLAGERS : x < 0.4 ? Tipo.ZUMBIS : x < 0.7 ? Tipo.ESQUELETOS : Tipo.ARANHAS;
        }
        Block b = onde.getBlock();
        if (!b.isEmpty() && b.getType() != Material.SHORT_GRASS && b.getType() != Material.TALL_GRASS) return false;
        b.setType(t.nucleo);
        Ninho n = new Ninho(t, b.getLocation());
        ninhos.add(n);
        Location c = b.getLocation().add(0.5, 1.6, 0.5);
        TextDisplay d = c.getWorld().spawn(c, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setShadowed(true);
        });
        n.rotulo = d.getUniqueId();
        atualizarRotulo(n);
        if (avisar != null) {
            avisar.sendMessage(Component.text("☠ Você sente um " + t.nome + " por perto, " + direcao(avisar.getLocation(), b.getLocation()) + "...", t.cor));
            avisar.playSound(avisar.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.8f);
        }
        return true;
    }

    private static String direcao(Location de, Location para) {
        double dx = para.getX() - de.getX(), dz = para.getZ() - de.getZ();
        double ang = Math.toDegrees(Math.atan2(-dx, dz));
        String[] nomes = {"ao sul", "a sudoeste", "a oeste", "a noroeste", "ao norte", "a nordeste", "a leste", "a sudeste"};
        int i = (int) Math.round(((ang % 360) + 360) % 360 / 45.0) % 8;
        return nomes[i] + " (~" + (int) Math.sqrt(dx * dx + dz * dz) + " blocos)";
    }

    private void atualizarRotulo(Ninho n) {
        if (n.rotulo == null || !(Bukkit.getEntity(n.rotulo) instanceof TextDisplay d)) return;
        d.text(Component.text("☠ " + n.tipo.nome, n.tipo.cor).append(Component.newline())
                .append(Component.text(n.guardioes.isEmpty() ? "Quebre o núcleo!" : "Guardiões: " + n.guardioes.size(),
                        n.guardioes.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
    }

    private void onda(Ninho n, Player alvo) {
        World w = n.nucleo.getWorld();
        int qtd = 3 + rnd().nextInt(2);
        for (int i = 0; i < qtd && n.guardioes.size() < MAX_VIVOS; i++) {
            Location l = n.nucleo.clone().add(rnd().nextDouble(-3, 4), 0, rnd().nextDouble(-3, 4));
            l.setY(w.getHighestBlockYAt(l) + 1);
            EntityType tipo = rnd().nextDouble() < 0.25 ? n.tipo.raro : n.tipo.comum;
            Entity e = w.spawnEntity(l, tipo, CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (!(e instanceof LivingEntity le)) continue;
            le.getPersistentDataContainer().set(kGuardiao, PersistentDataType.STRING, n.id.toString());
            le.setPersistent(false);
            plugin.perigo().envelhecer(le);
            if (rnd().nextDouble() < plugin.perigo().chanceElite() * 3) plugin.perigo().tornarElite(le, 1);
            if (le instanceof Zombie z) z.setShouldBurnInDay(false);
            if (le instanceof org.bukkit.entity.AbstractSkeleton s) s.setShouldBurnInDay(false);
            if (le instanceof org.bukkit.entity.Raider r) r.setCanJoinRaid(false);
            if (le instanceof Mob m) m.setTarget(alvo);
            n.guardioes.add(le.getUniqueId());
        }
        w.spawnParticle(Particle.LARGE_SMOKE, n.nucleo.clone().add(0.5, 1, 0.5), 30, 1, 0.5, 1, 0.02);
        w.playSound(n.nucleo, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 0.7f);
    }

    private Ninho no(Block b) {
        for (Ninho n : ninhos) if (n.nucleo.getBlock().equals(b)) return n;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Ninho n = no(e.getBlock());
        if (n == null) return;
        n.guardioes.removeIf(id -> { Entity x = Bukkit.getEntity(id); return x == null || !x.isValid() || x.isDead(); });
        if (!n.guardioes.isEmpty()) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("☠ Derrote os guardiões primeiro (" + n.guardioes.size() + ")", NamedTextColor.RED));
            return;
        }
        e.setDropItems(false);
        Location c = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack i : saque(n.tipo)) c.getWorld().dropItemNaturally(c, i);
        c.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, c, 60, 0.5, 0.8, 0.5, 0.3);
        c.getWorld().playSound(c, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        e.getPlayer().sendMessage(Component.text("☠ Você destruiu o " + n.tipo.nome + "!", n.tipo.cor));
        plugin.titulos().registrar(e.getPlayer(), "ninhos", 1);
        desfazer(n, false);
    }

    private static List<ItemStack> saque(Tipo t) {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Material.EMERALD, 4 + rnd().nextInt(6)));
        l.add(new ItemStack(switch (t) {
            case ZUMBIS -> Material.IRON_INGOT;
            case ESQUELETOS -> Material.BONE_BLOCK;
            case ARANHAS -> Material.STRING;
            case ILLAGERS -> Material.EMERALD;
        }, 6 + rnd().nextInt(10)));
        l.add(Reagente.PO_ARCANO.criar(2 + rnd().nextInt(3)));
        if (rnd().nextDouble() < 0.4) {
            Gema[] gs = Gema.values();
            l.add(gs[rnd().nextInt(gs.length)].criar(1, 1));
        }
        if (rnd().nextDouble() < 0.3) l.add(Raro.FRAGMENTO_DE_FORJA.criar(1 + rnd().nextInt(2)));
        if (rnd().nextDouble() < 0.1) l.add(Raro.MAPA_RASGADO.criar(1));
        if (rnd().nextDouble() < 0.15) l.add(new ItemStack(Material.DIAMOND, 1 + rnd().nextInt(2)));
        return l;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodir(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> no(b) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBloco(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> no(b) != null);
    }

    private void desfazer(Ninho n, boolean tirarBloco) {
        ninhos.remove(n);
        if (n.rotulo != null) {
            Entity d = Bukkit.getEntity(n.rotulo);
            if (d != null) d.remove();
        }
        if (tirarBloco && n.nucleo.isChunkLoaded() && n.nucleo.getBlock().getType() == n.tipo.nucleo) n.nucleo.getBlock().setType(Material.AIR);
    }

    /** Ao desligar: os ninhos somem (não ficam blocos soltos no mundo). */
    public void parar() {
        for (Ninho n : List.copyOf(ninhos)) desfazer(n, true);
    }

    public int quantos() { return ninhos.size(); }
}
