package br.rpgatributos.vida;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.alquimia.Ingrediente;
import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;

/**
 * Tacho do Artesão: 4 garrafas de vidro e 1 açúcar jogados num vaso decorado. Clique com o
 * ingrediente na mão (até 8 iguais de uma vez) e espere: leite vira queijo, fruta vira geleia,
 * legume vira conserva, ovo vira maionese, trufa vira óleo. Clique de novo para pegar.
 */
public final class Tachos extends Estacao {

    public static final TextColor COR = TextColor.color(0xC2185B);
    private static final String DADO = "tacho";
    private static final int MAXIMO = 8;
    private static final Set<Cultivo> FRUTAS = Set.of(Cultivo.MORANGO, Cultivo.MIRTILO, Cultivo.UVA, Cultivo.OXICOCO, Cultivo.MELAO);

    public Tachos(RPGAtributos plugin) {
        super(plugin, "tachos", "tacho_display");
    }

    /** O que esse item vira no tacho, com o nome da origem (null se não serve). */
    private record Receita(Artesanato tipo, String origem, boolean devolveBalde) { }

    private static Receita receita(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        String produto = Criacao.produto(s);
        if ("leite_cabra".equals(produto)) return new Receita(Artesanato.QUEIJO_DE_CABRA, null, true);
        if ("trufa".equals(produto)) return new Receita(Artesanato.OLEO_DE_TRUFA, null, false);
        Cultivo c = Cultivo.daColheita(s);
        if (c != null) return new Receita(FRUTAS.contains(c) ? Artesanato.GELEIA : Artesanato.CONSERVA, c.nome(), false);
        Fruta f = Fruta.de(s);
        if (f != null) return new Receita(Artesanato.GELEIA, f.nome(), false);
        if (!Ingrediente.simples(s)) return null;
        return switch (s.getType()) {
            case MILK_BUCKET -> new Receita(Artesanato.QUEIJO, null, true);
            case EGG -> new Receita(Artesanato.MAIONESE, null, false);
            case APPLE -> new Receita(Artesanato.GELEIA, "Maçã", false);
            case SWEET_BERRIES -> new Receita(Artesanato.GELEIA, "Bagas", false);
            case GLOW_BERRIES -> new Receita(Artesanato.GELEIA, "Bagas Brilhantes", false);
            case CARROT -> new Receita(Artesanato.CONSERVA, "Cenoura", false);
            case BEETROOT -> new Receita(Artesanato.CONSERVA, "Beterraba", false);
            case POTATO -> new Receita(Artesanato.CONSERVA, "Batata", false);
            default -> null;
        };
    }

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.DECORATED_POT;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.GLASS_BOTTLE || m == Material.SUGAR;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block aqui = item.getLocation().getBlock();
        if (aqui.getType() == Material.DECORATED_POT) return aqui;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.DECORATED_POT ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.GLASS_BOTTLE) < 4 || contar(itens, Material.SUGAR) < 1) return false;
        tirar(itens, Material.GLASS_BOTTLE, 4);
        tirar(itens, Material.SUGAR, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return Artesanato.GELEIA.criar("Morango", null, 1);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        String[] d = dados(bloco);
        return d == null ? new ItemStack(Material.GLASS_BOTTLE) : Artesanato.valueOf(d[0]).criar(d[1].isEmpty() ? null : d[1], null, 1);
    }

    @Override
    protected float escalaItem() {
        return 0.4f;
    }

    @Override
    protected double alturaItem() {
        return 1.3;
    }

    @Override
    protected Component nome() {
        return Component.text("🫙 Tacho do Artesão", COR);
    }

    @Override
    protected Component nome(Location bloco) {
        String[] d = dados(bloco);
        if (d == null) return nome().append(Component.newline()).append(Component.text("clique com leite, fruta, legume, ovo ou trufa", NamedTextColor.GRAY));
        Artesanato a = Artesanato.valueOf(d[0]);
        long falta = falta(d);
        return nome().append(Component.newline()).append(Component.text(d[3] + "x " + a.nomeCom(d[1].isEmpty() ? null : d[1])
                + (falta <= 0 ? " · pronto!" : " · " + (falta / 60_000 + 1) + " min"), falta <= 0 ? NamedTextColor.GREEN : a.cor()));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        Location c = b.getLocation().add(0.5, 1, 0.5);
        c.getWorld().playSound(c, Sound.BLOCK_DECORATED_POT_INSERT, 1f, 1f);
        c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c, 12, 0.3, 0.2, 0.3, 0);
        if (quem != null) {
            quem.sendMessage(Component.text("🫙 Tacho do Artesão pronto! Clique com o ingrediente na mão: leite → queijo, fruta → geleia, "
                    + "legume → conserva, ovo → maionese, trufa → óleo.", COR));
        }
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 10 == 0 && dados(centro.getBlock().getLocation().subtract(0, 1, 0)) != null) {
            centro.getWorld().spawnParticle(Particle.BUBBLE_POP, centro.clone().subtract(0, 0.6, 0), 2, 0.15, 0.05, 0.15, 0);
        }
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GLASS_BOTTLE, 4));
    }

    @Override
    protected void aoRemover(Location bloco) {
        // O que estava no tacho volta pronto (o artesão não perde o trabalho).
        String[] d = dados(bloco);
        if (d != null) {
            Artesanato a = Artesanato.valueOf(d[0]);
            bloco.getWorld().dropItemNaturally(bloco.clone().add(0.5, 0.5, 0.5),
                    a.criar(d[1].isEmpty() ? null : d[1], Qualidade.valueOf(d[2]), Integer.parseInt(d[3])));
        }
        DadosBloco.apagar(DADO, bloco);
    }

    /** [tipo, origem, qualidade, quantidade, fim] ou null. */
    private static String[] dados(Location l) {
        String s = DadosBloco.ler(DADO, l);
        if (s == null) return null;
        String[] d = s.split("\\|", -1);
        if (d.length != 5) return null;
        try {
            Artesanato.valueOf(d[0]);
            Qualidade.valueOf(d[2]);
            Integer.parseInt(d[3]);
            Long.parseLong(d[4]);
            return d;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static long falta(String[] d) {
        return Long.parseLong(d[4]) - System.currentTimeMillis();
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Location l = b.getLocation();
        String[] d = dados(l);
        if (d != null) {
            Artesanato a = Artesanato.valueOf(d[0]);
            String origem = d[1].isEmpty() ? null : d[1];
            long falta = falta(d);
            if (falta > 0) {
                p.sendActionBar(Component.text("🫙 " + d[3] + "x " + a.nomeCom(origem) + ": falta " + (falta / 60_000 + 1) + " min.", a.cor()));
                return;
            }
            int qtd = Integer.parseInt(d[3]);
            ItemStack pronto = a.criar(origem, Qualidade.valueOf(d[2]), qtd);
            for (ItemStack sobra : p.getInventory().addItem(pronto).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            DadosBloco.apagar(DADO, l);
            atualizarDisplays(l);
            p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1.2f);
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.CULINARIA, 4 * qtd);
            plugin.titulos().registrar(p, "artesanato", qtd);
            p.sendActionBar(Component.text("🫙 " + qtd + "x " + a.nomeCom(origem) + " pronto(a)!", a.cor()));
            return;
        }
        ItemStack mao = p.getInventory().getItemInMainHand();
        Receita r = receita(mao);
        if (r == null) {
            p.sendActionBar(Component.text("🫙 Clique com leite, fruta, legume, ovo ou trufa na mão.", NamedTextColor.GRAY));
            return;
        }
        int qtd = Math.min(MAXIMO, mao.getAmount());
        Qualidade q = Qualidade.de(mao);
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo) {
            mao.setAmount(mao.getAmount() - qtd);
            if (r.devolveBalde()) {
                for (ItemStack sobra : p.getInventory().addItem(new ItemStack(Material.BUCKET, qtd)).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
            }
        }
        long fim = System.currentTimeMillis() + r.tipo().minutos() * 60_000L;
        DadosBloco.gravar(DADO, l, r.tipo().name() + "|" + (r.origem() == null ? "" : r.origem()) + "|" + q.name() + "|" + qtd + "|" + fim);
        atualizarDisplays(l);
        p.playSound(p.getLocation(), Sound.BLOCK_DECORATED_POT_INSERT, 1f, 0.9f);
        p.sendActionBar(Component.text("🫙 " + qtd + "x " + r.tipo().nomeCom(r.origem()) + " no tacho: pronto em " + r.tipo().minutos() + " min.",
                r.tipo().cor()));
    }

    /** Comer o que saiu do tacho dá os efeitos dele (mais longos com qualidade). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoComer(PlayerItemConsumeEvent e) {
        Artesanato a = Artesanato.de(e.getItem());
        if (a == null) return;
        double fator = Qualidade.de(e.getItem()).fator();
        for (PotionEffect ef : a.efeitos()) {
            int ticks = ef.getDuration() <= 80 ? ef.getDuration() : (int) Math.round(ef.getDuration() * fator);
            e.getPlayer().addPotionEffect(new PotionEffect(ef.getType(), ticks, ef.getAmplifier()));
        }
    }
}
