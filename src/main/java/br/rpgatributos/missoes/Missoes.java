package br.rpgatributos.missoes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Mercadores;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.DadosForja;
import br.rpgatributos.forja.Raridade;
import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Missões dos aldeões: agachar + clique direito num aldeão com profissão mostra
 * 3 pedidos do dia. Cumprir dá esmeraldas, XP, às vezes material raro e
 * reputação (descontos de verdade no comércio com ele e com os vizinhos).
 */
public final class Missoes implements Listener {

    public static final int MAX_ATIVAS = 3;
    private static final int[] S_OFERTAS = {20, 22, 24};
    private static final int[] S_ATIVAS = {38, 40, 42};
    private static final int S_INFO = 4;
    private static final int S_FECHAR = 49;

    /** Modelo de pedido: o tipo, o alvo, a faixa de quantidade e o "valor" (pesa na recompensa). */
    private record Modelo(Missao.Tipo tipo, String alvo, int min, int max, int valor) {}

    /** Uma missão aceita pelo jogador. */
    private record Ativa(UUID aldeao, int indice, Missao missao, int progresso) {
        String serializar() {
            return aldeao + "|" + indice + "|" + missao.serializar() + "|" + progresso;
        }

        static Ativa ler(String t) {
            String[] p = t.split("\\|");
            if (p.length != 4) return null;
            try {
                Missao m = Missao.ler(p[2]);
                return m == null ? null : new Ativa(UUID.fromString(p[0]), Integer.parseInt(p[1]), m, Integer.parseInt(p[3]));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        UUID aldeao;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kAtivas, kDia, kOfertas, kFeitas;

    public Missoes(RPGAtributos plugin) {
        this.plugin = plugin;
        kAtivas = new NamespacedKey(plugin, "missoes");
        kDia = new NamespacedKey(plugin, "missoes_dia");
        kOfertas = new NamespacedKey(plugin, "missoes_ofertas");
        kFeitas = new NamespacedKey(plugin, "missoes_feitas");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Pedidos por profissão
    // =====================================================================

    private static Modelo e(String material, int min, int max, int valor) {
        return new Modelo(Missao.Tipo.ENTREGAR, material, min, max, valor);
    }

    private static Modelo c(EntityType tipo, int min, int max, int valor) {
        return new Modelo(Missao.Tipo.CACAR, tipo.name(), min, max, valor);
    }

    private static Modelo f(Categoria cat) {
        return new Modelo(Missao.Tipo.FORJAR, cat.name(), 1, 1, 0);
    }

    private static List<Modelo> modelos(Villager.Profession p) {
        if (p == Villager.Profession.FARMER) return List.of(e("WHEAT", 24, 40, 3), e("CARROT", 24, 40, 3), e("POTATO", 24, 40, 3),
                e("BEETROOT", 16, 32, 3), e("PUMPKIN", 8, 16, 4), e("MELON_SLICE", 24, 48, 3), e("BREAD", 12, 24, 4));
        if (p == Villager.Profession.FISHERMAN) return List.of(e("COD", 8, 16, 4), e("SALMON", 6, 12, 5),
                e("TROPICAL_FISH", 2, 5, 6), c(EntityType.DROWNED, 4, 8, 6));
        if (p == Villager.Profession.BUTCHER) return List.of(e("BEEF", 8, 16, 3), e("PORKCHOP", 8, 16, 3),
                e("CHICKEN", 8, 16, 3), e("MUTTON", 8, 16, 3), e("RABBIT", 3, 6, 5));
        if (p == Villager.Profession.SHEPHERD) return List.of(e("WHITE_WOOL", 16, 32, 3), e("SHEARS", 1, 1, 3),
                e("WHITE_DYE", 8, 16, 3), c(EntityType.SHEEP, 3, 5, 2));
        if (p == Villager.Profession.FLETCHER) return List.of(e("ARROW", 24, 48, 3), e("FEATHER", 12, 24, 3),
                e("FLINT", 12, 24, 3), c(EntityType.SKELETON, 6, 12, 5), c(EntityType.SPIDER, 6, 10, 5));
        if (p == Villager.Profession.LIBRARIAN) return List.of(e("BOOK", 4, 10, 5), e("PAPER", 24, 48, 3),
                e("INK_SAC", 6, 12, 4), e("BOOKSHELF", 2, 4, 6));
        if (p == Villager.Profession.CARTOGRAPHER) return List.of(e("COMPASS", 1, 3, 5), e("GLASS_PANE", 24, 48, 3),
                e("PAPER", 24, 40, 3), c(EntityType.CREEPER, 4, 8, 6));
        if (p == Villager.Profession.CLERIC) return List.of(e("ROTTEN_FLESH", 24, 40, 3), e("BONE", 16, 32, 3),
                e("SPIDER_EYE", 6, 12, 4), e("GUNPOWDER", 6, 12, 5), c(EntityType.ZOMBIE, 8, 16, 5),
                c(EntityType.SKELETON, 6, 12, 5), c(EntityType.WITCH, 1, 3, 8), c(EntityType.ENDERMAN, 2, 4, 8));
        if (p == Villager.Profession.ARMORER) return List.of(e("IRON_INGOT", 12, 24, 5), e("COAL", 24, 40, 3),
                e("LAVA_BUCKET", 1, 1, 4), f(Categoria.CAPACETE), f(Categoria.PEITORAL), f(Categoria.CALCA), f(Categoria.BOTAS));
        if (p == Villager.Profession.WEAPONSMITH) return List.of(e("IRON_INGOT", 12, 24, 5), e("COAL", 24, 40, 3),
                f(Categoria.ESPADA), f(Categoria.MACHADO), c(EntityType.ZOMBIE, 10, 16, 5));
        if (p == Villager.Profession.TOOLSMITH) return List.of(e("IRON_INGOT", 12, 24, 5), e("COAL", 24, 40, 3),
                f(Categoria.PICARETA), f(Categoria.PA), f(Categoria.ENXADA));
        if (p == Villager.Profession.MASON) return List.of(e("STONE", 48, 64, 2), e("CLAY_BALL", 16, 32, 3),
                e("BRICK", 24, 48, 3), e("QUARTZ", 12, 24, 4));
        if (p == Villager.Profession.LEATHERWORKER) return List.of(e("LEATHER", 12, 24, 4), e("RABBIT_HIDE", 6, 12, 4),
                c(EntityType.COW, 4, 8, 2));
        return List.of();
    }

    private static String nomeProfissao(Villager.Profession p) {
        Map<Villager.Profession, String> nomes = Map.ofEntries(
                Map.entry(Villager.Profession.FARMER, "Fazendeiro"), Map.entry(Villager.Profession.FISHERMAN, "Pescador"),
                Map.entry(Villager.Profession.BUTCHER, "Açougueiro"), Map.entry(Villager.Profession.SHEPHERD, "Pastor"),
                Map.entry(Villager.Profession.FLETCHER, "Flecheiro"), Map.entry(Villager.Profession.LIBRARIAN, "Bibliotecário"),
                Map.entry(Villager.Profession.CARTOGRAPHER, "Cartógrafo"), Map.entry(Villager.Profession.CLERIC, "Clérigo"),
                Map.entry(Villager.Profession.ARMORER, "Armeiro"), Map.entry(Villager.Profession.WEAPONSMITH, "Ferreiro de Armas"),
                Map.entry(Villager.Profession.TOOLSMITH, "Ferreiro de Ferramentas"), Map.entry(Villager.Profession.MASON, "Pedreiro"),
                Map.entry(Villager.Profession.LEATHERWORKER, "Coureiro"));
        return nomes.getOrDefault(p, "Aldeão");
    }

    private Missao criar(Modelo m) {
        int qtd = m.min() == m.max() ? m.min() : rnd().nextInt(m.min(), m.max() + 1);
        return switch (m.tipo()) {
            case ENTREGAR -> new Missao(m.tipo(), m.alvo(), qtd, 2 + m.valor(), 15 + 5 * m.valor(), "");
            case CACAR -> new Missao(m.tipo(), m.alvo(), qtd, 4 + m.valor(), 40 + 10 * m.valor(), rnd().nextDouble() < 0.1 ? "PEDRA" : "");
            case FORJAR -> {
                // Raro (mais comum) a Épico; Épico paga bem mais.
                Raridade r = rnd().nextDouble() < 0.7 ? Raridade.RARO : Raridade.EPICO;
                int i = r.ordinal();
                String extra = rnd().nextDouble() < 0.3 ? "FRAGMENTO" : rnd().nextDouble() < 0.15 ? "PEDRA" : "";
                yield new Missao(m.tipo(), m.alvo() + ":" + r.id(), 1, 10 + 6 * i, 80 + 40 * i, extra);
            }
        };
    }

    /** Os 3 pedidos de hoje desse aldeão (gera de novo a cada dia do jogo). */
    private List<Missao> ofertas(Villager v) {
        PersistentDataContainer pdc = v.getPersistentDataContainer();
        long hoje = v.getWorld().getFullTime() / 24000;
        Long dia = pdc.get(kDia, PersistentDataType.LONG);
        String salvo = pdc.get(kOfertas, PersistentDataType.STRING);
        if (dia != null && dia == hoje && salvo != null) {
            List<Missao> l = new ArrayList<>();
            for (String s : salvo.split("\\|")) {
                Missao m = Missao.ler(s);
                if (m != null) l.add(m);
            }
            return l;
        }
        List<Modelo> modelos = new ArrayList<>(modelos(v.getProfession()));
        java.util.Collections.shuffle(modelos);
        List<Missao> novas = new ArrayList<>();
        StringJoiner j = new StringJoiner("|");
        for (int i = 0; i < Math.min(3, modelos.size()); i++) {
            Missao m = criar(modelos.get(i));
            novas.add(m);
            j.add(m.serializar());
        }
        pdc.set(kDia, PersistentDataType.LONG, hoje);
        pdc.set(kOfertas, PersistentDataType.STRING, j.toString());
        pdc.remove(kFeitas);
        return novas;
    }

    private Set<String> feitas(Villager v) {
        String t = v.getPersistentDataContainer().get(kFeitas, PersistentDataType.STRING);
        Set<String> s = new HashSet<>();
        if (t != null && !t.isEmpty()) s.addAll(List.of(t.split(",")));
        return s;
    }

    private void marcarFeita(Villager v, UUID jogador, int indice) {
        Set<String> s = feitas(v);
        s.add(jogador + ":" + indice);
        v.getPersistentDataContainer().set(kFeitas, PersistentDataType.STRING, String.join(",", s));
    }

    // =====================================================================
    //  Missões do jogador
    // =====================================================================

    private List<Ativa> ativas(Player p) {
        List<Ativa> l = new ArrayList<>();
        String t = p.getPersistentDataContainer().get(kAtivas, PersistentDataType.STRING);
        if (t == null || t.isEmpty()) return l;
        for (String s : t.split("#")) {
            Ativa a = Ativa.ler(s);
            if (a != null) l.add(a);
        }
        return l;
    }

    private void salvar(Player p, List<Ativa> l) {
        StringJoiner j = new StringJoiner("#");
        for (Ativa a : l) j.add(a.serializar());
        p.getPersistentDataContainer().set(kAtivas, PersistentDataType.STRING, j.toString());
    }

    /** Progresso das missões de caça. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        List<Ativa> l = ativas(p);
        boolean mudou = false;
        for (int i = 0; i < l.size(); i++) {
            Ativa a = l.get(i);
            if (a.missao().tipo() != Missao.Tipo.CACAR || a.missao().entidade() != e.getEntityType()
                    || a.progresso() >= a.missao().quantidade()) continue;
            Ativa nova = new Ativa(a.aldeao(), a.indice(), a.missao(), a.progresso() + 1);
            l.set(i, nova);
            mudou = true;
            boolean pronta = nova.progresso() >= nova.missao().quantidade();
            p.sendActionBar(Component.text("✉ Missão: ", NamedTextColor.GREEN).append(nova.missao().descricao())
                    .append(Component.text("  " + nova.progresso() + "/" + nova.missao().quantidade()
                            + (pronta ? "  ✔ volte ao aldeão!" : ""), pronta ? NamedTextColor.GOLD : NamedTextColor.GRAY)));
        }
        if (mudou) salvar(p, l);
    }

    // =====================================================================
    //  Telas
    // =====================================================================

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void aoClicarAldeao(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Villager v) || !e.getPlayer().isSneaking()) return;
        if (v.getPersistentDataContainer().has(Mercadores.CHAVE_NPC) || modelos(v.getProfession()).isEmpty()) return;
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) abrir(e.getPlayer(), v);
    }

    public void abrir(Player p, Villager v) {
        Tela t = new Tela();
        t.aldeao = v.getUniqueId();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✉ Pedidos: " + nomeProfissao(v.getProfession())));
        desenhar(p, v, t.inventario);
        p.openInventory(t.inventario);
        p.playSound(v.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
    }

    private void desenhar(Player p, Villager v, Inventory inv) {
        inv.clear();
        Reputation rep = v.getReputation(p.getUniqueId());
        int reputacao = rep.getReputation(ReputationType.MINOR_POSITIVE) + rep.getReputation(ReputationType.MAJOR_POSITIVE) * 5;
        inv.setItem(S_INFO, item(Material.EMERALD, Component.text("✉ " + nomeProfissao(v.getProfession()), NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                Component.text("Pedidos novos todo dia (do jogo).", NamedTextColor.GRAY),
                Component.text("Cumprir pedidos melhora sua reputação:", NamedTextColor.GRAY),
                Component.text("este aldeão e os vizinhos dão desconto.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Reputação com ele: " + reputacao, NamedTextColor.WHITE),
                Component.text("Missões aceitas: " + ativas(p).size() + " / " + MAX_ATIVAS, NamedTextColor.WHITE)), false));

        List<Missao> ofertas = ofertas(v);
        Set<String> feitas = feitas(v);
        List<Ativa> minhas = ativas(p);
        for (int i = 0; i < ofertas.size() && i < S_OFERTAS.length; i++) {
            Missao m = ofertas.get(i);
            int idx = i;
            boolean aceita = minhas.stream().anyMatch(a -> a.aldeao().equals(v.getUniqueId()) && a.indice() == idx);
            boolean feita = feitas.contains(p.getUniqueId() + ":" + i);
            List<Component> lore = new ArrayList<>();
            lore.add(m.descricao());
            lore.add(Component.empty());
            lore.add(Component.text("Recompensa: ", NamedTextColor.GRAY).append(Component.text(m.recompensaTexto(), NamedTextColor.GREEN)));
            lore.add(Component.empty());
            if (feita) lore.add(Component.text("✔ Você já cumpriu hoje", NamedTextColor.DARK_GREEN));
            else if (aceita) lore.add(Component.text("Aceita (veja abaixo)", NamedTextColor.YELLOW));
            else lore.add(Component.text("» Clique para aceitar", NamedTextColor.YELLOW));
            inv.setItem(S_OFERTAS[i], item(feita ? Material.LIME_DYE : m.icone(), Component.text("Pedido " + (i + 1), NamedTextColor.GOLD, TextDecoration.BOLD), lore, aceita));
        }

        int s = 0;
        for (Ativa a : minhas) {
            if (!a.aldeao().equals(v.getUniqueId()) || s >= S_ATIVAS.length) continue;
            List<Component> lore = new ArrayList<>();
            lore.add(a.missao().descricao());
            if (a.missao().tipo() == Missao.Tipo.CACAR) {
                lore.add(Component.text("Progresso: " + a.progresso() + " / " + a.missao().quantidade(), NamedTextColor.WHITE));
            }
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para entregar", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift + clique: abandonar", NamedTextColor.RED));
            inv.setItem(S_ATIVAS[s++], item(Material.WRITABLE_BOOK, Component.text("Sua missão", NamedTextColor.AQUA, TextDecoration.BOLD), lore, false));
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (topo.getHolder(false) instanceof TelaLista) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player p && e.getRawSlot() < topo.getSize()) cliqueLista(p, e);
            return;
        }
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
        if (!(Bukkit.getEntity(t.aldeao) instanceof Villager v) || !v.isValid()) {
            p.closeInventory();
            return;
        }
        for (int i = 0; i < S_OFERTAS.length; i++) if (S_OFERTAS[i] == e.getSlot()) { aceitar(p, v, i); desenhar(p, v, topo); return; }
        int s = 0;
        for (Ativa a : ativas(p)) {
            if (!a.aldeao().equals(v.getUniqueId()) || s >= S_ATIVAS.length) continue;
            if (S_ATIVAS[s++] != e.getSlot()) continue;
            if (e.isShiftClick()) abandonar(p, a);
            else entregar(p, v, a);
            desenhar(p, v, topo);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        Object h = e.getView().getTopInventory().getHolder(false);
        if (h instanceof Tela || h instanceof TelaLista) e.setCancelled(true);
    }

    private void aceitar(Player p, Villager v, int indice) {
        List<Missao> ofertas = ofertas(v);
        if (indice >= ofertas.size()) return;
        List<Ativa> l = ativas(p);
        if (feitas(v).contains(p.getUniqueId() + ":" + indice)) {
            erro(p, "Você já cumpriu esse pedido hoje. Volte amanhã!");
            return;
        }
        if (l.stream().anyMatch(a -> a.aldeao().equals(v.getUniqueId()) && a.indice() == indice)) return;
        if (l.size() >= MAX_ATIVAS) {
            erro(p, "Você já tem " + MAX_ATIVAS + " missões. Cumpra ou abandone uma (/missoes).");
            return;
        }
        l.add(new Ativa(v.getUniqueId(), indice, ofertas.get(indice), 0));
        salvar(p, l);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        p.sendMessage(Component.text("✉ Missão aceita: ", NamedTextColor.GREEN).append(ofertas.get(indice).descricao()));
    }

    private void abandonar(Player p, Ativa a) {
        List<Ativa> l = ativas(p);
        l.removeIf(x -> x.aldeao().equals(a.aldeao()) && x.indice() == a.indice());
        salvar(p, l);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        p.sendMessage(Component.text("Missão abandonada.", NamedTextColor.GRAY));
    }

    private void entregar(Player p, Villager v, Ativa a) {
        Missao m = a.missao();
        PlayerInventory inv = p.getInventory();
        switch (m.tipo()) {
            case ENTREGAR -> {
                Material mat = m.material();
                if (mat == null || !inv.containsAtLeast(new ItemStack(mat), m.quantidade())) {
                    erro(p, "Você ainda não tem os itens.");
                    return;
                }
                inv.removeItem(new ItemStack(mat, m.quantidade()));
            }
            case CACAR -> {
                if (a.progresso() < m.quantidade()) {
                    erro(p, "Ainda faltam " + (m.quantidade() - a.progresso()) + ".");
                    return;
                }
            }
            case FORJAR -> {
                int slot = itemForjado(inv, m.categoria(), m.raridadeMinima());
                if (slot < 0) {
                    erro(p, "Você não tem um item forjado desse tipo e raridade no inventário.");
                    return;
                }
                inv.setItem(slot, null);
            }
        }
        List<Ativa> l = ativas(p);
        l.removeIf(x -> x.aldeao().equals(a.aldeao()) && x.indice() == a.indice());
        salvar(p, l);
        marcarFeita(v, p.getUniqueId(), a.indice());
        recompensar(p, v, m);
    }

    private int itemForjado(PlayerInventory inv, Categoria cat, Raridade minima) {
        ItemStack[] itens = inv.getStorageContents();
        for (int i = 0; i < itens.length; i++) {
            ItemStack it = itens[i];
            if (it == null || Categoria.de(it.getType()) != cat) continue;
            DadosForja d = plugin.forja().ler(it);
            if (d != null && d.raridade().peloMenos(minima)) return i;
        }
        return -1;
    }

    private void recompensar(Player p, Villager v, Missao m) {
        List<ItemStack> premio = new ArrayList<>();
        premio.add(new ItemStack(Material.EMERALD, m.esmeraldas()));
        if ("PEDRA".equals(m.extra())) premio.add(Raro.PEDRA_DE_PROTECAO.criar());
        if ("FRAGMENTO".equals(m.extra())) premio.add(Raro.FRAGMENTO_DE_FORJA.criar());
        for (ItemStack i : premio) {
            p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        }
        boolean ganha = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
        switch (m.tipo()) {
            case ENTREGAR -> p.getWorld().spawn(v.getLocation().add(0, 1, 0), ExperienceOrb.class, o -> o.setExperience(m.xp()));
            case CACAR -> { if (ganha) plugin.stats().darXp(p, Skill.COMBATE, m.xp()); }
            case FORJAR -> { if (ganha) plugin.stats().darXp(p, Skill.FERRARIA, m.xp()); }
        }
        // Reputação: este aldeão e os vizinhos passam a dar desconto.
        melhorarReputacao(v, p.getUniqueId(), 15);
        for (Entity vizinho : v.getNearbyEntities(32, 16, 32)) {
            if (vizinho instanceof Villager outro) melhorarReputacao(outro, p.getUniqueId(), 5);
        }
        plugin.titulos().registrar(p, "missoes", 1);
        v.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, v.getLocation().add(0, 2, 0), 15, 0.4, 0.3, 0.4);
        p.playSound(v.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        p.sendMessage(Component.text("✉ Missão cumprida! ", NamedTextColor.GREEN)
                .append(Component.text("Recompensa: " + m.recompensaTexto() + ".", NamedTextColor.GRAY)));
    }

    private static void melhorarReputacao(Villager v, UUID jogador, int quanto) {
        Reputation rep = v.getReputation(jogador);
        rep.setReputation(ReputationType.MINOR_POSITIVE, Math.min(200, rep.getReputation(ReputationType.MINOR_POSITIVE) + quanto));
        v.setReputation(jogador, rep);
    }

    // =====================================================================
    //  /missoes: todas as missões do jogador
    // =====================================================================

    private static final class TelaLista implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public void abrirLista(Player p) {
        TelaLista t = new TelaLista();
        t.inventario = Bukkit.createInventory(t, 27, Component.text("✉ Suas missões"));
        desenharLista(p, t.inventario);
        p.openInventory(t.inventario);
    }

    private void desenharLista(Player p, Inventory inv) {
        inv.clear();
        List<Ativa> l = ativas(p);
        int[] slots = {11, 13, 15};
        for (int i = 0; i < l.size() && i < slots.length; i++) {
            Ativa a = l.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(a.missao().descricao());
            if (a.missao().tipo() == Missao.Tipo.CACAR) {
                lore.add(Component.text("Progresso: " + a.progresso() + " / " + a.missao().quantidade(), NamedTextColor.WHITE));
            }
            Entity aldeao = Bukkit.getEntity(a.aldeao());
            lore.add(Component.empty());
            lore.add(aldeao == null ? Component.text("Aldeão: longe (chunk descarregado)", NamedTextColor.DARK_GRAY)
                    : Component.text("Aldeão em " + aldeao.getLocation().getBlockX() + ", " + aldeao.getLocation().getBlockY()
                    + ", " + aldeao.getLocation().getBlockZ(), NamedTextColor.GRAY));
            lore.add(Component.text("Recompensa: " + a.missao().recompensaTexto(), NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text("Entregue agachando + clicando no aldeão.", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift + clique: abandonar", NamedTextColor.RED));
            inv.setItem(slots[i], item(a.missao().icone(), Component.text("Missão " + (i + 1), NamedTextColor.AQUA, TextDecoration.BOLD), lore, false));
        }
        if (l.isEmpty()) {
            inv.setItem(13, item(Material.PAPER, Component.text("Nenhuma missão", NamedTextColor.GRAY), List.of(
                    Component.text("Agache e clique com o botão direito", NamedTextColor.DARK_GRAY),
                    Component.text("num aldeão com profissão.", NamedTextColor.DARK_GRAY)), false));
        }
        ItemStack vidro = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private void cliqueLista(Player p, InventoryClickEvent e) {
        if (!e.isShiftClick()) return;
        int[] slots = {11, 13, 15};
        List<Ativa> l = ativas(p);
        for (int i = 0; i < l.size() && i < slots.length; i++) {
            if (slots[i] != e.getSlot()) continue;
            abandonar(p, l.get(i));
            desenharLista(p, e.getView().getTopInventory());
            return;
        }
    }

    // =====================================================================
    //  Ajudantes
    // =====================================================================

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
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
}
