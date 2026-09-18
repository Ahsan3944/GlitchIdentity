package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlitchIdentityFabric implements ModInitializer {
    public static final String MOD_ID = "glitchidentity";
    public static final GlitchStore STORE = new GlitchStore();

    private static final Map<String, Long> SUPPRESS_DEATH_MESSAGES = new ConcurrentHashMap<>();
    private static final Path STORE_FILE = Path.of("config", "glitchidentity", "players.txt");

    @Override
    public void onInitialize() {
        loadStore();

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
                                    saveStore();
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
                                    saveStore();
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
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§dGlitch players: §f" + STORE.all().size()),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            loadStore();
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
            SUPPRESS_DEATH_MESSAGES.put(
                vanillaDeathMessage.getString(),
                System.currentTimeMillis() + 3000L
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
            SUPPRESS_DEATH_MESSAGES.entrySet().removeIf(e -> e.getValue() < now);

            Long expiry = SUPPRESS_DEATH_MESSAGES.get(message.getString());
            if (expiry != null) {
                SUPPRESS_DEATH_MESSAGES.remove(message.getString());
                return false;
            }

            return true;
        });
    }

    private static void loadStore() {
        STORE.clear();

        if (!Files.exists(STORE_FILE)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(STORE_FILE)) {
                String value = line.trim();
                if (value.isEmpty()) {
                    continue;
                }

                try {
                    STORE.add(UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed UUID entries so one bad line cannot break loading.
                }
            }
        } catch (IOException ignored) {
            // Keep an empty in-memory store if the configuration file cannot be read.
        }
    }

    private static void saveStore() {
        try {
            Path parent = STORE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StringBuilder data = new StringBuilder();
            for (UUID id : STORE.all()) {
                data.append(id).append(System.lineSeparator());
            }

            Files.writeString(STORE_FILE, data.toString());
        } catch (IOException ignored) {
            // Runtime behavior remains available even if persistence is temporarily unavailable.
        }
    }
}
