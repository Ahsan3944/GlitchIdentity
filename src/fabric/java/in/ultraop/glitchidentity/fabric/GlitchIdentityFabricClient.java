package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static FabricNetwork.GlitchPayload payload;
    private static long until;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.GlitchPayload.ID, (incoming, context) ->
            context.client().execute(() -> {
                payload = incoming;
                until = System.currentTimeMillis() + 6000L;
            })
        );
    }

    public static boolean shouldReplaceDeathMessage(int victimEntityId) {
        FabricNetwork.GlitchPayload current = payload;
        return current != null
            && current.victimEntityId() == victimEntityId
            && System.currentTimeMillis() <= until;
    }

    public static void renderChatLine(
        DrawContext draw,
        TextRenderer textRenderer,
        int windowHeight
    ) {
        FabricNetwork.GlitchPayload current = payload;
        if (current == null) {
            return;
        }

        if (System.currentTimeMillis() > until) {
            payload = null;
            return;
        }

        // Render in the actual chat area, above the hotbar, rather than as a
        // separate HUD notification.
        int x = 4;
        int y = windowHeight - 82;

        if (current.glitchVictim()) {
            x = drawGlitch(draw, textRenderer, x, y);
        } else {
            String victim = current.victim();
            draw.drawTextWithShadow(textRenderer, Text.literal(victim), x, y, 0xFFFFFF);
            x += textRenderer.getWidth(victim);
        }

        if (current.killer() != null && (!current.killer().isEmpty() || current.glitchKiller())) {
            String separator = " was slain by ";
            draw.drawTextWithShadow(textRenderer, Text.literal(separator), x, y, 0xFFFFFF);
            x += textRenderer.getWidth(separator);

            if (current.glitchKiller()) {
                drawGlitch(draw, textRenderer, x, y);
            } else {
                draw.drawTextWithShadow(
                    textRenderer,
                    Text.literal(current.killer()),
                    x,
                    y,
                    0xFFFFFF
                );
            }
        } else {
            draw.drawTextWithShadow(textRenderer, Text.literal(" died"), x, y, 0xFFFFFF);
        }
    }

    private static int drawGlitch(
        DrawContext draw,
        TextRenderer textRenderer,
        int x,
        int y
    ) {
        GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();
        int[] codePoints = frame.text().codePoints().toArray();

        for (int i = 0; i < codePoints.length; i++) {
            String character = new String(Character.toChars(codePoints[i]));

            draw.drawTextWithShadow(
                textRenderer,
                Text.literal(character),
                x,
                y + ((i & 1) == 0 ? 0 : 1),
                frame.colors()[i]
            );

            // Tiny horizontal corruption makes the identity feel less like
            // ordinary colored text and more like a rapidly changing glitch tag.
            x += textRenderer.getWidth(character);
        }

        return x;
    }
}
