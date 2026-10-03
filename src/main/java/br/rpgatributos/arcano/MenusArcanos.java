package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.aventura.Raro;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** As telas do sistema arcano. Tudo é botão: nada pode ser tirado dos menus. */
public final class MenusArcanos implements Listener {

    private enum Tipo { INFUSOR, GRIMORIO, CRIADOR, SECRETAS }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        Inventory inventario;
        Location infusor;
        int selecionado = -1;
        Material selecionadoTipo;
        /** Núcleo de chefe escolhido (infusão lendária), ou null. */
        Raro nucleo;
        final Set<Essencia> escolhidas = EnumSet.noneOf(Essencia.class);
        Forma forma;

        Tela(Tipo tipo) { this.tipo = tipo; }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    // ----- infusor -----
    private static final int I_RESUMO = 4;
    private static final int I_AJUDA = 10;
    private static final int I_PREVIA = 16;
    private static final int I_ESSENCIAS = 28;
    private static final int I_CARGA = 34;
    private static final int I_GRIMORIO = 48;
    private static final int I_FECHAR = 50;
    private static final Map<ParteCorpo, Integer> SLOT_PARTE = Map.of(
            ParteCorpo.MENTE, 13, ParteCorpo.BRACOS, 21, ParteCorpo.CORACAO, 22,
            ParteCorpo.SANGUE, 23, ParteCorpo.PERNAS, 31);

    // ----- grimório -----
    private static final int G_RESUMO = 4;
    private static final int[] G_MAGIAS = {11, 12, 13, 14, 15, 21, 22, 23};
    private static final int G_CRIAR = 39;
    private static final int G_SECRETAS = 41;
    private static final int G_FECHAR = 49;

    // ----- criador -----
    private static final int C_PREVIA = 4;
    private static final int[] C_ESSENCIAS = {10, 11, 12, 13, 14, 15, 16, 21, 22, 23};
    private static final int[] C_FORMAS = {38, 39, 40, 41, 42};
    private static final int C_VOLTAR = 45;
    private static final int C_CRIAR = 49;
    private static final int C_LIMPAR = 53;

    // ----- secretas -----
    private static final int S_RESUMO = 4;
    private static final int[] S_RECEITAS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
    private static final int S_VOLTAR = 49;

    private static final long TEMPO_PARA_DIGITAR_MS = 60_000;

    private final RPGAtributos plugin;
    /** Quem está digitando o nome de uma magia no chat → (espaço da magia, expira em). */
    private final Map<UUID, long[]> renomeando = new ConcurrentHashMap<>();

    public MenusArcanos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Arcano arcano() { return plugin.arcano(); }
    private Settings cfg() { return plugin.settings(); }

    // =====================================================================
    //  Infusor
    // =====================================================================

    public void abrirInfusor(Player p, Location infusor) {
        Tela t = new Tela(Tipo.INFUSOR);
        t.infusor = infusor;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Infusor Corpóreo"));
        desenharInfusor(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.4f);
    }

    private void desenharInfusor(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        int nivel = arc.nivel(p);

        // Resumo
        ItemStack cabeca = new ItemStack(Material.PLAYER_HEAD);
        cabeca.editMeta(SkullMeta.class, m -> m.setOwningPlayer(p));
        List<Component> resumo = new ArrayList<>();
        resumo.add(linha("Arcano: ", "Nv " + nivel, Arcano.COR));
        resumo.add(linha("Mana: ", (int) pf.mana + " / " + (int) arc.manaMax(p, pf), NamedTextColor.AQUA));
        resumo.add(linha("Regeneração: ", StatsManager.fmt(arc.regen(p, pf)) + "/s", NamedTextColor.AQUA));
        resumo.add(linha("Carga: ", pf.carga() + " / " + arc.capacidade(p), corCarga(p, pf)));
        if (pf.instavel()) resumo.add(Component.text("Instável por " + pf.segundosInstavel() + "s", NamedTextColor.RED));
        inv.setItem(I_RESUMO, botao(cabeca, Component.text("Seu corpo", Arcano.COR, TextDecoration.BOLD), resumo, false));

        // Partes do corpo
        for (ParteCorpo parte : ParteCorpo.values()) {
            inv.setItem(SLOT_PARTE.get(parte), iconeParte(p, pf, parte, nivel, t));
        }

        // Ajuda
        inv.setItem(I_AJUDA, item(Material.BOOK, Component.text("Como infundir", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("1. Clique num item do seu inventário.", NamedTextColor.GRAY),
                Component.text("2. Clique numa parte do corpo livre.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("O item é consumido e gasta níveis de XP.", NamedTextColor.GRAY),
                Component.text("Pode falhar (o item se perde)!", NamedTextColor.RED),
                Component.empty(),
                Component.text("Parte do corpo: dá um bônus fixo.", NamedTextColor.WHITE),
                Component.text("Essências do item: liberam magias.", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Passar da carga causa rejeição.", NamedTextColor.DARK_RED))));

        // Prévia do item escolhido
        if (t.selecionado < 0) {
            inv.setItem(I_PREVIA, item(Material.OAK_SIGN, Component.text("Nenhum item escolhido", NamedTextColor.GRAY),
                    List.of(Component.text("Clique num item do seu", NamedTextColor.DARK_GRAY),
                            Component.text("inventário (aqui embaixo).", NamedTextColor.DARK_GRAY))));
        } else if (t.nucleo != null) {
            Raro r = t.nucleo;
            List<Component> lore = new ArrayList<>(descreverEssencias(Lendarias.essencias(r)));
            lore.add(Component.empty());
            lore.add(Lendarias.anuncio(r));
            lore.add(Component.empty());
            lore.add(linha("Poder: ", String.valueOf(Lendarias.PODER), NamedTextColor.WHITE));
            lore.add(linha("Carga: ", "+" + Lendarias.CARGA, NamedTextColor.WHITE));
            lore.add(linha("Custo: ", Lendarias.CUSTO_XP + " níveis de XP", NamedTextColor.GREEN));
            lore.add(linha("Chance de sucesso: ", "100%", NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("» Agora clique numa parte do corpo", NamedTextColor.YELLOW));
            inv.setItem(I_PREVIA, botao(r.criar(), Component.text("✦ Infusão lendária", NamedTextColor.GOLD, TextDecoration.BOLD),
                    lore, true));
        } else {
            Material m = t.selecionadoTipo;
            List<Component> lore = new ArrayList<>(descreverEssencias(m));
            lore.add(Component.empty());
            lore.add(linha("Poder: ", String.valueOf(Catalogo.poder(m)), NamedTextColor.WHITE));
            lore.add(linha("Carga: ", "+" + Catalogo.carga(m), NamedTextColor.WHITE));
            lore.add(linha("Custo: ", arc.custoXp(m) + " níveis de XP", NamedTextColor.GREEN));
            lore.add(linha("Chance de sucesso: ", pct(arc.chanceInfusao(p, m)), NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("» Agora clique numa parte do corpo", NamedTextColor.YELLOW));
            inv.setItem(I_PREVIA, botao(new ItemStack(m), Component.text("Item escolhido", NamedTextColor.GOLD, TextDecoration.BOLD),
                    lore, true));
        }

        // Essências no corpo
        List<Component> ess = new ArrayList<>();
        Map<Essencia, Integer> total = pf.essencias();
        if (total.isEmpty()) ess.add(Component.text("Nenhuma ainda.", NamedTextColor.DARK_GRAY));
        total.forEach((e, v) -> ess.add(e.rotulo().append(Component.text("  " + v, NamedTextColor.WHITE))));
        ess.add(Component.empty());
        ess.add(Component.text("Mais pontos = magias mais fortes.", NamedTextColor.DARK_GRAY));
        inv.setItem(I_ESSENCIAS, item(Material.ENCHANTED_BOOK, Component.text("Essências no corpo", Arcano.COR, TextDecoration.BOLD), ess));

        // Carga
        List<Component> carga = new ArrayList<>();
        carga.add(linha("Carga: ", pf.carga() + " / " + arc.capacidade(p), corCarga(p, pf)));
        carga.add(linha("Limite com sobrecarga: ", String.valueOf(arc.limiteCarga(p)), NamedTextColor.GRAY));
        carga.add(Component.empty());
        carga.add(Component.text("Acima da capacidade o corpo rejeita:", NamedTextColor.GRAY));
        carga.add(Component.text("fome, fraqueza, náusea e menos mana.", NamedTextColor.RED));
        carga.add(Component.text("A capacidade sobe com o Arcano.", NamedTextColor.DARK_GRAY));
        inv.setItem(I_CARGA, item(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Component.text("Carga corporal", NamedTextColor.GOLD, TextDecoration.BOLD), carga));

        inv.setItem(I_GRIMORIO, item(Material.KNOWLEDGE_BOOK, Component.text("Pegar Grimório", Arcano.COR, TextDecoration.BOLD), List.of(
                Component.text("O livro que cria e lança magias.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Custa 1 livro comum.", NamedTextColor.WHITE),
                Component.empty(),
                Component.text("» Clique para pegar", NamedTextColor.YELLOW))));
        inv.setItem(I_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(inv);
    }

    private ItemStack iconeParte(Player p, Perfil pf, ParteCorpo parte, int nivel, Tela t) {
        Material infundido = pf.infusoes().get(parte);
        Raro nucleo = pf.nucleos().get(parte);
        if (!arcano().liberada(p, parte)) {
            return item(Material.GRAY_STAINED_GLASS_PANE, Component.text(parte.nome() + " (bloqueado)", NamedTextColor.DARK_GRAY), List.of(
                    Component.text(parte.funcao() + ".", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Libera no Arcano nível " + arcano().nivelDaParte(parte) + ".", NamedTextColor.RED)));
        }
        if (nucleo != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Infusão lendária: ", NamedTextColor.GRAY).append(Component.text(nucleo.nome(), nucleo.cor())));
            lore.add(Component.text(parte.bonus(Lendarias.PODER), NamedTextColor.GREEN));
            lore.add(Lendarias.anuncio(nucleo));
            lore.add(Component.empty());
            lore.addAll(descreverEssencias(Lendarias.essencias(nucleo)));
            lore.add(Component.empty());
            lore.add(Component.text("Shift + clique: desfazer (o núcleo se perde!)", NamedTextColor.RED));
            return botao(nucleo.criar(), Component.text(parte.nome() + " ✦", NamedTextColor.GOLD, TextDecoration.BOLD), lore, true);
        }
        if (infundido == null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(parte.funcao() + ".", NamedTextColor.GRAY));
            lore.add(Component.empty());
            if (t.selecionado >= 0) {
                int poder = t.nucleo != null ? Lendarias.PODER : Catalogo.poder(t.selecionadoTipo);
                lore.add(Component.text("Bônus aqui: " + parte.bonus(poder), NamedTextColor.GREEN));
                lore.add(Component.text("» Clique para infundir", NamedTextColor.YELLOW));
            } else {
                lore.add(Component.text("Escolha um item do inventário primeiro.", NamedTextColor.DARK_GRAY));
            }
            return item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(parte.nome() + " (vazio)", NamedTextColor.AQUA), lore);
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Infundido: ", NamedTextColor.GRAY).append(Component.translatable(infundido.translationKey(), NamedTextColor.WHITE)));
        lore.add(Component.text(parte.bonus(Catalogo.poder(infundido)), NamedTextColor.GREEN));
        lore.add(Component.empty());
        lore.addAll(descreverEssencias(infundido));
        lore.add(Component.empty());
        lore.add(Component.text("Shift + clique: desfazer (o item se perde)", NamedTextColor.RED));
        return botao(new ItemStack(infundido), Component.text(parte.nome(), Arcano.COR, TextDecoration.BOLD), lore, true);
    }

    private List<Component> descreverEssencias(Material m) {
        return descreverEssencias(Catalogo.de(m));
    }

    private List<Component> descreverEssencias(Map<Essencia, Integer> essencias) {
        List<Component> l = new ArrayList<>();
        l.add(Component.text("Essências:", NamedTextColor.GRAY));
        essencias.forEach((e, v) -> l.add(Component.text(" ").append(e.rotulo())
                .append(Component.text(" " + v, NamedTextColor.WHITE))));
        return l;
    }

    private void cliqueInfusor(Player p, Tela t, InventoryClickEvent e, boolean noMenu) {
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        if (!noMenu) {
            ItemStack it = e.getCurrentItem();
            if (it == null || it.isEmpty()) return;
            Raro raro = Raro.de(it);
            if (raro != null && !raro.nucleo()) {
                erro(p, "Materiais raros não podem ser infundidos (só os núcleos de chefe).");
                return;
            }
            if (raro == null && (arc.ehGrimorio(it) || !Catalogo.temEssencia(it.getType()))) {
                erro(p, "Esse item não tem essência.");
                return;
            }
            t.selecionado = e.getSlot();
            t.selecionadoTipo = it.getType();
            t.nucleo = raro;
            clique(p);
            desenharInfusor(p, t);
            return;
        }
        int slot = e.getSlot();
        if (slot == I_FECHAR) { p.closeInventory(); return; }
        if (slot == I_GRIMORIO) { darGrimorio(p); return; }
        for (Map.Entry<ParteCorpo, Integer> en : SLOT_PARTE.entrySet()) {
            if (en.getValue() != slot) continue;
            ParteCorpo parte = en.getKey();
            if (!arc.liberada(p, parte)) {
                erro(p, parte.nome() + " libera no Arcano nível " + arc.nivelDaParte(parte) + ".");
            } else if (pf.ocupada(parte)) {
                if (e.isShiftClick()) desfazer(p, pf, parte, t);
            } else if (t.selecionado < 0) {
                erro(p, "Escolha um item do seu inventário primeiro.");
            } else if (t.nucleo != null) {
                infundirNucleo(p, pf, parte, t);
            } else {
                infundir(p, pf, parte, t);
            }
            return;
        }
    }

    private void infundir(Player p, Perfil pf, ParteCorpo parte, Tela t) {
        Arcano arc = arcano();
        ItemStack fonte = p.getInventory().getItem(t.selecionado);
        if (fonte == null || fonte.getType() != t.selecionadoTipo) {
            erro(p, "O item escolhido não está mais lá.");
            t.selecionado = -1;
            desenharInfusor(p, t);
            return;
        }
        Material m = fonte.getType();
        if (pf.carga() + Catalogo.carga(m) > arc.limiteCarga(p)) {
            erro(p, "Seu corpo não aguenta mais (carga " + pf.carga() + " + " + Catalogo.carga(m) + " > " + arc.limiteCarga(p) + ").");
            return;
        }
        int custo = arc.custoXp(m);
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && p.getLevel() < custo) {
            erro(p, "Precisa de " + custo + " níveis de XP.");
            return;
        }

        if (!criativo) {
            p.setLevel(p.getLevel() - custo);
            fonte.setAmount(fonte.getAmount() - 1);
            p.getInventory().setItem(t.selecionado, fonte.getAmount() > 0 ? fonte : null);
        }
        Location onde = t.infusor != null ? t.infusor.clone().add(0.5, 1, 0.5) : p.getLocation().add(0, 1, 0);
        boolean sucesso = ThreadLocalRandom.current().nextDouble() < arc.chanceInfusao(p, m);
        if (sucesso) {
            pf.infusoes().put(parte, m);
            arc.aplicarBonus(p);
            arc.salvar(p);
            for (Essencia es : Catalogo.de(m).keySet()) {
                onde.getWorld().spawnParticle(Particle.DUST, onde, 30, 0.4, 0.5, 0.4, 0, es.poeira(1.5f));
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0, es.poeira(1.2f));
            }
            onde.getWorld().spawnParticle(Particle.ENCHANT, onde, 80, 0.5, 0.5, 0.5, 1);
            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8f, 1.2f);
            p.sendMessage(Component.text("✦ Você infundiu ", Arcano.COR)
                    .append(Component.translatable(m.translationKey(), NamedTextColor.WHITE))
                    .append(Component.text(" em " + parte.nome() + ". ", Arcano.COR))
                    .append(Component.text(parte.bonus(Catalogo.poder(m)), NamedTextColor.GREEN)));
            if (ganhaXp(p)) plugin.stats().darXp(p, Skill.ARCANO, cfg().arcXpInfusao * (1 + Catalogo.raridade(m)));
            if (arc.rejeitando(p, pf)) {
                p.sendMessage(Component.text("☠ Sobrecarga! Seu corpo começa a rejeitar as infusões.", NamedTextColor.RED));
            }
            conquistasDeInfusao(p, pf);
        } else {
            onde.getWorld().spawnParticle(Particle.LARGE_SMOKE, onde, 25, 0.3, 0.3, 0.3, 0.02);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.7f);
            p.sendMessage(Component.text("✖ A infusão falhou e o item se desfez.", NamedTextColor.RED));
            if (ganhaXp(p)) plugin.stats().darXp(p, Skill.ARCANO, 3);
        }
        t.selecionado = -1;
        desenharInfusor(p, t);
    }

    private void conquistasDeInfusao(Player p, Perfil pf) {
        plugin.titulos().registrar(p, "infusoes", 1);
        if (pf.partesOcupadas() >= ParteCorpo.values().length) plugin.titulos().definirMinimo(p, "corpo_completo", 1);
    }

    /** Infusão lendária: núcleo de chefe. Nunca falha (o núcleo é precioso demais). */
    private void infundirNucleo(Player p, Perfil pf, ParteCorpo parte, Tela t) {
        Arcano arc = arcano();
        Raro r = t.nucleo;
        ItemStack fonte = p.getInventory().getItem(t.selecionado);
        if (Raro.de(fonte) != r) {
            erro(p, "O núcleo escolhido não está mais lá.");
            t.selecionado = -1;
            t.nucleo = null;
            desenharInfusor(p, t);
            return;
        }
        if (pf.temNucleo(r)) {
            erro(p, "Você já tem esse núcleo infundido. Cada poder lendário só vale uma vez.");
            return;
        }
        if (pf.carga() + Lendarias.CARGA > arc.limiteCarga(p)) {
            erro(p, "Seu corpo não aguenta mais (carga " + pf.carga() + " + " + Lendarias.CARGA + " > " + arc.limiteCarga(p) + ").");
            return;
        }
        boolean criativo = p.getGameMode() == GameMode.CREATIVE;
        if (!criativo && p.getLevel() < Lendarias.CUSTO_XP) {
            erro(p, "Precisa de " + Lendarias.CUSTO_XP + " níveis de XP.");
            return;
        }
        if (!criativo) {
            p.setLevel(p.getLevel() - Lendarias.CUSTO_XP);
            fonte.setAmount(fonte.getAmount() - 1);
            p.getInventory().setItem(t.selecionado, fonte.getAmount() > 0 ? fonte : null);
        }
        pf.nucleos().put(parte, r);
        arc.aplicarBonus(p);
        arc.salvar(p);
        Location onde = t.infusor != null ? t.infusor.clone().add(0.5, 1, 0.5) : p.getLocation().add(0, 1, 0);
        onde.getWorld().strikeLightningEffect(onde);
        onde.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, onde, 100, 0.5, 0.8, 0.5, 0.4);
        p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1, 0), 120, 0.5, 1, 0.5, 1);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        p.sendMessage(Component.text("✦ Você infundiu ", NamedTextColor.GOLD)
                .append(Component.text(r.nome(), r.cor(), TextDecoration.BOLD))
                .append(Component.text(" em " + parte.nome() + "!", NamedTextColor.GOLD)));
        p.sendMessage(Lendarias.anuncio(r));
        if (ganhaXp(p)) plugin.stats().darXp(p, Skill.ARCANO, cfg().arcXpInfusao * 5);
        plugin.titulos().registrar(p, "nucleos", 1);
        conquistasDeInfusao(p, pf);
        t.selecionado = -1;
        t.nucleo = null;
        desenharInfusor(p, t);
    }

    private void desfazer(Player p, Perfil pf, ParteCorpo parte, Tela t) {
        Material m = pf.infusoes().remove(parte);
        Raro r = pf.nucleos().remove(parte);
        arcano().aplicarBonus(p);
        arcano().salvar(p);
        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
        Component nome = r != null ? Component.text(r.nome(), r.cor())
                : Component.translatable(m.translationKey(), NamedTextColor.WHITE);
        p.sendMessage(Component.text("A infusão de ", NamedTextColor.GRAY).append(nome)
                .append(Component.text(" em " + parte.nome() + " foi desfeita.", NamedTextColor.GRAY)));
        desenharInfusor(p, t);
    }

    private void darGrimorio(Player p) {
        Arcano arc = arcano();
        if (arc.temGrimorio(p)) {
            erro(p, "Você já tem um Grimório.");
            return;
        }
        if (p.getGameMode() != GameMode.CREATIVE) {
            if (!p.getInventory().containsAtLeast(new ItemStack(Material.BOOK), 1)) {
                erro(p, "Precisa de 1 livro comum.");
                return;
            }
            p.getInventory().removeItem(new ItemStack(Material.BOOK, 1));
        }
        p.getInventory().addItem(arc.criarGrimorio()).values()
                .forEach(sobra -> p.getWorld().dropItemNaturally(p.getLocation(), sobra));
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
        p.sendMessage(Component.text("✦ Você recebeu o Grimório. Agache + clique direito com ele para abrir.", Arcano.COR));
    }

    // =====================================================================
    //  Grimório
    // =====================================================================

    public void abrirGrimorio(Player p) {
        Tela t = new Tela(Tipo.GRIMORIO);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Grimório"));
        desenharGrimorio(p, t);
        p.openInventory(t.inventario);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
    }

    private void desenharGrimorio(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        int espacos = arc.espacosGrimorio(p);

        List<Component> resumo = new ArrayList<>();
        resumo.add(linha("Mana: ", (int) pf.mana + " / " + (int) arc.manaMax(p, pf), NamedTextColor.AQUA));
        resumo.add(linha("Magias: ", contarMagias(pf) + " / " + espacos, NamedTextColor.WHITE));
        resumo.add(linha("Secretas descobertas: ", pf.descobertas().size() + " / " + Receita.values().length, NamedTextColor.GOLD));
        resumo.add(Component.empty());
        resumo.add(Component.text("Segurando o grimório:", NamedTextColor.GRAY));
        resumo.add(Component.text(" Clique direito: lançar", NamedTextColor.WHITE));
        resumo.add(Component.text(" Clique esquerdo: trocar de magia", NamedTextColor.WHITE));
        resumo.add(Component.text(" Agachado + clique direito: abrir", NamedTextColor.WHITE));
        inv.setItem(G_RESUMO, item(Material.KNOWLEDGE_BOOK, Component.text("Grimório", Arcano.COR, TextDecoration.BOLD), resumo));

        for (int i = 0; i < G_MAGIAS.length; i++) {
            if (i >= espacos) {
                inv.setItem(G_MAGIAS[i], item(Material.GRAY_STAINED_GLASS_PANE, Component.text("Espaço bloqueado", NamedTextColor.DARK_GRAY),
                        List.of(Component.text("Libera no Arcano nível " + arcano().nivelParaEspaco(i) + ".", NamedTextColor.RED))));
                continue;
            }
            Magia m = pf.magia(i);
            if (m == null) {
                inv.setItem(G_MAGIAS[i], item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Espaço livre", NamedTextColor.GRAY),
                        List.of(Component.text("Use \"Criar magia\" abaixo.", NamedTextColor.DARK_GRAY))));
                continue;
            }
            inv.setItem(G_MAGIAS[i], iconeMagia(p, pf, m, i == pf.selecionada()));
        }

        inv.setItem(G_CRIAR, item(Material.WRITABLE_BOOK, Component.text("Criar magia", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Combine até 4 essências que você", NamedTextColor.GRAY),
                Component.text("tem no corpo com uma forma.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        inv.setItem(G_SECRETAS, item(Material.NETHER_STAR, Component.text("Magias secretas", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Combinações com efeitos únicos.", NamedTextColor.GRAY),
                Component.text("Descobertas: " + pf.descobertas().size() + " / " + Receita.values().length, NamedTextColor.WHITE),
                Component.empty(),
                Component.text("» Clique para ver", NamedTextColor.YELLOW))));
        inv.setItem(G_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(inv);
    }

    private ItemStack iconeMagia(Player p, Perfil pf, Magia m, boolean selecionada) {
        Arcano arc = arcano();
        Receita r = m.receita();
        List<Component> lore = new ArrayList<>();
        lore.add(rotuloEssencias(m.essencias()));
        lore.add(linha("Forma: ", m.forma().nome(), NamedTextColor.WHITE));
        lore.add(linha("Custo: ", (int) arc.custoMana(m, pf) + " mana", NamedTextColor.AQUA));
        lore.add(linha("Recarga: ", StatsManager.fmt(arc.recargaTicks(m) / 20.0) + "s", NamedTextColor.WHITE));
        lore.add(Component.empty());
        if (r != null) {
            lore.add(Component.text("✦ Magia secreta", NamedTextColor.GOLD));
            lore.add(Component.text(r.descricao(), NamedTextColor.GRAY));
        } else {
            lore.addAll(descricaoGenerica(m.essencias(), m.forma()));
        }
        lore.add(Component.empty());
        if (selecionada) lore.add(Component.text("✔ Selecionada", NamedTextColor.GREEN));
        else lore.add(Component.text("Clique: selecionar", NamedTextColor.YELLOW));
        lore.add(Component.text("Clique direito: renomear", NamedTextColor.YELLOW));
        lore.add(Component.text("Shift + clique: apagar", NamedTextColor.RED));
        Material icone = r != null ? r.icone() : m.essencias().iterator().next().icone();
        TextColor cor = r != null ? NamedTextColor.GOLD : m.essencias().iterator().next().cor();
        return botao(new ItemStack(icone), Component.text(m.nome(), cor, TextDecoration.BOLD), lore, selecionada);
    }

    private List<Component> descricaoGenerica(Set<Essencia> es, Forma forma) {
        List<Component> l = new ArrayList<>();
        l.add(Component.text(forma.descricao() + ":", NamedTextColor.GRAY));
        for (Essencia e : es) {
            l.add(Component.text(" ").append(e.rotulo()).append(Component.text(" " + e.papel(), NamedTextColor.WHITE)));
        }
        return l;
    }

    private void cliqueGrimorio(Player p, Tela t, InventoryClickEvent e) {
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        int slot = e.getSlot();
        if (slot == G_FECHAR) { p.closeInventory(); return; }
        if (slot == G_CRIAR) { clique(p); abrirCriador(p); return; }
        if (slot == G_SECRETAS) { clique(p); abrirSecretas(p); return; }
        for (int i = 0; i < G_MAGIAS.length; i++) {
            if (G_MAGIAS[i] != slot) continue;
            if (i >= arc.espacosGrimorio(p)) {
                erro(p, "Esse espaço libera no Arcano nível " + arc.nivelParaEspaco(i) + ".");
                return;
            }
            Magia m = pf.magia(i);
            if (m == null) return;
            if (e.isShiftClick()) {
                pf.magias[i] = null;
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.6f);
            } else if (e.getClick() == ClickType.RIGHT) {
                renomeando.put(p.getUniqueId(), new long[]{i, System.currentTimeMillis() + TEMPO_PARA_DIGITAR_MS});
                p.closeInventory();
                p.sendMessage(Component.text("✎ Digite o novo nome da magia no chat (ou ", Arcano.COR)
                        .append(Component.text("cancelar", NamedTextColor.RED))
                        .append(Component.text(").", Arcano.COR)));
                return;
            } else {
                pf.selecionada = i;
                clique(p);
            }
            arc.salvar(p);
            desenharGrimorio(p, t);
            return;
        }
    }

    // =====================================================================
    //  Criador de magias
    // =====================================================================

    public void abrirCriador(Player p) {
        Tela t = new Tela(Tipo.CRIADOR);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Criar magia"));
        desenharCriador(p, t);
        p.openInventory(t.inventario);
    }

    private void desenharCriador(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        Arcano arc = arcano();
        Perfil pf = arc.perfil(p);
        Map<Essencia, Integer> temNoCorpo = pf.essencias();

        Essencia[] todas = Essencia.values();
        for (int i = 0; i < todas.length; i++) {
            Essencia e = todas[i];
            Integer pontos = temNoCorpo.get(e);
            if (pontos == null) {
                inv.setItem(C_ESSENCIAS[i], item(Material.GRAY_DYE, Component.text("??? (" + e.nome() + ")", NamedTextColor.DARK_GRAY), List.of(
                        Component.text("Infunda um item com essência", NamedTextColor.GRAY),
                        Component.text("de " + e.nome() + " para usar.", NamedTextColor.GRAY))));
                continue;
            }
            boolean escolhida = t.escolhidas.contains(e);
            inv.setItem(C_ESSENCIAS[i], botao(new ItemStack(e.icone()), e.rotulo().decorate(TextDecoration.BOLD), List.of(
                    Component.text(e.papel(), NamedTextColor.GRAY),
                    linha("No corpo: ", pontos + " pontos", NamedTextColor.WHITE),
                    Component.empty(),
                    escolhida ? Component.text("✔ Escolhida (clique para tirar)", NamedTextColor.GREEN)
                            : Component.text("» Clique para escolher", NamedTextColor.YELLOW)), escolhida));
        }

        Forma[] formas = Forma.values();
        for (int i = 0; i < formas.length; i++) {
            Forma f = formas[i];
            boolean escolhida = f == t.forma;
            inv.setItem(C_FORMAS[i], botao(new ItemStack(f.icone()), Component.text(f.nome(), NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    Component.text(f.descricao() + ".", NamedTextColor.GRAY),
                    Component.empty(),
                    escolhida ? Component.text("✔ Forma escolhida", NamedTextColor.GREEN)
                            : Component.text("» Clique para escolher", NamedTextColor.YELLOW)), escolhida));
        }

        inv.setItem(C_PREVIA, previa(p, pf, t));
        inv.setItem(C_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        inv.setItem(C_LIMPAR, item(Material.RED_DYE, Component.text("Limpar", NamedTextColor.RED), List.of()));
        inv.setItem(C_CRIAR, item(Material.ENCHANTED_BOOK, Component.text("✦ Criar magia", Arcano.COR, TextDecoration.BOLD), List.of(
                Component.text("Grava a magia no grimório.", NamedTextColor.GRAY),
                Component.text("Criar gasta a mana da magia (é o teste).", NamedTextColor.GRAY))));
        preencher(inv);
    }

    private ItemStack previa(Player p, Perfil pf, Tela t) {
        if (t.escolhidas.isEmpty() || t.forma == null) {
            return item(Material.PAPER, Component.text("Sua magia", NamedTextColor.WHITE, TextDecoration.BOLD), List.of(
                    Component.text("Escolha de 1 a 4 essências", NamedTextColor.GRAY),
                    Component.text("e uma forma.", NamedTextColor.GRAY)));
        }
        Arcano arc = arcano();
        Magia m = new Magia(t.escolhidas, t.forma, "");
        Receita r = m.receita();
        boolean conhecida = r != null && pf.descobertas().contains(r);
        List<Component> lore = new ArrayList<>();
        lore.add(rotuloEssencias(m.essencias()));
        lore.add(linha("Forma: ", t.forma.nome(), NamedTextColor.WHITE));
        lore.add(linha("Custo: ", (int) arc.custoMana(m, pf) + " mana", NamedTextColor.AQUA));
        double recarga = conhecida || r == null ? arc.recargaTicks(m) / 20.0 : arc.recargaGenerica(m) / 20.0;
        lore.add(linha("Recarga: ", StatsManager.fmt(recarga) + "s", NamedTextColor.WHITE));
        lore.add(Component.empty());
        if (conhecida) {
            lore.add(Component.text("✦ Magia secreta", NamedTextColor.GOLD));
            lore.add(Component.text(r.descricao(), NamedTextColor.GRAY));
        } else {
            lore.addAll(descricaoGenerica(m.essencias(), m.forma()));
        }
        String nome = conhecida ? r.nome() : Magia.nomeGenerico(m.essencias(), m.forma());
        return botao(new ItemStack(Material.PAPER), Component.text(nome, conhecida ? NamedTextColor.GOLD : NamedTextColor.WHITE,
                TextDecoration.BOLD), lore, conhecida);
    }

    private void cliqueCriador(Player p, Tela t, InventoryClickEvent e) {
        Perfil pf = arcano().perfil(p);
        int slot = e.getSlot();
        if (slot == C_VOLTAR) { clique(p); abrirGrimorio(p); return; }
        if (slot == C_LIMPAR) {
            t.escolhidas.clear();
            t.forma = null;
            clique(p);
            desenharCriador(p, t);
            return;
        }
        if (slot == C_CRIAR) { criarMagia(p, pf, t); return; }
        Essencia[] todas = Essencia.values();
        for (int i = 0; i < todas.length; i++) {
            if (C_ESSENCIAS[i] != slot) continue;
            Essencia es = todas[i];
            if (!pf.essencias().containsKey(es)) {
                erro(p, "Infunda um item com essência de " + es.nome() + " primeiro.");
            } else if (t.escolhidas.contains(es)) {
                t.escolhidas.remove(es);
                clique(p);
            } else if (t.escolhidas.size() >= Magia.MAX_ESSENCIAS) {
                erro(p, "No máximo " + Magia.MAX_ESSENCIAS + " essências por magia.");
            } else {
                t.escolhidas.add(es);
                clique(p);
            }
            desenharCriador(p, t);
            return;
        }
        Forma[] formas = Forma.values();
        for (int i = 0; i < formas.length; i++) {
            if (C_FORMAS[i] != slot) continue;
            t.forma = formas[i];
            clique(p);
            desenharCriador(p, t);
            return;
        }
    }

    private void criarMagia(Player p, Perfil pf, Tela t) {
        Arcano arc = arcano();
        if (t.escolhidas.isEmpty() || t.forma == null) {
            erro(p, "Escolha pelo menos uma essência e uma forma.");
            return;
        }
        int livre = pf.espacoLivre(arc.espacosGrimorio(p));
        if (livre < 0) {
            erro(p, "Grimório cheio. Apague uma magia antes.");
            return;
        }
        Magia nova = new Magia(t.escolhidas, t.forma, Magia.nomePadrao(t.escolhidas, t.forma));
        double custo = arc.custoMana(nova, pf);
        if (pf.mana < custo) {
            erro(p, "Criar essa magia gasta " + (int) custo + " de mana (você tem " + (int) pf.mana + ").");
            return;
        }
        arc.gastarMana(p, pf, custo);
        pf.magias[livre] = nova;
        pf.selecionada = livre;
        Receita r = nova.receita();
        if (r != null && pf.descobertas().add(r)) descobrir(p, r);
        else {
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
            p.sendMessage(Component.text("✦ Magia criada: ", Arcano.COR).append(Component.text(nova.nome(), NamedTextColor.WHITE)));
        }
        arc.salvar(p);
        abrirGrimorio(p);
    }

    private void descobrir(Player p, Receita r) {
        p.showTitle(Title.title(
                Component.text("✦ Magia secreta! ✦", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(r.nome(), Arcano.COR),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2800), Duration.ofMillis(700))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 80, 0.5, 0.8, 0.5, 0.4);
        p.sendMessage(Component.text("✦ Você descobriu a magia secreta ", NamedTextColor.GOLD)
                .append(Component.text(r.nome(), Arcano.COR, TextDecoration.BOLD))
                .append(Component.text(": " + r.descricao(), NamedTextColor.GRAY)));
        plugin.titulos().definirMinimo(p, "secretas", arcano().perfil(p).descobertas().size());
        if (cfg().arcAnunciarDescoberta) {
            for (Player outro : plugin.getServer().getOnlinePlayers()) {
                if (outro == p) continue;
                outro.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" descobriu a magia secreta ", NamedTextColor.GRAY))
                        .append(Component.text(r.nome(), Arcano.COR, TextDecoration.BOLD))
                        .append(Component.text("!", NamedTextColor.GRAY)));
            }
        }
        if (ganhaXp(p)) plugin.stats().darXp(p, Skill.ARCANO, cfg().arcXpDescoberta);
    }

    // =====================================================================
    //  Magias secretas
    // =====================================================================

    public void abrirSecretas(Player p) {
        Tela t = new Tela(Tipo.SECRETAS);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("✦ Magias secretas"));
        Inventory inv = t.inventario;
        Perfil pf = arcano().perfil(p);
        Receita[] todas = Receita.values();
        inv.setItem(S_RESUMO, item(Material.NETHER_STAR, Component.text("Magias secretas", NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                Component.text("Descobertas: " + pf.descobertas().size() + " / " + todas.length, NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Experimente combinações no", NamedTextColor.GRAY),
                Component.text("criador de magias para achar mais.", NamedTextColor.GRAY))));
        for (int i = 0; i < todas.length && i < S_RECEITAS.length; i++) {
            Receita r = todas[i];
            if (!pf.descobertas().contains(r)) {
                // Dica: quantas essências e uma delas (a forma continua segredo).
                Essencia pista = r.essencias().iterator().next();
                inv.setItem(S_RECEITAS[i], item(Material.GRAY_DYE, Component.text("???", NamedTextColor.DARK_GRAY), List.of(
                        Component.text("Ainda não descoberta.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Dica: " + r.essencias().size() + " essências, uma delas é ", NamedTextColor.DARK_GRAY)
                                .append(pista.rotulo()))));
                continue;
            }
            inv.setItem(S_RECEITAS[i], botao(new ItemStack(r.icone()), Component.text(r.nome(), NamedTextColor.GOLD, TextDecoration.BOLD), List.of(
                    rotuloEssencias(r.essencias()),
                    linha("Forma: ", r.forma().nome(), NamedTextColor.WHITE),
                    Component.empty(),
                    Component.text(r.descricao(), NamedTextColor.GRAY)), false));
        }
        inv.setItem(S_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(inv);
        p.openInventory(inv);
    }

    // =====================================================================
    //  Eventos
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        boolean noMenu = e.getRawSlot() < topo.getSize();
        switch (t.tipo) {
            case INFUSOR -> cliqueInfusor(p, t, e, noMenu);
            case GRIMORIO -> { if (noMenu) cliqueGrimorio(p, t, e); }
            case CRIADOR -> { if (noMenu) cliqueCriador(p, t, e); }
            case SECRETAS -> {
                if (noMenu && e.getSlot() == S_VOLTAR) { clique(p); abrirGrimorio(p); }
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void aoFalar(AsyncChatEvent e) {
        Player p = e.getPlayer();
        long[] espera = renomeando.remove(p.getUniqueId());
        if (espera == null || System.currentTimeMillis() > espera[1]) return;
        e.setCancelled(true);
        String texto = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        int slot = (int) espera[0];
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            Perfil pf = arcano().perfil(p);
            Magia m = pf.magia(slot);
            if (texto.equalsIgnoreCase("cancelar") || m == null) {
                p.sendMessage(Component.text("Renomear cancelado.", NamedTextColor.GRAY));
            } else if (Magia.limparNome(texto).isEmpty()) {
                erro(p, "Nome inválido.");
            } else {
                pf.magias[slot] = m.comNome(texto);
                arcano().salvar(p);
                p.sendMessage(Component.text("✦ Magia renomeada para ", Arcano.COR)
                        .append(Component.text(pf.magias[slot].nome(), NamedTextColor.WHITE)));
            }
            abrirGrimorio(p);
        });
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        renomeando.remove(e.getPlayer().getUniqueId());
    }

    public void fecharTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Tela) p.closeInventory();
        }
    }

    // =====================================================================
    //  Ajudantes
    // =====================================================================

    private static int contarMagias(Perfil pf) {
        int n = 0;
        for (int i = 0; i < Perfil.MAX_MAGIAS; i++) if (pf.magia(i) != null) n++;
        return n;
    }

    private static boolean ganhaXp(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
    }

    private TextColor corCarga(Player p, Perfil pf) {
        return arcano().rejeitando(p, pf) ? NamedTextColor.RED : NamedTextColor.GREEN;
    }

    private static Component rotuloEssencias(Set<Essencia> es) {
        Component c = Component.empty();
        boolean primeira = true;
        for (Essencia e : es) {
            if (!primeira) c = c.append(Component.text(" + ", NamedTextColor.DARK_GRAY));
            c = c.append(e.rotulo());
            primeira = false;
        }
        return c;
    }

    private static String pct(double v) {
        return StatsManager.fmt(Math.round(v * 1000) / 10.0) + "%";
    }

    private static Component linha(String rotulo, String valor, TextColor cor) {
        return Component.text(rotulo, NamedTextColor.GRAY).append(Component.text(valor, cor));
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        return botao(new ItemStack(m), nome, lore, false);
    }

    private static ItemStack botao(ItemStack item, Component nome, List<Component> lore, boolean brilhar) {
        item.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
            if (brilhar) meta.setEnchantmentGlintOverride(true);
        });
        return item;
    }

    private static void preencher(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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
