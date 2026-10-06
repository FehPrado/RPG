package br.rpgatributos.combo;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Medidor de estilo (D, C, B, A, S): variar os golpes, acertar no tempo certo, no ar e quebrar o
 * equilíbrio sobe o medidor; repetir o mesmo golpe, ficar parado ou apanhar derruba. Quanto mais
 * alto, mais dano e mais XP de proficiência.
 */
public final class Estilo {

    public enum Rank {
        D(0, NamedTextColor.GRAY, 0, 0),
        C(30, NamedTextColor.WHITE, 0.04, 0),
        B(70, NamedTextColor.AQUA, 0.08, 0.10),
        A(120, NamedTextColor.GOLD, 0.12, 0.20),
        S(180, TextColor.color(0xFF4FD8), 0.18, 0.30);

        private final int pontos;
        private final TextColor cor;
        private final double dano, xp;

        Rank(int pontos, TextColor cor, double dano, double xp) {
            this.pontos = pontos;
            this.cor = cor;
            this.dano = dano;
            this.xp = xp;
        }

        public TextColor cor() { return cor; }
        /** Dano a mais (fração). */
        public double dano() { return dano; }
        /** XP de proficiência a mais (fração). */
        public double xp() { return xp; }

        static Rank de(double pontos) {
            Rank r = D;
            for (Rank x : values()) if (pontos >= x.pontos) r = x;
            return r;
        }
    }

    private static final double MAXIMO = 240, PARADO_MS = 4000, QUEDA_POR_SEGUNDO = 12;

    private static final class Estado {
        double pontos;
        long ultimo;
        Golpe anterior;
    }

    private final Map<UUID, Estado> estados = new HashMap<>();

    private Estado estado(Player p) {
        return estados.computeIfAbsent(p.getUniqueId(), k -> new Estado());
    }

    public Rank rank(Player p) {
        Estado e = estados.get(p.getUniqueId());
        return e == null ? Rank.D : Rank.de(e.pontos);
    }

    /** Um golpe de combo saiu. */
    void golpe(Player p, Golpe g, boolean perfeito, boolean finalizador) {
        Estado e = estado(p);
        double ganho = 12;
        ganho += g != e.anterior ? 8 : -10;
        if (perfeito) ganho += 6;
        if (finalizador) ganho += 20;
        e.anterior = g;
        somar(p, e, ganho);
    }

    /** Bônus avulsos (acerto no ar, quebra de equilíbrio...). */
    void bonus(Player p, double pontos) {
        somar(p, estado(p), pontos);
    }

    /** Levou dano: o medidor cai. */
    void apanhou(Player p, double dano) {
        Estado e = estados.get(p.getUniqueId());
        if (e == null || e.pontos <= 0) return;
        Rank antes = Rank.de(e.pontos);
        e.pontos = Math.max(0, e.pontos - Math.min(40, 8 + dano * 3));
        if (Rank.de(e.pontos).ordinal() < antes.ordinal() && antes.ordinal() >= Rank.B.ordinal()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.5f);
        }
    }

    private void somar(Player p, Estado e, double ganho) {
        Rank antes = Rank.de(e.pontos);
        e.pontos = Math.max(0, Math.min(MAXIMO, e.pontos + ganho));
        e.ultimo = System.currentTimeMillis();
        Rank depois = Rank.de(e.pontos);
        if (depois.ordinal() > antes.ordinal()) {
            p.playSound(p.getLocation(), depois == Rank.S ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_NOTE_BLOCK_CHIME,
                    0.6f, 0.8f + 0.2f * depois.ordinal());
        }
    }

    /** A cada meio segundo: quem parou de lutar perde estilo aos poucos. */
    void tick() {
        long t = System.currentTimeMillis();
        estados.values().removeIf(e -> {
            if (t - e.ultimo > PARADO_MS) e.pontos -= QUEDA_POR_SEGUNDO / 2;
            return e.pontos <= 0 && t - e.ultimo > PARADO_MS;
        });
    }

    void sair(UUID id) {
        estados.remove(id);
    }
}
