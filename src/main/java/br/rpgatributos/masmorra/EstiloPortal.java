package br.rpgatributos.masmorra;

import br.rpgatributos.forja.Traje;
import java.util.Locale;

/** Famílias visuais independentes do rank e dos atributos sorteados na Forja. */
public enum EstiloPortal {
    CLASSICO("Clássico", "espada", Traje.Estilo.PLACAS, 0x68747D),
    VIGIA("do Vigia", "larga", Traje.Estilo.PLACAS, 0x8E8057),
    PREDADOR("do Predador", "sabre", Traje.Estilo.ESCAMAS, 0x386752),
    RUNICO("Rúnico", "crescente", Traje.Estilo.TECIDO, 0x495985),
    ESPINHO("de Espinhos", "montante", Traje.Estilo.ESCAMAS, 0x804A4F),
    ABISSAL("Abissal", "adaga", Traje.Estilo.TECIDO, 0x352841);

    public final String nome, espada;
    public final Traje.Estilo armadura;
    public final int cor;

    EstiloPortal(String nome, String espada, Traje.Estilo armadura, int cor) {
        this.nome = nome; this.espada = espada; this.armadura = armadura; this.cor = cor;
    }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static EstiloPortal porId(String id) {
        if (id != null) for (EstiloPortal e : values()) if (e.name().equalsIgnoreCase(id)) return e;
        return CLASSICO;
    }
    public static EstiloPortal sortear() { return values()[java.util.concurrent.ThreadLocalRandom.current().nextInt(values().length)]; }
}
