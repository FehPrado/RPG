package br.rpgatributos.exploracao;

import java.util.Locale;

/**
 * As relíquias que a arqueologia desenterra. Cada uma conta um pedaço da história do
 * mundo, a mesma que o Cronista reconta na campanha.
 */
public enum Reliquia {
    MOEDA_DO_REI_CAIDO("Moeda do Rei Caído", "prize_pottery_sherd", 10,
            "Cunhada antes da última guerra.", "O rosto do rei foi raspado a faca."),
    LAMINA_PARTIDA("Lâmina Partida", "blade_pottery_sherd", 10,
            "Metade de uma espada do Ferreiro Antigo.", "Ainda corta, depois de mil anos."),
    TABUA_DE_FERRUM("Tábua de Ferrum", "miner_pottery_sherd", 9,
            "Uma prece entalhada na pedra:", "\"O fogo da forja lembra de tudo.\""),
    ESTATUETA_DE_SYLVA("Estatueta de Sylva", "sheaf_pottery_sherd", 9,
            "Raízes envolvem a figura de uma mulher.", "Brota musgo nela, mesmo no deserto."),
    CONCHA_DE_MARIS("Concha de Maris", "angler_pottery_sherd", 9,
            "Encostada no ouvido, ainda se ouve o mar.", "Achada a dias de qualquer praia."),
    ESTANDARTE_DE_BELLUM("Estandarte de Bellum", "arms_up_pottery_sherd", 8,
            "Pano rasgado de um exército", "que jurou nunca recuar."),
    PERGAMINHO_ARCANO("Pergaminho Arcano", "brewer_pottery_sherd", 8,
            "As fórmulas se apagaram.", "Só restou a palavra \"eco\"."),
    MASCARA_DE_MORTIS("Máscara de Mortis", "skull_pottery_sherd", 6,
            "Fria ao toque, mesmo ao sol.", "Quem a usa sonha com portas fechadas."),
    MAPA_DAS_ESTRELAS("Mapa das Estrelas", "explorer_pottery_sherd", 7,
            "Constelações que já não estão no céu.", "Ou que ainda não chegaram."),
    CHAVE_DA_TORRE("Chave da Torre", "shelter_pottery_sherd", 5,
            "Abre uma porta que ninguém encontrou.", "Os dentes mudam de forma sozinhos."),
    OLHO_DO_ARAUTO("Olho do Arauto", "danger_pottery_sherd", 4,
            "Dizem que o Arauto do Fim abriu", "os primeiros portais. E que ainda observa."),
    CORACAO_DE_SOMBRA("Coração de Sombra", "heartbreak_pottery_sherd", 3,
            "Pulsa quando os mortos se levantam.", "O último Soberano o deixou para trás.");

    private final String nome, modelo, linha1, linha2;
    private final int peso;

    Reliquia(String nome, String modelo, int peso, String linha1, String linha2) {
        this.nome = nome;
        this.modelo = modelo;
        this.peso = peso;
        this.linha1 = linha1;
        this.linha2 = linha2;
    }

    public String nome() { return nome; }
    public String modelo() { return modelo; }
    /** Quanto mais alto, mais comum. */
    public int peso() { return peso; }
    public String linha1() { return linha1; }
    public String linha2() { return linha2; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Reliquia porId(String s) {
        for (Reliquia r : values()) if (r.id().equalsIgnoreCase(s)) return r;
        return null;
    }
}
