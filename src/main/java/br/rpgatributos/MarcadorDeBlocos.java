package br.rpgatributos;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Marca blocos guardando a posição dentro do próprio chunk, então sobrevive a
 * reinícios do servidor. Usado para "bloco colocado por jogador" e para os infusores.
 * Cada chunk guarda uma lista ordenada com a posição dos blocos marcados.
 */
public final class MarcadorDeBlocos {

    private final NamespacedKey chave;

    public MarcadorDeBlocos(RPGAtributos plugin, String nome) {
        this.chave = new NamespacedKey(plugin, nome);
    }

    /** Posição dentro do chunk num único número: y (com folga para negativos), x e z. */
    private static int posicao(Block b) {
        return ((b.getY() + 4096) << 8) | ((b.getX() & 15) << 4) | (b.getZ() & 15);
    }

    public void marcar(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        int[] atual = pdc.getOrDefault(chave, PersistentDataType.INTEGER_ARRAY, new int[0]);
        int pos = posicao(b);
        int i = Arrays.binarySearch(atual, pos);
        if (i >= 0) return;
        i = -i - 1;
        int[] novo = new int[atual.length + 1];
        System.arraycopy(atual, 0, novo, 0, i);
        novo[i] = pos;
        System.arraycopy(atual, i, novo, i + 1, atual.length - i);
        pdc.set(chave, PersistentDataType.INTEGER_ARRAY, novo);
    }

    /** @return true se o bloco estava marcado. */
    public boolean desmarcar(Block b) {
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer();
        int[] atual = pdc.get(chave, PersistentDataType.INTEGER_ARRAY);
        if (atual == null) return false;
        int i = Arrays.binarySearch(atual, posicao(b));
        if (i < 0) return false;
        if (atual.length == 1) {
            pdc.remove(chave);
        } else {
            int[] novo = new int[atual.length - 1];
            System.arraycopy(atual, 0, novo, 0, i);
            System.arraycopy(atual, i + 1, novo, i, atual.length - i - 1);
            pdc.set(chave, PersistentDataType.INTEGER_ARRAY, novo);
        }
        return true;
    }

    public boolean marcado(Block b) {
        int[] atual = b.getChunk().getPersistentDataContainer().get(chave, PersistentDataType.INTEGER_ARRAY);
        return atual != null && Arrays.binarySearch(atual, posicao(b)) >= 0;
    }

    /** Todos os blocos marcados num chunk. */
    public List<Block> todos(Chunk chunk) {
        int[] atual = chunk.getPersistentDataContainer().get(chave, PersistentDataType.INTEGER_ARRAY);
        List<Block> l = new ArrayList<>();
        if (atual == null) return l;
        for (int p : atual) {
            l.add(chunk.getBlock((p >> 4) & 15, (p >>> 8) - 4096, p & 15));
        }
        return l;
    }
}
