package br.rpgatributos.masmorra;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uma masmorra em andamento: as salas, quem está nela e o estado. */
public final class Masmorra {

    public enum Estado { GERANDO, PRONTA, CONCLUIDA }

    final UUID id = UUID.randomUUID();
    final int vaga;
    final Dificuldade dificuldade;
    final Tema tema;
    final World mundo;
    final UUID dono;
    final List<Sala> salas = new ArrayList<>();
    Sala inicio, chefe;

    /** Quem está jogando (entrou e ainda não saiu nem morreu). */
    final Set<UUID> participantes = new HashSet<>();
    /** Quem já saiu ou morreu: não entra de novo. */
    final Set<UUID> sairam = new HashSet<>();

    Estado estado = Estado.GERANDO;
    final long criadaEm = System.currentTimeMillis();
    long limite;
    long ultimaPresenca = System.currentTimeMillis();
    long concluidaEm;
    /** Portal de saída (aparece no fim, na sala do chefe). */
    Location saida;
    int minX, minZ, maxX, maxZ;

    /** Aberta por um Portal do mundo: qualquer um entra, e ela espera gente entrar. */
    boolean publica;
    /** Itens a mais no baú do chefe. */
    int tesouroExtra;
    /** Chamado quando o chefe morre (o Portal do mundo se fecha). */
    java.util.function.Consumer<Masmorra> aoConcluir;
    /** Quem deu o golpe final no chefe (ou null). */
    UUID matadorChefe;

    // 2.68: Masmorra 2.0 (Cristal de Portal e portais com rank)
    /** O rank (E a S): força dos monstros, tesouro e XP de caçador. */
    Rank rank;
    /** O que a party precisa fazer (Caçada = o chefe, como sempre). */
    Objetivo objetivo = Objetivo.CACADA;
    final java.util.EnumSet<ModificadorPortal> mods = java.util.EnumSet.noneOf(ModificadorPortal.class);
    /** Aberta por um Cristal: no fim do tempo, quem está lá dentro perde o que pegou lá. */
    boolean cristal;
    /** Portal Vermelho: não dá para sair (nem por comando) antes de cumprir o objetivo. */
    boolean vermelho;
    /** O que cada um tinha ao entrar (para saber o que pegou lá dentro). */
    final java.util.Map<UUID, java.util.List<org.bukkit.inventory.ItemStack>> aoEntrar = new java.util.HashMap<>();
    /** Saque: os vasos ainda no lugar, quantos foram pegos e quantos precisa. */
    final Set<Location> reliquias = new HashSet<>();
    int reliquiasPegas, reliquiasPrecisa;
    /** Monólitos: os que faltam despertar, os que estão lutando (monólito -> monstros) e os já vencidos. */
    final Set<Location> monolitos = new HashSet<>();
    final java.util.Map<Location, Set<UUID>> monolitosLutando = new java.util.HashMap<>();
    int monolitosVencidos, monolitosTotal;

    // 2.69: geração nova
    /** As ligações entre salas (cada corredor), para construir. */
    final List<Gerador.Ligacao> ligacoes = new ArrayList<>();
    /** Blocos das paredes rachadas das salas secretas (um golpe derruba o grupo todo). */
    final java.util.Map<Location, Integer> segredos = new java.util.HashMap<>();
    /** Veios de Cristal de Mana nas Minas (quebrar dá cristais). */
    final Set<Location> minerios = new HashSet<>();

    // 2.70: dentro do vault
    /** Armadilhas (placas de pressão) e o tipo de cada uma; quando cada uma pode disparar de novo. */
    final java.util.Map<Location, Armadilha> armadilhas = new java.util.HashMap<>();
    final java.util.Map<Location, Long> armadilhaPronta = new java.util.HashMap<>();
    /** Baús que são mímicos (atacam ao abrir). */
    final Set<Location> mimicos = new HashSet<>();
    /** Pilares de bênção ainda não usados, e as bênçãos que a party já ganhou (com quem segue o deus). */
    final java.util.Map<Location, br.rpgatributos.fe.Deus> pilares = new java.util.HashMap<>();
    final java.util.EnumSet<br.rpgatributos.fe.Deus> bencaos = java.util.EnumSet.noneOf(br.rpgatributos.fe.Deus.class);
    /** Para a nota da corrida: quando começou, quanto tempo tinha, mortes e segredos achados. */
    long inicioMs, duracaoMs;
    int mortes, segredosAchados;

    /** Os tipos de armadilha. */
    enum Armadilha { ESPINHOS, FLECHAS, CHAMAS }

    /** 2.71: qual chefe saiu (para a arma de chefe no tesouro). */
    br.rpgatributos.aventura.Chefe chefeTipo;

    /** 2.72: o que as Inscrições do cristal pediram (salas de tesouro, minas e minutos a mais). */
    int salasTesouroExtra, minasExtra, minutosExtra;
    /** 2.72: quem já usou a Segunda Chance (característica) neste portal. */
    final Set<UUID> segundaChance = new HashSet<>();
    /** 2.74: Portal do Monarca (o chefe é esse Monarca), ou null. */
    Monarca monarca;
    /** 2.75: Masmorra Dupla (esconde a Câmara das Estátuas atrás do chefe) e a câmara, quando aberta. */
    boolean dupla;
    CamaraEstatuas camara;
    /** 2.75: Ataque de Rank S (o chefe cresce com quem está lá dentro). */
    boolean ataque;

    public Rank rank() { return rank != null ? rank : Rank.de(dificuldade); }
    public Objetivo objetivo() { return objetivo; }
    public boolean vermelho() { return vermelho; }

    Masmorra(int vaga, Dificuldade dificuldade, Tema tema, World mundo, UUID dono) {
        this.vaga = vaga;
        this.dificuldade = dificuldade;
        this.tema = tema;
        this.mundo = mundo;
        this.dono = dono;
    }

    /** Está na área dessa masmorra (todas as salas e corredores)? */
    boolean contem(Location l) {
        return l.getWorld() == mundo && l.getX() >= minX - 4 && l.getX() <= maxX + 4
                && l.getZ() >= minZ - 4 && l.getZ() <= maxZ + 4;
    }

    Sala salaEm(Location l) {
        for (Sala s : salas) if (s.dentro(l, -0.5)) return s;
        return null;
    }

    int salasLimpas() {
        int n = 0;
        for (Sala s : salas) if (s.estado == Sala.Estado.LIMPA && (s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.CHEFE)) n++;
        return n;
    }

    int salasDeLuta() {
        int n = 0;
        for (Sala s : salas) if (s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.CHEFE) n++;
        return n;
    }

    public Dificuldade dificuldade() { return dificuldade; }
    public Tema tema() { return tema; }
    public Estado estado() { return estado; }
    public UUID id() { return id; }
    public UUID dono() { return dono; }
    public UUID matadorChefe() { return matadorChefe; }
    /** Quem ainda está lá dentro. */
    public Set<UUID> participantes() { return java.util.Collections.unmodifiableSet(participantes); }
}
