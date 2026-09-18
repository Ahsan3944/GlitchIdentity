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

public final class GlitchIdentityFabric implements ModInitializer {
    public static final String MOD_ID = "glitchidentity";
    public static final GlitchStore STORE = new GlitchStore();

    private static final Map<String, Long> SUPPRESS_DEATH_MESSAGES = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("glitch")
                    .requires(src -> { var p = src.getPlayer(); return p != null && p.getPermissionLevel() >= 2; })
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(ctx -> {
                                var player = EntityArgumentType.getPlayer(ctx, "player");
                                boolean added = STORE.add(player.getUuid());
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
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§dGlitch players: §f" + STORE.all().size()),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("reload")
                        .executes(ctx -> {
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

        /*
         * AFTER_DEATH covers both:
         * 1) a configured player killing someone, and
         * 2) a configured player being killed.
         *
         * The real name of a configured player is never placed in the S2C payload.
         */
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
            SUPPRESS_DEATH_MESSAGES.put(vanillaDeathMessage.getString(), System.currentTimeMillis() + 3000L);

            FabricNetwork.sendGlitch(
                victim.getEntityWorld(),
                victim,
                killer,
                glitchVictim,
                glitchKiller
            );
        });

        /*
         * Prevent the original vanilla death line from exposing a configured
         * player's real name. Fabric documents GAME_MESSAGE as covering death
         * messages and ALLOW_GAME_MESSAGE can cancel the broadcast.
         */
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            if (overlay) {
                return true;
            }

            long now = System.currentTimeMillis();
            SUPPRESS_DEATH_MESSAGES.entrySet().removeIf(e -> e.getValue() < now);

            Long expiry = SUPPRESS_DEATH_MESSAGES.get(message.getString());
            if (expiry != null) {
                SUPPRESS_DEATH_MESSAGES.remove(message.getString());
                return false;
            }

            return true;
        });
    }
}
