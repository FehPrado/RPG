package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que os forjados "diferentes" fazem fora da mão: barco e carrinho guardam o item forjado
 * dentro da criatura (e devolvem ao quebrar), o carrinho anda mais rápido, quem vai dentro
 * recebe menos dano, o barco pesca melhor, o élitro dá mais impulso nos fogos e a vara de
 * pescar entra na Pesca. Também o "Reforjar": jogar um equipamento achado no mundo e 1
 * Fragmento de Forja em cima da Forja do Ferreiro faz dele um item forjado.
 */
public final class ForjadosEspeciais implements Listener {

    private static final double VELOCIDADE_CARRINHO = 0.4;
    private static final long JANELA_MS = 30_000;

    private final RPGAtributos plugin;
    private final NamespacedKey kItem;
    /** Veículo → item forjado guardado nele (cache do que está no PDC). */
    private final Map<UUID, ItemStack> veiculos = new HashMap<>();
    /** Itens jogados que podem ser um Reforjar → até quando contam. */
    private final Map<UUID, Long> jogados = new HashMap<>();

    public ForjadosEspeciais(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kItem = new NamespacedKey(plugin, "forjado_item");
    }

    private Forja forja() { return plugin.forja(); }

    /** Valor de um bônus num item forjado (0 se não for forjado). */
    public double valor(ItemStack item, Stat s) {
        if (item == null || item.isEmpty()) return 0;
        DadosForja d = forja().ler(item);
        return d == null ? 0 : forja().valor(d, item.getType(), s);
    }

    // =====================================================================
    //  Barco e carrinho
    // =====================================================================

    private ItemStack itemDo(Entity veiculo) {
        if (veiculo == null) return null;
        ItemStack c = veiculos.get(veiculo.getUniqueId());
        if (c != null) return c;
        byte[] b = veiculo.getPersistentDataContainer().get(kItem, PersistentDataType.BYTE_ARRAY);
        if (b == null) return null;
        try {
            c = ItemStack.deserializeBytes(b);
        } catch (RuntimeException ex) {
            return null;
        }
        veiculos.put(veiculo.getUniqueId(), c);
        return c;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocar(EntityPlaceEvent e) {
        Entity v = e.getEntity();
        if (!(v instanceof Boat) && !(v instanceof RideableMinecart)) return;
        Player p = e.getPlayer();
        if (p == null) return;
        ItemStack mao = p.getInventory().getItem(e.getHand() == null ? EquipmentSlot.HAND : e.getHand());
        if (mao == null || !forja().forjado(mao)) return;
        ItemStack item = mao.asOne();
        v.getPersistentDataContainer().set(kItem, PersistentDataType.BYTE_ARRAY, item.serializeAsBytes());
        veiculos.put(v.getUniqueId(), item);
        ajustar(v);
    }

    @EventHandler
    public void aoAparecer(EntityAddToWorldEvent e) {
        if (e.getEntity() instanceof Minecart m && itemDo(m) != null) ajustar(m);
    }

    private void ajustar(Entity v) {
        if (v instanceof Minecart m) m.setMaxSpeed(VELOCIDADE_CARRINHO * (1 + valor(itemDo(v), Stat.VEL_TRILHOS)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(VehicleDestroyEvent e) {
        Vehicle v = e.getVehicle();
        ItemStack item = itemDo(v);
        if (item == null) return;
        e.setCancelled(true);
        v.eject();
        if (v instanceof ChestBoat cb) {
            for (ItemStack i : cb.getInventory().getContents()) if (i != null && !i.isEmpty()) v.getWorld().dropItemNaturally(v.getLocation(), i);
            cb.getInventory().clear();
        }
        boolean criativo = e.getAttacker() instanceof Player p && p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) v.getWorld().dropItemNaturally(v.getLocation(), item);
        veiculos.remove(v.getUniqueId());
        v.remove();
    }

    /** Quem vai dentro de um barco ou carrinho forjado recebe menos dano. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoPassageiroSofrer(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.getVehicle() == null) return;
        double prot = valor(itemDo(p.getVehicle()), Stat.PROTECAO_PASSAGEIRO);
        if (prot > 0) e.setDamage(e.getDamage() * (1 - Math.min(0.6, prot)));
    }

    /** Isca mais rápida a mais (vara forjada + barco forjado). Usado pela Pesca. */
    public double iscaExtra(Player p, ItemStack vara) {
        double v = valor(vara, Stat.ISCA_RAPIDA);
        if (p.getVehicle() instanceof Boat b) v += valor(itemDo(b), Stat.PESCA_EMBARCADA);
        return Math.min(0.6, v);
    }

    /** Chance de peixe raro a mais da vara forjada. */
    public double peixeRaroExtra(ItemStack vara) {
        return valor(vara, Stat.PEIXE_RARO);
    }

    // =====================================================================
    //  Élitro
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoImpulsionar(PlayerElytraBoostEvent e) {
        Player p = e.getPlayer();
        double v = valor(p.getInventory().getChestplate(), Stat.IMPULSO);
        if (v <= 0) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (p.isGliding()) p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(1.6 * v)));
        });
    }

    // =====================================================================
    //  Reforjar: equipamento achado + Fragmento de Forja jogados na Forja
    // =====================================================================

    private boolean serveReforjar(ItemStack i) {
        return i != null && !i.isEmpty() && ((Categoria.de(i.getType()) != null && !forja().forjado(i))
                || Raro.de(i) == Raro.FRAGMENTO_DE_FORJA);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoJogar(PlayerDropItemEvent e) {
        if (serveReforjar(e.getItemDrop().getItemStack())) jogados.put(e.getItemDrop().getUniqueId(), System.currentTimeMillis() + JANELA_MS);
    }

    /** A cada 10 ticks. */
    public void tick() {
        if (jogados.isEmpty()) return;
        long agora = System.currentTimeMillis();
        Map<Block, List<Item>> porForja = new LinkedHashMap<>();
        Iterator<Map.Entry<UUID, Long>> it = jogados.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> en = it.next();
            if (!(Bukkit.getEntity(en.getKey()) instanceof Item item) || !item.isValid() || agora > en.getValue()) {
                it.remove();
                continue;
            }
            Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
            if (plugin.forjas().eh(abaixo)) porForja.computeIfAbsent(abaixo, k -> new ArrayList<>()).add(item);
        }
        for (List<Item> itens : porForja.values()) reforjar(itens);
    }

    private void reforjar(List<Item> itens) {
        Item equipamento = null, fragmento = null;
        for (Item i : itens) {
            ItemStack s = i.getItemStack();
            if (Raro.de(s) == Raro.FRAGMENTO_DE_FORJA) fragmento = i;
            else if (equipamento == null) equipamento = i;
        }
        if (equipamento == null || fragmento == null) return;
        UUID quem = equipamento.getThrower();
        Player p = quem == null ? null : Bukkit.getPlayer(quem);
        if (p == null) return;
        ItemStack frag = fragmento.getItemStack();
        if (frag.getAmount() <= 1) {
            jogados.remove(fragmento.getUniqueId());
            fragmento.remove();
        } else {
            frag.setAmount(frag.getAmount() - 1);
            fragmento.setItemStack(frag);
        }
        ItemStack base = equipamento.getItemStack();
        ItemStack feito = forja().forjar(p, base.asOne(), null);
        if (base.getAmount() > 1) {
            base.setAmount(base.getAmount() - 1);
            equipamento.setItemStack(base);
            equipamento.getWorld().dropItem(equipamento.getLocation(), feito).setPickupDelay(10);
        } else {
            equipamento.setItemStack(feito);
            equipamento.setPickupDelay(10);
            jogados.remove(equipamento.getUniqueId());
        }
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
            plugin.stats().darXp(p, Skill.FERRARIA, forja().xpFabricacao(feito.getType()));
        }
        forja().celebrar(p, equipamento.getLocation(), List.of(feito));
        forja().registrarConquistas(p, List.of(feito));
        p.sendMessage(Component.text("⚒ Reforjado! O item virou um equipamento forjado.", Skill.FERRARIA.cor()));
    }
}
