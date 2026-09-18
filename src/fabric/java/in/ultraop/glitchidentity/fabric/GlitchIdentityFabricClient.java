package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static final ConcurrentMap<Integer, GlitchPayload> PENDING_DEATHS =
        new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        System.out.println("[GlitchIdentity] Client animation module loaded.");

        ClientPlayNetworking.registerGlobalReceiver(GlitchPayload.ID, (incoming, context) -> {
            PENDING_DEATHS.put(incoming.victimEntityId(), incoming);

            System.out.println(
                "[GlitchIdentity] Received glitch payload: victimId=" +
                incoming.victimEntityId() +
                ", glitchVictim=" + incoming.glitchVictim() +
                ", glitchKiller=" + incoming.glitchKiller()
            );

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
        boolean replaced = PENDING_DEATHS.remove(victimEntityId) != null;

        System.out.println(
            "[GlitchIdentity] Death packet victimId=" +
            victimEntityId +
            ", customReplacement=" + replaced
        );

        return replaced;
    }
}
