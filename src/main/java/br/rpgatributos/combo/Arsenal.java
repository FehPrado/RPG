package br.rpgatributos.combo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O arsenal novo (2.51): adaga (uma em cada mão = Lâminas Duplas), faca e machadinha de arremesso,
 * foice, bastão e chicote. Todas usam a base de espada do jogo (material = tier) com a marca da
 * arma: a Forja, os combos e o pacote de recursos leem essa marca. Feitas na bancada e forjadas na
 * Forja do Ferreiro como as outras armas.
 */
public final class Arsenal implements Listener {

    public static final NamespacedKey K_ARMA = new NamespacedKey("rpgatributos", "arma_nova");
    private static final TextColor COR = TextColor.color(0xB0BEC5);

    /**
     * {@code dano}: somado ao dano da espada do mesmo material; {@code velocidade}: ataques por
     * segundo; {@code alcance}: alcance extra; {@code empurrao}: empurrão extra.
     */
    public enum Arma {
        ADAGA("adaga", "Adaga", TipoArma.ADAGA, -2, 2.4, 0, 0,
                "Rápida. Pelas costas: +50% de dano. Uma em cada mão: Lâminas Duplas."),
        FACA("faca_arremesso", "Faca de Arremesso", TipoArma.ARREMESSO, -3, 2.0, 0, 0,
                "Clique direito arremessa (depois recolha). Acerto à distância: dano x1,3."),
        MACHADINHA("machadinha", "Machadinha", TipoArma.ARREMESSO, -1, 1.4, 0, 0,
                "Clique direito arremessa (depois recolha). Pesada: dano x1,7."),
        FOICE("foice", "Foice", TipoArma.FOICE, 1, 1.1, 0.5, 0,
                "Corte largo. Cada inimigo derrotado com ela cura 1,5 ❤."),
        BASTAO("bastao", "Bastão", TipoArma.BASTAO, -1, 1.5, 1.0, 1.0,
                "Mais alcance e empurrão. Bom para segurar a linha."),
        CHICOTE("chicote", "Chicote", TipoArma.CHICOTE, -2, 1.3, 3.0, 0,
                "Alcança 3 blocos mais longe que as outras armas."),
        // ---------- armas dos minérios: uma receita só, com o mineral (nome e base fixos) ----------
        MONTANTE("montante", "Montante de Mitrilo", TipoArma.MONTANTE, 4, 0.8, 0.5, 0.8,
                "Duas mãos: pesado e com varredura. Com algo na outra mão, -40% de dano.", Material.DIAMOND_SWORD),
        BUMERANGUE("bumerangue", "Bumerangue de Âmbar", TipoArma.BUMERANGUE, -2, 1.6, 0, 0,
                "Clique direito arremessa: ele vai, volta e acerta nas duas vezes.", Material.IRON_SWORD),
        LEQUES("leques", "Leques de Guerra de Jade", TipoArma.LEQUES, -1, 2.0, 0.5, 0.5,
                "Rápidos. Às vezes uma rajada de vento empurra quem está em volta.", Material.IRON_SWORD),
        ARPAO("arpao", "Arpão de Pérola", TipoArma.ARPAO, 0, 1.1, 1.0, 0,
                "Clique direito arpoa: puxa o alvo até você (ou você até o bloco). Na água, dano x1,5.", Material.IRON_SWORD),
        MANGUAL("mangual", "Mangual de Gelo", TipoArma.MANGUAL, 2, 0.9, 0.5, 0.3,
                "Cada golpe deixa o inimigo lento; às vezes congela.", Material.IRON_SWORD),
        GARRAS("garras", "Garras de Pedra-Sangue", TipoArma.GARRAS, -2, 2.6, 0, 0,
                "Muito rápidas. Cada golpe rouba 10% do dano em vida.", Material.IRON_SWORD),
        CLAVA("clava", "Clava Fumegante", TipoArma.CLAVA, 2, 0.9, 0, 0.6,
                "Com Turfa no inventário, os golpes queimam (1 Turfa a cada 8 queimas).", Material.STONE_SWORD);

        public final String id, nome, descricao;
        private final TipoArma tipo;
        final double dano, velocidade, alcance, empurrao;
        /** Armas dos minérios: a espada de base é fixa (null = uma receita por material, como a adaga). */
        public final Material base;

        Arma(String id, String nome, TipoArma tipo, double dano, double velocidade, double alcance, double empurrao, String descricao) {
            this(id, nome, tipo, dano, velocidade, alcance, empurrao, descricao, null);
        }

        Arma(String id, String nome, TipoArma tipo, double dano, double velocidade, double alcance, double empurrao, String descricao, Material base) {
            this.base = base;
            this.id = id;
            this.nome = nome;
            this.tipo = tipo;
            this.dano = dano;
            this.velocidade = velocidade;
            this.alcance = alcance;
            this.empurrao = empurrao;
            this.descricao = descricao;
        }

        public TipoArma tipo() { return tipo; }
        public String visual() { return "arma_" + id; }
        public boolean arremesso() { return tipo == TipoArma.ARREMESSO; }
        /** Feita com um mineral (Montante, Bumerangue, Leques, Arpão, Mangual, Garras, Clava). */
        public boolean deMinerio() { return base != null; }

        public static Arma porId(String s) {
            for (Arma a : values()) if (a.id.equalsIgnoreCase(s) || a.name().equalsIgnoreCase(s)) return a;
            return null;
        }
    }

    private final RPGAtributos plugin;
    /** Arremessos no ar: projétil → [item, dano]. */
    private final Map<UUID, Object[]> voando = new HashMap<>();
    private final Map<UUID, Long> esperaArremesso = new HashMap<>();
    /** Golpe da segunda adaga em andamento (para não disparar de novo). */
    private final Set<UUID> segundaMao = new HashSet<>();

    /** Dono e dano do arremesso gravados no projétil (para não perder a arma se o servidor reiniciar com ela no ar). */
    private static final NamespacedKey K_DONO = new NamespacedKey("rpgatributos", "arremesso_dono");
    private static final NamespacedKey K_DANO = new NamespacedKey("rpgatributos", "arremesso_dano");

    public Arsenal(RPGAtributos plugin) {
        this.plugin = plugin;
        // Arma que caiu no vazio (nunca acerta nada): volta para o dono.
        Bukkit.getScheduler().runTaskTimer(plugin, this::vigiarArremessos, 40L, 20L);
    }

    /** A receita da arma no material dessa espada (null se não houver). */
    public static org.bukkit.inventory.Recipe receita(Arma a, Material espada) {
        if (a.deMinerio()) return Bukkit.getRecipe(new NamespacedKey("rpgatributos", a.id));
        String tier = espada.name().toLowerCase(Locale.ROOT).replace("_sword", "");
        return Bukkit.getRecipe(new NamespacedKey("rpgatributos", a.id + "_" + tier));
    }

    private void vigiarArremessos() {
        for (var it = voando.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (!(Bukkit.getEntity(en.getKey()) instanceof Snowball s)) continue; // chunk descarregado: ela volta com o chunk
            if (s.isValid() && s.getLocation().getY() >= s.getWorld().getMinHeight() - 8) continue;
            it.remove();
            ItemStack item = (ItemStack) en.getValue()[0];
            Player p = Bukkit.getPlayer((UUID) en.getValue()[2]);
            s.remove();
            if (item.getType().isAir()) continue;
            if (p != null) devolver(p, item);
            else s.getWorld().dropItemNaturally(s.getWorld().getSpawnLocation(), item);
        }
    }

    // =====================================================================
    //  Ler e criar
    // =====================================================================

    public static Arma de(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return null;
        return de(i.getItemMeta());
    }

    public static Arma de(ItemMeta m) {
        String id = m.getPersistentDataContainer().get(K_ARMA, PersistentDataType.STRING);
        return id == null ? null : Arma.porId(id);
    }

    /** "Ferro", "Diamante"... do material da espada. */
    public static String material(Material m) {
        String n = m.name();
        if (n.startsWith("WOODEN")) return "Madeira";
        if (n.startsWith("STONE")) return "Pedra";
        if (n.startsWith("COPPER")) return "Cobre";
        if (n.startsWith("IRON")) return "Ferro";
        if (n.startsWith("GOLDEN")) return "Ouro";
        if (n.startsWith("DIAMOND")) return "Diamante";
        if (n.startsWith("NETHERITE")) return "Netherite";
        return "";
    }

    public static String nome(Arma a, Material m) {
        if (a.deMinerio()) return a.nome;
        String mat = material(m);
        return mat.isEmpty() ? a.nome : a.nome + " de " + mat;
    }

    /** O valor padrão do atributo da espada, ajustado para a arma (usado também pela Forja). */
    public static double ajustar(Arma a, Attribute at, double valor) {
        if (at == Attribute.ATTACK_DAMAGE) return Math.max(0, valor + a.dano);
        if (at == Attribute.ATTACK_SPEED) return a.velocidade - 4;
        return valor;
    }

    /** Alcance e empurrão extras da arma (a Forja também põe). */
    public static void extras(Arma a, Multimap<Attribute, AttributeModifier> mods, EquipmentSlotGroup grupo) {
        if (a.alcance > 0) mods.put(Attribute.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                new NamespacedKey("rpgatributos", "arma_alcance"), a.alcance, AttributeModifier.Operation.ADD_NUMBER, grupo));
        if (a.empurrao > 0) mods.put(Attribute.ATTACK_KNOCKBACK, new AttributeModifier(
                new NamespacedKey("rpgatributos", "arma_empurrao"), a.empurrao, AttributeModifier.Operation.ADD_NUMBER, grupo));
    }

    /** Dano e velocidade da espada trocados pelos da arma (item feito na bancada, sem forja). */
    static void aplicarBase(ItemMeta meta, Arma a, Material espada) {
        Multimap<Attribute, AttributeModifier> mods = ArrayListMultimap.create();
        espada.getDefaultAttributeModifiers().forEach((at, m) -> mods.put(at, new AttributeModifier(m.getKey(),
                ajustar(a, at, m.getAmount()), m.getOperation(), EquipmentSlotGroup.MAINHAND)));
        extras(a, mods, EquipmentSlotGroup.MAINHAND);
        meta.setAttributeModifiers(mods);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        double dano = 1;
        for (AttributeModifier m : espada.getDefaultAttributeModifiers().get(Attribute.ATTACK_DAMAGE)) dano += ajustar(a, Attribute.ATTACK_DAMAGE, m.getAmount());
        List<Component> lore = new ArrayList<>(List.of(
                Component.text(a.descricao, NamedTextColor.GRAY),
                Component.empty(),
                Component.text(" ⚔ " + fmt(dano) + " de dano · " + fmt(a.velocidade) + " golpes/s"
                        + (a.alcance > 0 ? " · +" + fmt(a.alcance) + " de alcance" : ""), NamedTextColor.DARK_GREEN),
                Component.text("Combos: " + a.tipo.nome() + " (/combos)", NamedTextColor.DARK_GRAY)));
        meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    /** A arma feita na bancada (ainda não forjada). */
    public static ItemStack criar(Arma a, Material espada) {
        ItemStack i = new ItemStack(espada);
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(K_ARMA, PersistentDataType.STRING, a.id);
            m.itemName(Component.text(nome(a, espada)));
            PacoteRecursos.marcar(m, a.visual());
            aplicarBase(m, a, espada);
        });
        return i;
    }

    // =====================================================================
    //  Receitas (uma por material de espada)
    // =====================================================================

    private static final Map<String, RecipeChoice> MATERIAIS = new LinkedHashMap<>();

    public void registrarReceitas() {
        MATERIAIS.put("WOODEN_SWORD", new RecipeChoice.MaterialChoice(Tag.PLANKS));
        MATERIAIS.put("STONE_SWORD", new RecipeChoice.MaterialChoice(Material.COBBLESTONE, Material.BLACKSTONE, Material.COBBLED_DEEPSLATE));
        MATERIAIS.put("COPPER_SWORD", new RecipeChoice.MaterialChoice(Material.COPPER_INGOT));
        MATERIAIS.put("IRON_SWORD", new RecipeChoice.MaterialChoice(Material.IRON_INGOT));
        MATERIAIS.put("GOLDEN_SWORD", new RecipeChoice.MaterialChoice(Material.GOLD_INGOT));
        MATERIAIS.put("DIAMOND_SWORD", new RecipeChoice.MaterialChoice(Material.DIAMOND));
        for (Map.Entry<String, RecipeChoice> en : MATERIAIS.entrySet()) {
            Material espada = Material.matchMaterial(en.getKey());
            if (espada == null) continue;
            String tier = en.getKey().toLowerCase(Locale.ROOT).replace("_sword", "");
            for (Arma a : Arma.values()) {
                if (a.deMinerio()) continue;
                ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, a.id + "_" + tier), criar(a, espada));
                switch (a) {
                    case ADAGA -> r.shape(" M", "S ");
                    case FACA -> r.shape("M", "F");
                    case MACHADINHA -> r.shape("MM", "SF");
                    case FOICE -> r.shape("MMM", "  S", " S ");
                    case BASTAO -> r.shape("  M", " S ", "S  ");
                    case CHICOTE -> r.shape("  M", " L ", "S  ");
                }
                r.setIngredient('M', en.getValue());
                if (a != Arma.FACA) r.setIngredient('S', Material.STICK);
                if (a == Arma.FACA || a == Arma.MACHADINHA) r.setIngredient('F', Material.FEATHER);
                if (a == Arma.CHICOTE) r.setIngredient('L', Material.LEATHER);
                Bukkit.addRecipe(r);
            }
        }
        receitasDeMinerio();
    }

    /** As armas dos minérios: uma receita cada, com o mineral (o item do plugin, igualzinho). */
    private void receitasDeMinerio() {
        RecipeChoice sticks = new RecipeChoice.MaterialChoice(Material.STICK);
        for (Arma a : Arma.values()) {
            if (!a.deMinerio()) continue;
            ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, a.id), criar(a, a.base));
            switch (a) {
                case MONTANTE -> {
                    r.shape(" M ", " M ", "DSD");
                    r.setIngredient('M', new RecipeChoice.ExactChoice(br.rpgatributos.exploracao.Mitrilo.lingote(1)));
                    r.setIngredient('D', Material.DIAMOND);
                    r.setIngredient('S', sticks);
                }
                case BUMERANGUE -> {
                    r.shape("PA", " P");
                    r.setIngredient('A', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.AMBAR.criar(1)));
                    r.setIngredient('P', new RecipeChoice.MaterialChoice(Tag.PLANKS));
                }
                case LEQUES -> {
                    r.shape("JPJ", " S ");
                    r.setIngredient('J', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.JADE.criar(1)));
                    r.setIngredient('P', Material.PAPER);
                    r.setIngredient('S', sticks);
                }
                case ARPAO -> {
                    r.shape("  P", " I ", "S  ");
                    r.setIngredient('P', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.PEROLA.criar(1)));
                    r.setIngredient('I', Material.IRON_INGOT);
                    r.setIngredient('S', sticks);
                }
                case MANGUAL -> {
                    r.shape("  C", " N ", "S  ");
                    r.setIngredient('C', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.CRISTAL_DE_GELO.criar(1)));
                    Material corrente = Material.matchMaterial("IRON_CHAIN");
                    r.setIngredient('N', corrente != null ? corrente : Material.matchMaterial("CHAIN"));
                    r.setIngredient('S', sticks);
                }
                case GARRAS -> {
                    r.shape("B B", "III");
                    r.setIngredient('B', new RecipeChoice.ExactChoice(br.rpgatributos.aventura.Raro.PEDRA_SANGUE.criar(1)));
                    r.setIngredient('I', Material.IRON_INGOT);
                }
                case CLAVA -> {
                    r.shape(" T ", " L ", " S ");
                    r.setIngredient('T', new RecipeChoice.ExactChoice(br.rpgatributos.exploracao.Mineracao.turfa(1)));
                    r.setIngredient('L', new RecipeChoice.MaterialChoice(Tag.LOGS));
                    r.setIngredient('S', sticks);
                }
                default -> { continue; }
            }
            Bukkit.addRecipe(r);
        }
    }

    /** Arma feita na bancada que vira Netherite na mesa de ferraria: dano da Netherite (a forjada a Forja refaz). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoFerrar(PrepareSmithingEvent e) {
        ItemStack r = e.getResult();
        Arma a = de(r);
        if (a == null || plugin.forja().ler(r) != null) return;
        r.editMeta(m -> {
            m.itemName(Component.text(nome(a, r.getType())));
            aplicarBase(m, a, r.getType());
        });
        e.setResult(r);
    }

    // =====================================================================
    //  Arremesso
    // =====================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoArremessar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack mao = e.getItem();
        Arma a = de(mao);
        if (a == null || !(a.arremesso() || a == Arma.BUMERANGUE || a == Arma.ARPAO)) return;
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && (e.getClickedBlock().getType().isInteractable() || plugin.ehEstacao(e.getClickedBlock()))) return;
        if (plugin.combos().emSequencia(p)) return; // o clique faz parte de um combo
        if (a == Arma.BUMERANGUE || a == Arma.ARPAO) {
            // Bumerangue e arpão não saem da mão: o que voa é a imagem deles (e voltam sozinhos).
            e.setCancelled(true);
            if (a == Arma.BUMERANGUE) lancarBumerangue(p, mao);
            else arpoar(p, mao);
            return;
        }
        long agora = System.currentTimeMillis();
        if (agora - esperaArremesso.getOrDefault(p.getUniqueId(), 0L) < 600) return;
        esperaArremesso.put(p.getUniqueId(), agora);
        e.setCancelled(true);
        ItemStack voa = mao.clone();
        voa.setAmount(1);
        double dano = danoDoItem(voa) * (a == Arma.MACHADINHA ? 1.7 : 1.3) * mestra(p, TipoArma.ARREMESSO, 1.25);
        p.getInventory().setItemInMainHand(mao.getAmount() > 1 ? mao.asQuantity(mao.getAmount() - 1) : null);
        Snowball s = p.launchProjectile(Snowball.class, p.getEyeLocation().getDirection().multiply(a == Arma.MACHADINHA ? 1.4 : 1.8));
        s.setItem(voa);
        s.getPersistentDataContainer().set(K_DONO, PersistentDataType.STRING, p.getUniqueId().toString());
        s.getPersistentDataContainer().set(K_DANO, PersistentDataType.DOUBLE, dano);
        voando.put(s.getUniqueId(), new Object[]{voa, dano, p.getUniqueId()});
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, a == Arma.MACHADINHA ? 0.8f : 1.4f);
    }

    /** Dano da arma (atributos do item) + o do jogador. */
    private static double danoDoItem(ItemStack i) {
        double d = 1;
        if (i.hasItemMeta() && i.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE) != null) {
            for (AttributeModifier m : i.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE)) {
                if (m.getOperation() == AttributeModifier.Operation.ADD_NUMBER) d += m.getAmount();
            }
        } else {
            for (AttributeModifier m : i.getType().getDefaultAttributeModifiers().get(Attribute.ATTACK_DAMAGE)) d += m.getAmount();
        }
        return d;
    }

    @EventHandler
    public void aoAcertar(ProjectileHitEvent e) {
        Object[] v = voando.remove(e.getEntity().getUniqueId());
        if (v == null && e.getEntity() instanceof Snowball s && s.getPersistentDataContainer().has(K_DONO)) {
            // Arremessada antes do servidor reiniciar: os dados estão no próprio projétil.
            try {
                v = new Object[]{s.getItem(), s.getPersistentDataContainer().getOrDefault(K_DANO, PersistentDataType.DOUBLE, 1.0),
                        UUID.fromString(s.getPersistentDataContainer().get(K_DONO, PersistentDataType.STRING))};
            } catch (IllegalArgumentException ex) {
                return;
            }
        }
        if (v == null) return;
        ItemStack item = (ItemStack) v[0];
        double dano = (double) v[1];
        Player p = Bukkit.getPlayer((UUID) v[2]);
        org.bukkit.Location onde = e.getEntity().getLocation();
        if (e.getHitEntity() instanceof LivingEntity alvo && p != null && plugin.alvos().inimigo(p, alvo)) {
            plugin.alvos().ferir(p, alvo, dano);
            alvo.getWorld().spawnParticle(Particle.CRIT, alvo.getLocation().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.1);
            alvo.getWorld().playSound(alvo.getLocation(), Sound.ITEM_TRIDENT_HIT, 1f, 1.2f);
            plugin.combos().darXp(p, TipoArma.ARREMESSO, plugin.settings().comXpAcerto);
            onde = alvo.getLocation();
        }
        gastar(item);
        if (item.getType().isAir()) return;
        // Postura Mestra do arremesso: a arma volta sozinha para a mão.
        if (p != null && p.isOnline() && mestra(p, TipoArma.ARREMESSO, 2) > 1 && p.getWorld().equals(onde.getWorld())) {
            devolver(p, item);
            return;
        }
        Item caido = onde.getWorld().dropItem(onde, item);
        caido.setVelocity(new Vector());
        caido.setGlowing(true);
        caido.setPickupDelay(10);
        caido.setUnlimitedLifetime(true);
        if (p != null) {
            caido.setOwner(p.getUniqueId());
            UUID id = caido.getUniqueId(), dono = p.getUniqueId();
            // Esquecida no chão: volta sozinha para o dono depois de 1 minuto.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!(Bukkit.getEntity(id) instanceof Item it) || !it.isValid()) return;
                Player d = Bukkit.getPlayer(dono);
                if (d == null) return;
                devolver(d, it.getItemStack());
                it.remove();
            }, 20L * 60);
        }
    }

    private static void devolver(Player p, ItemStack i) {
        for (ItemStack sobra : p.getInventory().addItem(i).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.3f);
    }

    /** Cada arremesso gasta 1 de durabilidade. */
    private static void gastar(ItemStack i) {
        if (!(i.getItemMeta() instanceof Damageable d) || i.getType().getMaxDurability() <= 0 || i.getItemMeta().isUnbreakable()) return;
        int dano = d.getDamage() + 1;
        if (dano >= i.getType().getMaxDurability()) {
            i.setAmount(0);
            return;
        }
        i.editMeta(Damageable.class, m -> m.setDamage(dano));
    }

    // =====================================================================
    //  Adaga pelas costas, segunda mão, foice e chicote
    // =====================================================================

    /** Multiplicador da Postura Mestra da arma (1 sem ela). */
    private double mestra(Player p, TipoArma t, double valor) {
        return plugin.combos().posturas().ativa(p) == Postura.MESTRA && TipoArma.de(p) == t ? valor : 1;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || !(e.getEntity() instanceof LivingEntity alvo)) return;
        TipoArma t = TipoArma.de(p);
        aoBaterMinerio(e, p, alvo, t);
        if (t == TipoArma.ADAGA || t == TipoArma.DUPLAS) {
            Vector olhar = alvo.getLocation().getDirection().setY(0), deMim = p.getLocation().toVector().subtract(alvo.getLocation().toVector()).setY(0);
            if (olhar.lengthSquared() > 1e-4 && deMim.lengthSquared() > 1e-4 && olhar.normalize().dot(deMim.normalize()) < -0.4) {
                e.setDamage(e.getDamage() * (t == TipoArma.ADAGA ? mestra(p, TipoArma.ADAGA, 1.2) : 1) * 1.5);
                alvo.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, alvo.getLocation().add(0, 1.2, 0), 6, 0.2, 0.2, 0.2, 0.1);
                p.sendActionBar(Component.text("† Pelas costas! +50%", NamedTextColor.RED));
            }
        }
        if (t == TipoArma.CHICOTE && mestra(p, TipoArma.CHICOTE, 2) > 1) {
            alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true));
        }
        // Lâminas Duplas: às vezes a outra adaga também acerta (sempre, com a Postura Mestra).
        if (t == TipoArma.DUPLAS && !segundaMao.contains(p.getUniqueId())
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < (mestra(p, TipoArma.DUPLAS, 2) > 1 ? 1 : 0.4)) {
            double extra = danoDoItem(p.getInventory().getItemInOffHand()) * 0.6;
            UUID id = p.getUniqueId();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline() || !alvo.isValid() || alvo.isDead() || TipoArma.de(p) != TipoArma.DUPLAS) return;
                segundaMao.add(id);
                try {
                    p.swingOffHand();
                    alvo.setNoDamageTicks(0);
                    plugin.alvos().ferir(p, alvo, extra);
                    alvo.getWorld().spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                } finally {
                    segundaMao.remove(id);
                }
            }, 4L);
        }
    }

    // =====================================================================
    //  Armas dos minérios
    // =====================================================================

    /** Golpes de varredura do Montante em andamento (o dano da varredura não varre de novo). */
    private final Set<UUID> varrendo = new HashSet<>();
    /** Bumerangue ou arpão no ar (um de cada vez). */
    private final Set<UUID> noAr = new HashSet<>();
    /** Queimas da Clava desde a última Turfa gasta. */
    private final Map<UUID, Integer> queimas = new HashMap<>();

    private void aoBaterMinerio(EntityDamageByEntityEvent e, Player p, LivingEntity alvo, TipoArma t) {
        if (t == null) return;
        switch (t) {
            case MONTANTE -> {
                if (varrendo.contains(p.getUniqueId())) return;
                ItemStack outra = p.getInventory().getItemInOffHand();
                if (!outra.getType().isAir()) {
                    e.setDamage(e.getDamage() * 0.6);
                    if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.2) {
                        p.sendActionBar(Component.text("⚔ O Montante pede as duas mãos (-40% de dano).", NamedTextColor.GRAY));
                    }
                    return;
                }
                double parte = e.getDamage() * (mestra(p, TipoArma.MONTANTE, 2) > 1 ? 0.7 : 0.4);
                varrendo.add(p.getUniqueId());
                try {
                    for (org.bukkit.entity.Entity o : alvo.getNearbyEntities(2.5, 1.5, 2.5)) {
                        if (o instanceof LivingEntity le && le != p && plugin.alvos().inimigo(p, le)) plugin.alvos().ferir(p, le, parte);
                    }
                } finally {
                    varrendo.remove(p.getUniqueId());
                }
                alvo.getWorld().spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, 1, 0), 3, 0.8, 0.2, 0.8, 0);
            }
            case LEQUES -> {
                boolean mestre = mestra(p, TipoArma.LEQUES, 2) > 1;
                if (!mestre && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= 0.2) return;
                for (org.bukkit.entity.Entity o : p.getNearbyEntities(3, 2, 3)) {
                    if (!(o instanceof LivingEntity le) || le == alvo || !plugin.alvos().inimigo(p, le)) continue;
                    Vector v = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (v.lengthSquared() > 1e-4) le.setVelocity(v.normalize().multiply(0.9).setY(0.3));
                }
                p.getWorld().spawnParticle(Particle.GUST, p.getLocation().add(0, 1, 0), 2, 0.6, 0.2, 0.6, 0);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 1.5f);
            }
            case MANGUAL -> {
                boolean mestre = mestra(p, TipoArma.MANGUAL, 2) > 1;
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, mestre ? 2 : 0, false, true, true));
                if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.1) {
                    alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 5, false, true, true));
                    alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), 100));
                    alvo.getWorld().spawnParticle(Particle.SNOWFLAKE, alvo.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                    alvo.getWorld().playSound(alvo.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
                }
            }
            case GARRAS -> {
                double cura = e.getDamage() * (mestra(p, TipoArma.GARRAS, 2) > 1 ? 0.2 : 0.1);
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + cura));
                alvo.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, alvo.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.05);
            }
            case CLAVA -> {
                boolean mestre = mestra(p, TipoArma.CLAVA, 2) > 1;
                if (!mestre && !temTurfa(p)) return;
                alvo.setFireTicks(Math.max(alvo.getFireTicks(), 60));
                alvo.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, alvo.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
                if (mestre) return;
                int n = queimas.merge(p.getUniqueId(), 1, Integer::sum);
                if (n >= 8) {
                    queimas.remove(p.getUniqueId());
                    gastarTurfa(p);
                }
            }
            default -> { }
        }
    }

    private static boolean ehTurfa(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(br.rpgatributos.exploracao.Mineracao.K_TURFA);
    }

    private static boolean temTurfa(Player p) {
        for (ItemStack i : p.getInventory().getContents()) if (ehTurfa(i)) return true;
        return false;
    }

    private static void gastarTurfa(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (!ehTurfa(i)) continue;
            i.setAmount(i.getAmount() - 1);
            if (!temTurfa(p)) p.sendActionBar(Component.text("♨ A Clava apagou: acabou a Turfa.", NamedTextColor.GRAY));
            return;
        }
    }

    /** O bumerangue vai até 11 blocos (ou até bater) e volta para a mão, acertando na ida e na volta. */
    private void lancarBumerangue(Player p, ItemStack item) {
        if (!noAr.add(p.getUniqueId())) return;
        double dano = danoDoItem(item) * mestra(p, TipoArma.BUMERANGUE, 1.3);
        boolean mestre = mestra(p, TipoArma.BUMERANGUE, 2) > 1;
        org.bukkit.World w = p.getWorld();
        org.bukkit.Location pos = p.getEyeLocation().subtract(0, 0.3, 0);
        Vector dir = p.getEyeLocation().getDirection().normalize();
        ItemStack visual = item.asOne();
        org.bukkit.entity.ItemDisplay d = w.spawn(pos, org.bukkit.entity.ItemDisplay.class, x -> {
            x.setItemStack(visual);
            x.setPersistent(false);
            x.setTeleportDuration(1);
        });
        Set<UUID> ida = new HashSet<>(), volta = new HashSet<>();
        int[] t = {0};
        boolean[] voltando = {false};
        org.bukkit.scheduler.BukkitTask[] ref = new org.bukkit.scheduler.BukkitTask[1];
        Runnable fim = () -> {
            ref[0].cancel();
            if (d.isValid()) d.remove();
            noAr.remove(p.getUniqueId());
        };
        w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 0.8f, 1.6f);
        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            t[0]++;
            if (!p.isOnline() || !d.isValid() || t[0] > 70 || !p.getWorld().equals(w)) {
                fim.run();
                return;
            }
            if (t[0] > 10) voltando[0] = true;
            Vector passo;
            if (!voltando[0]) {
                passo = dir.clone().multiply(1.1);
                if (!pos.clone().add(passo).getBlock().isPassable()) {
                    voltando[0] = true;
                    return;
                }
            } else {
                Vector para = p.getEyeLocation().subtract(0, 0.3, 0).toVector().subtract(pos.toVector());
                if (para.lengthSquared() < 1.5) {
                    fim.run();
                    w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.5f);
                    return;
                }
                passo = para.normalize().multiply(1.3);
            }
            pos.add(passo);
            d.teleport(pos);
            d.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.AxisAngle4f(t[0] * 0.9f, 0, 1, 0),
                    new org.joml.Vector3f(0.8f), new org.joml.AxisAngle4f()));
            for (org.bukkit.entity.Entity o : w.getNearbyEntities(pos, 1.2, 1.2, 1.2)) {
                if (!(o instanceof LivingEntity le) || !plugin.alvos().inimigo(p, le)) continue;
                if (!(voltando[0] ? volta : ida).add(le.getUniqueId())) continue;
                plugin.alvos().ferir(p, le, voltando[0] && mestre ? dano * 2 : dano);
                w.spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.1);
                w.playSound(le.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.8f, 1.4f);
                plugin.combos().darXp(p, TipoArma.BUMERANGUE, plugin.settings().comXpAcerto);
            }
            if (t[0] % 4 == 0) w.playSound(pos, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.8f);
        }, 1L, 1L);
    }

    /** O arpão voa até 18 blocos: acertando alguém, puxa até você; acertando um bloco, puxa você até ele. */
    private void arpoar(Player p, ItemStack item) {
        if (!noAr.add(p.getUniqueId())) return;
        boolean mestre = mestra(p, TipoArma.ARPAO, 2) > 1;
        double dano = danoDoItem(item) * mestra(p, TipoArma.ARPAO, 1.3) * (p.isInWater() ? 1.5 : 1);
        org.bukkit.World w = p.getWorld();
        org.bukkit.Location pos = p.getEyeLocation().subtract(0, 0.2, 0);
        Vector passo = p.getEyeLocation().getDirection().normalize().multiply(1.6);
        ItemStack visual = item.asOne();
        org.bukkit.entity.ItemDisplay d = w.spawn(pos, org.bukkit.entity.ItemDisplay.class, x -> {
            x.setItemStack(visual);
            x.setPersistent(false);
            x.setTeleportDuration(1);
        });
        int[] t = {0};
        org.bukkit.scheduler.BukkitTask[] ref = new org.bukkit.scheduler.BukkitTask[1];
        Runnable fim = () -> {
            ref[0].cancel();
            if (d.isValid()) d.remove();
            noAr.remove(p.getUniqueId());
        };
        w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
        ref[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            t[0]++;
            if (!p.isOnline() || !d.isValid() || t[0] > 12 || !p.getWorld().equals(w)) {
                fim.run();
                return;
            }
            org.bukkit.Location prox = pos.clone().add(passo);
            // A corda: partículas do jogador até a ponta.
            Vector corda = pos.toVector().subtract(p.getEyeLocation().toVector());
            for (double s2 = 0; s2 < corda.length(); s2 += 0.8) {
                w.spawnParticle(Particle.CRIT, p.getEyeLocation().add(corda.clone().normalize().multiply(s2)), 1, 0, 0, 0, 0);
            }
            if (!prox.getBlock().isPassable()) {
                // Bateu num bloco: puxa o jogador até lá.
                Vector v = pos.toVector().subtract(p.getLocation().toVector());
                p.setVelocity(v.multiply(0.16 * (mestre ? 1.4 : 1)).setY(Math.max(0.4, v.getY() * 0.18 + 0.4)));
                w.playSound(pos, Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1f);
                fim.run();
                return;
            }
            pos.add(passo);
            d.teleport(pos);
            for (org.bukkit.entity.Entity o : w.getNearbyEntities(pos, 1.1, 1.1, 1.1)) {
                if (!(o instanceof LivingEntity le) || !plugin.alvos().inimigo(p, le)) continue;
                plugin.alvos().ferir(p, le, dano);
                if (!br.rpgatributos.aventura.Chefes.ehChefe(le)) {
                    Vector v = p.getLocation().toVector().subtract(le.getLocation().toVector());
                    if (v.lengthSquared() > 1) le.setVelocity(v.normalize().multiply(mestre ? 2.2 : 1.3).setY(0.4));
                }
                w.spawnParticle(Particle.SPLASH, le.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.1);
                w.playSound(le.getLocation(), Sound.ITEM_TRIDENT_HIT, 1f, 0.9f);
                plugin.combos().darXp(p, TipoArma.ARPAO, plugin.settings().comXpAcerto);
                fim.run();
                return;
            }
        }, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoCeifar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || TipoArma.de(p) != TipoArma.FOICE) return;
        boolean ceifador = mestra(p, TipoArma.FOICE, 2) > 1;
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + (ceifador ? 6 : 3)));
        if (ceifador) p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, true, true));
        p.getWorld().spawnParticle(Particle.SOUL, e.getEntity().getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.02);
    }

    /** Para o admin: dá uma arma pronta (feita na bancada). */
    public static ItemStack admin(String id, String material) {
        Arma a = Arma.porId(id);
        Material m = material == null ? Material.IRON_SWORD : Material.matchMaterial(material.toUpperCase(Locale.ROOT) + "_SWORD");
        if (a == null) return null;
        if (a.deMinerio()) return criar(a, a.base);
        return criar(a, m == null ? Material.IRON_SWORD : m);
    }
}
