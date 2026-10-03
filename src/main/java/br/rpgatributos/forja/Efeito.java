package br.rpgatributos.forja;

import br.rpgatributos.StatsManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

import static br.rpgatributos.forja.Categoria.*;

/**
 * Efeitos especiais de itens Épicos ou melhores, feitos com os efeitos nativos
 * do Minecraft (poções, fogo, congelamento, raio, partículas e sons do jogo).
 *
 * No texto: {chance} = "15%", {nivel} = " II", {seg} = duração em segundos.
 */
public enum Efeito {
    // ================= enquanto equipado =================
    VELOCIDADE(Gatilho.PASSIVO, PotionEffectType.SPEED, Raridade.UNICO, 1, 0, 0,
            "Velocidade{nivel}", null, null, de(BOTAS, CALCA)),
    PRESSA(Gatilho.PASSIVO, PotionEffectType.HASTE, Raridade.EPICO, 1, 0, 0,
            "Pressa{nivel}", null, null, ferramentas()),
    FORCA(Gatilho.PASSIVO, PotionEffectType.STRENGTH, Raridade.LENDARIO, 0, 0, 0,
            "Força{nivel}", null, null, doTipo(Tipo.CORPO_A_CORPO)),
    VISAO_NOTURNA(Gatilho.PASSIVO, PotionEffectType.NIGHT_VISION, Raridade.EPICO, 0, 0, 0,
            "Visão noturna", null, null, de(CAPACETE, PICARETA)),
    RESPIRACAO(Gatilho.PASSIVO, PotionEffectType.WATER_BREATHING, Raridade.EPICO, 0, 0, 0,
            "Respiração aquática", null, null, de(CAPACETE)),
    SALTO(Gatilho.PASSIVO, PotionEffectType.JUMP_BOOST, Raridade.EPICO, 1, 0, 0,
            "Salto{nivel}", null, null, de(BOTAS)),
    RES_FOGO(Gatilho.PASSIVO, PotionEffectType.FIRE_RESISTANCE, Raridade.UNICO, 0, 0, 0,
            "Resistência ao fogo", null, null, de(PEITORAL, CALCA)),
    GOLFINHO(Gatilho.PASSIVO, PotionEffectType.DOLPHINS_GRACE, Raridade.UNICO, 0, 0, 0,
            "Graça do golfinho", null, null, de(BOTAS, CALCA)),
    RESISTENCIA(Gatilho.PASSIVO, PotionEffectType.RESISTANCE, Raridade.LENDARIO, 0, 0, 0,
            "Resistência{nivel}", null, null, de(PEITORAL, ESCUDO)),
    REGENERACAO(Gatilho.PASSIVO, PotionEffectType.REGENERATION, Raridade.LENDARIO, 0, 0, 0,
            "Regeneração{nivel}", null, null, de(PEITORAL, CAPACETE)),
    HEROI(Gatilho.PASSIVO, PotionEffectType.HERO_OF_THE_VILLAGE, Raridade.LENDARIO, 0, 0, 0,
            "Herói da vila (descontos com aldeões)", null, null, de(CAPACETE, PEITORAL)),
    CONDUITE(Gatilho.PASSIVO, PotionEffectType.CONDUIT_POWER, Raridade.MITICO, 0, 0, 0,
            "Poder do conduíte", null, null, de(CAPACETE)),

    // ================= ao acertar =================
    LENTIDAO(Gatilho.AO_ACERTAR, PotionEffectType.SLOWNESS, Raridade.EPICO, 1, 60, 0.12,
            "{chance} de chance de aplicar Lentidão{nivel} ao acertar ({seg}s)",
            Particle.ITEM_SNOWBALL, Sound.BLOCK_POWDER_SNOW_STEP, armas()),
    VENENO(Gatilho.AO_ACERTAR, PotionEffectType.POISON, Raridade.EPICO, 1, 80, 0.10,
            "{chance} de chance de aplicar Veneno{nivel} ao acertar ({seg}s)",
            Particle.ITEM_SLIME, Sound.ENTITY_WITCH_THROW, armas()),
    FRAQUEZA(Gatilho.AO_ACERTAR, PotionEffectType.WEAKNESS, Raridade.EPICO, 0, 80, 0.10,
            "{chance} de chance de aplicar Fraqueza ao acertar ({seg}s)",
            Particle.SMOKE, Sound.ENTITY_ILLUSIONER_CAST_SPELL, armas()),
    BRILHO(Gatilho.AO_ACERTAR, PotionEffectType.GLOWING, Raridade.EPICO, 0, 140, 0.25,
            "{chance} de chance de marcar o alvo com Brilho ({seg}s)",
            Particle.GLOW, Sound.BLOCK_AMETHYST_BLOCK_CHIME, armas()),
    CHAMAS(Gatilho.AO_ACERTAR, null, Raridade.EPICO, 0, 80, 0.12,
            "{chance} de chance de incendiar o alvo ({seg}s)",
            Particle.FLAME, Sound.ITEM_FIRECHARGE_USE, armas()),
    CONGELAR(Gatilho.AO_ACERTAR, null, Raridade.UNICO, 0, 80, 0.10,
            "{chance} de chance de congelar o alvo",
            Particle.SNOWFLAKE, Sound.ENTITY_PLAYER_HURT_FREEZE, armas()),
    CEGUEIRA(Gatilho.AO_ACERTAR, PotionEffectType.BLINDNESS, Raridade.UNICO, 0, 40, 0.06,
            "{chance} de chance de cegar o alvo ({seg}s)",
            Particle.SQUID_INK, Sound.ENTITY_EVOKER_CAST_SPELL, armas()),
    MURCHAR(Gatilho.AO_ACERTAR, PotionEffectType.WITHER, Raridade.LENDARIO, 1, 80, 0.08,
            "{chance} de chance de aplicar Definhamento{nivel} ao acertar ({seg}s)",
            Particle.SCULK_SOUL, Sound.ENTITY_WITHER_SHOOT, armas()),
    LEVITACAO(Gatilho.AO_ACERTAR, PotionEffectType.LEVITATION, Raridade.LENDARIO, 0, 30, 0.05,
            "{chance} de chance de fazer o alvo levitar",
            Particle.END_ROD, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, armas()),
    RAIO(Gatilho.AO_ACERTAR, null, Raridade.MITICO, 0, 0, 0.06,
            "{chance} de chance de invocar um raio (+5 de dano)",
            Particle.ELECTRIC_SPARK, Sound.ITEM_TRIDENT_THUNDER, armas()),
    EXECUCAO(Gatilho.AO_ACERTAR, null, Raridade.MITICO, 0, 0, 1,
            "+35% de dano em alvos com menos de 30% de vida",
            Particle.DAMAGE_INDICATOR, Sound.ENTITY_PLAYER_ATTACK_STRONG, armas()),

    // ================= ao ser atingido =================
    ABSORCAO(Gatilho.AO_SER_ATINGIDO, PotionEffectType.ABSORPTION, Raridade.EPICO, 1, 120, 0.10,
            "{chance} de chance de ganhar Absorção{nivel} ao ser atingido ({seg}s)",
            Particle.WAX_ON, Sound.ITEM_ARMOR_EQUIP_GENERIC, doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    FUGA(Gatilho.AO_SER_ATINGIDO, PotionEffectType.SPEED, Raridade.EPICO, 1, 60, 0.10,
            "{chance} de chance de ganhar Velocidade{nivel} ao ser atingido ({seg}s)",
            Particle.CLOUD, Sound.ENTITY_BREEZE_WIND_BURST, doTipo(Tipo.ARMADURA)),
    CONTRA_LENTIDAO(Gatilho.AO_SER_ATINGIDO, PotionEffectType.SLOWNESS, Raridade.EPICO, 1, 60, 0.12,
            "{chance} de chance de aplicar Lentidão{nivel} em quem te atacar ({seg}s)",
            Particle.ITEM_SNOWBALL, Sound.BLOCK_POWDER_SNOW_STEP, doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    CONTRA_CHAMAS(Gatilho.AO_SER_ATINGIDO, null, Raridade.UNICO, 0, 60, 0.12,
            "{chance} de chance de incendiar quem te atacar ({seg}s)",
            Particle.FLAME, Sound.ITEM_FIRECHARGE_USE, doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    CURA(Gatilho.AO_SER_ATINGIDO, PotionEffectType.REGENERATION, Raridade.LENDARIO, 1, 60, 0.08,
            "{chance} de chance de ganhar Regeneração{nivel} ao ser atingido ({seg}s)",
            Particle.HEART, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, doTipo(Tipo.ARMADURA)),
    RAJADA(Gatilho.AO_SER_ATINGIDO, null, Raridade.LENDARIO, 0, 0, 0.10,
            "{chance} de chance de repelir quem te atacar com uma rajada de vento",
            Particle.GUST, Sound.ENTITY_BREEZE_WIND_BURST, doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    BASTIAO(Gatilho.AO_SER_ATINGIDO, PotionEffectType.RESISTANCE, Raridade.MITICO, 1, 60, 0.06,
            "{chance} de chance de ganhar Resistência{nivel} ao ser atingido ({seg}s)",
            Particle.TOTEM_OF_UNDYING, Sound.ITEM_TOTEM_USE, doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),

    // ================= ao minerar =================
    FRENESI(Gatilho.AO_MINERAR, PotionEffectType.HASTE, Raridade.EPICO, 1, 100, 0.04,
            "{chance} de chance de ganhar Pressa{nivel} ao quebrar blocos ({seg}s)",
            Particle.WAX_OFF, Sound.BLOCK_BEACON_POWER_SELECT, ferramentas()),
    SABEDORIA(Gatilho.AO_MINERAR, null, Raridade.UNICO, 0, 0, 0.25,
            "{chance} de chance de XP extra ao quebrar minérios",
            Particle.HAPPY_VILLAGER, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, ferramentas()),
    FUNDICAO(Gatilho.AO_MINERAR, null, Raridade.LENDARIO, 0, 0, 1,
            "Funde minérios brutos automaticamente",
            Particle.FLAME, Sound.BLOCK_FURNACE_FIRE_CRACKLE, de(PICARETA));

    public enum Gatilho { PASSIVO, AO_ACERTAR, AO_SER_ATINGIDO, AO_MINERAR }

    private final Gatilho gatilho;
    private final PotionEffectType pocao;
    private final Raridade minima;
    private final int nivelMax;
    private final int duracao;
    private final double chanceBase;
    private final String texto;
    private final Particle particula;
    private final Sound som;
    private final Set<Categoria> categorias;

    Efeito(Gatilho gatilho, PotionEffectType pocao, Raridade minima, int nivelMax, int duracao, double chanceBase,
           String texto, Particle particula, Sound som, Set<Categoria> categorias) {
        this.gatilho = gatilho;
        this.pocao = pocao;
        this.minima = minima;
        this.nivelMax = nivelMax;
        this.duracao = duracao;
        this.chanceBase = chanceBase;
        this.texto = texto;
        this.particula = particula;
        this.som = som;
        this.categorias = categorias;
    }

    private static Set<Categoria> armas() {
        return doTipo(Tipo.CORPO_A_CORPO, Tipo.DISTANCIA);
    }

    public Gatilho gatilho() { return gatilho; }
    /** Efeito de poção usado (null nos efeitos feitos à mão, como fogo e raio). */
    public PotionEffectType pocao() { return pocao; }
    public Raridade minima() { return minima; }
    /** Nível máximo do efeito (0 = I, 1 = II...). */
    public int nivelMax() { return nivelMax; }
    /** Duração em ticks (20 ticks = 1 segundo). */
    public int duracao() { return duracao; }
    public double chanceBase() { return chanceBase; }
    public Particle particula() { return particula; }
    public Sound som() { return som; }
    /** Só existe em itens Míticos. */
    public boolean exclusivoMitico() { return minima == Raridade.MITICO; }

    public boolean podeEm(Categoria c, Raridade r) {
        return categorias.contains(c) && r.peloMenos(minima);
    }

    /** Texto mostrado no item. */
    public String descrever(int nivel, double chance, Categoria cat) {
        String nivelTxt = (nivelMax > 0 || nivel > 0) ? " " + romano(nivel + 1) : "";
        String t = texto.replace("{nivel}", nivelTxt)
                .replace("{chance}", StatsManager.fmt(Math.round(chance * 1000) / 10.0) + "%")
                .replace("{seg}", StatsManager.fmt(Math.round(duracao / 10.0) / 2.0));
        if (gatilho == Gatilho.PASSIVO) t += cat.armadura() ? " enquanto vestido" : " na mão";
        return t;
    }

    public static String romano(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    public static Efeito porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
