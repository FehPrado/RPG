package br.rpgatributos.classe;

/**
 * Habilidades ativas das classes (agachar + F usa a selecionada; agachar + clique
 * esquerdo no ar troca). A força cresce com o nível da classe.
 */
public enum Habilidade {
    // Guerreiro
    INVESTIDA("Investida", 10, 0, "Avança e derruba quem estiver no caminho."),
    GRITO_DE_GUERRA("Grito de Guerra", 30, 0, "Você e a party ganham Força e Resistência; inimigos perto ficam fracos."),
    // Berserker
    FURIA("Fúria", 40, 0, "Força II e Pressa II por 10 s, mas você recebe +20% de dano."),
    REDEMOINHO("Redemoinho", 12, 0, "Gira a arma e acerta todos em volta."),
    // Cavaleiro
    CARGA("Carga", 12, 0, "Investida pesada (muito mais forte montado)."),
    BALUARTE("Baluarte", 35, 0, "Resistência III e atrai os monstros perto para você."),
    // Paladino
    LUZ_SAGRADA("Luz Sagrada", 25, 0, "Cura você e os aliados perto e queima os mortos-vivos."),
    ESCUDO_DIVINO("Escudo Divino", 45, 0, "Absorção III para você e os aliados perto."),
    // Arqueiro
    CHUVA_DE_FLECHAS("Chuva de Flechas", 20, 0, "Uma chuva de flechas cai onde você olha."),
    SALTO_EVASIVO("Salto Evasivo", 8, 0, "Salta para trás e ganha velocidade."),
    // Atirador de Elite
    TIRO_PERFURANTE("Tiro Perfurante", 10, 0, "Uma flecha rapidíssima que atravessa vários inimigos."),
    OLHO_DE_AGUIA("Olho de Águia", 30, 0, "Por 8 s suas flechas batem muito mais forte e os inimigos brilham."),
    // Caçador
    MARCA_DO_CACADOR("Marca do Caçador", 20, 0, "O alvo que você olha brilha e leva +30% de dano seu e dos seus companheiros."),
    ARMADILHA("Armadilha", 15, 0, "Arma uma armadilha no chão que prende e fere o primeiro monstro."),
    // Mago
    MISSIL_ARCANO("Míssil Arcano", 4, 15, "Um raio de energia no alvo que você olha."),
    BARREIRA_ARCANA("Barreira Arcana", 20, 30, "Absorção II por 8 s."),
    // Elementalista
    TEMPESTADE_ELEMENTAL("Tempestade Elemental", 25, 50, "Fogo, gelo e raio caem onde você olha."),
    ESCUDO_ELEMENTAL("Escudo Elemental", 30, 30, "Imune a fogo e resistente; quem te bater congela e pega fogo."),
    // Feiticeiro de Batalha
    LAMINA_ARCANA("Lâmina Arcana", 15, 20, "Os próximos 3 golpes dão dano mágico extra e devolvem mana."),
    PASSO_ARCANO("Passo Arcano", 10, 20, "Teleporta alguns blocos para a frente."),
    // Ladino
    BOMBA_DE_FUMACA("Bomba de Fumaça", 25, 0, "Fica invisível e cega os inimigos perto."),
    ROLAMENTO("Rolamento", 6, 0, "Rola para a direção em que anda e fica imune por um instante."),
    // Assassino
    PASSOS_DAS_SOMBRAS("Passos das Sombras", 12, 0, "Aparece nas costas do alvo que você olha."),
    GOLPE_LETAL("Golpe Letal", 20, 0, "O próximo golpe dá dano x2,5 (x4 se o alvo estiver fraco)."),
    // Ferreiro
    REPARO_RAPIDO("Reparo Rápido", 60, 0, "Conserta parte do item da sua mão."),
    ARMADURA_REFORCADA("Armadura Reforçada", 30, 0, "Resistência II por 8 s."),
    // Mestre Forjador
    AFIAR("Afiar", 90, 0, "Por 60 s a arma da mão bate +25% mais forte."),
    MARTELO_DO_TROVAO("Martelo do Trovão", 30, 0, "Um raio cai onde você olha e atordoa os inimigos."),
    // Domador
    CHAMADO("Chamado", 30, 0, "Seus companheiros correm até você e se curam."),
    FURIA_DA_MATILHA("Fúria da Matilha", 45, 0, "Seus companheiros ganham Força II e velocidade."),
    // Mestre das Feras
    RUGIDO("Rugido", 25, 0, "Inimigos perto ficam fracos, lentos e são empurrados."),
    VINCULO_VITAL("Vínculo Vital", 40, 0, "Cura metade da vida dos seus companheiros e os protege."),

    // ---------------- lendárias ----------------
    MARTELO_CELESTIAL("Martelo Celestial", 25, 0, "Um martelo de luz cai onde você olha: raio, dano enorme e arremesso."),
    BENCAO_DO_FERREIRO("Bênção do Ferreiro", 45, 0, "Você e a party ganham Força, Resistência e Pressa II por 20 s."),
    METEORO_ARCANO("Meteoro Arcano", 20, 60, "Um meteoro cai onde você olha e explode (sem quebrar blocos)."),
    TEMPO_SUSPENSO("Tempo Suspenso", 40, 50, "Os inimigos perto quase param no tempo e flutuam."),
    CHAMADO_ANCESTRAL("Chamado Ancestral", 60, 0, "Três lobos espirituais lutam ao seu lado por 30 s."),
    FORMA_BESTIAL("Forma Bestial", 45, 0, "Força II, Velocidade II, pulo alto e Regeneração por 15 s."),
    TERREMOTO("Terremoto", 18, 0, "Golpeia o chão: dano e arremessa os inimigos perto para o alto."),
    ESTANDARTE_DE_GUERRA("Estandarte de Guerra", 50, 0, "Você e a party ganham Força, Resistência e Regeneração por 20 s."),
    DANCA_DAS_LAMINAS("Dança das Lâminas", 20, 0, "Salta entre até 5 inimigos perto, cortando cada um."),
    VEU_SOMBRIO("Véu Sombrio", 35, 0, "Invisível por 8 s; o próximo golpe dá dano x3."),
    FLECHA_ESTELAR("Flecha Estelar", 12, 0, "Um raio de luz atravessa todos os inimigos na linha."),
    CHUVA_DE_ESTRELAS("Chuva de Estrelas", 30, 0, "Uma chuva de flechas brilhantes e raios cai onde você olha."),
    JULGAMENTO_DIVINO("Julgamento Divino", 20, 30, "Uma coluna de luz fere os inimigos (muito mais os mortos-vivos) e cura aliados."),
    AURA_SAGRADA("Aura Sagrada", 45, 40, "Você e a party ganham Regeneração II e Resistência e se livram de efeitos ruins."),
    // Soberano das Sombras
    LEVANTE_SE("Levante-se", 8, 20, "Os inimigos que você derrotou há pouco se levantam como sombras do seu exército."),
    EXERCITO_DAS_SOMBRAS("Exército das Sombras", 30, 40, "Chama as sombras guardadas; se já estão em campo, ficam furiosas e atacam o seu alvo.");

    private final String nome;
    private final int recarga;
    private final double mana;
    private final String descricao;

    Habilidade(String nome, int recarga, double mana, String descricao) {
        this.nome = nome;
        this.recarga = recarga;
        this.mana = mana;
        this.descricao = descricao;
    }

    public String nome() { return nome; }
    /** Recarga em segundos. */
    public int recarga() { return recarga; }
    /** Mana gasta (0 = não usa mana). */
    public double mana() { return mana; }
    public String descricao() { return descricao; }
}
