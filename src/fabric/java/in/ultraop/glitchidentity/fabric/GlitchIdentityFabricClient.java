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
        });
    }

    public static boolean handleDeathPacket(int victimEntityId) {
        GlitchPayload payload = PENDING_DEATHS.remove(victimEntityId);

        if (payload == null) {
            System.out.println(
                "[GlitchIdentity] Death packet victimId=" +
                victimEntityId +
                " had no glitch payload; vanilla message kept."
            );
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) {
            return false;
        }

        client.inGameHud.getChatHud().addMessage(
            AnimatedGlitchText.death(
                payload.glitchVictim() ? "" : payload.victim(),
                payload.glitchKiller() ? "" : payload.killer(),
                payload.glitchVictim(),
                payload.glitchKiller()
            )
        );

        System.out.println(
            "[GlitchIdentity] Replaced death message for victimId=" +
            victimEntityId
        );

        return true;
    }
}
