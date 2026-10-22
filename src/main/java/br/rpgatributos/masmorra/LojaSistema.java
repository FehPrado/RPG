package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A Loja do Sistema (2.71): troca Cristais de Mana por poções de caçador, Pergaminho de Retorno,
 * Medidor de Mana, Chave Instantânea e peças de Caçador do seu rank. Abre com /masmorra loja ou no
 * Altar do Portal. Aqui também ficam os usos desses itens.
 */
public final class LojaSistema implements Listener {

    private static final NamespacedKey K_ITEM = new NamespacedKey("rpgatributos", "item_sistema");
    static final TextColor COR = TextColor.color(0x4FC3F7);

    /** Os itens da loja: nome, preço em mana e a descrição. */
    public enum Oferta {
        POCAO_CACADOR("Poção do Caçador", 6, "Cura na hora e Regeneração por 10 s."),
        POCAO_MANA("Poção de Mana", 6, "Recupera 50 de mana."),
        POCAO_FORCA("Poção de Força", 10, "Força II por 60 s."),
        PERGAMINHO("Pergaminho de Retorno", 16, "Num portal: sai na hora com tudo o que pegou (não vale no Vermelho)."),
        MEDIDOR("Medidor de Mana", 12, "Clique: mostra o rank e o estado do portal mais perto (até 64 blocos)."),
        CHAVE("Chave Instantânea", 24, "Abre na hora, onde você está, um portal do seu Rank de Caçador (só sua party)."),
        PECA_CACADOR("Peça de Caçador", 30, "Uma peça do Equipamento de Caçador do seu rank.");

        public final String nome, descricao;
        final int preco;

        Oferta(String nome, int preco, String descricao) {
            this.nome = nome;
            this.preco = preco;
            this.descricao = descricao;
        }

        /** O preço para este caçador (a Chave e a peça sobem com o rank). */
        public int preco(Rank r) {
            return switch (this) {
                case CHAVE -> preco + 16 * r.ordinal();
                case PECA_CACADOR -> preco + 20 * r.ordinal();
                default -> preco;
            };
        }
    }

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private final RPGAtributos plugin;

    public LojaSistema(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Os itens
    // =====================================================================

    public ItemStack item(Oferta o, Player p) {
        Rank r = p == null ? Rank.E : Cacador.rank(p);
        if (o == Oferta.PECA_CACADOR) return plugin.equipamentoPortal().pecaCacadorAleatoria(r);
        ItemStack i = switch (o) {
            case POCAO_CACADOR, POCAO_MANA, POCAO_FORCA -> new ItemStack(Material.POTION);
            case PERGAMINHO -> new ItemStack(Material.PAPER);
            case MEDIDOR -> new ItemStack(Material.SPYGLASS);
            case CHAVE -> new ItemStack(Material.TRIPWIRE_HOOK);
            default -> new ItemStack(Material.PAPER);
        };
        i.editMeta(m -> {
            m.itemName(Component.text(o.nome, COR));
            m.lore(List.of(Component.text(o.descricao, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("[Sistema]", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)));
            m.getPersistentDataContainer().set(K_ITEM, PersistentDataType.STRING, o.name());
            if (o == Oferta.CHAVE || o == Oferta.MEDIDOR || o == Oferta.PERGAMINHO) m.setMaxStackSize(o == Oferta.MEDIDOR ? 1 : 16);
            if (m instanceof PotionMeta pm) {
                switch (o) {
                    case POCAO_CACADOR -> {
                        pm.setColor(Color.fromRGB(0xE53935));
                        pm.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), true);
                        pm.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0), true);
                    }
                    case POCAO_MANA -> pm.setColor(Color.fromRGB(0x1E88E5));
                    case POCAO_FORCA -> {
                        pm.setColor(Color.fromRGB(0xFF8F00));
                        pm.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 1), true);
                    }
                    default -> { }
                }
            }
        });
        return i;
    }

    public static Oferta de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_ITEM, PersistentDataType.STRING);
        if (s == null) return null;
        try { return Oferta.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** Uma chave para o MateriaisListener. */
    public static NamespacedKey chave() { return K_ITEM; }

    // =====================================================================
    //  A loja
    // =====================================================================

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inv = Bukkit.createInventory(t, 27, Component.text("[Sistema] Loja", COR));
        desenhar(p, t);
        p.openInventory(t.inv);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f);
    }

    private void desenhar(Player p, Tela t) {
        t.inv.clear();
        Rank r = Cacador.rank(p);
        int mana = ManaCristal.valor(p);
        Oferta[] os = Oferta.values();
        for (int k = 0; k < os.length && k < SLOTS.length; k++) {
            Oferta o = os[k];
            int preco = o.preco(r);
            ItemStack icone = o == Oferta.PECA_CACADOR ? new ItemStack(Material.IRON_CHESTPLATE) : item(o, p).clone();
            icone.editMeta(m -> {
                m.itemName(Component.text(o.nome + (o == Oferta.PECA_CACADOR || o == Oferta.CHAVE ? " (" + r.nome() + ")" : ""), COR, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(o.descricao, NamedTextColor.GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("Preço: " + preco + " de mana em cristais", mana >= preco ? NamedTextColor.GREEN : NamedTextColor.RED));
                lore.add(Component.text(mana >= preco ? "» Clique para comprar" : "Você tem " + mana + " de mana.", mana >= preco ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                m.addItemFlags(ItemFlag.values());
            });
            t.inv.setItem(SLOTS[k], icone);
        }
        ItemStack info = new ItemStack(Material.AMETHYST_CLUSTER);
        info.editMeta(m -> {
            m.itemName(Component.text("Seus Cristais de Mana: " + mana, ManaCristal.COR, TextDecoration.BOLD));
            m.lore(List.of(Component.text("Os monstros, relíquias, baús e minas dos portais deixam cristais.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        });
        t.inv.setItem(22, info);
        ItemStack vidro = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < t.inv.getSize(); i++) if (t.inv.getItem(i) == null) t.inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != topo) return;
        for (int k = 0; k < SLOTS.length && k < Oferta.values().length; k++) {
            if (SLOTS[k] != e.getSlot()) continue;
            Oferta o = Oferta.values()[k];
            int preco = o.preco(Cacador.rank(p));
            if (p.getGameMode() != GameMode.CREATIVE && !ManaCristal.gastar(p, preco)) {
                p.sendActionBar(Component.text("[Sistema] Faltam Cristais de Mana (" + preco + ").", NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                return;
            }
            for (ItemStack s : p.getInventory().addItem(item(o, p)).values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            desenhar(p, t);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Usar os itens
    // =====================================================================

    @EventHandler
    public void aoBeber(PlayerItemConsumeEvent e) {
        if (de(e.getItem()) == Oferta.POCAO_MANA) {
            plugin.arcano().darMana(e.getPlayer(), 50);
            e.getPlayer().sendActionBar(Component.text("✦ +50 de mana", COR));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Oferta o = de(e.getItem());
        if (o == null) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        switch (o) {
            case PERGAMINHO -> {
                e.setCancelled(true);
                Masmorra m = plugin.masmorras().de(p);
                if (m == null) { p.sendActionBar(Component.text("[Sistema] O pergaminho só funciona dentro de um portal.", NamedTextColor.GRAY)); return; }
                if (plugin.masmorras().presoNoVermelho(m)) { p.sendActionBar(Component.text("⛔ No Portal Vermelho não há retorno.", NamedTextColor.DARK_RED)); return; }
                if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
                plugin.masmorras().sair(p, m, "O Pergaminho de Retorno te trouxe de volta com tudo o que você pegou.");
            }
            case MEDIDOR -> {
                // Não cancela: a luneta continua dando zoom.
                if (p.hasCooldown(mao)) return;
                p.setCooldown(mao, 40);
                for (Component c : plugin.portais().medir(p, 64)) p.sendMessage(c);
            }
            case CHAVE -> {
                e.setCancelled(true);
                if (plugin.masmorras().ehMundo(p.getWorld()) || p.getWorld().getEnvironment() == World.Environment.THE_END) {
                    p.sendActionBar(Component.text("[Sistema] A chave não gira aqui.", NamedTextColor.RED));
                    return;
                }
                if (plugin.territorios().em(p.getLocation()) != null && !plugin.territorios().podeConstruir(p, p.getLocation())) {
                    p.sendActionBar(Component.text("[Sistema] Território de outra pessoa.", NamedTextColor.RED));
                    return;
                }
                Objetivo[] objs = Objetivo.values();
                CristalPortal.Dados d = new CristalPortal.Dados(Cacador.rank(p), objs[ThreadLocalRandom.current().nextInt(objs.length)],
                        EnumSet.noneOf(ModificadorPortal.class));
                String erro = plugin.masmorras().abrirComCristal(p, d, p.getLocation().getBlock().getLocation());
                if (erro != null) {
                    p.sendMessage(Component.text(erro, NamedTextColor.RED));
                    return;
                }
                if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
                p.playSound(p.getLocation(), Sound.BLOCK_VAULT_OPEN_SHUTTER, 1f, 1f);
            }
            default -> { }
        }
    }
}
