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
                "[GlitchIdentity] Received glitch payload: " +
                "glitchVictim=" + incoming.glitchVictim() +
                ", glitchKiller=" + incoming.glitchKiller()
            );
        });
    }

    public static Text consumeDeathMessage(Text original) {
        String plain = original.getString();

        for (GlitchPayload payload : PENDING_DEATHS) {
            String expected = stripMarkers(payload.message().getString());

            if (!expected.equals(plain)) {
                continue;
            }

            PENDING_DEATHS.remove(payload);

            if (!payload.glitchVictim() && !payload.glitchKiller()) {
                return null;
            }

            System.out.println(
                "[GlitchIdentity] Replacing ChatHud death message: " + plain
            );

            return AnimatedGlitchText.death(
                payload.message(),
                payload.glitchVictim(),
                payload.glitchKiller()
            );
        }

        return null;
    }

    private static String stripMarkers(String value) {
        return value
            .replace(GlitchTextSanitizer.VICTIM_MARKER, "")
            .replace(GlitchTextSanitizer.KILLER_MARKER, "");
    }
}
