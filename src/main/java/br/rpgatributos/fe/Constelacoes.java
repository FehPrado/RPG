package br.rpgatributos.fe;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * As Constelações que assistem (referência: Omniscient Reader's Viewpoint). Os seis deuses do
 * servidor acompanham os feitos dos jogadores: quando algo marcante acontece, um deles comenta no
 * chat e manda Moedas Estelares. Feitos com a vida baixa chamam mais atenção. As moedas compram
 * bênçãos no /constelacoes, e o deus que mais se interessa por você cobra menos.
 */
public final class Constelacoes implements Listener, CommandExecutor {

    private static final NamespacedKey K_MOEDAS = new NamespacedKey("rpgatributos", "moedas_estelares");
    private static final NamespacedKey K_INTERESSE = new NamespacedKey("rpgatributos", "interesse_constelacoes");
    private static final NamespacedKey K_SEGUNDA = new NamespacedKey("rpgatributos", "segunda_chance");
    private static final TextColor COR = TextColor.color(0xB9C7FF);
    /** Uma constelação não fala com o mesmo jogador mais de uma vez nesse tempo (evita enxurrada). */
    private static final long SILENCIO_MS = 45_000;

    /** Um feito que os deuses notam: quem nota, a chance, quantas moedas e o que dizem. */
    private record Feito(Deus deus, double chance, int moedas, String[] falas) { }

    private static final Map<String, Feito> FEITOS = new HashMap<>();

    private static void f(String id, Deus d, double chance, int moedas, String... falas) {
        FEITOS.put(id, new Feito(d, chance, moedas, falas));
    }

    static {
        String[] guerra = {"aplaude o seu golpe.", "bate o escudo em aprovação.", "quer ver mais sangue nessa história."};
        f("chefes", Deus.BELLUM, 1, 5, guerra);
        f("chefe_mundial", Deus.BELLUM, 1, 8, "se levanta do trono para olhar você.", "grita o seu nome para as outras estrelas.");
        f("elites", Deus.BELLUM, 0.35, 2, guerra);
        f("golpes_perfeitos", Deus.BELLUM, 0.08, 1, "sorri com a precisão do seu golpe.");
        f("finalizadores", Deus.BELLUM, 0.2, 1, guerra);
        f("hordas", Deus.BELLUM, 1, 4, "gostou de ver a colônia de pé.");
        f("guerras_vencidas", Deus.BELLUM, 1, 6, "declara este reino digno.");
        f("masmorras", Deus.BELLUM, 0.6, 3, "anota o seu nome na lista dos que voltaram.");
        f("nian_espantado", Deus.BELLUM, 1, 4, "ri alto vendo a fera fugir.");
        f("nucleos", Deus.FERRUM, 0.6, 2, "quer saber o que você vai forjar com isso.");
        f("gema_perfeita", Deus.FERRUM, 1, 3, "admira o brilho da gema.", "diz que nem ele lapidaria melhor.");
        f("mitrilo", Deus.FERRUM, 0.15, 1, "ouve o mitrilo cantar na sua picareta.");
        f("meteoritos", Deus.FERRUM, 1, 2, "reconhece metal que caiu do céu.");
        f("animais_raros", Deus.SYLVA, 1, 2, "se encanta com a criatura que você achou.");
        f("mutacoes", Deus.SYLVA, 0.5, 2, "vê a semente mudar nas suas mãos.");
        f("colheitas_estacao", Deus.SYLVA, 0.1, 1, "gosta de quem planta no tempo certo.");
        f("besta_errante", Deus.SYLVA, 1, 6, "não acredita que a besta aceitou você.");
        f("companheiro_lendario", Deus.SYLVA, 1, 3, "abençoa o laço entre você e o seu companheiro.");
        f("raizes", Deus.SYLVA, 1, 2, "vê a luz voltar às raízes do mundo.");
        f("especies_peixe", Deus.MARIS, 0.5, 1, "conta mais um peixe na sua coleção.");
        f("criaturas_marinhas", Deus.MARIS, 0.5, 2, "olha das ondas para o que você pescou.");
        f("tesouros_enterrados", Deus.MARIS, 1, 2, "sabia onde estava o tesouro, mas não contou.");
        f("cidades_despertas", Deus.MARIS, 1, 4, "chora ao ver a cidade afundada acordar.");
        f("reacoes", Deus.ARCANUS, 0.1, 1, "anota a reação no grimório dele.");
        f("magias", Deus.ARCANUS, 0.6, 2, "nunca tinha visto essa combinação.");
        f("runas", Deus.ARCANUS, 0.6, 2, "lê a runa por cima do seu ombro.");
        f("transmutacoes", Deus.ARCANUS, 0.4, 2, "aprova a troca equivalente.");
        f("segredos", Deus.ARCANUS, 1, 3, "sabia que você ia achar.");
        f("sonhos", Deus.ARCANUS, 0.3, 1, "estava no seu sonho também.");
        f("sacerdotes", Deus.MORTIS, 1, 4, "recebe mais uma alma no salão dela.");
        f("selos", Deus.MORTIS, 1, 3, "abre os olhos quando o selo cai.");
        f("lendas", Deus.MORTIS, 1, 5, "escreve o seu nome entre os que não serão esquecidos.");
        f("classe_lendaria", Deus.MORTIS, 1, 6, "diz que agora a sua história tem um preço.");
        f("renascimentos", Deus.MORTIS, 1, 6, "devolve a sua alma, mais forte.");
        f("locais", Deus.MORTIS, 1, 3, "conhece quem guardava esse lugar.");
    }

    /** O que se compra com as moedas: uma bênção de cada deus. */
    private enum Bencao {
        FURIA(Deus.BELLUM, "Fúria do Espetáculo", "Força I por 5 min", 10, Material.IRON_SWORD),
        FORJA(Deus.FERRUM, "Mãos da Forja", "Pressa II por 10 min", 8, Material.IRON_PICKAXE),
        BOSQUE(Deus.SYLVA, "Bênção do Bosque", "Regeneração I por 3 min", 10, Material.OAK_SAPLING),
        MARES(Deus.MARIS, "Graça das Marés", "Respirar e nadar rápido por 10 min", 6, Material.NAUTILUS_SHELL),
        CEU(Deus.ARCANUS, "Céu Estrelado", "Recupera 80 de mana", 6, Material.AMETHYST_SHARD),
        SEGUNDA(Deus.MORTIS, "Segunda Chance", "O próximo golpe mortal te deixa com meio coração", 25, Material.TOTEM_OF_UNDYING);

        final Deus deus;
        final String nome, efeito;
        final int custo;
        final Material icone;

        Bencao(Deus deus, String nome, String efeito, int custo, Material icone) {
            this.deus = deus;
            this.nome = nome;
            this.efeito = efeito;
            this.custo = custo;
            this.icone = icone;
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Map<Deus, Long>> falou = new HashMap<>();
    private final Map<UUID, Long> quaseMorte = new HashMap<>();

    public Constelacoes(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Moedas e interesse (no próprio jogador)
    // =====================================================================

    public int moedas(Player p) {
        return p.getPersistentDataContainer().getOrDefault(K_MOEDAS, PersistentDataType.INTEGER, 0);
    }

    private void darMoedas(Player p, int n) {
        p.getPersistentDataContainer().set(K_MOEDAS, PersistentDataType.INTEGER, Math.max(0, moedas(p) + n));
    }

    private int[] interesse(Player p) {
        int[] v = new int[Deus.values().length];
        String s = p.getPersistentDataContainer().get(K_INTERESSE, PersistentDataType.STRING);
        if (s == null) return v;
        String[] partes = s.split(",");
        for (int i = 0; i < Math.min(v.length, partes.length); i++) {
            try {
                v[i] = Integer.parseInt(partes[i].trim());
            } catch (NumberFormatException ignorado) {
                // valor estragado: fica 0
            }
        }
        return v;
    }

    private void somarInteresse(Player p, Deus d, int n) {
        int[] v = interesse(p);
        v[d.ordinal()] += n;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "," : "").append(v[i]);
        p.getPersistentDataContainer().set(K_INTERESSE, PersistentDataType.STRING, sb.toString());
    }

    /** O deus que mais acompanha o jogador (ou null se nenhum ainda). */
    private Deus favorito(Player p) {
        int[] v = interesse(p);
        int melhor = -1;
        for (int i = 0; i < v.length; i++) if (v[i] > 0 && (melhor < 0 || v[i] > v[melhor])) melhor = i;
        return melhor < 0 ? null : Deus.values()[melhor];
    }

    // =====================================================================
    //  Os deuses notam
    // =====================================================================

    /** Chamado pelos Títulos a cada feito contado. */
    public void feito(Player p, String id) {
        if (!plugin.settings().constelacoesAtivadas) return;
        Feito f = FEITOS.get(id);
        if (f == null && id.startsWith("chefe_")) f = FEITOS.get("chefes");
        if (f == null) return;
        boolean dramatico = vidaBaixa(p);
        double chance = Math.min(1, f.chance() * (dramatico ? 2 : 1));
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;
        comentar(p, f.deus(), f.falas()[ThreadLocalRandom.current().nextInt(f.falas().length)], f.moedas() * (dramatico ? 2 : 1), dramatico);
    }

    private static boolean vidaBaixa(Player p) {
        var max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        return max != null && p.getHealth() <= max.getValue() * 0.3;
    }

    private void comentar(Player p, Deus d, String fala, int moedas, boolean dramatico) {
        long agora = System.currentTimeMillis();
        Map<Deus, Long> m = falou.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        boolean silencio = agora - m.getOrDefault(d, 0L) < SILENCIO_MS;
        darMoedas(p, moedas);
        somarInteresse(p, d, moedas);
        if (silencio) return; // as moedas chegam, mas sem repetir a mensagem
        m.put(d, agora);
        Component linha = Component.text("[" + d.nome() + ", " + d.titulo() + "] ", d.cor(), TextDecoration.BOLD)
                .append(Component.text(fala, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
        p.sendMessage(linha);
        p.sendMessage(Component.text("  ✧ " + moedas + " Moeda" + (moedas > 1 ? "s" : "") + " Estelar" + (moedas > 1 ? "es" : "")
                + (dramatico ? " (o perigo deixou a cena mais bonita)" : "") + " · /constelacoes", COR));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
    }

    /** Sobreviver por um fio chama a Deusa da Morte (uma vez a cada 5 min). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!plugin.settings().constelacoesAtivadas || !(e.getEntity() instanceof Player p)) return;
        double depois = p.getHealth() - e.getFinalDamage();
        // Segunda Chance: o golpe mortal deixa com meio coração.
        if (depois <= 0 && p.getPersistentDataContainer().has(K_SEGUNDA)) {
            p.getPersistentDataContainer().remove(K_SEGUNDA);
            e.setDamage(0);
            p.setHealth(1);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.4f);
            p.sendMessage(Component.text("[Mortis, a Deusa da Morte] ", Deus.MORTIS.cor(), TextDecoration.BOLD)
                    .append(Component.text("ainda não é a sua hora.", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)));
            return;
        }
        if (depois <= 0 || depois > 3) return;
        long agora = System.currentTimeMillis();
        if (agora - quaseMorte.getOrDefault(p.getUniqueId(), 0L) < 300_000) return;
        quaseMorte.put(p.getUniqueId(), agora);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && !p.isDead()) comentar(p, Deus.MORTIS, "estende a mão... e decide esperar mais um pouco.", 2, true);
        });
    }

    /** Matar um monstro forte com a vida baixa: a Guerra adora. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMatar(EntityDeathEvent e) {
        if (!plugin.settings().constelacoesAtivadas || !(e.getEntity().getKiller() instanceof Player p) || !(e.getEntity() instanceof Enemy)) return;
        var max = e.getEntity().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (max == null || max.getValue() < 40 || !vidaBaixa(p) || ThreadLocalRandom.current().nextDouble() >= 0.5) return;
        comentar(p, Deus.BELLUM, "não esperava que você saísse vivo dessa.", 2, true);
    }

    // =====================================================================
    //  /constelacoes: moedas, interesse e bênçãos
    // =====================================================================

    private static final class Tela implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int[] SLOTS = {10, 11, 12, 14, 15, 16};

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    private int custo(Player p, Bencao b) {
        return b.deus == favorito(p) ? (int) Math.ceil(b.custo * 0.7) : b.custo;
    }

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inv = Bukkit.createInventory(t, 27, Component.text("✧ As Constelações"));
        int[] inter = interesse(p);
        Deus fav = favorito(p);
        ItemStack vidro = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.itemName(Component.text(" ")));
        for (int i = 0; i < 27; i++) t.inv.setItem(i, vidro);
        t.inv.setItem(4, item(Material.NETHER_STAR, Component.text("✧ " + moedas(p) + " Moedas Estelares", COR, TextDecoration.BOLD), List.of(
                Component.text("Os deuses assistem aos seus feitos e", NamedTextColor.GRAY),
                Component.text("mandam moedas quando algo os impressiona.", NamedTextColor.GRAY),
                Component.text("Com a vida baixa, a cena vale o dobro.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text(fav == null ? "Nenhum deus te acompanha de perto ainda." : "Quem mais te acompanha: " + fav.nome() + " (bênção 30% mais barata)",
                        NamedTextColor.YELLOW))));
        Bencao[] todas = Bencao.values();
        for (int k = 0; k < todas.length; k++) {
            Bencao b = todas[k];
            int custo = custo(p, b);
            boolean pode = moedas(p) >= custo;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(b.deus.nome() + ", " + b.deus.titulo(), b.deus.cor()));
            lore.add(Component.text(b.efeito, NamedTextColor.WHITE));
            lore.add(Component.empty());
            lore.add(Component.text("Interesse dele por você: " + inter[b.deus.ordinal()], NamedTextColor.GRAY));
            lore.add(Component.text("Custa " + custo + " moedas" + (custo < b.custo ? " (era " + b.custo + ")" : ""), pode ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.text(pode ? "» Clique para receber" : "Moedas insuficientes", pode ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            ItemStack icone = item(b.icone, Component.text(b.deus.simbolo() + " " + b.nome, b.deus.cor(), TextDecoration.BOLD), lore);
            if (pode) icone.editMeta(m -> m.setEnchantmentGlintOverride(true));
            t.inv.setItem(SLOTS[k], icone);
        }
        p.openInventory(t.inv);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Tela) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Tela) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        for (int k = 0; k < SLOTS.length; k++) {
            if (SLOTS[k] != e.getSlot()) continue;
            Bencao b = Bencao.values()[k];
            int custo = custo(p, b);
            if (moedas(p) < custo) {
                p.sendActionBar(Component.text("✖ Faltam moedas: precisa de " + custo + ".", NamedTextColor.RED));
                return;
            }
            if (b == Bencao.SEGUNDA && p.getPersistentDataContainer().has(K_SEGUNDA)) {
                p.sendActionBar(Component.text("✖ Você já tem uma Segunda Chance guardada.", NamedTextColor.RED));
                return;
            }
            darMoedas(p, -custo);
            receber(p, b);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
            p.sendMessage(Component.text("[" + b.deus.nome() + "] ", b.deus.cor(), TextDecoration.BOLD)
                    .append(Component.text("concede: " + b.nome + " (" + b.efeito + ").", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)));
            abrir(p);
            return;
        }
    }

    private void receber(Player p, Bencao b) {
        switch (b) {
            case FURIA -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 300, 0));
            case FORJA -> p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 600, 1));
            case BOSQUE -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 180, 0));
            case MARES -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 600, 0));
            }
            case CEU -> plugin.arcano().darMana(p, 80);
            case SEGUNDA -> p.getPersistentDataContainer().set(K_SEGUNDA, PersistentDataType.BYTE, (byte) 1);
        }
    }

    /** Admin: dá moedas (para testar). */
    public void darMoedasAdmin(Player p, int n) {
        darMoedas(p, n);
    }

    /** Admin: força um comentário (para testar). */
    public void testar(Player p) {
        comentar(p, Deus.values()[ThreadLocalRandom.current().nextInt(Deus.values().length)], "está de olho em você.", 1, vidaBaixa(p));
    }
}
