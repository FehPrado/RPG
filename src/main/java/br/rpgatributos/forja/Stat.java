package br.rpgatributos.forja;

import br.rpgatributos.StatsManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;

import java.util.Set;

import static br.rpgatributos.forja.Categoria.*;

/**
 * Bônus que um item forjado pode ganhar.
 * Os que têm atributo usam os atributos nativos do Minecraft; os outros
 * (crítico, roubo de vida, esquiva...) são calculados pelo plugin em combate.
 *
 * Valores mínimo/máximo são para um item Comum; depois multiplicam pela
 * raridade e, se {@code usaTier}, pelo material.
 */
public enum Stat {
    // ---------- armas ----------
    DANO("Dano", "⚔", Attribute.ATTACK_DAMAGE, Operation.ADD_NUMBER, 0.4, 1.0, true, false,
            doTipo(Tipo.CORPO_A_CORPO)),
    VEL_ATAQUE("Vel. de ataque", "⚡", Attribute.ATTACK_SPEED, Operation.ADD_NUMBER, 0.05, 0.15, false, false,
            doTipo(Tipo.CORPO_A_CORPO)),
    ALCANCE("Alcance de ataque", "↔", Attribute.ENTITY_INTERACTION_RANGE, Operation.ADD_NUMBER, 0.1, 0.3, false, false,
            doTipo(Tipo.CORPO_A_CORPO)),
    REPULSAO("Repulsão", "⇒", Attribute.ATTACK_KNOCKBACK, Operation.ADD_NUMBER, 0.1, 0.3, false, false,
            de(MACHADO, MACA, LANCA)),
    VARREDURA("Dano em área", "≈", Attribute.SWEEPING_DAMAGE_RATIO, Operation.ADD_NUMBER, 0.05, 0.12, false, true,
            de(ESPADA)),
    CRITICO("Chance de crítico", "✷", null, null, 0.02, 0.05, false, true,
            doTipo(Tipo.CORPO_A_CORPO, Tipo.DISTANCIA)),
    ROUBO_VIDA("Roubo de vida", "❥", null, null, 0.01, 0.03, false, true,
            doTipo(Tipo.CORPO_A_CORPO, Tipo.DISTANCIA)),
    DANO_FLECHA("Dano de flecha", "➶", null, null, 0.04, 0.10, false, true,
            doTipo(Tipo.DISTANCIA)),

    // ---------- ferramentas ----------
    VEL_MINERACAO("Vel. de mineração", "⛏", Attribute.BLOCK_BREAK_SPEED, Operation.ADD_SCALAR, 0.04, 0.10, false, true,
            ferramentas()),
    EFICIENCIA("Eficiência", "⛏", Attribute.MINING_EFFICIENCY, Operation.ADD_NUMBER, 1.0, 2.5, true, false,
            ferramentas()),
    ALCANCE_BLOCO("Alcance de blocos", "↔", Attribute.BLOCK_INTERACTION_RANGE, Operation.ADD_NUMBER, 0.2, 0.5, false, false,
            ferramentas()),
    SORTE("Sorte", "☘", Attribute.LUCK, Operation.ADD_NUMBER, 0.3, 1.0, false, false,
            de(ENXADA, PA)),

    // ---------- defesa ----------
    VIDA("Vida máxima", "❤", Attribute.MAX_HEALTH, Operation.ADD_NUMBER, 0.4, 1.0, true, false,
            doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    ARMADURA("Armadura", "⛨", Attribute.ARMOR, Operation.ADD_NUMBER, 0.3, 0.8, true, false,
            doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    RESISTENCIA("Resistência", "◈", Attribute.ARMOR_TOUGHNESS, Operation.ADD_NUMBER, 0.2, 0.6, true, false,
            doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    RES_REPULSAO("Res. a repulsão", "⚓", Attribute.KNOCKBACK_RESISTANCE, Operation.ADD_NUMBER, 0.02, 0.05, false, true,
            doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    ESQUIVA("Esquiva", "↺", null, null, 0.01, 0.03, false, true,
            doTipo(Tipo.ARMADURA)),
    ESPINHOS("Reflete dano", "✹", null, null, 0.03, 0.08, false, true,
            doTipo(Tipo.ARMADURA, Tipo.ESCUDO)),
    VELOCIDADE("Velocidade", "≫", Attribute.MOVEMENT_SPEED, Operation.ADD_SCALAR, 0.01, 0.03, false, true,
            de(BOTAS, CALCA)),
    PULO("Altura do pulo", "⬆", Attribute.JUMP_STRENGTH, Operation.ADD_SCALAR, 0.02, 0.05, false, true,
            de(BOTAS)),
    QUEDA_SEGURA("Queda segura (blocos)", "☁", Attribute.SAFE_FALL_DISTANCE, Operation.ADD_NUMBER, 0.3, 1.0, false, false,
            de(BOTAS)),
    NADO("Agilidade na água", "≋", Attribute.WATER_MOVEMENT_EFFICIENCY, Operation.ADD_NUMBER, 0.05, 0.12, false, true,
            de(BOTAS, CALCA)),
    AGACHADO("Vel. agachado", "↓", Attribute.SNEAKING_SPEED, Operation.ADD_NUMBER, 0.03, 0.08, false, true,
            de(CALCA)),
    RES_EXPLOSAO("Res. a explosões", "✸", Attribute.EXPLOSION_KNOCKBACK_RESISTANCE, Operation.ADD_NUMBER, 0.05, 0.10, false, true,
            de(PEITORAL, CALCA)),
    ANTI_FOGO("Tempo em chamas", "♨", Attribute.BURNING_TIME, Operation.ADD_SCALAR, -0.05, -0.12, false, true,
            de(PEITORAL, CAPACETE)),
    FOLEGO("Fôlego", "○", Attribute.OXYGEN_BONUS, Operation.ADD_NUMBER, 0.3, 1.0, false, false,
            de(CAPACETE)),
    MINERACAO_AQUATICA("Mineração na água", "≋", Attribute.SUBMERGED_MINING_SPEED, Operation.ADD_SCALAR, 0.1, 0.3, false, true,
            de(CAPACETE)),

    // ---------- todos ----------
    INQUEBRAVEL("Chance de não gastar durabilidade", "♦", null, null, 0.05, 0.12, false, true,
            todas());

    private final String nome;
    private final String icone;
    private final Attribute atributo;
    private final Operation operacao;
    private final double min;
    private final double max;
    private final boolean usaTier;
    private final boolean porcentagem;
    private final Set<Categoria> categorias;

    Stat(String nome, String icone, Attribute atributo, Operation operacao, double min, double max,
         boolean usaTier, boolean porcentagem, Set<Categoria> categorias) {
        this.nome = nome;
        this.icone = icone;
        this.atributo = atributo;
        this.operacao = operacao;
        this.min = min;
        this.max = max;
        this.usaTier = usaTier;
        this.porcentagem = porcentagem;
        this.categorias = categorias;
    }

    public String nome() { return nome; }
    public String icone() { return icone; }
    /** null = bônus especial calculado pelo plugin. */
    public Attribute atributo() { return atributo; }
    public Operation operacao() { return operacao; }
    public boolean podeEm(Categoria c) { return categorias.contains(c); }

    /** Valor final do bônus. {@code qualidade} vai de 0 (pior) a 1 (melhor). */
    public double valor(double qualidade, double multRaridade, Tier tier) {
        double v = (min + (max - min) * qualidade) * multRaridade * (usaTier ? tier.multiplicador() : 1);
        double casas = porcentagem ? 1000 : 100;
        return Math.round(v * casas) / casas;
    }

    /** "+1,4" ou "+12%" */
    public String formatar(double v) {
        String sinal = v < 0 ? "-" : "+";
        double abs = Math.abs(v);
        return porcentagem ? sinal + StatsManager.fmt(Math.round(abs * 1000) / 10.0) + "%" : sinal + StatsManager.fmt(abs);
    }

    public static Stat porNome(String nome) {
        try {
            return valueOf(nome);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
