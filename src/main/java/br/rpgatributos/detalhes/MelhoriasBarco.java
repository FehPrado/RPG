package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Boat;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Melhorias do Barco Grande (3.0, "Mundo e Mar"). Cada um dos 4 bancos é um encaixe: assento,
 * mastro (com a vela), baú de convés ou lugar de um bicho. Por cima disso:
 * <ul>
 *   <li>Baú de convés (um baú comum clicado no barco, até 2): 27 espaços guardados no barco; ao
 *       recolher, o conteúdo vai dentro do item, como numa shulker;</li>
 *   <li>Casco reforçado: o casco aguenta mais e leva metade do dano (Kraken, piratas, tempestade);</li>
 *   <li>Rede de pesca: navegando, pesca sozinha e põe no baú (bem mais no cardume);</li>
 *   <li>Âncora: baixada, o barco fica parado no lugar;</li>
 *   <li>Estandarte na vela: o mastro leva o desenho de um estandarte seu.</li>
 * </ul>
 * O casco tem vida: chega a zero, o barco afunda (tudo boia na água). Tábuas consertam.
 * Agachado + clique com a mão vazia abre o menu do barco.
 */
public final class MelhoriasBarco implements Listener {

    static final TextColor COR = TextColor.color(0xC9A26B);
    static final NamespacedKey K_BAUS = new NamespacedKey("rpgatributos", "barco_baus");
    static final NamespacedKey K_REFORCO = new NamespacedKey("rpgatributos", "barco_casco_reforcado");
    static final NamespacedKey K_REDE = new NamespacedKey("rpgatributos", "barco_rede");
    static final NamespacedKey K_ANCORA = new NamespacedKey("rpgatributos", "barco_ancora");
    static final NamespacedKey K_ANCORADO = new NamespacedKey("rpgatributos", "barco_ancorado");
    public static final NamespacedKey K_ESTANDARTE = new NamespacedKey("rpgatributos", "vela_estandarte");
    static final NamespacedKey K_VIDA = new NamespacedKey("rpgatributos", "barco_casco_vida");
    private static final NamespacedKey K_PECA = new NamespacedKey("rpgatributos", "barco_peca");
    private static final NamespacedKey[] K_BAU = {
            new NamespacedKey("rpgatributos", "barco_bau_0"), new NamespacedKey("rpgatributos", "barco_bau_1"),
            new NamespacedKey("rpgatributos", "barco_bau_2"), new NamespacedKey("rpgatributos", "barco_bau_3")};

    /** Ordem em que os baús ocupam os bancos: os de trás primeiro (o 1 é o do mastro). */
    static final int[] ORDEM_BAUS = {3, 2, 0};
    static final int MAX_BAUS = 2;
    static final int VIDA_CASCO = 40, VIDA_REFORCADO = 100;

    /** As peças que viram item (clicadas no barco). */
    public enum Peca {
        CASCO("casco", "Casco Reforçado", "iron_bars", "O casco aguenta mais e leva metade do dano."),
        REDE("rede", "Rede de Pesca", "cobweb", "Navegando, pesca sozinha e guarda no baú do barco."),
        ANCORA("ancora", "Âncora", "anvil", "Baixada, o barco fica parado no lugar.");

        final String id, nome, modelo, frase;

        Peca(String id, String nome, String modelo, String frase) {
            this.id = id;
            this.nome = nome;
            this.modelo = modelo;
            this.frase = frase;
        }

        NamespacedKey chave() {
            return switch (this) {
                case CASCO -> K_REFORCO;
                case REDE -> K_REDE;
                case ANCORA -> K_ANCORA;
            };
        }

        public ItemStack item() {
            ItemStack i = new ItemStack(Material.PAPER);
            i.editMeta(m -> {
                m.itemName(Component.text(nome, COR));
                m.lore(List.of(
                        Component.text(frase, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Clique no Barco Grande para instalar.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
                m.setMaxStackSize(1);
                m.setItemModel(NamespacedKey.minecraft(modelo));
                m.getPersistentDataContainer().set(K_PECA, PersistentDataType.STRING, id);
            });
            return i;
        }

        public static Peca porId(String s) {
            for (Peca p : values()) if (p.id.equalsIgnoreCase(s)) return p;
            return null;
        }
    }

    static Peca peca(ItemStack i) {
        if (i == null || i.getType() != Material.PAPER || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_PECA, PersistentDataType.STRING);
        return s == null ? null : Peca.porId(s);
    }

    private final RPGAtributos plugin;
    /** Baús abertos agora (barco + banco): todos que abrem o mesmo baú veem o mesmo inventário. */
    private final Map<String, BauConves> abertos = new HashMap<>();
    /** Onde cada barco ancorado tem de ficar. */
    final Map<UUID, Location> ancoras = new HashMap<>();
    /** Rede: onde o barco estava na última conta e quanto andou desde a última puxada. */
    private final Map<UUID, Location> ultimo = new HashMap<>();
    private final Map<UUID, Double> andado = new HashMap<>();

    public MelhoriasBarco(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public void registrarReceitas() {
        ShapedRecipe casco = new ShapedRecipe(new NamespacedKey(plugin, "barco_casco_reforcado"), Peca.CASCO.item());
        casco.shape("IPI", "PIP", "IPI");
        casco.setIngredient('I', Material.IRON_INGOT);
        casco.setIngredient('P', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        Bukkit.addRecipe(casco);
        ShapedRecipe rede = new ShapedRecipe(new NamespacedKey(plugin, "barco_rede"), Peca.REDE.item());
        rede.shape("SSS", "SVS", "SSS");
        rede.setIngredient('S', Material.STRING);
        rede.setIngredient('V', Material.FISHING_ROD);
        Bukkit.addRecipe(rede);
        ShapedRecipe ancora = new ShapedRecipe(new NamespacedKey(plugin, "barco_ancora"), Peca.ANCORA.item());
        ancora.shape(" C ", " I ", "IBI");
        ancora.setIngredient('C', Material.IRON_CHAIN);
        ancora.setIngredient('I', Material.IRON_INGOT);
        ancora.setIngredient('B', Material.IRON_BLOCK);
        Bukkit.addRecipe(ancora);
    }

    // =====================================================================
    //  O que o barco tem (no próprio barco ou no item, que levam as mesmas chaves)
    // =====================================================================

    static List<Integer> baus(PersistentDataContainer pdc) {
        List<Integer> l = new ArrayList<>();
        String s = pdc.get(K_BAUS, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return l;
        for (String p : s.split(",")) {
            try { l.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignored) { }
        }
        return l;
    }

    private static void gravarBaus(PersistentDataContainer pdc, List<Integer> l) {
        if (l.isEmpty()) pdc.remove(K_BAUS);
        else pdc.set(K_BAUS, PersistentDataType.STRING, String.join(",", l.stream().map(String::valueOf).toList()));
    }

    static boolean tem(Boat b, Peca p) {
        return b.getPersistentDataContainer().has(p.chave());
    }

    static boolean ancorado(Boat b) {
        return b.getPersistentDataContainer().has(K_ANCORADO);
    }

    public static int vidaMax(Boat b) {
        return tem(b, Peca.CASCO) ? VIDA_REFORCADO : VIDA_CASCO;
    }

    public static int vida(Boat b) {
        Integer v = b.getPersistentDataContainer().get(K_VIDA, PersistentDataType.INTEGER);
        return v == null ? vidaMax(b) : Math.min(v, vidaMax(b));
    }

    /** Copia as melhorias (e o conteúdo dos baús) do barco para o item ou do item para o barco. */
    static void copiar(io.papermc.paper.persistence.PersistentDataContainerView de, PersistentDataContainer para) {
        copiarByte(de, para, Velas.CHAVE);
        copiarByte(de, para, K_REFORCO);
        copiarByte(de, para, K_REDE);
        copiarByte(de, para, K_ANCORA);
        String baus = de.get(K_BAUS, PersistentDataType.STRING);
        if (baus != null) para.set(K_BAUS, PersistentDataType.STRING, baus);
        for (NamespacedKey k : K_BAU) {
            byte[] b = de.get(k, PersistentDataType.BYTE_ARRAY);
            if (b != null) para.set(k, PersistentDataType.BYTE_ARRAY, b);
        }
        byte[] est = de.get(K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
        if (est != null) para.set(K_ESTANDARTE, PersistentDataType.BYTE_ARRAY, est);
        Integer v = de.get(K_VIDA, PersistentDataType.INTEGER);
        if (v != null) para.set(K_VIDA, PersistentDataType.INTEGER, v);
    }

    private static void copiarByte(io.papermc.paper.persistence.PersistentDataContainerView de, PersistentDataContainer para, NamespacedKey k) {
        if (de.has(k)) para.set(k, PersistentDataType.BYTE, (byte) 1);
    }

    /** As linhas do item que contam o que o barco leva. */
    static List<Component> resumo(PersistentDataContainer pdc) {
        List<String> m = new ArrayList<>();
        if (pdc.has(Velas.CHAVE)) m.add(pdc.has(K_ESTANDARTE) ? "vela com estandarte" : "vela");
        int nb = baus(pdc).size();
        if (nb > 0) m.add(nb == 1 ? "1 baú" : nb + " baús");
        if (pdc.has(K_REDE)) m.add("rede");
        if (pdc.has(K_ANCORA)) m.add("âncora");
        if (pdc.has(K_REFORCO)) m.add("casco reforçado");
        List<Component> l = new ArrayList<>();
        if (!m.isEmpty()) l.add(Component.text("Leva: " + String.join(", ", m) + ".", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        Integer v = pdc.get(K_VIDA, PersistentDataType.INTEGER);
        int max = pdc.has(K_REFORCO) ? VIDA_REFORCADO : VIDA_CASCO;
        if (v != null && v < max) l.add(Component.text("Casco: " + v + "/" + max + " (conserte com tábuas).", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        return l;
    }

    // =====================================================================
    //  Instalar
    // =====================================================================

    /**
     * O jogador clicou o barco com algo na mão que pode ser uma melhoria. Devolve true se usou o
     * clique (instalou, consertou ou explicou por que não dá).
     */
    boolean instalar(Player p, Boat b, ItemStack mao, BarcoGrande barco) {
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (mao.getType() == Material.CHEST) {
            List<Integer> l = baus(pdc);
            if (l.size() >= MAX_BAUS) {
                p.sendActionBar(Component.text("⛵ O Barco Grande leva no máximo " + MAX_BAUS + " baús.", NamedTextColor.RED));
                return true;
            }
            int banco = -1;
            for (int i : ORDEM_BAUS) {
                if (!l.contains(i) && !barco.ocupado(b, i)) {
                    banco = i;
                    break;
                }
            }
            if (banco < 0) {
                p.sendActionBar(Component.text("⛵ Não há banco livre para o baú: peça para alguém trocar de lugar.", NamedTextColor.RED));
                return true;
            }
            l.add(banco);
            gravarBaus(pdc, l);
            if (!criativo) mao.setAmount(mao.getAmount() - 1);
            barco.porBau(b, banco);
            p.playSound(b.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1.1f);
            p.sendActionBar(Component.text("⛵ Baú de convés posto (" + l.size() + " de " + MAX_BAUS + "). Clique nele para abrir.", COR));
            return true;
        }
        if (Tag.ITEMS_BANNERS.isTagged(mao.getType())) {
            if (!pdc.has(Velas.CHAVE)) {
                p.sendActionBar(Component.text("⛵ O estandarte vai na vela: ponha a Vela de Barco primeiro.", NamedTextColor.RED));
                return true;
            }
            ItemStack um = mao.asOne();
            byte[] antes = pdc.get(K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
            pdc.set(K_ESTANDARTE, PersistentDataType.BYTE_ARRAY, um.serializeAsBytes());
            if (!criativo) mao.setAmount(mao.getAmount() - 1);
            if (antes != null) devolver(p, ItemStack.deserializeBytes(antes));
            barco.trocarVela(b);
            p.playSound(b.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1.3f);
            p.sendActionBar(Component.text("⛵ A vela agora leva o seu estandarte.", COR));
            return true;
        }
        if (Tag.PLANKS.isTagged(mao.getType())) {
            int v = vida(b), max = vidaMax(b);
            if (v >= max) return false; // casco inteiro: o clique segue (subir no barco)
            int nova = Math.min(max, v + 4);
            pdc.set(K_VIDA, PersistentDataType.INTEGER, nova);
            if (!criativo) mao.setAmount(mao.getAmount() - 1);
            p.playSound(b.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 0.8f);
            b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0, 0.5, 0), 10, 0.6, 0.2, 0.6, Material.SPRUCE_PLANKS.createBlockData());
            p.sendActionBar(Component.text("🔨 Casco consertado: " + nova + "/" + max + ".", COR));
            return true;
        }
        Peca peca = peca(mao);
        if (peca == null) return false;
        if (pdc.has(peca.chave())) {
            p.sendActionBar(Component.text("⛵ Este barco já tem " + peca.nome + ".", NamedTextColor.RED));
            return true;
        }
        if (peca == Peca.CASCO) {
            // O reforço não tira a vida que falta: soma a diferença.
            int v = vida(b);
            pdc.set(K_VIDA, PersistentDataType.INTEGER, v + (VIDA_REFORCADO - VIDA_CASCO));
        }
        pdc.set(peca.chave(), PersistentDataType.BYTE, (byte) 1);
        if (!criativo) mao.setAmount(mao.getAmount() - 1);
        p.playSound(b.getLocation(), peca == Peca.ANCORA ? Sound.BLOCK_ANVIL_PLACE : Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.8f, 1.2f);
        p.sendActionBar(Component.text("⛵ " + peca.nome + " instalado" + (peca == Peca.REDE || peca == Peca.ANCORA ? "a" : "")
                + (peca == Peca.ANCORA ? ". Agachado + mão vazia: menu do barco (baixar a âncora)." : peca == Peca.REDE ? ". Ponha um baú no barco para guardar os peixes." : "."), COR));
        return true;
    }

    private static void devolver(Player p, ItemStack i) {
        for (ItemStack s : p.getInventory().addItem(i).values()) p.getWorld().dropItem(p.getLocation(), s);
    }

    // =====================================================================
    //  Baús de convés
    // =====================================================================

    private static final class BauConves implements InventoryHolder {
        final UUID barco;
        final int banco;
        Inventory inv;

        BauConves(UUID barco, int banco) {
            this.barco = barco;
            this.banco = banco;
        }

        @Override public Inventory getInventory() { return inv; }
    }

    private static String chave(UUID barco, int banco) {
        return barco + ":" + banco;
    }

    void abrirBau(Player p, Boat b, int banco) {
        BauConves h = abertos.computeIfAbsent(chave(b.getUniqueId(), banco), k -> {
            BauConves n = new BauConves(b.getUniqueId(), banco);
            n.inv = Bukkit.createInventory(n, 27, Component.text("Baú do Barco Grande", COR));
            byte[] d = b.getPersistentDataContainer().get(K_BAU[banco], PersistentDataType.BYTE_ARRAY);
            if (d != null) n.inv.setContents(ItemStack.deserializeItemsFromBytes(d));
            return n;
        });
        p.openInventory(h.inv);
        p.playSound(b.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.1f);
    }

    @EventHandler
    public void aoFechar(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof BauConves h)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (Bukkit.getEntity(h.barco) instanceof Boat b && b.isValid()) guardar(h, b);
            if (h.inv.getViewers().isEmpty()) abertos.remove(chave(h.barco, h.banco));
        });
    }

    private static void guardar(BauConves h, Boat b) {
        b.getPersistentDataContainer().set(K_BAU[h.banco], PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(h.inv.getContents()));
    }

    /** Antes de recolher, afundar ou o barco sumir: grava os baús abertos e fecha para quem estava olhando. */
    void fecharBaus(Boat b) {
        abertos.entrySet().removeIf(en -> {
            BauConves h = en.getValue();
            if (!h.barco.equals(b.getUniqueId())) return false;
            guardar(h, b);
            for (HumanEntity v : List.copyOf(h.inv.getViewers())) v.closeInventory();
            return true;
        });
    }

    /** O conteúdo de um baú (fechado ou aberto). */
    private ItemStack[] conteudo(Boat b, int banco) {
        BauConves h = abertos.get(chave(b.getUniqueId(), banco));
        if (h != null) return h.inv.getContents();
        byte[] d = b.getPersistentDataContainer().get(K_BAU[banco], PersistentDataType.BYTE_ARRAY);
        return d == null ? new ItemStack[27] : ItemStack.deserializeItemsFromBytes(d);
    }

    /** Guarda um item em algum baú do barco. Devolve false se não coube. */
    boolean guardarNoBau(Boat b, ItemStack item) {
        for (int banco : baus(b.getPersistentDataContainer())) {
            BauConves h = abertos.get(chave(b.getUniqueId(), banco));
            if (h != null) {
                if (h.inv.addItem(item).isEmpty()) return true;
                continue;
            }
            Inventory tmp = Bukkit.createInventory(null, 27);
            tmp.setContents(conteudo(b, banco));
            if (tmp.addItem(item).isEmpty()) {
                b.getPersistentDataContainer().set(K_BAU[banco], PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(tmp.getContents()));
                return true;
            }
        }
        return false;
    }

    /** Tira o último baú posto: o conteúdo vai para o jogador (o que não couber, no chão). */
    private boolean tirarBau(Player p, Boat b, BarcoGrande barco) {
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        List<Integer> l = baus(pdc);
        if (l.isEmpty()) return false;
        fecharBaus(b);
        int banco = l.removeLast();
        for (ItemStack i : conteudo(b, banco)) if (i != null && !i.isEmpty()) devolver(p, i);
        pdc.remove(K_BAU[banco]);
        gravarBaus(pdc, l);
        barco.tirarBau(b, banco);
        devolver(p, new ItemStack(Material.CHEST));
        return true;
    }

    /** Tudo o que estava nos baús (para afundar). */
    List<ItemStack> esvaziarBaus(Boat b) {
        fecharBaus(b);
        List<ItemStack> l = new ArrayList<>();
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        for (int banco : baus(pdc)) {
            for (ItemStack i : conteudo(b, banco)) if (i != null && !i.isEmpty()) l.add(i);
            pdc.remove(K_BAU[banco]);
        }
        return l;
    }

    // =====================================================================
    //  O casco
    // =====================================================================

    /**
     * Dano no casco (Kraken, canhão pirata, raio, pedra na tempestade). Com o casco reforçado,
     * metade. Chegando a zero, o barco afunda.
     */
    public void danificar(Boat b, double dano, String motivo) {
        if (!BarcoGrande.eBarcoGrande(b) || !b.isValid()) return;
        double d = tem(b, Peca.CASCO) ? dano * 0.5 : dano;
        int nova = (int) Math.round(vida(b) - d);
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.7f, 1.3f);
        b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0, 0.4, 0), 14, 0.7, 0.2, 0.7, Material.SPRUCE_PLANKS.createBlockData());
        if (nova <= 0) {
            plugin.barcoGrande().afundar(b, motivo);
            return;
        }
        b.getPersistentDataContainer().set(K_VIDA, PersistentDataType.INTEGER, nova);
        Component aviso = Component.text("⛵ " + motivo + "! Casco: " + nova + "/" + vidaMax(b), nova <= vidaMax(b) / 4 ? NamedTextColor.RED : NamedTextColor.GOLD);
        for (Player p : plugin.barcoGrande().aBordo(b)) p.sendActionBar(aviso);
    }

    // =====================================================================
    //  Âncora e rede (a cada tick e a cada segundo)
    // =====================================================================

    /** A cada tick: barco ancorado não sai do lugar. */
    void segurar(Boat b) {
        if (!ancorado(b)) {
            ancoras.remove(b.getUniqueId());
            return;
        }
        Location fixo = ancoras.computeIfAbsent(b.getUniqueId(), k -> b.getLocation());
        Location agora = b.getLocation();
        if (!fixo.getWorld().equals(agora.getWorld())) {
            ancoras.put(b.getUniqueId(), agora);
            return;
        }
        double dx = agora.getX() - fixo.getX(), dz = agora.getZ() - fixo.getZ();
        if (dx * dx + dz * dz > 0.04) {
            Location volta = fixo.clone();
            volta.setYaw(agora.getYaw());
            b.teleport(volta, io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS);
            b.setVelocity(new org.bukkit.util.Vector());
            if (Bukkit.getCurrentTick() % 40 == 0 && BarcoGrande.piloto(b) instanceof Player p) {
                p.sendActionBar(Component.text("⚓ A âncora está baixada: levante pelo menu (agachado + mão vazia).", COR));
            }
        }
    }

    /**
     * A cada segundo: a rede puxa peixe conforme o barco anda na água (a cada ~24 blocos; no
     * cardume, a cada ~8 e sempre com peixe).
     */
    void puxarRede(Boat b) {
        if (!tem(b, Peca.REDE) || b.getStatus() != Boat.Status.IN_WATER) {
            ultimo.remove(b.getUniqueId());
            return;
        }
        Location agora = b.getLocation(), antes = ultimo.put(b.getUniqueId(), agora);
        if (antes == null || !antes.getWorld().equals(agora.getWorld())) return;
        double d = Math.min(12, antes.distance(agora));
        if (d < 0.5) return;
        boolean cardume = plugin.cardumes() != null && plugin.cardumes().em(agora);
        double soma = andado.merge(b.getUniqueId(), d, Double::sum);
        if (soma < (cardume ? 8 : 24)) return;
        andado.remove(b.getUniqueId());
        if (!cardume && Math.random() < 0.35) return;
        if (baus(b.getPersistentDataContainer()).isEmpty()) {
            if (BarcoGrande.piloto(b) instanceof Player p) p.sendActionBar(Component.text("🎣 A rede pegou um peixe, mas não há baú no barco para guardar.", NamedTextColor.YELLOW));
            return;
        }
        Player dono = BarcoGrande.piloto(b);
        ItemStack peixe = plugin.pesca().pescaDaRede(dono, agora, cardume);
        if (!guardarNoBau(b, peixe)) {
            if (dono != null) dono.sendActionBar(Component.text("🎣 Os baús do barco estão cheios: a rede soltou o peixe.", NamedTextColor.YELLOW));
            return;
        }
        b.getWorld().spawnParticle(Particle.SPLASH, agora.clone().add(0, 0.2, 0), 12, 0.8, 0.1, 0.8, 0.1);
        b.getWorld().playSound(agora, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.6f, 1.2f);
        if (dono != null) {
            Component nome = peixe.getItemMeta() != null && peixe.getItemMeta().hasItemName() ? peixe.getItemMeta().itemName() : Component.translatable(peixe.translationKey());
            dono.sendActionBar(Component.text("🎣 A rede pegou: ", COR).append(nome).append(Component.text(peixe.getAmount() > 1 ? " x" + peixe.getAmount() : "", COR)));
        }
    }

    void esquecer(UUID barco) {
        ultimo.remove(barco);
        andado.remove(barco);
        ancoras.remove(barco);
    }

    // =====================================================================
    //  Menu do barco (agachado + mão vazia)
    // =====================================================================

    private static final class Menu implements InventoryHolder {
        final UUID barco;
        Inventory inv;

        Menu(UUID barco) {
            this.barco = barco;
        }

        @Override public Inventory getInventory() { return inv; }
    }

    private static final int S_VELA = 10, S_ESTANDARTE = 11, S_BAUS = 12, S_ANCORA = 13, S_REDE = 14, S_CASCO = 15, S_BICHOS = 16, S_RECOLHER = 22;

    void abrirMenu(Player p, Boat b) {
        Menu m = new Menu(b.getUniqueId());
        m.inv = Bukkit.createInventory(m, 27, Component.text("⛵ Barco Grande", COR));
        desenhar(m.inv, b);
        p.openInventory(m.inv);
    }

    private void desenhar(Inventory inv, Boat b) {
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        ItemStack vidro = botao(Material.BROWN_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, vidro);
        boolean vela = pdc.has(Velas.CHAVE);
        inv.setItem(S_VELA, vela ? botao(Material.WHITE_BANNER, "Vela", NamedTextColor.WHITE, "Clique: tirar a vela (o mastro libera um banco).")
                : botao(Material.STRING, "Sem vela", NamedTextColor.GRAY, "Clique a Vela de Barco no barco para pôr."));
        inv.setItem(S_ESTANDARTE, pdc.has(K_ESTANDARTE) ? botao(Material.RED_BANNER, "Estandarte na vela", NamedTextColor.RED, "Clique: tirar o estandarte.")
                : botao(Material.GRAY_BANNER, "Vela sem estandarte", NamedTextColor.GRAY, "Com a vela posta, clique um estandarte no barco."));
        int nb = baus(pdc).size();
        inv.setItem(S_BAUS, botao(Material.CHEST, "Baús de convés: " + nb + " de " + MAX_BAUS, COR,
                "Clique um baú no barco para pôr.", nb > 0 ? "Clique aqui: tirar o último baú (o conteúdo vem junto)." : "Clique no baú do convés para abrir."));
        if (tem(b, Peca.ANCORA)) {
            boolean baixa = ancorado(b);
            inv.setItem(S_ANCORA, botao(Material.ANVIL, baixa ? "Âncora baixada" : "Âncora levantada", baixa ? NamedTextColor.GOLD : NamedTextColor.GREEN,
                    baixa ? "Clique: levantar a âncora e navegar." : "Clique: baixar a âncora (o barco fica parado).", "Agachado + clique: tirar a âncora do barco."));
        } else {
            inv.setItem(S_ANCORA, botao(Material.IRON_INGOT, "Sem âncora", NamedTextColor.GRAY, "Faça uma Âncora e clique no barco."));
        }
        inv.setItem(S_REDE, tem(b, Peca.REDE) ? botao(Material.COBWEB, "Rede de pesca", NamedTextColor.AQUA, "Navegando, pesca sozinha (mais no cardume).", "Clique: tirar a rede.")
                : botao(Material.STRING, "Sem rede", NamedTextColor.GRAY, "Faça uma Rede de Pesca e clique no barco."));
        int v = vida(b), max = vidaMax(b);
        inv.setItem(S_CASCO, botao(tem(b, Peca.CASCO) ? Material.IRON_BARS : Material.SPRUCE_PLANKS, "Casco: " + v + "/" + max,
                v < max / 4 ? NamedTextColor.RED : v < max ? NamedTextColor.YELLOW : NamedTextColor.GREEN,
                tem(b, Peca.CASCO) ? "Reforçado: leva metade do dano." : "Comum. O Casco Reforçado aguenta bem mais.",
                "Tábuas clicadas no barco consertam (+4 cada).", tem(b, Peca.CASCO) ? "Clique: tirar o reforço." : ""));
        int bichos = plugin.barcoGrande().bichos(b).size();
        inv.setItem(S_BICHOS, botao(Material.LEAD, "Bichos a bordo: " + bichos, NamedTextColor.GREEN,
                "Clique no barco segurando um bicho no laço: ele embarca.", bichos > 0 ? "Clique aqui: desembarcar os bichos." : ""));
        inv.setItem(S_RECOLHER, botao(Material.SPRUCE_BOAT, "Recolher o barco", COR, "Vira item com tudo o que ele leva.", "Ninguém pode estar a bordo."));
    }

    private static ItemStack botao(Material m, String nome, TextColor cor, String... linhas) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(Component.text(nome, cor));
            List<Component> l = new ArrayList<>();
            for (String s : linhas) if (!s.isEmpty()) l.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(l);
        });
        return i;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Menu m)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getInventory()) return;
        if (!(Bukkit.getEntity(m.barco) instanceof Boat b) || !b.isValid() || b.getLocation().distanceSquared(p.getLocation()) > 12 * 12) {
            p.closeInventory();
            return;
        }
        BarcoGrande barco = plugin.barcoGrande();
        PersistentDataContainer pdc = b.getPersistentDataContainer();
        switch (e.getSlot()) {
            case S_VELA -> {
                if (!pdc.has(Velas.CHAVE)) return;
                barco.tirarVela(b, p);
            }
            case S_ESTANDARTE -> {
                byte[] est = pdc.get(K_ESTANDARTE, PersistentDataType.BYTE_ARRAY);
                if (est == null) return;
                pdc.remove(K_ESTANDARTE);
                devolver(p, ItemStack.deserializeBytes(est));
                barco.trocarVela(b);
            }
            case S_BAUS -> {
                if (!tirarBau(p, b, barco)) return;
            }
            case S_ANCORA -> {
                if (!tem(b, Peca.ANCORA)) return;
                if (e.isShiftClick()) {
                    pdc.remove(K_ANCORA);
                    pdc.remove(K_ANCORADO);
                    ancoras.remove(b.getUniqueId());
                    devolver(p, Peca.ANCORA.item());
                } else if (ancorado(b)) {
                    pdc.remove(K_ANCORADO);
                    ancoras.remove(b.getUniqueId());
                    b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CHAIN_STEP, 1f, 1.2f);
                } else {
                    pdc.set(K_ANCORADO, PersistentDataType.BYTE, (byte) 1);
                    ancoras.put(b.getUniqueId(), b.getLocation());
                    b.setVelocity(new org.bukkit.util.Vector());
                    b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1f, 0.6f);
                    b.getWorld().playSound(b.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 0.8f);
                }
            }
            case S_REDE -> {
                if (!tem(b, Peca.REDE)) return;
                pdc.remove(K_REDE);
                devolver(p, Peca.REDE.item());
            }
            case S_CASCO -> {
                if (!tem(b, Peca.CASCO)) return;
                int v = vida(b);
                pdc.remove(K_REFORCO);
                pdc.set(K_VIDA, PersistentDataType.INTEGER, Math.max(1, Math.min(VIDA_CASCO, v - (VIDA_REFORCADO - VIDA_CASCO))));
                devolver(p, Peca.CASCO.item());
            }
            case S_BICHOS -> {
                if (barco.desembarcarBichos(b) == 0) return;
            }
            case S_RECOLHER -> {
                if (!barco.recolherPeloMenu(b, p)) return;
                p.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        desenhar(e.getInventory(), b);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }

    /** Na saída: grava os baús abertos. */
    public void desligar() {
        for (BauConves h : abertos.values()) {
            if (Bukkit.getEntity(h.barco) instanceof Boat b) guardar(h, b);
            for (HumanEntity v : List.copyOf(h.inv.getViewers())) v.closeInventory();
        }
        abertos.clear();
    }
}
