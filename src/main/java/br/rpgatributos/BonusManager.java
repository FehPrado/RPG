package br.rpgatributos;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Transforma nível em bônus reais usando os atributos nativos do Minecraft.
 * Cada bônus usa uma chave própria, então reaplicar só substitui o valor antigo.
 */
public final class BonusManager {

    private final RPGAtributos plugin;
    private final NamespacedKey kVelocidade, kDano, kVida, kPulo, kQuedaSegura, kDanoQueda, kQuebra;
    private final NamespacedKey kNado, kFolego, kMinerarAgua;

    public BonusManager(RPGAtributos plugin) {
        this.plugin = plugin;
        kNado = new NamespacedKey(plugin, "natacao_agilidade");
        kFolego = new NamespacedKey(plugin, "natacao_folego");
        kMinerarAgua = new NamespacedKey(plugin, "natacao_mineracao");
        kVelocidade = new NamespacedKey(plugin, "corrida");
        kDano = new NamespacedKey(plugin, "combate_dano");
        kVida = new NamespacedKey(plugin, "combate_vida");
        kPulo = new NamespacedKey(plugin, "pulo_forca");
        kQuedaSegura = new NamespacedKey(plugin, "pulo_queda_segura");
        kDanoQueda = new NamespacedKey(plugin, "pulo_dano_queda");
        kQuebra = new NamespacedKey(plugin, "velocidade_quebra");
    }

    private Settings cfg() { return plugin.settings(); }
    private int nivel(Player p, Skill s) { return plugin.stats().getNivel(p, s); }

    /** Reaplica todos os bônus fixos do jogador (chamado ao entrar e ao subir de nível). */
    public void aplicar(Player p) {
        int corrida = nivel(p, Skill.CORRIDA);
        int combate = nivel(p, Skill.COMBATE);
        int pulo = nivel(p, Skill.PULO);

        definir(p, Attribute.MOVEMENT_SPEED, kVelocidade, corrida * cfg().corVelocidade, AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.ATTACK_DAMAGE, kDano, combate * cfg().comDano, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MAX_HEALTH, kVida, combate * cfg().comVida, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.JUMP_STRENGTH, kPulo, pulo * cfg().puloForca, AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.SAFE_FALL_DISTANCE, kQuedaSegura, pulo * cfg().puloQuedaSegura, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.FALL_DAMAGE_MULTIPLIER, kDanoQueda,
                -Math.min(pulo * cfg().puloReducaoQueda, cfg().puloReducaoMax), AttributeModifier.Operation.ADD_SCALAR);

        int natacao = nivel(p, Skill.NATACAO);
        definir(p, Attribute.WATER_MOVEMENT_EFFICIENCY, kNado, natacao * cfg().natAgilidade, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.OXYGEN_BONUS, kFolego, natacao * cfg().natFolego, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.SUBMERGED_MINING_SPEED, kMinerarAgua, natacao * cfg().natMineracao, AttributeModifier.Operation.ADD_NUMBER);
    }

    /**
     * Velocidade de quebra muda conforme o bloco (pedra usa Mineração, tronco usa Madeira),
     * então ela é ajustada no momento em que o jogador começa a quebrar.
     */
    public void definirVelocidadeQuebra(Player p, double bonus) {
        AttributeInstance inst = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(kQuebra);
        double valorAtual = atual == null ? 0 : atual.getAmount();
        if (Math.abs(valorAtual - bonus) < 1e-9) return;
        definir(p, Attribute.BLOCK_BREAK_SPEED, kQuebra, bonus, AttributeModifier.Operation.ADD_SCALAR);
    }

    private void definir(Player p, Attribute atributo, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(atributo);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) {
            inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        }
        // Se a vida máxima diminuiu (ex.: reset), não deixa o jogador com vida acima do máximo.
        if (atributo == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) {
            p.setHealth(inst.getValue());
        }
    }

    /** Texto curto com o bônus atual de um atributo, usado no /atributos e ao subir de nível. */
    public String descricao(Skill s, int nivel) {
        Settings c = cfg();
        return switch (s) {
            case MINERACAO -> "+" + pct(nivel * c.minVelocidade) + " vel. de mineração, "
                    + pct(Math.min(1, nivel * c.minChanceDupla)) + " minério duplo";
            case MADEIRA -> "+" + pct(nivel * c.madVelocidade) + " vel. no machado, "
                    + pct(Math.min(1, nivel * c.madChanceDupla)) + " tronco duplo";
            case CORRIDA -> "+" + pct(nivel * c.corVelocidade) + " velocidade";
            case PULO -> "+" + pct(nivel * c.puloForca) + " pulo, -"
                    + pct(Math.min(nivel * c.puloReducaoQueda, c.puloReducaoMax)) + " dano de queda";
            case COMBATE -> "+" + StatsManager.fmt(nivel * c.comDano) + " dano, +"
                    + StatsManager.fmt(nivel * c.comVida / 2.0) + " ❤";
            case FERRARIA -> plugin.forja().descricaoFerraria(nivel);
            case ARCANO -> plugin.arcano().descricaoNivel(nivel);
            case NATACAO -> "+" + pct(nivel * c.natAgilidade) + " agilidade na água, +"
                    + StatsManager.fmt(nivel * c.natFolego) + " fôlego, mineração submersa mais rápida";
            case AGRICULTURA -> pct(Math.min(1, nivel * c.agrChanceDupla)) + " colheita dupla, toque verde "
                    + pct(nivel * c.agrToqueVerde) + ", colheitas de mais qualidade";
            case CULINARIA -> "+" + pct(nivel * c.culDuracaoExtra) + " duração dos pratos, pratos de mais qualidade";
            case DOMA -> c.domaCompanheiros(nivel) + " companheiro(s), " + c.domaComponentes(nivel) + " componente(s) cada, +"
                    + pct(nivel * c.domaVida) + " vida e +" + pct(nivel * c.domaDano) + " dano deles";
        };
    }

    private static String pct(double v) {
        return StatsManager.fmt(Math.round(v * 1000) / 10.0) + "%";
    }
}
