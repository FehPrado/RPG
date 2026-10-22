package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.sign.Side;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BlockVector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * A geração das masmorras (2.69, no molde do Vault Hunters): um quadrado de chunks de tamanho fixo
 * pelo rank, com uma sala por célula de 2x2 chunks (às vezes 3x3, 3x2 ou 2x3), corredores entre
 * elas, atalhos que fecham voltas, andares ligados por Escadarias, Minas de Cristal e salas secretas.
 * A sala do portal (entrada) fica no meio. A construção é feita aos poucos (uma sala ou um corredor
 * por vez) para o servidor não travar.
 */
final class Gerador {

    /** Altura do piso do andar da entrada. */
    static final int Y = 64;
    /** Diferença de altura entre dois andares. */
    static final int NIVEL = 12;
    /** Espaço entre a parede da sala e a borda dos chunks dela (o corredor tem o dobro). */
    static final int MARGEM = 4;
    /** Salas de luta que sempre sobram (além do chefe) depois de tesouros e minas. */
    private static final int MIN_LUTA = 2;
    private static final BlockFace[] DIRECOES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    /** O interior de uma sala. */
    enum Estilo { SIMPLES, PILARES, ARENA, PONTE, BIBLIOTECA, ALAGADO, MEZANINO, CRIPTA, JARDIM }

    /** O jeito do corredor. */
    enum Corredor { RETO, PONTE, TUNEL }

    /** Um corredor: da sala a para a b (b fica no lado {@code f} de a), no andar dado. */
    record Ligacao(Sala a, Sala b, BlockFace f, int linha, int nivel, boolean secreta, Corredor estilo) { }

    private final RPGAtributos plugin;
    private final SalasPersonalizadas personalizadas;
    /** Tipo de monstro pedido numa placa [monstro] (por posição). */
    final Map<Location, EntityType> tiposMarcados = new HashMap<>();

    Gerador(RPGAtributos plugin, SalasPersonalizadas personalizadas) {
        this.plugin = plugin;
        this.personalizadas = personalizadas;
    }

    // =====================================================================
    //  Planta
    // =====================================================================

    private static final int[] LARGURA = {6, 8, 10, 10, 12, 14};
    private static final int[] SALAS = {7, 11, 16, 20, 25, 34};
    private static final int[] ANDARES = {1, 1, 2, 2, 2, 3};

    /** O que já está ocupado em cada andar (em chunks). */
    private static final class Planta {
        final int w;
        final boolean[][][] ocupado;
        final List<List<Sala>> andares = new ArrayList<>();
        final int ox, oz;

        Planta(int w, int andares, int ox, int oz) {
            this.w = w;
            this.ocupado = new boolean[andares][w][w];
            for (int i = 0; i < andares; i++) this.andares.add(new ArrayList<>());
            this.ox = ox;
            this.oz = oz;
        }

        boolean livre(int nivel, int x, int z, int cw, int ch) {
            if (x < 0 || z < 0 || x + cw > w || z + ch > w) return false;
            for (int i = x; i < x + cw; i++) for (int k = z; k < z + ch; k++) if (ocupado[nivel][i][k]) return false;
            return true;
        }

        void marcar(int nivel, Sala s) {
            for (int i = s.gx; i < s.gx + s.cw; i++) for (int k = s.gz; k < s.gz + s.ch; k++) ocupado[nivel][i][k] = true;
        }
    }

    void planejar(Masmorra m, Random r) {
        int o = m.rank().ordinal();
        int w = LARGURA[o], total = SALAS[o], andares = ANDARES[o];
        Planta pl = new Planta(w, andares, 100_000 + m.vaga * 2_048, 100_000);
        int[] meta = new int[andares];
        for (int k = 0; k < andares; k++) meta[k] = total / andares;
        meta[0] += total % andares;

        int c = w / 2 - 1;
        Sala inicio = nova(m, pl, c, c, 2, 2, 0, TipoSala.INICIO);
        m.inicio = inicio;

        for (int k = 0; k < andares; k++) {
            if (k > 0 && !subirEscadaria(m, pl, k, r)) break;
            crescer(m, pl, k, meta[k], r);
        }
        atalhos(m, pl, r);
        secretas(m, pl, r);
        alturas(m, r);
        profundidades(m);
        tipos(m, r);

        m.minX = Integer.MAX_VALUE;
        m.minZ = Integer.MAX_VALUE;
        m.maxX = Integer.MIN_VALUE;
        m.maxZ = Integer.MIN_VALUE;
        for (Sala s : m.salas) {
            m.minX = Math.min(m.minX, s.minX);
            m.minZ = Math.min(m.minZ, s.minZ);
            m.maxX = Math.max(m.maxX, s.maxX);
            m.maxZ = Math.max(m.maxZ, s.maxZ);
        }
    }

    /** Uma sala nova no andar, com os limites no mundo (a altura vem depois). */
    private static Sala nova(Masmorra m, Planta pl, int gx, int gz, int cw, int ch, int nivel, TipoSala tipo) {
        Sala s = new Sala(gx, gz, tipo, 0);
        s.cw = cw;
        s.ch = ch;
        s.nivel = nivel;
        s.minX = pl.ox + gx * 16 + MARGEM;
        s.maxX = pl.ox + (gx + cw) * 16 - 1 - MARGEM;
        s.minZ = pl.oz + gz * 16 + MARGEM;
        s.maxZ = pl.oz + (gz + ch) * 16 - 1 - MARGEM;
        s.centroX = (s.minX + s.maxX) / 2;
        s.centroZ = (s.minZ + s.maxZ) / 2;
        m.salas.add(s);
        pl.andares.get(nivel).add(s);
        pl.marcar(nivel, s);
        return s;
    }

    private static boolean temNoAndar(Sala s, int k) {
        return s.nivel == k || (s.tipo == TipoSala.ESCADA && s.nivel + 1 == k);
    }

    /** Uma sala do andar de baixo vira Escadaria e já começa o andar {@code k}. */
    private static boolean subirEscadaria(Masmorra m, Planta pl, int k, Random r) {
        List<Sala> l = new ArrayList<>();
        for (Sala s : pl.andares.get(k - 1)) {
            if (s.tipo != TipoSala.COMBATE || s.grande() || s.nivel != k - 1) continue;
            if (temPortaRuim(m, s)) continue; // a escada encosta na parede norte
            if (pl.livre(k, s.gx, s.gz, s.cw, s.ch)) l.add(s);
        }
        if (l.isEmpty()) return false;
        Collections.shuffle(l, r);
        Sala esc = l.getFirst();
        esc.tipo = TipoSala.ESCADA;
        pl.andares.get(k).add(esc);
        pl.marcar(k, esc);
        return true;
    }

    /** A sala já tem porta na parede norte, ou porta dos lados perto dela? (então não vira Escadaria) */
    private static boolean temPortaRuim(Masmorra m, Sala s) {
        for (Ligacao l : m.ligacoes) {
            BlockFace f = l.a() == s ? l.f() : l.b() == s ? l.f().getOppositeFace() : null;
            if (f == null) continue;
            if (f == BlockFace.NORTH) return true;
            if ((f == BlockFace.EAST || f == BlockFace.WEST) && (l.linha() < s.minZ + 6 || l.linha() > s.maxZ - 6)) return true;
        }
        return false;
    }

    /** Põe salas no andar até ter {@code alvo}, cada uma encostada (e ligada) numa que já existe. */
    private static void crescer(Masmorra m, Planta pl, int k, int alvo, Random r) {
        List<Sala> andar = pl.andares.get(k);
        for (int t = 0; t < 4000 && contar(andar, k) < alvo; t++) {
            Sala base = andar.get(r.nextInt(andar.size()));
            BlockFace f = DIRECOES[r.nextInt(DIRECOES.length)];
            int cw = 2, ch = 2;
            if (r.nextDouble() < 0.07) {
                int[][] grandes = {{3, 3}, {3, 2}, {2, 3}};
                int[] g = grandes[r.nextInt(grandes.length)];
                cw = g[0];
                ch = g[1];
            }
            int nx, nz;
            if (f == BlockFace.EAST || f == BlockFace.WEST) {
                nx = f == BlockFace.EAST ? base.gx + base.cw : base.gx - cw;
                nz = base.gz - (ch - 1) + r.nextInt(base.ch + ch - 1);
            } else {
                nz = f == BlockFace.SOUTH ? base.gz + base.ch : base.gz - ch;
                nx = base.gx - (cw - 1) + r.nextInt(base.cw + cw - 1);
            }
            if (!pl.livre(k, nx, nz, cw, ch)) continue;
            if (base.tipo == TipoSala.ESCADA && f == BlockFace.NORTH) continue;
            Sala nova = nova(m, pl, nx, nz, cw, ch, k, TipoSala.COMBATE);
            if (!ligar(m, base, nova, f, k, false, r)) {
                desfazer(m, pl, k, nova);
            }
        }
    }

    private static int contar(List<Sala> andar, int k) {
        int n = 0;
        for (Sala s : andar) if (s.nivel == k) n++;
        return n;
    }

    private static void desfazer(Masmorra m, Planta pl, int k, Sala s) {
        m.salas.remove(s);
        pl.andares.get(k).remove(s);
        for (int i = s.gx; i < s.gx + s.cw; i++) for (int z = s.gz; z < s.gz + s.ch; z++) pl.ocupado[k][i][z] = false;
    }

    /** Liga a sala a à b (que fica no lado {@code f} de a). Escolhe a linha da porta onde as duas se encostam. */
    private static boolean ligar(Masmorra m, Sala a, Sala b, BlockFace f, int k, boolean secreta, Random r) {
        boolean emX = f == BlockFace.EAST || f == BlockFace.WEST;
        int lo = emX ? Math.max(a.minZ, b.minZ) + 2 : Math.max(a.minX, b.minX) + 2;
        int hi = emX ? Math.min(a.maxZ, b.maxZ) - 2 : Math.min(a.maxX, b.maxX) - 2;
        // Escadaria: as portas dos lados ficam longe da escada (na parede norte) e da parede sul.
        for (Sala s : new Sala[]{a, b}) {
            if (s.tipo != TipoSala.ESCADA || !emX) continue;
            lo = Math.max(lo, s.minZ + 6);
            hi = Math.min(hi, s.maxZ - 6);
        }
        if ((a.tipo == TipoSala.ESCADA && f == BlockFace.NORTH) || (b.tipo == TipoSala.ESCADA && f == BlockFace.SOUTH)) return false;
        if (lo > hi) return false;
        int meio = (lo + hi) / 2, folga = (hi - lo) / 3;
        int linha = folga <= 0 ? meio : meio - folga + r.nextInt(2 * folga + 1);
        Corredor estilo = secreta ? Corredor.RETO : r.nextDouble() < 0.2 ? Corredor.PONTE : r.nextDouble() < 0.25 ? Corredor.TUNEL : Corredor.RETO;
        m.ligacoes.add(new Ligacao(a, b, f, linha, k, secreta, estilo));
        return true;
    }

    private static boolean ligadas(Masmorra m, Sala a, Sala b) {
        for (Ligacao l : m.ligacoes) if ((l.a() == a && l.b() == b) || (l.a() == b && l.b() == a)) return true;
        return false;
    }

    /** Salas vizinhas que ainda não se ligam: às vezes ganham um corredor (o mapa fica com voltas). */
    private static void atalhos(Masmorra m, Planta pl, Random r) {
        for (int k = 0; k < pl.andares.size(); k++) {
            List<Sala> andar = pl.andares.get(k);
            for (Sala a : andar) {
                for (Sala b : andar) {
                    if (a == b || ligadas(m, a, b) || r.nextDouble() > 0.22) continue;
                    BlockFace f = null;
                    if (a.gx + a.cw == b.gx && sobrepoe(a.gz, a.ch, b.gz, b.ch)) f = BlockFace.EAST;
                    else if (a.gz + a.ch == b.gz && sobrepoe(a.gx, a.cw, b.gx, b.cw)) f = BlockFace.SOUTH;
                    if (f != null) ligar(m, a, b, f, k, false, r);
                }
            }
        }
    }

    private static boolean sobrepoe(int a0, int al, int b0, int bl) {
        return Math.min(a0 + al, b0 + bl) - Math.max(a0, b0) >= 1;
    }

    /** Salas secretas (do rank C para cima): encostadas numa sala comum, atrás de uma parede rachada. */
    private static void secretas(Masmorra m, Planta pl, Random r) {
        int quantas = m.rank().ordinal() >= Rank.A.ordinal() ? 2 : m.rank().ordinal() >= Rank.C.ordinal() ? 1 : 0;
        for (int t = 0; t < 400 && quantas > 0; t++) {
            int k = r.nextInt(pl.andares.size());
            List<Sala> andar = pl.andares.get(k);
            Sala base = andar.get(r.nextInt(andar.size()));
            if (base.tipo != TipoSala.COMBATE || base.nivel != k) continue;
            BlockFace f = DIRECOES[r.nextInt(DIRECOES.length)];
            int nx = f == BlockFace.EAST ? base.gx + base.cw : f == BlockFace.WEST ? base.gx - 2 : base.gx;
            int nz = f == BlockFace.SOUTH ? base.gz + base.ch : f == BlockFace.NORTH ? base.gz - 2 : base.gz;
            if (!pl.livre(k, nx, nz, 2, 2)) continue;
            Sala s = nova(m, pl, nx, nz, 2, 2, k, TipoSala.SECRETA);
            if (ligar(m, base, s, f, k, true, r)) quantas--;
            else desfazer(m, pl, k, s);
        }
    }

    /** A altura de cada sala: o andar, e às vezes 2 blocos acima ou abaixo (o corredor ganha degraus). */
    private static void alturas(Masmorra m, Random r) {
        for (Sala s : m.salas) {
            int off = s.tipo == TipoSala.INICIO || s.tipo == TipoSala.ESCADA ? 0 : new int[]{0, 0, 0, 2, -2}[r.nextInt(5)];
            s.minY = Y + s.nivel * NIVEL + off;
            int altura = s.tipo == TipoSala.ESCADA ? NIVEL + 10 : s.grande() ? 13 : 10;
            s.maxY = s.minY + altura - 1;
        }
        // As portas (cada ligação abre uma em cada sala, na altura do piso daquele andar).
        for (Ligacao l : m.ligacoes) {
            if (!l.secreta()) l.a().portas.add(new Sala.Porta(l.f(), l.linha(), piso(l.a(), l.nivel())));
            l.b().portas.add(new Sala.Porta(l.f().getOppositeFace(), l.linha(), piso(l.b(), l.nivel())));
        }
    }

    /** O piso da sala no andar {@code k} (a Escadaria tem dois). */
    static int piso(Sala s, int k) {
        return s.tipo == TipoSala.ESCADA && k == s.nivel + 1 ? s.minY + NIVEL : s.minY;
    }

    /** Distância (em salas) de cada uma até a entrada. */
    private static void profundidades(Masmorra m) {
        for (Sala s : m.salas) s.profundidade = Integer.MAX_VALUE;
        Deque<Sala> fila = new ArrayDeque<>();
        m.inicio.profundidade = 0;
        fila.add(m.inicio);
        while (!fila.isEmpty()) {
            Sala s = fila.poll();
            for (Ligacao l : m.ligacoes) {
                Sala o = l.a() == s ? l.b() : l.b() == s ? l.a() : null;
                if (o == null || o.profundidade <= s.profundidade + 1) continue;
                o.profundidade = s.profundidade + 1;
                fila.add(o);
            }
        }
        for (Sala s : m.salas) if (s.profundidade == Integer.MAX_VALUE) s.profundidade = 0;
    }

    private static int ligacoesDe(Masmorra m, Sala s) {
        int n = 0;
        for (Ligacao l : m.ligacoes) if (l.a() == s || l.b() == s) n++;
        return n;
    }

    /** Chefe (a mais longe; grande vale mais), tesouros (pontas), Minas e os estilos de cada sala. */
    private static void tipos(Masmorra m, Random r) {
        Sala chefe = null;
        for (Sala s : m.salas) {
            if (s.tipo != TipoSala.COMBATE) continue;
            if (chefe == null || s.profundidade + (s.grande() ? 2 : 0) > chefe.profundidade + (chefe.grande() ? 2 : 0)) chefe = s;
        }
        if (chefe == null) chefe = m.salas.getLast();
        chefe.tipo = TipoSala.CHEFE;
        m.chefe = chefe;

        int o = m.rank().ordinal();
        List<Sala> pontas = new ArrayList<>(), resto = new ArrayList<>();
        for (Sala s : m.salas) {
            if (s.tipo != TipoSala.COMBATE) continue;
            if (ligacoesDe(m, s) == 1) pontas.add(s);
            else resto.add(s);
        }
        Collections.shuffle(pontas, r);
        Collections.shuffle(resto, r);
        // Tesouro de preferência nas pontas (fim de caminho); faltando ponta, numa sala qualquer.
        // 2.72: + as Inscrições de Tesouro e de Mina. Sempre sobram 2 salas de luta.
        int livres = pontas.size() + resto.size();
        int tesouros = (o >= Rank.A.ordinal() ? 3 : 2) + m.salasTesouroExtra;
        for (int i = 0; i < tesouros && livres > MIN_LUTA; i++, livres--) {
            (!pontas.isEmpty() ? pontas : resto).removeFirst().tipo = TipoSala.TESOURO;
        }
        resto.addAll(pontas);
        int minas = (o >= Rank.B.ordinal() ? 2 : o >= Rank.D.ordinal() ? 1 : 0) + m.minasExtra;
        for (int i = 0; i < minas && livres > MIN_LUTA; i++, livres--) resto.removeFirst().tipo = TipoSala.MINA;

        Estilo[] combate = {Estilo.PILARES, Estilo.ARENA, Estilo.PONTE, Estilo.BIBLIOTECA, Estilo.ALAGADO, Estilo.MEZANINO,
                Estilo.CRIPTA, Estilo.JARDIM, Estilo.SIMPLES};
        for (Sala s : m.salas) {
            s.estilo = switch (s.tipo) {
                case COMBATE -> combate[r.nextInt(combate.length)];
                case CHEFE -> r.nextBoolean() ? Estilo.ARENA : Estilo.PILARES;
                case TESOURO -> new Estilo[]{Estilo.BIBLIOTECA, Estilo.JARDIM, Estilo.SIMPLES}[r.nextInt(3)];
                default -> Estilo.SIMPLES;
            };
        }
        // 2.70: Pilares de Bênção (em salas de chão firme: nada de ponte nem alagado).
        List<Sala> firmes = new ArrayList<>();
        for (Sala s : m.salas) if (s.tipo == TipoSala.COMBATE && s.estilo != Estilo.PONTE && s.estilo != Estilo.ALAGADO) firmes.add(s);
        Collections.shuffle(firmes, r);
        int pilares = o >= Rank.A.ordinal() ? 3 : o >= Rank.C.ordinal() ? 2 : 1;
        for (int i = 0; i < pilares && i < firmes.size(); i++) firmes.get(i).pilar = true;
    }

    // =====================================================================
    //  Obras (cada tarefa é um pedaço pequeno)
    // =====================================================================

    Deque<Runnable> obras(Masmorra m, Random r) {
        Deque<Runnable> fila = new ArrayDeque<>();
        double chancePersonalizada = plugin.settings().masChanceSalaPersonalizada;
        for (Sala s : m.salas) fila.add(() -> construirSala(m, s, r, chancePersonalizada));
        for (Ligacao l : m.ligacoes) fila.add(() -> corredor(m, l, r));
        // Por último, os baús (as salas personalizadas já foram lidas).
        fila.add(() -> encherBaus(m, r));
        return fila;
    }

    private static void por(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    // ---------- salas ----------

    private void construirSala(Masmorra m, Sala s, Random r, double chancePersonalizada) {
        World w = m.mundo;
        Tema t = m.tema;
        casca(w, s, t);
        SalasPersonalizadas.Modelo modelo = null;
        if (s.tipo == TipoSala.INICIO || s.tipo == TipoSala.COMBATE || s.tipo == TipoSala.TESOURO || s.tipo == TipoSala.CHEFE) {
            if (r.nextDouble() < chancePersonalizada) modelo = personalizadas.sortear(s.tipo, r);
        }
        if (modelo != null && cabe(s, modelo)) {
            colocarPersonalizada(m, s, modelo, r);
        } else {
            interior(m, w, s, t, r);
        }
        if (s.tipo == TipoSala.ESCADA) escadaria(w, s, t);
        abrirPortas(w, s);
        caminhos(w, s, t);
        if (s.pilar) pilar(m, w, s, t, r);
        if (s.tipo == TipoSala.COMBATE && r.nextDouble() < 0.25) armadilhaNaSala(m, w, s, r);
        if (s.tipo == TipoSala.INICIO && s.entrada == null) s.entrada = s.centro(w);
        if (s.tipo == TipoSala.CHEFE && s.pontoChefe == null) s.pontoChefe = new Location(w, s.centroX, s.minY, s.centroZ);
    }

    /** Piso, paredes e teto; luz no teto em grade. */
    private static void casca(World w, Sala s, Tema t) {
        for (int x = s.minX; x <= s.maxX; x++) {
            for (int z = s.minZ; z <= s.maxZ; z++) {
                por(w, x, s.minY, z, t.piso());
                por(w, x, s.maxY, z, t.parede());
                boolean borda = x == s.minX || x == s.maxX || z == s.minZ || z == s.maxZ;
                if (borda) for (int y = s.minY + 1; y < s.maxY; y++) por(w, x, y, z, t.parede());
                else for (int y = s.minY + 1; y < s.maxY; y++) por(w, x, y, z, Material.AIR);
            }
        }
        for (int x = s.minX + 3; x <= s.maxX - 3; x += 5) {
            for (int z = s.minZ + 3; z <= s.maxZ - 3; z += 5) por(w, x, s.maxY, z, t.luz());
        }
    }

    private void interior(Masmorra m, World w, Sala s, Tema t, Random r) {
        int cx = s.centroX, cz = s.centroZ, y = s.minY;
        switch (s.tipo) {
            case INICIO -> {
                for (int i = -3; i <= 3; i++) {
                    por(w, cx + i, y, cz, t.destaque());
                    por(w, cx, y, cz + i, t.destaque());
                }
                for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) por(w, cx + c[0], y + 1, cz + c[1], t.lanterna());
                s.entrada = new Location(w, cx + 0.5, y + 1, cz + 0.5);
                return;
            }
            case TESOURO -> {
                estilo(m, w, s, t, r);
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) por(w, cx + dx, y, cz + dz, t.destaque());
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) por(w, cx + dx, y + 1, cz + dz, t.destaque());
                s.pontosBau.add(new Location(w, cx, y + 2, cz));
                s.pontosBau.add(new Location(w, cx - 4, y + 1, cz));
                s.pontosBau.add(new Location(w, cx + 4, y + 1, cz));
                return;
            }
            case MINA -> {
                mina(m, w, s, t, r);
                return;
            }
            case SECRETA -> {
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) por(w, cx + dx, y, cz + dz, t.destaque());
                for (int[] c : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) por(w, cx + c[0], y + 1, cz + c[1], t.lanterna());
                s.pontosBau.add(new Location(w, cx, y + 1, cz));
                return;
            }
            case ESCADA -> {
                for (int[] c : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) por(w, cx + c[0], y + 1, cz + c[1], t.lanterna());
                return;
            }
            default -> estilo(m, w, s, t, r);
        }
        if (s.tipo == TipoSala.COMBATE && r.nextDouble() < 0.25) {
            Block b = w.getBlockAt(s.minX + 2, y + 1, s.minZ + 2);
            if (b.isEmpty() && w.getBlockAt(s.minX + 2, y, s.minZ + 2).getType().isSolid()) s.pontosBau.add(b.getLocation());
        }
        decorar(w, s, t, r);
    }

    private static void pilar(World w, Sala s, int x, int z, Tema t) {
        for (int y = s.minY + 1; y < s.maxY; y++) por(w, x, y, z, t.pilar());
    }

    /** O interior pelo estilo da sala. */
    private static void estilo(Masmorra m, World w, Sala s, Tema t, Random r) {
        int x0 = s.minX + 1, x1 = s.maxX - 1, z0 = s.minZ + 1, z1 = s.maxZ - 1, y = s.minY;
        int cx = s.centroX, cz = s.centroZ;
        switch (s.estilo) {
            case PILARES -> {
                for (int x = x0 + 4; x <= x1 - 4; x += 6) for (int z = z0 + 4; z <= z1 - 4; z += 6) {
                    pilar(w, s, x, z, t);
                    por(w, x + 1, y + 1, z, t.lanterna());
                }
            }
            case ARENA -> {
                int raio = Math.min(x1 - x0, z1 - z0) / 2 - 2;
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d <= raio && d >= raio - 1.5) por(w, x, y, z, t.destaque());
                }
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    int x = cx + (int) Math.round(Math.cos(a) * (raio + 1)), z = cz + (int) Math.round(Math.sin(a) * (raio + 1));
                    if (x > x0 && x < x1 && z > z0 && z < z1) pilar(w, s, x, z, t);
                }
            }
            case PONTE -> {
                // Abismo com líquido no fundo; os caminhos (feitos depois) viram as pontes.
                for (int x = s.minX; x <= s.maxX; x++) for (int z = s.minZ; z <= s.maxZ; z++) {
                    boolean borda = x == s.minX || x == s.maxX || z == s.minZ || z == s.maxZ;
                    for (int dy = -5; dy <= 0; dy++) {
                        Material mat = borda || dy == -5 ? t.parede() : dy == -4 ? t.liquido() : Material.AIR;
                        por(w, x, y + dy, z, mat);
                    }
                }
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) por(w, cx + dx, y, cz + dz, t.destaque());
            }
            case BIBLIOTECA -> {
                for (int z = z0 + 2; z <= z1 - 2; z += 4) {
                    for (int x = x0 + 2; x <= x1 - 2; x++) {
                        if ((x - x0) % 8 >= 6) continue; // vãos entre as estantes
                        for (int dy = 1; dy <= 3; dy++) por(w, x, y + dy, z, Material.BOOKSHELF);
                    }
                }
                for (int[] c : new int[][]{{x0 + 1, z0 + 1}, {x1 - 1, z0 + 1}, {x0 + 1, z1 - 1}, {x1 - 1, z1 - 1}}) por(w, c[0], y + 1, c[1], t.lanterna());
            }
            case ALAGADO -> {
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                    por(w, x, y - 1, z, t.piso());
                    por(w, x, y, z, Material.WATER);
                    if (r.nextDouble() < 0.05) por(w, x, y + 1, z, Material.LILY_PAD);
                }
            }
            case MEZANINO -> {
                // Varanda de 3 de largura em volta, 5 blocos acima, com grade e uma escada para subir.
                int ym = y + 5;
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                    boolean anel = x - x0 < 3 || x1 - x < 3 || z - z0 < 3 || z1 - z < 3;
                    if (!anel) continue;
                    por(w, x, ym, z, t.piso());
                    boolean beira = x - x0 == 2 || x1 - x == 2 || z - z0 == 2 || z1 - z == 2;
                    if (beira && !(x - x0 < 2 || x1 - x < 2 || z - z0 < 2 || z1 - z < 2)) por(w, x, ym + 1, z, t.cerca());
                }
                int zs = z0 + 3;
                for (int k = 0; k < 5; k++) {
                    int x = x0 + 3 + k;
                    for (int dy = 1; dy < 1 + k; dy++) por(w, x, y + dy, zs, t.parede());
                    Block b = w.getBlockAt(x, y + 1 + k, zs);
                    b.setType(t.escada(), false);
                    if (b.getBlockData() instanceof Stairs st) {
                        st.setFacing(BlockFace.EAST);
                        b.setBlockData(st, false);
                    }
                }
                por(w, x0 + 3 + 5, y + 5, zs, t.piso());
            }
            case CRIPTA -> {
                for (int x = x0 + 3; x <= x1 - 4; x += 4) for (int z = z0 + 3; z <= z1 - 3; z += 5) {
                    if (Math.abs(x - cx) <= 2 || Math.abs(z - cz) <= 2) continue;
                    por(w, x, y + 1, z, t.destaque());
                    por(w, x + 1, y + 1, z, t.destaque());
                    if (r.nextBoolean()) por(w, x, y + 2, z, Material.CANDLE);
                }
            }
            case JARDIM -> {
                for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
                    por(w, x, y, z, r.nextDouble() < 0.7 ? Material.MOSS_BLOCK : Material.GRASS_BLOCK);
                    double d = r.nextDouble();
                    if (d < 0.18) por(w, x, y + 1, z, Material.SHORT_GRASS);
                    else if (d < 0.22) por(w, x, y + 1, z, r.nextBoolean() ? Material.AZURE_BLUET : Material.BLUE_ORCHID);
                }
                for (int i = 0; i < 3 + s.cw; i++) {
                    int x = x0 + 3 + r.nextInt(Math.max(1, x1 - x0 - 6)), z = z0 + 3 + r.nextInt(Math.max(1, z1 - z0 - 6));
                    if (Math.abs(x - cx) <= 2 || Math.abs(z - cz) <= 2) continue;
                    for (int dy = 1; dy <= 3; dy++) por(w, x, y + dy, z, Material.OAK_LOG);
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 4; dy <= 5; dy++) {
                        if (w.getBlockAt(x + dx, y + dy, z + dz).isEmpty()) por(w, x + dx, y + dy, z + dz, Material.FLOWERING_AZALEA_LEAVES);
                    }
                }
            }
            case SIMPLES -> { }
        }
    }

    /** Mina de Cristal: veios de ametista nas paredes (quebrar dá Cristais de Mana) e um baú. */
    private static void mina(Masmorra m, World w, Sala s, Tema t, Random r) {
        int x0 = s.minX + 1, x1 = s.maxX - 1, z0 = s.minZ + 1, z1 = s.maxZ - 1, y = s.minY;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean beira = x == x0 || x == x1 || z == z0 || z == z1;
            if (!beira) {
                if (r.nextDouble() < 0.05) por(w, x, y, z, Material.AMETHYST_BLOCK);
                continue;
            }
            for (int dy = 1; dy <= 6; dy++) {
                if (r.nextDouble() > 0.10) continue;
                Block b = w.getBlockAt(x, y + dy, z);
                b.setType(Material.AMETHYST_CLUSTER, false);
                m.minerios.add(b.getLocation());
            }
        }
        for (int[] c : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) por(w, s.centroX + c[0], y + 1, s.centroZ + c[1], t.lanterna());
        s.pontosBau.add(new Location(w, s.centroX, y + 1, s.centroZ));
    }

    /**
     * Escadaria: a escada sobe encostada na parede norte (do lado oeste para o leste) até o piso de
     * cima, que cobre a sala inteira menos o vão da escada.
     */
    private static void escadaria(World w, Sala s, Tema t) {
        int y = s.minY, yc = s.minY + NIVEL, x0 = s.minX + 1, x1 = s.maxX - 1, z0 = s.minZ + 1, z1 = s.maxZ - 1;
        int xs = x0 + 2;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean vao = z <= z0 + 2 && x >= xs - 1 && x <= xs + NIVEL - 2;
            por(w, x, yc, z, vao ? Material.AIR : t.piso());
        }
        for (int x = xs - 1; x <= xs + NIVEL - 2; x++) por(w, x, yc + 1, z0 + 3, t.cerca());
        for (int k = 0; k < NIVEL; k++) {
            int x = xs + k;
            for (int z = z0; z <= z0 + 2; z++) {
                for (int dy = 1; dy < 1 + k; dy++) por(w, x, y + dy, z, t.parede());
                Block b = w.getBlockAt(x, y + 1 + k, z);
                b.setType(t.escada(), false);
                if (b.getBlockData() instanceof Stairs st) {
                    st.setFacing(BlockFace.EAST);
                    b.setBlockData(st, false);
                }
            }
        }
        // Luz no andar de cima.
        for (int x = x0 + 3; x <= x1 - 3; x += 6) for (int z = z0 + 6; z <= z1 - 2; z += 6) por(w, x, yc + 1, z, t.lanterna());
    }

    /** 2.70: o Pilar de Bênção de um deus, num canto da sala, com o nome flutuando. */
    private static void pilar(Masmorra m, World w, Sala s, Tema t, Random r) {
        int x = s.maxX - 3, z = s.maxZ - 3, y = s.minY;
        if (!w.getBlockAt(x, y, z).getType().isSolid()) return;
        por(w, x, y + 1, z, t.pilar());
        por(w, x, y + 2, z, t.pilar());
        por(w, x, y + 3, z, Material.CHISELED_QUARTZ_BLOCK);
        br.rpgatributos.fe.Deus[] ds = br.rpgatributos.fe.Deus.values();
        br.rpgatributos.fe.Deus d = ds[r.nextInt(ds.length)];
        Location topo = new Location(w, x, y + 3, z);
        m.pilares.put(topo, d);
        w.spawn(topo.clone().add(0.5, 1.6, 0.5), org.bukkit.entity.TextDisplay.class, td -> {
            td.setPersistent(false);
            td.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            td.text(net.kyori.adventure.text.Component.text(d.simbolo() + " Pilar de " + d.nome(), d.cor())
                    .append(net.kyori.adventure.text.Component.newline())
                    .append(net.kyori.adventure.text.Component.text("clique para a bênção", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        });
    }

    /** 2.70: uma placa-armadilha no caminho de uma porta (3 blocos para dentro). */
    private static void armadilhaNaSala(Masmorra m, World w, Sala s, Random r) {
        for (Sala.Porta p : s.portas) {
            if (p.y() != s.minY) continue;
            int x = switch (p.face()) { case EAST -> s.maxX - 3; case WEST -> s.minX + 3; default -> p.linha(); };
            int z = switch (p.face()) { case SOUTH -> s.maxZ - 3; case NORTH -> s.minZ + 3; default -> p.linha(); };
            Block chao = w.getBlockAt(x, s.minY, z), b = w.getBlockAt(x, s.minY + 1, z);
            if (!chao.getType().isSolid() || !b.isEmpty()) continue;
            colocarArmadilha(m, b, r);
            return;
        }
    }

    private static void colocarArmadilha(Masmorra m, Block b, Random r) {
        b.setType(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, false);
        Masmorra.Armadilha[] ts = Masmorra.Armadilha.values();
        m.armadilhas.put(b.getLocation(), ts[r.nextInt(ts.length)]);
    }

    /** Abre as portas (3 de largura, 4 de altura) nas paredes. */
    private static void abrirPortas(World w, Sala s) {
        for (Sala.Porta p : s.portas) {
            for (int lado = -1; lado <= 1; lado++) {
                for (int dy = 1; dy <= 4; dy++) {
                    int[] xz = naParede(s, p, lado);
                    por(w, xz[0], p.y() + dy, xz[1], Material.AIR);
                }
            }
        }
    }

    /** O bloco da parede da porta (lado = -1, 0, 1 ao longo dela). */
    static int[] naParede(Sala s, Sala.Porta p, int lado) {
        return switch (p.face()) {
            case EAST -> new int[]{s.maxX, p.linha() + lado};
            case WEST -> new int[]{s.minX, p.linha() + lado};
            case SOUTH -> new int[]{p.linha() + lado, s.maxZ};
            default -> new int[]{p.linha() + lado, s.minZ};
        };
    }

    /**
     * Um caminho livre de cada porta (do andar de baixo) até o meio da sala: chão firme e 3 blocos de
     * ar. Nas pontes é o que vira a ponte; no alagado, o caminho seco.
     */
    private static void caminhos(World w, Sala s, Tema t) {
        for (Sala.Porta p : s.portas) {
            if (p.y() != s.minY) continue;
            int px, pz;
            switch (p.face()) {
                case EAST -> { px = s.maxX - 1; pz = p.linha(); }
                case WEST -> { px = s.minX + 1; pz = p.linha(); }
                case SOUTH -> { px = p.linha(); pz = s.maxZ - 1; }
                default -> { px = p.linha(); pz = s.minZ + 1; }
            }
            boolean emX = p.face() == BlockFace.EAST || p.face() == BlockFace.WEST;
            // Primeiro até a linha do centro, depois ao longo dela até o centro.
            while (true) {
                abrirCaminho(w, s, t, px, pz);
                if (emX && px != s.centroX) px += Integer.signum(s.centroX - px);
                else if (!emX && pz != s.centroZ) pz += Integer.signum(s.centroZ - pz);
                else break;
            }
            while (px != s.centroX || pz != s.centroZ) {
                abrirCaminho(w, s, t, px, pz);
                if (emX) pz += Integer.signum(s.centroZ - pz);
                else px += Integer.signum(s.centroX - px);
            }
            abrirCaminho(w, s, t, px, pz);
        }
    }

    private static void abrirCaminho(World w, Sala s, Tema t, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int bx = x + dx, bz = z + dz;
            if (bx <= s.minX || bx >= s.maxX || bz <= s.minZ || bz >= s.maxZ) continue;
            Block chao = w.getBlockAt(bx, s.minY, bz);
            if (!chao.getType().isSolid() || chao.isLiquid()) chao.setType(t.piso(), false);
            for (int dy = 1; dy <= 3; dy++) {
                Block b = w.getBlockAt(bx, s.minY + dy, bz);
                if (b.getType() != Material.AIR && b.getType() != Material.CHEST && !(b.getBlockData() instanceof Stairs)) b.setType(Material.AIR, false);
            }
        }
    }

    /** Enfeites no chão perto das paredes e pendurados no teto. */
    private static void decorar(World w, Sala s, Tema t, Random r) {
        for (int x = s.minX + 1; x < s.maxX; x++) {
            for (int z = s.minZ + 1; z < s.maxZ; z++) {
                boolean beira = x == s.minX + 1 || x == s.maxX - 1 || z == s.minZ + 1 || z == s.maxZ - 1;
                if (!beira || r.nextDouble() > 0.06) continue;
                Block b = w.getBlockAt(x, s.minY + 1, z);
                if (b.isEmpty() && w.getBlockAt(x, s.minY, z).getType().isSolid()) b.setType(t.decorChao(), false);
            }
        }
        for (int i = 0; i < 4 + s.cw * s.ch; i++) {
            int x = s.minX + 2 + r.nextInt(s.maxX - s.minX - 3), z = s.minZ + 2 + r.nextInt(s.maxZ - s.minZ - 3);
            Block b = w.getBlockAt(x, s.maxY - 1, z);
            if (b.isEmpty()) b.setType(t.decorTeto(), false);
        }
    }

    // ---------- salas personalizadas (montadas no meio da sala) ----------

    private static boolean cabe(Sala s, SalasPersonalizadas.Modelo modelo) {
        BlockVector t = modelo.estrutura().getSize();
        return t.getBlockX() <= s.maxX - s.minX - 1 && t.getBlockZ() <= s.maxZ - s.minZ - 1 && t.getBlockY() <= s.maxY - s.minY + 1;
    }

    private void colocarPersonalizada(Masmorra m, Sala s, SalasPersonalizadas.Modelo modelo, Random r) {
        World w = m.mundo;
        BlockVector t = modelo.estrutura().getSize();
        int bx = s.centroX - (t.getBlockX() - 1) / 2, bz = s.centroZ - (t.getBlockZ() - 1) / 2;
        modelo.estrutura().place(new Location(w, bx, s.minY, bz), true, StructureRotation.NONE, Mirror.NONE, 0, 1f, r);
        s.personalizada = modelo.nome();
        // Placas viram marcas: [monstro] (tipo na 2ª linha), [bau], [chefe], [entrada].
        for (int x = bx; x < bx + t.getBlockX(); x++) {
            for (int y = s.minY; y < s.minY + t.getBlockY(); y++) {
                for (int z = bz; z < bz + t.getBlockZ(); z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (!Tag.ALL_SIGNS.isTagged(b.getType()) || !(b.getState() instanceof Sign placa)) continue;
                    String l0 = texto(placa, 0), l1 = texto(placa, 1);
                    Location l = new Location(w, x, y, z);
                    boolean marca = true;
                    switch (l0) {
                        case "[monstro]" -> {
                            s.pontosMonstro.add(l);
                            EntityType tipo = tipo(l1);
                            if (tipo != null) tiposMarcados.put(l, tipo);
                        }
                        case "[bau]", "[baú]" -> s.pontosBau.add(l);
                        case "[chefe]" -> s.pontoChefe = new Location(w, x, y - 1, z);
                        case "[entrada]" -> s.entrada = new Location(w, x + 0.5, y, z + 0.5);
                        default -> marca = false;
                    }
                    if (marca) b.setType(Material.AIR, false);
                }
            }
        }
    }

    private static String texto(Sign placa, int linha) {
        return PlainTextComponentSerializer.plainText().serialize(placa.getSide(Side.FRONT).line(linha)).trim().toLowerCase(Locale.ROOT);
    }

    private static EntityType tipo(String nome) {
        if (nome.isEmpty()) return null;
        try {
            EntityType t = EntityType.valueOf(nome.toUpperCase(Locale.ROOT).replace(' ', '_'));
            return t.isAlive() && t.isSpawnable() ? t : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------- corredores ----------

    /**
     * O corredor entre duas salas: 8 blocos, 3 de largura e 4 de altura. Se as salas estão em
     * alturas diferentes, ele ganha degraus no meio. Pode ser ponte sobre abismo ou túnel. Na sala
     * secreta, a porta do lado da sala comum vira uma parede rachada.
     */
    private static void corredor(Masmorra m, Ligacao l, Random r) {
        World w = m.mundo;
        Tema t = m.tema;
        // Sempre de quem está a oeste/norte para quem está a leste/sul.
        boolean inverte = l.f() == BlockFace.WEST || l.f() == BlockFace.NORTH;
        Sala a = inverte ? l.b() : l.a(), b = inverte ? l.a() : l.b();
        BlockFace f = inverte ? l.f().getOppositeFace() : l.f();
        boolean emX = f == BlockFace.EAST;
        int ya = piso(a, l.nivel()), yb = piso(b, l.nivel());
        int inicio = emX ? a.maxX + 1 : a.maxZ + 1, fim = emX ? b.minX - 1 : b.minZ - 1;
        int comp = fim - inicio + 1, d = yb - ya, sinal = Integer.signum(d);
        int linha = l.linha();
        Corredor estilo = d != 0 && l.estilo() == Corredor.PONTE ? Corredor.RETO : l.estilo();
        int largura = estilo == Corredor.PONTE ? 3 : 2;
        int[] fy = new int[comp];
        for (int i = 0; i < comp; i++) fy[i] = ya + sinal * Math.min(Math.abs(d), Math.max(0, i - 2));
        fy[comp - 1] = yb;
        for (int i = 0; i < comp; i++) {
            int p = inicio + i, y = fy[i];
            for (int lado = -largura; lado <= largura; lado++) {
                int x = emX ? p : linha + lado, z = emX ? linha + lado : p;
                boolean parede = Math.abs(lado) == largura;
                int baixo = estilo == Corredor.PONTE ? y - 5 : y;
                for (int yy = baixo; yy <= y + 5; yy++) {
                    Material mat;
                    if (parede || yy == y + 5) mat = yy == y + 5 && !parede && (i % 4 == 1) && lado == 0 ? t.luz() : t.parede();
                    else if (yy > y) mat = Material.AIR;
                    else if (estilo == Corredor.PONTE && Math.abs(lado) == 2) mat = yy == y - 5 ? t.parede() : yy == y - 4 ? t.liquido() : Material.AIR;
                    else mat = yy == y ? t.piso() : t.parede();
                    por(w, x, yy, z, mat);
                }
                if (estilo == Corredor.PONTE && Math.abs(lado) == 2) por(w, x, y + 1, z, t.cerca());
                if (estilo == Corredor.TUNEL && Math.abs(lado) == 1 && r.nextDouble() < 0.35) por(w, x, y + 4, z, t.parede());
                if (estilo == Corredor.TUNEL && lado == 0 && r.nextDouble() < 0.08) por(w, x, y + 4, z, Material.COBWEB);
            }
            // Degraus: subindo de a para b, a escada fica no bloco mais alto; descendo, no anterior.
            if (i > 0 && fy[i] != fy[i - 1]) {
                boolean sobe = fy[i] > fy[i - 1];
                int pi = sobe ? p : p - 1, yy = sobe ? fy[i] : fy[i - 1];
                BlockFace olha = sobe ? f : f.getOppositeFace();
                for (int lado = -1; lado <= 1; lado++) {
                    Block bl = emX ? w.getBlockAt(pi, yy, linha + lado) : w.getBlockAt(linha + lado, yy, pi);
                    bl.setType(t.escada(), false);
                    if (bl.getBlockData() instanceof Stairs st) {
                        st.setFacing(olha);
                        bl.setBlockData(st, false);
                    }
                }
            }
        }
        // 2.70: às vezes uma placa-armadilha no meio do corredor plano.
        if (!l.secreta() && d == 0 && estilo != Corredor.PONTE && r.nextDouble() < 0.30) {
            int p = inicio + comp / 2;
            Block placa = emX ? w.getBlockAt(p, ya + 1, linha) : w.getBlockAt(linha, ya + 1, p);
            if (placa.isEmpty()) colocarArmadilha(m, placa, r);
        }
        // As portas nas duas paredes (na secreta, o lado da sala comum fica rachado).
        Sala comum = l.secreta() ? l.a() : null;
        int grupo = l.secreta() ? m.segredos.size() + 1 : 0;
        for (Sala s : new Sala[]{a, b}) {
            int ys = s == a ? ya : yb;
            BlockFace face = s == a ? f : f.getOppositeFace();
            for (int lado = -1; lado <= 1; lado++) {
                for (int dy = 1; dy <= 4; dy++) {
                    int[] xz = naParede(s, new Sala.Porta(face, linha, ys), lado);
                    Block bl = w.getBlockAt(xz[0], ys + dy, xz[1]);
                    if (s == comum) {
                        bl.setType(t.rachado(), false);
                        m.segredos.put(bl.getLocation(), grupo);
                    } else {
                        bl.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    // ---------- baús ----------

    /**
     * 2.68: os baús têm raridade (madeira, ferro, ouro e cristal): quanto mais raro, mais sorteios e
     * mais Cristais de Mana. O nome aparece ao abrir. O da sala secreta é sempre de cristal.
     */
    private void encherBaus(Masmorra m, Random r) {
        for (Sala s : m.salas) {
            for (Location l : s.pontosBau) {
                Block b = l.getBlock();
                b.setType(Material.CHEST, false);
                if (b.getState() instanceof Chest bau) {
                    double x = r.nextDouble();
                    int nivel = s.tipo == TipoSala.SECRETA ? 3 : x < 0.03 + m.rank().ordinal() * 0.01 ? 3 : x < 0.15 ? 2 : x < 0.40 ? 1 : 0;
                    String[] nomes = {"Baú de Madeira", "Baú de Ferro", "Baú de Ouro", "Baú de Cristal"};
                    net.kyori.adventure.text.format.TextColor[] cores = {
                            net.kyori.adventure.text.format.TextColor.color(0xA1887F), net.kyori.adventure.text.format.TextColor.color(0xB0BEC5),
                            net.kyori.adventure.text.format.TextColor.color(0xFFC107), net.kyori.adventure.text.format.TextColor.color(0x4DD0E1)};
                    double[] mult = {1.0, 1.5, 2.2, 3.5};
                    bau.customName(net.kyori.adventure.text.Component.text(nomes[nivel], cores[nivel]));
                    bau.update();
                    int base = (int) Math.round((3 + m.dificuldade.nivel()) * mult[nivel]);
                    Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, Masmorras.rolagens(m, base), false, r, true);
                    int mana = nivel == 0 ? r.nextInt(2) : nivel + r.nextInt(nivel + 1);
                    if (mana > 0) bau.getBlockInventory().addItem(ManaCristal.criar(nivel >= 3 ? ManaCristal.Tamanho.MEDIO : ManaCristal.Tamanho.PEQUENO, mana));
                    if (nivel == 3 && r.nextDouble() < 0.01) bau.getBlockInventory().addItem(MagiaOculta.grimorio());
                    // 2.70: às vezes o baú é um mímico (o tesouro fica com ele: vença para pegar).
                    if (nivel <= 2 && s.tipo != TipoSala.SECRETA && r.nextDouble() < 0.04 + m.rank().ordinal() * 0.01) m.mimicos.add(b.getLocation());
                }
            }
        }
    }
}
