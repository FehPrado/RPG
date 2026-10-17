package br.rpgatributos.raca;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.exploracao.PacoteRecursos;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * As asas do Povo-Pássaro (estágio III em diante), abertas só enquanto ele plana, bate as asas
 * ou voa. Quem tem o pacote de recursos vê duas asas de verdade (dois ItemDisplay com o desenho
 * do pacote, que batem com interpolação); quem não tem vê as asas desenhadas com partículas.
 * Só existem enquanto as asas estão abertas e são poucas por vez, então não pesam como os
 * enfeites que seguiam montarias o tempo todo.
 */
public final class Asas {

    private static final Particle.DustOptions PENA = new Particle.DustOptions(Color.fromRGB(0xF5F5F5), 0.9f);
    private static final Particle.DustOptions PONTA = new Particle.DustOptions(Color.fromRGB(0x90A4AE), 0.8f);
    private static final Particle.DustOptions ESCAMA = new Particle.DustOptions(Color.fromRGB(0xE65100), 0.9f);
    private static final Particle.DustOptions ESCAMA_PONTA = new Particle.DustOptions(Color.fromRGB(0x4E1A08), 0.8f);

    private final RPGAtributos plugin;
    private final Racas racas;

    private static final class Par {
        final ItemDisplay esq, dir;
        final Set<UUID> mostrado = new HashSet<>();
        Par(ItemDisplay esq, ItemDisplay dir) {
            this.esq = esq;
            this.dir = dir;
        }
    }

    private final Map<UUID, Par> abertas = new HashMap<>();
    private final Map<UUID, Integer> batendo = new HashMap<>();

    Asas(RPGAtributos plugin, Racas racas) {
        this.plugin = plugin;
        this.racas = racas;
    }

    /** Bateu as asas: anima por alguns ticks. */
    void batida(Player p) {
        batendo.put(p.getUniqueId(), 10);
    }

    public void fechar(UUID id) {
        Par par = abertas.remove(id);
        batendo.remove(id);
        if (par == null) return;
        par.esq.remove();
        par.dir.remove();
    }

    /** Fecha todas (ao desligar o plugin). */
    public void fecharTodas() {
        for (UUID id : List.copyOf(abertas.keySet())) fechar(id);
    }

    void tick(int ciclo) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            boolean planando = plugin.mobilidade().planando(p);
            Raca r = racas.de(p);
            boolean aberto = planando && racas.estagio(p) >= 3 && ((r == Raca.PASSARO && !Racas.pesada(p)) || r == Raca.DRACONATO);
            if (!aberto) {
                if (abertas.containsKey(id)) fechar(id);
                continue;
            }
            boolean voando = racas.voar(p);
            if (voando) batendo.put(id, Math.max(batendo.getOrDefault(id, 0), 4));
            int bat = batendo.getOrDefault(id, 0);
            if (bat > 0) batendo.put(id, bat - 1);
            if (ciclo % 2 != 0) continue;
            // Abertas planando; batendo, abrem e fecham a cada 3 ticks.
            double angulo = bat > 0 ? ((ciclo / 2) % 2 == 0 ? 10 : 62) : 18;
            modelo(p, angulo, ciclo);
            if (ciclo % 4 == 0) particulas(p, angulo);
        }
    }

    private static double escala(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.SCALE);
        return a == null ? 1 : a.getValue();
    }

    /** Onde ficam as asas: na frente do movimento um pouco, para não ficarem para trás. */
    private static Location ancora(Player p) {
        Location l = p.getLocation();
        Vector v = p.getVelocity();
        l.add(v.getX() * 2, 0, v.getZ() * 2);
        l.setYaw(p.getBodyYaw());
        l.setPitch(0);
        return l;
    }

    // =====================================================================
    //  Com o pacote: dois ItemDisplay
    // =====================================================================

    private void modelo(Player p, double angulo, int ciclo) {
        UUID id = p.getUniqueId();
        Par par = abertas.get(id);
        Location l = ancora(p);
        if (par == null || !par.esq.isValid() || !par.dir.isValid()) {
            if (par != null) fechar(id);
            boolean dragao = racas.de(p) == Raca.DRACONATO;
            par = new Par(criar(l, dragao), criar(l, dragao));
            abertas.put(id, par);
        }
        par.esq.teleport(l);
        par.dir.teleport(l);
        double e = escala(p);
        par.esq.setInterpolationDelay(0);
        par.dir.setInterpolationDelay(0);
        double tam = racas.de(p) == Raca.DRACONATO ? e * 0.6 : e; // as asas do Draconato são curtas
        par.esq.setTransformationMatrix(matriz(tam, angulo, 1));
        par.dir.setTransformationMatrix(matriz(tam, angulo, -1));
        // Mostra só para quem tem o pacote (sem ele o item apareceria como uma pena comum).
        if (ciclo % 10 == 0 || par.mostrado.isEmpty()) {
            for (Player v : p.getWorld().getPlayers()) {
                if (par.mostrado.contains(v.getUniqueId()) || !plugin.ajuda().temPacote(v)) continue;
                v.showEntity(plugin, par.esq);
                v.showEntity(plugin, par.dir);
                par.mostrado.add(v.getUniqueId());
            }
        }
    }

    private ItemDisplay criar(Location l, boolean dragao) {
        ItemStack asa = new ItemStack(Material.FEATHER);
        asa.editMeta(m -> PacoteRecursos.marcar(m, dragao ? "asa_dragao" : "asa_passaro"));
        return l.getWorld().spawn(l, ItemDisplay.class, d -> {
            d.setItemStack(asa);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTeleportDuration(2);
            d.setInterpolationDuration(3);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        });
    }

    /**
     * A asa de um lado (+1 esquerda, -1 direita): sai das costas na altura do ombro e gira para
     * trás pelo ângulo. O desenho tem a raiz na borda esquerda; a direita é o mesmo desenho
     * espelhado.
     */
    private static Matrix4f matriz(double escala, double angulo, int lado) {
        float s = (float) (1.35 * escala);
        float a = (float) Math.toRadians(angulo) * lado;
        return new Matrix4f()
                .translate(0.12f * lado * (float) escala, (float) (1.2 * escala), (float) (-0.22 * escala))
                .rotateY(a)
                .rotateZ((float) Math.toRadians(12) * lado)
                .translate(0.5f * s * lado, 0, 0)
                .scale(s * lado, s, s);
    }

    // =====================================================================
    //  Sem o pacote: partículas
    // =====================================================================

    private void particulas(Player p, double angulo) {
        boolean dragao = racas.de(p) == Raca.DRACONATO;
        double e = escala(p) * (dragao ? 0.6 : 1);
        Location base = ancora(p);
        double yaw = Math.toRadians(base.getYaw());
        // Frente do corpo e o lado esquerdo (o +X do corpo).
        Vector frente = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector esquerda = new Vector(Math.cos(yaw), 0, Math.sin(yaw));
        double a = Math.toRadians(angulo);
        List<Player> veem = new java.util.ArrayList<>();
        for (Player v : p.getWorld().getPlayers()) {
            if (!plugin.ajuda().temPacote(v) && v.getLocation().distanceSquared(base) < 48 * 48) veem.add(v);
        }
        if (veem.isEmpty()) return;
        for (int lado = -1; lado <= 1; lado += 2) {
            for (int k = 0; k <= 7; k++) {
                double t = k / 7.0;
                double vao = 1.3 * e * t;
                double x = lado * (0.12 * e + vao * Math.cos(a));
                double z = -0.22 * e - vao * Math.sin(a);
                // Borda de cima (sobe até a ponta) e a de baixo (as penas, mais curtas na ponta).
                double cima = 1.2 * e + 0.45 * e * t - 0.25 * e * t * t;
                double baixo = cima - 0.55 * e * (1 - t) - 0.08 * e;
                for (int fila = 0; fila < 2; fila++) {
                    double y = fila == 0 ? cima : baixo;
                    Location pt = base.clone().add(esquerda.clone().multiply(x)).add(frente.clone().multiply(z)).add(0, y, 0);
                    for (Player v : veem) v.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, dragao ? (k >= 6 ? ESCAMA_PONTA : ESCAMA) : k >= 6 ? PONTA : PENA);
                }
            }
        }
    }
}
