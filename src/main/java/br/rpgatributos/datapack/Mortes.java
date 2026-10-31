package br.rpgatributos.datapack;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

/**
 * Tipos de dano do RPGAtributos (3.1), no datapack: o jogo sabe que foi o Kraken, o canhão, a
 * Tartaruga Ancestral ou a tempestade. A mensagem de morte vem daqui (em português, sem precisar
 * de tradução no pacote de recursos). Sem o datapack, o dano vira genérico e a mensagem é a do jogo.
 */
public final class Mortes implements Listener {

    private static final Map<String, String> MENSAGENS = Map.of(
            "kraken", " foi arrastado para o fundo pelo Kraken",
            "canhao", " foi atingido por uma bala de canhão pirata",
            "tartaruga", " foi esmagado pela Tartaruga Ancestral",
            "tempestade", " foi atingido por um raio da tempestade no mar",
            "sem_ar", " ficou sem ar na Lua");

    /** Dano com o tipo do datapack (ou genérico, se ele não carregou). {@code causa} pode ser null. */
    public static DamageSource fonte(String id, Entity causa) {
        DamageType tipo = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).get(new NamespacedKey("rpgatributos", id));
        if (tipo == null) tipo = causa == null ? DamageType.GENERIC : DamageType.MOB_ATTACK;
        DamageSource.Builder b = DamageSource.builder(tipo);
        if (causa != null) b.withCausingEntity(causa).withDirectEntity(causa);
        return b.build();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(PlayerDeathEvent e) {
        NamespacedKey k = e.getDamageSource().getDamageType().getKey();
        if (!"rpgatributos".equals(k.getNamespace())) return;
        String fim = MENSAGENS.get(k.getKey());
        if (fim == null) return;
        Player p = e.getPlayer();
        e.deathMessage(Component.text(p.getName() + fim, NamedTextColor.WHITE));
    }
}
