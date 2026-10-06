package br.rpgatributos.ajuda;

import org.bukkit.Material;

import java.util.List;

/**
 * Um assunto do Guia interativo: poucas linhas, o ritual (se for uma estação) desenhado com
 * os itens de verdade, uma dica, o comando que abre o sistema e os assuntos ligados.
 *
 * @param itens   itens jogados em cima do bloco, como pares {Material ou ItemStack, quantidade}
 * @param comando comando (sem a barra) que abre o sistema, ou null
 */
public record Topico(String id, Categoria categoria, String nome, Material icone, List<String> texto,
                     Material bloco, String nomeBloco, List<Object[]> itens, String dica, String comando,
                     List<String> ligados, String palavras) {

    public boolean temRitual() {
        return bloco != null;
    }

    /** Para a busca: nome, texto e palavras extras, sem acento e em minúsculas. */
    public String indice() {
        return Topicos.normalizar(nome + " " + String.join(" ", texto) + " " + (nomeBloco == null ? "" : nomeBloco) + " " + palavras);
    }
}
