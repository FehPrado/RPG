package br.rpgatributos.arcano;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/** Uma magia criada pelo jogador: de 1 a 4 essências + uma forma, com um nome. */
public record Magia(Set<Essencia> essencias, Forma forma, String nome) {

    public static final int MAX_ESSENCIAS = 4;
    public static final int MAX_NOME = 24;

    public Magia {
        essencias = EnumSet.copyOf(essencias);
    }

    public Receita receita() {
        return Receita.de(essencias, forma);
    }

    /** Identifica a combinação (usado na recarga). */
    public String chave() {
        StringJoiner j = new StringJoiner("+");
        for (Essencia e : essencias) j.add(e.name());
        return j + ":" + forma.name();
    }

    public Magia comNome(String novo) {
        return new Magia(essencias, forma, limparNome(novo));
    }

    /** Nome da receita secreta, se for uma; senão o nome genérico. */
    public static String nomePadrao(Set<Essencia> essencias, Forma forma) {
        Receita r = Receita.de(essencias, forma);
        return r != null ? r.nome() : nomeGenerico(essencias, forma);
    }

    /** "Projétil de Fogo e Vento" */
    public static String nomeGenerico(Set<Essencia> essencias, Forma forma) {
        StringBuilder sb = new StringBuilder(forma.nome()).append(" de ");
        int i = 0;
        for (Essencia e : essencias) {
            if (i > 0) sb.append(i == essencias.size() - 1 ? " e " : ", ");
            sb.append(e.nome());
            i++;
        }
        return sb.toString();
    }

    public static String limparNome(String nome) {
        String n = nome.replaceAll("[;|:&§]", "").trim();
        return n.length() > MAX_NOME ? n.substring(0, MAX_NOME) : n;
    }

    // ---------- texto salvo no jogador: "FOGO+VENTO;PROJETIL;Bola de Fogo" ----------

    public String serializar() {
        StringJoiner j = new StringJoiner("+");
        for (Essencia e : essencias) j.add(e.name());
        return j + ";" + forma.name() + ";" + nome;
    }

    public static Magia ler(String texto) {
        String[] p = texto.split(";", 3);
        if (p.length != 3) return null;
        Set<Essencia> es = EnumSet.noneOf(Essencia.class);
        for (String s : p[0].split("\\+")) {
            Essencia e = Essencia.porNome(s);
            if (e != null) es.add(e);
        }
        Forma f = Forma.porNome(p[1]);
        if (es.isEmpty() || es.size() > MAX_ESSENCIAS || f == null) return null;
        return new Magia(es, f, p[2].isBlank() ? nomePadrao(es, f) : p[2]);
    }
}
