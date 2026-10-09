package br.rpgatributos.colonia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Uma colônia: a Prefeitura, os cidadãos, os baús de depósito e o que foi achado em volta. */
public final class Colonia {

    public static final int NIVEL_MAXIMO = 5;
    /** Sem território, a colônia enxerga só até esta distância da Prefeitura. */
    public static final int RAIO = 32;
    /** Quanto abaixo e acima da Prefeitura a varredura olha (no território inteiro). */
    public static final int ABAIXO = 40, ACIMA = 60;

    /** O que foi achado num chunk na última vez que ele estava carregado. */
    static final class Censo {
        int camasPartes, agua;
        final Map<Material, Integer> plantas = new EnumMap<>(Material.class);
        final Map<Material, Integer> troncos = new EnumMap<>(Material.class);
        final Map<Profissao, Integer> postos = new EnumMap<>(Profissao.class);
        /** Blocos de posto ainda por conferir (x, y, z, material). */
        final List<Object[]> blocosPosto = new ArrayList<>();
    }

    /** Um morador da colônia (a entidade é um aldeão de verdade). */
    public static final class Cidadao {
        UUID entidade;
        String nome;
        Profissao profissao;
        int nivel;
        double xp;
        /** Quantas vezes seguidas não foi achado (para saber se sumiu de vez). */
        int sumido;
        /** O jeito dele (1 ou 2 traços). */
        final List<Traco> tracos = new ArrayList<>();
        /** O que ele pediu (null = nada) e quantos turnos faltam para o prazo acabar. */
        Pedido pedido;
        int prazo;
        /** Humor por um tempo: +1 contente (pedido atendido), -1 chateado (esquecido), 0 normal. */
        int humor;
        int humorTurnos;

        Cidadao(UUID entidade, String nome, Profissao profissao, int nivel, double xp) {
            this.entidade = entidade;
            this.nome = nome;
            this.profissao = profissao;
            this.nivel = nivel;
            this.xp = xp;
        }

        boolean tem(Traco t) { return tracos.contains(t); }

        /** Multiplicador de produção pelo jeito e pelo humor de agora. */
        double fatorPessoal() {
            double f = 1;
            if (tem(Traco.DEDICACAO)) f *= 1.15;
            if (tem(Traco.PREGUICA)) f *= 0.85;
            if (humor > 0) f *= 1.15;
            else if (humor < 0) f *= 0.9;
            return f;
        }

        /** Quanto come por turno. */
        double fome() {
            return 0.25 * (tem(Traco.APETITE) ? 1.8 : tem(Traco.FRUGALIDADE) ? 0.6 : 1);
        }

        public UUID entidade() { return entidade; }
        public String nome() { return nome; }
        public Profissao profissao() { return profissao; }
        public int nivel() { return nivel; }
        public double xp() { return xp; }

        /** XP para passar do nível atual (níveis 1 a 10). */
        public static double xpParaProximo(int nivel) {
            return 20 + 15 * nivel;
        }
    }

    final UUID dono;
    String nomeDono;
    final String mundo;
    final int x, y, z;
    int nivel = 1;
    double felicidade = 60;
    /** Fome acumulada (cada ponto = 1 comida que precisa sair do depósito). */
    double fome;
    long proximaChegada;
    final List<Location> depositos = new ArrayList<>();
    final List<Cidadao> cidadaos = new ArrayList<>();
    /** A casa que o Construtor está erguendo (null = nenhuma) e quantas ele já terminou. */
    Obras.Obra obra;
    int casas;

    // ---------- achado na última varredura (não é salvo) ----------
    int camas;
    int agua;
    final Map<Material, Integer> plantas = new EnumMap<>(Material.class);
    final Map<Material, Integer> troncos = new EnumMap<>(Material.class);
    final Map<Profissao, Integer> postos = new EnumMap<>(Profissao.class);
    /** Chunk → contagem (fica guardada quando o chunk descarrega). */
    final Map<Long, Censo> censo = new HashMap<>();
    boolean varrida;
    boolean varrendo;
    long ultimaVarredura;
    /** Proporção da fome atendida no último turno (0 a 1) e se comeram pratos. */
    double alimentados = 1;
    boolean comeuPratos;
    String ultimoAviso;
    /** Ordem do exército da colônia e quem os soldados seguem. */
    public enum Ordem { GUARDAR, SEGUIR, ATACAR }
    Ordem ordem = Ordem.GUARDAR;
    UUID seguindo;
    Location alvoAtaque;
    long ultimoAvisoMs;

    Colonia(UUID dono, String nomeDono, String mundo, int x, int y, int z) {
        this.dono = dono;
        this.nomeDono = nomeDono;
        this.mundo = mundo;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID dono() { return dono; }
    public String nomeDono() { return nomeDono; }
    public int nivel() { return nivel; }
    public double felicidade() { return felicidade; }
    public List<Cidadao> cidadaos() { return cidadaos; }
    public List<Location> depositos() { return depositos; }
    public int camas() { return camas; }

    public World world() { return Bukkit.getWorld(mundo); }

    public Location prefeitura() {
        World w = world();
        return w == null ? null : new Location(w, x, y, z);
    }

    /** Quantos cidadãos cabem no nível atual. */
    public int maxCidadaos() {
        return nivel * 3;
    }

    /** Multiplicador de produção pela felicidade (0,5 a 1,2). */
    public double fatorFelicidade() {
        return 0.5 + 0.7 * Math.max(0, Math.min(100, felicidade)) / 100.0;
    }

    public int quantos(Profissao p) {
        int n = 0;
        for (Cidadao c : cidadaos) if (c.profissao == p) n++;
        return n;
    }

    public boolean dentro(Location l) {
        return l.getWorld() != null && l.getWorld().getName().equals(mundo)
                && Math.abs(l.getBlockX() - x) <= RAIO && Math.abs(l.getBlockZ() - z) <= RAIO && Math.abs(l.getBlockY() - y) <= 24;
    }
}
