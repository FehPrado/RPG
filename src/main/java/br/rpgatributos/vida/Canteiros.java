package br.rpgatributos.vida;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.mundo.Estacoes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Canteiro de Ervas: 1 farinha de osso e 1 vaso de flor jogados num bloco de musgo. A cada
 * 10 minutos (tempo real, mesmo longe) nasce uma erva, até 4. A erva depende do lugar: frio,
 * quente, pântano, embaixo da terra, cerejeira/flores ou, nos outros lugares, Sálvia.
 */
public final class Canteiros extends Estacao {

    public static final TextColor COR = TextColor.color(0x7CB342);
    private static final String DADO = "canteiro";
    private static final int MAXIMO = 4;

    public Canteiros(RPGAtributos plugin) {
        super(plugin, "canteiros", "canteiro_display");
    }

    private long intervalo() {
        return plugin.settings().vidaCanteiroMinutos * 60_000L;
    }

    /** A erva que cresce nesse lugar. */
    public Erva ervaDe(Location l) {
        Block b = l.getBlock();
        String bioma = b.getWorld().getBiome(b.getX(), b.getY(), b.getZ()).getKey().getKey().toLowerCase(Locale.ROOT);
        double temperatura = b.getTemperature();
        if (b.getY() < 40 && b.getLightFromSky() < 8) return Erva.RAIZ_ABISSAL;
        if (temperatura < 0.15 || plugin.estacoes().atual() == Estacoes.Estacao.INVERNO) return Erva.FOLHA_GELIDA;
        if (bioma.contains("swamp") || bioma.contains("mangrove")) return Erva.MUSGO_LUNAR;
        if (bioma.contains("cherry") || bioma.contains("flower") || bioma.contains("meadow")) return Erva.FLOR_DE_CRISTAL;
        if (temperatura > 1.0) return Erva.ERVA_DE_SOL;
        return Erva.SALVIA;
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.MOSS_BLOCK;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.BONE_MEAL || m == Material.FLOWER_POT;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.MOSS_BLOCK ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.BONE_MEAL) < 1 || contar(itens, Material.FLOWER_POT) < 1) return false;
        tirar(itens, Material.BONE_MEAL, 1);
        tirar(itens, Material.FLOWER_POT, 1);
        DadosBloco.gravar(DADO, bloco.getLocation(), "0|" + System.currentTimeMillis());
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return Erva.SALVIA.criar(1);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        return ervaDe(bloco).criar(1);
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.45;
    }

    @Override
    protected Component nome() {
        return Component.text("☘ Canteiro de Ervas", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        Erva e = ervaDe(bloco);
        return Component.text("☘ Canteiro de Ervas", COR).append(Component.newline())
                .append(Component.text("aqui cresce " + e.nome(), e.cor()));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c, 20, 0.4, 0.2, 0.4, 0);
        c.getWorld().playSound(c, Sound.BLOCK_MOSS_PLACE, 1f, 1f);
        if (quem != null) quem.sendMessage(Component.text("☘ Canteiro pronto. Aqui cresce " + ervaDe(b.getLocation()).nome()
                + ": uma a cada " + plugin.settings().vidaCanteiroMinutos + " min, até " + MAXIMO + ".", COR));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 8 == 0) centro.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, centro.clone().subtract(0, 0.5, 0), 1, 0.3, 0.1, 0.3, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.FLOWER_POT));
    }

    @Override
    protected void aoRemover(Location bloco) {
        DadosBloco.apagar(DADO, bloco);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        long[] c = DadosBloco.produzir(DADO, l, intervalo(), MAXIMO);
        Erva e = ervaDe(l);
        if (c[0] <= 0) {
            long falta = intervalo() - (System.currentTimeMillis() - c[1]);
            p.sendMessage(Component.text("☘ Ainda não nasceu nada. A próxima " + e.nome() + " em " + Math.max(1, falta / 60000 + 1) + " min.", COR));
            DadosBloco.gravar(DADO, l, c[0] + "|" + c[1]);
            return;
        }
        p.getInventory().addItem(e.criar((int) c[0])).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        DadosBloco.gravar(DADO, l, "0|" + (c[0] >= MAXIMO ? System.currentTimeMillis() : c[1]));
        p.playSound(p.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1.1f);
        b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 1.1, 0.5), 8, 0.3, 0.1, 0.3, 0);
        plugin.stats().darXp(p, Skill.AGRICULTURA, 2 * c[0]);
        plugin.titulos().registrar(p, "ervas", (int) c[0]);
        p.sendMessage(Component.text("☘ Você colheu " + c[0] + " " + e.nome() + ".", COR));
    }
}
