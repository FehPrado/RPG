package br.rpgatributos.pesca;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.fazenda.Qualidade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Peso dos peixes (2.50): cada peixe pescado tem um peso, que depende da espécie, da qualidade e
 * do nível de Pesca. O maior de cada espécie fica como recorde pessoal (e do servidor). Para o
 * peixe continuar empilhando, só os troféus (recorde ou peixe enorme) guardam o peso no item.
 */
public final class PesoPeixe {

    private PesoPeixe() { }

    public static final NamespacedKey K_PESO = new NamespacedKey("rpgatributos", "peso_peixe");
    private static final TextColor COR = TextColor.color(0x4FC3F7);

    /** Espécie comum: id, nome e faixa de peso (kg). */
    private record Comum(Material tipo, String id, String nome, double min, double max) { }

    private static final Comum[] COMUNS = {
            new Comum(Material.COD, "bacalhau", "Bacalhau", 0.8, 4.5),
            new Comum(Material.SALMON, "salmao", "Salmão", 1.5, 7.0),
            new Comum(Material.TROPICAL_FISH, "tropical", "Peixe Tropical", 0.1, 0.7),
            new Comum(Material.PUFFERFISH, "baiacu", "Baiacu", 0.3, 1.8)};

    private static Comum comum(Material m) {
        for (Comum c : COMUNS) if (c.tipo == m) return c;
        return null;
    }

    /** Faixa de peso: a do peixe comum da mesma base, maior quanto mais raro (XP) for o peixe. */
    private static double[] faixa(PeixeRaro r) {
        Comum c = comum(r.base());
        double mult = 1 + r.xp() / 100.0;
        return c == null ? new double[]{0.5, 3 * mult} : new double[]{c.min * (1 + r.xp() / 300.0), c.max * mult};
    }

    /** Nome da espécie pelo id do recorde ("bacalhau", "raro_carpa_dourada"...). */
    public static String nome(String id) {
        for (Comum c : COMUNS) if (c.id.equals(id)) return c.nome;
        if (id.startsWith("raro_")) {
            try {
                return PeixeRaro.valueOf(id.substring(5).toUpperCase(Locale.ROOT)).nome();
            } catch (IllegalArgumentException ignorado) {
                // espécie que não existe mais
            }
        }
        return id;
    }

    public static String kg(double v) {
        return String.format(Locale.ROOT, "%.1f", v).replace('.', ',') + " kg";
    }

    private static NamespacedKey chave(String id) {
        return new NamespacedKey("rpgatributos", "rec_peixe_" + id);
    }

    /** O recorde pessoal (0 = nunca pescou). */
    public static double recorde(Player p, String id) {
        return p.getPersistentDataContainer().getOrDefault(chave(id), PersistentDataType.DOUBLE, 0.0);
    }

    /** Pesa o peixe que acabou de vir: avisa, registra recordes e, se for troféu, escreve o peso no item. */
    static void pesar(RPGAtributos plugin, Player p, ItemStack peixe, PeixeRaro raro, Qualidade q, double fracaoNivel) {
        String id;
        String nome;
        double min, max;
        if (raro != null) {
            double[] f = faixa(raro);
            id = "raro_" + raro.id();
            nome = raro.nome();
            min = f[0];
            max = f[1];
        } else {
            Comum c = comum(peixe.getType());
            if (c == null) return;
            id = c.id;
            nome = c.nome;
            min = c.min;
            max = c.max;
        }
        double r = ThreadLocalRandom.current().nextDouble();
        double kg = (min + (max - min) * Math.pow(r, 1.8)) * (q == null ? 1 : 1 + (q.fator() - 1) * 0.5) * (1 + 0.15 * fracaoNivel);
        kg = Math.round(kg * 10) / 10.0;
        double antes = recorde(p, id);
        boolean pessoal = kg > antes;
        if (pessoal) p.getPersistentDataContainer().set(chave(id), PersistentDataType.DOUBLE, kg);
        plugin.recordes().peixe(p, id, kg);
        boolean trofeu = (pessoal && antes > 0) || kg >= max * 0.85;
        if (trofeu) {
            plugin.titulos().registrar(p, "peixes_trofeu", 1);
            double fkg = kg;
            peixe.editMeta(m -> {
                m.getPersistentDataContainer().set(K_PESO, PersistentDataType.DOUBLE, fkg);
                List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
                lore.add(Component.text("⚖ " + kg(fkg) + " · troféu", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                m.lore(lore);
            });
        }
        Component msg = Component.text("🎣 " + nome + " — ", COR).append(Component.text(kg(kg), NamedTextColor.WHITE, TextDecoration.BOLD));
        if (pessoal && antes > 0) {
            msg = msg.append(Component.text("  ★ novo recorde pessoal! (era " + kg(antes) + ")", NamedTextColor.GOLD));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
        }
        p.sendActionBar(msg);
    }

    /** Linhas de recorde para o Diário do Pescador. */
    static List<Component> linhas(RPGAtributos plugin, Player p, String id) {
        List<Component> l = new ArrayList<>();
        double meu = recorde(p, id);
        l.add(Component.text("⚖ Seu recorde: " + (meu > 0 ? kg(meu) : "—"), meu > 0 ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY));
        String srv = plugin.recordes().textoPeixe(id);
        if (srv != null) l.add(Component.text("🏆 Do servidor: " + srv, NamedTextColor.GOLD));
        return l;
    }

    /** O item do Diário com os recordes dos peixes comuns. */
    static List<Component> comuns(RPGAtributos plugin, Player p) {
        List<Component> l = new ArrayList<>();
        for (Comum c : COMUNS) {
            double meu = recorde(p, c.id);
            String srv = plugin.recordes().textoPeixe(c.id);
            l.add(Component.text(c.nome + ": ", NamedTextColor.GRAY).append(Component.text(meu > 0 ? kg(meu) : "—", NamedTextColor.WHITE))
                    .append(Component.text(srv == null ? "" : "   (servidor: " + srv + ")", NamedTextColor.DARK_GRAY)));
        }
        return l;
    }
}
