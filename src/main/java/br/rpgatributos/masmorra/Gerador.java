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
import org.bukkit.block.sign.Side;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.EntityType;

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
 * Monta a planta da masmorra (uma árvore de salas numa grade) e constrói os blocos
 * em pedaços pequenos, um por vez, para o servidor não travar.
 */
final class Gerador {

    /** Altura do piso das salas. */
    static final int Y = 64;
    /** Distância entre os centros de duas salas vizinhas. */
    static final int PASSO = 29;
    private static final BlockFace[] DIRECOES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final RPGAtributos plugin;
    private final SalasPersonalizadas personalizadas;
    /** Tipo de monstro pedido numa placa [monstro] (por posição). */
    final Map<Location, EntityType> tiposMarcados = new HashMap<>();

    Gerador(RPGAtributos plugin, SalasPersonalizadas personalizadas) {
        this.plugin = plugin;
        this.personalizadas = personalizadas;
    }

    private static long chave(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    // =====================================================================
    //  Planta
    // =====================================================================

    void planejar(Masmorra m, Random r) {
        Dificuldade d = m.dificuldade;
        int total = d.salasMin() + r.nextInt(d.salasMax() - d.salasMin() + 1);
        Map<Long, Sala> grade = new HashMap<>();
        Sala inicio = new Sala(0, 0, TipoSala.INICIO, 0);
        m.salas.add(inicio);
        grade.put(chave(0, 0), inicio);
        for (int tentativa = 0; m.salas.size() < total && tentativa < 2000; tentativa++) {
            Sala base = m.salas.get(r.nextInt(m.salas.size()));
            if (base.portas.size() >= 3 || (base == inicio && base.portas.size() >= 2)) continue;
            BlockFace f = DIRECOES[r.nextInt(DIRECOES.length)];
            int nx = base.gx + f.getModX(), nz = base.gz + f.getModZ();
            if (grade.containsKey(chave(nx, nz))) continue;
            Sala nova = new Sala(nx, nz, TipoSala.COMBATE, base.profundidade + 1);
            base.portas.add(f);
            nova.portas.add(f.getOppositeFace());
            m.salas.add(nova);
            grade.put(chave(nx, nz), nova);
        }

        // O chefe fica na ponta mais longe da entrada.
        List<Sala> pontas = new ArrayList<>();
        for (Sala s : m.salas) if (s != inicio && s.portas.size() == 1) pontas.add(s);
        Sala chefe = null;
        for (Sala s : (pontas.isEmpty() ? m.salas : pontas)) {
            if (s != inicio && (chefe == null || s.profundidade > chefe.profundidade)) chefe = s;
        }
        chefe.tipo = TipoSala.CHEFE;
        m.chefe = chefe;
        m.inicio = inicio;

        // Tesouros nas outras pontas (ou em salas de combate, se faltar ponta).
        int tesouros = total >= 9 ? 2 : 1;
        List<Sala> candidatas = new ArrayList<>(pontas);
        candidatas.remove(chefe);
        Collections.shuffle(candidatas, r);
        List<Sala> resto = new ArrayList<>();
        for (Sala s : m.salas) if (s.tipo == TipoSala.COMBATE && !candidatas.contains(s)) resto.add(s);
        Collections.shuffle(resto, r);
        candidatas.addAll(resto);
        for (int i = 0; i < tesouros && i < candidatas.size(); i++) {
            Sala s = candidatas.get(i);
            if (s.tipo == TipoSala.COMBATE) s.tipo = TipoSala.TESOURO;
        }

        int ox = 100_000 + m.vaga * 2_000, oz = 100_000;
        m.minX = Integer.MAX_VALUE;
        m.minZ = Integer.MAX_VALUE;
        m.maxX = Integer.MIN_VALUE;
        m.maxZ = Integer.MIN_VALUE;
        for (Sala s : m.salas) {
            s.posicionar(ox + s.gx * PASSO, Y, oz + s.gz * PASSO);
            m.minX = Math.min(m.minX, s.minX);
            m.minZ = Math.min(m.minZ, s.minZ);
            m.maxX = Math.max(m.maxX, s.maxX);
            m.maxZ = Math.max(m.maxZ, s.maxZ);
        }
    }

    // =====================================================================
    //  Obras (cada tarefa é um pedaço pequeno)
    // =====================================================================

    Deque<Runnable> obras(Masmorra m, Random r) {
        Deque<Runnable> fila = new ArrayDeque<>();
        double chancePersonalizada = plugin.settings().masChanceSalaPersonalizada;
        for (Sala s : m.salas) {
            fila.add(() -> {
                SalasPersonalizadas.Modelo modelo = r.nextDouble() < chancePersonalizada ? personalizadas.sortear(s.tipo, r) : null;
                if (modelo != null) colocarPersonalizada(m, s, modelo, r);
                else construirSala(m, s, r);
            });
        }
        // Corredores (um por ligação: só para leste e sul, para não repetir).
        for (Sala s : m.salas) {
            for (BlockFace f : s.portas) {
                if (f != BlockFace.EAST && f != BlockFace.SOUTH) continue;
                Sala vizinha = vizinha(m, s, f);
                if (vizinha != null) fila.add(() -> corredor(m, s, vizinha, f));
            }
        }
        // Por último, os baús (as salas personalizadas já foram lidas).
        fila.add(() -> encherBaus(m, r));
        return fila;
    }

    static Sala vizinha(Masmorra m, Sala s, BlockFace f) {
        for (Sala o : m.salas) if (o.gx == s.gx + f.getModX() && o.gz == s.gz + f.getModZ()) return o;
        return null;
    }

    private static void por(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    // ---------- salas feitas pelo código ----------

    private void construirSala(Masmorra m, Sala s, Random r) {
        World w = m.mundo;
        Tema t = m.tema;
        for (int x = s.minX; x <= s.maxX; x++) {
            for (int z = s.minZ; z <= s.maxZ; z++) {
                por(w, x, s.minY, z, t.piso());
                por(w, x, s.maxY, z, t.parede());
                boolean borda = x == s.minX || x == s.maxX || z == s.minZ || z == s.maxZ;
                if (!borda) continue;
                for (int y = s.minY + 1; y < s.maxY; y++) por(w, x, y, z, t.parede());
            }
        }
        // Luz no teto, em grade.
        for (int x = s.minX + 3; x <= s.maxX - 3; x += 5) {
            for (int z = s.minZ + 3; z <= s.maxZ - 3; z += 5) por(w, x, s.maxY, z, t.luz());
        }
        int cx = s.centroX, cz = s.centroZ, y = s.minY;
        switch (s.tipo) {
            case INICIO -> {
                for (int i = -3; i <= 3; i++) {
                    por(w, cx + i, y, cz, t.destaque());
                    por(w, cx, y, cz + i, t.destaque());
                }
                for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) por(w, cx + c[0], y + 1, cz + c[1], t.lanterna());
                s.entrada = new Location(w, cx + 0.5, y + 1, cz + 0.5);
            }
            case COMBATE -> combate(w, s, t, r);
            case TESOURO -> {
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) por(w, cx + dx, y, cz + dz, t.destaque());
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) por(w, cx + dx, y + 1, cz + dz, t.destaque());
                s.pontosBau.add(new Location(w, cx, y + 2, cz));
                s.pontosBau.add(new Location(w, cx - 4, y + 1, cz));
                s.pontosBau.add(new Location(w, cx + 4, y + 1, cz));
                if (m.dificuldade.nivel() >= 2) {
                    for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) por(w, cx + c[0], y + 1, cz + c[1], Material.GOLD_BLOCK);
                }
                for (int[] c : new int[][]{{5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) por(w, cx + c[0], y + 1, cz + c[1], t.lanterna());
            }
            case CHEFE -> {
                for (int[] c : new int[][]{{7, 7}, {-7, 7}, {7, -7}, {-7, -7}}) {
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) pilar(w, s, cx + c[0] + dx, cz + c[1] + dz, t);
                    por(w, cx + c[0], s.maxY, cz + c[1], t.luz());
                }
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        double d2 = dx * dx + dz * dz;
                        if (d2 <= 16 && d2 >= 9) por(w, cx + dx, y, cz + dz, t.destaque());
                    }
                }
                s.pontoChefe = new Location(w, cx, y, cz);
            }
        }
        decorar(w, s, t, r);
    }

    private static void pilar(World w, Sala s, int x, int z, Tema t) {
        for (int y = s.minY + 1; y < s.maxY; y++) por(w, x, y, z, t.pilar());
    }

    private static void combate(World w, Sala s, Tema t, Random r) {
        int cx = s.centroX, cz = s.centroZ, y = s.minY;
        switch (r.nextInt(4)) {
            case 0 -> { // quatro pilares
                for (int[] c : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) {
                    pilar(w, s, cx + c[0], cz + c[1], t);
                    por(w, cx + c[0] + 1, y + 1, cz + c[1], t.lanterna());
                }
            }
            case 1 -> { // plataforma no meio
                for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) por(w, cx + dx, y + 1, cz + dz, t.destaque());
                for (int[] c : new int[][]{{6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) pilar(w, s, cx + c[0], cz + c[1], t);
            }
            case 2 -> { // barricadas baixas (dá para pular)
                for (int i = 0; i < 5; i++) {
                    boolean emX = r.nextBoolean();
                    int comp = 3 + r.nextInt(3);
                    int x0 = cx - 6 + r.nextInt(10), z0 = cz - 6 + r.nextInt(10);
                    for (int k = 0; k < comp; k++) {
                        int x = emX ? x0 + k : x0, z = emX ? z0 : z0 + k;
                        if (x <= s.minX + 1 || x >= s.maxX - 1 || z <= s.minZ + 1 || z >= s.maxZ - 1) continue;
                        if (Math.abs(x - cx) <= 1 || Math.abs(z - cz) <= 1) continue; // deixa os caminhos das portas livres
                        por(w, x, y + 1, z, t.parede());
                    }
                }
            }
            default -> { // fosso no meio (água, lava ou neve fofa)
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        por(w, cx + dx, y - 1, cz + dz, t.parede());
                        por(w, cx + dx, y, cz + dz, t.liquido());
                    }
                }
            }
        }
    }

    /** Enfeites no chão perto das paredes e pendurados no teto (sem tapar as portas). */
    private static void decorar(World w, Sala s, Tema t, Random r) {
        for (int x = s.minX + 1; x < s.maxX; x++) {
            for (int z = s.minZ + 1; z < s.maxZ; z++) {
                boolean beira = x == s.minX + 1 || x == s.maxX - 1 || z == s.minZ + 1 || z == s.maxZ - 1;
                if (!beira || Math.abs(x - s.centroX) <= 2 || Math.abs(z - s.centroZ) <= 2 || r.nextDouble() > 0.08) continue;
                Block b = w.getBlockAt(x, s.minY + 1, z);
                if (b.isEmpty()) b.setType(t.decorChao(), false);
            }
        }
        for (int i = 0; i < 6; i++) {
            int x = s.minX + 2 + r.nextInt(s.maxX - s.minX - 3), z = s.minZ + 2 + r.nextInt(s.maxZ - s.minZ - 3);
            Block b = w.getBlockAt(x, s.maxY - 1, z);
            if (b.isEmpty()) b.setType(t.decorTeto(), false);
        }
    }

    // ---------- salas personalizadas ----------

    private void colocarPersonalizada(Masmorra m, Sala s, SalasPersonalizadas.Modelo modelo, Random r) {
        World w = m.mundo;
        modelo.estrutura().place(new Location(w, s.minX, s.minY, s.minZ), true, StructureRotation.NONE, Mirror.NONE, 0, 1f, r);
        s.personalizada = modelo.nome();
        // Placas viram marcas: [monstro] (tipo na 2ª linha), [bau], [chefe], [entrada].
        for (int x = s.minX; x <= s.maxX; x++) {
            for (int y = s.minY; y <= s.maxY; y++) {
                for (int z = s.minZ; z <= s.maxZ; z++) {
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
        if (s.tipo == TipoSala.INICIO && s.entrada == null) s.entrada = s.centro(w);
        if (s.tipo == TipoSala.CHEFE && s.pontoChefe == null) s.pontoChefe = new Location(w, s.centroX, s.minY, s.centroZ);
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

    /** Liga duas salas: chão, paredes e teto do corredor, e abre as portas nas duas paredes. */
    private static void corredor(Masmorra m, Sala a, Sala b, BlockFace f) {
        World w = m.mundo;
        Tema t = m.tema;
        boolean emX = f == BlockFace.EAST;
        int inicio = emX ? a.maxX + 1 : a.maxZ + 1, fim = emX ? b.minX - 1 : b.minZ - 1;
        int linha = emX ? a.centroZ : a.centroX;
        int y = Y;
        for (int p = inicio; p <= fim; p++) {
            for (int lado = -2; lado <= 2; lado++) {
                int x = emX ? p : linha + lado, z = emX ? linha + lado : p;
                por(w, x, y, z, t.piso());
                por(w, x, y + 5, z, t.parede());
                if (Math.abs(lado) == 2) for (int dy = 1; dy <= 4; dy++) por(w, x, y + dy, z, t.parede());
            }
            if ((p - inicio) % 4 == 1) por(w, emX ? p : linha, y + 5, emX ? linha : p, t.luz());
        }
        // Portas: 3 de largura e 4 de altura, no meio das paredes.
        for (int lado = -1; lado <= 1; lado++) {
            for (int dy = 1; dy <= 4; dy++) {
                if (emX) {
                    por(w, a.maxX, y + dy, linha + lado, Material.AIR);
                    por(w, b.minX, y + dy, linha + lado, Material.AIR);
                } else {
                    por(w, linha + lado, y + dy, a.maxZ, Material.AIR);
                    por(w, linha + lado, y + dy, b.minZ, Material.AIR);
                }
            }
        }
    }

    // ---------- baús ----------

    private void encherBaus(Masmorra m, Random r) {
        for (Sala s : m.salas) {
            for (Location l : s.pontosBau) {
                Block b = l.getBlock();
                b.setType(Material.CHEST, false);
                if (b.getState() instanceof Chest bau) {
                    Tesouro.encher(plugin, bau.getBlockInventory(), m.dificuldade, 3 + m.dificuldade.nivel(), false, r);
                }
            }
        }
    }
}
