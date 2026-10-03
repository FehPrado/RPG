package br.rpgatributos.arcano;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static br.rpgatributos.arcano.Essencia.*;

/**
 * Magias secretas: combinações exatas de essências + forma com efeito único.
 * Os jogadores descobrem criando a magia no grimório. O efeito fica em Conjuracao.
 */
public enum Receita {
    // ---------- pares ----------
    BOLA_DE_FOGO("Bola de Fogo", Forma.PROJETIL, Material.FIRE_CHARGE, 3,
            "Dispara uma bola de fogo de verdade, que incendeia quem acertar.", FOGO, VENTO),
    NEVASCA("Nevasca", Forma.AURA, Material.POWDER_SNOW_BUCKET, 20,
            "Uma tempestade de neve congela e desacelera tudo em volta.", GELO, VENTO),
    PASSO_DO_VAZIO("Passo do Vazio", Forma.CORPO, Material.ENDER_EYE, 6,
            "Teleporta você até 12 blocos para a frente, sem dano de queda.", VAZIO, VENTO),
    CORACAO_PULSANTE("Coração Pulsante", Forma.CORPO, Material.GOLDEN_APPLE, 45,
            "Cura muito, dá Absorção II e Regeneração II.", VIDA, ENERGIA),
    DRENAR("Drenar", Forma.TOQUE, Material.WITHER_ROSE, 8,
            "Rouba a vida do alvo e cura você na mesma quantidade.", SOMBRA, VIDA),
    RAIZES("Raízes", Forma.PROJETIL, Material.HANGING_ROOTS, 15,
            "Onde cair, raízes prendem os inimigos no chão.", NATUREZA, TERRA),
    VAPOR("Vapor", Forma.AURA, Material.CAMPFIRE, 20,
            "Nuvem de vapor que cega inimigos, apaga o fogo e regenera aliados.", FOGO, AGUA),
    ARVORE_INSTANTANEA("Árvore Instantânea", Forma.CRIACAO, Material.OAK_SAPLING, 20,
            "Faz uma árvore inteira nascer na hora.", NATUREZA, VIDA),
    PONTE_DE_GELO("Ponte de Gelo", Forma.CRIACAO, Material.PACKED_ICE, 10,
            "Cria uma ponte de gelo à sua frente por 30 segundos.", GELO, AGUA),
    IMA_DO_VAZIO("Ímã do Vazio", Forma.AURA, Material.LODESTONE, 10,
            "Puxa itens e orbes de XP de até 16 blocos até você.", VAZIO, TERRA),
    BASTIAO("Bastião", Forma.CORPO, Material.SHIELD, 40,
            "Resistência II e devolve 30% do dano recebido por 10 segundos.", TERRA, ENERGIA),
    TERREMOTO("Terremoto", Forma.AURA, Material.COBBLED_DEEPSLATE, 15,
            "O chão treme em volta de você e joga os inimigos para cima.", TERRA, VENTO),
    CORRENTE_ELETRICA("Corrente Elétrica", Forma.PROJETIL, Material.COPPER_INGOT, 10,
            "Um raio que pula de inimigo em inimigo (até 5).", ENERGIA, AGUA),
    ESCUDO_DE_GELO("Escudo de Gelo", Forma.CORPO, Material.BLUE_ICE, 30,
            "Uma armadura de gelo: Absorção III e Resistência por 30 segundos.", GELO, TERRA),
    CHAMA_SAGRADA("Chama Sagrada", Forma.AURA, Material.SOUL_LANTERN, 20,
            "Fogo que queima mortos-vivos e cura os aliados em volta.", VIDA, FOGO),
    ESPINHEIRO("Espinheiro", Forma.CRIACAO, Material.SWEET_BERRIES, 20,
            "Faz brotar um campo de arbustos espinhosos e venenosos.", NATUREZA, SOMBRA),
    RETORNO("Retorno", Forma.CORPO, Material.RECOVERY_COMPASS, 300,
            "Depois de 3 segundos parado, leva você para a sua cama (ou para o spawn).", VAZIO, VIDA),
    CHAMAR_CHUVA("Chamar a Chuva", Forma.AURA, Material.WATER_BUCKET, 300,
            "Faz chover por 5 minutos (bom para plantações e para o Chamado da Tempestade).", AGUA, VENTO),
    OLHO_VIGILANTE("Olho Vigilante", Forma.AURA, Material.ENDER_EYE, 30,
            "Revela todas as criaturas em 40 blocos (Brilho por 30 segundos).", SOMBRA, ENERGIA),

    // ---------- trios ----------
    FARDO_FERTIL("Fardo Fértil", Forma.CRIACAO, Material.HAY_BLOCK, 30,
            "Cria um fardo de feno que acelera as plantações em volta por 20 minutos.", TERRA, VIDA, AGUA),
    TEMPESTADE("Chamado da Tempestade", Forma.TOQUE, Material.LIGHTNING_ROD, 25,
            "Invoca um raio onde você olha (até 25 blocos). Na chuva, dano em dobro.", ENERGIA, VENTO, AGUA),
    FANTASMA("Fantasma", Forma.CORPO, Material.PHANTOM_MEMBRANE, 60,
            "Invisível e veloz por 10 segundos; os monstros perdem você de vista.", SOMBRA, VENTO, VAZIO),
    METEORO("Meteoro", Forma.PROJETIL, Material.MAGMA_BLOCK, 30,
            "Um projétil em chamas que explode ao acertar (sem destruir blocos).", FOGO, ENERGIA, VAZIO),
    MATILHA("Matilha", Forma.CRIACAO, Material.BONE, 90,
            "Invoca 3 lobos que lutam por você durante 1 minuto.", NATUREZA, VIDA, VENTO),
    PRISAO_DE_GELO("Prisão de Gelo", Forma.TOQUE, Material.PACKED_ICE, 20,
            "Prende a criatura que você olha (até 12 blocos) num bloco de gelo por 4 segundos.", GELO, TERRA, VAZIO),

    // ---------- quartetos ----------
    FENIX("Fênix", Forma.CORPO, Material.TOTEM_OF_UNDYING, 300,
            "Por 60 segundos, se você morrer, renasce em chamas com metade da vida.", FOGO, VIDA, VENTO, ENERGIA),
    BURACO_NEGRO("Buraco Negro", Forma.PROJETIL, Material.ECHO_SHARD, 60,
            "Abre um vórtice que suga os inimigos e implode.", VAZIO, SOMBRA, ENERGIA, TERRA),
    GENESE("Gênese", Forma.AURA, Material.FLOWERING_AZALEA, 120,
            "Primavera em volta de você: plantas crescem, flores brotam, animais procriam e aliados se curam.",
            TERRA, AGUA, VIDA, NATUREZA),
    AVATAR("Avatar Elemental", Forma.CORPO, Material.NETHER_STAR, 120,
            "Por 20 segundos: Força II, Resistência e Velocidade; seus golpes queimam, congelam e empurram.",
            FOGO, GELO, TERRA, VENTO),
    SANTUARIO("Santuário", Forma.CRIACAO, Material.BEACON, 120,
            "Cria uma área sagrada por 30 segundos: aliados regeneram rápido e mortos-vivos queimam.",
            VIDA, TERRA, AGUA, ENERGIA);

    private final String nome;
    private final Forma forma;
    private final Material icone;
    private final int recarga;
    private final String descricao;
    private final Set<Essencia> essencias;

    Receita(String nome, Forma forma, Material icone, int recarga, String descricao, Essencia... essencias) {
        this.nome = nome;
        this.forma = forma;
        this.icone = icone;
        this.recarga = recarga;
        this.descricao = descricao;
        this.essencias = EnumSet.copyOf(Arrays.asList(essencias));
    }

    public String nome() { return nome; }
    public Forma forma() { return forma; }
    public Material icone() { return icone; }
    /** Recarga em segundos. */
    public int recarga() { return recarga; }
    public String descricao() { return descricao; }
    public Set<Essencia> essencias() { return essencias; }

    public static Receita de(Set<Essencia> essencias, Forma forma) {
        for (Receita r : values()) {
            if (r.forma == forma && r.essencias.equals(essencias)) return r;
        }
        return null;
    }

    public static Receita porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
