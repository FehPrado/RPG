package br.rpgatributos.oficio;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zoglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Curtume e tecelagem: as peles que as criaturas deixam, as peças dos conjuntos de armadura
 * leve e os bônus de conjunto. As peças podem ir à Forja (Reforjar com um Fragmento de Forja
 * ou refino): a Forja chama {@link #decorar} e o conjunto continua valendo.
 */
public final class Oficios implements Listener {

    public static final TextColor COR = TextColor.color(0xC08A5A);
    public static final NamespacedKey K_CONJUNTO = new NamespacedKey("rpgatributos", "conjunto");
    private static final NamespacedKey K_FORJADO = new NamespacedKey("rpgatributos", "forja_raridade");
    private static final NamespacedKey K_REFINO = new NamespacedKey("rpgatributos", "forja_refino");
    private static final NamespacedKey K_VELOCIDADE = new NamespacedKey("rpgatributos", "conjunto_velocidade");
    private static final NamespacedKey K_VIDA = new NamespacedKey("rpgatributos", "conjunto_vida");
    private static final NamespacedKey K_EMPURRAO = new NamespacedKey("rpgatributos", "conjunto_empurrao");

    private final RPGAtributos plugin;

    public Oficios(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Peças
    // =====================================================================

    public ItemStack criarPeca(Conjunto c, Conjunto.Peca pc) {
        ItemStack i = new ItemStack(pc.material());
        i.editMeta(m -> {
            if (m instanceof LeatherArmorMeta lm) lm.setColor(c.corCouro());
            m.getPersistentDataContainer().set(K_CONJUNTO, PersistentDataType.STRING, c.id());
        });
        i.editMeta(m -> decorar(m, pc.material()));
        return i;
    }

    public static Conjunto conjunto(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String id = i.getPersistentDataContainer().get(K_CONJUNTO, PersistentDataType.STRING);
        return id == null ? null : Conjunto.porId(id);
    }

    private static NamespacedKey chaveArmadura(Conjunto.Peca pc) {
        return new NamespacedKey("rpgatributos", "conjunto_armadura_" + pc.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static EquipmentSlotGroup grupo(Conjunto.Peca pc) {
        return switch (pc) {
            case CAPUZ -> EquipmentSlotGroup.HEAD;
            case GIBAO -> EquipmentSlotGroup.CHEST;
            case CALCAS -> EquipmentSlotGroup.LEGS;
            case BOTAS -> EquipmentSlotGroup.FEET;
        };
    }

    /**
     * Nome, texto e o +1 de armadura do couro reforçado. Chamado ao costurar e pela Forja depois
     * de refazer o item (que troca o nome, o texto e os atributos).
     */
    public void decorar(ItemMeta meta, Material tipo) {
        String id = meta.getPersistentDataContainer().get(K_CONJUNTO, PersistentDataType.STRING);
        Conjunto c = id == null ? null : Conjunto.porId(id);
        Conjunto.Peca pc = Conjunto.Peca.de(tipo);
        if (c == null || pc == null) return;
        boolean forjado = meta.getPersistentDataContainer().has(K_FORJADO);
        // Atributos: sem nenhum, o item perde os do jogo ao ganhar um; então copia os padrões antes.
        if (!meta.hasAttributeModifiers()) {
            tipo.getDefaultAttributeModifiers().forEach((a, m) -> meta.addAttributeModifier(a, new AttributeModifier(m.getKey(), m.getAmount(),
                    m.getOperation(), m.getSlotGroup() == EquipmentSlotGroup.ANY ? grupo(pc) : m.getSlotGroup())));
        }
        NamespacedKey chave = chaveArmadura(pc);
        var atuais = meta.getAttributeModifiers(Attribute.ARMOR);
        if (atuais != null) for (AttributeModifier am : new ArrayList<>(atuais)) if (am.getKey().equals(chave)) meta.removeAttributeModifier(Attribute.ARMOR, am);
        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(chave, 1, AttributeModifier.Operation.ADD_NUMBER, grupo(pc)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);

        // Nome: a cor da raridade se foi forjado (com o +N do refino), a do conjunto se não.
        TextColor cor = c.cor();
        if (forjado && meta.hasItemName() && meta.itemName().color() != null) cor = meta.itemName().color();
        Component nome = Component.text(c.nomePeca(pc), cor);
        int refino = meta.getPersistentDataContainer().getOrDefault(K_REFINO, PersistentDataType.INTEGER, 0);
        if (forjado && refino > 0) nome = nome.append(Component.text(" +" + refino, cor));
        meta.itemName(nome);

        List<Component> lore = new ArrayList<>();
        if (forjado && meta.lore() != null) {
            for (Component l : meta.lore()) {
                String t = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(l);
                if (t.startsWith("✂") || t.startsWith(" 2 peças") || t.startsWith(" 4 peças") || t.startsWith("   ")) continue;
                lore.add(l);
            }
            lore.add(Component.empty());
        } else {
            int base = 0;
            for (AttributeModifier am : tipo.getDefaultAttributeModifiers().get(Attribute.ARMOR)) base += (int) am.getAmount();
            lore.add(Component.text("🛡 Armadura " + (base + 1) + " (couro reforçado)", NamedTextColor.GRAY));
            lore.add(Component.text("Protege do frio como todo couro.", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
        }
        lore.add(Component.text("✂ " + c.nome(), c.cor(), TextDecoration.BOLD));
        lore.addAll(quebrar(" 2 peças: ", c.bonus2()));
        lore.addAll(quebrar(" 4 peças: ", c.bonus4()));
        if (forjado) lore.add(Component.text("✂ Couro reforçado: +1 de armadura", NamedTextColor.DARK_GRAY));
        else lore.add(Component.text("✂ Pode ser reforjado na Forja (com um Fragmento de Forja).", NamedTextColor.DARK_GRAY));
        meta.lore(lore.stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());
    }

    private static List<Component> quebrar(String prefixo, String texto) {
        List<Component> l = new ArrayList<>();
        StringBuilder linha = new StringBuilder(prefixo);
        for (String p : texto.split(" ")) {
            if (linha.length() + p.length() > 40) {
                l.add(Component.text(linha.toString(), NamedTextColor.GRAY));
                linha = new StringBuilder("   ");
            }
            linha.append(p).append(' ');
        }
        l.add(Component.text(linha.toString().stripTrailing(), NamedTextColor.GRAY));
        return l;
    }

    // =====================================================================
    //  Peles das criaturas
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (!(m.getKiller() instanceof Player)) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        MaterialOficio pele = null;
        double chance = 0;
        int qtd = 1;
        if (m instanceof Wolf w && !w.isTamed()) { pele = MaterialOficio.PELE_DE_LOBO; chance = 0.5; }
        else if (m instanceof PolarBear) { pele = MaterialOficio.PELE_DE_URSO; chance = 0.65; qtd = 1 + r.nextInt(2); }
        else if (m instanceof Fox) { pele = MaterialOficio.PELE_DE_RAPOSA; chance = 0.5; }
        else if (m instanceof ElderGuardian) { pele = MaterialOficio.ESCAMA_DE_GUARDIAO; chance = 1; qtd = 3 + r.nextInt(3); }
        else if (m instanceof Guardian) { pele = MaterialOficio.ESCAMA_DE_GUARDIAO; chance = 0.35; }
        else if (m instanceof Hoglin || m instanceof Zoglin) { pele = MaterialOficio.COURO_DE_HOGLIN; chance = 0.5; qtd = 1 + r.nextInt(2); }
        if (pele == null) return;
        if (br.rpgatributos.domador.Companheiros.eh(m)) return;
        if (br.rpgatributos.perigo.Perigo.ehElite(m)) { chance = 1; qtd++; }
        if (r.nextDouble() < chance) e.getDrops().add(pele.criar(qtd));
    }

    // =====================================================================
    //  Bônus de conjunto
    // =====================================================================

    /** Quantas peças de cada conjunto o jogador está vestindo. */
    public Map<Conjunto, Integer> vestidos(Player p) {
        Map<Conjunto, Integer> m = new EnumMap<>(Conjunto.class);
        for (ItemStack i : p.getInventory().getArmorContents()) {
            Conjunto c = conjunto(i);
            if (c != null) m.merge(c, 1, Integer::sum);
        }
        return m;
    }

    public boolean tem(Player p, Conjunto c, int pecas) {
        int n = 0;
        for (ItemStack i : p.getInventory().getArmorContents()) if (conjunto(i) == c) n++;
        return n >= pecas;
    }

    /** A cada segundo: atributos e efeitos dos conjuntos. */
    public void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Map<Conjunto, Integer> v = vestidos(p);
            int cacador = v.getOrDefault(Conjunto.CACADOR, 0), urso = v.getOrDefault(Conjunto.URSO, 0),
                    raposa = v.getOrDefault(Conjunto.RAPOSA, 0), mare = v.getOrDefault(Conjunto.MARE, 0),
                    noite = v.getOrDefault(Conjunto.NOITE, 0), brasa = v.getOrDefault(Conjunto.BRASA, 0);
            modificador(p, Attribute.MOVEMENT_SPEED, K_VELOCIDADE, cacador >= 2 ? 0.05 : 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            modificador(p, Attribute.MAX_HEALTH, K_VIDA, urso >= 2 ? 2 : 0, AttributeModifier.Operation.ADD_NUMBER);
            modificador(p, Attribute.KNOCKBACK_RESISTANCE, K_EMPURRAO, urso >= 4 ? 0.5 : 0, AttributeModifier.Operation.ADD_NUMBER);
            if (v.isEmpty() || p.getGameMode() == GameMode.SPECTATOR) continue;
            boolean noiteMundo = br.rpgatributos.mundo.Ceu.noite(p.getWorld());
            if (raposa >= 2) efeito(p, PotionEffectType.LUCK, 0);
            if (raposa >= 4 && noiteMundo) {
                efeito(p, PotionEffectType.NIGHT_VISION, 0);
                efeito(p, PotionEffectType.SPEED, 0);
            }
            if (mare >= 2 && p.isInWater()) efeito(p, PotionEffectType.WATER_BREATHING, 0);
            if (mare >= 4 && p.isInWater()) efeito(p, PotionEffectType.DOLPHINS_GRACE, 0);
            if (noite >= 4 && p.getFallDistance() > 5 && !p.isGliding() && !p.isFlying()) efeito(p, PotionEffectType.SLOW_FALLING, 0);
            if (brasa >= 4) {
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && p.getHealth() < max.getValue() * 0.3) efeito(p, PotionEffectType.STRENGTH, 0);
            }
        }
    }

    private static void efeito(Player p, PotionEffectType t, int nivel) {
        PotionEffect atual = p.getPotionEffect(t);
        if (atual != null && (atual.getAmplifier() > nivel || atual.getDuration() > 300 && t != PotionEffectType.NIGHT_VISION)) return;
        int dur = t == PotionEffectType.NIGHT_VISION ? 260 : 60;
        if (atual == null || atual.getDuration() < dur - 20) p.addPotionEffect(new PotionEffect(t, dur, nivel, true, false, true));
    }

    private static void modificador(Player p, Attribute a, NamespacedKey k, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier atual = inst.getModifier(k);
        if (valor == 0) {
            if (atual != null) inst.removeModifier(k);
            return;
        }
        if (atual != null && atual.getAmount() == valor) return;
        if (atual != null) inst.removeModifier(k);
        inst.addTransientModifier(new AttributeModifier(k, valor, op, EquipmentSlotGroup.ANY));
        if (a == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    /** Dano recebido: Urso (4) -10%, Noite (2) -50% de queda, Brasa (2) -30% de fogo. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Map<Conjunto, Integer> v = vestidos(p);
        if (v.isEmpty()) return;
        double mult = 1;
        if (v.getOrDefault(Conjunto.URSO, 0) >= 4) mult *= 0.9;
        switch (e.getCause()) {
            case FALL -> { if (v.getOrDefault(Conjunto.NOITE, 0) >= 2) mult *= 0.5; }
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR, CAMPFIRE -> { if (v.getOrDefault(Conjunto.BRASA, 0) >= 2) mult *= 0.7; }
            default -> { }
        }
        if (mult < 1) e.setDamage(e.getDamage() * mult);
    }

    /** Dano causado: Caçador (4) +10% com flechas e tridente, +8% na postura Ágil. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        Player p = d instanceof Player x ? x : d instanceof AbstractArrow a && a.getShooter() instanceof Player x ? x : null;
        if (p == null || !tem(p, Conjunto.CACADOR, 4)) return;
        double mult = d instanceof AbstractArrow ? 1.10 : 1;
        if (d instanceof Player && plugin.combos().posturas().ativa(p) == br.rpgatributos.combo.Postura.AGIL) mult *= 1.08;
        if (mult > 1) e.setDamage(e.getDamage() * mult);
    }

    /** Esquiva: o Caçador (4) gasta 25% menos vigor. */
    public double multEsquiva(Player p) {
        return tem(p, Conjunto.CACADOR, 4) ? 0.75 : 1;
    }

    /** Pesca: a Maré (4) dá +5% de peixe raro. */
    public double bonusPesca(Player p) {
        return tem(p, Conjunto.MARE, 4) ? 0.05 : 0;
    }

    /** Temperatura: a Brasa (4) não sente calor. */
    public boolean semCalor(Player p) {
        return tem(p, Conjunto.BRASA, 4);
    }
}
