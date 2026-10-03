package br.rpgatributos.arcano;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

/** Uma magia criada pelo jogador: de 1 a 4 essências + uma forma + até 2 modificadores, com um nome. */
public record Magia(Set<Essencia> essencias, Forma forma, Set<Modificador> mods, String nome) {

    public static final int MAX_ESSENCIAS = 4;
    public static final int MAX_NOME = 24;

    public Magia {
        essencias = EnumSet.copyOf(essencias);
        mods = mods == null || mods.isEmpty() ? EnumSet.noneOf(Modificador.class) : EnumSet.copyOf(mods);
    }

    public Magia(Set<Essencia> essencias, Forma forma, String nome) {
        this(essencias, forma, EnumSet.noneOf(Modificador.class), nome);
    }

    public Receita receita() {
        return Receita.de(essencias, forma);
    }

    public boolean tem(Modificador m) {
        return mods.contains(m);
    }

    /** Identifica a combinação (usado na recarga). */
    public String chave() {
        StringJoiner j = new StringJoiner("+");
        for (Essencia e : essencias) j.add(e.name());
        StringJoiner k = new StringJoiner("+");
        for (Modificador m : mods) k.add(m.name());
        return j + ":" + forma.name() + ":" + k;
    }

    public Magia comNome(String novo) {
        return new Magia(essencias, forma, mods, limparNome(novo));
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
        String s = sb.toString();
        return s.length() > MAX_NOME ? s.substring(0, MAX_NOME) : s;
    }

    public static String limparNome(String nome) {
        String n = nome.replaceAll("[;|:&§]", "").trim();
        return n.length() > MAX_NOME ? n.substring(0, MAX_NOME) : n;
    }

    // ---------- texto salvo: "FOGO+VENTO;PROJETIL;Bola de Fogo;DIVIDIR+ECO" (o último é opcional) ----------

    public String serializar() {
        StringJoiner j = new StringJoiner("+");
        for (Essencia e : essencias) j.add(e.name());
        StringJoiner k = new StringJoiner("+");
        for (Modificador m : mods) k.add(m.name());
        return j + ";" + forma.name() + ";" + nome + (mods.isEmpty() ? "" : ";" + k);
    }

    public static Magia ler(String texto) {
        String[] p = texto.split(";", 4);
        if (p.length < 3) return null;
        Set<Essencia> es = EnumSet.noneOf(Essencia.class);
        for (String s : p[0].split("\\+")) {
            Essencia e = Essencia.porNome(s);
            if (e != null) es.add(e);
        }
        Forma f = Forma.porNome(p[1]);
        if (es.isEmpty() || es.size() > MAX_ESSENCIAS || f == null) return null;
        Set<Modificador> mods = EnumSet.noneOf(Modificador.class);
        if (p.length == 4) {
            for (String s : p[3].split("\\+")) {
                Modificador m = Modificador.porNome(s);
                if (m != null && m.serveEm(f) && mods.size() < Modificador.MAXIMO) mods.add(m);
            }
        }
        return new Magia(es, f, mods, p[2].isBlank() ? nomePadrao(es, f) : p[2]);
    }
}
