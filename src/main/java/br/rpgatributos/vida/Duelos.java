package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.vida.CartasDuelo.Dom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Duelo de Cartas: três trilhas, 20 de vida para cada lado e energia que cresce a cada turno (até 6).
 * Você baixa cartas do seu baralho (12, montado com o que tem no álbum) e, ao passar o turno, cada
 * carta sua ataca a da frente; trilha vazia (ou carta que voa por cima) acerta o adversário. Dá para
 * duelar com os moradores das vilas (cada um com um baralho e uma dificuldade) e com jogadores
 * (/duelo). Só o jogo: ninguém perde ou troca cartas.
 */
public final class Duelos implements Listener, CommandExecutor {

    static final TextColor COR = TextColor.color(0xF2C14E);
    private static final int VIDA = 20, ENERGIA_MAX = 6, MAO_MAX = 6, TAMANHO_BARALHO = 12, TURNOS_MAX = 40;
    private static final NamespacedKey K_BARALHO = new NamespacedKey("rpgatributos", "baralho_duelo");
    private static final NamespacedKey K_VITORIA_DIA = new NamespacedKey("rpgatributos", "duelo_vitoria_dia");
    /** Baralho de treino (completa quem ainda tem poucas cartas). */
    private static final Carta[] TREINO = {Carta.ZUMBI, Carta.ZUMBI, Carta.ESQUELETO, Carta.ESQUELETO, Carta.ARANHA, Carta.ARANHA,
            Carta.TRACA, Carta.TRACA, Carta.CREEPER, Carta.CREEPER, Carta.SLIME, Carta.SLIME};

    // =====================================================================
    //  Estado de um duelo
    // =====================================================================

    /** Uma carta na mesa ou na mão. */
    static final class Peca {
        final Carta carta; // null = Lacaio
        final String nome;
        final org.bukkit.Material icone;
        final boolean brilhante;
        final int custo;
        int forca, vida;
        final int vidaMax;
        final Dom dom;
        boolean escudo, voltou, nova;
        int veneno;

        Peca(Carta c, boolean brilhante) {
            CartasDuelo.Status s = CartasDuelo.de(c);
            this.carta = c;
            this.nome = c.nome();
            this.icone = c.icone();
            this.brilhante = brilhante;
            this.custo = s.custo();
            this.forca = s.forca() + (brilhante ? 1 : 0);
            this.vida = s.vida() + (brilhante ? 1 : 0);
            this.vidaMax = vida;
            this.dom = s.dom();
            this.escudo = dom == Dom.ESCUDO;
        }

        /** O Lacaio que o Invocar cria. */
        Peca() {
            this.carta = null;
            this.nome = "Lacaio";
            this.icone = Material.BONE_MEAL;
            this.brilhante = false;
            this.custo = 0;
            this.forca = 1;
            this.vida = 1;
            this.vidaMax = 1;
            this.dom = Dom.NENHUM;
        }
    }

    /** Um dos lados: um jogador ou um morador (IA). */
    static final class Lado {
        final UUID jogador; // null = IA
        final String nome;
        final int dificuldade; // 0 fácil, 1 médio, 2 difícil (IA)
        int vida = VIDA, energia, energiaMax;
        final List<Peca> baralho = new ArrayList<>(), mao = new ArrayList<>();
        final Peca[] trilhas = new Peca[3];
        int selecionada = -1;

        Lado(UUID jogador, String nome, int dificuldade) {
            this.jogador = jogador;
            this.nome = nome;
            this.dificuldade = dificuldade;
        }

        Player player() { return jogador == null ? null : Bukkit.getPlayer(jogador); }
        boolean ia() { return jogador == null; }
    }

    final class Duelo implements InventoryHolder {
        final Lado a, b;
        Lado vez;
        int turno = 1;
        boolean acabou;
        final String npcChave; // morador da vila (null em duelo entre jogadores)
        final List<String> registro = new ArrayList<>();
        long ultimaAcao = System.currentTimeMillis();
        private final Map<UUID, Inventory> telas = new HashMap<>();

        Duelo(Lado a, Lado b, String npcChave) {
            this.a = a;
            this.b = b;
            this.npcChave = npcChave;
        }

        Lado outro(Lado l) { return l == a ? b : a; }

        Lado de(Player p) {
            return p.getUniqueId().equals(a.jogador) ? a : p.getUniqueId().equals(b.jogador) ? b : null;
        }

        @Override public Inventory getInventory() { return telas.values().stream().findFirst().orElse(null); }

        void log(String s) {
            registro.add(s);
            while (registro.size() > 6) registro.remove(0);
        }
    }

    private final RPGAtributos plugin;
    private final Map<UUID, Duelo> ativos = new HashMap<>();
    /** Convites: desafiado → desafiante (com a hora). */
    private final Map<UUID, UUID> convites = new HashMap<>();
    private final Map<UUID, Long> convitesHora = new HashMap<>();

    public Duelos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    public boolean emDuelo(Player p) {
        return ativos.containsKey(p.getUniqueId());
    }

    // =====================================================================
    //  Baralho
    // =====================================================================

    /** O baralho salvo (ids, com repetição). */
    List<Carta> baralhoSalvo(Player p) {
        List<Carta> l = new ArrayList<>();
        String s = p.getPersistentDataContainer().get(K_BARALHO, PersistentDataType.STRING);
        if (s == null || s.isBlank()) return l;
        for (String id : s.split(",")) {
            Carta c = Carta.porId(id.trim());
            if (c != null) l.add(c);
        }
        return l;
    }

    private void salvarBaralho(Player p, List<Carta> l) {
        List<String> ids = new ArrayList<>();
        for (Carta c : l) ids.add(c.id());
        p.getPersistentDataContainer().set(K_BARALHO, PersistentDataType.STRING, String.join(",", ids));
    }

    /** Quantas cópias dá para pôr no baralho (até 2, e não mais do que tem no álbum). */
    private int maxCopias(Player p, Carta c) {
        return Math.min(2, plugin.album().colecao(p).getOrDefault(c, 0));
    }

    /** O baralho que vai para a mesa: o salvo (só o que ainda tem) completado com o de treino. */
    private List<Peca> montarBaralho(Player p) {
        Map<Carta, Integer> usadas = new EnumMap<>(Carta.class);
        Set<String> brilho = plugin.album().brilhantes(p);
        List<Peca> l = new ArrayList<>();
        for (Carta c : baralhoSalvo(p)) {
            if (l.size() >= TAMANHO_BARALHO || usadas.getOrDefault(c, 0) >= maxCopias(p, c)) continue;
            usadas.merge(c, 1, Integer::sum);
            l.add(new Peca(c, brilho.contains(c.id()) && usadas.get(c) == 1));
        }
        int i = 0;
        while (l.size() < TAMANHO_BARALHO) l.add(new Peca(TREINO[i++ % TREINO.length], false));
        Collections.shuffle(l);
        return l;
    }

    private static List<Peca> baralhoNpc(String chave) {
        List<Carta> l = new ArrayList<>();
        Carta[] tema = switch (chave) {
            case "VELHO_PESCADOR" -> new Carta[]{Carta.CARPA_DOURADA, Carta.TRUTA_ARCO_IRIS, Carta.PEIXE_LUA, Carta.ENGUIA_ELETRICA, Carta.SALMAO_REI, Carta.AFOGADO, Carta.PEIXE_GELO, Carta.KOI_CELESTE};
            case "PEIXEIRA" -> new Carta[]{Carta.CARPA_DOURADA, Carta.TRUTA_ARCO_IRIS, Carta.PEIXE_LUA, Carta.PEIXE_PEDRA, Carta.ENGUIA_ELETRICA, Carta.AFOGADO};
            case "CARPINTEIRO_NAVAL" -> new Carta[]{Carta.PEIXE_PEDRA, Carta.SHULKER, Carta.GUARDIAO, Carta.ZUMBI_ALDEAO, Carta.PEIXE_LUA, Carta.ZUMBI};
            case "MENINA_DO_CAIS" -> new Carta[]{Carta.ZUMBI, Carta.ESQUELETO, Carta.ARANHA, Carta.CARPA_DOURADA, Carta.TRACA, Carta.SLIME};
            case "MESTRE" -> new Carta[]{Carta.VINGADOR, Carta.ENDERMAN, Carta.PIGLIN_BRUTO, Carta.BREEZE, Carta.ENDERMITE, Carta.PHANTOM, Carta.ZOGLIN, Carta.GUARDIAO_ANCIAO};
            case "HERBALISTA" -> new Carta[]{Carta.BRUXA, Carta.KOI_CELESTE, Carta.CARPA_DOURADA, Carta.ZUMBI_ALDEAO, Carta.SLIME, Carta.ARANHA};
            case "GUARDIAO_DO_SINO" -> new Carta[]{Carta.GUARDIAO, Carta.SHULKER, Carta.RANGEDOR, Carta.PEIXE_PEDRA, Carta.ZUMBI, Carta.HUSK};
            case "PEREGRINO" -> new Carta[]{Carta.PHANTOM, Carta.STRAY, Carta.BLAZE, Carta.HOGLIN, Carta.ENDERMAN, Carta.SAQUEADOR, Carta.BREEZE};
            case "MESTRE_MINEIRO" -> new Carta[]{Carta.TRACA, Carta.CREEPER, Carta.ARANHA_CAVERNA, Carta.SLIME, Carta.ESQUELETO, Carta.MAGMA};
            case "JOALHEIRO" -> new Carta[]{Carta.ENDERMAN, Carta.SHULKER, Carta.ENDERMITE, Carta.VEX, Carta.GUARDIAO};
            case "FERREIRO_ANAO" -> new Carta[]{Carta.BLAZE, Carta.GHAST, Carta.MAGMA, Carta.PIGLIN_BRUTO, Carta.HOGLIN, Carta.ZOGLIN, Carta.PIGLIN_ZUMBI, Carta.ESQUELETO_WITHER};
            case "TAVERNEIRO" -> new Carta[]{Carta.DEVASTADOR, Carta.INVOCADOR, Carta.PIGLIN_BRUTO, Carta.WARDEN, Carta.CHEFE_GOLEM, Carta.VINGADOR, Carta.GHAST, Carta.BRUXA};
            case "DRUIDA" -> new Carta[]{Carta.BRUXA, Carta.SLIME, Carta.KOI_CELESTE, Carta.CARPA_DOURADA, Carta.RANGEDOR, Carta.ARANHA};
            case "ARQUEIRA" -> new Carta[]{Carta.PHANTOM, Carta.VEX, Carta.BLAZE, Carta.ESQUELETO, Carta.VINGADOR, Carta.BREEZE};
            case "CURANDEIRA" -> new Carta[]{Carta.BRUXA, Carta.KOI_CELESTE, Carta.CARPA_DOURADA, Carta.GUARDIAO, Carta.SHULKER, Carta.ZUMBI_ALDEAO};
            case "TECELA" -> new Carta[]{Carta.PEIXE_LUA, Carta.PEIXE_PEDRA, Carta.SLIME, Carta.TRACA, Carta.ARANHA, Carta.MAGMA};
            default -> TREINO;
        };
        for (int i = 0; l.size() < TAMANHO_BARALHO; i++) l.add(tema[i % tema.length]);
        List<Peca> p = new ArrayList<>();
        for (Carta c : l) p.add(new Peca(c, false));
        Collections.shuffle(p);
        return p;
    }

    /** Dificuldade de cada morador: 0 fácil, 1 médio, 2 difícil. */
    private static int dificuldade(String chave) {
        return switch (chave) {
            case "MENINA_DO_CAIS", "PEIXEIRA", "CARPINTEIRO_NAVAL", "TECELA", "HERBALISTA" -> 0;
            case "MESTRE", "TAVERNEIRO", "FERREIRO_ANAO", "FERREIRO_PIGLIN" -> 2;
            default -> 1;
        };
    }

    public static String nomeDificuldade(String chave) {
        return new String[]{"Fácil", "Médio", "Difícil"}[dificuldade(chave)];
    }

    // =====================================================================
    //  Começar
    // =====================================================================

    /** Duelo contra um morador de vila (chamado pelo menu dele). */
    public void contraNpc(Player p, String chave, String nomeNpc) {
        if (emDuelo(p)) {
            abrir(p);
            return;
        }
        Lado eu = new Lado(p.getUniqueId(), p.getName(), 0);
        eu.baralho.addAll(montarBaralho(p));
        int dif = dificuldade(chave);
        Lado npc = new Lado(null, nomeNpc, dif);
        npc.baralho.addAll(baralhoNpc(chave));
        if (dif == 2) npc.vida = VIDA + 4;
        Duelo d = new Duelo(eu, npc, chave);
        ativos.put(p.getUniqueId(), d);
        iniciar(d);
    }

    private void contraJogador(Player desafiante, Player desafiado) {
        Lado a = new Lado(desafiante.getUniqueId(), desafiante.getName(), 0);
        a.baralho.addAll(montarBaralho(desafiante));
        Lado b = new Lado(desafiado.getUniqueId(), desafiado.getName(), 0);
        b.baralho.addAll(montarBaralho(desafiado));
        Duelo d = new Duelo(a, b, null);
        ativos.put(desafiante.getUniqueId(), d);
        ativos.put(desafiado.getUniqueId(), d);
        iniciar(d);
    }

    private void iniciar(Duelo d) {
        for (int i = 0; i < 3; i++) {
            comprar(d.a);
            comprar(d.b);
        }
        comprar(d.b); // quem joga depois começa com uma carta a mais
        d.vez = d.a;
        comecarTurno(d);
        for (Lado l : new Lado[]{d.a, d.b}) {
            Player p = l.player();
            if (p == null) continue;
            if (baralhoSalvo(p).size() < TAMANHO_BARALHO) {
                p.sendMessage(Component.text("🃏 Seu baralho tem menos de 12 cartas: completamos com cartas de treino. Monte o seu no /album → Baralho.", NamedTextColor.GRAY));
            }
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
        }
        desenhar(d);
    }

    private static void comprar(Lado l) {
        if (l.baralho.isEmpty() || l.mao.size() >= MAO_MAX) return;
        l.mao.add(l.baralho.remove(0));
    }

    private void comecarTurno(Duelo d) {
        Lado l = d.vez;
        l.energiaMax = Math.min(ENERGIA_MAX, l.energiaMax + 1);
        l.energia = l.energiaMax;
        l.selecionada = -1;
        comprar(l);
        // Veneno: cada carta envenenada perde 1 de vida no começo do turno do dono.
        for (int i = 0; i < 3; i++) {
            Peca c = l.trilhas[i];
            if (c == null || c.veneno <= 0) continue;
            c.veneno--;
            c.vida--;
            d.log("☠ " + c.nome + " de " + l.nome + " sofre com o veneno.");
            if (c.vida <= 0) cair(d, l, i);
        }
        d.ultimaAcao = System.currentTimeMillis();
        if (verificarFim(d)) return;
        if (l.ia()) Bukkit.getScheduler().runTaskLater(plugin, () -> turnoIa(d), 20L);
    }

    // =====================================================================
    //  Jogar e combater
    // =====================================================================

    /** Põe a carta da mão na trilha. @return o motivo de não poder, ou null. */
    private String jogar(Duelo d, Lado l, int indiceMao, int trilha) {
        if (indiceMao < 0 || indiceMao >= l.mao.size()) return "Escolha uma carta da mão.";
        if (l.trilhas[trilha] != null) return "Essa trilha já tem carta.";
        Peca c = l.mao.get(indiceMao);
        if (c.custo > l.energia) return "Energia insuficiente (custa " + c.custo + ").";
        l.energia -= c.custo;
        l.mao.remove(indiceMao);
        c.nova = true;
        l.trilhas[trilha] = c;
        d.log("▶ " + l.nome + " baixou " + c.nome + ".");
        if (c.dom == Dom.INVOCAR) {
            for (int i = 0; i < 3; i++) {
                if (l.trilhas[i] == null) {
                    Peca lacaio = new Peca();
                    lacaio.nova = true;
                    l.trilhas[i] = lacaio;
                    d.log("  " + c.nome + " chamou um Lacaio.");
                    break;
                }
            }
        }
        d.ultimaAcao = System.currentTimeMillis();
        return null;
    }

    /** Fim do turno: as cartas de quem jogou atacam, depois cura, e passa a vez. */
    private void passar(Duelo d) {
        Lado l = d.vez, o = d.outro(l);
        for (int i = 0; i < 3; i++) {
            Peca c = l.trilhas[i];
            if (c == null || c.dom == Dom.MURALHA || c.forca <= 0) continue;
            if (c.nova && c.dom != Dom.RAPIDO) continue;
            Peca alvo = o.trilhas[i];
            if (alvo == null || (c.dom == Dom.VOO && alvo.dom != Dom.VOO)) {
                o.vida -= c.forca;
                d.log("⚔ " + c.nome + " acerta " + o.nome + " (−" + c.forca + ").");
                if (c.dom == Dom.ROUBO) l.vida = Math.min(VIDA + 10, l.vida + c.forca);
            } else {
                ferir(d, o, i, c.forca, c);
                if (c.dom == Dom.ROUBO) l.vida = Math.min(VIDA + 10, l.vida + c.forca);
            }
        }
        for (Peca c : l.trilhas) {
            if (c == null) continue;
            c.nova = false;
            if (c.dom == Dom.CURA) for (Peca x : l.trilhas) if (x != null) x.vida = Math.min(x.vidaMax, x.vida + 1);
        }
        if (verificarFim(d)) return;
        d.turno++;
        if (d.turno > TURNOS_MAX) {
            terminar(d, d.a.vida == d.b.vida ? null : d.a.vida > d.b.vida ? d.a : d.b, "o tempo acabou");
            return;
        }
        d.vez = o;
        comecarTurno(d);
        desenhar(d);
    }

    private void ferir(Duelo d, Lado dono, int trilha, int dano, Peca atacante) {
        Peca c = dono.trilhas[trilha];
        if (c == null) return;
        if (c.escudo) {
            c.escudo = false;
            d.log("🛡 O escudo de " + c.nome + " segura o golpe.");
            return;
        }
        c.vida -= dano;
        d.log("⚔ " + atacante.nome + " acerta " + c.nome + " (−" + dano + ").");
        if (c.dom == Dom.FURIA && c.vida > 0) c.forca++;
        if (atacante.dom == Dom.VENENO && c.vida > 0) c.veneno = 2;
        if (c.vida <= 0) cair(d, dono, trilha);
    }

    private void cair(Duelo d, Lado dono, int trilha) {
        Peca c = dono.trilhas[trilha];
        if (c == null) return;
        if (c.dom == Dom.TEIMOSO && !c.voltou) {
            c.voltou = true;
            c.vida = 1;
            c.veneno = 0;
            d.log("↺ " + c.nome + " se levanta de novo!");
            return;
        }
        dono.trilhas[trilha] = null;
        d.log("✖ " + c.nome + " de " + dono.nome + " caiu.");
        if (c.dom == Dom.EXPLOSAO) {
            Lado o = d.outro(dono);
            if (o.trilhas[trilha] != null) {
                d.log("💥 " + c.nome + " explode!");
                ferir(d, o, trilha, 2, c);
            }
        }
    }

    private boolean verificarFim(Duelo d) {
        if (d.acabou) return true;
        if (d.a.vida <= 0 || d.b.vida <= 0) {
            terminar(d, d.a.vida <= 0 && d.b.vida <= 0 ? null : d.a.vida > 0 ? d.a : d.b, null);
            return true;
        }
        return false;
    }

    // =====================================================================
    //  A IA dos moradores
    // =====================================================================

    private void turnoIa(Duelo d) {
        if (d.acabou || !d.vez.ia()) return;
        Lado ia = d.vez, o = d.outro(ia);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean jogou = true;
        while (jogou) {
            jogou = false;
            // Fácil: carta qualquer que caiba; médio e difícil: a mais cara que caiba.
            List<Integer> ordem = new ArrayList<>();
            for (int i = 0; i < ia.mao.size(); i++) ordem.add(i);
            if (ia.dificuldade == 0) Collections.shuffle(ordem);
            else ordem.sort((x, y) -> ia.mao.get(y).custo - ia.mao.get(x).custo);
            for (int idx : ordem) {
                Peca c = ia.mao.get(idx);
                if (c.custo > ia.energia) continue;
                int trilha = escolherTrilha(ia, o, c, r);
                if (trilha < 0) break;
                jogar(d, ia, idx, trilha);
                jogou = true;
                break;
            }
        }
        passar(d);
    }

    private static int escolherTrilha(Lado ia, Lado o, Peca c, ThreadLocalRandom r) {
        int melhor = -1;
        double nota = -999;
        for (int i = 0; i < 3; i++) {
            if (ia.trilhas[i] != null) continue;
            Peca frente = o.trilhas[i];
            double n;
            if (ia.dificuldade == 0) n = r.nextDouble();
            else if (frente == null) n = c.dom == Dom.MURALHA ? 0 : 5 + c.forca;           // dano direto
            else if (c.forca >= frente.vida && !frente.escudo) n = 10 + frente.forca;       // derruba a carta da frente
            else n = 3 + frente.forca - c.vida * 0.2 + (c.dom == Dom.MURALHA ? 4 : 0);    // segura a carta forte
            if (n > nota) {
                nota = n;
                melhor = i;
            }
        }
        return melhor;
    }

    // =====================================================================
    //  Fim e prêmios
    // =====================================================================

    private void terminar(Duelo d, Lado vencedor, String motivo) {
        if (d.acabou) return;
        d.acabou = true;
        for (Lado l : new Lado[]{d.a, d.b}) {
            Player p = l.player();
            if (p == null) continue;
            ativos.remove(p.getUniqueId());
            if (p.getOpenInventory().getTopInventory().getHolder() == d) Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
            boolean ganhou = l == vencedor;
            Component titulo = vencedor == null ? Component.text("Empate", NamedTextColor.GRAY, TextDecoration.BOLD)
                    : ganhou ? Component.text("Vitória!", NamedTextColor.GOLD, TextDecoration.BOLD) : Component.text("Derrota", NamedTextColor.RED, TextDecoration.BOLD);
            p.showTitle(net.kyori.adventure.title.Title.title(titulo, Component.text(d.outro(l).nome + (motivo != null ? " · " + motivo : ""), NamedTextColor.GRAY)));
            p.playSound(p.getLocation(), ganhou ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            if (!ganhou) continue;
            if (d.npcChave != null) premiarNpc(p, d.npcChave, dificuldade(d.npcChave));
            else {
                plugin.titulos().registrar(p, "duelos_pvp", 1);
                p.sendMessage(Component.text("🃏 Você venceu o duelo contra " + d.outro(l).nome + "!", COR));
            }
            plugin.titulos().registrar(p, "duelos_vencidos", 1);
            plugin.diario().marco(p, "primeiro_duelo", "Venceu o primeiro Duelo de Cartas");
        }
    }

    private void premiarNpc(Player p, String chave, int dif) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> premio = new ArrayList<>();
        premio.add(new ItemStack(Material.EMERALD, 2 + dif * 2 + r.nextInt(3)));
        // A primeira vitória do dia contra cada morador dá um Pacote de Cartas garantido.
        String marca = LocalDate.now().toEpochDay() + ":" + chave;
        String feitas = p.getPersistentDataContainer().getOrDefault(K_VITORIA_DIA, PersistentDataType.STRING, "");
        boolean primeira = !java.util.Arrays.asList(feitas.split(";")).contains(marca);
        if (primeira) {
            String hoje = String.valueOf(LocalDate.now().toEpochDay());
            List<String> manter = new ArrayList<>();
            for (String x : feitas.split(";")) if (x.startsWith(hoje + ":")) manter.add(x);
            manter.add(marca);
            p.getPersistentDataContainer().set(K_VITORIA_DIA, PersistentDataType.STRING, String.join(";", manter));
        }
        if (primeira || r.nextDouble() < 0.3) premio.add(Album.pacote(1));
        for (ItemStack i : premio) for (ItemStack sobra : p.getInventory().addItem(i).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        p.sendMessage(Component.text("🃏 Vitória! Você ganhou " + premio.get(0).getAmount() + " esmeraldas"
                + (premio.size() > 1 ? " e um Pacote de Cartas" : "") + (primeira ? " (primeira vitória do dia contra este morador)." : "."), COR));
        if (dif == 2) plugin.titulos().registrar(p, "duelos_dificeis", 1);
    }

    // =====================================================================
    //  A mesa (menu)
    // =====================================================================

    private static final int S_ADV = 0, S_REGISTRO = 4, S_DESISTIR = 8, S_EU = 36, S_PASSAR = 44, S_ENERGIA = 52, S_BARALHO = 53;
    private static final int[] S_TRILHA_ADV = {11, 13, 15}, S_TRILHA_EU = {29, 31, 33}, S_SETAS = {20, 22, 24};
    private static final int[] S_MAO = {45, 46, 47, 48, 49, 50};

    private void desenhar(Duelo d) {
        for (Lado l : new Lado[]{d.a, d.b}) {
            Player p = l.player();
            if (p == null || !p.isOnline()) continue;
            Inventory inv = d.telas.computeIfAbsent(p.getUniqueId(),
                    k -> Bukkit.createInventory(d, 54, Component.text("🃏 Duelo contra " + d.outro(l).nome)));
            preencher(d, l, inv);
            if (p.getOpenInventory().getTopInventory() != inv) p.openInventory(inv);
        }
    }

    public void abrir(Player p) {
        Duelo d = ativos.get(p.getUniqueId());
        if (d == null) {
            p.sendMessage(Component.text("Você não está num duelo.", NamedTextColor.GRAY));
            return;
        }
        desenhar(d);
    }

    private void preencher(Duelo d, Lado eu, Inventory inv) {
        Lado o = d.outro(eu);
        boolean minhaVez = d.vez == eu;
        ItemStack vidro = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
        for (int i = 0; i < 54; i++) inv.setItem(i, vidro);
        inv.setItem(S_ADV, item(Material.SKELETON_SKULL, Component.text(o.nome, NamedTextColor.RED, TextDecoration.BOLD), List.of(
                Component.text("❤ " + o.vida + "   ⚡ " + o.energia + "/" + o.energiaMax, NamedTextColor.WHITE),
                Component.text("Mão: " + o.mao.size() + " · Baralho: " + o.baralho.size(), NamedTextColor.GRAY)), false));
        List<Component> reg = new ArrayList<>();
        for (String s : d.registro) reg.add(Component.text(s, NamedTextColor.GRAY));
        inv.setItem(S_REGISTRO, item(Material.WRITABLE_BOOK, Component.text("Turno " + d.turno + " · " + (minhaVez ? "sua vez" : "vez de " + o.nome),
                minhaVez ? NamedTextColor.GREEN : NamedTextColor.YELLOW, TextDecoration.BOLD), reg, false));
        inv.setItem(S_DESISTIR, item(Material.WHITE_BANNER, Component.text("Desistir", NamedTextColor.RED), List.of(
                Component.text("Shift + clique para desistir.", NamedTextColor.DARK_GRAY)), false));
        for (int i = 0; i < 3; i++) {
            inv.setItem(S_TRILHA_ADV[i], icone(o.trilhas[i], false, false));
            inv.setItem(S_TRILHA_EU[i], eu.trilhas[i] == null && minhaVez && eu.selecionada >= 0
                    ? item(Material.LIME_STAINED_GLASS_PANE, Component.text("» Baixar aqui", NamedTextColor.GREEN), List.of(), false)
                    : icone(eu.trilhas[i], false, false));
            inv.setItem(S_SETAS[i], item(Material.IRON_BARS, Component.text("Trilha " + (i + 1), NamedTextColor.GRAY), List.of(
                    Component.text("Cada carta ataca a da frente quando o", NamedTextColor.DARK_GRAY),
                    Component.text("dono passa o turno. Vazia: golpe direto.", NamedTextColor.DARK_GRAY)), false));
        }
        inv.setItem(S_EU, item(Material.PLAYER_HEAD, Component.text(eu.nome, NamedTextColor.AQUA, TextDecoration.BOLD), List.of(
                Component.text("❤ " + eu.vida + "   ⚡ " + eu.energia + "/" + eu.energiaMax, NamedTextColor.WHITE)), false));
        inv.setItem(S_PASSAR, minhaVez
                ? item(Material.LIME_CONCRETE, Component.text("Passar o turno", NamedTextColor.GREEN, TextDecoration.BOLD), List.of(
                        Component.text("Suas cartas atacam e a vez passa.", NamedTextColor.GRAY)), true)
                : item(Material.GRAY_CONCRETE, Component.text("Esperando " + o.nome + "...", NamedTextColor.GRAY), List.of(), false));
        for (int i = 0; i < S_MAO.length; i++) {
            if (i >= eu.mao.size()) continue;
            Peca c = eu.mao.get(i);
            inv.setItem(S_MAO[i], icone(c, true, i == eu.selecionada));
        }
        inv.setItem(S_ENERGIA, item(Material.GLOWSTONE_DUST, Component.text("⚡ Energia " + eu.energia + "/" + eu.energiaMax, NamedTextColor.YELLOW, TextDecoration.BOLD), List.of(
                Component.text("Cresce 1 por turno (até " + ENERGIA_MAX + ").", NamedTextColor.GRAY)), false));
        inv.setItem(S_BARALHO, item(Material.BOOK, Component.text("Baralho: " + eu.baralho.size(), NamedTextColor.WHITE), List.of(
                Component.text("Você compra 1 carta por turno.", NamedTextColor.GRAY)), false));
    }

    private static ItemStack icone(Peca c, boolean naMao, boolean selecionada) {
        if (c == null) return item(Material.GRAY_STAINED_GLASS_PANE, Component.text("Trilha vazia", NamedTextColor.DARK_GRAY), List.of(), false);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("⚡" + c.custo + "  ⚔" + c.forca + "  ❤" + c.vida + (naMao ? "" : "/" + c.vidaMax), NamedTextColor.WHITE));
        if (c.dom != Dom.NENHUM) lore.add(Component.text(c.dom.nome + ": " + c.dom.descricao, NamedTextColor.GRAY));
        if (c.escudo) lore.add(Component.text("🛡 Escudo ativo", NamedTextColor.AQUA));
        if (c.veneno > 0) lore.add(Component.text("☠ Envenenada", NamedTextColor.DARK_GREEN));
        if (c.nova && c.dom != Dom.RAPIDO && !naMao) lore.add(Component.text("Acabou de entrar: ataca no próximo turno.", NamedTextColor.DARK_GRAY));
        if (naMao) lore.add(Component.text(selecionada ? "● Escolhida: clique numa trilha sua vazia" : "» Clique para escolher", selecionada ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        ItemStack i = item(c.icone, Component.text((c.brilhante ? "✦ " : "") + c.nome + "  ⚔" + c.forca + " ❤" + c.vida,
                c.brilhante ? NamedTextColor.GOLD : NamedTextColor.WHITE, TextDecoration.BOLD), lore, selecionada);
        if (c.carta != null) i = comoCarta(i, c.carta, c.brilhante);
        i.setAmount(Math.max(1, Math.min(64, c.vida)));
        return i;
    }

    /**
     * Mostra o ícone como a própria carta (com o pacote de recursos: a moldura da raridade e o
     * desenho do bicho). Sem o pacote, aparece como papel.
     */
    private static ItemStack comoCarta(ItemStack i, Carta c, boolean brilhante) {
        ItemStack r = i.withType(Material.PAPER);
        r.editMeta(m -> br.rpgatributos.exploracao.PacoteRecursos.marcar(m, Album.visual(c, brilhante)));
        return r;
    }

    private static ItemStack item(Material m, Component nome, List<Component> lore, boolean brilho) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.itemName(nome);
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            if (brilho) meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @EventHandler
    public void aoArrastar(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Duelo || e.getInventory().getHolder() instanceof TelaBaralho) e.setCancelled(true);
    }

    @EventHandler
    public void aoClicar(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof TelaBaralho t) {
            cliqueBaralho(e, t);
            return;
        }
        if (!(e.getInventory().getHolder() instanceof Duelo d) || !(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory() || d.acabou) return;
        Lado eu = d.de(p);
        if (eu == null) return;
        int slot = e.getRawSlot();
        if (slot == S_DESISTIR && e.isShiftClick()) {
            terminar(d, d.outro(eu), eu.nome + " desistiu");
            return;
        }
        if (d.vez != eu) {
            p.sendActionBar(Component.text("Espere a sua vez.", NamedTextColor.GRAY));
            return;
        }
        for (int i = 0; i < S_MAO.length; i++) {
            if (S_MAO[i] != slot || i >= eu.mao.size()) continue;
            eu.selecionada = eu.selecionada == i ? -1 : i;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            desenhar(d);
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (S_TRILHA_EU[i] != slot) continue;
            if (eu.selecionada < 0) {
                p.sendActionBar(Component.text("Escolha primeiro uma carta da mão (embaixo).", NamedTextColor.GRAY));
                return;
            }
            String erro = jogar(d, eu, eu.selecionada, i);
            if (erro != null) {
                p.sendActionBar(Component.text("✖ " + erro, NamedTextColor.RED));
                return;
            }
            eu.selecionada = -1;
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PUT, 1f, 1.2f);
            desenhar(d);
            return;
        }
        if (slot == S_PASSAR) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
            passar(d);
            if (!d.acabou) desenhar(d);
        }
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        Duelo d = ativos.get(e.getPlayer().getUniqueId());
        if (d != null && !d.acabou) {
            Lado l = d.de(e.getPlayer());
            terminar(d, l == null ? null : d.outro(l), e.getPlayer().getName() + " saiu do jogo");
        }
        convites.remove(e.getPlayer().getUniqueId());
    }

    /** A cada 5 s: quem demora mais de 90 s no turno (duelo entre jogadores) passa a vez sozinho. */
    public void tick() {
        long agora = System.currentTimeMillis();
        for (Duelo d : new ArrayList<>(new java.util.HashSet<>(ativos.values()))) {
            if (d.acabou || d.vez.ia() || agora - d.ultimaAcao < 90_000) continue;
            Player p = d.vez.player();
            if (p != null) p.sendMessage(Component.text("⌛ Tempo esgotado: o turno passou.", NamedTextColor.GRAY));
            passar(d);
            if (!d.acabou) desenhar(d);
        }
        convitesHora.entrySet().removeIf(en -> {
            if (agora - en.getValue() <= 60_000) return false;
            convites.remove(en.getKey());
            return true;
        });
    }

    // =====================================================================
    //  /duelo <jogador> | aceitar | abrir | desistir
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        if (args.length == 0) {
            if (emDuelo(p)) abrir(p);
            else p.sendMessage(Component.text("Use: /duelo <jogador>, /duelo aceitar, /duelo abrir ou /duelo desistir. Para duelar com moradores, abra o menu deles nas vilas.", NamedTextColor.GRAY));
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "abrir" -> abrir(p);
            case "desistir" -> {
                Duelo d = ativos.get(p.getUniqueId());
                if (d == null) p.sendMessage(Component.text("Você não está num duelo.", NamedTextColor.GRAY));
                else terminar(d, d.outro(d.de(p)), p.getName() + " desistiu");
            }
            case "aceitar" -> {
                UUID de = convites.remove(p.getUniqueId());
                Player desafiante = de == null ? null : Bukkit.getPlayer(de);
                if (desafiante == null) {
                    p.sendMessage(Component.text("Nenhum desafio para aceitar (ou ele expirou).", NamedTextColor.GRAY));
                } else if (emDuelo(p) || emDuelo(desafiante)) {
                    p.sendMessage(Component.text("Um de vocês já está num duelo.", NamedTextColor.RED));
                } else {
                    contraJogador(desafiante, p);
                }
            }
            default -> {
                Player alvo = Bukkit.getPlayerExact(args[0]);
                if (alvo == null || alvo.equals(p)) {
                    p.sendMessage(Component.text("Jogador não encontrado.", NamedTextColor.RED));
                    return true;
                }
                if (emDuelo(p) || emDuelo(alvo)) {
                    p.sendMessage(Component.text("Um de vocês já está num duelo.", NamedTextColor.RED));
                    return true;
                }
                convites.put(alvo.getUniqueId(), p.getUniqueId());
                convitesHora.put(alvo.getUniqueId(), System.currentTimeMillis());
                p.sendMessage(Component.text("🃏 Desafio enviado para " + alvo.getName() + ".", COR));
                alvo.sendMessage(Component.text("🃏 " + p.getName() + " te desafiou para um Duelo de Cartas! ", COR)
                        .append(Component.text("[Aceitar]", NamedTextColor.GREEN, TextDecoration.BOLD).clickEvent(ClickEvent.runCommand("/duelo aceitar")))
                        .append(Component.text("  (ou /duelo aceitar, 60 s)", NamedTextColor.GRAY)));
            }
        }
        return true;
    }

    // =====================================================================
    //  Montar o baralho (12 cartas do álbum, até 2 de cada)
    // =====================================================================

    private static final class TelaBaralho implements InventoryHolder {
        Inventory inv;
        int folha;
        final Map<Integer, Carta> colecao = new HashMap<>();
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int B_POR_FOLHA = 27;
    private static final int[] B_BARALHO = {27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38};

    public void abrirBaralho(Player p, int folha) {
        TelaBaralho t = new TelaBaralho();
        t.folha = folha;
        List<Carta> baralho = baralhoSalvo(p);
        t.inv = Bukkit.createInventory(t, 54, Component.text("🃏 Baralho · " + baralho.size() + "/" + TAMANHO_BARALHO));
        Map<Carta, Integer> col = plugin.album().colecao(p);
        List<Carta> tem = new ArrayList<>();
        for (Carta c : Carta.values()) if (col.getOrDefault(c, 0) > 0) tem.add(c);
        int inicio = folha * B_POR_FOLHA;
        for (int i = 0; i < B_POR_FOLHA && inicio + i < tem.size(); i++) {
            Carta c = tem.get(inicio + i);
            int noBaralho = Collections.frequency(baralho, c), max = maxCopias(p, c);
            CartasDuelo.Status s = CartasDuelo.de(c);
            List<Component> lore = new ArrayList<>(List.of(
                    Component.text(CartasDuelo.resumo(c, false), NamedTextColor.WHITE),
                    Component.text("No baralho: " + noBaralho + "/" + max, noBaralho > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            if (s.dom() != Dom.NENHUM) lore.add(Component.text(s.dom().descricao, NamedTextColor.DARK_GRAY));
            lore.add(Component.text(noBaralho < max && baralho.size() < TAMANHO_BARALHO ? "» Clique para pôr no baralho" : "Limite atingido", NamedTextColor.YELLOW));
            t.inv.setItem(i, comoCarta(item(c.icone(), Component.text(c.nome(), Album.CORES[c.raridade()], TextDecoration.BOLD), lore, noBaralho > 0), c, false));
            t.colecao.put(i, c);
        }
        ItemStack vidro = item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.text(" "), List.of(), false);
        for (int i = 39; i < 54; i++) t.inv.setItem(i, vidro);
        for (int i = 0; i < B_BARALHO.length; i++) {
            if (i < baralho.size()) {
                Carta c = baralho.get(i);
                t.inv.setItem(B_BARALHO[i], comoCarta(item(c.icone(), Component.text(c.nome(), Album.CORES[c.raridade()]), List.of(
                        Component.text(CartasDuelo.resumo(c, false), NamedTextColor.WHITE),
                        Component.text("» Clique para tirar", NamedTextColor.YELLOW)), false), c, false));
            } else {
                t.inv.setItem(B_BARALHO[i], item(Material.GRAY_STAINED_GLASS_PANE, Component.text("Espaço vazio", NamedTextColor.DARK_GRAY), List.of(), false));
            }
        }
        t.inv.setItem(45, item(Material.ARROW, Component.text("« Álbum", NamedTextColor.YELLOW), List.of(), false));
        if (folha > 0) t.inv.setItem(48, item(Material.ARROW, Component.text("« Anterior", NamedTextColor.YELLOW), List.of(), false));
        if (inicio + B_POR_FOLHA < tem.size()) t.inv.setItem(50, item(Material.ARROW, Component.text("Próxima »", NamedTextColor.YELLOW), List.of(), false));
        t.inv.setItem(49, item(Material.WRITABLE_BOOK, Component.text("Baralho: " + baralho.size() + "/" + TAMANHO_BARALHO, COR, TextDecoration.BOLD), List.of(
                Component.text("Em cima: as cartas do seu álbum.", NamedTextColor.GRAY),
                Component.text("No meio: o baralho (até 2 de cada).", NamedTextColor.GRAY),
                Component.text("Faltando cartas, o duelo completa", NamedTextColor.GRAY),
                Component.text("com cartas de treino.", NamedTextColor.GRAY)), false));
        t.inv.setItem(53, item(Material.LAVA_BUCKET, Component.text("Esvaziar o baralho", NamedTextColor.RED), List.of(
                Component.text("Shift + clique.", NamedTextColor.DARK_GRAY)), false));
        p.openInventory(t.inv);
    }

    private void cliqueBaralho(InventoryClickEvent e, TelaBaralho t) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        List<Carta> baralho = baralhoSalvo(p);
        if (slot == 45) { plugin.album().abrir(p, Carta.Pagina.MORTOS_VIVOS); return; }
        if (slot == 48 && t.folha > 0) { abrirBaralho(p, t.folha - 1); return; }
        if (slot == 50) { abrirBaralho(p, t.folha + 1); return; }
        if (slot == 53 && e.isShiftClick()) {
            salvarBaralho(p, new ArrayList<>());
            abrirBaralho(p, t.folha);
            return;
        }
        for (int i = 0; i < B_BARALHO.length; i++) {
            if (B_BARALHO[i] != slot || i >= baralho.size()) continue;
            baralho.remove(i);
            salvarBaralho(p, baralho);
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 0.8f);
            abrirBaralho(p, t.folha);
            return;
        }
        Carta c = t.colecao.get(slot);
        if (c == null) return;
        if (baralho.size() >= TAMANHO_BARALHO) {
            p.sendActionBar(Component.text("O baralho já tem 12 cartas.", NamedTextColor.RED));
            return;
        }
        if (Collections.frequency(baralho, c) >= maxCopias(p, c)) {
            p.sendActionBar(Component.text("Limite: até 2 de cada (e só o que você tem no álbum).", NamedTextColor.RED));
            return;
        }
        baralho.add(c);
        salvarBaralho(p, baralho);
        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PUT, 0.8f, 1.2f);
        abrirBaralho(p, t.folha);
    }
}
