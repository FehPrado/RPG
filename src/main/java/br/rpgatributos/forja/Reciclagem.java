package br.rpgatributos.forja;

import br.rpgatributos.Estacao;
import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.Mitrilo;
import br.rpgatributos.fazenda.Cozinha;
import br.rpgatributos.lenda.Lendas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bancada de Reciclagem: um rebolo onde se jogou 1 funil e 1 tesoura. Desmonta equipamentos (itens que não
 * empilham e têm receita) e devolve parte do material.
 *
 * <p>Reciclagem 2.0 (2.60):
 * <ul>
 *   <li>Até 9 itens de uma vez (clique no item do inventário para pôr; clique na bancada para tirar).</li>
 *   <li>O material principal vem primeiro: a parte quebrada vira chance (1,5 ferro = 1 garantido e 50% de
 *       outro), o graveto nunca "rouba" o material, e item pouco gasto devolve pelo menos 1 do principal.</li>
 *   <li>As armas dos minérios devolvem o mineral do plugin (não o item comum por baixo dele); peças
 *       "✦ Mitrilo" devolvem o Lingote de Mitrilo com chance.</li>
 *   <li>Os encantamentos devolvem XP (como o rebolo do jogo), além da chance do livro.</li>
 *   <li>Item valioso (forjado Épico ou melhor, com nome, com gema ou refino, Mitrilo) pede confirmação.</li>
 * </ul>
 */
public final class Reciclagem extends Estacao {

    private static final TextColor COR = TextColor.color(0x9ACD32);
    private static final int R_INFO = 4;
    /** Os 9 espaços onde vão os itens (3 × 3 à esquerda). */
    private static final int[] R_ENTRADA = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int R_RESULTADO = 24;
    private static final int R_RECICLAR = 42;
    private static final int R_FECHAR = 49;
    private static final Material GRAVETO = Material.STICK;

    private static final class Tela implements InventoryHolder {
        Inventory inventario;
        Location bancada;
        /** Os itens postos para reciclar (fora do inventário do jogador enquanto a tela está aberta). */
        final ItemStack[] itens = new ItemStack[R_ENTRADA.length];
        long confirmarAte;

        @Override
        public Inventory getInventory() { return inventario; }

        int livre() {
            for (int i = 0; i < itens.length; i++) if (itens[i] == null) return i;
            return -1;
        }

        List<ItemStack> postos() {
            List<ItemStack> l = new ArrayList<>();
            for (ItemStack i : itens) if (i != null) l.add(i);
            return l;
        }
    }

    public Reciclagem(RPGAtributos plugin) {
        super(plugin, "reciclagens", "reciclagem_display");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Ritual: 1 funil + 1 tesoura em cima de um rebolo
    // =====================================================================

    @Override
    protected boolean blocoValido(Material m) {
        return m == Material.GRINDSTONE;
    }

    @Override
    protected boolean itemDoRitual(Material m) {
        return m == Material.HOPPER || m == Material.SHEARS;
    }

    @Override
    protected Block blocoDoRitual(Item item) {
        Block abaixo = item.getLocation().clone().subtract(0, 0.25, 0).getBlock();
        return abaixo.getType() == Material.GRINDSTONE ? abaixo : null;
    }

    @Override
    protected boolean consumirRitual(Block bloco, List<Item> itens) {
        if (contar(itens, Material.HOPPER) < 1 || contar(itens, Material.SHEARS) < 1) return false;
        tirar(itens, Material.HOPPER, 1);
        tirar(itens, Material.SHEARS, 1);
        return true;
    }

    @Override
    protected ItemStack itemFlutuante() {
        return new ItemStack(Material.SHEARS);
    }

    @Override
    protected float escalaItem() {
        return 0.5f;
    }

    @Override
    protected double alturaItem() {
        return 1.3;
    }

    @Override
    protected Component nome() {
        return Component.text("♻ Bancada de Reciclagem ♻", COR)
                .append(Component.newline())
                .append(Component.text("clique com o botão direito", NamedTextColor.GRAY));
    }

    @Override
    protected void aoCriar(Block b, Player quem) {
        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 1, 0.5);
        w.spawnParticle(Particle.CRIT, c, 40, 0.4, 0.4, 0.4, 0.2);
        w.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1f, 0.8f);
        if (quem == null) return;
        quem.showTitle(Title.title(Component.text("♻ Bancada de Reciclagem ♻", COR, TextDecoration.BOLD),
                Component.text("Clique com o botão direito para desmontar itens", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2500), Duration.ofMillis(600))));
    }

    @Override
    protected void particulas(Location centro, int ciclo) {
        if (ciclo % 4 == 0) centro.getWorld().spawnParticle(Particle.SCRAPE, centro.clone().subtract(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0);
    }

    @Override
    protected void devolver(Location centro) {
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.HOPPER));
        centro.getWorld().dropItemNaturally(centro, new ItemStack(Material.SHEARS));
    }

    @Override
    protected void abrir(Player p, Block b, boolean agachado) {
        Tela t = new Tela();
        t.bancada = b.getLocation();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("♻ Reciclagem"));
        desenhar(p, t);
        p.openInventory(t.inventario);
    }

    // =====================================================================
    //  O que volta ao reciclar
    // =====================================================================

    /** Uma parte do que volta: o item (1 unidade), quanto volta (com fração) e se é o material principal. */
    private static final class Parte {
        final ItemStack modelo;
        double exato;
        final boolean principal;

        Parte(ItemStack modelo, double exato, boolean principal) {
            this.modelo = modelo;
            this.exato = exato;
            this.principal = principal;
        }

        int garantido() { return (int) Math.floor(exato + 1e-9); }
        double chance() { return exato - garantido(); }
    }

    /** Fração do material que volta: 50% (+25% no nível máximo de Ferraria). */
    private double fatorBase(Player p) {
        return 0.5 + 0.25 * plugin.stats().getNivel(p, Skill.FERRARIA) / plugin.settings().nivelMaximo;
    }

    /** Quanto do item ainda está inteiro (1 = novo; itens sem durabilidade contam como novos). */
    private static double inteiro(ItemStack item) {
        if (item.getItemMeta() instanceof Damageable d && item.getType().getMaxDurability() > 0) {
            return 1 - (double) d.getDamage() / item.getType().getMaxDurability();
        }
        return 1;
    }

    /** Receita comum do jogo para o item (para saber do que ele é feito). */
    private static Recipe receita(Material tipo) {
        for (Recipe r : Bukkit.getRecipesFor(new ItemStack(tipo))) {
            if ((r instanceof ShapedRecipe || r instanceof ShapelessRecipe)
                    && r instanceof Keyed k && k.getKey().getNamespace().equals("minecraft")) {
                return r;
            }
        }
        return null;
    }

    /** A receita do item: a do arsenal (adaga, foice, armas dos minérios...); senão, a do jogo. */
    private static Recipe receitaDe(ItemStack item) {
        br.rpgatributos.combo.Arsenal.Arma a = br.rpgatributos.combo.Arsenal.de(item);
        if (a != null) {
            Recipe r = br.rpgatributos.combo.Arsenal.receita(a, base(item.getType()));
            if (r != null) return r;
        }
        Recipe m = ConjuntoMineral.receita(item); // 2.64: conjunto dos minerais devolve o mineral
        if (m != null) return m;
        return receita(base(item.getType()));
    }

    /** O item do ingrediente: o do plugin quando a receita pede um exato (Mitrilo, Âmbar...), senão o comum. */
    private static ItemStack modelo(RecipeChoice c) {
        if (c instanceof RecipeChoice.ExactChoice ec && !ec.getChoices().isEmpty()) return ec.getChoices().getFirst().asOne();
        if (c instanceof RecipeChoice.MaterialChoice mc && !mc.getChoices().isEmpty()) return new ItemStack(mc.getChoices().getFirst());
        return null;
    }

    public boolean reciclavel(ItemStack item) {
        if (item == null || item.isEmpty() || item.getMaxStackSize() > 1) return false;
        if (Raro.de(item) != null || item.getPersistentDataContainer().has(Cozinha.CHAVE_PRATO)) return false;
        if (plugin.arcano().ehGrimorio(item) || Lendas.marcado(item)) return false;
        return receitaDe(item) != null;
    }

    /** Netherite é melhoria de diamante: recicla como diamante + sucata de netherite. */
    private static Material base(Material tipo) {
        if (tipo.name().startsWith("NETHERITE_") && Categoria.de(tipo) != null) {
            Material diamante = Material.matchMaterial(tipo.name().replace("NETHERITE_", "DIAMOND_"));
            if (diamante != null) return diamante;
        }
        return tipo;
    }

    private static void somar(List<Parte> l, ItemStack modelo, double qtd, boolean principal) {
        for (Parte x : l) {
            if (x.modelo.isSimilar(modelo)) {
                x.exato += qtd;
                return;
            }
        }
        l.add(new Parte(modelo, qtd, principal));
    }

    /** O que volta de um item, com as frações (o sorteio é na hora de reciclar). */
    private List<Parte> partes(Player p, ItemStack item) {
        return partes(fatorBase(p), item);
    }

    /** O mesmo, com a fração da Ferraria já calculada (0,5 a 0,75). */
    static List<Parte> partes(double fatorBase, ItemStack item) {
        List<Parte> l = new ArrayList<>();
        Recipe r = receitaDe(item);
        if (r == null) return l;
        List<ItemStack> usados = new ArrayList<>();
        if (r instanceof ShapedRecipe s) {
            Map<Character, RecipeChoice> escolhas = s.getChoiceMap();
            for (String linha : s.getShape()) {
                for (char ch : linha.toCharArray()) {
                    ItemStack m = modelo(escolhas.get(ch));
                    if (m != null) usados.add(m);
                }
            }
        } else if (r instanceof ShapelessRecipe s) {
            for (RecipeChoice c : s.getChoiceList()) {
                ItemStack m = modelo(c);
                if (m != null) usados.add(m);
            }
        }
        double inteiro = inteiro(item);
        double f = fatorBase * inteiro;
        int rendimento = Math.max(1, r.getResult().getAmount());
        for (ItemStack u : usados) somar(l, u, f / rendimento, valor(u) > 0);
        // Item pouco gasto (mais da metade inteira) devolve pelo menos 1 do material mais valioso.
        if (inteiro >= 0.5) {
            Parte melhor = null;
            for (Parte x : l) {
                if (!x.principal) continue;
                if (melhor == null || valor(x.modelo) > valor(melhor.modelo)
                        || valor(x.modelo) == valor(melhor.modelo) && x.exato > melhor.exato) melhor = x;
            }
            if (melhor != null && melhor.garantido() < 1) melhor.exato = 1;
        }
        if (base(item.getType()) != item.getType()) somar(l, new ItemStack(Material.NETHERITE_SCRAP), 4 * f, true);
        if (Mitrilo.aprimorado(item)) somar(l, Mitrilo.lingote(1), fatorBase * Math.max(0.5, inteiro), true);
        l.removeIf(x -> x.exato < 0.01);
        return l;
    }

    /**
     * O quanto o material vale, para a garantia: 3 = item do plugin (mineral, Mitrilo...), 2 = metal ou
     * gema, 1 = o resto, 0 = o que só faz o cabo ou a armação (graveto, tábua, linha, pena, couro, pedregulho).
     */
    private static int valor(ItemStack i) {
        if (i.hasItemMeta() && !i.getItemMeta().getPersistentDataContainer().isEmpty()) return 3;
        Material m = i.getType();
        if (m == GRAVETO || org.bukkit.Tag.PLANKS.isTagged(m) || m == Material.STRING || m == Material.FEATHER || m == Material.LEATHER
                || m == Material.COBBLESTONE || m == Material.FLINT || m == Material.PAPER) return 0;
        String n = m.name();
        if (n.endsWith("_INGOT") || n.equals("DIAMOND") || n.equals("EMERALD") || n.equals("NETHERITE_SCRAP")) return 2;
        return 1;
    }

    /** XP dos encantamentos, como o rebolo do jogo. */
    private static int xpDosEncantamentos(ItemStack item) {
        int custo = 0;
        for (Map.Entry<Enchantment, Integer> en : item.getEnchantments().entrySet()) {
            if (en.getKey().isCursed()) continue;
            custo += en.getKey().getMinModifiedCost(en.getValue());
        }
        if (custo <= 0) return 0;
        int metade = (custo + 1) / 2;
        return metade + rnd().nextInt(metade + 1);
    }

    private double chanceLivro(Player p) {
        return 0.25 + 0.25 * plugin.stats().getNivel(p, Skill.FERRARIA) / plugin.settings().nivelMaximo;
    }

    private double chanceFragmento(ItemStack item) {
        DadosForja d = plugin.forja().ler(item);
        if (d == null) return 0;
        Tier tier = Tier.de(item.getType());
        if (tier != Tier.DIAMANTE && tier != Tier.NETHERITE) return 0;
        if (d.raridade() == Raridade.MITICO) return 0.5;
        if (d.raridade() == Raridade.LENDARIO) return 0.15;
        return 0;
    }

    /** Pede confirmação: forjado Épico ou melhor, com nome, com gema ou refino, ou aprimorado com Mitrilo. */
    private boolean valioso(ItemStack item) {
        if (Mitrilo.aprimorado(item) || Nomeada.de(item) != null) return true;
        DadosForja d = plugin.forja().ler(item);
        return d != null && (d.raridade().peloMenos(Raridade.EPICO) || !d.gemas().isEmpty() || d.refino() > 0);
    }

    // =====================================================================
    //  Tela
    // =====================================================================

    private static String numero(double v) {
        return Math.abs(v - Math.round(v)) < 0.05 ? String.valueOf(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    private static Component nomeDe(ItemStack i) {
        return i.hasItemMeta() && i.getItemMeta().hasItemName() ? i.getItemMeta().itemName() : Component.translatable(i.getType().translationKey());
    }

    private void desenhar(Player p, Tela t) {
        Inventory inv = t.inventario;
        inv.clear();
        inv.setItem(R_INFO, item(Material.GRINDSTONE, Component.text("♻ Reciclagem", COR, TextDecoration.BOLD), List.of(
                Component.text("Clique nos itens do seu inventário para", NamedTextColor.GRAY),
                Component.text("pô-los aqui (até 9). Clique aqui para tirar.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Volta " + Math.round(fatorBase(p) * 100) + "% do material (sobe com a Ferraria).", NamedTextColor.GREEN),
                Component.text("O material principal vem primeiro; o que", NamedTextColor.GRAY),
                Component.text("sobra em fração vira chance. Item pouco", NamedTextColor.GRAY),
                Component.text("gasto devolve ao menos 1 do principal.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Encantamentos: XP de volta e chance de livro.", NamedTextColor.LIGHT_PURPLE),
                Component.text("Lendário/Mítico de diamante ou netherite:", NamedTextColor.GOLD),
                Component.text("chance de Fragmento de Forja.", NamedTextColor.GOLD)), false));
        for (int i = 0; i < R_ENTRADA.length; i++) {
            if (t.itens[i] != null) inv.setItem(R_ENTRADA[i], t.itens[i].clone());
            else inv.setItem(R_ENTRADA[i], item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Vazio", NamedTextColor.DARK_GRAY),
                    List.of(Component.text("Clique num item do seu inventário.", NamedTextColor.DARK_GRAY)), false));
        }
        List<ItemStack> postos = t.postos();
        boolean confirmar = System.currentTimeMillis() < t.confirmarAte;
        if (postos.isEmpty()) {
            inv.setItem(R_RESULTADO, item(Material.OAK_SIGN, Component.text("Nenhum item", NamedTextColor.GRAY), List.of(
                    Component.text("Clique nos itens do seu inventário.", NamedTextColor.DARK_GRAY)), false));
        } else {
            List<Parte> total = new ArrayList<>();
            int xp = 0, livros = 0;
            double frag = 0;
            boolean algumValioso = false;
            for (ItemStack it : postos) {
                for (Parte x : partes(p, it)) somar(total, x.modelo, x.exato, x.principal);
                for (Map.Entry<Enchantment, Integer> en : it.getEnchantments().entrySet()) if (!en.getKey().isCursed()) xp += en.getKey().getMinModifiedCost(en.getValue());
                if (!it.getEnchantments().isEmpty()) livros++;
                frag += chanceFragmento(it);
                algumValioso |= valioso(it);
            }
            List<Component> lore = new ArrayList<>();
            for (Parte x : total) {
                Component linha = Component.text(" " + x.garantido() + "× ", NamedTextColor.WHITE).append(nomeDe(x.modelo));
                if (x.chance() > 0.01) linha = linha.append(Component.text("  (+" + Math.round(x.chance() * 100) + "% de mais 1)", NamedTextColor.GRAY));
                if (x.garantido() == 0 && x.chance() > 0.01) linha = Component.text(" " + Math.round(x.chance() * 100) + "% de 1× ", NamedTextColor.GRAY).append(nomeDe(x.modelo));
                lore.add(linha);
            }
            if (lore.isEmpty()) lore.add(Component.text(" nada (itens muito gastos)", NamedTextColor.RED));
            if (xp > 0) lore.add(Component.text(" + uns " + numero(xp * 0.75) + " pontos de XP dos encantamentos", NamedTextColor.GREEN));
            if (livros > 0) lore.add(Component.text(" + " + Forja.pct(chanceLivro(p)) + " de um livro encantado (por item encantado)", NamedTextColor.LIGHT_PURPLE));
            if (frag > 0) lore.add(Component.text(" + chance de Fragmento de Forja", NamedTextColor.GOLD));
            if (algumValioso) {
                lore.add(Component.empty());
                lore.add(Component.text("⚠ Há item valioso aqui (forjado, com nome ou gema).", NamedTextColor.RED));
            }
            inv.setItem(R_RESULTADO, item(Material.CHEST, Component.text("Você recebe (" + postos.size() + (postos.size() == 1 ? " item)" : " itens)"),
                    NamedTextColor.GOLD, TextDecoration.BOLD), lore, false));
        }
        if (confirmar) {
            inv.setItem(R_RECICLAR, item(Material.RED_CONCRETE, Component.text("⚠ Confirmar: reciclar mesmo?", NamedTextColor.RED, TextDecoration.BOLD), List.of(
                    Component.text("Há item valioso. Clique de novo para reciclar.", NamedTextColor.GRAY),
                    Component.text("Os itens serão destruídos.", NamedTextColor.DARK_GRAY)), true));
        } else {
            inv.setItem(R_RECICLAR, item(Material.GRINDSTONE, Component.text("♻ Reciclar " + (postos.size() > 1 ? "tudo" : ""), COR, TextDecoration.BOLD),
                    List.of(Component.text(postos.isEmpty() ? "Ponha um item primeiro." : "Os itens serão destruídos.", NamedTextColor.GRAY)), false));
        }
        inv.setItem(R_FECHAR, item(Material.BARRIER, Component.text("Fechar", NamedTextColor.RED),
                List.of(Component.text("Os itens postos voltam para você.", NamedTextColor.DARK_GRAY)), false));
        ItemStack vidro = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        vidro.editMeta(m -> m.setHideTooltip(true));
        for (int i = 0; i < inv.getSize(); i++) if (inv.getItem(i) == null) inv.setItem(i, vidro);
    }

    private boolean pertoDaBancada(Player p, Tela t) {
        if (t.bancada != null && !eh(t.bancada.getBlock())) {
            erro(p, "A Bancada de Reciclagem não existe mais.");
            return false;
        }
        if (t.bancada != null && (!p.getWorld().equals(t.bancada.getWorld())
                || p.getLocation().distanceSquared(t.bancada.clone().add(0.5, 0.5, 0.5)) > 8 * 8)) {
            erro(p, "Você está longe da Bancada de Reciclagem.");
            return false;
        }
        return true;
    }

    private void reciclar(Player p, Tela t) {
        List<ItemStack> postos = t.postos();
        if (postos.isEmpty()) {
            erro(p, "Ponha um item reciclável primeiro (clique nele no seu inventário).");
            return;
        }
        if (!pertoDaBancada(p, t)) {
            p.closeInventory();
            return;
        }
        boolean algumValioso = postos.stream().anyMatch(this::valioso);
        if (algumValioso && System.currentTimeMillis() > t.confirmarAte) {
            t.confirmarAte = System.currentTimeMillis() + 10_000;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            desenhar(p, t);
            return;
        }
        t.confirmarAte = 0;
        List<ItemStack> volta = new ArrayList<>();
        int xp = 0, fragmentos = 0, ferraria = 0;
        for (ItemStack alvo : postos) {
            Desmonte d = desmontar(alvo, fatorBase(p), chanceLivro(p), volta);
            xp += d.xp();
            fragmentos += d.fragmentos();
            ferraria += d.ferraria();
        }
        if (fragmentos > 0) volta.add(Raro.FRAGMENTO_DE_FORJA.criar(fragmentos));
        java.util.Arrays.fill(t.itens, null);
        for (ItemStack i : volta) dar(p, i);
        Location c = t.bancada != null ? t.bancada.clone().add(0.5, 1, 0.5) : p.getLocation();
        if (xp > 0) { int total = xp; c.getWorld().spawn(c, ExperienceOrb.class, o -> o.setExperience(total)); }
        if (p.getGameMode() != GameMode.CREATIVE) plugin.stats().darXp(p, Skill.FERRARIA, ferraria);
        plugin.titulos().registrar(p, "reciclados", postos.size());
        c.getWorld().spawnParticle(Particle.CRIT, c, 25, 0.3, 0.3, 0.3, 0.2);
        p.playSound(p.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1f);
        p.sendMessage(Component.text("♻ Reciclado" + (postos.size() > 1 ? "s " + postos.size() + " itens" : "") + "! Você recebeu "
                + volta.size() + " pilha(s) de material" + (fragmentos > 0 ? ", " + fragmentos + " Fragmento(s) de Forja" : "")
                + (xp > 0 ? " e " + xp + " de XP" : "") + ".", COR));
        desenhar(p, t);
    }

    private record Desmonte(int xp, int fragmentos, int ferraria) { }

    /** Desmonta um item: põe em {@code volta} o material sorteado, o livro e as gemas (os fragmentos vêm na contagem). */
    private Desmonte desmontar(ItemStack alvo, double fatorBase, double chanceLivro, List<ItemStack> volta) {
        for (Parte x : partes(fatorBase, alvo)) {
            int qtd = x.garantido() + (rnd().nextDouble() < x.chance() ? 1 : 0);
            if (qtd > 0) juntar(volta, x.modelo, qtd);
        }
        int xp = xpDosEncantamentos(alvo);
        Map<Enchantment, Integer> encantos = alvo.getEnchantments();
        if (!encantos.isEmpty() && rnd().nextDouble() < chanceLivro) {
            List<Map.Entry<Enchantment, Integer>> lista = new ArrayList<>(encantos.entrySet());
            Map.Entry<Enchantment, Integer> sorteado = lista.get(rnd().nextInt(lista.size()));
            ItemStack livro = new ItemStack(Material.ENCHANTED_BOOK);
            livro.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(sorteado.getKey(), sorteado.getValue(), true));
            volta.add(livro);
        }
        DadosForja d = plugin.forja().ler(alvo);
        int fr = rnd().nextDouble() < chanceFragmento(alvo) ? 1 : 0;
        if (d != null && d.refino() >= 5) fr++; // o refino investido volta um pouco
        // As gemas engastadas voltam inteiras.
        if (d != null) for (DadosForja.Engaste en : d.gemas()) volta.add(en.gema().criar(en.grau(), 1));
        return new Desmonte(xp, fr, d != null && d.raridade().peloMenos(Raridade.EPICO) ? 13 : 3);
    }

    // =====================================================================
    //  2.62: reciclagem automática (funil em cima, baú embaixo)
    // =====================================================================

    private int ciclosAuto;

    /** A cada 2 s: cada bancada com um funil em cima desmonta um item dele e põe o material no baú de baixo. */
    @Override
    protected void aoTick() {
        if (++ciclosAuto % 8 != 0) return;
        for (Location l : carregadas()) {
            Block b = l.getBlock();
            if (!(b.getRelative(0, 1, 0).getState(false) instanceof org.bukkit.block.Hopper funil)) continue;
            org.bukkit.inventory.Inventory entrada = funil.getInventory();
            int slot = -1;
            for (int i = 0; i < entrada.getSize(); i++) {
                ItemStack it = entrada.getItem(i);
                // Item valioso nunca é reciclado sozinho: fica no funil esperando alguém decidir.
                if (it != null && reciclavel(it) && !valioso(it)) { slot = i; break; }
            }
            if (slot < 0) continue;
            ItemStack alvo = entrada.getItem(slot);
            entrada.setItem(slot, null);
            List<ItemStack> volta = new ArrayList<>();
            Desmonte d = desmontar(alvo, 0.5, 0.25, volta);
            if (d.fragmentos() > 0) volta.add(Raro.FRAGMENTO_DE_FORJA.criar(d.fragmentos()));
            Block baixo = b.getRelative(0, -1, 0);
            org.bukkit.inventory.Inventory saida = baixo.getState(false) instanceof org.bukkit.block.Container c ? c.getInventory() : null;
            Location centro = l.clone().add(0.5, 1, 0.5);
            for (ItemStack i : volta) {
                var sobra = saida == null ? java.util.Map.of(0, i) : saida.addItem(i);
                for (ItemStack s : sobra.values()) b.getWorld().dropItemNaturally(l.clone().add(0.5, -0.2, 0.5), s);
            }
            if (d.xp() > 0) b.getWorld().spawn(centro, ExperienceOrb.class, o -> o.setExperience(d.xp()));
            b.getWorld().spawnParticle(Particle.CRIT, centro, 8, 0.2, 0.2, 0.2, 0.1);
            b.getWorld().playSound(centro, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 1.2f);
        }
    }

    /** Junta numa pilha igual que ainda tem espaço; senão, pilha nova. */
    private static void juntar(List<ItemStack> l, ItemStack modelo, int qtd) {
        for (ItemStack x : l) {
            if (qtd <= 0) return;
            if (!x.isSimilar(modelo) || x.getAmount() >= x.getMaxStackSize()) continue;
            int cabe = Math.min(qtd, x.getMaxStackSize() - x.getAmount());
            x.setAmount(x.getAmount() + cabe);
            qtd -= cabe;
        }
        while (qtd > 0) {
            ItemStack n = modelo.asOne();
            int pilha = Math.min(qtd, n.getMaxStackSize());
            n.setAmount(pilha);
            l.add(n);
            qtd -= pilha;
        }
    }

    private static void dar(Player p, ItemStack i) {
        p.getInventory().addItem(i).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
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
        if (e.getRawSlot() >= topo.getSize()) {
            // Do inventário para a bancada.
            ItemStack it = e.getCurrentItem();
            if (it == null || it.isEmpty()) return;
            if (!reciclavel(it)) {
                erro(p, "Esse item não pode ser reciclado.");
                return;
            }
            int vaga = t.livre();
            if (vaga < 0) {
                erro(p, "A bancada está cheia (9 itens). Recicle ou tire algum.");
                return;
            }
            t.itens[vaga] = it.clone();
            e.getClickedInventory().setItem(e.getSlot(), null);
            t.confirmarAte = 0;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            desenhar(p, t);
            return;
        }
        for (int i = 0; i < R_ENTRADA.length; i++) {
            if (e.getSlot() != R_ENTRADA[i] || t.itens[i] == null) continue;
            // Da bancada de volta para o inventário.
            dar(p, t.itens[i]);
            t.itens[i] = null;
            t.confirmarAte = 0;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.9f);
            desenhar(p, t);
            return;
        }
        if (e.getSlot() == R_FECHAR) p.closeInventory();
        else if (e.getSlot() == R_RECICLAR) reciclar(p, t);
    }

    @EventHandler
    public void aoArrastarMenu(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    /** Fechou a tela: o que estava posto volta para o jogador. */
    @EventHandler
    public void aoFechar(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Tela t) || !(e.getPlayer() instanceof Player p)) return;
        for (int i = 0; i < t.itens.length; i++) {
            if (t.itens[i] == null) continue;
            dar(p, t.itens[i]);
            t.itens[i] = null;
        }
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
