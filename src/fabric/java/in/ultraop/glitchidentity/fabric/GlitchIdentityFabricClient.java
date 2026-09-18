package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static FabricNetwork.GlitchPayload payload;
    private static long until;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.GLITCH, (incoming, context) ->
            context.client().execute(() -> {
                payload = incoming;
                until = System.currentTimeMillis() + 6000L;
            })
        );

        HudRenderCallback.EVENT.register((draw, tick) -> {
            if (payload == null || System.currentTimeMillis() > until) {
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer == null) {
                return;
            }

            int y = mc.getWindow().getScaledHeight() - 40;
            int x = 4;

            if (payload.glitchVictim()) {
                x = drawGlitch(draw, mc, x, y);
            } else {
                String victim = payload.victim();
                draw.drawTextWithShadow(mc.textRenderer, Text.literal(victim), x, y, 0xFFFFFF);
                x += mc.textRenderer.getWidth(victim);
            }

            if (!payload.killer().isEmpty() || payload.glitchKiller()) {
                String separator = " was slain by ";
                draw.drawTextWithShadow(mc.textRenderer, Text.literal(separator), x, y, 0xFFFFFF);
                x += mc.textRenderer.getWidth(separator);

                if (payload.glitchKiller()) {
                    drawGlitch(draw, mc, x, y);
                } else {
                    draw.drawTextWithShadow(
                        mc.textRenderer,
                        Text.literal(payload.killer()),
                        x,
                        y,
                        0xFFFFFF
                    );
                }
            } else {
                String died = " died";
                draw.drawTextWithShadow(mc.textRenderer, Text.literal(died), x, y, 0xFFFFFF);
            }
        });
    }

    private static int drawGlitch(HudRenderCallback.HudRenderContext draw, MinecraftClient mc, int x, int y) {
        GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();

        for (int i = 0; i < frame.text().length(); i++) {
            String character = String.valueOf(frame.text().codePoints()
                .skip(i)
                .findFirst()
                .orElse('?'));
            draw.drawTextWithShadow(
                mc.textRenderer,
                Text.literal(character),
                x,
                y + ((i & 1) == 0 ? 0 : 1),
                frame.colors()[i]
            );
            x += mc.textRenderer.getWidth(character);
        }

        return x;
    }
}
