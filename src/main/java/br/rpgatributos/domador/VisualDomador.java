package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
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

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        COURACA, NETHERITE, CHAMAS, MARE, CELESTE, SOMBRAS,
        /** Peles das lendárias: cobrem o cavalo inteiro (cabeça, crina, cauda e cascos). */
        QILIN, FENGHUANG, LONGMA, BAIHU, PIXIU, BAIZE;

        boolean pele() { return ordinal() >= QILIN.ordinal(); }

        String id() { return "domador_" + name().toLowerCase(Locale.ROOT); }
    }

    private final RPGAtributos plugin;

    public VisualDomador(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Companheiros comp() { return plugin.companheiros(); }

    // =====================================================================
    //  A cada 2 s: armadura e partículas (sem peças 3D seguindo o bicho: pesavam no jogo)
    // =====================================================================

    public void tick() {
        for (UUID id : List.copyOf(comp().ativos())) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity c) || !c.isValid()) continue;
            Set<Componente> comps = comp().componentes(c);
            int est = plugin.settings().evoAtivada ? Evolucao.estagio(c) : 1;
            armadura(c, comps, est);
            particulas(c, comps, est);
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
        Lendaria lend = Lendaria.de(c);
        Armadura a = lend != null ? lend.pele() : armaduraDe(c.getType(), comps, est);
        if (a == null) {
            if (nossa != null) eq.setItem(EquipmentSlot.BODY, null);
            return;
        }
        if (a.id().equals(nossa)) return;
        vestir(c, a);
    }

    /** Põe a armadura (ou pele) só de visual no corpo do cavalo ou do lobo. */
    static void vestir(LivingEntity c, Armadura a) {
        EntityEquipment eq = c.getEquipment();
        if (eq == null) return;
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

    static void particulas(LivingEntity c, Set<Componente> comps, int est) {
        Location meio = c.getLocation().add(0, c.getHeight() * 0.6, 0);
        Lendaria lend = Lendaria.de(c);
        if (lend == Lendaria.QILIN) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0x7FD8A6), 1.2f));
            c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, meio, 2, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0);
            return;
        }
        if (lend == Lendaria.LONGMA) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0x4FA3E0), 1.2f));
            c.getWorld().spawnParticle(Particle.DRIPPING_WATER, meio, 3, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0);
            return;
        }
        if (lend == Lendaria.BAIHU) {
            c.getWorld().spawnParticle(Particle.SNOWFLAKE, meio, 3, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0.01);
            c.getWorld().spawnParticle(Particle.DUST, meio, 3, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xFFD54F), 1f));
            return;
        }
        if (lend == Lendaria.XUANWU) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 6, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0x2E3B2E), 1.6f));
            c.getWorld().spawnParticle(Particle.SQUID_INK, meio, 1, c.getWidth() * 0.3, 0.2, c.getWidth() * 0.3, 0.01);
            return;
        }
        if (lend == Lendaria.FENGHUANG) {
            c.getWorld().spawnParticle(Particle.FLAME, meio, 5, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0.01);
            c.getWorld().spawnParticle(Particle.DUST, meio, 3, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xFFC107), 1.2f));
            return;
        }
        if (lend == Lendaria.PIXIU) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xFFD54F), 1.1f));
            c.getWorld().spawnParticle(Particle.WAX_ON, meio, 1, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0);
            return;
        }
        if (lend == Lendaria.BAIZE) {
            c.getWorld().spawnParticle(Particle.DUST, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0xBFE3FF), 1.2f));
            c.getWorld().spawnParticle(Particle.ENCHANT, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0.3);
            return;
        }
        if (lend == Lendaria.KUNPENG) {
            c.getWorld().spawnParticle(Particle.CLOUD, meio, 3, c.getWidth() * 0.4, 0.3, c.getWidth() * 0.4, 0.01);
            c.getWorld().spawnParticle(Particle.DUST, meio, 4, c.getWidth() * 0.4, 0.4, c.getWidth() * 0.4, 0, new Particle.DustOptions(Color.fromRGB(0x5FB8E6), 1.6f));
            return;
        }
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

    // =====================================================================
    //  Pacote de recursos: armaduras, modelos 3D e as texturas deles
    // =====================================================================

    /** Arquivos que o PacoteRecursos põe no zip. */
    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new HashMap<>();
        boolean[][] cavalo = mascara("/mascaras/cavalo.txt"), lobo = mascara("/mascaras/lobo.txt");
        char[][] pele = mapa("/mascaras/cavalo_pele.txt"), peleLobo = mapa("/mascaras/lobo_pele.txt");
        for (Armadura a : Armadura.values()) {
            f.put("assets/rpgatributos/equipment/" + a.id() + ".json", texto("""
                    { "layers": {
                        "horse_body": [ { "texture": "rpgatributos:%1$s" } ],
                        "wolf_body": [ { "texture": "rpgatributos:%1$s" } ]
                    } }
                    """.formatted(a.id())));
            f.put("assets/rpgatributos/textures/entity/equipment/horse_body/" + a.id() + ".png", png(a.pele() ? pintarPele(a, pele, false) : pintar(a, cavalo)));
            f.put("assets/rpgatributos/textures/entity/equipment/wolf_body/" + a.id() + ".png", png(a.pele() ? pintarPele(a, peleLobo, true) : pintar(a, lobo)));
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
                    case QILIN -> borda ? 0xFF1F6B4A : r < 0.06 ? 0xFFE2B53C : r < 0.4 ? 0xFF8FE3B8 : 0xFF4FB286;
                    case FENGHUANG -> borda ? 0xFF7A1408 : r < 0.06 ? 0xFFFFEB3B : r < 0.4 ? 0xFFFF7043 : 0xFFD84315;
                    case LONGMA -> borda ? 0xFF0D3B66 : r < 0.4 ? 0xFF64B5F6 : 0xFF1E88E5;
                    case BAIHU -> borda ? 0xFF1B1B1B : r < 0.4 ? 0xFFFFFFFF : 0xFFE8E8E8;
                    case PIXIU -> borda ? 0xFF6D4C00 : r < 0.06 ? 0xFFD32F2F : r < 0.4 ? 0xFFFFE082 : 0xFFE0B84A;
                    case BAIZE -> borda ? 0xFF5C8DB8 : r < 0.06 ? 0xFF7FC8FF : r < 0.4 ? 0xFFFFFFFF : 0xFFEAF4FB;
                };
                im.setRGB(x, y, cor);
            }
        }
        return im;
    }

    /** Mapa da textura do cavalo: '#' pele, 'e' escuro (pupila, narina, casco), 'w' brilho do olho, '.' vazio. */
    static char[][] mapa(String recurso) throws IOException {
        try (InputStream in = VisualDomador.class.getResourceAsStream(recurso)) {
            if (in == null) throw new IOException("faltou " + recurso);
            List<String> l = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (String s; (s = r.readLine()) != null; ) if (!s.isBlank()) l.add(s.trim());
            }
            char[][] m = new char[l.size()][];
            for (int y = 0; y < m.length; y++) m[y] = l.get(y).toCharArray();
            return m;
        }
    }

    /**
     * Pele das lendárias na disposição da textura do cavalo (64x64) ou do lobo (64x32): olhos,
     * focinho, cascos, crina e cauda com cor própria; o corpo em escamas (Qilin, Longma), penas
     * (Fenghuang) ou pelo listrado (Baihu).
     */
    static BufferedImage pintarPele(Armadura a, char[][] m, boolean lobo) {
        int h = m.length, w = m[0].length;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char ch = m[y][x];
                if (ch == '.') continue;
                double r = hash(x, y, a.ordinal() + 7);
                Parte p = lobo ? parteDoLobo(x, y, ch) : parteDoCavalo(x, y, ch);
                im.setRGB(x, y, corDaPele(a, p, x, y, r));
            }
        }
        return im;
    }

    private enum Parte { OLHO_CLARO, OLHO_ESCURO, ESCURO, CASCO, CASCO_TOPO, CRINA, CAUDA, PONTA_CAUDA, CABECA, CORPO }

    /** Partes da textura do cavalo (vistas no AbstractEquineModel: cabeça em 0,13, crina em 56,36, cauda em 42,36, pernas em 48,21). */
    private static Parte parteDoCavalo(int x, int y, char ch) {
        boolean olho = y >= 20 && y <= 22 && x < 20;
        if (olho && ch == 'w') return Parte.OLHO_CLARO;
        if (olho && ch == 'e') return Parte.OLHO_ESCURO;
        if (x >= 48 && y >= 33 && y <= 36) return y == 33 ? Parte.CASCO_TOPO : Parte.CASCO;
        if (ch == 'e') return Parte.ESCURO;
        if (x >= 56 && y >= 36 && y < 54) return Parte.CRINA;
        if (x >= 42 && x < 56 && y >= 36 && y < 54) return y >= 50 ? Parte.PONTA_CAUDA : Parte.CAUDA;
        return y < 35 && x < 26 ? Parte.CABECA : Parte.CORPO;
    }

    /** Partes da textura do lobo (cabeça em 0,0, focinho em 0,10, juba em 21,0, corpo em 18,14, pernas em 0,18, cauda em 9,18). */
    private static Parte parteDoLobo(int x, int y, char ch) {
        if (y == 6 && x < 12 && ch == 'w') return Parte.OLHO_CLARO;
        if (y == 6 && x < 12 && ch == 'e') return Parte.OLHO_ESCURO;
        if (ch == 'e') return Parte.ESCURO;
        if (x >= 9 && x < 17 && y >= 18 && y < 28) return y >= 25 ? Parte.PONTA_CAUDA : Parte.CAUDA;
        if (x >= 21 && y < 13) return Parte.CRINA;
        if (x < 8 && y >= 26) return Parte.CASCO;
        return y < 17 && x < 22 ? Parte.CABECA : Parte.CORPO;
    }

    private static int corDaPele(Armadura a, Parte p, int x, int y, double r) {
        // Chama, espuma ou pelo da crina e da cauda: mais claro na ponta.
        double t = Math.min(1, Math.max(0, (y % 18) / 18.0 + r * 0.35));
        return switch (a) {
            case QILIN -> switch (p) {
                case OLHO_CLARO -> 0xFFFFE082;
                case OLHO_ESCURO -> 0xFF10301F;
                case ESCURO -> 0xFF1F4B36;
                case CASCO_TOPO -> 0xFF8D6E1A;
                case CASCO -> r < 0.3 ? 0xFFFFE082 : 0xFFE2B53C;
                case CRINA, CAUDA, PONTA_CAUDA -> t > 0.95 ? 0xFFE8F5C8 : t > 0.6 ? 0xFF9CF0C0 : t > 0.3 ? 0xFF4FB286 : 0xFF2E7D32;
                default -> escama(x, y, r, p == Parte.CABECA, 0xFFE2B53C, 0xFF1F6B4A, 0xFF2E8B60, 0xFF8FE3B8, 0xFF4FB286, 0xFF6CCB9C);
            };
            case FENGHUANG -> switch (p) {
                case OLHO_CLARO -> 0xFFFFF59D;
                case OLHO_ESCURO -> 0xFF3E0A00;
                case ESCURO -> 0xFF5A1206;
                case CASCO_TOPO -> 0xFF8D6E1A;
                case CASCO -> r < 0.3 ? 0xFFFFE082 : 0xFFE2B53C;
                case CRINA, CAUDA, PONTA_CAUDA -> t > 0.95 ? 0xFFFFF59D : t > 0.6 ? 0xFFFFC107 : t > 0.3 ? 0xFFFF7043 : 0xFFC62828;
                default -> {
                    // Penas: cada fileira termina numa ponta escura; dourado no alto, vermelho embaixo.
                    boolean ponta = y % 4 == 3 && (x + y / 4) % 3 != 0;
                    boolean alto = p == Parte.CABECA || y < 44;
                    yield r < 0.05 ? 0xFFFFEB3B : ponta ? 0xFF7A1408 : alto ? (r < 0.4 ? 0xFFFFC107 : 0xFFFF8F00) : (r < 0.4 ? 0xFFFF7043 : 0xFFD84315);
                }
            };
            case LONGMA -> switch (p) {
                case OLHO_CLARO -> 0xFFFFF59D;
                case OLHO_ESCURO -> 0xFF0A1A2A;
                case ESCURO -> 0xFF0D3B66;
                case CASCO_TOPO -> 0xFF607D8B;
                case CASCO -> r < 0.3 ? 0xFFFFFFFF : 0xFFCFD8DC;
                case CRINA, CAUDA, PONTA_CAUDA -> t > 0.95 ? 0xFFFFFFFF : t > 0.6 ? 0xFFE0F7FA : t > 0.3 ? 0xFF80DEEA : 0xFF26C6DA;
                default -> escama(x, y, r, p == Parte.CABECA, 0xFFE3F2FD, 0xFF0D3B66, 0xFF1565C0, 0xFF90CAF9, 0xFF1E88E5, 0xFF42A5F5);
            };
            case BAIHU -> switch (p) {
                case OLHO_CLARO -> 0xFFFFD54F;
                case OLHO_ESCURO -> 0xFF1B1B1B;
                case ESCURO -> 0xFF2B2B2B;
                case PONTA_CAUDA -> 0xFF1B1B1B;
                default -> {
                    // Pelo branco com listras pretas (mais espaçadas na cabeça).
                    int k = x * 2 + y;
                    boolean listra = p == Parte.CABECA ? (x + y * 2) % 9 == 0 : (k % 7 == 0 || k % 7 == 1);
                    yield listra ? 0xFF1E1E1E : r < 0.3 ? 0xFFFFFFFF : r < 0.6 ? 0xFFF2F2F2 : 0xFFE3E7EC;
                }
            };
            case PIXIU -> switch (p) {
                case OLHO_CLARO -> 0xFFFF5252;
                case OLHO_ESCURO -> 0xFF3E2000;
                case ESCURO -> 0xFF6D4C00;
                case CASCO -> 0xFF8E1B1B;
                // Juba e cauda vermelhas, como as fitas do Ano-Novo.
                case CRINA, CAUDA, PONTA_CAUDA -> t > 0.9 ? 0xFFFFCDD2 : t > 0.55 ? 0xFFEF5350 : t > 0.25 ? 0xFFD32F2F : 0xFF8E1B1B;
                default -> {
                    // Escamas de ouro com moedas (anéis) de vez em quando.
                    boolean moeda = (x % 6 == 2 && y % 5 == 2);
                    yield moeda ? 0xFFFFF59D : escama(x, y, r, p == Parte.CABECA, 0xFFFFF8E1, 0xFF8D6E00, 0xFF9E7A00, 0xFFFFE082, 0xFFE0B84A, 0xFFF0C95A);
                }
            };
            case BAIZE -> switch (p) {
                case OLHO_CLARO -> 0xFF7FC8FF;
                case OLHO_ESCURO -> 0xFF0D2A40;
                case ESCURO -> 0xFF5C8DB8;
                case CASCO_TOPO -> 0xFF5C8DB8;
                case CASCO -> r < 0.3 ? 0xFFCFE8FF : 0xFF9CC7EA;
                case CRINA, CAUDA, PONTA_CAUDA -> t > 0.9 ? 0xFFFFFFFF : t > 0.55 ? 0xFFE3F2FD : t > 0.25 ? 0xFFBFE3FF : 0xFF7FB2DA;
                default -> {
                    // Pelo branco com "olhos" azuis no corpo: o Baize tem olhos pelo flanco.
                    boolean olho = p == Parte.CORPO && (x % 10 == 4 || x % 10 == 5) && (y % 7 == 3);
                    boolean contorno = p == Parte.CORPO && !olho && (x % 10 >= 3 && x % 10 <= 6) && (y % 7 >= 2 && y % 7 <= 4);
                    yield olho ? 0xFF1E88E5 : contorno ? 0xFF9CC7EA : r < 0.3 ? 0xFFFFFFFF : r < 0.6 ? 0xFFF4F9FD : 0xFFE6F0F8;
                }
            };
            default -> 0xFFFFFFFF;
        };
    }

    /** Escamas em fileiras desencontradas, com uns pontos de brilho. */
    private static int escama(int x, int y, double r, boolean cabeca, int brilho, int borda, int bordaCabeca, int claro, int base, int baseCabeca) {
        int cel = (x + ((y / 3) % 2) * 2) % 4;
        boolean b = cel == 0 || y % 3 == 2;
        return r < 0.04 ? brilho : b ? (cabeca ? bordaCabeca : borda) : r < 0.35 ? claro : (cabeca ? baseCabeca : base);
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

    static byte[] png(BufferedImage im) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(im, "png", b);
        return b.toByteArray();
    }
}
