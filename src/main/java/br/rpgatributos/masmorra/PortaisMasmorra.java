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
    private static final int[] S_RANKS = {19, 20, 21, 23, 24, 25};
    private static final int S_CACADOR = 31, S_LOJA = 33, S_AVALIADOR = 29;

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
        return Component.text("۞ Altar do Portal ۞", Masmorras.COR)
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
        quem.showTitle(Title.title(Component.text("۞ Altar do Portal ۞", Masmorras.COR, TextDecoration.BOLD),
                Component.text("Clique para fazer um Cristal de Portal", NamedTextColor.GRAY),
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
        // 2.68: com um Cristal de Portal na mão, o altar abre a masmorra dele.
        ItemStack mao = p.getInventory().getItemInMainHand();
        CristalPortal.Dados c = CristalPortal.ler(mao);
        if (c != null) {
            String erro = masmorras().abrirComCristal(p, c, b.getLocation());
            if (erro != null) erro(p, erro);
            else if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
            return;
        }
        // 2.71: o Avaliador revela o equipamento não identificado por 1 de mana (com chance de raridade a mais).
        if (EquipamentoPortal.naoIdentificado(mao)) {
            if (p.getGameMode() != org.bukkit.GameMode.CREATIVE && !ManaCristal.gastar(p, 1)) {
                erro(p, "O Avaliador cobra 1 de mana em Cristais de Mana (ou clique com o item fora do altar).");
                return;
            }
            p.getInventory().setItemInMainHand(plugin.equipamentoPortal().revelar(p, mao, true));
            return;
        }
        Tela t = new Tela();
        t.portal = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("۞ Altar do Portal"));
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
        int nivelTotal = plugin.renascimento().nivelTotalEfetivo(p);
        int maxTotal = plugin.settings().nivelMaximo * Skill.values().length;
        Rank cacador = Cacador.rank(p), maximo = Cacador.maximoCristal(p);
        String estado = masmorras().portalAberto(t.portal) ? "O portal está aberto: suba nele!"
                : masmorras().formando(t.portal) ? "Uma masmorra está se formando..." : "Faça um cristal abaixo, ou clique no altar com um na mão.";
        inv.setItem(S_INFO, item(Material.RECOVERY_COMPASS, Component.text("۞ Altar do Portal", Masmorras.COR, TextDecoration.BOLD), List.of(
                Component.text("1. Faça um Cristal de Portal (oferenda abaixo).", NamedTextColor.GRAY),
                Component.text("2. Clique no altar com o cristal na mão.", NamedTextColor.GRAY),
                Component.text("3. Suba no altar com a sua party.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Cada cristal: rank, objetivo (Caçada, Saque", NamedTextColor.GRAY),
                Component.text("ou Monólitos) e às vezes modificadores.", NamedTextColor.GRAY),
                Component.text("15 minutos: quem não sai a tempo perde", NamedTextColor.GRAY),
                Component.text("o que pegou lá dentro.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Seu nível total: " + nivelTotal, NamedTextColor.YELLOW),
                Component.text(estado, NamedTextColor.AQUA))));

        int mana = ManaCristal.valor(p);
        for (Rank r : Rank.values()) {
            int precisa = r.dificuldade().nivelTotalNecessario(maxTotal);
            boolean rankOk = r.ordinal() <= maximo.ordinal() || p.getGameMode() == org.bukkit.GameMode.CREATIVE;
            boolean ofOk = CristalPortal.temOferenda(p, r);
            boolean manaOk = mana >= CristalPortal.valorMana(r);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(r.nomeCompleto() + " • monstros x" + String.format(java.util.Locale.ROOT, "%.2f", r.forca()).replace('.', ','), NamedTextColor.GRAY));
            lore.add(Component.text("Nível total para entrar: " + precisa + (nivelTotal >= precisa ? " ✔" : " ✖"), nivelTotal >= precisa ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Oferenda (clique esquerdo):", NamedTextColor.WHITE));
            for (ItemStack i : CristalPortal.oferenda(p, r)) {
                boolean tem = p.getInventory().containsAtLeast(new ItemStack(i.getType()), i.getAmount());
                lore.add(Component.text((tem ? " ✔ " : " ✖ ") + i.getAmount() + "x ", tem ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(Component.translatable(i.getType().translationKey())));
            }
            lore.add(Component.text("Ou " + CristalPortal.valorMana(r) + " de mana em cristais (clique direito)" + (manaOk ? " ✔" : " ✖"),
                    manaOk ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            if (!rankOk) lore.add(Component.text("✖ Caçadores " + Rank.values()[Math.max(0, r.ordinal() - 1)].nome() + " ou acima.", NamedTextColor.RED));
            else lore.add(ofOk || manaOk ? Component.text("» Clique para fazer o cristal", NamedTextColor.YELLOW)
                    : Component.text("Ainda falta a oferenda.", NamedTextColor.DARK_GRAY));
            ItemStack icone = item(rankOk ? Material.AMETHYST_SHARD : Material.GRAY_DYE, Component.text("Cristal de Portal · " + r.nome(), r.cor(), TextDecoration.BOLD), lore);
            if (rankOk && (ofOk || manaOk)) icone.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(S_RANKS[r.ordinal()], icone);
        }

        inv.setItem(S_CACADOR, item(Material.NAME_TAG, Component.text("[Sistema] Caçador " + cacador.nome(), cacador.cor(), TextDecoration.BOLD), List.of(
                Component.text(Cacador.xp(p) + " XP de caçador" + (cacador == Rank.S ? "" : " (faltam " + Cacador.falta(p) + " para " + cacador.acima().nome() + ")"), NamedTextColor.GRAY),
                Component.text("Sobe vencendo portais (do altar e do mundo).", NamedTextColor.GRAY),
                Component.text("O altar faz cristais até " + maximo.nome() + ".", NamedTextColor.AQUA),
                Component.text("Cristais de Mana: " + mana + " de mana.", ManaCristal.COR),
                Component.empty(),
                Component.text("» Clique: Árvore de Caçador e Despertar", NamedTextColor.YELLOW))));
        inv.setItem(S_LOJA, item(Material.AMETHYST_CLUSTER, Component.text("[Sistema] Loja", LojaSistema.COR, TextDecoration.BOLD), List.of(
                Component.text("Poções, Pergaminho de Retorno, Medidor de Mana,", NamedTextColor.GRAY),
                Component.text("Chave Instantânea e peças de Caçador.", NamedTextColor.GRAY),
                Component.text("Paga com Cristais de Mana.", ManaCristal.COR),
                Component.empty(),
                Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        inv.setItem(S_AVALIADOR, item(Material.SPYGLASS, Component.text("Avaliador", EquipamentoPortal.COR, TextDecoration.BOLD), List.of(
                Component.text("Clique no altar com um equipamento", NamedTextColor.GRAY),
                Component.text("não identificado na mão: ele revela", NamedTextColor.GRAY),
                Component.text("por 1 de mana, com 15% de chance", NamedTextColor.GRAY),
                Component.text("de subir uma raridade.", NamedTextColor.GRAY),
                Component.text("(Sem o altar, clique com ele na mão.)", NamedTextColor.DARK_GRAY))));

        int pacotes = masmorras().pacotes(p.getUniqueId());
        inv.setItem(S_COFRE, item(pacotes > 0 ? Material.SOUL_LANTERN : Material.LANTERN,
                Component.text("☠ Cofre das Almas", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), pacotes > 0 ? List.of(
                        Component.text(pacotes + " pacote(s), " + masmorras().itensNoCofre(p.getUniqueId()) + " item(ns) guardados.", NamedTextColor.WHITE),
                        Component.text("Cada pacote custa " + plugin.settings().masCustoCofre + " diamantes.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("» Clique para recuperar o mais antigo", NamedTextColor.YELLOW))
                        : List.of(Component.text("Vazio. Que continue assim!", NamedTextColor.GRAY))));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (slot == S_LOJA) { plugin.lojaSistema().abrir(p); return; }
        if (slot == S_CACADOR) { plugin.sistemaCacador().abrir(p); return; }
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
        for (Rank r : Rank.values()) {
            if (S_RANKS[r.ordinal()] != slot) continue;
            boolean criativo = p.getGameMode() == org.bukkit.GameMode.CREATIVE;
            if (!criativo && r.ordinal() > Cacador.maximoCristal(p).ordinal()) {
                erro(p, "O altar não responde: seu Rank de Caçador ainda é " + Cacador.rank(p).nome() + ".");
                return;
            }
            if (!criativo) {
                if (e.isRightClick()) {
                    if (!ManaCristal.gastar(p, CristalPortal.valorMana(r))) {
                        erro(p, "Faltam Cristais de Mana (" + CristalPortal.valorMana(r) + " de mana).");
                        return;
                    }
                } else {
                    if (!CristalPortal.temOferenda(p, r)) {
                        erro(p, "Falta a oferenda (ou clique direito para pagar com Cristais de Mana).");
                        return;
                    }
                    CristalPortal.pagarOferenda(p, r);
                }
            }
            ItemStack cristal = CristalPortal.sortear(r);
            for (ItemStack s : p.getInventory().addItem(cristal).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            p.sendMessage(Component.text("۞ O altar forma um Cristal de Portal " + r.nome() + ". Clique no altar com ele na mão para abrir.", r.cor()));
            desenhar(p, t);
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
