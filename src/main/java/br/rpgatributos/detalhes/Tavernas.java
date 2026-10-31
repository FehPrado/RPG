package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.13: Bares e tavernas. Uma taverna é a obra Taverna de uma colônia ou qualquer lugar com uma
 * placa escrita <b>[Taverna]</b> na primeira linha (vale 10 blocos em volta; a segunda linha é o nome).
 * <ul>
 *   <li><b>Uma rodada:</b> beber lá dentro (as bebidas do barril, mel, leite, poções) dá Descanso; se
 *       dois ou mais beberam juntos no último minuto, todo mundo ali ganha <b>Camaradagem</b>:
 *       +10% de XP por 20 minutos e Sorte.</li>
 *   <li><b>Boatos:</b> agachado, clique num barril da taverna: o taverneiro conta um boato (uma
 *       estrutura com o lugar aproximado, ou onde viram um animal lendário). Um por dia de jogo.</li>
 *   <li><b>Dados:</b> o Copo de Dados rola dois dados para todo mundo da mesa ver.</li>
 * </ul>
 */
public final class Tavernas implements Listener {

    public static final TextColor COR = TextColor.color(0xC8873A);
    private static final NamespacedKey K_DADOS = new NamespacedKey("rpgatributos", "copo_de_dados");
    private static final NamespacedKey K_BOATO_DIA = new NamespacedKey("rpgatributos", "taverna_boato_dia");
    private static final int RAIO_PLACA = 10;

    private final RPGAtributos plugin;
    private final File arquivo;
    /** Placas [Taverna]: "mundo,x,y,z" → nome. */
    private final Map<String, String> placas = new HashMap<>();
    /** Quem bebeu numa taverna e quando (para a rodada). */
    private final Map<UUID, Long> bebeu = new HashMap<>();
    /** Até quando cada um tem Camaradagem. */
    private final Map<UUID, Long> camaradagem = new HashMap<>();

    public Tavernas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "tavernas.yml");
        carregar();
    }

    private void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String k : y.getKeys(false)) placas.put(k.replace('_', ','), y.getString(k, "Taverna"));
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, String> e : placas.entrySet()) y.set(e.getKey().replace(',', '_'), e.getValue());
        try {
            y.save(arquivo);
        } catch (IOException e) {
            plugin.getLogger().warning("Não consegui salvar tavernas.yml: " + e.getMessage());
        }
    }

    private static String chave(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    /** O nome da taverna onde este lugar fica, ou null se não é taverna. */
    public String taverna(Location l) {
        for (Map.Entry<String, String> e : placas.entrySet()) {
            String[] p = e.getKey().split(",");
            if (!p[0].equals(l.getWorld().getName())) continue;
            if (Math.abs(Integer.parseInt(p[1]) - l.getBlockX()) <= RAIO_PLACA && Math.abs(Integer.parseInt(p[2]) - l.getBlockY()) <= 6
                    && Math.abs(Integer.parseInt(p[3]) - l.getBlockZ()) <= RAIO_PLACA) return e.getValue();
        }
        if (plugin.colonias() != null && plugin.colonias().naTaverna(l)) return "Taverna da colônia";
        return null;
    }

    // =====================================================================
    //  A placa [Taverna]
    // =====================================================================

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoEscreverPlaca(SignChangeEvent e) {
        Component l0 = e.line(0);
        if (l0 == null || !PlainTextComponentSerializer.plainText().serialize(l0).trim().equalsIgnoreCase("[Taverna]")) return;
        Player p = e.getPlayer();
        if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, e.getBlock())) return;
        Component l1 = e.line(1);
        String nome = l1 == null ? "" : PlainTextComponentSerializer.plainText().serialize(l1).trim();
        if (nome.isEmpty()) nome = "Taverna de " + p.getName();
        placas.put(chave(e.getBlock().getLocation()), nome);
        salvar();
        e.line(0, Component.text("[Taverna]", COR, TextDecoration.BOLD));
        p.sendMessage(Component.text("🍺 " + nome + " está aberta! Beber aqui com amigos dá Camaradagem; agachado + clique num barril: um boato.", COR));
        p.playSound(p.getLocation(), Sound.BLOCK_BARREL_OPEN, 1f, 1f);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoQuebrarPlaca(BlockBreakEvent e) {
        if (placas.remove(chave(e.getBlock().getLocation())) != null) {
            salvar();
            e.getPlayer().sendActionBar(Component.text("A taverna fechou.", COR));
        }
    }

    // =====================================================================
    //  A rodada
    // =====================================================================

    private static boolean ehBebida(ItemStack i) {
        Material m = i.getType();
        return m == Material.POTION || m == Material.HONEY_BOTTLE || m == Material.MILK_BUCKET || br.rpgatributos.vida.Bebida.de(i) != null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoBeber(PlayerItemConsumeEvent e) {
        if (!ehBebida(e.getItem())) return;
        Player p = e.getPlayer();
        String nome = taverna(p.getLocation());
        if (nome == null) return;
        long agora = System.currentTimeMillis();
        bebeu.put(p.getUniqueId(), agora);
        // Quem mais bebeu aqui no último minuto?
        List<Player> roda = new ArrayList<>();
        for (Player o : p.getWorld().getPlayers()) {
            if (o.getLocation().distanceSquared(p.getLocation()) > 20 * 20 || !nome.equals(taverna(o.getLocation()))) continue;
            if (agora - bebeu.getOrDefault(o.getUniqueId(), 0L) < 60_000L) roda.add(o);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false, true, true));
            if (roda.size() < 2) {
                p.sendActionBar(Component.text("🍺 Descanso na " + nome + ". Bebendo com alguém, vira Camaradagem.", COR));
                return;
            }
            for (Player o : roda) {
                boolean nova = camaradagem.getOrDefault(o.getUniqueId(), 0L) < agora;
                camaradagem.put(o.getUniqueId(), agora + 20 * 60_000L);
                o.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 60 * 20, 0, false, true, true));
                if (nova) {
                    o.sendMessage(Component.text("🍻 Uma rodada na " + nome + " com " + (roda.size() - 1) + " amigo(s): Camaradagem (+10% de XP e Sorte por 20 minutos).", COR));
                    o.playSound(o.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 0.8f, 1.1f);
                    plugin.titulos().registrar(o, "rodadas", 1);
                }
            }
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.6, 0), 10, 0.6, 0.3, 0.6);
        });
    }

    /** +10% de XP de atributo enquanto dura a Camaradagem. */
    public double multiplicadorXp(Player p) {
        return camaradagem.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis() ? 1.10 : 1;
    }

    // =====================================================================
    //  Boatos (agachado + clique num barril da taverna)
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoPedirBoato(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        Player p = e.getPlayer();
        if (b.getType() != Material.BARREL || !p.isSneaking()) return;
        String nome = taverna(b.getLocation());
        if (nome == null) return;
        if (plugin.ehEstacao(b)) return; // o Barril de Envelhecimento tem o menu dele
        e.setCancelled(true);
        long dia = p.getWorld().getFullTime() / 24000;
        if (p.getPersistentDataContainer().getOrDefault(K_BOATO_DIA, PersistentDataType.LONG, -1L) == dia) {
            p.sendMessage(Component.text("🍺 \"Já contei o que sei hoje. Volte amanhã, que o povo fala muito.\"", COR));
            return;
        }
        p.getPersistentDataContainer().set(K_BOATO_DIA, PersistentDataType.LONG, dia);
        p.sendMessage(Component.text("🍺 O taverneiro se debruça no balcão: ", COR).append(Component.text("\"" + boato(p) + "\"", NamedTextColor.WHITE)));
        p.playSound(b.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 0.9f);
        plugin.titulos().registrar(p, "boatos", 1);
    }

    private String boato(Player p) {
        Random r = ThreadLocalRandom.current();
        if (r.nextDouble() < 0.6 && plugin.estruturas() != null) {
            String s = plugin.estruturas().boato(p.getLocation(), r);
            if (s != null) return "Um viajante jurou ter visto " + s + ". Eu iria de dia.";
        }
        String[] gerais = {
                "Dizem que no outono os cavalos selvagens passam pelas planícies. Quem tiver uma sela, que fique de olho.",
                "Ouvi falar de uma árvore tão velha que escorre resina. Um frasco daquilo cura qualquer ressaca.",
                "Um pescador jurou que viu uma baleia brilhando no mar. Ninguém acreditou, mas ele nunca mais bebeu.",
                "As rachaduras no chão levam a um lugar fundo demais. Os Antigos cavaram lá antes de todo mundo.",
                "Quem tem uma lanterna azul e coragem pode visitar os mortos. Mas tem que voltar antes da chama apagar.",
                "Arma que derruba mil inimigos ganha nome próprio. Lâmina velha é lâmina sábia.",
                "Num eclipse, o céu abre uma lua que fecha rápido. Quem tem foguete, que corra.",
                "Ninho de abelha selvagem perto de papoula dá um mel vermelho que deixa o braço forte."};
        return gerais[r.nextInt(gerais.length)];
    }

    // =====================================================================
    //  Dados
    // =====================================================================

    public static ItemStack copoDeDados() {
        ItemStack i = new ItemStack(Material.BOWL);
        i.editMeta(m -> {
            m.itemName(Component.text("Copo de Dados", COR));
            m.lore(List.of(Component.text("Clique para rolar dois dados (quem estiver perto vê).", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(K_DADOS, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "copo_de_dados");
        });
        return i;
    }

    public void registrarReceita() {
        ShapelessRecipe r = new ShapelessRecipe(new NamespacedKey(plugin, "copo_de_dados"), copoDeDados());
        r.addIngredient(Material.BOWL);
        r.addIngredient(Material.BONE);
        r.addIngredient(Material.BONE);
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    private final Map<UUID, Long> rolou = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void aoRolar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getItem() == null || !e.getItem().getPersistentDataContainer().has(K_DADOS)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        long agora = System.currentTimeMillis();
        if (agora - rolou.getOrDefault(p.getUniqueId(), 0L) < 2000) return;
        rolou.put(p.getUniqueId(), agora);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int a = 1 + r.nextInt(6), b = 1 + r.nextInt(6);
        String[] faces = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};
        Component msg = Component.text("🎲 " + p.getName() + " rolou ", COR).append(Component.text(faces[a - 1] + " " + faces[b - 1], NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" (" + (a + b) + ")" + (a == b ? " — dupla!" : ""), a == b ? NamedTextColor.GOLD : COR));
        for (Player o : p.getWorld().getPlayers()) if (o.getLocation().distanceSquared(p.getLocation()) < 16 * 16) o.sendMessage(msg);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BAMBOO_WOOD_HIT, 1f, 1.4f);
    }
}
