package br.rpgatributos.dimensao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.visual.Telas;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.7: o Mundo das Almas (parte 1: o mundo e as regras). Onde os Antigos foram parar.
 * <ul>
 *   <li><b>O ritual:</b> Lanterna das Almas na mão esquerda, Punhal do Sacrifício na direita; agachado,
 *       clique direito e confirme. O corpo fica deitado onde você estava (ninguém mexe nele) e a alma
 *       vai para o Mundo das Almas <b>sem nada</b>, só com a lanterna.</li>
 *   <li><b>A chama:</b> a lanterna queima por 15 minutos (barra no alto da tela). Lá a alma corre e pula
 *       mais, não sente fome e não constrói nem quebra blocos.</li>
 *   <li><b>A volta:</b> agachado, clique direito com a lanterna e a alma volta ao corpo trazendo o que
 *       achou. Se a chama apagar ou a alma morrer lá, ela volta do mesmo jeito, mas sem os achados.</li>
 *   <li>Tudo do corpo (inventário, XP, vida, efeitos) fica guardado num arquivo: cair o servidor, sair do
 *       jogo ou ser levado para fora de outro jeito devolve tudo. Ninguém fica preso.</li>
 * </ul>
 */
public final class Almas implements Listener {

    public static final TextColor COR = TextColor.color(0x7FD8E8);
    private static final NamespacedKey K_LANTERNA = new NamespacedKey("rpgatributos", "lanterna_das_almas");
    private static final NamespacedKey K_PUNHAL = new NamespacedKey("rpgatributos", "punhal_do_sacrificio");
    private static final NamespacedKey K_CORPO = new NamespacedKey("rpgatributos", "corpo_da_alma");
    private static final NamespacedKey K_VELOZ = new NamespacedKey("rpgatributos", "alma_veloz");
    private static final NamespacedKey K_PULO = new NamespacedKey("rpgatributos", "alma_pulo");

    private final RPGAtributos plugin;
    private final File pasta;
    /** Segundos de chama que faltam, por alma (salvo no arquivo a cada 15 s e ao sair). */
    private final Map<UUID, Integer> chama = new HashMap<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    /** Durante a volta, a troca de mundo não dispara outra volta. */
    private boolean voltando;

    public Almas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.pasta = new File(plugin.getDataFolder(), "almas");
    }

    private int minutos() {
        return Math.max(1, plugin.getConfig().getInt("almas.minutos-de-chama", 15));
    }

    // =====================================================================
    //  Os itens
    // =====================================================================

    public static ItemStack lanterna() {
        ItemStack i = new ItemStack(Material.SOUL_LANTERN);
        i.editMeta(m -> {
            m.itemName(Component.text("Lanterna das Almas", COR, TextDecoration.BOLD));
            m.lore(List.of(Component.text("Na mão esquerda, com o Punhal do Sacrifício na direita:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("agache e clique para deixar o corpo.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Lá, a chama dela é o seu tempo. Clique de novo para voltar.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(K_LANTERNA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "lanterna_das_almas");
        });
        return i;
    }

    public static ItemStack punhal() {
        ItemStack i = new ItemStack(Material.IRON_SWORD);
        i.editMeta(m -> {
            m.itemName(Component.text("Punhal do Sacrifício", TextColor.color(0x9FA8DA), TextDecoration.BOLD));
            m.lore(List.of(Component.text("Lâmina de Quartzo Negro, do Vale das Almas.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Com a Lanterna das Almas na outra mão, abre o caminho.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_PUNHAL, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "punhal_do_sacrificio");
        });
        return i;
    }

    public static boolean ehLanterna(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_LANTERNA);
    }

    public static boolean ehPunhal(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_PUNHAL);
    }

    public void registrarReceitas() {
        ShapedRecipe l = new ShapedRecipe(new NamespacedKey(plugin, "lanterna_das_almas"), lanterna());
        l.shape(" A ", "ALA", " A ");
        l.setIngredient('A', Material.SOUL_SAND);
        l.setIngredient('L', Material.SOUL_LANTERN);
        if (Bukkit.getRecipe(l.getKey()) == null) Bukkit.addRecipe(l);
        ShapedRecipe p = new ShapedRecipe(new NamespacedKey(plugin, "punhal_do_sacrificio"), punhal());
        p.shape("Q", "F", "S");
        p.setIngredient('Q', new RecipeChoice.ExactChoice(Raro.QUARTZO_NEGRO.criar(1)));
        p.setIngredient('F', Material.IRON_INGOT);
        p.setIngredient('S', Material.STICK);
        if (Bukkit.getRecipe(p.getKey()) == null) Bukkit.addRecipe(p);
    }

    // =====================================================================
    //  O arquivo do corpo
    // =====================================================================

    private File arquivo(UUID u) {
        return new File(pasta, u + ".yml");
    }

    /** Esta pessoa está fora do corpo (tem um corpo guardado)? */
    public boolean ehAlma(Player p) {
        return arquivo(p.getUniqueId()).exists();
    }

    private World mundo() {
        return plugin.dimensoes().mundo(Dimensoes.Dimensao.ALMAS);
    }

    private boolean naDimensao(Player p) {
        return Dimensoes.de(p.getWorld()) == Dimensoes.Dimensao.ALMAS;
    }

    // =====================================================================
    //  O ritual
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand(), esquerda = p.getInventory().getItemInOffHand();
        // Lá dentro: agachado, clicar com a lanterna (em qualquer mão) leva de volta. Sem agachar, os cliques
        // valem como sempre (baús, atris, o sino); só no ar a lanterna lembra como voltar.
        if (naDimensao(p) && ehAlma(p) && ehLanterna(e.getItem())) {
            if (!p.isSneaking()) {
                if (e.getAction() == Action.RIGHT_CLICK_AIR) p.sendActionBar(Component.text("Agache e clique com a lanterna para voltar ao corpo.", COR));
                return;
            }
            e.setCancelled(true);
            Telas.confirmar(p, Component.text("🏮 Voltar ao corpo", COR, TextDecoration.BOLD),
                    List.of(Component.text("A chama leva você de volta ao corpo, com o que achou aqui.", NamedTextColor.WHITE)),
                    "Voltar", "Ficar mais", q -> {
                        if (naDimensao(q) && ehAlma(q) && !q.isDead()) voltar(q, true, null);
                    });
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND || !ehPunhal(mao) || !ehLanterna(esquerda) || !p.isSneaking()) return;
        e.setCancelled(true);
        String erro = podeSacrificar(p);
        if (erro != null) {
            p.sendActionBar(Component.text(erro, NamedTextColor.RED));
            return;
        }
        Telas.confirmar(p, Component.text("🗡 O Sacrifício", COR, TextDecoration.BOLD), List.of(
                        Component.text("Sua alma deixa o corpo e vai para o Mundo das Almas.", NamedTextColor.WHITE),
                        Component.text("O corpo fica aqui, deitado, com tudo o que você carrega. Ninguém mexe nele.", NamedTextColor.GRAY),
                        Component.text("Lá você chega sem nada, só com a lanterna. A chama dura " + minutos() + " minutos.", NamedTextColor.GRAY),
                        Component.text("Volte pela lanterna e traga o que achar. Se a chama apagar ou você morrer lá, volta sem nada.", NamedTextColor.GRAY)),
                "Sacrificar-se", "Ainda não", q -> {
                    String erro2 = podeSacrificar(q);
                    if (erro2 != null) q.sendActionBar(Component.text(erro2, NamedTextColor.RED));
                    else if (!ehPunhal(q.getInventory().getItemInMainHand()) || !ehLanterna(q.getInventory().getItemInOffHand()))
                        q.sendActionBar(Component.text("Segure o punhal na direita e a lanterna na esquerda.", NamedTextColor.RED));
                    else sacrificar(q);
                });
    }

    /** A lanterna não vira bloco no chão. */
    @EventHandler(ignoreCancelled = true)
    public void aoColocar(BlockPlaceEvent e) {
        if (ehLanterna(e.getItemInHand())) e.setCancelled(true);
    }

    public String podeSacrificar(Player p) {
        if (p.isDead()) return "Os mortos não se sacrificam.";
        if (ehAlma(p)) return "Você já está fora do corpo.";
        if (Dimensoes.de(p.getWorld()) != null || plugin.masmorras().ehMundo(p.getWorld())) return "Aqui o caminho não abre. Faça o ritual no mundo de cima.";
        if (!plugin.dimensoes().ativa(Dimensoes.Dimensao.ALMAS)) return "O Mundo das Almas está fechado neste servidor.";
        if (mundo() == null) return "O Mundo das Almas ainda não existe: o servidor precisa reiniciar uma vez.";
        if (p.isInsideVehicle() || p.isGliding()) return "Fique de pé, parado, para o ritual.";
        return null;
    }

    /** O sacrifício: guarda o corpo, deita o boneco no lugar e leva a alma. */
    public void sacrificar(Player p) {
        World w = mundo();
        if (w == null) return;
        // 3.8: a alma chega num cais do Barqueiro (o mais perto, ou um novo ali).
        Location chegada = plugin.mundoDasAlmas() != null ? plugin.mundoDasAlmas().chegadaNoCais(chegada(w)) : chegada(w);
        Location corpo = p.getLocation().clone();
        // A lanterna vai com a alma; o resto fica com o corpo.
        ItemStack lanterna = p.getInventory().getItemInOffHand().clone();
        if (!ehLanterna(lanterna)) lanterna = lanterna();
        p.getInventory().setItemInOffHand(null);
        try {
            guardar(p, corpo);
        } catch (IOException ex) {
            p.getInventory().setItemInOffHand(lanterna);
            p.sendMessage(Component.text("✖ O ritual falhou (não deu para guardar o corpo): " + ex.getMessage(), NamedTextColor.RED));
            return;
        }
        UUID boneco = deitar(p, corpo);
        if (boneco != null) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo(p.getUniqueId()));
            y.set("boneco", boneco.toString());
            try { y.save(arquivo(p.getUniqueId())); } catch (IOException ignorado) { }
        }
        corpo.getWorld().spawnParticle(Particle.SOUL, corpo.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.03);
        corpo.getWorld().playSound(corpo, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.6f);
        corpo.getWorld().playSound(corpo, Sound.ENTITY_PLAYER_HURT, 1f, 0.6f);
        // A alma: sem nada, só a lanterna.
        p.getInventory().clear();
        p.getInventory().setItemInOffHand(lanterna);
        for (PotionEffect ef : p.getActivePotionEffects()) p.removePotionEffect(ef.getType());
        p.setFireTicks(0);
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        chama.put(p.getUniqueId(), minutos() * 60);
        voltando = true;
        try {
            p.setFallDistance(0);
            p.teleport(chegada);
        } finally {
            voltando = false;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false, false));
        p.playSound(chegada, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1f, 0.8f);
        Dimensoes.Dimensao d = Dimensoes.Dimensao.ALMAS;
        p.showTitle(Title.title(Component.text(d.nome(), d.cor(), TextDecoration.BOLD), Component.text(d.frase(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(800), Duration.ofMillis(3500), Duration.ofMillis(1200))));
        p.sendMessage(Component.text("🏮 A chama da lanterna dura " + minutos() + " minutos. Agache e clique com ela para voltar ao corpo, com o que achar.", COR));
        plugin.diario().marco(p, "dimensao_almas", "Deixou o corpo e entrou no Mundo das Almas");
        plugin.titulos().registrar(p, "viagens_almas", 1);
        plugin.titulos().registrar(p, "dimensoes", 1);
    }

    /** Um lugar firme numa ilha (acima do Mar de Névoa); sem nenhum, uma plataforma de tufo no ar. */
    private Location chegada(World w) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(-1500, 1501), z = r.nextInt(-1500, 1501);
            Block b = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (b.getY() < 70 || !b.getType().isSolid()) continue;
            boolean firme = true;
            for (int dx = -2; dx <= 2 && firme; dx++) for (int dz = -2; dz <= 2 && firme; dz++)
                if (Math.abs(w.getHighestBlockYAt(x + dx, z + dz, HeightMap.MOTION_BLOCKING_NO_LEAVES) - b.getY()) > 1) firme = false;
            if (firme) return b.getLocation().add(0.5, 1, 0.5);
        }
        int x = r.nextInt(-800, 801), z = r.nextInt(-800, 801);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) w.getBlockAt(x + dx, 110, z + dz).setType(Material.POLISHED_TUFF);
        return new Location(w, x + 0.5, 111, z + 0.5);
    }

    /** O corpo: um boneco deitado com a pele do jogador, que não se mexe e não apanha. */
    private UUID deitar(Player p, Location onde) {
        try {
            Mannequin m = onde.getWorld().spawn(onde, Mannequin.class, x -> {
                x.setProfile(ResolvableProfile.resolvableProfile(p.getPlayerProfile()));
                x.setImmovable(true);
                x.setInvulnerable(true);
                x.setPersistent(true);
                x.setSilent(true);
                x.setGravity(true);
                x.setDescription(Component.text("Corpo de " + p.getName(), COR));
                x.customName(Component.text("💤 " + p.getName(), NamedTextColor.GRAY));
                x.setCustomNameVisible(false);
                x.getPersistentDataContainer().set(K_CORPO, PersistentDataType.STRING, p.getUniqueId().toString());
                if (Mannequin.validPoses().contains(Pose.SLEEPING)) x.setPose(Pose.SLEEPING, true);
            });
            return m.getUniqueId();
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Almas: não deu para deitar o corpo de " + p.getName() + ": " + ex.getMessage());
            return null;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoMexerNoCorpo(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked().getPersistentDataContainer().has(K_CORPO)) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("Um corpo sem alma. Ela vai voltar.", NamedTextColor.GRAY));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFerirCorpo(EntityDamageEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(K_CORPO)) e.setCancelled(true);
    }

    /** Corpo sem dono fora do corpo (sobrou de um servidor que caiu, por exemplo): some. */
    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            String dono = en.getPersistentDataContainer().get(K_CORPO, PersistentDataType.STRING);
            if (dono == null) continue;
            try {
                if (!arquivo(UUID.fromString(dono)).exists()) en.remove();
            } catch (IllegalArgumentException ex) {
                en.remove();
            }
        }
    }

    // =====================================================================
    //  Guardar e devolver o corpo
    // =====================================================================

    private void guardar(Player p, Location corpo) throws IOException {
        pasta.mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        y.set("corpo", corpo);
        y.set("inventario", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(p.getInventory().getContents())));
        y.set("nivel", p.getLevel());
        y.set("exp", (double) p.getExp());
        y.set("vida", p.getHealth());
        y.set("fome", p.getFoodLevel());
        y.set("saturacao", (double) p.getSaturation());
        y.set("efeitos", List.copyOf(p.getActivePotionEffects()));
        y.set("chama", minutos() * 60);
        y.save(arquivo(p.getUniqueId()));
    }

    private void salvarChama(Player p) {
        Integer c = chama.get(p.getUniqueId());
        File f = arquivo(p.getUniqueId());
        if (c == null || !f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        y.set("chama", c);
        try { y.save(f); } catch (IOException ignorado) { }
    }

    /**
     * A alma volta ao corpo. {@code comAchados}: traz o que pegou lá dentro (pela lanterna); senão
     * (chama apagada, morte) só o corpo. {@code onde}: null leva até o corpo; senão fica onde está.
     */
    public void voltar(Player p, boolean comAchados, Location onde) {
        File f = arquivo(p.getUniqueId());
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        List<ItemStack> achados = new ArrayList<>();
        ItemStack lanterna = null;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.isEmpty()) continue;
            if (ehLanterna(i) && lanterna == null) lanterna = i;
            else if (comAchados) achados.add(i);
        }
        if (lanterna == null) lanterna = lanterna();
        Location corpo = y.getLocation("corpo");
        if (corpo == null || corpo.getWorld() == null) corpo = Bukkit.getWorlds().getFirst().getSpawnLocation();
        tirarBoneco(p.getUniqueId(), y, corpo);
        p.getInventory().setContents(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(y.getString("inventario", ""))));
        for (PotionEffect ef : p.getActivePotionEffects()) p.removePotionEffect(ef.getType());
        for (Object o : y.getList("efeitos", List.of())) if (o instanceof PotionEffect ef) p.addPotionEffect(ef);
        p.setLevel(y.getInt("nivel", p.getLevel()));
        p.setExp((float) y.getDouble("exp", p.getExp()));
        p.setFoodLevel(y.getInt("fome", 20));
        p.setSaturation((float) y.getDouble("saturacao", 5));
        f.delete();
        chama.remove(p.getUniqueId());
        BossBar b = barras.remove(p.getUniqueId());
        if (b != null) p.hideBossBar(b);
        semAlma(p);
        Location destino = onde != null ? onde : corpo;
        if (onde == null) {
            voltando = true;
            try {
                p.setFallDistance(0);
                p.teleport(destino);
            } finally {
                voltando = false;
            }
        }
        double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
        p.setHealth(Math.max(1, Math.min(max, y.getDouble("vida", max))));
        // A lanterna e os achados vêm por cima (o que não couber cai no chão).
        List<ItemStack> extra = new ArrayList<>(achados);
        extra.add(lanterna);
        for (ItemStack i : extra) p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.updateInventory();
        p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, false, false, false));
        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.03);
        p.playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 1.4f);
        p.showTitle(Title.title(Component.text("De volta ao corpo", COR, TextDecoration.BOLD),
                Component.text(comAchados ? (achados.isEmpty() ? "A lanterna trouxe você." : "Com " + achados.size() + " achado(s) do Mundo das Almas.") : "O que estava com a alma ficou lá.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2500), Duration.ofMillis(800))));
    }

    private void tirarBoneco(UUID dono, YamlConfiguration y, Location corpo) {
        String id = y.getString("boneco");
        corpo.getChunk().load();
        if (id != null) {
            try {
                Entity e = Bukkit.getEntity(UUID.fromString(id));
                if (e != null) e.remove();
            } catch (IllegalArgumentException ignorado) { }
        }
        // Por garantia: qualquer corpo deste jogador perto do lugar. (Se as entidades do chunk ainda não
        // carregaram, o corpo some quando carregarem: aoCarregarEntidades vê que o arquivo já foi apagado.)
        for (Entity e : corpo.getWorld().getNearbyEntities(corpo, 3, 3, 3))
            if (e.getPersistentDataContainer().has(K_CORPO)) {
                String de = e.getPersistentDataContainer().get(K_CORPO, PersistentDataType.STRING);
                if (dono.toString().equals(de)) e.remove();
            }
    }

    /** Admin/área de testes: esquece a alma sem devolver nada (o corpo some; o teste devolve o resto). */
    public void esquecer(Player p) {
        File f = arquivo(p.getUniqueId());
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        Location corpo = y.getLocation("corpo");
        if (corpo != null && corpo.getWorld() != null) tirarBoneco(p.getUniqueId(), y, corpo);
        f.delete();
        chama.remove(p.getUniqueId());
        BossBar b = barras.remove(p.getUniqueId());
        if (b != null) p.hideBossBar(b);
        semAlma(p);
    }

    // =====================================================================
    //  As regras lá dentro
    // =====================================================================

    public void segundo() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean dentro = naDimensao(p);
            if (!dentro) {
                if (p.getAttribute(Attribute.MOVEMENT_SPEED) != null && p.getAttribute(Attribute.MOVEMENT_SPEED).getModifier(K_VELOZ) != null) semAlma(p);
                continue;
            }
            // A alma: mais rápida, pula mais, sem fome.
            modificador(p, Attribute.MOVEMENT_SPEED, K_VELOZ, 0.3);
            modificador(p, Attribute.JUMP_STRENGTH, K_PULO, 0.25);
            if (p.getFoodLevel() < 20) p.setFoodLevel(20);
            if (!ehAlma(p) || p.isDead()) continue;
            int c = chama.computeIfAbsent(p.getUniqueId(), k -> YamlConfiguration.loadConfiguration(arquivo(k)).getInt("chama", minutos() * 60));
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) c--;
            chama.put(p.getUniqueId(), c);
            if (c % 15 == 0) salvarChama(p);
            float frac = Math.max(0f, Math.min(1f, c / (float) (minutos() * 60)));
            BossBar b = barras.computeIfAbsent(p.getUniqueId(), k -> {
                BossBar nova = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10);
                p.showBossBar(nova);
                return nova;
            });
            b.progress(frac);
            b.color(frac > 0.25f ? BossBar.Color.BLUE : BossBar.Color.RED);
            b.name(Component.text("🏮 Chama da Lanterna  " + (c / 60) + ":" + String.format("%02d", Math.max(0, c % 60)), frac > 0.25f ? COR : TextColor.color(0xE57373)));
            if (c == 60 || c == 30 || (c <= 10 && c > 0)) p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.6f);
            if (c == 60) p.sendMessage(Component.text("🏮 A chama está no fim: falta 1 minuto. Volte pela lanterna para trazer o que achou.", TextColor.color(0xE57373)));
            if (c <= 0) {
                p.sendMessage(Component.text("🏮 A chama apagou. A alma foi puxada de volta, sem os achados.", TextColor.color(0xE57373)));
                voltar(p, false, null);
            }
        }
    }

    private static void modificador(Player p, Attribute a, NamespacedKey k, double v) {
        var inst = p.getAttribute(a);
        if (inst == null || inst.getModifier(k) != null) return;
        inst.addTransientModifier(new org.bukkit.attribute.AttributeModifier(k, v, org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR, org.bukkit.inventory.EquipmentSlotGroup.ANY));
    }

    private static void semAlma(Player p) {
        var vel = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (vel != null) vel.removeModifier(K_VELOZ);
        var pulo = p.getAttribute(Attribute.JUMP_STRENGTH);
        if (pulo != null) pulo.removeModifier(K_PULO);
    }

    /** Alma não constrói nem quebra (o admin no criativo pode). */
    @EventHandler(ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (Dimensoes.de(e.getBlock().getWorld()) == Dimensoes.Dimensao.ALMAS && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("Almas não quebram nada aqui.", NamedTextColor.GRAY));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void aoConstruir(BlockPlaceEvent e) {
        if (Dimensoes.de(e.getBlock().getWorld()) == Dimensoes.Dimensao.ALMAS && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("Almas não constroem aqui.", NamedTextColor.GRAY));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTerFome(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && naDimensao(p) && e.getFoodLevel() < p.getFoodLevel()) e.setCancelled(true);
    }

    // =====================================================================
    //  Morrer, sair, voltar por outro caminho
    // =====================================================================

    /** Morrer como alma: o que estava com ela fica lá (some), sem túmulo; ela volta ao corpo. */
    @EventHandler(priority = EventPriority.LOW)
    public void aoMorrer(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (!naDimensao(p) || !ehAlma(p)) return;
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        File f = arquivo(p.getUniqueId());
        if (!f.exists()) return;
        Location corpo = YamlConfiguration.loadConfiguration(f).getLocation("corpo");
        if (corpo != null && corpo.getWorld() != null) e.setRespawnLocation(corpo);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || !ehAlma(p)) return;
            p.sendMessage(Component.text("🏮 A alma caiu no Mundo das Almas e voltou ao corpo. O que ela carregava ficou lá.", TextColor.color(0xE57373)));
            voltar(p, false, p.getLocation());
        });
    }

    /** Levado para fora do Mundo das Almas de outro jeito (comando, Pedra de Viagem): volta ao corpo ali mesmo. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoTrocarMundo(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (voltando || p.isDead() || !ehAlma(p) || naDimensao(p)) return;
        if (Dimensoes.de(e.getFrom()) != Dimensoes.Dimensao.ALMAS) return;
        voltar(p, true, p.getLocation());
    }

    @EventHandler
    public void aoSairDoJogo(PlayerQuitEvent e) {
        salvarChama(e.getPlayer());
        chama.remove(e.getPlayer().getUniqueId());
        barras.remove(e.getPlayer().getUniqueId());
    }

    /** Voltou ao jogo como alma, mas fora do Mundo das Almas (o mundo sumiu, por exemplo): volta ao corpo. */
    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!ehAlma(p)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || !ehAlma(p)) return;
            if (naDimensao(p)) p.sendMessage(Component.text("🏮 Você ainda está fora do corpo. A chama continua de onde parou.", COR));
            else voltar(p, true, null);
        });
    }

    public void desligar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            salvarChama(p);
            BossBar b = barras.get(p.getUniqueId());
            if (b != null) p.hideBossBar(b);
            semAlma(p);
        }
        barras.clear();
    }
}
