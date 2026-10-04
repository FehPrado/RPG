package br.rpgatributos.progressao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Árvore de talentos: 1 ponto a cada tantos níveis somados nos atributos (mais os pontos de
 * cada renascimento), gastos em 4 árvores. Os talentos ficam salvos no jogador. Os efeitos
 * entram nos sistemas do plugin por métodos daqui (mana, vigor, forja, colheita...) e pelos
 * eventos de dano desta classe.
 */
public final class Talentos implements Listener, CommandExecutor {

    private static final int[] S_ABAS = {1, 3, 5, 7};
    /** Onde cada talento fica na tela: faixa 1 a 3 têm dois talentos (esquerda e direita), a 4 tem um. */
    private static final int[][] S_FAIXA = {{}, {11, 15}, {20, 24}, {29, 33}, {40}};
    private static final int S_REFAZER = 45, S_PONTOS = 49, S_RENASCER = 53;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Arvore arvore;
        final Map<Integer, Talento> talentos = new HashMap<>();

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kTalentos, kBonus;
    private final NamespacedKey mArmadura, mEmpurrao, mVelocidade, mSorte, mQueda, mVida;
    private final Map<UUID, EnumMap<Talento, Integer>> cache = new HashMap<>();
    private final Map<UUID, Long> combateAte = new HashMap<>(), furiaAte = new HashMap<>(), furiaPronta = new HashMap<>(),
            sobrecargaPronta = new HashMap<>();

    public Talentos(RPGAtributos plugin) {
        this.plugin = plugin;
        kTalentos = new NamespacedKey(plugin, "talentos");
        kBonus = new NamespacedKey(plugin, "talentos_bonus");
        mArmadura = new NamespacedKey(plugin, "talento_armadura");
        mEmpurrao = new NamespacedKey(plugin, "talento_empurrao");
        mVelocidade = new NamespacedKey(plugin, "talento_velocidade");
        mSorte = new NamespacedKey(plugin, "talento_sorte");
        mQueda = new NamespacedKey(plugin, "talento_queda");
        mVida = new NamespacedKey(plugin, "renascer_vida");
    }

    private Settings cfg() { return plugin.settings(); }
    private Renascimento renascer() { return plugin.renascimento(); }

    // =====================================================================
    //  Dados
    // =====================================================================

    private EnumMap<Talento, Integer> dados(Player p) {
        return cache.computeIfAbsent(p.getUniqueId(), u -> {
            EnumMap<Talento, Integer> m = new EnumMap<>(Talento.class);
            String txt = p.getPersistentDataContainer().get(kTalentos, PersistentDataType.STRING);
            if (txt != null && !txt.isEmpty()) {
                for (String par : txt.split(",")) {
                    String[] kv = par.split(":");
                    Talento t = kv.length == 2 ? Talento.porId(kv[0]) : null;
                    if (t == null) continue;
                    try {
                        m.put(t, Math.max(0, Math.min(t.maximo(), Integer.parseInt(kv[1]))));
                    } catch (NumberFormatException ignorado) {
                        // grau inválido: ignora
                    }
                }
            }
            return m;
        });
    }

    private void gravar(Player p, EnumMap<Talento, Integer> m) {
        List<String> pares = new ArrayList<>();
        m.forEach((t, g) -> { if (g > 0) pares.add(t.id() + ":" + g); });
        if (pares.isEmpty()) p.getPersistentDataContainer().remove(kTalentos);
        else p.getPersistentDataContainer().set(kTalentos, PersistentDataType.STRING, String.join(",", pares));
        cache.put(p.getUniqueId(), m);
    }

    public int grau(Player p, Talento t) {
        return dados(p).getOrDefault(t, 0);
    }

    public int pontosTotais(Player p) {
        int por = Math.max(1, cfg().talNiveisPorPonto);
        return plugin.stats().nivelTotal(p) / por + renascer().vezes(p) * cfg().renPontosTalento
                + p.getPersistentDataContainer().getOrDefault(kBonus, PersistentDataType.INTEGER, 0);
    }

    public int gastos(Player p) {
        int s = 0;
        for (int g : dados(p).values()) s += g;
        return s;
    }

    public int gastosNaArvore(Player p, Arvore a) {
        int s = 0;
        for (Map.Entry<Talento, Integer> e : dados(p).entrySet()) if (e.getKey().arvore() == a) s += e.getValue();
        return s;
    }

    public int livres(Player p) {
        return pontosTotais(p) - gastos(p);
    }

    public void darBonus(Player p, int qtd) {
        int atual = p.getPersistentDataContainer().getOrDefault(kBonus, PersistentDataType.INTEGER, 0);
        p.getPersistentDataContainer().set(kBonus, PersistentDataType.INTEGER, Math.max(0, atual + qtd));
    }

    /** @return o motivo de não poder aprender, ou null. */
    public String motivoBloqueio(Player p, Talento t) {
        if (grau(p, t) >= t.maximo()) return "Grau máximo.";
        int precisa = Talento.PONTOS_FAIXA[t.faixa()];
        if (gastosNaArvore(p, t.arvore()) < precisa) return "Gaste " + precisa + " pontos na árvore " + t.arvore().nome() + ".";
        if (livres(p) <= 0) return "Sem pontos livres (1 a cada " + cfg().talNiveisPorPonto + " níveis somados).";
        return null;
    }

    public boolean aprender(Player p, Talento t) {
        if (motivoBloqueio(p, t) != null) return false;
        EnumMap<Talento, Integer> m = new EnumMap<>(dados(p));
        m.merge(t, 1, Integer::sum);
        gravar(p, m);
        if (gastosNaArvore(p, t.arvore()) == Talento.totalDaArvore(t.arvore())) plugin.titulos().registrar(p, "arvores_completas", 1);
        aplicar(p);
        return true;
    }

    /** Chamado quando um atributo sobe de nível: avisa do ponto de talento novo. */
    public void aoSubirNivel(Player p) {
        if (!cfg().talAtivado || plugin.stats().nivelTotal(p) % Math.max(1, cfg().talNiveisPorPonto) != 0) return;
        p.sendMessage(Component.text("✦ Novo ponto de talento! ", Arvore.ARCANO.cor(), TextDecoration.BOLD)
                .append(Component.text("[Abrir talentos]", NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/talentos"))));
    }

    /** Devolve todos os pontos. */
    public void zerar(Player p) {
        gravar(p, new EnumMap<>(Talento.class));
        aplicar(p);
    }

    /** Se o jogador tem mais pontos gastos do que possui (o admin baixou um nível), os talentos voltam. */
    public void conferir(Player p) {
        if (gastos(p) > pontosTotais(p)) {
            zerar(p);
            p.sendMessage(Component.text("✦ Seus pontos de talento mudaram e os talentos foram devolvidos. Use /talentos.", Arvore.ARCANO.cor()));
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        cache.remove(u);
        combateAte.remove(u);
        furiaAte.remove(u);
    }

    // =====================================================================
    //  Efeitos por atributo (chamado pelo BonusManager)
    // =====================================================================

    public void aplicar(Player p) {
        definir(p, Attribute.ARMOR, mArmadura, grau(p, Talento.PELE_GROSSA), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.KNOCKBACK_RESISTANCE, mEmpurrao, grau(p, Talento.INABALAVEL) * 0.1, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MOVEMENT_SPEED, mVelocidade, grau(p, Talento.PASSOS_LEVES) * 0.02, AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.LUCK, mSorte, grau(p, Talento.SORTE_DO_VIAJANTE) * 0.4, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.SAFE_FALL_DISTANCE, mQueda, grau(p, Talento.QUEDA_SUAVE) * 2.0, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MAX_HEALTH, mVida, renascer().vezes(p) * cfg().renVida, AttributeModifier.Operation.ADD_NUMBER);
    }

    private static void definir(Player p, Attribute a, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        if (a == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    // =====================================================================
    //  Efeitos usados pelos outros sistemas
    // =====================================================================

    public double multiplicadorXp(Player p) {
        return 1 + grau(p, Talento.ERUDITO) * 0.02 + renascer().vezes(p) * cfg().renXp;
    }

    public double bonusMana(Player p) {
        return grau(p, Talento.MENTE_AMPLA) * 10 + renascer().vezes(p) * cfg().renMana;
    }

    public double bonusRegen(Player p) {
        return grau(p, Talento.FLUXO) * 0.2;
    }

    public double custoMagia(Player p) {
        return 1 - grau(p, Talento.ECONOMIA_ARCANA) * 0.04;
    }

    /** Sobrecarga ou Eco de Mana: esta magia sai de graça? (gasta a Sobrecarga se for ela) */
    public boolean magiaGratis(Player p) {
        long agora = System.currentTimeMillis();
        if (grau(p, Talento.SOBRECARGA) > 0 && agora >= sobrecargaPronta.getOrDefault(p.getUniqueId(), 0L)) {
            sobrecargaPronta.put(p.getUniqueId(), agora + 30_000);
            p.sendActionBar(Component.text("✦ Sobrecarga: magia sem custo!", Arvore.ARCANO.cor()));
            return true;
        }
        if (ThreadLocalRandom.current().nextDouble() < grau(p, Talento.ECO_DE_MANA) * 0.06) {
            p.sendActionBar(Component.text("✦ Eco de Mana: a magia não gastou mana", Arvore.ARCANO.cor()));
            return true;
        }
        return false;
    }

    public double bonusVigor(Player p) {
        return grau(p, Talento.VIGOR_DE_BATALHA) * 15;
    }

    public double chanceRaridadeExtra(Player p) {
        return grau(p, Talento.MAOS_DE_FERREIRO) * 0.01 + grau(p, Talento.OBRA_PRIMA) * 0.05;
    }

    /** Obra-Prima: depois de subir uma raridade, chance de subir mais uma. */
    public double chanceSegundaRaridade(Player p) {
        return grau(p, Talento.OBRA_PRIMA) * 0.25;
    }

    public double chanceRefinoExtra(Player p) {
        return grau(p, Talento.REFINADOR) * 0.02;
    }

    /** Minério, tronco e colheita em dobro. */
    public double chanceDupla(Player p) {
        return grau(p, Talento.COLHEITA_FARTA) * 0.04;
    }

    public double chanceDobroAlquimia(Player p) {
        return grau(p, Talento.ALQUIMISTA_NATO) * 0.05;
    }

    public double duracaoPratos(Player p) {
        return 1 + grau(p, Talento.MESTRE_CUCA) * 0.15;
    }

    public double recargaEsquiva(Player p) {
        return 1 - grau(p, Talento.ESQUIVA_AGIL) * 0.15;
    }

    public double vigorEsquivaMenos(Player p) {
        return grau(p, Talento.ESQUIVA_AGIL) * 3;
    }

    public double multiplicadorMapas(Player p) {
        return 1 + grau(p, Talento.CACADOR_DE_TESOUROS) * 0.5;
    }

    public double chanceMitriloExtra(Player p) {
        return grau(p, Talento.GARIMPEIRO) * 0.15;
    }

    // =====================================================================
    //  Combate
    // =====================================================================

    private void emCombate(Player p) {
        combateAte.put(p.getUniqueId(), System.currentTimeMillis() + 10_000);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity)) return;
        emCombate(p);
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c != EntityDamageEvent.DamageCause.ENTITY_ATTACK && c != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        double mult = 1 + grau(p, Talento.FORCA_BRUTA) * 0.02 + renascer().vezes(p) * cfg().renDano;
        if (System.currentTimeMillis() < furiaAte.getOrDefault(p.getUniqueId(), 0L)) mult += 0.25;
        if (ThreadLocalRandom.current().nextDouble() < grau(p, Talento.GOLPE_CRITICO) * 0.04) {
            mult *= 1.5;
            e.getEntity().getWorld().spawnParticle(Particle.CRIT, e.getEntity().getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.2);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.1f);
        }
        if (mult != 1) e.setDamage(e.getDamage() * mult);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.VOID || c == EntityDamageEvent.DamageCause.KILL
                || c == EntityDamageEvent.DamageCause.SUICIDE || c == EntityDamageEvent.DamageCause.STARVATION) return;
        if (e instanceof EntityDamageByEntityEvent) emCombate(p);
        double dano = e.getDamage() * (1 - grau(p, Talento.INABALAVEL) * 0.04);
        // Escudo de Mana: parte do dano sai da mana (4 de mana por ponto de vida).
        int escudo = grau(p, Talento.ESCUDO_DE_MANA);
        if (escudo > 0 && dano > 0) {
            var pf = plugin.arcano().perfil(p);
            double absorve = dano * escudo * 0.10, custo = absorve * 4;
            if (pf.mana() >= custo) {
                plugin.arcano().gastarMana(p, pf, custo);
                dano -= absorve;
            }
        }
        e.setDamage(Math.max(0, dano));
        // Fúria Imortal
        if (grau(p, Talento.FURIA_IMORTAL) > 0) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            double vidaMax = max == null ? 20 : max.getValue();
            long agora = System.currentTimeMillis();
            if (p.getHealth() - e.getFinalDamage() < vidaMax * 0.25 && p.getHealth() - e.getFinalDamage() > 0
                    && agora >= furiaPronta.getOrDefault(p.getUniqueId(), 0L)) {
                furiaPronta.put(p.getUniqueId(), agora + 60_000);
                furiaAte.put(p.getUniqueId(), agora + 6_000);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 1));
                p.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation().add(0, 1.2, 0), 8, 0.4, 0.5, 0.4, 0);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.3f);
                p.sendActionBar(Component.text("⚔ Fúria Imortal!", Arvore.GUERREIRO.cor(), TextDecoration.BOLD));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || !(e.getEntity() instanceof Enemy)) return;
        int g = grau(p, Talento.SEDE_DE_SANGUE);
        if (g <= 0 || p.isDead()) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(Math.min(max == null ? 20 : max.getValue(), p.getHealth() + g));
    }

    @EventHandler(ignoreCancelled = true)
    public void aoGastarItem(PlayerItemDamageEvent e) {
        if (ThreadLocalRandom.current().nextDouble() < grau(e.getPlayer(), Talento.ENGENHOSO) * 0.10) e.setCancelled(true);
    }

    /** A cada 2 s: Andarilho Eterno. */
    public void tick() {
        long agora = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (grau(p, Talento.ANDARILHO_ETERNO) <= 0 || agora < combateAte.getOrDefault(p.getUniqueId(), 0L)) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, true, false, true));
        }
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (!cfg().talAtivado) {
            p.sendMessage(Component.text("Os talentos estão desligados neste servidor.", NamedTextColor.RED));
            return true;
        }
        abrir(p, Arvore.GUERREIRO);
        return true;
    }

    public void abrir(Player p, Arvore arvore) {
        Tela t = new Tela();
        t.arvore = arvore;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("Talentos · " + arvore.nome(), arvore.cor()));
        ItemStack vidro = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
        for (int i = 0; i < 54; i++) t.inventario.setItem(i, vidro);
        for (Arvore a : Arvore.values()) {
            int gastos = gastosNaArvore(p, a), total = Talento.totalDaArvore(a);
            t.inventario.setItem(S_ABAS[a.ordinal()], item(a.item(), Component.text(a.icone() + " " + a.nome(), a.cor(), TextDecoration.BOLD),
                    List.of(Component.text(a.descricao(), NamedTextColor.GRAY),
                            Component.text(gastos + "/" + total + " pontos", NamedTextColor.YELLOW),
                            Component.empty(),
                            Component.text(a == arvore ? "● Aberta" : "» Clique para abrir", a == arvore ? NamedTextColor.GREEN : NamedTextColor.YELLOW)),
                    a == arvore));
        }
        int[] usado = new int[5];
        for (Talento x : Talento.values()) {
            if (x.arvore() != arvore) continue;
            int slot = S_FAIXA[x.faixa()][usado[x.faixa()]++];
            t.talentos.put(slot, x);
            t.inventario.setItem(slot, itemTalento(p, x));
        }
        // Vidro verde ou vermelho no meio de cada faixa: liberada ou quantos pontos faltam.
        int[][] marcas = {{}, {13}, {22}, {31}, {39, 41}};
        for (int f = 1; f <= 4; f++) {
            int precisa = Talento.PONTOS_FAIXA[f];
            boolean ok = gastosNaArvore(p, arvore) >= precisa;
            ItemStack marca = item(ok ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    Component.text("Faixa " + f + (ok ? " liberada" : ": gaste " + precisa + " pontos na árvore"), ok ? NamedTextColor.GREEN : NamedTextColor.RED),
                    List.of(), false);
            for (int s : marcas[f]) t.inventario.setItem(s, marca);
        }
        int livres = livres(p);
        t.inventario.setItem(S_PONTOS, item(Material.EXPERIENCE_BOTTLE, Component.text("Pontos de talento", NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(Component.text("Livres: " + livres, livres > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                        Component.text("Gastos: " + gastos(p) + " de " + pontosTotais(p), NamedTextColor.YELLOW),
                        Component.empty(),
                        Component.text("1 ponto a cada " + cfg().talNiveisPorPonto + " níveis somados nos atributos", NamedTextColor.GRAY),
                        Component.text("e +" + cfg().renPontosTalento + " a cada renascimento.", NamedTextColor.GRAY)), livres > 0));
        t.inventario.setItem(S_REFAZER, item(Material.WATER_BUCKET, Component.text("Refazer talentos", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Devolve todos os pontos.", NamedTextColor.GRAY),
                        Component.text("Custa " + cfg().talCustoRefazer + " esmeraldas.", NamedTextColor.GREEN),
                        Component.empty(),
                        Component.text("» Shift + clique para refazer", NamedTextColor.YELLOW)), false));
        t.inventario.setItem(S_RENASCER, renascer().itemDoMenu(p));
        p.openInventory(t.inventario);
    }

    private ItemStack itemTalento(Player p, Talento x) {
        int g = grau(p, x);
        String motivo = motivoBloqueio(p, x);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(x.descricao(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Grau " + g + "/" + x.maximo(), g >= x.maximo() ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
        if (motivo == null) lore.add(Component.text("» Clique para aprender (1 ponto)", NamedTextColor.GREEN));
        else if (g < x.maximo()) lore.add(Component.text("✖ " + motivo, NamedTextColor.RED));
        Material m = g > 0 || motivo == null ? x.item() : Material.GRAY_DYE;
        return item(m, Component.text(x.nome(), g > 0 ? x.arvore().cor() : NamedTextColor.WHITE, TextDecoration.BOLD), lore, g >= x.maximo());
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilho) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (brilho) meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela t) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        for (Arvore a : Arvore.values()) {
            if (S_ABAS[a.ordinal()] == slot && a != t.arvore) {
                abrir(p, a);
                return;
            }
        }
        Talento x = t.talentos.get(slot);
        if (x != null) {
            if (aprender(p, x)) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
                p.sendMessage(Component.text("✦ " + x.nome() + " grau " + grau(p, x) + "/" + x.maximo(), x.arvore().cor()));
                abrir(p, t.arvore);
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
            }
            return;
        }
        if (slot == S_REFAZER && e.isShiftClick()) {
            if (gastos(p) == 0) return;
            int custo = cfg().talCustoRefazer;
            if (custo > 0 && !p.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), custo)) {
                p.sendMessage(Component.text("Faltam esmeraldas (" + custo + ").", NamedTextColor.RED));
                return;
            }
            if (custo > 0) p.getInventory().removeItem(new ItemStack(Material.EMERALD, custo));
            zerar(p);
            p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.8f, 1f);
            p.sendMessage(Component.text("✦ Talentos refeitos: " + pontosTotais(p) + " pontos livres.", NamedTextColor.AQUA));
            abrir(p, t.arvore);
            return;
        }
        if (slot == S_RENASCER) {
            p.closeInventory();
            renascer().pedir(p);
        }
    }

    /** Texto curto para o /atributos. */
    public String resumo(Player p) {
        return livres(p) + " ponto(s) livre(s) de " + pontosTotais(p);
    }
}
