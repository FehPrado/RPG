package br.rpgatributos.classe;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

/**
 * O que o jogador tem de classes: a ativa, o XP de cada classe que já aprendeu (o histórico),
 * o caminho que está seguindo (e de onde as tarefas começaram a contar) e a habilidade selecionada.
 */
public final class PerfilClasse {

    private static final NamespacedKey K_ATIVA = new NamespacedKey("rpgatributos", "classe_ativa");
    private static final NamespacedKey K_XP = new NamespacedKey("rpgatributos", "classe_xp");
    private static final NamespacedKey K_ALVO = new NamespacedKey("rpgatributos", "classe_alvo");
    private static final NamespacedKey K_BASE = new NamespacedKey("rpgatributos", "classe_base");
    private static final NamespacedKey K_HAB = new NamespacedKey("rpgatributos", "classe_habilidade");

    Classe ativa;
    /** Classes aprendidas → XP acumulado nelas. */
    final Map<Classe, Double> xp = new EnumMap<>(Classe.class);
    /** Caminho sendo seguido (tarefas contando), ou null. */
    Classe alvo;
    /** Valor de cada contador quando o caminho começou. */
    final Map<Tarefa.Tipo, Long> base = new EnumMap<>(Tarefa.Tipo.class);
    int selecionada;

    public Classe ativa() { return ativa; }
    public Classe alvo() { return alvo; }
    public boolean aprendeu(Classe c) { return xp.containsKey(c); }

    static PerfilClasse ler(Player p) {
        PerfilClasse pf = new PerfilClasse();
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pf.ativa = Classe.porId(pdc.getOrDefault(K_ATIVA, PersistentDataType.STRING, ""));
        pf.alvo = Classe.porId(pdc.getOrDefault(K_ALVO, PersistentDataType.STRING, ""));
        pf.selecionada = pdc.getOrDefault(K_HAB, PersistentDataType.INTEGER, 0);
        for (String par : pdc.getOrDefault(K_XP, PersistentDataType.STRING, "").split(";")) {
            String[] kv = par.split(":");
            Classe c = kv.length == 2 ? Classe.porId(kv[0]) : null;
            if (c == null) continue;
            try {
                pf.xp.put(c, Double.parseDouble(kv[1]));
            } catch (NumberFormatException ignorado) {
                // valor estragado: ignora
            }
        }
        for (String par : pdc.getOrDefault(K_BASE, PersistentDataType.STRING, "").split(";")) {
            String[] kv = par.split(":");
            if (kv.length != 2) continue;
            try {
                pf.base.put(Tarefa.Tipo.valueOf(kv[0]), Long.parseLong(kv[1]));
            } catch (IllegalArgumentException ignorado) {
                // tipo que não existe mais
            }
        }
        if (pf.ativa != null && !pf.xp.containsKey(pf.ativa)) pf.xp.put(pf.ativa, 0.0);
        return pf;
    }

    void salvar(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (ativa == null) pdc.remove(K_ATIVA);
        else pdc.set(K_ATIVA, PersistentDataType.STRING, ativa.name());
        if (alvo == null) pdc.remove(K_ALVO);
        else pdc.set(K_ALVO, PersistentDataType.STRING, alvo.name());
        pdc.set(K_HAB, PersistentDataType.INTEGER, selecionada);
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Classe, Double> en : xp.entrySet()) {
            if (!s.isEmpty()) s.append(';');
            s.append(en.getKey().name()).append(':').append(Math.round(en.getValue() * 100) / 100.0);
        }
        pdc.set(K_XP, PersistentDataType.STRING, s.toString());
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Tarefa.Tipo, Long> en : base.entrySet()) {
            if (!b.isEmpty()) b.append(';');
            b.append(en.getKey().name()).append(':').append(en.getValue());
        }
        pdc.set(K_BASE, PersistentDataType.STRING, b.toString());
    }
}
