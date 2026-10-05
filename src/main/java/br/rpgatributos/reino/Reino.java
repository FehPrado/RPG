package br.rpgatributos.reino;

import br.rpgatributos.territorio.Flag;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Um reino: o Rei, os membros com cargos, o tesouro, as tréguas e as leis. Os territórios dos membros são as províncias. */
public final class Reino {

    final UUID id;
    String nome;
    NamedTextColor cor;
    UUID rei;
    final Map<UUID, Cargo> membros = new LinkedHashMap<>();
    final Map<UUID, String> nomes = new HashMap<>();
    /** Leis do reino: regras de território que ficam proibidas em todas as províncias. */
    final EnumSet<Flag> leis = EnumSet.noneOf(Flag.class);
    /** Esmeraldas guardadas no tesouro. */
    long tesouro;
    /** Chunks a mais que o território do Rei pode ter (ganhos em guerras). */
    int chunksConquistados;
    int vitorias;
    /** Outro reino → até quando não pode haver guerra entre os dois (trégua). */
    final Map<UUID, Long> treguas = new HashMap<>();

    Reino(UUID id, String nome, NamedTextColor cor, UUID rei, String nomeRei) {
        this.id = id;
        this.nome = nome;
        this.cor = cor;
        this.rei = rei;
        membros.put(rei, Cargo.REI);
        nomes.put(rei, nomeRei);
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public NamedTextColor cor() { return cor; }
    public UUID rei() { return rei; }
    public long tesouro() { return tesouro; }
    public int vitorias() { return vitorias; }
    public Map<UUID, Cargo> membros() { return Collections.unmodifiableMap(membros); }

    public Cargo cargo(UUID jogador) { return membros.get(jogador); }

    public boolean membro(UUID jogador) { return membros.containsKey(jogador); }

    public String nomeDe(UUID jogador) { return nomes.getOrDefault(jogador, "?"); }

    public String nomeRei() { return nomeDe(rei); }

    /** A lei do reino proíbe essa regra nas províncias? */
    public boolean proibe(Flag f) { return leis.contains(f); }
}
