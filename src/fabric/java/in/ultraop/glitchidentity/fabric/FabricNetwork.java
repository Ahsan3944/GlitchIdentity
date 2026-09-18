package in.ultraop.glitchidentity.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FabricNetwork {
    private FabricNetwork() {}

    public static void sendGlitch(
        net.minecraft.server.world.ServerWorld world,
        ServerPlayerEntity victim,
        ServerPlayerEntity killer,
        boolean glitchVictim,
        boolean glitchKiller
    ) {
        String safeVictim = glitchVictim ? "" : victim.getName().getString();
        String safeKiller = killer == null || glitchKiller ? "" : killer.getName().getString();

        GlitchPayload payload = new GlitchPayload(
            victim.getId(),
            safeVictim,
            safeKiller,
            glitchVictim,
            glitchKiller
        );

        // Only clients with GlitchIdentity installed receive the custom payload.
        // Vanilla clients continue to receive the normal Minecraft death message.
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, GlitchPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
