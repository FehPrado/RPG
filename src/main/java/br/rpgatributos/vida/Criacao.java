package br.rpgatributos.vida;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Skill;
import br.rpgatributos.domador.Companheiros;
import br.rpgatributos.exploracao.PacoteRecursos;
import br.rpgatributos.fazenda.Qualidade;
import br.rpgatributos.mundo.Estacoes.Estacao;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Criação de animais (como em Harvest Moon e Stardew Valley): agachado + clique com a mão vazia num
 * animal da fazenda e ele passa a ser seu. Cada um tem afeto (0 a 5 corações), que sobe com carinho
 * e comida todo dia (ração na mão, pasto ao ar livre ou fardo de feno por perto) e cai quando ele é
 * esquecido ou passa a noite de inverno no frio. Bem cuidado, ele produz todo dia (ovo, leite, trufa,
 * lã, pele), com qualidade que sobe com o afeto.
 */
public final class Criacao implements Listener, CommandExecutor {

    public static final TextColor COR = TextColor.color(0xF4A261);
    public static final NamespacedKey K_PRODUTO = new NamespacedKey("rpgatributos", "produto_animal");
    private static final int MAX_POR_JOGADOR = 40;
    private static final int AFETO_MAX = 1000;
    /** Porções de comida de um fardo de feno (depois ele acaba). */
    private static final int PORCOES_FENO = 9;
    private static final String[] NOMES = {"Mimosa", "Estrela", "Pipoca", "Malhada", "Bolinha", "Florzinha", "Pintada", "Fubá", "Paçoca",
            "Canjica", "Mel", "Cacau", "Nuvem", "Algodão", "Biscoito", "Pérola", "Amora", "Cereja", "Jujuba", "Quindim", "Tapioca", "Brigadeiro",
            "Princesa", "Barão", "Trovão", "Valente", "Sardinha", "Rosinha", "Neve", "Lua", "Sol", "Bento", "Dengosa", "Pandora", "Faísca"};

    /** Os animais que dá para criar e o que comem. */
    public enum Especie {
        GALINHA("Galinha", EntityType.CHICKEN, Material.CHICKEN_SPAWN_EGG, "um ovo por dia (dois com 4 corações)",
                Set.of(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.TORCHFLOWER_SEEDS)),
        VACA("Vaca", EntityType.COW, Material.COW_SPAWN_EGG, "leite todo dia (balde vazio)", Set.of(Material.WHEAT)),
        VACA_COGUMELO("Vaca-Cogumelo", EntityType.MOOSHROOM, Material.MOOSHROOM_SPAWN_EGG, "leite todo dia (balde vazio)", Set.of(Material.WHEAT)),
        CABRA("Cabra", EntityType.GOAT, Material.GOAT_SPAWN_EGG, "leite de cabra todo dia (balde vazio)", Set.of(Material.WHEAT)),
        OVELHA("Ovelha", EntityType.SHEEP, Material.SHEEP_SPAWN_EGG, "lã de qualidade (tosquia)", Set.of(Material.WHEAT)),
        PORCO("Porco", EntityType.PIG, Material.PIG_SPAWN_EGG, "fuça trufas ao ar livre (menos no inverno)",
                Set.of(Material.CARROT, Material.POTATO, Material.BEETROOT)),
        COELHO("Coelho", EntityType.RABBIT, Material.RABBIT_SPAWN_EGG, "às vezes pele ou pé de coelho",
                Set.of(Material.CARROT, Material.GOLDEN_CARROT, Material.DANDELION));

        private final String nome, produz;
        private final EntityType tipo;
        private final Material ovo;
        private final Set<Material> comida;

        Especie(String nome, EntityType tipo, Material ovo, String produz, Set<Material> comida) {
            this.nome = nome;
            this.tipo = tipo;
            this.ovo = ovo;
            this.produz = produz;
            this.comida = comida;
        }

        public String nome() { return nome; }
        public String produz() { return produz; }
        public boolean leite() { return this == VACA || this == VACA_COGUMELO || this == CABRA; }

        static Especie de(EntityType t) {
            for (Especie e : values()) if (e.tipo == t) return e;
            return null;
        }
    }

    /** Um animal da fazenda de alguém. Os "dias" são os dias do mundo (24 000 ticks). */
    private static final class Animal {
        UUID id, dono;
        String nome, mundo;
        Especie especie;
        int afeto = 200;
        long carinho = -1, comida = -1, pasto = -1, ordenha = -1;
        boolean leitePronto;
    }

    private static final class Tela implements InventoryHolder {
        Inventory inventario;

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Animal> animais = new LinkedHashMap<>();
    /** Fardos de feno que já deram comida: "mundo,x,y,z" → porções que sobram. */
    private final Map<String, Integer> fenos = new HashMap<>();
    /** Último dia visto em cada mundo (para saber quando o dia vira). */
    private final Map<String, Long> diaDoMundo = new HashMap<>();
    private boolean sujo;
    private int ciclos;

    public Criacao(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "criacao.yml");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    private static long dia(World w) { return w.getFullTime() / 24000L; }

    // =====================================================================
    //  Itens dos animais
    // =====================================================================

    /** Trufa: o porco acha fuçando a terra. Vale muito e entra em pratos finos. */
    public static ItemStack trufa(int qtd) {
        ItemStack i = new ItemStack(Material.CLAY_BALL, qtd);
        i.editMeta(m -> {
            m.itemName(Component.text("Trufa", TextColor.color(0x8D6E63)));
            m.lore(List.of(Component.text("Um porco achou fuçando a terra.", NamedTextColor.GRAY),
                            Component.text("Ingrediente fino da Cozinha.", NamedTextColor.DARK_GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_PRODUTO, PersistentDataType.STRING, "trufa");
            PacoteRecursos.marcar(m, "produto_trufa");
        });
        return i;
    }

    /** Leite de cabra (vira queijo de cabra no barril). */
    public static ItemStack leiteDeCabra() {
        ItemStack i = new ItemStack(Material.MILK_BUCKET);
        i.editMeta(m -> {
            m.itemName(Component.text("Leite de Cabra", TextColor.color(0xFFF8E1)));
            m.lore(List.of(Component.text("Fresquinho da cabra.", NamedTextColor.GRAY))
                    .stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            m.getPersistentDataContainer().set(K_PRODUTO, PersistentDataType.STRING, "leite_cabra");
            PacoteRecursos.marcar(m, "produto_leite_cabra");
        });
        return i;
    }

    /** "trufa", "leite_cabra" ou null. */
    public static String produto(ItemStack i) {
        if (i == null || i.isEmpty() || !i.hasItemMeta()) return null;
        return i.getPersistentDataContainer().get(K_PRODUTO, PersistentDataType.STRING);
    }

    // =====================================================================
    //  Afeto e nome
    // =====================================================================

    private static int coracoes(Animal a) { return Math.min(5, a.afeto / 200); }

    private static String textoCoracoes(Animal a) {
        int c = coracoes(a);
        return "❤".repeat(c) + "♡".repeat(5 - c);
    }

    private void renomear(Animal a, Entity e) {
        e.customName(Component.text(a.nome + " ", NamedTextColor.WHITE).append(Component.text(textoCoracoes(a), NamedTextColor.RED)));
    }

    private void mudarAfeto(Animal a, int delta) {
        a.afeto = Math.max(0, Math.min(AFETO_MAX, a.afeto + delta));
        sujo = true;
        Entity e = Bukkit.getEntity(a.id);
        if (e != null) renomear(a, e);
    }

    /** Qualidade do produto: o afeto pesa mais, o nível de Doma do dono ajuda. */
    private Qualidade qualidade(Animal a) {
        double fr = 0.65 * a.afeto / AFETO_MAX;
        Player dono = Bukkit.getPlayer(a.dono);
        if (dono != null) fr += 0.35 * Math.min(1, plugin.stats().getNivel(dono, Skill.DOMA) / (double) plugin.settings().nivelMaximo);
        return Qualidade.sortear(fr);
    }

    private ItemStack comQualidade(ItemStack i, Animal a) {
        qualidade(a).aplicar(i);
        return i;
    }

    private boolean podeCuidar(Player p, Animal a, Entity e) {
        return p.getUniqueId().equals(a.dono) || plugin.territorios().podeConstruir(p, e.getLocation());
    }

    // =====================================================================
    //  Cuidar: adotar, carinho, comida, ordenhar, nome
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoInteragir(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity ent = e.getRightClicked();
        Especie esp = Especie.de(ent.getType());
        if (esp == null) return;
        Player p = e.getPlayer();
        ItemStack mao = p.getInventory().getItemInMainHand();
        Animal a = animais.get(ent.getUniqueId());
        if (a == null) {
            if (!p.isSneaking() || !mao.isEmpty()) return;
            e.setCancelled(true);
            adotar(p, ent, esp);
            return;
        }
        if (mao.isEmpty()) {
            e.setCancelled(true);
            if (!podeCuidar(p, a, ent)) {
                p.sendActionBar(Component.text(a.nome + " é da fazenda de " + nomeDono(a) + ".", NamedTextColor.GRAY));
                return;
            }
            carinho(p, a, ent);
            return;
        }
        if (mao.getType() == Material.NAME_TAG && mao.hasItemMeta() && mao.getItemMeta().hasDisplayName()) {
            e.setCancelled(true);
            if (!podeCuidar(p, a, ent)) return;
            String nome = PlainTextComponentSerializer.plainText().serialize(mao.getItemMeta().displayName()).trim();
            if (nome.isEmpty() || nome.length() > 20) return;
            a.nome = nome;
            sujo = true;
            renomear(a, ent);
            if (p.getGameMode() != GameMode.CREATIVE) mao.setAmount(mao.getAmount() - 1);
            p.sendActionBar(Component.text("Agora se chama " + nome + ".", COR));
            return;
        }
        if (mao.getType() == Material.BUCKET && esp.leite()) {
            e.setCancelled(true);
            if (!podeCuidar(p, a, ent)) return;
            ordenhar(p, a, ent, mao);
            return;
        }
        if (esp.comida.contains(mao.getType())) {
            if (!podeCuidar(p, a, ent)) return;
            long hoje = dia(ent.getWorld());
            if (a.comida != hoje) {
                a.comida = hoje;
                mudarAfeto(a, 5);
                p.sendActionBar(Component.text(a.nome + " comeu e ficou feliz " + textoCoracoes(a), COR));
                ent.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ent.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0);
            }
            // Não cancela: o jogo come o item (e o animal pode cruzar).
        }
    }

    private String nomeDono(Animal a) {
        var off = Bukkit.getOfflinePlayer(a.dono);
        return off.getName() == null ? "alguém" : off.getName();
    }

    private void adotar(Player p, Entity ent, Especie esp) {
        if (ent instanceof Ageable ag && !ag.isAdult()) { p.sendActionBar(Component.text("Espere crescer para adotar.", NamedTextColor.GRAY)); return; }
        if (ent.getPersistentDataContainer().has(Companheiros.CHAVE_DONO)) {
            p.sendActionBar(Component.text("Esse é um companheiro do Domador.", NamedTextColor.GRAY));
            return;
        }
        if (!plugin.territorios().podeConstruir(p, ent.getLocation())) {
            p.sendActionBar(Component.text("✖ Esse animal está no território de outra pessoa.", NamedTextColor.RED));
            return;
        }
        int meus = 0;
        for (Animal x : animais.values()) if (x.dono.equals(p.getUniqueId())) meus++;
        if (meus >= MAX_POR_JOGADOR) { p.sendActionBar(Component.text("✖ Você já cria " + MAX_POR_JOGADOR + " animais.", NamedTextColor.RED)); return; }
        Animal a = registrar(ent, esp, p.getUniqueId(), 200);
        if (ent instanceof LivingEntity le) le.setRemoveWhenFarAway(false);
        ent.getWorld().spawnParticle(Particle.HEART, ent.getLocation().add(0, 1, 0), 5, 0.4, 0.3, 0.4, 0);
        ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
        p.sendMessage(Component.text("🐾 " + a.nome + " (" + esp.nome() + ") agora é da sua fazenda! ", COR, TextDecoration.BOLD)
                .append(Component.text("Faça carinho (clique com a mão vazia) e dê comida todo dia. Produz " + esp.produz() + ". /animais mostra todos.",
                        NamedTextColor.GRAY)));
        plugin.diario().marco(p, "primeiro_animal", "Começou a criar animais com " + a.nome);
    }

    private Animal registrar(Entity ent, Especie esp, UUID dono, int afeto) {
        Animal a = new Animal();
        a.id = ent.getUniqueId();
        a.dono = dono;
        a.especie = esp;
        a.mundo = ent.getWorld().getName();
        a.nome = NOMES[rnd().nextInt(NOMES.length)];
        a.afeto = afeto;
        animais.put(a.id, a);
        ent.setPersistent(true);
        renomear(a, ent);
        sujo = true;
        return a;
    }

    private void carinho(Player p, Animal a, Entity ent) {
        long hoje = dia(ent.getWorld());
        if (a.carinho == hoje) {
            p.sendActionBar(Component.text(a.nome + " " + textoCoracoes(a) + " · hoje: carinho ✔, comida " + (comeuHoje(a, hoje) ? "✔" : "✖")
                    + (a.especie.leite() ? ", leite " + (a.leitePronto ? "pronto" : "amanhã") : ""), COR));
            return;
        }
        a.carinho = hoje;
        mudarAfeto(a, 15);
        ent.getWorld().spawnParticle(Particle.HEART, ent.getLocation().add(0, 1.1, 0), 3, 0.3, 0.2, 0.3, 0);
        ent.getWorld().playSound(ent.getLocation(), som(a.especie), 0.8f, 1.2f);
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.DOMA, 2);
        p.sendActionBar(Component.text("❤ Carinho em " + a.nome + " " + textoCoracoes(a)
                + (comeuHoje(a, hoje) ? "" : " · ainda não comeu hoje"), COR));
    }

    private static boolean comeuHoje(Animal a, long hoje) { return a.comida == hoje || a.pasto == hoje; }

    private static Sound som(Especie e) {
        return switch (e) {
            case GALINHA -> Sound.ENTITY_CHICKEN_AMBIENT;
            case VACA, VACA_COGUMELO -> Sound.ENTITY_COW_AMBIENT;
            case CABRA -> Sound.ENTITY_GOAT_AMBIENT;
            case OVELHA -> Sound.ENTITY_SHEEP_AMBIENT;
            case PORCO -> Sound.ENTITY_PIG_AMBIENT;
            case COELHO -> Sound.ENTITY_RABBIT_AMBIENT;
        };
    }

    private void ordenhar(Player p, Animal a, Entity ent, ItemStack balde) {
        if (!a.leitePronto) {
            p.sendActionBar(Component.text(a.nome + " ainda não tem leite: com comida hoje, amanhã tem.", NamedTextColor.GRAY));
            return;
        }
        a.leitePronto = false;
        a.ordenha = dia(ent.getWorld());
        sujo = true;
        ItemStack leite = comQualidade(a.especie == Especie.CABRA ? leiteDeCabra() : new ItemStack(Material.MILK_BUCKET), a);
        if (p.getGameMode() != GameMode.CREATIVE) balde.setAmount(balde.getAmount() - 1);
        for (ItemStack sobra : p.getInventory().addItem(leite).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
        ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_COW_MILK, 1f, 1f);
        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) plugin.stats().darXp(p, Skill.DOMA, 5);
        mudarAfeto(a, 2);
    }

    /** Tosquia: a lã sai com qualidade; com 3 corações ou mais, vem uma a mais. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoTosquiar(PlayerShearEntityEvent e) {
        Animal a = animais.get(e.getEntity().getUniqueId());
        if (a == null) return;
        if (!podeCuidar(e.getPlayer(), a, e.getEntity())) { e.setCancelled(true); return; }
        List<ItemStack> novas = new ArrayList<>();
        for (ItemStack d : e.getDrops()) novas.add(comQualidade(d.clone(), a));
        if (coracoes(a) >= 3 && !novas.isEmpty()) {
            ItemStack extra = novas.getFirst().clone();
            extra.setAmount(1);
            novas.add(extra);
        }
        e.setDrops(novas);
        mudarAfeto(a, 2);
        if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) plugin.stats().darXp(e.getPlayer(), Skill.DOMA, 5);
    }

    /** Filhotes de dois animais do mesmo dono nascem já na fazenda (na primavera, às vezes gêmeos). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCruzar(EntityBreedEvent e) {
        Animal mae = animais.get(e.getMother().getUniqueId()), pai = animais.get(e.getFather().getUniqueId());
        if (mae == null || pai == null || !mae.dono.equals(pai.dono)) return;
        Entity filhote = e.getEntity();
        Animal f = registrar(filhote, mae.especie, mae.dono, (mae.afeto + pai.afeto) / 4);
        Player dono = Bukkit.getPlayer(mae.dono);
        boolean gemeos = plugin.estacoes().atual() == Estacao.PRIMAVERA && rnd().nextDouble() < 0.25;
        if (gemeos) {
            Entity outro = filhote.getWorld().spawnEntity(filhote.getLocation(), filhote.getType());
            if (outro instanceof Ageable ag) ag.setBaby();
            registrar(outro, mae.especie, mae.dono, f.afeto);
        }
        if (dono != null) {
            dono.sendMessage(Component.text("🐾 Nasceu " + (gemeos ? "um casal de gêmeos" : f.nome) + " na sua fazenda (" + mae.especie.nome() + ")!", COR));
            plugin.stats().darXp(dono, Skill.DOMA, 10);
        }
    }

    @EventHandler
    public void aoMorrer(EntityDeathEvent e) {
        Animal a = animais.remove(e.getEntity().getUniqueId());
        if (a == null) return;
        sujo = true;
        Player dono = Bukkit.getPlayer(a.dono);
        if (dono != null) dono.sendMessage(Component.text("🐾 " + a.nome + " (" + a.especie.nome() + ") morreu.", NamedTextColor.GRAY));
    }

    /** Ninguém de fora machuca os animais da sua fazenda. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoFerir(EntityDamageByEntityEvent e) {
        Animal a = animais.get(e.getEntity().getUniqueId());
        if (a == null) return;
        Player atacante = e.getDamager() instanceof Player p ? p
                : e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player ps ? ps : null;
        if (atacante != null && !podeCuidar(atacante, a, e.getEntity())) e.setCancelled(true);
    }

    // =====================================================================
    //  O dia: pasto, virada do dia, produtos
    // =====================================================================

    /** Roda a cada segundo. */
    public void tick() {
        ciclos++;
        for (World w : Bukkit.getWorlds()) {
            long d = dia(w);
            Long antes = diaDoMundo.put(w.getName(), d);
            if (antes != null && antes != d) virarDia(w, d);
        }
        if (ciclos % 30 == 0) pastar();
        if (sujo && ciclos % 60 == 0) salvar();
    }

    /** Ao ar livre, de dia, em cima da grama (menos no inverno): o animal pastou e não precisa de ração hoje. */
    private void pastar() {
        boolean inverno = plugin.estacoes().atual() == Estacao.INVERNO;
        if (inverno) return;
        for (Animal a : animais.values()) {
            if (!(Bukkit.getEntity(a.id) instanceof LivingEntity e) || !e.isValid()) continue;
            World w = e.getWorld();
            long hora = w.getTime();
            if (hora > 12000) continue;
            Block pe = e.getLocation().getBlock();
            if (pe.getLightFromSky() < 14 || pe.getRelative(0, -1, 0).getType() != Material.GRASS_BLOCK) continue;
            long hoje = dia(w);
            if (a.pasto != hoje) {
                a.pasto = hoje;
                sujo = true;
            }
        }
    }

    /** Começou um dia novo nesse mundo: conta o dia de ontem de cada animal (os que estão carregados). */
    private void virarDia(World w, long hoje) {
        long ontem = hoje - 1;
        Estacao estacao = plugin.estacoes().atual();
        for (Animal a : new ArrayList<>(animais.values())) {
            if (!a.mundo.equals(w.getName()) || !(Bukkit.getEntity(a.id) instanceof LivingEntity e) || !e.isValid()) continue;
            boolean comeu = a.comida == ontem || a.pasto == ontem || comerFeno(e);
            boolean carinho = a.carinho == ontem;
            int delta = (carinho ? 0 : -10) + (comeu ? 0 : -40);
            boolean frio = estacao == Estacao.INVERNO && e.getLocation().getBlock().getLightFromSky() >= 14;
            if (frio) delta -= 15;
            if (delta != 0) mudarAfeto(a, delta);
            if (comeu) produzir(a, e, estacao);
            else if (a.especie.leite()) a.leitePronto = false;
            Player dono = Bukkit.getPlayer(a.dono);
            if (dono != null && (!comeu || frio) && rnd().nextDouble() < 0.5) {
                dono.sendActionBar(Component.text("🐾 " + a.nome + (!comeu ? " ficou com fome ontem." : " passou frio a noite toda (ponha num celeiro)."),
                        NamedTextColor.GOLD));
            }
        }
        sujo = true;
    }

    /** Um fardo de feno a até 6 blocos dá uma porção (cada fardo tem 9 e depois acaba). */
    private boolean comerFeno(LivingEntity e) {
        Block centro = e.getLocation().getBlock();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    Block b = centro.getRelative(dx, dy, dz);
                    if (b.getType() != Material.HAY_BLOCK || plugin.ehEstacao(b)) continue;
                    String k = b.getWorld().getName() + "," + b.getX() + "," + b.getY() + "," + b.getZ();
                    int resto = fenos.getOrDefault(k, PORCOES_FENO) - 1;
                    if (resto <= 0) {
                        fenos.remove(k);
                        b.setType(Material.AIR);
                        b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, Material.HAY_BLOCK.createBlockData());
                    } else {
                        fenos.put(k, resto);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void produzir(Animal a, LivingEntity e, Estacao estacao) {
        if (e instanceof Ageable ag && !ag.isAdult()) return;
        World w = e.getWorld();
        Location l = e.getLocation().add(0, 0.3, 0);
        switch (a.especie) {
            case GALINHA -> w.dropItemNaturally(l, comQualidade(new ItemStack(Material.EGG, coracoes(a) >= 4 ? 2 : 1), a));
            case VACA, VACA_COGUMELO, CABRA -> a.leitePronto = true;
            case PORCO -> {
                if (estacao != Estacao.INVERNO && e.getLocation().getBlock().getLightFromSky() >= 14 && rnd().nextDouble() < 0.4 + 0.1 * coracoes(a)) {
                    w.dropItemNaturally(l, comQualidade(trufa(1), a));
                    w.spawnParticle(Particle.BLOCK, l, 12, 0.3, 0.1, 0.3, Material.DIRT.createBlockData());
                }
            }
            case COELHO -> {
                double r = rnd().nextDouble();
                if (r < 0.1 + 0.03 * coracoes(a)) w.dropItemNaturally(l, comQualidade(new ItemStack(Material.RABBIT_FOOT), a));
                else if (r < 0.35 + 0.05 * coracoes(a)) w.dropItemNaturally(l, comQualidade(new ItemStack(Material.RABBIT_HIDE), a));
            }
            case OVELHA -> { } // a lã é na tosquia
        }
        sujo = true;
    }

    // =====================================================================
    //  /animais
    // =====================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Só jogadores.");
            return true;
        }
        abrir(p);
        return true;
    }

    public void abrir(Player p) {
        Tela t = new Tela();
        t.inventario = Bukkit.createInventory(t, 54, Component.text("🐾 Seus animais"));
        List<Animal> meus = new ArrayList<>();
        for (Animal a : animais.values()) if (a.dono.equals(p.getUniqueId())) meus.add(a);
        Map<Especie, Integer> porEspecie = new EnumMap<>(Especie.class);
        for (Animal a : meus) porEspecie.merge(a.especie, 1, Integer::sum);
        List<Component> info = new ArrayList<>();
        info.add(Component.text("Animais: " + meus.size() + " / " + MAX_POR_JOGADOR, NamedTextColor.WHITE));
        porEspecie.forEach((e, n) -> info.add(Component.text(" " + n + "x " + e.nome(), NamedTextColor.GRAY)));
        info.add(Component.empty());
        info.add(Component.text("Adotar: agachado + clique com a mão vazia.", NamedTextColor.GRAY));
        info.add(Component.text("Todo dia: carinho (mão vazia) e comida", NamedTextColor.GRAY));
        info.add(Component.text("(na mão, pasto na grama ou fardo de feno", NamedTextColor.GRAY));
        info.add(Component.text("a até 6 blocos; cada fardo dá 9 porções).", NamedTextColor.GRAY));
        info.add(Component.text("No inverno, deixe num celeiro (com teto).", NamedTextColor.GRAY));
        info.add(Component.text("Mais corações = produtos melhores.", NamedTextColor.GRAY));
        t.inventario.setItem(4, icone(Material.WHEAT, Component.text("Fazenda", COR, TextDecoration.BOLD), info));
        int slot = 9;
        for (Animal a : meus) {
            if (slot >= 54) break;
            Entity e = Bukkit.getEntity(a.id);
            World w = Bukkit.getWorld(a.mundo);
            long hoje = w == null ? -1 : dia(w);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(textoCoracoes(a) + "  (" + a.afeto / 10 + "%)", NamedTextColor.RED));
            lore.add(Component.text("Hoje: carinho " + (a.carinho == hoje ? "✔" : "✖") + " · comida " + (comeuHoje(a, hoje) ? "✔" : "✖"),
                    NamedTextColor.GRAY));
            if (a.especie.leite()) lore.add(Component.text("Leite: " + (a.leitePronto ? "pronto (balde vazio)" : "amanhã, se comer hoje"), NamedTextColor.WHITE));
            lore.add(Component.text("Produz " + a.especie.produz() + ".", NamedTextColor.DARK_GRAY));
            lore.add(Component.text(e == null ? "Longe (não carregado)" : "Em " + e.getLocation().getBlockX() + ", " + e.getLocation().getBlockZ(),
                    NamedTextColor.DARK_GRAY));
            t.inventario.setItem(slot++, icone(a.especie.ovo, Component.text(a.nome + " · " + a.especie.nome(), COR), lore));
        }
        p.openInventory(t.inventario);
    }

    private static ItemStack icone(Material m, Component nome, List<Component> lore) {
        ItemStack i = new ItemStack(m);
        i.editMeta(meta -> {
            meta.displayName(nome.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return i;
    }

    @EventHandler
    public void aoClicarMenu(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true);
    }

    // =====================================================================
    //  Arquivo
    // =====================================================================

    public void carregar() {
        if (!arquivo.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
        ConfigurationSection s = y.getConfigurationSection("animais");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                ConfigurationSection c = s.getConfigurationSection(k);
                if (c == null) continue;
                try {
                    Animal a = new Animal();
                    a.id = UUID.fromString(k);
                    a.dono = UUID.fromString(c.getString("dono", ""));
                    a.especie = Especie.valueOf(c.getString("especie", ""));
                    a.nome = c.getString("nome", "?");
                    a.mundo = c.getString("mundo", "world");
                    a.afeto = c.getInt("afeto", 200);
                    a.carinho = c.getLong("carinho", -1);
                    a.comida = c.getLong("comida", -1);
                    a.pasto = c.getLong("pasto", -1);
                    a.ordenha = c.getLong("ordenha", -1);
                    a.leitePronto = c.getBoolean("leite");
                    animais.put(a.id, a);
                } catch (IllegalArgumentException ignorado) {
                    // linha inválida
                }
            }
        }
        for (String linha : y.getStringList("fenos")) {
            int i = linha.lastIndexOf('=');
            if (i > 0) {
                try { fenos.put(linha.substring(0, i), Integer.parseInt(linha.substring(i + 1))); } catch (NumberFormatException ignorado) { }
            }
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Animal a : animais.values()) {
            String b = "animais." + a.id + ".";
            y.set(b + "dono", a.dono.toString());
            y.set(b + "especie", a.especie.name());
            y.set(b + "nome", a.nome);
            y.set(b + "mundo", a.mundo);
            y.set(b + "afeto", a.afeto);
            y.set(b + "carinho", a.carinho);
            y.set(b + "comida", a.comida);
            y.set(b + "pasto", a.pasto);
            y.set(b + "ordenha", a.ordenha);
            y.set(b + "leite", a.leitePronto);
        }
        List<String> l = new ArrayList<>();
        fenos.forEach((k, v) -> l.add(k + "=" + v));
        y.set("fenos", l);
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar criacao.yml: " + ex.getMessage());
        }
    }
}
