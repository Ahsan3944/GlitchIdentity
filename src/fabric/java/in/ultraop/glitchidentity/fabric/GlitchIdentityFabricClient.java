package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchColorMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[GlitchIdentity] Client animation module loaded.");

        // Registering the payload is also the capability handshake used by the
        // server to send sanitized death lines only to GlitchIdentity clients.
        ClientPlayNetworking.registerGlobalReceiver(
            GlitchPayload.ID,
            (incoming, context) -> {
                // The actual death line arrives through the normal chat path.
                // This payload exists only so the server can detect modded clients.
            }
        );
    }

    public static Text replaceSanitizedDeathMessage(Text message) {
        String value = message.getString();

        boolean glitchVictim =
            value.contains(GlitchTextSanitizer.VICTIM_MARKER)
                || value.contains(GlitchTextSanitizer.VICTIM_WHITE_MARKER);

        boolean glitchKiller =
            value.contains(GlitchTextSanitizer.KILLER_MARKER)
                || value.contains(GlitchTextSanitizer.KILLER_WHITE_MARKER);

        if (!glitchVictim && !glitchKiller) {
            return null;
        }

        GlitchColorMode victimMode =
            value.contains(GlitchTextSanitizer.VICTIM_WHITE_MARKER)
                ? GlitchColorMode.WHITE
                : GlitchColorMode.COLORFUL;

        GlitchColorMode killerMode =
            value.contains(GlitchTextSanitizer.KILLER_WHITE_MARKER)
                ? GlitchColorMode.WHITE
                : GlitchColorMode.COLORFUL;

        return AnimatedGlitchText.death(
            message,
            glitchVictim,
            glitchKiller,
            victimMode,
            killerMode
        );
    }
}
