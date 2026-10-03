package br.rpgatributos.domador;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Criaturas que o Altar do Domador invoca a partir de "componentes" delas mesmas
 * (couro e carne para a vaca, ossos para o lobo...). Já nascem como companheiros.
 */
public enum Invocavel {
    VACA("Vaca", EntityType.COW, 0, Material.LEATHER, 4, Material.BEEF, 2),
    PORCO("Porco", EntityType.PIG, 0, Material.PORKCHOP, 4, Material.CARROT, 2),
    OVELHA("Ovelha", EntityType.SHEEP, 0, Material.WHITE_WOOL, 4, Material.MUTTON, 2),
    GALINHA("Galinha", EntityType.CHICKEN, 0, Material.FEATHER, 4, Material.CHICKEN, 2, Material.EGG, 1),
    GATO("Gato", EntityType.CAT, 0.10, Material.COD, 4, Material.STRING, 4),
    COELHO("Coelho", EntityType.RABBIT, 0.10, Material.RABBIT_HIDE, 3, Material.RABBIT_FOOT, 1),
    GOLEM_DE_NEVE("Golem de Neve", EntityType.SNOW_GOLEM, 0.10, Material.SNOW_BLOCK, 4, Material.CARVED_PUMPKIN, 1),
    VACA_DE_COGUMELO("Vaca de Cogumelo", EntityType.MOOSHROOM, 0.15, Material.LEATHER, 4, Material.RED_MUSHROOM, 4, Material.BEEF, 2),
    ABELHA("Abelha", EntityType.BEE, 0.15, Material.HONEYCOMB, 4, Material.HONEY_BOTTLE, 1),
    PAPAGAIO("Papagaio", EntityType.PARROT, 0.15, Material.FEATHER, 8, Material.WHEAT_SEEDS, 8),
    LOBO("Lobo", EntityType.WOLF, 0.20, Material.BONE, 8, Material.BEEF, 4),
    RAPOSA("Raposa", EntityType.FOX, 0.20, Material.SWEET_BERRIES, 8, Material.RABBIT_HIDE, 2),
    TATU("Tatu", EntityType.ARMADILLO, 0.25, Material.ARMADILLO_SCUTE, 4, Material.SPIDER_EYE, 2),
    CAVALO("Cavalo", EntityType.HORSE, 0.25, Material.LEATHER, 8, Material.HAY_BLOCK, 4, Material.GOLDEN_CARROT, 2),
    CAMELO("Camelo", EntityType.CAMEL, 0.30, Material.CACTUS, 8, Material.LEATHER, 4, Material.HAY_BLOCK, 2),
    URSO_POLAR("Urso Polar", EntityType.POLAR_BEAR, 0.35, Material.SALMON, 8, Material.SNOW_BLOCK, 8),
    PANDA("Panda", EntityType.PANDA, 0.35, Material.BAMBOO, 16, Material.LEATHER, 4),
    GOLEM_DE_FERRO("Golem de Ferro", EntityType.IRON_GOLEM, 0.40, Material.IRON_BLOCK, 4, Material.CARVED_PUMPKIN, 1, Material.POPPY, 1),
    STRIDER("Strider", EntityType.STRIDER, 0.45, Material.STRING, 8, Material.WARPED_FUNGUS, 4),
    FAREJADOR("Farejador", EntityType.SNIFFER, 0.60, Material.SNIFFER_EGG, 1, Material.MOSS_BLOCK, 16);

    private final String nome;
    private final EntityType tipo;
    private final double nivel;
    private final Map<Material, Integer> receita = new LinkedHashMap<>();

    Invocavel(String nome, EntityType tipo, double nivel, Object... receita) {
        this.nome = nome;
        this.tipo = tipo;
        this.nivel = nivel;
        for (int i = 0; i + 1 < receita.length; i += 2) this.receita.put((Material) receita[i], (Integer) receita[i + 1]);
    }

    public String nome() { return nome; }
    public EntityType tipo() { return tipo; }
    public Map<Material, Integer> receita() { return receita; }

    public int nivelNecessario(int nivelMaximo) {
        return (int) Math.ceil(nivel * nivelMaximo);
    }

    /** Ovo de spawn da criatura (para os menus), ou um laço se não houver. */
    public static Material icone(EntityType tipo) {
        Material ovo = Material.matchMaterial(tipo.name() + "_SPAWN_EGG");
        return ovo == null ? Material.LEAD : ovo;
    }
}
