package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
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
                .filter(player -> deathLine.contains(player.getName().getString()))
                .findFirst()
                .orElse(null);
        }

        boolean glitchVictim = STORE.contains(victim.getUuid());
        boolean glitchKiller = killer != null && STORE.contains(killer.getUuid());

        if (!glitchVictim && !glitchKiller) return;

        PENDING_DEATH_REPLACEMENTS.put(
            victim.getUuid(),
            FabricNetwork.createSafeDeathMessage(victim, killer, glitchVictim, glitchKiller)
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
                            .executes(ctx -> {
                                String input = StringArgumentType.getString(ctx, "player");
                                UUID playerId = resolvePlayerId(ctx.getSource().getServer(), input);
                                if (playerId == null) {
                                    ctx.getSource().sendError(Text.literal("Player must be online by name or supplied as a UUID."));
                                    return 0;
                                }
                                boolean added = STORE.add(playerId);
                                if (added) FabricConfig.save(STORE);
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal(added ? "§aGlitch enabled." : "§eAlready enabled."), false);
                                return 1;
                            })))
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                String input = StringArgumentType.getString(ctx, "player");
                                UUID playerId = resolvePlayerId(ctx.getSource().getServer(), input);
                                if (playerId == null) {
                                    ctx.getSource().sendError(Text.literal("Player must be online by name or supplied as a UUID."));
                                    return 0;
                                }
                                boolean removed = STORE.remove(playerId);
                                if (removed) FabricConfig.save(STORE);
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal(removed ? "§aGlitch removed." : "§ePlayer is not configured."), false);
                                return 1;
                            })))
                    .then(CommandManager.literal("list")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var lines = STORE.all().stream()
                                .map(id -> {
                                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
                                    return online != null ? online.getName().getString() : id.toString();
                                })
                                .sorted()
                                .toList();

                            if (lines.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§7No glitch players configured."), false);
                            } else {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§dGlitch players:"), false);
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
                    .then(CommandManager.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                () -> Text.literal(GlitchMessages.HELP), false);
                            return 1;
                        }))
            )
        );
    }
}