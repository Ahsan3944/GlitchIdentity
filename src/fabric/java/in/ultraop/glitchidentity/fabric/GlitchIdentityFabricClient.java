package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static final ConcurrentLinkedDeque<GlitchPayload> PENDING_DEATHS =
        new ConcurrentLinkedDeque<>();

    private static final ThreadLocal<Boolean> INSERTING_REPLACEMENT =
        ThreadLocal.withInitial(() -> false);

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
        if (INSERTING_REPLACEMENT.get() || PENDING_DEATHS.isEmpty()) {
            return null;
        }

        String plain = original.getString();
        if (!looksLikeDeathMessage(plain)) {
            return null;
        }

        GlitchPayload payload = PENDING_DEATHS.pollFirst();
        if (payload == null) {
            return null;
        }

        System.out.println(
            "[GlitchIdentity] Intercepted vanilla death chat: " + plain
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

    public static void addReplacement(ChatHud chatHud, Text replacement) {
        INSERTING_REPLACEMENT.set(true);
        try {
            chatHud.addMessage(replacement);
        } finally {
            INSERTING_REPLACEMENT.set(false);
        }
    }
}
