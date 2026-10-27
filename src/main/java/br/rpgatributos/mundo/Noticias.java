package br.rpgatributos.mundo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Notícias do mundo (2.98): o que aconteceu de importante (caravanas, bandos, estradas, chefes, céu),
 * guardado só na memória, as 20 mais novas. Os viajantes das estradas contam as notícias.
 */
public final class Noticias {

    private static final int MAX = 20;

    private record Noticia(String texto, long quando) { }

    private final Deque<Noticia> lista = new ArrayDeque<>();

    public void registrar(String texto) {
        if (texto == null || texto.isBlank()) return;
        lista.removeIf(n -> n.texto().equals(texto));
        lista.addFirst(new Noticia(texto, System.currentTimeMillis()));
        while (lista.size() > MAX) lista.removeLast();
    }

    /** As mais novas primeiro, até {@code n}, com "há quanto tempo". */
    public List<String> recentes(int n) {
        List<String> l = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (Noticia x : lista) {
            if (l.size() >= n) break;
            long min = (agora - x.quando()) / 60_000;
            l.add(x.texto() + (min < 2 ? " (agora há pouco)" : min < 60 ? " (há " + min + " min)" : " (há " + (min / 60) + " h)"));
        }
        return l;
    }

    public boolean vazia() {
        return lista.isEmpty();
    }
}
