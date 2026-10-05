package br.rpgatributos.territorio;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Marco do Território: uma magnetita com 1 estandarte e 4 esmeraldas jogados em cima.
 * Quem já tem território e faz o ritual em outro lugar (com 1 bloco de esmeralda a mais)
 * cria um Marco de Expansão: outra área ligada ao mesmo território.
 * Funda o território do jogador; o estandarte vira a bandeira que gira em cima do Marco.
 */
public final class Marcos extends Estacao {

    private static final int ESMERALDAS = 4;

    /** O estandarte do ritual que acabou de ser consumido (vira a bandeira do território). */
    private ItemStack bandeiraDoRitual;
    /** O ritual em andamento é de um Marco de Expansão (quem fez já tem território). */
    private boolean ritualExpansao;

    public Marcos(RPGAtributos plugin) {
        super(plugin, "marcos", "marco_display");
    }

    // =====================================================================
    //  Ritual
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.LODESTONE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.EMERALD || m == Material.EMERALD_BLOCK || Tag.ITEMS_BANNERS.isTagged(m);
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.LODESTONE ? abaixo : null;
    }

    @Override
    protected String impedimento(Block b, UUID quem) {
        String base = super.impedimento(b, quem);
        if (base != null) return base;
        Player p = quem == null ? null : Bukkit.getPlayer(quem);
        if (p == null) return "Quem faz o ritual precisa estar online.";
        ritualExpansao = plugin.territorios().viraExpansao(p);
        return plugin.territorios().podeFundar(p, b);
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.EMERALD) < ESMERALDAS) return false;
        // A expansão custa também 1 bloco de esmeralda.
        if (ritualExpansao && contar(itens, Material.EMERALD_BLOCK) < 1) return false;
        Item estandarte = null;
        for (Item i : itens) {
            if (i.isValid() && Tag.ITEMS_BANNERS.isTagged(i.getItemStack().getType())) { estandarte = i; break; }
        }
        if (estandarte == null) return false;
        ItemStack bandeira = estandarte.getItemStack().clone();
        bandeira.setAmount(1);
        bandeiraDoRitual = bandeira;
        tirar(estandarte, 1);
        tirar(itens, Material.EMERALD, ESMERALDAS);
        if (ritualExpansao) tirar(itens, Material.EMERALD_BLOCK, 1);
        return true;
    }

    /** O território nasce antes do Marco, para a bandeira certa já aparecer girando. */
    @Override
    public void criar(Block b, Player quem) {
        if (quem == null) return;
        ItemStack bandeira = bandeiraDoRitual != null ? bandeiraDoRitual : new ItemStack(Material.WHITE_BANNER);
        bandeiraDoRitual = null;
        if (plugin.territorios().de(quem.getUniqueId()) == null) plugin.territorios().fundar(quem, b, bandeira);
        else plugin.territorios().fundarExpansao(quem, b);
        super.criar(b, quem);
    }

    // =====================================================================
    //  Aparência
    // =====================================================================

    private Territorio territorio(Location bloco) {
        Territorio t = plugin.territorios().em(bloco);
        if (t == null) return null;
        return t.ehMarco(bloco.getBlockX(), bloco.getBlockY(), bloco.getBlockZ()) ? t : null;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.WHITE_BANNER);
    }

    @Override
    protected ItemStack itemFlutuante(Location bloco) {
        Territorio t = territorio(bloco);
        return t == null ? itemFlutuante() : t.bandeira();
    }

    @Override
    protected float escalaItem() {
        return 0.8f;
    }

    @Override
    protected double alturaItem() {
        return 1.6;
    }

    @Override
    protected Component nome() {
        return Component.text("⚑ Marco do Território ⚑", Territorios.COR);
    }

    @Override
    protected Component nome(Location bloco) {
        Territorio t = territorio(bloco);
        if (t == null) return nome();
        boolean expansao = t.expansao(bloco.getBlockX(), bloco.getBlockY(), bloco.getBlockZ()) >= 0;
        return Component.text((expansao ? "⚑ Expansão de " : "⚑ Território de ") + t.nomeDono() + " ⚑", Territorios.COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, c, 60, 0.5, 0.8, 0.5, 0.3);
        w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        w.playSound(c, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1.2f);
        if (quem == null) return;
        Territorio t = plugin.territorios().de(quem.getUniqueId());
        int chunks = t == null ? 0 : t.chunks().size();
        boolean expansao = t != null && t.expansao(b.getX(), b.getY(), b.getZ()) >= 0;
        quem.showTitle(Title.title(Component.text(expansao ? "⚑ Expansão fundada! ⚑" : "⚑ Território fundado! ⚑", Territorios.COR, TextDecoration.BOLD),
                Component.text(chunks + " chunks protegidos. Clique no Marco para gerenciar.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(700))));
        plugin.territorios().mostrarBordas(quem, 15);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 6 == 0) centro.getWorld().spawnParticle(Particle.END_ROD, centro.clone().subtract(0, 0.6, 0), 1, 0.15, 0.1, 0.15, 0.01);
    }

    @Override
    protected void devolver(Location centro) {
        Territorio t = territorio(centro.getBlock().getLocation());
        centro.getWorld().dropItemNaturally(centro, t == null ? new ItemStack(Material.WHITE_BANNER) : t.bandeira());
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Territorio t = plugin.territorios().em(b);
        plugin.menusTerritorio().abrirPrincipal(p, t);
    }
}
