package br.rpgatributos.detalhes;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.aventura.Raro;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estante de Troféus: o núcleo de um chefe jogado num pilar de quartzo. O núcleo fica girando
 * (3.9: e também cabeças, peixes troféu, relíquias, armas com nome e armas com memória: a Galeria)
 * em cima, com quem venceu e quando. Cada troféu diferente dentro de um território dá +1% de
 * XP a quem pode construir nele e está lá dentro (até +6%, um de cada chefe).
 */
public final class Trofeus extends Estacao {

    public static final TextColor COR = TextColor.color(0xFFC107);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Map<UUID, Double> bonus = new HashMap<>();
    private int passos;

    public Trofeus(RPGAtributos plugin) {
        super(plugin, "trofeus", "trofeu_display");
    }

    private NamespacedKey chave(Location l) {
        return new NamespacedKey(plugin, "trofeu_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    /** [núcleo, quem, data] do troféu, ou null. */
    private String[] dados(Location l) {
        String s = l.getChunk().getPersistentDataContainer().get(chave(l), PersistentDataType.STRING);
        return s == null ? null : s.split("\\|", 3);
    }

    private Raro nucleo(Location l) {
        String[] d = dados(l);
        if (d == null) return null;
        try {
            return Raro.valueOf(d[0]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // =====================================================================
    //  3.9: a Galeria. Além do núcleo de chefe, a estante expõe qualquer troféu:
    //  cabeças, peixes troféu, armas com memória (100+ abates), relíquias e armas com nome.
    // =====================================================================

    private NamespacedKey chaveItem(Location l) {
        return new NamespacedKey(plugin, "trofeu_item_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ());
    }

    private static final java.util.Set<Material> CABECAS = java.util.EnumSet.of(Material.DRAGON_HEAD, Material.WITHER_SKELETON_SKULL,
            Material.ZOMBIE_HEAD, Material.SKELETON_SKULL, Material.CREEPER_HEAD, Material.PIGLIN_HEAD, Material.PLAYER_HEAD);

    /** O que este item é como troféu (a chave que conta para o bônus), ou null se não é troféu. */
    public static String tipoTrofeu(ItemStack i) {
        if (i == null || i.isEmpty()) return null;
        Raro r = Raro.de(i);
        if (r != null && r.nucleo()) return r.name();
        if (CABECAS.contains(i.getType())) return "cabeca:" + i.getType().name();
        if (br.rpgatributos.mundo.NaturezaViva.deLembranca(i) != null) return "lendario:" + br.rpgatributos.mundo.NaturezaViva.deLembranca(i).name();
        if (i.getPersistentDataContainer().has(br.rpgatributos.pesca.PesoPeixe.K_PESO)) return "peixe:" + i.getType().name() + ":" + nomeSimples(i);
        if (br.rpgatributos.exploracao.Arqueologia.de(i) != null) return "reliquia:" + br.rpgatributos.exploracao.Arqueologia.de(i).id();
        if (br.rpgatributos.masmorra.Reliquias.de(i) != null) return "reliquia_masmorra:" + nomeSimples(i);
        if (br.rpgatributos.forja.ArmasMemoria.ehArma(i) && br.rpgatributos.forja.ArmasMemoria.memoria(i) >= br.rpgatributos.forja.ArmasMemoria.MARCADA) return "arma:" + nomeSimples(i);
        if (br.rpgatributos.forja.Nomeada.de(i) != null) return "nomeada:" + br.rpgatributos.forja.Nomeada.de(i).name();
        return null;
    }

    private static String nomeSimples(ItemStack i) {
        var m = i.getItemMeta();
        Component c = m == null ? null : m.hasCustomName() ? m.customName() : m.hasItemName() ? m.itemName() : null;
        return c == null ? i.getType().name().toLowerCase() : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
    }

    private ItemStack itemExposto(Location l) {
        String s = l.getChunk().getPersistentDataContainer().get(chaveItem(l), PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return ItemStack.deserializeBytes(java.util.Base64.getDecoder().decode(s));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** A chave do troféu exposto (para o bônus), ou null. */
    private String tipo(Location l) {
        Raro r = nucleo(l);
        if (r != null) return r.name();
        ItemStack i = itemExposto(l);
        return i == null ? null : tipoTrofeu(i);
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.QUARTZ_PILLAR;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        // 3.9: qualquer item pode ser troféu; quem decide é tipoTrofeu (no ritual).
        return true;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.QUARTZ_PILLAR ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        for (Item i : itens) {
            ItemStack st = i.getItemStack();
            String tipo = tipoTrofeu(st);
            if (tipo == null) continue;
            UUID quem = i.getThrower();
            String nome = quem == null ? "?" : Bukkit.getOfflinePlayer(quem).getName();
            Raro r = Raro.de(st);
            var pdc = bloco.getChunk().getPersistentDataContainer();
            if (r != null && r.nucleo()) {
                pdc.set(chave(bloco.getLocation()), PersistentDataType.STRING, r.name() + "|" + nome + "|" + LocalDate.now().format(DATA));
            } else {
                ItemStack um = st.clone();
                um.setAmount(1);
                pdc.set(chave(bloco.getLocation()), PersistentDataType.STRING, "ITEM|" + nome + "|" + LocalDate.now().format(DATA));
                pdc.set(chaveItem(bloco.getLocation()), PersistentDataType.STRING, java.util.Base64.getEncoder().encodeToString(um.serializeAsBytes()));
            }
            tirar(i, 1);
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.GOLD_INGOT);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        Raro r = nucleo(bloco);
        if (r != null) return r.criar();
        ItemStack i = itemExposto(bloco);
        return i == null ? itemFlutuante() : i;
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.55;
    }

    @Override
    protected Component nome() {
        return Component.text("🏆 Troféu", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        String[] d = dados(bloco);
        if (d == null) return nome();
        Raro r = nucleo(bloco);
        Component titulo;
        String detalhe = null;
        if (r != null) titulo = Component.text(r.nome());
        else {
            ItemStack i = itemExposto(bloco);
            if (i == null) return nome();
            var m = i.getItemMeta();
            // Sem nome próprio, o nome do jogo (traduzido no cliente).
            titulo = m != null && m.hasCustomName() ? m.customName() : m != null && m.hasItemName() ? m.itemName() : Component.translatable(i.getType().translationKey());
            detalhe = linhaDeTrofeu(i);
        }
        Component c = Component.text("🏆 ", COR, TextDecoration.BOLD).append(titulo.colorIfAbsent(COR).decoration(TextDecoration.BOLD, true));
        if (detalhe != null) c = c.append(Component.newline()).append(Component.text(detalhe, NamedTextColor.WHITE));
        return c.append(Component.newline()).append(Component.text(d[1] + " · " + d[2], NamedTextColor.GRAY));
    }

    /** A linha que conta a história do troféu: o peso do peixe ou a memória da arma. */
    private static String linhaDeTrofeu(ItemStack i) {
        if (!i.hasItemMeta() || !i.getItemMeta().hasLore()) return null;
        for (Component l : i.getItemMeta().lore()) {
            String s = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(l);
            if (s.startsWith("⚖") || s.startsWith("⚔ Memória")) return s;
        }
        return null;
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1.4, 0.5);
        c.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, c, 40, 0.3, 0.4, 0.3, 0.3);
        c.getWorld().playSound(c, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        if (quem != null) {
            plugin.titulos().registrar(quem, "trofeus", 1);
            quem.sendMessage(Component.text("🏆 Troféu exposto! Dentro do seu território, cada troféu diferente dá +1% de XP (até +10%).", COR));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 5 == 0) centro.getWorld().spawnParticle(Particle.WAX_ON, centro, 2, 0.2, 0.2, 0.2, 0);
    }

    @Override
    protected void devolver(Location centro) {
        Location l = centro.getBlock().getLocation();
        Raro r = nucleo(l);
        if (r != null) centro.getWorld().dropItemNaturally(centro, r.criar());
        else {
            ItemStack i = itemExposto(l);
            if (i != null) centro.getWorld().dropItemNaturally(centro, i);
        }
    }

    @Override
    protected void aoRemover(Location bloco) {
        bloco.getChunk().getPersistentDataContainer().remove(chave(bloco));
        bloco.getChunk().getPersistentDataContainer().remove(chaveItem(bloco));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        String[] d = dados(b.getLocation());
        if (d == null) return;
        Raro r = nucleo(b.getLocation());
        ItemStack i = r == null ? itemExposto(b.getLocation()) : null;
        String nome = r != null ? r.nome() : i != null ? nomeSimples(i) : "?";
        String linha = i == null ? null : linhaDeTrofeu(i);
        p.sendMessage(Component.text("🏆 " + nome + ", trazido por " + d[1] + " em " + d[2] + "." + (linha == null ? "" : " " + linha), COR));
    }

    // =====================================================================
    //  Bônus no território
    // =====================================================================

    public double multiplicadorXp(Player p) {
        return 1 + bonus.getOrDefault(p.getUniqueId(), 0.0);
    }

    @Override
    protected void aoTick() {
        if (++passos % 40 != 0) return; // a cada 10 s
        bonus.clear();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            var t = plugin.territorios().em(p.getLocation());
            if (t == null || !plugin.territorios().podeConstruir(p.getUniqueId(), p.getLocation())) continue;
            Set<String> tipos = new java.util.HashSet<>();
            for (Location l : perto(p.getLocation(), 96)) {
                String k = tipo(l);
                if (k != null && t.equals(plugin.territorios().em(l))) tipos.add(k);
            }
            if (!tipos.isEmpty()) bonus.put(p.getUniqueId(), 0.01 * Math.min(10, tipos.size()));
        }
    }
}
