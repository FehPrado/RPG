package br.rpgatributos.domador;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.Settings;
import br.rpgatributos.Skill;
import br.rpgatributos.aventura.Chefes;
import br.rpgatributos.party.Party;
import br.rpgatributos.territorio.Flag;
import br.rpgatributos.territorio.ProtecaoListener;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Companheiros: criaturas vinculadas a um dono no Altar do Domador. Os dados ficam na
 * própria criatura (dono, componentes, modo, alforje); um registro em companheiros.yml
 * guarda onde cada uma está, para achar mesmo com o chunk descarregado.
 */
public final class Companheiros implements Listener {

    public static final TextColor COR = TextColor.color(0xC9955C);
    public static final NamespacedKey CHAVE_DONO = new NamespacedKey("rpgatributos", "companheiro_dono");
    private static final NamespacedKey K_COMPONENTES = new NamespacedKey("rpgatributos", "companheiro_componentes");
    private static final NamespacedKey K_MODO = new NamespacedKey("rpgatributos", "companheiro_modo");
    private static final NamespacedKey K_CENTRO = new NamespacedKey("rpgatributos", "companheiro_centro");
    private static final NamespacedKey K_ALFORJE = new NamespacedKey("rpgatributos", "companheiro_alforje");
    private static final NamespacedKey K_NIVEL = new NamespacedKey("rpgatributos", "companheiro_nivel_dono");
    private static final NamespacedKey M_VIDA = new NamespacedKey("rpgatributos", "domador_vida");
    private static final NamespacedKey M_ARMADURA = new NamespacedKey("rpgatributos", "domador_armadura");
    private static final NamespacedKey M_RESISTENCIA = new NamespacedKey("rpgatributos", "domador_resistencia");
    private static final NamespacedKey M_EMPURRAO = new NamespacedKey("rpgatributos", "domador_empurrao");
    private static final NamespacedKey M_VELOCIDADE = new NamespacedKey("rpgatributos", "domador_velocidade");
    private static final NamespacedKey M_DANO = new NamespacedKey("rpgatributos", "domador_dano");
    private static final List<NamespacedKey> CHAVES_DADOS = List.of(CHAVE_DONO, K_COMPONENTES, K_MODO, K_CENTRO, K_ALFORJE, K_NIVEL, Evolucao.K_XP,
            br.rpgatributos.detalhes.ItensDetalhes.K_TEM_FERRADURA);

    /** Onde o companheiro estava da última vez (para a lista /pets). */
    public static final class Registro {
        final UUID id;
        final UUID dono;
        EntityType tipo;
        String nome;
        String mundo;
        int x, y, z;

        Registro(UUID id, UUID dono) {
            this.id = id;
            this.dono = dono;
        }

        public UUID id() { return id; }
        public UUID dono() { return dono; }
        public EntityType tipo() { return tipo; }
        public String nome() { return nome; }
        public String mundo() { return mundo; }
        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
    }

    /** Inventário do alforje (sabe de qual criatura é). */
    public static final class Alforje implements InventoryHolder {
        final UUID companheiro;
        Inventory inventario;

        Alforje(UUID companheiro) { this.companheiro = companheiro; }

        @Override
        public Inventory getInventory() { return inventario; }
    }

    private final RPGAtributos plugin;
    private final File arquivo;
    private final Map<UUID, Registro> registros = new LinkedHashMap<>();
    /** Companheiros carregados no mundo agora. */
    private final Set<UUID> ativos = new HashSet<>();
    private final Map<UUID, Inventory> alforjes = new HashMap<>();
    /** Quem um companheiro acertou por último (alvo → dono, até quando conta). */
    private final Map<UUID, UUID> ultimoGolpe = new HashMap<>();
    private final Map<UUID, Long> golpeAte = new HashMap<>();
    private boolean sujo;

    public Companheiros(RPGAtributos plugin) {
        this.plugin = plugin;
        this.arquivo = new File(plugin.getDataFolder(), "companheiros.yml");
    }

    private Settings cfg() { return plugin.settings(); }

    // =====================================================================
    //  Dados na criatura
    // =====================================================================

    public static boolean eh(Entity e) {
        return e != null && e.getPersistentDataContainer().has(CHAVE_DONO);
    }

    public static UUID dono(Entity e) {
        String s = e == null ? null : e.getPersistentDataContainer().get(CHAVE_DONO, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean eDono(Player p, Entity e) {
        return p.getUniqueId().equals(dono(e));
    }

    public Set<Componente> componentes(Entity e) {
        EnumSet<Componente> s = EnumSet.noneOf(Componente.class);
        String txt = e.getPersistentDataContainer().get(K_COMPONENTES, PersistentDataType.STRING);
        if (txt == null || txt.isEmpty()) return s;
        for (String n : txt.split(",")) {
            try {
                s.add(Componente.valueOf(n));
            } catch (IllegalArgumentException ignorado) {
                // componente que não existe mais
            }
        }
        return s;
    }

    public boolean tem(Entity e, Componente c) {
        return componentes(e).contains(c);
    }

    void gravarComponentes(LivingEntity e, Set<Componente> comp) {
        List<String> nomes = new ArrayList<>();
        for (Componente c : comp) nomes.add(c.name());
        e.getPersistentDataContainer().set(K_COMPONENTES, PersistentDataType.STRING, String.join(",", nomes));
        aplicarAtributos(e);
    }

    public Modo modo(Entity e) {
        String m = e.getPersistentDataContainer().get(K_MODO, PersistentDataType.STRING);
        try {
            return m == null ? Modo.SEGUIR : Modo.valueOf(m);
        } catch (IllegalArgumentException ex) {
            return Modo.SEGUIR;
        }
    }

    /** Centro de Ficar/Guardar/Patrulhar (no mundo da criatura), ou a posição dela. */
    public Location centro(Entity e) {
        String s = e.getPersistentDataContainer().get(K_CENTRO, PersistentDataType.STRING);
        if (s != null) {
            String[] p = s.split(",");
            if (p.length == 3) {
                try {
                    return new Location(e.getWorld(), Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
                } catch (NumberFormatException ignorado) {
                    // centro inválido: usa a posição atual
                }
            }
        }
        return e.getLocation();
    }

    /** Dá uma ordem. O centro (onde o dono estava) vale para Ficar, Guardar e Patrulhar. */
    public void comandar(LivingEntity e, Modo m, Location centro) {
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        pdc.set(K_MODO, PersistentDataType.STRING, m.name());
        if (m.usaCentro() && centro != null && centro.getWorld().equals(e.getWorld())) {
            pdc.set(K_CENTRO, PersistentDataType.STRING, centro.getX() + "," + centro.getY() + "," + centro.getZ());
        }
        if (e instanceof org.bukkit.entity.Sittable s) s.setSitting(m == Modo.FICAR && centro != null
                && centro.distanceSquared(e.getLocation()) < 4);
        if (e instanceof Mob mob) mob.getPathfinder().stopPathfinding();
    }

    /** Nível de Doma do dono (o último conhecido, se ele estiver offline). */
    public int nivelDono(Entity e) {
        UUID d = dono(e);
        Player p = d == null ? null : Bukkit.getPlayer(d);
        if (p != null) {
            int n = plugin.stats().getNivel(p, Skill.DOMA);
            e.getPersistentDataContainer().set(K_NIVEL, PersistentDataType.INTEGER, n);
            return n;
        }
        return e.getPersistentDataContainer().getOrDefault(K_NIVEL, PersistentDataType.INTEGER, 0);
    }

    /** Vida, armadura, velocidade e dano conforme os componentes e o nível de Doma do dono. */
    public void aplicarAtributos(LivingEntity e) {
        int nivel = nivelDono(e);
        Set<Componente> c = componentes(e);
        double evo = plugin.evolucao().bonus(e);
        modificar(e, Attribute.MAX_HEALTH, M_VIDA, nivel * cfg().domaVida + evo + (c.contains(Componente.COURACA) ? 0.25 : 0),
                AttributeModifier.Operation.ADD_SCALAR);
        modificar(e, Attribute.ARMOR, M_ARMADURA, (c.contains(Componente.COURACA) ? 8 : 0) + (c.contains(Componente.NETHERITE) ? 6 : 0),
                AttributeModifier.Operation.ADD_NUMBER);
        modificar(e, Attribute.ARMOR_TOUGHNESS, M_RESISTENCIA, c.contains(Componente.NETHERITE) ? 4 : 0, AttributeModifier.Operation.ADD_NUMBER);
        modificar(e, Attribute.KNOCKBACK_RESISTANCE, M_EMPURRAO, c.contains(Componente.NETHERITE) ? 0.6 : 0, AttributeModifier.Operation.ADD_NUMBER);
        modificar(e, Attribute.MOVEMENT_SPEED, M_VELOCIDADE, c.contains(Componente.AGILIDADE) ? 0.3 : 0, AttributeModifier.Operation.ADD_SCALAR);
        modificar(e, Attribute.ATTACK_DAMAGE, M_DANO, nivel * cfg().domaDano + evo + (c.contains(Componente.GARRAS) ? 0.5 : 0),
                AttributeModifier.Operation.ADD_SCALAR);
        e.setGlowing(c.contains(Componente.FAROL));
        plugin.evolucao().aplicar(e);
    }

    private static void modificar(LivingEntity e, Attribute a, NamespacedKey chave, double valor, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(chave);
        if (valor != 0) inst.addModifier(new AttributeModifier(chave, valor, op, EquipmentSlotGroup.ANY));
        if (a == Attribute.MAX_HEALTH && e.getHealth() > inst.getValue()) e.setHealth(inst.getValue());
    }

    /** Dano de um ataque do companheiro (os que não atacam sozinhos usam isso). */
    public double danoAtaque(LivingEntity e) {
        double base = tem(e, Componente.GARRAS) ? 7 : 3;
        return base * (1 + nivelDono(e) * cfg().domaDano + plugin.evolucao().bonus(e));
    }

    // =====================================================================
    //  Vincular, invocar, libertar
    // =====================================================================

    /** Dá para essa criatura virar companheiro? (animais e golens; nada de monstros, aldeões ou peixes) */
    public static boolean vinculavel(Entity e) {
        return e instanceof Mob m && m.isValid() && !m.isDead() && !(e instanceof Enemy) && !(e instanceof AbstractVillager)
                && !(e instanceof WaterMob) && !Chefes.ehChefe(e) && !Chefes.ehLacaio(e) && !eh(e)
                && e.getType() != EntityType.ENDER_DRAGON && e.getType() != EntityType.WITHER;
    }

    /** Está preso no laço do jogador ou é um animal que ele domou. */
    public static boolean doJogador(Player p, LivingEntity e) {
        if (e.isLeashed() && p.equals(e.getLeashHolder())) return true;
        return e instanceof Tameable t && t.isTamed() && p.getUniqueId().equals(t.getOwnerUniqueId());
    }

    public List<Registro> de(UUID dono) {
        List<Registro> l = new ArrayList<>();
        for (Registro r : registros.values()) if (r.dono.equals(dono)) l.add(r);
        return l;
    }

    /** Quantos companheiros o jogador pode ter (Doma + classe). */
    public int limite(Player p) {
        return cfg().domaCompanheiros(plugin.stats().getNivel(p, Skill.DOMA)) + plugin.classes().companheirosExtra(p);
    }

    /** Quantos componentes cabem em cada companheiro desse jogador (Doma + classe). */
    public int espacos(Player p) {
        return cfg().domaComponentes(plugin.stats().getNivel(p, Skill.DOMA)) + plugin.classes().componentesExtra(p);
    }

    /** @return o motivo de não ter dado certo, ou null. */
    public String vincular(Player p, LivingEntity e) {
        if (!vinculavel(e)) return "Essa criatura não pode virar companheiro.";
        int limite = limite(p);
        if (de(p.getUniqueId()).size() >= limite) {
            return "Você já tem " + limite + " companheiro(s). Suba a Doma para ter mais (ou liberte um).";
        }
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        pdc.set(CHAVE_DONO, PersistentDataType.STRING, p.getUniqueId().toString());
        pdc.set(K_MODO, PersistentDataType.STRING, Modo.SEGUIR.name());
        if (e instanceof Tameable t) {
            t.setTamed(true);
            t.setOwner(p);
        }
        if (e.isLeashed()) e.setLeashHolder(null);
        e.setPersistent(true);
        e.setRemoveWhenFarAway(false);
        if (e.customName() == null) e.customName(Component.text(nomeTipo(e.getType()) + " de " + p.getName(), COR));
        aplicarAtributos(e);
        AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) e.setHealth(max.getValue());
        registrar(e);
        ativos.add(e.getUniqueId());
        prepararIA(e);
        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) plugin.stats().darXp(p, Skill.DOMA, cfg().domaXpVincular);
        plugin.titulos().registrar(p, "companheiros", 1);
        World w = e.getWorld();
        w.spawnParticle(Particle.HEART, e.getLocation().add(0, e.getHeight() + 0.3, 0), 10, 0.4, 0.3, 0.4);
        w.playSound(e.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        p.sendMessage(Component.text("♞ " + nome(e) + " agora é seu companheiro! Clique nele com a mão vazia (ou /pets).", COR));
        return null;
    }

    /** Invoca a criatura no lugar e já vincula. @return o motivo de não ter dado certo, ou null. */
    public String invocar(Player p, Invocavel inv, Location onde) {
        int limite = limite(p);
        if (de(p.getUniqueId()).size() >= limite) {
            return "Você já tem " + limite + " companheiro(s). Suba a Doma para ter mais (ou liberte um).";
        }
        Entity novo = onde.getWorld().spawnEntity(onde, inv.tipo());
        if (!(novo instanceof LivingEntity le)) {
            novo.remove();
            return "Não consegui invocar essa criatura.";
        }
        if (le instanceof org.bukkit.entity.Ageable a) a.setAdult();
        onde.getWorld().spawnParticle(Particle.CLOUD, onde.clone().add(0, 0.8, 0), 30, 0.4, 0.5, 0.4, 0.05);
        String erro = vincular(p, le);
        if (erro != null) le.remove();
        return erro;
    }

    /** Desfaz o vínculo: a criatura volta a ser comum e os componentes caem no chão. */
    public void libertar(LivingEntity e) {
        fecharAlforje(e.getUniqueId());
        plugin.itensDetalhes().soltarFerradura(e);
        Location l = e.getLocation();
        for (Componente c : componentes(e)) l.getWorld().dropItemNaturally(l, new ItemStack(c.item()));
        for (ItemStack i : alforje(e).getContents()) if (i != null && !i.isEmpty()) l.getWorld().dropItemNaturally(l, i);
        alforjes.remove(e.getUniqueId());
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        for (NamespacedKey k : CHAVES_DADOS) pdc.remove(k);
        for (NamespacedKey k : List.of(M_VIDA, M_ARMADURA, M_RESISTENCIA, M_EMPURRAO, M_VELOCIDADE, M_DANO)) {
            for (Attribute a : List.of(Attribute.MAX_HEALTH, Attribute.ARMOR, Attribute.ARMOR_TOUGHNESS,
                    Attribute.KNOCKBACK_RESISTANCE, Attribute.MOVEMENT_SPEED, Attribute.ATTACK_DAMAGE)) {
                AttributeInstance inst = e.getAttribute(a);
                if (inst != null) inst.removeModifier(k);
            }
        }
        e.setGlowing(false);
        plugin.evolucao().aplicar(e); // sem XP: volta ao tamanho normal
        e.setGravity(true);
        e.eject();
        registros.remove(e.getUniqueId());
        ativos.remove(e.getUniqueId());
        sujo = true;
        salvar();
    }

    // =====================================================================
    //  Alforje (inventário guardado na criatura)
    // =====================================================================

    public Inventory alforje(LivingEntity e) {
        return alforjes.computeIfAbsent(e.getUniqueId(), id -> {
            Alforje h = new Alforje(id);
            h.inventario = Bukkit.createInventory(h, 27, Component.text("Alforje de " + nome(e)));
            byte[] bytes = e.getPersistentDataContainer().get(K_ALFORJE, PersistentDataType.BYTE_ARRAY);
            if (bytes != null) {
                ItemStack[] itens = ItemStack.deserializeItemsFromBytes(bytes);
                for (int i = 0; i < itens.length && i < 27; i++) {
                    if (itens[i] != null && !itens[i].isEmpty()) h.inventario.setItem(i, itens[i]);
                }
            }
            return h.inventario;
        });
    }

    public void salvarAlforje(UUID id) {
        Inventory inv = alforjes.get(id);
        if (inv == null || !(Bukkit.getEntity(id) instanceof LivingEntity e)) return;
        ItemStack[] itens = inv.getContents();
        for (int i = 0; i < itens.length; i++) if (itens[i] == null) itens[i] = ItemStack.empty();
        e.getPersistentDataContainer().set(K_ALFORJE, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(itens));
    }

    @EventHandler
    public void aoFecharAlforje(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Alforje a) salvarAlforje(a.companheiro);
    }

    // =====================================================================
    //  Registro (companheiros.yml)
    // =====================================================================

    private void registrar(LivingEntity e) {
        UUID d = dono(e);
        if (d == null) return;
        Registro r = registros.computeIfAbsent(e.getUniqueId(), id -> new Registro(id, d));
        r.tipo = e.getType();
        r.nome = nome(e);
        r.mundo = e.getWorld().getName();
        r.x = e.getLocation().getBlockX();
        r.y = e.getLocation().getBlockY();
        r.z = e.getLocation().getBlockZ();
        sujo = true;
    }

    public Registro registro(UUID id) {
        return registros.get(id);
    }

    /** Companheiros carregados agora. */
    public Set<UUID> ativos() {
        return Collections.unmodifiableSet(ativos);
    }

    /** Atualiza onde os companheiros carregados estão (chamado de tempos em tempos). */
    public void atualizarRegistros() {
        for (UUID id : new ArrayList<>(ativos)) {
            if (Bukkit.getEntity(id) instanceof LivingEntity e && e.isValid()) registrar(e);
            else ativos.remove(id);
        }
        if (sujo) salvar();
    }

    public void carregar() {
        registros.clear();
        if (arquivo.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(arquivo);
            ConfigurationSection raiz = y.getConfigurationSection("companheiros");
            if (raiz != null) {
                for (String id : raiz.getKeys(false)) {
                    ConfigurationSection s = raiz.getConfigurationSection(id);
                    if (s == null) continue;
                    try {
                        Registro r = new Registro(UUID.fromString(id), UUID.fromString(s.getString("dono", "")));
                        r.tipo = EntityType.valueOf(s.getString("tipo", "COW"));
                        r.nome = s.getString("nome", "?");
                        r.mundo = s.getString("mundo", "world");
                        r.x = s.getInt("x");
                        r.y = s.getInt("y");
                        r.z = s.getInt("z");
                        registros.put(r.id, r);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Companheiro inválido em companheiros.yml (" + id + ")");
                    }
                }
            }
        }
        // Os que já estão carregados (servidor ligando ou /reload).
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getLivingEntities()) if (eh(e)) carregou(e);
        }
    }

    public void salvar() {
        YamlConfiguration y = new YamlConfiguration();
        for (Registro r : registros.values()) {
            String base = "companheiros." + r.id + ".";
            y.set(base + "dono", r.dono.toString());
            y.set(base + "tipo", r.tipo == null ? "COW" : r.tipo.name());
            y.set(base + "nome", r.nome);
            y.set(base + "mundo", r.mundo);
            y.set(base + "x", r.x);
            y.set(base + "y", r.y);
            y.set(base + "z", r.z);
        }
        try {
            y.save(arquivo);
            sujo = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Não consegui salvar companheiros.yml: " + ex.getMessage());
        }
    }

    /** Antes de desligar: guarda os alforjes abertos e o registro. */
    public void desligar() {
        for (UUID id : new ArrayList<>(alforjes.keySet())) salvarAlforje(id);
        for (UUID id : ativos) {
            if (Bukkit.getEntity(id) instanceof LivingEntity e) e.setGravity(true);
        }
        atualizarRegistros();
        salvar();
    }

    private void carregou(LivingEntity e) {
        ativos.add(e.getUniqueId());
        if (e.getPassengers().isEmpty()) e.setGravity(true); // caso o servidor tenha caído no meio de um voo
        registrar(e);
        aplicarAtributos(e);
        prepararIA(e);
    }

    /**
     * Tira dos pets (lobo, gato, papagaio...) o "seguir o dono" do jogo, que teleporta:
     * quem manda agora são as ordens do domador. Vale enquanto a criatura está carregada.
     */
    private static void prepararIA(LivingEntity e) {
        if (e instanceof Tameable t) Bukkit.getMobGoals().removeGoal(t, VanillaGoal.FOLLOW_OWNER);
    }

    @EventHandler
    public void aoCarregarEntidades(EntitiesLoadEvent e) {
        for (Entity ent : e.getEntities()) if (ent instanceof LivingEntity le && eh(le)) carregou(le);
    }

    @EventHandler
    public void aoDescarregarEntidades(EntitiesUnloadEvent e) {
        for (Entity ent : e.getEntities()) {
            if (!(ent instanceof LivingEntity le) || !ativos.remove(ent.getUniqueId())) continue;
            registrar(le);
            salvarAlforje(ent.getUniqueId());
            alforjes.remove(ent.getUniqueId());
        }
    }

    // =====================================================================
    //  Nomes
    // =====================================================================

    public static String nome(Entity e) {
        Component n = e.customName();
        return n == null ? nomeTipo(e.getType()) : PlainTextComponentSerializer.plainText().serialize(n);
    }

    public static String nomeTipo(EntityType t) {
        for (Invocavel i : Invocavel.values()) if (i.tipo() == t) return i.nome();
        String s = t.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // =====================================================================
    //  XP de Doma: domar e cruzar
    // =====================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoDomar(EntityTameEvent e) {
        if (e.getOwner() instanceof Player p && p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            plugin.stats().darXp(p, Skill.DOMA, cfg().domaXpDomar);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCruzar(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player p && p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            plugin.stats().darXp(p, Skill.DOMA, cfg().domaXpCruzar);
        }
    }

    // =====================================================================
    //  Proteção, dano, morte
    // =====================================================================

    /** O jogador não pode ferir esse companheiro (é o dono, é da party, ou o PvP está desligado ali). */
    private boolean protegido(Player atacante, LivingEntity comp) {
        UUID d = dono(comp);
        if (d == null) return false;
        if (d.equals(atacante.getUniqueId())) return true;
        if (Boolean.FALSE.equals(comp.getWorld().getGameRuleValue(GameRules.PVP))) return true;
        Party pt = plugin.parties().party(d);
        if (pt != null && pt.tem(atacante.getUniqueId()) && !pt.fogoAmigo()) return true;
        return !plugin.territorios().flagAqui(comp.getLocation(), Flag.PVP);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void aoFerirCompanheiro(EntityDamageByEntityEvent e) {
        Entity v = e.getEntity();
        if (v instanceof LivingEntity comp && eh(comp)) {
            Player a = ProtecaoListener.responsavel(e.getDamager());
            if (a != null && protegido(a, comp)) {
                e.setCancelled(true);
                return;
            }
            // Companheiros do mesmo dono não se ferem.
            if (eh(e.getDamager()) && dono(comp).equals(dono(e.getDamager()))) e.setCancelled(true);
        }
    }

    /** Lembra quem o companheiro acertou (para dar XP ao dono se o alvo morrer) e acende o fogo das Chamas. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoCompanheiroAcertar(EntityDamageByEntityEvent e) {
        Entity atacante = e.getDamager();
        if (!eh(atacante)) return;
        UUID d = dono(atacante);
        ultimoGolpe.put(e.getEntity().getUniqueId(), d);
        golpeAte.put(e.getEntity().getUniqueId(), System.currentTimeMillis() + 5000);
        if (tem(atacante, Componente.CHAMAS)) e.getEntity().setFireTicks(Math.max(e.getEntity().getFireTicks(), 80));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoSofrer(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity comp) || !eh(comp)) return;
        Set<Componente> c = componentes(comp);
        switch (e.getCause()) {
            case FALL -> {
                if (c.contains(Componente.ASAS) || !comp.getPassengers().isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
            }
            case FIRE, FIRE_TICK, LAVA, CAMPFIRE -> {
                if (c.contains(Componente.CHAMAS) || c.contains(Componente.NETHERITE)) {
                    e.setCancelled(true);
                    comp.setFireTicks(0);
                    return;
                }
            }
            case DROWNING -> {
                if (c.contains(Componente.MARE)) {
                    e.setCancelled(true);
                    return;
                }
            }
            default -> { }
        }
        // Segunda Vida: escapa da morte uma vez.
        if (c.contains(Componente.TOTEM) && e.getFinalDamage() >= comp.getHealth()) {
            e.setCancelled(true);
            c.remove(Componente.TOTEM);
            gravarComponentes(comp, c);
            AttributeInstance max = comp.getAttribute(Attribute.MAX_HEALTH);
            comp.setHealth(Math.max(1, (max == null ? 20 : max.getValue()) * 0.5));
            comp.setFireTicks(0);
            comp.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, comp.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.4);
            comp.getWorld().playSound(comp.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.1f);
            Player d = Bukkit.getPlayer(dono(comp));
            if (d != null) d.sendMessage(Component.text("♞ " + nome(comp) + " escapou da morte! (a Segunda Vida se gastou)", COR));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void aoMorrer(EntityDeathEvent e) {
        LivingEntity morto = e.getEntity();
        // XP para o dono do companheiro que derrotou um monstro.
        UUID quem = ultimoGolpe.remove(morto.getUniqueId());
        Long ate = golpeAte.remove(morto.getUniqueId());
        if (quem != null && ate != null && ate > System.currentTimeMillis() && morto instanceof Enemy && !eh(morto)) {
            Player d = Bukkit.getPlayer(quem);
            if (d != null) {
                plugin.stats().darXp(d, Skill.DOMA, cfg().domaXpAbate);
                plugin.titulos().registrar(d, "abates_companheiro", 1);
            }
        }
        if (ultimoGolpe.size() > 500) {
            long agora = System.currentTimeMillis();
            golpeAte.entrySet().removeIf(en -> en.getValue() < agora);
            ultimoGolpe.keySet().retainAll(golpeAte.keySet());
        }
        if (!eh(morto)) return;
        // O companheiro morreu: componentes e alforje caem.
        fecharAlforje(morto.getUniqueId());
        for (Componente c : componentes(morto)) e.getDrops().add(new ItemStack(c.item()));
        if (br.rpgatributos.detalhes.ItensDetalhes.temFerradura(morto)) e.getDrops().add(br.rpgatributos.detalhes.ItensDetalhes.ferradura(1));
        for (ItemStack i : alforje(morto).getContents()) if (i != null && !i.isEmpty()) e.getDrops().add(i);
        alforjes.remove(morto.getUniqueId());
        registros.remove(morto.getUniqueId());
        ativos.remove(morto.getUniqueId());
        salvar();
        Player d = Bukkit.getPlayer(dono(morto));
        if (d != null) {
            Location l = morto.getLocation();
            d.sendMessage(Component.text("☠ Seu companheiro " + nome(morto) + " morreu em " + l.getBlockX() + " " + l.getBlockY()
                    + " " + l.getBlockZ() + ". Os componentes ficaram no chão.", NamedTextColor.RED));
        }
    }

    /**
     * Sumiu de outro jeito (comando, vazio, despawn): tira do registro. Espera um tick:
     * se passou por um portal, a mesma criatura aparece no outro mundo e continua valendo.
     */
    @EventHandler
    public void aoSumir(EntityRemoveEvent e) {
        EntityRemoveEvent.Cause c = e.getCause();
        if (c == EntityRemoveEvent.Cause.UNLOAD || c == EntityRemoveEvent.Cause.PLAYER_QUIT
                || c == EntityRemoveEvent.Cause.DEATH || c == EntityRemoveEvent.Cause.TRANSFORMATION) return;
        if (!eh(e.getEntity())) return;
        UUID id = e.getEntity().getUniqueId();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Entity agora = Bukkit.getEntity(id);
            if (agora != null && agora.isValid()) return;
            ativos.remove(id);
            alforjes.remove(id);
            if (registros.remove(id) != null) sujo = true;
        });
    }

    /** Apareceu no mundo (chunk, portal, invocação): passa a ser cuidado pelo plugin. */
    @EventHandler
    public void aoEntrarNoMundo(EntityAddToWorldEvent e) {
        if (e.getEntity() instanceof LivingEntity le && eh(le) && !ativos.contains(le.getUniqueId())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (le.isValid()) carregou(le);
            });
        }
    }

    /** Fecha o alforje para quem estiver com ele aberto (antes de os itens caírem no chão). */
    private void fecharAlforje(UUID id) {
        Inventory inv = alforjes.get(id);
        if (inv != null) for (HumanEntity h : new ArrayList<>(inv.getViewers())) h.closeInventory();
    }

    /** Montado num companheiro, o jogador não leva dano de queda. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoCairMontado(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p && eh(p.getVehicle())) {
            e.setCancelled(true);
        }
    }

    /** A vaca de cogumelo tosquiada vira vaca: o vínculo passa para a nova criatura. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void aoTransformar(EntityTransformEvent e) {
        Entity antes = e.getEntity();
        if (!eh(antes) || !(e.getTransformedEntity() instanceof LivingEntity depois)) return;
        salvarAlforje(antes.getUniqueId());
        PersistentDataContainer de = antes.getPersistentDataContainer(), para = depois.getPersistentDataContainer();
        for (NamespacedKey k : CHAVES_DADOS) {
            if (de.has(k, PersistentDataType.STRING)) para.set(k, PersistentDataType.STRING, de.get(k, PersistentDataType.STRING));
            else if (de.has(k, PersistentDataType.BYTE_ARRAY)) para.set(k, PersistentDataType.BYTE_ARRAY, de.get(k, PersistentDataType.BYTE_ARRAY));
            else if (de.has(k, PersistentDataType.INTEGER)) para.set(k, PersistentDataType.INTEGER, de.get(k, PersistentDataType.INTEGER));
        }
        depois.setPersistent(true);
        depois.setRemoveWhenFarAway(false);
        alforjes.remove(antes.getUniqueId());
        registros.remove(antes.getUniqueId());
        ativos.remove(antes.getUniqueId());
        carregou(depois);
    }

    /** Pets (lobo, gato...) não teleportam até o dono: andam. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void aoTeleportar(EntityTeleportEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Tameable) || !eh(ent) || e.getTo() == null) return;
        Player d = Bukkit.getPlayer(dono(ent));
        if (d != null && d.getWorld().equals(e.getTo().getWorld()) && d.getLocation().distanceSquared(e.getTo()) < 16) {
            e.setCancelled(true);
        }
    }
}
