package br.rpgatributos.vida;

import java.util.EnumMap;
import java.util.Map;

/**
 * Os números de cada carta no Duelo de Cartas: custo (energia), força, vida e um dom (habilidade).
 * A família da carta dá o estilo: mortos-vivos voltam uma vez, aranhas envenenam, o Nether se
 * enfurece, os peixes curam e protegem, os chefes e as lendas são caros e fortes.
 */
public final class CartasDuelo {

    private CartasDuelo() { }

    /** A habilidade da carta no duelo. */
    public enum Dom {
        NENHUM("", ""),
        VOO("Voo", "Só é bloqueada por quem também voa: senão ataca direto o adversário."),
        VENENO("Veneno", "Quem leva dano dela perde 1 de vida no fim de cada turno."),
        ESCUDO("Escudo", "Ignora o primeiro dano que levar."),
        ROUBO("Roubo de vida", "Cura o dono no tanto de dano que causar."),
        EXPLOSAO("Explosão", "Ao cair, causa 2 de dano na carta da frente."),
        RAPIDO("Rápida", "Ataca já no turno em que entra."),
        INVOCAR("Invocar", "Ao entrar, cria um Lacaio 1/1 numa trilha livre."),
        TEIMOSO("Teimosa", "Na primeira vez que cai, volta com 1 de vida."),
        CURA("Cura", "No fim do seu turno, cura 1 de vida de cada carta sua."),
        FURIA("Fúria", "Ganha +1 de força cada vez que leva dano."),
        MURALHA("Muralha", "Não ataca, mas segura a trilha.");

        public final String nome, descricao;

        Dom(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }
    }

    /** Custo em energia, força, vida e o dom. */
    public record Status(int custo, int forca, int vida, Dom dom) { }

    private static final Map<Carta, Status> TABELA = new EnumMap<>(Carta.class);

    private static void s(Carta c, int custo, int forca, int vida, Dom d) {
        TABELA.put(c, new Status(custo, forca, vida, d));
    }

    static {
        // Mortos-vivos
        s(Carta.ZUMBI, 2, 2, 3, Dom.TEIMOSO);
        s(Carta.ESQUELETO, 2, 3, 2, Dom.NENHUM);
        s(Carta.HUSK, 3, 2, 4, Dom.TEIMOSO);
        s(Carta.STRAY, 3, 3, 2, Dom.VENENO);
        s(Carta.AFOGADO, 3, 3, 3, Dom.NENHUM);
        s(Carta.ZUMBI_ALDEAO, 2, 1, 4, Dom.TEIMOSO);
        s(Carta.PHANTOM, 3, 3, 2, Dom.VOO);
        s(Carta.BOGGED, 3, 2, 3, Dom.VENENO);
        // Artrópodes
        s(Carta.ARANHA, 2, 2, 2, Dom.VENENO);
        s(Carta.ARANHA_CAVERNA, 1, 1, 1, Dom.VENENO);
        s(Carta.TRACA, 1, 1, 1, Dom.INVOCAR);
        s(Carta.ENDERMITE, 1, 2, 1, Dom.RAPIDO);
        // Nether
        s(Carta.BLAZE, 3, 3, 2, Dom.VOO);
        s(Carta.GHAST, 4, 4, 3, Dom.VOO);
        s(Carta.MAGMA, 3, 2, 4, Dom.INVOCAR);
        s(Carta.PIGLIN_BRUTO, 4, 5, 4, Dom.FURIA);
        s(Carta.HOGLIN, 3, 4, 3, Dom.NENHUM);
        s(Carta.ZOGLIN, 3, 4, 2, Dom.FURIA);
        s(Carta.ESQUELETO_WITHER, 4, 4, 3, Dom.VENENO);
        s(Carta.PIGLIN_ZUMBI, 2, 2, 3, Dom.FURIA);
        // Illagers
        s(Carta.SAQUEADOR, 2, 3, 2, Dom.NENHUM);
        s(Carta.VINGADOR, 3, 4, 2, Dom.RAPIDO);
        s(Carta.INVOCADOR, 5, 3, 4, Dom.INVOCAR);
        s(Carta.BRUXA, 3, 2, 3, Dom.CURA);
        s(Carta.DEVASTADOR, 5, 5, 6, Dom.NENHUM);
        s(Carta.VEX, 1, 2, 1, Dom.VOO);
        // Estranhos
        s(Carta.CREEPER, 2, 1, 2, Dom.EXPLOSAO);
        s(Carta.SLIME, 2, 1, 3, Dom.INVOCAR);
        s(Carta.BREEZE, 3, 3, 3, Dom.VOO);
        s(Carta.GUARDIAO, 3, 3, 3, Dom.ESCUDO);
        s(Carta.GUARDIAO_ANCIAO, 5, 4, 7, Dom.ESCUDO);
        s(Carta.WARDEN, 6, 7, 8, Dom.FURIA);
        s(Carta.RANGEDOR, 4, 3, 5, Dom.ESCUDO);
        // Fim
        s(Carta.ENDERMAN, 3, 4, 3, Dom.RAPIDO);
        s(Carta.SHULKER, 3, 2, 5, Dom.ESCUDO);
        s(Carta.DRAGAO, 6, 6, 8, Dom.VOO);
        // Chefes do Altar
        s(Carta.CHEFE_GOLEM, 5, 5, 7, Dom.ESCUDO);
        s(Carta.CHEFE_RAINHA, 5, 4, 5, Dom.VENENO);
        s(Carta.CHEFE_CHAMAS, 5, 5, 5, Dom.EXPLOSAO);
        s(Carta.CHEFE_LICH, 6, 4, 6, Dom.INVOCAR);
        s(Carta.CHEFE_TEMPESTADE, 5, 4, 5, Dom.VOO);
        s(Carta.CHEFE_ARAUTO, 6, 6, 6, Dom.ROUBO);
        // Peixes raros
        s(Carta.CARPA_DOURADA, 1, 1, 2, Dom.CURA);
        s(Carta.TRUTA_ARCO_IRIS, 2, 2, 2, Dom.RAPIDO);
        s(Carta.PEIXE_LUA, 2, 1, 4, Dom.MURALHA);
        s(Carta.PEIXE_PEDRA, 2, 1, 5, Dom.MURALHA);
        s(Carta.ENGUIA_ELETRICA, 2, 3, 1, Dom.RAPIDO);
        s(Carta.PEIXE_GELO, 2, 2, 3, Dom.VENENO);
        s(Carta.KOI_CELESTE, 3, 2, 3, Dom.CURA);
        s(Carta.BAIACU_REI, 3, 2, 3, Dom.VENENO);
        s(Carta.SALMAO_REI, 3, 3, 3, Dom.FURIA);
        s(Carta.PEIXE_FANTASMA, 3, 3, 2, Dom.VOO);
        s(Carta.PEIXE_ABISSAL, 4, 4, 4, Dom.ROUBO);
        s(Carta.PEIXE_DRAGAO, 5, 5, 5, Dom.VOO);
        // Lendas
        s(Carta.QILIN, 6, 4, 8, Dom.CURA);
        s(Carta.FENGHUANG, 6, 5, 5, Dom.TEIMOSO);
        s(Carta.LONGMA, 5, 4, 6, Dom.ESCUDO);
        s(Carta.BAIHU, 5, 6, 4, Dom.FURIA);
        s(Carta.XUANWU, 6, 2, 10, Dom.MURALHA);
        s(Carta.PIXIU, 4, 3, 5, Dom.ROUBO);
        s(Carta.BAIZE, 4, 2, 5, Dom.CURA);
        s(Carta.KUNPENG, 6, 5, 6, Dom.VOO);
        s(Carta.NIAN, 6, 7, 6, Dom.EXPLOSAO);
        // Mundo
        s(Carta.CORACAO_PENUMBRA, 6, 5, 7, Dom.ROUBO);
        s(Carta.GUARDIAO_VENTOS, 5, 4, 5, Dom.VOO);
        s(Carta.SUMO_SACERDOTE, 6, 5, 6, Dom.INVOCAR);
        s(Carta.CAPITAO_ESQUECIDO, 4, 4, 4, Dom.TEIMOSO);
        s(Carta.CHEFE_BANDIDOS, 3, 4, 3, Dom.RAPIDO);
        s(Carta.APRENDIZ_CORROMPIDO, 3, 3, 2, Dom.VENENO);
        s(Carta.GUARDIAO_ANCESTRAL, 5, 3, 8, Dom.ESCUDO);
        // Moradores
        s(Carta.VELHO_PESCADOR, 2, 2, 2, Dom.INVOCAR);
        s(Carta.PEIXEIRA, 2, 1, 3, Dom.CURA);
        s(Carta.CARPINTEIRO_NAVAL, 3, 1, 5, Dom.MURALHA);
        s(Carta.MENINA_DO_CAIS, 1, 1, 1, Dom.CURA);
        s(Carta.MESTRE, 4, 4, 4, Dom.RAPIDO);
        s(Carta.HERBALISTA, 2, 1, 3, Dom.CURA);
        s(Carta.GUARDIAO_DO_SINO, 3, 1, 4, Dom.ESCUDO);
        s(Carta.PEREGRINO, 2, 2, 2, Dom.RAPIDO);
        s(Carta.MESTRE_MINEIRO, 3, 3, 3, Dom.FURIA);
        s(Carta.JOALHEIRO, 2, 1, 3, Dom.ESCUDO);
        s(Carta.FERREIRO_ANAO, 4, 3, 5, Dom.ESCUDO);
        s(Carta.TAVERNEIRO, 3, 2, 4, Dom.CURA);
        s(Carta.DRUIDA, 3, 2, 4, Dom.CURA);
        s(Carta.ARQUEIRA, 3, 3, 2, Dom.RAPIDO);
        s(Carta.CURANDEIRA, 3, 1, 4, Dom.CURA);
        s(Carta.TECELA, 2, 1, 4, Dom.MURALHA);
    }

    /** Os números da carta (uma carta sem tabela vale 2/2/2). */
    public static Status de(Carta c) {
        return TABELA.getOrDefault(c, new Status(2, 2, 2, Dom.NENHUM));
    }

    /** Linhas para a descrição da carta: "Custo 3 · ⚔ 3 · ❤ 2" e o dom. */
    public static String resumo(Carta c, boolean brilhante) {
        Status s = de(c);
        int b = brilhante ? 1 : 0;
        return "⚡" + s.custo() + "  ⚔" + (s.forca() + b) + "  ❤" + (s.vida() + b) + (s.dom() == Dom.NENHUM ? "" : "  · " + s.dom().nome);
    }
}
