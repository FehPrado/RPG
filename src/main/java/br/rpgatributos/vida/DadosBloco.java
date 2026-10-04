package br.rpgatributos.vida;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

/** Um texto guardado no chunk para um bloco (estado do canteiro, colmeia, barril...). */
final class DadosBloco {

    private DadosBloco() { }

    private static NamespacedKey chave(String prefixo, Location l) {
        return new NamespacedKey("rpgatributos", prefixo + "_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    static String ler(String prefixo, Location l) {
        return l.getChunk().getPersistentDataContainer().get(chave(prefixo, l), PersistentDataType.STRING);
    }

    static void gravar(String prefixo, Location l, String valor) {
        l.getChunk().getPersistentDataContainer().set(chave(prefixo, l), PersistentDataType.STRING, valor);
    }

    static void apagar(String prefixo, Location l) {
        l.getChunk().getPersistentDataContainer().remove(chave(prefixo, l));
    }

    /** "quantidade|desde" → [quantidade, desde]; vazio → [0, agora]. */
    static long[] contagem(String prefixo, Location l) {
        String s = ler(prefixo, l);
        if (s != null) {
            String[] p = s.split("\\|");
            try {
                return new long[]{Long.parseLong(p[0]), Long.parseLong(p[1])};
            } catch (RuntimeException ignorado) {
                // dado inválido: recomeça
            }
        }
        return new long[]{0, System.currentTimeMillis()};
    }

    /**
     * Quanto foi produzido desde a última vez, a cada {@code intervaloMs}, até {@code maximo}.
     * @return [quantidade agora, novo "desde"] (o resto do tempo continua contando).
     */
    static long[] produzir(String prefixo, Location l, long intervaloMs, int maximo) {
        long[] c = contagem(prefixo, l);
        long agora = System.currentTimeMillis();
        long ciclos = Math.max(0, (agora - c[1]) / intervaloMs);
        long qtd = Math.min(maximo, c[0] + ciclos);
        long desde = qtd >= maximo ? agora : c[1] + ciclos * intervaloMs;
        return new long[]{qtd, desde};
    }
}
