package br.rpgatributos.vida;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Reagente;
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
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Barril de Envelhecimento: 2 lingotes de ferro e 1 favo de mel jogados num barril vazio.
 * Clique para escolher uma bebida (os ingredientes saem do inventário). Ela envelhece em dias
 * reais; quando quiser, engarrafe com 3 garrafas de vidro. Quanto mais velha, melhor.
 */
public final class Barris extends Estacao {

    public static final TextColor COR = TextColor.color(0xA1887F);
    private static final String DADO = "barril";
    private static final int GARRAFAS = 3;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location barril;
        final Map<Integer, Bebida> bebidas = new HashMap<>();
        boolean engarrafar;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Barris(RPGAtributos plugin) {
        super(plugin, "barris", "barril_display");
    }

    private int[] limites() {
        var c = plugin.settings();
        return new int[]{c.vidaBarrilDias[0], c.vidaBarrilDias[1], c.vidaBarrilDias[2]};
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.BARREL;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.IRON_INGOT || m == Material.HONEYCOMB;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.BARREL ? abaixo : null;
    }

    @Override
    protected String impedimento(Block b, UUID quem) {
        if (b.getState() instanceof Barrel barril && !barril.getInventory().isEmpty()) return "Esvazie o barril antes.";
        return super.impedimento(b, quem);
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.IRON_INGOT) < 2 || contar(itens, Material.HONEYCOMB) < 1) return false;
        tirar(itens, Material.IRON_INGOT, 2);
        tirar(itens, Material.HONEYCOMB, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return Bebida.HIDROMEL.criar(0);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        String[] d = dados(bloco);
        return d == null ? new ItemStack(Material.GLASS_BOTTLE) : Bebida.valueOf(d[0]).criar(0);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("🛢 Barril de Envelhecimento", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        String[] d = dados(bloco);
        if (d == null) return nome().append(Component.newline()).append(Component.text("vazio: clique para começar", NamedTextColor.GRAY));
        Bebida b = Bebida.valueOf(d[0]);
        double dias = dias(d);
        return nome().append(Component.newline())
                .append(Component.text(b.nome() + " · " + Bebida.GRAUS[Bebida.grau(dias, limites())] + " (" + String.format("%.1f", dias) + " dias)", b.cor()));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().playSound(c, Sound.BLOCK_BARREL_CLOSE, 1f, 0.8f);
        c.getWorld().spawnParticle(Particle.CLOUD, c, 10, 0.3, 0.2, 0.3, 0.01);
        if (quem != null) quem.sendMessage(Component.text("🛢 Barril de Envelhecimento pronto. Clique para começar uma bebida.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 12 == 0 && dados(centro.getBlock().getLocation().subtract(0, 1, 0)) != null) {
            centro.getWorld().spawnParticle(Particle.BUBBLE_POP, centro.clone().subtract(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0);
        }
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.IRON_INGOT, 2));
    }

    @Override
    protected void aoRemover(Location bloco) {
        DadosBloco.apagar(DADO, bloco);
    }

    /** [bebida, início] ou null. */
    private static String[] dados(Location l) {
        String s = DadosBloco.ler(DADO, l);
        if (s == null) return null;
        String[] d = s.split("\\|");
        if (d.length != 2) return null;
        try {
            Bebida.valueOf(d[0]);
            Long.parseLong(d[1]);
            return d;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static double dias(String[] d) {
        return (System.currentTimeMillis() - Long.parseLong(d[1])) / 86_400_000.0;
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.barril = b.getLocation();
        String[] d = dados(t.barril);
        if (d != null) {
            Bebida beb = Bebida.valueOf(d[0]);
            double dias = dias(d);
            int g = Bebida.grau(dias, limites());
            t.engarrafar = true;
            t.inventario = Bukkit.createInventory(t, 27, Component.text("Barril · " + beb.nome(), COR));
            ItemStack info = beb.criar(g);
            info.editMeta(m -> {
                List<Component> lore = new ArrayList<>(m.lore() == null ? List.of() : m.lore());
                lore.add(Component.empty());
                lore.add(Component.text("No barril há " + String.format("%.1f", dias) + " dia(s).", NamedTextColor.YELLOW));
                int[] lim = limites();
                if (g < 3) lore.add(Component.text("Próximo grau (" + Bebida.GRAUS[g + 1] + ") com " + lim[g] + " dia(s).", NamedTextColor.GRAY));
                lore.add(Component.text("» Clique para engarrafar (" + GARRAFAS + " garrafas de vidro)", NamedTextColor.GREEN));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            });
            t.inventario.setItem(13, info);
            p.openInventory(t.inventario);
            return;
        }
        t.inventario = Bukkit.createInventory(t, 27, Component.text("Barril · escolha a bebida", COR));
        int slot = 10;
        for (Bebida beb : Bebida.values()) {
            ItemStack i = beb.criar(0);
            i.editMeta(m -> {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(beb.descricao(), NamedTextColor.GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("Ingredientes:", NamedTextColor.YELLOW));
                beb.ingredientes().forEach((mat, q) -> lore.add(Component.text(" " + q + "x ", NamedTextColor.WHITE)
                        .append(Component.translatable(mat.translationKey(), NamedTextColor.WHITE))));
                if (beb.poArcano() > 0) lore.add(Component.text(" " + beb.poArcano() + "x Pó Arcano", NamedTextColor.WHITE));
                if (beb.pedeErvas()) lore.add(Component.text(" 3x qualquer erva do Canteiro", NamedTextColor.WHITE));
                if (beb.pedeFrutas()) lore.add(Component.text(" 6x qualquer fruta do pomar", NamedTextColor.WHITE));
                lore.add(Component.empty());
                lore.add(Component.text("» Clique para pôr no barril", NamedTextColor.GREEN));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            });
            t.inventario.setItem(slot, i);
            t.bebidas.put(slot, beb);
            slot++;
        }
        p.openInventory(t.inventario);
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        if (t.engarrafar) {
            if (e.getRawSlot() == 13) engarrafar(p, t.barril);
            return;
        }
        Bebida b = t.bebidas.get(e.getRawSlot());
        if (b != null) comecar(p, t.barril, b);
    }

    private void comecar(Player p, Location barril, Bebida b) {
        if (dados(barril) != null) return;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            for (Map.Entry<Material, Integer> en : b.ingredientes().entrySet()) {
                if (contarComuns(p, en.getKey()) < en.getValue()) {
                    p.sendMessage(Component.text("Faltam ingredientes para o " + b.nome() + ".", NamedTextColor.RED));
                    return;
                }
            }
            if (b.poArcano() > 0 && contarReagente(p) < b.poArcano()) {
                p.sendMessage(Component.text("Falta Pó Arcano (" + b.poArcano() + ").", NamedTextColor.RED));
                return;
            }
            if (b.pedeErvas() && contarErvas(p) < 3) {
                p.sendMessage(Component.text("Faltam 3 ervas do Canteiro.", NamedTextColor.RED));
                return;
            }
            if (b.pedeFrutas() && contarFrutas(p) < 6) {
                p.sendMessage(Component.text("Faltam 6 frutas (do pomar, morango, mirtilo, uva, amora, groselha ou zimbro).", NamedTextColor.RED));
                return;
            }
            b.ingredientes().forEach((mat, q) -> tirarComuns(p, mat, q));
            if (b.poArcano() > 0) tirarEspeciais(p, b.poArcano(), true);
            if (b.pedeErvas()) tirarEspeciais(p, 3, false);
            if (b.pedeFrutas()) tirarFrutas(p, 6);
        }
        DadosBloco.gravar(DADO, barril, b.name() + "|" + System.currentTimeMillis());
        atualizarDisplays(barril);
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.BLOCK_BARREL_CLOSE, 1f, 1f);
        p.sendMessage(Component.text("🛢 " + b.nome() + " no barril. Envelhece em dias reais: Envelhecida com " + limites()[0]
                + " dia(s), Reserva com " + limites()[1] + " e Lendária com " + limites()[2] + ".", COR));
    }

    private void engarrafar(Player p, Location barril) {
        String[] d = dados(barril);
        if (d == null) return;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && !p.getInventory().containsAtLeast(new ItemStack(Material.GLASS_BOTTLE), GARRAFAS)) {
            p.sendMessage(Component.text("Precisa de " + GARRAFAS + " garrafas de vidro.", NamedTextColor.RED));
            return;
        }
        if (!criativo) p.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, GARRAFAS));
        Bebida b = Bebida.valueOf(d[0]);
        int g = Bebida.grau(dias(d), limites());
        for (int i = 0; i < GARRAFAS; i++) {
            p.getInventory().addItem(b.criar(g)).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        DadosBloco.apagar(DADO, barril);
        atualizarDisplays(barril);
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 0.8f);
        plugin.stats().darXp(p, Skill.CULINARIA, 10 + 10 * g);
        plugin.titulos().registrar(p, "bebidas", GARRAFAS);
        p.sendMessage(Component.text("🛢 " + GARRAFAS + "x " + b.nome() + " " + Bebida.GRAUS[g] + " engarrafado(a)!", g == 3 ? NamedTextColor.GOLD : COR));
    }

    private static void tirarComuns(Player p, Material m, int qtd) {
        ItemStack[] c = p.getInventory().getContents();
        for (int i = 0; i < c.length && qtd > 0; i++) {
            ItemStack s = c[i];
            if (s == null || s.getType() != m || !br.rpgatributos.alquimia.Ingrediente.simples(s)) continue;
            int t = Math.min(qtd, s.getAmount());
            s.setAmount(s.getAmount() - t);
            qtd -= t;
        }
    }

    /** Itens desse material (variedades raras não contam: não se gastam numa bebida comum). */
    private static int contarComuns(Player p, Material m) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && s.getType() == m && br.rpgatributos.alquimia.Ingrediente.simples(s)) n += s.getAmount();
        }
        return n;
    }

    private static int contarReagente(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) if (Reagente.de(s) == Reagente.PO_ARCANO) n += s.getAmount();
        return n;
    }

    /** Fruta que serve no Licor de Frutas: as do pomar, as plantações de fruta e as bagas colhidas pelo mundo. */
    private static boolean frutaDeLicor(ItemStack s) {
        if (Fruta.de(s) != null) return true;
        Cultivo c = Cultivo.daColheita(s);
        if (c == Cultivo.MORANGO || c == Cultivo.MIRTILO || c == Cultivo.UVA || c == Cultivo.OXICOCO || c == Cultivo.MELAO) return true;
        Coleta k = Coleta.de(s);
        return k == Coleta.AMORA_SILVESTRE || k == Coleta.GROSELHA || k == Coleta.FRUTO_DE_ZIMBRO;
    }

    private static int contarFrutas(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) if (frutaDeLicor(s)) n += s.getAmount();
        return n;
    }

    private static void tirarFrutas(Player p, int qtd) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (qtd <= 0) return;
            if (!frutaDeLicor(s)) continue;
            int t = Math.min(qtd, s.getAmount());
            s.setAmount(s.getAmount() - t);
            qtd -= t;
        }
    }

    private static int contarErvas(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) if (Erva.de(s) != null) n += s.getAmount();
        return n;
    }

    /** Tira Pó Arcano (pó = true) ou ervas. */
    private static void tirarEspeciais(Player p, int qtd, boolean po) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (qtd <= 0) return;
            if (s == null || (po ? Reagente.de(s) != Reagente.PO_ARCANO : Erva.de(s) == null)) continue;
            int t = Math.min(qtd, s.getAmount());
            s.setAmount(s.getAmount() - t);
            qtd -= t;
        }
    }

    // =====================================================================
    //  Beber
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoBeber(PlayerItemConsumeEvent e) {
        Bebida b = Bebida.de(e.getItem());
        if (b == null) return;
        int g = Bebida.grauDo(e.getItem());
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (b == Bebida.LICOR_ARCANO) plugin.arcano().darMana(p, 40 + 20 * g);
            if (b == Bebida.CERVEJA && g < 3) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100 - 25 * g, 0));
            }
            if (g == 3) plugin.segredos().conceder(p, Segredos.Segredo.BEBIDA_LENDARIA);
        });
    }
}
