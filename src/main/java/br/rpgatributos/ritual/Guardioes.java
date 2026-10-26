package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.domador.Companheiros;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 2.85–2.86: os guardiões e ajudantes do Altar da Convergência. O ritual dá um Selo; com ele na mão,
 * um clique num bloco põe o guardião ali, e esse lugar vira o posto dele. Cada guardião tem nível
 * de I a V (sobe no altar, com o Selo na mão, tributo e sacrifício). As Asas fazem ele voar e o
 * Cetro de Comando faz ele seguir você ou ficar de posto onde está.
 */
public final class Guardioes implements Listener {

    public static final TextColor COR = TextColor.color(0x7FD8FF);
    public static final int MAXIMO = 8;

    public enum Tipo {
        ESPIRITO("Espírito Guardião", "amethyst_cluster", Material.AMETHYST_CLUSTER, Material.AMETHYST_SHARD, 40,
                "Fica no posto e atira raios nos inimigos a até 12 blocos."),
        FERRO("Golem Guardião", "iron_block", Material.IRON_BLOCK, Material.IRON_INGOT, 200,
                "Golem de ferro com o dobro de vida que defende o posto e volta para ele."),
        FAZENDA("Golem de Fazenda", "hay_block", Material.HAY_BLOCK, Material.WHEAT, 80,
                "Colhe e replanta trigo, cenoura, batata, beterraba e fungo do Nether perto do posto e guarda num baú."),
        COBRE("Golem de Cobre", "copper_block", Material.COPPER_BLOCK, Material.COPPER_INGOT, 20,
                "Leva os itens do baú de cobre para os outros baús. Encerado: nunca vira estátua."),
        LENHADOR("Golem Lenhador", "oak_log", Material.OAK_LOG, Material.CHARCOAL, 80,
                "Derruba as árvores perto do posto, um tronco por vez, replanta a muda e guarda a madeira."),
        MINERADOR("Golem Minerador", "iron_pickaxe", Material.IRON_PICKAXE, Material.COAL, 80,
                "Fica na mina e tira os minérios à vista perto dele, um por vez, deixando pedra no lugar."),
        PASTOR("Pastor de Neve", "carved_pumpkin", Material.CARVED_PUMPKIN, Material.WHITE_WOOL, 40,
                "Tosquia as ovelhas, junta ovos, lã e penas do chão e traz de volta os animais que fogem do cercado."),
        CURANDEIRO("Espírito Curandeiro", "glistering_melon_slice", Material.GLISTERING_MELON_SLICE, Material.GLISTERING_MELON_SLICE, 40,
                "Cura jogadores e guardiões por perto a cada 2 segundos."),
        TORRE("Torre Arcana", "blaze_rod", Material.BLAZE_ROD, Material.BLAZE_POWDER, 80,
                "Fica parada e lança explosões de fogo arcano nos grupos de inimigos a até 14 blocos.");

        public final String nome, modelo, descricao;
        public final Material icone, tributo;
        public final double vida;

        Tipo(String nome, String modelo, Material icone, Material tributo, double vida, String descricao) {
            this.nome = nome;
            this.modelo = modelo;
            this.icone = icone;
            this.tributo = tributo;
            this.vida = vida;
            this.descricao = descricao;
        }

        /** Trabalha e guarda o que pega (tem mochila a partir do nível III). */
        boolean trabalhador() { return this == FAZENDA || this == LENHADOR || this == MINERADOR || this == PASTOR; }

        /** Sem IA do jogo: o plugin move (espíritos e a torre). */
        boolean semIa() { return this == ESPIRITO || this == CURANDEIRO || this == TORRE; }

        static Tipo de(String s) {
            try { return s == null ? null : valueOf(s); } catch (IllegalArgumentException e) { return null; }
        }
    }

    static final NamespacedKey K_SELO = new NamespacedKey("rpgatributos", "selo_guardiao");
    private static final NamespacedKey K_ASAS_ITEM = new NamespacedKey("rpgatributos", "asas_guardiao");
    private static final NamespacedKey K_CETRO = new NamespacedKey("rpgatributos", "cetro_guardiao");
    static final NamespacedKey K_TIPO = new NamespacedKey("rpgatributos", "guardiao");
    static final NamespacedKey K_DONO = new NamespacedKey("rpgatributos", "guardiao_dono");
    static final NamespacedKey K_POSTO = new NamespacedKey("rpgatributos", "guardiao_posto");
    static final NamespacedKey K_ASAS = new NamespacedKey("rpgatributos", "guardiao_asas");
    static final NamespacedKey K_NIVEL = new NamespacedKey("rpgatributos", "guardiao_nivel");
    static final NamespacedKey K_MODO = new NamespacedKey("rpgatributos", "guardiao_modo");
    static final NamespacedKey K_MOCHILA = new NamespacedKey("rpgatributos", "guardiao_mochila");

    private final RPGAtributos plugin;
    private final Trabalhos trabalhos;
    private final File arquivo;
    /** Dono → guardiões que ele tem (vivos, carregados ou não). */
    private final Map<UUID, Set<UUID>> porDono = new HashMap<>();
    /** Guardiões em chunks carregados. */
    private final Set<UUID> carregados = new HashSet<>();
    private final Map<UUID, Long> proximoTiro = new HashMap<>();
    private int ciclo;

    private static final class Mochila implements InventoryHolder {
        Inventory inventario;
        UUID guardiao;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Guardioes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.trabalhos = new Trabalhos(plugin, this);
        this.arquivo = new File(plugin.getDataFolder(), "guardioes.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String dono : y.getKeys(false)) {
            Set<UUID> s = new HashSet<>();
            for (String id : y.getStringList(dono)) {
                try { s.add(UUID.fromString(id)); } catch (IllegalArgumentException ignorado) { }
            }
            try { porDono.put(UUID.fromString(dono), s); } catch (IllegalArgumentException ignorado) { }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> en : porDono.entrySet()) {
            if (en.getValue().isEmpty()) continue;
            y.set(en.getKey().toString(), en.getValue().stream().map(UUID::toString).toList());
        }
        try { y.save(arquivo); } catch (IOException e) { plugin.getLogger().warning("Não deu para salvar guardioes.yml: " + e.getMessage()); }
    }

    public void iniciar() {
        for (World w : Bukkit.getWorlds()) for (Entity e : w.getEntities()) if (tipo(e) != null) carregados.add(e.getUniqueId());
    }

    // =====================================================================
    //  Itens: Selo, Asas, Cetro
    // =====================================================================

    public static ItemStack selo(Tipo t, int nivel, boolean asas) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            m.setItemModel(NamespacedKey.minecraft(t.modelo));
            m.setMaxStackSize(1);
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_SELO, PersistentDataType.STRING, t.name());
            m.getPersistentDataContainer().set(K_NIVEL, PersistentDataType.INTEGER, nivel);
            if (asas) m.getPersistentDataContainer().set(K_ASAS, PersistentDataType.BYTE, (byte) 1);
        });
        descreverSelo(i);
        return i;
    }

    private static void descreverSelo(ItemStack i) {
        Tipo t = tipoSelo(i);
        int nivel = nivelSelo(i);
        i.editMeta(m -> {
            boolean asas = m.getPersistentDataContainer().has(K_ASAS);
            m.itemName(Component.text("Selo: " + t.nome + " · Nv. " + ReceitaRitual.romano(nivel), COR));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(t.descricao, NamedTextColor.GRAY));
            lore.add(Component.text("Nível " + ReceitaRitual.romano(nivel) + ": " + (int) vidaMax(t, nivel) + " de vida"
                    + (t.trabalhador() && nivel >= 3 ? ", com mochila" : "") + (nivel >= 5 ? ", brilha e se cura mais rápido" : ""), NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(Component.text("Clique num bloco para pôr o guardião ali.", NamedTextColor.YELLOW));
            if (nivel < 5) lore.add(Component.text("Para subir de nível: Selo na mão no Altar da Convergência.", NamedTextColor.DARK_GRAY));
            if (asas) lore.add(Component.text("✦ Com asas", COR));
            if (m.getPersistentDataContainer().has(K_MOCHILA)) lore.add(Component.text("✦ Com itens na mochila", COR));
            if (m.getPersistentDataContainer().has(Equipamento.K_FERRAMENTA)) lore.add(Component.text("✦ Com a ferramenta dele", COR));
            if (m.getPersistentDataContainer().has(Equipamento.K_MUNICAO)) lore.add(Component.text("✦ Com munição", COR));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        });
    }

    /** O Selo um nível acima (mantém asas e mochila). */
    static ItemStack elevarSelo(ItemStack s, int nivel) {
        ItemStack r = s.clone();
        r.setAmount(1);
        r.editMeta(m -> m.getPersistentDataContainer().set(K_NIVEL, PersistentDataType.INTEGER, nivel));
        descreverSelo(r);
        return r;
    }

    public static ItemStack asas() {
        return itemSimples("Asas de Guardião", "elytra", K_ASAS_ITEM, 16,
                "Clique no seu guardião com elas para ele voar.", "(O Golem de Cobre não usa asas.)");
    }

    public static ItemStack cetro() {
        return itemSimples("Cetro de Comando", "blaze_rod", K_CETRO, 1,
                "Clique no seu guardião: ele passa a seguir você,", "ou, se já segue, fica de posto onde está.");
    }

    private static ItemStack itemSimples(String nome, String modelo, NamespacedKey chave, int pilha, String... linhas) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, COR));
            m.setItemModel(NamespacedKey.minecraft(modelo));
            m.setMaxStackSize(pilha);
            m.setEnchantmentGlintOverride(true);
            m.lore(Arrays.stream(linhas).map(l -> (Component) Component.text(l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(chave, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    static Tipo tipoSelo(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return Tipo.de(i.getItemMeta().getPersistentDataContainer().get(K_SELO, PersistentDataType.STRING));
    }

    static int nivelSelo(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return 1;
        return i.getItemMeta().getPersistentDataContainer().getOrDefault(K_NIVEL, PersistentDataType.INTEGER, 1);
    }

    private static boolean tem(ItemStack i, NamespacedKey k) {
        return i != null && !i.isEmpty() && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(k);
    }

    // =====================================================================
    //  Dados no guardião e números por nível
    // =====================================================================

    public static Tipo tipo(Entity e) {
        return e == null ? null : Tipo.de(e.getPersistentDataContainer().get(K_TIPO, PersistentDataType.STRING));
    }

    static UUID dono(Entity e) {
        String s = e.getPersistentDataContainer().get(K_DONO, PersistentDataType.STRING);
        try { return s == null ? null : UUID.fromString(s); } catch (IllegalArgumentException x) { return null; }
    }

    static boolean temAsas(Entity e) { return e.getPersistentDataContainer().has(K_ASAS); }

    static int nivel(Entity e) { return e.getPersistentDataContainer().getOrDefault(K_NIVEL, PersistentDataType.INTEGER, 1); }

    static boolean seguindo(Entity e) { return "SEGUIR".equals(e.getPersistentDataContainer().get(K_MODO, PersistentDataType.STRING)); }

    static Location posto(Entity e) {
        int[] p = e.getPersistentDataContainer().get(K_POSTO, PersistentDataType.INTEGER_ARRAY);
        if (p == null || p.length < 3) return e.getLocation();
        return new Location(e.getWorld(), p[0] + 0.5, p[1], p[2] + 0.5);
    }

    private static void definirPosto(Entity e, Location l) {
        e.getPersistentDataContainer().set(K_POSTO, PersistentDataType.INTEGER_ARRAY, new int[]{l.getBlockX(), l.getBlockY(), l.getBlockZ()});
    }

    static double vidaMax(Tipo t, int nivel) { return t.vida * (1 + 0.35 * (nivel - 1)); }

    static double forca(int nivel) { return 1 + 0.25 * (nivel - 1); }

    /** Até onde o guardião age (raio de defesa, de colheita, de cura...). */
    static double alcance(Tipo t, int nivel, boolean asas) {
        double base = switch (t) {
            case ESPIRITO -> asas ? 18 : 12;
            case TORRE -> 14;
            case CURANDEIRO -> 6;
            case MINERADOR -> 4;
            case FERRO -> 12;
            default -> 8;
        };
        return base + (t == Tipo.MINERADOR ? 1 : 2) * (nivel - 1);
    }

    /** Onde o guardião está "de guarda": o posto, ou o dono (seguindo). */
    Location centro(Entity g) {
        if (seguindo(g)) {
            UUID d = dono(g);
            Player p = d == null ? null : Bukkit.getPlayer(d);
            if (p != null && p.getWorld() == g.getWorld()) return p.getLocation();
        }
        return posto(g);
    }

    public int quantos(UUID dono) {
        Set<UUID> s = porDono.get(dono);
        return s == null ? 0 : s.size();
    }

    // =====================================================================
    //  Pôr e criar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsarSelo(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Tipo t = tipoSelo(e.getItem());
        if (t == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Block onde = e.getClickedBlock().getRelative(e.getBlockFace() == null ? BlockFace.UP : e.getBlockFace());
        if (!plugin.territorios().podeConstruir(p, onde)) {
            p.sendActionBar(Component.text("✖ Esse lugar é do território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        if (!onde.isPassable() || !onde.getRelative(BlockFace.UP).isPassable()) {
            p.sendActionBar(Component.text("✖ Não cabe aqui: precisa de 2 blocos livres.", NamedTextColor.RED));
            return;
        }
        if (quantos(p.getUniqueId()) >= MAXIMO && !br.rpgatributos.ModoTeste.ehTeste(p)) {
            p.sendMessage(Component.text("✖ Você já tem " + MAXIMO + " guardiões (o máximo). Guarde um antes (agachado, mão vazia).", NamedTextColor.RED));
            return;
        }
        ItemStack selo = e.getItem();
        PersistentDataContainer dados = selo.getItemMeta().getPersistentDataContainer();
        boolean asas = dados.has(K_ASAS);
        int nivel = nivelSelo(selo);
        byte[] mochila = dados.get(K_MOCHILA, PersistentDataType.BYTE_ARRAY);
        byte[] ferramenta = dados.get(Equipamento.K_FERRAMENTA, PersistentDataType.BYTE_ARRAY);
        String municao = dados.get(Equipamento.K_MUNICAO, PersistentDataType.STRING);
        int municaoQtd = dados.getOrDefault(Equipamento.K_MUNICAO_QTD, PersistentDataType.INTEGER, 0);
        selo.setAmount(selo.getAmount() - 1);
        LivingEntity g = criar(t, onde.getLocation().add(0.5, 0, 0.5), p, asas, nivel);
        if (mochila != null) g.getPersistentDataContainer().set(K_MOCHILA, PersistentDataType.BYTE_ARRAY, mochila);
        if (ferramenta != null) g.getPersistentDataContainer().set(Equipamento.K_FERRAMENTA, PersistentDataType.BYTE_ARRAY, ferramenta);
        if (municao != null) Equipamento.definirMunicao(g, Material.matchMaterial(municao), municaoQtd);
        if (t == Tipo.MINERADOR) Escavacao.comecar(g, p.getFacing());
        Location l = g.getLocation();
        l.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
        l.getWorld().playSound(l, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1f, 1.3f);
        p.sendMessage(Component.text("✦ " + t.nome + " (nível " + ReceitaRitual.romano(nivel) + ") está de guarda aqui ("
                + quantos(p.getUniqueId()) + "/" + MAXIMO + ").", COR));
        String precisa = Equipamento.ferramentaDoTipo(t);
        if (precisa != null && ferramenta == null) {
            p.sendMessage(Component.text("✦ Clique nele com uma " + precisa + " para ele " + (t == Tipo.FERRO ? "bater mais forte" : "trabalhar")
                    + " (quanto melhor, mais rápido).", NamedTextColor.GRAY));
        }
        if (t == Tipo.MINERADOR) {
            p.sendMessage(Component.text("✦ Ele vai cavar um túnel 3x3 para " + direcao(p.getFacing()) + ", até " + Escavacao.comprimentoMax(nivel)
                    + " blocos. Ponha cercas, tábuas e tochas no baú perto dele para as sustentações.", NamedTextColor.GRAY));
        }
        if (Equipamento.municoes(t) != null) {
            Material[] m = Equipamento.municoes(t);
            p.sendMessage(Component.text("✦ Munição: clique nele com " + ReceitaRitual.bonito(m[0]).toLowerCase() + " (ou "
                    + ReceitaRitual.bonito(m[1]).toLowerCase() + ", mais forte), ou deixe num baú perto. Sem munição ele fica fraco.", NamedTextColor.GRAY));
        }
    }

    private static String direcao(BlockFace f) {
        return switch (f) {
            case NORTH -> "o norte";
            case SOUTH -> "o sul";
            case EAST -> "o leste";
            default -> "o oeste";
        };
    }

    public LivingEntity criar(Tipo t, Location l, Player dono, boolean asas) {
        return criar(t, l, dono, asas, 1);
    }

    /** Cria o guardião (também usado pelos testes e pelo comando de admin). */
    public LivingEntity criar(Tipo t, Location l, Player dono, boolean asas, int nivel) {
        World w = l.getWorld();
        java.util.function.Consumer<LivingEntity> marcar = g -> {
            PersistentDataContainer pdc = g.getPersistentDataContainer();
            pdc.set(K_TIPO, PersistentDataType.STRING, t.name());
            if (dono != null) pdc.set(K_DONO, PersistentDataType.STRING, dono.getUniqueId().toString());
            definirPosto(g, l);
            pdc.set(K_NIVEL, PersistentDataType.INTEGER, nivel);
            if (asas) pdc.set(K_ASAS, PersistentDataType.BYTE, (byte) 1);
            g.setPersistent(true);
            g.setRemoveWhenFarAway(false);
        };
        LivingEntity g = switch (t) {
            case ESPIRITO, CURANDEIRO -> w.spawn(l.clone().add(0, 0.5, 0), Allay.class, a -> {
                marcar.accept(a);
                a.setAI(false);
                a.setCanDuplicate(false);
                base(a, Attribute.SCALE, t == Tipo.ESPIRITO ? 1.4 : 1.0);
            });
            case TORRE -> w.spawn(l.clone().add(0, 0.5, 0), Blaze.class, b -> {
                marcar.accept(b);
                b.setAI(false);
                base(b, Attribute.SCALE, 1.3);
            });
            case FERRO, FAZENDA, LENHADOR, MINERADOR -> w.spawn(l, IronGolem.class, gol -> {
                marcar.accept(gol);
                gol.setPlayerCreated(true);
                if (t != Tipo.FERRO) base(gol, Attribute.SCALE, t == Tipo.MINERADOR ? 0.65 : 0.7);
                else base(gol, Attribute.KNOCKBACK_RESISTANCE, 1);
            });
            case PASTOR -> w.spawn(l, Snowman.class, s -> {
                marcar.accept(s);
                s.setDerp(false);
            });
            case COBRE -> w.spawn(l, CopperGolem.class, c -> {
                marcar.accept(c);
                c.setOxidizing(CopperGolem.Oxidizing.waxed());
            });
        };
        aplicarNivel(g, t, nivel, dono == null ? null : dono.getName());
        if (t == Tipo.MINERADOR) Escavacao.comecar(g, BlockFace.EAST);
        carregados.add(g.getUniqueId());
        if (dono != null && !br.rpgatributos.ModoTeste.ehTeste(g)) { // na área de testes não conta no limite
            porDono.computeIfAbsent(dono.getUniqueId(), k -> new HashSet<>()).add(g.getUniqueId());
            salvar();
        }
        return g;
    }

    private static void aplicarNivel(LivingEntity g, Tipo t, int nivel, String nomeDono) {
        base(g, Attribute.MAX_HEALTH, vidaMax(t, nivel));
        g.setHealth(vidaMax(t, nivel));
        AttributeInstance dano = g.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dano != null) dano.setBaseValue(dano.getDefaultValue() * forca(nivel));
        if (t == Tipo.COBRE) {
            AttributeInstance vel = g.getAttribute(Attribute.MOVEMENT_SPEED);
            if (vel != null) vel.setBaseValue(vel.getDefaultValue() * (1 + 0.15 * (nivel - 1)));
        }
        g.setGlowing(nivel >= 5 || t.semIa() && t != Tipo.TORRE);
        if (nomeDono != null) g.getPersistentDataContainer().set(K_NOME_DONO, PersistentDataType.STRING, nomeDono);
        g.customName(nomeBase(g, t));
        g.setCustomNameVisible(true);
    }

    private static final NamespacedKey K_NOME_DONO = new NamespacedKey("rpgatributos", "guardiao_nome_dono");

    private static Component nomeBase(Entity g, Tipo t) {
        String dono = g.getPersistentDataContainer().get(K_NOME_DONO, PersistentDataType.STRING);
        return Component.text("⛨ " + (dono == null ? "" : dono + ": ") + t.nome + " · Nv. " + ReceitaRitual.romano(nivel(g)), COR);
    }

    /** O último aviso mostrado no nome de cada guardião (para só mexer no nome quando muda). */
    private final Map<UUID, String> ultimoEstado = new HashMap<>();

    /** Mostra no nome o que está faltando ou fazendo (ex.: "⚠ sem picareta", "túnel pronto"). */
    private void estado(LivingEntity g, Tipo t, String aviso) {
        String antes = ultimoEstado.get(g.getUniqueId());
        if (java.util.Objects.equals(antes, aviso) && ultimoEstado.containsKey(g.getUniqueId())) return;
        ultimoEstado.put(g.getUniqueId(), aviso);
        Component nome = nomeBase(g, t);
        if (aviso != null) nome = nome.append(Component.text(" · " + aviso, aviso.startsWith("⚠") ? NamedTextColor.RED : NamedTextColor.GRAY));
        g.customName(nome);
    }

    /** Aviso dos guardiões de defesa: munição e espada. */
    private static String avisoDefesa(LivingEntity g, Tipo t) {
        if (Equipamento.municoes(t) != null && Equipamento.municaoQtd(g) <= 0) return "⚠ sem munição (fraco)";
        if (t == Tipo.FERRO && Equipamento.ferramenta(g) == null) return "sem espada";
        return null;
    }

    /** A espada do Golem Guardião: dano a mais e gasto de durabilidade. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoGolemBater(EntityDamageByEntityEvent e) {
        if (tipo(e.getDamager()) != Tipo.FERRO) return;
        ItemStack espada = Equipamento.ferramenta(e.getDamager());
        if (espada == null) return;
        e.setDamage(e.getDamage() + Equipamento.bonusEspada(espada) * forca(nivel(e.getDamager())));
        if (Equipamento.gastar(e.getDamager())) {
            UUID d = dono(e.getDamager());
            Player p = d == null ? null : Bukkit.getPlayer(d);
            if (p != null) p.sendMessage(Component.text("⚒ A espada do Golem Guardião quebrou.", COR));
        }
    }

    /** Golem Guardião ferido: se conserta com lingotes de ferro do baú do posto (25 de vida cada). */
    private void consertar(IronGolem g) {
        double max = g.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (g.getHealth() > max * 0.7) return;
        if (Equipamento.tirar(g, posto(g), 8, m -> m == Material.IRON_INGOT, 1) == null) return;
        g.setHealth(Math.min(max, g.getHealth() + 25));
        g.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, g.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0);
        g.getWorld().playSound(g.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f);
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    // =====================================================================
    //  Clique no guardião: asas, cetro, guardar, mochila
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarGuardiao(PlayerInteractEntityEvent e) {
        Entity g = e.getRightClicked();
        Tipo t = tipo(g);
        if (t == null) return;
        if (e.getHand() != EquipmentSlot.HAND) { e.setCancelled(true); return; }
        Player p = e.getPlayer();
        UUID dono = dono(g);
        ItemStack mao = p.getInventory().getItemInMainHand();
        boolean meu = p.getUniqueId().equals(dono);
        if (tem(mao, K_ASAS_ITEM)) {
            e.setCancelled(true);
            if (!meu) { p.sendActionBar(Component.text("✖ Esse guardião não é seu.", NamedTextColor.RED)); return; }
            if (t == Tipo.COBRE) { p.sendActionBar(Component.text("✖ O Golem de Cobre não usa asas: ele anda de baú em baú.", NamedTextColor.RED)); return; }
            if (temAsas(g)) { p.sendActionBar(Component.text("✦ Ele já tem asas.", COR)); return; }
            mao.setAmount(mao.getAmount() - 1);
            g.getPersistentDataContainer().set(K_ASAS, PersistentDataType.BYTE, (byte) 1);
            g.getWorld().spawnParticle(Particle.CLOUD, g.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
            g.getWorld().playSound(g.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.2f);
            p.sendMessage(Component.text("✦ " + t.nome + " ganhou asas!", COR));
            return;
        }
        if (tem(mao, K_CETRO)) {
            e.setCancelled(true);
            if (!meu) { p.sendActionBar(Component.text("✖ Esse guardião não é seu.", NamedTextColor.RED)); return; }
            if (seguindo(g)) {
                g.getPersistentDataContainer().set(K_MODO, PersistentDataType.STRING, "POSTO");
                definirPosto(g, g.getLocation());
                if (t == Tipo.MINERADOR) Escavacao.comecar(g, p.getFacing());
                p.sendActionBar(Component.text("⛨ " + t.nome + " fica de posto aqui"
                        + (t == Tipo.MINERADOR ? " e cava para " + direcao(p.getFacing()) : "") + ".", COR));
            } else {
                g.getPersistentDataContainer().set(K_MODO, PersistentDataType.STRING, "SEGUIR");
                p.sendActionBar(Component.text("⛨ " + t.nome + " segue você.", COR));
            }
            g.getWorld().playSound(g.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, seguindo(g) ? 1.5f : 0.9f);
            return;
        }
        if (meu && Equipamento.serve(t, mao)) {
            // Entrega a ferramenta (a antiga volta para você).
            e.setCancelled(true);
            ItemStack antiga = Equipamento.ferramenta(g);
            ItemStack nova = mao.clone();
            nova.setAmount(1);
            mao.setAmount(mao.getAmount() - 1);
            Equipamento.definirFerramenta(g, nova);
            if (antiga != null) p.getInventory().addItem(antiga).values().forEach(r -> p.getWorld().dropItemNaturally(p.getLocation(), r));
            ultimoEstado.remove(g.getUniqueId());
            g.getWorld().playSound(g.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1.1f);
            p.sendMessage(Component.text("⚒ " + t.nome + " pegou: " + ReceitaRitual.bonito(nova.getType()) + Equipamento.durabilidade(nova)
                    + (antiga != null ? ". A antiga voltou para você." : "."), COR));
            return;
        }
        Material[] municoes = Equipamento.municoes(t);
        if (meu && municoes != null && !mao.isEmpty() && (mao.getType() == municoes[0] || mao.getType() == municoes[1])) {
            e.setCancelled(true);
            Material atual = Equipamento.municao(g);
            int qtd = Equipamento.municaoQtd(g);
            if (atual != null && qtd > 0 && atual != mao.getType()) {
                p.sendActionBar(Component.text("✖ Ele ainda tem " + qtd + " de " + ReceitaRitual.bonito(atual).toLowerCase() + ". Use até acabar.", NamedTextColor.RED));
                return;
            }
            Equipamento.definirMunicao(g, mao.getType(), qtd + mao.getAmount());
            p.sendActionBar(Component.text("✦ " + t.nome + ": " + (qtd + mao.getAmount()) + " de " + ReceitaRitual.bonito(mao.getType()).toLowerCase() + ".", COR));
            mao.setAmount(0);
            ultimoEstado.remove(g.getUniqueId());
            g.getWorld().playSound(g.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            return;
        }
        if (!mao.isEmpty()) return; // golem de ferro com lingote: cura do jogo
        e.setCancelled(true);
        if (meu && p.isSneaking()) {
            guardar((LivingEntity) g, t, p);
            return;
        }
        LivingEntity v = (LivingEntity) g;
        String quem = dono == null ? "ninguém" : Bukkit.getOfflinePlayer(dono).getName();
        StringBuilder painel = new StringBuilder("⛨ " + t.nome + " Nv. " + ReceitaRitual.romano(nivel(g)) + " de " + quem + " · ❤ "
                + (int) Math.ceil(v.getHealth()) + "/" + (int) v.getAttribute(Attribute.MAX_HEALTH).getValue());
        ItemStack ferramenta = Equipamento.ferramenta(g);
        if (Equipamento.ferramentaDoTipo(t) != null) {
            painel.append(" · ").append(ferramenta == null ? "sem " + Equipamento.ferramentaDoTipo(t)
                    : ReceitaRitual.bonito(ferramenta.getType()) + Equipamento.durabilidade(ferramenta));
        }
        if (municoes != null) {
            Material m = Equipamento.municao(g);
            painel.append(" · ").append(m == null || Equipamento.municaoQtd(g) <= 0 ? "sem munição"
                    : Equipamento.municaoQtd(g) + " " + ReceitaRitual.bonito(m).toLowerCase());
        }
        if (t == Tipo.MINERADOR) painel.append(" · túnel ").append(Escavacao.progresso(g)).append("/").append(Escavacao.comprimentoMax(nivel(g)));
        if (temAsas(g)) painel.append(" · com asas");
        if (seguindo(g)) painel.append(" · seguindo");
        p.sendActionBar(Component.text(painel.toString(), COR));
        if (meu && t.trabalhador() && nivel(g) >= 3) abrirMochila(p, g);
    }

    private void guardar(LivingEntity g, Tipo t, Player p) {
        ItemStack selo = selo(t, nivel(g), temAsas(g));
        PersistentDataContainer dados = g.getPersistentDataContainer();
        byte[] mochila = dados.get(K_MOCHILA, PersistentDataType.BYTE_ARRAY);
        byte[] ferramenta = dados.get(Equipamento.K_FERRAMENTA, PersistentDataType.BYTE_ARRAY);
        String municao = dados.get(Equipamento.K_MUNICAO, PersistentDataType.STRING);
        int municaoQtd = Equipamento.municaoQtd(g);
        selo.editMeta(m -> {
            PersistentDataContainer s = m.getPersistentDataContainer();
            if (mochila != null) s.set(K_MOCHILA, PersistentDataType.BYTE_ARRAY, mochila);
            if (ferramenta != null) s.set(Equipamento.K_FERRAMENTA, PersistentDataType.BYTE_ARRAY, ferramenta);
            if (municao != null && municaoQtd > 0) {
                s.set(Equipamento.K_MUNICAO, PersistentDataType.STRING, municao);
                s.set(Equipamento.K_MUNICAO_QTD, PersistentDataType.INTEGER, municaoQtd);
            }
        });
        descreverSelo(selo);
        esquecer(g);
        g.getWorld().spawnParticle(Particle.REVERSE_PORTAL, g.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
        g.getWorld().playSound(g.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.7f);
        g.remove();
        p.getInventory().addItem(selo).values().forEach(r -> p.getWorld().dropItemNaturally(p.getLocation(), r));
        p.sendMessage(Component.text("✦ " + t.nome + " voltou para o selo.", COR));
    }

    private void esquecer(Entity g) {
        carregados.remove(g.getUniqueId());
        trabalhos.esquecer(g.getUniqueId());
        proximoTiro.remove(g.getUniqueId());
        ultimoEstado.remove(g.getUniqueId());
        UUID d = dono(g);
        Set<UUID> s = d == null ? null : porDono.get(d);
        if (s != null && s.remove(g.getUniqueId())) salvar();
    }

    // ---------- Mochila (trabalhadores nível III+) ----------

    static ItemStack[] mochila(Entity g) {
        byte[] b = g.getPersistentDataContainer().get(K_MOCHILA, PersistentDataType.BYTE_ARRAY);
        ItemStack[] itens = b == null ? new ItemStack[0] : ItemStack.deserializeItemsFromBytes(b);
        return Arrays.copyOf(itens, 27);
    }

    static void guardarMochila(Entity g, ItemStack[] itens) {
        boolean vazia = Arrays.stream(itens).allMatch(i -> i == null || i.isEmpty());
        if (vazia) { g.getPersistentDataContainer().remove(K_MOCHILA); return; }
        ItemStack[] limpos = Arrays.stream(itens).map(i -> i == null ? ItemStack.empty() : i).toArray(ItemStack[]::new);
        g.getPersistentDataContainer().set(K_MOCHILA, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(limpos));
    }

    private void abrirMochila(Player p, Entity g) {
        Mochila m = new Mochila();
        m.guardiao = g.getUniqueId();
        m.inventario = Bukkit.createInventory(m, 27, Component.text("Mochila: " + tipo(g).nome));
        m.inventario.setContents(mochila(g));
        p.openInventory(m.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 0.8f, 1f);
    }

    @EventHandler
    public void aoFecharMochila(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Mochila m)) return;
        Entity g = Bukkit.getEntity(m.guardiao);
        if (g != null && g.isValid()) guardarMochila(g, e.getInventory().getContents());
        else for (ItemStack i : e.getInventory().getContents()) {
            if (i != null && !i.isEmpty()) e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), i);
        }
    }

    // =====================================================================
    //  Proteção
    // =====================================================================

    /** Os golens do jogo atacam qualquer inimigo; guardiões e aliados ficam de fora. */
    @EventHandler(ignoreCancelled = true)
    public void aoMirar(EntityTargetEvent e) {
        if (e.getTarget() == null) return;
        if (tipo(e.getTarget()) != null && tipo(e.getEntity()) != null) { e.setCancelled(true); return; }
        if (tipo(e.getEntity()) != null && !alvoValido(e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        if (tipo(e.getEntity()) == null) return;
        Entity fonte = e.getDamager();
        if (fonte instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) fonte = atirador;
        // Jogadores e outros guardiões não ferem os guardiões.
        if (fonte instanceof Player || tipo(fonte) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (tipo(e.getEntity()) == null) return;
        switch (e.getCause()) {
            case SUFFOCATION, FALL, CRAMMING, MELTING, DROWNING, FREEZE -> e.setCancelled(true);
            default -> { }
        }
    }

    /** Guardiões não pisoteiam a terra arada. */
    @EventHandler(ignoreCancelled = true)
    public void aoPisar(EntityInteractEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND && tipo(e.getEntity()) != null) e.setCancelled(true);
    }

    /** O Pastor de Neve não deixa rastro de neve. */
    @EventHandler(ignoreCancelled = true)
    public void aoFormarNeve(EntityBlockFormEvent e) {
        if (tipo(e.getEntity()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        Tipo t = tipo(e.getEntity());
        if (t == null) return;
        e.getDrops().clear();
        // A mochila, a ferramenta e a munição caem no chão.
        for (ItemStack i : mochila(e.getEntity())) if (i != null && !i.isEmpty()) e.getDrops().add(i);
        ItemStack ferramenta = Equipamento.ferramenta(e.getEntity());
        if (ferramenta != null) e.getDrops().add(ferramenta);
        Material municao = Equipamento.municao(e.getEntity());
        int qtd = Equipamento.municaoQtd(e.getEntity());
        while (municao != null && qtd > 0) {
            int n = Math.min(qtd, municao.getMaxStackSize());
            e.getDrops().add(new ItemStack(municao, n));
            qtd -= n;
        }
        e.setDroppedExp(0);
        UUID d = dono(e.getEntity());
        esquecer(e.getEntity());
        Player p = d == null ? null : Bukkit.getPlayer(d);
        if (p != null) p.sendMessage(Component.text("✖ Seu " + t.nome + " caiu em combate.", NamedTextColor.RED));
    }

    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) if (tipo(en) != null) carregados.add(en.getUniqueId());
    }

    @EventHandler
    public void aoDescarregarEntidades(EntitiesUnloadEvent e) {
        for (Entity en : e.getEntities()) carregados.remove(en.getUniqueId());
    }

    static boolean alvoValido(Entity e) {
        return e instanceof Enemy && e instanceof LivingEntity l && !l.isDead() && tipo(e) == null && !Companheiros.eh(e);
    }

    // =====================================================================
    //  A cada 2 ticks
    // =====================================================================

    public void tick() {
        ciclo++;
        for (UUID id : List.copyOf(carregados)) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity g) || !g.isValid()) continue;
            Tipo t = tipo(g);
            if (t == null) continue;
            boolean asas = temAsas(g);
            int nivel = nivel(g);
            if (seguindo(g) && seguir(g, t, asas)) {
                // Seguindo e longe do dono: só anda (os de combate lutam no caminho).
                if (t == Tipo.ESPIRITO) espirito(g, asas, nivel, false);
                else if (t == Tipo.TORRE) torre(g, nivel, false);
                else if (t == Tipo.CURANDEIRO && ciclo % 20 == 0) curar(g, nivel);
            } else {
                switch (t) {
                    case ESPIRITO -> espirito(g, asas, nivel, true);
                    case TORRE -> torre(g, nivel, true);
                    case CURANDEIRO -> { manterNoPosto(g, asas); if (ciclo % 20 == 0) curar(g, nivel); }
                    case FERRO -> { ferro((IronGolem) g, asas, nivel); if (ciclo % 50 == 0) consertar((IronGolem) g); }
                    case FAZENDA, LENHADOR, MINERADOR, PASTOR -> {
                        if (!seguindo(g)) {
                            String aviso = trabalhos.trabalhar((Mob) g, t, asas, nivel, ciclo);
                            if (aviso != null || ciclo % 10 == 0) estado(g, t, aviso);
                        }
                    }
                    case COBRE -> { }
                }
            }
            if (!t.trabalhador() && ciclo % 10 == 0) estado(g, t, avisoDefesa(g, t));
            if (asas && ciclo % 3 == 0) asasVisuais(g);
            double max = g.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (ciclo % 20 == 0 && g.getHealth() < max && g.getNoDamageTicks() == 0) {
                g.setHealth(Math.min(max, g.getHealth() + (nivel >= 5 ? 2 : 1)));
            }
        }
    }

    /**
     * Seguindo: vai atrás do dono. @return true se ainda está a caminho (longe do dono).
     */
    private boolean seguir(LivingEntity g, Tipo t, boolean asas) {
        UUID d = dono(g);
        Player p = d == null ? null : Bukkit.getPlayer(d);
        if (p == null || p.getWorld() != g.getWorld() || p.isDead()) return false;
        Location alvo = p.getLocation();
        double dist = g.getLocation().distance(alvo);
        if (dist > 40) {
            g.teleport(alvo.clone().add(alvo.getDirection().setY(0).normalize().multiply(-2)));
            return true;
        }
        if (dist < (t.semIa() ? 3 : 4)) {
            if (!t.semIa()) g.setGravity(true);
            return false;
        }
        if (t.semIa()) {
            passo(g, alvo.clone().add(0, 2, 0), 0.6);
        } else if (asas) {
            voarPara(g, alvo.clone().add(0, 1, 0), 0.5);
        } else if (ciclo % 10 == 0 && g instanceof Mob m) {
            m.getPathfinder().moveTo(alvo, 1.2);
        }
        return true;
    }

    /** Um passo de quem não tem IA (teleporte curto, suave no cliente). */
    static void passo(LivingEntity g, Location destino, double velocidade) {
        Location aqui = g.getLocation();
        Vector v = destino.toVector().subtract(aqui.toVector());
        if (v.lengthSquared() < 0.01) return;
        if (v.length() > velocidade) v.normalize().multiply(velocidade);
        Location prox = aqui.clone().add(v);
        if (!prox.getBlock().isPassable()) return;
        prox.setDirection(v);
        g.teleport(prox);
    }

    static void voarPara(LivingEntity g, Location destino, double velocidade) {
        Vector v = destino.toVector().subtract(g.getLocation().toVector());
        g.setGravity(false);
        g.setVelocity(v.length() > velocidade ? v.normalize().multiply(velocidade) : v);
    }

    private void manterNoPosto(LivingEntity g, boolean asas) {
        Location posto = posto(g).add(0, asas ? 2.5 : 0.5, 0);
        if (g.getLocation().distanceSquared(posto) > 1) passo(g, posto, 0.6);
    }

    private LivingEntity inimigoPerto(Location c, double raio) {
        LivingEntity melhor = null;
        double d = raio * raio;
        for (Entity e : c.getWorld().getNearbyEntities(c, raio, raio, raio)) {
            if (!alvoValido(e)) continue;
            double dd = e.getLocation().distanceSquared(c);
            if (dd < d) { d = dd; melhor = (LivingEntity) e; }
        }
        return melhor;
    }

    /** Espírito: atira raios; com asas, voa até o inimigo e volta para o posto. */
    private void espirito(LivingEntity g, boolean asas, int nivel, boolean noPosto) {
        Location centro = centro(g).add(0, asas ? 2.5 : 0.5, 0);
        LivingEntity alvo = inimigoPerto(centro, alcance(Tipo.ESPIRITO, nivel, asas));
        if (noPosto) {
            if (asas) {
                Location destino = alvo == null ? centro : alvo.getLocation().add(0, 2.5, 0);
                if (alvo == null || g.getLocation().distanceSquared(destino) > 16) passo(g, destino, 0.6);
            } else if (g.getLocation().distanceSquared(centro) > 1) {
                g.teleport(centro);
            }
        }
        boolean semMunicao = Equipamento.municaoQtd(g) <= 0;
        long espera = (1500 - 150 * (nivel - 1)) * (semMunicao ? 2L : 1L);
        if (alvo == null || !podeAtirar(g, espera) || !g.hasLineOfSight(alvo)) return;
        int modo = Equipamento.disparar(g, Tipo.ESPIRITO, posto(g));
        double mult = modo == 0 ? 0.5 : modo == 2 ? 1.6 : 1.0;
        Location olho = g.getEyeLocation();
        Location mira = alvo.getLocation().add(0, alvo.getHeight() / 2, 0);
        raio(olho, mira, modo == 2 ? Color.fromRGB(0x1DE9B6) : Color.fromRGB(0x7FD8FF));
        g.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mira, modo == 2 ? 25 : 12, 0.2, 0.3, 0.2, 0.1);
        g.getWorld().playSound(olho, Sound.ENTITY_ALLAY_ITEM_THROWN, 0.8f, 0.6f);
        g.getWorld().playSound(mira, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.4f);
        alvo.damage((asas ? 7 : 6) * forca(nivel) * mult, g);
    }

    /** Torre Arcana: explosão de fogo arcano em volta do inimigo mais perto (sem quebrar blocos). */
    private void torre(LivingEntity g, int nivel, boolean noPosto) {
        if (noPosto) {
            Location posto = posto(g).add(0, temAsas(g) ? 2.5 : 0.5, 0);
            if (g.getLocation().distanceSquared(posto) > 1) passo(g, posto, 0.6);
        }
        if (ciclo % 5 != 0) return;
        LivingEntity alvo = inimigoPerto(g.getLocation(), alcance(Tipo.TORRE, nivel, false));
        if (alvo == null) return;
        g.teleport(g.getLocation().setDirection(alvo.getLocation().toVector().subtract(g.getLocation().toVector())));
        long espera = (3000 - 300 * (nivel - 1)) * (Equipamento.municaoQtd(g) <= 0 ? 2L : 1L);
        if (!podeAtirar(g, espera) || !g.hasLineOfSight(alvo)) return;
        int modo = Equipamento.disparar(g, Tipo.TORRE, posto(g));
        double mult = modo == 0 ? 0.5 : modo == 2 ? 1.5 : 1.0, area = modo == 2 ? 4.5 : modo == 0 ? 2.5 : 3;
        Location mira = alvo.getLocation().add(0, 0.5, 0);
        raio(g.getEyeLocation(), mira, Color.fromRGB(0xFF7A2B));
        World w = g.getWorld();
        w.spawnParticle(Particle.FLAME, mira, (int) (20 * area), area / 2, 0.5, area / 2, 0.05);
        w.spawnParticle(Particle.EXPLOSION, mira, modo == 2 ? 5 : 2, 0.5, 0.2, 0.5, 0);
        w.playSound(mira, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.7f);
        w.playSound(mira, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
        for (Entity e : w.getNearbyEntities(mira, area, 2, area)) {
            if (alvoValido(e)) {
                ((LivingEntity) e).damage(5 * forca(nivel) * mult, g);
                e.setFireTicks(modo == 0 ? 20 : 60);
            }
        }
    }

    /** Curandeiro: cura jogadores e guardiões por perto (a partir do nível III, tira veneno e murchidão). */
    private void curar(LivingEntity g, int nivel) {
        double raio = alcance(Tipo.CURANDEIRO, nivel, temAsas(g));
        List<LivingEntity> feridos = new ArrayList<>();
        for (Entity e : g.getWorld().getNearbyEntities(g.getLocation(), raio, raio, raio)) {
            if (!(e instanceof LivingEntity v) || v == g || v.isDead() || !(v instanceof Player || tipo(v) != null)) continue;
            boolean envenenado = v.hasPotionEffect(PotionEffectType.POISON) || v.hasPotionEffect(PotionEffectType.WITHER);
            if (v.getHealth() < v.getAttribute(Attribute.MAX_HEALTH).getValue() || envenenado) feridos.add(v);
        }
        if (feridos.isEmpty()) return;
        // Munição: mel cura bem, melancia reluzente cura muito; sem nada, só um pouquinho.
        int modo = Equipamento.disparar(g, Tipo.CURANDEIRO, posto(g));
        double cura = modo == 0 ? 1 : modo == 2 ? 2 + nivel * 1.5 : 1 + nivel;
        for (LivingEntity v : feridos) {
            if (modo > 0 && (nivel >= 3 || modo == 2)) {
                v.removePotionEffect(PotionEffectType.POISON);
                v.removePotionEffect(PotionEffectType.WITHER);
            }
            double max = v.getAttribute(Attribute.MAX_HEALTH).getValue();
            v.setHealth(Math.min(max, v.getHealth() + cura));
            v.getWorld().spawnParticle(Particle.HEART, v.getLocation().add(0, v.getHeight() + 0.3, 0), modo == 2 ? 4 : 2, 0.3, 0.2, 0.3, 0);
        }
        g.getWorld().playSound(g.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.6f, 1.4f);
    }

    private boolean podeAtirar(Entity g, long espera) {
        long agora = System.currentTimeMillis();
        if (agora < proximoTiro.getOrDefault(g.getUniqueId(), 0L)) return false;
        proximoTiro.put(g.getUniqueId(), agora + espera);
        return true;
    }

    static void raio(Location de, Location ate, Color cor) {
        Vector passo = ate.toVector().subtract(de.toVector());
        int n = (int) Math.max(4, passo.length() * 3);
        passo.multiply(1.0 / n);
        Location l = de.clone();
        Particle.DustOptions pd = new Particle.DustOptions(cor, 0.9f);
        for (int k = 0; k < n; k++) {
            l.add(passo);
            l.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, pd);
        }
    }

    /** Golem Guardião: não se afasta do posto; com asas, voa atrás do inimigo (até os que voam). */
    private void ferro(IronGolem g, boolean asas, int nivel) {
        Location posto = posto(g);
        double raio = alcance(Tipo.FERRO, nivel, asas);
        LivingEntity alvo = g.getTarget();
        if (alvo == null && ciclo % 10 == 0) {
            LivingEntity novo = inimigoPerto(posto, raio);
            if (novo != null) g.setTarget(novo);
        }
        if (alvo != null && alvo.getLocation().distanceSquared(posto) > (raio + 6) * (raio + 6)) {
            g.setTarget(null);
            alvo = null;
        }
        if (asas) {
            Location destino = alvo != null ? alvo.getLocation() : posto;
            Vector v = destino.toVector().subtract(g.getLocation().toVector());
            boolean voar = (alvo != null && (v.length() > 3 || Math.abs(v.getY()) > 2)) || (alvo == null && v.length() > 3);
            g.setGravity(!voar);
            if (voar) g.setVelocity(v.normalize().multiply(0.45));
            return;
        }
        if (alvo == null && ciclo % 20 == 0 && g.getLocation().distanceSquared(posto) > 6 * 6) {
            g.getPathfinder().moveTo(posto, 1.0);
        }
    }

    /** Asas de partículas nas costas (sem entidade nenhuma seguindo o guardião). */
    private static void asasVisuais(LivingEntity g) {
        Location l = g.getLocation();
        double yaw = Math.toRadians(l.getYaw());
        Vector costas = new Vector(Math.sin(yaw), 0, -Math.cos(yaw)).multiply(-0.2);
        Vector lado = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        double alto = g.getHeight() * 0.7;
        double bate = Math.sin(System.currentTimeMillis() / 120.0) * 0.25;
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 1; k <= 3; k++) {
                Vector p = lado.clone().multiply(s * (0.25 + k * 0.28 * Math.max(0.6, g.getWidth())))
                        .add(costas).add(new Vector(0, alto + k * (0.08 + bate * 0.3), 0));
                l.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(p), 1, 0, 0, 0, 0);
            }
        }
    }
}
