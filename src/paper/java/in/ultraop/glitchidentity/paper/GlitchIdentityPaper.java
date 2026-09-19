package in.ultraop.glitchidentity.paper;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
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

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "players.txt");
        loadStore();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("glitch")).setExecutor(this);
        Objects.requireNonNull(getCommand("glitch")).setTabCompleter(this);
        getLogger().info("GlitchIdentity enabled. Paper uses a non-leaking static fallback.");
    }

    @Override
    public void onDisable() {
        saveStore();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!event.getShowDeathMessages()) return;

        Player victim = event.getEntity();
        DamageSource source = event.getDamageSource();

        Player killer = null;
        Entity causing = source.getCausingEntity();
        Entity direct = source.getDirectEntity();

        if (causing instanceof Player player) {
            killer = player;
        } else if (direct instanceof Player player) {
            killer = player;
        } else {
            killer = victim.getKiller();
        }

        boolean glitchVictim = store.contains(victim.getUniqueId());
        boolean glitchKiller = killer != null && store.contains(killer.getUniqueId());

        if (!glitchVictim && !glitchKiller) return;

        Component message = event.deathMessage();
        if (message == null) return;

        if (glitchVictim) {
            message = replaceLiteralWithGlitch(message, victim.getName());
        }
        if (glitchKiller) {
            message = replaceLiteralWithGlitch(message, killer.getName());
        }

        event.deathMessage(message);
    }

    private Component replaceLiteralWithGlitch(Component message, String name) {
        var frame = GlitchFrameGenerator.next();
        int[] codePoints = frame.text().codePoints().toArray();

        Component glitch = Component.empty();
        for (int i = 0; i < codePoints.length; i++) {
            glitch = glitch.append(
                Component.text(new String(Character.toChars(codePoints[i])))
                    .color(TextColor.color(frame.colors()[i] & 0xFFFFFF))
            );
        }

        final Component replacement = glitch;
        return message.replaceText(builder ->
            builder.matchLiteral(name).replacement(replacement)
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("glitchidentity.admin")) {
            sender.sendMessage("§cYou must have permission §fglitchidentity.admin§c to use GlitchIdentity.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(GlitchMessages.HELP);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /glitch add <player|@>");
                    return true;
                }

                if (args[1].equals("@")) {
                    int added = 0;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (store.add(player.getUniqueId())) added++;
                    }
                    if (added > 0) saveStore();
                    sender.sendMessage("§aGlitch enabled for §f" + added + "§a online player(s).");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[1]);
                OfflinePlayer offlinePlayer = player != null
                    ? player
                    : resolveOfflinePlayerWithoutNetwork(args[1]);

                if (offlinePlayer == null) {
                    sender.sendMessage("§cPlayer is not online or cached. Use the player's UUID or have them join the server first.");
                    return true;
                }

                if (store.add(offlinePlayer.getUniqueId())) {
                    saveStore();
                    sender.sendMessage("§aGlitch enabled for " + args[1]);
                } else {
                    sender.sendMessage("§eAlready enabled.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /glitch remove <player|@>");
                    return true;
                }

                if (args[1].equals("@")) {
                    int removed = 0;
                    for (UUID id : store.all()) {
                        if (store.remove(id)) removed++;
                    }
                    if (removed > 0) saveStore();
                    sender.sendMessage("§aGlitch removed from §f" + removed + "§a configured player(s).");
                    return true;
                }

                Player onlinePlayer = Bukkit.getPlayerExact(args[1]);
                OfflinePlayer offlinePlayer = onlinePlayer != null
                    ? onlinePlayer
                    : resolveOfflinePlayerWithoutNetwork(args[1]);

                if (offlinePlayer == null) {
                    sender.sendMessage("§cPlayer is not online or cached. Use the player's UUID or have them join the server first.");
                    return true;
                }

                if (store.remove(offlinePlayer.getUniqueId())) {
                    saveStore();
                    sender.sendMessage("§aGlitch removed for " + args[1]);
                } else {
                    sender.sendMessage("§ePlayer is not configured.");
                }
            }
            case "list" -> {
                if (store.all().isEmpty()) {
                    sender.sendMessage("§7No glitch players configured.");
                    return true;
                }
                sender.sendMessage("§dGlitch players §7(" + store.all().size() + "):");
                store.all().stream()
                    .sorted(Comparator.comparing(UUID::toString))
                    .forEach(id -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
                        sender.sendMessage("§7- §f" + (player.getName() == null ? id : player.getName()));
                    });
            }
            case "reload" -> {
                loadStore();
                sender.sendMessage("§aGlitchIdentity reloaded.");
            }
            case "version" -> sender.sendMessage(GlitchMessages.VERSION_MESSAGE);
            default -> sender.sendMessage(GlitchMessages.HELP);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("add", "remove", "list", "reload", "version", "help").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            suggestions.add("@");
            Bukkit.getOnlinePlayers().stream()
                .filter(player -> !store.contains(player.getUniqueId()))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(suggestions::add);

            return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            suggestions.add("@");
            Bukkit.getOnlinePlayers().stream()
                .filter(player -> store.contains(player.getUniqueId()))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(suggestions::add);

            return suggestions.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }

        return List.of();
    }

    private OfflinePlayer resolveOfflinePlayerWithoutNetwork(String input) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayerIfCached(input);
        }
    }

    private void loadStore() {
        store.clear();
        if (!dataFile.exists()) return;
        try (var reader = new BufferedReader(new FileReader(dataFile))) {
            reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .forEach(line -> {
                    try {
                        store.add(UUID.fromString(line));
                    } catch (IllegalArgumentException ignored) {
                        getLogger().warning("Ignoring malformed UUID in players.txt: " + line);
                    }
                });
        } catch (IOException e) {
            getLogger().warning("Could not read players.txt: " + e.getMessage());
        }
    }

    private void saveStore() {
        if (dataFile == null) return;
        getDataFolder().mkdirs();
        try (var writer = new PrintWriter(new FileWriter(dataFile))) {
            store.all().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(writer::println);
        } catch (IOException e) {
            getLogger().warning("Could not save players.txt: " + e.getMessage());
        }
    }
}
