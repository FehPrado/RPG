package br.rpgatributos.masmorra;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EquipamentoPortalTest {
    @Test
    void bonusDoConjuntoEDasPecasAvulsas() {
        assertEquals(0, EquipamentoPortal.calcularBonus(new Rank[4], null), 1e-9);
        assertEquals(0.01, EquipamentoPortal.calcularBonus(new Rank[4], Rank.E), 1e-9);
        assertEquals(0.18, EquipamentoPortal.calcularBonus(new Rank[]{Rank.S, Rank.S, null, null}, Rank.S), 1e-9);
        assertEquals(0.45, EquipamentoPortal.calcularBonus(new Rank[]{Rank.S, Rank.S, Rank.S, Rank.S}, Rank.S), 1e-9);
        assertEquals(0.36, EquipamentoPortal.calcularBonus(new Rank[]{Rank.S, Rank.S, Rank.S, Rank.S}, null), 1e-9);
    }

    @Test
    void flechaConsultaArmaDoDisparoSemConsultarMaoAtual() {
        AtomicBoolean consultou = new AtomicBoolean();
        AbstractArrow flecha = (AbstractArrow) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AbstractArrow.class}, (p, metodo, args) -> {
            assertEquals("getWeapon", metodo.getName());
            consultou.set(true);
            return null;
        });
        Player jogador = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Player.class}, (p, metodo, args) -> {
            throw new AssertionError("Não deve consultar o equipamento atual: " + metodo.getName());
        });
        assertNull(EquipamentoPortal.armaDoAtaque(flecha, jogador));
        assertTrue(consultou.get());
    }
}
