package in.ultraop.glitchidentity.paper;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public final class GlitchIdentityPaper extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "glitchidentity.admin";
    private static final String GLITCH_CHANNEL = "glitchidentity:glitch";

    private final GlitchStore store = new GlitchStore();
    private final Set<UUID> animatedClients = new HashSet<>();
    private File dataFile;

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "players.txt");
        loadStore();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, GLITCH_CHANNEL);
        Objects.requireNonNull(getCommand("glitch")).setExecutor(this);
        Objects.requireNonNull(getCommand("glitch")).setTabCompleter(this);

        getLogger().info("GlitchIdentity enabled. Paper supports animated Fabric clients and static fallback for vanilla clients.");
    }

    @Override
    public void onDisable() {
        saveStore();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        boolean glitchVictim = store.contains(victim.getUniqueId());
        boolean glitchKiller = killer != null && store.contains(killer.getUniqueId());

        if (!glitchVictim && !glitchKiller) {
            return;
        }

        Component message = event.deathMessage();
        if (message == null) {
            return;
        }

        Component animatedMessage = replaceWithMarker(
            message,
            glitchVictim ? victim.getName() : null,
            glitchKiller ? (killer != null ? killer.getName() : null) : null
        );
        Component staticMessage = replaceLiteralWithGlitch(
            animatedMessage,
            GlitchIdentityMarkers.VICTIM_MARKER
        );
        staticMessage = replaceLiteralWithGlitch(
            staticMessage,
            GlitchIdentityMarkers.KILLER_MARKER
        );

        event.setShowDeathMessages(false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(
                animatedClients.contains(player.getUniqueId())
                    ? animatedMessage
                    : staticMessage
            );
        }
    }

    private Component replaceWithMarker(
        Component message,
        String victimName,
        String killerName
    ) {
        Component result = message;

        if (victimName != null) {
            result = result.replaceText(builder ->
                builder.matchLiteral(victimName)
                    .replacement(Component.text(GlitchIdentityMarkers.VICTIM_MARKER))
            );
        }

        if (killerName != null) {
            result = result.replaceText(builder ->
                builder.matchLiteral(killerName)
                    .replacement(Component.text(GlitchIdentityMarkers.KILLER_MARKER))
            );
        }

        return result;
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

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        if (GLITCH_CHANNEL.equals(event.getChannel())) {
            animatedClients.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerUnregisterChannel(PlayerUnregisterChannelEvent event) {
        if (GLITCH_CHANNEL.equals(event.getChannel())) {
            animatedClients.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        animatedClients.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be OP to use GlitchIdentity.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(GlitchMessages.HELP);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /glitch add <player>");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[1]);
                OfflinePlayer offlinePlayer = player != null
                    ? player
                    : Bukkit.getOfflinePlayer(args[1]);

                if (store.add(offlinePlayer.getUniqueId())) {
                    saveStore();
                    sender.sendMessage("§aGlitch enabled for " + args[1]);
                } else {
                    sender.sendMessage("§eAlready enabled.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /glitch remove <player>");
                    return true;
                }

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
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

                sender.sendMessage("§dGlitch players:");
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
            default -> sender.sendMessage(GlitchMessages.HELP);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("add", "remove", "list", "reload", "help").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 &&
            (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        }

        return List.of();
    }

    private void loadStore() {
        store.clear();
        if (!dataFile.exists()) {
            return;
        }

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
        if (dataFile == null) {
            return;
        }

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
