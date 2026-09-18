package in.ultraop.glitchidentity.fabric;

import in.ultraop.glitchidentity.core.GlitchFrameGenerator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    private static String victim;
    private static long until;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.GLITCH, (payload, context) ->
            context.client().execute(() -> {
                victim = payload.victim();
                until = System.currentTimeMillis() + 6000L;
            })
        );

        HudRenderCallback.EVENT.register((draw, tick) -> {
            if (victim == null || System.currentTimeMillis() > until) {
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer == null) {
                return;
            }

            int y = mc.getWindow().getScaledHeight() - 40;
            String prefix = victim + " was slain by ";
            draw.drawTextWithShadow(mc.textRenderer, Text.literal(prefix), 4, y, 0xFFFFFF);

            GlitchFrameGenerator.Frame frame = GlitchFrameGenerator.next();
            int x = 4 + mc.textRenderer.getWidth(prefix);

            for (int i = 0; i < frame.text().length(); i++) {
                String character = String.valueOf(frame.text().charAt(i));
                draw.drawTextWithShadow(
                    mc.textRenderer,
                    Text.literal(character),
                    x,
                    y + ((i & 1) == 0 ? 0 : 1),
                    frame.colors()[i]
                );
                x += mc.textRenderer.getWidth(character);
            }
        });
    }
}