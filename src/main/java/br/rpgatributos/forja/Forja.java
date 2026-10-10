package br.rpgatributos.forja;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.forja.DadosForja.EfeitoRolado;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sorteia raridade, bônus e efeitos e transforma isso num item de verdade:
 * nome colorido, atributos nativos e descrição bonita.
 */
public final class Forja {

    private static final TextColor COR_EFEITO = TextColor.color(0xE2B6FF);

    /** Atributos padrão do item que aparecem somados ao bônus, nesta ordem. */
    private static final List<Attribute> ORDEM_BASE = List.of(
            Attribute.ATTACK_DAMAGE, Attribute.ATTACK_SPEED, Attribute.ARMOR,
            Attribute.ARMOR_TOUGHNESS, Attribute.KNOCKBACK_RESISTANCE);

    /** Valor que o jogador já tem sem item (o Minecraft mostra somado). */
    private static final Map<Attribute, Double> BASE_JOGADOR = Map.of(
            Attribute.ATTACK_DAMAGE, 1.0,
            Attribute.ATTACK_SPEED, 4.0);

    private final RPGAtributos plugin;
    private final NamespacedKey kRaridade, kFerreiro, kStatus, kEfeitos, kRefino, kGemas;

    public Forja(RPGAtributos plugin) {
        this.plugin = plugin;
        kRaridade = new NamespacedKey(plugin, "forja_raridade");
        kFerreiro = new NamespacedKey(plugin, "forja_ferreiro");
        kStatus = new NamespacedKey(plugin, "forja_status");
        kEfeitos = new NamespacedKey(plugin, "forja_efeitos");
        kRefino = new NamespacedKey(plugin, "forja_refino");
        kGemas = new NamespacedKey(plugin, "forja_gemas");
    }

    private Settings cfg() { return plugin.settings(); }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Leitura
    // =====================================================================

    public boolean forjado(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(kRaridade);
    }

    /** Dados da forja do item, ou null se ele não foi forjado. */
    public DadosForja ler(ItemStack item) {
        if (!forjado(item)) return null;
        var pdc = item.getPersistentDataContainer();
        Raridade r = Raridade.porId(pdc.get(kRaridade, PersistentDataType.STRING));
        if (r == null) return null;
        String ferreiro = pdc.get(kFerreiro, PersistentDataType.STRING);
        Integer refino = pdc.get(kRefino, PersistentDataType.INTEGER);
        return new DadosForja(r, ferreiro == null ? "?" : ferreiro,
                DadosForja.lerStatus(pdc.get(kStatus, PersistentDataType.STRING)),
                DadosForja.lerEfeitos(pdc.get(kEfeitos, PersistentDataType.STRING)),
                refino == null ? 0 : refino,
                DadosForja.lerGemas(pdc.get(kGemas, PersistentDataType.STRING)));
    }

    public double multiplicador(Raridade r) {
        return cfg().forjaMultiplicador.getOrDefault(r, r.multiplicadorPadrao());
    }

    /** Multiplicador final dos bônus: raridade × refino. */
    private double multiplicador(DadosForja d) {
        return multiplicador(d.raridade()) * d.fatorRefino();
    }

    /** Valor final de um bônus no item (0 se ele não tiver). */
    public double valor(DadosForja d, Material tipo, Stat s) {
        Double qualidade = d.status().get(s);
        double v = qualidade == null ? 0 : s.valor(qualidade, multiplicador(d), Tier.de(tipo));
        return v + valorGemas(d, Categoria.de(tipo), s);
    }

    /** Soma dos bônus das gemas engastadas para esse atributo. */
    public static double valorGemas(DadosForja d, Categoria cat, Stat s) {
        if (cat == null || d.gemas().isEmpty()) return 0;
        double v = 0;
        for (DadosForja.Engaste en : d.gemas()) {
            Gema.Bonus b = en.gema().bonus(cat);
            if (b != null && b.stat() == s) v += b.valor(en.grau());
        }
        return v;
    }

    public double valor(ItemStack item, Stat s) {
        DadosForja d = ler(item);
        return d == null ? 0 : valor(d, item.getType(), s);
    }

    // =====================================================================
    //  Chances
    // =====================================================================

    private double peso(Raridade r, int nivel) {
        double t = Math.min(1, Math.max(0, nivel / (double) cfg().nivelMaximo));
        double a = cfg().forjaPesoInicial.getOrDefault(r, r.pesoInicialPadrao());
        double b = cfg().forjaPesoFinal.getOrDefault(r, r.pesoFinalPadrao());
        return Math.max(0, a + (b - a) * t);
    }

    private double pesoTotal(int nivel) {
        double total = 0;
        for (Raridade r : Raridade.values()) total += peso(r, nivel);
        return total;
    }

    /** Chance (0 a 1) de sair exatamente essa raridade. */
    public double chance(Raridade r, int nivel) {
        double total = pesoTotal(nivel);
        if (total <= 0) return r == Raridade.COMUM ? 1 : 0;
        return peso(r, nivel) / total;
    }

    /** Chance de sair essa raridade ou uma melhor. */
    public double chancePeloMenos(Raridade r, int nivel) {
        double soma = 0;
        for (Raridade x : Raridade.values()) if (x.peloMenos(r)) soma += chance(x, nivel);
        return soma;
    }

    public Raridade sortearRaridade(int nivel) {
        double x = rnd().nextDouble() * pesoTotal(nivel);
        for (Raridade r : Raridade.values()) {
            x -= peso(r, nivel);
            if (x < 0) return r;
        }
        return Raridade.COMUM;
    }

    /** Ferraria alta puxa a qualidade dos bônus para cima (nível máx.: média 67% em vez de 50%). */
    private double sortearQualidade(int nivel) {
        double k = 1 + Math.min(1, nivel / (double) cfg().nivelMaximo);
        return 1 - Math.pow(1 - rnd().nextDouble(), k);
    }

    /** Quanto os bônus saem mais fortes que no nível 0 (0,33 = +33%). */
    public double bonusQualidade(int nivel) {
        double k = 1 + Math.min(1, nivel / (double) cfg().nivelMaximo);
        return (k / (k + 1)) / 0.5 - 1;
    }

    public String descricaoFerraria(int nivel) {
        return "Épico+ " + pct(chancePeloMenos(Raridade.EPICO, nivel))
                + ", Lendário+ " + pct(chancePeloMenos(Raridade.LENDARIO, nivel))
                + ", bônus +" + pct(bonusQualidade(nivel));
    }

    public static String pct(double v) {
        return StatsManager.fmt(Math.round(v * 1000) / 10.0) + "%";
    }

    // =====================================================================
    //  Sorteio
    // =====================================================================

    /** Bônus que todo item da categoria recebe primeiro (arma sempre tem dano etc.). */
    private static Stat principal(Categoria c) {
        return switch (c.tipo()) {
            case CORPO_A_CORPO -> Stat.DANO;
            case DISTANCIA -> Stat.DANO_FLECHA;
            case FERRAMENTA -> Stat.VEL_MINERACAO;
            case ARMADURA, ESCUDO -> Stat.ARMADURA;
            case MAGICO -> Stat.POTENCIA_MAGICA;
            case PESCA -> Stat.ISCA_RAPIDA;
            case VOO -> Stat.IMPULSO;
            case MONTARIA -> Stat.VELOCIDADE;
            case VEICULO -> c == Categoria.CARRINHO ? Stat.VEL_TRILHOS : Stat.PESCA_EMBARCADA;
        };
    }

    DadosForja sortear(Categoria cat, Raridade r, int nivel, String ferreiro) {
        List<Stat> stats = new ArrayList<>();
        for (Stat s : Stat.values()) if (s.podeEm(cat)) stats.add(s);
        Collections.shuffle(stats, rnd());
        Stat principal = principal(cat);
        if (stats.remove(principal)) stats.addFirst(principal);

        Map<Stat, Double> status = new EnumMap<>(Stat.class);
        for (int i = 0; i < Math.min(r.qtdStatus(), stats.size()); i++) {
            status.put(stats.get(i), sortearQualidade(nivel));
        }

        List<Efeito> possiveis = new ArrayList<>();
        for (Efeito e : Efeito.values()) if (e.podeEm(cat, r)) possiveis.add(e);
        Collections.shuffle(possiveis, rnd());
        if (r == Raridade.MITICO) {
            // Mítico sempre leva um efeito exclusivo, se a categoria tiver algum.
            for (int i = 0; i < possiveis.size(); i++) {
                if (possiveis.get(i).exclusivoMitico()) {
                    possiveis.addFirst(possiveis.remove(i));
                    break;
                }
            }
        }
        List<EfeitoRolado> efeitos = new ArrayList<>();
        for (int i = 0; i < Math.min(r.qtdEfeitos(), possiveis.size()); i++) {
            Efeito e = possiveis.get(i);
            efeitos.add(new EfeitoRolado(e, sortearNivel(e, r), sortearChance(e, r, nivel)));
        }
        return new DadosForja(r, ferreiro, status, efeitos, 0, List.of());
    }

    private int sortearNivel(Efeito e, Raridade r) {
        if (e.nivelMax() == 0) return 0;
        if (r == Raridade.MITICO) return e.nivelMax();
        if (r == Raridade.LENDARIO) return rnd().nextInt(Math.min(1, e.nivelMax()) + 1);
        return 0;
    }

    private double sortearChance(Efeito e, Raridade r, int nivel) {
        if (e.gatilho() == Efeito.Gatilho.PASSIVO || e.chanceBase() >= 1) return 1;
        double c = e.chanceBase() * r.fatorChance() * (0.85 + 0.3 * sortearQualidade(nivel));
        return Math.round(c * 1000) / 1000.0;
    }

    // =====================================================================
    //  Montar o item
    // =====================================================================

    /**
     * Forja o item: sorteia tudo com o nível de Ferraria do jogador.
     * Mantém o que o item já tinha (encantamentos, nome, acabamento).
     */
    public ItemStack forjar(Player ferreiro, ItemStack base, Raridade forcada) {
        Categoria cat = Categoria.de(base.getType());
        if (cat == null) return base;
        int nivel = ferreiro == null ? 0 : plugin.stats().getNivel(ferreiro, Skill.FERRARIA);
        Raridade r = forcada != null ? forcada : sortearRaridade(nivel);
        // Ferreiro e Mestre Forjador: chance de sair uma raridade acima.
        if (forcada == null && ferreiro != null && r != Raridade.MITICO
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < plugin.classes().chanceRaridadeExtra(ferreiro) + plugin.lendas().chanceRaridadeExtra(ferreiro)
                        + plugin.talentos().chanceRaridadeExtra(ferreiro)) {
            r = Raridade.values()[r.ordinal() + 1];
            // Obra-Prima: chance de subir mais uma.
            if (r != Raridade.MITICO && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < plugin.talentos().chanceSegundaRaridade(ferreiro)) {
                r = Raridade.values()[r.ordinal() + 1];
            }
        }
        DadosForja d = sortear(cat, r, nivel, ferreiro == null ? "Desconhecido" : ferreiro.getName());
        ItemStack item = base.clone();
        item.setAmount(1);
        construir(item, d);
        // Às vezes a peça já nasce com nome (mais chance nas raridades altas).
        if (plugin.settings().nomeadasAtivadas && Nomeada.de(item) == null && !br.rpgatributos.lenda.Lendas.marcado(item)
                && java.util.concurrent.ThreadLocalRandom.current().nextDouble() < Nomeada.chance(r) * plugin.settings().nomeadasChance) {
            Nomeada n = Nomeada.sortear(item.getType());
            if (n != null) {
                nomear(item, n);
                if (ferreiro != null) {
                    ferreiro.sendMessage(net.kyori.adventure.text.Component.text("⚒ A peça saiu da forja com nome: ", net.kyori.adventure.text.format.NamedTextColor.GOLD)
                            .append(net.kyori.adventure.text.Component.text(n.nome(), r.cor())));
                    plugin.diario().marco(ferreiro, "arma_nomeada", "Forjou uma arma com nome: " + n.nome());
                }
            }
        }
        return item;
    }

    /** Põe o nome (e o visual) numa peça forjada. */
    public void nomear(ItemStack item, Nomeada n) {
        item.editMeta(m -> m.getPersistentDataContainer().set(Nomeada.CHAVE, org.bukkit.persistence.PersistentDataType.STRING, n.name()));
        reconstruir(item);
    }

    /** Uma arma com nome pronta (forjada na raridade dada), para baús e chefes. */
    public ItemStack criarNomeada(Nomeada n, Raridade r) {
        ItemStack item = forjar(null, new ItemStack(n.sortearMaterial()), r);
        if (Nomeada.de(item) != n) nomear(item, n);
        return item;
    }

    /** Uma arma com nome qualquer. */
    public ItemStack criarNomeada(Raridade r) {
        Nomeada[] todas = Nomeada.values();
        return criarNomeada(todas[java.util.concurrent.ThreadLocalRandom.current().nextInt(todas.length)], r);
    }

    /** Refaz nome, atributos e descrição (ex.: depois de virar Netherite). */
    public boolean reconstruir(ItemStack item) {
        DadosForja d = ler(item);
        if (d == null) return false;
        construir(item, d);
        return true;
    }

    private NamespacedKey chaveModificador(Stat s, Categoria c) {
        return new NamespacedKey(plugin, "forja_" + s.name().toLowerCase(Locale.ROOT) + "_" + c.id());
    }

    public void construir(ItemStack item, DadosForja d) {
        Material tipo = item.getType();
        Categoria cat = Categoria.de(tipo);
        if (cat == null) return;
        Tier tier = Tier.de(tipo);
        Raridade r = d.raridade();
        double mult = multiplicador(d);

        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(kRaridade, PersistentDataType.STRING, r.id());
            pdc.set(kFerreiro, PersistentDataType.STRING, d.ferreiro());
            pdc.set(kStatus, PersistentDataType.STRING, d.statusTexto());
            pdc.set(kEfeitos, PersistentDataType.STRING, d.efeitosTexto());
            pdc.set(kRefino, PersistentDataType.INTEGER, d.refino());
            if (d.gemas().isEmpty()) pdc.remove(kGemas);
            else pdc.set(kGemas, PersistentDataType.STRING, d.gemasTexto());

            Component nome = cat == Categoria.CAJADO ? Component.text("Cajado Arcano", r.cor())
                    : Component.translatable(tipo.translationKey(), r.cor());
            if (d.refino() > 0) nome = nome.append(Component.text(" +" + d.refino(), r.cor()));
            meta.itemName(nome);
            if (cat == Categoria.CAJADO) br.rpgatributos.exploracao.PacoteRecursos.marcar(meta, "cajado_arcano");

            // Ao definir atributos no item, os padrões somem; por isso eles são copiados junto
            // (sempre presos ao slot certo: peitoral na mão não pode dar armadura).
            Multimap<Attribute, AttributeModifier> mods = ArrayListMultimap.create();
            tipo.getDefaultAttributeModifiers().forEach((a, m) -> {
                EquipmentSlotGroup grupo = m.getSlotGroup() == EquipmentSlotGroup.ANY ? cat.grupo() : m.getSlotGroup();
                mods.put(a, new AttributeModifier(m.getKey(), m.getAmount(), m.getOperation(), grupo));
                // Refino: dano/armadura base do item crescem 4% por nível.
                if (d.refino() > 0 && (a == Attribute.ATTACK_DAMAGE || a == Attribute.ARMOR)
                        && m.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                    mods.put(a, new AttributeModifier(new NamespacedKey(plugin, "forja_refino_" + cat.id() + "_"
                            + (a == Attribute.ARMOR ? "armadura" : "dano")),
                            m.getAmount() * DadosForja.BASE_POR_REFINO * d.refino(), AttributeModifier.Operation.ADD_NUMBER, grupo));
                }
            });
            d.status().forEach((s, qualidade) -> {
                if (s.atributo() == null) return;
                mods.put(s.atributo(), new AttributeModifier(chaveModificador(s, cat),
                        s.valor(qualidade, mult, tier), s.operacao(), cat.grupo()));
            });
            // Gemas engastadas: um modificador por engaste (os bônus especiais são somados em combate).
            for (int i = 0; i < d.gemas().size(); i++) {
                DadosForja.Engaste en = d.gemas().get(i);
                Gema.Bonus b = en.gema().bonus(cat);
                if (b == null || b.stat().atributo() == null) continue;
                mods.put(b.stat().atributo(), new AttributeModifier(new NamespacedKey(plugin, "forja_gema_" + i + "_" + cat.id()),
                        b.valor(en.grau()), b.stat().operacao(), cat.grupo()));
            }
            meta.setAttributeModifiers(mods);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // a descrição abaixo mostra tudo de forma mais clara
            meta.setEnchantmentGlintOverride(r.peloMenos(Raridade.LENDARIO) ? Boolean.TRUE : null);
            meta.lore(semItalico(montarDescricao(tipo, cat, tier, d)));
            plugin.lendas().decorar(meta, d.refino()); // se for uma lenda: nome, "Especial", poder e história
            Nomeada.decorar(meta, d.refino(), r.cor()); // arma com nome: nome, história e visual próprios
            plugin.runas().decorar(meta); // se tiver runa gravada, a linha dela volta depois de refazer a descrição
            plugin.mitrilo().decorar(meta, tipo); // aprimorado com Mitrilo: bônus e linha voltam também
            plugin.oficios().decorar(meta, tipo); // peça de conjunto do Ateliê: nome, bônus e o +1 de armadura voltam
            br.rpgatributos.combo.Ligacoes.decorar(meta); // óleo de lâmina passado: a linha dele volta
            plugin.pedrasAmolar().decorar(meta); // arma afiada: a linha do fio volta
        });
    }

    private List<Component> montarDescricao(Material tipo, Categoria cat, Tier tier, DadosForja d) {
        Raridade r = d.raridade();
        double mult = multiplicador(d);
        List<Component> l = new ArrayList<>();
        Component cabecalho = Component.text(r.estrelas() + " ", r.cor())
                .append(Component.text(r.nome().toUpperCase(Locale.ROOT), r.cor(), TextDecoration.BOLD));
        if (d.refino() > 0) cabecalho = cabecalho.append(Component.text("  +" + d.refino(), NamedTextColor.GOLD, TextDecoration.BOLD));
        l.add(cabecalho);

        Map<Stat, Double> valores = new EnumMap<>(Stat.class);
        d.status().forEach((s, q) -> valores.put(s, s.valor(q, mult, tier)));

        // Atributos padrão (dano, armadura...) já somados ao bônus da forja
        Map<Attribute, Double> base = new LinkedHashMap<>();
        for (Map.Entry<Attribute, AttributeModifier> e : tipo.getDefaultAttributeModifiers().entries()) {
            if (e.getValue().getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                base.merge(e.getKey(), e.getValue().getAmount(), Double::sum);
            }
        }
        Set<Stat> jaMostrados = EnumSet.noneOf(Stat.class);
        List<Component> linhasBase = new ArrayList<>();
        for (Attribute a : ORDEM_BASE) {
            Double padrao = base.get(a);
            Stat s = statDoAtributo(a);
            if (padrao == null || s == null) continue;
            double bonus = valores.getOrDefault(s, 0.0);
            if (bonus != 0) jaMostrados.add(s);
            if (a == Attribute.ATTACK_DAMAGE || a == Attribute.ARMOR) bonus += padrao * DadosForja.BASE_POR_REFINO * d.refino();
            double total = BASE_JOGADOR.getOrDefault(a, 0.0) + padrao + bonus;
            Component linha = Component.text(" " + s.icone() + " ", NamedTextColor.GRAY)
                    .append(Component.text(s.formatar(total).substring(1) + " " + s.nome(), NamedTextColor.DARK_GREEN));
            if (bonus != 0) linha = linha.append(Component.text(" (" + s.formatar(bonus) + ")", r.cor()));
            linhasBase.add(linha);
        }
        if (!linhasBase.isEmpty()) {
            l.add(Component.empty());
            l.add(Component.text(titulo(cat.grupo()), NamedTextColor.GRAY));
            l.addAll(linhasBase);
        }

        List<Component> bonus = new ArrayList<>();
        valores.forEach((s, v) -> {
            if (jaMostrados.contains(s)) return;
            bonus.add(Component.text(" " + s.icone() + " ", r.cor())
                    .append(Component.text(s.formatar(v) + " " + s.nome(), NamedTextColor.BLUE)));
        });
        if (!bonus.isEmpty()) {
            l.add(Component.empty());
            l.add(Component.text("Bônus de forja:", NamedTextColor.GRAY));
            l.addAll(bonus);
        }

        if (!d.efeitos().isEmpty()) {
            l.add(Component.empty());
            l.add(Component.text("Efeitos:", NamedTextColor.GRAY));
            for (EfeitoRolado er : d.efeitos()) {
                l.add(Component.text(" ✧ ", r.cor())
                        .append(Component.text(er.efeito().descrever(er.nivel(), er.chance(), cat), COR_EFEITO)));
            }
        }

        int engastes = cat != null && cat.aceitaGemas() ? d.engastes() : 0;
        if (engastes > 0) {
            l.add(Component.empty());
            l.add(Component.text("Engastes (" + d.gemas().size() + "/" + engastes + "):", NamedTextColor.GRAY));
            for (int i = 0; i < engastes; i++) {
                if (i >= d.gemas().size()) {
                    l.add(Component.text(" ◇ Engaste vazio", NamedTextColor.DARK_GRAY));
                    continue;
                }
                DadosForja.Engaste en = d.gemas().get(i);
                Gema.Bonus b = en.gema().bonus(cat);
                Component linha = Component.text(" ◆ ", en.gema().cor()).append(Component.text(en.gema().nome(en.grau()), en.gema().cor()));
                if (b != null) linha = linha.append(Component.text(": " + b.stat().formatar(b.valor(en.grau())) + " " + b.stat().nome(), NamedTextColor.WHITE));
                else if (cat == Categoria.CAJADO) linha = linha.append(Component.text(": +" + Math.round(en.gema().potenciaElemental(en.grau()) * 100)
                        + "% magias de " + en.gema().nomesElementos(), NamedTextColor.LIGHT_PURPLE));
                l.add(linha);
            }
        }

        if (cat.armadura()) {
            l.add(Component.empty());
            l.add(Component.text("Conjunto (4 peças " + r.nome() + " ou melhores):", NamedTextColor.GRAY));
            l.add(Component.text(" " + Conjuntos.texto(r), NamedTextColor.DARK_AQUA));
        }

        l.add(Component.empty());
        l.add(Component.text("⚒ Forjado por " + d.ferreiro(), NamedTextColor.DARK_GRAY));
        return l;
    }

    private static Stat statDoAtributo(Attribute a) {
        for (Stat s : Stat.values()) {
            if (s.atributo() == a && s.operacao() == AttributeModifier.Operation.ADD_NUMBER) return s;
        }
        return null;
    }

    private static String titulo(EquipmentSlotGroup g) {
        if (g == EquipmentSlotGroup.MAINHAND) return "Na mão principal:";
        if (g == EquipmentSlotGroup.HAND) return "Na mão:";
        return "Vestido:";
    }

    private static List<Component> semItalico(List<Component> l) {
        return l.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList();
    }

    /** O que aparece no resultado da bancada antes de pegar o item. */
    public ItemStack previa(ItemStack resultado, Player p) {
        int nivel = plugin.stats().getNivel(p, Skill.FERRARIA);
        ItemStack previa = resultado.clone();
        previa.editMeta(m -> m.lore(semItalico(List.of(
                Component.text("⚒ Será forjado ao retirar!", NamedTextColor.GOLD),
                Component.text("A raridade é sorteada na hora.", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("Ferraria Nv " + nivel + "  ·  Épico ou melhor: "
                        + pct(chancePeloMenos(Raridade.EPICO, nivel)), NamedTextColor.DARK_GRAY)))));
        return previa;
    }

    /** XP de Ferraria por fabricar o item. */
    public double xpFabricacao(Material tipo) {
        Categoria c = Categoria.de(tipo);
        if (c == null) return 0;
        Tier t = Tier.de(tipo);
        double xp = cfg().ferXpPorMaterial.getOrDefault(t, 1.0);
        return t == Tier.ESPECIAL ? xp : xp * c.custo();
    }

    // =====================================================================
    //  Show na hora de forjar
    // =====================================================================

    /** "[Espada de Ferro]" na cor da raridade, com o item aparecendo ao passar o mouse. */
    public Component nomeComHover(ItemStack item) {
        DadosForja d = ler(item);
        TextColor cor = d != null ? d.raridade().cor() : NamedTextColor.WHITE;
        return Component.text("[", cor)
                .append(item.effectiveName().colorIfAbsent(cor))
                .append(Component.text("]", cor))
                .hoverEvent(item);
    }

    /** Sons, partículas, título e anúncio. Com vários itens, o show é do melhor. */
    /** Conta as conquistas de forja (títulos). Só para forjas de verdade, não para o comando de admin. */
    public void registrarConquistas(Player p, List<ItemStack> itens) {
        int lendarios = 0, miticos = 0;
        for (ItemStack i : itens) {
            DadosForja d = ler(i);
            if (d == null) continue;
            if (d.raridade() == Raridade.LENDARIO) lendarios++;
            if (d.raridade() == Raridade.MITICO) miticos++;
        }
        var t = plugin.titulos();
        t.registrar(p, "forjados", itens.size());
        if (lendarios > 0) t.registrar(p, "forjado_lendario", lendarios);
        if (miticos > 0) t.registrar(p, "forjado_mitico", miticos);
    }

    public void celebrar(Player p, Location onde, List<ItemStack> itens) {
        ItemStack melhor = null;
        Raridade r = Raridade.COMUM;
        for (ItemStack i : itens) {
            DadosForja d = ler(i);
            if (d == null) continue;
            if (melhor == null || d.raridade().ordinal() > r.ordinal()) {
                melhor = i;
                r = d.raridade();
            }
            p.sendMessage(Component.text("⚒ Você forjou ", NamedTextColor.GRAY)
                    .append(nomeComHover(i))
                    .append(Component.text(" · " + d.raridade().nome(), d.raridade().cor())));
        }
        if (melhor == null) return;

        World w = onde.getWorld();
        Location centro = onde.clone().add(0, 0.6, 0);
        w.playSound(centro, Sound.BLOCK_ANVIL_USE, 0.6f, 1.15f);
        w.spawnParticle(Particle.CRIT, centro, 12, 0.3, 0.2, 0.3, 0.15);
        if (r.peloMenos(Raridade.RARO)) {
            w.playSound(centro, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.3f);
            w.spawnParticle(Particle.ENCHANTED_HIT, centro, 20, 0.35, 0.3, 0.35, 0.2);
        }
        if (r.peloMenos(Raridade.EPICO)) {
            w.playSound(centro, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            w.spawnParticle(Particle.ENCHANT, centro.clone().add(0, 0.6, 0), 80, 0.5, 0.5, 0.5, 0.8);
            w.spawnParticle(Particle.WITCH, centro, 15, 0.3, 0.3, 0.3, 0.05);
        }
        if (r.peloMenos(Raridade.UNICO)) {
            w.playSound(centro, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.4f);
            w.spawnParticle(Particle.END_ROD, centro, 30, 0.3, 0.6, 0.3, 0.08);
        }
        if (r.peloMenos(Raridade.LENDARIO)) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, centro, 90, 0.4, 0.7, 0.4, 0.45);
        }
        if (r == Raridade.MITICO) {
            if (cfg().forjaRaioMitico) w.strikeLightningEffect(onde);
            w.playSound(centro, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 60, 0.4, 0.8, 0.4, 0.06);
        }

        if (r.peloMenos(Raridade.EPICO)) {
            p.showTitle(Title.title(
                    Component.text("✦ " + r.nome().toUpperCase(Locale.ROOT) + " ✦", r.cor(), TextDecoration.BOLD),
                    melhor.effectiveName().colorIfAbsent(r.cor()),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2200), Duration.ofMillis(600))));
        }

        if (r.peloMenos(cfg().forjaAnunciar)) {
            plugin.getServer().broadcast(Component.text("⚒ ", NamedTextColor.GOLD)
                    .append(Component.text(p.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" forjou um item ", NamedTextColor.GRAY))
                    .append(Component.text(r.nome().toUpperCase(Locale.ROOT), r.cor(), TextDecoration.BOLD))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(nomeComHover(melhor)));
            if (r == Raridade.MITICO) {
                for (Player outro : plugin.getServer().getOnlinePlayers()) {
                    if (outro != p) outro.playSound(outro.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 0.8f);
                }
            }
        }
    }
}
