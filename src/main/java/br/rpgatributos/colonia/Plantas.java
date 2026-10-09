package br.rpgatributos.colonia;

import br.rpgatributos.RPGAtributos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Os projetos de construção da colônia. Os que vêm com o plugin são copiados para
 * plugins/RPGAtributos/plantas/ na primeira vez; o admin pode editar ou criar outros ali
 * (um .yml por projeto) e usar /rpgadmin reload.
 */
public final class Plantas {

    /** Projetos que vêm com o plugin (arquivo plantas/&lt;id&gt;.yml dentro do jar). */
    private static final List<String> PADRAO = List.of("casa", "fazenda", "poco", "torre_de_vigia", "armazem", "biblioteca", "quartel");

    private final RPGAtributos plugin;
    private final NamespacedKey kProjeto;
    private final Map<String, Planta> porId = new LinkedHashMap<>();

    Plantas(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kProjeto = new NamespacedKey(plugin, "projeto_colonia");
    }

    void carregar() {
        porId.clear();
        File pasta = new File(plugin.getDataFolder(), "plantas");
        if (!pasta.exists() && !pasta.mkdirs()) plugin.getLogger().warning("Não consegui criar a pasta plantas/");
        for (String id : PADRAO) {
            if (!new File(pasta, id + ".yml").exists()) {
                try {
                    plugin.saveResource("plantas/" + id + ".yml", false);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Projeto padrão sumiu do jar: plantas/" + id + ".yml");
                }
            }
        }
        // Os padrão primeiro (na ordem acima), depois os criados pelo admin.
        List<File> arquivos = new ArrayList<>();
        for (String id : PADRAO) arquivos.add(new File(pasta, id + ".yml"));
        File[] outros = pasta.listFiles((dir, nome) -> nome.endsWith(".yml"));
        if (outros != null) {
            java.util.Arrays.sort(outros);
            for (File f : outros) if (!arquivos.contains(f)) arquivos.add(f);
        }
        for (File f : arquivos) {
            if (!f.exists()) continue;
            String id = f.getName().substring(0, f.getName().length() - 4).toLowerCase(Locale.ROOT);
            try {
                porId.put(id, Planta.ler(id, YamlConfiguration.loadConfiguration(f)));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Projeto de colônia inválido (plantas/" + f.getName() + "): " + ex.getMessage());
            }
        }
        plugin.getLogger().info(porId.size() + " projeto(s) de colônia carregado(s).");
    }

    public Planta de(String id) { return id == null ? null : porId.get(id.toLowerCase(Locale.ROOT)); }

    public Collection<Planta> todas() { return Collections.unmodifiableCollection(porId.values()); }

    // =====================================================================
    //  O item do projeto
    // =====================================================================

    /** O papel do projeto, que o jogador coloca no chão para marcar a obra. */
    ItemStack item(Planta p) {
        ItemStack i = new ItemStack(Material.PAPER);
        i.editMeta(m -> {
            m.displayName(Component.text("📜 Projeto: " + p.nome(), Prefeituras.COR, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String d : p.descricao()) lore.add(Component.text(d, NamedTextColor.GRAY));
            lore.add(Component.text("Tamanho: " + p.largura() + "×" + p.profundidade() + ", altura " + p.altura(), NamedTextColor.WHITE));
            lore.add(Component.text(p.efeito().texto(), NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(Component.text("» Clique com o botão direito no chão onde", NamedTextColor.YELLOW));
            lore.add(Component.text("  quer a obra. O lado em que VOCÊ está", NamedTextColor.YELLOW));
            lore.add(Component.text("  vira a ENTRADA.", NamedTextColor.YELLOW));
            lore.add(Component.text("  Aparece uma cerca em volta: deixe um baú", NamedTextColor.GRAY));
            lore.add(Component.text("  com os materiais dentro dela.", NamedTextColor.GRAY));
            m.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.addItemFlags(ItemFlag.values());
            m.setEnchantmentGlintOverride(true);
            m.getPersistentDataContainer().set(kProjeto, PersistentDataType.STRING, p.id());
        });
        return i;
    }

    /** O projeto desse item (null se não for um projeto). */
    Planta deItem(ItemStack i) {
        if (i == null || i.isEmpty() || i.getType() != Material.PAPER || !i.hasItemMeta()) return null;
        return de(i.getItemMeta().getPersistentDataContainer().get(kProjeto, PersistentDataType.STRING));
    }
}
