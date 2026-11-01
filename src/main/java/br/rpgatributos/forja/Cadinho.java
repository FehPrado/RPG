package br.rpgatributos.forja;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.vida.DadosBloco;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * O Cadinho (3.19): um alto-forno onde se jogou 1 caldeirão e 1 balde de lava. Funde dois minerais
 * numa liga: 2 de cada mineral + 1 carvão dão 2 lingotes de liga em 1 minuto. As ligas fazem
 * conjuntos (bancada, como o ferro) que contam metade para cada mineral de origem.
 */
public final class Cadinho extends Estacao {

    public static final TextColor COR = TextColor.color(0xFF8A3A);
    private static final String DADO = "cadinho";
    private static final int POR_FORNADA = 2, MAX_FORNADAS = 4;
    private static final long TEMPO_MS = 60_000L;
    private static final ConjuntoMineral[] LIGAS = {ConjuntoMineral.ACO_LUNAR, ConjuntoMineral.AMBAR_SANGUE,
            ConjuntoMineral.GELO_MARINHO, ConjuntoMineral.BRONZE_VULCANICO};
    private static final int[] SLOTS = {10, 12, 14, 16};

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        Location cadinho;
        @Override public Inventory getInventory() { return inv; }
    }

    public Cadinho(RPGAtributos plugin) {
        super(plugin, "cadinhos", "cadinho_display");
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.BLAST_FURNACE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.CAULDRON || m == Material.LAVA_BUCKET;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.BLAST_FURNACE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.CAULDRON) < 1 || contar(itens, Material.LAVA_BUCKET) < 1) return false;
        tirar(itens, Material.CAULDRON, 1);
        tirar(itens, Material.LAVA_BUCKET, 1);
        bloco.getWorld().dropItemNaturally(bloco.getLocation().add(0.5, 1.2, 0.5), new ItemStack(Material.BUCKET));
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.CAULDRON);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        String[] d = dados(bloco);
        return d == null ? itemFlutuante() : ConjuntoMineral.valueOf(d[0]).mineral.criar(1);
    }

    @Override
    protected float escalaItem() {
        return 0.4f;
    }

    @Override
    protected double alturaItem() {
        return 1.3;
    }

    @Override
    protected Component nome() {
        return Component.text("🔥 Cadinho 🔥", COR).append(Component.newline())
                .append(Component.text("clique para fundir ligas", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.LAVA, c, 12, 0.3, 0.2, 0.3);
        c.getWorld().playSound(c, Sound.ITEM_BUCKET_EMPTY_LAVA, 1f, 1f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("🔥 Cadinho 🔥", COR, TextDecoration.BOLD),
                Component.text("Funde dois minerais numa liga", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.FERRARIA, 30);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (dados(centro.clone().subtract(0, alturaItem(), 0).getBlock().getLocation()) == null) return;
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.LAVA, centro, 1, 0.15, 0.05, 0.15, 0);
        if (ciclo % 8 == 0) centro.getWorld().playSound(centro, Sound.BLOCK_LAVA_POP, 0.4f, 1f);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.CAULDRON));
    }

    @Override
    protected void aoRemover(Location bloco) {
        String[] d = dados(bloco);
        if (d != null) {
            // Quebrou no meio: os lingotes saem mesmo assim (o ferreiro não perde o trabalho).
            ConjuntoMineral liga = ConjuntoMineral.valueOf(d[0]);
            bloco.getWorld().dropItemNaturally(bloco.clone().add(0.5, 0.5, 0.5), liga.mineral.criar(Integer.parseInt(d[1])));
        }
        DadosBloco.apagar(DADO, bloco);
    }

    /** [liga, quantidade, fim] ou null. */
    private static String[] dados(Location l) {
        String s = DadosBloco.ler(DADO, l);
        if (s == null) return null;
        String[] d = s.split("\\|");
        if (d.length != 3) return null;
        try {
            ConjuntoMineral.valueOf(d[0]);
            Integer.parseInt(d[1]);
            Long.parseLong(d[2]);
            return d;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        String[] d = dados(l);
        if (d != null) {
            ConjuntoMineral liga = ConjuntoMineral.valueOf(d[0]);
            long falta = Long.parseLong(d[2]) - System.currentTimeMillis();
            if (falta > 0) {
                p.sendActionBar(Component.text("🔥 " + d[1] + "x " + liga.mineral.nome() + ": falta " + (falta / 1000 + 1) + " s.", COR));
                return;
            }
            int qtd = Integer.parseInt(d[1]);
            for (ItemStack sobra : p.getInventory().addItem(liga.mineral.criar(qtd)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            DadosBloco.apagar(DADO, l);
            atualizarDisplays(l);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.FERRARIA, 15 * qtd);
            plugin.titulos().registrar(p, "ligas", qtd);
            p.sendActionBar(Component.text("🔥 " + qtd + "x " + liga.mineral.nome() + " pronto(s)!", liga.cor()));
            return;
        }
        abrirMenu(p, l);
    }

    private void abrirMenu(Player p, Location cadinho) {
        Tela t = new Tela();
        t.cadinho = cadinho;
        t.inv = Bukkit.createInventory(t, 27, Component.text("🔥 Cadinho"));
        ItemStack vidro = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        for (int i = 0; i < LIGAS.length; i++) {
            ConjuntoMineral liga = LIGAS[i];
            ConjuntoMineral[] pais = liga.pais();
            int fornadas = fornadas(p, liga);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Funde:", NamedTextColor.GRAY));
            for (ConjuntoMineral pai : pais) {
                int tem = contarRaro(p, pai.mineral);
                lore.add(Component.text((tem >= POR_FORNADA ? " ✔ " : " ✖ ") + POR_FORNADA + "x " + pai.mineral.nome() + " (" + tem + ")",
                        tem >= POR_FORNADA ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            int carvao = contarCarvao(p);
            lore.add(Component.text((carvao >= 1 ? " ✔ " : " ✖ ") + "1x carvão (" + carvao + ")", carvao >= 1 ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text("Sai: " + POR_FORNADA + "x " + liga.mineral.nome() + " em 1 minuto", liga.cor()));
            lore.add(Component.empty());
            lore.add(Component.text(liga.bonus2, NamedTextColor.DARK_GRAY));
            lore.add(Component.text(liga.bonus4, NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(fornadas > 0 ? Component.text("» Clique: 1 fornada · Shift: até " + Math.min(MAX_FORNADAS, fornadas), NamedTextColor.YELLOW)
                    : Component.text("Faltam materiais.", NamedTextColor.DARK_GRAY));
            ItemStack icone = liga.mineral.criar(1);
            icone.editMeta(m -> {
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                m.addItemFlags(ItemFlag.values());
            });
            t.inv.setItem(SLOTS[i], icone);
        }
        p.openInventory(t.inv);
    }

    private static int contarRaro(Player p, Raro r) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) if (Raro.de(s) == r) n += s.getAmount();
        return n;
    }

    private static boolean carvao(ItemStack s) {
        return s != null && (s.getType() == Material.COAL || s.getType() == Material.CHARCOAL) && !s.hasItemMeta();
    }

    private static int contarCarvao(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) if (carvao(s)) n += s.getAmount();
        return n;
    }

    private static int fornadas(Player p, ConjuntoMineral liga) {
        int n = contarCarvao(p);
        for (ConjuntoMineral pai : liga.pais()) n = Math.min(n, contarRaro(p, pai.mineral) / POR_FORNADA);
        return n;
    }

    private static void gastar(Player p, java.util.function.Predicate<ItemStack> teste, int qtd) {
        ItemStack[] c = p.getInventory().getStorageContents();
        for (int i = 0; i < c.length && qtd > 0; i++) {
            if (c[i] == null || !teste.test(c[i])) continue;
            int tira = Math.min(qtd, c[i].getAmount());
            c[i].setAmount(c[i].getAmount() - tira);
            if (c[i].getAmount() <= 0) c[i] = null;
            qtd -= tira;
        }
        p.getInventory().setStorageContents(c);
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        ConjuntoMineral liga = null;
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == e.getRawSlot()) liga = LIGAS[i];
        if (liga == null) return;
        if (!eh(t.cadinho.getBlock()) || dados(t.cadinho) != null) {
            p.closeInventory();
            return;
        }
        int fornadas = Math.min(e.isShiftClick() ? MAX_FORNADAS : 1, fornadas(p, liga));
        if (fornadas <= 0) {
            p.sendActionBar(Component.text("✖ Faltam materiais para " + liga.mineral.nome() + ".", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            for (ConjuntoMineral pai : liga.pais()) gastar(p, s -> Raro.de(s) == pai.mineral, POR_FORNADA * fornadas);
            gastar(p, Cadinho::carvao, fornadas);
        }
        int qtd = POR_FORNADA * fornadas;
        DadosBloco.gravar(DADO, t.cadinho, liga.name() + "|" + qtd + "|" + (System.currentTimeMillis() + TEMPO_MS));
        atualizarDisplays(t.cadinho);
        p.closeInventory();
        Location c = t.cadinho.clone().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.LAVA, c, 15, 0.3, 0.2, 0.3);
        c.getWorld().playSound(c, Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1f, 0.8f);
        p.sendActionBar(Component.text("🔥 Fundindo " + qtd + "x " + liga.mineral.nome() + ": pronto em 1 minuto.", liga.cor()));
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }
}
