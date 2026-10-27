package br.rpgatributos.estrada;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.colonia.Colonia;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gente nas estradas (2.98). Perto de uma estrada com nome (trechos ligados por marcos), de dia, às
 * vezes alguém vem andando por ela, de um lado para o outro:
 * <ul>
 *   <li><b>Visita</b> (estrada que passa por 2 colônias): um morador de verdade de uma colônia, com o
 *       nome e a roupa dele, indo visitar a outra.</li>
 *   <li><b>Peregrino</b>: conta as notícias do mundo e dá a Bênção do Caminho (Sorte por 5 min, uma vez).</li>
 *   <li><b>Mensageiro</b>: pede uma coisinha para a viagem e paga em esmeraldas.</li>
 *   <li><b>Mercador de estrada</b>: 3 trocas de coisas de estrada (pedra de calçar, lanterna, mapa...).</li>
 * </ul>
 * E, na prefeitura de uma colônia ligada, às vezes há um visitante da outra colônia parado na praça.
 * São aldeões que não são salvos no mundo e somem quando ninguém está perto.
 */
public final class Andarilhos implements Listener {

    public static final TextColor COR = TextColor.color(0xE6C88A);
    private static final long VIDA_MS = 5 * 60_000L;
    private static final int MAX = 10;

    private enum Tipo { VISITA, PEREGRINO, MENSAGEIRO, MERCADOR, PRACA }

    private record Pedido(Material item, int qtd, int esmeraldas, String nome) { }

    private static final List<Pedido> PEDIDOS = List.of(
            new Pedido(Material.BREAD, 6, 2, "6 pães"), new Pedido(Material.TORCH, 16, 1, "16 tochas"),
            new Pedido(Material.WHITE_WOOL, 4, 2, "4 lãs brancas"), new Pedido(Material.ARROW, 12, 2, "12 flechas"),
            new Pedido(Material.COAL, 8, 1, "8 carvões"), new Pedido(Material.PAPER, 6, 2, "6 papéis"),
            new Pedido(Material.APPLE, 4, 2, "4 maçãs"), new Pedido(Material.LEATHER, 3, 3, "3 couros"));

    private static final String[] FRASES_VISITA = {
            "Dizem que lá a torta de abóbora é a melhor da região.", "Vou ver minha irmã, que mora lá.",
            "Levo umas sementes para trocar na feira.", "A estrada está bonita hoje, não acha?",
            "Prometi ajudar a levantar um telhado.", "Quero ver se a taverna de lá é tão boa quanto falam."};
    private static final String[] FALAS_PEREGRINO = {
            "Toda estrada termina numa porta aberta.", "Ande devagar: o caminho também é lugar.",
            "Quem acende uma lanterna na estrada acende para todos."};

    private static final class Andarilho {
        final UUID id;
        final Tipo tipo;
        final List<Location> pontos;
        int i;
        final int dir;
        final long nasceu = System.currentTimeMillis();
        final String quem, origem, destino;
        final Pedido pedido;
        final Set<UUID> atendidos = new HashSet<>();
        Location ultimo;
        int parado;

        Andarilho(UUID id, Tipo tipo, List<Location> pontos, int i, int dir, String quem, String origem, String destino, Pedido pedido) {
            this.id = id;
            this.tipo = tipo;
            this.pontos = pontos;
            this.i = i;
            this.dir = dir;
            this.quem = quem;
            this.origem = origem;
            this.destino = destino;
            this.pedido = pedido;
        }
    }

    private final RPGAtributos plugin;
    private final NamespacedKey kAndarilho;
    private final Map<UUID, Andarilho> ativos = new HashMap<>();
    private final Map<UUID, Long> espera = new HashMap<>();
    private int passos;

    public Andarilhos(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kAndarilho = new NamespacedKey(plugin, "andarilho");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Aparecer
    // =====================================================================

    /** A cada 1 s: andam; a cada 10 s, talvez apareça alguém. */
    public void tick() {
        andar();
        if (!plugin.settings().estradaAndarilhos || ++passos % 10 != 0 || ativos.size() >= MAX) return;
        long agora = System.currentTimeMillis();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            World w = p.getWorld();
            if (p.getGameMode() == GameMode.SPECTATOR || w.getEnvironment() != World.Environment.NORMAL || w.getTime() > 12000) continue;
            if (agora < espera.getOrDefault(p.getUniqueId(), 0L) || rnd().nextDouble() >= 0.3) continue;
            if (aparecerNaPraca(p) || aparecerNaEstrada(p, null) != null) {
                espera.put(p.getUniqueId(), agora + plugin.settings().estradaAndarilhosMinutos * 60_000L);
            }
        }
    }

    /** Alguém aparece na estrada perto do jogador ({@code tipo}: "visita", "peregrino", "mensageiro", "mercador" ou null). @return o que apareceu, ou null. */
    public String aparecerNaEstrada(Player p, String forcado) {
        Rede.Perto perto = plugin.rede().pertoDaEstrada(p.getLocation(), forcado != null ? 64 : 24);
        if (perto == null || perto.pontos().size() < 4) return null;
        Tipo tipo;
        if (forcado != null) {
            try {
                tipo = Tipo.valueOf(forcado.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return null;
            }
            if (tipo == Tipo.VISITA && perto.colonias().size() < 2) return null;
        } else if (perto.colonias().size() >= 2 && rnd().nextDouble() < 0.45) {
            tipo = Tipo.VISITA;
        } else {
            double r = rnd().nextDouble();
            tipo = r < 0.35 ? Tipo.PEREGRINO : r < 0.7 ? Tipo.MENSAGEIRO : Tipo.MERCADOR;
        }
        int n = perto.pontos().size();
        int lado = rnd().nextBoolean() ? 1 : -1;
        int ini = Math.max(0, Math.min(n - 1, perto.indice() + lado * 8));
        if (ini == perto.indice()) ini = Math.max(0, Math.min(n - 1, perto.indice() - lado * 8));
        if (ini == perto.indice()) return null;
        int dir = ini < perto.indice() ? 1 : -1;
        Location onde = perto.pontos().get(ini);
        if (!onde.isChunkLoaded()) return null;
        Villager v = (Villager) onde.getWorld().spawnEntity(onde, EntityType.VILLAGER);
        String quem, origem = null, destino = null;
        Pedido pedido = null;
        switch (tipo) {
            case VISITA -> {
                List<Colonia> cs = new ArrayList<>(perto.colonias());
                Colonia a = cs.remove(rnd().nextInt(cs.size())), b = cs.get(rnd().nextInt(cs.size()));
                origem = "Colônia de " + a.nomeDono();
                destino = "Colônia de " + b.nomeDono();
                if (!a.cidadaos().isEmpty()) {
                    Colonia.Cidadao ci = a.cidadaos().get(rnd().nextInt(a.cidadaos().size()));
                    quem = ci.nome();
                    v.setProfession(ci.profissao().roupa());
                } else {
                    quem = "Morador";
                    v.setProfession(Villager.Profession.FARMER);
                }
            }
            case PEREGRINO -> { quem = "Peregrino"; v.setProfession(Villager.Profession.CLERIC); }
            case MENSAGEIRO -> {
                quem = "Mensageiro";
                v.setProfession(Villager.Profession.CARTOGRAPHER);
                pedido = PEDIDOS.get(rnd().nextInt(PEDIDOS.size()));
            }
            default -> { quem = "Mercador de Estrada"; v.setProfession(Villager.Profession.LEATHERWORKER); }
        }
        preparar(v, quem + (tipo == Tipo.VISITA ? " · " + origem : ""));
        ativos.put(v.getUniqueId(), new Andarilho(v.getUniqueId(), tipo, perto.pontos(), ini, dir, quem, origem, destino, pedido));
        return quem + " na " + perto.estrada();
    }

    /** Um visitante parado na praça (prefeitura) de uma colônia ligada por estrada. */
    private boolean aparecerNaPraca(Player p) {
        for (Colonia c : plugin.colonias().todas()) {
            Location pref = c.prefeitura();
            if (pref == null || pref.getWorld() != p.getWorld() || pref.distanceSquared(p.getLocation()) > 40 * 40) continue;
            Map<UUID, String> lig = plugin.rede().ligacoes(c);
            if (lig.isEmpty() || rnd().nextDouble() >= 0.5) return false;
            for (Andarilho a : ativos.values()) if (a.tipo == Tipo.PRACA && ("Colônia de " + c.nomeDono()).equals(a.destino)) return false;
            List<UUID> ds = new ArrayList<>(lig.keySet());
            Colonia de = plugin.colonias().de(ds.get(rnd().nextInt(ds.size())));
            if (de == null || de.cidadaos().isEmpty()) return false;
            Colonia.Cidadao ci = de.cidadaos().get(rnd().nextInt(de.cidadaos().size()));
            Location onde = pref.clone().add(rnd().nextInt(-3, 4) + 0.5, 1, rnd().nextInt(-3, 4) + 0.5);
            onde = onde.getWorld().getHighestBlockAt(onde).getLocation().add(0.5, 1, 0.5);
            if (Math.abs(onde.getY() - pref.getY()) > 6) onde = pref.clone().add(0.5, 1, 0.5);
            Villager v = (Villager) onde.getWorld().spawnEntity(onde, EntityType.VILLAGER);
            v.setProfession(ci.profissao().roupa());
            preparar(v, ci.nome() + " · visitante da Colônia de " + de.nomeDono());
            ativos.put(v.getUniqueId(), new Andarilho(v.getUniqueId(), Tipo.PRACA, List.of(onde), 0, 0, ci.nome(),
                    "Colônia de " + de.nomeDono(), "Colônia de " + c.nomeDono(), null));
            return true;
        }
        return false;
    }

    private void preparar(Villager v, String nome) {
        if (v.getProfession() != Villager.Profession.NONE && v.getProfession() != Villager.Profession.NITWIT) {
            v.setVillagerExperience(1);
            v.setVillagerLevel(2);
        }
        v.customName(Component.text(nome, COR));
        v.setCustomNameVisible(true);
        v.setPersistent(false);
        v.setRemoveWhenFarAway(true);
        v.getPersistentDataContainer().set(kAndarilho, PersistentDataType.BYTE, (byte) 1);
    }

    // =====================================================================
    //  Andar
    // =====================================================================

    private void andar() {
        long agora = System.currentTimeMillis();
        for (Iterator<Andarilho> it = ativos.values().iterator(); it.hasNext(); ) {
            Andarilho a = it.next();
            Entity e = plugin.getServer().getEntity(a.id);
            if (!(e instanceof Villager v) || !e.isValid()) {
                it.remove();
                continue;
            }
            boolean alguem = false;
            for (Player p : e.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(e.getLocation()) < 72 * 72) { alguem = true; break; }
            }
            long vida = a.tipo == Tipo.PRACA ? 3 * 60_000L : VIDA_MS;
            if (!alguem || agora - a.nasceu > vida) {
                e.remove();
                it.remove();
                continue;
            }
            if (a.tipo == Tipo.PRACA) continue;
            Location alvo = a.pontos.get(a.i);
            if (e.getLocation().distanceSquared(alvo) < 9) {
                a.i += a.dir;
                if (a.i < 0 || a.i >= a.pontos.size()) {
                    e.remove(); // seguiu viagem
                    it.remove();
                    continue;
                }
                alvo = a.pontos.get(a.i);
            }
            if (a.ultimo != null && a.ultimo.distanceSquared(e.getLocation()) < 0.25) a.parado++;
            else a.parado = 0;
            a.ultimo = e.getLocation();
            if (a.parado >= 6 && alvo.isChunkLoaded()) {
                e.teleport(alvo);
                a.parado = 0;
            }
            v.getPathfinder().moveTo(alvo, 0.7);
        }
    }

    // =====================================================================
    //  Conversar
    // =====================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoFalar(PlayerInteractEntityEvent e) {
        Andarilho a = ativos.get(e.getRightClicked().getUniqueId());
        if (a == null && !e.getRightClicked().getPersistentDataContainer().has(kAndarilho)) return;
        e.setCancelled(true);
        if (a == null || e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Component nome = Component.text(a.quem + ": ", COR);
        switch (a.tipo) {
            case VISITA -> p.sendMessage(nome.append(Component.text("Sou da " + a.origem + " e vou visitar a " + a.destino + ". "
                    + FRASES_VISITA[rnd().nextInt(FRASES_VISITA.length)], NamedTextColor.GRAY)));
            case PRACA -> p.sendMessage(nome.append(Component.text("Vim da " + a.origem + " pela estrada. "
                    + FRASES_VISITA[rnd().nextInt(FRASES_VISITA.length)], NamedTextColor.GRAY)));
            case PEREGRINO -> peregrino(p, a, nome);
            case MENSAGEIRO -> mensageiro(p, a, nome);
            case MERCADOR -> mercador(p);
        }
        p.playSound(e.getRightClicked().getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
    }

    private void peregrino(Player p, Andarilho a, Component nome) {
        List<String> nots = plugin.noticias().recentes(2);
        p.sendMessage(nome.append(Component.text(nots.isEmpty() ? FALAS_PEREGRINO[rnd().nextInt(FALAS_PEREGRINO.length)]
                : "Paz na estrada. Pelo caminho, ouvi dizer:", NamedTextColor.GRAY)));
        for (String s : nots) p.sendMessage(Component.text("  • " + s, NamedTextColor.GRAY));
        if (a.atendidos.add(p.getUniqueId())) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 5 * 60 * 20, 0));
            p.sendMessage(Component.text("✦ Bênção do Caminho: Sorte por 5 minutos.", COR));
        }
    }

    private void mensageiro(Player p, Andarilho a, Component nome) {
        Pedido pd = a.pedido;
        if (a.atendidos.contains(p.getUniqueId())) {
            p.sendMessage(nome.append(Component.text("Obrigado de novo! A carta vai chegar a tempo.", NamedTextColor.GRAY)));
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (mao.getType() == pd.item() && mao.getAmount() >= pd.qtd() && !mao.hasItemMeta()) {
            mao.setAmount(mao.getAmount() - pd.qtd());
            p.getInventory().addItem(new ItemStack(Material.EMERALD, pd.esmeraldas())).values().forEach(s -> p.getWorld().dropItem(p.getLocation(), s));
            p.giveExp(15);
            a.atendidos.add(p.getUniqueId());
            p.sendMessage(nome.append(Component.text("Era bem disso que eu precisava! Tome " + pd.esmeraldas() + " esmeralda(s).", NamedTextColor.GREEN)));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        } else {
            p.sendMessage(nome.append(Component.text("Levo cartas até o fim da estrada. Você teria " + pd.nome()
                    + " para a viagem? (segure na mão e clique em mim)", NamedTextColor.GRAY)));
        }
    }

    private void mercador(Player p) {
        Merchant m = plugin.getServer().createMerchant(Component.text("Mercador de Estrada", COR));
        List<MerchantRecipe> todas = new ArrayList<>(List.of(
                receita(new ItemStack(Material.POLISHED_ANDESITE, 12), new ItemStack(Material.EMERALD, 1)),
                receita(new ItemStack(Material.LANTERN, 3), new ItemStack(Material.EMERALD, 1)),
                receita(new ItemStack(Material.MAP, 1), new ItemStack(Material.EMERALD, 2)),
                receita(new ItemStack(Material.BREAD, 6), new ItemStack(Material.EMERALD, 1)),
                receita(new ItemStack(Material.OAK_SIGN, 4), new ItemStack(Material.EMERALD, 1)),
                receita(new ItemStack(Material.EMERALD, 1), new ItemStack(Material.GRAVEL, 16)),
                receita(new ItemStack(Material.EMERALD, 1), new ItemStack(Material.LEATHER, 4))));
        java.util.Collections.shuffle(todas);
        m.setRecipes(todas.subList(0, 3));
        p.openMerchant(m, true);
    }

    private static MerchantRecipe receita(ItemStack resultado, ItemStack preco) {
        MerchantRecipe r = new MerchantRecipe(resultado, 4);
        r.addIngredient(preco);
        return r;
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(kAndarilho)) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        ativos.remove(e.getEntity().getUniqueId());
    }

    public void desligar() {
        for (Andarilho a : ativos.values()) {
            Entity e = plugin.getServer().getEntity(a.id);
            if (e != null) e.remove();
        }
        ativos.clear();
    }

    public int quantos() { return ativos.size(); }
}
