package br.rpgatributos.conforto;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /rpg: o menu central, com um botão para cada sistema do plugin (o botão roda o comando dele,
 * então as permissões continuam valendo).
 */
public final class MenuRpg implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xFFD54F);

    private record Botao(int slot, Material icone, String nome, TextColor cor, String descricao, String comando) { }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        final Map<Integer, String> comandos = new HashMap<>();

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private static final List<Botao> BOTOES = List.of(
            // Você
            new Botao(10, Material.PLAYER_HEAD, "Atributos", NamedTextColor.GOLD, "Seus 13 atributos e bônus", "atributos"),
            new Botao(11, Material.EXPERIENCE_BOTTLE, "Talentos", TextColor.color(0xB39DDB), "Árvores de talentos e renascer", "talentos"),
            new Botao(12, Material.BOOK, "Classe", TextColor.color(0xFFD54F), "Árvore de classes e habilidades", "classe"),
            new Botao(13, Material.IRON_SWORD, "Combos", TextColor.color(0xE05050), "Golpes de 3 cliques e proficiência", "combos"),
            new Botao(14, Material.ENCHANTED_BOOK, "Grimório", TextColor.color(0xC77DFF), "Suas magias e o criador de magias", "grimorio"),
            new Botao(15, Material.NAME_TAG, "Títulos", NamedTextColor.YELLOW, "Conquistas que dão bônus", "titulos"),
            new Botao(16, Material.WRITABLE_BOOK, "Diário", TextColor.color(0x8D6E63), "Os marcos da sua jornada", "diario"),
            // Coisas
            new Botao(19, Material.CHEST, "Mochila", TextColor.color(0xC9955C), "Seu baú pessoal", "mochila"),
            new Botao(20, Material.GOLD_NUGGET, "Acessórios", TextColor.color(0xE8C15A), "Anéis, amuletos, cintos e bolso", "acessorios"),
            new Botao(21, Material.ARMOR_STAND, "Guarda-roupa", TextColor.color(0xAD1457), "Aparência da armadura", "guardaroupa"),
            new Botao(22, Material.SMITHING_TABLE, "Forja", TextColor.color(0xE8823A), "Raridades e chances da forja", "forja"),
            new Botao(23, Material.BREWING_STAND, "Alquimia", TextColor.color(0x4FD1A5), "Elixires, gemas e acessórios", "alquimia"),
            new Botao(24, Material.SMOKER, "Receitas", TextColor.color(0xE8A33D), "Pratos da Cozinha", "receitas"),
            new Botao(25, Material.FILLED_MAP, "Álbum", TextColor.color(0x4FC3F7), "Álbum de Cartas", "album"),
            // Mundo e gente
            new Botao(28, Material.CAKE, "Party", TextColor.color(0x55CDFC), "Seu grupo", "party"),
            new Botao(29, Material.LODESTONE, "Território", TextColor.color(0x4CAF50), "Seu território", "territorio"),
            new Botao(30, Material.BELL, "Colônia", TextColor.color(0x8BC34A), "Moradores e exército", "colonia"),
            new Botao(31, Material.GOLDEN_HELMET, "Reino", TextColor.color(0xFFC107), "Cargos, tesouro e guerra", "reino"),
            new Botao(32, Material.LEAD, "Companheiros", TextColor.color(0xC9955C), "Seus pets e montarias", "pets"),
            new Botao(33, Material.QUARTZ_BLOCK, "Fé", TextColor.color(0xFFE082), "Seu deus, devoção e milagre", "deus"),
            new Botao(34, Material.SPYGLASS, "Estrelas", TextColor.color(0x9FA8DA), "Constelações observadas", "estrelas"),
            // Aventura
            new Botao(37, Material.COMPASS, "Enciclopédia", TextColor.color(0x8D6E63), "Biomas, estruturas e relíquias", "enciclopedia"),
            new Botao(38, Material.LECTERN, "Crônicas", TextColor.color(0x6D4C41), "A campanha do Cronista", "cronista"),
            new Botao(39, Material.ZOMBIE_HEAD, "Bestiário", NamedTextColor.RED, "Monstros derrotados e marcos", "bestiario"),
            new Botao(40, Material.CLOCK, "Calendário", TextColor.color(0x9FE7FF), "Estação, lua e clima", "calendario"),
            new Botao(41, Material.END_PORTAL_FRAME, "Portais", TextColor.color(0x0277BD), "Portais abertos no mundo", "portais"),
            new Botao(42, Material.CRYING_OBSIDIAN, "Torre", TextColor.color(0x7E57C2), "Ranking da Torre Infinita", "torre ranking"),
            new Botao(43, Material.TROPICAL_FISH, "Peixes", TextColor.color(0x2FA4C9), "Diário do Pescador", "peixes"),
            // Outros
            new Botao(46, Material.KNOWLEDGE_BOOK, "Guia", TextColor.color(0x6A1B9A), "O livro Guia do Aventureiro", "guia"),
            new Botao(47, Material.GOLD_INGOT, "Recordes", TextColor.color(0xFFB300), "Recordes do servidor", "recordes"),
            new Botao(48, Material.AMETHYST_SHARD, "Segredos", TextColor.color(0xB388FF), "Conquistas secretas", "segredos"),
            new Botao(50, Material.OAK_SIGN, "Placar lateral", NamedTextColor.WHITE, "Liga ou desliga o placar do lado da tela", "placar"),
            new Botao(51, Material.LEATHER_HELMET, "Cosméticos", NamedTextColor.LIGHT_PURPLE, "Item na cabeça e tag", "cosmeticos"),
            new Botao(52, Material.PAPER, "Missões", TextColor.color(0x8BC34A), "Pedidos dos aldeões", "missoes"));

    private final RPGAtributos plugin;

    public MenuRpg(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⚔ RPGAtributos", COR, TextDecoration.BOLD));
        ItemStack vidro = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < 54; i++) t.inventario.setItem(i, vidro);
        t.inventario.setItem(4, item(Material.NETHER_STAR, Component.text("⚔ " + p.getName(), COR, TextDecoration.BOLD),
                List.of(Component.text("Nível total " + plugin.stats().nivelTotal(p), NamedTextColor.YELLOW),
                        Component.text("Talentos livres: " + plugin.talentos().livres(p), NamedTextColor.GRAY),
                        Component.text("Cartas: " + plugin.album().total(p), NamedTextColor.GRAY))));
        for (Botao b : BOTOES) {
            String perm = permissao(b.comando());
            if (perm != null && !p.hasPermission(perm)) continue;
            t.inventario.setItem(b.slot(), item(b.icone(), Component.text(b.nome(), b.cor(), TextDecoration.BOLD),
                    List.of(Component.text(b.descricao(), NamedTextColor.GRAY), Component.empty(),
                            Component.text("/" + b.comando(), NamedTextColor.DARK_GRAY))));
            t.comandos.put(b.slot(), b.comando());
        }
        p.openInventory(t.inventario);
    }

    /** Permissão do comando (a do plugin.yml), para esconder o que o jogador não pode usar. */
    private String permissao(String comando) {
        var c = plugin.getCommand(comando.split(" ")[0]);
        return c == null ? null : c.getPermission();
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return i;
    }

    /** Uma vez por jogador: avisa que o /rpg existe. */
    @EventHandler
    public void aoEntrar(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var k = new org.bukkit.NamespacedKey(plugin, "dica_rpg");
        if (p.getPersistentDataContainer().has(k)) return;
        p.getPersistentDataContainer().set(k, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            p.sendMessage(Component.text("⚔ Dica: ", COR, TextDecoration.BOLD).append(Component.text("use ", NamedTextColor.GRAY))
                    .append(Component.text("/rpg", COR).clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/rpg")))
                    .append(Component.text(" para abrir o menu com tudo do servidor.", NamedTextColor.GRAY)));
        }, 100L);
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
        String cmd = t.comandos.get(e.getRawSlot());
        if (cmd == null) return;
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        p.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> p.performCommand(cmd));
    }
}
