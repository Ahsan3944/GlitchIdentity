package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchMessages;
import in.ultraop.glitchidentity.core.GlitchStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;
import java.util.UUID;

public final class GlitchIdentityFabric implements ModInitializer {
    public static final String MOD_ID = "glitchidentity";
    public static final GlitchStore STORE = new GlitchStore();

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("glitch")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("add").then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .executes(ctx -> { var p = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player"); boolean a=STORE.add(p.getUuid()); ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal(a?"§aGlitch enabled.":"§eAlready enabled."), false); return 1; }))
                .then(CommandManager.literal("remove").then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .executes(ctx -> { var p = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player"); boolean a=STORE.remove(p.getUuid()); ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal(a?"§aGlitch removed.":"§ePlayer is not configured."), false); return 1; }))
                .then(CommandManager.literal("list").executes(ctx -> { ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§dGlitch players: §f"+STORE.all().size()), false); return 1; }))
                .then(CommandManager.literal("reload").executes(ctx -> { ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("§aGlitchIdentity reloaded."), false); return 1; }))
                .then(CommandManager.literal("help").executes(ctx -> { ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal(GlitchMessages.HELP), false); return 1; }))
            )
        );
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (!(killer instanceof ServerPlayerEntity player) || !(killed instanceof ServerPlayerEntity victim)) return;
            if (!STORE.contains(player.getUuid())) return;
            FabricNetwork.sendGlitch(world, victim, player.getName().getString());
        });
    }
}
