package br.rpgatributos.forja;

import br.rpgatributos.alquimia.Gema;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * O que foi sorteado na forja. Fica salvo dentro do próprio item.
 *
 * Os bônus guardam a "qualidade" sorteada (0 a 1), não o valor final: assim,
 * quando o item vira Netherite na mesa de ferraria, os valores sobem sozinhos.
 */
public record DadosForja(Raridade raridade, String ferreiro, Map<Stat, Double> status, List<EfeitoRolado> efeitos,
                         int refino, List<Engaste> gemas) {

    public static final int REFINO_MAXIMO = 10;
    /** Cada nível de refino deixa os bônus da forja 6% mais fortes. */
    public static final double BONUS_POR_REFINO = 0.06;
    /** E aumenta o dano/armadura base do item em 4%. */
    public static final double BASE_POR_REFINO = 0.04;

    /** Um efeito sorteado: nível (0 = I) e chance de ativar (1 = sempre). */
    public record EfeitoRolado(Efeito efeito, int nivel, double chance) {}

    /** Uma gema engastada no item (grau 1 a 3). */
    public record Engaste(Gema gema, int grau) {}

    /** Quantos engastes o item tem (pela raridade). */
    public int engastes() {
        return Gema.engastes(raridade);
    }

    public DadosForja comGemas(List<Engaste> novas) {
        return new DadosForja(raridade, ferreiro, status, efeitos, refino, List.copyOf(novas));
    }

    /** "RUBI:2,SAFIRA:1" */
    public String gemasTexto() {
        StringJoiner j = new StringJoiner(",");
        for (Engaste en : gemas) j.add(en.gema().name() + ":" + en.grau());
        return j.toString();
    }

    public static List<Engaste> lerGemas(String texto) {
        List<Engaste> l = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return l;
        for (String parte : texto.split(",")) {
            String[] p = parte.split(":");
            if (p.length != 2) continue;
            Gema g;
            try { g = Gema.valueOf(p[0]); } catch (IllegalArgumentException e) { continue; }
            try {
                l.add(new Engaste(g, Math.max(1, Math.min(Gema.GRAU_MAXIMO, Integer.parseInt(p[1])))));
            } catch (NumberFormatException ignored) { }
        }
        return l;
    }

    public double fatorRefino() {
        return 1 + BONUS_POR_REFINO * refino;
    }

    public DadosForja comRefino(int novo) {
        return new DadosForja(raridade, ferreiro, status, efeitos, Math.max(0, Math.min(REFINO_MAXIMO, novo)), gemas);
    }

    public EfeitoRolado efeito(Efeito e) {
        for (EfeitoRolado er : efeitos) if (er.efeito() == e) return er;
        return null;
    }

    public boolean temGatilho(Efeito.Gatilho g) {
        for (EfeitoRolado er : efeitos) if (er.efeito().gatilho() == g) return true;
        return false;
    }

    // ---------- texto salvo no item ----------

    /** "DANO=0.7312,VIDA=0.11" */
    public String statusTexto() {
        StringJoiner j = new StringJoiner(",");
        status.forEach((s, q) -> j.add(s.name() + "=" + String.format(Locale.ROOT, "%.4f", q)));
        return j.toString();
    }

    /** "FORCA:0:1,LENTIDAO:1:0.18" */
    public String efeitosTexto() {
        StringJoiner j = new StringJoiner(",");
        for (EfeitoRolado er : efeitos) {
            j.add(er.efeito().name() + ":" + er.nivel() + ":" + String.format(Locale.ROOT, "%.4f", er.chance()));
        }
        return j.toString();
    }

    /** Lê o texto salvo. Ignora o que não reconhecer (ex.: efeito removido numa versão nova). */
    public static Map<Stat, Double> lerStatus(String texto) {
        Map<Stat, Double> m = new EnumMap<>(Stat.class);
        if (texto == null || texto.isEmpty()) return m;
        for (String parte : texto.split(",")) {
            String[] kv = parte.split("=");
            if (kv.length != 2) continue;
            Stat s = Stat.porNome(kv[0]);
            if (s == null) continue;
            try {
                m.put(s, Math.max(0, Math.min(1, Double.parseDouble(kv[1]))));
            } catch (NumberFormatException ignored) { }
        }
        return m;
    }

    public static List<EfeitoRolado> lerEfeitos(String texto) {
        List<EfeitoRolado> l = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return l;
        for (String parte : texto.split(",")) {
            String[] p = parte.split(":");
            if (p.length != 3) continue;
            Efeito e = Efeito.porNome(p[0]);
            if (e == null) continue;
            try {
                l.add(new EfeitoRolado(e, Integer.parseInt(p[1]), Double.parseDouble(p[2])));
            } catch (NumberFormatException ignored) { }
        }
        return l;
    }
}
