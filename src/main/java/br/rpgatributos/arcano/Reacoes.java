package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reações elementais: quem é atingido por um elemento fica "marcado" por alguns segundos.
 * Um segundo elemento diferente no mesmo alvo causa uma reação (Eletrocutado, Congelado...).
 * Vale entre jogadores diferentes: ótimo para combos em party.
 */
public final class Reacoes {

    private static final long MARCA_MS = 6000;
    private static final long INTERVALO_MS = 1000;

    public enum Reacao {
        ELETROCUTADO("Eletrocutado", 0x7FD7FF, Essencia.AGUA, Essencia.ENERGIA, "o choque salta para até 4 inimigos perto"),
        CONGELADO("Congelado", 0x9FE7FF, Essencia.AGUA, Essencia.GELO, "o alvo fica quase parado por 3s"),
        DERRETER("Derreter", 0xFF9A3C, Essencia.FOGO, Essencia.GELO, "um golpe de vapor com muito dano"),
        INCENDIO("Incêndio", 0xFF6A2B, Essencia.FOGO, Essencia.VENTO, "o fogo se espalha para quem está perto"),
        QUEIMADA("Queimada", 0xD35400, Essencia.FOGO, Essencia.NATUREZA, "chamas fortes e longas, que pulam para 2 inimigos"),
        LAMA("Lama", 0x8B5A2B, Essencia.TERRA, Essencia.AGUA, "lentidão forte por 5s"),
        CORRUPCAO("Corrupção", 0x6B2E8A, Essencia.SOMBRA, Essencia.VIDA, "dano, definhamento, e você se cura"),
        TEMPESTADE("Tempestade", 0xFFE14D, Essencia.VENTO, Essencia.ENERGIA, "um raio cai no alvo e o joga para cima"),
        COLAPSO("Colapso", 0x3B2E7E, Essencia.VAZIO, Essencia.SOMBRA, "puxa todos os inimigos perto para o alvo"),
        ESTILHACAR("Estilhaçar", 0xBFD9E8, Essencia.GELO, Essencia.TERRA, "quebra o gelo: muito dano");

        private final String nome;
        private final TextColor cor;
        private final Essencia a, b;
        private final String descricao;

        Reacao(String nome, int cor, Essencia a, Essencia b, String descricao) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.a = a;
            this.b = b;
            this.descricao = descricao;
        }

        public String nome() { return nome; }
        public TextColor cor() { return cor; }
        public Essencia a() { return a; }
        public Essencia b() { return b; }
        public String descricao() { return descricao; }

        public boolean usa(Essencia e) { return a == e || b == e; }

        static Reacao entre(Essencia x, Essencia y) {
            for (Reacao r : values()) if ((r.a == x && r.b == y) || (r.a == y && r.b == x)) return r;
            return null;
        }
    }

    private final RPGAtributos plugin;
    private final Conjuracao conj;
    /** Entidade → elementos marcados → até quando. */
    private final Map<UUID, Map<Essencia, Long>> marcas = new HashMap<>();
    private final Map<UUID, Long> ultimaReacao = new HashMap<>();

    Reacoes(RPGAtributos plugin, Conjuracao conj) {
        this.plugin = plugin;
        this.conj = conj;
    }

    /**
     * Chamado quando uma magia atinge um alvo. Primeiro confere se algum elemento da magia
     * reage com uma marca que já estava no alvo; depois marca o alvo com o resto.
     */
    void aoAtingir(Player p, LivingEntity alvo, Set<Essencia> es, double pot) {
        long agora = System.currentTimeMillis();
        Map<Essencia, Long> m = marcas.computeIfAbsent(alvo.getUniqueId(), k -> new EnumMap<>(Essencia.class));
        m.values().removeIf(t -> t < agora);
        Long ultima = ultimaReacao.get(alvo.getUniqueId());
        boolean pode = ultima == null || agora - ultima >= INTERVALO_MS;
        Essencia usada = null;
        if (pode) {
            busca:
            for (Essencia nova : es) {
                for (Essencia velha : List.copyOf(m.keySet())) {
                    if (velha == nova) continue;
                    Reacao r = Reacao.entre(velha, nova);
                    if (r == null) continue;
                    m.remove(velha);
                    usada = nova;
                    ultimaReacao.put(alvo.getUniqueId(), agora);
                    reagir(p, alvo, r, pot);
                    break busca;
                }
            }
        }
        for (Essencia e : es) if (e != usada) m.put(e, agora + MARCA_MS);
        if (m.isEmpty()) marcas.remove(alvo.getUniqueId());
    }

    private void reagir(Player p, LivingEntity alvo, Reacao r, double pot) {
        Arcano arc = plugin.arcano();
        Perfil pf = arc.perfil(p);
        if (arc.nivelMaestria(pf, r.a) >= Arcano.MAESTRIA_MAXIMA || arc.nivelMaestria(pf, r.b) >= Arcano.MAESTRIA_MAXIMA) pot *= 1.5;
        arc.darMaestria(p, pf, r.a, 3);
        arc.darMaestria(p, pf, r.b, 3);
        plugin.titulos().registrar(p, "reacoes", 1);

        World w = alvo.getWorld();
        Location c = Conjuracao.meio(alvo);
        p.sendActionBar(Component.text("✦ " + r.nome + "!", r.cor, TextDecoration.BOLD));
        w.spawnParticle(Particle.DUST, c, 30, 0.5, 0.6, 0.5, 0, new Particle.DustOptions(org.bukkit.Color.fromRGB(r.cor.value()), 1.8f));
        switch (r) {
            case ELETROCUTADO -> {
                conj.ferir(p, alvo, 2.5 * pot);
                int saltos = 0;
                for (LivingEntity e : Conjuracao.perto(alvo.getLocation(), 5)) {
                    if (e == alvo || !Conjuracao.inimigo(p, e) || saltos >= 4) continue;
                    Conjuracao.feixeParticula(c, Conjuracao.meio(e), Particle.ELECTRIC_SPARK);
                    conj.ferir(p, e, 2 * pot);
                    saltos++;
                }
                w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
            }
            case CONGELADO -> {
                int dur = alvo instanceof Player ? 40 : 60;
                Conjuracao.efeito(alvo, PotionEffectType.SLOWNESS, dur, 6);
                alvo.setFreezeTicks(alvo.getMaxFreezeTicks() + dur);
                w.spawnParticle(Particle.BLOCK, c, 40, 0.4, 0.6, 0.4, Material.ICE.createBlockData());
                w.playSound(c, Sound.BLOCK_GLASS_PLACE, 1f, 0.6f);
            }
            case DERRETER -> {
                conj.ferir(p, alvo, 4 * pot);
                w.spawnParticle(Particle.CLOUD, c, 30, 0.4, 0.5, 0.4, 0.05);
                w.playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.8f);
            }
            case INCENDIO -> {
                for (LivingEntity e : Conjuracao.perto(alvo.getLocation(), 4)) {
                    if (!Conjuracao.inimigo(p, e)) continue;
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                    conj.ferir(p, e, 1.5 * pot);
                }
                Conjuracao.anelParticula(alvo.getLocation().add(0, 0.2, 0), 4, Particle.FLAME);
                w.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
            }
            case QUEIMADA -> {
                alvo.setFireTicks(Math.max(alvo.getFireTicks(), 160));
                conj.ferir(p, alvo, 2 * pot);
                int pulos = 0;
                for (LivingEntity e : Conjuracao.perto(alvo.getLocation(), 5)) {
                    if (e == alvo || !Conjuracao.inimigo(p, e) || pulos >= 2) continue;
                    e.setFireTicks(Math.max(e.getFireTicks(), 100));
                    Conjuracao.feixeParticula(c, Conjuracao.meio(e), Particle.FLAME);
                    pulos++;
                }
                w.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.6f);
            }
            case LAMA -> {
                Conjuracao.efeito(alvo, PotionEffectType.SLOWNESS, 100, 3);
                conj.ferir(p, alvo, pot);
                w.spawnParticle(Particle.BLOCK, alvo.getLocation(), 40, 0.6, 0.1, 0.6, Material.MUD.createBlockData());
                w.playSound(c, Sound.BLOCK_MUD_STEP, 1f, 0.7f);
            }
            case CORRUPCAO -> {
                conj.ferir(p, alvo, 3 * pot);
                Conjuracao.efeito(alvo, PotionEffectType.WITHER, 80, 1);
                Conjuracao.curar(p, 2 * pot);
                w.spawnParticle(Particle.SCULK_SOUL, c, 15, 0.4, 0.5, 0.4, 0.03);
                w.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.2f);
            }
            case TEMPESTADE -> {
                w.strikeLightningEffect(alvo.getLocation());
                conj.ferir(p, alvo, 3.5 * pot);
                alvo.setVelocity(alvo.getVelocity().setY(0.8));
            }
            case COLAPSO -> {
                for (LivingEntity e : Conjuracao.perto(alvo.getLocation(), 6)) {
                    if (e == alvo || !Conjuracao.inimigo(p, e)) continue;
                    Vector v = alvo.getLocation().toVector().subtract(e.getLocation().toVector());
                    if (v.lengthSquared() > 0.5) e.setVelocity(v.normalize().multiply(1.1).setY(0.25));
                    conj.ferir(p, e, 2 * pot);
                }
                conj.ferir(p, alvo, 2 * pot);
                Conjuracao.efeito(alvo, PotionEffectType.DARKNESS, 60, 0);
                w.spawnParticle(Particle.REVERSE_PORTAL, c, 60, 1.5, 1, 1.5, 0.1);
                w.playSound(c, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 1.8f);
            }
            case ESTILHACAR -> {
                boolean congelado = alvo.getFreezeTicks() > 0;
                conj.ferir(p, alvo, (congelado ? 6.5 : 4.5) * pot);
                w.spawnParticle(Particle.BLOCK, c, 50, 0.4, 0.6, 0.4, Material.PACKED_ICE.createBlockData());
                w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
            }
        }
    }

    /** Partícula de cada marca sobre a cabeça do alvo (a cada meio segundo) e limpeza. */
    void tick() {
        long agora = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Map<Essencia, Long>>> it = marcas.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<Essencia, Long>> en = it.next();
            en.getValue().values().removeIf(t -> t < agora);
            Entity e = Bukkit.getEntity(en.getKey());
            if (en.getValue().isEmpty() || !(e instanceof LivingEntity le) || !le.isValid()) {
                it.remove();
                continue;
            }
            Location topo = le.getLocation().add(0, le.getHeight() + 0.4, 0);
            List<Essencia> lista = new ArrayList<>(en.getValue().keySet());
            for (int i = 0; i < lista.size(); i++) {
                double a = (agora / 300.0) + i * 2 * Math.PI / lista.size();
                Location l = topo.clone().add(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35);
                le.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, lista.get(i).poeira(0.9f));
            }
        }
        ultimaReacao.values().removeIf(t -> agora - t > 10_000);
    }
}
