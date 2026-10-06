package br.rpgatributos.estruturas;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.exploracao.MapasDoTesouro;
import br.rpgatributos.vida.Album;
import br.rpgatributos.vida.Carta;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Os naufrágios do plugin: cada navio tem nome, capitão, diário e uma carga que conta de
 * onde ele vinha. Mesmas regras de {@link Projetos}: coordenadas locais, proa para +z; a
 * quilha fica um bloco enterrada no fundo do mar.
 */
final class Mar {

    /** O tipo de navio: madeira do casco, do convés, cor das velas e a carga. */
    enum Navio {
        MERCANTE("navio mercante", "spruce", "oak", Material.WHITE_WOOL,
                new String[]{"Andorinha do Sul", "Fortuna", "Boa Esperança", "Maré Mansa", "Estrela de Cobre", "Vento Bom"}),
        PIRATA("navio pirata", "dark_oak", "dark_oak", Material.BLACK_WOOL,
                new String[]{"Viúva Negra", "Dente de Tubarão", "Rainha Sem Coroa", "Corvo do Mar", "Última Risada", "Mão Leve"}),
        REAL("galeão real", "spruce", "spruce", Material.BLUE_WOOL,
                new String[]{"Real Coroa", "Soberano", "Leão Dourado", "Herdeira do Trono", "Glória do Reino", "Cetro de Ferro"}),
        EXPEDICAO("navio de expedição", "birch", "spruce", Material.LIGHT_GRAY_WOOL,
                new String[]{"Bússola Fiel", "Horizonte", "Descoberta", "Mapa Vivo", "Albatroz", "Sonda"});

        final String descricao, casco, conves;
        final Material vela;
        private final String[] nomes;

        Navio(String descricao, String casco, String conves, Material vela, String[] nomes) {
            this.descricao = descricao;
            this.casco = casco;
            this.conves = conves;
            this.vela = vela;
            this.nomes = nomes;
        }

        String nome(Random r) {
            return nomes[r.nextInt(nomes.length)];
        }
    }

    private static final String[] CAPITAES = {"Capitão Bartolomeu", "Capitã Isadora", "Capitão Vasco", "Capitã Leonor",
            "Capitão Teodoro", "Capitã Marina", "Capitão Rui Barbaruiva", "Capitã Celeste"};

    private final RPGAtributos plugin;
    private final Projetos base;

    Mar(RPGAtributos plugin, Projetos base) {
        this.plugin = plugin;
        this.base = base;
    }

    private static Material mat(String id) {
        return Material.valueOf(id.toUpperCase(Locale.ROOT));
    }

    // =====================================================================
    //  Naufrágio
    // =====================================================================
    //
    // O casco é desenhado "em pé" (u = comprimento, v = largura, h = altura) e depois
    // entortado: o navio aderna para estibordo (+x, mais alto = mais para o lado) e está
    // de proa enterrada na areia, com a popa erguida.

    /** Meia largura do casco na posição u (a proa afina, a popa é reta). */
    private static int largura(int u) {
        return u >= 7 ? 0 : u >= 5 ? 1 : 2;
    }

    /** Altura do convés: mais alto na proa e no castelo de popa. */
    private static int conves(int u) {
        return u >= 5 || u <= -4 ? 4 : 3;
    }

    /** Até onde vai o casco nesta altura (o fundo é mais estreito). */
    private static int limite(int h, int u) {
        int w = largura(u);
        return h == 0 ? w - 1 : w;
    }

    private static boolean dentro(int v, int h, int u) {
        return u >= -7 && u <= 7 && h >= 0 && h < conves(u) && Math.abs(v) <= limite(h, u);
    }

    /** Põe um bloco do casco já entortado. */
    private static Block casco(Obra o, int v, int h, int u, Object bloco) {
        int x = v + (int) Math.round(h * 0.35), y = h + (int) Math.round((7 - u) * 0.2);
        return bloco instanceof Material m ? o.por(x, y, u, m) : o.por(x, y, u, (String) bloco);
    }

    private static Block blocoCasco(Obra o, int v, int h, int u) {
        return o.bloco(v + (int) Math.round(h * 0.35), h + (int) Math.round((7 - u) * 0.2), u);
    }

    void naufragio(Obra o, Navio n, String nomeNavio, String pista) {
        Material tabua = mat(n.casco + "_planks"), piso = mat(n.conves + "_planks");
        Material faixa = mat("stripped_" + n.casco + "_log");
        String capitao = CAPITAES[o.r.nextInt(CAPITAES.length)];
        // Rombo no lado de baixo (estibordo), onde o navio bateu.
        int rz0 = -2 + o.r.nextInt(2), rz1 = rz0 + 2 + o.r.nextInt(2);

        // Areia por baixo da popa erguida (o navio não flutua).
        for (int u = -7; u <= 1; u++) {
            int lim = limite(0, u), alto = (int) Math.round((7 - u) * 0.2);
            for (int v = -lim; v <= lim; v++) {
                for (int y = 1; y < alto; y++) o.por(v, y, u, Material.SAND);
            }
        }
        // Casco: a borda é tábua (com uma faixa de tora), o meio é água.
        for (int u = -7; u <= 7; u++) {
            for (int h = 0; h < conves(u); h++) {
                int lim = limite(h, u);
                for (int v = -lim; v <= lim; v++) {
                    boolean borda = h == 0 || !dentro(v + 1, h, u) || !dentro(v - 1, h, u) || !dentro(v, h, u + 1) || !dentro(v, h, u - 1);
                    boolean rombo = v >= 1 && h >= 1 && h <= 2 && u >= rz0 && u <= rz1;
                    if (rombo || !borda) casco(o, v, h, u, Material.AIR);
                    else if (h >= 2 && o.chance(0.07)) casco(o, v, h, u, Material.AIR); // tábua podre que caiu
                    else casco(o, v, h, u, h == 2 ? faixa : tabua);
                }
            }
        }
        // Convés com buracos, o alçapão aberto e a amurada quebrada.
        for (int u = -7; u <= 7; u++) {
            int w = largura(u), h = conves(u);
            for (int v = -w; v <= w; v++) {
                boolean cabine = u <= -4;
                boolean alcapao = v == 0 && (u == 0 || u == -1);
                boolean rombo = v >= 1 && u >= rz0 && u <= rz1 && o.chance(0.6);
                if (alcapao || rombo || !cabine && Math.abs(v) < w && o.chance(0.15)) casco(o, v, h, u, Material.AIR);
                else casco(o, v, h, u, piso);
                if (!cabine && w > 0 && Math.abs(v) == w && o.chance(0.55)) casco(o, v, h + 1, u, n.casco + "_fence");
            }
        }
        // Proa: roda de proa e gurupés.
        casco(o, 0, 5, 7, n.casco + "_fence");
        casco(o, 0, 6, 7, n.casco + "_fence");

        // Castelo de popa com a cabine do capitão.
        for (int u = -7; u <= -4; u++) {
            for (int v = -2; v <= 2; v++) {
                boolean parede = Math.abs(v) == 2 || u == -7 || u == -4;
                for (int h = 5; h <= 7; h++) {
                    if (!parede) casco(o, v, h, u, Material.AIR);
                    else if (u == -4 && v == 0 && h <= 6) casco(o, v, h, u, Material.AIR); // porta arrombada
                    else if (h == 6 && (Math.abs(v) == 2 && (u == -5 || u == -6) || u == -7 && Math.abs(v) <= 1)) casco(o, v, h, u, "glass_pane");
                    else casco(o, v, h, u, tabua);
                }
                if (!o.chance(0.3)) casco(o, v, 8, u, n.casco + "_slab");
            }
        }
        if (n == Navio.REAL) {
            casco(o, -2, 7, -7, Material.GOLD_BLOCK);
            casco(o, 2, 7, -7, Material.GOLD_BLOCK);
        }
        casco(o, 0, 9, -7, n.casco + "_fence");
        casco(o, 0, 10, -7, switch (n) {
            case PIRATA -> "black_banner";
            case REAL -> "blue_banner";
            case EXPEDICAO -> "light_gray_banner";
            default -> "white_banner";
        });
        casco(o, 1, 5, -5, "lantern");
        casco(o, -1, 5, -6, "barrel[facing=up]");
        Block bau = casco(o, 0, 5, -6, "chest[facing=south]");
        List<ItemStack> cabine = new ArrayList<>();
        cabine.add(diario(n, nomeNavio, capitao, pista));
        cabine.addAll(cargaEspecial(o, n, bau));
        base.bau(bau, switch (n) {
            case PIRATA, REAL -> LootTables.SHIPWRECK_TREASURE;
            case EXPEDICAO -> LootTables.SHIPWRECK_MAP;
            default -> LootTables.SHIPWRECK_SUPPLY;
        }, o, cabine);

        // Porão: baú e barris da carga, areia que entrou pelo rombo.
        Block porao = casco(o, -1, 1, -3, "chest[facing=east]");
        base.bau(porao, LootTables.SHIPWRECK_SUPPLY, o, carga(o, n));
        Block barril = casco(o, -1, 1, 3, "barrel[facing=up]");
        base.bau(barril, LootTables.SHIPWRECK_SUPPLY, o, new ArrayList<>());
        casco(o, -1, 2, 0, "barrel[facing=east]");
        casco(o, 0, 1, -5, "barrel[facing=up]");
        for (int u = rz0; u <= rz1; u++) if (o.chance(0.6)) casco(o, 1, 1, u, Material.SAND);
        // Carga que caiu para fora pelo rombo.
        o.por(5, 1, rz1 + 1, "barrel[facing=west]");
        if (o.chance(0.6)) o.por(4, 1, rz0 - 1, "barrel[facing=north]");
        for (int u = rz0; u <= rz1; u++) if (o.chance(0.5)) o.por(4, 1, u, Material.SAND);

        // Mastros: o grande quebrado no meio e o de proa, e o pedaço que caiu ao lado.
        for (int h = 4; h <= 9; h++) casco(o, 0, h, 0, mat(n.casco + "_log"));
        for (int h = 4; h <= 6; h++) casco(o, 0, h, 4, mat(n.casco + "_log"));
        for (int z = -4; z <= 4; z++) o.por(-5, 1, z, n.casco + "_log[axis=z]");
        for (int x = -7; x <= -4; x++) if (x != -5) o.por(x, 1, 1, n.casco + "_log[axis=x]");
        for (int z = -3; z <= 4; z++) {
            for (int x = -7; x <= -6; x++) if (o.chance(0.5)) o.por(x, 1, z, n.vela);
            if (o.chance(0.35)) o.por(-4, 1, z, n.vela);
        }
        // Âncora caída perto da proa, com a corrente.
        o.por(3, 1, 7, Material.ANVIL);
        o.por(3, 2, 7, "iron_chain[axis=y]");

        // Vida no casco: pepinos-do-mar no convés, leques de coral e algas por perto.
        for (int i = 0; i < 3; i++) {
            int v = -1 + o.r.nextInt(3), u = 1 + o.r.nextInt(4);
            if (blocoCasco(o, v, conves(u), u).getType() == piso) casco(o, v, conves(u) + 1, u, "sea_pickle[pickles=" + (1 + o.r.nextInt(3)) + "]");
        }
        String[] leques = {"tube_coral_wall_fan", "brain_coral_wall_fan", "horn_coral_wall_fan", "fire_coral_wall_fan"};
        for (int u = -6; u <= 4; u++) {
            if (o.chance(0.3)) casco(o, -3, 2, u, leques[o.r.nextInt(leques.length)] + "[facing=west]");
        }
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (x > -4 && x < 6) continue;
                Block b = o.bloco(x, 1, z);
                if (b.getType() != Material.WATER || !o.bloco(x, 0, z).getType().isSolid()) continue;
                if (o.chance(0.06)) {
                    int topo = 1, alt = 2 + o.r.nextInt(4);
                    while (topo < alt && o.bloco(x, topo + 1, z).getType() == Material.WATER) topo++;
                    for (int y = 1; y < topo; y++) o.por(x, y, z, Material.KELP_PLANT);
                    o.por(x, topo, z, Material.KELP);
                } else if (o.chance(0.18)) {
                    o.por(x, 1, z, Material.SEAGRASS);
                }
            }
        }
    }

    private static ItemStack diario(Navio n, String navio, String capitao, String pista) {
        String[] paginas = switch (n) {
            case MERCANTE -> new String[]{
                    "Diário de bordo do " + navio + ".\n\nSaímos do porto com o porão cheio: lã, especiarias e vinho. " + capitao + " diz que esta viagem vai nos aposentar.",
                    "Dia 6.\n\nO mar mudou de cor. Os marujos juram ter visto luzes verdes debaixo da água, à noite. Rezei para Maris, por via das dúvidas.",
                    pista != null
                            ? "Dia 8.\n\nAs luzes de novo, " + pista + ". Os velhos falam de uma cidade que o mar engoliu. Eu só quero chegar em casa."
                            : "Dia 8.\n\nA tempestade chegou sem aviso. Amarrei este diário no baú. Se alguém o ler: a carga é sua. Cuide melhor dela do que nós.",
                    "Última página.\n\nO casco abriu do lado direito. A água entra mais rápido do que tiramos. Que Maris nos receba."};
            case PIRATA -> new String[]{
                    "Este diário é do " + navio + ". Quem ler sem ser da tripulação, que se afogue.\n\n— " + capitao,
                    "Saqueamos o galeão real na lua nova. Ouro, joias e um mapa que o capitão guarda como se fosse filho.",
                    pista != null
                            ? "O mapa leva " + pista + ", até torres que brilham no fundo do mar. Dizem que lá dorme um tesouro que nem Maris quis de volta."
                            : "O mapa leva a um tesouro enterrado numa ilha que só aparece na maré baixa. Eu vi. Eu juro que vi.",
                    "Dia 31.\n\nO mar não perdoa ladrão. Uma onda do tamanho de uma montanha. Se você achou isto, o mapa é seu. A maldição também."};
            case REAL -> new String[]{
                    "Por ordem da Coroa.\n\nO " + navio + " leva o tributo do reino para além-mar. " + capitao + " responde por ele. Que ninguém abra o cofre antes da chegada.",
                    "Dia 4.\n\nUma vela negra nos segue desde o amanhecer. Dobramos a guarda. Os soldados estão nervosos; eu também.",
                    "Dia 5.\n\nNão eram piratas. Era o mar. Ele subiu em volta do navio como uma mão."
                            + (pista != null ? " Ao fundo, " + pista + ", vi torres e a luz de um templo." : " Nunca vi nada igual."),
                    "Se este diário chegar à Coroa: o tributo está no baú da cabine. Cumprimos nosso dever até o fim."};
            case EXPEDICAO -> new String[]{
                    "Diário de " + capitao + ", a bordo do " + navio + ".\n\nObjetivo: mapear as águas do sul e achar a cidade de pedra-do-mar das lendas.",
                    "Dia 12.\n\nSondagens estranhas: o fundo está cheio de blocos lisos, cortados por mãos. Não é pedra natural. Estamos perto.",
                    pista != null
                            ? "Dia 13.\n\nAchei! A cidade fica " + pista + ". Marquei tudo na carta. Amanhã descemos com os sinos de mergulho."
                            : "Dia 13.\n\nAs sondagens sumiram. Talvez a cidade esteja mais longe do que pensávamos. Seguimos.",
                    "Dia 14.\n\nAlgo subiu do fundo e abraçou o casco. Guardei as cartas no baú. Alguém precisa terminar este mapa."};
        };
        ItemStack livro = new ItemStack(Material.WRITTEN_BOOK);
        livro.editMeta(BookMeta.class, m -> {
            m.title(Component.text("Diário do " + navio));
            m.author(Component.text(capitao));
            for (String p : paginas) m.addPages(Component.text(p));
        });
        return livro;
    }

    /** O que só esse tipo de navio levava (no baú da cabine). */
    private List<ItemStack> cargaEspecial(Obra o, Navio n, Block onde) {
        List<ItemStack> l = new ArrayList<>();
        switch (n) {
            case MERCANTE -> {
                l.add(new ItemStack(Material.EMERALD, 4 + o.r.nextInt(6)));
                if (o.chance(0.35)) l.add(Album.carta(Carta.values()[o.r.nextInt(Carta.values().length)], false));
            }
            case PIRATA -> {
                l.add(new ItemStack(Material.GOLD_INGOT, 3 + o.r.nextInt(6)));
                l.add(plugin.mapas().criar(o.chance(0.6) ? MapasDoTesouro.Tipo.RARO : MapasDoTesouro.Tipo.COMUM, onde.getLocation()));
                if (o.chance(0.3)) l.add(Raro.MAPA_RASGADO.criar(1));
            }
            case REAL -> {
                l.add(Gema.values()[o.r.nextInt(Gema.values().length)].criar(1 + (o.chance(0.25) ? 1 : 0), 1));
                l.add(new ItemStack(Material.EMERALD_BLOCK, 1 + o.r.nextInt(2)));
                if (o.chance(0.2)) l.add(Raro.PEDRA_DE_PROTECAO.criar(1));
            }
            case EXPEDICAO -> {
                l.add(new ItemStack(Material.COMPASS));
                l.add(plugin.mapas().criar(MapasDoTesouro.Tipo.COMUM, onde.getLocation()));
                l.add(Reagente.PEROLA_NEGRA.criar(1));
                if (o.chance(0.4)) l.add(plugin.arqueologia().reliquiaAleatoria());
            }
        }
        return l;
    }

    /** A carga do porão. */
    private static List<ItemStack> carga(Obra o, Navio n) {
        List<ItemStack> l = new ArrayList<>();
        switch (n) {
            case MERCANTE -> {
                Material[] las = {Material.WHITE_WOOL, Material.RED_WOOL, Material.YELLOW_WOOL, Material.CYAN_WOOL};
                l.add(new ItemStack(las[o.r.nextInt(las.length)], 6 + o.r.nextInt(10)));
                l.add(new ItemStack(Material.SUGAR, 4 + o.r.nextInt(8)));
                l.add(new ItemStack(Material.COCOA_BEANS, 3 + o.r.nextInt(6)));
            }
            case PIRATA -> {
                l.add(new ItemStack(Material.GUNPOWDER, 4 + o.r.nextInt(8)));
                l.add(new ItemStack(Material.TNT, 1 + o.r.nextInt(2)));
                l.add(new ItemStack(Material.IRON_SWORD));
            }
            case REAL -> {
                l.add(new ItemStack(Material.IRON_INGOT, 4 + o.r.nextInt(6)));
                l.add(new ItemStack(Material.GOLD_NUGGET, 8 + o.r.nextInt(12)));
            }
            case EXPEDICAO -> {
                l.add(new ItemStack(Material.PAPER, 6 + o.r.nextInt(8)));
                l.add(new ItemStack(Material.SPYGLASS));
            }
        }
        return l;
    }
}
