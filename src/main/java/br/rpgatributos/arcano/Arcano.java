package br.rpgatributos.arcano;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.StatsManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centro do sistema arcano: perfis dos jogadores, mana, bônus das partes do
 * corpo, carga/rejeição, instabilidade e o item Grimório.
 */
public final class Arcano {

    public static final TextColor COR = TextColor.color(0xC77DFF);
    /** De quantos em quantos ticks o tick() roda. */
    public static final int INTERVALO = 10;

    private final RPGAtributos plugin;
    private final NamespacedKey kInfusoes, kMana, kMagias, kSelecionada, kDescobertas, kInstavel, kGrimorio;
    private final NamespacedKey kVida, kDano, kVelocidade;
    private final Map<UUID, Perfil> perfis = new HashMap<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private int ciclo;

    private final BlocosTemporarios temporarios;
    private final Conjuracao conjuracao;
    private final Infusores infusores;
    private final MenusArcanos menus;
    private final Lendarias lendarias;

    public Arcano(RPGAtributos plugin) {
        this.plugin = plugin;
        this.temporarios = new BlocosTemporarios(plugin);
        this.conjuracao = new Conjuracao(plugin);
        this.infusores = new Infusores(plugin);
        this.menus = new MenusArcanos(plugin);
        this.lendarias = new Lendarias(plugin);
        kInfusoes = new NamespacedKey(plugin, "arcano_infusoes");
        kMana = new NamespacedKey(plugin, "arcano_mana");
        kMagias = new NamespacedKey(plugin, "arcano_magias");
        kSelecionada = new NamespacedKey(plugin, "arcano_selecionada");
        kDescobertas = new NamespacedKey(plugin, "arcano_descobertas");
        kInstavel = new NamespacedKey(plugin, "arcano_instavel");
        kGrimorio = new NamespacedKey(plugin, "grimorio");
        kVida = new NamespacedKey(plugin, "arcano_coracao");
        kDano = new NamespacedKey(plugin, "arcano_bracos");
        kVelocidade = new NamespacedKey(plugin, "arcano_pernas");
    }

    private Settings cfg() { return plugin.settings(); }

    public BlocosTemporarios temporarios() { return temporarios; }
    public Conjuracao conjuracao() { return conjuracao; }
    public Infusores infusores() { return infusores; }
    public MenusArcanos menus() { return menus; }
    public Lendarias lendarias() { return lendarias; }

    /** Registra os eventos e as tarefas repetidas. Chamado no onEnable. */
    public void iniciar() {
        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(temporarios, plugin);
        pm.registerEvents(infusores, plugin);
        pm.registerEvents(menus, plugin);
        pm.registerEvents(new ArcanoListener(plugin), plugin);
        pm.registerEvents(lendarias, plugin);
        plugin.efeitosPassivos().registrarFonte(lendarias::efeitos);

        var agenda = plugin.getServer().getScheduler();
        agenda.runTaskTimer(plugin, this::tick, INTERVALO, INTERVALO);
        agenda.runTaskTimer(plugin, lendarias::tick, 4L, 4L);
        agenda.runTaskTimer(plugin, infusores::tick, 5L, 5L);
        agenda.runTaskTimer(plugin, conjuracao::tickFardos, 40L, 40L);

        infusores.iniciar();
        for (Player p : plugin.getServer().getOnlinePlayers()) aplicarBonus(p);
    }

    /** Chamado no onDisable. */
    public void parar() {
        menus.fecharTodos();
        temporarios.reverterTodos();
        descarregarTodos();
    }

    // =====================================================================
    //  Perfis
    // =====================================================================

    public Perfil perfil(Player p) {
        Perfil pf = perfis.get(p.getUniqueId());
        if (pf == null) {
            pf = carregar(p);
            perfis.put(p.getUniqueId(), pf);
        }
        return pf;
    }

    private Perfil carregar(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Perfil pf = new Perfil();
        pf.lerInfusoes(pdc.get(kInfusoes, PersistentDataType.STRING));
        pf.lerMagias(pdc.get(kMagias, PersistentDataType.STRING));
        pf.lerDescobertas(pdc.get(kDescobertas, PersistentDataType.STRING));
        pf.selecionada = pdc.getOrDefault(kSelecionada, PersistentDataType.INTEGER, 0);
        pf.instavelAte = pdc.getOrDefault(kInstavel, PersistentDataType.LONG, 0L);
        Double mana = pdc.get(kMana, PersistentDataType.DOUBLE);
        pf.mana = mana == null ? manaMax(p, pf) : mana;
        return pf;
    }

    public void salvar(Player p) {
        Perfil pf = perfis.get(p.getUniqueId());
        if (pf == null) return;
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        pdc.set(kInfusoes, PersistentDataType.STRING, pf.infusoesTexto());
        pdc.set(kMagias, PersistentDataType.STRING, pf.magiasTexto());
        pdc.set(kDescobertas, PersistentDataType.STRING, pf.descobertasTexto());
        pdc.set(kSelecionada, PersistentDataType.INTEGER, pf.selecionada);
        pdc.set(kInstavel, PersistentDataType.LONG, pf.instavelAte);
        pdc.set(kMana, PersistentDataType.DOUBLE, pf.mana);
    }

    public void descarregar(Player p) {
        salvar(p);
        perfis.remove(p.getUniqueId());
        BossBar b = barras.remove(p.getUniqueId());
        if (b != null) p.hideBossBar(b);
    }

    public void descarregarTodos() {
        for (Player p : plugin.getServer().getOnlinePlayers()) descarregar(p);
    }

    // =====================================================================
    //  Números
    // =====================================================================

    public int nivel(Player p) {
        return plugin.stats().getNivel(p, Skill.ARCANO);
    }

    public double manaMax(Player p, Perfil pf) {
        return cfg().arcManaBase + nivel(p) * cfg().arcManaPorNivel + pf.poder(ParteCorpo.MENTE) * ParteCorpo.MANA_POR_PODER
                + plugin.titulos().bonusMana(p) + plugin.classes().bonusMana(p);
    }

    public double regen(Player p, Perfil pf) {
        double r = cfg().arcRegenBase + nivel(p) * cfg().arcRegenPorNivel
                + pf.poder(ParteCorpo.SANGUE) * ParteCorpo.REGEN_POR_PODER + plugin.titulos().bonusRegenMana(p)
                + plugin.classes().bonusRegenMana(p);
        return rejeitando(p, pf) ? r * 0.5 : r;
    }

    /** Carga que o corpo aguenta sem rejeição. */
    public int capacidade(Player p) {
        return capacidade(nivel(p));
    }

    private int capacidade(int nivel) {
        return cfg().arcCargaBase + (int) (nivel * cfg().arcCargaPorNivel);
    }

    /** Carga máxima absoluta (capacidade + sobrecarga). */
    public int limiteCarga(Player p) {
        return capacidade(p) + cfg().arcSobrecargaMax;
    }

    public boolean rejeitando(Player p, Perfil pf) {
        return pf.carga() > capacidade(p);
    }

    public int espacosGrimorio(Player p) {
        return espacosGrimorio(nivel(p));
    }

    /** De 3 espaços no nível 0 até 8 no nível máximo. */
    private int espacosGrimorio(int nivel) {
        return Math.min(Perfil.MAX_MAGIAS, 3 + (int) (5.0 * nivel / cfg().nivelMaximo));
    }

    /** Nível de Arcano que libera o espaço {@code i} do grimório (0 a 7). */
    public int nivelParaEspaco(int i) {
        return i < 3 ? 0 : (int) Math.ceil((i - 2) * cfg().nivelMaximo / 5.0);
    }

    public boolean liberada(Player p, ParteCorpo parte) {
        return parte.liberada(nivel(p), cfg().nivelMaximo);
    }

    public int nivelDaParte(ParteCorpo parte) {
        return parte.nivelNecessario(cfg().nivelMaximo);
    }

    public double chanceInfusao(Player p, Material m) {
        double c = cfg().arcChanceBase + nivel(p) * cfg().arcChancePorNivel - 0.1 * Catalogo.raridade(m);
        return Math.max(0.2, Math.min(1, c));
    }

    /** Níveis de XP do Minecraft gastos para infundir. */
    public int custoXp(Material m) {
        return 2 + 2 * Catalogo.raridade(m) + Catalogo.poder(m);
    }

    public double custoMana(Magia m, Perfil pf) {
        double c = cfg().arcCustoBase * Math.pow(m.essencias().size(), 1.4) * m.forma().multCusto();
        if (pf.instavel()) c *= 1.4;
        return Math.round(c);
    }

    public int recargaTicks(Magia m) {
        Receita r = m.receita();
        return r != null ? r.recarga() * 20 : recargaGenerica(m);
    }

    /** Recarga sem contar receita secreta (o criador não pode entregar o segredo). */
    public int recargaGenerica(Magia m) {
        double s = 1 + 1.2 * m.essencias().size() + (m.forma() == Forma.AURA ? 2 : 0);
        return (int) Math.round(s * 20);
    }

    /** Força da magia: vem da intensidade das essências usadas e do nível de Arcano. */
    public double potencia(Player p, Perfil pf, Magia m) {
        Map<Essencia, Integer> total = pf.essencias();
        double soma = 0;
        for (Essencia e : m.essencias()) soma += total.getOrDefault(e, 0);
        double media = soma / m.essencias().size();
        // Até +50% de força no nível máximo de Arcano.
        double pot = (1 + 0.15 * Math.min(media, 10)) * (1 + 0.5 * nivel(p) / cfg().nivelMaximo);
        return pf.instavel() ? pot * 0.65 : pot;
    }

    public String descricaoNivel(int nivel) {
        int partes = 0;
        for (ParteCorpo pc : ParteCorpo.values()) if (pc.liberada(nivel, cfg().nivelMaximo)) partes++;
        return "+" + StatsManager.fmt(nivel * cfg().arcManaPorNivel) + " mana, "
                + partes + " partes do corpo, carga " + capacidade(nivel) + ", "
                + espacosGrimorio(nivel) + " magias";
    }

    // =====================================================================
    //  Bônus das partes do corpo (atributos nativos)
    // =====================================================================

    public void aplicarBonus(Player p) {
        Perfil pf = perfil(p);
        definir(p, Attribute.MAX_HEALTH, kVida, pf.poder(ParteCorpo.CORACAO) * ParteCorpo.VIDA_POR_PODER,
                AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.ATTACK_DAMAGE, kDano, pf.poder(ParteCorpo.BRACOS) * ParteCorpo.DANO_POR_PODER,
                AttributeModifier.Operation.ADD_NUMBER);
        definir(p, Attribute.MOVEMENT_SPEED, kVelocidade, pf.poder(ParteCorpo.PERNAS) * ParteCorpo.VELOCIDADE_POR_PODER,
                AttributeModifier.Operation.ADD_SCALAR);
        lendarias.aplicar(p);
    }

    private static void definir(Player p, Attribute atributo, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(atributo);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        if (atributo == Attribute.MAX_HEALTH && p.getHealth() > inst.getValue()) p.setHealth(inst.getValue());
    }

    // =====================================================================
    //  Grimório
    // =====================================================================

    public ItemStack criarGrimorio() {
        ItemStack g = new ItemStack(Material.KNOWLEDGE_BOOK);
        g.editMeta(m -> {
            m.itemName(Component.text("Grimório", COR));
            m.lore(List.of(
                    Component.text("Guarda e lança as suas magias.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Clique direito: ", NamedTextColor.YELLOW).append(Component.text("lançar a magia", NamedTextColor.WHITE)),
                    Component.text("Clique esquerdo: ", NamedTextColor.YELLOW).append(Component.text("trocar de magia", NamedTextColor.WHITE)),
                    Component.text("Agachado + clique direito: ", NamedTextColor.YELLOW).append(Component.text("abrir", NamedTextColor.WHITE)))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(kGrimorio, PersistentDataType.BYTE, (byte) 1);
        });
        return g;
    }

    public boolean ehGrimorio(ItemStack item) {
        return item != null && item.getType() == Material.KNOWLEDGE_BOOK
                && item.getPersistentDataContainer().has(kGrimorio);
    }

    public boolean temGrimorio(Player p) {
        for (ItemStack i : p.getInventory().getContents()) if (ehGrimorio(i)) return true;
        return false;
    }

    // =====================================================================
    //  Mana e atualização periódica
    // =====================================================================

    /** Recupera mana (ex.: comendo Salada Arcana). */
    public void darMana(Player p, double quanto) {
        Perfil pf = perfil(p);
        pf.mana = Math.min(manaMax(p, pf), pf.mana + quanto);
        mostrarMana(pf, 60);
    }

    public void encherMana(Player p) {
        Perfil pf = perfil(p);
        pf.mana = manaMax(p, pf);
        pf.recargas.clear();
    }

    public void gastarMana(Player p, Perfil pf, double quanto) {
        pf.mana = Math.max(0, pf.mana - quanto);
        mostrarMana(pf, 100);
    }

    public void mostrarMana(Perfil pf, int ticks) {
        pf.mostrarManaAte = Math.max(pf.mostrarManaAte, Bukkit.getCurrentTick() + ticks);
    }

    /** Roda a cada {@link #INTERVALO} ticks. */
    public void tick() {
        ciclo++;
        int agora = Bukkit.getCurrentTick();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Perfil pf = perfil(p);
            double max = manaMax(p, pf);
            if (pf.mana < max && !p.isDead()) pf.mana = Math.min(max, pf.mana + regen(p, pf) * INTERVALO / 20.0);
            if (pf.mana > max) pf.mana = max;

            if (pf.avisouInstavel && !pf.instavel()) {
                pf.avisouInstavel = false;
                p.sendMessage(Component.text("✦ Suas infusões se estabilizaram.", COR));
            }
            if (ciclo % (200 / INTERVALO) == 0 && rejeitando(p, pf)) rejeitar(p, pf);
            atualizarBarra(p, pf, max, agora);
        }
        if (ciclo % (1200 / INTERVALO) == 0) {
            for (Player p : plugin.getServer().getOnlinePlayers()) salvar(p);
        }
    }

    private void rejeitar(Player p, Perfil pf) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        }
        p.sendActionBar(Component.text("☠ Seu corpo rejeita as infusões (carga " + pf.carga() + "/" + capacidade(p) + ")",
                NamedTextColor.RED));
    }

    private void atualizarBarra(Player p, Perfil pf, double max, int agora) {
        boolean segurando = ehGrimorio(p.getInventory().getItemInMainHand());
        boolean mostrar = segurando || pf.mana < max - 0.01 || agora < pf.mostrarManaAte;
        BossBar barra = barras.get(p.getUniqueId());
        if (!mostrar) {
            if (barra != null) {
                p.hideBossBar(barra);
                barras.remove(p.getUniqueId());
            }
            return;
        }
        Component nome = Component.text("✦ Mana " + (int) pf.mana + " / " + (int) max, COR);
        BossBar.Color cor = BossBar.Color.PURPLE;
        if (pf.instavel()) {
            nome = nome.append(Component.text("  · Instável " + pf.segundosInstavel() + "s", NamedTextColor.RED));
            cor = BossBar.Color.RED;
        }
        if (rejeitando(p, pf)) {
            nome = nome.append(Component.text("  · Rejeição", NamedTextColor.DARK_RED));
            cor = BossBar.Color.RED;
        }
        if (segurando) {
            Magia m = pf.magiaSelecionada();
            if (m != null) nome = nome.append(Component.text("  ·  " + m.nome(), NamedTextColor.WHITE));
        }
        float progresso = (float) Math.max(0, Math.min(1, max <= 0 ? 0 : pf.mana / max));
        if (barra == null) {
            barra = BossBar.bossBar(nome, progresso, cor, BossBar.Overlay.NOTCHED_10);
            barras.put(p.getUniqueId(), barra);
            p.showBossBar(barra);
        } else {
            barra.name(nome);
            barra.progress(progresso);
            barra.color(cor);
        }
    }
}
