package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Salas construídas pelos admins: salvas como estruturas em plugins/RPGAtributos/salas/<tipo>/<nome>.nbt
 * e sorteadas no lugar das salas geradas pelo código.
 */
public final class SalasPersonalizadas {

    /** Uma sala salva, já carregada. */
    record Modelo(String nome, Structure estrutura) {}

    private final RPGAtributos plugin;
    private final File pasta;
    private final Map<UUID, Location[]> selecao = new HashMap<>();
    private final Map<String, Structure> cache = new HashMap<>();

    public SalasPersonalizadas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.pasta = new File(plugin.getDataFolder(), "salas");
    }

    private File pasta(TipoSala t) {
        return new File(pasta, t.id());
    }

    public List<String> nomes(TipoSala t) {
        List<String> l = new ArrayList<>();
        File[] arquivos = pasta(t).listFiles((d, n) -> n.endsWith(".nbt"));
        if (arquivos != null) for (File f : arquivos) l.add(f.getName().substring(0, f.getName().length() - 4));
        l.sort(String::compareTo);
        return l;
    }

    /** Uma sala personalizada aleatória desse tipo, ou null se não houver nenhuma. */
    Modelo sortear(TipoSala t, Random r) {
        List<String> nomes = nomes(t);
        if (nomes.isEmpty()) return null;
        String nome = nomes.get(r.nextInt(nomes.size()));
        Structure s = carregar(t, nome);
        return s == null ? null : new Modelo(nome, s);
    }

    private Structure carregar(TipoSala t, String nome) {
        String chave = t.id() + "/" + nome;
        Structure s = cache.get(chave);
        if (s != null) return s;
        try {
            s = Bukkit.getStructureManager().loadStructure(new File(pasta(t), nome + ".nbt"));
            BlockVector tam = s.getSize();
            if (tam.getBlockX() != t.tamanho() || tam.getBlockZ() != t.tamanho() || tam.getBlockY() != t.altura()) {
                plugin.getLogger().warning("A sala " + chave + " tem o tamanho errado e foi ignorada.");
                return null;
            }
            cache.put(chave, s);
            return s;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui ler a sala " + chave + ": " + ex.getMessage());
            return null;
        }
    }

    // =====================================================================
    //  Ferramentas de admin
    // =====================================================================

    public void marcar(Player p, int qual, Location l) {
        Location[] s = selecao.computeIfAbsent(p.getUniqueId(), k -> new Location[2]);
        s[qual] = l.getBlock().getLocation();
    }

    /**
     * Monta uma moldura de vidro do tamanho certo, dois blocos à frente do jogador, e já
     * marca os dois cantos. As aberturas verdes são onde o plugin abre as portas.
     */
    public String modelo(Player p, TipoSala t) {
        World w = p.getWorld();
        Location base = p.getLocation().getBlock().getLocation().add(2, 0, 2);
        int tam = t.tamanho(), alt = t.altura(), meio = (tam - 1) / 2;
        int x0 = base.getBlockX(), y0 = base.getBlockY(), z0 = base.getBlockZ();
        for (int dx = 0; dx < tam; dx++) {
            for (int dz = 0; dz < tam; dz++) {
                boolean borda = dx == 0 || dz == 0 || dx == tam - 1 || dz == tam - 1;
                w.getBlockAt(x0 + dx, y0, z0 + dz).setType(Material.WHITE_STAINED_GLASS, false);
                if (!borda) continue;
                for (int dy = 1; dy < alt; dy++) {
                    boolean porta = dy <= 4 && (Math.abs(dx - meio) <= 1 || Math.abs(dz - meio) <= 1);
                    w.getBlockAt(x0 + dx, y0 + dy, z0 + dz).setType(porta ? Material.LIME_STAINED_GLASS : Material.GLASS, false);
                }
            }
        }
        marcar(p, 0, base);
        marcar(p, 1, base.clone().add(tam - 1, alt - 1, tam - 1));
        return null;
    }

    public String salvar(Player p, TipoSala t, String nome) {
        if (!nome.matches("[a-zA-Z0-9_-]{1,32}")) return "Nome inválido: use letras, números, _ ou - (até 32).";
        Location[] s = selecao.get(p.getUniqueId());
        if (s == null || s[0] == null || s[1] == null) return "Marque os dois cantos antes (/rpgadmin sala pos1 e pos2, ou /rpgadmin sala modelo).";
        if (!s[0].getWorld().equals(s[1].getWorld())) return "Os dois cantos precisam estar no mesmo mundo.";
        int x0 = Math.min(s[0].getBlockX(), s[1].getBlockX()), y0 = Math.min(s[0].getBlockY(), s[1].getBlockY());
        int z0 = Math.min(s[0].getBlockZ(), s[1].getBlockZ());
        int dx = Math.abs(s[0].getBlockX() - s[1].getBlockX()) + 1, dy = Math.abs(s[0].getBlockY() - s[1].getBlockY()) + 1;
        int dz = Math.abs(s[0].getBlockZ() - s[1].getBlockZ()) + 1;
        if (dx != t.tamanho() || dz != t.tamanho() || dy != t.altura()) {
            return "A sala de " + t.nome() + " precisa ter " + t.tamanho() + "x" + t.altura() + "x" + t.tamanho()
                    + " (largura x altura x comprimento, com paredes). A sua tem " + dx + "x" + dy + "x" + dz + ".";
        }
        Structure st = Bukkit.getStructureManager().createStructure();
        st.fill(new Location(s[0].getWorld(), x0, y0, z0), new BlockVector(dx, dy, dz), true);
        File destino = new File(pasta(t), nome.toLowerCase(Locale.ROOT) + ".nbt");
        destino.getParentFile().mkdirs();
        try {
            Bukkit.getStructureManager().saveStructure(destino, st);
        } catch (IOException ex) {
            return "Não consegui salvar: " + ex.getMessage();
        }
        cache.remove(t.id() + "/" + nome.toLowerCase(Locale.ROOT));
        return null;
    }

    public boolean apagar(TipoSala t, String nome) {
        cache.remove(t.id() + "/" + nome);
        return new File(pasta(t), nome + ".nbt").delete();
    }
}
