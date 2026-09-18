package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static FabricNetwork.GlitchPayload pendingDeath;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.GlitchPayload.ID, (incoming, context) -> {
            pendingDeath = incoming;

            MinecraftClient client = context.client();
            if (client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(
                    AnimatedGlitchText.death(
                        incoming.glitchVictim() ? "" : incoming.victim(),
                        incoming.glitchKiller() ? "" : incoming.killer(),
                        incoming.glitchVictim(),
                        incoming.glitchKiller()
                    )
                );
            }
        });
    }

    public static boolean shouldReplaceDeathMessage(int victimEntityId) {
        FabricNetwork.GlitchPayload current = pendingDeath;
        if (current == null || current.victimEntityId() != victimEntityId) {
            return false;
        }

        pendingDeath = null;
        return true;
    }
}
