package br.rpgatributos.domador;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Estábulo (2.50): um fardo de feno com uma sela e um laço jogados em cima. Guarda até 4 montarias
 * (cavalo, burro, mula, lhama, camelo, porco ou lavagante selados, e companheiros do Domador): elas
 * saem do mundo e voltam iguais (nome, linhagem, componentes, alforje) quando você as chama, perto
 * do estábulo. Só quem guardou chama. Quebrar o estábulo solta todas ali mesmo.
 */
public final class Estabulos extends Estacao {

    public static final TextColor COR = TextColor.color(0xC8A165);
    private static final int VAGAS = 4;
    private static final int[] S_VAGAS = {10, 12, 14, 16};
    private static final int S_GUARDAR = 22;

    /** Uma montaria guardada. */
    private record Vaga(UUID dono, String nome, EntityType tipo, String dados) { }

    private final File arquivo;
    private final Map<String, List<Vaga>> vagas = new HashMap<>();

    public Estabulos(RPGAtributos plugin) {
        super(plugin, "estabulos", "estabulo_display");
        this.arquivo = new File(plugin.getDataFolder(), "estabulos.yml");
        carregar();
    }

    // =====================================================================
    //  Estação
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.HAY_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.SADDLE || m == Material.LEAD;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.HAY_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.SADDLE) < 1 || contar(itens, Material.LEAD) < 1) return false;
        tirar(itens, Material.SADDLE, 1);
        tirar(itens, Material.LEAD, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.SADDLE);
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
        return Component.text("🐎 Estábulo", COR).append(Component.newline())
                .append(Component.text("guarda até " + VAGAS + " montarias", NamedTextColor.GRAY));
    }

    @Override
    protected Component nome(Location bloco) {
        int n = vagas.getOrDefault(chave(bloco), List.of()).size();
        return Component.text("🐎 Estábulo", COR).append(Component.newline())
                .append(Component.text(n + "/" + VAGAS + " montarias", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.BLOCK, c, 25, 0.3, 0.2, 0.3, Material.HAY_BLOCK.createBlockData());
        c.getWorld().playSound(c, Sound.ENTITY_HORSE_SADDLE, 1f, 1f);
        c.getWorld().playSound(c, Sound.ENTITY_HORSE_AMBIENT, 0.8f, 1f);
        if (quem != null) {
            quem.sendMessage(Component.text("🐎 Estábulo pronto! Clique nele para guardar a montaria em que você está", COR));
            quem.sendMessage(Component.text("  (ou a sua mais perto) e para chamar as que estão guardadas. Até " + VAGAS + " por estábulo.", NamedTextColor.GRAY));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 20 == 0) centro.getWorld().spawnParticle(Particle.BLOCK, centro.clone().subtract(0, 0.5, 0), 2, 0.2, 0.05, 0.2, Material.HAY_BLOCK.createBlockData());
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.SADDLE));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.LEAD));
    }

    /** O estábulo deixou de existir: todas as montarias saem ali mesmo. */
    @Override
    protected void aoRemover(Location bloco) {
        List<Vaga> l = vagas.remove(chave(bloco));
        if (l == null) return;
        for (Vaga v : l) soltar(v, bloco.clone().add(0.5, 1, 0.5));
        salvar();
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        Location bloco;
        @Override public Inventory getInventory() { return inv; }
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.bloco = b.getLocation();
        t.inv = Bukkit.createInventory(t, 27, Component.text("🐎 Estábulo"));
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        List<Vaga> l = vagas.getOrDefault(chave(t.bloco), List.of());
        for (int i = 0; i < VAGAS; i++) {
            if (i < l.size()) {
                Vaga v = l.get(i);
                boolean meu = v.dono.equals(p.getUniqueId());
                OfflineNome dono = new OfflineNome(v.dono);
                t.inv.setItem(S_VAGAS[i], item(ovo(v.tipo), Component.text(v.nome, COR, TextDecoration.BOLD), List.of(
                        Component.text(nomeTipo(v.tipo), NamedTextColor.GRAY),
                        Component.text("De " + dono.nome(), NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text(meu ? "» Clique para chamar" : "Só quem guardou pode chamar.", meu ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY))));
            } else {
                t.inv.setItem(S_VAGAS[i], item(Material.HAY_BLOCK, Component.text("Baia vazia", NamedTextColor.GRAY), List.of()));
            }
        }
        t.inv.setItem(S_GUARDAR, item(Material.SADDLE, Component.text("Guardar montaria", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text("Guarda a montaria em que você está,", NamedTextColor.GRAY),
                Component.text("ou a sua mais perto (até 10 blocos).", NamedTextColor.GRAY),
                Component.text("Ela sai do mundo e volta igual.", NamedTextColor.DARK_GRAY))));
        p.openInventory(t.inv);
    }

    private record OfflineNome(UUID id) {
        String nome() {
            String n = Bukkit.getOfflinePlayer(id).getName();
            return n == null ? "?" : n;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory() || !eh(t.bloco.getBlock())) return;
        int slot = e.getRawSlot();
        if (slot == S_GUARDAR) {
            guardar(p, t.bloco);
            p.closeInventory();
            return;
        }
        for (int i = 0; i < S_VAGAS.length; i++) {
            if (S_VAGAS[i] != slot) continue;
            List<Vaga> l = vagas.get(chave(t.bloco));
            if (l == null || i >= l.size()) return;
            Vaga v = l.get(i);
            if (!v.dono.equals(p.getUniqueId()) && !p.hasPermission("rpg.admin")) {
                p.sendActionBar(Component.text("Só quem guardou pode chamar essa montaria.", NamedTextColor.RED));
                return;
            }
            l.remove(i);
            salvar();
            Entity saiu = soltar(v, lugarDeSaida(t.bloco.getBlock()));
            atualizarDisplays(t.bloco);
            p.closeInventory();
            if (saiu != null) p.sendActionBar(Component.text("🐎 " + v.nome + " saiu do estábulo.", COR));
            return;
        }
    }

    // =====================================================================
    //  Guardar e soltar
    // =====================================================================

    private static boolean montaria(Entity e) {
        if (e.getPersistentDataContainer().has(Lendaria.CHAVE)) return false; // as lendárias têm vida própria
        return e instanceof AbstractHorse || (e instanceof Pig pig && pig.hasSaddle()) || (e instanceof Strider s && s.hasSaddle());
    }

    private static boolean minha(Entity e, Player p) {
        UUID d = Companheiros.dono(e);
        if (d != null) return d.equals(p.getUniqueId());
        if (e instanceof AbstractHorse h) return h.getOwner() != null && h.getOwner().getUniqueId().equals(p.getUniqueId());
        return true; // porco e lavagante selados não têm dono no jogo
    }

    private void guardar(Player p, Location bloco) {
        List<Vaga> l = vagas.computeIfAbsent(chave(bloco), k -> new ArrayList<>());
        if (l.size() >= VAGAS) {
            p.sendActionBar(Component.text("O estábulo está cheio (" + VAGAS + " baias).", NamedTextColor.RED));
            return;
        }
        Entity alvo = p.getVehicle();
        if (alvo == null || !montaria(alvo) || !minha(alvo, p)) {
            alvo = null;
            double melhor = 100;
            for (Entity e : p.getNearbyEntities(10, 5, 10)) {
                if (!montaria(e) || !minha(e, p) || !e.getPassengers().isEmpty() && !e.getPassengers().contains(p)) continue;
                double d = e.getLocation().distanceSquared(p.getLocation());
                if (d < melhor) {
                    melhor = d;
                    alvo = e;
                }
            }
        }
        if (!(alvo instanceof LivingEntity vivo)) {
            p.sendActionBar(Component.text("Nenhuma montaria sua por perto (selada ou domada).", NamedTextColor.GRAY));
            return;
        }
        if (vivo.getLocation().distanceSquared(bloco.clone().add(0.5, 0.5, 0.5)) > 20 * 20) {
            p.sendActionBar(Component.text("Traga a montaria até perto do estábulo.", NamedTextColor.GRAY));
            return;
        }
        vivo.eject();
        if (Companheiros.eh(vivo)) plugin.companheiros().antesDeGuardar(vivo);
        String dados = Base64.getEncoder().encodeToString(Bukkit.getUnsafe().serializeEntity(vivo));
        String nome = vivo.customName() != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(vivo.customName())
                : nomeTipo(vivo.getType());
        l.add(new Vaga(p.getUniqueId(), nome, vivo.getType(), dados));
        salvar();
        Location c = vivo.getLocation().add(0, 1, 0);
        vivo.getWorld().spawnParticle(Particle.CLOUD, c, 15, 0.4, 0.5, 0.4, 0.02);
        vivo.getWorld().playSound(c, Sound.ENTITY_HORSE_GALLOP, 1f, 1f);
        vivo.remove();
        atualizarDisplays(bloco);
        p.sendMessage(Component.text("🐎 " + nome + " foi guardado no estábulo.", COR));
    }

    private Entity soltar(Vaga v, Location onde) {
        World w = onde.getWorld();
        Entity e;
        try {
            byte[] b = Base64.getDecoder().decode(v.dados);
            e = Bukkit.getUnsafe().deserializeEntity(b, w, Bukkit.getEntity(uuidDe(b, w)) == null);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Estábulo: não consegui soltar " + v.nome + ": " + ex.getMessage());
            return null;
        }
        if (!e.spawnAt(onde)) return null;
        w.spawnParticle(Particle.CLOUD, onde.clone().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.02);
        w.playSound(onde, Sound.ENTITY_HORSE_AMBIENT, 1f, 1f);
        return e;
    }

    /** O UUID gravado na montaria (para não repetir um que já existe no mundo). */
    private static UUID uuidDe(byte[] b, World w) {
        try {
            return Bukkit.getUnsafe().deserializeEntity(b, w, true).getUniqueId();
        } catch (RuntimeException ex) {
            return UUID.randomUUID();
        }
    }

    /** Um lugar livre do lado do estábulo (2 de altura), ou em cima dele. */
    private static Location lugarDeSaida(Block b) {
        for (BlockFace f : new BlockFace[]{BlockFace.SOUTH, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST}) {
            Block lado = b.getRelative(f);
            if (lado.isPassable() && lado.getRelative(BlockFace.UP).isPassable() && !lado.getRelative(BlockFace.DOWN).isPassable()) {
                return lado.getLocation().add(0.5, 0, 0.5);
            }
        }
        return b.getLocation().add(0.5, 1, 0.5);
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    private static String chave(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        var s = y.getConfigurationSection("estabulos");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            List<Vaga> l = new ArrayList<>();
            for (Map<?, ?> m : s.getMapList(k)) {
                try {
                    l.add(new Vaga(UUID.fromString(String.valueOf(m.get("dono"))), String.valueOf(m.get("nome")),
                            EntityType.valueOf(String.valueOf(m.get("tipo"))), String.valueOf(m.get("dados"))));
                } catch (IllegalArgumentException ignorado) {
                    // baia corrompida
                }
            }
            if (!l.isEmpty()) vagas.put(k.replace(';', '.'), l);
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        vagas.forEach((k, l) -> {
            List<Map<String, Object>> lista = new ArrayList<>();
            for (Vaga v : l) lista.add(Map.of("dono", v.dono.toString(), "nome", v.nome, "tipo", v.tipo.name(), "dados", v.dados));
            if (!lista.isEmpty()) y.set("estabulos." + k.replace('.', ';'), lista);
        });
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar estabulos.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Ajuda
    // =====================================================================

    private static Material ovo(EntityType t) {
        Material m = Material.matchMaterial(t.name() + "_SPAWN_EGG");
        return m == null ? Material.SADDLE : m;
    }

    static String nomeTipo(EntityType t) {
        return switch (t) {
            case HORSE -> "Cavalo";
            case DONKEY -> "Burro";
            case MULE -> "Mula";
            case LLAMA, TRADER_LLAMA -> "Lhama";
            case CAMEL -> "Camelo";
            case SKELETON_HORSE -> "Cavalo-Esqueleto";
            case ZOMBIE_HORSE -> "Cavalo-Zumbi";
            case PIG -> "Porco";
            case STRIDER -> "Lavagante";
            default -> t.name();
        };
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
