package br.rpgatributos.missoes;

import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Raridade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Um pedido de aldeão: entregar itens, caçar monstros ou forjar um equipamento.
 * Fica salvo como texto no aldeão (ofertas do dia) e no jogador (missões aceitas).
 */
public record Missao(Tipo tipo, String alvo, int quantidade, int esmeraldas, int xp, String extra) {

    public enum Tipo { ENTREGAR, CACAR, FORJAR }

    public Material material() {
        return tipo == Tipo.ENTREGAR ? Material.matchMaterial(alvo) : null;
    }

    public EntityType entidade() {
        if (tipo != Tipo.CACAR) return null;
        try {
            return EntityType.valueOf(alvo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Para FORJAR: "PEITORAL:RARO" → categoria e raridade mínima. */
    public Categoria categoria() {
        if (tipo != Tipo.FORJAR) return null;
        try {
            return Categoria.valueOf(alvo.split(":")[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Raridade raridadeMinima() {
        if (tipo != Tipo.FORJAR || !alvo.contains(":")) return Raridade.COMUM;
        Raridade r = Raridade.porId(alvo.split(":")[1]);
        return r == null ? Raridade.COMUM : r;
    }

    /** "Entregar 32x Trigo", "Caçar 10x Zumbi", "Forjar: Peitoral Raro ou melhor" */
    public Component descricao() {
        return switch (tipo) {
            case ENTREGAR -> {
                Material m = material();
                yield Component.text("Entregar " + quantidade + "x ", NamedTextColor.WHITE)
                        .append(m == null ? Component.text(alvo) : Component.translatable(m.translationKey()));
            }
            case CACAR -> {
                EntityType e = entidade();
                yield Component.text("Caçar " + quantidade + "x ", NamedTextColor.WHITE)
                        .append(e == null ? Component.text(alvo) : Component.translatable(e.translationKey()));
            }
            case FORJAR -> {
                Categoria c = categoria();
                Raridade r = raridadeMinima();
                yield Component.text("Entregar forjado: " + (c == null ? alvo : c.nome()) + " ", NamedTextColor.WHITE)
                        .append(Component.text(r.nome() + " ou melhor", r.cor()));
            }
        };
    }

    public String recompensaTexto() {
        StringBuilder sb = new StringBuilder(esmeraldas + " esmeraldas");
        if (xp > 0) sb.append(", ").append(xp).append(tipo == Tipo.ENTREGAR ? " de XP" : " de XP de " + (tipo == Tipo.CACAR ? "Combate" : "Ferraria"));
        if ("PEDRA".equals(extra)) sb.append(", 1 Pedra de Proteção");
        if ("FRAGMENTO".equals(extra)) sb.append(", 1 Fragmento de Forja");
        return sb.toString();
    }

    public Material icone() {
        return switch (tipo) {
            case ENTREGAR -> material() == null ? Material.PAPER : material();
            case CACAR -> Material.IRON_SWORD;
            case FORJAR -> Material.ANVIL;
        };
    }

    // ---------- texto salvo: "ENTREGAR;WHEAT;32;5;20;" ----------

    public String serializar() {
        return tipo.name() + ";" + alvo + ";" + quantidade + ";" + esmeraldas + ";" + xp + ";" + (extra == null ? "" : extra);
    }

    public static Missao ler(String texto) {
        String[] p = texto.split(";", -1);
        if (p.length < 6) return null;
        try {
            return new Missao(Tipo.valueOf(p[0]), p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), p[5]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
