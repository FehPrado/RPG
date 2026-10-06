package br.rpgatributos;

import br.rpgatributos.alquimia.Acessorio;
import br.rpgatributos.alquimia.Elixir;
import br.rpgatributos.alquimia.Gema;
import br.rpgatributos.alquimia.Reagente;
import br.rpgatributos.arcano.Arcano;
import br.rpgatributos.arcano.Catalogo;
import br.rpgatributos.arcano.ParteCorpo;
import br.rpgatributos.arcano.Receita;
import br.rpgatributos.aventura.Chefe;
import br.rpgatributos.aventura.Invocacao;
import br.rpgatributos.aventura.Raro;
import br.rpgatributos.classe.Classe;
import br.rpgatributos.lenda.Lenda;
import br.rpgatributos.domador.Invocavel;
import br.rpgatributos.masmorra.Dificuldade;
import br.rpgatributos.masmorra.Masmorra;
import br.rpgatributos.masmorra.SalasPersonalizadas;
import br.rpgatributos.masmorra.TipoSala;
import br.rpgatributos.fazenda.Prato;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.fazenda.Variedade;
import br.rpgatributos.forja.DadosForja;
import br.rpgatributos.forja.Categoria;
import br.rpgatributos.forja.Raridade;
import br.rpgatributos.pesca.PeixeRaro;
import br.rpgatributos.territorio.Territorio;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Todos os comandos do plugin: /atributos, /rpgadmin, /chapeu, /tag, /receitas etc. */
public final class Comandos implements TabExecutor {

    private final RPGAtributos plugin;

    public Comandos(RPGAtributos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "atributos" -> atributos(sender, args);
            case "rpgadmin" -> admin(sender, args);
            case "chapeu" -> chapeu(sender, args);
            case "tag" -> tag(sender, args);
            case "cosmeticos" -> {
                if (sender instanceof Player p) plugin.menus().abrirCosmeticos(p);
                else erro(sender, "Só jogadores.");
            }
            case "forja" -> {
                if (sender instanceof Player p) plugin.menus().abrirForja(p);
                else erro(sender, "Só jogadores.");
            }
            case "grimorio" -> {
                if (sender instanceof Player p) plugin.arcano().menus().abrirGrimorio(p);
                else erro(sender, "Só jogadores.");
            }
            case "guia" -> {
                if (sender instanceof Player p) plugin.ajuda().comandoGuia(p, args);
                else erro(sender, "Só jogadores.");
            }
            case "jornada" -> {
                if (sender instanceof Player p) plugin.ajuda().abrirJornada(p);
                else erro(sender, "Só jogadores.");
            }
            case "titulos" -> {
                if (sender instanceof Player p) plugin.titulos().abrirMenu(p);
                else erro(sender, "Só jogadores.");
            }
            case "missoes" -> {
                if (sender instanceof Player p) plugin.missoes().abrirLista(p);
                else erro(sender, "Só jogadores.");
            }
            case "receitas" -> {
                if (sender instanceof Player p) plugin.cozinha().abrirReceitas(p, null);
                else erro(sender, "Só jogadores.");
            }
            case "pets" -> {
                if (sender instanceof Player p) plugin.menusDomador().abrirLista(p);
                else erro(sender, "Só jogadores.");
            }
            case "peixes" -> {
                if (sender instanceof Player p) plugin.pesca().abrirDiario(p);
                else erro(sender, "Só jogadores.");
            }
            case "alquimia" -> {
                if (sender instanceof Player p) plugin.alquimia().abrirLivro(p);
                else erro(sender, "Só jogadores.");
            }
            case "acessorios" -> {
                if (sender instanceof Player p) plugin.acessorios().abrir(p);
                else erro(sender, "Só jogadores.");
            }
            case "guardaroupa" -> {
                if (sender instanceof Player p) plugin.guardaRoupa().abrir(p);
                else erro(sender, "Só jogadores.");
            }
            case "calendario" -> {
                if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return true; }
                var est = plugin.estacoes();
                var e = est.atual();
                p.sendMessage(Component.text(e.icone() + " " + e.nome() + " — dia " + est.dia() + " de 7", e.cor(), TextDecoration.BOLD));
                p.sendMessage(Component.text("   " + e.descricao(), NamedTextColor.GRAY));
                if (est.festival()) p.sendMessage(Component.text("   ✦ Hoje é dia de festival!", NamedTextColor.GOLD));
                else p.sendMessage(Component.text("   Festival no dia 4 da estação.", NamedTextColor.DARK_GRAY));
                p.sendMessage(Component.text("   A estação muda em " + br.rpgatributos.mundo.Estacoes.duracao(est.restante()) + ".", NamedTextColor.GRAY));
                p.sendMessage(Component.text("   🍊 Frutas desta estação: " + plugin.pomar().frutasDaEstacao(), NamedTextColor.GOLD));
                p.sendMessage(Component.text("☾ " + plugin.ceu().resumo(), NamedTextColor.LIGHT_PURPLE));
                p.sendMessage(Component.text("☠ Mundo na semana " + plugin.perigo().semanas() + " (monstros +" + Math.round(plugin.perigo().forca() * 100)
                        + "%)  ·  Chefe Mundial: " + plugin.chefeMundial().situacao(), NamedTextColor.RED));
            }
            case "maldicao" -> {
                if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return true; }
                if (args.length >= 1 && args[0].equalsIgnoreCase("transformar")) {
                    String erro = plugin.maldicoes().alternarTransformacao(p);
                    if (erro != null) erro(sender, erro);
                } else {
                    plugin.maldicoes().info(p);
                }
            }
            case "bestiario" -> {
                if (sender instanceof Player p) plugin.bestiario().abrir(p);
                else erro(sender, "Só jogadores.");
            }
            case "colonia" -> {
                if (sender instanceof Player p) plugin.colonias().abrirMinha(p);
                else erro(sender, "Só jogadores.");
            }
            case "masmorra" -> masmorra(sender, args);
            case "classe" -> {
                if (sender instanceof Player p) plugin.santuarios().abrirArvore(p, null);
                else erro(sender, "Só jogadores.");
            }
            case "lendas" -> {
                if (sender instanceof Player p) plugin.menuLendas().abrir(p);
                else erro(sender, "Só jogadores.");
            }
            case "locais" -> {
                if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return true; }
                List<String> achados = plugin.locais().descobertos(p);
                p.sendMessage(Component.text("✦ Locais Ocultos que você achou: " + achados.size() + "/" + plugin.locais().total(), NamedTextColor.LIGHT_PURPLE));
                for (String s : achados) p.sendMessage(Component.text(" • " + s, NamedTextColor.GRAY));
                if (achados.isEmpty()) p.sendMessage(Component.text("Procure Mapas Rasgados nas masmorras e escute os sussurros pelo mundo.", NamedTextColor.DARK_GRAY));
            }
            default -> { return false; }
        }
        return true;
    }

    private static void erro(CommandSender s, String msg) {
        s.sendMessage(Component.text(msg, NamedTextColor.RED));
    }

    private static void ok(CommandSender s, String msg) {
        s.sendMessage(Component.text(msg, NamedTextColor.GREEN));
    }

    // ================= /atributos =================

    private void atributos(CommandSender sender, String[] args) {
        Player alvo;
        if (args.length >= 1) {
            if (!sender.hasPermission("rpg.atributos.outros")) { erro(sender, "Sem permissão."); return; }
            alvo = Bukkit.getPlayerExact(args[0]);
            if (alvo == null) { erro(sender, "Jogador não encontrado (precisa estar online)."); return; }
        } else if (sender instanceof Player p) {
            alvo = p;
        } else {
            erro(sender, "Use: /atributos <jogador>");
            return;
        }

        // Jogador vê o menu; "/atributos <jogador> chat" ou o console veem no chat.
        boolean noChat = args.length >= 2 && args[1].equalsIgnoreCase("chat");
        if (sender instanceof Player quemVe && !noChat) {
            plugin.menus().abrirPrincipal(quemVe, alvo);
            return;
        }

        StatsManager st = plugin.stats();
        int max = plugin.settings().nivelMaximo;

        sender.sendMessage(Component.text("━━━━━ ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Atributos de " + alvo.getName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" ━━━━━", NamedTextColor.DARK_GRAY)));

        for (Skill s : Skill.values()) {
            int nivel = st.getNivel(alvo, s);
            double prog = st.progresso(alvo, s);
            sender.sendMessage(Component.text(s.icone() + " " + s.nome(), s.cor())
                    .append(Component.text("  Nv " + nivel + (nivel >= max ? " (MÁX)" : ""), NamedTextColor.WHITE))
                    .append(Component.text("  ")).append(barra(prog))
                    .append(Component.text(" " + Math.round(prog * 100) + "%", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("   " + plugin.bonus().descricao(s, nivel), NamedTextColor.DARK_GRAY));
        }
        sender.sendMessage(Component.text("Nível total: " + st.nivelTotal(alvo), NamedTextColor.YELLOW));
    }

    private static Component barra(double progresso) {
        int cheios = (int) Math.round(progresso * 10);
        return Component.text("■".repeat(cheios), NamedTextColor.GREEN)
                .append(Component.text("■".repeat(10 - cheios), NamedTextColor.DARK_GRAY));
    }

    // ================= /rpgadmin =================

    private void admin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("""
                    /rpgadmin set <jogador> <atributo> <nível>
                    /rpgadmin addxp <jogador> <atributo> <xp>
                    /rpgadmin reset <jogador> [atributo]
                    /rpgadmin forjar [raridade]  (forja o item da sua mão)
                    /rpgadmin forjaferreiro  (a bigorna que você olha vira Forja do Ferreiro)
                    /rpgadmin infusor  (o caldeirão que você olha vira infusor)
                    /rpgadmin infundir <parte>  (infunde o item da mão, sem custo)
                    /rpgadmin grimorio [jogador]
                    /rpgadmin mana [jogador]  (enche a mana)
                    /rpgadmin descobrir [jogador]  (libera todas as magias secretas)
                    /rpgadmin altar  (a obsidiana chorosa que você olha vira Altar Ritualístico)
                    /rpgadmin chefe <golem|rainha|chamas|lich|tempestade|arauto>  (no altar que você olha)
                    /rpgadmin evento <lua_de_sangue|meteoros|ondas|mercador|cacador>  (no altar que você olha)
                    /rpgadmin raro <material> [quantidade] [jogador]
                    /rpgadmin refino <0-10>  (refino do item da sua mão)
                    /rpgadmin conquista <contador> <quantidade> [jogador]
                    /rpgadmin cozinha  (o defumador que você olha vira Cozinha)
                    /rpgadmin reciclagem  (o rebolo que você olha vira Bancada de Reciclagem)
                    /rpgadmin variedade <id> [quantidade] [semente]
                    /rpgadmin prato <id> [normal|boa|otima|perfeita]
                    /rpgadmin adubo [quantidade]
                    /rpgadmin marco  (a magnetita que você olha vira o seu Marco do Território)
                    /rpgadmin ignorar  (liga/desliga ignorar as proteções dos territórios)
                    /rpgadmin apagarterritorio <dono>
                    /rpgadmin altardomador  (o fardo de feno que você olha vira Altar do Domador)
                    /rpgadmin invocar <criatura>  (invoca um companheiro seu, sem custo)
                    /rpgadmin portalmasmorra  (os tijolos entalhados que você olha viram Portal da Masmorra)
                    /rpgadmin masmorra <dificuldade>  (abre uma masmorra só para você, sem custo)
                    /rpgadmin sala  (ferramentas para construir salas de masmorra)
                    /rpgadmin fecharmasmorras
                    /rpgadmin santuario  (o púlpito que você olha vira Santuário das Classes)
                    /rpgadmin classe <jogador> <classe> [nível 1-20]
                    /rpgadmin prova  (começa a prova do seu caminho atual, sem precisar das tarefas)
                    /rpgadmin lenda <lenda>  (forja a lenda no item da mão, sem feitos nem custo)
                    /rpgadmin liberarlenda <lenda>  (tira a lenda do dono atual)
                    /rpgadmin revelar <lenda|todas>  (mostra os feitos escondidos de uma lenda para você)
                    /rpgadmin pedraviagem  (a pedra entalhada de ardósia que você olha vira Pedra de Viagem)
                    /rpgadmin local <tipo>  (constrói um Local Oculto aqui do lado)
                    /rpgadmin locais  (lista onde ficam todos os Locais Ocultos)
                    /rpgadmin liberarclasse <classe lendária>
                    /rpgadmin bancadaalquimica  (o suporte de poções que você olha vira Bancada Alquímica)
                    /rpgadmin peixe <peixe raro> [quantidade]
                    /rpgadmin componente <componente> [quantidade]
                    /rpgadmin gema <gema> [grau 1-3] [quantidade]
                    /rpgadmin elixir <elixir> [normal|boa|otima|perfeita]
                    /rpgadmin acessorio <acessório>
                    /rpgadmin cajado [raridade]  (um Cajado Arcano forjado)
                    /rpgadmin tomo <magia proibida>
                    /rpgadmin maestria <jogador> <elemento> <nível 0-10>
                    /rpgadmin prefeitura  (o sino que você olha vira a Prefeitura da sua colônia)
                    /rpgadmin colono [quantidade]  (moradores chegam agora na sua colônia)
                    /rpgadmin turnocolonia  (um turno de trabalho agora)
                    /rpgadmin nivelcolonia <1-5>
                    /rpgadmin guerra iniciar <reino A> <reino B>  (guerra começando agora, para testar)
                    /rpgadmin guerra encerrar  (encerra as guerras em andamento)
                    /rpgadmin elite [1-3]  (o monstro que você olha vira Elite)
                    /rpgadmin ninho [zumbis|esqueletos|aranhas|illagers]  (um ninho à sua frente)
                    /rpgadmin horda  (a sua colônia é atacada em 1 minuto)
                    /rpgadmin chefemundial  (o Chefe Mundial aparece em 30 segundos)
                    /rpgadmin idademundo <semanas>  (muda a idade do mundo)
                    /rpgadmin estacao <primavera|verao|outono|inverno>
                    /rpgadmin festival  (hoje vira dia de festival)
                    /rpgadmin clima <nevasca|neblina|ventania|tempestade|calor>
                    /rpgadmin ceu <luadesangue|meteoros|eclipse|aurora>
                    /rpgadmin maldicao <jogador> <licantropia|vampirismo|nenhuma>
                    /rpgadmin portal <facil|normal|dificil|pesadelo> [eco]  (abre um portal à sua frente)
                    /rpgadmin portaltransbordar | portalfechar | portalsortear  (o portal mais perto)
                    /rpgadmin obelisco  (olhando para obsidiana chorona)
                    /rpgadmin torre <andar>  (sobe sozinho a partir do andar)
                    /rpgadmin proficiencia <jogador> <arma> <nível 1-20>  (proficiência de arma dos combos)
                    /rpgadmin vigor [jogador]  (enche o vigor)
                    /rpgadmin gancho [jogador]  (dá um Gancho de Escalada)
                    /rpgadmin criaturas  (quantas criaturas há em cada mundo e onde)
                    /rpgadmin limparcriaturas  (remove sobras de eventos e monstros presos)
                    /rpgadmin santuariodivino | circulo | mesarunica  (olhando para quartzo entalhado, ametista ou tufo entalhado)
                    /rpgadmin devocao <jogador> <quantidade>  (soma devoção ao deus que ele segue)
                    /rpgadmin pedrafilosofal  (dá uma Pedra Filosofal)
                    /rpgadmin runa <runa>  (grava a runa no item da sua mão, sem custo)
                    /rpgadmin mitrilo [minerio|bruto|lingote] [qtd]  (minério no bloco que você olha, ou os itens)
                    /rpgadmin mapatesouro [comum|raro|lendario]
                    /rpgadmin sitio  (um sítio de arqueologia à sua frente)
                    /rpgadmin reliquia <relíquia>
                    /rpgadmin cronista  (coloca o Cronista onde você está)
                    /rpgadmin capitulo <jogador> <0-8>  (capítulo da campanha)
                    /rpgadmin pacote  (onde está o pacote de recursos e como enviar)
                    /rpgadmin talentopontos <jogador> <qtd>  (pontos de talento a mais; negativo tira)
                    /rpgadmin renascer <jogador> <0-5>  (define quantas vezes ele renasceu)
                    /rpgadmin companheironivel <1-30>  (nível do companheiro que você olha)
                    /rpgadmin boneco|pedraamolar|fogueira|bebedouro|cartografo  (cria a estação no bloco que você olha)
                    /rpgadmin ferradura [qtd], /rpgadmin ninhopassaro [qtd]
                    /rpgadmin animalraro [ouro|neve]  (o animal que você olha vira raro)
                    /rpgadmin barril|colmeia|canteiro  (cria a estação no bloco que você olha)
                    /rpgadmin carta <carta> [brilhante], flecha|frasco|erva|mel <tipo> [qtd], bebida <tipo> [grau 0-3]
                    /rpgadmin mercador  (o Mercador Itinerante aparece perto de você)
                    /rpgadmin encontro [carroca|bandidos|viajante|estrela]
                    /rpgadmin muda|fruta <fruta> [qtd]  (pomar)
                    /rpgadmin estrutura <tipo>  (constrói à sua frente; console: <tipo> <mundo> <x> <z>)
                    /rpgadmin estruturas  (as mais perto de você), /rpgadmin esquecerestrutura
                    /rpgadmin reload""", NamedTextColor.YELLOW));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.recarregar();
                ok(sender, "Config recarregada.");
                return;
            }
            case "forjar" -> { forjar(sender, args); return; }
            case "infusor" -> { criarInfusor(sender); return; }
            case "forjaferreiro" -> { criarForja(sender); return; }
            case "infundir" -> { infundir(sender, args); return; }
            case "grimorio", "mana", "descobrir" -> { adminArcano(sender, sub, args); return; }
            case "altar", "chefe", "evento", "raro", "refino", "conquista" -> { adminAventura(sender, sub, args); return; }
            case "cozinha", "reciclagem", "variedade", "prato", "adubo" -> { adminFazenda(sender, sub, args); return; }
            case "marco", "ignorar", "apagarterritorio" -> { adminTerritorio(sender, sub, args); return; }
            case "altardomador", "invocar" -> { adminDomador(sender, sub, args); return; }
            case "portalmasmorra", "masmorra", "sala", "fecharmasmorras" -> { adminMasmorra(sender, sub, args); return; }
            case "santuario", "classe", "prova" -> { adminClasse(sender, sub, args); return; }
            case "lenda", "liberarlenda" -> { adminLenda(sender, sub, args); return; }
            case "revelar", "pedraviagem", "local", "locais", "liberarclasse" -> { adminOculto(sender, sub, args); return; }
            case "bancadaalquimica", "peixe", "componente", "gema", "elixir", "acessorio" -> { adminAlquimia(sender, sub, args); return; }
            case "cajado", "tomo", "maestria" -> { adminMagia(sender, sub, args); return; }
            case "prefeitura", "colono", "turnocolonia", "nivelcolonia" -> { adminColonia(sender, sub, args); return; }
            case "elite", "ninho", "horda", "chefemundial", "idademundo" -> { adminPerigo(sender, sub, args); return; }
            case "estacao", "festival", "clima", "ceu", "maldicao" -> { adminMundo(sender, sub, args); return; }
            case "portal", "portaltransbordar", "portalfechar", "portalsortear", "obelisco", "torre" -> { adminFaseC(sender, sub, args); return; }
            case "proficiencia", "vigor", "gancho", "criaturas", "limparcriaturas" -> { adminCombos(sender, sub, args); return; }
            case "santuariodivino", "circulo", "mesarunica", "devocao", "pedrafilosofal", "runa" -> { adminFe(sender, sub, args); return; }
            case "mitrilo", "mapatesouro", "sitio", "reliquia", "cronista", "capitulo", "pacote" -> { adminExploracao(sender, sub, args); return; }
            case "talentopontos", "renascer", "companheironivel" -> { adminProgressao(sender, sub, args); return; }
            case "boneco", "pedraamolar", "fogueira", "bebedouro", "cabanapesca", "cartografo", "ferradura", "ninhopassaro", "animalraro" -> { adminDetalhes(sender, sub, args); return; }
            case "barril", "colmeia", "canteiro", "carta", "flecha", "frasco", "erva", "mel", "bebida", "mercador", "encontro", "muda", "fruta" -> { adminVida(sender, sub, args); return; }
            case "estrutura", "estruturas", "esquecerestrutura" -> { adminEstruturas(sender, sub, args); return; }
            case "guerra" -> {
                if (args.length >= 4 && args[1].equalsIgnoreCase("iniciar")) {
                    String erro = plugin.reinos().guerraAgora(args[2], args[3]);
                    if (erro != null) erro(sender, erro); else ok(sender, "Guerra começa no próximo segundo.");
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("encerrar")) {
                    ok(sender, plugin.reinos().encerrarTodas() + " guerra(s) encerrada(s).");
                } else {
                    erro(sender, "Use: /rpgadmin guerra iniciar <reino A> <reino B>  |  /rpgadmin guerra encerrar");
                }
                return;
            }
            default -> { }
        }
        if (args.length < 2) { erro(sender, "Informe o jogador."); return; }
        Player alvo = Bukkit.getPlayerExact(args[1]);
        if (alvo == null) { erro(sender, "Jogador não encontrado (precisa estar online)."); return; }
        StatsManager st = plugin.stats();

        switch (sub) {
            case "set", "addxp" -> {
                if (args.length < 4) { erro(sender, "Faltam argumentos."); return; }
                Skill s = Skill.porId(args[2]);
                if (s == null) { erro(sender, "Atributo inválido. Use: " + nomesSkills()); return; }
                double valor;
                try { valor = Double.parseDouble(args[3]); } catch (NumberFormatException ex) { erro(sender, "Número inválido."); return; }
                if (sub.equals("set")) {
                    int nivel = (int) Math.max(0, Math.min(valor, plugin.settings().nivelMaximo));
                    st.setXp(alvo, s, st.xpTotalParaNivel(nivel));
                    ok(sender, alvo.getName() + " agora tem " + s.nome() + " nível " + nivel + ".");
                } else {
                    st.darXp(alvo, s, valor / plugin.settings().multiplicadorXp);
                    ok(sender, "XP adicionado.");
                }
            }
            case "reset" -> {
                if (args.length >= 3) {
                    Skill s = Skill.porId(args[2]);
                    if (s == null) { erro(sender, "Atributo inválido."); return; }
                    st.setXp(alvo, s, 0);
                } else {
                    for (Skill s : Skill.values()) st.setXp(alvo, s, 0);
                }
                ok(sender, "Atributos de " + alvo.getName() + " resetados.");
            }
            default -> { erro(sender, "Subcomando desconhecido."); return; }
        }
        plugin.bonus().aplicar(alvo);
        plugin.tags().atualizarTexto(alvo);
    }

    /** /rpgadmin forjar [raridade]: forja (ou reforja) o item da mão. Útil para testar. */
    private void forjar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        ItemStack mao = p.getInventory().getItemInMainHand();
        if (Categoria.de(mao.getType()) == null) {
            erro(sender, "Segure uma arma, ferramenta, armadura, arco ou escudo.");
            return;
        }
        Raridade r = null;
        if (args.length >= 2) {
            r = Raridade.porId(args[1]);
            if (r == null) { erro(sender, "Raridade inválida. Use: " + nomesRaridades()); return; }
        }
        ItemStack forjado = plugin.forja().forjar(p, mao, r);
        forjado.setAmount(mao.getAmount());
        p.getInventory().setItemInMainHand(forjado);
        plugin.forja().celebrar(p, p.getLocation(), List.of(forjado));
    }

    // ================= arcano (admin) =================

    private void criarInfusor(CommandSender sender) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        Block b = p.getTargetBlockExact(6);
        if (b == null || b.getType() != Material.WATER_CAULDRON) {
            erro(sender, "Olhe para um caldeirão com água (até 6 blocos).");
            return;
        }
        if (plugin.arcano().infusores().eh(b)) { erro(sender, "Esse caldeirão já é um infusor."); return; }
        plugin.arcano().infusores().criar(b, p);
        ok(sender, "Infusor Corpóreo criado.");
    }

    private void criarForja(CommandSender sender) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        Block b = p.getTargetBlockExact(6);
        if (b == null || !b.getType().name().endsWith("ANVIL")) {
            erro(sender, "Olhe para uma bigorna (até 6 blocos).");
            return;
        }
        if (plugin.forjas().eh(b)) { erro(sender, "Essa bigorna já é uma Forja do Ferreiro."); return; }
        plugin.forjas().criar(b, p);
        ok(sender, "Forja do Ferreiro criada.");
    }

    // ================= aventura (admin) =================

    private void adminAventura(CommandSender sender, String sub, String[] args) {
        switch (sub) {
            case "raro" -> {
                if (args.length < 2) { erro(sender, "Use: /rpgadmin raro <" + nomesRaros() + "> [quantidade] [jogador]"); return; }
                Raro r = Raro.porId(args[1]);
                if (r == null) { erro(sender, "Material inválido. Use: " + nomesRaros()); return; }
                int qtd = 1;
                if (args.length >= 3) {
                    try { qtd = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException e) { erro(sender, "Quantidade inválida."); return; }
                }
                Player alvo = args.length >= 4 ? Bukkit.getPlayerExact(args[3]) : sender instanceof Player p ? p : null;
                if (alvo == null) { erro(sender, "Jogador não encontrado."); return; }
                alvo.getInventory().addItem(r.criar(qtd)).values().forEach(s -> alvo.getWorld().dropItemNaturally(alvo.getLocation(), s));
                ok(sender, qtd + "x " + r.nome() + " para " + alvo.getName() + ".");
                return;
            }
            case "conquista" -> {
                if (args.length < 3) { erro(sender, "Use: /rpgadmin conquista <contador> <quantidade> [jogador]"); return; }
                Player alvo = args.length >= 4 ? Bukkit.getPlayerExact(args[3]) : sender instanceof Player p ? p : null;
                if (alvo == null) { erro(sender, "Jogador não encontrado."); return; }
                int qtd;
                try { qtd = Integer.parseInt(args[2]); } catch (NumberFormatException e) { erro(sender, "Quantidade inválida."); return; }
                plugin.titulos().registrar(alvo, args[1], qtd);
                ok(sender, "Conquista '" + args[1] + "' +" + qtd + " para " + alvo.getName() + ".");
                return;
            }
            default -> { }
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "altar" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.CRYING_OBSIDIAN) { erro(sender, "Olhe para uma obsidiana chorosa (até 6 blocos)."); return; }
                if (plugin.altares().eh(b)) { erro(sender, "Isso já é um Altar Ritualístico."); return; }
                plugin.altares().criar(b, p);
                ok(sender, "Altar Ritualístico criado.");
            }
            case "chefe" -> {
                Chefe c = args.length >= 2 ? Chefe.porId(args[1]) : null;
                if (c == null) { erro(sender, "Use: /rpgadmin chefe <" + nomesChefes() + ">"); return; }
                Block b = p.getTargetBlockExact(10);
                if (b == null || !plugin.altares().eh(b)) { erro(sender, "Olhe para um Altar Ritualístico."); return; }
                plugin.chefes().invocar(c, b.getLocation(), p);
            }
            case "evento" -> {
                Invocacao inv = args.length >= 2 ? Invocacao.porId(args[1]) : null;
                if (inv == null || inv.chefe() != null) { erro(sender, "Use: /rpgadmin evento <lua_de_sangue|meteoros|ondas|mercador|cacador>"); return; }
                Block b = p.getTargetBlockExact(10);
                if (b == null || !plugin.altares().eh(b)) { erro(sender, "Olhe para um Altar Ritualístico."); return; }
                switch (inv) {
                    case LUA_DE_SANGUE -> plugin.eventos().iniciarLua(b.getWorld(), p);
                    case METEOROS -> plugin.eventos().iniciarMeteoros(b.getLocation(), p);
                    case ONDAS -> { if (!plugin.eventos().iniciarOndas(b.getLocation(), p)) erro(sender, "Ninguém perto para lutar."); }
                    case MERCADOR -> plugin.mercadores().invocarMercador(b.getLocation(), p);
                    case CACADOR -> plugin.mercadores().invocarCacador(b.getLocation(), p);
                    default -> { }
                }
            }
            case "refino" -> {
                ItemStack mao = p.getInventory().getItemInMainHand();
                DadosForja d = plugin.forja().ler(mao);
                if (d == null) { erro(sender, "Segure um item forjado."); return; }
                int n;
                try { n = Integer.parseInt(args.length >= 2 ? args[1] : "x"); } catch (NumberFormatException e) { erro(sender, "Use: /rpgadmin refino <0-10>"); return; }
                plugin.forja().construir(mao, d.comRefino(n));
                p.getInventory().setItemInMainHand(mao);
                ok(sender, "Refino definido para +" + Math.max(0, Math.min(10, n)) + ".");
            }
            default -> { }
        }
    }

    // ================= fazenda e reciclagem (admin) =================

    private void adminFazenda(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "cozinha" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.SMOKER) { erro(sender, "Olhe para um defumador (até 6 blocos)."); return; }
                if (plugin.cozinha().eh(b)) { erro(sender, "Esse defumador já é uma Cozinha."); return; }
                plugin.cozinha().criar(b, p);
                ok(sender, "Cozinha criada.");
            }
            case "reciclagem" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.GRINDSTONE) { erro(sender, "Olhe para um rebolo (até 6 blocos)."); return; }
                if (plugin.reciclagem().eh(b)) { erro(sender, "Esse rebolo já é uma Bancada de Reciclagem."); return; }
                plugin.reciclagem().criar(b, p);
                ok(sender, "Bancada de Reciclagem criada.");
            }
            case "variedade" -> {
                Variedade v = args.length >= 2 ? Variedade.porId(args[1]) : null;
                if (v == null) { erro(sender, "Use: /rpgadmin variedade <" + nomesVariedades() + "> [quantidade] [semente]"); return; }
                int qtd = quantidade(args, 2, 16);
                if (qtd < 0) { erro(sender, "Quantidade inválida."); return; }
                boolean semente = args.length >= 4 && args[3].equalsIgnoreCase("semente");
                entregar(p, semente ? v.semente(qtd) : v.produto(qtd));
                ok(sender, qtd + "x " + v.nome() + (semente ? " (sementes)" : "") + ".");
            }
            case "prato" -> {
                Prato pr = args.length >= 2 ? Prato.porId(args[1]) : null;
                if (pr == null) { erro(sender, "Use: /rpgadmin prato <" + nomesPratos() + "> [normal|boa|otima|perfeita]"); return; }
                Qualidade q = Qualidade.NORMAL;
                if (args.length >= 3) {
                    try { q = Qualidade.valueOf(args[2].toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException e) { erro(sender, "Qualidade inválida. Use: normal, boa, otima, perfeita"); return; }
                }
                entregar(p, plugin.cozinha().criar(pr, q, 1.0, p.getName()));
                ok(sender, pr.nome() + " (" + q.nome() + ") entregue.");
            }
            case "adubo" -> {
                int qtd = quantidade(args, 1, 16);
                if (qtd < 0) { erro(sender, "Quantidade inválida."); return; }
                entregar(p, plugin.agricultura().adubo(qtd));
                ok(sender, qtd + "x Adubo Rico.");
            }
            default -> { }
        }
    }

    // ================= pesca e alquimia (admin) =================

    private void adminAlquimia(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "bancadaalquimica" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.BREWING_STAND) { erro(sender, "Olhe para um suporte de poções (até 6 blocos)."); return; }
                if (plugin.alquimia().eh(b)) { erro(sender, "Esse suporte já é uma Bancada Alquímica."); return; }
                plugin.alquimia().criar(b, p);
                ok(sender, "Bancada Alquímica criada.");
            }
            case "peixe" -> {
                PeixeRaro pr = args.length >= 2 ? PeixeRaro.porId(args[1]) : null;
                if (pr == null) { erro(sender, "Use: /rpgadmin peixe <" + nomes(PeixeRaro.values()) + "> [quantidade]"); return; }
                int qtd = quantidade(args, 2, 1);
                if (qtd < 0) { erro(sender, "Quantidade inválida."); return; }
                entregar(p, pr.criar(qtd, Qualidade.NORMAL));
                ok(sender, qtd + "x " + pr.nome() + ".");
            }
            case "componente" -> {
                Reagente r = args.length >= 2 ? Reagente.porId(args[1]) : null;
                if (r == null) { erro(sender, "Use: /rpgadmin componente <" + nomes(Reagente.values()) + "> [quantidade]"); return; }
                int qtd = quantidade(args, 2, 8);
                if (qtd < 0) { erro(sender, "Quantidade inválida."); return; }
                entregar(p, r.criar(qtd));
                ok(sender, qtd + "x " + r.nome() + ".");
            }
            case "gema" -> {
                Gema g = args.length >= 2 ? Gema.porId(args[1]) : null;
                if (g == null) { erro(sender, "Use: /rpgadmin gema <" + nomes(Gema.values()) + "> [grau 1-3] [quantidade]"); return; }
                int grau = quantidade(args, 2, 1);
                int qtd = quantidade(args, 3, 1);
                if (grau < 1 || grau > Gema.GRAU_MAXIMO || qtd < 0) { erro(sender, "Grau (1-3) ou quantidade inválidos."); return; }
                entregar(p, g.criar(grau, qtd));
                ok(sender, qtd + "x " + g.nome(grau) + ".");
            }
            case "elixir" -> {
                Elixir x = args.length >= 2 ? Elixir.porId(args[1]) : null;
                if (x == null) { erro(sender, "Use: /rpgadmin elixir <" + nomes(Elixir.values()) + "> [normal|boa|otima|perfeita]"); return; }
                Qualidade q = Qualidade.NORMAL;
                if (args.length >= 3) {
                    try { q = Qualidade.valueOf(args[2].toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException e) { erro(sender, "Qualidade inválida. Use: normal, boa, otima, perfeita"); return; }
                }
                entregar(p, x.criar(q, q.fator(), p.getName()));
                ok(sender, x.nome() + " (" + q.nome() + ") entregue.");
            }
            case "acessorio" -> {
                Acessorio a = args.length >= 2 ? Acessorio.porId(args[1]) : null;
                if (a == null) { erro(sender, "Use: /rpgadmin acessorio <" + nomes(Acessorio.values()) + ">"); return; }
                entregar(p, a.criar());
                ok(sender, a.nome() + " entregue.");
            }
            default -> { }
        }
    }

    private void adminMundo(CommandSender sender, String sub, String[] args) {
        org.bukkit.World w = Bukkit.getWorlds().getFirst();
        switch (sub) {
            case "estacao" -> {
                br.rpgatributos.mundo.Estacoes.Estacao e = null;
                if (args.length >= 2) for (var x : br.rpgatributos.mundo.Estacoes.Estacao.values()) if (x.name().equalsIgnoreCase(args[1])) e = x;
                if (e == null) { erro(sender, "Use: /rpgadmin estacao <primavera|verao|outono|inverno>"); return; }
                plugin.estacoes().definir(e);
                ok(sender, "Agora é " + e.nome() + ".");
            }
            case "festival" -> {
                plugin.estacoes().definirFestival();
                ok(sender, "Hoje é dia de festival.");
            }
            case "clima" -> {
                br.rpgatributos.mundo.Ceu.Clima c = null;
                if (args.length >= 2) for (var x : br.rpgatributos.mundo.Ceu.Clima.values()) if (x.name().equalsIgnoreCase(args[1])) c = x;
                if (c == null) { erro(sender, "Use: /rpgadmin clima <nevasca|neblina|ventania|tempestade|calor>"); return; }
                plugin.ceu().comecarClima(w, c);
                ok(sender, c.nome() + " começou.");
            }
            case "ceu" -> {
                String o = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
                switch (o) {
                    case "luadesangue" -> plugin.ceu().forcarLuaDeSangue();
                    case "meteoros" -> plugin.ceu().comecarMeteoros();
                    case "eclipse" -> plugin.ceu().comecarEclipse(w);
                    case "aurora" -> plugin.ceu().forcarAurora();
                    default -> { erro(sender, "Use: /rpgadmin ceu <luadesangue|meteoros|eclipse|aurora>"); return; }
                }
                ok(sender, "Feito.");
            }
            case "maldicao" -> {
                Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
                if (alvo == null || args.length < 3) { erro(sender, "Use: /rpgadmin maldicao <jogador> <licantropia|vampirismo|nenhuma>"); return; }
                br.rpgatributos.mundo.Maldicoes.Maldicao m = null;
                for (var x : br.rpgatributos.mundo.Maldicoes.Maldicao.values()) if (x.name().equalsIgnoreCase(args[2])) m = x;
                plugin.maldicoes().amaldicoar(alvo, m);
                ok(sender, "Feito.");
            }
            default -> { }
        }
    }

    private void adminEstruturas(CommandSender sender, String sub, String[] args) {
        var est = plugin.estruturas();
        if (sub.equals("estrutura")) {
            var t = args.length >= 2 ? br.rpgatributos.estruturas.Estrutura.porId(args[1]) : null;
            if (t == null) {
                erro(sender, "Use: /rpgadmin estrutura <" + String.join("|", java.util.Arrays.stream(br.rpgatributos.estruturas.Estrutura.values())
                        .map(br.rpgatributos.estruturas.Estrutura::id).toList()) + ">");
                return;
            }
            String falha;
            if (sender instanceof Player p) {
                falha = est.construirAdmin(p, t);
            } else {
                org.bukkit.World w = args.length >= 5 ? Bukkit.getWorld(args[2]) : null;
                if (w == null) { erro(sender, "Pelo console: /rpgadmin estrutura <tipo> <mundo> <x> <z>"); return; }
                try {
                    falha = est.construirEm(w, Integer.parseInt(args[3]), Integer.parseInt(args[4]), t);
                } catch (NumberFormatException ex) {
                    erro(sender, "x e z precisam ser números.");
                    return;
                }
            }
            if (falha != null) erro(sender, falha);
            else ok(sender, "Pronto: " + t.nome() + ".");
            return;
        }
        if (sub.equals("estruturas") && !est.recusas().isEmpty()) {
            sender.sendMessage(Component.text("Lugares recusados desde o início: " + est.recusas(), NamedTextColor.GRAY));
        }
        if (!(sender instanceof Player p)) {
            if (sub.equals("estruturas")) ok(sender, est.quantas() + " estrutura(s) registrada(s).");
            else erro(sender, "Só jogadores.");
            return;
        }
        if (sub.equals("esquecerestrutura")) {
            String nome = est.esquecer(p.getLocation());
            if (nome == null) erro(sender, "Nenhuma estrutura registrada a até 24 blocos.");
            else ok(sender, nome + " saiu do registro (os blocos ficam onde estão).");
            return;
        }
        List<String> l = est.listar(p.getLocation(), 10);
        ok(sender, est.quantas() + " estrutura(s) registrada(s). Mais perto de você:");
        if (l.isEmpty()) sender.sendMessage(Component.text("  (nenhuma neste mundo)", NamedTextColor.GRAY));
        for (String s : l) sender.sendMessage(Component.text("  " + s, NamedTextColor.GRAY));
    }

    private void adminVida(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        String a1 = args.length >= 2 ? args[1].toUpperCase(Locale.ROOT) : "";
        int qtd = Math.max(1, quantidade(args, 2, 1));
        try {
            switch (sub) {
                case "carta" -> {
                    var c = br.rpgatributos.vida.Carta.porId(a1);
                    if (c == null) { erro(sender, "Use: /rpgadmin carta <carta> [brilhante]"); return; }
                    entregar(p, br.rpgatributos.vida.Album.carta(c, args.length >= 3 && args[2].equalsIgnoreCase("brilhante")));
                }
                case "flecha" -> entregar(p, br.rpgatributos.vida.Flechas.criar(br.rpgatributos.vida.Flechas.Tipo.valueOf(a1), qtd));
                case "frasco" -> entregar(p, br.rpgatributos.vida.Frascos.criar(br.rpgatributos.vida.Frascos.Tipo.valueOf(a1), qtd));
                case "erva" -> entregar(p, br.rpgatributos.vida.Erva.valueOf(a1).criar(qtd));
                case "muda" -> entregar(p, br.rpgatributos.vida.Fruta.valueOf(a1).muda(qtd));
                case "fruta" -> entregar(p, br.rpgatributos.vida.Fruta.valueOf(a1).fruta(qtd));
                case "mel" -> entregar(p, br.rpgatributos.vida.Mel.valueOf(a1).criar(qtd));
                case "bebida" -> {
                    int grau = args.length >= 3 ? Math.max(0, Math.min(3, Integer.parseInt(args[2]))) : 0;
                    entregar(p, br.rpgatributos.vida.Bebida.valueOf(a1).criar(grau));
                }
                case "mercador" -> {
                    if (!plugin.mercador().aparecer(p)) erro(sender, "Não achei um lugar firme perto de você.");
                }
                case "encontro" -> {
                    String tipo = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : null;
                    String feito = plugin.encontros().sortear(p, tipo);
                    if (feito == null) { erro(sender, "Não achei um lugar firme perto de você."); return; }
                    ok(sender, "Encontro: " + feito + ".");
                    return;
                }
                default -> {
                    Block b = p.getTargetBlockExact(6);
                    Material precisa = switch (sub) {
                        case "barril" -> Material.BARREL;
                        case "colmeia" -> Material.BEEHIVE;
                        default -> Material.MOSS_BLOCK;
                    };
                    if (b == null || (b.getType() != precisa && !(sub.equals("colmeia") && b.getType() == Material.BEE_NEST))) {
                        erro(sender, "Olhe para " + precisa.name().toLowerCase(Locale.ROOT) + " (até 6 blocos)."); return;
                    }
                    if (plugin.ehEstacao(b)) { erro(sender, "Esse bloco já é uma estação."); return; }
                    switch (sub) {
                        case "barril" -> plugin.barris().criar(b, p);
                        case "colmeia" -> plugin.colmeias().criar(b, p);
                        default -> plugin.canteiros().criar(b, p);
                    }
                }
            }
            ok(sender, "Feito.");
        } catch (IllegalArgumentException ex) {
            erro(sender, "Nome inválido. Use o Tab para ver as opções.");
        }
    }
    private void adminDetalhes(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "ferradura" -> {
                entregar(p, br.rpgatributos.detalhes.ItensDetalhes.ferradura(Math.max(1, quantidade(args, 1, 1))));
                ok(sender, "Ferradura entregue.");
            }
            case "ninhopassaro" -> {
                entregar(p, br.rpgatributos.detalhes.ItensDetalhes.ninho(Math.max(1, quantidade(args, 1, 1))));
                ok(sender, "Ninho entregue.");
            }
            case "animalraro" -> {
                if (!(p.getTargetEntity(8) instanceof org.bukkit.entity.Animals a)) { erro(sender, "Olhe para um animal (até 8 blocos)."); return; }
                var r = args.length >= 2 && args[1].equalsIgnoreCase("neve") ? br.rpgatributos.detalhes.Natureza.Raridade.NEVE
                        : br.rpgatributos.detalhes.Natureza.Raridade.OURO;
                plugin.natureza().tornarRaro(a, r);
                ok(sender, "Agora é um animal raro.");
            }
            default -> {
                Block b = p.getTargetBlockExact(6);
                Material precisa = switch (sub) {
                    case "boneco" -> Material.HAY_BLOCK;
                    case "pedraamolar" -> Material.SMOOTH_STONE;
                    case "fogueira", "cabanapesca" -> Material.CAMPFIRE;
                    case "bebedouro" -> Material.WATER_CAULDRON;
                    default -> Material.CARTOGRAPHY_TABLE;
                };
                boolean amolar = sub.equals("pedraamolar") && b != null && (b.getType() == Material.GRINDSTONE || b.getType() == Material.SMOOTH_STONE_SLAB);
                if (b == null || (b.getType() != precisa && !amolar && !((sub.equals("fogueira") || sub.equals("cabanapesca")) && b.getType() == Material.SOUL_CAMPFIRE))) {
                    erro(sender, "Olhe para " + precisa.name().toLowerCase(Locale.ROOT) + " (até 6 blocos)."); return;
                }
                if (plugin.ehEstacao(b)) { erro(sender, "Esse bloco já é uma estação."); return; }
                switch (sub) {
                    case "boneco" -> plugin.bonecos().criar(b, p);
                    case "pedraamolar" -> plugin.pedrasAmolar().criar(b, p);
                    case "fogueira" -> plugin.fogueiras().criar(b, p);
                    case "bebedouro" -> plugin.bebedouros().criar(b, p);
                    case "cabanapesca" -> plugin.cabanasPesca().criar(b, p);
                    default -> plugin.mesasCartografo().criar(b, p);
                }
                ok(sender, "Estação criada.");
            }
        }
    }
    private void adminProgressao(CommandSender sender, String sub, String[] args) {
        if (sub.equals("companheironivel")) {
            if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
            if (!(p.getTargetEntity(8) instanceof org.bukkit.entity.LivingEntity e) || !br.rpgatributos.domador.Companheiros.eh(e)) {
                erro(sender, "Olhe para um companheiro (até 8 blocos)."); return;
            }
            plugin.evolucao().definirNivel(e, quantidade(args, 1, 1));
            ok(sender, br.rpgatributos.domador.Companheiros.nome(e) + ": nível " + br.rpgatributos.domador.Evolucao.nivel(e)
                    + ", estágio " + br.rpgatributos.domador.Evolucao.estagio(e) + ".");
            return;
        }
        Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
        if (alvo == null || args.length < 3) { erro(sender, "Use: /rpgadmin " + sub + " <jogador> <número>"); return; }
        int n;
        try { n = Integer.parseInt(args[2]); }
        catch (NumberFormatException ex) { erro(sender, "Número inválido: " + args[2]); return; }
        if (sub.equals("renascer")) {
            plugin.renascimento().definir(alvo, n);
            ok(sender, alvo.getName() + " agora renasceu " + plugin.renascimento().vezes(alvo) + " vez(es).");
        } else {
            plugin.talentos().darBonus(alvo, n);
            ok(sender, alvo.getName() + ": " + plugin.talentos().resumo(alvo) + ".");
        }
    }
    private void adminExploracao(CommandSender sender, String sub, String[] args) {
        if (sub.equals("pacote")) {
            var pac = plugin.pacote();
            if (pac.arquivo() == null) { erro(sender, "O pacote de recursos está desligado (pacote-de-recursos.ativado)."); return; }
            ok(sender, "Pacote: " + pac.arquivo().getPath());
            ok(sender, "SHA-1: " + pac.sha1());
            String url = pac.url();
            if (url != null) ok(sender, "Enviado aos jogadores por: " + url);
            else erro(sender, "Ainda não é enviado: ponha um link em pacote-de-recursos.url, ou use porta + endereco no config.");
            return;
        }
        if (sub.equals("capitulo")) {
            Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
            if (alvo == null || args.length < 3) { erro(sender, "Use: /rpgadmin capitulo <jogador> <0-8>"); return; }
            plugin.cronista().definir(alvo, quantidade(args, 2, 0));
            ok(sender, "Capítulo de " + alvo.getName() + ": " + (plugin.cronista().capitulo(alvo) + 1) + ".");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "mitrilo" -> {
                String o = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "minerio";
                switch (o) {
                    case "bruto" -> p.getInventory().addItem(br.rpgatributos.exploracao.Mitrilo.bruto(quantidade(args, 2, 4)));
                    case "lingote" -> p.getInventory().addItem(br.rpgatributos.exploracao.Mitrilo.lingote(quantidade(args, 2, 4)));
                    default -> {
                        if (!plugin.mitrilo().colocar(p.getTargetBlockExact(6))) { erro(sender, "Olhe para um bloco sólido (até 6 blocos)."); return; }
                    }
                }
                ok(sender, "Feito.");
            }
            case "mapatesouro" -> {
                var t = br.rpgatributos.exploracao.MapasDoTesouro.Tipo.porId(args.length >= 2 ? args[1] : "comum");
                if (t == null) { erro(sender, "Use: /rpgadmin mapatesouro [comum|raro|lendario]"); return; }
                p.getInventory().addItem(plugin.mapas().criar(t, p.getLocation()));
                ok(sender, "Mapa do Tesouro entregue.");
            }
            case "sitio" -> {
                if (plugin.arqueologia().construirAdmin(p)) ok(sender, "Sítio de arqueologia criado à sua frente.");
                else erro(sender, "O chão à frente precisa ser areia, cascalho ou terra.");
            }
            case "reliquia" -> {
                var r = args.length >= 2 ? br.rpgatributos.exploracao.Reliquia.porId(args[1]) : null;
                p.getInventory().addItem(r == null ? plugin.arqueologia().reliquiaAleatoria() : br.rpgatributos.exploracao.Arqueologia.criar(r));
                ok(sender, "Relíquia entregue (pegue-a do chão para entrar na coleção, ou jogue e pegue de novo).");
            }
            case "cronista" -> {
                plugin.cronista().colocar(p.getLocation());
                ok(sender, "O Cronista chegou.");
            }
            default -> { }
        }
    }

    private void adminFe(CommandSender sender, String sub, String[] args) {
        if (sub.equals("devocao")) {
            Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
            if (alvo == null || args.length < 3) { erro(sender, "Use: /rpgadmin devocao <jogador> <quantidade>"); return; }
            if (plugin.deuses().deus(alvo) == null) { erro(sender, alvo.getName() + " não segue nenhum deus."); return; }
            plugin.deuses().darDevocao(alvo, quantidade(args, 2, 100));
            ok(sender, "Devoção somada. Fé de " + alvo.getName() + ": " + plugin.deuses().nivel(alvo) + ".");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "pedrafilosofal" -> {
                p.getInventory().addItem(br.rpgatributos.fe.Transmutacao.pedraFilosofal());
                ok(sender, "Pedra Filosofal entregue.");
            }
            case "runa" -> {
                br.rpgatributos.fe.Runa r = args.length >= 2 ? br.rpgatributos.fe.Runa.porId(args[1]) : null;
                ItemStack mao = p.getInventory().getItemInMainHand();
                if (r == null) { erro(sender, "Use: /rpgadmin runa <salto|protecao|visao|impeto|explosao|gelo|tempestade|vampirica|fusao|eco>"); return; }
                if (mao.isEmpty() || !r.serve(mao.getType())) { erro(sender, "Segure uma peça onde a " + r.nome() + " vai."); return; }
                mao.editMeta(m -> {
                    m.getPersistentDataContainer().set(br.rpgatributos.fe.Runas.K_RUNA, org.bukkit.persistence.PersistentDataType.STRING, r.id());
                    plugin.runas().decorar(m);
                });
                ok(sender, r.nome() + " gravada.");
            }
            default -> {
                Block b = p.getTargetBlockExact(6);
                Material precisa = switch (sub) {
                    case "santuariodivino" -> Material.CHISELED_QUARTZ_BLOCK;
                    case "circulo" -> Material.AMETHYST_BLOCK;
                    default -> Material.CHISELED_TUFF;
                };
                if (b == null || b.getType() != precisa) { erro(sender, "Olhe para " + precisa.name().toLowerCase() + " (até 6 blocos)."); return; }
                if (plugin.ehEstacao(b)) { erro(sender, "Esse bloco já é uma estação."); return; }
                switch (sub) {
                    case "santuariodivino" -> plugin.santuariosDivinos().criar(b, p);
                    case "circulo" -> plugin.transmutacao().criar(b, p);
                    default -> plugin.mesasRunicas().criar(b, p);
                }
                ok(sender, "Estação criada.");
            }
        }
    }

    private void adminCombos(CommandSender sender, String sub, String[] args) {
        if (sub.equals("criaturas")) {
            plugin.controle().relatorio(sender);
            return;
        }
        if (sub.equals("limparcriaturas")) {
            ok(sender, plugin.controle().limpar() + " criatura(s) removida(s) (sobras de eventos e monstros que nunca sumiam).");
            return;
        }
        if (sub.equals("gancho")) {
            Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player p ? p : null;
            if (alvo == null) { erro(sender, "Use: /rpgadmin gancho [jogador]"); return; }
            alvo.getInventory().addItem(br.rpgatributos.combate.Mobilidade.gancho()).values()
                    .forEach(s -> alvo.getWorld().dropItemNaturally(alvo.getLocation(), s));
            ok(sender, "Gancho de Escalada entregue.");
            return;
        }
        if (sub.equals("vigor")) {
            Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player p ? p : null;
            if (alvo == null) { erro(sender, "Use: /rpgadmin vigor [jogador]"); return; }
            plugin.combos().darVigor(alvo, 100000);
            ok(sender, "Vigor cheio.");
            return;
        }
        Player alvo = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : null;
        br.rpgatributos.combo.TipoArma t = args.length >= 3 ? br.rpgatributos.combo.TipoArma.porId(args[2]) : null;
        if (alvo == null || t == null || args.length < 4) {
            erro(sender, "Use: /rpgadmin proficiencia <jogador> <espada|machado|lanca|tridente|maca|arco> <1-20>");
            return;
        }
        plugin.combos().definirNivel(alvo, t, quantidade(args, 3, 1));
        ok(sender, t.nome() + " de " + alvo.getName() + " no nível " + plugin.combos().nivel(alvo, t) + ".");
    }

    private void adminFaseC(CommandSender sender, String sub, String[] args) {
        if (sub.equals("portalsortear")) {
            if (plugin.portais().sortear() != null) ok(sender, "Um portal se abriu perto de alguém.");
            else erro(sender, "Não deu (limite de portais, ninguém no mundo normal ou sem lugar).");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "portal" -> {
                Dificuldade d = args.length >= 2 ? Dificuldade.porId(args[1]) : Dificuldade.NORMAL;
                if (d == null) { erro(sender, "Use: /rpgadmin portal <facil|normal|dificil|pesadelo> [eco]"); return; }
                Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(4));
                l.setY(p.getWorld().getHighestBlockYAt(l) + 1);
                l.setYaw(p.getLocation().getYaw() + 180);
                plugin.portais().abrir(l, d, args.length >= 3 && args[2].equalsIgnoreCase("eco"));
                ok(sender, "Portal aberto.");
            }
            case "portaltransbordar" -> {
                String erro = plugin.portais().transbordarAgora(p);
                if (erro != null) erro(sender, erro); else ok(sender, "O portal transborda no próximo segundo.");
            }
            case "portalfechar" -> {
                String erro = plugin.portais().fecharAdmin(p);
                if (erro != null) erro(sender, erro); else ok(sender, "Portal fechado.");
            }
            case "obelisco" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.CRYING_OBSIDIAN) { erro(sender, "Olhe para obsidiana chorona (até 6 blocos)."); return; }
                if (plugin.obeliscos().eh(b)) { erro(sender, "Isso já é um Obelisco da Torre."); return; }
                plugin.obeliscos().criar(b, p);
                ok(sender, "Obelisco da Torre criado.");
            }
            case "torre" -> {
                int andar = quantidade(args, 1, 1);
                String erro = plugin.torre().iniciar(p, Math.max(1, andar), null, true);
                if (erro != null) erro(sender, erro);
            }
            default -> { }
        }
    }

    private void adminPerigo(CommandSender sender, String sub, String[] args) {
        if (sub.equals("idademundo")) {
            int s;
            try { s = args.length >= 2 ? Integer.parseInt(args[1]) : 0; } catch (NumberFormatException e) { erro(sender, "Use: /rpgadmin idademundo <semanas>"); return; }
            plugin.perigo().definirSemanas(Math.max(0, s));
            ok(sender, "Mundo na semana " + plugin.perigo().semanas() + ": monstros +" + Math.round(plugin.perigo().forca() * 100) + "%.");
            return;
        }
        if (sub.equals("chefemundial")) {
            if (plugin.chefeMundial().comecar(30)) ok(sender, "Chefe Mundial em 30 segundos.");
            else erro(sender, "Já há um em andamento (" + plugin.chefeMundial().situacao() + ") ou não achei lugar.");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "elite" -> {
                var r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 20, 0.5, x -> x != p && x instanceof org.bukkit.entity.LivingEntity);
                if (r == null || !(r.getHitEntity() instanceof org.bukkit.entity.LivingEntity le)) { erro(sender, "Olhe para um monstro (até 20 blocos)."); return; }
                plugin.perigo().tornarElite(le, quantidade(args, 1, 1));
                ok(sender, "Elite criado.");
            }
            case "ninho" -> {
                Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(4));
                l.setY(p.getWorld().getHighestBlockYAt(l) + 1);
                if (plugin.ninhos().criar(l, args.length >= 2 ? args[1] : null, p)) ok(sender, "Ninho criado.");
                else erro(sender, "Não deu para criar ali (precisa de espaço livre, tipo válido).");
            }
            case "horda" -> {
                var c = plugin.colonias().de(p.getUniqueId());
                if (c == null) erro(sender, "Você não tem colônia.");
                else if (plugin.hordas().avisar(c, true)) ok(sender, "A horda chega em 1 minuto.");
                else erro(sender, "Já há um ataque em andamento.");
            }
            default -> { }
        }
    }

    private void adminColonia(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "prefeitura" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.BELL) { erro(sender, "Olhe para um sino (até 6 blocos)."); return; }
                if (plugin.prefeituras().eh(b)) { erro(sender, "Esse sino já é uma Prefeitura."); return; }
                if (plugin.colonias().de(p.getUniqueId()) != null) { erro(sender, "Você já tem uma colônia."); return; }
                plugin.prefeituras().criar(b, p);
                ok(sender, "Prefeitura criada.");
            }
            case "colono" -> {
                int qtd = quantidade(args, 1, 1);
                if (qtd < 0) { erro(sender, "Quantidade inválida."); return; }
                if (plugin.colonias().chegarAgora(p, Math.min(qtd, 20)) < 0) erro(sender, "Você não tem colônia.");
                else ok(sender, "Moradores chegando.");
            }
            case "turnocolonia" -> {
                if (plugin.colonias().turnoAgora(p)) ok(sender, "Turno de trabalho em meio segundo.");
                else erro(sender, "Você não tem colônia.");
            }
            case "nivelcolonia" -> {
                int n = quantidade(args, 1, 1);
                if (plugin.colonias().definirNivel(p, n)) ok(sender, "Nível da colônia definido.");
                else erro(sender, "Você não tem colônia.");
            }
            default -> { }
        }
    }

    private void adminMagia(CommandSender sender, String sub, String[] args) {
        if (sub.equals("maestria")) {
            if (args.length < 4) { erro(sender, "Use: /rpgadmin maestria <jogador> <elemento> <nível 0-10>"); return; }
            Player alvo = Bukkit.getPlayerExact(args[1]);
            br.rpgatributos.arcano.Essencia es = br.rpgatributos.arcano.Essencia.porNome(args[2].toUpperCase(Locale.ROOT));
            if (alvo == null || es == null) { erro(sender, "Jogador ou elemento inválido."); return; }
            int nivel;
            try { nivel = Integer.parseInt(args[3]); } catch (NumberFormatException e) { erro(sender, "Nível inválido."); return; }
            plugin.arcano().definirMaestria(alvo, es, nivel);
            ok(sender, "Maestria de " + es.nome() + " de " + alvo.getName() + " no nível " + Math.max(0, Math.min(10, nivel)) + ".");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (sub.equals("cajado")) {
            Raridade r = args.length >= 2 ? Raridade.porId(args[1]) : null;
            ItemStack cajado = plugin.forja().forjar(p, new ItemStack(Material.BREEZE_ROD), r);
            entregar(p, cajado);
            ok(sender, "Cajado Arcano entregue.");
            return;
        }
        Receita r = args.length >= 2 ? Receita.porNome(args[1].toUpperCase(Locale.ROOT)) : null;
        if (r == null || r.forma() != br.rpgatributos.arcano.Forma.PROIBIDA) {
            erro(sender, "Use: /rpgadmin tomo <chuva_de_meteoros|zero_absoluto|julgamento|ruptura_dimensional|renascer_da_floresta>");
            return;
        }
        entregar(p, br.rpgatributos.arcano.ItensMagicos.criarTomo(r));
        ok(sender, "Tomo Proibido: " + r.nome() + ".");
    }

    private static String nomes(Enum<?>[] valores) {
        return String.join("|", Arrays.stream(valores).map(v -> v.name().toLowerCase(Locale.ROOT)).toList());
    }

    // ================= territórios (admin) =================

    private void adminTerritorio(CommandSender sender, String sub, String[] args) {
        if (sub.equals("apagarterritorio")) {
            if (args.length < 2) { erro(sender, "Use: /rpgadmin apagarterritorio <dono>"); return; }
            for (Territorio t : List.copyOf(plugin.territorios().todos())) {
                if (t.nomeDono().equalsIgnoreCase(args[1])) {
                    plugin.territorios().abandonar(t);
                    ok(sender, "Território de " + t.nomeDono() + " apagado.");
                    return;
                }
            }
            erro(sender, "Ninguém com esse nome tem território.");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (sub.equals("ignorar")) {
            boolean ligado = plugin.territorios().alternarIgnorar(p);
            ok(sender, ligado ? "Ignorando as proteções dos territórios (até sair do servidor)." : "Proteções valendo de novo para você.");
            return;
        }
        Block b = p.getTargetBlockExact(6);
        if (b == null || b.getType() != Material.LODESTONE) { erro(sender, "Olhe para uma magnetita (até 6 blocos)."); return; }
        if (plugin.marcos().eh(b)) { erro(sender, "Essa magnetita já é um Marco."); return; }
        String impedimento = plugin.territorios().podeFundar(p, b);
        if (impedimento != null) { erro(sender, impedimento); return; }
        plugin.marcos().criar(b, p);
        ok(sender, "Marco do Território criado.");
    }

    // ================= locais ocultos, viagem (admin) =================

    private void adminOculto(CommandSender sender, String sub, String[] args) {
        switch (sub) {
            case "locais" -> {
                for (String s : plugin.locais().listar()) sender.sendMessage(Component.text(" • " + s, NamedTextColor.GRAY));
                return;
            }
            case "liberarclasse" -> {
                Classe c = args.length >= 2 ? Classe.porId(args[1]) : null;
                if (c == null || c.tier() != Classe.Tier.LENDARIA) { erro(sender, "Informe uma classe lendária."); return; }
                plugin.classes().liberarLendariaAdmin(c);
                ok(sender, c.nome() + " está livre.");
                return;
            }
            default -> { }
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "revelar" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("todas")) {
                    for (Lenda l : Lenda.values()) plugin.lendas().revelar(p, l);
                    ok(sender, "Todas as lendas reveladas.");
                    return;
                }
                Lenda l = args.length >= 2 ? Lenda.porId(args[1]) : null;
                if (l == null) { erro(sender, "Use: /rpgadmin revelar <lenda|todas>"); return; }
                plugin.lendas().revelar(p, l);
                ok(sender, "Feitos de " + l.nome() + " revelados.");
            }
            case "pedraviagem" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.CHISELED_DEEPSLATE) { erro(sender, "Olhe para uma pedra entalhada de ardósia (até 6 blocos)."); return; }
                if (plugin.pedras().eh(b)) { erro(sender, "Isso já é uma Pedra de Viagem."); return; }
                plugin.pedras().criar(b, p);
                ok(sender, "Pedra de Viagem criada.");
            }
            case "local" -> {
                var t = args.length >= 2 ? br.rpgatributos.oculto.TipoLocal.porId(args[1]) : null;
                if (t == null) {
                    erro(sender, "Use: /rpgadmin local <" + String.join("|", Arrays.stream(br.rpgatributos.oculto.TipoLocal.values())
                            .map(br.rpgatributos.oculto.TipoLocal::id).toList()) + ">");
                    return;
                }
                plugin.locais().criarAqui(t, p.getLocation());
                ok(sender, t.nome() + " construído a 8 blocos (leste) de você. Desça pelo poço.");
            }
            default -> { }
        }
    }

    // ================= lendas (admin) =================

    private void adminLenda(CommandSender sender, String sub, String[] args) {
        Lenda l = args.length >= 2 ? Lenda.porId(args[1]) : null;
        if (l == null) {
            erro(sender, "Use: /rpgadmin " + sub + " <" + String.join("|", Arrays.stream(Lenda.values()).map(Lenda::id).toList()) + ">");
            return;
        }
        if (sub.equals("liberarlenda")) {
            plugin.lendas().liberarAdmin(l);
            ok(sender, l.nome() + " está livre.");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        String erro = plugin.lendas().forjarAdmin(p, l);
        if (erro != null) erro(sender, erro);
    }

    // ================= classes (admin) =================

    private void adminClasse(CommandSender sender, String sub, String[] args) {
        if (sub.equals("classe")) {
            if (args.length < 3) { erro(sender, "Use: /rpgadmin classe <jogador> <" + nomesClasses() + "> [nível]"); return; }
            Player alvo = Bukkit.getPlayerExact(args[1]);
            if (alvo == null) { erro(sender, "Jogador não encontrado."); return; }
            Classe c = Classe.porId(args[2]);
            if (c == null) { erro(sender, "Classe inválida. Use: " + nomesClasses()); return; }
            int nivel = 1;
            if (args.length >= 4) {
                try { nivel = Integer.parseInt(args[3]); } catch (NumberFormatException e) { erro(sender, "Nível inválido."); return; }
            }
            plugin.classes().definir(alvo, c, nivel);
            ok(sender, alvo.getName() + " agora é " + c.nome() + " nível " + Math.max(1, Math.min(20, nivel)) + ".");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (sub.equals("santuario")) {
            Block b = p.getTargetBlockExact(6);
            if (b == null || b.getType() != Material.LECTERN) { erro(sender, "Olhe para um púlpito (até 6 blocos)."); return; }
            if (plugin.santuarios().eh(b)) { erro(sender, "Esse púlpito já é um Santuário."); return; }
            plugin.santuarios().criar(b, p);
            ok(sender, "Santuário das Classes criado.");
            return;
        }
        // prova: pula as tarefas do caminho atual (para testar)
        Classe alvo = plugin.classes().perfil(p).alvo();
        if (alvo == null) { erro(sender, "Escolha um caminho no /classe antes."); return; }
        String erro = plugin.provas().iniciar(p, alvo, true);
        if (erro != null) erro(sender, erro);
    }

    private static String nomesClasses() {
        return String.join("|", Arrays.stream(Classe.values()).map(Classe::id).toList());
    }

    // ================= /masmorra =================

    private void masmorra(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        Masmorra m = plugin.masmorras().de(p);
        if (args.length >= 1 && args[0].equalsIgnoreCase("sair")) {
            if (m == null) { erro(sender, "Você não está numa masmorra."); return; }
            plugin.masmorras().sair(p, m, "Você saiu da masmorra.");
            return;
        }
        if (m == null) {
            sender.sendMessage(Component.text("""
                    ۞ Masmorras: crie um Portal da Masmorra (tijolos de pedra entalhados
                    + 1 olho do ender + 1 bússola, tecla Q) e clique nele.
                    /masmorra sair  (sai da masmorra em que você está)""", NamedTextColor.LIGHT_PURPLE));
            return;
        }
        sender.sendMessage(Component.text("۞ Masmorra " + m.dificuldade().nome() + " (" + m.tema().nome()
                + "). Para sair: /masmorra sair", NamedTextColor.LIGHT_PURPLE));
    }

    private void adminMasmorra(CommandSender sender, String sub, String[] args) {
        if (sub.equals("fecharmasmorras")) {
            int n = plugin.masmorras().quantas();
            plugin.masmorras().encerrarTodas();
            ok(sender, n + " masmorra(s) fechada(s).");
            return;
        }
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        switch (sub) {
            case "portalmasmorra" -> {
                Block b = p.getTargetBlockExact(6);
                if (b == null || b.getType() != Material.CHISELED_STONE_BRICKS) { erro(sender, "Olhe para tijolos de pedra entalhados (até 6 blocos)."); return; }
                if (plugin.portaisMasmorra().eh(b)) { erro(sender, "Isso já é um Portal da Masmorra."); return; }
                plugin.portaisMasmorra().criar(b, p);
                ok(sender, "Portal da Masmorra criado.");
            }
            case "masmorra" -> {
                Dificuldade d = args.length >= 2 ? Dificuldade.porId(args[1]) : null;
                if (d == null) { erro(sender, "Use: /rpgadmin masmorra <facil|normal|dificil|pesadelo>"); return; }
                String erro = plugin.masmorras().abrirAdmin(p, d);
                if (erro != null) erro(sender, erro);
            }
            case "sala" -> adminSala(p, args);
            default -> { }
        }
    }

    private void adminSala(Player p, String[] args) {
        SalasPersonalizadas salas = plugin.masmorras().salas();
        String acao = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        TipoSala tipo = args.length >= 3 ? TipoSala.porId(args[2]) : null;
        switch (acao) {
            case "pos1", "pos2" -> {
                Block b = p.getTargetBlockExact(10);
                Location l = b != null ? b.getLocation() : p.getLocation();
                salas.marcar(p, acao.equals("pos1") ? 0 : 1, l);
                ok(p, "Canto " + acao.charAt(3) + " marcado em " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ() + ".");
            }
            case "modelo" -> {
                if (tipo == null) { erro(p, "Use: /rpgadmin sala modelo <" + nomesTipos() + ">"); return; }
                salas.modelo(p, tipo);
                ok(p, "Moldura de " + tipo.tamanho() + "x" + tipo.altura() + "x" + tipo.tamanho() + " criada e cantos marcados. "
                        + "O verde é onde as portas abrem. Construa dentro e use /rpgadmin sala salvar " + tipo.id() + " <nome>.");
                p.sendMessage(Component.text("Placas dentro da sala viram marcas: [monstro] (2ª linha: tipo, ex. zombie), [bau], "
                        + "[chefe], [entrada]. As placas somem quando a sala é usada.", NamedTextColor.GRAY));
            }
            case "salvar" -> {
                if (tipo == null || args.length < 4) { erro(p, "Use: /rpgadmin sala salvar <" + nomesTipos() + "> <nome>"); return; }
                String erro = salas.salvar(p, tipo, args[3]);
                if (erro != null) erro(p, erro);
                else ok(p, "Sala '" + args[3].toLowerCase(Locale.ROOT) + "' salva como " + tipo.nome() + ". Ela já entra no sorteio.");
            }
            case "lista" -> {
                for (TipoSala t : TipoSala.values()) {
                    List<String> nomes = salas.nomes(t);
                    p.sendMessage(Component.text(t.nome() + " (" + nomes.size() + "): " + (nomes.isEmpty() ? "-" : String.join(", ", nomes)), NamedTextColor.YELLOW));
                }
            }
            case "apagar" -> {
                if (tipo == null || args.length < 4) { erro(p, "Use: /rpgadmin sala apagar <" + nomesTipos() + "> <nome>"); return; }
                if (salas.apagar(tipo, args[3].toLowerCase(Locale.ROOT))) ok(p, "Sala apagada.");
                else erro(p, "Não achei essa sala.");
            }
            default -> p.sendMessage(Component.text("""
                    /rpgadmin sala modelo <tipo>  (moldura do tamanho certo, já com os cantos marcados)
                    /rpgadmin sala pos1 | pos2  (cantos: o bloco que você olha)
                    /rpgadmin sala salvar <tipo> <nome>
                    /rpgadmin sala lista
                    /rpgadmin sala apagar <tipo> <nome>
                    Tipos: inicio, combate, tesouro (19x9x19) e chefe (27x12x27).""", NamedTextColor.YELLOW));
        }
    }

    private static String nomesTipos() {
        return String.join("|", Arrays.stream(TipoSala.values()).map(TipoSala::id).toList());
    }

    // ================= domador (admin) =================

    private void adminDomador(CommandSender sender, String sub, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (sub.equals("altardomador")) {
            Block b = p.getTargetBlockExact(6);
            if (b == null || b.getType() != Material.HAY_BLOCK) { erro(sender, "Olhe para um fardo de feno (até 6 blocos)."); return; }
            if (plugin.altaresDomador().eh(b)) { erro(sender, "Esse feno já é um Altar do Domador."); return; }
            plugin.altaresDomador().criar(b, p);
            ok(sender, "Altar do Domador criado.");
            return;
        }
        Invocavel inv = args.length >= 2 ? invocavel(args[1]) : null;
        if (inv == null) { erro(sender, "Use: /rpgadmin invocar <" + nomesInvocaveis() + ">"); return; }
        String erro = plugin.companheiros().invocar(p, inv, p.getLocation());
        if (erro != null) erro(sender, erro);
    }

    private static Invocavel invocavel(String id) {
        for (Invocavel i : Invocavel.values()) if (i.name().equalsIgnoreCase(id)) return i;
        return null;
    }

    private static String nomesInvocaveis() {
        return String.join("|", Arrays.stream(Invocavel.values()).map(i -> i.name().toLowerCase(Locale.ROOT)).toList());
    }

    /** Lê {@code args[i]} como quantidade (padrão se não informado); -1 se inválido. */
    private static int quantidade(String[] args, int i, int padrao) {
        if (args.length <= i) return padrao;
        try { return Math.max(1, Math.min(64 * 36, Integer.parseInt(args[i]))); }
        catch (NumberFormatException e) { return -1; }
    }

    private static void entregar(Player p, ItemStack item) {
        p.getInventory().addItem(item).values().forEach(s -> p.getWorld().dropItemNaturally(p.getLocation(), s));
    }

    private static String nomesVariedades() {
        return String.join("|", Arrays.stream(Variedade.values()).map(Variedade::id).toList());
    }

    private static String nomesPratos() {
        return String.join("|", Arrays.stream(Prato.values()).map(Prato::id).toList());
    }

    private static String nomesRaros() {
        return String.join(", ", Arrays.stream(Raro.values()).map(Raro::id).toList());
    }

    private static String nomesChefes() {
        return String.join("|", Arrays.stream(Chefe.values()).map(Chefe::id).toList());
    }

    private void infundir(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (args.length < 2) { erro(sender, "Use: /rpgadmin infundir <" + nomesPartes() + ">"); return; }
        ParteCorpo parte = ParteCorpo.porNome(args[1].toUpperCase(Locale.ROOT));
        if (parte == null) { erro(sender, "Parte inválida. Use: " + nomesPartes()); return; }
        Material m = p.getInventory().getItemInMainHand().getType();
        if (!Catalogo.temEssencia(m)) { erro(sender, "O item da sua mão não tem essência."); return; }
        Arcano arc = plugin.arcano();
        arc.perfil(p).nucleos().remove(parte);
        arc.perfil(p).infusoes().put(parte, m);
        arc.aplicarBonus(p);
        arc.salvar(p);
        ok(sender, "Infundido em " + parte.nome() + ": " + Catalogo.de(m));
    }

    private void adminArcano(CommandSender sender, String sub, String[] args) {
        Player alvo;
        if (args.length >= 2) alvo = Bukkit.getPlayerExact(args[1]);
        else alvo = sender instanceof Player p ? p : null;
        if (alvo == null) { erro(sender, "Jogador não encontrado (precisa estar online)."); return; }
        Arcano arc = plugin.arcano();
        switch (sub) {
            case "grimorio" -> {
                alvo.getInventory().addItem(arc.criarGrimorio());
                ok(sender, "Grimório entregue para " + alvo.getName() + ".");
            }
            case "mana" -> {
                arc.encherMana(alvo);
                ok(sender, "Mana de " + alvo.getName() + " cheia.");
            }
            case "descobrir" -> {
                arc.perfil(alvo).descobertas().addAll(Arrays.asList(Receita.values()));
                arc.salvar(alvo);
                ok(sender, "Todas as magias secretas liberadas para " + alvo.getName() + ".");
            }
            default -> { }
        }
    }

    private static String nomesPartes() {
        return String.join(", ", Arrays.stream(ParteCorpo.values()).map(pc -> pc.name().toLowerCase(Locale.ROOT)).toList());
    }

    private static String nomesRaridades() {
        return String.join(", ", Arrays.stream(Raridade.values()).map(Raridade::id).toList());
    }

    private static String nomesSkills() {
        return String.join(", ", Arrays.stream(Skill.values()).map(Skill::id).toList());
    }

    // ================= /chapeu =================

    private void chapeu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        boolean tirar = args.length >= 1 && (args[0].equalsIgnoreCase("tirar") || args[0].equalsIgnoreCase("remover"));
        if (!tirar && p.getInventory().getItemInMainHand().isEmpty()) {
            erro(sender, "Segure o item que quer colocar na cabeça (ou use /cosmeticos).");
            return;
        }
        Chapeu.Resultado r = tirar ? Chapeu.tirar(p) : Chapeu.colocar(p, p.getInventory().getHeldItemSlot());
        if (r.ok()) ok(sender, r.mensagem() + (tirar ? "" : " (/chapeu tirar para remover)"));
        else erro(sender, r.mensagem());
    }

    // ================= /tag =================

    private void tag(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { erro(sender, "Só jogadores."); return; }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Use: /tag <texto>  (cores com &, ex: &6Rei &fdo Minério)  |  /tag remover", NamedTextColor.YELLOW));
            return;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("remover") || args[0].equalsIgnoreCase("reset"))) {
            plugin.tags().setTag(p, null);
            ok(sender, "Tag removida.");
            return;
        }

        String falha = plugin.tags().aplicarTagDigitada(p, String.join(" ", args));
        if (falha != null) { erro(sender, falha); return; }
        ok(sender, "Tag alterada! Os outros jogadores já estão vendo.");
    }

    // ================= tab complete =================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> op = new ArrayList<>();
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "atributos" -> {
                if (args.length == 1) return null; // null = nomes de jogadores
                if (args.length == 2) op.add("chat");
            }
            case "chapeu" -> { if (args.length == 1) op.add("tirar"); }
            case "guia" -> { if (args.length == 1) return plugin.ajuda().completarGuia(args[0]); }
            case "tag" -> { if (args.length == 1) op.add("remover"); }
            case "rpgadmin" -> {
                String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
                if (args.length == 1) {
                    op.addAll(List.of("set", "addxp", "reset", "forjar", "forjaferreiro", "infusor", "infundir",
                            "grimorio", "mana", "descobrir", "altar", "chefe", "evento", "raro", "refino", "conquista",
                            "cozinha", "reciclagem", "variedade", "prato", "adubo", "marco", "ignorar", "apagarterritorio",
                            "altardomador", "invocar", "portalmasmorra", "masmorra", "sala", "fecharmasmorras",
                            "santuario", "classe", "prova", "lenda", "liberarlenda", "revelar", "pedraviagem", "local", "locais",
                            "liberarclasse", "bancadaalquimica", "peixe", "componente", "gema", "elixir", "acessorio", "cajado", "tomo", "maestria", "prefeitura", "colono", "turnocolonia", "nivelcolonia", "guerra", "elite", "ninho", "horda", "chefemundial", "idademundo", "estacao", "festival", "clima", "ceu", "maldicao", "portal", "portaltransbordar", "portalfechar", "portalsortear", "obelisco", "torre", "proficiencia", "vigor", "gancho", "criaturas", "limparcriaturas", "santuariodivino", "circulo", "mesarunica", "devocao", "pedrafilosofal", "runa", "mitrilo", "mapatesouro", "sitio", "reliquia", "cronista", "capitulo", "pacote", "talentopontos", "renascer", "companheironivel", "boneco", "pedraamolar", "fogueira", "bebedouro", "cabanapesca", "cartografo", "ferradura", "ninhopassaro", "animalraro", "barril", "colmeia", "canteiro", "carta", "flecha", "frasco", "erva", "mel", "bebida", "mercador", "encontro", "muda", "fruta", "estrutura", "estruturas", "esquecerestrutura", "reload"));
                } else if (args.length == 2 && List.of("carta", "flecha", "frasco", "erva", "mel", "bebida", "encontro", "muda", "fruta").contains(sub)) {
                    Enum<?>[] vals = switch (sub) {
                        case "carta" -> br.rpgatributos.vida.Carta.values();
                        case "flecha" -> br.rpgatributos.vida.Flechas.Tipo.values();
                        case "frasco" -> br.rpgatributos.vida.Frascos.Tipo.values();
                        case "erva" -> br.rpgatributos.vida.Erva.values();
                        case "mel" -> br.rpgatributos.vida.Mel.values();
                        case "bebida" -> br.rpgatributos.vida.Bebida.values();
                        case "muda", "fruta" -> br.rpgatributos.vida.Fruta.values();
                        default -> new Enum<?>[0];
                    };
                    for (Enum<?> v : vals) op.add(v.name().toLowerCase(Locale.ROOT));
                    if (sub.equals("encontro")) op.addAll(List.of("carroca", "bandidos", "viajante", "estrela"));
                } else if (args.length == 2 && sub.equals("estrutura")) {
                    for (var t : br.rpgatributos.estruturas.Estrutura.values()) op.add(t.id());
                } else if (args.length == 2 && sub.equals("reliquia")) {
                    for (br.rpgatributos.exploracao.Reliquia r : br.rpgatributos.exploracao.Reliquia.values()) op.add(r.id());
                } else if (args.length == 2 && sub.equals("mapatesouro")) {
                    op.addAll(List.of("comum", "raro", "lendario"));
                } else if (args.length == 2 && sub.equals("mitrilo")) {
                    op.addAll(List.of("minerio", "bruto", "lingote"));
                } else if (args.length == 2 && sub.equals("runa")) {
                    for (br.rpgatributos.fe.Runa r : br.rpgatributos.fe.Runa.values()) op.add(r.id());
                } else if (args.length == 3 && sub.equals("proficiencia")) {
                    for (br.rpgatributos.combo.TipoArma t : br.rpgatributos.combo.TipoArma.values()) op.add(t.id());
                } else if (args.length == 2 && sub.equals("portal")) {
                    for (Dificuldade d : Dificuldade.values()) op.add(d.id());
                } else if (args.length == 2 && sub.equals("forjar")) {
                    for (Raridade r : Raridade.values()) op.add(r.id());
                } else if (args.length == 2 && sub.equals("infundir")) {
                    for (ParteCorpo pc : ParteCorpo.values()) op.add(pc.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 2 && sub.equals("chefe")) {
                    for (Chefe c : Chefe.values()) op.add(c.id());
                } else if (args.length == 2 && sub.equals("evento")) {
                    for (Invocacao i : Invocacao.values()) if (i.chefe() == null) op.add(i.id());
                } else if (args.length == 2 && sub.equals("cajado")) {
                    for (Raridade r : Raridade.values()) op.add(r.id());
                } else if (args.length == 2 && sub.equals("tomo")) {
                    for (Receita r : Receita.values()) if (r.forma() == br.rpgatributos.arcano.Forma.PROIBIDA) op.add(r.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 3 && sub.equals("maestria")) {
                    for (br.rpgatributos.arcano.Essencia es : br.rpgatributos.arcano.Essencia.values()) op.add(es.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 2 && sub.equals("peixe")) {
                    for (PeixeRaro pr : PeixeRaro.values()) op.add(pr.id());
                } else if (args.length == 2 && sub.equals("componente")) {
                    for (Reagente r : Reagente.values()) op.add(r.id());
                } else if (args.length == 2 && sub.equals("gema")) {
                    for (Gema g : Gema.values()) op.add(g.id());
                } else if (args.length == 3 && sub.equals("gema")) {
                    op.addAll(List.of("1", "2", "3"));
                } else if (args.length == 2 && sub.equals("elixir")) {
                    for (Elixir x : Elixir.values()) op.add(x.id());
                } else if (args.length == 3 && sub.equals("elixir")) {
                    for (Qualidade q : Qualidade.values()) op.add(q.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 2 && sub.equals("acessorio")) {
                    for (Acessorio a : Acessorio.values()) op.add(a.id());
                } else if (args.length == 2 && sub.equals("raro")) {
                    for (Raro r : Raro.values()) op.add(r.id());
                } else if (args.length == 2 && (sub.equals("lenda") || sub.equals("liberarlenda") || sub.equals("revelar"))) {
                    for (Lenda l : Lenda.values()) op.add(l.id());
                    if (sub.equals("revelar")) op.add("todas");
                } else if (args.length == 2 && sub.equals("local")) {
                    for (var t : br.rpgatributos.oculto.TipoLocal.values()) op.add(t.id());
                } else if (args.length == 2 && sub.equals("liberarclasse")) {
                    for (Classe c : Classe.values()) if (c.tier() == Classe.Tier.LENDARIA) op.add(c.id());
                } else if (args.length == 3 && sub.equals("classe")) {
                    for (Classe c : Classe.values()) op.add(c.id());
                } else if (args.length == 2 && sub.equals("masmorra")) {
                    for (Dificuldade d : Dificuldade.values()) op.add(d.id());
                } else if (args.length == 2 && sub.equals("sala")) {
                    op.addAll(List.of("modelo", "pos1", "pos2", "salvar", "lista", "apagar"));
                } else if (args.length == 3 && sub.equals("sala") && List.of("modelo", "salvar", "apagar").contains(args[1].toLowerCase(Locale.ROOT))) {
                    for (TipoSala t : TipoSala.values()) op.add(t.id());
                } else if (args.length == 4 && sub.equals("sala") && args[1].equalsIgnoreCase("apagar")) {
                    TipoSala t = TipoSala.porId(args[2]);
                    if (t != null) op.addAll(plugin.masmorras().salas().nomes(t));
                } else if (args.length == 2 && sub.equals("invocar")) {
                    for (Invocavel i : Invocavel.values()) op.add(i.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 2 && sub.equals("apagarterritorio")) {
                    for (Territorio t : plugin.territorios().todos()) op.add(t.nomeDono());
                } else if (args.length == 2 && sub.equals("variedade")) {
                    for (Variedade v : Variedade.values()) op.add(v.id());
                } else if (args.length == 2 && sub.equals("prato")) {
                    for (Prato pr : Prato.values()) op.add(pr.id());
                } else if (args.length == 3 && sub.equals("prato")) {
                    for (Qualidade q : Qualidade.values()) op.add(q.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 4 && sub.equals("variedade")) {
                    op.add("semente");
                } else if (args.length == 2 && sub.equals("refino")) {
                    for (int i = 0; i <= 10; i++) op.add(String.valueOf(i));
                } else if (args.length == 2 && sub.equals("conquista")) {
                    op.addAll(List.of("forjados", "forjado_lendario", "forjado_mitico", "refino10", "infusoes", "nucleos",
                            "secretas", "chefes", "ondas", "dragao", "missoes", "contratos", "reciclados", "mutacoes", "banquete",
                            "abates_party", "territorio", "chunks", "companheiros", "voadores", "abates_companheiro",
                            "masmorras", "masmorra_pesadelo", "abates_corpo", "abates_flecha", "magias", "classes",
                            "mortos_vivos", "aranhas", "troncos", "minerios", "abates_jogadores", "illagers", "peixes", "peixes_raros",
                            "tesouros", "especies_peixe", "criaturas_marinhas", "gema_perfeita", "alquimias", "elixires", "acessorios"));
                } else if (args.length == 2 && !List.of("reload", "infusor", "forjaferreiro", "altar", "cozinha", "reciclagem", "adubo",
                        "marco", "ignorar", "altardomador", "portalmasmorra", "fecharmasmorras", "santuario", "prova",
                        "pedraviagem", "locais", "bancadaalquimica", "cajado", "tomo", "prefeitura", "colono", "turnocolonia", "nivelcolonia", "guerra", "elite", "ninho", "horda", "chefemundial", "idademundo", "estacao", "festival", "clima", "ceu", "portaltransbordar", "portalfechar", "portalsortear", "obelisco", "torre", "criaturas", "limparcriaturas", "santuariodivino", "circulo", "mesarunica", "pedrafilosofal", "runa", "mitrilo", "mapatesouro", "sitio", "reliquia", "cronista", "pacote", "companheironivel", "boneco", "pedraamolar", "fogueira", "bebedouro", "cabanapesca", "cartografo", "ferradura", "ninhopassaro", "animalraro", "barril", "colmeia", "canteiro", "carta", "flecha", "frasco", "erva", "mel", "bebida", "mercador", "encontro", "muda", "fruta", "estruturas", "esquecerestrutura").contains(sub)) {
                    return null;
                } else if (args.length == 3 && List.of("set", "addxp", "reset").contains(sub)) {
                    for (Skill s : Skill.values()) op.add(s.id());
                } else if (args.length == 4 && List.of("raro", "conquista").contains(sub)) {
                    return null;
                }
            }
            default -> { }
        }
        String ultimo = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        op.removeIf(s -> !s.startsWith(ultimo));
        return op;
    }
}
