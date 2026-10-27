package br.rpgatributos.estrada;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.colonia.Colonia;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rede de estradas (2.96). Uma placa com {@code [estrada]} e um nome, na beira do piso, é um <b>marco</b>.
 * Cada marco novo procura os marcos da mesma estrada a até {@code trecho-maximo} blocos e confere se dá
 * para ir de um ao outro só por piso de estrada (busca em largura pelos blocos com {@link Pisos#nivelEm},
 * subindo e descendo 1, também na diagonal). Achou: o <b>trecho</b> fica ligado, com o caminho guardado
 * em pontos (para a caravana) e quanto dele é iluminado.
 * <p>
 * Marcos ligados formam uma estrada; se ela passa por duas colônias (marco dentro da colônia ou a até 48
 * blocos da prefeitura), as duas ficam <b>ligadas</b>: o Mercador vende mais, a felicidade sobe e saem
 * <b>caravanas</b> com as sobras do depósito, que voltam com esmeraldas (1,5× o lote) — ou são
 * assaltadas no caminho, mais nas estradas escuras. Se alguém estiver perto, a caravana aparece andando
 * na estrada (uma mula com carga), e o assalto vira luta de verdade.
 * Ninguém troca nada com ninguém: a carga sai e volta para a mesma colônia.
 */
public final class Rede implements Listener {

    public static final TextColor COR = TextColor.color(0xC9A66B);
    private static final int MAX_NOS = 40_000;
    private static final int PERTO_PREFEITURA = 48;
    private static final int PASSO_PONTOS = 6;
    /** Na busca, o vizinho no mesmo nível vem primeiro, depois um acima e um abaixo. */
    private static final int[] ALTURAS = {0, 1, -1};

    static final class Marco {
        final String estrada;
        final String mundo;
        final int x, y, z, px, py, pz;

        Marco(String estrada, String mundo, int x, int y, int z, int px, int py, int pz) {
            this.estrada = estrada;
            this.mundo = mundo;
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = px;
            this.py = py;
            this.pz = pz;
        }

        String id() { return mundo + ":" + x + ":" + y + ":" + z; }
        String chave() { return Placas.normal(estrada); }
        World world() { return Bukkit.getWorld(mundo); }
        Location placa() { World w = world(); return w == null ? null : new Location(w, x + 0.5, y, z + 0.5); }
        Location pe() { World w = world(); return w == null ? null : new Location(w, px + 0.5, py + 1, pz + 0.5); }
    }

    static final class Trecho {
        final String a, b;
        final List<int[]> pontos;
        final int luz, comprimento;

        Trecho(String a, String b, List<int[]> pontos, int luz, int comprimento) {
            this.a = a;
            this.b = b;
            this.pontos = pontos;
            this.luz = luz;
            this.comprimento = comprimento;
        }

        boolean liga(String m) { return a.equals(m) || b.equals(m); }
        String outro(String m) { return a.equals(m) ? b : a; }
    }

    /** Uma caravana a caminho (a carga sai da origem e volta com esmeraldas para a mesma colônia). */
    static final class Caravana {
        final UUID origem, destino;
        final String nomeOrigem, nomeDestino, estrada;
        final int lotes;
        final List<Location> rota;
        final long partida, chegada;
        final double assaltoEm;
        final int luz;
        boolean assaltada, defendida, perdida;
        UUID mula;
        final List<UUID> salteadores = new ArrayList<>();

        Caravana(UUID origem, UUID destino, String nomeOrigem, String nomeDestino, String estrada, int lotes,
                 List<Location> rota, long partida, long chegada, double assaltoEm, int luz) {
            this.origem = origem;
            this.destino = destino;
            this.nomeOrigem = nomeOrigem;
            this.nomeDestino = nomeDestino;
            this.estrada = estrada;
            this.lotes = lotes;
            this.rota = rota;
            this.partida = partida;
            this.chegada = chegada;
            this.assaltoEm = assaltoEm;
            this.luz = luz;
        }

        double progresso(long agora) {
            return Math.max(0, Math.min(1, (agora - partida) / (double) Math.max(1, chegada - partida)));
        }

        Location onde(long agora) {
            if (rota.isEmpty()) return null;
            return rota.get((int) Math.min(rota.size() - 1, Math.round(progresso(agora) * (rota.size() - 1))));
        }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final NamespacedKey kCaravana;
    private final Map<String, Marco> marcos = new LinkedHashMap<>();
    private final List<Trecho> trechos = new ArrayList<>();
    private final List<Caravana> caravanas = new ArrayList<>();
    private final Map<UUID, Long> proxima = new HashMap<>();
    /** Colônia (dono) → colônias ligadas por estrada, e o nome da estrada de cada ligação. */
    private final Map<UUID, Map<UUID, String>> ligacoes = new HashMap<>();
    private long ligacoesEm;
    private int versao;

    public Rede(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "estradas.yml");
        this.kCaravana = new NamespacedKey(plugin, "caravana");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Marcos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoEscrever(SignChangeEvent e) {
        Block b = e.getBlock();
        Marco antigo = marcos.get(id(b));
        if (antigo != null) remover(antigo); // reescreveu a placa de um marco
        if (!plugin.settings().estradaRede || !Placas.ehPlaca(Placas.texto(e.line(0)), "estrada", "marco")) return;
        Player p = e.getPlayer();
        String nome = (Placas.texto(e.line(1)) + " " + Placas.texto(e.line(2))).trim().replace("|", "/");
        if (nome.isEmpty()) {
            e.line(0, Component.text("[estrada]", NamedTextColor.RED));
            p.sendMessage(Component.text("✖ Escreva o nome da estrada na segunda linha (ex.: Estrada Real).", NamedTextColor.RED));
            return;
        }
        Block pe = acharPe(b);
        if (pe == null) {
            e.line(0, Component.text("[estrada]", NamedTextColor.RED));
            p.sendMessage(Component.text("✖ O marco precisa ficar na beira da estrada: a até 2 blocos de um piso de estrada (trilha, cascalho, calçada, ponte...).", NamedTextColor.RED));
            return;
        }
        Marco m = new Marco(nome, b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), pe.getX(), pe.getY(), pe.getZ());
        marcos.put(m.id(), m);
        int ligados = 0;
        Marco semCaminho = null;
        for (Marco o : vizinhosPossiveis(m)) {
            Trecho t = verificar(m, o);
            if (t != null) {
                trechos.add(t);
                ligados++;
            } else if (semCaminho == null) {
                semCaminho = o;
            }
        }
        int numero = (int) marcos.values().stream().filter(x -> x.chave().equals(m.chave())).count();
        String[] n = Placas.duasLinhas(nome);
        e.line(0, Component.text("═ Estrada ═", COR, TextDecoration.BOLD));
        e.line(1, Component.text(n[0], NamedTextColor.BLACK));
        e.line(2, Component.text(n[1], NamedTextColor.BLACK));
        e.line(3, Component.text("marco nº " + numero, NamedTextColor.DARK_GRAY));
        plugin.placas().encerar(b);
        ligacoesEm = 0;
        salvar();
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        if (ligados > 0) {
            b.getWorld().playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.4f);
            p.sendMessage(Component.text("✔ Marco da " + nome + ": ligado a " + ligados + " marco(s) pela estrada.", COR));
        } else if (semCaminho != null) {
            Location o = semCaminho.placa();
            p.sendMessage(Component.text("⚠ Marco da " + nome + " posto, mas não achei caminho de estrada contínuo até o marco a "
                    + Placas.distancia(c, o) + " blocos (" + Placas.rumo(c, o) + "). Complete a estrada e clique no marco para conferir de novo.", NamedTextColor.GOLD));
        } else {
            p.sendMessage(Component.text("✔ Primeiro marco da " + nome + ". Ponha o próximo a até " + plugin.settings().estradaTrecho
                    + " blocos, na beira da mesma estrada.", COR));
        }
        Map<UUID, String> lig = ligacoesDeTodos().getOrDefault(donoPerto(m), Map.of());
        if (!lig.isEmpty()) p.sendMessage(Component.text("✦ A estrada já liga a sua colônia a " + lig.size() + " outra(s).", COR));
    }

    /** Um piso de estrada a até 2 blocos (de lado) e até 3 abaixo da placa, com espaço em cima. */
    private static Block acharPe(Block placa) {
        Block melhor = null;
        int melhorD = Integer.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy >= -3; dy--) {
                    Block c = placa.getRelative(dx, dy, dz);
                    if (Pisos.nivelEm(c) > 0 && !c.getRelative(BlockFace.UP).getType().isOccluding()) {
                        int d = dx * dx + dz * dz + dy * dy;
                        if (d < melhorD) {
                            melhorD = d;
                            melhor = c;
                        }
                    }
                }
            }
        }
        return melhor;
    }

    private List<Marco> vizinhosPossiveis(Marco m) {
        List<Marco> l = new ArrayList<>();
        int max = plugin.settings().estradaTrecho;
        for (Marco o : marcos.values()) {
            if (o == m || !o.mundo.equals(m.mundo) || !o.chave().equals(m.chave())) continue;
            if (Math.hypot(o.px - m.px, o.pz - m.pz) <= max) l.add(o);
        }
        l.sort((a, b) -> Double.compare(Math.hypot(a.px - m.px, a.pz - m.pz), Math.hypot(b.px - m.px, b.pz - m.pz)));
        return l.size() > 3 ? l.subList(0, 3) : l;
    }

    static String id(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private void remover(Marco m) {
        marcos.remove(m.id());
        trechos.removeIf(t -> t.liga(m.id()));
        ligacoesEm = 0;
        salvar();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Marco m = marcos.get(id(e.getBlock()));
        if (m == null) return;
        remover(m);
        e.getPlayer().sendMessage(Component.text("Marco da " + m.estrada + " retirado.", NamedTextColor.GRAY));
    }

    /** Clique no marco: confere os trechos dele de novo e conta como está a estrada. */
    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null
                || !Tag.ALL_SIGNS.isTagged(e.getClickedBlock().getType())) return;
        Marco m = marcos.get(id(e.getClickedBlock()));
        if (m == null) return;
        e.setCancelled(true);
        reverificar(m);
        mostrar(e.getPlayer(), m);
    }

    private void reverificar(Marco m) {
        trechos.removeIf(t -> t.liga(m.id()));
        for (Marco o : vizinhosPossiveis(m)) {
            Trecho t = verificar(m, o);
            if (t != null) trechos.add(t);
        }
        ligacoesEm = 0;
        salvar();
    }

    private void mostrar(Player p, Marco m) {
        Set<String> comp = componente(m.id());
        int comprimento = 0, luz = 0, n = 0;
        Set<Trecho> vistos = new HashSet<>();
        for (Trecho t : trechos) {
            if (comp.contains(t.a) && vistos.add(t)) {
                comprimento += t.comprimento;
                luz += t.luz * t.comprimento;
                n += t.comprimento;
            }
        }
        p.sendMessage(Component.text("═ " + m.estrada + " ═", COR, TextDecoration.BOLD));
        p.sendMessage(Component.text("Marcos ligados: " + comp.size() + " • trechos: " + vistos.size()
                + (comprimento > 0 ? " • uns " + comprimento + " blocos" : ""), NamedTextColor.GRAY));
        if (n > 0) {
            int pct = luz / n;
            p.sendMessage(Component.text("Iluminação: " + pct + "%" + (pct < 40 ? " (estrada escura: os salteadores gostam)" : pct >= 70 ? " (bem iluminada)" : ""),
                    pct < 40 ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        }
        Set<String> nomes = new java.util.TreeSet<>();
        for (Colonia c : plugin.colonias().todas()) {
            for (String id : comp) {
                Marco o = marcos.get(id);
                if (o != null && toca(c, o)) { nomes.add("Colônia de " + c.nomeDono()); break; }
            }
        }
        p.sendMessage(Component.text(nomes.size() >= 2 ? "Liga: " + String.join(" ↔ ", nomes)
                : nomes.size() == 1 ? "Passa pela " + nomes.iterator().next() + "; ainda não chega a outra colônia."
                : "Ainda não passa por nenhuma colônia.", nomes.size() >= 2 ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        Location aqui = m.placa();
        List<String> prox = new ArrayList<>();
        for (Trecho t : trechos) {
            if (!t.liga(m.id())) continue;
            Marco o = marcos.get(t.outro(m.id()));
            if (o != null) prox.add(Placas.rumo(aqui, o.placa()) + " a " + Placas.distancia(aqui, o.placa()) + " blocos");
        }
        p.sendMessage(Component.text(prox.isEmpty() ? "Nenhum marco vizinho ligado (ponha outro a até " + plugin.settings().estradaTrecho
                + " blocos, na mesma estrada)." : "Próximos marcos: " + String.join("; ", prox) + ".", NamedTextColor.GRAY));
    }

    // =====================================================================
    //  Verificar um trecho (busca em largura pelo piso)
    // =====================================================================

    private static long chave(int x, int y, int z) {
        return ((long) ((x + (1 << 25)) & 0x3FFFFFF) << 38) | ((long) ((z + (1 << 25)) & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
    }

    private static int cx(long k) { return (int) ((k >>> 38) & 0x3FFFFFF) - (1 << 25); }
    private static int cz(long k) { return (int) ((k >>> 12) & 0x3FFFFFF) - (1 << 25); }
    private static int cy(long k) { return (int) (k & 0xFFF) - 2048; }

    /** Há caminho de estrada de {@code a} até {@code b}? Devolve o trecho (com o caminho) ou null. */
    Trecho verificar(Marco a, Marco b) {
        World w = a.world();
        if (w == null || !a.mundo.equals(b.mundo)) return null;
        double mx = (a.px + b.px) / 2.0, mz = (a.pz + b.pz) / 2.0;
        double raio = Math.hypot(a.px - b.px, a.pz - b.pz) / 2.0 + 32;
        long ini = chave(a.px, a.py, a.pz);
        Map<Long, Long> pai = new HashMap<>();
        ArrayDeque<Long> fila = new ArrayDeque<>();
        pai.put(ini, ini);
        fila.add(ini);
        Long fim = null;
        while (!fila.isEmpty() && pai.size() < MAX_NOS) {
            long k = fila.poll();
            int x = cx(k), y = cy(k), z = cz(k);
            if (Math.abs(x - b.px) <= 1 && Math.abs(z - b.pz) <= 1 && Math.abs(y - b.py) <= 2) {
                fim = k;
                break;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = x + dx, nz = z + dz;
                    if (Math.hypot(nx - mx, nz - mz) > raio) continue;
                    for (int dy : ALTURAS) {
                        long nk = chave(nx, y + dy, nz);
                        if (pai.containsKey(nk)) continue;
                        Block nb = w.getBlockAt(nx, y + dy, nz);
                        if (Pisos.nivelEm(nb) > 0 && !nb.getRelative(BlockFace.UP).getType().isOccluding()) {
                            pai.put(nk, k);
                            fila.add(nk);
                            break;
                        }
                    }
                }
            }
        }
        if (fim == null) return null;
        List<Long> caminho = new ArrayList<>();
        for (long k = fim; ; k = pai.get(k)) {
            caminho.add(k);
            if (k == ini) break;
        }
        Collections.reverse(caminho);
        List<int[]> pontos = new ArrayList<>();
        int acesos = 0;
        for (int i = 0; i < caminho.size(); i++) {
            long k = caminho.get(i);
            if (w.getBlockAt(cx(k), cy(k) + 1, cz(k)).getLightFromBlocks() >= 8) acesos++;
            if (i % PASSO_PONTOS == 0 || i == caminho.size() - 1) pontos.add(new int[]{cx(k), cy(k) + 1, cz(k)});
        }
        return new Trecho(a.id(), b.id(), pontos, Math.round(100f * acesos / caminho.size()), caminho.size());
    }

    // =====================================================================
    //  Ligações entre colônias
    // =====================================================================

    /** Os marcos ligados a este (o pedaço de estrada contínuo). */
    private Set<String> componente(String inicio) {
        Set<String> vistos = new HashSet<>();
        ArrayDeque<String> fila = new ArrayDeque<>();
        vistos.add(inicio);
        fila.add(inicio);
        while (!fila.isEmpty()) {
            String m = fila.poll();
            for (Trecho t : trechos) {
                if (!t.liga(m)) continue;
                String o = t.outro(m);
                if (marcos.containsKey(o) && vistos.add(o)) fila.add(o);
            }
        }
        return vistos;
    }

    private boolean toca(Colonia c, Marco m) {
        Location pref = c.prefeitura();
        if (pref == null || !pref.getWorld().getName().equals(m.mundo)) return false;
        if (Math.hypot(pref.getX() - m.x, pref.getZ() - m.z) <= PERTO_PREFEITURA) return true;
        Location l = m.placa();
        return l != null && plugin.colonias().naColonia(c, l);
    }

    private UUID donoPerto(Marco m) {
        for (Colonia c : plugin.colonias().todas()) if (toca(c, m)) return c.dono();
        return null;
    }

    private Map<UUID, Map<UUID, String>> ligacoesDeTodos() {
        long agora = System.currentTimeMillis();
        if (agora - ligacoesEm < 60_000) return ligacoes;
        ligacoesEm = agora;
        ligacoes.clear();
        Set<String> feitos = new HashSet<>();
        for (Marco m : marcos.values()) {
            if (feitos.contains(m.id())) continue;
            Set<String> comp = componente(m.id());
            feitos.addAll(comp);
            Set<UUID> tocadas = new HashSet<>();
            for (Colonia c : plugin.colonias().todas()) {
                for (String id : comp) {
                    Marco o = marcos.get(id);
                    if (o != null && toca(c, o)) { tocadas.add(c.dono()); break; }
                }
            }
            if (tocadas.size() < 2) continue;
            for (UUID a : tocadas) for (UUID b : tocadas) if (!a.equals(b)) ligacoes.computeIfAbsent(a, k -> new HashMap<>()).putIfAbsent(b, m.estrada);
        }
        return ligacoes;
    }

    /** As colônias ligadas a esta por estrada (dono → nome da estrada). */
    public Map<UUID, String> ligacoes(Colonia c) {
        if (!plugin.settings().estradaRede) return Map.of();
        return ligacoesDeTodos().getOrDefault(c.dono(), Map.of());
    }

    /** O caminho (pontos) de um marco que toca a origem até um que toca o destino, pelos trechos; {@code luz[0]} recebe a iluminação média (%). */
    private List<Location> rota(Colonia origem, Colonia destino, int[] luz) {
        World w = origem.world();
        if (w == null) return List.of();
        List<String> inicio = new ArrayList<>();
        Set<String> alvo = new HashSet<>();
        for (Marco m : marcos.values()) {
            if (toca(origem, m)) inicio.add(m.id());
            if (toca(destino, m)) alvo.add(m.id());
        }
        Map<String, String> pai = new HashMap<>();
        ArrayDeque<String> fila = new ArrayDeque<>();
        for (String s : inicio) { pai.put(s, s); fila.add(s); }
        String fim = null;
        while (!fila.isEmpty()) {
            String m = fila.poll();
            if (alvo.contains(m)) { fim = m; break; }
            for (Trecho t : trechos) {
                if (!t.liga(m)) continue;
                String o = t.outro(m);
                if (marcos.containsKey(o) && !pai.containsKey(o)) { pai.put(o, m); fila.add(o); }
            }
        }
        if (fim == null) return List.of();
        List<String> seq = new ArrayList<>();
        for (String m = fim; ; m = pai.get(m)) {
            seq.add(m);
            if (pai.get(m).equals(m)) break;
        }
        Collections.reverse(seq);
        List<Location> l = new ArrayList<>();
        int soma = 0, n = 0;
        for (int i = 0; i + 1 < seq.size(); i++) {
            Trecho t = trechoEntre(seq.get(i), seq.get(i + 1));
            if (t == null) continue;
            soma += t.luz * t.comprimento;
            n += t.comprimento;
            luz[0] = soma / n;
            List<int[]> pts = new ArrayList<>(t.pontos);
            if (!t.a.equals(seq.get(i))) Collections.reverse(pts);
            for (int[] p : pts) l.add(new Location(w, p[0] + 0.5, p[1], p[2] + 0.5));
        }
        return l;
    }

    private Trecho trechoEntre(String a, String b) {
        for (Trecho t : trechos) if (t.liga(a) && t.liga(b)) return t;
        return null;
    }

    // =====================================================================
    //  Caravanas
    // =====================================================================

    /** A cada 2 s: anda com as caravanas, mostra a mula para quem está perto e resolve as chegadas. */
    public void tick() {
        if (!plugin.settings().estradaRede) return;
        long agora = System.currentTimeMillis();
        for (Iterator<Caravana> it = caravanas.iterator(); it.hasNext(); ) {
            Caravana cv = it.next();
            if (!cv.perdida && !cv.defendida && !cv.assaltada && cv.assaltoEm > 0 && cv.progresso(agora) >= cv.assaltoEm) assaltar(cv);
            if (cv.perdida || (agora >= cv.chegada && cv.salteadores.isEmpty())) {
                terminar(cv);
                it.remove();
                continue;
            }
            if (!cv.salteadores.isEmpty()) {
                // A mula sumiu no meio da luta (ninguém ficou perto e o pedaço descarregou): a carga se foi.
                if (cv.mula == null || !(Bukkit.getEntity(cv.mula) instanceof Mule mu) || !mu.isValid()) {
                    cv.perdida = true;
                    continue;
                }
                cv.salteadores.removeIf(id -> !(Bukkit.getEntity(id) instanceof LivingEntity le) || le.isDead());
                if (cv.salteadores.isEmpty()) defendida(cv);
                continue; // parada enquanto lutam
            }
            mostrarMula(cv, agora);
        }
        if (plugin.settings().estradaCaravanas) partidas(agora);
    }

    private void partidas(long agora) {
        for (Colonia c : plugin.colonias().todas()) {
            Map<UUID, String> lig = ligacoes(c);
            if (lig.isEmpty()) continue;
            Location pref = c.prefeitura();
            if (pref == null || !pref.isChunkLoaded()) continue;
            long quando = proxima.computeIfAbsent(c.dono(), k -> agora + plugin.settings().estradaCaravanaMinutos * 60_000L);
            if (agora < quando || caravanas.stream().anyMatch(x -> x.origem.equals(c.dono()))) continue;
            proxima.put(c.dono(), agora + plugin.settings().estradaCaravanaMinutos * 60_000L);
            partir(c, null, false);
        }
    }

    /** Uma caravana sai de {@code c} (destino ao acaso entre as ligadas). @return o aviso, ou null se não saiu. */
    public String partir(Colonia c, UUID destinoQuerido, boolean rapida) {
        Map<UUID, String> lig = ligacoes(c);
        if (lig.isEmpty()) return null;
        List<UUID> ds = new ArrayList<>(lig.keySet());
        UUID d = destinoQuerido != null && lig.containsKey(destinoQuerido) ? destinoQuerido : ds.get(rnd().nextInt(ds.size()));
        Colonia destino = plugin.colonias().de(d);
        if (destino == null) return null;
        int lotes = plugin.colonias().venderSobras(c, 2 + c.nivel());
        if (lotes <= 0) return "sem sobras";
        int[] luzRota = new int[1];
        List<Location> rota = rota(c, destino, luzRota);
        Location a = c.prefeitura(), b = destino.prefeitura();
        long agora = System.currentTimeMillis();
        long tempo = rapida ? 60_000L : Math.max(90_000L, Math.min(15 * 60_000L, (long) (Placas.distancia(a, b) / 2.5 * 1000)));
        int luz = luzRota[0];
        double chance = Math.max(0.03, Math.min(0.35, 0.3 * (1 - luz / 100.0) - (plugin.colonias().quantosSoldados(c) > 0 ? 0.1 : 0)));
        double assaltoEm = rnd().nextDouble() < chance ? 0.3 + rnd().nextDouble() * 0.4 : -1;
        Caravana cv = new Caravana(c.dono(), d, "Colônia de " + c.nomeDono(), "Colônia de " + destino.nomeDono(), lig.get(d),
                lotes, rota, agora, agora + tempo, assaltoEm, luz);
        caravanas.add(cv);
        avisar(c.dono(), Component.text("🐴 Uma caravana saiu da sua colônia para a " + cv.nomeDestino + " pela " + cv.estrada
                + ", com " + lotes + " lote(s) de sobras.", COR));
        return "caravana para a " + cv.nomeDestino + " com " + lotes + " lote(s)";
    }

    /** Força o assalto da caravana desta colônia agora (admin). */
    public boolean assaltarAgora(UUID origem) {
        for (Caravana cv : caravanas) {
            if (cv.origem.equals(origem) && !cv.assaltada) {
                assaltar(cv);
                return true;
            }
        }
        return false;
    }

    private void assaltar(Caravana cv) {
        cv.assaltada = true;
        Mule m = cv.mula == null ? null : (Bukkit.getEntity(cv.mula) instanceof Mule x ? x : null);
        if (m == null || !m.isValid()) {
            cv.perdida = true; // ninguém viu: a carga se foi
            return;
        }
        World w = m.getWorld();
        int qtd = 2 + rnd().nextInt(2);
        for (int i = 0; i < qtd; i++) {
            Location l = m.getLocation().add(rnd().nextDouble(-6, 6), 0, rnd().nextDouble(-6, 6));
            l = w.getHighestBlockAt(l).getLocation().add(0.5, 1, 0.5);
            LivingEntity s = (LivingEntity) w.spawnEntity(l, i == 0 ? EntityType.VINDICATOR : EntityType.PILLAGER);
            s.customName(Component.text("Salteador da Estrada", NamedTextColor.RED));
            s.setPersistent(false);
            s.getPersistentDataContainer().set(kCaravana, PersistentDataType.STRING, cv.origem.toString());
            if (s instanceof Mob mob) mob.setTarget(m);
            cv.salteadores.add(s.getUniqueId());
        }
        w.playSound(m.getLocation(), Sound.EVENT_RAID_HORN, 1.2f, 1.2f);
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(m.getLocation()) < 64 * 64) {
                p.sendMessage(Component.text("⚔ Salteadores atacam a caravana da " + cv.nomeOrigem + "! Defenda a mula.", NamedTextColor.RED, TextDecoration.BOLD));
            }
        }
    }

    private void defendida(Caravana cv) {
        cv.defendida = true;
        Entity m = cv.mula == null ? null : Bukkit.getEntity(cv.mula);
        if (m == null) return;
        for (Player p : m.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(m.getLocation()) < 32 * 32) {
                p.getInventory().addItem(new ItemStack(Material.EMERALD, 2)).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
                p.sendMessage(Component.text("✔ Caravana salva! O caravaneiro agradece com 2 esmeraldas.", NamedTextColor.GREEN));
            }
        }
        plugin.noticias().registrar("Salteadores atacaram a caravana da " + cv.nomeOrigem + " na " + cv.estrada + ", mas alguém a defendeu.");
        avisar(cv.origem, Component.text("🐴 A caravana da sua colônia foi atacada na " + cv.estrada + ", mas alguém a defendeu.", COR));
    }

    private void terminar(Caravana cv) {
        tirarMula(cv);
        for (UUID id : cv.salteadores) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        Colonia c = plugin.colonias().de(cv.origem);
        if (c == null) return;
        if (cv.perdida) {
            plugin.noticias().registrar("A caravana da " + cv.nomeOrigem + " foi assaltada na " + cv.estrada + ".");
            avisar(cv.origem, Component.text("✖ A caravana da sua colônia foi assaltada na " + cv.estrada + " e perdeu a carga ("
                    + cv.lotes + " lote(s)). Estrada iluminada e soldados na colônia ajudam.", NamedTextColor.RED));
            return;
        }
        int esmeraldas = (int) Math.round(cv.lotes * 1.5);
        boolean coube = plugin.colonias().depositar(c, new ItemStack(Material.EMERALD, esmeraldas));
        Colonia destino = plugin.colonias().de(cv.destino);
        if (destino != null) plugin.colonias().alterarFelicidade(destino, 3);
        plugin.noticias().registrar("Uma caravana da " + cv.nomeOrigem + " fez feira na " + cv.nomeDestino + ".");
        avisar(cv.origem, Component.text("🐴 A caravana voltou da " + cv.nomeDestino + " com " + esmeraldas + " esmeralda(s)"
                + (coube ? " (no depósito)." : ", mas o depósito estava cheio e elas se perderam."), coube ? COR : NamedTextColor.GOLD));
    }

    private void avisar(UUID dono, Component msg) {
        Player p = Bukkit.getPlayer(dono);
        if (p != null) {
            p.sendMessage(msg);
            return;
        }
        // Colônia de teste (dono inventado): o aviso vai para quem está na área de testes.
        Colonia c = plugin.colonias().de(dono);
        World w = c == null ? null : c.world();
        if (w != null && br.rpgatributos.ModoTeste.ehTeste(w)) for (Player t : w.getPlayers()) t.sendMessage(msg);
    }

    /** A mula aparece só quando alguém está a até 48 blocos do ponto da rota; anda até o ponto seguinte. */
    private void mostrarMula(Caravana cv, long agora) {
        Location onde = cv.onde(agora);
        if (onde == null || !onde.isChunkLoaded()) {
            tirarMula(cv);
            return;
        }
        boolean alguem = false;
        for (Player p : onde.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(onde) < 48 * 48) { alguem = true; break; }
        }
        Mule m = cv.mula == null ? null : (Bukkit.getEntity(cv.mula) instanceof Mule x && x.isValid() ? x : null);
        if (!alguem) {
            tirarMula(cv);
            return;
        }
        if (m == null) {
            m = (Mule) onde.getWorld().spawnEntity(onde, EntityType.MULE);
            m.setCarryingChest(true);
            m.setTamed(true);
            m.setAdult();
            m.setPersistent(false);
            m.customName(Component.text("Caravana da " + cv.nomeOrigem, COR));
            m.setCustomNameVisible(true);
            m.getPersistentDataContainer().set(kCaravana, PersistentDataType.STRING, cv.origem.toString());
            cv.mula = m.getUniqueId();
        } else if (m.getLocation().distanceSquared(onde) > 20 * 20) {
            m.teleport(onde);
        }
        int i = cv.rota.indexOf(onde);
        Location prox = cv.rota.get(Math.min(cv.rota.size() - 1, Math.max(0, i) + 2));
        m.getPathfinder().moveTo(prox, 1.2);
        if (rnd().nextInt(4) == 0) m.getWorld().spawnParticle(Particle.DUST_PLUME, m.getLocation(), 3, 0.3, 0.05, 0.3, 0);
    }

    private void tirarMula(Caravana cv) {
        if (cv.mula == null) return;
        Entity e = Bukkit.getEntity(cv.mula);
        if (e != null) e.remove();
        cv.mula = null;
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(kCaravana)) return;
        e.getDrops().clear();
        e.setDroppedExp(e.getEntity() instanceof Mule ? 0 : e.getDroppedExp());
        if (!(e.getEntity() instanceof Mule)) return;
        for (Caravana cv : caravanas) {
            if (e.getEntity().getUniqueId().equals(cv.mula)) {
                cv.mula = null;
                cv.perdida = true;
            }
        }
    }

    public void desligar() {
        for (Caravana cv : caravanas) {
            tirarMula(cv);
            for (UUID id : cv.salteadores) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
        }
        // As caravanas na estrada voltam com a carga (não se perde nada porque o servidor desligou).
        for (Caravana cv : caravanas) {
            Colonia c = plugin.colonias().de(cv.origem);
            if (c != null && !cv.perdida) plugin.colonias().depositar(c, new ItemStack(Material.EMERALD, (int) Math.round(cv.lotes * 1.5)));
        }
        caravanas.clear();
    }

    /**
     * 2.99.1: põe um marco numa placa já colocada (o cenário da área de testes), como se alguém escrevesse {@code [estrada]}.
     * @return quantos marcos ele ligou, ou -1 se a placa não está na beira de um piso de estrada.
     */
    public int marcar(Block placa, String nome) {
        if (!(placa.getState() instanceof org.bukkit.block.Sign s)) return -1;
        Block pe = acharPe(placa);
        if (pe == null) return -1;
        Marco antigo = marcos.get(id(placa));
        if (antigo != null) remover(antigo);
        Marco m = new Marco(nome, placa.getWorld().getName(), placa.getX(), placa.getY(), placa.getZ(), pe.getX(), pe.getY(), pe.getZ());
        marcos.put(m.id(), m);
        int ligados = 0;
        for (Marco o : vizinhosPossiveis(m)) {
            Trecho t = verificar(m, o);
            if (t != null) {
                trechos.add(t);
                ligados++;
            }
        }
        int numero = (int) marcos.values().stream().filter(x -> x.chave().equals(m.chave())).count();
        String[] n = Placas.duasLinhas(nome);
        var frente = s.getSide(org.bukkit.block.sign.Side.FRONT);
        frente.line(0, Component.text("═ Estrada ═", COR, TextDecoration.BOLD));
        frente.line(1, Component.text(n[0], NamedTextColor.BLACK));
        frente.line(2, Component.text(n[1], NamedTextColor.BLACK));
        frente.line(3, Component.text("marco nº " + numero, NamedTextColor.DARK_GRAY));
        s.setWaxed(true);
        s.update();
        ligacoesEm = 0;
        salvar();
        return ligados;
    }

    /** 2.99.1: os marcos e as caravanas de um mundo que deixou de existir (o de testes) saem da rede. */
    public int esquecerMundo(String mundo) {
        int antes = marcos.size();
        for (Iterator<Marco> it = marcos.values().iterator(); it.hasNext(); ) {
            Marco m = it.next();
            if (!m.mundo.equals(mundo)) continue;
            it.remove();
            trechos.removeIf(t -> t.liga(m.id()));
        }
        for (Iterator<Caravana> it = caravanas.iterator(); it.hasNext(); ) {
            Caravana cv = it.next();
            if (cv.rota.isEmpty() || !cv.rota.getFirst().getWorld().getName().equals(mundo)) continue;
            tirarMula(cv);
            for (UUID id : cv.salteadores) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            proxima.remove(cv.origem);
            it.remove();
        }
        int n = antes - marcos.size();
        if (n > 0) {
            ligacoesEm = 0;
            salvar();
        }
        return n;
    }

    /** A cada 5 minutos: marcos cuja placa sumiu saem da rede. */
    public void validar() {
        boolean mudou = false;
        for (Iterator<Marco> it = marcos.values().iterator(); it.hasNext(); ) {
            Marco m = it.next();
            World w = m.world();
            if (w == null || !w.isChunkLoaded(m.x >> 4, m.z >> 4)) continue;
            if (!Tag.ALL_SIGNS.isTagged(w.getBlockAt(m.x, m.y, m.z).getType())) {
                it.remove();
                trechos.removeIf(t -> t.liga(m.id()));
                mudou = true;
            }
        }
        if (mudou) {
            ligacoesEm = 0;
            salvar();
        }
    }

    // =====================================================================
    //  Para o admin e o guia
    // =====================================================================

    /** 2.98: um trecho de estrada perto de um lugar: os pontos dele, o mais perto, o nome e as colônias da estrada inteira. */
    public record Perto(String estrada, List<Location> pontos, int indice, List<Colonia> colonias) { }

    public Perto pertoDaEstrada(Location l, double raio) {
        Trecho melhor = null;
        int melhorI = -1;
        double best = raio * raio;
        for (Trecho t : trechos) {
            Marco a = marcos.get(t.a);
            if (a == null || !a.mundo.equals(l.getWorld().getName())) continue;
            for (int i = 0; i < t.pontos.size(); i++) {
                int[] p = t.pontos.get(i);
                double dx = p[0] + 0.5 - l.getX(), dz = p[2] + 0.5 - l.getZ(), dy = p[1] - l.getY();
                double d2 = dx * dx + dz * dz + dy * dy;
                if (d2 < best) {
                    best = d2;
                    melhor = t;
                    melhorI = i;
                }
            }
        }
        if (melhor == null) return null;
        World w = l.getWorld();
        List<Location> pts = new ArrayList<>();
        for (int[] p : melhor.pontos) pts.add(new Location(w, p[0] + 0.5, p[1], p[2] + 0.5));
        List<Colonia> cols = new ArrayList<>();
        Set<String> comp = componente(melhor.a);
        for (Colonia c : plugin.colonias().todas()) {
            for (String id : comp) {
                Marco o = marcos.get(id);
                if (o != null && toca(c, o)) { cols.add(c); break; }
            }
        }
        return new Perto(marcos.get(melhor.a).estrada, pts, melhorI, cols);
    }

    /** 2.99: muda sempre que a rede muda (os mapas redesenham as estradas). */
    public int versao() { return versao; }

    /** 2.99: os caminhos (pontos x, y, z) de todos os trechos deste mundo. */
    public List<List<int[]>> trechosDo(World w) {
        List<List<int[]>> l = new ArrayList<>();
        for (Trecho t : trechos) {
            Marco a = marcos.get(t.a);
            if (a != null && a.mundo.equals(w.getName())) l.add(t.pontos);
        }
        return l;
    }

    /** 2.99: onde está cada caravana agora (e se está sendo atacada). */
    public record NaEstrada(Location onde, boolean atacada) { }

    public List<NaEstrada> caravanasEm(World w) {
        List<NaEstrada> l = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (Caravana cv : caravanas) {
            Location o = cv.onde(agora);
            if (o != null && o.getWorld() == w) l.add(new NaEstrada(o, !cv.salteadores.isEmpty()));
        }
        return l;
    }

    public int quantosMarcos() { return marcos.size(); }

    public int quantosTrechos() { return trechos.size(); }

    public int quantasCaravanas() { return caravanas.size(); }

    public List<String> estradas() {
        Map<String, int[]> por = new LinkedHashMap<>();
        for (Marco m : marcos.values()) por.computeIfAbsent(m.estrada, k -> new int[1])[0]++;
        List<String> l = new ArrayList<>();
        por.forEach((n, q) -> l.add(n + " (" + q[0] + " marco(s))"));
        return l;
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        marcos.clear();
        trechos.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        for (String s : y.getStringList("marcos")) {
            String[] p = s.split("\\|");
            if (p.length < 8) continue;
            try {
                Marco m = new Marco(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                        Integer.parseInt(p[5]), Integer.parseInt(p[6]), Integer.parseInt(p[7]));
                marcos.put(m.id(), m);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Marco de estrada inválido: " + s);
            }
        }
        for (String s : y.getStringList("trechos")) {
            String[] p = s.split("\\|");
            if (p.length < 5) continue;
            try {
                List<int[]> pts = new ArrayList<>();
                for (String pt : p[4].split(";")) {
                    if (pt.isBlank()) continue;
                    String[] c = pt.split(",");
                    pts.add(new int[]{Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])});
                }
                if (marcos.containsKey(p[0]) && marcos.containsKey(p[1])) {
                    trechos.add(new Trecho(p[0], p[1], pts, Integer.parseInt(p[2]), Integer.parseInt(p[3])));
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Trecho de estrada inválido em estradas.yml.");
            }
        }
        plugin.getLogger().info(marcos.size() + " marco(s) e " + trechos.size() + " trecho(s) de estrada carregado(s).");
    }

    public void salvar() {
        versao++;
        YamlConfiguration y = new YamlConfiguration();
        List<String> ms = new ArrayList<>();
        for (Marco m : marcos.values()) ms.add(String.join("|", m.estrada, m.mundo, "" + m.x, "" + m.y, "" + m.z, "" + m.px, "" + m.py, "" + m.pz));
        List<String> ts = new ArrayList<>();
        for (Trecho t : trechos) {
            StringBuilder pts = new StringBuilder();
            for (int[] p : t.pontos) {
                if (!pts.isEmpty()) pts.append(';');
                pts.append(p[0]).append(',').append(p[1]).append(',').append(p[2]);
            }
            ts.add(t.a + "|" + t.b + "|" + t.luz + "|" + t.comprimento + "|" + pts);
        }
        y.set("marcos", ms);
        y.set("trechos", ts);
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar estradas.yml: " + ex.getMessage());
        }
    }
}
