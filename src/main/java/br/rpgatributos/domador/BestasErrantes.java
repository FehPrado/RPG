package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As Bestas Errantes (Pixiu, Baize e Kunpeng). Não nascem de fusão: de vez em quando uma delas
 * aparece perto de um jogador com Doma alta, no lugar que combina com ela, e fica um tempo vagando.
 * Quem cumprir a prova dela a conquista (vira companheira, única no servidor). Solta no mundo ela
 * não se fere, não se doma pelo jeito do jogo e não é salva com o mapa (some ao reiniciar).
 */
public final class BestasErrantes implements Listener {

    /** Marca a besta solta no mundo (ainda não conquistada). */
    static final NamespacedKey K_SOLTA = new NamespacedKey("rpgatributos", "besta_solta");

    /** Quantas vezes o tick (a cada 2 s) roda entre uma tentativa de aparecer e outra (5 min). */
    private static final int CICLOS_PARA_APARECER = 150;
    private static final int OURO_DO_PIXIU = 64, NEVE_DO_KUNPENG = 16, ENIGMAS = 3;
    /** Quem errou o enigma do Baize espera para tentar de novo. */
    private static final long ESPERA_BAIZE_MS = 30 * 60 * 1000L;

    private final RPGAtributos plugin;
    private int ciclo;
    private final Map<Lendaria, Long> desde = new EnumMap<>(Lendaria.class);
    private final Map<UUID, Integer> ouro = new HashMap<>(), neve = new HashMap<>();
    private final Map<UUID, Long> esperaBaize = new HashMap<>();

    public BestasErrantes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Lendarias lend() { return plugin.lendarias(); }

    /** A besta solta (ainda não conquistada)? */
    public static boolean solta(Entity e) {
        return e != null && e.getPersistentDataContainer().has(K_SOLTA);
    }

    // =====================================================================
    //  A cada 2 s: as soltas vagam; de 5 em 5 min, uma pode aparecer
    // =====================================================================

    public void tick() {
        if (!plugin.settings().domaLendarias) return;
        ciclo++;
        long agora = System.currentTimeMillis();
        for (Lendaria l : Lendaria.values()) {
            if (!l.errante()) continue;
            Lendarias.Estado est = lend().estado(l);
            if (est.fase != Lendarias.Fase.ERRANDO) continue;
            Entity ent = est.entidade == null ? null : Bukkit.getEntity(est.entidade);
            if (!(ent instanceof LivingEntity e) || !e.isValid()) {
                sumir(l, null);
                continue;
            }
            boolean velha = agora - desde.getOrDefault(l, agora) > plugin.settings().domaErrantesMinutos * 60_000L;
            if (velha || e.getWorld().getNearbyPlayers(e.getLocation(), 96).isEmpty()) {
                sumir(l, e);
                continue;
            }
            vagar(l, e);
        }
        if (ciclo % CICLOS_PARA_APARECER == 0) tentarAparecer();
    }

    private void vagar(Lendaria l, LivingEntity e) {
        VisualDomador.particulas(e, Set.of(), 1);
        if (l == Lendaria.PIXIU) {
            // Foge de quem chega de mãos vazias.
            for (Player p : e.getWorld().getNearbyPlayers(e.getLocation(), 5)) {
                if (temOuro(p)) continue;
                Vector fora = e.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                if (fora.lengthSquared() < 0.01) fora = new Vector(1, 0, 0);
                e.setVelocity(fora.normalize().multiply(0.7).setY(0.35));
                break;
            }
        }
        if (ciclo % 10 == 0) {
            Sound som = switch (l) {
                case PIXIU -> Sound.ENTITY_WOLF_GROWL;
                case BAIZE -> Sound.BLOCK_AMETHYST_BLOCK_CHIME;
                default -> Sound.ENTITY_HAPPY_GHAST_AMBIENT;
            };
            e.getWorld().playSound(e.getLocation(), som, 1.2f, 0.7f);
        }
    }

    private static boolean temOuro(Player p) {
        Material m = p.getInventory().getItemInMainHand().getType();
        return m == Material.GOLD_INGOT || m == Material.GOLD_BLOCK;
    }

    /** A besta volta a ser só uma história (some do mundo e fica livre de novo). */
    private void sumir(Lendaria l, LivingEntity e) {
        if (e != null) {
            Location c = e.getLocation().add(0, e.getHeight() / 2, 0);
            c.getWorld().spawnParticle(Particle.CLOUD, c, 40, e.getWidth() / 2, e.getHeight() / 2, e.getWidth() / 2, 0.05);
            c.getWorld().playSound(c, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 0.8f);
            for (Player p : c.getWorld().getNearbyPlayers(c, 48)) {
                p.sendMessage(Component.text("✦ O " + l.nome() + " se foi. Quem sabe ele volte a cruzar o seu caminho.", TextColor.color(l.cor())));
            }
            e.remove();
        }
        Lendarias.Estado est = lend().estado(l);
        est.fase = Lendarias.Fase.LIVRE;
        est.entidade = null;
        desde.remove(l);
        // A oferenda fica com a besta que foi embora (o Pixiu nunca devolve o ouro).
        if (l == Lendaria.PIXIU) ouro.clear();
        if (l == Lendaria.KUNPENG) neve.clear();
        lend().salvar();
    }

    private void tentarAparecer() {
        List<Player> jogadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(jogadores);
        int minimo = (int) Math.ceil(plugin.settings().domaErrantesDoma * plugin.settings().nivelMaximo);
        for (Lendaria l : Lendaria.values()) {
            if (!l.errante() || lend().estado(l).fase != Lendarias.Fase.LIVRE) continue;
            if (ThreadLocalRandom.current().nextDouble() >= plugin.settings().domaErrantesChance) continue;
            for (Player p : jogadores) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                if (plugin.stats().getNivel(p, Skill.DOMA) < minimo || !habitat(l, p)) continue;
                if (aparecer(l, p.getLocation()) != null) break;
            }
        }
    }

    /** O jogador está no lugar por onde essa besta vaga? */
    static boolean habitat(Lendaria l, Player p) {
        World w = p.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL) return false;
        Location pe = p.getLocation();
        String bioma = pe.getBlock().getBiome().getKey().getKey();
        boolean dia = w.isDayTime(), ceuAberto = pe.getBlock().getLightFromSky() >= 12;
        return switch (l) {
            case PIXIU -> (bioma.contains("badlands") && dia && ceuAberto) || pe.getY() < 0;
            case BAIZE -> !dia && !w.hasStorm() && ceuAberto
                    && (bioma.contains("forest") || bioma.contains("taiga") || bioma.contains("grove") || bioma.contains("meadow") || bioma.contains("cherry"));
            case KUNPENG -> pe.getY() >= 140 || (bioma.contains("ocean") && ceuAberto);
            default -> false;
        };
    }

    /** Faz a besta aparecer perto do lugar (admin ou sorteio). Devolve a besta ou null se não achou lugar. */
    public LivingEntity aparecer(Lendaria l, Location perto) {
        Location onde = lugar(l, perto);
        if (onde == null) return null;
        LivingEntity e = (LivingEntity) onde.getWorld().spawnEntity(onde, l.base(), org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, x -> {
            Lendarias.preparar(l, x);
            if (l == Lendaria.KUNPENG && x instanceof LivingEntity le && le.getEquipment() != null) {
                le.getEquipment().setItem(EquipmentSlot.BODY, null); // solto, sem arreio: ninguém monta
            }
            x.getPersistentDataContainer().set(Lendaria.CHAVE, PersistentDataType.STRING, l.name());
            x.getPersistentDataContainer().set(K_SOLTA, PersistentDataType.BYTE, (byte) 1);
            x.customName(Component.text(l.nome() + " " + l.hanzi(), TextColor.color(l.cor()), TextDecoration.BOLD));
            x.setCustomNameVisible(true);
            x.setInvulnerable(true);
            x.setPersistent(false);
            x.setSilent(false);
            if (x instanceof Mob m) m.setRemoveWhenFarAway(false);
        });
        if (l.pele() != null) VisualDomador.vestir(e, l.pele());
        var esc = e.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (esc != null && l.escala() > 0) esc.setBaseValue(1 + l.escala());
        Lendarias.Estado est = lend().estado(l);
        est.fase = Lendarias.Fase.ERRANDO;
        est.entidade = e.getUniqueId();
        est.dono = null;
        desde.put(l, System.currentTimeMillis());
        lend().salvar();
        Location c = e.getLocation();
        c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 40, 1, 1, 1, 0.04);
        TextColor cor = TextColor.color(l.cor());
        for (Player p : c.getWorld().getNearbyPlayers(c, 64)) {
            p.sendMessage(Component.text("✦ Você sente algo antigo por perto... o " + l.nome() + " " + l.hanzi() + " cruzou o seu caminho.", cor, TextDecoration.BOLD));
            p.sendMessage(Component.text("  " + l.prova(), NamedTextColor.GRAY));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
        }
        return e;
    }

    /** Um lugar perto (24 a 44 blocos) que sirva para a besta: chão firme, caverna ou céu aberto. */
    private static Location lugar(Lendaria l, Location perto) {
        World w = perto.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean caverna = perto.getY() < 0 && l == Lendaria.PIXIU;
        for (int tentativa = 0; tentativa < 16; tentativa++) {
            double ang = r.nextDouble(Math.PI * 2), dist = 24 + r.nextDouble(20);
            int x = (int) Math.floor(perto.getX() + Math.cos(ang) * dist), z = (int) Math.floor(perto.getZ() + Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            if (l == Lendaria.KUNPENG) {
                int y = (int) Math.max(perto.getY() + 6, w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING) + 12);
                if (y > w.getMaxHeight() - 8) continue;
                if (livre(w, x, y, z, 5)) return new Location(w, x + 0.5, y, z + 0.5);
                continue;
            }
            if (caverna) {
                for (int y = perto.getBlockY() + 6; y >= perto.getBlockY() - 10; y--) {
                    Block chao = w.getBlockAt(x, y - 1, z);
                    if (chao.getType().isSolid() && livre(w, x, y, z, 3)) return new Location(w, x + 0.5, y, z + 0.5);
                }
                continue;
            }
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block chao = w.getBlockAt(x, y, z);
            if (!chao.getType().isSolid() || chao.isLiquid() || !livre(w, x, y + 1, z, 3)) continue;
            return new Location(w, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private static boolean livre(World w, int x, int y, int z, int altura) {
        for (int i = 0; i < altura; i++) {
            Block b = w.getBlockAt(x, y + i, z);
            if (!b.isPassable() || b.isLiquid()) return false;
        }
        return true;
    }

    // =====================================================================
    //  As provas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoClicar(PlayerInteractEntityEvent e) {
        Entity alvo = e.getRightClicked();
        if (!solta(alvo)) {
            alimentarPixiu(e);
            return;
        }
        e.setCancelled(true); // solta: nada do jogo (domar, montar, arreio, laço) funciona nela
        if (e.getHand() != EquipmentSlot.HAND || !(alvo instanceof LivingEntity besta)) return;
        Lendaria l = Lendaria.de(besta);
        Player p = e.getPlayer();
        if (l == null) return;
        ItemStack mao = p.getInventory().getItemInMainHand();
        TextColor cor = TextColor.color(l.cor());
        switch (l) {
            case PIXIU -> {
                int valor = mao.getType() == Material.GOLD_INGOT ? 1 : mao.getType() == Material.GOLD_BLOCK ? 9 : 0;
                if (valor == 0) {
                    p.sendActionBar(Component.text("O Pixiu fareja suas mãos e não acha ouro.", cor));
                    return;
                }
                int tem = ouro.getOrDefault(p.getUniqueId(), 0);
                int usar = Math.min(mao.getAmount(), (int) Math.ceil((OURO_DO_PIXIU - tem) / (double) valor));
                mao.setAmount(mao.getAmount() - usar);
                tem += usar * valor;
                ouro.put(p.getUniqueId(), tem);
                besta.getWorld().spawnParticle(Particle.WAX_ON, besta.getLocation().add(0, 0.8, 0), 20, 0.4, 0.4, 0.4, 0);
                besta.getWorld().playSound(besta.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 0.8f);
                if (tem >= OURO_DO_PIXIU) conquistar(l, besta, p);
                else p.sendActionBar(Component.text("O Pixiu devora o ouro... (" + tem + "/" + OURO_DO_PIXIU + ")", cor));
            }
            case KUNPENG -> {
                int tem = neve.getOrDefault(p.getUniqueId(), 0);
                if (mao.getType() == Material.SNOWBALL && tem < NEVE_DO_KUNPENG) {
                    int usar = Math.min(mao.getAmount(), NEVE_DO_KUNPENG - tem);
                    mao.setAmount(mao.getAmount() - usar);
                    tem += usar;
                    neve.put(p.getUniqueId(), tem);
                    besta.getWorld().spawnParticle(Particle.SNOWFLAKE, besta.getLocation().add(0, 2, 0), 30, 1.5, 1, 1.5, 0.02);
                    besta.getWorld().playSound(besta.getLocation(), Sound.ENTITY_HAPPY_GHAST_AMBIENT, 1f, 1.2f);
                    p.sendActionBar(Component.text(tem < NEVE_DO_KUNPENG ? "O Kunpeng aceita a neve... (" + tem + "/" + NEVE_DO_KUNPENG + ")"
                            : "O Kunpeng está satisfeito. Agora, um Fragmento Celeste.", cor));
                    return;
                }
                if (tem >= NEVE_DO_KUNPENG && Raro.FRAGMENTO_CELESTE.eh(mao)) {
                    mao.setAmount(mao.getAmount() - 1);
                    conquistar(l, besta, p);
                    return;
                }
                p.sendActionBar(Component.text(tem < NEVE_DO_KUNPENG ? "O Kunpeng quer neve (" + tem + "/" + NEVE_DO_KUNPENG + ")." : "Falta o Fragmento Celeste.", cor));
            }
            case BAIZE -> {
                long espera = esperaBaize.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
                if (espera > 0) {
                    p.sendActionBar(Component.text("O Baize não responde a quem já errou (" + (espera / 60000 + 1) + " min).", cor));
                    return;
                }
                abrirEnigma(p, besta, 0, sortearEnigmas());
            }
            default -> { }
        }
    }

    /** Conquistada: a besta solta some e nasce no lugar dela, já como companheira. */
    private void conquistar(Lendaria l, LivingEntity besta, Player p) {
        Location onde = besta.getLocation();
        besta.remove();
        desde.remove(l);
        ouro.clear();
        neve.clear();
        lend().nascer(l, p, onde);
    }

    // =====================================================================
    //  Os enigmas do Baize
    // =====================================================================

    /** Pergunta, a resposta certa e duas erradas (sobre o mundo do servidor). */
    private record Enigma(String pergunta, String certa, String errada1, String errada2) { }

    private static final List<Enigma> ENIGMAS_DO_BAIZE = List.of(
            new Enigma("Qual guardião renasce das próprias cinzas?", "O Fenghuang", "O Qilin", "O Xuanwu"),
            new Enigma("O que ilumina as Profundezas para sempre?", "As Raízes de Luz", "As tochas de alma", "A lava"),
            new Enigma("Que fera devora ouro e nunca o devolve?", "O Pixiu", "O Baihu", "O Longma"),
            new Enigma("Onde choca o ovo do Qilin?", "Num campo de flores, de dia", "Perto da lava", "Na neve"),
            new Enigma("O que rouba corações nas Profundezas?", "A Penumbra", "O Guardião dos Ventos", "O Afogado"),
            new Enigma("O que lança o viajante para o céu, sem caminho de volta?", "A Plataforma de Vento", "O Poço dos Desejos", "O Balão de Cristal"),
            new Enigma("Que tartaruga carrega uma serpente no casco?", "O Xuanwu", "O Longma", "O Kunpeng"),
            new Enigma("Que peixe era tão grande que virou um pássaro?", "O Kun, que virou Kunpeng", "O bacalhau", "O baiacu"),
            new Enigma("Qual destas criaturas não teme o fogo nem a lava?", "O strider", "O lobo", "A ovelha"),
            new Enigma("O que o lobo aceita para virar companheiro?", "Um osso", "Trigo", "Uma cenoura"),
            new Enigma("Que guardião é o tigre branco do oeste?", "O Baihu", "O Qilin", "O Fenghuang"),
            new Enigma("O que o cavalo-dragão trouxe nas costas ao sair do rio?", "O desenho do mundo", "Uma espada", "Um ovo"));

    private static List<Integer> sortearEnigmas() {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < ENIGMAS_DO_BAIZE.size(); i++) l.add(i);
        Collections.shuffle(l);
        return new ArrayList<>(l.subList(0, ENIGMAS));
    }

    /** A tela do enigma: guarda qual pergunta é, onde está a resposta certa e quem é o Baize. */
    private static final class TelaEnigma implements InventoryHolder {
        Inventory inv;
        UUID besta;
        int passo, certa;
        List<Integer> perguntas;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int[] RESPOSTAS = {11, 13, 15};

    private void abrirEnigma(Player p, LivingEntity besta, int passo, List<Integer> perguntas) {
        Enigma en = ENIGMAS_DO_BAIZE.get(perguntas.get(passo));
        TelaEnigma t = new TelaEnigma();
        t.besta = besta.getUniqueId();
        t.passo = passo;
        t.perguntas = perguntas;
        t.inv = Bukkit.createInventory(t, 27, Component.text("白泽 O Baize pergunta (" + (passo + 1) + "/" + ENIGMAS + ")"));
        ItemStack vidro = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.itemName(Component.text(" ")));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        t.inv.setItem(4, item(Material.WRITABLE_BOOK, Component.text(en.pergunta(), TextColor.color(Lendaria.BAIZE.cor()), TextDecoration.BOLD),
                List.of(Component.text("Escolha a resposta. Se errar, ele some na névoa.", NamedTextColor.GRAY))));
        List<String> opcoes = new ArrayList<>(List.of(en.certa(), en.errada1(), en.errada2()));
        Collections.shuffle(opcoes);
        for (int i = 0; i < 3; i++) {
            if (opcoes.get(i).equals(en.certa())) t.certa = RESPOSTAS[i];
            t.inv.setItem(RESPOSTAS[i], item(Material.PAPER, Component.text(opcoes.get(i), NamedTextColor.WHITE), List.of()));
        }
        p.openInventory(t.inv);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TelaEnigma) e.setCancelled(true);
    }

    @EventHandler
    public void aoResponder(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TelaEnigma t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getSlot();
        if (slot != RESPOSTAS[0] && slot != RESPOSTAS[1] && slot != RESPOSTAS[2]) return;
        Lendarias.Estado est = lend().estado(Lendaria.BAIZE);
        if (est.fase != Lendarias.Fase.ERRANDO || !t.besta.equals(est.entidade) || !(Bukkit.getEntity(t.besta) instanceof LivingEntity besta)) {
            p.closeInventory();
            p.sendMessage(Component.text("✖ O Baize já não está aqui.", NamedTextColor.RED));
            return;
        }
        if (slot != t.certa) {
            p.closeInventory();
            esperaBaize.put(p.getUniqueId(), System.currentTimeMillis() + ESPERA_BAIZE_MS);
            p.sendMessage(Component.text("✖ O Baize balança a cabeça devagar e some na névoa.", TextColor.color(Lendaria.BAIZE.cor())));
            sumir(Lendaria.BAIZE, besta);
            return;
        }
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
        if (t.passo + 1 >= ENIGMAS) {
            p.closeInventory();
            conquistar(Lendaria.BAIZE, besta, p);
            return;
        }
        abrirEnigma(p, besta, t.passo + 1, t.perguntas);
    }

    // =====================================================================
    //  Proteções da besta solta e os poderes das conquistadas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDomar(EntityTameEvent e) {
        if (solta(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMontar(EntityMountEvent e) {
        if (solta(e.getMount())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoLacar(PlayerLeashEntityEvent e) {
        if (solta(e.getEntity())) e.setCancelled(true);
    }

    /** Pixiu conquistado: pepita ou lingote de ouro na mão cura ele. */
    private void alimentarPixiu(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof LivingEntity px) || Lendaria.de(px) != Lendaria.PIXIU) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        double cura = mao.getType() == Material.GOLD_NUGGET ? 2 : mao.getType() == Material.GOLD_INGOT ? 10 : 0;
        if (cura == 0 || !plugin.companheiros().eDono(p, px)) return;
        var max = px.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (max == null || px.getHealth() >= max.getValue()) return;
        e.setCancelled(true);
        mao.setAmount(mao.getAmount() - 1);
        px.setHealth(Math.min(max.getValue(), px.getHealth() + cura));
        px.getWorld().spawnParticle(Particle.WAX_ON, px.getLocation().add(0, 0.8, 0), 12, 0.3, 0.3, 0.3, 0);
        px.getWorld().playSound(px.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
    }

    /** Fortuna do Pixiu: minério quebrado perto dele (16 blocos) às vezes rende em dobro. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoMinerar(BlockBreakEvent e) {
        Material m = e.getBlock().getType();
        if (!m.name().endsWith("_ORE") || !e.isDropItems() || ThreadLocalRandom.current().nextDouble() >= 0.25) return;
        Lendarias.Estado est = lend().estado(Lendaria.PIXIU);
        if (est.fase != Lendarias.Fase.VIVA || !e.getPlayer().getUniqueId().equals(est.dono) || est.entidade == null) return;
        if (!(Bukkit.getEntity(est.entidade) instanceof LivingEntity px) || !px.getWorld().equals(e.getBlock().getWorld())
                || px.getLocation().distanceSquared(e.getBlock().getLocation()) > 16 * 16) return;
        Location c = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack i : e.getBlock().getDrops(e.getPlayer().getInventory().getItemInMainHand(), e.getPlayer())) {
            c.getWorld().dropItemNaturally(c, i);
        }
        c.getWorld().spawnParticle(Particle.WAX_ON, c, 12, 0.4, 0.4, 0.4, 0);
        e.getPlayer().sendActionBar(Component.text("✦ Fortuna do Pixiu: o minério rendeu em dobro!", TextColor.color(Lendaria.PIXIU.cor())));
    }

    /** Kunpeng: quem desce dele no ar cai devagar. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoDescer(EntityDismountEvent e) {
        if (!(e.getEntity() instanceof Player p) || Lendaria.de(e.getDismounted()) != Lendaria.KUNPENG || solta(e.getDismounted())) return;
        if (!e.getDismounted().isOnGround()) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 160, 0, true, false, true));
    }

    /** Admin: o que está acontecendo com a besta (para /rpgadmin lendaria). */
    public String detalhes(Lendaria l) {
        Lendarias.Estado est = lend().estado(l);
        if (est.fase != Lendarias.Fase.ERRANDO) return lend().situacao(l);
        Entity e = est.entidade == null ? null : Bukkit.getEntity(est.entidade);
        long min = (System.currentTimeMillis() - desde.getOrDefault(l, System.currentTimeMillis())) / 60000;
        return e == null ? "solta, mas não carregada" : String.format(Locale.ROOT, "solta em %s %d %d %d há %d min",
                e.getWorld().getName(), e.getLocation().getBlockX(), e.getLocation().getBlockY(), e.getLocation().getBlockZ(), min);
    }
}
