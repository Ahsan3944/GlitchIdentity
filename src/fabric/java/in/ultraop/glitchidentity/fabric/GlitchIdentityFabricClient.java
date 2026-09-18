package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

import java.util.concurrent.ConcurrentLinkedDeque;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static final ConcurrentLinkedDeque<GlitchPayload> PENDING_DEATHS =
        new ConcurrentLinkedDeque<>();

    @Override
    public void onInitializeClient() {
        System.out.println("[GlitchIdentity] Client animation module loaded.");

        ClientPlayNetworking.registerGlobalReceiver(GlitchPayload.ID, (incoming, context) -> {
            PENDING_DEATHS.addLast(incoming);

            System.out.println(
                "[GlitchIdentity] Received glitch payload: victimId=" +
                incoming.victimEntityId() +
                ", glitchVictim=" + incoming.glitchVictim() +
                ", glitchKiller=" + incoming.glitchKiller()
            );
        });
    }

    public static Text consumeDeathMessage(Text original) {
        String plain = original.getString();
        if (!looksLikeDeathMessage(plain)) {
            return null;
        }

        GlitchPayload payload = PENDING_DEATHS.pollFirst();
        if (payload == null) {
            System.out.println(
                "[GlitchIdentity] Death chat reached ChatHud without a pending glitch payload: " +
                plain
            );
            return null;
        }

        if (!payload.glitchVictim() && !payload.glitchKiller()) {
            return null;
        }

        System.out.println(
            "[GlitchIdentity] Replacing ChatHud death message: " + plain
        );

        return AnimatedGlitchText.death(
            payload.glitchVictim() ? "" : payload.victim(),
            payload.glitchKiller() ? "" : payload.killer(),
            payload.glitchVictim(),
            payload.glitchKiller()
        );
    }

    private static boolean looksLikeDeathMessage(String message) {
        return message.contains(" was slain by ")
            || message.endsWith(" died")
            || message.contains(" died ");
    }
}
