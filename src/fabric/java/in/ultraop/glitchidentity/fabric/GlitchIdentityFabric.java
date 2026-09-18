package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GlitchIdentityFabric implements ModInitializer {
    public static final String MOD_ID = "glitchidentity";
    public static final GlitchStore STORE = new GlitchStore();

    private static final ConcurrentLinkedQueue<PendingSuppression> SUPPRESS_DEATH_MESSAGES =
        new ConcurrentLinkedQueue<>();

    private record PendingSuppression(String message, long expiresAt) {}

    @Override
    public void onInitialize() {
        FabricConfig.load(STORE);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("glitch")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> {
                                var player = EntityArgumentType.getPlayer(ctx, "player");
                                boolean added = STORE.add(player.getUuid());
                                if (added) {
                                    FabricConfig.save(STORE);
                                }
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal(added ? "§aGlitch enabled." : "§eAlready enabled."),
                                    false
                                );
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> {
                                var player = EntityArgumentType.getPlayer(ctx, "player");
                                boolean removed = STORE.remove(player.getUuid());
                                if (removed) {
                                    FabricConfig.save(STORE);
                                }
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal(removed ? "§aGlitch removed." : "§ePlayer is not configured."),
                                    false
                                );
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("list")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var lines = STORE.all().stream()
                                .map(id -> {
                                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
                                    return online != null
                                        ? online.getName().getString()
                                        : id.toString();
                                })
                                .sorted()
                                .toList();

                            if (lines.isEmpty()) {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§7No glitch players configured."),
                                    false
                                );
                            } else {
                                ctx.getSource().sendFeedback(
                                    () -> Text.literal("§dGlitch players:"),
                                    false
                                );
                                for (String name : lines) {
                                    ctx.getSource().sendFeedback(
                                        () -> Text.literal("§7- §f" + name),
                                        false
                                    );
                                }
                            }
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            FabricConfig.load(STORE);
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§aGlitchIdentity reloaded."),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                () -> Text.literal(GlitchMessages.HELP),
                                false
                            );
                            return 1;
                        })
                    )
            )
        );

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity victim)) {
                return;
            }

            var attacker = damageSource.getAttacker();
            ServerPlayerEntity killer = attacker instanceof ServerPlayerEntity p ? p : null;

            boolean glitchVictim = STORE.contains(victim.getUuid());
            boolean glitchKiller = killer != null && STORE.contains(killer.getUuid());

            if (!glitchVictim && !glitchKiller) {
                return;
            }

            Text vanillaDeathMessage = victim.getDamageTracker().getDeathMessage();
            SUPPRESS_DEATH_MESSAGES.add(
                new PendingSuppression(
                    vanillaDeathMessage.getString(),
                    System.currentTimeMillis() + 3000L
                )
            );

            FabricNetwork.sendGlitch(
                victim.getEntityWorld(),
                victim,
                killer,
                glitchVictim,
                glitchKiller
            );
        });

        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            if (overlay) {
                return true;
            }

            long now = System.currentTimeMillis();
            SUPPRESS_DEATH_MESSAGES.removeIf(entry -> entry.expiresAt() < now);

            for (PendingSuppression entry : SUPPRESS_DEATH_MESSAGES) {
                if (entry.message().equals(message.getString()) && entry.expiresAt() >= now) {
                    SUPPRESS_DEATH_MESSAGES.remove(entry);
                    return false;
                }
            }

            return true;
        });
    }
}
