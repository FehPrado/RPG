package br.rpgatributos.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Um grupo de jogadores: um líder, os membros (com o último nome conhecido) e o fogo amigo. */
public final class Party {

    private final UUID id;
    private UUID lider;
    /** Todos os membros, incluindo o líder (UUID → nome). */
    private final Map<UUID, String> membros = new LinkedHashMap<>();
    private boolean fogoAmigo;

    Party(UUID id, UUID lider, String nomeLider) {
        this.id = id;
        this.lider = lider;
        membros.put(lider, nomeLider);
    }

    public UUID id() { return id; }
    public UUID lider() { return lider; }
    void lider(UUID l) { lider = l; }
    public String nomeLider() { return membros.getOrDefault(lider, "?"); }

    public Map<UUID, String> membros() { return Collections.unmodifiableMap(membros); }
    Map<UUID, String> membrosEditaveis() { return membros; }
    public boolean tem(UUID jogador) { return membros.containsKey(jogador); }
    public int tamanho() { return membros.size(); }

    public boolean fogoAmigo() { return fogoAmigo; }
    void fogoAmigo(boolean f) { fogoAmigo = f; }

    /** Membros online agora. */
    public List<Player> online() {
        List<Player> l = new ArrayList<>();
        for (UUID u : membros.keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) l.add(p);
        }
        return l;
    }
}
