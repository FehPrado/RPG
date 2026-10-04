package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.perigo.Perigo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Evolução dos companheiros: eles ganham XP lutando (e as montarias, sendo montadas), sobem
 * do nível 1 ao 30 e evoluem no 10 (estágio II) e no 25 (estágio III). Cada estágio fica maior,
 * mais forte e ganha habilidades: Golpe Feroz, regeneração e, no III, o Vínculo Ancestral
 * (o dono perto recebe menos dano). O XP fica salvo na própria criatura.
 */
public final class Evolucao implements Listener {

    public static final NamespacedKey K_XP = new NamespacedKey("rpgatributos", "companheiro_xp");
    public static final int NIVEL_MAX = 30, NIVEL_II = 10, NIVEL_III = 25;
    private static final TextColor COR = Companheiros.COR;

    /** Nomes dos estágios II e III de algumas criaturas; as outras viram "Veterano" e "Ancestral". */
    private static final Map<EntityType, String[]> NOMES = Map.ofEntries(
            Map.entry(EntityType.WOLF, new String[]{"Lobo Alfa", "Lobo das Sombras"}),
            Map.entry(EntityType.CAT, new String[]{"Gato Selvagem", "Pantera Sombria"}),
            Map.entry(EntityType.HORSE, new String[]{"Corcel de Guerra", "Corcel Celeste"}),
            Map.entry(EntityType.DONKEY, new String[]{"Burro de Carga", "Burro Lendário"}),
            Map.entry(EntityType.MULE, new String[]{"Mula Robusta", "Mula Ancestral"}),
            Map.entry(EntityType.CAMEL, new String[]{"Camelo do Deserto", "Camelo das Areias Eternas"}),
            Map.entry(EntityType.IRON_GOLEM, new String[]{"Golem de Guerra", "Colosso de Ferro"}),
            Map.entry(EntityType.POLAR_BEAR, new String[]{"Urso Glacial", "Urso do Inverno Eterno"}),
            Map.entry(EntityType.FOX, new String[]{"Raposa Astuta", "Raposa Espiritual"}),
            Map.entry(EntityType.PARROT, new String[]{"Papagaio Real", "Ave do Trovão"}),
            Map.entry(EntityType.STRIDER, new String[]{"Strider Ígneo", "Senhor da Lava"}),
            Map.entry(EntityType.LLAMA, new String[]{"Lhama Guerreira", "Lhama Ancestral"}),
            Map.entry(EntityType.COW, new String[]{"Vaca Robusta", "Vaca Sagrada"}),
            Map.entry(EntityType.MOOSHROOM, new String[]{"Vaca Micélica", "Guardiã dos Fungos"}),
            Map.entry(EntityType.SNIFFER, new String[]{"Farejador Ancião", "Farejador Primordial"}));

    private final RPGAtributos plugin;
    private final NamespacedKey mEscala;
    private final Map<UUID, Long> combateAte = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> ultimaPosicao = new HashMap<>();
    private int passos;

    public Evolucao(RPGAtributos plugin) {
        this.plugin = plugin;
        this.mEscala = new NamespacedKey(plugin, "companheiro_escala");
    }

    private Companheiros comp() { return plugin.companheiros(); }

    // =====================================================================
    //  Nível e estágio
    // =====================================================================

    public static int xp(Entity e) {
        return e.getPersistentDataContainer().getOrDefault(K_XP, PersistentDataType.INTEGER, 0);
    }

    /** XP para ir do nível n para o n+1. */
    public static int xpParaProximo(int n) {
        return 20 + 10 * n;
    }

    public static int nivel(Entity e) {
        int x = xp(e), n = 1;
        while (n < NIVEL_MAX && x >= xpParaProximo(n)) {
            x -= xpParaProximo(n);
            n++;
        }
        return n;
    }

    /** XP dentro do nível atual. */
    public static int xpNoNivel(Entity e) {
        int x = xp(e), n = 1;
        while (n < NIVEL_MAX && x >= xpParaProximo(n)) {
            x -= xpParaProximo(n);
            n++;
        }
        return x;
    }

    public static int estagio(Entity e) {
        int n = nivel(e);
        return n >= NIVEL_III ? 3 : n >= NIVEL_II ? 2 : 1;
    }

    public static String nomeEstagio(EntityType t, int estagio) {
        String base = Companheiros.nomeTipo(t);
        if (estagio <= 1) return base;
        String[] n = NOMES.get(t);
        return n != null ? n[estagio - 2] : base + (estagio == 2 ? " Veterano" : " Ancestral");
    }

    public static String romano(int estagio) {
        return estagio == 3 ? "III" : estagio == 2 ? "II" : "I";
    }

    /** Vida e dano a mais (fração), somados aos do Companheiros. */
    public double bonus(LivingEntity e) {
        if (!plugin.settings().evoAtivada) return 0;
        int est = estagio(e);
        return (nivel(e) - 1) * 0.02 + (est >= 2 ? 0.10 : 0) + (est >= 3 ? 0.15 : 0);
    }

    /** Tamanho do estágio (chamado no fim do aplicarAtributos do Companheiros). */
    public void aplicar(LivingEntity e) {
        AttributeInstance inst = e.getAttribute(Attribute.SCALE);
        if (inst == null) return;
        inst.removeModifier(mEscala);
        int est = plugin.settings().evoAtivada ? estagio(e) : 1;
        double escala = est == 3 ? 0.25 : est == 2 ? 0.12 : 0;
        if (escala > 0) inst.addModifier(new AttributeModifier(mEscala, escala, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
    }

    /** Soma XP e cuida de subir de nível e evoluir. */
    public void darXp(LivingEntity e, int qtd) {
        if (!plugin.settings().evoAtivada || qtd <= 0 || !Companheiros.eh(e)) return;
        int nivelAntes = nivel(e), estAntes = estagio(e);
        if (nivelAntes >= NIVEL_MAX) return;
        e.getPersistentDataContainer().set(K_XP, PersistentDataType.INTEGER, xp(e) + qtd);
        int nivelDepois = nivel(e), estDepois = estagio(e);
        if (nivelDepois == nivelAntes) return;
        comp().aplicarAtributos(e);
        Player dono = Bukkit.getPlayer(Companheiros.dono(e));
        if (estDepois > estAntes) {
            evoluiu(e, dono, estAntes, estDepois);
        } else if (dono != null) {
            dono.sendMessage(Component.text("♞ " + Companheiros.nome(e) + " subiu para o nível " + nivelDepois + "!", COR));
            e.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 10, 0.4, 0.3, 0.4, 0);
        }
    }

    public void definirNivel(LivingEntity e, int nivel) {
        int alvo = Math.max(1, Math.min(NIVEL_MAX, nivel)), x = 0;
        for (int n = 1; n < alvo; n++) x += xpParaProximo(n);
        int estAntes = estagio(e);
        e.getPersistentDataContainer().set(K_XP, PersistentDataType.INTEGER, x);
        comp().aplicarAtributos(e);
        if (estagio(e) != estAntes) renomear(e, estAntes, estagio(e));
    }

    private void evoluiu(LivingEntity e, Player dono, int antes, int depois) {
        String nomeAntes = Companheiros.nome(e);
        renomear(e, antes, depois);
        e.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, e.getLocation().add(0, e.getHeight() / 2, 0), 80, 0.5, 0.8, 0.5, 0.4);
        e.getWorld().spawnParticle(depois == 3 ? Particle.END_ROD : Particle.ENCHANT, e.getLocation().add(0, e.getHeight() / 2, 0), 60, 0.6, 0.8, 0.6, 0.1);
        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.7f);
        e.getWorld().playSound(e.getLocation(), depois == 3 ? Sound.ENTITY_ENDER_DRAGON_GROWL : Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.6f, 1.4f);
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) e.setHealth(max.getValue());
        if (dono == null) return;
        dono.sendMessage(Component.text("✦ " + nomeAntes + " evoluiu para o estágio " + romano(depois) + ": ", COR, TextDecoration.BOLD)
                .append(Component.text(nomeEstagio(e.getType(), depois), NamedTextColor.GOLD, TextDecoration.BOLD)));
        dono.sendMessage(Component.text(depois == 2
                ? "  Maior e mais forte, se regenera fora de luta e ganhou o Golpe Feroz."
                : "  Ganhou o Vínculo Ancestral: perto dele você recebe 8% menos dano.", NamedTextColor.GRAY));
        if (depois == 3) {
            plugin.titulos().registrar(dono, "companheiro_lendario", 1);
            plugin.diario().marco(dono, "companheiro_iii", "Evoluiu um companheiro ao estágio III: " + nomeEstagio(e.getType(), 3));
            plugin.getServer().broadcast(Component.text("✦ ", COR).append(Component.text(dono.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" tem agora um " + nomeEstagio(e.getType(), 3) + "!", COR)));
        }
    }

    /** Criatura sem nome dado pelo jogador ganha o nome do estágio. */
    private void renomear(LivingEntity e, int antes, int depois) {
        Component atual = e.customName();
        String txt = atual == null ? null : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(atual);
        if (txt != null && !txt.equals(nomeEstagio(e.getType(), antes)) && !txt.equals(Companheiros.nomeTipo(e.getType()))) return;
        if (depois <= 1) {
            e.customName(null);
            return;
        }
        e.customName(Component.text(nomeEstagio(e.getType(), depois), depois == 3 ? NamedTextColor.GOLD : COR));
        e.setCustomNameVisible(true);
    }

    // =====================================================================
    //  XP: lutar e ser montado
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        if (!(morto instanceof Enemy) || Companheiros.eh(morto)) return;
        if (!(morto.getLastDamageCause() instanceof EntityDamageByEntityEvent ev)) return;
        Entity atacante = ev.getDamager();
        if (atacante instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Entity atirador) atacante = atirador;
        if (!(atacante instanceof LivingEntity c) || !Companheiros.eh(c)) return;
        int qtd = Chefes.ehChefe(morto) ? 60 : Perigo.ehElite(morto) ? 20 : 5;
        darXp(c, qtd);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoAcertar(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity c) || !Companheiros.eh(c) || !(e.getEntity() instanceof LivingEntity alvo)) return;
        combateAte.put(c.getUniqueId(), System.currentTimeMillis() + 8000);
        int est = plugin.settings().evoAtivada ? estagio(c) : 1;
        if (est < 2 || ThreadLocalRandom.current().nextDouble() >= (est == 3 ? 0.25 : 0.15)) return;
        // Golpe Feroz
        e.setDamage(e.getDamage() * 1.5);
        alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
        alvo.getWorld().spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, alvo.getHeight() / 2, 0), 1);
        alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_WOLF_GROWL, 0.7f, est == 3 ? 0.6f : 0.9f);
    }

    /** Vínculo Ancestral: o dono perto de um companheiro de estágio III recebe 8% menos dano. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDonoSofrer(EntityDamageEvent e) {
        if (e.getEntity() instanceof LivingEntity c && Companheiros.eh(c)) {
            combateAte.put(c.getUniqueId(), System.currentTimeMillis() + 8000);
            return;
        }
        if (!(e.getEntity() instanceof Player p) || !plugin.settings().evoAtivada) return;
        for (Companheiros.Registro r : comp().de(p.getUniqueId())) {
            Entity c = Bukkit.getEntity(r.id());
            if (c != null && c.isValid() && c.getWorld().equals(p.getWorld()) && c.getLocation().distanceSquared(p.getLocation()) < 144
                    && estagio(c) == 3) {
                e.setDamage(e.getDamage() * 0.92);
                return;
            }
        }
    }

    /** A cada 2 s: XP das montarias, presença perto do dono, regeneração e partículas. */
    public void tick() {
        if (!plugin.settings().evoAtivada) return;
        passos++;
        long agora = System.currentTimeMillis();
        for (UUID id : List.copyOf(comp().ativos())) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity c) || !c.isValid()) continue;
            UUID d = Companheiros.dono(c);
            Player dono = d == null ? null : Bukkit.getPlayer(d);
            org.bukkit.Location antes = ultimaPosicao.put(id, c.getLocation());
            boolean andou = antes != null && antes.getWorld().equals(c.getWorld()) && antes.distanceSquared(c.getLocation()) > 4;
            if (dono != null && andou && c.getPassengers().contains(dono)) darXp(c, 1);
            else if (dono != null && passos % 30 == 0 && dono.getWorld().equals(c.getWorld())
                    && dono.getLocation().distanceSquared(c.getLocation()) < 576) darXp(c, 1);
            int est = estagio(c);
            if (est < 2) continue;
            if (agora >= combateAte.getOrDefault(id, 0L)) {
                AttributeInstance max = c.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && c.getHealth() < max.getValue()) c.setHealth(Math.min(max.getValue(), c.getHealth() + (est == 3 ? 2 : 1)));
            }
            c.getWorld().spawnParticle(est == 3 ? Particle.END_ROD : Particle.ENCHANTED_HIT,
                    c.getLocation().add(0, c.getHeight() * 0.7, 0), est == 3 ? 3 : 2, 0.3, 0.3, 0.3, 0.01);
        }
        if (combateAte.size() > 200) combateAte.values().removeIf(t -> t < agora);
        if (ultimaPosicao.size() > 200) ultimaPosicao.keySet().retainAll(comp().ativos());
    }

    /** Linha para os menus: "Estágio II · Nível 12 (40/140 XP)". */
    public Component linha(Entity e) {
        int n = nivel(e), est = estagio(e);
        String xp = n >= NIVEL_MAX ? "MÁX" : xpNoNivel(e) + "/" + xpParaProximo(n) + " XP";
        return Component.text("★".repeat(est) + " Estágio " + romano(est) + " · Nível " + n + " (" + xp + ")",
                est == 3 ? NamedTextColor.GOLD : est == 2 ? NamedTextColor.YELLOW : NamedTextColor.GRAY);
    }
}
