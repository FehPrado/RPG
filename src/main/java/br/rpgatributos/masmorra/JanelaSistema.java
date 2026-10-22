package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * As janelas do Sistema (2.73): avisos com moldura azul no chat, como no Solo Leveling
 * (Despertar, rank novo, função, Missão Diária).
 */
public final class JanelaSistema {

    public static final TextColor AZUL = TextColor.color(0x4FC3F7);
    private static final TextColor BORDA = TextColor.color(0x1E88E5);

    private JanelaSistema() { }

    public static void mostrar(Player p, String titulo, List<Component> linhas) {
        p.sendMessage(Component.text("┏━━━ ", BORDA).append(Component.text("[Sistema] " + titulo, AZUL, TextDecoration.BOLD))
                .append(Component.text(" ━━━", BORDA)));
        for (Component l : linhas) p.sendMessage(Component.text("┃ ", BORDA).append(l.colorIfAbsent(NamedTextColor.WHITE)));
        p.sendMessage(Component.text("┗━━━━━━━━━━━━━━━━━━━━━━", BORDA));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 1.6f);
    }

    public static void mostrar(Player p, String titulo, Component... linhas) {
        mostrar(p, titulo, List.of(linhas));
    }
}
