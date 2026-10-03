package br.rpgatributos.alquimia;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.alquimia.ReceitaAlquimica.Aba;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.DadosForja;
import br.rpgatributos.territorio.Flag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A Bancada Alquímica: um suporte de poções onde se jogou 2 garrafas de vidro e 1 pó de blaze.
 * Clique direito abre a bancada (elixires, componentes, gemas, acessórios e engastes);
 * agachado + clique abre o suporte de poções normal.
 */
public final class Alquimia extends Estacao {

    public static final TextColor COR = TextColor.color(0x4FD1A5);
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int S_INFO = 0;
    private static final int[] S_ABAS = {2, 3, 4, 5, 6, 7};
    private static final int S_FECHAR = 49;
    // aba de engaste
    private static final int S_ALVO = 20;
    private static final int[] S_ENGASTES = {23, 24, 25};
    private static final int S_AJUDA = 40;
    private static final int MAX_POR_VEZ = 8;

    /** As abas do menu: as quatro de receitas e a de engaste. */
    private enum Painel {
        ELIXIRES("Elixires", Material.POTION, Aba.ELIXIRES),
        COMPONENTES("Componentes", Material.GLOWSTONE_DUST, Aba.COMPONENTES),
        GEMAS("Gemas", Material.EMERALD, Aba.GEMAS),
        ACESSORIOS("Acessórios", Material.GOLD_NUGGET, Aba.ACESSORIOS),
        ENGASTAR("Engastar", Material.SMITHING_TABLE, null),
        PERGAMINHOS("Pergaminhos", Material.PAPER, null);

        final String nome;
        final Material icone;
        final Aba aba;

        Painel(String nome, Material icone, Aba aba) {
            this.nome = nome;
            this.icone = icone;
            this.aba = aba;
        }
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        /** A bancada onde o jogador está (null = só o livro de receitas). */
        Location bancada;
        Painel painel = Painel.ELIXIRES;
        /** Item escolhido para engastar: posição no inventário do jogador e como ele estava. */
        int alvo = -1;
        ItemStack alvoCopia;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    /** Suporte de poções → quem abriu por último (para dar XP quando a poção ficar pronta). */
    private final Map<Location, UUID> quemUsou = new HashMap<>();

    public Alquimia(RPGAtributos plugin) {
        super(plugin, "bancadas_alquimicas", "alquimia_display");
    }

    // =====================================================================
    //  Ritual: 2 garrafas de vidro + 1 pó de blaze em cima de um suporte de poções
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.BREWING_STAND;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.GLASS_BOTTLE || m == Material.BLAZE_POWDER;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block aqui = item.getLocation().getBlock();
        if (aqui.getType() == Material.BREWING_STAND) return aqui;
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.BREWING_STAND ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.GLASS_BOTTLE) < 2 || contar(itens, Material.BLAZE_POWDER) < 1) return false;
        tirar(itens, Material.GLASS_BOTTLE, 2);
        tirar(itens, Material.BLAZE_POWDER, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        ItemStack i = new ItemStack(Material.POTION);
        i.editMeta(PotionMeta.class, m -> m.setColor(Color.fromRGB(COR.value())));
        return i;
    }

    @Override
    protected float escalaItem() {
        return 0.45f;
    }

    @Override
    protected double alturaItem() {
        return 1.35;
    }

    @Override
    protected Component nome() {
        return Component.text("⚗ Bancada Alquímica ⚗", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.WITCH, c, 40, 0.3, 0.4, 0.3, 0.05);
        w.spawnParticle(Particle.EFFECT, c, 30, 0.3, 0.4, 0.3, 0.1);
        w.playSound(c, Sound.BLOCK_BREWING_STAND_BREW, 1f, 0.8f);
        w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("⚗ Bancada Alquímica ⚗", COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito para usar", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
        plugin.stats().darXp(quem, Skill.ALQUIMIA, 25);
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 2 == 0) centro.getWorld().spawnParticle(Particle.WITCH, centro.clone().add(0, 0.2, 0), 1, 0.15, 0.1, 0.15, 0.01);
        if (ciclo % 12 == 0) centro.getWorld().playSound(centro, Sound.BLOCK_BREWING_STAND_BREW, 0.15f, 1.5f);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.GLASS_BOTTLE, 2));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.BLAZE_POWDER));
        centro.getWorld().playSound(centro, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        if (agachado && b.getState() instanceof BrewingStand suporte) {
            if (plugin.territorios().permite(p, b.getLocation(), Flag.BAUS)) {
                p.openInventory(suporte.getInventory()); // o suporte continua funcionando normal
                return;
            }
            p.sendActionBar(Component.text("✖ O suporte de poções é do dono do território.", NamedTextColor.RED));
        }
        abrirMenu(p, b.getLocation(), Painel.ELIXIRES);
    }

    // =====================================================================
    //  Menu
    // =====================================================================

    /** Abre o livro de receitas (sem bancada: só para ver). */
    public void abrirLivro(Player p) {
        abrirMenu(p, null, Painel.ELIXIRES);
    }

    private void abrirMenu(Player p, Location bancada, Painel painel) {
        Tela t = new Tela();
        t.bancada = bancada;
        t.painel = painel;
        t.inventario = Bukkit.createInventory(t, 54, Component.text(bancada == null ? "⚗ Livro de Alquimia" : "⚗ Bancada Alquímica"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private int nivel(Player p) {
        return plugin.stats().getNivel(p, Skill.ALQUIMIA);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        int nivel = nivel(p);
        int max = plugin.settings().nivelMaximo;
        inv.setItem(S_INFO, item(Material.BREWING_STAND, Component.text("⚗ Alquimia Nv " + nivel, COR, TextDecoration.BOLD), List.of(
                Component.text("Elixires duram +" + StatsManager.fmt(nivel * plugin.settings().alqDuracaoExtra * 100) + "%.", NamedTextColor.GREEN),
                Component.text(StatsManager.fmt(nivel * plugin.settings().alqChanceExtra * 100) + "% de chance de render", NamedTextColor.GREEN),
                Component.text("o dobro de componentes.", NamedTextColor.GREEN),
                Component.empty(),
                Component.text("Peixes raros e colheitas de qualidade", NamedTextColor.GRAY),
                Component.text("(★★+) deixam os elixires melhores.", NamedTextColor.GRAY),
                Component.empty(),
                t.bancada == null ? Component.text("Para criar, use uma Bancada Alquímica.", NamedTextColor.DARK_GRAY)
                        : Component.text("Shift + clique: cria até " + MAX_POR_VEZ + " de uma vez.", NamedTextColor.YELLOW)), false));
        Painel[] paineis = Painel.values();
        for (int i = 0; i < paineis.length; i++) {
            Painel pn = paineis[i];
            boolean aberto = pn == t.painel;
            inv.setItem(S_ABAS[i], item(pn.icone, Component.text(pn.nome, aberto ? COR : NamedTextColor.GRAY, TextDecoration.BOLD),
                    List.of(aberto ? Component.text("▶ Aberta", COR) : Component.text("» Clique para abrir", NamedTextColor.YELLOW)), aberto));
        }
        if (t.painel == Painel.ENGASTAR) desenharEngaste(p, t);
        else if (t.painel == Painel.PERGAMINHOS) desenharPergaminhos(p, t, nivel, max);
        else desenharReceitas(p, t, nivel, max);
        inv.setItem(S_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of(), false));
        ItemStack vidro = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        ItemStack escuro = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        escuro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, i >= 9 && i < 18 ? escuro : vidro);
        }
    }

    private void desenharReceitas(Player p, Tela t, int nivel, int max) {
        List<ReceitaAlquimica> receitas = ReceitaAlquimica.daAba(t.painel.aba);
        PlayerInventory pinv = p.getInventory();
        for (int i = 0; i < receitas.size() && i < SLOTS.length; i++) {
            ReceitaAlquimica r = receitas.get(i);
            boolean liberado = nivel >= r.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((liberado ? "✔" : "✖") + " Alquimia nível " + r.nivelNecessario(max),
                    liberado ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Ingredientes:", NamedTextColor.GRAY));
            boolean temTudo = true;
            for (Ingrediente ing : r.ingredientes()) {
                boolean ok = ing.contar(pinv) >= ing.qtd();
                temTudo &= ok;
                NamedTextColor cor = ok ? NamedTextColor.GREEN : NamedTextColor.RED;
                lore.add(Component.text((ok ? " ✔ " : " ✖ ") + ing.qtd() + "x ", cor).append(ing.nome().color(cor)));
            }
            lore.add(Component.empty());
            lore.addAll(efeito(r));
            lore.add(Component.empty());
            if (t.bancada == null) lore.add(Component.text("Crie numa Bancada Alquímica.", NamedTextColor.DARK_GRAY));
            else if (liberado && temTudo) lore.add(Component.text("» Clique para criar", NamedTextColor.YELLOW));
            else if (!liberado) lore.add(Component.text("Suba a Alquimia para liberar.", NamedTextColor.DARK_GRAY));
            else lore.add(Component.text("Faltam ingredientes.", NamedTextColor.DARK_GRAY));

            ItemStack icone = liberado ? r.previa() : new ItemStack(Material.GRAY_DYE);
            boolean brilha = liberado && temTudo && t.bancada != null;
            icone.editMeta(m -> {
                m.displayName(Component.text(r.nome(), liberado ? COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
                m.addItemFlags(ItemFlag.values());
                if (brilha) m.setEnchantmentGlintOverride(true);
            });
            icone.setAmount(1);
            t.inventario.setItem(SLOTS[i], icone);
        }
    }

    /** O que a receita faz, para mostrar no menu. */
    private List<Component> efeito(ReceitaAlquimica r) {
        if (r.elixir() != null) {
            List<Component> l = new ArrayList<>();
            l.add(Component.text("Efeitos (qualidade normal):", NamedTextColor.GRAY));
            l.addAll(r.elixir().descrever(1));
            return l;
        }
        if (r.gema() != null) return r.gema().descricao(r.grau());
        if (r.acessorio() != null) {
            return List.of(Component.text(r.acessorio().tipo().nome() + ":", NamedTextColor.GRAY),
                    Component.text(" ✧ " + r.acessorio().efeito(), r.acessorio().cor()));
        }
        return List.of(Component.text("Componente para outras receitas.", NamedTextColor.GRAY));
    }

    private void desenharEngaste(Player p, Tela t) {
        Inventory inv = t.inventario;
        ItemStack alvo = alvoValido(p, t);
        DadosForja d = alvo == null ? null : plugin.forja().ler(alvo);
        if (alvo == null || d == null) {
            t.alvo = -1;
            inv.setItem(S_ALVO, item(Material.ITEM_FRAME, Component.text("Escolha um item forjado", COR, TextDecoration.BOLD), List.of(
                    Component.text("Clique numa arma, ferramenta ou", NamedTextColor.GRAY),
                    Component.text("armadura forjada do SEU inventário", NamedTextColor.WHITE),
                    Component.text("(aqui embaixo).", NamedTextColor.GRAY)), false));
        } else {
            ItemStack mostra = alvo.clone();
            mostra.setAmount(1);
            inv.setItem(S_ALVO, mostra);
        }
        int engastes = d == null ? 0 : d.engastes();
        Categoria cat = alvo == null ? null : Categoria.de(alvo.getType());
        for (int i = 0; i < S_ENGASTES.length; i++) {
            if (d == null) {
                inv.setItem(S_ENGASTES[i], item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("◇ Engaste", NamedTextColor.GRAY), List.of(
                        Component.text("Escolha um item primeiro.", NamedTextColor.DARK_GRAY)), false));
            } else if (i >= engastes) {
                inv.setItem(S_ENGASTES[i], item(Material.BARRIER, Component.text("✖ Sem engaste", NamedTextColor.RED), List.of(
                        Component.text("Itens " + d.raridade().nome() + " têm " + engastes + " engaste(s).", NamedTextColor.GRAY)), false));
            } else if (i < d.gemas().size()) {
                DadosForja.Engaste en = d.gemas().get(i);
                ItemStack gema = en.gema().criar(en.grau(), 1);
                Gema.Bonus b = cat == null ? null : en.gema().bonus(cat);
                gema.editMeta(m -> m.lore(List.of(
                        cat == Categoria.CAJADO ? Component.text("✧ +" + Math.round(en.gema().potenciaElemental(en.grau()) * 100) + "% magias de " + en.gema().nomesElementos(), NamedTextColor.LIGHT_PURPLE)
                        : b == null ? Component.text("Não faz nada nesse item.", NamedTextColor.DARK_GRAY)
                                : Component.text("✧ " + b.stat().formatar(b.valor(en.grau())) + " " + b.stat().nome(), NamedTextColor.WHITE),
                        Component.empty(),
                        Component.text("» Clique para tirar a gema", NamedTextColor.YELLOW),
                        Component.text("  (gasta 1 Solvente Alquímico)", NamedTextColor.DARK_GRAY))
                        .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList()));
                inv.setItem(S_ENGASTES[i], gema);
            } else {
                inv.setItem(S_ENGASTES[i], item(Material.WHITE_STAINED_GLASS_PANE, Component.text("◇ Engaste vazio", NamedTextColor.WHITE), List.of(
                        Component.text("Clique numa gema do seu inventário", NamedTextColor.GRAY),
                        Component.text("para engastar aqui.", NamedTextColor.GRAY)), false));
            }
        }
        inv.setItem(S_AJUDA, item(Material.OAK_SIGN, Component.text("Como funciona", COR, TextDecoration.BOLD), List.of(
                Component.text("Itens forjados têm engastes:", NamedTextColor.GRAY),
                Component.text(" Raro e Épico: 1", NamedTextColor.WHITE),
                Component.text(" Único e Lendário: 2", NamedTextColor.WHITE),
                Component.text(" Mítico: 3", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Cada gema dá um bônus diferente", NamedTextColor.GRAY),
                Component.text("em armas, arcos, armaduras e ferramentas.", NamedTextColor.GRAY),
                Component.text("Tirar uma gema gasta 1 Solvente", NamedTextColor.GRAY),
                Component.text("Alquímico e devolve a gema inteira.", NamedTextColor.GRAY)), false));
    }

    /** O item escolhido para engastar, se ele ainda está no mesmo lugar do inventário. */
    private static ItemStack alvoValido(Player p, Tela t) {
        if (t.alvo < 0 || t.alvoCopia == null) return null;
        ItemStack atual = p.getInventory().getItem(t.alvo);
        return atual != null && atual.isSimilar(t.alvoCopia) ? atual : null;
    }

    // =====================================================================
    //  Criar
    // =====================================================================

    private boolean pertoDaBancada(Player p, Tela t) {
        if (t.bancada == null) {
            erro(p, "Para criar, use uma Bancada Alquímica.");
            return false;
        }
        if (!eh(t.bancada.getBlock())) {
            erro(p, "A Bancada Alquímica não existe mais.");
            p.closeInventory();
            return false;
        }
        if (!p.getWorld().equals(t.bancada.getWorld())
                || p.getLocation().distanceSquared(t.bancada.clone().add(0.5, 0.5, 0.5)) > 8 * 8) {
            erro(p, "Você está longe da Bancada Alquímica.");
            p.closeInventory();
            return false;
        }
        return true;
    }

    private void criar(Player p, Tela t, ReceitaAlquimica r, boolean varias) {
        if (!pertoDaBancada(p, t)) return;
        int max = plugin.settings().nivelMaximo;
        int nivel = nivel(p);
        if (nivel < r.nivelNecessario(max)) {
            erro(p, "Precisa de Alquimia nível " + r.nivelNecessario(max) + ".");
            return;
        }
        PlayerInventory inv = p.getInventory();
        int vezes = varias ? MAX_POR_VEZ : 1;
        for (Ingrediente ing : r.ingredientes()) vezes = Math.min(vezes, ing.contar(inv) / ing.qtd());
        if (vezes <= 0) {
            erro(p, "Faltam ingredientes.");
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        List<ItemStack> feitos = new ArrayList<>();
        Qualidade melhor = Qualidade.NORMAL;
        for (int v = 0; v < vezes; v++) {
            int comQualidade = 0, otimos = 0, perfeitos = 0;
            for (Ingrediente ing : r.ingredientes()) {
                List<Qualidade> qs = ing.tirar(inv);
                if (!ing.temQualidade()) continue;
                for (Qualidade q : qs) {
                    comQualidade++;
                    if (q.ordinal() >= Qualidade.OTIMA.ordinal()) otimos++;
                    if (q == Qualidade.PERFEITA) perfeitos++;
                }
            }
            if (r.elixir() != null) {
                Qualidade q = Qualidade.sortear(nivel / (double) max);
                if (comQualidade > 0 && perfeitos == comQualidade) q = Qualidade.PERFEITA;
                else if (comQualidade > 0 && otimos * 2 >= comQualidade) q = q.acima();
                if (q.ordinal() > melhor.ordinal()) melhor = q;
                double fator = q.fator() * (1 + nivel * plugin.settings().alqDuracaoExtra);
                feitos.add(r.elixir().criar(q, fator, p.getName()));
            } else if (r.reagente() != null) {
                int qtd = r.qtd();
                if (ThreadLocalRandom.current().nextDouble() < nivel * plugin.settings().alqChanceExtra) qtd *= 2;
                feitos.add(r.reagente().criar(qtd));
            } else if (r.gema() != null) {
                feitos.add(r.gema().criar(r.grau(), 1));
                if (r.grau() == Gema.GRAU_MAXIMO) plugin.titulos().registrar(p, "gema_perfeita", 1);
            } else {
                feitos.add(r.acessorio().criar());
                plugin.titulos().registrar(p, "acessorios", 1);
            }
        }
        for (ItemStack i : feitos) inv.addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));

        Location c = t.bancada.clone().add(0.5, 1, 0.5);
        c.getWorld().spawnParticle(Particle.WITCH, c, 15, 0.25, 0.3, 0.25, 0.05);
        c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c, 8, 0.3, 0.3, 0.3);
        p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
        if (r.gema() != null || r.acessorio() != null) p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.1f);
        Component msg = Component.text("⚗ Você criou ", COR).append(Component.text((vezes > 1 ? vezes + "x " : "") + r.nome(), NamedTextColor.WHITE));
        if (r.elixir() != null && melhor != Qualidade.NORMAL) msg = msg.append(Component.text(" (até " + melhor.nome() + ")", melhor.cor()));
        p.sendMessage(msg);
        if (!criativo) plugin.stats().darXp(p, Skill.ALQUIMIA, r.xp() * vezes);
        plugin.titulos().registrar(p, "alquimias", vezes);
        desenhar(p, t);
    }

    // =====================================================================
    //  Pergaminhos (uma magia do grimório guardada num papel)
    // =====================================================================

    private static final double NIVEL_PERGAMINHO = 0.2;
    private static final List<Ingrediente> CUSTO_PERGAMINHO = List.of(
            Ingrediente.m(Material.PAPER, 1), Ingrediente.m(Material.INK_SAC, 1), Ingrediente.r(Reagente.PO_ARCANO, 1));

    private void desenharPergaminhos(Player p, Tela t, int nivel, int max) {
        var arc = plugin.arcano();
        var pf = arc.perfil(p);
        int nivelNecessario = (int) Math.ceil(NIVEL_PERGAMINHO * max);
        boolean liberado = nivel >= nivelNecessario;
        boolean temTudo = true;
        List<Component> custo = new ArrayList<>();
        for (Ingrediente ing : CUSTO_PERGAMINHO) {
            boolean ok = ing.contar(p.getInventory()) >= ing.qtd();
            temTudo &= ok;
            NamedTextColor cor = ok ? NamedTextColor.GREEN : NamedTextColor.RED;
            custo.add(Component.text((ok ? " ✔ " : " ✖ ") + ing.qtd() + "x ", cor).append(ing.nome().color(cor)));
        }
        for (int i = 0; i < br.rpgatributos.arcano.Perfil.MAX_MAGIAS; i++) {
            var m = pf.magia(i);
            if (m == null) {
                t.inventario.setItem(SLOTS[i], item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Espaço vazio do grimório", NamedTextColor.GRAY),
                        List.of(Component.text("Crie magias no grimório para", NamedTextColor.DARK_GRAY),
                                Component.text("poder escrevê-las aqui.", NamedTextColor.DARK_GRAY)), false));
                continue;
            }
            int mana = (int) Math.round(arc.custoMana(m, pf) * 2);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((liberado ? "✔" : "✖") + " Alquimia nível " + nivelNecessario, liberado ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Custa:", NamedTextColor.GRAY));
            lore.addAll(custo);
            boolean manaOk = pf.mana() >= mana;
            lore.add(Component.text((manaOk ? " ✔ " : " ✖ ") + mana + " de mana", manaOk ? NamedTextColor.AQUA : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Vira um pergaminho de uso único que", NamedTextColor.GRAY));
            lore.add(Component.text("qualquer um lança, sem mana nem essências.", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(t.bancada == null ? Component.text("Escreva numa Bancada Alquímica.", NamedTextColor.DARK_GRAY)
                    : Component.text("» Clique para escrever", NamedTextColor.YELLOW));
            boolean brilha = liberado && temTudo && manaOk && t.bancada != null;
            t.inventario.setItem(SLOTS[i], item(Material.PAPER, Component.text(m.nome(), COR, TextDecoration.BOLD), lore, brilha));
        }
        t.inventario.setItem(S_AJUDA, item(Material.OAK_SIGN, Component.text("Pergaminhos", COR, TextDecoration.BOLD), List.of(
                Component.text("Escolha uma magia do seu grimório.", NamedTextColor.GRAY),
                Component.text("O pergaminho guarda a força que a", NamedTextColor.GRAY),
                Component.text("magia tem agora, para você ou para", NamedTextColor.GRAY),
                Component.text("dar a um amigo.", NamedTextColor.GRAY)), false));
    }

    private void escreverPergaminho(Player p, Tela t, int espaco) {
        if (!pertoDaBancada(p, t)) return;
        int max = plugin.settings().nivelMaximo;
        int nivelNecessario = (int) Math.ceil(NIVEL_PERGAMINHO * max);
        if (nivel(p) < nivelNecessario) {
            erro(p, "Precisa de Alquimia nível " + nivelNecessario + ".");
            return;
        }
        var arc = plugin.arcano();
        var pf = arc.perfil(p);
        var m = pf.magia(espaco);
        if (m == null) return;
        PlayerInventory inv = p.getInventory();
        for (Ingrediente ing : CUSTO_PERGAMINHO) {
            if (ing.contar(inv) < ing.qtd()) {
                erro(p, "Faltam materiais: papel, saco de tinta e Pó Arcano.");
                return;
            }
        }
        double mana = arc.custoMana(m, pf) * 2;
        if (pf.mana() < mana) {
            erro(p, "Precisa de " + (int) mana + " de mana para escrever esse pergaminho.");
            return;
        }
        for (Ingrediente ing : CUSTO_PERGAMINHO) ing.tirar(inv);
        arc.gastarMana(p, pf, mana);
        ItemStack pergaminho = br.rpgatributos.arcano.ItensMagicos.criarPergaminho(m, arc.potencia(p, pf, m), p.getName());
        inv.addItem(pergaminho).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.4f);
        p.sendMessage(Component.text("✎ Pergaminho escrito: ", COR).append(Component.text(m.nome(), NamedTextColor.WHITE)));
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.ALQUIMIA, 20);
        desenhar(p, t);
    }

    // =====================================================================
    //  Engastar
    // =====================================================================

    private void escolherAlvo(Player p, Tela t, int slot) {
        ItemStack item = p.getInventory().getItem(slot);
        DadosForja d = plugin.forja().ler(item);
        if (d == null) {
            erro(p, "Escolha um item forjado (armas, ferramentas e armaduras feitas na Forja do Ferreiro).");
            return;
        }
        t.alvo = slot;
        t.alvoCopia = item.clone();
        if (d.engastes() == 0) p.sendActionBar(Component.text("Itens Comuns não têm engastes.", NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        desenhar(p, t);
    }

    private void engastar(Player p, Tela t, int slotGema) {
        if (!pertoDaBancada(p, t)) return;
        ItemStack alvo = alvoValido(p, t);
        if (alvo == null) {
            erro(p, "Escolha primeiro o item forjado (clique nele no seu inventário).");
            return;
        }
        DadosForja d = plugin.forja().ler(alvo);
        ItemStack gemaItem = p.getInventory().getItem(slotGema);
        Gema g = Gema.de(gemaItem);
        if (d == null || g == null) return;
        if (d.gemas().size() >= d.engastes()) {
            erro(p, d.engastes() == 0 ? "Esse item não tem engastes." : "Todos os engastes desse item estão ocupados.");
            return;
        }
        Categoria cat = Categoria.de(alvo.getType());
        if (cat == null || (g.bonus(cat) == null && cat != Categoria.CAJADO)) {
            erro(p, "O " + g.nome() + " não serve nesse tipo de item.");
            return;
        }
        int grau = Gema.grau(gemaItem);
        gemaItem.setAmount(gemaItem.getAmount() - 1);
        p.getInventory().setItem(slotGema, gemaItem.getAmount() > 0 ? gemaItem : null);
        List<DadosForja.Engaste> novas = new ArrayList<>(d.gemas());
        novas.add(new DadosForja.Engaste(g, grau));
        atualizarAlvo(p, t, alvo, d.comGemas(novas));
        p.playSound(p.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        p.sendMessage(Component.text("◆ " + g.nome(grau) + " engastado!", g.cor()));
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.ALQUIMIA, 10 * grau);
        desenhar(p, t);
    }

    private void desengastar(Player p, Tela t, int indice) {
        if (!pertoDaBancada(p, t)) return;
        ItemStack alvo = alvoValido(p, t);
        DadosForja d = alvo == null ? null : plugin.forja().ler(alvo);
        if (d == null || indice >= d.gemas().size()) return;
        PlayerInventory inv = p.getInventory();
        if (p.getGameMode() != GameMode.CREATIVE) {
            Ingrediente solvente = Ingrediente.r(Reagente.SOLVENTE, 1);
            if (solvente.contar(inv) < 1) {
                erro(p, "Precisa de 1 Solvente Alquímico para tirar a gema.");
                return;
            }
            solvente.tirar(inv);
        }
        List<DadosForja.Engaste> novas = new ArrayList<>(d.gemas());
        DadosForja.Engaste tirada = novas.remove(indice);
        atualizarAlvo(p, t, alvo, d.comGemas(novas));
        inv.addItem(tirada.gema().criar(tirada.grau(), 1)).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.3f);
        p.sendMessage(Component.text("◇ " + tirada.gema().nome(tirada.grau()) + " tirado do item.", NamedTextColor.GRAY));
        desenhar(p, t);
    }

    private void atualizarAlvo(Player p, Tela t, ItemStack alvo, DadosForja novos) {
        ItemStack novo = alvo.clone();
        plugin.forja().construir(novo, novos);
        p.getInventory().setItem(t.alvo, novo);
        t.alvoCopia = novo.clone();
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        boolean noMenu = e.getRawSlot() < topo.getSize();
        int slot = e.getSlot();

        if (!noMenu) {
            // Inventário do jogador: só a aba de engaste usa.
            if (t.painel != Painel.ENGASTAR) return;
            ItemStack clicado = e.getCurrentItem();
            if (clicado == null || clicado.isEmpty()) return;
            if (t.bancada == null) {
                erro(p, "Para engastar, use uma Bancada Alquímica.");
                return;
            }
            if (Gema.de(clicado) != null) engastar(p, t, slot);
            else escolherAlvo(p, t, slot);
            return;
        }

        if (slot == S_FECHAR) {
            p.closeInventory();
            return;
        }
        for (int i = 0; i < S_ABAS.length; i++) {
            if (S_ABAS[i] == slot) {
                t.painel = Painel.values()[i];
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                desenhar(p, t);
                return;
            }
        }
        if (t.painel == Painel.PERGAMINHOS) {
            for (int i = 0; i < br.rpgatributos.arcano.Perfil.MAX_MAGIAS; i++) {
                if (SLOTS[i] == slot) { escreverPergaminho(p, t, i); return; }
            }
            return;
        }
        if (t.painel == Painel.ENGASTAR) {
            for (int i = 0; i < S_ENGASTES.length; i++) {
                if (S_ENGASTES[i] == slot) {
                    desengastar(p, t, i);
                    return;
                }
            }
            return;
        }
        List<ReceitaAlquimica> receitas = ReceitaAlquimica.daAba(t.painel.aba);
        for (int i = 0; i < receitas.size() && i < SLOTS.length; i++) {
            if (SLOTS[i] == slot) {
                criar(p, t, receitas.get(i), e.isShiftClick());
                return;
            }
        }
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Beber e o suporte de poções comum
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoBeber(PlayerItemConsumeEvent e) {
        Elixir x = Elixir.de(e.getItem());
        if (x == null) return;
        if (x.mana() > 0) plugin.arcano().darMana(e.getPlayer(), x.mana());
        if (x == Elixir.PURIFICACAO) plugin.maldicoes().curar(e.getPlayer());
        plugin.titulos().registrar(e.getPlayer(), "elixires", 1);
    }

    @EventHandler
    public void aoAbrirSuporte(InventoryOpenEvent e) {
        if (e.getInventory() instanceof BrewerInventory bi && bi.getHolder() instanceof BrewingStand bs) {
            quemUsou.put(bs.getLocation(), e.getPlayer().getUniqueId());
        }
    }

    /** Poção comum pronta no suporte: XP de Alquimia para quem usou. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoFicarPronta(BrewEvent e) {
        UUID id = quemUsou.get(e.getBlock().getLocation());
        Player p = id == null ? null : Bukkit.getPlayer(id);
        if (p == null || !p.getWorld().equals(e.getBlock().getWorld())
                || p.getLocation().distanceSquared(e.getBlock().getLocation()) > 32 * 32) return;
        int pocoes = 0;
        for (ItemStack i : e.getResults()) if (i != null && !i.isEmpty()) pocoes++;
        if (pocoes > 0) plugin.stats().darXp(p, Skill.ALQUIMIA, pocoes * plugin.settings().alqXpPocaoComum);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilhar) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return i;
    }
}
