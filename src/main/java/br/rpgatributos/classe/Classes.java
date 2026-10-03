package br.rpgatributos.classe;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.arcano.Perfil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classes: nível de classe (1 a 20, sobe com o XP dos atributos da classe), requisitos,
 * caminho e tarefas, passivos, bônus usados pelos outros sistemas e as habilidades
 * (agachar + F usa; agachar + clique esquerdo no ar troca).
 */
public final class Classes implements Listener {

    public static final int NIVEL_MAXIMO = 20;
    private static final NamespacedKey M_VIDA = new NamespacedKey("rpgatributos", "classe_vida");
    private static final NamespacedKey M_ARMADURA = new NamespacedKey("rpgatributos", "classe_armadura");
    private static final NamespacedKey M_VELOCIDADE = new NamespacedKey("rpgatributos", "classe_velocidade");

    private final RPGAtributos plugin;
    private final Habilidades habilidades;
    private final Map<UUID, PerfilClasse> perfis = new HashMap<>();
    /** Recargas: jogador → habilidade → quando fica pronta. */
    private final Map<UUID, Map<Habilidade, Long>> recargas = new HashMap<>();

    /** Dono de cada classe lendária (uma pessoa por vez). */
    private final Map<Classe, UUID> donosLendarias = new java.util.EnumMap<>(Classe.class);
    private final Map<Classe, String> nomesDonos = new java.util.EnumMap<>(Classe.class);
    private final java.io.File arquivoLendarias;

    public Classes(RPGAtributos plugin) {
        this.plugin = plugin;
        this.habilidades = new Habilidades(plugin);
        this.arquivoLendarias = new java.io.File(plugin.getDataFolder(), "classes_lendarias.yml");
    }

    // =====================================================================
    //  Classes lendárias: um dono por vez
    // =====================================================================

    public UUID donoLendaria(Classe c) { return donosLendarias.get(c); }
    public String nomeDonoLendaria(Classe c) { return nomesDonos.getOrDefault(c, "?"); }

    /** Lê o arquivo e libera as lendárias de quem sumiu por muitos dias. */
    public void carregarLendarias() {
        donosLendarias.clear();
        nomesDonos.clear();
        if (!arquivoLendarias.exists()) return;
        var y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(arquivoLendarias);
        long limite = cfg().claDiasInativoLendaria * 86_400_000L;
        boolean mudou = false;
        for (String id : y.getKeys(false)) {
            Classe c = Classe.porId(id);
            if (c == null || c.tier() != Classe.Tier.LENDARIA) continue;
            try {
                UUID dono = UUID.fromString(y.getString(id + ".dono", ""));
                var off = Bukkit.getOfflinePlayer(dono);
                if (limite > 0 && !off.isOnline() && off.getLastSeen() > 0 && System.currentTimeMillis() - off.getLastSeen() > limite) {
                    mudou = true; // ficou fora tempo demais: a classe volta a estar livre
                    continue;
                }
                donosLendarias.put(c, dono);
                nomesDonos.put(c, y.getString(id + ".nome", "?"));
            } catch (IllegalArgumentException ignorado) {
                mudou = true;
            }
        }
        if (mudou) salvarLendarias();
    }

    private void salvarLendarias() {
        var y = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<Classe, UUID> en : donosLendarias.entrySet()) {
            y.set(en.getKey().id() + ".dono", en.getValue().toString());
            y.set(en.getKey().id() + ".nome", nomesDonos.getOrDefault(en.getKey(), "?"));
        }
        try {
            y.save(arquivoLendarias);
        } catch (java.io.IOException ex) {
            plugin.getLogger().warning("Não consegui salvar classes_lendarias.yml: " + ex.getMessage());
        }
    }

    /** O selo de um Local Oculto tenta conceder a classe lendária. @return motivo de não ter dado certo, ou null. */
    public String reivindicarLendaria(Player p, Classe c) {
        UUID dono = donosLendarias.get(c);
        if (dono != null && !dono.equals(p.getUniqueId())) return "A classe " + c.nome() + " já pertence a " + nomeDonoLendaria(c) + ".";
        if (perfil(p).aprendeu(c)) return "Você já é herdeiro desta classe.";
        List<String> falta = faltando(p, c);
        if (!falta.isEmpty()) return "O selo não reconhece o seu caminho. Ele espera: " + String.join(", ", falta) + ".";
        donosLendarias.put(c, p.getUniqueId());
        nomesDonos.put(c, p.getName());
        salvarLendarias();
        conceder(p, c);
        plugin.titulos().registrar(p, "classe_lendaria", 1);
        plugin.getServer().broadcast(Component.text("✪ ", c.cor())
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" despertou a classe lendária ", NamedTextColor.GRAY))
                .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                .append(Component.text("! Só pode haver um.", NamedTextColor.GRAY)));
        return null;
    }

    /** Abre mão da classe lendária: sai do histórico e fica livre para outro. */
    public String renunciarLendaria(Player p, Classe c) {
        if (!p.getUniqueId().equals(donosLendarias.get(c))) return "Essa classe lendária não é sua.";
        donosLendarias.remove(c);
        nomesDonos.remove(c);
        salvarLendarias();
        tirarLendaria(p, c);
        plugin.getServer().broadcast(Component.text("✪ A classe lendária ", c.cor()).append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                .append(Component.text(" está livre de novo.", c.cor())));
        return null;
    }

    public void liberarLendariaAdmin(Classe c) {
        UUID dono = donosLendarias.remove(c);
        nomesDonos.remove(c);
        salvarLendarias();
        Player p = dono == null ? null : Bukkit.getPlayer(dono);
        if (p != null) tirarLendaria(p, c);
    }

    private void tirarLendaria(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        pf.xp.remove(c);
        if (pf.ativa == c) {
            pf.ativa = null;
            pf.salvar(p);
            aplicar(p);
            plugin.tags().atualizarTexto(p);
        } else {
            pf.salvar(p);
        }
    }

    private Settings cfg() { return plugin.settings(); }
    public Habilidades habilidades() { return habilidades; }

    // =====================================================================
    //  Perfil e nível de classe
    // =====================================================================

    public PerfilClasse perfil(Player p) {
        return perfis.computeIfAbsent(p.getUniqueId(), id -> PerfilClasse.ler(p));
    }

    public Classe ativa(Player p) {
        return perfil(p).ativa;
    }

    /** XP para ir do nível {@code n} ao {@code n+1}. Classes avançadas pedem mais. */
    public double xpParaProximo(Classe c, int n) {
        return cfg().claXpBase * Math.pow(n, cfg().claXpExpoente) * (c.avancada() ? cfg().claMultAvancada : 1);
    }

    public int nivel(Player p, Classe c) {
        Double xp = perfil(p).xp.get(c);
        if (xp == null) return 0;
        int n = 1;
        double resto = xp;
        while (n < NIVEL_MAXIMO && resto >= xpParaProximo(c, n)) {
            resto -= xpParaProximo(c, n);
            n++;
        }
        return n;
    }

    /** Fração do caminho até o próximo nível (0 a 1). */
    public double progresso(Player p, Classe c) {
        Double xp = perfil(p).xp.get(c);
        if (xp == null) return 0;
        int n = 1;
        double resto = xp;
        while (n < NIVEL_MAXIMO && resto >= xpParaProximo(c, n)) {
            resto -= xpParaProximo(c, n);
            n++;
        }
        return n >= NIVEL_MAXIMO ? 1 : resto / xpParaProximo(c, n);
    }

    public boolean dominou(Player p, Classe c) {
        return nivel(p, c) >= NIVEL_MAXIMO;
    }

    public int dominadas(Player p) {
        int n = 0;
        for (Classe c : perfil(p).xp.keySet()) if (dominou(p, c)) n++;
        return n;
    }

    /** O nível atual da classe ativa (0 sem classe). */
    public int nivelAtivo(Player p) {
        Classe c = ativa(p);
        return c == null ? 0 : nivel(p, c);
    }

    private double fator(Player p) {
        return Passivo.fator(nivelAtivo(p));
    }

    private Passivo passivo(Player p) {
        Classe c = ativa(p);
        return c == null ? null : c.passivo();
    }

    /** Chamado pelo StatsManager sempre que o jogador ganha XP de atributo. */
    public void aoGanharXp(Player p, Skill s, double qtd) {
        PerfilClasse pf = perfil(p);
        Classe c = pf.ativa;
        if (c == null || qtd <= 0 || !c.afinidades().contains(s) || dominou(p, c)) return;
        int antes = nivel(p, c);
        pf.xp.merge(c, qtd, Double::sum);
        int depois = nivel(p, c);
        if (depois > antes) subiu(p, c, depois);
        pf.salvar(p);
    }

    private void subiu(Player p, Classe c, int n) {
        aplicar(p);
        plugin.tags().atualizarTexto(p);
        if (n >= NIVEL_MAXIMO) {
            p.showTitle(Title.title(Component.text(c.simbolo() + " " + c.nome() + " DOMINADA " + c.simbolo(), c.cor(), TextDecoration.BOLD),
                    Component.text("Ela fica no seu histórico para sempre", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            plugin.titulos().definirMinimo(p, "classes_dominadas", dominadas(p));
            plugin.getServer().broadcast(Component.text("⚜ ", c.cor())
                    .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" dominou a classe ", NamedTextColor.GRAY))
                    .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY)));
        } else {
            p.showTitle(Title.title(Component.empty(), Component.text(c.simbolo() + " " + c.nome() + " nível " + n, c.cor()),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        }
    }

    // =====================================================================
    //  Requisitos, caminho e tarefas
    // =====================================================================

    public int nivelTotalNecessario(Classe c) {
        return c.avancada() ? 0 : (int) Math.ceil(cfg().claNivelTotalBasica * cfg().nivelMaximo * Skill.values().length);
    }

    public int atributoNecessario(Classe c) {
        return (int) Math.ceil((c.avancada() ? cfg().claAtributoAvancada : cfg().claAtributoBasica) * cfg().nivelMaximo);
    }

    /** O que falta para poder seguir o caminho dessa classe (lista vazia = pode). */
    public List<String> faltando(Player p, Classe c) {
        List<String> l = new ArrayList<>();
        int total = nivelTotalNecessario(c);
        if (total > 0 && plugin.stats().nivelTotal(p) < total) l.add("Nível total " + total);
        int atr = atributoNecessario(c);
        if (plugin.stats().getNivel(p, c.atributo()) < atr) l.add(c.atributo().nome() + " nível " + atr);
        for (Classe r : c.requisitos()) if (!dominou(p, r)) l.add(r.nome() + " dominada");
        return l;
    }

    /** Quanto já fez da tarefa desde que aceitou o caminho. */
    public long progresso(Player p, Tarefa t) {
        PerfilClasse pf = perfil(p);
        return Math.max(0, valor(p, t.tipo()) - pf.base.getOrDefault(t.tipo(), 0L));
    }

    private long valor(Player p, Tarefa.Tipo tipo) {
        if (tipo == Tarefa.Tipo.CORRER) return p.getStatistic(Statistic.SPRINT_ONE_CM) / 100;
        return plugin.titulos().contador(p, tipo.contador());
    }

    public boolean tarefasFeitas(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        if (pf.alvo != c) return false;
        for (Tarefa t : c.tarefas()) if (progresso(p, t) < t.qtd()) return false;
        return true;
    }

    /** Começa a seguir o caminho dessa classe: as tarefas contam a partir de agora. */
    public String aceitarCaminho(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        if (c.tier() == Classe.Tier.LENDARIA) return "Classes lendárias só vêm do selo do Local Oculto delas.";
        if (pf.aprendeu(c)) return "Você já aprendeu essa classe.";
        List<String> falta = faltando(p, c);
        if (!falta.isEmpty()) return "Ainda falta: " + String.join(", ", falta) + ".";
        pf.alvo = c;
        pf.base.clear();
        for (Tarefa t : c.tarefas()) pf.base.put(t.tipo(), valor(p, t.tipo()));
        pf.salvar(p);
        return null;
    }

    public void abandonarCaminho(Player p) {
        PerfilClasse pf = perfil(p);
        pf.alvo = null;
        pf.base.clear();
        pf.salvar(p);
    }

    /** Volta para uma classe que já aprendeu (paga esmeraldas). */
    public String trocarPara(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        if (!pf.aprendeu(c)) return "Você ainda não aprendeu essa classe.";
        if (pf.ativa == c) return "Essa já é a sua classe.";
        int custo = cfg().claCustoTroca;
        if (p.getGameMode() != GameMode.CREATIVE && custo > 0) {
            if (!p.getInventory().containsAtLeast(new ItemStack(Material.EMERALD), custo)) return "Trocar de classe custa " + custo + " esmeraldas.";
            p.getInventory().removeItem(new ItemStack(Material.EMERALD, custo));
        }
        ativar(p, c);
        p.sendMessage(Component.text(c.simbolo() + " Agora você é " + c.nome() + ".", c.cor()));
        return null;
    }

    /** Passou na prova: aprende a classe e ela vira a ativa. */
    public void conceder(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        pf.xp.putIfAbsent(c, 0.0);
        if (pf.alvo == c) {
            pf.alvo = null;
            pf.base.clear();
        }
        ativar(p, c);
        plugin.titulos().registrar(p, "classes", 1);
        p.showTitle(Title.title(Component.text(c.simbolo() + " " + c.nome() + " " + c.simbolo(), c.cor(), TextDecoration.BOLD),
                Component.text("Nova classe! Agache + F para usar as habilidades", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
        plugin.getServer().broadcast(Component.text("⚜ ", c.cor())
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" passou na prova e agora é ", NamedTextColor.GRAY))
                .append(Component.text(c.nome(), c.cor(), TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.GRAY)));
    }

    private void ativar(Player p, Classe c) {
        PerfilClasse pf = perfil(p);
        pf.ativa = c;
        pf.selecionada = 0;
        pf.salvar(p);
        recargas.remove(p.getUniqueId());
        aplicar(p);
        plugin.arcano().aplicarBonus(p);
        plugin.tags().atualizarTexto(p);
    }

    /** Admin: define a classe e o nível direto. */
    public void definir(Player p, Classe c, int nivel) {
        PerfilClasse pf = perfil(p);
        double xp = 0;
        for (int n = 1; n < Math.max(1, Math.min(NIVEL_MAXIMO, nivel)); n++) xp += xpParaProximo(c, n);
        pf.xp.put(c, xp + 0.01);
        if (c.tier() == Classe.Tier.LENDARIA) {
            UUID antigo = donosLendarias.put(c, p.getUniqueId());
            nomesDonos.put(c, p.getName());
            salvarLendarias();
            Player outro = antigo == null || antigo.equals(p.getUniqueId()) ? null : Bukkit.getPlayer(antigo);
            if (outro != null) tirarLendaria(outro, c);
        }
        ativar(p, c);
        plugin.titulos().definirMinimo(p, "classes_dominadas", dominadas(p));
    }

    // =====================================================================
    //  Passivos
    // =====================================================================

    /** Vida, armadura e velocidade da classe ativa. */
    public void aplicar(Player p) {
        Passivo ps = passivo(p);
        double f = fator(p);
        definir(p, Attribute.MAX_HEALTH, M_VIDA, ps == null ? 0 : ps.vida * f, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.ARMOR, M_ARMADURA, ps == null ? 0 : ps.armadura * f, AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MOVEMENT_SPEED, M_VELOCIDADE, ps == null ? 0 : ps.velocidade * f, AttributeModifier.Operation.ADD_SCALAR);
    }

    private static void definir(Player p, Attribute a, NamespacedKey k, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(k);
        if (valor != 0) inst.addModifier(new AttributeModifier(k, valor, op, EquipmentSlotGroup.ANY));
        if (a == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    public double bonusMana(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.mana * fator(p);
    }

    public double bonusRegenMana(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.regen * fator(p);
    }

    /** Multiplicador do custo de mana das magias (1 = normal). */
    public double custoMagia(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 1 : 1 - ps.custoMagia * fator(p);
    }

    /** Chance de a forja sair uma raridade acima. */
    public double chanceRaridadeExtra(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.raridade * fator(p);
    }

    public double chanceRefinoExtra(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.refino * fator(p);
    }

    public int companheirosExtra(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.companheiros;
    }

    public int componentesExtra(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.componentes;
    }

    public double danoCompanheiro(Player p) {
        Passivo ps = passivo(p);
        return ps == null ? 0 : ps.danoCompanheiro * fator(p);
    }

    /** Dano: passivos da classe e efeitos das habilidades (no atacante e na vítima). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void aoDano(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        Player a = null;
        boolean flecha = false;
        if (d instanceof Player pl) {
            if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) a = pl;
        } else if (d instanceof Projectile pr && pr.getShooter() instanceof Player pl) {
            a = pl;
            flecha = true;
        } else if (Companheiros.eh(d)) {
            Player dono = Bukkit.getPlayer(Companheiros.dono(d));
            if (dono != null) {
                double mult = 1 + danoCompanheiro(dono);
                if (habilidades.marcado(dono, e.getEntity())) mult += 0.3;
                e.setDamage(e.getDamage() * mult);
            }
        }
        if (a != null && e.getEntity() instanceof LivingEntity alvo) atacou(e, a, alvo, flecha);
        if (e.getEntity() instanceof Player v) apanhou(e, v);
    }

    private void atacou(EntityDamageByEntityEvent e, Player a, LivingEntity alvo, boolean flecha) {
        Classe c = ativa(a);
        double mult = 1, extra = 0;
        if (c != null) {
            double f = fator(a);
            mult += (flecha ? c.passivo().danoFlecha : c.passivo().danoCorpo) * f;
            if (!flecha) {
                AttributeInstance max = a.getAttribute(Attribute.MAX_HEALTH);
                double vida = a.getHealth() / (max == null ? 20 : max.getValue());
                if (c == Classe.BERSERKER || c == Classe.SENHOR_DA_GUERRA) mult += vida < 0.3 ? 0.4 * f : vida < 0.5 ? 0.2 * f : 0;
                if (c == Classe.CAVALEIRO && a.getVehicle() != null) mult += 0.25;
                if ((c == Classe.ASSASSINO || c == Classe.LAMINA_FANTASMA) && pelasCostas(a, alvo)) {
                    mult += c == Classe.LAMINA_FANTASMA ? 1.0 : 0.5;
                    alvo.getWorld().spawnParticle(Particle.CRIT, alvo.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.1);
                }
                if (c == Classe.FEITICEIRO) plugin.arcano().darMana(a, 2);
            } else if ((c == Classe.ATIRADOR && ThreadLocalRandom.current().nextDouble() < 0.15)
                    || (c == Classe.ARQUEIRO_CELESTIAL && ThreadLocalRandom.current().nextDouble() < 0.25)) {
                mult += 1;
                alvo.getWorld().spawnParticle(Particle.CRIT, alvo.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.2);
            }
        }
        // Efeitos das habilidades.
        Habilidades h = habilidades;
        if (!flecha) {
            if (h.consumirGolpeLetal(a)) {
                AttributeInstance max = alvo.getAttribute(Attribute.MAX_HEALTH);
                boolean fraco = max != null && alvo.getHealth() < max.getValue() * 0.3;
                mult *= fraco ? 4 : 2.5;
                alvo.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, alvo.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.2);
                a.playSound(a.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.6f);
            }
            if (h.consumirLamina(a)) {
                extra += 5 * fator(a);
                plugin.arcano().darMana(a, 5);
                alvo.getWorld().spawnParticle(Particle.WITCH, alvo.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.05);
            }
            if (h.afiado(a)) mult *= 1.25;
            if (h.consumirVeu(a)) {
                mult *= 3;
                a.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                alvo.getWorld().spawnParticle(Particle.SQUID_INK, alvo.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.05);
            }
        } else if (h.aguia(a)) {
            mult *= 1.6;
        }
        if (h.marcado(a, alvo)) mult *= 1.3;
        e.setDamage(e.getDamage() * mult + extra);
    }

    /** O atacante está atrás do alvo? */
    private static boolean pelasCostas(Player a, LivingEntity alvo) {
        Vector olhar = alvo.getLocation().getDirection().setY(0);
        Vector paraAtacante = a.getLocation().toVector().subtract(alvo.getLocation().toVector()).setY(0);
        if (olhar.lengthSquared() < 1e-4 || paraAtacante.lengthSquared() < 1e-4) return false;
        return olhar.normalize().dot(paraAtacante.normalize()) < -0.3;
    }

    private void apanhou(EntityDamageByEntityEvent e, Player v) {
        if (habilidades.emFuria(v)) e.setDamage(e.getDamage() * 1.2);
        Passivo ps = passivo(v);
        if (ps != null && ps.esquiva > 0 && ThreadLocalRandom.current().nextDouble() < ps.esquiva * fator(v)) {
            e.setCancelled(true);
            v.getWorld().spawnParticle(Particle.CLOUD, v.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.02);
            v.playSound(v.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 1.6f);
            v.sendActionBar(Component.text("✧ Esquivou!", NamedTextColor.AQUA));
            return;
        }
        if (habilidades.escudoElemental(v) && e.getDamager() instanceof LivingEntity atacante) {
            atacante.setFireTicks(Math.max(atacante.getFireTicks(), 60));
            atacante.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
        }
    }

    /** Contadores das tarefas: abates corpo a corpo e com flecha. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || !(morto instanceof Enemy)) return;
        if (morto.getLastDamageCause() instanceof EntityDamageByEntityEvent ev) {
            if (ev.getDamager() instanceof Projectile) plugin.titulos().registrar(p, "abates_flecha", 1);
            else if (ev.getDamager() == p) plugin.titulos().registrar(p, "abates_corpo", 1);
        }
    }

    // =====================================================================
    //  Habilidades: agachar + F usa, agachar + clique esquerdo no ar troca
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoApertarF(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking() || ativa(p) == null) return;
        e.setCancelled(true);
        usar(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarNoAr(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() != Action.LEFT_CLICK_AIR || e.getHand() != EquipmentSlot.HAND || !p.isSneaking()) return;
        Classe c = ativa(p);
        if (c == null || c.habilidades().size() < 2 || plugin.arcano().ehGrimorio(e.getItem())) return;
        PerfilClasse pf = perfil(p);
        pf.selecionada = (pf.selecionada + 1) % c.habilidades().size();
        pf.salvar(p);
        Habilidade h = c.habilidades().get(pf.selecionada);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
        p.sendActionBar(Component.text(c.simbolo() + " Habilidade: ", c.cor()).append(Component.text(h.nome(), NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(estado(p, h)));
    }

    private Component estado(Player p, Habilidade h) {
        long falta = faltaRecarga(p, h);
        return falta <= 0 ? Component.text("  (pronta)", NamedTextColor.GREEN)
                : Component.text("  (" + (falta / 1000 + 1) + " s)", NamedTextColor.RED);
    }

    private long faltaRecarga(Player p, Habilidade h) {
        Long ate = recargas.getOrDefault(p.getUniqueId(), Map.of()).get(h);
        return ate == null ? 0 : ate - System.currentTimeMillis();
    }

    public Habilidade selecionada(Player p) {
        Classe c = ativa(p);
        if (c == null || c.habilidades().isEmpty()) return null;
        PerfilClasse pf = perfil(p);
        return c.habilidades().get(Math.floorMod(pf.selecionada, c.habilidades().size()));
    }

    private void usar(Player p) {
        Classe c = ativa(p);
        Habilidade h = selecionada(p);
        if (c == null || h == null) return;
        long falta = faltaRecarga(p, h);
        if (falta > 0) {
            p.sendActionBar(Component.text(h.nome() + " recarregando: " + (falta / 1000 + 1) + " s", NamedTextColor.RED));
            return;
        }
        Perfil arc = null;
        if (h.mana() > 0) {
            arc = plugin.arcano().perfil(p);
            if (arc.mana() < h.mana()) {
                p.sendActionBar(Component.text("Mana insuficiente (" + (int) arc.mana() + "/" + (int) h.mana() + ")", NamedTextColor.RED));
                return;
            }
        }
        double forca = 0.6 + 0.4 * nivel(p, c) / (double) NIVEL_MAXIMO;
        if (!habilidades.usar(p, h, forca)) return;
        if (arc != null) plugin.arcano().gastarMana(p, arc, h.mana());
        recargas.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(h, System.currentTimeMillis() + h.recarga() * 1000L);
        p.sendActionBar(Component.text(c.simbolo() + " " + h.nome() + "!", c.cor(), TextDecoration.BOLD));
    }

    // =====================================================================
    //  Entrar e sair
    // =====================================================================

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        perfis.remove(p.getUniqueId());
        // Classe lendária que passou para outra pessoa (ou foi liberada) sai do histórico.
        for (Classe c : new ArrayList<>(perfil(p).xp.keySet())) {
            if (c.tier() == Classe.Tier.LENDARIA && !p.getUniqueId().equals(donosLendarias.get(c))) {
                tirarLendaria(p, c);
                p.sendMessage(Component.text("✪ Você não é mais o herdeiro de " + c.nome() + ".", NamedTextColor.GRAY));
            }
        }
        aplicar(p);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        perfis.remove(e.getPlayer().getUniqueId());
        recargas.remove(e.getPlayer().getUniqueId());
        habilidades.esquecer(e.getPlayer().getUniqueId());
    }

    /** Linha da tag: símbolo e nome da classe com o nível (ou null). */
    public Component linhaDaTag(Player p) {
        Classe c = ativa(p);
        if (c == null) return null;
        return Component.text(c.simbolo() + " " + c.nome() + " " + nivel(p, c), c.cor());
    }
}
