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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Álbum de Cartas (/album): monstros, chefes, peixes raros, lendas, chefes do mundo e moradores das
 * vilas viram cartas (5% delas brilhantes). Clique com a carta na mão para guardar. Completar uma
 * página dá um bônus permanente. As repetidas viram Pó de Carta, e com o pó você cria a carta que
 * falta. Pacotes de Cartas dão 3 cartas. Marcos (10, 25, 50, 80 cartas) dão prêmios. As cartas
 * também montam o baralho do Duelo de Cartas.
 */
public final class Album implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0x4FC3F7);
    public static final NamespacedKey K_CARTA = new NamespacedKey("rpgatributos", "carta");
    public static final NamespacedKey K_BRILHANTE = new NamespacedKey("rpgatributos", "carta_brilhante");
    public static final NamespacedKey K_PACOTE = new NamespacedKey("rpgatributos", "pacote_cartas");
    static final TextColor[] CORES = {TextColor.color(0xBDBDBD), TextColor.color(0x4FC3F7), TextColor.color(0xCE93D8)};
    static final String[] RARIDADES = {"Comum", "Rara", "Épica"};
    /** Pó que cada repetida vira, e quanto custa criar uma carta, por raridade. */
    private static final int[] PO_MOER = {5, 15, 40}, PO_CRIAR = {40, 100, 300};
    private static final int[] MARCOS = {10, 25, 50, 80};

    /** Onde ficam as páginas no menu (12): a fileira de cima e o começo da quinta. */
    private static final int[] S_PAGINAS = {0, 1, 2, 3, 4, 5, 6, 7, 36, 37, 38, 39};
    private static final int S_INFO = 8, S_BONUS = 45, S_MOER = 47, S_CRIAR = 48, S_BARALHO = 50, S_MARCOS = 51, S_DUELO = 53;
    private static final int[] S_CARTAS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private enum Tipo { ALBUM, CRIAR }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Tipo tipo = Tipo.ALBUM;
        Carta.Pagina pagina;
        int folha;
        final Map<Integer, Carta> cartas = new java.util.HashMap<>();

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kAlbum, kBrilhantes, kPo, kMarcos;

    public Album(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kAlbum = new NamespacedKey(plugin, "album");
        this.kBrilhantes = new NamespacedKey(plugin, "album_brilhantes");
        this.kPo = new NamespacedKey(plugin, "album_po");
        this.kMarcos = new NamespacedKey(plugin, "album_marcos");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Itens: a carta e o Pacote de Cartas
    // =====================================================================

    public static ItemStack carta(Carta c, boolean brilhante) {
        ItemStack i = new ItemStack(Material.PAPER);
        int r = c.raridade();
        CartasDuelo.Status s = CartasDuelo.de(c);
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("Página: " + c.pagina().nome(), NamedTextColor.GRAY),
                Component.text("Raridade: " + RARIDADES[r] + (brilhante ? " · Brilhante" : ""), brilhante ? NamedTextColor.GOLD : CORES[r]),
                Component.text("Duelo: " + CartasDuelo.resumo(c, brilhante), NamedTextColor.WHITE)));
        if (s.dom() != CartasDuelo.Dom.NENHUM) lore.add(Component.text("  " + s.dom().descricao, NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("» Clique com ela na mão para guardar", NamedTextColor.YELLOW));
        lore.add(Component.text("no seu álbum (/album).", NamedTextColor.YELLOW));
        i.editMeta(m -> {
            m.itemName(Component.text((brilhante ? "✦ " : "") + "Carta: " + c.nome(), brilhante ? NamedTextColor.GOLD : CORES[r]));
            m.lore(lore.stream().map(x -> x.decoration(TextDecoration.ITALIC, false)).toList());
            if (brilhante) m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K_CARTA, PersistentDataType.STRING, c.name());
            if (brilhante) m.getPersistentDataContainer().set(K_BRILHANTE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, visual(c, brilhante));
        });
        return i;
    }

    /** O visual da carta no pacote: a moldura da raridade e, numa moldura de item, o ícone dela. */
    public static String visual(Carta c, boolean brilhante) {
        return (brilhante ? "cartab_" : "carta_") + c.id();
    }

    public static ItemStack pacote(int qtd) {
        ItemStack i = new ItemStack(Material.PAPER, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Pacote de Cartas", COR, TextDecoration.BOLD));
            m.lore(List.of(
                    Component.text("3 cartas: chance de rara, épica e brilhante.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("» Clique com ele na mão para abrir", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_PACOTE, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "pacote_cartas");
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

    public static boolean ehPacote(ItemStack i) {
        return i != null && !i.isEmpty() && i.hasItemMeta() && i.getPersistentDataContainer().has(K_PACOTE);
    }

    // =====================================================================
    //  Dados (no próprio jogador)
    // =====================================================================

    /** Quantas de cada carta o jogador guardou no álbum. */
    public Map<Carta, Integer> colecao(Player p) {
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

    /** As cartas que o jogador já guardou na versão brilhante (ids). */
    public Set<String> brilhantes(Player p) {
        String s = p.getPersistentDataContainer().get(kBrilhantes, PersistentDataType.STRING);
        return s == null || s.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    public int po(Player p) {
        return p.getPersistentDataContainer().getOrDefault(kPo, PersistentDataType.INTEGER, 0);
    }

    public void darPo(Player p, int n) {
        p.getPersistentDataContainer().set(kPo, PersistentDataType.INTEGER, Math.max(0, po(p) + n));
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
            plugin.diario().marco(p, "album_" + c.pagina().name().toLowerCase(Locale.ROOT), "Completou a página " + c.pagina().nome() + " do álbum");
            boolean tudo = true;
            for (Carta.Pagina pg : Carta.Pagina.values()) if (!completa(p, pg)) { tudo = false; break; }
            if (tudo) {
                plugin.titulos().registrar(p, "album_completo", 1);
                plugin.getServer().broadcast(Component.text("🃏 " + p.getName() + " completou o Álbum de Cartas!", COR, TextDecoration.BOLD));
            }
        }
        if (nova) marcos(p);
    }

    /** Dá uma carta (item) ao jogador: inventário ou chão. Usado pelas lendas, laços e chefes. */
    public void darCarta(Player p, Carta c, boolean brilho) {
        for (ItemStack sobra : p.getInventory().addItem(carta(c, brilho)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        p.sendMessage(Component.text("🃏 Você ganhou a carta " + (brilho ? "brilhante " : "") + c.nome() + "! Clique com ela na mão para guardar.", COR));
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.5f);
    }

    // =====================================================================
    //  Marcos do álbum (cartas diferentes guardadas)
    // =====================================================================

    private void marcos(Player p) {
        int tem = total(p);
        int feitos = p.getPersistentDataContainer().getOrDefault(kMarcos, PersistentDataType.INTEGER, 0);
        for (int i = 0; i < MARCOS.length; i++) {
            if ((feitos & (1 << i)) != 0 || tem < MARCOS[i]) continue;
            feitos |= 1 << i;
            List<ItemStack> premio = new ArrayList<>();
            switch (i) {
                case 0 -> { premio.add(pacote(1)); premio.add(new ItemStack(Material.EMERALD, 5)); }
                case 1 -> { premio.add(pacote(2)); darPo(p, 100); }
                case 2 -> { premio.add(pacote(3)); premio.add(carta(sortear(rnd(), false), true)); }
                default -> { premio.add(pacote(5)); darPo(p, 300); }
            }
            for (ItemStack it : premio) for (ItemStack sobra : p.getInventory().addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            p.showTitle(net.kyori.adventure.title.Title.title(Component.text("🃏 " + MARCOS[i] + " cartas!", COR, TextDecoration.BOLD),
                    Component.text("Marco do álbum: " + descricaoMarco(i), NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.4f);
            plugin.titulos().registrar(p, "album_marcos", 1);
        }
        p.getPersistentDataContainer().set(kMarcos, PersistentDataType.INTEGER, feitos);
    }

    private static String descricaoMarco(int i) {
        return switch (i) {
            case 0 -> "1 Pacote de Cartas e 5 esmeraldas";
            case 1 -> "2 Pacotes de Cartas e 100 de Pó de Carta";
            case 2 -> "3 Pacotes de Cartas e uma carta brilhante";
            default -> "5 Pacotes de Cartas e 300 de Pó de Carta";
        };
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

    /** Chefe das estruturas, das Profundezas ou do céu: 30% de deixar a carta dele. */
    public void soltarChefeMundo(Entity morto, Carta c) {
        if (rnd().nextDouble() < 0.3 * plugin.settings().vidaCartaChance) soltar(morto, c);
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
        // Elites às vezes deixam um Pacote de Cartas.
        if (Perigo.ehElite(morto) && rnd().nextDouble() < 0.03 * plugin.settings().vidaCartaChance) {
            morto.getWorld().dropItemNaturally(morto.getLocation(), pacote(1));
        }
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
        if (ehPacote(mao)) {
            e.setCancelled(true);
            mao.setAmount(mao.getAmount() - 1);
            abrirPacote(e.getPlayer());
            return;
        }
        Carta c = de(mao);
        if (c == null) return;
        e.setCancelled(true);
        boolean brilho = brilhante(mao);
        mao.setAmount(mao.getAmount() - 1);
        guardar(e.getPlayer(), c, brilho);
    }

    // =====================================================================
    //  Pacote de Cartas
    // =====================================================================

    /** Uma carta sorteada para pacote (lendas e moradores não saem em pacote: são conquistados). */
    public static Carta sortear(ThreadLocalRandom r, boolean podeEpica) {
        double x = r.nextDouble();
        int raridade = x < 0.05 && podeEpica ? 2 : x < 0.30 ? 1 : 0;
        List<Carta> pool = new ArrayList<>();
        for (Carta c : Carta.values()) {
            if (c.pagina() == Carta.Pagina.LENDAS || c.pagina() == Carta.Pagina.MORADORES) continue;
            if (c.raridade() == raridade) pool.add(c);
        }
        return pool.get(r.nextInt(pool.size()));
    }

    private void abrirPacote(Player p) {
        ThreadLocalRandom r = rnd();
        List<String> nomes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Carta c = sortear(r, true);
            boolean brilho = r.nextDouble() < 0.06;
            for (ItemStack sobra : p.getInventory().addItem(carta(c, brilho)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            nomes.add((brilho ? "✦ " : "") + c.nome() + " (" + RARIDADES[c.raridade()] + ")");
        }
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.2, 0), 40, 0.4, 0.4, 0.4, 0.6);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.6f);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.8f);
        p.sendMessage(Component.text("🃏 No pacote: " + String.join(", ", nomes), COR));
        plugin.titulos().registrar(p, "pacotes_abertos", 1);
    }

    // =====================================================================
    //  Bônus das páginas
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        Player p = e.getDamager() instanceof Player pl ? pl
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null || !(e.getEntity() instanceof LivingEntity alvo)) return;
        double mult = 1;
        if (completa(p, Carta.Pagina.LENDAS)) mult *= 1.03;
        if (Perigo.ehElite(alvo) && completa(p, Carta.Pagina.MUNDO)) mult *= 1.05;
        Carta.Pagina pg = null;
        if (Chefes.ehChefe(alvo)) pg = Carta.Pagina.CHEFES;
        else {
            Carta c = Carta.deTipo(alvo.getType());
            if (c != null) pg = c.pagina();
        }
        if (pg != null && completa(p, pg)) mult *= 1.05;
        if (mult > 1) e.setDamage(e.getDamage() * mult);
    }

    public double bonusPesca(Player p) {
        return completa(p, Carta.Pagina.PEIXES) ? 0.02 : 0;
    }

    /** Página dos Moradores completa: +5% de XP em todos os atributos. */
    public double multiplicadorXp(Player p) {
        return completa(p, Carta.Pagina.MORADORES) ? 1.05 : 1;
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
        Carta.Pagina[] paginas = Carta.Pagina.values();
        for (int k = 0; k < paginas.length && k < S_PAGINAS.length; k++) {
            Carta.Pagina pg = paginas[k];
            int tem = 0, total = pg.cartas().size();
            for (Carta c : pg.cartas()) if (col.getOrDefault(c, 0) > 0) tem++;
            t.inventario.setItem(S_PAGINAS[k], item(pg.icone(), Component.text(pg.nome(), tem == total ? NamedTextColor.GOLD : COR, TextDecoration.BOLD),
                    List.of(Component.text(tem + "/" + total + " cartas", NamedTextColor.YELLOW),
                            Component.text("Completa: " + pg.bonus(), tem == total ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text(pg == pagina ? "● Aberta" : "» Clique para abrir", pg == pagina ? NamedTextColor.GREEN : NamedTextColor.YELLOW)),
                    pg == pagina));
        }
        t.inventario.setItem(S_INFO, item(Material.BOOK, Component.text("🃏 Álbum de Cartas", COR, TextDecoration.BOLD),
                List.of(Component.text(total(p) + "/" + Carta.values().length + " cartas", NamedTextColor.YELLOW),
                        Component.text(brilho.size() + " brilhantes", NamedTextColor.GOLD),
                        Component.text(po(p) + " de Pó de Carta", NamedTextColor.AQUA),
                        Component.empty(),
                        Component.text("Monstros, chefes, peixes raros, lendas,", NamedTextColor.GRAY),
                        Component.text("chefes do mundo e moradores das vilas.", NamedTextColor.GRAY)), false));
        List<Carta> cartas = pagina.cartas();
        for (int i = 0; i < cartas.size() && i < S_CARTAS.length; i++) {
            Carta c = cartas.get(i);
            int n = col.getOrDefault(c, 0);
            boolean b = brilho.contains(c.id());
            if (n <= 0) {
                t.inventario.setItem(S_CARTAS[i], item(Material.GRAY_DYE, Component.text("???", NamedTextColor.DARK_GRAY),
                        List.of(Component.text(comoConseguir(c), NamedTextColor.GRAY)), false));
            } else {
                CartasDuelo.Status s = CartasDuelo.de(c);
                List<Component> lore = new ArrayList<>(List.of(
                        Component.text(RARIDADES[c.raridade()] + (b ? " · Brilhante" : ""), b ? NamedTextColor.GOLD : CORES[c.raridade()]),
                        Component.text("Você tem: " + n, NamedTextColor.YELLOW),
                        Component.text("Duelo: " + CartasDuelo.resumo(c, b), NamedTextColor.WHITE)));
                if (s.dom() != CartasDuelo.Dom.NENHUM) lore.add(Component.text(s.dom().descricao, NamedTextColor.DARK_GRAY));
                t.inventario.setItem(S_CARTAS[i], item(c.icone(), Component.text((b ? "✦ " : "") + c.nome(), b ? NamedTextColor.GOLD : CORES[c.raridade()], TextDecoration.BOLD),
                        lore, b));
            }
        }
        boolean ok = completa(p, pagina);
        t.inventario.setItem(S_BONUS, item(ok ? Material.NETHER_STAR : Material.FIREWORK_STAR,
                Component.text("Bônus da página", ok ? NamedTextColor.GOLD : NamedTextColor.GRAY, TextDecoration.BOLD),
                List.of(Component.text(pagina.bonus(), ok ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                        Component.text(ok ? "✔ Ativo" : "Complete a página para ganhar.", ok ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)), ok));
        int repetidas = 0, poRepetidas = 0;
        for (Map.Entry<Carta, Integer> en : col.entrySet()) {
            int extra = Math.max(0, en.getValue() - 2);
            repetidas += extra;
            poRepetidas += extra * PO_MOER[en.getKey().raridade()];
        }
        t.inventario.setItem(S_MOER, item(Material.GRINDSTONE, Component.text("Moer repetidas", COR, TextDecoration.BOLD),
                List.of(Component.text("Da 3ª cópia em diante, vira Pó de Carta:", NamedTextColor.GRAY),
                        Component.text("comum 5, rara 15, épica 40.", NamedTextColor.GRAY),
                        Component.text("Repetidas: " + repetidas + " (= " + poRepetidas + " de pó)", NamedTextColor.YELLOW),
                        Component.text("Ficam sempre 2 de cada (para o baralho).", NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text("» Clique para moer", NamedTextColor.YELLOW)), false));
        t.inventario.setItem(S_CRIAR, item(Material.CARTOGRAPHY_TABLE, Component.text("Criar carta", COR, TextDecoration.BOLD),
                List.of(Component.text("Use o pó para criar a carta que falta:", NamedTextColor.GRAY),
                        Component.text("comum 40, rara 100, épica 300.", NamedTextColor.GRAY),
                        Component.text("Lendas e Moradores não: essas se conquistam.", NamedTextColor.DARK_GRAY),
                        Component.text("Você tem " + po(p) + " de pó.", NamedTextColor.AQUA),
                        Component.empty(),
                        Component.text("» Clique para escolher", NamedTextColor.YELLOW)), false));
        t.inventario.setItem(S_BARALHO, item(Material.WRITABLE_BOOK, Component.text("Baralho de Duelo", COR, TextDecoration.BOLD),
                List.of(Component.text("Monte o baralho de 12 cartas", NamedTextColor.GRAY),
                        Component.text("para o Duelo de Cartas.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("» Clique para montar", NamedTextColor.YELLOW)), false));
        int feitos = p.getPersistentDataContainer().getOrDefault(kMarcos, PersistentDataType.INTEGER, 0);
        List<Component> lm = new ArrayList<>();
        for (int i = 0; i < MARCOS.length; i++) {
            boolean f = (feitos & (1 << i)) != 0;
            lm.add(Component.text((f ? "✔ " : "• ") + MARCOS[i] + " cartas: " + descricaoMarco(i), f ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
        t.inventario.setItem(S_MARCOS, item(Material.GOLD_INGOT, Component.text("Marcos do álbum", NamedTextColor.GOLD, TextDecoration.BOLD), lm, false));
        t.inventario.setItem(S_DUELO, item(Material.PAPER, Component.text("Como duelar", COR, TextDecoration.BOLD), List.of(
                Component.text("Desafie os moradores das vilas pelo", NamedTextColor.GRAY),
                Component.text("menu deles, ou um jogador com", NamedTextColor.GRAY),
                Component.text("/duelo <jogador>.", NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("3 trilhas, 20 de vida, energia que", NamedTextColor.GRAY),
                Component.text("cresce a cada turno. Cada carta ataca", NamedTextColor.GRAY),
                Component.text("a da frente; trilha vazia = golpe", NamedTextColor.GRAY),
                Component.text("direto no adversário.", NamedTextColor.GRAY)), false));
        p.openInventory(t.inventario);
    }

    private static String comoConseguir(Carta c) {
        return switch (c.pagina()) {
            case LENDAS -> "Choque, conquiste ou espante esta lenda.";
            case MORADORES -> "Complete o laço com este morador.";
            case MUNDO -> "Derrote este chefe (ou crie com Pó de Carta).";
            case PEIXES -> "Pesque este peixe raro.";
            default -> "Ainda não encontrada.";
        };
    }

    /** Tela para criar a carta que falta com Pó de Carta. */
    private void abrirCriar(Player p, int folha) {
        Tela t = new Tela();
        t.tipo = Tipo.CRIAR;
        t.folha = folha;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("Criar carta · " + po(p) + " de pó", COR));
        Map<Carta, Integer> col = colecao(p);
        List<Carta> faltam = new ArrayList<>();
        for (Carta c : Carta.values()) {
            if (c.pagina() == Carta.Pagina.LENDAS || c.pagina() == Carta.Pagina.MORADORES) continue;
            if (col.getOrDefault(c, 0) < 2) faltam.add(c); // a 2ª cópia também serve para o baralho
        }
        int porFolha = 45, inicio = folha * porFolha;
        for (int i = 0; i < porFolha && inicio + i < faltam.size(); i++) {
            Carta c = faltam.get(inicio + i);
            int custo = PO_CRIAR[c.raridade()];
            boolean pode = po(p) >= custo;
            t.inventario.setItem(i, item(c.icone(), Component.text(c.nome(), CORES[c.raridade()], TextDecoration.BOLD), List.of(
                    Component.text(c.pagina().nome() + " · " + RARIDADES[c.raridade()], NamedTextColor.GRAY),
                    Component.text(col.getOrDefault(c, 0) == 0 ? "Falta no álbum" : "Você tem 1 (a 2ª vai para o baralho)", NamedTextColor.WHITE),
                    Component.text("Custa " + custo + " de pó", pode ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.text(pode ? "» Clique para criar" : "Pó insuficiente", pode ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)), false));
            t.cartas.put(i, c);
        }
        if (faltam.isEmpty()) t.inventario.setItem(22, item(Material.NETHER_STAR, Component.text("Você já tem 2 de cada carta!", NamedTextColor.GOLD), List.of(), true));
        t.inventario.setItem(45, item(Material.ARROW, Component.text("« Álbum", NamedTextColor.YELLOW), List.of(), false));
        if (folha > 0) t.inventario.setItem(48, item(Material.ARROW, Component.text("« Anterior", NamedTextColor.YELLOW), List.of(), false));
        if (inicio + porFolha < faltam.size()) t.inventario.setItem(50, item(Material.ARROW, Component.text("Próxima »", NamedTextColor.YELLOW), List.of(), false));
        t.inventario.setItem(49, item(Material.SUGAR, Component.text(po(p) + " de Pó de Carta", NamedTextColor.AQUA), List.of(), false));
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
        if (t.tipo == Tipo.CRIAR) {
            if (slot == 45) { abrir(p, Carta.Pagina.MORTOS_VIVOS); return; }
            if (slot == 48 && t.folha > 0) { abrirCriar(p, t.folha - 1); return; }
            if (slot == 50) { abrirCriar(p, t.folha + 1); return; }
            Carta c = t.cartas.get(slot);
            if (c == null) return;
            int custo = PO_CRIAR[c.raridade()];
            if (po(p) < custo) {
                p.sendActionBar(Component.text("✖ Pó insuficiente: precisa de " + custo + ".", NamedTextColor.RED));
                return;
            }
            darPo(p, -custo);
            guardar(p, c, false);
            p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.2, 0), 30, 0.4, 0.4, 0.4, 0.5);
            abrirCriar(p, t.folha);
            return;
        }
        for (int k = 0; k < S_PAGINAS.length && k < Carta.Pagina.values().length; k++) {
            if (S_PAGINAS[k] == slot) { abrir(p, Carta.Pagina.values()[k]); return; }
        }
        switch (slot) {
            case S_MOER -> moer(p, t.pagina);
            case S_CRIAR -> abrirCriar(p, 0);
            case S_BARALHO -> plugin.duelos().abrirBaralho(p, 0);
            default -> { }
        }
    }

    private void moer(Player p, Carta.Pagina voltar) {
        Map<Carta, Integer> col = colecao(p);
        int ganho = 0, quantas = 0;
        for (Map.Entry<Carta, Integer> en : col.entrySet()) {
            int extra = en.getValue() - 2;
            if (extra <= 0) continue;
            ganho += extra * PO_MOER[en.getKey().raridade()];
            quantas += extra;
            en.setValue(2);
        }
        if (quantas == 0) {
            p.sendActionBar(Component.text("Nenhuma carta com mais de 2 cópias.", NamedTextColor.GRAY));
            return;
        }
        gravar(p, col);
        darPo(p, ganho);
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1.2f);
        p.sendMessage(Component.text("🃏 " + quantas + " repetidas viraram " + ganho + " de Pó de Carta (agora: " + po(p) + ").", COR));
        abrir(p, voltar);
    }
}
