package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A Câmara das Estátuas (2.75): a Masmorra Dupla do Solo Leveling. Uma masmorra E ou D às vezes
 * esconde, atrás do chefe, uma câmara com três mandamentos escritos na parede:
 * 1. Adore o deus (agachar diante dele); 2. Louve o deus (tocar os 4 sinos); 3. Prove sua fé
 * (chegar ao altar sem se mexer enquanto os olhos dele estão abertos). Fica embaixo do vault.
 */
final class CamaraEstatuas {

    enum Fase { ADORAR, LOUVAR, PROVAR, FIM }

    static final TextColor COR = TextColor.color(0xBCAAA4);
    /** Tamanho (x, z) e altura; a câmara fica nesta altura, embaixo do vault. */
    private static final int LARG = 21, COMP = 41, ALT = 14, Y0 = 20;
    private static final int[][] SINOS = {{5, 14}, {15, 14}, {5, 26}, {15, 26}};

    private final RPGAtributos plugin;
    private final Masmorra m;
    private final World w;
    /** Canto (x0, Y0, z0) da câmara. */
    private final int x0, z0;
    /** Na sala do chefe: o portal escondido que leva para cá. */
    final Location entrada;
    /** A saída (depois de vencer), em cima do altar. */
    Location saida;

    private Fase fase = Fase.ADORAR;
    private int ticks;
    private boolean comecou;
    private final Map<UUID, Integer> ajoelhado = new HashMap<>();
    private final Set<Location> sinos = new HashSet<>();
    private final Set<UUID> salvos = new HashSet<>(), punidos = new HashSet<>();
    private final Map<UUID, Location> posicao = new HashMap<>();
    private final Set<UUID> viram = new HashSet<>();
    /** Montada (a passagem só leva para cá depois disso). */
    private boolean pronta;
    private BukkitTask tarefa;

    CamaraEstatuas(RPGAtributos plugin, Masmorra m, Location entrada) {
        this.plugin = plugin;
        this.m = m;
        this.w = m.mundo;
        this.x0 = m.inicio.centroX - LARG / 2;
        this.z0 = m.inicio.centroZ - COMP / 2;
        this.entrada = entrada;
    }

    Fase fase() { return fase; }

    /** 2.77 (HUD do mod): "abertos", "aviso" (vão abrir) ou "fechados"; null antes de começar e no fim. */
    String olhos() {
        if (!comecou || fase == Fase.FIM) return null;
        if (fase == Fase.ADORAR) return "abertos";
        if (fase != Fase.PROVAR) return "fechados";
        int c = ticks % 140;
        return c >= 80 && c < 130 ? "abertos" : c >= 60 && c < 80 ? "aviso" : "fechados";
    }

    /** 2.77 (HUD do mod): o mandamento da vez. */
    String mandamento() {
        return switch (fase) {
            case ADORAR -> "1. Adore o deus: agache-se";
            case LOUVAR -> "2. Louve o deus: sinos " + sinos.size() + "/4";
            case PROVAR -> "3. Prove a sua fé: chegue ao altar";
            case FIM -> "Fé provada";
        };
    }

    boolean dentroDela(org.bukkit.Location l) { return dentro(l); }
    boolean terminou() { return fase == Fase.FIM; }

    private Block b(int x, int y, int z) { return w.getBlockAt(x0 + x, Y0 + y, z0 + z); }

    private Location local(double x, double y, double z) { return new Location(w, x0 + x, Y0 + y, z0 + z); }

    // =====================================================================
    //  Construção (uma vez, quando o chefe cai)
    // =====================================================================

    /** Monta em 4 ticks (paredes em 3 fatias, depois os detalhes): nada de travar o servidor na hora em que o chefe cai. */
    void construir() {
        for (int fatia = 0; fatia < 3; fatia++) {
            int de = fatia * 7, ate = Math.min(LARG, de + 7);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> casca(de, ate), fatia);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, this::detalhes, 3L);
    }

    private void casca(int deX, int ateX) {
        for (int x = deX; x < ateX; x++) for (int z = 0; z < COMP; z++) for (int y = 0; y <= ALT; y++) {
            boolean borda = x == 0 || x == LARG - 1 || z == 0 || z == COMP - 1;
            Material mat;
            if (y == 0) mat = (x + z) % 4 == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE;
            else if (y == ALT) mat = Material.DEEPSLATE_TILES;
            else if (borda) mat = Material.DEEPSLATE_BRICKS;
            else mat = Material.AIR;
            b(x, y, z).setType(mat, false);
        }
    }

    private void detalhes() {
        // Pilares e luz (fraca, como num templo esquecido).
        for (int z = 6; z <= 36; z += 6) {
            for (int x : new int[]{3, LARG - 4}) {
                for (int y = 1; y < ALT; y++) b(x, y, z).setType(y % 4 == 0 ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICKS, false);
                b(x == 3 ? 4 : LARG - 5, 4, z).setType(Material.SOUL_LANTERN, false);
            }
            for (int x = 6; x <= 14; x += 4) b(x, 9, z).setType(Material.LIGHT, false);
        }
        // O deus: um corpo de pedra-negra e a cabeça com dois olhos.
        for (int x = 6; x <= 14; x++) for (int y = 1; y <= 12; y++) for (int z = 1; z <= 3; z++) {
            boolean cabeca = y >= 9 && x >= 8 && x <= 12;
            boolean corpo = y <= 8 && (x >= 7 && x <= 13);
            boolean ombros = y == 8 && x >= 6;
            if (cabeca || corpo || ombros) b(x, y, z).setType(cabeca ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE, false);
        }
        for (int y = 1; y <= 6; y++) { b(6, y, 3).setType(Material.POLISHED_BLACKSTONE_BRICKS, false); b(14, y, 3).setType(Material.POLISHED_BLACKSTONE_BRICKS, false); }
        olhos(true);
        // O altar diante dele.
        for (int x = 7; x <= 13; x++) for (int z = 5; z <= 9; z++) b(x, 0, z).setType(Material.GOLD_BLOCK, false);
        // Os quatro sinos.
        for (int[] s : SINOS) {
            Block sino = b(s[0], 1, s[1]);
            Bell bd = (Bell) Material.BELL.createBlockData();
            bd.setAttachment(Bell.Attachment.FLOOR);
            bd.setFacing(BlockFace.NORTH);
            sino.setBlockData(bd, false);
        }
        // Os mandamentos, nas paredes perto da entrada (na ordem de quem entra: esquerda e direita, depois o próximo par).
        placa(1, 3, 34, BlockFace.EAST, "Mandamentos", "da Câmara", "Quem não obedecer", "não sobreviverá.");
        placa(LARG - 2, 3, 34, BlockFace.WEST, "1. Adore", "o deus.", "", "(ajoelhe-se)");
        placa(1, 3, 31, BlockFace.EAST, "2. Louve", "o deus.", "", "(toque os sinos)");
        placa(LARG - 2, 3, 31, BlockFace.WEST, "3. Prove", "a sua fé.", "", "(chegue ao altar)");
        // As estátuas, olhando para o meio.
        for (int z = 10; z <= 34; z += 6) {
            estatua(local(2.5, 1, z + 0.5), -90f);
            estatua(local(LARG - 2.5, 1, z + 0.5), 90f);
        }
        saida = null;
        pronta = true;
    }

    private void placa(int x, int y, int z, BlockFace frente, String... linhas) {
        Block bl = b(x, y, z);
        WallSign bd = (WallSign) Material.DARK_OAK_WALL_SIGN.createBlockData();
        bd.setFacing(frente);
        bl.setBlockData(bd, false);
        if (bl.getState() instanceof Sign s) {
            for (int i = 0; i < 4 && i < linhas.length; i++) s.getSide(Side.FRONT).line(i, Component.text(linhas[i], NamedTextColor.WHITE));
            s.getSide(Side.FRONT).setGlowingText(true);
            s.setWaxed(true);
            s.update(true, false);
        }
    }

    private void estatua(Location l, float yaw) {
        l.setYaw(yaw);
        w.spawn(l, ArmorStand.class, a -> {
            a.setArms(true);
            a.setBasePlate(false);
            a.setGravity(false);
            a.setInvulnerable(true);
            a.setPersistent(false);
            a.getEquipment().setHelmet(new ItemStack(Material.STONE_BRICKS));
            a.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            a.getEquipment().setItemInMainHand(new ItemStack(ThreadLocalRandom.current().nextBoolean() ? Material.STONE_SWORD : Material.GOAT_HORN));
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                a.addEquipmentLock(s, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
        });
    }

    private void olhos(boolean abertos) {
        Material mat = abertos ? Material.REDSTONE_BLOCK : Material.POLISHED_BLACKSTONE;
        b(9, 10, 3).setType(mat, false);
        b(11, 10, 3).setType(mat, false);
    }

    // =====================================================================
    //  Entrar
    // =====================================================================

    /** O portal escondido na sala do chefe: partículas e quem entra nele vem para cá. A cada 10 ticks. */
    void portalDeEntrada(List<Player> presentes) {
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45 + Bukkit.getCurrentTick() * 9);
            w.spawnParticle(Particle.DUST, entrada.getX() + Math.cos(ang) * 0.8, entrada.getY() + (i % 3) * 0.7, entrada.getZ() + Math.sin(ang) * 0.8,
                    1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x5D4037), 1.4f));
        }
        if (fase == Fase.FIM || !pronta) return;
        for (Player p : presentes) {
            if (p.getLocation().distanceSquared(entrada) > 1.3 * 1.3) continue;
            p.teleport(local(10.5, 1, 37.5).setDirection(new org.bukkit.util.Vector(0, 0, -1)));
            if (viram.add(p.getUniqueId())) {
                JanelaSistema.mostrar(p, "Masmorra Dupla", Component.text("Você entrou na Câmara das Estátuas.", COR),
                        Component.text("1. Adore o deus: agache-se diante dele.", NamedTextColor.WHITE),
                        Component.text("2. Louve o deus: toque os quatro sinos.", NamedTextColor.WHITE),
                        Component.text("3. Prove a sua fé: chegue ao altar, mas não se mexa quando os olhos dele abrirem.", NamedTextColor.WHITE),
                        Component.text("Quem não obedecer não sobreviverá.", NamedTextColor.RED));
            }
            if (!comecou) comecar();
        }
    }

    private void comecar() {
        comecou = true;
        fase = Fase.ADORAR;
        ticks = 0;
        olhos(true);
        tarefa = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    void parar() {
        if (tarefa != null) tarefa.cancel();
        tarefa = null;
    }

    private boolean dentro(Location l) {
        return l.getWorld() == w && l.getX() >= x0 && l.getX() < x0 + LARG && l.getZ() >= z0 && l.getZ() < z0 + COMP
                && l.getY() >= Y0 && l.getY() <= Y0 + ALT;
    }

    private List<Player> naCamara() {
        List<Player> l = new ArrayList<>();
        for (UUID id : m.participantes) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !p.isDead() && dentro(p.getLocation())) l.add(p);
        }
        return l;
    }

    /** Um sino tocado (vem do clique). */
    void sino(Block b) {
        if (fase != Fase.LOUVAR || !dentro(b.getLocation())) return;
        if (sinos.add(b.getLocation())) {
            for (Player p : naCamara()) p.sendActionBar(Component.text("🔔 Sinos: " + sinos.size() + "/4", COR));
        }
    }

    // =====================================================================
    //  As fases (a cada 2 ticks)
    // =====================================================================

    private void tick() {
        List<Player> ps = naCamara();
        if (ps.isEmpty()) return;
        ticks += 2;
        int rank = m.rank().ordinal();
        switch (fase) {
            case ADORAR -> {
                for (Player p : ps) {
                    if (p.isSneaking()) ajoelhado.merge(p.getUniqueId(), 2, Integer::sum);
                    int t = Math.min(60, ajoelhado.getOrDefault(p.getUniqueId(), 0));
                    if (ticks % 10 == 0) p.sendActionBar(Component.text("1. Adore o deus: ajoelhe-se (agache)  " + (t / 20) + "/3 s", t >= 60 ? NamedTextColor.GREEN : COR));
                }
                olhar();
                boolean todos = true;
                for (Player p : ps) if (ajoelhado.getOrDefault(p.getUniqueId(), 0) < 60) todos = false;
                if (todos || ticks >= 800) {
                    if (!todos) for (Player p : ps) if (ajoelhado.getOrDefault(p.getUniqueId(), 0) < 60) julgar(p, 8 + 2 * rank, "Você não adorou o deus.");
                    proxima(Fase.LOUVAR, "2. Louve o deus: toque os quatro sinos.");
                    olhos(false);
                }
            }
            case LOUVAR -> {
                if (sinos.size() >= SINOS.length) {
                    for (Player p : ps) p.teleport(local(10.5, 1, 37.5).setDirection(new org.bukkit.util.Vector(0, 0, -1)));
                    proxima(Fase.PROVAR, "3. Prove a sua fé: chegue ao altar. Não se mexa quando os olhos abrirem.");
                } else if (ticks >= 1200) {
                    for (Player p : ps) julgar(p, 6 + 2 * rank, "Os sinos ficaram em silêncio.");
                    for (Player p : ps) p.teleport(local(10.5, 1, 37.5).setDirection(new org.bukkit.util.Vector(0, 0, -1)));
                    proxima(Fase.PROVAR, "3. Prove a sua fé: chegue ao altar. Não se mexa quando os olhos abrirem.");
                }
            }
            case PROVAR -> provar(ps, rank);
            case FIM -> parar();
        }
    }

    private void proxima(Fase f, String texto) {
        fase = f;
        ticks = 0;
        for (Player p : naCamara()) {
            p.showTitle(Title.title(Component.text("Mandamento", COR, TextDecoration.BOLD), Component.text(texto, NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        }
    }

    /** Os olhos abertos: o brilho vermelho e um zumbido. */
    private void olhar() {
        if (ticks % 6 != 0) return;
        Particle.DustOptions vermelho = new Particle.DustOptions(Color.fromRGB(0xD50000), 1.6f);
        for (int x : new int[]{9, 11}) w.spawnParticle(Particle.DUST, local(x + 0.5, 10.5, 4.2), 4, 0.15, 0.15, 0.1, 0, vermelho);
    }

    /** Ciclo de 7 s: 3 s de olhos fechados, 1 s de aviso, 2,5 s de olhos abertos (quem se mexer é punido). */
    private void provar(List<Player> ps, int rank) {
        int c = ticks % 140;
        if (c == 60) {
            for (Player p : ps) {
                p.sendActionBar(Component.text("⚠ Os olhos vão abrir!", NamedTextColor.GOLD, TextDecoration.BOLD));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1f, 0.5f);
            }
        }
        if (c == 80) {
            olhos(true);
            posicao.clear();
            punidos.clear();
            for (Player p : ps) posicao.put(p.getUniqueId(), p.getLocation());
            w.playSound(local(10, 10, 4), Sound.ENTITY_WARDEN_SONIC_CHARGE, 2f, 1.2f);
        }
        if (c >= 80 && c < 130) {
            olhar();
            for (Player p : ps) {
                if (salvos.contains(p.getUniqueId()) || punidos.contains(p.getUniqueId())) continue;
                Location antes = posicao.get(p.getUniqueId());
                if (antes == null) { posicao.put(p.getUniqueId(), p.getLocation()); continue; }
                double dx = p.getLocation().getX() - antes.getX(), dz = p.getLocation().getZ() - antes.getZ();
                if (dx * dx + dz * dz > 0.2 * 0.2) {
                    punidos.add(p.getUniqueId());
                    w.spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(0, 1, 0), 1);
                    julgar(p, 4 + 2 * rank, "Você se mexeu diante dos olhos do deus.");
                    Location volta = p.getLocation().add(0, 0, 5);
                    if (volta.getZ() > z0 + 37.5) volta.setZ(z0 + 37.5);
                    p.teleport(volta);
                }
            }
        }
        if (c == 130) olhos(false);
        // Quem chegou ao altar está salvo.
        for (Player p : ps) {
            Location l = p.getLocation();
            if (l.getZ() <= z0 + 10 && l.getX() >= x0 + 7 && l.getX() < x0 + 14 && salvos.add(p.getUniqueId())) {
                p.sendActionBar(Component.text("✔ Você chegou ao altar. Espere os outros.", NamedTextColor.GREEN));
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
            }
        }
        boolean todos = true;
        for (Player p : ps) if (!salvos.contains(p.getUniqueId())) todos = false;
        if (todos) vencer(ps);
    }

    private void julgar(Player p, double dano, String motivo) {
        w.strikeLightningEffect(p.getLocation());
        p.damage(dano);
        p.sendMessage(Component.text("⚖ " + motivo, NamedTextColor.RED));
    }

    private void vencer(List<Player> ps) {
        fase = Fase.FIM;
        olhos(false);
        parar();
        Block bau = b(10, 1, 7);
        bau.setType(Material.CHEST, false);
        if (bau.getState() instanceof Chest c) {
            c.customName(Component.text("Tesouro da Câmara", COR));
            c.update();
            Inventory inv = c.getBlockInventory();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            inv.addItem(Reliquias.sortear());
            if (r.nextBoolean()) inv.addItem(Reliquias.sortear());
            inv.addItem(ManaCristal.criar(ManaCristal.Tamanho.GRANDE, 3));
            for (int i = 0; i < 2; i++) inv.addItem(Inscricao.values()[r.nextInt(Inscricao.values().length)].criar(1));
            EquipamentoPortal eq = plugin.equipamentoPortal();
            if (eq != null) inv.addItem(eq.pecaCacadorAleatoria(Rank.C));
            if (r.nextDouble() < 0.2) inv.addItem(MagiaOculta.grimorio());
        }
        saida = local(13.5, 1, 7.5);
        for (Player p : ps) {
            p.showTitle(Title.title(Component.text("FÉ PROVADA", COR, TextDecoration.BOLD), Component.text("A Câmara das Estátuas se curva diante de você.", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
            Cacador.darXp(p, 100);
            plugin.titulos().registrar(p, "camara_estatuas", 1);
        }
        plugin.getServer().broadcast(Component.text("⚖ ", COR).append(Component.text(String.join(", ", ps.stream().map(Player::getName).toList()), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" sobreviveu à Câmara das Estátuas!", COR)));
    }
}
