package br.rpgatributos.domador;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A natureza de cada companheiro (como no Pokémon): um ponto forte e um fraco, sorteados quando
 * ele vira companheiro. Junto vem o Potencial (1 a 5 estrelas, +4% de vida e dano por estrela acima
 * de 1), que fica escondido até o estágio II (o dono do Baize vê na hora).
 */
public enum Natureza {
    VALENTE("Valente", "+10% de dano, −10% de velocidade", 0, 0.10, -0.10, 0, 1),
    ROBUSTO("Robusto", "+12% de vida, −8% de dano", 0.12, -0.08, 0, 0, 1),
    LIGEIRO("Ligeiro", "+12% de velocidade, −10% de vida", -0.10, 0, 0.12, 0, 1),
    TEIMOSO("Teimoso", "+3 de armadura, −8% de velocidade", 0, 0, -0.08, 3, 1),
    FEROZ("Feroz", "+15% de dano, −2 de armadura", 0, 0.15, 0, -2, 1),
    CALMO("Calmo", "+8% de vida e +4% de velocidade, −8% de dano", 0.08, -0.08, 0.04, 0, 1),
    LEAL("Leal", "Evolui 25% mais rápido", 0, 0, 0, 0, 1.25),
    CURIOSO("Curioso", "Evolui 15% mais rápido, −5% de dano", 0, -0.05, 0, 0, 1.15),
    MEDROSO("Medroso", "+18% de velocidade, −12% de dano", 0, -0.12, 0.18, 0, 1),
    EQUILIBRADO("Equilibrado", "Sem ponto forte nem fraco", 0, 0, 0, 0, 1);

    public static final NamespacedKey K_NATUREZA = new NamespacedKey("rpgatributos", "natureza");
    public static final NamespacedKey K_POTENCIAL = new NamespacedKey("rpgatributos", "potencial");

    private final String nome, descricao;
    private final double vida, dano, velocidade, armadura, xp;

    Natureza(String nome, String descricao, double vida, double dano, double velocidade, double armadura, double xp) {
        this.nome = nome;
        this.descricao = descricao;
        this.vida = vida;
        this.dano = dano;
        this.velocidade = velocidade;
        this.armadura = armadura;
        this.xp = xp;
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    /** Frações somadas aos multiplicadores de vida, dano e velocidade. */
    public double vida() { return vida; }
    public double dano() { return dano; }
    public double velocidade() { return velocidade; }
    /** Armadura somada (pode ser negativa). */
    public double armadura() { return armadura; }
    /** Multiplicador do XP de evolução. */
    public double xp() { return xp; }

    /** A natureza do companheiro; sorteia e grava se ainda não tiver (os antigos ganham na hora). */
    public static Natureza de(Entity e) {
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        String s = pdc.get(K_NATUREZA, PersistentDataType.STRING);
        if (s != null) {
            try {
                return valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignorado) {
                // natureza que não existe mais: sorteia outra
            }
        }
        Natureza n = values()[ThreadLocalRandom.current().nextInt(values().length)];
        pdc.set(K_NATUREZA, PersistentDataType.STRING, n.name());
        return n;
    }

    /** Potencial de 1 a 5 estrelas (sorteado uma vez: 5 estrelas é raro). */
    public static int potencial(Entity e) {
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        Integer p = pdc.get(K_POTENCIAL, PersistentDataType.INTEGER);
        if (p != null) return Math.max(1, Math.min(5, p));
        double r = ThreadLocalRandom.current().nextDouble();
        int novo = r < 0.30 ? 1 : r < 0.60 ? 2 : r < 0.82 ? 3 : r < 0.95 ? 4 : 5;
        pdc.set(K_POTENCIAL, PersistentDataType.INTEGER, novo);
        return novo;
    }

    /** Bônus de vida e dano do potencial: +4% por estrela acima de 1. */
    public static double bonusPotencial(Entity e) {
        return (potencial(e) - 1) * 0.04;
    }

    public static String estrelas(int n) {
        return "★".repeat(n) + "☆".repeat(5 - n);
    }
}
