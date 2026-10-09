package br.rpgatributos.colonia;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Ingrediente;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.fazenda.Cozinha;
import br.rpgatributos.fazenda.Prato;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.territorio.Territorio;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Colônias (estilo Minecolonies, mais simples): a Prefeitura num sino, cidadãos que são aldeões
 * de verdade (a IA do jogo leva eles ao posto de dia e à cama à noite), e a produção de cada
 * profissão calculada pelo plugin e guardada nos baús de depósito.
 */
public final class Colonias implements Listener {

    private static final int TURNO_SEGUNDOS = 60;
    private static final int VARREDURA_TURNOS = 2;
    private static final long CHEGADA_MS = 4 * 60 * 1000;
    private static final int MAX_DEPOSITOS = 4;
    private static final String[] NOMES = {"Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabi", "Heitor", "Iara", "João",
            "Kátia", "Lucas", "Marina", "Nico", "Olívia", "Paulo", "Quitéria", "Rafa", "Sofia", "Tiago", "Úrsula", "Vitor",
            "Wanda", "Xavier", "Yara", "Zeca", "Bento", "Clara", "Davi", "Helena", "Igor", "Luna", "Mateus", "Nina", "Otto", "Pietra"};

    /** Custo de cada nível da colônia (do 2 ao 5), tirado dos depósitos. */
    private static final List<Map<Material, Integer>> CUSTOS = List.of(
            Map.of(Material.EMERALD, 16, Material.COBBLESTONE, 64, Material.BREAD, 16),
            Map.of(Material.EMERALD, 32, Material.IRON_INGOT, 32, Material.BRICKS, 64),
            Map.of(Material.EMERALD, 48, Material.GOLD_INGOT, 16, Material.BOOKSHELF, 8),
            Map.of(Material.EMERALD, 64, Material.DIAMOND, 8, Material.EMERALD_BLOCK, 4));

    private enum Tipo { PREFEITURA, CIDADAO }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        final Colonia colonia;
        Inventory inventario;
        UUID cidadao;

        Tela(Tipo tipo, Colonia colonia) {
            this.tipo = tipo;
            this.colonia = colonia;
        }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private static final int P_INFO = 4;
    private static final int P_DEPOSITO = 10;
    private static final int P_POSTOS = 12;
    private static final int P_MELHORAR = 14;
    private static final int P_AJUDA = 16;
    private static final int[] P_CIDADAOS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 46, 47};
    private static final int P_FECHAR = 49;
    /** Profissões no menu do morador: as de trabalho nas duas fileiras de cima, as de guerra embaixo. */
    private static final int[] C_PROFISSOES = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 30, 31, 32};
    private static final int P_EXERCITO = 22;
    private static final int C_INFO = 4;
    private static final int C_VOLTAR = 36;
    private static final int C_PEDIDO = 40;
    private static final int C_DISPENSAR = 44;
    /** Chance por turno de um morador sem pedido fazer um (no máximo 2 pedidos abertos por colônia). */
    private static final double CHANCE_PEDIDO = 0.02;

    private final RPGAtributos plugin;
    private final NamespacedKey kCidadao;
    /** Cavalo de um Cavaleiro (guarda o dono da colônia). */
    private final NamespacedKey kMontaria;
    private final Obras obras = new Obras(this);
    /** Cavaleiro a pé → quando pode pegar outro cavalo. */
    private final Map<UUID, Long> proximoCavalo = new HashMap<>();
    private final File arquivo;
    private final Map<UUID, Colonia> porDono = new HashMap<>();
    /** Cidadão (entidade) → dono da colônia. */
    private final Map<UUID, UUID> cidadaos = new HashMap<>();
    /** Quem está escolhendo um baú de depósito → até quando. */
    private final Map<UUID, Long> definindo = new HashMap<>();
    private final Set<UUID> trocandoProfissao = new HashSet<>();
    private final Set<UUID> agendadas = new HashSet<>();
    private int segundos;

    public Colonias(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kCidadao = new NamespacedKey(plugin, "cidadao_colonia");
        this.kMontaria = new NamespacedKey(plugin, "montaria_colonia");
        this.arquivo = new File(plugin.getDataFolder(), "colonias.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    public Colonia de(UUID dono) { return porDono.get(dono); }

    public java.util.Collection<Colonia> todas() { return porDono.values(); }

    public void alterarFelicidade(Colonia c, double delta) {
        c.felicidade = Math.max(0, Math.min(100, c.felicidade + delta));
    }

    /** Guarda no depósito da colônia. @return false se não coube. */
    public boolean depositar(Colonia c, ItemStack item) { return guardar(c, item); }

    /** Jogador ligado à colônia: o dono, um membro do território ou do reino do dono. */
    public boolean daColonia(Player p, Colonia c) {
        if (p.getUniqueId().equals(c.dono)) return true;
        Territorio t = territorio(c);
        if (t != null && t.membro(p.getUniqueId())) return true;
        return plugin.reinos().aliados(p.getUniqueId(), c.dono);
    }

    public Colonia naPrefeitura(Location l) {
        for (Colonia c : porDono.values()) {
            if (c.mundo.equals(l.getWorld().getName()) && c.x == l.getBlockX() && c.y == l.getBlockY() && c.z == l.getBlockZ()) return c;
        }
        return null;
    }

    private Territorio territorio(Colonia c) {
        return plugin.territorios().de(c.dono);
    }

    private boolean gerencia(Player p, Colonia c) {
        if (p.getUniqueId().equals(c.dono)) return true;
        Territorio t = territorio(c);
        return t != null && plugin.territorios().gerencia(p, t);
    }

    // =====================================================================
    //  Fundar e perder
    // =====================================================================

    void fundar(Player dono, Block sino) {
        Colonia c = new Colonia(dono.getUniqueId(), dono.getName(), sino.getWorld().getName(), sino.getX(), sino.getY(), sino.getZ());
        porDono.put(c.dono, c);
        c.proximaChegada = System.currentTimeMillis() + CHEGADA_MS;
        // Os dois primeiros moradores chegam na hora.
        for (int i = 0; i < 2; i++) chegar(c);
        varrer(c);
        dono.sendMessage(Component.text("⌂ Colônia fundada! ", Prefeituras.COR)
                .append(Component.text("Coloque camas, baús (defina o depósito na Prefeitura) e os postos de trabalho por perto.", NamedTextColor.GRAY)));
        salvar();
    }

    void aoPerderPrefeitura(Location l) {
        Colonia c = naPrefeitura(l);
        if (c == null) return;
        for (Colonia.Cidadao ci : c.cidadaos) {
            cidadaos.remove(ci.entidade);
            if (Bukkit.getEntity(ci.entidade) instanceof Villager v) {
                v.getPersistentDataContainer().remove(kCidadao);
                v.customName(null);
            }
        }
        porDono.remove(c.dono);
        Player dono = Bukkit.getPlayer(c.dono);
        if (dono != null) dono.sendMessage(Component.text("⌂ A Prefeitura foi destruída: a colônia acabou e os moradores viraram aldeões comuns.", NamedTextColor.RED));
        salvar();
    }

    // =====================================================================
    //  Cidadãos
    // =====================================================================

    private Villager criarMorador(Colonia c, String nome, Location onde) {
        return onde.getWorld().spawn(onde, Villager.class, vv -> {
            vv.setProfession(Villager.Profession.NONE);
            vv.setAdult();
            vv.setBreed(false);
            vv.setPersistent(true);
            vv.setRemoveWhenFarAway(false);
            vv.customName(Component.text(nome, NamedTextColor.YELLOW).append(Component.text(" · Desempregado", NamedTextColor.GRAY)));
            vv.getPersistentDataContainer().set(kCidadao, PersistentDataType.STRING, c.dono.toString());
        });
    }

    private void chegar(Colonia c) {
        Location base = c.prefeitura();
        if (base == null || !base.isChunkLoaded()) return;
        Location onde = lugarLivre(base);
        String nome = NOMES[rnd().nextInt(NOMES.length)];
        Villager v = criarMorador(c, nome, onde);
        Colonia.Cidadao ci = new Colonia.Cidadao(v.getUniqueId(), nome, Profissao.DESEMPREGADO, 1, 0);
        ci.tracos.addAll(Traco.sortear());
        c.cidadaos.add(ci);
        cidadaos.put(v.getUniqueId(), c.dono);
        v.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, v.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4);
        v.getWorld().playSound(v.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        Player dono = Bukkit.getPlayer(c.dono);
        if (dono != null) {
            dono.sendMessage(Component.text("⌂ " + nome + " chegou na sua colônia! Dê um trabalho a ele(a) na Prefeitura.", Prefeituras.COR)
                    .append(Component.text(" (" + nomesTracos(ci) + ")", NamedTextColor.GRAY)));
        }
    }

    private static String nomesTracos(Colonia.Cidadao ci) {
        return String.join(", ", ci.tracos.stream().map(Traco::nome).toList());
    }

    /** Nome sobre a cabeça: "Ana · Fazendeiro", com um ❗ quando tem pedido aberto. */
    private Component nomeMorador(Colonia.Cidadao ci) {
        Component n = Component.text(ci.nome, NamedTextColor.YELLOW).append(Component.text(" · " + ci.profissao.nome(),
                ci.profissao == Profissao.DESEMPREGADO ? NamedTextColor.GRAY : ci.profissao.cor()));
        return ci.pedido == null ? n : n.append(Component.text(" ❗", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    private void renomear(Colonia c, Colonia.Cidadao ci) {
        Entity e = Bukkit.getEntity(ci.entidade);
        if (e instanceof Villager v) v.customName(nomeMorador(ci));
        else if (e != null && ci.profissao.soldado()) nomearSoldado(c, ci, e);
    }

    private static Location lugarLivre(Location base) {
        for (int i = 0; i < 12; i++) {
            Location l = base.clone().add(rnd().nextInt(-3, 4) + 0.5, 0, rnd().nextInt(-3, 4) + 0.5);
            for (int dy = 3; dy >= -3; dy--) {
                Block pe = l.clone().add(0, dy, 0).getBlock();
                if (pe.isPassable() && pe.getRelative(0, 1, 0).isPassable() && pe.getRelative(0, -1, 0).getType().isSolid()) {
                    return pe.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return base.clone().add(0.5, 1, 0.5);
    }

    private void definirProfissao(Colonia c, Colonia.Cidadao ci, Profissao p) {
        Profissao antes = ci.profissao;
        ci.profissao = p;
        // Virar soldado (ou deixar de ser) troca a entidade: o morador veste a armadura.
        if (p.soldado() || antes.soldado()) {
            Entity velha = Bukkit.getEntity(ci.entidade);
            if (velha == null) return; // longe: troca quando ele aparecer de novo
            trocarEntidade(c, ci, velha);
            if (!p.soldado()) definirProfissao(c, ci, p); // de volta a morador: põe a roupa da profissão
            return;
        }
        if (Bukkit.getEntity(ci.entidade) instanceof Villager v) {
            trocandoProfissao.add(v.getUniqueId());
            try {
                v.setProfession(p.roupa());
                if (p != Profissao.DESEMPREGADO) {
                    v.setVillagerExperience(Math.max(1, v.getVillagerExperience()));
                    v.setVillagerLevel(Math.min(5, 1 + ci.nivel / 2));
                }
            } finally {
                trocandoProfissao.remove(v.getUniqueId());
            }
            v.customName(nomeMorador(ci));
            v.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, v.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3);
        }
    }

    private Colonia.Cidadao cidadao(Colonia c, UUID id) {
        for (Colonia.Cidadao ci : c.cidadaos) if (ci.entidade.equals(id)) return ci;
        return null;
    }

    private void perderCidadao(UUID id, String motivo) {
        UUID dono = cidadaos.remove(id);
        proximoCavalo.remove(id);
        Colonia c = dono == null ? null : porDono.get(dono);
        if (c == null) return;
        Colonia.Cidadao ci = cidadao(c, id);
        if (ci == null) return;
        c.cidadaos.remove(ci);
        Player p = Bukkit.getPlayer(dono);
        if (p != null) p.sendMessage(Component.text("⌂ " + ci.nome + " " + motivo, NamedTextColor.RED));
        salvar();
    }

    // =====================================================================
    //  Depósito
    // =====================================================================

    private List<Inventory> inventarios(Colonia c) {
        List<Inventory> l = new ArrayList<>();
        Set<Inventory> vistos = new HashSet<>();
        for (Location d : c.depositos) {
            if (!d.isChunkLoaded() || !(d.getBlock().getState() instanceof Container cont)) continue;
            Inventory inv = cont.getInventory();
            if (vistos.add(inv)) l.add(inv);
        }
        return l;
    }

    /** Guarda no depósito. @return false se não coube tudo (o resto se perde). */
    private boolean guardar(Colonia c, ItemStack item) {
        ItemStack resto = item.clone();
        for (Inventory inv : inventarios(c)) {
            Map<Integer, ItemStack> sobra = inv.addItem(resto);
            if (sobra.isEmpty()) return true;
            resto = sobra.values().iterator().next();
        }
        return false;
    }

    int contar(Colonia c, Predicate<ItemStack> teste) {
        int n = 0;
        for (Inventory inv : inventarios(c)) for (ItemStack s : inv.getContents()) if (s != null && teste.test(s)) n += s.getAmount();
        return n;
    }

    /** Quanto o depósito tem de cada tipo de item que passa no teste. */
    Map<Material, Integer> contarPorTipo(Colonia c, Predicate<ItemStack> teste) {
        Map<Material, Integer> n = new EnumMap<>(Material.class);
        for (Inventory inv : inventarios(c)) {
            for (ItemStack s : inv.getContents()) if (s != null && !s.isEmpty() && teste.test(s)) n.merge(s.getType(), s.getAmount(), Integer::sum);
        }
        return n;
    }

    /** O tipo do primeiro item do depósito que passa no teste (null = nenhum). */
    Material primeiroTipo(Colonia c, Predicate<ItemStack> teste) {
        for (Inventory inv : inventarios(c)) for (ItemStack s : inv.getContents()) if (s != null && !s.isEmpty() && teste.test(s)) return s.getType();
        return null;
    }

    /** Tira {@code qtd} itens do depósito. @return a qualidade de cada unidade tirada. */
    List<Qualidade> tirar(Colonia c, Predicate<ItemStack> teste, int qtd) {
        List<Qualidade> qs = new ArrayList<>();
        for (Inventory inv : inventarios(c)) {
            ItemStack[] cont = inv.getContents();
            for (int i = 0; i < cont.length && qtd > 0; i++) {
                ItemStack s = cont[i];
                if (s == null || !teste.test(s)) continue;
                int tira = Math.min(qtd, s.getAmount());
                Qualidade q = Qualidade.de(s);
                for (int k = 0; k < tira; k++) qs.add(q);
                s.setAmount(s.getAmount() - tira);
                cont[i] = s.getAmount() > 0 ? s : null;
                qtd -= tira;
            }
            inv.setContents(cont);
            if (qtd <= 0) break;
        }
        return qs;
    }

    private static Predicate<ItemStack> simples(Material m) {
        return s -> s.getType() == m && Ingrediente.simples(s);
    }

    // =====================================================================
    //  Varredura (camas, água, plantações e postos no território da colônia)
    // =====================================================================

    private static final Set<Material> POSTOS_BLOCO = Set.of(Material.COMPOSTER, Material.FLETCHING_TABLE, Material.BARREL,
            Material.STONECUTTER, Material.SMOKER, Material.BREWING_STAND, Material.ANVIL, Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL, Material.HAY_BLOCK, Material.TARGET, Material.CARTOGRAPHY_TABLE, Material.LECTERN,
            Material.CRAFTING_TABLE);
    private static final Set<Material> PLANTAS = Set.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS);

    /** Os chunks que a colônia enxerga: o território inteiro do dono (ou, sem território, os perto da Prefeitura). */
    private List<long[]> chunksDaColonia(Colonia c) {
        List<long[]> l = new ArrayList<>();
        Territorio t = territorio(c);
        if (t != null && t.mundo().equals(c.mundo)) {
            for (long k : t.chunks()) l.add(new long[]{Territorio.chunkX(k), Territorio.chunkZ(k)});
            return l;
        }
        for (int cx = (c.x - Colonia.RAIO) >> 4; cx <= (c.x + Colonia.RAIO) >> 4; cx++) {
            for (int cz = (c.z - Colonia.RAIO) >> 4; cz <= (c.z + Colonia.RAIO) >> 4; cz++) l.add(new long[]{cx, cz});
        }
        return l;
    }

    /** O lugar faz parte da colônia (território do dono)? */
    public boolean naColonia(Colonia c, Location l) {
        if (l.getWorld() == null || !l.getWorld().getName().equals(c.mundo)) return false;
        Territorio t = territorio(c);
        if (t == null) return c.dentro(l);
        return plugin.territorios().em(l) == t && l.getBlockY() >= c.y - Colonia.ABAIXO && l.getBlockY() <= c.y + Colonia.ACIMA;
    }

    private void varrer(Colonia c) {
        World w = c.world();
        if (w == null || c.varrendo || !w.isChunkLoaded(c.x >> 4, c.z >> 4)) return;
        c.varrendo = true;
        List<ChunkSnapshot> fotos = new ArrayList<>();
        Set<Long> todos = new HashSet<>();
        for (long[] ch : chunksDaColonia(c)) {
            int cx = (int) ch[0], cz = (int) ch[1];
            todos.add(Territorio.chave(cx, cz));
            if (w.isChunkLoaded(cx, cz)) fotos.add(w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false));
        }
        boolean semTerritorio = territorio(c) == null;
        int minY = Math.max(w.getMinHeight(), c.y - Colonia.ABAIXO), maxY = Math.min(w.getMaxHeight() - 1, c.y + Colonia.ACIMA);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Long, Colonia.Censo> novos = new HashMap<>();
            for (ChunkSnapshot f : fotos) {
                Colonia.Censo s = new Colonia.Censo();
                int bx = f.getX() << 4, bz = f.getZ() << 4;
                for (int dx = 0; dx < 16; dx++) {
                    int x = bx + dx;
                    if (semTerritorio && Math.abs(x - c.x) > Colonia.RAIO) continue;
                    for (int dz = 0; dz < 16; dz++) {
                        int z = bz + dz;
                        if (semTerritorio && Math.abs(z - c.z) > Colonia.RAIO) continue;
                        for (int y = minY; y <= maxY; y++) {
                            Material m = f.getBlockType(dx, y, dz);
                            if (m.isAir()) continue;
                            if (m == Material.WATER) s.agua++;
                            else if (m.name().endsWith("_BED")) s.camasPartes++;
                            else if (PLANTAS.contains(m)) s.plantas.merge(m, 1, Integer::sum);
                            else if (m.name().endsWith("_LOG")) s.troncos.merge(m, 1, Integer::sum);
                            else if (POSTOS_BLOCO.contains(m)) s.blocosPosto.add(new Object[]{x, y, z, m});
                        }
                    }
                }
                novos.put(Territorio.chave(f.getX(), f.getZ()), s);
            }
            Bukkit.getScheduler().runTask(plugin, () -> aplicarVarredura(c, novos, todos));
        });
    }

    /** Junta a contagem nova (chunks carregados) com a guardada dos outros chunks do território. */
    private void aplicarVarredura(Colonia c, Map<Long, Colonia.Censo> novos, Set<Long> todos) {
        c.varrendo = false;
        World w = c.world();
        if (w == null || !porDono.containsKey(c.dono)) return;
        c.ultimaVarredura = System.currentTimeMillis();
        for (Map.Entry<Long, Colonia.Censo> en : novos.entrySet()) {
            Colonia.Censo s = en.getValue();
            for (Object[] o : s.blocosPosto) {
                int x = (Integer) o[0], y = (Integer) o[1], z = (Integer) o[2];
                if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                Block b = w.getBlockAt(x, y, z);
                Profissao p = switch ((Material) o[3]) {
                    case COMPOSTER -> Profissao.FAZENDEIRO;
                    case FLETCHING_TABLE -> Profissao.LENHADOR;
                    case BARREL -> Profissao.PESCADOR;
                    case STONECUTTER -> Profissao.MINERADOR;
                    case SMOKER -> plugin.cozinha().eh(b) ? Profissao.COZINHEIRO : null;
                    case BREWING_STAND -> plugin.alquimia().eh(b) ? Profissao.ALQUIMISTA : null;
                    case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> plugin.forjas().eh(b) ? Profissao.FERREIRO : null;
                    case HAY_BLOCK -> plugin.altaresDomador().eh(b) ? Profissao.TRATADOR : null;
                    case TARGET -> Profissao.SOLDADO;
                    case CARTOGRAPHY_TABLE -> Profissao.MERCADOR;
                    case LECTERN -> Profissao.BIBLIOTECARIO;
                    case CRAFTING_TABLE -> Profissao.CONSTRUTOR;
                    default -> null;
                };
                if (p != null) s.postos.merge(p, 1, Integer::sum);
            }
            s.blocosPosto.clear();
            c.censo.put(en.getKey(), s);
        }
        c.censo.keySet().retainAll(todos);
        int camasPartes = 0, agua = 0;
        c.plantas.clear();
        c.troncos.clear();
        c.postos.clear();
        for (Colonia.Censo s : c.censo.values()) {
            camasPartes += s.camasPartes;
            agua += s.agua;
            s.plantas.forEach((m, n) -> c.plantas.merge(m, n, Integer::sum));
            s.troncos.forEach((m, n) -> c.troncos.merge(m, n, Integer::sum));
            s.postos.forEach((p, n) -> c.postos.merge(p, n, Integer::sum));
        }
        c.camas = camasPartes / 2;
        c.agua = agua;
        c.varrida = true;
        redesenharMenus(c);
    }

    /** Quem está com o menu da colônia aberto vê os números novos. */
    private void redesenharMenus(Colonia c) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getOpenInventory().getTopInventory().getHolder(false) instanceof Tela t) || t.colonia != c) continue;
            if (t.tipo == Tipo.PREFEITURA) desenharPrefeitura(t);
            else {
                Colonia.Cidadao ci = cidadao(c, t.cidadao);
                if (ci != null) desenharCidadao(t, ci);
            }
        }
    }

    /** Bloco de posto ou cama colocado/quebrado perto de uma colônia: procura de novo logo. */
    private void talvezVarrer(Block b) {
        Material m = b.getType();
        if (!POSTOS_BLOCO.contains(m) && !m.name().endsWith("_BED") && !PLANTAS.contains(m)) return;
        for (Colonia c : porDono.values()) {
            if (!naColonia(c, b.getLocation()) || !agendadas.add(c.dono)) continue;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                agendadas.remove(c.dono);
                if (porDono.containsKey(c.dono)) varrer(c);
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoColocarBloco(org.bukkit.event.block.BlockPlaceEvent e) {
        talvezVarrer(e.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrarBloco(org.bukkit.event.block.BlockBreakEvent e) {
        talvezVarrer(e.getBlock());
    }

    // =====================================================================
    //  Turnos de trabalho
    // =====================================================================

    /** Roda 1x por segundo. */
    public void tick() {
        segundos++;
        if (segundos % TURNO_SEGUNDOS != 0) return;
        int turno = segundos / TURNO_SEGUNDOS;
        for (Colonia c : List.copyOf(porDono.values())) {
            Location pref = c.prefeitura();
            if (pref == null || !pref.isChunkLoaded()) continue;
            if (turno % VARREDURA_TURNOS == 0 || !c.varrida) varrer(c);
            turno(c);
        }
        if (turno % 5 == 0) salvar();
    }

    private void turno(Colonia c) {
        World w = c.world();
        // Moradores que sumiram (morreram longe, foram removidos...).
        for (Colonia.Cidadao ci : List.copyOf(c.cidadaos)) {
            Entity e = Bukkit.getEntity(ci.entidade);
            if (e != null && e.isValid()) { ci.sumido = 0; continue; }
            if (++ci.sumido >= 5) perderCidadao(ci.entidade, "não voltou para casa e saiu da colônia.");
        }
        comer(c);
        double alvo = 50 + (c.camas >= c.cidadaos.size() ? 15 : -20) + 25 * c.alimentados - (c.alimentados < 0.5 ? 25 : 0)
                + (c.comeuPratos ? 15 : 0) + humorDaColonia(c);
        c.felicidade += (Math.max(0, Math.min(100, alvo)) - c.felicidade) * 0.3;
        pedidos(c);

        // Chegam moradores novos se houver cama, comida e felicidade.
        long agora = System.currentTimeMillis();
        if (agora >= c.proximaChegada && c.cidadaos.size() < Math.min(c.maxCidadaos(), c.camas) && c.felicidade >= 40) {
            chegar(c);
            c.proximaChegada = agora + CHEGADA_MS;
        }

        // Trabalham de dia (quem acorda cedo pega também o começo e o fim da noite).
        long hora = w.getTime();
        boolean noite = hora > 12500 && hora < 23500;
        boolean madrugada = hora > 12500 && hora < 14500 || hora > 22000 && hora < 23500;
        if (noite && !madrugada) return;
        if (c.depositos.isEmpty()) {
            avisar(c, "Defina um baú de depósito na Prefeitura: sem ele ninguém trabalha.");
            return;
        }
        Map<Profissao, Integer> ocupados = new EnumMap<>(Profissao.class);
        boolean cheio = false;
        for (Colonia.Cidadao ci : c.cidadaos) {
            Profissao p = ci.profissao;
            if (p == Profissao.DESEMPREGADO || p.soldado()) continue;
            int usados = ocupados.merge(p, 1, Integer::sum);
            if (usados > c.postos.getOrDefault(p, 0)) continue; // falta posto para ele
            if (noite && !ci.tem(Traco.MADRUGADOR)) continue;
            double mult = (1 + 0.1 * ci.nivel) * c.fatorFelicidade() * (c.nivel >= Colonia.NIVEL_MAXIMO ? 1.2 : 1) * ci.fatorPessoal();
            List<ItemStack> feitos = trabalhar(c, ci, mult);
            if (feitos == null) continue;
            if (ci.tem(Traco.SORTE) && !feitos.isEmpty() && rnd().nextDouble() < 0.12) {
                ItemStack extra = feitos.get(rnd().nextInt(feitos.size())).clone();
                extra.setAmount(1);
                feitos.add(extra);
            }
            for (ItemStack s : feitos) if (!guardar(c, s)) cheio = true;
            ganharXp(c, ci, 5);
            if (Bukkit.getEntity(ci.entidade) instanceof Villager v) {
                v.swingMainHand();
                v.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, v.getLocation().add(0, 2, 0), 3, 0.2, 0.2, 0.2);
            }
        }
        if (cheio) avisar(c, "O depósito da colônia está cheio!");
    }

    private void avisar(Colonia c, String msg) {
        long agora = System.currentTimeMillis();
        // O mesmo aviso no máximo a cada 10 minutos, e nenhum aviso a menos de 3 minutos do anterior.
        if (agora - c.ultimoAvisoMs < (msg.equals(c.ultimoAviso) ? 600_000 : 180_000)) return;
        c.ultimoAviso = msg;
        c.ultimoAvisoMs = agora;
        Player p = Bukkit.getPlayer(c.dono);
        if (p != null) p.sendMessage(Component.text("⌂ " + msg, NamedTextColor.GOLD));
    }

    /** Quanto o jeito e o humor dos moradores mexem na felicidade da colônia (até ±12). */
    private static double humorDaColonia(Colonia c) {
        double d = 0;
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (ci.tem(Traco.BOM_HUMOR)) d += 3;
            if (ci.tem(Traco.MAU_HUMOR)) d -= 3;
            if (ci.humor < 0) d -= 2;
        }
        return Math.max(-12, Math.min(12, d));
    }

    /** Humor passando, prazos correndo e, de vez em quando, um pedido novo. */
    private void pedidos(Colonia c) {
        int abertos = 0;
        for (Colonia.Cidadao ci : c.cidadaos) if (ci.pedido != null) abertos++;
        Player dono = Bukkit.getPlayer(c.dono);
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (ci.humorTurnos > 0 && --ci.humorTurnos == 0) ci.humor = 0;
            if (ci.pedido != null) {
                if (--ci.prazo > 0) continue;
                // Ninguém atendeu: fica chateado.
                if (dono != null) dono.sendMessage(Component.text("⌂ " + ci.nome + " ficou chateado(a): ninguém trouxe " + ci.pedido.texto() + ".",
                        NamedTextColor.GRAY));
                ci.pedido = null;
                ci.humor = -1;
                ci.humorTurnos = Pedido.CHATEADO;
                alterarFelicidade(c, -5);
                renomear(c, ci);
                continue;
            }
            if (abertos >= 2 || ci.humor != 0 || rnd().nextDouble() >= CHANCE_PEDIDO) continue;
            ci.pedido = Pedido.sortear(ci.profissao);
            ci.prazo = Pedido.PRAZO;
            abertos++;
            renomear(c, ci);
            if (dono != null) {
                dono.sendMessage(Component.text("⌂ " + ci.nome + " (" + ci.profissao.nome() + ") tem um pedido: ", Prefeituras.COR)
                        .append(Component.text(ci.pedido.texto(), NamedTextColor.WHITE))
                        .append(Component.text(". \"" + ci.pedido.motivo() + "\" Fale com " + ci.nome + " para entregar.", NamedTextColor.GRAY)));
            }
        }
    }

    /** O jogador entrega o pedido do morador (tira do inventário dele). */
    private void entregarPedido(Player p, Colonia c, Colonia.Cidadao ci) {
        Pedido pe = ci.pedido;
        if (pe == null) return;
        int tem = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) if (pe.serve(s)) tem += s.getAmount();
        if (tem < pe.qtd()) { erro(p, "Você precisa ter " + pe.texto() + " no inventário."); return; }
        int falta = pe.qtd();
        ItemStack[] cont = p.getInventory().getStorageContents();
        for (int i = 0; i < cont.length && falta > 0; i++) {
            if (!pe.serve(cont[i])) continue;
            int tira = Math.min(falta, cont[i].getAmount());
            cont[i].setAmount(cont[i].getAmount() - tira);
            if (cont[i].getAmount() <= 0) cont[i] = null;
            falta -= tira;
        }
        p.getInventory().setStorageContents(cont);
        ci.pedido = null;
        ci.humor = 1;
        ci.humorTurnos = Pedido.CONTENTE;
        alterarFelicidade(c, 8);
        ganharXp(c, ci, 15);
        renomear(c, ci);
        Entity e = Bukkit.getEntity(ci.entidade);
        if (e != null) {
            e.getWorld().spawnParticle(Particle.HEART, e.getLocation().add(0, 2, 0), 6, 0.4, 0.3, 0.4);
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1.1f);
        }
        p.sendMessage(Component.text("⌂ " + ci.nome + " adorou! A colônia ficou mais feliz e " + ci.nome + " vai trabalhar com mais vontade.",
                Prefeituras.COR));
        salvar();
    }

    private void ganharXp(Colonia c, Colonia.Cidadao ci, double xp) {
        if (ci.nivel >= 10) return;
        if (ci.tem(Traco.CURIOSIDADE)) xp *= 1.5;
        ci.xp += xp;
        while (ci.nivel < 10 && ci.xp >= Colonia.Cidadao.xpParaProximo(ci.nivel)) {
            ci.xp -= Colonia.Cidadao.xpParaProximo(ci.nivel);
            ci.nivel++;
            if (Bukkit.getEntity(ci.entidade) instanceof Villager v) {
                trocandoProfissao.add(v.getUniqueId());
                try { v.setVillagerLevel(Math.min(5, 1 + ci.nivel / 2)); } finally { trocandoProfissao.remove(v.getUniqueId()); }
                v.getWorld().playSound(v.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1.2f);
            }
            Player p = Bukkit.getPlayer(c.dono);
            if (p != null) p.sendMessage(Component.text("⌂ " + ci.nome + " (" + ci.profissao.nome() + ") subiu para o nível " + ci.nivel + "!", Prefeituras.COR));
        }
    }

    /** Cada morador come um pouco por turno. Pratos da Cozinha alimentam mais e deixam todos felizes. */
    private void comer(Colonia c) {
        for (Colonia.Cidadao ci : c.cidadaos) c.fome += ci.fome();
        int precisa = (int) Math.floor(c.fome);
        if (precisa <= 0) return;
        int comidos = 0, pratos = 0;
        while (comidos < precisa) {
            List<Qualidade> q = tirar(c, s -> s.getPersistentDataContainer().has(Cozinha.CHAVE_PRATO), 1);
            if (!q.isEmpty()) { comidos += 3; pratos++; continue; }
            q = tirar(c, Colonias::comida, 1);
            if (q.isEmpty()) break;
            comidos++;
        }
        c.alimentados = Math.min(1, comidos / (double) precisa);
        c.comeuPratos = pratos > 0;
        if (pratos > 0) for (Colonia.Cidadao ci : c.cidadaos) if (ci.tem(Traco.APETITE)) alterarFelicidade(c, 1);
        c.fome = Math.max(0, c.fome - comidos);
        if (c.alimentados < 1) avisar(c, "A colônia está com fome! Coloque comida no depósito.");
    }

    private static boolean comida(ItemStack s) {
        Material m = s.getType();
        if (!m.isEdible() || !Ingrediente.simples(s)) return false;
        return m != Material.ROTTEN_FLESH && m != Material.SPIDER_EYE && m != Material.POISONOUS_POTATO
                && m != Material.PUFFERFISH && m != Material.CHORUS_FRUIT && m != Material.GOLDEN_APPLE
                && m != Material.ENCHANTED_GOLDEN_APPLE && m != Material.SUSPICIOUS_STEW;
    }

    /** O que um morador produz neste turno (null = não trabalhou). */
    private List<ItemStack> trabalhar(Colonia c, Colonia.Cidadao ci, double mult) {
        List<ItemStack> l = new ArrayList<>();
        switch (ci.profissao) {
            case FAZENDEIRO -> {
                Location prefeitura = c.prefeitura();
                if (prefeitura != null) l.addAll(plugin.pomar().colherEm(loc -> naColonia(c, loc), 2)); // frutas maduras do pomar
                if (prefeitura != null) l.addAll(plugin.cultivos().colherEm(loc -> naColonia(c, loc), 2)); // plantações da estação maduras
                if (c.plantas.isEmpty()) return l.isEmpty() ? null : l;
                Material planta = sortear(c.plantas);
                Material produto = switch (planta) {
                    case CARROTS -> Material.CARROT;
                    case POTATOES -> Material.POTATO;
                    case BEETROOTS -> Material.BEETROOT;
                    default -> Material.WHEAT;
                };
                adicionarComQualidade(l, produto, quantidade(3, 5, mult), ci.nivel);
                if (produto == Material.WHEAT || produto == Material.BEETROOT) {
                    l.add(new ItemStack(produto == Material.WHEAT ? Material.WHEAT_SEEDS : Material.BEETROOT_SEEDS, 1 + rnd().nextInt(2)));
                }
                if (rnd().nextDouble() < 0.02) {
                    Variedade[] vs = Variedade.values();
                    l.add(vs[rnd().nextInt(vs.length)].produto(1));
                }
            }
            case LENHADOR -> {
                Material tronco = c.troncos.isEmpty() ? Material.OAK_LOG : sortear(c.troncos);
                l.add(new ItemStack(tronco, quantidade(4, 7, mult)));
                Material muda = Material.matchMaterial(tronco.name().replace("_LOG", "_SAPLING"));
                if (muda != null && rnd().nextDouble() < 0.25) l.add(new ItemStack(muda));
                if (rnd().nextDouble() < 0.08) l.add(new ItemStack(Material.APPLE));
                l.add(new ItemStack(Material.STICK, rnd().nextInt(0, 3) + 1));
            }
            case PESCADOR -> {
                if (c.agua < 8) return null;
                adicionarComQualidade(l, rnd().nextDouble() < 0.6 ? Material.COD : Material.SALMON, quantidade(1, 2, mult), ci.nivel);
                if (rnd().nextDouble() < 0.03) l.add((rnd().nextBoolean() ? PeixeRaro.CARPA_DOURADA : PeixeRaro.TRUTA_ARCO_IRIS)
                        .criar(1, Qualidade.sortear(ci.nivel / 10.0)));
                if (rnd().nextDouble() < 0.05) l.add(new ItemStack(Material.INK_SAC));
            }
            case MINERADOR -> {
                l.add(new ItemStack(Material.COBBLESTONE, quantidade(4, 8, mult)));
                double b = ci.nivel * 0.005;
                if (rnd().nextDouble() < 0.4) l.add(new ItemStack(Material.COAL, quantidade(1, 3, mult)));
                if (rnd().nextDouble() < 0.25 + b) l.add(new ItemStack(Material.RAW_IRON, quantidade(1, 2, mult)));
                if (rnd().nextDouble() < 0.25) l.add(new ItemStack(Material.RAW_COPPER, quantidade(1, 3, mult)));
                if (rnd().nextDouble() < 0.1 + b) l.add(new ItemStack(Material.RAW_GOLD, 1));
                if (rnd().nextDouble() < 0.1) l.add(new ItemStack(Material.REDSTONE, quantidade(2, 4, mult)));
                if (rnd().nextDouble() < 0.08) l.add(new ItemStack(Material.LAPIS_LAZULI, quantidade(2, 4, mult)));
                if (rnd().nextDouble() < 0.02 + b) l.add(new ItemStack(Material.DIAMOND));
                if (rnd().nextDouble() < 0.01) l.add(new ItemStack(Material.EMERALD));
            }
            case COZINHEIRO -> {
                ItemStack prato = cozinhar(c, ci);
                if (prato == null) return null;
                l.add(prato);
            }
            case FERREIRO -> {
                boolean fez = consertar(c, ci);
                int fundir = 2 + ci.nivel / 3;
                for (Material[] par : new Material[][]{{Material.RAW_IRON, Material.IRON_INGOT}, {Material.RAW_GOLD, Material.GOLD_INGOT},
                        {Material.RAW_COPPER, Material.COPPER_INGOT}}) {
                    if (fundir <= 0) break;
                    int tem = contar(c, simples(par[0]));
                    int n = Math.min(fundir, tem);
                    if (n <= 0) continue;
                    tirar(c, simples(par[0]), n);
                    l.add(new ItemStack(par[1], n));
                    fundir -= n;
                    fez = true;
                }
                if (!fez) return null;
            }
            case TRATADOR -> {
                Material[] coisas = {Material.LEATHER, Material.WHITE_WOOL, Material.EGG, Material.FEATHER, Material.BEEF,
                        Material.CHICKEN, Material.PORKCHOP, Material.MUTTON};
                int n = quantidade(2, 3, mult);
                for (int i = 0; i < n; i++) l.add(new ItemStack(coisas[rnd().nextInt(coisas.length)]));
            }
            case ALQUIMISTA -> {
                if (contar(c, simples(Material.LAPIS_LAZULI)) >= 2 && contar(c, simples(Material.REDSTONE)) >= 1
                        && contar(c, simples(Material.GLOWSTONE_DUST)) >= 1) {
                    tirar(c, simples(Material.LAPIS_LAZULI), 2);
                    tirar(c, simples(Material.REDSTONE), 1);
                    tirar(c, simples(Material.GLOWSTONE_DUST), 1);
                    l.add(Reagente.PO_ARCANO.criar(rnd().nextDouble() < ci.nivel * 0.04 ? 4 : 2));
                } else if (contar(c, s -> (s.getType() == Material.COD || s.getType() == Material.SALMON) && Ingrediente.simples(s)) >= 3
                        && contar(c, simples(Material.GLASS_BOTTLE)) >= 1) {
                    tirar(c, s -> (s.getType() == Material.COD || s.getType() == Material.SALMON) && Ingrediente.simples(s), 3);
                    tirar(c, simples(Material.GLASS_BOTTLE), 1);
                    l.add(Reagente.OLEO_DE_PEIXE.criar(2));
                } else {
                    return null;
                }
            }
            case MERCADOR -> {
                int esmeraldas = vender(c, ci, mult);
                if (esmeraldas <= 0) return null;
                l.add(new ItemStack(Material.EMERALD, esmeraldas));
            }
            case BIBLIOTECARIO -> {
                if (contar(c, simples(Material.BOOK)) >= 1 && contar(c, simples(Material.LAPIS_LAZULI)) >= 4) {
                    tirar(c, simples(Material.BOOK), 1);
                    tirar(c, simples(Material.LAPIS_LAZULI), 4);
                    l.add(livroEncantado(ci));
                } else if (contar(c, simples(Material.PAPER)) >= 3 && contar(c, simples(Material.LEATHER)) >= 1) {
                    tirar(c, simples(Material.PAPER), 3);
                    tirar(c, simples(Material.LEATHER), 1);
                    l.add(new ItemStack(Material.BOOK, rnd().nextDouble() < 0.05 * ci.nivel ? 2 : 1));
                } else if (contar(c, simples(Material.SUGAR_CANE)) >= 3) {
                    tirar(c, simples(Material.SUGAR_CANE), 3);
                    l.add(new ItemStack(Material.PAPER, quantidade(3, 3, mult)));
                } else {
                    return null;
                }
            }
            case CONSTRUTOR -> {
                if (!construir(c, ci, mult)) return null;
            }
            case DESEMPREGADO, SOLDADO, ARQUEIRO, CAVALEIRO -> { return null; }
        }
        return l;
    }

    // ---------- Mercador ----------

    /** Quantos itens valem 1 esmeralda na estrada (só o que está aqui é vendido). */
    private static int preco(Material m) {
        String n = m.name();
        if (n.endsWith("_LOG") || n.endsWith("_STEM")) return 16;
        if (n.endsWith("_SAPLING")) return 24;
        return switch (m) {
            case WHEAT -> 20;
            case CARROT -> 22;
            case POTATO -> 26;
            case BEETROOT -> 15;
            case WHEAT_SEEDS, BEETROOT_SEEDS, STICK, ROTTEN_FLESH -> 32;
            case COD -> 15;
            case SALMON -> 13;
            case INK_SAC -> 8;
            case COBBLESTONE -> 48;
            case COAL -> 15;
            case RAW_IRON -> 6;
            case IRON_INGOT -> 4;
            case RAW_COPPER -> 20;
            case COPPER_INGOT -> 12;
            case RAW_GOLD -> 4;
            case GOLD_INGOT -> 3;
            case REDSTONE -> 12;
            case LEATHER -> 6;
            case WHITE_WOOL -> 12;
            case EGG -> 16;
            case FEATHER, BONE -> 24;
            case STRING -> 14;
            case BEEF, CHICKEN -> 12;
            case PORKCHOP, MUTTON -> 8;
            case APPLE -> 16;
            case BREAD -> 6;
            default -> 0;
        };
    }

    /** Quanto de cada coisa fica guardado (o Mercador só vende o que passar disso). */
    private static int reserva(Material m) {
        String n = m.name();
        // Material de construção e de comida fica com folga para o Construtor e para a fome.
        if (m == Material.COBBLESTONE || n.endsWith("_LOG") || n.endsWith("_STEM")) return 192;
        return m.isEdible() ? 128 : 64;
    }

    /** Vende a sobra do depósito, a que tiver mais sobrando primeiro. @return as esmeraldas ganhas. */
    private int vender(Colonia c, Colonia.Cidadao ci, double mult) {
        int teto = quantidade(1, 1 + ci.nivel / 3, mult);
        Map<Material, Integer> tem = contarPorTipo(c, s -> preco(s.getType()) > 0 && Ingrediente.simples(s));
        List<Map.Entry<Material, Integer>> sobras = new ArrayList<>(tem.entrySet());
        sobras.sort((a, b) -> Integer.compare((b.getValue() - reserva(b.getKey())) / preco(b.getKey()),
                (a.getValue() - reserva(a.getKey())) / preco(a.getKey())));
        int ganhou = 0;
        for (Map.Entry<Material, Integer> e : sobras) {
            int lotes = (e.getValue() - reserva(e.getKey())) / preco(e.getKey());
            if (lotes <= 0) break;
            int n = Math.min(lotes, teto - ganhou);
            tirar(c, simples(e.getKey()), n * preco(e.getKey()));
            ganhou += n;
            if (ganhou >= teto) break;
        }
        return ganhou;
    }

    // ---------- Bibliotecário ----------

    /** Um livro com um encantamento ao acaso; o nível sobe com o nível do bibliotecário (raros só do nível 7 em diante). */
    private static ItemStack livroEncantado(Colonia.Cidadao ci) {
        List<org.bukkit.enchantments.Enchantment> comuns = new ArrayList<>(), raros = new ArrayList<>();
        for (org.bukkit.enchantments.Enchantment e : io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT)) {
            if (e.isCursed()) continue;
            if (e.isTreasure()) { if (e.isTradeable()) raros.add(e); }
            else if (e.isDiscoverable()) comuns.add(e);
        }
        List<org.bukkit.enchantments.Enchantment> de = ci.nivel >= 7 && !raros.isEmpty() && rnd().nextDouble() < 0.08 ? raros : comuns;
        ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
        if (de.isEmpty()) return livro;
        org.bukkit.enchantments.Enchantment e = de.get(rnd().nextInt(de.size()));
        int max = e.getMaxLevel();
        int nivel = 1 + (int) Math.floor((max - 1) * Math.min(1, (ci.nivel + rnd().nextInt(0, 5)) / 12.0) + 0.0001);
        livro.editMeta(org.bukkit.inventory.meta.EnchantmentStorageMeta.class, m -> m.addStoredEnchant(e, Math.max(1, Math.min(max, nivel)), true));
        return livro;
    }

    // ---------- Construtor ----------

    /** Uma casa por vez, só quando faltam camas. @return se trabalhou neste turno. */
    private boolean construir(Colonia c, Colonia.Cidadao ci, double mult) {
        if (c.obra == null) {
            if (c.camas >= c.maxCidadaos() || !c.varrida) return false;
            c.obra = obras.procurarLugar(c, chunksDaColonia(c));
            if (c.obra == null) {
                avisar(c, ci.nome + " (Construtor) não achou lugar para uma casa: precisa de um chão plano e livre de 9×9 no território.");
                return false;
            }
            Player dono = Bukkit.getPlayer(c.dono);
            if (dono != null) dono.sendMessage(Component.text("⌂ " + ci.nome + " começou uma casa nova em " + c.obra.x + ", " + c.obra.y + ", " + c.obra.z
                    + ". Deixe madeira, pedra, 3 vidros, 2 camas (ou 6 lãs) e uma luz no depósito.", Prefeituras.COR));
        }
        int feitos = obras.construir(c, c.obra, quantidade(8, 12, mult));
        if (Bukkit.getEntity(ci.entidade) instanceof Villager v && v.getWorld().getName().equals(c.obra.mundo)) {
            v.getPathfinder().moveTo(c.obra.centro(v.getWorld()), 0.6);
        }
        if (c.obra.falta != null) avisar(c, "A casa nova parou: falta " + c.obra.falta + " no depósito.");
        if (obras.pronta(c.obra)) {
            c.casas++;
            Location centro = c.obra.centro(c.world());
            centro.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, centro.clone().add(0, 2, 0), 40, 2, 1.5, 2, 0.2);
            centro.getWorld().playSound(centro, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            Player dono = Bukkit.getPlayer(c.dono);
            if (dono != null) dono.sendMessage(Component.text("⌂ " + ci.nome + " terminou uma casa nova! Mais 2 camas: novos moradores podem chegar.",
                    Prefeituras.COR, TextDecoration.BOLD));
            c.obra = null;
            varrer(c);
            salvar();
        }
        return feitos > 0;
    }

    private static int quantidade(int min, int max, double mult) {
        double v = rnd().nextInt(min, max + 1) * mult;
        int base = (int) Math.floor(v);
        return Math.max(1, base + (rnd().nextDouble() < v - base ? 1 : 0));
    }

    private static Material sortear(Map<Material, Integer> pesos) {
        int total = 0;
        for (int v : pesos.values()) total += v;
        int x = rnd().nextInt(Math.max(1, total));
        for (Map.Entry<Material, Integer> e : pesos.entrySet()) {
            x -= e.getValue();
            if (x < 0) return e.getKey();
        }
        return pesos.keySet().iterator().next();
    }

    private static void adicionarComQualidade(List<ItemStack> l, Material m, int qtd, int nivel) {
        Map<Qualidade, Integer> porQualidade = new EnumMap<>(Qualidade.class);
        for (int i = 0; i < qtd; i++) porQualidade.merge(Qualidade.sortear(nivel / 10.0), 1, Integer::sum);
        porQualidade.forEach((q, n) -> {
            ItemStack s = new ItemStack(m, n);
            q.aplicar(s);
            l.add(s);
        });
    }

    /** O cozinheiro faz o prato mais forte que der com o que tem no depósito (até o nível dele). */
    private ItemStack cozinhar(Colonia c, Colonia.Cidadao ci) {
        int max = plugin.settings().nivelMaximo;
        Prato[] pratos = Prato.values();
        for (int i = pratos.length - 1; i >= 0; i--) {
            Prato pr = pratos[i];
            if (pr.nivelNecessario(max) > ci.nivel * max / 10) continue;
            boolean tem = true;
            for (Prato.Ingrediente ing : pr.ingredientes()) {
                if (contar(c, teste(ing)) < ing.qtd()) { tem = false; break; }
            }
            if (!tem) continue;
            int boas = 0, total = 0;
            for (Prato.Ingrediente ing : pr.ingredientes()) {
                for (Qualidade q : tirar(c, teste(ing), ing.qtd())) {
                    total++;
                    if (q.ordinal() >= Qualidade.OTIMA.ordinal()) boas++;
                }
            }
            Qualidade q = Qualidade.sortear(ci.nivel / 10.0);
            if (total > 0 && boas * 2 >= total) q = q.acima();
            double fator = q.fator() * (1 + ci.nivel * 0.03);
            return plugin.cozinha().criar(pr, q, fator, ci.nome + " (colônia)");
        }
        return null;
    }

    private static Predicate<ItemStack> teste(Prato.Ingrediente ing) {
        if (ing.variedade() != null) return s -> Variedade.de(s) == ing.variedade() && !Variedade.ehSemente(s);
        if (ing.peixe() != null) return s -> PeixeRaro.de(s) == ing.peixe();
        return simples(ing.material());
    }

    /** Conserta um equipamento gasto do depósito. */
    private boolean consertar(Colonia c, Colonia.Cidadao ci) {
        for (Inventory inv : inventarios(c)) {
            for (ItemStack s : inv.getContents()) {
                if (s == null || !(s.getItemMeta() instanceof Damageable d) || !d.hasDamage()) continue;
                int maxDur = s.getType().getMaxDurability();
                if (maxDur <= 0) continue;
                int conserto = (int) Math.ceil(maxDur * (0.15 + 0.02 * ci.nivel));
                s.editMeta(Damageable.class, m -> m.setDamage(Math.max(0, m.getDamage() - conserto)));
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    //  Menus
    // =====================================================================

    public void abrir(Player p, Colonia c) {
        // Procura os postos de novo (o menu se atualiza sozinho quando terminar).
        if (System.currentTimeMillis() - c.ultimaVarredura > 3000) varrer(c);
        Tela t = new Tela(Tipo.PREFEITURA, c);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⌂ Colônia de " + c.nomeDono));
        desenharPrefeitura(t);
        p.openInventory(t.inventario);
    }

    /** /colonia: abre a sua colônia de qualquer lugar. */
    public void abrirMinha(Player p) {
        Colonia c = de(p.getUniqueId());
        if (c == null) {
            Territorio t = plugin.territorios().em(p.getLocation());
            if (t != null) c = de(t.dono());
        }
        if (c == null) {
            p.sendMessage(Component.text("⌂ Você não tem colônia. No seu território, jogue 1 bloco de esmeralda e 1 cama em cima de um sino.",
                    NamedTextColor.GOLD));
            return;
        }
        abrir(p, c);
    }

    private void desenharPrefeitura(Tela t) {
        Colonia c = t.colonia;
        Inventory inv = t.inventario;
        inv.clear();
        int alimentados = (int) Math.round(c.alimentados * 100);
        inv.setItem(P_INFO, item(Material.BELL, Component.text("⌂ Colônia de " + c.nomeDono, Prefeituras.COR, TextDecoration.BOLD), List.of(
                linha("Nível: ", c.nivel + " / " + Colonia.NIVEL_MAXIMO, NamedTextColor.WHITE),
                linha("Moradores: ", c.cidadaos.size() + " / " + c.maxCidadaos(), NamedTextColor.WHITE),
                linha("Camas: ", String.valueOf(c.camas), c.camas >= c.cidadaos.size() ? NamedTextColor.GREEN : NamedTextColor.RED),
                linha("Felicidade: ", Math.round(c.felicidade) + "% (produção ×" + String.format("%.2f", c.fatorFelicidade()).replace('.', ',') + ")",
                        c.felicidade >= 60 ? NamedTextColor.GREEN : c.felicidade >= 40 ? NamedTextColor.YELLOW : NamedTextColor.RED),
                linha("Comida: ", alimentados + "% atendida" + (c.comeuPratos ? " (com pratos!)" : ""), alimentados >= 100 ? NamedTextColor.GREEN : NamedTextColor.RED),
                linha("Casas erguidas: ", c.casas + (c.obra == null ? "" : "  (uma em obra: " + c.obra.passo * 100 / c.obra.total() + "%)"), NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Chega um morador novo a cada 4 min se", NamedTextColor.GRAY),
                Component.text("houver cama livre, comida e felicidade.", NamedTextColor.GRAY)), false));

        List<Component> dep = new ArrayList<>();
        dep.add(linha("Baús: ", c.depositos.size() + " / " + MAX_DEPOSITOS, NamedTextColor.WHITE));
        int livres = 0;
        for (Inventory i : inventarios(c)) for (ItemStack s : i.getContents()) if (s == null || s.isEmpty()) livres++;
        dep.add(linha("Espaços livres: ", String.valueOf(livres), livres > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
        dep.add(Component.empty());
        dep.add(Component.text("Os moradores guardam o que produzem", NamedTextColor.GRAY));
        dep.add(Component.text("aqui e pegam comida e materiais daqui.", NamedTextColor.GRAY));
        dep.add(Component.empty());
        dep.add(Component.text("» Clique e depois clique num baú perto", NamedTextColor.YELLOW));
        dep.add(Component.text("  Shift + clique: limpar a lista", NamedTextColor.DARK_GRAY));
        inv.setItem(P_DEPOSITO, item(Material.CHEST, Component.text("Depósito", NamedTextColor.GOLD, TextDecoration.BOLD), dep, false));

        List<Component> postos = new ArrayList<>();
        postos.add(Component.text(territorio(c) != null ? "Encontrados no território (" + c.censo.size() + " chunks):" : "Encontrados até " + Colonia.RAIO + " blocos da Prefeitura:", NamedTextColor.GRAY));
        for (Profissao p : Profissao.values()) {
            if (p == Profissao.DESEMPREGADO || (p.soldado() && p != Profissao.SOLDADO)) continue;
            int n = c.postos.getOrDefault(p, 0);
            postos.add(Component.text(" " + p.nomePosto() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(n + " (" + p.nome() + ")", n > 0 ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
        }
        postos.add(linha(" Água: ", c.agua >= 8 ? "sim" : "não", c.agua >= 8 ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
        postos.add(linha(" Plantações: ", String.valueOf(c.plantas.values().stream().mapToInt(Integer::intValue).sum()), NamedTextColor.WHITE));
        postos.add(Component.empty());
        postos.add(Component.text("Cada morador precisa do seu posto.", NamedTextColor.DARK_GRAY));
        inv.setItem(P_POSTOS, item(Material.COMPOSTER, Component.text("Postos de trabalho", NamedTextColor.GREEN, TextDecoration.BOLD), postos, false));

        List<Component> melhorar = new ArrayList<>();
        if (c.nivel >= Colonia.NIVEL_MAXIMO) {
            melhorar.add(Component.text("Nível máximo! Produção +20%.", NamedTextColor.GOLD));
        } else {
            melhorar.add(Component.text("Nível " + (c.nivel + 1) + ": até " + (c.nivel + 1) * 3 + " moradores", NamedTextColor.GRAY));
            for (Profissao p : Profissao.values()) {
                if (p.nivelColonia() == c.nivel + 1) melhorar.add(Component.text(" + libera " + p.nome(), p.cor()));
            }
            if (c.nivel + 1 == Colonia.NIVEL_MAXIMO) melhorar.add(Component.text(" + produção +20%", NamedTextColor.GOLD));
            melhorar.add(Component.empty());
            melhorar.add(Component.text("Custa (do depósito):", NamedTextColor.GRAY));
            CUSTOS.get(c.nivel - 1).forEach((m, q) -> {
                boolean ok = contar(c, simples(m)) >= q;
                melhorar.add(Component.text((ok ? " ✔ " : " ✖ ") + q + "x ", ok ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(Component.translatable(m.translationKey())));
            });
            melhorar.add(Component.empty());
            melhorar.add(Component.text("» Clique para melhorar", NamedTextColor.YELLOW));
        }
        inv.setItem(P_MELHORAR, item(Material.EMERALD_BLOCK, Component.text("Melhorar a colônia", NamedTextColor.GREEN, TextDecoration.BOLD), melhorar,
                c.nivel >= Colonia.NIVEL_MAXIMO));

        inv.setItem(P_AJUDA, item(Material.BOOK, Component.text("Como funciona", Prefeituras.COR, TextDecoration.BOLD), List.of(
                Component.text("1. Coloque camas e defina um baú de depósito.", NamedTextColor.GRAY),
                Component.text("2. Construa postos no seu território:", NamedTextColor.GRAY),
                Component.text("   composteira, barril, cortador de pedras,", NamedTextColor.WHITE),
                Component.text("   bancada de flechas, bancada de trabalho,", NamedTextColor.WHITE),
                Component.text("   mesa de cartografia, atril, Cozinha, Forja...", NamedTextColor.WHITE),
                Component.text("3. Dê um trabalho a cada morador.", NamedTextColor.GRAY),
                Component.text("4. Mantenha comida no depósito (pratos", NamedTextColor.GRAY),
                Component.text("   da Cozinha deixam todos felizes).", NamedTextColor.GRAY),
                Component.text("5. Atenda os pedidos (❗) dos moradores.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Eles trabalham de dia e dormem à noite.", NamedTextColor.DARK_GRAY)), false));

        int soldados = quantosSoldados(c);
        var reino = plugin.reinos().de(c.dono);
        inv.setItem(P_EXERCITO, item(Material.IRON_SWORD, Component.text("⚔ Exército", NamedTextColor.RED, TextDecoration.BOLD), List.of(
                linha("Soldados: ", soldados + " / " + vagasSoldados(c) + " vagas", NamedTextColor.WHITE),
                linha("Quartéis: ", c.postos.getOrDefault(Profissao.SOLDADO, 0) + " (bloco de alvo, " + Profissao.SOLDADOS_POR_QUARTEL + " vagas cada)", NamedTextColor.GRAY),
                linha("Reino: ", reino == null ? "nenhum" : reino.nome(), reino == null ? NamedTextColor.DARK_GRAY : reino.cor()),
                linha("Ordem: ", nomeOrdem(c), NamedTextColor.YELLOW),
                Component.empty(),
                Component.text("Dê a profissão Soldado ou Arqueiro a um morador.", NamedTextColor.GRAY),
                Component.text("Eles atacam monstros sempre e, na guerra,", NamedTextColor.GRAY),
                Component.text("os jogadores e soldados do reino inimigo.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique: trocar a ordem", NamedTextColor.YELLOW),
                Component.text("  (guardar / seguir você / atacar)", NamedTextColor.DARK_GRAY),
                Component.text("» Shift + clique: equipar com o melhor", NamedTextColor.YELLOW),
                Component.text("  do depósito (armas e armaduras)", NamedTextColor.DARK_GRAY)), false));

        for (int i = 0; i < c.cidadaos.size() && i < P_CIDADAOS.length; i++) {
            Colonia.Cidadao ci = c.cidadaos.get(i);
            boolean semPosto = ci.profissao.soldado() ? quantosSoldados(c) > vagasSoldados(c)
                    : ci.profissao != Profissao.DESEMPREGADO && c.quantos(ci.profissao) > c.postos.getOrDefault(ci.profissao, 0);
            List<Component> lore = new ArrayList<>();
            lore.add(linha("Profissão: ", ci.profissao.nome(), ci.profissao.cor()));
            lore.add(linha("Nível: ", ci.nivel + " / 10", NamedTextColor.WHITE));
            lore.add(linha("Jeito: ", nomesTracos(ci), NamedTextColor.AQUA));
            if (ci.pedido != null) lore.add(Component.text("❗ Pede " + ci.pedido.texto(), NamedTextColor.GOLD));
            else if (ci.humor > 0) lore.add(Component.text("☺ Contente", NamedTextColor.GREEN));
            else if (ci.humor < 0) lore.add(Component.text("☹ Chateado(a)", NamedTextColor.RED));
            if (ci.profissao == Profissao.CONSTRUTOR && c.obra != null && !semPosto) {
                lore.add(Component.text("⚒ Casa em obra: " + c.obra.passo * 100 / c.obra.total() + "%", NamedTextColor.GOLD));
            } else if (ci.profissao == Profissao.CONSTRUTOR && !semPosto && c.camas >= c.maxCidadaos()) {
                lore.add(Component.text("✔ Camas de sobra: nada para construir", NamedTextColor.GRAY));
            }
            if (semPosto) lore.add(Component.text("✖ Sem posto: precisa de " + ci.profissao.nomePosto(), NamedTextColor.RED));
            else if (ci.profissao == Profissao.FAZENDEIRO && c.plantas.isEmpty()) lore.add(Component.text("✖ Sem plantações por perto", NamedTextColor.RED));
            else if (ci.profissao == Profissao.PESCADOR && c.agua < 8) lore.add(Component.text("✖ Sem água por perto", NamedTextColor.RED));
            else if (ci.profissao.soldado()) lore.add(Component.text("⚔ Em serviço", NamedTextColor.RED));
            else if (ci.profissao != Profissao.DESEMPREGADO) lore.add(Component.text("✔ Trabalhando (de dia)", NamedTextColor.GREEN));
            if (ci.sumido > 0) lore.add(Component.text("? Longe ou num lugar não carregado", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para gerenciar", NamedTextColor.YELLOW));
            inv.setItem(P_CIDADAOS[i], item(ci.profissao.icone(), Component.text(ci.nome, NamedTextColor.YELLOW, TextDecoration.BOLD), lore, false));
        }
        inv.setItem(P_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        preencher(inv);
    }

    private void abrirCidadao(Player p, Colonia c, Colonia.Cidadao ci) {
        Tela t = new Tela(Tipo.CIDADAO, c);
        t.cidadao = ci.entidade;
        t.inventario = Bukkit.createInventory(t, 45, Component.text("⌂ " + ci.nome));
        desenharCidadao(t, ci);
        p.openInventory(t.inventario);
    }

    private void desenharCidadao(Tela t, Colonia.Cidadao ci) {
        Colonia c = t.colonia;
        Inventory inv = t.inventario;
        inv.clear();
        double prox = Colonia.Cidadao.xpParaProximo(ci.nivel);
        List<Component> info = new ArrayList<>(List.of(
                linha("Profissão: ", ci.profissao.nome(), ci.profissao.cor()),
                Component.text(ci.profissao.descricao(), NamedTextColor.GRAY),
                linha("Nível: ", ci.nivel + " / 10" + (ci.nivel < 10 ? "  (" + (int) ci.xp + "/" + (int) prox + " XP)" : ""), NamedTextColor.WHITE),
                linha("Produção: ", "+" + ci.nivel * 10 + "% pelo nível", NamedTextColor.GREEN),
                Component.empty(),
                Component.text("Jeito:", NamedTextColor.GRAY)));
        for (Traco tr : ci.tracos) {
            info.add(Component.text(" " + (tr.bom() ? "✦ " : "✧ ") + tr.nome() + ": ", tr.cor())
                    .append(Component.text(tr.descricao(), NamedTextColor.GRAY)));
        }
        if (ci.humor != 0) {
            info.add(Component.empty());
            info.add(ci.humor > 0 ? Component.text("☺ Contente: produz 15% a mais por um tempo.", NamedTextColor.GREEN)
                    : Component.text("☹ Chateado(a): produz 10% a menos por um tempo.", NamedTextColor.RED));
        }
        inv.setItem(C_INFO, item(ci.profissao.icone(), Component.text(ci.nome, NamedTextColor.YELLOW, TextDecoration.BOLD), info, false));
        if (ci.pedido != null) {
            inv.setItem(C_PEDIDO, item(ci.pedido.icone(), Component.text("❗ Pedido de " + ci.nome, NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    Component.text("\"" + ci.pedido.motivo() + "\"", NamedTextColor.WHITE, TextDecoration.ITALIC),
                    linha("Quer: ", ci.pedido.texto(), NamedTextColor.YELLOW),
                    linha("Prazo: ", ci.prazo + " min", ci.prazo > 10 ? NamedTextColor.GREEN : NamedTextColor.RED),
                    Component.empty(),
                    Component.text("Atender: colônia +8% de felicidade e", NamedTextColor.GRAY),
                    Component.text(ci.nome + " contente (+15% de produção).", NamedTextColor.GRAY),
                    Component.text("Esquecer: colônia -5% e " + ci.nome + " chateado(a).", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("» Clique para entregar (do seu inventário)", NamedTextColor.YELLOW)), true));
        } else {
            inv.setItem(C_PEDIDO, item(Material.PAPER, Component.text("Sem pedidos", NamedTextColor.GRAY), List.of(
                    Component.text("De vez em quando um morador pede alguma coisa:", NamedTextColor.DARK_GRAY),
                    Component.text("um ❗ aparece sobre a cabeça dele.", NamedTextColor.DARK_GRAY)), false));
        }
        Profissao[] todas = Profissao.values();
        for (int i = 0; i < todas.length && i < C_PROFISSOES.length; i++) {
            Profissao p = todas[i];
            boolean liberada = c.nivel >= p.nivelColonia();
            int postos = c.postos.getOrDefault(p.soldado() ? Profissao.SOLDADO : p, 0);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(p.descricao(), NamedTextColor.GRAY));
            if (p != Profissao.DESEMPREGADO) {
                lore.add(linha("Posto: ", p.nomePosto(), NamedTextColor.WHITE));
                lore.add(linha("Postos achados: ", postos + "  ·  trabalhando: " + c.quantos(p), postos > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            lore.add(Component.empty());
            if (!liberada) lore.add(Component.text("✖ Colônia nível " + p.nivelColonia(), NamedTextColor.RED));
            else if (p == ci.profissao) lore.add(Component.text("✔ Profissão atual", NamedTextColor.GREEN));
            else lore.add(Component.text("» Clique para mudar", NamedTextColor.YELLOW));
            inv.setItem(C_PROFISSOES[i], item(liberada ? p.icone() : Material.GRAY_DYE,
                    Component.text(p.nome(), liberada ? p.cor() : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore, p == ci.profissao));
        }
        inv.setItem(C_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(C_DISPENSAR, item(Material.RED_DYE, Component.text("Dispensar morador", NamedTextColor.RED), List.of(
                Component.text("Ele vira um aldeão comum e sai da colônia.", NamedTextColor.GRAY),
                Component.text("Shift + clique para confirmar.", NamedTextColor.DARK_GRAY)), false));
        preencher(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        Colonia c = t.colonia;
        if (!porDono.containsKey(c.dono)) { p.closeInventory(); return; }
        int slot = e.getSlot();
        if (t.tipo == Tipo.PREFEITURA) {
            if (slot == P_FECHAR) { p.closeInventory(); return; }
            if (!gerencia(p, c)) {
                if (slot != P_INFO) erro(p, "Só o dono da colônia e os membros do território podem mexer aqui.");
                return;
            }
            if (slot == P_DEPOSITO) {
                if (e.isShiftClick()) {
                    c.depositos.clear();
                    p.sendMessage(Component.text("⌂ Lista de depósitos limpa.", NamedTextColor.GRAY));
                    desenharPrefeitura(t);
                    return;
                }
                definindo.put(p.getUniqueId(), System.currentTimeMillis() + 30_000);
                p.closeInventory();
                p.sendMessage(Component.text("⌂ Clique com o botão direito num baú perto da Prefeitura (30s).", Prefeituras.COR));
            } else if (slot == P_MELHORAR) {
                melhorar(p, c, t);
            } else if (slot == P_EXERCITO) {
                if (e.isShiftClick()) {
                    int n = equiparTodos(c);
                    p.sendMessage(Component.text("⚔ " + n + (n == 1 ? " peça trocada" : " peças trocadas") + " pelo melhor do depósito.", NamedTextColor.RED));
                } else {
                    trocarOrdem(p, c);
                }
                desenharPrefeitura(t);
            } else {
                for (int i = 0; i < P_CIDADAOS.length && i < c.cidadaos.size(); i++) {
                    if (P_CIDADAOS[i] == slot) { abrirCidadao(p, c, c.cidadaos.get(i)); return; }
                }
            }
            return;
        }
        Colonia.Cidadao ci = cidadao(c, t.cidadao);
        if (ci == null) { p.closeInventory(); return; }
        if (slot == C_VOLTAR) { abrir(p, c); return; }
        if (slot == C_PEDIDO) {
            // Qualquer um ligado à colônia pode atender um pedido.
            if (ci.pedido == null) return;
            if (!daColonia(p, c)) { erro(p, "Só quem é da colônia pode atender os pedidos."); return; }
            entregarPedido(p, c, ci);
            desenharCidadao(t, ci);
            return;
        }
        if (!gerencia(p, c)) { erro(p, "Só o dono da colônia e os membros do território podem mexer aqui."); return; }
        if (slot == C_DISPENSAR) {
            if (!e.isShiftClick()) { erro(p, "Shift + clique para confirmar."); return; }
            cidadaos.remove(ci.entidade);
            c.cidadaos.remove(ci);
            if (Bukkit.getEntity(ci.entidade) instanceof Villager v) {
                v.getPersistentDataContainer().remove(kCidadao);
                v.customName(null);
            }
            p.sendMessage(Component.text("⌂ " + ci.nome + " saiu da colônia.", NamedTextColor.GRAY));
            salvar();
            abrir(p, c);
            return;
        }
        Profissao[] todas = Profissao.values();
        for (int i = 0; i < todas.length && i < C_PROFISSOES.length; i++) {
            if (C_PROFISSOES[i] != slot) continue;
            Profissao nova = todas[i];
            if (c.nivel < nova.nivelColonia()) { erro(p, "Libera no nível " + nova.nivelColonia() + " da colônia."); return; }
            if (nova == ci.profissao) return;
            if (nova.soldado() && !ci.profissao.soldado() && quantosSoldados(c) >= vagasSoldados(c)) {
                erro(p, "Faltam vagas no Quartel: cada bloco de alvo no território abriga " + Profissao.SOLDADOS_POR_QUARTEL + " soldados.");
                return;
            }
            definirProfissao(c, ci, nova);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            if (nova != Profissao.DESEMPREGADO && c.quantos(nova) > c.postos.getOrDefault(nova, 0)) {
                p.sendMessage(Component.text("⌂ Atenção: falta um posto (" + nova.nomePosto() + ") no território para " + ci.nome + " trabalhar.",
                        NamedTextColor.GOLD));
            }
            salvar();
            desenharCidadao(t, ci);
            return;
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private void melhorar(Player p, Colonia c, Tela t) {
        if (c.nivel >= Colonia.NIVEL_MAXIMO) return;
        Map<Material, Integer> custo = CUSTOS.get(c.nivel - 1);
        for (Map.Entry<Material, Integer> en : custo.entrySet()) {
            if (contar(c, simples(en.getKey())) < en.getValue()) {
                erro(p, "Faltam materiais no depósito.");
                return;
            }
        }
        custo.forEach((m, q) -> tirar(c, simples(m), q));
        c.nivel++;
        Location pref = c.prefeitura();
        if (pref != null) {
            pref.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pref.clone().add(0.5, 1.5, 0.5), 60, 0.6, 0.8, 0.6, 0.3);
            pref.getWorld().playSound(pref, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        p.sendMessage(Component.text("⌂ A colônia subiu para o nível " + c.nivel + "!", Prefeituras.COR, TextDecoration.BOLD));
        salvar();
        desenharPrefeitura(t);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoEscolherBau(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Long ate = definindo.get(e.getPlayer().getUniqueId());
        if (ate == null) return;
        Player p = e.getPlayer();
        if (System.currentTimeMillis() > ate) { definindo.remove(p.getUniqueId()); return; }
        Block b = e.getClickedBlock();
        if (b == null || (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST)) return;
        e.setCancelled(true);
        definindo.remove(p.getUniqueId());
        Colonia c = de(p.getUniqueId());
        if (c == null) {
            Territorio t = plugin.territorios().em(b.getLocation());
            if (t != null) c = de(t.dono());
        }
        if (c == null || !gerencia(p, c)) { erro(p, "Esse baú não é de uma colônia sua."); return; }
        if (!c.dentro(b.getLocation())) { erro(p, "O baú precisa estar a até " + Colonia.RAIO + " blocos da Prefeitura."); return; }
        if (c.depositos.size() >= MAX_DEPOSITOS) { erro(p, "No máximo " + MAX_DEPOSITOS + " baús de depósito (shift + clique no botão limpa a lista)."); return; }
        for (Location d : c.depositos) if (d.getBlock().equals(b)) { erro(p, "Esse baú já é um depósito."); return; }
        c.depositos.add(b.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1.2f);
        p.sendMessage(Component.text("⌂ Baú definido como depósito (" + c.depositos.size() + "/" + MAX_DEPOSITOS + ").", Prefeituras.COR));
        salvar();
    }

    // =====================================================================
    //  Eventos dos moradores
    // =====================================================================

    private UUID donoDe(Entity e) {
        return cidadaos.get(e.getUniqueId());
    }

    /** Dono da colônia de quem esse morador (ou soldado) é, ou null. */
    public UUID colonoDe(Entity e) {
        return donoDe(e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoInteragir(PlayerInteractEntityEvent e) {
        UUID dono = donoDe(e.getRightClicked());
        if (dono == null) return;
        e.setCancelled(true); // morador não faz comércio: abre o menu dele
        if (e.getHand() != EquipmentSlot.HAND) return;
        Colonia c = porDono.get(dono);
        if (c == null) return;
        Colonia.Cidadao ci = cidadao(c, e.getRightClicked().getUniqueId());
        if (ci != null) abrirCidadao(e.getPlayer(), c, ci);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTrocarProfissao(VillagerCareerChangeEvent e) {
        if (donoDe(e.getEntity()) != null && !trocandoProfissao.contains(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoCruzar(EntityBreedEvent e) {
        if (donoDe(e.getMother()) != null || donoDe(e.getFather()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        UUID dono = donoDe(e.getEntity());
        if (dono == null) return;
        Player atacante = e.getDamager() instanceof Player pa ? pa
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player ps ? ps : null;
        if (atacante == null) return;
        // Na guerra, soldados do reino inimigo podem ser atacados.
        if (e.getEntity() instanceof org.bukkit.entity.Skeleton && plugin.reinos().inimigosEmGuerra(atacante.getUniqueId(), dono)) return;
        Colonia c = porDono.get(dono);
        if (c == null || !gerencia(atacante, c)) e.setCancelled(true);
        else if (!atacante.isSneaking()) e.setCancelled(true); // dono não bate sem querer (agachado bate)
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        if (donoDe(e.getEntity()) != null) perderCidadao(e.getEntity().getUniqueId(), "morreu!");
    }

    @EventHandler(ignoreCancelled = true)
    public void aoTransformar(EntityTransformEvent e) {
        if (donoDe(e.getEntity()) != null) perderCidadao(e.getEntity().getUniqueId(), "foi transformado e saiu da colônia!");
    }

    // =====================================================================
    //  Exército: soldados
    // =====================================================================

    private static final NamespacedKey CHAVE_UNIFORME = new NamespacedKey("rpgatributos", "uniforme");
    private static final NamespacedKey CHAVE_INVOCACAO = new NamespacedKey("rpgatributos", "invocacao_dono");
    private int cicloSoldados;

    public int quantosSoldados(Colonia c) {
        return c.quantos(Profissao.SOLDADO) + c.quantos(Profissao.ARQUEIRO) + c.quantos(Profissao.CAVALEIRO);
    }

    private int vagasSoldados(Colonia c) {
        return c.postos.getOrDefault(Profissao.SOLDADO, 0) * Profissao.SOLDADOS_POR_QUARTEL;
    }

    private UUID reinoDe(UUID jogador) {
        var r = plugin.reinos().de(jogador);
        return r == null ? null : r.id();
    }

    private org.bukkit.Color corUniforme(Colonia c) {
        var r = plugin.reinos().de(c.dono);
        return org.bukkit.Color.fromRGB(r == null ? 0xB0B0B0 : r.cor().value());
    }

    private static boolean uniforme(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(CHAVE_UNIFORME);
    }

    private static ItemStack itemUniforme(Material m, org.bukkit.Color cor) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.setUnbreakable(true);
            if (cor != null && meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta lm) lm.setColor(cor);
            meta.getPersistentDataContainer().set(CHAVE_UNIFORME, PersistentDataType.BYTE, (byte) 1);
        });
        return i;
    }

    /** Troca a entidade do morador: aldeão ↔ soldado (mantém nome, nível e lugar). */
    private void trocarEntidade(Colonia c, Colonia.Cidadao ci, Entity velha) {
        Location onde = velha.getLocation();
        if (velha instanceof org.bukkit.entity.LivingEntity le) devolverEquipamento(c, le);
        tirarCavalo(c, velha);
        org.bukkit.entity.LivingEntity nova = ci.profissao.soldado() ? criarSoldado(c, ci, onde) : criarMorador(c, ci.nome, onde);
        cidadaos.remove(ci.entidade);
        velha.getPersistentDataContainer().remove(kCidadao);
        velha.remove();
        ci.entidade = nova.getUniqueId();
        cidadaos.put(ci.entidade, c.dono);
        onde.getWorld().spawnParticle(Particle.CLOUD, onde.clone().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        onde.getWorld().playSound(onde, Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
        salvar();
    }

    private org.bukkit.entity.Skeleton criarSoldado(Colonia c, Colonia.Cidadao ci, Location onde) {
        org.bukkit.Color cor = corUniforme(c);
        boolean arqueiro = ci.profissao == Profissao.ARQUEIRO;
        org.bukkit.entity.Skeleton soldado = onde.getWorld().spawn(onde, org.bukkit.entity.Skeleton.class, s -> {
            s.setShouldBurnInDay(false);
            s.setPersistent(true);
            s.setRemoveWhenFarAway(false);
            s.setCanPickupItems(false);
            s.getPersistentDataContainer().set(kCidadao, PersistentDataType.STRING, c.dono.toString());
            org.bukkit.inventory.EntityEquipment eq = s.getEquipment();
            eq.setItemInMainHand(itemUniforme(arqueiro ? Material.BOW : Material.STONE_SWORD, null));
            eq.setItemInOffHand(null);
            eq.setHelmet(itemUniforme(Material.LEATHER_HELMET, cor));
            eq.setChestplate(itemUniforme(Material.LEATHER_CHESTPLATE, cor));
            eq.setLeggings(itemUniforme(Material.LEATHER_LEGGINGS, cor));
            eq.setBoots(itemUniforme(Material.LEATHER_BOOTS, cor));
            for (EquipmentSlot sl : EquipmentSlot.values()) {
                try { eq.setDropChance(sl, 0); } catch (IllegalArgumentException ignored) { }
            }
            atributosSoldado(s, ci);
            nomearSoldado(c, ci, s);
        });
        if (ci.profissao == Profissao.CAVALEIRO) montar(c, soldado);
        return soldado;
    }

    // ---------- Cavaleiro ----------

    private boolean montariaDe(Entity e, Colonia c) {
        return e instanceof org.bukkit.entity.Horse && c.dono.toString().equals(e.getPersistentDataContainer().get(kMontaria, PersistentDataType.STRING));
    }

    /** O Cavaleiro sobe num cavalo novo da colônia (selado, manso, sem soltar nada). */
    private void montar(Colonia c, org.bukkit.entity.Skeleton s) {
        org.bukkit.entity.Horse cavalo = s.getWorld().spawn(s.getLocation(), org.bukkit.entity.Horse.class, h -> {
            h.setAdult();
            h.setTamed(true);
            h.setPersistent(true);
            h.setRemoveWhenFarAway(false);
            h.setColor(org.bukkit.entity.Horse.Color.values()[rnd().nextInt(org.bukkit.entity.Horse.Color.values().length)]);
            h.setStyle(org.bukkit.entity.Horse.Style.values()[rnd().nextInt(org.bukkit.entity.Horse.Style.values().length)]);
            h.getInventory().setSaddle(itemUniforme(Material.SADDLE, null));
            var vida = h.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (vida != null) { vida.setBaseValue(30); h.setHealth(30); }
            var vel = h.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
            if (vel != null) vel.setBaseValue(0.3);
            for (EquipmentSlot sl : EquipmentSlot.values()) {
                try { h.getEquipment().setDropChance(sl, 0); } catch (IllegalArgumentException ignored) { }
            }
            h.getPersistentDataContainer().set(kMontaria, PersistentDataType.STRING, c.dono.toString());
        });
        cavalo.addPassenger(s);
    }

    /** Some com o cavalo do Cavaleiro (a armadura de verdade dele volta para o depósito). */
    private void tirarCavalo(Colonia c, Entity cavaleiro) {
        Entity v = cavaleiro.getVehicle();
        if (v == null || !montariaDe(v, c)) return;
        if (v instanceof org.bukkit.entity.Horse h) {
            ItemStack armadura = h.getEquipment().getItem(EquipmentSlot.BODY);
            if (!armadura.isEmpty() && !uniforme(armadura) && !guardar(c, armadura)) h.getWorld().dropItemNaturally(h.getLocation(), armadura);
            h.getEquipment().setItem(EquipmentSlot.BODY, null);
        }
        v.eject();
        v.remove();
    }

    /** Cavaleiro a pé: sobe de volta num cavalo da colônia por perto, ou pega outro (a cada 2 min). */
    private void remontar(Colonia c, Colonia.Cidadao ci, org.bukkit.entity.Skeleton s) {
        if (s.getVehicle() != null) return;
        for (Entity e : s.getNearbyEntities(12, 6, 12)) {
            if (montariaDe(e, c) && e.getPassengers().isEmpty()) {
                s.teleport(e.getLocation());
                e.addPassenger(s);
                return;
            }
        }
        long agora = System.currentTimeMillis();
        Long quando = proximoCavalo.get(ci.entidade);
        if (quando == null) { proximoCavalo.put(ci.entidade, agora + 120_000); return; }
        if (agora < quando) return;
        proximoCavalo.remove(ci.entidade);
        montar(c, s);
    }

    private static void atributosSoldado(org.bukkit.entity.LivingEntity s, Colonia.Cidadao ci) {
        double jeito = ci.tem(Traco.CORAGEM) ? 1.2 : ci.tem(Traco.MEDO) ? 0.85 : 1;
        boolean cavaleiro = ci.profissao == Profissao.CAVALEIRO;
        var vida = s.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(Math.round((20 + 3 * ci.nivel + (cavaleiro ? 10 : 0)) * jeito));
            s.setHealth(vida.getValue());
        }
        var dano = s.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (dano != null) dano.setBaseValue((2 + 0.5 * ci.nivel + (cavaleiro ? 1.5 : 0)) * jeito);
    }

    private void nomearSoldado(Colonia c, Colonia.Cidadao ci, Entity s) {
        var r = plugin.reinos().de(c.dono);
        Component n = Component.text("⚔ " + ci.nome, r == null ? NamedTextColor.WHITE : r.cor())
                .append(Component.text(" · " + ci.profissao.nome() + " Nv " + ci.nivel, NamedTextColor.GRAY));
        s.customName(ci.pedido == null ? n : n.append(Component.text(" ❗", NamedTextColor.GOLD, TextDecoration.BOLD)));
    }

    /** Equipamento de verdade (não o uniforme) volta para o depósito. */
    private void devolverEquipamento(Colonia c, org.bukkit.entity.LivingEntity le) {
        var eq = le.getEquipment();
        if (eq == null) return;
        for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack i = eq.getItem(sl);
            if (i.isEmpty() || uniforme(i)) continue;
            if (!guardar(c, i)) le.getWorld().dropItemNaturally(le.getLocation(), i);
            eq.setItem(sl, null);
        }
    }

    /** A cor do reino mudou (entrou, saiu, desfez): pinta os uniformes de novo. */
    public void atualizarUniformes(UUID dono) {
        Colonia c = porDono.get(dono);
        if (c == null) return;
        org.bukkit.Color cor = corUniforme(c);
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (!ci.profissao.soldado() || !(Bukkit.getEntity(ci.entidade) instanceof org.bukkit.entity.LivingEntity s)) continue;
            var eq = s.getEquipment();
            if (eq == null) continue;
            for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack i = eq.getItem(sl);
                if (!uniforme(i)) continue;
                i.editMeta(org.bukkit.inventory.meta.LeatherArmorMeta.class, m -> m.setColor(cor));
                eq.setItem(sl, i);
            }
            nomearSoldado(c, ci, s);
        }
    }

    /** Veste os soldados com as melhores armas e armaduras do depósito. */
    private int equiparTodos(Colonia c) {
        int trocas = 0;
        for (Colonia.Cidadao ci : c.cidadaos) {
            if (!ci.profissao.soldado() || !(Bukkit.getEntity(ci.entidade) instanceof org.bukkit.entity.LivingEntity s)) continue;
            var eq = s.getEquipment();
            if (eq == null) continue;
            boolean arqueiro = ci.profissao == Profissao.ARQUEIRO;
            trocas += trocarPeloMelhor(c, eq, EquipmentSlot.HAND, i -> arqueiro ? i.getType() == Material.BOW
                    : i.getType().name().endsWith("_SWORD") || i.getType().name().endsWith("_AXE"),
                    i -> arqueiro ? 1 + i.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.POWER) : atributo(i, org.bukkit.attribute.Attribute.ATTACK_DAMAGE));
            for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                String fim = switch (sl) { case HEAD -> "_HELMET"; case CHEST -> "_CHESTPLATE"; case LEGS -> "_LEGGINGS"; default -> "_BOOTS"; };
                trocas += trocarPeloMelhor(c, eq, sl, i -> i.getType().name().endsWith(fim), i -> atributo(i, org.bukkit.attribute.Attribute.ARMOR));
            }
        }
        return trocas;
    }

    private int trocarPeloMelhor(Colonia c, org.bukkit.inventory.EntityEquipment eq, EquipmentSlot sl, Predicate<ItemStack> serve,
                                 java.util.function.ToDoubleFunction<ItemStack> valor) {
        ItemStack atual = eq.getItem(sl);
        double melhor = atual.isEmpty() || uniforme(atual) ? (uniforme(atual) ? valor.applyAsDouble(atual) * 0.5 : 0) : valor.applyAsDouble(atual);
        Inventory origem = null;
        int indice = -1;
        for (Inventory inv : inventarios(c)) {
            ItemStack[] cont = inv.getContents();
            for (int i = 0; i < cont.length; i++) {
                ItemStack s = cont[i];
                if (s == null || s.isEmpty() || !serve.test(s)) continue;
                double v = valor.applyAsDouble(s);
                if (v > melhor) { melhor = v; origem = inv; indice = i; }
            }
        }
        if (origem == null) return 0;
        ItemStack novo = origem.getItem(indice).clone();
        novo.setAmount(1);
        ItemStack resto = origem.getItem(indice);
        resto.setAmount(resto.getAmount() - 1);
        origem.setItem(indice, resto.getAmount() > 0 ? resto : null);
        if (!atual.isEmpty() && !uniforme(atual)) guardar(c, atual);
        eq.setItem(sl, novo);
        eq.setDropChance(sl, 1f); // equipamento de verdade cai quando o soldado morre
        return 1;
    }

    /** Valor de um atributo no item (os bônus da forja contam). */
    private static double atributo(ItemStack i, org.bukkit.attribute.Attribute a) {
        var meta = i.getItemMeta();
        var mods = meta != null && meta.hasAttributeModifiers() ? meta.getAttributeModifiers(a) : i.getType().getDefaultAttributeModifiers().get(a);
        double v = 0;
        if (mods != null) for (var m : mods) if (m.getOperation() == org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER) v += m.getAmount();
        return v;
    }

    /** Soldados de um reino perto de um lugar (para a captura de Marcos). */
    public int soldadosPerto(UUID reino, Location l, double raio) {
        int n = 0;
        for (Colonia c : porDono.values()) {
            if (!reino.equals(reinoDe(c.dono))) continue;
            for (Colonia.Cidadao ci : c.cidadaos) {
                if (!ci.profissao.soldado()) continue;
                Entity e = Bukkit.getEntity(ci.entidade);
                if (e != null && e.isValid() && e.getWorld().equals(l.getWorld()) && e.getLocation().distanceSquared(l) <= raio * raio) n++;
            }
        }
        return n;
    }

    /** Alguém que um soldado dessa colônia deve atacar? */
    private boolean inimigoDoSoldado(Colonia c, Entity e) {
        if (!(e instanceof org.bukkit.entity.LivingEntity le) || !le.isValid() || le.isDead()) return false;
        UUID outro = donoDe(e);
        if (outro != null) {
            // Morador de outra colônia: só soldados, e só se os reinos estão em guerra agora.
            if (outro.equals(c.dono) || !(e instanceof org.bukkit.entity.Skeleton)) return false;
            return plugin.reinos().inimigosEmGuerra(c.dono, outro);
        }
        if (e instanceof Player p) return p.getGameMode() == org.bukkit.GameMode.SURVIVAL && plugin.reinos().inimigosEmGuerra(c.dono, p.getUniqueId());
        String invocador = e.getPersistentDataContainer().get(CHAVE_INVOCACAO, PersistentDataType.STRING);
        if (invocador != null) {
            try { return plugin.reinos().inimigosEmGuerra(c.dono, UUID.fromString(invocador)); }
            catch (IllegalArgumentException ex) { return false; }
        }
        return e instanceof org.bukkit.entity.Enemy;
    }

    private org.bukkit.entity.LivingEntity procurarInimigo(Colonia c, org.bukkit.entity.LivingEntity s, double raio) {
        org.bukkit.entity.LivingEntity melhor = null;
        double d = Double.MAX_VALUE;
        for (Entity e : s.getNearbyEntities(raio, raio / 2, raio)) {
            if (!inimigoDoSoldado(c, e)) continue;
            double dist = e.getLocation().distanceSquared(s.getLocation());
            if (dist < d) { d = dist; melhor = (org.bukkit.entity.LivingEntity) e; }
        }
        return melhor;
    }

    /** Soldados: escolhem alvo e cumprem a ordem (a cada meio segundo). */
    public void tickSoldados() {
        cicloSoldados++;
        for (Colonia c : porDono.values()) {
            Location pref = c.prefeitura();
            if (pref == null || !pref.isChunkLoaded()) continue;
            Player seguido = c.seguindo == null ? null : Bukkit.getPlayer(c.seguindo);
            for (Colonia.Cidadao ci : List.copyOf(c.cidadaos)) {
                Entity ent = Bukkit.getEntity(ci.entidade);
                // Trocou de profissão enquanto estava longe: troca a entidade agora.
                if (!ci.profissao.soldado()) {
                    if (ent instanceof org.bukkit.entity.Skeleton) {
                        trocarEntidade(c, ci, ent);
                        definirProfissao(c, ci, ci.profissao);
                    }
                    continue;
                }
                if (ent instanceof Villager) { trocarEntidade(c, ci, ent); continue; }
                if (!(ent instanceof org.bukkit.entity.Skeleton s) || !s.isValid()) continue;
                org.bukkit.entity.LivingEntity alvo = s.getTarget();
                if (alvo == null || !inimigoDoSoldado(c, alvo) || alvo.getLocation().distanceSquared(s.getLocation()) > 28 * 28) {
                    alvo = procurarInimigo(c, s, 16);
                    s.setTarget(alvo);
                }
                if (alvo != null) continue;
                // Sem inimigo: descansa (se cura) e cumpre a ordem.
                if (cicloSoldados % 4 == 0) {
                    var max = s.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (max != null && s.getHealth() < max.getValue()) s.setHealth(Math.min(max.getValue(), s.getHealth() + 1));
                }
                Location destino = null;
                double perto = 4;
                switch (c.ordem) {
                    case GUARDAR -> {
                        if (s.getWorld().equals(pref.getWorld()) && s.getLocation().distanceSquared(pref) > 16 * 16) {
                            destino = pref.clone().add(rnd().nextInt(-6, 7), 0, rnd().nextInt(-6, 7));
                        }
                    }
                    case SEGUIR -> {
                        if (seguido == null || seguido.isDead()) break;
                        if (!seguido.getWorld().equals(s.getWorld()) || seguido.getLocation().distanceSquared(s.getLocation()) > 40 * 40) {
                            s.teleport(lugarLivre(seguido.getLocation()));
                        } else if (seguido.getLocation().distanceSquared(s.getLocation()) > 16) {
                            destino = seguido.getLocation();
                        }
                    }
                    case ATACAR -> {
                        if (c.alvoAtaque != null && c.alvoAtaque.getWorld().equals(s.getWorld())) destino = c.alvoAtaque;
                        perto = 3;
                    }
                }
                if (destino != null && destino.distanceSquared(s.getLocation()) > perto * perto) s.getPathfinder().moveTo(destino, 1.1);
            }
        }
    }

    /** Clique no botão do exército: troca a ordem (Guardar → Seguir você → Atacar → Guardar). */
    private void trocarOrdem(Player p, Colonia c) {
        Colonia.Ordem nova = switch (c.ordem) {
            case GUARDAR -> Colonia.Ordem.SEGUIR;
            case SEGUIR -> Colonia.Ordem.ATACAR;
            case ATACAR -> Colonia.Ordem.GUARDAR;
        };
        if (nova == Colonia.Ordem.ATACAR) {
            Location alvo = marcoInimigoMaisPerto(c);
            if (alvo == null) {
                p.sendMessage(Component.text("⚔ Atacar só vale numa guerra em andamento: ninguém para atacar agora.", NamedTextColor.GRAY));
                nova = Colonia.Ordem.GUARDAR;
            } else {
                c.alvoAtaque = alvo;
            }
        }
        c.ordem = nova;
        c.seguindo = nova == Colonia.Ordem.SEGUIR ? p.getUniqueId() : null;
        p.sendMessage(Component.text("⚔ Ordem do exército: " + nomeOrdem(c), NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.4f, 1.5f);
    }

    private String nomeOrdem(Colonia c) {
        return switch (c.ordem) {
            case GUARDAR -> "guardar a colônia";
            case SEGUIR -> {
                Player p = c.seguindo == null ? null : Bukkit.getPlayer(c.seguindo);
                yield "seguir " + (p == null ? "?" : p.getName());
            }
            case ATACAR -> "atacar o Marco inimigo";
        };
    }

    private Location marcoInimigoMaisPerto(Colonia c) {
        UUID meu = reinoDe(c.dono);
        Location pref = c.prefeitura();
        if (meu == null || pref == null) return null;
        Location melhor = null;
        double d = Double.MAX_VALUE;
        for (UUID inimigo : plugin.reinos().inimigosAgora(meu)) {
            for (var r : plugin.reinos().todos()) {
                if (!r.id().equals(inimigo)) continue;
                for (UUID m : r.membros().keySet()) {
                    Territorio t = plugin.territorios().de(m);
                    Location marco = t == null ? null : t.marco();
                    if (marco == null || !marco.getWorld().equals(pref.getWorld())) continue;
                    double dist = marco.distanceSquared(pref);
                    if (dist < d) { d = dist; melhor = marco.clone().add(0.5, 1, 0.5); }
                }
            }
        }
        return melhor;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoMirar(org.bukkit.event.entity.EntityTargetLivingEntityEvent e) {
        UUID dono = donoDe(e.getEntity());
        if (dono != null && e.getEntity() instanceof org.bukkit.entity.Skeleton) {
            Colonia c = porDono.get(dono);
            if (c == null || (e.getTarget() != null && !inimigoDoSoldado(c, e.getTarget()))) e.setCancelled(true);
            return;
        }
        // Golens da vila não atacam os soldados.
        if (e.getTarget() != null && donoDe(e.getTarget()) != null && e.getEntity() instanceof org.bukkit.entity.IronGolem) e.setCancelled(true);
    }

    /** Soldado (ou a flecha dele) só machuca inimigo. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSoldadoAtacar(EntityDamageByEntityEvent e) {
        Entity atacante = e.getDamager();
        if (atacante instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) atacante = atirador;
        UUID dono = donoDe(atacante);
        if (dono == null || !(atacante instanceof org.bukkit.entity.Skeleton)) return;
        Colonia c = porDono.get(dono);
        if (c == null || !inimigoDoSoldado(c, e.getEntity())) e.setCancelled(true);
    }

    /** Quem um soldado derrotou ganha XP; na guerra, o abate vale pontos (antes do morador sair da lista). */
    @EventHandler(priority = EventPriority.LOW)
    public void aoAbater(EntityDeathEvent e) {
        if (!(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent ultimo)) return;
        Entity atacante = ultimo.getDamager();
        if (atacante instanceof Projectile pr && pr.getShooter() instanceof Entity atirador) atacante = atirador;
        UUID donoVitima = donoDe(e.getEntity());
        UUID donoAtacante = donoDe(atacante);
        // Soldado morto na guerra: ponto para quem matou (jogador ou soldado).
        if (donoVitima != null && e.getEntity() instanceof org.bukkit.entity.Skeleton) {
            UUID quem = atacante instanceof Player p ? p.getUniqueId() : donoAtacante;
            UUID ra = quem == null ? null : reinoDe(quem), rv = reinoDe(donoVitima);
            if (ra != null && rv != null) plugin.reinos().pontuarAbate(ra, rv, 1);
        }
        if (donoAtacante == null || !(atacante instanceof org.bukkit.entity.Skeleton)) return;
        Colonia c = porDono.get(donoAtacante);
        Colonia.Cidadao ci = c == null ? null : cidadao(c, atacante.getUniqueId());
        if (ci == null) return;
        int antes = ci.nivel;
        ganharXp(c, ci, e.getEntity() instanceof Player ? 30 : 8);
        if (ci.nivel != antes && atacante instanceof org.bukkit.entity.LivingEntity s) {
            atributosSoldado(s, ci);
            nomearSoldado(c, ci, s);
        }
    }

    // =====================================================================
    //  Salvar e carregar
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection sec = y.getConfigurationSection("colonias");
        if (sec == null) return;
        for (String chave : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(chave);
            if (s == null) continue;
            UUID dono;
            try { dono = UUID.fromString(chave); } catch (IllegalArgumentException ex) { continue; }
            Colonia c = new Colonia(dono, s.getString("nome", "?"), s.getString("mundo", "world"), s.getInt("x"), s.getInt("y"), s.getInt("z"));
            c.nivel = Math.max(1, Math.min(Colonia.NIVEL_MAXIMO, s.getInt("nivel", 1)));
            c.felicidade = s.getDouble("felicidade", 60);
            c.proximaChegada = System.currentTimeMillis() + CHEGADA_MS;
            World w = c.world();
            for (String d : s.getStringList("depositos")) {
                String[] p = d.split(",");
                if (p.length == 3 && w != null) {
                    try { c.depositos.add(new Location(w, Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]))); }
                    catch (NumberFormatException ignored) { }
                }
            }
            for (String linha : s.getStringList("cidadaos")) {
                String[] p = linha.split(";");
                if (p.length < 5) continue;
                try {
                    UUID id = UUID.fromString(p[0]);
                    Profissao prof = Profissao.porId(p[2]);
                    c.cidadaos.add(new Colonia.Cidadao(id, p[1], prof == null ? Profissao.DESEMPREGADO : prof,
                            Integer.parseInt(p[3]), Double.parseDouble(p[4])));
                    cidadaos.put(id, dono);
                } catch (IllegalArgumentException ignored) { }
            }
            porDono.put(dono, c);
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Colonia c : porDono.values()) {
            String b = "colonias." + c.dono + ".";
            y.set(b + "nome", c.nomeDono);
            y.set(b + "mundo", c.mundo);
            y.set(b + "x", c.x);
            y.set(b + "y", c.y);
            y.set(b + "z", c.z);
            y.set(b + "nivel", c.nivel);
            y.set(b + "felicidade", Math.round(c.felicidade * 10) / 10.0);
            List<String> deps = new ArrayList<>();
            for (Location d : c.depositos) deps.add(d.getBlockX() + "," + d.getBlockY() + "," + d.getBlockZ());
            y.set(b + "depositos", deps);
            List<String> cid = new ArrayList<>();
            for (Colonia.Cidadao ci : c.cidadaos) {
                cid.add(ci.entidade + ";" + ci.nome + ";" + ci.profissao.name() + ";" + ci.nivel + ";" + Math.round(ci.xp * 10) / 10.0);
            }
            y.set(b + "cidadaos", cid);
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não foi possível salvar colonias.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Admin
    // =====================================================================

    /** /rpgadmin colono [qtd]: moradores chegam na hora na sua colônia. */
    public int chegarAgora(Player p, int qtd) {
        Colonia c = de(p.getUniqueId());
        if (c == null) return -1;
        for (int i = 0; i < qtd; i++) chegar(c);
        salvar();
        return qtd;
    }

    /** /rpgadmin turnocolonia: um turno de trabalho agora (com varredura). */
    public boolean turnoAgora(Player p) {
        Colonia c = de(p.getUniqueId());
        if (c == null) return false;
        varrer(c);
        Bukkit.getScheduler().runTaskLater(plugin, () -> turno(c), 10L);
        return true;
    }

    /** /rpgadmin nivelcolonia <n> */
    public boolean definirNivel(Player p, int nivel) {
        Colonia c = de(p.getUniqueId());
        if (c == null) return false;
        c.nivel = Math.max(1, Math.min(Colonia.NIVEL_MAXIMO, nivel));
        salvar();
        return true;
    }

    // =====================================================================
    //  Ajudantes
    // =====================================================================

    private static Component linha(String rotulo, String valor, net.kyori.adventure.text.format.TextColor cor) {
        return Component.text(rotulo, NamedTextColor.GRAY).append(Component.text(valor, cor));
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilhar) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }

    private static void preencher(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
