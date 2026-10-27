package br.rpgatributos.visual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.arcano.Essencia;
import br.rpgatributos.arcano.Forma;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * 2.91–2.92: os círculos mágicos, desenhados com partículas (sem pacote de texturas), no mesmo
 * traço dos rituais: anel duplo, marcas de runa entre os anéis e, no meio, uma figura (polígono
 * ou estrela). Nas magias a figura é a do elemento; nos outros sistemas, a de quem chama
 * (o deus, o rank do portal, a raridade...).
 * Onde aparece: magias (frente, chão, céu), golpes marcados dos chefes (enchendo até o golpe),
 * lacaios saindo de pentagramas, habilidades de classe, Levante-se, portais no céu, Pedras de
 * Viagem, nível, renascer, rank e Despertar, forja Lendária/Mítica, preces e milagres,
 * constelações, Lua Azul/Eclipse/Lua de Sangue, Marco do território, captura na guerra e
 * guardiões. Só visual.
 */
public final class CirculosMagicos {

    private enum Plano { FRENTE, CHAO, CEU }

    private static final class Circulo {
        UUID dono;
        Plano plano;
        Color anel, figura;
        int lados, passo;
        int duracao;
        double raio, feixe;
        boolean seguir, crescer, longe;
        BooleanSupplier vivo;
        Location centro;
        Vector u = new Vector(1, 0, 0), v = new Vector(0, 0, 1);
        int idade;
    }

    private final RPGAtributos plugin;
    private final List<Circulo> ativos = new ArrayList<>();

    public CirculosMagicos(RPGAtributos plugin) { this.plugin = plugin; }

    private boolean magiasLigadas() { return plugin.getConfig().getBoolean("embelezamento.circulos-magicos", true); }

    private boolean ligado() { return plugin.getConfig().getBoolean("embelezamento.circulos-sistemas", true); }

    public static Color cor(TextColor c) { return Color.fromRGB(c.value()); }

    public static final Color BRANCO = Color.WHITE, OURO = Color.fromRGB(0xFFD27A), SANGUE = Color.fromRGB(0xD50000),
            SOMBRA = Color.fromRGB(0x5A2D82), ESCURO = Color.fromRGB(0x1E0F2E);

    // =====================================================================
    //  Magias (2.91)
    // =====================================================================

    /** Chamado quando uma magia comum (ou receita) acontece. */
    public void conjurar(Player p, Set<Essencia> es, Forma forma, boolean receita) {
        if (!magiasLigadas() || es.isEmpty()) return;
        switch (forma) {
            case AURA -> chao(p, p.getLocation(), es, 34, 2.6, true);
            case CORPO -> chao(p, p.getLocation(), es, 24, 1.5, true);
            case ARMA -> chao(p, p.getLocation(), es, 18, 1.1, true);
            case CRIACAO, INVOCACAO -> {
                Block b = p.getTargetBlockExact(forma == Forma.INVOCACAO ? 6 : 20);
                Location l = b != null ? b.getLocation().add(0.5, 1, 0.5)
                        : p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(2));
                chao(p, l, es, forma == Forma.INVOCACAO ? 40 : 24, forma == Forma.INVOCACAO ? 1.8 : 1.4, false);
            }
            case RAIO, CHUVA -> { } // o raio e a chuva desenham o deles (enquanto duram)
            default -> frente(p, es, receita ? 20 : 14, receita ? 1.6 : forma == Forma.SOPRO ? 1.4 : 1.1, false, false, null);
        }
        if (receita && forma != Forma.AURA && forma != Forma.CORPO) chao(p, p.getLocation(), es, 26, 2.2, true);
    }

    private static Color[] cores(Set<Essencia> es) {
        List<Essencia> l = List.copyOf(es);
        Essencia a = l.get(0), b = l.size() > 1 ? l.get(1) : a;
        return new Color[]{a.poeira(1).getColor(), b.poeira(1).getColor()};
    }

    private static int[] figuraDe(Set<Essencia> es) {
        List<Essencia> l = List.copyOf(es);
        return figura(l.size() > 1 ? l.get(1) : l.get(0));
    }

    /** Círculo de pé na frente de quem lança, virado para onde ele olha. */
    public void frente(Player p, Set<Essencia> es, int duracao, double raio, boolean seguir, boolean crescer, BooleanSupplier vivo) {
        if (!magiasLigadas() || es.isEmpty()) return;
        Color[] c = cores(es);
        int[] f = figuraDe(es);
        Circulo k = base(p, Plano.FRENTE, p.getEyeLocation(), raio, duracao, c[0], c[1], f[0], f[1]);
        k.seguir = seguir;
        k.crescer = crescer;
        k.vivo = vivo;
        orientarFrente(k, p);
        adicionar(k);
    }

    /** Círculo deitado no chão de uma magia (seguindo quem lança, ou parado). */
    public void chao(Player p, Location onde, Set<Essencia> es, int duracao, double raio, boolean seguir) {
        if (!magiasLigadas() || es.isEmpty()) return;
        Color[] c = cores(es);
        int[] f = figuraDe(es);
        Circulo k = base(p, Plano.CHAO, onde.clone().add(0, 0.12, 0), raio, duracao, c[0], c[1], f[0], f[1]);
        k.seguir = seguir;
        adicionar(k);
    }

    /** Círculo no céu, sobre a área de uma chuva. */
    public void ceu(Player p, Location onde, Set<Essencia> es, int duracao, double raio) {
        if (!magiasLigadas() || es.isEmpty()) return;
        Color[] c = cores(es);
        int[] f = figuraDe(es);
        adicionar(base(p, Plano.CEU, onde.clone(), raio, duracao, c[0], c[1], f[0], f[1]));
    }

    // =====================================================================
    //  Os outros sistemas (2.92)
    // =====================================================================

    /** Um círculo parado no chão (sem dono), com feixe opcional subindo do centro. */
    public void chaoEm(Location onde, double raio, int duracao, Color anel, Color figura, int lados, int passo, double feixe) {
        if (!ligado() || onde == null || onde.getWorld() == null) return;
        Circulo k = base(null, Plano.CHAO, onde.clone().add(0, 0.12, 0), raio, duracao, anel, figura, lados, passo);
        k.feixe = feixe;
        k.longe = raio >= 3 || feixe >= 10;
        adicionar(k);
    }

    /** Um círculo grande no céu, visto de longe. */
    public void ceuEm(Location onde, double raio, int duracao, Color anel, Color figura, int lados, int passo) {
        if (!ligado() || onde == null || onde.getWorld() == null) return;
        Circulo k = base(null, Plano.CEU, onde.clone(), raio, duracao, anel, figura, lados, passo);
        k.longe = true;
        adicionar(k);
    }

    /** Um círculo no chão que acompanha o jogador. */
    public void seguindo(Player p, double raio, int duracao, Color anel, Color figura, int lados, int passo, double feixe) {
        if (!ligado() || p == null) return;
        Circulo k = base(p, Plano.CHAO, p.getLocation().add(0, 0.12, 0), raio, duracao, anel, figura, lados, passo);
        k.seguir = true;
        k.feixe = feixe;
        k.longe = feixe >= 10;
        adicionar(k);
    }

    /** O mesmo círculo no céu sobre cada jogador do mundo (eventos do céu). */
    public void ceuParaTodos(World w, Color anel, Color figura, int lados, int passo) {
        if (!ligado()) return;
        for (Player p : w.getPlayers()) ceuEm(p.getLocation().add(0, 28, 0), 14, 120, anel, figura, lados, passo);
    }

    /** Uma constelação ligada no céu, na direção em que o jogador olha (as estrelas saem da semente). */
    public void constelacao(Player p, int semente, Color cor) {
        if (!ligado()) return;
        Random r = new Random(semente * 7919L);
        int n = 5 + r.nextInt(4);
        Vector d = p.getEyeLocation().getDirection().setY(0);
        if (d.lengthSquared() < 0.01) d = new Vector(0, 0, 1);
        d.normalize();
        Vector lado = d.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Location base = p.getEyeLocation().add(d.clone().multiply(18)).add(0, 16, 0);
        List<Location> estrelas = new ArrayList<>();
        for (int i = 0; i < n; i++) estrelas.add(base.clone().add(lado.clone().multiply(r.nextDouble(-9, 9))).add(0, r.nextDouble(-4, 6), 0)
                .add(d.clone().multiply(r.nextDouble(-3, 3))));
        Particle.DustOptions linha = new Particle.DustOptions(cor, 0.8f), estrela = new Particle.DustOptions(BRANCO, 2f);
        int[] t = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, tarefa -> {
            if (!p.isOnline() || t[0]++ > 40) { tarefa.cancel(); return; }
            // As estrelas acendem uma a uma; as linhas se desenham entre elas.
            int acesas = Math.min(n, t[0] / 3 + 1);
            for (int i = 0; i < acesas; i++) {
                p.spawnParticle(Particle.DUST, estrelas.get(i), 3, 0.05, 0.05, 0.05, 0, estrela);
                p.spawnParticle(Particle.END_ROD, estrelas.get(i), 1, 0.1, 0.1, 0.1, 0.005);
                if (i == 0) continue;
                Location a = estrelas.get(i - 1), b = estrelas.get(i);
                int m = (int) (a.distance(b) * 3);
                for (int j = 0; j <= m; j++) p.spawnParticle(Particle.DUST, a.clone().add(b.clone().subtract(a).toVector().multiply(j / (double) m)), 1, 0, 0, 0, 0, linha);
            }
        }, 0L, 2L);
    }

    /**
     * Desenha uma vez (para quem já tem o próprio relógio: o golpe do chefe, a captura do Marco).
     * {@code progresso} de 0 a 1 enche o anel de fora e faz um anel de carga crescer do centro.
     */
    public static void desenharUmaVez(Location centro, double raio, double giro, Color anel, Color figura, int lados, int passo,
                                      double progresso, boolean longe) {
        Circulo k = new Circulo();
        k.plano = Plano.CHAO;
        k.centro = centro;
        k.raio = raio;
        k.anel = anel;
        k.figura = figura;
        k.lados = lados;
        k.passo = passo;
        k.longe = longe;
        desenho(k, raio, giro, 1, Math.max(0, Math.min(1, progresso)));
    }

    // =====================================================================
    //  Registro e tick
    // =====================================================================

    private static Circulo base(Player p, Plano plano, Location centro, double raio, int duracao, Color anel, Color figura, int lados, int passo) {
        Circulo k = new Circulo();
        k.dono = p == null ? null : p.getUniqueId();
        k.plano = plano;
        k.centro = centro;
        k.raio = raio;
        k.duracao = duracao;
        k.anel = anel;
        k.figura = figura;
        k.lados = lados;
        k.passo = passo;
        return k;
    }

    private void adicionar(Circulo c) {
        if (c.dono != null && ativos.stream().filter(x -> c.dono.equals(x.dono)).count() >= 6) return;
        if (ativos.size() >= 96) return;
        ativos.add(c);
    }

    private static void orientarFrente(Circulo c, Player p) {
        Vector d = p.getEyeLocation().getDirection().normalize();
        c.centro = p.getEyeLocation().add(d.clone().multiply(1.3)).subtract(0, 0.25, 0);
        Vector cima = Math.abs(d.getY()) > 0.95 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        c.u = d.clone().crossProduct(cima).normalize();
        c.v = c.u.clone().crossProduct(d).normalize();
    }

    public void tick() {
        Iterator<Circulo> it = ativos.iterator();
        while (it.hasNext()) {
            Circulo c = it.next();
            Player p = c.dono == null ? null : Bukkit.getPlayer(c.dono);
            boolean acabou = (c.vivo != null && !c.vivo.getAsBoolean()) || c.idade > c.duracao;
            boolean semDono = c.dono != null && (p == null || p.isDead() || p.getWorld() != c.centro.getWorld());
            if (acabou || semDono || !c.centro.isChunkLoaded()) { it.remove(); continue; }
            if (c.seguir && p != null) {
                if (c.plano == Plano.FRENTE) orientarFrente(c, p);
                else c.centro = p.getLocation().add(0, 0.12, 0);
            }
            double entrada = Math.min(1, c.idade / 6.0);
            double saida = c.vivo != null ? 1 : Math.min(1, (c.duracao - c.idade) / 6.0);
            double raio = c.raio * (c.crescer ? Math.min(1, 0.3 + c.idade / 40.0) : entrada);
            double giro = c.idade * (c.plano == Plano.CEU ? 0.03 : 0.08);
            if (raio >= 0.15) desenho(c, raio, giro, Math.max(0.15, Math.min(entrada, saida)), 1);
            if (c.feixe > 0 && c.idade % 2 == 0 && saida > 0.3) feixe(c);
            c.idade += 2;
        }
    }

    public void parar() { ativos.clear(); }

    // =====================================================================
    //  O desenho
    // =====================================================================

    private static void desenho(Circulo c, double raio, double giro, double forca, double progresso) {
        World w = c.centro.getWorld();
        float tam = (float) (c.plano == Plano.CEU ? 1.6 : 0.7 + 0.3 * forca);
        Particle.DustOptions anel = new Particle.DustOptions(c.anel, tam), figura = new Particle.DustOptions(c.figura, tam),
                fino = new Particle.DustOptions(BRANCO, tam * 0.7f);
        boolean longe = c.longe;
        // anel de fora (só o pedaço já carregado, se tiver progresso) e anel de dentro ao contrário
        int n = (int) Math.max(16, raio * 14);
        int ate = (int) Math.ceil(n * progresso);
        for (int i = 0; i < n; i++) {
            double t = giro + i * Math.PI * 2 / n;
            if (i < ate) ponto(w, c, raio, t, anel, longe);
            if (i % 2 == 0) ponto(w, c, raio * 0.8, -t, fino, longe);
        }
        for (int i = 0; i < 12; i++) {
            double t = giro + i * Math.PI * 2 / 12;
            for (double r = 0.82; r <= 0.98; r += 0.08) ponto(w, c, raio * r, t, anel, longe);
        }
        double rf = raio * 0.78;
        for (int k = 0; k < c.lados; k++) {
            double t1 = -giro + k * Math.PI * 2 / c.lados - Math.PI / 2, t2 = -giro + ((k + c.passo) % c.lados) * Math.PI * 2 / c.lados - Math.PI / 2;
            double x1 = Math.cos(t1) * rf, y1 = Math.sin(t1) * rf, x2 = Math.cos(t2) * rf, y2 = Math.sin(t2) * rf;
            int m = (int) Math.max(4, Math.hypot(x2 - x1, y2 - y1) * 5);
            for (int i = 0; i <= m; i++) {
                double f = i / (double) m;
                w.spawnParticle(Particle.DUST, em(c, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f), 1, 0, 0, 0, 0, figura, longe);
            }
        }
        // carga: um anel que cresce do centro até a borda
        if (progresso < 1) {
            int m = (int) Math.max(10, raio * progresso * 12);
            for (int i = 0; i < m; i++) ponto(w, c, raio * progresso, giro * 3 + i * Math.PI * 2 / m, anel, longe);
        }
        for (int i = 0; i < 8; i++) ponto(w, c, raio * 0.18, giro * 2 + i * Math.PI / 4, fino, longe);
        if ((int) (giro * 100) % 6 == 0) w.spawnParticle(Particle.ENCHANT, c.centro, (int) (raio * 5), raio * 0.4, 0.15, raio * 0.4, 0.4, null, longe);
    }

    private static void feixe(Circulo c) {
        World w = c.centro.getWorld();
        Particle.DustOptions po = new Particle.DustOptions(c.anel, 1.4f);
        for (double y = 0; y <= c.feixe; y += 0.6) w.spawnParticle(Particle.DUST, c.centro.clone().add(0, y, 0), 1, 0.06, 0, 0.06, 0, po, c.longe);
        w.spawnParticle(Particle.END_ROD, c.centro.clone().add(0, Math.random() * c.feixe, 0), 1, 0.1, 0.3, 0.1, 0.02, null, c.longe);
    }

    private static void ponto(World w, Circulo c, double r, double t, Particle.DustOptions po, boolean longe) {
        w.spawnParticle(Particle.DUST, em(c, Math.cos(t) * r, Math.sin(t) * r), 1, 0, 0, 0, 0, po, longe);
    }

    private static Location em(Circulo c, double x, double y) {
        return c.centro.clone().add(c.u.clone().multiply(x)).add(c.v.clone().multiply(y));
    }

    /** A figura de cada elemento: {lados, passo} (passo 1 = polígono, 2+ = estrela). */
    public static int[] figura(Essencia e) {
        return switch (e) {
            case FOGO -> new int[]{3, 1};      // triângulo
            case GELO -> new int[]{6, 2};      // hexagrama (floco)
            case VENTO -> new int[]{8, 3};     // estrela de 8 pontas
            case AGUA -> new int[]{5, 1};      // pentágono
            case VIDA -> new int[]{6, 1};      // hexágono
            case TERRA -> new int[]{4, 1};     // quadrado
            case SOMBRA -> new int[]{5, 2};    // pentagrama
            case VAZIO -> new int[]{7, 3};     // heptagrama
            case ENERGIA -> new int[]{10, 3};  // decagrama (raio)
            case NATUREZA -> new int[]{7, 2};  // estrela de 7 pontas
        };
    }
}
