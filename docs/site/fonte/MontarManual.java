import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta o Manual completo (docs/site/index.html) a partir de:
 *   - docs/jogadores.html (o Guia: regras de tudo, índice e busca);
 *   - docs/site/fonte/receitas.json (as receitas de bancada, lidas do plugin por um plugin de teste);
 *   - docs/site/fonte/pt_br.json (os nomes dos itens do jogo em português);
 *   - docs/site/fonte/colecao.json (os grupos e itens da coleção).
 *
 * Uso: java -cp gson.jar MontarManual.java <pasta docs> <versão> [pasta do pacote descompactado]
 * Com a pasta do pacote, copia para img/itens as texturas de itens que ainda faltam.
 */
public class MontarManual {

    static Path docs, site, fonte;
    static JsonObject lang;
    static Path pacote;

    public static void main(String[] a) throws IOException {
        docs = Path.of(a[0]);
        String versao = a[1];
        pacote = a.length > 2 ? Path.of(a[2]) : null;
        site = docs.resolve("site");
        fonte = site.resolve("fonte");
        lang = JsonParser.parseString(Files.readString(fonte.resolve("pt_br.json"))).getAsJsonObject();
        String guia = Files.readString(docs.resolve("jogadores.html"));

        String css = entre(guia, "<style>", "</style>");
        String hero = entre(guia, "<div class=\"hero\">", "\n<section").trim();
        hero = "<div class=\"hero\">" + hero;
        hero = hero.replaceAll("<p class=\"rotulo\">[^<]*</p>", "<p class=\"rotulo\">Manual do jogador · versão " + versao + "</p>")
                .replace("site/img/", "img/");

        // As seções do Guia, por id.
        Map<String, String> secoes = new LinkedHashMap<>();
        Matcher m = Pattern.compile("<section class=\"secao\" id=\"([a-z0-9_-]+)\">(.*?)</section>", Pattern.DOTALL).matcher(guia);
        while (m.find()) secoes.put(m.group(1), m.group(0).replace("site/img/", "img/"));
        // Os rótulos do índice do Guia.
        Map<String, String> rotulos = new LinkedHashMap<>();
        Matcher n = Pattern.compile("<a href=\"#([a-z0-9_-]+)\">([^<]*)</a>").matcher(entre(guia, "<nav class=\"indice\" id=\"indice\">", "</nav>"));
        while (n.find()) rotulos.put(n.group(1), n.group(2));

        secoes.put("receitas", receitas());
        rotulos.put("receitas", "🛠 Receitas da bancada");
        secoes.put("colecao", colecao());
        rotulos.put("colecao", "▦ Coleção de itens");

        // A ordem de quem está jogando: começar, fabricar, lutar, explorar, conviver.
        String[][] grupos = {
                {"Começar", "inicio", "atributos", "comandos"},
                {"Fabricar", "receitas", "rituais", "forja", "alquimia", "arcano", "curtume", "fazenda", "campo", "vida", "detalhes"},
                {"Combate", "combos", "mobilidade", "classes", "lendarias", "lendas", "visual", "progressao"},
                {"Explorar", "descobertas", "exploracao", "ceu", "nether", "mundovivo", "perigo", "altar", "masmorras", "portais", "torre"},
                {"Conviver", "pesca", "domador", "fe", "party", "colonia", "reinos"},
                {"Referência", "colecao"}};
        StringBuilder nav = new StringBuilder("<nav class=\"indice\" id=\"indice\">\n");
        StringBuilder corpo = new StringBuilder();
        Set<String> usadas = new LinkedHashSet<>();
        for (String[] g : grupos) {
            nav.append("  <div class=\"grupo\">").append(g[0]).append("</div>\n");
            for (int i = 1; i < g.length; i++) {
                String id = g[i];
                if (!secoes.containsKey(id)) continue;
                usadas.add(id);
                nav.append("  <a href=\"#").append(id).append("\">").append(rotulos.getOrDefault(id, id)).append("</a>\n");
                corpo.append(secoes.get(id)).append("\n\n");
            }
        }
        // Alguma seção nova do Guia que ainda não está na ordem acima entra no fim (nada se perde).
        for (String id : secoes.keySet()) {
            if (usadas.contains(id)) continue;
            nav.append("  <a href=\"#").append(id).append("\">").append(rotulos.getOrDefault(id, id)).append("</a>\n");
            corpo.append(secoes.get(id)).append("\n\n");
        }
        nav.append("</nav>\n");

        String topo = "<header class=\"topo\"><div class=\"topo-in\">\n"
                + "  <a class=\"marca\" href=\"#inicio\"><b>RPG<span>Atributos</span></b><small>Manual do jogador · " + versao + "</small></a>\n"
                + "  <div class=\"busca\"><input id=\"busca\" type=\"search\" placeholder=\"Buscar (ex.: adaga, cantil, /pets)…\" aria-label=\"Buscar no manual\">"
                + "<button class=\"botao\" id=\"tema\" title=\"Tema claro/escuro\" aria-label=\"Trocar tema\">◐</button></div>\n"
                + "</div></header>\n";
        String script = entre(guia, "<script>", "</script>");
        String html = "<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">\n"
                + "<title>Manual do RPGAtributos</title>\n"
                + "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n"
                + "<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@500;700&family=Lexend:wght@300;400;600&family=JetBrains+Mono:wght@500&display=swap\">\n"
                + "<style>" + css.replace("/* Layout: o mesmo almanaque do Manual", "/* Layout: almanaque completo do jogador") + CSS_EXTRA + "</style>\n</head>\n<body>\n"
                + topo + "<div class=\"layout\">\n" + nav + "\n<main id=\"conteudo\">\n" + hero + "\n\n" + corpo
                + "<p id=\"vazio\" hidden>Nada encontrado. Tente outra palavra.</p>\n</main>\n</div>\n"
                + "<footer>RPGAtributos " + versao + " · Manual do jogador · Dúvidas? Leia o <code>/guia</code> no jogo ou pergunte a um admin.</footer>\n"
                + "<div id=\"toast\" role=\"status\" aria-live=\"polite\"></div>\n"
                + "<script>" + script + "</script>\n</body>\n</html>\n";
        Files.writeString(site.resolve("index.html"), html, StandardCharsets.UTF_8);
        System.out.println("Manual montado: " + secoes.size() + " seções, " + html.length() / 1024 + " KB");
    }

    // =====================================================================
    //  Receitas da bancada
    // =====================================================================

    static final Pattern ARMA = Pattern.compile("^(adaga|faca_arremesso|machadinha|foice|bastao|chicote)_(wooden|stone|copper|iron|golden|diamond)$");

    static String categoria(String id) {
        if (ARMA.matcher(id).matches()) return "Arsenal";
        if (id.matches("montante|bumerangue|leques|arpao|mangual|garras|clava|lanca_basalto")) return "Armas dos minérios";
        if (id.startsWith("flecha_") || id.startsWith("frasco_")) return "Flechas e frascos";
        if (id.startsWith("mitrilo_") || id.equals("cinzel") || id.equals("bateia")) return "Mineração";
        if (id.equals("adubo_rico") || id.equals("ferradura") || id.equals("livro_criador")) return "Campo e animais";
        if (id.equals("cajado_arcano")) return "Magia";
        return "Utilidade";
    }

    static final String[] ORDEM_CAT = {"Arsenal", "Armas dos minérios", "Utilidade", "Flechas e frascos", "Mineração", "Campo e animais", "Magia"};

    static String receitas() throws IOException {
        JsonArray rs = JsonParser.parseString(Files.readString(fonte.resolve("receitas.json"))).getAsJsonArray();
        Map<String, List<JsonObject>> porCat = new LinkedHashMap<>();
        for (String c : ORDEM_CAT) porCat.put(c, new ArrayList<>());
        Set<String> armasFeitas = new LinkedHashSet<>();
        for (JsonElement e : rs) {
            JsonObject r = e.getAsJsonObject();
            String id = r.get("id").getAsString();
            if (id.equals("mitrilo_alto_forno")) continue; // vai junto no cartão da fornalha
            Matcher am = ARMA.matcher(id);
            if (am.matches()) {
                // Uma carta por arma (o material da receita decide o material da arma).
                if (!am.group(2).equals("iron") || !armasFeitas.add(am.group(1))) continue;
            }
            porCat.computeIfAbsent(categoria(id), k -> new ArrayList<>()).add(r);
        }
        StringBuilder sb = new StringBuilder("<section class=\"secao\" id=\"receitas\">\n  <h2><span class=\"ico\">🛠</span>Receitas da bancada</h2>\n");
        sb.append("  <p class=\"intro\">Tudo o que o plugin acrescenta à bancada, lido direto do plugin. As letras na grade dizem onde vai cada ingrediente; "
                + "em receitas sem forma, a posição tanto faz. Use a busca do topo com o nome do item ou de um ingrediente.</p>\n  <div class=\"grade\">\n");
        sb.append(ondeSeFaz());
        for (Map.Entry<String, List<JsonObject>> en : porCat.entrySet()) {
            for (JsonObject r : en.getValue()) sb.append(cartaReceita(r, en.getKey()));
        }
        sb.append("  </div>\n</section>");
        return sb.toString();
    }

    static String ondeSeFaz() {
        String[][] l = {
                {"Bancada (esta seção)", "Armas do arsenal, artefatos, flechas, frascos, ferramentas de mineração, Livro do Criador", "#receitas"},
                {"Estações de ritual", "Jogue os itens (tecla Q) em cima do bloco certo: Forja, Cozinha, Altar, Comedouro, Estábulo e mais 25", "#rituais"},
                {"Forja do Ferreiro", "Armas e armaduras com raridade, refino, gemas e Netherite", "#forja"},
                {"Bancada Alquímica", "Elixires, componentes, gemas e acessórios", "#alquimia"},
                {"Infusor e grimório", "Magias (essências + forma), pergaminhos e o cajado", "#arcano"},
                {"Curtume, Tear e Ateliê", "Couros, tecidos e os conjuntos de armadura leve", "#curtume"},
                {"Cozinha", "Os pratos (inclusive os de monstro) e o que cada um faz", "#fazenda"},
                {"Barril, Colmeia, Canteiro, Tacho", "Bebidas, méis, ervas, artesanato e trufas", "#vida"},
                {"Mesa Rúnica e Círculo", "Runas e transmutação", "#fe"}};
        StringBuilder sb = new StringBuilder("    <div class=\"card largo\"><h3>Onde se faz cada coisa</h3>\n"
                + "      <div class=\"tabela\"><table><thead><tr><th>Onde</th><th>O que sai</th></tr></thead><tbody>\n");
        for (String[] x : l) sb.append("        <tr><td><a href=\"").append(x[2]).append("\">").append(x[0]).append("</a></td><td>").append(x[1]).append("</td></tr>\n");
        return sb.append("      </tbody></table></div>\n    </div>\n").toString();
    }

    static String nomeMat(String key) {
        for (String p : new String[]{"item.minecraft.", "block.minecraft."}) {
            JsonElement e = lang.get(p + key);
            if (e != null) return e.getAsString();
        }
        String s = key.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String nomeResultado(JsonObject it) {
        if (it.has("nome")) {
            String n = it.get("nome").getAsString();
            if (n.startsWith("@")) {
                JsonElement e = lang.get(n.substring(1));
                return e == null ? nomeMat(it.get("mat").getAsString()) : e.getAsString();
            }
            return n;
        }
        return nomeMat(it.get("mat").getAsString());
    }

    /** Nome de uma escolha de ingrediente ("Tábuas (qualquer madeira)", "Pedregulho ou Pedra-negra"...). */
    static String nomeEscolha(JsonObject c) {
        if (c.has("itens")) {
            List<String> l = new ArrayList<>();
            for (JsonElement e : c.getAsJsonArray("itens")) l.add(nomeResultado(e.getAsJsonObject()));
            return String.join(" ou ", l);
        }
        JsonArray mats = c.getAsJsonArray("mats");
        boolean tabuas = mats.size() >= 6;
        for (JsonElement e : mats) tabuas &= e.getAsString().endsWith("_planks");
        if (tabuas) return "Tábuas (qualquer madeira)";
        List<String> l = new ArrayList<>();
        for (JsonElement e : mats) l.add(nomeMat(e.getAsString()));
        return String.join(" ou ", l);
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String icone(JsonObject res) {
        String v = res.has("visual") ? res.get("visual").getAsString() : null;
        if (v != null) {
            Path img = site.resolve("img/itens/" + v + ".png");
            if (!Files.exists(img) && pacote != null) {
                Path tex = pacote.resolve("assets/rpgatributos/textures/item/" + v + ".png");
                try {
                    if (Files.exists(tex)) Files.copy(tex, img, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignorado) {
                    // fica sem imagem
                }
            }
            if (Files.exists(img)) return "<img class=\"pixel\" src=\"img/itens/" + v + ".png\" alt=\"\" width=\"48\" height=\"48\">";
        }
        String n = nomeResultado(res);
        return "<span class=\"sem-icone\" aria-hidden=\"true\">" + esc(n.substring(0, 1)) + "</span>";
    }

    static String cartaReceita(JsonObject r, String cat) {
        String id = r.get("id").getAsString();
        JsonObject res = r.getAsJsonObject("resultado");
        boolean arma = ARMA.matcher(id).matches();
        String nome = nomeResultado(res);
        if (arma) nome = nome.replaceAll(" de (Ferro|Madeira|Pedra|Cobre|Ouro|Diamante)$", "");
        int qtd = res.get("qtd").getAsInt();
        String desc = res.has("lore") && !res.getAsJsonArray("lore").isEmpty() ? res.getAsJsonArray("lore").get(0).getAsString() : "";
        if (id.equals("mitrilo_aprimorar")) {
            nome = "Peça ✦ Mitrilo";
            desc = "Uma peça de Netherite (forjada ou não) vira ✦ Mitrilo: 50% mais durabilidade e um bônus extra.";
            qtd = 1;
        }
        StringBuilder sb = new StringBuilder("    <div class=\"card receita\" data-cat=\"" + esc(cat) + "\">\n");
        sb.append("      <div class=\"rec-topo\">").append(icone(res)).append("<div><p class=\"rec-cat\">").append(esc(cat)).append("</p><h3>")
                .append(esc(nome)).append(qtd > 1 ? " <span class=\"muted\">×" + qtd + "</span>" : "").append("</h3></div></div>\n");
        if (!desc.isEmpty()) sb.append("      <p class=\"rec-desc\">").append(esc(desc)).append("</p>\n");
        String tipo = r.get("tipo").getAsString();
        switch (tipo) {
            case "bancada" -> {
                JsonArray forma = r.getAsJsonArray("forma");
                JsonObject chaves = r.getAsJsonObject("chaves");
                List<String> ordem = new ArrayList<>(chaves.keySet());
                sb.append("      <div class=\"rec-corpo\"><div class=\"bancada\" role=\"img\" aria-label=\"Grade da bancada\">");
                for (int y = 0; y < 3; y++) {
                    String linha = y < forma.size() ? forma.get(y).getAsString() : "";
                    for (int x = 0; x < 3; x++) {
                        char ch = x < linha.length() ? linha.charAt(x) : ' ';
                        int k = ordem.indexOf(String.valueOf(ch));
                        if (ch == ' ' || k < 0) sb.append("<span></span>");
                        else sb.append("<span class=\"c").append(k % 5).append("\">").append(ch).append("</span>");
                    }
                }
                sb.append("</div><ul class=\"legenda\">");
                for (int k = 0; k < ordem.size(); k++) {
                    String c = ordem.get(k);
                    String nomeIng = nomeEscolha(chaves.getAsJsonObject(c));
                    if (arma && c.equals("M")) nomeIng = "O material: tábuas, pedregulho, cobre, ferro, ouro ou diamante (a arma sai desse material; Netherite na mesa de ferraria)";
                    sb.append("<li><span class=\"c").append(k % 5).append("\">").append(esc(c)).append("</span>").append(esc(nomeIng)).append("</li>");
                }
                sb.append("</ul></div>\n");
            }
            case "sem_forma" -> {
                Map<String, Integer> cont = new LinkedHashMap<>();
                for (JsonElement e : r.getAsJsonArray("ingredientes")) cont.merge(nomeEscolha(e.getAsJsonObject()), 1, Integer::sum);
                sb.append("      <p class=\"rec-modo\">Sem forma: em qualquer lugar da grade</p><ul class=\"lista-ing\">");
                cont.forEach((k, v) -> sb.append("<li>").append(v > 1 ? v + "× " : "").append(esc(k)).append("</li>"));
                sb.append("</ul>\n");
            }
            case "fornalha", "alto_forno" -> sb.append("      <p class=\"rec-modo\">").append(tipo.equals("fornalha") ? "Fornalha (ou alto-forno, mais rápido)" : "Alto-forno").append("</p><ul class=\"lista-ing\"><li>")
                    .append(esc(nomeEscolha(r.getAsJsonObject("entrada")))).append(" → ").append(esc(nome)).append("</li></ul>\n");
            case "ferraria" -> sb.append("      <p class=\"rec-modo\">Mesa de ferraria</p><ul class=\"lista-ing\"><li>").append(esc(nomeEscolha(r.getAsJsonObject("molde"))))
                    .append("</li><li>").append(esc(nomeEscolha(r.getAsJsonObject("base")))).append("</li><li>").append(esc(nomeEscolha(r.getAsJsonObject("adicao"))))
                    .append("</li></ul>\n");
            default -> sb.append("      <p class=\"rec-modo\">").append(esc(tipo)).append("</p>\n");
        }
        return sb.append("    </div>\n").toString();
    }

    // =====================================================================
    //  Coleção de itens (as texturas do pacote)
    // =====================================================================

    static String colecao() throws IOException {
        JsonObject c = JsonParser.parseString(Files.readString(fonte.resolve("colecao.json"))).getAsJsonObject();
        StringBuilder sb = new StringBuilder("<section class=\"secao\" id=\"colecao\">\n  <h2><span class=\"ico\">▦</span>Coleção de itens</h2>\n"
                + "  <p class=\"intro\">Os itens do plugin com o visual do pacote de recursos (aceite o pacote ao entrar no servidor).</p>\n  <div class=\"grade\">\n");
        for (JsonElement ge : c.getAsJsonArray("grupos")) {
            JsonArray g = ge.getAsJsonArray();
            String id = g.get(0).getAsString();
            JsonArray itens = c.getAsJsonObject("itens").getAsJsonArray(id);
            if (itens == null || itens.isEmpty()) continue;
            sb.append("    <div class=\"card largo vitrine-itens\"><h3>").append(esc(g.get(1).getAsString())).append("</h3><p class=\"muted\">")
                    .append(esc(g.get(2).getAsString())).append("</p><div class=\"itens\">");
            for (JsonElement ie : itens) {
                JsonArray it = ie.getAsJsonArray();
                sb.append("<figure><img class=\"pixel\" src=\"img/itens/").append(it.get(0).getAsString()).append(".png\" alt=\"\" width=\"40\" height=\"40\" loading=\"lazy\"><figcaption>")
                        .append(esc(it.get(1).getAsString())).append("</figcaption></figure>");
            }
            sb.append("</div></div>\n");
        }
        return sb.append("  </div>\n</section>").toString();
    }

    static String entre(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i < 0) throw new IllegalStateException("não achei " + a);
        i += a.length();
        int j = s.indexOf(b, i);
        if (j < 0) throw new IllegalStateException("não achei " + b);
        return s.substring(i, j);
    }

    static final String CSS_EXTRA = """

/* receitas da bancada */
.pixel{image-rendering:pixelated}
.receita{display:flex;flex-direction:column;gap:10px}
.rec-topo{display:flex;align-items:center;gap:12px}
.rec-topo img,.sem-icone{width:48px;height:48px;flex:none;border-radius:8px;background:var(--surface-2);border:1px solid var(--border);padding:4px}
.sem-icone{display:grid;place-items:center;font-family:var(--display);font-size:1.4rem;color:var(--accent)}
.rec-topo h3{margin:0}
.rec-cat{margin:0;font-size:.7rem;letter-spacing:.12em;text-transform:uppercase;color:var(--accent);font-weight:600}
.rec-desc{color:var(--muted);font-size:.9rem;margin:0}
.rec-modo{margin:0;font-size:.78rem;letter-spacing:.06em;text-transform:uppercase;color:var(--muted);font-weight:600}
.rec-corpo{display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap}
.bancada{display:grid;grid-template-columns:repeat(3,34px);grid-template-rows:repeat(3,34px);gap:3px;padding:6px;background:var(--surface-2);border:1px solid var(--border);border-radius:8px;flex:none}
.bancada span{border-radius:5px;background:var(--bg);border:1px solid var(--border);display:grid;place-items:center;font-family:var(--mono);font-size:.9rem;font-weight:500}
.legenda{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:5px;min-width:0;flex:1 1 160px;font-size:.88rem}
.legenda li{display:flex;gap:8px;align-items:flex-start}
.legenda span{flex:none;width:22px;height:22px;border-radius:5px;display:grid;place-items:center;font-family:var(--mono);font-size:.8rem}
.c0{background:var(--accent-soft)!important;color:var(--chip-ink);border-color:var(--accent)!important}
.c1{background:color-mix(in srgb,var(--gold) 22%,var(--surface))!important;color:var(--text);border-color:var(--gold)!important}
.c2{background:color-mix(in srgb,var(--verde) 20%,var(--surface))!important;color:var(--text);border-color:var(--verde)!important}
.c3{background:color-mix(in srgb,var(--vermelho) 18%,var(--surface))!important;color:var(--text);border-color:var(--vermelho)!important}
.c4{background:color-mix(in srgb,var(--muted) 22%,var(--surface))!important;color:var(--text);border-color:var(--muted)!important}
.lista-ing{margin:0;padding-left:18px;font-size:.9rem}

/* coleção */
.vitrine-itens .itens{display:grid;grid-template-columns:repeat(auto-fill,minmax(96px,1fr));gap:8px;margin-top:10px}
.vitrine-itens figure{margin:0;display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 4px;border-radius:8px;background:var(--surface-2);text-align:center}
.vitrine-itens figcaption{font-size:.74rem;line-height:1.2}
""";
}
