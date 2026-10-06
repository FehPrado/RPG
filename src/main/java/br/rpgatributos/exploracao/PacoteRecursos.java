package br.rpgatributos.exploracao;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.alquimia.Acessorio;
import br.rpgatributos.combate.Mobilidade;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * O pacote de recursos do próprio plugin: gerado ao ligar (texturas desenhadas pelo código),
 * guardado em plugins/RPGAtributos/pacote/ e enviado aos jogadores junto com qualquer outro
 * pacote do servidor. Ele pode ser servido pelo próprio plugin (numa porta) ou por um link.
 * Sem o pacote, tudo continua funcionando com a aparência de itens e blocos do jogo.
 */
public final class PacoteRecursos implements Listener {

    public static final UUID ID = UUID.nameUUIDFromBytes("rpgatributos-pacote".getBytes(StandardCharsets.UTF_8));
    public static final String NOME = "RPGAtributos-recursos.zip";

    private final RPGAtributos plugin;
    private File arquivo;
    private String sha1;
    private Object servidor;

    public PacoteRecursos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public File arquivo() { return arquivo; }
    public String sha1() { return sha1; }

    /** Link que os jogadores usam para baixar, ou null se não há como enviar. */
    public String url() {
        var c = plugin.settings();
        if (!c.pacAtivado) return null;
        if (!c.pacUrl.isBlank()) return c.pacUrl.trim();
        if (servidor != null && c.pacPorta > 0 && !c.pacEndereco.isBlank()) return "http://" + c.pacEndereco.trim() + ":" + c.pacPorta + "/" + NOME;
        return null;
    }

    // =====================================================================
    //  Gerar
    // =====================================================================

    public void iniciar() {
        if (!plugin.settings().pacAtivado) return;
        try {
            File pasta = new File(plugin.getDataFolder(), "pacote");
            if (!pasta.exists() && !pasta.mkdirs()) throw new IOException("não criei a pasta " + pasta);
            byte[] zip = montar(pasta);
            arquivo = new File(pasta, NOME);
            Files.write(arquivo.toPath(), zip);
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(zip));
        } catch (Exception ex) {
            plugin.getLogger().warning("Não consegui gerar o pacote de recursos: " + ex.getMessage());
            return;
        }
        int porta = plugin.settings().pacPorta;
        if (porta > 0) servir(porta);
        if (url() == null) {
            plugin.getLogger().info("Pacote de recursos gerado em " + arquivo.getPath() + " (SHA-1 " + sha1 + "). "
                    + "Para enviar aos jogadores: suba o arquivo e ponha o link em pacote-de-recursos.url, "
                    + "ou use pacote-de-recursos.porta + endereco.");
        }
    }

    private void servir(int porta) {
        try {
            com.sun.net.httpserver.HttpServer s = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(porta), 0);
            s.createContext("/" + NOME, troca -> {
                byte[] dados = Files.readAllBytes(arquivo.toPath());
                troca.getResponseHeaders().add("Content-Type", "application/zip");
                troca.sendResponseHeaders(200, dados.length);
                try (var out = troca.getResponseBody()) {
                    out.write(dados);
                }
            });
            s.start();
            servidor = s;
            plugin.getLogger().info("Pacote de recursos servido na porta " + porta + ".");
        } catch (Throwable t) {
            plugin.getLogger().warning("Não consegui abrir a porta " + porta + " para o pacote de recursos: " + t.getMessage());
        }
    }

    public void parar() {
        if (servidor instanceof com.sun.net.httpserver.HttpServer s) s.stop(0);
        servidor = null;
    }

    // =====================================================================
    //  Enviar
    // =====================================================================

    public void enviar(Player p) {
        String url = url();
        if (url == null || sha1 == null) return;
        try {
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(ID, URI.create(url), sha1);
            p.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).replace(false)
                    .required(plugin.settings().pacObrigatorio)
                    .prompt(Component.text("Texturas do RPGAtributos (minérios e itens novos)", NamedTextColor.GOLD)).build());
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Link do pacote de recursos inválido: " + ex.getMessage());
        }
    }

    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        atualizar(p.getInventory());
        atualizar(p.getEnderChest());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) enviar(p); }, 40L);
    }

    // =====================================================================
    //  Itens com visual próprio
    // =====================================================================

    private static final NamespacedKey K_FORJA = new NamespacedKey("rpgatributos", "forja_raridade");

    /** Marca o item para usar o visual do pacote (sem o pacote, ele continua com o visual do jogo). */
    public static void marcar(ItemMeta m, String id) {
        var cmd = m.getCustomModelDataComponent();
        // O pacote olha a primeira string para o visual; a segunda é a postura (pegada da arma).
        List<String> l = new java.util.ArrayList<>(cmd.getStrings());
        if (l.isEmpty()) l.add("rpgatributos:" + id);
        else l.set(0, "rpgatributos:" + id);
        cmd.setStrings(l);
        m.setCustomModelDataComponent(cmd);
    }

    /** Põe (ou tira, com null) a postura na segunda marca do item, sem mexer no visual da primeira. */
    public static void marcarPostura(ItemMeta m, String postura) {
        var cmd = m.getCustomModelDataComponent();
        List<String> l = new java.util.ArrayList<>(cmd.getStrings());
        if (postura == null) {
            if (l.size() < 2) return;
            while (l.size() > 1) l.removeLast();
            if (l.getFirst().isEmpty()) l.clear();
        } else {
            if (l.isEmpty()) l.add("");
            if (l.size() == 1) l.add("rpgatributos:postura_" + postura);
            else l.set(1, "rpgatributos:postura_" + postura);
        }
        cmd.setStrings(l);
        m.setCustomModelDataComponent(cmd);
    }

    /** A postura marcada no item ("" se nenhuma). */
    public static String postura(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return "";
        List<String> l = i.getItemMeta().getCustomModelDataComponent().getStrings();
        return l.size() < 2 ? "" : l.get(1).replace("rpgatributos:postura_", "");
    }

    /** Itens feitos antes das texturas (gancho, cajado forjado, acessórios) ganham a marca. */
    public static ItemStack atualizar(ItemStack i) {
        String id = idVisual(i);
        if (id == null) return i;
        Acessorio acessorio = Acessorio.de(i);
        // Acessórios: o item do jogo por baixo pode ter mudado (escudo e relógio têm modelos especiais).
        boolean modeloCerto = acessorio == null || acessorio.modelo().equals(i.getItemMeta().getItemModel());
        List<String> atual = i.getItemMeta().getCustomModelDataComponent().getStrings();
        if (modeloCerto && !atual.isEmpty() && atual.getFirst().equals("rpgatributos:" + id)) return i;
        i.editMeta(m -> {
            marcar(m, id);
            if (acessorio != null) m.setItemModel(acessorio.modelo());
        });
        return i;
    }

    private static String idVisual(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        if (Mobilidade.ehGancho(i)) return "gancho";
        if (i.getType() == Material.BREEZE_ROD && i.getPersistentDataContainer().has(K_FORJA)) return "cajado_arcano";
        Acessorio a = Acessorio.de(i);
        if (a != null) return a.id();
        var peixe = br.rpgatributos.pesca.PeixeRaro.de(i);
        if (peixe != null) return "peixe_" + peixe.name().toLowerCase(java.util.Locale.ROOT);
        String prato = i.getPersistentDataContainer().get(br.rpgatributos.fazenda.Cozinha.CHAVE_PRATO, org.bukkit.persistence.PersistentDataType.STRING);
        if (prato != null) return "prato_" + prato.toLowerCase(java.util.Locale.ROOT);
        var v = br.rpgatributos.fazenda.Variedade.de(i);
        if (v != null) return br.rpgatributos.fazenda.Variedade.ehSemente(i) ? v.idVisualSemente() : "variedade_" + v.id();
        var gema = br.rpgatributos.alquimia.Gema.de(i);
        if (gema != null) return "gema_" + gema.name().toLowerCase(java.util.Locale.ROOT) + "_" + br.rpgatributos.alquimia.Gema.grau(i);
        var r = br.rpgatributos.alquimia.Reagente.de(i);
        if (r != null) return "componente_" + r.id();
        var e = br.rpgatributos.alquimia.Elixir.de(i);
        if (e != null) return "elixir_" + e.id();
        var raro = br.rpgatributos.aventura.Raro.de(i);
        if (raro != null) return "raro_" + raro.id();
        String lenda = i.getPersistentDataContainer().get(K_LENDA, org.bukkit.persistence.PersistentDataType.STRING);
        if (lenda != null) return "lenda_" + lenda.toLowerCase(java.util.Locale.ROOT);
        var erva = br.rpgatributos.vida.Erva.de(i);
        if (erva != null) return "erva_" + erva.id();
        var mel = br.rpgatributos.vida.Mel.de(i);
        if (mel != null) return "mel_" + mel.id();
        var bebida = br.rpgatributos.vida.Bebida.de(i);
        if (bebida != null) return "bebida_" + bebida.id();
        var carta = br.rpgatributos.vida.Album.de(i);
        if (carta != null) return br.rpgatributos.vida.Album.brilhante(i) ? "carta_brilhante"
                : "carta_" + new String[]{"comum", "rara", "epica"}[carta.raridade()];
        var flecha = br.rpgatributos.vida.Flechas.tipo(i);
        if (flecha != null) return "flecha_" + flecha.id();
        var fruta = br.rpgatributos.vida.Fruta.de(i);
        if (fruta != null) return "fruta_" + fruta.id();
        var frasco = br.rpgatributos.vida.Frascos.tipo(i);
        return frasco == null ? null : "frasco_" + frasco.id();
    }

    private static final NamespacedKey K_LENDA = new NamespacedKey("rpgatributos", "lenda");

    /** Baús e outros inventários abertos: os itens antigos ganham o visual novo. */
    @EventHandler(ignoreCancelled = true)
    public void aoAbrir(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof org.bukkit.block.Container || e.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest) {
            atualizar(e.getInventory());
        }
    }

    /** Item pego do chão também. */
    @EventHandler(ignoreCancelled = true)
    public void aoPegar(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        ItemStack s = e.getItem().getItemStack();
        if (idVisual(s) != null) e.getItem().setItemStack(atualizar(s));
    }

    private static void atualizar(Inventory inv) {
        for (int s = 0; s < inv.getSize(); s++) {
            ItemStack i = inv.getItem(s);
            if (idVisual(i) != null) inv.setItem(s, atualizar(i));
        }
    }

    // =====================================================================
    //  Conteúdo do pacote
    // =====================================================================

    /**
     * Fonte dos menus: dois "espaços negativos" (voltam o cursor) e uma letra por fundo, do tamanho
     * do baú de 6 linhas (176x222). O título do menu escreve: volta 8 px, desenha o fundo, volta 169 px.
     */
    private static String fonteMenus() {
        StringBuilder sb = new StringBuilder("{\n  \"providers\": [\n");
        sb.append(String.format("    { \"type\": \"space\", \"advances\": { \"\\u%04X\": -8, \"\\u%04X\": -169 } }",
                (int) br.rpgatributos.ajuda.Fundo.VOLTA_ANTES, (int) br.rpgatributos.ajuda.Fundo.VOLTA_DEPOIS));
        for (br.rpgatributos.ajuda.Fundo fu : br.rpgatributos.ajuda.Fundo.values()) {
            sb.append(String.format(",\n    { \"type\": \"bitmap\", \"file\": \"rpgatributos:gui/menu_%s.png\", \"ascent\": 13, \"height\": %d, \"chars\": [\"\\u%04X\"] }",
                    fu.id(), br.rpgatributos.ajuda.Fundo.ALTURA, (int) fu.letra()));
        }
        return sb.append("\n  ]\n}\n").toString();
    }

    private byte[] montar(File pasta) throws IOException {
        Map<String, byte[]> f = new LinkedHashMap<>();
        f.put("pack.mcmeta", texto("""
                {
                  "pack": {
                    "description": "§6RPGAtributos §7— texturas do plugin",
                    "pack_format": 97,
                    "min_format": 88,
                    "max_format": 999
                  }
                }
                """));
        // Minério de Mitrilo: um estado do bloco de cogumelo marrom que a natureza não gera.
        StringBuilder estados = new StringBuilder("{\n  \"variants\": {\n");
        String[] faces = {"down", "east", "north", "south", "up", "west"};
        for (int bits = 0; bits < 64; bits++) {
            StringBuilder chave = new StringBuilder();
            for (int i = 0; i < 6; i++) chave.append(i == 0 ? "" : ",").append(faces[i]).append('=').append((bits >> i & 1) == 1);
            String modelo = bits == Mitrilo.ESTADO_MINERIO ? "rpgatributos:block/minerio_mitrilo" : "rpgatributos:block/cogumelo/c" + bits;
            estados.append("    \"").append(chave).append("\": { \"model\": \"").append(modelo).append("\" }").append(bits < 63 ? ",\n" : "\n");
            if (bits != Mitrilo.ESTADO_MINERIO) f.put("assets/rpgatributos/models/block/cogumelo/c" + bits + ".json", texto(modeloCogumelo(bits, faces)));
        }
        estados.append("  }\n}\n");
        f.put("assets/minecraft/blockstates/brown_mushroom_block.json", texto(estados.toString()));
        f.put("assets/rpgatributos/models/block/minerio_mitrilo.json",
                texto("{ \"parent\": \"minecraft:block/cube_all\", \"textures\": { \"all\": \"rpgatributos:block/minerio_mitrilo\" } }"));
        // Itens: só trocam o visual quando têm o custom_model_data do plugin (o resto fica igual ao jogo).
        // Vários visuais podem entrar no mesmo item do jogo (ex.: vários peixes no bacalhau).
        Map<String, List<String>> casos = new LinkedHashMap<>();
        caso(casos, "raw_iron", "mitrilo_bruto");
        caso(casos, "iron_ingot", "lingote_mitrilo");
        caso(casos, "breeze_rod", "cajado_arcano");
        caso(casos, "phantom_membrane", "capa_planadora");
        caso(casos, "iron_nugget", "ferradura");
        caso(casos, "bowl", "ninho_de_passaro");
        java.util.Set<String> naMao = new java.util.HashSet<>(List.of("cajado_arcano"));
        for (ArteItens.Arte a : ArteItens.todas()) {
            caso(casos, a.vanilla(), a.id());
            if (a.naMao()) naMao.add(a.id());
        }
        for (Map.Entry<String, List<String>> en : casos.entrySet()) {
            f.put("assets/minecraft/items/" + en.getKey() + ".json", texto(selecao(en.getKey(), en.getValue())));
            for (String id : en.getValue()) {
                // As fases das plantações da estação são plantas em X (como as do jogo), mostradas em cima da terra.
                f.put("assets/rpgatributos/models/item/" + id + ".json", texto(id.startsWith("planta_")
                        ? "{ \"parent\": \"minecraft:block/cross\", \"textures\": { \"cross\": \"rpgatributos:item/" + id + "\" } }"
                        : naMao.contains(id) ? itemComPai("minecraft:item/handheld", "rpgatributos:item/" + id) : itemGerado("rpgatributos:item/" + id)));
            }
        }
        // Espadas, machados e maça: a pegada muda com a postura (2ª marca), por cima do visual da lenda (1ª).
        for (Material m : Material.values()) {
            String n = m.name();
            if (m.isLegacy() || !m.isItem() || !(n.endsWith("_SWORD") || n.endsWith("_AXE") || m == Material.MACE)) continue;
            String vanilla = n.toLowerCase(java.util.Locale.ROOT);
            List<String> lendas = casos.getOrDefault(vanilla, List.of());
            List<String> pegadas = new java.util.ArrayList<>(List.of("ofensiva", "defensiva", "agil"));
            pegadas.add(n.endsWith("_SWORD") ? "duelista" : n.endsWith("_AXE") ? "carrasco" : "colosso");
            f.put("assets/minecraft/items/" + vanilla + ".json", texto(selecaoPostura(vanilla, lendas, pegadas)));
            for (String pg : pegadas) {
                f.put("assets/rpgatributos/models/item/postura/" + vanilla + "_" + pg + ".json", texto(modeloPegada("minecraft:item/" + vanilla, pg)));
                for (String id : lendas) {
                    f.put("assets/rpgatributos/models/item/postura/" + id + "_" + pg + ".json", texto(modeloPegada("rpgatributos:item/" + id, pg)));
                }
            }
        }
        f.put("assets/minecraft/items/fishing_rod.json", texto(vara()));
        f.put("assets/rpgatributos/models/item/gancho.json", texto(itemComPai("minecraft:item/handheld_rod", "rpgatributos:item/gancho")));
        f.put("assets/rpgatributos/models/item/gancho_lancado.json", texto(itemComPai("minecraft:item/handheld_rod", "rpgatributos:item/gancho_lancado")));

        // Texturas: as desenhadas pelo código, ou um PNG seu com o mesmo nome em pacote/texturas/.
        Map<String, BufferedImage> texturas = new LinkedHashMap<>();
        // Fundos desenhados dos menus (/rpg, Guia, Jornada): entram como letras da fonte rpgatributos:menus.
        for (br.rpgatributos.ajuda.Fundo fu : br.rpgatributos.ajuda.Fundo.values()) texturas.put("gui/menu_" + fu.id(), fu.desenhar());
        texturas.put("block/minerio_mitrilo", minerio());
        texturas.put("item/mitrilo_bruto", desenho(BRUTO, MITRILO));
        texturas.put("item/lingote_mitrilo", desenho(LINGOTE, MITRILO));
        texturas.put("item/cajado_arcano", desenho(CAJADO, CORES_CAJADO));
        texturas.put("item/gancho", desenho(GANCHO, CORES_GANCHO));
        texturas.put("item/gancho_lancado", desenho(GANCHO_LANCADO, CORES_GANCHO));
        texturas.put("item/capa_planadora", desenho(CAPA, CORES_CAPA));
        texturas.put("item/ferradura", desenho(FERRADURA, CORES_FERRADURA));
        texturas.put("item/ninho_de_passaro", desenho(NINHO, CORES_NINHO));
        for (ArteItens.Arte a : ArteItens.todas()) texturas.putIfAbsent("item/" + a.id(), desenho(a.desenho(), a.cores()));
        File padrao = new File(pasta, "texturas-padrao"), proprias = new File(pasta, "texturas");
        if (!padrao.exists() && !padrao.mkdirs()) throw new IOException("não criei a pasta " + padrao);
        if (!proprias.exists() && proprias.mkdirs()) {
            Files.writeString(new File(proprias, "LEIA-ME.txt").toPath(), """
                    Texturas suas para o pacote de recursos do RPGAtributos.

                    Os desenhos originais ficam em ../texturas-padrao/ (refeitos a cada início).
                    Copie um deles para esta pasta, edite (Paint, Aseprite, Photoshop...) e reinicie o servidor:
                    o PNG daqui entra no lugar do original. Apague o arquivo para voltar ao original.

                    - Mantenha o mesmo nome (ex.: cajado_arcano.png) e uma imagem quadrada (16x16, 32x32, 64x64...).
                    - O pacote muda a cada edição: se ele estiver num link (Dropbox etc.), suba o zip novo de
                      ../RPGAtributos-recursos.zip por cima do antigo.
                    """, StandardCharsets.UTF_8);
        }
        for (Map.Entry<String, BufferedImage> t : texturas.entrySet()) {
            String nome = t.getKey().substring(t.getKey().indexOf('/') + 1) + ".png";
            byte[] original = png(t.getValue());
            Files.write(new File(padrao, nome).toPath(), original);
            f.put("assets/rpgatributos/textures/" + t.getKey() + ".png", textura(new File(proprias, nome), original));
        }
        f.put("assets/rpgatributos/font/menus.json", texto(fonteMenus()));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> en : f.entrySet()) {
                ZipEntry z = new ZipEntry(en.getKey());
                z.setTime(0); // o mesmo conteúdo gera sempre o mesmo arquivo (e o mesmo SHA-1)
                zip.putNextEntry(z);
                zip.write(en.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] texto(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String modeloCogumelo(int bits, String[] faces) {
        StringBuilder sb = new StringBuilder("{ \"textures\": { \"fora\": \"minecraft:block/brown_mushroom_block\", ")
                .append("\"dentro\": \"minecraft:block/mushroom_block_inside\", \"particle\": \"minecraft:block/brown_mushroom_block\" }, ")
                .append("\"elements\": [ { \"from\": [0, 0, 0], \"to\": [16, 16, 16], \"faces\": { ");
        for (int i = 0; i < 6; i++) {
            sb.append(i == 0 ? "" : ", ").append('"').append(faces[i]).append("\": { \"uv\": [0, 0, 16, 16], \"texture\": \"#")
                    .append((bits >> i & 1) == 1 ? "fora" : "dentro").append("\", \"cullface\": \"").append(faces[i]).append("\" }");
        }
        return sb.append(" } } ] }").toString();
    }

    /** Itens do jogo cujo modelo normal é o do bloco. */
    private static final Map<String, String> BLOCOS = Map.of("lodestone", "minecraft:block/lodestone");

    private static void caso(Map<String, List<String>> casos, String vanilla, String id) {
        casos.computeIfAbsent(vanilla, k -> new java.util.ArrayList<>()).add(id);
    }

    /** Definição do item do jogo: um caso por visual do plugin e, sem marca, o modelo normal. */
    private static String selecao(String vanilla, List<String> ids) {
        StringBuilder sb = new StringBuilder("{ \"model\": { \"type\": \"minecraft:select\", \"property\": \"minecraft:custom_model_data\", \"cases\": [ ");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append("{ \"when\": \"rpgatributos:").append(ids.get(i))
                    .append("\", \"model\": { \"type\": \"minecraft:model\", \"model\": \"rpgatributos:item/").append(ids.get(i)).append("\" } }");
        }
        // A poção do jogo pinta o líquido com a cor da poção; o modelo normal precisa manter isso.
        // O capim (usado pelas plantações da estação) é pintado com a cor da grama.
        String padrao = vanilla.equals("potion")
                ? "{ \"type\": \"minecraft:model\", \"model\": \"minecraft:item/potion\", \"tints\": [ { \"type\": \"minecraft:potion\", \"default\": -13083194 } ] }"
                : vanilla.equals("short_grass")
                ? "{ \"type\": \"minecraft:model\", \"model\": \"minecraft:item/short_grass\", \"tints\": [ { \"type\": \"minecraft:grass\", \"temperature\": 0.5, \"downfall\": 1.0 } ] }"
                : "{ \"type\": \"minecraft:model\", \"model\": \"" + BLOCOS.getOrDefault(vanilla, "minecraft:item/" + vanilla) + "\" }";
        return sb.append(" ], \"fallback\": ").append(padrao).append(" } }").toString();
    }

    /**
     * Arma com postura: primeiro olha a 2ª marca (postura) e depois a 1ª (lenda). Sem marca, o
     * modelo normal. Cada postura usa um modelo que só muda a posição na mão.
     */
    private static String selecaoPostura(String vanilla, List<String> lendas, List<String> pegadas) {
        StringBuilder sb = new StringBuilder("{ \"model\": { \"type\": \"minecraft:select\", \"property\": \"minecraft:custom_model_data\", \"index\": 1, \"cases\": [ ");
        for (int i = 0; i < pegadas.size(); i++) {
            String pg = pegadas.get(i);
            sb.append(i == 0 ? "" : ", ").append("{ \"when\": \"rpgatributos:postura_").append(pg).append("\", \"model\": ")
                    .append(porLenda(vanilla, lendas, pg)).append(" }");
        }
        return sb.append(" ], \"fallback\": ").append(porLenda(vanilla, lendas, null)).append(" } }").toString();
    }

    private static String porLenda(String vanilla, List<String> lendas, String pegada) {
        String padrao = "{ \"type\": \"minecraft:model\", \"model\": \""
                + (pegada == null ? "minecraft:item/" + vanilla : "rpgatributos:item/postura/" + vanilla + "_" + pegada) + "\" }";
        if (lendas.isEmpty()) return padrao;
        StringBuilder sb = new StringBuilder("{ \"type\": \"minecraft:select\", \"property\": \"minecraft:custom_model_data\", \"cases\": [ ");
        for (int i = 0; i < lendas.size(); i++) {
            String id = lendas.get(i);
            sb.append(i == 0 ? "" : ", ").append("{ \"when\": \"rpgatributos:").append(id).append("\", \"model\": { \"type\": \"minecraft:model\", \"model\": \"")
                    .append(pegada == null ? "rpgatributos:item/" + id : "rpgatributos:item/postura/" + id + "_" + pegada).append("\" } }");
        }
        return sb.append(" ], \"fallback\": ").append(padrao).append(" }").toString();
    }

    /**
     * Como a arma fica na mão em cada postura (rotação, posição e escala; a mão esquerda espelha).
     * {terceira pessoa: rot x,y,z, pos x,y,z; primeira pessoa: rot x,y,z, pos x,y,z}
     */
    private static final Map<String, double[]> PEGADAS = Map.of(
            "ofensiva", new double[]{5, 5, -23, 1, 1.75, 2, 0, -90, 0, 1.13, 4.2, 1.13},
            "defensiva", new double[]{-24.13, 26.67, 60.62, -2.5, 0.5, 2.5, 0, -150, 75, 0, 3.2, 0},
            "agil", new double[]{-171.04, 19, 2.93, 0.25, -5, 1, 0, -90, -160, 1.13, 2.5, 1.13},
            "duelista", new double[]{-142.19, -53.63, -87.98, 2.75, 0.25, -0.25, 0, -90, 80, 1.13, 3.6, 1.13},
            "carrasco", new double[]{-32.5, 5, 1, 0, 0.25, 1, 0, -90, 15, 1.13, 4.4, 1.13},
            "colosso", new double[]{15, 40, -141, 2.25, -3, -0.25, 0, -90, -30, 1.13, 4.0, 1.13});

    /** Tamanho da arma na mão em terceira pessoa (x, y, z) em cada postura. */
    private static final Map<String, double[]> ESCALAS_3P = Map.of(
            "ofensiva", new double[]{1, 1, 1},
            "defensiva", new double[]{1, 1, 1},
            "agil", new double[]{1, 1, 1},
            "duelista", new double[]{0.8, 0.86, 0.72},
            "carrasco", new double[]{1.38, 1.29, 1.52},
            "colosso", new double[]{1, 1, 1});

    private static String modeloPegada(String pai, String pegada) {
        double[] v = PEGADAS.get(pegada);
        double[] e = ESCALAS_3P.getOrDefault(pegada, new double[]{0.85, 0.85, 0.85});
        return "{ \"parent\": \"" + pai + "\", \"display\": { "
                + transf("thirdperson_righthand", v[0], v[1], v[2], v[3], v[4], v[5], e[0], e[1], e[2]) + ", "
                + transf("thirdperson_lefthand", v[0], -v[1], -v[2], v[3], v[4], v[5], e[0], e[1], e[2]) + ", "
                + transf("firstperson_righthand", v[6], v[7], v[8], v[9], v[10], v[11], 0.68, 0.68, 0.68) + ", "
                + transf("firstperson_lefthand", v[6], -v[7], -v[8], v[9], v[10], v[11], 0.68, 0.68, 0.68) + " } }";
    }

    private static String transf(String onde, double rx, double ry, double rz, double tx, double ty, double tz, double sx, double sy, double sz) {
        return String.format(java.util.Locale.ROOT, "\"%s\": { \"rotation\": [%.2f, %.2f, %.2f], \"translation\": [%.2f, %.2f, %.2f], \"scale\": [%.2f, %.2f, %.2f] }",
                onde, rx, ry, rz, tx, ty, tz, sx, sy, sz);
    }

    private static String itemGerado(String textura) {
        return itemComPai("minecraft:item/generated", textura);
    }

    private static String itemComPai(String pai, String textura) {
        return "{ \"parent\": \"" + pai + "\", \"textures\": { \"layer0\": \"" + textura + "\" } }";
    }

    /** A vara de pescar do jogo troca de modelo quando está lançada; o Gancho também. */
    private static String vara() {
        return "{ \"model\": { \"type\": \"minecraft:select\", \"property\": \"minecraft:custom_model_data\", \"cases\": [ "
                + "{ \"when\": \"rpgatributos:gancho\", \"model\": " + lancada("rpgatributos:item/gancho", "rpgatributos:item/gancho_lancado") + " } ], "
                + "\"fallback\": " + lancada("minecraft:item/fishing_rod", "minecraft:item/fishing_rod_cast") + " } }";
    }

    private static String lancada(String normal, String lancado) {
        return "{ \"type\": \"minecraft:condition\", \"property\": \"minecraft:fishing_rod/cast\", "
                + "\"on_false\": { \"type\": \"minecraft:model\", \"model\": \"" + normal + "\" }, "
                + "\"on_true\": { \"type\": \"minecraft:model\", \"model\": \"" + lancado + "\" } }";
    }

    /** Um PNG do admin, se existir e for uma imagem quadrada; senão, o desenho original. */
    private byte[] textura(File propria, byte[] original) {
        if (!propria.isFile()) return original;
        try {
            byte[] dados = Files.readAllBytes(propria.toPath());
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(dados));
            if (img == null || img.getWidth() != img.getHeight() || img.getWidth() < 16) {
                plugin.getLogger().warning("Textura " + propria.getName() + " ignorada: precisa ser um PNG quadrado (16x16, 32x32...).");
                return original;
            }
            plugin.getLogger().info("Pacote de recursos: usando a sua textura " + propria.getName() + ".");
            return dados;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui ler a textura " + propria.getName() + ": " + ex.getMessage());
            return original;
        }
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ---------- texturas (pixel art feita pelo código) ----------

    // Cada letra do desenho é um pixel; '.' é transparente.
    private static final int CONTORNO = 0xFF1E4A5C, ESCURO = 0xFF3A7F96, MEIO = 0xFF7FCFE0, BRILHO = 0xFFE6FFFF;
    private static final Map<Character, Integer> MITRILO = Map.of('o', CONTORNO, 'd', ESCURO, 'm', MEIO, 'h', BRILHO);
    private static final Map<Character, Integer> CORES_CAJADO = Map.of(
            'o', 0xFF241634,  // contorno
            'w', 0xFF5C3A1E,  // madeira escura
            'W', 0xFF8B5A2B,  // madeira clara
            'g', 0xFFE0B040,  // aro de ouro
            'c', 0xFF6A3FA0,  // ametista escura
            'C', 0xFFA070E0,  // ametista
            'h', 0xFFEBD8FF); // brilho
    private static final Map<Character, Integer> CORES_GANCHO = Map.of(
            'w', 0xFF6B4423,  // vara
            'W', 0xFF9C6B3A,  // vara (luz)
            'g', 0xFF3A2414,  // cabo
            'l', 0xFFDDDDDD,  // corda
            'o', 0xFF2A2A30,  // contorno do gancho
            'i', 0xFF7A7A86,  // ferro escuro
            'I', 0xFFD0D0DA); // ferro claro
    private static final Map<Character, Integer> CORES_CAPA = Map.of(
            'o', 0xFF22304A,  // contorno
            'd', 0xFF4A6FA5,  // dobras
            'm', 0xFF7FA6E0,  // tecido
            'h', 0xFFCFE2FF,  // luz
            'g', 0xFFE0B040); // fecho de ouro

    private static final Map<Character, Integer> CORES_FERRADURA = Map.of(
            'o', 0xFF2E2E36,  // contorno
            'd', 0xFF6E6E7A,  // ferro escuro
            'm', 0xFFA8A8B4,  // ferro
            'h', 0xFFE2E2EA,  // brilho
            'n', 0xFF1A1A1F); // furo do cravo
    private static final Map<Character, Integer> CORES_NINHO = Map.of(
            'o', 0xFF3B2412,  // contorno
            'd', 0xFF6B4423,  // galho escuro
            'm', 0xFF9C6B3A,  // galho
            'h', 0xFFC8955A,  // galho claro
            'e', 0xFFE8F4F8,  // ovo
            'b', 0xFF9CC9D8,  // sombra do ovo
            's', 0xFF5A7F8C); // pinta do ovo

    private static final String[] FERRADURA = {
            "................",
            "................",
            "...oo......oo...",
            "..ohdo....ohdo..",
            "..onmo....omno..",
            "..ohdo....ohdo..",
            "..omdo....omdo..",
            "..onmo....omno..",
            "..ohdo....ohdo..",
            "..omddo..oddmo..",
            "..ohmddooddmho..",
            "...ohmmddmmho...",
            "....oohhhhoo....",
            "......oooo......",
            "................",
            "................"};

    private static final String[] NINHO = {
            "................",
            "................",
            "................",
            "................",
            "........ee......",
            ".....es.eeb.....",
            "....eeebebb.....",
            "..oheeebbbbmho..",
            ".ohmdmhmdmhmdmo.",
            ".omhdmmhdmmhdmo.",
            "..odmhdmhdmhdo..",
            "...oddmmmmddo...",
            "....oooooooo....",
            "................",
            "................",
            "................"};

    private static final String[] CAJADO = {
            "............oo..",
            "...........ohCo.",
            "..........ohCCco",
            ".........ohCCcco",
            "..........oCcco.",
            ".........gocoo..",
            "........gWgo....",
            ".......oWwo.....",
            "......oWwo......",
            ".....oWwo.......",
            "....oWwo........",
            "...oWwo.........",
            "..oWwo..........",
            ".oWwo...........",
            ".owo............",
            "..o............."};

    private static final String[] GANCHO = {
            "................",
            "................",
            "............w...",
            "...........Wl...",
            "..........w.l...",
            ".........W..l...",
            "........w...l...",
            ".......W....l...",
            "......w....oIo..",
            ".....W.....oIo..",
            "....g....I.oIo.I",
            "...g.....Ii.I.iI",
            "..g.......IiIiI.",
            ".g.........III..",
            "g...............",
            "................"};

    private static final String[] GANCHO_LANCADO = {
            "................",
            "................",
            "............w...",
            "...........W....",
            "..........w.....",
            ".........W......",
            "........w.......",
            ".......W........",
            "......w.........",
            ".....W..........",
            "....g...........",
            "...g............",
            "..g.............",
            ".g..............",
            "g...............",
            "................"};

    private static final String[] CAPA = {
            "................",
            "................",
            "......oooo......",
            ".....ohggho.....",
            "....ohmmmmdo....",
            "...ohmmmmmmdo...",
            "...ohmmmmmmdo...",
            "..ohmmmmmmmmdo..",
            "..ohmmmdmmmmdo..",
            ".ohmmmmdmmmmmdo.",
            ".ohmmmmdmmmmmdo.",
            ".ohmmmdmmdmmmdo.",
            "ohmmmmdmmdmmmmdo",
            "odmmdmmdmmdmmdmo",
            "oo.ooo.oo.ooo.oo",
            "................"};

    private static final String[] BRUTO = {
            "................",
            "................",
            "......oooo......",
            "....oohhmmoo....",
            "...ohhmmmmmdo...",
            "..ohmmhmmmmddo..",
            "..ohmmmmmhmmdo..",
            ".ohmmmmmmmmmddo.",
            ".ommmhmmmmmmddo.",
            ".ommmmmmmhmdddo.",
            "..odmmmmmmdddo..",
            "..oddmmmddddoo..",
            "...ooddddddo....",
            ".....oooooo.....",
            "................",
            "................"};

    private static final String[] LINGOTE = {
            "................",
            "................",
            "................",
            "................",
            "......oooooo....",
            ".....ohhhhhmo...",
            "....ohhmmmmmdo..",
            "...ohmmmmmmmdo..",
            "..ommmmmmmmddo..",
            "..odmmmmmmddo...",
            "..oddddddddo....",
            "...oooooooo.....",
            "................",
            "................",
            "................",
            "................"};

    private static BufferedImage desenho(String[] linhas, Map<Character, Integer> cores) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) img.setRGB(x, y, cores.getOrDefault(linhas[y].charAt(x), 0));
        }
        return img;
    }

    /** Ardósia com veios de mitrilo azul-prateado. */
    private static BufferedImage minerio() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(4242);
        for (int y = 0; y < 16; y++) {
            int faixa = (y % 4 == 0) ? -10 : 0; // camadas da ardósia
            for (int x = 0; x < 16; x++) {
                int v = 58 + r.nextInt(16) + faixa;
                img.setRGB(x, y, 0xFF000000 | (v << 16) | (v << 8) | (v + 4));
            }
        }
        int[][] pedras = {{3, 3}, {10, 2}, {6, 8}, {12, 10}, {2, 12}, {9, 13}};
        for (int[] c : pedras) {
            img.setRGB(c[0], c[1], CONTORNO);
            img.setRGB(c[0] + 1, c[1], ESCURO);
            img.setRGB(c[0], c[1] + 1, MEIO);
            img.setRGB(c[0] + 1, c[1] + 1, BRILHO);
            if (c[0] + 2 < 16) img.setRGB(c[0] + 2, c[1] + 1, ESCURO);
        }
        return img;
    }
}
