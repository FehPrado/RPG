package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.perigo.Perigo;
import br.rpgatributos.pesca.PeixeRaro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Álbum de Cartas (/album): monstros, chefes e peixes raros às vezes deixam uma carta (5% delas
 * são brilhantes). Clique com a carta na mão para guardar. Completar uma página dá um bônus
 * permanente; 5 cartas repetidas trocam por uma que falta.
 */
public final class Album implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x4FC3F7);
    public static final NamespacedKey K_CARTA = new NamespacedKey("rpgatributos", "carta");
    public static final NamespacedKey K_BRILHANTE = new NamespacedKey("rpgatributos", "carta_brilhante");
    private static final TextColor[] CORES = {TextColor.color(0xBDBDBD), TextColor.color(0x4FC3F7), TextColor.color(0xCE93D8)};
    private static final String[] RARIDADES = {"Comum", "Rara", "Épica"};
    private static final int S_INFO = 8, S_BONUS = 45, S_TROCAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Carta.Pagina pagina;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kAlbum, kBrilhantes;

    public Album(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kAlbum = new NamespacedKey(plugin, "album");
        this.kBrilhantes = new NamespacedKey(plugin, "album_brilhantes");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Item
    // =====================================================================

    public static ItemStack carta(Carta c, boolean brilhante) {
        ItemStack i = new ItemStack(Material.PAPER);
        int r = c.raridade();
        i.editMeta(m -> {
            m.itemName(Component.text((brilhante ? "✦ " : "") + "Carta: " + c.nome(), brilhante ? NamedTextColor.GOLD : CORES[r]));
            m.lore(List.of(Component.text("Página: " + c.pagina().nome(), NamedTextColor.GRAY),
                            Component.text("Raridade: " + RARIDADES[r] + (brilhante ? " · Brilhante" : ""), brilhante ? NamedTextColor.GOLD : CORES[r]),
                            Component.empty(),
                            Component.text("» Clique com ela na mão para guardar", NamedTextColor.YELLOW),
                            Component.text("no seu álbum (/album).", NamedTextColor.YELLOW))
                    .stream().map(x -> x.decoration(TextDecoration.ITALIC, false)).toList());
            if (brilhante) m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_CARTA, PersistentDataType.STRING, c.name());
            if (brilhante) m.getPersistentDataContainer().set(K_BRILHANTE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, brilhante ? "carta_brilhante" : "carta_" + new String[]{"comum", "rara", "epica"}[r]);
        });
        return i;
    }

    public static Carta de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_CARTA, PersistentDataType.STRING);
        return s == null ? null : Carta.porId(s);
    }

    public static boolean brilhante(ItemStack i) {
        return i != null && i.hasItemMeta() && i.getPersistentDataContainer().has(K_BRILHANTE);
    }

    // =====================================================================
    //  Dados
    // =====================================================================

    private Map<Carta, Integer> colecao(Player p) {
        Map<Carta, Integer> m = new EnumMap<>(Carta.class);
        String s = p.getPersistentDataContainer().get(kAlbum, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return m;
        for (String par : s.split(",")) {
            String[] kv = par.split(":");
            Carta c = kv.length == 2 ? Carta.porId(kv[0]) : null;
            if (c == null) continue;
            try {
                m.put(c, Integer.parseInt(kv[1]));
            } catch (NumberFormatException ignorado) {
                // valor inválido
            }
        }
        return m;
    }

    private void gravar(Player p, Map<Carta, Integer> m) {
        List<String> l = new ArrayList<>();
        m.forEach((c, n) -> { if (n > 0) l.add(c.id() + ":" + n); });
        p.getPersistentDataContainer().set(kAlbum, PersistentDataType.STRING, String.join(",", l));
    }

    private Set<String> brilhantes(Player p) {
        String s = p.getPersistentDataContainer().get(kBrilhantes, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    public boolean completa(Player p, Carta.Pagina pg) {
        Map<Carta, Integer> m = colecao(p);
        for (Carta c : pg.cartas()) if (m.getOrDefault(c, 0) <= 0) return false;
        return true;
    }

    public int total(Player p) {
        int n = 0;
        for (int q : colecao(p).values()) if (q > 0) n++;
        return n;
    }

    /** Guarda a carta no álbum. */
    public void guardar(Player p, Carta c, boolean brilho) {
        Map<Carta, Integer> m = colecao(p);
        boolean nova = m.getOrDefault(c, 0) <= 0;
        boolean paginaAntes = completa(p, c.pagina());
        m.merge(c, 1, Integer::sum);
        gravar(p, m);
        if (brilho) {
            Set<String> b = brilhantes(p);
            if (b.add(c.id())) {
                p.getPersistentDataContainer().set(kBrilhantes, PersistentDataType.STRING, String.join(",", b));
                plugin.titulos().registrar(p, "cartas_brilhantes", 1);
                plugin.segredos().conceder(p, Segredos.Segredo.CARTA_BRILHANTE);
            }
        }
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, nova ? 1.3f : 0.9f);
        p.sendMessage(Component.text("🃏 " + c.nome() + (nova ? " entrou no álbum!" : " (repetida: " + m.get(c) + ")"), nova ? COR : NamedTextColor.GRAY));
        if (nova) plugin.diario().marco(p, "primeira_carta", "Guardou a primeira carta no álbum: " + c.nome());
        if (!paginaAntes && completa(p, c.pagina())) {
            p.showTitle(net.kyori.adventure.title.Title.title(Component.text("🃏 Página completa!", COR, TextDecoration.BOLD),
                    Component.text(c.pagina().nome() + ": " + c.pagina().bonus(), NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            plugin.titulos().registrar(p, "album_paginas", 1);
            plugin.diario().marco(p, "album_" + c.pagina().name().toLowerCase(java.util.Locale.ROOT), "Completou a página " + c.pagina().nome() + " do álbum");
            boolean tudo = true;
            for (Carta.Pagina pg : Carta.Pagina.values()) if (!completa(p, pg)) { tudo = false; break; }
            if (tudo) {
                plugin.titulos().registrar(p, "album_completo", 1);
                plugin.getServer().broadcast(Component.text("🃏 " + p.getName() + " completou o Álbum de Cartas!", COR, TextDecoration.BOLD));
            }
        }
    }

    // =====================================================================
    //  Cartas caindo
    // =====================================================================

    private static void soltar(Entity onde, Carta c) {
        boolean brilho = rnd().nextDouble() < 0.05;
        Item i = onde.getWorld().dropItemNaturally(onde.getLocation(), carta(c, brilho));
        i.setGlowing(true);
        onde.getWorld().playSound(onde.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.5f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || morto.fromMobSpawner()) return;
        Carta c;
        double chance;
        String chefe = morto.getPersistentDataContainer().get(Chefes.CHAVE_CHEFE, PersistentDataType.STRING);
        if (chefe != null) {
            c = Carta.deChave(chefe);
            chance = 0.25;
        } else {
            c = Carta.deTipo(morto.getType());
            if (c == null) return;
            chance = switch (c.raridade()) { case 2 -> 0.10; case 1 -> 0.02; default -> 0.006; };
            if (c == Carta.DRAGAO) chance = 0.5;
            if (Perigo.ehElite(morto)) chance = Math.max(chance, 0.04);
        }
        if (c != null && rnd().nextDouble() < chance * plugin.settings().vidaCartaChance) soltar(morto, c);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoPescar(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof Item item)) return;
        PeixeRaro peixe = PeixeRaro.de(item.getItemStack());
        if (peixe == null) return;
        Carta c = Carta.deChave(peixe.name());
        if (c != null && rnd().nextDouble() < 0.08 * plugin.settings().vidaCartaChance) soltar(e.getPlayer(), c);
        if (plugin.ceu().eclipse()) plugin.segredos().conceder(e.getPlayer(), Segredos.Segredo.PESCA_ECLIPSE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsarCarta(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack mao = e.getPlayer().getInventory().getItemInMainHand();
        Carta c = de(mao);
        if (c == null) return;
        e.setCancelled(true);
        boolean brilho = brilhante(mao);
        mao.setAmount(mao.getAmount() - 1);
        guardar(e.getPlayer(), c, brilho);
    }

    // =====================================================================
    //  Bônus das páginas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        Player p = e.getDamager() instanceof Player pl ? pl
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null || !(e.getEntity() instanceof LivingEntity alvo)) return;
        Carta.Pagina pg;
        if (Chefes.ehChefe(alvo)) pg = Carta.Pagina.CHEFES;
        else {
            Carta c = Carta.deTipo(alvo.getType());
            if (c == null) return;
            pg = c.pagina();
        }
        if (completa(p, pg)) e.setDamage(e.getDamage() * 1.05);
    }

    public double bonusPesca(Player p) {
        return completa(p, Carta.Pagina.PEIXES) ? 0.02 : 0;
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p, Carta.Pagina.MORTOS_VIVOS);
        return true;
    }

    public void abrir(Player p, Carta.Pagina pagina) {
        Tela t = new Tela();
        t.pagina = pagina;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("Álbum · " + pagina.nome(), COR));
        Map<Carta, Integer> col = colecao(p);
        Set<String> brilho = brilhantes(p);
        ItemStack vidro = item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
        for (int i = 0; i < 54; i++) t.inventario.setItem(i, vidro);
        for (Carta.Pagina pg : Carta.Pagina.values()) {
            int tem = 0, total = pg.cartas().size();
            for (Carta c : pg.cartas()) if (col.getOrDefault(c, 0) > 0) tem++;
            t.inventario.setItem(pg.ordinal(), item(pg.icone(), Component.text(pg.nome(), tem == total ? NamedTextColor.GOLD : COR, TextDecoration.BOLD),
                    List.of(Component.text(tem + "/" + total + " cartas", NamedTextColor.YELLOW),
                            Component.text("Completa: " + pg.bonus(), tem == total ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text(pg == pagina ? "● Aberta" : "» Clique para abrir", pg == pagina ? NamedTextColor.GREEN : NamedTextColor.YELLOW)),
                    pg == pagina));
        }
        t.inventario.setItem(S_INFO, item(Material.BOOK, Component.text("🃏 Álbum de Cartas", COR, TextDecoration.BOLD),
                List.of(Component.text(total(p) + "/" + Carta.values().length + " cartas", NamedTextColor.YELLOW),
                        Component.text(brilho.size() + " brilhantes", NamedTextColor.GOLD),
                        Component.empty(),
                        Component.text("Monstros, chefes e peixes raros", NamedTextColor.GRAY),
                        Component.text("às vezes deixam uma carta.", NamedTextColor.GRAY)), false));
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        List<Carta> cartas = pagina.cartas();
        for (int i = 0; i < cartas.size() && i < slots.length; i++) {
            Carta c = cartas.get(i);
            int n = col.getOrDefault(c, 0);
            boolean b = brilho.contains(c.id());
            if (n <= 0) {
                t.inventario.setItem(slots[i], item(Material.GRAY_DYE, Component.text("???", NamedTextColor.DARK_GRAY),
                        List.of(Component.text("Ainda não encontrada.", NamedTextColor.GRAY)), false));
            } else {
                t.inventario.setItem(slots[i], item(c.icone(), Component.text((b ? "✦ " : "") + c.nome(), b ? NamedTextColor.GOLD : CORES[c.raridade()], TextDecoration.BOLD),
                        List.of(Component.text(RARIDADES[c.raridade()] + (b ? " · Brilhante" : ""), b ? NamedTextColor.GOLD : CORES[c.raridade()]),
                                Component.text("Você tem: " + n, NamedTextColor.YELLOW)), b));
            }
        }
        boolean ok = completa(p, pagina);
        t.inventario.setItem(S_BONUS, item(ok ? Material.NETHER_STAR : Material.FIREWORK_STAR,
                Component.text("Bônus da página", ok ? NamedTextColor.GOLD : NamedTextColor.GRAY, TextDecoration.BOLD),
                List.of(Component.text(pagina.bonus(), ok ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                        Component.text(ok ? "✔ Ativo" : "Complete a página para ganhar.", ok ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)), ok));
        int repetidas = 0;
        for (int q : col.values()) repetidas += Math.max(0, q - 1);
        t.inventario.setItem(S_TROCAR, item(Material.HOPPER, Component.text("Trocar repetidas", COR, TextDecoration.BOLD),
                List.of(Component.text("5 cartas repetidas viram 1 carta", NamedTextColor.GRAY),
                        Component.text("que falta (de qualquer página).", NamedTextColor.GRAY),
                        Component.text("Repetidas: " + repetidas, NamedTextColor.YELLOW),
                        Component.empty(),
                        Component.text("» Clique para trocar", NamedTextColor.YELLOW)), false));
        p.openInventory(t.inventario);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilho) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (brilho) meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        if (slot >= 0 && slot < Carta.Pagina.values().length) {
            abrir(p, Carta.Pagina.values()[slot]);
            return;
        }
        if (slot == S_TROCAR) trocar(p, t.pagina);
    }

    private void trocar(Player p, Carta.Pagina voltar) {
        Map<Carta, Integer> col = colecao(p);
        List<Carta> faltam = new ArrayList<>();
        for (Carta c : Carta.values()) if (col.getOrDefault(c, 0) <= 0) faltam.add(c);
        int repetidas = 0;
        for (int q : col.values()) repetidas += Math.max(0, q - 1);
        if (faltam.isEmpty()) {
            p.sendMessage(Component.text("🃏 Você já tem todas as cartas!", COR));
            return;
        }
        if (repetidas < 5) {
            p.sendMessage(Component.text("🃏 Precisa de 5 cartas repetidas (tem " + repetidas + ").", NamedTextColor.RED));
            return;
        }
        int tirar = 5;
        for (Map.Entry<Carta, Integer> en : col.entrySet()) {
            while (tirar > 0 && en.getValue() > 1) {
                en.setValue(en.getValue() - 1);
                tirar--;
            }
        }
        gravar(p, col);
        Carta nova = faltam.get(rnd().nextInt(faltam.size()));
        guardar(p, nova, false);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.2, 0), 30, 0.4, 0.4, 0.4, 0.5);
        abrir(p, voltar);
    }
}
