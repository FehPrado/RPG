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
    /** Não é sorteada por chunk: a {@link IlhasDoCeu} escolhe o lugar (uma por região, em terra firme, lá no alto). */
    ILHA_DO_CEU("Ilha do Céu", "a", 0x90CAF9, "Lá em cima, o vento ainda lembra de quem morou entre as nuvens.",
            0, 30, 99, 0, b -> !b.contains("ocean") && !b.contains("river"));

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
        if (this == FAROL || submersa()) return this.bioma.test(bioma); // o farol é justamente na praia; os do mar, no mar
        return !agua(bioma) && this.bioma.test(bioma);
    }

    /** Fica no fundo do mar. */
    public boolean submersa() {
        return this == NAUFRAGIO || this == CIDADE_SUBMERSA;
    }

    /** Construída sobre palafitas: aceita água rasa no chão e não aterra nada. */
    public boolean palafita() {
        return this == CABANA_DA_BRUXA;
    }

    /** Tem um morador (NPC) que negocia. */
    public boolean temMorador() {
        return this == CABANA_DO_EREMITA || this == OASIS || this == CABANA_DA_BRUXA;
    }

    public static Estrutura sortear(Random r) {
        int total = 0;
        for (Estrutura e : values()) total += e.peso;
        int v = r.nextInt(total);
        for (Estrutura e : values()) {
            v -= e.peso;
            if (v < 0) return e;
        }
        return TORRE_DE_VIGIA;
    }

    public static Estrutura porId(String s) {
        for (Estrutura e : values()) if (e.id().equalsIgnoreCase(s) || e.name().replace("_", "").equalsIgnoreCase(s)) return e;
        return null;
    }
}
