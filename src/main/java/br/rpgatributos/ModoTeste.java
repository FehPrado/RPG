package br.rpgatributos;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * 2.88: a área de testes do admin ({@code /rpgadmin teste}). Um mundo plano à parte onde nada
 * conta: XP, títulos, conquistas (do plugin e do jogo), caçador e recordes ficam parados. Ao entrar,
 * tudo do jogador é guardado num arquivo (inventário, baú do fim, XP, vida, efeitos, posição, modo
 * de jogo e os dados do plugin no jogador); ao sair (pelo comando ou de qualquer outro jeito),
 * tudo volta como estava e o que foi pego no teste fica lá.
 */
public final class ModoTeste implements Listener {

    public static final String MUNDO = "rpg_teste";
    private static final TextColor COR = TextColor.color(0x80DEEA);

    private final RPGAtributos plugin;
    private final File pasta;
    /** Durante a restauração, a troca de mundo não dispara outra restauração. */
    private boolean restaurando;

    public ModoTeste(RPGAtributos plugin) {
        this.plugin = plugin;
        this.pasta = new File(plugin.getDataFolder(), "teste");
    }

    /** Jogadores em teste (com os dados guardados). Valem também dentro das masmorras abertas no teste. */
    private static final java.util.Set<java.util.UUID> ATIVOS = new java.util.HashSet<>();

    /** O jogador está testando (ou a criatura está na área de testes)? Usado para não contar progresso. */
    public static boolean ehTeste(Entity e) {
        if (e == null) return false;
        if (e instanceof Player p && ATIVOS.contains(p.getUniqueId())) return true;
        return e.getWorld().getName().equals(MUNDO);
    }

    public static boolean ehTeste(World w) {
        return w != null && w.getName().equals(MUNDO);
    }

    private File arquivo(Player p) {
        return new File(pasta, p.getUniqueId() + ".yml");
    }

    // =====================================================================
    //  O mundo
    // =====================================================================

    private World mundo() {
        World w = Bukkit.getWorld(MUNDO);
        if (w != null) return w;
        w = new WorldCreator(MUNDO).type(WorldType.FLAT).generateStructures(false).createWorld();
        if (w == null) return null;
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setDifficulty(Difficulty.NORMAL);
        w.setTime(1000);
        w.setStorm(false);
        w.setSpawnLocation(0, w.getHighestBlockYAt(0, 0) + 1, 0);
        return w;
    }

    /** Carrega o mundo de testes se ele já existe (ao ligar o servidor). */
    public void iniciar() {
        File[] arquivos = pasta.listFiles((d, n) -> n.endsWith(".yml"));
        if (arquivos != null) for (File f : arquivos) {
            try { ATIVOS.add(java.util.UUID.fromString(f.getName().replace(".yml", ""))); } catch (IllegalArgumentException ignorado) { }
        }
        if (ATIVOS.isEmpty()) esquecerDados();
        // No Paper 26 os mundos extras ficam em <mundo>/dimensions/minecraft/<nome>, ao lado do overworld.
        File dir = new File(Bukkit.getWorlds().getFirst().getWorldFolder().getParentFile(), MUNDO);
        if (!dir.isDirectory()) dir = new File(Bukkit.getWorldContainer(), MUNDO);
        if (!dir.isDirectory()) return;
        // Ninguém testando: o mundo de testes antigo vai embora. Alguém no meio de um teste: carrega para ele voltar.
        if (ATIVOS.isEmpty()) apagarPasta(dir);
        else mundo();
    }

    /** Quando o último testador sai, o mundo de testes é descarregado e apagado (na próxima vez nasce limpo). */
    private void apagarSeVazio() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World w = Bukkit.getWorld(MUNDO);
            if (w == null || !ATIVOS.isEmpty() || !w.getPlayers().isEmpty()) return;
            File dir = w.getWorldFolder();
            if (!Bukkit.unloadWorld(w, false)) return;
            apagarPasta(dir);
            esquecerDados();
            plugin.getLogger().info("Área de testes vazia: o mundo " + MUNDO + " foi apagado.");
        }, 20L);
    }

    /** 2.99.1: o que foi guardado sobre o mundo de testes (colônias de teste, marcos de estrada) vai embora com ele. */
    private void esquecerDados() {
        int c = plugin.colonias().esquecerMundo(MUNDO);
        int m = plugin.rede().esquecerMundo(MUNDO);
        if (c + m > 0) plugin.getLogger().info("Área de testes: " + c + " colônia(s) e " + m + " marco(s) de estrada esquecidos.");
    }

    private void apagarPasta(File dir) {
        // Só apaga a pasta do mundo de testes, nunca outra.
        if (!dir.getName().equals(MUNDO)) return;
        try (var arvore = java.nio.file.Files.walk(dir.toPath())) {
            arvore.sorted(java.util.Comparator.reverseOrder()).forEach(c -> c.toFile().delete());
        } catch (IOException e) {
            plugin.getLogger().warning("Não deu para apagar o mundo de testes: " + e.getMessage());
        }
    }

    // =====================================================================
    //  Entrar e sair
    // =====================================================================

    /** {@code /rpgadmin teste}: entra, ou sai se já está lá. */
    public void alternar(Player p) {
        if (ATIVOS.contains(p.getUniqueId()) || ehTeste(p.getWorld())) {
            sair(p);
            return;
        }
        if (arquivo(p).exists()) {
            // Sobrou um arquivo (o servidor caiu no meio de um teste): devolve antes de entrar de novo.
            restaurar(p, true);
        }
        World w = mundo();
        if (w == null) {
            p.sendMessage(Component.text("✖ Não deu para criar o mundo de testes.", NamedTextColor.RED));
            return;
        }
        try {
            guardar(p);
        } catch (IOException e) {
            p.sendMessage(Component.text("✖ Não deu para guardar seus dados: " + e.getMessage(), NamedTextColor.RED));
            return;
        }
        restaurando = true;
        try {
            p.teleport(w.getSpawnLocation().add(0.5, 0, 0.5));
        } finally {
            restaurando = false;
        }
        p.setGameMode(GameMode.CREATIVE);
        p.sendMessage(Component.text("⚗ Área de testes", COR, TextDecoration.BOLD));
        p.sendMessage(Component.text("Aqui nada conta: XP, títulos, conquistas, caçador e recordes ficam parados. "
                + "Você entrou com uma cópia do seu inventário e está no criativo (/gamemode survival para testar como jogador).", NamedTextColor.GRAY));
        p.sendMessage(Component.text("Para sair: /rpgadmin teste. Tudo volta como estava e o que você pegou aqui fica aqui. "
                + "/rpgadmin teste limpar tira as criaturas e itens soltos perto de você.", NamedTextColor.GRAY));
    }

    /** Sai do teste (do mundo de testes ou de uma masmorra aberta nele) e volta para onde estava antes. */
    private void sair(Player p) {
        if (!arquivo(p).exists()) {
            p.sendMessage(Component.text("✖ Não achei seus dados guardados; levando você para o mundo principal.", NamedTextColor.RED));
            p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            return;
        }
        restaurar(p, true);
        p.sendMessage(Component.text("⚗ Você saiu da área de testes. Tudo voltou como estava.", COR));
    }

    // =====================================================================
    //  Cenário: duas colônias ligadas por estrada (2.99.1)
    // =====================================================================

    private static final String[] VILAS = {"Aurora", "Brisa"};

    /**
     * {@code /rpgadmin teste estrada}: duas colônias de teste (Aurora e Brisa, a 90 blocos uma da outra), cada uma
     * com depósito cheio de sobras, ligadas por uma Estrada Real iluminada com um marco em cada ponta.
     * A estrada segue para onde o jogador olha, passando por ele. @return o aviso de erro, ou null.
     */
    public String montarEstrada(Player p) {
        World w = p.getWorld();
        if (!ehTeste(w)) return "Só dentro da área de testes (/rpgadmin teste).";
        // Montado de novo: as colônias de antes saem (a prefeitura delas some e os moradores também).
        for (String nome : VILAS) {
            br.rpgatributos.colonia.Colonia velha = plugin.colonias().de(idVila(nome));
            if (velha == null) continue;
            for (br.rpgatributos.colonia.Colonia.Cidadao ci : velha.cidadaos()) {
                Entity e = Bukkit.getEntity(ci.entidade());
                if (e != null) e.remove();
            }
            Location l = velha.prefeitura();
            if (l != null) {
                plugin.prefeituras().remover(l, false); // a colônia acaba junto com a prefeitura
                l.getBlock().setType(org.bukkit.Material.AIR);
            }
        }
        org.bukkit.block.BlockFace f = p.getFacing();
        int dx = f.getModX(), dz = f.getModZ();
        int lx = -dz, lz = dx; // para o lado (esquerda de quem olha)
        int x0 = p.getLocation().getBlockX(), z0 = p.getLocation().getBlockZ();
        int y = w.getHighestBlockYAt(x0, z0);
        java.util.function.BiFunction<Integer, Integer, org.bukkit.block.Block> em =
                (t, lado) -> w.getBlockAt(x0 + dx * t + lx * lado, y, z0 + dz * t + lz * lado);
        // A estrada: 3 de largura, de -48 a 48, com lanternas a cada 8 blocos do outro lado.
        for (int t = -48; t <= 48; t++) {
            for (int lado = -1; lado <= 1; lado++) {
                org.bukkit.block.Block b = em.apply(t, lado);
                b.setType(org.bukkit.Material.POLISHED_ANDESITE, false);
                b.getRelative(0, 1, 0).setType(org.bukkit.Material.AIR, false);
                b.getRelative(0, 2, 0).setType(org.bukkit.Material.AIR, false);
            }
            if (t % 8 == 0) {
                org.bukkit.block.Block poste = em.apply(t, -2).getRelative(0, 1, 0);
                poste.setType(org.bukkit.Material.COBBLESTONE_WALL);
                poste.getRelative(0, 1, 0).setType(org.bukkit.Material.LANTERN);
            }
        }
        // As duas colônias, uma em cada ponta, do lado de cá da estrada.
        org.bukkit.block.Block[] marcos = new org.bukkit.block.Block[2];
        for (int i = 0; i < 2; i++) {
            int t = i == 0 ? -45 : 45;
            org.bukkit.block.Block sino = em.apply(t, 5).getRelative(0, 1, 0);
            sino.setType(org.bukkit.Material.BELL);
            org.bukkit.block.Block bau = em.apply(t + 2, 5).getRelative(0, 1, 0);
            bau.setType(org.bukkit.Material.CHEST);
            if (bau.getState(false) instanceof org.bukkit.block.Chest ch) { // sem cópia: os itens vão direto para o baú
                for (int k = 0; k < 6; k++) ch.getBlockInventory().addItem(new ItemStack(org.bukkit.Material.WHEAT, 64));
                for (int k = 0; k < 6; k++) ch.getBlockInventory().addItem(new ItemStack(org.bukkit.Material.COBBLESTONE, 64));
                for (int k = 0; k < 2; k++) ch.getBlockInventory().addItem(new ItemStack(org.bukkit.Material.OAK_LOG, 64));
            }
            plugin.prefeituras().criar(sino, null);
            plugin.colonias().fundarTeste(idVila(VILAS[i]), VILAS[i], sino, bau);
            plugin.prefeituras().atualizarDisplays(sino.getLocation());
            org.bukkit.block.Block placa = em.apply(i == 0 ? -43 : 43, 2).getRelative(0, 1, 0);
            placa.setType(org.bukkit.Material.OAK_SIGN);
            marcos[i] = placa;
        }
        // Os marcos esperam a luz das lanternas assentar (a iluminação entra na conta do trecho).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int ligados = 0;
            for (org.bukkit.block.Block m : marcos) ligados = Math.max(ligados, plugin.rede().marcar(m, "Estrada de Teste"));
            p.sendMessage(Component.text(ligados > 0
                    ? "✔ Estrada de Teste ligada: Colônia de Aurora ↔ Colônia de Brisa."
                    : "⚠ Os marcos não se ligaram; clique num deles para conferir.", ligados > 0 ? COR : NamedTextColor.GOLD));
        }, 40L);
        p.sendMessage(Component.text("⚗ Cenário de estrada montado", COR, TextDecoration.BOLD));
        p.sendMessage(Component.text("Aurora fica atrás de você e Brisa à frente (90 blocos), cada uma com 2 moradores e um depósito com sobras. "
                + "Na prefeitura delas você mexe como dono. Teste: /rpgadmin estrada caravana (da colônia mais perto), "
                + "/rpgadmin estrada assalto, /rpgadmin andarilho visita e /rpgadmin andarilho praca. Tudo some quando a área de testes é apagada.", NamedTextColor.GRAY));
        return null;
    }

    private static java.util.UUID idVila(String nome) {
        return java.util.UUID.nameUUIDFromBytes(("rpg_teste:" + nome).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** {@code /rpgadmin teste limpar}: tira criaturas (menos jogadores) e itens soltos a até 64 blocos. */
    public int limpar(Player p) {
        if (!ehTeste(p)) return -1;
        int n = 0;
        org.bukkit.NamespacedKey morador = new org.bukkit.NamespacedKey(plugin, "cidadao_colonia");
        for (Entity e : p.getNearbyEntities(64, 64, 64)) {
            // Os moradores das colônias de teste ficam (sem eles a colônia esvazia).
            if (e instanceof Player || e.getPersistentDataContainer().has(morador)) continue;
            e.remove();
            n++;
        }
        return n;
    }

    // =====================================================================
    //  Guardar e restaurar
    // =====================================================================

    private void guardar(Player p) throws IOException {
        pasta.mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        y.set("local", p.getLocation());
        y.set("modo", p.getGameMode().name());
        y.set("inventario", base64(ItemStack.serializeItemsAsBytes(p.getInventory().getContents())));
        y.set("bau-do-fim", base64(ItemStack.serializeItemsAsBytes(p.getEnderChest().getContents())));
        y.set("nivel", p.getLevel());
        y.set("exp", (double) p.getExp());
        y.set("vida", p.getHealth());
        y.set("fome", p.getFoodLevel());
        y.set("saturacao", (double) p.getSaturation());
        y.set("efeitos", List.copyOf(p.getActivePotionEffects()));
        y.set("voo", p.getAllowFlight());
        y.set("dados", base64(p.getPersistentDataContainer().serializeToBytes()));
        y.save(arquivo(p));
        ATIVOS.add(p.getUniqueId());
    }

    /** Devolve tudo do arquivo. {@code levar}: volta também para onde estava. */
    private void restaurar(Player p, boolean levar) {
        File f = arquivo(p);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        try {
            p.getPersistentDataContainer().readFromBytes(Base64.getDecoder().decode(y.getString("dados", "")), true);
        } catch (IOException | IllegalArgumentException e) {
            plugin.getLogger().warning("Teste: não deu para devolver os dados de " + p.getName() + ": " + e.getMessage());
        }
        p.getInventory().setContents(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(y.getString("inventario", ""))));
        p.getEnderChest().setContents(ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(y.getString("bau-do-fim", ""))));
        for (PotionEffect ef : p.getActivePotionEffects()) p.removePotionEffect(ef.getType());
        for (Object o : y.getList("efeitos", List.of())) if (o instanceof PotionEffect ef) p.addPotionEffect(ef);
        p.setLevel(y.getInt("nivel"));
        p.setExp((float) y.getDouble("exp"));
        p.setFoodLevel(y.getInt("fome", 20));
        p.setSaturation((float) y.getDouble("saturacao", 5));
        try {
            p.setGameMode(GameMode.valueOf(y.getString("modo", "SURVIVAL")));
        } catch (IllegalArgumentException e) {
            p.setGameMode(GameMode.SURVIVAL);
        }
        p.setAllowFlight(y.getBoolean("voo") || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
        if (levar) {
            Location l = y.getLocation("local");
            if (l == null || l.getWorld() == null || ehTeste(l.getWorld())) l = Bukkit.getWorlds().getFirst().getSpawnLocation();
            restaurando = true;
            try {
                p.teleport(l);
            } finally {
                restaurando = false;
            }
        }
        double max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        p.setHealth(Math.max(1, Math.min(max, y.getDouble("vida", max))));
        f.delete();
        ATIVOS.remove(p.getUniqueId());
        p.updateInventory();
        apagarSeVazio();
    }

    private static String base64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    // =====================================================================
    //  Proteções
    // =====================================================================

    /** Saiu da área de testes por outro caminho (outro comando, portal, morte): devolve tudo mesmo assim. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void aoTrocarMundo(PlayerChangedWorldEvent e) {
        if (restaurando) return;
        Player p = e.getPlayer();
        // Masmorras abertas no teste continuam sendo teste; qualquer outro mundo encerra.
        boolean continuaTestando = ehTeste(p.getWorld()) || plugin.masmorras().ehMundo(p.getWorld());
        if (ATIVOS.contains(p.getUniqueId()) && !continuaTestando && arquivo(p).exists()) {
            restaurar(p, false);
            p.sendMessage(Component.text("⚗ Você saiu da área de testes: seus itens e dados voltaram.", COR));
        } else if (ehTeste(p.getWorld()) && !arquivo(p).exists()) {
            // Entrou sem o comando (sem nada guardado): não fica.
            p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            p.sendMessage(Component.text("✖ A área de testes só abre com /rpgadmin teste.", NamedTextColor.RED));
        }
    }

    /** Caiu o servidor (ou saiu) no meio do teste: ao voltar, tudo é devolvido. */
    @EventHandler
    public void aoEntrar(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!arquivo(p).exists()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || !arquivo(p).exists()) return;
            restaurar(p, true);
            p.sendMessage(Component.text("⚗ Você estava na área de testes: tudo voltou como estava antes dela.", COR));
        });
    }

    /** Morreu no teste: nasce de novo no teste (com o inventário, que lá não se perde). */
    @EventHandler
    public void aoRenascer(PlayerRespawnEvent e) {
        if (ehTeste(e.getPlayer()) && arquivo(e.getPlayer()).exists()) {
            World w = mundo();
            if (w != null) e.setRespawnLocation(w.getSpawnLocation().add(0.5, 0, 0.5));
        }
    }

    /** As conquistas do jogo também não contam. */
    @EventHandler(ignoreCancelled = true)
    public void aoConquistar(PlayerAdvancementCriterionGrantEvent e) {
        if (ehTeste(e.getPlayer())) e.setCancelled(true);
    }
}
