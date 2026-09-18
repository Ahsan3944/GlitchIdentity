package in.ultraop.glitchidentity.paper;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public final class GlitchIdentityPaper extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final GlitchStore store = new GlitchStore();
    private File dataFile;

    @Override public void onEnable() {
        dataFile = new File(getDataFolder(), "players.txt");
        loadStore();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("glitch")).setExecutor(this);
        Objects.requireNonNull(getCommand("glitch")).setTabCompleter(this);
        getLogger().info("GlitchIdentity enabled. Paper fallback uses a static corrupted identity for vanilla clients.");
    }

    @Override public void onDisable() { saveStore(); }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || !store.contains(killer.getUniqueId())) return;
        // Paper can replace the death Component, but a vanilla client cannot mutate an already-rendered
        // chat line. Keep the real name hidden and use a colored static corruption as safe fallback.
        var f = in.ultraop.glitchidentity.core.GlitchFrameGenerator.next();
        Component glitch = Component.empty();
        for (int i = 0; i < f.text().length(); i++) {
            glitch = glitch.append(Component.text(String.valueOf(f.text().charAt(i)))
                .color(TextColor.color(f.colors()[i])));
        }
        event.deathMessage(Component.text(event.getEntity().getName() + " was slain by ").append(glitch));
    }

    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.isOp()) { s.sendMessage("§cYou must be OP to use GlitchIdentity."); return true; }
        if (a.length == 0 || a[0].equalsIgnoreCase("help")) { s.sendMessage(GlitchMessages.HELP); return true; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (a.length < 2) { s.sendMessage("§cUsage: /glitch add <player>"); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                OfflinePlayer op = p != null ? p : Bukkit.getOfflinePlayer(a[1]);
                if (op.getUniqueId() == null) { s.sendMessage("§cPlayer not found."); return true; }
                if (store.add(op.getUniqueId())) { saveStore(); s.sendMessage("§aGlitch enabled for " + a[1]); }
                else s.sendMessage("§eAlready enabled.");
            }
            case "remove" -> {
                if (a.length < 2) { s.sendMessage("§cUsage: /glitch remove <player>"); return true; }
                OfflinePlayer op = Bukkit.getOfflinePlayer(a[1]);
                if (store.remove(op.getUniqueId())) { saveStore(); s.sendMessage("§aGlitch removed for " + a[1]); }
                else s.sendMessage("§ePlayer is not configured.");
            }
            case "list" -> {
                if (store.all().isEmpty()) { s.sendMessage("§7No glitch players configured."); return true; }
                s.sendMessage("§dGlitch players:");
                store.all().forEach(id -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(id);
                    s.sendMessage("§7- §f" + (p.getName() == null ? id : p.getName()));
                });
            }
            case "reload" -> { loadStore(); s.sendMessage("§aGlitchIdentity reloaded."); }
            default -> s.sendMessage(GlitchMessages.HELP);
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("add","remove","list","reload","help").stream().filter(x -> x.startsWith(a[0].toLowerCase())).toList();
        if (a.length == 2 && (a[0].equalsIgnoreCase("add") || a[0].equalsIgnoreCase("remove")))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(x -> x.toLowerCase().startsWith(a[1].toLowerCase())).toList();
        return List.of();
    }

    private void loadStore() {
        store.clear();
        if (!dataFile.exists()) return;
        try (var br = new BufferedReader(new FileReader(dataFile))) {
            br.lines().map(String::trim).filter(x -> !x.isEmpty()).forEach(x -> { try { store.add(UUID.fromString(x)); } catch (IllegalArgumentException ignored) {} });
        } catch (IOException e) { getLogger().warning("Could not read players.txt: " + e.getMessage()); }
    }
    private void saveStore() {
        if (dataFile == null) return;
        getDataFolder().mkdirs();
        try (var pw = new PrintWriter(new FileWriter(dataFile))) { store.all().forEach(id -> pw.println(id)); }
        catch (IOException e) { getLogger().warning("Could not save players.txt: " + e.getMessage()); }
    }
}
