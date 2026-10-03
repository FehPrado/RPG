package br.rpgatributos.classe;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Santuário das Classes: um púlpito com 1 livro e pena e 1 diamante jogados em cima.
 * Mostra a árvore de classes; nele se troca de classe e se começa a prova.
 * O /classe abre a mesma árvore em qualquer lugar (só para ver e aceitar caminhos).
 */
public final class Santuarios extends Estacao {

    /** Posição de cada classe na árvore (colunas: a básica em cima, as avançadas embaixo). */
    private static final Map<Classe, Integer> POSICAO = Map.ofEntries(
            Map.entry(Classe.GUERREIRO, 10), Map.entry(Classe.ARQUEIRO, 11), Map.entry(Classe.MAGO, 12),
            Map.entry(Classe.LADINO, 13), Map.entry(Classe.FERREIRO, 14), Map.entry(Classe.DOMADOR, 15),
            Map.entry(Classe.BERSERKER, 19), Map.entry(Classe.CAVALEIRO, 28), Map.entry(Classe.PALADINO, 37),
            Map.entry(Classe.ATIRADOR, 20), Map.entry(Classe.CACADOR, 29),
            Map.entry(Classe.ELEMENTALISTA, 21), Map.entry(Classe.FEITICEIRO, 30),
            Map.entry(Classe.ASSASSINO, 22), Map.entry(Classe.MESTRE_FORJADOR, 23), Map.entry(Classe.MESTRE_DAS_FERAS, 24),
            // lendárias, nas duas colunas da direita
            Map.entry(Classe.HERDEIRO_DO_FERREIRO, 16), Map.entry(Classe.ARQUIMAGO_PRIMORDIAL, 17),
            Map.entry(Classe.SENHOR_DAS_FERAS_ANCESTRAIS, 25), Map.entry(Classe.SENHOR_DA_GUERRA, 26),
            Map.entry(Classe.LAMINA_FANTASMA, 34), Map.entry(Classe.ARQUEIRO_CELESTIAL, 35), Map.entry(Classe.SANTO_PALADINO, 43),
            Map.entry(Classe.SOBERANO_DAS_SOMBRAS, 44));
    private static final int S_PERFIL = 4, S_ABANDONAR = 45, S_FECHAR = 49, S_COMO = 53;
    private static final TextColor COR = TextColor.color(0xFFD54F);

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        /** Aberto num Santuário (pode trocar de classe e começar a prova) ou pelo /classe (null). */
        Location santuario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    public Santuarios(RPGAtributos plugin) {
        super(plugin, "santuarios_classe", "santuario_display");
    }

    private Classes classes() { return plugin.classes(); }

    // =====================================================================
    //  Ritual e aparência
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.LECTERN;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.WRITABLE_BOOK || m == Material.DIAMOND;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.LECTERN ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.WRITABLE_BOOK) < 1 || contar(itens, Material.DIAMOND) < 1) return false;
        tirar(itens, Material.WRITABLE_BOOK, 1);
        tirar(itens, Material.DIAMOND, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.ENCHANTED_BOOK);
    }

    @Override
    protected float escalaItem() {
        return 0.6f;
    }

    @Override
    protected double alturaItem() {
        return 1.5;
    }

    @Override
    protected Component nome() {
        return Component.text("⚜ Santuário das Classes ⚜", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1.2, 0.5);
        w.spawnParticle(Particle.ENCHANT, c, 80, 0.5, 0.8, 0.5, 1);
        w.playSound(c, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.6f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("⚜ Santuário das Classes ⚜", COR, TextDecoration.BOLD),
                Component.text("Escolha o seu caminho", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 3 == 0) centro.getWorld().spawnParticle(Particle.ENCHANT, centro, 3, 0.3, 0.3, 0.3, 0.5);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.WRITABLE_BOOK));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.DIAMOND));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        abrirArvore(p, b.getLocation());
    }

    // =====================================================================
    //  Árvore
    // =====================================================================

    public void abrirArvore(Player p, Location santuario) {
        Tela t = new Tela();
        t.santuario = santuario;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("⚜ Classes"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Classes cl = classes();
        PerfilClasse pf = cl.perfil(p);
        inv.setItem(S_PERFIL, perfil(p, pf));
        for (Map.Entry<Classe, Integer> en : POSICAO.entrySet()) {
            Classe c = en.getKey();
            inv.setItem(en.getValue(), c.tier() == Classe.Tier.LENDARIA ? lendaria(p, pf, c, t.santuario != null) : icone(p, pf, c, t.santuario != null));
        }
        if (pf.alvo != null) {
            inv.setItem(S_ABANDONAR, item(Material.BARRIER, Component.text("Abandonar o caminho de " + pf.alvo.nome(), NamedTextColor.RED), List.of(
                    Component.text("As tarefas voltam para o zero.", NamedTextColor.GRAY),
                    Component.text("Shift + clique para confirmar.", NamedTextColor.GRAY))));
        }
        inv.setItem(S_FECHAR, item(Material.ARROW, Component.text("Fechar", NamedTextColor.YELLOW), List.of()));
        inv.setItem(S_COMO, item(Material.BOOK, Component.text("Como funciona", COR, TextDecoration.BOLD), List.of(
                Component.text("1. Escolha um caminho (clique na classe).", NamedTextColor.GRAY),
                Component.text("2. Faça as tarefas dele.", NamedTextColor.GRAY),
                Component.text("3. Passe na Prova num Santuário.", NamedTextColor.GRAY),
                Component.text("A classe sobe de nível (até 20) com o XP", NamedTextColor.GRAY),
                Component.text("dos atributos dela. No 20 fica Dominada", NamedTextColor.GRAY),
                Component.text("e libera as classes avançadas.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Agache + F: usa a habilidade", NamedTextColor.YELLOW),
                Component.text("Agache + clique esquerdo no ar: troca", NamedTextColor.YELLOW),
                Component.text("Voltar a uma classe aprendida: " + plugin.settings().claCustoTroca + " esmeraldas", NamedTextColor.DARK_GRAY))));
        ItemStack vidro = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private ItemStack perfil(Player p, PerfilClasse pf) {
        Classes cl = classes();
        List<Component> lore = new ArrayList<>();
        Classe a = pf.ativa;
        if (a == null) {
            lore.add(Component.text("Sem classe (Aventureiro).", NamedTextColor.GRAY));
        } else {
            int n = cl.nivel(p, a);
            lore.add(Component.text(a.simbolo() + " " + a.nome() + " — nível " + n + "/" + Classes.NIVEL_MAXIMO
                    + (n >= Classes.NIVEL_MAXIMO ? " (DOMINADA)" : ""), a.cor()));
            if (n < Classes.NIVEL_MAXIMO) lore.add(barra(cl.progresso(p, a)));
            lore.add(Component.text("Sobe com XP de: " + afinidades(a), NamedTextColor.DARK_GRAY));
            Habilidade h = cl.selecionada(p);
            if (h != null) lore.add(Component.text("Habilidade selecionada: " + h.nome(), NamedTextColor.YELLOW));
        }
        List<String> dom = new ArrayList<>();
        for (Classe c : pf.xp.keySet()) if (cl.dominou(p, c)) dom.add(c.nome());
        lore.add(Component.empty());
        lore.add(Component.text("Dominadas: " + (dom.isEmpty() ? "nenhuma" : String.join(", ", dom)), NamedTextColor.GOLD));
        if (pf.alvo != null) {
            lore.add(Component.empty());
            lore.add(Component.text("Caminho: " + pf.alvo.nome(), pf.alvo.cor()));
            for (Tarefa tf : pf.alvo.tarefas()) lore.add(tarefa(p, tf));
        }
        return item(a == null ? Material.PLAYER_HEAD : a.icone(), Component.text("⚜ " + p.getName(), COR, TextDecoration.BOLD), lore);
    }

    private static String afinidades(Classe c) {
        List<String> l = new ArrayList<>();
        for (var s : c.afinidades()) l.add(s.nome());
        return String.join(", ", l);
    }

    private static Component barra(double prog) {
        int cheios = (int) Math.round(prog * 20);
        return Component.text("■".repeat(cheios), NamedTextColor.GREEN).append(Component.text("■".repeat(20 - cheios), NamedTextColor.DARK_GRAY));
    }

    private Component tarefa(Player p, Tarefa t) {
        long feito = Math.min(t.qtd(), classes().progresso(p, t));
        boolean ok = feito >= t.qtd();
        return Component.text((ok ? " ✔ " : " ✖ ") + t.tipo().texto(t.qtd()) + " (" + feito + "/" + t.qtd() + ")",
                ok ? NamedTextColor.GREEN : NamedTextColor.RED);
    }

    private ItemStack icone(Player p, PerfilClasse pf, Classe c, boolean noSantuario) {
        Classes cl = classes();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Classe " + c.tier().nome(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(c.descricao(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Passivo: " + c.passivo().texto(), NamedTextColor.WHITE));
        for (Habilidade h : c.habilidades()) {
            lore.add(Component.text(" ✦ " + h.nome() + " (" + h.recarga() + " s" + (h.mana() > 0 ? ", " + (int) h.mana() + " mana" : "") + ")", c.cor()));
            lore.add(Component.text("   " + h.descricao(), NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        boolean brilha = false;
        Material mat = c.icone();
        NamedTextColor corNome = null;
        if (pf.ativa == c) {
            int n = cl.nivel(p, c);
            lore.add(Component.text("● Sua classe — nível " + n + "/" + Classes.NIVEL_MAXIMO + (n >= Classes.NIVEL_MAXIMO ? " (DOMINADA)" : ""), NamedTextColor.GREEN));
            brilha = true;
        } else if (pf.aprendeu(c)) {
            int n = cl.nivel(p, c);
            lore.add(Component.text("Aprendida — nível " + n + "/" + Classes.NIVEL_MAXIMO + (n >= Classes.NIVEL_MAXIMO ? " (DOMINADA)" : ""), NamedTextColor.AQUA));
            lore.add(noSantuario ? Component.text("» Clique para voltar a ela (" + plugin.settings().claCustoTroca + " esmeraldas)", NamedTextColor.YELLOW)
                    : Component.text("Troque num Santuário das Classes.", NamedTextColor.DARK_GRAY));
        } else if (pf.alvo == c) {
            lore.add(Component.text("Seu caminho atual:", NamedTextColor.GOLD));
            for (Tarefa tf : c.tarefas()) lore.add(tarefa(p, tf));
            lore.add(Component.text("Prova: " + c.prova().descricao(), NamedTextColor.GRAY));
            if (cl.tarefasFeitas(p, c)) {
                lore.add(noSantuario ? Component.text("» Clique para começar a Prova!", NamedTextColor.YELLOW)
                        : Component.text("Vá a um Santuário das Classes para a Prova.", NamedTextColor.YELLOW));
                brilha = true;
            }
        } else {
            List<String> falta = cl.faltando(p, c);
            lore.add(Component.text("Requisitos:", NamedTextColor.GRAY));
            int total = cl.nivelTotalNecessario(c);
            if (total > 0) lore.add(req(plugin.stats().nivelTotal(p) >= total, "Nível total " + total));
            lore.add(req(plugin.stats().getNivel(p, c.atributo()) >= cl.atributoNecessario(c), c.atributo().nome() + " nível " + cl.atributoNecessario(c)));
            for (Classe r : c.requisitos()) lore.add(req(cl.dominou(p, r), r.nome() + " dominada"));
            lore.add(Component.text("Tarefas: ", NamedTextColor.GRAY));
            for (Tarefa tf : c.tarefas()) lore.add(Component.text("  • " + tf.tipo().texto(tf.qtd()), NamedTextColor.GRAY));
            lore.add(Component.text("Prova: " + c.prova().descricao(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            if (falta.isEmpty()) {
                lore.add(Component.text("» Clique para seguir este caminho", NamedTextColor.YELLOW));
            } else {
                mat = Material.GRAY_DYE;
                corNome = NamedTextColor.DARK_GRAY;
            }
        }
        ItemStack i = item(mat, Component.text(c.simbolo() + " " + c.nome(), corNome != null ? corNome : c.cor(), TextDecoration.BOLD), lore);
        if (brilha) i.editMeta(m -> m.setEnchantmentGlintOverride(true));
        return i;
    }

    /** Classe lendária: escondida até o jogador achar o Local Oculto dela (ou alguém já ter a classe). */
    private ItemStack lendaria(Player p, PerfilClasse pf, Classe c, boolean noSantuario) {
        Classes cl = classes();
        var tipo = br.rpgatributos.oculto.TipoLocal.daClasse(c);
        java.util.UUID dono = cl.donoLendaria(c);
        boolean minha = p.getUniqueId().equals(dono);
        boolean soberano = c == Classe.SOBERANO_DAS_SOMBRAS;
        boolean conhece = soberano || minha || dono != null || (tipo != null && plugin.locais().descobriuTipo(p, tipo));
        if (!conhece) {
            return item(Material.BLACK_CANDLE, Component.text("??? Classe lendária esquecida", NamedTextColor.DARK_GRAY, TextDecoration.BOLD), List.of(
                    Component.text("Nenhum santuário ensina este caminho.", NamedTextColor.GRAY),
                    Component.text("Ele repousa num lugar escondido do mundo.", NamedTextColor.GRAY),
                    Component.text("(Mapas Rasgados mostram onde procurar.)", NamedTextColor.DARK_GRAY)));
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("✪ Classe Lendária — só pode haver um", c.cor()));
        lore.add(Component.text(c.descricao(), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Passivo: " + c.passivo().texto(), NamedTextColor.WHITE));
        for (Habilidade h : c.habilidades()) {
            lore.add(Component.text(" ✦ " + h.nome() + " (" + h.recarga() + " s" + (h.mana() > 0 ? ", " + (int) h.mana() + " mana" : "") + ")", c.cor()));
            lore.add(Component.text("   " + h.descricao(), NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        if (tipo != null) lore.add(Component.text("Selo: " + tipo.nome(), NamedTextColor.GOLD));
        if (soberano) {
            lore.add(Component.text("Origem: um Portal Pesadelo raro (Eco do Soberano)", NamedTextColor.GOLD));
            lore.add(req(cl.dominouAvancada(p), "Uma classe avançada dominada"));
        }
        for (Classe r : c.requisitos()) lore.add(req(cl.dominou(p, r), r.nome() + " dominada"));
        lore.add(req(plugin.stats().getNivel(p, c.atributo()) >= cl.atributoNecessario(c), c.atributo().nome() + " nível " + cl.atributoNecessario(c)));
        lore.add(Component.empty());
        if (minha) {
            int n = cl.nivel(p, c);
            lore.add(Component.text(pf.ativa == c ? "● Sua classe — nível " + n + "/20" : "Sua (nível " + n + "/20)", NamedTextColor.GREEN));
            if (pf.ativa != c) lore.add(noSantuario ? Component.text("» Clique para voltar a ela", NamedTextColor.YELLOW)
                    : Component.text("Troque num Santuário das Classes.", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("Shift + clique: abrir mão (fica livre para outro)", NamedTextColor.DARK_GRAY));
        } else if (dono != null) {
            lore.add(Component.text("Pertence a " + cl.nomeDonoLendaria(c) + ".", NamedTextColor.RED));
        } else {
            lore.add(Component.text(soberano ? "Livre! Feche um Portal Pesadelo com o Eco do Soberano." : "Livre! Vença o guardião e toque o selo.", NamedTextColor.GREEN));
        }
        ItemStack i = item(dono == null || minha ? c.icone() : Material.GRAY_DYE,
                Component.text(c.simbolo() + " " + c.nome(), dono == null || minha ? c.cor() : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore);
        if (pf.ativa == c) i.editMeta(m -> m.setEnchantmentGlintOverride(true));
        return i;
    }

    private static Component req(boolean ok, String texto) {
        return Component.text((ok ? " ✔ " : " ✖ ") + texto, ok ? NamedTextColor.GREEN : NamedTextColor.RED);
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicarMenu(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= topo.getSize()) return;
        int slot = e.getSlot();
        if (slot == S_FECHAR) { p.closeInventory(); return; }
        Classes cl = classes();
        PerfilClasse pf = cl.perfil(p);
        if (slot == S_ABANDONAR && pf.alvo != null) {
            if (!e.isShiftClick()) { erro(p, "Use shift + clique para abandonar o caminho."); return; }
            cl.abandonarCaminho(p);
            p.sendMessage(Component.text("Você abandonou o caminho.", NamedTextColor.GRAY));
            desenhar(p, t);
            return;
        }
        Classe c = null;
        for (Map.Entry<Classe, Integer> en : POSICAO.entrySet()) if (en.getValue() == slot) c = en.getKey();
        if (c == null || pf.ativa == c && !(c.tier() == Classe.Tier.LENDARIA && e.isShiftClick())) return;
        boolean noSantuario = t.santuario != null && eh(t.santuario.getBlock());
        if (c.tier() == Classe.Tier.LENDARIA) {
            if (e.isShiftClick() && p.getUniqueId().equals(cl.donoLendaria(c))) {
                String erro = cl.renunciarLendaria(p, c);
                if (erro != null) erro(p, erro);
                desenhar(p, t);
                return;
            }
            if (!pf.aprendeu(c)) {
                erro(p, "Classes lendárias só vêm do selo do Local Oculto delas.");
                return;
            }
        }
        if (pf.aprendeu(c)) {
            if (!noSantuario) { erro(p, "Troque de classe num Santuário das Classes."); return; }
            String erro = cl.trocarPara(p, c);
            if (erro != null) erro(p, erro);
            desenhar(p, t);
            return;
        }
        if (pf.alvo == c) {
            if (!cl.tarefasFeitas(p, c)) { erro(p, "Termine as tarefas do caminho antes."); return; }
            if (!noSantuario) { erro(p, "A Prova começa num Santuário das Classes."); return; }
            p.closeInventory();
            String erro = plugin.provas().iniciar(p, c);
            if (erro != null) erro(p, erro);
            return;
        }
        if (pf.alvo != null && !e.isShiftClick()) {
            erro(p, "Você já segue o caminho de " + pf.alvo.nome() + ". Shift + clique para trocar (as tarefas zeram).");
            return;
        }
        String erro = cl.aceitarCaminho(p, c);
        if (erro != null) {
            erro(p, erro);
            return;
        }
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        p.sendMessage(Component.text("⚜ Você começou o caminho de " + c.nome() + "! Faça as tarefas e volte a um Santuário para a Prova.", c.cor()));
        desenhar(p, t);
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }
}
