package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O visual dos companheiros, feito só com o pacote de recursos:
 * - armaduras próprias em cavalos e lobos (Couraça, Pele de Netherite, Chamas, Coração do Mar
 *   e o visual do estágio III), num item que só muda a aparência (não dá defesa nem se gasta);
 * - peças 3D que acompanham o bicho: sela, alforjes, lanterna do Farol, asas que batem quando ele
 *   voa e uma auréola no estágio III;
 * - partículas próprias de cada criatura no estágio III.
 */
public final class VisualDomador implements Listener {

    public static final NamespacedKey K_VISUAL = new NamespacedKey("rpgatributos", "visual_domador");

    /** As armaduras desenhadas (uma textura para o cavalo, 64x64, e outra para o lobo, 64x32). */
    enum Armadura {
        COURACA, NETHERITE, CHAMAS, MARE, CELESTE, SOMBRAS;

        String id() { return "domador_" + name().toLowerCase(Locale.ROOT); }
    }

    /** As peças 3D (modelos do pacote) que seguem o companheiro. */
    enum Peca {
        SELA("domador_sela"), ALFORJE("domador_alforje"), FAROL(null), ASAS("domador_asas"), AUREOLA("domador_aureola");

        final String modelo;

        Peca(String modelo) { this.modelo = modelo; }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Map<Peca, UUID>> pecas = new HashMap<>();
    private int ciclo;

    public VisualDomador(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Companheiros comp() { return plugin.companheiros(); }

    // =====================================================================
    //  A cada tick: as peças seguem o bicho; a cada 2 s: armadura e partículas
    // =====================================================================

    public void tick() {
        ciclo++;
        Set<UUID> ativos = comp().ativos();
        // Peças de quem sumiu, morreu ou deixou de ser companheiro.
        for (var it = pecas.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (ativos.contains(en.getKey()) && Bukkit.getEntity(en.getKey()) instanceof LivingEntity le && le.isValid()) continue;
            for (UUID d : en.getValue().values()) {
                Entity x = Bukkit.getEntity(d);
                if (x != null) x.remove();
            }
            it.remove();
        }
        for (UUID id : List.copyOf(ativos)) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity c) || !c.isValid()) continue;
            Set<Componente> comps = comp().componentes(c);
            int est = plugin.settings().evoAtivada ? Evolucao.estagio(c) : 1;
            EnumMap<Peca, Boolean> quer = new EnumMap<>(Peca.class);
            quer.put(Peca.SELA, comps.contains(Componente.SELA) && !temSelaDoJogo(c));
            quer.put(Peca.ALFORJE, comps.contains(Componente.ALFORJE));
            quer.put(Peca.FAROL, comps.contains(Componente.FAROL));
            quer.put(Peca.ASAS, comps.contains(Componente.ASAS));
            quer.put(Peca.AUREOLA, est >= 3);
            Map<Peca, UUID> minhas = pecas.computeIfAbsent(id, k -> new EnumMap<>(Peca.class));
            for (Peca p : Peca.values()) {
                UUID did = minhas.get(p);
                ItemDisplay d = did != null && Bukkit.getEntity(did) instanceof ItemDisplay x && x.isValid() ? x : null;
                if (!quer.get(p)) {
                    if (d != null) d.remove();
                    minhas.remove(p);
                    continue;
                }
                if (d == null) {
                    d = criar(c, p);
                    minhas.put(p, d.getUniqueId());
                }
                posicionar(c, p, d);
            }
            if (ciclo % 40 == 0) {
                armadura(c, comps, est);
                particulas(c, comps, est);
            }
        }
    }

    /** Sela de verdade do jogo (cavalo, porco, camelo...) já aparece: não precisa da nossa. */
    private static boolean temSelaDoJogo(LivingEntity c) {
        EntityEquipment eq = c.getEquipment();
        return eq != null && c.canUseEquipmentSlot(EquipmentSlot.SADDLE) && !eq.getItem(EquipmentSlot.SADDLE).isEmpty();
    }

    private static ItemStack itemDaPeca(Peca p, boolean asasBaixas) {
        if (p == Peca.FAROL) return new ItemStack(Material.LANTERN);
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> m.setItemModel(new NamespacedKey("rpgatributos", p == Peca.ASAS && asasBaixas ? "domador_asas_baixas" : p.modelo)));
        return i;
    }

    private ItemDisplay criar(LivingEntity c, Peca p) {
        return c.getWorld().spawn(c.getLocation(), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(itemDaPeca(p, false));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTeleportDuration(2);
            d.setInterpolationDuration(4);
            d.setViewRange(0.6f);
            d.getPersistentDataContainer().set(K_VISUAL, PersistentDataType.STRING, "peca");
            if (p == Peca.AUREOLA || p == Peca.FAROL) d.setBrightness(new Display.Brightness(15, 15));
        });
    }

    /** Onde cada peça fica, conforme o tamanho do bicho (já com o tamanho do estágio). */
    private void posicionar(LivingEntity c, Peca p, ItemDisplay d) {
        double h = c.getHeight(), w = c.getWidth();
        double y;
        float s;
        float yaw = c.getBodyYaw();
        switch (p) {
            case SELA -> { y = h * 0.86; s = (float) (w * 0.85); }
            case ALFORJE -> { y = h * 0.6; s = (float) (w * 0.9); }
            case FAROL -> { y = h * 1.02; s = 0.45f; }
            case ASAS -> { y = h * 0.82; s = (float) (w * 1.25); }
            default -> { y = h + 0.3; s = (float) (w * 0.75); yaw += (ciclo * 3) % 360; } // a auréola gira devagar
        }
        Location l = c.getLocation().add(0, y, 0);
        l.setYaw(yaw);
        l.setPitch(0);
        d.teleport(l);
        Transformation t = d.getTransformation();
        if (Math.abs(t.getScale().x - s) > 0.01) {
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(s, s, s), new AxisAngle4f()));
        }
        // Asas: batem quando ele está no ar (montado e voando) ou correndo.
        if (p == Peca.ASAS && ciclo % 4 == 0) {
            boolean noAr = !c.isOnGround();
            boolean baixas = noAr && (ciclo / 4) % 2 == 0;
            d.setItemStack(itemDaPeca(p, baixas));
        }
    }

    // =====================================================================
    //  Armaduras (cavalo e lobo)
    // =====================================================================

    /** A armadura que aparece: a do componente mais forte, senão a do estágio III. */
    static Armadura armaduraDe(EntityType t, Set<Componente> comps, int est) {
        if (comps.contains(Componente.NETHERITE)) return Armadura.NETHERITE;
        if (comps.contains(Componente.CHAMAS)) return Armadura.CHAMAS;
        if (comps.contains(Componente.MARE)) return Armadura.MARE;
        if (comps.contains(Componente.COURACA)) return Armadura.COURACA;
        if (est >= 3) return t == EntityType.WOLF ? Armadura.SOMBRAS : Armadura.CELESTE;
        return null;
    }

    private void armadura(LivingEntity c, Set<Componente> comps, int est) {
        if (c.getType() != EntityType.HORSE && c.getType() != EntityType.WOLF) return;
        EntityEquipment eq = c.getEquipment();
        if (eq == null) return;
        ItemStack atual = eq.getItem(EquipmentSlot.BODY);
        String nossa = atual.isEmpty() ? null : atual.getPersistentDataContainer().get(K_VISUAL, PersistentDataType.STRING);
        if (!atual.isEmpty() && nossa == null) return; // armadura de verdade que o jogador pôs: fica
        Armadura a = armaduraDe(c.getType(), comps, est);
        if (a == null) {
            if (nossa != null) eq.setItem(EquipmentSlot.BODY, null);
            return;
        }
        if (a.id().equals(nossa)) return;
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            EquippableComponent e = m.getEquippable();
            e.setSlot(EquipmentSlot.BODY);
            e.setModel(new NamespacedKey("rpgatributos", a.id()));
            e.setDispensable(false);
            e.setSwappable(false);
            m.setEquippable(e);
            m.getPersistentDataContainer().set(K_VISUAL, PersistentDataType.STRING, a.id());
        });
        eq.setItem(EquipmentSlot.BODY, i);
        eq.setDropChance(EquipmentSlot.BODY, 0f);
    }

    // =====================================================================
    //  Partículas
    // =====================================================================

    private static final Map<EntityType, Particle> ESTAGIO3 = Map.ofEntries(
            Map.entry(EntityType.WOLF, Particle.SMOKE),
            Map.entry(EntityType.CAT, Particle.WITCH),
            Map.entry(EntityType.HORSE, Particle.END_ROD),
            Map.entry(EntityType.FOX, Particle.SOUL_FIRE_FLAME),
            Map.entry(EntityType.POLAR_BEAR, Particle.SNOWFLAKE),
            Map.entry(EntityType.PARROT, Particle.ELECTRIC_SPARK),
            Map.entry(EntityType.STRIDER, Particle.FLAME),
            Map.entry(EntityType.MOOSHROOM, Particle.SPORE_BLOSSOM_AIR),
            Map.entry(EntityType.CAMEL, Particle.WHITE_ASH),
            Map.entry(EntityType.IRON_GOLEM, Particle.CRIT));

    private void particulas(LivingEntity c, Set<Componente> comps, int est) {
        Location meio = c.getLocation().add(0, c.getHeight() * 0.6, 0);
        if (comps.contains(Componente.CHAMAS)) c.getWorld().spawnParticle(Particle.FLAME, meio, 4, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0.01);
        if (comps.contains(Componente.MARE)) c.getWorld().spawnParticle(Particle.DRIPPING_WATER, meio, 4, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0);
        if (est == 2) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 3, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xD8DCE6), 0.9f));
        } else if (est >= 3) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 3, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xFFC93C), 1.1f));
            Particle propria = ESTAGIO3.get(c.getType());
            if (propria != null) c.getWorld().spawnParticle(propria, meio, 3, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0.01);
        }
    }

    // =====================================================================
    //  Ninguém leva os itens de visual
    // =====================================================================

    private static boolean visual(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_VISUAL);
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        e.getDrops().removeIf(VisualDomador::visual);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoClicar(InventoryClickEvent e) {
        if (visual(e.getCurrentItem()) || visual(e.getCursor())) {
            e.setCancelled(true);
            if (e.getClickedInventory() == e.getWhoClicked().getInventory() && visual(e.getCurrentItem())) e.setCurrentItem(null);
        }
    }

    /** A tesoura tira a armadura do lobo: a de visual não sai. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoTosquiar(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity le) || le.getEquipment() == null) return;
        ItemStack mao = e.getPlayer().getInventory().getItem(e.getHand());
        if (mao.getType() == Material.SHEARS && visual(le.getEquipment().getItem(EquipmentSlot.BODY))) e.setCancelled(true);
    }

    /** Ao desligar: as peças somem (elas não são salvas no mundo). */
    public void parar() {
        for (Map<Peca, UUID> m : pecas.values()) {
            for (UUID d : m.values()) {
                Entity x = Bukkit.getEntity(d);
                if (x != null) x.remove();
            }
        }
        pecas.clear();
    }

    // =====================================================================
    //  Pacote de recursos: armaduras, modelos 3D e as texturas deles
    // =====================================================================

    /** Arquivos que o PacoteRecursos põe no zip. */
    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new HashMap<>();
        boolean[][] cavalo = mascara("/mascaras/cavalo.txt"), lobo = mascara("/mascaras/lobo.txt");
        for (Armadura a : Armadura.values()) {
            f.put("assets/rpgatributos/equipment/" + a.id() + ".json", texto("""
                    { "layers": {
                        "horse_body": [ { "texture": "rpgatributos:%1$s" } ],
                        "wolf_body": [ { "texture": "rpgatributos:%1$s" } ]
                    } }
                    """.formatted(a.id())));
            f.put("assets/rpgatributos/textures/entity/equipment/horse_body/" + a.id() + ".png", png(pintar(a, cavalo)));
            f.put("assets/rpgatributos/textures/entity/equipment/wolf_body/" + a.id() + ".png", png(pintar(a, lobo)));
        }
        modelo(f, "domador_sela", SELA_JSON, textura(0xFF5D3A1E, 0xFF7A4E2A, 0xFF3A2412, 0xFFC9A227));
        modelo(f, "domador_alforje", ALFORJE_JSON, textura(0xFF6B4A2B, 0xFF8D6440, 0xFF3E2A18, 0xFFB0B0B0));
        modelo(f, "domador_asas", asas(22.5f), penas());
        modelo(f, "domador_asas_baixas", asas(-22.5f), null);
        modelo(f, "domador_aureola", AUREOLA_JSON, textura(0xFFFFD54F, 0xFFFFF59D, 0xFFE0A800, 0xFFFFFFFF));
        return f;
    }

    private static void modelo(Map<String, byte[]> f, String id, String json, BufferedImage tex) throws IOException {
        f.put("assets/rpgatributos/items/" + id + ".json",
                texto("{ \"model\": { \"type\": \"minecraft:model\", \"model\": \"rpgatributos:item/" + id + "\" } }"));
        f.put("assets/rpgatributos/models/item/" + id + ".json", texto(json));
        if (tex != null) f.put("assets/rpgatributos/textures/item/" + id + ".png", png(tex));
    }

    private static boolean[][] mascara(String recurso) throws IOException {
        try (InputStream in = VisualDomador.class.getResourceAsStream(recurso)) {
            if (in == null) throw new IOException("faltou " + recurso);
            List<String> l = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String s; (s = r.readLine()) != null; ) if (!s.isBlank()) l.add(s.trim());
            }
            boolean[][] m = new boolean[l.size()][l.get(0).length()];
            for (int y = 0; y < m.length; y++) for (int x = 0; x < m[0].length; x++) m[y][x] = l.get(y).charAt(x) == '#';
            return m;
        }
    }

    private static double hash(int x, int y, int s) {
        long h = x * 0x9E3779B97F4A7C15L + y * 0xC2B2AE3D27D4EB4FL + s * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) / (double) (1L << 53);
    }

    /** Pinta a armadura: cor do padrão em cada pixel da máscara, borda escura e brilho em cima. */
    private static BufferedImage pintar(Armadura a, boolean[][] m) {
        int h = m.length, w = m[0].length;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!m[y][x]) continue;
                boolean borda = !dentro(m, x - 1, y) || !dentro(m, x + 1, y) || !dentro(m, x, y - 1) || !dentro(m, x, y + 1);
                boolean topo = !dentro(m, x, y - 1);
                double r = hash(x, y, a.ordinal());
                int cor = switch (a) {
                    case COURACA -> borda ? (topo ? 0xFFD0DAE6 : 0xFF4E5C6E) : (x % 4 == 1 && y % 4 == 1) ? 0xFFC9A227
                            : r < 0.3 ? 0xFF9AAAC0 : 0xFF7F90A6;
                    case NETHERITE -> borda ? (topo ? 0xFF6A5F68 : 0xFF151215) : r < 0.05 ? 0xFF9B59D0 : r < 0.4 ? 0xFF3E363C : 0xFF2E282D;
                    case CHAMAS -> {
                        double chama = (Math.sin(x * 0.9 + y * 0.35) + 1) / 2 + r * 0.4;
                        yield borda ? 0xFF4A0E06 : chama > 1.05 ? 0xFFFFD54F : chama > 0.7 ? 0xFFF57C00 : 0xFFB3260E;
                    }
                    case MARE -> {
                        boolean onda = ((x + (int) Math.round(Math.sin(y * 0.8) * 2)) % 6 + 6) % 6 < 2;
                        yield borda ? (topo ? 0xFFB2F5EA : 0xFF0D4D52) : onda ? 0xFF6FE0D2 : r < 0.3 ? 0xFF2A9D9A : 0xFF1F7F86;
                    }
                    case CELESTE -> borda ? 0xFFE2B53C : r < 0.06 ? 0xFFAEE3FF : r < 0.35 ? 0xFFFFFFFF : 0xFFE8ECF6;
                    case SOMBRAS -> borda ? 0xFF120B1C : r < 0.08 ? 0xFF9575CD : r < 0.4 ? 0xFF3A2952 : 0xFF2A1F3A;
                };
                im.setRGB(x, y, cor);
            }
        }
        return im;
    }

    private static boolean dentro(boolean[][] m, int x, int y) {
        return y >= 0 && y < m.length && x >= 0 && x < m[0].length && m[y][x];
    }

    /** Textura 16x16 lisa com costura (couro, ouro...): cor, claro, escuro e o detalhe. */
    private static BufferedImage textura(int cor, int claro, int escuro, int detalhe) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double r = hash(x, y, cor);
                int c = (x == 0 || y == 0) ? claro : (x == 15 || y == 15) ? escuro : (x % 5 == 2 && y % 5 == 2) ? detalhe : r < 0.25 ? claro : cor;
                im.setRGB(x, y, c);
            }
        }
        return im;
    }

    /** Penas brancas com o cálamo cinza. */
    private static BufferedImage penas() {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean calamo = (x + y / 2) % 4 == 0;
                im.setRGB(x, y, calamo ? 0xFFB8BFC9 : (y > 11 ? 0xFFD9DEE6 : 0xFFF5F7FA));
            }
        }
        return im;
    }

    private static final String SELA_JSON = """
            { "textures": { "0": "rpgatributos:item/domador_sela", "particle": "rpgatributos:item/domador_sela" },
              "elements": [
                { "from": [3, 0, 3], "to": [13, 2, 13], "faces": %s },
                { "from": [3, 2, 10], "to": [13, 4, 13], "faces": %s },
                { "from": [7, 2, 3], "to": [9, 4, 5], "faces": %s },
                { "from": [2, -4, 7], "to": [3, 1, 9], "faces": %s },
                { "from": [13, -4, 7], "to": [14, 1, 9], "faces": %s }
              ] }
            """.formatted(FACES("0"), FACES("0"), FACES("0"), FACES("0"), FACES("0"));

    private static final String ALFORJE_JSON = """
            { "textures": { "0": "rpgatributos:item/domador_alforje", "particle": "rpgatributos:item/domador_alforje" },
              "elements": [
                { "from": [-3, 0, 4], "to": [2, 7, 12], "faces": %s },
                { "from": [14, 0, 4], "to": [19, 7, 12], "faces": %s },
                { "from": [2, 6, 7], "to": [14, 7, 9], "faces": %s }
              ] }
            """.formatted(FACES("0"), FACES("0"), FACES("0"));

    private static final String AUREOLA_JSON = """
            { "textures": { "0": "rpgatributos:item/domador_aureola", "particle": "rpgatributos:item/domador_aureola" },
              "elements": [
                { "from": [3, 0, 3], "to": [13, 1, 4], "light_emission": 15, "faces": %s },
                { "from": [3, 0, 12], "to": [13, 1, 13], "light_emission": 15, "faces": %s },
                { "from": [3, 0, 4], "to": [4, 1, 12], "light_emission": 15, "faces": %s },
                { "from": [12, 0, 4], "to": [13, 1, 12], "light_emission": 15, "faces": %s }
              ] }
            """.formatted(FACES("0"), FACES("0"), FACES("0"), FACES("0"));

    /** Asas abertas: duas placas de penas, com as pontas para cima ({@code angulo} > 0) ou para baixo. */
    private static String asas(float angulo) {
        return """
                { "textures": { "0": "rpgatributos:item/domador_asas", "particle": "rpgatributos:item/domador_asas" },
                  "elements": [
                    { "from": [-12, 8, 4], "to": [7, 9, 12], "rotation": { "angle": %1$s, "axis": "z", "origin": [7, 8, 8] }, "faces": %2$s },
                    { "from": [9, 8, 4], "to": [28, 9, 12], "rotation": { "angle": %3$s, "axis": "z", "origin": [9, 8, 8] }, "faces": %2$s }
                  ] }
                """.formatted(-angulo, FACES("0"), angulo);
    }

    private static String FACES(String t) {
        StringBuilder sb = new StringBuilder("{");
        String[] lados = {"north", "east", "south", "west", "up", "down"};
        for (int i = 0; i < lados.length; i++) {
            sb.append(i == 0 ? "" : ", ").append('"').append(lados[i]).append("\": { \"uv\": [0, 0, 16, 16], \"texture\": \"#").append(t).append("\" }");
        }
        return sb.append('}').toString();
    }

    private static byte[] texto(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] png(BufferedImage im) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(im, "png", b);
        return b.toByteArray();
    }
}
