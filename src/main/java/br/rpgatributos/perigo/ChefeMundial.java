package br.rpgatributos.perigo;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.arcano.ItensMagicos;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chefe Mundial: algumas vezes por semana um dos chefes do Altar aparece muito mais forte
 * num lugar do mundo. O servidor é avisado com as coordenadas e 10 minutos de antecedência;
 * ele espera até alguém chegar. O saque é para todos que lutaram.
 */
public final class ChefeMundial implements Listener {

    private static final int CONTAGEM_SEGUNDOS = 600;
    private static final int ESPERA_MAXIMA_SEGUNDOS = 1800;
    private static final NamespacedKey CHAVE = new NamespacedKey("rpgatributos", "chefe_mundial");

    private enum Estado { PARADO, CONTAGEM, ESPERANDO, LUTANDO }

    private final RPGAtributos plugin;
    private final File arquivo;
    private Estado estado = Estado.PARADO;
    private long proximo;
    private Location local;
    private Chefe chefe;
    private int segundos;
    private UUID entidade;
    private final BossBar barra = BossBar.bossBar(Component.empty(), 1, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);

    public ChefeMundial(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "mundo.yml");
        YamlConfiguration y = arquivo.exists() ? YamlConfiguration.loadConfiguration(arquivo) : new YamlConfiguration();
        proximo = y.getLong("proximo-chefe-mundial", 0);
        if (proximo <= 0) agendar();
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private void agendar() {
        int porSemana = plugin.settings().perChefePorSemana;
        if (porSemana <= 0) { proximo = Long.MAX_VALUE; return; }
        long intervalo = 7L * 24 * 3_600_000 / porSemana;
        proximo = System.currentTimeMillis() + (long) (intervalo * rnd().nextDouble(0.7, 1.3));
        YamlConfiguration y = arquivo.exists() ? YamlConfiguration.loadConfiguration(arquivo) : new YamlConfiguration();
        y.set("proximo-chefe-mundial", proximo);
        try { y.save(arquivo); } catch (IOException ex) { plugin.getLogger().warning("Não foi possível salvar mundo.yml"); }
    }

    /** Roda 1x por segundo. */
    public void tick() {
        switch (estado) {
            case PARADO -> {
                if (System.currentTimeMillis() >= proximo && Bukkit.getOnlinePlayers().size() >= plugin.settings().perChefeMinJogadores) {
                    comecar(CONTAGEM_SEGUNDOS);
                }
            }
            case CONTAGEM -> {
                segundos--;
                barra.name(Component.text("☠ Chefe Mundial: " + chefe.nome() + " chega em " + segundos / 60 + ":"
                        + String.format("%02d", segundos % 60) + "  ·  " + local.getBlockX() + " " + local.getBlockZ(), chefe.cor()));
                barra.progress(Math.max(0, Math.min(1, segundos / (float) CONTAGEM_SEGUNDOS)));
                for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(barra);
                if (segundos == 60) Bukkit.broadcast(Component.text("☠ O Chefe Mundial chega em 1 minuto em " + coords(), chefe.cor()));
                if (segundos <= 0) {
                    estado = Estado.ESPERANDO;
                    segundos = 0;
                }
            }
            case ESPERANDO -> {
                segundos++;
                barra.name(Component.text("☠ " + chefe.nome() + " espera desafiantes em " + coords(), chefe.cor()));
                barra.progress(1);
                for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(barra);
                if (local.isChunkLoaded()) {
                    local.getWorld().spawnParticle(Particle.END_ROD, local.clone().add(0.5, 3, 0.5), 30, 0.3, 6, 0.3, 0.01);
                    for (Player p : local.getWorld().getPlayers()) {
                        if (p.getGameMode() == GameMode.SURVIVAL && p.getLocation().distanceSquared(local) < 40 * 40) {
                            invocar();
                            return;
                        }
                    }
                }
                if (segundos > ESPERA_MAXIMA_SEGUNDOS) {
                    Bukkit.broadcast(Component.text("☠ Ninguém desafiou " + chefe.nome() + ". Ele voltou para as sombras.", NamedTextColor.GRAY));
                    terminar();
                }
            }
            case LUTANDO -> {
                Entity e = entidade == null ? null : Bukkit.getEntity(entidade);
                if (e == null || !e.isValid() || e.isDead()) terminar();
            }
        }
    }

    private String coords() {
        return "X " + local.getBlockX() + ", Z " + local.getBlockZ() + (local.getWorld().getEnvironment() == World.Environment.NORMAL ? "" : " (" + local.getWorld().getName() + ")");
    }

    /** Começa a contagem (o admin pode adiantar). @return false se já tem um em andamento ou não achou lugar. */
    public boolean comecar(int contagem) {
        if (estado != Estado.PARADO) return false;
        World w = Bukkit.getWorlds().getFirst();
        Location l = escolherLugar(w);
        if (l == null) {
            proximo = System.currentTimeMillis() + 30 * 60_000L;
            return false;
        }
        Chefe[] todos = Chefe.values();
        chefe = todos[rnd().nextInt(todos.length)];
        local = l;
        segundos = contagem;
        estado = Estado.CONTAGEM;
        barra.color(BossBar.Color.PURPLE);
        Bukkit.broadcast(Component.text("☠ ", NamedTextColor.DARK_PURPLE).append(Component.text("CHEFE MUNDIAL", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.GRAY)).append(Component.text(chefe.nome(), chefe.cor(), TextDecoration.BOLD))
                .append(Component.text(" vai aparecer em " + coords() + " daqui a " + Math.max(1, contagem / 60) + " minutos! Juntem-se!", NamedTextColor.GRAY)));
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.6f);
        return true;
    }

    private Location escolherLugar(World w) {
        Location spawn = w.getSpawnLocation();
        for (int i = 0; i < 30; i++) {
            double a = rnd().nextDouble(Math.PI * 2), d = rnd().nextDouble(400, 1500);
            int x = (int) (spawn.getX() + Math.cos(a) * d), z = (int) (spawn.getZ() + Math.sin(a) * d);
            if (plugin.territorios().em(w.getName(), x >> 4, z >> 4) != null) continue;
            String bioma = w.getBiome(x, 64, z).getKey().getKey();
            if (bioma.contains("ocean") || bioma.contains("river")) continue;
            Block chao = w.getHighestBlockAt(x, z);
            if (chao.isLiquid()) continue;
            return chao.getLocation();
        }
        return null;
    }

    private void invocar() {
        int online = Bukkit.getOnlinePlayers().size();
        LivingEntity boss = plugin.chefes().invocar(chefe, local, null, false);
        boss.getPersistentDataContainer().set(CHAVE, PersistentDataType.BYTE, (byte) 1);
        AttributeInstance vida = boss.getAttribute(Attribute.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(Math.min(4000, vida.getBaseValue() * (3 + 0.8 * online)));
            boss.setHealth(vida.getValue());
        }
        AttributeInstance escala = boss.getAttribute(Attribute.SCALE);
        if (escala != null) escala.setBaseValue(escala.getBaseValue() * 1.4);
        AttributeInstance dano = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (dano != null) dano.setBaseValue(dano.getBaseValue() * 1.5);
        plugin.perigo().envelhecer(boss);
        boss.customName(Component.text("☠ Chefe Mundial: " + chefe.nome() + " ☠", chefe.cor(), TextDecoration.BOLD));
        entidade = boss.getUniqueId();
        estado = Estado.LUTANDO;
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
        Bukkit.broadcast(Component.text("☠ " + chefe.nome() + " despertou em " + coords() + "! A luta começou.", chefe.cor()));
    }

    private void terminar() {
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
        estado = Estado.PARADO;
        entidade = null;
        agendar();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (!m.getPersistentDataContainer().has(CHAVE)) return;
        List<ItemStack> loot = e.getDrops();
        loot.add(Raro.FRAGMENTO_DE_FORJA.criar(4 + rnd().nextInt(4)));
        loot.add(Raro.ESSENCIA_PRIMORDIAL.criar(1));
        loot.add(Raro.PEDRA_DE_PROTECAO.criar(2));
        Gema[] gs = Gema.values();
        for (int i = 0; i < 2; i++) loot.add(gs[rnd().nextInt(gs.length)].criar(2, 1));
        if (rnd().nextDouble() < 0.5) loot.add(ItensMagicos.tomoAleatorio());
        loot.add(new ItemStack(Material.DIAMOND, 4 + rnd().nextInt(5)));
        e.setDroppedExp(e.getDroppedExp() + 800);
        StringBuilder nomes = new StringBuilder();
        for (Player p : m.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(m.getLocation()) > 48 * 48) continue;
            nomes.append(nomes.isEmpty() ? "" : ", ").append(p.getName());
            plugin.titulos().registrar(p, "chefe_mundial", 1);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.8f);
        }
        Bukkit.broadcast(Component.text("☠ O Chefe Mundial " + chefe.nome() + " caiu! Heróis: " + (nomes.isEmpty() ? "?" : nomes), NamedTextColor.GOLD));
        terminar();
    }

    /** Para o /rpgadmin e o /bestiario. */
    public String situacao() {
        return switch (estado) {
            case PARADO -> "próximo em ~" + Math.max(0, (proximo - System.currentTimeMillis()) / 3_600_000) + "h";
            case CONTAGEM -> chefe.nome() + " chegando em " + coords();
            case ESPERANDO -> chefe.nome() + " esperando em " + coords();
            case LUTANDO -> chefe.nome() + " lutando em " + coords();
        };
    }

    public void parar() {
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(barra);
    }
}
