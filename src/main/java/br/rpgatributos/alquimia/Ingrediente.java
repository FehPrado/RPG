package br.rpgatributos.alquimia;

import br.rpgatributos.aventura.Raro;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.pesca.PeixeRaro;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Um ingrediente de receita alquímica: um material comum, um componente, uma gema,
 * um peixe raro, um material raro ou uma variedade da Agricultura.
 */
public record Ingrediente(Tipo tipo, Material material, Reagente reagente, Gema gema, int grau,
                          PeixeRaro peixe, Raro raro, Variedade variedade, int qtd) {

    public enum Tipo { MATERIAL, PEIXE_COMUM, REAGENTE, GEMA, PEIXE, RARO, VARIEDADE }

    public static Ingrediente m(Material m, int qtd) { return new Ingrediente(Tipo.MATERIAL, m, null, null, 0, null, null, null, qtd); }
    public static Ingrediente peixeComum(int qtd) { return new Ingrediente(Tipo.PEIXE_COMUM, null, null, null, 0, null, null, null, qtd); }
    public static Ingrediente r(Reagente r, int qtd) { return new Ingrediente(Tipo.REAGENTE, null, r, null, 0, null, null, null, qtd); }
    public static Ingrediente g(Gema g, int grau, int qtd) { return new Ingrediente(Tipo.GEMA, null, null, g, grau, null, null, null, qtd); }
    public static Ingrediente p(PeixeRaro p, int qtd) { return new Ingrediente(Tipo.PEIXE, null, null, null, 0, p, null, null, qtd); }
    public static Ingrediente raro(Raro r, int qtd) { return new Ingrediente(Tipo.RARO, null, null, null, 0, null, r, null, qtd); }
    public static Ingrediente v(Variedade v, int qtd) { return new Ingrediente(Tipo.VARIEDADE, null, null, null, 0, null, null, v, qtd); }

    /** Item comum: sem nenhuma marca do plugin (a qualidade ★ das colheitas e peixes não conta). */
    public static boolean simples(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        for (NamespacedKey k : s.getPersistentDataContainer().getKeys()) {
            if (k.getNamespace().equals("rpgatributos") && !k.equals(Qualidade.CHAVE)) return false;
        }
        return true;
    }

    public boolean combina(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        return switch (tipo) {
            case MATERIAL -> s.getType() == material && simples(s);
            case PEIXE_COMUM -> (s.getType() == Material.COD || s.getType() == Material.SALMON) && simples(s);
            case REAGENTE -> Reagente.de(s) == reagente;
            case GEMA -> Gema.de(s) == gema && Gema.grau(s) == grau;
            case PEIXE -> PeixeRaro.de(s) == peixe;
            case RARO -> Raro.de(s) == raro;
            case VARIEDADE -> Variedade.de(s) == variedade && !Variedade.ehSemente(s);
        };
    }

    /** Conta para melhorar a qualidade do elixir? */
    public boolean temQualidade() {
        return tipo == Tipo.PEIXE || tipo == Tipo.PEIXE_COMUM || tipo == Tipo.VARIEDADE
                || (tipo == Tipo.MATERIAL && material.isEdible());
    }

    public Component nome() {
        return switch (tipo) {
            case MATERIAL -> Component.translatable(material.translationKey());
            case PEIXE_COMUM -> Component.text("Bacalhau ou salmão");
            case REAGENTE -> Component.text(reagente.nome(), reagente.cor());
            case GEMA -> Component.text(gema.nome(grau), gema.cor());
            case PEIXE -> Component.text(peixe.nome(), peixe.cor());
            case RARO -> Component.text(raro.nome(), raro.cor());
            case VARIEDADE -> Component.text(variedade.nome(), variedade.cor());
        };
    }

    public int contar(PlayerInventory inv) {
        int n = 0;
        for (ItemStack s : inv.getStorageContents()) if (combina(s)) n += s.getAmount();
        return n;
    }

    /** Tira do inventário (primeiro os de melhor qualidade) e diz a qualidade de cada unidade tirada. */
    public List<Qualidade> tirar(PlayerInventory inv) {
        ItemStack[] c = inv.getStorageContents();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < c.length; i++) if (combina(c[i])) indices.add(i);
        indices.sort(Comparator.comparingInt((Integer i) -> Qualidade.de(c[i]).ordinal()).reversed());
        List<Qualidade> qualidades = new ArrayList<>();
        int falta = qtd;
        for (int i : indices) {
            if (falta <= 0) break;
            int tira = Math.min(falta, c[i].getAmount());
            Qualidade q = Qualidade.de(c[i]);
            for (int k = 0; k < tira; k++) qualidades.add(q);
            c[i].setAmount(c[i].getAmount() - tira);
            if (c[i].getAmount() <= 0) c[i] = null;
            falta -= tira;
        }
        inv.setStorageContents(c);
        return qualidades;
    }
}
