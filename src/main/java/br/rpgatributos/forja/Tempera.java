package br.rpgatributos.forja;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.mundo.NaturezaViva;
import br.rpgatributos.vida.DadosBloco;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A Tina de Têmpera (3.19): um caldeirão onde se jogou 1 balde de água e 1 lingote de ferro. A peça
 * que sai da Forja do Ferreiro sai "em brasa" por 10 segundos: clique na tina com ela na mão e ela
 * ganha a têmpera do líquido da tina. Esfriou fora da tina: fica normal (não perde nada).
 * <ul>
 *   <li>Água (balde de água): +10% de durabilidade.</li>
 *   <li>Óleo (um óleo de lâmina da Alquimia): arma +4% de dano; armadura −1,5% de dano recebido por peça.</li>
 *   <li>Almas (Essência Etérea): arma +15% de dano em mortos-vivos e nas criaturas do Mundo das Almas.</li>
 *   <li>Seiva (Seiva Anciã): a peça se conserta sozinha devagar (1 ponto a cada 20 s, vestida ou na mão).</li>
 *   <li>Abismo (Cristal Abissal): arma +8% de dano no escuro; armadura −2% de dano recebido no escuro.</li>
 * </ul>
 * Cada líquido dá 8 têmperas.
 */
public final class Tempera extends Estacao {

    public enum Liquido {
        AGUA("Água", 0x4FA3D1, "+10% de durabilidade"),
        OLEO("Óleo", 0xE8C15A, "arma +4% de dano; armadura −1,5% de dano recebido"),
        ALMAS("Almas", 0x7FD8E8, "+15% de dano em mortos-vivos e criaturas das Almas"),
        SEIVA("Seiva", 0xE08A2E, "conserta sozinha devagar"),
        ABISMO("Abismo", 0x8A5AE0, "+8% de dano no escuro; armadura −2% no escuro");

        final String nome, efeito;
        final TextColor cor;

        Liquido(String nome, int cor, String efeito) {
            this.nome = nome;
            this.cor = TextColor.color(cor);
            this.efeito = efeito;
        }

        public String nome() { return nome; }
    }

    public static final TextColor COR = TextColor.color(0x4FA3D1);
    private static final String DADO = "tina";
    private static final int CARGAS = 8;
    private static final long BRASA_MS = 10_000L;
    private static final NamespacedKey K_BRASA = new NamespacedKey("rpgatributos", "em_brasa");
    private static final NamespacedKey K_TEMPERA = new NamespacedKey("rpgatributos", "tempera");
    private int ciclo;

    public Tempera(RPGAtributos plugin) {
        super(plugin, "tinas", "tina_display");
    }

    // =====================================================================
    //  Em brasa
    // =====================================================================

    /** A peça saiu da Forja: fica em brasa por 10 s. */
    public static void emBrasa(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return;
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(K_BRASA, PersistentDataType.LONG, System.currentTimeMillis() + BRASA_MS);
            List<Component> lore = new ArrayList<>(m.hasLore() ? m.lore() : List.of());
            lore.addFirst(Component.text("🔥 Em brasa: leve à Tina de Têmpera (10 s)", TextColor.color(0xFF7043)).decoration(TextDecoration.ITALIC, false));
            m.lore(lore);
        });
    }

    private static boolean emBrasaAgora(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return false;
        Long ate = i.getPersistentDataContainer().get(K_BRASA, PersistentDataType.LONG);
        return ate != null && ate > System.currentTimeMillis();
    }

    private static void esfriar(ItemStack i) {
        i.editMeta(m -> {
            m.getPersistentDataContainer().remove(K_BRASA);
            if (!m.hasLore()) return;
            List<Component> lore = new ArrayList<>(m.lore());
            lore.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c).startsWith("🔥 Em brasa"));
            m.lore(lore);
        });
    }

    public static Liquido tempera(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        String s = i.getPersistentDataContainer().get(K_TEMPERA, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Liquido.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void temperar(ItemStack i, Liquido l) {
        esfriar(i);
        i.editMeta(m -> {
            m.getPersistentDataContainer().set(K_TEMPERA, PersistentDataType.STRING, l.name());
            if (l == Liquido.AGUA && m instanceof Damageable d) {
                int max = d.hasMaxDamage() ? d.getMaxDamage() : i.getType().getMaxDurability();
                if (max > 0) d.setMaxDamage((int) Math.round(max * 1.1));
            }
            List<Component> lore = new ArrayList<>(m.hasLore() ? m.lore() : List.of());
            lore.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c).startsWith("♨ Têmpera"));
            lore.addFirst(Component.text("♨ Têmpera de " + l.nome + ": " + l.efeito, l.cor).decoration(TextDecoration.ITALIC, false));
            m.lore(lore);
        });
    }

    // =====================================================================
    //  A estação
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.CAULDRON || m == Material.WATER_CAULDRON;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.WATER_BUCKET || m == Material.IRON_INGOT;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block b = item.getLocation().getBlock();
        if (blocoValido(b.getType())) return b;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return blocoValido(abaixo.getType()) ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.WATER_BUCKET) < 1 || contar(itens, Material.IRON_INGOT) < 1) return false;
        tirar(itens, Material.WATER_BUCKET, 1);
        tirar(itens, Material.IRON_INGOT, 1);
        bloco.getWorld().dropItemNaturally(bloco.getLocation().add(0.5, 1.2, 0.5), new ItemStack(Material.BUCKET));
        DadosBloco.gravar(DADO, bloco.getLocation(), Liquido.AGUA.name() + "|" + CARGAS);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.WATER_BUCKET);
    }

    @Override
    protected float escalaItem() {
        return 0.35f;
    }

    @Override
    protected double alturaItem() {
        return 1.4;
    }

    @Override
    protected Component nome() {
        return Component.text("♨ Tina de Têmpera", COR).append(Component.newline())
                .append(Component.text("clique com a peça em brasa", NamedTextColor.GRAY));
    }

    @Override
    protected Component nome(Location bloco) {
        String[] d = dados(bloco);
        if (d == null) return Component.text("♨ Tina de Têmpera", COR).append(Component.newline()).append(Component.text("vazia: encha com um líquido", NamedTextColor.GRAY));
        Liquido l = Liquido.valueOf(d[0]);
        return Component.text("♨ Tina de Têmpera", COR).append(Component.newline())
                .append(Component.text(l.nome + " · " + d[1] + " têmpera(s)", l.cor));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        if (b.getType() == Material.CAULDRON) b.setType(Material.WATER_CAULDRON);
        if (b.getBlockData() instanceof org.bukkit.block.data.Levelled lv) {
            lv.setLevel(lv.getMaximumLevel());
            b.setBlockData(lv);
        }
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.SPLASH, c, 30, 0.3, 0.1, 0.3);
        c.getWorld().playSound(c, Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("♨ Tina de Têmpera pronta (Água, 8 têmperas). Peças da Forja saem em brasa: clique aqui com elas em até 10 s.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 12 == 0) centro.getWorld().spawnParticle(Particle.CLOUD, centro.clone().subtract(0, 0.6, 0), 1, 0.15, 0.02, 0.15, 0.005);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.IRON_INGOT));
    }

    @Override
    protected void aoRemover(Location bloco) {
        DadosBloco.apagar(DADO, bloco);
    }

    /** [líquido, cargas] ou null. */
    private static String[] dados(Location l) {
        String s = DadosBloco.ler(DADO, l);
        if (s == null) return null;
        String[] d = s.split("\\|");
        if (d.length != 2) return null;
        try {
            Liquido.valueOf(d[0]);
            if (Integer.parseInt(d[1]) <= 0) return null;
            return d;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** O líquido que este item põe na tina (null se não serve). */
    private static Liquido liquidoDe(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        if (i.getType() == Material.WATER_BUCKET && !i.hasItemMeta()) return Liquido.AGUA;
        if (NaturezaViva.ehSeiva(i)) return Liquido.SEIVA;
        Raro r = Raro.de(i);
        if (r == Raro.ESSENCIA_ETEREA) return Liquido.ALMAS;
        if (r == Raro.CRISTAL_ABISSAL) return Liquido.ABISMO;
        Reagente re = Reagente.de(i);
        if (re != null && re.name().startsWith("OLEO_") && re != Reagente.OLEO_DE_PEIXE) return Liquido.OLEO;
        return null;
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        ItemStack mao = p.getInventory().getItemInMainHand();
        String[] d = dados(l);
        if (emBrasaAgora(mao)) {
            if (d == null) {
                p.sendActionBar(Component.text("✖ A tina está vazia. Encha com água, óleo, Essência Etérea, Seiva Anciã ou Cristal Abissal.", NamedTextColor.RED));
                return;
            }
            Liquido liq = Liquido.valueOf(d[0]);
            temperar(mao, liq);
            int resto = Integer.parseInt(d[1]) - 1;
            if (resto <= 0) DadosBloco.apagar(DADO, l);
            else DadosBloco.gravar(DADO, l, liq.name() + "|" + resto);
            atualizarDisplays(l);
            Location c = l.clone().add(0.5, 1, 0.5);
            c.getWorld().spawnParticle(Particle.CLOUD, c, 25, 0.3, 0.3, 0.3, 0.05);
            c.getWorld().playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
            c.getWorld().playSound(c, Sound.BLOCK_LAVA_EXTINGUISH, 0.7f, 1.5f);
            p.sendActionBar(Component.text("♨ Temperada em " + liq.nome + ": " + liq.efeito, liq.cor));
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.FERRARIA, 20);
            plugin.titulos().registrar(p, "temperas", 1);
            return;
        }
        Liquido novo = liquidoDe(mao);
        if (novo != null) {
            if (p.getGameMode() != GameMode.CREATIVE) {
                if (novo == Liquido.AGUA) p.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                else {
                    boolean garrafa = mao.getType() == Material.HONEY_BOTTLE || mao.getType() == Material.EXPERIENCE_BOTTLE
                            || mao.getType() == Material.DRAGON_BREATH || mao.getType() == Material.OMINOUS_BOTTLE;
                    mao.setAmount(mao.getAmount() - 1);
                    if (garrafa) p.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE)).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
                }
            }
            DadosBloco.gravar(DADO, l, novo.name() + "|" + CARGAS);
            atualizarDisplays(l);
            p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1.1f);
            p.sendActionBar(Component.text("♨ Tina com " + novo.nome + " (" + CARGAS + " têmperas): " + novo.efeito, novo.cor));
            return;
        }
        if (d == null) {
            p.sendActionBar(Component.text("♨ Vazia: encha com balde de água, óleo de lâmina, Essência Etérea, Seiva Anciã ou Cristal Abissal.", NamedTextColor.GRAY));
        } else {
            Liquido liq = Liquido.valueOf(d[0]);
            p.sendActionBar(Component.text("♨ " + liq.nome + " · " + d[1] + " têmpera(s). Peça da Forja em brasa: clique com ela na mão.", liq.cor));
        }
    }

    // =====================================================================
    //  A cada segundo: brasa que esfriou e a Seiva consertando
    // =====================================================================

    @Override
    protected void aoTick() {
        if (++ciclo % 4 != 0) return; // a estação anda a cada 5 ticks: isto é ~1 s
        boolean seiva = ciclo % 80 == 0; // ~20 s
        long agora = System.currentTimeMillis();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            for (ItemStack i : p.getInventory().getContents()) {
                if (i == null || i.isEmpty() || !i.hasItemMeta()) continue;
                Long ate = i.getPersistentDataContainer().get(K_BRASA, PersistentDataType.LONG);
                if (ate != null && ate <= agora) esfriar(i);
            }
            if (!seiva) continue;
            List<ItemStack> usados = new ArrayList<>(List.of(p.getInventory().getArmorContents()));
            usados.add(p.getInventory().getItemInMainHand());
            for (ItemStack i : usados) {
                if (tempera(i) != Liquido.SEIVA || !(i.getItemMeta() instanceof Damageable dm) || !dm.hasDamage()) continue;
                i.editMeta(Damageable.class, x -> x.setDamage(Math.max(0, x.getDamage() - 1)));
            }
        }
    }

    // =====================================================================
    //  Os efeitos
    // =====================================================================

    private static boolean escuro(Player p) {
        return p.getEyeLocation().getBlock().getLightLevel() < 7;
    }

    private static boolean mortoVivo(Entity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof Phantom || e instanceof Wither || e instanceof Zoglin
                || e.getWorld().getName().toLowerCase(Locale.ROOT).contains("almas");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();
        Player p = d instanceof Player pl ? pl : d instanceof Projectile pr && pr.getShooter() instanceof Player pl ? pl : null;
        if (p == null || !(e.getEntity() instanceof LivingEntity alvo)) return;
        Liquido l = tempera(p.getInventory().getItemInMainHand());
        if (l == null) return;
        switch (l) {
            case OLEO -> e.setDamage(e.getDamage() * 1.04);
            case ALMAS -> { if (mortoVivo(alvo)) e.setDamage(e.getDamage() * 1.15); }
            case ABISMO -> { if (escuro(p)) e.setDamage(e.getDamage() * 1.08); }
            default -> { }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoApanhar(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(e instanceof EntityDamageByEntityEvent)) return;
        double mult = 1;
        boolean escuro = escuro(p);
        for (ItemStack i : p.getInventory().getArmorContents()) {
            Liquido l = tempera(i);
            if (l == Liquido.OLEO) mult -= 0.015;
            else if (l == Liquido.ABISMO && escuro) mult -= 0.02;
        }
        if (mult < 1) e.setDamage(e.getDamage() * mult);
    }
}
