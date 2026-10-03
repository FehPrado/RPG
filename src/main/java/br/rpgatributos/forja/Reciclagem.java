package br.rpgatributos.forja;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Cozinha;
import br.rpgatributos.lenda.Lendas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bancada de Reciclagem: um rebolo onde se jogou 1 funil e 1 tesoura.
 * Desmonta equipamentos (itens que não empilham e têm receita) e devolve parte
 * do material. Itens forjados Lendários/Míticos de diamante ou netherite podem
 * render Fragmento de Forja, e às vezes sai um encantamento como livro.
 */
public final class Reciclagem extends Estacao {

    private static final TextColor COR = TextColor.color(0x9ACD32);
    private static final int R_INFO = 4;
    private static final int R_ITEM = 20;
    private static final int R_RESULTADO = 24;
    private static final int R_RECICLAR = 40;
    private static final int R_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location bancada;
        int slot = -1;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Reciclagem(RPGAtributos plugin) {
        super(plugin, "reciclagens", "reciclagem_display");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Ritual: 1 funil + 1 tesoura em cima de um rebolo
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.GRINDSTONE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.HOPPER || m == Material.SHEARS;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.GRINDSTONE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.HOPPER) < 1 || contar(itens, Material.SHEARS) < 1) return false;
        tirar(itens, Material.HOPPER, 1);
        tirar(itens, Material.SHEARS, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.SHEARS);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.3;
    }

    @Override
    protected Component nome() {
        return Component.text("♻ Bancada de Reciclagem ♻", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.CRIT, c, 40, 0.4, 0.4, 0.4, 0.2);
        w.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1f, 0.8f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("♻ Bancada de Reciclagem ♻", COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito para desmontar itens", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.SCRAPE, centro.clone().subtract(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.HOPPER));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.SHEARS));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.bancada = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♻ Reciclagem"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    // =====================================================================
    //  O que volta ao reciclar
    // =====================================================================

    /** Fração do material que volta: 50% (+25% no nível máximo de Ferraria), menos se o item estiver gasto. */
    private double fator(Player p, ItemStack item) {
        double f = 0.5 + 0.25 * plugin.stats().getNivel(p, Skill.FERRARIA) / plugin.settings().nivelMaximo;
        if (item.getItemMeta() instanceof Damageable d && item.getType().getMaxDurability() > 0) {
            f *= 1 - (double) d.getDamage() / item.getType().getMaxDurability();
        }
        return f;
    }

    /** Receita comum do jogo para o item (para saber do que ele é feito). */
    private static Recipe receita(Material tipo) {
        for (Recipe r : Bukkit.getRecipesFor(new ItemStack(tipo))) {
            if ((r instanceof ShapedRecipe || r instanceof ShapelessRecipe)
                    && r instanceof Keyed k && k.getKey().getNamespace().equals("minecraft")) {
                return r;
            }
        }
        return null;
    }

    private static Material material(RecipeChoice c) {
        if (c instanceof RecipeChoice.MaterialChoice mc && !mc.getChoices().isEmpty()) return mc.getChoices().getFirst();
        if (c instanceof RecipeChoice.ExactChoice ec && !ec.getChoices().isEmpty()) return ec.getChoices().getFirst().getType();
        return null;
    }

    public boolean reciclavel(ItemStack item) {
        if (item == null || item.isEmpty() || item.getMaxStackSize() > 1) return false;
        if (Raro.de(item) != null || item.getPersistentDataContainer().has(Cozinha.CHAVE_PRATO)) return false;
        if (plugin.arcano().ehGrimorio(item) || Lendas.marcado(item)) return false;
        return receita(base(item.getType())) != null;
    }

    /** Netherite é melhoria de diamante: recicla como diamante + sucata de netherite. */
    private static Material base(Material tipo) {
        if (tipo.name().startsWith("NETHERITE_") && Categoria.de(tipo) != null) {
            Material diamante = Material.matchMaterial(tipo.name().replace("NETHERITE_", "DIAMOND_"));
            if (diamante != null) return diamante;
        }
        return tipo;
    }

    /** Materiais devolvidos (sem os bônus aleatórios). */
    private Map<Material, Integer> materiais(Player p, ItemStack item) {
        Map<Material, Integer> m = new LinkedHashMap<>();
        Recipe r = receita(base(item.getType()));
        if (r == null) return m;
        Map<Material, Integer> usados = new LinkedHashMap<>();
        if (r instanceof ShapedRecipe s) {
            Map<Character, RecipeChoice> escolhas = s.getChoiceMap();
            for (String linha : s.getShape()) {
                for (char ch : linha.toCharArray()) {
                    Material mat = material(escolhas.get(ch));
                    if (mat != null) usados.merge(mat, 1, Integer::sum);
                }
            }
        } else if (r instanceof ShapelessRecipe s) {
            for (RecipeChoice c : s.getChoiceList()) {
                Material mat = material(c);
                if (mat != null) usados.merge(mat, 1, Integer::sum);
            }
        }
        double f = fator(p, item);
        int rendimento = Math.max(1, r.getResult().getAmount());
        usados.forEach((mat, qtd) -> {
            int volta = (int) Math.floor(qtd * f / rendimento);
            if (volta > 0) m.put(mat, volta);
        });
        if (base(item.getType()) != item.getType()) {
            int sucata = (int) Math.floor(4 * f); // 1 lingote de netherite = 4 sucatas
            if (sucata > 0) m.put(Material.NETHERITE_SCRAP, sucata);
        }
        return m;
    }

    // =====================================================================
    //  Tela
    // =====================================================================

    private ItemStack selecionado(Player p, Tela t) {
        if (t.slot < 0) return null;
        ItemStack i = p.getInventory().getItem(t.slot);
        return reciclavel(i) ? i : null;
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        int ferraria = plugin.stats().getNivel(p, Skill.FERRARIA);
        int max = plugin.settings().nivelMaximo;
        inv.setItem(R_INFO, item(Material.GRINDSTONE, Component.text("♻ Reciclagem", COR, TextDecoration.BOLD), List.of(
                Component.text("Desmonta armas, ferramentas, armaduras", NamedTextColor.GRAY),
                Component.text("e outros itens que não empilham.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Volta " + Math.round((0.5 + 0.25 * ferraria / max) * 100) + "% do material (item inteiro).", NamedTextColor.GREEN),
                Component.text("Item gasto devolve menos.", NamedTextColor.DARK_GRAY),
                Component.empty(),
                Component.text("Encantado: chance de virar livro.", NamedTextColor.LIGHT_PURPLE),
                Component.text("Lendário/Mítico de diamante ou netherite:", NamedTextColor.GOLD),
                Component.text("chance de Fragmento de Forja.", NamedTextColor.GOLD)), false));

        ItemStack alvo = selecionado(p, t);
        if (alvo == null) {
            t.slot = -1;
            inv.setItem(R_ITEM, item(Material.OAK_SIGN, Component.text("Nenhum item", NamedTextColor.GRAY), List.of(
                    Component.text("Clique num item do seu inventário.", NamedTextColor.DARK_GRAY)), false));
        } else {
            inv.setItem(R_ITEM, alvo.clone());
            List<Component> lore = new ArrayList<>();
            materiais(p, alvo).forEach((mat, qtd) -> lore.add(Component.text(" " + qtd + "x ", NamedTextColor.WHITE)
                    .append(Component.translatable(mat.translationKey()))));
            if (lore.isEmpty()) lore.add(Component.text(" nada (item muito gasto)", NamedTextColor.RED));
            if (!alvo.getEnchantments().isEmpty()) {
                lore.add(Component.text(" + chance de " + Forja.pct(chanceLivro(p)) + " de um livro encantado", NamedTextColor.LIGHT_PURPLE));
            }
            double frag = chanceFragmento(alvo);
            if (frag > 0) lore.add(Component.text(" + chance de " + Forja.pct(frag) + " de Fragmento de Forja", NamedTextColor.GOLD));
            inv.setItem(R_RESULTADO, item(Material.CHEST, Component.text("Você recebe", NamedTextColor.GOLD, TextDecoration.BOLD), lore, false));
        }
        inv.setItem(R_RECICLAR, item(Material.GRINDSTONE, Component.text("♻ Reciclar!", COR, TextDecoration.BOLD),
                List.of(Component.text(alvo == null ? "Escolha um item primeiro." : "O item será destruído.", NamedTextColor.GRAY)), false));
        inv.setItem(R_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private double chanceLivro(Player p) {
        return 0.25 + 0.25 * plugin.stats().getNivel(p, Skill.FERRARIA) / plugin.settings().nivelMaximo;
    }

    private double chanceFragmento(ItemStack item) {
        DadosForja d = plugin.forja().ler(item);
        if (d == null) return 0;
        Tier tier = Tier.de(item.getType());
        if (tier != Tier.DIAMANTE && tier != Tier.NETHERITE) return 0;
        if (d.raridade() == Raridade.MITICO) return 0.5;
        if (d.raridade() == Raridade.LENDARIO) return 0.15;
        return 0;
    }

    private void reciclar(Player p, Tela t) {
        ItemStack alvo = selecionado(p, t);
        if (alvo == null) {
            erro(p, "Escolha um item reciclável do seu inventário.");
            return;
        }
        if (t.bancada != null && !eh(t.bancada.getBlock())) {
            erro(p, "A Bancada de Reciclagem não existe mais.");
            p.closeInventory();
            return;
        }
        if (t.bancada != null && (!p.getWorld().equals(t.bancada.getWorld())
                || p.getLocation().distanceSquared(t.bancada.clone().add(0.5, 0.5, 0.5)) > 8 * 8)) {
            erro(p, "Você está longe da Bancada de Reciclagem.");
            p.closeInventory();
            return;
        }
        List<ItemStack> volta = new ArrayList<>();
        materiais(p, alvo).forEach((mat, qtd) -> volta.add(new ItemStack(mat, qtd)));
        Map<Enchantment, Integer> encantos = alvo.getEnchantments();
        if (!encantos.isEmpty() && rnd().nextDouble() < chanceLivro(p)) {
            List<Map.Entry<Enchantment, Integer>> lista = new ArrayList<>(encantos.entrySet());
            Map.Entry<Enchantment, Integer> sorteado = lista.get(rnd().nextInt(lista.size()));
            ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
            livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(sorteado.getKey(), sorteado.getValue(), true));
            volta.add(livro);
        }
        DadosForja d = plugin.forja().ler(alvo);
        int fragmentos = rnd().nextDouble() < chanceFragmento(alvo) ? 1 : 0;
        if (d != null && d.refino() >= 5) fragmentos++; // o refino investido volta um pouco
        if (fragmentos > 0) volta.add(Raro.FRAGMENTO_DE_FORJA.criar(fragmentos));

        p.getInventory().setItem(t.slot, null);
        t.slot = -1;
        for (ItemStack i : volta) {
            p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        if (p.getGameMode() != GameMode.CREATIVE) {
            plugin.stats().darXp(p, Skill.FERRARIA, d != null && d.raridade().peloMenos(Raridade.EPICO) ? 13 : 3);
        }
        plugin.titulos().registrar(p, "reciclados", 1);
        Location c = t.bancada != null ? t.bancada.clone().add(0.5, 1, 0.5) : p.getLocation();
        c.getWorld().spawnParticle(Particle.CRIT, c, 25, 0.3, 0.3, 0.3, 0.2);
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        p.sendMessage(Component.text("♻ Reciclado! Você recebeu " + volta.size() + " tipo(s) de material"
                + (fragmentos > 0 ? " e " + fragmentos + " Fragmento(s) de Forja" : "") + ".", COR));
        desenhar(p, t);
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        if (e.getRawSlot() >= topo.getSize()) {
            ItemStack it = e.getCurrentItem();
            if (it == null || it.isEmpty()) return;
            if (!reciclavel(it)) {
                erro(p, "Esse item não pode ser reciclado.");
                return;
            }
            t.slot = e.getSlot();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            desenhar(p, t);
            return;
        }
        if (e.getSlot() == R_FECHAR) p.closeInventory();
        else if (e.getSlot() == R_RECICLAR) reciclar(p, t);
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
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
