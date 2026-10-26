package br.rpgatributos.detalhes;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class ElevadoresTest {
    private static <T> T proxy(Class<T> tipo, java.lang.reflect.InvocationHandler h) {
        return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, h));
    }
    @Test void usaAOrigemMesmoQuandoOJogadorJaEstaNoAr() {
        Block elevador = proxy(Block.class, (p,m,a) -> null);
        World mundo = proxy(World.class, (p,m,a) -> {
            if (m.getName().equals("getBlockAt")) {
                Location apoio = (Location) a[0];
                assertEquals(-2, apoio.getBlockX());
                assertEquals(64, apoio.getBlockY());
                assertEquals(3, apoio.getBlockZ());
                return elevador;
            }
            return null;
        });
        Location de = new Location(mundo,-1.5,65,3.5), noAr = de.clone().add(0,.42,0);
        Player jogador = proxy(Player.class, (p,m,a) -> switch (m.getName()) {
            case "getLocation" -> noAr;
            case "isInsideVehicle", "isFlying" -> false;
            default -> null;
        });
        assertSame(elevador, Elevadores.apoioDoPulo(new PlayerJumpEvent(jogador,de,noAr)));
        assertEquals(65, de.getY(), "Não altera a origem do evento");
    }
    @Test void naoAcionaEnquantoVoaOuEstaMontado() {
        for (String impedimento : new String[]{"isFlying","isInsideVehicle"}) {
            Player jogador = proxy(Player.class, (p,m,a) -> {
                if (m.getReturnType() == boolean.class) return m.getName().equals(impedimento);
                return null;
            });
            Location de = new Location(null,0,65,0);
            assertNull(Elevadores.apoioDoPulo(new PlayerJumpEvent(jogador,de,de.clone().add(0,.42,0))));
        }
    }
}
