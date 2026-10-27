package br.rpgatributos.estrada;

import br.rpgatributos.RPGAtributos;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Andar na estrada é mais rápido (2.94): a pé ou montado, o piso dá de +5% (trilha) a +15% (Estrada Real).
 * O bônus é um modificador de velocidade com chave própria, posto e tirado conforme o piso; montado, vai
 * para a montaria. Pulando, o nível fica o mesmo (senão piscaria a cada pulo).
 */
public final class Velocidade implements Listener {

    private final RPGAtributos plugin;
    private final NamespacedKey chave;
    /** Jogador → a montaria que está com o bônus agora. */
    private final Map<UUID, UUID> montarias = new HashMap<>();
    /** Na última volta o bônus estava ligado (para limpar uma vez só quando o config desliga). */
    private boolean ligada;

    public Velocidade(RPGAtributos plugin) {
        this.plugin = plugin;
        this.chave = new NamespacedKey(plugin, "estrada_velocidade");
    }

    /** A cada 4 ticks. */
    public void tick() {
        if (!plugin.settings().estradaVelocidade) {
            if (ligada) desligar();
            ligada = false;
            return;
        }
        ligada = true;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Entity veiculo = p.getVehicle();
            UUID antes = montarias.get(p.getUniqueId());
            if (antes != null && (veiculo == null || !veiculo.getUniqueId().equals(antes))) {
                Entity velha = plugin.getServer().getEntity(antes);
                if (velha instanceof LivingEntity le) tirar(le);
                montarias.remove(p.getUniqueId());
            }
            LivingEntity alvo;
            if (veiculo == null) alvo = p;
            else if (veiculo instanceof LivingEntity le) {
                alvo = le;
                tirar(p);
            } else {
                tirar(p); // barco, carroça e carrinho têm velocidade própria
                continue;
            }
            int nivel;
            if (p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isGliding() || p.isSwimming() || alvo.isInWater()) nivel = 0;
            else if (!alvo.isOnGround()) continue;
            else nivel = Pisos.nivelEm(Pisos.debaixo(alvo));
            // 2.95: descansou num Pouso de Viajante: um pouco mais rápido na estrada.
            double extra = plugin.fogueiras().pousoAte(p) > System.currentTimeMillis() ? plugin.settings().estradaBonusPouso : 0;
            // 2.99: o ensinamento do Cartógrafo (Laço completo).
            if (plugin.lacos().sabe(p, br.rpgatributos.colonia.Profissao.CARTOGRAFO)) extra += 0.05;
            aplicar(alvo, nivel, extra);
            if (alvo != p) montarias.put(p.getUniqueId(), alvo.getUniqueId());
        }
    }

    private void aplicar(LivingEntity e, int nivel, double extra) {
        AttributeInstance a = e.getAttribute(Attribute.MOVEMENT_SPEED);
        if (a == null) return;
        double quer = nivel <= 0 ? 0 : plugin.settings().estradaBonus[Math.min(nivel, plugin.settings().estradaBonus.length) - 1] + extra;
        AttributeModifier atual = a.getModifier(chave);
        if (atual != null && atual.getAmount() == quer) return;
        if (atual != null) a.removeModifier(chave);
        if (quer > 0) a.addTransientModifier(new AttributeModifier(chave, quer, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
    }

    private void tirar(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.MOVEMENT_SPEED);
        if (a != null && a.getModifier(chave) != null) a.removeModifier(chave);
    }

    /** O bônus é passageiro (não é salvo), mas um jogador de versão antiga pode ter ficado com ele. */
    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        tirar(e.getPlayer());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        tirar(p);
        UUID m = montarias.remove(p.getUniqueId());
        if (m != null && plugin.getServer().getEntity(m) instanceof LivingEntity le) tirar(le);
    }

    public void desligar() {
        for (Player p : plugin.getServer().getOnlinePlayers()) tirar(p);
        for (UUID m : montarias.values()) if (plugin.getServer().getEntity(m) instanceof LivingEntity le) tirar(le);
        montarias.clear();
    }
}
