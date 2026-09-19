package in.ultraop.glitchidentity.fabric;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import in.ultraop.glitchidentity.core.GlitchColorMode;
import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class GlitchIdentityFabric implements ModInitializer {
    public static final String MOD_ID = "glitchidentity";
    public static final GlitchStore STORE = new GlitchStore();
    private static final Map<UUID, Text> PENDING_DEATH_REPLACEMENTS = new ConcurrentHashMap<>();

    public static void prepareDeath(ServerPlayerEntity victim, net.minecraft.entity.damage.DamageSource damageSource) {
        ServerPlayerEntity killer = FabricNetwork.resolvePlayerAttacker(damageSource);

        if (killer == null) {
            String deathLine = victim.getDamageTracker().getDeathMessage().getString();
            killer = victim.getEntityWorld().getServer().getPlayerManager().getPlayerList().stream()
                .filter(player -> !player.getUuid().equals(victim.getUuid()))
                .filter(player -> STORE.contains(player.getUuid()))
                .filter(player ->
                    deathLine.contains(player.getName().getString())
                        || deathLine.contains(player.getDisplayName().getString())
                )
                .findFirst()
                .orElse(null);
        }

        boolean glitchVictim = STORE.contains(victim.getUuid());
        boolean glitchKiller = killer != null && STORE.contains(killer.getUuid());

        if (!glitchVictim && !glitchKiller) return;

        PENDING_DEATH_REPLACEMENTS.put(
            victim.getUuid(),
            FabricNetwork.createSafeDeathMessage(
                victim,
                killer,
                glitchVictim,
                glitchKiller,
                STORE.modeOf(victim.getUuid()),
                killer == null
                    ? GlitchColorMode.COLORFUL
                    : STORE.modeOf(killer.getUuid())
            )
        );
    }

    public static Text peekDeathReplacement(UUID victimId) {
        return PENDING_DEATH_REPLACEMENTS.get(victimId);
    }

    public static Text consumeDeathReplacement(UUID victimId) {
        return PENDING_DEATH_REPLACEMENTS.remove(victimId);
    }

    public static void discardDeathReplacement(UUID victimId) {
        PENDING_DEATH_REPLACEMENTS.remove(victimId);
    }

    private static UUID resolvePlayerId(net.minecraft.server.MinecraftServer server, String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayerList().stream()
                .filter(candidate -> candidate.getName().getString().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
            return player == null ? null : player.getUuid();
        }
    }

    private static int addAllOnline(
        net.minecraft.server.MinecraftServer server,
        GlitchColorMode mode
    ) {
        int changed = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (STORE.add(player.getUuid(), mode)) {
                changed++;
            }
        }
        return changed;
    }

    private static int removeAll() {
        int removed = 0;
        for (UUID id : STORE.all()) {
            if (STORE.remove(id)) {
                removed++;
            }
        }
        return removed;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestTargets(
        CommandContext<ServerCommandSource> context,
        SuggestionsBuilder builder,
        boolean addMode
    ) {
        var server = context.getSource().getServer();
        List<String> candidates = new ArrayList<>();
        candidates.add("@");

        server.getPlayerManager().getPlayerList().stream()
            .filter(player -> addMode
                ? !STORE.contains(player.getUuid())
                : STORE.contains(player.getUuid()))
            .map(player -> player.getName().getString())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEach(candidates::add);

        return CommandSource.suggestMatching(candidates, builder);
    }

    @Override
    public void onInitialize() {
        GlitchPayload.register();
        FabricConfig.load(STORE);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("glitch")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTargets(context, builder, true))
                            .then(CommandManager.literal("colorful")
                                .executes(ctx -> addCommand(ctx, GlitchColorMode.COLORFUL)))
                            .then(CommandManager.literal("white")
                                .executes(ctx -> addCommand(ctx, GlitchColorMode.WHITE))))
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .suggests((context, builder) -> suggestTargets(context, builder, false))
                            .executes(ctx -> {
                                String input = StringArgumentType.getString(ctx, "player");

                                if (input.equals("@")) {
                                    int removed = removeAll();
                                    if (removed > 0) FabricConfig.save(STORE);
                                    int finalRemoved = removed;
                                    ctx.getSource().sendFeedback(
                                        () -> Text.literal("§aGlitch removed from §f" + finalRemoved
                                            + "§a configured player(s)."),
                                        false
                                    );
                                    return 1;
                                }

                                UUID playerId = resolvePlayerId(ctx.getSource().getServer(), input);
                                if (playerId == null) {
                                    ctx.getSource().sendError(Text.literal(
                                        "§cPlayer must be online by name or supplied as a UUID."));
                                    return 0;
                                }

                                boolean removed = STORE.remove(playerId);
                                if (removed) FabricConfig.save(STORE);
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal(
                                        removed ? "§aGlitch removed." : "§ePlayer is not configured."
                                    ),
                                    false
                                );
                                return 1;
                            })))
                    .then(CommandManager.literal("list")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var lines = STORE.all().stream()
                                .map(id -> {
                                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
                                    String name = online != null
                                        ? online.getName().getString()
                                        : id.toString();

                                    return name + " [" + STORE.modeOf(id).name() + "]";
                                })
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList();

                            if (lines.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§7No glitch players configured."), false);
                            } else {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§dGlitch players §7(" + lines.size() + "):"), false);
                                for (String name : lines) {
                                    ctx.getSource().sendFeedback(
                                        () -> Text.literal("§7- §f" + name), false);
                                }
                            }
                            return 1;
                        }))
                    .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            FabricConfig.load(STORE);
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§aGlitchIdentity reloaded."), false);
                            return 1;
                        }))
                    .then(CommandManager.literal("version")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                () -> Text.literal(GlitchMessages.VERSION_MESSAGE), false);
                            return 1;
                        }))
                    .then(CommandManager.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendMessage(Text.literal(GlitchMessages.HELP));
                            return 1;
                        }))
            )
        );
    }

    private static int addCommand(
        CommandContext<ServerCommandSource> context,
        GlitchColorMode mode
    ) {
        String input = StringArgumentType.getString(context, "player");

        if (input.equals("@")) {
            int changed = addAllOnline(context.getSource().getServer(), mode);
            if (changed > 0) {
                FabricConfig.save(STORE);
            }

            int total = context.getSource().getServer().getPlayerManager().getCurrentPlayerCount();
            context.getSource().sendFeedback(
                () -> Text.literal("§aGlitch " + mode.name().toLowerCase() + " mode configured for §f"
                    + changed + "§a online player(s). §7(" + total + " online)"),
                false
            );
            return 1;
        }

        UUID playerId = resolvePlayerId(context.getSource().getServer(), input);
        if (playerId == null) {
            context.getSource().sendError(Text.literal(
                "§cPlayer must be online by name or supplied as a UUID."));
            return 0;
        }

        boolean changed = STORE.add(playerId, mode);
        if (changed) {
            FabricConfig.save(STORE);
        }

        context.getSource().sendFeedback(
            () -> Text.literal(
                changed
                    ? "§aGlitch " + mode.name().toLowerCase() + " mode configured."
                    : "§eAlready configured with " + mode.name().toLowerCase() + " mode."
            ),
            false
        );
        return 1;
    }
}
