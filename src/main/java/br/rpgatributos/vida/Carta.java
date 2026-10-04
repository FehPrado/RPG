package br.rpgatributos.vida;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** As cartas do Álbum. Cada uma pertence a uma página; completar a página dá um bônus. */
public enum Carta {
    // Mortos-vivos
    ZUMBI(Pagina.MORTOS_VIVOS, "Zumbi", EntityType.ZOMBIE, Material.ROTTEN_FLESH),
    ESQUELETO(Pagina.MORTOS_VIVOS, "Esqueleto", EntityType.SKELETON, Material.BONE),
    HUSK(Pagina.MORTOS_VIVOS, "Zumbi do Deserto", EntityType.HUSK, Material.SAND),
    STRAY(Pagina.MORTOS_VIVOS, "Esqueleto Errante", EntityType.STRAY, Material.ICE),
    AFOGADO(Pagina.MORTOS_VIVOS, "Afogado", EntityType.DROWNED, Material.TRIDENT),
    ZUMBI_ALDEAO(Pagina.MORTOS_VIVOS, "Zumbi Aldeão", EntityType.ZOMBIE_VILLAGER, Material.EMERALD),
    PHANTOM(Pagina.MORTOS_VIVOS, "Phantom", EntityType.PHANTOM, Material.PHANTOM_MEMBRANE),
    BOGGED(Pagina.MORTOS_VIVOS, "Esqueleto do Pântano", EntityType.BOGGED, Material.BROWN_MUSHROOM),
    // Artrópodes
    ARANHA(Pagina.ARTROPODES, "Aranha", EntityType.SPIDER, Material.STRING),
    ARANHA_CAVERNA(Pagina.ARTROPODES, "Aranha da Caverna", EntityType.CAVE_SPIDER, Material.COBWEB),
    TRACA(Pagina.ARTROPODES, "Traça", EntityType.SILVERFISH, Material.STONE_BRICKS),
    ENDERMITE(Pagina.ARTROPODES, "Endermite", EntityType.ENDERMITE, Material.ENDER_PEARL),
    // Nether
    BLAZE(Pagina.NETHER, "Blaze", EntityType.BLAZE, Material.BLAZE_ROD),
    GHAST(Pagina.NETHER, "Ghast", EntityType.GHAST, Material.GHAST_TEAR),
    MAGMA(Pagina.NETHER, "Cubo de Magma", EntityType.MAGMA_CUBE, Material.MAGMA_CREAM),
    PIGLIN_BRUTO(Pagina.NETHER, "Piglin Bruto", EntityType.PIGLIN_BRUTE, Material.GOLDEN_AXE),
    HOGLIN(Pagina.NETHER, "Hoglin", EntityType.HOGLIN, Material.PORKCHOP),
    ZOGLIN(Pagina.NETHER, "Zoglin", EntityType.ZOGLIN, Material.ROTTEN_FLESH),
    ESQUELETO_WITHER(Pagina.NETHER, "Esqueleto Wither", EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL),
    PIGLIN_ZUMBI(Pagina.NETHER, "Piglin Zumbi", EntityType.ZOMBIFIED_PIGLIN, Material.GOLD_NUGGET),
    // Illagers
    SAQUEADOR(Pagina.ILLAGERS, "Saqueador", EntityType.PILLAGER, Material.CROSSBOW),
    VINGADOR(Pagina.ILLAGERS, "Vingador", EntityType.VINDICATOR, Material.IRON_AXE),
    INVOCADOR(Pagina.ILLAGERS, "Invocador", EntityType.EVOKER, Material.TOTEM_OF_UNDYING),
    BRUXA(Pagina.ILLAGERS, "Bruxa", EntityType.WITCH, Material.GLASS_BOTTLE),
    DEVASTADOR(Pagina.ILLAGERS, "Devastador", EntityType.RAVAGER, Material.SADDLE),
    VEX(Pagina.ILLAGERS, "Vex", EntityType.VEX, Material.IRON_SWORD),
    // Estranhos
    CREEPER(Pagina.ESTRANHOS, "Creeper", EntityType.CREEPER, Material.GUNPOWDER),
    SLIME(Pagina.ESTRANHOS, "Slime", EntityType.SLIME, Material.SLIME_BALL),
    BREEZE(Pagina.ESTRANHOS, "Breeze", EntityType.BREEZE, Material.BREEZE_ROD),
    GUARDIAO(Pagina.ESTRANHOS, "Guardião", EntityType.GUARDIAN, Material.PRISMARINE_SHARD),
    GUARDIAO_ANCIAO(Pagina.ESTRANHOS, "Guardião Ancião", EntityType.ELDER_GUARDIAN, Material.SPONGE),
    WARDEN(Pagina.ESTRANHOS, "Warden", EntityType.WARDEN, Material.SCULK),
    RANGEDOR(Pagina.ESTRANHOS, "Rangedor", EntityType.CREAKING, Material.PALE_OAK_LOG),
    // Fim
    ENDERMAN(Pagina.FIM, "Enderman", EntityType.ENDERMAN, Material.ENDER_PEARL),
    SHULKER(Pagina.FIM, "Shulker", EntityType.SHULKER, Material.SHULKER_SHELL),
    DRAGAO(Pagina.FIM, "Dragão do End", EntityType.ENDER_DRAGON, Material.DRAGON_HEAD),
    // Chefes do Altar
    CHEFE_GOLEM(Pagina.CHEFES, "Golem Ancestral", "golem", Material.IRON_BLOCK),
    CHEFE_RAINHA(Pagina.CHEFES, "Rainha Aracnídea", "rainha", Material.SPIDER_EYE),
    CHEFE_CHAMAS(Pagina.CHEFES, "Senhor das Chamas", "chamas", Material.BLAZE_POWDER),
    CHEFE_LICH(Pagina.CHEFES, "Lich", "lich", Material.SKELETON_SKULL),
    CHEFE_TEMPESTADE(Pagina.CHEFES, "Tempestade Viva", "tempestade", Material.WIND_CHARGE),
    CHEFE_ARAUTO(Pagina.CHEFES, "Arauto do Fim", "arauto", Material.END_CRYSTAL),
    // Peixes raros
    CARPA_DOURADA(Pagina.PEIXES, "Carpa Dourada", "CARPA_DOURADA", Material.COD),
    TRUTA_ARCO_IRIS(Pagina.PEIXES, "Truta Arco-Íris", "TRUTA_ARCO_IRIS", Material.SALMON),
    PEIXE_LUA(Pagina.PEIXES, "Peixe-Lua", "PEIXE_LUA", Material.TROPICAL_FISH),
    PEIXE_PEDRA(Pagina.PEIXES, "Peixe-Pedra", "PEIXE_PEDRA", Material.COD),
    ENGUIA_ELETRICA(Pagina.PEIXES, "Enguia Elétrica", "ENGUIA_ELETRICA", Material.COD),
    PEIXE_GELO(Pagina.PEIXES, "Peixe-Gelo", "PEIXE_GELO", Material.COD),
    KOI_CELESTE(Pagina.PEIXES, "Koi Celeste", "KOI_CELESTE", Material.TROPICAL_FISH),
    BAIACU_REI(Pagina.PEIXES, "Baiacu-Rei", "BAIACU_REI", Material.PUFFERFISH),
    SALMAO_REI(Pagina.PEIXES, "Salmão-Rei", "SALMAO_REI", Material.SALMON),
    PEIXE_FANTASMA(Pagina.PEIXES, "Peixe-Fantasma", "PEIXE_FANTASMA", Material.SALMON),
    PEIXE_ABISSAL(Pagina.PEIXES, "Peixe Abissal", "PEIXE_ABISSAL", Material.COD),
    PEIXE_DRAGAO(Pagina.PEIXES, "Peixe-Dragão", "PEIXE_DRAGAO", Material.TROPICAL_FISH);

    /** As páginas do álbum e o bônus de cada uma. */
    public enum Pagina {
        MORTOS_VIVOS("Mortos-vivos", Material.ROTTEN_FLESH, "+5% de dano em mortos-vivos"),
        ARTROPODES("Artrópodes", Material.SPIDER_EYE, "+5% de dano em aranhas, traças e endermites"),
        NETHER("Nether", Material.NETHERRACK, "+5% de dano nas criaturas do Nether"),
        ILLAGERS("Illagers", Material.CROSSBOW, "+5% de dano em saqueadores, bruxas e companhia"),
        ESTRANHOS("Estranhos", Material.SCULK, "+5% de dano em creepers, slimes, guardiões e afins"),
        FIM("Fim", Material.END_STONE, "+5% de dano nas criaturas do End"),
        CHEFES("Chefes", Material.WITHER_ROSE, "+5% de dano em chefes"),
        PEIXES("Peixes raros", Material.TROPICAL_FISH, "+2% de chance de peixe raro");

        private final String nome, bonus;
        private final Material icone;

        Pagina(String nome, Material icone, String bonus) {
            this.nome = nome;
            this.icone = icone;
            this.bonus = bonus;
        }

        public String nome() { return nome; }
        public Material icone() { return icone; }
        public String bonus() { return bonus; }

        public List<Carta> cartas() {
            List<Carta> l = new ArrayList<>();
            for (Carta c : Carta.values()) if (c.pagina == this) l.add(c);
            return l;
        }
    }

    private final Pagina pagina;
    private final String nome;
    private final EntityType tipo;
    /** Chefe do Altar (id) ou peixe raro (nome do enum). */
    private final String chave;
    private final Material icone;

    Carta(Pagina pagina, String nome, EntityType tipo, Material icone) {
        this.pagina = pagina;
        this.nome = nome;
        this.tipo = tipo;
        this.chave = null;
        this.icone = icone;
    }

    Carta(Pagina pagina, String nome, String chave, Material icone) {
        this.pagina = pagina;
        this.nome = nome;
        this.tipo = null;
        this.chave = chave;
        this.icone = icone;
    }

    public Pagina pagina() { return pagina; }
    public String nome() { return nome; }
    public EntityType tipo() { return tipo; }
    public String chave() { return chave; }
    public Material icone() { return icone; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Raridade da carta: comum (monstros), rara (peixes, Fim, chefes de masmorra) ou épica (chefes do Altar e o Dragão). */
    public int raridade() {
        if (pagina == Pagina.CHEFES || this == DRAGAO || this == WARDEN || this == GUARDIAO_ANCIAO) return 2;
        if (pagina == Pagina.PEIXES || pagina == Pagina.FIM || this == INVOCADOR || this == DEVASTADOR || this == BREEZE) return 1;
        return 0;
    }

    public static Carta deTipo(EntityType t) {
        for (Carta c : values()) if (c.tipo == t) return c;
        return null;
    }

    public static Carta deChave(String chave) {
        for (Carta c : values()) if (chave.equalsIgnoreCase(c.chave)) return c;
        return null;
    }

    public static Carta porId(String id) {
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
