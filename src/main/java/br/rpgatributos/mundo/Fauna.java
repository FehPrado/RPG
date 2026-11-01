package br.rpgatributos.mundo;

import br.rpgatributos.ModoTeste;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.PacoteRecursos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Os animais novos (3.16), feitos com bichos do jogo (sem mod): cervo e corça (fogem de quem corre;
 * o cervo tem galhada), javali (investe em quem chega perto ou bate nele), coruja (de noite, nas
 * matas), pinguim (praias geladas; às vezes pesca um peixe), tartaruga-gigante (rara nas praias; dá
 * para subir nela) e borboletas e mariposas (enfeite; pegue com uma garrafa de vidro).
 * Ninguém é salvo no mundo: aparecem perto de quem explora e somem quando ficam sozinhos.
 */
public final class Fauna implements Listener {

    public enum Tipo {
        CERVO("Cervo", 0x8D6E63), CORCA("Corça", 0xA1887F), JAVALI("Javali", 0x6D4C41), CORUJA("Coruja", 0x9E9E9E),
        PINGUIM("Pinguim", 0x37474F), TARTARUGA("Tartaruga-Gigante", 0x558B2F);

        final String nome;
        final TextColor cor;

        Tipo(String nome, int cor) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
        }

        public String nome() { return nome; }
    }

    /** Carne de caça: entra nas receitas que pedem "qualquer carne". */
    public enum Carne {
        CERVO("Carne de Cervo", Material.BEEF, 0xB0503A), JAVALI("Carne de Javali", Material.PORKCHOP, 0xC0705A);

        final String nome;
        final Material base;
        final TextColor cor;

        Carne(String nome, Material base, int cor) {
            this.nome = nome;
            this.base = base;
            this.cor = TextColor.color(cor);
        }

        public ItemStack criar(int qtd) {
            ItemStack i = new ItemStack(base, qtd);
            i.editMeta(m -> {
                m.itemName(Component.text(nome, cor));
                m.lore(List.of(Component.text("Carne de caça. Entra nas receitas com \"qualquer carne\".", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
                m.getPersistentDataContainer().set(K_CARNE, PersistentDataType.STRING, name());
                PacoteRecursos.marcar(m, "fauna_carne_" + name().toLowerCase(Locale.ROOT));
            });
            return i;
        }
    }

    public enum Cor {
        AZUL("Borboleta Azul", 0x42A5F5), AMARELA("Borboleta Amarela", 0xFDD835), MONARCA("Borboleta-Monarca", 0xFF8F00),
        BRANCA("Borboleta Branca", 0xF5F5F5), MARIPOSA("Mariposa", 0x9E9E8E);

        final String nome;
        final TextColor cor;

        Cor(String nome, int cor) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
        }

        String id() { return name().toLowerCase(Locale.ROOT); }

        /** O bichinho voando (só visual). */
        ItemStack voando() {
            ItemStack i = new ItemStack(Material.PAPER);
            i.editMeta(m -> PacoteRecursos.marcar(m, "fauna_" + (this == MARIPOSA ? "mariposa" : "borboleta_" + id())));
            return i;
        }

        public ItemStack pote() {
            ItemStack i = new ItemStack(Material.PAPER);
            i.editMeta(m -> {
                m.itemName(Component.text(nome + " no Pote", cor));
                m.lore(List.of(Component.text("Agachado + clique: solte ela de novo.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                m.setMaxStackSize(16);
                m.getPersistentDataContainer().set(K_POTE, PersistentDataType.STRING, name());
                PacoteRecursos.marcar(m, "fauna_pote_" + id());
            });
            return i;
        }
    }

    static final NamespacedKey K_FAUNA = new NamespacedKey("rpgatributos", "fauna");
    static final NamespacedKey K_CARNE = new NamespacedKey("rpgatributos", "carne_de_caca");
    static final NamespacedKey K_POTE = new NamespacedKey("rpgatributos", "borboleta_pote");
    private static final NamespacedKey K_PECA = new NamespacedKey("rpgatributos", "fauna_peca");

    /** Uma borboleta ou mariposa voando em volta de um ponto. */
    private static final class Voo {
        final ItemDisplay d;
        final Location centro;
        final Cor cor;
        final long ate;
        double fase = ThreadLocalRandom.current().nextDouble(Math.PI * 2);

        Voo(ItemDisplay d, Location centro, Cor cor, long ate) {
            this.d = d;
            this.centro = centro;
            this.cor = cor;
            this.ate = ate;
        }
    }

    private final RPGAtributos plugin;
    /** Cervo -> as duas metades da galhada. */
    private final Map<UUID, ItemDisplay[]> galhadas = new HashMap<>();
    /** Javali investindo: javali -> alvo e até quando. */
    private final Map<UUID, UUID> alvoJavali = new HashMap<>();
    private final Map<UUID, Long> investidaAte = new HashMap<>();
    private final Map<UUID, Long> ultimoGolpe = new HashMap<>();
    private final List<Voo> voos = new ArrayList<>();
    private int ciclo;
    private boolean flap;

    public Fauna(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private boolean ligado(String chave) {
        return plugin.getConfig().getBoolean("fauna." + chave, true);
    }

    public static Tipo tipo(Entity e) {
        String s = e.getPersistentDataContainer().get(K_FAUNA, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Tipo.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Carne carne(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        String s = i.getPersistentDataContainer().get(K_CARNE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Carne.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Cor pote(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        String s = i.getPersistentDataContainer().get(K_POTE, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Cor.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String bioma(Location l) {
        return l.getWorld().getBiome(l).getKey().getKey();
    }

    private static boolean dia(World w) {
        long t = w.getTime();
        return t < 12300 || t > 23850;
    }

    /** Onde a fauna aparece: o mundo principal e a área de testes. */
    private static boolean mundoValido(World w) {
        return w.equals(Bukkit.getWorlds().getFirst()) || ModoTeste.ehTeste(w);
    }

    private static void base(LivingEntity e, Attribute a, double v) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(v);
    }

    private static void marcar(LivingEntity e, Tipo t) {
        e.setPersistent(false);
        e.setRemoveWhenFarAway(true);
        e.getPersistentDataContainer().set(K_FAUNA, PersistentDataType.STRING, t.name());
        e.customName(Component.text(t.nome, t.cor));
        e.setCustomNameVisible(false);
    }

    // =====================================================================
    //  Nascer
    // =====================================================================

    /** Chamado a cada segundo pelo plugin. */
    public void segundo() {
        ciclo++;
        comportamento();
        if (ciclo % 15 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR || !mundoValido(p.getWorld())) continue;
                try {
                    nascerPerto(p);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Fauna: " + ex.getMessage());
                }
            }
        }
        if (ciclo % 20 == 0) sons();
    }

    private int contar(Player p, Tipo t, double raio) {
        int n = 0;
        for (Entity e : p.getNearbyEntities(raio, 32, raio)) if (tipo(e) == t) n++;
        return n;
    }

    private void nascerPerto(Player p) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        World w = p.getWorld();
        String b = bioma(p.getLocation());
        boolean dia = dia(w);
        if (p.getLocation().getY() < w.getSeaLevel() - 12) return; // no subsolo, nada
        boolean mata = b.contains("forest") || b.contains("taiga") || b.contains("grove") || b.contains("meadow");
        if (ligado("cervos") && mata && dia && contar(p, Tipo.CERVO, 48) + contar(p, Tipo.CORCA, 48) < 3 && r.nextDouble() < 0.18) {
            Location l = lugar(p, r, false);
            if (l != null) {
                soltar(Tipo.CERVO, l);
                for (int i = 0; i < 1 + r.nextInt(2); i++) soltar(Tipo.CORCA, l.clone().add(r.nextInt(5) - 2, 0, r.nextInt(5) - 2));
            }
            return;
        }
        boolean javalis = b.contains("forest") || b.contains("taiga") || b.contains("savanna") || b.contains("jungle");
        if (ligado("javalis") && javalis && contar(p, Tipo.JAVALI, 48) < 2 && r.nextDouble() < 0.12) {
            Location l = lugar(p, r, false);
            if (l != null) for (int i = 0; i < 1 + r.nextInt(2); i++) soltar(Tipo.JAVALI, l.clone().add(r.nextInt(3) - 1, 0, r.nextInt(3) - 1));
            return;
        }
        if (ligado("corujas") && !dia && (b.contains("forest") || b.contains("taiga")) && contar(p, Tipo.CORUJA, 48) < 2 && r.nextDouble() < 0.2) {
            Location l = lugar(p, r, false);
            if (l != null) soltar(Tipo.CORUJA, l.add(0, 1, 0));
            return;
        }
        boolean gelado = b.contains("snowy_beach") || b.contains("frozen") || b.contains("ice_spikes") || b.contains("snowy_plains");
        if (ligado("pinguins") && gelado && contar(p, Tipo.PINGUIM, 48) < 4 && r.nextDouble() < 0.2) {
            Location l = lugar(p, r, false);
            if (l != null) for (int i = 0; i < 2 + r.nextInt(3); i++) soltar(Tipo.PINGUIM, l.clone().add(r.nextInt(5) - 2, 0, r.nextInt(5) - 2));
            return;
        }
        if (ligado("tartarugas") && b.equals("beach") && dia && r.nextDouble() < 0.03) {
            for (Entity e : w.getEntities()) if (tipo(e) == Tipo.TARTARUGA) return; // uma por mundo
            Location l = lugar(p, r, false);
            if (l != null) {
                soltar(Tipo.TARTARUGA, l);
                p.sendActionBar(Component.text("Algo enorme se move na areia...", Tipo.TARTARUGA.cor));
            }
            return;
        }
        if (ligado("borboletas")) borboletas(p, r, dia, b);
    }

    /** Um lugar no chão a 18–34 blocos (de céu aberto). */
    private static Location lugar(Player p, ThreadLocalRandom r, boolean perto) {
        World w = p.getWorld();
        for (int tent = 0; tent < 6; tent++) {
            double a = r.nextDouble() * Math.PI * 2, dist = perto ? 6 + r.nextDouble() * 8 : 18 + r.nextDouble() * 16;
            int x = (int) Math.floor(p.getLocation().getX() + Math.cos(a) * dist), z = (int) Math.floor(p.getLocation().getZ() + Math.sin(a) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block chao = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (chao.isLiquid() || !chao.getType().isSolid() || Tag.LEAVES.isTagged(chao.getType())) continue;
            if (Math.abs(chao.getY() - p.getLocation().getY()) > 20) continue;
            return chao.getLocation().add(0.5, 1, 0.5);
        }
        return null;
    }

    /** Solta um animal (também pelo /rpgadmin). */
    public LivingEntity soltar(Tipo t, Location l) {
        World w = l.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (t) {
            case CERVO, CORCA -> w.spawn(l, Horse.class, h -> {
                marcar(h, t);
                h.setColor(r.nextBoolean() ? Horse.Color.BROWN : Horse.Color.CHESTNUT);
                h.setStyle(t == Tipo.CORCA && r.nextBoolean() ? Horse.Style.WHITE_DOTS : Horse.Style.NONE);
                h.setTamed(false);
                base(h, Attribute.SCALE, t == Tipo.CERVO ? 0.85 : 0.72);
                base(h, Attribute.MAX_HEALTH, t == Tipo.CERVO ? 16 : 12);
                h.setHealth(t == Tipo.CERVO ? 16 : 12);
                base(h, Attribute.MOVEMENT_SPEED, 0.3);
                if (t == Tipo.CERVO) galhada(h);
            });
            case JAVALI -> w.spawn(l, Pig.class, p -> {
                marcar(p, t);
                p.setVariant(Pig.Variant.WARM);
                p.setSoundVariant(Pig.SoundVariant.BIG);
                base(p, Attribute.SCALE, 1.15);
                base(p, Attribute.MAX_HEALTH, 18);
                p.setHealth(18);
                base(p, Attribute.MOVEMENT_SPEED, 0.27);
            });
            case CORUJA -> w.spawn(l, Parrot.class, p -> {
                marcar(p, t);
                p.setVariant(Parrot.Variant.GRAY);
                base(p, Attribute.SCALE, 1.2);
            });
            case PINGUIM -> w.spawn(l, Chicken.class, c -> {
                marcar(c, t);
                c.setVariant(Chicken.Variant.COLD);
                base(c, Attribute.SCALE, 1.15);
                c.setEggLayTime(Integer.MAX_VALUE);
            });
            case TARTARUGA -> w.spawn(l, Turtle.class, tt -> {
                marcar(tt, t);
                base(tt, Attribute.SCALE, 3.0);
                base(tt, Attribute.MAX_HEALTH, 80);
                tt.setHealth(80);
                tt.setCustomNameVisible(true);
            });
        };
    }

    // =====================================================================
    //  A galhada do cervo: duas metades presas na cabeça (sem mod)
    // =====================================================================

    private void galhada(Horse h) {
        ItemDisplay[] par = new ItemDisplay[2];
        for (int i = 0; i < 2; i++) {
            float lado = i == 0 ? 1 : -1;
            par[i] = h.getWorld().spawn(h.getLocation(), ItemDisplay.class, d -> {
                d.setPersistent(false);
                ItemStack chifre = Raro.GALHADA.criar();
                chifre.editMeta(m -> m.setEnchantmentGlintOverride(false));
                d.setItemStack(chifre);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setTeleportDuration(2);
                d.getPersistentDataContainer().set(K_PECA, PersistentDataType.BYTE, (byte) 1);
                // A cabeça do cavalo (escala 0,85) fica ~1,55 acima do chão e ~0,75 à frente.
                d.setTransformation(new Transformation(new Vector3f(0.12f * lado, 1.62f, 0.72f),
                        new AxisAngle4f((float) Math.toRadians(70 * lado), 0, 1, 0), new Vector3f(0.55f, 0.55f, 0.55f), new AxisAngle4f()));
            });
        }
        galhadas.put(h.getUniqueId(), par);
    }

    /** A cada 2 ticks: a galhada acompanha o cervo, as borboletas voam. */
    public void tick() {
        for (Iterator<Map.Entry<UUID, ItemDisplay[]>> it = galhadas.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity h = Bukkit.getEntity(en.getKey());
            if (h == null || !h.isValid()) {
                for (ItemDisplay d : en.getValue()) if (d != null) d.remove();
                it.remove();
                continue;
            }
            Location l = h.getLocation();
            l.setPitch(0);
            for (ItemDisplay d : en.getValue()) if (d != null && d.isValid()) d.teleport(l);
        }
        flap = !flap;
        long agora = System.currentTimeMillis();
        for (Iterator<Voo> it = voos.iterator(); it.hasNext(); ) {
            Voo v = it.next();
            if (!v.d.isValid() || agora > v.ate) {
                v.d.remove();
                it.remove();
                continue;
            }
            v.fase += v.cor == Cor.MARIPOSA ? 0.22 : 0.1;
            double raio = v.cor == Cor.MARIPOSA ? 0.8 : 2.2;
            Location l = v.centro.clone().add(Math.cos(v.fase) * raio, 0.6 + Math.sin(v.fase * 1.7) * 0.4, Math.sin(v.fase * 1.3) * raio);
            v.d.teleport(l);
            float s = flap ? 0.38f : 0.14f;
            v.d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(s, 0.38f, 0.38f), new AxisAngle4f()));
        }
    }

    // =====================================================================
    //  Borboletas e mariposas
    // =====================================================================

    private void borboletas(Player p, ThreadLocalRandom r, boolean dia, String b) {
        int perto = 0;
        for (Voo v : voos) if (v.centro.getWorld().equals(p.getWorld()) && v.centro.distanceSquared(p.getLocation()) < 48 * 48) perto++;
        if (perto >= 8) return;
        if (dia) {
            boolean flores = b.contains("flower") || b.contains("meadow") || b.contains("cherry") || b.contains("plains") || b.contains("forest");
            if (!flores || r.nextDouble() > 0.5) return;
            Location l = lugar(p, r, true);
            if (l == null || !temFlor(l.getBlock(), 3)) return;
            Cor c = new Cor[]{Cor.AZUL, Cor.AMARELA, Cor.MONARCA, Cor.BRANCA}[r.nextInt(4)];
            for (int i = 0; i < 1 + r.nextInt(2); i++) voar(c, l.clone(), 90);
        } else {
            Block luz = luzPerto(p.getLocation().getBlock(), r);
            if (luz != null) for (int i = 0; i < 2; i++) voar(Cor.MARIPOSA, luz.getLocation().add(0.5, -0.2, 0.5), 120);
        }
    }

    private static boolean temFlor(Block centro, int raio) {
        for (int dx = -raio; dx <= raio; dx++) for (int dz = -raio; dz <= raio; dz++) for (int dy = -1; dy <= 1; dy++) {
            if (Tag.FLOWERS.isTagged(centro.getRelative(dx, dy, dz).getType())) return true;
        }
        return false;
    }

    /** Uma lanterna, tocha ou vela acesa perto (as mariposas vêm para a luz). */
    private static Block luzPerto(Block centro, ThreadLocalRandom r) {
        for (int tent = 0; tent < 300; tent++) {
            Block b = centro.getRelative(r.nextInt(21) - 10, r.nextInt(9) - 3, r.nextInt(21) - 10);
            Material m = b.getType();
            if (m == Material.LANTERN || m == Material.TORCH || m == Material.WALL_TORCH || m == Material.SOUL_LANTERN
                    || m == Material.CAMPFIRE || m == Material.GLOWSTONE || m == Material.SEA_LANTERN || m == Material.JACK_O_LANTERN) return b;
        }
        return null;
    }

    /** Solta uma borboleta (ou mariposa) por {@code segundos}. */
    public void voar(Cor c, Location centro, int segundos) {
        ItemDisplay d = centro.getWorld().spawn(centro, ItemDisplay.class, x -> {
            x.setPersistent(false);
            x.setItemStack(c.voando());
            x.setBillboard(Display.Billboard.CENTER);
            x.setTeleportDuration(2);
            x.setInterpolationDuration(2);
            x.setBrightness(new Display.Brightness(15, 15));
            x.getPersistentDataContainer().set(K_PECA, PersistentDataType.BYTE, (byte) 1);
        });
        voos.add(new Voo(d, centro, c, System.currentTimeMillis() + segundos * 1000L));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void aoUsarGarrafa(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mao = e.getItem();
        if (mao == null) return;
        Cor solta = pote(mao);
        if (solta != null && p.isSneaking()) {
            e.setCancelled(true);
            mao.setAmount(mao.getAmount() - 1);
            p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE)).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
            voar(solta, p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.5)), 90);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
            return;
        }
        if (mao.getType() != Material.GLASS_BOTTLE || mao.hasItemMeta() && !mao.getItemMeta().getPersistentDataContainer().isEmpty()) return;
        Location olho = p.getEyeLocation();
        Vector dir = olho.getDirection();
        Voo pego = null;
        for (Voo v : voos) {
            if (!v.d.getWorld().equals(p.getWorld())) continue;
            Vector ate = v.d.getLocation().toVector().subtract(olho.toVector());
            double ao = ate.dot(dir);
            if (ao < 0 || ao > 4) continue;
            if (ate.clone().subtract(dir.clone().multiply(ao)).lengthSquared() < 0.5 * 0.5) { pego = v; break; }
        }
        if (pego == null) return;
        e.setCancelled(true);
        voos.remove(pego);
        pego.d.remove();
        mao.setAmount(mao.getAmount() - 1);
        p.getInventory().addItem(pego.cor.pote()).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
        p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 0.8f, 1.6f);
        p.sendActionBar(Component.text("Você pegou uma " + pego.cor.nome + "!", pego.cor.cor));
        plugin.titulos().registrar(p, "borboletas", 1);
    }

    // =====================================================================
    //  Comportamento: cervos fogem, javalis investem, pinguins pescam
    // =====================================================================

    private void comportamento() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long agora = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            for (Entity e : p.getNearbyEntities(14, 8, 14)) {
                Tipo t = tipo(e);
                if (t == null || !(e instanceof Mob m)) continue;
                double d2 = e.getLocation().distanceSquared(p.getLocation());
                if ((t == Tipo.CERVO || t == Tipo.CORCA) && (p.isSprinting() && d2 < 12 * 12 || !p.isSneaking() && d2 < 6 * 6)) fugir(m, p);
                if (t == Tipo.JAVALI && !alvoJavali.containsKey(e.getUniqueId()) && !p.isSneaking() && d2 < 4 * 4 && r.nextDouble() < 0.3) investir(m, p);
            }
        }
        for (Iterator<Map.Entry<UUID, UUID>> it = alvoJavali.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            Entity j = Bukkit.getEntity(en.getKey());
            Player alvo = Bukkit.getPlayer(en.getValue());
            if (!(j instanceof Mob m) || !j.isValid() || alvo == null || !alvo.getWorld().equals(j.getWorld()) || alvo.isDead()
                    || agora > investidaAte.getOrDefault(en.getKey(), 0L)) {
                investidaAte.remove(en.getKey());
                it.remove();
                continue;
            }
            m.getPathfinder().moveTo(alvo.getLocation(), 2.0);
            if (j.getLocation().distanceSquared(alvo.getLocation()) < 1.7 * 1.7 && agora - ultimoGolpe.getOrDefault(en.getKey(), 0L) > 1200) {
                ultimoGolpe.put(en.getKey(), agora);
                alvo.damage(4, j);
                Vector empurra = alvo.getLocation().toVector().subtract(j.getLocation().toVector()).setY(0);
                if (empurra.lengthSquared() > 0.01) alvo.setVelocity(empurra.normalize().multiply(0.9).setY(0.35));
                j.getWorld().playSound(j.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 0.8f, 1.3f);
            }
        }
        // Pinguins perto da água às vezes voltam com um peixe.
        if (ciclo % 30 == 0) {
            for (World w : Bukkit.getWorlds()) {
                if (!mundoValido(w)) continue;
                for (Chicken c : w.getEntitiesByClass(Chicken.class)) {
                    if (tipo(c) != Tipo.PINGUIM || r.nextDouble() > 0.12 || !perto(c.getLocation().getBlock(), Material.WATER, 3)) continue;
                    w.dropItemNaturally(c.getLocation(), new ItemStack(r.nextDouble() < 0.25 ? Material.SALMON : Material.COD));
                    w.spawnParticle(Particle.SPLASH, c.getLocation().add(0, 0.5, 0), 15, 0.3, 0.2, 0.3);
                    w.playSound(c.getLocation(), Sound.ENTITY_FISH_SWIM, 0.8f, 1.2f);
                }
            }
        }
        // Corujas vão embora de dia.
        if (ciclo % 10 == 0) {
            for (World w : Bukkit.getWorlds()) {
                if (!mundoValido(w) || !dia(w)) continue;
                for (Parrot pa : w.getEntitiesByClass(Parrot.class)) {
                    if (tipo(pa) != Tipo.CORUJA) continue;
                    w.spawnParticle(Particle.CLOUD, pa.getLocation(), 6, 0.2, 0.2, 0.2, 0.02);
                    pa.remove();
                }
            }
        }
    }

    private static boolean perto(Block b, Material m, int raio) {
        for (int dx = -raio; dx <= raio; dx++) for (int dz = -raio; dz <= raio; dz++) for (int dy = -1; dy <= 1; dy++) {
            if (b.getRelative(dx, dy, dz).getType() == m) return true;
        }
        return false;
    }

    private static void fugir(Mob m, Entity de) {
        Vector longe = m.getLocation().toVector().subtract(de.getLocation().toVector()).setY(0);
        if (longe.lengthSquared() < 0.01) longe = new Vector(1, 0, 0);
        Location alvo = m.getLocation().add(longe.normalize().multiply(12));
        m.getPathfinder().moveTo(alvo, 2.2);
    }

    private void investir(Mob javali, Player p) {
        alvoJavali.put(javali.getUniqueId(), p.getUniqueId());
        investidaAte.put(javali.getUniqueId(), System.currentTimeMillis() + 8000);
        javali.getWorld().playSound(javali.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1f, 1.2f);
        javali.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, javali.getLocation().add(0, 1, 0), 3, 0.3, 0.2, 0.3);
    }

    private void sons() {
        for (World w : Bukkit.getWorlds()) {
            if (!mundoValido(w) || dia(w)) continue;
            for (Parrot pa : w.getEntitiesByClass(Parrot.class)) {
                if (tipo(pa) != Tipo.CORUJA || ThreadLocalRandom.current().nextDouble() > 0.4) continue;
                // "Hu-hu": duas notas graves de flauta.
                w.playSound(pa.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.2f, 0.6f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (pa.isValid()) w.playSound(pa.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.2f, 0.53f);
                }, 8);
            }
        }
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoApanhar(EntityDamageByEntityEvent e) {
        Tipo t = tipo(e.getEntity());
        if (t == null) return;
        Entity quem = e.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Entity s ? s : e.getDamager();
        if (t == Tipo.TARTARUGA && quem instanceof Player p) {
            e.setCancelled(true);
            p.sendActionBar(Component.text("A tartaruga se encolhe no casco. Ela é velha demais para isso.", Tipo.TARTARUGA.cor));
            return;
        }
        if (!(e.getEntity() instanceof Mob m)) return;
        if ((t == Tipo.CERVO || t == Tipo.CORCA)) {
            // A manada inteira foge junto.
            for (Entity outro : m.getNearbyEntities(10, 4, 10)) {
                Tipo to = tipo(outro);
                if ((to == Tipo.CERVO || to == Tipo.CORCA) && outro instanceof Mob om) fugir(om, quem);
            }
            fugir(m, quem);
        }
        if (t == Tipo.JAVALI && quem instanceof Player p) investir(m, p);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (tipo(e.getEntity()) == Tipo.TARTARUGA && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) e.setCancelled(true);
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        Tipo t = tipo(e.getEntity());
        if (t == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> drops = e.getDrops();
        drops.clear();
        switch (t) {
            case CERVO -> {
                drops.add(Carne.CERVO.criar(2 + r.nextInt(2)));
                if (r.nextDouble() < 0.5) drops.add(new ItemStack(Material.LEATHER));
                if (r.nextDouble() < 0.35) drops.add(Raro.GALHADA.criar());
            }
            case CORCA -> {
                drops.add(Carne.CERVO.criar(1 + r.nextInt(2)));
                if (r.nextDouble() < 0.5) drops.add(new ItemStack(Material.LEATHER));
            }
            case JAVALI -> {
                drops.add(Carne.JAVALI.criar(1 + r.nextInt(3)));
                if (r.nextDouble() < 0.25) drops.add(Raro.PRESA_DE_JAVALI.criar());
            }
            case CORUJA -> { if (r.nextDouble() < 0.6) drops.add(Raro.PENA_DE_CORUJA.criar()); }
            case PINGUIM -> {
                drops.add(new ItemStack(Material.FEATHER, 1 + r.nextInt(2)));
                if (r.nextDouble() < 0.3) drops.add(new ItemStack(Material.COD));
            }
            case TARTARUGA -> drops.add(new ItemStack(Material.TURTLE_SCUTE, 3));
        }
        Player k = e.getEntity().getKiller();
        if (k != null && (t == Tipo.CERVO || t == Tipo.CORCA || t == Tipo.JAVALI)) {
            plugin.stats().darXp(k, Skill.COMBATE, 8);
            plugin.titulos().registrar(k, "caca", 1);
        }
        UUID id = e.getEntity().getUniqueId();
        ItemDisplay[] g = galhadas.remove(id);
        if (g != null) for (ItemDisplay d : g) if (d != null) d.remove();
        alvoJavali.remove(id);
        investidaAte.remove(id);
        ultimoGolpe.remove(id);
    }

    /** Cervos não se montam (ainda: a Doma vai ensinar); a tartaruga dá carona. */
    @EventHandler(ignoreCancelled = true)
    public void aoMontar(EntityMountEvent e) {
        Tipo t = tipo(e.getMount());
        if ((t == Tipo.CERVO || t == Tipo.CORCA) && e.getEntity() instanceof Player) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void aoDomar(EntityTameEvent e) {
        if (tipo(e.getEntity()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoClicar(PlayerInteractEntityEvent e) {
        Tipo t = tipo(e.getRightClicked());
        if (t == null) return;
        Player p = e.getPlayer();
        if (t == Tipo.CERVO || t == Tipo.CORCA || t == Tipo.CORUJA) {
            e.setCancelled(true);
            if (t != Tipo.CORUJA && e.getRightClicked() instanceof Mob m) fugir(m, p);
            return;
        }
        if (t == Tipo.TARTARUGA && e.getHand() == EquipmentSlot.HAND && e.getRightClicked().getPassengers().isEmpty()) {
            e.setCancelled(true);
            e.getRightClicked().addPassenger(p);
            p.sendActionBar(Component.text("Você subiu na Tartaruga-Gigante. Agache para descer.", Tipo.TARTARUGA.cor));
            plugin.titulos().registrar(p, "tartaruga", 1);
        }
    }

    public void desligar() {
        for (ItemDisplay[] g : galhadas.values()) for (ItemDisplay d : g) if (d != null) d.remove();
        galhadas.clear();
        for (Voo v : voos) v.d.remove();
        voos.clear();
    }

    /** Peças soltas de antes de um reinício (galhadas e borboletas não são salvas, mas por garantia). */
    @EventHandler
    public void aoCarregar(org.bukkit.event.world.EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) if (en.getPersistentDataContainer().has(K_PECA)) en.remove();
    }

    public static Tipo tipoPorNome(String s) {
        for (Tipo t : Tipo.values()) if (t.name().equalsIgnoreCase(s)) return t;
        return null;
    }
}
