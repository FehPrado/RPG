package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.visual.Telas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Boss;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3.9: Armas com memória. Toda arma conta o que derrubou (monstros, chefes e jogadores) e mostra
 * isso na descrição. Pelos marcos ela muda:
 * <ul>
 *   <li><b>100</b> abates: <i>Marcada</i> (a contagem fica dourada).</li>
 *   <li><b>500</b>: <i>Veterana</i> (+3% de dano).</li>
 *   <li><b>1.000</b>: <i>Lendária</i>. O dono dá um <b>nome</b> a ela (uma tela pergunta) e ela ganha
 *       uma <b>habilidade sorteada</b>: Sede de Sangue, Eco, Ímpeto, Ceifadora ou Lembrança.</li>
 * </ul>
 * Chefe vale 25 abates para os marcos e jogador vale 5. Só muda o que está escrito e os efeitos
 * da memória: a raridade e a forja continuam iguais.
 */
public final class ArmasMemoria implements Listener {

    public static final TextColor COR = TextColor.color(0xD4A85A);
    private static final NamespacedKey K_MONSTROS = new NamespacedKey("rpgatributos", "memoria_monstros");
    private static final NamespacedKey K_CHEFES = new NamespacedKey("rpgatributos", "memoria_chefes");
    private static final NamespacedKey K_JOGADORES = new NamespacedKey("rpgatributos", "memoria_jogadores");
    private static final NamespacedKey K_HABILIDADE = new NamespacedKey("rpgatributos", "memoria_habilidade");
    private static final NamespacedKey K_NOMEADA = new NamespacedKey("rpgatributos", "memoria_nomeada");

    public static final int MARCADA = 100, VETERANA = 500, LENDARIA = 1000;

    public enum Habilidade {
        SEDE("Sede de Sangue", "cada abate cura 1 coração"),
        ECO("Eco", "15% de chance de o golpe repetir com metade do dano"),
        IMPETO("Ímpeto", "cada abate dá Velocidade II por 3 s"),
        CEIFADORA("Ceifadora", "+20% de dano em quem tem menos de 30% da vida"),
        LEMBRANCA("Lembrança", "cada abate dá XP em dobro");

        final String nome, efeito;

        Habilidade(String nome, String efeito) {
            this.nome = nome;
            this.efeito = efeito;
        }
    }

    private final RPGAtributos plugin;
    /** Durante o golpe repetido do Eco, ele não repete de novo. */
    private boolean ecoando;

    public ArmasMemoria(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    /** Conta como arma: espadas, machados, tridente, maça, arco, besta e as armas do arsenal. */
    public static boolean ehArma(ItemStack i) {
        if (i == null || i.isEmpty()) return false;
        Material m = i.getType();
        return Tag.ITEMS_SWORDS.isTagged(m) || Tag.ITEMS_AXES.isTagged(m) || m == Material.TRIDENT || m == Material.MACE
                || m == Material.BOW || m == Material.CROSSBOW || Tag.ITEMS_SPEARS.isTagged(m)
                || br.rpgatributos.combo.Arsenal.de(i) != null;
    }

    private static int le(io.papermc.paper.persistence.PersistentDataContainerView c, NamespacedKey k) {
        return c.getOrDefault(k, PersistentDataType.INTEGER, 0);
    }

    /** Os abates que contam para os marcos (chefe vale 25, jogador vale 5). */
    public static int memoria(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return 0;
        var c = i.getPersistentDataContainer();
        return le(c, K_MONSTROS) + 25 * le(c, K_CHEFES) + 5 * le(c, K_JOGADORES);
    }

    public static Habilidade habilidade(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_HABILIDADE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Habilidade.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean ehChefe(Entity e) {
        if (br.rpgatributos.aventura.Chefes.ehChefe(e) || e instanceof Boss || e instanceof Warden || e instanceof ElderGuardian) return true;
        if (e.getPersistentDataContainer().has(br.rpgatributos.estruturas.Estruturas.K_GUARDA)) {
            var hp = e instanceof LivingEntity l ? l.getAttribute(Attribute.MAX_HEALTH) : null;
            return hp != null && hp.getValue() >= 150;
        }
        return e instanceof LivingEntity l && l.getAttribute(Attribute.MAX_HEALTH) != null && l.getAttribute(Attribute.MAX_HEALTH).getValue() >= 200;
    }

    // =====================================================================
    //  Contar
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || p == morto || br.rpgatributos.ModoTeste.ehTeste(p)) return;
        ItemStack arma = p.getInventory().getItemInMainHand();
        if (!ehArma(arma)) return;
        NamespacedKey k;
        if (morto instanceof Player) k = K_JOGADORES;
        else if (ehChefe(morto)) k = K_CHEFES;
        else if (morto instanceof Enemy) k = K_MONSTROS;
        else return;
        int antes = memoria(arma);
        arma.editMeta(m -> {
            m.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, le(m.getPersistentDataContainer(), k) + 1);
        });
        int depois = memoria(arma);
        if (antes < MARCADA && depois >= MARCADA) marco(p, arma, "Marcada", "A arma começou a lembrar das batalhas.");
        if (antes < VETERANA && depois >= VETERANA) marco(p, arma, "Veterana", "Ela conhece cada golpe seu: +3% de dano.");
        if (antes < LENDARIA && depois >= LENDARIA) lendaria(p, arma);
        escrever(arma);
        // As habilidades que agem no abate.
        Habilidade h = habilidade(arma);
        if (h == null) return;
        switch (h) {
            case SEDE -> {
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && !p.isDead()) p.setHealth(Math.min(max.getValue(), p.getHealth() + 2));
            }
            case IMPETO -> p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, true, true));
            case LEMBRANCA -> e.setDroppedExp(e.getDroppedExp() * 2);
            default -> { }
        }
    }

    /** Admin: a arma passa a lembrar de tantos monstros e chefes (o marco vem no próximo abate). */
    public void definir(Player p, ItemStack arma, int monstros, int chefes) {
        arma.editMeta(m -> {
            m.getPersistentDataContainer().set(K_MONSTROS, PersistentDataType.INTEGER, Math.max(0, monstros));
            m.getPersistentDataContainer().set(K_CHEFES, PersistentDataType.INTEGER, Math.max(0, chefes));
        });
        escrever(arma);
    }

    private void marco(Player p, ItemStack arma, String titulo, String frase) {
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.4f);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1, 0), 40, 0.5, 0.6, 0.5, 0.5);
        p.sendMessage(Component.text("⚔ " + nome(arma) + " agora é " + titulo + ". " + frase, COR));
    }

    /** 1.000 abates: sorteia a habilidade e pergunta o nome. */
    private void lendaria(Player p, ItemStack arma) {
        Habilidade h = Habilidade.values()[ThreadLocalRandom.current().nextInt(Habilidade.values().length)];
        arma.editMeta(m -> m.getPersistentDataContainer().set(K_HABILIDADE, PersistentDataType.STRING, h.name()));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.3);
        plugin.titulos().registrar(p, "armas_lendarias", 1);
        plugin.diario().marco(p, "arma_lendaria", "Uma arma lembrou de mil batalhas");
        perguntarNome(p);
        lembrarNome(p);
    }

    /** A tela do nome (também pelo comando /arma nomear, se o dono fechou a tela antes). */
    public void perguntarNome(Player p) {
        ItemStack arma = p.getInventory().getItemInMainHand();
        if (!ehArma(arma) || memoria(arma) < LENDARIA) {
            p.sendMessage(Component.text("Segure uma arma com 1.000 abates de memória.", NamedTextColor.RED));
            return;
        }
        Habilidade h = habilidade(arma);
        Telas.perguntar(p, Component.text("⚔ Uma arma lendária", COR, TextDecoration.BOLD), List.of(
                        Component.text("Ela lembra de mil batalhas. Que nome ela vai ter?", NamedTextColor.WHITE),
                        Component.text("Habilidade: " + (h == null ? "?" : h.nome + " (" + h.efeito + ")"), NamedTextColor.GRAY)),
                "Nome da arma", "", 32, (q, nome) -> {
                    ItemStack a = q.getInventory().getItemInMainHand();
                    if (!ehArma(a) || memoria(a) < LENDARIA) {
                        q.sendMessage(Component.text("Segure a arma na mão para dar o nome.", NamedTextColor.RED));
                        return;
                    }
                    String limpo = nome.replaceAll("[§&]", "");
                    a.editMeta(m -> {
                        m.customName(Component.text(limpo, COR, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                        m.getPersistentDataContainer().set(K_NOMEADA, PersistentDataType.BYTE, (byte) 1);
                    });
                    escrever(a);
                    q.sendMessage(Component.text("⚔ Daqui em diante ela se chama " + limpo + ".", COR));
                    q.playSound(q.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.4f);
                });
    }

    private final java.util.Map<java.util.UUID, Long> lembrado = new java.util.HashMap<>();

    /** Uma linha no chat com [dar um nome], para quem fechou a tela sem responder. */
    private void lembrarNome(Player p) {
        p.sendMessage(Component.text("⚔ Sua arma lendária ainda não tem nome. ", COR)
                .append(Component.text("[dar um nome]", NamedTextColor.YELLOW, TextDecoration.UNDERLINED)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.callback(a -> {
                            if (a instanceof Player q) perguntarNome(q);
                        }, net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).lifetime(java.time.Duration.ofMinutes(10)).build()))));
    }

    /** Pegou na mão uma lendária sem nome: lembra (no máximo a cada 5 minutos). */
    @EventHandler
    public void aoTrocarMao(org.bukkit.event.player.PlayerItemHeldEvent e) {
        ItemStack i = e.getPlayer().getInventory().getItem(e.getNewSlot());
        if (!ehArma(i) || memoria(i) < LENDARIA || i.getPersistentDataContainer().has(K_NOMEADA)) return;
        long agora = System.currentTimeMillis();
        if (lembrado.getOrDefault(e.getPlayer().getUniqueId(), 0L) > agora) return;
        lembrado.put(e.getPlayer().getUniqueId(), agora + 5 * 60_000L);
        lembrarNome(e.getPlayer());
    }

    private static String nome(ItemStack i) {
        ItemMeta m = i.getItemMeta();
        Component c = m.hasCustomName() ? m.customName() : m.hasItemName() ? m.itemName() : null;
        return c == null ? "A arma" : PlainTextComponentSerializer.plainText().serialize(c);
    }

    // =====================================================================
    //  A descrição
    // =====================================================================

    /** Reescreve as linhas da memória no fim da descrição (as outras linhas ficam como estão). */
    public static void escrever(ItemStack arma) {
        arma.editMeta(m -> {
            var c = m.getPersistentDataContainer();
            int mon = le(c, K_MONSTROS), che = le(c, K_CHEFES), jog = le(c, K_JOGADORES);
            if (mon + che + jog == 0) return;
            List<Component> lore = new ArrayList<>(m.hasLore() ? m.lore() : List.of());
            lore.removeIf(l -> {
                String s = PlainTextComponentSerializer.plainText().serialize(l);
                return s.startsWith("⚔ Memória") || s.startsWith("✦ Habilidade da memória") || s.startsWith("✦ Golpe da lenda");
            });
            int total = mon + 25 * che + 5 * jog;
            String grau = total >= LENDARIA ? " · Lendária" : total >= VETERANA ? " · Veterana" : total >= MARCADA ? " · Marcada" : "";
            StringBuilder sb = new StringBuilder("⚔ Memória: " + mon + " monstro" + (mon == 1 ? "" : "s"));
            if (che > 0) sb.append(" · ").append(che).append(che == 1 ? " chefe" : " chefes");
            if (jog > 0) sb.append(" · ").append(jog).append(jog == 1 ? " jogador" : " jogadores");
            sb.append(grau);
            lore.add(Component.text(sb.toString(), total >= MARCADA ? COR : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            Habilidade h = habilidade(arma);
            if (h != null) {
                lore.add(Component.text("✦ Habilidade da memória: " + h.nome + " (" + h.efeito + ")", TextColor.color(0xFFD27F)).decoration(TextDecoration.ITALIC, false));
                lore.add(linhaGolpe(h));
            }
            m.lore(lore);
        });
    }

    // =====================================================================
    //  Os efeitos no golpe
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoBater(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        Player p = d instanceof Player pl ? pl : d instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null || !(e.getEntity() instanceof LivingEntity alvo)) return;
        ItemStack arma = p.getInventory().getItemInMainHand();
        if (!ehArma(arma)) return;
        int mem = memoria(arma);
        if (mem >= VETERANA) e.setDamage(e.getDamage() * 1.03);
        Habilidade h = habilidade(arma);
        if (h != null && d instanceof Player && alvo instanceof Enemy && !ecoando) carregar(p, arma);
        if (h == Habilidade.CEIFADORA) {
            var max = alvo.getAttribute(Attribute.MAX_HEALTH);
            if (max != null && alvo.getHealth() < max.getValue() * 0.3) e.setDamage(e.getDamage() * 1.2);
        } else if (h == Habilidade.ECO && !ecoando && d instanceof Player && ThreadLocalRandom.current().nextDouble() < 0.15) {
            double metade = e.getDamage() * 0.5;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!alvo.isValid() || alvo.isDead() || !p.isOnline()) return;
                ecoando = true;
                try {
                    alvo.setNoDamageTicks(0);
                    alvo.damage(metade, p);
                } finally {
                    ecoando = false;
                }
                alvo.getWorld().spawnParticle(Particle.SWEEP_ATTACK, alvo.getLocation().add(0, 1, 0), 1);
                alvo.getWorld().playSound(alvo.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);
            }, 6L);
        }
    }

    // =====================================================================
    //  3.18: o golpe da lenda (o 5º golpe, só da arma Lendária)
    // =====================================================================

    /** Golpes em monstros para a arma despertar. */
    public static final int CARGA_CHEIA = 12;
    private final java.util.Map<java.util.UUID, Integer> carga = new java.util.HashMap<>();

    /** O nome do golpe de cada habilidade. */
    public static String golpeDa(Habilidade h) {
        return switch (h) {
            case SEDE -> "Banquete Rubro";
            case ECO -> "Eco Sem Fim";
            case IMPETO -> "Investida da Lenda";
            case CEIFADORA -> "Colheita";
            case LEMBRANCA -> "Chamado dos Caídos";
        };
    }

    private static String descricaoGolpe(Habilidade h) {
        return switch (h) {
            case SEDE -> "corte em leque à frente; cura 30% do dano feito";
            case ECO -> "três golpes seguidos no inimigo mais perto";
            case IMPETO -> "avança 6 blocos ferindo quem estiver no caminho";
            case CEIFADORA -> "giro em volta; quem tiver menos de 15% da vida cai (menos chefes)";
            case LEMBRANCA -> "explosão de memórias: fere em volta e deixa os inimigos fracos e marcados";
        };
    }

    private void carregar(Player p, ItemStack arma) {
        int antes = carga.getOrDefault(p.getUniqueId(), 0);
        if (antes >= CARGA_CHEIA) return;
        int agora = antes + 1;
        carga.put(p.getUniqueId(), agora);
        if (agora < CARGA_CHEIA) return;
        Habilidade h = habilidade(arma);
        p.sendActionBar(Component.text("✦ A arma desperta: agachado + clique direito = " + golpeDa(h), NamedTextColor.GOLD, TextDecoration.BOLD));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.6f);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.2, 0), 15, 0.3, 0.4, 0.3, 0.03);
    }

    /** A linha do golpe na descrição da arma Lendária. */
    public static Component linhaGolpe(Habilidade h) {
        return Component.text("✦ Golpe da lenda: " + golpeDa(h) + " (" + descricaoGolpe(h) + ")", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoSoltarGolpe(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || !e.getPlayer().isSneaking()) return;
        var a = e.getAction();
        if (a != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && a != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (a == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && (e.getClickedBlock().getType().isInteractable() || plugin.ehEstacao(e.getClickedBlock()))) return;
        Player p = e.getPlayer();
        ItemStack arma = p.getInventory().getItemInMainHand();
        Habilidade h = habilidade(arma);
        if (h == null || memoria(arma) < LENDARIA) return;
        int c = carga.getOrDefault(p.getUniqueId(), 0);
        if (c < CARGA_CHEIA) {
            p.sendActionBar(Component.text("A arma ainda dorme: " + c + "/" + CARGA_CHEIA + " golpes em monstros", NamedTextColor.GRAY));
            return;
        }
        e.setCancelled(true);
        carga.put(p.getUniqueId(), 0);
        golpeDaLenda(p, h);
        plugin.titulos().registrar(p, "golpes_da_lenda", 1);
    }

    private double danoBase(Player p) {
        var a = p.getAttribute(Attribute.ATTACK_DAMAGE);
        return Math.max(4, a == null ? 4 : a.getValue());
    }

    private List<LivingEntity> inimigosPerto(Player p, double raio) {
        List<LivingEntity> l = new ArrayList<>();
        for (Entity en : p.getNearbyEntities(raio, raio / 2 + 1, raio)) {
            if (en instanceof LivingEntity le && !le.isDead() && plugin.alvos().inimigo(p, le)) l.add(le);
        }
        return l;
    }

    /** O golpe próprio de cada arma lendária (também pelo /rpgadmin). */
    public void golpeDaLenda(Player p, Habilidade h) {
        double base = danoBase(p);
        var w = p.getWorld();
        org.bukkit.Location c = p.getLocation();
        org.bukkit.util.Vector frente = c.getDirection().setY(0);
        if (frente.lengthSquared() < 0.01) frente = new org.bukkit.util.Vector(1, 0, 0);
        frente.normalize();
        p.showTitle(net.kyori.adventure.title.Title.title(Component.empty(), Component.text("✦ " + golpeDa(h), NamedTextColor.GOLD, TextDecoration.BOLD),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(50), java.time.Duration.ofMillis(900), java.time.Duration.ofMillis(300))));
        w.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 0.5f, 1.5f);
        switch (h) {
            case SEDE -> {
                double total = 0;
                for (LivingEntity le : inimigosPerto(p, 5)) {
                    org.bukkit.util.Vector ate = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                    if (ate.lengthSquared() > 0.01 && ate.normalize().dot(frente) < 0.4) continue;
                    double antes = le.getHealth();
                    plugin.alvos().ferir(p, le, base * 2);
                    total += Math.max(0, antes - le.getHealth());
                    w.spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(0xB71C1C), 1.5f));
                }
                var max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + total * 0.3));
                w.spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(frente.clone().multiply(2)).add(0, 1, 0), 5, 1, 0.3, 1, 0);
            }
            case ECO -> {
                LivingEntity alvo = null;
                double melhor = 36;
                for (LivingEntity le : inimigosPerto(p, 6)) {
                    double d = le.getLocation().distanceSquared(c);
                    if (d < melhor) { melhor = d; alvo = le; }
                }
                if (alvo == null) return;
                LivingEntity a = alvo;
                for (int i = 0; i < 3; i++) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!a.isValid() || a.isDead() || !p.isOnline()) return;
                        a.setNoDamageTicks(0);
                        plugin.alvos().ferir(p, a, base * 1.2);
                        w.spawnParticle(Particle.SONIC_BOOM, a.getLocation().add(0, 1, 0), 1);
                        w.playSound(a.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.8f);
                    }, i * 5L);
                }
            }
            case IMPETO -> {
                p.setVelocity(frente.clone().multiply(1.8).setY(0.2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 1));
                java.util.Set<java.util.UUID> ja = new java.util.HashSet<>();
                for (int i = 1; i <= 6; i++) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!p.isOnline()) return;
                        for (LivingEntity le : inimigosPerto(p, 2.2)) {
                            if (!ja.add(le.getUniqueId())) continue;
                            plugin.alvos().ferir(p, le, base * 1.5);
                            le.setVelocity(le.getVelocity().setY(0.4));
                        }
                        w.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 4, 0.2, 0.2, 0.2, 0.02);
                    }, i * 2L);
                }
            }
            case CEIFADORA -> {
                for (int i = 0; i < 16; i++) {
                    double ang = i * Math.PI / 8;
                    w.spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(Math.cos(ang) * 2.5, 1, Math.sin(ang) * 2.5), 1);
                }
                for (LivingEntity le : inimigosPerto(p, 4)) {
                    var max = le.getAttribute(Attribute.MAX_HEALTH);
                    boolean chefe = max != null && max.getValue() >= 150;
                    if (!chefe && max != null && le.getHealth() < max.getValue() * 0.15) {
                        plugin.alvos().ferir(p, le, le.getHealth() + 1);
                    } else {
                        plugin.alvos().ferir(p, le, base * 1.5);
                    }
                }
            }
            case LEMBRANCA -> {
                w.spawnParticle(Particle.SOUL, c.clone().add(0, 1, 0), 60, 2.5, 1, 2.5, 0.05);
                w.playSound(c, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.8f);
                for (LivingEntity le : inimigosPerto(p, 5)) {
                    plugin.alvos().ferir(p, le, base * 1.2);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0));
                }
            }
        }
    }

    /** Para o /rpgadmin: enche a carga da arma lendária. */
    public void encher(Player p) {
        carga.put(p.getUniqueId(), CARGA_CHEIA);
    }
}
