package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Refino +1 a +10 na Forja do Ferreiro (agachado + clique direito).
 * Cada nível deixa o item mais forte; a partir do +6 falhar faz perder nível,
 * e no +9/+10 o item pode quebrar. A Pedra de Proteção evita as duas coisas.
 */
public final class Refinaria implements Listener {

    /** Material comum, quantidade, segundo material, fragmentos, essências, níveis de XP. */
    private record Custo(Material material, int qtd, Material material2, int qtd2,
                         int fragmentos, int essencias, int niveis) {}

    /** Custo para chegar no nível (índice 0 = +1). */
    private static final Custo[] CUSTOS = {
            new Custo(Material.IRON_INGOT, 4, null, 0, 0, 0, 2),
            new Custo(Material.IRON_INGOT, 8, null, 0, 0, 0, 3),
            new Custo(Material.IRON_INGOT, 8, Material.GOLD_INGOT, 4, 0, 0, 4),
            new Custo(Material.DIAMOND, 2, null, 0, 0, 0, 5),
            new Custo(Material.DIAMOND, 4, null, 0, 0, 0, 6),
            new Custo(Material.DIAMOND, 6, null, 0, 1, 0, 8),
            new Custo(Material.NETHERITE_INGOT, 1, null, 0, 2, 0, 10),
            new Custo(Material.NETHERITE_INGOT, 2, null, 0, 3, 0, 12),
            new Custo(Material.NETHERITE_INGOT, 3, null, 0, 5, 0, 15),
            new Custo(Material.NETHERITE_INGOT, 4, null, 0, 8, 1, 20),
    };
    /** Chance base de sucesso para chegar no nível (índice 0 = +1). */
    private static final double[] CHANCES = {1, 1, 0.95, 0.85, 0.75, 0.65, 0.5, 0.4, 0.3, 0.2};
    /** A partir de qual nível falhar faz perder um nível. */
    private static final int PERDE_NIVEL_A_PARTIR = 6;
    /** A partir de qual nível falhar pode quebrar o item. */
    private static final int QUEBRA_A_PARTIR = 9;
    private static final double CHANCE_QUEBRAR = 1 / 3.0;

    private static final int R_INFO = 4;
    private static final int R_ITEM = 19;
    private static final int R_PROXIMO = 21;
    private static final int R_CUSTO = 23;
    private static final int R_PEDRA = 25;
    private static final int R_REFINAR = 40;
    private static final int R_GUIA = 45;
    private static final int R_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location forja;
        int slot = -1;
        boolean proteger;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;

    public Refinaria(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Forja forja() { return plugin.forja(); }

    public void abrir(Player p, Location forja) {
        Tela t = new Tela();
        t.forja = forja;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⚒ Refinar"));
        desenhar(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.6f);
    }

    private double chance(Player p, int alvo) {
        double bonus = 0.1 * plugin.stats().getNivel(p, Skill.FERRARIA) / plugin.settings().nivelMaximo
                + plugin.classes().chanceRefinoExtra(p);
        return Math.min(1, CHANCES[alvo - 1] + bonus);
    }

    private ItemStack itemSelecionado(Player p, Tela t) {
        if (t.slot < 0) return null;
        ItemStack i = p.getInventory().getItem(t.slot);
        return forja().forjado(i) ? i : null;
    }

    // =====================================================================
    //  Desenho
    // =====================================================================

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        int ferraria = plugin.stats().getNivel(p, Skill.FERRARIA);

        inv.setItem(R_INFO, item(Material.ANVIL, Component.text("⚒ Refino", Skill.FERRARIA.cor(), TextDecoration.BOLD), List.of(
                Component.text("Cada nível: +6% nos bônus da forja", NamedTextColor.GRAY),
                Component.text("e +4% no dano/armadura base.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Até +5: falhar só gasta o material.", NamedTextColor.GREEN),
                Component.text("+6 a +8: falhar perde 1 nível.", NamedTextColor.YELLOW),
                Component.text("+9 e +10: falhar pode QUEBRAR o item.", NamedTextColor.RED),
                Component.empty(),
                Component.text("Sua Ferraria (Nv " + ferraria + ") soma +"
                        + Forja.pct(0.1 * ferraria / plugin.settings().nivelMaximo) + " de chance.", NamedTextColor.DARK_GRAY))));

        ItemStack alvo = itemSelecionado(p, t);
        DadosForja d = alvo == null ? null : forja().ler(alvo);
        if (alvo == null) {
            t.slot = -1;
            inv.setItem(R_ITEM, item(Material.OAK_SIGN, Component.text("Nenhum item", NamedTextColor.GRAY), List.of(
                    Component.text("Clique num item forjado do seu", NamedTextColor.DARK_GRAY),
                    Component.text("inventário (aqui embaixo).", NamedTextColor.DARK_GRAY))));
        } else {
            inv.setItem(R_ITEM, alvo.clone());
        }

        if (d != null && d.refino() >= DadosForja.REFINO_MAXIMO) {
            inv.setItem(R_PROXIMO, item(Material.NETHER_STAR, Component.text("Refino máximo! (+10)", NamedTextColor.GOLD, TextDecoration.BOLD),
                    List.of(Component.text("Esse item já está no auge.", NamedTextColor.GRAY)), true));
        } else if (d != null) {
            int prox = d.refino() + 1;
            List<Component> lore = new ArrayList<>();
            lore.add(linha("Bônus da forja: ", "×" + StatsManager.fmt(d.fatorRefino()) + " → ×"
                    + StatsManager.fmt(d.comRefino(prox).fatorRefino()), NamedTextColor.GREEN));
            lore.add(linha("Dano/armadura base: ", "+" + Math.round(DadosForja.BASE_POR_REFINO * prox * 100) + "%", NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(linha("Chance de sucesso: ", Forja.pct(chance(p, prox)), NamedTextColor.YELLOW));
            lore.add(risco(prox));
            inv.setItem(R_PROXIMO, item(Material.PAPER, Component.text("Refinar para +" + prox, NamedTextColor.GOLD, TextDecoration.BOLD), lore));
            inv.setItem(R_CUSTO, iconeCusto(p, CUSTOS[prox - 1]));
        }

        int pedras = Raro.PEDRA_DE_PROTECAO.contar(p.getInventory());
        if (pedras == 0) t.proteger = false;
        inv.setItem(R_PEDRA, item(Raro.PEDRA_DE_PROTECAO.base(),
                Component.text("Pedra de Proteção: " + (t.proteger ? "LIGADA" : "desligada"),
                        t.proteger ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD), List.of(
                        Component.text("Se o refino falhar, o item não perde", NamedTextColor.GRAY),
                        Component.text("nível nem quebra. Só é gasta do +6 em diante.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Você tem: " + pedras, NamedTextColor.WHITE),
                        Component.text(pedras > 0 ? "» Clique para ligar/desligar" : "Caem de chefes do Altar Ritualístico.",
                                NamedTextColor.YELLOW)), t.proteger));

        inv.setItem(R_REFINAR, item(Material.SMITHING_TABLE, Component.text("⚒ Refinar!", Skill.FERRARIA.cor(), TextDecoration.BOLD),
                List.of(Component.text(d == null ? "Escolha um item primeiro." : "Gasta o custo e tenta a sorte.", NamedTextColor.GRAY))));
        inv.setItem(R_GUIA, item(Material.BOOK, Component.text("Guia da forja", NamedTextColor.YELLOW), List.of()));
        inv.setItem(R_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));

        ItemStack vidro = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static Component risco(int alvo) {
        if (alvo >= QUEBRA_A_PARTIR) return Component.text("Se falhar: perde 1 nível ou QUEBRA (1 em 3).", NamedTextColor.RED);
        if (alvo >= PERDE_NIVEL_A_PARTIR) return Component.text("Se falhar: perde 1 nível.", NamedTextColor.YELLOW);
        return Component.text("Se falhar: só perde o material.", NamedTextColor.GREEN);
    }

    private ItemStack iconeCusto(Player p, Custo c) {
        PlayerInventory inv = p.getInventory();
        List<Component> lore = new ArrayList<>();
        lore.add(custo(Component.translatable(c.material().translationKey()), c.qtd(),
                inv.containsAtLeast(new ItemStack(c.material()), c.qtd())));
        if (c.material2() != null) {
            lore.add(custo(Component.translatable(c.material2().translationKey()), c.qtd2(),
                    inv.containsAtLeast(new ItemStack(c.material2()), c.qtd2())));
        }
        if (c.fragmentos() > 0) {
            lore.add(custo(Component.text(Raro.FRAGMENTO_DE_FORJA.nome()), c.fragmentos(),
                    Raro.FRAGMENTO_DE_FORJA.contar(inv) >= c.fragmentos()));
        }
        if (c.essencias() > 0) {
            lore.add(custo(Component.text(Raro.ESSENCIA_PRIMORDIAL.nome()), c.essencias(),
                    Raro.ESSENCIA_PRIMORDIAL.contar(inv) >= c.essencias()));
        }
        boolean xp = p.getGameMode() == GameMode.CREATIVE || p.getLevel() >= c.niveis();
        lore.add(custo(Component.text("níveis de XP"), c.niveis(), xp));
        return item(Material.CHEST, Component.text("Custo", NamedTextColor.GOLD, TextDecoration.BOLD), lore);
    }

    private static Component custo(Component nome, int qtd, boolean tem) {
        TextColor cor = tem ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text((tem ? "✔ " : "✖ ") + qtd + "x ", cor).append(nome.color(cor));
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;

        if (e.getRawSlot() >= topo.getSize()) {
            ItemStack it = e.getCurrentItem();
            if (it == null || it.isEmpty()) return;
            if (!forja().forjado(it)) {
                erro(p, "Só itens forjados podem ser refinados.");
                return;
            }
            t.slot = e.getSlot();
            clique(p);
            desenhar(p, t);
            return;
        }
        switch (e.getSlot()) {
            case R_FECHAR -> p.closeInventory();
            case R_GUIA -> plugin.menus().abrirForja(p);
            case R_PEDRA -> {
                if (Raro.PEDRA_DE_PROTECAO.contar(p.getInventory()) == 0) {
                    erro(p, "Você não tem Pedra de Proteção. Elas caem de chefes.");
                    return;
                }
                t.proteger = !t.proteger;
                clique(p);
                desenhar(p, t);
            }
            case R_REFINAR -> refinar(p, t);
            default -> { }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Refinar
    // =====================================================================

    private void refinar(Player p, Tela t) {
        ItemStack alvo = itemSelecionado(p, t);
        if (alvo == null) {
            erro(p, "Escolha um item forjado do seu inventário.");
            return;
        }
        if (t.forja != null && !plugin.forjas().eh(t.forja.getBlock())) {
            erro(p, "A Forja do Ferreiro não existe mais.");
            p.closeInventory();
            return;
        }
        DadosForja d = forja().ler(alvo);
        if (d.refino() >= DadosForja.REFINO_MAXIMO) {
            erro(p, "Esse item já está no +10.");
            return;
        }
        int prox = d.refino() + 1;
        Custo c = CUSTOS[prox - 1];
        PlayerInventory inv = p.getInventory();
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            boolean temTudo = inv.containsAtLeast(new ItemStack(c.material()), c.qtd())
                    && (c.material2() == null || inv.containsAtLeast(new ItemStack(c.material2()), c.qtd2()))
                    && Raro.FRAGMENTO_DE_FORJA.contar(inv) >= c.fragmentos()
                    && Raro.ESSENCIA_PRIMORDIAL.contar(inv) >= c.essencias()
                    && p.getLevel() >= c.niveis();
            if (!temTudo) {
                erro(p, "Faltam materiais (veja o custo).");
                return;
            }
            inv.removeItem(new ItemStack(c.material(), c.qtd()));
            if (c.material2() != null) inv.removeItem(new ItemStack(c.material2(), c.qtd2()));
            Raro.FRAGMENTO_DE_FORJA.tirar(inv, c.fragmentos());
            Raro.ESSENCIA_PRIMORDIAL.tirar(inv, c.essencias());
            p.setLevel(p.getLevel() - c.niveis());
        }
        boolean protegido = false;
        if (t.proteger && prox >= PERDE_NIVEL_A_PARTIR) protegido = Raro.PEDRA_DE_PROTECAO.tirar(inv, 1);

        if (!criativo) plugin.stats().darXp(p, Skill.FERRARIA, 15.0 * prox);
        Location onde = t.forja != null ? t.forja.clone().add(0.5, 1.2, 0.5) : p.getLocation().add(0, 1, 0);
        boolean sucesso = ThreadLocalRandom.current().nextDouble() < chance(p, prox);

        if (sucesso) {
            aplicar(p, t.slot, alvo, d.comRefino(prox));
            onde.getWorld().spawnParticle(Particle.FLAME, onde, 30, 0.3, 0.3, 0.3, 0.05);
            onde.getWorld().spawnParticle(Particle.ENCHANTED_HIT, onde, 30, 0.3, 0.3, 0.3, 0.3);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            p.sendMessage(Component.text("⚒ Sucesso! ", NamedTextColor.GREEN).append(forja().nomeComHover(p.getInventory().getItem(t.slot))));
            if (prox == DadosForja.REFINO_MAXIMO) {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                plugin.getServer().broadcast(Component.text("⚒ ", NamedTextColor.GOLD)
                        .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" refinou um item até ", NamedTextColor.GRAY))
                        .append(Component.text("+10", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(forja().nomeComHover(p.getInventory().getItem(t.slot))));
                plugin.titulos().registrar(p, "refino10", 1);
            }
        } else {
            onde.getWorld().spawnParticle(Particle.LARGE_SMOKE, onde, 25, 0.3, 0.3, 0.3, 0.02);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
            if (prox < PERDE_NIVEL_A_PARTIR || protegido) {
                p.sendMessage(Component.text("✖ O refino falhou" + (protegido ? ", mas a Pedra de Proteção salvou o item." : ".")
                        + " (o material foi gasto)", NamedTextColor.RED));
            } else if (prox >= QUEBRA_A_PARTIR && ThreadLocalRandom.current().nextDouble() < CHANCE_QUEBRAR) {
                p.getInventory().setItem(t.slot, null);
                t.slot = -1;
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
                p.sendMessage(Component.text("✖ O refino falhou e o item QUEBROU!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            } else {
                aplicar(p, t.slot, alvo, d.comRefino(d.refino() - 1));
                p.sendMessage(Component.text("✖ O refino falhou e o item perdeu 1 nível (agora +" + (d.refino() - 1) + ").",
                        NamedTextColor.RED));
            }
        }
        desenhar(p, t);
    }

    private void aplicar(Player p, int slot, ItemStack item, DadosForja novo) {
        ItemStack copia = item.clone();
        forja().construir(copia, novo);
        p.getInventory().setItem(slot, copia);
    }

    // =====================================================================
    //  Ajudantes
    // =====================================================================

    private static Component linha(String rotulo, String valor, TextColor cor) {
        return Component.text(rotulo, NamedTextColor.GRAY).append(Component.text(valor, cor));
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return item(m, nome, lore, false);
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

    private static void clique(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
