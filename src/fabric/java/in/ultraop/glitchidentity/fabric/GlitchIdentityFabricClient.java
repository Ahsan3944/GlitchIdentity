package in.ultraop.glitchidentity.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class GlitchIdentityFabricClient implements ClientModInitializer {
    public static String victim = null;
    public static String killer = null;
    public static long until = 0;

    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetwork.GLITCH, (payload, context) -> {
            context.client().execute(() -> {
                victim = payload.victim();
                killer = "GLITCH";
                until = System.currentTimeMillis() + 6000L;
            });
        });
        HudRenderCallback.EVENT.register((draw, tick) -> {
            if (victim == null || killer == null || System.currentTimeMillis() > until) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer == null) return;
            int y = mc.getWindow().getScaledHeight() - 40;
            draw.drawTextWithShadow(mc.textRenderer, Text.literal(victim + " was slain by "), 4, y, 0xFFFFFF);
            String chars = randomCode();
            int x = 4 + mc.textRenderer.getWidth(victim + " was slain by ");
            for (int i=0;i<chars.length();i++) {
                int[] colors={0x55FFFF,0xFF55FF,0xFFFF55,0x55FF55,0xFF5555,0x5555FF,0xFFFFFF};
                draw.drawTextWithShadow(mc.textRenderer, String.valueOf(chars.charAt(i)), x, y + ((i&1)==0?0:1), colors[(int)(System.nanoTime()/1_000_000 + i)%colors.length]);
                x += mc.textRenderer.getWidth(String.valueOf(chars.charAt(i)));
            }
        });
    }
    private static String randomCode() {
        String pool="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@$%&*+=?!_<>[]{}\\/|";
        var r=new java.util.Random(System.nanoTime());
        StringBuilder s=new StringBuilder(n);
        for(int i=0;i<n;i++) s.append(pool.charAt(r.nextInt(pool.length())));
        return s.toString();
    }
}
