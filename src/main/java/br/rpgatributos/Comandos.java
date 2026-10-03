package br.rpgatributos;

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
                if (sender instanceof Player p) plugin.guia().dar(p);
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
            case "tag" -> { if (args.length == 1) op.add("remover"); }
            case "rpgadmin" -> {
                String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
                if (args.length == 1) {
                    op.addAll(List.of("set", "addxp", "reset", "forjar", "forjaferreiro", "infusor", "infundir",
                            "grimorio", "mana", "descobrir", "altar", "chefe", "evento", "raro", "refino", "conquista",
                            "cozinha", "reciclagem", "variedade", "prato", "adubo", "marco", "ignorar", "apagarterritorio",
                            "altardomador", "invocar", "portalmasmorra", "masmorra", "sala", "fecharmasmorras",
                            "santuario", "classe", "prova", "lenda", "liberarlenda", "revelar", "pedraviagem", "local", "locais",
                            "liberarclasse", "reload"));
                } else if (args.length == 2 && sub.equals("forjar")) {
                    for (Raridade r : Raridade.values()) op.add(r.id());
                } else if (args.length == 2 && sub.equals("infundir")) {
                    for (ParteCorpo pc : ParteCorpo.values()) op.add(pc.name().toLowerCase(Locale.ROOT));
                } else if (args.length == 2 && sub.equals("chefe")) {
                    for (Chefe c : Chefe.values()) op.add(c.id());
                } else if (args.length == 2 && sub.equals("evento")) {
                    for (Invocacao i : Invocacao.values()) if (i.chefe() == null) op.add(i.id());
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
                            "mortos_vivos", "aranhas", "troncos", "minerios", "abates_jogadores", "illagers"));
                } else if (args.length == 2 && !List.of("reload", "infusor", "forjaferreiro", "altar", "cozinha", "reciclagem", "adubo",
                        "marco", "ignorar", "altardomador", "portalmasmorra", "fecharmasmorras", "santuario", "prova",
                        "pedraviagem", "locais").contains(sub)) {
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
