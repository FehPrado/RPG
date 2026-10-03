package br.rpgatributos.viagem;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pedras de Viagem (waystones): pedra entalhada de ardósia com 2 pérolas do ender e 4 fragmentos
 * de ametista jogados em cima. Cada jogador descobre as pedras que visita e viaja entre elas
 * pagando níveis de XP conforme a distância.
 */
public final class PedrasDeViagem extends Estacao {

    public static final TextColor COR = TextColor.color(0x9C7CFF);
    private static final NamespacedKey K_CONHECIDAS = new NamespacedKey("rpgatributos", "pedras_conhecidas");
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int S_INFO = 4, S_NOME = 45, S_PUBLICA = 46, S_FECHAR = 49;

    /** Uma pedra registrada. */
    public static final class Pedra {
        final String id;
        final String mundo;
        final int x, y, z;
        String nome;
        final UUID dono;
        final String nomeDono;
        boolean publica;

        Pedra(String id, String mundo, int x, int y, int z, String nome, UUID dono, String nomeDono, boolean publica) {
            this.id = id;
            this.mundo = mundo;
            this.x = x;
            this.y = y;
            this.z = z;
            this.nome = nome;
            this.dono = dono;
            this.nomeDono = nomeDono;
            this.publica = publica;
        }

        Location bloco() {
            World w = Bukkit.getWorld(mundo);
            return w == null ? null : new Location(w, x, y, z);
        }
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Pedra aqui;
        final Map<Integer, Pedra> destinos = new HashMap<>();

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final File arquivo;
    private final Map<String, Pedra> pedras = new LinkedHashMap<>();
    private final Map<UUID, BukkitTask> viagens = new HashMap<>();

    public PedrasDeViagem(RPGAtributos plugin) {
        super(plugin, "pedras_viagem", "pedra_viagem_display");
        this.arquivo = new File(plugin.getDataFolder(), "pedras.yml");
    }

    private static String chave(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private Pedra em(Location bloco) {
        return pedras.get(chave(bloco));
    }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CHISELED_DEEPSLATE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.ENDER_PEARL || m == Material.AMETHYST_SHARD;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.CHISELED_DEEPSLATE ? abaixo : null;
    }

    @Override
    protected String impedimento(Block b, UUID quem) {
        if (plugin.masmorras().ehMundo(b.getWorld())) return "As Pedras de Viagem não funcionam aqui.";
        return super.impedimento(b, quem);
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.ENDER_PEARL) < 2 || contar(itens, Material.AMETHYST_SHARD) < 4) return false;
        tirar(itens, Material.ENDER_PEARL, 2);
        tirar(itens, Material.AMETHYST_SHARD, 4);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENDER_PEARL);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("✧ Pedra de Viagem ✧", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        Pedra p = em(bloco);
        if (p == null) return nome();
        return Component.text("✧ " + p.nome + " ✧", COR).append(Component.newline())
                .append(Component.text("Pedra de Viagem" + (p.publica ? "" : " (privada)"), NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location l = b.getLocation();
        String nomeDono = quem == null ? "Servidor" : quem.getName();
        int n = 1;
        for (Pedra p : pedras.values()) if (p.nomeDono.equals(nomeDono)) n++;
        Pedra nova = new Pedra(UUID.randomUUID().toString().substring(0, 8), l.getWorld().getName(), l.getBlockX(), l.getBlockY(),
                l.getBlockZ(), "Pedra de " + nomeDono + (n > 1 ? " " + n : ""), quem == null ? new UUID(0, 0) : quem.getUniqueId(), nomeDono, true);
        pedras.put(chave(l), nova);
        salvar();
        atualizarDisplays(l);
        World w = b.getWorld();
        w.spawnParticle(Particle.REVERSE_PORTAL, l.clone().add(0.5, 1.2, 0.5), 60, 0.4, 0.8, 0.4, 0.05);
        w.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
        if (quem == null) return;
        conhecer(quem, nova);
        quem.showTitle(Title.title(Component.text("✧ Pedra de Viagem ✧", COR, TextDecoration.BOLD),
                Component.text("Viaje entre as pedras que você já visitou", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.PORTAL, centro, 4, 0.2, 0.3, 0.2, 0.3);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.ENDER_PEARL, 2));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.AMETHYST_SHARD, 4));
    }

    @Override
    protected void aoRemover(Location bloco) {
        if (pedras.remove(chave(bloco)) != null) salvar();
    }

    // =====================================================================
    //  Descobrir e viajar
    // =====================================================================

    private boolean conhece(Player p, Pedra pedra) {
        String s = p.getPersistentDataContainer().getOrDefault(K_CONHECIDAS, PersistentDataType.STRING, "");
        return List.of(s.split(",")).contains(pedra.id);
    }

    private void conhecer(Player p, Pedra pedra) {
        if (conhece(p, pedra)) return;
        String s = p.getPersistentDataContainer().getOrDefault(K_CONHECIDAS, PersistentDataType.STRING, "");
        p.getPersistentDataContainer().set(K_CONHECIDAS, PersistentDataType.STRING, s.isEmpty() ? pedra.id : s + "," + pedra.id);
        plugin.titulos().registrar(p, "pedras", 1);
    }

    /** Pode usar a pedra? Pública: todos. Privada: o dono e a party dele. */
    private boolean pode(Player p, Pedra pedra) {
        return pedra.publica || pedra.dono.equals(p.getUniqueId()) || plugin.parties().mesmaParty(pedra.dono, p.getUniqueId());
    }

    public int custo(Player p, Pedra destino) {
        Location l = destino.bloco();
        if (l == null) return 0;
        int porNivel = Math.max(1, plugin.settings().viaBlocosPorNivel);
        if (!l.getWorld().equals(p.getWorld())) return plugin.settings().viaNiveisOutroMundo;
        return Math.max(1, (int) Math.ceil(l.distance(p.getLocation()) / porNivel));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Pedra aqui = em(b.getLocation());
        if (aqui == null) return;
        if (!pode(p, aqui)) {
            p.sendActionBar(Component.text("✖ Esta pedra é privada.", NamedTextColor.RED));
            return;
        }
        if (!conhece(p, aqui)) {
            conhecer(p, aqui);
            p.sendMessage(Component.text("✧ Você descobriu a pedra \"" + aqui.nome + "\". Agora dá para viajar até ela.", COR));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        }
        Tela t = new Tela();
        t.aqui = aqui;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✧ " + aqui.nome));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        t.destinos.clear();
        boolean dono = t.aqui.dono.equals(p.getUniqueId()) || plugin.territorios().ignorando(p);
        inv.setItem(S_INFO, item(Material.ENDER_EYE, Component.text("✧ " + t.aqui.nome, COR, TextDecoration.BOLD), List.of(
                Component.text("Dona: " + t.aqui.nomeDono + (t.aqui.publica ? " • pública" : " • privada"), NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Clique numa pedra para viajar. Custa níveis", NamedTextColor.GRAY),
                Component.text("de XP: 1 a cada " + plugin.settings().viaBlocosPorNivel + " blocos (outro mundo: "
                        + plugin.settings().viaNiveisOutroMundo + ").", NamedTextColor.GRAY),
                Component.text("Fique parado " + plugin.settings().viaEsperaSeg + " s; tomar dano cancela.", NamedTextColor.DARK_GRAY))));
        int i = 0;
        for (Pedra destino : pedras.values()) {
            if (i >= SLOTS.length) break;
            if (destino == t.aqui || !conhece(p, destino) || !pode(p, destino)) continue;
            Location l = destino.bloco();
            if (l == null) continue;
            int custo = custo(p, destino);
            boolean pago = p.getGameMode() == GameMode.CREATIVE || p.getLevel() >= custo;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(destino.mundo + " • " + destino.x + " " + destino.y + " " + destino.z, NamedTextColor.GRAY));
            if (l.getWorld().equals(p.getWorld())) lore.add(Component.text("A " + Math.round(l.distance(p.getLocation())) + " blocos", NamedTextColor.GRAY));
            lore.add(Component.text("Custo: " + custo + " nível(is) de XP", pago ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text(pago ? "» Clique para viajar" : "XP insuficiente.", pago ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            inv.setItem(SLOTS[i], item(destino.publica ? Material.ENDER_PEARL : Material.ENDER_EYE,
                    Component.text("✧ " + destino.nome, COR, TextDecoration.BOLD), lore));
            t.destinos.put(SLOTS[i], destino);
            i++;
        }
        if (i == 0) {
            inv.setItem(22, item(Material.OAK_SIGN, Component.text("Nenhum outro destino conhecido", NamedTextColor.GRAY), List.of(
                    Component.text("Clique em outras Pedras de Viagem", NamedTextColor.DARK_GRAY),
                    Component.text("pelo mundo para descobri-las.", NamedTextColor.DARK_GRAY))));
        }
        if (dono) {
            inv.setItem(S_NOME, item(Material.NAME_TAG, Component.text("Renomear", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                    Component.text("Segure uma etiqueta de nome com o nome", NamedTextColor.GRAY),
                    Component.text("novo (feita na bigorna) e clique aqui.", NamedTextColor.GRAY))));
            inv.setItem(S_PUBLICA, item(t.aqui.publica ? Material.LIME_DYE : Material.GRAY_DYE,
                    Component.text(t.aqui.publica ? "Pública" : "Privada", t.aqui.publica ? NamedTextColor.GREEN : NamedTextColor.GRAY, TextDecoration.BOLD),
                    List.of(Component.text(t.aqui.publica ? "Qualquer um pode descobrir e usar." : "Só você e a sua party.", NamedTextColor.GRAY),
                            Component.text("» Clique para mudar", NamedTextColor.YELLOW))));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int k = 0; k < inv.getSize(); k++) if (inv.getItem(k) == null) inv.setItem(k, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        boolean dono = t.aqui.dono.equals(p.getUniqueId()) || plugin.territorios().ignorando(p);
        if (slot == S_NOME && dono) {
            ItemStack mao = p.getInventory().getItemInMainHand();
            if (mao.getType() != Material.NAME_TAG || mao.getItemMeta().customName() == null) {
                erro(p, "Segure uma etiqueta de nome renomeada na bigorna.");
                return;
            }
            String novo = PlainTextComponentSerializer.plainText().serialize(mao.getItemMeta().customName()).trim();
            if (novo.isEmpty() || novo.length() > 32) { erro(p, "Nome inválido (até 32 letras)."); return; }
            t.aqui.nome = novo;
            salvar();
            Location l = t.aqui.bloco();
            if (l != null) atualizarDisplays(l);
            p.sendMessage(Component.text("✧ A pedra agora se chama \"" + novo + "\".", COR));
            p.closeInventory();
            return;
        }
        if (slot == S_PUBLICA && dono) {
            t.aqui.publica = !t.aqui.publica;
            salvar();
            Location l = t.aqui.bloco();
            if (l != null) atualizarDisplays(l);
            desenhar(p, t);
            return;
        }
        Pedra destino = t.destinos.get(slot);
        if (destino == null) return;
        p.closeInventory();
        viajar(p, t.aqui, destino);
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private void viajar(Player p, Pedra de, Pedra destino) {
        Location alvo = chegada(destino);
        if (alvo == null) { erro(p, "Essa pedra está bloqueada ou o mundo dela não existe."); return; }
        int custo = custo(p, destino);
        if (p.getGameMode() != GameMode.CREATIVE && p.getLevel() < custo) { erro(p, "Você precisa de " + custo + " níveis de XP."); return; }
        cancelar(p.getUniqueId());
        Location inicio = p.getLocation();
        int[] falta = {plugin.settings().viaEsperaSeg * 4};
        BukkitTask t = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !p.getWorld().equals(inicio.getWorld()) || p.getLocation().distanceSquared(inicio) > 1) {
                cancelar(p.getUniqueId());
                if (p.isOnline()) p.sendActionBar(Component.text("✖ Viagem cancelada (você se mexeu).", NamedTextColor.RED));
                return;
            }
            p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 20, 0.4, 0.8, 0.4, 0.5);
            if (falta[0] % 4 == 0) {
                p.sendActionBar(Component.text("✧ Viajando para " + destino.nome + " em " + (falta[0] / 4) + "...", COR));
                p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.3f, 1.8f);
            }
            if (--falta[0] > 0) return;
            cancelar(p.getUniqueId());
            Location agora = chegada(destino);
            if (agora == null) { erro(p, "A pedra de destino sumiu."); return; }
            if (p.getGameMode() != GameMode.CREATIVE) {
                if (p.getLevel() < custo) { erro(p, "XP insuficiente."); return; }
                p.setLevel(p.getLevel() - custo);
            }
            p.teleport(agora);
            p.getWorld().playSound(agora, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, agora.clone().add(0, 1, 0), 50, 0.4, 0.8, 0.4, 0.05);
            plugin.titulos().registrar(p, "viagens", 1);
        }, 5L, 5L);
        viagens.put(p.getUniqueId(), t);
    }

    /** Um lugar livre do lado da pedra (ou em cima dela). */
    private static Location chegada(Pedra pedra) {
        Location b = pedra.bloco();
        if (b == null) return null;
        Block pedraBloco = b.getBlock();
        for (BlockFace f : new BlockFace[]{BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST}) {
            for (int dy = 0; dy >= -1; dy--) {
                Block pe = pedraBloco.getRelative(f).getRelative(0, dy, 0);
                if (livre(pe)) return olhandoPara(pe, b);
            }
        }
        Block cima = pedraBloco.getRelative(BlockFace.UP);
        return livre(cima) ? olhandoPara(cima, b) : null;
    }

    /** Dá para ficar em pé aqui (pés e cabeça livres, chão firme)? */
    private static boolean livre(Block pe) {
        return pe.isPassable() && !pe.isLiquid() && pe.getRelative(BlockFace.UP).isPassable()
                && pe.getRelative(BlockFace.DOWN).getType().isSolid();
    }

    private static Location olhandoPara(Block pe, Location alvo) {
        Location l = pe.getLocation().add(0.5, 0, 0.5);
        org.bukkit.util.Vector dir = alvo.toVector().add(new org.bukkit.util.Vector(0.5, 0, 0.5)).subtract(l.toVector()).setY(0);
        if (dir.lengthSquared() > 1e-4) l.setDirection(dir);
        return l;
    }

    private void cancelar(UUID id) {
        BukkitTask t = viagens.remove(id);
        if (t != null) t.cancel();
    }

    @EventHandler(ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && viagens.containsKey(p.getUniqueId())) {
            cancelar(p.getUniqueId());
            p.sendActionBar(Component.text("✖ Viagem cancelada (você levou dano).", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        cancelar(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        pedras.clear();
        if (!arquivo.exists()) return;
        ConfigurationSection raiz = YamlConfiguration.loadConfiguration(arquivo).getConfigurationSection("pedras");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            ConfigurationSection s = raiz.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Pedra p = new Pedra(id, s.getString("mundo", "world"), s.getInt("x"), s.getInt("y"), s.getInt("z"),
                        s.getString("nome", "Pedra"), UUID.fromString(s.getString("dono", "")), s.getString("nome-dono", "?"),
                        s.getBoolean("publica", true));
                pedras.put(p.mundo + "," + p.x + "," + p.y + "," + p.z, p);
            } catch (IllegalArgumentException ignorado) {
                // linha estragada
            }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Pedra p : pedras.values()) {
            String b = "pedras." + p.id + ".";
            y.set(b + "mundo", p.mundo);
            y.set(b + "x", p.x);
            y.set(b + "y", p.y);
            y.set(b + "z", p.z);
            y.set(b + "nome", p.nome);
            y.set(b + "dono", p.dono.toString());
            y.set(b + "nome-dono", p.nomeDono);
            y.set(b + "publica", p.publica);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar pedras.yml: " + ex.getMessage());
        }
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
