package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Menus do domador: Altar (vincular e invocar), companheiro (ordens e componentes), /pets. */
public final class MenusDomador implements Listener {

    private enum Tipo { ALTAR, COMPANHEIRO, LISTA, COMPONENTES }

    private static final class Tela implements InventoryHolder {
        final Tipo tipo;
        Inventory inventario;
        Location altar;
        UUID companheiro;
        final Map<Integer, UUID> entidades = new HashMap<>();
        final Map<Integer, Invocavel> invocaveis = new HashMap<>();

        Tela(Tipo tipo) { this.tipo = tipo; }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    /** Distância máxima até um Altar do Domador para vincular ou mexer nos componentes. */
    private static final double RAIO_ALTAR = 10;

    // altar
    private static final int A_INFO = 4, A_CRIATURAS = 9, A_INVOCAR = 18, A_FECHAR = 49;
    private static final int[] A_SLOTS_CRIATURAS = {10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] A_SLOTS_INVOCAR = {19, 20, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 33, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44};
    // companheiro
    private static final int C_INFO = 4, C_MONTAR = 25, C_ROTULO_COMP = 28, C_ALFORJE = 45, C_CATALOGO = 47, C_VOLTAR = 49, C_LIBERTAR = 53;
    private static final int[] C_MODOS = {19, 20, 21, 22, 23};
    private static final int[] C_COMPONENTES = {29, 30, 31, 32, 33, 34};

    private final RPGAtributos plugin;

    public MenusDomador(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    private Companheiros comp() { return plugin.companheiros(); }
    private Settings cfg() { return plugin.settings(); }
    private int nivel(Player p) { return plugin.stats().getNivel(p, Skill.DOMA); }

    // =====================================================================
    //  Altar
    // =====================================================================

    public void abrirAltar(Player p, Location altar) {
        Tela t = new Tela(Tipo.ALTAR);
        t.altar = altar;
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♞ Altar do Domador"));
        desenharAltar(p, t);
        p.openInventory(t.inventario);
    }

    private void desenharAltar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        t.entidades.clear();
        t.invocaveis.clear();
        int nivel = nivel(p);
        int max = cfg().nivelMaximo;
        inv.setItem(A_INFO, item(Material.HAY_BLOCK, Component.text("♞ Doma Nv " + nivel, Companheiros.COR, TextDecoration.BOLD), List.of(
                Component.text("Companheiros: " + comp().de(p.getUniqueId()).size() + " / " + comp().limite(p), NamedTextColor.WHITE),
                Component.text("Componentes em cada um: " + comp().espacos(p), NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Vincular: traga a criatura presa no", NamedTextColor.GRAY),
                Component.text("seu laço (ou um pet domado por você)", NamedTextColor.GRAY),
                Component.text("até perto do altar e clique nela abaixo.", NamedTextColor.GRAY),
                Component.text("Invocar: entregue os componentes", NamedTextColor.GRAY),
                Component.text("da criatura e ela nasce aqui.", NamedTextColor.GRAY))));

        inv.setItem(A_CRIATURAS, item(Material.LEAD, Component.text("Criaturas perto do altar →", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("Até " + (int) RAIO_ALTAR + " blocos do altar.", NamedTextColor.GRAY))));
        Location centro = t.altar.clone().add(0.5, 1, 0.5);
        int i = 0;
        for (LivingEntity e : centro.getWorld().getNearbyLivingEntities(centro, RAIO_ALTAR)) {
            if (i >= A_SLOTS_CRIATURAS.length) break;
            boolean meu = Companheiros.eh(e) && comp().eDono(p, e);
            boolean vinculavel = !meu && Companheiros.vinculavel(e) && Companheiros.doJogador(p, e);
            if (!meu && !vinculavel) continue;
            List<Component> lore = new ArrayList<>();
            if (meu) {
                lore.add(Component.text("Seu companheiro.", NamedTextColor.GREEN));
                lore.add(Component.text("» Clique para personalizar", NamedTextColor.YELLOW));
            } else {
                lore.add(Component.text(Companheiros.nomeTipo(e.getType()), NamedTextColor.GRAY));
                lore.add(Component.text("» Clique para vincular", NamedTextColor.YELLOW));
            }
            inv.setItem(A_SLOTS_CRIATURAS[i], brilhar(item(Invocavel.icone(e.getType()),
                    Component.text(Companheiros.nome(e), meu ? Companheiros.COR : NamedTextColor.WHITE, TextDecoration.BOLD), lore), meu));
            t.entidades.put(A_SLOTS_CRIATURAS[i], e.getUniqueId());
            i++;
        }
        if (i == 0) {
            inv.setItem(A_SLOTS_CRIATURAS[0], item(Material.OAK_SIGN, Component.text("Nenhuma criatura sua aqui", NamedTextColor.GRAY), List.of(
                    Component.text("Prenda um animal no laço e traga", NamedTextColor.DARK_GRAY),
                    Component.text("até o altar (ou traga um pet seu).", NamedTextColor.DARK_GRAY))));
        }

        inv.setItem(A_INVOCAR, item(Material.EGG, Component.text("Invocar com componentes →", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("A criatura nasce já como seu companheiro.", NamedTextColor.GRAY))));
        PlayerInventory pi = p.getInventory();
        Invocavel[] todos = Invocavel.values();
        for (int k = 0; k < todos.length && k < A_SLOTS_INVOCAR.length; k++) {
            Invocavel invoc = todos[k];
            boolean liberado = nivel >= invoc.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text((liberado ? "✔" : "✖") + " Doma nível " + invoc.nivelNecessario(max),
                    liberado ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            boolean temTudo = true;
            for (Map.Entry<Material, Integer> en : invoc.receita().entrySet()) {
                boolean tem = pi.containsAtLeast(new ItemStack(en.getKey()), en.getValue());
                temTudo &= tem;
                lore.add(Component.text((tem ? " ✔ " : " ✖ ") + en.getValue() + "x ", tem ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .append(Component.translatable(en.getKey().translationKey(), tem ? NamedTextColor.GREEN : NamedTextColor.RED)));
            }
            lore.add(Component.empty());
            if (!liberado) lore.add(Component.text("Suba a Doma para liberar.", NamedTextColor.DARK_GRAY));
            else if (temTudo) lore.add(Component.text("» Clique para invocar", NamedTextColor.YELLOW));
            else lore.add(Component.text("Faltam componentes.", NamedTextColor.DARK_GRAY));
            inv.setItem(A_SLOTS_INVOCAR[k], brilhar(item(liberado ? Invocavel.icone(invoc.tipo()) : Material.GRAY_DYE,
                    Component.text(invoc.nome(), liberado ? Companheiros.COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore), liberado && temTudo));
            t.invocaveis.put(A_SLOTS_INVOCAR[k], invoc);
        }
        inv.setItem(A_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(inv);
    }

    private void clicarAltar(Player p, Tela t, int slot) {
        if (slot == A_FECHAR) { p.closeInventory(); return; }
        if (!plugin.altaresDomador().eh(t.altar.getBlock())) {
            erro(p, "O Altar do Domador não existe mais.");
            p.closeInventory();
            return;
        }
        UUID id = t.entidades.get(slot);
        if (id != null) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity e) || !e.isValid()) { erro(p, "Essa criatura sumiu."); return; }
            if (Companheiros.eh(e)) {
                clique(p);
                abrirCompanheiro(p, e);
                return;
            }
            if (!Companheiros.doJogador(p, e)) { erro(p, "Prenda a criatura no seu laço primeiro."); return; }
            if (resultado(p, comp().vincular(p, e))) abrirCompanheiro(p, e);
            return;
        }
        Invocavel invoc = t.invocaveis.get(slot);
        if (invoc == null) return;
        if (nivel(p) < invoc.nivelNecessario(cfg().nivelMaximo)) {
            erro(p, "Precisa de Doma nível " + invoc.nivelNecessario(cfg().nivelMaximo) + ".");
            return;
        }
        PlayerInventory pi = p.getInventory();
        for (Map.Entry<Material, Integer> en : invoc.receita().entrySet()) {
            if (!pi.containsAtLeast(new ItemStack(en.getKey()), en.getValue())) { erro(p, "Faltam componentes."); return; }
        }
        if (comp().de(p.getUniqueId()).size() >= comp().limite(p)) {
            erro(p, "Você já tem " + comp().limite(p) + " companheiro(s). Suba a Doma para ter mais (ou liberte um).");
            return;
        }
        for (Map.Entry<Material, Integer> en : invoc.receita().entrySet()) pi.removeItem(new ItemStack(en.getKey(), en.getValue()));
        if (resultado(p, comp().invocar(p, invoc, t.altar.clone().add(0.5, 1, 0.5)))) desenharAltar(p, t);
    }

    // =====================================================================
    //  Companheiro
    // =====================================================================

    public void abrirCompanheiro(Player p, LivingEntity e) {
        Tela t = new Tela(Tipo.COMPANHEIRO);
        t.companheiro = e.getUniqueId();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♞ " + Companheiros.nome(e)));
        desenharCompanheiro(p, t, e);
        p.openInventory(t.inventario);
    }

    /** Há um Altar do Domador perto do companheiro (para mexer nos componentes)? */
    private boolean altarPerto(LivingEntity e) {
        return !plugin.altaresDomador().perto(e.getLocation(), RAIO_ALTAR).isEmpty();
    }

    private void desenharCompanheiro(Player p, Tela t, LivingEntity e) {
        Inventory inv = t.inventario;
        inv.clear();
        int nivel = nivel(p);
        int max = cfg().nivelMaximo;
        Set<Componente> comps = comp().componentes(e);
        Modo modo = comp().modo(e);

        List<Component> info = new ArrayList<>();
        info.add(Component.text(Companheiros.nomeTipo(e.getType()), NamedTextColor.GRAY));
        if (plugin.settings().evoAtivada) info.add(plugin.evolucao().linha(e));
        if (br.rpgatributos.detalhes.ItensDetalhes.temFerradura(e)) info.add(Component.text("Ferradura: +15% de velocidade e +10% de pulo", NamedTextColor.GRAY));
        Component raro = br.rpgatributos.detalhes.Natureza.linha(e);
        if (raro != null) info.add(raro);
        AttributeInstance vidaMax = e.getAttribute(Attribute.MAX_HEALTH);
        info.add(Component.text(String.format("❤ %.1f / %.0f", e.getHealth(), vidaMax == null ? 20 : vidaMax.getValue()), NamedTextColor.RED));
        info.add(Component.text("Ordem: " + modo.nome(), NamedTextColor.YELLOW));
        Location l = e.getLocation();
        info.add(Component.text("Em " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ(), NamedTextColor.GRAY));
        if (!comps.isEmpty()) {
            info.add(Component.empty());
            for (Componente c : comps) info.add(Component.text(" ✦ " + c.nome(), Companheiros.COR));
        }
        info.add(Component.empty());
        info.add(Component.text("Para mudar o nome, use uma etiqueta de nome.", NamedTextColor.DARK_GRAY));
        inv.setItem(C_INFO, item(Invocavel.icone(e.getType()), Component.text(Companheiros.nome(e), Companheiros.COR, TextDecoration.BOLD), info));

        Modo[] modos = Modo.values();
        for (int i = 0; i < modos.length && i < C_MODOS.length; i++) {
            Modo m = modos[i];
            boolean liberado = nivel >= m.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(m.descricao(), NamedTextColor.GRAY));
            if (m == Modo.GUARDAR) lore.add(Component.text("Raio: " + Math.round(6 + nivel / (double) max * 10) + " blocos.", NamedTextColor.DARK_GRAY));
            if (m == Modo.PATRULHAR) lore.add(Component.text("Raio: " + Math.round(8 + nivel / (double) max * 16) + " blocos.", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            if (!liberado) lore.add(Component.text("✖ Doma nível " + m.nivelNecessario(max), NamedTextColor.RED));
            else if (m == modo) lore.add(Component.text("● Ordem atual", NamedTextColor.GREEN));
            else lore.add(Component.text("» Clique para dar a ordem", NamedTextColor.YELLOW));
            inv.setItem(C_MODOS[i], brilhar(item(liberado ? m.icone() : Material.GRAY_DYE,
                    Component.text(m.nome(), liberado ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore), m == modo));
        }
        if (comps.contains(Componente.SELA)) {
            inv.setItem(C_MONTAR, item(Material.SADDLE, Component.text("Montar", Companheiros.COR, TextDecoration.BOLD), List.of(
                    Component.text("Ou clique nele com a mão vazia.", NamedTextColor.GRAY),
                    Component.text(comps.contains(Componente.ASAS) ? "Espaço sobe, agachar desce." : "WASD anda, espaço pula.", NamedTextColor.GRAY))));
        }

        int espacos = comp().espacos(p);
        boolean altar = altarPerto(e);
        inv.setItem(C_ROTULO_COMP, item(Material.SMITHING_TABLE, Component.text("Componentes (" + comps.size() + "/" + espacos + ") →",
                NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text(altar ? "Clique num componente do seu inventário" : "Leve até um Altar do Domador", NamedTextColor.GRAY),
                Component.text(altar ? "para colocar; clique num daqui para tirar." : "para colocar ou tirar componentes.", NamedTextColor.GRAY))));
        List<Componente> lista = new ArrayList<>(comps);
        for (int i = 0; i < C_COMPONENTES.length; i++) {
            if (i < lista.size()) {
                Componente c = lista.get(i);
                inv.setItem(C_COMPONENTES[i], brilhar(item(c.item(), Component.text(c.nome(), Companheiros.COR, TextDecoration.BOLD), List.of(
                        Component.text(c.descricao(), NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text(altar ? "» Clique para tirar" : "Tire no Altar do Domador.", altar ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY))), true));
            } else if (i < espacos) {
                inv.setItem(C_COMPONENTES[i], item(Material.LIME_STAINED_GLASS_PANE, Component.text("Espaço livre", NamedTextColor.GREEN), List.of(
                        Component.text("Clique num componente do seu inventário.", NamedTextColor.GRAY))));
            } else {
                int precisa = (int) Math.ceil((double) i * max / Math.max(1, cfg().domaComponentes(max) - 1));
                inv.setItem(C_COMPONENTES[i], item(Material.GRAY_STAINED_GLASS_PANE, Component.text("Bloqueado", NamedTextColor.DARK_GRAY), List.of(
                        Component.text("Libera com mais Doma (≈ nível " + Math.min(max, precisa) + ").", NamedTextColor.GRAY))));
            }
        }
        if (comps.contains(Componente.ALFORJE)) {
            inv.setItem(C_ALFORJE, item(Material.CHEST, Component.text("Alforje", Companheiros.COR, TextDecoration.BOLD), List.of(
                    Component.text("27 espaços carregados pelo companheiro.", NamedTextColor.GRAY),
                    Component.text("» Clique para abrir", NamedTextColor.YELLOW))));
        }
        inv.setItem(C_CATALOGO, item(Material.KNOWLEDGE_BOOK, Component.text("Todos os componentes", NamedTextColor.AQUA, TextDecoration.BOLD), List.of(
                Component.text("O que cada um faz e o nível de Doma.", NamedTextColor.GRAY))));
        inv.setItem(C_VOLTAR, item(Material.ARROW, Component.text("« Seus companheiros", NamedTextColor.YELLOW), List.of()));
        inv.setItem(C_LIBERTAR, item(Material.SHEARS, Component.text("Libertar", NamedTextColor.DARK_RED, TextDecoration.BOLD), List.of(
                Component.text("Desfaz o vínculo: vira uma criatura comum", NamedTextColor.GRAY),
                Component.text("e os componentes caem no chão.", NamedTextColor.GRAY),
                Component.text("Shift + clique para confirmar.", NamedTextColor.RED))));
        preencher(inv);
    }

    private void clicarCompanheiro(Player p, Tela t, InventoryClickEvent ev) {
        if (!(Bukkit.getEntity(t.companheiro) instanceof LivingEntity e) || !e.isValid() || !Companheiros.eh(e)) {
            erro(p, "Esse companheiro não está mais aqui.");
            p.closeInventory();
            return;
        }
        if (!comp().eDono(p, e)) return;
        int slot = ev.getSlot();
        // Clique no próprio inventário: colocar um componente.
        if (ev.getRawSlot() >= t.inventario.getSize()) {
            ItemStack it = ev.getCurrentItem();
            Componente c = it == null ? null : Componente.porItem(it.getType());
            if (c == null) return;
            // Item gasto viraria um novo ao tirar o componente: precisa estar inteiro.
            if (it.getItemMeta() instanceof Damageable d && d.hasDamage()) {
                erro(p, "Conserte o item antes: o componente precisa estar inteiro.");
                return;
            }
            if (resultado(p, colocar(p, e, c))) {
                ItemStack resto = it.clone();
                resto.setAmount(it.getAmount() - 1);
                ev.getClickedInventory().setItem(slot, resto.getAmount() > 0 ? resto : null);
                desenharCompanheiro(p, t, e);
            }
            return;
        }
        Modo[] modos = Modo.values();
        for (int i = 0; i < modos.length && i < C_MODOS.length; i++) {
            if (C_MODOS[i] == slot) {
                if (resultado(p, ordenar(p, e, modos[i]))) desenharCompanheiro(p, t, e);
                return;
            }
        }
        for (int i = 0; i < C_COMPONENTES.length; i++) {
            if (C_COMPONENTES[i] != slot) continue;
            List<Componente> lista = new ArrayList<>(comp().componentes(e));
            if (i < lista.size() && resultado(p, tirar(p, e, lista.get(i)))) desenharCompanheiro(p, t, e);
            return;
        }
        switch (slot) {
            case C_MONTAR -> {
                if (!comp().tem(e, Componente.SELA)) return;
                if (!p.getWorld().equals(e.getWorld()) || p.getLocation().distanceSquared(e.getLocation()) > 6 * 6) {
                    erro(p, "Chegue mais perto para montar.");
                    return;
                }
                p.closeInventory();
                if (e.getPassengers().isEmpty()) e.addPassenger(p);
            }
            case C_ALFORJE -> {
                if (!comp().tem(e, Componente.ALFORJE)) return;
                clique(p);
                p.openInventory(comp().alforje(e));
            }
            case C_CATALOGO -> { clique(p); abrirCatalogo(p, e); }
            case C_VOLTAR -> { clique(p); abrirLista(p); }
            case C_LIBERTAR -> {
                if (!ev.isShiftClick()) { erro(p, "Use shift + clique para libertar."); return; }
                String nome = Companheiros.nome(e);
                comp().libertar(e);
                p.closeInventory();
                p.sendMessage(Component.text("♞ " + nome + " foi libertado.", NamedTextColor.GRAY));
            }
            default -> { }
        }
    }

    /** @return motivo de não ter dado certo, ou null. */
    private String ordenar(Player p, LivingEntity e, Modo m) {
        int precisa = m.nivelNecessario(cfg().nivelMaximo);
        if (nivel(p) < precisa) return "Precisa de Doma nível " + precisa + " para essa ordem.";
        if (m.usaCentro() && (!p.getWorld().equals(e.getWorld()) || p.getLocation().distanceSquared(e.getLocation()) > 48 * 48)) {
            return "Chegue mais perto (até 48 blocos) para dar essa ordem: ela usa o lugar onde você está.";
        }
        comp().comandar(e, m, p.getLocation());
        String nome = Companheiros.nome(e);
        p.sendMessage(Component.text("♞ " + switch (m) {
            case SEGUIR -> nome + " vai seguir você.";
            case FICAR -> nome + " vai ficar aqui.";
            case GUARDAR -> nome + " vai guardar este lugar.";
            case PATRULHAR -> nome + " vai patrulhar em volta daqui.";
            case SOLTO -> nome + " está solto.";
        }, Companheiros.COR));
        return null;
    }

    private String colocar(Player p, LivingEntity e, Componente c) {
        if (!altarPerto(e)) return "Leve o companheiro até perto de um Altar do Domador.";
        int nivel = nivel(p);
        int precisa = c.nivelNecessario(cfg().nivelMaximo);
        if (nivel < precisa) return "Precisa de Doma nível " + precisa + " para " + c.nome() + ".";
        Set<Componente> comps = comp().componentes(e);
        if (comps.contains(c)) return "Ele já tem " + c.nome() + ".";
        if (c.precisa() != null && !comps.contains(c.precisa())) return c.nome() + " precisa de " + c.precisa().nome() + " antes.";
        if (comps.size() >= comp().espacos(p)) return "Sem espaço: suba a Doma para colocar mais componentes.";
        comps.add(c);
        comp().gravarComponentes(e, comps);
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.DOMA, cfg().domaXpComponente);
        if (c == Componente.ASAS) plugin.titulos().registrar(p, "voadores", 1);
        e.getWorld().playSound(e.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);
        e.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, e.getLocation().add(0, e.getHeight(), 0), 15, 0.4, 0.3, 0.4);
        p.sendMessage(Component.text("♞ " + Companheiros.nome(e) + " ganhou " + c.nome() + "!", Companheiros.COR));
        return null;
    }

    private String tirar(Player p, LivingEntity e, Componente c) {
        if (!altarPerto(e)) return "Leve o companheiro até perto de um Altar do Domador para tirar componentes.";
        Set<Componente> comps = comp().componentes(e);
        for (Componente outro : comps) {
            if (outro.precisa() == c) return "Tire " + outro.nome() + " antes (ele precisa de " + c.nome() + ").";
        }
        if (c == Componente.ALFORJE) {
            for (ItemStack i : comp().alforje(e).getContents()) {
                if (i != null && !i.isEmpty()) return "Esvazie o alforje antes de tirar.";
            }
        }
        comps.remove(c);
        comp().gravarComponentes(e, comps);
        if (c == Componente.SELA) e.eject();
        p.getInventory().addItem(new ItemStack(c.item())).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
        p.sendMessage(Component.text("♞ " + c.nome() + " tirado de " + Companheiros.nome(e) + ".", NamedTextColor.GRAY));
        return null;
    }

    // =====================================================================
    //  Catálogo de componentes
    // =====================================================================

    private void abrirCatalogo(Player p, LivingEntity e) {
        Tela t = new Tela(Tipo.COMPONENTES);
        t.companheiro = e.getUniqueId();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♞ Componentes"));
        int nivel = nivel(p), max = cfg().nivelMaximo;
        Componente[] todos = Componente.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < todos.length && i < slots.length; i++) {
            Componente c = todos[i];
            boolean ok = nivel >= c.nivelNecessario(max);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(c.descricao(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Item: ", NamedTextColor.GRAY).append(Component.translatable(c.item().translationKey(), NamedTextColor.WHITE)));
            if (c.precisa() != null) lore.add(Component.text("Precisa de: " + c.precisa().nome(), NamedTextColor.GOLD));
            lore.add(Component.text((ok ? "✔" : "✖") + " Doma nível " + c.nivelNecessario(max), ok ? NamedTextColor.GREEN : NamedTextColor.RED));
            t.inventario.setItem(slots[i], item(c.item(), Component.text(c.nome(), ok ? Companheiros.COR : NamedTextColor.DARK_GRAY, TextDecoration.BOLD), lore));
        }
        t.inventario.setItem(31, item(Material.BOOK, Component.text("Exemplo: montaria voadora", NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("Vaca de cogumelo + Sela + Asas:", NamedTextColor.GRAY),
                Component.text("monte nela e voe! (Doma 50)", NamedTextColor.GRAY))));
        t.inventario.setItem(C_VOLTAR, item(Material.ARROW, Component.text("« Voltar", NamedTextColor.YELLOW), List.of()));
        preencher(t.inventario);
        p.openInventory(t.inventario);
    }

    // =====================================================================
    //  Lista (/pets)
    // =====================================================================

    public void abrirLista(Player p) {
        Tela t = new Tela(Tipo.LISTA);
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♞ Seus companheiros"));
        int nivel = nivel(p);
        List<Companheiros.Registro> meus = comp().de(p.getUniqueId());
        t.inventario.setItem(4, item(Material.LEAD, Component.text("♞ Doma Nv " + nivel, Companheiros.COR, TextDecoration.BOLD), List.of(
                Component.text("Companheiros: " + meus.size() + " / " + comp().limite(p), NamedTextColor.WHITE),
                Component.text("Componentes em cada um: " + comp().espacos(p), NamedTextColor.WHITE),
                Component.empty(),
                Component.text("Consiga companheiros no Altar do Domador:", NamedTextColor.GRAY),
                Component.text("fardo de feno + 1 laço + 1 sela (tecla Q).", NamedTextColor.GRAY))));
        int slot = 9;
        for (Companheiros.Registro r : meus) {
            if (slot > 44) break;
            LivingEntity e = Bukkit.getEntity(r.id()) instanceof LivingEntity le && le.isValid() ? le : null;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(Companheiros.nomeTipo(r.tipo()), NamedTextColor.GRAY));
            if (e != null) {
                AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
                lore.add(Component.text(String.format("❤ %.1f / %.0f", e.getHealth(), max == null ? 20 : max.getValue()), NamedTextColor.RED));
                if (plugin.settings().evoAtivada) lore.add(plugin.evolucao().linha(e));
                lore.add(Component.text("Ordem: " + comp().modo(e).nome(), NamedTextColor.YELLOW));
                if (e.getWorld().equals(p.getWorld())) {
                    lore.add(Component.text("A " + Math.round(e.getLocation().distance(p.getLocation())) + " blocos de você", NamedTextColor.GRAY));
                } else {
                    lore.add(Component.text("Em outro mundo (" + e.getWorld().getName() + ")", NamedTextColor.GRAY));
                }
                lore.add(Component.empty());
                lore.add(Component.text("Clique: abrir", NamedTextColor.YELLOW));
                lore.add(Component.text("Shift + clique: vem até você (andando)", NamedTextColor.YELLOW));
            } else {
                lore.add(Component.text("Longe: o lugar dele não está carregado.", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("Última posição: " + r.x() + " " + r.y() + " " + r.z() + " (" + r.mundo() + ")", NamedTextColor.GRAY));
            }
            t.inventario.setItem(slot, brilhar(item(r.tipo() == null ? Material.LEAD : Invocavel.icone(r.tipo()),
                    Component.text(r.nome() == null ? "?" : r.nome(), e != null ? Companheiros.COR : NamedTextColor.GRAY, TextDecoration.BOLD), lore), e != null));
            t.entidades.put(slot, r.id());
            slot++;
        }
        if (meus.isEmpty()) {
            t.inventario.setItem(22, item(Material.OAK_SIGN, Component.text("Nenhum companheiro ainda", NamedTextColor.GRAY), List.of(
                    Component.text("Crie um Altar do Domador (veja o /guia).", NamedTextColor.DARK_GRAY))));
        }
        t.inventario.setItem(49, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED), List.of()));
        preencher(t.inventario);
        p.openInventory(t.inventario);
    }

    private void clicarLista(Player p, Tela t, InventoryClickEvent ev) {
        int slot = ev.getSlot();
        if (slot == 49) { p.closeInventory(); return; }
        UUID id = t.entidades.get(slot);
        if (id == null) return;
        if (!(Bukkit.getEntity(id) instanceof LivingEntity e) || !e.isValid()) {
            erro(p, "Esse companheiro está longe (o lugar dele não está carregado). Vá até lá.");
            return;
        }
        if (ev.isShiftClick()) {
            if (resultado(p, ordenar(p, e, Modo.SEGUIR))) p.closeInventory();
            return;
        }
        clique(p);
        abrirCompanheiro(p, e);
    }

    // =====================================================================
    //  Cliques
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoClicar(InventoryClickEvent e) {
        Inventory topo = e.getView().getTopInventory();
        if (!(topo.getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() == null) return;
        boolean noMenu = e.getRawSlot() < topo.getSize();
        switch (t.tipo) {
            case ALTAR -> { if (noMenu) clicarAltar(p, t, e.getSlot()); }
            case COMPANHEIRO -> clicarCompanheiro(p, t, e);
            case LISTA -> { if (noMenu) clicarLista(p, t, e); }
            case COMPONENTES -> {
                if (noMenu && e.getSlot() == C_VOLTAR && Bukkit.getEntity(t.companheiro) instanceof LivingEntity le && le.isValid()) {
                    clique(p);
                    abrirCompanheiro(p, le);
                }
            }
        }
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    private static boolean resultado(Player p, String erro) {
        if (erro == null) {
            clique(p);
            return true;
        }
        erro(p, erro);
        return false;
    }

    private static void erro(Player p, String msg) {
        p.sendMessage(Component.text(msg, NamedTextColor.RED));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static void clique(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    // =====================================================================
    //  Itens
    // =====================================================================

    private static ItemStack item(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    private static ItemStack brilhar(ItemStack i, boolean sim) {
        if (sim) i.editMeta(m -> m.setEnchantmentGlintOverride(true));
        return i;
    }

    private static void preencher(Inventory inv) {
        ItemStack vidro = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }
}
