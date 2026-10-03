package br.rpgatributos;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bloco especial criado por ritual (jogar itens no lugar certo): Infusor Corpóreo,
 * Forja do Ferreiro... Cuida do que é igual para todos: marca no chunk, item
 * girando em cima, nome flutuando, partículas, quebrar/explodir/pistão e clique.
 */
public abstract class Estacao implements Listener {

    private static final long JANELA_RITUAL_MS = 30_000;

    protected final RPGAtributos plugin;
    private final MarcadorDeBlocos marcas;
    private final NamespacedKey kDisplay;
    /** Estações em chunks carregados → [item girando, nome]. */
    private final Map<Location, UUID[]> carregados = new HashMap<>();
    /** Itens do ritual jogados por jogadores → até quando contam. */
    private final Map<UUID, Long> rituais = new HashMap<>();
    private int ciclo;

    protected Estacao(RPGAtributos plugin, String marcador, String chaveDisplay) {
        this.plugin = plugin;
        this.marcas = new MarcadorDeBlocos(plugin, marcador);
        this.kDisplay = new NamespacedKey(plugin, chaveDisplay);
    }

    // =====================================================================
    //  O que cada estação define
    // =====================================================================

    /** Blocos que podem ser essa estação (ex.: caldeirões). */
    protected abstract boolean blocoValido(Material m);

    /** Itens que participam do ritual. */
    protected abstract boolean itemDoRitual(Material m);

    /** Bloco em que o item está fazendo o ritual, ou null se não está no lugar certo. */
    protected abstract Block blocoDoRitual(Item item);

    /** Se os itens jogados bastam, consome o necessário e retorna true. */
    protected abstract boolean consumirRitual(Block bloco, List<Item> itens);

    protected abstract ItemStack itemFlutuante();
    protected abstract float escalaItem();
    /** Altura do item flutuante acima do bloco. */
    protected abstract double alturaItem();
    protected abstract Component nome();

    protected abstract void aoCriar(Block b, Player quem);
    protected abstract void particulas(Location centro, int ciclo);
    /** Dropa o que volta para o jogador quando a estação é quebrada. */
    protected abstract void devolver(Location centro);
    protected abstract void abrir(Player p, Block b, boolean agachado);

    /** Chamado a cada 5 ticks, para a estação fazer algo extra. */
    protected void aoTick() { }

    /** Item girando de uma estação específica (ex.: a bandeira de cada território). */
    protected ItemStack itemFlutuante(Location bloco) { return itemFlutuante(); }

    /** Nome flutuando de uma estação específica. */
    protected Component nome(Location bloco) { return nome(); }

    /**
     * Motivo para o ritual não poder criar a estação aqui, ou null se pode.
     * Por padrão: ninguém cria estação no território dos outros.
     */
    protected String impedimento(Block b, UUID quem) {
        var t = plugin.territorios().em(b.getLocation());
        if (t == null || (quem != null && plugin.territorios().podeConstruir(quem, b.getLocation()))) return null;
        return "Esse lugar é do território de " + t.nomeDono() + ".";
    }

    // =====================================================================
    //  Básico
    // =====================================================================

    public boolean eh(Block b) {
        return b != null && blocoValido(b.getType()) && marcas.marcado(b);
    }

    /** Estações carregadas a até {@code raio} blocos. */
    public List<Location> perto(Location l, double raio) {
        List<Location> lista = new ArrayList<>();
        for (Location e : carregados.keySet()) {
            if (e.getWorld().equals(l.getWorld()) && e.clone().add(0.5, 0.5, 0.5).distanceSquared(l) <= raio * raio) {
                lista.add(e);
            }
        }
        return lista;
    }

    private static String chave(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    public void iniciar() {
        for (World w : plugin.getServer().getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) carregarChunk(c);
        }
    }

    private void carregarChunk(Chunk c) {
        for (Block b : marcas.todos(c)) carregados.putIfAbsent(b.getLocation(), new UUID[2]);
    }

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        carregarChunk(e.getChunk());
    }

    @EventHandler
    public void aoDescarregarChunk(ChunkUnloadEvent e) {
        Chunk c = e.getChunk();
        carregados.keySet().removeIf(l -> l.getWorld().equals(c.getWorld())
                && l.getBlockX() >> 4 == c.getX() && l.getBlockZ() >> 4 == c.getZ());
    }

    /** Transforma o bloco na estação. */
    public void criar(Block b, Player quem) {
        marcas.marcar(b);
        UUID[] ids = new UUID[2];
        carregados.put(b.getLocation(), ids);
        garantirDisplays(b.getLocation(), ids);
        aoCriar(b, quem);
    }

    /** Desfaz a estação; com {@code devolver}, dropa os itens do ritual. */
    public void remover(Location l, boolean devolverItens) {
        marcas.desmarcar(l.getBlock());
        UUID[] ids = carregados.remove(l);
        if (ids != null) {
            for (UUID id : ids) {
                Entity e = id == null ? null : Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
        }
        String chave = chave(l);
        for (Entity e : l.getWorld().getNearbyEntities(l.clone().add(0.5, 1.5, 0.5), 1, 1.5, 1)) {
            if (chave.equals(e.getPersistentDataContainer().get(kDisplay, PersistentDataType.STRING))) e.remove();
        }
        if (devolverItens) devolver(l.clone().add(0.5, 0.5, 0.5));
        aoRemover(l);
    }

    /** Chamado quando a estação deixa de existir (quebrada, explodida, sumiu). */
    protected void aoRemover(Location bloco) { }

    // =====================================================================
    //  Ritual
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoJogarItem(PlayerDropItemEvent e) {
        if (itemDoRitual(e.getItemDrop().getItemStack().getType())) {
            rituais.put(e.getItemDrop().getUniqueId(), System.currentTimeMillis() + JANELA_RITUAL_MS);
        }
    }

    /** Roda a cada 5 ticks. */
    public void tick() {
        ciclo++;
        if (!rituais.isEmpty()) verificarRituais();
        if (ciclo % 2 == 0) animar();
        aoTick();
    }

    private void verificarRituais() {
        long agora = System.currentTimeMillis();
        Map<Block, List<Item>> porBloco = new LinkedHashMap<>();
        Iterator<Map.Entry<UUID, Long>> it = rituais.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (!(ent instanceof Item item) || !item.isValid() || agora > en.getValue()) {
                it.remove();
                continue;
            }
            Block b = blocoDoRitual(item);
            if (b != null && !plugin.ehEstacao(b)) { // um bloco nunca vira duas estações
                // Não deixa o item voltar para o inventário de quem está do lado antes do ritual completar.
                if (item.getPickupDelay() < 40) item.setPickupDelay(40);
                porBloco.computeIfAbsent(b, k -> new ArrayList<>()).add(item);
            }
        }
        for (Map.Entry<Block, List<Item>> en : porBloco.entrySet()) {
            List<Item> itens = en.getValue();
            UUID quem = null;
            for (Item i : itens) if (i.getThrower() != null) { quem = i.getThrower(); break; }
            String erro = impedimento(en.getKey(), quem);
            if (erro != null) {
                // Os itens ficam no chão como itens comuns (sem tentar de novo a cada tick).
                for (Item i : itens) {
                    rituais.remove(i.getUniqueId());
                    i.setPickupDelay(10);
                }
                Player p = quem == null ? null : Bukkit.getPlayer(quem);
                if (p != null) {
                    p.sendMessage(Component.text("✖ " + erro, NamedTextColor.RED));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                }
                continue;
            }
            if (!consumirRitual(en.getKey(), itens)) continue;
            criar(en.getKey(), quem == null ? null : Bukkit.getPlayer(quem));
            return; // um ritual por vez
        }
    }

    /** Tira até {@code qtd} unidades do item jogado. @return quantas tirou. */
    protected int tirar(Item item, int qtd) {
        if (!item.isValid() || qtd <= 0) return 0;
        ItemStack s = item.getItemStack();
        int tirou = Math.min(qtd, s.getAmount());
        if (tirou >= s.getAmount()) {
            rituais.remove(item.getUniqueId());
            item.remove();
        } else {
            s.setAmount(s.getAmount() - tirou);
            item.setItemStack(s);
        }
        return tirou;
    }

    /** Soma quantas unidades de um material há nos itens jogados. */
    protected static int contar(List<Item> itens, Material m) {
        int n = 0;
        for (Item i : itens) if (i.isValid() && i.getItemStack().getType() == m) n += i.getItemStack().getAmount();
        return n;
    }

    /** Tira {@code qtd} unidades de um material, espalhadas pelos itens jogados. */
    protected void tirar(List<Item> itens, Material m, int qtd) {
        for (Item i : itens) {
            if (qtd <= 0) return;
            if (i.isValid() && i.getItemStack().getType() == m) qtd -= tirar(i, qtd);
        }
    }

    // =====================================================================
    //  Item girando e nome flutuando
    // =====================================================================

    private void garantirDisplays(Location bloco, UUID[] ids) {
        ItemDisplay item = ids[0] != null && Bukkit.getEntity(ids[0]) instanceof ItemDisplay d && d.isValid() ? d : null;
        TextDisplay nome = ids[1] != null && Bukkit.getEntity(ids[1]) instanceof TextDisplay d && d.isValid() ? d : null;
        if (item != null && nome != null) return;
        // As entidades do chunk carregam depois do chunk: sem esperar, nasceria um item repetido.
        if (!bloco.getChunk().isEntitiesLoaded()) return;

        String chave = chave(bloco);
        Location centro = bloco.clone().add(0.5, 0, 0.5);
        // Procura os que já existem (depois de reiniciar o servidor) e apaga repetidos.
        for (Entity e : bloco.getWorld().getNearbyEntities(centro.clone().add(0, 1.5, 0), 1, 1.5, 1)) {
            if (!chave.equals(e.getPersistentDataContainer().get(kDisplay, PersistentDataType.STRING))) continue;
            if (e instanceof ItemDisplay d) {
                if (item == null) item = d;
                else if (d != item) d.remove();
            } else if (e instanceof TextDisplay d) {
                if (nome == null) nome = d;
                else if (d != nome) d.remove();
            }
        }
        if (item == null) {
            float escala = escalaItem();
            ItemStack girando = itemFlutuante(bloco);
            item = bloco.getWorld().spawn(centro.clone().add(0, alturaItem(), 0), ItemDisplay.class, d -> {
                d.setItemStack(girando);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(escala, escala, escala), new AxisAngle4f()));
                d.setTeleportDuration(10);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(true);
                d.getPersistentDataContainer().set(kDisplay, PersistentDataType.STRING, chave);
            });
        }
        if (nome == null) {
            Component texto = nome(bloco);
            nome = bloco.getWorld().spawn(centro.clone().add(0, alturaItem() + 0.7, 0), TextDisplay.class, d -> {
                d.text(texto);
                d.setBillboard(Display.Billboard.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                d.setPersistent(true);
                d.getPersistentDataContainer().set(kDisplay, PersistentDataType.STRING, chave);
            });
        }
        ids[0] = item.getUniqueId();
        ids[1] = nome.getUniqueId();
    }

    /** Gira o item e solta partículas. Desfaz estações cujo bloco sumiu. */
    private void animar() {
        List<Location> sumiram = new ArrayList<>();
        for (Map.Entry<Location, UUID[]> en : carregados.entrySet()) {
            Location l = en.getKey();
            if (!l.isChunkLoaded()) continue;
            if (!blocoValido(l.getBlock().getType())) {
                sumiram.add(l);
                continue;
            }
            Location centro = l.clone().add(0.5, alturaItem(), 0.5);
            boolean alguemPerto = false;
            for (Player p : l.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(centro) < 32 * 32) { alguemPerto = true; break; }
            }
            if (!alguemPerto) continue;
            garantirDisplays(l, en.getValue());
            UUID idItem = en.getValue()[0];
            if (idItem != null && Bukkit.getEntity(idItem) instanceof ItemDisplay d) {
                Location pos = d.getLocation();
                pos.setYaw(pos.getYaw() + 18);
                d.teleport(pos);
            }
            particulas(centro, ciclo);
        }
        for (Location l : sumiram) remover(l, false);
    }

    // =====================================================================
    //  Eventos do bloco
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarEstacao(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !eh(e.getClickedBlock())) return;
        e.setCancelled(true); // não deixa usar o bloco do jeito normal
        if (e.getHand() != EquipmentSlot.HAND) return;
        // Visitantes de um território também usam as estações (a Cozinha e o Altar têm regras próprias).
        Player p = e.getPlayer();
        abrir(p, e.getClickedBlock(), p.isSneaking());
    }

    /** Recria o item e o nome flutuando (ex.: quando a bandeira do território muda). */
    public void atualizarDisplays(Location l) {
        UUID[] ids = carregados.get(l);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity e = id == null ? null : Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        String chave = chave(l);
        for (Entity e : l.getWorld().getNearbyEntities(l.clone().add(0.5, 1.5, 0.5), 1, 1.5, 1)) {
            if (chave.equals(e.getPersistentDataContainer().get(kDisplay, PersistentDataType.STRING))) e.remove();
        }
        ids[0] = null;
        ids[1] = null;
        garantirDisplays(l, ids);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrarEstacao(BlockBreakEvent e) {
        if (eh(e.getBlock())) remover(e.getBlock().getLocation(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirEstacao(EntityExplodeEvent e) {
        e.blockList().removeIf(this::eh);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoExplodirBlocoEstacao(BlockExplodeEvent e) {
        e.blockList().removeIf(this::eh);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoEmpurrarEstacao(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (eh(b)) { e.setCancelled(true); return; }
    }

    @EventHandler(ignoreCancelled = true)
    public void aoPuxarEstacao(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (eh(b)) { e.setCancelled(true); return; }
    }
}
