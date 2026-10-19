package br.rpgatributos.mundo;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.Vector;


/**
 * O vento (2.66): sopra de um lado e muda 4 vezes por dia de jogo (a virada leva um tempinho, não é
 * de repente). É o mesmo para todos e não depende de reinício: sai da semente do mundo e da hora.
 * A força vem do tempo: fraco com tempo bom, bom na chuva e forte na Ventania.
 * <p>Ângulos no jeito do jogo: rumo 0 = para o sul, 90 = para o oeste, 180 = para o norte,
 * 270 = para o leste. O rumo é para onde o vento vai; o nome ("do leste") é de onde ele vem.
 */
public final class Vento {

    private static final long PERIODO = 6000;
    /** A virada de um rumo para o outro ocupa o último quinto do período. */
    private static final double VIRADA = 0.2;
    private static final String[] NOMES = {"sul", "sudoeste", "oeste", "noroeste", "norte", "nordeste", "leste", "sudeste"};

    private Vento() { }

    private static double sorteado(long semente, long k) {
        return new java.util.SplittableRandom(semente ^ (k * 0x9E3779B97F4A7C15L)).nextDouble() * 360;
    }

    private static World principal() {
        return Bukkit.getWorlds().getFirst();
    }

    /** Para onde o vento vai agora (graus, no jeito do jogo). */
    public static double rumo() {
        World w = principal();
        long t = w.getFullTime(), k = t / PERIODO;
        double a0 = sorteado(w.getSeed(), k), a1 = sorteado(w.getSeed(), k + 1);
        double f = (t % PERIODO) / (double) PERIODO;
        double s = Math.max(0, (f - (1 - VIRADA)) / VIRADA);
        s = s * s * (3 - 2 * s);
        double d = ((a1 - a0) % 360 + 540) % 360 - 180;
        return ((a0 + d * s) % 360 + 360) % 360;
    }

    /** A direção do vento (para onde ele empurra). */
    public static Vector direcao() {
        double r = Math.toRadians(rumo());
        return new Vector(-Math.sin(r), 0, Math.cos(r));
    }

    /** 0,8 = fraco (tempo bom), 1,15 = bom (chuva), 1,5 = forte (Ventania). */
    public static double forca(RPGAtributos plugin) {
        if (plugin.ceu() != null && plugin.ceu().clima() == Ceu.Clima.VENTANIA) return 1.5;
        return principal().hasStorm() ? 1.15 : 0.8;
    }

    public static String intensidade(double forca) {
        return forca >= 1.5 ? "forte" : forca >= 1.1 ? "bom" : "fraco";
    }

    /** "do leste": de onde o vento vem. */
    public static String de() {
        double origem = (rumo() + 180) % 360;
        return "do " + NOMES[(int) Math.round(origem / 45) % 8];
    }

    /** "para o oeste": para onde o vento vai (quem veleja para lá vai com ele). */
    public static String para() {
        return "para o " + NOMES[(int) Math.round(rumo() / 45) % 8];
    }

    /** "Vento do leste, fraco". */
    public static String texto(RPGAtributos plugin) {
        return "Vento " + de() + ", " + intensidade(forca(plugin));
    }
}
