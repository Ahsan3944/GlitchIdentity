package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

import java.util.concurrent.ConcurrentLinkedDeque;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static final long PENDING_TIMEOUT_NANOS = 15_000_000_000L;

    private static final ConcurrentLinkedDeque<PendingPayload> PENDING_DEATHS =
        new ConcurrentLinkedDeque<>();

    @Override
    public void onInitializeClient() {
        System.out.println("[GlitchIdentity] Client animation module loaded.");

        ClientPlayNetworking.registerGlobalReceiver(GlitchPayload.ID, (incoming, context) -> {
            purgeExpired();

            if (incoming.glitchVictim() || incoming.glitchKiller()) {
                PENDING_DEATHS.addLast(new PendingPayload(incoming, System.nanoTime()));
            }
        });
    }

    public static Text consumeDeathMessage(Text original) {
        return consumeDeathMessage(original, -1);
    }

    public static Text consumeDeathMessage(Text original, int victimEntityId) {
        purgeExpired();

        String plain = original.getString();

        for (PendingPayload pending : PENDING_DEATHS) {
            GlitchPayload payload = pending.payload();

            boolean idMatches = victimEntityId >= 0 && payload.victimEntityId() == victimEntityId;
            boolean textMatches = stripMarkers(payload.message().getString()).equals(plain);

            if (!idMatches && !textMatches) {
                continue;
            }

            if (!PENDING_DEATHS.remove(pending)) {
                continue;
            }

            return AnimatedGlitchText.death(
                payload.message(),
                payload.glitchVictim(),
                payload.glitchKiller()
            );
        }

        return null;
    }

    private static void purgeExpired() {
        long cutoff = System.nanoTime() - PENDING_TIMEOUT_NANOS;

        while (true) {
            PendingPayload first = PENDING_DEATHS.peekFirst();
            if (first == null || first.receivedAtNanos() >= cutoff) {
                return;
            }
            PENDING_DEATHS.pollFirst();
        }
    }

    private static String stripMarkers(String value) {
        return value
            .replace(GlitchTextSanitizer.VICTIM_MARKER, "")
            .replace(GlitchTextSanitizer.KILLER_MARKER, "");
    }

    private record PendingPayload(GlitchPayload payload, long receivedAtNanos) {}
}
