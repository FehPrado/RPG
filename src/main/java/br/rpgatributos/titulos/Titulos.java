package br.rpgatributos.titulos;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.arcano.Receita;
import br.rpgatributos.aventura.Chefe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Títulos: contadores de conquistas, desbloqueio, título em uso, bônus e a tela /titulos.
 * Tudo fica salvo no próprio jogador.
 */
public final class Titulos implements Listener {

    /** As cinco primeiras linhas (menos o item de informação): 44 títulos por página. */
    private static final int[] SLOTS = {
            0, 1, 2, 3, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int S_ANTERIOR = 45, S_PROXIMA = 53;
    private static final int S_INFO = 4;
    private static final int S_REMOVER = 48;
    private static final int S_FECHAR = 50;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        int pagina;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kContadores, kDesbloqueados, kAtivo;
    private final NamespacedKey kVida, kDano, kArmadura, kVelocidade, kQuebra, kQueda, kFolego;
    /** Título em uso de cada jogador online (cache para não ler o PDC a cada XP ganho). */
    private final Map<UUID, Optional<Titulo>> ativos = new HashMap<>();

    public Titulos(RPGAtributos plugin) {
        this.plugin = plugin;
        kContadores = new NamespacedKey(plugin, "conquistas");
        kDesbloqueados = new NamespacedKey(plugin, "titulos");
        kAtivo = new NamespacedKey(plugin, "titulo_ativo");
        kVida = new NamespacedKey(plugin, "titulo_vida");
        kDano = new NamespacedKey(plugin, "titulo_dano");
        kArmadura = new NamespacedKey(plugin, "titulo_armadura");
        kVelocidade = new NamespacedKey(plugin, "titulo_velocidade");
        kQuebra = new NamespacedKey(plugin, "titulo_quebra");
        kQueda = new NamespacedKey(plugin, "titulo_queda");
        kFolego = new NamespacedKey(plugin, "titulo_folego");
    }

    // =====================================================================
    //  Contadores de conquistas
    // =====================================================================

    private Map<String, Integer> contadores(Player p) {
        Map<String, Integer> m = new LinkedHashMap<>();
        String texto = p.getPersistentDataContainer().get(kContadores, PersistentDataType.STRING);
        if (texto == null || texto.isEmpty()) return m;
        for (String parte : texto.split(";")) {
            String[] kv = parte.split("=");
            if (kv.length != 2) continue;
            try {
                m.put(kv[0], Integer.parseInt(kv[1]));
            } catch (NumberFormatException ignored) { }
        }
        return m;
    }

    private void salvarContadores(Player p, Map<String, Integer> m) {
        StringJoiner j = new StringJoiner(";");
        m.forEach((k, v) -> j.add(k + "=" + v));
        p.getPersistentDataContainer().set(kContadores, PersistentDataType.STRING, j.toString());
    }

    public int contador(Player p, String id) {
        return contadores(p).getOrDefault(id, 0);
    }

    /** Soma ao contador e confere se algum título foi desbloqueado. */
    public void registrar(Player p, String id, int quantidade) {
        Map<String, Integer> m = contadores(p);
        m.merge(id, quantidade, Integer::sum);
        salvarContadores(p, m);
        verificar(p);
    }

    /** O contador passa a ser pelo menos {@code valor} (ex.: quantas magias secretas já descobriu). */
    public void definirMinimo(Player p, String id, int valor) {
        Map<String, Integer> m = contadores(p);
        if (m.getOrDefault(id, 0) >= valor) return;
        m.put(id, valor);
        salvarContadores(p, m);
        verificar(p);
    }

    // =====================================================================
    //  Desbloqueio
    // =====================================================================

    public Set<Titulo> desbloqueados(Player p) {
        Set<Titulo> s = EnumSet.noneOf(Titulo.class);
        String texto = p.getPersistentDataContainer().get(kDesbloqueados, PersistentDataType.STRING);
        if (texto == null || texto.isEmpty()) return s;
        for (String n : texto.split(",")) {
            Titulo t = Titulo.porNome(n);
            if (t != null) s.add(t);
        }
        return s;
    }

    private void salvarDesbloqueados(Player p, Set<Titulo> s) {
        StringJoiner j = new StringJoiner(",");
        for (Titulo t : s) j.add(t.name());
        p.getPersistentDataContainer().set(kDesbloqueados, PersistentDataType.STRING, j.toString());
    }

    public boolean cumpre(Player p, Titulo t) {
        Titulo.Requisito r = t.requisito();
        StatsManager st = plugin.stats();
        int max = plugin.settings().nivelMaximo;
        return switch (r.tipo()) {
            case NIVEL -> st.getNivel(p, r.skill()) >= Math.ceil(r.fracao() * max);
            case NIVEL_TOTAL -> st.nivelTotal(p) >= Math.ceil(r.fracao() * max * Skill.values().length);
            case ALGUM_MAXIMO -> {
                for (Skill s : Skill.values()) if (st.getNivel(p, s) >= max) yield true;
                yield false;
            }
            case TODOS_MAXIMO -> {
                for (Skill s : Skill.values()) if (st.getNivel(p, s) < max) yield false;
                yield true;
            }
            case CONTADOR -> contador(p, r.contador()) >= r.quantidade();
            case TODOS_CHEFES -> {
                Map<String, Integer> m = contadores(p);
                for (Chefe c : Chefe.values()) if (m.getOrDefault("chefe_" + c.id(), 0) < 1) yield false;
                yield true;
            }
            case TODAS_SECRETAS -> contador(p, "secretas") >= Receita.values().length;
        };
    }

    /** Desbloqueia todos os títulos cujos requisitos o jogador já cumpre. */
    public void verificar(Player p) {
        Set<Titulo> tem = desbloqueados(p);
        List<Titulo> novos = new ArrayList<>();
        for (Titulo t : Titulo.values()) {
            if (!tem.contains(t) && cumpre(p, t)) novos.add(t);
        }
        if (novos.isEmpty()) return;
        tem.addAll(novos);
        salvarDesbloqueados(p, tem);
        for (Titulo t : novos) anunciar(p, t);
    }

    private void anunciar(Player p, Titulo t) {
        p.showTitle(Title.title(
                Component.text("« " + t.nome() + " »", t.cor(), TextDecoration.BOLD),
                Component.text("Novo título desbloqueado! (/titulos)", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.3);
        p.sendMessage(Component.text("✦ Título desbloqueado: ", NamedTextColor.GOLD)
                .append(Component.text(t.nome(), t.cor(), TextDecoration.BOLD))
                .append(Component.text(" (" + t.bonus().texto() + "). Use em /titulos", NamedTextColor.GRAY)));
        if (t.anunciado()) {
            for (Player outro : plugin.getServer().getOnlinePlayers()) {
                if (outro == p) continue;
                outro.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" conquistou o título ", NamedTextColor.GRAY))
                        .append(Component.text("« " + t.nome() + " »", t.cor(), TextDecoration.BOLD))
                        .append(Component.text("!", NamedTextColor.GRAY)));
            }
        }
    }

    // =====================================================================
    //  Título em uso e bônus
    // =====================================================================

    public Titulo ativo(Player p) {
        return ativos.computeIfAbsent(p.getUniqueId(), id -> {
            Titulo t = Titulo.porNome(p.getPersistentDataContainer().getOrDefault(kAtivo, PersistentDataType.STRING, ""));
            return Optional.ofNullable(t != null && desbloqueados(p).contains(t) ? t : null);
        }).orElse(null);
    }

    public void usar(Player p, Titulo t) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (t == null) pdc.remove(kAtivo);
        else pdc.set(kAtivo, PersistentDataType.STRING, t.name());
        ativos.put(p.getUniqueId(), Optional.ofNullable(t));
        aplicarBonus(p);
        plugin.tags().atualizarTexto(p);
        plugin.tags().garantir(p);
    }

    public void aplicarBonus(Player p) {
        Titulo t = ativo(p);
        Titulo.Bonus b = t == null ? null : t.bonus();
        definir(p, Attribute.MAX_HEALTH, kVida, b == null ? 0 : b.vida(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.ATTACK_DAMAGE, kDano, b == null ? 0 : b.dano(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.ARMOR, kArmadura, b == null ? 0 : b.armadura(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MOVEMENT_SPEED, kVelocidade, b == null ? 0 : b.velocidade(), AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.BLOCK_BREAK_SPEED, kQuebra, b == null ? 0 : b.quebra(), AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.SAFE_FALL_DISTANCE, kQueda, b == null ? 0 : b.queda(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.OXYGEN_BONUS, kFolego, b == null ? 0 : b.folego(), AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void definir(Player p, Attribute atributo, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(atributo);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        if (atributo == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    public double multiplicadorXp(Player p, Skill s) {
        Titulo t = ativo(p);
        return t == null ? 1 : t.bonus().multiplicadorXp(s);
    }

    public double bonusMana(Player p) {
        Titulo t = ativo(p);
        return t == null ? 0 : t.bonus().mana();
    }

    public double bonusRegenMana(Player p) {
        Titulo t = ativo(p);
        return t == null ? 0 : t.bonus().regenMana();
    }

    /** Efeitos de poção vindos do título (usado pelo sistema de passivos). */
    public Map<PotionEffectType, Integer> efeitos(Player p) {
        Titulo t = ativo(p);
        return t != null && t.bonus().heroi() ? Map.of(PotionEffectType.HERO_OF_THE_VILLAGE, 0) : Map.of();
    }

    /** Linha do título acima do nome (ou null). */
    public Component linhaDaTag(Player p) {
        Titulo t = ativo(p);
        return t == null ? null : Component.text("« " + t.nome() + " »", t.cor());
    }

    // =====================================================================
    //  Tela /titulos
    // =====================================================================

    public void abrirMenu(Player p) {
        verificar(p);
        Tela tela = new Tela();
        tela.inventario = Bukkit.createInventory(tela, 54, Component.text("✦ Títulos"));
        desenhar(p, tela);
        p.openInventory(tela.inventario);
    }

    private void desenhar(Player p, Tela tela) {
        Inventory inv = tela.inventario;
        inv.clear();
        int paginas = Math.max(1, (Titulo.values().length + SLOTS.length - 1) / SLOTS.length);
        tela.pagina = Math.max(0, Math.min(paginas - 1, tela.pagina));
        int inicio = tela.pagina * SLOTS.length;
        Set<Titulo> tem = desbloqueados(p);
        Titulo usando = ativo(p);
        Map<String, Integer> cont = contadores(p);
        int max = plugin.settings().nivelMaximo;

        inv.setItem(S_INFO, item(Material.NAME_TAG, Component.text("Seus títulos", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Desbloqueados: " + tem.size() + " / " + Titulo.values().length, NamedTextColor.WHITE),
                Component.text("Em uso: ", NamedTextColor.GRAY).append(usando == null
                        ? Component.text("nenhum", NamedTextColor.DARK_GRAY)
                        : Component.text(usando.nome(), usando.cor())),
                Component.empty(),
                Component.text("Só o título em uso dá bônus.", NamedTextColor.DARK_GRAY),
                Component.text("Ele aparece acima da sua cabeça.", NamedTextColor.DARK_GRAY)), false));

        Titulo[] todos = Titulo.values();
        for (int i = 0; i < SLOTS.length && inicio + i < todos.length; i++) {
            Titulo t = todos[inicio + i];
            boolean desbloqueado = tem.contains(t);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((desbloqueado ? "✔ " : "✖ ") + t.requisito().descrever(max),
                    desbloqueado ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (!desbloqueado && t.requisito().tipo() == Titulo.Requisito.Tipo.CONTADOR && t.requisito().quantidade() > 1) {
                lore.add(Component.text("  Progresso: " + Math.min(cont.getOrDefault(t.requisito().contador(), 0),
                        t.requisito().quantidade()) + " / " + t.requisito().quantidade(), NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Bônus: ", NamedTextColor.GRAY).append(Component.text(t.bonus().texto(), NamedTextColor.AQUA)));
            lore.add(Component.empty());
            if (t == usando) lore.add(Component.text("✔ Em uso", NamedTextColor.GREEN));
            else if (desbloqueado) lore.add(Component.text("» Clique para usar", NamedTextColor.YELLOW));
            Component nome = desbloqueado ? Component.text("« " + t.nome() + " »", t.cor(), TextDecoration.BOLD)
                    : Component.text("« " + t.nome() + " »", NamedTextColor.DARK_GRAY);
            inv.setItem(SLOTS[i], item(desbloqueado ? Material.NAME_TAG : Material.GRAY_DYE, nome, lore, t == usando));
        }
        inv.setItem(S_REMOVER, item(Material.RED_DYE, Component.text("Não usar título", NamedTextColor.RED), List.of(), false));
        if (tela.pagina > 0) inv.setItem(S_ANTERIOR, item(Material.ARROW, Component.text("« Página " + tela.pagina, NamedTextColor.YELLOW), List.of(), false));
        if (tela.pagina < paginas - 1) inv.setItem(S_PROXIMA, item(Material.ARROW, Component.text("Página " + (tela.pagina + 2) + " »", NamedTextColor.YELLOW), List.of(), false));
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));

        ItemStack vidro = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela tela)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        if (slot == S_REMOVER) {
            usar(p, null);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            desenhar(p, tela);
            return;
        }
        if (slot == S_ANTERIOR || slot == S_PROXIMA) {
            tela.pagina += slot == S_PROXIMA ? 1 : -1;
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1f);
            desenhar(p, tela);
            return;
        }
        Titulo[] todos = Titulo.values();
        int inicio = tela.pagina * SLOTS.length;
        for (int i = 0; i < SLOTS.length && inicio + i < todos.length; i++) {
            if (SLOTS[i] != slot) continue;
            Titulo t = todos[inicio + i];
            if (!desbloqueados(p).contains(t)) {
                p.sendMessage(Component.text("Você ainda não desbloqueou esse título.", NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                return;
            }
            usar(p, t);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 0.8f, 1.2f);
            desenhar(p, tela);
            return;
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        verificar(e.getPlayer());
        aplicarBonus(e.getPlayer());
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        ativos.remove(e.getPlayer().getUniqueId());
    }

    /** Todos que estavam perto quando o Dragão do Fim morreu ganham a conquista. */
    @EventHandler
    public void aoMorrerDragao(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon dragao)) return;
        for (Player p : dragao.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(dragao.getLocation()) < 200 * 200) registrar(p, "dragao", 1);
        }
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
