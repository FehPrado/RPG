package br.rpgatributos.fe;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runas: a Mesa Rúnica grava uma runa num equipamento; aqui ficam os efeitos de cada uma
 * e o menu de gravar.
 */
public final class Runas implements Listener {

    public static final TextColor COR = TextColor.color(0x80CBC4);
    public static final NamespacedKey K_RUNA = new NamespacedKey("rpgatributos", "runa");
    private static final String MARCA = "◈ ";
    private static final int S_INFO = 13, S_FECHAR = 49;
    private static final EquipmentSlot[] PECAS = {EquipmentSlot.HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int[] S_PECAS = {2, 3, 4, 5, 6};
    private static final int[] S_RUNAS = {29, 30, 31, 32, 33, 38, 39, 40, 41, 42};
    private static final Set<Material> FUNDIVEIS = Set.of(Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER, Material.SAND,
            Material.RED_SAND, Material.COBBLESTONE, Material.COBBLED_DEEPSLATE, Material.ANCIENT_DEBRIS, Material.CLAY_BALL, Material.WET_SPONGE);

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location mesa;
        int peca = 0;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kImpeto;
    private final Map<UUID, Boolean> pulando = new HashMap<>();
    private final Set<UUID> saltoUsado = new HashSet<>();
    private final Map<UUID, Long> esperaProtecao = new HashMap<>();
    private final Map<UUID, Long> esperaExplosao = new HashMap<>();
    private Map<Material, Material> fundir;
    private boolean aplicando;
    private int ciclo;

    public Runas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kImpeto = new NamespacedKey(plugin, "runa_impeto");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    public static Runa runa(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return Runa.porId(i.getItemMeta().getPersistentDataContainer().getOrDefault(K_RUNA, PersistentDataType.STRING, ""));
    }

    private Runa runaEm(Player p, EquipmentSlot slot) {
        return runa(p.getInventory().getItem(slot));
    }

    /** Linha da runa no texto do item (chamado também quando a Forja refaz a descrição). */
    public void decorar(ItemMeta meta) {
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c).startsWith(MARCA));
        Runa r = Runa.porId(meta.getPersistentDataContainer().getOrDefault(K_RUNA, PersistentDataType.STRING, ""));
        if (r != null) lore.add(Component.text(MARCA + r.simbolo() + " " + r.nome() + ": " + r.descricao(), r.cor()).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }

    // =====================================================================
    //  Efeitos
    // =====================================================================

    /** Pulo duplo disponível agora? (a Capa Planadora espera o pulo duplo ser usado) */
    public boolean saltoDisponivel(Player p) {
        return runaEm(p, EquipmentSlot.FEET) == Runa.SALTO && !saltoUsado.contains(p.getUniqueId()) && !p.isOnGround()
                && !p.isInWater() && !p.isFlying() && !p.isGliding() && !p.isInsideVehicle();
    }

    @EventHandler
    public void aoApertar(PlayerInputEvent e) {
        Player p = e.getPlayer();
        boolean pula = e.getInput().isJump();
        Boolean antes = pulando.put(p.getUniqueId(), pula);
        if (!pula || Boolean.TRUE.equals(antes) || !saltoDisponivel(p)) return;
        saltoUsado.add(p.getUniqueId());
        org.bukkit.util.Vector olhar = p.getLocation().getDirection().setY(0);
        if (olhar.lengthSquared() > 1e-4) olhar.normalize().multiply(0.3);
        p.setVelocity(new org.bukkit.util.Vector(olhar.getX(), 0.65, olhar.getZ()));
        p.setFallDistance(0);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 10, 0.3, 0.05, 0.3, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.6f);
    }

    /** A cada 2 ticks: libera o pulo duplo ao tocar o chão; a cada 2 s: Visão e Ímpeto. */
    public void tick() {
        ciclo++;
        for (Iterator<UUID> it = saltoUsado.iterator(); it.hasNext(); ) {
            Player p = Bukkit.getPlayer(it.next());
            if (p == null || p.isOnGround() || p.isInWater()) it.remove();
        }
        if (ciclo % 20 != 0) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (runaEm(p, EquipmentSlot.HEAD) == Runa.VISAO && p.getEyeLocation().getBlock().getLightLevel() < 7) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false, true));
            }
            AttributeInstance vel = p.getAttribute(Attribute.MOVEMENT_SPEED);
            if (vel == null) continue;
            boolean impeto = runaEm(p, EquipmentSlot.LEGS) == Runa.IMPETO;
            boolean tem = vel.getModifier(kImpeto) != null;
            if (impeto && !tem) vel.addModifier(new AttributeModifier(kImpeto, 0.08, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
            else if (!impeto && tem) vel.removeModifier(kImpeto);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || runaEm(p, EquipmentSlot.CHEST) != Runa.PROTECAO) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max == null || p.getHealth() - e.getFinalDamage() > max.getValue() * 0.3) return;
        long t = System.currentTimeMillis();
        Long espera = esperaProtecao.get(p.getUniqueId());
        if (espera != null && espera > t) return;
        esperaProtecao.put(p.getUniqueId(), t + 60_000);
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1));
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.5f);
        p.sendActionBar(Component.text("ᛉ A Runa da Proteção brilha!", Runa.PROTECAO.cor()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        if (aplicando || !(e.getEntity() instanceof LivingEntity alvo)) return;
        Player p;
        Runa r;
        if (e.getDamager() instanceof Player pl) {
            p = pl;
            r = runa(pl.getInventory().getItemInMainHand());
        } else if (e.getDamager() instanceof Arrow a && a.getShooter() instanceof Player pl) {
            p = pl;
            r = runa(pl.getInventory().getItemInMainHand());
            if (r != Runa.TEMPESTADE) return;
        } else return;
        if (r == null || alvo == p) return;
        aplicando = true;
        try {
            switch (r) {
                case EXPLOSAO -> {
                    long t = System.currentTimeMillis();
                    Long espera = esperaExplosao.get(p.getUniqueId());
                    if ((espera == null || espera < t) && rnd().nextDouble() < 0.08) {
                        esperaExplosao.put(p.getUniqueId(), t + 1500);
                        Location c = alvo.getLocation();
                        c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0);
                        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
                        for (Entity x : c.getWorld().getNearbyEntities(c, 2.5, 2.5, 2.5)) {
                            if (x instanceof LivingEntity le && x != p && !(x instanceof Player o && !plugin.pvpPermitido(p, o))
                                    && !br.rpgatributos.domador.Companheiros.eh(x)) le.damage(5, p);
                        }
                    }
                }
                case GELO -> {
                    if (rnd().nextDouble() < 0.15) {
                        alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
                        alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), 140));
                        alvo.getWorld().spawnParticle(Particle.SNOWFLAKE, alvo.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                    }
                }
                case TEMPESTADE -> {
                    if (rnd().nextDouble() < 0.06) {
                        alvo.getWorld().strikeLightningEffect(alvo.getLocation());
                        alvo.damage(6, p);
                    }
                }
                case VAMPIRICA -> {
                    AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                    if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + e.getFinalDamage() * 0.08));
                }
                default -> { }
            }
        } finally {
            aplicando = false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockDropItemEvent e) {
        if (runa(e.getPlayer().getInventory().getItemInMainHand()) != Runa.FUSAO) return;
        if (fundir == null) montarFundicao();
        boolean fundiu = false;
        for (Item item : e.getItems()) {
            ItemStack s = item.getItemStack();
            Material res = fundir.get(s.getType());
            if (res == null) continue;
            item.setItemStack(new ItemStack(res, s.getAmount()));
            fundiu = true;
        }
        if (fundiu) e.getBlock().getWorld().spawnParticle(Particle.FLAME, e.getBlock().getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.01);
    }

    private void montarFundicao() {
        fundir = new EnumMap<>(Material.class);
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (!(it.next() instanceof FurnaceRecipe f) || !(f.getInputChoice() instanceof RecipeChoice.MaterialChoice mc)) continue;
            for (Material m : mc.getChoices()) {
                if (FUNDIVEIS.contains(m) || m.name().endsWith("_ORE")) fundir.putIfAbsent(m, f.getResult().getType());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoAtirar(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || runa(e.getBow()) != Runa.ECO || !(e.getProjectile() instanceof AbstractArrow original)) return;
        org.bukkit.util.Vector vel = original.getVelocity().clone();
        double dano = original.getDamage() * 0.5;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Arrow eco = p.getWorld().spawnArrow(p.getEyeLocation(), vel, (float) vel.length(), 0f);
            eco.setShooter(p);
            eco.setDamage(dano);
            eco.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            p.getWorld().spawnParticle(Particle.WITCH, p.getEyeLocation(), 6, 0.2, 0.2, 0.2, 0.02);
        }, 5L);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        pulando.remove(e.getPlayer().getUniqueId());
        saltoUsado.remove(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Mesa Rúnica
    // =====================================================================

    public void abrirMenu(Player p, Location mesa) {
        Tela t = new Tela();
        t.mesa = mesa;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("ᚱ Mesa Rúnica"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private static final String[] NOMES_PECAS = {"Mão (arma ou ferramenta)", "Capacete", "Peitoral", "Calça", "Botas"};

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        for (int i = 0; i < PECAS.length; i++) {
            ItemStack peca = p.getInventory().getItem(PECAS[i]);
            List<Component> lore = new ArrayList<>();
            Runa atual = runa(peca);
            lore.add(Component.text(atual == null ? "Sem runa" : "Runa: " + atual.nome(), atual == null ? NamedTextColor.DARK_GRAY : atual.cor()));
            lore.add(t.peca == i ? Component.text("● Selecionado", NamedTextColor.GREEN) : Component.text("» Clique para escolher", NamedTextColor.YELLOW));
            ItemStack icone = peca == null || peca.isEmpty() ? new ItemStack(Material.GRAY_STAINED_GLASS_PANE) : new ItemStack(peca.getType());
            ItemStack it = item(icone.getType(), Component.text(NOMES_PECAS[i], NamedTextColor.WHITE, TextDecoration.BOLD), lore);
            if (t.peca == i) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_PECAS[i], it);
        }
        ItemStack sel = p.getInventory().getItem(PECAS[t.peca]);
        inv.setItem(S_INFO, item(Material.ENCHANTED_BOOK, Component.text("ᚱ Gravar runas", COR, TextDecoration.BOLD), List.of(
                Component.text("1. Escolha a peça (em cima).", NamedTextColor.GRAY),
                Component.text("2. Clique na runa (embaixo).", NamedTextColor.GRAY),
                Component.text("Cada peça tem uma runa; gravar outra troca.", NamedTextColor.GRAY),
                Component.text("Gasta os materiais e níveis de XP.", NamedTextColor.DARK_GRAY))));
        Runa[] todas = Runa.values();
        for (int i = 0; i < todas.length && i < S_RUNAS.length; i++) {
            Runa r = todas[i];
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(r.descricao(), NamedTextColor.WHITE));
            List<String> onde = new ArrayList<>();
            for (var c : r.onde()) onde.add(c.nome());
            lore.add(Component.text("Vai em: " + String.join(", ", onde), NamedTextColor.GRAY));
            lore.add(Component.empty());
            for (Map.Entry<Material, Integer> c : r.custo().entrySet()) {
                int tem = contar(p, c.getKey());
                boolean ok = tem >= c.getValue();
                lore.add(Component.text((ok ? "✔ " : "✖ ") + c.getValue() + "x " + nome(c.getKey()) + " (" + tem + ")", ok ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            boolean xp = p.getLevel() >= r.niveis() || p.getGameMode() == GameMode.CREATIVE;
            lore.add(Component.text((xp ? "✔ " : "✖ ") + r.niveis() + " níveis de XP", xp ? NamedTextColor.GREEN : NamedTextColor.RED));
            boolean serve = sel != null && !sel.isEmpty() && r.serve(sel.getType());
            lore.add(Component.empty());
            lore.add(serve ? Component.text("» Clique para gravar na peça escolhida", NamedTextColor.YELLOW)
                    : Component.text("Não vai na peça escolhida.", NamedTextColor.DARK_GRAY));
            inv.setItem(S_RUNAS[i], item(serve ? Material.ENCHANTED_BOOK : Material.BOOK,
                    Component.text(r.simbolo() + " " + r.nome(), r.cor(), TextDecoration.BOLD), lore));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static boolean comum(ItemStack i, Material m) {
        return i != null && i.getType() == m && !(i.hasItemMeta() && i.getItemMeta().hasItemName());
    }

    private static int contar(Player p, Material m) {
        int n = 0;
        for (ItemStack i : p.getInventory().getStorageContents()) if (comum(i, m)) n += i.getAmount();
        return n;
    }

    private static void tirar(Player p, Material m, int qtd) {
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int k = 0; k < c.length && qtd > 0; k++) {
            if (!comum(c[k], m)) continue;
            int tira = Math.min(qtd, c[k].getAmount());
            c[k].setAmount(c[k].getAmount() - tira);
            qtd -= tira;
        }
        p.getInventory().setStorageContents(c);
    }

    private static String nome(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** @return motivo de não ter gravado, ou null. */
    private String gravar(Player p, int peca, Runa r) {
        ItemStack item = p.getInventory().getItem(PECAS[peca]);
        if (item == null || item.isEmpty()) return "Não há nada nesse lugar.";
        if (!r.serve(item.getType())) return "A " + r.nome() + " não vai nessa peça.";
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            if (p.getLevel() < r.niveis()) return "Precisa de " + r.niveis() + " níveis de XP.";
            for (Map.Entry<Material, Integer> c : r.custo().entrySet()) {
                if (contar(p, c.getKey()) < c.getValue()) return "Faltam materiais: " + c.getValue() + "x " + nome(c.getKey()) + ".";
            }
            for (Map.Entry<Material, Integer> c : r.custo().entrySet()) tirar(p, c.getKey(), c.getValue());
            p.setLevel(p.getLevel() - r.niveis());
        }
        item.editMeta(m -> {
            m.getPersistentDataContainer().set(K_RUNA, PersistentDataType.STRING, r.id());
            decorar(m);
        });
        p.getInventory().setItem(PECAS[peca], item);
        plugin.titulos().registrar(p, "runas", 1);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.2, 0), 60, 0.5, 0.6, 0.5, 0.8);
        p.sendMessage(Component.text(r.simbolo() + " " + r.nome() + " gravada!", r.cor()));
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        if (!plugin.mesasRunicas().eh(t.mesa.getBlock())) {
            p.closeInventory();
            return;
        }
        for (int i = 0; i < S_PECAS.length; i++) {
            if (S_PECAS[i] != slot) continue;
            t.peca = i;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            desenhar(p, t);
            return;
        }
        Runa[] todas = Runa.values();
        for (int i = 0; i < todas.length && i < S_RUNAS.length; i++) {
            if (S_RUNAS[i] != slot) continue;
            String erro = gravar(p, t.peca, todas[i]);
            if (erro != null) {
                p.sendMessage(Component.text(erro, NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            }
            desenhar(p, t);
            return;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
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
}
