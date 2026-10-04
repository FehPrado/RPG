package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mineração profunda: o Mitrilo aparece em pequenos veios no fundo da ardósia (abaixo da
 * altura -24). Precisa de picareta de diamante ou netherite. O Mitrilo Bruto vira lingote
 * na fornalha, e o lingote aprimora equipamentos de netherite na mesa de ferraria (com o
 * molde de melhoria de netherite): mais dano ou armadura e 50% mais durabilidade.
 */
public final class Mitrilo implements Listener {

    public static final TextColor COR = TextColor.color(0x7FCFE0);
    /** Estado do bloco de cogumelo marrom usado como minério: só a face de baixo "de fora". */
    public static final int ESTADO_MINERIO = 1;
    private static final NamespacedKey K_ITEM = new NamespacedKey("rpgatributos", "mitrilo");
    private static final NamespacedKey K_APRIMORADO = new NamespacedKey("rpgatributos", "mitrilo_aprimorado");
    private static final String MARCA = "✦ Mitrilo: ";

    private final RPGAtributos plugin;
    private final NamespacedKey kChunkGerado, kChunkPos;
    private final Map<Attribute, NamespacedKey> kMods = new HashMap<>();
    /** Minérios dos chunks carregados (chave do chunk → posições). */
    private final Map<Long, List<Location>> minerios = new HashMap<>();

    public Mitrilo(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kChunkGerado = new NamespacedKey(plugin, "mitrilo_gerado_v1");
        this.kChunkPos = new NamespacedKey(plugin, "mitrilo_pos");
        for (Attribute a : List.of(Attribute.ATTACK_DAMAGE, Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS, Attribute.BLOCK_BREAK_SPEED)) {
            kMods.put(a, new NamespacedKey(plugin, "mitrilo_" + a.getKey().getKey()));
        }
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    private static ItemStack item(String tipo, String nome, String modelo, int qtd, String descricao) {
        ItemStack i = new ItemStack(Material.QUARTZ, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text(nome, COR));
            m.setItemModel(NamespacedKey.minecraft(modelo));
            var cmd = m.getCustomModelDataComponent();
            cmd.setStrings(List.of("rpgatributos:" + tipo));
            m.setCustomModelDataComponent(cmd);
            m.lore(List.of(Component.text(descricao, NamedTextColor.GRAY), Component.empty(), Component.text("⛏ Mineração profunda", COR))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_ITEM, PersistentDataType.STRING, tipo);
        });
        return i;
    }

    public static ItemStack bruto(int qtd) {
        return item("mitrilo_bruto", "Mitrilo Bruto", "raw_iron", qtd, "Funda numa fornalha para ter um lingote.");
    }

    public static ItemStack lingote(int qtd) {
        return item("lingote_mitrilo", "Lingote de Mitrilo", "iron_ingot", qtd, "Aprimora netherite na mesa de ferraria.");
    }

    public static boolean ehLingote(ItemStack i) {
        return i != null && !i.isEmpty() && "lingote_mitrilo".equals(i.getPersistentDataContainer().get(K_ITEM, PersistentDataType.STRING));
    }

    public static boolean aprimorado(ItemStack i) {
        return i != null && !i.isEmpty() && i.getPersistentDataContainer().has(K_APRIMORADO);
    }

    public void registrarReceitas() {
        NamespacedKey forno = new NamespacedKey(plugin, "mitrilo_fornalha"), alto = new NamespacedKey(plugin, "mitrilo_alto_forno"),
                mesa = new NamespacedKey(plugin, "mitrilo_aprimorar");
        Bukkit.removeRecipe(forno);
        Bukkit.removeRecipe(alto);
        Bukkit.removeRecipe(mesa);
        Bukkit.addRecipe(new FurnaceRecipe(forno, lingote(1), RecipeChoice.exactChoice(bruto(1)), 2f, 200));
        Bukkit.addRecipe(new BlastingRecipe(alto, lingote(1), RecipeChoice.exactChoice(bruto(1)), 2f, 100));
        List<Material> netherite = new ArrayList<>();
        for (Material m : Material.values()) if (m.isItem() && m.name().startsWith("NETHERITE_") && m != Material.NETHERITE_INGOT
                && m != Material.NETHERITE_SCRAP && m != Material.NETHERITE_BLOCK && m != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) netherite.add(m);
        Bukkit.addRecipe(new SmithingTransformRecipe(mesa, lingote(1), new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(netherite), RecipeChoice.exactChoice(lingote(1)), true));
    }

    // =====================================================================
    //  Aprimorar (mesa de ferraria)
    // =====================================================================

    private enum Tipo { ARMA, ARMADURA, FERRAMENTA }

    private static Tipo tipo(Material m) {
        String n = m.name();
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) return Tipo.ARMADURA;
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_SPEAR") || m == Material.MACE || m == Material.TRIDENT) return Tipo.ARMA;
        return Tipo.FERRAMENTA;
    }

    private static EquipmentSlotGroup grupo(Material m) {
        String n = m.name();
        if (n.endsWith("_HELMET")) return EquipmentSlotGroup.HEAD;
        if (n.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (n.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (n.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        return EquipmentSlotGroup.MAINHAND;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void aoPrepararMesa(PrepareSmithingEvent e) {
        SmithingInventory inv = e.getInventory();
        if (!ehLingote(inv.getInputMineral())) return;
        ItemStack base = inv.getInputEquipment();
        if (base == null || base.isEmpty() || !base.getType().name().startsWith("NETHERITE_") || aprimorado(base)
                || inv.getInputTemplate() == null || inv.getInputTemplate().getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
            e.setResult(null);
            return;
        }
        ItemStack r = base.clone();
        r.setAmount(1);
        r.editMeta(m -> {
            m.getPersistentDataContainer().set(K_APRIMORADO, PersistentDataType.BYTE, (byte) 1);
            if (m instanceof Damageable d) {
                int max = d.hasMaxDamage() ? d.getMaxDamage() : base.getType().getMaxDurability();
                if (max > 0) d.setMaxDamage((int) Math.round(max * 1.5));
            }
            decorar(m, base.getType());
        });
        e.setResult(r);
    }

    /** Bônus e linha do Mitrilo (chamado também quando a Forja refaz o item). */
    public void decorar(ItemMeta meta, Material material) {
        if (!meta.getPersistentDataContainer().has(K_APRIMORADO)) return;
        // Item sem atributos próprios: copia os do jogo antes, senão ele perde o dano/armadura de base.
        if (!meta.hasAttributeModifiers()) {
            Multimap<Attribute, AttributeModifier> padrao = material.asItemType().getDefaultAttributeModifiers();
            padrao.forEach(meta::addAttributeModifier);
        }
        for (Map.Entry<Attribute, NamespacedKey> en : kMods.entrySet()) {
            var lista = meta.getAttributeModifiers(en.getKey());
            if (lista == null) continue;
            for (AttributeModifier mod : List.copyOf(lista)) if (mod.getKey().equals(en.getValue())) meta.removeAttributeModifier(en.getKey(), mod);
        }
        EquipmentSlotGroup g = grupo(material);
        String texto;
        switch (tipo(material)) {
            case ARMA -> {
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(kMods.get(Attribute.ATTACK_DAMAGE), 1.5, AttributeModifier.Operation.ADD_NUMBER, g));
                texto = "+1,5 de dano e 50% mais durabilidade";
            }
            case ARMADURA -> {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(kMods.get(Attribute.ARMOR), 1, AttributeModifier.Operation.ADD_NUMBER, g));
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(kMods.get(Attribute.ARMOR_TOUGHNESS), 1, AttributeModifier.Operation.ADD_NUMBER, g));
                texto = "+1 de armadura, +1 de resistência e 50% mais durabilidade";
            }
            default -> {
                meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(kMods.get(Attribute.BLOCK_BREAK_SPEED), 0.2, AttributeModifier.Operation.ADD_SCALAR, g));
                texto = "+20% de velocidade e 50% mais durabilidade";
            }
        }
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c).startsWith(MARCA));
        lore.add(Component.text(MARCA + texto, COR).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }

    // =====================================================================
    //  Minério no mundo
    // =====================================================================

    private static long chave(Chunk c) {
        return ((long) c.getX() << 32) ^ (c.getZ() & 0xffffffffL) ^ ((long) c.getWorld().getName().hashCode() << 48);
    }

    private static int empacotar(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y + 2048) << 8);
    }

    @EventHandler
    public void aoCarregarChunk(ChunkLoadEvent e) {
        Chunk c = e.getChunk();
        World w = c.getWorld();
        if (w.getEnvironment() != World.Environment.NORMAL || plugin.masmorras().ehMundo(w)) return;
        var pdc = c.getPersistentDataContainer();
        if (!pdc.has(kChunkGerado)) {
            pdc.set(kChunkGerado, PersistentDataType.BYTE, (byte) 1);
            gerar(c);
        }
        int[] pos = pdc.get(kChunkPos, PersistentDataType.INTEGER_ARRAY);
        if (pos == null || pos.length == 0) return;
        List<Location> l = new ArrayList<>();
        for (int p : pos) l.add(new Location(w, c.getX() * 16 + (p & 15), (p >> 8) - 2048, c.getZ() * 16 + ((p >> 4) & 15)));
        minerios.put(chave(c), l);
    }

    @EventHandler
    public void aoDescarregarChunk(ChunkUnloadEvent e) {
        minerios.remove(chave(e.getChunk()));
    }

    /** Pequenos veios (1 a 3 blocos, em linha) no fundo da ardósia, em 1 a cada 4 chunks. */
    private void gerar(Chunk c) {
        Random r = new Random(c.getWorld().getSeed() ^ (c.getX() * 341873128712L) ^ (c.getZ() * 132897987541L) ^ 0x4D17L);
        if (r.nextDouble() > 0.25) return;
        List<Integer> marcas = new ArrayList<>();
        int veios = 1 + r.nextInt(2);
        int minY = Math.max(c.getWorld().getMinHeight() + 4, -58);
        for (int v = 0; v < veios; v++) {
            int x = 1 + r.nextInt(12), z = 1 + r.nextInt(12), y = minY + r.nextInt(-24 - minY);
            int tam = 1 + r.nextInt(3);
            boolean emX = r.nextBoolean();
            for (int i = 0; i < tam; i++) {
                int bx = x + (emX ? i : 0), bz = z + (emX ? 0 : i);
                Block b = c.getBlock(bx, y, bz);
                if (b.getType() != Material.DEEPSLATE && b.getType() != Material.TUFF) continue;
                b.setBlockData(dadosMinerio(), false);
                marcas.add(empacotar(bx, y, bz));
            }
        }
        if (marcas.isEmpty()) return;
        c.getPersistentDataContainer().set(kChunkPos, PersistentDataType.INTEGER_ARRAY, marcas.stream().mapToInt(Integer::intValue).toArray());
    }

    private static MultipleFacing dadosMinerio() {
        MultipleFacing d = (MultipleFacing) Material.BROWN_MUSHROOM_BLOCK.createBlockData();
        for (BlockFace f : d.getAllowedFaces()) d.setFace(f, f == BlockFace.DOWN);
        return d;
    }

    private boolean minerio(Block b) {
        if (b.getType() != Material.BROWN_MUSHROOM_BLOCK) return false;
        List<Location> l = minerios.get(chave(b.getChunk()));
        if (l == null) return false;
        for (Location x : l) if (x.getBlockX() == b.getX() && x.getBlockY() == b.getY() && x.getBlockZ() == b.getZ()) return true;
        return false;
    }

    private void tirarMarca(Block b) {
        Chunk c = b.getChunk();
        List<Location> l = minerios.get(chave(c));
        if (l != null) l.removeIf(x -> x.getBlockX() == b.getX() && x.getBlockY() == b.getY() && x.getBlockZ() == b.getZ());
        int[] pos = c.getPersistentDataContainer().get(kChunkPos, PersistentDataType.INTEGER_ARRAY);
        if (pos == null) return;
        int alvo = empacotar(b.getX() & 15, b.getY(), b.getZ() & 15);
        int[] novo = java.util.Arrays.stream(pos).filter(p -> p != alvo).toArray();
        c.getPersistentDataContainer().set(kChunkPos, PersistentDataType.INTEGER_ARRAY, novo);
    }

    private static boolean picaretaBoa(ItemStack i) {
        return i != null && (i.getType() == Material.DIAMOND_PICKAXE || i.getType() == Material.NETHERITE_PICKAXE);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoBater(BlockDamageEvent e) {
        if (!minerio(e.getBlock())) return;
        if (!picaretaBoa(e.getItemInHand())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("✖ O Mitrilo só sai com picareta de diamante ou netherite.", NamedTextColor.RED));
            return;
        }
        // Cogumelo quebra na hora: aqui ele fica duro como minério de verdade.
        e.setInstaBreak(false);
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 2, false, false, false));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!minerio(b)) return;
        Player p = e.getPlayer();
        ItemStack ferramenta = p.getInventory().getItemInMainHand();
        if (!picaretaBoa(ferramenta) && p.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            return;
        }
        e.setDropItems(false);
        e.setExpToDrop(6);
        tirarMarca(b);
        int qtd = 1;
        int fortuna = ferramenta.getEnchantmentLevel(Enchantment.FORTUNE);
        if (fortuna > 0 && ThreadLocalRandom.current().nextInt(fortuna + 2) >= 2) qtd++;
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), bruto(qtd));
        b.getWorld().spawnParticle(Particle.END_ROD, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
        b.getWorld().playSound(b.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 1.4f);
        plugin.stats().darXp(p, Skill.MINERACAO, 80);
        plugin.titulos().registrar(p, "mitrilo", qtd);
        p.removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    /** A cada 2 segundos: brilho dos minérios perto de quem está no fundo. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().getY() > 0 || p.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
            Chunk c = p.getLocation().getChunk();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (!p.getWorld().isChunkLoaded(c.getX() + dx, c.getZ() + dz)) continue;
                List<Location> l = minerios.get(chave(p.getWorld().getChunkAt(c.getX() + dx, c.getZ() + dz)));
                if (l == null) continue;
                for (Location x : l) {
                    if (x.distanceSquared(p.getLocation()) > 12 * 12) continue;
                    p.spawnParticle(Particle.ELECTRIC_SPARK, x.clone().add(0.5, 0.5, 0.5), 3, 0.4, 0.4, 0.4, 0);
                }
            }
        }
    }

    /** Admin: coloca um minério de Mitrilo no bloco que você olha. */
    public boolean colocar(Block b) {
        if (b == null || !b.getType().isSolid()) return false;
        b.setBlockData(dadosMinerio(), false);
        Chunk c = b.getChunk();
        int[] pos = c.getPersistentDataContainer().getOrDefault(kChunkPos, PersistentDataType.INTEGER_ARRAY, new int[0]);
        int[] novo = java.util.Arrays.copyOf(pos, pos.length + 1);
        novo[pos.length] = empacotar(b.getX() & 15, b.getY(), b.getZ() & 15);
        c.getPersistentDataContainer().set(kChunkPos, PersistentDataType.INTEGER_ARRAY, novo);
        minerios.computeIfAbsent(chave(c), k -> new ArrayList<>()).add(b.getLocation());
        return true;
    }
}
