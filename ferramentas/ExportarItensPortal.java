import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.exploracao.ArtePortal;
import br.rpgatributos.masmorra.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Exporta os recursos para inspeção e uma prancha dos ícones sem precisar ligar o servidor. */
public class ExportarItensPortal {
    record Linha(String nome, List<String> ids) { }

    public static void main(String[] args) throws Exception {
        Path destino = Path.of(args.length > 0 ? args[0] : "target/itens-portal");
        Map<String, byte[]> arquivos = ArtePortal.arquivosDoPacote();
        for (var e : arquivos.entrySet()) {
            Path p = destino.resolve(e.getKey());
            Files.createDirectories(p.getParent());
            Files.write(p, e.getValue());
        }
        List<Linha> linhas = new ArrayList<>();
        for (Rank r : Rank.values()) linhas.add(new Linha("Caçador " + r.name(), java.util.Arrays.stream(EquipamentoPortal.Peca.values()).map(p -> ArtePortal.visual(r, p)).toList()));
        linhas.add(new Linha("Chefes", java.util.Arrays.stream(Chefe.values()).map(ArtePortal::visual).toList()));
        linhas.add(new Linha("Mana", java.util.Arrays.stream(ManaCristal.Tamanho.values()).map(ArtePortal::visual).toList()));
        linhas.add(new Linha("Portais", java.util.Arrays.stream(Rank.values()).map(ArtePortal::visualCristal).toList()));
        for (int inicio = 0; inicio < Inscricao.values().length; inicio += 7) {
            List<String> ids = new ArrayList<>();
            for (int n = inicio; n < Math.min(inicio + 7, Inscricao.values().length); n++) ids.add(ArtePortal.visual(Inscricao.values()[n]));
            linhas.add(new Linha("Inscrições", ids));
        }
        for (Monarca m : Monarca.values()) {
            List<String> ids = new ArrayList<>();
            for (int n = 0; n < 5; n++) ids.add(ArtePortal.visual(m, n));
            linhas.add(new Linha(m.nome.replace("Monarca ", ""), ids));
        }
        linhas.add(new Linha("A identificar", List.of("portal_nao_identificado")));
        linhas.add(new Linha("Grimório oculto", List.of("portal_grimorio_oculto")));
        if (args.length > 1 && args[1].equals("variantes")) {
            linhas.clear();
            for (EstiloPortal e : EstiloPortal.values()) {
                linhas.add(new Linha("Caçador S · " + e.nome, java.util.Arrays.stream(EquipamentoPortal.Peca.values()).map(p -> ArtePortal.visual(Rank.S, p, e)).toList()));
            }
            for (EstiloPortal e : EstiloPortal.values()) {
                linhas.add(new Linha("Chefes · " + e.nome, java.util.Arrays.stream(Chefe.values()).map(c -> ArtePortal.visual(c, e)).toList()));
            }
        }
        BufferedImage prancha = new BufferedImage(1060, 120 + linhas.size() * 94, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prancha.createGraphics();
        g.setColor(new Color(0x101522)); g.fillRect(0, 0, prancha.getWidth(), prancha.getHeight());
        g.setColor(new Color(0xE8C77B)); g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Equipamentos e tesouros dos portais", 30, 42);
        g.setColor(new Color(0xAAB8CE)); g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("180 peças de Caçador · 36 armas de chefe · seis famílias de desenhos", 30, 73);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int n = 0; n < linhas.size(); n++) {
            Linha linha = linhas.get(n);
            int y = 108 + n * 94;
            g.setColor(new Color(n % 2 == 0 ? 0x171F30 : 0x131B2A)); g.fillRoundRect(18, y - 8, 1024, 88, 10, 10);
            g.setColor(new Color(0xCFD8E6)); g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(linha.nome(), 32, y + 35);
            for (int c = 0; c < linha.ids().size(); c++) {
                byte[] png = arquivos.get("assets/rpgatributos/textures/item/" + linha.ids().get(c) + ".png");
                BufferedImage icone = ImageIO.read(new ByteArrayInputStream(png));
                g.drawImage(icone, 265 + c * 105, y, 64, 64, null);
            }
        }
        g.dispose();
        ImageIO.write(prancha, "png", destino.resolve("previa.png").toFile());
        long itens = arquivos.keySet().stream().filter(s -> s.contains("/items/")).count();
        System.out.println(itens + " definições de itens; " + arquivos.size() + " arquivos. Prévia: " + destino.resolve("previa.png"));
    }
}
