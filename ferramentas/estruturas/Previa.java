import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Prévia isométrica de um molde (cubinhos coloridos pelo nome do bloco), mais um corte no meio. */
public class Previa {

    static Color cor(String s) {
        String n = s.replace("minecraft:", "").replaceAll("\\[.*", "");
        if (n.contains("leaves")) return n.contains("flowering") ? new Color(0x7FA35A) : new Color(0x3F7A2E);
        if (n.contains("wood") || n.contains("log")) return n.contains("dark") ? new Color(0x3E2A17) : new Color(0x6B4F2A);
        if (n.contains("planks") || n.contains("slab") && n.contains("oak") || n.contains("spruce")) return new Color(0xA07A48);
        if (n.contains("bone") || n.contains("calcite")) return new Color(0xE8E2CC);
        if (n.contains("moss")) return new Color(0x5C8A35);
        if (n.contains("deepslate") || n.contains("blackstone") || n.contains("tuff")) return new Color(0x4A4A52);
        if (n.contains("mossy")) return new Color(0x6E8060);
        if (n.contains("stone") || n.contains("andesite") || n.contains("cobble")) return new Color(0x8E8E8E);
        if (n.contains("sand")) return new Color(0xD9C98E);
        if (n.contains("gravel")) return new Color(0x857F7A);
        if (n.contains("dirt") || n.contains("roots")) return new Color(0x7A5A3A);
        if (n.contains("gold")) return new Color(0xF2C94C);
        if (n.contains("iron")) return new Color(0xD8D8D8);
        if (n.contains("lava") || n.contains("froglight") || n.contains("shroomlight") || n.contains("lantern")) return new Color(0xFF9A3C);
        if (n.contains("wool") || n.contains("carpet")) return n.contains("red") ? new Color(0xB03030) : new Color(0xEEEEEE);
        if (n.contains("obsidian")) return new Color(0x5A2A8A);
        if (n.contains("water")) return new Color(0x3F76E4);
        if (n.contains("chest") || n.contains("barrel") || n.contains("vault") || n.contains("spawner")) return new Color(0xFF3FD0);
        return new Color(0xB0A090);
    }

    static void salvar(Molde m, File f) throws IOException {
        double S = Math.max(3, Math.min(7, 520.0 / (m.sx + m.sz)));
        int w = (int) ((m.sx + m.sz) * S * 0.87) + 40, h = (int) ((m.sx + m.sz) * S * 0.5 + m.sy * S) + 40;
        BufferedImage img = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x1E2228));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        desenhar(g, m, S, 20 + m.sz * S * 0.87, h - 20 - (m.sx + m.sz) * S * 0.5, false);
        desenhar(g, m, S, w + 20 + m.sz * S * 0.87, h - 20 - (m.sx + m.sz) * S * 0.5, true);
        g.setColor(Color.WHITE);
        g.drawString(f.getName() + " (direita: corte no meio)", 10, 15);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
    }

    static void desenhar(Graphics2D g, Molde m, double S, double ox, double oy, boolean corte) {
        int zMax = corte ? m.sz / 2 : m.sz - 1;
        for (int y = 0; y < m.sy; y++)
            for (int soma = 0; soma <= m.sx + zMax; soma++)
                for (int x = Math.max(0, soma - zMax); x <= Math.min(m.sx - 1, soma); x++) {
                    int z = soma - x;
                    String s = m.get(x, y, z);
                    if (s == null || s.equals("minecraft:air")) continue;
                    boolean cima = vazio(m, x, y + 1, z), frente = corte && z == zMax || vazio(m, x, y, z + 1), lado = vazio(m, x + 1, y, z);
                    if (!cima && !frente && !lado) continue;
                    // projeção: px = (z - x) * cos30, py = (x + z) * sin30 - y
                    double px = ox + (z - x) * S * 0.87, py = oy + (x + z) * S * 0.5 - y * S;
                    Color c = cor(s);
                    if (cima) face(g, c, px, py, S, 0);
                    if (frente) face(g, c.darker(), px, py, S, 1);
                    if (lado) face(g, c.darker().darker(), px, py, S, 2);
                }
    }

    static boolean vazio(Molde m, int x, int y, int z) {
        String s = m.get(x, y, z);
        return s == null || s.equals("minecraft:air");
    }

    static void face(Graphics2D g, Color c, double px, double py, double S, int qual) {
        double a = S * 0.87, b = S * 0.5;
        Path2D p = new Path2D.Double();
        if (qual == 0) { p.moveTo(px, py - S); p.lineTo(px + a, py - S + b); p.lineTo(px, py - S + 2 * b); p.lineTo(px - a, py - S + b); }
        else if (qual == 1) { p.moveTo(px, py - S + 2 * b); p.lineTo(px + a, py - S + b); p.lineTo(px + a, py + b); p.lineTo(px, py + 2 * b); }
        else { p.moveTo(px, py - S + 2 * b); p.lineTo(px - a, py - S + b); p.lineTo(px - a, py + b); p.lineTo(px, py + 2 * b); }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }
}
