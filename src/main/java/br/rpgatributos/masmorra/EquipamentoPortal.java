package br.rpgatributos.masmorra;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.forja.Raridade;
import com.google.common.collect.ArrayListMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * O equipamento dos portais (2.71):
 * <ul>
 *   <li><b>Não identificado:</b> armas e armaduras dos baús do vault vêm sem atributos. Clique direito
 *       revela (a raridade sai na hora); no Altar do Portal, o Avaliador revela com 15% de chance de
 *       subir uma raridade (custa 1 de mana em cristais).</li>
 *   <li><b>Equipamento de Caçador</b> (E a S): cada peça dá +1% de dano contra os monstros dos portais
 *       (e -1% do dano deles) por nível do rank; as 4 peças de armadura juntas valem 50% a mais.</li>
 *   <li><b>Armas de chefe:</b> uma arma única por chefe, com um efeito ao acertar.</li>
 * </ul>
 */
public final class EquipamentoPortal implements Listener {

    private static final NamespacedKey K_NI = new NamespacedKey("rpgatributos", "nao_identificado");
    private static final NamespacedKey K_NI_MIN = new NamespacedKey("rpgatributos", "ni_min");
    private static final NamespacedKey K_NI_MAX = new NamespacedKey("rpgatributos", "ni_max");
    private static final NamespacedKey K_NI_MELHOR = new NamespacedKey("rpgatributos", "ni_melhor");
    public static final NamespacedKey K_CACADOR = new NamespacedKey("rpgatributos", "equip_cacador");
    public static final NamespacedKey K_ARMA_CHEFE = new NamespacedKey("rpgatributos", "arma_chefe");
    static final TextColor COR = TextColor.color(0x9FD8F5);

    private final RPGAtributos plugin;

    public EquipamentoPortal(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Não identificado
    // =====================================================================

    public static ItemStack naoIdentificado(Material m, Raridade min, Raridade max, boolean melhor) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(Component.text("❓ " + nomeTipo(m) + " não identificada", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    Component.text("Raridade: " + min.nome() + " a " + max.nome(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Clique direito: revelar.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                    Component.text("No Altar do Portal, o Avaliador pode subir a raridade.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.setAttributeModifiers(ArrayListMultimap.create()); // sem atributos até revelar
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(K_NI, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(K_NI_MIN, PersistentDataType.INTEGER, min.ordinal());
            meta.getPersistentDataContainer().set(K_NI_MAX, PersistentDataType.INTEGER, max.ordinal());
            meta.getPersistentDataContainer().set(K_NI_MELHOR, PersistentDataType.BYTE, (byte) (melhor ? 1 : 0));
        });
        return i;
    }

    public static boolean naoIdentificado(ItemStack i) {
        return i != null && !i.isEmpty() && i.hasItemMeta() && i.getPersistentDataContainer().has(K_NI);
    }

    /** Uma chave para o MateriaisListener (nada de bigorna, encantar ou bancada com ele). */
    public static NamespacedKey chaveNaoIdentificado() { return K_NI; }

    private static String nomeTipo(Material m) {
        String n = m.name();
        if (n.endsWith("_SWORD")) return "Espada";
        if (n.endsWith("_AXE")) return "Machado";
        if (n.endsWith("_PICKAXE")) return "Picareta";
        if (n.endsWith("_HELMET")) return "Elmo";
        if (n.endsWith("_CHESTPLATE")) return "Peitoral";
        if (n.endsWith("_LEGGINGS")) return "Calça";
        if (n.endsWith("_BOOTS")) return "Bota";
        if (m == Material.BOW) return "Arma";
        return "Peça";
    }

    /** Revela: forja o item de verdade com a raridade sorteada (e, no Avaliador, às vezes uma acima). */
    public ItemStack revelar(Player p, ItemStack ni, boolean avaliador) {
        var pdc = ni.getPersistentDataContainer();
        Raridade[] todas = Raridade.values();
        int min = pdc.getOrDefault(K_NI_MIN, PersistentDataType.INTEGER, 0), max = pdc.getOrDefault(K_NI_MAX, PersistentDataType.INTEGER, min);
        boolean melhor = pdc.getOrDefault(K_NI_MELHOR, PersistentDataType.BYTE, (byte) 0) == 1;
        int r = melhor ? (rnd().nextDouble() < 0.5 ? max : min + rnd().nextInt(max - min + 1))
                : (rnd().nextDouble() < 0.7 ? min : min + rnd().nextInt(max - min + 1));
        boolean subiu = false;
        // Avaliador 15%; a Sorte do Avaliador (Árvore de Caçador) soma 5% por grau, até sem o altar.
        double chance = (avaliador ? 0.15 : 0) + PoderCacador.sorteRevelar(p);
        if (r < todas.length - 1 && rnd().nextDouble() < chance) {
            r++;
            subiu = true;
        }
        Raridade rar = todas[Math.max(0, Math.min(todas.length - 1, r))];
        ItemStack item = plugin.forja().forjar(null, new ItemStack(ni.getType()), rar);
        if (p == null) return item;
        p.showTitle(Title.title(Component.text("✦ " + rar.nome() + (subiu ? " (subiu!)" : ""), rar.cor(), TextDecoration.BOLD),
                Component.text("Equipamento revelado", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1600), Duration.ofMillis(400))));
        p.playSound(p.getLocation(), rar.peloMenos(Raridade.LENDARIO) ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.2, 0), 40, 0.4, 0.5, 0.4, 0.5);
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoRevelar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !naoIdentificado(e.getItem())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // No Altar do Portal quem cuida é o Avaliador.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.CHISELED_STONE_BRICKS
                && plugin.portaisMasmorra().eh(e.getClickedBlock())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        p.getInventory().setItemInMainHand(revelar(p, mao, false));
    }

    // =====================================================================
    //  Equipamento de Caçador
    // =====================================================================

    /** As peças: espada e as 4 de armadura. */
    public enum Peca { ESPADA("Espada", "_SWORD"), ELMO("Elmo", "_HELMET"), PEITORAL("Peitoral", "_CHESTPLATE"), GREVAS("Grevas", "_LEGGINGS"), BOTAS("Botas", "_BOOTS");
        final String nome, sufixo;
        Peca(String nome, String sufixo) { this.nome = nome; this.sufixo = sufixo; }
    }

    private static Material material(Rank r, Peca p) {
        String base = r.ordinal() <= Rank.D.ordinal() ? "IRON" : r.ordinal() <= Rank.B.ordinal() ? "DIAMOND" : "NETHERITE";
        return Material.matchMaterial(base + p.sufixo);
    }

    private static Raridade raridade(Rank r) {
        return switch (r) {
            case E -> Raridade.RARO;
            case D, C -> Raridade.EPICO;
            case B, A -> Raridade.UNICO;
            case S -> Raridade.LENDARIO;
        };
    }

    /** Uma peça de Caçador do rank, já forjada. */
    public ItemStack pecaCacador(Rank r, Peca p) {
        ItemStack base = new ItemStack(material(r, p));
        base.editMeta(m -> m.getPersistentDataContainer().set(K_CACADOR, PersistentDataType.STRING, r.name()));
        return plugin.forja().forjar(null, base, raridade(r));
    }

    public ItemStack pecaCacadorAleatoria(Rank r) {
        Peca[] ps = Peca.values();
        return pecaCacador(r, ps[rnd().nextInt(ps.length)]);
    }

    private static Rank rankCacador(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return Rank.porId(i.getPersistentDataContainer().get(K_CACADOR, PersistentDataType.STRING));
    }

    // =====================================================================
    //  Armas de chefe
    // =====================================================================

    /** A arma de cada chefe: nome, base e o efeito ao acertar. */
    private static Material baseArma(Chefe c) {
        return switch (c) {
            case GOLEM -> Material.MACE;
            case RAINHA -> Material.DIAMOND_SWORD;
            case CHAMAS -> Material.DIAMOND_AXE;
            case LICH -> Material.BOW;
            case TEMPESTADE -> Material.TRIDENT;
            case ARAUTO -> Material.NETHERITE_SWORD;
        };
    }

    private static String nomeArma(Chefe c) {
        return switch (c) {
            case GOLEM -> "Punho do Golem Ancestral";
            case RAINHA -> "Ferrão da Rainha";
            case CHAMAS -> "Machado do Senhor das Chamas";
            case LICH -> "Arco do Lich";
            case TEMPESTADE -> "Tridente da Tempestade Viva";
            case ARAUTO -> "Lâmina do Arauto do Fim";
        };
    }

    private static String efeitoArma(Chefe c) {
        return switch (c) {
            case GOLEM -> "Ao acertar: empurra longe e deixa lento.";
            case RAINHA -> "Ao acertar: envenena.";
            case CHAMAS -> "Ao acertar: põe fogo.";
            case LICH -> "Flechas que acertam definham o alvo.";
            case TEMPESTADE -> "Ao acertar: 20% de chance de um raio.";
            case ARAUTO -> "Ao acertar: definha e cura você um pouco.";
        };
    }

    /** A arma do chefe, já forjada como Lendária. */
    public ItemStack armaDeChefe(Chefe c) {
        ItemStack base = new ItemStack(baseArma(c));
        base.editMeta(m -> m.getPersistentDataContainer().set(K_ARMA_CHEFE, PersistentDataType.STRING, c.name()));
        return plugin.forja().forjar(null, base, Raridade.LENDARIO);
    }

    private static Chefe armaChefe(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_ARMA_CHEFE, PersistentDataType.STRING);
        if (s == null) return null;
        try { return Chefe.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    /**
     * Chamado pela Forja depois de refazer o item: o nome e a linha da peça de Caçador ou da arma de
     * chefe voltam.
     */
    public static void decorar(ItemMeta meta, Material tipo) {
        var pdc = meta.getPersistentDataContainer();
        String rk = pdc.get(K_CACADOR, PersistentDataType.STRING), ch = pdc.get(K_ARMA_CHEFE, PersistentDataType.STRING);
        if (rk == null && ch == null) return;
        TextColor corNome = meta.hasItemName() && meta.itemName().color() != null ? meta.itemName().color() : COR;
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(l -> {
            String t = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(l);
            return t.startsWith("◈ ");
        });
        if (rk != null) {
            Rank r = Rank.porId(rk);
            if (r == null) return;
            meta.itemName(Component.text(nomePeca(tipo) + " de Caçador " + r.name(), corNome));
            int bonus = r.ordinal() + 1;
            lore.add(Component.text("◈ Caçador " + r.name() + ": +" + bonus + "% de dano nos monstros dos portais e -" + bonus + "% do dano deles.", r.cor())
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("◈ As 4 peças de armadura juntas: +50% nisso.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        } else {
            Chefe c;
            try { c = Chefe.valueOf(ch); } catch (IllegalArgumentException e) { return; }
            meta.itemName(Component.text(nomeArma(c), corNome));
            lore.add(Component.text("◈ Arma de chefe: " + efeitoArma(c), c.cor()).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
    }

    private static String nomePeca(Material tipo) {
        for (Peca p : Peca.values()) if (tipo.name().endsWith(p.sufixo)) return p.nome;
        return "Peça";
    }

    // =====================================================================
    //  Em combate
    // =====================================================================

    /** O bônus de Caçador (fração) de quem veste e segura as peças. */
    private static double bonusCacador(Player p) {
        double b = 0;
        int armadura = 0;
        for (ItemStack i : p.getInventory().getArmorContents()) {
            Rank r = rankCacador(i);
            if (r == null) continue;
            b += (r.ordinal() + 1) * 0.01;
            armadura++;
        }
        Rank arma = rankCacador(p.getInventory().getItemInMainHand());
        if (arma != null) b += (arma.ordinal() + 1) * 0.01;
        if (armadura >= 4) b *= 1.5;
        return b;
    }

    private boolean doPortal(Entity e) {
        return PoderCacador.monstroDoPortal(plugin.masmorras(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        Player atacante = d instanceof Player p ? p : d instanceof Projectile pr && pr.getShooter() instanceof Player p ? p : null;
        // Caçador: mais dano nos monstros dos portais, menos dano deles.
        if (atacante != null && doPortal(e.getEntity())) {
            double b = bonusCacador(atacante);
            if (b > 0) e.setDamage(e.getDamage() * (1 + b));
        }
        if (e.getEntity() instanceof Player vitima) {
            Entity origem = d instanceof Projectile pr && pr.getShooter() instanceof Entity s ? s : d;
            if (doPortal(origem)) {
                double b = bonusCacador(vitima);
                if (b > 0) e.setDamage(e.getDamage() * Math.max(0.5, 1 - b));
            }
        }
        // Armas de chefe.
        if (atacante == null || !(e.getEntity() instanceof LivingEntity alvo)) return;
        ItemStack arma = d instanceof org.bukkit.entity.Trident t ? t.getItemStack()
                : d instanceof Projectile ? null : atacante.getInventory().getItemInMainHand();
        Chefe c = arma != null ? armaChefe(arma) : null;
        if (c == null && d instanceof org.bukkit.entity.AbstractArrow && armaChefe(atacante.getInventory().getItemInMainHand()) == Chefe.LICH) c = Chefe.LICH;
        if (c == null) return;
        switch (c) {
            case GOLEM -> {
                org.bukkit.util.Vector v = alvo.getLocation().toVector().subtract(atacante.getLocation().toVector()).setY(0);
                if (v.lengthSquared() < 1.0E-4) v = atacante.getLocation().getDirection().setY(0);
                if (v.lengthSquared() > 1.0E-4) alvo.setVelocity(v.normalize().multiply(1.2).setY(0.4));
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
            case RAINHA -> alvo.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
            case CHAMAS -> alvo.setFireTicks(Math.max(alvo.getFireTicks(), 100));
            case LICH -> { if (d instanceof org.bukkit.entity.AbstractArrow) alvo.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1)); }
            case TEMPESTADE -> {
                if (rnd().nextDouble() < 0.2) {
                    alvo.getWorld().strikeLightningEffect(alvo.getLocation()); // só o visual: o dano vem neste mesmo golpe
                    e.setDamage(e.getDamage() + 5);
                }
            }
            case ARAUTO -> {
                alvo.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                var max = atacante.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (max != null) atacante.setHealth(Math.min(max.getValue(), atacante.getHealth() + 1));
            }
        }
    }
}
