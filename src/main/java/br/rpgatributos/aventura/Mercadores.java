package br.rpgatributos.aventura;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NPCs invocados no altar: o Mercador Arcano (trocas raras) e o Caçador de
 * Recompensas (contratos para matar chefes). Os dois ficam 10 minutos.
 */
public final class Mercadores implements Listener {

    public static final NamespacedKey CHAVE_NPC = new NamespacedKey("rpgatributos", "npc");
    private static final long DURACAO_TICKS = 20L * 60 * 10;
    private static final int[] SLOTS_CONTRATOS = {10, 11, 12, 14, 15, 16};
    private static final int C_INFO = 4;
    private static final int C_ABANDONAR = 31;
    private static final int C_FECHAR = 40;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kContrato;
    private final Set<UUID> npcs = new HashSet<>();

    public Mercadores(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kContrato = new NamespacedKey(plugin, "contrato");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Mercador Arcano
    // =====================================================================

    public void invocarMercador(Location altar, Player quem) {
        Location onde = altar.clone().add(0.5, 1.05, 0.5);
        WanderingTrader m = onde.getWorld().spawn(onde, WanderingTrader.class, t -> {
            t.customName(Component.text("✦ Mercador Arcano ✦", NamedTextColor.GREEN, TextDecoration.BOLD));
            t.setCustomNameVisible(true);
            t.setAI(false);
            t.setInvulnerable(true);
            t.setPersistent(false);
            t.setRemoveWhenFarAway(false);
            t.getPersistentDataContainer().set(CHAVE_NPC, PersistentDataType.STRING, "mercador");
            t.setRecipes(trocas());
        });
        registrar(m, NamedTextColor.GREEN, "O Mercador Arcano chegou! Ele fica 10 minutos.");
    }

    private List<MerchantRecipe> trocas() {
        List<MerchantRecipe> fixas = new ArrayList<>(List.of(
                troca(Raro.PEDRA_DE_PROTECAO.criar(), 3, new ItemStack(Material.EMERALD, 32)),
                troca(Raro.FRAGMENTO_DE_FORJA.criar(), 4, new ItemStack(Material.EMERALD, 16), new ItemStack(Material.DIAMOND, 4))));
        List<MerchantRecipe> sorteio = new ArrayList<>(List.of(
                troca(Raro.ESSENCIA_PRIMORDIAL.criar(), 1, new ItemStack(Material.EMERALD, 64), new ItemStack(Material.DIAMOND, 8)),
                troca(new ItemStack(Material.TOTEM_OF_UNDYING), 1, new ItemStack(Material.EMERALD, 40)),
                troca(new ItemStack(Material.SHULKER_SHELL, 2), 2, new ItemStack(Material.EMERALD, 20)),
                troca(new ItemStack(Material.ECHO_SHARD, 2), 2, new ItemStack(Material.EMERALD, 16)),
                troca(new ItemStack(Material.EXPERIENCE_BOTTLE, 16), 4, new ItemStack(Material.EMERALD, 10)),
                troca(new ItemStack(Material.GOLDEN_APPLE, 2), 3, new ItemStack(Material.EMERALD, 12)),
                troca(new ItemStack(Material.NETHERITE_SCRAP), 2, new ItemStack(Material.EMERALD, 24), new ItemStack(Material.DIAMOND, 2)),
                troca(livro(Enchantment.MENDING, 1), 1, new ItemStack(Material.EMERALD, 30), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.UNBREAKING, 3), 1, new ItemStack(Material.EMERALD, 20), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.EFFICIENCY, 5), 1, new ItemStack(Material.EMERALD, 24), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.SHARPNESS, 5), 1, new ItemStack(Material.EMERALD, 24), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.PROTECTION, 4), 1, new ItemStack(Material.EMERALD, 24), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.FORTUNE, 3), 1, new ItemStack(Material.EMERALD, 28), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.LOOTING, 3), 1, new ItemStack(Material.EMERALD, 28), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.POWER, 5), 1, new ItemStack(Material.EMERALD, 24), new ItemStack(Material.BOOK)),
                troca(livro(Enchantment.FEATHER_FALLING, 4), 1, new ItemStack(Material.EMERALD, 20), new ItemStack(Material.BOOK))));
        Collections.shuffle(sorteio);
        fixas.addAll(sorteio.subList(0, 6));
        return fixas;
    }

    private static MerchantRecipe troca(ItemStack resultado, int usos, ItemStack... ingredientes) {
        MerchantRecipe r = new MerchantRecipe(resultado, 0, usos, false);
        for (ItemStack i : ingredientes) r.addIngredient(i);
        return r;
    }

    private static ItemStack livro(Enchantment e, int nivel) {
        ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
        livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(e, nivel, true));
        return livro;
    }

    // =====================================================================
    //  Caçador de Recompensas e contratos
    // =====================================================================

    public void invocarCacador(Location altar, Player quem) {
        Location onde = altar.clone().add(0.5, 1.05, 0.5);
        Villager v = onde.getWorld().spawn(onde, Villager.class, c -> {
            c.customName(Component.text("☠ Caçador de Recompensas ☠", NamedTextColor.GOLD, TextDecoration.BOLD));
            c.setCustomNameVisible(true);
            c.setProfession(Villager.Profession.WEAPONSMITH);
            c.setVillagerLevel(5);
            c.setAI(false);
            c.setInvulnerable(true);
            c.setPersistent(false);
            c.setRemoveWhenFarAway(false);
            c.getPersistentDataContainer().set(CHAVE_NPC, PersistentDataType.STRING, "cacador");
        });
        registrar(v, NamedTextColor.GOLD, "O Caçador de Recompensas chegou! Clique nele para ver os contratos (10 minutos).");
    }

    private void registrar(Entity npc, NamedTextColor cor, String aviso) {
        npcs.add(npc.getUniqueId());
        Location l = npc.getLocation();
        l.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, l.clone().add(0, 1, 0), 40, 0.5, 1, 0.5);
        l.getWorld().playSound(l, Sound.ENTITY_WANDERING_TRADER_YES, 1.5f, 1f);
        for (Player p : l.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(l) < 60 * 60) p.sendMessage(Component.text("✦ " + aviso, cor));
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!npc.isValid()) return;
            npc.getWorld().spawnParticle(Particle.POOF, npc.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.02);
            npc.remove();
            npcs.remove(npc.getUniqueId());
        }, DURACAO_TICKS);
    }

    public Chefe contrato(Player p) {
        return Chefe.porId(p.getPersistentDataContainer().getOrDefault(kContrato, PersistentDataType.STRING, ""));
    }

    private static int premioEsmeraldas(Chefe c) { return 16 * c.nivel(); }

    private static String premioTexto(Chefe c) {
        return premioEsmeraldas(c) + " esmeraldas, " + c.nivel() + " Fragmento(s) de Forja"
                + (c.nivel() >= 3 ? ", 1 Pedra de Proteção" : "") + " e muito XP de Combate";
    }

    /** Chamado quando um chefe morre: paga o contrato de quem participou. */
    public void contratoCumprido(Player p, Chefe c) {
        if (contrato(p) != c) return;
        p.getPersistentDataContainer().remove(kContrato);
        List<ItemStack> premio = new ArrayList<>(List.of(
                new ItemStack(Material.EMERALD, premioEsmeraldas(c)),
                Raro.FRAGMENTO_DE_FORJA.criar(c.nivel())));
        if (c.nivel() >= 3) premio.add(Raro.PEDRA_DE_PROTECAO.criar());
        for (ItemStack i : premio) {
            p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
            plugin.stats().darXp(p, Skill.COMBATE, 200.0 * c.nivel());
        }
        plugin.titulos().registrar(p, "contratos", 1);
        p.showTitle(Title.title(Component.text("☠ Contrato cumprido! ☠", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(c.nome(), c.cor()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        p.sendMessage(Component.text("☠ Recompensa do contrato: " + premioTexto(c) + ".", NamedTextColor.GOLD));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
    }

    private void abrirContratos(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 45, Component.text("☠ Contratos"));
        desenhar(p, t.inventario);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 0.8f);
    }

    private void desenhar(Player p, Inventory inv) {
        inv.clear();
        Chefe atual = contrato(p);
        inv.setItem(C_INFO, item(Material.CROSSBOW, Component.text("Caçador de Recompensas", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Aceite um contrato e derrote o chefe", NamedTextColor.GRAY),
                Component.text("no Altar Ritualístico. A recompensa", NamedTextColor.GRAY),
                Component.text("chega na hora, onde você estiver.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Contrato atual: ", NamedTextColor.GRAY).append(atual == null
                        ? Component.text("nenhum", NamedTextColor.DARK_GRAY)
                        : Component.text(atual.nome(), atual.cor()))), false));
        Chefe[] chefes = Chefe.values();
        for (int i = 0; i < chefes.length && i < SLOTS_CONTRATOS.length; i++) {
            Chefe c = chefes[i];
            boolean este = c == atual;
            inv.setItem(SLOTS_CONTRATOS[i], item(Material.PAPER, Component.text("Contrato: " + c.nome(), c.cor(), TextDecoration.BOLD), List.of(
                    Component.text("Dificuldade " + c.estrelas(), NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Recompensa:", NamedTextColor.GRAY),
                    Component.text(" " + premioTexto(c), NamedTextColor.GREEN),
                    Component.empty(),
                    este ? Component.text("✔ Contrato aceito", NamedTextColor.GREEN)
                            : Component.text(atual == null ? "» Clique para aceitar" : "Termine ou abandone o contrato atual",
                            atual == null ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)), este));
        }
        if (atual != null) {
            inv.setItem(C_ABANDONAR, item(Material.RED_DYE, Component.text("Abandonar contrato", NamedTextColor.RED), List.of(), false));
        }
        inv.setItem(C_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarNpc(PlayerInteractEntityEvent e) {
        String tipo = e.getRightClicked().getPersistentDataContainer().get(CHAVE_NPC, PersistentDataType.STRING);
        if (!"cacador".equals(tipo)) return;
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) abrirContratos(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == C_FECHAR) { p.closeInventory(); return; }
        if (slot == C_ABANDONAR && contrato(p) != null) {
            p.getPersistentDataContainer().remove(kContrato);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.6f);
            desenhar(p, topo);
            return;
        }
        Chefe[] chefes = Chefe.values();
        for (int i = 0; i < chefes.length && i < SLOTS_CONTRATOS.length; i++) {
            if (SLOTS_CONTRATOS[i] != slot) continue;
            if (contrato(p) != null) {
                p.sendMessage(Component.text("Você já tem um contrato. Termine ou abandone ele primeiro.", NamedTextColor.RED));
                return;
            }
            p.getPersistentDataContainer().set(kContrato, PersistentDataType.STRING, chefes[i].id());
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            p.sendMessage(Component.text("☠ Contrato aceito: derrote ", NamedTextColor.GOLD)
                    .append(Component.text(chefes[i].nome(), chefes[i].cor(), TextDecoration.BOLD))
                    .append(Component.text(" no Altar Ritualístico.", NamedTextColor.GOLD)));
            desenhar(p, topo);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoFerirNpc(EntityDamageEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(CHAVE_NPC)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTransformarNpc(EntityTransformEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(CHAVE_NPC)) e.setCancelled(true);
    }

    public void removerTodos() {
        for (UUID id : npcs) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        npcs.clear();
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
}
