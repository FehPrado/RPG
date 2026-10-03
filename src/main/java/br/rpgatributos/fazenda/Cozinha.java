package br.rpgatributos.fazenda;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.alquimia.Ingrediente;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.territorio.Flag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Smoker;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A Cozinha: um defumador onde se jogou 1 caldeirão e 1 balde de água.
 * Clique direito abre as receitas (e cozinha); agachado + clique abre o defumador normal.
 * Pratos dão buffs; a qualidade dos ingredientes e o nível de Culinária deixam eles mais fortes.
 */
public final class Cozinha extends Estacao {

    public static final NamespacedKey CHAVE_PRATO = new NamespacedKey("rpgatributos", "prato");
    public static final NamespacedKey CHAVE_FATOR = new NamespacedKey("rpgatributos", "prato_fator");
    private static final TextColor COR = TextColor.color(0xE8A33D);
    /** Ingredientes que podem ter qualidade (contam para melhorar o prato). */
    private static final Set<Material> COM_QUALIDADE = Set.of(
            Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.PUMPKIN,
            Material.SWEET_BERRIES, Material.GLOW_BERRIES, Material.COCOA_BEANS, Material.MELON_SLICE,
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH);
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int S_INFO = 4;
    private static final int S_FECHAR = 49;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        /** A Cozinha onde o jogador está (null = só o livro de receitas). */
        Location cozinha;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Cozinha(RPGAtributos plugin) {
        super(plugin, "cozinhas", "cozinha_display");
    }

    // =====================================================================
    //  Ritual: 1 caldeirão + 1 balde de água em cima de um defumador
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.SMOKER;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.CAULDRON || m == Material.WATER_BUCKET;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.SMOKER ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.CAULDRON) < 1 || contar(itens, Material.WATER_BUCKET) < 1) return false;
        tirar(itens, Material.CAULDRON, 1);
        tirar(itens, Material.WATER_BUCKET, 1);
        bloco.getWorld().dropItemNaturally(bloco.getLocation().add(0.5, 1.2, 0.5), new ItemStack(Material.BUCKET));
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.CAULDRON);
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
        return Component.text("♨ Cozinha ♨", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c, 20, 0.3, 0.3, 0.3, 0.02);
        w.spawnParticle(Particle.SPLASH, c, 40, 0.3, 0.2, 0.3);
        w.playSound(c, Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
        w.playSound(c, Sound.BLOCK_SMOKER_SMOKE, 1f, 1f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("♨ Cozinha ♨", COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito para cozinhar", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.CULINARIA, 25);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 2 == 0) centro.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, centro.clone().add(0, 0.3, 0), 1, 0.1, 0.05, 0.1, 0.01);
        if (ciclo % 10 == 0) centro.getWorld().playSound(centro, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.3f, 1f);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.CAULDRON));
        centro.getWorld().playSound(centro, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        if (agachado && b.getState() instanceof Smoker defumador) {
            // A comida dentro do defumador é do dono do território: vale a regra de baús.
            if (plugin.territorios().permite(p, b.getLocation(), Flag.BAUS)) {
                p.openInventory(defumador.getInventory()); // o defumador continua funcionando normal
                return;
            }
            p.sendActionBar(Component.text("✖ O defumador é do dono do território.", NamedTextColor.RED));
        }
        abrirReceitas(p, b.getLocation());
    }

    // =====================================================================
    //  Livro de receitas / cozinhar
    // =====================================================================

    public void abrirReceitas(Player p, Location cozinha) {
        Tela t = new Tela();
        t.cozinha = cozinha;
        t.inventario = Bukkit.createInventory(t, 54, Component.text(cozinha == null ? "♨ Livro de Receitas" : "♨ Cozinha"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private int nivel(Player p) {
        return plugin.stats().getNivel(p, Skill.CULINARIA);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        int nivel = nivel(p);
        int max = plugin.settings().nivelMaximo;
        inv.setItem(S_INFO, item(Material.SMOKER, Component.text("♨ Culinária Nv " + nivel, COR, TextDecoration.BOLD), List.of(
                Component.text("Pratos dão buffs ao serem comidos.", NamedTextColor.GRAY),
                Component.text("Ingredientes de qualidade (★★+)", NamedTextColor.GRAY),
                Component.text("deixam o prato melhor.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Seus buffs duram +" + StatsManager.fmt(nivel * plugin.settings().culDuracaoExtra * 100) + "%.", NamedTextColor.GREEN),
                t.cozinha == null ? Component.text("Para cozinhar, use uma Cozinha (veja o /guia).", NamedTextColor.DARK_GRAY)
                        : Component.text("Clique num prato para cozinhar.", NamedTextColor.YELLOW)), false));

        Prato[] pratos = Prato.values();
        for (int i = 0; i < pratos.length && i < SLOTS.length; i++) {
            Prato pr = pratos[i];
            boolean liberado = nivel >= pr.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((liberado ? "✔" : "✖") + " Culinária nível " + pr.nivelNecessario(max),
                    liberado ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Ingredientes:", NamedTextColor.GRAY));
            boolean temTudo = true;
            for (Prato.Ingrediente ing : pr.ingredientes()) {
                int tem = contar(p.getInventory(), ing);
                boolean ok = tem >= ing.qtd();
                temTudo &= ok;
                Component nome = ing.variedade() != null ? Component.text(ing.variedade().nome(), ing.variedade().cor())
                        : ing.peixe() != null ? Component.text(ing.peixe().nome(), ing.peixe().cor())
                        : Component.translatable(ing.material().translationKey());
                lore.add(Component.text((ok ? " ✔ " : " ✖ ") + ing.qtd() + "x ", ok ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(nome.color(ok ? NamedTextColor.GREEN : NamedTextColor.RED)));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Efeitos (qualidade normal):", NamedTextColor.GRAY));
            lore.addAll(descreverEfeitos(pr, 1));
            lore.add(Component.empty());
            if (t.cozinha == null) lore.add(Component.text("Cozinhe numa Cozinha.", NamedTextColor.DARK_GRAY));
            else if (liberado && temTudo) lore.add(Component.text("» Clique para cozinhar", NamedTextColor.YELLOW));
            else if (!liberado) lore.add(Component.text("Suba a Culinária para liberar.", NamedTextColor.DARK_GRAY));
            else lore.add(Component.text("Faltam ingredientes.", NamedTextColor.DARK_GRAY));
            inv.setItem(SLOTS[i], item(liberado ? pr.base() : Material.GRAY_DYE,
                    Component.text(pr.nome(), liberado ? COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore,
                    liberado && temTudo && t.cozinha != null));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static List<Component> descreverEfeitos(Prato pr, double fator) {
        List<Component> l = new ArrayList<>();
        for (Prato.Efeito ef : pr.efeitos()) {
            int seg = (int) Math.round(ef.segundos() * fator);
            String duracao = ef.segundos() <= 1 ? "" : " (" + seg / 60 + ":" + String.format("%02d", seg % 60) + ")";
            l.add(Component.text(" ✧ ", COR).append(Component.translatable(ef.tipo().translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(ef.nivel() > 0 ? " " + romano(ef.nivel() + 1) : "", NamedTextColor.WHITE))
                    .append(Component.text(duracao, NamedTextColor.GRAY)));
        }
        if (pr.especial() == Prato.Especial.MANA) l.add(Component.text(" ✧ Recupera 60 de mana", NamedTextColor.LIGHT_PURPLE));
        if (pr.especial() == Prato.Especial.BANQUETE) l.add(Component.text(" ✧ Vale para todos a até 10 blocos!", NamedTextColor.GOLD));
        return l;
    }

    private static String romano(int n) {
        return switch (n) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> "I"; };
    }

    // ---------- ingredientes no inventário ----------

    private static boolean combina(ItemStack s, Prato.Ingrediente ing) {
        if (s == null || s.isEmpty()) return false;
        if (ing.variedade() != null) return Variedade.de(s) == ing.variedade() && !Variedade.ehSemente(s);
        if (ing.peixe() != null) return PeixeRaro.de(s) == ing.peixe();
        return s.getType() == ing.material() && Ingrediente.simples(s);
    }

    private static int contar(PlayerInventory inv, Prato.Ingrediente ing) {
        int n = 0;
        for (ItemStack s : inv.getStorageContents()) if (combina(s, ing)) n += s.getAmount();
        return n;
    }

    /** Tira os ingredientes (usando primeiro os de melhor qualidade) e diz a qualidade de cada um. */
    private static List<Qualidade> tirar(PlayerInventory inv, Prato.Ingrediente ing) {
        ItemStack[] c = inv.getStorageContents();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < c.length; i++) if (combina(c[i], ing)) indices.add(i);
        indices.sort(Comparator.comparingInt((Integer i) -> Qualidade.de(c[i]).ordinal()).reversed());
        List<Qualidade> qualidades = new ArrayList<>();
        int falta = ing.qtd();
        for (int i : indices) {
            if (falta <= 0) break;
            int tira = Math.min(falta, c[i].getAmount());
            Qualidade q = Qualidade.de(c[i]);
            for (int k = 0; k < tira; k++) qualidades.add(q);
            c[i].setAmount(c[i].getAmount() - tira);
            if (c[i].getAmount() <= 0) c[i] = null;
            falta -= tira;
        }
        inv.setStorageContents(c);
        return qualidades;
    }

    private void cozinhar(Player p, Tela t, Prato pr) {
        if (t.cozinha == null) {
            erro(p, "Para cozinhar, use uma Cozinha.");
            return;
        }
        if (!eh(t.cozinha.getBlock())) {
            erro(p, "A Cozinha não existe mais.");
            p.closeInventory();
            return;
        }
        if (!p.getWorld().equals(t.cozinha.getWorld())
                || p.getLocation().distanceSquared(t.cozinha.clone().add(0.5, 0.5, 0.5)) > 8 * 8) {
            erro(p, "Você está longe da Cozinha.");
            p.closeInventory();
            return;
        }
        int max = plugin.settings().nivelMaximo;
        if (nivel(p) < pr.nivelNecessario(max)) {
            erro(p, "Precisa de Culinária nível " + pr.nivelNecessario(max) + ".");
            return;
        }
        PlayerInventory inv = p.getInventory();
        for (Prato.Ingrediente ing : pr.ingredientes()) {
            if (contar(inv, ing) < ing.qtd()) {
                erro(p, "Faltam ingredientes.");
                return;
            }
        }
        // Ingredientes de qualidade melhoram o prato.
        int comQualidade = 0, otimos = 0, perfeitos = 0;
        for (Prato.Ingrediente ing : pr.ingredientes()) {
            List<Qualidade> qs = tirar(inv, ing);
            boolean conta = ing.variedade() != null || ing.peixe() != null || COM_QUALIDADE.contains(ing.material());
            if (!conta) continue;
            for (Qualidade q : qs) {
                comQualidade++;
                if (q.ordinal() >= Qualidade.OTIMA.ordinal()) otimos++;
                if (q == Qualidade.PERFEITA) perfeitos++;
            }
        }
        if (pr.ingredientes().stream().anyMatch(i -> i.material() == Material.HONEY_BOTTLE)) {
            inv.addItem(new ItemStack(Material.GLASS_BOTTLE));
        }
        int nivel = nivel(p);
        Qualidade q = Qualidade.sortear(nivel / (double) max);
        if (comQualidade > 0 && perfeitos == comQualidade) q = Qualidade.PERFEITA;
        else if (comQualidade > 0 && otimos * 2 >= comQualidade) q = q.acima();

        double fator = q.fator() * (1 + nivel * plugin.settings().culDuracaoExtra);
        ItemStack prato = criar(pr, q, fator, p.getName());
        inv.addItem(prato).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));

        Location c = t.cozinha.clone().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c, 10, 0.2, 0.2, 0.2, 0.03);
        c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c, 10, 0.3, 0.3, 0.3);
        p.playSound(p.getLocation(), Sound.BLOCK_SMOKER_SMOKE, 1f, 1f);
        p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 1.4f);
        p.sendMessage(Component.text("♨ Você cozinhou ", COR).append(prato.effectiveName())
                .append(Component.text(q == Qualidade.NORMAL ? "" : " (Qualidade " + q.nome() + ")", q.cor())));
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.CULINARIA, pr.xp());
        if (pr == Prato.BANQUETE_LENDARIO) plugin.titulos().registrar(p, "banquete", 1);
        desenhar(p, t);
    }

    /** O item do prato. A duração dos buffs já fica gravada nele. */
    public ItemStack criar(Prato pr, Qualidade q, double fator, String cozinheiro) {
        ItemStack i = new ItemStack(pr.base());
        i.editMeta(m -> {
            m.itemName(Component.text(pr.nome() + (q == Qualidade.NORMAL ? "" : " " + q.estrelas()),
                    q == Qualidade.NORMAL ? COR : q.cor()));
            List<Component> lore = new ArrayList<>();
            if (q != Qualidade.NORMAL) lore.add(Component.text(q.estrelas() + " Qualidade " + q.nome(), q.cor()));
            lore.add(Component.text("Ao comer:", NamedTextColor.GRAY));
            lore.addAll(descreverEfeitos(pr, fator));
            lore.add(Component.empty());
            lore.add(Component.text("♨ Feito por " + cozinheiro, NamedTextColor.DARK_GRAY));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(CHAVE_PRATO, PersistentDataType.STRING, pr.name());
            pdc.set(CHAVE_FATOR, PersistentDataType.DOUBLE, Math.round(fator * 100) / 100.0);
            pdc.set(Qualidade.CHAVE, PersistentDataType.STRING, q.name());
            if (q == Qualidade.PERFEITA) m.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    // =====================================================================
    //  Comer
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        PersistentDataContainer pdc = e.getItem().getItemMeta().getPersistentDataContainer();
        Prato pr = Prato.porId(pdc.getOrDefault(CHAVE_PRATO, PersistentDataType.STRING, ""));
        if (pr == null) return;
        double fator = pdc.getOrDefault(CHAVE_FATOR, PersistentDataType.DOUBLE, 1.0);
        Player p = e.getPlayer();
        // Um tick depois, para os efeitos não serem sobrescritos pela comida normal.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            List<Player> alvos = new ArrayList<>(List.of(p));
            if (pr.especial() == Prato.Especial.BANQUETE) {
                for (Player outro : p.getWorld().getPlayers()) {
                    if (outro != p && outro.getLocation().distanceSquared(p.getLocation()) <= 100) alvos.add(outro);
                }
            }
            for (Player alvo : alvos) {
                for (Prato.Efeito ef : pr.efeitos()) {
                    alvo.addPotionEffect(new PotionEffect(ef.tipo(), (int) Math.round(ef.segundos() * 20 * fator), ef.nivel()));
                }
                alvo.setSaturation(Math.min(alvo.getFoodLevel(), alvo.getSaturation() + pr.saturacao()));
                alvo.getWorld().spawnParticle(Particle.HEART, alvo.getLocation().add(0, 2, 0), 3, 0.3, 0.2, 0.3);
                if (alvo != p) alvo.sendMessage(Component.text("♨ " + p.getName() + " serviu um " + pr.nome() + " para você!", COR));
            }
            if (pr.especial() == Prato.Especial.MANA) plugin.arcano().darMana(p, 60);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.1f);
        });
    }

    // =====================================================================
    //  Cliques no menu
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
        Prato[] pratos = Prato.values();
        for (int i = 0; i < pratos.length && i < SLOTS.length; i++) {
            if (SLOTS[i] == e.getSlot()) {
                cozinhar(p, t, pratos[i]);
                return;
            }
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
