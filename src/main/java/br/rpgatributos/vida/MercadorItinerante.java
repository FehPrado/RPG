package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.detalhes.ItensDetalhes;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.masmorra.Masmorras;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Mercador Itinerante: de tempos em tempos arma a tenda perto de alguém que está explorando,
 * por 20 minutos, com 6 ofertas raras sorteadas do estoque (Fragmento de Forja, Pedra de
 * Proteção, gemas, elixires, mapas, cartas...). O servidor é avisado de onde ele está.
 */
public final class MercadorItinerante implements Listener {

    public static final TextColor COR = TextColor.color(0x26A69A);
    public static final NamespacedKey K_MERCADOR = new NamespacedKey("rpgatributos", "mercador_itinerante");

    private record Oferta(Supplier<ItemStack> item, int esmeraldas, int diamantes, int usos) { }

    private final RPGAtributos plugin;
    private UUID ativo;
    private long ate, proximo;

    public MercadorItinerante(RPGAtributos plugin) {
        this.plugin = plugin;
        this.proximo = System.currentTimeMillis() + 10 * 60_000L;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private List<Oferta> estoque(Location onde) {
        List<Oferta> l = new ArrayList<>();
        l.add(new Oferta(() -> Raro.FRAGMENTO_DE_FORJA.criar(1), 18, 0, 3));
        l.add(new Oferta(Raro.PEDRA_DE_PROTECAO::criar, 28, 0, 2));
        l.add(new Oferta(() -> plugin.mapas().criar(MapasDoTesouro.Tipo.RARO, onde), 20, 0, 2));
        l.add(new Oferta(() -> Gema.values()[rnd().nextInt(Gema.values().length)].criar(2, 1), 14, 0, 3));
        l.add(new Oferta(() -> Elixir.values()[rnd().nextInt(Elixir.values().length)].criar(Qualidade.BOA, 1.2, "Mercador"), 10, 0, 3));
        l.add(new Oferta(() -> Variedade.values()[rnd().nextInt(Variedade.values().length)].semente(2), 8, 0, 4));
        l.add(new Oferta(() -> Album.carta(Carta.values()[rnd().nextInt(Carta.values().length)], false), 6, 0, 5));
        l.add(new Oferta(() -> ItensDetalhes.ferradura(1), 6, 0, 3));
        l.add(new Oferta(() -> Mitrilo.lingote(1), 20, 1, 2));
        l.add(new Oferta(() -> Bebida.values()[rnd().nextInt(Bebida.values().length)].criar(2), 16, 0, 2));
        l.add(new Oferta(() -> Erva.values()[rnd().nextInt(Erva.values().length)].criar(3), 5, 0, 5));
        l.add(new Oferta(() -> Fruta.sortear(rnd()).muda(1), 8, 0, 3));
        // Sementes das plantações desta estação, um óleo de lâmina e peles para o curtume.
        var estacao = plugin.estacoes().atual();
        List<Cultivo> daEstacao = new java.util.ArrayList<>();
        for (Cultivo c : Cultivo.values()) if (c.cresceEm(estacao)) daEstacao.add(c);
        if (!daEstacao.isEmpty()) l.add(new Oferta(() -> daEstacao.get(rnd().nextInt(daEstacao.size())).semente(4), 4, 0, 4));
        l.add(new Oferta(() -> {
            List<br.rpgatributos.alquimia.Reagente> oleos = new java.util.ArrayList<>();
            for (var r : br.rpgatributos.alquimia.Reagente.values()) if (r.oleo()) oleos.add(r);
            return oleos.get(rnd().nextInt(oleos.size())).criar(1);
        }, 7, 0, 3));
        l.add(new Oferta(() -> {
            var peles = new br.rpgatributos.oficio.MaterialOficio[]{br.rpgatributos.oficio.MaterialOficio.PELE_DE_LOBO,
                    br.rpgatributos.oficio.MaterialOficio.PELE_DE_URSO, br.rpgatributos.oficio.MaterialOficio.PELE_DE_RAPOSA,
                    br.rpgatributos.oficio.MaterialOficio.ESCAMA_DE_GUARDIAO, br.rpgatributos.oficio.MaterialOficio.COURO_DE_HOGLIN};
            return peles[rnd().nextInt(peles.length)].criar(3);
        }, 9, 0, 3));
        if (rnd().nextDouble() < 0.25) l.add(new Oferta(() -> Raro.ESSENCIA_PRIMORDIAL.criar(1), 48, 2, 1));
        Collections.shuffle(l, rnd());
        return l.subList(0, Math.min(6, l.size()));
    }

    /** A cada minuto. */
    public void tick() {
        long agora = System.currentTimeMillis();
        if (ativo != null) {
            Entity e = Bukkit.getEntity(ativo);
            if (e == null || !e.isValid() || agora > ate) {
                if (e != null && e.isValid()) {
                    e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
                    e.remove();
                    plugin.getServer().broadcast(Component.text("✦ O Mercador Itinerante desarmou a tenda e seguiu viagem.", COR));
                }
                ativo = null;
                proximo = agora + plugin.settings().vidaMercadorMinutos * 60_000L;
            }
            return;
        }
        if (agora < proximo) return;
        if (rnd().nextDouble() < 0.5) aparecer(null);
        else proximo = agora + 15 * 60_000L;
    }

    /** Faz o mercador aparecer perto de alguém (ou de {@code perto}, se não for null). @return se apareceu. */
    public boolean aparecer(Player perto) {
        List<Player> candidatos = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() == World.Environment.NORMAL && !w.getName().equals(Masmorras.MUNDO) && p.getGameMode() != GameMode.SPECTATOR) candidatos.add(p);
        }
        Player alvo = perto != null ? perto : candidatos.isEmpty() ? null : candidatos.get(rnd().nextInt(candidatos.size()));
        if (alvo == null) return false;
        Location onde = lugar(alvo.getLocation());
        if (onde == null) return false;
        WanderingTrader t = onde.getWorld().spawn(onde, WanderingTrader.class, m -> {
            m.customName(Component.text("✦ Mercador Itinerante", COR, TextDecoration.BOLD));
            m.setCustomNameVisible(true);
            m.setInvulnerable(true);
            m.setPersistent(false);
            m.setDespawnDelay(plugin.settings().vidaMercadorDuracao * 60 * 20);
            m.getPersistentDataContainer().set(K_MERCADOR, PersistentDataType.BYTE, (byte) 1);
        });
        List<MerchantRecipe> receitas = new ArrayList<>();
        for (Oferta o : estoque(onde)) {
            MerchantRecipe r = new MerchantRecipe(o.item().get(), o.usos());
            r.addIngredient(new ItemStack(Material.EMERALD, Math.min(64, o.esmeraldas())));
            if (o.diamantes() > 0) r.addIngredient(new ItemStack(Material.DIAMOND, o.diamantes()));
            receitas.add(r);
        }
        t.setRecipes(receitas);
        ativo = t.getUniqueId();
        ate = System.currentTimeMillis() + plugin.settings().vidaMercadorDuracao * 60_000L;
        onde.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, onde.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0);
        onde.getWorld().playSound(onde, Sound.ENTITY_WANDERING_TRADER_YES, 1f, 1f);
        plugin.getServer().broadcast(Component.text("✦ O Mercador Itinerante armou a tenda em ", COR)
                .append(Component.text(onde.getBlockX() + " " + onde.getBlockY() + " " + onde.getBlockZ(), NamedTextColor.WHITE))
                .append(Component.text(", perto de " + alvo.getName() + ". Fica " + plugin.settings().vidaMercadorDuracao + " minutos!", COR)));
        return true;
    }

    /** Um lugar firme e a céu aberto, de 20 a 35 blocos do jogador. */
    private static Location lugar(Location centro) {
        World w = centro.getWorld();
        for (int tentativa = 0; tentativa < 12; tentativa++) {
            double ang = rnd().nextDouble() * Math.PI * 2, dist = 20 + rnd().nextDouble() * 15;
            int x = (int) (centro.getX() + Math.cos(ang) * dist), z = (int) (centro.getZ() + Math.sin(ang) * dist);
            Block chao = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (chao.isLiquid() || !chao.getType().isSolid()) continue;
            if (Math.abs(chao.getY() - centro.getY()) > 30) continue;
            return chao.getLocation().add(0.5, 1, 0.5);
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(K_MERCADOR)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFalar(PlayerInteractEntityEvent e) {
        if (!e.getRightClicked().getPersistentDataContainer().has(K_MERCADOR)) return;
        plugin.segredos().conceder(e.getPlayer(), Segredos.Segredo.MERCADOR);
        plugin.diario().marco(e.getPlayer(), "mercador", "Encontrou o Mercador Itinerante");
    }

    public void parar() {
        Entity e = ativo == null ? null : Bukkit.getEntity(ativo);
        if (e != null) e.remove();
        ativo = null;
    }
}
