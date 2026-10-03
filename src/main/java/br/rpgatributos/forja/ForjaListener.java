package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Onde a forja acontece:
 * - bancada: armas, ferramentas e armaduras saem forjadas;
 * - mesa de ferraria: virar Netherite mantém a raridade (ou forja itens achados no mundo);
 * - fornalha: tirar lingotes dá XP de Ferraria;
 * - bigorna: renomear mantém a cor da raridade.
 *
 * Na bancada e na mesa o clique é cancelado e o item é entregue pelo plugin,
 * porque cada unidade precisa de um sorteio próprio (inclusive no shift+clique).
 */
public final class ForjaListener implements Listener {

    /** Distância máxima entre a mesa de ferraria e uma Forja do Ferreiro para forjar itens achados. */
    private static final double RAIO_MESA = 6;

    private final RPGAtributos plugin;
    private final Set<UUID> avisados = new HashSet<>();

    public ForjaListener(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Settings cfg() { return plugin.settings(); }
    private Forja forja() { return plugin.forja(); }

    private static boolean ganhaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    /** Só receitas normais do jogo (não a de juntar dois itens gastos, nem de outros plugins). */
    private boolean forjavel(ItemStack resultado, Recipe receita) {
        if (resultado == null || resultado.isEmpty() || Categoria.de(resultado.getType()) == null) return false;
        if (forja().forjado(resultado)) return false;
        if (receita == null || receita instanceof ComplexRecipe) return false;
        return receita instanceof Keyed k && k.getKey().getNamespace().equals("minecraft");
    }

    // =====================================================================
    //  Bancada
    // =====================================================================

    /** Está fabricando num lugar que forja? (com a regra padrão, só a Forja do Ferreiro) */
    private boolean lugarDeForjar(Player p, Inventory inv) {
        return !cfg().forjaExigeEstacao || plugin.forjas().naForja(p, inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoPrepararReceita(PrepareItemCraftEvent e) {
        if (!cfg().forjaAtivada) return;
        CraftingInventory inv = e.getInventory();
        if (!forjavel(inv.getResult(), e.getRecipe())) return;
        if (e.getView().getPlayer() instanceof Player p && lugarDeForjar(p, inv)) {
            inv.setResult(forja().previa(inv.getResult(), p));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFabricar(CraftItemEvent e) {
        if (!cfg().forjaAtivada || !(e.getWhoClicked() instanceof Player p)) return;
        CraftingInventory inv = e.getInventory();
        ItemStack resultado = inv.getResult();
        if (!forjavel(resultado, e.getRecipe())) return;
        if (!lugarDeForjar(p, inv)) {
            dica(p);
            return; // bancada comum: item normal do jogo
        }

        e.setCancelled(true);
        ClickType clique = e.getClick();
        if (!suportado(clique)) return;
        Material tipo = resultado.getType();
        int hotbar = e.getHotbarButton();
        plugin.getServer().getScheduler().runTask(plugin, () -> fabricar(p, inv, tipo, clique, hotbar));
    }

    private void fabricar(Player p, CraftingInventory inv, Material tipo, ClickType clique, int hotbar) {
        if (!aberto(p, inv)) return;
        int vezes = clique.isShiftClick() ? maxFabricacoes(inv.getMatrix()) : 1;

        List<ItemStack> feitos = new ArrayList<>();
        for (int i = 0; i < vezes; i++) {
            ItemStack atual = inv.getResult();
            if (atual == null || atual.getType() != tipo) break;
            ItemStack item = forja().forjar(p, new ItemStack(tipo), null);
            if (!entregar(p, item, clique, hotbar)) {
                if (feitos.isEmpty()) semEspaco(p);
                break;
            }
            consumir(inv);
            feitos.add(item);
        }
        if (feitos.isEmpty()) return;
        p.updateInventory();

        if (ganhaXp(p)) plugin.stats().darXp(p, Skill.FERRARIA, forja().xpFabricacao(tipo) * feitos.size());
        forja().celebrar(p, local(inv, p), feitos);
        forja().registrarConquistas(p, feitos);
    }

    /** Avisa uma vez por sessão que na Forja do Ferreiro o item sairia forjado. */
    private void dica(Player p) {
        if (!avisados.add(p.getUniqueId())) return;
        p.sendMessage(Component.text("⚒ Dica: ", Skill.FERRARIA.cor())
                .append(Component.text("feito numa ", NamedTextColor.GRAY))
                .append(Component.text("Forja do Ferreiro", Skill.FERRARIA.cor()))
                .append(Component.text(", esse item sairia com raridade, bônus e efeitos. Veja o /guia.", NamedTextColor.GRAY)));
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        avisados.remove(e.getPlayer().getUniqueId());
    }

    private static int maxFabricacoes(ItemStack[] matriz) {
        int min = Integer.MAX_VALUE;
        for (ItemStack s : matriz) {
            if (s != null && !s.isEmpty()) min = Math.min(min, s.getAmount());
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }

    private static void consumir(CraftingInventory inv) {
        ItemStack[] m = inv.getMatrix();
        for (int i = 0; i < m.length; i++) {
            ItemStack s = m[i];
            if (s == null || s.isEmpty()) continue;
            if (s.getAmount() > 1) {
                s.setAmount(s.getAmount() - 1);
            } else {
                Material sobra = s.getType().getCraftingRemainingItem();
                m[i] = sobra == null ? null : new ItemStack(sobra);
            }
        }
        inv.setMatrix(m);
    }

    // =====================================================================
    //  Mesa de ferraria (melhoria para Netherite)
    // =====================================================================

    /** Melhoria = o item muda de tipo (diamante → netherite). Acabamento de armadura não conta. */
    private static boolean melhoria(ItemStack equipamento, ItemStack resultado) {
        return equipamento != null && !equipamento.isEmpty()
                && resultado != null && !resultado.isEmpty()
                && resultado.getType() != equipamento.getType()
                && Categoria.de(resultado.getType()) != null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoPrepararFerraria(PrepareSmithingEvent e) {
        ItemStack resultado = e.getResult();
        ItemStack equipamento = e.getInventory().getInputEquipment();
        if (!melhoria(equipamento, resultado)) return;

        if (forja().forjado(equipamento)) {
            // Mostra já com os valores de Netherite.
            ItemStack previa = resultado.clone();
            forja().reconstruir(previa);
            e.setResult(previa);
        } else if (mesaForja(e.getInventory()) && e.getView().getPlayer() instanceof Player p) {
            e.setResult(forja().previa(resultado, p));
        }
    }

    /** Itens achados no mundo só são forjados numa mesa de ferraria perto de uma Forja do Ferreiro. */
    private boolean mesaForja(SmithingInventory inv) {
        if (!cfg().forjaAtivada) return false;
        if (!cfg().forjaExigeEstacao) return true;
        Location l = inv.getLocation();
        return l != null && !plugin.forjas().perto(l, RAIO_MESA).isEmpty();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerrar(SmithItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        SmithingInventory inv = e.getInventory();
        ItemStack equipamento = inv.getInputEquipment();
        ItemStack resultado = inv.getResult();
        if (!melhoria(equipamento, resultado)) return;
        if (!forja().forjado(equipamento) && !mesaForja(inv)) return; // melhoria normal do jogo

        e.setCancelled(true);
        ClickType clique = e.getClick();
        if (!suportado(clique)) return;
        Material tipo = resultado.getType();
        int hotbar = e.getHotbarButton();
        plugin.getServer().getScheduler().runTask(plugin, () -> ferrar(p, inv, tipo, clique, hotbar));
    }

    private void ferrar(Player p, SmithingInventory inv, Material tipo, ClickType clique, int hotbar) {
        if (!aberto(p, inv)) return;
        ItemStack equipamento = inv.getInputEquipment();
        ItemStack resultado = inv.getResult();
        if (!melhoria(equipamento, resultado) || resultado.getType() != tipo) return;

        // Igual ao jogo: o item novo herda tudo do antigo (encantamentos, nome, dados da forja).
        ItemStack base = equipamento.withType(tipo);
        base.setAmount(1);
        boolean novo = !forja().forjado(equipamento);
        ItemStack item;
        if (novo) {
            item = forja().forjar(p, base, null);
        } else {
            forja().reconstruir(base);
            item = base;
        }
        if (!entregar(p, item, clique, hotbar)) {
            semEspaco(p);
            return;
        }
        inv.setInputTemplate(diminuir(inv.getInputTemplate()));
        inv.setInputEquipment(diminuir(inv.getInputEquipment()));
        inv.setInputMineral(diminuir(inv.getInputMineral()));
        p.updateInventory();

        Location onde = local(inv, p);
        onde.getWorld().playSound(onde, Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
        if (ganhaXp(p)) plugin.stats().darXp(p, Skill.FERRARIA, cfg().ferXpNetherite);
        if (novo) {
            forja().celebrar(p, onde, List.of(item));
            forja().registrarConquistas(p, List.of(item));
        } else {
            onde.getWorld().spawnParticle(Particle.SMALL_FLAME, onde.clone().add(0, 0.8, 0), 25, 0.3, 0.2, 0.3, 0.02);
            p.sendMessage(Component.text("⚒ Aprimorado: ", NamedTextColor.GRAY).append(forja().nomeComHover(item))
                    .append(Component.text(" (bônus mais fortes)", NamedTextColor.DARK_GRAY)));
        }
    }

    private static ItemStack diminuir(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        if (s.getAmount() <= 1) return null;
        ItemStack r = s.clone();
        r.setAmount(s.getAmount() - 1);
        return r;
    }

    // =====================================================================
    //  Fornalha e bigorna
    // =====================================================================

    @EventHandler
    public void aoTirarDaFornalha(FurnaceExtractEvent e) {
        Material m = e.getItemType();
        double peso;
        if (m == Material.IRON_INGOT || m == Material.GOLD_INGOT || m == Material.COPPER_INGOT) peso = 1;
        else if (m == Material.NETHERITE_SCRAP) peso = 4;
        else return;
        if (!ganhaXp(e.getPlayer())) return;
        plugin.stats().darXp(e.getPlayer(), Skill.FERRARIA, e.getItemAmount() * peso * cfg().ferXpLingote);
    }

    /** Renomear item forjado na bigorna mantém a cor da raridade (e sem itálico). */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoPrepararBigorna(PrepareAnvilEvent e) {
        ItemStack resultado = e.getResult();
        DadosForja d = forja().ler(resultado);
        if (d == null) return;
        String texto = e.getView().getRenameText();
        if (texto == null || texto.isBlank()) return;
        ItemStack novo = resultado.clone();
        novo.editMeta(m -> {
            if (m.hasCustomName()) {
                m.customName(Component.text(texto, d.raridade().cor()).decoration(TextDecoration.ITALIC, false));
            }
        });
        e.setResult(novo);
    }

    // =====================================================================
    //  Entregar o item
    // =====================================================================

    private static boolean aberto(Player p, Inventory inv) {
        return p.isOnline() && p.getOpenInventory().getTopInventory().equals(inv);
    }

    private static Location local(Inventory inv, Player p) {
        Location l = inv.getLocation();
        return l != null ? l.toCenterLocation() : p.getLocation();
    }

    private static void semEspaco(Player p) {
        p.sendActionBar(Component.text("Sem espaço para pegar o item!", NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    /** Cliques que pegam o resultado; os outros (botão do meio etc.) são só ignorados. */
    private static boolean suportado(ClickType c) {
        return switch (c) {
            case LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, NUMBER_KEY, SWAP_OFFHAND, DROP, CONTROL_DROP -> true;
            default -> false;
        };
    }

    /** Coloca o item onde o jogador pediu com o clique. */
    private static boolean entregar(Player p, ItemStack item, ClickType clique, int hotbar) {
        PlayerInventory inv = p.getInventory();
        switch (clique) {
            case LEFT, RIGHT -> {
                if (!p.getItemOnCursor().isEmpty()) return false;
                p.setItemOnCursor(item);
                return true;
            }
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                if (inv.firstEmpty() == -1) return false;
                inv.addItem(item);
                return true;
            }
            case NUMBER_KEY -> {
                if (hotbar < 0 || hotbar > 8) return false;
                ItemStack atual = inv.getItem(hotbar);
                if (atual != null && !atual.isEmpty()) return false;
                inv.setItem(hotbar, item);
                return true;
            }
            case SWAP_OFFHAND -> {
                if (!inv.getItemInOffHand().isEmpty()) return false;
                inv.setItemInOffHand(item);
                return true;
            }
            case DROP, CONTROL_DROP -> {
                p.getWorld().dropItem(p.getEyeLocation(), item,
                        d -> d.setVelocity(p.getLocation().getDirection().multiply(0.3)));
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
