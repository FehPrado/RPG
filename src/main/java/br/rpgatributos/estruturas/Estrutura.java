package br.rpgatributos.estruturas;

import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

/**
 * As estruturas que o mundo ganha sozinho, uma por chunk novo (bem de vez em quando).
 * {@code raio}: metade do tamanho (cabe sempre dentro de um chunk); {@code desnivel}: quanto o
 * chão pode variar para ela caber sem ficar feia; {@code altura}: espaço livre acima do chão.
 */
public enum Estrutura {
    TORRE_DE_VIGIA("Torre de Vigia Abandonada", "a", 0x9E9E9E, "Os vigias partiram. Algo ainda guarda o alto.",
            5, 4, 3, 20, b -> !agua(b) && !b.contains("swamp")),
    ACAMPAMENTO("Acampamento de Bandidos", "um", 0xC62828, "Fogo aceso, tendas armadas... e ninguém de boa índole.",
            4, 7, 2, 6, b -> b.contains("plains") || b.contains("forest") || b.contains("taiga") || b.contains("savanna")
            || b.contains("meadow") || b.contains("grove") || b.contains("badlands")),
    CEMITERIO("Cemitério Antigo", "um", 0x78909C, "Nomes apagados pelo tempo. À noite, a terra se mexe.",
            4, 7, 2, 7, b -> b.contains("plains") || b.contains("forest") || b.contains("taiga") || b.contains("meadow")
            || b.contains("snowy") && !b.contains("peaks") || b.contains("swamp")),
    MINA("Mina Abandonada", "uma", 0x8D6E63, "Fechada depois do desabamento. Dizem que ficou muito lá embaixo.",
            3, 6, 3, 6, b -> !agua(b) && !b.contains("swamp") && !b.contains("desert") && !b.contains("jungle")),
    POCO_DOS_DESEJOS("Poço dos Desejos", "um", 0x4FC3F7, "Jogue uma esmeralda na água e faça um pedido.",
            4, 4, 2, 7, b -> b.contains("plains") || b.contains("forest") || b.contains("meadow") || b.contains("savanna")
            || b.contains("cherry") || b.contains("birch") || b.contains("taiga")),
    CABANA_DO_EREMITA("Cabana do Eremita", "a", 0x7CB342, "Alguém escolheu viver longe de tudo. E sabe muita coisa.",
            3, 6, 2, 9, b -> b.contains("forest") || b.contains("taiga") || b.contains("swamp") || b.contains("jungle")
            || b.contains("grove") || b.contains("birch") || b.contains("cherry")),
    SANTUARIO_ESQUECIDO("Santuário Esquecido", "um", 0xFFD54F, "Um altar antigo a um dos seis deuses.",
            3, 5, 2, 7, b -> !agua(b)),
    // ---------- 2.25 ----------
    OASIS("Oásis do Deserto", "um", 0x26C6DA, "Água, sombra e um mercador que não tem pressa.",
            4, 6, 2, 10, b -> b.contains("desert")),
    CABANA_DA_BRUXA("Cabana da Bruxa", "a", 0x7E57C2, "De dia ela vende. De noite, melhor não bater na porta.",
            4, 5, 2, 11, b -> b.contains("swamp")),
    FAROL("Farol Abandonado", "o", 0xEF5350, "A lanterna apagou faz tempo. Talvez ainda acenda.",
            4, 4, 2, 21, b -> b.contains("beach") || b.contains("shore")),
    EXPEDICAO("Acampamento da Expedição Perdida", "o", 0xB3E5FC, "Eles foram para o norte e não voltaram.",
            3, 6, 2, 6, b -> b.contains("snowy") || b.contains("ice") || b.contains("frozen") || b.contains("grove")),
    VILA_SAQUEADA("Vila Saqueada", "a", 0x8D6E63, "Ainda sai fumaça das casas.",
            3, 7, 2, 8, b -> b.contains("plains") && !b.contains("snowy") || b.contains("savanna") || b.contains("meadow") || b.contains("taiga") && !b.contains("snowy")),
    TORRE_DO_MAGO("Torre do Mago em Ruínas", "a", 0x9575CD, "Os livros ainda sussurram lá em cima.",
            3, 4, 3, 21, b -> b.contains("forest") || b.contains("taiga") || b.contains("birch") || b.contains("plains") || b.contains("meadow") || b.contains("cherry")),
    CIRCULO_DE_PEDRAS("Círculo de Pedras Antigas", "o", 0xB0BEC5, "Na lua cheia, as pedras lembram.",
            3, 6, 2, 6, b -> !b.contains("desert") && !b.contains("jungle") && !b.contains("swamp")),
    FORJA_DOS_ANOES("Forja dos Anões", "a", 0xFF8A65, "O fogo daqui nunca apagou de verdade.",
            3, 6, 4, 8, b -> b.contains("windswept") || b.contains("meadow") || b.contains("grove") || b.contains("badlands")
            || b.contains("stony") || b.contains("savanna_plateau") || b.contains("slopes") || b.contains("taiga")),
    FORTIM("Fortim em Ruínas", "o", 0x90A4AE, "Os soldados ainda montam guarda. Mesmo mortos.",
            2, 7, 2, 10, b -> b.contains("plains") || b.contains("forest") || b.contains("taiga") || b.contains("savanna") || b.contains("meadow")),
    // ---------- 2.29: o mar ----------
    NAUFRAGIO("Naufrágio", "um", 0x4DB6AC, "Cada navio no fundo tem uma história. Este deixou um diário.",
            6, 7, 4, 14, b -> b.contains("ocean")),
    /** Não é sorteada por chunk: a {@link CidadesSubmersas} escolhe o lugar (uma por região, no mar fundo). */
    CIDADE_SUBMERSA("Cidade Submersa", "a", 0x26C6DA, "Ela adorava Maris. Um dia, o mar a levou de volta.",
            0, 36, 12, 24, b -> b.contains("ocean")),
    // ---------- 2.33: o céu ----------
    // ---------------- 2.45: vilas com vários moradores (veja Vilas) ----------------
    VILA_PESCADORES("Vila de Pescadores", "a", 0x4FC3F7, "Redes secando, cheiro de mar e alguém sempre contando uma história de pescador.",
            3, 7, 2, 9, b -> b.contains("beach") || b.contains("river")),
    MOSTEIRO("Mosteiro da Montanha", "o", 0xE57373, "O sino toca três vezes ao dia. Ninguém aqui tem pressa.",
            3, 7, 2, 12, b -> b.contains("meadow") || b.contains("grove") || b.contains("cherry") || b.contains("windswept") || b.contains("snowy_slopes")),
    ALDEIA_ANOES("Aldeia dos Anões", "a", 0xFFB74D, "Martelos, cerveja forte e uma mina que desce até onde ninguém sabe.",
            3, 7, 4, 10, b -> b.contains("windswept") || b.contains("stony") || b.contains("badlands") || b.contains("grove") || b.contains("jagged")),
    VILA_ELFICA("Vila Élfica do Bosque", "a", 0x81C784, "As árvores aqui têm nome, e os moradores lembram de todos.",
            3, 7, 2, 14, b -> b.contains("dark_forest") || b.contains("birch") || b.contains("old_growth") || b.contains("flower_forest") || b.equals("forest")),
    /** Não é sorteada por chunk: a {@link IlhasDoCeu} escolhe o lugar (uma por região, em terra firme, lá no alto). */
    ILHA_DO_CEU("Ilha do Céu", "a", 0x90CAF9, "Lá em cima, o vento ainda lembra de quem morou entre as nuvens.",
            0, 30, 99, 0, b -> !b.contains("ocean") && !b.contains("river")),
    // ---------------- 2.56: o Nether (só nascem lá; veja nether()) ----------------
    FORJA_PIGLIN("Forja dos Piglins", "a", 0xFFB300, "Ouro, fogo e um ferreiro que só confia em quem paga.",
            6, 6, 3, 8, b -> b.contains("crimson") || b.contains("wastes")),
    PRIMEIRO_PORTAL("Primeiro Portal em Ruínas", "o", 0xB388FF, "Foi por aqui que alguém abriu, pela primeira vez, o caminho entre os mundos.",
            2, 5, 3, 10, b -> !b.contains("basalt")),
    // ---------------- 3.0: o mar aberto (na superfície, longe da costa; veja MarAberto) ----------------
    ILHOTA("Ilhota", "a", 0xFFE082, "Areia, uma palmeira e o mar até onde a vista alcança.",
            6, 7, 0, 12, b -> b.contains("ocean") && !b.contains("frozen")),
    NAVIO_PIRATA("Navio Pirata", "o", 0xC62828, "Bandeira negra no mastro. Eles já te viram.",
            3, 7, 0, 16, b -> b.contains("ocean") && !b.contains("frozen")),
    ICEBERG("Iceberg", "o", 0xB3E5FC, "Uma montanha de gelo à deriva. Tem alguém lá dentro.",
            5, 6, 0, 14, b -> b.contains("frozen") || b.contains("cold")),
    BARCO_MERCANTE("Barco Mercante", "o", 0x81D4FA, "Um mercador ancorado no meio do mar. Vende o que serve para navegar.",
            4, 5, 0, 12, b -> b.contains("ocean") && !b.contains("frozen")),
    ILHA_TARTARUGA("Ilha-Tartaruga", "a", 0x66BB6A, "Uma ilha redonda demais. E ela respira.",
            2, 7, 0, 8, b -> (b.contains("warm") || b.contains("lukewarm") || b.equals("ocean") || b.equals("deep_ocean"))),
    // ---------------- 3.1: as grandes, do datapack (o jogo gera com o mundo; veja Gigantes) ----------------
    ARVORE_MUNDO("Árvore-Mundo", "a", 0x66BB6A, "Ela já era velha quando o mundo era novo. Algo dorme nas raízes.",
            0, 30, 0, 0, b -> true),
    COLOSSO_CAIDO("Colosso Caído", "o", 0xB0BEC5, "Um gigante de pedra que caiu e nunca mais levantou. O coração ainda bate.",
            0, 34, 0, 0, b -> true),
    CIDADE_ANA("Cidade Anã de Pedra Funda", "a", 0xFFB74D, "Os anões cavaram fundo demais. O rei nunca deixou o salão.",
            0, 24, 0, 0, b -> true),
    OSSADA_DRAGAO("Ossada do Dragão", "a", 0xE8E2CC, "Os ossos de um dragão antigo. À noite, o espírito dele volta.",
            0, 31, 0, 0, b -> true),
    COLISEU("Coliseu", "o", 0xE57373, "A arena ainda espera os lutadores. E a plateia, os vencedores.",
            0, 29, 0, 0, b -> true),
    CASTELO_REI_CAIDO("Castelo do Rei Caído", "o", 0x9575CD, "O rei morreu sentado no trono. Ninguém contou isso a ele.",
            0, 22, 0, 0, b -> true);

    private final String nome, artigo, frase;
    private final TextColor cor;
    private final int peso, raio, desnivel, altura;
    private final Predicate<String> bioma;

    Estrutura(String nome, String artigo, int cor, String frase, int peso, int raio, int desnivel, int altura, Predicate<String> bioma) {
        this.nome = nome;
        this.artigo = artigo;
        this.cor = TextColor.color(cor);
        this.frase = frase;
        this.peso = peso;
        this.raio = raio;
        this.desnivel = desnivel;
        this.altura = altura;
        this.bioma = bioma;
    }

    private static boolean agua(String b) {
        return b.contains("ocean") || b.contains("river") || b.contains("beach") || b.contains("shore") || b.contains("mushroom");
    }

    public String nome() { return nome; }
    /** "um", "uma" ou "a" (para "Encontrou a Torre..."). */
    public String artigo() { return artigo; }
    public String frase() { return frase; }
    public TextColor cor() { return cor; }
    public int raio() { return raio; }
    public int desnivel() { return desnivel; }
    public int altura() { return altura; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** "uma Torre..." / "um Cemitério..." para os boatos. */
    public String comArtigoIndefinido() {
        return (artigo.equals("a") || artigo.equals("uma") ? "uma " : "um ") + nome;
    }

    public boolean aceita(String bioma) {
        if (this == FAROL || this == VILA_PESCADORES || submersa() || nether() || marAberto()) return this.bioma.test(bioma); // o farol e a vila de pescadores são na praia; os do mar, no mar
        return !agua(bioma) && this.bioma.test(bioma);
    }

    /** 2.56: nasce no Nether (e só lá). */
    public boolean nether() {
        return this == FORJA_PIGLIN || this == PRIMEIRO_PORTAL;
    }

    /** Fica no fundo do mar. */
    public boolean submersa() {
        return this == NAUFRAGIO || this == CIDADE_SUBMERSA;
    }

    /** 3.0: boia na superfície do mar aberto (o y dela é a água da superfície). */
    public boolean marAberto() {
        return this == ILHOTA || this == NAVIO_PIRATA || this == ICEBERG || this == BARCO_MERCANTE || this == ILHA_TARTARUGA;
    }

    /** 3.1: vem do datapack (o jogo gera junto com o mundo; o plugin só reconhece e dá vida). */
    public boolean gigante() {
        return this == ARVORE_MUNDO || this == COLOSSO_CAIDO || this == CIDADE_ANA || this == OSSADA_DRAGAO || this == COLISEU || this == CASTELO_REI_CAIDO;
    }

    /** É do mar (nasce em chunk de oceano). */
    public boolean doMar() {
        return submersa() || marAberto();
    }

    /** Construída sobre palafitas: aceita água rasa no chão e não aterra nada. */
    public boolean palafita() {
        return this == CABANA_DA_BRUXA || this == VILA_PESCADORES;
    }

    /** Tem um morador (NPC) que negocia. */
    /** Vila com vários moradores (NPCs com loja, serviço e laço; veja Vilas). */
    public boolean vila() {
        return this == VILA_PESCADORES || this == MOSTEIRO || this == ALDEIA_ANOES || this == VILA_ELFICA || this == FORJA_PIGLIN;
    }

    public boolean temMorador() {
        return this == CABANA_DO_EREMITA || this == OASIS || this == CABANA_DA_BRUXA || this == BARCO_MERCANTE;
    }

    /**
     * Sorteia pelo peso: com {@code nether}, entre as do Nether; com {@code mar} (3.0: chunk de
     * oceano), entre as do mar; senão, entre as de terra.
     */
    public static Estrutura sortear(Random r, boolean nether, boolean mar) {
        int total = 0;
        for (Estrutura e : values()) if (serve(e, nether, mar)) total += e.peso;
        if (total <= 0) return nether ? FORJA_PIGLIN : TORRE_DE_VIGIA;
        int v = r.nextInt(total);
        for (Estrutura e : values()) {
            if (!serve(e, nether, mar)) continue;
            v -= e.peso;
            if (v < 0) return e;
        }
        return nether ? FORJA_PIGLIN : TORRE_DE_VIGIA;
    }

    private static boolean serve(Estrutura e, boolean nether, boolean mar) {
        if (e.peso <= 0) return false;
        if (nether) return e.nether();
        return !e.nether() && e.doMar() == mar;
    }

    public static Estrutura porId(String s) {
        for (Estrutura e : values()) if (e.id().equalsIgnoreCase(s) || e.name().replace("_", "").equalsIgnoreCase(s)) return e;
        return null;
    }
}
