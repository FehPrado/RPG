package br.rpgatributos.fazenda;

import br.rpgatributos.mundo.NaturezaViva;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.vida.Artesanato;
import br.rpgatributos.vida.Coleta;
import br.rpgatributos.vida.Criacao;
import br.rpgatributos.vida.Cultivo;
import br.rpgatributos.vida.Erva;
import br.rpgatributos.vida.Fruta;
import br.rpgatributos.vida.Mel;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

/**
 * Receitas da Cozinha. Liberadas pelo nível de Culinária (em fração do nível máximo).
 * As variedades mutantes só entram nas receitas mais fortes.
 */
public enum Prato {
    PAO_CASEIRO("Pão Caseiro", Material.BREAD, 0, 10, 4,
            List.of(m(Material.WHEAT, 3), m(Material.EGG, 1)),
            List.of(e(PotionEffectType.HASTE, 120, 0)), null),
    ENSOPADO_LEGUMES("Ensopado de Legumes", Material.MUSHROOM_STEW, 0, 12, 6,
            List.of(m(Material.CARROT, 2), m(Material.POTATO, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 45, 0)), null),
    TORTA_ABOBORA("Torta de Abóbora da Vovó", Material.PUMPKIN_PIE, 0.1, 15, 4,
            List.of(m(Material.PUMPKIN, 1), m(Material.SUGAR, 1), m(Material.EGG, 1), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.LUCK, 300, 0), e(PotionEffectType.SPEED, 180, 0)), null),
    PEIXE_ERVAS("Salmão com Ervas", Material.COOKED_SALMON, 0.15, 15, 4,
            List.of(m(Material.SALMON, 1), m(Material.KELP, 2), g(GrupoAlimento.ERVA, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 300, 0), e(PotionEffectType.DOLPHINS_GRACE, 60, 0)), null),
    BATATA_RECHEADA("Batata Recheada", Material.BAKED_POTATO, 0.2, 20, 6,
            List.of(m(Material.POTATO, 2), m(Material.COOKED_PORKCHOP, 1), m(Material.BROWN_MUSHROOM, 1)),
            List.of(e(PotionEffectType.ABSORPTION, 120, 0), e(PotionEffectType.RESISTANCE, 60, 0)), null),
    BISCOITO_MEL("Biscoito de Mel", Material.COOKIE, 0.25, 20, 2,
            List.of(m(Material.WHEAT, 2), mel(1), m(Material.COCOA_BEANS, 1)),
            List.of(e(PotionEffectType.HASTE, 180, 1)), null),
    ENSOPADO_CACADOR("Ensopado do Caçador", Material.RABBIT_STEW, 0.3, 25, 8,
            List.of(m(Material.COOKED_BEEF, 1), m(Material.CARROT, 1), m(Material.POTATO, 1),
                    m(Material.RED_MUSHROOM, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0)), null),
    SALADA_ARCANA("Salada Arcana", Material.BEETROOT_SOUP, 0.4, 30, 6,
            List.of(m(Material.BEETROOT, 2), m(Material.GLOW_BERRIES, 1), m(Material.CHORUS_FRUIT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 300, 0)), Especial.MANA),
    BANQUETE_GUERREIRO("Banquete do Guerreiro", Material.COOKED_BEEF, 0.5, 40, 10,
            List.of(m(Material.COOKED_BEEF, 2), m(Material.GOLDEN_CARROT, 1), v(Variedade.TRIGO_DOURADO, 1)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0), e(PotionEffectType.RESISTANCE, 180, 0),
                    e(PotionEffectType.REGENERATION, 180, 0)), null),
    PAO_DOURADO("Pão Dourado", Material.BREAD, 0.6, 45, 12,
            List.of(v(Variedade.TRIGO_DOURADO, 3)),
            List.of(e(PotionEffectType.ABSORPTION, 180, 1), e(PotionEffectType.SATURATION, 1, 4)), null),
    SOPA_CRISTALINA("Sopa Cristalina", Material.MUSHROOM_STEW, 0.7, 50, 8,
            List.of(v(Variedade.CENOURA_CRISTALINA, 2), v(Variedade.BATATA_ANCESTRAL, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 300, 0), e(PotionEffectType.SPEED, 180, 1)), null),
    TORTA_RUBI("Torta Rubi", Material.PUMPKIN_PIE, 0.8, 60, 8,
            List.of(v(Variedade.BETERRABA_RUBI, 2), m(Material.SUGAR, 1), m(Material.EGG, 1)),
            List.of(e(PotionEffectType.HEALTH_BOOST, 300, 1), e(PotionEffectType.REGENERATION, 30, 1)), null),
    SOPA_PESCADOR("Sopa do Pescador", Material.BEETROOT_SOUP, 0.1, 15, 6,
            List.of(m(Material.COD, 2), m(Material.POTATO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 300, 0), e(PotionEffectType.LUCK, 300, 0)), null),
    SUSHI_REAL("Sushi Real", Material.COOKED_SALMON, 0.45, 35, 8,
            List.of(p(PeixeRaro.SALMAO_REI, 1), m(Material.KELP, 2), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.DOLPHINS_GRACE, 120, 0), e(PotionEffectType.CONDUIT_POWER, 300, 0),
                    e(PotionEffectType.HASTE, 300, 0)), null),
    CALDEIRADA_ABISSAL("Caldeirada Abissal", Material.RABBIT_STEW, 0.75, 60, 10,
            List.of(p(PeixeRaro.PEIXE_ABISSAL, 1), m(Material.INK_SAC, 1), v(Variedade.BATATA_ANCESTRAL, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.RESISTANCE, 240, 0), e(PotionEffectType.NIGHT_VISION, 300, 0),
                    e(PotionEffectType.STRENGTH, 180, 0)), null),
    // ---------- 2.29: da pesca no gelo ----------
    CALDO_QUENTE("Caldo Quente do Pescador", Material.MUSHROOM_STEW, 0.15, 18, 7,
            List.of(p(PeixeRaro.TRUTA_DO_GELO, 1), m(Material.POTATO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 480, 0), e(PotionEffectType.REGENERATION, 30, 0)), null),
    SORVETE_CRISTAL("Sorvete de Cristal", Material.BEETROOT_SOUP, 0.5, 40, 4,
            List.of(p(PeixeRaro.ENGUIA_DE_CRISTAL, 1), m(Material.SNOWBALL, 2), m(Material.SUGAR, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 1), e(PotionEffectType.HASTE, 240, 1)), null),
    CAVIAR_ANCESTRAL("Caviar Ancestral", Material.SUSPICIOUS_STEW, 0.8, 90, 10,
            List.of(p(PeixeRaro.ESTURJAO_ANCESTRAL, 1), m(Material.GOLDEN_CARROT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.HEALTH_BOOST, 300, 1), e(PotionEffectType.LUCK, 600, 0)), null),
    BANQUETE_LENDARIO("Banquete Lendário", Material.SUSPICIOUS_STEW, 0.9, 100, 12,
            List.of(v(Variedade.TRIGO_DOURADO, 1), v(Variedade.CENOURA_CRISTALINA, 1), v(Variedade.BATATA_ANCESTRAL, 1),
                    v(Variedade.BETERRABA_RUBI, 1), m(Material.GOLDEN_APPLE, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.REGENERATION, 300, 0), e(PotionEffectType.SPEED, 300, 0)), Especial.BANQUETE),
    // ---------- 2.33: vida no campo (plantações da estação, animais e Tacho do Artesão) ----------
    PAO_COM_GELEIA("Pão com Geleia", Material.BREAD, 0.05, 10, 6,
            List.of(m(Material.BREAD, 1), a(Artesanato.GELEIA, 1)),
            List.of(e(PotionEffectType.SPEED, 180, 0), e(PotionEffectType.HASTE, 120, 0)), null),
    SALADA_PRIMAVERA("Salada da Primavera", Material.BEETROOT_SOUP, 0.1, 15, 6,
            List.of(c(Cultivo.PASTINACA, 1), c(Cultivo.ALHO, 1), c(Cultivo.FEIJAO_VERDE, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 40, 0), e(PotionEffectType.SPEED, 240, 0)), null),
    MILHO_ASSADO("Milho Assado com Mel", Material.BAKED_POTATO, 0.15, 15, 8,
            List.of(c(Cultivo.MILHO, 3), mel(1)),
            List.of(e(PotionEffectType.HASTE, 240, 0), e(PotionEffectType.SATURATION, 1, 2)), null),
    OMELETE_QUEIJO("Omelete de Queijo", Material.BAKED_POTATO, 0.2, 20, 8,
            List.of(m(Material.EGG, 3), a(Artesanato.QUEIJO, 1), c(Cultivo.ALHO_PORO, 1)),
            List.of(e(PotionEffectType.ABSORPTION, 240, 0), e(PotionEffectType.HASTE, 240, 0)), null),
    TORTA_MORANGO("Torta de Morango", Material.PUMPKIN_PIE, 0.25, 25, 6,
            List.of(c(Cultivo.MORANGO, 3), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 1), e(PotionEffectType.LUCK, 300, 0)), null),
    CREME_ABOBORA("Creme de Abóbora-Moranga", Material.MUSHROOM_STEW, 0.3, 25, 8,
            List.of(c(Cultivo.ABOBORA_MORANGA, 2), c(Cultivo.CEBOLA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.ABSORPTION, 180, 1), e(PotionEffectType.RESISTANCE, 180, 0)), null),
    SORVETE_MELAO("Sorvete de Melão", Material.BEETROOT_SOUP, 0.3, 25, 4,
            List.of(c(Cultivo.MELAO, 2), m(Material.SNOWBALL, 2), m(Material.SUGAR, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 0), e(PotionEffectType.FIRE_RESISTANCE, 480, 0)), null),
    PIZZA_DA_ROCA("Pizza da Roça", Material.PUMPKIN_PIE, 0.35, 30, 10,
            List.of(c(Cultivo.TOMATE, 2), a(Artesanato.QUEIJO, 1), m(Material.WHEAT, 2)),
            List.of(e(PotionEffectType.STRENGTH, 240, 0), e(PotionEffectType.ABSORPTION, 240, 0)), null),
    SOPA_DE_INVERNO("Sopa de Inverno", Material.MUSHROOM_STEW, 0.4, 30, 8,
            List.of(c(Cultivo.NABO_DE_NEVE, 1), c(Cultivo.RABANETE_GELADO, 1), c(Cultivo.COUVE_GELADA, 1), c(Cultivo.ALHO_PORO, 1),
                    m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 600, 0), e(PotionEffectType.REGENERATION, 45, 0)), null),
    RATATOUILLE("Ratatouille", Material.RABBIT_STEW, 0.5, 40, 10,
            List.of(c(Cultivo.BERINJELA, 1), c(Cultivo.TOMATE, 1), c(Cultivo.PIMENTA, 1), c(Cultivo.CEBOLA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.REGENERATION, 60, 0), e(PotionEffectType.RESISTANCE, 300, 0)), null),
    BOLO_OXICOCO("Bolo de Oxicoco", Material.PUMPKIN_PIE, 0.55, 45, 8,
            List.of(c(Cultivo.OXICOCO, 3), m(Material.WHEAT, 2), m(Material.EGG, 2), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.HEALTH_BOOST, 300, 0), e(PotionEffectType.REGENERATION, 60, 0)), null),
    RISOTO_TRUFA("Risoto de Trufa", Material.MUSHROOM_STEW, 0.7, 60, 10,
            List.of(t(1), a(Artesanato.QUEIJO, 1), m(Material.WHEAT, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.LUCK, 600, 1), e(PotionEffectType.NIGHT_VISION, 600, 0), e(PotionEffectType.REGENERATION, 60, 1)), null),
    BANQUETE_FAZENDA("Banquete da Fazenda", Material.SUSPICIOUS_STEW, 0.85, 90, 12,
            List.of(a(Artesanato.QUEIJO_DE_CABRA, 1), t(1), c(Cultivo.UVA, 2), c(Cultivo.MILHO, 1), m(Material.EGG, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.REGENERATION, 300, 0), e(PotionEffectType.HASTE, 300, 0)), Especial.BANQUETE),

    // ---------------- 2.44: cozinhar monstros (o que eles deixam vira prato, com efeitos estranhos) ----------------
    ENSOPADO_DO_NECROMANTE("Ensopado do Necromante", Material.RABBIT_STEW, 0.1, 15, 6,
            List.of(m(Material.ROTTEN_FLESH, 4), m(Material.POTATO, 1), m(Material.BROWN_MUSHROOM, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SATURATION, 1, 1)), Especial.ESTOMAGO_DE_FERRO),
    OMELETE_DE_ARANHA("Omelete de Olho de Aranha", Material.BAKED_POTATO, 0.15, 20, 5,
            List.of(m(Material.SPIDER_EYE, 2), m(Material.EGG, 2)),
            List.of(e(PotionEffectType.NIGHT_VISION, 90, 0)), Especial.ESCALAR),
    GELATINA_DE_SLIME("Gelatina de Slime", Material.BEETROOT_SOUP, 0.2, 20, 4,
            List.of(m(Material.SLIME_BALL, 3), m(Material.SUGAR, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.JUMP_BOOST, 120, 1)), Especial.AMORTECER),
    CALDO_DE_OSSOS("Caldo de Ossos", Material.MUSHROOM_STEW, 0.25, 25, 6,
            List.of(m(Material.BONE, 4), m(Material.CARROT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.RESISTANCE, 120, 0)), Especial.FIRME),
    CROCANTE_DE_PHANTOM("Crocante de Phantom", Material.COOKIE, 0.3, 30, 3,
            List.of(m(Material.PHANTOM_MEMBRANE, 2), mel(1), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.SLOW_FALLING, 180, 0)), Especial.SEM_SONO),
    CALDO_DO_GUARDIAO("Caldo do Guardião", Material.MUSHROOM_STEW, 0.4, 35, 6,
            List.of(m(Material.PRISMARINE_CRYSTALS, 2), m(Material.PRISMARINE_SHARD, 1), m(Material.COD, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.CONDUIT_POWER, 240, 0)), null),
    ESPETINHO_DE_BLAZE("Espetinho de Blaze", Material.COOKED_BEEF, 0.45, 40, 8,
            List.of(m(Material.BLAZE_POWDER, 1), m(Material.MAGMA_CREAM, 1), m(Material.COOKED_BEEF, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 300, 0)), Especial.PUNHOS_DE_FOGO),
    SASHIMI_DE_BRISA("Sashimi de Brisa", Material.COOKED_SALMON, 0.5, 45, 6,
            List.of(m(Material.BREEZE_ROD, 1), m(Material.SALMON, 1), m(Material.KELP, 1)),
            List.of(e(PotionEffectType.JUMP_BOOST, 120, 2), e(PotionEffectType.SLOW_FALLING, 120, 0)), Especial.AMORTECER),
    PUDIM_DO_FIM("Pudim do Fim", Material.PUMPKIN_PIE, 0.55, 50, 6,
            List.of(m(Material.ENDER_PEARL, 1), m(Material.CHORUS_FRUIT, 1), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.SPEED, 90, 0)), Especial.PASSO_DO_VAZIO),
    LAGRIMA_CARAMELIZADA("Lágrima de Ghast Caramelizada", Material.COOKIE, 0.6, 55, 4,
            List.of(m(Material.GHAST_TEAR, 1), m(Material.SUGAR, 2), m(Material.GLOW_BERRIES, 1)),
            List.of(e(PotionEffectType.REGENERATION, 30, 1), e(PotionEffectType.ABSORPTION, 120, 0)), null),

    // ---------------- 2.56: a cozinha do Nether (fungos, hoglin e os peixes da lava) ----------------
    ENSOPADO_CARMESIM("Ensopado Carmesim", Material.MUSHROOM_STEW, 0.2, 20, 6,
            List.of(m(Material.CRIMSON_FUNGUS, 2), m(Material.CRIMSON_ROOTS, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0), e(PotionEffectType.FIRE_RESISTANCE, 180, 0)), null),
    CALDO_DISTORCIDO("Caldo Distorcido", Material.BEETROOT_SOUP, 0.25, 25, 5,
            List.of(m(Material.WARPED_FUNGUS, 2), m(Material.WARPED_ROOTS, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 300, 0)), Especial.CHEIRO_DISTORCIDO),
    ESPETO_DE_HOGLIN("Espeto de Hoglin", Material.COOKED_BEEF, 0.3, 30, 9,
            List.of(m(Material.COOKED_PORKCHOP, 2), m(Material.CRIMSON_FUNGUS, 1), m(Material.GOLD_NUGGET, 4)),
            List.of(e(PotionEffectType.STRENGTH, 240, 0), e(PotionEffectType.ABSORPTION, 120, 0)), Especial.AMIGO_DOS_PIGLINS),
    PEIXE_BRASA_NA_PEDRA("Peixe-Brasa na Pedra", Material.COOKED_COD, 0.3, 30, 8,
            List.of(p(PeixeRaro.PEIXE_BRASA, 1), m(Material.MAGMA_CREAM, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 480, 0), e(PotionEffectType.HASTE, 180, 0)), Especial.PUNHOS_DE_FOGO),
    CALDO_VULCANICO("Caldo Vulcânico", Material.RABBIT_STEW, 0.5, 45, 8,
            List.of(p(PeixeRaro.ENGUIA_DE_MAGMA, 1), m(Material.NETHER_WART, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0), e(PotionEffectType.FIRE_RESISTANCE, 300, 0)), null),
    ENSOPADO_DAS_ALMAS("Ensopado das Almas", Material.MUSHROOM_STEW, 0.55, 50, 8,
            List.of(p(PeixeRaro.CARPA_DE_ALMAS, 1), m(Material.WARPED_FUNGUS, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 1), e(PotionEffectType.NIGHT_VISION, 300, 0)), Especial.CHEIRO_DISTORCIDO),
    BANQUETE_INFERNAL("Banquete Infernal", Material.SUSPICIOUS_STEW, 0.85, 90, 12,
            List.of(p(PeixeRaro.SALAMANDRA_DE_LAVA, 1), m(Material.COOKED_PORKCHOP, 2), m(Material.GOLDEN_CARROT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.REGENERATION, 300, 0), e(PotionEffectType.FIRE_RESISTANCE, 600, 0)), Especial.BANQUETE),

    // ---------------- 3.15: o que se colhia e não tinha panela (pomar, coleta, ervas, tacho, peixes, natureza) ----------------
    // Pomar
    TORTA_PESSEGO("Torta de Pêssego", Material.PUMPKIN_PIE, 0.15, 20, 6,
            List.of(f(Fruta.PESSEGO, 2), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.SPEED, 300, 0), e(PotionEffectType.JUMP_BOOST, 180, 0)), null),
    LIMONADA("Limonada", Material.HONEY_BOTTLE, 0.05, 10, 2,
            List.of(f(Fruta.LIMAO, 2), m(Material.SUGAR, 1), m(Material.GLASS_BOTTLE, 1)),
            List.of(e(PotionEffectType.HASTE, 300, 0), e(PotionEffectType.FIRE_RESISTANCE, 180, 0)), null),
    BOLO_LARANJA("Bolo de Laranja", Material.PUMPKIN_PIE, 0.2, 25, 8,
            List.of(f(Fruta.LARANJA, 2), m(Material.WHEAT, 2), m(Material.EGG, 2), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.REGENERATION, 60, 0), e(PotionEffectType.ABSORPTION, 240, 0)), null),
    TORTA_CEREJA("Torta de Cereja", Material.PUMPKIN_PIE, 0.25, 25, 6,
            List.of(f(Fruta.CEREJA, 3), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.LUCK, 600, 0), e(PotionEffectType.SPEED, 180, 0)), null),
    MACA_CARAMELADA("Maçã Dourada Caramelada", Material.APPLE, 0.4, 35, 6,
            List.of(f(Fruta.MACA_DOURADA, 1), m(Material.SUGAR, 2), mel(1)),
            List.of(e(PotionEffectType.ABSORPTION, 600, 1), e(PotionEffectType.REGENERATION, 20, 1), e(PotionEffectType.LUCK, 300, 0)), null),
    // Coleta da estação
    RISOTO_MOREL("Risoto de Morel", Material.MUSHROOM_STEW, 0.3, 30, 8,
            List.of(co(Coleta.MOREL, 2), a(Artesanato.QUEIJO, 1), m(Material.WHEAT, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 60, 0), e(PotionEffectType.HASTE, 300, 0)), null),
    SALADA_SAMAMBAIA("Salada de Samambaia", Material.BEETROOT_SOUP, 0.1, 15, 5,
            List.of(co(Coleta.BROTO_DE_SAMAMBAIA, 2), g(GrupoAlimento.ERVA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SPEED, 300, 0), e(PotionEffectType.REGENERATION, 20, 0)), null),
    ASSADO_ALHO_SELVAGEM("Assado ao Alho-Selvagem", Material.COOKED_BEEF, 0.2, 25, 9,
            List.of(co(Coleta.ALHO_SELVAGEM, 2), g(GrupoAlimento.CARNE, 2)),
            List.of(e(PotionEffectType.RESISTANCE, 300, 0), e(PotionEffectType.STRENGTH, 120, 0)), null),
    TORTA_AMORA("Torta de Amora", Material.PUMPKIN_PIE, 0.15, 20, 6,
            List.of(co(Coleta.AMORA_SILVESTRE, 3), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.SPEED, 300, 0), e(PotionEffectType.REGENERATION, 30, 0)), null),
    SOPA_ALGA_DOCE("Sopa de Alga-Doce", Material.BEETROOT_SOUP, 0.15, 20, 6,
            List.of(co(Coleta.ALGA_DOCE, 2), g(GrupoAlimento.PEIXE, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 600, 0), e(PotionEffectType.DOLPHINS_GRACE, 60, 0)), null),
    BOLO_GROSELHA("Bolo de Groselha", Material.PUMPKIN_PIE, 0.2, 25, 8,
            List.of(co(Coleta.GROSELHA, 3), m(Material.WHEAT, 2), m(Material.EGG, 2), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.LUCK, 600, 0), e(PotionEffectType.HEALTH_BOOST, 180, 0)), null),
    SOPA_CASTANHA("Sopa de Castanha", Material.MUSHROOM_STEW, 0.15, 20, 9,
            List.of(co(Coleta.CASTANHA, 3), g(GrupoAlimento.LEGUME, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.SATURATION, 1, 3), e(PotionEffectType.RESISTANCE, 180, 0)), null),
    COGUMELOS_SALTEADOS("Cogumelos do Bosque Salteados", Material.MUSHROOM_STEW, 0.2, 25, 7,
            List.of(co(Coleta.COGUMELO_DO_BOSQUE, 3), g(GrupoAlimento.ERVA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 600, 0), e(PotionEffectType.RESISTANCE, 120, 0)), null),
    PAO_AVELA("Pão de Avelã", Material.BREAD, 0.1, 15, 7,
            List.of(co(Coleta.AVELA, 2), m(Material.WHEAT, 3), mel(1)),
            List.of(e(PotionEffectType.HASTE, 300, 0), e(PotionEffectType.SATURATION, 1, 1)), null),
    ENSOPADO_RAIZ_INVERNO("Ensopado de Raiz de Inverno", Material.RABBIT_STEW, 0.25, 30, 9,
            List.of(co(Coleta.RAIZ_DE_INVERNO, 2), g(GrupoAlimento.CARNE, 1), m(Material.POTATO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 600, 0), e(PotionEffectType.REGENERATION, 45, 0)), null),
    CALDO_COGUMELO_NEVE("Caldo de Cogumelo-de-Neve", Material.MUSHROOM_STEW, 0.2, 25, 7,
            List.of(co(Coleta.COGUMELO_DE_NEVE, 2), g(GrupoAlimento.LEGUME, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 60, 0), e(PotionEffectType.FIRE_RESISTANCE, 300, 0)), null),
    ASSADO_ZIMBRO("Assado com Zimbro", Material.COOKED_BEEF, 0.35, 35, 10,
            List.of(co(Coleta.FRUTO_DE_ZIMBRO, 2), g(GrupoAlimento.CARNE, 2)),
            List.of(e(PotionEffectType.STRENGTH, 300, 0), e(PotionEffectType.RESISTANCE, 180, 0)), null),
    // Plantações sem prato
    COUVE_FLOR_GRATINADA("Couve-Flor Gratinada", Material.BAKED_POTATO, 0.2, 25, 8,
            List.of(c(Cultivo.COUVE_FLOR, 1), a(Artesanato.QUEIJO, 1), m(Material.WHEAT, 1)),
            List.of(e(PotionEffectType.ABSORPTION, 240, 0), e(PotionEffectType.REGENERATION, 30, 0)), null),
    MUFFIN_MIRTILO("Muffin de Mirtilo", Material.COOKIE, 0.15, 20, 4,
            List.of(c(Cultivo.MIRTILO, 3), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 600, 0), e(PotionEffectType.HASTE, 180, 0)), null),
    INHAME_COM_MEL("Inhame Assado com Mel", Material.BAKED_POTATO, 0.15, 20, 8,
            List.of(c(Cultivo.INHAME, 2), mel(1)),
            List.of(e(PotionEffectType.ABSORPTION, 240, 0), e(PotionEffectType.SATURATION, 1, 2)), null),
    // Ervas do canteiro (e qualquer erva tempera um prato: agachado + clique na Cozinha)
    CHA_SALVIA("Chá de Sálvia", Material.HONEY_BOTTLE, 0.1, 15, 2,
            List.of(er(Erva.SALVIA, 2), mel(1)),
            List.of(e(PotionEffectType.REGENERATION, 90, 0)), null),
    PAO_ERVA_DE_SOL("Pão de Erva-de-Sol", Material.BREAD, 0.2, 20, 7,
            List.of(er(Erva.ERVA_DE_SOL, 1), m(Material.WHEAT, 3)),
            List.of(e(PotionEffectType.HASTE, 360, 0), e(PotionEffectType.FIRE_RESISTANCE, 120, 0)), null),
    PEIXE_FOLHA_GELIDA("Peixe na Folha Gélida", Material.COOKED_COD, 0.3, 30, 7,
            List.of(er(Erva.FOLHA_GELIDA, 2), g(GrupoAlimento.PEIXE, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 600, 0), e(PotionEffectType.SPEED, 240, 0)), null),
    SOPA_MUSGO_LUNAR("Sopa de Musgo Lunar", Material.BEETROOT_SOUP, 0.35, 35, 6,
            List.of(er(Erva.MUSGO_LUNAR, 2), g(GrupoAlimento.COGUMELO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 900, 0), e(PotionEffectType.SPEED, 180, 0)), Especial.MANA),
    GUISADO_RAIZ_ABISSAL("Guisado da Raiz Abissal", Material.RABBIT_STEW, 0.4, 40, 9,
            List.of(er(Erva.RAIZ_ABISSAL, 2), g(GrupoAlimento.CARNE, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.HASTE, 300, 1), e(PotionEffectType.NIGHT_VISION, 300, 0)), null),
    DOCE_FLOR_DE_CRISTAL("Doce de Flor-de-Cristal", Material.COOKIE, 0.45, 40, 3,
            List.of(er(Erva.FLOR_DE_CRISTAL, 2), m(Material.SUGAR, 2), mel(1)),
            List.of(e(PotionEffectType.LUCK, 300, 0)), Especial.MANA),
    // Peixes raros que só eram de coleção
    CARPA_DOURADA_ASSADA("Carpa Dourada Assada", Material.COOKED_COD, 0.1, 20, 7,
            List.of(p(PeixeRaro.CARPA_DOURADA, 1), g(GrupoAlimento.ERVA, 1)),
            List.of(e(PotionEffectType.LUCK, 600, 0), e(PotionEffectType.HASTE, 180, 0)), null),
    TRUTA_GRELHADA("Truta Arco-Íris Grelhada", Material.COOKED_SALMON, 0.1, 20, 7,
            List.of(p(PeixeRaro.TRUTA_ARCO_IRIS, 1), g(GrupoAlimento.LEGUME, 1)),
            List.of(e(PotionEffectType.SPEED, 300, 0), e(PotionEffectType.REGENERATION, 30, 0)), null),
    PEIXE_LUA_DEFUMADO("Peixe-Lua Defumado", Material.COOKED_COD, 0.15, 25, 7,
            List.of(p(PeixeRaro.PEIXE_LUA, 1), m(Material.DRIED_KELP, 2)),
            List.of(e(PotionEffectType.NIGHT_VISION, 900, 0), e(PotionEffectType.SLOW_FALLING, 120, 0)), null),
    CALDO_PEIXE_PEDRA("Caldo de Peixe-Pedra", Material.MUSHROOM_STEW, 0.2, 25, 7,
            List.of(p(PeixeRaro.PEIXE_PEDRA, 1), m(Material.POTATO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.RESISTANCE, 300, 0)), Especial.FIRME),
    ESPETO_ENGUIA_ELETRICA("Espeto de Enguia Elétrica", Material.COOKED_COD, 0.2, 30, 7,
            List.of(p(PeixeRaro.ENGUIA_ELETRICA, 1), g(GrupoAlimento.LEGUME, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 1), e(PotionEffectType.HASTE, 240, 1)), null),
    CARPACCIO_PEIXE_GELO("Carpaccio de Peixe-Gelo", Material.COOKED_SALMON, 0.25, 30, 6,
            List.of(p(PeixeRaro.PEIXE_GELO, 1), f(Fruta.LIMAO, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 900, 0), e(PotionEffectType.RESISTANCE, 120, 0)), null),
    KOI_GLACEADO("Koi Glaceado", Material.COOKED_SALMON, 0.3, 35, 7,
            List.of(p(PeixeRaro.KOI_CELESTE, 1), mel(1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.LUCK, 900, 1), e(PotionEffectType.REGENERATION, 30, 0)), null),
    MOQUECA_BAIACU("Moqueca de Baiacu-Rei", Material.RABBIT_STEW, 0.35, 40, 10,
            List.of(p(PeixeRaro.BAIACU_REI, 1), c(Cultivo.TOMATE, 1), c(Cultivo.PIMENTA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 600, 0), e(PotionEffectType.STRENGTH, 240, 0)), Especial.ESTOMAGO_DE_FERRO),
    LUCIO_AO_FORNO("Lúcio Polar ao Forno", Material.COOKED_COD, 0.3, 35, 9,
            List.of(p(PeixeRaro.LUCIO_POLAR, 1), m(Material.POTATO, 2), g(GrupoAlimento.ERVA, 1)),
            List.of(e(PotionEffectType.FIRE_RESISTANCE, 600, 0), e(PotionEffectType.STRENGTH, 180, 0)), null),
    CALDO_LUMINOSO("Caldo Luminoso", Material.MUSHROOM_STEW, 0.35, 40, 6,
            List.of(p(PeixeRaro.PEIXE_LANTERNA, 1), m(Material.GLOW_BERRIES, 2), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 1200, 0), e(PotionEffectType.WATER_BREATHING, 300, 0)), null),
    SOPA_FANTASMA("Sopa do Peixe-Fantasma", Material.BEETROOT_SOUP, 0.45, 45, 6,
            List.of(p(PeixeRaro.PEIXE_FANTASMA, 1), g(GrupoAlimento.COGUMELO, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.INVISIBILITY, 180, 0), e(PotionEffectType.SPEED, 180, 0)), null),
    BANQUETE_DRAGAO("Banquete do Dragão", Material.SUSPICIOUS_STEW, 0.9, 100, 12,
            List.of(p(PeixeRaro.PEIXE_DRAGAO, 1), g(GrupoAlimento.FRUTA, 2), g(GrupoAlimento.ERVA, 1), m(Material.GOLDEN_CARROT, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.STRENGTH, 300, 1), e(PotionEffectType.RESISTANCE, 300, 0),
                    e(PotionEffectType.REGENERATION, 300, 0), e(PotionEffectType.FIRE_RESISTANCE, 600, 0)), Especial.BANQUETE),
    // Do Tacho do Artesão
    SANDUICHE("Sanduíche Caprichado", Material.BREAD, 0.1, 15, 8,
            List.of(m(Material.BREAD, 1), a(Artesanato.MAIONESE, 1), g(GrupoAlimento.CARNE, 1)),
            List.of(e(PotionEffectType.SATURATION, 1, 4), e(PotionEffectType.ABSORPTION, 180, 0)), null),
    MARMITA_VIAJANTE("Marmita do Viajante", Material.BAKED_POTATO, 0.2, 20, 9,
            List.of(a(Artesanato.CONSERVA, 1), m(Material.BREAD, 1), g(GrupoAlimento.CARNE, 1)),
            List.of(e(PotionEffectType.SPEED, 600, 0), e(PotionEffectType.SATURATION, 1, 3)), null),
    MASSA_OLEO_TRUFA("Massa ao Óleo de Trufa", Material.MUSHROOM_STEW, 0.65, 60, 10,
            List.of(a(Artesanato.OLEO_DE_TRUFA, 1), a(Artesanato.QUEIJO, 1), m(Material.WHEAT, 3), m(Material.EGG, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.LUCK, 900, 1), e(PotionEffectType.HASTE, 600, 0), e(PotionEffectType.REGENERATION, 60, 0)), null),
    // Da natureza nova (árvores anciãs e ninhos)
    CHA_SEIVA_ANCIA("Chá de Seiva Anciã", Material.HONEY_BOTTLE, 0.5, 45, 4,
            List.of(seiva(1), g(GrupoAlimento.ERVA, 1)),
            List.of(e(PotionEffectType.REGENERATION, 120, 1), e(PotionEffectType.HEALTH_BOOST, 600, 0)), null),
    HIDROMEL_GELEIA_REAL("Hidromel de Geleia Real", Material.HONEY_BOTTLE, 0.7, 70, 4,
            List.of(mel(Mel.GELEIA_REAL, 1), mel(2)),
            List.of(e(PotionEffectType.ABSORPTION, 600, 2), e(PotionEffectType.REGENERATION, 60, 1), e(PotionEffectType.LUCK, 600, 0)), null),
    // Receitas abertas: qualquer coisa do grupo serve
    SALADA_ESTACAO("Salada da Estação", Material.BEETROOT_SOUP, 0.05, 12, 6,
            List.of(g(GrupoAlimento.LEGUME, 3), g(GrupoAlimento.ERVA, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.REGENERATION, 30, 0), e(PotionEffectType.HASTE, 180, 0)), null),
    SOPA_DA_MATA("Sopa da Mata", Material.MUSHROOM_STEW, 0.1, 12, 7,
            List.of(g(GrupoAlimento.COGUMELO, 3), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.NIGHT_VISION, 300, 0), e(PotionEffectType.SATURATION, 1, 2)), null),
    TORTA_FRUTAS("Torta de Frutas Sortidas", Material.PUMPKIN_PIE, 0.2, 20, 7,
            List.of(g(GrupoAlimento.FRUTA, 3), m(Material.WHEAT, 2), m(Material.EGG, 1), m(Material.SUGAR, 1)),
            List.of(e(PotionEffectType.SPEED, 240, 0), e(PotionEffectType.LUCK, 240, 0)), null),
    GRELHADO_MISTO("Grelhado Misto", Material.COOKED_BEEF, 0.15, 15, 10,
            List.of(g(GrupoAlimento.CARNE, 2), g(GrupoAlimento.LEGUME, 2)),
            List.of(e(PotionEffectType.STRENGTH, 180, 0), e(PotionEffectType.SATURATION, 1, 2)), null),
    PEIXADA("Peixada da Casa", Material.RABBIT_STEW, 0.15, 15, 9,
            List.of(g(GrupoAlimento.PEIXE, 2), g(GrupoAlimento.LEGUME, 1), m(Material.BOWL, 1)),
            List.of(e(PotionEffectType.WATER_BREATHING, 300, 0), e(PotionEffectType.REGENERATION, 30, 0)), null);

    /** Ingrediente: um material comum, uma variedade mutante, um peixe raro OU outro item do plugin. */
    public record Ingrediente(Material material, Variedade variedade, PeixeRaro peixe, Extra extra, int qtd) {}

    /**
     * Um item do plugin como ingrediente (colheita da estação, produto do Tacho, trufa...). {@code aberto}: aceita vários
     * itens ("qualquer fruta", "qualquer mel"); estes saem do inventário depois dos ingredientes exatos.
     */
    public record Extra(String nome, net.kyori.adventure.text.format.TextColor cor, java.util.function.Predicate<org.bukkit.inventory.ItemStack> teste,
                        boolean aberto) {
        public Extra(String nome, net.kyori.adventure.text.format.TextColor cor, java.util.function.Predicate<org.bukkit.inventory.ItemStack> teste) {
            this(nome, cor, teste, false);
        }
    }

    /** Efeito do prato: tipo, duração base em segundos e nível (0 = I). */
    public record Efeito(PotionEffectType tipo, int segundos, int nivel) {}

    /** Algo além dos efeitos de poção. */
    public enum Especial {
        /** Recupera 60 de mana. */
        MANA("Recupera 60 de mana", 0),
        /** Os efeitos valem para todos os jogadores a até 10 blocos. */
        BANQUETE("Vale para todos a até 10 blocos!", 0),
        // Pratos de monstro (veja IguariasMonstro): duração base em segundos.
        ESTOMAGO_DE_FERRO("Estômago de ferro: fome e veneno não pegam em você", 300),
        ESCALAR("Patas de aranha: andando contra uma parede, você sobe", 45),
        AMORTECER("Corpo de gelatina: sem dano de queda", 120),
        FIRME("Ossos de pedra: quase não é empurrado", 180),
        SEM_SONO("Os phantoms esquecem você (como se tivesse dormido)", 0),
        PUNHOS_DE_FOGO("Seus golpes põem fogo no alvo", 120),
        PASSO_DO_VAZIO("Ao levar um golpe, às vezes some e reaparece atrás de quem bateu", 90),
        // 2.56: da cozinha do Nether
        AMIGO_DOS_PIGLINS("Cheiro de ouro: os piglins te deixam em paz, mesmo sem armadura de ouro", 300),
        CHEIRO_DISTORCIDO("Cheiro de fungo distorcido: hoglins e piglins brutos não te atacam", 240);

        private final String descricao;
        private final int segundos;

        Especial(String descricao, int segundos) {
            this.descricao = descricao;
            this.segundos = segundos;
        }

        public String descricao() { return descricao; }
        /** Quanto dura (0 = acontece na hora). */
        public int segundos() { return segundos; }
        /** Efeito de prato de monstro (os dois primeiros são da Cozinha comum). */
        public boolean deMonstro() { return ordinal() >= ESTOMAGO_DE_FERRO.ordinal(); }
    }

    private final String nome;
    private final Material base;
    private final double nivel;
    private final double xp;
    private final float saturacao;
    private final List<Ingrediente> ingredientes;
    private final List<Efeito> efeitos;
    private final Especial especial;

    Prato(String nome, Material base, double nivel, double xp, float saturacao,
          List<Ingrediente> ingredientes, List<Efeito> efeitos, Especial especial) {
        this.nome = nome;
        this.base = base;
        this.nivel = nivel;
        this.xp = xp;
        this.saturacao = saturacao;
        this.ingredientes = ingredientes;
        this.efeitos = efeitos;
        this.especial = especial;
    }

    private static Ingrediente m(Material mat, int qtd) { return new Ingrediente(mat, null, null, null, qtd); }
    private static Ingrediente v(Variedade var, int qtd) { return new Ingrediente(null, var, null, null, qtd); }
    private static Ingrediente p(PeixeRaro peixe, int qtd) { return new Ingrediente(null, null, peixe, null, qtd); }
    /** Colheita da estação (não a semente). */
    private static Ingrediente c(Cultivo cv, int qtd) {
        return new Ingrediente(null, null, null, new Extra(cv.nome(), cv.cor(), s -> Cultivo.daColheita(s) == cv), qtd);
    }
    /** Produto do Tacho do Artesão (qualquer origem: geleia de qualquer fruta serve). */
    private static Ingrediente a(Artesanato art, int qtd) {
        return new Ingrediente(null, null, null, new Extra(art.nome(), art.cor(), s -> Artesanato.de(s) == art), qtd);
    }
    /** Trufa do porco. */
    private static Ingrediente t(int qtd) {
        return new Ingrediente(null, null, null, new Extra("Trufa", net.kyori.adventure.text.format.TextColor.color(0x8D6E63),
                s -> "trufa".equals(Criacao.produto(s))), qtd);
    }
    // 3.15: pomar, coleta, ervas, grupos, mel e seiva.
    private static Ingrediente f(Fruta fr, int qtd) {
        return new Ingrediente(null, null, null, new Extra(fr.nome(), fr.cor(), s -> Fruta.de(s) == fr), qtd);
    }
    private static Ingrediente co(Coleta col, int qtd) {
        return new Ingrediente(null, null, null, new Extra(col.nome(), col.cor(), s -> Coleta.de(s) == col), qtd);
    }
    private static Ingrediente er(Erva ev, int qtd) {
        return new Ingrediente(null, null, null, new Extra(ev.nome(), ev.cor(), s -> Erva.de(s) == ev), qtd);
    }
    private static Ingrediente g(GrupoAlimento gr, int qtd) { return grupo(gr, qtd); }
    /** "Qualquer fruta", "qualquer erva"... (também o tempero da Cozinha). */
    public static Ingrediente grupo(GrupoAlimento gr, int qtd) {
        return new Ingrediente(null, null, null, new Extra("Qualquer " + gr.nome(), gr.cor(), s -> GrupoAlimento.de(s) == gr, true), qtd);
    }
    /** Qualquer mel (o do jogo ou do catálogo), menos a Geleia Real, que é rara demais para um biscoito. */
    private static Ingrediente mel(int qtd) {
        return new Ingrediente(null, null, null, new Extra("Mel (qualquer)", net.kyori.adventure.text.format.TextColor.color(0xE8A33D), s -> {
            Mel m = Mel.de(s);
            return m != null ? m != Mel.GELEIA_REAL : s.getType() == Material.HONEY_BOTTLE && br.rpgatributos.alquimia.Ingrediente.simples(s);
        }, true), qtd);
    }
    private static Ingrediente mel(Mel tipo, int qtd) {
        return new Ingrediente(null, null, null, new Extra(tipo.nome(), tipo.cor(), s -> Mel.de(s) == tipo), qtd);
    }
    private static Ingrediente seiva(int qtd) {
        return new Ingrediente(null, null, null, new Extra("Seiva Anciã", net.kyori.adventure.text.format.TextColor.color(0xE08A2E),
                NaturezaViva::ehSeiva), qtd);
    }
    private static Efeito e(PotionEffectType t, int seg, int nivel) { return new Efeito(t, seg, nivel); }

    public String nome() { return nome; }
    public Material base() { return base; }
    /** Nível de Culinária para liberar (ex.: 0,3 = 30% do nível máximo). */
    public int nivelNecessario(int nivelMaximo) { return (int) Math.ceil(nivel * nivelMaximo); }
    public double xp() { return xp; }
    public float saturacao() { return saturacao; }
    public List<Ingrediente> ingredientes() { return ingredientes; }
    public List<Efeito> efeitos() { return efeitos; }
    public Especial especial() { return especial; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Prato porId(String id) {
        for (Prato p : values()) if (p.id().equalsIgnoreCase(id) || p.name().equalsIgnoreCase(id)) return p;
        return null;
    }
}
