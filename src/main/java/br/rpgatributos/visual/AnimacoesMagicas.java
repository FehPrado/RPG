package br.rpgatributos.visual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.arcano.Forma;
import br.rpgatributos.arcano.Receita;
import net.kyori.adventure.key.Key;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;
import java.util.*;

/** Animações só visuais: duração e limpeza centralizadas; não alteram dano nem colisões. */
public final class AnimacoesMagicas {
    private final RPGAtributos plugin;
    private static final class Efeito {
        final ItemDisplay display; final UUID dono; final int duracao; final String tipo;
        Location pos; int idade; boolean seguir;
        Efeito(ItemDisplay display, Player p, Location pos, String tipo, int duracao) {
            this.display = display; this.dono = p.getUniqueId(); this.pos = pos.clone(); this.tipo = tipo; this.duracao = duracao;
        }
    }
    private final Map<UUID, Efeito> efeitos = new LinkedHashMap<>();
    public AnimacoesMagicas(RPGAtributos plugin) { this.plugin = plugin; }

    public static ItemStack item(String id) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("rpgatributos", "beleza_" + id));
        return item;
    }
    private UUID criar(Player p, Location pos, String modelo, String tipo, int duracao) {
        if (!plugin.getConfig().getBoolean("embelezamento.magias", true) || !pos.isChunkLoaded()) return null;
        int limite = Math.max(16, Math.min(256, plugin.getConfig().getInt("embelezamento.limite-animacoes", 96)));
        if (efeitos.size() >= limite || efeitos.values().stream().filter(e -> e.dono.equals(p.getUniqueId())).count() >= 12) return null;
        ItemDisplay d = pos.getWorld().spawn(pos, ItemDisplay.class, e -> {
            e.setItemStack(item(modelo)); e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            e.setPersistent(false); e.setInvulnerable(true); e.setGravity(false); e.setViewRange(1.5f);
            e.setInterpolationDuration(2); e.setTeleportDuration(2); e.setBrightness(new Display.Brightness(15, 15));
            e.setTransformationMatrix(new Matrix4f().scale(.01f));
        });
        efeitos.put(d.getUniqueId(), new Efeito(d, p, pos, tipo, Math.max(6, duracao))); return d.getUniqueId();
    }
    private static Essencia principal(Set<Essencia> es) { return es.stream().findFirst().orElse(Essencia.ENERGIA); }
    public void conjurar(Player p, Set<Essencia> es, Forma forma, Receita receita) {
        Essencia e = principal(es);
        UUID id = criar(p, p.getLocation().add(0, .08, 0), "runa_" + e.name().toLowerCase(Locale.ROOT), "runa", 20);
        if (id != null && (forma == Forma.CORPO || forma == Forma.AURA)) efeitos.get(id).seguir = true;
        if (forma == Forma.AURA && es.contains(Essencia.FOGO)) pilar(p, p.getLocation(), 24);
    }
    public UUID orbe(Player p, Location pos, Set<Essencia> es, int duracao) {
        return criar(p, pos, "orbe_" + principal(es).name().toLowerCase(Locale.ROOT), "orbe", duracao);
    }
    public void mover(UUID id, Location pos) { Efeito e = efeitos.get(id); if (e != null) e.pos = pos.clone(); }
    public void remover(UUID id) { Efeito e = efeitos.remove(id); if (e != null) e.display.remove(); }
    public void buracoNegro(Player p, Location pos, int duracao) { criar(p, pos.clone().add(0, .7, 0), "buraco_negro", "buraco_negro", duracao); }
    public void pilar(Player p, Location pos, int duracao) { criar(p, pos, "pilar_fogo", "pilar", duracao); }
    public void invocacao(Player p, Location pos, Set<Essencia> es) { criar(p, pos.clone().add(0, .05, 0), "invocacao_" + principal(es).name().toLowerCase(Locale.ROOT), "invocacao", 32); }
    public void impacto(Player p, Location pos, Set<Essencia> es) { criar(p, pos, "orbe_" + principal(es).name().toLowerCase(Locale.ROOT), "impacto", 12); }

    /** Função sem servidor usada também para validar os limites da animação. */
    public static float[] escala(String tipo, int idade, int duracao) {
        double progresso = Math.max(0, Math.min(1, idade / (double) Math.max(1, duracao)));
        float entrada = (float) Math.min(1, progresso * 6), saida = (float) Math.min(1, (1 - progresso) * 6);
        float pulso = (float) (1 + Math.sin(idade * .22) * .08), s = Math.max(.01f, entrada * saida * pulso);
        return switch (tipo) {
            case "buraco_negro" -> new float[]{s * 2, s * 1.3f, s * 2};
            case "pilar" -> new float[]{s * 1.4f, s * 5, s * 1.4f};
            case "runa", "invocacao" -> new float[]{s * 3, .5f, s * 3};
            case "orbe" -> new float[]{.35f, .35f, .35f};
            default -> new float[]{s * 1.2f, s * 1.2f, s * 1.2f};
        };
    }
    public void tick() {
        Iterator<Efeito> it = efeitos.values().iterator();
        while (it.hasNext()) {
            Efeito e = it.next(); Player p = Bukkit.getPlayer(e.dono);
            if (p == null || p.isDead() || !e.display.isValid() || !e.pos.isChunkLoaded() || e.idade >= e.duracao || p.getWorld() != e.pos.getWorld()) {
                e.display.remove(); it.remove(); continue;
            }
            if (e.seguir) e.pos = p.getLocation().add(0, .08, 0);
            e.display.teleport(e.pos);
            float[] s = escala(e.tipo, e.idade, e.duracao);
            Matrix4f matriz = new Matrix4f().rotateY(e.idade * .12f).scale(s[0], s[1], s[2]).translate(-.5f, 0, -.5f);
            if (e.tipo.equals("orbe") || e.tipo.equals("impacto")) matriz.translate(0, -.5f, 0);
            e.display.setInterpolationDelay(0); e.display.setTransformationMatrix(matriz); e.idade += 2;
        }
    }
    public void parar() { efeitos.values().forEach(e -> e.display.remove()); efeitos.clear(); }
}
