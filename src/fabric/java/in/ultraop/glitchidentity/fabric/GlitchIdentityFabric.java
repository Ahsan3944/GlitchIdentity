package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlitchIdentityFabric implements DedicatedServerModInitializer {
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

        if (!glitchVictim && !glitchKiller) {
            return;
        }

        Text safeMessage = FabricNetwork.createSafeDeathMessage(
            victim,
            killer,
            glitchVictim,
            glitchKiller
        );

        PENDING_DEATH_REPLACEMENTS.put(victim.getUuid(), safeMessage);
    }

    public static Text consumeDeathReplacement(UUID victimId) {
        return PENDING_DEATH_REPLACEMENTS.remove(victimId);
    }

    @Override
    public void onInitializeServer() {
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
                                if (added) FabricConfig.save(STORE);
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
                                if (removed) FabricConfig.save(STORE);
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
                                    return online != null ? online.getName().getString() : id.toString();
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

    }
}
