package br.rpgatributos.arcano;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Tudo de arcano de um jogador. Fica salvo dentro do próprio jogador (PDC). */
public final class Perfil {

    public static final int MAX_MAGIAS = 8;

    final Map<ParteCorpo, org.bukkit.Material> infusoes = new EnumMap<>(ParteCorpo.class);
    /** Partes com núcleo de chefe infundido (infusão lendária). Uma parte tem item OU núcleo. */
    final Map<ParteCorpo, br.rpgatributos.aventura.Raro> nucleos = new EnumMap<>(ParteCorpo.class);
    double mana = -1; // -1 = ainda não carregado, começa cheio
    final Magia[] magias = new Magia[MAX_MAGIAS];
    int selecionada;
    final Set<Receita> descobertas = EnumSet.noneOf(Receita.class);
    long instavelAte;
    /** XP de maestria de cada elemento (o nível sai daqui). */
    final Map<Essencia, Integer> maestria = new EnumMap<>(Essencia.class);
    Estilo estilo = Estilo.PADRAO;

    // ---------- só em memória ----------
    /** chave da magia → tick em que pode lançar de novo */
    final Map<String, Integer> recargas = new HashMap<>();
    int bastiaoAte;
    int fenixAte;
    int fantasmaAte;
    int avatarAte;
    int mostrarManaAte;
    boolean avisouInstavel;
    /** Arma encantada por uma magia de "Encantar arma" (null = nenhuma). */
    Feiticos.ArmaEncantada arma;

    public Map<ParteCorpo, org.bukkit.Material> infusoes() { return infusoes; }
    public Map<ParteCorpo, br.rpgatributos.aventura.Raro> nucleos() { return nucleos; }

    public boolean ocupada(ParteCorpo parte) {
        return infusoes.containsKey(parte) || nucleos.containsKey(parte);
    }

    public boolean temInfusao() {
        return !infusoes.isEmpty() || !nucleos.isEmpty();
    }

    public int partesOcupadas() {
        return infusoes.size() + nucleos.size();
    }

    public boolean temNucleo(br.rpgatributos.aventura.Raro r) {
        return nucleos.containsValue(r);
    }
    public double mana() { return mana; }
    public Magia magia(int i) { return i >= 0 && i < MAX_MAGIAS ? magias[i] : null; }
    public int selecionada() { return selecionada; }
    public Set<Receita> descobertas() { return descobertas; }

    public boolean instavel() {
        return System.currentTimeMillis() < instavelAte;
    }

    public long segundosInstavel() {
        return Math.max(0, (instavelAte - System.currentTimeMillis()) / 1000);
    }

    /** Pontos de cada essência somando todas as infusões. */
    public Map<Essencia, Integer> essencias() {
        Map<Essencia, Integer> total = new EnumMap<>(Essencia.class);
        for (org.bukkit.Material m : infusoes.values()) {
            Catalogo.de(m).forEach((e, v) -> total.merge(e, v, Integer::sum));
        }
        for (br.rpgatributos.aventura.Raro r : nucleos.values()) {
            Lendarias.essencias(r).forEach((e, v) -> total.merge(e, v, Integer::sum));
        }
        return total;
    }

    public int carga() {
        int c = 0;
        for (org.bukkit.Material m : infusoes.values()) c += Catalogo.carga(m);
        c += nucleos.size() * Lendarias.CARGA;
        return c;
    }

    /** Poder do que está infundido numa parte (0 se vazia). */
    public int poder(ParteCorpo parte) {
        if (nucleos.containsKey(parte)) return Lendarias.PODER;
        org.bukkit.Material m = infusoes.get(parte);
        return m == null ? 0 : Catalogo.poder(m);
    }

    public Magia magiaSelecionada() {
        return magia(selecionada);
    }

    /** Primeiro espaço livre do grimório até o limite liberado, ou -1. */
    public int espacoLivre(int liberados) {
        for (int i = 0; i < Math.min(liberados, MAX_MAGIAS); i++) if (magias[i] == null) return i;
        return -1;
    }

    // ---------- texto salvo ----------

    /** "MENTE=BLAZE_ROD,CORACAO=nucleo:NUCLEO_GOLEM" */
    String infusoesTexto() {
        StringJoiner j = new StringJoiner(",");
        infusoes.forEach((p, m) -> j.add(p.name() + "=" + m.name()));
        nucleos.forEach((p, r) -> j.add(p.name() + "=nucleo:" + r.name()));
        return j.toString();
    }

    void lerInfusoes(String texto) {
        infusoes.clear();
        nucleos.clear();
        if (texto == null || texto.isEmpty()) return;
        for (String parte : texto.split(",")) {
            String[] kv = parte.split("=");
            if (kv.length != 2) continue;
            ParteCorpo p = ParteCorpo.porNome(kv[0]);
            if (p == null) continue;
            if (kv[1].startsWith("nucleo:")) {
                br.rpgatributos.aventura.Raro r = br.rpgatributos.aventura.Raro.porId(kv[1].substring(7));
                if (r != null && r.nucleo()) nucleos.put(p, r);
                continue;
            }
            org.bukkit.Material m = org.bukkit.Material.matchMaterial(kv[1]);
            if (m != null) infusoes.put(p, m);
        }
    }

    String magiasTexto() {
        StringJoiner j = new StringJoiner("|");
        for (int i = 0; i < MAX_MAGIAS; i++) {
            j.add(magias[i] == null ? "" : magias[i].serializar());
        }
        return j.toString();
    }

    void lerMagias(String texto) {
        for (int i = 0; i < MAX_MAGIAS; i++) magias[i] = null;
        if (texto == null || texto.isEmpty()) return;
        String[] partes = texto.split("\\|", -1);
        for (int i = 0; i < Math.min(partes.length, MAX_MAGIAS); i++) {
            if (!partes[i].isEmpty()) magias[i] = Magia.ler(partes[i]);
        }
    }

    public int xpMaestria(Essencia e) { return maestria.getOrDefault(e, 0); }
    public Estilo estilo() { return estilo; }

    /** "FOGO=120,GELO=40" */
    String maestriaTexto() {
        StringJoiner j = new StringJoiner(",");
        maestria.forEach((e, v) -> j.add(e.name() + "=" + v));
        return j.toString();
    }

    void lerMaestria(String texto) {
        maestria.clear();
        if (texto == null || texto.isEmpty()) return;
        for (String parte : texto.split(",")) {
            String[] kv = parte.split("=");
            if (kv.length != 2) continue;
            Essencia e = Essencia.porNome(kv[0]);
            if (e == null) continue;
            try { maestria.put(e, Math.max(0, Integer.parseInt(kv[1]))); } catch (NumberFormatException ignored) { }
        }
    }

    String descobertasTexto() {
        StringJoiner j = new StringJoiner(",");
        for (Receita r : descobertas) j.add(r.name());
        return j.toString();
    }

    void lerDescobertas(String texto) {
        descobertas.clear();
        if (texto == null || texto.isEmpty()) return;
        for (String s : texto.split(",")) {
            Receita r = Receita.porNome(s);
            if (r != null) descobertas.add(r);
        }
    }
}
