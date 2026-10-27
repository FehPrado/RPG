package br.rpgatributos.ritual;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Eventos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 2.90: o Círculo Arcano. Um pentagrama desenhado no chão em volta de uma obsidiana: velas acesas
 * nas 5 pontas (a 5 blocos do centro) e o traço da estrela feito com pó de redstone ou com blocos
 * "de traço" no piso (obsidiana, pedra-negra, tijolos do Nether, ametista...). Jogando 1 olho do
 * ender e 4 pós de pedra luminosa na obsidiana, o círculo desperta.
 * Os rituais: as oferendas são jogadas dentro do círculo; com o catalisador na mão, um clique na
 * obsidiana acende o traço, e um círculo gigante de luz se desenha no céu, gira e desce em feixe.
 * Pactos (bônus com um preço), tempestade, fenda do vazio, conjurações, eclipse, lua de sangue,
 * banimento e revelação.
 */
public final class CirculoArcano extends Estacao {

    public static final TextColor COR = TextColor.color(0xE0405A);
    private static final Color VERMELHO = Color.fromRGB(0xE0405A), ROXO = Color.fromRGB(0x9B59FF), OURO = Color.fromRGB(0xFFD27A);
    static final int RAIO = 5;
    /** As 5 pontas da estrela (dx, dz), começando pela do fundo. */
    static final int[][] PONTAS = {{0, -5}, {5, -2}, {3, 4}, {-3, 4}, {-5, -2}};
    /** As casas do traço da estrela (cada ponta liga na segunda seguinte), sem as pontas. */
    static final List<int[]> TRACO = new ArrayList<>();
    /** Os 5 segmentos, na ordem de acender. */
    static final List<List<int[]>> SEGMENTOS = new ArrayList<>();
    /** Blocos que valem como traço no piso (além do pó de redstone em cima). */
    static final Set<Material> BLOCOS_TRACO = Set.of(Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE,
            Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS, Material.GILDED_BLACKSTONE, Material.NETHER_BRICKS,
            Material.RED_NETHER_BRICKS, Material.REDSTONE_BLOCK, Material.AMETHYST_BLOCK, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.GLOWSTONE, Material.SHROOMLIGHT, Material.CRIMSON_NYLIUM, Material.MAGMA_BLOCK, Material.RED_CONCRETE,
            Material.BLACK_CONCRETE, Material.PURPLE_CONCRETE, Material.RED_WOOL, Material.BLACK_WOOL, Material.PURPLE_WOOL);

    static {
        Set<String> vistos = new LinkedHashSet<>();
        for (int k = 0; k < 5; k++) {
            int[] a = PONTAS[k], b = PONTAS[(k + 2) % 5];
            List<int[]> seg = new ArrayList<>();
            int n = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
            for (int i = 1; i < n; i++) {
                int x = Math.round(a[0] + (b[0] - a[0]) * i / (float) n), z = Math.round(a[1] + (b[1] - a[1]) * i / (float) n);
                seg.add(new int[]{x, z});
                if (vistos.add(x + "," + z) && !(x == 0 && z == 0)) TRACO.add(new int[]{x, z});
            }
            SEGMENTOS.add(seg);
        }
    }

    private static final NamespacedKey K_PACTO = new NamespacedKey("rpgatributos", "pacto_arcano");
    private static final NamespacedKey K_PACTO_ATE = new NamespacedKey("rpgatributos", "pacto_arcano_ate");
    private static final NamespacedKey M_PACTO_DANO = new NamespacedKey("rpgatributos", "pacto_forca_dano");
    private static final NamespacedKey M_PACTO_VIDA = new NamespacedKey("rpgatributos", "pacto_forca_vida");
    private static final NamespacedKey K_FENDA = new NamespacedKey("rpgatributos", "fenda_vazio");
    private static final NamespacedKey K_ALIADO = new NamespacedKey("rpgatributos", "espirito_aliado");

    // =====================================================================
    //  Os rituais
    // =====================================================================

    enum Pacto { FORCA, SOMBRA, FORTUNA }

    /** Um ritual do círculo: catalisador na mão, oferendas jogadas dentro do círculo. */
    record Ritual(String id, String nome, Material icone, Material catalisador, int qtdCatalisador, Map<Object, Integer> oferendas,
                  String descricao, String condicao) {
        String listaOferendas() {
            List<String> l = new ArrayList<>();
            for (Map.Entry<Object, Integer> e : oferendas.entrySet()) {
                l.add(e.getValue() + "x " + (e.getKey() instanceof Raro r ? r.nome() : ReceitaRitual.bonito((Material) e.getKey()).toLowerCase()));
            }
            return String.join(", ", l);
        }
    }

    private static Map<Object, Integer> of(Object... pares) {
        Map<Object, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) m.put(pares[i], (Integer) pares[i + 1]);
        return m;
    }

    static final List<Ritual> RITUAIS = List.of(
            new Ritual("pacto_forca", "Pacto da Força", Material.BLAZE_POWDER, Material.BLAZE_ROD, 2,
                    of(Material.ROTTEN_FLESH, 16, Material.BONE, 8, Material.REDSTONE, 8),
                    "Por 1 hora: +25% de dano corpo a corpo. O preço: 2 corações a menos.", null),
            new Ritual("pacto_sombra", "Pacto da Sombra", Material.PHANTOM_MEMBRANE, Material.ECHO_SHARD, 1,
                    of(Material.PHANTOM_MEMBRANE, 4, Material.INK_SAC, 8, Material.SOUL_SAND, 4),
                    "Por 1 hora: no escuro (noite ou caverna), invisível e mais rápido. O preço: sob o sol, fraqueza.", null),
            new Ritual("pacto_fortuna", "Pacto da Fortuna", Material.RABBIT_FOOT, Material.EMERALD, 8,
                    of(Material.GOLD_INGOT, 8, Material.RABBIT_FOOT, 1, Material.LAPIS_LAZULI, 16),
                    "Por 1 hora: Sorte II, Pressa e Herói da Vila (desconto nos aldeões). O preço: fome o tempo todo.", null),
            new Ritual("tempestade", "Tempestade Arcana", Material.LIGHTNING_ROD, Material.LIGHTNING_ROD, 1,
                    of(Material.COPPER_INGOT, 8, Material.BREEZE_ROD, 2, Material.GLOWSTONE_DUST, 8),
                    "Uma tempestade de 5 minutos. Por 2 minutos, raios caem nos monstros a até 48 blocos do círculo.", null),
            new Ritual("fenda", "Fenda do Vazio", Material.END_PORTAL_FRAME, Material.ENDER_PEARL, 4,
                    of(Material.OBSIDIAN, 8, Material.CHORUS_FRUIT, 8, Material.ECHO_SHARD, 2),
                    "Uma fenda se abre sobre o círculo: 3 ondas de criaturas do vazio e o Guardião da Fenda. Feche-a para o tesouro.",
                    "Sem outra fenda aberta"),
            new Ritual("corcel", "Conjurar Corcel Espectral", Material.SADDLE, Material.SADDLE, 1,
                    of(Material.BONE_BLOCK, 4, Material.SOUL_SAND, 8, Material.GOLDEN_CARROT, 4),
                    "Um cavalo esqueleto selado, seu companheiro (se tiver vaga).", null),
            new Ritual("espiritos", "Conjurar Espíritos Aliados", Material.GHAST_TEAR, Material.GHAST_TEAR, 1,
                    of(Material.FEATHER, 8, Material.GLOWSTONE_DUST, 8, Material.SOUL_SAND, 4),
                    "3 espíritos (vexes) lutam ao seu lado por 3 minutos, só contra monstros.", null),
            new Ritual("eclipse", "Chamar o Eclipse", Material.CLOCK, Material.CLOCK, 1,
                    of(Material.COAL_BLOCK, 4, Material.INK_SAC, 16, Material.OBSIDIAN, 4),
                    "O sol some: eclipse de 2,5 minutos (monstros e magias mais fortes, o dobro de XP).", "De dia, no mundo principal"),
            new Ritual("lua_sangue", "Chamar a Lua de Sangue", Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, 2,
                    of(Material.ROTTEN_FLESH, 16, Material.BONE, 16, Material.SPIDER_EYE, 4),
                    "A lua fica vermelha até o amanhecer: monstros mais fortes, mais Elites e o dobro de XP.", "À noite, no mundo principal"),
            new Ritual("banimento", "Banimento", Material.SOUL_LANTERN, Material.SOUL_LANTERN, 1,
                    of(Material.BONE_MEAL, 16, Material.GLOWSTONE_DUST, 8, Material.WHITE_CANDLE, 1),
                    "Os monstros a até 32 blocos se desfazem (chefes não), e nenhum nasce a até 48 blocos por 10 minutos.", null),
            new Ritual("revelacao", "Revelação Astral", Material.SPYGLASS, Material.SPYGLASS, 1,
                    of(Material.GLOW_INK_SAC, 4, Material.AMETHYST_SHARD, 8, Material.ENDER_EYE, 1),
                    "Por 1 minuto, toda criatura a até 96 blocos brilha; visão noturna por 5 minutos e o rumo da estrutura mais perto que você não viu.", null));

    // =====================================================================
    //  Estado
    // =====================================================================

    /** Uma cerimônia acontecendo (uma por círculo). */
    private final Map<Location, BukkitTask> cerimonias = new HashMap<>();
    /** Zonas de banimento: centro → até quando. */
    private final Map<Location, Long> banidas = new HashMap<>();
    /** Tempestades: centro → até quando (ms). */
    private final Map<Location, Long> tempestades = new HashMap<>();
    /** Espíritos aliados → dono. */
    private final Map<UUID, UUID> aliados = new HashMap<>();
    private Fenda fenda;
    private int voltas;

    private static final class Grimorio implements InventoryHolder {
        Inventory inventario;
        @Override public Inventory getInventory() { return inventario; }
    }

    public CirculoArcano(RPGAtributos plugin) {
        super(plugin, "circulos_arcanos", "circulo_arcano_display");
    }

    // =====================================================================
    //  O desenho
    // =====================================================================

    /** O que tem desenhado em volta do centro: velas acesas, casas do traço feitas e o total. */
    record Desenho(int velas, int traco, int total) {
        boolean pronto() { return velas == 5 && traco * 100 >= total * 85; }

        String falta() {
            List<String> l = new ArrayList<>();
            if (velas < 5) l.add((5 - velas) + " vela(s) acesa(s) nas pontas");
            int precisa = (int) Math.ceil(total * 0.85);
            if (traco < precisa) l.add((precisa - traco) + " casa(s) do traço da estrela");
            return String.join(" e ", l);
        }
    }

    static Desenho desenho(Block centro) {
        int velas = 0, traco = 0;
        for (int[] p : PONTAS) {
            for (int dy = 1; dy <= 2; dy++) {
                Block b = centro.getRelative(p[0], dy, p[1]);
                if (Tag.CANDLES.isTagged(b.getType()) && b.getBlockData() instanceof Lightable l && l.isLit()) { velas++; break; }
            }
        }
        for (int[] c : TRACO) if (traco(centro, c[0], c[1])) traco++;
        return new Desenho(velas, traco, TRACO.size());
    }

    private static boolean traco(Block centro, int dx, int dz) {
        Block piso = centro.getRelative(dx, 0, dz);
        return piso.getRelative(0, 1, 0).getType() == Material.REDSTONE_WIRE || BLOCOS_TRACO.contains(piso.getType());
    }

    // =====================================================================
    //  Estação
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) { return m == Material.OBSIDIAN; }

    @Override
    protected boolean itemDoRitual(Material m) { return m == Material.ENDER_EYE || m == Material.GLOWSTONE_DUST; }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.OBSIDIAN ? abaixo : null;
    }

    @Override
    protected String impedimento(Block b, UUID quem) {
        String t = super.impedimento(b, quem);
        if (t != null) return t;
        Desenho d = desenho(b);
        return d.pronto() ? null : "Para despertar o Círculo Arcano, desenhe o pentagrama em volta da obsidiana: falta " + d.falta()
                + ". O desenho está em /guia circuloarcano.";
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.ENDER_EYE) < 1 || contar(itens, Material.GLOWSTONE_DUST) < 4) return false;
        tirar(itens, Material.ENDER_EYE, 1);
        tirar(itens, Material.GLOWSTONE_DUST, 4);
        return true;
    }

    @Override protected ItemStack itemFlutuante() { return new ItemStack(Material.ENDER_EYE); }
    @Override protected float escalaItem() { return 0.5f; }
    @Override protected double alturaItem() { return 1.6; }

    @Override
    protected Component nome() {
        return Component.text("⛤ Círculo Arcano ⛤", COR)
                .append(Component.newline())
                .append(Component.text("oferendas no círculo, catalisador na mão", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        acenderTraco(b, 1, VERMELHO);
        circuloNoCeu(c.clone().add(0, 14, 0), 7, 0, VERMELHO, 1);
        w.strikeLightningEffect(c);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.6f);
        w.playSound(c, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 0.7f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("⛤ Círculo Arcano ⛤", COR, TextDecoration.BOLD),
                Component.text("O pentagrama despertou", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))));
        quem.sendMessage(Component.text("⛤ Jogue as oferendas dentro do círculo e clique na obsidiana com o catalisador. Mão vazia abre o grimório.", COR));
        plugin.stats().darXp(quem, Skill.ARCANO, 40);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        World w = centro.getWorld();
        w.spawnParticle(Particle.DUST, centro, 2, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(VERMELHO, 1f));
        if (ciclo % 4 == 0) {
            Block b = centro.getBlock().getRelative(0, -1, 0);
            Block piso = centro.clone().subtract(0, alturaItem(), 0).getBlock();
            int[] p = PONTAS[(ciclo / 4) % 5];
            w.spawnParticle(Particle.SMALL_FLAME, piso.getLocation().add(p[0] + 0.5, 1.6, p[1] + 0.5), 2, 0.05, 0.1, 0.05, 0.01);
            if (b != null) w.spawnParticle(Particle.ENCHANT, centro, 6, 0.4, 0.2, 0.4, 0.6);
        }
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.ENDER_EYE));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GLOWSTONE_DUST, 4));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        if (cerimonias.containsKey(l)) { p.sendActionBar(Component.text("⛤ O ritual já está acontecendo.", COR)); return; }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.isEmpty()) {
            if (agachado) {
                Desenho d = desenho(b);
                p.sendMessage(Component.text("⛤ Velas acesas: " + d.velas() + "/5 · traço: " + d.traco() + "/" + d.total()
                        + (d.pronto() ? " · o pentagrama está inteiro." : " · falta " + d.falta() + "."), COR));
            } else {
                abrirGrimorio(p);
            }
            return;
        }
        tentar(p, b, mao);
    }

    // =====================================================================
    //  Tentar um ritual
    // =====================================================================

    /** As oferendas jogadas dentro do círculo. */
    private List<Item> oferendas(Block centro) {
        List<Item> l = new ArrayList<>();
        Location c = centro.getLocation().add(0.5, 1, 0.5);
        for (Entity e : c.getWorld().getNearbyEntities(c, RAIO + 0.5, 2.5, RAIO + 0.5)) {
            if (e instanceof Item i && i.isValid() && i.getOwner() == null) l.add(i);
        }
        return l;
    }

    private static int quantas(List<Item> itens, Object qual) {
        int n = 0;
        for (Item i : itens) {
            ItemStack s = i.getItemStack();
            if (qual instanceof Raro r ? Raro.de(s) == r : s.getType() == qual && Raro.de(s) == null) n += s.getAmount();
        }
        return n;
    }

    private void gastar(List<Item> itens, Object qual, int qtd) {
        for (Item i : itens) {
            if (qtd <= 0) return;
            ItemStack s = i.getItemStack();
            if (!(qual instanceof Raro r ? Raro.de(s) == r : s.getType() == qual && Raro.de(s) == null)) continue;
            int tira = Math.min(qtd, s.getAmount());
            qtd -= tira;
            Location l = i.getLocation();
            l.getWorld().spawnParticle(Particle.SOUL, l, 3, 0.1, 0.1, 0.1, 0.02);
            if (tira >= s.getAmount()) i.remove();
            else { s.setAmount(s.getAmount() - tira); i.setItemStack(s); }
        }
    }

    private void tentar(Player p, Block b, ItemStack mao) {
        if (!plugin.territorios().podeConstruir(p, b)) {
            p.sendMessage(Component.text("✖ Só quem pode construir aqui acende esse círculo.", NamedTextColor.RED));
            return;
        }
        Desenho d = desenho(b);
        if (!d.pronto()) {
            p.sendMessage(Component.text("✖ O pentagrama está incompleto: falta " + d.falta() + ".", NamedTextColor.RED));
            return;
        }
        List<Item> itens = oferendas(b);
        String erro = null;
        boolean algum = false;
        for (Ritual r : RITUAIS) {
            if (mao.getType() != r.catalisador() || mao.getAmount() < r.qtdCatalisador()
                    || (mao.hasItemMeta() && !mao.getItemMeta().getPersistentDataContainer().getKeys().isEmpty())) continue;
            algum = true;
            String falta = null;
            for (Map.Entry<Object, Integer> e : r.oferendas().entrySet()) {
                if (quantas(itens, e.getKey()) < e.getValue()) { falta = r.listaOferendas(); break; }
            }
            if (falta != null) { erro = r.nome() + " pede, jogadas dentro do círculo: " + falta + "."; continue; }
            String imp = impedimento(r, p, b);
            if (imp != null) { erro = imp; continue; }
            for (Map.Entry<Object, Integer> e : r.oferendas().entrySet()) gastar(itens, e.getKey(), e.getValue());
            mao.setAmount(mao.getAmount() - r.qtdCatalisador());
            cerimonia(p, b, r);
            return;
        }
        if (erro == null) erro = algum ? "Faltam oferendas." : "Esse item não é catalisador de nenhum ritual do círculo (mão vazia abre o grimório).";
        p.sendMessage(Component.text("✖ " + erro, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private String impedimento(Ritual r, Player p, Block b) {
        World w = b.getWorld();
        boolean principal = w.equals(Bukkit.getWorlds().getFirst());
        return switch (r.id()) {
            case "fenda" -> fenda != null ? "Já tem uma Fenda do Vazio aberta: feche aquela antes." : null;
            case "eclipse" -> !principal ? "O eclipse só acontece no mundo principal."
                    : Eventos.noite(w) ? "O eclipse precisa de sol: faça de dia." : plugin.ceu().eclipse() ? "Já é eclipse!" : null;
            case "lua_sangue" -> !principal ? "A Lua de Sangue só nasce no mundo principal."
                    : !Eventos.noite(w) ? "A Lua de Sangue só pode ser chamada à noite." : plugin.ceu().luaDeSangue() ? "Já é Lua de Sangue!" : null;
            case "pacto_forca", "pacto_sombra", "pacto_fortuna" -> {
                String atual = p.getPersistentDataContainer().get(K_PACTO, PersistentDataType.STRING);
                long ate = p.getPersistentDataContainer().getOrDefault(K_PACTO_ATE, PersistentDataType.LONG, 0L);
                yield atual != null && ate > System.currentTimeMillis()
                        ? "Você já tem um pacto (mais " + ((ate - System.currentTimeMillis()) / 60_000 + 1) + " min). Um por vez." : null;
            }
            default -> null;
        };
    }

    // =====================================================================
    //  A cerimônia: o traço acende, o círculo se desenha no céu, o feixe desce
    // =====================================================================

    private static final int DURACAO = 160;

    private void cerimonia(Player p, Block b, Ritual r) {
        Location bloco = b.getLocation();
        Location c = bloco.clone().add(0.5, 1, 0.5);
        Location ceu = c.clone().add(0, 16, 0);
        World w = b.getWorld();
        UUID dono = p.getUniqueId();
        Color cor = r.id().startsWith("pacto") ? VERMELHO : r.id().equals("revelacao") || r.id().equals("banimento") ? OURO : ROXO;
        p.sendMessage(Component.text("⛤ " + r.nome() + ": não saia do círculo e não apague as velas.", COR));
        w.playSound(c, Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 1.5f, 0.6f);
        int[] t = {0};
        double[] giro = {0};
        BukkitTask tarefa = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            t[0]++;
            int k = t[0];
            if (!eh(b)) { parar(bloco); return; }
            if (k % 10 == 0 && k < DURACAO - 20) {
                Player dp = Bukkit.getPlayer(dono);
                String falha = dp == null || dp.isDead() || dp.getWorld() != w ? "quem acendeu sumiu"
                        : dp.getLocation().distanceSquared(c) > 16 * 16 ? dp.getName() + " saiu de perto do círculo"
                        : !desenho(b).pronto() ? "o pentagrama foi quebrado" : null;
                if (falha != null) { contragolpe(b, dp, r, falha); return; }
            }
            // 0–2 s: o traço acende, ponta por ponta
            if (k <= 40) {
                acenderTraco(b, Math.min(5, k / 8 + 1), VERMELHO);
                if (k % 8 == 1) w.playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 0.5f + k / 40f);
            } else {
                acenderTraco(b, 5, k % 4 < 2 ? VERMELHO : cor);
            }
            // 1,5–6 s: o círculo do céu cresce e gira; feixe entre o céu e o chão
            if (k >= 30) {
                double f = Math.min(1, (k - 30) / 60.0);
                giro[0] += 0.02 + (k > 110 ? (k - 110) * 0.004 : 0);
                if (k % 2 == 0) circuloNoCeu(ceu, 2 + 7 * f, giro[0], cor, f);
                if (k >= 70 && k % 2 == 0) feixe(c, ceu, cor);
                if (k % 20 == 0) w.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.6f + (float) f * 0.6f);
            }
            if (k == 90 || k == 120 || k == 140) w.strikeLightningEffect(c.clone().add(ThreadLocalRandom.current().nextInt(-4, 5), 0, ThreadLocalRandom.current().nextInt(-4, 5)));
            if (k == 100) w.playSound(c, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2f, 0.5f);
            if (k == DURACAO - 20) {
                // O clarão: o círculo desce num pulso e o ritual acontece.
                w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 1, 0), 1);
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 2, 0), 120, 1.5, 3, 1.5, 0.15);
                w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 2f, 0.5f);
                Player dp = Bukkit.getPlayer(dono);
                if (dp != null) {
                    aplicar(r, dp, b);
                    plugin.stats().darXp(dp, Skill.ARCANO, 60);
                    plugin.titulos().registrar(dp, "rituais_circulo", 1);
                }
            }
            if (k > DURACAO - 20 && k % 3 == 0) circuloNoCeu(ceu, 9, giro[0], cor, Math.max(0, (DURACAO - k) / 20.0));
            if (k >= DURACAO) parar(bloco);
        }, 1L, 1L);
        cerimonias.put(bloco, tarefa);
    }

    private void parar(Location bloco) {
        BukkitTask t = cerimonias.remove(bloco);
        if (t != null) t.cancel();
    }

    /** O ritual quebrado devolve o golpe: as oferendas se perderam e espíritos saem do círculo. */
    private void contragolpe(Block b, Player p, Ritual r, String motivo) {
        parar(b.getLocation());
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.SQUID_INK, c, 80, 2, 1, 2, 0.1);
        w.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.7f);
        for (int i = 0; i < 4; i++) {
            w.spawn(c.clone().add(ThreadLocalRandom.current().nextDouble(-2, 2), 1, ThreadLocalRandom.current().nextDouble(-2, 2)), Vex.class, v -> {
                v.setPersistent(false);
                v.setLimitedLifetimeTicks(20 * 30);
            });
        }
        if (p != null) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 160, 0));
            p.sendMessage(Component.text("✖ O ritual " + r.nome() + " se quebrou (" + motivo + "): as oferendas se perderam e o círculo devolveu o golpe.",
                    NamedTextColor.DARK_RED, TextDecoration.BOLD));
        }
    }

    /** O traço da estrela aceso até o segmento {@code segmentos}, com as velas soltando chamas. */
    private static void acenderTraco(Block centro, int segmentos, Color cor) {
        World w = centro.getWorld();
        Particle.DustOptions po = new Particle.DustOptions(cor, 1.1f);
        for (int s = 0; s < segmentos && s < SEGMENTOS.size(); s++) {
            for (int[] c : SEGMENTOS.get(s)) w.spawnParticle(Particle.DUST, centro.getLocation().add(c[0] + 0.5, 1.15, c[1] + 0.5), 1, 0.2, 0, 0.2, 0, po, true);
        }
        for (int i = 0; i < Math.min(5, segmentos); i++) {
            int[] p = PONTAS[i];
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, centro.getLocation().add(p[0] + 0.5, 1.6, p[1] + 0.5), 2, 0.05, 0.25, 0.05, 0.01, null, true);
        }
    }

    /**
     * O círculo no céu: anel externo, anel interno, a estrela de 5 pontas (girando) e um anelzinho
     * em cada ponta. {@code forca} 0–1 apaga ou acende.
     */
    static void circuloNoCeu(Location c, double raio, double giro, Color cor, double forca) {
        if (forca <= 0.02) return;
        World w = c.getWorld();
        Particle.DustOptions po = new Particle.DustOptions(cor, (float) (1.2 + forca));
        Particle.DustOptions fino = new Particle.DustOptions(Color.WHITE, 0.9f);
        int n = (int) (raio * 9);
        for (int i = 0; i < n; i++) {
            double a = giro + i * Math.PI * 2 / n;
            w.spawnParticle(Particle.DUST, c.clone().add(Math.cos(a) * raio, 0, Math.sin(a) * raio), 1, 0, 0, 0, 0, po, true);
            if (i % 2 == 0) w.spawnParticle(Particle.DUST, c.clone().add(Math.cos(-a) * raio * 0.82, 0, Math.sin(-a) * raio * 0.82), 1, 0, 0, 0, 0, fino, true);
        }
        double[][] pts = new double[5][];
        for (int k = 0; k < 5; k++) {
            double a = giro + k * Math.PI * 2 / 5 - Math.PI / 2;
            pts[k] = new double[]{Math.cos(a) * raio * 0.82, Math.sin(a) * raio * 0.82};
        }
        for (int k = 0; k < 5; k++) {
            double[] a = pts[k], b = pts[(k + 2) % 5];
            int m = (int) (raio * 3);
            for (int i = 0; i <= m; i++) {
                double f = i / (double) m;
                w.spawnParticle(Particle.DUST, c.clone().add(a[0] + (b[0] - a[0]) * f, 0, a[1] + (b[1] - a[1]) * f), 1, 0, 0, 0, 0, po, true);
            }
            for (int i = 0; i < 10; i++) {
                double an = i * Math.PI / 5;
                w.spawnParticle(Particle.DUST, c.clone().add(a[0] + Math.cos(an) * 0.9, 0, a[1] + Math.sin(an) * 0.9), 1, 0, 0, 0, 0, fino, true);
            }
        }
        if (ThreadLocalRandom.current().nextInt(3) == 0) w.spawnParticle(Particle.ENCHANT, c, 30, raio * 0.5, 0.2, raio * 0.5, 1, null, true);
    }

    private static void feixe(Location chao, Location ceu, Color cor) {
        World w = chao.getWorld();
        Particle.DustOptions po = new Particle.DustOptions(cor, 1.6f);
        for (double y = 0; y <= ceu.getY() - chao.getY(); y += 0.6) {
            w.spawnParticle(Particle.DUST, chao.clone().add(0, y, 0), 1, 0.08, 0, 0.08, 0, po, true);
        }
        w.spawnParticle(Particle.END_ROD, chao.clone().add(0, ThreadLocalRandom.current().nextDouble(0, 16), 0), 2, 0.1, 0.4, 0.1, 0.02, null, true);
    }

    // =====================================================================
    //  O que cada ritual faz
    // =====================================================================

    private void aplicar(Ritual r, Player p, Block b) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        World w = b.getWorld();
        switch (r.id()) {
            case "pacto_forca" -> pactuar(p, Pacto.FORCA);
            case "pacto_sombra" -> pactuar(p, Pacto.SOMBRA);
            case "pacto_fortuna" -> pactuar(p, Pacto.FORTUNA);
            case "tempestade" -> {
                w.setStorm(true);
                w.setThundering(true);
                w.setWeatherDuration(6000);
                w.setThunderDuration(6000);
                tempestades.put(b.getLocation(), System.currentTimeMillis() + 120_000);
                avisar(c, Component.text("⚡ A Tempestade Arcana começou: raios vão cair nos monstros perto do círculo por 2 minutos.", ROXO_TXT));
            }
            case "fenda" -> abrirFenda(b, p);
            case "corcel" -> {
                SkeletonHorse cavalo = w.spawn(c.clone().add(0, 0.2, 0), SkeletonHorse.class, h -> {
                    h.setTamed(true);
                    h.setOwner(p);
                    h.setAdult();
                    h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                    h.customName(Component.text("Corcel Espectral de " + p.getName(), ROXO_TXT));
                    h.setPersistent(true);
                    h.setRemoveWhenFarAway(false);
                });
                String erro = plugin.companheiros().vincular(p, cavalo);
                if (erro != null) p.sendMessage(Component.text("⛤ O Corcel Espectral é seu, mas não virou companheiro: " + erro, NamedTextColor.GRAY));
                w.spawnParticle(Particle.SOUL, c.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.05);
            }
            case "espiritos" -> {
                for (int i = 0; i < 3; i++) {
                    Vex v = w.spawn(c.clone().add(ThreadLocalRandom.current().nextDouble(-1.5, 1.5), 2, ThreadLocalRandom.current().nextDouble(-1.5, 1.5)), Vex.class, x -> {
                        x.setPersistent(false);
                        x.setLimitedLifetimeTicks(20 * 180);
                        x.customName(Component.text("Espírito de " + p.getName(), NamedTextColor.AQUA));
                        x.setGlowing(true);
                        x.getPersistentDataContainer().set(K_ALIADO, PersistentDataType.STRING, p.getUniqueId().toString());
                        if (x.getEquipment() != null) x.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    });
                    aliados.put(v.getUniqueId(), p.getUniqueId());
                }
                p.sendMessage(Component.text("⛤ 3 espíritos lutam com você por 3 minutos.", NamedTextColor.AQUA));
            }
            case "eclipse" -> plugin.ceu().comecarEclipse(w);
            case "lua_sangue" -> plugin.ceu().forcarLuaDeSangue();
            case "banimento" -> {
                int n = 0;
                for (Entity e : w.getNearbyEntities(c, 32, 16, 32)) {
                    if (!(e instanceof Enemy) || !(e instanceof LivingEntity v) || e instanceof Boss || e.customName() != null) continue;
                    if (Guardioes.tipo(e) != null || Companheiros.eh(e) || e.getPersistentDataContainer().has(K_ALIADO)) continue;
                    v.getWorld().spawnParticle(Particle.SOUL, v.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.03);
                    v.remove();
                    n++;
                }
                banidas.put(b.getLocation(), System.currentTimeMillis() + 10 * 60_000L);
                avisar(c, Component.text("⛤ Banimento: " + n + " monstro(s) se desfizeram. Nenhum nasce perto do círculo por 10 minutos.", OURO_TXT));
            }
            case "revelacao" -> {
                for (Entity e : w.getNearbyEntities(c, 96, 48, 96)) {
                    if (e instanceof LivingEntity v && v != p) v.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60, 0, false, false));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 300, 0, false, true));
                var pista = plugin.estruturas().paraSonhar(p, p.getLocation(), Set.of());
                if (pista == null) p.sendMessage(Component.text("⛤ As estrelas não mostram nenhum lugar novo por perto.", OURO_TXT));
                else {
                    double dx = pista.x() - p.getLocation().getX(), dz = pista.z() - p.getLocation().getZ();
                    p.sendMessage(Component.text("⛤ As estrelas apontam: " + pista.nome() + ", a uns " + (int) Math.sqrt(dx * dx + dz * dz)
                            + " blocos para o " + rumo(dx, dz) + ".", OURO_TXT));
                }
            }
            default -> { }
        }
    }

    private static final TextColor ROXO_TXT = TextColor.color(0x9B59FF), OURO_TXT = TextColor.color(0xFFD27A);

    private static String rumo(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] r = {"norte", "nordeste", "leste", "sudeste", "sul", "sudoeste", "oeste", "noroeste"};
        return r[(int) Math.floorMod(Math.round(a / 45), 8)];
    }

    private static void avisar(Location c, Component texto) {
        for (Player p : c.getWorld().getPlayers()) if (p.getLocation().distanceSquared(c) < 96 * 96) p.sendMessage(texto);
    }

    // ---------- Pactos ----------

    void pactuar(Player p, Pacto pacto) {
        p.getPersistentDataContainer().set(K_PACTO, PersistentDataType.STRING, pacto.name());
        p.getPersistentDataContainer().set(K_PACTO_ATE, PersistentDataType.LONG, System.currentTimeMillis() + 60 * 60_000L);
        aplicarPacto(p);
        p.showTitle(Title.title(Component.text("⛤ Pacto selado", COR, TextDecoration.BOLD),
                Component.text(switch (pacto) {
                    case FORCA -> "Força: +25% de dano, 2 corações a menos";
                    case SOMBRA -> "Sombra: no escuro, invisível e veloz; ao sol, fraco";
                    case FORTUNA -> "Fortuna: sorte e pressa, mas sempre com fome";
                }, NamedTextColor.GRAY), Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(600))));
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.6f);
    }

    /** Põe (ou tira, quando acaba) os efeitos do pacto. A cada 2 s. */
    private void aplicarPacto(Player p) {
        String s = p.getPersistentDataContainer().get(K_PACTO, PersistentDataType.STRING);
        long ate = p.getPersistentDataContainer().getOrDefault(K_PACTO_ATE, PersistentDataType.LONG, 0L);
        boolean vale = s != null && ate > System.currentTimeMillis();
        Pacto pacto = null;
        if (vale) try { pacto = Pacto.valueOf(s); } catch (IllegalArgumentException x) { vale = false; }
        AttributeInstance dano = p.getAttribute(Attribute.ATTACK_DAMAGE), vida = p.getAttribute(Attribute.MAX_HEALTH);
        boolean forca = vale && pacto == Pacto.FORCA;
        if (dano != null) {
            if (forca && dano.getModifier(M_PACTO_DANO) == null) {
                dano.addTransientModifier(new AttributeModifier(M_PACTO_DANO, 0.25, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
            } else if (!forca && dano.getModifier(M_PACTO_DANO) != null) dano.removeModifier(M_PACTO_DANO);
        }
        if (vida != null) {
            if (forca && vida.getModifier(M_PACTO_VIDA) == null) {
                vida.addTransientModifier(new AttributeModifier(M_PACTO_VIDA, -4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
                if (p.getHealth() > vida.getValue()) p.setHealth(vida.getValue());
            } else if (!forca && vida.getModifier(M_PACTO_VIDA) != null) vida.removeModifier(M_PACTO_VIDA);
        }
        if (!vale) {
            if (s != null) {
                p.getPersistentDataContainer().remove(K_PACTO);
                p.getPersistentDataContainer().remove(K_PACTO_ATE);
                p.sendMessage(Component.text("⛤ O pacto acabou.", COR));
            }
            return;
        }
        switch (pacto) {
            case SOMBRA -> {
                boolean sol = !Eventos.noite(p.getWorld()) && p.getLocation().getBlock().getLightFromSky() >= 14
                        && p.getWorld().getEnvironment() == World.Environment.NORMAL;
                if (sol) p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false));
                else if (p.getLocation().getBlock().getLightLevel() <= 8 || Eventos.noite(p.getWorld())) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false));
                }
            }
            case FORTUNA -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 60, 1, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 60, 1, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, false));
            }
            default -> { }
        }
    }

    // ---------- Fenda do Vazio ----------

    private final class Fenda {
        final Location centro;
        final UUID dono;
        int onda;
        final Set<UUID> vivos = new HashSet<>();
        final long fechaEm = System.currentTimeMillis() + 4 * 60_000L;

        Fenda(Location centro, UUID dono) { this.centro = centro; this.dono = dono; }
    }

    private void abrirFenda(Block b, Player p) {
        fenda = new Fenda(b.getLocation().add(0.5, 4, 0.5), p.getUniqueId());
        avisar(fenda.centro, Component.text("⛤ Uma Fenda do Vazio se abriu sobre o círculo! Três ondas e o Guardião da Fenda.", ROXO_TXT, TextDecoration.BOLD));
        proximaOnda();
    }

    private void proximaOnda() {
        Fenda f = fenda;
        f.onda++;
        World w = f.centro.getWorld();
        w.playSound(f.centro, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.8f);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        java.util.function.Function<Class<? extends LivingEntity>, LivingEntity> nascer = tipo -> {
            Location l = f.centro.clone().add(r.nextDouble(-2, 2), r.nextDouble(-1, 1), r.nextDouble(-2, 2));
            plugin.circulosMagicos().chaoEm(l.clone().subtract(0, 3.9, 0), 0.9, 20, ROXO, Color.BLACK, 5, 2, 0);
            LivingEntity e = w.spawn(l, tipo, x -> {
                x.getPersistentDataContainer().set(K_FENDA, PersistentDataType.BYTE, (byte) 1);
                if (x instanceof Mob m) { m.setPersistent(false); m.setRemoveWhenFarAway(true); }
            });
            f.vivos.add(e.getUniqueId());
            return e;
        };
        Player alvo = Bukkit.getPlayer(f.dono);
        switch (f.onda) {
            case 1 -> { for (int i = 0; i < 6; i++) nascer.apply(org.bukkit.entity.Endermite.class); for (int i = 0; i < 2; i++) nascer.apply(Vex.class); }
            case 2 -> { for (int i = 0; i < 3; i++) { LivingEntity e = nascer.apply(Enderman.class); if (alvo != null && e instanceof Enderman en) en.setTarget(alvo); } for (int i = 0; i < 3; i++) nascer.apply(Vex.class); }
            case 3 -> { for (int i = 0; i < 2; i++) nascer.apply(org.bukkit.entity.Phantom.class); for (int i = 0; i < 2; i++) { LivingEntity e = nascer.apply(Enderman.class); if (alvo != null && e instanceof Enderman en) en.setTarget(alvo); } }
            default -> {
                LivingEntity g = nascer.apply(Enderman.class);
                g.customName(Component.text("☠ Guardião da Fenda", ROXO_TXT, TextDecoration.BOLD));
                AttributeInstance v = g.getAttribute(Attribute.MAX_HEALTH);
                if (v != null) { v.setBaseValue(160); g.setHealth(160); }
                AttributeInstance s = g.getAttribute(Attribute.SCALE);
                if (s != null) s.setBaseValue(1.6);
                AttributeInstance d = g.getAttribute(Attribute.ATTACK_DAMAGE);
                if (d != null) d.setBaseValue(d.getBaseValue() * 1.6);
                g.setGlowing(true);
                if (alvo != null && g instanceof Enderman en) en.setTarget(alvo);
            }
        }
        avisar(f.centro, Component.text(f.onda <= 3 ? "⛤ Onda " + f.onda + " de 3 sai da fenda!" : "⛤ O Guardião da Fenda atravessou!", ROXO_TXT));
    }

    @EventHandler
    public void aoMorrerNaFenda(EntityDeathEvent e) {
        if (fenda == null || !fenda.vivos.remove(e.getEntity().getUniqueId())) return;
        e.getDrops().clear();
        e.setDroppedExp(e.getDroppedExp() + 3);
        if (!fenda.vivos.isEmpty()) return;
        if (fenda.onda < 4) { Bukkit.getScheduler().runTaskLater(plugin, () -> { if (fenda != null) proximaOnda(); }, 60L); return; }
        fecharFenda(true);
    }

    private void fecharFenda(boolean vencida) {
        Fenda f = fenda;
        fenda = null;
        if (f == null) return;
        World w = f.centro.getWorld();
        for (UUID id : f.vivos) { Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); }
        w.spawnParticle(Particle.REVERSE_PORTAL, f.centro, 200, 1, 1, 1, 0.3);
        w.playSound(f.centro, Sound.BLOCK_END_PORTAL_FRAME_FILL, 2f, 0.5f);
        if (!vencida) { avisar(f.centro, Component.text("⛤ A Fenda do Vazio se fechou sozinha, sem tesouro.", NamedTextColor.GRAY)); return; }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location chao = f.centro.clone().subtract(0, 3, 0);
        List<ItemStack> tesouro = new ArrayList<>(List.of(new ItemStack(Material.ENDER_PEARL, 4 + r.nextInt(5)), new ItemStack(Material.ECHO_SHARD, 1 + r.nextInt(2)),
                new ItemStack(Material.SHULKER_SHELL, 1 + r.nextInt(2)), Raro.FRAGMENTO_DE_FORJA.criar(2 + r.nextInt(3))));
        if (r.nextDouble() < 0.25) tesouro.add(Raro.ALMA_DESPERTA.criar());
        if (r.nextDouble() < 0.10) tesouro.add(new ItemStack(Material.ELYTRA));
        for (ItemStack s : tesouro) w.dropItem(chao, s, i -> i.setOwner(f.dono));
        avisar(f.centro, Component.text("⛤ A Fenda do Vazio foi selada! O tesouro caiu no círculo.", ROXO_TXT, TextDecoration.BOLD));
        Player p = Bukkit.getPlayer(f.dono);
        if (p != null) plugin.titulos().registrar(p, "fendas_seladas", 1);
    }

    // ---------- Proteções e alvos ----------

    @EventHandler(ignoreCancelled = true)
    public void aoNascer(CreatureSpawnEvent e) {
        if (banidas.isEmpty() || !(e.getEntity() instanceof Enemy)) return;
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) return;
        long agora = System.currentTimeMillis();
        for (Map.Entry<Location, Long> b : banidas.entrySet()) {
            if (b.getValue() > agora && b.getKey().getWorld() == e.getLocation().getWorld() && b.getKey().distanceSquared(e.getLocation()) < 48 * 48) {
                e.setCancelled(true);
                return;
            }
        }
    }

    /** Os espíritos aliados só miram monstros (nunca jogadores, guardiões ou companheiros). */
    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetEvent e) {
        if (!aliados.containsKey(e.getEntity().getUniqueId()) || e.getTarget() == null) return;
        if (!(e.getTarget() instanceof Enemy) || aliados.containsKey(e.getTarget().getUniqueId()) || Guardioes.tipo(e.getTarget()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void aoAliadoFerir(EntityDamageByEntityEvent e) {
        if (aliados.containsKey(e.getDamager().getUniqueId()) && !(e.getEntity() instanceof Enemy)) e.setCancelled(true);
    }

    @Override
    protected void aoTick() {
        voltas++;
        if (voltas % 8 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getPersistentDataContainer().has(K_PACTO)) aplicarPacto(p);
            long agora = System.currentTimeMillis();
            banidas.values().removeIf(t -> t <= agora);
            tempestades.entrySet().removeIf(t -> t.getValue() <= agora);
            if (fenda != null && agora > fenda.fechaEm) fecharFenda(false);
            // Os espíritos aliados seguem o dono e procuram monstros perto dele.
            aliados.entrySet().removeIf(en -> !(Bukkit.getEntity(en.getKey()) instanceof Vex v) || !v.isValid());
            for (Map.Entry<UUID, UUID> en : aliados.entrySet()) {
                if (!(Bukkit.getEntity(en.getKey()) instanceof Vex v)) continue;
                Player dono = Bukkit.getPlayer(en.getValue());
                if (dono == null || dono.getWorld() != v.getWorld()) { v.remove(); continue; }
                v.setBound(dono.getLocation().add(0, 2, 0));
                if (v.getTarget() == null || !v.getTarget().isValid()) {
                    LivingEntity alvo = null;
                    double d = 16 * 16;
                    for (Entity e : v.getWorld().getNearbyEntities(dono.getLocation(), 16, 8, 16)) {
                        if (!(e instanceof Enemy) || !(e instanceof LivingEntity l) || aliados.containsKey(e.getUniqueId()) || Guardioes.tipo(e) != null) continue;
                        double dd = e.getLocation().distanceSquared(dono.getLocation());
                        if (dd < d) { d = dd; alvo = l; }
                    }
                    v.setTarget(alvo);
                    if (alvo == null && v.getLocation().distanceSquared(dono.getLocation()) > 12 * 12) v.teleport(dono.getLocation().add(0, 2, 0));
                }
            }
        }
        // A cada segundo: raios da Tempestade Arcana e a fenda girando.
        if (voltas % 4 == 0) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (Location c : tempestades.keySet()) {
                if (!c.isChunkLoaded() || r.nextBoolean()) continue;
                List<LivingEntity> monstros = new ArrayList<>();
                for (Entity e : c.getWorld().getNearbyEntities(c, 48, 24, 48)) {
                    if (e instanceof Enemy && e instanceof LivingEntity l && !(e instanceof Boss) && Guardioes.tipo(e) == null && !aliados.containsKey(e.getUniqueId())) monstros.add(l);
                }
                if (monstros.isEmpty()) continue;
                LivingEntity alvo = monstros.get(r.nextInt(monstros.size()));
                alvo.getWorld().strikeLightningEffect(alvo.getLocation());
                alvo.damage(12);
                alvo.setFireTicks(60);
            }
            if (fenda != null && fenda.centro.isChunkLoaded()) {
                circuloNoCeu(fenda.centro, 2.5, voltas * 0.15, ROXO, 1);
                fenda.centro.getWorld().spawnParticle(Particle.PORTAL, fenda.centro, 40, 0.6, 0.6, 0.6, 0.6);
            }
        }
        // Os círculos parados desenham um anel fraco no céu quando alguém está perto.
        if (voltas % 6 == 0) {
            for (Location l : carregadas()) {
                if (!l.isChunkLoaded() || cerimonias.containsKey(l)) continue;
                boolean perto = false;
                for (Player p : l.getWorld().getPlayers()) if (p.getLocation().distanceSquared(l) < 40 * 40) { perto = true; break; }
                if (perto) circuloNoCeu(l.clone().add(0.5, 14, 0.5), 5, voltas * 0.01, VERMELHO, 0.3);
            }
        }
    }

    /** Servidor desligando: cerimônias param, a fenda fecha, os espíritos somem. */
    public void desligar() {
        for (BukkitTask t : cerimonias.values()) t.cancel();
        cerimonias.clear();
        if (fenda != null) fecharFenda(false);
        for (UUID id : aliados.keySet()) { Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); }
        aliados.clear();
    }

    // =====================================================================
    //  Grimório
    // =====================================================================

    public void abrirGrimorio(Player p) {
        Grimorio g = new Grimorio();
        g.inventario = Bukkit.createInventory(g, 27, Component.text("⛤ Grimório do Círculo"));
        g.inventario.setItem(4, item(Material.CANDLE, Component.text("⛤ O pentagrama", COR, TextDecoration.BOLD), List.of(
                Component.text("Velas acesas nas 5 pontas, a 5 blocos da obsidiana:", NamedTextColor.GRAY),
                Component.text("(0,-5) (5,-2) (3,4) (-3,4) (-5,-2).", NamedTextColor.GRAY),
                Component.text("O traço da estrela: pó de redstone, ou blocos no piso", NamedTextColor.GRAY),
                Component.text("(obsidiana, pedra-negra, tijolos do Nether, ametista...).", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Jogue as oferendas dentro do círculo e clique", NamedTextColor.YELLOW),
                Component.text("na obsidiana com o catalisador na mão.", NamedTextColor.YELLOW),
                Component.text("Agachado + mão vazia: confere o desenho.", NamedTextColor.DARK_GRAY))));
        int[] slots = {9, 10, 11, 12, 13, 14, 15, 16, 17, 21, 23};
        for (int i = 0; i < RITUAIS.size() && i < slots.length; i++) {
            Ritual r = RITUAIS.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(r.descricao(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Na mão: " + (r.qtdCatalisador() > 1 ? r.qtdCatalisador() + "x " : "") + ReceitaRitual.bonito(r.catalisador()).toLowerCase(), NamedTextColor.YELLOW));
            lore.add(Component.text("No círculo: " + r.listaOferendas(), NamedTextColor.AQUA));
            if (r.condicao() != null) lore.add(Component.text(r.condicao(), NamedTextColor.LIGHT_PURPLE));
            g.inventario.setItem(slots[i], item(r.icone(), Component.text(r.nome(), COR, TextDecoration.BOLD), lore));
        }
        p.openInventory(g.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.7f);
    }

    @EventHandler
    public void aoClicarGrimorio(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Grimorio) e.setCancelled(true);
    }

    @EventHandler
    public void aoArrastarGrimorio(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Grimorio) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    // =====================================================================
    //  Admin: desenhar o pentagrama pronto
    // =====================================================================

    /** Desenha o pentagrama em volta do bloco (obsidiana no centro, traço de pó de redstone, velas acesas). */
    public static void desenhar(Block centro) {
        centro.setType(Material.OBSIDIAN);
        for (int[] c : TRACO) {
            Block piso = centro.getRelative(c[0], 0, c[1]);
            if (!piso.getType().isSolid()) piso.setType(Material.POLISHED_BLACKSTONE);
            piso.getRelative(0, 1, 0).setType(Material.REDSTONE_WIRE);
        }
        for (int[] p : PONTAS) {
            Block piso = centro.getRelative(p[0], 0, p[1]);
            if (!piso.getType().isSolid()) piso.setType(Material.POLISHED_BLACKSTONE);
            Block vela = piso.getRelative(0, 1, 0);
            vela.setType(Material.BLACK_CANDLE);
            if (vela.getBlockData() instanceof org.bukkit.block.data.type.Candle cd) {
                cd.setCandles(3);
                cd.setLit(true);
                vela.setBlockData(cd);
            }
        }
    }
}
