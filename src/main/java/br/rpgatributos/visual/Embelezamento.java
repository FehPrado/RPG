package br.rpgatributos.visual;

import br.rpgatributos.RPGAtributos;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joml.Matrix4f;
import java.util.*;

/** Personalização livre: acessórios no corpo, cores, auras, passos e três combinações salvas. */
public final class Embelezamento implements Listener {
    private final RPGAtributos plugin;
    private static NamespacedKey chave(String id) { return new NamespacedKey("rpgatributos", "aparencia_" + id); }
    private static final NamespacedKey CORPO = chave("corpo"), COLAR = chave("colar"), AURA = chave("aura"), PASSOS = chave("passos");
    private static final int[] CORES = {0xFFFFFF,0xA06D40,0xFFC75A,0x4E485B,0xF3F2E7,0x83C6FF,0xFF8194,0x8FE1A4};
    private static final String[] NOMES_CORES = {"Natural","Castanho","Dourado","Negro","Marfim","Azul","Rosa","Verde"};
    private static final String[] CORPOS = {"Nenhum","Mochila","Aljava","Capa"};
    private static final String[] IDS_CORPO = {"","mochila","aljava","capa"};
    private static final String[] EFEITOS = {"Nenhum","Runas","Folhas","Faíscas"};
    private static final String[] LOOKS = {"Exploração","Combate","Cidade"};
    private static final class Tela implements InventoryHolder { Inventory inv; public Inventory getInventory() { return inv; } }
    private record Corpo(ItemDisplay costas, ItemDisplay colar) { }
    private final Map<UUID, Corpo> corpos = new HashMap<>();
    private final Map<UUID, Location> passos = new HashMap<>();
    private int ciclo;
    public Embelezamento(RPGAtributos plugin) { this.plugin = plugin; }
    private static int ler(Player p, NamespacedKey chave, int max) { return Math.floorMod(p.getPersistentDataContainer().getOrDefault(chave, PersistentDataType.INTEGER, 0), max); }
    private static void alternar(Player p, NamespacedKey chave, int max) { p.getPersistentDataContainer().set(chave, PersistentDataType.INTEGER, (ler(p, chave, max) + 1) % max); }

    public record Look(List<String> enfeites, int tom, int corpo, int colar, int aura, int passos) {
        public Look {
            enfeites = enfeites.stream().filter(id -> br.rpgatributos.raca.Enfeites.porId(id) != null).distinct().toList();
            tom &= 0xFFFFFF; corpo = Math.floorMod(corpo, 4); colar = Math.floorMod(colar, 2); aura = Math.floorMod(aura, 4); passos = Math.floorMod(passos, 4);
        }
        public String salvar() { return String.join(",", enfeites) + "|" + tom + "|" + corpo + "|" + colar + "|" + aura + "|" + passos; }
        public static Optional<Look> ler(String s) {
            if (s == null) return Optional.empty();
            try { String[] p = s.split("\\|", -1); if (p.length != 6) return Optional.empty();
                return Optional.of(new Look(p[0].isBlank() ? List.of() : List.of(p[0].split(",")), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])));
            } catch (RuntimeException e) { return Optional.empty(); }
        }
    }
    public void abrir(Player p) {
        Tela t = new Tela(); t.inv = Bukkit.createInventory(t, 54, titulo(plugin, p, br.rpgatributos.ajuda.Fundo.ESTUDIO, "✦ Estúdio do personagem")); desenhar(p, t); p.openInventory(t.inv);
    }
    public static Component titulo(RPGAtributos plugin, Player p, br.rpgatributos.ajuda.Fundo f, String texto) {
        Component titulo = Component.text(texto);
        return plugin.ajuda().temPacote(p) ? Component.text("" + br.rpgatributos.ajuda.Fundo.VOLTA_ANTES + f.letra() + br.rpgatributos.ajuda.Fundo.VOLTA_DEPOIS)
                .font(net.kyori.adventure.key.Key.key("rpgatributos", "menus")).color(NamedTextColor.WHITE).append(titulo) : titulo;
    }
    private static ItemStack botao(Material material, String nome, String... linhas) {
        ItemStack i = new ItemStack(material); i.editMeta(m -> { m.itemName(Component.text(nome, NamedTextColor.LIGHT_PURPLE)); m.lore(Arrays.stream(linhas).map(l -> Component.text(l, NamedTextColor.GRAY)).toList()); }); return i;
    }
    private void desenhar(Player p, Tela t) {
        t.inv.clear(); int cor = p.getPersistentDataContainer().getOrDefault(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, 0xFFFFFF), indice = 0;
        for (int n = 0; n < CORES.length; n++) if (CORES[n] == cor) indice = n;
        t.inv.setItem(4, botao(Material.PLAYER_HEAD, "Seu estilo, suas combinações", "Só aparência: não altera atributos.", "A coleção de enfeites é gratuita."));
        t.inv.setItem(10, botao(Material.LEATHER_HELMET, "Cores · " + NOMES_CORES[indice], "Clique para trocar o tom dos enfeites", "e acessórios. A cor fica no seu visual."));
        t.inv.setItem(12, botao(Material.CHEST, "Costas · " + CORPOS[ler(p, CORPO, 4)], "Clique: mochila, aljava, capa ou nenhum.", "Acessórios ornamentais, sem armazenamento."));
        t.inv.setItem(14, botao(Material.HEART_OF_THE_SEA, "Colar · " + (ler(p, COLAR, 2) == 1 ? "Visível" : "Oculto"), "Um amuleto visível no peito."));
        t.inv.setItem(16, botao(Material.FIREWORK_STAR, "Aura · " + EFEITOS[ler(p, AURA, 4)], "Efeito discreto em volta do personagem."));
        t.inv.setItem(22, botao(Material.LEATHER_BOOTS, "Passos · " + EFEITOS[ler(p, PASSOS, 4)], "Rastro curto enquanto anda no chão."));
        for (int n = 0; n < LOOKS.length; n++) {
            boolean salvo = p.getPersistentDataContainer().has(chave("look_" + n));
            t.inv.setItem(29 + n * 2, botao(salvo ? Material.WRITABLE_BOOK : Material.BOOK, LOOKS[n], "Botão direito: salvar este visual.", "Botão esquerdo: usar o visual salvo.", "Salva enfeites, cores, costas, colar e efeitos.", "Armaduras e equipamentos não são copiados."));
        }
        t.inv.setItem(45, botao(Material.ARMOR_STAND, "Guarda-roupa", "Escolha os enfeites e a roupa."));
        t.inv.setItem(49, botao(Material.BRICKS, "Decoração e cenários", "Fabrique móveis, estandartes e ornamentos."));
        t.inv.setItem(53, botao(Material.BARRIER, "Fechar"));
    }
    @EventHandler public void clicar(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Tela t)) return;
        e.setCancelled(true); if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() < 0 || e.getRawSlot() >= 54) return;
        switch (e.getRawSlot()) {
            case 10 -> { int atual = p.getPersistentDataContainer().getOrDefault(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, 0xFFFFFF), i = 0; for (int n = 0; n < CORES.length; n++) if (atual == CORES[n]) i = n; p.getPersistentDataContainer().set(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, CORES[(i + 1) % CORES.length]); plugin.guardaRoupa().atualizar(p); remover(p.getUniqueId()); }
            case 12 -> { alternar(p, CORPO, 4); remover(p.getUniqueId()); }
            case 14 -> { alternar(p, COLAR, 2); remover(p.getUniqueId()); }
            case 16 -> alternar(p, AURA, 4);
            case 22 -> alternar(p, PASSOS, 4);
            case 45 -> { plugin.guardaRoupa().abrir(p); return; }
            case 49 -> { plugin.decoracoes().abrir(p); return; }
            case 53 -> { p.closeInventory(); return; }
            default -> {
                for (int n = 0; n < LOOKS.length; n++) if (e.getRawSlot() == 29 + n * 2) {
                    if (e.isRightClick()) {
                        Look look = new Look(plugin.guardaRoupa().selecao(p), p.getPersistentDataContainer().getOrDefault(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, 0xFFFFFF), ler(p, CORPO, 4), ler(p, COLAR, 2), ler(p, AURA, 4), ler(p, PASSOS, 4));
                        p.getPersistentDataContainer().set(chave("look_" + n), PersistentDataType.STRING, look.salvar());
                        p.sendMessage(Component.text("Visual de " + LOOKS[n] + " salvo.", NamedTextColor.GREEN));
                    } else {
                        var salvo = Look.ler(p.getPersistentDataContainer().get(chave("look_" + n), PersistentDataType.STRING));
                        if (salvo.isEmpty()) { p.sendMessage(Component.text("Salve um visual com o botão direito primeiro.", NamedTextColor.YELLOW)); return; }
                        Look l = salvo.get(); p.getPersistentDataContainer().set(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, l.tom());
                        p.getPersistentDataContainer().set(CORPO, PersistentDataType.INTEGER, l.corpo()); p.getPersistentDataContainer().set(COLAR, PersistentDataType.INTEGER, l.colar());
                        p.getPersistentDataContainer().set(AURA, PersistentDataType.INTEGER, l.aura()); p.getPersistentDataContainer().set(PASSOS, PersistentDataType.INTEGER, l.passos());
                        plugin.guardaRoupa().usarSelecao(p, l.enfeites()); remover(p.getUniqueId());
                    }
                }
            }
        }
        desenhar(p, t);
    }
    @EventHandler public void arrastar(InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder(false) instanceof Tela) e.setCancelled(true); }

    private ItemDisplay criar(Player p, String modelo) {
        return p.getWorld().spawn(p.getLocation(), ItemDisplay.class, d -> {
            ItemStack i = AnimacoesMagicas.item("corpo_" + modelo);
            i.editMeta(m -> { var cmd = m.getCustomModelDataComponent(); cmd.setColors(List.of(Color.fromRGB(p.getPersistentDataContainer().getOrDefault(GuardaRoupa.K_TOM, PersistentDataType.INTEGER, 0xFFFFFF)))); m.setCustomModelDataComponent(cmd); });
            d.setItemStack(i); d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE); d.setPersistent(false); d.setInvulnerable(true); d.setGravity(false);
            d.setVisibleByDefault(false); d.setTeleportDuration(2); d.setInterpolationDuration(2); d.setViewRange(1.5f);
        });
    }
    private void remover(UUID id) { Corpo c = corpos.remove(id); if (c != null) { if (c.costas() != null) c.costas().remove(); if (c.colar() != null) c.colar().remove(); } }
    @EventHandler public void sair(PlayerQuitEvent e) { remover(e.getPlayer().getUniqueId()); passos.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void mudarMundo(org.bukkit.event.player.PlayerChangedWorldEvent e) {
        remover(e.getPlayer().getUniqueId()); passos.remove(e.getPlayer().getUniqueId());
        plugin.animacoesMagicas().invocacao(e.getPlayer(), e.getPlayer().getLocation(), java.util.EnumSet.of(br.rpgatributos.arcano.Essencia.VAZIO));
    }
    public void tick() {
        ciclo += 2;
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            if (!plugin.getConfig().getBoolean("embelezamento.personagens", true) || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.isInvisible() || p.hasPotionEffect(PotionEffectType.INVISIBILITY) || p.isGliding() || p.isSwimming() || p.isSleeping()) { remover(id); passos.remove(id); continue; }
            int costa = ler(p, CORPO, 4), colar = ler(p, COLAR, 2);
            Corpo c = corpos.get(id);
            if (c != null && ((c.costas() != null && (!c.costas().isValid() || c.costas().getWorld() != p.getWorld())) || (c.colar() != null && (!c.colar().isValid() || c.colar().getWorld() != p.getWorld())))) { remover(id); c = null; }
            if (c == null && (costa != 0 || colar != 0)) { c = new Corpo(costa == 0 ? null : criar(p, IDS_CORPO[costa]), colar == 0 ? null : criar(p, "colar")); corpos.put(id, c); }
            double escala = p.getAttribute(Attribute.SCALE) == null ? 1 : p.getAttribute(Attribute.SCALE).getValue();
            if (c != null) for (ItemDisplay d : new ItemDisplay[]{c.costas(), c.colar()}) if (d != null) {
                Location l = p.getLocation().add(0, escala, 0); l.setYaw(p.getBodyYaw() + 180); l.setPitch(0); d.teleport(l);
                d.setTransformationMatrix(new Matrix4f().scale((float) escala * .7f).translate(-.5f, 0, -.5f));
                if (ciclo % 20 == 0 || d.getTicksLived() < 4) for (Player o : Bukkit.getOnlinePlayers()) {
                    if (o.getWorld() == p.getWorld() && o.canSee(p) && o.getLocation().distanceSquared(p.getLocation()) < 2304) o.showEntity(plugin, d); else o.hideEntity(plugin, d);
                }
            }
            if (ciclo % 10 == 0) particula(p, ler(p, AURA, 4), p.getLocation().add(0, 1, 0));
            if (ciclo % 20 == 0 && plugin.getConfig().getBoolean("embelezamento.cenarios", true)) {
                Decoracoes.ambiente(p);
                var tema = plugin.masmorras().temaAtual(p);
                if (tema != null) {
                    Particle ambiente = switch (tema) { case CRIPTA -> Particle.ASH; case CAVERNA -> Particle.SPORE_BLOSSOM_AIR; case INFERNO -> Particle.WHITE_ASH; case GELO -> Particle.SNOWFLAKE; };
                    p.spawnParticle(ambiente, p.getLocation().add(0, 1, 0), 4, 4, 2, 4, .002);
                }
            }
            Location antes = passos.get(id), agora = p.getLocation();
            if (antes == null || antes.getWorld() != agora.getWorld() || antes.distanceSquared(agora) > 64) passos.put(id, agora);
            else if (antes.distanceSquared(agora) > .25) { if (p.isOnGround() && ciclo % 4 == 0) particula(p, ler(p, PASSOS, 4), agora.clone().add(0, .08, 0)); passos.put(id, agora); }
        }
    }
    private static void particula(Player p, int tipo, Location l) {
        if (tipo == 0) return;
        Particle particula = switch (tipo) { case 1 -> Particle.ENCHANT; case 2 -> Particle.CHERRY_LEAVES; default -> Particle.ELECTRIC_SPARK; };
        p.getWorld().spawnParticle(particula, l, 2, .25, .12, .25, .01);
    }
    public void parar() { for (UUID id : List.copyOf(corpos.keySet())) remover(id); passos.clear(); }
}
