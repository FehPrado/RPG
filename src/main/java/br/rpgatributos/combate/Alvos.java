package br.rpgatributos.combate;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.sombra.Sombras;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Quem os golpes de combo e as habilidades de classe podem acertar. Monstros e chefes sempre;
 * jogadores onde há PvP; e (com "combate.golpes-em-qualquer-criatura") qualquer outra criatura.
 * Ficam de fora: aldeões e NPCs do plugin, os seus pets e companheiros (e os de quem você não
 * pode atacar), invocações aliadas, moradores de colônia (fora da guerra) e animais com nome.
 */
public final class Alvos {

    private static final NamespacedKey K_INVOCACAO = new NamespacedKey("rpgatributos", "invocacao_dono");

    private final RPGAtributos plugin;

    public Alvos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public boolean inimigo(Player p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !le.isValid() || le.isDead() || e instanceof ArmorStand) return false;
        if (Sombras.eh(e)) return plugin.sombras().inimigoDoJogador(p, e);
        if (e instanceof Player o) {
            return (o.getGameMode() == GameMode.SURVIVAL || o.getGameMode() == GameMode.ADVENTURE) && plugin.pvpPermitido(p, o);
        }
        UUID dono = Companheiros.dono(e);
        if (dono == null && e instanceof Tameable t && t.isTamed()) dono = t.getOwnerUniqueId();
        if (dono == null) {
            String inv = e.getPersistentDataContainer().get(K_INVOCACAO, PersistentDataType.STRING);
            if (inv != null) {
                try {
                    dono = UUID.fromString(inv);
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }
        }
        if (dono != null) return !dono.equals(p.getUniqueId()) && pvp(p, dono);
        UUID colono = plugin.colonias().colonoDe(e);
        if (colono != null) return plugin.reinos().inimigosEmGuerra(p.getUniqueId(), colono);
        if (e instanceof Enemy || Chefes.ehChefe(e) || Chefes.ehLacaio(e)) return true;
        if (e instanceof Mob m && m.getTarget() == p) return true;
        if (!plugin.settings().golpesEmQualquerCriatura) return false;
        if (e instanceof AbstractVillager || e.isInvulnerable()) return false; // aldeões, Cronista, mercadores, viajantes
        // Animal com etiqueta de nome é de alguém (os animais raros não contam).
        return !(e instanceof Animals && e.customName() != null && br.rpgatributos.detalhes.Natureza.raridade(e) == null);
    }

    private boolean pvp(Player p, UUID outro) {
        Player o = plugin.getServer().getPlayer(outro);
        return o != null && plugin.pvpPermitido(p, o);
    }
}
