package br.rpgatributos.exploracao;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelos e texturas próprios para quem joga com EMF + ETF (ou OptiFine), 2.55. Cada {@link Alvo} é uma
 * criatura do plugin reconhecida pelo nome (o cliente não vê os dados do servidor, só o nome): moradores
 * das vilas, mestres de armas, chefes do Altar, Bestas Errantes, companheiros evoluídos, invocações,
 * Sombras e Elites.
 *
 * <p>Os arquivos preparados (forma e textura vanilla de cada criatura) ficam em blockbench/preparados,
 * para abrir no Blockbench e editar. O que estiver pronto entra no pacote de dois jeitos:
 * <ul>
 *   <li>no jar, em resources/modelos/visual/&lt;id&gt;.jem e &lt;id&gt;.png;</li>
 *   <li>no servidor, em plugins/RPGAtributos/pacote/modelos/&lt;id&gt;.jem e .png (vale sem recompilar,
 *       depois de /rpgadmin modelos).</li>
 * </ul>
 * Só vai para o pacote o alvo que tem o .jem; as regras por nome (os .properties) são montadas aqui,
 * somadas às dos Guardiões. Num aldeão com modelo próprio, as camadas de bioma, profissão e nível ficam
 * transparentes (a textura do modelo já traz a roupa).
 */
public final class ModelosVisuais {

    private ModelosVisuais() { }

    /**
     * @param grupo  pasta em blockbench/preparados
     * @param id     nome dos arquivos (id.jem, id.png)
     * @param cem    arquivo da criatura no EMF (villager, wolf, iron_golem...)
     * @param nome   regra por nome (regex Java, o nome inteiro)
     * @param texturas texturas vanilla de base, empilhadas (para o arquivo preparado)
     * @param sobre  o que é, para o LEIA-ME
     */
    public record Alvo(String grupo, String id, String cem, String nome, List<String> texturas, String sobre) { }

    private static final List<Alvo> ALVOS = new ArrayList<>();

    private static void a(String grupo, String id, String cem, String nome, String sobre, String... texturas) {
        ALVOS.add(new Alvo(grupo, id, cem, nome, List.of(texturas), sobre));
    }

    private static void npc(String vila, String id, String titulo, String tipo, String profissao, String sobre) {
        List<String> t = new ArrayList<>(List.of("villager/villager", "villager/type/" + tipo));
        if (!profissao.equals("none")) t.add("villager/profession/" + profissao);
        ALVOS.add(new Alvo("vilas/" + vila, id, "villager", titulo + " .*", t, sobre));
    }

    static {
        // Vilas: um modelo por morador (o nome é "<título> <nome>"). Bioma e profissão de cada um.
        npc("pescadores", "velho_pescador", "Velho Pescador", "plains", "fisherman", "Velho Pescador (Vila de Pescadores)");
        npc("pescadores", "peixeira", "Peixeira", "swamp", "fisherman", "Peixeira (Vila de Pescadores)");
        npc("pescadores", "carpinteiro_naval", "Carpinteiro Naval", "plains", "fletcher", "Carpinteiro Naval (Vila de Pescadores)");
        npc("pescadores", "menina_do_cais", "Menina do Cais", "plains", "none", "Menina do Cais (Vila de Pescadores, pequena)");
        npc("pescadores", "mestra_das_mares", "Mestra das Marés", "swamp", "fisherman", "Mestra das Marés: mestra de armas (arpão)");
        npc("mosteiro", "mestre", "Mestre", "snow", "weaponsmith", "Mestre do Mosteiro (treino diário)");
        npc("mosteiro", "monge_herbalista", "Monge Herbalista", "snow", "cleric", "Monge Herbalista (Mosteiro)");
        npc("mosteiro", "guardiao_do_sino", "Guardião do Sino", "snow", "cleric", "Guardião do Sino (Mosteiro)");
        npc("mosteiro", "peregrino", "Peregrino", "taiga", "nitwit", "Peregrino (Mosteiro)");
        npc("mosteiro", "mestre_do_bastao", "Mestre do Bastão", "snow", "weaponsmith", "Mestre do Bastão: mestre de armas");
        npc("anoes", "mestre_mineiro", "Mestre Mineiro", "taiga", "toolsmith", "Mestre Mineiro (Aldeia dos Anões)");
        npc("anoes", "joalheiro", "Joalheiro", "taiga", "mason", "Joalheiro (Aldeia dos Anões)");
        npc("anoes", "ferreiro_anao", "Ferreiro Anão", "taiga", "armorer", "Ferreiro Anão (Aldeia dos Anões)");
        npc("anoes", "taverneiro", "Taverneiro", "taiga", "butcher", "Taverneiro (Aldeia dos Anões)");
        npc("anoes", "mestre_de_guerra", "Mestre de Guerra", "taiga", "weaponsmith", "Mestre de Guerra: mestre de armas anão");
        npc("elfica", "druida", "Druida", "jungle", "farmer", "Druida (Vila Élfica)");
        npc("elfica", "arqueira", "Arqueira", "jungle", "fletcher", "Arqueira (Vila Élfica)");
        npc("elfica", "curandeira", "Curandeira", "jungle", "cleric", "Curandeira (Vila Élfica)");
        npc("elfica", "tecela", "Tecelã", "jungle", "shepherd", "Tecelã (Vila Élfica)");
        npc("elfica", "mestra_da_lamina", "Mestra da Lâmina", "jungle", "weaponsmith", "Mestra da Lâmina: mestra de armas élfica");
        a("personagens", "cronista", "villager", ".*O Cronista.*", "O Cronista", "villager/villager", "villager/type/plains", "villager/profession/librarian");
        a("personagens", "mercador_arcano", "wandering_trader", ".*Mercador Arcano.*", "Mercador Arcano (Altar)", "wandering_trader/wandering_trader");

        // Chefes do Altar (nome "☠ <chefe> ☠").
        a("chefes", "golem_ancestral", "iron_golem", ".*Golem Ancestral.*", "Golem Ancestral", "iron_golem/iron_golem");
        a("chefes", "rainha_aracnidea", "spider", ".*Rainha Aracnídea.*", "Rainha Aracnídea", "spider/spider");
        a("chefes", "senhor_das_chamas", "blaze", ".*Senhor das Chamas.*", "Senhor das Chamas", "blaze/blaze");
        a("chefes", "lich", "skeleton", ".*☠ Lich.*", "Lich", "skeleton/skeleton");
        a("chefes", "tempestade_viva", "breeze", ".*Tempestade Viva.*", "Tempestade Viva", "breeze/breeze");
        a("chefes", "arauto_do_fim", "wither_skeleton", ".*Arauto do Fim.*", "Arauto do Fim", "skeleton/wither_skeleton");

        // Bestas Errantes (as lendárias soltas; os Guardiões já têm modelo próprio).
        a("bestas", "pixiu", "wolf", "Pixiu.*", "Pixiu, a besta do ouro (lobo)", "wolf/wolf");
        a("bestas", "baize", "horse", "Baize.*", "Baize, a besta da sabedoria (cavalo)", "horse/horse_white");
        a("bestas", "kunpeng", "happy_ghast", "Kunpeng.*", "Kunpeng, o peixe-pássaro (ghast feliz)", "ghast/happy_ghast");

        // Companheiros evoluídos (vale enquanto o jogador não der outro nome).
        a("companheiros/lobo", "lobo_alfa", "wolf", "Lobo Alfa", "Lobo, estágio II", "wolf/wolf_tame");
        a("companheiros/lobo", "lobo_das_sombras", "wolf", "Lobo das Sombras", "Lobo, estágio III", "wolf/wolf_tame");
        a("companheiros/gato", "gato_selvagem", "cat", "Gato Selvagem", "Gato, estágio II", "cat/cat_tabby");
        a("companheiros/gato", "pantera_sombria", "cat", "Pantera Sombria", "Gato, estágio III", "cat/cat_all_black");
        a("companheiros/cavalo", "corcel_de_guerra", "horse", "Corcel de Guerra", "Cavalo, estágio II", "horse/horse_brown");
        a("companheiros/cavalo", "corcel_celeste", "horse", "Corcel Celeste", "Cavalo, estágio III", "horse/horse_white");
        a("companheiros/golem", "golem_de_guerra", "iron_golem", "Golem de Guerra", "Golem de ferro, estágio II", "iron_golem/iron_golem");
        a("companheiros/golem", "colosso_de_ferro", "iron_golem", "Colosso de Ferro", "Golem de ferro, estágio III", "iron_golem/iron_golem");
        a("companheiros/urso", "urso_glacial", "polar_bear", "Urso Glacial", "Urso-polar, estágio II", "bear/polarbear");
        a("companheiros/urso", "urso_do_inverno_eterno", "polar_bear", "Urso do Inverno Eterno", "Urso-polar, estágio III", "bear/polarbear");
        a("companheiros/raposa", "raposa_astuta", "fox", "Raposa Astuta", "Raposa, estágio II", "fox/fox");
        a("companheiros/raposa", "raposa_espiritual", "fox", "Raposa Espiritual", "Raposa, estágio III", "fox/fox_snow");
        a("companheiros/camelo", "camelo_do_deserto", "camel", "Camelo do Deserto", "Camelo, estágio II", "camel/camel");
        a("companheiros/camelo", "camelo_das_areias_eternas", "camel", "Camelo das Areias Eternas", "Camelo, estágio III", "camel/camel");
        a("companheiros/lhama", "lhama_guerreira", "llama", "Lhama Guerreira", "Lhama, estágio II", "llama/llama_brown");
        a("companheiros/lhama", "lhama_ancestral", "llama", "Lhama Ancestral", "Lhama, estágio III", "llama/llama_white");
        a("companheiros/papagaio", "papagaio_real", "parrot", "Papagaio Real", "Papagaio, estágio II", "parrot/parrot_red_blue");
        a("companheiros/papagaio", "ave_do_trovao", "parrot", "Ave do Trovão", "Papagaio, estágio III", "parrot/parrot_blue");

        // Invocações.
        a("invocacoes", "lobo_espectral", "wolf", "Lobo Espectral", "Lobos espectrais (técnicas e golpes)", "wolf/wolf_tame");
        a("invocacoes", "lobo_ancestral", "wolf", "Lobo Ancestral", "Lobos ancestrais (habilidade de classe)", "wolf/wolf_tame");
        a("invocacoes", "automato", "iron_golem", "⚙ Autômato.*", "Autômato do Gnomo Inventor", "iron_golem/iron_golem");
        a("invocacoes", "golem_de_pedra", "iron_golem", "✦ (Golem de Pedra|Elemental de Terra).*", "Feitiço de Terra (golem)", "iron_golem/iron_golem");
        a("invocacoes", "elemental", "vex", "✦ (Elemental de|Sombra Gêmea).*", "Feitiços: elementais e Sombra Gêmea (vex)", "illager/vex");
        a("invocacoes", "espirito", "allay", "✦ Espírito.*", "Feitiço de Vida: espíritos (allay)", "allay/allay");
        a("sombras", "sombra_morto_vivo", "zombie", "☾ .*", "Sombra: Morto-vivo (zumbi)", "zombie/zombie");
        a("sombras", "sombra_arqueiro", "skeleton", "☾ .*", "Sombra: Arqueiro (esqueleto)", "skeleton/skeleton");
        a("sombras", "sombra_aranha", "spider", "☾ .*", "Sombra: Aranha", "spider/spider");

        // Elites por afixo (o nome traz as estrelas, o grau e os afixos; vale o primeiro da lista).
        String[][] elites = {{"zombie", "zombie/zombie"}, {"skeleton", "skeleton/skeleton"}, {"spider", "spider/spider"}, {"creeper", "creeper/creeper"}};
        String[][] afixos = {{"gelido", "Gélido"}, {"incendiario", "Incendiário"}, {"venenoso", "Venenoso"}, {"fantasma", "Fantasma"}, {"blindado", "Blindado"}};
        for (String[] af : afixos) {
            for (String[] el : elites) {
                a("elites/" + af[0], "elite_" + el[0] + "_" + af[0], el[0], "★.*" + af[1] + ".*",
                        "Elite " + af[1] + " (" + el[0] + ")", el[1]);
            }
        }
    }

    public static List<Alvo> alvos() {
        return List.copyOf(ALVOS);
    }

    private static final String[] TIPOS_ALDEAO = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};
    private static final String[] PROFISSOES = {"armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith"};
    private static final String[] NIVEIS = {"stone", "iron", "gold", "emerald", "diamond"};

    /** Quantos alvos têm modelo neste pacote (para o /rpgadmin modelos). */
    public static List<String> prontos(File pasta) {
        List<String> l = new ArrayList<>();
        for (Alvo a : ALVOS) if (ler(pasta, a.id() + ".jem") != null) l.add(a.id());
        return l;
    }

    /**
     * Junta os modelos prontos ao pacote. Recebe o mapa já montado (com os Guardiões) para somar as
     * regras por nome às que já existem no mesmo .properties.
     */
    public static void somar(Map<String, byte[]> f, File pasta) throws IOException {
        String cem = "assets/minecraft/optifine/cem/";
        Map<String, List<String[]>> novas = new LinkedHashMap<>(); // cem -> [arquivo, regex]
        List<String> aldeoes = new ArrayList<>();
        for (Alvo a : ALVOS) {
            byte[] jem = ler(pasta, a.id() + ".jem");
            if (jem == null) continue;
            byte[] png = ler(pasta, a.id() + ".png");
            String textura = "rpg_" + a.id() + ".png";
            JsonObject o;
            try {
                o = JsonParser.parseString(new String(jem, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (RuntimeException ex) {
                throw new IOException("Modelo " + a.id() + ".jem com erro: " + ex.getMessage());
            }
            if (png != null) {
                o.addProperty("texture", textura);
                f.put(cem + textura, png);
            }
            List<String[]> l = novas.computeIfAbsent(a.cem(), k -> new ArrayList<>());
            l.add(new String[]{a.id(), a.nome()});
            f.put(cem + "rpg_" + a.id() + ".jem", o.toString().getBytes(StandardCharsets.UTF_8));
            if (a.cem().equals("villager")) aldeoes.add(a.nome());
        }
        for (Map.Entry<String, List<String[]>> en : novas.entrySet()) {
            String base = en.getKey();
            String chave = cem + base + ".properties";
            Regras r = Regras.ler(f.get(chave));
            for (String[] n : en.getValue()) {
                // O EMF numera os modelos (base2.jem, base3.jem...): o modelo vai com o próximo número livre.
                int num = r.proximoArquivo();
                f.put(cem + base + num + ".jem", f.remove(cem + "rpg_" + n[0] + ".jem"));
                r.regras.add(new String[]{String.valueOf(num), n[1]});
            }
            f.put(chave, r.gravar("RPGAtributos - modelos por nome (" + base + ")"));
        }
        if (!aldeoes.isEmpty()) esconderCamadasAldeao(f, aldeoes);
    }

    /** Bioma, profissão e nível do aldeão com modelo próprio viram textura transparente (ETF). */
    private static void esconderCamadasAldeao(Map<String, byte[]> f, List<String> nomes) {
        byte[] vazio = pngVazio();
        String raiz = "assets/minecraft/optifine/random/entity/villager/";
        List<String> caminhos = new ArrayList<>();
        for (String t : TIPOS_ALDEAO) caminhos.add("type/" + t);
        for (String p : PROFISSOES) caminhos.add("profession/" + p);
        for (String n : NIVEIS) caminhos.add("profession_level/" + n);
        String regex = "regex:(" + String.join("|", nomes) + ")";
        for (String c : caminhos) {
            f.put(raiz + c + "2.png", vazio);
            String props = "# RPGAtributos - aldeoes com modelo proprio: a roupa vem na textura do modelo\n"
                    + "textures.1=2\nname.1=" + ascii(regex) + "\n";
            f.put(raiz + c + ".properties", props.getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private static byte[] pngVazio() {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Primeiro o servidor (pacote/modelos, em qualquer subpasta), depois o jar. */
    private static byte[] ler(File pasta, String nome) {
        if (pasta != null && pasta.isDirectory()) {
            File achado = achar(pasta, nome, 0);
            if (achado != null) {
                try {
                    return Files.readAllBytes(achado.toPath());
                } catch (IOException ignored) { }
            }
        }
        try (InputStream in = ModelosVisuais.class.getResourceAsStream("/modelos/visual/" + nome)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException ex) {
            return null;
        }
    }

    private static File achar(File pasta, String nome, int fundo) {
        File direto = new File(pasta, nome);
        if (direto.isFile()) return direto;
        if (fundo > 3) return null;
        File[] sub = pasta.listFiles(File::isDirectory);
        if (sub == null) return null;
        for (File s : sub) {
            File r = achar(s, nome, fundo + 1);
            if (r != null) return r;
        }
        return null;
    }

    /** Escreve o texto só com ASCII (o .properties é lido em ISO-8859-1): o resto vira \\uXXXX. */
    static String ascii(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 128) b.append(c);
            else b.append(String.format("\\u%04x", (int) c));
        }
        return b.toString();
    }

    /** As regras de um .properties do EMF: modelo + nome, e a regra sem nome (o padrão) por último. */
    static final class Regras {
        final List<String[]> regras = new ArrayList<>();
        String padrao;
        int maior = 1;

        static Regras ler(byte[] dados) {
            Regras r = new Regras();
            if (dados == null) return r;
            java.util.Properties p = new java.util.Properties();
            try {
                p.load(new java.io.ByteArrayInputStream(dados));
            } catch (IOException ex) {
                return r;
            }
            for (int i = 1; i <= 64; i++) {
                String m = p.getProperty("models." + i);
                if (m == null) continue;
                String n = p.getProperty("name." + i);
                try {
                    r.maior = Math.max(r.maior, Integer.parseInt(m.trim()));
                } catch (NumberFormatException ignored) { }
                if (n == null) r.padrao = m.trim();
                else r.regras.add(new String[]{m.trim(), n.startsWith("regex:") ? n.substring(6) : n});
            }
            return r;
        }

        int proximoArquivo() {
            return ++maior;
        }

        byte[] gravar(String titulo) {
            StringBuilder b = new StringBuilder("# ").append(titulo).append('\n');
            int i = 1;
            for (String[] r : regras) {
                b.append("models.").append(i).append('=').append(r[0]).append('\n');
                b.append("name.").append(i).append("=regex:").append(ascii(r[1])).append('\n');
                i++;
            }
            b.append("models.").append(i).append('=').append(padrao != null ? padrao : "1").append('\n');
            return b.toString().getBytes(StandardCharsets.ISO_8859_1);
        }
    }
}
