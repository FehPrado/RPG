package br.rpgatributos.exploracao;

/**
 * Os capítulos da campanha do Cronista. Cada um conta um pedaço da história do mundo e
 * pede algo de um sistema que já existe (o contador de conquistas que mede isso).
 */
public enum Capitulo {
    AVENTUREIRO("O Aventureiro", null, 50, "Chegar ao nível total 50",
            "Antes de qualquer história, alguém precisa sobreviver a ela.",
            "Este mundo já teve reis, deuses e dragões. Hoje tem você. Mostre que é capaz de durar o bastante para ouvir o resto."),
    FORJA("O Fogo da Forja", "forjados", 5, "Forjar 5 itens na Forja do Ferreiro",
            "Na primeira era, o Ferreiro Antigo forjava armas para os próprios deuses.",
            "Dizem que Ferrum soprava a brasa e que cada martelada ecoava até o céu. A Lâmina Partida que às vezes se desenterra é dele. Forje algo digno."),
    ECOS("Ecos Arcanos", "magias", 100, "Lançar 100 magias",
            "A magia nasceu no dia em que alguém escutou o eco das essências.",
            "Fogo, gelo, vento, sombra... tudo fala, para quem sabe ouvir. Hoje, poucos ouvem. Lance magias até ouvir também."),
    DEUSES("Os Deuses Esquecidos", "devocao_maxima", 2, "Chegar à fé 2 com um deus",
            "Seis deuses dividiam o mundo: forja, natureza, mar, guerra, magia e morte.",
            "Quando os reis pararam de rezar, eles se calaram. Os santuários viraram ruína. Faça um deles falar de novo."),
    MASMORRAS("Sob a Terra", "masmorras", 3, "Vencer 3 masmorras",
            "O Rei Caído não aceitou perder a guerra. Enterrou o seu tesouro em salões sem fim.",
            "E encheu cada salão de monstros, para que ninguém o levasse. São as masmorras que vocês abrem hoje pelos portais."),
    ESTRELAS("O Céu Fala", "constelacoes", 4, "Observar 4 constelações",
            "Os antigos liam o futuro nas estrelas.",
            "Algumas constelações sumiram na noite em que o Arauto do Fim abriu o primeiro portal. Olhe para o céu e me diga quais ainda estão lá."),
    RUINAS("As Ruínas Contam", "reliquias", 6, "Juntar 6 relíquias diferentes",
            "Cada pedra caída guarda uma frase, cada moeda guarda um rosto.",
            "Pincele as ruínas pelo mundo e traga as relíquias para a sua coleção. Eu leio para você o que elas dizem."),
    SELO("O Selo", "selos", 1, "Romper o selo de um Local Oculto",
            "Os últimos guardiões esconderam o que restou da primeira era nos Locais Ocultos.",
            "Cada um tem um selo, e cada selo, uma classe esquecida. Rompa um selo. Então a história estará completa... ou vai começar de novo.");

    private final String titulo, contador, objetivo, abertura, historia;
    private final int meta;

    Capitulo(String titulo, String contador, int meta, String objetivo, String abertura, String historia) {
        this.titulo = titulo;
        this.contador = contador;
        this.meta = meta;
        this.objetivo = objetivo;
        this.abertura = abertura;
        this.historia = historia;
    }

    public String titulo() { return titulo; }
    /** Contador de conquistas que mede a tarefa (null = nível total). */
    public String contador() { return contador; }
    public int meta() { return meta; }
    public String objetivo() { return objetivo; }
    public String abertura() { return abertura; }
    public String historia() { return historia; }
}
