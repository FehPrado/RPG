package br.rpgatributos.detalhes;

import br.rpgatributos.RPGAtributos;
import br.rpgatributos.territorio.Flag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla+: pequenas melhorias com cara de jogo normal, sem item nem textura nova.
 * <ul>
 *   <li>Mudas caídas no chão (fora de territórios) criam raiz sozinhas.</li>
 *   <li>Folhas que caem sozinhas às vezes soltam graveto (e maçã, no carvalho).</li>
 *   <li>Zumbi, esqueleto, creeper e piglin às vezes deixam a cabeça (Pilhagem ajuda).</li>
 *   <li>Baú ou barril com placa: olhando para ele, a barra mostra o que tem dentro.</li>
 *   <li>Peixes pulam fora d'água em rios, lagos e no mar; pescar ali pode dar peixe em dobro.</li>
 *   <li>Gelo fino: parado em cima de gelo sobre a água, ele racha e quebra (agachado aguenta).</li>
 * </ul>
 */
public final class VanillaMais implements Listener {

    private static final TextColor COR = TextColor.color(0xC8E6A0);
    /** Os peixes que pulam (não contam como criatura do plugin nem caem itens). */
    private final NamespacedKey kPeixe;

    private final RPGAtributos plugin;
    private final Set<UUID> mudas = new HashSet<>();
    /** Onde um peixe pulou e quando (para o bônus de pesca). */
    private final List<Object[]> pulos = new ArrayList<>();
    /** Gelo fino: o bloco em que o jogador está e há quantos ticks. */
    private final Map<UUID, Object[]> noGelo = new HashMap<>();
    /** Último baú mostrado (para não reenviar a mesma barra o tempo todo). */
    private final Map<UUID, Long> ultimoBau = new HashMap<>();

    public VanillaMais(RPGAtributos plugin) {
        this.plugin = plugin;
        this.kPeixe = new NamespacedKey(plugin, "peixe_pulando");
    }

    private static ThreadLocalRandom rnd() { return ThreadLocalRandom.current(); }

    // =====================================================================
    //  Mudas que criam raiz
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void aoCairItem(ItemSpawnEvent e) {
        if (!plugin.settings().vanillaMudas) return;
        Material m = e.getEntity().getItemStack().getType();
        if (Tag.SAPLINGS.isTagged(m) || m == Material.MANGROVE_PROPAGULE) mudas.add(e.getEntity().getUniqueId());
    }

    /** Duas mudas no chão viram uma pilha só: a que sobra continua sendo acompanhada. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoJuntar(org.bukkit.event.entity.ItemMergeEvent e) {
        if (mudas.contains(e.getEntity().getUniqueId()) || mudas.contains(e.getTarget().getUniqueId())) mudas.add(e.getTarget().getUniqueId());
    }

    /** Muda que já estava no chão quando o pedaço do mundo carregou também conta. */
    @EventHandler
    public void aoCarregar(org.bukkit.event.world.EntitiesLoadEvent e) {
        if (!plugin.settings().vanillaMudas) return;
        for (org.bukkit.entity.Entity en : e.getEntities()) {
            if (en instanceof Item i && (Tag.SAPLINGS.isTagged(i.getItemStack().getType()) || i.getItemStack().getType() == Material.MANGROVE_PROPAGULE)) {
                mudas.add(i.getUniqueId());
            }
        }
    }

    /** A cada 5 s: muda parada no chão há 40 s ou mais pode se plantar. */
    public void tickMudas() {
        for (Iterator<UUID> it = mudas.iterator(); it.hasNext(); ) {
            if (!(plugin.getServer().getEntity(it.next()) instanceof Item item) || !item.isValid()) {
                it.remove();
                continue;
            }
            if (item.getTicksLived() < 800 || !item.isOnGround() || rnd().nextDouble() > 0.35) continue;
            Block b = item.getLocation().getBlock();
            if (!b.isReplaceable() || b.isLiquid()) continue;
            if (plugin.territorios().em(b) != null || apertado(b)) continue;
            ItemStack is = item.getItemStack();
            BlockData muda = is.getType().createBlockData();
            // A regra do próprio jogo diz em que chão cada muda fica (no 26.3 a tag "dirt" não tem mais a grama).
            if (!muda.isSupported(b)) continue;
            b.setBlockData(muda);
            b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.4, 0.5), 6, 0.25, 0.2, 0.25, 0);
            b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GRASS_PLACE, 0.6f, 1.1f);
            if (is.getAmount() <= 1) {
                item.remove();
                it.remove();
            } else {
                is.setAmount(is.getAmount() - 1);
                item.setItemStack(is);
            }
        }
    }

    /** Já tem muda ou tronco perto demais (2 blocos): a floresta não fica entupida. */
    private static boolean apertado(Block b) {
        for (int x = -2; x <= 2; x++) for (int y = -1; y <= 1; y++) for (int z = -2; z <= 2; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            Material t = b.getRelative(x, y, z).getType();
            if (Tag.SAPLINGS.isTagged(t) || Tag.LOGS.isTagged(t) || t == Material.MANGROVE_PROPAGULE) return true;
        }
        return false;
    }

    // =====================================================================
    //  Folhas soltam gravetos
    // =====================================================================

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoCairFolha(LeavesDecayEvent e) {
        if (!plugin.settings().vanillaGalhos) return;
        Block b = e.getBlock();
        Location l = b.getLocation().add(0.5, 0.5, 0.5);
        if (rnd().nextDouble() < 0.08) b.getWorld().dropItemNaturally(l, new ItemStack(Material.STICK));
        Material t = b.getType();
        if ((t == Material.OAK_LEAVES || t == Material.DARK_OAK_LEAVES) && rnd().nextDouble() < 0.01) {
            b.getWorld().dropItemNaturally(l, new ItemStack(Material.APPLE));
        }
    }

    // =====================================================================
    //  Cabeças de monstro
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void aoMorrer(EntityDeathEvent e) {
        if (!plugin.settings().vanillaCabecas) return;
        LivingEntity morto = e.getEntity();
        Player p = morto.getKiller();
        if (p == null || morto.fromMobSpawner()) return;
        Material cabeca = switch (morto.getType()) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case PIGLIN -> Material.PIGLIN_HEAD;
            default -> null;
        };
        if (cabeca == null) return;
        for (ItemStack d : e.getDrops()) if (d.getType() == cabeca) return;
        int pilhagem = p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        if (rnd().nextDouble() < plugin.settings().vanillaCabecaChance * (1 + 0.5 * pilhagem)) e.getDrops().add(new ItemStack(cabeca));
    }

    // =====================================================================
    //  Placa no baú mostra o conteúdo
    // =====================================================================

    /** A cada meio segundo: quem olha para um baú/barril com placa vê o que tem dentro. */
    public void tickPlacas() {
        if (!plugin.settings().vanillaPlacaBau) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Block b = p.getTargetBlockExact(5);
            if (b == null || !(b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST || b.getType() == Material.BARREL)) continue;
            if (!(b.getState(false) instanceof Container c)) continue;
            Inventory inv = c.getInventory();
            List<Block> partes = new ArrayList<>(List.of(b));
            if (c instanceof Chest && inv instanceof DoubleChestInventory dc) {
                if (dc.getLeftSide().getLocation() != null) partes.add(dc.getLeftSide().getLocation().getBlock());
                if (dc.getRightSide().getLocation() != null) partes.add(dc.getRightSide().getLocation().getBlock());
            }
            Sign placa = null;
            for (Block parte : partes) {
                placa = placa(parte);
                if (placa != null) break;
            }
            if (placa == null || !plugin.territorios().permite(p, b.getLocation(), Flag.BAUS)) continue;
            long agora = System.currentTimeMillis();
            if (agora - ultimoBau.getOrDefault(p.getUniqueId(), 0L) < 1500) continue;
            ultimoBau.put(p.getUniqueId(), agora);
            p.sendActionBar(resumo(placa, inv, c instanceof Barrel));
        }
    }

    /** A placa de parede pregada neste bloco (de qualquer lado), se houver. */
    private static Sign placa(Block b) {
        for (BlockFace f : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block r = b.getRelative(f);
            if (r.getBlockData() instanceof WallSign ws && ws.getFacing() == f && r.getState(false) instanceof Sign s) return s;
        }
        return null;
    }

    private static Component resumo(Sign placa, Inventory inv, boolean barril) {
        PlainTextComponentSerializer plano = PlainTextComponentSerializer.plainText();
        String titulo = "";
        for (Component linha : placa.getSide(Side.FRONT).lines()) {
            String s = plano.serialize(linha).trim();
            if (!s.isEmpty()) {
                titulo = s;
                break;
            }
        }
        Map<String, Integer> qtd = new LinkedHashMap<>();
        Map<String, Component> nomes = new HashMap<>();
        for (ItemStack i : inv.getContents()) {
            if (i == null || i.getType().isAir()) continue;
            Component nome = i.effectiveName();
            String chave = i.getType().name() + "|" + plano.serialize(nome);
            qtd.merge(chave, i.getAmount(), Integer::sum);
            nomes.putIfAbsent(chave, nome);
        }
        Component c = Component.text((barril ? "🛢 " : "📦 ") + (titulo.isEmpty() ? "" : titulo + ": "), COR);
        if (qtd.isEmpty()) return c.append(Component.text("vazio", NamedTextColor.GRAY));
        List<Map.Entry<String, Integer>> l = new ArrayList<>(qtd.entrySet());
        l.sort((x, y) -> y.getValue() - x.getValue());
        for (int i = 0; i < Math.min(4, l.size()); i++) {
            if (i > 0) c = c.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            c = c.append(Component.text(l.get(i).getValue() + "× ", NamedTextColor.WHITE))
                    .append(nomes.get(l.get(i).getKey()).colorIfAbsent(NamedTextColor.WHITE));
        }
        if (l.size() > 4) c = c.append(Component.text("  +" + (l.size() - 4) + " tipos", NamedTextColor.GRAY));
        return c;
    }

    // =====================================================================
    //  Peixes pulando
    // =====================================================================

    /** A cada 2 s: perto de cada jogador, às vezes um peixe pula fora d'água. */
    public void tickPeixes() {
        if (!plugin.settings().vanillaPeixes) return;
        long agora = System.currentTimeMillis();
        pulos.removeIf(x -> agora - (long) x[1] > 40_000);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL || rnd().nextDouble() > 0.12) continue;
            double ang = rnd().nextDouble() * Math.PI * 2, dist = 6 + rnd().nextDouble() * 16;
            int x = p.getLocation().getBlockX() + (int) Math.round(Math.cos(ang) * dist);
            int z = p.getLocation().getBlockZ() + (int) Math.round(Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block topo = w.getHighestBlockAt(x, z);
            if (topo.getType() != Material.WATER || topo.getRelative(BlockFace.DOWN).getType() != Material.WATER) continue;
            if (!(topo.getBlockData() instanceof Levelled lv) || lv.getLevel() != 0) continue;
            if (Math.abs(topo.getY() - p.getLocation().getY()) > 24) continue;
            pular(topo);
            pulos.add(new Object[]{topo.getLocation(), agora});
        }
    }

    private void pular(Block agua) {
        World w = agua.getWorld();
        Biome bio = agua.getBiome();
        String nome = bio.getKey().getKey();
        EntityType tipo = nome.contains("warm") || nome.contains("lukewarm") || nome.contains("mangrove") ? EntityType.TROPICAL_FISH
                : nome.contains("river") || nome.contains("taiga") || nome.contains("frozen") || nome.contains("cold") ? EntityType.SALMON : EntityType.COD;
        Location l = agua.getLocation().add(0.5, 0.95, 0.5);
        Fish f = (Fish) w.spawnEntity(l, tipo);
        f.setPersistent(false);
        f.setRemoveWhenFarAway(true);
        f.setInvulnerable(true);
        f.getPersistentDataContainer().set(kPeixe, PersistentDataType.BYTE, (byte) 1);
        f.setVelocity(new org.bukkit.util.Vector(rnd().nextDouble(-0.15, 0.15), 0.42 + rnd().nextDouble(0.15), rnd().nextDouble(-0.15, 0.15)));
        w.spawnParticle(Particle.SPLASH, l, 12, 0.25, 0.05, 0.25, 0.1);
        w.playSound(l, Sound.ENTITY_FISH_SWIM, 0.7f, 1.2f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!f.isValid()) return;
            Location fim = f.getLocation();
            w.spawnParticle(Particle.SPLASH, fim, 16, 0.2, 0.05, 0.2, 0.15);
            w.playSound(fim, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.6f);
            f.remove();
        }, 22L);
    }

    /** Peixe que pulou não deixa item nem XP (sumiu na água). */
    @EventHandler
    public void aoMorrerPeixe(EntityDeathEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(kPeixe)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    /** Pescar perto de onde um peixe pulou (até 40 s antes): 30% de vir um peixe a mais. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoPescar(PlayerFishEvent e) {
        if (!plugin.settings().vanillaPeixes || e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof Item pego)) return;
        Location anzol = e.getHook().getLocation();
        long agora = System.currentTimeMillis();
        for (Object[] x : pulos) {
            Location l = (Location) x[0];
            if (l.getWorld() != anzol.getWorld() || l.distanceSquared(anzol) > 36 || agora - (long) x[1] > 40_000) continue;
            if (rnd().nextDouble() < 0.3) {
                Player p = e.getPlayer();
                for (ItemStack sobra : p.getInventory().addItem(pego.getItemStack().asOne()).values()) p.getWorld().dropItemNaturally(p.getLocation(), sobra);
                p.sendActionBar(Component.text("🐟 Um cardume! Veio um peixe a mais.", COR));
            }
            return;
        }
    }

    // =====================================================================
    //  Gelo fino
    // =====================================================================

    /** A cada meio segundo: parado no gelo sobre a água, ele racha (1,5 s) e quebra (3 s). Agachado, aguenta. */
    public void tickGelo() {
        if (!plugin.settings().vanillaGeloFino) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || p.isFlying() || p.isInsideVehicle()) {
                noGelo.remove(p.getUniqueId());
                continue;
            }
            Block sob = p.getLocation().subtract(0, 0.2, 0).getBlock();
            if (sob.getType() != Material.ICE || sob.getRelative(BlockFace.DOWN).getType() != Material.WATER
                    || p.isSneaking() || plugin.territorios().em(sob) != null) {
                noGelo.remove(p.getUniqueId());
                continue;
            }
            Object[] st = noGelo.get(p.getUniqueId());
            if (st == null || !sob.equals(st[0])) {
                noGelo.put(p.getUniqueId(), new Object[]{sob, 0});
                continue;
            }
            int t = (int) st[1] + 10;
            st[1] = t;
            Location c = sob.getLocation().add(0.5, 1.0, 0.5);
            if (t >= 60) {
                sob.setType(Material.WATER);
                sob.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                sob.getWorld().spawnParticle(Particle.BLOCK, c, 30, 0.4, 0.1, 0.4, Material.ICE.createBlockData());
                noGelo.remove(p.getUniqueId());
            } else if (t >= 30) {
                sob.getWorld().playSound(c, Sound.BLOCK_GLASS_HIT, 0.8f, 0.6f + t / 100f);
                sob.getWorld().spawnParticle(Particle.BLOCK, c, 6, 0.3, 0.02, 0.3, Material.ICE.createBlockData());
                if (t == 30) p.sendActionBar(Component.text("❄ O gelo está rachando...", NamedTextColor.AQUA));
            }
        }
    }

    // =====================================================================
    //  2.49: ovelhas de cores misturadas
    // =====================================================================

    /** Filhote de pais de cores diferentes que o jogo não mistura: 30% de sair a cor "do meio". */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoCruzar(org.bukkit.event.entity.EntityBreedEvent e) {
        if (!plugin.settings().vanillaOvelhas || !(e.getEntity() instanceof org.bukkit.entity.Sheep filhote)
                || !(e.getMother() instanceof org.bukkit.entity.Sheep mae) || !(e.getFather() instanceof org.bukkit.entity.Sheep pai)) return;
        org.bukkit.DyeColor a = mae.getColor(), b = pai.getColor();
        if (a == null || b == null || a == b) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!filhote.isValid() || (filhote.getColor() != a && filhote.getColor() != b) || rnd().nextDouble() > 0.3) return;
            org.bukkit.Color ca = a.getColor(), cb = b.getColor();
            int r = (ca.getRed() + cb.getRed()) / 2, g = (ca.getGreen() + cb.getGreen()) / 2, bl = (ca.getBlue() + cb.getBlue()) / 2;
            org.bukkit.DyeColor melhor = null;
            double dist = Double.MAX_VALUE;
            for (org.bukkit.DyeColor d : org.bukkit.DyeColor.values()) {
                if (d == a || d == b) continue;
                org.bukkit.Color c = d.getColor();
                double x = Math.pow(c.getRed() - r, 2) + Math.pow(c.getGreen() - g, 2) + Math.pow(c.getBlue() - bl, 2);
                if (x < dist) {
                    dist = x;
                    melhor = d;
                }
            }
            if (melhor != null) filhote.setColor(melhor);
        });
    }

    // =====================================================================
    //  2.49: animais seguem a comida de mais longe
    // =====================================================================

    private void atrair(org.bukkit.entity.Entity en) {
        if (!(en instanceof org.bukkit.entity.Animals a)) return;
        org.bukkit.attribute.AttributeInstance at = a.getAttribute(org.bukkit.attribute.Attribute.TEMPT_RANGE);
        if (at != null && at.getBaseValue() < 16) at.setBaseValue(16);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoNascerAnimal(org.bukkit.event.entity.CreatureSpawnEvent e) {
        if (plugin.settings().vanillaFila) atrair(e.getEntity());
    }

    @EventHandler
    public void aoCarregarAnimais(org.bukkit.event.world.EntitiesLoadEvent e) {
        if (!plugin.settings().vanillaFila) return;
        for (org.bukkit.entity.Entity en : e.getEntities()) atrair(en);
    }

    // =====================================================================
    //  2.49: suporte de armadura com braços
    // =====================================================================

    /** Marca de quem desligou os braços com o graveto (aí não liga de novo sozinho). */
    private final NamespacedKey kSemBracos = new NamespacedKey("rpgatributos", "suporte_sem_bracos");

    /** Suporte de verdade (visível, não é marcador de holograma nem o Boneco de Treino) que ainda não tem braços. */
    private boolean precisaBracos(org.bukkit.entity.ArmorStand a) {
        return !a.hasArms() && !a.isMarker() && !a.isInvisible() && !a.getPersistentDataContainer().has(kSemBracos);
    }

    /** Apareceu no mundo (colocado, dispensador, comando): ganha braços no tick seguinte, depois que o jogo termina de montar. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoSurgirSuporte(org.bukkit.event.entity.CreatureSpawnEvent e) {
        darBracosDepois(e.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoColocarSuporte(org.bukkit.event.entity.EntityPlaceEvent e) {
        darBracosDepois(e.getEntity());
    }

    /** O suporte de armadura conta como criatura (o evento geral de surgir não chega aqui). */
    private void darBracosDepois(org.bukkit.entity.Entity e) {
        if (!plugin.settings().vanillaSuporte || !(e instanceof org.bukkit.entity.ArmorStand a)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (a.isValid() && precisaBracos(a)) a.setArms(true);
        });
    }

    /** Os suportes que já existiam no mundo também ganham braços quando o pedaço do mundo carrega. */
    @EventHandler
    public void aoCarregarSuportes(org.bukkit.event.world.EntitiesLoadEvent e) {
        if (!plugin.settings().vanillaSuporte) return;
        for (org.bukkit.entity.Entity en : e.getEntities()) {
            if (en instanceof org.bukkit.entity.ArmorStand a && precisaBracos(a)) a.setArms(true);
        }
    }

    /** Agachado, com um graveto na mão: liga e desliga os braços do suporte. */
    @EventHandler(ignoreCancelled = true)
    public void aoMexerSuporte(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
        if (!plugin.settings().vanillaSuporte || !(e.getRightClicked() instanceof org.bukkit.entity.ArmorStand a)) return;
        Player p = e.getPlayer();
        if (!p.isSneaking() || e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || p.getInventory().getItemInMainHand().getType() != Material.STICK) return;
        e.setCancelled(true);
        if (!plugin.territorios().podeConstruir(p, a.getLocation())) return;
        a.setArms(!a.hasArms());
        if (a.hasArms()) a.getPersistentDataContainer().remove(kSemBracos);
        else a.getPersistentDataContainer().set(kSemBracos, PersistentDataType.BYTE, (byte) 1);
        p.playSound(a.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 0.8f, 1.2f);
    }

    // =====================================================================
    //  2.49: escadas de mão mais rápidas
    // =====================================================================

    /** Todo tick: subindo a escada (para a frente ou pulando), sobe mais rápido. Agachado, fica parado como no jogo. */
    public void tickEscadas() {
        if (!plugin.settings().vanillaEscadas) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.isFlying() || p.isSneaking() || p.isInsideVehicle()) continue;
            if (p.getLocation().getBlock().getType() != Material.LADDER) continue;
            org.bukkit.Input in = p.getCurrentInput();
            if (!in.isForward() && !in.isJump()) continue;
            org.bukkit.util.Vector v = p.getVelocity();
            if (v.getY() < 0.3) {
                v.setY(0.3);
                p.setVelocity(v);
            }
        }
    }

    // =====================================================================
    //  2.49: lanternas rangendo na chuva
    // =====================================================================

    /** A cada 3 s, na chuva: lanternas penduradas ao ar livre perto de você rangem e balançam com o vento. */
    public void tickLanternas() {
        if (!plugin.settings().vanillaLanternas) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL || !w.hasStorm()) continue;
            Block c = p.getLocation().getBlock();
            int tocou = 0;
            for (int x = -6; x <= 6 && tocou < 3; x++) for (int y = -2; y <= 4 && tocou < 3; y++) for (int z = -6; z <= 6 && tocou < 3; z++) {
                Block b = c.getRelative(x, y, z);
                if (b.getType() != Material.LANTERN && b.getType() != Material.SOUL_LANTERN) continue;
                if (!(b.getBlockData() instanceof org.bukkit.block.data.Hangable h) || !h.isHanging() || b.getLightFromSky() < 12) continue;
                if (rnd().nextDouble() > 0.45) continue;
                tocou++;
                Location l = b.getLocation().add(0.5, 0.4, 0.5);
                w.playSound(l, Sound.BLOCK_CHAIN_STEP, 0.35f, 0.6f + (float) rnd().nextDouble(0.3));
                w.spawnParticle(Particle.FALLING_WATER, l.clone().add(rnd().nextDouble(-0.2, 0.2), 0.3, rnd().nextDouble(-0.2, 0.2)), 2, 0.1, 0, 0.1, 0);
            }
        }
    }

    // =====================================================================
    //  2.49: raio queima árvore
    // =====================================================================

    /** Raio de tempestade numa árvore: as folhas pegam fogo e sobra carvão vegetal no pé dela. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void aoCairRaio(org.bukkit.event.weather.LightningStrikeEvent e) {
        if (!plugin.settings().vanillaRaio || e.getCause() != org.bukkit.event.weather.LightningStrikeEvent.Cause.WEATHER) return;
        Location l = e.getLightning().getLocation();
        Block centro = l.getBlock();
        Block tronco = null;
        for (int y = 1; y >= -4 && tronco == null; y--) for (int x = -1; x <= 1 && tronco == null; x++) for (int z = -1; z <= 1 && tronco == null; z++) {
            Block b = centro.getRelative(x, y, z);
            if (Tag.LOGS.isTagged(b.getType())) tronco = b;
        }
        if (tronco == null) return;
        boolean fogo = plugin.territorios().flagAqui(l, Flag.FOGO);
        if (fogo) {
            int acesos = 0;
            for (int x = -2; x <= 2 && acesos < 3; x++) for (int y = -1; y <= 3 && acesos < 3; y++) for (int z = -2; z <= 2 && acesos < 3; z++) {
                Block b = tronco.getRelative(x, y, z);
                if (Tag.LEAVES.isTagged(b.getType()) && b.getRelative(BlockFace.UP).getType().isAir() && rnd().nextDouble() < 0.5) {
                    b.getRelative(BlockFace.UP).setType(Material.FIRE);
                    acesos++;
                }
            }
        }
        Block pe = tronco;
        while (Tag.LOGS.isTagged(pe.getRelative(BlockFace.DOWN).getType()) && pe.getY() > pe.getWorld().getMinHeight()) pe = pe.getRelative(BlockFace.DOWN);
        Location chao = pe.getLocation().add(0.5, 0.2, 0.5);
        for (int i = 0; i < 4; i++) {
            Block lado = pe.getRelative(new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}[i]);
            if (lado.isPassable()) {
                chao = lado.getLocation().add(0.5, 0.2, 0.5);
                break;
            }
        }
        pe.getWorld().dropItemNaturally(chao, new ItemStack(Material.CHARCOAL, 1 + rnd().nextInt(3)));
        pe.getWorld().spawnParticle(Particle.LARGE_SMOKE, tronco.getLocation().add(0.5, 1, 0.5), 20, 0.4, 0.6, 0.4, 0.02);
    }

    @EventHandler
    public void aoSair(PlayerQuitEvent e) {
        noGelo.remove(e.getPlayer().getUniqueId());
        ultimoBau.remove(e.getPlayer().getUniqueId());
    }
}
