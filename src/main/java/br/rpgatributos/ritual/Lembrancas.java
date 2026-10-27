package br.rpgatributos.ritual;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.domador.Lendaria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 2.89: quando um companheiro morre, fica a lembrança dele (a criatura inteira, com o nível e a
 * evolução) e cai a coleira com o nome dele. No Altar da Convergência, a coleira (ou uma
 * etiqueta com o nome do companheiro) traz ele de volta. Os componentes e o alforje caíram no
 * chão quando ele morreu, então ele volta sem eles. As montarias lendárias não entram aqui
 * (elas têm o próprio jeito de voltar).
 */
public final class Lembrancas implements Listener {

    static final NamespacedKey K_COLEIRA = new NamespacedKey("rpgatributos", "coleira_companheiro");
    private static final int MAX_POR_DONO = 10;
    /** O que caiu no chão quando ele morreu (não volta junto). */
    private static final List<NamespacedKey> CAIRAM = List.of(new NamespacedKey("rpgatributos", "companheiro_componentes"),
            new NamespacedKey("rpgatributos", "companheiro_alforje"), br.rpgatributos.detalhes.ItensDetalhes.K_TEM_FERRADURA);

    private final RPGAtributos plugin;
    private final File arquivo;
    private final YamlConfiguration dados;

    public Lembrancas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "lembrancas.yml");
        this.dados = YamlConfiguration.loadConfiguration(arquivo);
    }

    private void salvar() {
        try { dados.save(arquivo); } catch (IOException e) { plugin.getLogger().warning("Não deu para salvar lembrancas.yml: " + e.getMessage()); }
    }

    // =====================================================================
    //  Coleira
    // =====================================================================

    static ItemStack coleira(UUID id, String nome) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            m.itemName(Component.text("Coleira de " + nome, Companheiros.COR));
            m.setItemModel(NamespacedKey.minecraft("lead"));
            m.setMaxStackSize(1);
            m.lore(List.of(
                    Component.text("A lembrança de um companheiro que se foi.", NamedTextColor.GRAY),
                    Component.text("No Altar da Convergência (ritual Trazer de Volta),", NamedTextColor.YELLOW),
                    Component.text("ela traz " + nome + " de volta, com o nível e a evolução.", NamedTextColor.YELLOW))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_COLEIRA, PersistentDataType.STRING, id.toString());
        });
        return i;
    }

    /** A coleira de um companheiro, ou uma etiqueta com nome (o catalisador do Trazer de Volta). */
    static boolean serveDeCatalisador(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return false;
        if (i.getItemMeta().getPersistentDataContainer().has(K_COLEIRA)) return true;
        return i.getType() == Material.NAME_TAG && i.getItemMeta().hasCustomName();
    }

    // =====================================================================
    //  Morte: guarda a lembrança
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity m = e.getEntity();
        if (!Companheiros.eh(m) || Lendaria.de(m) != null) return;
        UUID dono = Companheiros.dono(m);
        EntitySnapshot foto = m.createSnapshot();
        if (dono == null || foto == null) return;
        String nome = Companheiros.nome(m);
        String chave = m.getUniqueId().toString();
        dados.set(chave + ".dono", dono.toString());
        dados.set(chave + ".tipo", m.getType().name());
        dados.set(chave + ".nome", nome);
        dados.set(chave + ".quando", System.currentTimeMillis());
        dados.set(chave + ".criatura", foto.getAsString());
        // Só as 10 mais recentes de cada dono.
        List<String> dele = new ArrayList<>(doDono(dono));
        dele.sort(Comparator.comparingLong(k -> dados.getLong(k + ".quando")));
        while (dele.size() > MAX_POR_DONO) dados.set(dele.removeFirst(), null);
        salvar();
        e.getDrops().add(coleira(m.getUniqueId(), nome));
        Player p = Bukkit.getPlayer(dono);
        if (p != null) {
            p.sendMessage(Component.text("♞ A coleira de " + nome + " ficou no chão. No Altar da Convergência, ela traz " + nome + " de volta.",
                    Companheiros.COR));
        }
    }

    private List<String> doDono(UUID dono) {
        List<String> l = new ArrayList<>();
        for (String k : dados.getKeys(false)) if (dono.toString().equals(dados.getString(k + ".dono"))) l.add(k);
        return l;
    }

    /** A lembrança que esse catalisador chama (pela coleira, ou pelo nome entre os companheiros de {@code quem}). */
    private String achar(ItemStack catalisador, Player quem) {
        var pdc = catalisador.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(K_COLEIRA, PersistentDataType.STRING);
        if (id != null) return dados.isConfigurationSection(id) ? id : null;
        String nome = PlainTextComponentSerializer.plainText().serialize(catalisador.getItemMeta().customName()).trim();
        String melhor = null;
        for (String k : doDono(quem.getUniqueId())) {
            if (!nome.equalsIgnoreCase(dados.getString(k + ".nome", ""))) continue;
            if (melhor == null || dados.getLong(k + ".quando") > dados.getLong(melhor + ".quando")) melhor = k;
        }
        return melhor;
    }

    // =====================================================================
    //  O ritual
    // =====================================================================

    static ReceitaRitual.Efeito efeito() {
        return new ReceitaRitual.Efeito() {
            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo) { }

            @Override
            public void aplicar(RPGAtributos plugin, Location altar, Player quem, List<Player> grupo, ItemStack catalisador) {
                plugin.lembrancas().trazer(altar, quem, catalisador);
            }

            @Override
            public String impedimento(RPGAtributos plugin, Location altar, Player quem, ItemStack catalisador) {
                return plugin.lembrancas().porqueNao(quem, catalisador);
            }
        };
    }

    String porqueNao(Player quem, ItemStack catalisador) {
        String k = achar(catalisador, quem);
        if (k == null) {
            return catalisador.getType() == Material.NAME_TAG ? "Nenhum companheiro seu que morreu tem esse nome (o nome precisa ser igual)."
                    : "Esse companheiro já voltou (essa coleira é só uma lembrança).";
        }
        if (!quem.getUniqueId().toString().equals(dados.getString(k + ".dono"))) {
            return "Só o dono de " + dados.getString(k + ".nome") + " pode trazer ele de volta.";
        }
        if (plugin.companheiros().de(quem.getUniqueId()).size() >= plugin.companheiros().limite(quem)) {
            return "Você já tem o máximo de companheiros. Liberte um (ou suba a Doma) antes.";
        }
        return null;
    }

    private void trazer(Location altar, Player quem, ItemStack catalisador) {
        String k = quem == null ? null : achar(catalisador, quem);
        Location onde = altar.clone().add(0.5, 1.1, 0.5);
        if (k == null) {
            altar.getWorld().dropItemNaturally(onde, catalisador);
            return;
        }
        Entity e;
        try {
            EntitySnapshot foto = Bukkit.getEntityFactory().createEntitySnapshot(dados.getString(k + ".criatura"));
            e = foto.createEntity(onde);
        } catch (IllegalArgumentException x) {
            plugin.getLogger().warning("Lembrança de companheiro inválida (" + k + "): " + x.getMessage());
            altar.getWorld().dropItemNaturally(onde, catalisador);
            return;
        }
        if (!(e instanceof LivingEntity v)) {
            e.remove();
            altar.getWorld().dropItemNaturally(onde, catalisador);
            return;
        }
        // Volta inteiro, sem o que caiu no chão quando morreu (sela, armadura, baú, componentes).
        var max = v.getAttribute(Attribute.MAX_HEALTH);
        v.setHealth(max == null ? 20 : max.getValue());
        v.setFireTicks(0);
        v.setNoDamageTicks(40);
        if (v.getEquipment() != null) v.getEquipment().clear();
        if (v instanceof AbstractHorse h) h.getInventory().clear();
        if (v instanceof ChestedHorse c) c.setCarryingChest(false);
        for (NamespacedKey ch : CAIRAM) v.getPersistentDataContainer().remove(ch);
        String nome = dados.getString(k + ".nome", "Seu companheiro");
        dados.set(k, null);
        salvar();
        v.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, onde, 80, 0.5, 0.8, 0.5, 0.4);
        v.getWorld().spawnParticle(Particle.HEART, onde.clone().add(0, 1, 0), 10, 0.5, 0.4, 0.5);
        v.getWorld().playSound(onde, Sound.ITEM_TOTEM_USE, 1f, 1.1f);
        quem.sendMessage(Component.text("♞ " + nome + " voltou para você!", Companheiros.COR, TextDecoration.BOLD));
    }

    /** Para o comando de admin: as lembranças de alguém. */
    public List<String> listar(UUID dono) {
        List<String> l = new ArrayList<>();
        for (String k : doDono(dono)) {
            ConfigurationSection s = dados.getConfigurationSection(k);
            if (s != null) l.add(s.getString("nome") + " (" + s.getString("tipo") + ")");
        }
        return l;
    }
}
