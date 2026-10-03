package br.rpgatributos.titulos;

import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Títulos: conquistas que o jogador desbloqueia e pode usar (um por vez).
 * Só o título em uso dá bônus, então nenhuma combinação fica forte demais.
 */
public enum Titulo {
    // ---------- atributos (níveis em fração do máximo: 0,5 = metade) ----------
    MINERADOR("Minerador", 0xB0B0B0, Requisito.nivel(Skill.MINERACAO, 0.5), Bonus.quebra(0.05)),
    LENHADOR("Lenhador", 0xC98A3C, Requisito.nivel(Skill.MADEIRA, 0.5), Bonus.quebra(0.05)),
    VELOCISTA("Velocista", 0x55FFFF, Requisito.nivel(Skill.CORRIDA, 0.5), Bonus.velocidade(0.03)),
    ACROBATA("Acrobata", 0x55FF55, Requisito.nivel(Skill.PULO, 0.5), Bonus.queda(2)),
    GUERREIRO("Guerreiro", 0xFF5555, Requisito.nivel(Skill.COMBATE, 0.5), Bonus.dano(1)),
    MESTRE_FERREIRO("Mestre Ferreiro", 0xE8823A, Requisito.nivel(Skill.FERRARIA, 0.5), Bonus.xpSkill(Skill.FERRARIA, 0.1)),
    ARQUIMAGO("Arquimago", 0xC77DFF, Requisito.nivel(Skill.ARCANO, 0.5), Bonus.mana(25)),
    NADADOR("Nadador", 0x3D8BFF, Requisito.nivel(Skill.NATACAO, 0.5), Bonus.folego(1)),
    FAZENDEIRO("Fazendeiro", 0x7BC043, Requisito.nivel(Skill.AGRICULTURA, 0.5), Bonus.xpSkill(Skill.AGRICULTURA, 0.1)),
    CHEF("Chef", 0xE8A33D, Requisito.nivel(Skill.CULINARIA, 0.5), Bonus.xpSkill(Skill.CULINARIA, 0.1)),
    DOMADOR("Domador", 0xC9955C, Requisito.nivel(Skill.DOMA, 0.5), Bonus.xpSkill(Skill.DOMA, 0.1)),
    DESPERTO("Desperto", 0xFFD54F, Requisito.contador("classes", 1, "Passar numa Prova de Classe"), Bonus.xpTudo(0.03)),
    MESTRE_DE_ARMAS("Mestre de Armas", 0xFFB300, Requisito.contador("classes_dominadas", 3, "Dominar 3 classes"), new Bonus().comDano(1).comVida(2)),
    VETERANO("Veterano", 0xAAAAAA, Requisito.nivelTotal(0.35), Bonus.vida(2)),
    GRAO_MESTRE("Grão-Mestre", 0xFFD23F, Requisito.algumMaximo(), Bonus.vida(4)),
    LENDA_VIVA("Lenda Viva", 0xFF4058, Requisito.todosMaximo(), new Bonus().comDano(2).comVida(8).comVelocidade(0.05)),

    // ---------- forja ----------
    APRENDIZ("Aprendiz de Ferreiro", 0xD8D8D8, Requisito.contador("forjados", 1, "Forjar 1 item"), new Bonus()),
    FERREIRO_LENDARIO("Ferreiro Lendário", 0xFFD23F, Requisito.contador("forjado_lendario", 1, "Forjar um item Lendário"), Bonus.armadura(1)),
    MAO_DE_DEUS("Mão de Deus", 0xFF4058, Requisito.contador("forjado_mitico", 1, "Forjar um item Mítico"), new Bonus().comDano(1).comArmadura(1)),
    DESBRAVADOR("Desbravador", 0x8D6E63, Requisito.contador("locais", 3, "Achar 3 Locais Ocultos"), Bonus.velocidade(0.03)),
    HERDEIRO_LENDARIO("Herdeiro Lendário", 0xFF8F00, Requisito.contador("classe_lendaria", 1, "Despertar uma classe lendária"),
            new Bonus().comDano(2).comVida(4)),
    VIAJANTE("Viajante", 0x9C7CFF, Requisito.contador("viagens", 50, "Viajar 50 vezes pelas Pedras de Viagem"), Bonus.xpTudo(0.03)),
    PORTADOR_DE_LENDA("Portador de Lenda", 0xFF5FD7, Requisito.contador("lendas", 1, "Forjar uma lenda"), new Bonus().comDano(1).comVida(2)),
    REFINADOR("Refinador", 0xFFAA00, Requisito.contador("refino10", 1, "Refinar um item até +10"), Bonus.armadura(2)),
    RECICLADOR("Reciclador", 0x9ACD32, Requisito.contador("reciclados", 50, "Reciclar 50 itens"), Bonus.xpSkill(Skill.FERRARIA, 0.05)),

    // ---------- fazenda e cozinha ----------
    BOTANICO("Botânico", 0x5BD15B, Requisito.contador("mutacoes", 1, "Ver nascer uma semente mutante"), Bonus.xpSkill(Skill.AGRICULTURA, 0.15)),
    MESTRE_CUCA("Mestre-Cuca", 0xFFD23F, Requisito.contador("banquete", 1, "Cozinhar um Banquete Lendário"), Bonus.vida(2)),

    // ---------- domador ----------
    SENHOR_DAS_FERAS("Senhor das Feras", 0x8B5A2B, Requisito.contador("voadores", 1, "Dar Asas a um companheiro"), Bonus.velocidade(0.03)),

    // ---------- arcano ----------
    INICIADO("Iniciado", 0xB66BFF, Requisito.contador("infusoes", 1, "Fazer a primeira infusão"), Bonus.mana(10)),
    CORPO_DIVINO("Corpo Divino", 0xC77DFF, Requisito.contador("corpo_completo", 1, "Ter as 5 partes do corpo infundidas"), Bonus.regenMana(1)),
    SABIO("Sábio", 0x9B6BFF, Requisito.contador("secretas", 10, "Descobrir 10 magias secretas"), Bonus.mana(25)),
    ONISCIENTE("Onisciente", 0xFF55FF, Requisito.todasSecretas(), Bonus.regenMana(2)),
    PORTADOR_LENDARIO("Portador Lendário", 0xFFD23F, Requisito.contador("nucleos", 1, "Infundir um núcleo de chefe"), Bonus.vida(2)),

    // ---------- aventura ----------
    CACADOR_DE_CHEFES("Caçador de Chefes", 0xFF7F50, Requisito.contador("chefes", 1, "Derrotar um chefe do Altar"), Bonus.dano(0.5)),
    MATA_GIGANTES("Mata-Gigantes", 0xFF4500, Requisito.todosOsChefes(), new Bonus().comDano(1).comVida(4)),
    FIM_DOS_TEMPOS("Fim dos Tempos", 0x8B00FF, Requisito.contador("chefe_arauto", 1, "Derrotar o Arauto do Fim"), new Bonus().comDano(1).comArmadura(2)),
    EXPLORADOR("Explorador de Masmorras", 0x7E57C2, Requisito.contador("masmorras", 1, "Vencer uma masmorra"), Bonus.armadura(1)),
    SENHOR_DAS_MASMORRAS("Senhor das Masmorras", 0xAA00AA, Requisito.contador("masmorra_pesadelo", 1, "Vencer uma masmorra Pesadelo"),
            new Bonus().comDano(1).comVida(4)),
    SOBREVIVENTE("Sobrevivente", 0xAA5500, Requisito.contador("ondas", 1, "Vencer as Ondas de Monstros"), Bonus.armadura(1)),
    MATADOR_DE_DRAGOES("Matador de Dragões", 0xAA00AA, Requisito.contador("dragao", 1, "Ajudar a derrotar o Dragão do Fim"), Bonus.dano(1)),

    // ---------- aldeões ----------
    AMIGO_DA_VILA("Amigo da Vila", 0x55FF55, Requisito.contador("missoes", 10, "Completar 10 missões de aldeões"), Bonus.xpTudo(0.05)),
    HEROI_DO_POVO("Herói do Povo", 0x00AA00, Requisito.contador("missoes", 50, "Completar 50 missões de aldeões"), new Bonus().comHeroi()),
    CACADOR_DE_RECOMPENSAS("Caçador de Recompensas", 0xFFAA00, Requisito.contador("contratos", 5, "Cumprir 5 contratos do Caçador"), Bonus.dano(1)),

    // ---------- party e territórios ----------
    COMPANHEIRO("Companheiro", 0x55CDFC, Requisito.contador("abates_party", 200, "Derrotar 200 monstros com a party por perto"), Bonus.xpSkill(Skill.COMBATE, 0.1)),
    FUNDADOR("Fundador", 0x4CAF50, Requisito.contador("territorio", 1, "Fundar um território"), Bonus.armadura(1)),
    SENHOR_DAS_TERRAS("Senhor das Terras", 0x2E7D32, Requisito.contador("chunks", 40, "Ter um território com 40 chunks"), new Bonus().comVida(2).comArmadura(1));

    /** Títulos tão difíceis que o servidor inteiro fica sabendo. */
    private static final List<Titulo> ANUNCIADOS = List.of(
            GRAO_MESTRE, LENDA_VIVA, MAO_DE_DEUS, REFINADOR, ONISCIENTE, MATA_GIGANTES, FIM_DOS_TEMPOS,
            MATADOR_DE_DRAGOES, HEROI_DO_POVO, SENHOR_DAS_MASMORRAS, HERDEIRO_LENDARIO);

    private final String nome;
    private final TextColor cor;
    private final Requisito requisito;
    private final Bonus bonus;

    Titulo(String nome, int cor, Requisito requisito, Bonus bonus) {
        this.nome = nome;
        this.cor = TextColor.color(cor);
        this.requisito = requisito;
        this.bonus = bonus;
    }

    public String nome() { return nome; }
    public TextColor cor() { return cor; }
    public Requisito requisito() { return requisito; }
    public Bonus bonus() { return bonus; }
    public boolean anunciado() { return ANUNCIADOS.contains(this); }

    public static Titulo porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // =====================================================================
    //  Requisito
    // =====================================================================

    /** O que precisa para desbloquear. Tipos simples para caber no enum. */
    public record Requisito(Tipo tipo, Skill skill, double fracao, String contador, int quantidade, String texto) {
        public enum Tipo { NIVEL, NIVEL_TOTAL, ALGUM_MAXIMO, TODOS_MAXIMO, CONTADOR, TODOS_CHEFES, TODAS_SECRETAS }

        static Requisito todasSecretas() {
            return new Requisito(Tipo.TODAS_SECRETAS, null, 0, "secretas", 0, "Descobrir todas as magias secretas");
        }

        static Requisito nivel(Skill s, double fracao) {
            return new Requisito(Tipo.NIVEL, s, fracao, null, 0, null);
        }

        static Requisito nivelTotal(double fracao) {
            return new Requisito(Tipo.NIVEL_TOTAL, null, fracao, null, 0, null);
        }

        static Requisito algumMaximo() {
            return new Requisito(Tipo.ALGUM_MAXIMO, null, 1, null, 0, "Chegar ao nível máximo em qualquer atributo");
        }

        static Requisito todosMaximo() {
            return new Requisito(Tipo.TODOS_MAXIMO, null, 1, null, 0, "Chegar ao nível máximo em TODOS os atributos");
        }

        static Requisito contador(String id, int qtd, String texto) {
            return new Requisito(Tipo.CONTADOR, null, 0, id, qtd, texto);
        }

        static Requisito todosOsChefes() {
            return new Requisito(Tipo.TODOS_CHEFES, null, 0, null, 0, "Derrotar cada um dos chefes do Altar");
        }

        /** Texto do requisito (níveis dependem do nível máximo do servidor). */
        public String descrever(int nivelMaximo) {
            return switch (tipo) {
                case NIVEL -> skill.nome() + " nível " + (int) Math.ceil(fracao * nivelMaximo);
                case NIVEL_TOTAL -> "Nível total " + (int) Math.ceil(fracao * nivelMaximo * Skill.values().length);
                default -> texto;
            };
        }
    }

    // =====================================================================
    //  Bônus
    // =====================================================================

    /** Bônus do título em uso. Vida em pontos (2 = 1 coração); velocidade/quebra/XP em fração. */
    public static final class Bonus {
        double vida, dano, armadura, velocidade, quebra, queda, folego, mana, regenMana, xpTudo, xpSkill;
        Skill skill;
        boolean heroi;

        static Bonus vida(double v) { return new Bonus().comVida(v); }
        static Bonus dano(double v) { return new Bonus().comDano(v); }
        static Bonus armadura(double v) { return new Bonus().comArmadura(v); }
        static Bonus velocidade(double v) { return new Bonus().comVelocidade(v); }
        static Bonus quebra(double v) { Bonus b = new Bonus(); b.quebra = v; return b; }
        static Bonus queda(double v) { Bonus b = new Bonus(); b.queda = v; return b; }
        static Bonus folego(double v) { Bonus b = new Bonus(); b.folego = v; return b; }
        static Bonus mana(double v) { Bonus b = new Bonus(); b.mana = v; return b; }
        static Bonus regenMana(double v) { Bonus b = new Bonus(); b.regenMana = v; return b; }
        static Bonus xpTudo(double v) { Bonus b = new Bonus(); b.xpTudo = v; return b; }
        static Bonus xpSkill(Skill s, double v) { Bonus b = new Bonus(); b.skill = s; b.xpSkill = v; return b; }

        Bonus comVida(double v) { vida = v; return this; }
        Bonus comDano(double v) { dano = v; return this; }
        Bonus comArmadura(double v) { armadura = v; return this; }
        Bonus comVelocidade(double v) { velocidade = v; return this; }
        Bonus comHeroi() { heroi = true; return this; }

        public double vida() { return vida; }
        public double dano() { return dano; }
        public double armadura() { return armadura; }
        public double velocidade() { return velocidade; }
        public double quebra() { return quebra; }
        public double queda() { return queda; }
        public double folego() { return folego; }
        public double mana() { return mana; }
        public double regenMana() { return regenMana; }
        public boolean heroi() { return heroi; }

        public double multiplicadorXp(Skill s) {
            return 1 + xpTudo + (s == skill ? xpSkill : 0);
        }

        public String texto() {
            List<String> l = new ArrayList<>();
            if (vida > 0) l.add("+" + StatsManager.fmt(vida / 2) + " ❤");
            if (dano > 0) l.add("+" + StatsManager.fmt(dano) + " de dano");
            if (armadura > 0) l.add("+" + StatsManager.fmt(armadura) + " de armadura");
            if (velocidade > 0) l.add("+" + StatsManager.fmt(velocidade * 100) + "% de velocidade");
            if (quebra > 0) l.add("+" + StatsManager.fmt(quebra * 100) + "% de velocidade de quebra");
            if (queda > 0) l.add("+" + StatsManager.fmt(queda) + " blocos de queda segura");
            if (folego > 0) l.add("+" + StatsManager.fmt(folego) + " de fôlego debaixo d'água");
            if (mana > 0) l.add("+" + StatsManager.fmt(mana) + " de mana");
            if (regenMana > 0) l.add("+" + StatsManager.fmt(regenMana) + " de mana/s");
            if (xpTudo > 0) l.add("+" + StatsManager.fmt(xpTudo * 100) + "% de XP em tudo");
            if (xpSkill > 0) l.add("+" + StatsManager.fmt(xpSkill * 100) + "% de XP de " + skill.nome());
            if (heroi) l.add("Herói da Vila permanente (descontos)");
            return l.isEmpty() ? "Só o título (sem bônus)" : String.join(", ", l);
        }
    }
}
