package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bestiário: cada tipo de monstro derrotado conta. Nos marcos (10, 50, 200, 500, 1000)
 * o jogador causa mais dano e leva menos dano daquele tipo. Elites contam 5.
 */
public final class Bestiario implements Listener {

    private static final int[] MARCOS = {10, 50, 200, 500, 1000};
    private static final double DANO_POR_MARCO = 0.03;
    private static final double DEFESA_POR_MARCO = 0.02;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kBestiario;

    public Bestiario(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kBestiario = new NamespacedKey(plugin, "bestiario");
    }

    // ---------- dados no jogador: "ZOMBIE=120,SKELETON=50" ----------

    private Map<EntityType, Integer> ler(Player p) {
        Map<EntityType, Integer> m = new EnumMap<>(EntityType.class);
        String t = p.getPersistentDataContainer().get(kBestiario, PersistentDataType.STRING);
        if (t == null || t.isEmpty()) return m;
        for (String parte : t.split(",")) {
            String[] kv = parte.split("=");
            if (kv.length != 2) continue;
            try { m.put(EntityType.valueOf(kv[0]), Integer.parseInt(kv[1])); } catch (IllegalArgumentException ignored) { }
        }
        return m;
    }

    private void gravar(Player p, Map<EntityType, Integer> m) {
        StringBuilder sb = new StringBuilder();
        m.forEach((t, v) -> sb.append(sb.isEmpty() ? "" : ",").append(t.name()).append('=').append(v));
        p.getPersistentDataContainer().set(kBestiario, PersistentDataType.STRING, sb.toString());
    }

    public static int marco(int abates) {
        int n = 0;
        for (int m : MARCOS) if (abates >= m) n++;
        return n;
    }

    private int marco(Player p, EntityType t) {
        return marco(ler(p).getOrDefault(t, 0));
    }

    private static boolean conta(Entity e) {
        return e instanceof Enemy && !(e instanceof Player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        Player p = m.getKiller();
        if (p == null || !conta(m) || m.getPersistentDataContainer().has(Perigo.CHAVE_LACAIO)) return;
        Map<EntityType, Integer> dados = ler(p);
        int antes = dados.getOrDefault(m.getType(), 0);
        int depois = antes + (Perigo.ehElite(m) ? 5 : 1);
        dados.put(m.getType(), depois);
        gravar(p, dados);
        int ma = marco(antes), md = marco(depois);
        if (md > ma) {
            p.sendMessage(Component.text("📖 Bestiário: ", NamedTextColor.GOLD)
                    .append(Component.translatable(m.getType().translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(" — marco " + md + "! +" + Math.round(md * DANO_POR_MARCO * 100) + "% de dano e "
                            + Math.round(md * DEFESA_POR_MARCO * 100) + "% menos dano contra eles.", NamedTextColor.YELLOW)));
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.4f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDano(EntityDamageByEntityEvent e) {
        Entity atacante = e.getDamager();
        if (atacante instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) atacante = atirador;
        if (atacante instanceof Player p && conta(e.getEntity())) {
            int m = marco(p, e.getEntity().getType());
            if (m > 0) e.setDamage(e.getDamage() * (1 + m * DANO_POR_MARCO));
        } else if (e.getEntity() instanceof Player p && conta(atacante)) {
            int m = marco(p, atacante.getType());
            if (m > 0) e.setDamage(e.getDamage() * (1 - m * DEFESA_POR_MARCO));
        }
    }

    // =====================================================================
    //  Menu (/bestiario)
    // =====================================================================

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("📖 Bestiário"));
        Inventory inv = t.inventario;
        Map<EntityType, Integer> dados = ler(p);
        Perigo perigo = plugin.perigo();
        int total = dados.values().stream().mapToInt(Integer::intValue).sum();
        inv.setItem(4, item(Material.WRITABLE_BOOK, Component.text("📖 Bestiário", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Monstros derrotados: " + total, NamedTextColor.WHITE),
                Component.text("Espécies: " + dados.size(), NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Marcos: 10, 50, 200, 500, 1000 abates.", NamedTextColor.GRAY),
                Component.text("Cada marco: +3% de dano e -2% de dano", NamedTextColor.GRAY),
                Component.text("recebido contra aquele monstro. Elites contam 5.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("☠ Mundo na semana " + perigo.semanas() + ": monstros +" + Math.round(perigo.forca() * 100) + "% mais fortes,", NamedTextColor.RED),
                Component.text("  " + String.format(Locale.ROOT, "%.1f", perigo.chanceElite() * 100).replace('.', ',') + "% de chance de Elite.", NamedTextColor.RED)), false));
        List<Map.Entry<EntityType, Integer>> lista = new ArrayList<>(dados.entrySet());
        lista.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int slot = 9;
        for (Map.Entry<EntityType, Integer> en : lista) {
            if (slot >= 45) break;
            int abates = en.getValue(), m = marco(abates);
            int prox = m < MARCOS.length ? MARCOS[m] : -1;
            Material ovo = Material.matchMaterial(en.getKey().name() + "_SPAWN_EGG");
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Abates: " + abates, NamedTextColor.WHITE));
            lore.add(Component.text("Marco: " + m + " / " + MARCOS.length, m > 0 ? NamedTextColor.GOLD : NamedTextColor.GRAY));
            if (m > 0) lore.add(Component.text("+" + Math.round(m * DANO_POR_MARCO * 100) + "% dano · -" + Math.round(m * DEFESA_POR_MARCO * 100)
                    + "% dano recebido", NamedTextColor.GREEN));
            if (prox > 0) lore.add(Component.text("Próximo marco: " + prox, NamedTextColor.DARK_GRAY));
            ItemStack icone = new ItemStack(ovo == null ? Material.ZOMBIE_HEAD : ovo);
            icone.editMeta(meta -> {
                meta.displayName(Component.translatable(en.getKey().translationKey(), m >= MARCOS.length ? NamedTextColor.GOLD : NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                meta.addItemFlags(ItemFlag.values());
                if (m >= MARCOS.length) meta.setEnchantmentGlintOverride(true);
            });
            inv.setItem(slot++, icone);
        }
        if (lista.isEmpty()) inv.setItem(22, item(Material.BOOK, Component.text("Nenhum monstro ainda", NamedTextColor.GRAY), List.of(
                Component.text("Derrote monstros para preencher.", NamedTextColor.DARK_GRAY)), false));
        inv.setItem(49, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela)) return;
        e.setCancelled(true);
        if (e.getWhoClicked() instanceof Player p && e.getRawSlot() == 49) p.closeInventory();
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilho) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilho) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }
}
