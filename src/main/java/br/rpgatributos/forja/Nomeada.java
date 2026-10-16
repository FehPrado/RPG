package br.rpgatributos.forja;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Armas e ferramentas com nome: já nascem assim quando são forjadas (com sorte) ou encontradas
 * em baús e chefes. O nome, a textura e uma linha de história são só aparência (não dão bônus).
 * O desenho de cada uma fica no {@code ArteNomeadas} (molde + paleta).
 */
public enum Nomeada {
    // ---------- espadas ----------
    PRESA_DO_INVERNO("Presa do Inverno", Categoria.ESPADA, "espada", "Forjada no gelo da Expedição Perdida. Ainda está fria ao toque.",
            'o', 0xFF0D2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFB3E5FC, 'd', 0xFF4FC3F7, 'g', 0xFF90A4AE, 'w', 0xFF37474F, 'p', 0xFF80DEEA, 'a', 0xFFE1F5FE),
    LAMINA_DE_THALASSA("Lâmina de Thalassa", Categoria.ESPADA, "larga", "Dos guardas da cidade submersa. O sal nunca a enferrujou.",
            'o', 0xFF003A3A, 'h', 0xFFB2FFF0, 'm', 0xFF26C6DA, 'd', 0xFF00838F, 'g', 0xFFFF8A65, 'w', 0xFF00695C, 'p', 0xFFFF7043, 'a', 0xFFFFAB91),
    AURORA_DE_AERILIA("Aurora de Aerília", Categoria.ESPADA, "sabre", "Caiu de uma ilha do céu numa noite de tempestade.",
            'o', 0xFF3A2E0A, 'h', 0xFFFFFFFF, 'm', 0xFFFFF3C4, 'd', 0xFFE2B53C, 'g', 0xFFFFD54F, 'w', 0xFF8D6E63, 'p', 0xFF90CAF9, 'a', 0xFFFFF59D),
    JURAMENTO_DO_FORTIM("Juramento do Fortim", Categoria.ESPADA, "larga", "Os soldados do fortim juravam por ela antes da última batalha.",
            'o', 0xFF1A1A1A, 'h', 0xFFB0BEC5, 'm', 0xFF607D8B, 'd', 0xFF37474F, 'g', 0xFF8B0000, 'w', 0xFF3E2723, 'p', 0xFFB71C1C, 'a', 0xFFE53935),
    BRASA_DOS_ANOES("Brasa dos Anões", Categoria.ESPADA, "espada", "Saiu da forja que nunca apagou, debaixo da montanha.",
            'o', 0xFF2A1000, 'h', 0xFFFFE0B2, 'm', 0xFFFF8F00, 'd', 0xFFBF360C, 'g', 0xFF795548, 'w', 0xFF4E342E, 'p', 0xFFFFC107, 'a', 0xFFFF6F00),
    SUSSURRO_DA_PENUMBRA("Sussurro da Penumbra", Categoria.ESPADA, "sabre", "Achada no escuro, abaixo das raízes. Ninguém lembra de forjá-la.",
            'o', 0xFF0A0614, 'h', 0xFFB39DDB, 'm', 0xFF5E35B1, 'd', 0xFF311B92, 'g', 0xFF212121, 'w', 0xFF1A1226, 'p', 0xFF7C4DFF, 'a', 0xFF7C4DFF),
    LAMINA_DA_RAIZ_DE_LUZ("Lâmina da Raiz de Luz", Categoria.ESPADA, "sabre", "Talhada da primeira raiz acesa nas profundezas. Ainda brilha um pouco.",
            'o', 0xFF2A1A05, 'h', 0xFFFFF8E1, 'm', 0xFFFFD54F, 'd', 0xFFFFA000, 'g', 0xFF5D4037, 'w', 0xFF3E2723, 'p', 0xFFFFE082, 'a', 0xFFFFB300),
    FIO_DO_GUARDIAO("Fio do Guardião dos Ventos", Categoria.ESPADA, "espada", "O Guardião dos Ventos a deixou cair quando caiu da ilha.",
            'o', 0xFF1A2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFE3F2FD, 'd', 0xFF90CAF9, 'g', 0xFFE2B53C, 'w', 0xFF546E7A, 'p', 0xFFB3E5FC, 'a', 0xFFFFF59D),
    PROMESSA_DO_PRIMEIRO_COLONO("Promessa do Primeiro Colono", Categoria.ESPADA, "larga", "O fundador da primeira colônia a fincou no chão e disse: aqui.",
            'o', 0xFF1A1A10, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF616161, 'g', 0xFF2E7D32, 'w', 0xFF6D4C41, 'p', 0xFFC9A227, 'a', 0xFF66BB6A),
    LAMINA_DO_ECLIPSE("Lâmina do Eclipse", Categoria.ESPADA, "sabre", "Forjada no dia em que a lua cobriu o sol. Fica mais escura ao meio-dia.",
            'o', 0xFF0A0A0A, 'h', 0xFFFFCC80, 'm', 0xFF424242, 'd', 0xFF1A1A1A, 'g', 0xFFFF6F00, 'w', 0xFF212121, 'p', 0xFFFFB74D, 'a', 0xFFFF9800),
    // ---------- machados e maças ----------
    TALHA_RAIZES("Talha-Raízes", Categoria.MACHADO, "machado", "Do lenhador que abriu a primeira estrada no meio da mata.",
            'o', 0xFF1B2A10, 'h', 0xFFDCE775, 'm', 0xFF9E9D24, 'd', 0xFF5D5A0E, 'w', 0xFF6D4C41, 'a', 0xFF43A047),
    FURIA_DO_SAQUEADOR("Fúria do Saqueador", Categoria.MACHADO, "machado2", "Dos bandidos que queimaram a vila. Ainda cheira a fumaça.",
            'o', 0xFF1A0000, 'h', 0xFFBDBDBD, 'm', 0xFF757575, 'd', 0xFF424242, 'w', 0xFF3E2723, 'a', 0xFFC62828),
    MACHADO_DO_FAROLEIRO("Machado do Faroleiro", Categoria.MACHADO, "machado2", "O faroleiro cortava lenha para a lanterna com ele.",
            'o', 0xFF2A1A0A, 'h', 0xFFFFFFFF, 'm', 0xFFE53935, 'd', 0xFF8E1B1B, 'w', 0xFF5D4037, 'a', 0xFFFFD54F),
    MACHADO_DO_VELHO_LENHADOR("Machado do Velho Lenhador", Categoria.MACHADO, "machado", "O lenhador da colônia cortou mil árvores e plantou duas mil.",
            'o', 0xFF1A1205, 'h', 0xFFCFD8DC, 'm', 0xFF90A4AE, 'd', 0xFF546E7A, 'w', 0xFF8D6E63, 'a', 0xFF7CB342),
    MORDIDA_DA_GEADA("Mordida da Geada", Categoria.MACHADO, "machado2", "Só sai do gelo no inverno. No verão, sua.",
            'o', 0xFF0D2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFB3E5FC, 'd', 0xFF4FC3F7, 'w', 0xFF37474F, 'a', 0xFFE1F5FE),
    SINO_DE_MARIS("Sino de Maris", Categoria.MACA, "maca", "Tocava no templo de Maris para chamar a maré.",
            'o', 0xFF0A1A3A, 'h', 0xFFE3F2FD, 'm', 0xFF42A5F5, 'd', 0xFF1565C0, 'w', 0xFF5D4037, 'p', 0xFFFFD54F, 'a', 0xFFFFE082),
    MARTELO_DO_EREMITA("Martelo do Eremita", Categoria.MACA, "martelo", "O eremita dizia que ele só bate em quem merece.",
            'o', 0xFF1A2A10, 'h', 0xFFB0BEC5, 'm', 0xFF78909C, 'd', 0xFF455A64, 'w', 0xFF5D4037, 'p', 0xFF7CB342, 'a', 0xFF9CCC65),
    MARTELO_DO_MESTRE_DE_OBRAS("Martelo do Mestre de Obras", Categoria.MACA, "martelo", "Ergueu a prefeitura, a torre de vigia e metade das casas.",
            'o', 0xFF2A1A0A, 'h', 0xFFFFE0B2, 'm', 0xFFBCAAA4, 'd', 0xFF795548, 'w', 0xFF5D4037, 'p', 0xFFFFC107, 'a', 0xFFFFD54F),
    CORACAO_ROUBADO("Coração Roubado", Categoria.MACA, "maca", "Arrancada do Coração da Penumbra. Bate devagar, como se estivesse viva.",
            'o', 0xFF0A0614, 'h', 0xFFB39DDB, 'm', 0xFF5E35B1, 'd', 0xFF311B92, 'w', 0xFF1A1226, 'p', 0xFFE040FB, 'a', 0xFF7C4DFF),
    // ---------- lanças e tridentes ----------
    LANCA_DA_SENTINELA("Lança da Sentinela", Categoria.LANCA, "lanca", "A sentinela da muralha a apoiava no ombro a noite inteira. Nunca dormiu em serviço.",
            'o', 0xFF1A1A1A, 'h', 0xFFECEFF1, 'm', 0xFF90A4AE, 'd', 0xFF546E7A, 'w', 0xFF6D4C41, 'g', 0xFF37474F, 'p', 0xFF455A64, 'a', 0xFF1565C0),
    CHIFRE_DO_QILIN("Chifre do Qilin", Categoria.LANCA, "lanca", "Dizem que é o chifre de um Qilin que ninguém viu. Esquenta na mão de quem mente.",
            'o', 0xFF2A1A00, 'h', 0xFFFFF8E1, 'm', 0xFFFFD54F, 'd', 0xFFC9A227, 'w', 0xFF2E7D32, 'g', 0xFFC9A227, 'p', 0xFF1B5E20, 'a', 0xFFD32F2F),
    AGULHA_DOS_VENTOS("Agulha dos Ventos", Categoria.LANCA, "lanca", "Os guardas das ilhas do céu a usavam para afastar as nuvens. Pelo menos é o que contam.",
            'o', 0xFF1A2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFE3F2FD, 'd', 0xFF90CAF9, 'w', 0xFFD9DEE8, 'g', 0xFFE2B53C, 'p', 0xFF90A4AE, 'a', 0xFF81D4FA),
    PIQUE_DO_CAVALEIRO_ERRANTE("Pique do Cavaleiro Errante", Categoria.LANCA, "lanca", "Ele cavalgou de vila em vila e nunca baixou a lança, nem para comer.",
            'o', 0xFF1A0A0A, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF616161, 'w', 0xFF8D2A2A, 'g', 0xFFC9A227, 'p', 0xFF5D1A1A, 'a', 0xFFFFFFFF),
    CRESCENTE_VERDE("Lâmina do Crescente Verde", Categoria.LANCA, "crescente", "Uma meia-lua num cabo comprido. Chegou do leste numa caravana e ninguém quis vendê-la de volta.",
            'o', 0xFF0A1A0A, 'h', 0xFFE8F5E9, 'm', 0xFFA5D6A7, 'd', 0xFF388E3C, 'w', 0xFF4E342E, 'g', 0xFFC9A227, 'p', 0xFF3E2723, 'a', 0xFFD32F2F),
    ESPETO_DO_BASTIAO("Espeto do Bastião", Categoria.LANCA, "crescente", "Os piglins o tomaram de um guerreiro que caiu na lava. Ainda está quente.",
            'o', 0xFF1A0A00, 'h', 0xFFFFE082, 'm', 0xFF5D4037, 'd', 0xFF3E2723, 'w', 0xFF212121, 'g', 0xFFFFC107, 'p', 0xFF4E342E, 'a', 0xFFFF6F00),
    TRIDENTE_DE_MARIS("Tridente de Maris", Categoria.LANCA, "tridente", "O sacerdote de Maris o erguia e o mar recuava três passos.",
            'o', 0xFF0A1A3A, 'h', 0xFFE1F5FE, 'm', 0xFF4FC3F7, 'd', 0xFF0277BD, 'w', 0xFF1565C0, 'g', 0xFFFFD54F, 'p', 0xFF0D47A1, 'a', 0xFFFFE082),
    GARFO_DO_AFOGADO_REI("Garfo do Afogado-Rei", Categoria.LANCA, "tridente", "O Afogado-Rei da cidade submersa nunca o soltou. Até agora.",
            'o', 0xFF001A1A, 'h', 0xFFB2DFDB, 'm', 0xFF4DB6AC, 'd', 0xFF00695C, 'w', 0xFF263238, 'g', 0xFF8D6E63, 'p', 0xFF37474F, 'a', 0xFF76FF03),
    TRIDENTE_DO_PESCADOR_DE_PEROLAS("Tridente do Pescador de Pérolas", Categoria.LANCA, "tridente", "Abria ostras no recife e espantava tubarão quando precisava.",
            'o', 0xFF2A1A1A, 'h', 0xFFFFFFFF, 'm', 0xFFF8BBD0, 'd', 0xFFE57373, 'w', 0xFFBCAAA4, 'g', 0xFFFF8A65, 'p', 0xFF8D6E63, 'a', 0xFFFFFDE7),
    TRIDENTE_DA_TEMPESTADE("Tridente da Tempestade", Categoria.LANCA, "tridente", "Chama o raio mesmo sem chuva. Quase sempre.",
            'o', 0xFF0A0A1A, 'h', 0xFFFFFDE7, 'm', 0xFFFFF176, 'd', 0xFF9E9D24, 'w', 0xFF37474F, 'g', 0xFF7E57C2, 'p', 0xFF263238, 'a', 0xFF7C4DFF),
    // ---------- arcos e bestas ----------
    ARCO_DO_CACADOR_SILENCIOSO("Arco do Caçador Silencioso", Categoria.ARCO, "arco", "Nunca rangeu. Os cervos nem viam de onde vinha.",
            'w', 0xFF2E4A1E, 'h', 0xFF6B8E23, 'a', 0xFF1B2A10),
    ASA_DE_ZEFIRA("Asa de Zéfira", Categoria.ARCO, "arco", "Feito com penas que o vento trouxe de uma ilha do céu.",
            'w', 0xFFD9DEE8, 'h', 0xFFFFFFFF, 'a', 0xFF90CAF9),
    ARCO_DAS_BRASAS("Arco das Brasas", Categoria.ARCO, "arco", "Os anões o temperaram na lava. A corda nunca esfria.",
            'w', 0xFF6D2A0A, 'h', 0xFFFF8F00, 'a', 0xFFFFD54F),
    ARCO_DAS_FOLHAS_CAIDAS("Arco das Folhas Caídas", Categoria.ARCO, "arco", "Feito no outono, com a madeira de uma árvore que caiu sozinha.",
            'w', 0xFF8D4A1E, 'h', 0xFFFFA726, 'a', 0xFFD84315),
    BESTA_DO_CAPITAO("Besta do Capitão", Categoria.BESTA, "besta", "Do capitão de um galeão real que hoje dorme no fundo do mar.",
            'w', 0xFF3E2723, 'h', 0xFF8D6E63, 'a', 0xFFC9A227),
    BESTA_GLACIAL("Besta Glacial", Categoria.BESTA, "besta", "Feita na expedição do norte. O gelo segura a corda.",
            'w', 0xFF455A64, 'h', 0xFFB3E5FC, 'a', 0xFFE1F5FE),
    BESTA_DA_TORRE_DE_VIGIA("Besta da Torre de Vigia", Categoria.BESTA, "besta", "A sentinela da colônia nunca errou um tiro do alto da torre.",
            'w', 0xFF5D4037, 'h', 0xFFA1887F, 'a', 0xFF90A4AE),
    // ---------- arsenal (2.51): adaga, faca, machadinha, foice, bastão e chicote ----------
    PRESAS_GEMEAS("Presas Gêmeas", Categoria.ESPADA, "adaga", "Eram duas, forjadas juntas. Uma sempre sabe onde a outra está.",
            'o', 0xFF101418, 'h', 0xFFFFFFFF, 'm', 0xFFB0BEC5, 'd', 0xFF607D8B, 'g', 0xFFC9A227, 'w', 0xFF3E2723, 'p', 0xFFB71C1C, 'a', 0xFFE53935),
    BEIJO_DA_VIBORA("Beijo da Víbora", Categoria.ESPADA, "adaga", "O veneno secou há cem anos. O medo, não.",
            'o', 0xFF0A1A0A, 'h', 0xFFDCEDC8, 'm', 0xFF7CB342, 'd', 0xFF33691E, 'g', 0xFF212121, 'w', 0xFF1B2A10, 'p', 0xFF558B2F, 'a', 0xFFAEEA00),
    PENA_DO_CORVO("Pena do Corvo", Categoria.ESPADA, "faca", "Voa reto como o corvo e volta como ele: quando quer.",
            'o', 0xFF0A0A10, 'h', 0xFFCFD8DC, 'm', 0xFF546E7A, 'd', 0xFF263238, 'g', 0xFF37474F, 'w', 0xFF1A1A1A, 'p', 0xFF311B92, 'a', 0xFF7E57C2),
    MACHADINHA_DO_VELHO_ANAO("Machadinha do Velho Anão", Categoria.ESPADA, "machadinha", "Os anões a jogavam por cima da mesa para pedir o sal.",
            'o', 0xFF2A1000, 'h', 0xFFFFE0B2, 'm', 0xFFFF8F00, 'd', 0xFFBF360C, 'g', 0xFF795548, 'w', 0xFF4E342E, 'p', 0xFFFFC107, 'a', 0xFFFF6F00),
    FOICE_DO_CEIFADOR_DE_TRIGO("Foice do Ceifador de Trigo", Categoria.ESPADA, "foice", "Colheu o trigo de três vilas numa tarde. Depois, outras coisas.",
            'o', 0xFF1A1205, 'h', 0xFFFFF8E1, 'm', 0xFFBDBDBD, 'd', 0xFF616161, 'g', 0xFFC9A227, 'w', 0xFF8D6E63, 'p', 0xFFFFD54F, 'a', 0xFFFFEB3B),
    LUA_MINGUANTE("Lua Minguante", Categoria.ESPADA, "foice", "Brilha mais quando a lua some do céu.",
            'o', 0xFF0A0A1A, 'h', 0xFFF3E5F5, 'm', 0xFF9FA8DA, 'd', 0xFF3949AB, 'g', 0xFF1A237E, 'w', 0xFF263238, 'p', 0xFF5C6BC0, 'a', 0xFFE1F5FE),
    BASTAO_DO_MONGE_DA_MONTANHA("Bastão do Monge da Montanha", Categoria.ESPADA, "bastao", "Os monges do mosteiro treinam mil golpes com ele antes do café.",
            'o', 0xFF2A1A0A, 'h', 0xFFD7A86E, 'm', 0xFF8D6E63, 'd', 0xFF5D4037, 'g', 0xFFC62828, 'w', 0xFF8D5A2B, 'p', 0xFFFFD54F, 'a', 0xFFFFC107),
    BORDAO_DO_PEREGRINO("Bordão do Peregrino", Categoria.ESPADA, "bastao", "Andou todas as estradas do mundo e ainda está inteiro.",
            'o', 0xFF1A1A10, 'h', 0xFFC5E1A5, 'm', 0xFF8D6E63, 'd', 0xFF4E342E, 'g', 0xFF33691E, 'w', 0xFF6D4C41, 'p', 0xFF7CB342, 'a', 0xFF90A4AE),
    CHICOTE_DO_DOMADOR_DE_FERAS("Chicote do Domador de Feras", Categoria.ESPADA, "chicote", "Nunca bateu num bicho. Só estala, e eles obedecem.",
            'o', 0xFF1A0E05, 'h', 0xFFD7A86E, 'm', 0xFF8D5A2B, 'd', 0xFF4E342E, 'g', 0xFFC9A227, 'w', 0xFF5D4037, 'p', 0xFFB71C1C, 'a', 0xFFFFD54F),
    LINGUA_DE_BRASA("Língua de Brasa", Categoria.ESPADA, "chicote", "Trançado com couro de strider. Deixa um rastro de faísca.",
            'o', 0xFF1A0500, 'h', 0xFFFFCC80, 'm', 0xFFE64A19, 'd', 0xFF8B1A1A, 'g', 0xFF212121, 'w', 0xFF3E2723, 'p', 0xFFFF6F00, 'a', 0xFFFFEB3B),
    // ---------- armas dos minérios ----------
    CORACAO_DA_MONTANHA("Coração da Montanha", Categoria.ESPADA, "montante", "Os anões o forjaram com o mitrilo mais fundo que acharam. Pesa como uma promessa.",
            'o', 0xFF0A1A2A, 'h', 0xFFE0F7FA, 'm', 0xFF7FCFE0, 'd', 0xFF37718A, 'g', 0xFFC9A227, 'w', 0xFF4E342E, 'p', 0xFFFFD54F, 'a', 0xFF4DD0E1),
    LAMINA_DO_REI_GIGANTE("Lâmina do Rei Gigante", Categoria.ESPADA, "montante", "Só o Rei Gigante a erguia com uma mão. Você vai precisar das duas.",
            'o', 0xFF1A1A1A, 'h', 0xFFFFFFFF, 'm', 0xFFB0BEC5, 'd', 0xFF607D8B, 'g', 0xFF8B1A1A, 'w', 0xFF3E2723, 'p', 0xFFC9A227, 'a', 0xFFE53935),
    RETORNO_DO_ESCORPIAO("Retorno do Escorpião", Categoria.ESPADA, "bumerangue", "Do caçador do deserto que nunca errou. O âmbar ainda guarda um escorpião dentro.",
            'o', 0xFF2A1A05, 'h', 0xFFFFE0B2, 'm', 0xFFD7A86E, 'd', 0xFF8D6E3F, 'g', 0xFF5D4037, 'w', 0xFF8D5A2B, 'p', 0xFF3E2723, 'a', 0xFFF2A33A),
    ASAS_DE_JADE("Asas de Jade", Categoria.ESPADA, "leques", "Uma dançarina da selva lutava com eles sem nunca tocar o chão.",
            'o', 0xFF0A2A1A, 'h', 0xFFE8F5E9, 'm', 0xFF66BB6A, 'd', 0xFF1B5E20, 'g', 0xFFC9A227, 'w', 0xFF3E2723, 'p', 0xFFFFD54F, 'a', 0xFFA5D6A7),
    PRESA_DO_KRAKEN("Presa do Kraken", Categoria.ESPADA, "arpao", "Arrancada do kraken que afundou três galeões. A pérola no colar era o olho dele.",
            'o', 0xFF0A1A2A, 'h', 0xFFE0F2F1, 'm', 0xFF4DB6AC, 'd', 0xFF00695C, 'g', 0xFF4A148C, 'w', 0xFF263238, 'p', 0xFF7E57C2, 'a', 0xFFF3E9F0),
    ARPAO_DO_VELHO_BALEEIRO("Arpão do Velho Baleeiro", Categoria.ESPADA, "arpao", "Ele jurava que pegou uma baleia do tamanho de uma ilha. Ninguém acreditava, até ver o arpão.",
            'o', 0xFF1A1A1A, 'h', 0xFFECEFF1, 'm', 0xFF90A4AE, 'd', 0xFF455A64, 'g', 0xFF8D6E63, 'w', 0xFF6D4C41, 'p', 0xFFC9A26B, 'a', 0xFFFFFFFF),
    ESTRELA_DA_GEADA("Estrela da Geada", Categoria.ESPADA, "mangual", "Uma bola de gelo que nunca derrete, presa a uma corrente que nunca enferruja.",
            'o', 0xFF0D2A3A, 'h', 0xFFFFFFFF, 'm', 0xFFB3E5FC, 'd', 0xFF4FC3F7, 'g', 0xFF37474F, 'w', 0xFF546E7A, 'p', 0xFFB0BEC5, 'a', 0xFFE1F5FE),
    PRESAS_DO_LOBISOMEM("Presas do Lobisomem", Categoria.ESPADA, "garras", "Feitas das garras de um lobisomem que caiu numa lua cheia. Às vezes ainda uivam.",
            'o', 0xFF1A0A0A, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF424242, 'g', 0xFF5D4037, 'w', 0xFF3E2723, 'p', 0xFF8D6E63, 'a', 0xFFB0102A),
    TOCHA_DO_PANTANO("Tocha do Pântano", Categoria.ESPADA, "clava", "O eremita do pântano espantava as feras com ela acesa. A turfa nunca apagou.",
            'o', 0xFF1A1205, 'h', 0xFFFFCC80, 'm', 0xFF6D4C2F, 'd', 0xFF3E2A18, 'g', 0xFF33691E, 'w', 0xFF4E342E, 'p', 0xFF3C2819, 'a', 0xFFFF6F00),
    // ---------- ferramentas ----------
    PICARETA_DO_MINEIRO_PERDIDO("Picareta do Mineiro Perdido", Categoria.PICARETA, "picareta", "Achada no fundo da mina abandonada, ao lado de um capacete.",
            'o', 0xFF2A1A0A, 'h', 0xFFD7A86E, 'm', 0xFFA0522D, 'd', 0xFF6D3A1A, 'w', 0xFF5D4037, 'a', 0xFFFFB74D),
    QUEBRA_CRISTAIS("Quebra-Cristais", Categoria.PICARETA, "picareta", "Ela canta quando acerta um geodo.",
            'o', 0xFF1A0A2A, 'h', 0xFFE1BEE7, 'm', 0xFFAB47BC, 'd', 0xFF6A1B9A, 'w', 0xFF4E342E, 'a', 0xFFCE93D8),
    PICARETA_DAS_PROFUNDEZAS("Picareta das Profundezas", Categoria.PICARETA, "picareta", "Bate melhor no escuro. Dizem que veio de baixo das raízes.",
            'o', 0xFF0A0A0A, 'h', 0xFF9E9E9E, 'm', 0xFF4E4E52, 'd', 0xFF2A2A2E, 'w', 0xFF3E2723, 'a', 0xFFFFC107),
    PICARETA_DO_POCO_FUNDO("Picareta do Poço Fundo", Categoria.PICARETA, "picareta", "O Minerador da colônia desceu com ela pelo poço e voltou com mitrilo.",
            'o', 0xFF0A1A2A, 'h', 0xFFE1F5FE, 'm', 0xFF7FCFE0, 'd', 0xFF37718A, 'w', 0xFF4E342E, 'a', 0xFFFFD54F),
    PA_DO_COVEIRO("Pá do Coveiro", Categoria.PA, "pa", "Cavou todas as covas do cemitério antigo.",
            'o', 0xFF1A1A1A, 'h', 0xFFE0E0E0, 'm', 0xFF9E9E9E, 'd', 0xFF616161, 'w', 0xFFD7CCC8, 'p', 0xFFBDBDBD),
    PA_DO_ARQUEOLOGO("Pá do Arqueólogo", Categoria.PA, "pa", "Desenterrou metade das relíquias do mundo.",
            'o', 0xFF3A2A0A, 'h', 0xFFFFE0B2, 'm', 0xFFC9A227, 'd', 0xFF8D6E1A, 'w', 0xFF795548, 'p', 0xFFFFD54F),
    ENXADA_DA_COLHEITA_DOURADA("Enxada da Colheita Dourada", Categoria.ENXADA, "enxada", "Onde ela passa, o trigo cresce mais alto.",
            'o', 0xFF3A2A00, 'h', 0xFFFFF59D, 'm', 0xFFFFD54F, 'd', 0xFFC9A227, 'w', 0xFF8D6E63, 'p', 0xFF7CB342),
    ENXADA_DAS_QUATRO_ESTACOES("Enxada das Quatro Estações", Categoria.ENXADA, "enxada", "Planta morango na primavera, abóbora no outono e nunca erra a época.",
            'o', 0xFF1A2A10, 'h', 0xFFF8BBD0, 'm', 0xFF81C784, 'd', 0xFFFF8F00, 'w', 0xFF8D6E63, 'p', 0xFF90CAF9),
    ENXADA_DO_JARDIM_SUSPENSO("Enxada do Jardim Suspenso", Categoria.ENXADA, "enxada", "Cuidava da Árvore Celeste, lá no alto.",
            'o', 0xFF3A1A2A, 'h', 0xFFFCE4EC, 'm', 0xFFF48FB1, 'd', 0xFFAD1457, 'w', 0xFF5D2E3A, 'p', 0xFFB39DDB),
    VARA_DO_PESCADOR_DO_GELO("Vara do Pescador do Gelo", Categoria.VARA, "vara", "Pescou o primeiro Esturjão Ancestral.",
            'o', 0xFF263238, 'w', 0xFF90A4AE, 's', 0xFFE0E0E0, 'a', 0xFF4FC3F7, 'g', 0xFF37474F, 'p', 0xFF4FC3F7),
    VARA_DE_CORAL("Vara de Coral", Categoria.VARA, "vara", "Feita com um galho de coral da cidade submersa.",
            'o', 0xFF4A1A0A, 'w', 0xFFFF8A65, 's', 0xFFE0E0E0, 'a', 0xFFFFD54F, 'g', 0xFFBF360C, 'p', 0xFF26C6DA);

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "nomeada");

    private final String nome;
    private final Categoria categoria;
    private final String molde, historia;
    private final Object[] cores;

    Nomeada(String nome, Categoria categoria, String molde, String historia, Object... cores) {
        this.nome = nome;
        this.categoria = categoria;
        this.molde = molde;
        this.historia = historia;
        this.cores = cores;
    }

    public String nome() { return nome; }
    public Categoria categoria() { return categoria; }
    public String molde() { return molde; }
    public String historia() { return historia; }
    /** Paleta (pares letra, cor ARGB) para o desenho. */
    public Object[] cores() { return cores; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    /** O id da textura e da marca no item. */
    public String visual() { return "nomeada_" + id(); }

    /** Tridente (na Forja ele conta como lança, mas tem nomes e desenhos próprios). */
    public boolean tridente() { return molde.equals("tridente"); }

    /** A arma do arsenal que essa nomeada é (adaga, foice...), ou null se é uma arma comum. */
    public br.rpgatributos.combo.Arsenal.Arma arma() {
        return switch (molde) {
            case "adaga" -> br.rpgatributos.combo.Arsenal.Arma.ADAGA;
            case "faca" -> br.rpgatributos.combo.Arsenal.Arma.FACA;
            case "machadinha" -> br.rpgatributos.combo.Arsenal.Arma.MACHADINHA;
            case "foice" -> br.rpgatributos.combo.Arsenal.Arma.FOICE;
            case "bastao" -> br.rpgatributos.combo.Arsenal.Arma.BASTAO;
            case "chicote" -> br.rpgatributos.combo.Arsenal.Arma.CHICOTE;
            case "montante" -> br.rpgatributos.combo.Arsenal.Arma.MONTANTE;
            case "bumerangue" -> br.rpgatributos.combo.Arsenal.Arma.BUMERANGUE;
            case "leques" -> br.rpgatributos.combo.Arsenal.Arma.LEQUES;
            case "arpao" -> br.rpgatributos.combo.Arsenal.Arma.ARPAO;
            case "mangual" -> br.rpgatributos.combo.Arsenal.Arma.MANGUAL;
            case "garras" -> br.rpgatributos.combo.Arsenal.Arma.GARRAS;
            case "clava" -> br.rpgatributos.combo.Arsenal.Arma.CLAVA;
            default -> null;
        };
    }

    /** Uma nomeada ao acaso para esse tipo de item (null se a categoria não tem nenhuma). */
    public static Nomeada sortear(Material tipo) {
        return sortear(tipo, null);
    }

    /** Para um item já pronto: uma arma do arsenal só sorteia nomeadas dela (e uma espada comum, nenhuma do arsenal). */
    public static Nomeada sortear(ItemStack item) {
        return sortear(item.getType(), br.rpgatributos.combo.Arsenal.de(item));
    }

    private static Nomeada sortear(Material tipo, br.rpgatributos.combo.Arsenal.Arma arma) {
        Categoria c = Categoria.de(tipo);
        boolean tridente = tipo == Material.TRIDENT;
        List<Nomeada> l = new ArrayList<>();
        for (Nomeada n : values()) if (n.categoria == c && n.tridente() == tridente && n.arma() == arma) l.add(n);
        return l.isEmpty() ? null : l.get(ThreadLocalRandom.current().nextInt(l.size()));
    }

    private static final NamespacedKey K_LENDA = new NamespacedKey("rpgatributos", "lenda");

    /**
     * Chamado pela Forja depois de montar o item: nome, a história no topo da descrição e o
     * visual. Lendas ficam com o que já têm.
     */
    public static void decorar(org.bukkit.inventory.meta.ItemMeta meta, int refino, net.kyori.adventure.text.format.TextColor cor) {
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(K_LENDA)) return;
        Nomeada n = porId(pdc.getOrDefault(CHAVE, PersistentDataType.STRING, ""));
        if (n == null) return;
        meta.itemName(net.kyori.adventure.text.Component.text(n.nome + (refino > 0 ? " +" + refino : ""), cor));
        List<net.kyori.adventure.text.Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(0, net.kyori.adventure.text.Component.text("“" + n.historia + "”", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true));
        lore.add(1, net.kyori.adventure.text.Component.empty());
        meta.lore(lore);
        br.rpgatributos.exploracao.PacoteRecursos.marcar(meta, n.visual());
    }

    /** Chance de um item forjado nascer com nome, pela raridade. */
    public static double chance(Raridade r) {
        return switch (r) {
            case COMUM -> 0.02;
            case RARO -> 0.05;
            case EPICO -> 0.10;
            case UNICO -> 0.18;
            case LENDARIO -> 0.30;
            case MITICO -> 0.45;
        };
    }

    /** Um item do jogo para essa nomeada (o material da arma é sorteado: ferro e diamante são os mais comuns). */
    public Material sortearMaterial() {
        String sufixo = switch (categoria) {
            case ESPADA -> "_SWORD";
            case MACHADO -> "_AXE";
            case PICARETA -> "_PICKAXE";
            case PA -> "_SHOVEL";
            case ENXADA -> "_HOE";
            case LANCA -> tridente() ? null : "_SPEAR";
            case MACA -> null;
            case ARCO -> null;
            case BESTA -> null;
            default -> null;
        };
        if (sufixo == null) {
            return switch (categoria) {
                case MACA -> Material.MACE;
                case ARCO -> Material.BOW;
                case BESTA -> Material.CROSSBOW;
                case LANCA -> Material.TRIDENT;
                default -> Material.FISHING_ROD;
            };
        }
        double r = ThreadLocalRandom.current().nextDouble();
        String tier = r < 0.40 ? "IRON" : r < 0.70 ? "DIAMOND" : r < 0.82 ? "GOLDEN" : r < 0.95 ? "STONE" : "NETHERITE";
        Material m = Material.matchMaterial(tier + sufixo);
        return m != null ? m : Material.matchMaterial("IRON" + sufixo);
    }

    public static Nomeada de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Nomeada porId(String id) {
        for (Nomeada n : values()) if (n.id().equalsIgnoreCase(id) || n.name().equalsIgnoreCase(id)) return n;
        return null;
    }
}
