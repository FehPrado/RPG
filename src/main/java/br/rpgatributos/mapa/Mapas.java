package br.rpgatributos.mapa;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.colonia.Colonia;
import br.rpgatributos.estrada.Rede;
import br.rpgatributos.estruturas.Estruturas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapas (2.99), feitos na Mesa do Cartógrafo (e pelo Cartógrafo da colônia):
 * <ul>
 *   <li><b>Mapa da Região</b> (mapa vazio na mão): um mapa do jogo de 1024 blocos centrado na mesa. O
 *       terreno se desenha como no jogo (segurando e andando); por cima, o plugin desenha as estradas com
 *       nome, as colônias (bandeira com o nome), as Pedras de Viagem públicas e as caravanas andando (e o X
 *       vermelho de um assalto).</li>
 *   <li><b>Mural</b> (agachado, 4 ou 9 mapas vazios): 2×2 ou 3×3 mapas de 512 blocos, lado a lado, para
 *       as molduras da parede, com o mesmo desenho por cima.</li>
 *   <li><b>Mapa do Explorador</b> (bússola na mão + 1 mapa vazio no inventário): 2048 blocos que se
 *       preenchem sozinhos por onde o dono anda com ele no inventário (não precisa segurar), com as
 *       estruturas que ele já visitou, as colônias, as estradas e a seta dele.</li>
 *   <li><b>Mapa de Estrutura</b> (só o Cartógrafo da colônia faz): 512 blocos em volta de uma estrutura
 *       que o dono da colônia ainda não visitou, com um X.</li>
 * </ul>
 * O desenho por cima não fica salvo no mapa: os mapas ficam anotados em {@code mapas.yml} (o do
 * Explorador guarda os pixels em {@code mapas/<id>.bin}) e o desenho volta quando o servidor liga.
 */
public final class Mapas {

    public static final TextColor COR = TextColor.color(0xD7B98E);
    private static final Color ESTRADA = new Color(122, 86, 50);
    private static final int RAIO_EXPLORAR = 4;

    public enum Tipo { REGIAO, MURAL, EXPLORADOR, ESTRUTURA }

    static final class Registro {
        final int id;
        final Tipo tipo;
        final String mundo;
        final int cx, cz;
        final UUID dono;
        final int ax, az;
        final String alvo;
        byte[] pixels;
        int versaoPixels;
        boolean sujo;

        Registro(int id, Tipo tipo, String mundo, int cx, int cz, UUID dono, int ax, int az, String alvo) {
            this.id = id;
            this.tipo = tipo;
            this.mundo = mundo;
            this.cx = cx;
            this.cz = cz;
            this.dono = dono;
            this.ax = ax;
            this.az = az;
            this.alvo = alvo;
        }
    }

    private final RPGAtributos plugin;
    private final File arquivo, pasta;
    private final NamespacedKey kMapa;
    private final Map<Integer, Registro> registros = new HashMap<>();

    public Mapas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "mapas.yml");
        this.pasta = new File(plugin.getDataFolder(), "mapas");
        this.kMapa = new NamespacedKey(plugin, "mapa_rpg");
    }

    // =====================================================================
    //  Fazer
    // =====================================================================

    /** Clique na Mesa do Cartógrafo. @return true se foi um dos mapas (senão a mesa faz o de sempre). */
    public boolean aoUsarMesa(Player p, Block mesa, boolean agachado) {
        if (!plugin.settings().mapasAtivados) return false;
        ItemStack mao = p.getInventory().getItemInMainHand();
        Location c = mesa.getLocation().add(0.5, 0, 0.5);
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (mao.getType() == Material.MAP && !mao.hasItemMeta()) {
            if (agachado && mao.getAmount() >= 4) {
                int lado = mao.getAmount() >= 9 ? 3 : 2;
                if (!criativo) mao.setAmount(mao.getAmount() - lado * lado);
                int passo = 512;
                for (int linha = 0; linha < lado; linha++) {
                    for (int col = 0; col < lado; col++) {
                        int x = c.getBlockX() + (int) Math.round((col - (lado - 1) / 2.0) * passo);
                        int z = c.getBlockZ() + (int) Math.round((linha - (lado - 1) / 2.0) * passo);
                        ItemStack item = criar(Tipo.MURAL, c.getWorld(), x, z, MapView.Scale.NORMAL, null, 0, 0, null);
                        Component nome = Component.text("Mural da Região (" + (linha + 1) + ", " + (col + 1) + ")", COR);
                        item.editMeta(m -> m.itemName(nome));
                        dar(p, item);
                    }
                }
                p.sendMessage(Component.text("🗺 Mural " + lado + "×" + lado + " pronto. Na parede, ponha cada mapa na moldura da linha e coluna do nome (linha 1 em cima, coluna 1 à esquerda, o norte para cima).", COR));
            } else {
                if (!criativo) mao.setAmount(mao.getAmount() - 1);
                dar(p, criar(Tipo.REGIAO, c.getWorld(), c.getBlockX(), c.getBlockZ(), MapView.Scale.FAR, null, 0, 0, null));
                p.sendMessage(Component.text("🗺 Mapa da Região pronto: estradas com nome, colônias, Pedras de Viagem públicas e caravanas aparecem nele sozinhas. O terreno se desenha como no jogo: segure e ande.", COR));
            }
            p.playSound(c, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
            return true;
        }
        if (mao.getType() == Material.COMPASS && !mao.hasItemMeta()) {
            int vazio = p.getInventory().first(Material.MAP);
            if (vazio < 0) {
                p.sendMessage(Component.text("🧭 Para o Mapa do Explorador: bússola na mão e 1 mapa vazio no inventário.", COR));
                return true;
            }
            if (!criativo) {
                mao.setAmount(mao.getAmount() - 1);
                ItemStack m = p.getInventory().getItem(vazio);
                if (m != null) m.setAmount(m.getAmount() - 1);
            }
            dar(p, criar(Tipo.EXPLORADOR, c.getWorld(), c.getBlockX(), c.getBlockZ(), MapView.Scale.FARTHEST, p.getUniqueId(), 0, 0, null));
            p.sendMessage(Component.text("🧭 Mapa do Explorador pronto: 2048 blocos em volta desta mesa. Ele se preenche por onde você andar com ele no inventário, e marca as estruturas que você já visitou.", COR));
            p.playSound(c, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 0.8f);
            return true;
        }
        return false;
    }

    /** O que o Cartógrafo da colônia desenha: um Mapa da Região da colônia, ou um mapa até uma estrutura que o dono não visitou. */
    public ItemStack paraCartografo(Colonia c) {
        Location pref = c.prefeitura();
        if (pref == null) return null;
        if (ThreadLocalRandomHolder.chance(0.4)) {
            Estruturas.Lugar l = plugin.estruturas().naoVisitadaMaisPerto(c.dono(), pref, 1500);
            if (l != null) {
                return criar(Tipo.ESTRUTURA, pref.getWorld(), l.x(), l.z(), MapView.Scale.NORMAL, null, l.x(), l.z(), l.nome());
            }
        }
        return criar(Tipo.REGIAO, pref.getWorld(), pref.getBlockX(), pref.getBlockZ(), MapView.Scale.FAR, null, 0, 0, null);
    }

    private static final class ThreadLocalRandomHolder {
        static boolean chance(double c) { return java.util.concurrent.ThreadLocalRandom.current().nextDouble() < c; }
    }

    public ItemStack criar(Tipo tipo, World w, int cx, int cz, MapView.Scale escala, UUID dono, int ax, int az, String alvo) {
        MapView v = Bukkit.createMap(w);
        v.setScale(escala);
        v.setCenterX(cx);
        v.setCenterZ(cz);
        v.setTrackingPosition(tipo != Tipo.EXPLORADOR);
        v.setUnlimitedTracking(false);
        Registro r = new Registro(v.getId(), tipo, w.getName(), cx, cz, dono, ax, az, alvo);
        if (tipo == Tipo.EXPLORADOR) r.pixels = new byte[128 * 128];
        registros.put(r.id, r);
        preparar(v, r);
        salvar();
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        item.editMeta(MapMeta.class, m -> {
            m.setMapView(v);
            m.itemName(Component.text(switch (tipo) {
                case REGIAO -> "Mapa da Região";
                case MURAL -> "Mural da Região";
                case EXPLORADOR -> "Mapa do Explorador";
                case ESTRUTURA -> "Mapa de " + alvo;
            }, COR));
            List<Component> lore = new ArrayList<>();
            switch (tipo) {
                case REGIAO, MURAL -> lore.add(Component.text("Estradas, colônias e caravanas aparecem sozinhas.", NamedTextColor.GRAY));
                case EXPLORADOR -> lore.add(Component.text("Se preenche por onde você anda (no inventário basta).", NamedTextColor.GRAY));
                case ESTRUTURA -> lore.add(Component.text("Desenhado pelo Cartógrafo: o X marca o lugar.", NamedTextColor.GRAY));
            }
            m.lore(lore.stream().map(x -> x.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(kMapa, PersistentDataType.STRING, tipo.name());
        });
        return item;
    }

    private static void dar(Player p, ItemStack item) {
        p.getInventory().addItem(item).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
    }

    /** Põe o desenho por cima (o Explorador perde o desenho do jogo: ele mesmo pinta o terreno). */
    private void preparar(MapView v, Registro r) {
        if (r.tipo == Tipo.EXPLORADOR) {
            for (MapRenderer x : new ArrayList<>(v.getRenderers())) v.removeRenderer(x);
        }
        v.addRenderer(new Desenho(r));
    }

    // =====================================================================
    //  Desenho
    // =====================================================================

    private final class Desenho extends MapRenderer {
        private final Registro r;
        /** Por jogador (o Explorador tem um desenho para cada um; os outros, um só, redesenhado ao chegar alguém novo). */
        private final Map<UUID, Integer> estradasVistas = new HashMap<>();
        private final Map<UUID, Integer> pixelsVistos = new HashMap<>();
        private long proximosIcones;

        Desenho(Registro r) {
            super(r.tipo == Tipo.EXPLORADOR);
            this.r = r;
        }

        @Override
        public void render(MapView view, MapCanvas canvas, Player p) {
            int escala = 1 << view.getScale().getValue();
            World w = view.getWorld();
            if (w == null) return;
            boolean redesenhar = false;
            if (r.tipo == Tipo.EXPLORADOR) {
                Integer visto = pixelsVistos.get(p.getUniqueId());
                if (visto == null || visto != r.versaoPixels) {
                    for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) canvas.setPixel(x, y, r.pixels[y * 128 + x]);
                    pixelsVistos.put(p.getUniqueId(), r.versaoPixels);
                    redesenhar = true;
                }
            }
            int vr = plugin.rede().versao();
            Integer vistas = estradasVistas.get(p.getUniqueId());
            if (redesenhar || vistas == null || vistas != vr) {
                if (r.tipo != Tipo.EXPLORADOR) for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) canvas.setPixel(x, y, MapPalette.TRANSPARENT);
                for (List<int[]> pts : plugin.rede().trechosDo(w)) {
                    for (int i = 0; i + 1 < pts.size(); i++) {
                        linha(canvas, px(pts.get(i)[0], view.getCenterX(), escala), px(pts.get(i)[2], view.getCenterZ(), escala),
                                px(pts.get(i + 1)[0], view.getCenterX(), escala), px(pts.get(i + 1)[2], view.getCenterZ(), escala));
                    }
                }
                estradasVistas.put(p.getUniqueId(), vr);
            }
            long agora = System.currentTimeMillis();
            if (agora < proximosIcones && r.tipo != Tipo.EXPLORADOR) return;
            proximosIcones = agora + 500;
            MapCursorCollection cur = new MapCursorCollection();
            for (Colonia c : plugin.colonias().todas()) {
                Location pref = c.prefeitura();
                if (pref != null && pref.getWorld() == w) {
                    icone(cur, view, escala, pref.getBlockX(), pref.getBlockZ(), MapCursor.Type.BANNER_WHITE, 8, "Colônia de " + c.nomeDono());
                }
            }
            for (var pd : plugin.pedras().publicas()) {
                Location l = pd.local();
                if (l != null && l.getWorld() == w) icone(cur, view, escala, l.getBlockX(), l.getBlockZ(), MapCursor.Type.TARGET_POINT, 8, null);
            }
            for (Rede.NaEstrada cv : plugin.rede().caravanasEm(w)) {
                icone(cur, view, escala, cv.onde().getBlockX(), cv.onde().getBlockZ(), cv.atacada() ? MapCursor.Type.RED_X : MapCursor.Type.BLUE_MARKER, 8, null);
            }
            if (r.tipo == Tipo.ESTRUTURA) icone(cur, view, escala, r.ax, r.az, MapCursor.Type.TARGET_X, 8, r.alvo);
            if (r.tipo == Tipo.EXPLORADOR) {
                for (Estruturas.Lugar l : plugin.estruturas().visitadas(p.getUniqueId())) {
                    if (l.mundo().equals(w.getName())) icone(cur, view, escala, l.x(), l.z(), MapCursor.Type.RED_MARKER, 8, null);
                }
                if (p.getWorld() == w) {
                    float yaw = p.getLocation().getYaw();
                    double d = yaw < 0 ? yaw - 8 : yaw + 8;
                    icone(cur, view, escala, p.getLocation().getBlockX(), p.getLocation().getBlockZ(), MapCursor.Type.PLAYER, (int) (d * 16.0 / 360.0) & 15, null);
                }
            }
            canvas.setCursors(cur);
        }

        private void linha(MapCanvas canvas, int x0, int y0, int x1, int y1) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            for (int n = 0; n < 512; n++) {
                if (x0 >= 0 && x0 < 128 && y0 >= 0 && y0 < 128) canvas.setPixelColor(x0, y0, ESTRADA);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }
    }

    private static int px(int mundo, int centro, int escala) {
        return Math.floorDiv(mundo - centro, escala) + 64;
    }

    private static void icone(MapCursorCollection cur, MapView v, int escala, int x, int z, MapCursor.Type tipo, int direcao, String legenda) {
        double mx = (x - v.getCenterX()) / (double) escala * 2, mz = (z - v.getCenterZ()) / (double) escala * 2;
        if (mx < -128 || mx > 127 || mz < -128 || mz > 127) return;
        MapCursor c = legenda == null
                ? new MapCursor((byte) Math.round(mx), (byte) Math.round(mz), (byte) (direcao & 15), tipo, true)
                : new MapCursor((byte) Math.round(mx), (byte) Math.round(mz), (byte) (direcao & 15), tipo, true, Component.text(legenda));
        cur.addCursor(c);
    }

    // =====================================================================
    //  Explorador: pinta por onde o dono anda
    // =====================================================================

    /** A cada 2 s: quem leva um Mapa do Explorador no inventário pinta o terreno em volta (4 pixels = 64 blocos). */
    public void tick() {
        if (!plugin.settings().mapasAtivados) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null || it.getType() != Material.FILLED_MAP || !(it.getItemMeta() instanceof MapMeta mm) || !mm.hasMapView()) continue;
                MapView v = mm.getMapView();
                Registro r = v == null ? null : registros.get(v.getId());
                if (r == null || r.tipo != Tipo.EXPLORADOR || !r.mundo.equals(p.getWorld().getName())) continue;
                pintar(r, v, p);
            }
        }
    }

    private void pintar(Registro r, MapView v, Player p) {
        int escala = 1 << v.getScale().getValue();
        World w = p.getWorld();
        int pxp = px(p.getLocation().getBlockX(), r.cx, escala), pzp = px(p.getLocation().getBlockZ(), r.cz, escala);
        boolean mudou = false;
        for (int dz = -RAIO_EXPLORAR; dz <= RAIO_EXPLORAR; dz++) {
            for (int dx = -RAIO_EXPLORAR; dx <= RAIO_EXPLORAR; dx++) {
                if (dx * dx + dz * dz > RAIO_EXPLORAR * RAIO_EXPLORAR) continue;
                int x = pxp + dx, z = pzp + dz;
                if (x < 0 || x >= 128 || z < 0 || z >= 128 || r.pixels[z * 128 + x] != 0) continue;
                int bx = r.cx + (x - 64) * escala + escala / 2, bz = r.cz + (z - 64) * escala + escala / 2;
                if (!w.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                Block topo = w.getHighestBlockAt(bx, bz);
                org.bukkit.Color cor = topo.getBlockData().getMapColor();
                byte b = MapPalette.matchColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue()));
                r.pixels[z * 128 + x] = b == 0 ? MapPalette.matchColor(new Color(130, 120, 100)) : b;
                mudou = true;
            }
        }
        if (mudou) {
            r.versaoPixels++;
            r.sujo = true;
        }
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        registros.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        int ok = 0;
        for (String s : y.getStringList("mapas")) {
            String[] p = s.split("\\|", -1);
            if (p.length < 9) continue;
            try {
                Registro r = new Registro(Integer.parseInt(p[0]), Tipo.valueOf(p[1]), p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                        p[5].isEmpty() ? null : UUID.fromString(p[5]), Integer.parseInt(p[6]), Integer.parseInt(p[7]), p[8].isEmpty() ? null : p[8]);
                MapView v = Bukkit.getMap(r.id);
                if (v == null) continue;
                if (r.tipo == Tipo.EXPLORADOR) {
                    File f = new File(pasta, r.id + ".bin");
                    r.pixels = f.exists() ? Files.readAllBytes(f.toPath()) : new byte[128 * 128];
                    if (r.pixels.length != 128 * 128) r.pixels = new byte[128 * 128];
                }
                registros.put(r.id, r);
                preparar(v, r);
                ok++;
            } catch (IOException | RuntimeException ex) {
                plugin.getLogger().warning("Mapa inválido em mapas.yml: " + s);
            }
        }
        plugin.getLogger().info(ok + " mapa(s) da região/explorador carregado(s).");
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (Registro r : registros.values()) {
            l.add(r.id + "|" + r.tipo.name() + "|" + r.mundo + "|" + r.cx + "|" + r.cz + "|" + (r.dono == null ? "" : r.dono)
                    + "|" + r.ax + "|" + r.az + "|" + (r.alvo == null ? "" : r.alvo.replace("|", "/")));
        }
        y.set("mapas", l);
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar mapas.yml: " + ex.getMessage());
        }
        salvarPixels();
    }

    /** Os pixels dos Mapas do Explorador que mudaram. */
    public void salvarPixels() {
        for (Registro r : registros.values()) {
            if (r.tipo != Tipo.EXPLORADOR || !r.sujo || r.pixels == null) continue;
            try {
                if (!pasta.exists()) pasta.mkdirs();
                Files.write(new File(pasta, r.id + ".bin").toPath(), r.pixels);
                r.sujo = false;
            } catch (IOException ex) {
                plugin.getLogger().warning("Não consegui salvar o mapa " + r.id + ": " + ex.getMessage());
            }
        }
    }

    public int quantos() { return registros.size(); }
}
