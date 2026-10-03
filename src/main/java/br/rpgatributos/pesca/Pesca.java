package br.rpgatributos.pesca;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O atributo Pesca: isca mais rápida, peixes com qualidade, peixes raros (por bioma,
 * horário e clima), Tesouros do Mar e criaturas marinhas que mordem a isca.
 * O /peixes abre o Diário do Pescador.
 */
public final class Pesca implements Listener {

    public static final TextColor COR = TextColor.color(0x2FA4C9);
    private static final Set<Material> PEIXES = Set.of(Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH);
    private static final Set<Material> TESOUROS = Set.of(Material.BOW, Material.ENCHANTED_BOOK, Material.NAME_TAG,
            Material.NAUTILUS_SHELL, Material.SADDLE);
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
    private static final int S_INFO = 4;
    private static final int S_FECHAR = 49;

    /** Criaturas que mordem a isca no lugar de um peixe. */
    enum Criatura {
        AFOGADO_FAMINTO("Afogado Faminto", EntityType.DROWNED, 30, 0, 50, 30, 0.3, 0.05, 1),
        GUARDIAO_DAS_ONDAS("Guardião das Ondas", EntityType.GUARDIAN, 40, 0.25, 30, 50, 0.6, 0.1, 1),
        CAPITAO_AFOGADO("Capitão Afogado", EntityType.DROWNED, 80, 0.5, 15, 90, 1, 0.2, 1),
        SENHOR_DAS_MARES("Senhor das Marés", EntityType.DROWNED, 200, 0.8, 5, 250, 1, 0.5, 3);

        final String nome;
        final EntityType tipo;
        final double vida;
        final double nivel;
        final int peso;
        final double xp;
        final double chanceEscama;
        final double chancePerola;
        final int escamas;

        Criatura(String nome, EntityType tipo, double vida, double nivel, int peso, double xp,
                 double chanceEscama, double chancePerola, int escamas) {
            this.nome = nome;
            this.tipo = tipo;
            this.vida = vida;
            this.nivel = nivel;
            this.peso = peso;
            this.xp = xp;
            this.chanceEscama = chanceEscama;
            this.chancePerola = chancePerola;
            this.escamas = escamas;
        }
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kCriatura, kVistos;

    public Pesca(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kCriatura = new NamespacedKey(plugin, "criatura_marinha");
        this.kVistos = new NamespacedKey(plugin, "peixes_vistos");
    }

    private Settings cfg() { return plugin.settings(); }
    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private int nivel(Player p) { return plugin.stats().getNivel(p, Skill.PESCA); }

    private double fracao(Player p) { return Math.min(1, nivel(p) / (double) cfg().nivelMaximo); }

    public double chanceRaro(int nivel) {
        return cfg().pesChanceRaroBase + (cfg().pesChanceRaroMax - cfg().pesChanceRaroBase) * Math.min(1, nivel / (double) cfg().nivelMaximo);
    }

    public double chanceTesouro(int nivel) {
        return cfg().pesChanceTesouroBase + (cfg().pesChanceTesouroMax - cfg().pesChanceTesouroBase) * Math.min(1, nivel / (double) cfg().nivelMaximo);
    }

    public double chanceCriatura(int nivel) {
        return cfg().pesChanceCriaturaBase + (cfg().pesChanceCriaturaMax - cfg().pesChanceCriaturaBase) * Math.min(1, nivel / (double) cfg().nivelMaximo);
    }

    public String descricao(int nivel) {
        return "isca " + pct(nivel * cfg().pesIscaRapida) + " mais rápida, " + pct(chanceRaro(nivel)) + " peixe raro, "
                + pct(chanceTesouro(nivel)) + " tesouro, " + pct(nivel * cfg().pesChanceDupla) + " peixe em dobro";
    }

    private static String pct(double v) {
        return StatsManager.fmt(Math.round(v * 1000) / 10.0) + "%";
    }

    // =====================================================================
    //  Pescar
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        Player p = e.getPlayer();
        switch (e.getState()) {
            case FISHING -> isca(p, e.getHook());
            case CAUGHT_FISH -> {
                if (e.getCaught() instanceof Item item) fisgou(p, e.getHook(), item, e);
            }
            default -> { }
        }
    }

    /** A isca afunda: com Pesca alta, o peixe morde mais rápido. */
    private void isca(Player p, FishHook hook) {
        double f = 1 - nivel(p) * cfg().pesIscaRapida;
        if (f >= 0.999) return;
        int min = Math.max(20, (int) Math.round(hook.getMinWaitTime() * f));
        int max = Math.max(min + 20, (int) Math.round(hook.getMaxWaitTime() * f));
        hook.setWaitTime(min, max);
    }

    private PeixeRaro.Contexto contexto(Location l) {
        World w = l.getWorld();
        Block b = l.getBlock();
        long hora = w.getTime();
        return new PeixeRaro.Contexto(w.getBiome(b.getX(), b.getY(), b.getZ()).getKey().getKey(), b.getTemperature(), b.getY(),
                hora >= 13000 && hora <= 23000, w.hasStorm(), w.isThundering());
    }

    private void fisgou(Player p, FishHook hook, Item item, PlayerFishEvent e) {
        ItemStack pego = item.getItemStack();
        Material tipo = pego.getType();
        int nivel = nivel(p);
        double sorteDoMar = 0;
        ItemStack vara = p.getInventory().getItem(e.getHand() == null ? org.bukkit.inventory.EquipmentSlot.HAND : e.getHand());
        if (vara != null && vara.getType() == Material.FISHING_ROD) {
            sorteDoMar = vara.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA) * 0.01;
        }
        Location onde = hook.getLocation();
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        var titulos = plugin.titulos();

        // 1) Criatura marinha: vem no lugar do peixe.
        if (p.getWorld().getDifficulty() != Difficulty.PEACEFUL && onde.getBlock().isLiquid()
                && rnd().nextDouble() < chanceCriatura(nivel)) {
            Criatura c = sortearCriatura(nivel, contexto(onde).oceano());
            if (c != null) {
                item.remove();
                invocar(c, onde, p);
                return;
            }
        }

        // 2) Tesouro do Mar: extra, por cima do que veio.
        if (rnd().nextDouble() < chanceTesouro(nivel) + sorteDoMar) {
            List<ItemStack> loot = tesouro(fracao(p));
            item.setItemStack(loot.getFirst());
            for (int i = 1; i < loot.size(); i++) {
                Item extra = item.getWorld().dropItem(item.getLocation(), loot.get(i));
                extra.setVelocity(item.getVelocity());
            }
            p.showTitle(Title.title(Component.text("✦ Tesouro do Mar!", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Algo brilhante veio na linha", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1600), Duration.ofMillis(400))));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
            onde.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, onde, 30, 0.3, 0.3, 0.3, 0.3);
            if (!criativo) plugin.stats().darXp(p, Skill.PESCA, cfg().pesXpTesouro * 1.5);
            titulos.registrar(p, "tesouros", 1);
            titulos.registrar(p, "peixes", 1);
            return;
        }

        if (PEIXES.contains(tipo)) {
            // 3) Peixe raro, se o lugar/horário/clima deixar.
            if (rnd().nextDouble() < chanceRaro(nivel) + sorteDoMar) {
                PeixeRaro raro = sortearPeixe(contexto(onde), nivel);
                if (raro != null) {
                    item.setItemStack(raro.criar(1, Qualidade.sortear(fracao(p))));
                    anunciarPeixe(p, raro);
                    if (!criativo) plugin.stats().darXp(p, Skill.PESCA, raro.xp());
                    titulos.registrar(p, "peixes", 1);
                    titulos.registrar(p, "peixes_raros", 1);
                    return;
                }
            }
            // 4) Peixe comum: qualidade e chance de vir dois.
            ItemStack peixe = pego.clone();
            if (rnd().nextDouble() < nivel * cfg().pesChanceDupla) peixe.setAmount(peixe.getAmount() + 1);
            Qualidade.sortear(fracao(p)).aplicar(peixe);
            item.setItemStack(peixe);
            if (!criativo) plugin.stats().darXp(p, Skill.PESCA, cfg().pesXpPeixe);
        } else if (TESOUROS.contains(tipo) || (tipo == Material.FISHING_ROD && !pego.getEnchantments().isEmpty())) {
            if (!criativo) plugin.stats().darXp(p, Skill.PESCA, cfg().pesXpTesouro);
            titulos.registrar(p, "tesouros", 1);
        } else if (!criativo) {
            plugin.stats().darXp(p, Skill.PESCA, cfg().pesXpLixo);
        }
        titulos.registrar(p, "peixes", 1);
    }

    private PeixeRaro sortearPeixe(PeixeRaro.Contexto c, int nivel) {
        List<PeixeRaro> possiveis = new ArrayList<>();
        int total = 0;
        for (PeixeRaro pr : PeixeRaro.values()) {
            if (nivel < pr.nivelNecessario(cfg().nivelMaximo) || !pr.pode(c)) continue;
            possiveis.add(pr);
            total += peso(pr);
        }
        if (total <= 0) return null;
        int x = rnd().nextInt(total);
        for (PeixeRaro pr : possiveis) {
            x -= peso(pr);
            if (x < 0) return pr;
        }
        return null;
    }

    /** Peso do peixe no sorteio (a estação favorece alguns). */
    private int peso(PeixeRaro pr) {
        return (int) Math.round(pr.peso() * plugin.estacoes().pesoPeixe(pr));
    }

    private void anunciarPeixe(Player p, PeixeRaro pr) {
        boolean novo = marcarVisto(p, pr);
        p.sendMessage(Component.text("⚓ Você pescou um ", COR)
                .append(Component.text(pr.nome(), pr.cor(), TextDecoration.BOLD))
                .append(Component.text(novo ? "! (novo no Diário do Pescador)" : "!", COR)));
        p.playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 1.4f);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        if (pr == PeixeRaro.PEIXE_DRAGAO) {
            Bukkit.broadcast(Component.text("⚓ ", COR).append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" pescou um ", NamedTextColor.GRAY))
                    .append(Component.text(pr.nome(), pr.cor(), TextDecoration.BOLD))
                    .append(Component.text(" no meio da tempestade!", NamedTextColor.GRAY)));
        }
    }

    // ---------- Diário: espécies vistas ----------

    private Set<String> vistos(Player p) {
        String s = p.getPersistentDataContainer().get(kVistos, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    private boolean marcarVisto(Player p, PeixeRaro pr) {
        Set<String> v = vistos(p);
        if (!v.add(pr.name())) return false;
        p.getPersistentDataContainer().set(kVistos, PersistentDataType.STRING, String.join(",", v));
        plugin.titulos().registrar(p, "especies_peixe", 1);
        return true;
    }

    // =====================================================================
    //  Tesouro do Mar
    // =====================================================================

    private List<ItemStack> tesouro(double fracao) {
        List<ItemStack> l = new ArrayList<>();
        int quantos = 2 + (rnd().nextDouble() < fracao ? 1 : 0) + (rnd().nextDouble() < fracao * 0.5 ? 1 : 0);
        for (int i = 0; i < quantos; i++) l.add(itemTesouro());
        return l;
    }

    private ItemStack itemTesouro() {
        int x = rnd().nextInt(100);
        if ((x -= 26) < 0) return new ItemStack(Material.EMERALD, 2 + rnd().nextInt(5));
        if ((x -= 16) < 0) return new ItemStack(Material.GOLD_INGOT, 2 + rnd().nextInt(4));
        if ((x -= 14) < 0) return Reagente.PEROLA_NEGRA.criar(1);
        if ((x -= 12) < 0) return livro();
        if ((x -= 8) < 0) return new ItemStack(Material.DIAMOND, 1 + rnd().nextInt(2));
        if ((x -= 7) < 0) return new ItemStack(Material.NAUTILUS_SHELL);
        if ((x -= 5) < 0) return new ItemStack(Material.NAME_TAG);
        if ((x -= 4) < 0) return new ItemStack(Material.SADDLE);
        if ((x -= 3) < 0) return new ItemStack(Material.HEART_OF_THE_SEA);
        if ((x -= 3) < 0) return Raro.MAPA_RASGADO.criar();
        if ((x -= 2) < 0) return Raro.FRAGMENTO_DE_FORJA.criar();
        return Reagente.ESCAMA_DO_ABISMO.criar(1);
    }

    private static ItemStack livro() {
        Enchantment[] opcoes = {Enchantment.LURE, Enchantment.LUCK_OF_THE_SEA, Enchantment.UNBREAKING, Enchantment.DEPTH_STRIDER,
                Enchantment.RESPIRATION, Enchantment.AQUA_AFFINITY, Enchantment.FROST_WALKER, Enchantment.IMPALING,
                Enchantment.RIPTIDE, Enchantment.LOYALTY, Enchantment.MENDING};
        Enchantment en = opcoes[rnd().nextInt(opcoes.length)];
        ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
        livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(en, 1 + rnd().nextInt(en.getMaxLevel()), false));
        return livro;
    }

    // =====================================================================
    //  Criaturas marinhas
    // =====================================================================

    private Criatura sortearCriatura(int nivel, boolean oceano) {
        List<Criatura> possiveis = new ArrayList<>();
        int total = 0;
        for (Criatura c : Criatura.values()) {
            if (nivel < Math.ceil(c.nivel * cfg().nivelMaximo)) continue;
            if (c.tipo == EntityType.GUARDIAN && !oceano) continue;
            possiveis.add(c);
            total += c.peso;
        }
        if (total <= 0) return null;
        int x = rnd().nextInt(total);
        for (Criatura c : possiveis) {
            x -= c.peso;
            if (x < 0) return c;
        }
        return null;
    }

    private void invocar(Criatura c, Location onde, Player p) {
        World w = onde.getWorld();
        LivingEntity ent = (LivingEntity) w.spawnEntity(onde, c.tipo);
        ent.customName(Component.text("⚓ " + c.nome, COR, TextDecoration.BOLD));
        ent.setCustomNameVisible(true);
        AttributeInstance vida = ent.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(c.vida);
            ent.setHealth(c.vida);
        }
        if (c == Criatura.SENHOR_DAS_MARES) {
            AttributeInstance escala = ent.getAttribute(Attribute.SCALE);
            if (escala != null) escala.setBaseValue(1.8);
            AttributeInstance dano = ent.getAttribute(Attribute.ATTACK_DAMAGE);
            if (dano != null) dano.setBaseValue(dano.getBaseValue() + 5);
        }
        if (ent instanceof Drowned d && c != Criatura.AFOGADO_FAMINTO) {
            EntityEquipment eq = d.getEquipment();
            eq.setItemInMainHand(new ItemStack(Material.TRIDENT));
            eq.setItemInMainHandDropChance(0);
            eq.setChestplate(new ItemStack(c == Criatura.SENHOR_DAS_MARES ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE));
            eq.setChestplateDropChance(0);
        }
        ent.getPersistentDataContainer().set(kCriatura, PersistentDataType.STRING, c.name());
        if (ent instanceof org.bukkit.entity.Mob m) m.setTarget(p);
        Vector v = p.getLocation().toVector().subtract(onde.toVector());
        if (v.lengthSquared() > 1e-4) ent.setVelocity(v.normalize().multiply(0.6).setY(0.5));
        w.spawnParticle(Particle.SPLASH, onde, 60, 0.6, 0.4, 0.6);
        w.playSound(onde, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.6f);
        p.showTitle(Title.title(Component.text("Algo enorme mordeu a isca!", COR, TextDecoration.BOLD),
                Component.text(c.nome, NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(400))));
    }

    @EventHandler
    public void aoMorrerCriatura(EntityDeathEvent e) {
        String id = e.getEntity().getPersistentDataContainer().get(kCriatura, PersistentDataType.STRING);
        if (id == null) return;
        Criatura c;
        try { c = Criatura.valueOf(id); } catch (IllegalArgumentException ex) { return; }
        Player p = e.getEntity().getKiller();
        if (rnd().nextDouble() < c.chanceEscama) e.getDrops().add(Reagente.ESCAMA_DO_ABISMO.criar(c.escamas));
        if (rnd().nextDouble() < c.chancePerola) e.getDrops().add(Reagente.PEROLA_NEGRA.criar(1));
        if (p == null) return;
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.PESCA, c.xp);
        plugin.titulos().registrar(p, "criaturas_marinhas", 1);
        p.sendMessage(Component.text("⚓ Você venceu o " + c.nome + "!", COR));
    }

    // =====================================================================
    //  Diário do Pescador (/peixes)
    // =====================================================================

    public void abrirDiario(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⚓ Diário do Pescador"));
        Inventory inv = t.inventario;
        int nivel = nivel(p);
        int max = cfg().nivelMaximo;
        Set<String> vistos = vistos(p);
        inv.setItem(S_INFO, item(Material.FISHING_ROD, Component.text("⚓ Pesca Nv " + nivel, COR, TextDecoration.BOLD), List.of(
                Component.text("Peixe raro: " + pct(chanceRaro(nivel)), NamedTextColor.GRAY),
                Component.text("Tesouro do Mar: " + pct(chanceTesouro(nivel)), NamedTextColor.GRAY),
                Component.text("Criatura marinha: " + pct(chanceCriatura(nivel)), NamedTextColor.GRAY),
                Component.text("Isca " + pct(nivel * cfg().pesIscaRapida) + " mais rápida", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Espécies raras: " + vistos.size() + "/" + PeixeRaro.values().length, NamedTextColor.AQUA),
                Component.text("Sorte do Mar na vara ajuda!", NamedTextColor.DARK_GRAY)), false));
        PeixeRaro[] peixes = PeixeRaro.values();
        for (int i = 0; i < peixes.length && i < SLOTS.length; i++) {
            PeixeRaro pr = peixes[i];
            boolean visto = vistos.contains(pr.name());
            boolean liberado = nivel >= pr.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((liberado ? "✔" : "✖") + " Pesca nível " + pr.nivelNecessario(max),
                    liberado ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Onde: ", NamedTextColor.GRAY).append(Component.text(pr.onde(), NamedTextColor.WHITE)));
            if (visto) {
                lore.add(Component.text(pr.uso(), NamedTextColor.DARK_AQUA));
                lore.add(Component.empty());
                lore.add(Component.text("✔ Já pescado", NamedTextColor.GREEN));
            } else {
                lore.add(Component.empty());
                lore.add(Component.text("Ainda não pescado.", NamedTextColor.DARK_GRAY));
            }
            inv.setItem(SLOTS[i], item(visto ? pr.base() : Material.GRAY_DYE,
                    Component.text(visto ? pr.nome() : "??? " + pr.nome(), visto ? pr.cor() : NamedTextColor.DARK_GRAY, TextDecoration.BOLD),
                    lore, visto));
        }
        inv.setItem(31, item(Material.CHEST, Component.text("Tesouros e criaturas", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Tesouros do Mar trazem esmeraldas,", NamedTextColor.GRAY),
                Component.text("livros, diamantes e Pérolas Negras.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Criaturas marinhas lutam de volta:", NamedTextColor.GRAY),
                Component.text(" Afogado Faminto, Guardião das Ondas,", NamedTextColor.WHITE),
                Component.text(" Capitão Afogado e o Senhor das Marés.", NamedTextColor.WHITE),
                Component.text("Deixam Escamas do Abismo e Pérolas.", NamedTextColor.DARK_AQUA)), false));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarDiario(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela)) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p && e.getRawSlot() == S_FECHAR) p.closeInventory();
    }

    @EventHandler
    public void aoArrastarDiario(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilhar) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    /** Para o /rpgadmin. */
    public static String nomes() {
        return String.join("|", Arrays.stream(PeixeRaro.values()).map(p -> p.name().toLowerCase(Locale.ROOT)).toList());
    }
}
