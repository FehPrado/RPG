package br.rpgatributos.oficio;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Ateliê do Curtidor: 1 tesoura, 4 couros e 2 linhas jogados num tear. Três abas:
 * <b>Curtir</b> (pele crua + farinha de osso = couro curtido), <b>Tecer</b> (linha, lã ou alga
 * = tecido) e <b>Costurar</b> (as 24 peças dos 6 conjuntos). Agachado + clique: o tear normal.
 */
public final class Ateliers extends Estacao {

    enum Aba { CURTIR, TECER, COSTURAR }

    /** Um ingrediente: o que serve, quanto e como aparece no menu. */
    record Ing(String nome, int qtd, Predicate<ItemStack> serve) { }

    record Receita(String nome, Material icone, Supplier<ItemStack> resultado, List<Ing> ings, double xp) { }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Aba aba;
        final List<Receita> receitas = new ArrayList<>();
        final List<Integer> slots = new ArrayList<>();

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private static final int S_CURTIR = 2, S_TECER = 4, S_COSTURAR = 6, S_INFO = 49;
    private static final int[] SLOTS_SIMPLES = {20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34};

    private final Oficios oficios;

    public Ateliers(RPGAtributos plugin, Oficios oficios) {
        super(plugin, "ateliers", "atelier_display");
        this.oficios = oficios;
    }

    // =====================================================================
    //  Ritual
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.LOOM;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.SHEARS || m == Material.LEATHER || m == Material.STRING;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block b = item.getLocation().getBlock();
        if (b.getType() == Material.LOOM) return b;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.LOOM ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.SHEARS) < 1 || contar(itens, Material.LEATHER) < 4 || contar(itens, Material.STRING) < 2) return false;
        tirar(itens, Material.SHEARS, 1);
        tirar(itens, Material.LEATHER, 4);
        tirar(itens, Material.STRING, 2);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.SHEARS);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.45;
    }

    @Override
    protected Component nome() {
        return Component.text("✂ Ateliê do Curtidor", Oficios.COR)
                .append(Component.newline())
                .append(Component.text("curtir, tecer e costurar", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.1, 0.5);
        c.getWorld().spawnParticle(Particle.WAX_OFF, c, 25, 0.4, 0.3, 0.4, 0.05);
        c.getWorld().playSound(c, Sound.UI_LOOM_TAKE_RESULT, 1f, 0.9f);
        if (quem != null) quem.sendMessage(Component.text("✂ Ateliê do Curtidor pronto: curta as peles, teça os panos e costure "
                + "conjuntos de armadura leve. Agachado + clique: o tear normal.", Oficios.COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 12 == 0) centro.getWorld().spawnParticle(Particle.WAX_OFF, centro, 2, 0.3, 0.2, 0.3, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.SHEARS));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void abrir(Player p, Block b, boolean agachado) {
        if (agachado) {
            p.openLoom(b.getLocation(), true);
            return;
        }
        abrirMenu(p, Aba.COSTURAR);
    }

    // =====================================================================
    //  Receitas
    // =====================================================================

    private static Ing de(MaterialOficio m, int qtd) {
        return new Ing(m.nome(), qtd, s -> MaterialOficio.de(s) == m);
    }

    private static boolean comum(ItemStack s) {
        return s.getPersistentDataContainer().getKeys().isEmpty();
    }

    private static Ing de(Material m, String nome, int qtd) {
        return new Ing(nome, qtd, s -> s.getType() == m && comum(s));
    }

    private List<Receita> receitas(Aba aba) {
        List<Receita> l = new ArrayList<>();
        switch (aba) {
            case CURTIR -> {
                curtir(l, MaterialOficio.PELE_DE_LOBO, MaterialOficio.COURO_LOBO);
                curtir(l, MaterialOficio.PELE_DE_URSO, MaterialOficio.COURO_URSO);
                curtir(l, MaterialOficio.PELE_DE_RAPOSA, MaterialOficio.COURO_RAPOSA);
                curtir(l, MaterialOficio.ESCAMA_DE_GUARDIAO, MaterialOficio.COURO_ESCAMAS);
                curtir(l, MaterialOficio.COURO_DE_HOGLIN, MaterialOficio.COURO_BRASA);
                l.add(new Receita(MaterialOficio.COURO_NOTURNO.nome(), Material.PHANTOM_MEMBRANE, () -> MaterialOficio.COURO_NOTURNO.criar(1),
                        List.of(de(Material.PHANTOM_MEMBRANE, "Membrana de phantom", 2), de(Material.BONE_MEAL, "Farinha de osso", 1)), 3));
            }
            case TECER -> {
                l.add(new Receita(MaterialOficio.TECIDO.nome(), Material.STRING, () -> MaterialOficio.TECIDO.criar(1),
                        List.of(de(Material.STRING, "Linha", 4)), 2));
                l.add(new Receita(MaterialOficio.FELTRO.nome(), Material.WHITE_WOOL, () -> MaterialOficio.FELTRO.criar(1),
                        List.of(new Ing("Lã (qualquer cor)", 3, s -> Tag.WOOL.isTagged(s.getType()) && comum(s))), 2));
                l.add(new Receita(MaterialOficio.TECIDO_DE_ALGA.nome(), Material.DRIED_KELP, () -> MaterialOficio.TECIDO_DE_ALGA.criar(1),
                        List.of(de(Material.DRIED_KELP, "Alga seca", 4)), 2));
            }
            case COSTURAR -> {
                for (Conjunto.Peca pc : Conjunto.Peca.values()) {
                    for (Conjunto c : Conjunto.values()) {
                        l.add(new Receita(c.nomePeca(pc), pc.material(), () -> oficios.criarPeca(c, pc),
                                List.of(de(c.couro(), pc.couros()), de(c.tecido(), pc.tecidos())), 6 + pc.couros() * 2));
                    }
                }
            }
        }
        return l;
    }

    private static void curtir(List<Receita> l, MaterialOficio pele, MaterialOficio couro) {
        l.add(new Receita(couro.nome(), Material.LEATHER, () -> couro.criar(1),
                List.of(de(pele, 1), de(Material.BONE_MEAL, "Farinha de osso", 1)), 3));
    }

    private static int tem(Player p, Ing ing) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) if (s != null && !s.isEmpty() && ing.serve().test(s)) n += s.getAmount();
        return n;
    }

    private static void gastar(Player p, Ing ing) {
        int falta = ing.qtd();
        ItemStack[] cont = p.getInventory().getStorageContents();
        for (int i = 0; i < cont.length && falta > 0; i++) {
            ItemStack s = cont[i];
            if (s == null || s.isEmpty() || !ing.serve().test(s)) continue;
            int tira = Math.min(falta, s.getAmount());
            s.setAmount(s.getAmount() - tira);
            falta -= tira;
        }
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    private void abrirMenu(Player p, Aba aba) {
        Tela t = new Tela();
        t.aba = aba;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✂ Ateliê do Curtidor"));
        Inventory inv = t.inventario;
        inv.setItem(S_CURTIR, aba(Material.LEATHER, "Curtir", "Pele crua + farinha de osso = couro curtido", aba == Aba.CURTIR));
        inv.setItem(S_TECER, aba(Material.STRING, "Tecer", "Linha, lã ou alga seca viram tecido", aba == Aba.TECER));
        inv.setItem(S_COSTURAR, aba(Material.LEATHER_CHESTPLATE, "Costurar", "As peças dos 6 conjuntos", aba == Aba.COSTURAR));
        List<Receita> rs = receitas(aba);
        for (int i = 0; i < rs.size(); i++) {
            int slot;
            if (aba == Aba.COSTURAR) {
                int peca = i / Conjunto.values().length, coluna = i % Conjunto.values().length;
                slot = (peca + 1) * 9 + 1 + coluna + (coluna >= 3 ? 1 : 0);
            } else {
                if (i >= SLOTS_SIMPLES.length) break;
                slot = SLOTS_SIMPLES[i];
            }
            t.receitas.add(rs.get(i));
            t.slots.add(slot);
            inv.setItem(slot, icone(p, rs.get(i), aba == Aba.COSTURAR));
        }
        inv.setItem(S_INFO, info());
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
        p.openInventory(inv);
    }

    private static ItemStack aba(Material m, String nome, String texto, boolean atual) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(Component.text(nome, atual ? NamedTextColor.GOLD : NamedTextColor.YELLOW, TextDecoration.BOLD));
            meta.lore(List.of(Component.text(texto, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text(atual ? "● Aberta" : "» Clique para abrir", atual ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)));
            meta.addItemFlags(ItemFlag.values());
            if (atual) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    private ItemStack icone(Player p, Receita r, boolean peca) {
        ItemStack i = r.resultado().get();
        boolean pode = true;
        List<Component> lore = new ArrayList<>();
        if (peca && i.lore() != null) lore.addAll(i.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Precisa:", NamedTextColor.WHITE));
        for (Ing ing : r.ings()) {
            int n = tem(p, ing);
            boolean ok = n >= ing.qtd();
            pode &= ok;
            lore.add(Component.text((ok ? "✔ " : "✖ ") + ing.qtd() + "x " + ing.nome() + " (tem " + n + ")", ok ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        lore.add(Component.empty());
        lore.add(pode ? Component.text("» Clique para fazer" + (peca ? "" : " (shift: até 8)"), NamedTextColor.YELLOW)
                : Component.text("Faltam materiais.", NamedTextColor.DARK_GRAY));
        boolean podeFinal = pode;
        i.editMeta(m -> {
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (podeFinal && !peca) m.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    private static ItemStack info() {
        ItemStack i = new ItemStack(Material.BOOK);
        i.editMeta(m -> {
            m.itemName(Component.text("Como funciona", NamedTextColor.YELLOW, TextDecoration.BOLD));
            m.lore(List.of(
                    Component.text("Peles: lobos, ursos polares, raposas,", NamedTextColor.GRAY),
                    Component.text("guardiões e hoglins (elites: sempre).", NamedTextColor.GRAY),
                    Component.text("Membrana de phantom vira Couro Noturno.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Cada conjunto dá um bônus com 2 e", NamedTextColor.GRAY),
                    Component.text("outro com 4 peças vestidas.", NamedTextColor.GRAY),
                    Component.text("Reforje as peças na Forja com um", NamedTextColor.GRAY),
                    Component.text("Fragmento de Forja: o conjunto fica.", NamedTextColor.GRAY)).stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        });
        return i;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= 54) return;
        int s = e.getRawSlot();
        if (s == S_CURTIR) { abrirMenu(p, Aba.CURTIR); return; }
        if (s == S_TECER) { abrirMenu(p, Aba.TECER); return; }
        if (s == S_COSTURAR) { abrirMenu(p, Aba.COSTURAR); return; }
        int idx = t.slots.indexOf(s);
        if (idx < 0) return;
        Receita r = t.receitas.get(idx);
        int vezes = e.isShiftClick() && t.aba != Aba.COSTURAR ? 8 : 1, feitas = 0;
        for (int v = 0; v < vezes; v++) {
            boolean pode = true;
            for (Ing ing : r.ings()) if (tem(p, ing) < ing.qtd()) pode = false;
            if (!pode) break;
            for (Ing ing : r.ings()) gastar(p, ing);
            for (ItemStack sobra : p.getInventory().addItem(r.resultado().get()).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            feitas++;
        }
        if (feitas == 0) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            p.sendActionBar(Component.text("✖ Faltam materiais para " + r.nome() + ".", NamedTextColor.RED));
            return;
        }
        p.playSound(p.getLocation(), t.aba == Aba.COSTURAR ? Sound.ITEM_ARMOR_EQUIP_LEATHER : Sound.UI_LOOM_TAKE_RESULT, 1f, 1.1f);
        p.sendActionBar(Component.text("✂ " + (feitas > 1 ? feitas + "x " : "") + r.nome(), Oficios.COR));
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.FERRARIA, r.xp() * feitas);
        if (t.aba == Aba.COSTURAR) plugin.titulos().registrar(p, "pecas_costuradas", 1);
        abrirMenu(p, t.aba);
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }
}
