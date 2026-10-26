package br.rpgatributos.masmorra;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Inscrições (2.72): escritas na Mesa de Inscrição e aplicadas num Cristal de Portal. Cada uma põe
 * um modificador, mais tempo, uma sala a mais ou troca o objetivo. Um cristal aceita até 4.
 */
public enum Inscricao {
    FURIA(ModificadorPortal.FURIA, 4),
    COURACA(ModificadorPortal.COURACA, 4),
    RELOGIO_CURTO(ModificadorPortal.RELOGIO_CURTO, 4),
    SEM_CURA(ModificadorPortal.SEM_CURA, 4),
    ONDA_EXTRA(ModificadorPortal.ONDA_EXTRA, 4),
    TESOURO_FARTO(ModificadorPortal.TESOURO_FARTO, 12),
    BENCAO(ModificadorPortal.BENCAO, 10),
    PRESSA(ModificadorPortal.PRESSA, 8),
    TEMPO("Tempo", "+3 minutos no portal.", 8, 2),
    TESOURO("Tesouro", "Uma sala de tesouro a mais.", 12, 2),
    MINA("Mina", "Uma Mina de Cristal a mais.", 8, 2),
    CACADA(Objetivo.CACADA, 6),
    SAQUE(Objetivo.SAQUE, 6),
    MONOLITOS(Objetivo.MONOLITOS, 6);

    /** Quantas inscrições cabem num cristal. */
    public static final int MAXIMO = 4;
    private static final NamespacedKey K = new NamespacedKey("rpgatributos", "inscricao");

    private final String nome, descricao;
    private final int preco, maximo;
    private final ModificadorPortal mod;
    private final Objetivo objetivo;

    Inscricao(ModificadorPortal mod, int preco) {
        this.nome = mod.nome();
        this.descricao = mod.descricao();
        this.preco = preco;
        this.maximo = 1;
        this.mod = mod;
        this.objetivo = null;
    }

    Inscricao(String nome, String descricao, int preco, int maximo) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.maximo = maximo;
        this.mod = null;
        this.objetivo = null;
    }

    Inscricao(Objetivo o, int preco) {
        this.nome = "Objetivo: " + o.nome();
        this.descricao = "O objetivo vira " + o.nome() + ".";
        this.preco = preco;
        this.maximo = 1;
        this.mod = null;
        this.objetivo = o;
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    /** Mana para escrever (além de 1 papel). */
    public int preco() { return preco; }
    /** Quantas desta cabem no mesmo cristal. */
    public int maximo() { return maximo; }
    public ModificadorPortal mod() { return mod; }
    public Objetivo objetivo() { return objetivo; }

    public TextColor cor() {
        if (mod != null) return mod.bom() ? NamedTextColor.GREEN : NamedTextColor.RED;
        if (objetivo != null) return NamedTextColor.GOLD;
        return NamedTextColor.AQUA;
    }

    public ItemStack criar(int qtd) {
        ItemStack i = new ItemStack(Material.PAPER, Math.max(1, qtd));
        i.editMeta(m -> {
            m.itemName(Component.text("Inscrição: " + nome, cor()));
            m.lore(List.of(Component.text(descricao, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Aplique num Cristal de Portal na Mesa de Inscrição.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(K, PersistentDataType.STRING, name());
        });
        EquipamentoPortal.aplicarVisual(i);
        return i;
    }

    public static Inscricao de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K, PersistentDataType.STRING);
        if (s == null) return null;
        try { return valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    public static NamespacedKey chave() { return K; }

    // ---- o que fica gravado no cristal: "TEMPO:2,TESOURO:1" ----

    public static String texto(Map<Inscricao, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Inscricao, Integer> e : m.entrySet()) {
            if (e.getValue() <= 0) continue;
            if (!sb.isEmpty()) sb.append(',');
            sb.append(e.getKey().name()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    public static EnumMap<Inscricao, Integer> ler(String s) {
        EnumMap<Inscricao, Integer> m = new EnumMap<>(Inscricao.class);
        if (s == null || s.isBlank()) return m;
        for (String par : s.split(",")) {
            String[] p = par.split(":");
            try { m.put(valueOf(p[0]), p.length > 1 ? Integer.parseInt(p[1]) : 1); } catch (IllegalArgumentException ignorado) { }
        }
        return m;
    }

    public static int total(Map<Inscricao, Integer> m) {
        int n = 0;
        for (int v : m.values()) n += v;
        return n;
    }
}
