package br.rpgatributos.aventura;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Locale;

import static br.rpgatributos.aventura.Chefe.Habilidade.*;

/**
 * Os chefes do Altar Ritualístico, do mais fácil (nível 1) ao mais difícil (nível 4).
 * A vida cresce com o número de jogadores perto quando o chefe nasce.
 */
public enum Chefe {
    GOLEM("Golem Ancestral", 0xD8D8D8, BossBar.Color.WHITE, EntityType.IRON_GOLEM, 1.6,
            300, 12, 10, 1, Raro.NUCLEO_GOLEM, List.of(TREMOR, ARREMESSO, ESCUDO_DE_FERRO),
            List.of(new Saque(Material.IRON_INGOT, 16, 32), new Saque(Material.EMERALD, 4, 8))),
    RAINHA("Rainha Aracnídea", 0x8E5B9E, BossBar.Color.PURPLE, EntityType.SPIDER, 3.0,
            260, 9, 6, 1, Raro.NUCLEO_RAINHA, List.of(NINHADA, TEIA, NUVEM_VENENOSA, SALTO),
            List.of(new Saque(Material.STRING, 16, 32), new Saque(Material.SPIDER_EYE, 4, 8), new Saque(Material.EMERALD, 4, 8))),
    CHAMAS("Senhor das Chamas", 0xFF6A2B, BossBar.Color.RED, EntityType.BLAZE, 2.5,
            380, 10, 8, 2, Raro.NUCLEO_CHAMAS, List.of(BOLAS_DE_FOGO, ANEL_DE_FOGO, METEORO),
            List.of(new Saque(Material.BLAZE_ROD, 8, 16), new Saque(Material.GOLD_INGOT, 8, 16), new Saque(Material.EMERALD, 8, 12))),
    LICH("Lich", 0x9A86B8, BossBar.Color.PURPLE, EntityType.SKELETON, 1.5,
            420, 10, 8, 2, Raro.NUCLEO_LICH, List.of(INVOCAR_MORTOS, RAIO_SOMBRIO, TELEPORTE, DRENO),
            List.of(new Saque(Material.BONE, 16, 32), new Saque(Material.EXPERIENCE_BOTTLE, 8, 16), new Saque(Material.EMERALD, 8, 12))),
    TEMPESTADE("Tempestade Viva", 0xDDEFEF, BossBar.Color.BLUE, EntityType.BREEZE, 2.5,
            500, 12, 10, 3, Raro.NUCLEO_TEMPESTADE, List.of(RAJADA, RAIOS, CICLONE),
            List.of(new Saque(Material.BREEZE_ROD, 4, 8), new Saque(Material.DIAMOND, 2, 5), new Saque(Material.EMERALD, 12, 20))),
    ARAUTO("Arauto do Fim", 0xC77DFF, BossBar.Color.PINK, EntityType.WITHER_SKELETON, 2.0,
            900, 18, 15, 4, Raro.NUCLEO_ARAUTO, List.of(GOLPE_DO_VAZIO, VEXES, ONDA_SONICA, ESCURIDAO),
            List.of(new Saque(Material.DIAMOND, 6, 12), new Saque(Material.NETHERITE_SCRAP, 2, 4), new Saque(Material.EMERALD, 24, 40)));

    /** Item comum que o chefe deixa cair (quantidade sorteada entre min e max). */
    public record Saque(Material material, int min, int max) {}

    /** Habilidades especiais, com recarga em segundos. */
    public enum Habilidade {
        TREMOR(8), ARREMESSO(10), ESCUDO_DE_FERRO(25),
        NINHADA(14), TEIA(9), NUVEM_VENENOSA(12), SALTO(6),
        BOLAS_DE_FOGO(6), ANEL_DE_FOGO(12), METEORO(15),
        INVOCAR_MORTOS(18), RAIO_SOMBRIO(5), TELEPORTE(9), DRENO(14),
        RAJADA(8), RAIOS(12), CICLONE(16),
        GOLPE_DO_VAZIO(8), VEXES(22), ONDA_SONICA(10), ESCURIDAO(20);

        private final int recarga;

        Habilidade(int recarga) { this.recarga = recarga; }

        public int recarga() { return recarga; }
    }

    private final String nome;
    private final TextColor cor;
    private final BossBar.Color corBarra;
    private final EntityType tipo;
    private final double escala;
    private final double vida;
    private final double dano;
    private final double armadura;
    private final int nivel;
    private final Raro nucleo;
    private final List<Habilidade> habilidades;
    private final List<Saque> saque;

    Chefe(String nome, int cor, BossBar.Color corBarra, EntityType tipo, double escala, double vida, double dano,
          double armadura, int nivel, Raro nucleo, List<Habilidade> habilidades, List<Saque> saque) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.corBarra = corBarra;
        this.tipo = tipo;
        this.escala = escala;
        this.vida = vida;
        this.dano = dano;
        this.armadura = armadura;
        this.nivel = nivel;
        this.nucleo = nucleo;
        this.habilidades = habilidades;
        this.saque = saque;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public BossBar.Color corBarra() { return corBarra; }
    public EntityType tipo() { return tipo; }
    public double escala() { return escala; }
    public double vida() { return vida; }
    public double dano() { return dano; }
    public double armadura() { return armadura; }
    /** Dificuldade de 1 a 4 (decide recompensas). */
    public int nivel() { return nivel; }
    public Raro nucleo() { return nucleo; }
    public List<Habilidade> habilidades() { return habilidades; }
    public List<Saque> saque() { return saque; }

    /** "★★☆☆" */
    public String estrelas() {
        return "★".repeat(nivel) + "☆".repeat(4 - nivel);
    }

    public static Chefe porId(String id) {
        for (Chefe c : values()) if (c.id().equalsIgnoreCase(id)) return c;
        return null;
    }
}
