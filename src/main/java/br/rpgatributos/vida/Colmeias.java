package br.rpgatributos.vida;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.mundo.Ceu;
import br.rpgatributos.mundo.Estacoes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Colmeia do Apicultor: 1 favo de mel e 1 flor jogados numa colmeia. Com pelo menos 4 flores
 * por perto, faz um mel a cada 5 minutos (até 6). O tipo depende do lugar e da hora: girassóis
 * dão Mel Dourado, frio ou inverno dão Mel Gelado, 5 flores diferentes dão Mel Floral, a noite
 * dá Mel Noturno; senão, Mel Silvestre. Para tirar, clique com garrafas de vidro.
 */
public final class Colmeias extends Estacao {

    public static final TextColor COR = TextColor.color(0xFFB300);
    private static final String DADO = "colmeia";
    private static final int MAXIMO = 6;

    public Colmeias(RPGAtributos plugin) {
        super(plugin, "colmeias", "colmeia_display");
    }

    private long intervalo() {
        return plugin.settings().vidaColmeiaMinutos * 60_000L;
    }

    /** Flores a até 5 blocos: [total, tipos diferentes, girassóis]. */
    private static int[] flores(Location l) {
        int total = 0, girassois = 0;
        Set<Material> tipos = EnumSet.noneOf(Material.class);
        for (int x = -5; x <= 5; x++) for (int y = -2; y <= 2; y++) for (int z = -5; z <= 5; z++) {
            Material m = l.getWorld().getBlockAt(l.getBlockX() + x, l.getBlockY() + y, l.getBlockZ() + z).getType();
            if (!Tag.FLOWERS.isTagged(m)) continue;
            total++;
            tipos.add(m);
            if (m == Material.SUNFLOWER) girassois++;
        }
        return new int[]{total, tipos.size(), girassois};
    }

    public Mel melDe(Location l, int[] f) {
        if (f[2] >= 2) return Mel.DOURADO;
        if (l.getBlock().getTemperature() < 0.15 || plugin.estacoes().atual() == Estacoes.Estacao.INVERNO) return Mel.GELADO;
        if (f[1] >= 5) return Mel.FLORAL;
        if (Ceu.noite(l.getWorld())) return Mel.NOTURNO;
        return Mel.SILVESTRE;
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.BEEHIVE || m == Material.BEE_NEST;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.HONEYCOMB || Tag.SMALL_FLOWERS.isTagged(m);
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return blocoValido(abaixo.getType()) ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        Item flor = null;
        for (Item i : itens) if (Tag.SMALL_FLOWERS.isTagged(i.getItemStack().getType())) { flor = i; break; }
        if (contar(itens, Material.HONEYCOMB) < 1 || flor == null) return false;
        tirar(itens, Material.HONEYCOMB, 1);
        tirar(flor, 1);
        DadosBloco.gravar(DADO, bloco.getLocation(), "0|" + System.currentTimeMillis());
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return Mel.SILVESTRE.criar(1);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("🐝 Colmeia do Apicultor", COR).append(Component.newline())
                .append(Component.text("clique com garrafas de vidro", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.FALLING_HONEY, c, 15, 0.4, 0.2, 0.4, 0);
        c.getWorld().playSound(c, Sound.BLOCK_BEEHIVE_WORK, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("🐝 Colmeia pronta. Com 4 flores por perto ela faz mel a cada "
                + plugin.settings().vidaColmeiaMinutos + " min.", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 10 == 0) centro.getWorld().spawnParticle(Particle.DRIPPING_HONEY, centro.clone().subtract(0, 0.5, 0), 1, 0.3, 0.1, 0.3, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.HONEYCOMB));
    }

    @Override
    protected void aoRemover(Location bloco) {
        DadosBloco.apagar(DADO, bloco);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        int[] f = flores(l);
        if (f[0] < 4) {
            p.sendMessage(Component.text("🐝 Faltam flores: a colmeia precisa de 4 flores a até 5 blocos (tem " + f[0] + ").", NamedTextColor.RED));
            DadosBloco.gravar(DADO, l, "0|" + System.currentTimeMillis());
            return;
        }
        long[] c = DadosBloco.produzir(DADO, l, intervalo(), MAXIMO);
        Mel mel = melDe(l, f);
        if (c[0] <= 0) {
            long falta = intervalo() - (System.currentTimeMillis() - c[1]);
            p.sendMessage(Component.text("🐝 Ainda sem mel. O próximo (" + mel.nome() + ") em " + Math.max(1, falta / 60000 + 1) + " min.", COR));
            DadosBloco.gravar(DADO, l, c[0] + "|" + c[1]);
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        int garrafas = criativo ? (int) c[0] : contarGarrafas(p);
        int tirar = (int) Math.min(c[0], garrafas);
        if (tirar <= 0) {
            p.sendMessage(Component.text("🐝 Há " + c[0] + " " + mel.nome() + " pronto(s). Traga garrafas de vidro.", COR));
            DadosBloco.gravar(DADO, l, c[0] + "|" + c[1]);
            return;
        }
        if (!criativo) p.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, tirar));
        p.getInventory().addItem(mel.criar(tirar)).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        long resto = c[0] - tirar;
        DadosBloco.gravar(DADO, l, resto + "|" + (c[0] >= MAXIMO ? System.currentTimeMillis() : c[1]));
        p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
        b.getWorld().spawnParticle(Particle.FALLING_HONEY, b.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.2, 0.3, 0);
        plugin.stats().darXp(p, Skill.AGRICULTURA, 3.0 * tirar);
        plugin.titulos().registrar(p, "meis", tirar);
        p.sendMessage(Component.text("🐝 Você tirou " + tirar + " " + mel.nome() + ".", mel.cor()));
    }

    private static int contarGarrafas(Player p) {
        int n = 0;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == Material.GLASS_BOTTLE && !i.hasItemMeta()) n += i.getAmount();
        }
        return n;
    }

    /** O efeito do mel especial ao beber. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoBeber(PlayerItemConsumeEvent e) {
        Mel m = Mel.de(e.getItem());
        if (m == null) return;
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> { if (p.isOnline()) p.addPotionEffect(m.efeito()); });
    }
}
