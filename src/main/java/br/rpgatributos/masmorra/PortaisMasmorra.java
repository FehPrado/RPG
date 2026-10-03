package br.rpgatributos.masmorra;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Portal da Masmorra: tijolos de pedra entalhados com 1 olho do ender e 1 bússola jogados
 * em cima. Abre masmorras e guarda o Cofre das Almas.
 */
public final class PortaisMasmorra extends Estacao {

    private static final int S_INFO = 4, S_COFRE = 40, S_FECHAR = 49;
    private static final int[] S_DIFICULDADES = {19, 21, 23, 25};

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location portal;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public PortaisMasmorra(RPGAtributos plugin) {
        super(plugin, "portais_masmorra", "portal_masmorra_display");
    }

    private Masmorras masmorras() { return plugin.masmorras(); }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHISELED_STONE_BRICKS;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENDER_EYE || m == Material.COMPASS;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHISELED_STONE_BRICKS ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.ENDER_EYE) < 1 || contar(itens, Material.COMPASS) < 1) return false;
        tirar(itens, Material.ENDER_EYE, 1);
        tirar(itens, Material.COMPASS, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.RECOVERY_COMPASS);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 2.8;
    }

    @Override
    protected Component nome() {
        return Component.text("۞ Portal da Masmorra ۞", Masmorras.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.REVERSE_PORTAL, c, 80, 0.4, 0.8, 0.4, 0.05);
        w.playSound(c, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 0.8f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("۞ Portal da Masmorra ۞", Masmorras.COR, TextDecoration.BOLD),
                Component.text("Clique para escolher a dificuldade", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.PORTAL, centro.clone().subtract(0, 1.5, 0), 4, 0.3, 0.3, 0.3, 0.2);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.ENDER_EYE));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.COMPASS));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.portal = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("۞ Portal da Masmorra"));
        desenhar(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 0.6f);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        int nivelTotal = plugin.stats().nivelTotal(p);
        int maxTotal = plugin.settings().nivelMaximo * Skill.values().length;
        String estado = masmorras().portalAberto(t.portal) ? "O portal está aberto: suba nele!"
                : masmorras().formando(t.portal) ? "Uma masmorra está se formando..." : "Escolha a dificuldade abaixo.";
        inv.setItem(S_INFO, item(Material.RECOVERY_COMPASS, Component.text("۞ Masmorras", Masmorras.COR, TextDecoration.BOLD), List.of(
                Component.text("Salas aleatórias, ondas de monstros,", NamedTextColor.GRAY),
                Component.text("tesouros e um chefe no final.", NamedTextColor.GRAY),
                Component.text("Sua party entra junto (subindo no portal).", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Morreu lá dentro? Os itens vão para o", NamedTextColor.GRAY),
                Component.text("Cofre das Almas (" + plugin.settings().masCustoCofre + " diamantes para pegar).", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Seu nível total: " + nivelTotal, NamedTextColor.YELLOW),
                Component.text(estado, NamedTextColor.AQUA))));

        Dificuldade[] difs = Dificuldade.values();
        for (int i = 0; i < difs.length && i < S_DIFICULDADES.length; i++) {
            Dificuldade d = difs[i];
            int precisa = d.nivelTotalNecessario(maxTotal);
            boolean nivelOk = nivelTotal >= precisa;
            boolean pago = p.getInventory().containsAtLeast(new ItemStack(d.custo()), d.qtdCusto());
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(d.salasMin() + " a " + d.salasMax() + " salas • " + d.ondas() + " ondas por sala", NamedTextColor.GRAY));
            lore.add(Component.text("Monstros: vida x" + d.vida() + ", dano x" + d.dano(), NamedTextColor.GRAY));
            StringBuilder chefes = new StringBuilder();
            for (Chefe c : d.chefes()) chefes.append(chefes.isEmpty() ? "" : " ou ").append(c.nome());
            lore.add(Component.text("Chefe: " + chefes, NamedTextColor.GRAY));
            lore.add(Component.text("Tesouro: forjados " + d.raridadeMin().nome() + " a " + d.raridadeMax().nome(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text((nivelOk ? "✔" : "✖") + " Nível total " + precisa, nivelOk ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text((pago ? "✔ " : "✖ ") + d.qtdCusto() + "x ", pago ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.translatable(d.custo().translationKey())));
            lore.add(Component.empty());
            lore.add(nivelOk && pago ? Component.text("» Clique para abrir o portal", NamedTextColor.YELLOW)
                    : Component.text("Ainda não dá.", NamedTextColor.DARK_GRAY));
            ItemStack icone = item(nivelOk ? icone(d) : Material.GRAY_DYE, Component.text("۞ " + d.nome(), d.cor(), TextDecoration.BOLD), lore);
            if (nivelOk && pago) icone.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_DIFICULDADES[i], icone);
        }

        int pacotes = masmorras().pacotes(p.getUniqueId());
        inv.setItem(S_COFRE, item(pacotes > 0 ? Material.SOUL_LANTERN : Material.LANTERN,
                Component.text("☠ Cofre das Almas", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), pacotes > 0 ? List.of(
                        Component.text(pacotes + " morte(s), " + masmorras().itensNoCofre(p.getUniqueId()) + " item(ns) guardados.", NamedTextColor.WHITE),
                        Component.text("Cada morte custa " + plugin.settings().masCustoCofre + " diamantes.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("» Clique para recuperar a mais antiga", NamedTextColor.YELLOW))
                        : List.of(Component.text("Vazio. Que continue assim!", NamedTextColor.GRAY))));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static Material icone(Dificuldade d) {
        return switch (d) {
            case FACIL -> Material.IRON_SWORD;
            case NORMAL -> Material.DIAMOND_SWORD;
            case DIFICIL -> Material.NETHERITE_SWORD;
            case PESADELO -> Material.WITHER_SKELETON_SKULL;
        };
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (!eh(t.portal.getBlock())) {
            p.closeInventory();
            return;
        }
        if (slot == S_COFRE) {
            String erro = masmorras().recuperar(p);
            if (erro != null) erro(p, erro);
            desenhar(p, t);
            return;
        }
        Dificuldade[] difs = Dificuldade.values();
        for (int i = 0; i < difs.length && i < S_DIFICULDADES.length; i++) {
            if (S_DIFICULDADES[i] != slot) continue;
            String erro = masmorras().abrir(p, difs[i], t.portal);
            if (erro != null) erro(p, erro);
            else p.closeInventory();
            return;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
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
