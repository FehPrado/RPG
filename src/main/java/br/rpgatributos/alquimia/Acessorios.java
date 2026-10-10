package br.rpgatributos.alquimia;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Os espaços de acessório de cada jogador (2 anéis, 1 amuleto, 1 cinto e 2 de bolso),
 * guardados no próprio jogador. Aplica atributos, efeitos e os poderes especiais
 * (Lanterna de Bolso, Ímã, Relógio e Amuleto da Fênix). Os acessórios não caem ao morrer.
 */
public final class Acessorios implements Listener {

    private static final TextColor COR = TextColor.color(0xE8C15A);
    /** Tipo de cada espaço, na ordem em que são guardados. */
    private static final Acessorio.Tipo[] ESPACOS = {Acessorio.Tipo.ANEL, Acessorio.Tipo.ANEL, Acessorio.Tipo.AMULETO,
            Acessorio.Tipo.CINTO, Acessorio.Tipo.BOLSO, Acessorio.Tipo.BOLSO};
    private static final int[] SLOTS_MENU = {19, 20, 21, 23, 24, 25};
    private static final int S_INFO = 4;
    private static final int S_FECHAR = 40;
    private static final long RECARGA_FENIX_MS = 10 * 60 * 1000;
    private static final double RAIO_IMA = 6;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kAcessorios, kFenix;
    /** Acessórios em uso de quem está online (cada posição = um espaço; null = vazio). */
    private final Map<UUID, Acessorio[]> equipados = new HashMap<>();
    /** Onde está a luz falsa da Lanterna de Bolso de cada jogador. */
    private final Map<UUID, Location> luzes = new HashMap<>();
    private int ciclo;

    public Acessorios(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kAcessorios = new NamespacedKey(plugin, "acessorios");
        this.kFenix = new NamespacedKey(plugin, "fenix_recarga");
    }

    // =====================================================================
    //  Guardar e ler
    // =====================================================================

    private ItemStack[] itens(Player p) {
        ItemStack[] r = new ItemStack[ESPACOS.length];
        byte[] bytes = p.getPersistentDataContainer().get(kAcessorios, PersistentDataType.BYTE_ARRAY);
        if (bytes != null) {
            ItemStack[] lidos = ItemStack.deserializeItemsFromBytes(bytes);
            for (int i = 0; i < lidos.length && i < r.length; i++) {
                r[i] = lidos[i] == null || lidos[i].isEmpty() ? null : br.rpgatributos.exploracao.PacoteRecursos.atualizar(lidos[i]);
            }
        }
        return r;
    }

    private void salvar(Player p, ItemStack[] itens) {
        ItemStack[] copia = new ItemStack[itens.length];
        for (int i = 0; i < itens.length; i++) copia[i] = itens[i] == null ? ItemStack.empty() : itens[i];
        p.getPersistentDataContainer().set(kAcessorios, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(copia));
        carregar(p, itens);
    }

    private void carregar(Player p, ItemStack[] itens) {
        Acessorio[] a = new Acessorio[ESPACOS.length];
        for (int i = 0; i < itens.length; i++) a[i] = Acessorio.de(itens[i]);
        equipados.put(p.getUniqueId(), a);
        aplicar(p);
    }

    public void carregar(Player p) {
        carregar(p, itens(p));
    }

    /**
     * Abre o Balão de Cristal: gasta um uso (a linha "Usos" do item muda).
     * @return quantos usos sobraram (0 = estoura quando o voo acabar), ou -1 se não tem balão.
     */
    public int gastarBalao(Player p) {
        ItemStack[] itens = itens(p);
        for (int i = 0; i < itens.length; i++) {
            if (Acessorio.de(itens[i]) != Acessorio.BALAO_DE_CRISTAL) continue;
            int usos = itens[i].getPersistentDataContainer().getOrDefault(Acessorio.CHAVE_USOS, PersistentDataType.INTEGER, Acessorio.USOS_BALAO);
            int restam = Math.max(0, usos - 1);
            itens[i].editMeta(m -> {
                m.getPersistentDataContainer().set(Acessorio.CHAVE_USOS, PersistentDataType.INTEGER, restam);
                List<Component> lore = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
                if (lore.size() > 1) lore.set(1, Acessorio.linhaUsos(restam));
                m.lore(lore);
            });
            salvar(p, itens);
            return restam;
        }
        return -1;
    }

    /** O balão sem usos estoura (some do espaço de bolso). */
    public void estourarBalao(Player p) {
        ItemStack[] itens = itens(p);
        for (int i = 0; i < itens.length; i++) {
            if (Acessorio.de(itens[i]) != Acessorio.BALAO_DE_CRISTAL) continue;
            if (itens[i].getPersistentDataContainer().getOrDefault(Acessorio.CHAVE_USOS, PersistentDataType.INTEGER, 1) > 0) continue;
            itens[i] = null;
            salvar(p, itens);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.3f);
            p.getWorld().spawnParticle(Particle.ITEM, p.getLocation().add(0, 2.2, 0), 20, 0.3, 0.3, 0.3, 0.05,
                    new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
            p.sendActionBar(Component.text("◌ O Balão de Cristal estourou!", NamedTextColor.RED));
            return;
        }
    }

    public boolean tem(Player p, Acessorio.Especial e) {
        Acessorio[] a = equipados.get(p.getUniqueId());
        if (a == null) return false;
        for (Acessorio x : a) if (x != null && x.especial() == e) return true;
        return false;
    }

    // =====================================================================
    //  Bônus
    // =====================================================================

    private static final Set<Attribute> ATRIBUTOS = atributos();

    private static Set<Attribute> atributos() {
        Set<Attribute> s = new java.util.HashSet<>();
        for (Acessorio a : Acessorio.values()) for (Acessorio.Mod m : a.mods()) s.add(m.atributo());
        return s;
    }

    /** Refaz os atributos dados pelos acessórios. */
    public void aplicar(Player p) {
        for (Attribute at : ATRIBUTOS) {
            AttributeInstance inst = p.getAttribute(at);
            if (inst == null) continue;
            for (AttributeModifier m : List.copyOf(inst.getModifiers())) {
                if (m.getKey().getNamespace().equals(kAcessorios.getNamespace()) && m.getKey().getKey().startsWith("acessorio_")) {
                    inst.removeModifier(m);
                }
            }
        }
        Acessorio[] a = equipados.get(p.getUniqueId());
        if (a != null) {
            for (int i = 0; i < a.length; i++) {
                if (a[i] == null) continue;
                List<Acessorio.Mod> mods = a[i].mods();
                for (int k = 0; k < mods.size(); k++) {
                    Acessorio.Mod m = mods.get(k);
                    AttributeInstance inst = p.getAttribute(m.atributo());
                    if (inst == null) continue;
                    inst.addModifier(new AttributeModifier(new NamespacedKey(plugin, "acessorio_" + i + "_" + k),
                            m.valor(), m.operacao(), EquipmentSlotGroup.ANY));
                }
            }
        }
        AttributeInstance vida = p.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null && p.getHealth() > vida.getValue()) p.setHealth(vida.getValue());
        if (!tem(p, Acessorio.Especial.LANTERNA)) apagarLuz(p);
    }

    /** Efeitos de poção dos acessórios (renovados junto com os dos itens forjados). */
    public Map<PotionEffectType, Integer> efeitos(Player p) {
        Map<PotionEffectType, Integer> m = new HashMap<>();
        Acessorio[] a = equipados.get(p.getUniqueId());
        if (a == null) return m;
        for (Acessorio x : a) if (x != null && x.pocao() != null) m.merge(x.pocao().tipo(), x.pocao().nivel(), Math::max);
        return m;
    }

    public double bonusMana(Player p) { return tem(p, Acessorio.Especial.MANA) ? 40 : 0; }

    public double bonusRegenMana(Player p) { return tem(p, Acessorio.Especial.MANA) ? 1 : 0; }

    public double multiplicadorXp(Player p) { return tem(p, Acessorio.Especial.XP) ? 1.05 : 1; }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        carregar(e.getPlayer());
    }

    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (p.isOnline()) aplicar(p); });
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        apagarLuz(e.getPlayer());
        equipados.remove(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Menu (/acessorios)
    // =====================================================================

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 45, Component.text("❖ Acessórios"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        inv.setItem(S_INFO, item(Material.GOLD_NUGGET, Component.text("❖ Acessórios", COR, TextDecoration.BOLD), List.of(
                Component.text("Anéis, amuletos, cintos e itens de bolso", NamedTextColor.GRAY),
                Component.text("funcionam enquanto estão aqui.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Clique num acessório do seu inventário", NamedTextColor.WHITE),
                Component.text("para colocar; clique aqui em cima para tirar.", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Não caem quando você morre.", NamedTextColor.GREEN),
                Component.text("Feitos na Bancada Alquímica.", NamedTextColor.DARK_GRAY)), false));
        ItemStack[] itens = itens(p);
        for (int i = 0; i < ESPACOS.length; i++) {
            Acessorio.Tipo tipo = ESPACOS[i];
            inv.setItem(SLOTS_MENU[i] - 9, item(Material.YELLOW_STAINED_GLASS_PANE, Component.text(tipo.nome(), COR), List.of(), false));
            if (itens[i] == null) {
                inv.setItem(SLOTS_MENU[i], item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("◇ " + tipo.nome() + " (vazio)", NamedTextColor.GRAY),
                        List.of(Component.text("Clique num " + tipo.nome().toLowerCase(java.util.Locale.ROOT) + " do seu inventário.", NamedTextColor.DARK_GRAY)), false));
            } else {
                ItemStack mostra = itens[i].clone();
                mostra.editMeta(m -> {
                    List<Component> lore = m.hasLore() && m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
                    lore.add(Component.empty());
                    lore.add(Component.text("» Clique para tirar", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    m.lore(lore);
                });
                inv.setItem(SLOTS_MENU[i], mostra);
            }
        }
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        boolean noMenu = e.getRawSlot() < topo.getSize();
        if (noMenu) {
            if (e.getSlot() == S_FECHAR) { p.closeInventory(); return; }
            for (int i = 0; i < SLOTS_MENU.length; i++) {
                if (SLOTS_MENU[i] == e.getSlot()) { tirar(p, t, i); return; }
            }
            return;
        }
        ItemStack clicado = e.getCurrentItem();
        Acessorio a = Acessorio.de(clicado);
        if (a == null) {
            if (clicado != null && !clicado.isEmpty()) erro(p, "Isso não é um acessório.");
            return;
        }
        colocar(p, t, e.getSlot(), a);
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private void colocar(Player p, Tela t, int slotInv, Acessorio a) {
        ItemStack[] itens = itens(p);
        int livre = -1;
        for (int i = 0; i < ESPACOS.length; i++) {
            if (itens[i] != null && Acessorio.de(itens[i]) == a) {
                erro(p, "Você já está usando um " + a.nome() + ".");
                return;
            }
            if (livre < 0 && ESPACOS[i] == a.tipo() && itens[i] == null) livre = i;
        }
        if (livre < 0) {
            erro(p, "Seus espaços de " + a.tipo().nome().toLowerCase(java.util.Locale.ROOT) + " estão cheios. Tire um primeiro.");
            return;
        }
        ItemStack fonte = p.getInventory().getItem(slotInv);
        if (fonte == null || Acessorio.de(fonte) != a) return;
        itens[livre] = fonte.asOne();
        fonte.setAmount(fonte.getAmount() - 1);
        p.getInventory().setItem(slotInv, fonte.getAmount() > 0 ? fonte : null);
        salvar(p, itens);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1.3f);
        p.sendActionBar(Component.text("❖ " + a.nome() + ": " + a.efeito(), a.cor()));
        desenhar(p, t);
    }

    private void tirar(Player p, Tela t, int espaco) {
        ItemStack[] itens = itens(p);
        if (itens[espaco] == null) return;
        if (p.getInventory().firstEmpty() < 0) {
            erro(p, "Seu inventário está cheio.");
            return;
        }
        p.getInventory().addItem(itens[espaco]);
        itens[espaco] = null;
        salvar(p, itens);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 0.9f);
        desenhar(p, t);
    }

    // =====================================================================
    //  Amuleto da Fênix
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void aoLevarDano(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !tem(p, Acessorio.Especial.FENIX)) return;
        if (e.getFinalDamage() < p.getHealth() + p.getAbsorptionAmount()) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID || e.getCause() == EntityDamageEvent.DamageCause.KILL) return;
        // Totem na mão funciona primeiro (do jeito normal do jogo).
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HAND, EquipmentSlot.OFF_HAND}) {
            if (p.getInventory().getItem(s).getType() == Material.TOTEM_OF_UNDYING) return;
        }
        long agora = System.currentTimeMillis();
        long pronto = p.getPersistentDataContainer().getOrDefault(kFenix, PersistentDataType.LONG, 0L);
        if (agora < pronto) return;
        e.setCancelled(true);
        p.getPersistentDataContainer().set(kFenix, PersistentDataType.LONG, agora + RECARGA_FENIX_MS);
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(Math.min(max == null ? 20 : max.getValue(), 8));
        p.setFireTicks(0);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
        Location l = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.FLAME, l, 80, 0.4, 0.8, 0.4, 0.08);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, l, 60, 0.4, 0.8, 0.4, 0.4);
        p.getWorld().playSound(l, Sound.ITEM_TOTEM_USE, 1f, 1.2f);
        p.showTitle(Title.title(Component.text("🔥 Renascido!", TextColor.color(0xFF7A1A), TextDecoration.BOLD),
                Component.text("O Amuleto da Fênix volta em 10 minutos", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1800), Duration.ofMillis(500))));
    }

    // =====================================================================
    //  Lanterna, Ímã e Relógio (a cada 2 ticks)
    // =====================================================================

    public void tick() {
        ciclo++;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Acessorio[] a = equipados.get(p.getUniqueId());
            if (a == null || p.isDead()) continue;
            boolean lanterna = false, ima = false, relogio = false;
            for (Acessorio x : a) {
                if (x == null) continue;
                lanterna |= x.especial() == Acessorio.Especial.LANTERNA;
                ima |= x.especial() == Acessorio.Especial.IMA;
                relogio |= x.especial() == Acessorio.Especial.RELOGIO;
            }
            if (lanterna) iluminar(p);
            if (ima && ciclo % 2 == 0) puxarItens(p);
            if (relogio && ciclo % 5 == 0 && p.isSneaking()) mostrarRelogio(p);
        }
    }

    private void iluminar(Player p) {
        Block alvo = p.getEyeLocation().getBlock();
        BlockData luz = luzPara(alvo);
        if (luz == null) {
            alvo = p.getLocation().getBlock();
            luz = luzPara(alvo);
        }
        Location antiga = luzes.get(p.getUniqueId());
        Location nova = luz == null ? null : alvo.getLocation();
        boolean mudou = antiga == null || nova == null || !antiga.equals(nova);
        if (mudou && antiga != null) restaurar(antiga);
        if (nova == null) {
            luzes.remove(p.getUniqueId());
            return;
        }
        luzes.put(p.getUniqueId(), nova);
        // Reenvia de vez em quando para quem chegou perto depois.
        if (mudou || ciclo % 10 == 0) {
            for (Player v : p.getWorld().getPlayers()) {
                if (v.getLocation().distanceSquared(nova) < 64 * 64) v.sendBlockChange(nova, luz);
            }
        }
    }

    /** Luz que dá para pôr nesse bloco sem esconder nada (ar ou água parada), ou null. */
    private static BlockData luzPara(Block b) {
        Material t = b.getType();
        if (t != Material.AIR && t != Material.CAVE_AIR && t != Material.WATER) return null;
        if (t == Material.WATER && b.getBlockData() instanceof Levelled lv && lv.getLevel() != 0) return null;
        BlockData d = Material.LIGHT.createBlockData();
        if (d instanceof Levelled lv) lv.setLevel(14);
        if (d instanceof Waterlogged w) w.setWaterlogged(t == Material.WATER);
        return d;
    }

    private static void restaurar(Location l) {
        if (!l.isChunkLoaded()) return;
        BlockData real = l.getBlock().getBlockData();
        for (Player v : l.getWorld().getPlayers()) {
            if (v.getLocation().distanceSquared(l) < 80 * 80) v.sendBlockChange(l, real);
        }
    }

    private void apagarLuz(Player p) {
        Location l = luzes.remove(p.getUniqueId());
        if (l != null) restaurar(l);
    }

    /** Desliga todas as luzes falsas (ao desligar o plugin). */
    public void apagarTodas() {
        for (Location l : luzes.values()) restaurar(l);
        luzes.clear();
    }

    private static void puxarItens(Player p) {
        Location destino = p.getLocation().add(0, 0.6, 0);
        for (Entity e : p.getNearbyEntities(RAIO_IMA, RAIO_IMA, RAIO_IMA)) {
            if (!(e instanceof Item item) || !item.isValid()) continue;
            // Itens jogados por alguém (rituais, trocas) e itens com dono ficam onde estão.
            if (item.getThrower() != null || (item.getOwner() != null && !item.getOwner().equals(p.getUniqueId()))) continue;
            if (item.getPickupDelay() > 10) continue;
            Vector v = destino.toVector().subtract(item.getLocation().toVector());
            double dist = v.length();
            if (dist < 1.2 || dist > RAIO_IMA + 1) continue;
            item.setVelocity(v.normalize().multiply(Math.min(0.5, 0.15 + dist * 0.05)));
        }
    }

    private static void mostrarRelogio(Player p) {
        long t = p.getWorld().getTime();
        int hora = (int) ((t / 1000 + 6) % 24);
        int minuto = (int) ((t % 1000) * 60 / 1000);
        Location l = p.getLocation();
        String bioma = p.getWorld().getBiome(l.getBlockX(), l.getBlockY(), l.getBlockZ()).getKey().getKey().replace('_', ' ');
        boolean dia = t < 12300 || t > 23850;
        p.sendActionBar(Component.text((dia ? "☀ " : "☽ ") + String.format("%02d:%02d", hora, minuto), NamedTextColor.GOLD)
                .append(Component.text("  ·  " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ(), NamedTextColor.WHITE))
                .append(Component.text("  ·  " + bioma, NamedTextColor.GREEN)));
    }

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
