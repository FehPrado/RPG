package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.forja.DadosForja.EfeitoRolado;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/** Faz os bônus especiais e os efeitos dos itens forjados funcionarem. */
public final class EfeitosListener implements Listener {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final double LIMITE_EXECUCAO = 0.30;
    private static final double BONUS_EXECUCAO = 0.35;
    private static final double BONUS_CRITICO = 0.5;
    private static final double DANO_RAIO = 5;

    /** Item forjado num slot onde ele funciona. */
    private record Ativo(EquipmentSlot slot, ItemStack item, Categoria cat, DadosForja dados) {}

    private static final EquipmentSlot[] ARMADURA = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final RPGAtributos plugin;
    /** Efeitos passivos que o plugin está dando para cada jogador (tipo → nível). */
    private final Map<UUID, Map<PotionEffectType, Integer>> concedidos = new HashMap<>();
    /** Outros sistemas (núcleos lendários, títulos...) que também dão efeitos passivos. */
    private final List<Function<Player, Map<PotionEffectType, Integer>>> fontes = new ArrayList<>();
    /** Bônus de conjunto aplicado agora em cada jogador. */
    private final Map<UUID, Raridade> conjuntoAtual = new HashMap<>();
    private final NamespacedKey kConjArmadura, kConjVida, kConjVelocidade, kConjResistencia;
    private final Map<UUID, Integer> esperaAtaque = new HashMap<>();
    private final Map<UUID, Integer> esperaDefesa = new HashMap<>();
    /** Evita que espinhos causem espinhos de volta (loop). */
    private boolean refletindo;

    public EfeitosListener(RPGAtributos plugin) {
        this.plugin = plugin;
        kConjArmadura = new NamespacedKey(plugin, "conjunto_armadura");
        kConjVida = new NamespacedKey(plugin, "conjunto_vida");
        kConjVelocidade = new NamespacedKey(plugin, "conjunto_velocidade");
        kConjResistencia = new NamespacedKey(plugin, "conjunto_resistencia");
    }

    /** Registra outra fonte de efeitos passivos (aplicados e renovados junto com os dos itens). */
    public void registrarFonte(Function<Player, Map<PotionEffectType, Integer>> fonte) {
        fontes.add(fonte);
    }

    private Settings cfg() { return plugin.settings(); }
    private Forja forja() { return plugin.forja(); }
    private static double rnd() { return ThreadLocalRandom.current().nextDouble(); }

    private List<Ativo> ativos(Player p) {
        List<Ativo> l = new ArrayList<>(SLOTS.length);
        EntityEquipment eq = p.getEquipment();
        for (EquipmentSlot s : SLOTS) {
            ItemStack item = eq.getItem(s);
            if (item.isEmpty()) continue;
            Categoria c = Categoria.de(item.getType());
            if (c == null || !c.ativoEm(s)) continue;
            DadosForja d = forja().ler(item);
            if (d != null) l.add(new Ativo(s, item, c, d));
        }
        return l;
    }

    private double soma(List<Ativo> ativos, Stat s) {
        double total = 0;
        for (Ativo a : ativos) total += forja().valor(a.dados(), a.item().getType(), s);
        return total;
    }

    /** Respeita o intervalo mínimo entre ativações para não virar spam. */
    private static boolean pronto(Map<UUID, Integer> espera, Player p, int intervaloTicks) {
        int agora = Bukkit.getCurrentTick();
        Integer ultimo = espera.get(p.getUniqueId());
        if (ultimo != null && agora - ultimo < intervaloTicks) return false;
        espera.put(p.getUniqueId(), agora);
        return true;
    }

    private static Location meio(Entity e) {
        return e.getLocation().add(0, e.getHeight() / 2, 0);
    }

    private static void mostrar(Entity e, Efeito ef) {
        if (ef.particula() != null) e.getWorld().spawnParticle(ef.particula(), meio(e), 14, 0.3, 0.4, 0.3, 0.05);
        if (ef.som() != null) e.getWorld().playSound(e.getLocation(), ef.som(), 0.8f, 1.1f);
    }

    // =====================================================================
    //  Passivos (roda 1x por segundo)
    // =====================================================================

    public void aplicarPassivos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Map<PotionEffectType, Integer> desejado = new HashMap<>();
            if (!p.isDead()) {
                List<Ativo> ativos = ativos(p);
                for (Ativo a : ativos) {
                    for (EfeitoRolado er : a.dados().efeitos()) {
                        if (er.efeito().gatilho() == Efeito.Gatilho.PASSIVO) {
                            desejado.merge(er.efeito().pocao(), er.nivel(), Math::max);
                        }
                    }
                }
                Raridade conjunto = aplicarConjunto(p, ativos);
                if (conjunto != null) {
                    Conjuntos.Bonus b = Conjuntos.de(conjunto);
                    if (b.regeneracao()) desejado.merge(PotionEffectType.REGENERATION, 0, Math::max);
                    if (b.resistenciaPocao()) desejado.merge(PotionEffectType.RESISTANCE, 0, Math::max);
                }
                for (var fonte : fontes) {
                    fonte.apply(p).forEach((tipo, nivel) -> desejado.merge(tipo, nivel, Math::max));
                }
            }

            Map<PotionEffectType, Integer> antes = concedidos.getOrDefault(p.getUniqueId(), Map.of());
            antes.forEach((tipo, nivel) -> {
                Integer agora = desejado.get(tipo);
                if (agora == null || agora < nivel) removerNosso(p, tipo, nivel);
            });

            desejado.forEach((tipo, nivel) -> {
                boolean visao = tipo == PotionEffectType.NIGHT_VISION;
                int duracao = visao ? 400 : 100;    // visão noturna pisca quando falta menos de 10s
                int renovarAbaixo = visao ? 220 : 40;
                PotionEffect atual = p.getPotionEffect(tipo);
                boolean renovar = atual == null
                        || atual.getAmplifier() < nivel
                        || (atual.getAmplifier() == nivel && atual.isAmbient() && !atual.isInfinite()
                            && atual.getDuration() < renovarAbaixo);
                // ambient = sem partículas fortes, igual efeito de sinalizador
                if (renovar) p.addPotionEffect(new PotionEffect(tipo, duracao, nivel, true, false, true));
            });

            if (desejado.isEmpty()) concedidos.remove(p.getUniqueId());
            else concedidos.put(p.getUniqueId(), desejado);
        }
    }

    /**
     * Conjunto: as 4 peças de armadura forjadas; vale a raridade mais baixa entre elas.
     * Só mexe nos atributos quando o conjunto muda.
     * @return a raridade do conjunto, ou null se não está completo.
     */
    private Raridade aplicarConjunto(Player p, List<Ativo> ativos) {
        Raridade menor = null;
        int pecas = 0;
        for (Ativo a : ativos) {
            if (!a.cat().armadura()) continue;
            pecas++;
            if (menor == null || a.dados().raridade().ordinal() < menor.ordinal()) menor = a.dados().raridade();
        }
        Raridade conjunto = pecas == ARMADURA.length ? menor : null;
        if (conjunto == conjuntoAtual.get(p.getUniqueId())) return conjunto;

        if (conjunto == null) conjuntoAtual.remove(p.getUniqueId());
        else conjuntoAtual.put(p.getUniqueId(), conjunto);
        Conjuntos.Bonus b = conjunto == null ? null : Conjuntos.de(conjunto);
        definir(p, Attribute.ARMOR, kConjArmadura, b == null ? 0 : b.armadura(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MAX_HEALTH, kConjVida, b == null ? 0 : b.vida(), AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MOVEMENT_SPEED, kConjVelocidade, b == null ? 0 : b.velocidade(), AttributeModifier.Operation.ADD_SCALAR);
        definir(p, Attribute.ARMOR_TOUGHNESS, kConjResistencia, b == null ? 0 : b.resistencia(), AttributeModifier.Operation.ADD_NUMBER);
        if (conjunto != null) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("✦ Conjunto " + conjunto.nome() + ": "
                    + Conjuntos.texto(conjunto), conjunto.cor()));
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 0.6f, 1.3f);
        }
        return conjunto;
    }

    private static void definir(Player p, Attribute atributo, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(atributo);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        if (atributo == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    /** Tira o efeito só se for o que o plugin deu (não mexe em poção bebida). */
    private static void removerNosso(Player p, PotionEffectType tipo, int nivel) {
        PotionEffect atual = p.getPotionEffect(tipo);
        if (atual != null && atual.isAmbient() && atual.getAmplifier() == nivel && atual.getDuration() <= 400) {
            p.removePotionEffect(tipo);
        }
    }

    public void removerTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Map<PotionEffectType, Integer> m = concedidos.get(p.getUniqueId());
            if (m != null) m.forEach((tipo, nivel) -> removerNosso(p, tipo, nivel));
        }
        concedidos.clear();
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        Map<PotionEffectType, Integer> m = concedidos.remove(id);
        if (m != null) m.forEach((tipo, nivel) -> removerNosso(e.getPlayer(), tipo, nivel));
        esperaAtaque.remove(id);
        esperaDefesa.remove(id);
        conjuntoAtual.remove(id);
    }

    // =====================================================================
    //  Combate
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoDano(EntityDamageByEntityEvent e) {
        if (refletindo || !(e.getEntity() instanceof LivingEntity alvo) || alvo instanceof ArmorStand) return;
        DamageCause causa = e.getCause();
        if (causa == DamageCause.THORNS) return;

        Entity fonte = e.getDamager();
        Player atacante = null;
        ItemStack arma = null;
        boolean projetil = false;
        if (fonte instanceof Player p && (causa == DamageCause.ENTITY_ATTACK || causa == DamageCause.ENTITY_SWEEP_ATTACK)) {
            atacante = p;
            arma = p.getInventory().getItemInMainHand();
        } else if (fonte instanceof AbstractArrow flecha && flecha.getShooter() instanceof Player p) {
            atacante = p;
            arma = flecha.getWeapon();
            projetil = true;
        }

        LivingEntity agressor = null;
        if (fonte instanceof LivingEntity le) agressor = le;
        else if (fonte instanceof Projectile pr && pr.getShooter() instanceof LivingEntity le) agressor = le;

        // 1) Esquiva: cancela tudo (só contra ataques de mobs, jogadores e projéteis; explosão não)
        List<Ativo> defesa = List.of();
        if (alvo instanceof Player defensor && agressor != null && agressor != defensor) {
            defesa = ativos(defensor);
            double esquiva = Math.min(cfg().forjaEsquivaMax, soma(defesa, Stat.ESQUIVA));
            if (esquiva > 0 && rnd() < esquiva) {
                e.setCancelled(true);
                defensor.getWorld().spawnParticle(Particle.CLOUD, meio(defensor), 12, 0.3, 0.4, 0.3, 0.03);
                defensor.getWorld().playSound(defensor.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 0.8f, 1.3f);
                defensor.sendActionBar(Component.text("↺ Esquivou!", NamedTextColor.AQUA));
                return;
            }
        }

        // 2) Arma de quem atacou
        if (atacante != null && atacante != alvo && arma != null && !arma.isEmpty()) {
            atacar(e, atacante, arma, alvo, projetil);
        }

        // 3) Armadura de quem apanhou
        if (alvo instanceof Player defensor && !defesa.isEmpty() && agressor != null && agressor != defensor) {
            defender(e, defensor, defesa, agressor, fonte == agressor);
        }
    }

    private void atacar(EntityDamageByEntityEvent e, Player atacante, ItemStack arma, LivingEntity alvo, boolean projetil) {
        DadosForja d = forja().ler(arma);
        Categoria cat = Categoria.de(arma.getType());
        if (d == null || cat == null) return;
        if (projetil ? !cat.armaDistancia() : !cat.armaCorpoACorpo()) return;
        Material tipo = arma.getType();
        boolean varredura = e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK;

        double mult = 1;
        if (projetil) mult += forja().valor(d, tipo, Stat.DANO_FLECHA) + 0.03 * d.refino(); // refino: +3% por nível
        if (!varredura) {
            double critico = forja().valor(d, tipo, Stat.CRITICO);
            if (critico > 0 && rnd() < critico) {
                mult += BONUS_CRITICO;
                alvo.getWorld().spawnParticle(Particle.CRIT, meio(alvo), 25, 0.3, 0.4, 0.3, 0.3);
                alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.9f);
                atacante.sendActionBar(Component.text("✷ CRÍTICO!", NamedTextColor.GOLD));
            }
        }
        if (d.efeito(Efeito.EXECUCAO) != null && vidaRelativa(alvo) <= LIMITE_EXECUCAO) {
            mult += BONUS_EXECUCAO;
            mostrar(alvo, Efeito.EXECUCAO);
        }
        if (mult != 1) e.setDamage(e.getDamage() * mult);

        if (!varredura && d.temGatilho(Efeito.Gatilho.AO_ACERTAR)
                && pronto(esperaAtaque, atacante, cfg().forjaIntervaloAtaque)) {
            for (EfeitoRolado er : d.efeitos()) {
                Efeito ef = er.efeito();
                if (ef.gatilho() != Efeito.Gatilho.AO_ACERTAR || ef == Efeito.EXECUCAO || rnd() >= er.chance()) continue;
                switch (ef) {
                    case CHAMAS -> alvo.setFireTicks(Math.max(alvo.getFireTicks(), ef.duracao()));
                    case CONGELAR -> alvo.setFreezeTicks(Math.max(alvo.getFreezeTicks(), alvo.getMaxFreezeTicks() + ef.duracao()));
                    case RAIO -> {
                        alvo.getWorld().strikeLightningEffect(alvo.getLocation());
                        e.setDamage(e.getDamage() + DANO_RAIO);
                    }
                    default -> {
                        if (ef.pocao() != null) alvo.addPotionEffect(new PotionEffect(ef.pocao(), ef.duracao(), er.nivel()));
                    }
                }
                mostrar(alvo, ef);
            }
        }

        double roubo = forja().valor(d, tipo, Stat.ROUBO_VIDA);
        if (roubo > 0) curar(atacante, e.getFinalDamage() * roubo);
    }

    private void defender(EntityDamageByEntityEvent e, Player defensor, List<Ativo> defesa,
                          LivingEntity agressor, boolean corpoACorpo) {
        // Reflete parte do dano de golpes corpo a corpo
        double espinhos = Math.min(cfg().forjaEspinhosMax, soma(defesa, Stat.ESPINHOS));
        if (corpoACorpo && espinhos > 0) {
            double refletido = e.getFinalDamage() * espinhos;
            if (refletido >= 0.5) {
                refletindo = true;
                try {
                    agressor.damage(refletido, DamageSource.builder(DamageType.THORNS)
                            .withCausingEntity(defensor).withDirectEntity(defensor).build());
                } finally {
                    refletindo = false;
                }
                agressor.getWorld().spawnParticle(Particle.ENCHANTED_HIT, meio(agressor), 10, 0.3, 0.4, 0.3, 0.1);
                agressor.getWorld().playSound(agressor.getLocation(), Sound.ENCHANT_THORNS_HIT, 0.8f, 1f);
            }
        }

        boolean temEfeito = false;
        for (Ativo a : defesa) temEfeito |= a.dados().temGatilho(Efeito.Gatilho.AO_SER_ATINGIDO);
        if (!temEfeito || !pronto(esperaDefesa, defensor, cfg().forjaIntervaloDefesa)) return;

        for (Ativo a : defesa) {
            if (!a.cat().defesa()) continue;
            for (EfeitoRolado er : a.dados().efeitos()) {
                Efeito ef = er.efeito();
                if (ef.gatilho() != Efeito.Gatilho.AO_SER_ATINGIDO || rnd() >= er.chance()) continue;
                Entity onde = defensor;
                switch (ef) {
                    case CONTRA_LENTIDAO -> {
                        agressor.addPotionEffect(new PotionEffect(ef.pocao(), ef.duracao(), er.nivel()));
                        onde = agressor;
                    }
                    case CONTRA_CHAMAS -> {
                        agressor.setFireTicks(Math.max(agressor.getFireTicks(), ef.duracao()));
                        onde = agressor;
                    }
                    case RAJADA -> {
                        Vector v = agressor.getLocation().toVector().subtract(defensor.getLocation().toVector()).setY(0);
                        if (v.lengthSquared() < 1e-4) v = defensor.getLocation().getDirection().setY(0);
                        if (v.lengthSquared() < 1e-4) v = new Vector(1, 0, 0);
                        agressor.setVelocity(v.normalize().multiply(1.4).setY(0.5));
                        onde = agressor;
                    }
                    default -> defensor.addPotionEffect(new PotionEffect(ef.pocao(), ef.duracao(), er.nivel()));
                }
                mostrar(onde, ef);
            }
        }
    }

    private static double vidaRelativa(LivingEntity e) {
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        return max == null || max.getValue() <= 0 ? 1 : e.getHealth() / max.getValue();
    }

    private static void curar(Player p, double quanto) {
        if (quanto <= 0 || p.isDead()) return;
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        if (max == null) return;
        p.setHealth(Math.min(max.getValue(), p.getHealth() + quanto));
    }

    // =====================================================================
    //  Mineração
    // =====================================================================

    private static boolean ehMinerio(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    private DadosForja ferramenta(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        Categoria c = Categoria.de(item.getType());
        if (c == null || !c.ferramenta()) return null;
        return forja().ler(item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        DadosForja d = ferramenta(p);
        if (d == null) return;

        for (EfeitoRolado er : d.efeitos()) {
            Efeito ef = er.efeito();
            if (ef == Efeito.FRENESI && rnd() < er.chance()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ef.duracao(), er.nivel()));
                mostrar(p, ef);
            } else if (ef == Efeito.SABEDORIA && ehMinerio(e.getBlock().getType()) && rnd() < er.chance()) {
                e.setExpToDrop(e.getExpToDrop() + 2 + ThreadLocalRandom.current().nextInt(4));
                Location l = e.getBlock().getLocation().toCenterLocation();
                l.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, l, 10, 0.3, 0.3, 0.3);
            }
        }
    }

    /** Fundição: minério bruto já sai em lingote. Roda antes do drop duplo da Mineração. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoDropar(BlockDropItemEvent e) {
        DadosForja d = ferramenta(e.getPlayer());
        if (d == null || d.efeito(Efeito.FUNDICAO) == null) return;
        boolean fundiu = false;
        for (Item item : e.getItems()) {
            ItemStack s = item.getItemStack();
            Material lingote = switch (s.getType()) {
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                default -> null;
            };
            if (lingote == null) continue;
            item.setItemStack(s.withType(lingote));
            fundiu = true;
        }
        if (fundiu) {
            Location l = e.getBlock().getLocation().toCenterLocation();
            l.getWorld().spawnParticle(Particle.FLAME, l, 12, 0.25, 0.25, 0.25, 0.02);
            l.getWorld().playSound(l, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1f, 1.2f);
        }
    }

    // =====================================================================
    //  Durabilidade
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoGastar(PlayerItemDamageEvent e) {
        double chance = forja().valor(e.getItem(), Stat.INQUEBRAVEL);
        if (chance > 0 && rnd() < chance) e.setCancelled(true);
    }
}
