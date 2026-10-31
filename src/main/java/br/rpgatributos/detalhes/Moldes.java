package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.visual.Telas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 3.10: Moldes de construção. Com a <b>Régua do Construtor</b>:
 * <ul>
 *   <li>clique esquerdo num bloco marca o canto 1, clique direito marca o canto 2 (o contorno aparece);</li>
 *   <li>agachado + clique direito abre o menu: <b>salvar</b> a seleção como molde (com um nome) ou
 *       <b>escolher</b> um molde para colocar;</li>
 *   <li>escolhido o molde, o contorno segue para onde você olha (gira com você); clique direito confirma.
 *       Os blocos saem do inventário (só blocos comuns, sem nome nem marca do RPG) e a obra sobe aos poucos.</li>
 * </ul>
 * Só põe bloco onde há ar (ou mato) e onde você pode construir. Até 32 x 32 x 32 e 20 moldes por jogador.
 */
public final class Moldes implements Listener {

    public static final TextColor COR = TextColor.color(0x8FB8DE);
    private static final NamespacedKey K_REGUA = new NamespacedKey("rpgatributos", "regua_construtor");
    private static final int LADO = 32, MAX_MOLDES = 20, POR_TICK = 40;

    /** Um bloco do molde: posição relativa ao canto de menor x, y e z. */
    private record Peca(int x, int y, int z, String dado) { }

    private record Molde(String nome, int dx, int dy, int dz, List<Peca> pecas) { }

    private final RPGAtributos plugin;
    private final File pasta;
    private final Map<UUID, Location[]> cantos = new HashMap<>();
    /** Quem está escolhendo onde colocar, e qual molde. */
    private final Map<UUID, Molde> colocando = new HashMap<>();
    private final Map<UUID, Integer> giro = new HashMap<>();
    private int ciclo;

    public Moldes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.pasta = new File(plugin.getDataFolder(), "moldes");
    }

    public static ItemStack regua() {
        ItemStack i = new ItemStack(Material.BREEZE_ROD);
        i.editMeta(m -> {
            m.itemName(Component.text("Régua do Construtor", COR, TextDecoration.BOLD));
            m.lore(List.of(
                    Component.text("Clique esquerdo/direito num bloco: os dois cantos.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Agachado + clique direito: salvar ou colocar um molde.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            m.setMaxStackSize(1);
            m.getPersistentDataContainer().set(K_REGUA, PersistentDataType.BYTE, (byte) 1);
            PacoteRecursos.marcar(m, "regua_construtor");
        });
        return i;
    }

    public static boolean ehRegua(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_REGUA);
    }

    public void registrarReceita() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(plugin, "regua_construtor"), regua());
        r.shape("  C", " S ", "P  ");
        r.setIngredient('C', Material.COPPER_INGOT);
        r.setIngredient('S', Material.STICK);
        r.setIngredient('P', Material.PAPER);
        if (Bukkit.getRecipe(r.getKey()) == null) Bukkit.addRecipe(r);
    }

    // =====================================================================
    //  Arquivo (um por jogador)
    // =====================================================================

    private File arquivo(UUID u) {
        return new File(pasta, u + ".yml");
    }

    private Map<String, Molde> moldes(UUID u) {
        Map<String, Molde> m = new LinkedHashMap<>();
        File f = arquivo(u);
        if (!f.exists()) return m;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String k : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(k);
            if (s == null) continue;
            List<Peca> pecas = new ArrayList<>();
            for (String linha : s.getStringList("blocos")) {
                String[] p = linha.split(";", 4);
                if (p.length == 4) pecas.add(new Peca(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3]));
            }
            m.put(k, new Molde(s.getString("nome", k), s.getInt("dx"), s.getInt("dy"), s.getInt("dz"), pecas));
        }
        return m;
    }

    private void salvarMoldes(UUID u, Map<String, Molde> m) {
        pasta.mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Molde> e : m.entrySet()) {
            Molde md = e.getValue();
            y.set(e.getKey() + ".nome", md.nome());
            y.set(e.getKey() + ".dx", md.dx());
            y.set(e.getKey() + ".dy", md.dy());
            y.set(e.getKey() + ".dz", md.dz());
            List<String> l = new ArrayList<>();
            for (Peca p : md.pecas()) l.add(p.x() + ";" + p.y() + ";" + p.z() + ";" + p.dado());
            y.set(e.getKey() + ".blocos", l);
        }
        try {
            y.save(arquivo(u));
        } catch (IOException ex) {
            plugin.getLogger().warning("Moldes: não deu para salvar: " + ex.getMessage());
        }
    }

    private static String chave(String nome) {
        String k = nome.toLowerCase().replaceAll("[^a-z0-9à-ú]+", "_");
        return k.isBlank() ? "molde" : k;
    }

    // =====================================================================
    //  Cliques com a régua
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !ehRegua(e.getItem())) return;
        Player p = e.getPlayer();
        e.setCancelled(true);
        Action a = e.getAction();
        boolean direito = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        if (direito && p.isSneaking()) {
            menu(p);
            return;
        }
        Molde m = colocando.get(p.getUniqueId());
        if (m != null) {
            if (direito) confirmarColocar(p, m);
            else {
                // Clique esquerdo gira o molde 90 graus.
                giro.merge(p.getUniqueId(), 1, (x, y) -> (x + y) % 4);
                p.sendActionBar(Component.text("↻ Girou o molde.", COR));
            }
            return;
        }
        if (e.getClickedBlock() == null) return;
        Location[] c = cantos.computeIfAbsent(p.getUniqueId(), k -> new Location[2]);
        int i = a == Action.LEFT_CLICK_BLOCK ? 0 : 1;
        c[i] = e.getClickedBlock().getLocation();
        p.sendActionBar(Component.text("Canto " + (i + 1) + ": " + c[i].getBlockX() + " " + c[i].getBlockY() + " " + c[i].getBlockZ()
                + (c[0] != null && c[1] != null ? "  (" + tamanho(c) + ")" : ""), COR));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, i == 0 ? 1.2f : 1.5f);
    }

    private static String tamanho(Location[] c) {
        return (Math.abs(c[0].getBlockX() - c[1].getBlockX()) + 1) + " x " + (Math.abs(c[0].getBlockY() - c[1].getBlockY()) + 1) + " x " + (Math.abs(c[0].getBlockZ() - c[1].getBlockZ()) + 1);
    }

    private void menu(Player p) {
        Map<String, Molde> meus = moldes(p.getUniqueId());
        List<Telas.Opcao> op = new ArrayList<>();
        Location[] c = cantos.get(p.getUniqueId());
        if (colocando.containsKey(p.getUniqueId()))
            op.add(Telas.Opcao.de("Parar de colocar", null, q -> {
                colocando.remove(q.getUniqueId());
                q.sendActionBar(Component.text("Ok, nada colocado.", COR));
            }));
        if (c != null && c[0] != null && c[1] != null)
            op.add(Telas.Opcao.de("💾 Salvar a seleção (" + tamanho(c) + ")", "Dá um nome e guarda o molde", this::pedirNome));
        for (Map.Entry<String, Molde> e : meus.entrySet()) {
            Molde m = e.getValue();
            op.add(Telas.Opcao.de("🏗 " + m.nome() + "  (" + m.pecas().size() + " blocos)", "Escolher onde colocar", q -> escolher(q, m)));
        }
        if (!meus.isEmpty()) op.add(Telas.Opcao.de("🗑 Apagar um molde...", null, this::menuApagar));
        Telas.conversa(p, Component.text("📐 Moldes de construção", COR, TextDecoration.BOLD), regua(), List.of(
                Component.text("Marque dois cantos (clique esquerdo e direito num bloco) e salve a construção como molde.", NamedTextColor.WHITE),
                Component.text("Para colocar: escolha o molde, mire onde quer e clique direito. Clique esquerdo gira.", NamedTextColor.GRAY),
                Component.text("Você tem " + meus.size() + " de " + MAX_MOLDES + " moldes.", NamedTextColor.GRAY)), op);
    }

    private void menuApagar(Player p) {
        List<Telas.Opcao> op = new ArrayList<>();
        for (Map.Entry<String, Molde> e : moldes(p.getUniqueId()).entrySet()) {
            String k = e.getKey();
            op.add(Telas.Opcao.de("Apagar " + e.getValue().nome(), null, q -> {
                Map<String, Molde> m = moldes(q.getUniqueId());
                m.remove(k);
                salvarMoldes(q.getUniqueId(), m);
                q.sendActionBar(Component.text("Molde apagado.", COR));
            }));
        }
        Telas.conversa(p, Component.text("🗑 Apagar um molde", COR, TextDecoration.BOLD), null, List.of(Component.text("Qual?", NamedTextColor.WHITE)), op);
    }

    // =====================================================================
    //  Salvar
    // =====================================================================

    private void pedirNome(Player p) {
        Telas.perguntar(p, Component.text("💾 Salvar molde", COR, TextDecoration.BOLD), List.of(Component.text("Que nome o molde vai ter?", NamedTextColor.WHITE)),
                "Nome", "", 24, this::salvar);
    }

    private void salvar(Player p, String nome) {
        Location[] c = cantos.get(p.getUniqueId());
        if (c == null || c[0] == null || c[1] == null || c[0].getWorld() != c[1].getWorld()) {
            p.sendMessage(Component.text("Marque os dois cantos no mesmo mundo.", NamedTextColor.RED));
            return;
        }
        int x0 = Math.min(c[0].getBlockX(), c[1].getBlockX()), y0 = Math.min(c[0].getBlockY(), c[1].getBlockY()), z0 = Math.min(c[0].getBlockZ(), c[1].getBlockZ());
        int dx = Math.abs(c[0].getBlockX() - c[1].getBlockX()) + 1, dy = Math.abs(c[0].getBlockY() - c[1].getBlockY()) + 1, dz = Math.abs(c[0].getBlockZ() - c[1].getBlockZ()) + 1;
        if (dx > LADO || dy > LADO || dz > LADO) {
            p.sendMessage(Component.text("Grande demais: o molde vai até " + LADO + " x " + LADO + " x " + LADO + ".", NamedTextColor.RED));
            return;
        }
        Map<String, Molde> meus = moldes(p.getUniqueId());
        String k = chave(nome);
        if (!meus.containsKey(k) && meus.size() >= MAX_MOLDES) {
            p.sendMessage(Component.text("Você já tem " + MAX_MOLDES + " moldes. Apague um antes.", NamedTextColor.RED));
            return;
        }
        World w = c[0].getWorld();
        List<Peca> pecas = new ArrayList<>();
        for (int y = 0; y < dy; y++) for (int x = 0; x < dx; x++) for (int z = 0; z < dz; z++) {
            Block b = w.getBlockAt(x0 + x, y0 + y, z0 + z);
            Material m = b.getType();
            if (m.isAir() || b.isLiquid() || m == Material.BEDROCK || m == Material.SPAWNER || m == Material.REINFORCED_DEEPSLATE) continue;
            if (plugin.ehEstacao(b)) continue; // estações do plugin não viram molde
            pecas.add(new Peca(x, y, z, b.getBlockData().getAsString()));
        }
        if (pecas.isEmpty()) {
            p.sendMessage(Component.text("Não há blocos na seleção.", NamedTextColor.RED));
            return;
        }
        meus.put(k, new Molde(nome, dx, dy, dz, pecas));
        salvarMoldes(p.getUniqueId(), meus);
        p.sendMessage(Component.text("💾 Molde \"" + nome + "\" salvo: " + pecas.size() + " blocos (" + dx + " x " + dy + " x " + dz + ").", COR));
        p.playSound(p.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1.2f);
    }

    // =====================================================================
    //  Colocar
    // =====================================================================

    private void escolher(Player p, Molde m) {
        colocando.put(p.getUniqueId(), m);
        giro.put(p.getUniqueId(), 0);
        p.sendMessage(Component.text("🏗 " + m.nome() + ": mire onde quer (o contorno mostra). Clique direito coloca, esquerdo gira. Agachado + clique direito cancela.", COR));
    }

    /** Onde o molde ficaria agora: no bloco que o jogador mira, em cima dele. */
    private Location alvo(Player p) {
        RayTraceResult r = p.rayTraceBlocks(24);
        if (r == null || r.getHitBlock() == null) return null;
        return r.getHitBlock().getLocation().add(0, 1, 0);
    }

    /** Gira (x, z) dentro do molde: 0, 90, 180 ou 270 graus. */
    private static int[] girar(int x, int z, int dx, int dz, int g) {
        return switch (g) {
            case 1 -> new int[]{dz - 1 - z, x};
            case 2 -> new int[]{dx - 1 - x, dz - 1 - z};
            case 3 -> new int[]{z, dx - 1 - x};
            default -> new int[]{x, z};
        };
    }

    private static StructureRotation rotacao(int g) {
        return switch (g) {
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            case 3 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }

    /** O item que cada bloco gasta (meia laje dupla gasta 2; a parte de cima da porta e a cabeceira da cama, nada). */
    private static Map<Material, Integer> custo(Molde m) {
        Map<Material, Integer> c = new EnumMap<>(Material.class);
        for (Peca pc : m.pecas()) {
            BlockData d;
            try {
                d = Bukkit.createBlockData(pc.dado());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (d instanceof Bisected b && b.getHalf() == Bisected.Half.TOP && !(d instanceof org.bukkit.block.data.type.Stairs) && !(d instanceof org.bukkit.block.data.type.TrapDoor)) continue;
            if (d instanceof Bed b && b.getPart() == Bed.Part.HEAD) continue;
            Material item = d.getPlacementMaterial();
            if (item == null || item.isAir() || !item.isItem()) continue;
            int n = d instanceof Slab s && s.getType() == Slab.Type.DOUBLE ? 2 : 1;
            c.merge(item, n, Integer::sum);
        }
        return c;
    }

    /** Só itens comuns contam (sem nome, sem marca do RPG): ninguém gasta um item especial sem querer. */
    private static boolean comum(ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        if (!i.hasItemMeta()) return true;
        var m = i.getItemMeta();
        return !m.hasCustomName() && !m.hasItemName() && !m.hasLore() && m.getPersistentDataContainer().isEmpty();
    }

    private static int tem(Player p, Material mat) {
        int n = 0;
        for (ItemStack i : p.getInventory().getStorageContents()) if (comum(i) && i.getType() == mat) n += i.getAmount();
        return n;
    }

    private static void gastar(Player p, Material mat, int qtd) {
        ItemStack[] inv = p.getInventory().getStorageContents();
        for (ItemStack i : inv) {
            if (qtd <= 0) break;
            if (!comum(i) || i.getType() != mat) continue;
            int t = Math.min(qtd, i.getAmount());
            i.setAmount(i.getAmount() - t);
            qtd -= t;
        }
        p.getInventory().setStorageContents(inv);
    }

    private void confirmarColocar(Player p, Molde m) {
        Location base = alvo(p);
        if (base == null) {
            p.sendActionBar(Component.text("Mire num bloco (até 24 de distância).", NamedTextColor.RED));
            return;
        }
        int g = giro.getOrDefault(p.getUniqueId(), 0);
        int ldx = g % 2 == 0 ? m.dx() : m.dz(), ldz = g % 2 == 0 ? m.dz() : m.dx();
        // Lugar livre e permitido?
        int ocupados = 0;
        for (Peca pc : m.pecas()) {
            int[] r = girar(pc.x(), pc.z(), m.dx(), m.dz(), g);
            Block b = base.getWorld().getBlockAt(base.getBlockX() + r[0], base.getBlockY() + pc.y(), base.getBlockZ() + r[1]);
            if (b.getY() >= b.getWorld().getMaxHeight()) {
                p.sendMessage(Component.text("O molde passa do limite de altura aqui.", NamedTextColor.RED));
                return;
            }
            if (plugin.territorios() != null && !plugin.territorios().podeConstruir(p, b)) {
                p.sendMessage(Component.text("Parte do molde cai num lugar onde você não pode construir.", NamedTextColor.RED));
                return;
            }
            if (!b.getType().isAir() && !b.isReplaceable()) ocupados++;
        }
        if (ocupados > 0) {
            p.sendMessage(Component.text("Tem " + ocupados + " bloco(s) no caminho. O molde só ocupa lugar vazio: mova o contorno ou limpe o lugar.", NamedTextColor.RED));
            return;
        }
        Map<Material, Integer> custo = custo(m);
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            List<String> faltam = new ArrayList<>();
            for (Map.Entry<Material, Integer> e : custo.entrySet()) {
                int falta = e.getValue() - tem(p, e.getKey());
                if (falta > 0) faltam.add(falta + " " + e.getKey().name().toLowerCase().replace('_', ' '));
            }
            if (!faltam.isEmpty()) {
                p.sendMessage(Component.text("Faltam blocos: " + String.join(", ", faltam.subList(0, Math.min(10, faltam.size())))
                        + (faltam.size() > 10 ? " e mais " + (faltam.size() - 10) + " tipo(s)" : "") + ".", NamedTextColor.RED));
                return;
            }
            for (Map.Entry<Material, Integer> e : custo.entrySet()) gastar(p, e.getKey(), e.getValue());
        }
        colocando.remove(p.getUniqueId());
        construir(p, m, base, g);
        p.sendMessage(Component.text("🏗 " + m.nome() + " (" + ldx + " x " + m.dy() + " x " + ldz + ") começou a subir.", COR));
    }

    /** A obra sobe de baixo para cima, 40 blocos por tick. */
    private void construir(Player p, Molde m, Location base, int g) {
        List<Peca> ordem = new ArrayList<>(m.pecas());
        ordem.sort((a, b) -> Integer.compare(a.y(), b.y()));
        StructureRotation rot = rotacao(g);
        int[] i = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, t -> {
            for (int n = 0; n < POR_TICK && i[0] < ordem.size(); n++, i[0]++) {
                Peca pc = ordem.get(i[0]);
                int[] r = girar(pc.x(), pc.z(), m.dx(), m.dz(), g);
                Block b = base.getWorld().getBlockAt(base.getBlockX() + r[0], base.getBlockY() + pc.y(), base.getBlockZ() + r[1]);
                if (!b.getType().isAir() && !b.isReplaceable()) continue;
                try {
                    BlockData d = Bukkit.createBlockData(pc.dado());
                    d.rotate(rot);
                    b.setBlockData(d, false);
                } catch (IllegalArgumentException ignorado) { }
                if (n == 0) b.getWorld().playSound(b.getLocation(), Sound.BLOCK_WOOD_PLACE, 0.5f, 1f);
            }
            if (i[0] >= ordem.size()) {
                t.cancel();
                base.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, base.clone().add(m.dx() / 2.0, m.dy() / 2.0, m.dz() / 2.0), 40, m.dx() / 3.0, m.dy() / 3.0, m.dz() / 3.0);
                if (p != null && p.isOnline()) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.4f);
                    plugin.titulos().registrar(p, "moldes_colocados", 1);
                }
            }
        }, 1L, 1L);
    }

    // =====================================================================
    //  Contornos (a cada meio segundo, para quem está com a régua na mão)
    // =====================================================================

    public void tick() {
        ciclo++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!ehRegua(p.getInventory().getItemInMainHand())) continue;
            Molde m = colocando.get(p.getUniqueId());
            if (m != null) {
                Location base = alvo(p);
                if (base == null) continue;
                int g = giro.getOrDefault(p.getUniqueId(), 0);
                int ldx = g % 2 == 0 ? m.dx() : m.dz(), ldz = g % 2 == 0 ? m.dz() : m.dx();
                contorno(p, base, base.clone().add(ldx, m.dy(), ldz), Particle.WAX_OFF);
                continue;
            }
            Location[] c = cantos.get(p.getUniqueId());
            if (c == null || c[0] == null || c[1] == null || c[0].getWorld() != p.getWorld()) continue;
            Location a = new Location(p.getWorld(), Math.min(c[0].getBlockX(), c[1].getBlockX()), Math.min(c[0].getBlockY(), c[1].getBlockY()), Math.min(c[0].getBlockZ(), c[1].getBlockZ()));
            Location b = new Location(p.getWorld(), Math.max(c[0].getBlockX(), c[1].getBlockX()) + 1, Math.max(c[0].getBlockY(), c[1].getBlockY()) + 1, Math.max(c[0].getBlockZ(), c[1].getBlockZ()) + 1);
            contorno(p, a, b, Particle.WAX_ON);
        }
    }

    /** As 12 arestas da caixa, só para o próprio jogador. */
    private static void contorno(Player p, Location a, Location b, Particle part) {
        double x0 = a.getX(), y0 = a.getY(), z0 = a.getZ(), x1 = b.getX(), y1 = b.getY(), z1 = b.getZ();
        double[][] arestas = {
                {x0, y0, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z0, x1, y1, z0}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
        for (double[] e : arestas) {
            double len = Math.max(Math.abs(e[3] - e[0]), Math.max(Math.abs(e[4] - e[1]), Math.abs(e[5] - e[2])));
            int passos = (int) Math.max(1, Math.min(64, len * 1.5));
            for (int i = 0; i <= passos; i++) {
                double f = i / (double) passos;
                p.spawnParticle(part, e[0] + (e[3] - e[0]) * f, e[1] + (e[4] - e[1]) * f, e[2] + (e[5] - e[2]) * f, 1, 0, 0, 0, 0);
            }
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        colocando.remove(e.getPlayer().getUniqueId());
        cantos.remove(e.getPlayer().getUniqueId());
        giro.remove(e.getPlayer().getUniqueId());
    }
}
