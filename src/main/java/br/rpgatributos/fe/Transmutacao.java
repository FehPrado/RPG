package br.rpgatributos.fe;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Círculo de Transmutação: bloco de ametista com 1 Mercúrio Vivo e 1 lingote de ouro
 * jogados em cima. Transforma materiais em outros mais valiosos, com chance de falhar
 * (e, às vezes, de explodir na sua cara). A Pedra Filosofal no inventário ajuda muito.
 */
public final class Transmutacao extends Estacao {

    public static final TextColor COR = TextColor.color(0xFFAB40);
    public static final NamespacedKey K_PEDRA = new NamespacedKey("rpgatributos", "pedra_filosofal");
    private static final int S_INFO = 4, S_FECHAR = 22;
    private static final int[] S_RECEITAS = {10, 11, 12, 13, 14, 15, 16};

    /** Um ingrediente: material comum, componente alquímico ou material raro. */
    private record Custo(Material material, Reagente reagente, Raro raro, int qtd) {
        static Custo m(Material m, int q) { return new Custo(m, null, null, q); }
        static Custo r(Reagente r, int q) { return new Custo(null, r, null, q); }
        static Custo raro(Raro r, int q) { return new Custo(null, null, r, q); }

        boolean serve(ItemStack i) {
            if (i == null || i.isEmpty()) return false;
            if (reagente != null) return Reagente.de(i) == reagente;
            if (raro != null) return Raro.de(i) == raro;
            return i.getType() == material && Reagente.de(i) == null && Raro.de(i) == null && !i.getPersistentDataContainer().has(K_PEDRA);
        }

        String nome() {
            if (reagente != null) return reagente.nome();
            if (raro != null) return raro.nome();
            String s = material.name().toLowerCase().replace('_', ' ');
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    private record Receita(String nome, Material icone, List<Custo> custos, Supplier<ItemStack> resultado, double chance) {}

    private static final List<Receita> RECEITAS = List.of(
            new Receita("Cobre em Ferro", Material.IRON_INGOT, List.of(Custo.m(Material.COPPER_INGOT, 8), Custo.r(Reagente.PO_ARCANO, 1)),
                    () -> new ItemStack(Material.IRON_INGOT, 3), 0.85),
            new Receita("Ferro em Ouro", Material.GOLD_INGOT, List.of(Custo.m(Material.IRON_INGOT, 8), Custo.r(Reagente.PO_ARCANO, 1)),
                    () -> new ItemStack(Material.GOLD_INGOT, 3), 0.80),
            new Receita("Redstone em Luz", Material.GLOWSTONE_DUST, List.of(Custo.m(Material.REDSTONE, 9), Custo.r(Reagente.PO_ARCANO, 1)),
                    () -> new ItemStack(Material.GLOWSTONE_DUST, 3), 0.90),
            new Receita("Lápis em Esmeralda", Material.EMERALD, List.of(Custo.m(Material.LAPIS_LAZULI, 8), Custo.r(Reagente.PO_ARCANO, 1)),
                    () -> new ItemStack(Material.EMERALD, 1), 0.75),
            new Receita("Ouro em Diamante", Material.DIAMOND, List.of(Custo.m(Material.GOLD_INGOT, 16), Custo.r(Reagente.MERCURIO_VIVO, 2)),
                    () -> new ItemStack(Material.DIAMOND, 1), 0.60),
            new Receita("Diamante em Netherite", Material.NETHERITE_SCRAP, List.of(Custo.m(Material.DIAMOND, 4), Custo.r(Reagente.MERCURIO_VIVO, 2),
                    Custo.r(Reagente.CRISTAL_DE_MANA, 1)), () -> new ItemStack(Material.NETHERITE_SCRAP, 1), 0.45),
            new Receita("Pedra Filosofal", Material.MAGMA_CREAM, List.of(Custo.m(Material.DIAMOND_BLOCK, 1), Custo.r(Reagente.MERCURIO_VIVO, 4),
                    Custo.r(Reagente.CRISTAL_DE_MANA, 4), Custo.raro(Raro.ESSENCIA_PRIMORDIAL, 1)), Transmutacao::pedraFilosofal, 0.50));

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location circulo;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Transmutacao(RPGAtributos plugin) {
        super(plugin, "circulos_transmutacao", "circulo_transmutacao_display");
    }

    public static ItemStack pedraFilosofal() {
        ItemStack i = new ItemStack(Material.QUARTZ);
        i.editMeta(m -> {
            m.itemName(Component.text("Pedra Filosofal", TextColor.color(0xD32F2F)));
            m.setItemModel(NamespacedKey.minecraft("magma_cream"));
            m.setMaxStackSize(1);
            m.setEnchantmentGlintOverride(true);
            m.lore(List.of(
                    Component.text("Com ela no inventário, as transmutações", NamedTextColor.GRAY),
                    Component.text("têm +20% de chance de dar certo.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("⚗ Lendária da alquimia", TextColor.color(0xD32F2F))).stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_PEDRA, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    private static boolean temPedra(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_PEDRA)) return true;
        }
        return false;
    }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.AMETHYST_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.QUARTZ || m == Material.GOLD_INGOT;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.AMETHYST_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        Item mercurio = null;
        for (Item i : itens) if (Reagente.de(i.getItemStack()) == Reagente.MERCURIO_VIVO) mercurio = i;
        if (mercurio == null || contar(itens, Material.GOLD_INGOT) < 1) return false;
        tirar(mercurio, 1);
        tirar(itens, Material.GOLD_INGOT, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.AMETHYST_CLUSTER);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("⚗ Círculo de Transmutação ⚗", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.WITCH, c, 60, 0.5, 0.6, 0.5, 0.1);
        c.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.8f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("⚗ Círculo de Transmutação ⚗", COR, TextDecoration.BOLD),
                Component.text("Ferro em ouro, ouro em diamante...", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.WITCH, centro.clone().subtract(0, 0.8, 0), 2, 0.4, 0.1, 0.4, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, Reagente.MERCURIO_VIVO.criar(1));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GOLD_INGOT));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.circulo = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 27, Component.text("⚗ Círculo de Transmutação"));
        desenhar(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 1.2f);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    private double chance(Player p, Receita r) {
        double c = r.chance() + plugin.stats().getNivel(p, Skill.ALQUIMIA) * 0.002;
        if (temPedra(p) && !r.nome().equals("Pedra Filosofal")) c += 0.2;
        return Math.min(0.98, c);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        inv.setItem(S_INFO, item(Material.AMETHYST_CLUSTER, Component.text("⚗ Transmutação", COR, TextDecoration.BOLD), List.of(
                Component.text("Clique numa receita para tentar.", NamedTextColor.GRAY),
                Component.text("Shift + clique: tenta até 5 vezes.", NamedTextColor.GRAY),
                Component.text("Falhou? Os ingredientes se perdem, e às", NamedTextColor.GRAY),
                Component.text("vezes a mistura explode.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Alquimia alta aumenta a chance.", NamedTextColor.YELLOW),
                Component.text(temPedra(p) ? "✔ Pedra Filosofal: +20% de chance" : "Pedra Filosofal: +20% (você não tem)",
                        temPedra(p) ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY))));
        for (int i = 0; i < RECEITAS.size() && i < S_RECEITAS.length; i++) {
            Receita r = RECEITAS.get(i);
            List<Component> lore = new ArrayList<>();
            boolean tudo = true;
            for (Custo c : r.custos()) {
                int tem = contar(p, c);
                boolean ok = tem >= c.qtd();
                tudo &= ok;
                lore.add(Component.text((ok ? "✔ " : "✖ ") + c.qtd() + "x " + c.nome() + " (" + tem + ")", ok ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            ItemStack res = r.resultado().get();
            lore.add(Component.text("→ " + res.getAmount() + "x " + (r.nome().equals("Pedra Filosofal") ? "Pedra Filosofal" : Custo.m(res.getType(), 1).nome()),
                    NamedTextColor.WHITE));
            lore.add(Component.text("Chance: " + Math.round(chance(p, r) * 100) + "%", NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(tudo ? Component.text("» Clique para transmutar", NamedTextColor.YELLOW) : Component.text("Faltam ingredientes.", NamedTextColor.DARK_GRAY));
            inv.setItem(S_RECEITAS[i], item(r.icone(), Component.text(r.nome(), COR, TextDecoration.BOLD), lore));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static int contar(Player p, Custo c) {
        int n = 0;
        for (ItemStack i : p.getInventory().getStorageContents()) if (c.serve(i)) n += i.getAmount();
        return n;
    }

    private static void tirar(Player p, Custo c) {
        int falta = c.qtd();
        ItemStack[] conteudo = p.getInventory().getStorageContents();
        for (int k = 0; k < conteudo.length && falta > 0; k++) {
            ItemStack i = conteudo[k];
            if (!c.serve(i)) continue;
            int tira = Math.min(falta, i.getAmount());
            i.setAmount(i.getAmount() - tira);
            falta -= tira;
        }
        p.getInventory().setStorageContents(conteudo);
    }

    /** @return true se deu para tentar (tinha os ingredientes). */
    private boolean tentar(Player p, Receita r, Location circulo) {
        for (Custo c : r.custos()) if (contar(p, c) < c.qtd()) return false;
        for (Custo c : r.custos()) tirar(p, c);
        Location l = circulo.clone().add(0.5, 1.2, 0.5);
        plugin.stats().darXp(p, Skill.ALQUIMIA, 10);
        if (ThreadLocalRandom.current().nextDouble() < chance(p, r)) {
            ItemStack res = r.resultado().get();
            p.getInventory().addItem(res).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
            plugin.stats().darXp(p, Skill.ALQUIMIA, 15);
            plugin.titulos().registrar(p, "transmutacoes", 1);
            l.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l, 30, 0.3, 0.4, 0.3, 0.2);
            l.getWorld().playSound(l, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            p.sendActionBar(Component.text("✔ " + r.nome() + ": deu certo!", NamedTextColor.GREEN));
            if (r.nome().equals("Pedra Filosofal")) {
                plugin.getServer().broadcast(Component.text("⚗ " + p.getName() + " criou uma Pedra Filosofal!", TextColor.color(0xD32F2F)));
            }
        } else if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            l.getWorld().spawnParticle(Particle.EXPLOSION, l, 2, 0.3, 0.3, 0.3, 0);
            l.getWorld().playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
            p.damage(4);
            p.sendActionBar(Component.text("✖ A mistura explodiu!", NamedTextColor.RED));
        } else {
            l.getWorld().spawnParticle(Particle.LARGE_SMOKE, l, 15, 0.3, 0.3, 0.3, 0.02);
            l.getWorld().playSound(l, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1f);
            p.sendActionBar(Component.text("✖ A transmutação falhou. Os ingredientes viraram fumaça.", NamedTextColor.RED));
        }
        return true;
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
        if (!eh(t.circulo.getBlock())) {
            p.closeInventory();
            return;
        }
        for (int i = 0; i < RECEITAS.size() && i < S_RECEITAS.length; i++) {
            if (S_RECEITAS[i] != slot) continue;
            int vezes = e.isShiftClick() ? 5 : 1, feitas = 0;
            for (int k = 0; k < vezes; k++) if (tentar(p, RECEITAS.get(i), t.circulo)) feitas++; else break;
            if (feitas == 0) {
                p.sendMessage(Component.text("Faltam ingredientes para " + RECEITAS.get(i).nome() + ".", NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            }
            if (p.isDead()) return;
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
