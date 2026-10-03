package br.rpgatributos.lenda;

import br.rpgatributos.MarcadorDeBlocos;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Raridade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Illager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lendas e Feitos: o registro de quem tem cada lenda (uma por servidor), a Forja Lendária,
 * o despertar (a lenda fica mais forte com os abates feitos com ela) e os poderes.
 * Também conta alguns feitos novos (mortos-vivos, aranhas, troncos, minérios, jogadores...).
 */
public final class Lendas implements Listener {

    public static final TextColor COR = TextColor.color(0xFF5FD7);
    /** Contador especial: blocos corridos (vem da estatística do jogo). */
    public static final String CORRIDA = "blocos_corridos";
    private static final NamespacedKey K_LENDA = new NamespacedKey("rpgatributos", "lenda");
    private static final NamespacedKey K_VERSAO = new NamespacedKey("rpgatributos", "lenda_versao");
    private static final NamespacedKey K_ABATES = new NamespacedKey("rpgatributos", "lenda_abates");
    private static final NamespacedKey K_DONO = new NamespacedKey("rpgatributos", "lenda_dono");
    private static final NamespacedKey K_DATA = new NamespacedKey("rpgatributos", "lenda_data");
    private static final NamespacedKey K_FEITO = new NamespacedKey("rpgatributos", "lenda_feito");
    private static final NamespacedKey K_FLECHA = new NamespacedKey("rpgatributos", "lenda_flecha");
    private static final int[] DESPERTAR = {0, 100, 300, 700, 1500};
    private static final String[] ROMANO = {"I", "II", "III", "IV", "V"};
    private static final int FERRARIA = 50, NIVEIS_XP = 30, FRAGMENTOS = 6;

    /** Quem tem cada lenda. {@code versao} sobe se o dono forjar de novo (a cópia antiga apaga). */
    public record Registro(UUID dono, String nome, long data, int versao) {}

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<Lenda, Registro> registro = new EnumMap<>(Lenda.class);
    private final MarcadorDeBlocos colocados;
    private final Map<UUID, Long> recargaCoroa = new HashMap<>();
    private final Map<UUID, Long> recargaEgide = new HashMap<>();
    /** Evita que os blocos quebrados pelo poder (árvore, veio) disparem o poder de novo. */
    private boolean quebrando;

    public Lendas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "lendas.yml");
        this.colocados = new MarcadorDeBlocos(plugin, "colocados");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Registro
    // =====================================================================

    public Registro dono(Lenda l) {
        return registro.get(l);
    }

    public void carregar() {
        registro.clear();
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection raiz = y.getConfigurationSection("lendas");
        if (raiz == null) return;
        for (String id : raiz.getKeys(false)) {
            Lenda l = Lenda.porId(id);
            ConfigurationSection s = raiz.getConfigurationSection(id);
            if (l == null || s == null) continue;
            try {
                registro.put(l, new Registro(UUID.fromString(s.getString("dono", "")), s.getString("nome", "?"),
                        s.getLong("data"), s.getInt("versao", 1)));
            } catch (IllegalArgumentException ignorado) {
                // registro estragado
            }
        }
    }

    private void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<Lenda, Registro> en : registro.entrySet()) {
            String b = "lendas." + en.getKey().id() + ".";
            y.set(b + "dono", en.getValue().dono().toString());
            y.set(b + "nome", en.getValue().nome());
            y.set(b + "data", en.getValue().data());
            y.set(b + "versao", en.getValue().versao());
        }
        try {
            y.save(arquivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar lendas.yml: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  Feitos e requisitos
    // =====================================================================

    public long valor(Player p, String contador) {
        if (CORRIDA.equals(contador)) return p.getStatistic(Statistic.SPRINT_ONE_CM) / 100;
        return plugin.titulos().contador(p, contador);
    }

    /** O que falta para forjar essa lenda (lista vazia = pode). */
    public List<String> faltando(Player p, Lenda l) {
        List<String> f = new ArrayList<>();
        for (Lenda.Requisito r : l.requisitos()) if (valor(p, r.contador()) < r.qtd()) f.add(r.texto());
        if (plugin.stats().getNivel(p, Skill.FERRARIA) < FERRARIA) f.add("Ferraria nível " + FERRARIA);
        return f;
    }

    // ---------- feitos escondidos ----------

    private static final NamespacedKey K_REVELADAS = new NamespacedKey("rpgatributos", "lendas_reveladas");

    /** O jogador já leu a página dessa lenda (os feitos aparecem no livro)? */
    public boolean revelada(Player p, Lenda l) {
        String s = p.getPersistentDataContainer().getOrDefault(K_REVELADAS, PersistentDataType.STRING, "");
        return List.of(s.split(",")).contains(l.name());
    }

    /** Um feito aparece se a lenda foi revelada ou se o jogador já fez metade dele. */
    public boolean visivel(Player p, Lenda l, Lenda.Requisito r) {
        return revelada(p, l) || valor(p, r.contador()) * 2 >= r.qtd();
    }

    /** Revela os feitos de uma lenda que o jogador ainda não conhece. @return qual, ou null se já sabe todas. */
    public Lenda revelarAleatoria(Player p) {
        List<Lenda> faltam = new ArrayList<>();
        for (Lenda l : Lenda.values()) if (!revelada(p, l)) faltam.add(l);
        if (faltam.isEmpty()) return null;
        Lenda l = faltam.get(rnd().nextInt(faltam.size()));
        revelar(p, l);
        return l;
    }

    public void revelar(Player p, Lenda l) {
        if (revelada(p, l)) return;
        String s = p.getPersistentDataContainer().getOrDefault(K_REVELADAS, PersistentDataType.STRING, "");
        p.getPersistentDataContainer().set(K_REVELADAS, PersistentDataType.STRING, s.isEmpty() ? l.name() : s + "," + l.name());
    }

    public static String custo() {
        return "1 Essência Primordial, " + FRAGMENTOS + " Fragmentos de Forja e " + NIVEIS_XP + " níveis de XP";
    }

    // =====================================================================
    //  Forja Lendária
    // =====================================================================

    /** @return motivo de não ter forjado, ou null. */
    public String forjar(Player p, Lenda l) {
        Registro r = registro.get(l);
        if (r != null && !r.dono().equals(p.getUniqueId())) return l.nome() + " já pertence a " + r.nome() + ".";
        if (plugin.forjas().perto(p.getLocation(), 6).isEmpty()) return "Fique perto de uma Forja do Ferreiro para forjar uma lenda.";
        List<String> falta = faltando(p, l);
        if (!falta.isEmpty()) {
            return revelada(p, l) ? "Ainda falta: " + String.join(", ", falta) + "."
                    : "Seus feitos ainda não bastam para essa lenda. (Uma Página do Livro das Lendas revela o que ela pede.)";
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        Categoria cat = Categoria.de(mao.getType());
        if (cat == null || !l.categorias().contains(cat)) return "Segure na mão o item que vai virar a lenda (" + categorias(l) + ").";
        if (lenda(mao) != null) return "Esse item já é uma lenda.";
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            if (Raro.ESSENCIA_PRIMORDIAL.contar(p.getInventory()) < 1 || Raro.FRAGMENTO_DE_FORJA.contar(p.getInventory()) < FRAGMENTOS
                    || p.getLevel() < NIVEIS_XP) {
                return "A Forja Lendária pede " + custo() + ".";
            }
            Raro.ESSENCIA_PRIMORDIAL.tirar(p.getInventory(), 1);
            Raro.FRAGMENTO_DE_FORJA.tirar(p.getInventory(), FRAGMENTOS);
            p.setLevel(p.getLevel() - NIVEIS_XP);
        }
        criar(p, l, mao);
        return null;
    }

    /** Admin: transforma o item da mão na lenda, sem feitos, forja nem custo (tira de quem tinha). */
    public String forjarAdmin(Player p, Lenda l) {
        ItemStack mao = p.getInventory().getItemInMainHand();
        Categoria cat = Categoria.de(mao.getType());
        if (cat == null || !l.categorias().contains(cat)) return "Segure " + categorias(l) + ".";
        Registro r = registro.get(l);
        if (r != null && !r.dono().equals(p.getUniqueId())) registro.remove(l);
        criar(p, l, mao);
        return null;
    }

    private void criar(Player p, Lenda l, ItemStack mao) {
        Registro r = registro.get(l);
        int versao = r == null ? 1 : r.versao() + 1;
        long agora = System.currentTimeMillis();
        ItemStack item = plugin.forja().forjar(p, mao, Raridade.MITICO);
        item.editMeta(m -> {
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(K_LENDA, PersistentDataType.STRING, l.name());
            pdc.set(K_VERSAO, PersistentDataType.INTEGER, versao);
            pdc.set(K_ABATES, PersistentDataType.INTEGER, 0);
            pdc.set(K_DONO, PersistentDataType.STRING, p.getName());
            pdc.set(K_DATA, PersistentDataType.LONG, agora);
            pdc.set(K_FEITO, PersistentDataType.LONG, valor(p, l.contadorHistoria()));
        });
        // Registra antes de montar o item: a decoração confere se esta é a cópia válida.
        registro.put(l, new Registro(p.getUniqueId(), p.getName(), agora, versao));
        salvar();
        plugin.forja().reconstruir(item);
        p.getInventory().setItemInMainHand(item);
        plugin.titulos().registrar(p, "lendas", 1);

        World w = p.getWorld();
        w.strikeLightningEffect(p.getLocation());
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 120, 0.6, 1, 0.6, 0.5);
        w.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.6f);
        w.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 0.5f);
        p.showTitle(Title.title(Component.text("✦ " + l.nome() + " ✦", COR, TextDecoration.BOLD),
                Component.text("Uma lenda nasceu dos seus feitos", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(4000), Duration.ofMillis(1000))));
        plugin.getServer().broadcast(Component.text("✦ ", COR)
                .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" forjou a lenda ", NamedTextColor.GRAY))
                .append(Component.text(l.nome(), COR, TextDecoration.BOLD))
                .append(Component.text("! Ela é única no mundo.", NamedTextColor.GRAY)));
    }

    /** O dono abre mão da lenda: ela fica livre e a cópia dele se apaga. */
    public String renunciar(Player p, Lenda l) {
        Registro r = registro.get(l);
        if (r == null || !r.dono().equals(p.getUniqueId())) return "Essa lenda não é sua.";
        registro.remove(l);
        salvar();
        plugin.getServer().broadcast(Component.text("✦ A lenda ", COR).append(Component.text(l.nome(), COR, TextDecoration.BOLD))
                .append(Component.text(" está livre de novo.", COR)));
        return null;
    }

    public void liberarAdmin(Lenda l) {
        registro.remove(l);
        salvar();
    }

    private static String categorias(Lenda l) {
        List<String> n = new ArrayList<>();
        for (Categoria c : l.categorias()) n.add(c.nome().toLowerCase());
        return String.join(" ou ", n);
    }

    // =====================================================================
    //  O item
    // =====================================================================

    /** A lenda desse item, se ele for uma lenda válida (cópias antigas não valem). */
    public Lenda lenda(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Lenda l = Lenda.porId(pdc.getOrDefault(K_LENDA, PersistentDataType.STRING, ""));
        if (l == null) return null;
        Registro r = registro.get(l);
        return r != null && r.versao() == pdc.getOrDefault(K_VERSAO, PersistentDataType.INTEGER, 0) ? l : null;
    }

    /** Qualquer marca de lenda (até as apagadas): a reciclagem não aceita. */
    public static boolean marcado(ItemStack item) {
        return item != null && !item.isEmpty() && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(K_LENDA);
    }

    private static int nivelDespertar(int abates) {
        int n = 0;
        for (int i = 0; i < DESPERTAR.length; i++) if (abates >= DESPERTAR[i]) n = i;
        return n;
    }

    /** Força do poder conforme o despertar: 1,0 no I até 1,6 no V. */
    private static double forca(ItemStack item) {
        int abates = item.getItemMeta().getPersistentDataContainer().getOrDefault(K_ABATES, PersistentDataType.INTEGER, 0);
        return 1 + 0.15 * nivelDespertar(abates);
    }

    /**
     * Chamado pela forja depois de montar o item: se for uma lenda, troca o nome, o cabeçalho
     * (✦ ESPECIAL ✦) e acrescenta o poder, a história e o despertar.
     */
    public void decorar(ItemMeta meta, int refino) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Lenda l = Lenda.porId(pdc.getOrDefault(K_LENDA, PersistentDataType.STRING, ""));
        if (l == null) return;
        Registro r = registro.get(l);
        boolean valida = r != null && r.versao() == pdc.getOrDefault(K_VERSAO, PersistentDataType.INTEGER, 0);
        Component nome = Component.text(l.nome(), valida ? COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD);
        if (refino > 0) nome = nome.append(Component.text(" +" + refino, COR));
        meta.itemName(nome);
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        Component cabecalho = valida ? Component.text("✦ ESPECIAL ✦", COR, TextDecoration.BOLD)
                : Component.text("✖ LENDA APAGADA", NamedTextColor.DARK_GRAY, TextDecoration.BOLD);
        if (refino > 0) cabecalho = cabecalho.append(Component.text("  +" + refino, NamedTextColor.GOLD, TextDecoration.BOLD));
        if (lore.isEmpty()) lore.add(cabecalho);
        else lore.set(0, cabecalho);
        lore.add(Component.empty());
        if (valida) {
            lore.add(Component.text("✦ Poder da Lenda", COR, TextDecoration.BOLD));
            for (String linha : quebrar(l.poder(), 38)) lore.add(Component.text("  " + linha, NamedTextColor.LIGHT_PURPLE));
            int abates = pdc.getOrDefault(K_ABATES, PersistentDataType.INTEGER, 0);
            int n = nivelDespertar(abates);
            String prox = n + 1 < DESPERTAR.length ? " (" + abates + "/" + DESPERTAR[n + 1] + " abates)" : " (máximo)";
            lore.add(Component.text("  Despertar " + ROMANO[n] + prox, NamedTextColor.GOLD));
            lore.add(Component.empty());
        }
        String dono = pdc.getOrDefault(K_DONO, PersistentDataType.STRING, "?");
        long data = pdc.getOrDefault(K_DATA, PersistentDataType.LONG, 0L);
        long feito = pdc.getOrDefault(K_FEITO, PersistentDataType.LONG, 0L);
        lore.add(Component.text("Forjada por " + dono + " em " + new SimpleDateFormat("dd/MM/yyyy").format(new Date(data)) + ",", NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));
        lore.add(Component.text("que " + String.format(l.historia(), String.format("%,d", feito).replace(',', '.')) + ".", NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));
        lore.add(Component.text("Única no mundo.", COR).decorate(TextDecoration.ITALIC));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(valida);
    }

    private static List<String> quebrar(String texto, int largura) {
        List<String> l = new ArrayList<>();
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            if (linha.length() + palavra.length() + 1 > largura && !linha.isEmpty()) {
                l.add(linha.toString());
                linha.setLength(0);
            }
            if (!linha.isEmpty()) linha.append(' ');
            linha.append(palavra);
        }
        if (!linha.isEmpty()) l.add(linha.toString());
        return l;
    }

    // =====================================================================
    //  Ganchos para outros sistemas
    // =====================================================================

    private Lenda naMao(Player p) {
        return lenda(p.getInventory().getItemInMainHand());
    }

    private boolean veste(Player p, Lenda l) {
        for (ItemStack i : p.getInventory().getArmorContents()) if (lenda(i) == l) return true;
        return lenda(p.getInventory().getItemInOffHand()) == l;
    }

    /** Martelo do Forjador na mão: +10% de a forja sair uma raridade acima. */
    public double chanceRaridadeExtra(Player p) {
        return naMao(p) == Lenda.MARTELO_DO_FORJADOR ? 0.10 : 0;
    }

    /** Lâmina do Arquimago na mão: magias 30% mais baratas. */
    public double custoMagia(Player p) {
        return naMao(p) == Lenda.LAMINA_DO_ARQUIMAGO ? 0.7 : 1;
    }

    /** Efeitos passivos (botas). */
    public Map<PotionEffectType, Integer> efeitos(Player p) {
        Map<PotionEffectType, Integer> m = new HashMap<>();
        if (veste(p, Lenda.BOTAS_DO_ANDARILHO)) m.put(PotionEffectType.SPEED, 0);
        return m;
    }

    /** A cada 2 segundos: o Elmo das Feras fortalece os companheiros perto. */
    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!veste(p, Lenda.ELMO_DAS_FERAS)) continue;
            for (var r : plugin.companheiros().de(p.getUniqueId())) {
                if (Bukkit.getEntity(r.id()) instanceof LivingEntity c && c.getWorld().equals(p.getWorld())
                        && c.getLocation().distanceSquared(p.getLocation()) < 24 * 24) {
                    c.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false));
                    c.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
                }
            }
        }
    }

    // =====================================================================
    //  Poderes em combate
    // =====================================================================

    private static boolean mortoVivo(Entity e) {
        return e instanceof Zombie || e instanceof AbstractSkeleton || e instanceof Phantom || e instanceof Wither || e instanceof Zoglin;
    }

    @EventHandler(ignoreCancelled = true)
    public void aoAtirar(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p && lenda(e.getBow()) == Lenda.ARCO_DA_TEMPESTADE) {
            e.getProjectile().getPersistentDataContainer().set(K_FLECHA, PersistentDataType.DOUBLE, forca(e.getBow()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void aoDano(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity alvo) || alvo instanceof ArmorStand) return;
        // Flecha do Arco da Tempestade.
        if (e.getDamager() instanceof Projectile pr && pr.getPersistentDataContainer().has(K_FLECHA)) {
            double f = pr.getPersistentDataContainer().getOrDefault(K_FLECHA, PersistentDataType.DOUBLE, 1.0);
            if (rnd().nextDouble() < 0.25) {
                alvo.getWorld().strikeLightningEffect(alvo.getLocation());
                e.setDamage(e.getDamage() + 5 * f);
            }
        }
        if (e.getDamager() instanceof Player a && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            ItemStack arma = a.getInventory().getItemInMainHand();
            Lenda l = lenda(arma);
            if (l != null) {
                double f = forca(arma);
                switch (l) {
                    case LAMINA_DO_EXORCISTA -> {
                        if (mortoVivo(alvo)) {
                            e.setDamage(e.getDamage() * (1 + 0.6 * f));
                            alvo.setFireTicks(Math.max(alvo.getFireTicks(), 80));
                            alvo.getWorld().spawnParticle(Particle.END_ROD, alvo.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.05);
                        }
                    }
                    case PRESA_DA_TECELA -> {
                        alvo.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                        if (rnd().nextDouble() < 0.2 * f) {
                            alvo.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 4));
                            alvo.getWorld().spawnParticle(Particle.BLOCK, alvo.getLocation().add(0, 1, 0), 25, 0.3, 0.4, 0.3,
                                    Material.COBWEB.createBlockData());
                        }
                    }
                    case LAMINA_DO_CARRASCO -> {
                        AttributeInstance max = alvo.getAttribute(Attribute.MAX_HEALTH);
                        if (max != null && alvo.getHealth() < max.getValue() * 0.2) {
                            e.setDamage(e.getDamage() * (1 + f));
                            alvo.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, alvo.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.1);
                        }
                    }
                    case MARTELO_DO_FORJADOR -> {
                        if (rnd().nextDouble() < 0.1 * f) {
                            alvo.getWorld().strikeLightningEffect(alvo.getLocation());
                            e.setDamage(e.getDamage() + 6 * f);
                        }
                    }
                    case LAMINA_DO_ARQUIMAGO -> plugin.arcano().darMana(a, 3 * f);
                    default -> { }
                }
            }
            if (Chefes.ehChefe(alvo) && veste(a, Lenda.COROA_DO_FIM)) e.setDamage(e.getDamage() * 1.25);
        }
        // Quem está apanhando.
        if (alvo instanceof Player v) {
            long agora = System.currentTimeMillis();
            if (veste(v, Lenda.EGIDE_DO_GUARDIAO)) {
                if (rnd().nextDouble() < 0.15 && e.getDamager() instanceof LivingEntity atacante && atacante != v) {
                    atacante.damage(e.getDamage() * 0.5);
                    v.getWorld().spawnParticle(Particle.ENCHANTED_HIT, v.getLocation().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.1);
                }
                if (agora > recargaEgide.getOrDefault(v.getUniqueId(), 0L)) {
                    recargaEgide.put(v.getUniqueId(), agora + 30_000);
                    v.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                }
            }
            if (veste(v, Lenda.COROA_DO_FIM) && agora > recargaCoroa.getOrDefault(v.getUniqueId(), 0L)) {
                AttributeInstance max = v.getAttribute(Attribute.MAX_HEALTH);
                if (max != null && v.getHealth() - e.getFinalDamage() < max.getValue() * 0.3) {
                    recargaCoroa.put(v.getUniqueId(), agora + 60_000);
                    v.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1));
                    v.getWorld().spawnParticle(Particle.REVERSE_PORTAL, v.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCair(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p && veste(p, Lenda.BOTAS_DO_ANDARILHO)) {
            e.setCancelled(true);
        }
    }

    /** Feitos novos e o despertar da lenda que matou. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || p == morto) return;
        String feito = null;
        if (morto instanceof Player) feito = "abates_jogadores";
        else if (mortoVivo(morto)) feito = "mortos_vivos";
        else if (morto instanceof Spider || morto instanceof CaveSpider || morto instanceof Silverfish) feito = "aranhas";
        else if (morto instanceof Illager || morto instanceof Ravager || morto instanceof Vex || morto instanceof Witch) feito = "illagers";
        if (feito != null) plugin.titulos().registrar(p, feito, 1);

        ItemStack arma = p.getInventory().getItemInMainHand();
        Lenda l = lenda(arma);
        if (l == null || Companheiros.eh(morto)) return;
        if (l == Lenda.LAMINA_DO_EXORCISTA && mortoVivo(morto)) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            p.setHealth(Math.min(max == null ? 20 : max.getValue(), p.getHealth() + 2));
        }
        int antes = arma.getItemMeta().getPersistentDataContainer().getOrDefault(K_ABATES, PersistentDataType.INTEGER, 0);
        int depois = antes + 1;
        arma.editMeta(m -> m.getPersistentDataContainer().set(K_ABATES, PersistentDataType.INTEGER, depois));
        if (nivelDespertar(depois) > nivelDespertar(antes) || depois % 25 == 0) {
            plugin.forja().reconstruir(arma);
            if (nivelDespertar(depois) > nivelDespertar(antes)) {
                p.showTitle(Title.title(Component.empty(), Component.text("✦ " + l.nome() + " despertou: " + ROMANO[nivelDespertar(depois)] + " ✦", COR),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
            }
        }
        p.getInventory().setItemInMainHand(arma);
    }

    // =====================================================================
    //  Árvore inteira e veio de minério
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoQuebrar(BlockBreakEvent e) {
        if (quebrando) return;
        Player p = e.getPlayer();
        Lenda l = naMao(p);
        if (l == null) return;
        Block b = e.getBlock();
        if (l == Lenda.MACHADO_DO_LENHADOR && !p.isSneaking() && Tag.LOGS.isTagged(b.getType())) {
            List<Block> troncos = conectados(b, Tag.LOGS, 64, true);
            if (folhasNaturais(troncos) >= 4) quebrarTodos(p, troncos);
        } else if (l == Lenda.PICARETA_DO_ABISMO && p.isSneaking() && minerio(b.getType())) {
            quebrarTodos(p, conectados(b, null, 24, false));
        }
    }

    private static boolean minerio(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    /** Blocos do mesmo tipo (troncos: qualquer tronco) encostados, sem os colocados por jogadores. */
    private List<Block> conectados(Block inicio, Tag<Material> tag, int max, boolean diagonais) {
        List<Block> achados = new ArrayList<>();
        Set<Block> vistos = new HashSet<>();
        Deque<Block> fila = new ArrayDeque<>();
        fila.add(inicio);
        vistos.add(inicio);
        Material tipo = inicio.getType();
        while (!fila.isEmpty() && achados.size() < max) {
            Block b = fila.poll();
            if (b != inicio) achados.add(b);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (!diagonais && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                        Block v = b.getRelative(dx, dy, dz);
                        if (vistos.contains(v)) continue;
                        boolean igual = tag != null ? tag.isTagged(v.getType()) : v.getType() == tipo;
                        if (!igual || colocados.marcado(v)) continue;
                        vistos.add(v);
                        fila.add(v);
                    }
                }
            }
        }
        return achados;
    }

    /** Folhas naturais (não colocadas) perto dos troncos: é uma árvore, não uma casa. */
    private static int folhasNaturais(List<Block> troncos) {
        int n = 0;
        for (Block t : troncos) {
            for (Block v : new Block[]{t.getRelative(1, 0, 0), t.getRelative(-1, 0, 0), t.getRelative(0, 1, 0),
                    t.getRelative(0, 0, 1), t.getRelative(0, 0, -1)}) {
                if (v.getBlockData() instanceof Leaves f && !f.isPersistent()) n++;
            }
        }
        return n;
    }

    private void quebrarTodos(Player p, List<Block> blocos) {
        quebrando = true;
        try {
            for (Block b : blocos) {
                if (!plugin.territorios().podeConstruir(p, b) || plugin.masmorras().ehMundo(b.getWorld())) continue;
                ItemStack ferramenta = p.getInventory().getItemInMainHand();
                if (lenda(ferramenta) == null) break; // a ferramenta quebrou (ou saiu da mão)
                Location c = b.getLocation().add(0.5, 0.5, 0.5);
                b.breakNaturally(ferramenta, true);
                if (p.getGameMode() != GameMode.CREATIVE) p.damageItemStack(org.bukkit.inventory.EquipmentSlot.HAND, 1);
                b.getWorld().spawnParticle(Particle.CRIT, c, 2, 0.2, 0.2, 0.2, 0.02);
            }
        } finally {
            quebrando = false;
        }
    }
}
