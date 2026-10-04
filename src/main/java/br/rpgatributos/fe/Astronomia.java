package br.rpgatributos.fe;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.mundo.Ceu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Astronomia: à noite, olhe para o céu aberto pela luneta por 5 segundos para observar a
 * constelação daquela noite (muda a cada noite, 12 ao todo). Ela fica no seu diário
 * (/estrelas) e dá uma bênção até o amanhecer. Uma observação por noite.
 */
public final class Astronomia implements CommandExecutor {

    public static final TextColor COR = TextColor.color(0x9FA8DA);

    public enum Constelacao {
        FERREIRO("O Ferreiro", "+15% de XP de Ferraria e Mineração"),
        PESCADOR("O Pescador", "Sorte II (pesca e baús melhores)"),
        CORUJA("A Coruja", "Visão noturna"),
        LOBO("O Lobo", "Força I"),
        VIAJANTE("O Viajante", "Velocidade I e +15% de XP de Corrida"),
        FENIX("A Fênix", "Regeneração I"),
        SABIO("O Sábio", "+10% de XP em todos os atributos"),
        ANCORA("A Âncora", "Respiração aquática e Graça do golfinho"),
        MAGO("O Mago", "+2 de mana por segundo"),
        COROA("A Coroa", "Devoção em dobro"),
        SERPENTE("A Serpente", "Pressa I"),
        GIGANTE("O Gigante", "Resistência I");

        private final String nome, bencao;

        Constelacao(String nome, String bencao) {
            this.nome = nome;
            this.bencao = bencao;
        }

        public String nome() { return nome; }
        public String bencao() { return bencao; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    private record Bencao(Constelacao c, long ate) {}

    private final RPGAtributos plugin;
    private final NamespacedKey kVistas, kNoite;
    private final Map<UUID, Integer> observando = new HashMap<>();
    private final Map<UUID, Bencao> bencaos = new HashMap<>();

    public Astronomia(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kVistas = new NamespacedKey(plugin, "constelacoes");
        this.kNoite = new NamespacedKey(plugin, "constelacao_noite");
    }

    /** A constelação que aparece nesta noite do mundo. */
    public static Constelacao daNoite(World w) {
        long noite = w.getFullTime() / 24000;
        return Constelacao.values()[(int) Math.floorMod(noite, (long) Constelacao.values().length)];
    }

    public Set<Constelacao> vistas(Player p) {
        Set<Constelacao> s = EnumSet.noneOf(Constelacao.class);
        String txt = p.getPersistentDataContainer().getOrDefault(kVistas, PersistentDataType.STRING, "");
        for (String id : txt.split(",")) {
            for (Constelacao c : Constelacao.values()) if (c.id().equals(id)) s.add(c);
        }
        return s;
    }

    private Constelacao ativa(Player p) {
        Bencao b = bencaos.get(p.getUniqueId());
        return b != null && b.ate > System.currentTimeMillis() ? b.c : null;
    }

    public double multiplicadorXp(Player p, Skill s) {
        Constelacao c = ativa(p);
        if (c == null) return 1;
        return switch (c) {
            case FERREIRO -> s == Skill.FERRARIA || s == Skill.MINERACAO ? 1.15 : 1;
            case VIAJANTE -> s == Skill.CORRIDA ? 1.15 : 1;
            case SABIO -> 1.10;
            default -> 1;
        };
    }

    public double multiplicadorDevocao(Player p) {
        return ativa(p) == Constelacao.COROA ? 2 : 1;
    }

    public double bonusRegen(Player p) {
        return ativa(p) == Constelacao.MAGO ? 2 : 0;
    }

    /** A cada meio segundo: quem está olhando o céu pela luneta. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            boolean luneta = p.hasActiveItem() && p.getActiveItem().getType() == Material.SPYGLASS;
            World w = p.getWorld();
            boolean ceuAberto = w.getEnvironment() == World.Environment.NORMAL && Ceu.noite(w)
                    && p.getLocation().getPitch() < -50 && w.getHighestBlockYAt(p.getLocation()) <= p.getEyeLocation().getBlockY();
            if (!luneta || !ceuAberto) {
                observando.remove(id);
                continue;
            }
            int n = observando.merge(id, 1, Integer::sum);
            if (n % 2 == 0 && n < 10) p.sendActionBar(Component.text("✦ Observando as estrelas" + ".".repeat(n / 2), COR));
            if (n >= 10) {
                observando.remove(id);
                observar(p);
            }
        }
    }

    private void observar(Player p) {
        World w = p.getWorld();
        long noite = w.getFullTime() / 24000;
        if (p.getPersistentDataContainer().getOrDefault(kNoite, PersistentDataType.LONG, -1L) == noite) {
            p.sendActionBar(Component.text("✦ Você já observou o céu desta noite.", NamedTextColor.GRAY));
            return;
        }
        p.getPersistentDataContainer().set(kNoite, PersistentDataType.LONG, noite);
        Constelacao c = daNoite(w);
        Set<Constelacao> vistas = vistas(p);
        boolean nova = vistas.add(c);
        if (nova) {
            StringBuilder sb = new StringBuilder();
            for (Constelacao x : vistas) sb.append(sb.isEmpty() ? "" : ",").append(x.id());
            p.getPersistentDataContainer().set(kVistas, PersistentDataType.STRING, sb.toString());
            plugin.titulos().definirMinimo(p, "constelacoes", vistas.size());
        }
        long ticks = 24000 - Math.floorMod(w.getTime(), 24000L);
        bencaos.put(p.getUniqueId(), new Bencao(c, System.currentTimeMillis() + ticks * 50));
        int dur = (int) Math.min(Integer.MAX_VALUE, ticks);
        switch (c) {
            case PESCADOR -> efeito(p, PotionEffectType.LUCK, dur, 1);
            case CORUJA -> efeito(p, PotionEffectType.NIGHT_VISION, dur, 0);
            case LOBO -> efeito(p, PotionEffectType.STRENGTH, dur, 0);
            case VIAJANTE -> efeito(p, PotionEffectType.SPEED, dur, 0);
            case FENIX -> efeito(p, PotionEffectType.REGENERATION, dur, 0);
            case ANCORA -> {
                efeito(p, PotionEffectType.WATER_BREATHING, dur, 0);
                efeito(p, PotionEffectType.DOLPHINS_GRACE, dur, 0);
            }
            case SERPENTE -> efeito(p, PotionEffectType.HASTE, dur, 0);
            case GIGANTE -> efeito(p, PotionEffectType.RESISTANCE, dur, 0);
            default -> { }
        }
        p.showTitle(Title.title(Component.text("✦ " + c.nome() + " ✦", COR, TextDecoration.BOLD),
                Component.text((nova ? "Nova constelação! " : "") + c.bencao() + " até o amanhecer", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3000), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getEyeLocation().add(0, 3, 0), 40, 2, 1, 2, 0.02);
        if (nova) p.sendMessage(Component.text("✦ " + c.nome() + " entrou no seu diário (" + vistas.size() + "/12). /estrelas", COR));
    }

    private static void efeito(Player p, PotionEffectType t, int ticks, int nivel) {
        p.addPotionEffect(new PotionEffect(t, ticks, nivel, true, false, true));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        Set<Constelacao> vistas = vistas(p);
        Constelacao hoje = daNoite(p.getWorld());
        p.sendMessage(Component.text("✦ Diário das Estrelas (" + vistas.size() + "/12)", COR, TextDecoration.BOLD));
        for (Constelacao c : Constelacao.values()) {
            boolean viu = vistas.contains(c);
            p.sendMessage(Component.text(viu ? " ✦ " + c.nome() + " — " + c.bencao() : " ✧ ???", viu ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY)
                    .append(c == hoje ? Component.text("  ← no céu esta noite", NamedTextColor.YELLOW) : Component.empty()));
        }
        Constelacao a = ativa(p);
        if (a != null) p.sendMessage(Component.text("Bênção ativa: " + a.nome() + " (" + a.bencao() + ")", NamedTextColor.GREEN));
        p.sendMessage(Component.text("À noite, olhe para o céu aberto pela luneta por 5 s.", NamedTextColor.GRAY));
        return true;
    }
}
