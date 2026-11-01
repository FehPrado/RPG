package br.rpgatributos.apoio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O Limiar (0.3.0): o livro de missões em forma de mapa, no estilo dos modpacks. Os capítulos ficam na
 * barra da esquerda; no mapa, cada missão é um círculo com o ícone, ligada às anteriores por linhas.
 * Arraste para mover, use a roda do mouse para o zoom e clique numa missão para ver como fazer no Guia.
 * Quem decide o que está feito é o plugin (ele manda o livro inteiro pelo canal de dados).
 */
final class TelaLimiar extends Screen {

    private static final int LATERAL = 132, CASA = 30, LINHA_CAP = 30;
    private static final int OURO = 0xFFF2C14E;
    /** O capítulo aberto da última vez (volta nele). */
    private static int capituloAtual;
    private static final Map<String, ItemStack> ICONES = new HashMap<>();
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId();

    private JsonObject livro;
    private double panX, panY, zoom = 1;
    private boolean arrastando, moveu;
    private double pressX, pressY;
    private JsonObject sobre;

    private TelaLimiar(JsonObject livro) {
        super(Component.literal("Limiar"));
        this.livro = livro;
        centralizar();
    }

    /** O plugin mandou o livro: abre (se pediu) ou atualiza o que está aberto; avisa a missão concluída. */
    static void receber(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof TelaLimiar t) t.livro = o;
        else if (o.has("abrir") && o.get("abrir").getAsBoolean()) mc.gui.setScreen(new TelaLimiar(o));
        String c = Estado.texto(o, "concluida", null);
        if (c != null) SystemToast.add(mc.gui.toastManager(), TOAST, Component.literal("✦ Missão do Limiar"), Component.literal(c));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- dados ----

    private JsonArray capitulos() {
        return livro != null && livro.has("capitulos") ? livro.getAsJsonArray("capitulos") : new JsonArray();
    }

    private JsonObject capitulo() {
        JsonArray caps = capitulos();
        if (caps.isEmpty()) return null;
        capituloAtual = Math.max(0, Math.min(caps.size() - 1, capituloAtual));
        return caps.get(capituloAtual).getAsJsonObject();
    }

    private List<JsonObject> missoes() {
        List<JsonObject> l = new ArrayList<>();
        JsonObject c = capitulo();
        if (c == null || !c.has("missoes")) return l;
        for (JsonElement e : c.getAsJsonArray("missoes")) l.add(e.getAsJsonObject());
        return l;
    }

    private static ItemStack icone(String id, String cmd) {
        return ICONES.computeIfAbsent(id + "|" + cmd, k -> {
            Item item = Items.BARRIER;
            try {
                Item achado = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
                if (achado != null && achado != Items.AIR) item = achado;
            } catch (RuntimeException ignorado) { }
            ItemStack s = new ItemStack(item);
            if (cmd != null) s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(cmd), List.of()));
            return s;
        });
    }

    /** Centra o mapa nas missões do capítulo. */
    private void centralizar() {
        List<JsonObject> ms = missoes();
        if (ms.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (JsonObject m : ms) {
            int x = Estado.inteiro(m, "x", 0), y = Estado.inteiro(m, "y", 0);
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        zoom = 1;
        panX = -(minX + maxX) / 2.0 * CASA;
        panY = -(minY + maxY) / 2.0 * CASA;
    }

    private double origemX() {
        return LATERAL + (this.width - LATERAL) / 2.0 + panX;
    }

    private double origemY() {
        return this.height / 2.0 + panY;
    }

    private static boolean grande(JsonObject m) {
        return m.has("grande") && m.get("grande").getAsBoolean();
    }

    private static int raio(JsonObject m) {
        return grande(m) ? 15 : 11;
    }

    // ---- desenho ----

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // O fundo é desenhado junto com o mapa.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int w = this.width, h = this.height;
        g.fill(0, 0, w, h, 0xFF1B1E21);
        mapa(g, mouseX, mouseY);
        lateral(g, mouseX, mouseY);
        if (sobre != null && !arrastando) dica(g, sobre, mouseX, mouseY);
    }

    private void mapa(GuiGraphicsExtractor g, int mx, int my) {
        int x0 = LATERAL, w = this.width, h = this.height;
        g.fill(x0, 0, w, h, 0xFF2E3336);
        double ox = origemX(), oy = origemY();
        // A grade de fundo (anda junto com o mapa).
        int passo = Math.max(8, (int) Math.round(CASA * zoom));
        int gx = (int) Math.floorMod((long) Math.round(ox), passo), gy = (int) Math.floorMod((long) Math.round(oy), passo);
        for (int x = x0 + gx; x < w; x += passo) g.verticalLine(x, 0, h, 0x14FFFFFF);
        for (int y = gy; y < h; y += passo) g.horizontalLine(x0, w, y, 0x14FFFFFF);
        g.enableScissor(x0, 0, w, h);
        g.pose().pushMatrix();
        g.pose().translate((float) ox, (float) oy);
        g.pose().scale((float) zoom, (float) zoom);
        List<JsonObject> ms = missoes();
        Map<String, JsonObject> porId = new HashMap<>();
        for (JsonObject m : ms) porId.put(Estado.texto(m, "id", ""), m);
        // As linhas primeiro (atrás dos círculos).
        for (JsonObject m : ms) {
            if (!m.has("antes")) continue;
            for (JsonElement e : m.getAsJsonArray("antes")) {
                JsonObject d = porId.get(e.getAsString());
                if (d == null) continue;
                boolean feitas = "feita".equals(Estado.texto(d, "estado", "")) && "feita".equals(Estado.texto(m, "estado", ""));
                boolean aberta = !"travada".equals(Estado.texto(m, "estado", ""));
                int cor = feitas ? 0xFFD8A848 : aberta ? 0xFFB89A9A : 0xFF4A4444;
                linha(g, Estado.inteiro(d, "x", 0) * CASA, Estado.inteiro(d, "y", 0) * CASA, Estado.inteiro(m, "x", 0) * CASA, Estado.inteiro(m, "y", 0) * CASA, cor);
            }
        }
        // Os círculos.
        sobre = null;
        double wx = (mx - ox) / zoom, wy = (my - oy) / zoom;
        boolean noMapa = mx >= x0;
        long agora = System.currentTimeMillis();
        for (JsonObject m : ms) {
            int cx = Estado.inteiro(m, "x", 0) * CASA, cy = Estado.inteiro(m, "y", 0) * CASA, r = raio(m);
            String estado = Estado.texto(m, "estado", "travada");
            boolean emCima = noMapa && (wx - cx) * (wx - cx) + (wy - cy) * (wy - cy) <= (r + 2) * (r + 2);
            if (emCima) sobre = m;
            int borda = switch (estado) {
                case "feita" -> OURO;
                case "aberta" -> (agora / 500) % 2 == 0 ? 0xFFF0F0F0 : 0xFFB0B0B0; // pisca de leve: dá para fazer agora
                default -> 0xFF555555;
            };
            if (emCima) borda = 0xFFFFFFFF;
            circulo(g, cx, cy, r + 2, borda);
            circulo(g, cx, cy, r, "feita".equals(estado) ? 0xFF3A3424 : "aberta".equals(estado) ? 0xFF2A2C2E : 0xFF1C1D1E);
            g.item(icone(Estado.texto(m, "icone", "minecraft:barrier"), Estado.texto(m, "cmd", null)), cx - 8, cy - 8);
            if ("travada".equals(estado)) circulo(g, cx, cy, r, 0xA0101010);
            if ("feita".equals(estado)) g.text(this.font, "✔", cx + r - 5, cy + r - 7, 0xFF7CE07C, true);
        }
        g.pose().popMatrix();
        g.disableScissor();
    }

    /** Um círculo cheio (linha a linha). */
    private static void circulo(GuiGraphicsExtractor g, int cx, int cy, int r, int cor) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt((double) r * r - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, cor);
        }
    }

    /** Uma linha grossa entre dois pontos (em qualquer direção). */
    private static void linha(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int cor) {
        double dx = x1 - x0, dy = y1 - y0;
        int n = (int) Math.max(1, Math.ceil(Math.hypot(dx, dy) / 2));
        for (int i = 0; i <= n; i++) {
            int x = (int) Math.round(x0 + dx * i / n), y = (int) Math.round(y0 + dy * i / n);
            g.fill(x - 1, y - 1, x + 2, y + 2, cor);
        }
    }

    private void lateral(GuiGraphicsExtractor g, int mx, int my) {
        int h = this.height;
        g.fill(0, 0, LATERAL, h, 0xFF15181A);
        g.verticalLine(LATERAL - 1, 0, h, 0x60F2C14E);
        // O título.
        g.pose().pushMatrix();
        g.pose().translate(LATERAL / 2f, 8);
        g.pose().scale(2, 2);
        g.centeredText(this.font, Estado.texto(livro, "titulo", "Limiar").toUpperCase(), 0, 0, OURO);
        g.pose().popMatrix();
        int feitas = Estado.inteiro(livro, "feitas", 0), total = Math.max(1, Estado.inteiro(livro, "total", 1));
        g.centeredText(this.font, feitas + " de " + total + " missões", LATERAL / 2, 30, 0xFFB0B0B0);
        Desenho.barra(g, 10, 42, LATERAL - 20, 4, feitas / (double) total, OURO);
        // Os capítulos.
        JsonArray caps = capitulos();
        int y = 56;
        for (int i = 0; i < caps.size(); i++) {
            JsonObject c = caps.get(i).getAsJsonObject();
            boolean atual = i == capituloAtual;
            boolean emCima = mx >= 0 && mx < LATERAL && my >= y && my < y + LINHA_CAP;
            if (atual || emCima) g.fill(2, y, LATERAL - 3, y + LINHA_CAP - 2, atual ? 0x50F2C14E : 0x25FFFFFF);
            g.item(icone(Estado.texto(c, "icone", "minecraft:book"), null), 6, y + 5);
            g.text(this.font, caber(Estado.texto(c, "nome", "?"), LATERAL - 32), 26, y + 4, atual ? OURO : 0xFFE0E0E0, false);
            int cf = Estado.inteiro(c, "feitas", 0), ct = Math.max(1, Estado.inteiro(c, "total", 1));
            g.text(this.font, cf + "/" + ct, 26, y + 15, cf >= ct ? 0xFF7CE07C : 0xFF909090, false);
            Desenho.barra(g, 50, y + 17, LATERAL - 60, 3, cf / (double) ct, cf >= ct ? 0xFF7CE07C : OURO);
            y += LINHA_CAP;
        }
        // Como usar.
        String[] ajuda = {"Arraste: mover", "Roda do mouse: zoom", "Clique: ver no Guia", "Brilhando: dá para fazer"};
        for (int i = 0; i < ajuda.length; i++) g.text(this.font, ajuda[i], 8, h - 12 - (ajuda.length - 1 - i) * 10, 0xFF707070, false);
    }

    private void dica(GuiGraphicsExtractor g, JsonObject m, int mx, int my) {
        List<FormattedCharSequence> l = new ArrayList<>();
        String estado = Estado.texto(m, "estado", "travada");
        l.add(Component.literal(Estado.texto(m, "nome", "?")).withColor(OURO & 0xFFFFFF).getVisualOrderText());
        l.addAll(this.font.split(Component.literal(Estado.texto(m, "desc", "")).withColor(0xC8C8C8), 220));
        String prog = Estado.texto(m, "prog", null);
        if (prog != null) l.add(Component.literal("Progresso: " + prog).withColor(0xFFFFFF).getVisualOrderText());
        String premio = Estado.texto(m, "premio", null);
        if (premio != null) l.add(Component.literal("Prêmio: " + premio).withColor(OURO & 0xFFFFFF).getVisualOrderText());
        l.add(switch (estado) {
            case "feita" -> Component.literal("✔ Feita").withColor(0x7CE07C).getVisualOrderText();
            case "aberta" -> Component.literal("◆ Dá para fazer agora · clique: ver como").withColor(0xFFE082).getVisualOrderText();
            default -> Component.literal("✖ Faça as anteriores primeiro").withColor(0x808080).getVisualOrderText();
        });
        g.setTooltipForNextFrame(l, mx, my);
    }

    private String caber(String s, int largura) {
        if (this.font.width(s) <= largura) return s;
        while (!s.isEmpty() && this.font.width(s + "…") > largura) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    // ---- mouse ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (x < LATERAL) {
            int i = (int) Math.floor((y - 56) / LINHA_CAP);
            if (y >= 56 && i >= 0 && i < capitulos().size() && i != capituloAtual) {
                capituloAtual = i;
                centralizar();
            }
            return true;
        }
        arrastando = true;
        moveu = false;
        pressX = x;
        pressY = y;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!arrastando) return false;
        panX += dx;
        panY += dy;
        if (Math.abs(event.x() - pressX) + Math.abs(event.y() - pressY) > 3) moveu = true;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean clique = arrastando && !moveu;
        arrastando = false;
        if (clique && sobre != null) {
            String topico = Estado.texto(sobre, "topico", null);
            if (topico != null && !topico.isEmpty()) {
                ClientPacketDistributor.sendToServer(new TeclaPayload("limiar:guia:" + topico));
                this.onClose();
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x < LATERAL || scrollY == 0) return false;
        double novo = Math.max(0.5, Math.min(2.5, zoom * (scrollY > 0 ? 1.15 : 1 / 1.15)));
        double ox = origemX(), oy = origemY();
        double wx = (x - ox) / zoom, wy = (y - oy) / zoom;
        zoom = novo;
        // O ponto debaixo do mouse fica onde está.
        panX += (x - wx * zoom) - origemX();
        panY += (y - wy * zoom) - origemY();
        return true;
    }
}
