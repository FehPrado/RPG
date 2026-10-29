package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Bogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As estruturas grandes da 3.1 (Árvore-Mundo, Colosso Caído, Cidade Anã, Ossada do Dragão, Coliseu e
 * Castelo do Rei Caído). Quem constrói é o próprio jogo, pelo datapack, junto com o mundo (só em
 * terreno novo; aparecem no /locate). O plugin reconhece cada uma quando o chunk nasce e dá vida:
 * guardas, o chefe de cada uma, os títulos e o Campeão do Coliseu.
 * <p>
 * O jogo gira a estrutura ao acaso; por isso o lugar do chefe é sempre perto do meio da caixa dela
 * (o molde foi desenhado assim) e o plugin procura um chão livre por ali.
 */
final class Gigantes implements Listener {

    private final RPGAtributos plugin;
    private final Estruturas estruturas;
    private final Map<String, BossBar> barras = new HashMap<>();
    private final Map<String, UUID> chefes = new HashMap<>();

    Gigantes(RPGAtributos plugin, Estruturas estruturas) {
        this.plugin = plugin;
        this.estruturas = estruturas;
    }

    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }

    /** Quantos blocos o molde começa abaixo do chão (o start_height do datapack, com sinal trocado). */
    static int superficie(Estrutura t) {
        return switch (t) {
            case ARVORE_MUNDO -> 18;
            case COLOSSO_CAIDO -> 4;
            case CIDADE_ANA -> 36;
            case OSSADA_DRAGAO, COLISEU -> 1;
            default -> 0;
        };
    }

    /** O tamanho do molde (x, y, z), para o admin pôr um no lugar certo. */
    static int[] tamanho(Estrutura t) {
        return switch (t) {
            case ARVORE_MUNDO -> new int[]{61, 121, 61};
            case COLOSSO_CAIDO -> new int[]{71, 26, 41};
            case CIDADE_ANA -> new int[]{49, 50, 49};
            case OSSADA_DRAGAO -> new int[]{65, 26, 41};
            case COLISEU -> new int[]{61, 22, 49};
            default -> new int[]{45, 32, 45};
        };
    }

    /** O tipo do RPG que o jogo gerou (pelo id da estrutura do datapack), ou null. */
    static Estrutura tipoDe(GeneratedStructure g) {
        NamespacedKey k = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).getKey(g.getStructure());
        if (k == null || !k.getNamespace().equals("rpgatributos")) return null;
        Estrutura t = Estrutura.porId(k.getKey());
        return t != null && t.gigante() ? t : null;
    }

    /** Chunk novo: anota as estruturas grandes que passam por ele (uma vez cada, pela caixa). */
    void aoNascer(Chunk c) {
        for (GeneratedStructure g : c.getStructures()) {
            Estrutura t = tipoDe(g);
            if (t == null) continue;
            BoundingBox bb = g.getBoundingBox();
            int x = (int) Math.floor(bb.getCenterX()), z = (int) Math.floor(bb.getCenterZ()), y = (int) Math.floor(bb.getMinY()) + superficie(t);
            estruturas.anotarGigante(c.getWorld(), t, x, y, z);
        }
    }

    // =====================================================================
    //  Vida (a cada 2 s, para cada jogador perto)
    // =====================================================================

    void tick(Sitio s, World w, Player p, boolean jogando, double d2, double dy) {
        mostrarBarra(s, w, p, d2);
        if (!jogando) return;
        // Altura do jogador contada do fundo do molde (a câmara das raízes e o salão anão ficam lá embaixo).
        double db = dy + superficie(s.tipo);
        switch (s.tipo) {
            case ARVORE_MUNDO -> {
                // A câmara das raízes fica uns 18 blocos abaixo do chão, no meio.
                if (!s.guardas && d2 <= 13 * 13 && db >= -1 && db <= 14) soltar(s, w, p);
            }
            case COLOSSO_CAIDO -> {
                if (!s.guardas && d2 <= 11 * 11 && db <= 16) soltar(s, w, p);
                reTarget(s, p, d2);
            }
            case CIDADE_ANA -> {
                if (!s.guardas && d2 <= 14 * 14 && db >= -1 && db <= 18) soltar(s, w, p);
            }
            case OSSADA_DRAGAO -> {
                long agora = System.currentTimeMillis();
                boolean noite = w.getTime() >= 13000 && w.getTime() <= 23000;
                if (noite && d2 <= 26 * 26 && agora - s.ronda > 15 * 60_000L && !vivo(s)) {
                    s.ronda = agora;
                    estruturas.sujar();
                    soltar(s, w, p);
                }
            }
            case CASTELO_REI_CAIDO -> {
                if (!s.guardas && d2 <= 22 * 22 && Math.abs(dy) <= 12) soltarCavaleiros(s, w);
                if (s.chefeEm == 0 && d2 <= 7 * 7 && db >= -1 && db <= 9) {
                    s.chefeEm = 1;
                    estruturas.sujar();
                    soltar(s, w, p);
                }
            }
            default -> { }
        }
    }

    private boolean vivo(Sitio s) {
        UUID u = chefes.get(s.id);
        Entity e = u == null ? null : Bukkit.getEntity(u);
        return e != null && e.isValid();
    }

    /** O Sentinela do Colosso é um golem: de tempos em tempos ele volta a mirar em quem está perto. */
    private void reTarget(Sitio s, Player p, double d2) {
        UUID u = chefes.get(s.id);
        if (u == null || d2 > 24 * 24) return;
        if (Bukkit.getEntity(u) instanceof IronGolem g && g.isValid() && (g.getTarget() == null || !g.getTarget().isValid())) g.setTarget(p);
    }

    /** Um chão livre perto do meio da estrutura, na faixa de altura dada (relativa ao fundo dela). */
    private static Location chaoLivre(World w, Sitio s, int dy0, int dy1, int raio) {
        Location melhor = null;
        double best = Double.MAX_VALUE;
        for (int dx = -raio; dx <= raio; dx++)
            for (int dz = -raio; dz <= raio; dz++)
                for (int dy = dy0; dy <= dy1; dy++) {
                    Block b = w.getBlockAt(s.x + dx, s.y - superficie(s.tipo) + dy, s.z + dz);
                    if (!b.getType().isAir() || !b.getRelative(0, 1, 0).getType().isAir() || !b.getRelative(0, 2, 0).getType().isAir()) continue;
                    if (!b.getRelative(0, -1, 0).getType().isSolid()) continue;
                    double d = dx * dx + dz * dz + dy * 0.1;
                    if (d < best) {
                        best = d;
                        melhor = b.getLocation().add(0.5, 0, 0.5);
                    }
                }
        return melhor;
    }

    private static void vida(LivingEntity e, double max) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        if (a != null) a.setBaseValue(max);
        e.setHealth(max);
    }

    private static void escala(LivingEntity e, double v) {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a != null) a.setBaseValue(v);
    }

    private static void equipar(LivingEntity e, Material mao, Material cabeca) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null) return;
        if (mao != null) eq.setItemInMainHand(new ItemStack(mao));
        if (cabeca != null) eq.setHelmet(new ItemStack(cabeca));
        eq.setItemInMainHandDropChance(0);
        eq.setHelmetDropChance(0);
    }

    private <T extends Mob> T chefe(Sitio s, Location l, Class<T> c, String nome, String papel, double hp, double esc, java.util.function.Consumer<T> extra) {
        T m = estruturas.guarda(s, l, c, nome, NamedTextColor.GOLD, x -> {
            x.setRemoveWhenFarAway(false);
            x.getPersistentDataContainer().set(Estruturas.K_GUARDA, PersistentDataType.STRING, s.id + ":" + papel);
            vida(x, hp);
            escala(x, esc);
            extra.accept(x);
        });
        chefes.put(s.id, m.getUniqueId());
        return m;
    }

    private void soltar(Sitio s, World w, Player p) {
        s.guardas = true;
        estruturas.sujar();
        switch (s.tipo) {
            case ARVORE_MUNDO -> {
                Location l = chaoLivre(w, s, 1, 6, 6);
                if (l == null) return;
                Ravager r = chefe(s, l, Ravager.class, "Guardião das Raízes", "raizes", 260, 1.25, x -> { });
                r.setTarget(p);
                for (int i = 0; i < 4; i++) {
                    Location g = chaoLivre(w, s, 1, 6, 9);
                    if (g == null) break;
                    estruturas.guarda(s, g.add(rnd().nextInt(-3, 4), 0, rnd().nextInt(-3, 4)), Bogged.class, "Raiz Viva", NamedTextColor.DARK_GREEN, x -> x.setTarget(p));
                }
                aviso(w, l, "As raízes se mexem... o Guardião acordou!", NamedTextColor.GREEN, Sound.ENTITY_RAVAGER_ROAR);
            }
            case COLOSSO_CAIDO -> {
                Location l = chaoLivre(w, s, 3, 10, 6);
                if (l == null) return;
                IronGolem g = chefe(s, l, IronGolem.class, "Sentinela do Colosso", "colosso", 300, 1.7, x -> x.setPlayerCreated(false));
                g.setTarget(p);
                aviso(w, l, "O Coração do Colosso pulsa. A Sentinela se levanta!", NamedTextColor.GRAY, Sound.ENTITY_IRON_GOLEM_REPAIR);
            }
            case CIDADE_ANA -> {
                Location l = chaoLivre(w, s, 1, 4, 8);
                if (l == null) return;
                Vindicator rei = chefe(s, l, Vindicator.class, "Rei da Montanha", "reianao", 280, 1.35, x -> equipar(x, Material.DIAMOND_AXE, Material.GOLDEN_HELMET));
                rei.setTarget(p);
                for (int i = 0; i < 4; i++) {
                    Location g = chaoLivre(w, s, 1, 4, 11);
                    if (g == null) break;
                    estruturas.guarda(s, g.add(rnd().nextInt(-4, 5), 0, rnd().nextInt(-4, 5)), Vindicator.class, "Guarda Anão", NamedTextColor.GOLD, x -> {
                        escala(x, 0.8);
                        equipar(x, Material.IRON_AXE, Material.IRON_HELMET);
                        x.setTarget(p);
                    });
                }
                aviso(w, l, "\"Quem ousa pisar no salão do rei?\"", NamedTextColor.GOLD, Sound.ENTITY_VINDICATOR_CELEBRATE);
            }
            case OSSADA_DRAGAO -> {
                Location l = new Location(w, s.x + 0.5, s.y + 16, s.z + 0.5);
                Phantom f = chefe(s, l, Phantom.class, "Espírito do Dragão", "dragao", 220, 1, x -> {
                    x.setSize(10);
                    x.setShouldBurnInDay(false);
                });
                f.setTarget(p);
                for (int i = 0; i < 3; i++) {
                    Location g = chaoLivre(w, s, 1, 6, 14);
                    if (g == null) break;
                    estruturas.guarda(s, g.add(rnd().nextInt(-6, 7), 0, rnd().nextInt(-6, 7)), Skeleton.class, "Osso Errante", NamedTextColor.WHITE, x -> x.setTarget(p));
                }
                aviso(w, l, "Os ossos estalam. O Espírito do Dragão voltou!", NamedTextColor.LIGHT_PURPLE, Sound.ENTITY_ENDER_DRAGON_GROWL);
            }
            case CASTELO_REI_CAIDO -> {
                Location l = chaoLivre(w, s, 1, 4, 7);
                if (l == null) return;
                WitherSkeleton rei = chefe(s, l, WitherSkeleton.class, "Rei Caído", "reicaido", 320, 1.5, x -> equipar(x, Material.NETHERITE_SWORD, Material.GOLDEN_HELMET));
                rei.setTarget(p);
                aviso(w, l, "\"Ajoelhe-se diante do seu rei.\"", NamedTextColor.DARK_PURPLE, Sound.ENTITY_WITHER_SPAWN);
            }
            default -> { }
        }
    }

    /** Os cavaleiros do castelo: aparecem uma vez, no pátio, quando alguém chega. */
    private void soltarCavaleiros(Sitio s, World w) {
        s.guardas = true;
        estruturas.sujar();
        for (int i = 0; i < 5; i++) {
            Location g = chaoLivre(w, s, 1, 3, 18);
            if (g == null) break;
            g.add(rnd().nextInt(-12, 13), 0, rnd().nextInt(-12, 13));
            if (!g.getBlock().getType().isAir()) continue;
            estruturas.guarda(s, g, Skeleton.class, "Cavaleiro Esquecido", NamedTextColor.GRAY, x -> {
                EntityEquipment eq = x.getEquipment();
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                eq.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                eq.setHelmetDropChance(0);
                eq.setChestplateDropChance(0);
                eq.setItemInMainHandDropChance(0);
            });
        }
    }

    private static void aviso(World w, Location l, String texto, NamedTextColor cor, Sound som) {
        for (Player o : w.getPlayers()) {
            if (o.getLocation().distanceSquared(l) > 48 * 48) continue;
            o.sendMessage(Component.text("⚔ " + texto, cor));
            o.playSound(l, som, 1.5f, 0.8f);
        }
        w.spawnParticle(Particle.EXPLOSION, l.clone().add(0, 1, 0), 3, 0.6, 0.6, 0.6, 0);
    }

    private void mostrarBarra(Sitio s, World w, Player p, double d2) {
        UUID u = chefes.get(s.id);
        Entity e = u == null ? null : Bukkit.getEntity(u);
        BossBar barra = barras.get(s.id);
        if (!(e instanceof LivingEntity v) || !v.isValid()) {
            if (barra != null) p.hideBossBar(barra);
            return;
        }
        if (barra == null) {
            barra = BossBar.bossBar(v.customName() == null ? Component.text(s.tipo.nome()) : v.customName(), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
            barras.put(s.id, barra);
        }
        AttributeInstance a = v.getAttribute(Attribute.MAX_HEALTH);
        barra.progress((float) Math.max(0, Math.min(1, v.getHealth() / (a == null ? 20 : a.getValue()))));
        if (d2 <= 48 * 48) p.showBossBar(barra);
        else p.hideBossBar(barra);
    }

    /** No meio da luta, o Rei Caído chama os cavaleiros dele (uma vez). */
    @EventHandler(ignoreCancelled = true)
    public void aoFerir(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof WitherSkeleton rei)) return;
        String v = rei.getPersistentDataContainer().get(Estruturas.K_GUARDA, PersistentDataType.STRING);
        if (v == null || !v.endsWith(":reicaido") || rei.getPersistentDataContainer().has(K_CHAMOU)) return;
        AttributeInstance a = rei.getAttribute(Attribute.MAX_HEALTH);
        if (a == null || rei.getHealth() - e.getFinalDamage() > a.getValue() / 2) return;
        rei.getPersistentDataContainer().set(K_CHAMOU, PersistentDataType.BYTE, (byte) 1);
        Sitio s = estruturas.sitio(v.substring(0, v.indexOf(':')));
        if (s == null) return;
        World w = rei.getWorld();
        for (int i = 0; i < 3; i++) {
            Location l = rei.getLocation().add(rnd().nextInt(-3, 4), 0, rnd().nextInt(-3, 4));
            if (!l.getBlock().getType().isAir()) l = rei.getLocation();
            estruturas.guarda(s, l, Skeleton.class, "Cavaleiro Esquecido", NamedTextColor.GRAY, x -> {
                x.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                x.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                x.getEquipment().setHelmetDropChance(0);
                x.getEquipment().setItemInMainHandDropChance(0);
            });
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, l.clone().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        }
        for (Player o : w.getPlayers()) if (o.getLocation().distanceSquared(rei.getLocation()) < 40 * 40) o.sendMessage(Component.text("⚔ \"Cavaleiros! Ao seu rei!\"", NamedTextColor.DARK_PURPLE));
    }

    private static final NamespacedKey K_CHAMOU = new NamespacedKey("rpgatributos", "rei_chamou");

    /** Os chefes das estruturas grandes: saque, título e diário. */
    boolean morreu(EntityDeathEvent e, String id, String papel) {
        Player quem = e.getEntity().getKiller();
        ThreadLocalRandom r = rnd();
        String contador, marco;
        switch (papel) {
            case "raizes" -> {
                e.getDrops().add(new ItemStack(Material.EMERALD, 6 + r.nextInt(8)));
                e.getDrops().add(new ItemStack(Material.GOLDEN_APPLE, 1 + r.nextInt(2)));
                if (r.nextDouble() < 0.35) e.getDrops().add(Raro.PAGINA_DE_LENDA.criar(1));
                contador = "guardiao_raizes";
                marco = "Venceu o Guardião das Raízes da Árvore-Mundo";
            }
            case "colosso" -> {
                e.getDrops().add(new ItemStack(Material.IRON_BLOCK, 2 + r.nextInt(3)));
                e.getDrops().add(new ItemStack(Material.DIAMOND, 2 + r.nextInt(3)));
                if (r.nextDouble() < 0.3) e.getDrops().add(Raro.FRAGMENTO_DE_FORJA.criar(1));
                contador = "sentinela_colosso";
                marco = "Derrubou a Sentinela do Colosso Caído";
            }
            case "reianao" -> {
                e.getDrops().add(new ItemStack(Material.GOLD_BLOCK, 2 + r.nextInt(3)));
                e.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, 1));
                if (r.nextDouble() < 0.4) e.getDrops().add(Raro.FRAGMENTO_DE_FORJA.criar(1 + r.nextInt(2)));
                contador = "rei_montanha";
                marco = "Derrotou o Rei da Montanha na Cidade Anã";
            }
            case "dragao" -> {
                e.getDrops().add(new ItemStack(Material.DRAGON_BREATH, 2 + r.nextInt(3)));
                e.getDrops().add(new ItemStack(Material.PHANTOM_MEMBRANE, 4 + r.nextInt(4)));
                if (r.nextDouble() < 0.12) e.getDrops().add(new ItemStack(Material.DRAGON_HEAD));
                if (r.nextDouble() < 0.3) e.getDrops().add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
                contador = "espirito_dragao";
                marco = "Devolveu o Espírito do Dragão ao descanso";
            }
            case "reicaido" -> {
                e.getDrops().add(new ItemStack(Material.GOLD_INGOT, 6 + r.nextInt(8)));
                e.getDrops().add(new ItemStack(Material.EXPERIENCE_BOTTLE, 4 + r.nextInt(5)));
                if (r.nextDouble() < 0.4) e.getDrops().add(Raro.PAGINA_DE_LENDA.criar(1));
                ItemStack coroa = new ItemStack(Material.GOLDEN_HELMET);
                coroa.editMeta(m -> m.customName(Component.text("Coroa do Rei Caído", NamedTextColor.GOLD)));
                if (r.nextDouble() < 0.5) e.getDrops().add(coroa);
                contador = "rei_caido";
                marco = "Tirou o Rei Caído do trono";
            }
            default -> {
                return false;
            }
        }
        e.setDroppedExp(e.getDroppedExp() + 120);
        BossBar barra = barras.remove(id);
        if (barra != null) for (Player o : Bukkit.getOnlinePlayers()) o.hideBossBar(barra);
        chefes.remove(id);
        if (quem != null) {
            plugin.diario().marco(quem, contador, marco);
            plugin.titulos().registrar(quem, contador, 1);
        }
        return true;
    }

    /** Coliseu: abrir o cofre do camarote com a chave conta como vencer a arena. */
    @EventHandler(ignoreCancelled = false)
    public void aoAbrirCofre(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.VAULT) return;
        ItemStack mao = e.getItem();
        if (mao == null || mao.getType() != Material.TRIAL_KEY) return;
        Block b = e.getClickedBlock();
        Sitio s = estruturas.perto(b.getWorld(), b.getX(), b.getZ(), 40);
        if (s == null || s.tipo != Estrutura.COLISEU) return;
        Player p = e.getPlayer();
        if (br.rpgatributos.ModoTeste.ehTeste(p)) return;
        plugin.diario().marco(p, "campeao_coliseu", "Venceu o Coliseu e abriu o cofre do imperador");
        plugin.titulos().registrar(p, "campeao_coliseu", 1);
        p.sendMessage(Component.text("🏆 A plateia de pedra aplaude: você é o Campeão do Coliseu!", NamedTextColor.GOLD));
    }
}
