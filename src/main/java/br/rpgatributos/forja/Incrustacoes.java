package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.Mineracao;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Incrustar minério (ajustes da 2.51): com a arma na mão e um mineral na outra, clique na Forja do
 * Ferreiro. Cada arma corpo a corpo aceita uma incrustação (uma nova troca a antiga) e ela fica ao
 * refinar e reforjar. Jade dá vida, Âmbar dá sorte, Pérola dá dano na água, Cristal de Gelo deixa
 * lento, Pedra-Sangue rouba vida e Turfa às vezes queima.
 */
public final class Incrustacoes implements Listener {

    public static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "incrustacao");
    private static final NamespacedKey K_VIDA = new NamespacedKey("rpgatributos", "incrustacao_vida");
    private static final NamespacedKey K_SORTE = new NamespacedKey("rpgatributos", "incrustacao_sorte");
    private static final int NIVEIS = 2;

    public enum Minerio {
        JADE("Jade", 0x4CAF7D, "+1 ❤ de vida máxima"),
        AMBAR("Âmbar", 0xF2A33A, "+1 de sorte"),
        PEROLA("Pérola", 0xF3E9F0, "+25% de dano na água ou na chuva"),
        GELO("Cristal de Gelo", 0xA8E4FF, "cada acerto deixa o inimigo lento"),
        SANGUE("Pedra-Sangue", 0xB0102A, "rouba 6% do dano em vida"),
        TURFA("Turfa", 0xFF6F00, "15% de chance de pôr fogo no inimigo");

        final String nome, efeito;
        final TextColor cor;

        Minerio(String nome, int cor, String efeito) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.efeito = efeito;
        }

        public String nome() { return nome; }
        public String id() { return name().toLowerCase(Locale.ROOT); }

        /** Que mineral é esse item (null se não serve). */
        static Minerio doItem(ItemStack i) {
            if (i == null || i.isEmpty()) return null;
            if (i.getPersistentDataContainer().has(Mineracao.K_TURFA)) return TURFA;
            Raro r = Raro.de(i);
            if (r == null) return null;
            return switch (r) {
                case JADE -> JADE;
                case AMBAR -> AMBAR;
                case PEROLA -> PEROLA;
                case CRISTAL_DE_GELO -> GELO;
                case PEDRA_SANGUE -> SANGUE;
                default -> null;
            };
        }

        static Minerio porId(String s) {
            for (Minerio m : values()) if (m.name().equalsIgnoreCase(s)) return m;
            return null;
        }
    }

    private final RPGAtributos plugin;

    public Incrustacoes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public static Minerio de(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return Minerio.porId(i.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING));
    }

    private static boolean aceita(ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        Categoria c = Categoria.de(i.getType());
        return c != null && c.armaCorpoACorpo();
    }

    // =====================================================================
    //  Incrustar na Forja
    // =====================================================================

    /** Chamado pela Forja do Ferreiro no clique: se der para incrustar, incrusta e devolve true. */
    public boolean tentar(Player p, Location forja) {
        ItemStack arma = p.getInventory().getItemInMainHand(), outra = p.getInventory().getItemInOffHand();
        Minerio m = Minerio.doItem(outra);
        if (m == null || !aceita(arma)) return false;
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && p.getLevel() < NIVEIS) {
            p.sendActionBar(Component.text("◆ Incrustar custa " + NIVEIS + " níveis de experiência.", NamedTextColor.RED));
            return true;
        }
        Minerio antes = de(arma);
        arma.editMeta(meta -> meta.getPersistentDataContainer().set(CHAVE, PersistentDataType.STRING, m.name()));
        if (!plugin.forja().reconstruir(arma)) arma.editMeta(meta -> decorar(meta, arma.getType()));
        if (!criativo) {
            p.setLevel(p.getLevel() - NIVEIS);
            outra.setAmount(outra.getAmount() - 1);
            p.getInventory().setItemInOffHand(outra.getAmount() > 0 ? outra : null);
        }
        Location c = forja.clone().add(0.5, 1.2, 0.5);
        c.getWorld().spawnParticle(Particle.WAX_ON, c, 20, 0.3, 0.2, 0.3, 0.1);
        c.getWorld().playSound(c, Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.3f);
        c.getWorld().playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        p.sendMessage(Component.text("◆ " + m.nome + " incrustado: " + m.efeito + ".", m.cor)
                .append(Component.text(antes != null && antes != m ? " (o " + antes.nome + " saiu)" : "", NamedTextColor.GRAY)));
        return true;
    }

    /**
     * Chamado pela Forja depois de montar o item (e na arma sem forja): a linha na descrição e,
     * para Jade e Âmbar, o atributo.
     */
    public static void decorar(ItemMeta meta, Material tipo) {
        String id = meta.getPersistentDataContainer().get(CHAVE, PersistentDataType.STRING);
        Minerio m = id == null ? null : Minerio.porId(id);
        if (m == null) return;
        // Sem atributos próprios, o item perde os do jogo ao ganhar um: copia os padrões antes.
        if (!meta.hasAttributeModifiers()) {
            tipo.getDefaultAttributeModifiers().forEach((a, mod) -> meta.addAttributeModifier(a, new AttributeModifier(mod.getKey(), mod.getAmount(),
                    mod.getOperation(), mod.getSlotGroup() == EquipmentSlotGroup.ANY ? EquipmentSlotGroup.MAINHAND : mod.getSlotGroup())));
        }
        tirar(meta, Attribute.MAX_HEALTH, K_VIDA);
        tirar(meta, Attribute.LUCK, K_SORTE);
        if (m == Minerio.JADE) meta.addAttributeModifier(Attribute.MAX_HEALTH,
                new AttributeModifier(K_VIDA, 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        if (m == Minerio.AMBAR) meta.addAttributeModifier(Attribute.LUCK,
                new AttributeModifier(K_SORTE, 1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c).startsWith("◆ Incrustado"));
        lore.add(Component.text("◆ Incrustado: " + m.nome + " (" + m.efeito + ")", m.cor).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }

    private static void tirar(ItemMeta meta, Attribute a, NamespacedKey chave) {
        var mods = meta.getAttributeModifiers(a);
        if (mods == null) return;
        for (AttributeModifier mod : List.copyOf(mods)) if (mod.getKey().equals(chave)) meta.removeAttributeModifier(a, mod);
    }

    // =====================================================================
    //  Em combate
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || !(e.getEntity() instanceof LivingEntity alvo)) return;
        Minerio m = de(p.getInventory().getItemInMainHand());
        if (m == null) return;
        switch (m) {
            case PEROLA -> {
                if (p.isInWater() || p.isInRain()) {
                    e.setDamage(e.getDamage() * 1.25);
                    alvo.getWorld().spawnParticle(Particle.SPLASH, alvo.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.05);
                }
            }
            case GELO -> alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, true, true));
            case SANGUE -> {
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + e.getDamage() * 0.06));
            }
            case TURFA -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.15) alvo.setFireTicks(Math.max(alvo.getFireTicks(), 60));
            }
            default -> { }
        }
    }
}
