package br.rpgatributos;

import br.rpgatributos.forja.Forja;
import br.rpgatributos.forja.Raridade;
import com.google.common.collect.ImmutableMultimap;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menus de baú: tela de atributos e tela de cosméticos (chapéu e tag).
 * Nenhum item pode ser tirado dos menus; tudo é botão.
 */
public final class MenuManager implements Listener {

    private enum Tipo { PRINCIPAL, COSMETICOS, FORJA }

    /** Marca um inventário como menu nosso. */
    private static final class Menu implements InventoryHolder {
        final Tipo tipo;
        final UUID alvo;
        Inventory inventario;

        Menu(Tipo tipo, UUID alvo) {
            this.tipo = tipo;
            this.alvo = alvo;
        }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    // ----- posições no menu principal (6 linhas) -----
    private static final int P_CABECA = 4;
    /**
     * Na ordem de Skill.values(). Linha de cima: Mineração, Madeira, Corrida, Pulo, Natação,
     * Combate, Agricultura. Embaixo, em destaque: Ferraria, Arcano, Culinária e Doma.
     */
    private static final int[] P_SKILLS = {19, 20, 21, 22, 24, 28, 30, 23, 25, 32, 34, 29, 33};
    private static final int P_PARTY = 46;
    private static final int P_CLASSE = 47;
    private static final int P_COSMETICOS = 48;
    private static final int P_FORJA = 49;
    private static final int P_FECHAR = 50;
    private static final int P_ACESSORIOS = 51;
    private static final int P_TERRITORIO = 52;
    private static final int P_TALENTOS = 45;

    // ----- posições no guia da forja (6 linhas) -----
    private static final int F_FERRARIA = 13;
    private static final int[] F_RARIDADES = {10, 11, 12, 14, 15, 16};
    private static final int F_COMO = 29;
    private static final int F_EFEITOS = 31;
    private static final int F_MESA = 33;
    private static final int F_VOLTAR = 49;

    // ----- posições no menu de cosméticos (4 linhas) -----
    private static final int C_DICA_CHAPEU = 10;
    private static final int C_CHAPEU_ATUAL = 11;
    private static final int C_TAG_ATUAL = 15;
    private static final int C_TAG_REMOVER = 16;
    private static final int C_GUARDA_ROUPA = 13;
    private static final int C_VOLTAR = 31;

    private static final long TEMPO_PARA_DIGITAR_MS = 60_000;

    private final RPGAtributos plugin;
    /** Jogadores que clicaram em "trocar tag" e estão digitando no chat (UUID → expira em). */
    private final Map<UUID, Long> esperandoTag = new ConcurrentHashMap<>();

    public MenuManager(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Abrir menus
    // =====================================================================

    public void abrirPrincipal(Player quemVe, Player alvo) {
        Menu menu = new Menu(Tipo.PRINCIPAL, alvo.getUniqueId());
        boolean proprio = quemVe.equals(alvo);
        Component titulo = Component.text(proprio ? "Seus Atributos" : "Atributos de " + alvo.getName());
        Inventory inv = Bukkit.createInventory(menu, 54, titulo);
        menu.inventario = inv;

        StatsManager st = plugin.stats();
        int max = plugin.settings().nivelMaximo;

        // Cabeça do jogador com o resumo
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(alvo));
        List<Component> resumo = new ArrayList<>();
        resumo.add(linha("Nível total: ", NamedTextColor.GRAY, String.valueOf(st.nivelTotal(alvo)), NamedTextColor.YELLOW));
        int renasceu = plugin.renascimento().vezes(alvo);
        if (renasceu > 0) resumo.add(linha("Renascimentos: ", NamedTextColor.GRAY, "★".repeat(renasceu), NamedTextColor.GOLD));
        resumo.add(Component.empty());
        for (Skill s : Skill.values()) {
            resumo.add(Component.text(s.icone() + " " + s.nome() + ": ", s.cor())
                    .append(Component.text("Nv " + st.getNivel(alvo, s), NamedTextColor.WHITE)));
        }
        String tag = plugin.tags().getTag(alvo);
        if (tag != null) {
            resumo.add(Component.empty());
            resumo.add(Component.text("Tag: ", NamedTextColor.GRAY).append(LegacyComponentSerializer.legacyAmpersand().deserialize(tag)));
        }
        inv.setItem(P_CABECA, enfeitar(cabeca, Component.text(alvo.getName(), NamedTextColor.GOLD, TextDecoration.BOLD), resumo, false));

        // Um ícone por atributo
        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length && i < P_SKILLS.length; i++) {
            inv.setItem(P_SKILLS[i], iconeSkill(alvo, skills[i], max));
        }

        if (proprio && (quemVe.hasPermission("rpg.chapeu") || quemVe.hasPermission("rpg.tag"))) {
            inv.setItem(P_COSMETICOS, item(Material.LEATHER_HELMET,
                    Component.text("✦ Cosméticos", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
                    List.of(Component.text("Item na cabeça e tag personalizada.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        }
        if (proprio) {
            var pt = plugin.parties().party(quemVe);
            inv.setItem(P_PARTY, item(Material.CAKE,
                    Component.text("☺ Party", TextColor.color(0x55CDFC), TextDecoration.BOLD),
                    List.of(Component.text(pt == null ? "Jogue em grupo: XP dividido," : "Party de " + pt.nomeLider()
                                    + " (" + pt.tamanho() + " membros)", NamedTextColor.GRAY),
                            Component.text(pt == null ? "sem fogo amigo e chat próprio." : "Membros, chat e fogo amigo.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
            var classe = plugin.classes().ativa(quemVe);
            inv.setItem(P_CLASSE, item(classe == null ? Material.BOOK : classe.icone(),
                    Component.text("⚜ Classe", TextColor.color(0xFFD54F), TextDecoration.BOLD),
                    List.of(Component.text(classe == null ? "Sem classe: escolha um caminho." : classe.nome() + " — nível "
                                    + plugin.classes().nivel(quemVe, classe) + "/20", NamedTextColor.GRAY),
                            Component.text("Agache + F: habilidade", NamedTextColor.DARK_GRAY),
                            Component.empty(),
                            Component.text("» Clique para ver a árvore", NamedTextColor.YELLOW))));
            var terr = plugin.territorios().de(quemVe.getUniqueId());
            inv.setItem(P_TERRITORIO, item(Material.LODESTONE,
                    Component.text("⚑ Território", TextColor.color(0x4CAF50), TextDecoration.BOLD),
                    List.of(Component.text(terr == null ? "Proteja sua base com um" : terr.chunks().size() + " chunks protegidos.", NamedTextColor.GRAY),
                            Component.text(terr == null ? "Marco do Território." : "Mapa, membros e regras.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
            inv.setItem(P_ACESSORIOS, item(Material.GOLD_NUGGET,
                    Component.text("❖ Acessórios", TextColor.color(0xE8C15A), TextDecoration.BOLD),
                    List.of(Component.text("Anéis, amuletos, cintos e", NamedTextColor.GRAY),
                            Component.text("itens de bolso (lanterna, ímã...).", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
            int livres = plugin.talentos().livres(quemVe);
            inv.setItem(P_TALENTOS, item(Material.EXPERIENCE_BOTTLE,
                    Component.text("✦ Talentos", TextColor.color(0xB39DDB), TextDecoration.BOLD),
                    List.of(Component.text(plugin.talentos().resumo(quemVe), livres > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                            Component.text("Guerreiro, Arcano, Artesão e Explorador.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
            inv.setItem(P_FORJA, item(Material.SMITHING_TABLE,
                    Component.text("⚒ Forja", Skill.FERRARIA.cor(), TextDecoration.BOLD),
                    List.of(Component.text("Raridades, chances e efeitos", NamedTextColor.GRAY),
                            Component.text("dos itens forjados.", NamedTextColor.GRAY),
                            Component.empty(),
                            Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        }
        inv.setItem(P_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));

        preencherBorda(inv);
        quemVe.openInventory(inv);
    }

    public void abrirCosmeticos(Player p) {
        Menu menu = new Menu(Tipo.COSMETICOS, p.getUniqueId());
        Inventory inv = Bukkit.createInventory(menu, 36, Component.text("Cosméticos"));
        menu.inventario = inv;
        desenharCosmeticos(inv, p);
        p.openInventory(inv);
    }

    public void abrirForja(Player p) {
        Menu menu = new Menu(Tipo.FORJA, p.getUniqueId());
        Inventory inv = Bukkit.createInventory(menu, 54, Component.text("⚒ Forja"));
        menu.inventario = inv;

        Forja f = plugin.forja();
        int nivel = plugin.stats().getNivel(p, Skill.FERRARIA);
        int max = plugin.settings().nivelMaximo;

        // Centro: a Ferraria do jogador
        List<Component> lore = new ArrayList<>();
        lore.add(linha("Nível ", NamedTextColor.GRAY, nivel + " / " + max, NamedTextColor.WHITE));
        lore.add(barra(plugin.stats().progresso(p, Skill.FERRARIA)));
        lore.add(Component.empty());
        lore.add(linha("Épico ou melhor: ", NamedTextColor.GRAY,
                Forja.pct(f.chancePeloMenos(Raridade.EPICO, nivel)), NamedTextColor.WHITE));
        lore.add(linha("Lendário ou melhor: ", NamedTextColor.GRAY,
                Forja.pct(f.chancePeloMenos(Raridade.LENDARIO, nivel)), NamedTextColor.WHITE));
        lore.add(linha("Bônus mais fortes: ", NamedTextColor.GRAY,
                "+" + Forja.pct(f.bonusQualidade(nivel)), NamedTextColor.GREEN));
        lore.add(Component.empty());
        lore.add(Component.text("Como evoluir:", NamedTextColor.GRAY));
        lore.add(Component.text(" Fabrique armas, ferramentas e armaduras,", NamedTextColor.WHITE));
        lore.add(Component.text(" melhore itens para Netherite e funda lingotes.", NamedTextColor.WHITE));
        inv.setItem(F_FERRARIA, enfeitar(new ItemStack(Material.ANVIL),
                Component.text("⚒ Sua Ferraria", Skill.FERRARIA.cor(), TextDecoration.BOLD), lore, false));

        Raridade[] raridades = Raridade.values();
        for (int i = 0; i < raridades.length && i < F_RARIDADES.length; i++) {
            inv.setItem(F_RARIDADES[i], iconeRaridade(raridades[i], nivel, max));
        }

        inv.setItem(F_COMO, item(Material.CRAFTING_TABLE,
                Component.text("Como forjar", NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(Component.text("Crie uma Forja do Ferreiro: jogue", NamedTextColor.GRAY),
                        Component.text("2 blocos de ferro e 1 balde de lava", NamedTextColor.WHITE),
                        Component.text("em cima de uma bigorna.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Clique direito nela abre a bancada do", NamedTextColor.GRAY),
                        Component.text("ferreiro: armas, ferramentas e armaduras", NamedTextColor.GRAY),
                        Component.text("saem com raridade, bônus e efeitos.", NamedTextColor.GRAY),
                        Component.text("Na bancada comum, saem normais.", NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text("Agachado + clique na Forja: refinar", NamedTextColor.GOLD),
                        Component.text("itens forjados de +1 a +10.", NamedTextColor.GOLD),
                        Component.empty(),
                        Component.text("Shift+clique forja vários de uma vez.", NamedTextColor.DARK_GRAY))));

        TextColor roxo = Raridade.EPICO.cor();
        inv.setItem(F_EFEITOS, item(Material.BREWING_STAND,
                Component.text("Efeitos especiais", roxo, TextDecoration.BOLD),
                List.of(Component.text("Itens Épicos ou melhores ganham efeitos", NamedTextColor.GRAY),
                        Component.text("feitos com os efeitos do próprio jogo:", NamedTextColor.GRAY),
                        Component.empty(),
                        rotulo("✧ Equipado: ", roxo, "Velocidade, Pressa, Força,"),
                        Component.text("   Visão noturna, Salto, Regeneração...", NamedTextColor.WHITE),
                        rotulo("✧ Ao acertar: ", roxo, "Lentidão, Veneno, Chamas,"),
                        Component.text("   Congelar, Levitação, Definhamento...", NamedTextColor.WHITE),
                        rotulo("✧ Ao apanhar: ", roxo, "Absorção, Rajada de vento,"),
                        Component.text("   Regeneração, incendiar o atacante...", NamedTextColor.WHITE),
                        rotulo("✧ Ao minerar: ", roxo, "Frenesi, Sabedoria, Fundição"),
                        Component.empty(),
                        rotulo("Só em Míticos: ", Raridade.MITICO.cor(), "Raio, Execução,"),
                        Component.text("   Bastião, Poder do conduíte", NamedTextColor.WHITE))));

        inv.setItem(F_MESA, item(Material.SMITHING_TABLE,
                Component.text("Mesa de Ferraria", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Melhorar um item forjado para Netherite", NamedTextColor.GRAY),
                        Component.text("mantém a raridade e deixa os bônus", NamedTextColor.GRAY),
                        Component.text("ainda mais fortes.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Itens achados no mundo (baús, aldeões)", NamedTextColor.GRAY),
                        Component.text("são forjados ao melhorar numa mesa de", NamedTextColor.GRAY),
                        Component.text("ferraria perto da Forja do Ferreiro.", NamedTextColor.GRAY))));

        inv.setItem(F_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencherBorda(inv);
        p.openInventory(inv);
    }

    private ItemStack iconeRaridade(Raridade r, int nivel, int max) {
        Forja f = plugin.forja();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(r.estrelas(), r.cor()));
        lore.add(Component.empty());
        lore.add(linha("Sua chance: ", NamedTextColor.GRAY, Forja.pct(f.chance(r, nivel)), NamedTextColor.WHITE));
        lore.add(linha("No nível " + max + ": ", NamedTextColor.GRAY, Forja.pct(f.chance(r, max)), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(linha("Bônus de forja: ", NamedTextColor.GRAY, String.valueOf(r.qtdStatus()), NamedTextColor.WHITE));
        lore.add(linha("Efeitos especiais: ", NamedTextColor.GRAY,
                r.qtdEfeitos() == 0 ? "nenhum" : String.valueOf(r.qtdEfeitos()), NamedTextColor.WHITE));
        lore.add(linha("Força dos bônus: ", NamedTextColor.GRAY, "×" + StatsManager.fmt(f.multiplicador(r)), NamedTextColor.WHITE));
        if (r == Raridade.MITICO) lore.add(Component.text("Pode trazer efeitos exclusivos!", r.cor()));
        if (r.peloMenos(plugin.settings().forjaAnunciar)) {
            lore.add(Component.text("Anunciado para o servidor todo!", NamedTextColor.GOLD));
        }
        return enfeitar(new ItemStack(r.icone()), Component.text("✦ " + r.nome(), r.cor(), TextDecoration.BOLD),
                lore, r.peloMenos(Raridade.LENDARIO));
    }

    private static Component rotulo(String rotulo, TextColor cor, String texto) {
        return Component.text(rotulo, cor).append(Component.text(texto, NamedTextColor.WHITE));
    }

    private void desenharCosmeticos(Inventory inv, Player p) {
        inv.clear();

        // --- chapéu ---
        inv.setItem(C_DICA_CHAPEU, item(Material.OAK_SIGN,
                Component.text("Item na cabeça", NamedTextColor.GOLD, TextDecoration.BOLD),
                List.of(Component.text("Clique em qualquer item do", NamedTextColor.GRAY),
                        Component.text("SEU inventário (aqui embaixo)", NamedTextColor.WHITE),
                        Component.text("para colocá-lo na cabeça.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Vale tudo: picareta, bloco, flor...", NamedTextColor.DARK_GRAY))));

        ItemStack capacete = p.getInventory().getHelmet();
        if (capacete == null || capacete.isEmpty()) {
            inv.setItem(C_CHAPEU_ATUAL, item(Material.GLASS,
                    Component.text("Nada na cabeça", NamedTextColor.GRAY),
                    List.of(Component.text("Escolha um item lá embaixo.", NamedTextColor.DARK_GRAY))));
        } else {
            ItemStack mostra = capacete.clone();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Está na sua cabeça agora.", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique para tirar", NamedTextColor.YELLOW));
            mostra.editMeta(m -> m.lore(semItalico(lore)));
            inv.setItem(C_CHAPEU_ATUAL, mostra);
        }

        // --- guarda-roupa ---
        inv.setItem(C_GUARDA_ROUPA, item(Material.ARMOR_STAND,
                Component.text("✦ Guarda-roupa", TextColor.color(0xFF8FD7), TextDecoration.BOLD),
                List.of(Component.text("Mude a aparência da armadura", NamedTextColor.GRAY),
                        Component.text("sem tirar a armadura de verdade.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("» Clique para abrir", NamedTextColor.YELLOW))));

        // --- tag ---
        String tag = plugin.tags().getTag(p);
        Component tagAtual = tag == null
                ? Component.text(p.getName() + " (padrão)", NamedTextColor.WHITE)
                : LegacyComponentSerializer.legacyAmpersand().deserialize(tag);
        inv.setItem(C_TAG_ATUAL, item(Material.NAME_TAG,
                Component.text("Trocar tag", NamedTextColor.AQUA, TextDecoration.BOLD),
                List.of(Component.text("Atual: ", NamedTextColor.GRAY).append(tagAtual),
                        Component.empty(),
                        Component.text("Cores com &: &6dourado &cvermelho &averde", NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text("» Clique e digite no chat", NamedTextColor.YELLOW))));
        if (tag != null) {
            inv.setItem(C_TAG_REMOVER, item(Material.RED_DYE,
                    Component.text("Remover tag", NamedTextColor.RED),
                    List.of(Component.text("Volta a mostrar seu nome normal.", NamedTextColor.GRAY))));
        }

        inv.setItem(C_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencherBorda(inv);
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Menu menu)) return;
        e.setCancelled(true); // nada sai nem entra no menu
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null) return;

        boolean clicouNoMenu = e.getRawSlot() < topo.getSize();
        int slot = e.getSlot();

        if (menu.tipo == Tipo.PRINCIPAL) {
            if (!clicouNoMenu) return;
            boolean proprio = menu.alvo.equals(p.getUniqueId());
            if (slot == P_FECHAR) {
                p.closeInventory();
            } else if (slot == P_COSMETICOS && topo.getItem(slot) != null
                    && topo.getItem(slot).getType() == Material.LEATHER_HELMET) {
                clique(p);
                abrirCosmeticos(p);
            } else if (proprio && (slot == P_FORJA || slot == P_SKILLS[Skill.FERRARIA.ordinal()])) {
                clique(p);
                abrirForja(p);
            } else if (proprio && slot == P_SKILLS[Skill.ARCANO.ordinal()]) {
                clique(p);
                plugin.arcano().menus().abrirGrimorio(p);
            } else if (proprio && slot == P_SKILLS[Skill.CULINARIA.ordinal()]) {
                clique(p);
                plugin.cozinha().abrirReceitas(p, null);
            } else if (proprio && slot == P_SKILLS[Skill.DOMA.ordinal()]) {
                clique(p);
                plugin.menusDomador().abrirLista(p);
            } else if (proprio && slot == P_SKILLS[Skill.PESCA.ordinal()]) {
                clique(p);
                plugin.pesca().abrirDiario(p);
            } else if (proprio && slot == P_SKILLS[Skill.ALQUIMIA.ordinal()]) {
                clique(p);
                plugin.alquimia().abrirLivro(p);
            } else if (proprio && slot == P_TALENTOS) {
                clique(p);
                plugin.talentos().abrir(p, br.rpgatributos.progressao.Arvore.GUERREIRO);
            } else if (proprio && slot == P_ACESSORIOS) {
                clique(p);
                plugin.acessorios().abrir(p);
            } else if (proprio && slot == P_CLASSE) {
                clique(p);
                plugin.santuarios().abrirArvore(p, null);
            } else if (proprio && slot == P_PARTY) {
                clique(p);
                plugin.menuParty().abrir(p);
            } else if (proprio && slot == P_TERRITORIO) {
                clique(p);
                plugin.menusTerritorio().abrirPrincipal(p, plugin.territorios().de(p.getUniqueId()));
            }
            return;
        }

        if (menu.tipo == Tipo.FORJA) {
            if (clicouNoMenu && slot == F_VOLTAR) {
                clique(p);
                abrirPrincipal(p, p);
            }
            return;
        }

        // ----- cosméticos -----
        if (!clicouNoMenu) {
            // Clicou num item do próprio inventário: vai para a cabeça.
            if (!p.hasPermission("rpg.chapeu")) { erro(p, "Sem permissão."); return; }
            if (e.getCurrentItem() == null || e.getCurrentItem().isEmpty()) return;
            Chapeu.Resultado r = Chapeu.colocar(p, slot);
            if (!r.ok()) erro(p, r.mensagem());
            desenharCosmeticos(topo, p);
            return;
        }

        switch (slot) {
            case C_CHAPEU_ATUAL -> {
                ItemStack capacete = p.getInventory().getHelmet();
                if (capacete != null && !capacete.isEmpty()) {
                    Chapeu.Resultado r = Chapeu.tirar(p);
                    if (!r.ok()) erro(p, r.mensagem());
                    desenharCosmeticos(topo, p);
                }
            }
            case C_TAG_ATUAL -> {
                if (!p.hasPermission("rpg.tag")) { erro(p, "Sem permissão."); return; }
                esperandoTag.put(p.getUniqueId(), System.currentTimeMillis() + TEMPO_PARA_DIGITAR_MS);
                p.closeInventory();
                clique(p);
                p.sendMessage(Component.text("✎ Digite sua nova tag no chat.", NamedTextColor.AQUA));
                p.sendMessage(Component.text("  Use & para cores (ex: &6Rei &fdo Minério). Digite ", NamedTextColor.GRAY)
                        .append(Component.text("cancelar", NamedTextColor.RED))
                        .append(Component.text(" para desistir.", NamedTextColor.GRAY)));
            }
            case C_GUARDA_ROUPA -> {
                clique(p);
                plugin.guardaRoupa().abrir(p);
            }
            case C_TAG_REMOVER -> {
                if (plugin.tags().getTag(p) != null) {
                    plugin.tags().setTag(p, null);
                    clique(p);
                    desenharCosmeticos(topo, p);
                }
            }
            case C_VOLTAR -> {
                clique(p);
                abrirPrincipal(p, p);
            }
            default -> { }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }

    // =====================================================================
    //  Digitar a tag no chat
    // =====================================================================

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoFalar(AsyncChatEvent e) {
        Player p = e.getPlayer();
        Long expira = esperandoTag.remove(p.getUniqueId());
        if (expira == null || System.currentTimeMillis() > expira) return;

        e.setCancelled(true); // a mensagem não vai para o chat público
        String texto = PlainTextComponentSerializer.plainText().serialize(e.message());

        // O chat roda em outra thread; volta para a thread principal para mexer no jogo.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (texto.equalsIgnoreCase("cancelar")) {
                p.sendMessage(Component.text("Troca de tag cancelada.", NamedTextColor.GRAY));
            } else {
                String erro = plugin.tags().aplicarTagDigitada(p, texto);
                if (erro != null) {
                    erro(p, erro);
                } else {
                    p.sendMessage(Component.text("Tag alterada! ", NamedTextColor.GREEN)
                            .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.tags().getTag(p))));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2f);
                }
            }
            abrirCosmeticos(p);
        });
    }

    /** Fecha os menus abertos (usado ao desligar o plugin). */
    public void fecharTodos() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) p.closeInventory();
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        esperandoTag.remove(e.getPlayer().getUniqueId());
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    private ItemStack iconeSkill(Player alvo, Skill s, int max) {
        StatsManager st = plugin.stats();
        int nivel = st.getNivel(alvo, s);
        boolean noMax = nivel >= max;
        double prog = st.progresso(alvo, s);

        List<Component> lore = new ArrayList<>();
        lore.add(linha("Nível ", NamedTextColor.GRAY, nivel + " / " + max, NamedTextColor.WHITE));
        lore.add(barra(prog).append(Component.text(" " + Math.round(prog * 100) + "%", NamedTextColor.GRAY)));
        if (noMax) {
            lore.add(Component.text("★ Nível máximo!", NamedTextColor.GOLD));
        } else {
            double xp = st.getXp(alvo, s);
            double dentro = xp - st.xpTotalParaNivel(nivel);
            lore.add(linha("XP: ", NamedTextColor.GRAY,
                    StatsManager.fmt(Math.floor(dentro)) + " / " + StatsManager.fmt(Math.ceil(st.xpParaProximo(nivel))),
                    NamedTextColor.YELLOW));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Bônus atual:", NamedTextColor.GRAY));
        // Ferraria e Arcano já fazem algo no nível 0 (chances de forja, mana).
        boolean semBonus = nivel == 0 && s != Skill.FERRARIA && s != Skill.ARCANO;
        lore.add(Component.text(" " + (semBonus ? "nenhum ainda" : plugin.bonus().descricao(s, nivel)), NamedTextColor.GREEN));
        if (!noMax) {
            lore.add(Component.text("No nível " + (nivel + 1) + ":", NamedTextColor.GRAY));
            lore.add(Component.text(" " + plugin.bonus().descricao(s, nivel + 1), NamedTextColor.DARK_GREEN));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Como evoluir:", NamedTextColor.GRAY));
        lore.add(Component.text(" " + s.comoEvoluir(), NamedTextColor.WHITE));

        ItemStack icone = new ItemStack(s.material());
        return enfeitar(icone, Component.text(s.icone() + " " + s.nome(), s.cor(), TextDecoration.BOLD), lore, noMax);
    }

    private static Component barra(double progresso) {
        int cheios = (int) Math.round(Math.max(0, Math.min(1, progresso)) * 10);
        return Component.text("■".repeat(cheios), NamedTextColor.GREEN)
                .append(Component.text("■".repeat(10 - cheios), NamedTextColor.DARK_GRAY));
    }

    private static Component linha(String rotulo, NamedTextColor corRotulo, String valor, NamedTextColor corValor) {
        return Component.text(rotulo, corRotulo).append(Component.text(valor, corValor));
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return enfeitar(new ItemStack(m), nome, lore, false);
    }

    /** Coloca nome, descrição, esconde informações de atributo e opcionalmente faz brilhar. */
    private static ItemStack enfeitar(ItemStack item, Component nome, List<Component> lore, boolean brilhar) {
        item.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(semItalico(lore));
            meta.setAttributeModifiers(ImmutableMultimap.of()); // necessário para esconder dano/velocidade
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    private static List<Component> semItalico(List<Component> lore) {
        return lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList();
    }

    private static void preencherBorda(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, vidro);
        }
    }

    private static void clique(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }
}
