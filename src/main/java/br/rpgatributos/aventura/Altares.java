package br.rpgatributos.aventura;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
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
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O Altar Ritualístico: obsidiana chorosa onde se jogou 1 olho do ender e 4 velas.
 * Oferendas jogadas em cima dele (ou entregues pelo menu) invocam chefes e eventos.
 */
public final class Altares extends Estacao {

    private static final TextColor COR = TextColor.color(0xB22222);
    private static final long RECARGA_MS = 2 * 60 * 1000;
    private static final double RAIO_OFERENDA = 8;
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
    private static final int S_INFO = 4;
    private static final int S_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location altar;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    /** Itens jogados perto de um altar (podem ser oferendas) → até quando contam. */
    private final Map<UUID, Long> oferendas = new HashMap<>();
    /** "mundo,x,y,z:invocação" → quando pode de novo. */
    private final Map<String, Long> recargas = new HashMap<>();
    /** Para não repetir o mesmo aviso a cada meio segundo. */
    private final Map<UUID, Long> avisado = new HashMap<>();
    /** Altares com chefe prestes a aparecer (os 2 segundos de suspense). */
    private final java.util.Set<Location> pendentes = new java.util.HashSet<>();

    public Altares(RPGAtributos plugin) {
        super(plugin, "altares", "altar_display");
    }

    // =====================================================================
    //  Ritual de criação
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CRYING_OBSIDIAN;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENDER_EYE || Tag.CANDLES.isTagged(m);
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CRYING_OBSIDIAN ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        int velas = 0;
        for (Item i : itens) if (Tag.CANDLES.isTagged(i.getItemStack().getType())) velas += i.getItemStack().getAmount();
        if (contar(itens, Material.ENDER_EYE) < 1 || velas < 4) return false;
        tirar(itens, Material.ENDER_EYE, 1);
        int falta = 4;
        for (Item i : itens) {
            if (falta <= 0) break;
            if (i.isValid() && Tag.CANDLES.isTagged(i.getItemStack().getType())) falta -= tirar(i, falta);
        }
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENDER_EYE);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("☠ Altar Ritualístico ☠", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.strikeLightningEffect(c);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c, 80, 0.5, 1, 0.5, 0.05);
        w.spawnParticle(Particle.REVERSE_PORTAL, c, 80, 0.5, 1, 0.5, 0.1);
        w.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 1f, 0.6f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("☠ Altar Ritualístico ☠", COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito para ver as oferendas", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.COMBATE, 25);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        World w = centro.getWorld();
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, centro.clone().subtract(0, 0.3, 0), 2, 0.3, 0.05, 0.3, 0.01);
        if (ciclo % 3 == 0) w.spawnParticle(Particle.REVERSE_PORTAL, centro, 4, 0.3, 0.3, 0.3, 0.02);
        if (ciclo % 6 == 0) {
            w.spawnParticle(Particle.DUST, centro, 3, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(0xB22222), 1.2f));
        }
    }

    @Override
    protected void devolver(Location centro) {
        World w = centro.getWorld();
        w.dropItemNaturally(centro, new ItemStack(Material.ENDER_EYE));
        w.dropItemNaturally(centro, new ItemStack(Material.CANDLE, 4));
        w.spawnParticle(Particle.LARGE_SMOKE, centro, 20, 0.3, 0.3, 0.3, 0.02);
        w.playSound(centro, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.6f);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.altar = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("☠ Altar Ritualístico"));
        desenhar(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 0.6f);
    }

    // =====================================================================
    //  Oferendas
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoJogarOferenda(PlayerDropItemEvent e) {
        Location l = e.getItemDrop().getLocation();
        if (!perto(l, RAIO_OFERENDA).isEmpty()) {
            oferendas.put(e.getItemDrop().getUniqueId(), System.currentTimeMillis() + 30_000);
        }
    }

    @Override
    protected void aoTick() {
        if (oferendas.isEmpty()) return;
        long agora = System.currentTimeMillis();
        Map<Block, List<Item>> porAltar = new LinkedHashMap<>();
        for (Iterator<Map.Entry<UUID, Long>> it = oferendas.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (!(ent instanceof Item item) || !item.isValid() || agora > en.getValue()) {
                it.remove();
                continue;
            }
            Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
            if (!eh(abaixo)) continue;
            if (item.getPickupDelay() < 40) item.setPickupDelay(40);
            porAltar.computeIfAbsent(abaixo, k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<Block, List<Item>> en : porAltar.entrySet()) {
            List<Item> itens = en.getValue();
            for (Invocacao inv : Invocacao.values()) {
                if (!temOferenda(itens, inv)) continue;
                Player quem = quemJogou(itens);
                String erro = impedimento(inv, en.getKey().getLocation(), quem);
                if (erro != null) {
                    if (quem != null && agora - avisado.getOrDefault(quem.getUniqueId(), 0L) > 5000) {
                        avisado.put(quem.getUniqueId(), agora);
                        quem.sendMessage(Component.text(erro, NamedTextColor.RED));
                    }
                    break;
                }
                inv.oferenda().forEach((m, q) -> tirar(itens, m, q));
                iniciar(inv, en.getKey().getLocation(), quem);
                break;
            }
        }
    }

    private static boolean temOferenda(List<Item> itens, Invocacao inv) {
        for (Map.Entry<Material, Integer> en : inv.oferenda().entrySet()) {
            if (contar(itens, en.getKey()) < en.getValue()) return false;
        }
        return true;
    }

    private static Player quemJogou(List<Item> itens) {
        for (Item i : itens) {
            if (i.getThrower() != null && Bukkit.getPlayer(i.getThrower()) != null) return Bukkit.getPlayer(i.getThrower());
        }
        return null;
    }

    private static String chave(Location altar, Invocacao inv) {
        return altar.getWorld().getName() + "," + altar.getBlockX() + "," + altar.getBlockY() + "," + altar.getBlockZ() + ":" + inv.name();
    }

    /** Motivo para não poder invocar agora, ou null se pode. */
    private String impedimento(Invocacao inv, Location altar, Player quem) {
        var dono = plugin.territorios().em(altar);
        if (dono != null && (quem == null || !plugin.territorios().podeConstruir(quem, altar))) {
            return "☠ Este altar é do território de " + dono.nomeDono() + ": só quem mora lá pode invocar.";
        }
        if (pendentes.contains(altar) || plugin.chefes().ocupado(altar) || plugin.eventos().ocupado(altar)) {
            return "☠ O altar já está em uso. Termine o que está acontecendo primeiro.";
        }
        long falta = recargas.getOrDefault(chave(altar, inv), 0L) - System.currentTimeMillis();
        if (falta > 0) return "☠ O altar ainda está se recuperando dessa invocação (" + (falta / 1000 + 1) + "s).";
        if (inv.soDeNoite() && !Eventos.noite(altar.getWorld())) return "☾ A Lua de Sangue só pode ser invocada à noite.";
        if (inv == Invocacao.LUA_DE_SANGUE && plugin.eventos().luaAtiva(altar.getWorld())) return "☾ Já é Lua de Sangue!";
        if (inv == Invocacao.ONDAS && jogadoresPerto(altar, 30).isEmpty()) return "⚔ Ninguém perto do altar para lutar.";
        return null;
    }

    private static List<Player> jogadoresPerto(Location l, double raio) {
        List<Player> lista = new ArrayList<>();
        for (Player p : l.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(l) <= raio * raio) lista.add(p);
        }
        return lista;
    }

    private void iniciar(Invocacao inv, Location altar, Player quem) {
        recargas.put(chave(altar, inv), System.currentTimeMillis() + RECARGA_MS);
        World w = altar.getWorld();
        Location topo = altar.clone().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, topo, 60, 0.4, 0.6, 0.4, 0.08);
        w.playSound(topo, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.2f);
        if (inv.chefe() != null) {
            // Pequena espera dramática antes de o chefe aparecer.
            pendentes.add(altar);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                pendentes.remove(altar);
                if (eh(altar.getBlock())) plugin.chefes().invocar(inv.chefe(), altar, quem);
            }, 40L);
            for (Player p : jogadoresPerto(altar, 40)) {
                p.sendActionBar(Component.text("☠ O altar estremece...", COR, TextDecoration.BOLD));
            }
            return;
        }
        switch (inv) {
            case LUA_DE_SANGUE -> plugin.eventos().iniciarLua(w, quem);
            case METEOROS -> plugin.eventos().iniciarMeteoros(altar, quem);
            case ONDAS -> plugin.eventos().iniciarOndas(altar, quem);
            case MERCADOR -> plugin.mercadores().invocarMercador(altar, quem);
            case CACADOR -> plugin.mercadores().invocarCacador(altar, quem);
            default -> { }
        }
    }

    // =====================================================================
    //  Menu do altar
    // =====================================================================

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        inv.setItem(S_INFO, item(Material.CRYING_OBSIDIAN, Component.text("☠ Altar Ritualístico", COR, TextDecoration.BOLD), List.of(
                Component.text("Jogue a oferenda (tecla Q) em cima do", NamedTextColor.GRAY),
                Component.text("altar, ou clique aqui num ritual para", NamedTextColor.GRAY),
                Component.text("entregar direto do seu inventário.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Chefes deixam materiais raros para o", NamedTextColor.DARK_GRAY),
                Component.text("refino e núcleos para infusões lendárias.", NamedTextColor.DARK_GRAY)), false));

        Invocacao[] todas = Invocacao.values();
        for (int i = 0; i < todas.length && i < SLOTS.length; i++) {
            Invocacao inv2 = todas[i];
            List<Component> lore = new ArrayList<>();
            if (inv2.chefe() != null) {
                Chefe c = inv2.chefe();
                lore.add(Component.text("Chefe · dificuldade " + c.estrelas(), NamedTextColor.GRAY));
                lore.add(Component.text(inv2.descricao(), NamedTextColor.WHITE));
                lore.add(Component.text("Deixa: ", NamedTextColor.GRAY).append(Component.text(c.nucleo().nome(), c.nucleo().cor()))
                        .append(Component.text(", Fragmentos de Forja e mais", NamedTextColor.GRAY)));
            } else {
                lore.addAll(quebrar(inv2.descricao()));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Oferenda:", NamedTextColor.GRAY));
            PlayerInventory pi = p.getInventory();
            boolean temTudo = true;
            for (Map.Entry<Material, Integer> en : inv2.oferenda().entrySet()) {
                boolean tem = pi.containsAtLeast(new ItemStack(en.getKey()), en.getValue());
                temTudo &= tem;
                lore.add(Component.text((tem ? " ✔ " : " ✖ ") + en.getValue() + "x ", tem ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(Component.translatable(en.getKey().translationKey(), tem ? NamedTextColor.GREEN : NamedTextColor.RED)));
            }
            if (inv2.soDeNoite()) lore.add(Component.text("☾ Só à noite", NamedTextColor.DARK_RED));
            String erro = impedimento(inv2, t.altar, p);
            lore.add(Component.empty());
            if (erro != null) lore.add(Component.text(erro, NamedTextColor.RED));
            else if (temTudo) lore.add(Component.text("» Clique para entregar a oferenda", NamedTextColor.YELLOW));
            else lore.add(Component.text("Faltam itens para a oferenda.", NamedTextColor.DARK_GRAY));
            inv.setItem(SLOTS[i], item(inv2.icone(), Component.text(inv2.nome(), inv2.cor(), TextDecoration.BOLD), lore, inv2.chefe() != null));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    /** Quebra textos longos em linhas de ~40 letras para a descrição do item. */
    private static List<Component> quebrar(String texto) {
        List<Component> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (atual.length() + palavra.length() > 40) {
                linhas.add(Component.text(atual.toString().trim(), NamedTextColor.WHITE));
                atual.setLength(0);
            }
            atual.append(palavra).append(' ');
        }
        if (!atual.isEmpty()) linhas.add(Component.text(atual.toString().trim(), NamedTextColor.WHITE));
        return linhas;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
        Invocacao[] todas = Invocacao.values();
        for (int i = 0; i < todas.length && i < SLOTS.length; i++) {
            if (SLOTS[i] != e.getSlot()) continue;
            Invocacao inv = todas[i];
            if (!eh(t.altar.getBlock())) { p.closeInventory(); return; }
            String erro = impedimento(inv, t.altar, p);
            if (erro != null) {
                p.sendMessage(Component.text(erro, NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                return;
            }
            PlayerInventory pi = p.getInventory();
            for (Map.Entry<Material, Integer> en : inv.oferenda().entrySet()) {
                if (!pi.containsAtLeast(new ItemStack(en.getKey()), en.getValue())) {
                    p.sendMessage(Component.text("Faltam itens para a oferenda.", NamedTextColor.RED));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                    return;
                }
            }
            inv.oferenda().forEach((m, q) -> pi.removeItem(new ItemStack(m, q)));
            p.closeInventory();
            iniciar(inv, t.altar, p);
            return;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
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
}
