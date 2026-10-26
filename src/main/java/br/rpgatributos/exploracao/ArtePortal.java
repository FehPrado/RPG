package br.rpgatributos.exploracao;

import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.forja.Traje;
import br.rpgatributos.masmorra.EquipamentoPortal.Peca;
import br.rpgatributos.masmorra.Rank;
import br.rpgatributos.masmorra.ManaCristal;
import br.rpgatributos.masmorra.CristalPortal;
import br.rpgatributos.masmorra.Inscricao;
import br.rpgatributos.masmorra.Monarca;
import br.rpgatributos.masmorra.Reliquias;
import br.rpgatributos.masmorra.EquipamentoPortal;
import br.rpgatributos.masmorra.MagiaOculta;
import br.rpgatributos.masmorra.EstiloPortal;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Visuais dos equipamentos dos portais, usando o mesmo formato dos Trajes e armas nomeadas. */
public final class ArtePortal {
    private ArtePortal() { }

    public record Paleta(int base, int luz, int sombra, int detalhe, int gema,
                         Traje.Estilo estilo) implements ArteTrajes.Paleta { }

    public static Paleta paleta(Rank r) {
        return switch (r) {
            case E -> new Paleta(0xFF68747D, 0xFFBECAD0, 0xFF303C48, 0xFF9CACB0, 0xFFCEE8EB, Traje.Estilo.PLACAS);
            case D -> new Paleta(0xFF285343, 0xFF70AE83, 0xFF142D2C, 0xFFA7C39A, 0xFF8AFFB7, Traje.Estilo.ESCAMAS);
            case C -> new Paleta(0xFF234D7F, 0xFF70BDDF, 0xFF10263F, 0xFFB2D5E0, 0xFF65ECFF, Traje.Estilo.PLACAS);
            case B -> new Paleta(0xFF4C326B, 0xFFA67DE0, 0xFF221532, 0xFFD5B6E9, 0xFFE09AFF, Traje.Estilo.ESCAMAS);
            case A -> new Paleta(0xFF6B2434, 0xFFDF6376, 0xFF2E131F, 0xFFE2B36A, 0xFFFF7777, Traje.Estilo.PLACAS);
            case S -> new Paleta(0xFF25283F, 0xFF697390, 0xFF101320, 0xFFF0C45B, 0xFFB3F7FF, Traje.Estilo.PLACAS);
        };
    }

    public static Paleta paleta(Chefe c) {
        return switch (c) {
            case GOLEM -> new Paleta(0xFF747E72, 0xFFC7D4AC, 0xFF303B37, 0xFFB29967, 0xFF7CFFD0, Traje.Estilo.PLACAS);
            case RAINHA -> new Paleta(0xFF493150, 0xFFA186B0, 0xFF211526, 0xFF86B955, 0xFFA5FF63, Traje.Estilo.ESCAMAS);
            case CHAMAS -> new Paleta(0xFF66332B, 0xFFE88A46, 0xFF281B23, 0xFFE6B263, 0xFFFFD04B, Traje.Estilo.PLACAS);
            case LICH -> new Paleta(0xFF555378, 0xFFC0BFD9, 0xFF211D38, 0xFFBBA2D0, 0xFF85F3E3, Traje.Estilo.TECIDO);
            case TEMPESTADE -> new Paleta(0xFF28748C, 0xFFBDEBFA, 0xFF15334C, 0xFF9FBED8, 0xFFB7FFFF, Traje.Estilo.PLACAS);
            case ARAUTO -> new Paleta(0xFF343044, 0xFF8A6EAA, 0xFF14101E, 0xFFBC799D, 0xFFFF7CAE, Traje.Estilo.PLACAS);
        };
    }

    public static String visual(Rank r, Peca p) { return "portal_cacador_" + r.id() + "_" + p.name().toLowerCase(java.util.Locale.ROOT); }
    public static String equipamento(Rank r) { return "portal_cacador_" + r.id(); }
    private static String sufixo(EstiloPortal e) { return e == EstiloPortal.CLASSICO ? "" : "_" + e.id(); }
    public static String visual(Rank r, Peca p, EstiloPortal e) { return visual(r, p) + sufixo(e); }
    public static String equipamento(Rank r, EstiloPortal e) { return equipamento(r) + sufixo(e); }
    public static String visual(Chefe c, EstiloPortal e) { return visual(c) + sufixo(e); }
    public static Paleta paleta(Paleta p, EstiloPortal e) {
        if (e == EstiloPortal.CLASSICO) return p;
        int base = ArteTrajes.misturar(p.base(), 0xFF000000 | e.cor, 0.55);
        return new Paleta(base, ArteTrajes.misturar(base, p.luz(), 0.65), ArteTrajes.escurecer(base, 0.45), p.detalhe(), p.gema(), e.armadura);
    }

    public static BufferedImage iconeArmadura(Paleta p, Traje.Peca peca, EstiloPortal e) {
        BufferedImage im = ArteTrajes.icone(p, peca);
        if (e == EstiloPortal.CLASSICO) return im;
        java.awt.Graphics2D g = im.createGraphics();
        g.setColor(new java.awt.Color(p.detalhe(), true));
        switch (e) {
            case VIGIA -> { g.fillRect(7, 3, 2, 10); g.fillRect(3, 6, 10, 2); }
            case PREDADOR -> {
                g.setComposite(java.awt.AlphaComposite.Clear);
                if (peca == Traje.Peca.CAPACETE) g.fillRect(4, 8, 8, 6);
                g.setComposite(java.awt.AlphaComposite.SrcOver);
                g.setColor(new java.awt.Color(p.detalhe(), true));
                g.drawLine(3, 3, 12, 12); g.drawLine(3, 5, 10, 12);
            }
            case RUNICO -> { g.drawPolygon(new int[]{8, 11, 8, 5}, new int[]{4, 7, 10, 7}, 4); g.fillRect(7, 1, 2, 2); }
            case ESPINHO -> {
                g.fillPolygon(new int[]{1, 4, 4}, new int[]{1, 4, 7}, 3);
                g.fillPolygon(new int[]{14, 11, 11}, new int[]{1, 4, 7}, 3);
                g.drawLine(4, 10, 8, 13); g.drawLine(11, 10, 8, 13);
            }
            case ABISSAL -> {
                g.fillPolygon(new int[]{3, 5, 8, 10, 12, 11, 4}, new int[]{1, 4, 1, 4, 1, 6, 6}, 7);
                g.setColor(new java.awt.Color(p.sombra(), true)); g.fillRect(6, 7, 4, 4);
            }
            default -> { }
        }
        g.setColor(new java.awt.Color(p.gema(), true)); g.fillRect(7, 6, 2, 2);
        g.dispose();
        return im;
    }

    public static BufferedImage corpo(Paleta p, EstiloPortal e, boolean pernas) {
        BufferedImage im = pernas ? ArteTrajes.pernas(p) : ArteTrajes.corpo(p);
        if (e == EstiloPortal.CLASSICO) return im;
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) {
            if ((im.getRGB(x, y) >>> 24) == 0) continue;
            boolean detalhe = switch (e) {
                case VIGIA -> x % 8 == 3 || y % 8 == 3;
                case PREDADOR -> (x + y * 2) % 9 == 0;
                case RUNICO -> x % 4 == 0 && y % 5 < 3;
                case ESPINHO -> (x + y) % 7 == 0 || (x - y) % 7 == 0;
                case ABISSAL -> y % 6 == 0 && x % 3 != 0;
                default -> false;
            };
            if (detalhe) im.setRGB(x, y, e == EstiloPortal.RUNICO ? p.gema() : p.detalhe());
        }
        return im;
    }

    public static String molde(Chefe c, EstiloPortal e) {
        if (e == EstiloPortal.CLASSICO || c == Chefe.LICH || c == Chefe.TEMPESTADE) return molde(c);
        if (c == Chefe.GOLEM) return switch (e) { case VIGIA, ESPINHO -> "martelo"; case PREDADOR, ABISSAL -> "clava"; default -> "maca"; };
        if (c == Chefe.CHAMAS) return e == EstiloPortal.PREDADOR || e == EstiloPortal.ABISSAL ? "machadinha" : e == EstiloPortal.RUNICO ? "machado" : "machado2";
        return e.espada;
    }
    public static String visual(Chefe c) { return "portal_chefe_" + c.name().toLowerCase(java.util.Locale.ROOT); }
    public static String visual(ManaCristal.Tamanho t) { return "portal_mana_" + t.name().toLowerCase(java.util.Locale.ROOT); }
    public static String visual(Inscricao i) { return "portal_inscricao_" + i.name().toLowerCase(java.util.Locale.ROOT); }
    public static String visual(Monarca m, int indice) { return "portal_reliquia_" + m.name().toLowerCase(java.util.Locale.ROOT) + "_" + indice; }
    public static String visualCristal(Rank r) { return "portal_cristal_" + r.id(); }

    public static String visualExtra(ItemStack i) {
        ManaCristal.Tamanho mana = ManaCristal.de(i);
        if (mana != null) return visual(mana);
        CristalPortal.Dados cristal = CristalPortal.ler(i);
        if (cristal != null) return visualCristal(cristal.rank());
        Inscricao inscricao = Inscricao.de(i);
        if (inscricao != null) return visual(inscricao);
        Reliquias.Reliquia reliquia = Reliquias.de(i);
        if (reliquia != null) return visual(reliquia.monarca(), reliquia.indice());
        if (MagiaOculta.eGrimorio(i)) return "portal_grimorio_oculto";
        return EquipamentoPortal.naoIdentificado(i) ? "portal_nao_identificado" : null;
    }

    /** Cristais facetados; os tamanhos de mana têm silhuetas crescentes. */
    public static BufferedImage cristal(int cor, int tamanho, boolean portal) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        int topo = 5 - tamanho, largura = 2 + tamanho;
        g.setColor(new java.awt.Color(ArteTrajes.escurecer(cor, 0.35), true));
        g.fillPolygon(new int[]{8, 8 + largura, 8 + largura, 8, 7 - largura, 7 - largura}, new int[]{topo, 5, 11, 15, 11, 5}, 6);
        g.setColor(new java.awt.Color(cor, true));
        g.fillPolygon(new int[]{8, 7 + largura, 8, 8 - largura}, new int[]{topo + 1, 6, 13, 10}, 4);
        g.setColor(new java.awt.Color(ArteTrajes.misturar(cor, 0xFFFFFFFF, 0.65), true));
        g.drawLine(8, topo + 2, 8 - largura + 1, 7);
        g.drawLine(8 - largura + 1, 7, 8, 12);
        if (portal) {
            g.setColor(new java.awt.Color(0xFF10172C, true));
            g.fillRect(6, 6, 5, 5);
            g.setColor(new java.awt.Color(0xFFE5FFFF, true));
            g.drawRect(7, 7, 2, 2);
        }
        g.dispose();
        return im;
    }

    /** Pergaminhos de borda escura e runa própria para cada inscrição. */
    public static BufferedImage inscricao(Inscricao i) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        g.setColor(new java.awt.Color(0xFF45384F, true)); g.fillRect(3, 1, 10, 14);
        g.setColor(new java.awt.Color(0xFFE7D9B5, true)); g.fillRect(4, 2, 8, 12);
        g.setColor(new java.awt.Color(0xFFBDA780, true)); g.fillRect(2, 2, 3, 2); g.fillRect(11, 12, 3, 2);
        g.setColor(new java.awt.Color(0xFF000000 | i.cor().value(), true));
        g.drawLine(5, 5, 10, 5); g.drawLine(6, 6, 9, 10);
        // Quatro traços rúnicos codificam o tipo, legíveis também quando a cor coincide.
        for (int bit = 0; bit < 4; bit++) if (((i.ordinal() + 1) & (1 << bit)) != 0) g.fillRect(5 + bit * 2, 8 + (bit % 2) * 2, 1, 3);
        g.dispose();
        return im;
    }

    public static BufferedImage reliquia(Monarca m, int indice) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = im.createGraphics();
        int cor = 0xFF000000 | m.cor.value();
        g.setColor(new java.awt.Color(ArteTrajes.escurecer(cor, 0.35), true));
        // Fragmento, lágrima/presa, asa/manto, coração e coroa.
        switch (indice) {
            case 0 -> g.fillPolygon(new int[]{3, 10, 13, 7, 5}, new int[]{3, 1, 7, 14, 9}, 5);
            case 1 -> g.fillPolygon(new int[]{7, 12, 10, 6, 3}, new int[]{1, 8, 13, 14, 10}, 5);
            case 2 -> g.fillPolygon(new int[]{2, 7, 13, 12, 9, 6, 3}, new int[]{2, 5, 3, 11, 14, 10, 12}, 7);
            case 3 -> g.fillPolygon(new int[]{2, 5, 8, 11, 14, 13, 8, 3}, new int[]{4, 2, 4, 2, 4, 9, 14, 9}, 8);
            case 4 -> g.fillPolygon(new int[]{1, 5, 8, 11, 14, 12, 3}, new int[]{3, 6, 1, 6, 3, 13, 13}, 7);
            default -> throw new IllegalArgumentException("Índice de relíquia inválido: " + indice);
        }
        g.dispose();
        BufferedImage mascara = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        mascara.setData(im.getData());
        for (int y = 1; y < 15; y++) for (int x = 1; x < 15; x++) {
            if ((mascara.getRGB(x, y) >>> 24) == 0) continue;
            boolean borda = (mascara.getRGB(x - 1, y) >>> 24) == 0 || (mascara.getRGB(x + 1, y) >>> 24) == 0
                    || (mascara.getRGB(x, y - 1) >>> 24) == 0 || (mascara.getRGB(x, y + 1) >>> 24) == 0;
            if (!borda) im.setRGB(x, y, x + y < 14 ? ArteTrajes.misturar(cor, 0xFFFFFFFF, 0.45) : cor);
        }
        g = im.createGraphics();
        g.setColor(new java.awt.Color(0xFFF2E8CF, true)); g.fillRect(7, 7, 2, 2);
        g.dispose();
        return im;
    }

    public static String molde(Chefe c) {
        return switch (c) {
            case GOLEM -> "martelo";
            case RAINHA -> "sabre";
            case CHAMAS -> "machado2";
            case LICH -> "arco";
            case TEMPESTADE -> "tridente";
            case ARAUTO -> "larga";
        };
    }

    private static Map<Character, Integer> cores(Paleta p) {
        return Map.of('o', ArteTrajes.escurecer(p.sombra(), 0.5), 'h', p.luz(), 'm', p.base(),
                'd', p.sombra(), 'g', p.detalhe(), 'w', p.sombra(), 'p', p.detalhe(), 'a', p.gema(), 's', p.luz());
    }

    public static BufferedImage iconeArma(Paleta p, String molde, int fase) {
        String[] linhas = molde.equals("arco") ? ArteNomeadas.arco(fase) : ArteNomeadas.molde(molde);
        Map<Character, Integer> cores = cores(p);
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, cores.getOrDefault(linhas[y].charAt(x), 0));
        return im;
    }

    public static BufferedImage iconeArma(Paleta p, String molde, int fase, EstiloPortal e) {
        BufferedImage im = iconeArma(p, molde, fase);
        if (e == EstiloPortal.CLASSICO || !molde.equals("arco")) return im;
        java.awt.Graphics2D g = im.createGraphics();
        g.setColor(new java.awt.Color(p.detalhe(), true));
        switch (e) {
            case VIGIA -> { g.fillRect(10, 1, 3, 2); g.fillRect(2, 11, 2, 3); }
            case PREDADOR -> { g.drawLine(10, 0, 14, 3); g.drawLine(0, 10, 3, 14); }
            case RUNICO -> { g.drawRect(10, 1, 3, 3); g.drawRect(1, 10, 3, 3); }
            case ESPINHO -> { g.fillPolygon(new int[]{9, 14, 12}, new int[]{0, 0, 4}, 3); g.fillPolygon(new int[]{0, 0, 4}, new int[]{9, 14, 12}, 3); }
            case ABISSAL -> { g.drawLine(8, 1, 14, 1); g.drawLine(1, 8, 1, 14); g.fillRect(6, 6, 2, 2); }
            default -> { }
        }
        g.dispose();
        return im;
    }

    private static byte[] texto(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static String modelo(String id) { return "{\"type\":\"minecraft:model\",\"model\":\"rpgatributos:item/" + id + "\"}"; }

    private static void item(Map<String, byte[]> f, String id, BufferedImage im, String pai, String molde, Paleta paleta) throws IOException {
        item(f, id, im, pai, molde, paleta, EstiloPortal.CLASSICO);
    }

    private static void item(Map<String, byte[]> f, String id, BufferedImage im, String pai, String molde, Paleta paleta, EstiloPortal estilo) throws IOException {
        f.put("assets/rpgatributos/textures/item/" + id + ".png", ArteTrajes.png(im));
        f.put("assets/rpgatributos/models/item/" + id + ".json", texto("{\"parent\":\"minecraft:item/" + pai + "\",\"textures\":{\"layer0\":\"rpgatributos:item/" + id + "\"}}"));
        String definicao = modelo(id);
        if (molde != null && !molde.equals("arco")) {
            f.put("assets/rpgatributos/models/item/" + id + "_3d.json", texto(ArteNomeadas3D.modeloPortal(molde, id, estilo.ordinal())));
            f.put("assets/rpgatributos/textures/item/" + id + "_cores.png", ArteTrajes.png(ArteNomeadas3D.cores(cores(paleta))));
            definicao = "{\"type\":\"minecraft:select\",\"property\":\"minecraft:display_context\",\"cases\":[{\"when\":[\"gui\",\"ground\",\"fixed\",\"on_shelf\"],\"model\":" + modelo(id) + "}],\"fallback\":" + modelo(id + "_3d") + "}";
        }
        f.put("assets/rpgatributos/items/" + id + ".json", texto("{\"model\":" + definicao + "}"));
    }

    public static Map<String, byte[]> arquivosDoPacote() throws IOException {
        Map<String, byte[]> f = new HashMap<>();
        for (Rank r : Rank.values()) for (EstiloPortal estilo : EstiloPortal.values()) {
            Paleta paleta = paleta(paleta(r), estilo);
            for (Peca p : Peca.values()) {
                Traje.Peca armadura = switch (p) {
                    case ESPADA -> null;
                    case ELMO -> Traje.Peca.CAPACETE;
                    case PEITORAL -> Traje.Peca.PEITORAL;
                    case GREVAS -> Traje.Peca.CALCA;
                    case BOTAS -> Traje.Peca.BOTAS;
                };
                item(f, visual(r, p, estilo), armadura == null ? iconeArma(paleta, estilo.espada, -1) : iconeArmadura(paleta, armadura, estilo),
                        armadura == null ? "handheld" : "generated", armadura == null ? estilo.espada : null, paleta, estilo);
            }
            String id = equipamento(r, estilo);
            f.put("assets/rpgatributos/equipment/" + id + ".json", texto("{\"layers\":{\"humanoid\":[{\"texture\":\"rpgatributos:" + id + "\"}],\"humanoid_leggings\":[{\"texture\":\"rpgatributos:" + id + "\"}]}}"));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid/" + id + ".png", ArteTrajes.png(corpo(paleta, estilo, false)));
            f.put("assets/rpgatributos/textures/entity/equipment/humanoid_leggings/" + id + ".png", ArteTrajes.png(corpo(paleta, estilo, true)));
        }
        for (Chefe c : Chefe.values()) for (EstiloPortal estilo : EstiloPortal.values()) {
            String id = visual(c, estilo), molde = molde(c, estilo);
            Paleta p = paleta(paleta(c), estilo);
            item(f, id, iconeArma(p, molde, -1, estilo), c == Chefe.LICH ? "bow" : "handheld", molde, p, estilo);
            if (c == Chefe.LICH) {
                for (int fase = 0; fase < 3; fase++) item(f, id + "_pulling_" + fase, iconeArma(p, molde, fase, estilo), "bow", null, p);
                f.put("assets/rpgatributos/items/" + id + ".json", texto("{\"model\":" + PacoteRecursos.arcoPuxando("rpgatributos:item/" + id) + "}"));
            }
        }
        for (ManaCristal.Tamanho t : ManaCristal.Tamanho.values()) item(f, visual(t), cristal(0xFF36CBDC, t.ordinal(), false), "generated", null, null);
        for (Rank r : Rank.values()) item(f, visualCristal(r), cristal(0xFF000000 | r.cor().value(), 3, true), "generated", null, null);
        for (Inscricao i : Inscricao.values()) item(f, visual(i), inscricao(i), "generated", null, null);
        for (Monarca m : Monarca.values()) for (int n = 0; n < 5; n++) item(f, visual(m, n), reliquia(m, n), "generated", null, null);
        BufferedImage misterio = cristal(0xFFB982D9, 3, true);
        java.awt.Graphics2D g = misterio.createGraphics();
        g.setColor(new java.awt.Color(0xFFFFFFFF, true));
        g.drawLine(7, 5, 9, 5); g.drawLine(9, 5, 9, 7); g.drawLine(9, 7, 8, 8); g.fillRect(8, 10, 1, 1); g.dispose();
        item(f, "portal_nao_identificado", misterio, "generated", null, null);
        BufferedImage livro = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        g = livro.createGraphics();
        g.setColor(new java.awt.Color(0xFF211B38, true)); g.fillRect(2, 1, 12, 14);
        g.setColor(new java.awt.Color(0xFFEEE5D0, true)); g.fillRect(4, 11, 9, 3);
        g.setColor(new java.awt.Color(0xFF624291, true)); g.fillRect(3, 2, 10, 10);
        g.setColor(new java.awt.Color(0xFFD5B877, true)); g.drawRect(4, 3, 7, 7);
        g.setColor(new java.awt.Color(0xFF8BF0FF, true)); g.drawRect(6, 5, 3, 3);
        g.setColor(new java.awt.Color(0xFF302242, true)); g.fillRect(2, 2, 2, 11);
        g.dispose();
        item(f, "portal_grimorio_oculto", livro, "generated", null, null);
        return f;
    }
}
